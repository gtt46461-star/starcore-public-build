#!/usr/bin/env python3
"""Fresh actual CAS-retired startup checkpoint consumer. The source-root option
joins explicitly owned source worktrees before integration; default is this ROOT.
Every controlled draw/model byte is synthetic and no Android/natural claim is made."""
from pathlib import Path
import argparse,ast,hashlib,importlib.util,json,os,shutil,subprocess,tempfile,uuid

ROOT=Path(__file__).resolve().parents[2]
def digest(path):
 if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
 h=hashlib.sha256()
 for item in sorted(path.rglob('*.class')):h.update(str(item.relative_to(path)).encode()+b'\0'+hashlib.sha256(item.read_bytes()).digest())
 return h.hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--source-root',type=Path,default=ROOT);p.add_argument('--source-map',type=Path);p.add_argument('--classes',type=Path,default=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes');a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 source=a.source_root/'app/src/main/java/com/ethk3/novaonepro';names=['strategyv2/KeepMigrationR31.java','strategyv2/KeepMigrationStoreR31.java','strategyv2/KeepMismatchReferenceR31.java','strategyv2/KeepArchiveRetirementR31.java','strategyv2/GapReferenceR31.java','strategyv2/RetentionR26.java','strategyv2/DecisionExplanationStoreR30.java','strategyv2/ResearchRetentionR30.java','strategyv2/ResearchRetentionStoreR30.java','strategyv2/WarmPublicationR31.java','strategyv2/QualificationGapRepairR30R1.java','strategyv2/CheckpointGcStoreR31.java','strategyv2/CheckpointArtifactsR26.java','strategyv2/CheckpointScratchGcR31.java','strategyv2/CheckpointReferencesR26.java','strategyv2/ModelPublicationR31.java','StorageGcR31.java','EvidenceReachabilityR31.java']
 owners_tree=ast.parse((a.source_root/'tools/r31/run_lifecycle_stores_host.py').read_text());owners=next(ast.literal_eval(n.value) for n in ast.walk(owners_tree) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='OWNERS' for t in n.targets));names=list(dict.fromkeys(names+[n+'.java' for n in owners]+['DatabaseCoordinator.java','DbStore.java','DbHelper.java','strategyv2/RuntimeSchemaR26.java','strategyv2/IssueTimelineWriterR30.java']))
 declared=json.loads(a.source_map.read_text()) if a.source_map else {};prefix='app/src/main/java/com/ethk3/novaonepro/';assert all(k.startswith(prefix) and k.endswith('.java') for k in declared),'UNDECLARED_SOURCE_MAP_PATH';names=list(dict.fromkeys(names+[k[len(prefix):] for k in declared]))
 producers=[Path(declared.get(prefix+n,source/n)) for n in names if not n.endswith('/StartupCheckpointIdentityR31.java')]+[ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2/StartupCheckpointIdentityR31.java'];host=ROOT/'tools/r31/unified_host/StartupCheckpointR31Host.java';harness=[host,Path(__file__),a.source_root/'tools/r31/run_lifecycle_stores_host.py',a.source_root/'tools/r31/run_atomnet_publication_host.py',a.source_root/'tools/r31/run_task_runtime_host.py',*sorted((a.source_root/'tools/r29/qual_host/shim').rglob('*.java'))]
 assert all(f.is_file() for f in producers+harness),'ACTUAL_DECLARED_SOURCE_UNAVAILABLE'
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));env=dict(os.environ);env.pop('JAVA_TOOL_OPTIONS',None)
 hashes={str(f):digest(f) for f in producers+harness};support=digest(a.classes)
 multi=a.source_map is not None or a.source_root.resolve()!=ROOT.resolve()
 receipt={'format':'R31_STARTUP_CHECKPOINT_NATIVE_1','invocationId':str(uuid.uuid4()),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sharedSourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=a.source_root,text=True).strip(),'sourceMode':'MULTI_WORKTREE_INTERFACE_PRECHECK_NOT_FINAL_GATE' if multi else 'SINGLE_CURRENT_SOURCE_TREE','dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceSha256':hashes,'compiledSupportSha256':support,'scope':'ACTUAL_RUNTIME_FACTORY_REAL_DBSTORE_COORDINATOR_READERS_AND_COMPLETE_TYPED_CAS_COMPARISON_REAL_STARTUP_CONSUMER_AFTER_SQLITE_DUPLICATE_RETIREMENT; SYNTHETIC_ONLY_NOT_ANDROID_NOT_NATURAL','records':[],'status':'FAILED_OR_INCOMPLETE'}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 def run(label,cmd,expected=0):
  r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,capture_output=True,text=True,timeout=180);(out/(label+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'name':label,'exitCode':r.returncode,'expectedExit':expected});print(r.stdout+r.stderr,end='');save()
  if r.returncode!=expected:raise RuntimeError('ACTUAL_STARTUP_PROTOCOL_FAILURE '+label)
 save()
 with tempfile.TemporaryDirectory(prefix='r31-startup-native-') as temporary:
  work=Path(temporary);platform=work/'platform';product=work/'product';generated=work/'generated';platform.mkdir();product.mkdir();generated.mkdir();shims=[]
  portable=a.source_root/'tools/r31/run_atomnet_publication_host.py';spec=importlib.util.spec_from_file_location('startup_native_platform',portable);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);boundaries=module.platform_sources()
  for name,text in boundaries.items():target=generated/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)
  receipt['platformAdapterSha256']={str(f.relative_to(generated)):digest(f) for f in shims};save()
  cp=os.pathsep.join(map(str,[platform,product,a.classes,jdbc,sdk]));run('compile_platform',['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[a.classes,sdk,jdbc])),'-d',platform,*shims]);run('compile_actual_production',['javac','-J-Xmx768m','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[a.classes,sdk])),'-d',product,*producers]);run('compile_actual_consumer_host',['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',cp,'-d',product,host])
  base=['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.StartupCheckpointR31Host'];initial=work/'original/databases/startup.db';run('first_process_preserve_migrate_and_actual_retire',base+['init',initial])
  for mode in ['normal','race-blob','race-append','race-replace','race-storage','race-null','race-during','trigger-drift','corrupt-cas','kill-before','kill-after']:
   target=work/mode;shutil.copytree(work/'original',target);database=target/'databases/startup.db';run(mode,base+[mode,database],73 if mode=='kill-before' else 74 if mode=='kill-after' else 0)
   if mode.startswith('kill-'):run(mode+'-actual-fresh-process-resume',base+['normal',database])
 if hashes!={str(f):digest(f) for f in producers+harness}:raise RuntimeError('SOURCE_OR_HARNESS_CHANGED_DURING_EXECUTION')
 if support!=digest(a.classes):raise RuntimeError('COMPILED_SUPPORT_CHANGED_DURING_EXECUTION')
 receipt.update(status='PASS_NATIVE_INTERFACE_PRECHECK_NOT_FINAL_GATE' if multi else 'PASS_NATIVE_ACTUAL_STARTUP_PROTOCOL_ONLY',sourceAndSupportUnchanged=True,physicalDevice='NOT_RUN',naturalQuality='NOT_PROVEN');save()
if __name__=='__main__':main()
