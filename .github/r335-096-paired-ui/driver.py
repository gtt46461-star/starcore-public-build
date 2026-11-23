"""Open two signed installed versions' own default reports in one test guest with data retained."""
import ast
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile

project = Path(os.environ['GITHUB_WORKSPACE'])
temp = Path(os.environ['RUNNER_TEMP'])
repo = 'https://github.com/gtt46461-star/starcore-public-build.git'
compact = project/'.github/r335-096-compact/driver.py'
fixed_driver_sha = 'f30fddb848a50267eb984cb655dba30be06f93dd84f74992522457cbdbd4a701'
assert hashlib.sha256(compact.read_bytes()).hexdigest() == fixed_driver_sha
commits = {95: ('273641e838e0b8dd842b42a67a674e184942a30a','0faab4a067efcd63c71bced7c4bbb8287e43f784bb636f6a0faa0dcb8e3d0b2a'),
           96: ('68280a1f8d2e05ab7b1996f876a94ac95793fc7b','08663d5998db4d346ecdb0612d15e2aa378970ff7952d978c1921d98e8a2008d')}
run_id = int(os.environ['GITHUB_RUN_ID'])
out = temp/f'R335-096-paired-ui-{run_id}.json'
receipt = dict(sourceCommit=commits[96][0],apkSha256=commits[96][1],runId=run_id,scope='TWO_NATIVE_DEFAULT_REPORTS_SAME_DISPOSABLE_ANDROID14_RETAINED_UID',attempts=[],verifiedPair=False)
def persist():
    out.write_text(json.dumps(receipt,indent=2)+'\n')
def sha(data):
    return hashlib.sha256(data).hexdigest()
