"""Receive the actual single-volume or multipart package and reopen bytes saved by the Android document picker."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import zipfile

workspace=Path(os.environ['GITHUB_WORKSPACE'])
sys.path.insert(0,str(workspace/'.github/r335-field-metrics'))
import field
import observe

source='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
apk='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
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
    drawer=tap(lambda n:n.get('content-desc','').lower() in ['show roots','open navigation drawer','显示根目录','打开导航抽屉'],12)
    if drawer:
        check(tap(lambda n:n.get('text','') in ['Downloads','下载','Download']),'SAF_DOWNLOADS_ROOT')
    else:
        if folder not in labels():tap(lambda n:n.get('text','') in ['Downloads','下载','Download'],8)
    check(tap(lambda n:n.get('text','')==folder),'SAF_DESTINATION_FOLDER')
    check(tap(lambda n:n.get('resource-id','').endswith('/action_menu_select') or n.get('text','').lower() in ['use this folder','使用此文件夹','使用这个文件夹','选择此文件夹']),'SAF_USE_FOLDER_BUTTON')
    tap(lambda n:n.get('text','').lower() in ['allow','允许'] or n.get('resource-id')=='android:id/button1',15)
def system_save(index,job,archive,dest,first_volume):
    if index['format']=='SCREPORT_SINGLE_ZIP_R992':
        report_dialog(single_label)
        saf=ui.save_via_real_document_picker(job,index['files'][0],archive)
        check(saf['actualSystemPicker'] and saf['sameAsPublishedZip'] and saf['externalFileSha256']==field.fingerprint(archive),'SINGLE_SAF_RETURNED')
        return dict(actualSystemPicker=True,safExternalZipSha256=saf['externalFileSha256'],verifiedVolumesIncludingIndex=1)
    report_dialog(save_label)
    key='STARCORE-R335-'+job[:16]
    folder='/sdcard/Download/'+key
    field.adb('shell','mkdir',folder)
    prior=ui.acknowledgements();select_tree(key)
    verified=None
    end=time.monotonic()+350
    while time.monotonic()<end:
        for path in ui.acknowledgements()-prior:
            try:ack=ui.read_json(path)
            except (ValueError,RuntimeError):continue
            if ack.get('state')=='VERIFIED_READBACK' and ack.get('allVolumesSaved') is True:
                verified=ack;break
        if verified:break
        time.sleep(1)
    check(verified is not None and verified.get('volumeCount')==len(index['files'])+1 and verified.get('manifestSha256')==index['manifestSha256'],'SAF_TRANSFER_ACK')
    saved=folder+'/STARCORE-'+job;ext=dest/'external';ext.mkdir()
    externally_saved_index=ext/'index.json';pull(saved+'/index.json',externally_saved_index)
    check(externally_saved_index.read_bytes()==(dest/'index.json').read_bytes(),'SAF_EXTERNAL_INDEX_SHA')
    external=ext/'joined.zip';parts=assemble(read(externally_saved_index),saved,ext,external)
    check(parts==first_volume and field.fingerprint(external)==field.fingerprint(archive),'SAF_EXTERNAL_READBACK_DIGEST')
    return dict(actualSystemPicker=True,safExternalZipSha256=field.fingerprint(external),verifiedVolumesIncludingIndex=len(parts)+1)
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
            result.update(sourceAndApkBound=True,crcAndManifestMatch=True,memberCount=len(z.namelist()),rawEvidenceIncluded=mf.get('rawEvidenceIncluded') is True,hasNativeSnapshot='snapshot.db' in z.namelist())
            if result['hasNativeSnapshot']:
                with z.open('snapshot.db') as stream:db=hashlib.file_digest(stream,'sha256').hexdigest()
                result['originalSnapshotHashAndSizeMatch']=(db,z.getinfo('snapshot.db').file_size)==(mf.get('snapshotSha256'),mf.get('snapshotBytes'))
        try:
            strict=oracle.validate_export(joined,{'profile':'FULL_RAW','jobId':job,'sourceCommit':source,'apkSha256':apk})
            result.update(strictStatus='ARTIFACT_CONSISTENT' if strict['status']=='ARTIFACT_CONSISTENT' else 'OTHER',sourceClosureVerified=strict.get('sourceClosureVerified') is True)
        except (ValueError,KeyError,TypeError) as ex:
            result['strictStatus']='REFUSED';result['strictReasonFingerprint']=digest(str(ex).encode())
        result['phase']='SAF'
        result.update(system_save(index,job,joined,dest,hashes))
        result['fullyVerifiedRawAndSystemPicker']=result.get('strictStatus')=='ARTIFACT_CONSISTENT' and result.get('sourceClosureVerified') is True and result.get('rawEvidenceIncluded') is True and result.get('originalSnapshotHashAndSizeMatch') is True
        result['phase']='DONE'
    except (ValueError,OSError,KeyError,TypeError,RuntimeError,AssertionError,subprocess.SubprocessError) as error:
        allowed={'UNKNOWN_INDEX','VOLUME_COUNT','DUPLICATE_OR_FORMAT','NATIVE_INDEX_PATH','NATIVE_MEMBER_IDENTITY','NATIVE_PART_ORDER','NATIVE_PART_SIZE','NATIVE_MEMBER_PULL_FAILED','NATIVE_PART_READBACK','REASSEMBLED_ZIP_IDENTITY','CRC_MEMBER_DAMAGE','SOURCE_APK_MISMATCH','INNER_MANIFEST','CURRENT_JOB_SHARE_DIALOG_UNAVAILABLE','SINGLE_SAF_RETURNED','SAF_DOWNLOADS_ROOT','SAF_DESTINATION_FOLDER','SAF_USE_FOLDER_BUTTON','SAF_TRANSFER_ACK','SAF_EXTERNAL_INDEX_SHA','SAF_EXTERNAL_READBACK_DIGEST'}
        result['failureCode']=str(error) if str(error) in allowed else type(error).__name__
        result['failureDigest']=digest(str(error).encode())
    return result
def main():
    assert field.adb('get-serialno').decode().startswith('emulator-') and field.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    field.adb('root');field.adb('wait-for-device')
    (field.EVID/'B08').mkdir(parents=True)
    field.NATIVE.mkdir()
    prev,final=[observe.get_apk(*x) for x in [observe.B07,observe.B08]]
    check(field.fingerprint(final)==apk and observe.B08[2]==source,'CANDIDATE')
    check(b'Success' in field.adb('install',str(prev)),'PREVIOUS_INSTALL')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY);time.sleep(45)
    uid,old=field.installed(field.EVID/'previous-dump.txt',observe.B07[1]);check(old==field.fingerprint(prev),'PRIOR_APK')
    check(b'Success' in field.adb('install','-r',str(final)),'CURRENT_INSTALL')
    check(field.installed(field.EVID/'package.txt',observe.B08[1])==(uid,apk),'INITIAL_PACKAGE')
    field.adb('shell','pm','grant',field.PKG,'android.permission.POST_NOTIFICATIONS')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY)
    original=field.dump;known=set();results=[]
    def at_attempt(path,value):
        original(path,value)
        if path.name.startswith('report-attempt-'):
            n=int(value['attempt'])
            results.append(observe_native(field.NATIVE/'B08'/str(n),n,known))
    field.dump=at_attempt
    field.safe_reports('B08',3,source)
    assert len(results)==3 and field.installed(field.EVID/'final-package.txt',observe.B08[1])==(uid,apk)
    doc=dict(sourceCommit=source,apkSha256=apk,runId=int(os.environ['GITHUB_RUN_ID']),scope='NATIVE_INDEX_PRIVATE_MEMBER_REASSEMBLY_AND_SYSTEM_SAF',attempts=results,
        strictDistinct=len({x['jobId'] for x in results if x.get('fullyVerifiedRawAndSystemPicker')}))
    (field.TEMP/('R335-export-readback-'+str(doc['runId'])+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
if __name__=='__main__':main()
