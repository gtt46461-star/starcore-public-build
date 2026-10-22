#!/usr/bin/env python3
"""Run unchanged canonical assertions against freshly built product classes.
Each SQLite fixture is isolated. Passes are host engineering evidence, never device/Future proof.
"""
from pathlib import Path
import argparse, hashlib, json, re, shutil, subprocess, time

p=argparse.ArgumentParser()
for flag in ('java-home','classpath','schema','native','out'):
    p.add_argument('--'+flag,required=True)
a=p.parse_args();src=Path(__file__).resolve().parents[2];out=Path(a.out).resolve()
out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
java=Path(a.java_home)/'bin/java';javac=java.with_name('javac')
cp=str(classes)+':'+a.classpath
tests=json.loads((src/'verification/canonical/TEST_SOURCES.json').read_text())
extra=[src/'verification/canonical/extended/ModernModelContractTest.java',
       *sorted((src/'verification/canonical/adapters').rglob('*.java')),
       src/'verification/r994/adapters/android/os/StatFs.java',
       src/'verification/r996/android/system/Os.java']
extra=[f for f in extra if not str(f).endswith('canonical/adapters/android/system/Os.java')]
extra+=list((src/'verification').rglob('TrainingFixtureCP11.java'))
cmd=[str(javac),'-encoding','UTF-8','-cp',cp,'-d',str(classes),
     *map(str,extra),*[str(src/f) for f in tests]]
q=subprocess.run(cmd,capture_output=True,text=True)
(out/'compile.log').write_text(q.stdout+q.stderr)
if q.returncode:raise SystemExit('CANONICAL_TEST_COMPILE_FAILED: '+str(out/'compile.log'))
pure={'EvolutionStoragePolicyCP12Test','ExclusiveStageClockCP12Test','ResumableSha256CP12Test',
      'MatrixProjectionCP12Test','ClockAnchorContractCP12','ReportSemanticContractCP12',
      'CertificateAtomCapCP12Test','DrawGapMin21CP12Test'}
receipt_only={'MutationGraphContractCP12','ResearchFeedbackMetricsContractCP12',
              'RecipeMemoryContractCP12','CoreRetirementPitContractCP12','CP9KernelContractCP12'}
io={'ReportPublicationContractCP12','ReportSpoolContractCP12','ReportQueueRecoveryContractCP12',
    'ReportSafContractCP12','ReportSortContractCP12'}
asset=src/'app/src/main/assets/cp4_atom_registry.jsonl'
seeds=src/'app/src/main/assets/research/cp4_molecule_seeds.jsonl'
results=[]
for path in tests:
    f=src/path;name=f.stem;d=out/name;d.mkdir(exist_ok=True)
    receipt=d/'receipt.json';files=d/'files';files.mkdir(exist_ok=True)
    db=d/'fixture.db';shutil.copyfile(a.schema,db)
    args=[str(src/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(receipt)]
    if name in pure:args=[]
    elif name in receipt_only:args=[str(receipt)]
    elif name in io:args=[str(files)]
    elif name in {'MutationOperatorsCP12Test','MarkovOrder2CP12Test'}:args=[str(asset)]
    elif name=='MutationNewFamilyCP12Test':args=[str(asset),str(receipt)]
    elif name=='ResearchDirichletCallerCP12Test':args=[str(asset),args[0],str(db)]
    elif name in {'RetentionIoContractCP12','RetentionPublishedProofContractCP12'}:
        args=[str(files),str(receipt)]
    elif name=='ModernSeedImportContractCP12':args=[args[0],str(db),str(seeds),str(receipt)]
    elif name in {'ModernFamilyContractCP12','ModernSeedRegistrationContractCP12'}:
        args=[args[0],str(db),str(files),str(receipt)]
    elif name=='ColdCreatorPublicContractCP12':
        args=[args[0],str(db),str(asset),str(seeds),str(files),str(receipt)]
    elif name=='AtomAssetFdCP12Test':args=[args[0],str(db),str(asset),str(d/'container.bin')]
    elif name=='AtomCatalogImportCP12Test':args=[args[0],str(db),str(asset)]
    package=re.search(r'package\s+([\w.]+)\s*;',f.read_text())
    cmd=[str(java),'-Xmx256m','-Djava.library.path='+str(Path(a.native).resolve()),
         '-cp',cp,(package.group(1)+'.' if package else '')+name,*args]
    started=time.monotonic()
    try:
        with (d/'execution.log').open('w') as log:
            q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=150)
        status='PASS' if q.returncode==0 else 'FAIL';code=q.returncode
    except subprocess.TimeoutExpired:status='TIMEOUT';code=None
    results.append({'test':name,'status':status,'exit_code':code,'command':cmd,
        'seconds':round(time.monotonic()-started,3),'test_sha256':hashlib.sha256(f.read_bytes()).hexdigest(),
        'scope':'FRESH_FULL_PRODUCT_HOST_SQLITE_SYNTHETIC_NOT_DEVICE_NOT_MODEL_PERFORMANCE'})
    (out/'test_ledger.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
    print(name,status,flush=True)
print('COUNTS',{s:sum(r['status']==s for r in results)for s in {'PASS','FAIL','TIMEOUT'}},flush=True)
raise SystemExit(0 if all(r['status']=='PASS' for r in results) else 1)
