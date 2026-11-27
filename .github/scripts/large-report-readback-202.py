"""Measure a real signed Android report against a field-sized synthetic freelist."""
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import struct
import subprocess
import sys
import time

source = Path('source').resolve()
sys.path.insert(0, str(source/'tools/r342'))
import ci_candidate_tests as ci
import report_ui_driver as ui

out = Path(os.environ['RUNNER_TEMP'])/'large-report-evidence'
out.mkdir()
serial = ui.DEVICE
pkg = ci.PACKAGE
tmp = Path(os.environ['RUNNER_TEMP'])
baseline = tmp/'installed-201.apk'
candidate = tmp/'candidate-202.apk'
assert serial.startswith('emulator-') and ci.adb(serial,'shell','getprop','ro.kernel.qemu') == '1'
old_sha = 'a941717eb18ed99c9854291bb5c55ba8cf6ab5bff3534e1f44388cd58e040fa5'
old_source = '752846983da564d5dbf9dc5c3021f4625a07a68e'
new_source = '36f075222c8a8f7970a9dd04aefdda9379b11f1a'
assert ci.sha(baseline)==old_sha
assert 'Success' in ci.adb(serial,'install',baseline)
before=ci.installed(serial,out/'before.txt')
assert before['versionCode']==577311201 and before['apkSha256']==old_sha
capture=ci.signed_report_campaign(serial,out,old_sha,old_sha)
capture('baseline','COMPACT_DIAGNOSTIC',old_source)
ci.adb(serial,'shell','am','force-stop',pkg)
db = json.loads((source/'release/release_manifest.json').read_text())['databaseName']
assert '/' not in db
live = f'/data/user/0/{pkg}/databases/{db}'
local = tmp/db
identity = ci.adb(serial,'shell','stat','-c','%u:%g',live)
assert identity.replace(':','').isdigit()
for suffix in ['', '-wal', '-shm']:
    if subprocess.run(['adb','-s',serial,'shell','test','-f',live+suffix]).returncode==0:
        ci.adb(serial,'pull',live+suffix,str(local)+suffix)
with sqlite3.connect(local) as c:
    c.execute('PRAGMA wal_checkpoint(TRUNCATE)').fetchall()
    c.execute('PRAGMA journal_mode=DELETE').fetchall()
    c.execute('VACUUM')
    assert c.execute('PRAGMA freelist_count').fetchone()==(0,)
    used=c.execute('PRAGMA page_count').fetchone()[0]
    page=c.execute('PRAGMA page_size').fetchone()[0]
assert page==4096 and used < 100000
size=4778991616
pages=size//page
skip=0x40000000//page+1
with local.open('r+b') as f:
    f.truncate(size)
    f.seek(28)
    f.write(struct.pack('>III',pages,used+1,pages-used-1))
    start=used+1
    while start<=pages:
        end=min(pages+1,start+1+page//4-8)
        leaves=[i for i in range(start+1,end) if i!=skip]
        if end==skip:end+=1
        f.seek((start-1)*page)
        f.write(struct.pack('>'+str(len(leaves)+2)+'I',end if end<=pages else 0,len(leaves),*leaves))
        start=end
with sqlite3.connect(f'file:{local}?mode=ro',uri=True) as c:
    assert c.execute('PRAGMA quick_check').fetchall()==[('ok',)]
    assert c.execute('PRAGMA page_count').fetchone()==(pages,)
# This is the stopped, disposable, offline emulator seeded by the baseline APK.
assert ci.adb(serial,'shell','getprop','ro.kernel.qemu')=='1'
ci.adb(serial,'shell','rm','-f',live+'-wal',live+'-shm')
ci.adb(serial,'push','-z','brotli',local,live)
ci.adb(serial,'shell','chown',identity,live)
ci.adb(serial,'shell','restorecon',live)
assert int(ci.adb(serial,'shell','stat','-c','%s',live))==size
assert 'Success' in ci.adb(serial,'install','-r',candidate)
after=ci.installed(serial,out/'after.txt')
assert after=={'versionCode':577311202,'uid':before['uid'],'apkSha256':ci.sha(candidate)}
capture=ci.signed_report_campaign(serial,out,ci.sha(candidate),old_sha)
start=time.monotonic()
capture('candidate-sized','COMPACT_DIAGNOSTIC',new_source)
elapsed=time.monotonic()-start
result=json.loads((out/'signed-report-readbacks.json').read_text())['reports'][-1]
assert result['status']=='VERIFIED_ANDROID_SAF_ZIP' and result['sourceCommit']==new_source
receipt={'scope':'API34_EMULATOR_UPGRADE_201_TO_202_WITH_SYNTHETIC_FREE_PAGES','source':new_source,'before':before,'after':after,'sourceBytesBeforeLaunch':size,'populatedPagesBeforePadding':used,'secondsFromUiEntryThroughReopenAndEvidenceEncryption':round(elapsed,2),'underTwentyMinutes':elapsed<1200,'checks':result['checks'],'metrics':result['metrics'],'jobId':result['jobId'],'physicalDevice':False}
(out/'field-size-android-readback.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt),flush=True)
