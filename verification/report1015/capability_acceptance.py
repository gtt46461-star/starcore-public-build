"""Actual product owners; isolated report and synthetic fixtures, never phone/Future proof."""
from pathlib import Path
import json,sqlite3,shutil,subprocess,os
R=Path(__file__).resolve().parents[2];W=R.parent;c=json.loads((W/'audit_work/host/config.json').read_text())
O=W/'audit_work'/os.environ.get('STARCORE_CAPABILITY_OUT','capabilities-latest');O.mkdir(exist_ok=False)
sources=[R/p for p in ['verification/report1015/MultiscaleTrainingR78.java','verification/r77/MainlineMaintenanceR77.java','verification/r75/MainlineCaptureR75.java']]
with (O/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',c['tests'],*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
results=[]
def run(name,klass,args,ast=None):
 cmd=[c['java'],'-Xmx384m','-Djava.library.path='+str(W/'audit_work/native/build')]
 if ast:cmd+=['-Dstarcore.test.ast='+str(ast)]
 cmd+=['-cp',c['classpath'],'com.ethk3.novaonepro.'+klass,c['bridge'],*map(str,args)]
 (O/(name+'-command.json')).write_text(json.dumps(cmd,indent=2))
 with (O/(name+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=600)
 results.append({'name':name,'exit_code':z.returncode,'scope':'ACTUAL_PRODUCT_HOST_NOT_PHONE_OR_WIN_RATE'})
 (O/'summary.json').write_text(json.dumps(results,indent=2));print(name,z.returncode,flush=True)
 assert z.returncode==0,name+' failed; see log'
if os.environ.get('STARCORE_CAPABILITY_ONLY','')!='calibration':
 db=O/'multiscale.db';shutil.copy2(W/'audit_work/training-replay/replay.db',db)
 data=json.loads((W/'report_full/DIAGNOSTIC_SCOPE.json').read_text());e={x['table']:x['rows']for x in data['recentEvidence']}
 with sqlite3.connect(db)as d:
  triggers=d.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger'").fetchall()
  for name,_ in triggers:d.execute('DROP TRIGGER "'+name+'"')
  for table in ['v57_draw_canonical_r811','v57_settlement_task_r36']:
   rows=e[table];keys=list(rows[0]);d.executemany('INSERT INTO "'+table+'"('+','.join('"'+k+'"'for k in keys)+')VALUES('+','.join('?'for _ in keys)+')',[[x.get(k)for k in keys]for x in rows])
  for _,sql in triggers:d.execute(sql)
 del data,e
 run('multiscale','MultiscaleTrainingR78',[db,O/'app',R/'app/src/main/assets',R/'verification/report1015/failed_neural_parent.ast'])
if os.environ.get('STARCORE_CAPABILITY_ONLY','')!='multiscale':
 db=O/'calibration.db';shutil.copy2(W/'audit_work/acceptance-latest/seed.db',db);ast=W/'audit_work/acceptance-latest/fixture.ast'
 run('calibration-seed','MainlineMaintenanceR77',[db,'seed',O/'calibration-seed.json'],ast)
 run('calibration-resume','MainlineMaintenanceR77',[db,'resume',O/'calibration-resume.json'],ast)
print('CAPABILITY_ACCEPTANCE_PASS',len(results))
