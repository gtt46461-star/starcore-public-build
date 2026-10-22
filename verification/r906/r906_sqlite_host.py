#!/usr/bin/env python3
import sqlite3, hashlib, time, sys
from pathlib import Path

def h(s): return hashlib.sha256(s.encode()).hexdigest()
con=sqlite3.connect(':memory:')
con.execute("PRAGMA foreign_keys=ON")
# Minimal pre-R906 tables needed for additive migration semantics.
con.executescript('''
CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER);
CREATE TABLE v57_settlement_task_r36(target_issue TEXT PRIMARY KEY,status TEXT NOT NULL DEFAULT 'PENDING');
''')
# R906 additive task columns.
for col in ["lease_generation INTEGER NOT NULL DEFAULT 0","process_boot_id TEXT NOT NULL DEFAULT ''","last_progress_at INTEGER NOT NULL DEFAULT 0","eligibility TEXT NOT NULL DEFAULT 'UNKNOWN'","failure_class TEXT NOT NULL DEFAULT ''","recovery_count INTEGER NOT NULL DEFAULT 0"]:
    con.execute('ALTER TABLE v57_settlement_task_r36 ADD COLUMN '+col)
# Execute the versioned in-project blueprint. R911 shipped a developer-machine absolute path,
# which made this acceptance test non-reproducible for every downstream user.
schema=(Path(__file__).resolve().parent/'R906_SCHEMA.sql').read_text(encoding='utf-8')
con.executescript(schema)
# Idempotent replay.
con.executescript(schema)
# 200 draws: 16 excluded exactly as acceptance matrix SET-002.
for i in range(1,201):
    issue=f'20260827{i:04d}'; excluded=(i<=16)
    eligibility='EXCLUDED_NO_PREDRAW' if excluded else 'ELIGIBLE_PREDRAW'
    status='EXCLUDED' if excluded else 'COMPLETED'
    hit=-1 if excluded else (1 if i%3==0 else 0)
    rec=h(f'{issue}|{eligibility}|{status}|{hit}')
    con.execute('''INSERT INTO v57_issue_closure_r906(target_issue,draw_hash,predraw_record_hash,eligibility,core_status,actual_number,hit_label,oos_record_hash,settlement_hash,classified_at,completed_at,record_hash)
                   VALUES(?,?,?,?,?,?,?,?,?,?,?,?)''',(issue,h('draw'+issue),'' if excluded else h('pred'+issue),eligibility,status,0 if excluded else i%10+1,hit,'' if excluded else h('oos'+issue),'' if excluded else h('settle'+issue),1000+i,1000+i,rec))
# Replay same semantic row must not duplicate due PK; emulate receipt replay with query, not overwrite.
row=con.execute("SELECT record_hash FROM v57_issue_closure_r906 WHERE target_issue='202608270001'").fetchone()[0]
assert row==h('202608270001|EXCLUDED_NO_PREDRAW|EXCLUDED|-1')
# Conflicting hash cannot be inserted under same target.
try:
    con.execute("INSERT INTO v57_issue_closure_r906(target_issue,draw_hash,eligibility,core_status,classified_at,record_hash) VALUES(?,?,?,?,?,?)",('202608270001',h('conflict'),'BLOCKED_DRAW_CONFLICT','BLOCKED',9999,h('other')))
    raise AssertionError('conflict accepted')
except sqlite3.IntegrityError:
    pass
closure=con.execute('SELECT count(*) FROM v57_issue_closure_r906').fetchone()[0]
eligible=con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE eligibility='ELIGIBLE_PREDRAW'").fetchone()[0]
settled=con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE eligibility='ELIGIBLE_PREDRAW' AND core_status='COMPLETED'").fetchone()[0]
excluded=con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE eligibility='EXCLUDED_NO_PREDRAW'").fetchone()[0]
hitden=con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE eligibility='ELIGIBLE_PREDRAW' AND hit_label IN (0,1)").fetchone()[0]
assert (closure,eligible,settled,excluded,hitden)==(200,184,184,16,184),(closure,eligible,settled,excluded,hitden)
# Atomicity simulation: transaction failure leaves neither closure nor outbox.
try:
    with con:
        con.execute("INSERT INTO v57_issue_closure_r906(target_issue,draw_hash,eligibility,core_status,classified_at,record_hash) VALUES(?,?,?,?,?,?)",('202608279999',h('d'),'ELIGIBLE_PREDRAW','RUNNING_CORE',1,h('r')))
        con.execute("INSERT INTO v57_settlement_outbox_r906(event_id,event_type,target_issue,payload_hash,status,priority,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?)",('e','P1','202608279999',h('p'),'PENDING',1,1,1))
        raise RuntimeError('fault injection')
except RuntimeError: pass
assert con.execute("SELECT count(*) FROM v57_issue_closure_r906 WHERE target_issue='202608279999'").fetchone()[0]==0
assert con.execute("SELECT count(*) FROM v57_settlement_outbox_r906 WHERE target_issue='202608279999'").fetchone()[0]==0
# Schema cardinality / task columns.
req_tables=['v57_issue_closure_r906','v57_settlement_outbox_r906','v57_selected_evidence_r906','v57_research_asset_r906','v57_research_eval_coverage_r906','v57_research_compact_label_r906','v57_research_learner_update_r906','v57_research_artifact_job_r906','v57_research_certificate_r906','v57_runtime_truth_snapshot_r906']
for t in req_tables: assert con.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",(t,)).fetchone()
cols={r[1] for r in con.execute('PRAGMA table_info(v57_settlement_task_r36)')}
for c in ['lease_generation','process_boot_id','last_progress_at','eligibility','failure_class','recovery_count']: assert c in cols
print('R906_SQLITE_HOST=PASS')
print('SET_001_MODEL=closure=200/200')
print('SET_002_MODEL=eligible=184/184|excluded=16|hit_denominator=184')
print('ATOMIC_FAULT_ROLLBACK=PASS|CONFLICT_PK_HARD_BLOCK=PASS|MIGRATION_REPLAY=PASS')
print('NOTE=HOST_SQLITE_MODEL_NOT_ANDROID_DEVICE_RUNTIME')
