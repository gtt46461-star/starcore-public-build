#!/usr/bin/env python3
"""Production R929/R932 business paths on native SQLite adapters; never Android/device proof."""
import hashlib,json,os,shutil,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro'
OUT=ROOT/'verification/r31/p02/research-business-schema'
SOURCES=[MAIN/'ResearchPlatformStoreR929.java',MAIN/'UnifiedResearchChoiceR932.java',MAIN/'TransactionSchemaCacheR941.java',MAIN/'SchemaDomainCoordinatorR926.java',MAIN/'ReleaseManifestR926.java',MAIN/'strategyv2/RuntimeSchemaR26.java',MAIN/'FetchLedgerR925.java',MAIN/'ApiClient.java']
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 OUT.mkdir(parents=True,exist_ok=True);before={str(p.relative_to(ROOT)):sha(p) for p in SOURCES}
 classes=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));commands=[]
 def run(command,log):
  p=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=120);(OUT/log).write_text(p.stdout+p.stderr);commands.append({'command':list(map(str,command)),'exitCode':p.returncode,'log':log})
  if p.returncode:raise RuntimeError(log)
  return p.stdout
 with tempfile.TemporaryDirectory(prefix='r31-r929-schema-') as directory:
  temp=Path(directory);platform=temp/'platform';sources=temp/'shim';current=temp/'current';baseline=temp/'baseline';test=temp/'test'
  for p in [platform,sources,current,baseline,test]:p.mkdir()
  files=[]
  for source in sorted((ROOT/'tools/r29/qual_host/shim').rglob('*.java')):
   path=sources/source.relative_to(ROOT/'tools/r29/qual_host/shim');path.parent.mkdir(parents=True,exist_ok=True);text=source.read_text()
   if source.name=='SQLiteDatabase.java':
    text=text.replace('public class SQLiteDatabase {','public class SQLiteDatabase implements java.lang.AutoCloseable {\n    public int ddlCount;public final java.util.List<String> ddlStatements=new java.util.ArrayList<>();private boolean successful;')
    text=text.replace('    public void execSQL(String sql, Object[] args) {','    public void execSQL(String sql, Object[] args) {\n        if(sql.trim().toUpperCase(java.util.Locale.ROOT).matches("(?s)^(CREATE|ALTER|DROP)\\\\b.*")){ddlCount++;if(ddlStatements.size()<1024)ddlStatements.add(sql);}')
    point=text.index('    static final class Rows')
    text=text[:point]+'''    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();jdbc.setAutoCommit(true);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
'''+text[point:]
   path.write_text(text);files.append(path)
  adapters={
   'com/ethk3/novaonepro/DatabaseCoordinator.java':'''package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DatabaseCoordinator{
    public interface WriteCallable<T>{T call(SQLiteDatabase db)throws Exception;}public interface WriteRunnable{void run(SQLiteDatabase db)throws Exception;}
    final SQLiteDatabase db;public int migrations;public DatabaseCoordinator(SQLiteDatabase db){this.db=db;}
    public <T>T runAtomicWrite(String operation,WriteCallable<T> body,T fallback){if(operation.contains("SCHEMA"))migrations++;db.beginTransaction();TransactionSchemaCacheR941.Scope scope=TransactionSchemaCacheR941.enter(db);boolean committed=false;try{T result=body.call(db);db.setTransactionSuccessful();committed=true;return result;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}finally{db.endTransaction();if(committed)TransactionSchemaCacheR941.afterCommit(scope);TransactionSchemaCacheR941.leave(scope);}}
   }''',
   'com/ethk3/novaonepro/DbStore.java':'''package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbStore{final SQLiteDatabase db;public final DatabaseCoordinator coordinator;final java.util.Map<String,String> state=new java.util.HashMap<>();public DbStore(SQLiteDatabase db){this.db=db;coordinator=new DatabaseCoordinator(db);}public SQLiteDatabase readable(){return db;}public DatabaseCoordinator coordinatorR36(){return coordinator;}public String getState(String k,String fallback){String v=state.get(k);return v==null?fallback:v;}public void setState(String k,String v){state.put(k,v);}}''',
  }
  for name,text in adapters.items():path=sources/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
  # Compile database boundary adapters against actual cache, then production classes against full project deps.
  json_sources=sorted((ROOT/'verification/r932/android-json').rglob('*.java'))
  run(['javac','-cp',str(sdk)+os.pathsep+str(classes),'-d',platform,*files,*[sources/name for name in adapters],MAIN/'TransactionSchemaCacheR941.java',*json_sources],'adapter-javac.log')
  compile_cp=os.pathsep.join(map(str,[sdk,classes]))
  run(['javac','-cp',compile_cp,'-d',current,*SOURCES],'production-javac.log')
  # Original source is a real repository snapshot, not a rewritten reference implementation.
  baseline_files=[]
  for name in ['ResearchPlatformStoreR929.java','UnifiedResearchChoiceR932.java']:
   path=temp/'baseline-source'/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(subprocess.check_output(['git','show','31fea28c5723ac70e47ae4cc7542344b16fb3f91:app/src/main/java/com/ethk3/novaonepro/'+name],cwd=ROOT));baseline_files.append(path)
  run(['javac','-cp',compile_cp,'-d',baseline,*baseline_files],'baseline-javac.log')
  run(['javac','-cp',os.pathsep.join(map(str,[platform,current,classes,jdbc,sdk])),'-d',test,ROOT/'tools/r31/schema_host/R929BusinessSchemaHost.java'],'harness-javac.log')
  baseline_cp=os.pathsep.join(map(str,[platform,baseline,test,current,classes,jdbc,sdk]));current_cp=os.pathsep.join(map(str,[platform,current,test,classes,jdbc,sdk]))
  old=run(['java','-cp',baseline_cp,'com.ethk3.novaonepro.R929BusinessSchemaHost','baseline'],'baseline-native.log')
  new=run(['java','-cp',current_cp,'com.ethk3.novaonepro.R929BusinessSchemaHost'],'current-native.log')
  if 'R929_SCHEMA_HOST_PASS' not in new:raise RuntimeError('missing native success receipt')
 after={str(p.relative_to(ROOT)):sha(p) for p in SOURCES}
 if before!=after:raise RuntimeError('source changed during verification')
 receipt={'scope':'P02_B02_D05_D06_RUNTIME_DDL_REPAIR','state':'CODE_COMPLETE','actualProductionChanges':['ResearchPlatformStoreR929.ensureSchema/schemaReadyReadOnlyR31/activateRuntimeBundle','UnifiedResearchChoiceR932.freeze'],'rootStartupIntegration':['SchemaDomainCoordinatorR926.researchContract includes research_consumer_choice_r932','ReleaseManifestR926 research domain revision17; SQLite userVersion36 and Critical revision6 retained'],'startupJournalTestScope':'Actual SchemaDomain coordinator methods with a reduced two-object fixture; full Android R932 startup graph NOT_RUN','toolSha256':sha(Path(__file__)),'hostHarnessSha256':sha(ROOT/'tools/r31/schema_host/R929BusinessSchemaHost.java'),'sourceSha256':after,'sourceHashStableDuringRun':True,'baselineCommit':'31fea28c5723ac70e47ae4cc7542344b16fb3f91','baselineFaults':old.strip().splitlines(),'currentResults':new.strip().splitlines(),'businessCreateAlterDrop':0,'databaseBackend':'native-sqlite-jdbc','realAndroidOwnerAdvance':'NOT_RUN','device':'NOT_RUN','constraintsPreserved':['verified bundle registry','unique active bundle index','certificate primary/unique keys','immutable predraw choice and scope','one existing migration owner, no fake schema_version writes'],'startupOwner':'existing ResearchSchemaCoordinatorR926.R932_UNIFIED; root research domain revision17 includes choice and re-arms drift journal','testAdapters':'Only SQLite/DbStore/coordinator platform boundaries adapted; production two stores and actual transaction schema/domain cache execute native SQL','commands':commands}
 (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(json.dumps({'status':'HOST_PASS','businessDDL':0,'sourceHashes':'MATCH','device':'NOT_RUN'}))
if __name__=='__main__':main()
