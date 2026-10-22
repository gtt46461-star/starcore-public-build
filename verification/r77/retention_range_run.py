#!/usr/bin/env python3
import argparse,hashlib,json,pathlib,sqlite3,subprocess,itertools,shutil,datetime,time
p=argparse.ArgumentParser()
for k in ('jdk','classes','schema','android-runtime','report','out'):p.add_argument('--'+k,type=pathlib.Path,required=True)
p.add_argument('--final-bytecode',action='store_true');p.add_argument('--fixture-root',type=pathlib.Path);a=p.parse_args();root=pathlib.Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(exist_ok=False);test=out/'classes';test.mkdir();fixtures=(a.fixture_root or out/'fixtures').resolve();fixtures.mkdir(exist_ok=False)
H=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
cp=':'.join(map(str,[test,a.schema/'classes',a.classes,a.android_runtime]));src=root/'app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java'
clock=root/'verification/canonical/adapters/android/os/SystemClock.java'
sources=[root/'verification/r77/RetentionRangeR77.java',clock]+([] if a.final_bytecode else [src]);before={str(f):H(f) for f in (a.classes/'com/ethk3/novaonepro').glob('DataRetentionGovernorR900*.class')};assert before
with (out/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-encoding','UTF-8','-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(test),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
# The test-class directory is first on the runtime classpath. Never inherit a
# counter clock from a supplied host schema; this proof is retained for replay.
clock_bytecode=subprocess.check_output([str(a.jdk/'bin/javap'),'-c','-p','-classpath',str(test),'android.os.SystemClock'],text=True)
assert clock_bytecode.count('java/lang/System.nanoTime')==2 and 'setForTest' not in clock_bytecode,'REAL_MONOTONIC_CLOCK_REQUIRED'
(out/'clock-bytecode.txt').write_text(clock_bytecode)
# Actual installed DDL/indexes; no triggers are needed for this read-only selection benchmark.
allrows=[json.loads(l) for f in sorted(a.report.glob('catalog-*.jsonl')) for l in f.open()];names={'draws','v57_k3_action_space_1200','research_row_revision_v2','v57_research_input_pin_r967','v57_research_task_r967','r952_candidate_predraw','r958_frozen_predraw','r952_research_outbox'}
base=out/'base.db';db=sqlite3.connect(base)
for r in allrows:
 if r.get('kind')=='schema_sql_chunk' and r.get('type')=='table' and r.get('name') in names:db.execute(r['text'])
for r in allrows:
 if r.get('kind')=='schema_sql_chunk' and r.get('type')=='index':
  try:db.execute(r['text'])
  except sqlite3.Error:pass
# Real report draw rows supply the exact 5000-row cutoff, including original typed values.
d=json.loads((a.report/'DIAGNOSTIC_SCOPE.json').read_text());draws=next(x['rows'] for x in d['recentEvidence'] if x['table']=='draws');cols=list(draws[0]);db.executemany('INSERT INTO draws('+','.join(cols)+')VALUES('+','.join('?' for _ in cols)+')',[[x.get(k) for k in cols] for x in draws]);cutoff=min(r['issue'] for r in draws)
db.execute('CREATE TABLE range_specimen(issue TEXT)');db.execute('CREATE INDEX range_specimen_issue ON range_specimen(issue)');db.executemany('INSERT INTO range_specimen VALUES(?)',[(None,),(cutoff,),('202610130693',)])
db.execute('CREATE TABLE range_unindexed(issue TEXT)');db.executemany('INSERT INTO range_unindexed VALUES(?)',[(None,),('',),('bad',),('202610090001',),(cutoff,)])
db.execute('CREATE TABLE range_time(at INTEGER)');db.execute('CREATE INDEX range_time_at ON range_time(at)');db.executemany('INSERT INTO range_time VALUES(?)',[(None,),(-1,),(0,),(1,),(100,)])
cols=[r for r in db.execute('PRAGMA table_info(v57_k3_action_space_1200)')];default={r[1]:(0 if any(x in r[2] for x in ('INT','REAL','NUMERIC')) else '') for r in cols if r[3] and r[4] is None};db.commit();db.close()
sourcebridge=(root/'verification/canonical/schema/sqlite_bridge.py').read_text()
# Count VM work at 100-op intervals per statement; lower/upper bounds remain explicit.
sourcebridge=sourcebridge.replace('import sys,sqlite3,base64','import sys,sqlite3,base64,json,time')
sourcebridge=sourcebridge.replace("sqlite3.connect(sys.argv[1] if len(sys.argv)>1 else ':memory:')","sqlite3.connect('file:'+sys.argv[1]+'?mode=ro',uri=True)")
sourcebridge=sourcebridge.replace('D.isolation_level=None','D.isolation_level=None\nwork=[0]\ndef progress():work[0]+=100;return 0\nD.set_progress_handler(progress,100)')
sourcebridge=sourcebridge.replace('c=D.execute(sql,args);cols=', 'work[0]=0;started=time.monotonic();c=D.execute(sql,args);cols=')
sourcebridge=sourcebridge.replace("   print('\\t'.join(['OK'", "   with open(sys.argv[1]+'.queries.jsonl','a') as log:log.write(json.dumps({'sql':sql,'args':args,'vm_lower':work[0],'vm_upper':work[0]+99,'elapsed_ms':(time.monotonic()-started)*1000,'row_count':len(rows),'first_row':rows[0] if rows else None},default=lambda value:{'blob_bytes':len(value)})+'\\n')\n   print('\\t'.join(['OK'")
bridge=out/'bridge.py';bridge.write_text(sourcebridge)
plans={};results=[];fixture_paths={};expected_hashes={};expected_counts={};actions=[(pos,*n) for pos in range(1,11) for n in itertools.combinations(range(1,11),3)];issues=sorted(r['issue'] for r in draws)[-1300:]
def insert(db,t,values):
 defs={r[1]:(0 if any(x in r[2] for x in ('INT','REAL','NUMERIC')) else '') for r in db.execute('pragma table_info('+t+')') if r[3] and r[4] is None};defs.update(values);db.execute('INSERT INTO '+t+'('+','.join(defs)+') VALUES('+','.join('?' for _ in defs)+')',list(defs.values()))
for count,mode in [(50000,'protected'),(1500000,'protected'),(50000,'mixed'),(50000,'pinned')]:
 name=mode+'-'+str(count);directory=fixtures/name;directory.mkdir();path=directory/'source.db';fixture_paths[name]=path;shutil.copy2(base,path);db=sqlite3.connect(path);assert db.execute('PRAGMA journal_mode=TRUNCATE').fetchone()==('truncate',);columns=list(default);assert 'target_issue' in columns
 def records():
  for i in range(count):
   values=default.copy();pos,n1,n2,n3=actions[i%1200];values.update(target_issue=issues[i//1200],position=pos,n1=n1,n2=n2,n3=n3,rank_no=i%1200+1,fingerprint=str(i),action_hash=str(i));yield [values[k] for k in columns]
 db.executemany('INSERT INTO v57_k3_action_space_1200('+','.join(columns)+') VALUES('+','.join('?' for _ in columns)+')',records())
 if mode in ('mixed','pinned'):
  for i in range(180):
   pos,n1,n2,n3=actions[i];insert(db,'v57_k3_action_space_1200',dict(target_issue='202610090001',position=pos,n1=n1,n2=n2,n3=n3,rank_no=i+1,fingerprint='old'+str(i),action_hash='old'+str(i)))
  if mode=='pinned':
   insert(db,'v57_research_task_r967',{'task_id':'active-pin','status':'PENDING'});insert(db,'v57_research_input_pin_r967',{'task_id':'active-pin','source_table':'v57_k3_action_space_1200','source_issue':'202610090001'})
  # Invalid or equal cutoff values must not be admitted as old valid input.
  for i,value in enumerate(['','bad','20261009000',cutoff]):insert(db,'v57_k3_action_space_1200',dict(target_issue=value,position=1,n1=1,n2=2,n3=3,rank_no=1,fingerprint='invalid'+str(i),action_hash='invalid'+str(i)))
 db.commit();plans[name]=db.execute('EXPLAIN QUERY PLAN SELECT 1 FROM v57_k3_action_space_1200 WHERE target_issue<? LIMIT 1',(cutoff,)).fetchall();db.close();journal=pathlib.Path(str(path)+'-journal');assert not journal.exists() or journal.stat().st_size==0,'FIXTURE_HOT_JOURNAL_AFTER_COMMIT';digest=H(path);print('FIXTURE_COMMITTED',name,path.stat().st_size,digest,flush=True)
 expected_hashes[name]=digest;expected_counts[name]=count+(184 if mode in ('mixed','pinned') else 0)
 cmd=[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.RetentionRangeR77',bridge,path,mode,out/(name+'.json')]
 with (out/(name+'.log')).open('w') as f:r=subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT)
 if r.returncode:print((out/(name+'.log')).read_text()[-5000:]);raise SystemExit(r.returncode)
 assert H(path)==digest,'SELECTOR_MUTATED_FIXTURE';result=json.loads((out/(name+'.json')).read_text());queries=[json.loads(l) for l in pathlib.Path(str(path)+'.queries.jsonl').read_text().splitlines()];result['range_queries']=[q for q in queries if q['sql'].startswith('SELECT 1 FROM "v57_k3_action_space_1200"')];result['selector_queries']=[q for q in queries if 'ORDER BY "v57_k3_action_space_1200".rowid LIMIT 128' in q['sql']];result['rows']=count;result['source_bytes_unchanged']=True;results.append(result)
 assert any('COVERING INDEX' in x[3] for x in plans[name])
 if mode=='protected':assert len(result['range_queries'])==1 and not result['selector_queries'] and result['range_queries'][0]['vm_upper']<1000
# Replay the exact unchanged original selector on the same closed, read-only fixtures.
# This contrasts SQL work, not phone wall-clock latency.
original=next(x['selector_queries'][0] for x in results if x['mode']=='mixed');baseline=[]
for count in (50000,1500000):
 path=fixture_paths['protected-'+str(count)];digest=H(path);assert digest==expected_hashes['protected-'+str(count)];probe=sqlite3.connect('file:'+str(path)+'?mode=ro',uri=True);work=[0]
 def progress():work[0]+=100;return 0
 probe.set_progress_handler(progress,100);started=time.monotonic();rows=probe.execute(original['sql'],original['args']).fetchall();elapsed=(time.monotonic()-started)*1000
 plan=probe.execute('EXPLAIN QUERY PLAN '+original['sql'],original['args']).fetchall();probe.close();assert not rows and H(path)==digest
 baseline.append({'rows':count,'vm_lower':work[0],'vm_upper':work[0]+99,'elapsed_ms':elapsed,'plan':plan,'empty_result_unchanged':True})
assert baseline[1]['vm_lower']>baseline[0]['vm_lower']*20
# Reopen the prepared mixed fixture in another JVM; page identity/cursor request must be unchanged.
path=fixture_paths['mixed-50000'];cmd=[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.RetentionRangeR77',bridge,path,'mixed',out/'mixed-reopened.json']
with (out/'mixed-reopened.log').open('w') as f:subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT,check=True)
assert (out/'mixed-50000.json.page.json').read_bytes()==(out/'mixed-reopened.json.page.json').read_bytes()
after={str(f):H(f) for f in (a.classes/'com/ethk3/novaonepro').glob('DataRetentionGovernorR900*.class')};assert before==after
for name,path in fixture_paths.items():
 assert H(path)==expected_hashes[name],'FIXTURE_CHANGED_AFTER_CASE:'+name
 probe=sqlite3.connect('file:'+str(path)+'?mode=ro',uri=True);assert probe.execute('SELECT COUNT(*) FROM v57_k3_action_space_1200').fetchone()==(expected_counts[name],),'FIXTURE_ROWS_CHANGED_AFTER_CASE:'+name;assert probe.execute('PRAGMA quick_check(1)').fetchone()==('ok',);probe.close();assert H(path)==expected_hashes[name]
result={'status':'PASS','mode':'FINAL_BYTECODE_NO_PRODUCT_OVERLAY' if a.final_bytecode else 'CHANGED_GOVERNOR_SOURCE_WITH_RETAINED_PRODUCT_DEPENDENCIES','scope':'CURRENT_PROTECTED_RANGE_PREFILTER_NOT_GENERAL_SQL_DEADLINE_OR_PHONE_TIMING','results':results,'plans':plans,'original_selector_baseline':baseline,'fresh_jvm_page_identity_unchanged':True,'product_before':before,'product_after':after,'source_hash':H(src),'fixture_hashes':expected_hashes,'fixture_paths':{k:str(v) for k,v in fixture_paths.items()},'bridge_hash':H(bridge),'clock':{'source_sha256':H(clock),'class_sha256':H(test/'android/os/SystemClock.class'),'javap_sha256':H(out/'clock-bytecode.txt'),'explicit_real_nanoTime_adapter':True}}
(out/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('product_before','product_after','plans')},indent=2))
