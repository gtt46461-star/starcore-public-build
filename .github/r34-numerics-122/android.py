"""Run the immutable registered probes on a fresh API 34 guest using only public inputs."""
import hashlib,json,os,re,subprocess,time
from pathlib import Path
root=Path(os.environ['GITHUB_WORKSPACE']);tmp=Path(os.environ['RUNNER_TEMP'])
suite=root/'.github/r335-verified-art-20261121';expected='5b887677a727e3b8d07362c02bc3cd497ae26de052944e8b399b5cefc9d96cc6'
pin=json.loads((root/'.github/r34-numerics-122/INPUTS.json').read_bytes())
apk=tmp/'v122.apk';probe=root/'.github/r34-numerics-122/probes.dex.jar';assert hashlib.sha256(apk.read_bytes()).hexdigest()==expected
assert hashlib.sha256(probe.read_bytes()).hexdigest()==pin['dexSha256']
def command(*args):
    return subprocess.check_output(list(map(str,args)),stderr=subprocess.STDOUT,timeout=180).decode().strip()
def adb(*args):return command('adb',*args)
assert adb('shell','getprop','ro.kernel.qemu')=='1' and adb('shell','getprop','ro.build.version.sdk')=='34'
assert adb('install',apk).endswith('Success')
path=adb('shell','pm','path','com.starcore.research.r13').split(':',1)[1]
assert adb('shell','sha256sum',path).split()[0]==expected
dest='/data/local/tmp/fixed-numerical-checks.jar';adb('push',probe,dest);adb('shell','chmod','0444',dest)
assert adb('shell','sha256sum',dest).split()[0]==pin['dexSha256']
out=tmp/('art-'+os.environ['GITHUB_RUN_ID']+'.json')
doc=dict(sourceCommit='5562f9b19aa09f9f7d9e69ca085db56c537967fc',apkSha256=expected,api=34,physical=False,testDexSha256=pin['dexSha256'],runId=int(os.environ['GITHUB_RUN_ID']),cases={})
def save():out.write_text(json.dumps(doc,indent=2)+'\n')
save()
cases=[('AtomNetSessionResumeReviewTest',['fixed'],'PASS_EXPECTED_REVISION_BEHAVIOR'),('AtomNetSessionNumericalProbe',[],'B04_ACTUAL_SESSION_NUMERICS'),('R335AndroidGradientTrace',[],'R335_ANDROID_GRADIENT_TRACE'),('ResearchConsumerR335Host',[],'R335_ACTUAL_CONSUMER_PURE'),('R33NaturalQualificationHost',[],'PASS')]
for cls,args,marker in cases:
    call=['adb','shell','env','CLASSPATH='+path+':'+dest,'app_process','-Xms16m','-Xmx256m','/system/bin','ProbeInvoker',cls,*args]
    start=time.monotonic();p=subprocess.run(call,capture_output=True,timeout=600);raw=p.stdout+p.stderr
    record=dict(exit=p.returncode,containsExpectedMarker=marker.encode() in raw,elapsedMs=int((time.monotonic()-start)*1000),rawLogSha256=hashlib.sha256(raw).hexdigest(),probeSourceSha256=hashlib.sha256((suite/(cls+'.java')).read_bytes()).hexdigest())
    record['passed']=p.returncode==0 and record['containsExpectedMarker'] and b'ORIGINAL_MAIN_RETURNED' in raw
    for key in ['gradientSlices','pretrainSamples','epoch','onlineUpdates','workSlices','checks','evalSlices','trainingRows','selectRows','heldOutRows']:
        m=re.search(rb'\b'+key.encode()+rb'=([0-9]+)\b',raw)
        if m:record[key]=int(m[1])
    for key in ['initial','final','resumed','completeState','restoredCompleteState','weightsSha256']:
        m=re.search(rb'\b'+key.encode()+rb'=([a-f0-9]{64})\b',raw)
        if m:record[key]=m[1].decode()
    doc['cases'][cls]=record;save()
doc['completed']=len(doc['cases'])==len(cases);save()
if not all(row['passed'] for row in doc['cases'].values()):raise RuntimeError('CASE_RESULT_NOT_VERIFIED')
