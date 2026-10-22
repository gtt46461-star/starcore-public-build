#!/usr/bin/env python3
"""Unchanged original anchor13 on an explicit old-FK fixture of the current schema.

Synthetic registration metadata is a migration prerequisite, never a training,
model validation, certificate, natural Future, or customer-data claim.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sqlite3
import subprocess
import time

p=argparse.ArgumentParser()
for flag in ['out','schema','host-classpath']:p.add_argument('--'+flag,type=Path,required=True)
p.add_argument('--javac',default='javac')
a=p.parse_args()
root=Path(__file__).resolve().parents[2]
out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
fixture=out/'old-fk-fixture.db';shutil.copyfile(a.schema,fixture)
h=lambda label:hashlib.sha256(('EXPLICIT_SYNTHETIC_OLD_RECEIPT_FIXTURE:'+label).encode()).hexdigest()
with sqlite3.connect(fixture) as db:
    db.execute('PRAGMA foreign_keys=ON')
    assert db.execute('SELECT count(*) FROM research_development_receipt_v2').fetchone()[0]==0
    db.execute("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,?,'{}',?,'','NURSERY',0,'202609230422',0,1)",(h('candidate'),h('intent'),h('ast')))
    db.execute('INSERT INTO research_training_input_v2 VALUES(?,?,?,?,?,?,?,?,?)',(h('input'),'EXPLICIT_SYNTHETIC_METADATA_ONLY.scrow',1,420,'202609230001','202609230421',h('schema'),h('source'),1))
    db.execute('INSERT INTO research_training_protocol_v2 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)',(h('protocol'),h('candidate'),h('input'),h('ast'),h('features'),'FIXED_GRAPH','202609230001','202609230300','202609230301','202609230302','202609230421',300,120,42,'{}','202609230422',1))
    ddl=db.execute("SELECT sql FROM sqlite_master WHERE name='research_development_receipt_v2'").fetchone()[0].replace('research_artifact_identity_v2','r952_artifact')
    db.execute('DROP TABLE research_development_receipt_v2');db.execute(ddl)
    db.execute("INSERT INTO r952_artifact(artifact_hash,candidate_id,parent_hash,target_issue,delivery_hash,payload_hash,created_at,model_payload,numeric_updates,training_kind,development_receipt_hash,development_sample_n,validation_sample_n,validation_pass,validation_hash) VALUES(?,?,'','202609230421',?,?,1,'{}',0,'DEVELOPMENT_AST3',?,300,120,1,?)",(h('artifact'),h('candidate'),h('delivery'),h('payload'),h('receipt'),h('validation')))
    db.execute('INSERT INTO research_development_receipt_v2 VALUES(?,?,?,?,?,?,1)',(h('receipt'),h('artifact'),h('protocol'),h('validation'),'{}','{}'))
    assert not db.execute('PRAGMA foreign_key_check').fetchall()
    db.commit()
shutil.copyfile(fixture,out/'old-fk-before.db')
test=root/'verification/canonical/tests/audit_cp12/retention/tests/ArtifactAnchorContractCP12.java'
production=root/'app/src/main/java/com/ethk3/novaonepro/RetentionArtifactIdentityCP12.java'
adapter=root/'verification/canonical/schema/SQLiteDatabase.java'
classes=out/'classes';classes.mkdir()
cp=a.host_classpath.read_text().strip()
commands=[('compile',[a.javac,'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(adapter),str(production),str(test)]),
          ('original_anchor13',['java','-Xmx256m','-cp',str(classes)+':'+cp,'com.ethk3.novaonepro.ArtifactAnchorContractCP12',str(root/'verification/canonical/schema/sqlite_bridge.py'),str(fixture),str(out/'original_assertions_receipt.json')])]
records=[]
for name,command in commands:
    start=time.monotonic();result=subprocess.run(command,capture_output=True,text=True,timeout=120)
    (out/(name+'.log')).write_text(result.stdout+result.stderr)
    records.append({'name':name,'command':command,'exit_code':result.returncode,'seconds':time.monotonic()-start})
    print(name,result.returncode,result.stdout.strip(),result.stderr[-800:],flush=True)
    if result.returncode:break
inputs=[test,production,adapter,a.schema.resolve()]
(out/'CURRENT_EXECUTION.json').write_text(json.dumps({'executions':records,'inputs':[{'path':str(f),'bytes':f.stat().st_size,'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in inputs],
    'scope':'Current production Java installer/register and full current schema with real host SQLite. Explicit synthetic old-FK receipt prerequisite; no training algorithm/model qualification/customer legacy receipt claim. Original 13 assertions unchanged.'},indent=2)+'\n')
raise SystemExit(records[-1]['exit_code'])
