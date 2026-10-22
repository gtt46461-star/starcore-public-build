#!/usr/bin/env python3
"""Reproduce WAL/cancel assertions using the current production JNI, never a backup mock."""
import argparse
import hashlib
import json
from pathlib import Path
import sqlite3
import subprocess
import time

parser=argparse.ArgumentParser()
parser.add_argument('--out',type=Path,required=True)
parser.add_argument('--native',type=Path,required=True)
parser.add_argument('--host-classpath',type=Path,required=True)
parser.add_argument('--javac',default='javac')
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]
out=args.out.resolve()
out.mkdir(parents=True,exist_ok=False)
fixture=out/'wal-fixture.db'
with sqlite3.connect(fixture) as db:
    db.executescript('PRAGMA journal_mode=WAL; CREATE TABLE marker_a(revision INTEGER NOT NULL);'
                    'CREATE TABLE marker_b(revision INTEGER NOT NULL);'
                    'INSERT INTO marker_a VALUES(0); INSERT INTO marker_b VALUES(0);'
                    'CREATE TABLE filler(value BLOB);')
    db.executemany('INSERT INTO filler VALUES(zeroblob(1048576))',[()]*32)
    db.commit()
classpath=args.host_classpath.read_text().strip()
classes=out/'classes'
classes.mkdir()
source=root/'verification/numbered/NativeBackupSnapshotCurrent.java'
wrapper=root/'app/src/main/java/com/ethk3/novaonepro/ReportNativeCP4.java'
commands=[('compile',[args.javac,'-encoding','UTF-8','-cp',classpath,'-d',str(classes),str(wrapper),str(source)]),
          ('snapshot_and_cancel',['java','-Xmx128m','-Djava.library.path='+str(args.native.resolve()),
             '-cp',str(classes)+':'+classpath,'com.ethk3.novaonepro.NativeBackupSnapshotCurrent',
             str(root/'verification/canonical/schema/sqlite_bridge.py'),str(fixture),str(out/'output-fixture')])]
records=[]
for name,command in commands:
    start=time.monotonic()
    result=subprocess.run(command,capture_output=True,text=True,timeout=120)
    (out/(name+'.log')).write_text(result.stdout+result.stderr)
    records.append({'name':name,'command':command,'exit_code':result.returncode,'seconds':time.monotonic()-start})
    print(name,result.returncode,result.stdout.strip(),flush=True)
    if result.returncode:break
inputs=[source,wrapper,root/'app/src/main/cpp/report_native_cp4.c',args.native.resolve()/'libstarcore_report_cp4.so']
(out/'CURRENT_EXECUTION.json').write_text(json.dumps({
    'executions':records,'inputs':[{'path':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in inputs],
    'fixture':'Fresh 32MiB synthetic SQLite database; zero initial marker revisions; two marker tables changed together under real WAL transaction after every native progress callback.',
    'scope':'Current production Java/JNI with real SQLite WAL writer. Host Android adapters; not device/SAF/upgrade coverage.'},indent=2)+'\n')
raise SystemExit(records[-1]['exit_code'])
