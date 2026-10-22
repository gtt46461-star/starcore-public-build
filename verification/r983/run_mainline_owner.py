#!/usr/bin/env python3
"""Isolated synthetic current-source certificate → Mainline/race regression. Never production evidence."""
import argparse,subprocess,shutil,json,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--classes',type=Path,required=True);p.add_argument('--fixture-db',type=Path,required=True);p.add_argument('--ast',type=Path,required=True);p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[2];v=Path(__file__).resolve().parent;o=a.out.resolve()
if o.exists() and any(o.iterdir()):raise SystemExit('OUTPUT_MUST_BE_EMPTY')
o.mkdir(parents=True,exist_ok=True);cl=o/'adapters';cl.mkdir();db=o/'fixture.db';shutil.copy2(a.fixture_db,db);records=[]
files=sorted((v/'android').rglob('*.java'))+[r/'verification/canonical/adapters/android/util/Base64.java',r/'verification/r982/FrozenAtomFixtureR982.java',r/'verification/r982/PairedCertificateFixtureR982.java']+sorted(v.glob('*.java'))
cp=str(cl)+':'+str(a.classes.resolve())+':'+str(a.android_jar.resolve())
def run(name,cmd):
 with (o/(name+'.log')).open('w') as f:q=subprocess.run(list(map(str,cmd)),stdout=f,stderr=subprocess.STDOUT,timeout=240)
 records.append({'execution':name,'command':list(map(str,cmd)),'exit_code':q.returncode})
 receipt={'status':'PASS' if all(x['exit_code']==0 for x in records) else 'FAIL','source_dirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=r,text=True).strip()),'product_input_sha256':{str(f.relative_to(r)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sorted((r/'app/src/main').rglob('*')) if f.is_file()},'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'executions':records,'product_sha256':hashlib.sha256((r/'app/src/main/java/com/ethk3/novaonepro/UnifiedResearchChoiceR932.java').read_bytes()).hexdigest(),'fixture_sha256':hashlib.sha256(a.fixture_db.read_bytes()).hexdigest(),'ast_sha256':hashlib.sha256(a.ast.read_bytes()).hexdigest(),'scope':'Host SQLite synthetic 360 Future rows, real product publishers/authorization/choice consumer/race fences. Includes actual REAL authorization and settlement only in isolated synthetic SQLite. No natural Future or Android scenario claim.'}
 (o/'receipt.json').write_text(json.dumps(receipt,indent=2))
 if q.returncode:raise SystemExit(q.returncode)
run('compile',['javac','-cp',cp,'-d',cl,*files])
def java(name,fixture,mode=None):
 cmd=['java','-Xmx512m','-Dstarcore.test.ast='+str(a.ast.resolve()),'-cp',cp,'com.ethk3.novaonepro.'+name,v/'typed_live_bridge.py',fixture]
 if mode:cmd.append(mode)
 run(name+('-'+mode if mode else ''),cmd)
java('CertificateExecutionFixtureR982',db);java('CertificateMainlineProbeR982',db)
for mode in ['repeat','revoke','draw']:
 fixture=o/(mode+'.db');shutil.copy2(db,fixture);java('CertificateMainlineGuardsR982',fixture,mode)
fixture=o/'runtime-chain.db';shutil.copy2(db,fixture);java('RuntimeConsumerChainR984',fixture)
fixture=o/'revoke-before-gate.db';shutil.copy2(db,fixture);java('RuntimeRevocationBeforeGateR985',fixture)
print('MAINLINE_OWNER_AND_RUNTIME_CHAIN_PASS')
