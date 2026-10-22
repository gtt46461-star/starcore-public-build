#!/usr/bin/env python3
"""Current production migration/retention proofs; keep customer bytes outside evidence."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sqlite3
import subprocess
import time

p=argparse.ArgumentParser()
for name in ['out','before-schema','host-classpath','customer-snapshot','private-work']:
    p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--javac',default='javac')
a=p.parse_args()
root=Path(__file__).resolve().parents[2]
out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
private=a.private_work.resolve();private.mkdir(parents=True,exist_ok=False,mode=0o700)
classes=out/'classes';classes.mkdir()
cp=str(classes)+':'+a.host_classpath.read_text().strip()
records=[]
def run(name,command):
    start=time.monotonic()
    result=subprocess.run(command,cwd=root,capture_output=True,text=True,timeout=240)
    log=out/(name+'.log');log.write_text(result.stdout+result.stderr)
    records.append({'name':name,'command':command,'exit_code':result.returncode,
                    'seconds':time.monotonic()-start,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    print(name,result.returncode,result.stdout.strip(),result.stderr[-1400:],flush=True)
    return result.returncode
source=root/'verification/numbered/DiagnosisRetentionCurrent.java'
if run('compile',[a.javac,'-encoding','UTF-8','-cp',cp,'-d',str(classes),str(source)]):
    raise SystemExit(1)
synthetic=private/'synthetic-405.db';shutil.copyfile(a.before_schema,synthetic)
command=['java','-Xmx256m','-cp',cp,'com.ethk3.novaonepro.DiagnosisRetentionCurrent',
         str(root/'verification/canonical/schema/sqlite_bridge.py')]
run('synthetic',command+[str(synthetic),str(out/'synthetic_receipt.json'),'synthetic'])
customer=private/'customer-consistent-copy.db'
with sqlite3.connect('file:'+str(a.customer_snapshot.resolve())+'?mode=ro',uri=True) as src, sqlite3.connect(customer) as dest:
    src.backup(dest)
backup_hash=hashlib.sha256(customer.read_bytes()).hexdigest()
watched=['draws','trades','r952_candidate_predraw','r967_frozen_evaluation_v2',
         'adaptive_judge_log','diagnosis_case_log','diagnosis_fix_plan',
         'dynamic_freeze_matrix','module_effect_cache','shadow_promotion_cache']
def identities(path):
    result={}
    with sqlite3.connect('file:'+str(path)+'?mode=ro',uri=True) as db:
        for table in watched:
            h=hashlib.sha256();count=0
            for row in db.execute('SELECT * FROM "'+table+'" ORDER BY rowid'):
                for value in row:
                    if value is None:raw=b'N'
                    elif isinstance(value,bytes):raw=b'B'+value
                    else:raw=(type(value).__name__+':'+str(value)).encode()
                    h.update(len(raw).to_bytes(8,'big'));h.update(raw)
                count+=1
            result[table]={'rows':count,'row_identity_sha256':h.hexdigest()}
    return result
before=identities(customer)
run('customer',command+[str(customer),str(out/'customer_receipt.json'),'customer'])
after=identities(customer)
preserved=before==after
binding=json.loads(a.host_classpath.with_name('CURRENT_BYTECODE_BINDING.json').read_text())
receipt={'executions':records,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
         'customer_backup_sha256':backup_hash,'customer_input_sha256':hashlib.sha256(a.customer_snapshot.read_bytes()).hexdigest(),
         'customer_watched_tables':before,'all_watched_customer_rows_unchanged':preserved,
         'current_product_source_binding':binding['sources'],
         'scope':'Current production Java methods on real host SQLite with explicit Android API adapters. Customer input was read-only and consistently backed up. No customer row values/API/signing data are included. Synthetic proof receipts are not native archive verification or natural model evidence.'}
(out/'CURRENT_EXECUTION.json').write_text(json.dumps(receipt,indent=2)+'\n')
assert preserved,'CUSTOMER_ROW_IDENTITY_CHANGED'
raise SystemExit(0 if all(x['exit_code']==0 for x in records) else 1)
