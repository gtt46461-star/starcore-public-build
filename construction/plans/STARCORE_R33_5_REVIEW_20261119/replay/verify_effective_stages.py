"""Replay all reviewed production stages on one fresh, real 033 Git checkout.

This is an isolated source rehearsal, not an APK, device or natural-quality claim.
The supplied source checkout is never mutated; every artifact stays under --out.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time
import stage_checked as checked
import production_overlay


def run(plan, review, source, out):
    plan, review, source, out = (Path(p).resolve() for p in (plan, review, source, out))
    if out.exists() or out.is_relative_to(source) or out.is_relative_to(review) or out.is_relative_to(plan):
        raise ValueError('FRESH_EXTERNAL_REHEARSAL_DIRECTORY_REQUIRED')
    seal = checked.load_module(review / 'verify_review.py', 'rehearsal_review_seal').verify(review)
    tool = checked.load_module(plan / 'verification/contractor/stage.py', 'rehearsal_stage')
    gen = checked.load_module(plan / 'P00/reference/scripts/r926_release_manifest.py', 'rehearsal_generator')
    pair = checked.load_module(review / 'research/b05_pair_source.py', 'rehearsal_pair')
    tool.git(source, 'cat-file', '-e', tool.BASE + '^{commit}')
    out.mkdir(parents=True, exist_ok=False)
    repo = out / 'candidate'
    checked.create_verified_checkout(tool, source, tool.BASE, repo, b'', {},
                                    {'scope': 'ISOLATED_BASELINE_SOURCE', 'sourceCommit': tool.BASE})
    effective = out / 'effective-plan'
    effective.mkdir()
    summary = production_overlay.prepare(tool, plan, pair, review, effective)
    manifest = json.loads((effective / 'control/APPLY_MANIFEST_R335.json').read_text())
    records = {b['id']: b['files'] for b in manifest['batches']}
    original = json.loads((repo / checked.VERSION_FILES[0]).read_text())['versionCode']
    binding_path = 'release/r33_plan_binding.json'
    binding_script = [sys.executable, '-B', str(repo / 'tools/r33/verify_plan_binding.py'),
                      '--repo', str(repo), '--plan', str(plan)]
    receipts = []
    retained = {}
    for batch in tool.STAGES:
        t = time.monotonic()
        patch = out / (batch + '.patch')
        receipt = checked.run_stage(tool, gen, pair, plan, repo, batch, tool.BASE, seal,
                                    emit=patch, review_root=review)
        if patch.stat().st_size:
            tool.git(repo, 'apply', '--check', '--index', str(patch))
            tool.git(repo, 'apply', '--index', str(patch))
        for row in records[batch]:
            name = row['path']
            body = (effective / row['payload']).read_bytes()
            if (repo / name).read_bytes() != body or tool.git(repo, 'show', ':' + name) != body:
                raise ValueError('REHEARSAL_INSTALLED_BYTES_DIFFER:' + batch + ':' + name)
            retained[name] = row['afterSha256']
        advance = checked.load_module(repo / 'tools/r33/advance_release_version.py', 'rehearsal_advance_' + batch)
        tx = out / ('version-' + batch)
        advance.prepare(repo, tx, original, batch, '2026-11-20')
        advance.apply(repo, tx)
        version_files = list(checked.VERSION_FILES)
        if batch == 'P00':
            created = subprocess.check_output([*binding_script, '--init-binding'], text=True)
            (out / 'P00-plan-binding.json').write_text(created, encoding='utf-8')
            version_files.append(binding_path)
        tool.git(repo, 'add', '--', *version_files)
        tool.git(repo, '-c', 'user.name=R33.5 source rehearsal', '-c', 'user.email=rehearsal@localhost',
                 '-c', 'commit.gpgsign=false', 'commit', '--quiet', '-m', 'Isolated reviewed source stage ' + batch)
        commit = tool.git(repo, 'rev-parse', 'HEAD').decode().strip()
        # Actual generator check confirms the committed review images, including
        # tracked R967 outputs, rather than silently writing new source bytes.
        semantic = subprocess.run([sys.executable, '-B', 'scripts/r967_semantic_identity.py', '--check'],
                                  cwd=repo, capture_output=True, text=True)
        (out / (batch + '-r967-check.log')).write_text(semantic.stdout + semantic.stderr)
        if semantic.returncode:
            raise ValueError('REHEARSAL_R967_CHECK_FAILED:' + batch)
        if tool.git(repo, 'status', '--porcelain', '--untracked-files=all'):
            raise ValueError('REHEARSAL_DIRTY_POST_STAGE:' + batch)
        for name, expected in retained.items():
            if name not in checked.VERSION_FILES:
                actual = hashlib.sha256((repo / name).read_bytes()).hexdigest()
                if actual != expected:
                    raise ValueError('REHEARSAL_INHERITED_BYTES_DIFFER:' + batch + ':' + name)
        item = {'stage': batch, 'commit': commit,
                'versionCode': json.loads((repo / checked.VERSION_FILES[0]).read_text())['versionCode'],
                'stageFileOperations': len(records[batch]), 'retainedProductionPaths': len(retained),
                'r967Check': 'PASS', 'sourceClean': True,
                'durationSeconds': round(time.monotonic() - t, 3), 'stageReceipt': receipt}
        (out / (batch + '-receipt.json')).write_text(json.dumps(item, ensure_ascii=False, indent=2) + '\n')
        receipts.append(item)
        print(json.dumps({k: v for k, v in item.items() if k != 'stageReceipt'}), flush=True)
    readback = json.loads(subprocess.check_output([*binding_script, '--require-clean'], text=True))
    if (tool.git(repo, 'show', receipts[0]['commit'] + ':' + binding_path) != (repo / binding_path).read_bytes()
            or readback['source_commit'] != receipts[-1]['commit']):
        raise ValueError('REHEARSAL_PLAN_BINDING_HANDOFF_DIFFERS')
    (out / 'B08-plan-binding.json').write_text(json.dumps(readback, indent=2) + '\n', encoding='utf-8')
    result = {'schema': 'STARCORE_R335_EFFECTIVE_SOURCE_REHEARSAL_20261120',
              'scope': 'ISOLATED_REAL_033_GIT_SOURCE_REHEARSAL_NO_APK_DEVICE_OR_QUALITY_CLAIM',
              'sourceBaseline': tool.BASE, 'reviewManifestSha256': seal,
              'productionReview': summary, 'stages': receipts, 'planBinding': readback, 'status': 'PASS',
              'candidateDirectory': str(repo)}
    (out / 'RESULT.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    return result


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('plan', 'review', 'source', 'out'):
        p.add_argument('--' + name, required=True, type=Path)
    args = p.parse_args()
    run(args.plan, args.review, args.source, args.out)
