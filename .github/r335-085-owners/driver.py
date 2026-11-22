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
         sourceCommit='02b7e3b46dcec7f1ae274c665c40b7ba1f35d62d',apkSha256=next(line.split()[0] for line in (temp/'SHA256SUMS.txt').read_text().splitlines() if line.endswith('02b7e3b46dce-release.apk')),
         registeredMethod='terminalDrawAdvancesOneTrainingCursorAndGrantsNoNaturalAuthority',
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
                        item={'id':check,'executionState':state if state in states else 'OTHER'}
                        explanation=' '.join(str(row.get(k,'')) for k in ('reason','error_detail','evidence'))
                        item['reasonSha256']=hashlib.sha256(explanation.encode()).hexdigest()
                        item['reasonTypes']=[key for key in ('no such table','no such column','database is locked','database or disk is full','IllegalStateException','NullPointerException','SQLiteException','SQLiteDatabaseLockedException','REPORT_QUERY','REPORT_CONTEXT','REPORT_ASSET','FileNotFoundException') if key.lower() in explanation.lower()]
                        result['incompleteCheckIds'].append(item)
    return result
def main():
    spec=importlib.util.spec_from_file_location('native_reader',workspace/'.github/r335-vivo-final-export/driver.py')
    n=importlib.util.module_from_spec(spec);spec.loader.exec_module(n);n.source=doc['sourceCommit'];n.apk=doc['apkSha256'];f=n.field;pkg=f.PKG
    verify(f.adb('get-serialno').decode().startswith('emulator-') and f.adb('shell','getprop','ro.kernel.qemu').strip()==b'1','GUEST')
    f.adb('root');f.adb('wait-for-device')
    verify(not f.adb('shell','pm','list','packages',pkg).strip(),'FRESH_GUEST')
    candidate=temp/'protocol-candidate';apk=next(candidate.glob('*-release.apk'));test=candidate/'original-su1-androidTest.apk'
    verify(f.fingerprint(apk)==doc['apkSha256'],'APK_SHA')
    prior=('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    downloaded=n.observe.get_apk(*prior);previous=downloaded.with_name('prior-065.apk');downloaded.rename(previous)
    verify(f.fingerprint(previous)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced','PREVIOUS_APK')
    f.adb('shell','cmd','connectivity','airplane-mode','enable')
    f.adb('shell','svc','wifi','disable');f.adb('shell','svc','data','disable')
    verify(f.adb('shell','settings','get','global','airplane_mode_on').strip()==b'1','GUEST_NETWORK_ISOLATION')
    f.adb('install',str(previous))
    f.adb('shell','am','start','-W','-n',f.ACTIVITY)
    time.sleep(70)
    old_uid,old_sha=f.installed(f.EVID/'before-training-upgrade.txt',prior[1])
    verify(old_sha==f.fingerprint(previous),'PREVIOUS_APK')
    f.adb('install','-r',str(apk));f.adb('install',str(test))
    identity=f.installed(f.EVID/'trained-backup-package.txt',577311085);verify(identity[1]==doc['apkSha256'],'INSTALLED_APK')
    verify(identity[0]==old_uid,'UPGRADE_UID')
    f.adb('shell','am','start','-W','-n',f.ACTIVITY)
    time.sleep(90)
    f.adb('shell','am','force-stop',pkg)
    directory=temp/'registered-pretraining-state'
    n.observe.dbprobe.snapshot(directory)
    with sqlite3.connect((directory/n.observe.DB).as_uri()+'?mode=ro',uri=True) as c:
        draws=c.execute('SELECT count(*) FROM draws').fetchone()[0]
        tables=c.execute("SELECT count(*) FROM sqlite_master WHERE type='table'").fetchone()[0]
        verified=c.execute('PRAGMA quick_check').fetchone()[0]=='ok'
    doc['priorInstallation']={'priorApkSha256':old_sha,'packageUidRetained':True,'freshDraws':draws,'tableCount':tables,'sqliteQuickCheck':verified,'guestNetworkIsolated':True}
    verify(draws==0 and tables>0 and verified,'FRESH_UPGRADED_DATABASE')
    doc['phase']='REAL_TRAINING'
    component=pkg+'.test/com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner'
    cmd=['adb','shell','am','instrument','-w','-r','-e','class','com.ethk3.novaonepro.strategyv2.R335TerminalBackupTest#'+doc['registeredMethod'],component]
    proc=subprocess.run(cmd,capture_output=True,timeout=420);log=proc.stdout+proc.stderr
    (temp/'trained-backup-instrumentation-private.log').write_bytes(log)
    derived=workspace/'app-source/app/src/androidTest/java/com/ethk3/novaonepro/strategyv2/R335TerminalBackupTest.java'
    doc.update(instrumentationExit=proc.returncode,testFixtureSha256=f.fingerprint(derived),originalFixtureSha256=f.fingerprint(kit/'R335TrainedBackupTest.java'),trainingLogSha256=hashlib.sha256(log).hexdigest())
    if not (proc.returncode==0 and re.search(rb'OK\s*\(1 test\)',log) and b'FAILURES!!!' not in log):
        frames=re.findall(rb'(?:R335TerminalBackupTest|DbStore|DatabaseCoordinator)[.]java:(\d+)\)',log)
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
    cp=importlib.util.spec_from_file_location('capture',workspace/'.github/r335-085-owners/capture.py')
    capture=importlib.util.module_from_spec(cp);cp.loader.exec_module(capture)
    capture.n=n;capture.f=f;capture.request(1,set());observation=capture.record['attempts'][0];doc['attempt']=observation
    verify(observation.get('phase')=='DONE','NATIVE_REPORT_INCOMPLETE')
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
    verify(f.installed(f.EVID/'trained-backup-returned-package.txt',577311085)==identity,'FINAL_TARGET')
    doc.update(consumedCheckpoints=consumed,liveModelWithExactCheckpoint=live,certificatesInControlledSnapshot=cert)
    verify(observation['fullyVerifiedRawAndSystemPicker'] and observation['strictStatus']=='ARTIFACT_CONSISTENT','ORIGINAL_ARCHIVE_VERIFIER_REFUSED')
    doc.update(trainedAndReopened=True,phase='DONE')
with (temp/'trained-backup-private.log').open('w') as private,redirect_stderr(private),redirect_stdout(private):
    try:main()
    except Exception as error:
        allowed={'GUEST','FRESH_GUEST','APK_SHA','INSTALLED_APK','NATIVE_TRAINING_TEST','CHECKPOINT_KEY','NONEMPTY_RAW_GRAPH_AND_SAF','REOPENED_SNAPSHOT','TRAINING_CURSOR_CHECKPOINT_BINDING','FINAL_TARGET','ARCHIVE_SHA','ORIGINAL_ARCHIVE_VERIFIER_REFUSED','GUEST_NETWORK_ISOLATION','PREVIOUS_APK','UPGRADE_UID','FRESH_UPGRADED_DATABASE'}
        doc['failureCode']=str(error) if str(error) in allowed else type(error).__name__
        doc['failureLineNumbers']=[f.lineno for f in traceback.extract_tb(error.__traceback__) if Path(f.filename).name=='driver.py']
(temp/('R335-085-checkpoints-'+str(run)+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
print('Controlled final-APK trained export',doc['phase'],doc['trainedAndReopened'],flush=True)

subprocess.run([os.sys.executable,'-B',str(workspace/'.github/r335-085-owners/owners.py')],check=True)
