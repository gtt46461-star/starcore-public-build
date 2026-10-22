#!/usr/bin/env python3
"""Actual normal Gradle product classes, full installed schema, real SQLite and clock.
Only Android transport and uniquely named test classes are compiled; no product overlay.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,shutil,sqlite3,subprocess
p=argparse.ArgumentParser()
p.add_argument('--config',type=Path,required=True);p.add_argument('--schema',type=Path,required=True)
p.add_argument('--out',type=Path,required=True);p.add_argument('--baseline',action='store_true')
a=p.parse_args();r=Path(__file__).resolve().parents[3];src=Path(__file__).resolve().parent
c=json.loads(a.config.read_text());o=a.out.resolve();o.mkdir(parents=True,exist_ok=False)
t=o/'tests';t.mkdir();product=Path(c['product']);sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
product_before={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')}
assert product_before,'normal Gradle product bytecode required'
cp=':'.join([str(t),c['tests'],c['adapters'],c['product'],c['android']])
sources=[src/'RetentionIndexResumeR80.java',*sorted((src/'adapters').rglob('*.java')),r/'verification/r73/adapters/android/system/Os.java',r/'verification/canonical/adapters/android/util/AtomicFile.java']
with (o/'compile.log').open('w') as f:subprocess.run([c['javac'],'-encoding','UTF-8','-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(t),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
for root in [t,Path(c['tests']),Path(c['adapters'])]:
 collisions=sorted(set(product_before)&{str(x.relative_to(root)) for x in root.rglob('*.class')})
 assert not collisions,('PRODUCT_OVERLAY',root,collisions)
results={}
def fixture(name,live=0,orphan=0,draws=False):
 db=o/(name+'.db');shutil.copy2(a.schema,db)
 with sqlite3.connect(db) as d:
  d.execute('DELETE FROM research_row_revision_v2')
  d.execute("DELETE FROM app_state WHERE state_key LIKE 'retention_cp12_%'")
  for i in range(1,live+1):d.execute("INSERT INTO diagnostic_log(rowid,log_key,created_at,level,module,code,message,detail) VALUES(?,?,1,'INFO','R80','HOST','live','')",(i,'r80-'+str(i)))
  for i in range(live+1,live+orphan+1):d.execute("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',?)",(i,))
  d.execute("CREATE TABLE r80_expected_generation AS SELECT source_rowid,generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid<=?",(live,))
  if draws:
   start=datetime.datetime(2026,10,7,tzinfo=datetime.timezone.utc)
   for i in range(5000):
    date=start+datetime.timedelta(days=i//1440);issue=date.strftime('%Y%m%d')+f'{i%1440+1:04}';at=int((start+datetime.timedelta(minutes=i+1)).timestamp()*1000)
    d.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1]+[(i+k)%10+1 for k in range(10)])
  d.commit()
 return db

def run(mode,db):
 cmd=[c['java'],'-Xmx192m','-cp',cp,'com.ethk3.novaonepro.RetentionIndexResumeR80',mode,c['bridge'],str(db),str(o/(mode+'-files')),str(o/(mode+'.json'))]
 with (o/(mode+'.log')).open('w') as f:proc=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 if proc.returncode:print((o/(mode+'.log')).read_text());raise SystemExit(proc.returncode)
 results[mode]=json.loads((o/(mode+'.json')).read_text());print(mode,results[mode],flush=True)
live=fixture('live-prefix',8192,257)
if a.baseline:run('baseline',live)
else:
 run('initial',live);run('resume',live)
 run('invariants',fixture('orphan-backlog',0,8192))
 run('empty',fixture('empty'))
 run('fairness',fixture('fairness',1,8192,draws=True))
product_after={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')}
assert product_before==product_after,'PRODUCT_BYTECODE_CHANGED_DURING_TEST'
summary={'status':'PASS','baseline_reproduction':a.baseline,'scope':'ACTUAL_GRADLE_PRODUCT_FULL_SCHEMA_REAL_SQLITE_HOST_NOT_PHONE','compile_overlay':False,'product_root':str(product),'product_unchanged':True,'governor_bytecode':{k:v for k,v in product_before.items() if '/DataRetentionGovernorR900' in k},'test_source_sha256':{str(x.relative_to(r)):sha(x) for x in sources},'results':results,'limits':'Synthetic full-schema host tests. Transport traces observe real SQL but do not measure Android device latency. Deadline fault injects 60 ms transport delay after a page; production 50 ms limit is unchanged. Service start is recorded at Android boundary; report request and queue are actual product. No user DB reclamation conclusion.'}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps({'status':summary['status'],'checks':sum(x['checks'] for x in results.values()),'modes':list(results)},indent=2))
