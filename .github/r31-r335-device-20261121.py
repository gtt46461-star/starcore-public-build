"""Exercise upgrades and production report UI on one disposable API 34 Android guest."""
from pathlib import Path
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.error
import urllib.request

STAGES = [
    ("P00", 577311036, "3b3e63e1dc1d7985ab3860c6dd900ffeb427c5db"),
    ("B01", 577311037, "66149e066d089ce9be008d3b836134e0c88186e8"),
    ("B02", 577311038, "0bb45fe209dd625f720486c5a0b8c0a80703f63b"),
    ("B03", 577311039, "ac75b096b5b45420480ac2357d3e15f42dd1f94d"),
    ("B04", 577311040, "ae75874ae69f87fc821b70b8ec7a5a4f254e6ad4"),
    ("B05", 577311041, "c0d7d1a498956f1a6ddc88af8eca0680d9a55a5e"),
    ("B06", 577311042, "38153a3883fe8099afb67c40f2eec3b60339591c"),
    ("B07", 577311043, "81dc26f276bd394ee08bb5a1d78140837bfad4a5"),
    ("B08", 577311044, "ee9fbd37f4bbed4cf4d8ec1ab1fcbd296ae04322"),
]
PKG = 'com.starcore.research.r13'
CERT = 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
BASE_APK = '8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
REPO = 'gtt46461-star/starcore-public-build'
TEMP = Path(os.environ['RUNNER_TEMP'])
EVID = TEMP/'upgrade-evidence'
NATIVE = TEMP/'report-originals'
APP_ROOT = '/data/user/0/'+PKG+'/files/r981_reports'
ACTIVITY = PKG+'/com.ethk3.novaonepro.MainActivity'
SRC = Path('app-source').resolve()

def cmd(*args, timeout=120):
    proc = subprocess.run(args, capture_output=True, timeout=timeout)
    if proc.returncode:
        raise RuntimeError(str(args[0])+' exited '+str(proc.returncode))
    return proc.stdout

def adb(*args, timeout=120):
    return cmd('adb', *args, timeout=timeout)

def dump(path, obj):
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n')

def download(url, dest, wait=False):
    deadline = time.monotonic() + (7200 if wait else 240)
    while True:
        try:
            with urllib.request.urlopen(url,timeout=90) as response, dest.open('wb') as file:
                shutil.copyfileobj(response,file,1<<20)
            return
        except (OSError,urllib.error.URLError,TimeoutError):
            if time.monotonic()>=deadline:raise
            time.sleep(20)

def fingerprint(file):
    with file.open('rb') as stream:
        return hashlib.file_digest(stream,'sha256').hexdigest()

def installed(path, version):
    raw=adb('shell','dumpsys','package',PKG)
    path.write_bytes(raw)
    assert {int(v) for v in re.findall(rb'\bversionCode=(\d+)',raw)}=={version}
    uid=re.search(rb'\b(?:userId|appId)=(\d+)',raw)
    assert uid, 'PackageManager UID missing'
    base=adb('shell','pm','path',PKG).decode().strip().splitlines()
    base=next(s.removeprefix('package:') for s in base if s.endswith('/base.apk'))
    digest=adb('shell','sha256sum',base).decode().split()[0]
    return int(uid[1]),digest

