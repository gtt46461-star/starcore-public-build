"""Current actual keep Store/codec/consumers on native SQLite; real SIGKILL/reopen.
No Android/service/device or whole-card cutover claim is made; mismatch and gap reference producers are exercised.
"""
import argparse,ast,datetime,hashlib,json,os,pathlib,signal,sqlite3,subprocess,tempfile,time,uuid
ROOT=pathlib.Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser();p.add_argument('--out',type=pathlib.Path,required=True);p.add_argument('--gc-root',type=pathlib.Path,default=ROOT);p.add_argument('--classes',type=pathlib.Path,default=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes');a=p.parse_args()
out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
env=dict(os.environ);env.pop('JAVA_TOOL_OPTIONS',None)
def sha(p):
 if p.is_file():return hashlib.sha256(p.read_bytes()).hexdigest()
 h=hashlib.sha256()
 for child in sorted(p.rglob('*.class')):h.update(str(child.relative_to(p)).encode()+b'\0'+hashlib.sha256(child.read_bytes()).digest())
 return h.hexdigest()
sources=[ROOT/'app/src/main/java/com/ethk3/novaonepro/strategyv2'/name for name in ['KeepMigrationR31.java','KeepMigrationStoreR31.java','KeepMismatchReferenceR31.java','KeepArchiveRetirementR31.java','StartupCheckpointIdentityR31.java','GapReferenceR31.java','RetentionR26.java','DecisionExplanationStoreR30.java','ResearchRetentionR30.java','ResearchRetentionStoreR30.java','WarmPublicationR31.java','QualificationGapRepairR30R1.java']]
gc_names=['strategyv2/CheckpointGcStoreR31.java','strategyv2/CheckpointArtifactsR26.java','strategyv2/CheckpointScratchGcR31.java','strategyv2/CheckpointReferencesR26.java','strategyv2/ModelPublicationR31.java','StorageGcR31.java','EvidenceReachabilityR31.java']
sources.extend(a.gc_root/'app/src/main/java/com/ethk3/novaonepro'/name for name in gc_names)
def source_key(path):return str(path.relative_to(ROOT)) if path.is_relative_to(ROOT) else str(path)
host=ROOT/'tools/r31/unified_host/KeepStoreR31Host.java'
receipt={'format':'R31_KEEP_NATIVE_STORE_EXECUTION_1','invocationId':str(uuid.uuid4()),'source':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'scope':'ACTUAL_CURRENT_KEEP_ADAPTER_NATIVE_SQLITE_AND_FILES; HOST_NOT_ANDROID_NOT_DEVICE','sourceSha256':{source_key(s):sha(s) for s in sources},'compiledSupportSha256':sha(a.classes),'hostSha256':sha(host),'runnerSha256':sha(pathlib.Path(__file__)),'androidJarSha256':sha(sdk),'sqliteJdbcSha256':sha(jdbc),'records':[],'killReopens':[],'status':'RUNNING_NOT_PASS','destructiveCutover':'ONLY_DECLARED_MISMATCH_OWNER_WITH_ORIGINAL_CAS_RETAINED;OTHER_TABLES_NOT_AUTHORIZED','naturalQuality':'NOT_PROVEN','physicalDevice':'NOT_RUN'}
def save():(out/'RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
def run(cmd,label,timeout=90):
 began=time.monotonic_ns();r=subprocess.run(list(map(str,cmd)),cwd=ROOT,env=env,capture_output=True,text=True,timeout=timeout);(out/(label+'.log')).write_text(r.stdout+r.stderr);receipt['records'].append({'label':label,'exitCode':r.returncode,'durationNs':time.monotonic_ns()-began});save()
 if r.returncode:raise RuntimeError(label+' failed: '+(r.stdout+r.stderr)[-5000:])
 return r.stdout
save()
with tempfile.TemporaryDirectory(prefix='r31-keep-native-') as temp:
 w=pathlib.Path(temp);platform=w/'platform';production=w/'production';test=w/'test';generated=w/'generated';fixture=w/'fixture'
 for d in [platform,production,test,generated,fixture]:d.mkdir()
 shims=[]
 for origin in sorted((ROOT/'tools/r29/qual_host/shim').rglob('*.java')):
  text=origin.read_text();target=generated/origin.relative_to(ROOT/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True)
  if origin.name=='SQLiteDatabase.java':
   text=text.replace('public class SQLiteDatabase {','public class SQLiteDatabase implements AutoCloseable {')
   at=text.index('    static final class Rows')
   text=text[:at]+'''    public static final int OPEN_READONLY=1;public interface CursorFactory{}
    public static SQLiteDatabase openDatabase(String path,CursorFactory ignored,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path));}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
''' +text[at:]
  target.write_text(text);shims.append(target)
 tree=ast.parse((ROOT/'tools/r31/run_report_cas_scope_host.py').read_text());extras=next(ast.literal_eval(n.value) for n in ast.walk(tree) if isinstance(n,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='extras' for t in n.targets))
 for name,text in extras.items():target=generated/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)
 run(['python3',ROOT/'tools/r31/verify_keep_reader_inventory.py','--root',ROOT],'executable-legacy-keep-reader-inventory-not-runtime-proof')
 receipt['hostPlatformSourceSha256']={str(p.relative_to(generated)):sha(p) for p in shims};save()
 run(['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[a.classes,sdk,jdbc])),'-d',platform,*shims],'compile-native-platform')
 run(['javac','-encoding','UTF-8','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[a.classes,sdk])),'-d',production,*sources],'compile-actual-current-keep-and-consumers')
 runtime=os.pathsep.join(map(str,[platform,production,test,a.classes,jdbc,sdk]));base=['java','-Xmx512m','-cp',runtime,'com.ethk3.novaonepro.strategyv2.KeepStoreR31Host']
 run(['javac','-encoding','UTF-8','-cp',runtime,'-d',test,host],'compile-host')
 run([*base,fixture,'init'],'actual-producer-fixture')
 db=fixture/'databases/keep.db';seen=set();i=0
 def checkpoint():
  with sqlite3.connect(db) as c:
   row=c.execute('SELECT revision,state_json FROM r31_keep_checkpoint').fetchone()
   return None if row is None else (row[0],json.loads(row[1]))
 while i<100:
  state=checkpoint();stage='VERIFY_HASH' if state is None else state[1]['stage']
  if stage not in seen:
   seen.add(stage)
   for point,marker in [('before','KEEP_NATIVE_BEFORE_COMMIT'),('after','KEEP_NATIVE_AFTER_COMMIT')]:
    before=checkpoint();log=out/('kill-'+stage+'-'+point+'.log')
    with log.open('w') as output:
     child=subprocess.Popen(list(map(str,[*base,fixture,'one',point])),cwd=ROOT,env=env,stdout=output,stderr=subprocess.STDOUT,start_new_session=True)
     deadline=time.monotonic()+30
     while time.monotonic()<deadline:
      if marker in log.read_text():break
      if child.poll() is not None:raise RuntimeError('kill handshake failed '+log.read_text())
      time.sleep(.02)
     else:child.kill();child.wait();raise RuntimeError('kill handshake timed out')
     os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=10)
    after=checkpoint();assert child.returncode==-signal.SIGKILL
    if point=='before':assert before==after,'Uncommitted migration changed checkpoint'
    else:assert after[0]==(0 if before is None else before[0]+1),'Committed checkpoint not recovered'
    receipt['killReopens'].append({'stage':stage,'point':point,'actualSignal':'SIGKILL','exitCode':child.returncode,'checkpointContract':'PASS'});save()
   if stage=='SWITCH':break
  run([*base,fixture,'one'],'actual-migration-unit-%03d'%i);i+=1
 assert seen=={'VERIFY_HASH','REGISTER_REFERENCE','MIGRATE','SWITCH'}
 run([*base,fixture,'append'],'actual-new-producer-append-after-sealed-high-water')
 run([*base,fixture,'one'],'actual-next-range-start')
 for unit in range(20):
  state=checkpoint()
  if state and state[1]['stage']=='SWITCH':break
  run([*base,fixture,'one'],'actual-next-range-unit-%02d'%unit)
 else:raise AssertionError('Appended originals did not migrate the next bounded range')
 run([*base,fixture,'verify'],'actual-full-original-cas-consumers-negatives')
 mismatch=w/'mismatch-fixture';mismatch.mkdir();run([*base,mismatch,'mismatch-init'],'actual-mismatch-p05-fallback-fixture')
 mismatch_db=mismatch/'databases/keep.db'
 def mismatch_state():
  with sqlite3.connect(mismatch_db) as c:
   row=c.execute('SELECT revision,state_json FROM r31_keep_checkpoint').fetchone();return None if row is None else (row[0],json.loads(row[1]))
 for unit in range(20):
  current=mismatch_state()
  if current and current[1]['stage']=='SWITCH':break
  run([*base,mismatch,'mismatch-one'],'actual-mismatch-migration-%02d'%unit)
 else:raise AssertionError('Actual mismatch migration did not reach guarded switch')
 for operation in ['mismatch-cutover','mismatch-cutover','mismatch-retire','mismatch-produce']:
  for point,marker in [('before','KEEP_NATIVE_BEFORE_COMMIT'),('after','KEEP_NATIVE_AFTER_COMMIT')]:
   def logical_snapshot():
    with sqlite3.connect(mismatch_db) as c:
     tables=['r30_decision_mismatch','r30_keep_r30_decision_mismatch','r31_keep_original','r31_keep_checkpoint','r31_keep_cutover','r31_keep_mismatch_original','r31_keep_mismatch_permit','r31_keep_retire_permit','r30_retention_archive_index']
     rows={'schema':c.execute("SELECT name,sql FROM sqlite_master WHERE type='trigger' ORDER BY name").fetchall()}
     for table in tables:rows[table]=c.execute('SELECT * FROM '+table+' ORDER BY 1').fetchall()
     return hashlib.sha256(repr(rows).encode()).hexdigest()
   before_snapshot=logical_snapshot()
   log=out/('reference-'+operation+'-'+point+'-'+str(len(receipt['killReopens']))+'.log')
   with log.open('w') as output:
    child=subprocess.Popen(list(map(str,[*base,mismatch,operation,point])),cwd=ROOT,env=env,stdout=output,stderr=subprocess.STDOUT,start_new_session=True);deadline=time.monotonic()+30
    while time.monotonic()<deadline:
     if marker in log.read_text():break
     if child.poll() is not None:raise RuntimeError('reference kill handshake failed '+log.read_text())
     time.sleep(.02)
    else:child.kill();child.wait();raise RuntimeError('reference kill handshake timed out')
    os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=10)
   assert child.returncode==-signal.SIGKILL
   with sqlite3.connect(mismatch_db) as c:assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
   after_snapshot=logical_snapshot()
   assert (before_snapshot==after_snapshot) if point=='before' else (before_snapshot!=after_snapshot),'actual cutover/permit transaction changed its logical atomicity'
   receipt['killReopens'].append({'stage':operation,'point':point,'actualSignal':'SIGKILL','exitCode':child.returncode,'checkpointContract':'NATIVE_SQLITE_REOPEN_ATOMIC_STATE_PASS','beforeLogicalSha256':before_snapshot,'afterLogicalSha256':after_snapshot});save()
 run([*base,mismatch,'mismatch-verify'],'actual-reference-only-producer-cutover-readers-negatives')
 receipt['singleTableCutover']='r30_decision_mismatch:ACTUAL_REFERENCE_ONLY_NATIVE_EXECUTION;OTHER_TABLES_WAIT_NOT_CLOSED'
 generic=w/'generic-fixture';generic.mkdir();run([*base,generic,'generic-init'],'actual-generic-real-source-schema-fixture');generic_db=generic/'databases/keep.db'
 for source in ['r26_candidate','r26_gap_receipt','r27_live_frozen']:
  for unit in range(20):
   with sqlite3.connect(generic_db) as c:current=c.execute('SELECT state_json FROM r31_keep_checkpoint WHERE archive_table=?',('r30_keep_'+source,)).fetchone()
   if current and json.loads(current[0])['stage']=='SWITCH':break
   run([*base,generic,'generic-one',source],'actual-generic-capture-'+source+'-'+str(unit))
  else:raise AssertionError('actual generic source failed capture')
 for unit,operation in enumerate(['generic-cutover']*6+['generic-retire']*3):
  for point,marker in [('before','KEEP_NATIVE_BEFORE_COMMIT'),('after','KEEP_NATIVE_AFTER_COMMIT')]:
   with sqlite3.connect(generic_db) as c:before='\n'.join(c.iterdump())
   log=out/('generic-kill-'+str(unit)+'-'+point+'.log')
   with log.open('w') as output:
    child=subprocess.Popen(list(map(str,[*base,generic,operation,point])),cwd=ROOT,env=env,stdout=output,stderr=subprocess.STDOUT,start_new_session=True);deadline=time.monotonic()+30
    while time.monotonic()<deadline:
     if marker in log.read_text():break
     if child.poll() is not None:raise RuntimeError('generic kill handshake failed '+log.read_text())
     time.sleep(.02)
    else:child.kill();child.wait();raise RuntimeError('generic kill handshake timed out')
    os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=10)
   with sqlite3.connect(generic_db) as c:after='\n'.join(c.iterdump());assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
   assert child.returncode==-signal.SIGKILL and (before==after if point=='before' else before!=after)
   receipt['killReopens'].append({'stage':operation,'point':point,'actualSignal':'SIGKILL','exitCode':child.returncode,'checkpointContract':'GENERIC_NATIVE_ATOMICITY_PASS'});save()
 run([*base,generic,'generic-append'],'actual-generic-legacy-copy-still-labelled-copy')
 run([*base,generic,'generic-one','r26_gap_receipt'],'actual-generic-appended-range-start')
 for unit in range(20):
  with sqlite3.connect(generic_db) as c:current=json.loads(c.execute('SELECT state_json FROM r31_keep_checkpoint WHERE archive_table=?',('r30_keep_r26_gap_receipt',)).fetchone()[0])
  if current['stage']=='SWITCH':break
  run([*base,generic,'generic-one','r26_gap_receipt'],'actual-generic-appended-range-'+str(unit))
 run([*base,generic,'generic-retire'],'actual-generic-new-copy-migrated-and-retired')
 run([*base,generic,'generic-verify'],'actual-generic-complete-readers-and-live-append-verification')
 receipt['genericDuplicateRetirement']='ACTUAL_THREE_REAL_SCHEMA_SOURCES;P05_TYPED_FALLBACK;NO_REFERENCE_ONLY_OR_PHYSICAL_BYTE_RELEASE_CLAIM'
 run([*base,generic,'gap-init'],'actual-gap-reference-source-cutover-and-original-retention-selection')
 seen_gap=set();gap_steps=0
 while gap_steps<40:
  with sqlite3.connect(generic_db) as c:
   checkpoint=json.loads(c.execute('SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?',('r30-retention-control','maintenance-phase')).fetchone()[0]);batch=c.execute('SELECT state,prepared_rows,applied_rows FROM r31_gap_reference_batch').fetchone()
  if checkpoint['phase']==21:break
  stage='PLAN' if batch is None else batch[0]
  if stage not in seen_gap:
   seen_gap.add(stage)
   for point,marker in [('before','KEEP_NATIVE_BEFORE_COMMIT'),('after','KEEP_NATIVE_AFTER_COMMIT')]:
    with sqlite3.connect(generic_db) as c:before='\n'.join(c.iterdump())
    log=out/('gap-kill-'+stage+'-'+point+'.log')
    with log.open('w') as output:
     child=subprocess.Popen(list(map(str,[*base,generic,'gap-step',point])),cwd=ROOT,env=env,stdout=output,stderr=subprocess.STDOUT,start_new_session=True);deadline=time.monotonic()+30
     while time.monotonic()<deadline:
      if marker in log.read_text():break
      if child.poll() is not None:raise RuntimeError('gap kill handshake failed '+log.read_text())
      time.sleep(.02)
     else:child.kill();child.wait();raise RuntimeError('gap kill handshake timed out')
     os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=10)
    with sqlite3.connect(generic_db) as c:after='\n'.join(c.iterdump());assert c.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
    assert child.returncode==-signal.SIGKILL and (before==after if point=='before' else before!=after)
    receipt['killReopens'].append({'stage':'GAP_'+stage,'point':point,'actualSignal':'SIGKILL','exitCode':child.returncode,'checkpointContract':'SOURCE_CAS_REFERENCE_BATCH_AND_RETENTION_PHASE_ATOMICITY_PASS'});save()
  run([*base,generic,'gap-step'],'actual-gap-reference-producer-step-%02d'%gap_steps);gap_steps+=1
 else:raise AssertionError('Gap producer did not advance the original retention phase')
 run([*base,generic,'gap-verify'],'actual-gap-reference-production-engine-tail-batch-readback-and-permit')
 receipt['gapReferenceProducer']='ACTUAL_KEEP_2048_SELECT_256_APPLY_AT_MOST_64_PER_WRITER;DURABLE_CAS_PREPARATION;SECOND_DECLARED_REFERENCE_SOURCE;NO_NATURAL_OR_DEVICE_CLAIM'
 receipt['compiledProductSha256']={str(s.relative_to(production)):sha(s) for s in sorted(production.rglob('*.class'))}
 receipt['sourceAndSupportUnchanged']=sha(pathlib.Path(__file__))==receipt['runnerSha256'] and sha(host)==receipt['hostSha256'] and all(sha(ROOT/name)==value for name,value in receipt['sourceSha256'].items()) and sha(a.classes)==receipt['compiledSupportSha256'];assert receipt['sourceAndSupportUnchanged']
 receipt['executedSet']=['actual legacy producer archive','keyset capture and second verify pass','durable reference and migration state','real SIGKILL before and after each stage commit','independent native critical commits between units','full CAS BLOB/text/integer/real/null identity readers','corrupt CAS refusal','unknown immutable guard refusal','read-only report','default-deny undeclared table cutover','actual one-table P05 typed fallback cutover','actual old keep shrink retaining CAS originals','new writes use exact CAS permit without old keep copy','5000-period tail remains active','INSERT OR REPLACE bypass refused with recursive triggers off','actual generic old keep duplicate retirement on three actual source schemas','actual Warm archive contract after guard migration and CAS-aware high water','actual QualificationGapRepair production entry restores original CAS prediction without alpha reset','live append remains explicitly legacy copy then CAS migration and duplicate retirement','SQL unknown projection/DQS refusal']
receipt['status']='PASS_NATIVE_STORE_READERS_MISMATCH_AND_GAP_REFERENCE_AND_DECLARED_DUPLICATE_RETIREMENT;OTHER_SOURCE_REFERENCE_PRODUCERS_NOT_CLOSED';receipt['finishedAt']=datetime.datetime.now(datetime.timezone.utc).isoformat();save();print('R31_KEEP_NATIVE_STORE_AND_READERS_PASS realKillReopens='+str(len(receipt['killReopens'])))
