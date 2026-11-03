#!/usr/bin/env bash
set -euo pipefail
archive=target/allure-archive
branch=allure-history
if git ls-remote --exit-code --heads origin "refs/heads/$branch"; then
  git fetch --no-tags origin "$branch"
  git worktree add --detach "$archive" FETCH_HEAD
else
  status=$?
  if [ "$status" -ne 2 ]; then
    exit "$status"
  fi
  git worktree add --detach "$archive" HEAD
  git -C "$archive" checkout --orphan "$branch"
  git -C "$archive" rm -rf .
fi
