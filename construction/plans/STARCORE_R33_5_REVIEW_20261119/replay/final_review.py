#!/usr/bin/env python3
"""Recompute B08 engineering scope and the repository review's additional checks."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile

from stage_checked import load_module
from verify_completed_source import verify as verify_completed_source

REVIEW = Path(__file__).resolve().parents[1]


def sha(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def read(path):
    return json.loads(path.read_bytes())


def require(ok, message):
    if not ok:
        raise ValueError(message)


def referenced(root, ref):
    name = Path(ref['path'])
    path = (root / name).resolve(strict=True)
    require(not name.is_absolute() and path.is_relative_to(root) and sha(path) == ref['sha256'],
            'EVIDENCE_REFERENCE_MISMATCH:' + str(name))
    return path


def proof_paths(execution, root, assertion_id):
    matches = [a for g in execution['groups'] for a in g['assertions'] if a['id'] == assertion_id]
    require(len(matches) == 1 and matches[0].get('status') == 'PASS', 'PENDING:' + assertion_id)
    paths = [referenced(root, p) for p in matches[0]['proofs']]
    require(bool(paths), 'PENDING_ORIGINAL:' + assertion_id)
    return [read(p) for p in paths]


def run(args, out, temp):
    base = load_module(REVIEW / 'prepare.py', 'restore')
    seal = load_module(REVIEW / 'verify_review.py', 'review_seal').verify(REVIEW)
    plan = args.plan.resolve(strict=True)
    root = args.evidence.resolve(strict=True)
    plan_sha = sha(plan / 'verification/SHA256.json')
    require(plan_sha == base.SEAL, 'WRONG_BASE_PLAN_SEAL')
    exe, stage = read(args.execution), read(args.stage_receipt)
    commit, apk = exe['sourceCommit'], exe['apkSha256']
    out.update(sourceCommit=commit, apkSha256=apk, reviewManifestSha256=seal, planManifestSha256=plan_sha,
               executionSha256=sha(args.execution), stageReceiptSha256=sha(args.stage_receipt))
    require(stage.get('stage') == 'B08' and stage.get('reviewManifestSha256') == seal, 'WRONG_B08_HANDOFF')
    git = ['git', '-C', str(args.repo)]
    require(subprocess.check_output(git + ['rev-parse', 'HEAD'], text=True).strip() == commit, 'FINAL_SOURCE_HEAD')
    subprocess.run(git + ['merge-base', '--is-ancestor', stage['sourceHead'], commit], check=True)
    manifest = json.loads(subprocess.check_output(git + ['show', commit + ':release/release_manifest.json']))
    require(manifest['versionName'].startswith('R33.5-B08-') and manifest['versionCode'] == exe['versionCode']
            and manifest['versionName'] == exe['versionName'], 'FINAL_B08_VERSION')
    out['completedSource'] = verify_completed_source(plan, args.repo, REVIEW, stage)

    def invoke(label, script, *argv):
        process = subprocess.run([sys.executable, '-B', str(script), *map(str, argv)],
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        out['checks'][label] = {'exitCode':process.returncode,
                                'outputSha256':hashlib.sha256(process.stdout).hexdigest()}
        if process.returncode:
            raise ValueError(label + ':' + process.stdout.decode(errors='replace')[-700:])
        return process.stdout

    invoke('baseSeal', plan / 'verification/verify_package.py')
    release = temp / 'release.json'
    invoke('signedRelease', REVIEW / 'source/verify_delivery.py', '--plan', plan, '--repo', args.repo,
           '--candidate', args.candidate, '--dumpsys', args.dumpsys, '--baseline-apk', args.baseline_apk,
           '--aapt', args.aapt, '--apksigner', args.apksigner, '--out', release)
    actual = read(release)
    require(actual['sourceCommit'] == commit and actual['apk']['sha256'] == apk and
            actual == read(referenced(root, exe['finalBuild']['releaseReceipt'])), 'MIXED_FINAL_RELEASE_OR_BUILD')
    score = temp / 'score.json'
    invoke('originalScore', plan / 'verification/score_final.py', args.execution, root, score,
           '--aapt', args.aapt, '--apksigner', args.apksigner)
    scored = read(score)
    out['engineeringGroups'] = scored['engineeringGroups']
    out['currentOriginalClauses'] = scored['currentOriginalClauses']
    out['currentOriginalRequirements'] = scored['currentOriginalRequirements']
    require(out['engineeringGroups']['verified'] == out['engineeringGroups']['denominator'] == 32,
            'PENDING_ENGINEERING_GROUPS')
    trace_paths = {referenced(root, e['rawTraceManifest']) for suffix in ('POS', 'TRACE')
                   for e in proof_paths(exe, root, 'B08.2.' + suffix)}
    require(len(trace_paths) == 1, 'MIXED_TRACE_MANIFESTS')
    bundle = trace_paths.pop()
    tools = temp / 'runtime'
    invoke('runtimeTools', REVIEW / 'runtime/prepare_tools.py', plan, tools)
    # The short B08.2 join and the 100-period timing window may have distinct calendars.
    window = args.window_bundle or bundle
    invoke('enabledWindow', REVIEW / 'runtime/check_window.py', '--tools', tools, '--bundle', window,
           '--evidence', root, '--start', args.start, '--end', args.end, '--commit', commit, '--apk', apk)
    research = REVIEW / 'research/check_research.py'
    invoke('strictTrace', research, 'trace', plan, bundle, root, commit, apk)
    refs = {referenced(root, raw) for env in proof_paths(exe, root, 'B07.4.TRACE')
            for raw in env['rawArtifacts']}
    chains = {p.resolve(strict=True) for p in args.followup}
    indexed = set()
    for raw in refs:
        if raw.suffix.lower() == '.json':
            evidence = read(raw)
            if isinstance(evidence, dict) and {'firstCase','work','nextCase'} <= set(evidence):
                indexed.add(raw)
    require(chains == indexed, 'FOLLOWUPS_MUST_MATCH_CURRENT_B07_4_TRACE_CHAINS')
    out['researchFindings'] = []
    for n, chain in enumerate(sorted(chains), 1):
        next_case = read(referenced(chain.parent, read(chain)['nextCase']))
        require(next_case['commit'] == commit, 'FOLLOWUP_FROM_OTHER_COMMIT')
        outcome = json.loads(invoke('researchFollowup' + str(n), research, 'followup', plan, chain))
        out['researchFindings'].append({'caseKey':outcome['caseKey'], 'chainSha256':sha(chain),
                                       'effect':outcome['effect']})
        require(outcome['effect'] not in ('EVIDENCE_MISSING', 'EFFECT_UNPROVEN'), 'PENDING_FOLLOWUP_EVIDENCE')
    out['status'] = 'VERIFIED_32_ENGINEERING_GROUPS_AND_REVIEW_CHECKS'


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    fields = ('plan', 'repo', 'candidate', 'dumpsys', 'baseline-apk', 'aapt', 'apksigner', 'execution',
              'evidence', 'stage-receipt', 'start', 'end')
    for field in (*fields, 'window-bundle'):
        parser.add_argument('--' + field, type=Path)
    parser.add_argument('--followup', action='append', type=Path, default=[])
    args = parser.parse_args(argv)
    absent = [key for key in fields if not (p := getattr(args, key.replace('-', '_'))) or not p.exists()]
    if not args.followup or any(not p.is_file() for p in args.followup):
        absent.append('followup')
    if args.window_bundle and not args.window_bundle.is_file():
        absent.append('window-bundle')
    result = {'status':'INCOMPLETE', 'checks':{}, 'missingInputs':absent, 'naturalQuality':'SEPARATE_REVIEW'}
    if not absent:
        try:
            with tempfile.TemporaryDirectory() as temporary:
                run(args, result, Path(temporary))
        except (OSError, ValueError, KeyError, IndexError, TypeError, subprocess.SubprocessError) as error:
            result['reason'] = str(error)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if result['status'].startswith('VERIFIED_') else 1


if __name__ == '__main__':
    sys.exit(main())
