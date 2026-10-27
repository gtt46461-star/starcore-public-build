#!/usr/bin/env bash
set -euo pipefail
screen_case=${1:?BRIGHT or LOCKED required}
[[ "$screen_case" == BRIGHT || "$screen_case" == LOCKED ]]
collect() {
  adb logcat -d > android-logcat.txt || true
  adb exec-out run-as com.starcore.research.r13 cat cache/r26-runtime-synthetic.json > r26-runtime-synthetic.json 2>/dev/null || true
  adb shell dumpsys battery reset || true
}
trap collect EXIT
python3 - <<'PY'
from pathlib import Path
import hashlib,json
m=json.loads(Path('delivered/package-receipt/instrumentation-tests.json').read_text())
c=next(x for x in m['classes'] if x['className'].endswith('.R26RuntimeDeviceTest'))
assert hashlib.sha256(Path(c['sourcePath']).read_bytes()).hexdigest()==c['sourceSha256'],'instrumentation source/artifact mismatch'
PY
adb install -r delivered/app/build/outputs/apk/debug/*.apk
adb install -r delivered/app/build/outputs/apk/androidTest/debug/*.apk
adb shell am force-stop com.starcore.research.r13
adb shell dumpsys battery unplug
adb shell settings put system screen_off_timeout 7200000
if [[ "$screen_case" == BRIGHT ]]; then
  adb shell input keyevent KEYCODE_WAKEUP
else
  adb shell input keyevent KEYCODE_SLEEP
fi
python3 - "$screen_case" <<'PY'
from pathlib import Path
import json,re,subprocess,sys
screen=sys.argv[1]
args=['adb','shell','am','instrument','-w','-e','class','com.ethk3.novaonepro.strategyv2.R26RuntimeDeviceTest','-e','r26EnduranceSeconds','3600','-e','r26EnduranceScreen',screen,'com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner']
run=subprocess.run(args,capture_output=True,text=True,timeout=5000)
Path('android-r13.txt').write_text(run.stdout+run.stderr)
with open('r26-owner-logcat.txt','w') as trace:
    subprocess.run(['adb','logcat','-d','-s','STARCORE_R26:V','TestRunner:V','AndroidRuntime:E','*:S'],stdout=trace,stderr=subprocess.STDOUT,check=False)
print(run.stdout,flush=True)
assert run.returncode==0 and re.search(r'OK \(1 test\)',run.stdout) and 'FAILURES' not in run.stdout,'60-minute engineering scenario failed'
receipt=subprocess.check_output(['adb','exec-out','run-as','com.starcore.research.r13','cat','cache/r26-runtime-synthetic.json'])
Path('r26-runtime-synthetic.json').write_bytes(receipt)
r=json.loads(receipt)
assert r['result']=='PASS_WITH_STATED_ENGINEERING_SCOPE' and r['endurance']['elapsedMs']>=3600000
assert r['endurance']['screen']==screen and len(r['endurance']['samples'])>=59
assert r['familyMechanismsWithCommittedProgress']==24 and r['expertsWithActualReceipts']==16 and r['restartResumed']
print('R26_ENDURANCE_ENGINEERING_PASS; physical user phone and natural quality are separate evidence')
PY
