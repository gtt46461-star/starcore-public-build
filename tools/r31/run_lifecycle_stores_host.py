#!/usr/bin/env python3
"""Actual lifecycle declarations/owner and publisher guard integration on SQLite.
Final gates use one source tree. --source-map is an explicit multi-worktree
interface precheck and is never labeled a final acceptance result.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
OWNERS=[
 'LifecycleSchemaR31','SchemaDomainCoordinatorR926','LegacyDdlR31','StorageGcR31',
 'strategyv2/QualificationInboxR29','strategyv2/ModelSurfaceStoreR30','strategyv2/AtomNetPublicationR31','strategyv2/ModelPublicationR31','strategyv2/ModelArtifactR29','strategyv2/StrategySchemaSU2','strategyv2/StartupCheckpointIdentityR31','strategyv2/MainlineFreezeProofR31','strategyv2/KeepArchiveRetirementR31','strategyv2/GapReferenceR31','strategyv2/RetentionR26','strategyv2/ResearchRetentionR30',
 'StorageReclaimR943','StorageCheckpointR31','StorageCheckpointClientR31','StorageCheckpointServiceR31','SourceReadLeaseR82','ReportNativeCP4',
 'strategyv2/ReplayKnowledgeStoreR31','strategyv2/ReplayDatasetR27','strategyv2/ChartCacheR27',
 'strategyv2/ResearchTaskRegistryR30R1','strategyv2/ResearchConsumptionStoreR31','strategyv2/RuntimeCoordinatorR26','strategyv2/ReportR29','strategyv2/ReportSnapshotR30',
 'strategyv2/KeepMigrationR31','strategyv2/KeepMigrationStoreR31','strategyv2/KeepMismatchReferenceR31','strategyv2/ResearchRetentionStoreR30','strategyv2/WarmPublicationR31','strategyv2/QualificationGapRepairR30R1',
 'strategyv2/CheckpointGcStoreR31','strategyv2/CheckpointScratchGcR31','strategyv2/ArtifactMaintenanceR26','strategyv2/CheckpointArtifactsR26','strategyv2/CheckpointReferencesR26','strategyv2/CheckpointManifestR26',
 'strategyv2/ResearchHypothesisR31','strategyv2/HypothesisStoreR31','strategyv2/FailureDirectedResearchR26','strategyv2/FoundryCampaignR26',
 'strategyv2/EvidenceContractR31','strategyv2/EvidenceContractStoreR31','strategyv2/FactoryCapabilityStoreR31','strategyv2/FactoryCapabilityExperimentR31']
def files_digest(paths,base):
 h=hashlib.sha256()
 for path in sorted(paths):h.update(str(path.relative_to(base)).encode()+b'\0'+hashlib.sha256(path.read_bytes()).digest())
 return h.hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--source-map',type=Path);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 classes=Path(os.environ.get('R31_COMPILED_SUPPORT',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 names=['app/src/main/java/com/ethk3/novaonepro/'+name+'.java' for name in OWNERS]
 sources={name:ROOT/name for name in names}
 if a.source_map:
  declared=json.loads(a.source_map.read_text());assert all(name in declared for name in names)
  names=sorted(declared);assert all(name.startswith('app/src/main/java/') and name.endswith('.java') for name in names)
  sources={name:Path(declared[name]).resolve() for name in names}
 sources.update({'tools/r31/unified_host/LifecycleStoresR31Host.java':ROOT/'tools/r31/unified_host/LifecycleStoresR31Host.java','tools/r31/run_lifecycle_stores_host.py':Path(__file__).resolve()})
 hashes={name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in sources.items()};support_before=files_digest(classes.rglob('*.class'),classes);commands=[]
 (out/'INPUTS_BEFORE.json').write_text(json.dumps({'sources':hashes,'compiledSupportSha256':support_before},indent=2)+'\n')
 def run(command,name):
  r=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=180);(out/name).write_text(r.stdout+r.stderr);commands.append({'command':list(map(str,command)),'exitCode':r.returncode,'log':name})
  if r.returncode:raise RuntimeError(name+'\n'+(r.stdout+r.stderr)[-5000:])
  return r.stdout
 with tempfile.TemporaryDirectory(prefix='r31-lifecycle-stores-') as temp:
  work=Path(temp);platform=work/'platform';production=work/'production';test=work/'test'
  for d in [platform,production,test]:d.mkdir()
  shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'));shims.extend((ROOT/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
  run(['javac','-cp',str(sdk),'-d',platform,*shims],'platform.log')
  run(['javac','-J-Xmx512m','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',production,*[sources[name] for name in names]],'production.log')
  cp=os.pathsep.join(map(str,[platform,production,test,classes,jdbc,slf,sdk]))
  run(['javac','-cp',cp,'-d',test,sources['tools/r31/unified_host/LifecycleStoresR31Host.java']],'host.log')
  result=run(['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.LifecycleStoresR31Host',work/'owner.db'],'run.log')
 after={name:hashlib.sha256(path.read_bytes()).hexdigest() for name,path in sources.items()}
 if hashes!=after:raise RuntimeError('SOURCE_CHANGED_DURING_EXECUTION:'+','.join(name for name in hashes if hashes[name]!=after[name]))
 if support_before!=files_digest(classes.rglob('*.class'),classes):raise RuntimeError('COMPILED_SUPPORT_CHANGED_DURING_EXECUTION')
 print(result)
 receipt={'state':'PASS_NATIVE_INTERFACE_PRECHECK_NOT_FINAL_GATE' if a.source_map else 'PASS_HOST_NATIVE_CURRENT_STORE_DECLARATIONS','sourceMode':'MULTI_WORKTREE_EXPLICIT_SOURCE_MAP' if a.source_map else 'SINGLE_CURRENT_SOURCE_TREE','scope':'ACTUAL_LIFECYCLE_OWNER_AND_STORE_DECLARATIONS; MINIMAL_CAS_PUBLISHER_FIXTURES; NOT_ANDROID_OR_COMPLETE_GC_OR_FULL_REPORT','capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'workingTreeDirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sources':hashes,'compiledSupportSha256':support_before,'sourceAndCompiledSupportUnchangedDuringExecution':True,'commands':commands,'maxActualDdlPerTransaction':1,'actualStoreDeclarations':True,'dynamicLateSourceAndMissingGuardRecovery':True,'deletingInsertAndUpdateRefused':True,'Android':'NOT_RUN','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'}
 (out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
if __name__=='__main__':main()
