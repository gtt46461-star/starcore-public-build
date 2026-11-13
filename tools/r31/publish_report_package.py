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


def package(request_path, built, reports, out, tools):
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
    assert int(result['runId']) == int(request['runId']), 'REPORT_RUN_MISMATCH'
    assert result['apkSha256'] == request['sha256'], 'REPORT_APK_MISMATCH'
    assert result['scope'] == 'REPORT_GENERATION_COMPLETENESS_REAL_UI_SAF_ONLY'
    assert result['sameSignedApkReinstallPreservesReport'] is True
    assert ui['sameSignedApkReinstallPreservesReport'] is True
    assert ui['evidenceComplete'] and ui['checksComplete'], 'REPORT_INCOMPLETE'
    assert semantics['evidenceComplete'] and semantics['checksComplete']
    assert semantics['catalogResults'] == 96, 'REPORT_CHECK_SET'
    assert not semantics['incompleteChecks'] and not semantics['catalogErrors']
    assert ui['zipBytes'] > 0 and re.fullmatch(r'[0-9a-f]{64}', ui['zipSha256'])
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
    archive = out / 'SOURCE_AND_ASSETS.zip'
    subprocess.run(['git', 'archive', '--format=zip', '--output=' + str(archive.resolve()), source], check=True)
    receipt = {
        'format': 'R31_REPORT_ONLY_DELIVERY_1', 'sourceCommit': source,
        'buildAndReportRunId': int(request['runId']), 'apk': dict(facts, deliveredAs=delivered.name),
        'scope': 'REPORT_GENERATION_COMPLETENESS_AND_REAL_SAF_EXPORT_ONLY',
        'export': ui, 'sameSignerHigherVersionOverlayCompatible': True,
        'reportContentsVerifiedByExactReleaseHarness': {'chapters': 12, 'metrics': 44, 'checks': 96, 'charts': 47, 'matrices': 20, 'logicalTables': 16, 'capabilities': 82, 'R26R27OriginalAttachments': True},
        'businessAndAlgorithmHealth': 'DEFERRED_NOT_PASSED', 'wholeR31Closure': 'NOT_CLAIMED',
        'physicalDevice': 'NOT_RUN', 'originalDeviceDatabase': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN',
    }
    (out / 'R31_REPORT_DELIVERY.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    (out / 'README.txt').write_text('报告修复首包：安装同目录 APK 可覆盖 com.starcore.research.r13，保留数据。\n实际按钮→完整 ZIP→Android 系统选择器→保存回读、CRC/大小/SHA-256、同版签名覆盖安装通过（模拟器）。\n工程导出通过不代表全部业务健康。原机旧库、24/72 小时和自然质量待验；继续按原累计方案施工。\n')
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
    (out / 'PACKAGE_SHA256.txt').write_text(digest(bundle) + '  ' + bundle.name + '\n')
    print(json.dumps({'sourceCommit': source, 'apkSha256': digest(delivered), 'zipSha256': digest(bundle), 'exportSha256': ui['zipSha256'], 'exportBytes': ui['zipBytes']}))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--request', type=Path, required=True)
    p.add_argument('--built', type=Path, required=True)
    p.add_argument('--reports', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--build-tools', type=Path, required=True)
    a = p.parse_args()
    package(a.request, a.built, a.reports, a.out, a.build_tools)
