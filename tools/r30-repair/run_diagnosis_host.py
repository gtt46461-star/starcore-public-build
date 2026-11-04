"""Source-bound diagnosis regressions. Native JDBC and bounded HOST adapters, never device claims."""
import os,pathlib,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2]
plan=root/'construction/plans/STARCORE_R30_FINAL_REPAIR_20261101'
source=root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
classes=pathlib.Path(os.environ.get('R30_DIAGNOSIS_CLASSES',str(root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes')))
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
assert (classes.is_dir() or classes.is_file()) and jdbc.is_file()
def run(argv):subprocess.run([str(a) for a in argv],cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='fr-diagnosis-') as temporary:
 out=pathlib.Path(temporary);pure=out/'pure';pure.mkdir()
 run(['javac','-encoding','UTF-8','-d',pure,*sorted((plan/'verification/adapters').rglob('*.java')),source/'DiagnosisTextSU2.java',source/'UpgradeDiagnosisSU2.java',plan/'verification/DiagnosisMinimalProbe.java',plan/'verification/DiagnosisRenderRegression.java'])
 run(['java','-cp',pure,'com.ethk3.novaonepro.strategyv2.DiagnosisRenderRegression',root/'tools/strategy_su2/fixtures/r16_diag'])
 shim=out/'shim';shim.mkdir()
 # The report's per-PID observation calls Android Process.myPid. android.jar throws
 # Stub! on a HOST JVM: adapt only that getter to this actual JVM PID. Java heap and
 # /proc PSS/RSS stay measured by the unmodified production MemoryBudget RuntimeProbe;
 # native allocation and unavailable Android owner facts retain their UNKNOWN states.
 pid_source=out/'Process.java'
 pid_source.write_text('package android.os;public final class Process{public static int myPid(){long pid=java.lang.ProcessHandle.current().pid();if(pid<=0||pid>Integer.MAX_VALUE)throw new IllegalStateException("HOST_PID_OUT_OF_ANDROID_INT_RANGE");return(int)pid;}}\n')
 run(['javac','-encoding','UTF-8','-d',shim,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),pid_source])
 test=out/'test';test.mkdir();cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 run(['javac','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30-repair/qual_host/DiagnosisReportR30R1HostTest.java'])
 run(['java','-cp',os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk])),'com.ethk3.novaonepro.strategyv2.DiagnosisReportR30R1HostTest'])
print('FR_B01_DIAGNOSIS_HOST_SCOPE_PASS_NOT_ANDROID_OR_NATURAL')
