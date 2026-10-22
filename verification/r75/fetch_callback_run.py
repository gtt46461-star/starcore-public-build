#!/usr/bin/env python3
"""Run the real Service callback with real SQLite and injected maintenance duration."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for key in ('jdk','classes','schema','out'):
    p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--source-revision')
p.add_argument('--final-bytecode',action='store_true',help='Use final Service bytecode; owner-work collaborators remain explicitly injected')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
if a.final_bytecode and a.source_revision:p.error('--final-bytecode cannot use --source-revision')
product=out/'product';product.mkdir();tests=out/'tests';tests.mkdir()
android=Path((a.schema/'host-classpath').read_text().strip().split(':')[-1])
resources=root/'app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/processReleaseResources/R.jar'
base=[product,a.classes.resolve(),resources,android];cp=':'.join(map(str,base))
source=root/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java'
def product_hashes():
    base=a.classes.resolve()/'com/ethk3/novaonepro'
    return {str(v.relative_to(a.classes.resolve())):hashlib.sha256(v.read_bytes()).hexdigest() for v in sorted(base.glob('AutoFetchService*.class'))}
before=product_hashes()
if a.source_revision:
    path=out/'AutoFetchService.java';path.write_bytes(subprocess.check_output(['git','show',a.source_revision+':'+str(source.relative_to(root))],cwd=root));source=path
records=[]
def run(label,args,required=True):
    log=out/(label+'.log')
    with log.open('w') as f:result=subprocess.run(list(map(str,args)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
    records.append({'name':label,'exit_code':result.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    (out/'steps.json').write_text(json.dumps(records,indent=2)+'\n')
    if required and result.returncode:print(log.read_text()[-6000:]);raise SystemExit(result.returncode)
    return result.returncode
if not a.final_bytecode:run('product-compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty-sourcepath','-cp',cp,'-d',product,source])
if a.final_bytecode:
    schema_overlays=[str(v.relative_to(a.schema.resolve()/'classes')) for v in (a.schema.resolve()/'classes').rglob('*.class')
                     if (a.classes.resolve()/v.relative_to(a.schema.resolve()/'classes')).is_file()]
    assert not schema_overlays,'UNDECLARED_SCHEMA_PRODUCT_OVERLAY:'+str(schema_overlays)
cp=':'.join(map(str,[tests,product,a.schema.resolve()/'classes',a.classes.resolve(),resources,android]))
test=root/'verification/r75/fetch_callback.java'
run('test-compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty-sourcepath','-cp',cp,'-d',tests,test,
    root/'verification/r973/clock-stubs/android/os/Handler.java',root/'verification/r72/realtime/android/os/SystemClock.java',root/'verification/r974/HostProductR974.java'])
status=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.fetch_callback',root/'verification/canonical/schema/sqlite_bridge.py',out/'wheel.db',out/'result.json'],False)
(out/'source-hashes.json').write_text(json.dumps({'AutoFetchService.java':hashlib.sha256(source.read_bytes()).hexdigest(),'fetch_callback.java':hashlib.sha256(test.read_bytes()).hexdigest()},indent=2)+'\n')
after=product_hashes();assert before and before==after,'PRODUCT_SERVICE_BYTECODE_CHANGED_DURING_TEST'
if a.final_bytecode:
    assert not list(product.rglob('*.class')),'SERVICE_PRODUCT_OVERLAY_FORBIDDEN'
    allowed=('DataRetentionGovernorR900','StorageReclaimR943','ResearchRuntimeSqlGateR911',
             'ResearchCaptureQueueR946','ResearchLearningContinuityR912','ServiceHeartbeatStore')
    overlays=[str(v.relative_to(tests)) for v in tests.rglob('*.class') if (a.classes.resolve()/v.relative_to(tests)).is_file()]
    assert all(Path(v).stem.split('$')[0] in allowed for v in overlays),'UNDECLARED_WORK_OWNER_OVERLAY:'+str(overlays)
(out/'product-bytecode.json').write_text(json.dumps({'mode':'FINAL_SERVICE_BYTECODE_WITH_EXPLICIT_OWNER_WORK_FIXTURES' if a.final_bytecode else 'ISOLATED_SOURCE_CALLBACK_REGRESSION','declared_fixture_overlays':overlays if a.final_bytecode else [],'unchanged':before==after,'before':before,'after':after},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
