"""Supply prerequisite fixtures to unchanged canonical tests. All DBs are explicit test data."""
from pathlib import Path
import subprocess,sqlite3,shutil,json,hashlib,os
src=Path(__file__).resolve().parents[2];root=src.parent;out=root/'evidence'/os.environ.get('STARCORE_INTEGRATED_EVIDENCE','final-integrated');out.mkdir(exist_ok=True)
tool=Path('/tmp/starcore-cdaf1f33/toolchain');java=tool/'jdk-17.0.16+8/bin/java';jar=tool/'android-sdk/platforms/android-36/android.jar';schema_root=root/'evidence'/os.environ.get('STARCORE_HOST_SCHEMA','final-schema');cl=schema_root/'classes';schema=schema_root/'schema.db';bridge=src/'verification/canonical/schema/sqlite_bridge.py'
receipts=[]
def db(name,source=schema):
 d=out/name;d.mkdir(exist_ok=True);p=d/'fixture.db';shutil.copy2(source,p);return d,p
def run(name,args,package='com.ethk3.novaonepro',heap='512m',directory=None):
 d=directory or out/name;d.mkdir(exist_ok=True)
 cmd=[str(java),'-Xmx'+heap,'-Djava.library.path='+str(root/'evidence'/os.environ.get('STARCORE_NATIVE_EVIDENCE','current-native')),'-cp',str(cl)+':'+str(jar),(package+'.' if package else '')+name]+list(map(str,args))
 with (d/'execution.log').open('w')as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=300)
 receipts.append({'test':name,'status':'PASS' if r.returncode==0 else 'FAIL','exit_code':r.returncode,'command':cmd,'scope':'Final product bytecode; explicit isolated test fixtures; no production performance or certificate evidence'})
 (out/'execution_receipts.json').write_text(json.dumps(receipts,indent=2));print(name,receipts[-1]['status'],flush=True)
 return r.returncode
def history(p):
 c=sqlite3.connect(p);s=sqlite3.connect(root/'evidence/replay-prerequisites/ModernFamilyContractCP12/fixture.db')
 for t in ('draws','v57_settlement_task_r36'):
  rows=s.execute('SELECT * FROM '+t).fetchall();c.executemany('INSERT OR REPLACE INTO '+t+' VALUES('+','.join('?' for _ in rows[0])+')',rows)
 c.commit();c.close();s.close()
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
