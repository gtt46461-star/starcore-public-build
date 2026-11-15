"""Actual production bytecode with native SQLite; this is not Android device acceptance."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2]
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-progress-host-') as temporary:
    out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir()
    run(['javac','-nowarn','-encoding','UTF-8','-cp',sdk,'-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java'))])
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',test,root/'tools/r30-repair/qual_host/RepairTaskProgressR30HostTest.java'])
    run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk])),'com.ethk3.novaonepro.strategyv2.RepairTaskProgressR30HostTest'])
