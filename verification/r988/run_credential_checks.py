#!/usr/bin/env python3
"""Run actual Service request producer and journal command consumer using fresh current classes."""
from pathlib import Path
import argparse,subprocess,json,hashlib,time
p=argparse.ArgumentParser();p.add_argument('--classes',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
src=Path(__file__).resolve().parents[2];jdk=Path('/tmp/starcore-cdaf1f33/toolchain/jdk-17.0.16+8/bin');jar=Path('/tmp/starcore-cdaf1f33/toolchain/android-sdk/platforms/android-36/android.jar');assert not a.out.exists() or not any(a.out.iterdir());a.out.mkdir(parents=True,exist_ok=True);drivers=a.out/'drivers';drivers.mkdir();cp=str(a.classes)+':'+str(jar)
sources=[src/'verification/canonical/adapters/android/util/Base64.java',src/'verification/r988/PrivateCredentialConsumerR988.java'];cmd=[str(jdk/'javac'),'-cp',cp,'-d',str(drivers),*map(str,sources)]
with (a.out/'compile.log').open('w')as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
records=[{'operation':'compile_drivers_and_android_transport_only','command':cmd,'exit_code':q.returncode}];assert q.returncode==0
cmd=[str(jdk/'java'),'-Xmx128m','-cp',str(drivers)+':'+cp,'com.ethk3.novaonepro.PrivateCredentialConsumerR988',str(a.out/'files')];t=time.monotonic()
with (a.out/'execution.log').open('w')as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=90)
records.append({'operation':'actual_async_private_owner_key_provider_and_api_url_consumer','command':cmd,'exit_code':q.returncode,'elapsed_s':time.monotonic()-t,'scope':'Synthetic JDK RSA/file storage port; actual async configure/reload owner, provider and ApiClient consumer; NO_HTTP; AndroidKeyStore/UI/device lifecycle DEVICE_NOT_VERIFIED'})
paths=['app/src/main/java/com/ethk3/novaonepro/'+n for n in ('KeyProvider.java','PrivateApiCredentialCP12.java','ApiClient.java','StarcoreApplication.java','BootReceiver.java','MainActivity.java','ApiCredentialSettingsCP12.java')];receipt={'operations':records,'status':'PASS' if q.returncode==0 else 'FAIL','product_source_sha256':{x:hashlib.sha256((src/x).read_bytes()).hexdigest() for x in paths},'test_source_sha256':{str(x.relative_to(src)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources}}
(a.out/'receipt.json').write_text(json.dumps(receipt,indent=2));print((a.out/'execution.log').read_text());raise SystemExit(q.returncode)
