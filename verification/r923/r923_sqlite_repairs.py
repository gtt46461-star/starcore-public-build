#!/usr/bin/env python3
"""SQLite fault models; these are synthetic fixtures, not Android or live win-rate proof."""
from pathlib import Path
import datetime as dt
import hashlib
import json
import random
import re
import sqlite3

JAVA=Path(__file__).resolve().parents[2]/'app/src/main/java/com/ethk3/novaonepro'
LITERAL=r'"(?:[^"\\]|\\.)*"'
def source(n):return (JAVA/(n+'.java')).read_text()
def constants(n):return {k:json.loads(v) for k,v in re.findall(r'(?:public |private )?static final String\s+(\w+)\s*=\s*('+LITERAL+r')\s*;',source(n))}
def concat(expr,names):return ''.join(json.loads(t) if t.startswith('"') else names[t] for t in re.findall(LITERAL+r'|[\w.]+',expr))

def query_contracts():
    db=sqlite3.connect(':memory:');names=constants('AssetGenesisEngineR915')
    for expr in re.findall(r'db\.execSQL\((.*?)\);',source('AssetGenesisEngineR915'),re.S):
        if expr.lstrip().startswith('"CREATE TABLE IF NOT EXISTS "'):db.execute(concat(expr,names))
    for literal in re.findall(LITERAL,source('DbHelper')):
        sql=json.loads(literal)
        if sql.startswith(('CREATE TABLE IF NOT EXISTS v57_research_asset_r906 ','CREATE TABLE IF NOT EXISTS v57_algorithm_genome_r908 ')):db.execute(sql)
    names={**{'DbHelper.'+k:v for k,v in constants('DbHelper').items()},**{'AssetGenesisEngineR915.'+k:v for k,v in names.items()},**{'LegacyPredrawContextBridgeR915.'+k:v for k,v in constants('LegacyPredrawContextBridgeR915').items()}}
    pairs=re.findall(r'appendTable\(out, db, ([\w.]+), "(\w+)"\)',source('FullResearchInventoryR923'))
    assert len(pairs)==7
    for table,key in pairs:assert db.execute('SELECT * FROM '+names[table]+' ORDER BY '+key).description
    text=source('EssentialSettlementFeedbackR923')
    expr=text.split('c = store.readable().rawQuery(',1)[1].split('new String[]',1)[0].rstrip().rstrip(',')
    query=concat(expr,names)
    for key,cols in [('DbHelper.TABLE_R36_SETTLEMENT_TASK','target_issue TEXT,status TEXT,completed_at INTEGER'),('DbHelper.TABLE_DRAWS','issue TEXT,dirty INTEGER'),('DbHelper.TABLE_R35_SHADOW_OBSERVATION','target_issue TEXT,pre_draw_frozen INTEGER,settled INTEGER,hit_label INTEGER'),('LegacyPredrawContextBridgeR915.POST_TABLE','target_issue TEXT')]:db.execute('CREATE TABLE '+names[key]+'('+cols+')')
    for i in range(1,65):
        target=f'20260905{i:04d}'
        db.execute('INSERT INTO '+names['DbHelper.TABLE_R36_SETTLEMENT_TASK']+' VALUES(?,?,?)',(target,'COMPLETED',i))
        db.execute('INSERT INTO '+names['DbHelper.TABLE_DRAWS']+' VALUES(?,0)',(target,))
        if i<=20:db.execute('INSERT INTO '+names['DbHelper.TABLE_R35_SHADOW_OBSERVATION']+' VALUES(?,1,1,0)',(target,))
    rows=db.execute(query,('COMPLETED','16')).fetchall()
    assert len(rows)==16 and rows[0][0]=='202609050020'
    print('R923_PRODUCTION_SQL=PASS|INVENTORY_TABLES=7|UNREADY_NEWEST=44|READY_NOT_STARVED=16')

