"""Keep complete Allure reports and recover available earlier main-branch runs."""
import html
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile


def command(*args, **kwargs):
    return subprocess.run(args, check=True, **kwargs)


def read_json(path, fallback):
    return json.loads(path.read_text()) if path.exists() else fallback


def sanitize(value, excluded):
    if isinstance(value, dict):
        if "attachments" in value:
            kept = []
            for attachment in value["attachments"]:
                if attachment.get("type") == "application/zip" or attachment.get("source", "").endswith(".zip"):
                    excluded.add(attachment["source"])
                else:
                    kept.append(attachment)
            value["attachments"] = kept
        for child in value.values():
            sanitize(child, excluded)
    elif isinstance(value, list):
        for child in value:
            sanitize(child, excluded)


def public_results(source, target):
    if not list(source.glob("*-result.json")):
        raise ValueError(f"No test results in {source}")
    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(source, target)
    excluded = set()
    for path in target.glob("*.json"):
        data = json.loads(path.read_text())
        sanitize(data, excluded)
        path.write_text(json.dumps(data, ensure_ascii=False))
    for path in target.iterdir():
        if path.name in excluded or path.suffix == ".zip":
            path.unlink()
    # Never trust embedded history/executor from imported artifacts.
    shutil.rmtree(target / "history", ignore_errors=True)
    (target / "executor.json").unlink(missing_ok=True)


def generate(source, report):
    executor = read_json(source / "executor.json", {})
    command("mvn", "-B", "-Pallure-archive", "allure:report",
            f"-Dallure.executor.url={executor['url']}",
            f"-Dallure.build.order={executor['buildOrder']}",
            f"-Dallure.build.name={executor['buildName']}",
            f"-Dallure.build.url={executor['buildUrl']}",
            f"-Dallure.archive.report.url={executor['reportUrl']}",
            f"-Dallure.archive.report.name={executor['reportName']}",
            f"-Dallure.results.directory={source.resolve()}",
            f"-Dallure.report.directory={report.resolve()}")
    if not (report / "index.html").is_file():
        raise RuntimeError("Allure did not generate index.html")


