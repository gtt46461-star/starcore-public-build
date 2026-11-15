"""R31 K09 charts page reader (stable ids, per-panel isolation, DISPLAY-only cache GC) on native SQLite. No device claim."""
import pathlib,os,shutil,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='r31-chart-page-host-') as temporary:
 out=pathlib.Path(temporary);src=out/'src';shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir()
 shutil.copytree(root/'tools/r29/qual_host/shim',src)
 # the charts reader passes a CancellationSignal: use the chart shim's SQLiteDatabase (same JDBC binding plus that overload)
 shutil.copy(root/'tools/r30/qual_host/chart_shim/android/database/sqlite/SQLiteDatabase.java',src/'android/database/sqlite/SQLiteDatabase.java')
 log=src/'android/util/Log.java';log.parent.mkdir(parents=True,exist_ok=True);log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}public static int e(String t,String v,Throwable e){return 0;}public static int i(String t,String v){return 0;}public static int d(String t,String v){return 0;}}')
 run(['javac','-nowarn','-encoding','UTF-8','-cp',sdk,'-d',shim,*sorted(src.rglob('*.java'))])
 cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30-repair/qual_host/R31ChartPageHost.java'])
 cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
 run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.R31ChartPageHost'])
