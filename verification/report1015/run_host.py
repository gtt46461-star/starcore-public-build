"""Acceptance on sealed Gradle product classes and real SQLite transport."""
from pathlib import Path
import concurrent.futures,hashlib,json,shutil,sqlite3,subprocess,os
R=Path(__file__).resolve().parents[2];W=R.parent;O=W/'audit_work/host';c=json.loads((O/'config.json').read_text());out=W/'audit_work'/os.environ.get('STARCORE_ACCEPTANCE_OUT','acceptance');out.mkdir(exist_ok=False)
sources=[R/p for p in ['verification/r976/PostRetryHarnessR976.java','verification/report1015/PostBatchR78.java','verification/report1015/ReservedBatchR78.java','verification/r77/MainlineAstR77.java','verification/canonical/adapters/android/os/Debug.java','verification/canonical/adapters/android/util/Base64.java']]
with (out/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(out/'empty'),'-cp',c['classpath'],'-d',c['tests'],*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
ast=out/'fixture.ast';subprocess.run([c['java'],'-cp',c['classpath'],'com.ethk3.novaonepro.r952.MainlineAstR77',str(R/'app/src/main/assets/cp4_atom_registry.jsonl'),str(ast)],check=True)
required={x['atomId']for x in json.loads(ast.read_text()[5:])['nodes']if x.get('atomId')};seed=out/'seed.db';shutil.copy2(W/'audit_work/host-schema.db',seed)
with sqlite3.connect(seed)as d:
 for line in (R/'app/src/main/assets/cp4_atom_registry.jsonl').open():
  x=json.loads(line)
  if x['atomId']not in required:continue
  s=x['spec'];wire=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
  d.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state)VALUES(?,?,?,?,?,?,?,?,?,?)',[x['atomId'],s['family'],s['version'],wire,s['position'],x['outputType'],x['minimumHistory'],x['specHash'],x['missingPolicy'],x['state']])
def run(name,klass,mode=None):
 db=out/(name+'.db');shutil.copy2(seed,db)
 args=[c['bridge'],str(db)]
 if klass=='CaptureDeadlineR77':args +=[mode,str(out/(name+'.json'))]
 if klass=='training_DirectorFairnessR77':args +=[mode]
 cmd=[c['java'],'-Xmx384m','-Dstarcore.test.ast='+str(ast),'-cp',c['classpath'],'com.ethk3.novaonepro.'+klass]+args
 (out/(name+'-command.json')).write_text(json.dumps(cmd,indent=2))
 with (out/(name+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 result={'name':name,'exit_code':z.returncode,'log_sha256':hashlib.sha256((out/(name+'.log')).read_bytes()).hexdigest(),'status':'PASS'if z.returncode==0 else 'FAIL'}
 print(name,result['status'],flush=True);return result
jobs=[('post-batch','PostBatchR78',None),('reserved-batch','ReservedBatchR78',None),('post-repair','PostProjectionRecoveryR982',None),('draw-ingest','DrawIngestIdempotency',None),('fairness-low','training_DirectorFairnessR77','low_budget'),('capture-timely','CaptureDeadlineR77','timely'),('capture-expired','CaptureDeadlineR77','expired'),('capture-crossing','CaptureDeadlineR77','crossing')]
if os.environ.get('STARCORE_ACCEPTANCE_ONLY'):jobs=[x for x in jobs if x[0]in os.environ['STARCORE_ACCEPTANCE_ONLY'].split(',')]
with concurrent.futures.ThreadPoolExecutor(max_workers=3)as pool:results=list(pool.map(lambda t:run(*t),jobs))
expected=json.loads((O/'product_hashes.json').read_text());product=Path(c['product']);assert all(hashlib.sha256((product/p).read_bytes()).hexdigest()==h for p,h in expected.items()),'PRODUCT_CHANGED'
for root in [Path(c['tests']),Path(c['adapters'])]:assert not(set(expected)&{str(p.relative_to(root))for p in root.rglob('*.class')}),'PRODUCT_OVERLAY'
with sqlite3.connect(W/'audit_work/host-schema.db')as d:integrity={'quick_check':d.execute('pragma quick_check').fetchall(),'foreign_key_check':d.execute('pragma foreign_key_check').fetchall()}
(out/'summary.json').write_text(json.dumps({'scope':'ACTUAL_GRADLE_PRODUCT_HOST_SQLITE_NOT_PHONE_OR_FORECAST_QUALITY','product_classes':len(expected),'results':results,'schema_integrity':integrity},indent=2))
assert all(r['status']=='PASS'for r in results),'ACCEPTANCE_FAILURE_SEE_LOGS'
print('ACCEPTANCE_PASS',len(results),'groups')
