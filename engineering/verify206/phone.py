"""Measure a real signed Android report against a field-sized synthetic event and fetch ledgers."""
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
import threading
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
baseline = tmp/'installed-204.apk'
candidate = tmp/'candidate-206.apk'
assert serial.startswith('emulator-') and ci.adb(serial,'shell','getprop','ro.kernel.qemu') == '1'
old_sha = os.environ['BASELINE_SHA']
old_source = '85678fcfd12816aec8b47474cd2349350a5684d7'
new_source = '56c64f4ca761f1d40b1dec5d8546f537c6f136f0'
assert ci.sha(baseline)==old_sha
assert 'Success' in ci.adb(serial,'install',baseline)
before=ci.installed(serial,out/'before.txt')
assert before['versionCode']==577311204 and before['apkSha256']==old_sha
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
    # Two actual report ledgers at the cardinalities observed in the sealed 173 original.
    # The fixture values are synthetic and use a separate game key.
    event='v57_runtime_event_r926'
    fetch='v57_fetch_ledger_r925'
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<718660)
      INSERT INTO v57_runtime_event_r926(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at)
      SELECT printf('F206E%d',i),printf('F206D%d',i),'report-fixture','000','fixture','fixture','fixture',
             printf('F206_STAGE_%02d',i%23),0,printf('%0300d',i)||lower(hex(randomblob(320))),'fixture','offline-regression',i FROM n""")
    c.execute("""WITH RECURSIVE n(i) AS (VALUES(1) UNION ALL SELECT i+1 FROM n WHERE i<164173)
      INSERT INTO v57_fetch_ledger_r925(request_id,method,endpoint,request_at,status,source_line)
      SELECT printf('F206F%d',i),'FIXTURE','offline',i,printf('F206_STATUS_%02d',i%8),printf('%0188d',i)||lower(hex(randomblob(256))) FROM n""")
    c.commit()
    assert c.execute("SELECT COUNT(*) FROM v57_atom_edge_r925 WHERE left_atom LIKE 'test-%'").fetchone()[0]==len(pairs)
    c.execute('PRAGMA wal_checkpoint(TRUNCATE)').fetchall()
    c.execute('PRAGMA journal_mode=DELETE').fetchall()
    c.execute('VACUUM')
    assert c.execute('PRAGMA freelist_count').fetchone()==(0,)
    used=c.execute('PRAGMA page_count').fetchone()[0]
    page=c.execute('PRAGMA page_size').fetchone()[0]
assert page==4096 and 100000 < used < 600000
size=4913827840
pages=size//page
skip=0x40000000//page+1
with local.open('r+b') as f:
    f.truncate(size)
    f.seek(28)
    start=used+1+(used+1==skip)
    f.write(struct.pack('>III',pages,start,pages-used-int(used<skip<=pages)))
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
assert after=={'versionCode':577311206,'uid':before['uid'],'apkSha256':ci.sha(candidate)}
capture=ci.signed_report_campaign(serial,out,ci.sha(candidate),old_sha)
profile='COMPACT_DIAGNOSTIC'
stop=threading.Event()
events={}
left_app=threading.Event()
root='/data/user/0/'+pkg+'/files/r981_reports'
def observe():
    while not stop.is_set():
        try:
            current=json.loads(ci.adb(serial,'shell','cat',root+'/current-job.json'))['jobId']
            if re.fullmatch('[a-f0-9]{64}',current):
                job=root+'/jobs/'+current
                if not left_app.is_set():
                    envelope=json.loads(ci.adb(serial,'shell','cat',root+'/requests/'+current+'.json'))
                    if envelope.get('saveZipDestination') and envelope.get('includeRawEvidence') is False:
                        ci.adb(serial,'shell','input','keyevent','KEYCODE_HOME')
                        left_app.set()
                sample=json.loads(ci.adb(serial,'shell','cat',job+'/stage-progress.json'))
                stage=sample['stage']
                started=sample.get('startedWallMs',0)
                if started:
                    events.setdefault(current,{})[stage]=started
                journal=json.loads(ci.adb(serial,'shell','cat',job+'/job.json'))
                if journal.get('stage')=='READY_TO_SHARE':
                    events.setdefault(current,{})['READY_TO_SHARE']=journal['updatedWallMs']
        except (KeyError,ValueError,RuntimeError,subprocess.SubprocessError):
            pass
        stop.wait(0.75)
monitor=threading.Thread(target=observe,daemon=True)
monitor.start()
start=time.monotonic()
try:
    ui.commit = new_source
    ui.out = {'reportReadbacks': [], 'activeProfile': profile}
    label='生成并保存精简报告 ZIP'
    ui.CREATE=label
    ui.report_entry(label)
    candidate=ui.save_report(None,'field206-compact',picker_open=True)
    assert candidate['status']=='VERIFIED_ANDROID_SAF_ZIP' and not candidate['rawEvidenceIncluded']
    with zipfile.ZipFile(ui.OUT/candidate['file']) as zip_saved:
        source_manifest=json.loads(zip_saved.read('source-manifest.json'))
        assert source_manifest['apkSha256']==ci.sha(tmp/'candidate-206.apk')
        assert source_manifest['rawEvidenceIncluded'] is False
        assert 'snapshot.db' not in zip_saved.namelist() and 'installed-app.apk' not in zip_saved.namelist()
        assert 'DIAGNOSTIC_SCOPE.json' in zip_saved.namelist()
    result=dict(candidate,sourceCommit=new_source)

finally:
    stop.set()
    monitor.join(timeout=6)
    (out/'sampled-stage-transitions.json').write_text(json.dumps(events,indent=2))
elapsed=time.monotonic()-start
assert result['status']=='VERIFIED_ANDROID_SAF_ZIP' and result['sourceCommit']==new_source
with zipfile.ZipFile(ui.OUT/result['file']) as reopened:
    manifest=json.loads(reopened.read('source-manifest.json'))
    if profile=='FULL_RAW':
        assert result['rawEvidenceIncluded'] and result['snapshotQuickCheck']=='ok' and 'snapshot.db' in reopened.namelist()
    else:
        assert not result['rawEvidenceIncluded'] and 'DIAGNOSTIC_SCOPE.json' in reopened.namelist()
    if profile=='COMPACT_DIAGNOSTIC':
        scope=json.loads(reopened.read('DIAGNOSTIC_SCOPE.json'))
        tables={r['table']:r for r in scope['tables']}
        recent={r['table']:r for r in scope['recentEvidence']}
        for table,column,prefix,n,k in [('v57_runtime_event_r926','stage','F206_STAGE_',718660,23),('v57_fetch_ledger_r925','status','F206_STATUS_',164173,8)]:
            groups={r['value']:r['rows'] for r in tables[table]['states'] if r['column']==column and str(r['value']).startswith(prefix)}
            assert groups=={prefix+('%02d'%i): n//k+(1 if 0<i<=n%k else 0) for i in range(k)},(table,groups)
            assert recent[table]['totalRows']==tables[table]['rows']>=n and recent[table]['exportedRows']==5000
            assert any(str(row.get(column,'')).startswith(prefix) for row in recent[table]['rows'])
    checks={}
    for member in sorted(reopened.namelist()):
        if re.fullmatch(r'catalog-\d+\.jsonl',member):
            for line in reopened.open(member):
                row=json.loads(line)
                if row.get('kind')=='check_result':checks[row['id']]=row['execution_state']
    assert len(checks)==96 and 'RSEC-011' in checks
    if profile=='FULL_RAW':assert result['snapshotBytes']>=size

import hashlib
transfer=hashlib.sha256(('SAVE_PUBLISHED_ZIP|'+result['jobId']).encode()).hexdigest()
for attempt in range(90):
    try:
        saved=json.loads(ci.adb(serial,'shell','cat',root+'/saf/'+transfer+'.json'))
        if saved.get('state')=='VERIFIED_READBACK':break
    except (ValueError,RuntimeError,subprocess.SubprocessError):
        pass
    time.sleep(1)
assert saved['state']=='VERIFIED_READBACK' and saved['actualSha256']==result['sha256'] and saved['actualBytes']==result['bytes']
assert saved['allVolumesSaved'] and saved['volumeName']=='STARCORE_FULL_REPORT.zip'
assert left_app.is_set(), 'Background save was not exercised'
assert 0 < result['bytes'] < manifest['snapshotBytes']
assert manifest['snapshotBytes']>=size and manifest['snapshotSha256']==result['snapshotSha256']
receipt={'profile':profile,'publicSaveReadback':saved['state'],'publicSha256':saved['actualSha256'],'activityBackgrounded':left_app.is_set(),'rawDatabaseIncluded':False,'includedCompactScope':True,'stageStartsWallMs':events.get(result['jobId'],{}),'dashboardCheck':checks['RSEC-011'],'allCheckStates':checks,'fixtureEventRows':718660,'fixtureFetchRows':164173,'fixtureEdgeRows':len(pairs),'verifiedSnapshotBytes':manifest['snapshotBytes'],'exportedZipBytes':result['bytes'],'scope':'API34_EMULATOR_204_TO_206_718660_EVENTS_164173_FETCHES_28680_EDGES','source':new_source,'before':before,'after':after,'sourceBytesBeforeLaunch':size,'populatedPagesBeforePadding':used,'secondsFromUiEntryThroughReadback':round(elapsed,2),'underTwentyMinutes':elapsed<1200,'checks':result['checks'],'metrics':result['metrics'],'jobId':result['jobId'],'physicalDevice':False}
(out/'field-size-android-readback.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt),flush=True)

with Path(os.environ['GITHUB_STEP_SUMMARY']).open('a') as summary:
    summary.write('\n```json\n'+json.dumps(receipt)+'\n```\n')
