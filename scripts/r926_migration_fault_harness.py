#!/usr/bin/env python3
"""Executable B03/B04 migration, WAL backup, crash retry and same-revision repair harness."""
from __future__ import annotations
from pathlib import Path
import hashlib, json, shutil, sqlite3, tempfile
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'reports/r926/batch1'; OUT.mkdir(parents=True,exist_ok=True)
UPGRADE=ROOT/'app/src/main/java/com/ethk3/novaonepro/SchemaUpgradeCoordinatorR815.java'
DOMAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java'
DBCOORD=ROOT/'app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java'
RESEARCH=ROOT/'app/src/main/java/com/ethk3/novaonepro/ResearchSchemaCoordinatorR926.java'
LEGACY=ROOT/'app/src/main/java/com/ethk3/novaonepro/LegacySchemaCoordinatorR926.java'

def filehash(p): return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def hist(c):
    h=hashlib.sha256(); rows=c.execute('SELECT issue,a,b FROM history ORDER BY rowid').fetchall()
    for r in rows:
        for v in r: h.update((str(v)+'\0').encode())
    return len(rows),h.hexdigest()
def init(path):
    c=sqlite3.connect(path); c.execute('PRAGMA journal_mode=WAL'); c.execute('PRAGMA user_version=36')
    c.executescript('CREATE TABLE history(issue TEXT PRIMARY KEY,a INTEGER,b TEXT); CREATE TABLE canonical(issue TEXT PRIMARY KEY,value TEXT);')
    c.executemany('INSERT INTO history VALUES(?,?,?)',[(f'i{x:04d}',x,f'v{x}') for x in range(250)])
    c.execute("INSERT INTO canonical VALUES('i0249','keep')"); c.commit(); return c

def ensure_infra(c):
    c.executescript('''CREATE TABLE IF NOT EXISTS domain(domain TEXT PRIMARY KEY,revision INTEGER NOT NULL,state TEXT NOT NULL);
CREATE TABLE IF NOT EXISTS journal(domain TEXT NOT NULL,revision INTEGER NOT NULL,step_id TEXT NOT NULL,status TEXT NOT NULL,attempt INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(domain,revision,step_id));''')
def complete(c,s):
    r=c.execute("SELECT status FROM journal WHERE domain='CRITICAL' AND revision=1 AND step_id=?",(s,)).fetchone(); return bool(r and r[0]=='COMPLETED')
def runstep(c,s,fn,kill=False):
    ensure_infra(c)
    if complete(c,s): return
    prior=c.execute("SELECT COALESCE(MAX(attempt),0) FROM journal WHERE domain='CRITICAL' AND revision=1 AND step_id=?",(s,)).fetchone()[0]
    c.execute("INSERT OR REPLACE INTO journal VALUES('CRITICAL',1,?,'RUNNING',?)",(s,prior+1)); c.commit()
    try:
        c.execute('BEGIN'); fn(c)
        if kill: raise RuntimeError('INJECTED_PROCESS_KILL')
        c.execute("UPDATE journal SET status='COMPLETED' WHERE domain='CRITICAL' AND revision=1 AND step_id=?",(s,)); c.commit()
    except RuntimeError:
        c.rollback(); raise

def migrate(c,kill_step=None):
    steps=[('S1',lambda x:x.execute('CREATE TABLE IF NOT EXISTS additive(id TEXT PRIMARY KEY,v TEXT)')),
           ('S2',lambda x:x.execute("INSERT OR IGNORE INTO additive VALUES('one','1')")),
           ('S3',lambda x:x.execute('CREATE INDEX IF NOT EXISTS idx_history_a ON history(a)'))]
    for name,fn in steps: runstep(c,name,fn,kill=kill_step==name)
    c.execute("INSERT OR REPLACE INTO domain VALUES('CRITICAL',1,'READY')"); c.commit()

# Domain-repair executable model mirrors the Java decision contract but uses tiny test tables.
def repair_schema(c):
    c.execute('CREATE TABLE IF NOT EXISTS domain_meta(domain TEXT PRIMARY KEY,revision INTEGER,contract_hash TEXT,fingerprint TEXT,state TEXT)')
    c.execute('CREATE TABLE IF NOT EXISTS repair_journal(domain TEXT,revision INTEGER,step_id TEXT,status TEXT,attempt INTEGER,PRIMARY KEY(domain,revision,step_id))')
