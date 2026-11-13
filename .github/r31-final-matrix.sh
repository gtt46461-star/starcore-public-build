#!/usr/bin/env bash
# All original current-source matrix entry points, with the normal start-button
# precondition between the preceding service-stop test and full report probe.
set -uo pipefail
API="$1"; APP="$2"; TEST="$3"; OUT="$4"; mkdir -p "$OUT"
harness=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
adb wait-for-device
adb install -r -g "$APP" > "$OUT/install-app.txt" 2>&1; a=$?
adb install -r -g "$TEST" > "$OUT/install-test.txt" 2>&1; t=$?
rc=0
if [ "$a" -ne 0 ] || [ "$t" -ne 0 ]; then echo "R31_EMU_INSTALL_FAILED app=$a test=$t"; cat "$OUT"/install-*.txt; exit 2; fi
python3 tools/r31/run_android_joint_protocols.py --out "$OUT/joint" --sandbox-prefix "r31v$API" || rc=1
if bash tools/r30-repair/prepare_emulator.sh; then
  python3 -B "$harness/r31-normal-ui-start.py" "$OUT/normal-start-precondition" || rc=1
  bash tools/r30-repair/run_android_regression.sh "$OUT/original-regression" || rc=1
else
  echo R31_ORIGINAL_UI_REGRESSION_NOT_RUN_EMULATOR_ROOT_UNAVAILABLE
  rc=1
fi
bash tools/r30/run_fault_matrix.sh "$OUT/original-fault-matrix" || rc=1
bash tools/r30-repair/run_research_fault_matrix.sh "$OUT/research-fault-matrix" || rc=1
python3 tools/r31/run_emulator_upgrade_matrix.py --apk "$APP" --out "$OUT/upgrade" --api "$API" || rc=1
echo "R31_EMU_MATRIX api=$API rc=$rc"
exit "$rc"
