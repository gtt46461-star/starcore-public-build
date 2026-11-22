"""Read back a native full report after recreating the measured size pressure on a disposable Android guest."""
from pathlib import Path
import hashlib,importlib.util,json,os,re,subprocess,time,zipfile
root=Path(os.environ['GITHUB_WORKSPACE'])
tmp=Path(os.environ['RUNNER_TEMP'])
spec=importlib.util.spec_from_file_location('native_archive',root/'.github/r335-vivo-final-export/driver.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field
active='1c8382a233c0836e84c31b5827e86d74a60bc2ba'
import urllib.request
sumsUrl='https://github.com/gtt46461-star/starcore-public-build/releases/download/r335-B08-'+active[:12]+'/SHA256SUMS.txt'
with urllib.request.urlopen(sumsUrl,timeout=30) as response:
    expected=next(line.split()[0] for line in response.read().decode().splitlines() if line.endswith(active[:12]+'-release.apk'))
record={'sourceCommit':active,'apkSha256':expected,'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'DISPOSABLE_ANDROID_14_APPLICATION_UPGRADE_WITH_1373483008_BYTE_SIZE_FIXTURE','passed':False,'phase':'START'}
out=tmp/('R335-081-sized-export-'+str(record['runId'])+'.json')
def save(stage):
    record['phase']=stage;out.write_text(json.dumps(record,indent=2)+'\n')

def archive_execution(seq):
    contract=tmp/'material/STARCORE_R33_5_20261118/B01/scripts/report_expected_ids_r334.json'
    registered=set(json.loads(contract.read_bytes())['checks'])
    with zipfile.ZipFile(n.private/str(seq)/'joined.zip') as archive:
        receipt=json.loads(archive.read('R31_SEMANTIC_RECEIPT.json'))
        footer=json.loads(archive.read('ARCHIVE_MANIFEST.jsonl').splitlines()[-1])
    observed=receipt['checks']
    assert set(observed)==registered
    return {'registeredCheckCount':len(registered),
            'checksThatDidNotComplete':sorted(k for k in registered if observed[k].get('completes_contract') is not True),
            'checksComplete':footer['checks_complete'],'evidenceComplete':footer['evidence_complete']}
def main():
    assert f.adb('get-serialno').decode().strip().startswith('emulator-') and f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    f.adb('root');f.adb('wait-for-device')
    assert not f.adb('shell','pm','list','packages',f.PKG).strip()
    (f.EVID/'B08').mkdir(parents=True);f.NATIVE.mkdir()
    incoming=n.observe.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    previous=incoming.with_name('sizing-prior-065.apk');incoming.rename(previous)
    assert f.fingerprint(previous)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
    final=n.observe.get_apk('B08',577311081,active)
    assert f.fingerprint(final)==expected
    n.source=active;n.apk=expected
    f.adb('install',str(previous))
    f.adb('shell','cmd','connectivity','airplane-mode','enable')
    f.adb('shell','svc','wifi','disable');f.adb('shell','svc','data','disable')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(75)
    uid,_=f.installed(f.EVID/'pre-sized-upgrade.txt',577311065)
    f.adb('shell','am','force-stop',f.PKG)
    location='/data/user/0/'+f.PKG+'/databases/'+n.observe.DB
    save('CREATE_CONTROLLED_SIZE')
    holder='r335_disposable_size_fixture'
    # Keep the size-only rows until the external ZIP is verified. SQLite may reclaim pages
    # at close, and a file of reclaimed pages is not a stable test input across devices.
    def available_kib(path):
        return int(f.adb('shell','df','-k',path).decode().splitlines()[-1].split()[3])
    record['guestFreeKiBBeforeFixture']=available_kib('/data')
    record['hostFreeKiBBeforeFixture']=os.statvfs(tmp).f_bavail*os.statvfs(tmp).f_frsize//1024
    def sql(query):
        p=subprocess.run(['adb','shell','sqlite3','-batch','-bail',location],input='.timeout 30000\n'+query+';\n',capture_output=True,text=True,timeout=600)
        if p.returncode:
            detail=(p.stdout+p.stderr).lower()
            reason=next((name for fragment,name in [('locked','SQLITE_BUSY'),('full','DATABASE_FULL'),('i/o','SQLITE_IO'),('too big','SQLITE_LIMIT'),('readonly','SQLITE_READONLY')] if fragment in detail),'SQLITE_FIXTURE')
            record.update(sqliteFixtureError=reason,guestFreeKiBOnError=available_kib('/data'))
            save('CREATE_CONTROLLED_SIZE')
            raise RuntimeError(reason)
        return p.stdout.strip()
    draws=int(sql('SELECT count(*) FROM draws'));page=int(sql('PRAGMA page_size'));prior=int(sql('PRAGMA page_count'))*page
    vacuum=int(sql('PRAGMA auto_vacuum'))
    assert not sql("SELECT 1 FROM sqlite_master WHERE name='"+holder+"'") and prior<1373483008
    # Take and compare the exact catalogue under one SQLite writer reservation. Separate
    # sqlite3 processes return multi-line catalogue text across different snapshots.
    definitions="SELECT type,name,tbl_name,sql FROM main.sqlite_master WHERE name<>'"+holder+"'"
    chunks=(1373483008-prior+4194303)//4194304
    before='SELECT type,name,tbl_name,sql FROM baseline'
    result=sql(f"""BEGIN IMMEDIATE;
        CREATE TEMP TABLE baseline AS {definitions};
        CREATE TEMP TABLE draw_check AS SELECT count(*) AS n FROM main.draws;
        CREATE TABLE {holder}(sample BLOB);
        WITH RECURSIVE block(n) AS (VALUES(1) UNION ALL SELECT n+1 FROM block WHERE n<{chunks})
            INSERT INTO {holder} SELECT zeroblob(4194304) FROM block;
        SELECT count(*) FROM ({before} EXCEPT {definitions});
        SELECT count(*) FROM ({definitions} EXCEPT {before});
        SELECT (SELECT count(*) FROM main.draws)=(SELECT n FROM draw_check);
        COMMIT""").splitlines()
    assert len(result)==3, 'SIZE_FIXTURE_CATALOGUE_READBACK'
    added_or_changed=int(result[1]);removed_or_changed=int(result[0])
    sql('PRAGMA wal_checkpoint(TRUNCATE)')
    size=int(sql('PRAGMA page_count'))*page
    record.update(priorDbBytes=prior,controlledDbBytes=size,autoVacuum=vacuum,
                  sizeOnlyTablePresent=True,freePages=int(sql('PRAGMA freelist_count')),pageSize=page,
                  targetReached=size>=1373483008,drawCountPreserved=int(result[2])==1 and int(sql('SELECT count(*) FROM draws'))==draws,
                  addedOrChangedProductionDefinitions=added_or_changed,removedOrChangedProductionDefinitions=removed_or_changed,
                  schemaPreserved=not (added_or_changed or removed_or_changed),vacuumModePreserved=int(sql('PRAGMA auto_vacuum'))==vacuum)
    save('VERIFY_CONTROLLED_SIZE')
    assert all(record[key] for key in ('targetReached','drawCountPreserved','schemaPreserved','vacuumModePreserved'))
    record.update(unchangedDrawCount=draws)
    kept='/data/user/0/'+f.PKG+'/files/r335-qa-retained-original'
    assert subprocess.run(['adb','shell','test','-e',kept]).returncode!=0
    f.adb('shell','truncate','-s','1073741824',kept)
    f.adb('shell','chown',str(uid)+':'+str(uid),kept)
    def kept_digest():return f.adb('shell','sha256sum',kept,timeout=250).split()[0].decode()
    original=kept_digest()
    assert b'Success' in f.adb('install','-r',str(final))
    assert f.installed(f.EVID/'sized-upgraded.txt',577311081)==(uid,expected)
    record.update(uidRetained=True,controlledRetainedBytes=1<<30);save('NORMAL_FULL_REPORT_UI')
    f.adb('shell','pm','grant',f.PKG,'android.permission.POST_NOTIFICATIONS')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(65)
    lib=importlib.util.spec_from_file_location('controlled_ui',root/'.github/r335-081-owners/capture.py')
    ui=importlib.util.module_from_spec(lib);lib.loader.exec_module(ui);ui.n=n;ui.f=f
    known=set();ui.request(1,known);result=ui.record['attempts'][0];record['attempt']=result;save('EXTERNAL_RECEIVER')
    if result.get('phase')=='DONE':record['firstCheckExecution']=archive_execution(1)
    if result.get('strictRefusalCode')=='ARCHIVE_INCOMPLETE':
        save('REPEAT_ORIGINAL_AFTER_INCOMPLETE_CHECKS')
        ui.request(2,known);second=ui.record['attempts'][1];record['repeatAttempt']=second
        if second.get('phase')=='DONE':record['repeatCheckExecution']=archive_execution(2)
    save('EXTERNAL_RECEIVER')
    assert kept_digest()==original and f.installed(f.EVID/'returned.txt',577311081)==(uid,expected)
    record['privateOriginalPreserved']=True
    assert result.get('fullyVerifiedRawAndSystemPicker') and result.get('rawSnapshotBytes',0)>=1373483008 and result.get('archiveBytes',0)>0
    record.update(passed=True);save('COMPLETE')
try:
    main()
except Exception as e:
    import traceback
    record['failureType']=type(e).__name__;record['sourceFrames']=[Path(x.filename).name+':'+str(x.lineno) for x in traceback.extract_tb(e.__traceback__)][-12:]
    save(record['phase']);raise
