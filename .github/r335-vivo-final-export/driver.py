"""Receive the actual single-volume or multipart package and reopen bytes saved by the Android document picker."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import sqlite3
import subprocess
import sys
import time
import zipfile
from urllib.parse import urlparse,unquote

workspace=Path(os.environ['GITHUB_WORKSPACE'])
sys.path.insert(0,str(workspace/'.github/r335-field-metrics'))
import field
import observe

source='43fef2458c33e7c6140d9822ae91ad1bf4cc6529'
apk=None
identity=re.compile('[0-9a-f]{64}')
expected_formats={'SCREPORT_SINGLE_ZIP_R992','SCREPORT_ZIP_PARTS_R30_1'}
save_label='一次保存完整报告到文件夹'
single_label='一次保存完整ZIP文件'
private=field.TEMP/'separate-private-native-readback';private.mkdir(exist_ok=True)
def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);return mod
sys.argv=[str(__file__),'--out',str(private)]
ui=load('actual_phone_picker',workspace/'.github/r335-field-metrics/phone_full_backup.py')
oracle=load('original_export_oracle',field.TEMP/'material/STARCORE_R33_5_20261118/B01/scripts/verify_export_artifacts_r334.py')
def check(value,code):
    if not value:raise ValueError(code)
def read(file):return json.loads(file.read_bytes())
def digest(data):return hashlib.sha256(data).hexdigest()
def labels():return {x.get('text','') for x in ui.hierarchy().iter('node')}
def tap(predicate,limit=22):
    end=time.monotonic()+limit
    while time.monotonic()<end:
        if ui.click_node(predicate):return True
        time.sleep(.6)
    return False
def report_dialog(label):
    get='取回上次报告或继续生成'
    for attempt in range(4):
        if label in labels():return
        if get in labels():
            ui.click_text(get)
            if tap_probe(label,25):return
        width,height=map(int,re.findall(r'(\d+)x(\d+)',ui.adb('shell','wm','size'))[-1])
        for _ in range(3):ui.adb('shell','input','swipe',str(width//2),str(height//3),str(width//2),str(height*4//5),'230')
        ui.click_text('导出完整报告（含数据库和训练附件）',scrolls=42)
    raise ValueError('CURRENT_JOB_SHARE_DIALOG_UNAVAILABLE')
def tap_probe(label,limit):
    until=time.monotonic()+limit
    while time.monotonic()<until:
        if label in labels():return True
        time.sleep(.5)
    return False
def pull(remote,target):
    p=subprocess.run(['adb','pull',remote,str(target)],capture_output=True,timeout=360)
    check(p.returncode==0 and target.is_file(),'NATIVE_MEMBER_PULL_FAILED')
def assemble(index,directory,volume_dir,out):
    single=index['format']=='SCREPORT_SINGLE_ZIP_R992'
    members=index['files']
    check(1<=len(members)<=128 and index['expectedVolumes']==index['exportedVolumes']==len(members) and index['omittedVolumes']==0,'VOLUME_COUNT')
    check(single==(len(members)==1) and len({m['name'] for m in members})==len(members),'DUPLICATE_OR_FORMAT')
    offset=0;volume_chks=[];whole=hashlib.sha256()
    with out.open('wb') as stream:
        for i,m in enumerate(members,1):
            name='STARCORE_FULL_REPORT.zip' if single else 'STARCORE_FULL_REPORT.zip.part'+format(i,'02d')
            check(m['name']==name and m['path']=='delivery/'+name and m['kind']==('SINGLE_ZIP' if single else 'ZIP_PART'),'NATIVE_INDEX_PATH')
            check(type(m['bytes'])==int and m['bytes']>0 and identity.fullmatch(m['sha256']),'NATIVE_MEMBER_IDENTITY')
            if not single:
                check(m['partNumber']==i and m['offset']==offset and m['bytes']<=20*1024**2,'NATIVE_PART_ORDER')
                if i<len(members):check(m['bytes']==20*1024**2,'NATIVE_PART_SIZE')
            file=volume_dir/name;pull(directory+'/'+name,file)
            check(file.stat().st_size==m['bytes'] and field.fingerprint(file)==m['sha256'],'NATIVE_PART_READBACK')
            with file.open('rb') as src:
                while True:
                    chunk=src.read(1048576)
                    if not chunk:break
                    stream.write(chunk);whole.update(chunk)
            volume_chks.append(m['sha256']);offset+=m['bytes']
    expected_sha=members[0]['sha256'] if single else index['zipSha256']
    expected_size=members[0]['bytes'] if single else index['zipBytes']
    check(offset==expected_size and whole.hexdigest()==expected_sha and field.fingerprint(out)==expected_sha,'REASSEMBLED_ZIP_IDENTITY')
    return volume_chks
def select_tree(folder):
    ui.click_text(save_label)
    choose=lambda n:n.get('resource-id','').endswith('/action_menu_select') or n.get('text','').lower() in ['use this folder','使用此文件夹','使用这个文件夹','选择此文件夹']
    def current():
        for node in ui.hierarchy().iter('node'):
            res=node.get('resource-id','')
            if node.get('text')==folder and ('breadcrumb' in res or 'action_bar' in res or 'toolbar' in res or res.endswith('/title')):
                return True
        return False
    if not current():
        if folder not in labels():
            if tap(lambda n:n.get('content-desc','').lower() in ['show roots','open navigation drawer','显示根目录','打开导航抽屉'],6):
                check(tap(lambda n:n.get('text','') in ['Downloads','下载','Download']),'SAF_DOWNLOADS_ROOT')
        if not current():
            if not tap(lambda n:n.get('text','')==folder,8):
                create=lambda n: ('create_dir' in n.get('resource-id','') or n.get('content-desc','').lower() in ['new folder','create folder','新建文件夹','创建文件夹'])
                check(tap(create,6),'SAF_DESTINATION_FOLDER')
                field.adb('shell','input','text',folder)
                check(tap(lambda n:n.get('resource-id','')=='android:id/button1' or n.get('text','').lower() in ['create','创建','确定'],10),'SAF_DESTINATION_FOLDER')
    check(tap(lambda n:n.get('resource-id','').endswith('/action_menu_select') or n.get('text','').lower() in ['use this folder','使用此文件夹','使用这个文件夹','选择此文件夹']),'SAF_USE_FOLDER_BUTTON')
    tap(lambda n:n.get('text','').lower() in ['allow','允许'] or n.get('resource-id')=='android:id/button1',15)
def training_objects(archive,snapshot):
    expected={}
    with sqlite3.connect(snapshot.as_uri()+'?mode=ro&immutable=1',uri=True) as db:
        tables={name for name, in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        for table,column,prefix,suffix in [('research_training_input_v2','input_hash','training-input-','.scdev'),('research_training_attachment_cp12','attachment_hash','training-bound-','.scbind')]:
            if table not in tables:continue
            for sha,rel,size in db.execute(f'SELECT {column},relative_path,byte_count FROM {table}'):
                check(re.fullmatch('[0-9a-f]{64}',sha) and rel==sha+suffix and 0<size<=8*1024**2,'TRAINING_ROW_IDENTITY')
                name=prefix+rel
                check(name not in expected,'TRAINING_ROW_IDENTITY')
                expected[name]=(sha,size)
    rows=[json.loads(line) for line in archive.read('attachments.jsonl').splitlines() if line.strip()]
    observed={}
    for row in rows:
        if row.get('sourceKind')!='TRAINING':continue
        name=row['logicalName']
        check(name not in observed and name in expected and row.get('state')=='FIXED_VERIFIED','TRAINING_DESCRIPTOR')
        with archive.open(name) as body:sha=hashlib.file_digest(body,'sha256').hexdigest()
        observed[name]=(sha,archive.getinfo(name).file_size)
        check(observed[name]==(row['sha256'],row['bytes']),'TRAINING_BODY_IDENTITY')
    check(expected==observed,'TRAINING_DB_CLOSURE')
    bound=digest(''.join(name+' '+sha+' '+str(size)+chr(10) for name,(sha,size) in sorted(observed.items())).encode())
    return dict(trainingRowsReopened=True,trainingMembers=len(observed),trainingBytes=sum(v[1] for v in observed.values()),trainingSetSha256=bound)
def extract_full_raw_closure(joined,job):
    # FULL_RAW exports the actual reference graph and bodies. The compact audit is a separate profile.
    from pathlib import PurePosixPath
    area=private/('closure-'+job);area.mkdir();bodies=area/'bodies';bodies.mkdir()
    with zipfile.ZipFile(joined) as z:
        mf=json.loads(z.read('source-manifest.json'))
        check(mf.get('rawEvidenceIncluded') is True and mf.get('r26CheckpointBodiesIncluded') is True and mf.get('r26CheckpointByteValidation')=='EACH_INCLUDED_BODY_SHA256_VERIFIED','FULL_RAW_MODE')
        hashes={}
        for name in z.namelist():
            match=re.fullmatch('r26-checkpoint-([0-9a-f]{64})[.]bin',PurePosixPath(name).name)
            if not match:continue
            h=match[1];check(h not in hashes,'DUPLICATE_RAW_BODY')
            body=bodies/(h+'.bin')
            with z.open(name) as src,body.open('wb') as out:shutil.copyfileobj(src,out)
            size=body.stat().st_size;check(size>0 and field.fingerprint(body)==h,'RAW_BODY_IDENTITY')
            hashes[h]=size
        n=mf['r26CheckpointObjects'];total=mf['r26CheckpointBytes']
        check(type(n)==int and type(total)==int and n==len(hashes) and total==sum(hashes.values()),'RAW_INVENTORY')
        db=area/'snapshot.db'
        with z.open('snapshot.db') as src,db.open('wb') as out:shutil.copyfileobj(src,out)
        check(field.fingerprint(db)==mf['snapshotSha256'] and db.stat().st_size==mf['snapshotBytes'],'ORIGINAL_DB_BYTES')
        training=training_objects(z,db)
    expected=digest(''.join(h+' '+str(hashes[h])+chr(10) for h in sorted(hashes)).encode())
    remote='/data/local/tmp/r335-raw-closure-'+job
    field.adb('shell','mkdir',remote)
    field.adb('push',str(db),remote+'/snapshot.db',timeout=320)
    field.adb('push',str(bodies),remote+'/bodies',timeout=320)
    dex=field.TEMP/'raw-closure-dex.jar';field.adb('push',str(dex),remote+'/test.jar');field.adb('shell','chmod','444',remote+'/test.jar')
    target=next(x[8:] for x in field.adb('shell','pm','path',field.PKG).decode().splitlines() if x.startswith('package:') and x.endswith('base.apk'))
    check(field.adb('shell','sha256sum',target).decode().split()[0]==apk and field.adb('shell','sha256sum',remote+'/test.jar').decode().split()[0]==field.fingerprint(dex),'ORIGINAL_APK')
    race=subprocess.run(['adb','shell','env','CLASSPATH='+target+':'+remote+'/test.jar','app_process','-Xms24m','-Xmx256m','/system/bin','com.ethk3.novaonepro.strategyv2.RawClosure',remote,str(n),str(total),expected],capture_output=True,timeout=420)
    body=race.stdout+race.stderr;(area/'android-closure.log').write_bytes(body)
    proof=re.search(rb'RAW_CLOSURE roots=(\d+) objects=(\d+) bytes=(\d+) collection=([a-f0-9]{64}) sqlite=ok',body)
    check(race.returncode==0 and proof and int(proof[2])==n and int(proof[3])==total and proof[4].decode()==expected,'SNAPSHOT_DESCENDANTS')
    return dict(fullRawClosureReplayed=True,uniqueSnapshotRoots=int(proof[1]),checkpointBodies=n,checkpointBytes=total,checkpointSetSha256=expected,sqliteIntegrityOk=True,nativeClosureLogSha256=digest(body),rawSnapshotBytes=db.stat().st_size,rawSnapshotSha256=field.fingerprint(db),**training)
def reopen_uri(uri,file,expected):
    parsed=urlparse(uri)
    check(parsed.scheme=='content' and parsed.netloc in {'com.android.externalstorage.documents','com.android.providers.downloads.documents','com.google.android.documentsui.fileprovider'},'SAF_URI_AUTHORITY')
    with file.open('wb') as target:
        p=subprocess.run(['adb','exec-out','content','read','--user','0','--uri',uri],stdout=target,stderr=subprocess.PIPE,timeout=350)
    if p.returncode==0 and field.fingerprint(file)==expected:return 'CONTENT_RESOLVER'
    document=unquote(parsed.path.rsplit('/document/',1)[-1])
    local='/storage/emulated/0/'+document[len('primary:'):] if document.startswith('primary:') else document[4:] if document.startswith('raw:') else None
    if local and local.startswith(('/storage/emulated/0/','/sdcard/','/mnt/user/0/primary/')) and '..' not in Path(local).parts:
        pull(local,file)
        if field.fingerprint(file)==expected:return 'EXACT_DOCUMENT_PATH'
    return 'UNREAD'
def system_save(index,job,archive,dest,first_volume):
    if index['format']=='SCREPORT_SINGLE_ZIP_R992':
        report_dialog(single_label)
        saf=ui.save_via_real_document_picker(job,index['files'][0],archive)
        check(saf['actualSystemPicker'] and saf['sameAsPublishedZip'] and saf['externalFileSha256']==field.fingerprint(archive),'SINGLE_SAF_RETURNED')
        return dict(actualSystemPicker=True,safExternalZipSha256=saf['externalFileSha256'],verifiedVolumesIncludingIndex=1)
    report_dialog(save_label)
    key='STARCORE-SameCandidate-Exports'
    folder='/sdcard/Download/'+key
    field.adb('shell','mkdir','-p',folder)
    prior=ui.acknowledgements();select_tree(key)
    verified=None
    end=time.monotonic()+350
    while time.monotonic()<end:
        for path in ui.acknowledgements()-prior:
            try:ack=ui.read_json(path)
            except (ValueError,RuntimeError):continue
            if ack.get('state')=='VERIFIED_READBACK' and ack.get('allVolumesSaved') is True:
                verified=ack;verified_file=path;break
        if verified:break
        time.sleep(1)
    check(verified is not None and verified.get('volumeCount')==len(index['files'])+1 and verified.get('manifestSha256')==index['manifestSha256'],'SAF_TRANSFER_ACK')
    transfer=Path(verified_file).name.removesuffix('.ack.json')
    check(identity.fullmatch(transfer),'SAF_TRANSFER_ACK')
    ext=dest/'external';ext.mkdir()
    expect=[(m['name'],m['sha256'],m['bytes']) for m in index['files']]+[('index.json',field.fingerprint(dest/'index.json'),(dest/'index.json').stat().st_size)]
    verified_parts=[];access=[]
    for i,(name,expected,sz) in enumerate(expect):
        saved=ui.read_json(ui.ROOT+'/saf-parts/'+transfer+'/'+str(i)+'.json')
        check((saved['name'],saved['sha256'],saved['bytes'])==(name,expected,sz),'SAF_TRANSFER_ACK')
        one=ext/name;by=reopen_uri(saved['uri'],one,expected)
        check(by!='UNREAD' and one.stat().st_size==sz and field.fingerprint(one)==expected,'SAF_EXTERNAL_PROVIDER_BYTES')
        access.append(by)
        if name!='index.json':verified_parts.append(one)
    check((ext/'index.json').read_bytes()==(dest/'index.json').read_bytes(),'SAF_EXTERNAL_INDEX_SHA')
    external=ext/'joined.zip'
    with external.open('wb') as out:
        for member in verified_parts:
            with member.open('rb') as src:shutil.copyfileobj(src,out)
    check([field.fingerprint(f) for f in verified_parts]==first_volume and field.fingerprint(external)==field.fingerprint(archive),'SAF_EXTERNAL_READBACK_DIGEST')
    parts=verified_parts
    return dict(actualSystemPicker=True,safExternalZipSha256=field.fingerprint(external),verifiedVolumesIncludingIndex=len(parts)+1,providerAccess=access)
def observe_native(raw,attempt,known):
    result=dict(attempt=attempt,sourceAndApkBound=False,fullyVerifiedRawAndSystemPicker=False,phase='UI_INDEX')
    log=raw/'ui-console.txt'
    if log.is_file():
        body=log.read_bytes();result['originalDriverLogSha256']=digest(body)
        result['originalErrors']=[c for c in ['UI_REPORT_SINGLE_ZIP_REQUIRED','UI_REPORT_PUBLICATION_TIMEOUT','UI_REPORT_FAILED','UI_REPORT_JOB_ID','UI_TEXT_NOT_FOUND','ZLIB_ERROR'] if c.encode() in body]
    indices=dict((p.parent.name,p) for p in (raw/'native').glob('*/index.json') if identity.fullmatch(p.parent.name))
    fresh=sorted(set(indices)-known);known.update(indices)
    if len(fresh)!=1:
        result['newNativeJobs']=len(fresh);return result
    job=fresh[0];index_file=indices[job];index=read(index_file)
    result.update(jobId=job,format=index['format'] if index.get('format') in expected_formats else 'OTHER',indexSha256=field.fingerprint(index_file))
    dest=private/str(attempt);dest.mkdir()
    shutil.copyfile(index_file,dest/'index.json')
    try:
        check(index['jobId']==job and index['format'] in expected_formats,'UNKNOWN_INDEX')
        result['phase']='SOURCE_PARTS'
        vols=dest/'parts';vols.mkdir()
        joined=dest/'joined.zip'
        hashes=assemble(index,ui.ROOT+'/jobs/'+job+'/delivery',vols,joined)
        result.update(parts=len(hashes),partSha256=hashes,archiveBytes=joined.stat().st_size,archiveSha256=field.fingerprint(joined))
        result['phase']='INNER'
        with zipfile.ZipFile(joined) as z:
            check(z.testzip() is None and len(set(z.namelist()))==len(z.namelist()),'CRC_MEMBER_DAMAGE')
            mf=json.loads(z.read('source-manifest.json'))
            check(mf['sourceCommit']==source and mf['apkSha256']==apk,'SOURCE_APK_MISMATCH')
            check(digest(z.read('ARCHIVE_MANIFEST.jsonl'))==index['manifestSha256'],'INNER_MANIFEST')
            audit=mf.get('r26CheckpointAuditR32',{})
            result['checkpointSource']=dict((k,audit[k]) for k in ('observedObjects','missingObjects','unknownChildSets','sourceClosureVerified') if type(audit.get(k)) in (int,bool))
            meta=audit.get('metadataMember')
            if isinstance(meta,str) and meta in z.namelist():
                items=[json.loads(line) for line in z.read(meta).splitlines() if line.strip()]
                objs=[row for row in items if row.get('kind')=='checkpoint_object']
                result['inventorySha256']=digest(z.read(meta))
                result['recountedCheckpointObjects']=len(objs)
                result['recountedMissingCheckpointObjects']=sum(row.get('availability')=='MISSING_OBJECT' for row in objs)
                result['recountedUnknownChildren']=sum(row.get('childrenKnown') is False for row in objs)
            result.update(sourceAndApkBound=True,crcAndManifestMatch=True,memberCount=len(z.namelist()),rawEvidenceIncluded=mf.get('rawEvidenceIncluded') is True,hasNativeSnapshot='snapshot.db' in z.namelist())
            if result['hasNativeSnapshot']:
                with z.open('snapshot.db') as stream:db=hashlib.file_digest(stream,'sha256').hexdigest()
                result['originalSnapshotHashAndSizeMatch']=(db,z.getinfo('snapshot.db').file_size)==(mf.get('snapshotSha256'),mf.get('snapshotBytes'))
        try:
            strict=oracle.validate_export(joined,{'profile':'FULL_RAW','jobId':job,'sourceCommit':source,'apkSha256':apk})
            result.update(strictStatus='ARTIFACT_CONSISTENT' if strict['status']=='ARTIFACT_CONSISTENT' else 'OTHER',sourceClosureVerified=strict.get('sourceClosureVerified') is True)
        except (ValueError,KeyError,TypeError) as ex:
            result['strictStatus']='REFUSED';result['strictReasonFingerprint']=digest(str(ex).encode())
            result['strictRefusalCode']=str(ex) if str(ex) in {'ARCHIVE_INCOMPLETE','CHECKPOINT_RECOUNT','CHECKPOINT_FALSE_CLOSURE','CHECKPOINT_SUMMARY_SOURCE_MISMATCH','ZIP_MANIFEST_SET','SNAPSHOT_SHA','SOURCE_READER'} else 'OTHER'
        result['phase']='RAW_CLOSURE'
        result.update(extract_full_raw_closure(joined,job))
        result['phase']='SAF'
        result.update(system_save(index,job,joined,dest,hashes))
        result['fullyVerifiedRawAndSystemPicker']=result.get('strictStatus')=='ARTIFACT_CONSISTENT' and result.get('fullRawClosureReplayed') is True and result.get('rawEvidenceIncluded') is True and result.get('originalSnapshotHashAndSizeMatch') is True
        result['phase']='DONE'
    except (ValueError,OSError,KeyError,TypeError,RuntimeError,AssertionError,subprocess.SubprocessError) as error:
        allowed={'TRAINING_ROW_IDENTITY','TRAINING_DESCRIPTOR','TRAINING_BODY_IDENTITY','TRAINING_DB_CLOSURE','FULL_RAW_MODE','DUPLICATE_RAW_BODY','RAW_BODY_IDENTITY','RAW_INVENTORY','ORIGINAL_DB_BYTES','SNAPSHOT_DESCENDANTS','ORIGINAL_APK','SAF_URI_AUTHORITY','SAF_EXTERNAL_PROVIDER_BYTES','UNKNOWN_INDEX','VOLUME_COUNT','DUPLICATE_OR_FORMAT','NATIVE_INDEX_PATH','NATIVE_MEMBER_IDENTITY','NATIVE_PART_ORDER','NATIVE_PART_SIZE','NATIVE_MEMBER_PULL_FAILED','NATIVE_PART_READBACK','REASSEMBLED_ZIP_IDENTITY','CRC_MEMBER_DAMAGE','SOURCE_APK_MISMATCH','INNER_MANIFEST','CURRENT_JOB_SHARE_DIALOG_UNAVAILABLE','SINGLE_SAF_RETURNED','SAF_DOWNLOADS_ROOT','SAF_DESTINATION_FOLDER','SAF_USE_FOLDER_BUTTON','SAF_TRANSFER_ACK','SAF_EXTERNAL_INDEX_SHA','SAF_EXTERNAL_READBACK_DIGEST'}
        result['failureCode']=str(error) if str(error) in allowed else type(error).__name__
        result['failureDigest']=digest(str(error).encode())
    return result
def main():
    global apk
    assert field.adb('get-serialno').decode().startswith('emulator-') and field.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    field.adb('root');field.adb('wait-for-device')
    (field.EVID/'B08').mkdir(parents=True)
    field.NATIVE.mkdir()
    original=observe.get_apk('B08',577311065,'a724adbfb31dd96f1459aa2e5cbd94582dd6e82d')
    prev=original.with_name('prior-065.apk');original.rename(prev)
    check(field.fingerprint(prev)=='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced','PRIOR_APK')
    final=observe.get_apk('B08',577311067,source)
    apk=field.fingerprint(final)
    check(field.fingerprint(final)==apk and field.fingerprint(prev)!=apk,'CANDIDATE')
    check(b'Success' in field.adb('install',str(prev)),'PREVIOUS_INSTALL')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY);time.sleep(45)
    uid,old=field.installed(field.EVID/'previous-dump.txt',577311065);check(old==field.fingerprint(prev),'PRIOR_APK')
    check(b'Success' in field.adb('install','-r',str(final)),'CURRENT_INSTALL')
    check(field.installed(field.EVID/'package.txt',577311067)==(uid,apk),'INITIAL_PACKAGE')
    field.adb('shell','pm','grant',field.PKG,'android.permission.POST_NOTIFICATIONS')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY)
    original=field.dump;known=set();results=[]
    def at_attempt(path,value):
        original(path,value)
        if path.name.startswith('report-attempt-'):
            n=int(value['attempt'])
            results.append(observe_native(field.NATIVE/'B08'/str(n),n,known))
            if n<3:
                # Exit the completed transfer's system UI between requests and reopen MainActivity with
                # a clean Android activity stack, preserving the installed release, data and prior jobs.
                field.adb('shell','am','start','-W','-S','-n',field.ACTIVITY)
                time.sleep(10)
    field.dump=at_attempt
    field.safe_reports('B08',3,source)
    assert len(results)==3 and field.installed(field.EVID/'final-package.txt',577311067)==(uid,apk)
    doc=dict(sourceCommit=source,apkSha256=apk,runId=int(os.environ['GITHUB_RUN_ID']),scope='ACTUAL_ANDROID_SNAPSHOT_CHECKPOINT_GRAPH_AND_SAF_PROVIDER',attempts=results,
        strictDistinct=len({x['jobId'] for x in results if x.get('fullyVerifiedRawAndSystemPicker')}),
        reopenedRawSnapshots=len({x['jobId'] for x in results if x.get('actualSystemPicker') and x.get('originalSnapshotHashAndSizeMatch') and x.get('safExternalZipSha256')==x.get('archiveSha256')}))
    (field.TEMP/('R335-vivo-final-export-'+str(doc['runId'])+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
if __name__=='__main__':main()
