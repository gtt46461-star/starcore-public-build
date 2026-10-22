#!/usr/bin/env python3
"""Actual reserved owner time fences, with optional development-source overlay."""
import argparse,json,subprocess,hashlib,shutil,sqlite3
from pathlib import Path
p=argparse.ArgumentParser()
for k in ['jdk','classes','adapters','android-runtime','schema','out']:p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--overlay',action='store_true');a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=False);c=o/'classes';c.mkdir()
cp=':'.join(map(str,[c,a.adapters,a.classes,a.android_runtime]));sources=[r/('verification/'+x) for x in ['r77/CaptureDeadlineR77.java','r75/MainlineCaptureR75.java','r77/MainlineAstR77.java','r982/TrainingFixtureCP11.java']]
sources.append(r/'verification/canonical/adapters/android/os/Debug.java')
sources.append(r/'verification/canonical/adapters/android/os/SystemClock.java')
if a.overlay:sources += [r/('app/src/main/java/com/ethk3/novaonepro/'+x+'.java') for x in ['CandidateCalibrationR932','FrozenCandidateAdapterR958','ResearchPredrawCaptureR923','DrawDependencyInputCP12']]
with (o/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(c),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
ast=o/'ast.txt';subprocess.run([str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.r952.MainlineAstR77',str(r/'app/src/main/assets/cp4_atom_registry.jsonl'),str(ast)],check=True,stdout=subprocess.DEVNULL)
required={x['atomId'] for x in json.loads(ast.read_text()[5:])['nodes'] if x.get('atomId')};results={}
for mode in ['timely','expired','crossing']:
 db=o/(mode+'.db');shutil.copy2(a.schema,db)
 with sqlite3.connect(db) as conn:
  for line in (r/'app/src/main/assets/cp4_atom_registry.jsonl').open():
   x=json.loads(line)
   if x['atomId'] not in required:continue
   s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
   conn.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
 log=o/(mode+'.log')
 with log.open('w') as f:z=subprocess.run([str(a.jdk/'bin/java'),'-Xmx384m','-Dstarcore.test.ast='+str(ast),'-cp',cp,'com.ethk3.novaonepro.CaptureDeadlineR77',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),mode,str(o/(mode+'.json'))],stdout=f,stderr=subprocess.STDOUT,timeout=120)
 assert z.returncode==0,log.read_text();results[mode]=json.loads((o/(mode+'.json')).read_text());print(mode,results[mode],flush=True)
summary={'status':'PASS','checks':sum(v['checks'] for v in results.values()),'runs':results,'implementation':'development-source-overlay' if a.overlay else 'supplied-product-bytecode','sources':{str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'product_classes':str(a.classes)}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
