#!/usr/bin/env python3
"""Package an already accepted report-only release; never infer whole-product health."""
import argparse
import hashlib
import json
import re
import shutil
import subprocess
import zipfile
from pathlib import Path

from package_release import verify_apk


def one(root, name):
    matches = list(root.rglob(name))
    if len(matches) != 1:
        raise ValueError('EXPECTED_ONE_FILE:' + name)
    return matches[0]


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def cumulative_results(directory, request, source):
    """Require the original full current invocation, including the real Android jobs."""
    assert directory is not None, 'CUMULATIVE_VERIFICATION_REQUIRED'
    acceptance = one(directory, 'ACCEPTANCE_RESULTS.json')
    document = json.loads(acceptance.read_text())
    identity = document['identity']
    assert identity['source_head'] == source, 'CUMULATIVE_SOURCE_MISMATCH'
    assert str(identity['runId']) == str(request['runId']), 'CUMULATIVE_RUN_MISMATCH'
    assert identity['state'] == 'PASS_ENGINEERING_ONLY' and identity['exitCode'] == 0, 'CURRENT_ENGINEERING_FAILURE'
    assert identity['inputsSha256'] == identity['inputsAfterSha256'], 'CUMULATIVE_INPUTS_CHANGED'
    assert re.fullmatch('[0-9a-f]{64}', identity['inputsSha256']) and identity['invocationId'], 'CUMULATIVE_INVOCATION_REQUIRED'
    assert len(document['items']) == 222 and len(document['cards']) == 58, 'CUMULATIVE_CARD_OR_ITEM_SET'
    assert document['itemCounts'].get('PASS') == 189, 'ORIGINAL_LOCAL_ACCEPTANCE_NOT_PASSED'
    verification_path = one(directory, 'R31_VERIFICATION_RECEIPT.json')
    verification = json.loads(verification_path.read_text())
    assert verification['format'] == 'R31_VERIFICATION_RECEIPT_V1' and verification['overall'] == 'PASS', 'CUMULATIVE_VERIFICATION_NOT_PASS'
    for name in ['baseline', 'contracts', 'android_build', 'instrumentation', 'priority_report_runtime']:
        assert verification['stages'].get(name, {}).get('status') == 'PASS', 'CURRENT_STAGE_NOT_PASS:' + name
        assert verification['stages'][name].get('jobResult') == 'success', 'CURRENT_STAGE_DID_NOT_EXECUTE:' + name
    files = [verification_path, acceptance, one(directory, 'ACCEPTANCE_RESULTS.csv')]
    summary = {'sourceCommit': source, 'runId': int(request['runId']), 'cards': 58, 'items': 222,
               'originalLocalAcceptancePassed': 189, 'currentStages': {name: row['status'] for name, row in verification['stages'].items()},
               'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN'}
    return summary, files


