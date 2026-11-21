from pathlib import Path
import hashlib,json,os,subprocess,time
tmp=Path(os.environ['RUNNER_TEMP']);out=tmp/'upgrade-evidence';out.mkdir(exist_ok=True)
base=Path('.github/r335-signed-runtime-probes');inputs=json.loads((base/'inputs.json').read_bytes())
def adb(*args):return subprocess.check_output(['adb',*map(str,args)],timeout=180).decode().strip()
assert adb('shell','getprop','ro.kernel.qemu')=='1' and adb('install',tmp/'B08-release.apk').endswith('Success')
installed=adb('shell','pm','path','com.starcore.research.r13').split('package:')[1]
assert adb('shell','sha256sum',installed).split()[0]==inputs['apkSha256']
helper='/data/local/tmp/r335-extended-tests.dex.jar'
adb('push',base/'test-probes.dex.jar',helper);adb('shell','chmod','444',helper)
assert adb('shell','sha256sum',helper).split()[0]==inputs['dexSha256']
info={k:inputs[k] for k in ('sourceCommit','apkSha256','dexSha256')};info.update(androidApi=adb('shell','getprop','ro.build.version.sdk'),fingerprint=adb('shell','getprop','ro.build.fingerprint'),cases=[])
for z in inputs['cases']:
    cmd=['adb','shell','env','CLASSPATH='+installed+':'+helper,'app_process','-Xms32m','-Xmx512m','/system/bin',z['className'],*z['args']]
    a=time.time();proc=subprocess.run(cmd,capture_output=True,timeout=240);body=proc.stdout+proc.stderr
    raw=z['className'].split('.')[-1]+'.log';(out/raw).write_bytes(body);txt=body.decode(errors='replace')
    ok=proc.returncode==0 and z['marker'] in txt
    info['cases'].append({**z,'actualExit':proc.returncode,'pass':ok,'started':a,'finished':time.time(),'command':cmd,'rawLog':raw,'rawSha256':hashlib.sha256(body).hexdigest()})
    (out/'extended-probes.json').write_text(json.dumps(info,indent=2)+'\n')
    print(z['className'],ok,flush=True)
assert all(z['pass'] for z in info['cases'])
