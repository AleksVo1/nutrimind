"""Copy Allure results for public Pages without Playwright session traces."""
import json
import shutil
from pathlib import Path

source = Path("target/allure-results")
target = Path("target/allure-pages-results")
if not list(source.glob("*-result.json")):
    raise SystemExit("No Allure test results to publish")
if target.exists():
    shutil.rmtree(target)
shutil.copytree(source, target)
removed = set()


def sanitize(value):
    if isinstance(value, dict):
        attachments = value.get("attachments", [])
        for attachment in attachments:
            if attachment.get("type") == "application/zip" or attachment.get("source", "").endswith(".zip"):
                removed.add(attachment["source"])
        if "attachments" in value:
            value["attachments"] = [a for a in attachments if a.get("source") not in removed]
        for child in value.values():
            sanitize(child)
    elif isinstance(value, list):
        for child in value:
            sanitize(child)


for path in target.glob("*.json"):
    data = json.loads(path.read_text())
    sanitize(data)
    path.write_text(json.dumps(data, ensure_ascii=False))
for path in target.iterdir():
    if path.name in removed or path.suffix == ".zip":
        path.unlink()
print(f"Public Allure results prepared; excluded {len(removed)} trace attachments")
