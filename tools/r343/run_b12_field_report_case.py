#!/usr/bin/env python3
"""Bind the original-signed B12 Android SAF campaign to exact report-production evidence."""
import copy
import hashlib
import json
import os
from pathlib import Path
import sys
import zipfile

import run_native_field_case as native

ROOT = native.ROOT
CONTRACT = native.contract
BINDING = CONTRACT.binding
PLAN = ROOT / CONTRACT.PLAN_PATH
SELECTOR = 'com.ethk3.novaonepro.Field173B12ReportTest'
PROFILES = {
    'baseline.zip': 'COMPACT_DIAGNOSTIC',
    'candidate-1.zip': 'COMPACT_DIAGNOSTIC',
    'candidate-2.zip': 'FULL_RAW',
    'candidate-3.zip': 'COMPACT_DIAGNOSTIC',
}


def package_report(archive, record, source, apk_sha, code):
    manifest = json.loads(archive.read('source-manifest.json'))
    semantic = json.loads(archive.read('R31_SEMANTIC_RECEIPT.json'))
    report_body = archive.read('R30_REPORT.json')
    report = json.loads(report_body)
    installation = manifest['verifiedInstallationR341']
    scope = manifest['r341SqlObservationScope']
    CONTRACT.require(record['sourceCommit'] == semantic['sourceCommit'] == manifest['sourceCommit']
                     == installation['sourceCommit'] == scope['sourceCommit'] == source,
                     'FIELD173_B12_REPORT_SOURCE_MISMATCH')
    CONTRACT.require(installation['state'] == 'VERIFIED_INSTALLATION'
                     and installation['installedVersionCode'] == code
                     and manifest['apkSha256'] == installation['apkSha256'] == apk_sha
                     and manifest['signingCertificateSha256'] == installation['certificateSha256']
                     == 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7',
                     'FIELD173_B12_REPORT_SIGNED_APK_MISMATCH')
    CONTRACT.require(semantic['reportSha256'] == CONTRACT.digest(report_body)
                     and record['snapshotSha256'] == semantic['snapshotSha256']
                     == manifest['snapshotSha256'] == scope['sqlWatermark']
                     and scope['scopeKind'] == 'SQL_SNAPSHOT'
                     and scope['databaseIdentity'] not in ('', 'UNRESOLVED', 'LEGACY_UNSCOPED')
                     and scope['bootId'] not in ('', 'UNRESOLVED', 'LEGACY_UNSCOPED')
                     and scope['sampleWallAt'] > 0 and scope['sampleMonoAt'] >= 0,
                     'FIELD173_B12_REPORT_DATABASE_SCOPE_MISMATCH')
    CONTRACT.require(record['jobId'] == semantic['jobId'] and record['status'] == 'VERIFIED_ANDROID_SAF_ZIP'
                     and semantic['chapters'] == record['chapters'] == 12
                     and semantic['metrics'] == record['metrics'] == len(report['metricResults']) == 44
                     and len(semantic['checks']) == record['checks'] == 96 and len(archive.namelist()) == record['members'],
                     'FIELD173_B12_REPORT_INVENTORY_MISMATCH')
    return manifest, semantic, report


def observe_object_index(report):
    inventory = BINDING.read(PLAN / 'coverage/object_coverage_165.json')
    registered = {}
    for item in inventory['objects']:
        registered.setdefault(item['category'], set()).add(item['id'])
    index = report['objectIndex']
    result = {}
    for category, ids in registered.items():
        rows = index[category]['objects']
        CONTRACT.require(len(rows) == len(ids) and {row['id'] for row in rows} == ids,
                         'FIELD173_B12_OBJECT_IDS_DIFFER:' + category)
        result[category] = {row['id']: {
            'production': row.get('produced'),
            'consumption': row.get('consumed'),
            'lastStatus': row.get('evidenceState', row.get('status')),
            'reportedReason': row.get('notProducedReason') or row.get('qualityReason'),
            'reasonPresence': 'CALLED_OUT_IN_REPORT' if (row.get('notProducedReason') or row.get('qualityReason'))
            else 'NO_REASON_IN_THIS_REPORT',
        } for row in rows}
    return result


