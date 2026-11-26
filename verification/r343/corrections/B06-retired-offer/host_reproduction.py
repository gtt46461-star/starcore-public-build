#!/usr/bin/env python3
"""Run exact Java SQL fragments against synthetic SQLite data. Android and field results stay separate."""
from pathlib import Path
import hashlib
import json
import re
import sqlite3
import sys

HERE = Path(__file__).resolve().parent
PACKAGE = Path('app/src/main/java/com/ethk3/novaonepro/r952')
NAME = 'RollingFrozenPublisherR343.java'
before = (HERE/'baseline'/PACKAGE/NAME).read_text()
after = (HERE/'followup'/PACKAGE/NAME).read_text()
V = 'research_rolling_version_r343'
O = 'research_rolling_offer_r343'
H = 'research_rolling_head_r343'
P = 'research_rolling_frozen_publication_r343'
R = 'research_rolling_publication_reject_r343'
T = 'v57_research_task_r967'
L = 'research_lifecycle_r932'
K = 'R343_ROLLING_FREEZE'

def expr(text, **extra):
    values = {'RollingLearnerR343.VERSION': V, 'RollingLearnerR343.OFFER': O,
              'RollingLearnerR343.HEAD': H, 'ResearchTaskQueueR967.TABLE': T,
              'ResearchLifecycleR932.TABLE': L, 'TABLE': P, 'REJECT': R, 'KIND': K,
              'TERMINAL': "('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED')", **extra}
    tokens = re.findall(r'"(?:[^"\\]|\\.)*"|[A-Za-z][\w.]*', text)
    return ''.join(json.loads(token) if token.startswith('"') else values[token] for token in tokens)

planner = expr(re.search(r'rawQuery\(("SELECT o.version_id,o.cutoff_issue.*?),new String\[\]',before).group(1))
pin_sqls = [expr(x) for x in re.findall(r'rawQuery\((.*?),new String\[\]',
                                        before.split('boolean pinsRawVersion(')[1].split('/** Compact')[0])]
supersede = expr(re.search(r'rawQuery\(("SELECT o.version_id,o.candidate_id,o.cutoff_issue.*?),version==',before).group(1),
                 filter=" AND NOT EXISTS(SELECT 1 FROM "+T+" t WHERE t.kind='"+K+"' AND t.authority_ref=o.version_id)")
fix = expr(re.search(r'String sql=("SELECT o.candidate_id,parent.state.*?);',after,re.S).group(1))
insert = expr(re.search(r'execSQL\(("INSERT INTO "\+REJECT\+" VALUES\(\?,\?,\?,\?,\?\)"),\s*new Object\[\]\{version,reason',after).group(1))
candidate = 'parent'
cutoff = '202611260200'

db = sqlite3.connect(':memory:')
db.executescript(f"""
CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY, structural_hash TEXT, state TEXT);
CREATE TABLE {V}(version_id TEXT PRIMARY KEY,candidate_id TEXT,state TEXT,ast_hash TEXT,
                 cutoff_issue TEXT,model_hash TEXT,receipt_hash TEXT,input_blob BLOB,checkpoint BLOB,protocol_hash TEXT,input_hash TEXT);
CREATE TABLE {H}(candidate_id TEXT PRIMARY KEY,version_id TEXT,through_issue TEXT,model_hash TEXT);
CREATE TABLE {O}(version_id TEXT PRIMARY KEY,candidate_id TEXT,cutoff_issue TEXT,model_hash TEXT,receipt_hash TEXT,created_at INT);
CREATE TABLE {P}(version_id TEXT PRIMARY KEY,frozen_id TEXT NOT NULL, proof BLOB NOT NULL);
CREATE TABLE {L}(candidate_key TEXT PRIMARY KEY, state TEXT NOT NULL);
CREATE TABLE {T}(task_id TEXT PRIMARY KEY,kind TEXT,authority_ref TEXT,status TEXT);
CREATE INDEX idx_r975_post_authority ON {T}(kind,authority_ref);
""")
db.execute(expr(re.search(r'execSQL\(("CREATE TABLE IF NOT EXISTS "\+REJECT.*?\)"),?\);',before).group(1)))
for line in before.splitlines():
    if 'r343_publication_reject_exclusive' in line or 'r343_publication_reject_no_replace' in line:
        db.execute(expr(line.strip().removeprefix('db.execSQL(').removesuffix(');')))

