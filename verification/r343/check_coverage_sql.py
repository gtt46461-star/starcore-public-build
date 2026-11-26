#!/usr/bin/env python3
"""Run the actual Java-generated receipt predicate against SQLite positive/negative fixtures."""
import argparse
import sqlite3
import subprocess
import json

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--classes',required=True)
args=p.parse_args()
predicate=subprocess.check_output(['java','-cp',args.classes,
    'com.ethk3.novaonepro.CoverageSettlementR343Test','sql'],text=True).strip()
db=sqlite3.connect(':memory:')
db.execute('CREATE TABLE d(issue TEXT PRIMARY KEY,open_time INTEGER)')
db.execute('CREATE TABLE t(target_issue TEXT PRIMARY KEY,completed_at INTEGER,settlement_hash TEXT)')
fields={'target_issue':'202611260100','game_code':'Binance1-PK10','base_issue':'202611260099',
        'position':1,'n1':1,'n2':2,'n3':3,'settlement_hash':'a'*64,'actual_number':2,
        'hit_label':1,'settled_at':1100}
db.execute('CREATE TABLE o('+','.join(k+(' INTEGER' if isinstance(v,int) else ' TEXT') for k,v in fields.items())+')')
label={k:v for k,v in fields.items() if k!='settlement_hash'}
label.update(record_hash=fields['settlement_hash'],strict_oos=1,pre_draw_frozen=1,
             prediction_created_at=900,draw_open_time=1000)
db.execute('CREATE TABLE oos('+','.join(k+(' INTEGER' if isinstance(v,int) else ' TEXT') for k,v in label.items())+')')
db.execute('INSERT INTO d VALUES(?,?)',('202611260100',1000))
db.execute('INSERT INTO t VALUES(?,?,?)',('202611260100',1101,'b'*64))
db.execute('INSERT INTO o VALUES('+','.join('?' for _ in fields)+')',list(fields.values()))
db.execute('INSERT INTO oos VALUES('+','.join('?' for _ in label)+')',list(label.values()))
query='SELECT '+predicate+' FROM d JOIN o ON o.target_issue=d.issue JOIN t ON t.target_issue=d.issue'
assert db.execute(query).fetchone()[0]==1
assert db.execute('SELECT o.settlement_hash=t.settlement_hash FROM o JOIN t ON o.target_issue=t.target_issue').fetchone()[0]==0
negatives={'target_issue':'202611260101','game_code':'OTHER','base_issue':'202611260098',
           'position':2,'n1':4,'n2':4,'n3':4,'record_hash':'c'*64,'actual_number':5,
           'hit_label':0,'strict_oos':0,'pre_draw_frozen':0,'prediction_created_at':1000,
           'draw_open_time':1001,'settled_at':1200}
for key,bad in negatives.items():
    db.execute('UPDATE oos SET '+key+'=?',(bad,))
    assert db.execute(query).fetchone()[0]==0,key
    db.execute('UPDATE oos SET '+key+'=?',(label[key],))
db.execute('UPDATE t SET completed_at=1099')
assert db.execute(query).fetchone()[0]==0
print(json.dumps({'status':'PASS','engine':'host sqlite3','positive':1,
                  'negative':len(negatives)+1,'old_cross_domain_equality_reproduced':True,
                  'android_production_database':'NOT_RUN'}))