persist()
SAF_EXACT_SOURCE='# Android may select a suffixed display name for a second export in the same Downloads folder.\n# Retain the original system picker, acknowledgement, and expected SHA checks up to the reopening step.\nimport inspect as _inspect\n_saf_source = _inspect.getsource(n.ui.save_via_real_document_picker)\nassert hashlib.sha256(_saf_source.encode()).hexdigest() == \'ebfd62ef6f74c7b659b1558ea43a30280bf78099d083dd57a4adb80a28acfcd0\'\n_cut = _saf_source.index("    downloaded=args.out/\'SAF_SAVED_FULL_REPORT.zip\'")\nn.ui._paired_read_uri = n.reopen_uri\n_saf_source = _saf_source[:_cut] + """    downloaded=args.out/\'SAF_SAVED_FULL_REPORT.zip\'\n    receipt_request=read_json(path.removesuffix(\'.ack.json\')+\'.json\')\n    assert receipt_request[\'jobId\']==job and receipt_request[\'volumeName\']==member[\'name\']\n    destination=receipt_request[\'destination\']\n    access=_paired_read_uri(destination,downloaded,member[\'sha256\'])\n    digest=hashlib.sha256(downloaded.read_bytes()).hexdigest()\n    assert access in (\'CONTENT_RESOLVER\',\'EXACT_DOCUMENT_PATH\') and digest==member[\'sha256\'] and downloaded.stat().st_size==member[\'bytes\'],\'REPORT_SAF_EXACT_DOCUMENT_READBACK_MISMATCH\'\n    return dict(state=\'VERIFIED_READBACK\',externalFileSha256=digest,bytes=downloaded.stat().st_size,actualSystemPicker=True,sameAsPublishedZip=True)\n"""\nexec(compile(_saf_source,n.ui.__file__,\'exec\'),n.ui.__dict__)\n'
def run_report(v,commit,digest,attempt):
    root = temp/('paired-report-%03d-%d'%(v,attempt))
    root.mkdir()
    (root/'material').symlink_to(temp/'material',target_is_directory=True)
    (root/'tools').symlink_to(temp/'tools',target_is_directory=True)
    (root/'report_ui_navigation.py').symlink_to(compact.parent/'report_ui_navigation.py')
    # Reuse the exact registered UI/readback driver. Only the installation history is changed.
    template = compact.read_text()
    start = template.index(" check(not f.adb('shell','pm','list','packages',pkg).strip()")
    stop = template.index(" f.adb('shell','pm','grant',pkg", start)
    if attempt > 1:
        setup = f""" olduid,prior=f.installed(f.EVID/'before.txt',{577311000+v})
 check(prior==n.apk,'GUEST_VERSION')
 f.adb('shell','input','keyevent','KEYCODE_BACK')
 f.adb('shell','am','start','-W','-n',f.ACTIVITY)
"""
    elif v == 95:
        setup = template[start:stop].replace('577311096,n.source','577311095,n.source').replace("'current.txt',577311096","'current.txt',577311095")
    else:
        setup = """ olduid,prior=f.installed(f.EVID/'before.txt',577311095)
 check(prior=='0faab4a067efcd63c71bced7c4bbb8287e43f784bb636f6a0faa0dcb8e3d0b2a','PRIOR_RELEASE')
 current=n.observe.get_apk('B08',577311096,n.source);check(f.fingerprint(current)==n.apk,'SIGNED_IDENTITY')
 f.adb('install','-r',str(current))
 check(f.installed(f.EVID/'current.txt',577311096)==(olduid,n.apk),'GUEST_VERSION')
"""
    setup += " record['androidUid']=olduid\n"
    modified = (template[:start]+setup+template[stop:]).replace("n.source='"+commits[96][0]+"'","n.source='"+commit+"'")
    marker='def main():'
    modified=modified.replace(marker,SAF_EXACT_SOURCE+'\n'+marker,1)
    target="    strict=n.oracle.validate_export(archive,{'profile':'DIAGNOSTIC','jobId':job,'sourceCommit':n.source,'apkSha256':n.apk})"
    assert modified.count(target)==1
    modified=modified.replace(target, "    try:\n "+target+"\n    except n.oracle.InvalidEvidence as rejected:\n     if str(rejected)!='ARCHIVE_INCOMPLETE':raise\n     strict={'status':str(rejected)}\n     record['originalVerifierRejected']=True")
    modified=modified.replace("save('VERIFIED');return","save('RETURNED_WITH_INCOMPLETE_CHECKS' if record.get('originalVerifierRejected') else 'VERIFIED');return")
    ast.parse(modified)
    driver=root/'run_report.py';driver.write_text(modified)
    env=dict(os.environ,RUNNER_TEMP=str(root),REPAIRED_APK_SHA=digest)
    with (root/'private.log').open('wb') as log:
        process=subprocess.run([sys.executable,'-B',driver],env=env,stdout=log,stderr=log,timeout=2700)
    original=json.loads((root/f'R335-095-compact-{run_id}.json').read_bytes())
    assert original['sourceCommit']==commit and original['apkSha256']==digest
    item={'version':577311000+v,'sourceCommit':commit,'apkSha256':digest,'exit':process.returncode,'driverSha256':sha(driver.read_bytes()),'stage':original['stage'],'originalReceiptSha256':sha((root/f'R335-095-compact-{run_id}.json').read_bytes())}
    item['attempt']=attempt
    for key in ('checksComplete','allChecksPassed','failurePhase','failure','androidUid','actualSystemPicker','originalVerifierRejected','archiveBytes','jobId','archiveSha256','safExternalZipSha256','originalVerifier','checkExecutions'):
        if key in original:item[key]=original[key]
    if attempt>1:
        receipt.setdefault('priorAttempts',[]).append(receipt['attempts'].pop())
    receipt['attempts'].append(item);persist()
    if original['stage'] not in ('VERIFIED','RETURNED_WITH_INCOMPLETE_CHECKS'):return False
    assert original['actualSystemPicker'] and original['safExternalZipSha256']==original['archiveSha256']
    local=root/'separate-private-native-readback/joined.zip'
    assert sha(local.read_bytes())==original['archiveSha256']
    with zipfile.ZipFile(local) as z:
        assert z.testzip() is None
        manifest=json.loads(z.read('source-manifest.json'));sem=json.loads(z.read('R31_SEMANTIC_RECEIPT.json'))
        raw=z.read('R30_REPORT.json');report=json.loads(raw)
        assert manifest['sourceCommit']==commit and manifest['apkSha256']==digest and sem['reportSha256']==sha(raw)
        assert sem['jobId']==report['snapshotWatermark']['reportJobId']==original['jobId']
        ids=json.loads((temp/'material/STARCORE_R33_5_20261118/B01/scripts/report_expected_ids_r334.json').read_bytes())['checks']
        assert set(ids)==set(sem['checks']) and len(ids)==96
        states={}
        for key in ids:
            assert re.fullmatch('RSEC-[0-9]{3}',key)
            check=sem['checks'][key]
            states[key]=[check['execution_state'],check.get('domain_outcome') or check.get('health')]
            assert all(value=='N/A' or re.fullmatch('[A-Z][A-Z0-9_|-]{1,100}',value) for value in states[key])
        item.update(uid=original['androidUid'],jobId=original['jobId'],archiveBytes=original['archiveBytes'],archiveSha256=original['archiveSha256'],safSha256=original['safExternalZipSha256'],reportSha256=sha(raw),semanticSha256=sha(z.read('R31_SEMANTIC_RECEIPT.json')),states=states,actualSystemPicker=True,oracle=original['originalVerifier'])
    persist()
    return True

