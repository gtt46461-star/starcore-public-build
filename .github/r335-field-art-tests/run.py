"""Run only the sealed, controlled probes against the exact installed public application DEX on Android ART."""
import hashlib,json,os,re,subprocess,time
from pathlib import Path
folder=Path('.github/r335-field-art-tests');tmp=Path(os.environ['RUNNER_TEMP']);out=tmp/'upgrade-evidence';out.mkdir(exist_ok=True)
private=tmp/'report-originals';private.mkdir(exist_ok=True);app=tmp/'B08-release.apk';expected=hashlib.sha256(app.read_bytes()).hexdigest()
ops=json.loads((folder/'INPUTS.json').read_bytes())
def command(*a,timeout=300):return subprocess.check_output(list(map(str,a)),timeout=timeout).decode().strip()
def adb(*a):return command('adb',*a)
assert adb('shell','getprop','ro.kernel.qemu')=='1'
assert adb('install',app).endswith('Success')
path=adb('shell','pm','path','com.starcore.research.r13').split('package:',1)[1]
assert path.endswith('/base.apk') and adb('shell','sha256sum',path).split()[0]==expected
p=folder/'test-probes.dex.jar';helper='/data/local/tmp/starcore-r335-final-probes.jar'
assert hashlib.sha256(p.read_bytes()).hexdigest()==ops['dexSha256']
adb('push',p,helper);adb('shell','chmod','444',helper);assert adb('shell','sha256sum',helper).split()[0]==ops['dexSha256']
(out/'device.txt').write_text(adb('shell','getprop','ro.build.fingerprint')+'\n')
(out/'target-package.txt').write_text(adb('shell','dumpsys','package','com.starcore.research.r13')+'\n')
cases=[('AtomNetSessionNumericalProbe',[]),('AtomNetSessionResumeReviewTest',['fixed']),('R335AndroidGradientTrace',[]),('ResearchConsumerR335Host',[]),('R33NaturalQualificationHost',[])]
res={'diagnosticsDatasets':ops,'sourceCommit':ops['finalSourceCommit'],'apkSha256':expected,'installedPath':path,'testDexSha256':ops['dexSha256'],'device':adb('shell','getprop','ro.build.fingerprint'),
     'scope':'Actual release classes loaded on Android ART in a separate shell process on a disposable emulator. Controlled inputs; no Android database, application lifecycle or future evidence asserted.','cases':[]}
def save():(out/'art-runtime.json').write_text(json.dumps(res,indent=2)+'\n')
save()
for c,args in cases:
    invocation=['adb','shell','env','CLASSPATH='+path+':'+helper,'app_process','-Xms32m','-Xmx512m','/system/bin','com.ethk3.novaonepro.strategyv2.R335ProbeLauncher',('R335EpochReceiptProbe' if c=='R335AndroidGradientTrace' else c),*args]
    time.sleep(2)
    start=time.time()
    trials=[]
    for trial in range(1,2):
        (private/(c+'-before-'+str(trial)+'.txt')).write_text(adb('shell','cat','/proc/meminfo'))
        proc=subprocess.run(invocation,capture_output=True,timeout=900)
        raw=proc.stdout+proc.stderr
        (out/(c+'-attempt-'+str(trial)+'.log')).write_bytes(raw)
        trials.append({'attempt':trial,'exitCode':proc.returncode,'rawSha256':hashlib.sha256(raw).hexdigest()})
        if proc.returncode!=137:break
        kernel=subprocess.run(['adb','logcat','-d','-b','events','-s','am_kill','am_proc_died'],capture_output=True)
        (private/(c+'-killed-'+str(trial)+'.txt')).write_bytes(kernel.stdout+kernel.stderr)
        time.sleep(20)
    f=out/(c+'.log');f.write_bytes(raw);s=raw.decode(errors='replace')
    expectedSymbol={'AtomNetSessionNumericalProbe':'B04_ACTUAL_SESSION_NUMERICS','AtomNetSessionResumeReviewTest':'PASS_EXPECTED_REVISION_BEHAVIOR','R335AndroidGradientTrace':'R335_ANDROID_GRADIENT_TRACE','ResearchConsumerR335Host':'R335_ACTUAL_CONSUMER_PURE','R33NaturalQualificationHost':'HOST_PASS assertions=31 scenarios=9'}[c]
    ok=proc.returncode==0 and expectedSymbol in s and 'Exception in thread' not in s
    res['cases'].append({'class':c,'startUtc':start,'endUtc':time.time(),'exitCode':proc.returncode,'pass':ok,'rawLog':f.name,'rawSha256':hashlib.sha256(raw).hexdigest(),'command':invocation,'attempts':trials});save()
    print(c,ok,flush=True)
assert all(row['pass'] for row in res['cases'])
