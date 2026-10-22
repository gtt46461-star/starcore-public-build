#!/usr/bin/env python3
"""Real production C snapshot on a physically filled synthetic large SQLite source.
No live API or user DB. This measures host snapshot consistency, never phone timing.
"""
import argparse, ctypes, hashlib, json, os, sqlite3, subprocess, sys, time
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--library',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--revision-rows',type=int,default=11_408_058);p.add_argument('--payload-mib',type=int,default=1024);p.add_argument('--source',type=Path);a=p.parse_args()
a.out.mkdir(parents=True,exist_ok=False);source=a.source.resolve() if a.source else a.out/'large-source.db'; checks=[];timings={}
def check(ok,name):
    if not ok: raise AssertionError(name)
    checks.append(name)
def sha(path):
    d=hashlib.sha256()
    with open(path,'rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):d.update(b)
    return d.hexdigest()
base=100_000_000
if a.source is None:
    begin=time.monotonic();d=sqlite3.connect(source);d.execute('PRAGMA journal_mode=OFF');d.execute('PRAGMA synchronous=OFF');d.execute('PRAGMA auto_vacuum=INCREMENTAL')
    d.executescript('''CREATE TABLE selected_rows(id INTEGER PRIMARY KEY,epoch INTEGER NOT NULL,payload BLOB NOT NULL);
    CREATE TABLE unrelated_history(id INTEGER PRIMARY KEY,payload BLOB NOT NULL);
    CREATE TABLE research_row_revision_v2(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,UNIQUE(source_table,source_rowid));''')
    base=100_000_000
    for i in range(1,129):
        d.execute('INSERT INTO selected_rows VALUES(?,0,?)',(i,bytes([i%251])*2048));d.execute('INSERT INTO research_row_revision_v2 VALUES(?,?,?)',(base+i,'selected_rows',i))
    d.commit()
    for start in range(1,a.revision_rows-128+1,100_000):
        n=min(100_000,a.revision_rows-128-start+1)
        d.execute('WITH RECURSIVE n(x) AS(SELECT 0 UNION ALL SELECT x+1 FROM n WHERE x+1<?) INSERT INTO research_row_revision_v2(generation,source_table,source_rowid) SELECT ?+x,\'v57_k3_action_space_1200\',?+x FROM n',(n,start,start));d.commit()
    for i in range(a.payload_mib):
        d.execute('INSERT INTO unrelated_history VALUES(?,?)',(i,bytes([i%251])*1024*1024))
        if i%32==31:d.commit()
    d.commit();check(d.execute('SELECT count(*) FROM research_row_revision_v2').fetchone()[0]==a.revision_rows,'actual revision cardinality equals configured count');check(d.execute('PRAGMA quick_check').fetchone()[0]=='ok','large source quick_check before native reads')
    d.execute('PRAGMA journal_mode=WAL');d.execute('PRAGMA synchronous=NORMAL');d.close();timings['fixture_seconds']=time.monotonic()-begin
else:
    with sqlite3.connect(source) as d:
        check(d.execute('SELECT count(*) FROM research_row_revision_v2').fetchone()[0]==a.revision_rows,'reused isolated fixture has exact revision cardinality')
        check(d.execute('PRAGMA quick_check').fetchone()[0]=='ok','reused isolated fixture integrity')
physical=source.stat().st_blocks*512;logical=source.stat().st_size
check(physical>logical*.99,'large SQLite file is physically allocated, not sparse')
lib=ctypes.CDLL(str(a.library.resolve()));control_type=ctypes.CFUNCTYPE(ctypes.c_int,ctypes.c_void_p)
fn=lib.cp76_retention_page;fn.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_char_p,ctypes.POINTER(ctypes.c_int64),ctypes.c_int,ctypes.c_int64,ctypes.c_int64,control_type,ctypes.c_void_p];fn.restype=ctypes.c_int
ids=(ctypes.c_int64*128)(*range(1,129));calls=[0]
def native(path, rowids=ids, count=128, limit=1024*1024, stop_at=None, slow=False):
    calls[0]=0
    @control_type
    def control(_):
        calls[0]+=1
        if slow:time.sleep(.0005)
        return 0 if stop_at and calls[0]>=stop_at else 1
    start=time.monotonic();rc=fn(os.fsencode(source),os.fsencode(path),b'selected_rows',rowids,count,32<<20,limit,control,None)
    return rc,time.monotonic()-start,calls[0]
