#!/usr/bin/env python3
"""Exercise real plan-tool I/O gates in disposable copies; no Android/product tests.

Pass an exact 211 checkout or the six declared, hash-verified candidate preimages.
The latter proves candidate applicability only, never the full application baseline.
Results must be written to a fresh path outside the plan and source directories.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from plan_tool_safety import fresh_output, require, write_fresh_text


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--candidate-source-root', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    plan, source = args.plan_root.resolve(), args.candidate_source_root.resolve()
    output = fresh_output(args.output, [plan, source]) if args.output else None
    catalog = json.loads((plan / 'PATCHSET.json').read_text())
    originals = {f['path']: f['before_sha256'] for patch in catalog['patches'] for f in patch['files']}
    require(all(digest(source / path) == expected for path, expected in originals.items()),
            'candidate source preimages do not match the declared 211 hashes')
    results = []
    environment = dict(os.environ, PYTHONDONTWRITEBYTECODE='1')

    with tempfile.TemporaryDirectory(prefix='starcore-plan-tool-tests-') as directory:
        temporary = Path(directory)
        copied = temporary / 'plan'
        shutil.copytree(plan, copied)
        copied_source = temporary / 'source'
        copied_source.mkdir()
        for path, expected in originals.items():
            if expected is not None:
                target = copied_source / path
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source / path, target)
        tools = copied / 'tools'

        def invoke(tool, *arguments, optimized=False):
            cmd = [sys.executable, '-B'] + (['-O'] if optimized else []) + [str(tools / tool), *map(str, arguments)]
            return subprocess.run(cmd, text=True, capture_output=True, env=environment, timeout=90)

        def reject(case, result, needle=None):
            require(result.returncode != 0, case + ' unexpectedly succeeded')
            if needle:
                require(needle in result.stderr + result.stdout, case + ' failed for an unrelated reason')
            results.append({'case': case, 'status': 'PASS', 'observed_exit': result.returncode})

        verification = invoke('verify_plan.py')
        require(verification.returncode == 0, 'clean copied plan did not verify: ' + verification.stdout + verification.stderr)
        require(json.loads(verification.stdout)['verification_pass'], 'clean verifier returned false')
        results.append({'case': 'clean_complete_materialized_plan', 'status': 'PASS'})

        good = invoke('check_candidates.py', '--source-root', copied_source)
        require(good.returncode == 0, 'real five-candidate check failed: ' + good.stderr)
        candidate_result = json.loads(good.stdout)
        require(candidate_result['passed'] and len(candidate_result['patches']) == 5, 'not all candidates checked')
        require(all(digest(copied_source / path) == expected for path, expected in originals.items()), 'candidate check changed input')
        results.append({'case': 'five_real_candidates_and_source_preserved', 'status': 'PASS', 'declared_files': len(originals)})

        source_file = copied_source / next(path for path, expected in originals.items() if expected)
        source_before = source_file.read_bytes()
        reject('candidate_output_inside_source', invoke('check_candidates.py', '--source-root', copied_source, '--output', source_file), 'output must be outside')
        require(source_file.read_bytes() == source_before, 'source output probe changed input')
        readme = copied / 'README.md'
        readme_before = readme.read_bytes()
        reject('candidate_output_inside_plan', invoke('check_candidates.py', '--source-root', copied_source, '--output', readme), 'output must be outside')
        reject('verifier_output_inside_plan', invoke('verify_plan.py', '--output', readme), 'output must be outside')
        require(readme.read_bytes() == readme_before, 'plan output probe changed README')
        reject('verifier_output_inside_source', invoke('verify_plan.py', '--source-root', copied_source, '--output', source_file), 'output must be outside')
        require(source_file.read_bytes() == source_before, 'source changed')

        existing = temporary / 'existing.json'
        existing.write_bytes(b'preserve-existing-receipt')
        reject('existing_result_not_overwritten', invoke('check_candidates.py', '--source-root', copied_source, '--output', existing), 'output already exists')
        require(existing.read_bytes() == b'preserve-existing-receipt', 'existing receipt overwritten')
        symlink = temporary / 'symlink-result'
        symlink.symlink_to(existing)
        reject('symlink_result_not_followed', invoke('check_candidates.py', '--source-root', copied_source, '--output', symlink), 'symlink')
        require(existing.read_bytes() == b'preserve-existing-receipt', 'symlink target overwritten')

        spec_path = copied / 'PATCHSET.json'
        spec_original = spec_path.read_bytes()
        corrupt = json.loads(spec_original)
        corrupt['patches'][0]['sha256'] = '0' * 64
        spec_path.write_text(json.dumps(corrupt))
        for optimize in [False, True]:
            reject('corrupt_patch_hash_' + ('optimized' if optimize else 'normal'), invoke('check_candidates.py', '--source-root', copied_source, optimized=optimize), 'patch hash mismatch')
        spec_path.write_bytes(spec_original)

        empty = json.loads(spec_original)
        empty['patches'] = []
        spec_path.write_text(json.dumps(empty))
        reject('empty_candidate_catalog', invoke('check_candidates.py', '--source-root', copied_source), 'all five')
        spec_path.write_bytes(spec_original)

        manifest = copied / 'MANIFEST.json'
        manifest_original = manifest.read_bytes()
        manifest.unlink()
        spec_path.unlink()
        incomplete = invoke('verify_plan.py')
        reject('missing_mandatory_distribution_files', incomplete, 'REQUIRED_DISTRIBUTION_FILE_MISSING')
        require(not json.loads(incomplete.stdout)['verification_pass'], 'incomplete plan returned PASS')
        manifest.write_bytes(manifest_original)
        spec_path.write_bytes(spec_original)

        seal = copied / 'MANIFEST.sha256'
        seal_original = seal.read_bytes()
        seal.write_text('0' * 64 + '  MANIFEST.json\n')
        reject('wrong_manifest_seal', invoke('verify_plan.py'), 'MANIFEST.sha256 seal mismatch')
        seal.write_bytes(seal_original)

        integrity = copied / 'evidence/REPORT_INTEGRITY.json'
        integrity_original = integrity.read_bytes()
        corrupt = json.loads(integrity_original)
        corrupt['outer_sha256'] = '0' * 64
        integrity.write_text(json.dumps(corrupt))
        for optimize in [False, True]:
            destination = temporary / ('bad-extract-optimized' if optimize else 'bad-extract-normal')
            reject('corrupt_report_hash_' + ('optimized' if optimize else 'normal'), invoke('unpack_report.py', '--output', destination, optimized=optimize), 'outer ZIP SHA mismatch')
            require(not destination.exists(), 'corrupt report created an extraction directory')
        integrity.write_bytes(integrity_original)

        extracted = temporary / 'verified-report'
        unpacked = invoke('unpack_report.py', '--output', extracted, optimized=True)
        require(unpacked.returncode == 0, 'valid original unpack failed: ' + unpacked.stderr)
        unpack_result = json.loads(unpacked.stdout)
        require(unpack_result['verified'] and unpack_result['payloads_verified'] == 56, 'original payload proof missing')
        saved_integrity = json.loads(integrity_original)
        require(all((extracted / e['name']).stat().st_size == e['bytes'] and digest(extracted / e['name']) == e['sha256'] for e in saved_integrity['member_checks']), 'extracted payload differs')
        results.append({'case': 'valid_report_optimized_mode_and_56_payload_readback', 'status': 'PASS'})
        reject('existing_extraction_directory_preserved', invoke('unpack_report.py', '--output', extracted), 'output already exists')
        reject('extraction_inside_frozen_plan', invoke('unpack_report.py', '--output', copied / 'new-output'), 'output must be outside')
        require(not (copied / 'new-output').exists(), 'output probe added frozen plan files')
        require(not list(copied.rglob('__pycache__')), 'helper import polluted the frozen plan')
        require(all(digest(copied_source / path) == expected for path, expected in originals.items()), 'source preimages changed')
        results.append({'case': 'no_bytecode_or_source_pollution', 'status': 'PASS'})

    require(all(digest(source / path) == expected for path, expected in originals.items()), 'real source input changed')
    report = {'schema': 'STARCORE_R344_PLAN_TOOL_REGRESSION_1', 'scope': 'PLAN_TOOL_IO_AND_REAL_CANDIDATE_APPLICATION_ONLY',
              'tests': results, 'passed': len(results), 'failed': 0, 'tested_manifest_sha256': digest(plan / 'MANIFEST.json'),
              'tool_sha256': {name: digest(plan / 'tools' / name) for name in ['check_candidates.py', 'unpack_report.py', 'verify_plan.py', 'plan_tool_safety.py', 'test_plan_tools.py']},
              'candidate_preimage_files': sum(v is not None for v in originals.values()),
              'full_application_baseline': 'NOT_IMPLIED_BY_CANDIDATE_PREIMAGES', 'android_build': 'NOT_RUN',
              'physical_device': 'NOT_RUN', 'natural_quality': 'NOT_PROVEN', 'original_inputs_changed': False}
    text = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
    if output:
        write_fresh_text(output, text, [plan, source])
    else:
        print(text, end='')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError, subprocess.TimeoutExpired) as error:
        print('test_plan_tools: ' + str(error), file=sys.stderr)
        sys.exit(1)
