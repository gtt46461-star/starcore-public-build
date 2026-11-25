#!/usr/bin/env python3
"""Gate publication on same-run candidate originals and verify the public bytes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path[:0] = [str(ROOT / 'tools/r33'), str(ROOT / 'tools/r341')]
import package_ci_candidate as package
from publish_delivery_manifest import collect
from verify_plan_binding import read, require, sha, write_json
from build_identity import BASELINE, BASELINE_CODE, PLAN_COMMIT, SEAL

REPOSITORY = 'gtt46461-star/starcore-public-build'


def archive_member_sha(archive, name):
    with archive.open(name) as content:
        return hashlib.file_digest(content, 'sha256').hexdigest()


def inspect_candidate(candidate, source, stage, run_id, sdk):
    report = read(candidate / 'DELIVERY.json')
    build = read(candidate / 'CI_BUILD_RECEIPT.json')
    identity = read(candidate / 'build-identity-r334.json')
    field = identity['r342']
    require(re.fullmatch(r'[0-9a-f]{40}', source) and re.fullmatch(r'B(?:0[1-9]|1[0-2])', stage)
            and run_id.isdecimal(), 'R342_PUBLICATION_CANDIDATE_REFERENCE_INVALID')
    require(field['sourceCommit'] == report['sourceCommit'] == build['sourceCommit'] == source
            and field['sourceTree'] == report['sourceTree'] == build['sourceTree'] == identity['source_tree']
            and field == report['r342'] and field['planCommit'] == build['r342PlanCommit'] == PLAN_COMMIT
            and field['planSha256'] == build['r342PlanManifestSha256'] == SEAL
            and field['batch'] == build['r342Batch'] == stage and report['ciRun'] == build['ciRun'] == run_id
            and field['sourceBaseline'] == BASELINE and field['versionCode'] > BASELINE_CODE
            and build['exitCode'] == build['wrapperExitCode'] == 0 and build['gradleRelease'] == 'PASS'
            and build['sourceCleanBeforeBuild'] is True and build['sourceClean'] is True
            and build['sourceUnchangedDuringBuild'] is True and ':app:assembleRelease' in build['command']
            and sha(candidate / 'gradle-build.log') == report['gradleLogSha256'] == build['gradleLogSha256'],
            'R342_PUBLICATION_BUILD_RECEIPT_MISMATCH')

    prefix = f"STARCORE-R342-{field['versionCode']}-{source[:12]}"
    apk = candidate / (prefix + '-release.apk')
    exact, _, _ = package.inspect(apk, sdk / 'aapt', sdk / 'apksigner')
    require(dict(exact, file=apk.name) == report['apk'] and exact['sha256'] == build['apkSha256']
            and exact['versionName'] == field['versionName'] and exact['package'] == package.PACKAGE,
            'R342_PUBLICATION_APK_MISMATCH')
    package.embedded(apk, identity)
    raw = candidate / 'engineering-originals.zip'
    require(sha(raw) == report['engineeringEvidenceAttachment']['sha256']
            and raw.stat().st_size == report['engineeringEvidenceAttachment']['bytes'],
            'R342_PUBLICATION_TEST_ATTACHMENT_MISMATCH')
    with zipfile.ZipFile(raw) as archive:
        evidence = json.loads(archive.read('batch-receipt.json'))
        require(evidence['sourceCommit'] == source and evidence['apkSha256'] == exact['sha256']
                and evidence['batch'] == stage and evidence['planSha256'] == SEAL and evidence['buildRun'] == run_id
                and evidence['status'] == 'ENGINEERING_CHECKS_PASSED'
                and archive.testzip() is None and len(set(archive.namelist())) == len(archive.namelist()),
                'R342_PUBLICATION_ENGINEERING_RECEIPT_INVALID')
        originals = evidence['originals']
        require(originals and set(archive.namelist()) == {*originals, 'batch-receipt.json'}
                and all(archive_member_sha(archive, name) == digest for name, digest in originals.items()),
                'R342_PUBLICATION_ENGINEERING_ORIGINALS_INVALID')

    source_zip = candidate / (prefix + '-source.zip')
    git_bundle = candidate / (prefix + '-source.bundle')
    require(report['sourceArchive']['file'] == source_zip.name and report['sourceArchive']['gitTree'] == field['sourceTree']
            and report['sourceArchive']['sha256'] == sha(source_zip)
            and report['gitBundle']['file'] == git_bundle.name and report['gitBundle']['sha256'] == sha(git_bundle),
            'R342_PUBLICATION_SOURCE_MISMATCH')
    total = candidate / (prefix + '-APK-SOURCE-EVIDENCE.zip')
    sums = read(candidate / 'ZIP_MEMBERS_SHA256.json')
    required = {apk.name, source_zip.name, git_bundle.name, raw.name, 'DELIVERY.json',
                'CI_BUILD_RECEIPT.json', 'build-identity-r334.json', 'gradle-build.log'}
    with zipfile.ZipFile(total) as archive:
        require(required <= sums.keys() and set(archive.namelist()) == {*sums, 'ZIP_MEMBERS_SHA256.json'}
                and len(archive.namelist()) == len(sums) + 1 and all(
                    archive_member_sha(archive, name) == digest == sha(candidate / name)
                    for name, digest in sums.items()) and json.loads(archive.read('ZIP_MEMBERS_SHA256.json')) == sums,
                'R342_PUBLICATION_TOTAL_CONTENT_MISMATCH')

    assets = [apk, total, source_zip, git_bundle, raw, *(candidate / name for name in (
        'DELIVERY.json', 'CI_BUILD_RECEIPT.json', 'ZIP_MEMBERS_SHA256.json',
        'build-identity-r334.json', 'SHA256SUMS.txt', 'gradle-build.log'))]
    url = f"https://github.com/{REPOSITORY}/releases/download/r34.2-{stage}-{source[:12]}"
    return {'series': 'R34.2', 'sourceCommit': source, 'planCommit': PLAN_COMMIT, 'planSha256': SEAL,
            'batch': stage, 'buildRun': run_id, 'versionCode': field['versionCode'],
            'artifacts': [{'filename': p.name, 'url': f'{url}/{p.name}', 'bytes': p.stat().st_size,
                           'sha256': sha(p)} for p in assets]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--sdk-tools', type=Path, required=True)
    parser.add_argument('--source', required=True)
    parser.add_argument('--stage', required=True)
    parser.add_argument('--index', type=Path, required=True)
    parser.add_argument('--received', type=Path)
    args = parser.parse_args()
    receipt = inspect_candidate(args.candidate, args.source, args.stage, os.environ['GITHUB_RUN_ID'], args.sdk_tools)
    if args.received:
        require(receipt == read(args.index), 'R342_PUBLICATION_INDEX_CHANGED')
        downloaded = collect(receipt, args.received, 'R34.2')
        reopened = inspect_candidate(args.received, args.source, args.stage, os.environ['GITHUB_RUN_ID'], args.sdk_tools)
        require(reopened == receipt, 'R342_PUBLICATION_READBACK_MISMATCH')
        write_json(args.received / 'DELIVERY_R342.json', downloaded)
    else:
        write_json(args.index, receipt)


if __name__ == '__main__':
    main()
