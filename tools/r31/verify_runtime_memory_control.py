#!/usr/bin/env python3
"""Source-bound targeted coordinator test. HOST evidence only; no release or device claim."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,re,shutil,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
OUT=Path(os.environ.get('R31_MEMORY_CONTROL_OUT',str(ROOT/'verification/r31/p02/runtime-memory-control')))
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
NAMES=['RuntimeCoordinatorR26','RuntimeR27','MemoryBudgetR31','SharedFeatureStoreR31','FeatureCacheR27','EvalContextR27','PolicyAstR27','ReplayEvaluatorR27','BirthGateR27','PitGuardR30','EvolutionLoopR27','NullEvolutionR27','NullPipelineSimulationR30']
TEST=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R31RuntimeMemoryControlTest.java'
HOST=ROOT/'tools/r31/runtime_memory_host/RuntimeMemoryControlR31Host.java'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    work=Path(tempfile.mkdtemp(prefix='r31-runtime-memory-control-'));prod=work/'production';host=work/'host';platform=work/'platform'
    for p in [prod,host,platform]:p.mkdir()
    support=Path(os.environ.get('R31_MEMORY_CONTROL_SUPPORT',str(ROOT/'app/build/intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar')));classes=work/'support-classes.jar';shutil.copyfile(support,classes);support_sha=sha(classes)
    local=Path('/workspace/starcore-environment')
    sdk=Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or str(local/'android-sdk'))
    android=sdk/'platforms/android-36/android.jar'
    default_gradle=local/'gradle-home' if (local/'gradle-home').is_dir() else Path.home()/'.gradle'
    gradle=Path(os.environ.get('GRADLE_USER_HOME',str(default_gradle)))/'caches/modules-2/files-2.1'
    if not android.is_file():raise RuntimeError('ANDROID_36_JAR_NOT_FOUND:'+str(android))
    junit=next(gradle.glob('junit/junit/4.13.2/*/junit-4.13.2.jar'));hamcrest=next(gradle.glob('org.hamcrest/hamcrest-core/1.3/*/hamcrest-core-1.3.jar'))
    javac=shutil.which('javac');java=shutil.which('java');commands=[]
    sources=[*[MAIN/(n+'.java') for n in NAMES],TEST,HOST,Path(__file__).resolve()];before={str(p.relative_to(ROOT)):sha(p) for p in sources}
    def run(args,log,timeout=180):
        args=list(map(str,args));p=subprocess.run(args,cwd=ROOT,capture_output=True,text=True,timeout=timeout);(OUT/log).write_text(p.stdout+p.stderr);commands.append(dict(command=args,log=log,exitCode=p.returncode));
        if p.returncode:raise RuntimeError(log)
        return p.stdout
    cp=os.pathsep.join(map(str,[android,classes,junit,hamcrest]))
    run([javac,'-encoding','UTF-8','-sourcepath','', '-cp',cp,'-d',prod,*[MAIN/(n+'.java') for n in NAMES],TEST],'production-javac.log')
    # Clock/log/CPU priority only are host adapters; no business implementation replaced.
    shims={
      'SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
      'Process.java':'package android.os;public final class Process{public static final int THREAD_PRIORITY_BACKGROUND=10;public static int myPid(){return(int)java.lang.ProcessHandle.current().pid();}public static void setThreadPriority(int ignored){}}',
      'Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int i(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}}',
    }
    paths=[]
    for name,text in shims.items():p=work/name;p.write_text(text);paths.append(p)
    run([javac,'-encoding','UTF-8','-d',platform,*paths],'host-platform-javac.log')
    runtime_cp=os.pathsep.join(map(str,[platform,prod,host,classes,junit,hamcrest,android]))
    run([javac,'-encoding','UTF-8','-sourcepath','','-cp',runtime_cp,'-d',host,HOST],'host-javac.log')
    unit=run([java,'-Xmx512m','-cp',runtime_cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R31RuntimeMemoryControlTest'],'junit.log')
    test_count=int(re.search(r'OK \((\d+) tests\)',unit).group(1))
    duration=os.environ.get('R31_MEMORY_CONTROL_REAL_MS','120200')
    real=run([java,'-Xmx512m','-cp',runtime_cp,'com.ethk3.novaonepro.strategyv2.RuntimeMemoryControlR31Host',duration],'real-process.log',timeout=max(180,int(duration)//1000+45))
    result=json.loads(real);(OUT/'real-process.json').write_text(json.dumps(result,indent=2)+'\n');after={str(p.relative_to(ROOT)):sha(p) for p in sources}
    if before!=after:raise RuntimeError('source changed during verification')
    receipt=dict(status='TARGETED_HOST_PASS',scope='SOURCE_BOUND_ACTUAL_RUNTIME_COORDINATOR_WITH_REAL_DEFAULT_MEMORY_PROBE_AND_CONTROLLED_CLOCK_UNIT_TESTS_NOT_RELEASE_OR_DEVICE',capturedAt=datetime.now(timezone.utc).isoformat(),head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),sourceSha256=before,sourceStableDuringRun=True,snapshot=str(work),unitTests=test_count,realProcessAssertions=result['assertions'],actualMonotonicElapsedMs=result['elapsedRealMonotonicMs'],realTwoMinuteEpisode=result['elapsedRealMonotonicMs']>=120000,healthGetter='NO_DB_STORE_ABSENT; PENDING_REMAINS_UNKNOWN',physicalDevice='NOT_RUN',continuous24h='NOT_RUN',continuous72h='NOT_RUN',naturalQuality='NOT_EVALUATED',supportClasspath=str(classes),supportOriginal=str(support),supportSha256=support_sha,supportClasspathScope='READONLY_COMPILED_SUPPORT_JAR_SNAPSHOT; THIRTEEN_CURRENT_PRODUCERS_COMPILED_AHEAD; WHOLE_ANDROID_PROVENANCE_RECORDED_SEPARATELY_IN_BOOTSTRAP_SUPPORT_SNAPSHOT_WHEN_USED',commands=commands)
    (OUT/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    (OUT/'FROZEN_SOURCES.json').write_text(json.dumps(dict(scope='ONE_OWNED_PRODUCTION_SOURCE_TARGETED_HOST_PASS_PENDING_ROOT_JOINT',sources={str((MAIN/'RuntimeCoordinatorR26.java').relative_to(ROOT)):sha(MAIN/'RuntimeCoordinatorR26.java')},device='NOT_RUN',quality='NOT_EVALUATED'),indent=2)+'\n')
    print(json.dumps(dict(status=receipt['status'],unitTests=test_count,realMs=receipt['actualMonotonicElapsedMs'],assertions=receipt['realProcessAssertions'],snapshot=str(work))))
if __name__=='__main__':main()
