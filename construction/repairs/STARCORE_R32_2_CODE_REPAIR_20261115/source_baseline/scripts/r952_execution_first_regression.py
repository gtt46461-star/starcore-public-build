#!/usr/bin/env python3
"""R952 regression: admission cannot outrun first execution attempts.

This is a source-wiring + real SQLite semantics gate. It does not claim device latency,
future-OOS effect, or APK signing evidence.
"""
from pathlib import Path
import sqlite3, json
R=Path(__file__).resolve().parents[1]
S=R/'app/src/main/java/com/ethk3/novaonepro'

def text(name): return (S/name).read_text(encoding='utf-8')
def need(src, token, code):
    if token not in src: raise AssertionError(f'{code}|missing={token}')

sched=text('GlobalResearchScheduleR932.java')
predraw=text('ResearchPredrawCaptureR923.java')
alg=text('AlgorithmInventionLabR908.java')
gen=text('AssetGenesisEngineR915.java')
formula=text('ResearchEvaluationSchedulerR906.java')
tiny=text('ResearchTinyDeepLearnerR909.java')

need(predraw,'GlobalResearchScheduleR932.prepareExecutionFirst(store,target,snapshot.recordHash);','R952_OWNER_GATE_NOT_WIRED')
need(sched,'DEFER_REGISTRATION_UNSTARTED_ADMITTED','R952_OWNER_DEFER_STATE_MISSING')
need(sched,'COMPLETE_PLAN_ALREADY_PUBLISHED','R952_COMPLETE_PLAN_FAST_PATH_MISSING')
need(sched,"cap.status='SELECTED' AND cap.execution_started=0 AND cap.frozen=0",'R952_UNSTARTED_GATE_SQL_MISSING')
need(sched,'public static int pendingLaneMask','R952_PENDING_LANE_MASK_MISSING')
need(predraw,'chooseLane(first, pendingMask, attemptedLane)','R952_PENDING_LANE_PRIORITY_NOT_WIRED')
need(text('ResearchPipelineStatusR941.java'),'R952执行优先：owner=','R952_DIAGNOSTIC_VISIBILITY_MISSING')
need(tiny,'R952_TINY_DEEP_CAPTURE_LEDGER_RECONCILE','R952_TINY_RECONCILE_MISSING')
need(tiny,'R952_RESEARCH_TINY_DEEP_PREDRAW_ATOMIC','R952_TINY_ATOMIC_FREEZE_MISSING')
need(tiny,'ResearchCaptureLedgerR932.record(db,target,"tiny:"+CANDIDATE_ID,"FROZEN"','R952_TINY_LEDGER_FREEZE_MISSING')
need(tiny,'scoringCostMs,true','R952_TINY_FULL1200_MISSING')
for name,src,prefix in [
    ('algorithm',alg,"('algorithm:'||g.genome_id)"),
    ('genesis',gen,"('genesis:'||"),
    ('formula',formula,"('formula:'||a.asset_id)")]:
    need(src,'cap.execution_started',f'R952_{name.upper()}_UNSTARTED_PRIORITY_MISSING')

# Exact SQL semantics of admittedPage for a partial manifest prefix.
c=sqlite3.connect(':memory:')
c.executescript('''
CREATE TABLE research_schedule_plan_r932(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,roster_hash TEXT,created_at INTEGER);
CREATE TABLE research_schedule_task_r932(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,family TEXT,structure_hash TEXT,tier TEXT,selected INTEGER NOT NULL,queue_rank INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key));
CREATE TABLE research_capture_opportunity_r932(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,system_available INTEGER,eligible INTEGER,selected INTEGER,execution_started INTEGER,frozen INTEGER,full1200 INTEGER,status TEXT,reason TEXT,record_hash TEXT,cost_ms INTEGER,updated_at INTEGER,PRIMARY KEY(target_issue,candidate_key));
CREATE TABLE research_roster_prepare_r943(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,cursor INTEGER NOT NULL,created_at INTEGER NOT NULL);
CREATE TABLE research_roster_manifest_r948(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,candidate_count INTEGER NOT NULL,created_at INTEGER NOT NULL);
CREATE TABLE research_candidate_identity_r932(candidate_key TEXT PRIMARY KEY,family TEXT,structure_hash TEXT,first_issue TEXT);
''')
t='202609140101'; inp='a'*64; ph='b'*64
c.execute('INSERT INTO research_roster_prepare_r943 VALUES(?,?,?,?,?,?)',(t,inp,'[]',ph,32,1))
c.execute('INSERT INTO research_roster_manifest_r948 VALUES(?,?,?,?,?)',(t,inp,ph,96,1))
for i in range(32):
    key=f'algorithm:a{i:02d}'
    c.execute('INSERT INTO research_schedule_task_r932 VALUES(?,?,?,?,?,?,?)',(t,key,'f','c'*64,'B',1,i))
    c.execute('INSERT INTO research_capture_opportunity_r932 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)',(t,key,1,1,1,0,0,0,'SELECTED','','',0,1))
    c.execute('INSERT INTO research_candidate_identity_r932 VALUES(?,?,?,?)',(key,'f','c'*64,t))

