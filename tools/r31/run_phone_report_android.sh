#!/usr/bin/env bash
set -euo pipefail
phone_built=$(realpath "${1:?built artifact directory}")
phone_public=$(realpath -m "${2:?public evidence directory}")
phone_old=$(realpath "${3:?025 artifact directory}")
phone_harness=${R31_PHONE_HARNESS_ROOT:-.}
mkdir -p "$phone_public"
bash tools/r30-repair/prepare_emulator.sh
phone_apk=$(find "$phone_built" -name '*-release.apk' -type f)
phone_old_apk=$(find "$phone_old" -name '*-release.apk' -type f)
test -n "$phone_apk" && test -n "$phone_old_apk"
test "$(cat "$phone_built/source-commit.txt")" = "$(git rev-parse HEAD)"
export R31_REPORT_APK_SHA=$(sha256sum "$phone_apk" | cut -d ' ' -f1)
adb install -r "$phone_old_apk"
adb shell mkdir -p /data/user/0/com.starcore.research.r13/files
adb shell 'echo PHONE_025_OVERLAY_MARKER > /data/user/0/com.starcore.research.r13/files/phone-025-overlay-marker'
adb install -r "$phone_apk"
test "$(adb shell cat /data/user/0/com.starcore.research.r13/files/phone-025-overlay-marker | tr -d '\r')" = PHONE_025_OVERLAY_MARKER
adb shell pm grant com.starcore.research.r13 android.permission.POST_NOTIFICATIONS
phone_private=$(mktemp -d "${RUNNER_TEMP:-/tmp}/phone-report-private.XXXXXX")
export R31_PHONE_PRIVATE="$phone_private" R31_PHONE_PUBLIC="$phone_public"
python3 - "$phone_apk" <<'PY'
import hashlib,os,subprocess,sys
from pathlib import Path
path=subprocess.check_output(['adb','shell','pm','path','com.starcore.research.r13'],text=True).strip().split(':',1)[1]
actual=subprocess.check_output(['adb','exec-out','cat',path])
assert hashlib.sha256(actual).hexdigest()==os.environ['R31_REPORT_APK_SHA'],'EXACT_RELEASE_NOT_INSTALLED'
PY
set +e
python3 -B "$phone_harness/tools/r31/phone_report_ui.py" --out "$phone_private" > "$phone_private/report-probe.log" 2>&1
phone_exit=$?
set -e
python3 - "$phone_exit" <<'PY'
import hashlib,json,os,re,subprocess,sys
from pathlib import Path
private=Path(os.environ['R31_PHONE_PRIVATE']);public=Path(os.environ['R31_PHONE_PUBLIC'])
result={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'apkSha256':os.environ['R31_REPORT_APK_SHA'],'runId':os.environ['GITHUB_RUN_ID'],'scope':'REPORT_GENERATION_COMPLETENESS_REAL_UI_SAF_ONLY','probeExit':int(sys.argv[1]),'result':'REPORT_PASS_REINSTALL_PENDING' if sys.argv[1]=='0' else 'FAIL','physicalDevice':'NOT_RUN','businessAndAlgorithmHealth':'DEFERRED_NOT_PASSED','rawReportsAndCredentials':'PRIVATE_NOT_PUBLISHED','overlayMarkerPreserved':True,'overlayFromVersionCode':577311025}
result['validationSourceCommit']=os.environ.get('R31_PHONE_VALIDATION_SOURCE',result['sourceCommit'])
result['startupWarmupSeconds']=int(os.environ.get('R31_REPORT_STARTUP_WAIT_SECONDS','0'))
for name in ['ui-report-acceptance.json','safe-recovery-failure.json','safe-report-semantics.json','safe-report-liveness.json','safe-timeout-source-frames.json']:
    path=private/name
    if path.is_file():(public/name).write_bytes(path.read_bytes())
text=(private/'report-probe.log').read_text(errors='replace')
result['publicFailureLabels']=sorted(set(re.findall(r'AssertionError: ([A-Z][A-Z0-9_]{3,80})',text)))
result['missingUiTargets']=[label for label in ['导出完整报告（含数据库和训练附件）','清理旧报告并生成当前完整报告','一次保存完整ZIP文件'] if 'UI_TEXT_NOT_FOUND:'+label in text]
(public/'REPORT_ONLY_RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False))
PY
test "$phone_exit" = 0
adb install -r "$phone_apk"
python3 - <<'PY'
import hashlib,json,os,subprocess
from pathlib import Path
public=Path(os.environ['R31_PHONE_PUBLIC']);r=json.loads((public/'ui-report-acceptance.json').read_text());job=r['jobId']
body=subprocess.check_output(['adb','exec-out','cat',f'/data/user/0/com.starcore.research.r13/files/r981_reports/jobs/{job}/delivery/STARCORE_FULL_REPORT.zip'])
assert hashlib.sha256(body).hexdigest()==r['zipSha256'],'SIGNED_REINSTALL_LOST_REPORT'
r['sameSignedApkReinstallPreservesReport']=True;r['overlayMarkerPreserved']=True;r['overlayFromVersionCode']=577311025
(public/'ui-report-acceptance.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n')
PY
set +e
python3 -B "$phone_harness/tools/r31/phone_report_failure_ui.py" --out "$phone_private" > "$phone_private/failure-ui.log" 2>&1
phone_failure_exit=$?
set -e
python3 - "$phone_failure_exit" <<'PY'
import json,os,re,sys
from pathlib import Path
private=Path(os.environ['R31_PHONE_PRIVATE']);public=Path(os.environ['R31_PHONE_PUBLIC'])
text=(private/'failure-ui.log').read_text(errors='replace')
labels=sorted(set(re.findall(r'AssertionError: ([A-Z][A-Z0-9_]{3,80})',text)))
print(json.dumps({'failureUiExit':int(sys.argv[1]),'publicFailureLabels':labels}))
receipt=private/'failure-ui-acceptance.json'
if receipt.is_file():(public/receipt.name).write_bytes(receipt.read_bytes())
result=json.loads((public/'REPORT_ONLY_RESULT.json').read_text());result['failureUiExit']=int(sys.argv[1]);result['sameSignedApkReinstallPreservesReport']=True
result['result']='PASS' if sys.argv[1]=='0' else 'FAIL'
(public/'REPORT_ONLY_RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
PY
test "$phone_failure_exit" = 0
echo PHONE_REPORT_RELEASE_ANDROID14_UI_SAF_AND_ACK_HISTORY_PASS
