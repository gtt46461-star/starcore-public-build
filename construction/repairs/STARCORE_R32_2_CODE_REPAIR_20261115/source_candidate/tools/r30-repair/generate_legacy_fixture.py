"""Freeze an actual legacy encoder result; never derive old bytes by truncating today's format."""
import hashlib,json,os,pathlib,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2]
base='b225ab204db043b347f9d76d6c5c1376fd9a2590'
name='app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java'
source=subprocess.check_output(['git','show',base+':'+name],cwd=root)
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
target=root/'app/src/test/resources/r30-repair-legacy-v1.bin';target.parent.mkdir(parents=True,exist_ok=True)
with tempfile.TemporaryDirectory(prefix='r30-real-legacy-') as tmp:
 out=pathlib.Path(tmp);old=out/'AtomNetSessionR30.java';old.write_bytes(source)
 subprocess.run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[classes,sdk])),'-d',str(out),str(old),str(root/'tools/r30-repair/qual_host/EmitLegacyOuterR30.java')],check=True)
 subprocess.run(['java','-cp',os.pathsep.join(map(str,[out,classes,sdk])),'com.ethk3.novaonepro.strategyv2.EmitLegacyOuterR30',str(target)],check=True)
meta={'encoderSourceCommit':base,'encoderPath':name,'encoderSourceSha256':hashlib.sha256(source).hexdigest(),'artifactSha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size,'scope':'SYNTHETIC_ONE_REAL_64_SAMPLE_GRADIENT_UNIT; ORIGINAL_V1_OUTER_ENCODER; CURRENT_COMPATIBLE_INNER_OPTIMIZER_CODEC; NOT_DEVICE_OR_NATURAL_QUALITY'}
target.with_suffix('.json').write_text(json.dumps(meta,indent=2)+'\n');print(json.dumps(meta))
