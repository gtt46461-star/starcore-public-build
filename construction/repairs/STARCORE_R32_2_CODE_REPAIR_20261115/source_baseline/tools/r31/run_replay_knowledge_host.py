#!/usr/bin/env python3
"""Fresh actual replay loader, pin/source owner and original-row codecs on
native SQLite. Controlled synthetic rows and actual process reentry; never
Android or natural-quality evidence. Old pass files are not consulted."""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,tempfile,uuid
ROOT=Path(__file__).resolve().parents[2]
def digest(paths,base):
 h=hashlib.sha256()
 for path in sorted(paths):h.update(str(path.relative_to(base)).encode()+b'\0'+hashlib.sha256(path.read_bytes()).digest())
 return h.hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--out',type=Path);args=parser.parse_args()
 out=args.out or Path(os.environ.get('R31_REPLAY_KNOWLEDGE_OUT',ROOT/'app/build/r31-replay-knowledge'));out.mkdir(parents=True,exist_ok=True)
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
 sources=[source/name for name in ['ReplayDatasetR27.java','StoreR27.java','HistoryImportR27.java','ResearchInputPinR30R1.java','ResearchSourceJournalR31.java','FrozenConfirmationStoreR30.java','AtomNetStoreR30.java']]
 sources.append(ROOT/'app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java')
 host=ROOT/'tools/r31/unified_host/ReplayKnowledgeR31Host.java'
 shims=list((ROOT/'tools/r29/qual_host/shim').rglob('*.java'))
 classes=Path(os.environ.get('R31_REPLAY_KNOWLEDGE_SUPPORT_CLASSES',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
 assert classes.is_dir() and sdk.is_file() and jdbc.is_file(),'COMPILED_SUPPORT_OR_NATIVE_DEPENDENCY_UNAVAILABLE'
 before=digest(sources+[host,*shims],ROOT);support=digest(classes.rglob('*.class'),classes)
 receipt={'invocationId':str(uuid.uuid4()),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceTree':subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip(),'dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sourceHashes':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},'sourceAndHarnessSha256':before,'compiledSupportSha256':support,'scope':'ACTUAL_LOADER_PIN_SOURCE_ADMISSION_AND_THREE_ORIGINAL_CODECS_NATIVE_SQLITE; SYNTHETIC_CONTROL_ROWS; NOT_ANDROID_OR_NATURAL','status':'FAILED_OR_INCOMPLETE','executed':[]}
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
 with tempfile.TemporaryDirectory(prefix='r31-replay-knowledge-') as temp:
  work=Path(temp);platform=work/'platform';product=work/'product';platform.mkdir();product.mkdir()
  copied=[]
  for path in shims:
   target=work/'shim-source'/path.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True);code=path.read_text()
   if path.name=='SQLiteDatabase.java':
    methods="""    private boolean transactionSuccessfulR31;
    public String getPath(){try{return jdbc.getMetaData().getURL().substring(\"jdbc:sqlite:\".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void beginTransaction(){try{jdbc.setAutoCommit(false);transactionSuccessfulR31=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){transactionSuccessfulR31=true;}
    public void endTransaction(){try{if(transactionSuccessfulR31)jdbc.commit();else jdbc.rollback();jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
"""
    anchor='    public final Connection jdbc;';assert anchor in code;code=code.replace(anchor,methods+anchor)
    receipt['hostAdapterSha256']=hashlib.sha256(code.encode()).hexdigest();receipt['hostAdapterScope']='JDBC transaction/path adapter only; actual owner migration, guards and codecs unchanged'
   target.write_text(code);copied.append(target)
  cp=os.pathsep.join(map(str,[product,platform,classes,jdbc,sdk]))
  commands=[('compile_platform',['javac','-encoding','UTF-8','-cp',str(sdk),'-d',str(platform),*map(str,copied)]),('compile_actual_sources_and_host',['javac','-encoding','UTF-8','-cp',cp,'-d',str(product),*map(str,sources),str(host)]),('native_sqlite_original_codecs_and_process_reopen',['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ReplayKnowledgeR31Host',str(work/'execution')])]
  for index,(name,command) in enumerate(commands):
   run=subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=240);(out/(str(index)+'-'+name+'.log')).write_text(run.stdout+run.stderr)
   receipt['executed'].append({'name':name,'exitCode':run.returncode});print(run.stdout+run.stderr,end='');(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n');run.check_returncode()
 assert digest(sources+[host,*shims],ROOT)==before,'SOURCE_OR_HARNESS_CHANGED_DURING_EXECUTION'
 assert digest(classes.rglob('*.class'),classes)==support,'COMPILED_SUPPORT_CHANGED_DURING_EXECUTION'
 receipt.update(status='PASS_NATIVE_PROTOCOL_ONLY',sourcesAndCompiledSupportUnchanged=True)
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':main()
