#!/usr/bin/env python3
"""R951 host regression for predraw roster/execution throughput.

Proves source wiring and the SQL semantics of a committed partial roster prefix. It does not
manufacture future outcomes and does not claim device throughput.
"""
from pathlib import Path
import sqlite3, json, re
R=Path(__file__).resolve().parents[1]
SRC=R/'app/src/main/java/com/ethk3/novaonepro'

def text(name): return (SRC/name).read_text(encoding='utf-8')
def need(hay, token, code):
    if token not in hay: raise AssertionError(f'{code}|missing={token}')

global_schedule=text('GlobalResearchScheduleR932.java')
predraw=text('ResearchPredrawCaptureR923.java')
tiny=text('ResearchTinyDeepLearnerR909.java')
alg=text('AlgorithmInventionLabR908.java')
gen=text('AssetGenesisEngineR915.java')
formula=text('ResearchShadowChallengerRuntimeR817.java')

# The outer owner advances exactly one roster page per predraw retry; consumers only self-prime
# when there is no admitted prefix.
need(predraw,'GlobalResearchScheduleR932.prepare(store,target,snapshot.recordHash);','R951_OWNER_ROSTER_ADVANCE_MISSING')
need(global_schedule,'public static void prepareConsumer','R951_CONSUMER_GUARD_MISSING')
need(global_schedule,'p.cursor>0 AND p.cursor<=m.candidate_count','R951_ADMITTED_PREFIX_SQL_MISSING')
for name,src in [('tiny',tiny),('algorithm',alg),('genesis',gen),('formula',formula)]:
    need(src,'GlobalResearchScheduleR932.prepareConsumer(','R951_CONSUMER_NOT_GUARDED_'+name.upper())
    if 'GlobalResearchScheduleR932.prepare(store,target' in src:
        raise AssertionError('R951_CONSUMER_STILL_ADVANCES_ROSTER_'+name.upper())

# Genesis must not allocate/construct the 1200-action atom world before it knows that the
# current admitted page contains a runnable genesis asset.
snapshot_start=gen.index('public static int capturePredraw(final DbStore store,\n            final DeferredEvidenceSnapshotR36.Snapshot snapshot')
snapshot_end=gen.index('private static int capturePrepared',snapshot_start)
snapshot=gen[snapshot_start:snapshot_end]
if snapshot.index('List<AssetRow> assets=loadRunnable') > snapshot.index('GlobalResearchScheduleR932.contexts'):
    raise AssertionError('R951_GENESIS_SNAPSHOT_CONTEXTS_BUILT_BEFORE_RUNNABLE')
runtime_start=gen.index('public static int capturePredraw(\n            final DbStore store, final V51RuntimeResult runtime')
runtime_end=snapshot_start
runtime=gen[runtime_start:runtime_end]
if runtime.index('List<AssetRow> assets=loadRunnable') > runtime.index('new GenesisAssetVmR915.PredrawContext'):
    raise AssertionError('R951_GENESIS_RUNTIME_CONTEXTS_BUILT_BEFORE_RUNNABLE')
need(gen,'R951_GENESIS_RUNNABLE_REQUIRED','R951_GENESIS_PREPARED_REQUIRES_ADMISSION')

# SQL semantic fixture for the consumer guard: partial committed page is enough for execution,
# but an empty cursor is not. This is the exact condition used in GlobalResearchScheduleR932.
c=sqlite3.connect(':memory:')
c.executescript('''
CREATE TABLE research_schedule_plan_r932(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL);
CREATE TABLE research_roster_prepare_r943(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,cursor INTEGER NOT NULL,created_at INTEGER NOT NULL);
CREATE TABLE research_roster_manifest_r948(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,candidate_count INTEGER NOT NULL,created_at INTEGER NOT NULL);
''')
target='202609140101'; inp='a'*64; ph='b'*64
q='''SELECT 1 FROM research_roster_prepare_r943 p JOIN research_roster_manifest_r948 m
 ON m.target_issue=p.target_issue AND m.input_hash=p.input_hash AND m.payload_hash=p.payload_hash
 WHERE p.target_issue=? AND p.input_hash=? AND p.cursor>0 AND p.cursor<=m.candidate_count LIMIT 1'''
c.execute('INSERT INTO research_roster_prepare_r943 VALUES(?,?,?,?,?,?)',(target,inp,'[]',ph,0,1))
c.execute('INSERT INTO research_roster_manifest_r948 VALUES(?,?,?,?,?)',(target,inp,ph,256,1))
assert c.execute(q,(target,inp)).fetchone() is None
c.execute('UPDATE research_roster_prepare_r943 SET cursor=32 WHERE target_issue=?',(target,))
assert c.execute(q,(target,inp)).fetchone()==(1,)
c.execute('UPDATE research_roster_prepare_r943 SET cursor=257 WHERE target_issue=?',(target,))
assert c.execute(q,(target,inp)).fetchone() is None
c.execute('UPDATE research_roster_prepare_r943 SET cursor=32 WHERE target_issue=?',(target,))
c.execute('INSERT INTO research_schedule_plan_r932 VALUES(?,?)',(target,inp))
assert c.execute('SELECT input_hash FROM research_schedule_plan_r932 WHERE target_issue=?',(target,)).fetchone()==(inp,)

print('R951_PREDRAW_THROUGHPUT_REGRESSION_PASS|'+json.dumps({
    'scope':'SOURCE_WIRING_AND_SQL_SEMANTICS_NOT_DEVICE_EFFECT',
    'owner_advances_one_roster_slice':True,
    'four_consumers_do_not_advance_existing_prefix':True,
    'consumer_direct_call_self_prime_guard_present':True,
    'genesis_runnable_checked_before_context_world':True,
    'partial_prefix_cursor_semantics':True,
},ensure_ascii=False,sort_keys=True))
