"""Open two signed installed versions' own default reports in one test guest with data retained."""
import ast
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

project = Path(os.environ['GITHUB_WORKSPACE'])
temp = Path(os.environ['RUNNER_TEMP'])
repo = 'https://github.com/gtt46461-star/starcore-public-build.git'
compact = project/'.github/r335-095-compact/driver.py'
fixed_driver_sha = '2fbbdbc0f095eca848eddfe7ab6421b79f027e689b2430968ebf51812970d982'
assert hashlib.sha256(compact.read_bytes()).hexdigest() == fixed_driver_sha
commits = {94: ('d417e459c1bd0e89d70b4dd549f5555b93e49d4e','e7d36e074852ba330b7786460b48569271a9e46abb1a75c40a6824aa44c94aca'),
           95: ('273641e838e0b8dd842b42a67a674e184942a30a','0faab4a067efcd63c71bced7c4bbb8287e43f784bb636f6a0faa0dcb8e3d0b2a')}
run_id = int(os.environ['GITHUB_RUN_ID'])
out = temp/f'R335-095-paired-ui-{run_id}.json'
receipt = dict(sourceCommit=commits[95][0],apkSha256=commits[95][1],runId=run_id,scope='TWO_NATIVE_DEFAULT_REPORTS_SAME_DISPOSABLE_ANDROID14_RETAINED_UID',attempts=[],verifiedPair=False)
def persist():
    out.write_text(json.dumps(receipt,indent=2)+'\n')
def sha(data):
    return hashlib.sha256(data).hexdigest()
persist()
for v,(commit,digest) in commits.items():
    root = temp/('paired-report-%03d'%v)
    root.mkdir()
    (root/'material').symlink_to(temp/'material',target_is_directory=True)
    (root/'tools').symlink_to(temp/'tools',target_is_directory=True)
    (root/'report_ui_navigation.py').symlink_to(compact.parent/'report_ui_navigation.py')
    # Reuse the exact registered UI/readback driver. Only the installation history is changed.
    template = compact.read_text()
    start = template.index(" check(not f.adb('shell','pm','list','packages',pkg).strip()")
    stop = template.index(" f.adb('shell','pm','grant',pkg", start)
    if v == 94:
        setup = template[start:stop].replace('577311095,n.source','577311094,n.source').replace("'current.txt',577311095","'current.txt',577311094")
    else:
        setup = """ olduid,prior=f.installed(f.EVID/'before.txt',577311094)
 check(prior=='e7d36e074852ba330b7786460b48569271a9e46abb1a75c40a6824aa44c94aca','PRIOR_RELEASE')
 current=n.observe.get_apk('B08',577311095,n.source);check(f.fingerprint(current)==n.apk,'SIGNED_IDENTITY')
 f.adb('install','-r',str(current))
 check(f.installed(f.EVID/'current.txt',577311095)==(olduid,n.apk),'GUEST_VERSION')
"""
    setup += " record['androidUid']=olduid\n"
    modified = (template[:start]+setup+template[stop:]).replace("n.source='"+commits[95][0]+"'","n.source='"+commit+"'")
    ast.parse(modified)
    driver=root/'run_report.py';driver.write_text(modified)
    env=dict(os.environ,RUNNER_TEMP=str(root),REPAIRED_APK_SHA=digest)
    with (root/'private.log').open('wb') as log:
        process=subprocess.run([sys.executable,'-B',driver],env=env,stdout=log,stderr=log,timeout=2700)
    original=json.loads((root/f'R335-095-compact-{run_id}.json').read_bytes())
    assert original['sourceCommit']==commit and original['apkSha256']==digest
    item={'version':577311000+v,'sourceCommit':commit,'apkSha256':digest,'exit':process.returncode,'driverSha256':sha(driver.read_bytes()),'stage':original['stage'],'originalReceiptSha256':sha((root/f'R335-095-compact-{run_id}.json').read_bytes())}
    receipt['attempts'].append(item);persist()
    if original['stage']!='VERIFIED':continue
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
            assert all(re.fullmatch('[A-Z][A-Z0-9_|-]{1,85}',s) for s in states[key])
        item.update(uid=original['androidUid'],jobId=original['jobId'],archiveBytes=original['archiveBytes'],archiveSha256=original['archiveSha256'],safSha256=original['safExternalZipSha256'],reportSha256=sha(raw),semanticSha256=sha(z.read('R31_SEMANTIC_RECEIPT.json')),states=states,actualSystemPicker=True,oracle=original['originalVerifier'])
    persist()
if len(receipt['attempts'])==2 and all(x.get('actualSystemPicker') for x in receipt['attempts']):
    first,last=receipt['attempts'];assert first['uid']==last['uid'] and first['jobId']!=last['jobId']
    repo_dir=project/'app-source'
    subprocess.run(['git','-C',repo_dir,'fetch','--quiet','--depth=1','--filter=blob:none','origin',commits[94][0]],check=True)
    changes=[]
    for filename in ['ResearchMigrationRegistryR952.java','SchemaDomainCoordinatorR926.java']:
        paths=[p.relative_to(repo_dir).as_posix() for p in (repo_dir/'app/src').rglob(filename)]
        assert len(paths)==1;path=paths[0]
        images=[sha(subprocess.check_output(['git','-C',repo_dir,'show',commit+':'+path])) for commit,_ in commits.values()]
        assert images[0]!=images[1];changes.append(dict(path=path,beforeSha256=images[0],afterSha256=images[1]))
    receipt.update(verifiedPair=True,gitChanges=changes,casesNowExecuted=[cid for cid,state in first['states'].items() if state[0]=='READ_ERROR' and last['states'][cid][0]=='EXECUTED'],matchingCaseIds=sorted(first['states'])==sorted(last['states']))
    persist()
