#!/usr/bin/env bash
# Actual CP06/CP16 same-certificate upgrade, downgrade and re-upgrade. Fresh disposable emulator only.
set -euo pipefail
candidate=${1:?candidate APK required}; lane=${2:?existing certificate lane}; checkpoint=${3:?CP06 or CP16}; out=${4:?evidence directory}
mkdir -p "$out"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
pkg=com.starcore.research.r13
[[ -z $(adb shell pm path "$pkg" | tr -d '\r') ]] || { echo FRESH_EMULATOR_REQUIRED; exit 1; }
work=$(mktemp -d)
finish() {
  local result=$?
  if [[ "$result" != 0 ]]; then
    python3 - "$out" <<'PYCODE'
import json,pathlib,sys
root=pathlib.Path(sys.argv[1])
codes={'Fresh service/heartbeat never remained READY for 60 seconds':'FRESH_SERVICE_60S_GATE_FAILED','STARTUP_ERROR:':'STARTUP_ERROR','STARTUP_RETRIED':'STARTUP_RETRIED','ADB_FAILED:':'ADB_FAILED'}
for stage in ['upgrade','rollback','reupgrade']:
 p=root/(stage+'-startup.log')
 if p.is_file():
  text=p.read_text(errors='replace');print('UPGRADE_STARTUP_FAILURE_SUMMARY',json.dumps({'stage':stage,'codes':[safe for raw,safe in codes.items() if raw in text]}))
PYCODE
  fi
  rm -rf "$work"
  return "$result"
}
trap finish EXIT
python3 - "$lane" "$checkpoint" > "$work/baseline.tsv" <<'PY'
import json,sys
rows=[r for r in json.load(open('tools/r30/UPGRADE_BASELINES.json')) if r['lane']==sys.argv[1] and r['checkpoint']==sys.argv[2]];assert len(rows)==1
r=rows[0];print('\t'.join([r['url'],r['sha256'],r['certificateSha256']]))
PY
IFS=$'\t' read -r url baseline_sha certificate < "$work/baseline.tsv"
curl -fLsS --connect-timeout 15 --max-time 180 "$url" -o "$work/baseline.apk"
printf '%s  %s\n' "$baseline_sha" "$work/baseline.apk" | sha256sum -c -
signer="$ANDROID_HOME/build-tools/36.0.0/apksigner"
"$signer" verify --verbose --print-certs "$work/baseline.apk" > "$out/baseline-public-certificate.txt"
"$signer" verify --verbose --print-certs "$candidate" > "$out/candidate-public-certificate.txt"
grep -q "$certificate" "$out/baseline-public-certificate.txt" && grep -q "$certificate" "$out/candidate-public-certificate.txt"
adb root > "$out/emulator-root.txt"; adb wait-for-device
adb install "$work/baseline.apk" > "$out/install-baseline.txt"
adb shell am start -W -n "$pkg/com.ethk3.novaonepro.MainActivity" > "$out/start-baseline.txt"
sleep 45
adb shell am force-stop "$pkg"
adb shell "mkdir -p /data/user/0/$pkg/files; echo R30-SAME-CERTIFICATE-DATA-RETENTION > /data/user/0/$pkg/files/r30-upgrade-marker"
adb pull "/data/user/0/$pkg/databases" "$work/before" > "$out/baseline-snapshot.txt" 2>&1
adb install -r "$candidate" > "$out/install-upgrade.txt"
python3 scripts/probe_r13_startup.py --installed > "$out/upgrade-startup.log" 2>&1
cp startup-evidence/startup.json "$out/upgrade-startup.json"
adb shell am force-stop "$pkg"
adb pull "/data/user/0/$pkg/databases" "$work/upgraded" > "$out/upgrade-snapshot.txt" 2>&1
python3 tools/r30/check_upgrade_data.py --before "$work/before" --after "$work/upgraded" --label "$lane-$checkpoint-upgrade" --out "$out/upgrade.json"
adb shell "cat /data/user/0/$pkg/files/r30-upgrade-marker" | tr -d '\r' | grep -qx R30-SAME-CERTIFICATE-DATA-RETENTION
# -d is supported on this userdebug emulator. It is not a claim that a production phone permits APK downgrade.
adb install -r -d "$work/baseline.apk" > "$out/install-rollback.txt"
python3 scripts/probe_r13_startup.py --installed > "$out/rollback-startup.log" 2>&1
cp startup-evidence/startup.json "$out/rollback-startup.json"
adb shell am force-stop "$pkg"
adb pull "/data/user/0/$pkg/databases" "$work/rolled-back" > "$out/rollback-snapshot.txt" 2>&1
python3 tools/r30/check_upgrade_data.py --before "$work/upgraded" --after "$work/rolled-back" --label "$lane-$checkpoint-rollback" --out "$out/rollback.json"
adb install -r "$candidate" > "$out/install-reupgrade.txt"
python3 scripts/probe_r13_startup.py --installed > "$out/reupgrade-startup.log" 2>&1
cp startup-evidence/startup.json "$out/reupgrade-startup.json"
adb shell am force-stop "$pkg"
adb pull "/data/user/0/$pkg/databases" "$work/reupgraded" > "$out/reupgrade-snapshot.txt" 2>&1
python3 tools/r30/check_upgrade_data.py --before "$work/upgraded" --after "$work/reupgraded" --label "$lane-$checkpoint-reupgrade" --out "$out/reupgrade.json"
adb shell "cat /data/user/0/$pkg/files/r30-upgrade-marker" | tr -d '\r' | grep -qx R30-SAME-CERTIFICATE-DATA-RETENTION
adb logcat -d -v threadtime > "$out/final-logcat.txt"
printf '%s\n' "R30_UPGRADE_ROLLBACK_REUPGRADE|$lane|$checkpoint|ACTUAL_EMULATOR|NO_UNINSTALL|PUBLIC_CERT_ONLY|PASS" | tee "$out/result.txt"
