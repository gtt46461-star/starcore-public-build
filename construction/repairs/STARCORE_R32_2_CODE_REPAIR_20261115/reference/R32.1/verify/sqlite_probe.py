"""Replays minimal legacy fixtures on SQLite using declarations emitted by actual patched product Java."""
import json, sqlite3, sys
from pathlib import Path
def run(ddl_path):
 db=sqlite3.connect(':memory:')
 db.execute('CREATE TABLE v20_link_candidate_snapshot(_id INTEGER PRIMARY KEY AUTOINCREMENT, snapshot_key TEXT NOT NULL UNIQUE)')
 db.execute("INSERT INTO v20_link_candidate_snapshot(snapshot_key) VALUES('old')")
 try:db.execute('CREATE UNIQUE INDEX idx_store_v20_candidate_unique ON v20_link_candidate_snapshot(target_issue,candidate_key)')
 except sqlite3.OperationalError as err: assert 'no such column' in str(err)
 else:raise AssertionError('032 incomplete table must reproduce missing column')
 for _ in range(2):
  with db:
   for spec in ['target_issue TEXT','candidate_rank INTEGER DEFAULT 0','candidate_key TEXT']:
    if spec.split()[0] not in {x[1] for x in db.execute('PRAGMA table_info(v20_link_candidate_snapshot)')}:
     db.execute('ALTER TABLE v20_link_candidate_snapshot ADD COLUMN '+spec)
   db.execute('CREATE UNIQUE INDEX IF NOT EXISTS idx_store_v20_candidate_unique ON v20_link_candidate_snapshot(target_issue,candidate_key)')
 assert db.execute('SELECT snapshot_key,target_issue,candidate_key FROM v20_link_candidate_snapshot').fetchall()==[('old',None,None)]
 assert [r[2] for r in db.execute('pragma index_info(idx_store_v20_candidate_unique)')]==['target_issue','candidate_key']
 ddl=json.loads(Path(ddl_path).read_text());db.executescript(';\n'.join(ddl)+';')
 insert="INSERT INTO r31_hypothesis_registry VALUES('h','p','s','f','OPEN','{}','hash',1)"
 try:db.execute(insert)
 except sqlite3.IntegrityError as e: assert 'PROTECTED_STORAGE_WAIT' in str(e)
 else:raise AssertionError('empty quota must stop business writes')
 db.rollback()
 with db:
  assert all(db.execute('SELECT 1 FROM '+t+' LIMIT 1').fetchone() is None for t in ['r31_hypothesis_registry','r31_hypothesis_binding','r31_hypothesis_select','r31_hypothesis_outcome'])
  db.execute('INSERT INTO r31_hypothesis_quota VALUES(1,0)')
 with db: db.execute(insert)
 before=db.execute('SELECT logical_bytes FROM r31_hypothesis_quota').fetchone()[0]
 with db:db.execute(insert)
 assert before==1026 and db.execute('SELECT logical_bytes FROM r31_hypothesis_quota').fetchone()[0]==before
 try:db.execute("UPDATE r31_hypothesis_registry SET state='X'".replace("state=","status="))
 except sqlite3.IntegrityError as e:assert 'IMMUTABLE' in str(e)
 else:raise AssertionError('protected evidence was mutable')
 print('PASS old schema reproducer, idempotent repair, preserved original row, exact index columns, quota wait, accounted insert, idempotent insert, immutable original')
if __name__=='__main__':run(sys.argv[1])
