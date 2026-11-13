#!/usr/bin/env python3
"""Actual legacy replay + native SQLite + SIGKILL/reopen controls; never Android/device evidence."""
import argparse,hashlib,json,os,pathlib,signal,subprocess,tempfile,time
ROOT=pathlib.Path(__file__).resolve().parents[2]
def sha(path):return hashlib.sha256(pathlib.Path(path).read_bytes()).hexdigest()
def identity(path):
 path=pathlib.Path(path).resolve()
 if path.is_file():return {'kind':'FILE','sha256':sha(path),'files':1}
 rows=[(str(p.relative_to(path)),sha(p)) for p in sorted(path.rglob('*')) if p.is_file()] if path.is_dir() else []
 if not rows:raise RuntimeError('COMPILED_SUPPORT_MISSING_OR_EMPTY')
 return {'kind':'DIRECTORY','sha256':hashlib.sha256(json.dumps(rows,separators=(',',':')).encode()).hexdigest(),'files':len(rows)}
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=pathlib.Path);p.add_argument('--classes',type=pathlib.Path);p.add_argument('--controls-only',action='store_true');p.add_argument('--baseline-legacy-source',type=pathlib.Path);a=p.parse_args()
 out=(a.output or pathlib.Path(tempfile.mkdtemp(prefix='r31-replay-checkpoint-'))).resolve();out.mkdir(parents=True,exist_ok=True)
 if any(out.iterdir()):raise RuntimeError('FRESH_OUTPUT_DIRECTORY_REQUIRED')
 baseline_before=sha(a.baseline_legacy_source) if a.baseline_legacy_source else None
 classes=(a.classes or pathlib.Path(os.environ.get('R31_CLASSES',os.environ.get('R31_HOST_CLASSES',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes')))).resolve();support_before=identity(classes)
 sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));libraries={str(f):sha(f) for f in [sdk,jdbc]}
 # Freshly compile the whole numerical/replay/owner package, plus the actual DB owner.
 production=[f for f in sorted((ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2').glob('*.java')) if f.name!='StrategyResearchActivitySU1.java']+[ROOT/'app/src/main/java/com/ethk3/novaonepro'/(s+'.java') for s in ['DatabaseCoordinator','DbStore','DbHelper']]
 harness=ROOT/'tools/r31/replay_checkpoint_host/LegacyReplayCheckpointHostR31.java'
 original=[ROOT/'tools/r31/ddl_host'/s for s in ['android/database/Cursor.java','android/content/ContentValues.java','android/database/sqlite/SQLiteOpenHelper.java']]
 original += [ROOT/'verification/r31/warm_abi_protocol/adapters/android/database/sqlite/SQLiteDatabase.java',ROOT/'verification/r31/warm_abi_protocol/adapters/android/os/StatFs.java',ROOT/'verification/r73/adapters/android/system/Os.java']
 original += list((ROOT/'tools/r29/qual_host/shim/android/database/sqlite').glob('*Exception.java'))+sorted((ROOT/'verification/r932/android-json/org/json').glob('*.java'))
 bound=production+[harness,pathlib.Path(__file__).resolve()]+original;before={str(f.relative_to(ROOT)):sha(f) for f in bound}
 generated=out/'platform-source';platform=out/'platform';compiled=out/'production';drivers=out/'drivers'
 for d in [generated,platform,compiled,drivers]:d.mkdir(exist_ok=True)
 platform_sources=[]
 for f in original:
  if '/android/' in str(f):relative='android/'+str(f).split('/android/',1)[1]
  else:relative='org/json/'+f.name
  target=generated/relative;target.parent.mkdir(parents=True,exist_ok=True);s=f.read_text()
  if f.name=='SQLiteDatabase.java':
   s=s.replace('public final Connection jdbc;','public static java.util.function.Consumer<String> beforeExecR31; public final Connection jdbc;')
   s=s.replace('public void execSQL(String sql,Object[] args){log(', 'public void execSQL(String sql,Object[] args){if(beforeExecR31!=null)beforeExecR31.accept(sql);log(')
   s=s.replace('System.out.println("FRAME "+JsonR27.write(row));System.out.flush();','')
  target.write_text(s);platform_sources.append(target)
 adapters={
 'android/content/Context.java':'package android.content;public class Context{public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}public java.io.File getDatabasePath(String name){return new java.io.File(name);}public java.io.File getFilesDir(){return new java.io.File("files");}public Context getApplicationContext(){return this;}}',
 'android/content/res/AssetManager.java':'package android.content.res;public class AssetManager{public java.io.InputStream open(String name)throws java.io.IOException{return new java.io.FileInputStream(new java.io.File(System.getProperty("starcore.host.assets"),name));}}',
 'android/app/Application.java':'package android.app;public class Application extends android.content.Context{private final java.io.File database;public Application(java.io.File file){database=file;}public java.io.File getDatabasePath(String name){return database;}public java.io.File getFilesDir(){java.io.File f=new java.io.File(database.getParentFile(),"files");f.mkdirs();return f;}}',
 'android/system/OsConstants.java':'package android.system;public final class OsConstants{public static final int O_RDONLY=0;}',
 'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000L;}public static long elapsedRealtimeNanos(){return System.nanoTime();}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
 'android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
 'android/os/Process.java':'package android.os;public final class Process{public static final int THREAD_PRIORITY_BACKGROUND=10;public static void setThreadPriority(int value){}public static int myPid(){return(int)java.lang.ProcessHandle.current().pid();}}',
 'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}public static int e(String t,String v,Throwable e){return 0;}public static int d(String t,String v){return 0;}public static int i(String t,String v){return 0;}}'}
 for name,s in adapters.items():target=generated/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(s);platform_sources.append(target)
 generated_before={str(f.relative_to(out)):sha(f) for f in platform_sources};commands=[]
 def run(name,argv,expected=0,timeout=240):
  with (out/(name+'.log')).open('w') as log:r=subprocess.run(list(map(str,argv)),cwd=out,stdout=log,stderr=subprocess.STDOUT,timeout=timeout)
  commands.append({'name':name,'exit':r.returncode,'logSha256':sha(out/(name+'.log'))});print(name,r.returncode,flush=True)
  if r.returncode!=expected:raise RuntimeError('CONTROL_FAILED:'+name)
 javac=['javac','-proc:none','-implicit:none','-encoding','UTF-8','-sourcepath','']
 run('platform-javac',javac+['-cp',os.pathsep.join(map(str,[sdk,classes,jdbc])),'-d',platform]+platform_sources)
 fresh=list(production)
 if a.baseline_legacy_source:fresh=[f for f in fresh if f.name!='LegacyReplayR26.java']+[a.baseline_legacy_source.resolve()]
 run('production-javac',javac+['-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',compiled]+fresh)
 cp=os.pathsep.join(map(str,[platform,compiled,classes,jdbc,sdk]));run('drivers-javac',javac+['-cp',cp,'-d',drivers,harness]);cp=str(drivers)+os.pathsep+cp
 executed={str(d.relative_to(out)):identity(d) for d in [platform,compiled,drivers]}
 java=['java','-Xmx768m','-Dstarcore.host.assets='+str(ROOT/'app/src/main/assets'),'-cp',cp,'com.ethk3.novaonepro.strategyv2.LegacyReplayCheckpointHostR31']
 if a.baseline_legacy_source:
  run('baseline-binding-refusal',java+['refusals',out/'refusals'],expected=1)
  log=(out/'baseline-binding-refusal.log').read_text();assert 'MALFORMED_BINDING_MUST_NOT_LEARN_snapshot' in log,'BASELINE_MUST_FAIL_THE_TARGET_ASSERTION'
 else:
  run('binding-refusals',java+['refusals',out/'refusals'])
  run('generation-resume-refusal',java+['generation-refusal',out/'generation-refusal'])
  if not a.controls_only:
   run('golden',java+['golden',out/'golden'])
   golden=json.loads((out/'golden/result.json').read_text());comparisons=[]
   for mode in ['kill-before','kill-after']:
    directory=out/mode;directory.mkdir();marker=directory/'ready'
    with (out/(mode+'.log')).open('w') as log:
     child=subprocess.Popen(list(map(str,java+[mode,directory])),cwd=out,stdout=log,stderr=subprocess.STDOUT)
     try:
      stop=time.monotonic()+180
      while not marker.exists() and child.poll() is None and time.monotonic()<stop:time.sleep(.05)
      if not marker.exists():raise RuntimeError('INTERRUPTION_EDGE_NOT_REACHED:'+mode)
      child.kill();child.wait(timeout=20);assert child.returncode==-signal.SIGKILL
     finally:
      if child.poll() is None:child.kill();child.wait(timeout=20)
    commands.append({'name':mode,'exit':child.returncode,'signal':'SIGKILL','logSha256':sha(out/(mode+'.log'))})
    run(mode+'-reopen',java+['resume',directory]);actual=json.loads((directory/'result.json').read_text())
    fields=['stateSha256','decisionSha256','checkpointHash','manifests','cursor','modelSteps'];assert all(golden[k]==actual[k] for k in fields),'RESTART_DIVERGED:'+mode
    comparisons.append({'edge':mode,'status':'PASS_HOST_SYNTHETIC','exactFields':fields})
 assert before=={str(f.relative_to(ROOT)):sha(f) for f in bound},'CURRENT_SOURCE_CHANGED_DURING_TEST'
 assert support_before==identity(classes),'COMPILED_SUPPORT_CHANGED_DURING_TEST'
 assert libraries=={str(f):sha(f) for f in [sdk,jdbc]},'LIBRARY_CHANGED_DURING_TEST'
 assert generated_before=={str(f.relative_to(out)):sha(f) for f in platform_sources},'PLATFORM_SOURCE_CHANGED_DURING_TEST'
 assert executed=={str(d.relative_to(out)):identity(d) for d in [platform,compiled,drivers]},'EXECUTED_BYTES_CHANGED_DURING_TEST'
 receipt={'status':'EXPECTED_RED_BASELINE' if a.baseline_legacy_source else 'PASS_HOST_CONTROLS_ONLY','sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceSha256':before,'freshProductionFiles':len(fresh),'compiledSupport':support_before,'librariesSha256':libraries,'generatedPlatformSourceSha256':generated_before,'executedClasses':executed,'commands':commands,'AndroidProcessOrScheduler':'NOT_RUN','deviceThroughputMemoryLatency':'NOT_RUN','naturalQuality':'NOT_APPLICABLE','futureFreezeDeadlineGate':'NOT_RUN','scope':'ACTUAL_LEGACY_REPLAY_FULL_NUMERICS_NATIVE_SQLITE_REAL_COORDINATOR_CAS_AND_LINUX_SIGKILL; ANDROID_TRANSPORTS_ADAPTED'}
 if a.baseline_legacy_source:
  assert baseline_before==sha(a.baseline_legacy_source),'BASELINE_SOURCE_CHANGED_DURING_TEST'
  receipt['baselineLegacySourceSha256']=baseline_before
 if not a.controls_only and not a.baseline_legacy_source:receipt['restartComparisons']=comparisons;receipt['golden']=golden
 (out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps({'status':receipt['status'],'sourceHead':receipt['sourceHead'],'receiptSha256':sha(out/'receipt.json'),'freshProductionFiles':len(fresh)}))
if __name__=='__main__':main()
