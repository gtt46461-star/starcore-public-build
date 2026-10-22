"""Focused R77 training acceptance against one sealed product class directory.
Host Android/SQLite adapters are transport only; no product overlay is allowed.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, sqlite3, subprocess
p=argparse.ArgumentParser()
for k in ('java','product','adapters','tests','android','bridge','seed','schema','assets','out'):
 p.add_argument('--'+k,type=Path,required=True)
a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
classes={str(q.relative_to(a.product)):hashlib.sha256(q.read_bytes()).hexdigest() for q in sorted(a.product.rglob('*.class'))}
assert 'com/ethk3/novaonepro/DbHelper.class' in classes,'Product classes are incomplete'
for root in (a.adapters,a.tests):
 collision=sorted(set(classes)&{str(q.relative_to(root)) for q in root.rglob('*.class')})
 assert not collision,'Host directory overrides product classes: '+str(collision)
cp=':'.join(str(x.resolve()) for x in (a.tests,a.adapters,a.product,a.android))
receipt={'scope':'SEALED_PRODUCT_CLASS_HOST_OWNER_TESTS_NOT_ANDROID_PHONE_OR_QUALITY','product_class_count':len(classes),'product_classset_sha256':hashlib.sha256(''.join(rel+'\0'+classes[rel]+'\n' for rel in sorted(classes)).encode()).hexdigest(),'classset_digest_encoding':'UTF8 sorted relative path + NUL + file sha256 + LF','runs':{}}
(a.out/'product-classes.json').write_text(json.dumps(classes,indent=2))
def clone(seed,path):
 assert not path.exists(),'Fresh output path required: '+str(path)
 s=sqlite3.connect(seed);d=sqlite3.connect(path);s.backup(d);d.close();s.close()
def run(name,klass,args,expected=0):
 cmd=[str(a.java.resolve()),'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.'+klass]+[str(x) for x in args]
 (a.out/(name+'-command.json')).write_text(json.dumps(cmd,indent=2))
 with (a.out/(name+'.log')).open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 receipt['runs'][name]={'returncode':r.returncode,'log_sha256':hashlib.sha256((a.out/(name+'.log')).read_bytes()).hexdigest()};(a.out/'receipt.json').write_text(json.dumps(receipt,indent=2))
 assert r.returncode==expected,name+' failed: '+str(a.out/(name+'.log'))
 assert 'HISTORY_ASSET_CONTEXT' not in (a.out/(name+'.log')).read_text(),'Missing application context in host fixture'
# The full actual continuity entry reconstructs each original CAS file and verifies its hash.
db=a.out/'continuity.db';clone(a.seed,db);app=a.out/'app'
for name,count in [('continuity',77),('restart',77),('complete',230)]:
 run(name,'training_ContinuityR77',[a.bridge,db,app,a.assets,a.out/name,count,'fast'])
c=sqlite3.connect(db);receipt['completed']={}
for label,sql in {'feature_rows':'SELECT COUNT(*) FROM research_training_feature_row_cp11','receipts':'SELECT COUNT(*) FROM research_development_receipt_v2','artifact_identities':'SELECT COUNT(*) FROM research_artifact_identity_v2','train_completed':"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='COMPLETED'"}.items():receipt['completed'][label]=c.execute(sql).fetchone()[0]
assert receipt['completed']=={'feature_rows':21120,'receipts':16,'artifact_identities':16,'train_completed':16},receipt['completed']
c.close()
for mode,klass,extra in [('page','training_PreparedPageR77',[]),('page-p0','training_PreparedPageR77',['p0']),('cache-bound','training_CacheBoundR77',[])]:
 d=a.out/(mode+'.db');clone(a.seed,d);run(mode,klass,[a.bridge,d,app,a.assets]+extra)
for mode in ('first','restart','low_budget'):
 db=a.out/('fairness.db' if mode!='low_budget' else 'low-budget.db')
 run('fairness-'+mode,'training_DirectorFairnessR77',[a.bridge,db,mode])
db=a.out/'online.db';clone(a.schema,db);run('online','training_OnlineContinuityR77',[a.bridge,db,a.out/'online-result.json',a.assets])
receipt['online']=json.loads((a.out/'online-result.json').read_text())
# Detect a concurrent rebuild/overwrite rather than binding success to a changed classset.
for rel,h in classes.items():assert hashlib.sha256((a.product/rel).read_bytes()).hexdigest()==h,'Product class changed during tests: '+rel
receipt['status']='PASS';(a.out/'receipt.json').write_text(json.dumps(receipt,indent=2));print(json.dumps(receipt,indent=2))
