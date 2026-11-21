"""Read back a native full report after recreating the measured size pressure on a disposable Android guest."""
from pathlib import Path
import hashlib,importlib.util,json,os,re,subprocess,time
root=Path(os.environ['GITHUB_WORKSPACE'])
tmp=Path(os.environ['RUNNER_TEMP'])
spec=importlib.util.spec_from_file_location('native_archive',root/'.github/r335-vivo-final-export/driver.py')
n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n)
f=n.field
active='ff555a802da6c135d395cce51451b0b1ade93b69'
import urllib.request
sumsUrl='https://github.com/gtt46461-star/starcore-public-build/releases/download/r335-B08-'+active[:12]+'/SHA256SUMS.txt'
with urllib.request.urlopen(sumsUrl,timeout=30) as response:
    expected=next(line.split()[0] for line in response.read().decode().splitlines() if line.endswith(active[:12]+'-release.apk'))
record={'sourceCommit':active,'apkSha256':expected,'runId':int(os.environ['GITHUB_RUN_ID']),'scope':'DISPOSABLE_ANDROID_14_APPLICATION_UPGRADE_WITH_1373483008_BYTE_SIZE_FIXTURE','passed':False,'phase':'START'}
out=tmp/('R335-070-sized-export-'+str(record['runId'])+'.json')
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
    final=n.observe.get_apk('B08',577311070,active)
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
    import hashlib
    holder='r335_disposable_size_fixture'
    # AOSP databases may use FULL auto-vacuum. Preserve that setting and retain a labelled
    # size-only table in this fresh, isolated guest when dropping it would reclaim the pages.
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
    def schema():
        return hashlib.sha256(sql("SELECT type,name,coalesce(sql,'') FROM sqlite_master WHERE name!='"+holder+"' ORDER BY type,name").encode()).hexdigest()
    draws=int(sql('SELECT count(*) FROM draws'));page=int(sql('PRAGMA page_size'));prior=int(sql('PRAGMA page_count'))*page
    vacuum=int(sql('PRAGMA auto_vacuum'));original_schema=schema()
    assert not sql("SELECT 1 FROM sqlite_master WHERE name='"+holder+"'") and prior<1373483008
    sql("CREATE TABLE "+holder+"(sample BLOB)")
    while int(sql('PRAGMA page_count'))*page<1373483008:
        sql("BEGIN; WITH RECURSIVE a(i) AS (VALUES(0) UNION ALL SELECT i+1 FROM a WHERE i<3) INSERT INTO "+holder+" SELECT zeroblob(4194304) FROM a; COMMIT")
        sql('PRAGMA wal_checkpoint(TRUNCATE)')
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
    assert f.installed(f.EVID/'sized-upgraded.txt',577311070)==(uid,expected)
    record.update(uidRetained=True,controlledRetainedBytes=1<<30);save('NORMAL_FULL_REPORT_UI')
    f.adb('shell','pm','grant',f.PKG,'android.permission.POST_NOTIFICATIONS')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(65)
    lib=importlib.util.spec_from_file_location('controlled_ui',root/'.github/r335-070-owners/capture.py')
    ui=importlib.util.module_from_spec(lib);lib.loader.exec_module(ui);ui.n=n;ui.f=f
    ui.request(1,set());result=ui.record['attempts'][0];record['attempt']=result;save('EXTERNAL_RECEIVER')
    assert kept_digest()==original and f.installed(f.EVID/'returned.txt',577311070)==(uid,expected)
    record['privateOriginalPreserved']=True
    assert result.get('fullyVerifiedRawAndSystemPicker') and result.get('rawSnapshotBytes',0)>0 and result.get('archiveBytes',0)>0
    record.update(passed=True);save('COMPLETE')
try:
    main()
except Exception as e:
    import traceback
    record['failureType']=type(e).__name__;record['sourceFrames']=[Path(x.filename).name+':'+str(x.lineno) for x in traceback.extract_tb(e.__traceback__)][-12:]
    save(record['phase']);raise
