#!/usr/bin/env python3
"""Execute actual current product report owners, JNI and filesystem against reproducible synthetic fixtures."""
import argparse,hashlib,json,subprocess,time
from pathlib import Path
p=argparse.ArgumentParser()
for flag in ('source','product-classes','host-classes','android-jar','native-dir','schema','out','java-home'):p.add_argument('--'+flag,type=Path,required=True)
a=p.parse_args();r=a.source.resolve();o=a.out.resolve()
if o.exists() and any(o.iterdir()):raise SystemExit('OUT_MUST_BE_EMPTY')
o.mkdir(parents=True,exist_ok=True);cl=o/'drivers';cl.mkdir();jdk=a.java_home.resolve()/'bin'
cp=':'.join(map(str,[a.product_classes.resolve(),cl,a.host_classes.resolve(),a.android_jar.resolve()]))
files=sorted((r/'verification/r992').glob('*.java'))+sorted((r/'verification/canonical/tests/audit_cp12/report/tests').glob('*.java'))+sorted((r/'verification/r993').glob('*.java'))+[r/'verification/canonical/adapters/android/util/AtomicFile.java',r/'verification/r986/ReportSafFinalizationR986.java',r/'verification/r986/ReportSafResumeIdentityR986.java']
cmd=[str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(cl),*map(str,files)]
q=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit(q.returncode)
cases=[]
for f in files:
 n=f.stem
 if n in ('AtomicFile','ReportMetadataRecoveryR993'):continue
 cases.append((n,[str(a.schema.resolve())] if n=='RetentionSchemaBudgetR993' else None,f))
cases += [('ReportMetadataRecoveryR993-orphan',['orphan'],r/'verification/r993/ReportMetadataRecoveryR993.java'),('ReportMetadataRecoveryR993-bulk',['bulk'],r/'verification/r993/ReportMetadataRecoveryR993.java')]
results=[]
for case,custom,f in cases:
 d=o/case;d.mkdir();n=f.stem
 if n=='ReportMetadataRecoveryR993':args=[str(d/'files'),*custom]
 elif n=='RetentionSchemaBudgetR993':args=custom
 elif n in ('ProductionSchemaBackupR992','SingleZipExportR992'):args=[str(a.schema.resolve()),str(d/'files')]
 elif n in ('ParserMissingTimeR992','ReportSemanticContractCP12'):args=[]
 else:args=[str(d/'files')]
 cmd=[str(jdk/'java'),'-Xmx64m','-Djava.library.path='+str(a.native_dir.resolve()),'-cp',cp,'com.ethk3.novaonepro.'+n,*args];start=time.monotonic()
 with(d/'execution.log').open('w')as log:q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=180)
 results.append({'case':case,'exitCode':q.returncode,'seconds':round(time.monotonic()-start,3),'testSha256':hashlib.sha256(f.read_bytes()).hexdigest(),'command':cmd,'scope':'Current entire Android release product bytecode first; actual JNI/filesystem, host Android/SQLite API adapters, synthetic fixtures; handset and userDB not verified'})
 (o/'receipt.json').write_text(json.dumps(results,indent=2));print(case,q.returncode,flush=True)
print('COUNTS',len(results),sum(x['exitCode']==0 for x in results),flush=True)
raise SystemExit(0 if all(x['exitCode']==0 for x in results)else 1)
