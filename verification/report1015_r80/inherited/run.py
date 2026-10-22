#!/usr/bin/env python3
"""Unchanged inherited owner/outbox contracts on an explicit sealed Gradle product.

Only unique test classes are compiled. SQLite fixtures and AST/model bytes stay
outside the repository; published evidence is hashes, commands, exits and logs.
"""
from pathlib import Path
import argparse, datetime, hashlib, json, shutil, sqlite3, subprocess, time

ROOT=Path(__file__).resolve().parents[3]
p=argparse.ArgumentParser()
for flag in ('config','schema','ast','out'):p.add_argument('--'+flag,type=Path,required=True)
a=p.parse_args(); out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
cfg=json.loads(a.config.read_text());product=Path(cfg['product']).resolve()
tests=out/'tests';tests.mkdir();empty=out/'empty';empty.mkdir()

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def manifest(p,pattern='*.class'):return {str(f.relative_to(p)):sha(f) for f in sorted(p.rglob(pattern)) if f.is_file()}
def write(name,value):(out/name).write_text(json.dumps(value,indent=2)+'\n')
original=manifest(product);assert len(original)>3000 and 'com/ethk3/novaonepro/AutoFetchService.class' in original
source_root=ROOT/'app/src/main';source_before=manifest(source_root,'*')
cp=':'.join([str(tests),cfg['classpath']]);assert str(product) in map(lambda x:str(Path(x).resolve()),cp.split(':'))
for entry in cp.split(':'):
 d=Path(entry).resolve()
 if d.is_dir() and d!=product:assert not set(original).intersection(manifest(d)),f'PRODUCT_OVERLAY:{d}'
sources=[ROOT/x for x in [
 'verification/report1015/PostBatchR78.java','verification/report1015/ReservedBatchR78.java',
 'verification/r976/PostRetryHarnessR976.java','verification/r967/host/OutboxTransactionHarnessR967.java',
 'verification/r75/training_OutboxRecoveryR75.java',
 'verification/canonical/tests/audit_cp12/training/tests/ResidualOwnersContractCP12.java']]
dependencies=[ROOT/x for x in ['verification/r974/HostProductR974.java','verification/r75/MainlineCaptureR75.java',
 'verification/canonical/tests/audit_cp11/training/tests/TrainingFixtureCP11.java','verification/report1015/DrawIngestIdempotency.java',
 'verification/r982/PostProjectionRecoveryR982.java','verification/r77/training_DirectorFairnessR77.java',
 'verification/r77/CaptureDeadlineR77.java']]
# Resolve the original helper source without silently substituting its compiled implementation.
dependencies=[f if f.exists() else next((ROOT/'verification').rglob(f.name)) for f in dependencies]
records=[]

def run(name,cmd,timeout=240):
 write(name+'-command.json',cmd);start=time.monotonic()
 with (out/(name+'.log')).open('w') as log:
  try:result=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=timeout);code=result.returncode
  except subprocess.TimeoutExpired:log.write('\nHARNESS_TIMEOUT\n');code=124
 elapsed=round(time.monotonic()-start,3);(out/(name+'.exit')).write_text(str(code)+'\n')
 rec={'name':name,'exit_code':code,'status':'PASS' if code==0 else 'FAIL','seconds':elapsed,'log_sha256':sha(out/(name+'.log'))}
 records.append(rec);write('executions.json',records)
 print(json.dumps(rec),flush=True);return code

write('product_hashes.json',{'path':str(product),'sha256':original})
write('source_hashes.json',{'root':'app/src/main','sha256':source_before})
write('input_identity.json',{'started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'source_head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
 'product_classes':len(original),'product_manifest_sha256':hashlib.sha256(json.dumps(original,sort_keys=True).encode()).hexdigest(),
 'schema_sha256':sha(a.schema),'config_sha256':sha(a.config),'ast_sha256':sha(a.ast),
 'test_source_sha256':{str(f.relative_to(ROOT)):sha(f) for f in sources+dependencies+[Path(__file__)]},
 'classpath_entries':{entry:manifest(Path(entry)) if Path(entry).is_dir() else {'sha256':sha(Path(entry))} for entry in cfg['classpath'].split(':')},
 'product_overlay':False,'fixture_policy':'Original source assertions unchanged; original AST copied; current fresh product schema plus only required immutable atom metadata.'})
