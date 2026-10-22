"""Compile transport/test classes separately; never overlay product classes."""
from pathlib import Path
import subprocess,json,hashlib,os
R=Path(__file__).resolve().parents[2];W=R.parent
J=W/'offline_tools/toolchain/zulu17.62.17-ca-jdk17.0.17-linux_x64/bin'
P=Path(os.environ.get('STARCORE_PRODUCT_CLASSES',str(R/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))).resolve()
assert P.is_dir(),'ACTUAL_PRODUCT_CLASSES_REQUIRED'
A=W/'offline_tools/toolchain/android-sdk/platforms/android-36/android.jar'
O=Path(os.environ.get('STARCORE_HOST_ROOT',str(W/'audit_work/host'))).resolve();O.mkdir(parents=True,exist_ok=True)
C=O/'adapters';C.mkdir(exist_ok=True);T=O/'tests';T.mkdir(exist_ok=True)
sqlite=O/'SQLiteDatabase.java'
raw=(R/'verification/canonical/schema/SQLiteDatabase.java').read_text()
raw=raw.replace('public class SQLiteDatabase {','public class SQLiteDatabase implements java.lang.AutoCloseable {\n public void close(){shutdown();}')
sqlite.write_text(raw)
ad=[sqlite,*((R/'verification/r974/android').rglob('*.java'))]
ad=[p for p in ad if p==sqlite or p.name not in ('SQLiteDatabase.java','Looper.java')]
ad+=list((R/'verification/r932/android-json').rglob('*.java'))
ad += [R/p for p in ['verification/r967/host/android/content/ContentValues.java','verification/canonical/adapters/android/os/SystemClock.java','verification/r967/host/android/os/CancellationSignal.java','verification/canonical/adapters/android/os/Debug.java','verification/canonical/adapters/android/util/Base64.java','verification/canonical/adapters/android/system/Os.java','verification/canonical/schema/Looper.java','verification/canonical/adapters/android/content/res/AssetManager.java','verification/canonical/adapters/android/content/res/AssetFileDescriptor.java','verification/report1015/host/android/content/Context.java','verification/r77/adapters/android/app/Application.java']]
ad.append(R/'verification/r983/android/database/CursorWrapper.java')
cp=':'.join(map(str,[C,P,A]))
def compile(name,sources,out):
    cmd=[str(J/'javac'),'-encoding','UTF-8','-sourcepath',str(O/'empty'),'-cp',cp,'-d',str(out),*map(str,sources)]
    with (O/(name+'.log')).open('w')as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
compile('adapters',ad,C)
schema_main=O/'SchemaCaptureCP4.java'
schema_main.write_text((R/'verification/canonical/schema/SchemaCaptureCP4.java').read_text().replace('h.onCreate(db);','db.beginTransaction();try{h.onCreate(db);db.setTransactionSuccessful();}finally{db.endTransaction();}').replace('i<8','i<1024'))
tests=[schema_main]+[R/p for p in ['verification/r974/HostProductR974.java','verification/r982/TrainingFixtureCP11.java','verification/r75/MainlineCaptureR75.java','verification/r77/CaptureDeadlineR77.java','verification/r77/training_DirectorFairnessR77.java','verification/r982/PostProjectionRecoveryR982.java','verification/report1015/DrawIngestIdempotency.java','verification/r77/training_ContinuityR77.java','verification/r77/training_PreparedPageR77.java','verification/r77/training_CacheBoundR77.java']]
compile('tests',tests,T)
product={str(p.relative_to(P)):hashlib.sha256(p.read_bytes()).hexdigest() for p in P.rglob('*.class')}
for root in [C,T]:assert not(set(product)&{str(p.relative_to(root))for p in root.rglob('*.class')}),'PRODUCT_OVERLAY'
(O/'product_hashes.json').write_text(json.dumps(product,indent=2))
config={'java':str(J/'java'),'javac':str(J/'javac'),'product':str(P),'adapters':str(C),'tests':str(T),'android':str(A),'classpath':':'.join(map(str,[T,C,P,A])),'bridge':str(R/'verification/canonical/schema/sqlite_bridge.py')}
(O/'config.json').write_text(json.dumps(config,indent=2))
schema=Path(os.environ.get('STARCORE_HOST_SCHEMA',str(W/'audit_work/host-schema.db')));assert not schema.exists(),'FRESH_SCHEMA_REQUIRED'
with (O/'schema.log').open('w')as f:subprocess.run([config['java'],'-cp',config['classpath'],'com.ethk3.novaonepro.SchemaCaptureCP4',config['bridge'],str(schema)],stdout=f,stderr=subprocess.STDOUT,check=True)
print('HOST_TRANSPORT_AND_ACTUAL_PRODUCT_SCHEMA_READY')
