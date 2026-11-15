#!/usr/bin/env python3
"""Source-bound family admission/recovery checks against an actual Android runtime classes jar.
The supplied jar is compiled support, not a fake Android project. No Gradle, signing, fake process Probe,
heap tuning or collector request is used to make the original mathematical methods pass.
"""
from pathlib import Path
import argparse,concurrent.futures,hashlib,json,os,re,shutil,subprocess,tempfile,time
from datetime import datetime,timezone
ROOT=Path(__file__).resolve().parents[2]
SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2';TEST=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2'
OUT=Path(os.environ.get('R312_HOST_OUTPUT',ROOT/'verification/r31/p02/family-admission'))
HOST=ROOT/'tools/r31/family_admission_host'
BASE='5237b265a27fe5ad27fd842901c445d5f8c27749'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--support-jar',type=Path,default=ROOT/'app/build/intermediates/runtime_app_classes_jar/debug/bundleDebugClassesToRuntimeJar/classes.jar');args=parser.parse_args();support=args.support_jar.resolve();assert support.is_file(),support
 OUT.mkdir(parents=True,exist_ok=True);java=shutil.which('java');javac=shutil.which('javac');gradle=Path(os.environ.get('GRADLE_USER_HOME','/workspace/starcore-environment/gradle-home'));junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/junit-*.jar'));hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'));work=Path(tempfile.mkdtemp(prefix='r31-family-admission-'));classes=work/'current';classes.mkdir();baseline=work/'baseline';baseline.mkdir()
 names=['R31MemoryGovernanceTest','R31SharedFeatureIntegrationTest','R31AllocatedMemoryLifecycleTest','R31ScopedAstLifecycleTest','R31PrivateBackingReuseTest','R31FamilyScopeIsolationTest','R31FamilyAdmissionTest','R31FamilyBudgetResumeTest','R27TestSupport','R27BirthGateTest','R30PolicyContractTest','R27LeakageTest','R30ModelSurfaceTest','R30EvolutionConfirmIntegrationTest','R27EvolutionTest','R30NullPipelineTest']
 hosts=[*sorted(HOST.glob('*.java')),ROOT/'tools/r31/memory_host/FeatureGoldenR31Host.java',ROOT/'verification/r31/p02/family-stream-isolation-probe/FamilyStreamIsolationR31Host.java']
 dependencies=[*sorted(SRC.glob('FamilyF*KernelR26.java')),SRC/'FamilyF19TensorR26.java',*[SRC/(n+'.java') for n in ['MemoryBudgetR31','SharedFeatureStoreR31','PolicyAstR27','CooperativeContinuationR26','ExecutionBudgetR26','FamilyRegistryR26','OnlineModelsSU2','NeuralFamilyR26','AutoDiffR26','SymbolicFamilyR26','SetFamilyR26','PortfolioFamilyR26','ResidualFamilyR26','DistillationFamilyR26','PermutationModelR26']]]
 paths=[SRC/'FeatureCacheR27.java',*[TEST/(n+'.java') for n in names],*hosts,*dependencies,Path(__file__).resolve()];before={str(p.relative_to(ROOT)):sha(p) for p in paths};commands=[];support_sha=sha(support);cp=os.pathsep.join(map(str,[classes,support,junit,hamcrest]))
 def run(cmd,log,allow_failure=False,timeout=240):
  command=list(map(str,cmd));started=datetime.now(timezone.utc).isoformat();state={'status':'IN_PROGRESS','command':command,'timeLimitSeconds':timeout,'startedAt':started};status_path=OUT/(log+'.status.json');status_path.write_text(json.dumps(state,indent=2)+'\n')
  with (OUT/log).open('w',buffering=1) as stream, (OUT/(log+'.stderr.log')).open('w',buffering=1) as errors:
   child=subprocess.Popen(command,cwd=ROOT,stdout=stream,stderr=errors,text=True);state['pid']=child.pid;status_path.write_text(json.dumps(state,indent=2)+'\n')
   try:code=child.wait(timeout=timeout)
   except subprocess.TimeoutExpired:
    child.terminate()
    try:child.wait(timeout=5)
    except subprocess.TimeoutExpired:child.kill();child.wait()
    code=124;stream.write('\nHOST_VERIFIER_TIME_LIMIT_SECONDS='+str(timeout)+'\n')
  contents=(OUT/log).read_text();stderr=(OUT/(log+'.stderr.log')).read_text();process=subprocess.CompletedProcess(command,code,contents,stderr);state.update(status='TIMED_OUT' if code==124 else 'EXITED',exitCode=code,completedAt=datetime.now(timezone.utc).isoformat());status_path.write_text(json.dumps(state,indent=2)+'\n');commands.append(dict(state,log=log,stderrLog=log+'.stderr.log'))
  if process.returncode and not allow_failure:print('R31_HOST_STEP_FAILED '+log+' exit='+str(code)+'\n'+(contents+stderr)[-6000:],flush=True);raise RuntimeError(log)
  return process
 receipt={'status':'FAILED_OR_INCOMPLETE','sourceSha256':before,'supportJar':str(support),'supportJarSha256':support_sha,'scope':'CURRENT_FEATURE_SOURCE_ON_ACTUAL_ANDROID_SUPPORT_JAR_NOT_DEVICE_OR_APK','work':str(work),'device':'NOT_RUN','continuous24h':'NOT_RUN','continuous72h':'NOT_RUN','naturalQuality':'NOT_EVALUATED'}
 try:
  run([javac,'-cp',cp,'-d',classes,SRC/'FeatureCacheR27.java',*[TEST/(n+'.java') for n in names],*hosts],'compile.log')
  selected=['R31MemoryGovernanceTest','R31SharedFeatureIntegrationTest','R31AllocatedMemoryLifecycleTest','R31ScopedAstLifecycleTest','R31PrivateBackingReuseTest','R31FamilyScopeIsolationTest','R31FamilyAdmissionTest','R31FamilyBudgetResumeTest'];target=run([java,'-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore',*['com.ethk3.novaonepro.strategyv2.'+n for n in selected]],'targeted-junit.log');count=int(re.search(r'OK \((\d+) tests\)',target.stdout).group(1));receipt['targetedJUnit']=count
  existing=run([java,'-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R27LeakageTest','com.ethk3.novaonepro.strategyv2.R30ModelSurfaceTest'],'original-pit-model-junit.log');receipt['originalPitModelJUnit']=int(re.search(r'OK \((\d+) tests\)',existing.stdout).group(1))
  # Baseline fingerprints use the original sampler too: the current OperatorGoldenR27 consumes PolicyAstR27.evalScopedR31,
  # which the original PolicyAstR27 does not have (NoSuchMethodError at baseline-golden). Current==original is still asserted.
  golden=ROOT/'app/src/main/assets/r27/golden_512.bin';old=[]
  for name in ['FeatureCacheR27','PolicyAstR27','ReplayDatasetR27','ReplayEvaluatorR27','OperatorGoldenR27']:
   path=work/(name+'.java');path.write_bytes(subprocess.check_output(['git','show',BASE+':'+str((SRC/(name+'.java')).relative_to(ROOT))],cwd=ROOT));old.append(path)
  run([javac,'-cp',cp,'-d',baseline,*old,ROOT/'tools/r31/memory_host/FeatureGoldenR31Host.java'],'baseline-compile.log');a=json.loads(run([java,'-Xmx512m','-cp',str(baseline)+os.pathsep+str(support),'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'baseline-golden.json').stdout);b=json.loads(run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FeatureGoldenR31Host',golden],'current-golden.json').stdout);assert a==b;receipt['goldenFingerprints']=len(a)
  family=json.loads(run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FamilyStreamIsolationR31Host'],'family-isolation.json').stdout);values=0
  for case in family['cases']:
   assert not any(case['aliasesImmediatelyAfterExtend'].values()) and not case['olderModelMutatedByChild'] and case['oldReadRebuildDelta']==0
   assert case['olderBeforeExtend']['modelStateSha256']==case['olderAfterItsRead']['modelStateSha256']
   for row in case['allTenPositionsVsIndependent']:
    for role in ['child','older']:assert row[role]['bitMismatches']==0;values+=row[role]['actualValues']
  receipt['familyIsolation']={'families':len(family['cases']),'positions':50,'comparedValues':values,'bitMismatches':0,'parentStateStable':True}
  run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FamilyShapeR31Host'],'current-model-shapes.json')
  run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FamilyCodecR31Host'],'codec-wire-and-cpu.json')
  def cancellation(case):
   f,boundary=case;p=run([java,'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.FamilyRecoveryR31Host','require-recovered',str(f),boundary],f'cancel-F{f:02d}-{boundary}.json');d=json.loads(p.stdout);assert d['bitMismatchesAfterRetry']==0 and d['oldWorkerActuallyTerminated'] and d['modelCodecEqualAfterRetry'];return d
  cases=[(f,b) for f in [3,8,11,12,20,21] for b in ['before-predict','after-predict','before-update','after-update']]
  with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:results=list(pool.map(cancellation,cases))
  (OUT/'cancellation-cases.json').write_text(json.dumps(results,indent=2)+'\n');receipt['realCooperativeCancellationCases']=len(results)
  math=run([java,'-Xmx512m','-cp',cp,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.strategyv2.R27EvolutionTest','com.ethk3.novaonepro.strategyv2.R30NullPipelineTest'],'original-six-default512.log',allow_failure=True,timeout=900);receipt['originalSix']={'status':'PASS' if math.returncode==0 else 'TIMED_OUT' if math.returncode==124 else 'FAIL','exitCode':math.returncode,'maximumHeapBytes':536870912,'observer':False,'substitutedProbe':False,'explicitGc':False,'originalCountsAndNumericThresholdsPreserved':True}
  receipt['status']='TARGETED_HOST_PASS' if math.returncode==0 else 'TARGETED_OWNERSHIP_PASS_ORIGINAL_MATH_FAIL'
 finally:
  receipt['sourceStableDuringRun']=before=={str(p.relative_to(ROOT)):sha(p) for p in paths};receipt['supportJarStableDuringRun']=support_sha==sha(support);receipt['capturedAt']=datetime.now(timezone.utc).isoformat();receipt['commands']=commands;receipt['remaining']=['Android full source/test compile, identity, full-suite integration, signed APK and device proof belong to root integration','initial built-in constructors and private matrices are admitted; unregistered per-step model-kernel/native/application allocation owners remain debt','F23 arbitrary external adapters require an explicitly registered construction/read bound; no inferred universal bound','row codec plus rollback protects model-state and pending outputs; it is in-memory recovery, not a durable training checkpoint','explicit cold familyInto overload reaches REALTIME model/matrix/checkpoint; root must integrate PolicyAst existing critical parameter at its owned call site','cross-PID admission NOT_COORDINATED; no device or natural future quality proof'];(OUT/'RECEIPT.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
 if not receipt['sourceStableDuringRun'] or not receipt['supportJarStableDuringRun']:raise RuntimeError('mixed-source verification refused')
 print(json.dumps({k:receipt[k] for k in ['status','targetedJUnit','originalPitModelJUnit','goldenFingerprints','realCooperativeCancellationCases','originalSix']}))
 if receipt['status']!='TARGETED_HOST_PASS':raise SystemExit(1)
if __name__=='__main__':main()
