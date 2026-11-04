#!/usr/bin/env python3
from pathlib import Path
from datetime import datetime,timezone
import os,sys,json,hashlib,subprocess,tempfile,ast,shutil,re
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent;MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2';BASE='2cdf5939c25aad6dd77632c124a59f17e68b8521';BA35='ba35b0d6328c98737cbfbd483ddd3dcbf182aaac'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 mode=sys.argv[1] if len(sys.argv)>1 else 'fixed';tmp=Path(tempfile.mkdtemp(prefix='r31-live-fork-'+mode+'-'));platform=tmp/'platform';production=tmp/'production';test=tmp/'test';sources=tmp/'sources'
 for p in [platform,production,test,sources]:p.mkdir()
 support=Path(os.environ.get('R31_LIVE_FORK_SUPPORT','/tmp/r31-ba35-host-integration-mifthdj3/frozen-debug-classes.jar'));frozen=tmp/'support.jar';shutil.copyfile(support,frozen);sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));gradle=Path(os.environ['GRADLE_USER_HOME']);junit=next(gradle.glob('caches/modules-2/files-2.1/junit/junit/4.13.2/*/*.jar'));hamcrest=next(gradle.glob('caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/*.jar'));commands=[]
 def run(cmd,log):
  cmd=list(map(str,cmd));p=subprocess.run(cmd,cwd=ROOT,capture_output=True,text=True,timeout=180);(OUT/(mode+'-'+log)).write_text(p.stdout+p.stderr);commands.append(dict(command=cmd,exitCode=p.returncode,log=mode+'-'+log));p.check_returncode();return p.stdout
 tree=ast.parse((ROOT/'tools/r31/run_task_runtime_host.py').read_text());adapters=next(ast.literal_eval(n.value) for n in ast.walk(tree) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='adapters' for t in n.targets));shims=[]
 for p in (ROOT/'tools/r29/qual_host/shim').rglob('*.java'):
  text=p.read_text();target=sources/p.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True)
  if p.name=='SQLiteDatabase.java':
   text=text.replace('public class SQLiteDatabase {','public class SQLiteDatabase implements AutoCloseable {');at=text.index('    static final class Rows');text=text[:at]+'''public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;public interface CursorFactory{}private boolean successful;public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public void setTransactionSuccessful(){successful=true;}public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}finally{try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}}public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public static SQLiteDatabase openDatabase(String path,CursorFactory f,int flags){throw new UnsupportedOperationException("NOT_USED_BY_LIVE_FORK_FIXTURE");}'''+text[at:]
  target.write_text(text);shims.append(target)
 for name,text in adapters.items():
  p=sources/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);shims.append(p)
 cp=os.pathsep.join(map(str,[sdk,frozen,junit,hamcrest]));run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',platform,*shims],'platform.log')
 paths=[MAIN/'FeatureCacheR27.java',MAIN/'RuntimeR27.java'];inputs=[*paths,ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2/R27ReplayTest.java',OUT/'LiveFamilyForkHost.java',Path(__file__).resolve()];before={str(p.relative_to(ROOT)):sha(p) for p in inputs};compilepaths=[]
 for p in paths:
  if mode=='baseline':target=sources/p.name;target.write_bytes(subprocess.check_output(['git','show',BASE+':'+str(p.relative_to(ROOT))],cwd=ROOT));compilepaths.append(target)
  else:compilepaths.append(p)
 run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',production,*compilepaths],'production.log');runtime=os.pathsep.join(map(str,[platform,production,test,frozen,jdbc,junit,hamcrest,sdk]));testroot=ROOT/'app/src/test/java/com/ethk3/novaonepro/strategyv2';run(['javac','-sourcepath','','-encoding','UTF-8','-cp',runtime,'-d',test,testroot/'R27TestSupport.java',OUT/'LiveFamilyForkHost.java'],'host.log');data=json.loads(run(['java','-Xmx512m','-cp',runtime,'com.ethk3.novaonepro.strategyv2.LiveFamilyForkHost',mode,tmp/'native.db'],'native.log'));(OUT/(mode+'-result.json')).write_text(json.dumps(data,indent=2)+'\n');assert before=={str(p.relative_to(ROOT)):sha(p) for p in inputs};receipt=dict(status='TARGETED_HOST_PASS',baseline=BASE,mode=mode,scope=data['scope'],sourceSha256=before,compiledProduction={p.name:sha(p) for p in compilepaths},supportJarSha256=sha(frozen),supportSourceCommit=BA35,sourceStableDuringRun=True,assertions=data['assertions'],commands=commands,work=str(tmp),device='NOT_RUN',quality='NOT_EVALUATED');(OUT/(mode+'-RECEIPT.json')).write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
if __name__=='__main__':main()
