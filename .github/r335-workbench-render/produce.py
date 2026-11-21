"""Generate deterministic synthetic inputs with the checked-out source and approved original Android/JDBC adapter."""
from pathlib import Path
import hashlib,os,subprocess,sys
root=Path('app-source').resolve()
work=Path(os.environ['RUNNER_TEMP'])/'controlled-workbench';work.mkdir()
kit=Path(__file__).resolve().parent
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
platform=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
sys.path.insert(0,str(root/'tools/r31'))
from host_runtime_fixture import compile_owned_platform
shim=compile_owned_platform(root,work/'adapter',classes,platform)
jdbc=work/'sqlite.jar'
subprocess.run(['curl','--fail','--silent','--show-error','--retry','3','https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar','-o',jdbc],check=True)
assert hashlib.sha256(jdbc.read_bytes()).hexdigest()=='4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa'
testing=work/'test-classes';testing.mkdir()
path=lambda *xs:os.pathsep.join(map(str,xs))
subprocess.run(['javac','-encoding','UTF-8','-cp',path(shim,classes,platform),'-d',testing,kit/'PrepareChartInput.java'],check=True)
assets=root/'app/src/androidTest/assets';assets.mkdir(parents=True,exist_ok=True)
db=assets/'r335-generated-render.db';assert not db.exists()
subprocess.run(['java','-Xmx750m','-cp',path(shim,testing,classes,jdbc,platform),'com.ethk3.novaonepro.strategyv2.PrepareChartInput',db],check=True)
