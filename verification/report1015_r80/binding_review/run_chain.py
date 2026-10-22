#!/usr/bin/env python3
"""Compile unique independent test classes against normal Gradle product bytecode.
No product source compilation/overlay; no full build. All DBs are isolated synthetic.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess, time
p=argparse.ArgumentParser();p.add_argument('--config',type=Path,required=True);p.add_argument('--schema',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--ast',type=Path,required=True);p.add_argument('--modes',default='seed,capture_first,capture_resume,register,train');p.add_argument('--heap',default='256m');p.add_argument('--native',type=Path);a=p.parse_args()
r=Path(__file__).resolve().parents[3];src=Path(__file__).resolve().parent;c=json.loads(a.config.read_text());o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);t=o/'tests';t.mkdir(exist_ok=True)
product=Path(c['product']);sha=lambda x:hashlib.sha256(x.read_bytes()).hexdigest();before={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')}
assert any('BoundAtomLifecycleR80' in x for x in before),'CURRENT_ACTUAL_PRODUCT_CLASSES_REQUIRED'
cp=':'.join([str(t),c['tests'],c['adapters'],c['product'],c['android']]);sources=[src/'BoundChainReviewR80.java',src/'BoundReceiptOracleReviewR80.java',src/'BoundRetentionReviewR80.java',src/'BoundRejectionReviewR80.java',src/'BoundStallReviewR80.java',r/'verification/r73/adapters/android/system/Os.java',r/'verification/canonical/adapters/android/util/AtomicFile.java']
with (o/'compile.log').open('w') as f:subprocess.run([c['javac'],'-encoding','UTF-8','-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(t),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
native_adapters=o/'native-adapters';native_adapters.mkdir(exist_ok=True)
native_sources=[r/'verification/r983/android/database/sqlite/SQLiteDatabase.java',r/'verification/r996/android/system/Os.java']
with (o/'native-adapters-compile.log').open('w') as f:subprocess.run([c['javac'],'-encoding','UTF-8','-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(native_adapters),*map(str,native_sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
native_cp=str(native_adapters)+':'+cp
for root in [t,native_adapters,Path(c['tests']),Path(c['adapters'])]:assert not(set(before)&{str(x.relative_to(root)) for x in root.rglob('*.class')}),('PRODUCT_OVERLAY',root)
db=o/'chain.db'
if not db.exists():shutil.copy2(a.schema,db)
results=json.loads((o/'run_status.json').read_text()) if (o/'run_status.json').exists() else {}
for mode in a.modes.split(','):
 current_db=db
 if mode in ('stall','stall_register','stall_resume'):
  current_db=o/'stall.db'
  if not current_db.exists():shutil.copy2(db,current_db)
 if mode in ('rejection','rejection_resume'):
  current_db=o/'rejection.db'
  if not current_db.exists():shutil.copy2(db,current_db)
 if mode=='retirement':
  current_db=o/'retirement.db';shutil.copy2(db,current_db)
 if mode in ('retention','retention_resume'):
  current_db=o/'retention.db'
  if not current_db.exists():shutil.copy2(db,current_db)
 cmd=[c['java'],'-Xmx'+a.heap,'-Dstarcore.test.ast='+str(a.ast.resolve()),'-cp',cp,'com.ethk3.novaonepro.BoundChainReviewR80',mode,c['bridge'],str(current_db),str(o/'files'),str(r/'app/src/main/assets'),str(o/(mode+'.json'))]
 if mode=='oracle':cmd=[c['java'],'-Xmx'+a.heap,'-cp',cp,'com.ethk3.novaonepro.r952.BoundReceiptOracleReviewR80',c['bridge'],str(db),str(o/(mode+'.json'))]
 if mode in ('retention','retention_resume'):
  assert a.native and a.native.is_dir(),'FRESH_OR_HASH_VERIFIED_NATIVE_LIBRARY_REQUIRED'
  cmd=[c['java'],'-Xmx'+a.heap,'-Djava.library.path='+str(a.native.resolve()),'-cp',cp,'com.ethk3.novaonepro.r952.BoundRetentionReviewR80',c['bridge'],str(current_db),str(o/'archive-files'),str(o/(mode+'.json'))]+(['resume'] if mode=='retention_resume' else [])
  cmd[cmd.index('-cp')+1]=native_cp;cmd[cmd.index('com.ethk3.novaonepro.r952.BoundRetentionReviewR80')+1]=str(r/'verification/r983/typed_live_bridge.py')
  cmd.insert(1,'-Dr982.readonly.bridge='+str(r/'verification/r996/read_only_bridge.py'))
 if mode in ('rejection','rejection_resume','stall','stall_register','stall_resume'):
  assert a.native and a.native.is_dir(),'NATIVE_LIBRARY_REQUIRED'
  is_stall=mode.startswith('stall');klass='BoundStallReviewR80' if is_stall else 'BoundRejectionReviewR80'
  cmd=[c['java'],'-Xmx'+a.heap,'-Dstarcore.test.ast='+str(a.ast.resolve()),'-Djava.library.path='+str(a.native.resolve()),'-Dr982.readonly.bridge='+str(r/'verification/r996/read_only_bridge.py'),'-cp',native_cp,'com.ethk3.novaonepro.'+klass]
  if is_stall:cmd += [{'stall':'later','stall_register':'register','stall_resume':'resume'}[mode]]
  cmd += [str(r/'verification/r983/typed_live_bridge.py'),str(current_db),str(o/('stall-files' if is_stall else 'rejection-files')),str(r/'app/src/main/assets'),str(o/(mode+'.json')),str(o/('stall-archives' if is_stall else 'rejection-archives'))]
  if mode=='rejection_resume':cmd += ['resume']
 if a.native and not any(x.startswith('-Djava.library.path=') for x in cmd):cmd.insert(1,'-Djava.library.path='+str(a.native.resolve()))
 (o/(mode+'-command.json')).write_text(json.dumps(cmd,indent=2)+'\n');started=time.time()
 with (o/(mode+'.log')).open('w') as f:proc=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 result={'exit_code':proc.returncode,'elapsed_seconds':round(time.time()-started,3)}
 if (o/(mode+'.json')).exists():result['receipt']=json.loads((o/(mode+'.json')).read_text())
 results[mode]=result;(o/'run_status.json').write_text(json.dumps(results,indent=2)+'\n');print(mode,result,flush=True)
 if proc.returncode:print((o/(mode+'.log')).read_text()[-12000:]);raise SystemExit(proc.returncode)
after={str(x.relative_to(product)):sha(x) for x in product.rglob('*.class')};assert before==after,'PRODUCT_BYTECODE_CHANGED_DURING_TEST'
summary={'status':'PASS','scope':'ACTUAL_GRADLE_PRODUCT_ORIGINAL_PRODUCER_FULL_SCHEMA_SYNTHETIC_HOST','overlay':False,'heap':a.heap,'product_root':str(product),'product_unchanged':True,'source_sha256':{str(x.relative_to(r)):sha(x) for x in sources+native_sources},'product_sha256':{k:v for k,v in before.items() if any(s in k for s in ['BoundAtomLifecycleR80','FrozenCandidateAdapterR958','HistoricalTrainingReceiptCP12','ResearchDirectorR952'])},'results':results,'limits':'Synthetic original-producer host branch. Controlled generated draw arrival uses real clock and actual product capture before settlement; does not establish live Future, qualification, phone timing or REAL readiness.'};(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
