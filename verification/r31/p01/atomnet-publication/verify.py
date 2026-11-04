#!/usr/bin/env python3
"""Compile actual owners ahead of frozen actual Android dependencies; native SQLite HOST only."""
from pathlib import Path
import ast,hashlib,json,os,shutil,subprocess,tempfile,sys
ROOT=Path(__file__).resolve().parents[4];OUT=Path(__file__).resolve().parent
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    work=Path(tempfile.mkdtemp(prefix='r31-atomnet-native-'));platform,production,test,sources=[work/n for n in ('platform','production','test','sources')]
    for p in (platform,production,test,sources):p.mkdir()
    support=work/'support.jar';shutil.copyfile('/workspace/r31-shared-bootstrap-30ff-frozen/classes.jar',support);assert sha(support)=='582f7475b43d218c50d34f8f24d4399ae4916b53579807c22c162bd25b7fea56'
    sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')))/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));commands=[]
    def run(command,log,expected=0):
        cmd=list(map(str,command));r=subprocess.run(cmd,cwd=ROOT,text=True,capture_output=True,timeout=180);(OUT/log).write_text(r.stdout+r.stderr);commands.append(dict(command=cmd,exitCode=r.returncode,log=log));assert r.returncode==expected,(cmd,r.returncode,(r.stdout+r.stderr)[-2000:]);return r.stdout
    tree=ast.parse((ROOT/'tools/r31/run_task_runtime_host.py').read_text());adapters=next(ast.literal_eval(n.value) for n in ast.walk(tree) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='adapters' for t in n.targets))
    # The actual DbStore is used. Only Android framework/SQLiteOpenHelper are platform adapters.
    adapters.pop('com/ethk3/novaonepro/DbStore.java',None)
    adapters['android/content/Context.java']='''package android.content;public class Context{private final java.io.File database;public Context(java.io.File database){this.database=database;}public java.io.File getDatabasePath(String name){return database;}public java.io.File getFilesDir(){java.io.File f=new java.io.File(database.getParentFile().getParentFile(),"files");f.mkdirs();return f;}public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}public Context getApplicationContext(){return this;}}'''
    adapters['android/app/Application.java']='''package android.app;public class Application extends android.content.Context{public Application(java.io.File database){super(database);}}'''
    adapters['android/content/res/AssetManager.java']='''package android.content.res;public class AssetManager{public java.io.InputStream open(String path)throws java.io.IOException{return new java.io.FileInputStream(new java.io.File(System.getProperty("r31.assets"),path));}}'''
    adapters['android/os/SystemClock.java']='''package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}'''
    adapters['android/system/ErrnoException.java']='''package android.system;public class ErrnoException extends Exception{public ErrnoException(String op,int code){super(op+":"+code);}}'''
    adapters['android/system/OsConstants.java']='''package android.system;public final class OsConstants{public static final int O_RDONLY=0;}'''
    adapters['android/system/Os.java']='''package android.system;public final class Os{private static final java.util.Map<java.io.FileDescriptor,java.nio.channels.FileChannel> files=new java.util.IdentityHashMap<>();public static synchronized java.io.FileDescriptor open(String path,int flags,int mode)throws ErrnoException{try{java.io.FileDescriptor d=new java.io.FileDescriptor();files.put(d,java.nio.channels.FileChannel.open(java.nio.file.Paths.get(path),java.nio.file.StandardOpenOption.READ));return d;}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}public static synchronized void fsync(java.io.FileDescriptor d)throws ErrnoException{try{files.get(d).force(true);}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}public static synchronized void close(java.io.FileDescriptor d)throws ErrnoException{try{java.nio.channels.FileChannel c=files.remove(d);if(c!=null)c.close();}catch(java.io.IOException e){throw new ErrnoException(e.toString(),1);}}}'''
    shims=[]
    for p in (ROOT/'tools/r29/qual_host/shim').rglob('*.java'):
        text=p.read_text();target=sources/p.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True)
        if p.name=='SQLiteDatabase.java':
            text=text.replace('public class SQLiteDatabase {','public class SQLiteDatabase implements java.lang.AutoCloseable {')
            at=text.index('    static final class Rows')
            text=text[:at]+'''public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;public interface CursorFactory{}private boolean successful;public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path));}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}public void setTransactionSuccessful(){successful=true;}public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}finally{try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}}public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}'''+text[at:]
        target.write_text(text);shims.append(target)
    for name,text in adapters.items():
        p=sources/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);shims.append(p)
    cp=os.pathsep.join(map(str,(support,sdk)));run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',platform,*shims],'native-platform.log')
    names=['AtomNetPublicationR31','AtomNetStoreR30','AtomNetSessionR30','ModelArtifactR29','ModelSurfaceStoreR30','ModelSurfaceProviderR30','CheckpointArtifactsR26','CheckpointReferencesR26','AtomNetRuntimeR30','ModelReplayStudyR30R1','CapabilityRegistryStoreR30','ResearchProvenanceGraphR30R1']
    paths=[MAIN/'strategyv2'/(name+'.java') for name in names];compile_paths=paths
    before={str(p):sha(p) for p in compile_paths};cp=os.pathsep.join(map(str,(platform,support,sdk)))
    run(['javac','-sourcepath','','-implicit:none','-encoding','UTF-8','-cp',cp,'-d',production,*compile_paths],'native-production.log')
    runtime=os.pathsep.join(map(str,(platform,production,test,support,jdbc,sdk)))
    setup=dict(work=str(work),platform=str(platform),production=str(production),test=str(test),runtime=runtime,sourceSha256=before,supportSha256=sha(support),commands=commands,sourceStable=before=={str(p):sha(p) for p in compile_paths})
    (OUT/'COMPILE_SETUP.json').write_text(json.dumps(setup,indent=2)+'\n');print(json.dumps(setup))
if __name__=='__main__':main()
