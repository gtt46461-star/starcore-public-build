#!/usr/bin/env python3
import sqlite3, math, hashlib
DB=sqlite3.connect(':memory:')
DB.executescript('''
CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER);
CREATE TABLE miss_reason_sample(target_issue TEXT,created_at INTEGER,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL);
''')
def draw(issue,actual=7):
    nums=[1,2,3,4,5,6,7,8,9,10]; nums[0]=actual
    DB.execute('INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',(issue,0,*nums))
def miss(issue,actual=7,w=.1,p=.8,m=.1,t=.1,c=.7,created=1):
    DB.execute('INSERT INTO miss_reason_sample VALUES(?,?,?,?,?,?,?,?,?)',(issue,created,1,actual,w,p,m,t,c))
# 16 valid settled rows make RELATION dominant.
for i in range(16):
    issue=f'20260913{i:04d}'
    draw(issue,7); miss(issue,7, .08,.82,.10,.12,.76, i)
# Poisoned row: wrong actual number must be rejected despite huge TREND score.
draw('202609130100',7); miss('202609130100',9,1.0,0,0,0,0,999)
# Future row beyond cutoff must be rejected.
draw('202609139999',7); miss('202609139999',7,1.0,0,0,0,0,1000)
through='202609130015'
actual="CASE m.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4 WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7 WHEN 8 THEN d.n8 WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 ELSE 0 END"
sql=("SELECT m.target_issue,m.window_wrong_score,m.position_wrong_score,m.main_number_wrong_score,m.third_drag_score,m.counter_ignored_score "
     "FROM miss_reason_sample m JOIN draws d ON d.issue=m.target_issue AND d.dirty=0 "
     "WHERE m.target_issue<=? AND m.position BETWEEN 1 AND 10 AND m.actual_number BETWEEN 1 AND 10 AND m.actual_number=("+actual+") "
     "ORDER BY m.target_issue DESC,m.created_at DESC LIMIT ?")
rows=DB.execute(sql,(through,240)).fetchall()
assert len(rows)==16, len(rows)
trend=shape=relation=0.0; tn=sn=rn=0
cl=lambda x:max(0,min(1,float(x)))
for row in rows:
    w,p,m,t,c=map(cl,row[1:]); tv=w; sv=.62*m+.38*t; rv=.62*p+.38*c
    trend+=tv; shape+=sv; relation+=rv
    tn+=tv>=.15; sn+=sv>=.15; rn+=rv>=.15
means={'TREND':trend/len(rows),'SHAPE':shape/len(rows),'RELATION':relation/len(rows)}
module=max(means,key=means.get); support={'TREND':tn,'SHAPE':sn,'RELATION':rn}[module]
assert module=='RELATION', (module,means)
assert support>=8 and means[module]>=.20
source=open('app/src/main/java/com/ethk3/novaonepro/CausalResearchFeedbackR947.java',encoding='utf-8').read()
creation=open('app/src/main/java/com/ethk3/novaonepro/DirectedResearchCreationR932.java',encoding='utf-8').read()
assert 'm.actual_number=(' in source and 'd.dirty=0' in source and 'm.target_issue<=?' in source
assert 'diagnosticAssociationOnly=1|causalProof=0' in source
assert 'CausalResearchFeedbackR947.choose' in creation and 'directedSeed(seed,kind,module)' in creation
h=hashlib.sha256((module+str(len(rows))+str(means[module])).encode()).hexdigest()
print(f'R947_CAUSAL_FEEDBACK_SQL_PASS|rows={len(rows)}|module={module}|support={support}|score={means[module]:.4f}|fixture={h[:16]}')
