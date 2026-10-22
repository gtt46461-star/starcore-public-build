#!/usr/bin/env python3
"""Before/after regression using actual complete current release classes, never a copied provider."""
from pathlib import Path
import argparse,subprocess,json,hashlib
p=argparse.ArgumentParser()
for flag in ('source','product-classes','host-classes','android-jar','out','java-home'):p.add_argument('--'+flag,type=Path,required=True)
a=p.parse_args();r=a.source.resolve();o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);drivers=o/'drivers';drivers.mkdir(exist_ok=True);jdk=a.java_home/'bin'
cp=':'.join(map(str,[drivers,a.product_classes,a.host_classes,a.android_jar]))
sources=[r/'verification/r995/LegacyDualApiConsumerR995.java',r/'verification/r988/PrivateCredentialConsumerR988.java',r/'verification/canonical/adapters/android/util/Base64.java']
q=subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(drivers),*map(str,sources)],capture_output=True,text=True);(o/'compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit(q.returncode)
before=o/'before-classes';before.mkdir(exist_ok=True)
q=subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(before),str(r/'construction/r995/before/app/src/main/java/com/ethk3/novaonepro/KeyProvider.java')],capture_output=True,text=True);(o/'before-compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit(q.returncode)
results=[]
for name,classpath,mode in [('before',str(before)+':'+cp,'http'),('after-http-failover',cp,'http'),('after-stale-failover',cp,'stale')]:
 q=subprocess.run([str(jdk/'java'),'-Xmx64m','-cp',classpath,'com.ethk3.novaonepro.LegacyDualApiConsumerR995',mode,str(o/name)],capture_output=True,text=True,timeout=60)
 (o/(name+'.log')).write_text(q.stdout+q.stderr);results.append({'case':name,'exitCode':q.returncode});print(name,q.returncode)
ok=results[0]['exitCode']!=0 and all(x['exitCode']==0 for x in results[1:])
(o/'receipt.json').write_text(json.dumps({'status':'PASS' if ok else 'FAIL','scope':'ENTIRE_CURRENT_RELEASE_CLASSES_REAL_KEY_SELECTION_REAL_APICLIENT_REAL_GOVERNANCE_REAL_PARSER_SYNTHETIC_HTTP_STORAGE_TRANSPORT_NO_NETWORK','results':results,'testSha256':hashlib.sha256(sources[0].read_bytes()).hexdigest()},indent=2))
raise SystemExit(0 if ok else 1)
