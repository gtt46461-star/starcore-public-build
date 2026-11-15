#!/usr/bin/env python3
"""Current C/Java owners, real cross-process SQLite WAL and file leases.
No Android Binder, handset IO bound or uninterrupted-runtime evidence is inferred.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,sqlite3,subprocess,tempfile,time,uuid,shutil
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--classes',type=Path,required=True);p.add_argument('--reuse-native-from',type=Path);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
cpp=ROOT/'app/src/main/cpp';sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdk=Path(os.environ['JAVA_HOME']);support_source=a.classes.resolve();classes=out/'support';shutil.copytree(support_source,classes)
production=[ROOT/'app/src/main/java/com/ethk3/novaonepro'/n for n in ['StorageCheckpointR31.java','SourceReadLeaseR82.java','StorageReclaimR943.java','StorageHealthR811.java','DatabaseCompactionSU2.java','ReportNativeCP4.java','StorageCheckpointClientR31.java','StorageCheckpointServiceR31.java']]
native=[cpp/'report_native_cp4.c',cpp/'vendor/sqlite3.c',*sorted(cpp.glob('*.inc')),*sorted(cpp.rglob('*.h'))]
for part in ['common','compress','decompress']:native+=sorted((cpp/'vendor/zstd/lib'/part).glob('*.c'))
inputs=production+native+[Path(__file__),ROOT/'tools/r31/unified_host/StorageCheckpointNativeR31Host.c',ROOT/'tools/r31/unified_host/SourceReaderOwnerR31Host.java',ROOT/'tools/r31/unified_host/StorageCompactionReadOnlyR31Host.java',ROOT/'app/src/androidTest/java/com/ethk3/novaonepro/R31StorageCheckpointAndroidTest.java',ROOT/'app/src/main/AndroidManifest.xml']
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
def class_digest(folder):
 h=hashlib.sha256()
 for p in sorted(folder.rglob('*.class')):h.update(str(p.relative_to(folder)).encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
 return h.hexdigest()
receipt={'format':'R31_ACTUAL_WAL_CHECKPOINT_HOST_1','invocationId':str(uuid.uuid4()),'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'sourceHashes':{str(p.relative_to(ROOT)):sha(p) for p in inputs},'status':'STARTED_NOT_PASS','scope':'ACTUAL_SOURCE_NATIVE_SQLITE_FILE_LEASES_HOST;ANDROID_BINDER_AND_DEVICE_IO_BOUND_NOT_RUN','records':[],'compiledSupportCopySha256':class_digest(classes),'compiledSupportScope':'DEPENDENCY_ABI_SNAPSHOT_ONLY;WHOLE_CURRENT_ANDROID_BUILD_REQUIRED'}
def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
def run(cmd,label,timeout=180):
 r=subprocess.run(list(map(str,cmd)),cwd=ROOT,capture_output=True,text=True,timeout=timeout);(out/(label+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'stage':label,'exitCode':r.returncode});save();r.check_returncode();return r.stdout
save();cli=out/'checkpoint-native';compile_sources=[cpp/'vendor/sqlite3.c']+[p for p in native if p.suffix=='.c' and p not in [cpp/'report_native_cp4.c',cpp/'vendor/sqlite3.c']]
if a.reuse_native_from:
 prior=json.loads((a.reuse_native_from/'RECEIPT.json').read_text());assert any(r['stage']=='compile-current-native-owner' and r['exitCode']==0 for r in prior['records'])
 for path in native+[ROOT/'tools/r31/unified_host/StorageCheckpointNativeR31Host.c']:
  if sha(path)!=prior.get('sourceHashes',{}).get(str(path.relative_to(ROOT))):raise RuntimeError('NATIVE_COMPLETE_HEADER_OR_INCLUDE_IDENTITY_MISSING_OR_CHANGED_REBUILD_REQUIRED')
 shutil.copyfile(a.reuse_native_from/'checkpoint-native',cli);cli.chmod(0o700);receipt['reusedOnlyNativeCompilation']={'priorReceiptSha256':sha(a.reuse_native_from/'RECEIPT.json'),'binarySha256':sha(cli),'testResultsReused':False};save()
else:
 run(['gcc','-std=c11','-O2','-DCP4_HOST','-DSQLITE_THREADSAFE=1','-DSQLITE_OMIT_LOAD_EXTENSION=1','-DZSTD_DISABLE_ASM=1','-I'+str(cpp/'vendor'),'-I'+str(cpp/'vendor/zstd/lib'),*compile_sources,ROOT/'tools/r31/unified_host/StorageCheckpointNativeR31Host.c','-o',cli,'-ldl','-lm','-pthread'],'compile-current-native-owner',300)
run(['gcc','-std=c11','-fsyntax-only','-DSQLITE_THREADSAFE=1','-DZSTD_DISABLE_ASM=1','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-I'+str(cpp/'vendor'),'-I'+str(cpp/'vendor/zstd/lib'),cpp/'report_native_cp4.c'],'compile-current-jni-signature')
run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[classes,sdk])),'-d',out/'product',*production],'compile-current-java-owners')
checks=[]
def check(value,name):
 if not value:raise AssertionError(name)
 checks.append(name)
def checkpoint(path,mode=0,allowed=True):
 start=time.monotonic();r=subprocess.run([str(cli),str(path),str(mode),str(int(allowed))],capture_output=True,text=True,timeout=3);r.check_returncode();values=list(map(int,r.stdout.strip().split(',')));check(len(values)==8,'NATIVE_RESULT_8_FIELDS');check(time.monotonic()-start<1,'NATIVE_ZERO_BUSY_WAIT_OBSERVED_HOST');receipt.setdefault('actualNativeCalls',[]).append({'mode':mode,'controlPermitted':allowed,'nativeResult':values,'hostProcessDurationMs':int((time.monotonic()-start)*1000)});save();return values
dbpath=out/'platform-created.db';writer=sqlite3.connect(dbpath,isolation_level=None);writer.execute('PRAGMA journal_mode=WAL');writer.execute('PRAGMA wal_autocheckpoint=0');writer.execute('CREATE TABLE original_assets(id INTEGER PRIMARY KEY,payload BLOB)');writer.execute('INSERT INTO original_assets(payload) VALUES(?)',(b'PRESERVE_TRAINED_ASSET',));writer.execute('CREATE TABLE frames(id INTEGER PRIMARY KEY,payload BLOB)');writer.execute('BEGIN');writer.executemany('INSERT INTO frames(payload) VALUES(?)',[(bytes([i%256])*4096,) for i in range(128)]);writer.execute('COMMIT')
reader=sqlite3.connect(dbpath,isolation_level=None);reader.execute('BEGIN');reader.execute('SELECT count(*) FROM frames').fetchone();writer.execute('BEGIN');writer.executemany('INSERT INTO frames(payload) VALUES(?)',[(b'new'*1500,) for _ in range(16)]);writer.execute('COMMIT')
blocked=checkpoint(dbpath);check(blocked[2]>blocked[3]>=0,'ACTUAL_PASSIVE_LONG_READER_PINS_UNCHECKPOINTED_FRAMES');deferred=checkpoint(dbpath,1);check(deferred[0]!=0 and deferred[1]==1 and deferred[7]==0,'TRUNCATE_REFUSES_NONEMPTY_BACKFILL_PROBE')
for i in range(5):writer.execute('INSERT INTO frames(payload) VALUES(?)',(b'critical-'+bytes([i]),))
check(writer.execute('SELECT count(*) FROM frames').fetchone()[0]==149,'ACTUAL_CRITICAL_COMMITS_CONTINUE_WHILE_READER_PINS_WAL')
reader.execute('ROLLBACK');reader.close();done=checkpoint(dbpath);check(done[0]==0 and done[2]==done[3] and done[7]==1,'ACTUAL_PASSIVE_RECOVERS_AFTER_READER_OWNER_RELEASE');truncated=checkpoint(dbpath,1);check(truncated[0]==0 and truncated[1]==0 and (not Path(str(dbpath)+'-wal').exists() or Path(str(dbpath)+'-wal').stat().st_size==0),'CONTROLLED_NATIVE_TRUNCATE_ZERO_WAIT_REAL_WAL_SHRINK')
empty_wal=checkpoint(dbpath);check(empty_wal[0]==0 and empty_wal[7]==0,'EMPTY_WAL_PROBE_NOT_AN_EXECUTED_PASSIVE')
check(writer.execute('SELECT payload FROM original_assets').fetchone()[0]==b'PRESERVE_TRAINED_ASSET','TRAINED_ASSET_BYTES_SURVIVE_NATIVE_PLATFORM_INTEROP');check(writer.execute('PRAGMA integrity_check').fetchone()[0]=='ok','PLATFORM_REOPEN_NATIVE_CHECKPOINT_INTEGRITY');cancelled_native=checkpoint(dbpath,0,False);check(cancelled_native[0]==-1101 and cancelled_native[7]==0,'CANCEL_BEFORE_NATIVE_OPERATION_NO_CHECKPOINT')
writer.execute('INSERT INTO frames(payload) VALUES(?)',(b'large-wal-start',));largeReader=sqlite3.connect(dbpath,isolation_level=None);largeReader.execute('BEGIN');largeReader.execute('SELECT count(*) FROM frames').fetchone();writer.execute('BEGIN');writer.executemany('INSERT INTO frames(payload) VALUES(?)',[(bytes(8192),) for _ in range(700)]);writer.execute('COMMIT');large=checkpoint(dbpath);check(large[0]==-1102 and large[7]==0,'OVERSIZED_UNBACKFILLED_WAL_PRESERVED_WITH_EXPLICIT_ONLINE_COPY_BUDGET_WAIT');largeReader.execute('ROLLBACK');largeReader.close();writer.close()
legacy=out/'legacy.db';old=sqlite3.connect(legacy);old.execute('CREATE TABLE original(value TEXT)');old.execute("INSERT INTO original VALUES('PRESERVE_OLD_DB')");old.commit();old.close();before=sha(legacy);refused=checkpoint(legacy);check(refused[0]==-1103 and before==sha(legacy),'LEGACY_NONWAL_DB_NEVER_CONVERTED_OR_RECOVERED_AUTOMATICALLY')
source=(ROOT/'app/src/main/java/com/ethk3/novaonepro/DatabaseCompactionSU2.java').read_text();check('db.execSQL("VACUUM")' not in source and 'wal_checkpoint(TRUNCATE)' not in source,'ROUTINE_CONVERSION_HAS_NO_VACUUM_OR_TRUNCATE');check('ONLINE_TRUNCATE_VALIDATED_R31=false' in (ROOT/'app/src/main/java/com/ethk3/novaonepro/StorageReclaimR943.java').read_text(),'UNVERIFIED_DEVICE_TRUNCATE_DEFAULT_DISABLED')
platform=out/'platform';cancel=out/'shim/android/os/CancellationSignal.java';cancel.parent.mkdir(parents=True);cancel.write_text('package android.os;public class CancellationSignal{private volatile boolean cancelled;public void cancel(){cancelled=true;}public boolean isCanceled(){return cancelled;}public void throwIfCanceled(){if(cancelled)throw new java.util.concurrent.CancellationException();}}')
run(['javac','-d',platform,cancel],'compile-owner-signal-transport-shim');cp=os.pathsep.join(map(str,[platform,out/'product',classes,sdk]));run(['javac','-cp',cp,'-d',out/'tests',ROOT/'tools/r31/unified_host/SourceReaderOwnerR31Host.java'],'compile-actual-reader-owner-fixture');run(['java','-cp',str(out/'tests')+os.pathsep+cp,'com.ethk3.novaonepro.SourceReaderOwnerR31Host','parent',out/'lease-fixture'],'real-cross-process-reader-owner-cancel')
shim=[]
for original in (ROOT/'tools/r29/qual_host/shim').rglob('*.java'):
 target=out/'sqlite-shim'/original.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True);text=original.read_text()
 if original.name=='SQLiteDatabase.java':text=text.replace('    static final class Rows','    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}\n    static final class Rows')
 target.write_text(text);shim.append(target)
run(['javac','-cp',cp,'-d',platform,*shim],'compile-native-sqlite-platform-adapter');jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));cp+=os.pathsep+str(jdbc)
run(['javac','-cp',cp,'-d',out/'tests',ROOT/'tools/r31/unified_host/StorageCompactionReadOnlyR31Host.java'],'compile-actual-routine-negative-fixture')
old=sqlite3.connect(legacy);old.execute('CREATE TABLE freelist_repro(payload BLOB)');old.executemany('INSERT INTO freelist_repro VALUES(?)',[(bytes(4096),) for _ in range(2048)]);old.commit();old.execute('DELETE FROM freelist_repro WHERE rowid<=1800');old.commit();check(old.execute('PRAGMA freelist_count').fetchone()[0]>1024,'ACTUAL_LEGACY_VACUUM_PATH_REPRO_HAS_LARGE_FREELIST');old.close()
baseline=out/'baseline/com/ethk3/novaonepro/DatabaseCompactionSU2.java';baseline.parent.mkdir(parents=True);baseline.write_text(subprocess.check_output(['git','show','97e53aee275238f73c2c0e5856f3fedcf8151324:app/src/main/java/com/ethk3/novaonepro/DatabaseCompactionSU2.java'],cwd=ROOT,text=True));baseline_db=out/'baseline-legacy.db';shutil.copyfile(legacy,baseline_db)
run(['javac','-cp',cp,'-d',out/'baseline-classes',baseline],'compile-original-compaction-red-owner');red=subprocess.run(['java','-cp',str(out/'baseline-classes')+os.pathsep+str(out/'tests')+os.pathsep+cp,'com.ethk3.novaonepro.StorageCompactionReadOnlyR31Host',baseline_db],capture_output=True,text=True,timeout=20);(out/'baseline-real-red.log').write_text(red.stdout+red.stderr);check(red.returncode!=0 and 'SYNC_LEGACY_REBUILD_ENTERED' in red.stderr,'ORIGINAL_REAL_ROUTINE_SYNC_VACUUM_REPRODUCED_RED')
observations=[('ACTUAL_PASSIVE_RESULT',blocked),('ACTUAL_PASSIVE_RESULT',done),('CANCELLED_NATIVE_BUDGET',cancelled_native),('RESOURCE_WAIT_NATIVE_COPY_BUDGET',large),('PASSIVE_PRECHECK_NOT_EXECUTED',refused),('PASSIVE_NOT_ATTEMPTED_NO_WAL',empty_wal)]
run(['java','-cp',str(out/'tests')+os.pathsep+cp,'com.ethk3.novaonepro.StorageCompactionReadOnlyR31Host',legacy,*[state+':'+','.join(map(str,values)) for state,values in observations]],'actual-routine-offline-negative-and-original-byte-preservation')
check(True,'ACTUAL_NATIVE_BUDGET_CANCEL_UNSAFE_NEVER_CLASSIFIED_AS_EXECUTED_PASSIVE')
check(all(sha(p)==receipt['sourceHashes'][str(p.relative_to(ROOT))] for p in inputs),'CURRENT_SOURCES_UNCHANGED_DURING_EXECUTION');receipt.update(status='PASS_TARGETED_NATIVE_AND_OWNER_NOT_ANDROID_OR_PHONE',checks=checks,nativeCheckpointSha256=sha(cli),platformSqliteVersion=sqlite3.sqlite_version,deviceIoBound='NOT_RUN',androidBinder='NOT_RUN',onlineTruncateDefault='DISABLED_UNVALIDATED_DEVICE_IO_BOUND');save();print('STORAGE_CHECKPOINT_NATIVE_HOST_PASS checks='+str(len(checks))+' actualPlatformNativeWalInterop=YES actualCriticalCommits=5 AndroidBinder=NOT_RUN deviceIoBound=NOT_RUN')
