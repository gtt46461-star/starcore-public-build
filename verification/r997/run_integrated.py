"""Supply prerequisite fixtures to unchanged canonical tests. All DBs are explicit test data."""
from pathlib import Path
import subprocess,sqlite3,shutil,json,hashlib,os,argparse
p=argparse.ArgumentParser();p.add_argument('--out',type=Path);p.add_argument('--test-classes',type=Path);a=p.parse_args()
src=Path(__file__).resolve().parents[2];root=src.parent;out=a.out or root/'evidence/r997/integrated';out.mkdir(exist_ok=True)
if any(out.iterdir()):raise SystemExit('OUTPUT_MUST_BE_EMPTY_PRESERVE_PREVIOUS_FAILURES')
tool=root/'toolchain_restore/toolchain';java=tool/'zulu17.62.17-ca-jdk17.0.17-linux_x64/bin/java';jar=tool/'android-sdk/platforms/android-36/android.jar';schema_root=root/'evidence/r997/host';cl=a.test_classes or root/'evidence/r997/canonical/classes';schema=schema_root/'schema.db';cp=str(cl)+':'+(schema_root/'host-classpath').read_text().strip();bridge=src/'verification/canonical/schema/sqlite_bridge.py'
receipts=[]
def db(name,source=schema):
 d=out/name;d.mkdir(exist_ok=True);p=d/'fixture.db';shutil.copy2(source,p);return d,p
def run(name,args,package='com.ethk3.novaonepro',heap='512m',directory=None):
 d=directory or out/name;d.mkdir(exist_ok=True)
 cmd=[str(java),'-Xmx'+heap,'-Djava.library.path='+str(root/'evidence/r997/native'),'-Dstarcore.test.ast='+str(root/'evidence/r997/test-ast.txt'),'-cp',cp,(package+'.' if package else '')+name]+list(map(str,args))
 with (d/'execution.log').open('w')as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=300)
 receipts.append({'test':name,'status':'PASS' if r.returncode==0 else 'FAIL','exit_code':r.returncode,'command':cmd,'scope':'Final product bytecode; explicit isolated test fixtures; no production performance or certificate evidence'})
 (out/'execution_receipts.json').write_text(json.dumps(receipts,indent=2));print(name,receipts[-1]['status'],flush=True)
 return r.returncode
def history(p):
 c=sqlite3.connect(p)
 for i in range(1,901):
  target=f'20260923{i:04d}';values=[(i+j)%10+1 for j in range(10)];h=hashlib.sha256(('EXPLICIT_SYNTHETIC_PREREQUISITE:'+target).encode()).hexdigest()
  c.execute('INSERT OR IGNORE INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,first_seen_at,source_line) VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,?)',[target,60000*i,60000*i+1,*values,60000*i+1,'EXPLICIT_SYNTHETIC_TRAINING_FIXTURE_NOT_PHONE'])
  c.execute("INSERT OR IGNORE INTO v57_settlement_task_r36(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',1,1,?,?,?)",[target,target,60000*i+2,h,h])
 c.commit();c.close()
d,atom=db('AtomCatalogImportCP12Test');run('AtomCatalogImportCP12Test',[bridge,atom,src/'app/src/main/assets/cp4_atom_registry.jsonl'],'com.ethk3.novaonepro.r952')
d,seed=db('ModernSeedImportContractCP12',atom);run('ModernSeedImportContractCP12',[bridge,seed,src/'app/src/main/assets/research/cp4_molecule_seeds.jsonl',d/'receipt.json'])
d,family=db('ModernFamilyContractCP12',seed);history(family);run('ModernFamilyContractCP12',[bridge,family,d/'files',d/'receipt.json'])
d,shared=db('SharedFamilyBuilderContractCP12',family);run('SharedFamilyBuilderContractCP12',[bridge,shared,d/'receipt.json'])
d,cold=db('ColdCreatorPublicContractCP12',atom);history(cold);run('ColdCreatorPublicContractCP12',[bridge,cold,src/'app/src/main/assets/cp4_atom_registry.jsonl',src/'app/src/main/assets/research/cp4_molecule_seeds.jsonl',d/'files',d/'receipt.json'])
run('MarkovOrder2CP12Test',[src/'app/src/main/assets/cp4_atom_registry.jsonl'],package='',heap='128m')
# Old receipt FK migration: real registered protocol, explicitly synthetic old artifact/receipt.
d,anchor=db('ArtifactAnchorContractCP12',cold);c=sqlite3.connect(anchor);c.execute('PRAGMA foreign_keys=ON');protocol,candidate,train,valid=c.execute('SELECT protocol_hash,candidate_id,train_n,validation_n FROM research_training_protocol_v2').fetchone()
h=lambda s:hashlib.sha256(('EXPLICIT_SYNTHETIC_OLD_RECEIPT_FIXTURE:'+s).encode()).hexdigest()
ddl=c.execute("SELECT sql FROM sqlite_master WHERE name='research_development_receipt_v2'").fetchone()[0].replace('research_artifact_identity_v2','r952_artifact');c.execute('DROP TABLE research_development_receipt_v2');c.execute(ddl)
c.execute("INSERT INTO r952_artifact(artifact_hash,candidate_id,parent_hash,target_issue,delivery_hash,payload_hash,created_at,model_payload,numeric_updates,training_kind,development_receipt_hash,development_sample_n,validation_sample_n,validation_pass,validation_hash) VALUES(?,?,'','202609230901',?,?,1,'{}',0,'DEVELOPMENT_AST3',?,?,?,1,?)",(h('artifact'),candidate,h('delivery'),h('payload'),h('receipt'),train,valid,h('validation')))
c.execute('INSERT INTO research_development_receipt_v2 VALUES(?,?,?,?,?,?,1)',(h('receipt'),h('artifact'),protocol,h('validation'),'{}','{}'));c.commit();c.close();run('ArtifactAnchorContractCP12',[bridge,anchor,d/'receipt.json'])
# 80 earlier clean draws + canonical20 = resource regime100. Assertions and gates unchanged.
d,numerical=db('ReservedNumericalCaptureContractCP12');c=sqlite3.connect(numerical)
for i in range(1361,1441):c.execute('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[f'20260922{i:04}',i-860,i-859]+[(i+j)%10+1 for j in range(10)])
c.commit();c.close();run('ReservedNumericalCaptureContractCP12',[bridge,numerical,d/'receipt.json'])
