#!/usr/bin/env python3
"""Run parity against already compiled product classes without replacing any product class."""
from pathlib import Path
import hashlib,json,subprocess,sys,os
R=Path(__file__).resolve().parents[3];W=R.parent
config=Path(os.environ.get('STARCORE_HOST_CONFIG',str(W/'audit_work/host/config.json')))
c=json.loads(config.read_text());O=Path(sys.argv[1]).resolve();O.mkdir(exist_ok=False)
report=W/'r79_inputs/report/DIAGNOSTIC_SCOPE.json'
data=json.loads(report.read_text());draws=next(x['rows']for x in data['recentEvidence']if x['table']=='draws')
with (O/'report_draws.tsv').open('w')as f:
 for row in sorted(draws,key=lambda r:r['issue']):f.write('\t'.join(str(row[k])for k in ['issue','open_time']+['n'+str(i)for i in range(1,11)])+'\n')
del data,draws
P=Path(c['product']);hashes={str(p.relative_to(P)):hashlib.sha256(p.read_bytes()).hexdigest()for p in P.rglob('*.class')}
T=O/'tests';T.mkdir();sources=[R/'verification/report1015_followup/queue'/name for name in ['AtomWindowParityR79.java','FeaturePageParityR79.java','FeaturePageMemoryR79.java']]
with (O/'compile.log').open('w')as log:
 cmd=[c['javac'],'-sourcepath',str(O/'empty'),'-cp',c['classpath'],'-d',str(T),*map(str,sources)]
 (O/'compile-command.json').write_text(json.dumps(cmd,indent=2))
 subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,check=True)
assert not(set(hashes)&{str(p.relative_to(T))for p in T.rglob('*.class')}),'PRODUCT_OVERLAY'
results=[]
for main,args in [('AtomWindowParityR79',[]),('FeaturePageParityR79',[R/'app/src/main/assets/cp4_atom_registry.jsonl',O/'report_draws.tsv']),('FeaturePageMemoryR79',[R/'app/src/main/assets/cp4_atom_registry.jsonl',O/'report_draws.tsv'])]:
 cmd=[c['java'],'-Xmx256m','-cp',str(T)+':'+c['classpath'],'com.ethk3.novaonepro.r952.'+main,*map(str,args)]
 (O/(main+'-command.json')).write_text(json.dumps(cmd,indent=2))
 run=subprocess.run(cmd,text=True,capture_output=True,timeout=180)
 (O/(main+'.log')).write_text(run.stdout+run.stderr)
 (O/(main+'-exit.json')).write_text(json.dumps({'exit_code':run.returncode})+'\n')
 assert run.returncode==0,main+' failed; see log'
 result=json.loads(run.stdout.strip().splitlines()[-1]);result['main']=main;results.append(result)
assert all(hashlib.sha256((P/name).read_bytes()).hexdigest()==h for name,h in hashes.items()),'PRODUCT_CHANGED_DURING_RUN'
summary={'scope':'ACTUAL_COMPILED_PRODUCT_NO_OVERLAYS_NOT_PHONE_OR_NATURAL_FUTURE','product_classes':len(hashes),'report_sha256':hashlib.sha256(report.read_bytes()).hexdigest(),'source_sha256':hashlib.sha256((R/'app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java').read_bytes()).hexdigest(),'results':results}
(O/'summary.json').write_text(json.dumps(summary,indent=2));(O/'product_hashes.json').write_text(json.dumps(hashes,indent=2));print(json.dumps(summary,indent=2))
