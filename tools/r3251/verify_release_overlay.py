#!/usr/bin/env python3
"""Observe the original 032 -> 033 release overlay on a fresh disposable emulator.

The old APK creates its own database. It is never edited by this probe. A separate
private SQLite file provides a nonempty installer-preservation witness. Raw app
databases remain in private scratch, outside the uploaded public receipt folder.
"""
import argparse,datetime,hashlib,json,os,re,sqlite3,subprocess,tempfile,time,uuid,zipfile
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
PKG='com.starcore.research.r13'
DB='binance_k3_nova_one_pro_v57.db'
CERT='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
OLD_SHA='876f26e81114acfa3d2fa2d023b2e36309bec6dde3ef32f0e87c562b48d165ac'

def digest(data):return hashlib.sha256(data).hexdigest()
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def redact(text):
 text=re.sub(r'(?i)Bearer\s+[^\s,;\"\'}]+','Bearer [REDACTED]',text)
 return re.sub(r'(?i)((?:api[_-]?key|authorization|password|token|secret)[\"\']?\s*[=:]\s*[\"\']?)[^\s,;\"\'}]+',r'\1[REDACTED]',text)
def command(argv,timeout=180,required=True):
 result=subprocess.run(list(map(str,argv)),capture_output=True,timeout=timeout)
 if required and result.returncode:
  raise RuntimeError('COMMAND_FAILED:'+str(argv[:5])+':'+redact((result.stdout+result.stderr).decode(errors='replace'))[-1800:])
 return result
def adb(*args,**kwargs):return command(['adb',*args],**kwargs)
def text(*args,**kwargs):return adb(*args,**kwargs).stdout.decode(errors='replace').strip()
def install_succeeded(output):return bool(output.splitlines()) and output.splitlines()[-1]=='Success' and 'Failure [' not in output
def application_data_uid():
 value=text('shell','stat','-c','%u','/data/user/0/'+PKG)
 assert value.isdigit() and int(value)>=10000,'APPLICATION_DATA_DIRECTORY_UID_NOT_OBSERVED'
 return value

def snapshot(folder):
 text('shell','am','force-stop',PKG)
 process=text('shell','ps','-A','-o','PID,ARGS')
 assert not any(len(row.split())>=2 and (row.split()[1]==PKG or row.split()[1].startswith(PKG+':')) for row in process.splitlines()),'PACKAGE_PROCESS_STILL_ALIVE_DURING_COPY'
 folder.mkdir();receipt={}
 for suffix in ('','-wal','-shm'):
  remote='/data/user/0/'+PKG+'/databases/'+DB+suffix
  present=adb('shell','test','-f',remote,required=False).returncode==0
  if not present:
   receipt[DB+suffix]=dict(present=False);continue
  before=text('shell','sha256sum',remote).split()[0]
  data=adb('exec-out','cat',remote,timeout=300).stdout
  after=text('shell','sha256sum',remote).split()[0]
  assert before==digest(data)==after,'DATABASE_CAPTURE_GENERATION_CHANGED'
  (folder/(DB+suffix)).write_bytes(data)
  receipt[DB+suffix]=dict(present=True,bytes=len(data),sha256=before,copyAndPostHashMatch=True)
 assert receipt[DB]['present'],'ACTUAL_APPLICATION_DATABASE_MISSING'
 return receipt

