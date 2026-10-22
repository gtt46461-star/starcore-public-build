#!/usr/bin/env python3
import sqlite3
c=sqlite3.connect(':memory:')
c.executescript('''
CREATE TABLE alg(genome_id TEXT,structural_hash TEXT,status TEXT,earliest_use_issue TEXT,trained_until_issue TEXT);
CREATE TABLE gen(asset_id TEXT,structural_hash TEXT,status TEXT,may_authorize_real INTEGER,earliest_use_issue TEXT,trained_until_issue TEXT);
CREATE TABLE formula(asset_id TEXT,artifact_hash TEXT,tier TEXT,runtime_executable INTEGER,may_authorize_real INTEGER,earliest_use_issue TEXT,trained_until_issue TEXT);
''')
latest='202609130100'; nxt='202609130101'
c.execute('INSERT INTO alg VALUES(?,?,?,?,?)',('a','a'*64,'IDEA_WAIT_FUTURE_OOS',nxt,latest))
c.execute('INSERT INTO gen VALUES(?,?,?,?,?,?)',('g','b'*64,'IDEA_WAIT_FUTURE_OOS',0,nxt,latest))
c.execute('INSERT INTO formula VALUES(?,?,?,?,?,?,?)',('f','c'*64,'NURSERY',1,0,nxt,latest))
qa="SELECT COUNT(*) FROM alg WHERE status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS','PAUSED_CAPACITY_R912') AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?)"
qg="SELECT COUNT(*) FROM gen WHERE status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CERTIFIED_FUTURE_OOS') AND may_authorize_real=0 AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?)"
qf="SELECT COUNT(*) FROM formula WHERE runtime_executable=1 AND may_authorize_real=0 AND tier IN ('IDEA','NURSERY','ACTIVE','CHALLENGER','CERTIFIED_SHADOW') AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?)"
assert c.execute(qa,(latest,latest)).fetchone()[0]==0
assert c.execute(qg,(latest,latest)).fetchone()[0]==0
assert c.execute(qf,(latest,latest)).fetchone()[0]==0
assert c.execute(qa,(nxt,nxt)).fetchone()[0]==1
assert c.execute(qg,(nxt,nxt)).fetchone()[0]==1
assert c.execute(qf,(nxt,nxt)).fetchone()[0]==1
src=open('app/src/main/java/com/ethk3/novaonepro/StagedResearchRosterR943.java',encoding='utf8').read()
for token in ["earliest_use_issue<=?","trained_until_issue<?","IDEA_WAIT_FUTURE_OOS","runtime_executable=1"]: assert token in src
svc=open('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java',encoding='utf8').read()
assert 'ResearchEvolutionHeartbeatR947.pulse(store, 600L)' in svc
assert 'R947_PER_DRAW_EVOLUTION:' in svc
print('R947_NEXT_ROSTER_AND_SCHEDULER_PASS|algorithm=1|genesis=1|formula=1|sameIssue=0')
