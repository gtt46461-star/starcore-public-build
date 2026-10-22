#!/usr/bin/env python3
"""Run new recovery regressions on actual compiled products without product overlays."""
from pathlib import Path
import argparse,hashlib,json,re,shutil,sqlite3,subprocess

R=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser()
for k in ['config','schema','out']:p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--baseline-product',type=Path);a=p.parse_args();c=json.loads(a.config.read_text());o=a.out.resolve();o.mkdir(parents=True,exist_ok=False)
product=Path(c['product']);tests=o/'tests';tests.mkdir();cp=':'.join([str(tests),c['classpath']]);results=[]
H=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
before={str(x.relative_to(product)):H(x)for x in product.rglob('*.class')}
def receipt():
    data={'scope':'ACTUAL_GRADLE_PRODUCT_HOST_SYNTHETIC_SQLITE_NOT_PHONE_NATURAL_QUALITY_OR_REAL','product_classes':len(before),
      'product_manifest_sha256':hashlib.sha256(json.dumps(before,sort_keys=True).encode()).hexdigest(),'schema_sha256':H(a.schema),
      'source_hashes':{str(x.relative_to(R)):H(x)for x in (R/'verification/report1015_r79').glob('*.java')},'results':results,
      'product_overlay':False,'all_project_items_closed':False}
    (o/'summary.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def run(name,klass,db,mode='repair',baseline=False,extra=None):
    path=cp
    if baseline:path=path.replace(c['product'],str(a.baseline_product.resolve()))
    command=[c['java'],'-Xmx192m','-cp',path,'com.ethk3.novaonepro.'+klass,c['bridge'],str(db),mode]
    if extra is not None:command=extra
    (o/(name+'-command.json')).write_text(json.dumps(command,indent=2))
    with (o/(name+'.log')).open('w')as log:z=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=240)
    text=(o/(name+'.log')).read_text();counts=re.findall(r'checks=(\d+)',text)
    results.append({'name':name,'exit_code':z.returncode,'assertions':int(counts[-1])if counts else None,
        'role':'BASELINE_BUG_REPRODUCTION'if baseline else 'CURRENT_PRODUCT_REGRESSION','log_sha256':H(o/(name+'.log'))});receipt()
    print(name,z.returncode,flush=True);assert z.returncode==0,name+': see '+str(o/(name+'.log'))
sources=[R/'verification/report1015_r79'/x for x in ['HistoricalOutcomeR79.java','RevisionSweepR79.java','RequestHandoffR79.java','ComputeProfileR79.java']]
with (o/'compile.log').open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(tests),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
assert not set(before)&{str(x.relative_to(tests))for x in tests.rglob('*.class')},'PRODUCT_TEST_OVERLAY'
history=o/'historical-seed.db';shutil.copy2(a.schema,history);run('historical-seed','HistoricalOutcomeR79',history,'seed')
if a.baseline_product:run('historical-baseline','HistoricalOutcomeR79',history,'baseline',True)
run('historical-repair','HistoricalOutcomeR79',history)
seed=o/'revision-seed.db';shutil.copy2(a.schema,seed)
with sqlite3.connect(seed)as db:
    db.executemany("INSERT OR REPLACE INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES(?,1,'INFO','SYNTHETIC_HOST','R79','actual SQLite replacement','obsolete revision fixture')",[(f'R79-{i%32}',)for i in range(100000)])
for name,klass,original in [('revision','RevisionSweepR79',seed),('handoff','RequestHandoffR79',a.schema)]:
    if a.baseline_product:
        db=o/(name+'-baseline.db');shutil.copy2(original,db);run(name+'-baseline',klass,db,'baseline',True)
    db=o/(name+'-repair.db');shutil.copy2(original,db);run(name+'-repair',klass,db)
after={str(x.relative_to(product)):H(x)for x in product.rglob('*.class')};assert before==after,'PRODUCT_CHANGED_DURING_TEST'
receipt();print('R79_CURRENT_PRODUCT_HOST_RECOVERY_PASS')
