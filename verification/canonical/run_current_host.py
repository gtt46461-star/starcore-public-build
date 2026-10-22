from pathlib import Path
import json,re,subprocess,shutil,hashlib,time,os
src=Path(__file__).resolve().parents[2];root=src.parent
base=Path(os.environ.get('STARCORE_TOOLCHAIN_ROOT','/tmp/starcore-cdaf1f33'))
cl=root/'evidence/current-schema/classes';jar=base/'toolchain/android-sdk/platforms/android-36/android.jar'
java=base/'toolchain/jdk-17.0.16+8/bin/java';bridge=Path(__file__).resolve().parent/'schema/sqlite_bridge.py'
asset=src/'app/src/main/assets/cp4_atom_registry.jsonl';seeds=src/'app/src/main/assets/research/cp4_molecule_seeds.jsonl'
out=root/'evidence/current-host';out.mkdir(exist_ok=True)
tests=json.loads((Path(__file__).resolve().parent/'TEST_SOURCES.json').read_text());results=[]
pure={'EvolutionStoragePolicyCP12Test','ExclusiveStageClockCP12Test','ResumableSha256CP12Test','MatrixProjectionCP12Test','ClockAnchorContractCP12','ReportSemanticContractCP12','CertificateAtomCapCP12Test','DrawGapMin21CP12Test','MarkovOrder2CP12Test'}
receipt_only={'MutationGraphContractCP12','ResearchFeedbackMetricsContractCP12','RecipeMemoryContractCP12','CoreRetirementPitContractCP12','CP9KernelContractCP12'}
io={'ReportPublicationContractCP12','ReportSpoolContractCP12','ReportQueueRecoveryContractCP12','ReportSafContractCP12','ReportSortContractCP12'}
skip={'ReservedModernCaptureContractCP12','ReservedModernCaptureContractCP12'}
compile_results=[]
for path in tests:
 f=src/path;cmd=[str(java.with_name('javac')),'-cp',str(cl)+':'+str(jar),'-d',str(cl),str(f)]
 result=subprocess.run(cmd,capture_output=True,text=True)
 (out/(f.stem+'.compile.log')).write_text(result.stdout+result.stderr)
 compile_results.append({'test':f.stem,'exit_code':result.returncode,'command':cmd})
 if result.returncode:skip.add(f.stem)
(out/'compile_ledger.json').write_text(json.dumps(compile_results,indent=2))
for path in tests:
 f=src/path;name=f.stem;d=out/name;d.mkdir(exist_ok=True);receipt=d/'receipt.json';root=d/'files';root.mkdir(exist_ok=True);db=d/'fixture.db';shutil.copy2((src.parent/'evidence/current-schema/schema.db'),db)
 if name in skip:
  results.append({'test':name,'status':'NOT_RUN','reason':'Missing original fixture dependency or dedicated fixture preparation required; assertions unchanged'});continue
 args=[str(bridge),str(db),str(receipt)]
 if name in pure:args=[]
 elif name in receipt_only:args=[str(receipt)]
 elif name in io:args=[str(root)]
 elif name in ['MutationOperatorsCP12Test','MarkovOrder2CP12Test']:args=[str(asset)]
 elif name=='MutationNewFamilyCP12Test':args=[str(asset),str(receipt)]
 elif name=='ResearchDirichletCallerCP12Test':args=[str(asset),str(bridge),str(db)]
 elif name in ['RetentionIoContractCP12','RetentionPublishedProofContractCP12']:args=[str(root),str(receipt)]
 elif name=='ModernSeedImportContractCP12':args=[str(bridge),str(db),str(seeds),str(receipt)]
 elif name in ['ModernFamilyContractCP12','ModernSeedRegistrationContractCP12']:args=[str(bridge),str(db),str(root),str(receipt)]
 elif name=='ColdCreatorPublicContractCP12':args=[str(bridge),str(db),str(asset),str(seeds),str(root),str(receipt)]
 elif name=='AtomAssetFdCP12Test':args=[str(bridge),str(db),str(asset),str(d/'container.bin')]
 elif name=='AtomCatalogImportCP12Test':args=[str(bridge),str(db),str(asset)]
 elif name in ['RetentionPublishedProofContractCP12']:args=[str(root),str(receipt)]
 cmd=[str(java),'-Xmx128m','-Djava.library.path='+str(src.parent/'evidence/current-native'),'-cp',str(cl)+':'+str(jar),(re.search(r'package\s+([\w.]+)\s*;',f.read_text()).group(1)+'.' if re.search(r'package\s+([\w.]+)\s*;',f.read_text()) else '')+name]+args
 start=time.monotonic()
 try:
  with (d/'execution.log').open('w')as log:r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=150)
  status='PASS' if r.returncode==0 else 'FAIL';code=r.returncode
 except subprocess.TimeoutExpired:status='TIMEOUT';code=None
 results.append({'test':name,'status':status,'exit_code':code,'command':cmd,'elapsed_s':round(time.monotonic()-start,3),'test_source_sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'log':str(d/'execution.log'),'fixture_sha256':hashlib.sha256(((src.parent/'evidence/current-schema/schema.db')).read_bytes()).hexdigest(),'scope':'Production classes compiled from final source; bounded synthetic host fixtures; not Android device or model performance'})
 (out/'test_ledger.json').write_text(json.dumps(results,indent=2))
 print(name,status,flush=True)
print('COUNTS',{v:sum(r['status']==v for r in results)for v in {r['status'] for r in results}},flush=True)
