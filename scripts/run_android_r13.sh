#!/usr/bin/env bash
set -euo pipefail
# Emulator-runner executes each YAML line in a fresh shell; keep the failure trap
# and all real checks together in this process so a failing test retains logcat.
trap 'adb logcat -d > android-logcat.txt || true' EXIT
adb install -r delivered/app/build/outputs/apk/debug/*.apk
if [[ "${1:-29}" == "29" ]]; then
adb install -r delivered/app/build/outputs/apk/androidTest/debug/*.apk
python3 - <<'PYTEST'
import re, subprocess
classes='ResearchUiDeviceSU1Test SchemaAndLiveSU1Test ProgramTreeDeviceSU1Test AtomKernelDeviceSU1Test TrainingParityDeviceSU1Test BundledApiDeviceSU1Test RelGraphDeviceSU1Test ResearchSchedulingDeviceSU1Test SchemaAndChartsR13Test StartupLeaseR13Test Plan1025DeviceTest R26DeviceTest'.split()
total=0
with open('android-r13.txt','w') as report:
    for name in classes:
        subprocess.run(['adb','shell','am','force-stop','com.starcore.research.r13'],check=True)
        run=subprocess.run(['adb','shell','am','instrument','-w','-e','class','com.ethk3.novaonepro.strategyv2.'+name,'com.starcore.research.r13.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'],capture_output=True,text=True,timeout=600)
        report.write(run.stdout+run.stderr);report.flush();print(run.stdout,flush=True)
        match=re.search(r'OK \((\d+) tests?\)',run.stdout)
        assert run.returncode==0 and match and 'FAILURES' not in run.stdout,name+' failed'
        total+=int(match.group(1))
    assert total==36,total
    report.write('Isolated class aggregate: OK (36 tests)\n')
PYTEST
adb exec-out run-as com.starcore.research.r13 cat cache/su1-research.png > r13-research.png
adb exec-out run-as com.starcore.research.r13 cat cache/r13-charts.png > r13-charts.png
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
