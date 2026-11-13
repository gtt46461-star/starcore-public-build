#!/usr/bin/env bash
set -euo pipefail
phone_source=$(realpath "${1:?source checkout}")
phone_harness=$(realpath "${2:?validation checkout}")
phone_built=$(realpath "${3:?signed build artifact}")
phone_accepted=$(realpath "${4:?accepted generation and SAF receipt artifact}")
cd "$phone_source"
bash tools/r30-repair/prepare_emulator.sh
phone_apk=$(find "$phone_built" -name '*-release.apk' -type f)
test "$(sha256sum "$phone_apk" | cut -d ' ' -f1)" = 9a3dabe36dcacc4e726ef85ca62dc95ace9fa688125ae6b22a2cf34ddf14c0b8
test "$(cat "$phone_built/source-commit.txt")" = "$(git rev-parse HEAD)"
adb install -r "$phone_apk"
adb shell pm grant com.starcore.research.r13 android.permission.POST_NOTIFICATIONS
phone_private=$(mktemp -d "${RUNNER_TEMP:-/tmp}/phone-failure-ui.XXXXXX")
mkdir -p report-only-public
export R31_PHONE_PRIVATE="$phone_private" R31_PHONE_ACCEPTED="$phone_accepted"
export R31_PHONE_VALIDATION_SOURCE=$(git -C "$phone_harness" rev-parse HEAD)
set +e
python3 -B "$phone_harness/tools/r31/phone_report_failure_ui.py" --out "$phone_private" > "$phone_private/failure-ui.log" 2>&1
phone_failure_exit=$?
set -e
python3 - "$phone_failure_exit" <<'PY'
import json,os,re,shutil,subprocess,sys
from pathlib import Path
private=Path(os.environ['R31_PHONE_PRIVATE']);accepted=Path(os.environ['R31_PHONE_ACCEPTED']);public=Path('report-only-public')
source=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
original=json.loads((accepted/'REPORT_ONLY_RESULT.json').read_text())
ui=json.loads((accepted/'ui-report-acceptance.json').read_text())
assert original['sourceCommit']==ui['sourceCommit']==source
assert original['probeExit']==0 and original['sameSignedApkReinstallPreservesReport'] is True
assert ui['result']=='PASS' and ui['saf']['state']=='VERIFIED_READBACK'
assert original['apkSha256']=='9a3dabe36dcacc4e726ef85ca62dc95ace9fa688125ae6b22a2cf34ddf14c0b8'
text=(private/'failure-ui.log').read_text(errors='replace')
for line in text.splitlines():
    if line.startswith(('PHONE_FAILURE_UI_STEP:','PHONE_FAILURE_UI_MISSING_TARGET:')):print(line)
labels=sorted(set(re.findall(r'AssertionError: ([A-Z][A-Z0-9_]{3,80})',text)))
result=dict(original)
result.update(runId=os.environ['GITHUB_RUN_ID'],result='PASS' if sys.argv[1]=='0' else 'FAIL',failureUiExit=int(sys.argv[1]),
              validationSourceCommit=os.environ['R31_PHONE_VALIDATION_SOURCE'],reportGenerationRunId=43455367451,
              failureUiRunId=int(os.environ['GITHUB_RUN_ID']),publicFailureLabels=labels,
              retainedPreviousFailureUiResult=original,
              executionComposition='SAME_IMMUTABLE_SIGNED_APK; ACCEPTED_PRIOR_GENERATION_SAF_AND_OVERLAY; ONLY_FAILURE_MENU_UI_REEXECUTED')
for name in ['ui-report-acceptance.json','safe-report-semantics.json']:shutil.copyfile(accepted/name,public/name)
failure=private/'failure-ui-acceptance.json'
if failure.is_file():
    detail=json.loads(failure.read_text());assert detail['sourceCommit']==source and detail['result']=='PASS'
    detail.update(apkSha256=original['apkSha256'],runId=os.environ['GITHUB_RUN_ID'],validationSourceCommit=os.environ['R31_PHONE_VALIDATION_SOURCE'])
    (public/failure.name).write_text(json.dumps(detail,ensure_ascii=False,indent=2)+'\n')
(public/'REPORT_ONLY_RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({'result':result['result'],'failureUiExit':result['failureUiExit'],'publicFailureLabels':labels,'sourceCommit':source}))
PY
test "$phone_failure_exit" = 0
