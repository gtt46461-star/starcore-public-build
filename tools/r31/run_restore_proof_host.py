#!/usr/bin/env python3
"""B05-C10 actual native copy/process/consumer controls. Android/Binder stay NOT_RUN."""
import argparse,hashlib,json,os,pathlib,subprocess,sys,tempfile
root=pathlib.Path(__file__).resolve().parents[2]
def sha(p):return hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest()
def classpath_identity(path):
 path=pathlib.Path(path).resolve()
 if path.is_file():return {'path':str(path),'kind':'FILE','sha256':sha(path),'files':1,'bytes':path.stat().st_size}
 if not path.is_dir():raise RuntimeError('COMPILED_SUPPORT_MISSING')
 rows=[(str(p.relative_to(path)),sha(p),p.stat().st_size) for p in sorted(path.rglob('*')) if p.is_file()]
 if not rows:raise RuntimeError('COMPILED_SUPPORT_EMPTY')
 digest=hashlib.sha256(json.dumps(rows,separators=(',',':'),ensure_ascii=True).encode()).hexdigest()
 return {'path':str(path),'kind':'DIRECTORY','sha256':digest,'files':len(rows),'bytes':sum(row[2] for row in rows)}
def file_identities(paths):return {str(pathlib.Path(p).resolve()):sha(p) for p in paths}
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',type=pathlib.Path);p.add_argument('--native-dir',type=pathlib.Path);a=p.parse_args();out=(a.output or pathlib.Path(tempfile.mkdtemp(prefix='r31-restore-proof-host-'))).resolve();out.mkdir(parents=True,exist_ok=True)
 sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';classes=pathlib.Path(os.environ.get('R31_HOST_CLASSES',root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'));jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));gradle=pathlib.Path(os.environ['GRADLE_USER_HOME'])
 junit=next(gradle.rglob('junit-4.13.2.jar'));hamcrest=next(gradle.rglob('hamcrest-core-1.3.jar'));source=root/'app/src/main/java/com/ethk3/novaonepro'
 names=['DatabaseCoordinator','StorageMaintenanceFileR82','StorageMaintenanceServiceR82','StorageRestoreProofR31','StorageRestoreProbeServiceR31','StorageRestoreBusinessProbeR31','StorageCheckpointR31','ModelBundleManagerR929','ModelBundleRuntimeR932','SettlementTaskStoreR36']
 names += ['strategyv2/'+s for s in ['ResearchContextR30R1','ResearchResultContractR31','ResearchConsumptionStoreR31','ResearchStoreR30R1','EvolutionLoopR27','CheckpointArtifactsR26','ChunkedCheckpointR26','AtomNetSessionR30','AtomNetR27','ModelArtifactR29','LiveStateR26','LegacyReplayStateR26','ExpertExperimentR26','ReplayWorkspaceR26','CampaignStateSU1','ArchiveStoreSU1','KeepMigrationStoreR31','KeepMigrationR31']]
 production=[source/(n+'.java') for n in names];test=[root/'tools/r31/storage_host/R31RestoreProofHost.java',root/'app/src/test/java/com/ethk3/novaonepro/R31StorageCheckpointRebuildTest.java']
 original_platform=[*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),root/'tools/r31/storage_host/platform/android/os/SystemClock.java',root/'tools/r31/storage_host/platform/android/os/StatFs.java']
 inputs=production+test+original_platform+[root/'app/src/androidTest/java/com/ethk3/novaonepro/R31RestoreProofInstrumentedTest.java',pathlib.Path(__file__).resolve(),root/'app/src/main/cpp/storage_maintenance_r82.inc'];before={str(f.relative_to(root)):sha(f) for f in inputs};(out/'source-before.json').write_text(json.dumps(before,indent=2)+'\n')
 support_before=classpath_identity(classes);libraries_before=file_identities([sdk,jdbc,junit,hamcrest]);(out/'compiled-support-before.json').write_text(json.dumps({'compiledSupport':support_before,'runtimeLibraries':libraries_before},indent=2)+'\n')
 def run(cmd,log,cwd=root):
  with (out/log).open('w') as f:subprocess.run(list(map(str,cmd)),cwd=cwd,stdout=f,stderr=subprocess.STDOUT,check=True)
 native=a.native_dir.resolve() if a.native_dir else out/'native';binding=native/'restore-binding.json'
 if a.native_dir:
  run([sys.executable,root/'verification/batch01/build_host_report_native.py','--verify-reuse',native],'native-reuse-verification.log')
  data=json.loads(binding.read_text());assert data['nativeLibrarySha256']==sha(native/'libstarcore_report_cp4.so');assert all(sha(root/path)==value for path,value in data['sourceHashes'].items()),'REUSED_NATIVE_INPUT_CHANGED'
 else:
  cpp=[root/'app/src/main/cpp/report_native_cp4.c',root/'app/src/main/cpp/storage_maintenance_r82.inc'];native_before={str(f.relative_to(root)):sha(f) for f in cpp}
  run([sys.executable,root/'verification/batch01/build_host_report_native.py','--jdk',os.environ['JAVA_HOME'],'--output-dir',native],'native-build.log');assert native_before=={str(f.relative_to(root)):sha(f) for f in cpp},'NATIVE_INCLUDE_CHANGED_DURING_BUILD'
  data={'sourceHashes':native_before,'nativeLibrarySha256':sha(native/'libstarcore_report_cp4.so')};binding.write_text(json.dumps(data,indent=2)+'\n')
 native_receipt=json.loads((native/'receipt.json').read_text());native_receipt_sha=sha(native/'receipt.json');assert all(sha(root/f['path'])==f['sha256'] for f in native_receipt['source_files']);assert sha(native/'libstarcore_report_cp4.so')==native_receipt['library_sha256']
 shim=out/'shim';compiled=out/'production';drivers=out/'drivers';generated=out/'platform-source'
 for d in [shim,compiled,drivers,generated]:d.mkdir(exist_ok=True)
 platform=[]
 for f in (root/'tools/r29/qual_host/shim').rglob('*.java'):
  target=generated/f.relative_to(root/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True);s=f.read_text()
  if f.name=='SQLiteDatabase.java':s=s.replace('public final Connection jdbc;','public final Connection jdbc;\n    public String getPath(){try(java.sql.Statement s=jdbc.createStatement();java.sql.ResultSet r=s.executeQuery("PRAGMA database_list")){while(r.next())if("main".equals(r.getString(2)))return r.getString(3);throw new IllegalStateException("HOST_DB_PATH");}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}')
  target.write_text(s);platform.append(target)
 adapters={'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}}','android/os/Build.java':'package android.os;public final class Build{public static final class VERSION{public static final int SDK_INT=35;}}'}
 for name,text in adapters.items():f=generated/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(text);platform.append(f)
 platform.append(root/'tools/r31/storage_host/platform/android/os/SystemClock.java')
 platform.append(root/'tools/r31/storage_host/platform/android/os/StatFs.java')
 platform_generated_hashes=file_identities(platform);(out/'generated-platform-source-sha256.json').write_text(json.dumps(platform_generated_hashes,indent=2)+'\n')
 run(['javac','-encoding','UTF-8','-cp',sdk,'-d',shim,*platform],'platform-javac.log')
 cp=os.pathsep.join(map(str,[classes,sdk]));run(['javac','-proc:none','-implicit:none','-sourcepath','','-cp',cp,'-d',compiled,*production],'production-javac.log')
 cp=os.pathsep.join(map(str,[shim,compiled,classes,jdbc,sdk,junit,hamcrest]));run(['javac','-proc:none','-implicit:none','-sourcepath','','-cp',cp,'-d',drivers,*test],'driver-javac.log');runtime=os.pathsep.join([str(drivers),cp]);execution_before={name:classpath_identity(path) for name,path in [('platform',shim),('production',compiled),('drivers',drivers)]}
 run(['java','-Xmx512m','-Djava.library.path='+str(native),'-cp',runtime,'com.ethk3.novaonepro.R31RestoreProofHost','parent',out/'fixture'],'native-process.log')
 run(['java','-Xmx256m','-Djava.library.path='+str(native),'-cp',runtime,'org.junit.runner.JUnitCore','com.ethk3.novaonepro.R31StorageCheckpointRebuildTest'],'original-protocol-junit.log',root/'app')
 assert before=={str(f.relative_to(root)):sha(f) for f in inputs},'SOURCE_CHANGED_DURING_HOST_RUN'
 assert support_before==classpath_identity(classes),'COMPILED_SUPPORT_CHANGED_DURING_HOST_RUN'
 assert libraries_before==file_identities([sdk,jdbc,junit,hamcrest]),'RUNTIME_LIBRARY_CHANGED_DURING_HOST_RUN'
 assert platform_generated_hashes==file_identities(platform),'GENERATED_PLATFORM_SOURCE_CHANGED_DURING_HOST_RUN'
 assert execution_before=={name:classpath_identity(path) for name,path in [('platform',shim),('production',compiled),('drivers',drivers)]},'EXECUTED_CLASS_BYTES_CHANGED_DURING_HOST_RUN'
 run([sys.executable,root/'verification/batch01/build_host_report_native.py','--verify-reuse',native],'native-final-input-verification.log')
 assert sha(native/'receipt.json')==native_receipt_sha and json.loads((native/'receipt.json').read_text())==native_receipt,'NATIVE_RECEIPT_CHANGED_DURING_HOST_RUN'
 assert all(sha(root/f['path'])==f['sha256'] for f in native_receipt['source_files']),'NATIVE_INPUT_CHANGED_DURING_HOST_RUN'
 assert sha(native/'libstarcore_report_cp4.so')==native_receipt['library_sha256'],'NATIVE_BINARY_CHANGED_DURING_HOST_RUN'
 receipt={'status':'PASS_HOST_CONTROLS_ONLY','scope':'REAL_NATIVE_REBUILD_COPY_SEPARATE_JVM_MODEL_REFERENCE_READERS_AND_REFUSAL_GATES; NOT_ANDROID_STARTUP_OR_BINDER_OR_CLEANUP_AUTHORIZATION','sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'sourceSha256':before,'freshCompiledProductionFiles':len(production),'compiledSupport':support_before,'supportScope':'EXPLICIT_COMPILED_DEPENDENCIES_ONLY; FRESH_WHOLE_ANDROID_SOURCE_GATE_REQUIRED_SEPARATELY','runtimeLibrarySha256':libraries_before,'generatedPlatformSourceSha256':platform_generated_hashes,'executedClasses':execution_before,'nativeReceiptSha256':native_receipt_sha,'nativeSourceSha256':{f['path']:f['sha256'] for f in native_receipt['source_files']},'nativeLibrarySha256':sha(native/'libstarcore_report_cp4.so'),'AndroidStartup':'NOT_RUN','BinderCallerAuthentication':'COMPILED_NOT_RUN','productionCleanupAuthorization':'NOT_GRANTED','handset':'NOT_RUN','naturalQuality':'NOT_APPLICABLE'};(out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
if __name__=='__main__':main()
