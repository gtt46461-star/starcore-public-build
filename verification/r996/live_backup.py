"""Real vendored SQLite backup under uninterrupted WAL commits; synthetic data only."""
from pathlib import Path
import sqlite3,threading,time,subprocess,json,hashlib
r=Path(__file__).resolve().parents[3];o=r/'evidence/r996/live-backup';o.mkdir(parents=True,exist_ok=True)
src=o/'live.db';d=sqlite3.connect(src);d.execute('PRAGMA journal_mode=WAL');d.execute('PRAGMA wal_autocheckpoint=0')
d.execute('CREATE TABLE history(issue INTEGER PRIMARY KEY, numbers TEXT NOT NULL)');d.executemany('INSERT INTO history VALUES(?,?)',((i,'1,2,3,4,5,6,7,8,9,10') for i in range(5000)))
d.execute('CREATE TABLE payload(id INTEGER PRIMARY KEY,b BLOB)');d.executemany('INSERT INTO payload VALUES(?,zeroblob(262144))',((i,) for i in range(768)))
d.execute('CREATE TABLE heartbeat(id INTEGER PRIMARY KEY,value INTEGER)');d.execute('INSERT INTO heartbeat VALUES(1,0)');d.commit();d.execute('PRAGMA wal_checkpoint(TRUNCATE)');d.close()
stop=threading.Event();commits=[];errors=[]
def writer():
 c=sqlite3.connect(src,timeout=1);c.execute('PRAGMA wal_autocheckpoint=0')
 try:
  n=0
  while not stop.is_set():
   n+=1;c.execute('UPDATE heartbeat SET value=? WHERE id=1',(n,));c.commit();commits.append(time.monotonic());stop.wait(.003)
 except Exception as e:errors.append(str(e))
 finally:c.close()
t=threading.Thread(target=writer);t.start();results=[]
try:
 for era,budget in [('before',4000),('after',10000),('after-cancel',20)]:
  dest=o/(era+'.db');start=time.monotonic();n=len(commits)
  p=subprocess.run([str(r/'evidence/r996'/('backup-before' if era=='before' else 'backup-after')),str(src),str(dest),str(budget)],capture_output=True,text=True,timeout=15)
  result=json.loads(p.stdout);result.update(case=era,writerCommits=len(commits)-n,sourceBytes=src.stat().st_size,outputExists=dest.exists(),seconds=round(time.monotonic()-start,3))
  if dest.exists():
   c=sqlite3.connect('file:'+str(dest)+'?mode=ro',uri=True);result.update(integrity=c.execute('PRAGMA integrity_check').fetchone()[0],historyRows=c.execute('SELECT count(*) FROM history').fetchone()[0],payloadRows=c.execute('SELECT count(*) FROM payload').fetchone()[0],snapshotHeartbeat=c.execute('SELECT value FROM heartbeat').fetchone()[0]);c.close()
   result['bytes']=dest.stat().st_size
   h=hashlib.sha256()
   with dest.open('rb')as f:
    for b in iter(lambda:f.read(1048576),b''):h.update(b)
   result['sha256']=h.hexdigest();dest.unlink() # This large fixture is reproducible; retain its receipt.
  results.append(result);print(json.dumps(result),flush=True)
finally:stop.set();t.join()
assert not errors,errors
assert results[0]['nativeCode']==-1001 and not results[0]['outputExists'],results
assert results[1]['nativeCode']==0 and results[1]['integrity']=='ok' and results[1]['historyRows']==5000 and results[1]['payloadRows']==768 and results[1]['writerCommits']>20,results
assert results[2]['nativeCode']==-1001 and not results[2]['outputExists'] and results[2]['writerCommits']>0,results
receipt={'scope':'real current native code and real SQLite, 5000 synthetic draws plus 192MiB data, continuous 3ms WAL writer, not user device','results':results,'writerErrors':errors,'passed':True}
(o/'receipt.json').write_text(json.dumps(receipt,indent=2));src.unlink()
