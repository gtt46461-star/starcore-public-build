#!/usr/bin/env python3
"""R79 original report DEV replay + P0/rollback recovery using sealed product classes.
No phone DB is changed. Missing feature payloads are recomputed from original CAS.
"""
from pathlib import Path
import json,sqlite3,shutil,subprocess,hashlib,sys,os
R=Path(__file__).resolve().parents[3];W=R.parent
O=Path(sys.argv[1]).resolve();O.mkdir(exist_ok=False)
config=Path(os.environ.get('STARCORE_HOST_CONFIG',str(W/'audit_work/host/config.json')))
c=json.loads(config.read_text());report=W/'r79_inputs/report/DIAGNOSTIC_SCOPE.json'
P=Path(c['product']);hashes={str(p.relative_to(P)):hashlib.sha256(p.read_bytes()).hexdigest()for p in P.rglob('*.class')}
(O/'product_hashes.json').write_text(json.dumps(hashes,indent=2))
(O/'source_hashes.json').write_text(json.dumps({str(p.relative_to(R)):hashlib.sha256(p.read_bytes()).hexdigest()for p in [R/'app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java',Path(__file__)]},indent=2))
data=json.loads(report.read_text());e={x['table']:x['rows']for x in data['recentEvidence']}
tasks=[x.copy()for x in e['v57_research_task_r967']if x['kind']=='TRAIN_CANDIDATE'and x['status']=='YIELDED'];ids={x['candidate_id']for x in tasks}
protocols=[x for x in e['research_training_protocol_v2']if x['candidate_id']in ids];inputs={x['input_hash']for x in protocols}
selected={'draws':e['draws'],'research_atom_spec_v2':e['research_atom_spec_v2'],'r952_candidate':e['r952_candidate'],'research_training_protocol_v2':protocols,'research_training_input_v2':[x for x in e['research_training_input_v2']if x['input_hash']in inputs]}
for x in tasks:x.update(cursor='',status='PENDING',lease_owner='',lease_until=0,available_at=0,last_error='TEST_RECOMPUTE_FEATURES_RAW_CAS_ONLY')
selected['v57_research_task_r967']=tasks
seed=O/'seed.db';shutil.copy2(Path(os.environ.get('STARCORE_HOST_SCHEMA',str(W/'audit_work/host-schema.db'))),seed)
with sqlite3.connect(seed)as d:
 triggers=d.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger'").fetchall()
 for name,_ in triggers:d.execute('DROP TRIGGER "'+name+'"')
 for table,rows in selected.items():
  if not rows:continue
  keys=list(rows[0]);d.executemany('INSERT INTO "'+table+'"('+','.join('"'+k+'"'for k in keys)+')VALUES('+','.join('?'for _ in keys)+')',[[x.get(k)for k in keys]for x in rows])
 for _,sql in triggers:d.execute(sql)
 assert d.execute('pragma quick_check').fetchone()==('ok',)
(O/'fixture-receipt.json').write_text(json.dumps({'scope':'ISOLATED_REPORT_RAW_INPUT_RECONSTRUCTION_NOT_PHONE_CHECKPOINT','tasks':len(tasks),'inputs':len(inputs),'omitted_partial_feature_payloads_recomputed':True,'report_sha256':hashlib.sha256(report.read_bytes()).hexdigest()},indent=2))
del data,e,selected
T=O/'tests';T.mkdir();cp=str(T)+':'+c['classpath']
sources=[R/'verification/report1015/TrainingReplayR78.java',R/'verification/r77/training_PreparedPageR77.java']
cmd=[c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',str(T),*map(str,sources)]
(O/'compile-command.json').write_text(json.dumps(cmd,indent=2))
with (O/'compile.log').open('w')as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
assert not(set(hashes)&{str(p.relative_to(T))for p in T.rglob('*.class')}),'PRODUCT_OVERLAY'
db=O/'replay.db';shutil.copy2(seed,db)
cmd=[c['java'],'-Xmx'+os.environ.get('STARCORE_TEST_HEAP','384m'),'-cp',cp,'com.ethk3.novaonepro.TrainingReplayR78',c['bridge'],str(db),str(O/'app'),str(R/'app/src/main/assets'),'ALLOW_DOCUMENTED_FEATURE_REJECTION']
(O/'command.json').write_text(json.dumps(cmd,indent=2))
with (O/'run.log').open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=900)
result={'exit_code':z.returncode,'scope':'ACTUAL_RELEASE_TRAIN_OWNER_ORIGINAL_CAS_HOST_REPLAY_NOT_PHONE_TIMING'}
with sqlite3.connect(db)as d:
 result['tasks']=d.execute("SELECT status,count(*)FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' GROUP BY status").fetchall();result['features']=d.execute('SELECT count(*)FROM research_training_feature_row_cp11').fetchone()[0];result['receipts']=d.execute('SELECT count(*)FROM research_development_receipt_v2').fetchone()[0];result['terminal_reasons']=d.execute("SELECT last_error,count(*)FROM v57_research_task_r967 WHERE status='FAILED_TERMINAL' GROUP BY last_error").fetchall()
(O/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result),flush=True)
if z.returncode:raise SystemExit('SEE_REPLAY_LOG')
for mode in ['rollback','p0']:
 testdb=O/(mode+'.db');shutil.copy2(seed,testdb)
 cmd=[c['java'],'-Xmx'+os.environ.get('STARCORE_TEST_HEAP','384m'),'-cp',cp,'com.ethk3.novaonepro.training_PreparedPageR77',c['bridge'],str(testdb),str(O/'app'),str(R/'app/src/main/assets')]+(['p0']if mode=='p0'else[])
 (O/(mode+'-command.json')).write_text(json.dumps(cmd,indent=2))
 with (O/(mode+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 result[mode]={'exit_code':z.returncode};(O/'result.json').write_text(json.dumps(result,indent=2));print(mode,z.returncode,flush=True)
 if z.returncode:raise SystemExit('SEE_'+mode+'_LOG')
assert all(hashlib.sha256((P/name).read_bytes()).hexdigest()==h for name,h in hashes.items()),'PRODUCT_CHANGED_DURING_RUN'
assert result['features']==19800 and result['receipts']==14
print('PASS_R79_ORIGINAL_TRAINING_AND_RECOVERY',flush=True)
