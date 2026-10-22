"""Fresh compiled product acceptance; retained failures are not silently relabelled.

Run after run_schema_capture.py into evidence/certificate-final-schema. Isolated
synthetic fixtures and host adapters never grant production certificates or REAL BET.
"""
from pathlib import Path
import subprocess, os, json, re, shutil, hashlib, time
src=Path(__file__).resolve().parents[2]; root=src.parent
tool=Path('/tmp/starcore-cdaf1f33/toolchain'); java=tool/'jdk-17.0.16+8/bin/java'
jar=tool/'android-sdk/platforms/android-36/android.jar'; schema=root/'evidence'/os.environ.get('STARCORE_HOST_SCHEMA','certificate-final-schema')
classes=schema/'classes'; out=root/'evidence'/os.environ.get('STARCORE_ACCEPTANCE_EVIDENCE','certificate-final-host-complete'); out.mkdir(exist_ok=True)
bridge=src/'verification/canonical/schema/sqlite_bridge.py'; asset=src/'app/src/main/assets/cp4_atom_registry.jsonl'
test_sources=[src/p for p in json.loads((src/'verification/canonical/TEST_SOURCES.json').read_text())]
adapters=list((src/'verification/canonical/adapters').rglob('*.java'))
extras=list((src/'verification/r982').glob('*.java'))+[p for p in (src/'verification/canonical/extended').glob('*.java') if p.name!='Os.java']
compile_cmd=[str(java.with_name('javac')),'-encoding','UTF-8','-cp',str(classes)+':'+str(jar),'-d',str(classes)]+list(map(str,adapters+extras+test_sources))
with (out/'compile.log').open('w') as log: result=subprocess.run(compile_cmd,stdout=log,stderr=subprocess.STDOUT)
(out/'compile-command.json').write_text(json.dumps(compile_cmd,indent=2))
if result.returncode: raise SystemExit(result.returncode)
receipts=[]
def run(name,args,package='com.ethk3.novaonepro',heap='128m',directory=None):
    d=directory or out/name; d.mkdir(exist_ok=True)
    cmd=[str(java),'-Xmx'+heap,'-Djava.library.path='+str(root/'evidence'/os.environ.get('STARCORE_NATIVE_EVIDENCE','current-native')),
         '-Dstarcore.test.ast='+str(root/'evidence/training-fixture-ast.txt'),'-cp',str(classes)+':'+str(jar),
         (package+'.' if package else '')+name]+list(map(str,args))
    start=time.monotonic()
    with (d/'execution.log').open('w') as log:
        try: result=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=300); code=result.returncode
        except subprocess.TimeoutExpired: code=124
    receipts.append({'test':name,'status':'PASS' if code==0 else 'FAIL','exit_code':code,'command':cmd,
                     'elapsed_s':time.monotonic()-start,'scope':'Fresh current product bytecode, synthetic host fixtures only; no device or production Future performance claim'})
    (out/'execution_receipts.json').write_text(json.dumps(receipts,indent=2))
    print(name,receipts[-1]['status'],flush=True)
    return d
def fixture(name):
    d=out/name
    if d.exists(): raise RuntimeError('FRESH_FIXTURE_REQUIRED:'+str(d))
    d.mkdir(); db=d/'fixture.db'; shutil.copy2(schema/'schema.db',db); return d,db
# These eight tests have dedicated dependency preparation in run_integrated_host.py.
integrated={'ArtifactAnchorContractCP12','ModernSeedImportContractCP12','SharedFamilyBuilderContractCP12',
            'ModernFamilyContractCP12','ReservedNumericalCaptureContractCP12','ColdCreatorPublicContractCP12'}
special={'BusinessFunnelContractCP12','ReservedModernCaptureContractCP12','ModernSeedRegistrationContractCP12'}
pure={'EvolutionStoragePolicyCP12Test','ExclusiveStageClockCP12Test','ResumableSha256CP12Test',
      'MatrixProjectionCP12Test','ClockAnchorContractCP12','ReportSemanticContractCP12',
      'CertificateAtomCapCP12Test','DrawGapMin21CP12Test'}
receipt_only={'MutationGraphContractCP12','ResearchFeedbackMetricsContractCP12','RecipeMemoryContractCP12',
              'CoreRetirementPitContractCP12','CP9KernelContractCP12'}
