#!/usr/bin/env bash
set -euo pipefail
qa_repo="$(cd "$(dirname "$0")/.." && pwd)"
exec node "$qa_repo/qa/run.mjs" "$@"
