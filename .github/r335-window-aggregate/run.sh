#!/usr/bin/env bash
set -euo pipefail
python3 -B .github/r335-window-aggregate/observe.py > "$RUNNER_TEMP/guest-observation-console.txt" 2>&1
