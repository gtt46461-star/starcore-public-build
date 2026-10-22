#!/usr/bin/env python3
"""R75 public capture/calibration owners, isolated synthetic SQLite fixture only."""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3, subprocess
p=argparse.ArgumentParser()
for x in ('jdk','schema','out'):p.add_argument('--'+x,type=Path,required=True)
p.add_argument('--reproduce-r74',action='store_true',help='Requires unchanged R74 product classes; normal runs validate the current code only')
p.add_argument('--product-classes',type=Path,help='Validate final compiled product bytecode; compile test sources only')
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True)
classes=o/'classes';classes.mkdir(exist_ok=True);java=a.jdk.resolve()/'bin/java'
original=(a.schema/'host-classpath').read_text().strip();cp=str(classes)+':'+original
if a.product_classes:
    if a.reproduce_r74:p.error('--product-classes cannot be combined with --reproduce-r74')
    parts=original.split(':');cp=':'.join([str(classes),parts[0],str(a.product_classes.resolve()),*parts[2:]])
ast=o/'ast.txt';bridge=r/'verification/canonical/schema/sqlite_bridge.py'
sources=[r/'verification/r74/CaptureAdmissionR74.java',r/'verification/r75/MainlineCapacityR75.java',r/'verification/r75/MainlineCaptureR75.java',r/'verification/r982/TrainingFixtureCP11.java',r/'verification/r75/MainlineAstR75.java',r/'verification/r997/TestAstR997.java',r/'verification/r75/MainlineIsolationR75.java']
with (o/'compile.log').open('w') as f:subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
receipts=[]
def run(name,db,mode='',expected=0):
    args=[str(bridge),str(db)]+([mode,str(o/(name+'-'+mode+'.json'))] if mode else [str(o/(name+'.json'))])
    if name=='MainlineIsolationR75':args.append(str(o/'negative-ast.txt'))
    command=[str(java),'-Xmx384m','-Dstarcore.test.ast='+str(ast),'-cp',cp,'com.ethk3.novaonepro.'+name,*args]
    log=o/(name+('-'+mode if mode else '')+('-baseline' if expected else '')+'.log')
    with log.open('w') as f:result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT,timeout=600)
    receipts.append(dict(test=name,mode=mode,exit_code=result.returncode,expected_failure=bool(expected),log_sha256=hashlib.sha256(log.read_bytes()).hexdigest()))
    (o/'results.json').write_text(json.dumps(receipts,indent=2)+'\n');print(log.name,result.returncode,flush=True)
    if bool(result.returncode)!=bool(expected):print(log.read_text()[-5000:]);raise SystemExit(1)
if a.reproduce_r74:
    db=o/'capacity-baseline.db';shutil.copy2(a.schema/'schema-complete.db',db);run('MainlineCapacityR75',db,expected=1)
if not a.product_classes:
    with (o/'compile-product.log').open('w') as f:subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),str(r/'app/src/main/java/com/ethk3/novaonepro/GlobalResearchScheduleR932.java'),str(r/'app/src/main/java/com/ethk3/novaonepro/FrozenCandidateAdapterR958.java')],stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'capacity.db';shutil.copy2(a.schema/'schema-complete.db',db);run('MainlineCapacityR75',db)
subprocess.run([str(java),'-cp',cp,'com.ethk3.novaonepro.r952.MainlineAstR75',str(r/'app/src/main/assets/cp4_atom_registry.jsonl'),str(ast)],check=True,stdout=subprocess.DEVNULL)
subprocess.run([str(java),'-cp',cp,'com.ethk3.novaonepro.r952.TestAstR997',str(r/'app/src/main/assets/cp4_atom_registry.jsonl'),str(o/'negative-ast.txt')],check=True,stdout=subprocess.DEVNULL)
db=o/'capture.db';shutil.copy2(a.schema/'schema-complete.db',db)
root=json.loads(ast.read_text()[5:]);required={x['atomId'] for x in root['nodes'] if x.get('atomId')}
with sqlite3.connect(db) as c:
    for line in (r/'app/src/main/assets/cp4_atom_registry.jsonl').open():
        x=json.loads(line)
        if x['atomId'] not in required:continue
        s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        c.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
run('MainlineCaptureR75',db,'seed');run('MainlineCaptureR75',db,'resume')
db=o/'isolation.db';shutil.copy2(a.schema/'schema-complete.db',db)
required=set()
for path in (ast,o/'negative-ast.txt'):
    root=json.loads(path.read_text()[5:]);required.update(x['atomId'] for x in root['nodes'] if x.get('atomId'))
with sqlite3.connect(db) as c:
    for line in (r/'app/src/main/assets/cp4_atom_registry.jsonl').open():
        x=json.loads(line)
        if x['atomId'] not in required:continue
        s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        c.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
run('MainlineIsolationR75',db)
if a.product_classes:
    products=['GlobalResearchScheduleR932','FrozenCandidateAdapterR958','CandidateCalibrationR932']
    summary={'status':'PASS','checks':sum(sum(line.startswith('PASS ') for line in path.read_text().splitlines()) for path in o.glob('Mainline*.log')),'final_product_classes':str(a.product_classes.resolve()),'product_class_sha256':{name:hashlib.sha256((a.product_classes/'com/ethk3/novaonepro'/(name+'.class')).read_bytes()).hexdigest() for name in products},'fixture_sha256':hashlib.sha256((o/'capture.db').read_bytes()).hexdigest(),'scope':'ACTUAL_PRODUCT_BYTECODE_241_PROSPECTIVE_SYNTHETIC_HOST_OBSERVATIONS_NO_LIVE_QUALITY'}
    (o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
