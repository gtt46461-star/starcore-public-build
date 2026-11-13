#!/usr/bin/env python3
"""Compile current GC owners, then run persistent native SQLite + actual CAS files.
All output is bound to source and compiled-support hashes; no Android/service claim.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,subprocess,tempfile,uuid
from host_runtime_fixture import compile_owned_platform
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--classes',type=Path,default=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes');p.add_argument('--keep-root',type=Path,default=ROOT);args=p.parse_args()
out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
sources=[ROOT/'app/src/main/java/com/ethk3/novaonepro'/name for name in ['StorageGcR31.java','EvidenceReachabilityR31.java']]
sources += [ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/name for name in ['CheckpointGcStoreR31.java','CheckpointScratchGcR31.java','CheckpointArtifactsR26.java','CheckpointManifestR26.java','CheckpointReferencesR26.java','ModelPublicationR31.java']]
sources += [args.keep_root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/name for name in ['KeepMigrationR31.java','KeepMigrationStoreR31.java','ResearchRetentionStoreR30.java','KeepMismatchReferenceR31.java']]
host=ROOT/'tools/r31/unified_host/CheckpointGcStoreR31Host.java'
scratch_host=ROOT/'tools/r31/unified_host/CheckpointScratchGcR31Host.java'
def source_key(path):
 try:return str(path.relative_to(ROOT))
 except ValueError:return 'integration-keep/'+str(path.relative_to(args.keep_root))
def digest(path):
 if path.is_file():return hashlib.sha256(path.read_bytes()).hexdigest()
 value=hashlib.sha256()
 for child in sorted(path.rglob('*.class')):value.update(str(child.relative_to(path)).encode()+b'\0'+hashlib.sha256(child.read_bytes()).digest())
 return value.hexdigest()
receipt={'format':'R31_ACTUAL_GC_NATIVE_EXECUTION_1','invocationId':str(uuid.uuid4()),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceTree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceSha256':{source_key(s):digest(s) for s in sources+[host,scratch_host]},'compiledSupportSha256':digest(args.classes),'androidJarSha256':digest(sdk),'sqliteJdbcSha256':digest(jdbc),'startedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'HOST_ACTUAL_GC_STORE_AND_FILES_NATIVE_SQLITE_REAL_PROCESS_KILL; NOT_ANDROID_SERVICE_OR_DEVICE_OR_NATURAL_QUALITY','status':'STARTED_NOT_PASS','records':[]}
def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
def run(command,label):
 command=list(map(str,command));r=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=180);(out/(label+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'stage':label,'exitCode':r.returncode});save();print(label,'exit='+str(r.returncode),flush=True)
 if r.returncode:receipt['status']='FAILED_CURRENT_EXECUTION';save();print((r.stdout+r.stderr)[-12000:]);r.check_returncode()
save()
try:
 with tempfile.TemporaryDirectory(prefix='r31-gc-native-') as temp:
  work=Path(temp);platform=compile_owned_platform(ROOT,work,args.classes,sdk);product=work/'product';tests=work/'tests';product.mkdir();tests.mkdir()
  run(['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[args.classes,sdk])),'-d',product,*sources],'compile-actual-gc-owners')
  cp=os.pathsep.join(map(str,[platform,product,tests,args.classes,jdbc,sdk]));run(['javac','-encoding','UTF-8','-cp',cp,'-d',tests,host,scratch_host],'compile-native-execution-fixture')
  run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.CheckpointGcStoreR31Host',out/'fixtures'],'real-native-gc-cas-kill-restart')
  run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.CheckpointScratchGcR31Host',out/'scratch-fixtures'],'real-native-scratch-kill-restart')
  receipt['compiledGcOwnersSha256']=digest(product)
 if {source_key(s):digest(s) for s in sources+[host,scratch_host]}!=receipt['sourceSha256'] or digest(args.classes)!=receipt['compiledSupportSha256']:raise RuntimeError('CURRENT_SOURCE_OR_COMPILED_SUPPORT_CHANGED_DURING_EXECUTION')
 receipt['sourceAndSupportUnchanged']=True;receipt['status']='PASS_TARGETED_NATIVE_NOT_ANDROID_OR_WHOLE_CARD_CLOSURE'
finally:receipt['finishedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();save()
