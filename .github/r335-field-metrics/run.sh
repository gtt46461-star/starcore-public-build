#!/usr/bin/env bash
set -euo pipefail
observe_status=0
python3 -B .github/r335-field-metrics/observe.py > "$RUNNER_TEMP/guest-observation-console.txt" 2>&1 || observe_status=$?
python3 -B .github/r335-field-metrics/private_window_measurement.py > "$RUNNER_TEMP/private-measurement-console.txt" 2>&1
exit "$observe_status"
