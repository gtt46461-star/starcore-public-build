"""Independent public MATRIX owner test on copied, already owner-trained/frozen databases."""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3, subprocess
p=argparse.ArgumentParser()
for key in ('jdk','schema','classes','repair','neural','out'):
    p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--overlay',type=Path)
a=p.parse_args(); root=Path(__file__).resolve().parents[2]; out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
classes=out/'classes';classes.mkdir(exist_ok=True)
snapshot=out/'matrix-product-snapshot';snapshot.mkdir(exist_ok=True)
matrix_origin=(a.overlay or a.classes).resolve()
origin_class_hashes={}
for family in ('MatrixRegistrationR75','MatrixSnapshotBusR952','MatrixSourceRouterCP12'):
    files=sorted((matrix_origin/'com/ethk3/novaonepro/r952').glob(family+'*.class'))
    if not files:raise RuntimeError('Missing compiled matrix family: '+family)
    for compiled in files:
        relative=compiled.relative_to(matrix_origin);target=snapshot/relative;target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(compiled,target);digest=hashlib.sha256(compiled.read_bytes()).hexdigest()
        if hashlib.sha256(target.read_bytes()).hexdigest()!=digest:raise RuntimeError('Matrix class snapshot mismatch')
        origin_class_hashes[str(relative)]=digest
product_sources=[root/'app/src/main/java/com/ethk3/novaonepro/r952'/n for n in ('MatrixRegistrationR75.java','MatrixSnapshotBusR952.java','MatrixSourceRouterCP12.java')]
source_hashes={str(x.relative_to(root)):hashlib.sha256(x.read_bytes()).hexdigest() for x in product_sources}
class_hashes={str(x.relative_to(snapshot)):hashlib.sha256(x.read_bytes()).hexdigest() for x in snapshot.rglob('Matrix*.class')}
original=(a.schema/'host-classpath').read_text().strip().split(':')
cp=':'.join(map(str,[classes,a.schema/'classes',snapshot,a.classes,original[-1]]))
source=root/'verification/r75/matrix_registration_owner_R75.java'
with (out/'compile.log').open('w') as f:
    subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(classes),str(source),str(root/'verification/r72/realtime/android/os/SystemClock.java')],check=True,stdout=f,stderr=subprocess.STDOUT)
results=[]
for kind,fixture in [('repair',a.repair),('neural',a.neural)]:
    db=out/(kind+'.db')
    with sqlite3.connect('file:'+str(fixture.resolve())+'?mode=ro',uri=True) as src,sqlite3.connect(db) as dst:src.backup(dst)
    for phase in ('first','resume'):
        result=out/(kind+'-'+phase+'.json');log=out/(kind+'-'+phase+'.log')
        cmd=[str(a.jdk/'bin/java'),'-Djava.library.path='+str(root.parent/'host-native'),'-cp',cp,'com.ethk3.novaonepro.matrix_registration_owner_R75',str(root/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(out/(kind+'-app')),str(root/'app/src/main/assets'),phase,kind,str(result)]
        with log.open('w') as f: run=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
        if run.returncode:
            print(log.read_text()[-5000:]);raise SystemExit(run.returncode)
        results.append(json.loads(result.read_text()));print(kind,phase,results[-1]['checks'])
unchanged_classes=all(hashlib.sha256((matrix_origin/name).read_bytes()).hexdigest()==digest for name,digest in origin_class_hashes.items())
if not unchanged_classes:raise RuntimeError('Product matrix bytecode changed during run')
manifest={'status':'PASS','results':results,'fixtureSha256':{str(x):hashlib.sha256(x.read_bytes()).hexdigest() for x in (a.repair,a.neural)},'productSourceSha256':source_hashes,'isolatedClassSha256':class_hashes,'originClassSha256':origin_class_hashes,'originClassesUnchangedDuringRun':unchanged_classes,'snapshotExactlyMatchesOriginClasses':class_hashes==origin_class_hashes,'productClasses':str(a.classes.resolve()),'productOverlayUsed':a.overlay is not None,'productionSourceCompiledByRunner':False,'gitSourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'sourcesUnchangedDuringRun':all(hashlib.sha256(x.read_bytes()).hexdigest()==source_hashes[str(x.relative_to(root))] for x in product_sources),'testSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'runnerSha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
(out/'summary.json').write_text(json.dumps(manifest,indent=2)+'\n');print('PASS checks='+str(sum(x['checks'] for x in results)))
