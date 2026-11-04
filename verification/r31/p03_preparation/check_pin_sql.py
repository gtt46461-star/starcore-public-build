import json,sqlite3
from pathlib import Path
source=json.loads(Path(__file__).with_name('java-facts.json').read_text())
db=sqlite3.connect(':memory:');db.execute('PRAGMA foreign_keys=ON')
db.execute('CREATE TABLE r30_live_source_revision(singleton INTEGER PRIMARY KEY,generation INTEGER NOT NULL)');db.execute('INSERT INTO r30_live_source_revision VALUES(1,0)')
db.execute('CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,fetch_time INTEGER,dirty INTEGER,'+','.join('n%d INTEGER'%i for i in range(1,11))+')')
db.execute('CREATE TABLE r27_history_import(issue TEXT PRIMARY KEY,ordinal INTEGER,numbers TEXT,status TEXT)')
for ddl in source['researchDdl'][:2]:db.execute(ddl)
for sql in source['sourceTriggerSql']+source['pinTriggerSql']:db.execute(sql)
issue='202611010001';ordinal=source['canonicalOrdinal']
def initial():
    db.execute('DELETE FROM r30r1_research_measure');db.execute('DELETE FROM r30r1_analysis_run');db.execute('DELETE FROM draws')
    db.execute('INSERT INTO draws VALUES('+','.join('?' for _ in range(14))+')',[issue,100,100,0,*range(1,11)])
    revision=db.execute('SELECT generation FROM r30_live_source_revision').fetchone()[0]
    db.execute('INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',['pin','dataset','policy','SELECT','source',revision,ordinal,ordinal,'contract','INPUT_DATASET_PINNED','{}',100])
    return revision
def invalid():return bool(db.execute("SELECT 1 FROM r30r1_research_measure WHERE run_id='pin' AND kind='INPUT_INVALIDATION'").fetchone())
base=initial();db.execute('UPDATE draws SET fetch_time=200 WHERE issue=?',[issue]);fetch_revision=db.execute('SELECT generation FROM r30_live_source_revision').fetchone()[0]
facts={'fetch_time_change_increments_source_revision':fetch_revision!=base,'fetch_time_change_invalidates_pin':invalid()}
base=initial();db.execute('DELETE FROM draws WHERE issue=?',[issue]);delete_revision=db.execute('SELECT generation FROM r30_live_source_revision').fetchone()[0]
facts.update(delete_increments_global_revision=delete_revision!=base,delete_invalidates_pin=invalid(),stored_pin_revision=db.execute("SELECT source_revision FROM r30r1_analysis_run WHERE run_id='pin'").fetchone()[0],global_after_delete=delete_revision)
base=initial();db.execute('UPDATE draws SET n1=2,n2=1 WHERE issue=?',[issue]);facts['number_correction_invalidates_pin']=invalid()
assert not facts['fetch_time_change_increments_source_revision'] and not facts['fetch_time_change_invalidates_pin']
assert facts['delete_increments_global_revision'] and not facts['delete_invalidates_pin'] and facts['number_correction_invalidates_pin']
facts['scope']='ACTUAL_PRODUCTION_GENERATED_TRIGGER_SQL_ON_ISOLATED_HOST_SQLITE_NOT_ANDROID_MIGRATION'
facts['delete_interpretation']='Semantic withdrawal is not distinguished from rolling retention. Frozen copy may legitimately survive physical hot-row removal; do not indiscriminately invalidate all pins on retention.'
facts['known_at_interpretation']='fetch_time is absent from current input identity and revision. Stable first-known-at must be recorded; last-refetch time must not churn experiment identity.'
Path(__file__).with_name('sql-facts.json').write_text(json.dumps(facts,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(facts,ensure_ascii=False,indent=2))
