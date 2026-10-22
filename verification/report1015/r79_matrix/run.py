"""Run R79 matrix regressions against the current full product build, never overlays.
The SQLite records and permutations here are synthetic and confer no qualification.
"""
from pathlib import Path
import os,json,subprocess,shutil,datetime,sqlite3,hashlib
R=Path(__file__).resolve().parents[3]; W=R.parent
config=Path(os.environ.get('STARCORE_HOST_CONFIG',str(W/'audit_work/host/config.json')))
c=json.loads(config.read_text())
O=Path(os.environ.get('STARCORE_R79_MATRIX_OUT',str(W/'audit_work/r79-matrix-current')));O.mkdir(parents=True,exist_ok=False)
tests=O/'tests';tests.mkdir();empty=O/'empty';empty.mkdir()
product=Path(c['product']);before={str(p.relative_to(product)):hashlib.sha256(p.read_bytes()).hexdigest() for p in product.rglob('*.class')}
assert before,'FULL_PRODUCT_CLASSES_REQUIRED'
expected_manifest=config.parent/'product_hashes.json'
assert json.loads(expected_manifest.read_text())==before,'PREPARED_PRODUCT_MANIFEST_MISMATCH'
(O/'prepared_product_manifest_sha256.txt').write_text(hashlib.sha256(expected_manifest.read_bytes()).hexdigest()+'\n')
cp=':'.join([str(tests),c['classpath']])
sources=[R/'verification/canonical/extended'/name for name in ['ModernModelContractTest.java','MatrixBindingsCP12Test.java','MatrixHistoricalDrawCP12Test.java']]
sources+=[R/'verification/report1015/MatrixSourcesR78.java',*Path(__file__).parent.glob('*.java')]
with (O/'compile.log').open('w') as f:
 subprocess.run([c['javac'],'-encoding','UTF-8','-sourcepath',str(empty),'-cp',cp,'-d',str(tests),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
assert not set(before)&{str(p.relative_to(tests)) for p in tests.rglob('*.class')},'PRODUCT_OVERLAY_FORBIDDEN'
schema=Path(os.environ.get('STARCORE_HOST_SCHEMA',str(W/'audit_work/host-schema.db')))
fixture=Path(os.environ.get('STARCORE_TEST_AST',str(R/'verification/report1015/failed_neural_parent.ast')))
# Shared original matrix/teacher fixtures explicitly require position1. This is a new
# synthetic template, not a modified persisted candidate or historical receipt.
template=json.loads(fixture.read_text()[5:]);template['position']=1
fixture=O/'position1-synthetic-template.ast'
fixture.write_text('AST3:'+json.dumps(template,sort_keys=True,separators=(',',':'),ensure_ascii=False))
results=[]
# An old ready cache must not suppress the new additive step. Roll back in one JVM,
# then restart in a second JVM against the same test-only database.
for route in ['inner','outer']:
 migration_db=O/('MatrixReadyMigrationR79-'+route+'.db');shutil.copy2(schema,migration_db)
 with sqlite3.connect(migration_db) as s:
  s.execute('DROP INDEX IF EXISTS idx_r79_matrix_ready_partition_order')
  s.execute("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id='R79_MATRIX_READY_ORDER_V1'")
 for phase in ['rollback','restart']:
  mode=route+'-'+phase
  with (O/('MatrixReadyMigrationR79-'+mode+'.log')).open('w') as f:
   p=subprocess.run([c['java'],'-Xmx512m','-cp',cp,'com.ethk3.novaonepro.r952.MatrixReadyMigrationR79',c['bridge'],str(migration_db),mode],stdout=f,stderr=subprocess.STDOUT,timeout=180)
  results.append({'test':'MatrixReadyMigrationR79-'+mode,'exit_code':p.returncode,'scope':'ACTUAL_PRODUCT_OLD_READY_CACHE_ROLLBACK_RESTART'})
  print('MatrixReadyMigrationR79-'+mode,p.returncode,flush=True)
for name in ['MatrixEventOrderR79','MatrixSelectorsR79','TeacherEligibilityR79','MatrixExpiryR79','MatrixSourcesR78','MatrixBindingsCP12Test','MatrixHistoricalDrawCP12Test']:
 db=O/(name+'.db');shutil.copy2(schema,db)
 if name=='MatrixEventOrderR79':
  # Simulate the prior release schema: all old migration receipts remain, new index/receipt absent.
  with sqlite3.connect(db) as s:
   s.execute('DROP INDEX IF EXISTS idx_r79_matrix_ready_partition_order')
   s.execute("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id='R79_MATRIX_READY_ORDER_V1'")
 if name=='MatrixExpiryR79':
  start=datetime.date(2026,9,23)
  with sqlite3.connect(db) as s:
   s.executemany('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,1,2,3,4,5,6,7,8,9,10)',[((start+datetime.timedelta(days=i//1440)).strftime('%Y%m%d')+f'{i%1440+1:04d}',1000000+i*60000,1000001+i*60000) for i in range(5000)])
 arg=str(O/(name+'.json')) if name=='TeacherEligibilityR79' else str(R/'app/src/main/assets/cp4_atom_registry.jsonl')
 with (O/(name+'.log')).open('w') as f:
  p=subprocess.run([c['java'],'-Xmx512m','-Dstarcore.test.ast='+str(fixture),'-cp',cp,'com.ethk3.novaonepro.r952.'+name,c['bridge'],str(db),arg],stdout=f,stderr=subprocess.STDOUT,timeout=240)
 results.append({'test':name,'exit_code':p.returncode,'scope':'ACTUAL_PRODUCT_SYNTHETIC_SQLITE_NOT_NATURAL_QUALIFICATION'})
 (O/'summary.json').write_text(json.dumps(results,indent=2));print(name,p.returncode,flush=True)
after={str(p.relative_to(product)):hashlib.sha256(p.read_bytes()).hexdigest() for p in product.rglob('*.class')}
assert before==after,'PRODUCT_CHANGED_DURING_TEST_RUN'
(O/'product_hashes.json').write_text(json.dumps(before,indent=2))
assert all(r['exit_code']==0 for r in results),'SEE_UNIQUE_OUTPUT_LOGS'
from sql_scale import run as run_scale
run_scale(schema,O/'sql-scale')
print('R79_MATRIX_ALL_PASS',O)