def verify_page(path):
    s=sqlite3.connect(path)
    check(s.execute('PRAGMA quick_check').fetchone()[0]=='ok',path.name+' quick_check')
    rows=s.execute('SELECT id,epoch,payload,generation FROM selected_rows JOIN research_row_revision_v2 ON source_table=\'selected_rows\' AND source_rowid=id ORDER BY id').fetchall()
    check(len(rows)==128,path.name+' all128 requested rows present')
    check(len({r[1] for r in rows})==1,path.name+' single source epoch across all rows')
    check(all(g==base+e*128+i and payload==bytes([i%251])*2048 for i,e,payload,g in rows),path.name+' cell bytes and exact generation from same snapshot')
    check(s.execute("SELECT count(*) FROM sqlite_master WHERE name='unrelated_history'").fetchone()[0]==0,path.name+' unrelated1GiB history never copied')
    check(s.execute('SELECT count(*) FROM research_row_revision_v2').fetchone()[0]==128,path.name+' no unrelated revisions copied')
    check(path.stat().st_size<1024*1024,path.name+' selected copy remains below1MiB');s.close();return rows[0][1]
pre=sha(source);small=a.out/'selected.db';rc,secs,callbacks=native(small);check(rc==0,'native selected page succeeds on large source');verify_page(small);timings['selected_seconds']=secs;timings['selected_callbacks']=callbacks;check(sha(source)==pre,'read-only native selection leaves entire source hash unchanged')
for label,kwargs,expected in [('cancel',{'stop_at':3},-1001),('missing',{'rowids':(ctypes.c_int64*1)(99999999),'count':1},12),('disk-limit',{'limit':65536},13)]:
    dest=a.out/(label+'.db');rc,secs,_=native(dest,**kwargs);check(rc==expected,label+' exact native failure code');check(not dest.exists(),label+' removes unpublished private copy only');check(sha(source)==pre,label+' source bytes unchanged')
check(native(small)[0]==-1004,'existing destination not overwritten');verify_page(small)
# Use a separate process, as Android :diagnostics does. Mixing two independent
# SQLite libraries' lock managers in one process is not a supported deployment.
stop=a.out/'writer.stop';countfile=a.out/'writer-commits.txt';err=a.out/'writer-error.log'
writer_code=r"""
import sqlite3,sys,time,pathlib
source,stop,counts=sys.argv[1:];x=sqlite3.connect(source,timeout=5);x.execute('PRAGMA journal_mode=WAL');epoch=x.execute('SELECT max(epoch) FROM selected_rows').fetchone()[0];count=0
while not pathlib.Path(stop).exists():
    epoch+=1;x.execute('BEGIN IMMEDIATE');x.execute('UPDATE selected_rows SET epoch=?',(epoch,));x.execute("UPDATE research_row_revision_v2 SET generation=?+source_rowid WHERE source_table='selected_rows'",(100_000_000+epoch*128,));x.commit();count+=1
    with open(counts,'a') as f:f.write(str(epoch)+'\n');f.flush()
    if count==1:print('READY',flush=True)
    time.sleep(.001)
x.close()
"""
errors=err.open('w');writer=subprocess.Popen([sys.executable,'-u','-c',writer_code,str(source),str(stop),str(countfile)],stdout=subprocess.PIPE,stderr=errors,text=True)
check(writer.stdout.readline().strip()=='READY','separate-process WAL writer committed before snapshot')
def commits():return len(countfile.read_text().splitlines()) if countfile.exists() else 0
epochs=[];race_times=[]
try:
    for i in range(4):
        before=commits();dest=a.out/('concurrent-'+str(i)+'.db');rc,secs,_=native(dest,slow=True);check(rc==0,'concurrent snapshot'+str(i)+' succeeds');epochs.append(verify_page(dest));race_times.append(secs);check(commits()>before,'writer continues during native snapshot'+str(i))
finally:
    stop.touch();writer.wait(timeout=10);errors.close()
check(writer.returncode==0 and not err.read_text(),'separate-process writer terminates without error');check(epochs==sorted(epochs),'fresh snapshots advance rather than reuse stale page')
with sqlite3.connect(source) as d:
    check(d.execute('SELECT count(*) FROM research_row_revision_v2').fetchone()[0]==a.revision_rows,'all revision rows preserved after concurrent snapshots');check(d.execute('SELECT sum(length(payload)) FROM unrelated_history').fetchone()[0]==a.payload_mib*1024*1024,'all unrelated payload preserved');check(d.execute('PRAGMA quick_check').fetchone()[0]=='ok','source integrity after failures and concurrent snapshots')
result={'status':'PASS','scope':'ACTUAL_PRODUCTION_NATIVE_C_HOST_SYNTHETIC_SOURCE_NOT_USER_PHONE','product_library_sha256':sha(a.library),'source_bytes':logical,'allocated_bytes':physical,'revision_rows':a.revision_rows,'unrelated_payload_bytes':a.payload_mib*1024*1024,'selected_bytes':small.stat().st_size,'checks':len(checks),'checks_detail':checks,'timings':timings,'concurrent_epochs':epochs,'writer_commits':commits(),'concurrent_seconds':race_times,'limits':'No original14GBDB, Android device latency, retention eligibility, Java publication/prune or actual user reclaim conclusion is claimed.'}
(a.out/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result,indent=2))
