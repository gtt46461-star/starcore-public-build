#!/usr/bin/env python3
"""Read-only SQLite snapshot collector regression. Generated fixtures are not phone evidence."""
import argparse,hashlib,itertools,json,pathlib,sqlite3,subprocess
p=argparse.ArgumentParser()
for k in ('jdk','classes','android-runtime','report','out'):p.add_argument('--'+k,type=pathlib.Path,required=True)
p.add_argument('--final-bytecode',action='store_true');p.add_argument('--baseline',action='store_true');a=p.parse_args()
assert not(a.final_bytecode and a.baseline)
root=pathlib.Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
classes=out/'classes';classes.mkdir();empty=out/'empty';empty.mkdir();dbdir=out/'fixtures';dbdir.mkdir()
H=lambda b:hashlib.sha256(b).hexdigest()
def product_hashes():
 return {str(f.relative_to(a.classes.resolve())):H(f.read_bytes()) for f in sorted(a.classes.resolve().rglob('*.class')) if any(f.name.startswith(n) for n in ('LiveResearchDiagnosisOrchestrator','RuntimeHealthLedgerR36','CrashRecorder'))}
before=product_hashes();assert before
# Production DDL comes from the supplied immutable report catalog. Omitted bodies are not invented.
names={'v57_k3_action_space_1200','v57_deferred_evidence_job','v57_analysis_request_r36','app_state','draws','v57_shadow_observation_r35','v57_oos_label_r35','v57_runtime_health_r35'};ddl={n:[] for n in names}
for f in sorted(a.report.glob('catalog-*.jsonl')):
 for line in f.open():
  r=json.loads(line)
  if r.get('kind')=='schema_sql_chunk' and r.get('type')=='table' and r.get('name') in names:ddl[r['name']].append((r['start_char'],r['text']))
sqls={n:''.join(x[1] for x in sorted(chunks)) for n,chunks in ddl.items()};assert all(sqls.values())
def insert(db,table,overrides):
 vals={r[1]:(0 if any(x in r[2] for x in ('INT','REAL','NUMERIC')) else b'' if r[2]=='BLOB' else '') for r in db.execute('PRAGMA table_info('+table+')') if r[3] and r[4] is None};vals.update(overrides)
 db.execute('INSERT INTO '+table+'('+','.join(vals)+') VALUES('+','.join('?' for _ in vals)+')',list(vals.values()))
cases=[('partial_pending',360,'CANONICAL_COMMITTED','PENDING'),('partial_running',360,'CANONICAL_COMMITTED','RUNNING'),('partial_retry',360,'CANONICAL_COMMITTED','RETRY_WAIT'),('partial_completed',360,'CANONICAL_COMMITTED','COMPLETED'),('empty_completed',0,'CANONICAL_COMMITTED','COMPLETED'),('partial_orphan',360,'CANONICAL_COMMITTED',None),('overflow_pending',1201,'CANONICAL_COMMITTED','PENDING'),('full_completed',1200,'CANONICAL_COMMITTED','COMPLETED'),('empty_requested',0,'REQUESTED',None),('missing_table',0,'CANONICAL_COMMITTED','PENDING')]
for name,count,request,deferred in cases:
 db=sqlite3.connect(dbdir/(name+'.db'))
 for table,sql in sqls.items():
  if name=='missing_table' and table=='v57_k3_action_space_1200':continue
  db.execute(sql)
 insert(db,'v57_analysis_request_r36',{'target_issue':'202610130694','base_issue':'202610130693','status':request,'generation':1,'request_at':1791891191807})
 if deferred:insert(db,'v57_deferred_evidence_job',{'target_issue':'202610130694','job_type':'FULL','status':deferred,'created_at':1791891191807,'updated_at':1791891191807,'record_hash':'fixture'})
 actions=[(pos,*triple) for pos in range(1,12) for triple in itertools.combinations(range(1,11),3)]
 for i,(pos,n1,n2,n3) in enumerate(actions[:count]):insert(db,'v57_k3_action_space_1200',{'target_issue':'202610130694','position':pos,'n1':n1,'n2':n2,'n3':n3,'rank_no':i+1,'fingerprint':str(i),'action_hash':str(i)})
 db.commit();db.close()
