"""Compile the new read-only projection; reconstruct UI metadata, never numerical evidence."""
from pathlib import Path
import argparse,hashlib,json,re,shutil,sqlite3,subprocess
p=argparse.ArgumentParser()
for k in ('jdk','schema','classes','report','repair','out'):p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--final-classes',action='store_true',help='Compile tests only, use locked product helper bytecode')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
source=root/'app/src/main/java/com/ethk3/novaonepro/ResearchPipelineStatusR76.java'
scope=json.loads(a.report.read_text());entries={x['table']:x for x in scope['recentEvidence']}
tables=['draws','app_state','r952_candidate_predraw','r952_research_outbox','r952_artifact','r952_matrix_feedback','research_training_protocol_v2','v57_research_task_r967','research_training_feature_cursor_cp12','research_task_checkpoint_v2','research_development_receipt_v2','r967_frozen_evaluation_v2','research_action_v2','research_finding_v2','research_atom_catalog_import_cp12','research_molecule_import_cp12','research_matrix_spec_cp12','research_matrix_checkpoint_v2','research_atom_binding_v2','research_atom_admission_cp12','research_capture_reservation_v2','research_candidate_cal_study_r932','research_candidate_cal_model_r932','research_calibration_quality_cp12','research_certificate_r929','r975_calibration_failure','v57_research_mainline_contribution_r922','v57_research_artifact_r811']
fixture=out/'report.db';fixture.unlink(missing_ok=True)
schema=sqlite3.connect('file:'+str((a.schema/'schema-complete.db').resolve())+'?mode=ro',uri=True)
db=sqlite3.connect(fixture);counts={}
for table in tables:
 row=schema.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",(table,)).fetchone()
 if not row:raise RuntimeError('Missing schema '+table)
 db.execute(row[0])
 for index,sql in schema.execute("SELECT name,sql FROM sqlite_master WHERE type='index' AND tbl_name=? AND sql IS NOT NULL",(table,)):db.execute(sql)
 rows=entries.get(table,{}).get('rows',[])
 if rows:
  columns=[r[1] for r in db.execute('PRAGMA table_info('+table+')')];selected=[c for c in columns if c in rows[0]]
  sql='INSERT INTO '+table+'('+','.join(selected)+') VALUES('+','.join('?' for _ in selected)+')'
  def value(row,key):
   v=row.get(key)
   # UI never reads these bytes. The report supplies lengths, not payload contents.
   match=re.fullmatch(r'\[BINARY_PAYLOAD_NOT_INCLUDED bytes=(\d+)\]',v or '')
   return bytes(int(match.group(1))) if match else v
  db.executemany(sql,[[value(r,k) for k in selected] for r in rows])
 counts[table]=len(rows)
for s in scope['researchSchedulerState']:
 if s.get('status')=='READ_OK':db.execute('INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)',(s['key'],s.get('value'),s.get('updatedAt',0)))
db.commit();db.close();schema.close()
compiled=out/'classes';compiled.mkdir(exist_ok=True);orig=(a.schema/'host-classpath').read_text().strip().split(':')
if a.final_classes:
 for stale in (compiled/'com/ethk3/novaonepro').glob('ResearchPipelineStatusR76*.class'):stale.unlink()
 if not (a.classes/'com/ethk3/novaonepro/ResearchPipelineStatusR76.class').is_file():raise RuntimeError('Locked final helper bytecode is missing')
