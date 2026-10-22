"""Restore reported raw inputs in isolated SQLite, with actual installed schema.
Fixture import temporarily removes/reinstalls triggers. No user DB is modified.
Incomplete feature payloads are absent from the report and explicitly recomputed.
"""
from pathlib import Path
import json,sqlite3,shutil,subprocess,hashlib,os
R=Path(__file__).resolve().parents[2];W=R.parent;O=W/'audit_work'/os.environ.get('STARCORE_TRAIN_OUT','training-replay');O.mkdir(exist_ok=False)
c=json.loads((W/'audit_work/host/config.json').read_text());db=O/'replay.db';shutil.copy2(W/'audit_work/host-schema.db',db)
data=json.loads((W/'report_full/DIAGNOSTIC_SCOPE.json').read_text());e={x['table']:x['rows']for x in data['recentEvidence']}
tasks=[x.copy()for x in e['v57_research_task_r967']if x['kind']=='TRAIN_CANDIDATE'and x['status']=='YIELDED'];ids={x['candidate_id']for x in tasks}
protocols=[x for x in e['research_training_protocol_v2']if x['candidate_id']in ids];inputs={x['input_hash']for x in protocols}
selected={'draws':e['draws'],'research_atom_spec_v2':e['research_atom_spec_v2'],'r952_candidate':e['r952_candidate'],'research_training_protocol_v2':protocols,'research_training_input_v2':[x for x in e['research_training_input_v2']if x['input_hash']in inputs]}
for x in tasks:x.update(cursor='',status='PENDING',lease_owner='',lease_until=0,available_at=0,last_error='TEST_RECOMPUTE_FEATURES_RAW_CAS_ONLY')
selected['v57_research_task_r967']=tasks
with sqlite3.connect(db)as d:
 triggers=d.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger'").fetchall()
 for name,_ in triggers:d.execute('DROP TRIGGER "'+name+'"')
 for table,rows in selected.items():
  if not rows:continue
  keys=list(rows[0]);d.executemany('INSERT INTO "'+table+'"('+','.join('"'+k+'"'for k in keys)+')VALUES('+','.join('?'for _ in keys)+')',[[x.get(k)for k in keys]for x in rows])
 for _,sql in triggers:d.execute(sql)
 assert d.execute('pragma quick_check').fetchone()==('ok',)
seed=O/'fixture-receipt.json';seed.write_text(json.dumps({'scope':'ISOLATED_REPORT_RAW_INPUT_RECONSTRUCTION_NOT_PHONE_CHECKPOINT','tasks':len(tasks),'inputs':len(inputs),'omitted_partial_feature_payloads_recomputed':True,'report_sha256':hashlib.sha256((W/'report_full/DIAGNOSTIC_SCOPE.json').read_bytes()).hexdigest()},indent=2))
with (O/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',c['tests'],str(R/'verification/report1015/TrainingReplayR78.java')],stdout=f,stderr=subprocess.STDOUT,check=True)
cmd=[c['java'],'-Xmx384m','-cp',c['classpath'],'com.ethk3.novaonepro.TrainingReplayR78',c['bridge'],str(db),str(O/'app'),str(R/'app/src/main/assets')]
if os.environ.get('STARCORE_TRAIN_ALLOW_FEATURE_REJECTION')=='1':cmd.append('ALLOW_DOCUMENTED_FEATURE_REJECTION')
(O/'command.json').write_text(json.dumps(cmd,indent=2))
with (O/'run.log').open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=900)
result={'exit_code':z.returncode,'scope':'ACTUAL_TRAIN_OWNER_ORIGINAL_CAS_HOST_REPLAY_NOT_PHONE_TIMING'}
with sqlite3.connect(db)as d:
 result['tasks']=d.execute("SELECT status,count(*)FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' GROUP BY status").fetchall()
 result['features']=d.execute('SELECT count(*)FROM research_training_feature_row_cp11').fetchone()[0]
 result['receipts']=d.execute('SELECT count(*)FROM research_development_receipt_v2').fetchone()[0]
 result['terminal_reasons']=d.execute("SELECT last_error,count(*)FROM v57_research_task_r967 WHERE status='FAILED_TERMINAL' GROUP BY last_error").fetchall()
(O/'result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result));assert z.returncode==0,'SEE_REPLAY_LOG'
