"""Read back the intact released 068 and 073 packages and SQLite sidecars before opening the update."""
from pathlib import Path
import importlib.util,sys,shlex,json,subprocess,os,time
kit=Path('.github/r335-independent-window').resolve();sys.path.insert(0,str(kit))
spec=importlib.util.spec_from_file_location('original_field',kit/'observe.py')
o=importlib.util.module_from_spec(spec);spec.loader.exec_module(o);f=o.base;temp=f.TEMP
old='0ba6625b870892b084e91e2fc91569267d9c653e';new='6338395e97c9ff43b0480b2fe7689081a2ee6532'
vold=577311069;vnew=577311073
pkg=f.PKG;db='/data/user/0/'+pkg+'/databases/'+o.DB
dest=temp/('R335-073-overlay-'+os.environ['GITHUB_RUN_ID']+'.json')
doc={'device':'disposable AOSP x86_64','api':34,'scope':'SYNTHETIC_APPLICATION_DATABASE_NORMAL_UPGRADE','sourceCommit':new,'apkSha256':None}
def save():dest.write_text(json.dumps(doc,indent=2)+'\n')
def fingerprints():
    res={}
    for suffix in ('','-wal','-shm'):
        p=db+suffix
        if subprocess.run(['adb','shell','test','-s',p],capture_output=True).returncode==0:
            res[suffix or 'database']={'bytes':int(f.adb('shell','stat','-c','%s',p).strip()),'sha256':f.adb('shell','sha256sum',p).decode().split()[0]}
    assert 'database' in res
    return res
def main():
    assert f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    assert f.adb('shell','getprop','ro.build.version.sdk').strip()==b'34' and not f.adb('shell','pm','list','packages',pkg).strip()
    f.EVID.mkdir(exist_ok=True)
    f.adb('root');f.adb('wait-for-device')
    prior=o.get_apk('B08',vold,old);a=f.fingerprint(prior);assert a=='74afc9fa72d111160c669c54dbf783079f688b2fb5d1059b8b9cd4ff433b7c81'
    assert b'Success' in f.adb('install',str(prior))
    f.adb('shell','am','start','-W','-n',f.ACTIVITY);time.sleep(75)
    uid,sha=f.installed(f.EVID/'prior.txt',vold);assert sha==a
    updated=o.get_apk('B08',vnew,new);doc['apkSha256']=f.fingerprint(updated);assert doc['apkSha256']==next(line.split()[0] for line in (temp/'B08-SUMS').read_text().splitlines() if line.endswith(new[:12]+'-release.apk'))
    f.adb('shell','am','force-stop',pkg);before=fingerprints();doc['beforeLaunchDbAndSidecars']=before;doc['prior']={'versionCode':vold,'sha256':a,'uid':uid.decode() if isinstance(uid,bytes) else uid};save()
    installed=f.adb('install','-r',str(updated)).decode().strip()
    assert 'Success' in installed;after=fingerprints()
    uid2,sha2=f.installed(f.EVID/'updated.txt',vnew);assert (uid2,sha2)==(uid,doc['apkSha256']) and before==after
    doc.update(new={'versionCode':vnew,'sha256':sha2,'uid':doc['prior']['uid']},afterInstallDbAndSidecars=after,upgrade=installed);save()
    f.adb('shell','am','force-stop',pkg)
    started=f.adb('shell','am','start','-W','-n',f.ACTIVITY,timeout=180).decode()
    assert 'Status: ok' in started
    doc.update(launch=started,completed=True);save()
try:main()
except Exception as ex:doc.update(completed=False,failureType=type(ex).__name__);save();raise