cp=':'.join(map(str,[compiled,a.schema/'classes',a.classes,orig[-1]]))
cmd=[str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(compiled)]+([] if a.final_classes else [str(source)])+[str(root/'verification/r76/pipeline_status_R76.java'),str(root/'verification/r72/realtime/android/os/SystemClock.java')]
with (out/'compile.log').open('w') as f:subprocess.run(cmd,check=True,stdout=f,stderr=subprocess.STDOUT)
product_origin=a.classes if a.final_classes else compiled
product_hashes={str(f.relative_to(product_origin)):hashlib.sha256(f.read_bytes()).hexdigest() for f in (product_origin/'com/ethk3/novaonepro').glob('ResearchPipelineStatusR76*.class')}
modes=[('report',fixture),('repair',a.repair)]
for mode in ['empty','failure','false-complete','online','cap']:
 dest=out/(mode+'.db');dest.unlink(missing_ok=True)
 with sqlite3.connect(fixture) as src,sqlite3.connect(dest) as dst:src.backup(dst)
 with sqlite3.connect(dest) as db:
  if mode=='empty':
   for table in tables:db.execute('DELETE FROM '+table)
  elif mode=='failure':db.execute('DROP TABLE research_training_feature_cursor_cp12')
  elif mode=='false-complete':db.execute("UPDATE v57_research_task_r967 SET status='COMPLETED' WHERE kind='TRAIN_CANDIDATE'")
  elif mode=='online':db.execute("INSERT INTO v57_research_artifact_r811(artifact_id,artifact_type,trained_until_issue,feature_schema_hash,artifact_blob,artifact_hash,status,created_at) VALUES(?,?,?,?,?,?,?,?)",('UI_SYNTHETIC_FTRL','FTRL_SHADOW','202610130483','a'*64,b'UI_FIXTURE_NOT_NUMERIC','b'*64,'RESEARCH_ONLY_SHADOW_LOADABLE_B09_ATOMIC',1))
  elif mode=='cap':
   cols=[r[1] for r in db.execute('PRAGMA table_info(research_action_v2)')];row=dict(zip(cols,db.execute('SELECT '+','.join(cols)+' FROM research_action_v2 LIMIT 1').fetchone()))
   for i in range(97):
    row.update(action_id='UI_CAP_'+str(i),seed=100000+i,state='PROPOSED',child_id=None)
    db.execute('INSERT INTO research_action_v2('+','.join(cols)+') VALUES('+','.join('?' for _ in cols)+')',[row[k] for k in cols])
 modes.append((mode,dest))
for mode,path in modes:
 if mode=='repair':
  dest=out/'repair.db'
  with sqlite3.connect('file:'+str(path.resolve())+'?mode=ro',uri=True) as src,sqlite3.connect(dest) as dst:src.backup(dst)
  path=dest
 before=hashlib.sha256(path.read_bytes()).hexdigest()
 cmd=[str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.pipeline_status_R76',str(root/'verification/canonical/schema/sqlite_bridge.py'),str(path),mode,str(out/mode)]
 with (out/(mode+'.log')).open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 if r.returncode:print((out/(mode+'.log')).read_text()[-6000:]);raise SystemExit(r.returncode)
 if hashlib.sha256(path.read_bytes()).hexdigest()!=before:raise RuntimeError('Projection changed '+mode+' database')
 print(mode,json.loads((out/(mode+'.json')).read_text())['checks'])
manifest={'status':'PASS','scope':'READ_ONLY_UI_PROJECTION_NOT_OWNER_TRAINING_OR_QUALIFICATION','productionSourceCompiled':not a.final_classes,'reportMetadataRows':counts,'reportMetadataOmissions':['Binary payloads have zero-byte length placeholders; helper never reads them and they cannot support numerical claims','FTRL artifact rows are not present in report recentEvidence; that table is empty in this UI fixture, not claimed empty on phone','Mainline contributions absent in metadata fixture'],'controlledModes':['empty','failure','false-complete','online synthetic UI metadata only','cap'],'sourceSha256':hashlib.sha256(source.read_bytes()).hexdigest(),'fixtureSha256':hashlib.sha256(a.report.read_bytes()).hexdigest()}
if any(hashlib.sha256((product_origin/name).read_bytes()).hexdigest()!=h for name,h in product_hashes.items()):raise RuntimeError('Helper bytecode changed during verification')
manifest.update(productClasses=str(product_origin.resolve()),productClassSha256=product_hashes,productClassesUnchanged=True,results={mode:json.loads((out/(mode+'.json')).read_text())['checks'] for mode,_ in modes})
(out/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
