#!/usr/bin/env python3
"""Actual R343 learner DDL/guard and CAS SQL in SQLite; synthetic state, no Android claim."""
from pathlib import Path
import hashlib,json,re,sqlite3
root=Path(__file__).resolve().parents[1]
file=root/'rolling_candidate/app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java';source=file.read_text()
env={'EVENT':'research_rolling_event_r343','VERSION':'research_rolling_version_r343','HEAD':'research_rolling_head_r343','FEATURE':'research_rolling_feature_r343','OFFER':'research_rolling_offer_r343','DbHelper.TABLE_DRAWS':'draws','DbHelper.TABLE_R811_DRAW_CANONICAL':'v57_draw_canonical_r811','DrawDependencyInputCP12.TABLE':'draw_dependency_input_cp12'}
def val(expr,extra={}):
 out=[];names=env|extra
 for p in re.split(r'\s*\+\s*(?=(?:[^"\\]|\\.|"(?:[^"\\]|\\.)*")*$)',expr.strip()):
  p=p.strip();out.append(json.loads(p) if p.startswith('"') else names[p])
 return ''.join(out)
schema=source[source.index('public static void schema'):source.index('public static boolean installed')]
exprs=re.findall(r'db\.execSQL\(((?:"(?:[^"\\]|\\.)*"|[^;])*?)\);',schema,re.S)
commands=[]
for e in exprs:
 if re.search(r'\+table\+',e):
  commands += [val(e,{'table':env[k]}) for k in ['EVENT','OFFER']]
 elif re.search(r'\+action\+',e):
  actions=['INSERT','DELETE'] if 'DrawDependencyInputCP12.TABLE' in e else ['INSERT','UPDATE','DELETE']
  commands += [val(e,{'action':a}) for a in actions]
 else:commands.append(val(e))
db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
db.execute('CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY)');db.execute('CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)')
for t in ['draws','draw_dependency_input_cp12']:db.execute(f'CREATE TABLE {t}(issue TEXT PRIMARY KEY,dirty INTEGER,n1 INTEGER)')
db.execute('CREATE TABLE v57_draw_canonical_r811(target_issue TEXT PRIMARY KEY,status TEXT,canonical_draw_hash TEXT)')
for cmd in commands:db.execute(cmd)
n=0
def ok(b):
 global n;n+=1
 if not b:raise AssertionError(n)
def reject(sql,args=()):
 global n;n+=1
 try:db.execute(sql,args)
 except sqlite3.IntegrityError:return
 raise AssertionError('invariant accepted at '+str(n)+': '+sql)
def h(v):return hashlib.sha256(v.encode()).hexdigest()
def version(vid,candidate='base',cutoff='202611260100',protocol=None):
 protocol=protocol or h('p'+vid)
 columns='version_id,candidate_id,predecessor,cutoff_issue,input_first_issue,ast_hash,protocol_hash,protocol_json,input_hash,input_bytes,input_blob,train_n,train_first,train_last,validation_first,validation_last,seed,state,prefix_hash,created_at,updated_at'
 values=(vid,candidate,'',cutoff,'202611250080',h('ast'),protocol,'{}',h('input'),16,b'0123456789012345',300,'202611250100','202611250399','202611250401',cutoff,42,'FEATURES',h('prefix'),1,1)
 return 'INSERT INTO '+env['VERSION']+'('+columns+') VALUES('+','.join('?'for _ in values)+')',values
