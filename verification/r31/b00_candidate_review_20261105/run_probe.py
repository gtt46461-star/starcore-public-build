#!/usr/bin/env python3
"""Re-run the retained synthetic bounded-URL negative on a read-only candidate snapshot."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=Path('/workspace/r31-integration-20261105'));a=p.parse_args()
root=a.repo.resolve();here=Path(__file__).resolve().parent
out=here/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');out.mkdir(parents=True)
tmp=Path(tempfile.mkdtemp(prefix='.r31-b00-bounded-probe-',dir='/workspace'));(tmp/'java-tmp').mkdir()
sources=[];hashes={}
for name in ['JsonR27','StatsR27','FailureDispositionR31','WaitKindR27','RuntimeFaultEvidenceR31']:
    rel=Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(name+'.java');data=(root/rel).read_bytes();dest=out/'source_snapshot'/rel;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data);sources.append(dest);hashes[str(rel)]=hashlib.sha256(data).hexdigest()
classes=tmp/'classes';classes.mkdir();commands=[]
for name,argv in [('compile',['java','-Djava.io.tmpdir='+str(tmp/'java-tmp'),'-m','jdk.compiler/com.sun.tools.javac.Main','-d',classes,*sources,here/'B00CandidateObservationProbeR31.java']),('probe',['java','-Djava.io.tmpdir='+str(tmp/'java-tmp'),'-cp',classes,'com.ethk3.novaonepro.strategyv2.B00CandidateObservationProbeR31'])]:
    env=dict(os.environ);env.pop('JAVA_TOOL_OPTIONS',None);r=subprocess.run(list(map(str,argv)),env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(out/(name+'.log')).write_text(r.stdout);commands.append({'name':name,'argv':list(map(str,argv)),'exit':r.returncode});print(name,r.returncode,r.stdout[-1000:])
    if r.returncode:break
receipt={'scope':'ACTUAL_CAPTURE_REDACTOR_PURE_JAVA_SYNTHETIC_CREDENTIAL_ONLY_NOT_ANDROID','sourceSha256':hashes,'commands':commands,'status':'PASS' if len(commands)==2 and commands[-1]['exit']==0 else 'FAIL','sourceChangedAfterSnapshot':[rel for rel,h in hashes.items() if hashlib.sha256((root/rel).read_bytes()).hexdigest()!=h]}
(out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print('EVIDENCE',out)
raise SystemExit(0 if receipt['status']=='PASS' and not receipt['sourceChangedAfterSnapshot'] else 1)