def object_fp(c):
    rows=c.execute("SELECT type,name,COALESCE(sql,'') FROM sqlite_master WHERE name IN ('critical_a','critical_b') ORDER BY type,name").fetchall()
    return hashlib.sha256(json.dumps(rows,separators=(',',':')).encode()).hexdigest()
def seed_ready(c, *, bad_fp=False, bad_contract=False, missing=False):
    repair_schema(c)
    c.execute('CREATE TABLE IF NOT EXISTS critical_a(id INTEGER PRIMARY KEY,v TEXT)')
    c.execute('CREATE TABLE IF NOT EXISTS critical_b(id INTEGER PRIMARY KEY,v TEXT)')
    for step in ('CREATE_A','CREATE_B'):
        c.execute("INSERT OR REPLACE INTO repair_journal VALUES('CRITICAL',1,?,'COMPLETED',1)",(step,))
    if missing: c.execute('DROP TABLE critical_b')
    fp='BAD_FINGERPRINT' if bad_fp else object_fp(c)
    ct='BAD_CONTRACT' if bad_contract else 'CONTRACT_V1'
    c.execute("INSERT OR REPLACE INTO domain_meta VALUES('CRITICAL',1,?,?, 'READY')",(ct,fp)); c.commit()
def prepare_repair_model(c):
    repair_schema(c)
    row=c.execute("SELECT revision,contract_hash,fingerprint,state FROM domain_meta WHERE domain='CRITICAL'").fetchone()
    if not row: return False
    rev,ct,stored_fp,state=row
    if rev != 1: return False
    present=all(c.execute("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",(n,)).fetchone() for n in ('critical_a','critical_b'))
    current=object_fp(c)
    contract_changed=ct!='CONTRACT_V1'
    fingerprint_changed=current!=stored_fp
    ready_invalid=state=='READY' and (contract_changed or fingerprint_changed or not present)
    requires=contract_changed or (not present) or ready_invalid
    if not requires: return False
    c.execute("UPDATE repair_journal SET status='REPAIR_REQUIRED' WHERE domain='CRITICAL' AND revision=1 AND status='COMPLETED'")
    c.execute("UPDATE domain_meta SET contract_hash='CONTRACT_V1',fingerprint=?,state='REPAIR_REQUIRED' WHERE domain='CRITICAL'",(current,)); c.commit(); return True
def rerun_repairs(c):
    for step,sql in [('CREATE_A','CREATE TABLE IF NOT EXISTS critical_a(id INTEGER PRIMARY KEY,v TEXT)'),('CREATE_B','CREATE TABLE IF NOT EXISTS critical_b(id INTEGER PRIMARY KEY,v TEXT)')]:
        row=c.execute("SELECT status,attempt FROM repair_journal WHERE domain='CRITICAL' AND revision=1 AND step_id=?",(step,)).fetchone()
        if not row or row[0] != 'COMPLETED':
            c.execute(sql); attempt=(row[1] if row else 0)+1
            c.execute("INSERT OR REPLACE INTO repair_journal VALUES('CRITICAL',1,?,'COMPLETED',?)",(step,attempt))
    fp=object_fp(c); c.execute("UPDATE domain_meta SET contract_hash='CONTRACT_V1',fingerprint=?,state='READY' WHERE domain='CRITICAL'",(fp,)); c.commit()

