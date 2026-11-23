#!/usr/bin/env python3
"""Verify the *actual* STARCORE export ZIP schema, without claiming phone execution.

Uses only the Python standard library. Does not extract ZIP members. The adjacent
ID contract freezes the existing 12/44/96/165 registries for this revision.
An internally consistent ZIP is not proof that a physical device ran a test.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys
import zipfile


class InvalidEvidence(ValueError):
    pass


def need(condition, reason):
    if not condition:
        raise InvalidEvidence(reason)


def no_duplicates(pairs):
    result = {}
    for key, value in pairs:
        need(key not in result, 'JSON_DUPLICATE_KEY:' + key)
        result[key] = value
    return result


def parse_json(body):
    return json.loads(body, object_pairs_hook=no_duplicates,
                      parse_constant=lambda v: (_ for _ in ()).throw(InvalidEvidence('NON_FINITE_JSON:' + v)))


def sha256_file(path):
    digest = hashlib.sha256()
    with pathlib.Path(path).open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def safe_name(name):
    path = pathlib.PurePosixPath(name)
    return bool(name) and not name.startswith('/') and '\\' not in name and '\x00' not in name \
        and all(part not in ('', '.', '..') for part in name.split('/')) and not path.is_absolute()


def exact_ids(rows, field, expected, label):
    need(isinstance(rows, list), 'ROWSET_SHAPE:' + label)
    values = [row.get(field) if isinstance(row, dict) else None for row in rows]
    need(all(isinstance(v, str) for v in values), 'ID_SHAPE:' + label)
    need(len(values) == len(set(values)), 'DUPLICATE_ID:' + label)
    need(set(values) == set(expected), 'ID_SET:' + label)


def validate_export(path, expected=None, contract_path=None, allow_legacy_033=False):
    """Return verified artifact facts; expected may pin job/commit/APK/snapshot/profile.

    The ZIP's semantic receipt and source manifest must agree. FULL_RAW must
    contain snapshot.db with the actual declared digest; compact output cannot
    count as FULL_RAW. Returned sourceClosureVerified is an observation from the
    captured inventory, never an authorization to delete local source objects.
    """
    expected = expected or {}
    contract_path = contract_path or pathlib.Path(__file__).with_name('report_expected_ids_r334.json')
    contract = parse_json(pathlib.Path(contract_path).read_bytes())
    hashes, sizes = {}, {}
    with zipfile.ZipFile(path) as archive:
        entries = archive.infolist()
        need(1 <= len(entries) <= 200000, 'ZIP_MEMBER_COUNT')
        names = [item.filename for item in entries]
        need(len(names) == len(set(names)), 'ZIP_DUPLICATE_MEMBER')
        need(all(safe_name(name) for name in names), 'ZIP_UNSAFE_MEMBER')
        need(all(not item.is_dir() and not (item.flag_bits & 1) for item in entries), 'ZIP_MEMBER_TYPE')
        need(sum(item.file_size for item in entries) <= 16 * 1024**3, 'ZIP_EXPANDED_BOUND')
        required = {'ARCHIVE_MANIFEST.jsonl', 'source-manifest.json', 'R31_SEMANTIC_RECEIPT.json',
                    'R30_REPORT.json', 'R30_REPORT.md', 'REPORT_DETAILS.txt', 'R26_REPORT.json',
                    'R27_REPORT.md', 'R26_POLICY_IDENTITIES.jsonl', 'R26_FROZEN_SURFACE.json',
                    'STORAGE_CENSUS.txt'}
        need(required <= set(names), 'ZIP_REQUIRED_MEMBERS:' + ','.join(sorted(required - set(names))))
        for item in entries:
            digest, count = hashlib.sha256(), 0
            with archive.open(item) as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(block)
                    count += len(block)
            need(count == item.file_size, 'ZIP_LENGTH:' + item.filename)
            hashes[item.filename], sizes[item.filename] = digest.hexdigest(), count

        def read(name, limit=16 * 1024**2):
            need(sizes[name] <= limit, 'JSON_MEMBER_BOUND:' + name)
            return archive.read(name)

        manifest_bytes = read('ARCHIVE_MANIFEST.jsonl', 64 * 1024**2)
        lines = manifest_bytes.splitlines(keepends=True)
        need(len(lines) >= 3 and len(lines) <= 200000, 'MANIFEST_LINES')
        manifest = [parse_json(line) for line in lines]
        header, footer = manifest[0], manifest[-1]
        need(header.get('kind') == 'header' and header.get('format') == 'STARCORE_REPORT_CP4', 'MANIFEST_HEADER')
        need(footer.get('kind') == 'footer', 'MANIFEST_FOOTER')
        need(hashlib.sha256(b''.join(lines[:-1])).hexdigest() == footer.get('manifest_body_sha256'), 'MANIFEST_BODY_HASH')
        members = [row for row in manifest[1:-1] if row.get('kind') == 'member']
        parts = [row for row in manifest[1:-1] if row.get('kind') == 'part']
        need(len(members) + len(parts) == len(manifest) - 2, 'MANIFEST_UNKNOWN_RECORD')
        need(footer.get('members') == len(members) and footer.get('parts') == len(parts), 'MANIFEST_COUNTS')
        manifest_names = [row.get('name') for row in members]
        need(all(isinstance(name, str) and safe_name(name) for name in manifest_names), 'MANIFEST_MEMBER_PATH')
        need(len(set(manifest_names)) == len(manifest_names), 'MANIFEST_DUPLICATE_MEMBER')
        need(set(names) == set(manifest_names) | {'ARCHIVE_MANIFEST.jsonl'}, 'ZIP_MANIFEST_SET')
        part_names = [row.get('name') for row in parts]
        need(len(part_names) == len(set(part_names)), 'MANIFEST_DUPLICATE_PART')
        for row in members:
            name = row['name']
            need(sizes[name] == row.get('raw_bytes') and hashes[name] == row.get('raw_sha256'), 'MEMBER_HASH:' + name)
            own_parts = [part for part in parts if part.get('member') == name]
            need(row.get('part_count') == len(own_parts), 'PART_COUNT:' + name)
        need(all(row.get('member') in set(manifest_names) for row in parts), 'PART_MEMBER_UNKNOWN')

        source = parse_json(read('source-manifest.json'))
        receipt = parse_json(read('R31_SEMANTIC_RECEIPT.json'))
        document = parse_json(read('R30_REPORT.json', 64 * 1024**2))
        need(receipt.get('format') == 'R31_REPORT_SEMANTICS_V1', 'SEMANTIC_FORMAT')
        need(receipt.get('reportSha256') == hashes['R30_REPORT.json'], 'SEMANTIC_REPORT_HASH')
        job = receipt.get('jobId')
        need(isinstance(job, str) and re.fullmatch(r'[0-9a-f]{64}', job) is not None, 'JOB_ID')
        need(header.get('report_id') == job, 'MANIFEST_JOB_ID')
        watermark = document.get('snapshotWatermark', {})
        need(watermark.get('reportJobId') == job, 'DOCUMENT_JOB_ID')
        need(receipt.get('sourceCommit') == source.get('sourceCommit') == watermark.get('sourceCommit'), 'SOURCE_COMMIT')
        need(receipt.get('snapshotSha256') == source.get('snapshotSha256') == watermark.get('sourceManifest', {}).get('snapshotSha256'), 'SNAPSHOT_ID')
        need(source.get('apkSha256') == watermark.get('sourceManifest', {}).get('apkSha256'), 'APK_ID')
        raw = receipt.get('rawEvidence')
        need(type(raw) is bool and source.get('rawEvidenceIncluded') is raw, 'RAW_SCOPE_CONFLICT')
        if raw:
            need('snapshot.db' in hashes, 'RAW_SNAPSHOT_MISSING')
            need(hashes['snapshot.db'] == source.get('snapshotSha256') and sizes['snapshot.db'] == source.get('snapshotBytes'), 'RAW_SNAPSHOT_HASH')
            need('RAW_PAYLOADS_EXCLUDED' not in str(footer.get('scope')), 'RAW_FOOTER_SCOPE')
        else:
            need('DIAGNOSTIC_SCOPE.json' in hashes and 'snapshot.db' not in hashes, 'COMPACT_SCOPE_CONTENT')
            need('RAW_PAYLOADS_EXCLUDED' in str(footer.get('scope')), 'COMPACT_FOOTER_SCOPE')
        profile = 'FULL_RAW' if raw else 'DIAGNOSTIC'
        for key, value in {'jobId': job, 'sourceCommit': source.get('sourceCommit'),
                           'apkSha256': source.get('apkSha256'), 'snapshotSha256': source.get('snapshotSha256'),
                           'profile': profile}.items():
            if key in expected:
                need(value == expected[key], 'EXPECTED_IDENTITY:' + key)

        exact_ids(document.get('chapters'), 'sectionId', ['R30_%02d' % i for i in range(1, 13)], 'chapters')
        exact_ids(document.get('metricResults'), 'metricId', contract['metrics'], 'metrics')
        need(receipt.get('chapters') == 12 and receipt.get('metrics') == 44, 'SEMANTIC_COUNTS')
        for chapter in document['chapters']:
            need(chapter.get('snapshotWatermark') == watermark, 'CHAPTER_WATERMARK')
        objects = document.get('objectIndex', {})
        for group, expected_ids in contract['objects'].items():
            field = 'id' if group == 'logicalTables' else 'objectId'
            exact_ids(objects.get(group, {}).get('objects'), field, expected_ids, 'objects/' + group)

        catalog_xor = bytearray(32)
        catalog_checks, pages = {}, 0
        legacy_duplicate_names = 0
        for name in names:
            if not re.fullmatch(r'catalog-[0-9]{4,}\.jsonl', name):
                continue
            pages += 1
            part = hashlib.sha256((name + '\x00' + hashes[name]).encode()).digest()
            for n, value in enumerate(part):
                catalog_xor[n] ^= value
            with archive.open(name) as stream:
                for line in stream:
                    need(len(line) <= 131072, 'CATALOG_LINE_BOUND')
                    try:
                        row = parse_json(line)
                    except InvalidEvidence as error:
                        # This compatibility path is explicit, read-only, and pins
                        # the known installed source/APK. New-build acceptance is
                        # always strict; never silently drop an old duplicate key.
                        need(allow_legacy_033 and source.get('sourceCommit') == 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'
                             and source.get('apkSha256') == '8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
                             and str(error) == 'JSON_DUPLICATE_KEY:name', 'CATALOG_INVALID_JSON:' + str(error))
                        pairs = json.loads(line, object_pairs_hook=lambda items: items)
                        need(isinstance(pairs, list) and all(isinstance(p, tuple) and len(p) == 2 for p in pairs), 'LEGACY_PRAGMA_SHAPE')
                        keys = [p[0] for p in pairs]
                        row = dict(pairs)
                        need(row.get('kind') == 'pragma_row' and row.get('operation') in {'table_info', 'index_info', 'index_list'}
                             and keys.count('name') == 2 and len(keys) == len(set(keys)) + 1, 'LEGACY_PRAGMA_ONLY')
                        row['legacyNameValues'] = [v for k, v in pairs if k == 'name']
                        legacy_duplicate_names += 1
                    if row.get('kind') != 'check_result':
                        continue
                    key = row.get('id')
                    need(key not in catalog_checks, 'CATALOG_CHECK_DUPLICATE')
                    catalog_checks[key] = {field: row.get(field) for field in
                                          ('execution_state', 'domain_outcome', 'health', 'completes_contract')}
        need(set(catalog_checks) == set(contract['checks']), 'CATALOG_CHECK_SET')
        need(catalog_checks == receipt.get('checks'), 'CATALOG_RECEIPT_CHECKS')
        need(pages == receipt.get('catalogPages') and catalog_xor.hex() == receipt.get('catalogHashXor'), 'CATALOG_CLOSURE')
        need(footer.get('evidence_complete') is True and footer.get('checks_complete') is True, 'ARCHIVE_INCOMPLETE')
        checkpoint = source.get('r26CheckpointAuditR32', {})
        if checkpoint:
            inventory_name = checkpoint.get('metadataMember')
            need(inventory_name in hashes, 'CHECKPOINT_INVENTORY_MISSING')
            checkpoint_rows, summaries, ids = [], [], set()
            with archive.open(inventory_name) as stream:
                for line in stream:
                    need(len(line) <= 131072, 'CHECKPOINT_LINE_BOUND')
                    row = parse_json(line)
                    if row.get('kind') == 'checkpoint_audit_summary':
                        summaries.append(row.get('summary'))
                        continue
                    need(row.get('kind') == 'checkpoint_object', 'CHECKPOINT_RECORD_KIND')
                    identity = row.get('expectedSha256')
                    need(isinstance(identity, str) and re.fullmatch(r'[0-9a-f]{64}', identity) is not None
                         and identity not in ids, 'CHECKPOINT_DUPLICATE_OR_BAD_ID')
                    ids.add(identity)
                    checkpoint_rows.append(row)
            need(summaries == [checkpoint], 'CHECKPOINT_SUMMARY_SOURCE_MISMATCH')
            missing = sum(row.get('availability') == 'MISSING_OBJECT' for row in checkpoint_rows)
            unknown_children = sum(row.get('childrenKnown') is False for row in checkpoint_rows)
            need(checkpoint.get('observedObjects') == len(checkpoint_rows)
                 and checkpoint.get('missingObjects') == missing
                 and checkpoint.get('unknownChildSets') == unknown_children, 'CHECKPOINT_RECOUNT')
            if missing or unknown_children:
                need(checkpoint.get('sourceClosureVerified') is False, 'CHECKPOINT_FALSE_CLOSURE')
    checkpoint = source.get('r26CheckpointAuditR32', {})
    return {'status': 'LEGACY_ARTIFACT_VERIFIED_WITH_SCHEMA_DEFECT' if legacy_duplicate_names else 'ARTIFACT_CONSISTENT', 'phoneExecutionVerified': False,
            'jobId': job, 'sourceCommit': source.get('sourceCommit'), 'apkSha256': source.get('apkSha256'),
            'profile': profile, 'snapshotSha256': source.get('snapshotSha256'),
            'zipSha256': sha256_file(path), 'zipBytes': pathlib.Path(path).stat().st_size,
            'members': len(names), 'missingCheckpointObjects': checkpoint.get('missingObjects'),
            'legacyDuplicateNameRows': legacy_duplicate_names,
            'sourceClosureVerified': checkpoint.get('sourceClosureVerified'),
            'boundary': 'EXPORTED_BYTES_AND_REFERENCES_ONLY; NO_DEVICE_TEST_OR_QUALITY_PASS'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('zip_path', type=pathlib.Path)
    parser.add_argument('--expected', type=pathlib.Path, help='JSON with pinned job/commit/APK/snapshot/profile')
    parser.add_argument('--contract', type=pathlib.Path)
    parser.add_argument('--allow-legacy-033', action='store_true', help='Inspect the exact old 033 build while retaining its duplicate-name defect; never grants new-build acceptance')
    arguments = parser.parse_args()
    try:
        expected = parse_json(arguments.expected.read_bytes()) if arguments.expected else None
        print(json.dumps(validate_export(arguments.zip_path, expected, arguments.contract, arguments.allow_legacy_033), ensure_ascii=False, indent=2))
    except (InvalidEvidence, OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        print(json.dumps({'status': 'REJECTED', 'reason': str(error)}, ensure_ascii=False))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
