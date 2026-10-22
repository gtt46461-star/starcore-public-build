#!/usr/bin/env python3
"""R950 regression: future-OOS capture/settlement cannot be starved by legacy proof audit.

Host-only contract test. It does not manufacture future evidence and never writes a phone DB.
"""
from pathlib import Path
import sqlite3, json, sys
R=Path(__file__).resolve().parents[1]
SRC=R/'app/src/main/java/com/ethk3/novaonepro'

def need(text, token, code):
    if token not in text:
        raise AssertionError(f'{code}|missing={token}')

# Source wiring assertions: one canonical pending predicate + dedicated maintenance lane.
learning=(SRC/'ResearchLearningContinuityR912.java').read_text(encoding='utf-8')
predraw=(SRC/'ResearchPredrawCaptureR923.java').read_text(encoding='utf-8')
settle=(SRC/'ResearchPredrawSettlementPumpR908.java').read_text(encoding='utf-8')
roster=(SRC/'StagedResearchRosterR943.java').read_text(encoding='utf-8')
alg=(SRC/'AlgorithmInventionLabR908.java').read_text(encoding='utf-8')
proof=(SRC/'AlgorithmProofRecoveryR948.java').read_text(encoding='utf-8')
need(learning,'String target=ResearchCaptureQueueR946.recover(store);','R950_CANONICAL_RECOVERY_NOT_WIRED')
need(learning,'AlgorithmProofRecoveryR948.maintain(store, 2)','R950_PROOF_MAINTENANCE_LANE_MISSING')
need(predraw,'DEFERRED_TO_CONTINUITY_LANE','R950_PREDRAW_STILL_BLOCKED_BY_AUDIT')
if 'AlgorithmProofRecoveryR948.maintain(store, 16)' in predraw:
    raise AssertionError('R950_PREDRAW_LEGACY_AUDIT_STILL_INLINE')
if 'AlgorithmProofRecoveryR948.maintain(store, 4)' in settle:
    raise AssertionError('R950_SETTLEMENT_LEGACY_AUDIT_STILL_INLINE')
need(roster,'AlgorithmProofRecoveryR948.freezeReadySql("g.genome_id",target)','R950_ROSTER_PROOF_GATE_MISSING')
need(alg,'AlgorithmProofRecoveryR948.freezeReadySql("g.genome_id",target)','R950_EXECUTION_PROOF_GATE_MISSING')
need(proof,"h950.status IN('CLEAN','RESET_CLEAN')",'R950_CLEAN_HEAD_GATE_MISSING')
need(proof,'COALESCE(g.state_updates,0)=0 AND COALESCE(g.future_n,0)=0','R950_NEW_HEAD_ENROLLMENT_GATE_MISSING')

# SQL semantics of the new pre-freeze proof gate.
c=sqlite3.connect(':memory:')
c.executescript('''
CREATE TABLE alg(genome_id TEXT PRIMARY KEY,state_updates INTEGER,future_n INTEGER);
CREATE TABLE head(genome_id TEXT PRIMARY KEY,status TEXT,epoch_start_issue TEXT);
CREATE TABLE predraw(genome_id TEXT,target_issue TEXT);
''')
target='202609140101'
def ready(gid):
    q="""SELECT COUNT(*) FROM alg g WHERE g.genome_id=? AND (
      EXISTS(SELECT 1 FROM head h950 WHERE h950.genome_id=g.genome_id
        AND h950.status IN('CLEAN','RESET_CLEAN')
        AND (h950.epoch_start_issue='' OR ? >= h950.epoch_start_issue))
      OR (COALESCE(g.state_updates,0)=0 AND COALESCE(g.future_n,0)=0
        AND NOT EXISTS(SELECT 1 FROM predraw old950 WHERE old950.genome_id=g.genome_id)))"""
    return c.execute(q,(gid,target)).fetchone()[0]==1
