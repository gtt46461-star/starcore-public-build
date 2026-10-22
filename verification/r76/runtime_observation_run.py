#!/usr/bin/env python3
"""Real heartbeat/pressure owner entry points; only Android I/O boundaries adapted.

Source mode compiles the two owners in an isolated overlay (optionally old git ref).
Final mode prohibits product overlays and verifies unchanged release class hashes.
No database, production checkpoint, grant, prediction or natural sample is synthesized.
"""
import argparse, hashlib, json, subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for k in ('jdk','classes','android-runtime','out'):
    p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--source-revision')
p.add_argument('--final-bytecode',action='store_true')
p.add_argument('--case',choices=('all','heartbeat','pressure'),default='all')
a=p.parse_args()
if a.final_bytecode and a.source_revision:p.error('final bytecode cannot use source revision')
root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
product=out/'product';product.mkdir();fixtures=out/'fixtures';fixtures.mkdir()
classes=a.classes.resolve();cp=':'.join(map(str,[fixtures,product,classes,a.android_runtime.resolve()]))
owners=('ServiceHeartbeatStore','RealtimePressureGateR811','ReportExportServiceCP4')
def hashes():
    return {str(v.relative_to(classes)):hashlib.sha256(v.read_bytes()).hexdigest()
        for name in owners for v in sorted((classes/'com/ethk3/novaonepro').glob(name+'*.class'))}
before=hashes();sources=[root/'app/src/main/java/com/ethk3/novaonepro'/s for s in ('ServiceHeartbeatStore.java','RealtimePressureGateR811.java')]
if a.source_revision:
    for i,source in enumerate(sources):
        dest=out/source.name;dest.write_bytes(subprocess.check_output(['git','show',a.source_revision+':'+str(source.relative_to(root))],cwd=root));sources[i]=dest
records=[]
def run(label,cmd,required=True):
    log=out/(label+'.log')
    with log.open('w') as f:r=subprocess.run(list(map(str,cmd)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
    records.append({'step':label,'exit_code':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    (out/'steps.json').write_text(json.dumps(records,indent=2)+'\n')
    if required and r.returncode:print(log.read_text()[-8000:]);raise SystemExit(r.returncode)
    return r.returncode
if not a.final_bytecode:run('product_compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty','-cp',cp,'-d',product,*sources])
test_sources=[root/v for v in (
    'verification/r76/runtime_observation.java',
    'verification/r76/runtime_observation_adapters/android/util/AtomicFile.java',
    'verification/r72/realtime/android/os/SystemClock.java',
    'verification/canonical/schema/Looper.java',
    'verification/canonical/adapters/android/os/Process.java',
    'verification/r991/android/os/Build.java',
    'verification/r36-stubs/android/os/StatFs.java',
    'verification/r36-stubs/android/util/Log.java')]
run('fixture_compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty','-cp',cp,'-d',fixtures,*test_sources])
status=run('test',[a.jdk/'bin/java','-Djava.security.manager=allow','-cp',cp,'com.ethk3.novaonepro.runtime_observation',a.case,out/'context',out/'result.json'],False)
after=hashes();assert before and before==after,'PRODUCT_BYTECODE_CHANGED'
collisions=[str(v.relative_to(fixtures)) for v in fixtures.rglob('*.class') if (classes/v.relative_to(fixtures)).exists()]
assert not collisions,'PRODUCT_FIXTURE_COLLISION:'+str(collisions)
if a.final_bytecode:assert not list(product.rglob('*.class')),'FINAL_PRODUCT_OVERLAY_FORBIDDEN'
(out/'product-bytecode.json').write_text(json.dumps({'mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'ISOLATED_TWO_OWNER_SOURCE_REGRESSION','classes':str(classes),'unchanged':before==after,'productFixtureCollisions':collisions,'sourceRevision':a.source_revision,'before':before,'after':after},indent=2)+'\n')
(out/'sources.json').write_text(json.dumps({str(v):hashlib.sha256(v.read_bytes()).hexdigest() for v in sources+test_sources},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
