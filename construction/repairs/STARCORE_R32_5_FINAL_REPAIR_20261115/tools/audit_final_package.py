#!/usr/bin/env python3
"""Independently verify final source snapshots and sequential batch identity gates.

This does not claim Android Gradle, device behavior, business acceptance, or
natural prediction quality. It executes the actual packaged application tool.
"""
import argparse
from collections import Counter
import hashlib
import importlib.util
import json
from pathlib import Path, PurePosixPath
import stat
import subprocess
import sys
import zipfile


def sha(data):
    return hashlib.sha256(data).hexdigest()


def archive(path, destination=None):
    hashes = {}
    with zipfile.ZipFile(path) as z:
        for info in z.infolist():
            rel = PurePosixPath(info.filename)
            if rel.is_absolute() or '..' in rel.parts or '\\' in info.filename:
                raise ValueError('ARCHIVE_UNSAFE_PATH ' + info.filename)
            if stat.S_ISLNK(info.external_attr >> 16):
                raise ValueError('ARCHIVE_SYMLINK ' + info.filename)
            if info.is_dir():
                continue
            if info.filename in hashes:
                raise ValueError('ARCHIVE_DUPLICATE ' + info.filename)
            data = z.read(info)
            hashes[info.filename] = sha(data)
            if destination:
                target = destination.joinpath(*rel.parts)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(data)
                if info.external_attr >> 16 & 0o111:
                    target.chmod(0o755)
    return hashes


def archive_modes(path):
    with zipfile.ZipFile(path) as z:
        return {i.filename: ('755' if i.external_attr >> 16 & 0o111 else '644')
                for i in z.infolist() if not i.is_dir()}