def inherited_ledgers(archive, manifest):
    result = {}
    for name in ('REQUIREMENTS_1832', 'CARDS_299', 'SUBITEMS'):
        key = f'diagnostics/construction-current-{name}.csv'
        declared = manifest['assets'][key]
        with archive.open(declared['member']) as data:
            digest = hashlib.file_digest(data, 'sha256').hexdigest()
        CONTRACT.require(digest == declared['sha256']
                         and archive.getinfo(declared['member']).file_size == declared['bytes'],
                         'FIELD173_B12_INHERITED_LEDGER_CHANGED:' + name)
        result[name] = declared
    return result


def inspect_report(record, candidate, field, artifact_dir, raw_reports):
    name = record['file']
    CONTRACT.require(name in PROFILES and record['profile'] == PROFILES[name], 'FIELD173_B12_REPORT_PROFILE_MISMATCH')
    report_zip = raw_reports / name
    encrypted = artifact_dir / record['encryptedOriginal']
    CONTRACT.require(encrypted.name == 'signed-report-' + Path(name).stem + '.zip.cms'
                     and BINDING.sha(encrypted) == record['encryptedSha256']
                     and BINDING.sha(report_zip) == record['sha256'] and report_zip.stat().st_size == record['bytes'],
                     'FIELD173_B12_RETRIEVED_REPORT_BYTES_CHANGED')
    source = CONTRACT.BASELINE if name == 'baseline.zip' else field['sourceCommit']
    apk_sha = CONTRACT.BASELINE_APK if name == 'baseline.zip' else BINDING.sha(candidate)
    code = CONTRACT.BASELINE_CODE if name == 'baseline.zip' else field['versionCode']
    with zipfile.ZipFile(report_zip) as archive:
        CONTRACT.require(archive.testzip() is None and len(set(archive.namelist())) == len(archive.namelist()),
                         'FIELD173_B12_ZIP_CORRUPT_OR_DUPLICATE')
        manifest, semantic, report = package_report(archive, record, source, apk_sha, code)
        ledgers = inherited_ledgers(archive, manifest)
    observed = {row['metricId']: {
        'status': row['status'], 'reason': row.get('missingReason'),
        'sourceReceipt': row.get('sourceReceiptKind'), 'role': row.get('role'),
        'sourceSnapshot': row.get('sourceSnapshot'), 'sampleCount': row.get('sampleCount'),
    } for row in report['metricResults']}
    expected = {item['id'] for item in BINDING.read(PLAN/'coverage/METRIC_ACTIONS_44.json')['metrics']}
    expected_checks = {item['id'] for item in BINDING.read(PLAN/'coverage/CHECK_ACTIONS_96.json')['checks']}
    CONTRACT.require(set(observed) == expected and set(semantic['checks']) == expected_checks,
                     'FIELD173_B12_EXACT_REPORT_ROW_IDS')
    overview = {'record': record, 'snapshotScope': manifest['r341SqlObservationScope'],
                'metricResults': observed, 'checkIds': sorted(semantic['checks']),
                'overallAcceptance': report['overallAcceptance'],
                'naturalQuality': report['naturalQuality'], 'physicalDevice': report['physicalDevice'],
                'objectObservations': observe_object_index(report), 'inheritedLedgers': ledgers}
    if name == 'candidate-2.zip':
        CONTRACT.require(manifest['rawEvidenceIncluded'] is True and semantic['rawEvidence'] is True
                         and record['snapshotQuickCheck'] == 'ok' and record['trainingRowsReopened'] is True,
                         'FIELD173_B12_FULL_REPORT_ORIGINALS_NOT_REOPENED')
    else:
        CONTRACT.require(record['rawEvidenceIncluded'] is False and semantic['rawEvidence'] is False,
                         'FIELD173_B12_COMPACT_REPORT_RAW_SCOPE')
    return overview


