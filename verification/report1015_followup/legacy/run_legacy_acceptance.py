#!/usr/bin/env python3
"""Tests/transport remain separate from sealed actual Gradle product classes."""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess, sqlite3

R=Path(__file__).resolve().parents[3]
p=argparse.ArgumentParser()
for name in ('config','schema','migration-schema','out'):
    p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();c=json.loads(a.config.read_text());out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
product=Path(c['product']);tests=out/'tests';tests.mkdir();cp=str(tests)+':'+c['classpath']
sha=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
before={str(f.relative_to(product)):sha(f)for f in product.rglob('*.class')}
assert before,'ACTUAL_GRADLE_PRODUCT_REQUIRED'
sources=[R/'verification/r72/SettlementProgressR72.java',R/'verification/report1015_followup/legacy/LegacySettlementAcceptanceR79.java',R/'verification/report1015_followup/legacy/Batch34EpochAcceptanceR79.java',R/'verification/report1015_r79/HistoricalOutcomeR79.java']
cmd=[c['javac'],'-encoding','UTF-8','-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(tests),*map(str,sources)]
(out/'compile-command.json').write_text(json.dumps(cmd,indent=2))
with (out/'compile.log').open('w')as log:subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,check=True)
for root in (tests,Path(c['tests']),Path(c['adapters'])):
    assert not set(before)&{str(f.relative_to(root))for f in root.rglob('*.class')},'PRODUCT_CLASS_OVERLAY'
results=[]
def run(name,klass,db,mode):
    command=[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.'+klass,c['bridge'],str(db),str(mode)]
    (out/(name+'-command.json')).write_text(json.dumps(command,indent=2))
    with (out/(name+'.log')).open('w')as log:z=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=300)
    results.append({'name':name,'exit_code':z.returncode,'log_sha256':sha(out/(name+'.log'))})
    print(name,'PASS'if z.returncode==0 else 'FAIL',flush=True)
    return z.returncode==0
groups=[('legacy-negative-%02d'%i,'LegacySettlementAcceptanceR79','negative:'+str(i),a.schema)for i in range(23)]+[
    *[('legacy-'+mode,'LegacySettlementAcceptanceR79',mode,a.schema) for mode in ('head-valid','head-retained','head-canonical-crud','head-owner','head-long','head-restart','head-ambiguous','head-state','head-proof')],
    ('legacy-positive','LegacySettlementAcceptanceR79','positive',a.schema),
    ('legacy-mirror-mutation','LegacySettlementAcceptanceR79','mirror-mutation',a.schema),
    ('legacy-current-quarantine','LegacySettlementAcceptanceR79','current-quarantine',a.schema),
    ('legacy-unproven-head','LegacySettlementAcceptanceR79','unproven',a.schema),
    ('legacy-algorithm','LegacySettlementAcceptanceR79','algorithms',a.schema),
    ('legacy-backlog','LegacySettlementAcceptanceR79','backlog',a.schema),
    ('legacy-ready-upgrade','LegacySettlementAcceptanceR79','migration',a.migration_schema),
    ('r72-positive-late-restart','SettlementProgressR72',out/'r72-result.json',a.schema),
    ('batch34-epoch','Batch34EpochAcceptanceR79','repair',a.schema)]
for name,klass,mode,seed in groups:
    db=out/(name+'.db');shutil.copy2(seed,db);run(name,klass,db,mode)
db=out/'remote-r79-historical.db';shutil.copy2(a.schema,db)
remote_seed_fk=[]
if run('remote-r79-seed','HistoricalOutcomeR79',db,'seed'):
    with sqlite3.connect(db)as conn:remote_seed_fk=conn.execute('PRAGMA foreign_key_check').fetchall()
    # The unchanged remote negative fixture deletes its eighth original proof.
    # If CP4 resource measurement ran, that deliberate corruption creates this orphan.
    assert all(x[0]=='research_cp4_candidate_resource' and x[2]=='research_candidate_execution_r932' for x in remote_seed_fk) and len(remote_seed_fk)<=1,'UNEXPECTED_SEED_CORRUPTION'
    run('remote-r79-preserved','HistoricalOutcomeR79',db,'repair')
after={str(f.relative_to(product)):sha(f)for f in product.rglob('*.class')}
assert before==after,'SEALED_PRODUCT_CHANGED'
checks={}
for f in out.glob('*.db'):
    with sqlite3.connect(f)as db:checks[f.name]={'quick_check':db.execute('PRAGMA quick_check').fetchall(),'foreign_key_check':db.execute('PRAGMA foreign_key_check').fetchall()}
summary={'scope':'ACTUAL_GRADLE_PRODUCT_SYNTHETIC_SQLITE_NOT_DEVICE_NATURAL_FUTURE_OR_REAL','product_classes':len(before),'product_unchanged':True,'product_overlay':False,'source_hashes':{str(f.relative_to(R)):sha(f)for f in sources},'results':results,'database_checks':checks,'unchanged_remote_negative_fixture_seed_fk':remote_seed_fk,'remote_fixture_note':'Original remote missing-proof test deliberately deletes proof; any CP4 orphan must preexist repair and remain exactly unchanged. All new fixtures require empty FK check.','remaining_scope':['Host synthetic recovery tests are not device performance or natural Future evidence.']}
(out/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
(out/'product_hashes.json').write_text(json.dumps(before,indent=2)+'\n')
assert all(x['exit_code']==0 for x in results),'LEGACY_ACCEPTANCE_FAILED_SEE_LOGS'
assert all(x['quick_check']==[('ok',)] and x['foreign_key_check']==(remote_seed_fk if name=='remote-r79-historical.db' else [])for name,x in checks.items()),'SQLITE_INTEGRITY_FAILURE'
print('LEGACY_ACCEPTANCE_PASS',len(results),'groups')