io={'ReportPublicationContractCP12','ReportSpoolContractCP12','ReportQueueRecoveryContractCP12',
    'ReportSafContractCP12','ReportSortContractCP12'}
for path in test_sources:
    name=path.stem
    if name in integrated|special: continue
    d,db=fixture(name); files=d/'files'; files.mkdir(); receipt=d/'receipt.json'
    args=[bridge,db,receipt]
    if name in pure: args=[]
    elif name in receipt_only: args=[receipt]
    elif name in io: args=[files]
    elif name in {'MutationOperatorsCP12Test','MarkovOrder2CP12Test'}: args=[asset]
    elif name=='MutationNewFamilyCP12Test': args=[asset,receipt]
    elif name=='ResearchDirichletCallerCP12Test': args=[asset,bridge,db]
    elif name in {'RetentionIoContractCP12','RetentionPublishedProofContractCP12'}: args=[files,receipt]
    elif name=='ModernSeedRegistrationContractCP12': args=[bridge,db,files,receipt]
    elif name=='AtomAssetFdCP12Test': args=[bridge,db,asset,d/'container.bin']
    elif name=='AtomCatalogImportCP12Test': args=[bridge,db,asset]
    package=re.search(r'package\s+([\w.]+)\s*;',path.read_text())
    run(name,args,package.group(1) if package else '',directory=d)
# Certificate original fixture's PUBLISHED expectation is retained in its FAIL log;
# the new actual producer fixture supplies paired and atom evidence without relaxing gates.
d,db=fixture('CertificateCommonFutureR982');shutil.copy2(out/'AtomCatalogImportCP12Test/fixture.db',db);run('CertificateCommonFutureR982',[bridge,db],heap='512m',directory=d)
for name in ('ReportRecoveryContractR982','ReportRecoveryLookupR982'):
    d,db=fixture(name);run(name,[d/'files'],directory=d)
d,db=fixture('PostProjectionRecoveryR982');run('PostProjectionRecoveryR982',[bridge,db],directory=d)
# Original modern reserved capture assertions with explicitly new trained helper.
d,db=fixture('ReservedModernCaptureContractCP12');shutil.copy2(out/'AtomCatalogImportCP12Test/fixture.db',db)
import sqlite3
c=sqlite3.connect(db)
for i in range(342,422): c.execute('INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,?,?,?,?,?,?,?,?,?,?)',[f'20260923{i:04}',1000*i,1000*i+1]+[(i+j)%10+1 for j in range(10)])
c.commit();c.close();run('ReservedModernCaptureContractCP12',[bridge,db,d/'receipt.json'],heap='512m',directory=d)
d,db=fixture('BusinessFunnelContractCP12');shutil.copy2(schema/'schema.db',d/'empty.db')
subprocess.run(['python',str(src/'verification/r982/business_funnel_fixture.py'),str(d)],check=True)
run('BusinessFunnelContractCP12',[bridge,d/'empty.db',db,d/'receipt.json'],directory=d)
env=os.environ.copy();env.update(STARCORE_HOST_SCHEMA=schema.name,STARCORE_INTEGRATED_EVIDENCE=out.name+'-integrated',STARCORE_EXTENDED_EVIDENCE=out.name+'-extended')
for driver in ('run_integrated_host.py','run_extended_host.py'):
    result=subprocess.run(['python',str(src/'verification/r982'/driver)],env=env,cwd=src)
    if result.returncode: raise SystemExit(result.returncode)
d,db=fixture('ModernSeedRegistrationContractCP12');shutil.copy2(root/'evidence'/env['STARCORE_INTEGRATED_EVIDENCE']/'ModernSeedImportContractCP12/fixture.db',db)
c=sqlite3.connect(db);s=sqlite3.connect(root/'evidence/replay-prerequisites/ModernFamilyContractCP12/fixture.db')
for table in ('draws','v57_settlement_task_r36'):
    rows=s.execute('SELECT * FROM '+table).fetchall();c.executemany('INSERT OR REPLACE INTO '+table+' VALUES('+','.join('?' for _ in rows[0])+')',rows)
c.commit();c.close();s.close();run('ModernSeedRegistrationContractCP12',[bridge,db,d/'files',d/'receipt.json'],directory=d)
print('BASE_COUNTS',{s:sum(x['status']==s for x in receipts) for s in {'PASS','FAIL'}},flush=True)
