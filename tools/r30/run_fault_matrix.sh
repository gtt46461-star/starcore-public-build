#!/usr/bin/env bash
# Genuine Android faults on a disposable emulator. No production files/models or signing identities are read.
set -euo pipefail
out=${1:?evidence directory required}
mkdir -p "$out"
diagnostic=${2:-full}
case "$diagnostic" in
  full) fault_kinds=(fetch_timeout draw_conflict kill_settlement kill_freeze sqlite_full clock_rollback service_stop); expected_cells=14 ;;
  kill_freeze,sqlite_full) fault_kinds=(kill_freeze sqlite_full); expected_cells=4 ;;
  *) echo UNKNOWN_FAULT_DIAGNOSTIC_SCOPE; exit 1 ;;
esac
app_id=com.starcore.research.r13
runner="$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
run_id=${GITHUB_RUN_ID:-local-$(date +%s)}
adb shell svc wifi disable
adb shell svc data disable
clock_restore_needed=0
clock_original_auto_time=
restore_clock() {
  local restored_ms
  restored_ms=$(python3 -c 'import time; print(time.time_ns()//1000000)')
  local result=0
  adb shell cmd alarm set-time "$restored_ms" >> "$out/os-clock-restore.txt" || result=1
  if [[ "$clock_original_auto_time" == null ]]; then adb shell settings delete global auto_time >> "$out/os-clock-restore.txt" || result=1;
  else adb shell settings put global auto_time "$clock_original_auto_time" >> "$out/os-clock-restore.txt" || result=1; fi
  if [[ "$result" == 0 ]]; then clock_restore_needed=0; fi
  return "$result"
}
finish() {
  local result=$?
  if [[ "$clock_restore_needed" == 1 ]]; then restore_clock || result=1; fi
  if [[ "$result" != 0 ]]; then
    for file in "$out/"*-run.txt "$out/"*-recover.txt "$out/"*-arm.txt "$out/"*-os-rollback.txt "$out/os-clock-restore.txt"; do
      [[ -f "$file" ]] && { echo "FAULT_FAILURE_OUTPUT:$file"; tail -n 80 "$file"; }
    done
  fi
  adb shell am force-stop "$app_id" >/dev/null 2>&1 || true
  adb shell svc wifi enable >/dev/null 2>&1 || true
  adb shell svc data enable >/dev/null 2>&1 || true
  return "$result"
}
trap finish EXIT
: > "$out/cells.tsv"
instrument() {
  local fault=$1 visibility=$2 phase=$3 scope=$4
  adb shell am instrument -w -r -e class com.ethk3.novaonepro.R30FaultMatrixTest#faultCell \
    -e r30Sandbox "$scope" -e r30Fault "$fault" -e r30Visibility "$visibility" -e r30Phase "$phase" "$runner"
}
run_cell() (
  set -euo pipefail
  fault=$1
  visibility=$2
  cell_finish() {
    local result=$?
    if [[ "$clock_restore_needed" == 1 ]] && ! restore_clock; then result=92; fi
    adb shell am force-stop "$app_id" >/dev/null 2>&1 || true
    exit "$result"
  }
  trap cell_finish EXIT
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
  elif [[ "$fault" == clock_rollback ]]; then
    instrument "$fault" "$visibility" run "$scope" > "$out/$cell-run.txt" 2>&1 & clock_pid=$!
    seen=0
    for attempt in $(seq 1 450); do
      if grep -q 'R30_FAULT_CLOCK_ARMED|' "$out/$cell-run.txt"; then seen=1; break; fi
      kill -0 "$clock_pid" 2>/dev/null || break
      sleep .1
    done
    [[ "$seen" == 1 ]] || { wait "$clock_pid" || true; echo "OS_CLOCK_NOT_ARMED:$cell"; exit 1; }
    clock_original_auto_time=$(adb shell settings get global auto_time | tr -d '\r')
    clock_restore_needed=1
    adb shell settings put global auto_time 0
    clock_before_seconds=$(adb shell date +%s | tr -d '\r')
    [[ "$clock_before_seconds" =~ ^[0-9]+$ ]] || { echo OS_CLOCK_UNREADABLE; exit 1; }
    adb shell cmd alarm set-time "$((clock_before_seconds * 1000 - 60000))" > "$out/$cell-os-rollback.txt"
    seen=0
    for attempt in $(seq 1 150); do
      if grep -q 'R30_FAULT_CLOCK_OBSERVED|' "$out/$cell-run.txt"; then seen=1; break; fi
      kill -0 "$clock_pid" 2>/dev/null || break
      sleep .1
    done
    restore_clock
    [[ "$seen" == 1 ]] || { wait "$clock_pid" || true; echo "ACTUAL_OS_CLOCK_ROLLBACK_NOT_OBSERVED:$cell"; exit 1; }
    wait "$clock_pid"
    result="$out/$cell-run.txt"
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
)
: > "$out/results.tsv"
failed_cells=0
clock_original_auto_time=$(adb shell settings get global auto_time | tr -d '\r')
for visibility in foreground background; do
  for fault in "${fault_kinds[@]}"; do
    set +e
    run_cell "$fault" "$visibility"
    cell_status=$?
    set -e
    printf '%s\t%s\t%s\n' "$fault" "$visibility" "$cell_status" >> "$out/results.tsv"
    if [[ "$cell_status" != 0 ]]; then
      failed_cells=$((failed_cells + 1))
      echo "R30_FAULT_MATRIX_PROGRESS:$visibility-$fault:FAIL:status=$cell_status"
      if [[ "$cell_status" == 92 ]]; then clock_restore_needed=1; echo OS_CLOCK_RESTORE_BLOCKED_REMAINING_NOT_RUN; break 2; fi
    fi
  done
done
if [[ "$failed_cells" != 0 ]]; then echo "R30_FAULT_MATRIX_FAILED_CELLS:$failed_cells"; exit 1; fi
python3 - "$out" "$diagnostic" "$expected_cells" <<'PY'
import json, pathlib, subprocess, sys
out=pathlib.Path(sys.argv[1]); diagnostic=sys.argv[2]; expected=int(sys.argv[3]); rows=[line.split('\t') for line in (out/'cells.tsv').read_text().splitlines()]; assert len(rows)==expected and len({tuple(r[:2]) for r in rows})==expected
receipt={'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'environment':'ANDROID_EMULATOR','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','cells':[dict(zip(('fault','visibility','mechanics'),row)) for row in rows],'conflictRecovery':'HARD_BLOCK_AWAITING_VERIFIED_CANONICAL; never automatically erase conflicting evidence','serviceStop':'actual system force-stop then explicit user reopen; no automatic restart claim','wallClockFault':'actual rooted-emulator OS rollback and restoration through AlarmManager shell, two real wall/monotonic samples; restored before original-deadline recovery','signingKeysRead':False,'productModelsDeleted':False}
if diagnostic=='full':
 (out/'matrix.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
 print('R30_FAULT_MATRIX|14_ACTUAL_EMULATOR_CELLS|NO_PHYSICAL_OR_NATURAL_CLAIM|PASS')
else:
 receipt.update(scope='INTERNAL_REMAINING_FOUR_FAULTS_ONLY',full14Acceptance=False)
 (out/'diagnostic-matrix.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
 print('R30_FAULT_DIAGNOSTIC|4_ACTUAL_CELLS|NOT_FULL14_ACCEPTANCE|PASS')
PY