rc=run('compile',[cfg['javac'],'-encoding','UTF-8','-sourcepath',str(empty),'-cp',cp,'-d',str(tests),*map(str,sources)])
if rc:raise SystemExit(rc)
assert not set(original).intersection(manifest(tests))
ast=out/'fixture.ast';shutil.copy2(a.ast,ast)
seed=out/'seed.db';shutil.copy2(a.schema,seed)
asset=ROOT/'app/src/main/assets/cp4_atom_registry.jsonl'
required={n['atomId'] for n in json.loads(ast.read_text()[5:])['nodes'] if n.get('atomId')}
with sqlite3.connect(seed) as d:
 for line in asset.open():
  row=json.loads(line)
  if row['atomId'] not in required:continue
  s=row['spec'];canonical=json.dumps(s,sort_keys=True,separators=(',',':'),ensure_ascii=False)
  d.execute('INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)',
   [row['atomId'],s['family'],s['version'],canonical,s['position'],row['outputType'],row['minimumHistory'],row['specHash'],row['missingPolicy'],row['state']])
 assert d.execute('SELECT count(*) FROM research_atom_spec_v2').fetchone()[0]==len(required)
 d.commit()
write('fixture_identity.json',{'original_schema_sha256':sha(a.schema),'seed_sha256':sha(seed),'ast_sha256':sha(ast),'atom_registry_sha256':sha(asset),'atom_metadata_rows':len(required)})
base=[cfg['java'],'-Xmx384m','-Dstarcore.test.ast='+str(ast),'-cp',cp]
rows=[('post-batch','PostBatchR78',[]),('reserved-batch','ReservedBatchR78',[]),
 ('post-repair','PostProjectionRecoveryR982',[]),('draw-ingest','DrawIngestIdempotency',[]),
 ('fairness-low','training_DirectorFairnessR77',['low_budget']),
 *[('capture-'+mode,'CaptureDeadlineR77',[mode,str(out/('capture-'+mode+'.json'))]) for mode in ('timely','expired','crossing')],
 ('residual-owners','r952.ResidualOwnersContractCP12',[str(out/'residual-owners.json')]),
 ('outbox-transaction','r952.OutboxTransactionHarnessR967',[]),
 ('outbox-recovery','training_OutboxRecoveryR75',[str(out/'outbox-recovery.json'),str(ROOT/'app/src/main/assets')]),
 ('post-retry','PostRetryHarnessR976',[])]
for name,main,extra in rows:
 db=out/(name+'.db');shutil.copy2(seed,db)
 run(name,base+['com.ethk3.novaonepro.'+main,cfg['bridge'],str(db),*extra])
# Historical optional mini-schema fixtures predate current schema columns/tables.
# Keep their original failures and rerun identical assertion bodies on the explicit
# current product schema. Generated copies are uniquely named tests, not overlays.
adapted=out/'adapted_sources';adapted.mkdir()
adaptations=[]
for old,oldfile,replacements in [
 ('OutboxTransactionHarnessR967',ROOT/'verification/r967/host/OutboxTransactionHarnessR967.java',[
  ('new SQLiteDatabase(args[0])','new SQLiteDatabase(args[0],args[1])'),
  ('tx(d,()->schema(d));tx(d,()->{','tx(d,()->{'),
  ('snapshot_hash,prediction_mask,position,hit) VALUES(?,?,1,?,?,?,?,?,?,?,?,?,1)',
   'snapshot_hash,prediction_mask,position,hit,structure_hash,model_hash,code_hash,data_hash,captured_at) VALUES(?,?,1,?,?,?,?,?,?,?,?,?,1,?,?,?,?,1)'),
  ('input,role,snapshot,mask,pos});','input,role,snapshot,mask,pos,h(candidate),CandidateCompilerR952.Model.initial().hash(),h("code"),h("data")});')]),
 ('PostRetryHarnessR976',ROOT/'verification/r976/PostRetryHarnessR976.java',[
  ('new SQLiteDatabase(args[0])','new SQLiteDatabase(args[0],args[1])'),
  ('HostProductR974.schema(d,s);','')])]:
 new=old+'CurrentSchemaR80';text=oldfile.read_text();text=text.replace(old,new)
 for before,after in replacements:
  assert text.count(before)==1,(old,before)
  text=text.replace(before,after)
 target=adapted/(new+'.java');target.write_text(text)
 adaptations.append({'original_source':str(oldfile.relative_to(ROOT)), 'original_source_sha256':sha(oldfile),
  'generated_source':str(target),'generated_source_sha256':sha(target),'unique_class':new,
  'replacements':[{'before':b,'after':c} for b,c in replacements],
  'assertion_body_changes':False,'reason':'Use fresh current product schema; fill mandatory immutable synthetic identity columns omitted by historical mini-schema; preserve assertions and original labels/roles/lease logic'})
