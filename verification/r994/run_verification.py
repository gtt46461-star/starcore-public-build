#!/usr/bin/env python3
"""Whole current Android product classes + actual installed APK assets; API transport adapters only."""
import argparse,hashlib,json,shutil,sqlite3,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for flag in ('source','product-classes','host-classes','android-jar','native-dir','schema','apk','out','java-home'):p.add_argument('--'+flag,type=Path,required=True)
a=p.parse_args();r=a.source.resolve();o=a.out.resolve()
if o.exists() and any(o.iterdir()):raise SystemExit('OUT_MUST_BE_EMPTY')
o.mkdir(parents=True,exist_ok=True);cl=o/'drivers';cl.mkdir();jdk=a.java_home.resolve()/'bin'
cp=':'.join(map(str,[cl,a.product_classes.resolve(),a.host_classes.resolve(),a.android_jar.resolve()]))
files=sorted((r/'verification/r994').rglob('*.java'))+[r/'verification/canonical/adapters/android/util/AtomicFile.java']
q=subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(cl),*map(str,files)],capture_output=True,text=True);(o/'compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit(q.returncode)
schema=o/'historical-seed.db';shutil.copyfile(a.schema,schema);d=sqlite3.connect(schema)
d.execute('INSERT INTO research_model_bundle_r929 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',('0'*64,'synthetic historical seed','Binance1-PK10','1'*64,'2'*64,'0'*64,'INSTALLED_APK_SEED','SHA256withRSA',1,6,0,'202610040259','202610040260','ROLLED_BACK','synthetic',1,1,'','3'*64))
d.execute('INSERT OR REPLACE INTO app_state(state_key,state_value) VALUES(?,?)',('r929_model_bundle_previous','0'*64));d.commit();d.close()
before=o/'before-classes';before.mkdir();old=r/'construction/r994/before/app/src/main/java/com/ethk3/novaonepro'
q=subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(before),str(old/'ReportAttachmentClosureCP12.java'),str(old/'V51ArtifactBundleManifest.java')],capture_output=True,text=True);(o/'before-compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit(q.returncode)
results=[]
def run(case,classpath,name,args):
 cmd=[str(jdk/'java'),'-Xmx64m','-Djava.library.path='+str(a.native_dir.resolve()),'-cp',classpath,'com.ethk3.novaonepro.'+name,*map(str,args)]
 q=subprocess.run(cmd,capture_output=True,text=True,timeout=240);(o/(case+'.log')).write_text(q.stdout+q.stderr)
 results.append({'case':case,'exitCode':q.returncode,'command':cmd});print(case,q.returncode,flush=True);return q
for era,classpath in [('before',str(before)+':'+cp),('after',cp)]:
 hashes=[run(era+'-seed-'+variant,classpath,'SeedCredentialIndependenceR994',args).stdout.strip() for variant,args in [('empty',[]),('configured',['SyntheticApiFixtureKey994'])]]
 results.append({'case':era+'-seed-identity','stable':hashes[0]==hashes[1]})
 run(era+'-capture',classpath,'ReportInstalledAssetsR994',[o/era,a.apk.resolve(),schema,r/'verification/r994/sqlite_readonly_bridge.py'])
run('after-recovery',cp,'RecoverySnapshotR994',[o/'recovery',schema])
(o/'receipt.json').write_text(json.dumps({'scope':'current entire product classes; actual APK assets; real readonly SQLite; synthetic historical seed; DEVICE_NOT_VERIFIED','apkSha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'results':results},indent=2))
expected={x['case']:x for x in results}
passed=(not expected['before-seed-identity']['stable'] and expected['after-seed-identity']['stable'] and expected['before-capture']['exitCode']!=0 and all(x.get('exitCode',0)==0 for x in results if x['case'].startswith('after-')))
raise SystemExit(0 if passed else 1)
