#!/usr/bin/env python3
"""Plan payload checks; these never claim Android compilation or device execution."""
import argparse,hashlib,json,pathlib,re,sqlite3,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--baseline',required=True);a=p.parse_args()
base=pathlib.Path(a.baseline).resolve(); payload=pathlib.Path(__file__).resolve().parents[1]
m=json.loads((payload/'PAYLOAD_MANIFEST.json').read_text());checked=[]
for f in m['files']:
 path=base/f['path']; after=payload/'after_full'/f['path']
 if f['before_sha256'] is not None:assert hashlib.sha256(path.read_bytes()).hexdigest()==f['before_sha256'],f['path']
 assert hashlib.sha256(after.read_bytes()).hexdigest()==f['after_sha256'],f['path']
 checked.append(f['path'])
subprocess.run(['git','-C',str(base),'apply','--check',str(payload/'patches/ALL_RUNTIME.patch')],check=True,capture_output=True,text=True)
with tempfile.TemporaryDirectory(prefix='r34-runtime-host-') as tmp:
 tmp=pathlib.Path(tmp);dest=tmp/'classes';dest.mkdir()
 source=(payload/'after_full/app/src/main/java/com/ethk3/novaonepro/DeferredEvidenceWriterR36.java').read_text()
 start=source.index('    static boolean transientResourceFailure(String error) {');i=source.index('{',start);depth=1;j=i+1
 while depth:
  if source[j]=='{':depth+=1
  elif source[j]=='}':depth-=1
  j+=1
 extracted=tmp/'DeferredPolicyExtract.java';extracted.write_text('package com.ethk3.novaonepro;\nfinal class DeferredPolicyExtract {\n'+source[start:j]+'\n}\n')
 java=[payload/'after_full/app/src/main/java/com/ethk3/novaonepro/RawHttpIdentityR34.java',payload/'after_full/app/src/main/java/com/ethk3/novaonepro/DeferredWaitR34.java',base/'app/src/main/java/com/ethk3/novaonepro/V51ChecksumEngine.java',payload/'tests/RuntimeR34Host.java',extracted]
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','17','-target','17','-encoding','UTF-8','-d',str(dest),*map(str,java)],check=True,capture_output=True,text=True)
 r=subprocess.run(['java','-cp',str(dest),'com.ethk3.novaonepro.RuntimeR34Host'],check=True,capture_output=True,text=True)
 host=json.loads(r.stdout)
# SQLite contract fixture: transport receipt and canonical draw must share the final transaction;
# no partial draw remains after a receipt conflict or owner rollback.
sqlchecks=[]
con=sqlite3.connect(':memory:');con.executescript('CREATE TABLE receipt(request_id TEXT PRIMARY KEY,payload_hash TEXT NOT NULL); CREATE TABLE draws(issue TEXT PRIMARY KEY,request_id TEXT NOT NULL,payload_hash TEXT NOT NULL);')
h='a'*64
with con:
 con.execute('INSERT INTO receipt VALUES(?,?)',('req',h));con.execute('INSERT INTO draws VALUES(?,?,?)',('202611230288','req',h))
assert con.execute('SELECT count(*) FROM draws d JOIN receipt r ON d.request_id=r.request_id AND d.payload_hash=r.payload_hash').fetchone()[0]==1;sqlchecks.append('receipt_draw_same_transaction')
try:
 with con:
  con.execute('INSERT INTO receipt VALUES(?,?)',('req2',h));con.execute('INSERT INTO draws VALUES(?,?,?)',('202611230289','req2',h));raise RuntimeError('injected rollback')
except RuntimeError:pass
assert con.execute('SELECT count(*) FROM receipt WHERE request_id=?',('req2',)).fetchone()[0]==0
assert con.execute('SELECT count(*) FROM draws WHERE issue=?',('202611230289',)).fetchone()[0]==0;sqlchecks.append('rollback_preserves_atomicity')
# Non-destructive owner/fence CAS for a postponed deferred row: stale lease owner must not acknowledge.
con.executescript("CREATE TABLE deferred(target TEXT PRIMARY KEY,status TEXT,lease_owner TEXT,attempts INTEGER);INSERT INTO deferred VALUES('202611230288','RUNNING','new-owner',4);")
with con: n=con.execute("UPDATE deferred SET status='RETRY_WAIT',attempts=attempts-1 WHERE target=? AND status='RUNNING' AND lease_owner=?",('202611230288','old-owner')).rowcount
assert n==0;sqlchecks.append('stale_retry_owner_rejected')
result={'status':'PASS','baseline':m['baseline_commit'],'hashes_checked':len(checked),'patch_application_check':'PASS',**host,'sqlite_contract_checks':sqlchecks,'android_full_compile':'NOT_RUN','device_test':'NOT_RUN','release_apk':'NOT_BUILT','scope':'Helpers and extracted exact policy compiled/executed; SQL contracts exercised. Full Android modified classes remain integration-gated.'}
print(json.dumps(result,ensure_ascii=False,indent=2))