# New genome: allowed to atomically enroll CLEAN on its first freeze.
c.execute('INSERT INTO alg VALUES(?,?,?)',('new',0,0)); assert ready('new')
# Legacy mutable genome: blocked until audit, so it cannot throw inside the live capture loop.
c.execute('INSERT INTO alg VALUES(?,?,?)',('legacy',7,7)); assert not ready('legacy')
c.execute('INSERT INTO head VALUES(?,?,?)',('legacy','CLEAN','')); assert ready('legacy')
# Reset epoch is future-only: no pre-epoch evidence reuse.
c.execute('INSERT INTO alg VALUES(?,?,?)',('reset',9,9)); c.execute('INSERT INTO head VALUES(?,?,?)',('reset','RESET_CLEAN','202609140102')); assert not ready('reset')
c.execute("UPDATE head SET epoch_start_issue=? WHERE genome_id='reset'",(target,)); assert ready('reset')
# A supposedly-new genome with prior predraw evidence is not silently re-enrolled.
c.execute('INSERT INTO alg VALUES(?,?,?)',('suspicious',0,0)); c.execute('INSERT INTO predraw VALUES(?,?)',('suspicious','202609140000')); assert not ready('suspicious')

# Demonstrate the stale-recovery false positive that R950 removes. The immutable roster is
# complete, while a globally active but non-admitted asset has no predraw row. Old logic says
# pending; canonical roster recovery says no work.
c.executescript('''
CREATE TABLE jobs(target_issue TEXT,snapshot_blob BLOB,snapshot_hash TEXT);
CREATE TABLE draws(issue TEXT,dirty INTEGER);
CREATE TABLE plan(target_issue TEXT PRIMARY KEY);
CREATE TABLE task(target_issue TEXT,candidate_key TEXT,selected INTEGER);
CREATE TABLE capture(target_issue TEXT,candidate_key TEXT,frozen INTEGER,full1200 INTEGER);
CREATE TABLE formula(asset_id TEXT,tier TEXT,runtime_executable INTEGER);
CREATE TABLE shared_predraw(candidate_id TEXT,target_issue TEXT);
''')
h='a'*64
c.execute('INSERT INTO jobs VALUES(?,?,?)',(target,b'x',h)); c.execute('INSERT INTO plan VALUES(?)',(target,))
c.execute('INSERT INTO task VALUES(?,?,1)',(target,'formula:admitted'))
c.execute('INSERT INTO capture VALUES(?,?,1,1)',(target,'formula:admitted'))
c.execute("INSERT INTO formula VALUES('not_admitted','ACTIVE',1)")
old=c.execute("""SELECT j.target_issue FROM jobs j WHERE j.snapshot_blob IS NOT NULL
 AND NOT EXISTS(SELECT 1 FROM draws d WHERE d.issue=j.target_issue)
 AND EXISTS(SELECT 1 FROM formula a WHERE a.runtime_executable=1
   AND a.tier IN ('IDEA','NURSERY','ACTIVE','CHALLENGER','CERTIFIED_SHADOW')
   AND NOT EXISTS(SELECT 1 FROM shared_predraw p WHERE p.candidate_id=a.asset_id AND p.target_issue=j.target_issue))
 LIMIT 1""").fetchone()
canonical=c.execute("""SELECT j.target_issue FROM jobs j WHERE j.snapshot_blob IS NOT NULL
 AND length(j.snapshot_hash)=64 AND length(j.target_issue)=12
 AND NOT EXISTS(SELECT 1 FROM draws d WHERE d.dirty=0 AND d.issue>=j.target_issue)
 AND (NOT EXISTS(SELECT 1 FROM plan p WHERE p.target_issue=j.target_issue)
 OR EXISTS(SELECT 1 FROM task t WHERE t.target_issue=j.target_issue AND t.selected=1
   AND NOT EXISTS(SELECT 1 FROM capture e WHERE e.target_issue=t.target_issue
     AND e.candidate_key=t.candidate_key AND e.frozen=1 AND e.full1200=1)))
 LIMIT 1""").fetchone()
assert old and old[0]==target
assert canonical is None

result={
  'status':'PASS',
  'scope':'HOST_SQL_AND_SOURCE_WIRING_NOT_DEVICE_EFFECT',
  'fresh_untrained_freeze_ready':True,
  'legacy_unaudited_blocked':True,
  'reset_epoch_enforced':True,
  'prior_predraw_cannot_reenroll_as_new':True,
  'stale_global_recovery_false_positive_reproduced':True,
  'canonical_roster_recovery_false_positive_removed':True,
  'predraw_proof_audit_deferred':True,
  'settlement_proof_audit_deferred':True,
}
print('R950_RESEARCH_CONTINUITY_REGRESSION_PASS|'+json.dumps(result,ensure_ascii=False,sort_keys=True))
