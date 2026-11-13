#!/usr/bin/env python3
"""Package already verified exact release bytes and public engineering receipts only."""
import argparse
import hashlib
import importlib.util
import json
import shutil
import subprocess
import zipfile
from pathlib import Path

CERT = 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
THRESHOLDS = 'c577c58f63879ce8c9090d5b9d7494108db8f0d317b31d22639b4888b47bc08c'


def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()


def one(root, name):
    paths = list(root.rglob(name))
    assert len(paths) == 1, 'EXACT_PUBLIC_RECEIPT_REQUIRED:'+name+':'+str(len(paths))
    return paths[0]


def load(root, name):
    return json.loads(one(root, name).read_text())


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--request', type=Path, required=True)
    p.add_argument('--stages', type=Path, required=True)
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--live', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    request = json.loads(a.request.read_text())
    source = request['sourceCommit']
    assert subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip() == source, 'PACKAGING_SOURCE_MISMATCH'
    assert sha(Path('construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json')) == THRESHOLDS, 'ORIGINAL_THRESHOLDS_CHANGED'
    built = a.stages/'r31v-android-build'
    assert one(built,'source-commit.txt').read_text().strip() == source, 'APK_SOURCE_MISMATCH'
    host_phone = load(built,'HOST_PHONE_REPORT.json')
    assert host_phone['sourceHead']==source and host_phone['status']=='PASS' and host_phone['snapshotBytes']==679215104 and host_phone['fixture']['pages']==165824, 'CURRENT_LARGE_REPORT_BACKUP_NOT_PASS'
    summary=load(a.stages/'r31v-contracts','BUILD_SUMMARY.json')
    assert summary['source_head']==source and str(summary['runId'])==str(request['verificationRunId']) and summary['state']=='PASS_ENGINEERING_ONLY' and summary['exitCode']==0 and summary['inputsSha256']==summary['inputsAfterSha256'], 'CURRENT_FULL_ENGINEERING_INVOCATION_MISMATCH'
    assert summary['unit']['tests']>=648 and all(summary['unit'][k]==0 for k in ['failures','errors','skipped']), 'ORIGINAL_CURRENT_UNIT_TESTS_DROPPED_OR_FAILED'
    required_hosts={r['id'] for r in json.loads(Path('construction/r31/HOST_SUITE.json').read_text())['host'] if not r.get('retired')}
    assert len(required_hosts)>=72 and {r['id'] for r in summary['host'] if r['status']=='PASS'}==required_hosts, 'FULL_CURRENT_HOST_SUITE_NOT_PASS'
    verification = load(a.stages/'r31-verification-receipt','R31_VERIFICATION_RECEIPT.json')
    assert verification['overall'] == 'PASS' and all(v['status']=='PASS' for v in verification['stages'].values()), 'FINAL_CURRENT_ENGINEERING_STAGES_NOT_PASS'
    acceptance = load(a.stages/'r31-verification-receipt','ACCEPTANCE_RESULTS.json')
    assert acceptance['identity']['source_head'] == source and acceptance['identity']['state'] == 'PASS_ENGINEERING_ONLY', 'FINAL_CURRENT_INVOCATION_NOT_PASS'
    assert len(acceptance['items']) == 222 and len(acceptance['cards']) == 58, 'ORIGINAL_CARD_ITEM_SET_CHANGED'
    assert sum(r['level']=='LOCAL' for r in acceptance['items']) == 189
    assert all(r['engineeringBindingResult']=='PASS' for r in acceptance['items'] if r['level']=='LOCAL'), 'LOCAL_ITEM_NOT_PASS'
    priority = load(a.stages/'r31v-priority-report-runtime','DELIVERY_REGRESSION.json')
    assert priority['status']=='PASS_ANDROID_EMULATOR' and priority['tests']==7, 'EXACT_RELEASE_PRIORITY_PROTOCOLS_NOT_PASS'
    assert priority['installedReleaseSha256']==request['apkSha256'], 'PRIORITY_RELEASE_BINDING_MISMATCH'
    report = load(a.report,'ui-report-acceptance.json')
    report_result = load(a.report,'REPORT_ONLY_RESULT.json')
    semantics = load(a.report,'safe-report-semantics.json')
    failure_menu = load(a.report,'failure-ui-acceptance.json')
    assert report_result['result']=='PASS' and report_result['sourceCommit']==source and report_result['apkSha256']==request['apkSha256'], 'FINAL_RELEASE_REPORT_NOT_PASS'
    assert report['sourceCommit']==source and report['evidenceComplete'] and report['checksComplete'], 'COMPLETE_REPORT_SEMANTICS_NOT_EXECUTED'
    assert report['saf']['state']=='VERIFIED_READBACK' and report['saf']['actualSystemPicker'] and report['saf']['sameAsPublishedZip'], 'SYSTEM_FILE_PICKER_READBACK_NOT_VERIFIED'
    assert report['saf']['externalFileSha256']==report['zipSha256'], 'SAVED_REPORT_HASH_DIFFERS'
    assert report['sameSignedApkReinstallPreservesReport'] and report['overlayMarkerPreserved'], 'RELEASE_OVERLAY_RETENTION_NOT_OBSERVED'
    assert semantics['catalogResults']==96 and semantics['checksComplete'] and not semantics['incompleteChecks'], '96_CHECKS_NOT_COMPLETELY_EXECUTED'
    assert failure_menu['result']=='PASS', 'BOTH_FAILURE_RECOVERY_ENTRANCES_NOT_PASS'
    live = {'sourceCommit':source,'apkSha256':priority['installedReleaseSha256'],'tests':priority['tests'],'status':priority['status'],'scope':'EXACT_RELEASE_SEVEN_PROTOCOLS_INCLUDING_ACTUAL_FUTURE_FREEZE_AND_FOLLOWING_SETTLEMENT; NOT_NATURAL_QUALITY'}
    tables = load(a.live,'TABLE_UI_READERS.json')
    transport = load(a.stages/'r31v-priority-report-runtime','ANDROID_PROTOCOL_VERIFICATION.json')
    assert transport['passed'] and not transport['problems'] and len(transport['completedTests'])==7 and len(transport['markers'])==7, 'CURRENT_SEVEN_TEST_LIFECYCLE_MARKERS_MISSING'
    assert transport['protocolSha256']==priority['protocolSha256'], 'PRIORITY_PROTOCOL_RECEIPT_DIGEST_MISMATCH'
    live['protocolSha256']=transport['protocolSha256']
    selected=[]
    for api in [29,35]:
        r=load(a.stages/('r31-029-critical-targeted-api-'+str(api)),'SELECTED_ANDROID_RECEIPT.json')
        assert r['sourceCommit']==source and r['apkSha256']==request['apkSha256'] and r['status']=='PASS_TARGETED_ANDROID_EMULATOR' and r['expectedTests']==5 and len(r['completedTests'])==5 and 'fullReportReachesPublishedZipAndProviderReadback' in r['completedTests'], 'CURRENT_READY_UI_LEASE_AND_RESTORE_CHECK_NOT_PASS'
        selected.append(r)
    assert tables['sourceCommit']==source and tables['apkSha256']==request['apkSha256'] and tables['state']=='OBSERVED_16_ACTUAL_READERS'
    assert len(tables['reads'])==16 and {r['id'] for r in tables['reads']}=={'T%02d'%n for n in range(1,17)}
    assert all(r['actualStatus'] in ['READ_OK','EMPTY_NO_ROWS'] and r['readOnly'] for r in tables['reads']), 'SIXTEEN_ACTUAL_TABLE_READERS_NOT_PASS'
    android = []
    for api in [29,34,35]:
        folder = a.stages/('r31v-emu-'+str(api))
        joint, runtime, research, gallery, upgrade = [load(folder, name) for name in ['JOINT-ANDROID-RECEIPT.json','matrix.json','research-fault-matrix.json','repair-chart-acceptance.json','EMU-UPGRADE-RECEIPT.json']]
        assert joint['sourceCommit']==source and all(row['status']=='PASS_NAMED_ENGINEERING_PROTOCOLS' for row in joint['groups']), 'CURRENT_ANDROID_GROUP_FAILURE:'+str(api)
        assert len(joint['groups'])==(8 if api==35 else 7), 'ORIGINAL_ANDROID_GROUPS_MISSING'
        assert runtime['source']==source and len(runtime['cells'])==14 and all(row['mechanics']=='ANDROID_EMULATOR_MECHANICS_PASS' for row in runtime['cells']), 'ORIGINAL_RUNTIME_FAULT_MATRIX_NOT_PASS'
        assert research['sourceCommit']==source and research['result']=='PASS' and len(research['cells'])==12 and all(row['pass'] for row in research['cells']), 'ORIGINAL_RESEARCH_FAULT_MATRIX_NOT_PASS'
        assert gallery['sourceCommit']==source and gallery['registeredObjects']==47 and gallery['screenshots']==188, '47_ACTUAL_CHART_OBJECTS_NOT_VERIFIED'
        assert upgrade['status']=='OBSERVED_PASS', 'CURRENT_OVERLAY_RECOVERY_NOT_PASS'
        android.append({'api':api,'namedProtocols':sum(row['observedTests'] for row in joint['groups']),'originalRuntimeFaultCells':14,'originalResearchFaultCells':12,'chartObjects':47,'chartScreenshots':188,'upgrade':upgrade['status']})
    spec = importlib.util.spec_from_file_location('release_package','tools/r31/package_release.py')
    package = importlib.util.module_from_spec(spec);spec.loader.exec_module(package)
    apk = one(built,'*-release.apk'); stem=apk.stem
    facts = package.verify_apk(apk,one(built,'badging-'+stem+'.txt'),one(built,'signature-'+stem+'.txt'),one(built,'SHA256SUMS.txt'),package.identity())
    assert facts['sha256']==request['apkSha256'] and facts['signerCertSha256']==CERT and facts['versionCode']==request['versionCode'] and facts['versionCode']>577311026
    out = a.out;out.mkdir(parents=True,exist_ok=False)
    shutil.copyfile(apk,out/request['apkName'])
    shutil.copyfile(one(built,'signature-'+stem+'.txt'),out/'PUBLIC_CERTIFICATE.txt')
    shutil.copyfile(one(built,'badging-'+stem+'.txt'),out/'APK_BADGING.txt')
    for root,names in [(a.stages/'r31-verification-receipt',['R31_VERIFICATION_RECEIPT.json','ACCEPTANCE_RESULTS.json','ACCEPTANCE_RESULTS.csv']), (a.report,['ui-report-acceptance.json','REPORT_ONLY_RESULT.json','safe-report-semantics.json','failure-ui-acceptance.json']), (a.live,['TABLE_UI_READERS.json']), (a.stages/'r31v-priority-report-runtime',['DELIVERY_REGRESSION.json','ANDROID_PROTOCOL_VERIFICATION.json'])]:
        for name in names:shutil.copyfile(one(root,name),out/name)
    shutil.copyfile(a.request,out/'FINAL_PUBLISH_REQUEST.json')
    orchestration_files=['.github/workflows/r31-build-r27.yml','.github/r31-critical-targeted.sh','.github/r31-final-matrix.sh','.github/r31-normal-ui-start.py','.github/r31-final-current-android.py']
    subprocess.run(['git','-C','../harness','diff','--exit-code',request['orchestrationCommit'],'--',*orchestration_files],check=True)
    subprocess.run(['git','-C','../harness','archive','--format=zip','--output='+str((out/'CI_ORCHESTRATION.zip').resolve()),request['orchestrationCommit'],*orchestration_files],check=True)

    engineering_public={k:summary[k] for k in ['source_head','source_tree','runId','invocationId','inputsSha256','inputsAfterSha256','state','exitCode','unit']}
    engineering_public['host']=[{'id':r['id'],'status':r['status']} for r in summary['host']]
    (out/'CURRENT_ENGINEERING_SUMMARY.json').write_text(json.dumps(engineering_public,indent=2)+'\n')
    (out/'SELECTED_ANDROID_RECEIPTS.json').write_text(json.dumps(selected,indent=2)+'\n')
    (out/'HOST_PHONE_REPORT.json').write_text(json.dumps(host_phone,indent=2)+'\n')
    archive = out/'SOURCE_AND_ALL_R31_PLANS.zip'
    paths=['app/src','app/build.gradle','app/proguard-rules.pro','build.gradle','settings.gradle','gradle.properties','gradle','gradlew','gradlew.bat','scripts','tools','schema','release','.github','verification/r932','CURRENT_FINAL_PLAN.md','construction/STARCORE_R31_FINAL_20261104','construction/archives/STARCORE_R31_FINAL_20261104','construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112','construction/repairs/R31_PHONE_REPORT_20261113','construction/r31']
    subprocess.run(['git','archive','--format=zip','--output='+str(archive.resolve()),source,*paths],check=True)
    mobile = out/'SOURCE_AND_ASSETS.zip'; excluded=[]
    with zipfile.ZipFile(archive) as source_zip,zipfile.ZipFile(mobile,'w',zipfile.ZIP_DEFLATED) as target:
        for entry in source_zip.infolist():
            if entry.filename.startswith('tools/strategy_su2/fixtures/'):
                excluded.append(entry.filename);continue
            with source_zip.open(entry) as reader,target.open(entry.filename,'w') as writer:shutil.copyfileobj(reader,writer,1024*1024)
    archives={'sourceCommit':source,'fullSourceAndAllR31Plans':{'file':archive.name,'bytes':archive.stat().st_size,'sha256':sha(archive)},'mobileSource':{'file':mobile.name,'bytes':mobile.stat().st_size,'sha256':sha(mobile),'omittedOnlyOldSu2ReferenceFixtureFiles':excluded,'omittedFilesIncludedInFullArchive':True},'allApplicationSourcesAssetsAndOriginalPlansInBoth':True,'completeRepositoryIncludingAllVerificationData':request['completeRepositoryArchive']}
    (out/'SOURCE_ARCHIVES.json').write_text(json.dumps(archives,ensure_ascii=False,indent=2)+'\n')
    for row in archives.values():
        if isinstance(row,dict) and 'file' in row:
            with zipfile.ZipFile(out/row['file']) as z:assert z.testzip() is None,'SOURCE_ARCHIVE_CRC_FAILURE'
    receipt={'format':'R31_FINAL_CURRENT_ENGINEERING_DELIVERY_V1','sourceCommit':source,'applicationBranch':'construction/r31-final-20261104','apk':dict(facts,deliveredAs=request['apkName']),'buildRunId':request['buildRunId'],'verificationRunId':request['verificationRunId'],'verificationRunSource':source,'buildArtifactDigest':request['buildArtifactDigest'],'reportRunId':request['reportRunId'],'liveRunId':request['liveRunId'],'originalThresholdsSha256':THRESHOLDS,'sameSignerHigherVersionOverlayCompatible':True,'cards':58,'repairCards':20,'currentItems':222,'localBindingsPassed':189,'unitTests':summary['unit'],'hostEntriesPassed':len(required_hosts),'selectedAndroid':selected,'currentLargeSyntheticReport':host_phone,'completeRepositoryArchive':request['completeRepositoryArchive'],'android':android,'exactSignedLiveChain':live,'sixteenActualReaders':tables,'actualExport':report,'reportObjects':{'chapters':12,'metrics':44,'checks':96,'charts':47,'matrices':20,'logicalTables':16,'capabilities':82,'originalR26R27Attachments':True},'physicalDevice':'NOT_RUN','originalHandsetDatabase':'NOT_RUN','24h':'NOT_RUN','72h':'NOT_RUN','naturalQuality':'NOT_PROVEN','emptyResearchRowsAreUtilityProof':False,'inheritedQualification':False,'engineeringState':'CURRENT_LOCAL_AND_EMULATOR_CHECKS_PASS','wholeProductNaturalQuality':'NOT_INFERRED_FROM_ENGINEERING_CHECKS','privateDatabaseRecordsPublished':False}
    (out/'R31_FINAL_DELIVERY.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    (out/'README.txt').write_text('R31 累计工程交付。APK 同包名、原机原证书、更高版本，可覆盖安装。\n收据分别列出本轮工程、模拟器、报告保存和真实页面读取结果。原机旧库、24/72小时和自然质量仍待验。空表不代表算法有效，不继承历史资格。完整仓库ZIP地址和逐文件Git对象核对结果见COMPLETE_REPOSITORY_SOURCE.txt与SOURCE_ARCHIVES.json。\nSOURCE_AND_ASSETS.zip 包含全部应用源码/资产及完整 R31 原方案；旧 SU2 数值参考样本在单独完整源码包 SOURCE_AND_ALL_R31_PLANS.zip 中一并提供。\n')
    (out/'COMPLETE_REPOSITORY_SOURCE.txt').write_text(request['completeRepositoryArchive']['url']+'\nSHA-256 '+request['completeRepositoryArchive']['sha256']+'\nIncludes every tracked source, build input, fixture, verification record and original plan from the frozen source commit.\n')
    members=sorted(p for p in out.iterdir() if p.is_file() and p!=archive)
    (out/'SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in members))
    bundle=out/request['zipName']
    with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED) as z:
        for member in members+[out/'SHA256SUMS.txt']:z.write(member,member.name)
    with zipfile.ZipFile(bundle) as z:assert z.testzip() is None,'FINAL_DELIVERY_ZIP_CRC_FAILURE'
    assert bundle.stat().st_size<100*1024*1024 and archive.stat().st_size<100*1024*1024,'DIRECT_REPOSITORY_FILE_SIZE_LIMIT'
    (out/'PACKAGE_SHA256.txt').write_text(sha(bundle)+'  '+bundle.name+'\n'+sha(archive)+'  '+archive.name+'\n')
    print(json.dumps({'sourceCommit':source,'apkSha256':facts['sha256'],'zipSha256':sha(bundle),'zipBytes':bundle.stat().st_size,'fullSourceZipBytes':archive.stat().st_size,'exportBytes':report['zipBytes'],'exportSha256':report['zipSha256']}))


if __name__=='__main__':main()
