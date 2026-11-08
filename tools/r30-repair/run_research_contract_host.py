"""R31 K07 research result contract and 16 logical views on original native SQLite shim. No device/quality claim."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='r31-research-contract-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}}')
 run(['javac','-nowarn','-encoding','UTF-8','-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),log])
 cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30-repair/qual_host/R31ResearchContractHost.java'])
 cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
 for name in ["R31ResearchContractHost"]:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.'+name])
