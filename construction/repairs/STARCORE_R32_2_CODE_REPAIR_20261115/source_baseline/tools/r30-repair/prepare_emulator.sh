#!/usr/bin/env bash
# ADB may reconnect while the emulator action finishes its animation/settings commands.
# Require the actual disposable rooted emulator; never replace a failed test with success.
set -euo pipefail
for attempt in 1 2 3; do
  if ! timeout 25s adb wait-for-device; then
    echo "R30_EMULATOR_TRANSPORT_NOT_READY:attempt=$attempt"
    continue
  fi
  serial=$(adb get-serialno)
  [[ "$serial" == emulator-* ]] || { echo EMULATOR_REQUIRED; exit 1; }
  root_status=0
  adb root || root_status=$?
  echo "R30_EMULATOR_ROOT_COMMAND:attempt=$attempt:status=$root_status"
  if ! timeout 25s adb wait-for-device; then continue; fi
  actual_uid=$(adb shell id -u | tr -d '\r') || actual_uid=UNKNOWN
  actual_boot=$(adb shell getprop sys.boot_completed | tr -d '\r') || actual_boot=UNKNOWN
  actual_qemu=$(adb shell getprop ro.kernel.qemu | tr -d '\r') || actual_qemu=UNKNOWN
  if [[ $actual_uid == 0 && $actual_boot == 1 && $actual_qemu == 1 ]]; then
    echo "R30_EMULATOR_ACTUAL_READY:serial=$serial:uid=$actual_uid:boot=$actual_boot:qemu=$actual_qemu"
    exit 0
  fi
  echo "R30_EMULATOR_RETRY:attempt=$attempt:uid=$actual_uid:boot=$actual_boot:qemu=$actual_qemu"
  sleep 2
done
echo R30_EMULATOR_ROOT_READINESS_FAILED
exit 1
