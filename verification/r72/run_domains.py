#!/usr/bin/env python3
"""Actual compiled product/domain contracts; explicitly isolated synthetic data."""
from pathlib import Path
import argparse, subprocess, json, re, shutil, sqlite3, datetime, hashlib
p=argparse.ArgumentParser();p.add_argument('--jdk',type=Path,required=True);p.add_argument('--schema',type=Path,required=True);p.add_argument('--workspace',type=Path,required=True);p.add_argument('--out',default='r70-domains');p.add_argument('--tests',default='');a=p.parse_args()
w=a.workspace.resolve();r=Path(__file__).resolve().parents[2];o=w/a.out;o.mkdir()
java=a.jdk.resolve()/'bin/java'
cp=(a.schema/'host-classpath').read_text().strip();cl=a.schema/'classes'
bridge=r/'verification/canonical/schema/sqlite_bridge.py';asset=r/'app/src/main/assets/cp4_atom_registry.jsonl';ast=o/'test-ast.txt'
names=['ModernModelContractTest','TestAstR997','TrainingFixtureCP11','FrozenAtomFixtureR982','CertificateScopeR70',
 'MatrixProjectionCP12Test','MatrixPersistenceCP12Test','ContinuityWheelContractCP12','ResidualOwnersContractCP12',
 'OosSettlementReplayContractCP12','ReservedModernCaptureContractCP12','DrawDependencyRollingCP12','MutationGraphContractCP12']
names.insert(4,"PairedCertificateFixtureR982")
sources={name:next((r/'verification').rglob(name+'.java')) for name in names}
with (o/'compile.log').open('w') as f:rc=subprocess.run([str(java.with_name('javac')),'-cp',cp,'-d',str(cl)]+list(map(str,sources.values()))+[str(r/'verification/canonical/adapters/android/os/Debug.java')],stdout=f,stderr=subprocess.STDOUT).returncode
if rc:raise SystemExit(rc)
receipts=[]
def run(name,args):
 source=sources[name];package=re.search(r'package\s+([\w.]+)\s*;',source.read_text()).group(1)
 cmd=[str(java),'-Xmx512m','-Dstarcore.test.ast='+str(ast),'-Djava.library.path='+str(w/'host-native'),'-cp',cp,package+'.'+name]+list(map(str,args))
 with (o/(name+'.log')).open('w') as f:code=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180).returncode
 receipts.append(dict(test=name,exit_code=code,source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),scope='ACTUAL_PRODUCT_SYNTHETIC_HOST_NOT_PHONE_OR_LIVE_QUALIFICATION'))
 (o/'results.json').write_text(json.dumps(receipts,indent=2)+'\n');print(name,code,flush=True)
 return code
run('TestAstR997',[asset,ast])
for name in names[5:]:
 if a.tests and name not in a.tests.split(','):continue
 db=o/(name+'.db');shutil.copy2(a.schema/'schema-complete.db',db);args=[bridge,db,o/(name+'.json')]
 if name in ('ReservedModernCaptureContractCP12','CertificateScopeR70'):
  c=sqlite3.connect(db)
  root=json.loads(ast.read_text()[5:]);required={n['atomId'] for n in root['nodes'] if n.get('atomId')}
  for line in asset.open():
   rec=json.loads(line)
   if rec['atomId'] not in required:continue
   spec=rec['spec'];canonical=json.dumps(spec,sort_keys=True,separators=(',',':'),ensure_ascii=False)
   c.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',[rec['atomId'],spec['family'],spec['version'],canonical,spec['position'],rec['outputType'],rec['minimumHistory'],rec['specHash'],rec['missingPolicy'],rec['state']])
  if name=='ReservedModernCaptureContractCP12':
   for i in range(342,422):c.execute('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[f'20260923{i:04}',1000*i,1000*i+1]+[(i+j)%10+1 for j in range(10)])
  c.commit();c.close()
 if name=='DrawDependencyRollingCP12':
  c=sqlite3.connect(db);start=datetime.datetime(2026,10,7,tzinfo=datetime.timezone.utc)
  for i in range(5000):
   date=start+datetime.timedelta(days=i//1440);issue=date.strftime('%Y%m%d')+f'{i%1440+1:04}';at=int((start+datetime.timedelta(minutes=i+1)).timestamp()*1000)
   c.execute('INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,raw_json,source_line,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[issue,at,at+1,at+1,'','R70_SYNTHETIC_ROLLING_FIXTURE']+[(i+j)%10+1 for j in range(10)])
  c.commit();c.close()
 if name=='MatrixProjectionCP12Test':args=[]
 if name=='MutationGraphContractCP12':args=[o/(name+'.json')]
 run(name,args)
raise SystemExit(1 if any(x['exit_code'] for x in receipts) else 0)
