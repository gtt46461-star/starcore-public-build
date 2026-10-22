#!/usr/bin/env python3
"""Replay production deferred materialization on a disposable copy of a supplied real backup.

Never changes source DB, resets leases, fills missing snapshots, or asserts phone latency.
Run r925_compile_host.sh and r927_engineering_closure_harness.py first.
"""
import argparse,hashlib,json,os,sqlite3,subprocess,time
from pathlib import Path

R=Path(__file__).resolve().parents[1]
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def counts(db):
 return dict(db.execute("SELECT status,COUNT(*) FROM v57_deferred_evidence_job GROUP BY status"))
def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('--db',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
 a=p.parse_args();source=a.db.resolve();out=a.out.resolve()
 if not source.is_file():p.error('a real SQLite backup is required')
 out.mkdir(parents=True,exist_ok=False);copy=out/'replay-copy.sqlite';before_hash=sha(source)
 original=sqlite3.connect(source.as_uri()+'?mode=ro',uri=True)
 copied=sqlite3.connect(copy)
 try:
  original.backup(copied)
  if copied.execute('PRAGMA integrity_check').fetchone()[0]!='ok':raise RuntimeError('INPUT_INTEGRITY_FAILED')
  before=counts(copied)
 finally:copied.close();original.close()
 h=R/'build/r925-host/compiled-classes';e=R/'build/r927-engineering'
 cp=os.pathsep.join(map(str,[e,h,e/'android-runtime']))
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(out),str(R/'verification/r925/R927BackupReplay.java')],check=True)
 started=time.monotonic()
 with (out/'slices.tsv').open('w') as log:
  log.write('target_issue\tslice\tsuccess\tstatus\thost_call_elapsed_ms\n');log.flush()
  run=subprocess.run(['java','-cp',str(out)+os.pathsep+cp,'com.ethk3.novaonepro.R927BackupReplay',str(R/'verification/r925/sqlite_bridge.py'),str(copy)],stdout=log,stderr=subprocess.PIPE,text=True)
 (out/'stderr.txt').write_text(run.stderr)
 db=sqlite3.connect(copy)
 try:
  after=counts(db)
  missing=db.execute("SELECT COUNT(*) FROM v57_deferred_evidence_job WHERE status<>'COMPLETED' AND (snapshot_blob IS NULL OR length(snapshot_blob)=0)").fetchone()[0]
  integrity=db.execute('PRAGMA integrity_check').fetchone()[0]
 finally:db.close()
 unchanged=sha(source)==before_hash;remaining=sum(n for s,n in after.items() if s!='COMPLETED')
 result={'source_sha256':before_hash,'source_unchanged':unchanged,'copy_sha256':sha(copy),'before':before,'after':after,'remaining':remaining,'missing_snapshot':missing,'integrity':integrity,'command_exit_code':run.returncode,'elapsed_seconds':time.monotonic()-started,'production_materializer':True,'coordinator':'SYNCHRONOUS_SQLITE_TEST_ADAPTER','phone_latency_proven':False,'passed':run.returncode==0 and unchanged and integrity=='ok' and remaining==0 and sum(before.values())>0,'scope':'SUPPLIED_DATABASE_REPLAY_NOT_PHYSICAL_DEVICE'}
 (out/'RESULT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
 raise SystemExit(0 if result['passed'] else 1)
if __name__=='__main__':main()