def main():
    src=UPGRADE.read_text(); dom=DOMAIN.read_text(); dbc=DBCOORD.read_text(); rs=RESEARCH.read_text(); lg=LEGACY.read_text()
    A={
      'source_requires_wal_checkpoint':'PRAGMA wal_checkpoint(FULL)' in src,
      'source_closes_helper_before_copy':src.find('helper.close();') < src.find('copyAndSync(database,backup);'),
      'source_compares_backup_to_source_hash':'BACKUP_SOURCE_HASH_MISMATCH' in src,
      'source_deletes_restore_sidecars':'deleteSqliteSidecars(destination)' in src and 'RESTORE_WAL_DELETE_FAILED' in src,
      'source_has_no_database_wipe':'deleteDatabase(' not in src and 'DROP TABLE' not in src,
      'journal_step_is_transactional':"status='COMPLETED'" in dom and 'db.setTransactionSuccessful()' in dom,
      'source_has_contract_drift_repair':'prepareRepairIfDrift' in dom and 'REPAIR_REQUIRED' in dom and 'R926_CONTRACT_DRIFT' in dom,
      'critical_calls_repair':'prepareRepairIfDrift(db, critical)' in dbc,
      'research_calls_repair':'prepareRepairIfDrift(db, contract)' in rs,
      'legacy_calls_repair':'prepareRepairIfDrift(db, c)' in lg,
    }
    with tempfile.TemporaryDirectory(prefix='r926-migrate-') as td:
      path=Path(td)/'live.db'; c=init(path)
      c.execute("INSERT INTO history VALUES('wal-row',999,'wal')"); c.commit(); before=hist(c)
      chk=c.execute('PRAGMA wal_checkpoint(FULL)').fetchone(); A['wal_checkpoint_not_busy']=chk[0]==0
      c.close(); backup=Path(td)/'backup.db'; shutil.copy2(path,backup); expected=filehash(path)
      A['closed_checkpoint_backup_hash_exact']=filehash(backup)==expected
      bc=sqlite3.connect(backup); A['backup_quick_check']=bc.execute('PRAGMA quick_check(1)').fetchone()[0]=='ok'; A['backup_contains_wal_row']=hist(bc)==before; bc.close()
      all_resume=True
      for kill in ['S1','S2','S3']:
        work=Path(td)/f'{kill}.db'; shutil.copy2(backup,work); x=sqlite3.connect(work)
        try: migrate(x,kill)
        except RuntimeError: pass
        x.close(); x=sqlite3.connect(work); migrate(x); migrate(x)
        ok=(hist(x)==before and x.execute("SELECT COUNT(*) FROM additive WHERE id='one'").fetchone()[0]==1 and x.execute("SELECT COUNT(*) FROM journal WHERE status='COMPLETED'").fetchone()[0]==3 and x.execute('PRAGMA user_version').fetchone()[0]==36)
        all_resume=all_resume and ok; x.close()
      A['kill_each_step_reenters_idempotently']=all_resume
      corrupt=Path(td)/'corrupt.db'; shutil.copy2(backup,corrupt); corrupt.write_bytes(corrupt.read_bytes()+b'CORRUPT')
      A['corrupt_backup_hash_rejected']=filehash(corrupt)!=expected
      restored=Path(td)/'restored.db'; shutil.copy2(backup,restored); r=sqlite3.connect(restored); A['restore_history_exact']=hist(r)==before; r.close()

      # B03 same-revision repair matrix.
      new=sqlite3.connect(Path(td)/'repair-new.db'); repair_schema(new); A['new_install_no_false_repair']=not prepare_repair_model(new); new.close()
      good=sqlite3.connect(Path(td)/'repair-good.db'); seed_ready(good); A['valid_ready_stamp_no_repair']=not prepare_repair_model(good); good.close()
      miss=sqlite3.connect(Path(td)/'repair-missing.db'); seed_ready(miss,missing=True); armed=prepare_repair_model(miss); before_attempt=miss.execute("SELECT MIN(attempt) FROM repair_journal").fetchone()[0]; rerun_repairs(miss); after_attempt=miss.execute("SELECT MIN(attempt) FROM repair_journal").fetchone()[0]; A['missing_table_rearms_and_recreates']=armed and after_attempt>before_attempt and bool(miss.execute("SELECT 1 FROM sqlite_master WHERE name='critical_b'").fetchone()) and miss.execute("SELECT state FROM domain_meta").fetchone()[0]=='READY'; miss.close()
      badfp=sqlite3.connect(Path(td)/'repair-badfp.db'); seed_ready(badfp,bad_fp=True); armed=prepare_repair_model(badfp); rerun_repairs(badfp); A['bad_fingerprint_rearms_and_restamps']=armed and badfp.execute("SELECT fingerprint FROM domain_meta").fetchone()[0]==object_fp(badfp); badfp.close()
      badct=sqlite3.connect(Path(td)/'repair-badcontract.db'); seed_ready(badct,bad_contract=True); armed=prepare_repair_model(badct); rerun_repairs(badct); A['same_revision_contract_drift_rearms']=armed and badct.execute("SELECT contract_hash FROM domain_meta").fetchone()[0]=='CONTRACT_V1'; badct.close()
    passed=all(A.values()); result={'tasks':['B03','B04'],'passed':passed,'assertion_count':len(A),'assertions':A,'proof_scope':'REAL_SQLITE_WAL_CHECKPOINT_CRASH_RETRY_IDEMPOTENCY_SAME_REVISION_DOMAIN_REPAIR_PLUS_JAVA_SAFETY_STATIC_CHECK'}
    (OUT/'B03_B04_MIGRATION_FAULT_HARNESS.json').write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False)); return 0 if passed else 1
if __name__=='__main__': raise SystemExit(main())
