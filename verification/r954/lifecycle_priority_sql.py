#!/usr/bin/env python3
"""R954 real-SQLite regression for certificate projection scheduling.

This exercises the exact relational invariant introduced in ResearchLifecycleR932:
a freshly updated ACTIVE certificate must be reviewed before ordinary old backlog,
then stop receiving priority after reviewed_at catches up.  It also guards the
user-visible CHALLENGER aggregate and pending-projection diagnostic wiring.
"""
from pathlib import Path
import sqlite3, re, sys

ROOT=Path(__file__).resolve().parents[2]
LIFE=ROOT/'app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java'
ALG=ROOT/'app/src/main/java/com/ethk3/novaonepro/AlgorithmEvolutionLifecycleR912.java'
STATUS=ROOT/'app/src/main/java/com/ethk3/novaonepro/ResearchPipelineStatusR941.java'

PRIORITY_SQL="""
SELECT l.candidate_key
FROM research_lifecycle_r932 l
JOIN research_certificate_r929 c
  ON c.candidate_id=substr(l.candidate_key,instr(l.candidate_key,':')+1)
 AND c.structural_hash=l.structural_hash
 AND c.consumer=CASE
      WHEN l.candidate_key LIKE 'algorithm:%' THEN 'INVENTED_ALGORITHM'
      WHEN l.candidate_key LIKE 'genesis:%' THEN 'GENESIS_ASSET'
      WHEN l.candidate_key LIKE 'formula:%' THEN 'FORMULA_GRAPH'
      ELSE '' END
WHERE c.state='ACTIVE'
  AND c.updated_at>l.reviewed_at
  AND l.state NOT IN ('QUALITY_PRUNED','QUARANTINED')
ORDER BY c.updated_at DESC,l.reviewed_at,l.candidate_key
LIMIT 4
"""

con=sqlite3.connect(':memory:')
con.executescript('''
CREATE TABLE research_lifecycle_r932(
 candidate_key TEXT PRIMARY KEY, structural_hash TEXT NOT NULL, state TEXT NOT NULL,
 reason TEXT NOT NULL, first_prune_reason TEXT NOT NULL, future_n INTEGER NOT NULL,
 paired_n INTEGER NOT NULL, evidence_hash TEXT NOT NULL, last_issue TEXT NOT NULL,
 reviewed_at INTEGER NOT NULL);
CREATE TABLE research_certificate_r929(
 candidate_key TEXT PRIMARY KEY, candidate_id TEXT NOT NULL, structural_hash TEXT NOT NULL,
 consumer TEXT NOT NULL, state TEXT NOT NULL, level TEXT NOT NULL, updated_at INTEGER NOT NULL);
''')

def life(key, structure, reviewed, state='OBSERVING'):
    con.execute('INSERT INTO research_lifecycle_r932 VALUES(?,?,?,?,?,?,?,?,?,?)',
                (key,structure,state,'fixture','',0,0,'e'*64,'202609100001',reviewed))

for i in range(8):
    life(f'algorithm:r954-old-{i}', f'old-hash-{i}', 0)

target='algorithm:r954-target'; structure='target-hash'
life(target,structure,90)
con.execute('INSERT INTO research_certificate_r929 VALUES(?,?,?,?,?,?,?)',
            ('cert-row','r954-target',structure,'INVENTED_ALGORITHM','ACTIVE','L4_CHALLENGER',100))

priority=[r[0] for r in con.execute(PRIORITY_SQL)]
assert priority and priority[0]==target, ('fresh certificate did not win priority',priority)

# A terminal lifecycle row must never be pulled back into automatic review priority.
con.execute("UPDATE research_lifecycle_r932 SET state='QUALITY_PRUNED',reviewed_at=90 WHERE candidate_key=?",(target,))
assert target not in [r[0] for r in con.execute(PRIORITY_SQL)], 'terminal row re-entered priority queue'
con.execute("UPDATE research_lifecycle_r932 SET state='OBSERVING',reviewed_at=110 WHERE candidate_key=?",(target,))
assert target not in [r[0] for r in con.execute(PRIORITY_SQL)], 'already-reviewed certificate monopolizes queue'

# A later certificate publication must re-open exactly one scheduling opportunity.
con.execute("UPDATE research_certificate_r929 SET updated_at=120 WHERE candidate_id='r954-target'")
assert [r[0] for r in con.execute(PRIORITY_SQL)][0]==target, 'later certificate update not detected'
con.execute("UPDATE research_lifecycle_r932 SET reviewed_at=121 WHERE candidate_key=?",(target,))
assert target not in [r[0] for r in con.execute(PRIORITY_SQL)], 'priority did not self-clear after review'

# Consumer mapping is deliberately closed: unsupported prefixes cannot inherit influence.
life('tiny:r954-target','tiny-hash',0)
con.execute('INSERT INTO research_certificate_r929 VALUES(?,?,?,?,?,?,?)',
            ('tiny-cert','r954-target','tiny-hash','INVENTED_ALGORITHM','ACTIVE','L4_CHALLENGER',200))
assert 'tiny:r954-target' not in [r[0] for r in con.execute(PRIORITY_SQL)], 'unsupported consumer prefix joined certificate queue'

life_src=LIFE.read_text(encoding='utf-8')
alg_src=ALG.read_text(encoding='utf-8')
status_src=STATUS.read_text(encoding='utf-8')
required_life=[
    "c.updated_at>l.reviewed_at",
    "l.state NOT IN ('QUALITY_PRUNED','QUARANTINED')",
    "ORDER BY c.updated_at DESC,l.reviewed_at,l.candidate_key LIMIT 4",
    "LinkedHashSet<String> selected",
]
for marker in required_life:
    assert marker in life_src, 'production lifecycle marker missing: '+marker
assert 'state.equals("CHALLENGER")||state.equals("SHADOW")' in alg_src, 'CHALLENGER omitted from lifecycle aggregate'
assert 'challenger_or_later=' in alg_src, 'aggregate diagnostic label did not change'
assert 'certificateProjectionPending' in status_src and '待生命周期投影=' in status_src, 'pending projection diagnostic missing'

print('R954_LIFECYCLE_PRIORITY_SQL=PASS')
print('fresh_certificate_first=1')
print('terminal_fail_closed=1')
print('priority_self_clears=1')
print('later_certificate_reopens=1')
print('unsupported_prefix_fail_closed=1')
print('challenger_aggregate_includes_CHALLENGER=1')
print('projection_pending_diagnostic=1')
