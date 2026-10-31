#!/usr/bin/env bash
# Genuine Android faults on a disposable emulator. No production files/models or signing identities are read.
set -euo pipefail
out=${1:?evidence directory required}
mkdir -p "$out"
app_id=com.starcore.research.r13
runner="$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
run_id=${GITHUB_RUN_ID:-local-$(date +%s)}
adb shell svc wifi disable
adb shell svc data disable
trap 'adb shell am force-stop com.starcore.research.r13 >/dev/null 2>&1 || true; adb shell svc wifi enable >/dev/null 2>&1 || true; adb shell svc data enable >/dev/null 2>&1 || true' EXIT
: > "$out/cells.tsv"
instrument() {
  local fault=$1 visibility=$2 phase=$3 scope=$4
  adb shell am instrument -w -r -e class com.ethk3.novaonepro.R30FaultMatrixTest#faultCell \
    -e r30Sandbox "$scope" -e r30Fault "$fault" -e r30Visibility "$visibility" -e r30Phase "$phase" "$runner"
}
for visibility in foreground background; do
  for fault in fetch_timeout draw_conflict kill_settlement kill_freeze sqlite_full clock_rollback service_stop; do
    cell="$visibility-$fault"
    scope="fault-$run_id-$cell"
    adb shell am force-stop "$app_id"
    adb logcat -c
    if [[ "$fault" == kill_* ]]; then
      instrument "$fault" "$visibility" arm "$scope" > "$out/$cell-arm.txt" 2>&1 || true
      adb logcat -d -v threadtime > "$out/$cell-arm-logcat.txt"
      if [[ "$fault" == kill_freeze ]]; then operation=R36_CANONICAL; else operation=R36_SETTLEMENT_CORE_R911; fi
      grep -F "R30_FAULT_KILL_POINT" "$out/$cell-arm-logcat.txt" | grep -F "$operation|202610310241|ACTUAL_OWNER_BEFORE_COMMIT" > "$out/$cell-kill-point.txt" || { echo "ACTUAL_KILL_POINT_MISSING:$cell"; exit 1; }
      instrument "$fault" "$visibility" recover "$scope" > "$out/$cell-recover.txt" 2>&1
      result="$out/$cell-recover.txt"
    elif [[ "$fault" == service_stop ]]; then
      instrument "$fault" "$visibility" arm "$scope" > "$out/$cell-arm.txt" 2>&1 & arm_pid=$!
      seen=0
      for attempt in $(seq 1 450); do
        if grep -q 'R30_FAULT_ARMED|service_stop' "$out/$cell-arm.txt"; then seen=1; break; fi
        kill -0 "$arm_pid" 2>/dev/null || break
        sleep .1
      done
      [[ "$seen" == 1 ]] || { wait "$arm_pid" || true; adb logcat -d > "$out/$cell-arm-logcat.txt"; echo "SERVICE_NOT_ARMED:$cell"; exit 1; }
      adb shell dumpsys activity services "$app_id" > "$out/$cell-before-system-stop.txt"
      grep -q 'AutoFetchService' "$out/$cell-before-system-stop.txt" || { echo "ACTUAL_SERVICE_NOT_RUNNING:$cell"; exit 1; }
      adb shell am force-stop "$app_id" > "$out/$cell-system-force-stop.txt"
      wait "$arm_pid" || true
      adb shell dumpsys package "$app_id" > "$out/$cell-after-system-stop.txt"
      grep -q 'stopped=true' "$out/$cell-after-system-stop.txt" || { echo "FORCE_STOP_NOT_APPLIED:$cell"; exit 1; }
      instrument "$fault" "$visibility" recover "$scope" > "$out/$cell-recover.txt" 2>&1
      result="$out/$cell-recover.txt"
    else
      instrument "$fault" "$visibility" run "$scope" > "$out/$cell-run.txt" 2>&1
      result="$out/$cell-run.txt"
    fi
    adb logcat -d -v threadtime > "$out/$cell-logcat.txt"
    grep -q 'OK (1 test)' "$result" && grep -F "R30_FAULT_CELL|$fault|$visibility|" "$result" | grep -q '|PASS' || { echo "FAULT_CELL_FAILED:$cell"; exit 1; }
    printf '%s\t%s\t%s\n' "$fault" "$visibility" "ANDROID_EMULATOR_MECHANICS_PASS" >> "$out/cells.tsv"
    echo "R30_FAULT_MATRIX_PROGRESS:$cell:PASS"
  done
done
python3 - "$out" <<'PY'
import json, pathlib, subprocess, sys
out=pathlib.Path(sys.argv[1]); rows=[line.split('\t') for line in (out/'cells.tsv').read_text().splitlines()]; assert len(rows)==14 and len({tuple(r[:2]) for r in rows})==14
receipt={'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'environment':'ANDROID_EMULATOR','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','cells':[dict(zip(('fault','visibility','mechanics'),row)) for row in rows],'conflictRecovery':'HARD_BLOCK_AWAITING_VERIFIED_CANONICAL; never automatically erase conflicting evidence','serviceStop':'actual system force-stop then explicit user reopen; no automatic restart claim','wallClockFault':'injected wall input with real monotonic samples, not an OS clock change','signingKeysRead':False,'productModelsDeleted':False}
(out/'matrix.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
print('R30_FAULT_MATRIX|14_ACTUAL_EMULATOR_CELLS|NO_PHYSICAL_OR_NATURAL_CLAIM|PASS')
PY
