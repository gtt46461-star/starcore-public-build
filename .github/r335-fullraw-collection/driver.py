"""Run three real full-raw requests on the final APK and publish only typed artifact checks."""
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import time
import zipfile

workspace = Path(os.environ['GITHUB_WORKSPACE'])
sys.path.insert(0,str(workspace/'.github/r335-field-metrics'))
import observe
import field

apk_sha='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
source='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
def read(p):
    return json.loads(p.read_bytes())
def summarize(folder,attempt):
    row={'attempt':attempt, 'profileRequested':'FULL_RAW','strictFullRawVerified':False,'safBytesReopened':False}
    raw=folder/'UI_EXPORTED_FULL_REPORT.zip'
    if raw.is_file():
        row['archiveSha256']=field.fingerprint(raw)
        row['archiveBytes']=raw.stat().st_size
        with zipfile.ZipFile(raw) as archive:
            names=archive.namelist()
            row['crcAllOk']=archive.testzip() is None
            row['distinctMembers']=len(set(names))
            if 'source-manifest.json' in names:
                manifest=json.loads(archive.read('source-manifest.json'))
                row['sourceAndApkMatch']=(manifest.get('sourceCommit'),manifest.get('apkSha256'))==(source,apk_sha)
                row['rawEvidenceIncluded']=manifest.get('rawEvidenceIncluded') is True
                if 'snapshot.db' in names:
                    with archive.open('snapshot.db') as db:
                        checked=hashlib.file_digest(db,'sha256').hexdigest()
                    row['originalDbMatchesDeclaredHash']=checked==manifest.get('snapshotSha256')
                    row['originalDbBytes']=archive.getinfo('snapshot.db').file_size
    strict=folder/'strict-full-raw.json'
    if strict.is_file():
        doc=read(strict)
        row['strictFullRawVerified']=doc.get('sourceClosureVerified') is True and doc.get('status')=='ARTIFACT_CONSISTENT'
        row['strictToolReceiptSha256']=field.fingerprint(strict)
    acceptance=folder/'ui-report-acceptance.json'
    if acceptance.is_file():
        result=read(acceptance)
        assert result['sourceCommit']==source and result['zipSha256']==row['archiveSha256']
        assert re.fullmatch('[a-f0-9]{64}',result['jobId']) and result['result']=='PASS'
        saf=result['saf']
        assert saf['state']=='VERIFIED_READBACK' and saf['actualSystemPicker'] is True and saf['sameAsPublishedZip'] is True
        assert saf['externalFileSha256']==row['archiveSha256'] and saf['bytes']==row['archiveBytes']
        row.update(jobId=result['jobId'],safBytesReopened=True,acceptanceSha256=field.fingerprint(acceptance))
    live=folder/'safe-report-liveness.json'
    if live.is_file():
        samples=read(live)
        reasons={'CANCEL_COMMAND_LOCK_BUSY','REPORT_RESOURCE_WAIT','FREE_SPACE_RESERVE','ENOSPC','CHECKPOINT_ORIGINAL_MISSING','REPORT_STAGE_TIMEOUT','REPORT_DISK_BUDGET'}
        states={'CLAIMED','BACKUP','PAUSED','INDEXING','PUBLISHING','READY_TO_SHARE','FAILED','CANCELLED','CONTROL_FAILED'}
        jobs=[]
        for sample in samples:
            for rec in sample['rows']:
                job=rec.get('jobId');stage=rec.get('stage')
                if rec.get('area')!='jobs' or stage not in states or not isinstance(job,str) or not re.fullmatch('[a-f0-9]{64}',job):continue
                observation=dict(jobId=job,stage=stage,revision=int(rec.get('revision',0)),reasonCodes=sorted(reasons.intersection(rec.get('publicReasonCodes',[]))))
                if observation not in jobs:jobs.append(observation)
        row['observedStates']=jobs
        row['rawLivenessSha256']=field.fingerprint(live)
    return row
def main():
    assert field.adb('get-serialno').decode().startswith('emulator-') and field.adb('shell','getprop','ro.kernel.qemu').strip()==b'1'
    field.adb('root');field.adb('wait-for-device')
    (field.EVID/'B08').mkdir(parents=True)
    field.NATIVE.mkdir()
    earlier,latest=[observe.get_apk(*binding) for binding in [observe.B07,observe.B08]]
    assert field.fingerprint(latest)==apk_sha and observe.B08[2]==source
    assert b'Success' in field.adb('install',str(earlier))
    field.adb('shell','am','start','-W','-n',field.ACTIVITY)
    time.sleep(50)
    before,previous=field.installed(field.EVID/'prior-package.txt',observe.B07[1])
    assert previous==field.fingerprint(earlier)
    assert b'Success' in field.adb('install','-r',str(latest))
    after,current=field.installed(field.EVID/'installed-package.txt',observe.B08[1])
    assert after==before and current==apk_sha
    field.adb('shell','pm','grant',field.PKG,'android.permission.POST_NOTIFICATIONS')
    field.adb('shell','am','start','-W','-n',field.ACTIVITY)
    field.safe_reports('B08',3,source)
    rows=[summarize(field.NATIVE/'B08'/str(i),i) for i in range(1,4)]
    assert field.installed(field.EVID/'final-package.txt',observe.B08[1])==(before,apk_sha)
    successes=[x['jobId'] for x in rows if x.get('jobId') and x['strictFullRawVerified'] and x['safBytesReopened']]
    doc=dict(role='API34_NATIVE_FULL_RAW_SEQUENTIAL',runId=int(os.environ['GITHUB_RUN_ID']),sourceCommit=source,apkSha256=apk_sha,
             installedBeforeAndAfterSame=True,attempts=rows,distinctJobsWithStrictFullRawAndSaf=len(set(successes)))
    (field.TEMP/('R335-fullraw-collection-'+str(doc['runId'])+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
if __name__=='__main__':main()
