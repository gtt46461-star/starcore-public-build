"""Existing upstream recovery and certificate tests against unchanged current product classes.
Certificate fixture explicitly inserts synthetic qualification evidence; never a phone-quality claim.
"""
from pathlib import Path
import os,json,sqlite3,shutil,subprocess
R=Path(__file__).resolve().parents[2];W=R.parent;c=json.loads((W/'audit_work/host/config.json').read_text());O=W/'audit_work'/os.environ.get('STARCORE_ADDITIONAL_OUT','additional-latest');O.mkdir(exist_ok=False)
A=O/'adapters';A.mkdir();T=O/'tests';T.mkdir();cp=':'.join([str(T),str(A),c['classpath']]);results=[]
def compile(sources,dest):
 with (O/('compile-'+dest.name+'.log')).open('w')as f:subprocess.run([c['javac'],'-sourcepath',str(O/'empty'),'-cp',cp,'-d',str(dest),*[str(R/p)for p in sources]],stdout=f,stderr=subprocess.STDOUT,check=True)
compile(['verification/r983/android/database/sqlite/SQLiteDatabase.java','verification/r983/android/os/Process.java','verification/canonical/adapters/android/util/AtomicFile.java'],A)
compile(['verification/r77/training_OnlineContinuityR77.java','verification/r983/CertificateMainlineProbeR982.java','verification/r983/CertificateMainlineGuardsR982.java','verification/r77/CertificateScopeR77.java','verification/r77/PairedCertificateFixtureR77.java','verification/r982/FrozenAtomFixtureR982.java','verification/r77/CertifiedEveryPeriodR77.java'],T)
for root in [A,T]:assert not any((Path(c['product'])/p.relative_to(root)).exists()for p in root.rglob('*.class')),'PRODUCT_OVERLAY'
def run(name,klass,db,extra=[]):
 cmd=[c['java'],'-Xmx512m','-Dstarcore.test.ast='+str(W/'audit_work/acceptance-latest/fixture.ast'),'-cp',cp,'com.ethk3.novaonepro.'+klass,c['bridge'],str(db),*map(str,extra)]
 with (O/(name+'.log')).open('w')as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=480)
 results.append({'name':name,'exit_code':z.returncode,'scope':'ACTUAL_PRODUCT_SYNTHETIC_TEST_EVIDENCE_NOT_NATURAL_QUALIFICATION_OR_USER_BET'});(O/'summary.json').write_text(json.dumps(results,indent=2));print(name,z.returncode,flush=True);assert z.returncode==0,name
seed=W/'audit_work/acceptance-latest/seed.db';db=O/'online.db';shutil.copy2(seed,db)
run('online-continuity','training_OnlineContinuityR77',db,[O/'online.json',R/'app/src/main/assets'])
fixture=O/'certificate.db';shutil.copy2(seed,fixture);run('qualification','CertificateScopeR77',fixture)
for name,klass,args in [('choice','CertificateMainlineProbeR982',[]),('repeat','CertificateMainlineGuardsR982',['repeat']),('revoke','CertificateMainlineGuardsR982',['revoke']),('draw-race','CertificateMainlineGuardsR982',['draw']),('three-periods','CertifiedEveryPeriodR77',[])]:
 db=O/(name+'.db');shutil.copy2(fixture,db);run(name,klass,db,args)
print('ADDITIONAL_ACCEPTANCE_PASS',len(results))
