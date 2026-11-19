"""Persist the test attempt independently of later deployment-only retries."""
from datetime import datetime, timezone
import json
import os
from pathlib import Path

results = Path("target/allure-results")
if list(results.glob("*-result.json")):
    metadata = {
        "id": int(os.environ["GITHUB_RUN_ID"]),
        "run_attempt": int(os.environ["GITHUB_RUN_ATTEMPT"]),
        "run_number": int(os.environ["GITHUB_RUN_NUMBER"]),
        "head_sha": os.environ["GITHUB_SHA"],
        "created_at": datetime.now(timezone.utc).isoformat(),
    }
    (results / "github-run.json").write_text(json.dumps(metadata))