admitted="""((NOT EXISTS(SELECT 1 FROM research_roster_manifest_r948 absent WHERE absent.target_issue=sq.target_issue) AND EXISTS(SELECT 1 FROM research_schedule_plan_r932 sp WHERE sp.target_issue=sq.target_issue)) OR EXISTS(SELECT 1 FROM research_roster_manifest_r948 m948 JOIN research_roster_prepare_r943 p948 ON p948.target_issue=m948.target_issue AND p948.input_hash=m948.input_hash AND p948.payload_hash=m948.payload_hash WHERE m948.target_issue=sq.target_issue AND sq.queue_rank>=0 AND sq.queue_rank<p948.cursor AND sq.queue_rank<m948.candidate_count AND EXISTS(SELECT 1 FROM research_candidate_identity_r932 i948 WHERE i948.candidate_key=sq.candidate_key AND i948.structure_hash=sq.structure_hash)))"""
unstarted=f"""SELECT 1 FROM research_schedule_task_r932 sq JOIN research_capture_opportunity_r932 cap ON cap.target_issue=sq.target_issue AND cap.candidate_key=sq.candidate_key WHERE sq.target_issue=? AND sq.selected=1 AND cap.status='SELECTED' AND cap.execution_started=0 AND cap.frozen=0 AND {admitted} LIMIT 1"""
assert c.execute(unstarted,(t,)).fetchone()==(1,)
# A candidate already classified as unavailable/ineligible is handled for roster-progress purposes
# without lying that model execution started; it remains unfrozen and retryable by its lane.
c.execute("UPDATE research_capture_opportunity_r932 SET status='SYSTEM_INPUT_UNAVAILABLE' WHERE target_issue=? AND candidate_key='algorithm:a00'",(t,))
assert c.execute(unstarted,(t,)).fetchone()==(1,)  # other untouched SELECTED rows still block
c.execute("UPDATE research_capture_opportunity_r932 SET status='SYSTEM_INPUT_UNAVAILABLE' WHERE target_issue=?",(t,))
assert c.execute(unstarted,(t,)).fetchone() is None
c.execute("UPDATE research_capture_opportunity_r932 SET status='SELECTED' WHERE target_issue=?",(t,))

# Once every currently admitted candidate has received an attempt, registration may advance.
c.execute("UPDATE research_capture_opportunity_r932 SET execution_started=1 WHERE target_issue=?",(t,))
assert c.execute(unstarted,(t,)).fetchone() is None

# Pending lane remains algorithm until freezes complete; this drives lane priority, not registration gating.
pending=f"""SELECT sq.candidate_key FROM research_schedule_task_r932 sq JOIN research_capture_opportunity_r932 cap ON cap.target_issue=sq.target_issue AND cap.candidate_key=sq.candidate_key WHERE sq.target_issue=? AND sq.selected=1 AND cap.frozen=0 AND {admitted}"""
keys=[r[0] for r in c.execute(pending,(t,))]
mask=0
for key in keys:
    if key.startswith('tiny:'): mask|=1
    elif key.startswith('algorithm:'): mask|=2
    elif key.startswith('genesis:'): mask|=4
    elif key.startswith('formula:'): mask|=8
assert mask==2
c.execute("UPDATE research_capture_opportunity_r932 SET frozen=1 WHERE target_issue=?",(t,))
assert c.execute(pending,(t,)).fetchall()==[]

# Retry ordering: never-started candidates must precede already-started retry candidates.
c.executescript('CREATE TABLE genomes(id TEXT PRIMARY KEY);')
c.executemany('INSERT INTO genomes VALUES(?)',[(f'a{i}',) for i in range(4)])
c.execute('DELETE FROM research_capture_opportunity_r932')
for i in range(4):
    c.execute('INSERT INTO research_capture_opportunity_r932 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)',(t,'algorithm:a'+str(i),1,1,1,1 if i<2 else 0,0,0,'X','','',0,1))
order=[r[0] for r in c.execute("SELECT id FROM genomes g ORDER BY COALESCE((SELECT cap.execution_started FROM research_capture_opportunity_r932 cap WHERE cap.target_issue=? AND cap.candidate_key=('algorithm:'||g.id)),0) ASC,id ASC",(t,))]
assert order[:2]==['a2','a3'],order

# Pure lane-selector contract used by the Java implementation.
def choose(first,mask,attempted):
    start=first%4
    for p in range(2):
        for off in range(4):
            lane=(start+off)%4
            if attempted[lane]: continue
            if p==0 and mask!=0 and (mask&(1<<lane))==0: continue
            return lane
    return -1
assert choose(0,2,[False]*4)==1
assert choose(3,4,[False]*4)==2
assert choose(2,0,[False]*4)==2
attempt=[False]*4; attempt[1]=True
assert choose(0,2,attempt)==0  # pending lane already attempted -> deterministic fallback

print('R952_EXECUTION_FIRST_REGRESSION_PASS|'+json.dumps({
    'scope':'SOURCE_WIRING_AND_REAL_SQLITE_SEMANTICS_NOT_DEVICE_EFFECT',
    'registration_defers_while_admitted_rows_never_started':True,
    'registration_resumes_after_first_attempts':True,
    'pending_lane_mask_prioritizes_nonfrozen_work':True,
    'algorithm_genesis_formula_prioritize_never_started':True,
    'lane_rotation_is_tiebreaker_not_starvation_source':True,
    'complete_plan_fast_path_present':True,
    'diagnostic_visibility_present':True,
    'handled_unavailable_does_not_block_roster_growth':True,
    'tiny_deep_freeze_updates_unified_capture_ledger':True,
},sort_keys=True))
