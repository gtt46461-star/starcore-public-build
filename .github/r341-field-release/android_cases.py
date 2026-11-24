"""Capture native Android test results and actual upgrade on the disposable API34 guest."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys,zipfile

root=Path(os.environ['GITHUB_WORKSPACE'])
temp=Path(os.environ['RUNNER_TEMP'])
repo=root/'app-source'
plan=Path(os.environ['STARCORE_R341_PLAN_ROOT'])
output=temp/'engineering'
output.mkdir(exist_ok=True)
source=os.environ['SOURCE'];stage=os.environ['STAGE'];version=int(os.environ['VERSION'])
pkg='com.starcore.research.r13'
receipt=dict(sourceCommit=source,versionCode=version,deviceSerial=None,api=None,upgrade={},cases=[])
def save():
    (output/'device-run.json').write_text(json.dumps(receipt,indent=2)+'\n')
def run(cmd,*,cwd=None,check=True,timeout=240):
    p=subprocess.run([str(x) for x in cmd],cwd=cwd,capture_output=True,text=True,timeout=timeout)
    if check and p.returncode:raise RuntimeError('ANDROID_COMMAND_FAILED:'+Path(str(cmd[0])).name)
    return p
def adb(*args):return run(['adb',*args]).stdout.strip()
def sha(file):
    with file.open('rb') as stream:return hashlib.file_digest(stream,'sha256').hexdigest()
def installed(known):
    out=adb('shell','dumpsys','package',pkg)
    found=re.search(r'\bversionCode=(\d+)\b',out);uid=re.search(r'\b(?:userId|appId)=(\d+)\b',out)
    assert found and int(found[1])==known and uid
    path=adb('shell','pm','path',pkg).split('package:',1)[1]
    return dict(versionCode=int(found[1]),uid=int(uid[1]),apkSha256=adb('shell','sha256sum',path).split()[0])

def covers_122():
    assert 'Success' in adb('install',temp/'baseline122.apk')
    before=installed(577311122)
    candidate,=sorted((temp/'candidate').glob('*-release.apk'))
    assert before['apkSha256']==sha(temp/'baseline122.apk')
    assert 'Success' in adb('install','-r',candidate)
    after=installed(version)
    assert before['uid']==after['uid'] and after['apkSha256']==sha(candidate)
    receipt['upgrade']={'baseline':before,'installed':after,'scope':'DISPOSABLE_ANDROID_API34_SAME_SIGNER_COVER_INSTALL'}
    save()
    assert 'Success' in adb('uninstall',pkg)

def case_run(case):
    ident=case['id']
    dst=output/ident;dst.mkdir()
    argv=json.loads(run([sys.executable,'-B',plan/'tools/dispatch5.py','--plan',plan,'--case',ident],cwd=repo).stdout)
    scope=next(v.split('=',1)[1] for v in argv if v.startswith('-Pandroid.testInstrumentationRunnerArguments.r30Sandbox='))
    results_path=repo/'app/build/outputs/androidTest-results/connected'
    if results_path.is_dir():shutil.rmtree(results_path)
    (dst/'argv.json').write_text(json.dumps(argv,indent=2)+'\n')
    started=int(adb('shell','date','+%s'))
    with (dst/'gradle-case.log').open('w') as log:
        result=subprocess.run(argv,cwd=repo,stdout=log,stderr=subprocess.STDOUT,timeout=1200)
    logs=run(['adb','logcat','-d','-v','brief','STARCORE_R341:I','*:S']).stdout
    logs=''.join(line+'\n' for line in logs.splitlines() if 'R341_INSTRUMENTATION ' in line and scope in line)
    (dst/'android-observation.log').write_text(logs)
    junit=[]
    for j,xml in enumerate(sorted(results_path.rglob('*.xml'))):
        target=dst/f'native-result-{j}.xml'
        shutil.copyfile(xml,target);junit.append(dict(name=target.name,sha256=sha(target)))
    receipt['cases'].append(dict(id=ident,exitCode=result.returncode,scope=scope,junit=junit,
         deviceLogSha256=sha(dst/'android-observation.log'),observedMarkers=len(logs.splitlines()),beganOnGuest=started))
    save()
    return result.returncode==0 and bool(junit) and len(logs.splitlines())==2

def run_full_report_readback():
    dest=output/'production-report-service'
    dest.mkdir()
    results_path=repo/'app/build/outputs/androidTest-results/connected'
    if results_path.is_dir():
        shutil.rmtree(results_path)
    argv=['./gradlew',':app:connectedDebugAndroidTest','-Pr30Instrumentation=true',
          '-Pandroid.testInstrumentationRunnerArguments.class=com.ethk3.novaonepro.R31QDeliveryInstrumentedTest#fullReportReachesPublishedZipAndProviderReadback',
          '-Pandroid.testInstrumentationRunnerArguments.r30Sandbox=R341_B12_FULL_REPORT_NATIVE',
          '-Pandroid.testInstrumentationRunnerArguments.r31qRealService=true',
          '-Pandroid.testInstrumentationRunnerArguments.r341IncludeRaw=true']
    (dest/'argv.json').write_text(json.dumps(argv,indent=2)+'\n')
    with (dest/'gradle-case.log').open('w') as log:
        result=subprocess.run(argv,cwd=repo,stdout=log,stderr=subprocess.STDOUT,timeout=900)
    logs=run(['adb','logcat','-d','-v','brief','STARCORE_R341:I','*:S']).stdout
    marker=[line for line in logs.splitlines() if re.search(r'FULL_REPORT_PROVIDER_READBACK\|[0-9a-f]{64}\|bytes=[1-9][0-9]*$',line)]
    (dest/'android-observation.log').write_text(''.join(line+'\n' for line in marker))
    junit=[]
    for j,xml in enumerate(sorted(results_path.rglob('*.xml'))):
        copied=dest/f'native-result-{j}.xml'
        shutil.copyfile(xml,copied)
        junit.append(dict(name=copied.name,sha256=sha(copied)))
    receipt['reportExport']={'scope':'DISPOSABLE_API34_ANDROID_DEBUG_DIAGNOSTICS_PROCESS',
        'includesRequestedRawDatabase':result.returncode==0 and len(marker)==1,
        'exitCode':result.returncode,'deviceMarkerCount':len(marker),'junit':junit,
        'deviceLogSha256':sha(dest/'android-observation.log')}
    save()
    return result.returncode==0 and bool(junit) and len(marker)==1

try:
    receipt['deviceSerial']=adb('get-serialno')
    receipt['api']=int(adb('shell','getprop','ro.build.version.sdk'))
    assert receipt['api']==34 and receipt['deviceSerial'].startswith('emulator-')
    assert adb('shell','getprop','ro.kernel.qemu')=='1'
    adb('root')
    adb('wait-for-device')
    covers_122()
    cases=[c for c in json.loads((plan/'ACCEPTANCE_MATRIX.json').read_bytes())['cases']
           if c['batch']==stage and c['level']=='ENGINEERING' and c['test_contract'].get('android_scope_contract')]
    if stage=='B01':
        assert len(cases)==8
    if stage=='B12':
        # Cumulative smoke proofs on the exact final candidate source. These
        # are prior Android cases and are never reported as the missing B12
        # three-generation or physical-device witness.
        replay=['R341-B06-02-A01','R341-B07-02-A01','R341-B08-01-A03',
                'R341-B09-02-A02','A-B10-02b','A-B10-05b','A-B11-04b','A-B11-06b']
        if version>=577311144:
            replay+=['R341-B01-04-T01','A-B11-02a']
        if version>=577311145:
            replay+=['R341-B06-01-A01','R341-B06-03-A02','R341-B06-05-A02']
        if version>=577311146:
            replay+=['A-B11-01b']
        available={case['id']:case for case in json.loads((plan/'ACCEPTANCE_MATRIX.json').read_bytes())['cases']}
        cases=[available[ident] for ident in replay]
        receipt['scope']='FINAL_CANDIDATE_CUMULATIVE_EMULATOR_SMOKE'
    outcomes=[case_run(c) for c in cases]
    if stage=='B12' and version>=577311144:
        outcomes.append(run_full_report_readback())
    assert all(outcomes),'REGISTERED_ANDROID_PROBES_NOT_ALL_PASSED'
except Exception as error:
    receipt['failureType']=type(error).__name__
    if re.fullmatch('[A-Z0-9_:]{1,100}',str(error)):receipt['failureCode']=str(error)
    raise
finally:
    save()
