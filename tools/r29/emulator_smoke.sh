#!/usr/bin/env bash
# Destructive installation steps are restricted to a disposable Android emulator, never a connected phone.
# This smoke test does NOT sign off the 60-minute/6-hour/72-hour physical-device scenarios or quality targets.
set -euo pipefail
DELIVERED=$1; OUT=$2; mkdir -p "$OUT"
BASE="$OUT/baseline.apk"
cleanup() { rm -f "$OUT"/*.db "$OUT"/*.db-wal "$OUT"/*.db-shm "$BASE"; }
trap cleanup EXIT
PKG=com.starcore.research.r13
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] || { echo 'EMULATOR_REQUIRED'; exit 1; }
APK=$(find "$DELIVERED" -maxdepth 1 -name '*debug.apk' -print -quit)
[ -n "$APK" ]
python3 scripts/wait_r13_android_ready.py
adb root > "$OUT/root.txt"; adb wait-for-device
BASE_URL=${R29_BASELINE_URL:-https://github.com/gtt46461-star/starcore-public-build/releases/download/r26-20261028-runtime-117/STARCORE-R26-20261028-117-release.apk}
BASE_HASH=${R29_BASELINE_SHA256:-739e6b13c692d11ed1cbc7c09f5f95d904328cefc1bf9d6616a9e5266203d433}
curl -fLsS --connect-timeout 15 --max-time 120 -o "$BASE" "$BASE_URL"
echo "$BASE_HASH  $BASE" | sha256sum -c -
adb uninstall "$PKG" > /dev/null 2>&1 || true
adb install "$BASE" > "$OUT/install-baseline.txt"
adb shell am start -W -n "$PKG/com.ethk3.novaonepro.MainActivity" > "$OUT/launch-baseline.txt"
sleep 45
adb shell am force-stop "$PKG"
adb shell "mkdir -p /data/user/0/$PKG/files; echo r29-official-upgrade-probe > /data/user/0/$PKG/files/r29-upgrade-probe"
# Baseline database must actually exist; a marker file alone does not establish schema/data continuity.
adb pull "/data/user/0/$PKG/databases/binance_k3_nova_one_pro_v57.db" "$OUT/before.db" > /dev/null
for suffix in -wal -shm; do adb pull "/data/user/0/$PKG/databases/binance_k3_nova_one_pro_v57.db$suffix" "$OUT/before.db$suffix" > /dev/null 2>&1 || true; done
adb install -r "$APK" > "$OUT/install-upgrade.txt"
adb shell "cat /data/user/0/$PKG/files/r29-upgrade-probe" | tr -d '\r' > "$OUT/retained-marker.txt"
grep -qx 'r29-official-upgrade-probe' "$OUT/retained-marker.txt"
python3 scripts/probe_r13_startup.py --installed > "$OUT/upgrade-startup.log" 2>&1
adb shell am force-stop "$PKG"
adb pull "/data/user/0/$PKG/databases/binance_k3_nova_one_pro_v57.db" "$OUT/after.db" > /dev/null
for suffix in -wal -shm; do adb pull "/data/user/0/$PKG/databases/binance_k3_nova_one_pro_v57.db$suffix" "$OUT/after.db$suffix" > /dev/null 2>&1 || true; done
python3 - "$OUT" <<'PY'
import json, sqlite3, sys
from pathlib import Path
p=Path(sys.argv[1]); before=sqlite3.connect(p/'before.db'); after=sqlite3.connect(p/'after.db')
tables=lambda c:{r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
missing=sorted(tables(before)-tables(after))
integrity=after.execute('PRAGMA integrity_check').fetchone()[0]
draws={}
for name,c in [('before',before),('after',after)]:
    draws[name]={r[0]:tuple(r[1:]) for r in c.execute('SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws')}
changed=[i for i,v in draws['before'].items() if draws['after'].get(i)!=v]
r={'scope':'API34_EMULATOR_SMOKE_NOT_PHYSICAL_DEVICE','schemaTablesLost':missing,'integrity':integrity,'baselineDrawsLostOrChanged':changed,
   'beforeDrawCount':len(draws['before']),'afterDrawCount':len(draws['after']),'naturalQuality':'NOT_EVALUATED',
   'foreground60min':'NOT_RUN','screenOff6h':'NOT_RUN','continuous72h':'NOT_RUN'}
r['upgradeDatabase']='PASS' if not missing and not changed and integrity=='ok' else 'FAIL'
(p/'upgrade.json').write_text(json.dumps(r,indent=2)+'\n'); print(json.dumps(r))
before.close(); after.close()
assert r['upgradeDatabase']=='PASS',r
PY
adb uninstall "$PKG" > "$OUT/uninstall-for-clean-test.txt"
adb install "$APK" > "$OUT/install-clean.txt"
python3 scripts/probe_r13_startup.py --installed > "$OUT/clean-startup.log" 2>&1
adb logcat -d > "$OUT/logcat-clean.txt"
adb shell am force-stop "$PKG"
# Uploaded evidence is summary/logs only. Do not publish the baseline package or private application databases.
rm -f "$OUT"/*.db "$OUT"/*.db-wal "$OUT"/*.db-shm "$BASE"
echo 'API34_EMULATOR_UPGRADE_AND_CLEAN_STARTUP_PASS; LONG_TESTS_NOT_RUN' | tee "$OUT/result.txt"
