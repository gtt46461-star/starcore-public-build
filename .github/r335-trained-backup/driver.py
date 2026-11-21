"""Export one controlled trained model through the production UI on a fresh disposable Android installation."""
from contextlib import redirect_stderr, redirect_stdout
import hashlib,importlib.util,json,os,re,sqlite3,subprocess,traceback
from pathlib import Path
import time
import zipfile

temp=Path(os.environ['RUNNER_TEMP'])
workspace=Path(os.environ['GITHUB_WORKSPACE'])
kit=workspace/'.github/r335-trained-backup'
run=int(os.environ['GITHUB_RUN_ID'])
doc=dict(runId=run,scope='CONTROLLED_TRAINING_CHECKPOINT_IN_GENUINE_FULL_RAW_UI_EXPORT',
         sourceCommit='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d',apkSha256='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced',
         registeredMethod='confirmedDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority',
         trainedAndReopened=False,phase='START')
def verify(v,code):
    if not v:raise ValueError(code)
def catalog_observation(reader, archive):
    contract=json.loads((temp/'material/STARCORE_R33_5_20261118/B01/scripts/report_expected_ids_r334.json').read_bytes())
    known=set(contract['checks'])
    assert len(known)==96
    result={'incompleteCheckIds':[],'errorKinds':{},'catalogPages':0,'observedChecks':0}
    states={'EXECUTED','READ_ERROR','UNCOMPUTED','NOT_RUN','MISSING','CONDITIONAL','NO_DATA','OBSERVED','PASS','FAIL','SKIP'}
    with zipfile.ZipFile(archive) as z:
        footer=json.loads(z.read('ARCHIVE_MANIFEST.jsonl').splitlines()[-1])
        assert footer['kind']=='footer'
        for key in ('evidence_complete','checks_complete'):
            assert type(footer[key])==bool
            result[key]=footer[key]
        for name in z.namelist():
            if not re.fullmatch(r'catalog-[0-9]{4,}\.jsonl',name):continue
            result['catalogPages']+=1
            with z.open(name) as records:
                for line in records:
                    row=json.loads(line)
                    kind=row.get('kind')
                    if kind in ('table_error','index_error','catalog_error'):
                        counts=result['errorKinds']
                        counts[kind]=counts.get(kind,0)+1
                    if kind!='check_result':continue
                    check=row['id'];assert check in known
                    result['observedChecks']+=1
                    if row.get('completes_contract') is not True:
                        state=row.get('execution_state')
                        result['incompleteCheckIds'].append({'id':check,'executionState':state if state in states else 'OTHER'})
    return result
