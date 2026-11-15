"""Actual compiled matrix/schema and calibration task on original native SQLite shim. No device/quality claim."""
import pathlib,os,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sys.path.insert(0,str(root/'tools/r31'))
from host_tool_support import dependencies, run as host_run, pid_adapter
classes,sdk,resources,jdbc=dependencies(root)
def run(args):host_run(root,args)
with tempfile.TemporaryDirectory(prefix='repair-snapshot-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}}')
 run(['javac','-nowarn','-encoding','UTF-8','-cp',sdk,'-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),log,pid_adapter(out)])
 run(['javac','-nowarn','-d',shim,root/'tools/r30/qual_host/report_shim/android/os/Build.java'])
 cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30/qual_host/ReportSnapshotR30HostTest.java'])
 cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
 for name in ['ReportSnapshotR30HostTest']:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.'+name])
