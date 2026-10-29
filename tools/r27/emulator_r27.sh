#!/usr/bin/env bash
# R27 emulator evidence (CI). Not a substitute for the docs/12 real-device matrix (R27-057): emulator receipts are
# labelled EMULATOR and never move a card to "已验证".
#   1. install the 1713d11 baseline debug APK, run it, snapshot its database;
#   2. install the R27 debug APK over it (same signing key), run it, snapshot again  -> upgrade without data loss;
#   3. downgrade back to the baseline (adb install -r -d), run it                    -> rollback keeps working;
#   4. upgrade to R27 again                                                          -> R27 evidence continues;
#   5. uninstall and install R27 fresh                                               -> research ready time, coverage.
# Usage: emulator_r27.sh <baseline.apk> <r27.apk> <out_dir> <minutes_r27_run>
set -uo pipefail
BASE_APK=$1; R27_APK=$2; OUT=$3; MIN=${4:-15}
PKG=com.starcore.research.r13; DB=binance_k3_nova_one_pro_v57.db
mkdir -p "$OUT"
log() { echo "[$(date -u +%H:%M:%S)] $*" | tee -a "$OUT/emulator.log"; }
wake() { adb shell input keyevent 224 >/dev/null 2>&1; adb shell wm dismiss-keyguard >/dev/null 2>&1; adb shell settings put system screen_off_timeout 1800000 >/dev/null 2>&1; }
launch() { adb shell am start -W -n $PKG/com.ethk3.novaonepro.MainActivity > "$OUT/launch-$1.txt" 2>&1; grep -q "Status: ok" "$OUT/launch-$1.txt"; }
snapshot() {  # copy db + wal + shm; "stop" first force-stops the app so the copy is consistent (phase ends only)
  local d="$OUT/db-$1"; mkdir -p "$d"; [ "${2:-}" = stop ] && { adb shell am force-stop $PKG; sleep 2; }
  for f in "$DB" "$DB-wal" "$DB-shm"; do adb exec-out run-as $PKG cat "databases/$f" > "$d/$f" 2>/dev/null || rm -f "$d/$f"; done
  ls -l "$d" >> "$OUT/emulator.log"
}
alive() { local pid; pid=$(adb shell pidof $PKG | tr -d '\r'); [ -n "$pid" ]; }
fatal() { adb logcat -d > "$OUT/logcat-$1.txt" 2>&1; ! grep -q "FATAL EXCEPTION\|ANR in $PKG" "$OUT/logcat-$1.txt"; }
rc=0; step() { local name=$1; shift; if "$@"; then log "STEP $name OK"; else log "STEP $name FAILED"; rc=1; fi; }

python3 scripts/wait_r13_android_ready.py; wake
adb uninstall $PKG >/dev/null 2>&1
log "baseline install"; step baseline_install adb install "$BASE_APK"
adb logcat -c; step baseline_launch launch baseline
sleep 240; wake; step baseline_alive alive; step baseline_no_fatal fatal baseline; snapshot baseline stop

log "upgrade to R27"; step r27_install_over_baseline adb install -r "$R27_APK"
adb logcat -c; step r27_launch launch r27
for i in $(seq 1 "$MIN"); do sleep 60; wake; [ $((i % 5)) -eq 0 ] && snapshot "r27-${i}m"; done
step r27_alive alive; step r27_no_fatal fatal r27; snapshot r27 stop

log "rollback to baseline"; step rollback_install adb install -r -d "$BASE_APK"
adb logcat -c; step rollback_launch launch rollback
sleep 180; wake; step rollback_alive alive; step rollback_no_fatal fatal rollback; snapshot rollback stop

log "re-upgrade to R27"; step reupgrade_install adb install -r "$R27_APK"
adb logcat -c; step reupgrade_launch launch reupgrade
sleep 300; wake; step reupgrade_alive alive; step reupgrade_no_fatal fatal reupgrade; snapshot reupgrade stop

# 5. Fresh install (what a user gets after uninstalling): how fast research storage becomes ready, and how many of
#    the periods drawn while the app runs get a pre-draw prediction (with the embedded data key).
log "fresh install R27"; adb uninstall $PKG >/dev/null 2>&1; step fresh_install adb install "$R27_APK"
adb logcat -c; date +%s > "$OUT/fresh_start_epoch.txt"; step fresh_launch launch fresh
for i in 1 2 3 5 10 15; do while [ $(( $(date +%s) - $(cat "$OUT/fresh_start_epoch.txt") )) -lt $((i*60)) ]; do sleep 10; wake; done; snapshot "fresh-${i}m"; done
step fresh_alive alive; step fresh_no_fatal fatal fresh
grep -iE "fetch|R925|R914|draw|api|http" "$OUT/logcat-fresh.txt" | grep -i "$PKG\|starcore\|r925\|r914\|fetch" | tail -n 60 | sed 's/^/R27_EMULATOR_LOGCAT|/' | tee "$OUT/logcat-fresh-fetch.txt"

python3 tools/r27/emulator_receipt.py "$OUT" | tee "$OUT/receipt.txt"; r=${PIPESTATUS[0]}; [ $r -eq 0 ] || rc=1
log "EMULATOR_R27 rc=$rc"; exit $rc
