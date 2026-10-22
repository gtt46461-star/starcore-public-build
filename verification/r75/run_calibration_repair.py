#!/usr/bin/env python3
"""Real failed-fit repair through product feedback/EVOLVE/Director, isolated host DB.

Pass --fixture the completed capture.db from run_mainline_capture.py; no quality,
action, child, protocol, model, Future, certificate or REAL state is fabricated.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3, subprocess, time

p=argparse.ArgumentParser()
for name in ('jdk','schema','fixture','out'): p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--baseline-classes',type=Path,help='Optional saved R74 classes overriding current classes for formal-only reproduction')
p.add_argument('--native',type=Path,help='Existing host JNI build; defaults to schema parent / host-native')
p.add_argument('--product-classes',type=Path,help='Validate final compiled product bytecode; compile test sources only')
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True)
classes=o/'classes';classes.mkdir();java=a.jdk.resolve()/'bin/java';original=(a.schema/'host-classpath').read_text().strip();cp=str(classes)+':'+original
if a.product_classes:
    parts=original.split(':');cp=':'.join([str(classes),parts[0],str(a.product_classes.resolve()),*parts[2:]])
products=['CalibrationRepairR75.java','ResearchFeedbackCP12.java','r952/EvolutionControllerR952.java','r952/DevelopmentProductionCP12.java','r952/ResearchDirectorR952.java','r952/MutationGraphCP12.java']
sources=[r/'app/src/main/java/com/ethk3/novaonepro'/name for name in products]+[r/'verification/r75/CalibrationRepairR75Test.java']
compile_sources=[r/'verification/r75/CalibrationRepairR75Test.java'] if a.product_classes else sources
with (o/'compile.log').open('w') as f: subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),*map(str,compile_sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
receipts=[]
def run(db,mode,classpath=cp):
    log=o/(mode+'.log');result_file=o/(mode+'.json')
    native=a.native.resolve() if a.native else a.schema.resolve().parent/'host-native'
    command=[str(java),'-Xmx384m','-Djava.library.path='+str(native),'-cp',classpath,'com.ethk3.novaonepro.CalibrationRepairR75Test',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),mode,str(o/('app-'+db.stem)),str(r/'app/src/main/assets'),str(result_file)]
    with log.open('w') as f: result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,timeout=180)
    receipts.append(dict(mode=mode,exit_code=result.returncode,log_sha256=hashlib.sha256(log.read_bytes()).hexdigest()))
    (o/'results.json').write_text(json.dumps(receipts,indent=2)+'\n');print(mode,result.returncode,flush=True)
    if result.returncode:print(log.read_text()[-6000:]);raise SystemExit(1)

if a.baseline_classes:
    baseline=o/'baseline.db';shutil.copy2(a.fixture,baseline)
    run(baseline,'baseline',str(a.baseline_classes.resolve())+':'+cp)
db=o/'repair.db';shutil.copy2(a.fixture,db)
# The product atom registry is an input catalogue. Import real registry definitions
# for the parent's position, so typed window/hyperparameter pairs can be selected.
with sqlite3.connect(db) as c:
    parent=json.loads(c.execute('SELECT ast FROM r952_candidate LIMIT 1').fetchone()[0][5:]);position=parent['position']
    for line in (r/'app/src/main/assets/cp4_atom_registry.jsonl').open():
        atom=json.loads(line);s=atom['spec']
        if s['position']!=position:continue
        wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        c.execute('INSERT OR IGNORE INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[atom['atomId'],s['family'],s['version'],wire,s['position'],atom['outputType'],atom['minimumHistory'],atom['specHash'],atom['missingPolicy'],atom['state']])
budget=o/'budget.db';shutil.copy2(db,budget);run(budget,'budget')
run(db,'enqueue')
for mode in ('parent_changed','invalid_cutoff','busy'):
    broken=o/(mode+'.db');shutil.copy2(db,broken);run(broken,mode)
    if mode=='busy':time.sleep(1.1);run(broken,'busy_resume')
run(db,'resume')
run(db,'archive')
summary={'status':'PASS','fixture_sha256':hashlib.sha256(a.fixture.read_bytes()).hexdigest(),'checks':sum(json.loads((o/(x['mode']+'.json')).read_text())['checks'] for x in receipts),'modes':receipts,'sources':{str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources+[Path(__file__).resolve()]},'baseline_classes':{str(s.relative_to(a.baseline_classes)):hashlib.sha256(s.read_bytes()).hexdigest() for s in a.baseline_classes.rglob('*.class')} if a.baseline_classes else {},'limitation':'Synthetic host inputs; repair creates a training candidate, not proof of improved future quality or REAL authority.'}
summary['final_product_classes']=str(a.product_classes.resolve()) if a.product_classes else None
summary['product_class_sha256']={name:hashlib.sha256((a.product_classes/'com/ethk3/novaonepro'/name.replace('.java','.class')).read_bytes()).hexdigest() for name in products} if a.product_classes else {}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
