#!/usr/bin/env bash
set -euo pipefail
cd source
bash tools/r30-repair/prepare_emulator.sh
APK=$(find ../built/app/build/outputs/apk/release -maxdepth 1 -name "*.apk" -type f)
test -n "$APK"
export R31_REPORT_APK_SHA=${R31_REPORT_APK_SHA:-$(sha256sum "$APK" | cut -d " " -f1)}
adb install -r "$APK"
# Disposable overlay setup: avoid the first-install notification prompt stealing the report-menu tap.
adb shell pm grant com.starcore.research.r13 android.permission.POST_NOTIFICATIONS
mkdir -p report-only-public "$RUNNER_TEMP/private-report"
export R31_REPORT_PRIVATE="$RUNNER_TEMP/private-report"
python3 - <<'PY'
import hashlib,subprocess
import os
paths=list(__import__('pathlib').Path('../built/app/build/outputs/apk/release').glob('*.apk'));assert len(paths)==1,'ONE_RELEASE_REQUIRED';apk=paths[0]
assert hashlib.sha256(apk.read_bytes()).hexdigest()==os.environ['R31_REPORT_APK_SHA'],'EXPECTED_SIGNED_APK_SHA'
if os.environ.get('R31_REPORT_EXPECTED_SOURCE'):assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()==os.environ['R31_REPORT_EXPECTED_SOURCE'],'EXPECTED_SOURCE_COMMIT'
installed=subprocess.check_output(['adb','shell','pm','path','com.starcore.research.r13'],text=True).strip().split(':',1)[1]
actual=subprocess.check_output(['adb','exec-out','cat',installed])
assert hashlib.sha256(actual).hexdigest()==hashlib.sha256(apk.read_bytes()).hexdigest(),'INSTALLED_APK_MUST_BE_EXACT_RELEASE'
PY
set +e
python3 ../harness/evidence/r27/ci/r31-report-export-only.py --out "$R31_REPORT_PRIVATE" > "$R31_REPORT_PRIVATE/probe.log" 2>&1
probe_status=$?
set -e
python3 - "$probe_status" <<'PY'
import hashlib,json,os,pathlib,re,sys
private=pathlib.Path(os.environ['R31_REPORT_PRIVATE']);public=pathlib.Path('report-only-public')
source=__import__('subprocess').check_output(['git','rev-parse','HEAD'],text=True).strip()
r={'sourceCommit':source,'apkSha256':os.environ['R31_REPORT_APK_SHA'],'runId':os.environ['GITHUB_RUN_ID'],'scope':'REPORT_GENERATION_COMPLETENESS_REAL_UI_SAF_ONLY','probeExit':int(sys.argv[1]),'result':'REPORT_PASS_REINSTALL_PENDING' if sys.argv[1]=='0' else 'FAIL','physicalDevice':'NOT_RUN','businessAndAlgorithmHealth':'DEFERRED_NOT_PASSED','rawReportsAndCredentials':'PRIVATE_NOT_PUBLISHED'}
if os.environ.get('R31_DIAGNOSTIC_ONLY')=='1':r['scope']='REPORT_WRITER_SAMPLING_ONLY_NOT_ACCEPTANCE';r['result']='DIAGNOSTIC_ONLY_NOT_ACCEPTANCE'
r['harnessSha256']=hashlib.sha256(pathlib.Path('../harness/evidence/r27/ci/r31-report-export-only.py').read_bytes()).hexdigest()
for name in ['ui-report-acceptance.json','safe-recovery-failure.json','safe-report-semantics.json','safe-report-liveness.json','safe-timeout-source-frames.json']:
    p=private/name
    if p.exists():
        (public/name).write_bytes(p.read_bytes())
text=(private/'probe.log').read_text(errors='replace')
r['notificationPermission']='GRANTED_ON_DISPOSABLE_EMULATOR_TO_MATCH_OVERLAY_STATE'
r['publicUiTargets']=[label for label in ['导出完整报告（含数据库和训练附件）','清理旧报告并生成当前完整报告','一次保存完整ZIP文件'] if 'UI_TEXT_NOT_FOUND:'+label in text]
r['publicFailureLabels']=sorted(set(re.findall(r'AssertionError: ([A-Z][A-Z0-9_]{3,80})',text)))
r['exceptionClasses']=sorted(set(re.findall(r'^([A-Z][A-Za-z0-9]*(?:Error|Exception)):',text,re.M)))
(public/'REPORT_ONLY_RESULT.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(r,ensure_ascii=False))
PY
if [ "$probe_status" != 0 ]; then exit "$probe_status"; fi
# Reinstallation must preserve this generated report. This is an actual -r reinstall of the same signed bytes,
# not a claim of testing the user's unavailable phone or its 020 database.
adb install -r "$APK"
python3 - <<'PY'
import hashlib,json,subprocess,pathlib
public=pathlib.Path('report-only-public');r=json.loads((public/'ui-report-acceptance.json').read_text());job=r['jobId']
path=f'/data/user/0/com.starcore.research.r13/files/r981_reports/jobs/{job}/delivery/STARCORE_FULL_REPORT.zip'
body=subprocess.check_output(['adb','exec-out','cat',path]);assert hashlib.sha256(body).hexdigest()==r['zipSha256'],'OVERLAY_REINSTALL_REPORT_NOT_PRESERVED'
r['sameSignedApkReinstallPreservesReport']=True
r['phone020ToCurrentOverlay']='SAME_PACKAGE_CERT_AND_HIGHER_VERSION_VERIFIED_PHONE_NOT_RUN'
(public/'ui-report-acceptance.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n')
final=json.loads((public/'REPORT_ONLY_RESULT.json').read_text());final['result']='PASS';final['sameSignedApkReinstallPreservesReport']=True
(public/'REPORT_ONLY_RESULT.json').write_text(json.dumps(final,ensure_ascii=False,indent=2)+'\n')
print('REAL_REPORT_UI_AND_SAF_PASS_SAME_RELEASE_REINSTALL_PRESERVES_REPORT')
PY
