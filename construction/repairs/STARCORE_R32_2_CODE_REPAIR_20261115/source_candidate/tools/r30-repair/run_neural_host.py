"""Production model/store bytecode with native SQLite; no phone or natural-quality claim."""
import pathlib,os,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'tools/r31'))
from host_runtime_fixture import compile_owned_platform
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-neural-host-') as temporary:
    out=pathlib.Path(temporary);test=out/'test';test.mkdir()
    shim=compile_owned_platform(root,out/'owned-platform',classes,sdk)
    sources=[root/'tools/r31/host/R31OwnedRuntimeFixture.java',root/'tools/r30-repair/qual_host/RepairNeuralR30HostTest.java',root/'tools/r30/qual_host/ModelSurfaceR30HostTest.java',root/'tools/r30/qual_host/TenPositionR30HostTest.java']
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',test,*sources])
    cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
    run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.RepairNeuralR30HostTest',root/'app/src/test/resources/r30-repair-legacy-v1.bin'])
    for mode in ['valid','missing','target','race']:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.ModelSurfaceR30HostTest',mode])
    for stop in [4,5,9]:run(['java','-Xmx768m','-Dr30.host.stopPosition='+str(stop),'-cp',cp,'com.ethk3.novaonepro.strategyv2.TenPositionR30HostTest'])
