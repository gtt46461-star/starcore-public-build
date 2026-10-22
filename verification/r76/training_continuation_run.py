#!/usr/bin/env python3
"""Normal final bytecode plus separately labelled generated-input mutation tests."""
import argparse,json,hashlib,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for name in ('classes','jdk','out'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--mutations-only',action='store_true');a=p.parse_args()
r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=False)
classes=a.classes.resolve();jdk=a.jdk.resolve()/'bin';fixture=r/'verification/r76/training_continuation.java'
owners=['TrainingContinuationR973','TrainingCodeIdentityR964','ResearchSemanticsGeneratedR967']
def hashes():return {n:hashlib.sha256((classes/'com/ethk3/novaonepro'/ (n+'.class')).read_bytes()).hexdigest() for n in owners}
before=hashes();results=[]
def run(mode,cp,sources,expected):
 d=o/mode;d.mkdir();test=d/'classes';test.mkdir()
 with (d/'compile.log').open('w') as f:subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-sourcepath',str(d/'empty'),'-cp',cp,'-d',str(test),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
 with (d/'test.log').open('w') as f:subprocess.run([str(jdk/'java'),'-cp',str(test)+':'+cp,'com.ethk3.novaonepro.training_continuation',expected,str(d/'result.json')],stdout=f,stderr=subprocess.STDOUT,check=True)
 if mode=='normal':assert not any((classes/v.relative_to(test)).exists() for v in test.rglob('*.class'))
 results.append({'mode':mode,'scope':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if mode=='normal' else 'ISOLATED_GENERATED_INPUT_MUTATION_ACTUAL_PRODUCT_SOURCE','result':json.loads((d/'result.json').read_text())})
if not a.mutations_only:run('normal',str(classes),[fixture],'normal')
for mode,values,error in [('too_many',[f'{i:064x}' for i in range(12)],'R976_TOO_MANY_PREDECESSORS'),('duplicate',['0'*64]*2,'R976_INVALID_GENERATED_PREDECESSOR'),('malformed',['invalid'],'R976_INVALID_GENERATED_PREDECESSOR')]:
 d=o/(mode+'-input');d.mkdir()
 generated=d/'ResearchSemanticsGeneratedR967.java'
 generated.write_text('package com.ethk3.novaonepro; public final class ResearchSemanticsGeneratedR967 { public static final String COMPATIBLE_PREDECESSOR_RULES_SHA256='+json.dumps(','.join(values))+'; public static final String TRAINING_RULE_SHA256="'+'e'*64+'"; public static final String CONTINUATION_PROOF_SHA256="TEST_MUTATION_ONLY"; }\n')
 run(mode,str(classes),[generated,r/'app/src/main/java/com/ethk3/novaonepro/TrainingCodeIdentityR964.java',r/'app/src/main/java/com/ethk3/novaonepro/TrainingContinuationR973.java',fixture],error)
after=hashes();assert before==after
(o/'summary.json').write_text(json.dumps({'status':'PASS','results':results,'unchanged':True,'before':before,'after':after},indent=2)+'\n')
print('PASS continuation',len(results),'groups')
