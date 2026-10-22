#!/usr/bin/env python3
"""Verify actual release retention classes with native SQLite and full installed schema.
Only Android database/filesystem transports are adapted; no phone/runtime claim.
"""
from pathlib import Path
import argparse,subprocess,sqlite3,shutil,datetime,json,hashlib
p=argparse.ArgumentParser()
for name in ('workspace','schema','classes','jdk','out'):p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();w=a.workspace.resolve();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir()
j=a.jdk.resolve()/'bin';product=a.classes.resolve();host=a.schema.resolve();tests=o/'classes';tests.mkdir()
original=(host/'host-classpath').read_text().strip().split(':')
cp=':'.join([str(host/'classes'),str(product),original[-1]])
cmd=[str(j/'javac'),'-cp',cp,'-d',str(tests),str(r/'verification/r73/RetentionClosureR73.java'),str(r/'verification/r73/adapters/android/system/Os.java')]
with (o/'compile.log').open('w') as f:subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'live.db';shutil.copy2(host/'schema-complete.db',db)
c=sqlite3.connect(db);start=datetime.datetime(2026,10,7,tzinfo=datetime.timezone.utc)
for i in range(5000):
 date=start+datetime.timedelta(days=i//1440);issue=date.strftime('%Y%m%d')+f'{i%1440+1:04}';at=int((start+datetime.timedelta(minutes=i+1)).timestamp()*1000)
 c.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1]+[(i+k)%10+1 for k in range(10)])
for i in range(400):
 c.execute("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES(?,1,'INFO','R73_TEST','HOST','eligible old log',?)",[f'R73-{i}',bytes(8<<20) if i==0 else 'test-detail-'+str(i)])
c.commit();c.close()
cmd=[str(j/'java'),'-Xmx64m','-Djava.library.path='+str(w/'host-native'),'-cp',str(tests)+':'+cp,'com.ethk3.novaonepro.RetentionClosureR73',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'files'),str(o/'result.json')]
with (o/'test.log').open('w') as f:rc=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT).returncode
(o/'run.json').write_text(json.dumps({'exit_code':rc,'command':cmd,'log_sha256':hashlib.sha256((o/'test.log').read_bytes()).hexdigest()},indent=2)+'\n')
print('RETENTION_EXIT',rc,flush=True)
if rc:print((o/'test.log').read_text()[-5000:])
raise SystemExit(rc)