def queue():
    assert 'ORDER BY core_materialized DESC,' in source('DeferredEvidenceWriterR36')
    assert 'next_action_ordinal DESC,target_issue DESC,' in source('DeferredEvidenceWriterR36')
    db=sqlite3.connect(':memory:');db.execute('CREATE TABLE jobs(target_issue INTEGER PRIMARY KEY,core_materialized INTEGER,next_action_ordinal INTEGER)')
    db.executemany('INSERT INTO jobs VALUES(?,0,0)',[(i,) for i in range(1,721)])
    done=set()
    def pulse():
        t,c,o=db.execute('SELECT * FROM jobs ORDER BY core_materialized DESC,next_action_ordinal DESC,target_issue DESC LIMIT 1').fetchone()
        if c and o+400>=1200:db.execute('DELETE FROM jobs WHERE target_issue=?',(t,));done.add(t)
        else:db.execute('UPDATE jobs SET core_materialized=1,next_action_ordinal=? WHERE target_issue=?',(o+400 if c else 0,t))
    for i in range(1,721):
        if i%5==0:db.execute('INSERT INTO jobs VALUES(?,0,0)',(720+i//5,))
        pulse()
    assert any(t<=720 for t in done)
    while db.execute('SELECT count(*) FROM jobs').fetchone()[0]:pulse()
    assert done==set(range(1,865))
    print('R923_SQLITE_QUEUE_MODEL=PASS|OLD=720/720|NEW=144/144')

def replay(seed):
    rng=random.Random(seed);db=sqlite3.connect(':memory:')
    db.executescript('CREATE TABLE draws(issue TEXT PRIMARY KEY,actual INTEGER,opened INTEGER); CREATE TABLE frozen(issue TEXT PRIMARY KEY,k1 INTEGER,k2 INTEGER,k3 INTEGER,created INTEGER,trained TEXT,CHECK(k1<>k2 AND k1<>k3 AND k2<>k3)); CREATE TABLE labels(issue TEXT PRIMARY KEY,hit INTEGER CHECK(hit IN(0,1)),hash TEXT); CREATE TABLE feedback(issue TEXT PRIMARY KEY,updates INTEGER CHECK(updates=1));')
    missing=set(rng.sample(range(4320),300));faults=set(rng.sample(sorted(set(range(4320))-missing),90))
    start=dt.datetime(2026,9,2,tzinfo=dt.timezone.utc)
    def issue(i):
        t=start+dt.timedelta(minutes=i);return t.strftime('%Y%m%d')+f'{t.hour*60+t.minute+1:04d}'
    for i in range(4320):
        target=issue(i)
        if i not in missing:db.execute('INSERT INTO frozen VALUES(?,?,?,?,?,?)',(target,*rng.sample(range(1,11),3),i*60000+1,issue(i-1)))
        actual=rng.randrange(1,11);db.execute('INSERT INTO draws VALUES(?,?,?)',(target,actual,i*60000+59000))
        row=db.execute('SELECT k1,k2,k3,created,trained FROM frozen WHERE issue=?',(target,)).fetchone()
        if row is None:continue
        assert row[3]<i*60000+59000 and row[4]<target
        hit=int(actual in row[:3]);digest=hashlib.sha256(f'{target}|{row}|{actual}|{hit}'.encode()).hexdigest()
        for repeat in range(3):db.execute('INSERT OR IGNORE INTO labels VALUES(?,?,?)',(target,hit,digest))
        if i not in faults:db.execute('INSERT OR IGNORE INTO feedback VALUES(?,1)',(target,))
    assert db.execute('SELECT count(*) FROM labels').fetchone()[0]==4020
    assert db.execute('SELECT count(*) FROM feedback').fetchone()[0]==3930
    db.execute('INSERT OR IGNORE INTO feedback SELECT issue,1 FROM labels')
    assert db.execute('SELECT sum(updates) FROM feedback').fetchone()[0]==4020
    assert db.execute('SELECT count(*) FROM draws d LEFT JOIN frozen f ON f.issue=d.issue WHERE f.issue IS NULL').fetchone()[0]==300
    print(f'R923_THREE_DAY_SYNTHETIC=PASS|SEED={seed}|DRAWS=4320|MISSING_NOT_FABRICATED=300|DUPLICATE_LABELS=0|FAULTS=90|FEEDBACK_CATCHUP=4020')

if __name__=='__main__':
    query_contracts();queue()
    for seed in (7,43,923):replay(seed)
    print('R923_SQLITE_REPAIRS=PASS|PHYSICAL_DEVICE=NOT_TESTED|LIVE_WIN_RATE=NOT_PROVEN')
