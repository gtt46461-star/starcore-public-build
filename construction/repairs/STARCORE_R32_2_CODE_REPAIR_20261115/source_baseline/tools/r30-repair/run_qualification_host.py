"""Actual qualification/confirmation/authority code on native SQLite; synthetic mechanism evidence only."""
import pathlib,os,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'tools/r31'))
from host_runtime_fixture import compile_owned_platform
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-qualification-host-') as temporary:
    out=pathlib.Path(temporary);test=out/'test';test.mkdir();authority=out/'authority';authority.mkdir()
    shim=compile_owned_platform(root,out/'owned-platform',classes,sdk)
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',test,root/'tools/r31/host/R31OwnedRuntimeFixture.java',root/'tools/r30-repair/qual_host/RepairQualificationR30HostTest.java',root/'tools/r30/qual_host/FrozenConfirmationR30HostTest.java',root/'tools/r30/qual_host/DecisionExplanationR30HostTest.java'])
    cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
    for name in ['RepairQualificationR30HostTest','FrozenConfirmationR30HostTest','DecisionExplanationR30HostTest']:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.'+name])
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',authority,root/'tools/r30/qual_host/authority_shim/com/ethk3/novaonepro/DbStore.java'])
    acp=os.pathsep.join(map(str,[authority,shim,test,classes,jdbc,sdk]))
    run(['javac','-nowarn','-encoding','UTF-8','-cp',acp,'-d',test,root/'tools/r30/qual_host/RealAuthorityR30HostTest.java',root/'tools/r30/qual_host/CertificateRevocationR30HostTest.java'])
    for name in ['RealAuthorityR30HostTest','CertificateRevocationR30HostTest']:run(['java','-Xmx768m','-cp',acp,'com.ethk3.novaonepro.strategyv2.'+name])