def seed(version, cutoff):
    db.execute(f"INSERT INTO {V} VALUES(?,?,'COMPLETE','ast',?,?,?,x'01020304',x'0506','protocol','input')",
               (version,candidate,cutoff,'model|'+version,'receipt|'+version))
    db.execute(f'INSERT INTO {O} VALUES(?,?,?,?,?,0)',(version,candidate,cutoff,'model|'+version,'receipt|'+version))

def pins(version):
    none = db.execute(pin_sqls[0],(version,version)).fetchone()
    if none and none[0] in {'FAILED','DEPENDENCY_GAP'}:
        return False
    if db.execute(pin_sqls[1],(version,)).fetchone():
        return False
    formal = db.execute(pin_sqls[2],(version,)).fetchone()
    return not formal or formal[1] is None or formal[1] not in {'QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED'}

db.execute("INSERT INTO r952_candidate VALUES(?,'ast','ACTIVE')",(candidate,))
seed('formal','202611260199')
db.execute(f"INSERT INTO {P} VALUES('formal','frozen-old',x'21')")
db.execute(f"INSERT INTO {L} VALUES('algorithm:frozen-old','INCUBATING')")
seed('orphan',cutoff)
db.execute(f"INSERT INTO {H} VALUES(?,'orphan',?,'model|orphan')",(candidate,cutoff))
assert db.execute(planner,(K,K)).fetchone()[0]=='orphan'
db.execute("UPDATE r952_candidate SET state='RESOURCE_RETIRED' WHERE candidate_id=?",(candidate,))
assert db.execute(planner,(K,K)).fetchone() is None
assert db.execute(supersede).fetchone() is None
assert pins('orphan')
print('BASELINE_REPRODUCED|RETIRED_PARENT_UNPLANNABLE|NO_LATER_HEAD|RAW_PIN_STUCK')

seed('queued','202611260201')
db.execute(f"INSERT INTO {T} VALUES('task',?,'queued','WAIT_INPUT')",(K,))
formal_before = tuple(db.execute(f"SELECT * FROM {P} WHERE version_id='formal'").fetchone())
assert db.execute(fix,('formal',K)).fetchone() is None
assert db.execute(fix,('queued',K)).fetchone() is None
assert db.execute(fix,('orphan',K)).fetchone()==(candidate,'RESOURCE_RETIRED',cutoff,'model|orphan','receipt|orphan')
plan=[row[3] for row in db.execute('EXPLAIN QUERY PLAN '+fix,('orphan',K))]
assert any('SEARCH t USING COVERING INDEX idx_r975_post_authority' in row for row in plan), plan
assert not any('SCAN ' in row for row in plan), plan
db.execute('SAVEPOINT rejected')
reason='PARENT_RETIRED_UNPUBLISHED'
fixture_receipt=json.dumps({'reason':reason,'scope':'SQL_CONTRACT_FIXTURE'},sort_keys=True,separators=(',',':'))
params=('orphan',reason,fixture_receipt,hashlib.sha256(fixture_receipt.encode()).hexdigest(),190)
db.execute(insert,params)
assert not pins('orphan')
db.execute('ROLLBACK TO rejected')
assert pins('orphan')
db.execute(insert,params)
assert not pins('orphan')
assert db.execute(fix,('orphan',K)).fetchone() is None
assert pins('formal')
assert pins('queued')
assert formal_before==tuple(db.execute(f"SELECT * FROM {P} WHERE version_id='formal'").fetchone())
assert db.execute(f"SELECT through_issue FROM {H} WHERE version_id='orphan'").fetchone()==(cutoff,)
assert db.execute(f"SELECT status FROM {T} WHERE authority_ref='queued'").fetchone()==('WAIT_INPUT',)
print(json.dumps({'status':'PASS','scope':'HOST_SOURCE_SQL_SYNTHETIC','sqlite':sqlite3.sqlite_version,
                  'plan':plan,'formalBytesUnchanged':True,'queuedPinUnchanged':True,'rollbackRemovesRejection':True,
                  'rawPinAfterReceipt':False,'noUnpublishedReselection':True},indent=2))
