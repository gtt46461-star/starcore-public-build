#!/usr/bin/env python3
from pathlib import Path
import hashlib,json,os,re,subprocess,time
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 d=json.loads((OUT/'COMPILE_SETUP.json').read_text());cp=d['runtime'];inputs=[Path(p) for p in d['sourceSha256']]+list(OUT.glob('*.java'))+[Path(__file__).resolve()];before={str(p):sha(p) for p in inputs};commands=[]
 for p in d['sourceSha256']:assert sha(Path(p))==d['sourceSha256'][p],p
 def run(command,log,expected=0):
  cmd=list(map(str,command));started=time.time();r=subprocess.run(cmd,cwd=ROOT,text=True,capture_output=True,timeout=240);(OUT/log).write_text(r.stdout+r.stderr);commands.append(dict(command=cmd,exitCode=r.returncode,expectedExit=expected,wallSeconds=time.time()-started,log=log));assert r.returncode==expected,(log,r.returncode,(r.stdout+r.stderr)[-2500:]);return r.stdout
 hosts=[OUT/n for n in ['AtomNativeFaultsR31.java','AtomPublicationNativeHost.java','AtomPublicationConsumerHost.java','AtomPublicationProcessHost.java','MaterializedCursorNativeHost.java','AtomRuntimeProducerHost.java']]
 run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',d['test'],ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R27TestSupport.java',*hosts],'final-native-host-compile.log')
 java=['java','-Xmx512m','-Dr31.assets='+str(ROOT/'app/src/main/assets'),'-cp',cp];work=Path(d['work']);runid=str(time.time_ns())
 producer=json.loads(run([*java,'com.ethk3.novaonepro.strategyv2.AtomRuntimeProducerHost',work/('actual-producer-'+runid)/'databases/main.db'],'actual-runtime-producer.log'));(OUT/'actual-runtime-producer.json').write_text(json.dumps(producer,indent=2)+'\n')
 cursor=json.loads(run([*java,'com.ethk3.novaonepro.strategyv2.MaterializedCursorNativeHost',work/('cursor-fixed-'+runid)/'databases/main.db','fixed'],'cursor-fixed.log'));(OUT/'cursor-fixed.json').write_text(json.dumps(cursor,indent=2)+'\n');assert cursor['rejected'] and cursor['actualStoredMaterialized']==0 and cursor['actualStoredSamples']==0
 result=json.loads(run([*java,'com.ethk3.novaonepro.strategyv2.AtomPublicationConsumerHost',work/('final-consumer-'+runid)/'databases/main.db'],'final-native-consumer.log'));(OUT/'final-native-consumer.json').write_text(json.dumps(result,indent=2)+'\n')
 processes={}
 for branch,halt,code in [('before','halt-before-final',71),('after','halt-after-final',73)]:
  db=work/('process-'+runid+'-'+branch)/'databases/main.db';raw=run([*java,'com.ethk3.novaonepro.strategyv2.AtomPublicationProcessHost',db,halt],'process-'+branch+'-halt.log',code);event=json.loads(raw);recovery=json.loads(run([*java,'com.ethk3.novaonepro.strategyv2.AtomPublicationProcessHost',db,'recover-'+branch],'process-'+branch+'-recovery.log'));assert event['pid']!=recovery['pid'];processes[branch]=dict(haltExit=code,haltEvent=event,recovery=recovery)
 (OUT/'CROSS_JVM.json').write_text(json.dumps(processes,indent=2)+'\n')
 # Exact original numerical/state assertions, all in one JVM. No Gradle isolation, heap or GC changes.
 gradle=Path(os.environ.get('GRADLE_USER_HOME',Path.home()/'.gradle'));junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar'));hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'));jcp=cp+os.pathsep+str(junit)+os.pathsep+str(hamcrest)+os.pathsep+str(ROOT/'app/src/test/resources')
 names=['R29ModelArtifactTest','R30ResumeTrainingTest','R30TenPositionTrainingTest','RepairNeuralR30Test','R30NumberRankerTest','RepairModelReplayR30Test','R30ModelSurfaceTest'];testroot=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2';tests=[testroot/(n+'.java') for n in names];testHashes={str(p):sha(p) for p in tests}
 for p in tests:assert subprocess.check_output(['git','show','961a6b8a74b9624a2b195e9562fbdfc9283b29fc:'+str(p.relative_to(ROOT))],cwd=ROOT)==p.read_bytes()
 run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',jcp,'-d',d['test'],*tests],'original-model-junit-compile.log');unit=run(['java','-Xmx512m','-cp',jcp,'org.junit.runner.JUnitCore',*['com.ethk3.novaonepro.strategyv2.'+n for n in names]],'original-model-junit.log');count=int(re.search(r'OK \((\d+) tests\)',unit).group(1))
 assert before=={str(p):sha(p) for p in inputs};assert testHashes=={str(p):sha(p) for p in tests}
 receipt=dict(status='TARGETED_NATIVE_HOST_AND_ORIGINAL_JUNIT_PASS',scope='ACTUAL_OWNERS_FILE_CAS_SQLITE_CONSUMERS_AND_HOST_JVM_HALT_NOT_ANDROID_NOT_SOAK_NOT_QUALITY',sourceSha256=before,sourceStableDuringRun=True,originalTestSha256=testHashes,originalTestBytesRetained=True,originalJUnit=count,consumerAssertions=result['assertions'],actualRuntimeProducer=producer,cursorFault=cursor,fullOriginalSamplePlan=[800,400,160],crossJvm=processes,compileSetup=d,supportSha256=d['supportSha256'],commands=commands,device='NOT_RUN',soak24h='NOT_RUN',soak72h='NOT_RUN',naturalQuality='NOT_EVALUATED',explicitGc=False,heapArgument='-Xmx512m_ONLY')
 (OUT/'FINAL_NATIVE_RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(dict(status=receipt['status'],consumerAssertions=result['assertions'],originalJUnit=count,crossJvm=True)))
if __name__=='__main__':main()
