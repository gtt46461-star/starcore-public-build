#!/usr/bin/env python3
"""Execute the actual generated queue predicate + recovery DDL on SQLite.
No Android coordinator, original database, or CAS replay is claimed.
"""
from pathlib import Path
import json,re,sqlite3,hashlib
base=Path(__file__).resolve().parents[1]
main=base/'after/app/src/main/java/com/ethk3/novaonepro'
queue=(main/'ResearchTaskQueueR967.java').read_text();recovery=(main/'r952/TrainingRecoveryR343.java').read_text()
constants={'TABLE':'v57_research_task_r967','PIN':'v57_research_input_pin_r967','LANE':'v57_research_lane_cursor_r967','com.ethk3.novaonepro.r952.TrainingRecoveryR343.TABLE':'research_training_recovery_r343'}
def value(expr,extra=None):
 env=constants| (extra or {});out=[]
 for part in re.split(r'\s*\+\s*(?=(?:[^"\\]|\\.|"(?:[^"\\]|\\.)*")*$)',expr.strip()):
  part=part.strip()
  if part.startswith('"'):out.append(json.loads(part))
  elif part in env:out.append(env[part])
  else:raise ValueError('unhandled Java constant: '+part)
 return ''.join(out)
def literals(text,extra=None):
 return [value(e,extra) for e in re.findall(r'db\.execSQL\(((?:"(?:[^"\\]|\\.)*"|[^;])*?)\);',text,re.S) if e.strip().startswith('"CREATE')]
# Use only the actual production schema method, no hand-maintained predicate copy.
qs=queue[queue.index('public static void schema'):queue.index('public static String naturalKey')]
rs=recovery[recovery.index('public static void schema'):recovery.index('public static boolean installed')]
predicate_source=queue[queue.index('static String readyPredicate()'):queue.index('public static boolean hasReady')]
expr=predicate_source[predicate_source.index('return ')+7:predicate_source.rindex(';')]
predicate=value(expr)
db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
for t,cols in [('r952_candidate','candidate_id TEXT PRIMARY KEY'),('research_training_protocol_v2','protocol_hash TEXT PRIMARY KEY'),('research_training_input_v2','input_hash TEXT PRIMARY KEY'),('research_action_v2','action_id TEXT PRIMARY KEY')]:db.execute(f'CREATE TABLE {t}({cols})')
for sql in literals(qs):db.execute(sql)
for sql in literals(rs,{'TABLE':'research_training_recovery_r343'}):db.execute(sql)
def h(s):return hashlib.sha256(s.encode()).hexdigest()
cand='candidate';protocol=h('protocol');inp=h('input');authority=h('action')
for table,key in [('r952_candidate',cand),('research_training_protocol_v2',protocol),('research_training_input_v2',inp),('research_action_v2',authority)]:db.execute(f'INSERT INTO {table} VALUES(?)',(key,))
def task(id,issue,status,kind='TRAIN_CANDIDATE',candidate=cand):
 db.execute("INSERT INTO v57_research_task_r967(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,cursor,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,'FEATURES|128|original',1,1)",(id,kind,candidate,issue,protocol,inp,authority,status))
def ready(id,kind='TRAIN_CANDIDATE'):
 return bool(db.execute('SELECT 1 FROM v57_research_task_r967 t WHERE '+predicate+' AND t.task_id=?',(kind,'9999','process',id)).fetchone())
def expect_error(sql,args=()):
 try:db.execute(sql,args)
 except sqlite3.IntegrityError:return
 raise AssertionError('expected invariant rejection: '+sql)
old=h('old');new=h('new');task(old,'202611260092','FAILED_TERMINAL');task(new,'202611260101','PENDING')
assert not ready(new),'unaudited successor must be blocked'
prior=db.execute('SELECT * FROM v57_research_task_r967 WHERE task_id=?',(old,)).fetchone()
expect_error("UPDATE v57_research_task_r967 SET status='PENDING' WHERE task_id=?",(old,))
receipt=(h('recovery'),old,new,cand,protocol,inp,h('ast'),authority,h('reason'),'{}',h('receipt'),1)
db.execute('INSERT INTO research_training_recovery_r343 VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',receipt)
assert ready(new),'only audited successor resumes'
expect_error('UPDATE research_training_recovery_r343 SET candidate_id=?',('changed',))
expect_error('DELETE FROM research_training_recovery_r343')
expect_error('INSERT OR REPLACE INTO research_training_recovery_r343 VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',receipt)
assert db.execute('SELECT * FROM v57_research_task_r967 WHERE task_id=?',(old,)).fetchone()==prior
other=h('qualityfail');task(other,'202611260093','FAILED_TERMINAL')
assert not ready(new),'another quality/integrity terminal remains blocking'
postold=h('postold');postnew=h('postnew');task(postold,'202611260092','FAILED_TERMINAL','POST_TRAIN');task(postnew,'202611260101','PENDING','POST_TRAIN')
assert ready(postnew,'POST_TRAIN'),'existing independent POST semantics preserved'
db.commit();before=db.execute('SELECT count(*) FROM v57_research_task_r967').fetchone()[0]
db.execute('BEGIN');task(h('rollback'),'202611260102','PENDING');db.rollback()
assert before==db.execute('SELECT count(*) FROM v57_research_task_r967').fetchone()[0]
assert db.execute('PRAGMA foreign_key_check').fetchall()==[]
assert db.execute('PRAGMA integrity_check').fetchone()==('ok',)
result={'status':'PASS','assertions':11,'scope':'actual patched production DDL and queue predicate in SQLite, synthetic fixtures; not Android or real CAS recovery','predicate_sha256':h(predicate),'sqlite_version':sqlite3.sqlite_version,'android':'NOT_RUN','original_device_database':'NOT_AVAILABLE'}
(base/'verification/recovery_sql_result.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result))