write('schema_adaptations.json',adaptations)
rc=run('compile-current-schema',[cfg['javac'],'-encoding','UTF-8','-sourcepath',str(empty),'-cp',cp,'-d',str(tests),*map(str,sorted(adapted.glob('*.java')))])
if rc==0:
 for name,main in [('outbox-transaction-current-schema','r952.OutboxTransactionHarnessR967CurrentSchemaR80'),
                   ('post-retry-current-schema','PostRetryHarnessR976CurrentSchemaR80')]:
  db=out/(name+'.db');shutil.copy2(seed,db)
  run(name,base+['com.ethk3.novaonepro.'+main,cfg['bridge'],str(db)])
# The historical R967 clock jump is not a monotonic-expiry simulation. Preserve
# its failure above, then exercise the real unchanged 45-second owner expiry.
original_adapted=adapted/'OutboxTransactionHarnessR967CurrentSchemaR80.java'
monotonic=adapted/'OutboxTransactionHarnessR967MonotonicExpiryR80.java'
text=original_adapted.read_text().replace('OutboxTransactionHarnessR967CurrentSchemaR80','OutboxTransactionHarnessR967MonotonicExpiryR80')
old='   tx(d,()->e[2]=ResearchOutboxR952.claim(d,e[1].lease+1));'
new='''   tx(d,()->e[2]=ResearchOutboxR952.claim(d,e[1].lease+1));
   check(e[2]==null,"WALL_CLOCK_FAST_FORWARD_CANNOT_STEAL_LIVE_MONOTONIC_OWNER");
   while(TimeSourceR811.monoNowMs()<=e[1].monotonicUntil)Thread.sleep(Math.max(1,Math.min(250,e[1].monotonicUntil-TimeSourceR811.monoNowMs()+1)));
   tx(d,()->e[2]=ResearchOutboxR952.claim(d,System.currentTimeMillis()));
   check(e[2]!=null&&e[2].attempt==e[1].attempt+1,"REAL_MONOTONIC_EXPIRY_RECLAIMS_NEXT_GENERATION");'''
assert text.count(old)==1;text=text.replace(old,new);monotonic.write_text(text)
write('monotonic_expiry_adaptation.json',{'original_schema_adapted_source_sha256':sha(original_adapted),
 'diagnostic_source_sha256':sha(monotonic),'replaced':old,'replacement':new,
 'original_assertions_preserved':True,'product_lease_changed':False,'clock_mocked':False,
 'reason':'Historical wall-clock-only jump does not expire a live current same-process monotonic owner. Retain original failure; add wall-jump safety and actual expiry checks before the original stale-owner assertions.'})
rc=run('compile-monotonic-expiry',[cfg['javac'],'-encoding','UTF-8','-sourcepath',str(empty),'-cp',cp,'-d',str(tests),str(monotonic)])
if rc==0:
 db=out/'outbox-transaction-monotonic-expiry.db';shutil.copy2(seed,db)
 run('outbox-transaction-monotonic-expiry',base+['com.ethk3.novaonepro.r952.OutboxTransactionHarnessR967MonotonicExpiryR80',cfg['bridge'],str(db)])
assert manifest(product)==original,'PRODUCT_CHANGED_DURING_TEST'
assert manifest(source_root,'*')==source_before,'PRODUCT_SOURCE_CHANGED_DURING_TEST'
assert sha(ast)==sha(a.ast),'ORIGINAL_AST_CHANGED'
with sqlite3.connect(seed) as d:integrity={'quick_check':d.execute('PRAGMA quick_check').fetchall(),'foreign_key_check':d.execute('PRAGMA foreign_key_check').fetchall()}
write('summary.json',{'status':'PASS' if all(r['exit_code']==0 for r in records) else 'FAIL',
 'scope':'ACTUAL_SEALED_GRADLE_PRODUCT_HOST_SQLITE_SYNTHETIC_NOT_PHONE_OR_FORECAST_QUALITY',
 'product_classes':len(original),'product_manifest_sha256':hashlib.sha256(json.dumps(original,sort_keys=True).encode()).hexdigest(),
 'product_unchanged':True,'product_source_unchanged':True,'product_overlay':False,'original_assertions_unchanged':True,
 'schema_integrity':integrity,'results':records})
raise SystemExit(0 if all(r['exit_code']==0 for r in records) else 1)
