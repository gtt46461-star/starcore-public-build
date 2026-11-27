"""Measure a real signed Android report against a field-sized synthetic freelist."""
from contextlib import closing
import re
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
baseline = tmp/'installed-203.apk'
candidate = tmp/'candidate-204.apk'
assert serial.startswith('emulator-') and ci.adb(serial,'shell','getprop','ro.kernel.qemu') == '1'
old_sha = '59c459b46770d6c8f03407b2ce3b283793fd4f3d7ec6af07f77a50ba64330ee8'
old_source = '6ca140d4f41dd079c4828ac128f6396163adb0fd'
new_source = '85678fcfd12816aec8b47474cd2349350a5684d7'
assert ci.sha(baseline)==old_sha
assert 'Success' in ci.adb(serial,'install',baseline)
before=ci.installed(serial,out/'before.txt')
assert before['versionCode']==577311203 and before['apkSha256']==old_sha
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
pairs=[(f'test-{a:03}',f'test-{b:03}',(a+b)/480.0,100,'202611270384') for a in range(240) for b in range(a+1,240)]
with closing(sqlite3.connect(local)) as c:
    c.executemany("INSERT INTO v57_atom_edge_r925(left_atom,right_atom,correlation,support_issues,last_issue) VALUES(?,?,?,?,?)",pairs)
    c.commit()
    assert c.execute("SELECT COUNT(*) FROM v57_atom_edge_r925 WHERE left_atom LIKE 'test-%'").fetchone()[0]==len(pairs)
    c.execute('PRAGMA wal_checkpoint(TRUNCATE)').fetchall()
    c.execute('PRAGMA journal_mode=DELETE').fetchall()
    c.execute('VACUUM')
    assert c.execute('PRAGMA freelist_count').fetchone()==(0,)
    used=c.execute('PRAGMA page_count').fetchone()[0]
    page=c.execute('PRAGMA page_size').fetchone()[0]
assert page==4096 and used < 100000
size=4871925760
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
assert after=={'versionCode':577311204,'uid':before['uid'],'apkSha256':ci.sha(candidate)}
capture=ci.signed_report_campaign(serial,out,ci.sha(candidate),old_sha)
start=time.monotonic()
capture('candidate-sized','FULL_RAW',new_source)
elapsed=time.monotonic()-start
result=json.loads((out/'signed-report-readbacks.json').read_text())['reports'][-1]
assert result['status']=='VERIFIED_ANDROID_SAF_ZIP' and result['sourceCommit']==new_source
with zipfile.ZipFile(ui.OUT/result['file']) as reopened:
    manifest=json.loads(reopened.read('source-manifest.json'))
    assert result['rawEvidenceIncluded'] and result['snapshotQuickCheck']=='ok' and 'snapshot.db' in reopened.namelist()
    checks={}
    for member in sorted(reopened.namelist()):
        if re.fullmatch(r'catalog-\d+\.jsonl',member):
            for line in reopened.open(member):
                row=json.loads(line)
                if row.get('kind')=='check_result':checks[row['id']]=row['execution_state']
    assert len(checks)==96 and 'RSEC-011' in checks
    assert result['snapshotBytes']>=size

assert manifest['snapshotBytes']>=size and manifest['snapshotSha256']==result['snapshotSha256']
receipt={'dashboardCheck':checks['RSEC-011'],'allCheckStates':checks,'fixtureRows':len(pairs),'verifiedSnapshotBytes':manifest['snapshotBytes'],'exportedZipBytes':result['bytes'],'scope':'API34_EMULATOR_203_TO_204_FULL_RAW_28680_SYNTHETIC_EDGES_AND_FREE_PAGES','source':new_source,'before':before,'after':after,'sourceBytesBeforeLaunch':size,'populatedPagesBeforePadding':used,'secondsFromUiEntryThroughReopenAndEvidenceEncryption':round(elapsed,2),'underTwentyMinutes':elapsed<1200,'checks':result['checks'],'metrics':result['metrics'],'jobId':result['jobId'],'physicalDevice':False}
(out/'field-size-android-readback.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt),flush=True)

with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as summary:
    summary.write('\n```json\n'+json.dumps(receipt)+'\n```\n')