for variant in ('health_missing_predraw','health_existing_unsettled'):
 db=sqlite3.connect(dbdir/(variant+'.db'))
 for sql in sqls.values():db.execute(sql)
 for key in ['b13_fetch_beat_ms','b13_analysis_beat_ms','b13_settlement_beat_ms','b13_service_beat_ms']:insert(db,'app_state',{'state_key':key,'state_value':'1791891200000'})
 insert(db,'app_state',{'state_key':'r817_runtime_epoch_at','state_value':'1791886582918'})
 insert(db,'draws',{'issue':'202610130693','open_time':1791891183000,'fetch_time':1791891186500,'dirty':0,**{'n'+str(n):n for n in range(1,11)}})
 for issue,settled in [('202610130692',1),('202610130694',0)]+([('202610130693',0)] if variant=='health_existing_unsettled' else []):insert(db,'v57_shadow_observation_r35',{'target_issue':issue,'base_issue':str(int(issue)-1),'settled':settled,'pre_draw_frozen':1,'record_hash':issue,'game_code':'Binance1-PK10','position':6,'n1':2,'n2':5,'n3':8})
 insert(db,'v57_oos_label_r35',{'target_issue':'202610130692','strict_oos':1,'record_hash':'692','game_code':'Binance1-PK10','position':6,'pre_draw_frozen':1})
 insert(db,'v57_runtime_health_r35',{'event_type':'ANALYSIS_DURABLE_RETURNED_R923','event_at':1791891191807,'target_issue':'202610130694','elapsed_ms':41346,'status':'SUCCESS','record_hash':'runtime'})
 db.commit();db.close()
fixtures_before={f.name:H(f.read_bytes()) for f in dbdir.glob('*.db')}
# Canonical host adapter sends actual SQL to Python SQLite. Add truthful read-only URI connection
# for this report-only fixture; no product class is stubbed/reimplemented.
adapter=out/'SQLiteDatabase.java';s=(root/'verification/canonical/schema/SQLiteDatabase.java').read_text();s=s.replace('public String getPath(){return "HOST_SQLITE";}','public String getPath(){return "HOST_SQLITE";}\n    public boolean isReadOnly(){return true;}');adapter.write_text(s)
bridge=out/'sqlite_bridge.py';s=(root/'verification/canonical/schema/sqlite_bridge.py').read_text();s=s.replace("D=sqlite3.connect(sys.argv[1] if len(sys.argv)>1 else ':memory:');D.isolation_level=None","D=sqlite3.connect('file:'+sys.argv[1]+'?mode=ro',uri=True);D.isolation_level=None");bridge.write_text(s)
sources=[adapter,root/'verification/r36-stubs/android/content/ContentResolver.java',root/'verification/r77/diagnostic_classification.java'];product=['LiveResearchDiagnosisOrchestrator.java','RuntimeHealthLedgerR36.java','CrashRecorder.java']
if not a.final_bytecode:
 for name in product:
  src=root/'app/src/main/java/com/ethk3/novaonepro'/name
  if a.baseline:
   dest=out/name;dest.write_bytes(subprocess.check_output(['git','show','HEAD:'+str(src.relative_to(root))],cwd=root));src=dest
  sources.append(src)
cp=':'.join(map(str,[classes,a.classes.resolve(),a.android_runtime.resolve()]))
steps=[]
def run(label,cmd):
 log=out/(label+'.log')
 with log.open('w') as f:r=subprocess.run(list(map(str,cmd)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
 steps.append({'step':label,'exit_code':r.returncode,'log_sha256':H(log.read_bytes())});(out/'steps.json').write_text(json.dumps(steps,indent=2)+'\n')
 if r.returncode:print(log.read_text()[-5000:])
 return r.returncode
status=run('compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',empty,'-cp',cp,'-d',classes,*sources])
if status:raise SystemExit(status)
status=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.diagnostic_classification',bridge,dbdir,out/'result.json'])
after=product_hashes();assert before==after
fixtures_after={f.name:H(f.read_bytes()) for f in dbdir.glob('*.db')};assert fixtures_before==fixtures_after
if a.final_bytecode:assert not any((a.classes.resolve()/f.relative_to(classes)).exists() for f in classes.rglob('*.class') if f.relative_to(classes).parts[0]!='android')
(out/'identity.json').write_text(json.dumps({'mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'BASELINE_SOURCE_NEGATIVE' if a.baseline else 'ISOLATED_CHANGED_SOURCE_WITH_RETAINED_DEPENDENCY_BYTECODE','product_bytecode_unchanged':True,'database_bytes_unchanged':True,'product_before':before,'product_after':after,'source_sha256':{str(f):H(f.read_bytes()) for f in sources},'database_sha256':fixtures_before},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
