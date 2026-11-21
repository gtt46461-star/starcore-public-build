"""Read back a native full report after recreating the measured size pressure on a disposable Android guest."""
from pathlib import Path
import hashlib,importlib.util,json,os,re,subprocess,time
root=Path(os.environ['GITHUB_WORKSPACE'])
tmp=Path(os.environ['RUNNER_TEMP'])
spec=importlib.util.spec_from_file_location('native_archive',root/'.github/r335-vivo-final-export/driver.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field
active='43fef2458c33e7c6140d9822ae91ad1bf4cc6529'
expected='a8e1ea1fdec69dc36bac5b2d573b44528ebd6c5e32f5cc2ee8f35ea2f7e4e3cb'
record={'sourceCommit':active,'apkSha256':expected,'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'DISPOSABLE_ANDROID_14_APPLICATION_UPGRADE_WITH_1373483008_BYTE_SIZE_FIXTURE','passed':False,'phase':'START'}
out=tmp/('R335-vivo-sized-export-'+str(record['runId'])+'.json')
def save(stage):
    record['phase']=stage;out.write_text(json.dumps(record,indent=2)+'\n')
def main():
    assert f.adb('get-serialno').decode().strip().startswith('emulator-') and f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    f.adb('root');f.adb('wait-for-device')
    assert not f.adb('shell','pm','list','packages',f.PKG).strip()
    (f.EVID/'B08').mkdir(parents=True);f.NATIVE.mkdir()
    incoming=n.observe.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    previous=incoming.with_name('sizing-prior-065.apk');incoming.rename(previous)
    assert f.fingerprint(previous)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
    final=n.observe.get_apk('B08',577311067,active)
    assert f.fingerprint(final)==expected
    n.apk=expected
    f.adb('install',str(previous))
    f.adb('shell','cmd','connectivity','airplane-mode','enable')
    f.adb('shell','svc','wifi','disable');f.adb('shell','svc','data','disable')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(75)
    uid,_=f.installed(f.EVID/'pre-sized-upgrade.txt',577311065)
    f.adb('shell','am','force-stop',f.PKG)
    location='/data/user/0/'+f.PKG+'/databases/'+n.observe.DB
    save('CREATE_CONTROLLED_SIZE')
    import hashlib
    holder='r335_disposable_size_fixture'
    # AOSP databases may use FULL auto-vacuum. Preserve that setting and retain a labelled
    # size-only table in this fresh, isolated guest when dropping it would reclaim the pages.
    def sql(query):
        p=subprocess.run(['adb','shell','sqlite3','-batch','-bail',location],input=query+';\n',capture_output=True,text=True,timeout=600)
        if p.returncode:
            reason='DATABASE_FULL' if 'full' in p.stderr.lower() else 'SQLITE_FIXTURE'
            raise RuntimeError(reason)
        return p.stdout.strip()
    def schema():
        return hashlib.sha256(sql("SELECT type,name,coalesce(sql,'') FROM sqlite_master WHERE name!='"+holder+"' ORDER BY type,name").encode()).hexdigest()
    draws=int(sql('SELECT count(*) FROM draws'));page=int(sql('PRAGMA page_size'));prior=int(sql('PRAGMA page_count'))*page
    vacuum=int(sql('PRAGMA auto_vacuum'));original_schema=schema()
    assert not sql("SELECT 1 FROM sqlite_master WHERE name='"+holder+"'") and prior<1373483008
    sql("CREATE TABLE "+holder+"(sample BLOB)")
    while int(sql('PRAGMA page_count'))*page<1373483008:
        sql("BEGIN; WITH RECURSIVE a(i) AS (VALUES(0) UNION ALL SELECT i+1 FROM a WHERE i<7) INSERT INTO "+holder+" SELECT zeroblob(8388608) FROM a; COMMIT")
    if vacuum!=1:sql("DROP TABLE "+holder)
    sql('PRAGMA wal_checkpoint(TRUNCATE)')
    size=int(sql('PRAGMA page_count'))*page
    assert size>=1373483008 and int(sql('SELECT count(*) FROM draws'))==draws and schema()==original_schema and int(sql('PRAGMA auto_vacuum'))==vacuum
    record.update(priorDbBytes=prior,controlledDbBytes=size,autoVacuum=vacuum,sizeOnlyTablePresent=vacuum==1,freePages=int(sql('PRAGMA freelist_count')),pageSize=page,unchangedDrawCount=draws,unchangedProductionSchemaSha256=original_schema)
    kept='/data/user/0/'+f.PKG+'/files/r335-qa-retained-original'
    assert subprocess.run(['adb','shell','test','-e',kept]).returncode!=0
    f.adb('shell','truncate','-s','1073741824',kept)
    f.adb('shell','chown',str(uid)+':'+str(uid),kept)
    def kept_digest():return f.adb('shell','sha256sum',kept,timeout=250).split()[0].decode()
    original=kept_digest()
    assert b'Success' in f.adb('install','-r',str(final))
    assert f.installed(f.EVID/'sized-upgraded.txt',577311067)==(uid,expected)
    record.update(uidRetained=True,controlledRetainedBytes=1<<30);save('NORMAL_FULL_REPORT_UI')
    f.adb('shell','pm','grant',f.PKG,'android.permission.POST_NOTIFICATIONS')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(65)
    f.safe_reports('B08',1,active)
    result=n.observe_native(f.NATIVE/'B08/1',1,set());record['attempt']=result;save('EXTERNAL_RECEIVER')
    assert kept_digest()==original and f.installed(f.EVID/'returned.txt',577311067)==(uid,expected)
    record['privateOriginalPreserved']=True
    assert result.get('fullyVerifiedRawAndSystemPicker') and result.get('rawSnapshotBytes',0)>0 and result.get('archiveBytes',0)>0
    record.update(passed=True);save('COMPLETE')
try:
    main()
except Exception as e:
    import traceback
    record['failureType']=type(e).__name__;record['sourceFrames']=[Path(x.filename).name+':'+str(x.lineno) for x in traceback.extract_tb(e.__traceback__)][-12:]
    save(record['phase']);raise
