#!/usr/bin/env bash
set -euo pipefail
# Emulator-runner executes each YAML line in a fresh shell; keep the failure trap
# and all real checks together in this process so a failing test retains logcat.
trap 'adb logcat -d > android-logcat.txt || true' EXIT
adb install -r delivered/app/build/outputs/apk/debug/*.apk
if [[ "${1:-29}" == "29" ]]; then
adb install -r delivered/app/build/outputs/apk/androidTest/debug/*.apk
python3 - <<'PYTEST'
import json,re,subprocess
from pathlib import Path
manifest=json.loads(Path('delivered/package-receipt/instrumentation-tests.json').read_text())
assert manifest['schema']=='R26_INSTRUMENTATION_MANIFEST_V1'
classes=manifest['classes'];assert classes and manifest['total']==sum(c['count'] for c in classes)
total=0
with open('android-r13.txt','w') as report:
    for item in classes:
        name=item['className']
        subprocess.run(['adb','shell','am','force-stop','com.starcore.research.r13'],check=True)
        run=subprocess.run(['adb','shell','am','instrument','-w','-e','class',name,'com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'],capture_output=True,text=True,timeout=600)
        report.write(run.stdout+run.stderr);report.flush();print(run.stdout,flush=True)
        match=re.search(r'OK \((\d+) tests?\)',run.stdout)
        assert run.returncode==0 and match and 'FAILURES' not in run.stdout,name+' failed'
        assert int(match.group(1))==item['count'],f'{name}: executed {match.group(1)} expected {item["count"]}'
        total+=int(match.group(1))
    assert total==manifest['total'],total
    report.write(f'Isolated class aggregate: OK ({total} tests)\n')
PYTEST
adb exec-out run-as com.starcore.research.r13 cat cache/su1-research.png > r13-research.png
adb exec-out run-as com.starcore.research.r13 cat cache/r13-charts.png > r13-charts.png
adb exec-out run-as com.starcore.research.r13 cat cache/r26-runtime-synthetic.json > r26-runtime-synthetic.json
fi
python3 scripts/probe_r13_startup.py
# The deliverable is the release variant. Confirm the same fixed certificate
# permits an in-place update, then exercise its actual launcher on every API.
adb install -r delivered/app/build/outputs/apk/release/*.apk
adb shell am force-stop com.starcore.research.r13
adb logcat -c
adb shell am start -W -n com.starcore.research.r13/com.ethk3.novaonepro.MainActivity > release-launcher.txt
python3 - <<'PYRELEASE'
from pathlib import Path
import subprocess,time
text=Path('release-launcher.txt').read_text()
assert 'Status: ok' in text,text
time.sleep(15)
pid=subprocess.check_output(['adb','shell','pidof','com.starcore.research.r13'],text=True).strip()
assert pid,'release process exited'
logs=subprocess.check_output(['adb','logcat','-d'],text=True)
Path('release-logcat.txt').write_text(logs)
assert 'FATAL EXCEPTION' not in logs and 'ANR in com.starcore.research.r13' not in logs
Path('release-runtime.txt').write_text('Release launcher and process survival PASS; package=com.starcore.research.r13; pid='+pid+'\n')
PYRELEASE