def main():
    spec=importlib.util.spec_from_file_location('native_reader',workspace/'.github/r335-raw-closure/driver.py')
    n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n);f=n.field;pkg=f.PKG
    verify(f.adb('get-serialno').decode().startswith('emulator-') and f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1','GUEST')
    f.adb('root');f.adb('wait-for-device')
    verify(not f.adb('shell','pm','list','packages',pkg).strip(),'FRESH_GUEST')
    candidate=temp/'protocol-candidate';apk=next(candidate.glob('*-release.apk'));test=candidate/'original-su1-androidTest.apk'
    verify(f.fingerprint(apk)==doc['apkSha256'],'APK_SHA')
    f.adb('install',str(apk));f.adb('install',str(test))
    identity=f.installed(f.EVID/'trained-backup-package.txt',577311065);verify(identity[1]==doc['apkSha256'],'INSTALLED_APK')
    doc['phase']='REAL_TRAINING'
    component=pkg+'.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'
    cmd=['adb','shell','am','instrument','-w','-r','-e','class','com.ethk3.novaonepro.strategyv2.R335TrainedBackupTest#'+doc['registeredMethod'],component]
    proc=subprocess.run(cmd,capture_output=True,timeout=420);log=proc.stdout+proc.stderr
    (temp/'trained-backup-instrumentation-private.log').write_bytes(log)
    doc.update(instrumentationExit=proc.returncode,testFixtureSha256=f.fingerprint(kit/'R335TrainedBackupTest.java'),trainingLogSha256=hashlib.sha256(log).hexdigest())
    if not (proc.returncode==0 and re.search(rb'OK\s*\(1 test\)',log) and b'FAILURES!!!' not in log):
        frames=re.findall(rb'(?:R335TrainedBackupTest|DbStore|DatabaseCoordinator)[.]java:(\d+)\)',log)
        doc['trainingFailureLineNumbers']=[int(x) for x in frames[:8]]
        raise ValueError('NATIVE_TRAINING_TEST')
    checkpoint=f.adb('exec-out','cat','/data/user/0/'+pkg+'/cache/r335-controlled-backup-checkpoint.txt').decode().strip()
    verify(bool(re.fullmatch('[0-9a-f]{64}',checkpoint)),'CHECKPOINT_KEY')
    doc.update(nativeTrainingPassed=True,trainingCheckpointSha256=checkpoint)
    doc['phase']='NORMAL_FULL_RAW_UI'
    if 'origin' not in f.cmd('git','-C',str(f.SRC),'remote').decode().split():
        f.cmd('git','-C',str(f.SRC),'remote','add','origin','https://github.com/'+f.REPO+'.git')
    (f.EVID/'B08').mkdir(exist_ok=True)
    f.adb('shell','pm','grant',pkg,'android.permission.POST_NOTIFICATIONS')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY)
    time.sleep(75)
    f.safe_reports('B08',1,doc['sourceCommit'])
    observation=n.observe_native(f.NATIVE/'B08/1',1,set());doc['attempt']=observation
    archive=n.private/'1/external/joined.zip'
    if not archive.is_file():archive=n.private/'1/joined.zip'
    verify(f.fingerprint(archive)==observation['archiveSha256'],'ARCHIVE_SHA')
    doc['catalogFooter']=catalog_observation(n,archive)
    verify(observation.get('actualSystemPicker') and observation.get('fullRawClosureReplayed') and observation['checkpointBodies']>0 and observation['checkpointBytes']>0 and observation['uniqueSnapshotRoots']>0,'NONEMPTY_RAW_GRAPH_AND_SAF')
    db=n.private/('closure-'+observation['jobId'])/'snapshot.db'
    verify(f.fingerprint(db)==observation['rawSnapshotSha256'],'REOPENED_SNAPSHOT')
    with sqlite3.connect(db.as_uri()+'?mode=ro&immutable=1',uri=True) as c:
        consumed=c.execute('SELECT count(*) FROM r26_consumption WHERE checkpoint_hash=?',(checkpoint,)).fetchone()[0]
        live=c.execute('SELECT count(*) FROM r26_live_model WHERE checkpoint_hash=?',(checkpoint,)).fetchone()[0]
        cert=c.execute('SELECT count(*) FROM su2_certificate').fetchone()[0]
    verify(consumed==1 and live==1 and cert==0,'TRAINING_CURSOR_CHECKPOINT_BINDING')
    verify(f.installed(f.EVID/'trained-backup-returned-package.txt',577311065)==identity,'FINAL_TARGET')
    doc.update(consumedCheckpoints=consumed,liveModelWithExactCheckpoint=live,certificatesInControlledSnapshot=cert)
    verify(observation['fullyVerifiedRawAndSystemPicker'] and observation['strictStatus']=='ARTIFACT_CONSISTENT','ORIGINAL_ARCHIVE_VERIFIER_REFUSED')
    doc.update(trainedAndReopened=True,phase='DONE')
with (temp/'trained-backup-private.log').open('w') as private,redirect_stderr(private),redirect_stdout(private):
    try:main()
    except Exception as error:
        allowed={'GUEST','FRESH_GUEST','APK_SHA','INSTALLED_APK','NATIVE_TRAINING_TEST','CHECKPOINT_KEY','NONEMPTY_RAW_GRAPH_AND_SAF','REOPENED_SNAPSHOT','TRAINING_CURSOR_CHECKPOINT_BINDING','FINAL_TARGET','ARCHIVE_SHA','ORIGINAL_ARCHIVE_VERIFIER_REFUSED'}
        doc['failureCode']=str(error) if str(error) in allowed else type(error).__name__
        doc['failureLineNumbers']=[f.lineno for f in traceback.extract_tb(error.__traceback__) if Path(f.filename).name=='driver.py']
(temp/('R335-trained-backup-'+str(run)+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
print('Controlled final-APK trained export',doc['phase'],doc['trainedAndReopened'],flush=True)
