#!/usr/bin/env python3
"""Prepare a final scorer's flat inner-ZIP reference from a phone single-ZIP export."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import tempfile
import zipfile

from stage_checked import pinned_module

LIMIT = 16 * 1024**3
BASE_APK = '8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
BASE_SOURCE = 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'


def need(ok, message):
    if not ok:
        raise ValueError(message)


def copy_member(stream, output, limit):
    size = 0
    digest = hashlib.sha256()
    for chunk in iter(lambda: stream.read(1024 * 1024), b''):
        size += len(chunk)
        need(size <= limit, 'EXPORT_LENGTH_LIMIT')
        digest.update(chunk)
        if output is not None:
            output.write(chunk)
    return size, digest.hexdigest()


def unwrap(outer, job, output):
    with zipfile.ZipFile(outer) as z:
        members = z.infolist()
        names = [row.filename for row in members]
        need(len(names) == len(set(names)) and len(names) <= 4, 'ONE_INDEX_AND_ONE_REPORT_REQUIRED')
        index_names = [name for name in names if PurePosixPath(name).name == 'index.json']
        need(len(index_names) == 1, 'SINGLE_INDEX_REQUIRED')
        parent = PurePosixPath(index_names[0]).parent
        need(parent in (PurePosixPath('.'), PurePosixPath('STARCORE-' + job)), 'INDEX_JOB_DIRECTORY')
        inner_name = str(parent / 'STARCORE_FULL_REPORT.zip')
        need({row.filename for row in members if not row.is_dir()} == {index_names[0], inner_name},
             'ONE_INDEX_AND_ONE_REPORT_REQUIRED')
        index_info = z.getinfo(index_names[0])
        need(index_info.file_size <= 2**20, 'INDEX_SIZE_LIMIT')
        index = json.loads(z.read(index_info))
        need(index.get('format') == 'SCREPORT_SINGLE_ZIP_R992' and index.get('jobId') == job
             and index.get('evidenceComplete') is True and index.get('checksComplete') is True
             and index.get('expectedVolumes') == index.get('exportedVolumes') == 1
             and index.get('omittedVolumes') == 0, 'INCOMPLETE_OR_WRONG_EXPORT')
        need(isinstance(index.get('files'), list) and len(index['files']) == 1, 'INDEX_FILES')
        entry = index['files'][0]
        info = z.getinfo(inner_name)
        need(entry.get('kind') == 'SINGLE_ZIP' and entry.get('name') == 'STARCORE_FULL_REPORT.zip'
             and 0 < info.file_size == entry.get('bytes') == index.get('totalVolumeBytes') <= LIMIT,
             'INDEX_BYTES_OR_TYPE')
        with z.open(info) as stream:
            count, digest = copy_member(stream, output, info.file_size)
        need(count == info.file_size and digest == entry.get('sha256'), 'INNER_BYTES_OR_HASH')
    output.flush()
    with zipfile.ZipFile(output.name) as inner:
        with inner.open('ARCHIVE_MANIFEST.jsonl') as stream:
            _, actual_manifest = copy_member(stream, None, LIMIT)
        need(index.get('manifestSha256') == actual_manifest, 'OUTER_INNER_MANIFEST_HASH')
    return digest


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', required=True, type=Path)
    parser.add_argument('--evidence-root', required=True, type=Path)
    parser.add_argument('--outer', required=True, type=Path)
    parser.add_argument('--job', required=True)
    parser.add_argument('--commit', required=True)
    parser.add_argument('--apk-sha', required=True)
    parser.add_argument('--profile', required=True, choices=('DIAGNOSTIC', 'FULL_RAW'))
    args = parser.parse_args()
    try:
        need(re.fullmatch('[0-9a-f]{64}', args.job) and re.fullmatch('[0-9a-f]{40}', args.commit)
             and re.fullmatch('[0-9a-f]{64}', args.apk_sha), 'CANDIDATE_IDENTITY')
        need(args.commit != BASE_SOURCE and args.apk_sha != BASE_APK, 'BASELINE_IS_HISTORICAL')
        validator = pinned_module(args.plan.resolve(), 'B01/scripts/verify_export_artifacts_r334.py',
                                  'verify_export', '25f75664f00b69693416bcc3766d7a1434ee01ba7816bb40eb3fb52932c2e034')
        root = args.evidence_root.resolve(strict=True)
        parent = root / 'verified-report-zips'
        parent.mkdir(exist_ok=True)
        need(not parent.is_symlink() and parent.is_dir(), 'OUTPUT_DIRECTORY')
        with tempfile.NamedTemporaryFile(dir=parent) as temp:
            sha = unwrap(args.outer, args.job, temp)
            os.fsync(temp.fileno())
            expected = {'jobId': args.job, 'profile': args.profile,
                        'sourceCommit': args.commit, 'apkSha256': args.apk_sha}
            facts = validator.validate_export(Path(temp.name), expected)
            need(facts['status'] == 'ARTIFACT_CONSISTENT', 'CURRENT_REPORT_REQUIRED')
            dest = parent / (sha + '.zip')
            try:
                os.link(temp.name, dest)
            except FileExistsError:
                need(not dest.is_symlink() and dest.is_file() and validator.sha256_file(dest) == sha,
                     'EXISTING_REPORT_DIFFERS')
        print(json.dumps({'path': dest.relative_to(root).as_posix(), 'sha256': sha,
                          'jobId': args.job, 'profile': args.profile}))
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as exc:
        parser.exit(2, str(exc) + '\n')


if __name__ == '__main__':
    main()
