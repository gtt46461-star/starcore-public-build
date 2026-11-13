#!/usr/bin/env bash
set -euo pipefail
out=${1:?evidence directory required}
mkdir -p "$out"
serial=$(adb get-serialno)
[[ "$serial" == emulator-* ]] && [[ $(adb shell getprop ro.kernel.qemu | tr -d '\r') == 1 ]] || { echo EMULATOR_REQUIRED; exit 1; }
# Collect independent gallery/storage regressions even when the production UI
# export fails. Preserve its real exit and fail the overall script at the end;
# never turn a partial gallery PASS into a successful UI export receipt.
mkdir -p "$out/ui-report"
ui_report_exit=0
python3 tools/r30-repair/probe_ui_report.py --out "$out/ui-report" > "$out/ui-report/probe-output.txt" 2>&1 || ui_report_exit=$?
echo "R31_ORIGINAL_SUBSTEP:production-ui-report:exit=$ui_report_exit"
# Keep all prior non-gallery Android scenarios, with the expanded old gallery count checked below.
baseline_copy=$(mktemp)
trap 'rm -f "$baseline_copy"' EXIT
python3 - "$baseline_copy" <<'PY'
import pathlib,sys
source=pathlib.Path('tools/r30/run_android_regression.sh').read_text()
source=source.replace('len(images)==48','len(images)==116').replace("range(1,17)","range(0,29)").replace("('READY','EMPTY','READ_ERROR')","('READY','EMPTY','INSUFFICIENT','READ_ERROR')").replace('48_ACTUAL_ACTIVITY_SCREENSHOTS','116_ACTUAL_ACTIVITY_SCREENSHOTS')
pathlib.Path(sys.argv[1]).write_text(source)
PY
bash "$baseline_copy" "$out/original"
app_id=com.starcore.research.r13
adb shell am instrument -w -r -e class com.ethk3.novaonepro.SchemaStampReasonAndroidTest -e r30Sandbox "stamp-${GITHUB_RUN_ID:-local}" "$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner" > "$out/schema-stamp-reason.txt" 2>&1
grep -Fq 'OK (3 tests' "$out/schema-stamp-reason.txt" || { cat "$out/schema-stamp-reason.txt"; exit 1; }
python3 - "$out" <<'PY'
import hashlib,json,pathlib,subprocess,sys
p=pathlib.Path(sys.argv[1]);raw=(p/'schema-stamp-reason.txt').read_bytes()
receipt={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'result':'PASS','androidTests':3,'outputSha256':hashlib.sha256(raw).hexdigest(),'scope':'Android SQLite report schema fixture; live and readonly snapshot after guarded restamp; unknown drift rejected; not the user 9.66GB database'}
(p/'schema-stamp-acceptance.json').write_text(json.dumps(receipt,indent=2)+'\n')
PY
adb shell am force-stop "$app_id"
adb logcat -c
chart_scope="repair-chart-${GITHUB_RUN_ID:-local}"
# The existing signed test transport predates the R31 feedback dependency.
# Run its actual additive R26 -> R27 migration owner in this same disposable
# namespace before the unchanged 18-view assertions. No graph rows or READY
# statuses are supplied by this prerequisite.
adb shell am instrument -w -r -e class com.ethk3.novaonepro.R31RuntimeProtocolInstrumentedTest#migrationIsAdditiveAndIdempotent -e r30Sandbox "$chart_scope" "$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner" > "$out/workbench-schema-owner.txt" 2>&1
grep -Fq 'OK (1 test' "$out/workbench-schema-owner.txt" || { cat "$out/workbench-schema-owner.txt"; exit 1; }
adb shell am instrument -w -r -e class com.ethk3.novaonepro.R30RepairChartsAndroidTest -e r30Sandbox "$chart_scope" "$app_id.test/com.ethk3.novaonepro.R30FaultAndroidRunner" > "$out/workbench.txt" 2>&1
adb logcat -d -v threadtime > "$out/workbench-logcat.txt"
pull_status=0
adb pull "/sdcard/Android/data/$app_id/files/r30-repair-chart-regression" "$out/workbench-screenshots" > "$out/workbench-pull.txt" 2>&1 || pull_status=$?
grep -Fq 'OK (1 test' "$out/workbench.txt" || { cat "$out/workbench.txt"; exit 1; }
[[ $pull_status == 0 ]] || { cat "$out/workbench-pull.txt"; exit 1; }
python3 - "$out" "$ui_report_exit" <<'PY'
import pathlib,json,hashlib,sys,subprocess
root=pathlib.Path(sys.argv[1]);images=sorted((root/'workbench-screenshots').rglob('*.png'));assert len(images)==72,len(images)
expected={f'{state}-R{i:02d}.png' for state in ('READY','EMPTY','INSUFFICIENT','READ_ERROR') for i in range(1,19)};assert {p.name for p in images}==expected
states=list((root/'workbench-screenshots').rglob('states.json'));assert len(states)==1
record={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'environment':'ANDROID_EMULATOR','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN','registeredObjects':47,'displayScenarios':4,'screenshots':188,'originalObjects':29,'researchViews':18,'researchActualStates':json.loads(states[0].read_text()),'researchMissingSourcePolicy':'Original source missing remains INSUFFICIENT; gallery does not prove effectiveness','artifacts':[{'file':str(p.relative_to(root)),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in images]}
(root/'repair-chart-acceptance.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n')
if int(sys.argv[2])==0:
 print('R30_REPAIR_ANDROID|47_REGISTERED_OBJECTS_4_DISPLAY_SCENARIOS_188_SCREENSHOTS|ORIGINAL_17_PLUS_REPAIR_1_TESTS|MISSING_EVIDENCE_EXPLICIT|NOT_PHYSICAL_OR_NATURAL|PASS')
else:
 print('R31_GALLERY_ONLY|47_OBJECTS_188_SCREENSHOTS|PASS_GALLERY_ONLY_PRODUCTION_UI_EXPORT_FAILED_SEPARATELY')
PY
if [[ "$ui_report_exit" != 0 ]]; then echo "R31_ORIGINAL_REGRESSION_FAILED:production-ui-report:exit=$ui_report_exit"; exit "$ui_report_exit"; fi