def package(request_path, built, reports, out, tools, verification=None):
    request = json.loads(request_path.read_text())
    source = request['sourceCommit']
    if not re.fullmatch(r'[0-9a-f]{40}', source):
        raise ValueError('INVALID_SOURCE')
    out.mkdir(parents=True, exist_ok=False)
    result = json.loads(one(reports, 'REPORT_ONLY_RESULT.json').read_text())
    ui_path = one(reports, 'ui-report-acceptance.json')
    ui = json.loads(ui_path.read_text())
    semantics_path = one(reports, 'safe-report-semantics.json')
    semantics = json.loads(semantics_path.read_text())
    for record in [result, ui, semantics]:
        assert record['sourceCommit'] == source, 'REPORT_SOURCE_MISMATCH'
    assert result['result'] == ui['result'] == 'PASS', 'REPORT_NOT_ACCEPTED'
    assert result['probeExit'] == 0, 'REPORT_PROBE_FAILED'
    assert int(result['runId']) == int(request.get('reportRunId', request['runId'])), 'REPORT_RUN_MISMATCH'
    assert result['apkSha256'] == request['sha256'], 'REPORT_APK_MISMATCH'
    assert result['scope'] == 'REPORT_GENERATION_COMPLETENESS_REAL_UI_SAF_ONLY'
    assert result['sameSignedApkReinstallPreservesReport'] is True
    assert ui['sameSignedApkReinstallPreservesReport'] is True
    assert ui['evidenceComplete'] and ui['checksComplete'], 'REPORT_INCOMPLETE'
    assert semantics['evidenceComplete'] and semantics['checksComplete']
    assert semantics['catalogResults'] == 96, 'REPORT_CHECK_SET'
    assert not semantics['incompleteChecks'] and not semantics['catalogErrors']
    assert ui['zipBytes'] > 0 and re.fullmatch(r'[0-9a-f]{64}', ui['zipSha256'])
    phone_host = phone_failure = None
    if int(request['versionCode']) >= 577311026:
        phone_host = json.loads(one(built, 'HOST_PHONE_REPORT.json').read_text())
        phone_failure = json.loads(one(reports, 'failure-ui-acceptance.json').read_text())
        assert phone_host['sourceHead'] == phone_failure['sourceCommit'] == source, 'PHONE_REPAIR_SOURCE_MISMATCH'
        assert phone_host['status'] == phone_failure['result'] == 'PASS', 'PHONE_REPAIR_NOT_ACCEPTED'
        assert phone_host['fixture']['pages'] == 165824 and phone_host['snapshotBytes'] == 679215104
        assert result['failureUiExit'] == 0
        if 'overlayMarkerPreserved' in result:
            assert result['overlayMarkerPreserved'] is True and result['overlayFromVersionCode'] == 577311025
        else:
            assert result['025To026OverlayMarkerPreserved'] is True
        assert phone_failure['existingEvidenceBytesPreserved'] is True and phone_failure['newDatabaseBackup'] is False
        assert phone_failure['failedFullReportJobDirectoriesAbsent'] is True
        assert len(phone_failure['entrances']) == 2 and all(v['correctRequest'] and v['ackReason'] and v['originalFailure'] and v['credentialsMasked'] and v['recoveryShare'] for v in phone_failure['entrances'])
        if request.get('validationSourceCommit'):
            assert result['validationSourceCommit'] == request['validationSourceCommit'], 'VALIDATION_SOURCE_MISMATCH'
    saf = ui['saf']
    assert saf['state'] == 'VERIFIED_READBACK' and saf['actualSystemPicker'] is True
    assert saf['sameAsPublishedZip'] is True
    assert saf['externalFileSha256'] == ui['zipSha256'] and saf['bytes'] == ui['zipBytes']
    assert one(built, 'source-commit.txt').read_text().strip() == source
    apk = one(built, request['builtApk'])
    assert digest(apk) == request['sha256'], 'SIGNED_APK_BYTES_MISMATCH'
    signature = out / 'public-certificate.txt'
    badging = out / 'badging.txt'
    signature.write_bytes(subprocess.check_output([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)]))
    print(signature.read_text(), flush=True)
    badging.write_bytes(subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)]))
    ident = {'applicationId': 'com.starcore.research.r13', 'versionCode': str(request['versionCode']), 'versionName': request['versionName']}
    facts = verify_apk(apk, badging, signature, one(built, 'SHA256SUMS.txt'), ident)
    assert facts['signerCertSha256'] == request['signerCertSha256'], 'ORIGINAL_DEVICE_SIGNER_MISMATCH'
    assert facts['versionCode'] > 577311020, 'OVERLAY_VERSION_NOT_HIGHER'
    delivered = out / request['apk']
    shutil.copyfile(apk, delivered)
    for path in [ui_path, semantics_path, one(reports, 'REPORT_ONLY_RESULT.json')]:
        shutil.copyfile(path, out / path.name)
    if phone_host is not None:
        for root, name in [(built, 'HOST_PHONE_REPORT.json'), (reports, 'failure-ui-acceptance.json')]:
            shutil.copyfile(one(root, name), out / name)
    engineering = None
    if request.get('scope') == 'R31_CUMULATIVE_ENGINEERING_DELIVERY':
        engineering, evidence = cumulative_results(verification, request, source)
        for path in evidence:
            shutil.copyfile(path, out / path.name)
    archive = out / 'SOURCE_AND_ASSETS.zip'
    source_paths = ['app/src', 'app/build.gradle', 'app/proguard-rules.pro', 'build.gradle',
                    'settings.gradle', 'gradle.properties', 'gradle', 'gradlew', 'gradlew.bat',
                    'scripts', 'tools/r31', 'tools/r312', 'tools/r30-repair', 'schema', 'release',
                    'CURRENT_FINAL_PLAN.md', 'construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112',
                    'construction/repairs/R31_PHONE_REPORT_20261113']
    subprocess.run(['git', 'archive', '--format=zip', '--output=' + str(archive.resolve()), source, *source_paths], check=True)
    (out / 'SOURCE_REFERENCE.json').write_text(json.dumps({
        'sourceCommit': source, 'repository': 'https://github.com/gtt46461-star/starcore-public-build/tree/' + source,
        'archiveScope': 'CURRENT_APPLICATION_ASSETS_AND_REPAIR_TOOLS; FULL_SEALED_PLANS_AND_HISTORICAL_AUDITS_AT_EXACT_REPOSITORY_SHA',
        'includedPaths': source_paths,
    }, indent=2) + '\n')
    if request.get('validationSourceCommit'):
        validation_source = request['validationSourceCommit']
        assert re.fullmatch(r'[0-9a-f]{40}', validation_source)
        subprocess.run(['git', 'diff', '--exit-code', source, validation_source, '--', 'app/src/main', 'app/build.gradle',
                        'release/release_manifest.json', 'release/release_identity.properties'], check=True)
        subprocess.run(['git', 'archive', '--format=zip', '--output=' + str((out / 'VALIDATION_AND_DELIVERY_SCRIPTS.zip').resolve()),
                        validation_source, 'tools/r31', '.github/workflows/r31-report-publish.yml'], check=True)
    receipt = {
        'format': 'R31_REPORT_ONLY_DELIVERY_1', 'sourceCommit': source,
        'buildAndReportRunId': int(request['runId']), 'apk': dict(facts, deliveredAs=delivered.name),
        'scope': 'REPORT_GENERATION_COMPLETENESS_AND_REAL_SAF_EXPORT_ONLY',
        'export': ui, 'sameSignerHigherVersionOverlayCompatible': True,
        'reportContentsVerifiedByExactReleaseHarness': {'chapters': 12, 'metrics': 44, 'checks': 96, 'charts': 47, 'matrices': 20, 'logicalTables': 16, 'capabilities': 82, 'R26R27OriginalAttachments': True},
        'businessAndAlgorithmHealth': 'DEFERRED_NOT_PASSED', 'wholeR31Closure': 'NOT_CLAIMED',
        'physicalDevice': 'NOT_RUN', 'originalDeviceDatabase': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN',
    }
    if phone_host is not None:
        receipt['phoneReportRepair'] = {'hostSyntheticLargeDatabase': phone_host, 'android14SyntheticAckHistory': phone_failure,
                                      'overlayMarkerPreserved': True, 'overlayFromVersionCode': 577311025,
                                      'overlayToVersionCode': int(request['versionCode']), 'physicalV2353A': 'NOT_RUN'}
        receipt['reportExecutionRunId'] = int(request.get('reportRunId', request['runId']))
        receipt['validationSourceCommit'] = request.get('validationSourceCommit', source)
    if engineering is not None:
        receipt['format'] = 'R31_CUMULATIVE_ENGINEERING_DELIVERY_1'
        receipt['scope'] = 'CUMULATIVE_R31_ENGINEERING_AND_EXACT_RELEASE_UI_SAF_EXPORT'
        receipt['engineeringAcceptance'] = engineering
        receipt.pop('buildAndReportRunId')
        receipt['buildRunId'] = int(request['runId'])
        receipt['businessAndAlgorithmHealth'] = 'REPORTS_ACTUAL_RUNTIME_DATA_UNKNOWN_AND_FAILURES;NATURAL_QUALITY_NOT_PROVEN'
    (out / 'R31_REPORT_DELIVERY.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    (out / 'README.txt').write_text('报告修复首包：安装同目录 APK 可覆盖 com.starcore.research.r13，保留数据。\n实际按钮→完整 ZIP→Android 系统选择器→保存回读、CRC/大小/SHA-256、同版签名覆盖安装通过（模拟器）。\n工程导出通过不代表全部业务健康。原机旧库、24/72 小时和自然质量待验；继续按原累计方案施工。\n')
    if engineering is not None:
        (out / 'README.txt').write_text('R31累计施工交付包：安装同目录APK，沿用com.starcore.research.r13及原签名，递增版本，可覆盖安装保留数据。\n附完整源码与资产、56+2卡/222项验收、同次工程与Android29/34/35结果、真实按钮及系统选择器保存回读收据。\n原机旧库、24/72小时及自然质量保持NOT_RUN/NOT_PROVEN；报告如实保留未知、缺数据和健康失败。\n')
    members = sorted(p for p in out.iterdir() if p.is_file())
    (out / 'SHA256SUMS.txt').write_text(''.join(digest(p) + '  ' + p.name + '\n' for p in members))
    bundle = out / request['zipName']
    with zipfile.ZipFile(bundle, 'w', zipfile.ZIP_DEFLATED) as z:
        for path in members + [out / 'SHA256SUMS.txt']:
            z.write(path, path.name)
    with zipfile.ZipFile(bundle) as z:
        assert z.testzip() is None, 'DELIVERY_ZIP_CRC'
        for path in members:
            assert hashlib.sha256(z.read(path.name)).hexdigest() == digest(path), 'DELIVERY_MEMBER_HASH'
    assert bundle.stat().st_size < 95 * 1024 * 1024, 'DIRECT_REPOSITORY_DELIVERY_SIZE_LIMIT'
    (out / 'PACKAGE_SHA256.txt').write_text(digest(bundle) + '  ' + bundle.name + '\n')
    print(json.dumps({'sourceCommit': source, 'apkSha256': digest(delivered), 'zipSha256': digest(bundle), 'exportSha256': ui['zipSha256'], 'exportBytes': ui['zipBytes']}))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--request', type=Path, required=True)
    p.add_argument('--built', type=Path, required=True)
    p.add_argument('--reports', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--build-tools', type=Path, required=True)
    p.add_argument('--verification', type=Path)
    a = p.parse_args()
    package(a.request, a.built, a.reports, a.out, a.build_tools, a.verification)
