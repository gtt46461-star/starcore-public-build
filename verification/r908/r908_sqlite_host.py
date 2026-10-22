#!/usr/bin/env python3
import sqlite3, hashlib
h=lambda s:hashlib.sha256(s.encode()).hexdigest()
con=sqlite3.connect(':memory:')
con.execute('PRAGMA foreign_keys=ON')
# Pre-R908 installed database skeleton: DB_VERSION intentionally remains 36.
con.executescript('''
CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER);
CREATE TABLE v57_settlement_task_r36(target_issue TEXT PRIMARY KEY,status TEXT NOT NULL DEFAULT 'PENDING',task_class TEXT NOT NULL DEFAULT 'BACKLOG',priority INTEGER NOT NULL DEFAULT 0,next_attempt_at INTEGER NOT NULL DEFAULT 0,lease_until INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL DEFAULT 0);
CREATE TABLE v57_algorithm_genome_r908(genome_id TEXT PRIMARY KEY,display_name_zh TEXT NOT NULL,genome_spec TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,behavior_hash TEXT NOT NULL DEFAULT '',generation INTEGER NOT NULL,parent_ids TEXT NOT NULL DEFAULT '',trained_until_issue TEXT NOT NULL DEFAULT '',earliest_use_issue TEXT NOT NULL DEFAULT '',dev_n INTEGER NOT NULL DEFAULT 0,dev_hit INTEGER NOT NULL DEFAULT 0,dev_hit_rate REAL,dev_wilson_lower REAL,dev_block_min REAL,dev_max_miss INTEGER NOT NULL DEFAULT 0,novelty REAL NOT NULL DEFAULT 0,complexity REAL NOT NULL DEFAULT 0,state_blob TEXT NOT NULL,state_updates INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,may_authorize_real INTEGER NOT NULL DEFAULT 0 CHECK(may_authorize_real=0),created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);
''')
# Same-version additive migrations. Re-run helpers idempotently just like DbHelper.addColumnR36.
def addcol(table, definition):
    name=definition.split()[0]
    cols={r[1] for r in con.execute(f'PRAGMA table_info({table})')}
    if name not in cols: con.execute(f'ALTER TABLE {table} ADD COLUMN {definition}')
for c in ["lease_generation INTEGER NOT NULL DEFAULT 0","process_boot_id TEXT NOT NULL DEFAULT ''","last_progress_at INTEGER NOT NULL DEFAULT 0","eligibility TEXT NOT NULL DEFAULT 'UNKNOWN'","failure_class TEXT NOT NULL DEFAULT ''","recovery_count INTEGER NOT NULL DEFAULT 0"]: addcol('v57_settlement_task_r36',c)
for c in ["future_n INTEGER NOT NULL DEFAULT 0","future_hit INTEGER NOT NULL DEFAULT 0","future_hit_rate REAL","future_wilson_lower REAL","future_last_issue TEXT NOT NULL DEFAULT ''","future_evidence_hash TEXT NOT NULL DEFAULT ''"]: addcol('v57_algorithm_genome_r908',c)
# Replay migrations must be no-op.
for c in ["future_n INTEGER NOT NULL DEFAULT 0","future_hit INTEGER NOT NULL DEFAULT 0","future_hit_rate REAL","future_wilson_lower REAL","future_last_issue TEXT NOT NULL DEFAULT ''","future_evidence_hash TEXT NOT NULL DEFAULT ''"]: addcol('v57_algorithm_genome_r908',c)
con.executescript('''
CREATE TABLE IF NOT EXISTS v57_issue_closure_r906(target_issue TEXT PRIMARY KEY,draw_hash TEXT NOT NULL,predraw_record_hash TEXT NOT NULL DEFAULT '',eligibility TEXT NOT NULL,core_status TEXT NOT NULL,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1,oos_record_hash TEXT NOT NULL DEFAULT '',settlement_hash TEXT NOT NULL DEFAULT '',classified_at INTEGER NOT NULL,completed_at INTEGER NOT NULL DEFAULT 0,record_hash TEXT NOT NULL UNIQUE);
CREATE TABLE IF NOT EXISTS v57_settlement_outbox_r906(event_id TEXT PRIMARY KEY,event_type TEXT NOT NULL,target_issue TEXT NOT NULL,payload_hash TEXT NOT NULL,status TEXT NOT NULL,priority INTEGER NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS v57_algorithm_predraw_r908(genome_id TEXT NOT NULL,target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_ordinal INTEGER NOT NULL,score REAL NOT NULL,feature_blob TEXT NOT NULL,feature_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,record_hash TEXT NOT NULL UNIQUE,actual_number INTEGER NOT NULL DEFAULT 0,hit_label INTEGER NOT NULL DEFAULT -1 CHECK(hit_label IN (-1,0,1)),settled_at INTEGER NOT NULL DEFAULT 0,outcome_hash TEXT NOT NULL DEFAULT '',PRIMARY KEY(genome_id,target_issue));
''')
# 200 closure rows; 184 eligible, 16 excluded. Same denominator semantics as R906 acceptance.
for i in range(1,201):
    issue=f'20260827{i:04d}'; ex=i<=16; elig='EXCLUDED_NO_PREDRAW' if ex else 'ELIGIBLE_PREDRAW'; status='EXCLUDED' if ex else 'COMPLETED'; hit=-1 if ex else (1 if i%3==0 else 0)
    con.execute('INSERT INTO v57_issue_closure_r906 VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',(issue,h('d'+issue),'' if ex else h('p'+issue),elig,status,0 if ex else i%10+1,hit,'' if ex else h('o'+issue),'' if ex else h('s'+issue),1000+i,1000+i,h(issue+elig+status+str(hit))))
