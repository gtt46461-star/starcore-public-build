#!/usr/bin/env python3
"""Actual release open() timing with full schema and a 3.35M-row revision index.

Host fixture only. It cannot certify Android flash performance or thread contention.
Only supplied synthetic DB is mutated. Product bytecode is not substituted.
"""
import argparse
import json
from pathlib import Path
import shutil
import sqlite3
import subprocess

p=argparse.ArgumentParser()
p.add_argument('--schema',type=Path,required=True)
p.add_argument('--classpath',type=Path,required=True,help='text file containing actual-release host classpath')
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--bundle-report',type=Path,required=True)
p.add_argument('--out',type=Path,required=True)
p.add_argument('--rows',type=int,default=3350000)
a=p.parse_args()
root=Path(__file__).resolve().parents[2]
out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
db=out/'bundle-scale.db'
if db.exists():raise SystemExit('Use a new output folder; do not overwrite an earlier fixture.')
shutil.copyfile(a.schema,db)
with sqlite3.connect(db) as c:
    c.execute('PRAGMA journal_mode=WAL')
    c.execute('PRAGMA synchronous=NORMAL')
    report=json.loads(a.bundle_report.read_text())
    for row in report['rows']:
        cols=report['columns']
        c.execute('INSERT INTO research_model_bundle_r929('+','.join(cols)+') VALUES('+','.join('?' for _ in cols)+')',[row[col] for col in cols])
    c.execute("WITH RECURSIVE r(n) AS(SELECT 1 UNION ALL SELECT n+1 FROM r WHERE n<?) INSERT INTO research_row_revision_v2(source_table,source_rowid) SELECT 'r75_scale_only',n FROM r",[a.rows])
classes=out/'classes';classes.mkdir()
cp=a.classpath.read_text().strip()
subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(classes),str(root/'verification/r75/bundle_ProfileR75.java')],check=True)
cmd=[str(a.jdk/'bin/java'),'-cp',str(classes)+':'+cp,'com.ethk3.novaonepro.bundle_ProfileR75',str(root/'verification/r967/host/sqlite_bridge.py'),str(db),str(out/'empty-models'),str(out/'profile.tsv')]
r=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
(out/'run.log').write_text(r.stdout)
print(r.stdout,end='')
raise SystemExit(r.returncode)
