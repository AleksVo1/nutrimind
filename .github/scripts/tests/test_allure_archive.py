import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("archive", Path(__file__).parents[1] / "allure_archive.py")
archive_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(archive_module)


class ArchiveTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.archive = self.root / "archive"
        self.archive.mkdir()
        self.source = self.root / "results"
        self.source.mkdir()
        (self.source / "test-result.json").write_text(json.dumps({
            "name": "test", "attachments": [{"type": "application/zip", "source": "trace.zip"},
                                              {"type": "image/png", "source": "screen.png"}]}))
        (self.source / "trace.zip").write_bytes(b"session data")
        (self.source / "screen.png").write_bytes(b"screenshot")
        self.env = patch.dict(os.environ, {"GITHUB_REPOSITORY": "owner/repo"})
        self.env.start()
        self.addCleanup(self.env.stop)

    def run_info(self, number, attempt=1):
        return {"id": 100 + number, "run_number": number, "run_attempt": attempt,
                "head_sha": "abcdef123456", "head_branch": "main", "created_at": "2026-11-03T12:00:00Z",
                "html_url": f"https://github.com/owner/repo/actions/runs/{100+number}"}

    def fake_generate(self, results, report):
        # The next report must receive the previous report history.
        old = archive_module.read_json(results / "history" / "history-trend.json", [])
        executor = json.loads((results / "executor.json").read_text())
        self.assertTrue(executor["reportUrl"].startswith("https://owner.github.io/repo/runs/"))
        self.assertFalse((results / "trace.zip").exists())
        (report / "widgets").mkdir(parents=True)
        (report / "history").mkdir()
        (report / "index.html").write_text("report")
        (report / "widgets" / "summary.json").write_text(json.dumps({"statistic": {
            "passed": 7, "failed": 1, "broken": 0, "skipped": 0, "unknown": 0, "total": 8}}))
        (report / "history" / "history-trend.json").write_text(json.dumps([executor] + old[:19]))

    def add(self, number, attempt=1):
        archive_module.add_run(self.archive, self.source, self.run_info(number, attempt),
                               "https://owner.github.io/repo/")

    def test_keeps_all_reports_beyond_allure_history_limit(self):
        with patch.object(archive_module, "generate", side_effect=self.fake_generate):
            for number in range(1, 26):
                self.add(number)
        self.assertEqual(len(list((self.archive / "runs").iterdir())), 25)
        trend = json.loads((self.archive / "latest/history/history-trend.json").read_text())
        self.assertEqual(len(trend), 20)
        self.assertTrue((self.archive / "runs/101-1/index.html").exists())
        archive_module.build_index(self.archive)
        self.assertIn('runs/101-1/', (self.archive / "index.html").read_text())
        self.assertIn('runs/125-1/', (self.archive / "index.html").read_text())

    def test_retry_is_separate_and_republishing_is_idempotent(self):
        with patch.object(archive_module, "generate", side_effect=self.fake_generate) as generate:
            self.add(1)
            self.add(1, 2)
            self.add(1, 2)
            self.assertEqual(generate.call_count, 2)
        manifest = json.loads((self.archive / "runs.json").read_text())
        self.assertEqual([x["key"] for x in manifest], ["101-2", "101-1"])
        # Original diagnostics remain intact.
        self.assertTrue((self.source / "trace.zip").exists())

    def test_does_not_backfill_expired_current_or_other_branch_artifacts(self):
        records = [
            {"id": 1, "expired": True, "workflow_run": {"id": 101, "head_branch": "main"}},
            {"id": 2, "expired": False, "workflow_run": {"id": 102, "head_branch": "feature"}},
            {"id": 3, "expired": False, "workflow_run": {"id": 103, "head_branch": "main"}},
        ]
        with patch.object(archive_module, "api", return_value={"artifacts": records}) as api:
            archive_module.backfill(self.archive, 103, "https://owner.github.io/repo/")
            self.assertEqual(api.call_count, 1)


if __name__ == "__main__":
    unittest.main()
