#!/usr/bin/env python3
"""Current fresh product report regression, with recorded commands/hashes/exit status."""
from pathlib import Path
import argparse,subprocess,json,hashlib,time
p=argparse.ArgumentParser();p.add_argument('--classes',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
src=Path(__file__).resolve().parents[2];jdk=Path('/tmp/starcore-cdaf1f33/toolchain/jdk-17.0.16+8/bin');jar=Path('/tmp/starcore-cdaf1f33/toolchain/android-sdk/platforms/android-36/android.jar')
assert not a.out.exists() or not any(a.out.iterdir()),'Fresh output required';a.out.mkdir(parents=True,exist_ok=True);cl=a.out/'drivers';cl.mkdir();cp=str(a.classes)+':'+str(jar)
tests=list((src/'verification/canonical/tests/audit_cp12/report/tests').glob('*.java'))+[src/'verification/r986/ReportSafFinalizationR986.java',src/'verification/r986/ReportSafResumeIdentityR986.java'];adapters=[src/'verification/canonical/adapters/android/util/AtomicFile.java'];cmd=[str(jdk/'javac'),'-cp',cp,'-d',str(cl),*map(str,adapters+tests)]
with (a.out/'compile.log').open('w') as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
records=[{'phase':'compile_test_drivers_only','command':cmd,'exit_code':q.returncode}];assert q.returncode==0
for test in tests:
 d=a.out/test.stem;d.mkdir();args=[] if test.stem=='ReportSemanticContractCP12' else [str(d/'files')];cmd=[str(jdk/'java'),'-Xmx128m','-cp',str(cl)+':'+cp,'com.ethk3.novaonepro.'+test.stem,*args];t=time.monotonic()
 with (d/'execution.log').open('w') as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 records.append({'test':test.stem,'command':cmd,'exit_code':q.returncode,'elapsed_s':time.monotonic()-t,'status':'PASS' if q.returncode==0 else 'FAIL','test_source_sha256':hashlib.sha256(test.read_bytes()).hexdigest(),'scope':'Actual current product bytecode; host filesystem transport only; DEVICE_NOT_VERIFIED'});(a.out/'receipt.json').write_text(json.dumps(records,indent=2));print(test.stem,q.returncode,flush=True)
assert all(x['exit_code']==0 for x in records)
