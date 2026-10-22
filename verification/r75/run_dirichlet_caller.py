#!/usr/bin/env python3
"""Run the canonical actual ResearchFeedback Dirichlet caller on isolated SQLite.
Only the two test classes are compiled; production comes from final bytecode.
"""
from pathlib import Path
import argparse,hashlib,json,shutil,subprocess
p=argparse.ArgumentParser()
for name in ('jdk','schema','product-classes','out'):p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True)
classes=o/'classes';classes.mkdir();java=a.jdk.resolve()/'bin/java'
parts=(a.schema/'host-classpath').read_text().strip().split(':')
cp=':'.join([str(classes),parts[0],str(a.product_classes.resolve()),*parts[2:]])
source=r/'verification/canonical/tests/audit_cp12/runtime_research/tests'
tests=[source/'MutationOperatorsCP12Test.java',source/'ResearchDirichletCallerCP12Test.java']
with (o/'compile.log').open('w') as f:subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),*map(str,tests)],check=True,stdout=f,stderr=subprocess.STDOUT)
db=o/'fixture.db';shutil.copy2(a.schema/'schema-complete.db',db)
command=[str(java),'-Xmx384m','-cp',cp,'com.ethk3.novaonepro.r952.ResearchDirichletCallerCP12Test',str(r/'app/src/main/assets/cp4_atom_registry.jsonl'),str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db)]
log=o/'execution.log'
with log.open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,timeout=120)
print(log.read_text(),end='')
if result.returncode:raise SystemExit(result.returncode)
receipt=json.loads(next(line for line in reversed(log.read_text().splitlines()) if line.startswith('{')))
receipt.update(final_product_classes=str(a.product_classes.resolve()),product_class_sha256=hashlib.sha256((a.product_classes/'com/ethk3/novaonepro/ResearchFeedbackCP12.class').read_bytes()).hexdigest(),sources={str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in tests},execution_log_sha256=hashlib.sha256(log.read_bytes()).hexdigest())
(o/'summary.json').write_text(json.dumps(receipt,indent=2)+'\n')