assert con.execute('SELECT count(*) FROM v57_issue_closure_r906').fetchone()[0]==200
assert con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE eligibility='ELIGIBLE_PREDRAW' AND core_status='COMPLETED'").fetchone()[0]==184
# Atomic fault: closure + outbox must roll back together.
try:
    with con:
        con.execute("INSERT INTO v57_issue_closure_r906(target_issue,draw_hash,eligibility,core_status,classified_at,record_hash) VALUES(?,?,?,?,?,?)",('FAULT',h('d'),'ELIGIBLE_PREDRAW','RUNNING_CORE',1,h('r')))
        con.execute("INSERT INTO v57_settlement_outbox_r906 VALUES(?,?,?,?,?,?,?,?)",('E','P1','FAULT',h('p'),'PENDING',100,1,1))
        raise RuntimeError('inject')
except RuntimeError: pass
assert con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE target_issue='FAULT'").fetchone()[0]==0
assert con.execute("SELECT count(*) FROM v57_settlement_outbox_r906 WHERE target_issue='FAULT'").fetchone()[0]==0
# Algorithm contract: future OOS fields persist, research cannot authorize REAL.
con.execute("INSERT INTO v57_algorithm_genome_r908(genome_id,display_name_zh,genome_spec,structural_hash,generation,state_blob,status,created_at,updated_at,future_n,future_hit,future_hit_rate,future_wilson_lower,future_last_issue,future_evidence_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",('G1','玄策·迁移记忆分流算法·第01代','spec',h('struct'),1,'R908S1','ACTIVE_FUTURE_OOS',1,1,24,11,11/24,0.2667,'202608270760',h('future')))
r=con.execute('SELECT future_n,future_hit,future_last_issue,may_authorize_real FROM v57_algorithm_genome_r908 WHERE genome_id=?',('G1',)).fetchone(); assert r==(24,11,'202608270760',0),r
try:
    con.execute("INSERT INTO v57_algorithm_genome_r908(genome_id,display_name_zh,genome_spec,structural_hash,generation,state_blob,status,may_authorize_real,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",('BAD','bad','spec2',h('s2'),1,'x','BAD',1,1,1)); raise AssertionError('REAL boundary accepted')
except sqlite3.IntegrityError: pass
# Predraw label integrity.
con.execute("INSERT INTO v57_algorithm_predraw_r908(genome_id,target_issue,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash,frozen_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",('G1','202608270761',3,1,4,8,222,.7,'f',h('f'),10,h('rec')))
try:
    con.execute("UPDATE v57_algorithm_predraw_r908 SET hit_label=2 WHERE genome_id='G1'"); raise AssertionError('bad label accepted')
except sqlite3.IntegrityError: pass
print('R908_SQLITE_HOST=PASS')
print('DB_VERSION_CONTRACT=36_UNCHANGED|SCHEMA_REVISION=4_ADDITIVE')
print('CLOSURE=200|ELIGIBLE_SETTLED=184|EXCLUDED=16')
print('ATOMIC_FAULT_ROLLBACK=PASS|MIGRATION_REPLAY=PASS')
print('ALGORITHM_FUTURE_OOS_COLUMNS=PASS|RESEARCH_REAL_CHECK=PASS|PREDRAW_LABEL_CHECK=PASS')
print('NOTE=HOST_SQLITE_MODEL_NOT_ANDROID_DEVICE_RUNTIME')
