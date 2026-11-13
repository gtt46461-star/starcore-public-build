#!/usr/bin/env python3
"""Execute production memory/scoped-feature code, then compare existing protected golden fingerprints."""
from datetime import datetime,timezone
import hashlib,json,os,re,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
TESTS=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2'
HOST=ROOT/'tools/r31/memory_host'
OUT=ROOT/os.environ.get('R31_MEMORY_OUTPUT','verification/r31/memory')
BASELINE='5237b265a27fe5ad27fd842901c445d5f8c27749'
NAMES=['MemoryBudgetR31','SharedFeatureStoreR31','BoundedResearchExecutorR31','FeatureCacheR27','PolicyAstR27','ReplayDatasetR27','EvalContextR27','BirthGateR27','ReplayEvaluatorR27']
DEPENDENCIES=['PitGuardR30','EvolutionLoopR27','NullEvolutionR27','NullPipelineSimulationR30','SetIntersectionPolicyR30','OperatorGoldenR27']
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    classes=Path(os.environ.get('R31_MEMORY_CLASSES',str(ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes')))
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
        test_names=['R31FeatureAdmissionReclaimTest','R31MemoryGovernanceTest','R31SharedFeatureIntegrationTest','R31AllocatedMemoryLifecycleTest','R31ScopedAstLifecycleTest','R31PrivateBackingReuseTest','R31FamilyScopeIsolationTest','R27LeakageTest','R30ModelSurfaceTest','R27TestSupport','R27BirthGateTest','R30PolicyContractTest']
        paths=[*[SOURCE/(n+'.java') for n in NAMES+DEPENDENCIES],*[TESTS/(n+'.java') for n in test_names],*sorted(HOST.glob('*.java')),Path(__file__).resolve()]
        source_before={str(p.relative_to(ROOT)):sha(p) for p in paths}
        run([javac,'-cp',cp,'-d',current,*[SOURCE/(n+'.java') for n in NAMES+DEPENDENCIES],*[TESTS/(n+'.java') for n in test_names],*sorted(HOST.glob('*.java'))],'javac.log')
        current_cp=str(current)+os.pathsep+runtime_cp
        log=run([java,'-Xmx512m','-cp',current_cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R31FeatureAdmissionReclaimTest','com.ethk3.novaonepro.strategyv2.R31MemoryGovernanceTest','com.ethk3.novaonepro.strategyv2.R31SharedFeatureIntegrationTest','com.ethk3.novaonepro.strategyv2.R31AllocatedMemoryLifecycleTest','com.ethk3.novaonepro.strategyv2.R31ScopedAstLifecycleTest','com.ethk3.novaonepro.strategyv2.R31PrivateBackingReuseTest','com.ethk3.novaonepro.strategyv2.R31FamilyScopeIsolationTest'],'memory-integration-junit.log')
        count=int(re.search(r'OK \((\d+) tests\)',log).group(1))
        original=run([java,'-Xmx512m','-cp',current_cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R27LeakageTest','com.ethk3.novaonepro.strategyv2.R30ModelSurfaceTest'],'original-pit-model-junit.log')
        original_count=int(re.search(r'OK \((\d+) tests\)',original).group(1))
        old=[]
        # The negative control must use its own golden consumer: the current scoped
        # consumer cannot link against the baseline AST, which predates scoped scores.
        for name in ['FeatureCacheR27','PolicyAstR27','ReplayDatasetR27','ReplayEvaluatorR27','OperatorGoldenR27']:
            path=temp/'baseline-source/com/ethk3/novaonepro/strategyv2'/(name+'.java');path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(subprocess.check_output(['git','show',BASELINE+':'+str((SOURCE/(name+'.java')).relative_to(ROOT))],cwd=ROOT));old.append(path)
        run([javac,'-cp',cp,'-d',baseline,*old,HOST/'FeatureGoldenR31Host.java'],'baseline-javac.log')
        golden=ROOT/'app/src/main/assets/r27/golden_512.bin'
        before=json.loads(run([java,'-Xmx512m','-cp',str(baseline)+os.pathsep+runtime_cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'baseline-golden.json'))
        after=json.loads(run([java,'-Xmx512m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'current-golden.json'))
        if before!=after:raise RuntimeError('protected golden operator fingerprints changed')
        pid_samples=[]
        for index in range(2):pid_samples.append(json.loads(run([java,'-Xmx512m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.MemoryPidR31Host'],f'actual-host-pid-{index}.json')))
        assert pid_samples[0]['pid']!=pid_samples[1]['pid']
        for sample in pid_samples:assert sample['scope']=='PER_PID' and sample['crossProcessBudget']=='NOT_COORDINATED'
        factory=[]
        for mode in ['healthy','resume']:factory.append(json.loads(run([java,'-Xmx512m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.FactoryPressureR31Host',mode],f'factory-real-process-{mode}.json')))
        for key in ['events','scoreRows','champion','rngState']:assert factory[0][key]==factory[1][key],('factory pressure replay mismatch',key)
        assert factory[1]['oldWorkerActuallyTerminated'] and factory[1]['cursorAndCandidatesPreserved'] and factory[1]['candidateCount']==128
        live_reserve=json.loads(run([java,'-Xmx512m','-cp',current_cp,'com.ethk3.novaonepro.strategyv2.LiveReserveR31Host'],'live-reserve-actual-heap.json'))
        assert live_reserve['criticalContextRestored'] and live_reserve['optionalWait']=='DECLARED_CAPACITY' and not live_reserve['forcedGc']
    source_after={str(p.relative_to(ROOT)):sha(p) for p in paths}
    if source_after!=source_before:raise RuntimeError('source/dependency changed during targeted verification; refuse mixed-version receipt')
    receipt=dict(card='R31-B05-C11',phase='P02',state='BLOCKED',implementedScope='MEMORY_ACCOUNTING_SCOPED_CONSUMERS_SOURCE_COMPLETE_TARGETED_HOST_PASS',baseline=BASELINE,inheritedP01Commit='844a78488f396189c2db98591782b7bb214799d2',evaluatedAt=datetime.now(timezone.utc).isoformat(),sourceCommit='UNCOMMITTED_P02_ROOT_INTEGRATION_PENDING',
        actualChanges={
            'MemoryBudgetR31':['per-PID measured heap/native/PSS/RSS separately','declared estimates never represented as measured heap','0.75/0.60/0.90 watermarks and >=0.15 realtime reserve','explicit native capacity config; no Java-derived native hard cap','cancel/grow/actual-holder-finally idempotent leases','OS observation separate from unknown probe','wrapped process-fatal propagation','only proven allocated primitive payload covered by a post-allocation heap sample stops counting as a future promise; owner ledger remains','stale-probe allocation race remains conservative','one bounded cache-pressure reclaim and actual remeasurement; never force GC or subtract dropped refs','fixed private primitive backing observed collection may close only its own immutable-size declaration; generic working/grow promises retain explicit lifetime','real optional-denial metadata distinct from heap high-water and from hypothetical last-request recheck'],
            'SharedFeatureStoreR31':['immutable private primitive blocks','dataset/PIT/operator ABI/normalizer/feature key','reader reference count and byte LRU','public publication still clones and public arrays never enter the reuse pool','only private built-in backing may be rebound after its entry is evicted and its last reader actually exits','one original byte capacity includes entries, idle pool, private working loans and in-flight producers','partial cancellation and duplicate publication return the exclusive private backing only from actual finally','every new built-in target is completely initialized before production','working-loan metadata cannot strongly root an abandoned array; observed collection reaps its declaration','no mutable model/optimizer pooling or shared publication'],
            'BoundedResearchExecutorR31':['2*workers ArrayBlockingQueue plus queued payload byte bound','typed WAIT; no CallerRuns or silent drop','queued cancel removes callable; running cancel retains owner/leases through actual body finally','standard Future cancellation and explicit owner/byte submit','worker-start failure cleanup'],
            'FeatureCacheR27':['authorized scope extension for actual shared consumer','scoped immutable leaf readers and private built-in target production','nested built-in frequency/mean-gap/intersection inputs close scoped immutable readers without allocating escaped raw dependencies','unique instance isolation until actual golden ABI bound','override/model/family states never enter shared immutable keys','raw cache count+byte bound; escaped public arrays are never recycled','weak cache/array registrations and pressure trim with no production forced GC','familyInto fills only an internal exclusive scoped output; public family leaf retains its independent array','same-length family advance checks all prior prefix guards then retains its private matrix without copying or updating model state','extendFrom deep-copies the existing model codec plus state/cursors/matrix; parent mutable state is never shared or reset','new family fork admission includes reflected private model workspace, codec copies and retained matrix; initial non-fork model construction remains an explicitly uncovered owner','close requests fork-owner release after actual active use ends; orphan fork declaration requires observed owner collection'],
            'PolicyAstR27':['scoped evaluation consumes immutable shared leaf readers','private intermediate byte-bounded scratch buffers reused only after actual scope close','shared-store scratch respects the original per-cache cap and stricter shared entry/idle/working/inflight cap','last actual score scope returns its exclusive working backing to global idle rather than monopolizing it in an idle cache','internal family consumer fills an exclusive numeric scratch array without pooling model weights or public family outputs','public returned arrays remain independent and never recycled','all operator arithmetic order and per-node model masking retained','live explicit critical scope reaches output scratch, cold leaves and nested dependencies; history remains optional'],
            'ReplayDatasetR27':['authorized withDraws-only deep-copy of supplied replacement rows; public arrays/append/tail remain residual mutable API'],
            'EvalContextR27':['strong public score memo byte LRU <= min(8MiB,maxHeap/64) plus original 96 entries','physical output identity accounting prevents duplicate retained-byte declarations','internal temperature/log-loss/correlation and replay borrow private scoped values without a public output copy','pressure/close drop memo references only; escaped score arrays are never reused or claimed freed','concurrent close does not repopulate score/failure memo'],
            'BirthGateR27':['actual RESOURCE_WAIT previously recorded as RANGE:EVAL:CapacityWait now propagates through common scheduler classification','cancellation/retry/stale-owner/process-fatal do not permanently reject valid candidates','all AST/quality/K3/cost gates preserved'],
            'ReplayEvaluatorR27':['actual role-checked production replay and live input consumers close scoped scores in finally','per-pass sorted K3/PIT K3/QUANTILE scratch replaces row allocations without changing ordering or statistical opportunities','public trace arrays remain independent and are not recycled'],
        },sourceSha256=source_before,sourceStableDuringVerification=True,compiledProductionDependencies=DEPENDENCIES,
        tests=dict(newJUnit=count,existingPitAndModelJUnit=original_count,registeredAtoms=240,protectedGoldenFingerprints=len(before),protectedGoldenMatch=True,parallelCandidates=64,duplicatePublicationRacers=32,queueRequests=1000,realCancellationFinallyObserved=True,rawAliasGcObservation='ISOLATED_TEST_ONLY_NO_PRODUCTION_FORCED_GC'),
        hostPidMeasurements=dict(scope='ACTUAL_HOST_JVM_PER_PID_NOT_ANDROID_DEVICE',pids=[s['pid'] for s in pid_samples],crossPidAdmission='NOT_COORDINATED',nativeAndroidCalibration='NOT_RUN'),
        factoryPressure=dict(scope='ACTUAL_DEFAULT_RUNTIME_PROBE_512M_HOST_WITH_INJECTED_OS_FLAG',inputRows=5000,candidates=128,completeOriginalNineSteps=True,oldWorkerActuallyTerminated=True,cursorAndCandidatesPreserved=True,eventsAndNumericScoresEqualToHealthy=True,forcedGc=False,device='NOT_RUN',quality='NOT_EVALUATED'),
        liveReserve=dict(scope='ACTUAL_DEFAULT_RUNTIME_PROBE_REAL_HEAP_REGISTERED_HELD_OWNER',payloadBytes=live_reserve['registeredPressurePayloadBytes'],coldNestedLeafAndScratchCritical=True,historyStillWaits=True,forcedGc=False,device='NOT_RUN'),
        integration=dict(androidCompile='PENDING_ROOT_JOINT',apk=None,nativeCapacities='CALLER_REGISTERED_STAGE_LIMITS_NOT_DEVICE_CALIBRATION',javaAdmission='MEASURED_HEAP_PLUS_UNALLOCATED_PROMISES; PROVEN_PRIMITIVE_PAYLOAD_REQUIRES_FRESH_SAMPLE',fullUnitSuite='FAIL_PENDING_ROOT_REBUILD; TARGETED_TESTS_DO_NOT_OVERRIDE_FULL_SUITE'),
        naturalCapacityWaitEvidence=['verification/r31/p02/build-identity/committed-unexcluded-joint-receipt.json','verification/r31/p02/memory-suite-repair/fix5-failure-details.json','verification/r31/p02/memory-independent-review','verification/r31/p02/memory-owned-lifecycle'],
        uncoveredOwners=['escaped public score/trace result allocation estimates not fully registered; byte-LRU drops references only','initial/rebuilt non-fork mutable family model construction and 100-column matrix growth still lack complete pre-allocation owner admission; deep-fork coverage does not imply whole model-owner coverage','per-period model-kernel and leaf internal temporary scratch allocations','public ReplayDataset arrays and append/tail row aliasing','all unregistered native/SQLite/bitmap owners','other application/process allocations not integrated with this PID controller'],
        remainingAcceptance=['Android joint integration and owner coverage audit','device heap/native/PSS/RSS baseline and calibrated realtime P99 reserve','100 Activity/panel cycles with real heap dominators','native/FD/SQLite cancellation resource envelope','cross-PID aggregate admission requires explicit IPC/file control; currently NOT_COORDINATED','24h/72h comparable-load live heap/PSS/OOM acceptance NOT_RUN'],commands=commands)
    (OUT/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');(OUT/'FAILED_COMMAND.json').unlink(missing_ok=True)
    print(json.dumps(dict(status='HOST_PASS',newJUnit=count,existingPitModelJUnit=original_count,goldenFingerprints=len(before),goldenMatch=True,crossPidAdmission='NOT_COORDINATED',device='NOT_RUN',naturalQuality='NOT_EVALUATED')))
if __name__=='__main__':main()
