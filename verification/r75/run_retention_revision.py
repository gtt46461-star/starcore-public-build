#!/usr/bin/env python3
from pathlib import Path
import argparse,subprocess,sqlite3,shutil,json,hashlib
p=argparse.ArgumentParser()
for x in ('schema','classes','jdk','out'):p.add_argument('--'+x,type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir();classes=out/'classes';classes.mkdir()
cp=':'.join([str(classes),str(a.schema.resolve()/'classes'),str(a.classes.resolve()),(a.schema/'host-classpath').read_text().strip().split(':')[-1]])
java=a.jdk/'bin';source=root/'app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java'
with (out/'compile.log').open('w') as f:subprocess.run([str(java/'javac'),'-cp',cp,'-d',str(classes),str(source),str(root/'verification/r75/RetentionRevisionR75.java'),str(root/'verification/r73/adapters/android/system/Os.java')],stdout=f,stderr=subprocess.STDOUT,check=True)
db=out/'live.db';shutil.copy2(a.schema/'schema-complete.db',db);c=sqlite3.connect(db)
for i in range(1,301):c.execute("INSERT INTO diagnostic_log(rowid,log_key,created_at,level,module,code,message,detail) VALUES(?,?,1,'INFO','R75','HOST','test','')",(i,'live-'+str(i)))
for i in range(1001,1261):c.execute("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',?)",(i,))
c.commit();c.close()
command=[str(java/'java'),'-Xmx128m','-cp',cp,'com.ethk3.novaonepro.RetentionRevisionR75',str(root/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(out/'result.json')]
with (out/'test.log').open('w') as f:r=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
(out/'run.json').write_text(json.dumps({'exit_code':r.returncode,'command':command,'product_sha256':hashlib.sha256(source.read_bytes()).hexdigest()},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(r.returncode)