def safe_reports(stage, count, commit):
    cmd('git','-C',str(SRC),'fetch','--quiet','--depth=1','--filter=blob:none','origin',commit)
    cmd('git','-C',str(SRC),'checkout','--quiet','--detach',commit)
    assert cmd('git','-C',str(SRC),'rev-parse','HEAD').decode().strip()==commit
    for i in range(1,count+1):
        raw=NATIVE/stage/str(i)
        raw.mkdir(parents=True)
        # Keep each actual SAF readback under a new filename before the next picker invocation.
        prior=adb('shell','ls','/sdcard/Download').decode().splitlines()
        if 'STARCORE_FULL_REPORT.zip' in prior:
            adb('shell','mv','/sdcard/Download/STARCORE_FULL_REPORT.zip',f'/sdcard/Download/STARCORE-{stage}-{i}-previous.zip')
        with (raw/'ui-console.txt').open('wb') as out:
            try:
                proc=subprocess.run([sys.executable,'-B','tools/r31/phone_report_ui.py','--out',str(raw)],cwd=SRC,
                    stdout=out,stderr=subprocess.STDOUT,env=dict(os.environ,R31_REPORT_STARTUP_WAIT_SECONDS='90',R31_REPORT_PROBE_SECONDS='600'),timeout=1100)
                code=proc.returncode
            except subprocess.TimeoutExpired:
                code=124
        try:
            indices=adb('shell',f'find {APP_ROOT}/jobs -path "*/delivery/index.json" -type f 2>/dev/null || true').decode().splitlines()
            for index in indices:
                match=re.fullmatch(re.escape(APP_ROOT)+'/jobs/([0-9a-f]{64})/delivery/index.json',index)
                if match:
                    prefix=raw/'native'/match[1]
                    prefix.mkdir(parents=True,exist_ok=True)
                    for name in ['delivery/index.json','job.json','published.json']:
                        remote=f'{APP_ROOT}/jobs/{match[1]}/{name}'
                        data=adb('shell',f'if [ -f {remote} ]; then cat {remote}; fi')
                        if data:(prefix/Path(name).name).write_bytes(data)
            (raw/'package-pids.txt').write_bytes(adb('shell','pidof',PKG) if adb('shell','ps','-A').find(PKG.encode())>=0 else b'')
        except (RuntimeError,subprocess.TimeoutExpired):
            pass
        output=EVID/stage
        summary={'source':commit,'stage':stage,'attempt':i,'scriptExit':code,
                 'sha256':{p.name:fingerprint(p) for p in raw.iterdir() if p.is_file()},
                 'acceptance':(raw/'ui-report-acceptance.json').is_file()}
        for f in ['ui-report-acceptance.json','safe-report-semantics.json','safe-ui-failure.json']:
            if (raw/f).is_file():shutil.copyfile(raw/f,output/(f'{i}-'+f))
        dump(output/f'report-attempt-{i}.json',summary)
        print(stage,'production report attempt',i,'exit',code,'acceptance',summary['acceptance'],flush=True)

def main():
    EVID.mkdir();NATIVE.mkdir()
    adb('root');adb('wait-for-device')
    assert adb('get-serialno').decode().strip().startswith('emulator-')
    assert adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    assert adb('shell','id','-u').strip()==b'0'
    dump(EVID/'device.json',{'type':'DISPOSABLE_ANDROID_EMULATOR','serial':adb('get-serialno').decode().strip(),
         'fingerprint':adb('shell','getprop','ro.build.fingerprint').decode().strip(),
         'abi':adb('shell','getprop','ro.product.cpu.abi').decode().strip(), 'api':int(adb('shell','getprop','ro.build.version.sdk'))})
    apk=TEMP/'stage.apk'
    download('https://raw.githubusercontent.com/'+REPO+'/0fcf8b9e6286e24dc185328af0d02a0458aaee03/STARCORE-R32.5.1-C1-033-release.apk',apk)
    assert fingerprint(apk)==BASE_APK
    assert b'Success' in adb('install',str(apk))
    old,old_digest=577311033,BASE_APK
    adb('shell','am','start','-W','-n',ACTIVITY)
    for stage,version,commit in STAGES:
        out=EVID/stage;out.mkdir()
        url='https://github.com/'+REPO+'/releases/download/r335-'+stage+'-'+commit[:12]+'/'
        name='STARCORE-R33-'+str(version)+'-'+commit[:12]+'-release.apk'
        download(url+'SHA256SUMS.txt',out/'SHA256SUMS.txt',wait=True)
        checks={n:d for d,n in [line.split('  ',1) for line in (out/'SHA256SUMS.txt').read_text().splitlines()]}
        download(url+name,apk)
        assert checks[name]==fingerprint(apk)
        signer=cmd(os.environ['ANDROID_HOME']+'/build-tools/35.0.0/apksigner','verify','--print-certs',str(apk)).decode()
        assert 'Signer #1 certificate SHA-256 digest: '+CERT in signer
        before,previous=installed(out/'device-before-upgrade.txt',old)
        assert previous==old_digest
        install=adb('install','-r',str(apk))
        (out/'install.txt').write_bytes(install)
        assert b'Success' in install
        after,actual=installed(out/'device-after-upgrade.txt',version)
        assert after==before and actual==checks[name]
        launch=adb('shell','am','start','-W','-n',ACTIVITY)
        (out/'launch.txt').write_bytes(launch)
        adb('shell','input','keyevent','KEYCODE_HOME')
        time.sleep(4)
        (out/'background-services.txt').write_bytes(adb('shell','dumpsys','activity','services',PKG))
        dump(out/'upgrade.json',{'commit':commit,'fromVersion':old,'version':version,'fromInstalledApkSha256':previous,
              'installedApkSha256':actual,'fromUid':before,'uid':after,'certificateSha256':CERT,
              'replaceInstall':True,'entryLaunchOK':b'Status: ok' in launch,'utc':int(time.time())})
        print(stage,version,'upgrade read back',actual,flush=True)
        old,old_digest=version,actual
        if stage in ['B07','B08']:safe_reports(stage,3 if stage=='B08' else 1,commit)

if __name__=='__main__':
    main()
