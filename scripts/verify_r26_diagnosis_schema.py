#!/usr/bin/env python3
"""Exact legacy SQL/data parity and interrupted unit recovery on real host SQLite.

Android writer latency is checked separately by R26SchemaMigrationDeviceTest.
"""
from pathlib import Path
import collections,hashlib,json,re,sqlite3,tempfile,time

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/ethk3/novaonepro'
ORACLE=ROOT/'verification/r26/diagnosis_schema/legacy_sql_oracle.json'

def strings(source,name):
    body=re.search(r'\b'+name+r'\s*=\s*\{(.*?)\n    \};',source,re.S)[1]
    return [json.loads(s) for s in re.findall(r'"(?:[^"\\]|\\.)*"',body)]

def main():
    old=json.loads(ORACLE.read_text());helper=(BASE/'DbHelper.java').read_text();ret=(BASE/'RetentionRevisionSchemaCP12.java').read_text()
    tables=strings(helper,'DIAGNOSIS_TABLES_CP12');creates=strings(helper,'DIAGNOSIS_CREATE_CP12');columns=strings(helper,'DIAGNOSIS_COLUMNS_CP12');indexes=strings(helper,'DIAGNOSIS_INDEXES_CP12');guards=strings(ret,'DIAGNOSIS_RETENTION_SQL_CP12');order=strings(ret,'DIAGNOSIS_RETENTION_TABLES_CP12')
    assert (tables,creates,columns,indexes,guards)==tuple(old[k] for k in ['tables','creates','columns','indexes','retention'])
    keys=['case_key','cache_key','promotion_key','freeze_key','judge_key','plan_key']
    grouped=collections.OrderedDict((t,[]) for t in order)
    for sql in guards:grouped[re.search(r'(?: ON | FROM )"([a-z_]+)"',sql)[1]].append(sql)
    def fixture(path):
        db=sqlite3.connect(path,isolation_level=None)
        db.execute('CREATE TABLE research_row_revision_v2(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,UNIQUE(source_table,source_rowid))')
        db.execute('CREATE TABLE research_retention_permit_v2(source_table TEXT,source_rowid INTEGER,source_generation INTEGER,archive_id TEXT)')
        db.execute('CREATE TABLE research_evidence_archive_v2(archive_id TEXT,state TEXT)')
        db.execute('CREATE TABLE fixture_marker(value TEXT)');db.execute("INSERT INTO fixture_marker VALUES('PRESERVE')")
        # Thousands of independent schema objects force ALTER TABLE to work on a large catalogue.
        db.execute('BEGIN')
        for i in range(1700):
            db.execute(f'CREATE TABLE fixture_{i}(id INTEGER PRIMARY KEY,value TEXT)');db.execute(f'CREATE INDEX fixture_i_{i} ON fixture_{i}(value)')
        for table,sql,key in zip(tables,creates,keys):
            db.execute(sql);db.executemany(f'INSERT INTO "{table}"("{key}") VALUES(?)',[(f'legacy-{i}',) for i in range(4096)])
        db.execute('COMMIT');return db
    def unit(db,table):
        i=tables.index(table);db.execute(creates[i]);present={row[1] for row in db.execute(f'PRAGMA table_info("{table}")')}
        for col in columns:
            if col.split(' ')[0] not in present:db.execute(f'ALTER TABLE "{table}" ADD COLUMN {col}')
        for sql in indexes:
            if re.search(r' ON '+table+r'\(',sql):db.execute(sql)
        for sql in grouped[table]:db.execute(sql)
    def snapshot(db):
        return list(db.execute("SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name")),{t:list(db.execute(f'SELECT * FROM "{t}" ORDER BY _id')) for t in tables},list(db.execute('SELECT source_table,source_rowid,generation FROM research_row_revision_v2 ORDER BY generation'))
    timings=[]
    with tempfile.TemporaryDirectory(prefix='r26-diagnosis-') as tmp:
        original=fixture(Path(tmp)/'old.db');new=fixture(Path(tmp)/'new.db')
        original.execute('BEGIN')
        for sql in old['creates']:original.execute(sql)
        for table in tables:
            present={r[1] for r in original.execute(f'PRAGMA table_info("{table}")')}
            for col in columns:
                if col.split(' ')[0] not in present:original.execute(f'ALTER TABLE "{table}" ADD COLUMN {col}')
        for sql in old['indexes']+old['retention']:original.execute(sql)
        original.execute('COMMIT')
        before=snapshot(new);new.execute('BEGIN');unit(new,order[0]);new.execute('ROLLBACK');assert before==snapshot(new),'Interrupted unit leaked DDL/data'
        for table in order:
            start=time.monotonic();new.execute('BEGIN');unit(new,table);new.execute('COMMIT');timings.append({'table':table,'hostSqliteMs':round((time.monotonic()-start)*1000,3)})
            # Reopen the database between units; no process-local ready flag can resume it.
            new.close();new=sqlite3.connect(Path(tmp)/'new.db',isolation_level=None)
        assert snapshot(original)==snapshot(new),'Final schema, rows or generations changed'
        before=snapshot(new)
        for table in order:
            new.execute('BEGIN');unit(new,table);new.execute('COMMIT')
        assert before==snapshot(new),'Reentry changed already installed state'
        for table in tables:
            assert new.execute('SELECT count(*) FROM research_row_revision_v2 WHERE source_table=?',(table,)).fetchone()[0]==4096
            try:new.execute(f'DELETE FROM "{table}" WHERE rowid=1')
            except sqlite3.IntegrityError as failure:assert 'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE' in str(failure)
            else:raise AssertionError('Unpermitted deletion accepted: '+table)
            generation=new.execute('SELECT generation FROM research_row_revision_v2 WHERE source_table=? AND source_rowid=1',(table,)).fetchone()[0]
            new.execute(f'UPDATE "{table}" SET summary_text=? WHERE rowid=1',('updated',))
            assert new.execute('SELECT generation FROM research_row_revision_v2 WHERE source_table=? AND source_rowid=1',(table,)).fetchone()[0]>generation
            new.execute("INSERT INTO research_evidence_archive_v2 VALUES(?, 'VERIFIED')",('a-'+table,));new.execute('INSERT INTO research_retention_permit_v2 VALUES(?,?,?,?)',(table,1,generation,'a-'+table))
            try:new.execute(f'DELETE FROM "{table}" WHERE rowid=1')
            except sqlite3.IntegrityError as failure:assert 'CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE' in str(failure)
            else:raise AssertionError('Stale generation permit accepted: '+table)
        assert new.execute('SELECT value FROM fixture_marker').fetchone()[0]=='PRESERVE'
        original.close();new.close()
    result={'status':'PASS','scope':'HOST_SQLITE_SQL_AND_DATA_PARITY_ONLY; NOT_ANDROID_WRITER_TIMING_OR_NATURAL_QUALITY','oldSource':'64d1472088d51d1e28032bfb1b094d2c39113658','oracleSha256':hashlib.sha256(ORACLE.read_bytes()).hexdigest(),'schemaFixtureObjects':3400,'rowsPerTable':4096,'checks':['exact_60_SQL_statements_and_18_column_definitions','final_schema_rows_and_generation_identity','rollback_restores_unit_ddl_and_data','database_reopen_between_six_units','idempotent_reentry','all_24576_row_versions_backfilled','unpermitted_and_stale_generation_delete_rejected','retained_fixture_marker'],'units':timings}
    out=ROOT/'build/r26/diagnosis-schema-parity.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,sort_keys=True))

if __name__=='__main__':main()