for c in ['base','other']:db.execute('INSERT INTO r952_candidate VALUES(?)',(c,))
db.execute(*version('v1'));db.execute('INSERT INTO '+env['HEAD']+"(candidate_id,updated_at) VALUES('base',1)")
reject(*version('v2','base','202611260101')) # one active per base
reject(*version('bad','absent')) # FK
for col,value in [('ast_hash',h('new')),('candidate_id','other'),('cutoff_issue','202611260101'),('input_first_issue','202611240001'),('protocol_json','{"changed":true}')]:reject('UPDATE '+env['VERSION']+' SET '+col+'=? WHERE version_id=?',(value,'v1'))
reject('UPDATE '+env['VERSION']+' SET input_blob=NULL WHERE version_id=?',('v1',))
row=('v1',0,'202611250100',1,h('raw'),b'x'*18,h('row'),'{}',h('{}'))
db.execute('INSERT INTO '+env['FEATURE']+' VALUES(?,?,?,?,?,?,?,?,?)',row)
reject('UPDATE '+env['FEATURE']+" SET actual=2 WHERE version_id='v1'")
reject('DELETE FROM '+env['FEATURE']+" WHERE version_id='v1'")
reject('INSERT OR REPLACE INTO '+env['FEATURE']+' VALUES(?,?,?,?,?,?,?,?,?)',row)
reject('INSERT OR REPLACE INTO '+env['VERSION']+" SELECT * FROM "+env['VERSION']+" WHERE version_id='v1'")
reject('DELETE FROM '+env['VERSION']+" WHERE version_id='v1'")
# Exact HEAD compare-and-swap SQL, extracted from product method.
match=re.search(r'db\.execSQL\(("UPDATE "\+HEAD\+" SET version_id=.*?),new Object\[\]',source)
head_sql=val(match.group(1));params=('v1','202611260100',h('model'),2,'base','')
db.commit();db.execute('BEGIN');db.execute(head_sql,params);ok(db.execute('SELECT changes()').fetchone()[0]==1);db.rollback();ok(db.execute('SELECT version_id FROM '+env['HEAD']).fetchone()==('',))
db.execute(head_sql,params);ok(db.execute('SELECT changes()').fetchone()[0]==1)
db.execute(head_sql,params);ok(db.execute('SELECT changes()').fetchone()[0]==0) # stale predecessor
# Mark finished once, never rewrite completion evidence. Cleanup permits NULL bytes only.
db.execute('UPDATE '+env['VERSION']+" SET state='COMPLETE',model_payload='original',model_hash=?,receipt_json='{}',receipt_hash=?,checkpoint=?,checkpoint_hash=? WHERE version_id='v1'",(h('model'),h('{}'),b'checkpoint',h('cp')))
for column,value in [('state','TRAINING'),('model_hash',h('forged')),('receipt_json','forged'),('checkpoint_hash',h('new')),('prefix_hash',h('new'))]:reject('UPDATE '+env['VERSION']+' SET '+column+'=? WHERE version_id=?',(value,'v1'))
reject('UPDATE '+env['VERSION']+" SET model_payload='forged' WHERE version_id='v1'")
db.execute('UPDATE '+env['VERSION']+" SET input_blob=NULL,checkpoint=NULL,model_payload='' WHERE version_id='v1'")
reject('UPDATE '+env['VERSION']+" SET input_blob=? WHERE version_id='v1'",(b'original',))
reject('UPDATE '+env['VERSION']+" SET model_payload='original' WHERE version_id='v1'")
db.execute('DELETE FROM '+env['FEATURE']+" WHERE version_id='v1'");ok(db.execute('SELECT COUNT(*) FROM '+env['FEATURE']).fetchone()[0]==0)
# A later numeric version is permitted without a new strategy record.
db.execute(*version('v2','base','202611260101'));ok(db.execute('SELECT COUNT(*) FROM r952_candidate').fetchone()[0]==2)
db.execute('UPDATE '+env['VERSION']+" SET state='DEPENDENCY_GAP',receipt_json='{}',receipt_hash=? WHERE version_id='v2'",(h('gap'),));reject('UPDATE '+env['VERSION']+" SET state='FEATURES' WHERE version_id='v2'")
db.execute(*version('v3','base','202611260102'))
# Event/offer rows cannot be rewritten, deleted or replaced.
event=('202611260100',h('draw'),h('settled'),1,h('event'));db.execute('INSERT INTO '+env['EVENT']+' VALUES(?,?,?,?,?)',event)
reject('INSERT OR REPLACE INTO '+env['EVENT']+' VALUES(?,?,?,?,?)',event);reject('DELETE FROM '+env['EVENT']);reject('UPDATE '+env['EVENT']+" SET draw_hash='forged'")
offer=('v1','base','202611260100',h('model'),h('{}'),'NEEDS_INDEPENDENT_PUBLICATION_ADAPTER',1);db.execute('INSERT INTO '+env['OFFER']+' VALUES(?,?,?,?,?,?,?)',offer)
reject('INSERT OR REPLACE INTO '+env['OFFER']+' VALUES(?,?,?,?,?,?,?)',offer);reject('DELETE FROM '+env['OFFER']);reject('UPDATE '+env['OFFER']+" SET model_hash='forged'")
epoch=lambda:int(db.execute("SELECT state_value FROM app_state WHERE state_key='r343_rolling_canonical_epoch'").fetchone()[0])
e=epoch();db.execute("INSERT INTO draws VALUES('202611260001',0,1)");ok(epoch()==e+1);db.execute("UPDATE draws SET dirty=1");ok(epoch()==e+2);db.execute("DELETE FROM draws");ok(epoch()==e+3)
db.execute("INSERT INTO v57_draw_canonical_r811 VALUES('202611260001','CONFIRMED_CANONICAL_DRAW','hash')");ok(epoch()==e+4);db.execute("UPDATE v57_draw_canonical_r811 SET canonical_draw_hash='new'");ok(epoch()==e+5);db.execute('DELETE FROM v57_draw_canonical_r811');ok(epoch()==e+6)
db.execute("INSERT INTO draw_dependency_input_cp12 VALUES('202611260001',0,1)");ok(epoch()==e+7);db.execute('DELETE FROM draw_dependency_input_cp12');ok(epoch()==e+8)
ok(db.execute('PRAGMA foreign_key_check').fetchall()==[]);ok(db.execute('PRAGMA integrity_check').fetchone()==('ok',))
result={'status':'PASS','assertions':n,'source_sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'ddl_statements':len(commands),'sqlite_version':sqlite3.sqlite_version,'scope':'actual learner DDL/guards and head CAS SQL, synthetic host SQLite; no Android owner, kernel throughput, actual report database or quality claim','android':'NOT_RUN'}
(root/'verification/rolling_sql_result.json').write_text(json.dumps(result,indent=2));print(json.dumps(result))
