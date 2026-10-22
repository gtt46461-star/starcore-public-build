#!/usr/bin/env python3
"""Original R980 source DDL and owner facts -> actual current migration owners twice."""
from pathlib import Path
import argparse,subprocess,json,hashlib,sqlite3,shutil,base64
p=argparse.ArgumentParser();p.add_argument('--baseline-schema',type=Path,required=True);p.add_argument('--current-classes',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args();src=Path(__file__).resolve().parents[2];jdk=Path('/tmp/starcore-cdaf1f33/toolchain/jdk-17.0.16+8/bin');jar=Path('/tmp/starcore-cdaf1f33/toolchain/android-sdk/platforms/android-36/android.jar');bridge=src/'verification/canonical/schema/sqlite_bridge.py'
assert not a.out.exists() or not any(a.out.iterdir());a.out.mkdir(parents=True,exist_ok=True);cl=a.out/'drivers';cl.mkdir();records=[]
def run(name,cmd):
 with (a.out/(name+'.log')).open('w') as f:q=subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT,timeout=180)
 records.append({'operation':name,'command':list(map(str,cmd)),'exit_code':q.returncode});(a.out/'execution_receipts.json').write_text(json.dumps(records,indent=2));assert q.returncode==0,name
def cell(v):return {'blob':base64.b64encode(v).decode()} if isinstance(v,bytes) else v
def facts(db,columns=None):
 with sqlite3.connect(db) as c:
  result={}
  for (table,) in c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"):
   if columns is not None and table not in columns:continue
   names=columns[table]['columns'] if columns is not None else [x[1] for x in c.execute('PRAGMA table_info("'+table+'")')]
   rows=[json.dumps([cell(x) for x in row],sort_keys=True,separators=(',',':')) for row in c.execute('SELECT '+','.join('"'+n+'"' for n in names)+' FROM "'+table+'"')]
   result[table]={'columns':names,'rows':sorted(rows),'count':len(rows),'sha256':hashlib.sha256('\n'.join(sorted(rows)).encode()).hexdigest()}
  return result
oldcp=str(a.baseline_schema/'classes')+':'+str(jar);newcp=str(a.current_classes)+':'+str(jar)
run('compile-original-owner-fixture',[jdk/'javac','-cp',oldcp,'-d',cl,src/'verification/r986/PopulateBaselineR980.java'])
run('compile-current-migration-driver',[jdk/'javac','-cp',newcp,'-d',cl,src/'verification/canonical/schema/MigrationUpgradeActualCP12.java'])
db=a.out/'fixture.db';shutil.copy2(a.baseline_schema/'schema.db',db)
run('populate-through-original-R980-owners',[jdk/'java','-Xmx128m','-cp',str(cl)+':'+oldcp,'com.ethk3.novaonepro.PopulateBaselineR980',bridge,db]);before=facts(db);(a.out/'BEFORE_ORIGINAL_R980_FACTS.json').write_text(json.dumps(before,indent=2));shutil.copy2(db,a.out/'before-r980.db')
for i in range(2):run('current-migration-'+str(i),[jdk/'java','-Xmx128m','-cp',str(cl)+':'+newcp,'com.ethk3.novaonepro.MigrationUpgradeActualCP12',bridge,db])
after=facts(db,before);changes={t:{'before_count':v['count'],'after_count':after[t]['count'],'before_sha':v['sha256'],'after_sha':after[t]['sha256'],'removed_rows':list(set(v['rows'])-set(after[t]['rows'])),'added_rows':list(set(after[t]['rows'])-set(v['rows']))} for t,v in before.items() if v['sha256']!=after[t]['sha256']}
with sqlite3.connect(db) as c:
 integrity=c.execute('pragma integrity_check').fetchall();fk=c.execute('pragma foreign_key_check').fetchall();objects=dict(c.execute("SELECT type,count(*) FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' GROUP BY type"));sentinel=c.execute("SELECT state_value FROM app_state WHERE state_key='R986_ISOLATED_MIGRATION_SENTINEL'").fetchone()
receipt={'baseline_commit':'8e188bd4726ad205f971428876967de5f89fd633','baseline_source_java_count':758,'original_product_owner_population':True,'before_sha256':hashlib.sha256((a.out/'before-r980.db').read_bytes()).hexdigest(),'after_sha256':hashlib.sha256(db.read_bytes()).hexdigest(),'original_table_count':len(before),'changes':changes,'integrity':integrity,'foreign_keys':fk,'objects':objects,'sentinel_preserved':sentinel==('user-state-must-survive-actual-upgrade',),'scope':'Real original R980 DDL and product owners; synthetic populated DB. Actual current migration owners twice. Not customer DB, not original missing CP11 training fixture, not device upgrade.'};(a.out/'receipt.json').write_text(json.dumps(receipt,indent=2));(a.out/'AFTER_ORIGINAL_COLUMN_FACTS.json').write_text(json.dumps(after,indent=2));print(json.dumps(receipt,indent=2),flush=True)
assert integrity==[('ok',)] and not fk and receipt['sentinel_preserved']
# Original business rows may gain metadata elsewhere, but may never silently disappear.
assert not any(x['removed_rows'] for t,x in changes.items() if not t.startswith('r926_schema_')),'Review original row mutation before claiming migration PASS'
