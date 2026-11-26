#!/usr/bin/env python3
"""Capture the exact on-device request and stop states before the emulator exits."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

serial = subprocess.check_output(['adb','get-serialno'],text=True).strip()
assert re.fullmatch('emulator-[0-9]+',serial)
output = Path(os.environ['RUNNER_TEMP'])/'resume-diagnostic'
output.mkdir()
source = Path.cwd()/'app-source'
root = '/data/user/0/com.starcore.research.r13/files/r981_reports'


def adb(*args, check=True):
    result = subprocess.run(['adb','-s',serial,*args],text=True,capture_output=True,timeout=30,check=check)
    return result.stdout.strip()


def observe():
    queued = adb('shell','ls','-t',root+'/requests',check=False).splitlines()
    pointer = adb('shell','cat',root+'/current-job.json',check=False)
    try: active = json.loads(pointer).get('jobId','')
    except json.JSONDecodeError: active = ''
    observed = {'jobId':active,'requests':len(queued)}
    if re.fullmatch('[0-9a-f]{64}',active):
        for file in ('job.json','stage-progress.json','backup-progress.json','inputs-complete.json'):
            data = adb('shell','cat',f'{root}/jobs/{active}/{file}',check=False)
            if data:
                try: observed[file] = json.loads(data)
                except json.JSONDecodeError: observed[file] = {'parse':'unavailable'}
    for ack in (file for file in queued[:16] if re.fullmatch('[0-9a-f]{64}\\.ack\\.json',file)):
        data = adb('shell','cat',f'{root}/requests/{ack}',check=False)
        if data:
            try:
                row = json.loads(data)
                observed['latestAck'] = {key:row.get(key) for key in (
                    'requestId','jobId','stage','mode','reason','resumeStage','failurePhaseR32','failureDetailR32')}
                break
            except json.JSONDecodeError: pass
    return observed


assert adb('shell','getprop','ro.kernel.qemu') == '1'
adb('root')
adb('wait-for-device')
adb('shell','wm','size','480x850')
adb('shell','wm','density','195')
adb('shell','settings','put','system','screen_off_timeout','7200000')
adb('shell','svc','power','stayon','true')
adb('install',os.environ['APK'])
command = [sys.executable,'-B',str(source/'tools/r343/run_report_resume_probe.py'),
           '--originals',str(output),'--build-receipt',os.environ['ORIGINAL_BUILD']]
last = None
trace = output/'stage-trace.jsonl'
with (output/'runner.log').open('w') as log, trace.open('w') as observations:
    process = subprocess.Popen(command,stdout=log,stderr=subprocess.STDOUT)
    try:
        while process.poll() is None:
            sample = observe()
            if sample != last:
                observations.write(json.dumps(sample,ensure_ascii=False,sort_keys=True)+'\n')
                observations.flush()
                last = sample
                ack = sample.get('latestAck', {})
                if ack.get('stage') == 'FAILED':
                    print('REPORT_DIAGNOSTIC_TERMINAL',json.dumps(ack,ensure_ascii=False),flush=True)
                    break
            time.sleep(3)
    finally:
        if process.poll() is None:
            process.terminate()
        process.wait(timeout=30)
        final = observe()
        (output/'final-observed.json').write_text(json.dumps(final,ensure_ascii=False,indent=2)+'\n')
        logcat = adb('logcat','-d','-v','brief','ReportExportCP12:V','ReportJournalCP12:V','*:S',check=False)
        (output/'report-logcat.log').write_text(logcat[-30000:]+'\n')
        shutil.copyfile(os.environ['ORIGINAL_BUILD'],output/'original-build-receipt.json')
        print('REPORT_DIAGNOSTIC_FINAL',json.dumps(final,ensure_ascii=False)[:7000],flush=True)
result = (output/'runner.log').read_text()
print(result[-3000:],flush=True)
raise SystemExit(process.returncode if process.returncode is not None else 1)
