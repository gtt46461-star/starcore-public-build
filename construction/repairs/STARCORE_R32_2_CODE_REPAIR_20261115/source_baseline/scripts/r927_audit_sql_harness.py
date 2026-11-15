#!/usr/bin/env python3
"""Run changed production Java SQL/report code with real SQLite and adversarial fixtures."""
import json, os, re, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / 'app/src/main/java/com/ethk3/novaonepro'
OUT = ROOT / 'build/r927-sql'
OUT.mkdir(parents=True, exist_ok=True)
tc = Path(os.environ.get('STARCORE_TOOLCHAIN_ROOT', ROOT.parent/'08_TOOLCHAIN'))
android = Path(os.environ.get('ANDROID_PLATFORM_JAR', tc/'android-platform-36/android.jar'))
host = ROOT/'build/r925-host/compiled-classes'
sql = []
tables = set()
def table(name, cols):
    if name in tables: return
    tables.add(name)
    sql.append('CREATE TABLE '+name+' ('+cols+');')
table('v57_attempt_resource_r927',"execution_id TEXT PRIMARY KEY,attempt_id TEXT NOT NULL,target_issue TEXT NOT NULL,process_id TEXT NOT NULL,release_code INTEGER NOT NULL,started_at INTEGER NOT NULL,finished_at INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL,elapsed_ms INTEGER NOT NULL DEFAULT -1,heap_peak_bytes INTEGER NOT NULL DEFAULT -1,heap_samples INTEGER NOT NULL DEFAULT 0,gc_count INTEGER NOT NULL DEFAULT -1,gc_time_ms INTEGER NOT NULL DEFAULT -1,gc_status TEXT NOT NULL DEFAULT 'UNMEASURED',lock_wait_ms INTEGER NOT NULL DEFAULT 0,lock_wait_max_ms INTEGER NOT NULL DEFAULT 0,db_body_max_ms INTEGER NOT NULL DEFAULT 0,db_slice_overruns INTEGER NOT NULL DEFAULT 0,budget_hash TEXT NOT NULL,stage_samples TEXT NOT NULL DEFAULT '',reason TEXT NOT NULL DEFAULT ''")
table('v57_prediction_timing_r925','target_issue TEXT PRIMARY KEY,process_id TEXT,release_code INTEGER,request_at INTEGER,runtime_at INTEGER,durable_at INTEGER,queue_ms INTEGER,compute_ms INTEGER,commit_ms INTEGER,total_ms INTEGER,sample_class TEXT,attempt_id TEXT')
table('v57_runtime_event_r926','seq INTEGER PRIMARY KEY AUTOINCREMENT,attempt_id TEXT,game_code TEXT,event_type TEXT,created_at INTEGER')
table('v57_runtime_epoch_r925','epoch_id TEXT,kind TEXT,started_at INTEGER,start_issue TEXT,release_code INTEGER')
version=json.loads((ROOT/'release/release_manifest.json').read_text())['versionCode']
sql.append(f"INSERT INTO v57_runtime_epoch_r925 VALUES('release-{version}','RELEASE',100,'',{version});")
table('v57_issue_projection_r926','game_code TEXT,target_issue TEXT,canonical_seen INTEGER,full_seen INTEGER')
for i in range(2216):
    sql.append(f"INSERT INTO v57_issue_projection_r926 VALUES('Binance1-PK10','20260909{i:04}',1,{0 if i==10 else 1});")
report=(SRC/'AlgorithmLifecycleReportR927.java').read_text()
cols=sorted(set(re.findall(r'value\(c,\s*"([a-z_]+)"\)',report)))
table('v57_algorithm_genome_r908',','.join(c+' TEXT' for c in cols))
for i in range(401):
    sql.append(f"INSERT INTO v57_algorithm_genome_r908(genome_id,status,display_name_zh) VALUES('g{i:04}','{'PAUSED_CAPACITY_R912' if i%2 else 'PRUNED_EVIDENCE_R912'}','算法{i}');")
table('v57_research_asset_r906',','.join(c+' TEXT' for c in cols if c not in ['frozen_n','settled_n','hit_rate','interval_lower','brier','ece','block_stability','drift_score']))
table('v57_research_eval_coverage_r906','asset_id TEXT,frozen_n INTEGER,settled_n INTEGER,hit_rate REAL,interval_lower REAL,brier REAL,ece REAL,block_stability REAL,drift_score REAL')
sql.append("INSERT INTO v57_research_asset_r906(asset_id,display_name_zh,tier,last_transition_code) VALUES('old','归档优质样例','ARCHIVED','HISTORICAL_DRIFT');")
sql.append("INSERT INTO v57_research_eval_coverage_r906 VALUES('old',240,240,0.5,0.43,0.2,0.03,0.9,0.4);")
table('v57_lifecycle_audit_r925','genome_id TEXT,first_prune_reason TEXT,first_prune_issue TEXT,original_available INTEGER,latest_status TEXT,latest_reason TEXT,first_prune_metrics TEXT,first_prune_at INTEGER')
# Resolve the actual fixed table constants used by the inventory, then make unrelated empty tables.
constants={}
# Inventory tables now span more than the original three owners; resolve every fixed TABLE_*
# declaration from production sources rather than failing when a new audited owner is added.
for p in SRC.glob('*.java'):
    for k,v in re.findall(r'String\s+(TABLE_\w+|TABLE)\s*=\s*"([^"]+)"',p.read_text()):
        constants[p.stem+'.'+k]=v
for expr,key in re.findall(r'appendTable\(out, db, ([^,]+), "([^"]+)"\)',(SRC/'FullResearchInventoryR923.java').read_text()):
    # Runtime-discovered table names are appended through the loop variable `name`; fixed fixture
    # tables are created below from explicit names/constants only. Do not treat a local variable as
    # a Java static constant.
    if expr == 'name':
        continue
    name=expr.strip('"') if expr.startswith('"') else constants[expr]
    table(name,','.join(k+' TEXT' for k in key.split(','))+',status TEXT')
(OUT/'fixture.sql').write_text('\n'.join(sql)+'\n')
cp=os.pathsep.join(map(str,[host,android]))
files=[ROOT/'verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java',ROOT/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java',ROOT/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',ROOT/'verification/r925/R927AuditSqlHarness.java']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(OUT),*map(str,files)],check=True)
# Android classpath archive can be APK-shaped; use the extracted classes from the host toolchain.
runtime=OUT/'android-runtime';runtime.mkdir(exist_ok=True)
import zipfile
with zipfile.ZipFile(android) as z:
    for n in z.namelist():
        if n.endswith('.class'): z.extract(n,runtime)
run=subprocess.run(['java','-cp',os.pathsep.join(map(str,[OUT,host,runtime])),'com.ethk3.novaonepro.R927AuditSqlHarness',str(ROOT/'verification/r925/sqlite_bridge.py'),str(OUT/'fixture.sql')],capture_output=True,text=True)
(OUT/'RESULT.txt').write_text(run.stdout+run.stderr)
print(run.stdout+run.stderr)
if run.returncode or 'R927_AUDIT_SQL_HARNESS_PASS' not in run.stdout: raise SystemExit(1)
evidence=ROOT/'reports/r927';evidence.mkdir(exist_ok=True)
(evidence/'AUDIT_SQL_RESULT.json').write_text(json.dumps({'passed':True,'scope':'PRODUCTION_JAVA_SQL_AND_REPORTS_REAL_SQLITE','assertions':run.stdout.splitlines(),'android_device':'NOT_RUN'},ensure_ascii=False,indent=2)+'\n')
