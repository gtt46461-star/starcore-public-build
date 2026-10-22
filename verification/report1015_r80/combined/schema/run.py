#!/usr/bin/env python3
"""Actual combined product classes only; test overlay contains no production classes."""
import argparse,json,pathlib,subprocess,shutil,hashlib
p=argparse.ArgumentParser();p.add_argument('--config',required=True);p.add_argument('--schema',required=True);p.add_argument('--out',required=True);p.add_argument('--prior-schema');a=p.parse_args();c=json.load(open(a.config));out=pathlib.Path(a.out).resolve();out.mkdir(parents=True,exist_ok=False);tests=out/'tests';tests.mkdir();src=pathlib.Path(__file__).with_name('CombinedSchemaMigrationR80.java').resolve()
cp=str(tests)+':'+c['classpath'];commands=[];exits=[]
def run(name,args):
 commands.append(args);r=subprocess.run(args,capture_output=True,text=True);(out/(name+'.log')).write_text(r.stdout+r.stderr);exits.append({'name':name,'exit_code':r.returncode,'log':name+'.log'});(out/'exit_codes.json').write_text(json.dumps(exits,indent=2));print(r.stdout+r.stderr,end='');
 if r.returncode: raise RuntimeError(name+' failed '+str(r.returncode))
run('compile',[c['javac'],'-encoding','UTF-8','-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(tests),str(src)])
assert all('CombinedSchemaMigrationR80' in x.name for x in tests.rglob('*.class'))
db=out/'upgrade.db';shutil.copy2(a.schema,db)
for mode in ['prepare','restart','fault','nested-fault','pressure-expiry','fingerprints','trigger-dml']:
 run(mode,[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.CombinedSchemaMigrationR80',c['bridge'],str(db),mode])
if a.prior_schema:
 prior=out/'prior-shipped.db';shutil.copy2(a.prior_schema,prior)
 run('prior-shipped',[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.CombinedSchemaMigrationR80',c['bridge'],str(prior),'prior-shipped'])
 populated=out/'prior-populated.db';shutil.copy2(a.prior_schema,populated)
 run('prior-populated',[c['java'],'-Xmx256m','-cp',cp,'com.ethk3.novaonepro.CombinedSchemaMigrationR80',c['bridge'],str(populated),'prior-populated'])
(out/'commands.json').write_text(json.dumps(commands,indent=2));(out/'summary.json').write_text(json.dumps({'status':'PASS','product':c['product'],'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'source_overlay':False,'prior_shipped_test':bool(a.prior_schema)},indent=2))