def verify_host(expected, candidate, serial, artifact_dir):
    field = CONTRACT.bound_field(ROOT)
    run_temp = Path(os.environ['RUNNER_TEMP'])
    build_receipt = BINDING.read(run_temp / 'build/CI_BUILD_RECEIPT.json')
    CONTRACT.require(build_receipt['fieldRepair173'] == field and build_receipt['sourceCommit'] == expected['sourceCommit']
                     and build_receipt['ciRun'] == expected['buildRun']
                     and build_receipt['gradleRelease'] == 'PASS' and build_receipt['apkSha256'] == expected['apkSha256']
                     and build_receipt['sourceUnchangedDuringBuild'] is True
                     and BINDING.sha(run_temp / 'build/gradle-build.log') == build_receipt['gradleLogSha256'],
                     'FIELD173_B12_BUILD_LOG_OR_HEAD_MISMATCH')
    exports = BINDING.read(artifact_dir / 'signed-report-readbacks.json')
    CONTRACT.require(exports['sourceCommit'] == expected['sourceCommit'] and exports['apkSha256'] == expected['apkSha256']
                     and exports['scope'] == 'SIGNED_RELEASE_API34_OFFLINE_UI_AND_SAF'
                     and exports['natural'] is False and exports['physicalDevice'] is False,
                     'FIELD173_B12_ANDROID_RAW_CAMPAIGN_REQUIRED')
    records = exports['reports']
    CONTRACT.require([record['file'] for record in records] == list(PROFILES)
                     and len({record['jobId'] for record in records}) == 4,
                     'FIELD173_B12_INDEPENDENT_BASELINE_AND_THREE_EXPORTS_REQUIRED')
    raw = run_temp / 'test-documents'
    observations = [inspect_report(record, candidate, field, artifact_dir, raw) for record in records]
    after = observations[1:]
    CONTRACT.require(len({item['snapshotScope']['databaseIdentity'] for item in observations}) == 1
                     and len({item['snapshotScope']['bootId'] for item in observations}) == 1
                     and [item['snapshotScope']['sampleMonoAt'] for item in after] ==
                     sorted(item['snapshotScope']['sampleMonoAt'] for item in after),
                     'FIELD173_B12_COVER_DATABASE_AND_CLOCK_SCOPE_MISMATCH')
    cover = BINDING.read(artifact_dir / 'cover-install.json')
    CONTRACT.require(cover['status'] == 'PASS' and cover['baselineSourceCommit'] == CONTRACT.BASELINE
                     and cover['before']['apkSha256'] == CONTRACT.BASELINE_APK
                     and cover['after']['apkSha256'] == expected['apkSha256']
                     and cover['after']['uid'] == cover['before']['uid'],
                     'FIELD173_B12_COVER_INSTALL_RECEIPT_MISMATCH')
    objects = BINDING.read(PLAN / 'coverage/object_coverage_165.json')
    CONTRACT.require(objects['count'] == len(objects['objects']) == 165, 'FIELD173_B12_UNREGISTERED_OBJECT_SCOPE')
    evidence = {'signedSafReadbacks': observations, 'coverInstall': cover, 'objectInventory': objects['categoryCounts'],
                'inheritedObservationFiles': {
                    item.name: BINDING.sha(item) for item in sorted(artifact_dir.glob('R341-*-host.json'))
                },
                'limits': {'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN',
                           'userScaleRecoveredDatabase': 'EXACT_173_DEVICE_SNAPSHOT_NOT_INCLUDED_IN_FIELD_REPORT',
                           'naturalQuality': 'NOT_PROVEN',
                           'objectInventory': 'PLAN_TRACEABILITY_WITH_PER_OBJECT_PRODUCTION_CLOSURE_SEPARATE'}}
    if expected['caseId'].endswith('-NEG'):
        changed = copy.deepcopy(records[-1])
        changed['sourceCommit'] = records[0]['sourceCommit']
        CONTRACT.require(changed['sourceCommit'] != records[-1]['sourceCommit'],
                         'FIELD173_B12_NEGATIVE_NEEDS_DISTINCT_ACTUAL_RELEASE_OWNER')
        try:
            inspect_report(changed, candidate, field, artifact_dir, raw)
        except ValueError as error:
            CONTRACT.require(str(error) == 'FIELD173_B12_REPORT_SOURCE_MISMATCH',
                             'FIELD173_B12_WRONG_NEGATIVE_FAILURE')
            evidence['mismatchedOriginalRejection'] = str(error)
        else:
            raise AssertionError('Mixed real signed capture identities were accepted')
    stem = 'field173-' + expected['caseId']
    return [native.write_original(artifact_dir / (stem + '-signed-saf-readback.json'), CONTRACT.canonical(evidence))]


if __name__ == '__main__':
    if len(sys.argv) != 2 or sys.argv[1] not in ('reportPositive', 'reportNegative', 'reportPreservation'):
        raise SystemExit('Supply one exact B12 instrumentation method')
    native.run(SELECTOR + '#' + sys.argv[1], host_verify=verify_host)
