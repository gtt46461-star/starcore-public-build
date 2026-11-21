#!/usr/bin/env bash
set -euo pipefail
p="$RUNNER_TEMP/protocol-candidate"
raw="$RUNNER_TEMP/report-originals"
e="$RUNNER_TEMP/upgrade-evidence"
cd app-source
release=$(find "$p" -name '*-release.apk' -print -quit)
adb install -r "$release" > "$e/target-install.txt"
adb install -r -t "$p/instrument-androidTest.apk" > "$e/test-install.txt"
read -r installed <<< "$(adb shell pm path com.starcore.research.r13 | tr -d '\r' | sed 's/^package://' | head -1)"
actual=$(adb shell sha256sum "$installed" | awk '{print $1}' | tr -d '\r')
original=$(sha256sum "$release" | awk '{print $1}')
test "$actual" = "$original"
adb shell getprop ro.build.fingerprint > "$e/emulator-fingerprint.txt"
adb shell dumpsys package com.starcore.research.r13 > "$e/installed-package.txt"
printf '%s\n' "$actual" > "$e/target-source-apk-sha256.txt"
git rev-parse HEAD > "$e/source-commit.txt"
adb shell pm grant com.starcore.research.r13 android.permission.POST_NOTIFICATIONS

test "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" = 1
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
export R335_DEVICE_API="$api" R335_INSTALLED_SHA="$actual" R335_PROBE_EVIDENCE="$e"
scratch="$RUNNER_TEMP/local-synthetic-probes"
mkdir -p "$scratch"
export R335_FAULT_SCRATCH="$scratch"
overall=0
run_suite() {
  label="$1"
  shift
  code=0
  "$@" > "$scratch/$label-console.txt" 2>&1 || code=$?
  python3 - "$scratch/$label.exit" "$code" <<'EXITREC'
import sys
from pathlib import Path
Path(sys.argv[1]).write_text(sys.argv[2]+'\n')
EXITREC
  echo "$label exit=$code"
  if [ "$code" -ne 0 ]; then overall=1; fi
}
run_suite preparation bash tools/r30-repair/prepare_emulator.sh
run_suite runtime bash tools/r30/run_fault_matrix.sh "$scratch/runtime"
run_suite research bash tools/r30-repair/run_research_fault_matrix.sh "$scratch/research"
run_suite gallery bash tools/r30-repair/run_android_regression.sh "$scratch/gallery"
run_suite upgrade python3 -B tools/r31/run_emulator_upgrade_matrix.py --apk "$release" --out "$scratch/upgrade" --api "$api"
python3 -B "$GITHUB_WORKSPACE/.github/r335-final-faults/collect.py"
exit "$overall"