for version,(source,apk) in commits.items():
    for attempt in (1,2):
        try:
            if run_report(version,source,apk,attempt):break
        except (ValueError, KeyError, AssertionError, OSError, subprocess.SubprocessError) as problem:
            import traceback
            frame=traceback.extract_tb(problem.__traceback__)[-1]
            current=receipt['attempts'][-1] if receipt['attempts'] else {}
            if current.get('version')!=577311000+version or current.get('attempt')!=attempt:
                if attempt>1 and current.get('version')==577311000+version:
                    receipt.setdefault('priorAttempts',[]).append(receipt['attempts'].pop())
                current=dict(version=577311000+version,attempt=attempt,sourceCommit=source,apkSha256=apk,stage='POST_VERIFICATION_INCOMPLETE')
                receipt['attempts'].append(current)
            current.update(projectionErrorType=type(problem).__name__,projectionErrorLine=frame.lineno)
            persist()
if len(receipt['attempts'])==2 and all(x.get('actualSystemPicker') and len(x.get('states',{}))==96 for x in receipt['attempts']):
    first,last=receipt['attempts'];assert first['uid']==last['uid'] and first['jobId']!=last['jobId']
    repo_dir=project/'app-source'
    subprocess.run(['git','-C',repo_dir,'fetch','--quiet','--depth=1','--filter=blob:none','origin',commits[95][0]],check=True)
    changes=[]
    for filename in ['StartupCheckpointIdentityR31.java','ResearchRetentionStoreR30.java','RuntimeCoordinatorR26.java','AutoFetchService.java','SchemaDomainCoordinatorR926.java']:
        paths=[p.relative_to(repo_dir).as_posix() for p in (repo_dir/'app/src').rglob(filename)]
        assert len(paths)==1;path=paths[0]
        images=[sha(subprocess.check_output(['git','-C',repo_dir,'show',commit+':'+path])) for commit,_ in commits.values()]
        assert images[0]!=images[1];changes.append(dict(path=path,beforeSha256=images[0],afterSha256=images[1]))
    receipt.update(verifiedPair=True,gitChanges=changes,casesNowExecuted=[cid for cid,state in first['states'].items() if state[0]=='READ_ERROR' and last['states'][cid][0]=='EXECUTED'],matchingCaseIds=sorted(first['states'])==sorted(last['states']))
    persist()

if not receipt['verifiedPair']:raise SystemExit(1)
