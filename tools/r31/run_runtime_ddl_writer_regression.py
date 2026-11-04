"""Run the existing P01 writer Java regressions with a current Android support-classes directory.

Support classpath/Java launcher are configurable; transport dependencies compile from source.
The original Java drivers and assertions are unchanged.
"""
from datetime import datetime,timezone
import hashlib,json,os
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
original=ROOT/'tools/r31/run_write_protocol_host.py'
driver=original.read_text()
needle='CLASSES = ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"'
if driver.count(needle)!=1:raise RuntimeError('writer orchestration CLASSES binding changed; review required')
driver=driver.replace(needle,'CLASSES = pathlib.Path(os.environ.get("R31_HOST_SUPPORT_CLASSES", str(ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")))')
support_needle='support_classes = CLASSES'
if driver.count(support_needle)!=1:raise RuntimeError('writer support binding changed; review required')
driver=driver.replace(support_needle,'support_classes = os.pathsep.join([str(CLASSES), str(ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")])')
driver=driver.replace('["javac",','[os.environ.get("R31_HOST_JAVA","java"), "-m", "jdk.compiler/com.sun.tools.javac.Main",')
driver=driver.replace('["java",','[os.environ.get("R31_HOST_JAVA","java"),')
source_needle='SOURCE_HASHES = '
if driver.count(source_needle)!=1:raise RuntimeError('writer source binding changed; review required')
driver=driver.replace(source_needle,'PRODUCTION_SOURCES += [MAIN / "FetchLedgerR925.java", MAIN / "ApiClient.java"]\n'+source_needle)
paths=[ROOT/'app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java',ROOT/'tools/r31/host/R31WriteProtocolHost.java',ROOT/'tools/r31/host/R31QueueCancellationHost.java']
before={str(path.relative_to(ROOT)):hashlib.sha256(path.read_bytes()).hexdigest() for path in paths}
exec(compile(driver,str(original),'exec'),{'__file__':str(original),'__name__':'__main__'})
if before!={str(path.relative_to(ROOT)):hashlib.sha256(path.read_bytes()).hexdigest() for path in paths}:raise RuntimeError('writer regression input changed while running')
result=dict(status='HOST_PASS_NOT_ANDROID',capturedAt=datetime.now(timezone.utc).isoformat(),actualInputsSHA256=before,originalOrchestrationSHA256=hashlib.sha256(original.read_bytes()).hexdigest(),orchestrationAdaptation='Support classpath and Java/compiler launcher configurable; original Java drivers and assertions unchanged',supportClasses=os.environ.get('R31_HOST_SUPPORT_CLASSES',str(ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes')),hostJavaLauncher=os.environ.get('R31_HOST_JAVA','java'),regressions=['9 post-commit diagnostic phases','commit acknowledgement unknown/readback and later replay','typed storage/process fatal separation and cleanup','legacy outer rollback/cancelled SQL rollback','real host JVM halt after commit followed by fresh-JVM replay','10 original writer unit tests'],device='NOT_RUN',continuous24h='NOT_RUN',continuous72h='NOT_RUN',naturalQuality='NOT_PROVEN')
(ROOT/'verification/r31/p02/runtime-ddl-hotspots/WRITER_REGRESSION_RECEIPT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
