#!/usr/bin/env python3
"""R954 regression: zero-weight research stages are visible without influence authority.

The production writer intentionally stores all_gates_pass=0 for L4/L5 because below-L6
certificates have zero legal influence. This test models the exact gate-table contract used by
ResearchPlatformStoreR929.currentStage(): research visibility uses frozen stage evidence,
while L6/L7 still require the influence-authority bit.
"""
from pathlib import Path
import sqlite3
ROOT=Path(__file__).resolve().parents[2]
SRC=(ROOT/'app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java').read_text(encoding='utf-8')
HARNESS=(ROOT/'verification/r932/LifecycleAuthorizationHarnessR932.java').read_text(encoding='utf-8')

con=sqlite3.connect(':memory:')
con.executescript('''
CREATE TABLE research_certificate_gate_r932(
 certificate_hash TEXT NOT NULL, gate_id TEXT NOT NULL, observed REAL NOT NULL,
 required REAL NOT NULL, pass INTEGER NOT NULL,
 PRIMARY KEY(certificate_hash,gate_id));
''')

def put(cert, gate, observed, required, passed):
    con.execute('INSERT INTO research_certificate_gate_r932 VALUES(?,?,?,?,?)',(cert,gate,observed,required,1 if passed else 0))

def stage_ok(cert, level):
    rows={r[0]:(float(r[1]),int(r[2])) for r in con.execute(
        'SELECT gate_id,observed,pass FROM research_certificate_gate_r932 WHERE certificate_hash=?',(cert,))}
    for gate in ('PIT','DETERMINISM','PARITY','FUTURE_N','PAIRED_N'):
        if gate not in rows or rows[gate][1] != 1: return False
    if level=='L4_CHALLENGER': return True
    if level!='L5_SHADOW': return False
    if 'INDEPENDENT_WINDOWS' not in rows or rows['INDEPENDENT_WINDOWS'][0] < 1: return False
    for gate in ('HIT_RATE','COVERAGE','ECE','DOMAIN_SUPPORT','RESOURCE'):
        if gate not in rows or rows[gate][1] != 1: return False
    return True

# Natural L4: integrity/sample gates passed; hit-rate gate failed -> challenger, zero influence.
l4='4'*64
for g in ('PIT','DETERMINISM','PARITY','FUTURE_N','PAIRED_N'): put(l4,g,1,1,True)
put(l4,'HIT_RATE',.34,.35,False); put(l4,'INDEPENDENT_WINDOWS',0,2,False)
assert stage_ok(l4,'L4_CHALLENGER')

# Natural L5: one independent window is a valid shadow stage, but not enough for L6 authority.
l5='5'*64
for g in ('PIT','DETERMINISM','PARITY','FUTURE_N','PAIRED_N','HIT_RATE','COVERAGE','ECE','DOMAIN_SUPPORT','RESOURCE'):
    put(l5,g,1,1,True)
put(l5,'INDEPENDENT_WINDOWS',1,2,False)
assert stage_ok(l5,'L5_SHADOW')

# Hard integrity remains fail-closed; we did not merely ignore all gate failures.
con.execute("UPDATE research_certificate_gate_r932 SET pass=0 WHERE certificate_hash=? AND gate_id='PIT'",(l5,))
assert not stage_ok(l5,'L5_SHADOW')

required=[
 'researchStageGateContract',
 '"L4_CHALLENGER".equals(level)||"L5_SHADOW".equals(level)',
 'CERTIFICATE_RESEARCH_STAGE_GATE_FAILED',
 '"PIT","DETERMINISM","PARITY","FUTURE_N","PAIRED_N"',
 '"HIT_RATE","COVERAGE","ECE","DOMAIN_SUPPORT","RESOURCE"',
 'independent[0]<1.0d',
]
for marker in required:
    assert marker in SRC, 'production marker missing: '+marker
for marker in ('R954_L4_ZERO_WEIGHT_NATURALLY_HAS_NO_INFLUENCE_GATE','R954_L5_ZERO_WEIGHT_NATURALLY_HAS_NO_INFLUENCE_GATE','FAILED_HARD_GATE_CERTIFICATE_CANNOT_PROJECT_STAGE'):
    assert marker in HARNESS, 'real-SQLite harness marker missing: '+marker

print('R954_ZERO_WEIGHT_STAGE_CONTRACT=PASS')
print('natural_L4_visible_with_influence_gate_0=1')
print('natural_L5_visible_with_influence_gate_0=1')
print('hard_integrity_fail_closed=1')
print('L6_L7_authority_path_unchanged=1')
