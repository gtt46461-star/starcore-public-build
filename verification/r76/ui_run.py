#!/usr/bin/env python3
"""Actual Activity/RuntimeTruth entrypoint regression against isolated source or final classes."""
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for k in ('jdk','classes','android-runtime','out'):p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--source-revision');p.add_argument('--final-bytecode',action='store_true')
p.add_argument('--case',choices=['all','pending','countdown','race'],default='all')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
if a.final_bytecode and a.source_revision:p.error('--final-bytecode cannot use --source-revision')
product=out/'product';product.mkdir();fixtures=out/'fixtures';fixtures.mkdir()
resources=root/'app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/processReleaseResources/R.jar'
cp=':'.join(map(str,[fixtures,product,a.classes.resolve(),resources,a.android_runtime.resolve()]))
def product_hashes():
    return {str(v.relative_to(a.classes.resolve())):hashlib.sha256(v.read_bytes()).hexdigest()
            for name in ('MainActivity','RuntimeTruthSnapshotR906','ResearchPipelineStatusR76')
            for v in sorted((a.classes.resolve()/'com/ethk3/novaonepro').glob(name+'*.class'))}
before=product_hashes()
sources=[root/'app/src/main/java/com/ethk3/novaonepro'/s for s in ['MainActivity.java','RuntimeTruthSnapshotR906.java']]
if a.source_revision:
    for i,source in enumerate(sources):
        dest=out/source.name;dest.write_bytes(subprocess.check_output(['git','show',a.source_revision+':'+str(source.relative_to(root))],cwd=root));sources[i]=dest
elif (root/'app/src/main/java/com/ethk3/novaonepro/ResearchPipelineStatusR76.java').exists():
    sources.append(root/'app/src/main/java/com/ethk3/novaonepro/ResearchPipelineStatusR76.java')
records=[]
def run(label,cmd,required=True):
    log=out/(label+'.log')
    with log.open('w') as f:r=subprocess.run(list(map(str,cmd)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
    records.append({'step':label,'exit_code':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    (out/'steps.json').write_text(json.dumps(records,indent=2)+'\n')
    if required and r.returncode:print(log.read_text()[-6000:]);raise SystemExit(r.returncode)
    return r.returncode
if not a.final_bytecode:run('product_compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty','-cp',cp,'-d',product,*sources])
test_sources=[root/'verification/r76/ui_snapshot.java',root/'verification/r76/android/os/SystemClock.java']
run('fixture_compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty','-cp',cp,'-d',fixtures,*test_sources])
status=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.ui_snapshot',a.case,out/'result.json'],False)
after=product_hashes();assert before and before==after,'PRODUCT_BYTECODE_CHANGED_DURING_UI_TEST'
if a.final_bytecode:
    assert not list(product.rglob('*.class')),'PRODUCT_SOURCE_OVERLAY_FORBIDDEN'
    assert not any((a.classes.resolve()/v.relative_to(fixtures)).exists() for v in fixtures.rglob('*.class')),'PRODUCT_FIXTURE_OVERLAY_FORBIDDEN'
(out/'product-bytecode.json').write_text(json.dumps({'mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'ISOLATED_SOURCE_REGRESSION','classes':str(a.classes.resolve()),'unchanged':before==after,'before':before,'after':after},indent=2)+'\n')
(out/'sources.json').write_text(json.dumps({str(v):hashlib.sha256(v.read_bytes()).hexdigest() for v in sources+test_sources},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
