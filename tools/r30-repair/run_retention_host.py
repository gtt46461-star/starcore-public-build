"""Actual new retention sources and native SQLite; temporary Android/JDBC adapters never enter the APK."""
import os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2]
main=root/'app/src/main/java/com/ethk3/novaonepro'
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-retention-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();tests=out/'tests';tests.mkdir()
 run(['javac','-encoding','UTF-8','-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),*sorted((root/'verification/r932/android-json').rglob('*.java'))])
 run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[shim,classes,sdk])),'-d',tests,main/'RetentionRulesSU2.java',main/'strategyv2/RetentionR26.java',root/'tools/r30-repair/qual_host/RepairRetentionR30HostTest.java'])
 run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[shim,tests,classes,jdbc,sdk])),'com.ethk3.novaonepro.strategyv2.RepairRetentionR30HostTest'])
print('REPAIR_RETENTION_PRODUCTION_SOURCES_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID_OR_PHYSICAL_DEVICE')
