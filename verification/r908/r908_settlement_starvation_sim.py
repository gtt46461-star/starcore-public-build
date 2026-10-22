#!/usr/bin/env python3
import sqlite3, pathlib
N_READY=192; N_NO_PREDRAW=1871
now=10_000_000
con=sqlite3.connect(':memory:')
con.executescript('''
CREATE TABLE task(target_issue TEXT PRIMARY KEY,status TEXT,task_class TEXT,priority INTEGER,next_attempt_at INTEGER,lease_until INTEGER,created_at INTEGER);
CREATE TABLE shadow(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,settled INTEGER);
CREATE TABLE draw(issue TEXT PRIMARY KEY,dirty INTEGER);
''')
for i in range(N_NO_PREDRAW):
    issue=f'NP{i:05d}'
    con.execute('INSERT INTO task VALUES(?,?,?,?,?,?,?)',(issue,'NO_PREDRAW_CANONICAL_TERMINAL','BACKLOG',0,0,0,i))
for i in range(N_READY):
    issue=f'R{i:05d}'
    cls='LIVE_RETRY' if i < 160 else 'BACKLOG'
    pri=50 if cls=='LIVE_RETRY' else 0
    con.execute('INSERT INTO task VALUES(?,?,?,?,?,?,?)',(issue,'RETRY_WAIT',cls,pri,0,0,100000+i))
    con.execute('INSERT INTO shadow VALUES(?,?,?)',(issue,1,0))
    con.execute('INSERT INTO draw VALUES(?,0)',(issue,))
cur='CURRENT_EXACT'
con.execute('INSERT INTO task VALUES(?,?,?,?,?,?,?)',(cur,'PENDING','LIVE_CURRENT',100,0,0,999999))
con.execute('INSERT INTO shadow VALUES(?,?,?)',(cur,1,0))
con.execute('INSERT INTO draw VALUES(?,0)',(cur,))
q="""SELECT t.target_issue FROM task t JOIN shadow o ON o.target_issue=t.target_issue JOIN draw d ON d.issue=t.target_issue AND d.dirty=0 WHERE o.pre_draw_frozen=1 AND o.settled=0 AND ((t.status IN ('PENDING','RETRY_WAIT') AND t.next_attempt_at<=?) OR (t.status='RUNNING' AND t.lease_until<?)) ORDER BY t.priority DESC,t.next_attempt_at ASC,t.created_at ASC,t.target_issue ASC LIMIT 1"""
first=con.execute(q,(now,now)).fetchone()[0]
assert first==cur, first
con.execute("UPDATE shadow SET settled=1 WHERE target_issue=?",(cur,))
con.execute("UPDATE task SET status='COMPLETED' WHERE target_issue=?",(cur,))
selected=[]
for _ in range(32):
    row=con.execute(q,(now,now)).fetchone()
    if not row: break
    x=row[0]; selected.append(x)
    assert not x.startswith('NP')
    con.execute('UPDATE shadow SET settled=1 WHERE target_issue=?',(x,))
    con.execute("UPDATE task SET status='COMPLETED' WHERE target_issue=?",(x,))
remaining=con.execute("SELECT COUNT(*) FROM shadow WHERE pre_draw_frozen=1 AND settled=0").fetchone()[0]
np_unchanged=con.execute("SELECT COUNT(*) FROM task WHERE status='NO_PREDRAW_CANONICAL_TERMINAL'").fetchone()[0]
assert len(selected)==32
assert remaining==N_READY-32
assert np_unchanged==N_NO_PREDRAW
# Simulate subsequent correctness pulses while Research is assumed continuously backlogged.
# Research cannot consume this queue or change task priority, so all frozen ready rows must converge.
pulses=1
while remaining>0 and pulses<20:
    pulses+=1
    for _ in range(32):
        row=con.execute(q,(now,now)).fetchone()
        if not row: break
        x=row[0]
        assert not x.startswith('NP')
        con.execute('UPDATE shadow SET settled=1 WHERE target_issue=?',(x,))
        con.execute("UPDATE task SET status='COMPLETED' WHERE target_issue=?",(x,))
    remaining=con.execute("SELECT COUNT(*) FROM shadow WHERE pre_draw_frozen=1 AND settled=0").fetchone()[0]
assert remaining==0, remaining
assert pulses==6, pulses
root=pathlib.Path(__file__).resolve().parents[2]
s=(root/'app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java').read_text()
a=s.index('drainReadyPredraw'); b=s.index('reopenRecoverableNoPredrawTerminals'); c=s.index('seedHistorical'); d=s.index('retireLegacyBacklogWithoutPredraw')
assert b < a < c < d
svc=(root/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text()
assert 'postAtFrontOfQueue(task)' in svc
assert 'R908_SETTLEMENT_PULSE_MS = 1200L' in svc
assert 'R908_RESEARCH_SELF_PULSE_MS = 15000L' in svc
assert 'R912_CURRENT_EXACT_PRIORITY_WITH_BACKLOG_FAIRNESS' in svc
assert 'if (!currentExactSettlementPendingR908.get())' not in svc
chunk=svc[svc.index('private void scheduleStartupMaintenanceR3611'):svc.index('private void runSettlementBacklogMaintenanceR3611')]
assert 'settlementRecoveryHandlerR908 != null' in chunk
print('R908_SETTLEMENT_STARVATION_SIM=PASS')
print(f'FIRST={first}')
print(f'READY_DRAINED_PER_PULSE={len(selected)}')
print(f'READY_REMAINING_AFTER_ALL_PULSES={remaining}')
print(f'PULSES_TO_DRAIN_192={pulses}')
print(f'NO_PREDRAW_UNTOUCHED={np_unchanged}')
print('ORDER=CURRENT_EXACT_PRIORITY>FALSE_TERMINAL_REOPEN>READY_PREDRAW>HISTORICAL_SEED>LEGACY_RETIRE')
print('R912_CURRENT_RETRY_DOES_NOT_GLOBAL_LOCK_BACKLOG=PASS')
print('BACKGROUND_LOOPS=SETTLEMENT_1200MS|RESEARCH_15000MS')
