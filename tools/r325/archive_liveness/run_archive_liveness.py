#!/usr/bin/env python3
"""Production archive liveness on native SQLite. Android boundary adapters only.
The supplied classes must correspond to --repo. Optional baseline classes execute
the unmodified R32.4 cold-start starvation counterexample; no APK claim is made.
"""
import argparse,datetime,hashlib,importlib.util,json,os,re,shutil,subprocess,sys
from pathlib import Path
BUSINESS=['AtomNetSampleArchiveR324','AtomNetRuntimeR30','AtomNetPublicationR31','RuntimeR27','ResearchTaskRegistryR30R1']
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def main(a):
 repo=a.repo.resolve();classes=a.classes.resolve();out=a.out.resolve()
 if out.exists() and any(out.iterdir()):raise ValueError('OUT_MUST_BE_NEW_OR_EMPTY')
 out.mkdir(parents=True,exist_ok=True)
 receipt={'status':'RUNNING','scope':'ACTUAL_PRODUCTION_JAVA_AND_NATIVE_SQLITE_WITH_ANDROID_BOUNDARY_ADAPTERS','AndroidWorkerScheduling':'NOT_RUN','AndroidBuild':'NOT_RUN','naturalQuality':'NOT_EVALUATED','productionBusinessStubs':[],'commands':[],'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2,ensure_ascii=False)+'\n')
 def run(label,command):
  done=subprocess.run(list(map(str,command)),cwd=repo,capture_output=True,text=True,timeout=240)
  log=out/(label+'.log');log.write_text(done.stdout+done.stderr);receipt['commands'].append({'id':label,'argv':list(map(str,command)),'exitCode':done.returncode,'log':log.name,'sha256':sha(log)});save()
  if done.returncode:raise RuntimeError(label+': '+(done.stdout+done.stderr)[-7000:])
  return done.stdout
 try:
  sdk=a.android_jar.resolve();jdbc=a.jdbc_jar.resolve();slf=a.slf4j_jar.resolve()
  receipt['dependencies']={x.name:sha(x) for x in [sdk,jdbc,slf]}
  receipt['productionSourceSha256']={name:sha(repo/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/(name+'.java')) for name in BUSINESS}
  receipt['productionClassSha256']={name:sha(classes/'com/ethk3/novaonepro/strategyv2'/(name+'.class')) for name in BUSINESS}
  factory=repo/'tools/r31/run_atomnet_publication_host.py';spec=importlib.util.spec_from_file_location('r325_android_adapters',factory);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
  platform=out/'platform';tests=out/'tests';platform.mkdir();tests.mkdir();sources=[]
  for name,content in m.platform_sources().items():
   if not name.startswith('android/'):raise ValueError('BUSINESS_STUB_REFUSED')
   p=out/'platform_sources'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(content);sources.append(p)
  receipt['platformSourceSha256']={str(p.relative_to(out)):sha(p) for p in sources}
  compiler=[shutil.which('javac')] if shutil.which('javac') else ['java','-m','jdk.compiler/com.sun.tools.javac.Main']
  flags=['-encoding','UTF-8','-sourcepath','','-implicit:none']
  run('platform_compile',compiler+flags+['-cp',os.pathsep.join(map(str,[classes,sdk])),'-d',platform]+sources)
  if (platform/'com').exists():raise ValueError('BUSINESS_CLASS_IN_PLATFORM_ADAPTERS')
  fixtures=Path(__file__).resolve().parent
  fixture=fixtures/'ArchiveLivenessFixtureR325.java';test=fixtures/'AtomNetArchiveR325HostTest.java'
  receipt['testSourceSha256']={p.name:sha(p) for p in [fixture,test]}
  cp=os.pathsep.join(map(str,[platform,classes,sdk,jdbc,slf]))
  run('test_compile',compiler+flags+['-cp',cp,'-d',tests,fixture,test])
  runtime=os.pathsep.join(map(str,[tests,platform,classes,sdk,jdbc,slf]))
  text=run('fixed_production_run',['java','-Xmx512m','-cp',runtime,'com.ethk3.novaonepro.strategyv2.AtomNetArchiveR325HostTest',out])
  receipt['assertions']=int(re.search(r'ARCHIVE_LIVENESS_ASSERTIONS=(\d+)',text).group(1))
  if a.baseline_classes:
   baseline=a.baseline_classes.resolve();before=out/'baseline_tests';before.mkdir();probe=fixtures/'LegacyArchiveStarvationR325Probe.java'
   receipt['baselineClassSha256']={name:sha(baseline/'com/ethk3/novaonepro/strategyv2'/(name+'.class')) for name in BUSINESS}
   receipt['baselineProbeSha256']=sha(probe)
   oldcp=os.pathsep.join(map(str,[platform,baseline,sdk,jdbc,slf]))
   run('baseline_probe_compile',compiler+flags+['-cp',oldcp,'-d',before,fixture,probe])
   text=run('baseline_production_run',['java','-Xmx512m','-cp',os.pathsep.join(map(str,[before,platform,baseline,sdk,jdbc,slf])),'com.ethk3.novaonepro.strategyv2.LegacyArchiveStarvationR325Probe',out])
   receipt['baselineRepeatedBlockedColdRestores']=int(re.search(r'LEGACY_REPRODUCED_BLOCKED_COLD_RESTORE_ATTEMPTS=(\d+)',text).group(1))
  else:receipt['baselineCounterexample']='NOT_RUN_IN_THIS_INVOCATION'
  receipt.update(status='PASS_HOST_PRODUCTION_ARCHIVE_LIVENESS_ONLY',completedAt=datetime.datetime.now(datetime.timezone.utc).isoformat());save();print(json.dumps({'status':receipt['status'],'assertions':receipt['assertions'],'baseline':receipt.get('baselineRepeatedBlockedColdRestores',receipt.get('baselineCounterexample'))}));return 0
 except BaseException as e:receipt.update(status='FAILED',error=type(e).__name__+': '+str(e));save();raise
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__)
 for n in ['repo','classes','android-jar','jdbc-jar','slf4j-jar','out']:p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--baseline-classes',type=Path)
 sys.exit(main(p.parse_args()))
