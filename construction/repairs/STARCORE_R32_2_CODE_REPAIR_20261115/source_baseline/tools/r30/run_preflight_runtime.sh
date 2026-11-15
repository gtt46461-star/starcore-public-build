#!/usr/bin/env bash
# Internal diagnostics only. Final signed release workflows always run the full separate gates.
set -euo pipefail
mode=${1:-full}
app_id=com.starcore.research.r13
chart_status=0
if [[ "$mode" == full ]]; then
  bash tools/r30/run_android_regression.sh evidence/r30/android-regression
  adb shell am force-stop "$app_id"
  python3 scripts/probe_r13_startup.py --installed
elif [[ "$mode" == charts-and-faults || "$mode" == charts-and-remaining-faults ]]; then
  set +e
  (
  set -euo pipefail
  serial=$(adb get-serialno)
  [[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
  out=evidence/r30/android-focus
  mkdir -p "$out"
  adb shell am force-stop "$app_id"
  adb logcat -c
  status=0
  adb shell am instrument -w -r -e class com.ethk3.novaonepro.R30ChartsAndroidTest -e r30Sandbox "focus-${GITHUB_RUN_ID:-local}-charts" "$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner" > "$out/charts.txt" 2>&1 || status=$?
  adb logcat -d -v threadtime > "$out/charts-logcat.txt"
  if [[ "$status" != 0 ]] || ! grep -Fq 'OK (1 test)' "$out/charts.txt"; then cat "$out/charts.txt"; echo INTERNAL_CHART_FOCUS_FAILED; exit 1; fi
  adb pull "/sdcard/Android/data/$app_id/files/r30-chart-regression" "$out/screenshots" > "$out/screenshots-pull.txt" 2>&1
  python3 - "$out" <<'PY'
import hashlib,json,pathlib,subprocess,sys
out=pathlib.Path(sys.argv[1]); images=sorted((out/'screenshots').rglob('*.png'))
expected={f'{state}-{i:02d}.png' for state in ('READY','EMPTY','READ_ERROR') for i in range(1,17)}
assert len(images)==48 and {p.name for p in images}==expected
receipt={'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'INTERNAL_CHART_DIAGNOSTIC_ONLY','junitCases':1,'actualScreenshots':48,'fullRegression':'NOT_RUN_IN_THIS_FOCUS','fullAcceptance':False,'physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','screenshots':[{'file':str(p.relative_to(out)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in images]}
(out/'focus.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('R30_INTERNAL_CHART_FOCUS|1_ACTUAL_ANDROID_CASE|48_ACTUAL_PNG|NOT_FINAL_REGRESSION_ACCEPTANCE|PASS')
PY
  )
  chart_status=$?
  set -e
elif [[ "$mode" == sqlite-full-only ]]; then
  echo R30_INTERNAL_SQLITE_FULL_ONLY_REGRESSION_AND_CHARTS_NOT_RUN
else
  echo UNKNOWN_INTERNAL_PREFLIGHT_MODE
  exit 1
fi
set +e
if [[ "$mode" == sqlite-full-only ]]; then
  bash tools/r30/run_fault_matrix.sh evidence/r30/fault-focus sqlite_full
elif [[ "$mode" == charts-and-remaining-faults ]]; then
  bash tools/r30/run_fault_matrix.sh evidence/r30/fault-focus kill_freeze,sqlite_full
else
  bash tools/r30/run_fault_matrix.sh evidence/r30/fault-matrix
fi
fault_status=$?
set -e
if [[ "$chart_status" != 0 || "$fault_status" != 0 ]]; then
  echo "R30_INTERNAL_FOCUS_FAILED:charts=$chart_status:faults=$fault_status"
  exit 1
fi
