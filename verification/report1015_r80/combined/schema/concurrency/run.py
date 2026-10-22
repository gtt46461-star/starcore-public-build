#!/usr/bin/env python3
"""Normal-product regression. Only test classes and opt-in host Android Looper shim compile."""
import argparse,json,pathlib,subprocess,shutil,hashlib
p=argparse.ArgumentParser();p.add_argument('--config',required=True);p.add_argument('--schema',required=True);p.add_argument('--out',required=True);a=p.parse_args();c=json.load(open(a.config));out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=False);tests=out/'tests';tests.mkdir();src=pathlib.Path(__file__).parent.resolve();cp=str(tests)+':'+c['classpath'];runs=[]
def run(name,args):
 r=subprocess.run(args,capture_output=True,text=True,timeout=60);(out/(name+'.log')).write_text(r.stdout+r.stderr);runs.append({'name':name,'argv':args,'exit_code':r.returncode});(out/'commands.json').write_text(json.dumps(runs,indent=2));print(r.stdout+r.stderr,end='')
 if r.returncode:raise RuntimeError(name+' failed')
sources=[src/'android/os/Looper.java',src/'SchemaConcurrencyR80.java',src/'SchemaDurableRaceR80.java']
run('compile',[c['javac'],'-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(tests)]+[str(s) for s in sources])
assert all(x.name.startswith(('SchemaConcurrencyR80','SchemaDurableRaceR80','Looper')) for x in tests.rglob('*.class'))
for name,clazz in [('concurrency','SchemaConcurrencyR80'),('durable','SchemaDurableRaceR80')]:
 db=out/(name+'.db');shutil.copy2(a.schema,db)
 run(name,[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.'+clazz,c['bridge'],str(db)])
(out/'summary.json').write_text(json.dumps({'status':'PASS','production_overlay':False,'source_sha256':{s.name:hashlib.sha256(s.read_bytes()).hexdigest() for s in sources}},indent=2))
