"""Run original-report DEV-repair positive and corrupt-input negative on separate databases."""
from pathlib import Path
import os,json,sqlite3,shutil,subprocess,hashlib
R=Path(__file__).resolve().parents[2];W=R.parent;c=json.loads((W/'audit_work/host/config.json').read_text());O=W/'audit_work'/os.environ.get('STARCORE_DEV_REPAIR_OUT','dev-repair');O.mkdir(exist_ok=False)
with (O/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',c['tests'],str(R/'verification/report1015/DevelopmentRepairR78.java')],stdout=f,stderr=subprocess.STDOUT,check=True)
seed=O/'seed.db'
with sqlite3.connect(W/'audit_work/training-replay-complete/replay.db')as src,sqlite3.connect(seed)as dst:src.backup(dst)
data=json.loads((W/'report_full/DIAGNOSTIC_SCOPE.json').read_text());e={x['table']:x['rows']for x in data['recentEvidence']}
with sqlite3.connect(seed)as d:
 triggers=d.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger'").fetchall()
 for name,_ in triggers:d.execute('DROP TRIGGER "'+name+'"')
 for table in ['v57_draw_canonical_r811','v57_settlement_task_r36']:
  rows=e[table];keys=list(rows[0]);d.executemany('INSERT INTO "'+table+'"('+','.join('"'+k+'"'for k in keys)+')VALUES('+','.join('?'for _ in keys)+')',[[x.get(k)for k in keys]for x in rows])
 for _,sql in triggers:d.execute(sql)
del data,e
results=[]
for mode in ['corrupt','positive']:
 db=O/(mode+'.db');shutil.copy2(seed,db);cmd=[c['java'],'-Xmx384m','-Djava.library.path='+str(W/'audit_work/native/build'),'-cp',c['classpath'],'com.ethk3.novaonepro.DevelopmentRepairR78',c['bridge'],str(db),str(O/(mode+'-app')),str(R/'app/src/main/assets'),mode]
 (O/(mode+'-command.json')).write_text(json.dumps(cmd,indent=2))
 with (O/(mode+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=420)
 results.append({'mode':mode,'exit_code':z.returncode,'log_sha256':hashlib.sha256((O/(mode+'.log')).read_bytes()).hexdigest(),'scope':'ORIGINAL_REPORT_ISOLATED_HOST_NOT_PHONE_OR_QUALITY'});(O/'summary.json').write_text(json.dumps(results,indent=2));print(mode,z.returncode,flush=True)
 assert z.returncode==0,'SEE_'+mode+'_LOG'
print('DEVELOPMENT_REPAIR_ACCEPTANCE_PASS')