def add_run(archive, source, run, base_url):
    run_id, attempt, number = int(run["id"]), int(run["run_attempt"]), int(run["run_number"])
    key = f"{run_id}-{attempt}"
    manifest = read_json(archive / "runs.json", [])
    if any(item["key"] == key for item in manifest):
        print(f"Already archived: {key}", flush=True)
        return
    with tempfile.TemporaryDirectory() as temp:
        results = Path(temp) / "results"
        report = Path(temp) / "report"
        public_results(source, results)
        history = archive / "latest" / "history"
        if history.exists():
            shutil.copytree(history, results / "history")
        report_url = f"{base_url.rstrip('/')}/runs/{key}/"
        executor = {
            "name": "GitHub Actions", "type": "github",
            "url": f"https://github.com/{os.environ['GITHUB_REPOSITORY']}",
            "buildOrder": number * 1000 + attempt,
            "buildName": f"Run #{number}, attempt {attempt}",
            "buildUrl": run["html_url"], "reportUrl": report_url,
            "reportName": f"NutriMind #{number}.{attempt}",
        }
        (results / "executor.json").write_text(json.dumps(executor))
        generate(results, report)
        summary = read_json(report / "widgets" / "summary.json", {})
        destination = archive / "runs" / key
        shutil.copytree(report, destination)
        manifest.append({
            "key": key, "number": number, "attempt": attempt,
            "commit": run["head_sha"][:7], "branch": run["head_branch"],
            "date": run["created_at"], "actions_url": run["html_url"],
            "statistic": summary["statistic"],
        })
        manifest.sort(key=lambda item: (item["number"], item["attempt"]), reverse=True)
        (archive / "runs.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2))
        latest = archive / "latest"
        shutil.rmtree(latest, ignore_errors=True)
        shutil.copytree(archive / "runs" / manifest[0]["key"], latest)
    print(f"Archived #{number}.{attempt}: {key}", flush=True)


def build_index(archive):
    rows = []
    manifest = read_json(archive / "runs.json", [])
    for item in manifest:
        stats = item["statistic"]
        rows.append(
            f'<tr><td><a href="runs/{html.escape(item["key"])}/">#{item["number"]}.{item["attempt"]}</a></td>'
            f'<td>{html.escape(item["date"])}</td><td>{html.escape(item["commit"])}</td>'
            f'<td>{stats["passed"]}</td><td>{stats["failed"] + stats["broken"]}</td>'
            f'<td>{stats["skipped"]}</td><td><a href="{html.escape(item["actions_url"], quote=True)}">Actions</a></td></tr>'
        )
    (archive / "index.html").write_text('''<!doctype html><html lang="uk"><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1"><title>NutriMind — архів Allure</title>
<style>body{font:16px system-ui;max-width:1100px;margin:40px auto;padding:0 20px;color:#202124}a{color:#1263bd}table{border-collapse:collapse;width:100%}th,td{padding:12px;text-align:left;border-bottom:1px solid #ddd}nav{margin:24px 0}small{color:#555}</style>
<h1>NutriMind — архів Allure</h1><nav><a href="latest/">Відкрити останній звіт</a></nav>
<p>Збережених запусків: ''' + str(len(manifest)) + '''. Кожна спроба має окремий звіт.</p>
<small>Повні звіти збережені в архіві. History і Trends у Allure 2 показують останні 20 прогонів.</small>
<table><thead><tr><th>Запуск</th><th>Дата (UTC)</th><th>Commit</th><th>Passed</th><th>Failed / Broken</th><th>Skipped</th><th>Логи</th></tr></thead><tbody>'''
        + "".join(rows) + "</tbody></table></html>")
    (archive / ".nojekyll").touch()


def api(path):
    return json.loads(subprocess.check_output(["gh", "api", path], text=True))


def backfill(archive, current_id, base_url):
    repository = os.environ["GITHUB_REPOSITORY"]
    artifacts = []
    page = 1
    while True:
        batch = api(f"repos/{repository}/actions/artifacts?name=ui-test-results&per_page=100&page={page}")["artifacts"]
        artifacts.extend(batch)
        if len(batch) < 100:
            break
        page += 1
    candidates = {}
    for artifact in artifacts:
        run = artifact.get("workflow_run", {})
        if artifact["expired"] or run.get("head_branch") != "main" or run.get("id") == current_id:
            continue
        # Only the newest available artifact per run can identify its current attempt.
        run_id = run["id"]
        if run_id not in candidates or artifact["id"] > candidates[run_id]["id"]:
            candidates[run_id] = artifact
    archived_keys = {item["key"] for item in read_json(archive / "runs.json", [])}
    runs = []
    for run_id, artifact in candidates.items():
        run = api(f"repos/{repository}/actions/runs/{run_id}")
        key = f"{run_id}-{run['run_attempt']}"
        if (key not in archived_keys and run["status"] == "completed"
                and run["event"] in ("push", "workflow_dispatch")
                and run["path"] == ".github/workflows/ui-tests.yml"):
            runs.append((run, artifact))
    for run, artifact in sorted(runs, key=lambda pair: (pair[0]["run_number"], pair[0]["run_attempt"])):
        with tempfile.TemporaryDirectory() as temp:
            bundle = Path(temp) / "artifact.zip"
            with bundle.open("wb") as output:
                command("gh", "api", f"repos/{repository}/actions/artifacts/{artifact['id']}/zip", stdout=output)
            results = Path(temp) / "results"
            results.mkdir()
            with zipfile.ZipFile(bundle) as zipped:
                for entry in zipped.infolist():
                    parts = Path(entry.filename).parts
                    # Only flat Allure result files, no filesystem paths or symlinks.
                    if len(parts) >= 2 and parts[-2] == "allure-results" and not entry.is_dir():
                        name = parts[-1]
                        if re.fullmatch(r"[A-Za-z0-9_.-]+", name):
                            (results / name).write_bytes(zipped.read(entry))
            if not list(results.glob("*-result.json")):
                print(f"Artifact {artifact['id']} has no Allure results; skipped", flush=True)
                continue
            add_run(archive, results, run, base_url)


def main():
    archive = Path("target/allure-archive")
    base_url = os.environ.get("ALLURE_PAGES_URL") or (
        f"https://{os.environ['GITHUB_REPOSITORY_OWNER']}.github.io/"
        f"{os.environ['GITHUB_REPOSITORY'].split('/')[1]}/")
    backfill(archive, int(os.environ["GITHUB_RUN_ID"]), base_url)
    current = {
        "id": os.environ["GITHUB_RUN_ID"], "run_attempt": os.environ["GITHUB_RUN_ATTEMPT"],
        "run_number": os.environ["GITHUB_RUN_NUMBER"], "head_sha": os.environ["GITHUB_SHA"],
        "head_branch": "main", "created_at": os.environ["RUN_CREATED_AT"],
        "html_url": f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/actions/runs/{os.environ['GITHUB_RUN_ID']}",
    }
    add_run(archive, Path("target/allure-public-results"), current, base_url)
    build_index(archive)


if __name__ == "__main__":
    main()
