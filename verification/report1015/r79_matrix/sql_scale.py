"""Scale the exact product SQL/index schema in an isolated synthetic SQLite fixture.
No production admissions, ACKs, qualifications or timing promises result from this benchmark.
"""
from pathlib import Path
import sqlite3,hashlib,struct,time,statistics,json,re,sys
R=Path(__file__).resolve().parents[3]
def run(schema,out):
 out=Path(out);out.mkdir(parents=True,exist_ok=False)
 source=R/'app/src/main/java/com/ethk3/novaonepro/r952/MatrixSnapshotBusR952.java'
 expr=re.search(r'db.rawQuery\(("SELECT i\.matrix_id.*?),excludeArgs\.toArray',source.read_text()).group(1)
 query=''.join(json.loads('"'+x+'"') for x in re.findall(r'"((?:[^"\\]|\\.)*)"',expr))
 assert "earlier.source_ordinal<i.source_ordinal" in query
 h=lambda s:hashlib.sha256(s.encode()).hexdigest()
 original=sqlite3.connect(schema);s=sqlite3.connect(out/'scale.db');s.execute('PRAGMA foreign_keys=ON')
 for name in ['research_matrix_spec_cp12','research_matrix_input_cp12','idx_cp12_matrix_input_ready','idx_cp12_matrix_input_replay']:
  s.execute(original.execute('SELECT sql FROM sqlite_master WHERE name=?',(name,)).fetchone()[0])
 partial=original.execute("SELECT sql FROM sqlite_master WHERE name='idx_r79_matrix_ready_partition_order'").fetchone()[0];original.close()
 sh=h('SCM4/BINARY_V1/CP12/20_TYPES/FIXED43_RING/STATS_DOUBLE/FREEZE_UTF8/V1')
 ids=[]
 for i in range(256):
  partition=f'stage:TEST_{i}|unit:WALL_MICROS|device:ANDROID|release:979';manifest=h('scale:'+str(i));mid=h(f'MATRIX_CP4|LatencyMatrix|{partition}|20|{manifest}');ids.append(mid)
  s.execute('INSERT INTO research_matrix_spec_cp12 VALUES(?,?,?,?,?,?)',(mid,'LatencyMatrix',partition,20,manifest,sh))
 def row(mid,ordinal,issue,state):
  receipt=h(f'scale-receipt:{mid}:{ordinal}:{state}');proof=h('synthetic:'+receipt);payload=struct.pack('>qiBBBHHdddH',ordinal,0,0,0,0,0,0,1000.0,0.0,0.0,0)
  ih=h(f'CP12_MATRIX_INPUT|{mid}|{issue}|{ordinal}|{receipt}|{proof}|'+hashlib.sha256(payload).hexdigest())
  return mid,receipt,issue,ordinal,ih,proof,payload,state,'' if state=='READY' else 'SYNTHETIC_INDEX_LOAD_ONLY',1,1
 s.executemany('INSERT INTO research_matrix_input_cp12 VALUES(?,?,?,?,?,?,?,?,?,?,?)',(row(mid,n,f'20260923{65-n:04d}','READY') for mid in ids for n in range(1,65)))
 # Non-ready retained history exercises the initial full-table index scan without fabricated ACKs.
 s.executemany('INSERT INTO research_matrix_input_cp12 VALUES(?,?,?,?,?,?,?,?,?,?,?)',(row(ids[i%256],1000+i,'202609220001','QUARANTINED') for i in range(100000)))
 s.commit();start=time.perf_counter();s.execute(partial);s.commit();index_ms=(time.perf_counter()-start)*1000
 old='SELECT source_ordinal FROM research_matrix_input_cp12 WHERE state=\'READY\' ORDER BY source_issue,source_ordinal,matrix_id,source_receipt_hash LIMIT 1'
 assert s.execute(old).fetchone()[0]==64
 durations=[]
 for i in range(50):
  start=time.perf_counter();r=s.execute(query).fetchone();durations.append((time.perf_counter()-start)*1000);assert r[3]==1
 visited=[]
 for i in range(32):
  modified=query.replace("WHERE i.state='READY'","WHERE i.state='READY'"+''.join(' AND i.matrix_id<>?' for _ in visited),1)
  r=s.execute(modified,visited).fetchone();assert r and r[0] not in visited and r[3]==1;visited.append(r[0])
 plan=[r[3] for r in s.execute('EXPLAIN QUERY PLAN '+query)]
 assert any('idx_r79_matrix_ready_partition_order' in p for p in plan)
 report={'scope':'EXACT_PRODUCT_QUERY_AND_TABLE_INDEX_DDL_SYNTHETIC_SCALE_ONLY_NOT_FULL_APP_OR_PHONE','ready_rows':16384,'partitions':256,'nonready_retained_rows':100000,'total_rows':116384,'legacy_first_ordinal':64,'fixed_first_ordinal':1,'unique_excluded_partitions_checked':len(visited),'query_runs':len(durations),'query_ms_min':min(durations),'query_ms_median':statistics.median(durations),'query_ms_p95':sorted(durations)[47],'query_ms_max':max(durations),'index_initial_full_scan_ms':index_ms,'sqlite_version':sqlite3.sqlite_version,'query_plan':plan,'sql':query,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'caveat':'The READY index has at most16384 live entries; CREATE INDEX scans the entire retained table. This116384-row fixture is not the14GB device database and establishes no device latency limit.'}
 (out/'result.json').write_text(json.dumps(report,indent=2));print(json.dumps({k:v for k,v in report.items() if k.endswith('_ms') or k.startswith('query_ms')},indent=2));s.close();return report
if __name__=='__main__':run(sys.argv[1],sys.argv[2])
