#!/usr/bin/env python3
"""Execute production memory/scoped-feature code, then compare existing protected golden fingerprints."""
from datetime import datetime,timezone
import hashlib,json,os,re,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
TESTS=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2'
HOST=ROOT/'tools/r31/memory_host'
OUT=ROOT/'verification/r31/memory'
BASELINE='5237b265a27fe5ad27fd842901c445d5f8c27749'
NAMES=['MemoryBudgetR31','SharedFeatureStoreR31','BoundedResearchExecutorR31','FeatureCacheR27','PolicyAstR27','ReplayDatasetR27']
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    classes=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    gradle=Path(os.environ.get('GRADLE_USER_HOME','/workspace/starcore-environment/gradle-home'))
    junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/junit-4.13.2.jar'))
    hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/hamcrest-core-1.3.jar'))
    android=Path(os.environ.get('ANDROID_HOME','/workspace/starcore-environment/android-sdk'))/'platforms/android-36/android.jar'
    cp=os.pathsep.join(map(str,[classes,android,junit,hamcrest]))
    runtime_cp=os.pathsep.join(map(str,[classes,junit,hamcrest]))
    java,javac=shutil.which('java'),shutil.which('javac')
    commands=[]
    def run(command,log,timeout=180):
        p=subprocess.run(list(map(str,command)),cwd=ROOT,text=True,capture_output=True,timeout=timeout)
        (OUT/log).write_text(p.stdout+p.stderr)
        commands.append(dict(command=list(map(str,command)),log=log,exitCode=p.returncode))
        if p.returncode:
            (OUT/'FAILED_COMMAND.json').write_text(json.dumps(commands[-1],indent=2)+'\n')
            raise RuntimeError(log)
        return p.stdout
    with tempfile.TemporaryDirectory(prefix='r31-memory-') as d:
        temp=Path(d);current=temp/'current';baseline=temp/'baseline';current.mkdir();baseline.mkdir()
        test_names=['R31MemoryGovernanceTest','R31SharedFeatureIntegrationTest','R27LeakageTest','R30ModelSurfaceTest','R27TestSupport']
        run([javac,'-cp',cp,'-d',current,*[SOURCE/(n+'.java') for n in NAMES],*[TESTS/(n+'.java') for n in test_names],*sorted(HOST.glob('*.java'))],'javac.log')
        current_cp=str(current)+os.pathsep+runtime_cp
        log=run([java,'-Xmx768m','-cp',current_cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R31MemoryGovernanceTest','com.ethk3.novaonepro.strategyv2.R31SharedFeatureIntegrationTest'],'memory-integration-junit.log')
        count=int(re.search(r'OK \((\d+) tests\)',log).group(1))
        original=run([java,'-Xmx768m','-cp',current_cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R27LeakageTest','com.ethk3.novaonepro.strategyv2.R30ModelSurfaceTest'],'original-pit-model-junit.log')
        original_count=int(re.search(r'OK \((\d+) tests\)',original).group(1))
        old=[]
        for name in ['FeatureCacheR27','PolicyAstR27','ReplayDatasetR27']:
            path=temp/'baseline-source/com/ethk3/novaonepro/strategyv2'/(name+'.java');path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(subprocess.check_output(['git','show',BASELINE+':'+str((SOURCE/(name+'.java')).relative_to(ROOT))],cwd=ROOT));old.append(path)
        run([javac,'-cp',cp,'-d',baseline,*old,HOST/'FeatureGoldenR31Host.java'],'baseline-javac.log')
        golden=ROOT/'app/src/main/assets/r27/golden_512.bin'
        before=json.loads(run([java,'-Xmx768m','-cp',str(baseline)+os.pathsep+runtime_cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'baseline-golden.json'))
        after=json.loads(run([java,'-Xmx768m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'current-golden.json'))
        if before!=after:raise RuntimeError('protected golden operator fingerprints changed')
        pid_samples=[]
        for index in range(2):pid_samples.append(json.loads(run([java,'-Xmx768m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.MemoryPidR31Host'],f'actual-host-pid-{index}.json')))
        assert pid_samples[0]['pid']!=pid_samples[1]['pid']
        for sample in pid_samples:assert sample['scope']=='PER_PID' and sample['crossProcessBudget']=='NOT_COORDINATED'
    paths=[*[SOURCE/(n+'.java') for n in NAMES],*[TESTS/(n+'.java') for n in test_names[:2]],*HOST.glob('*.java'),Path(__file__).resolve()]
    receipt=dict(card='R31-B05-C11',phase='P02',state='CODE_COMPLETE',baseline=BASELINE,inheritedP01Commit='844a78488f396189c2db98591782b7bb214799d2',evaluatedAt=datetime.now(timezone.utc).isoformat(),sourceCommit='UNCOMMITTED_P02_ROOT_INTEGRATION_PENDING',
        actualChanges={
            'MemoryBudgetR31':['per-PID measured heap/native/PSS/RSS separately','declared estimates never represented as measured heap','0.75/0.60/0.90 watermarks and >=0.15 realtime reserve','explicit native capacity config; no Java-derived native hard cap','cancel/grow/actual-holder-finally idempotent leases','OS observation separate from unknown probe','wrapped process-fatal propagation'],
            'SharedFeatureStoreR31':['immutable private primitive blocks','dataset/PIT/operator ABI/normalizer/feature key','reader reference count and byte LRU','duplicate publication closes unadopted clone declarations','no mutable array/model state exposure'],
            'BoundedResearchExecutorR31':['2*workers ArrayBlockingQueue plus queued payload byte bound','typed WAIT; no CallerRuns or silent drop','queued cancel removes callable; running cancel retains owner/leases through actual body finally','standard Future cancellation and explicit owner/byte submit','worker-start failure cleanup'],
            'FeatureCacheR27':['authorized scope extension for actual shared consumer','scoped raw-leaf reader for AST rowwise operators','unique instance isolation until actual golden ABI bound','override/model/family paths do not reuse shared blocks','raw cache count+byte bound','global weak array tracking returns declared lease only after observed GC','weak cache registry and pressure trim; no forced production GC','close drops only own retained references'],
            'PolicyAstR27':['rank/zscore/softmax direct-leaf hot path consumes immutable shared reader in 10-element rows','original arithmetic order retained','direct model leaf copied before missing-row mask to avoid mutating cached input'],
            'ReplayDatasetR27':['authorized withDraws-only deep-copy of supplied replacement rows; public arrays/append/tail remain residual mutable API'],
        },sourceSha256={str(p.relative_to(ROOT)):sha(p) for p in paths},
        tests=dict(newJUnit=count,existingPitAndModelJUnit=original_count,registeredAtoms=240,protectedGoldenFingerprints=len(before),protectedGoldenMatch=True,parallelCandidates=64,duplicatePublicationRacers=32,queueRequests=1000,realCancellationFinallyObserved=True,rawAliasGcObservation='ISOLATED_TEST_ONLY_NO_PRODUCTION_FORCED_GC'),
        hostPidMeasurements=dict(scope='ACTUAL_HOST_JVM_PER_PID_NOT_ANDROID_DEVICE',pids=[s['pid'] for s in pid_samples],crossPidAdmission='NOT_COORDINATED',nativeAndroidCalibration='NOT_RUN'),
        integration=dict(androidCompile='PENDING_ROOT_JOINT',apk=None,nativeCapacities='CALLER_REGISTERED_STAGE_LIMITS_NOT_DEVICE_CALIBRATION',javaAdmission='CONSERVATIVE_HEAP_PLUS_DECLARATIONS_MAY_DOUBLE_COUNT'),
        uncoveredOwners=['AST returned score arrays/EvalContext scoreMemo lifecycle','mutable family model state and existing extendFrom shared stream semantics','leaf internal temporary scratch allocations','public ReplayDataset arrays and append/tail row aliasing','all unregistered native/SQLite/bitmap owners','other application/process allocations not integrated with this PID controller'],
        remainingAcceptance=['Android joint integration and owner coverage audit','device heap/native/PSS/RSS baseline and calibrated realtime P99 reserve','100 Activity/panel cycles with real heap dominators','native/FD/SQLite cancellation resource envelope','cross-PID aggregate admission requires explicit IPC/file control; currently NOT_COORDINATED','24h/72h comparable-load live heap/PSS/OOM acceptance NOT_RUN'],commands=commands)
    (OUT/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');(OUT/'FAILED_COMMAND.json').unlink(missing_ok=True)
    print(json.dumps(dict(status='HOST_PASS',newJUnit=count,existingPitModelJUnit=original_count,goldenFingerprints=len(before),goldenMatch=True,crossPidAdmission='NOT_COORDINATED',device='NOT_RUN',naturalQuality='NOT_EVALUATED')))
if __name__=='__main__':main()
