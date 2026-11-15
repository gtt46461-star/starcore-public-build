#!/usr/bin/env bash
# R31-B12 EMU matrix on one disposable emulator (called by r31-verification.yml instrumentation job).
# Usage: bash tools/r31/emulator_matrix.sh <api> <app.apk> <androidTest.apk> <out-dir>
set -uo pipefail
API="$1"; APP="$2"; TEST="$3"; OUT="$4"; mkdir -p "$OUT"
adb wait-for-device
adb install -r -g "$APP" > "$OUT/install-app.txt" 2>&1; a=$?
adb install -r -g "$TEST" > "$OUT/install-test.txt" 2>&1; t=$?
rc=0
if [ $a -ne 0 ] || [ $t -ne 0 ]; then echo "R31_EMU_INSTALL_FAILED app=$a test=$t"; cat "$OUT"/install-*.txt; exit 2; fi
# C02#A3b: the existing named Android protocol groups (runtime/storage/owner fence, continuation, history, platform)
python3 tools/r31/run_android_joint_protocols.py --out "$OUT/joint" --sandbox-prefix "r31v$API" || rc=1
# Preserve and execute the original cumulative Android regressions as well as
# the new named protocols. Their own strict collectors bind real screenshots,
# original-owner process deaths, fourteen runtime and twelve research cells.
# A named protocol pass never stands in for these separate matrices.
if bash tools/r30-repair/prepare_emulator.sh; then
  bash tools/r30-repair/run_android_regression.sh "$OUT/original-regression" || rc=1
else
  echo R31_ORIGINAL_UI_REGRESSION_NOT_RUN_EMULATOR_ROOT_UNAVAILABLE
  rc=1
fi
bash tools/r30/run_fault_matrix.sh "$OUT/original-fault-matrix" || rc=1
bash tools/r30-repair/run_research_fault_matrix.sh "$OUT/research-fault-matrix" || rc=1
# C01#A1b/#A2: overlay install, process kills during startup, relaunch and database inspection
python3 tools/r31/run_emulator_upgrade_matrix.py --apk "$APP" --out "$OUT/upgrade" --api "$API" || rc=1
echo "R31_EMU_MATRIX api=$API rc=$rc"
exit $rc
