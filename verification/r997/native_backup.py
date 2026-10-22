#!/usr/bin/env python3
"""Current vendored SQLite backup: 5000 real report draws plus synthetic concurrent writes.
Only disposable copies are changed. This is host native/WAL evidence, never device evidence.
"""
from pathlib import Path
import argparse, ctypes, hashlib, json, shutil, sqlite3, threading, time
p=argparse.ArgumentParser()
for f in ('library','phone-db','out'):p.add_argument('--'+f,required=True)
a=p.parse_args();out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
source=out/'live.db';shutil.copy2(a.phone_db,source)
d=sqlite3.connect(source);d.execute('PRAGMA journal_mode=WAL');d.execute('PRAGMA wal_autocheckpoint=0')
d.execute('CREATE TABLE r997_explicit_synthetic_native_heartbeat(id INTEGER PRIMARY KEY,value INTEGER)')
d.execute('INSERT INTO r997_explicit_synthetic_native_heartbeat VALUES(1,0)');d.commit();d.close()
lib=ctypes.CDLL(a.library);callback=ctypes.CFUNCTYPE(ctypes.c_int,ctypes.c_void_p)
lib.cp4_backup.argtypes=[ctypes.c_char_p,ctypes.c_char_p,ctypes.c_int64,ctypes.c_int64,callback,ctypes.c_void_p]
lib.cp4_backup.restype=ctypes.c_int
stop=threading.Event();commits=[];errors=[]
def writer():
    db=sqlite3.connect(source,timeout=1);db.execute('PRAGMA wal_autocheckpoint=0');i=0
    try:
        while not stop.is_set():
            i+=1;db.execute('UPDATE r997_explicit_synthetic_native_heartbeat SET value=?',(i,));db.commit()
            commits.append(time.monotonic());stop.wait(.003)
    except Exception as failure:errors.append(str(failure))
    finally:db.close()
t=threading.Thread(target=writer);t.start();results=[]
try:
    for name,cancel_ms in [('continuous_wal',None),('cancel',20)]:
        dest=out/(name+'.db');at=time.monotonic();before=len(commits)
        control=callback(lambda _:int(cancel_ms is None or (time.monotonic()-at)*1000<cancel_ms))
        code=lib.cp4_backup(str(source).encode(),str(dest).encode(),32<<20,0,control,None)
        r={'case':name,'native_code':code,'seconds':round(time.monotonic()-at,3),
           'writer_commits':len(commits)-before,'source_bytes':source.stat().st_size,'output_exists':dest.exists()}
        if dest.exists():
            with sqlite3.connect(dest) as db:
                r['integrity']=db.execute('PRAGMA integrity_check').fetchone()[0]
                r['draw_rows']=db.execute('SELECT COUNT(*) FROM draws').fetchone()[0]
            digest=hashlib.sha256()
            with dest.open('rb') as f:
                for chunk in iter(lambda:f.read(1<<20),b''):digest.update(chunk)
            r['sha256']=digest.hexdigest();r['bytes']=dest.stat().st_size;dest.unlink()
        results.append(r);print(json.dumps(r),flush=True)
finally:stop.set();t.join()
passed=(not errors and results[0]['native_code']==0 and results[0]['integrity']=='ok'
        and results[0]['draw_rows']==5000 and results[0]['writer_commits']>20
        and results[1]['native_code']==-1001 and not results[1]['output_exists'])
(out/'receipt.json').write_text(json.dumps({'passed':passed,'results':results,'writer_errors':errors,
    'scope':'CURRENT_NATIVE_SQLITE_SUPPLIED_5000_DRAWS_SYNTHETIC_WAL_WRITER_NOT_DEVICE'},indent=2))
if passed:source.unlink()
raise SystemExit(0 if passed else 1)