def inspect(path):
 with sqlite3.connect(path) as db:
  assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok','DATABASE_INTEGRITY_FAILED'
  tables={row[0] for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
  assert 'app_state' in tables,'APPLICATION_STATE_NOT_READY'
  switch=db.execute('SELECT state_value FROM app_state WHERE state_key=?',('su2_real_authorized_by_user',)).fetchone()
  assert switch is None or switch[0]!='1','REAL_ACTION_WAS_ENABLED'
  stamps=dict(db.execute('SELECT domain,state FROM v57_schema_domain_r926')) if 'v57_schema_domain_r926' in tables else {}
  protected={}
  for table in ('draws','trades'):
   if table not in tables:continue
   columns={row[1] for row in db.execute('PRAGMA table_info('+table+')')}
   selected=([c for c in ['issue',*[f'n{i}' for i in range(1,11)]] if c in columns] if table=='draws' else [c for c in ['_id','target_issue','action','position','n1','n2','n3','stake'] if c in columns])
   assert selected,'PROTECTED_TABLE_WITHOUT_RECOGNIZABLE_COLUMNS'
   rows=db.execute('SELECT '+','.join('"'+c+'"' for c in selected)+' FROM "'+table+'"').fetchall()
   protected[table]=dict(columns=selected,rows={digest(repr(row).encode()) for row in rows},count=len(rows))
  return dict(integrity='ok',userVersion=db.execute('PRAGMA user_version').fetchone()[0],criticalStamp=stamps.get('CRITICAL'),realActionEnabled=False),protected

def main():
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument('--old-apk',required=True,type=Path);parser.add_argument('--new-apk',required=True,type=Path);parser.add_argument('--out',required=True,type=Path)
 parser.add_argument('--expected-source');parser.add_argument('--expected-apk-sha256')
 args=parser.parse_args();out=args.out.resolve();out.mkdir(parents=True,exist_ok=False)
 validation_source=command(['git','rev-parse','HEAD']).stdout.decode().strip()
 receipt=dict(scope='ACTUAL_ORIGINAL_032_TO_033_RELEASE_EMULATOR_OVERLAY',startedAt=now(),validationSourceCommit=validation_source,state='RUNNING',physicalUserDatabase='NOT_AVAILABLE_NOT_CLAIMED',physicalDevice='NOT_RUN',physical24h='NOT_RUN',physical72h='NOT_RUN',naturalQuality='NOT_PROVEN')
 def save():(out/'RELEASE_OVERLAY.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
 save()
 try:
  assert os.environ.get('R3251_DISPOSABLE_EMULATOR')=='1','EXPLICIT_DISPOSABLE_EMULATOR_REQUIRED'
  command(['bash',ROOT/'tools/r30-repair/prepare_emulator.sh'])
  assert text('shell','getprop','ro.kernel.qemu')=='1'
  assert text('shell','id','-u')=='0'
  assert text('get-serialno').startswith('emulator-')
  assert adb('shell','pm','path',PKG,required=False).returncode!=0 or not text('shell','pm','path',PKG,required=False),'FRESH_EMULATOR_REQUIRED'
  receipt.update(api=int(text('shell','getprop','ro.build.version.sdk')),serial=text('get-serialno'),oldApkSha256=digest(args.old_apk.read_bytes()),newApkSha256=digest(args.new_apk.read_bytes()))
  assert receipt['oldApkSha256']==OLD_SHA,'ORIGINAL_032_APK_MISMATCH'
  if args.expected_apk_sha256:assert receipt['newApkSha256']==args.expected_apk_sha256,'EXACT_SIGNED_033_APK_MISMATCH'
  with zipfile.ZipFile(args.new_apk) as archive:embedded=json.loads(archive.read('assets/diagnostics/build-identity-r31.json'))
  expected_source=args.expected_source or validation_source
  assert re.fullmatch(r'[0-9a-f]{40}',expected_source) and embedded['source_commit']==expected_source and embedded['source_dirty'] is False,'EXACT_CLEAN_BUILD_SOURCE_MISMATCH'
  receipt.update(sourceCommit=embedded['source_commit'],sourceTree=embedded['source_tree'],embeddedSourceDirty=False)
  sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0';identities=[]
  for label,path in [('old',args.old_apk),('new',args.new_apk)]:
   cert=command([sdk/'apksigner','verify','--verbose','--print-certs',path]).stdout.decode()
   assert 'Signer #1 certificate SHA-256 digest: '+CERT in cert,'SIGNATURE_CONTINUITY_FAILED'
   badging=command([sdk/'aapt','dump','badging',path]).stdout.decode()
   match=re.search(r"^package: name='([^']+)' versionCode='([0-9]+)'",badging,re.M);assert match and match[1]==PKG
   identities.append(int(match[2]));(out/(label+'-certificate.txt')).write_text(cert)
  assert identities==[577311032,577311033],'EXPECTED_REAL_032_TO_033_UPGRADE'
  receipt.update(certificateSha256=CERT,oldVersionCode=identities[0],newVersionCode=identities[1],samePackage=True,sameSignature=True,higherVersionCode=True)
  install=text('install','-r',args.old_apk,timeout=600);assert install_succeeded(install),install
  (out/'install-original032.txt').write_text(install+'\n')
  if receipt['api']>=33:adb('shell','pm','grant',PKG,'android.permission.POST_NOTIFICATIONS')
  start=text('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity');assert 'Error:' not in start
  for _ in range(40):
   if adb('shell','test','-f','/data/user/0/'+PKG+'/databases/'+DB,required=False).returncode==0:break
   time.sleep(1)
  time.sleep(20)
  with tempfile.TemporaryDirectory(prefix='r3251-release-overlay-private-',dir=os.environ.get('RUNNER_TEMP')) as temporary:
   private=Path(temporary);receipt['oldDatabaseCapture']=snapshot(private/'old')
   old_facts,old_rows=inspect(private/'old'/DB);receipt['oldDatabase']=old_facts
   receipt['oldObservedBusinessRows']={name:value['count'] for name,value in old_rows.items()}
   marker=private/'overlay-probe.sqlite';nonce=uuid.uuid4().hex
   with sqlite3.connect(marker) as witness:
    witness.execute('CREATE TABLE overlay_witness(id INTEGER PRIMARY KEY,nonce TEXT NOT NULL)');witness.execute('INSERT INTO overlay_witness VALUES(1,?)',(nonce,))
   marker_sha=digest(marker.read_bytes());remote_marker='/data/user/0/'+PKG+'/files/r3251-overlay-probe.sqlite'
   text('shell','mkdir','-p','/data/user/0/'+PKG+'/files');text('push',marker,remote_marker)
   old_uid=application_data_uid();receipt['oldApplicationDataUid']=int(old_uid)
   text('shell','chown',old_uid+':'+old_uid,remote_marker)
   install=text('install','-r',args.new_apk,timeout=600);assert install_succeeded(install),install
   (out/'install-release033.txt').write_text(install+'\n')
   assert digest(adb('exec-out','cat',remote_marker).stdout)==marker_sha,'PRIVATE_SQLITE_WITNESS_LOST_DURING_UPGRADE'
   installed_paths=text('shell','pm','path',PKG).splitlines();assert len(installed_paths)==1
   assert digest(adb('exec-out','cat',installed_paths[0].removeprefix('package:')).stdout)==receipt['newApkSha256'],'DELIVERED_RELEASE_NOT_ACTUALLY_INSTALLED'
   new_uid=application_data_uid();assert new_uid==old_uid,'APPLICATION_UID_CHANGED'
   receipt['newApplicationDataUid']=int(new_uid)
   start=text('shell','am','start','-W','-n',PKG+'/com.ethk3.novaonepro.MainActivity');assert 'Error:' not in start
   time.sleep(35)
   assert text('shell','pidof',PKG),'UPGRADED_APPLICATION_NOT_ALIVE'
   crash=text('logcat','-d','-b','crash','-v','threadtime');(out/'crash-redacted.txt').write_text(redact(crash));assert PKG not in crash,'PACKAGE_CRASH_OBSERVED'
   receipt['newDatabaseCapture']=snapshot(private/'new');new_facts,new_rows=inspect(private/'new'/DB);receipt['newDatabase']=new_facts
   assert new_facts['criticalStamp']=='READY','UPGRADED_CRITICAL_SCHEMA_NOT_READY'
   comparisons=[]
   for name,value in old_rows.items():
    current=new_rows.get(name);assert current and current['columns']==value['columns'],'PROTECTED_TABLE_OR_COLUMNS_LOST'
    preserved=len(value['rows'] & current['rows']);assert preserved==len(value['rows']),'OBSERVED_OLD_BUSINESS_ROWS_LOST'
    comparisons.append(dict(table=name,oldObservedRows=value['count'],newObservedRows=current['count'],preservedOldRowHashes=preserved,columns=value['columns'],zeroRowsAreNotNonemptyBusinessEvidence=value['count']==0))
   assert digest(adb('exec-out','cat',remote_marker).stdout)==marker_sha
   install=text('install','-r',args.new_apk,timeout=600);assert install_succeeded(install),install
   assert digest(adb('exec-out','cat',remote_marker).stdout)==marker_sha,'SAME_RELEASE_REINSTALL_LOST_PRIVATE_SQLITE'
   receipt['reinstallDatabaseCapture']=snapshot(private/'reinstalled');re_facts,re_rows=inspect(private/'reinstalled'/DB)
   assert all(name in re_rows and value['rows']<=re_rows[name]['rows'] for name,value in new_rows.items()),'SAME_RELEASE_REINSTALL_LOST_OBSERVED_ROWS'
   receipt.update(state='PASS_OBSERVED_RELEASE_OVERLAY',actualReleaseOverlayInstall='PASS',sameReleaseReinstall='PASS',original032DatabaseGeneratedByActualOldApk=True,originalOldDatabaseWasModifiedByProbe=False,originalUserDatabaseIncluded=False,privateSqliteWitnessPreserved=True,privateSqliteWitnessSha256=marker_sha,applicationUidPreserved=True,observedBusinessRows=comparisons,realActionEnabled=False,installedReleaseBytesMatch=True,rawApplicationDataPublished=False)
 except BaseException as error:
  receipt.update(state='FAIL',failure=redact(str(error))[:2400]);raise
 finally:
  receipt['finishedAt']=now();save();print(json.dumps(receipt,ensure_ascii=False),flush=True)

if __name__=='__main__':main()
