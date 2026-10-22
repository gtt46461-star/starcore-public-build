#!/usr/bin/env python3
import argparse,hashlib,json,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for k in ('jdk','classes','android-runtime','out'):p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--source-revision');p.add_argument('--final-bytecode',action='store_true');a=p.parse_args()
if a.final_bytecode and a.source_revision:p.error('source revision incompatible with final bytecode')
root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False);test=out/'tests';test.mkdir()
cp=':'.join(map(str,[test,a.classes.resolve(),a.android_runtime.resolve()]));source=root/'app/src/main/java/com/ethk3/novaonepro/LiveResearchDiagnosisOrchestrator.java'
if a.source_revision:
    revision=out/source.name;revision.write_bytes(subprocess.check_output(['git','show',a.source_revision+':'+str(source.relative_to(root))],cwd=root));source=revision
def hashes():return {str(v.relative_to(a.classes.resolve())):hashlib.sha256(v.read_bytes()).hexdigest() for v in sorted((a.classes.resolve()/'com/ethk3/novaonepro').glob('LiveResearchDiagnosisOrchestrator*.class'))}
before=hashes();records=[]
def run(label,cmd,required=True):
    log=out/(label+'.log')
    with log.open('w') as f:r=subprocess.run(list(map(str,cmd)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
    records.append({'step':label,'exit_code':r.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()});(out/'steps.json').write_text(json.dumps(records,indent=2)+'\n')
    if required and r.returncode:print(log.read_text()[-5000:]);raise SystemExit(r.returncode)
    return r.returncode
fixture=root/'verification/r76/runtime_diagnosis.java';sources=[fixture] if a.final_bytecode else [source,fixture]
run('compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty','-cp',cp,'-d',test,*sources])
status=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.runtime_diagnosis',out/'result.json'],False)
after=hashes();assert before and before==after
if a.final_bytecode:assert not any((a.classes.resolve()/v.relative_to(test)).exists() for v in test.rglob('*.class'))
(out/'product-bytecode.json').write_text(json.dumps({'mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'ISOLATED_SOURCE_REGRESSION','unchanged':before==after,'before':before,'after':after},indent=2)+'\n')
(out/'sources.json').write_text(json.dumps({str(v):hashlib.sha256(v.read_bytes()).hexdigest() for v in sources},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
