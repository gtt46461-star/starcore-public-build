#!/usr/bin/env python3
"""Exact-source strict Legacy migration on native SQLite and real JVM kill/restart."""
import argparse,datetime,hashlib,json,os,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro';classes=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes';sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 sources=[source/(x+'.java') for x in ['LegacyDdlR31','LegacySchemaCoordinatorR926','SchemaDomainCoordinatorR926','DbHelper','DbStore']]
 sources += [ROOT/'tools/r31/unified_host/StrictMigrationR31Host.java',ROOT/'tools/r31/unified_host/ColdSchemaR31Host.java',Path(__file__).resolve()]
 hashes={str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources};commands=[]
 def run(cmd,name):
  result=subprocess.run(list(map(str,cmd)),cwd=ROOT,capture_output=True,text=True,timeout=180);(out/name).write_text(result.stdout+result.stderr);commands.append({'command':list(map(str,cmd)),'exitCode':result.returncode,'log':name})
  if result.returncode:raise RuntimeError(name+'\n'+(result.stdout+result.stderr)[-5000:])
  return result.stdout
 with tempfile.TemporaryDirectory(prefix='r31-unified-migration-') as directory:
  work=Path(directory);platform=work/'platform';production=work/'production';test=work/'test'
  for x in [platform,production,test]:x.mkdir()
  shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'));shims.extend((ROOT/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
  log=work/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String a,String b){return 0;}public static int w(String a,String b,Throwable c){return 0;}}');shims.append(log)
  run(['javac','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',platform,*shims],'platform.log')
  run(['javac','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,platform,classes])),'-d',production,*sources[:4]],'production.log')
  cp=os.pathsep.join(map(str,[platform,production,test,classes,jdbc,slf,sdk]))
  run(['javac','-cp',cp,'-d',test,ROOT/'tools/r31/unified_host/StrictMigrationR31Host.java',ROOT/'tools/r31/unified_host/ColdSchemaR31Host.java'],'host.log')
  result=run(['java','-Xmx256m','-cp',cp,'com.ethk3.novaonepro.StrictMigrationR31Host','run',work/'migration.db'],'run.log');print(result)
  result=run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.ColdSchemaR31Host',work/'cold.db'],'cold.log');print(result)
 if hashes!={str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources}:raise RuntimeError('SOURCE_CHANGED_DURING_RUN')
 (out/'RECEIPT.json').write_text(json.dumps({'state':'PASS_HOST_NATIVE_ONLY','capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sources':hashes,'commands':commands,'realKillRestart':True,'criticalCommitsWhilePending':1,'sourceDb670MB':'NOT_RUN','Android':'NOT_RUN','physicalDevice':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_PROVEN'},ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
