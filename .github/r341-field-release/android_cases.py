"""Capture native Android test results and actual upgrade on the disposable API34 guest."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys,zipfile
import xml.etree.ElementTree as ET

root=Path(os.environ['GITHUB_WORKSPACE'])
temp=Path(os.environ['RUNNER_TEMP'])
repo=root/'app-source'
plan=Path(os.environ['STARCORE_R341_PLAN_ROOT'])
output=temp/'engineering'
output.mkdir(exist_ok=True)
source=os.environ['SOURCE'];stage=os.environ['STAGE'];version=int(os.environ['VERSION'])
replay_batches=set(os.environ.get('R341_REPLAY_BATCHES','').split(','))-{''}
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
    intermediate=None
    if (temp/'previous150.apk').is_file():
        assert 'Success' in adb('install','-r',temp/'previous150.apk')
        intermediate=installed(577311150)
        assert intermediate['uid']==before['uid'] and intermediate['apkSha256']==sha(temp/'previous150.apk')
    assert 'Success' in adb('install','-r',candidate)
    after=installed(version)
    assert before['uid']==after['uid'] and after['apkSha256']==sha(candidate)
    receipt['upgrade']={'baseline':before,'installed':after,'scope':'DISPOSABLE_ANDROID_API34_SAME_SIGNER_COVER_INSTALL'}
    if intermediate is not None:
        receipt['upgrade']['previousAffectedBuild']=intermediate
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
    device_log=run(['adb','logcat','-d','-v','brief','STARCORE_R341:I','*:S']).stdout
    logs=''.join(line+'\n' for line in device_log.splitlines() if 'R341_INSTRUMENTATION ' in line and scope in line)
    (dst/'android-observation.log').write_text(logs)
    if ident=='A-B10-05b' and version>=577311148:
        states=''.join(line+'\n' for line in device_log.splitlines() if 'R341_STORAGE_RECOVERY ' in line and scope in line)
        (dst/'storage-recovery.log').write_text(states)
    junit=[]
    for j,xml in enumerate(sorted(results_path.rglob('*.xml'))):
        target=dst/f'native-result-{j}.xml'
        shutil.copyfile(xml,target);junit.append(dict(name=target.name,sha256=sha(target)))
    observed=dict(id=ident,exitCode=result.returncode,scope=scope,junit=junit,
         deviceLogSha256=sha(dst/'android-observation.log'),observedMarkers=len(logs.splitlines()),beganOnGuest=started)
    if ident=='A-B10-05b' and version>=577311148:
        observed.update(storageRecoverySha256=sha(dst/'storage-recovery.log'),observedStorageBoundaries=len(states.splitlines()))
    receipt['cases'].append(observed)
    save()
    verified=result.returncode==0 and bool(junit) and len(logs.splitlines())==2
    return verified and (ident!='A-B10-05b' or version<577311148 or len(states.splitlines())==2)

def run_full_report_readback(include_raw=True):
    dense=version>=577311151
    profile='true' if include_raw else 'false'
    dest=output/('production-report-service' if include_raw else 'production-report-diagnostic')
    dest.mkdir()
    if dense and 'package:'+pkg in adb('shell','pm','list','packages','--user','0',pkg).splitlines():
        assert 'Success' in adb('shell','pm','clear',pkg)
    results_path=repo/'app/build/outputs/androidTest-results/connected'
    if results_path.is_dir():
        shutil.rmtree(results_path)
    argv=['./gradlew',':app:connectedDebugAndroidTest','-Pr30Instrumentation=true',
          '-Pandroid.testInstrumentationRunnerArguments.class=com.ethk3.novaonepro.R31QDeliveryInstrumentedTest#fullReportReachesPublishedZipAndProviderReadback',
          '-Pandroid.testInstrumentationRunnerArguments.r30Sandbox=R341_B12_FULL_REPORT_NATIVE',
          '-Pandroid.testInstrumentationRunnerArguments.r31qRealService=true',
          '-Pandroid.testInstrumentationRunnerArguments.r341IncludeRaw='+profile]
    if dense:
        argv.append('-Pandroid.testInstrumentationRunnerArguments.r341DenseCohort=true')
    (dest/'argv.json').write_text(json.dumps(argv,indent=2)+'\n')
    with (dest/'gradle-case.log').open('w') as log:
        result=subprocess.run(argv,cwd=repo,stdout=log,stderr=subprocess.STDOUT,timeout=900)
    logs=run(['adb','logcat','-d','-v','brief','STARCORE_R341:I','*:S']).stdout
    marker=[line for line in logs.splitlines() if include_raw and re.search(r'FULL_REPORT_PROVIDER_READBACK\|[0-9a-f]{64}\|bytes=[1-9][0-9]*$',line)]
    cohorts=[line for line in logs.splitlines() if dense and re.search(
        r'DENSE_REPORT_COHORT\|raw='+profile+r'\|periods=120\|events=2880\|bytes=[1-9][0-9]*\|sha256=[0-9a-f]{64}$',line)]
    (dest/'android-observation.log').write_text(''.join(line+'\n' for line in marker))
    if dense:
        (dest/'dense-cohort.log').write_text(''.join(line+'\n' for line in cohorts))
    junit=[]
    for j,xml in enumerate(sorted(results_path.rglob('*.xml'))):
        copied=dest/f'native-result-{j}.xml'
        shutil.copyfile(xml,copied)
        junit.append(dict(name=copied.name,sha256=sha(copied)))
    record={'scope':'DISPOSABLE_API34_ANDROID_DEBUG_DIAGNOSTICS_PROCESS',
        'includesRequestedRawDatabase':include_raw and result.returncode==0 and len(marker)==1,
        'exitCode':result.returncode,'deviceMarkerCount':len(marker),'junit':junit,
        'deviceLogSha256':sha(dest/'android-observation.log')}
    if dense:
        record.update(denseCohortObservations=len(cohorts),denseCohortSha256=sha(dest/'dense-cohort.log'))
    receipt['reportExport' if include_raw else 'reportExportCompact']=record
    save()
    return result.returncode==0 and bool(junit) and (not include_raw or len(marker)==1) and (not dense or len(cohorts)==1)

def reuse_exact_source_smoke(prior, cases):
    original=prior/'engineering'
    old=json.loads((original/'device-run.json').read_bytes())
    assert (old['sourceCommit'],old['versionCode'],old['api'])==(source,version,34)
    assert [case['id'] for case in old['cases']]==[case['id'] for case in cases]
    for case in old['cases']:
        directory=original/case['id']
        assert case['exitCode']==0 and case['observedMarkers']==2 and len(case['junit'])==1
        assert sha(directory/'android-observation.log')==case['deviceLogSha256']
        witness=case['junit'][0]
        xml=directory/witness['name']
        assert sha(xml)==witness['sha256']
        node,=ET.parse(xml).getroot().iter('testcase')
        assert all(node.find(state) is None for state in ('failure','error','skipped'))
        if 'storageRecoverySha256' in case:
            assert sha(directory/'storage-recovery.log')==case['storageRecoverySha256'] and case['observedStorageBoundaries']==2
        shutil.copytree(directory,output/case['id'])
        receipt['cases'].append({**case,'originalWorkflowRun':45539082278})
    shutil.copyfile(original/'device-run.json',output/'first-attempt-device-run.json')
    receipt['exactSourceSmokeReusedFrom']=45539082278
    save()

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
    if replay_batches:
        assert stage=='B12' and replay_batches <= {f'B{i:02d}' for i in range(1,12)}
        cases=[c for c in json.loads((plan/'ACCEPTANCE_MATRIX.json').read_bytes())['cases']
               if c['batch'] in replay_batches and c['level']=='ENGINEERING'
               and c['test_contract'].get('android_scope_contract')]
        assert {case['batch'] for case in cases}==replay_batches
        receipt['scope']='FINAL_CANDIDATE_CUMULATIVE_ANDROID_RECHECK'
        receipt['replayedBatches']=sorted(replay_batches)
    elif stage=='B12':
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
        if version>=577311150:
            replay+=['A-B11-02b']
        available={case['id']:case for case in json.loads((plan/'ACCEPTANCE_MATRIX.json').read_bytes())['cases']}
        cases=[available[ident] for ident in replay]
        receipt['scope']='FINAL_CANDIDATE_CUMULATIVE_EMULATOR_SMOKE'
    prior=os.environ.get('R341_REPORT_RETRY_PRIOR')
    if prior:
        reuse_exact_source_smoke(Path(prior),cases)
        outcomes=[]
    else:
        outcomes=[case_run(c) for c in cases]
    if stage=='B12' and not replay_batches and version>=577311144:
        outcomes.append(run_full_report_readback())
        if version>=577311151:
            outcomes.append(run_full_report_readback(include_raw=False))
    assert all(outcomes),'REGISTERED_ANDROID_PROBES_NOT_ALL_PASSED'
except Exception as error:
    receipt['failureType']=type(error).__name__
    if re.fullmatch('[A-Z0-9_:]{1,100}',str(error)):receipt['failureCode']=str(error)
    raise
finally:
    save()
