#!/usr/bin/env python3
"""Current-class host regression; caller supplies private fixtures, never packages DBs."""
import argparse, pathlib, json, sqlite3, subprocess, hashlib
p=argparse.ArgumentParser();p.add_argument('--config',required=True);p.add_argument('--seed-fixture',required=True,help='Zero molecule seeds; atom registry present. Real importer bootstraps missing atom checkpoint.');p.add_argument('--checkpoint-fixture',required=True,help='Retained MATERIALIZING checkpoint with at least two remaining pages.');p.add_argument('--output',required=True);a=p.parse_args()
r=pathlib.Path(__file__).resolve().parent;out=pathlib.Path(a.output).resolve();out.mkdir(parents=True,exist_ok=False);c=json.load(open(a.config));classes=out/'tests';classes.mkdir();db=out/'private-test.db';commands=[];results={'config_sha256':hashlib.sha256(pathlib.Path(a.config).read_bytes()).hexdigest(),'product_class_sha256':hashlib.sha256((pathlib.Path(c['product'])/'com/ethk3/novaonepro/r952/ModernSeedFactoryCP12.class').read_bytes()).hexdigest()}
def execute(cmd,name):
 commands.append(cmd);q=subprocess.run(cmd,text=True,capture_output=True);(out/(name+'.log')).write_text(q.stdout+q.stderr);assert q.returncode==0,(name,q.returncode);return q.stdout
execute([c['javac'],'-cp',c['classpath'],'-d',str(classes),*[str(x) for x in sorted((r/'tests').glob('*.java'))]],'compile-tests')
cp=str(classes)+':'+c['classpath']
def restore(src):
 for suf in ['','-wal','-shm']:
  f=pathlib.Path(str(db)+suf)
  if f.exists():f.unlink()
 source=sqlite3.connect('file:'+str(pathlib.Path(src).resolve())+'?mode=ro',uri=True);target=sqlite3.connect(db);source.backup(target);source.close();target.close()
def run(cls,args,name):return execute([c['java'],'-Xmx192m','-cp',cp,'com.ethk3.novaonepro.'+cls,c['bridge'],str(db),*args],name)
def row():
 with sqlite3.connect('file:'+str(db)+'?mode=ro',uri=True) as d:return d.execute('select material_row from research_molecule_import_checkpoint_cp12').fetchone()[0]
restore(a.seed_fixture);run('ModernSeedImportContractCP12',[str(pathlib.Path('app/src/main/assets/research/cp4_molecule_seeds.jsonl').resolve()),str(out/'full4200.json')],'full4200')
with sqlite3.connect('file:'+str(db)+'?mode=ro',uri=True) as d:
 cols=[x[1] for x in d.execute('pragma table_info(research_molecule_seed_v2)') if x[1]!='imported_at'];rows=d.execute('select '+','.join(cols)+' from research_molecule_seed_v2 order by seed_hash').fetchall();assert len(rows)==4200;results['seed_semantic_columns']=cols;results['seed_rows_sha256']=hashlib.sha256(json.dumps(rows,separators=(',',':'),ensure_ascii=False).encode()).hexdigest()
d.close()
assert results['seed_rows_sha256']=='8f7fb80ba5dea02e36ad95d95d6bb19f84febfb28b874d064040910bc950004e','Full4200 semantic hash differs from retained baseline'
restore(a.checkpoint_fixture);run('AdmissionNegative',[str(out/'files')],'negative')
for mode in ['steady','later','first']:
 restore(a.checkpoint_fixture);before=row();logs=[]
 for i in range(5 if mode=='steady' else 1):logs.append(run('AdmissionProbe',[str(out/'files'),mode],mode+'-'+str(i)))
 after=row();assert after>before if mode=='steady' else after==before,(mode,before,after);assert all('leaseReleased=true' in s for s in logs);results[mode]={'before':before,'after':after}
results['scope']='Current-class host regression only; no Android600s or C07 acceptance.'
(out/'receipt.json').write_text(json.dumps(results,indent=2)+'\n');(out/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');print(json.dumps(results))
