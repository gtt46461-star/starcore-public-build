#!/usr/bin/env python3
"""Actual capture -> public maintenance settlement -> actual calibration fit, 120 + fresh JVM + 121."""
from pathlib import Path
import argparse,hashlib,json,shutil,sqlite3,subprocess
p=argparse.ArgumentParser()
for x in ('jdk','schema','out'):p.add_argument('--'+x,type=Path,required=True)
p.add_argument('--product-classes',type=Path,help='Final bytecode mode: test classes only')
p.add_argument('--adapters',type=Path,help='Host adapters only; must not contain product bytecode')
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True);classes=o/'classes';classes.mkdir()
java=a.jdk.resolve()/'bin/java';parts=(a.schema/'host-classpath').read_text().strip().split(':')
if a.adapters:parts[0]=str(a.adapters.resolve())
if a.product_classes:parts[1]=str(a.product_classes.resolve())
cp=':'.join([str(classes),*parts]);ast=o/'ast.txt';bridge=r/'verification/canonical/schema/sqlite_bridge.py'
sources=[r/'verification/r75/MainlineCaptureR75.java',r/'verification/r982/TrainingFixtureCP11.java',r/'verification/r77/MainlineAstR77.java',r/'verification/r77/MainlineMaintenanceR77.java']
sources.append(r/'verification/canonical/adapters/android/os/Debug.java')
sources.append(r/'verification/canonical/adapters/android/os/SystemClock.java')
if not a.product_classes:sources += [r/('app/src/main/java/com/ethk3/novaonepro/'+x+'.java') for x in ['FixedResearchWindowR932','CandidateCalibrationR932','FrozenCandidateAdapterR958','ResearchPredrawCaptureR923','DrawDependencyInputCP12']]
with (o/'compile.log').open('w') as f:subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(classes),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
subprocess.run([str(java),'-cp',cp,'com.ethk3.novaonepro.r952.MainlineAstR77',str(r/'app/src/main/assets/cp4_atom_registry.jsonl'),str(ast)],check=True,stdout=subprocess.DEVNULL)
db=o/'capture.db';shutil.copy2(a.schema/'schema-complete.db',db)
required={x['atomId'] for x in json.loads(ast.read_text()[5:])['nodes'] if x.get('atomId')}
with sqlite3.connect(db) as c:
    for line in (r/'app/src/main/assets/cp4_atom_registry.jsonl').open():
        x=json.loads(line)
        if x['atomId'] not in required:continue
        s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
        c.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
for mode in ['seed','resume']:
    log=o/(mode+'.log')
    with log.open('w') as f:result=subprocess.run([str(java),'-Xmx384m','-Dstarcore.test.ast='+str(ast),'-cp',cp,'com.ethk3.novaonepro.MainlineMaintenanceR77',str(bridge),str(db),mode,str(o/(mode+'.json'))],stdout=f,stderr=subprocess.STDOUT,timeout=600)
    print(mode,result.returncode,flush=True)
    if result.returncode:print(log.read_text()[-5000:]);raise SystemExit(result.returncode)
summary={'status':'PASS','checks':sum(json.loads((o/(m+'.json')).read_text())['checks'] for m in ['seed','resume']),'runs':{m:json.loads((o/(m+'.json')).read_text()) for m in ['seed','resume']},'scope':'241_PROSPECTIVE_SYNTHETIC_HOST_INPUTS_PRODUCT_CAPTURE_PUBLIC_MAINTENANCE_NOT_PHONE_QUALITY','fixture_sha256':hashlib.sha256(db.read_bytes()).hexdigest(),'final_product_classes':str(a.product_classes.resolve()) if a.product_classes else None,'source_sha256':{str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources}}
products=['FixedResearchWindowR932','CandidateCalibrationR932','FrozenCandidateAdapterR958','GlobalResearchScheduleR932','DrawDependencyInputCP12']
summary['product_class_sha256']={}
for name in products:
    rel=Path('com/ethk3/novaonepro')/(name+'.class');path=classes/rel
    if not path.exists():path=Path(parts[1])/rel
    summary['product_class_sha256'][name]=hashlib.sha256(path.read_bytes()).hexdigest()
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n');print(json.dumps(summary['runs'],indent=2))
