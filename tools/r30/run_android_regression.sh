#!/usr/bin/env bash
# Final batch regression on the installed candidate and test APK. Every fixture is isolated at process startup.
set -euo pipefail
out=${1:?evidence directory required}
mkdir -p "$out"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
app_id=com.starcore.research.r13
runner="$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner"
run_id=${GITHUB_RUN_ID:-local-$(date +%s)}
: > "$out/cases.tsv"
run_case() {
  local id=$1 selected=$2 expected=$3
  shift 3
  adb shell am force-stop "$app_id"
  adb logcat -c
  adb shell am instrument -w -r -e class "$selected" -e r30Sandbox "reg-$run_id-$id" "$@" "$runner" > "$out/$id.txt" 2>&1
  adb logcat -d -v threadtime > "$out/$id-logcat.txt"
  grep -Fq "OK ($expected test" "$out/$id.txt" || { echo "ANDROID_REGRESSION_FAILED:$id"; exit 1; }
  printf '%s\t%s\t%s\n' "$id" "$selected" "$expected" >> "$out/cases.tsv"
  echo "R30_ANDROID_REGRESSION:$id:PASS"
}
run_case snapshot com.ethk3.novaonepro.R30ReportSnapshotAndroidTest 2
run_case retention com.ethk3.novaonepro.R30ResearchRetentionAndroidTest 1
run_case wait com.ethk3.novaonepro.R30WriteQueueAndroidTest 2
run_case revocation 'com.ethk3.novaonepro.R30DecisionChainAndroidTest#correctionRevokesAndNextAuthorityReadRejectsWithinFiveSeconds' 1
for mode in positive expired missing-freeze existing-real user-off; do
  run_case "decision-$mode" 'com.ethk3.novaonepro.R30DecisionChainAndroidTest#originalOwnerPositiveAndIndependentNegativeAuthorities' 1 -e r30DecisionMode "$mode"
done
run_case charts com.ethk3.novaonepro.R30ChartsAndroidTest 1
adb pull "/sdcard/Android/data/$app_id/files/r30-chart-regression" "$out/screenshots" > "$out/chart-screenshot-pull.txt" 2>&1
python3 - "$out" <<'PY'
import hashlib,json,pathlib,subprocess,sys
out=pathlib.Path(sys.argv[1]); images=sorted((out/'screenshots').rglob('*.png'));assert len(images)==48, len(images)
expected={f'{state}-{i:02d}.png' for state in ('READY','EMPTY','READ_ERROR') for i in range(1,17)};assert {p.name for p in images}==expected
rows=[r.split('\t') for r in (out/'cases.tsv').read_text().splitlines()];assert len(rows)==10 and sum(int(r[2]) for r in rows)==12
receipt={'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'environment':'ANDROID_EMULATOR','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','junitCases':sum(int(r[2]) for r in rows),'processes':len(rows),'cases':rows,'screenshots':[{'file':str(p.relative_to(out)),'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size} for p in images],'fixtureIsolation':'original Application routed before sole coordinator opens; never reset its singleton','originalProductDataDeleted':False}
(out/'regression.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
print('R30_ANDROID_REGRESSION|12_TESTS_10_INDEPENDENT_PROCESSES|48_ACTUAL_ACTIVITY_SCREENSHOTS|NOT_PHYSICAL_OR_NATURAL|PASS')
PY
