#!/usr/bin/env python3
"""Fresh actual RuntimeR27.DatasetTask publication/recovery, original guards and
native SQLite. HOST POSIX measurement uses real stat and unchanged disk limits;
synthetic inputs only, with no Android/device/natural-quality claim."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,tempfile,uuid
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies

ROOT=Path(__file__).resolve().parents[2]
def digest(paths,base):
 h=hashlib.sha256()
 for path in sorted(paths):h.update(str(path.relative_to(base)).encode()+b'\0'+hashlib.sha256(path.read_bytes()).digest())
 return h.hexdigest()
def support_digest(path):
 return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else digest(path.rglob('*.class'),path)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path);args=parser.parse_args()
 out=args.out or ROOT/'app/build/r31-replay-knowledge-publication';out.mkdir(parents=True,exist_ok=True)
 classes,sdk,_,jdbc=dependencies(ROOT)
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
 sources=[source/name for name in ['RuntimeR27.java','ReplayKnowledgeStoreR31.java','ReplayDatasetR27.java','StoreR27.java','HistoryImportR27.java','ResearchInputPinR30R1.java','ResearchSourceJournalR31.java','FrozenConfirmationStoreR30.java','AtomNetStoreR30.java']]+[ROOT/'app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java']
 hosts=[ROOT/'tools/r31/unified_host/ReplayKnowledgePublicationR31Host.java',ROOT/'tools/r31/host/R31OwnedRuntimeFixture.java',ROOT/'tools/r31/history_import_host/HistoryPosixStatR31Host.java']
 native=ROOT/'tools/r31/history_import_host/history_posix_stat_r31.c'
 tracked=sources+hosts+[source/'LiveShadowsR27.java',native,Path(__file__),ROOT/'tools/r31/host_runtime_fixture.py',ROOT/'tools/r31/host_tool_support.py']+list((ROOT/'tools/r29/qual_host/shim').rglob('*.java'))
 before=digest(tracked,ROOT);support=support_digest(classes)
 receipt={'invocationId':str(uuid.uuid4()),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceTree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceHashes':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'sourceAndHarnessSha256':before,'compiledSupportSha256':support,'scope':'ACTUAL_RUNTIME_DATASET_TASK_THREE_BRANCHES_ORIGINAL_CLAIM_FENCE_SOLE_WRITER_NATIVE_SQLITE; SYNTHETIC_ROWS; HOST_REAL_POSIX_DEFAULT_BUDGET; NOT_ANDROID_OR_NATURAL','status':'FAILED_OR_INCOMPLETE','executed':[]}
 def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 def execute(name,command):
  run=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=240);(out/(str(len(receipt['executed']))+'-'+name+'.log')).write_text(run.stdout+run.stderr);receipt['executed'].append({'name':name,'exitCode':run.returncode});print(run.stdout+run.stderr,end='');save();run.check_returncode()
 save()
 with tempfile.TemporaryDirectory(prefix='r31-replay-publication-') as temporary:
  work=Path(temporary);platform=compile_owned_platform(ROOT,work/'owned',classes,sdk);adapter=work/'owned/sources';product=work/'product';product.mkdir()
  application=adapter/'android/app/Application.java';application.write_text('package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File path){this.path=path;}@Override public java.io.File getDatabasePath(String name){return path;}@Override public java.io.File getFilesDir(){java.io.File files=new java.io.File(path.getParentFile().getParentFile(),"files");if(!files.isDirectory()&&!files.mkdirs()&&!files.isDirectory())throw new IllegalStateException("HOST_FILES_DIRECTORY");return files;}}')
  store=adapter/'com/ethk3/novaonepro/DbStore.java';store.write_text(store.read_text().replace('public android.content.Context contextForR26()', 'public java.io.File researchInputRootCP11(){return new java.io.File(context.getFilesDir(),"r31-inputs");}public android.content.Context contextForR26()'))
  process=adapter/'android/os/Process.java';process.write_text(process.read_text().replace('public static final int','public static int myPid(){return Math.toIntExact(java.lang.ProcessHandle.current().pid());}public static int myTid(){return Math.toIntExact(Thread.currentThread().getId());}public static final int'))
  receipt['platformAdapterHashes']={str(p.relative_to(adapter)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (application,store,process)};save()
  cp=os.pathsep.join(map(str,[platform,product,classes,jdbc,sdk]))
  execute('compile_path_pid_platform_adapters',['javac','-encoding','UTF-8','-sourcepath','','-cp',cp,'-d',platform,application,store,process])
  execute('compile_actual_producers',['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[classes,sdk])),'-d',product,*sources])
  execute('compile_actual_owned_runtime_host',['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',cp,'-d',product,*hosts])
  library=work/'libreplay_posix_stat_r31.so';java_home=Path(os.environ['JAVA_HOME'])
  execute('compile_real_posix_measurement_boundary',['cc','-std=c11','-O2','-Wall','-Wextra','-Werror','-fPIC','-shared','-I'+str(java_home/'include'),'-I'+str(java_home/'include/linux'),native,'-o',library]);receipt['hostNativeMeasurementLibrarySha256']=hashlib.sha256(library.read_bytes()).hexdigest();save()
  execute('actual_dataset_task_new_restore_legacy_readonly_process_reopen',['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ReplayKnowledgePublicationR31Host',work/'execution',library])
 assert digest(tracked,ROOT)==before,'SOURCE_OR_HARNESS_CHANGED_DURING_EXECUTION'
 assert support_digest(classes)==support,'COMPILED_SUPPORT_CHANGED_DURING_EXECUTION'
 receipt.update(status='PASS_NATIVE_RUNTIME_PUBLICATION_ONLY',sourcesAndCompiledSupportUnchanged=True);save()
if __name__=='__main__':main()
