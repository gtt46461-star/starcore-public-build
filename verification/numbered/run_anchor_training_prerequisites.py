#!/usr/bin/env python3
"""Actual cold import/Creator prerequisite for the anchor and model-quota consumers.

All history is explicit synthetic test input. Production algorithms/DAO/Owner
run unchanged; this is not Ethereum networking, device, or natural Future proof.
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
for flag in ['out','schema','host-classpath','native']:p.add_argument('--'+flag,type=Path,required=True)
p.add_argument('--javac',default='javac')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
classes=out/'classes';classes.mkdir();cp=a.host_classpath.read_text().strip();bridge=root/'verification/canonical/schema/sqlite_bridge.py'
tests=[root/'verification/canonical/tests/audit_cp12/matrix/tests/AtomCatalogImportCP12Test.java',
       root/'verification/canonical/tests/audit_cp12/coverage/tests/com/ethk3/novaonepro/ColdCreatorPublicContractCP12.java']
adapters=[root/'verification/canonical/schema/SQLiteDatabase.java',
          *[f for f in (root/'verification/canonical/adapters').rglob('*.java') if not str(f).endswith('/android/system/Os.java')],
          root/'verification/r994/adapters/android/os/StatFs.java',root/'verification/r996/android/system/Os.java']
records=[]
def run(name,command):
    started=time.monotonic();result=subprocess.run(command,capture_output=True,text=True,timeout=300)
    (out/(name+'.log')).write_text(result.stdout+result.stderr)
    records.append({'name':name,'command':command,'exit_code':result.returncode,'seconds':time.monotonic()-started})
    (out/'CURRENT_EXECUTION.json').write_text(json.dumps({'executions':records,'native_sha256':hashlib.sha256((a.native/'libstarcore_report_cp4.so').read_bytes()).hexdigest(),'test_sources':[{'path':str(f.relative_to(root)),'sha256':hashlib.sha256(f.read_bytes()).hexdigest()} for f in tests],
        'scope':'Fresh current Android product bytecode; unchanged original assertions, actual import/Creator and unique writer; synthetic settled history and explicit Android filesystem transports. Not networking/device/Future/model qualification evidence.'},indent=2)+'\n')
    print(name,result.returncode,result.stdout[-1200:],result.stderr[-1200:],flush=True)
    if result.returncode:raise SystemExit(result.returncode)
run('compile',[a.javac,'-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,adapters+tests)])
cp=str(classes)+':'+cp
atom=out/'atom-fixture.db';shutil.copyfile(a.schema,atom)
asset=root/'app/src/main/assets/cp4_atom_registry.jsonl';seeds=root/'app/src/main/assets/research/cp4_molecule_seeds.jsonl'
run('actual_atom_catalog',['java','-Xmx256m','-Djava.library.path='+str(a.native.resolve()),'-cp',cp,'com.ethk3.novaonepro.r952.AtomCatalogImportCP12Test',str(bridge),str(atom),str(asset)])
cold=out/'cold-creator-fixture.db';shutil.copyfile(atom,cold)
with sqlite3.connect(cold) as db:
    for i in range(1,901):
        issue=f'20260923{i:04d}';numbers=[(i+j)%10+1 for j in range(10)]
        h=hashlib.sha256(('EXPLICIT_SYNTHETIC_PREREQUISITE:'+issue).encode()).hexdigest()
        db.execute('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,first_seen_at,source_line) VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?,?,?)',[issue,60000*i,60000*i+1,*numbers,60000*i+1,'EXPLICIT_SYNTHETIC_TRAINING_FIXTURE_NOT_PHONE'])
        db.execute("INSERT INTO v57_settlement_task_r36(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',1,1,?,?,?)",[issue,issue,60000*i+2,h,h])
    db.commit()
run('actual_cold_creator',['java','-Xmx256m','-Djava.library.path='+str(a.native.resolve()),'-cp',cp,'com.ethk3.novaonepro.ColdCreatorPublicContractCP12',str(bridge),str(cold),str(asset),str(seeds),str(out/'files'),str(out/'creator_receipt.json')])
(out/'host-classpath').write_text(cp+'\n')
