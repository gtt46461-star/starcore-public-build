#!/usr/bin/env python3
"""Checks design consistency only. It never grants app, device, or model acceptance."""
from pathlib import Path
import json,sqlite3,csv,sys
root=Path(__file__).resolve().parents[1]
def read(p):return json.loads((root/p).read_text(encoding='utf-8'))
tasks=read('02_施工台账/tasks.json');byid={t['id']:t for t in tasks};assert len(tasks)==len(byid)==42
visiting=set();done=set()
def visit(id):
 assert id in byid,id
 assert id not in visiting,'DEPENDENCY_CYCLE '+id
 if id in done:return
 visiting.add(id)
 for dep in byid[id]['dependencies']:visit(dep)
 visiting.remove(id);done.add(id)
for t in tasks:
 visit(t['id']);assert t['gap'] and len(t['steps'])>=4 and t['acceptance'] and t['file_details']
 assert all(f['state']=='EXISTING' and f['sha256'] and f['lines']>0 for f in t['file_details']),t['id']
 assert t['status']=='NOT_STARTED'
trace=read('02_施工台账/原要求到新任务追踪.json')
for r in trace:assert r['tasks'] and set(r['tasks'])<=set(byid)
assert {f'M{i}' for i in range(1,11)}<={r['requirement'] for r in trace}
file_rows=read('02_施工台账/原73条逐文件要求映射.json')
assert len(file_rows)==73
assert all(r['current_tasks'] and set(r['current_tasks'])<=set(byid) for r in file_rows)
oldtests=read('04_验收与执行/原全部验收映射.json')
for r in oldtests:assert r['mapped_tasks'] and set(r['mapped_tasks'])<=set(byid)
cases=read('04_验收与执行/完整验收用例.json');assert len({t['id'] for t in cases})==len(cases)
assert {t['task'] for t in cases}==set(byid)
assert all(t['status']=='NOT_RUN' for t in cases)
with (root/'07_源码定位/R925_559_Java基线审计索引.csv').open(encoding='utf-8-sig') as f:java=list(csv.DictReader(f))
assert len(java)==559
sql=(root/'03_合同与迁移设计/schema_r926_design.sql').read_text();db=sqlite3.connect(':memory:');db.executescript(sql);before=list(db.execute("SELECT type,name,sql FROM sqlite_master ORDER BY type,name"));db.executescript(sql);assert before==list(db.execute("SELECT type,name,sql FROM sqlite_master ORDER BY type,name"))
checks=[]
def rejected(name,sql,args=()):
 try:db.execute(sql,args)
 except sqlite3.DatabaseError:checks.append(name);return
 raise AssertionError('EXPECTED_REJECTION '+name)
db.execute("INSERT INTO r926_runtime_event VALUES ('e','Binance1-PK10','202609070001',577250926,'p','c','a','OWNER','FROZEN','OK',1,1,'h','{}',NULL,'key')")
rejected('IMMUTABLE_EVENT',"UPDATE r926_runtime_event SET result='FAIL' WHERE event_id='e'")
rejected('NO_DELETE_EVENT',"DELETE FROM r926_runtime_event WHERE event_id='e'")
rejected('UNIQUE_EVENT',"INSERT INTO r926_runtime_event SELECT * FROM r926_runtime_event")
db.execute("INSERT INTO r926_hypothesis VALUES ('h','f','algorithm','a','s','202609070000','b',0.001,'p',NULL)")
db.execute("INSERT INTO r926_window VALUES ('w','Binance1-PK10','h','candidate','champion','cal','selection','domain','202609070000','202609070120',120,20,'protocol','look','REGISTERED',NULL)")
rejected('FIXED_WINDOW',"UPDATE r926_window SET champion_bundle_hash='other' WHERE window_id='w'")
rejected('NO_SKIP_WINDOW_STATES',"UPDATE r926_window SET state='PASSED' WHERE window_id='w'")
db.execute("INSERT INTO r926_pair VALUES ('w','Binance1-PK10','202609070001','cp','bp',1,0,0.4,0.3,'cal','label','pair')")
rejected('DUP_PAIR',"INSERT INTO r926_pair SELECT * FROM r926_pair")
rejected('OUT_OF_WINDOW',"INSERT INTO r926_pair VALUES ('w','Binance1-PK10','202609070121','cp','bp',1,0,0.4,0.3,'cal','label2','pair2')")
rejected('WRONG_GAME',"INSERT INTO r926_pair VALUES ('w','OTHER','202609070002','cp','bp',1,0,0.4,0.3,'cal','label2','pair2')")
rejected('WRONG_CAL',"INSERT INTO r926_pair VALUES ('w','Binance1-PK10','202609070002','cp','bp',1,0,0.4,0.3,'bad','label2','pair2')")
rejected('INVALID_PROBABILITY',"INSERT INTO r926_pair VALUES ('w','Binance1-PK10','202609070002','cp','bp',1,0,2.0,0.3,'cal','label2','pair2')")
rejected('CHILD_EVIDENCE_ZERO',"INSERT INTO r926_repair_intent VALUES ('r','parent','child','cluster','type','[]','effect','rule','202609070000',200,NULL,'PENDING')")
rejected('MISSING_MASK_REASON',"INSERT INTO r926_atom_measure VALUES ('Binance1-PK10','202609070001','atom','schema',0,NULL,NULL,'202609070000','PRODUCTION',1,'i','o')")
db.execute("INSERT INTO r926_atom_measure VALUES ('Binance1-PK10','202609070001','atom','schema',1,0,NULL,'202609070000','PRODUCTION',1,'i','o')")
checks.append('ZERO_IS_VALID_NOT_MISSING')
assert not list(db.execute('PRAGMA foreign_key_check'))
result={'plan_structure':'PASS','tasks':len(tasks),'mapped_requirement_rows':len(trace),'original_file_requirements':len(file_rows),'original_acceptance_rows':len(oldtests),'planned_test_cases':len(cases),'java_baseline_files':len(java),'sql_tables':db.execute("SELECT count(*) FROM sqlite_master WHERE type='table'").fetchone()[0],'sql_idempotence':'PASS','design_constraint_checks':checks,'application_implemented':False,'android_runtime':'NOT_RUN','production_model':'NOT_PROVEN'}
print(json.dumps(result,ensure_ascii=False,indent=2))
