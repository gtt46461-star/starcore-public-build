"""Compare deployed and repaired statements with field-sized ledgers."""
import json
import re
import sqlite3
import subprocess
import time
from pathlib import Path

root=Path("app/src/main/java/com/ethk3/novaonepro")
parent="5c7f1b36eaee6036df280e85835bb0fb4ce0b40d"
tables={
    "RuntimeEventLedgerR926.TABLE_EVENT":"event", "RuntimeObservationScopeR341.EVENT_SCOPE":"scope",
    "DbHelper.TABLE_DRAWS":"draws", "DbHelper.TABLE_R35_SHADOW_OBSERVATION":"shadow",
    "DbHelper.TABLE_R36_SETTLEMENT_TASK":"task", "DbHelper.TABLE_R35_OOS_LABEL":"label",
    "EVENTS":"event", "events":"event",
}
def source(name, original=False):
    path=root/(name+".java")
    return subprocess.check_output(["git","show",parent+":"+str(path)],text=True) if original else path.read_text()

def sql(fragment):
    parts=re.findall(r'"(?:\\.|[^"\\])*"|[A-Za-z_][A-Za-z_.0-9]*',fragment)
    return "".join(json.loads(t) if t.startswith('"') else tables[t] for t in parts)

def statements(original):
    health=source("RuntimeHealthLedgerR36",original)
    truth=source("RuntimeTruthSnapshotR906",original)
    attempt=source("RuntimeAttemptObservabilityR926",original)
    return [
        sql(re.search(r"String scope = (.*?);",health,re.S)[1]),
        sql(re.search(r"String COVERAGE_R341 = (.*?);",truth,re.S)[1]),
        sql(re.search(r'Cursor c = db\.rawQuery\((.*?),\s*new String',attempt[attempt.index("static String acceptanceWindow(DbStore"):],re.S)[1]
            if original else re.search(r"String ACCEPTANCE_WINDOW = (.*?);",attempt,re.S)[1]),
    ]

db=sqlite3.connect(":memory:")
db.executescript("""
CREATE TABLE event (seq INTEGER PRIMARY KEY,event_id TEXT UNIQUE,game_code TEXT,target_issue TEXT,attempt_id TEXT,event_type TEXT,created_at INTEGER);
CREATE INDEX idx_r926_event_issue ON event(game_code,target_issue,seq);
CREATE INDEX idx_r926_event_attempt ON event(attempt_id,seq);
CREATE TABLE scope (event_id TEXT PRIMARY KEY,release_id TEXT,source_commit TEXT,database_identity TEXT,sample_wall_at INTEGER);
CREATE INDEX r341_observation_release ON scope(release_id,source_commit,database_identity,sample_wall_at);
CREATE TABLE draws (_id INTEGER PRIMARY KEY,issue TEXT UNIQUE,dirty INTEGER);
CREATE TABLE shadow (target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,settled INTEGER);
CREATE TABLE task (target_issue TEXT PRIMARY KEY,status TEXT);
CREATE TABLE label (target_issue TEXT PRIMARY KEY,strict_oos INTEGER);
CREATE TABLE v57_attempt_resource_r927 (attempt_id TEXT,status TEXT,started_at INTEGER,process_id TEXT,release_code INTEGER);
CREATE INDEX idx_r927_resource_attempt ON v57_attempt_resource_r927(attempt_id,started_at);
CREATE INDEX idx_r928_resource_status ON v57_attempt_resource_r927(status,process_id);
CREATE INDEX idx_r928_resource_release_time ON v57_attempt_resource_r927(release_code,started_at DESC);
CREATE TABLE v57_prediction_timing_r925 (attempt_id TEXT,sample_class TEXT,release_code INTEGER);
CREATE INDEX idx_fixture_timing_attempt ON v57_prediction_timing_r925(attempt_id,release_code);

WITH RECURSIVE i(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM i WHERE n<1220000)
INSERT INTO draws SELECT n,printf('%012d',n), n%17=0 FROM i;
WITH RECURSIVE i(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM i WHERE n<320000)
INSERT INTO event SELECT n, 'e'||n, CASE WHEN n%21=0 THEN 'other' ELSE 'g' END,printf('%012d',n),
    CASE WHEN n%37=0 THEN '' ELSE 'a'||(n/3) END,
    CASE n%4 WHEN 0 THEN 'REQUESTED' WHEN 1 THEN 'CANONICAL' WHEN 2 THEN 'REJECTED' ELSE 'STARTED' END,n FROM i;
INSERT INTO event VALUES(320001,'malformed','g','bad','z','CANONICAL',320001);
INSERT INTO event VALUES(320002,'other-same','other','000000000001','a106665','REQUESTED',320002);
INSERT INTO event VALUES(320003,'before-epoch','g','000000000002','a106665','REQUESTED',12);
INSERT INTO scope SELECT event_id, 'old','older','db',created_at FROM event WHERE seq<319850;
INSERT INTO scope SELECT event_id, 'now','new','db',created_at FROM event WHERE seq>=319850;
INSERT INTO shadow SELECT issue, _id%23!=0,_id%7!=0 FROM draws WHERE _id BETWEEN 1210000 AND 1219500;
INSERT INTO shadow VALUES('000001220001',1,1);
INSERT INTO label SELECT target_issue,CAST(target_issue AS INTEGER)%5!=0 FROM shadow;
INSERT INTO task SELECT target_issue, CASE CAST(target_issue AS INTEGER)%5 WHEN 0 THEN 'COMPLETED' WHEN 1 THEN 'NO_PREDRAW_CANONICAL_TERMINAL' WHEN 2 THEN 'FAILED_TERMINAL' WHEN 3 THEN 'INVALID_DRAW_TERMINAL' ELSE 'PENDING' END FROM shadow;
INSERT INTO v57_attempt_resource_r927 SELECT attempt_id,CASE event_type WHEN 'CANONICAL' THEN 'CANONICAL_COMMITTED' ELSE 'CANCELLED' END,created_at,'process',1 FROM event WHERE seq%4=1;
INSERT INTO v57_prediction_timing_r925 SELECT DISTINCT attempt_id,'WARM_MONOTONIC',42 FROM event WHERE seq>315000 AND seq%3=0;
INSERT INTO v57_prediction_timing_r925 VALUES('a106665','COLD',42);
INSERT INTO v57_prediction_timing_r925 VALUES('a106662','WARM_MONOTONIC',41);
""")
old=statements(True)
new=statements(False)
for i, options in enumerate(([("now","new","db","g"), ("missing","new","db","g"), ("old","older","db","g")],
                             [()],
                             [("g",315000,42), ("g",320003,42), ("other",315000,42)])):
    for args in options:
        results=[]
        for shape,statement in (("original",old[i]),("repair",new[i])):
            started=time.monotonic()
            rows=db.execute(statement,args).fetchall()
            results.append(rows)
            print(f"SQL {i} {shape} args={args} rows={len(rows)} elapsed={(time.monotonic()-started)*1000:.2f}ms",flush=True)
            if shape=="repair" and args==options[0]:
                print("PLAN",*[x[3] for x in db.execute("EXPLAIN QUERY PLAN "+statement,args)],sep=" | ",flush=True)
        assert sorted(results[0],key=repr)==sorted(results[1],key=repr), (i,args)
    if i==1:
        db.execute("SAVEPOINT empty")
        db.execute("DELETE FROM draws")
        assert db.execute(old[i]).fetchall()==db.execute(new[i]).fetchall()
        db.execute("ROLLBACK TO empty")
print("All production-query results match.")
