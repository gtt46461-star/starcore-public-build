#!/usr/bin/env python3
"""Actual default-Probe collector recovery in two separate 512m HOST JVMs; never model/device evidence."""
from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,shutil,subprocess,tempfile,concurrent.futures,re,time
ROOT=Path(__file__).resolve().parents[2];SOURCE=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2';HOST=ROOT/'tools/r31/resource_recovery_host/ResourceRecoveryR31Host.java';TEST=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R31OptionalHeapRecoveryTest.java';OUT=ROOT/'verification/r31/p02/resource-recovery'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 OUT.mkdir(parents=True,exist_ok=True);work=Path(tempfile.mkdtemp(prefix='r31-real-resource-recovery-'));classes=work/'classes';classes.mkdir();gradle=Path(os.environ.get('GRADLE_USER_HOME',str(Path.home()/'.gradle')));local=Path('/workspace/starcore-environment/gradle-home');gradle=gradle if gradle.exists() else local
 junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar'));hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'));java,javac=shutil.which('java'),shutil.which('javac');paths=[SOURCE/'MemoryBudgetR31.java',SOURCE/'JsonR27.java',HOST,TEST,Path(__file__).resolve()];before={str(p.relative_to(ROOT)):sha(p) for p in paths};commands=[];cp=os.pathsep.join(map(str,[classes,junit,hamcrest]))
 # Each mode really waits 120.2 s before the controlled collector request; two modes run in parallel on a shared
 # CI runner. 420 s is the harness process limit, not an acceptance threshold; a real hang still fails.
 def run(command,log,timeout=420):
  command=list(map(str,command));started=time.monotonic();p=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=timeout);print('R31_HOST_STEP_ELAPSED '+log+' '+str(round(time.monotonic()-started,1))+'s',flush=True);(OUT/log).write_text(p.stdout+p.stderr);commands.append(dict(command=command,log=log,exitCode=p.returncode))
  if p.returncode:print('R31_HOST_STEP_FAILED '+log+'\n'+p.stdout[-4000:]+'\n'+p.stderr[-4000:],flush=True)
  p.check_returncode();return p.stdout
 run([javac,'-encoding','UTF-8','-cp',cp,'-d',classes,*[SOURCE/n for n in ['MemoryBudgetR31.java','JsonR27.java']],HOST,TEST],'javac.log');unit=run([java,'-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R31OptionalHeapRecoveryTest'],'unit.log');count=int(re.search(r'OK \((\d+) tests\)',unit).group(1))
 def mode(name):
  result=json.loads(run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ResourceRecoveryR31Host',name],name+'.log'));(OUT/(name+'.json')).write_text(json.dumps(result,indent=2)+'\n');return result
 with concurrent.futures.ThreadPoolExecutor(max_workers=2) as executor:results=list(executor.map(mode,['garbage','held']))
 after={str(p.relative_to(ROOT)):sha(p) for p in paths}
 if before!=after:raise RuntimeError('source changed during real verification')
 receipt=dict(status='TARGETED_HOST_PASS',capturedAt=datetime.now(timezone.utc).isoformat(),scope='ACTUAL_DEFAULT_RUNTIME_PROBE_REAL_HEAP_REAL_120S_EPISODE_AND_GC_REQUEST_NOT_ANDROID_OR_NATURAL_QUALITY',sourceSha256=before,sourceStableDuringRun=True,work=str(work),unitTests=count,realCases=[dict(mode=r['mode'],actualWaitMs=r['actualElapsedWaitMs'],assertions=r['assertions'],decision=r['decision']['outcome'],collectorRequests=r['explicitCollectorRequests'],actualGcCountDelta=r['decision']['gcCountDelta'],heapBefore=r['decision']['before']['heapUsedBytes'],heapAfter=r['decision']['after']['heapUsedBytes']) for r in results],probeSubstitution=False,clockSubstitution=False,ordinaryAdmissionExplicitGc=False,mathematicalTestsUsedCollector=False,device='NOT_RUN',continuous24h='NOT_RUN',continuous72h='NOT_RUN',naturalQuality='NOT_EVALUATED',commands=commands)
 (OUT/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
if __name__=='__main__':main()
