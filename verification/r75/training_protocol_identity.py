"""Compare old/new normal protocols using the same build identity and observed report."""
from pathlib import Path
import argparse, subprocess, shutil, json, hashlib, sys
p=argparse.ArgumentParser()
for name in ('schema','classes','jdk','out','report'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--baseline',default='0dba780')
p.add_argument('--overlay',type=Path,action='append',default=[])
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=True)
for name in ('old-src','old-overlay','tests'):(o/name).mkdir(exist_ok=True)
original=(a.schema/'host-classpath').read_text().strip().split(':');android=original[-1]
base=':'.join(map(str,[a.schema.resolve()/'classes',a.classes.resolve(),android]))
old=o/'old-src/DevelopmentProductionCP12.java'
old.write_bytes(subprocess.check_output(['git','show',a.baseline+':app/src/main/java/com/ethk3/novaonepro/r952/DevelopmentProductionCP12.java'],cwd=r))
sources=[r/'verification/r75/training_ProtocolIdentityR75.java',r/'verification/r72/realtime/android/os/SystemClock.java']
with (o/'compile.log').open('w') as log:
    subprocess.run([str(a.jdk/'bin/javac'),'-cp',base,'-d',str(o/'old-overlay'),str(old)],stdout=log,stderr=subprocess.STDOUT,check=True)
    subprocess.run([str(a.jdk/'bin/javac'),'-cp',base,'-d',str(o/'tests'),*map(str,sources)],stdout=log,stderr=subprocess.STDOUT,check=True)
with (o/'fixture.log').open('w') as log:subprocess.run([sys.executable,str(r/'verification/r75/training_prepare_report.py'),'--report',str(a.report),'--schema',str(a.schema),'--out',str(o/'fixture')],stdout=log,stderr=subprocess.STDOUT,check=True)
commands=[]
for version in ('old','new'):
    overlay=[o/'old-overlay'] if version=='old' else [x.resolve() for x in a.overlay]
    cp=':'.join(map(str,[o/'tests',a.schema.resolve()/'classes',*overlay,a.classes.resolve(),android]))
    db=o/(version+'.db');shutil.copy2(o/'fixture/family-report.db',db)
    command=[str(a.jdk/'bin/java'),'-Djava.library.path='+str(r.parent/'host-native'),'-cp',cp,'com.ethk3.novaonepro.training_ProtocolIdentityR75',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/(version+'-inputs')),str(r/'app/src/main/assets'),str(o/(version+'.json'))];commands.append(command)
    with (o/(version+'.log')).open('w') as log:subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,check=True)
old_bytes=(o/'old.json').read_bytes();new_bytes=(o/'new.json').read_bytes()
assert old_bytes==new_bytes,'ORDINARY_PROTOCOL_BYTES_INPUT_HASH_OR_SEED_CHANGED'
result={'status':'PASS','ordinary_families':11,'baseline':a.baseline,'canonical_protocol_json_byte_identical':True,'input_hash_seed_unchanged':True,'old_result_sha256':hashlib.sha256(old_bytes).hexdigest(),'new_result_sha256':hashlib.sha256(new_bytes).hexdigest(),'commands':commands,'sources':{str(x.relative_to(r)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources},'baseline_development_source_sha256':hashlib.sha256(old.read_bytes()).hexdigest()}
(o/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('commands','sources')},indent=2))
