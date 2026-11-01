"""Production model/store bytecode with native SQLite; no phone or natural-quality claim."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-neural-host-') as temporary:
    out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}}')
    run(['javac','-nowarn','-encoding','UTF-8','-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),log])
    sources=[root/'tools/r30-repair/qual_host/RepairNeuralR30HostTest.java',root/'tools/r30/qual_host/ModelSurfaceR30HostTest.java',root/'tools/r30/qual_host/TenPositionR30HostTest.java']
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',test,*sources])
    cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
    run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.RepairNeuralR30HostTest',root/'app/src/test/resources/r30-repair-legacy-v1.bin'])
    for mode in ['valid','missing','target','race']:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ModelSurfaceR30HostTest',mode])
    for stop in [4,5,9]:run(['java','-Xmx768m','-Dr30.host.stopPosition='+str(stop),'-cp',cp,'com.ethk3.novaonepro.strategyv2.TenPositionR30HostTest'])
