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
VARIANT=${R29_VARIANT:-debug}
[[ "$VARIANT" == debug || "$VARIANT" == release ]] || exit 1
APK=$(find "$DELIVERED" -maxdepth 1 -name "*-$VARIANT.apk" -print -quit)
[ -n "$APK" ]
PROBE=(--installed)
if [ "$VARIANT" = release ]; then PROBE+=(--emulator-root-prefs); fi
python3 scripts/wait_r13_android_ready.py
adb root > "$OUT/root.txt"; adb wait-for-device
BASE_URL=${R29_BASELINE_URL:-https://github.com/gtt46461-star/starcore-public-build/releases/download/r26-20261028-runtime-117/STARCORE-R26-20261028-117-release.apk}
BASE_HASH=${R29_BASELINE_SHA256:-739e6b13c692d11ed1cbc7c09f5f95d904328cefc1bf9d6616a9e5266203d433}
curl -fLsS --connect-timeout 15 --max-time 120 -o "$BASE" "$BASE_URL"
echo "$BASE_HASH  $BASE" | sha256sum -c -
"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$BASE" > "$OUT/baseline-signature.txt"
grep -F "${EXPECTED_CERT:-9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca}" "$OUT/baseline-signature.txt"
python3 - "$DELIVERED/delivery.json" "$BASE" "$APK" "$OUT/package-manifests.json" <<'PY'
import json,os,re,subprocess,sys
from pathlib import Path
rows=json.loads(Path(sys.argv[1]).read_text()); target=next(r for r in rows if r['apk']==Path(sys.argv[3]).name) if isinstance(rows,list) else rows
manifest=[]
for path in sys.argv[2:4]:
    text=subprocess.check_output([os.environ['ANDROID_HOME']+'/build-tools/36.0.0/aapt','dump','badging',path],text=True)
    line=next(x for x in text.splitlines() if x.startswith('package:'))
    values=dict(re.findall(r"(\w+)='([^']*)'",line)); assert values['name']=='com.starcore.research.r13'
    manifest.append({'package':values['name'],'versionCode':int(values['versionCode']),'versionName':values['versionName']})
assert manifest[1]['versionCode']==target['versionCode'] and manifest[1]['versionName']==target['versionName']
assert manifest[0]['versionCode']<manifest[1]['versionCode'], 'BASELINE_MUST_PRECEDE_DELIVERED_VERSION'
if os.environ.get('R29_BASELINE_VERSION'): assert manifest[0]['versionCode']==int(os.environ['R29_BASELINE_VERSION'])
Path(sys.argv[4]).write_text(json.dumps({'baseline':manifest[0],'delivered':manifest[1]},indent=2)+'\n')
PY
pull_database() {
    local remote=$1 local_name=$2
    adb pull "/data/user/0/$PKG/databases/$remote" "$OUT/$local_name.db" > /dev/null || return $?
    for suffix in -wal -shm; do adb pull "/data/user/0/$PKG/databases/$remote$suffix" "$OUT/$local_name.db$suffix" > /dev/null 2>&1 || true; done
}
pull_runtime() {
    local phase=$1 main_name=$2 baseline_name stage_status=0
    pull_database binance_k3_nova_one_pro_v57.db "$main_name"
    pull_database starcore-research-r29-stage.db "stage-$phase" || stage_status=$?
    # Aggregate failure context is durable before missing/unpublished RP causes the required hard failure.
    python3 tools/r29/inspect_runtime.py --observe-only --main "$OUT/$main_name.db" --stage "$OUT/stage-$phase.db" --out "$OUT/$phase-migration-observation.json"
    if [ "$stage_status" -ne 0 ]; then return "$stage_status"; fi
    pull_database starcore-r29-research.db "research-$phase"
    baseline_name=$(python3 - "$OUT/$main_name.db" <<'PY'
import re,sqlite3,sys
with sqlite3.connect(sys.argv[1]) as db:
    row=db.execute("SELECT baseline_name FROM r29_migration_publication WHERE singleton=1 AND phase='PUBLISHED'").fetchone()
    assert row and re.fullmatch(r'starcore-r29-baseline-[0-9]+\.db',row[0]), 'PUBLISHED_SEALED_BASELINE_REQUIRED'
    print(row[0])
PY
)
    pull_database "$baseline_name" "sealed-$phase"
    python3 tools/r29/inspect_runtime.py --main "$OUT/$main_name.db" --stage "$OUT/stage-$phase.db" --research "$OUT/research-$phase.db" --baseline "$OUT/sealed-$phase.db" --out "$OUT/$phase-protocol.json"
}
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
python3 scripts/probe_r13_startup.py "${PROBE[@]}" > "$OUT/upgrade-startup.log" 2>&1
mv startup-evidence "$OUT/startup-upgrade"
adb shell am force-stop "$PKG"
pull_runtime upgrade after
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
python3 scripts/probe_r13_startup.py "${PROBE[@]}" > "$OUT/clean-startup.log" 2>&1
mv startup-evidence "$OUT/startup-clean"
adb logcat -d > "$OUT/logcat-clean.txt"
adb shell am force-stop "$PKG"
pull_runtime clean clean
# Uploaded evidence is summary/logs only. Do not publish the baseline package or private application databases.
rm -f "$OUT"/*.db "$OUT"/*.db-wal "$OUT"/*.db-shm "$BASE"
echo 'API34_EMULATOR_UPGRADE_AND_CLEAN_STARTUP_PASS; LONG_TESTS_NOT_RUN' | tee "$OUT/result.txt"
