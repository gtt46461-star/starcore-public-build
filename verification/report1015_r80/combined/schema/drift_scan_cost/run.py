#!/usr/bin/env python3
"""Compile only the normal-product drift fast-path regression."""
import argparse,json,pathlib,subprocess,shutil,hashlib
p=argparse.ArgumentParser();p.add_argument('--config',required=True);p.add_argument('--schema',required=True);p.add_argument('--out',required=True);a=p.parse_args();c=json.load(open(a.config));out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=False);tests=out/'tests';tests.mkdir();src=pathlib.Path(__file__).with_name('DriftFastPathR80.java').resolve();cp=str(tests)+':'+c['classpath'];runs=[]
def run(name,args):
 r=subprocess.run(args,capture_output=True,text=True,timeout=60);(out/(name+'.log')).write_text(r.stdout+r.stderr);runs.append({'name':name,'argv':args,'exit_code':r.returncode});(out/'commands.json').write_text(json.dumps(runs,indent=2));print(r.stdout+r.stderr,end='')
 if r.returncode:raise RuntimeError(name+' failed')
run('compile',[c['javac'],'-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(tests),str(src)])
assert all(x.name.startswith('DriftFastPathR80') for x in tests.rglob('*.class'))
db=out/'fixture.db';shutil.copy2(a.schema,db)
run('regression',[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.DriftFastPathR80',c['bridge'],str(db)])
(out/'summary.json').write_text(json.dumps({'status':'PASS','cases':19,'production_overlay':False,'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest()},indent=2))
