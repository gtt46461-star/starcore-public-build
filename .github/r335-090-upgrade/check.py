"""Read back the intact released 065 and 090 packages and SQLite sidecars before opening the update."""
from pathlib import Path
import importlib.util,sys,shlex,json,subprocess,os,time
kit=Path('.github/r335-independent-window').resolve();sys.path.insert(0,str(kit))
spec=importlib.util.spec_from_file_location('original_field',kit/'observe.py')
o=importlib.util.module_from_spec(spec);spec.loader.exec_module(o);f=o.base;temp=f.TEMP
old='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d';new='27a8bc5f6c1a5f218dea1b49949c5594a55b4684'
vold=577311065;vnew=577311090
pkg=f.PKG;db='/data/user/0/'+pkg+'/databases/'+o.DB
dest=temp/('R335-090-overlay-'+os.environ['GITHUB_RUN_ID']+'.json')
doc={'device':'disposable AOSP x86_64','api':34,'scope':'SYNTHETIC_APPLICATION_DATABASE_NORMAL_UPGRADE','sourceCommit':new,'apkSha256':None}
def save():dest.write_text(json.dumps(doc,indent=2)+'\n')
def stage(value):
    doc['stage']=value
    save()
def fingerprints():
    res={}
    for suffix in ('','-wal','-shm'):
        p=db+suffix
        if subprocess.run(['adb','shell','test','-s',p],capture_output=True).returncode==0:
            res[suffix or 'database']={'bytes':int(f.adb('shell','stat','-c','%s',p).strip()),'sha256':f.adb('shell','sha256sum',p).decode().split()[0]}
    assert 'database' in res
    return res
def main():
    stage('VERIFY_GUEST')
    assert f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    assert f.adb('shell','getprop','ro.build.version.sdk').strip()==b'34' and not f.adb('shell','pm','list','packages',pkg).strip()
    f.EVID.mkdir(exist_ok=True)
    f.adb('root');f.adb('wait-for-device')
    stage('INSTALL_PRIOR')
    prior=o.get_apk('B08',vold,old);a=f.fingerprint(prior);assert a=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
    assert b'Success' in f.adb('install',str(prior))
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(75)
    uid,sha=f.installed(f.EVID/'prior.txt',vold);assert sha==a
    stage('VERIFY_CURRENT_ARTIFACT')
    updated=o.get_apk('B08',vnew,new);doc['apkSha256']=f.fingerprint(updated);assert doc['apkSha256']==next(line.split()[0] for line in (temp/'B08-SUMS').read_text().splitlines() if line.endswith(new[:12]+'-release.apk'))
    doc['prior']={'versionCode':vold,'sha256':a,'uid':uid}
    stage('PAUSE_PRIOR_FOR_PHYSICAL_FILE_SNAPSHOT')
    assert 'disabled-user' in f.adb('shell','pm','disable-user','--user','0',pkg).decode()
    doc['quiescenceMode']='EXPLICIT_DISABLE_ON_DISPOSABLE_GUEST_FOR_RAW_BYTE_COMPARISON'
    stage('STOP_PRIOR')
    f.adb('shell','am','force-stop',pkg)
    # Wait for the previous process and its sidecars to become stationary. The
    # following install still has to preserve all the same hashes.
    before=fingerprints()
    doc['quiescentSamples']=[]
    for _ in range(45):
        time.sleep(1)
        next_read=fingerprints()
        pnames=[line.split()[-1] for line in f.adb('shell','ps','-A','-o','NAME').decode().splitlines() if line.strip()]
        running=sum(n==pkg or n.startswith(pkg+':') for n in pnames)
        doc['quiescentSamples'].append({'identical':before==next_read,'appProcesses':running})
        if before==next_read and running==0:break
        before=next_read
    else:raise AssertionError('PRIOR_DATABASE_DID_NOT_QUIESCE')
    doc['beforeLaunchDbAndSidecars']=before;doc['prior']={'versionCode':vold,'sha256':a,'uid':uid.decode() if isinstance(uid,bytes) else uid};save()
    stage('IN_PLACE_INSTALL')
    installed=f.adb('install','-r',str(updated)).decode().strip()
    assert 'Success' in installed;after=fingerprints()
    doc['afterInstallDbAndSidecars']=after
    stage('VERIFY_PACKAGE_MANAGER')
    uid2,sha2=f.installed(f.EVID/'updated.txt',vnew)
    doc.update(new={'versionCode':vnew,'sha256':sha2,'uid':int(uid2)},afterInstallDbAndSidecars=after,upgrade=installed,
               mismatchedSidecars=[key for key in sorted(set(before)|set(after)) if before.get(key)!=after.get(key)])
    save()
    stage('VERIFY_UID_HASH_DATABASE_AND_SIDECARS')
    assert (uid2,sha2)==(uid,doc['apkSha256']) and before==after
    doc.update(new={'versionCode':vnew,'sha256':sha2,'uid':doc['prior']['uid']},afterInstallDbAndSidecars=after,upgrade=installed);save()
    f.adb('shell','am','force-stop',pkg)
    stage('RESTORE_PRIOR_ENABLED_STATE')
    assert 'enabled' in f.adb('shell','pm','enable','--user','0',pkg).decode()
    stage('COLD_LAUNCH')
    started=f.adb('shell','am','start','-W','-n',f.ACTIVITY,timeout=180).decode()
    assert 'Status: ok' in started
    doc.update(launch=started,completed=True);stage('VERIFIED')
try:main()
except Exception as ex:
    import traceback
    doc.update(completed=False,failureType=type(ex).__name__,failureFrames=[Path(fr.filename).name+':'+str(fr.lineno) for fr in traceback.extract_tb(ex.__traceback__)][-9:])
    save();raise
