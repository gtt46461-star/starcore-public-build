"""Measure a real signed Android report against a field-sized synthetic freelist."""
from contextlib import closing
import hashlib
import json
import os
from pathlib import Path
import sqlite3
import struct
import subprocess
import sys
import time
import zipfile

source = Path('source').resolve()
sys.path.insert(0, str(source/'tools/r342'))
import ci_candidate_tests as ci
import report_ui_driver as ui

out = Path(os.environ['RUNNER_TEMP'])/'large-report-evidence'
out.mkdir()
serial = ui.DEVICE
pkg = ci.PACKAGE
tmp = Path(os.environ['RUNNER_TEMP'])
baseline = tmp/'installed-202.apk'
candidate = tmp/'candidate-203.apk'
assert serial.startswith('emulator-') and ci.adb(serial,'shell','getprop','ro.kernel.qemu') == '1'
old_sha = 'b20dabb1d5e2f05df00f6ff2bfce519fcf18f76ffe290f02ab1683bc8fad30ee'
old_source = '36f075222c8a8f7970a9dd04aefdda9379b11f1a'
new_source = '6ca140d4f41dd079c4828ac128f6396163adb0fd'
assert ci.sha(baseline)==old_sha
assert 'Success' in ci.adb(serial,'install',baseline)
before=ci.installed(serial,out/'before.txt')
assert before['versionCode']==577311202 and before['apkSha256']==old_sha
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
sys.path.insert(0,str(source/'verification/r343/corrections/B12-report-indexing-203'))
from populated_fixture import check_archive
field=source/'construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip'
archive=tmp/'faults.tar.gz'
# The fixture commit and all sqlite handles must close before journal-mode changes.
populate_script = "import sys;sys.path.insert(0,sys.argv[1]);from populated_fixture import populate;assert populate(*sys.argv[2:])==11396"
subprocess.run([sys.executable,"-B","-c",populate_script,sys.path[0],local,field,archive],check=True)
count=11396
ci.adb(serial,'push',archive,'/data/local/tmp/index-fixture.tar.gz')
data='/data/user/0/'+pkg+'/files'
ci.adb(serial,'shell','tar','-xzf','/data/local/tmp/index-fixture.tar.gz','-C',data)
ci.adb(serial,'shell','chown','-R',identity,data+'/r335-fault-originals')
ci.adb(serial,'shell','restorecon','-R',data+'/r335-fault-originals')
with closing(sqlite3.connect(local)) as c:
    c.execute('PRAGMA wal_checkpoint(TRUNCATE)').fetchall()
    c.execute('PRAGMA journal_mode=DELETE').fetchall()
    c.execute('VACUUM')
    assert c.execute('PRAGMA freelist_count').fetchone()==(0,)
    used=c.execute('PRAGMA page_count').fetchone()[0]
    page=c.execute('PRAGMA page_size').fetchone()[0]
assert page==4096 and used < 100000
size=4835307520
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
with closing(sqlite3.connect(f'file:{local}?mode=ro',uri=True)) as c:
    assert c.execute('PRAGMA quick_check').fetchall()==[('ok',)]
    assert c.execute('PRAGMA page_count').fetchone()==(pages,)
# This is the stopped, disposable, offline emulator seeded by the baseline APK.
assert ci.adb(serial,'shell','getprop','ro.kernel.qemu')=='1'
ci.adb(serial,'shell','rm','-f',live+'-wal',live+'-shm')
subprocess.run(['adb','-s',serial,'push','-z','lz4',str(local),live],check=True,timeout=600)
ci.adb(serial,'shell','chown',identity,live)
ci.adb(serial,'shell','restorecon',live)
assert int(ci.adb(serial,'shell','stat','-c','%s',live))==size
assert 'Success' in ci.adb(serial,'install','-r',candidate)
after=ci.installed(serial,out/'after.txt')
assert after=={'versionCode':577311203,'uid':before['uid'],'apkSha256':ci.sha(candidate)}
capture=ci.signed_report_campaign(serial,out,ci.sha(candidate),old_sha)
start=time.monotonic()
capture('candidate-sized','COMPACT_DIAGNOSTIC',new_source)
elapsed=time.monotonic()-start
result=json.loads((out/'signed-report-readbacks.json').read_text())['reports'][-1]
assert result['status']=='VERIFIED_ANDROID_SAF_ZIP' and result['sourceCommit']==new_source
with zipfile.ZipFile(ui.OUT/result['file']) as reopened:
    manifest=json.loads(reopened.read('source-manifest.json'))
    states=check_archive(reopened)
assert manifest['snapshotBytes']>=size and manifest['snapshotSha256']==result['snapshotSha256']
receipt={'archiveStates':states,'fixtureRows':count,'verifiedSnapshotBytes':manifest['snapshotBytes'],'exportedZipBytes':result['bytes'],'scope':'API34_EMULATOR_202_TO_203_SYNTHETIC_ARCHIVE_BODIES_FIELD_173_ROW_SIZES_AND_FREE_PAGES','source':new_source,'before':before,'after':after,'sourceBytesBeforeLaunch':size,'populatedPagesBeforePadding':used,'secondsFromUiEntryThroughReopenAndEvidenceEncryption':round(elapsed,2),'underTwentyMinutes':elapsed<1200,'checks':result['checks'],'metrics':result['metrics'],'jobId':result['jobId'],'physicalDevice':False}
(out/'field-size-android-readback.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt),flush=True)
