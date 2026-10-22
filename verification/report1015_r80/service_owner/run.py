from pathlib import Path
import json,subprocess,hashlib,shutil,sys
r=Path(__file__).resolve().parents[3];s=Path(__file__).resolve().parent;c=json.loads(Path(sys.argv[1]).read_text());o=Path(sys.argv[2]).resolve();o.mkdir(parents=True,exist_ok=False);t=o/'tests';t.mkdir()
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest();p=Path(c['product']);before={str(f.relative_to(p)):h(f) for f in p.rglob('*.class')};cp=':'.join([str(t),c['classpath']]);cmds=[]
def run(cmd,log):
 with (o/log).open('w') as f:z=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 cmds.append({'argv':cmd,'exit_code':z.returncode,'log':log,'sha256':h(o/log)});print(log,z.returncode);print((o/log).read_text()[-3000:]);return z.returncode
rc=run([c['javac'],'-sourcepath',str(o/'empty'),'-cp',cp,'-d',str(t),str(s/'ServiceOwnerR80.java')],'compile.log')
if not rc:
 for mode in ['recovery','lock','expiry','integrity']:
  db=o/(mode+'.db');shutil.copy2(Path(sys.argv[1]).parent/'host-schema.db',db);run([c['java'],'-cp',cp,'com.ethk3.novaonepro.ServiceOwnerR80',c['bridge'],str(db),mode],mode+'.log')
after={str(f.relative_to(p)):h(f) for f in p.rglob('*.class')};collisions=[]
for root in [t,Path(c['tests']),Path(c['adapters'])]:collisions += sorted(set(before)&{str(f.relative_to(root)) for f in root.rglob('*.class')})
receipt={'commands':cmds,'product_unchanged':before==after,'product_overlays':collisions,'product_hashes':before,'source_sha256':h(s/'ServiceOwnerR80.java'),'scope':'Actual sealed product BusinessReconciler + real SQLite, synthetic full schema, Android startup bypassed. No real network/wagers. Writer deadline scope plus genuine concurrent product WriterGuard holder; no product overlay.'};(o/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
