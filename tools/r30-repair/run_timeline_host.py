"""Actual normalized timeline source and original JDBC SQLite adapter; no Android/device claims."""
import pathlib,os,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2];source=root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
sys.path.insert(0,str(root/'tools/r31'))
from host_tool_support import dependencies, run as host_run, pid_adapter
classes,sdk,resources,jdbc=dependencies(root)
def run(args):host_run(root,args)
with tempfile.TemporaryDirectory(prefix='repair-timeline-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String tag,String value){return 0;}public static int w(String tag,String value,Throwable failure){return 0;}}')
 run(['javac','-nowarn','-encoding','UTF-8','-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),log,pid_adapter(out)])
 run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',test,source/'IssueTimelineR30.java',source/'IssueTimelineWriterR30.java'])
 run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,test,classes,sdk])),'-d',test,root/'tools/r30-repair/qual_host/RepairTimelineR30HostTest.java'])
 run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk])),'com.ethk3.novaonepro.strategyv2.RepairTimelineR30HostTest'])
print('REPAIR_TIMELINE_ACTUAL_SOURCES_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID')