def run(package, out):
    if out.exists():
        raise ValueError('OUTPUT_MUST_BE_NEW')
    out.mkdir(parents=True)
    project = out/'project'
    project.mkdir()
    before = archive(package/'SOURCE_BASELINE.zip', project)
    after = archive(package/'SOURCE_CANDIDATE.zip')
    before_modes = archive_modes(package/'SOURCE_BASELINE.zip')
    after_modes = archive_modes(package/'SOURCE_CANDIDATE.zip')
    cumulative_modes = dict(before_modes)
    manifest = json.loads((package/'code_manifest.json').read_text())
    spec = importlib.util.spec_from_file_location('packaged_apply', package/'tools/apply_batch.py')
    apply = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(apply)
    apply.validate_manifest(manifest)
    cumulative = dict(before)
    manual_actions, results, manual_paths, derived_paths = Counter(), [], set(), set()
    last_generated = []
    for bid in sorted(manifest['batches'], key=lambda b: int(b[1:])):
        row = manifest['batches'][bid]
        for item in row['files']:
            manual_actions[item['action']] += 1
            if item['path'] in manual_paths:
                raise ValueError('DUPLICATE_MANUAL_PATH ' + item['path'])
            manual_paths.add(item['path'])
        for item in row.get('derived_files', []):
            derived_paths.add(item['path'])
        for item in apply.batch_entries(manifest, bid):
            if cumulative.get(item['path']) != item['before_sha256']:
                raise ValueError('STAGE_ARCHIVE_BEFORE_MISMATCH ' + bid + ' ' + item['path'])
            if sha((package/item['source']).read_bytes()) != item['after_sha256']:
                raise ValueError('PAYLOAD_HASH_MISMATCH ' + item['path'])
            cumulative[item['path']] = item['after_sha256']
            cumulative_modes[item['path']] = str(item.get('mode', cumulative_modes.get(item['path'], '644')))[-3:]
        snapshot = json.loads((package/row['stage_identity']['snapshot']).read_text())
        last_generated = snapshot.get('generated_outputs', [])
        for generated in last_generated:
            if sha((package/generated['source']).read_bytes()) != generated['sha256']:
                raise ValueError('GENERATED_PAYLOAD_CORRUPT ' + generated['path'])
        actual = apply.execute(package, project, bid, True)
        (out/(bid+'-apply.json')).write_text(json.dumps(actual, ensure_ascii=False, indent=2)+'\n')
        repeated = apply.execute(package, project, bid, True)
        if repeated['changed_files'] != 0:
            raise ValueError('NOT_IDEMPOTENT ' + bid)
        for item in apply.batch_entries(manifest, bid):
            expected_mode = apply.declared_mode(item)
            if expected_mode is not None and apply.file_mode(project/item['path']) != expected_mode:
                raise ValueError('COMPOSITION_MODE_MISMATCH ' + bid + ' ' + item['path'])
        receipt = json.loads((project/'.starcore-plan-receipts'/ (bid+'.json')).read_text())
        if receipt['manifest_sha256'] != sha((package/'code_manifest.json').read_bytes()) or receipt['scope'] != 'SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE':
            raise ValueError('RECEIPT_SCOPE_OR_IDENTITY ' + bid)
        command = [sys.executable, str(package/'tools/verify_stage_identity.py'), '--package', str(package),
                   '--repo', str(project), '--batch', bid, '--generate']
        completed = subprocess.run(command, text=True, capture_output=True)
        (out/(bid+'-original-identity.log')).write_text(completed.stdout+completed.stderr)
        completed.check_returncode()
        for generated in last_generated:
            if sha((project/generated['path']).read_bytes()) != generated['sha256']:
                raise ValueError('REAL_GENERATOR_OUTPUT_MISMATCH ' + bid + ' ' + generated['path'])
        result = {'batch': bid, 'application': 'PASS', 'changed_files': actual['changed_files'],
                  'repeat_changed_files': repeated['changed_files'], 'original_identity_generator': 'PASS',
                  'original_identity_mutation_cases': 8, 'receipt_scope': receipt['scope']}
        results.append(result)
        print(json.dumps(result), flush=True)
    for generated in last_generated:
        cumulative[generated['path']] = generated['sha256']
        cumulative_modes.setdefault(generated['path'], '644')
    differences = [{'path': p, 'expected': cumulative.get(p), 'actual': after.get(p)}
                   for p in sorted(set(cumulative)|set(after)) if cumulative.get(p) != after.get(p)]
    (out/'SOURCE_ARCHIVE_DIFFERENCES.json').write_text(json.dumps(differences, ensure_ascii=False, indent=2)+'\n')
    if differences:
        raise ValueError('SOURCE_CANDIDATE_NOT_EXACT_COMPOSITION ' + str(len(differences)))
    mode_differences = [{'path': p, 'expected': cumulative_modes.get(p), 'actual': after_modes.get(p)}
                        for p in sorted(set(cumulative_modes)|set(after_modes))
                        if cumulative_modes.get(p) != after_modes.get(p)]
    (out/'SOURCE_ARCHIVE_MODE_DIFFERENCES.json').write_text(json.dumps(mode_differences, indent=2)+'\n')
    if mode_differences:
        raise ValueError('SOURCE_CANDIDATE_MODE_NOT_EXACT_COMPOSITION '+str(len(mode_differences)))
    report = {'status': 'PASS', 'scope': 'SOURCE_COMPOSITION_SEQUENTIAL_APPLICATION_AND_ORIGINAL_BUILD_IDENTITY',
              'code_manifest_sha256': sha((package/'code_manifest.json').read_bytes()),
              'baseline_archive_files': len(before), 'candidate_archive_files': len(after),
              'manual_source_paths': len(manual_paths), 'manual_actions': dict(manual_actions),
              'derived_identity_paths': len(derived_paths), 'batches': results,
              'source_archive_unlisted_differences': 0, 'source_archive_mode_unlisted_differences': 0,
              'declared_payload_modes_verified': True,
              'android_gradle': 'NOT_RUN_BY_THIS_CHECK', 'business_acceptance': 'NOT_PROVEN_BY_THIS_CHECK',
              'device_runtime': 'NOT_RUN_BY_THIS_CHECK', 'natural_quality': 'NOT_PROVEN'}
    (out/'RESULTS.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    print(json.dumps(run(args.package.resolve(), args.out.resolve()), ensure_ascii=False, indent=2))
