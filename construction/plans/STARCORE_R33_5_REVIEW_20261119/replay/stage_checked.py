#!/usr/bin/env python3
"""Install one R33.5 stage from a committed source with the ordered version history."""
import argparse
import csv
import datetime
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

INVALIDATION = "release/r82_training_invalidation.json"
INVALIDATION_AFTER = {
    'B05': '4cd79138d64f8a48499a8a08ad7294bae300c44dde8c5bc351add01e6dee4563',
    'B07': 'b58acd80066dea55828d5f86852e21cc2bac0144c7d527a6b5dbd0e3526be7ad',
}
VERSION_FILES = (
    'release/release_manifest.json',
    'release/release_identity.properties',
    'app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java',
    'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java',
)


def load_module(path, name):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def pinned_module(plan, path, name, expected):
    data = (plan / path).read_bytes()
    if hashlib.sha256(data).hexdigest() != expected:
        raise ValueError('REVIEWED_TOOL_BYTES_CHANGED:' + path)
    return load_module(plan / path, name)


def release_at(stage_tool, repo, commit):
    data, _ = stage_tool.committed_file(repo, commit, VERSION_FILES[0])
    if not data:
        return None
    obj = json.loads(data)
    match = re.fullmatch(r'R33\.5-(P00|B0[1-8])-(\d{4}-\d{2}-\d{2})-(\d+)', obj.get('versionName', ''))
    if not match:
        return None
    datetime.date.fromisoformat(match[2])
    if type(obj.get('versionCode')) is not int or obj['versionCode'] != int(match[3]) or obj['versionCode'] <= 577311033:
        raise ValueError('INVALID_STAGE_VERSION:' + commit)
    return match[1], obj['versionCode'], obj, data


def verify_four(tool, generator, repo, commit, identity):
    _, _, obj, manifest = identity
    digest = hashlib.sha256(manifest).hexdigest()
    for path, render in zip(VERSION_FILES[1:], (generator.prop_text, generator.java_text, generator.host_build_config_text)):
        actual, _ = tool.committed_file(repo, commit, path)
        if actual != render(obj, digest).encode():
            raise ValueError('FOUR_FILE_VERSION_DRIFT:' + commit + ':' + path)


def ancestry(tool, generator, repo, target, baseline):
    head = tool.git(repo, 'rev-parse', 'HEAD').decode().strip()
    tool.git(repo, 'merge-base', '--is-ancestor', baseline, head)
    index = tool.STAGES.index(target)
    current = release_at(tool, repo, head)
    if index == 0:
        if current:
            raise ValueError('P00_ALREADY_VERSIONED_USE_NEXT_STAGE_OR_INSPECT')
        return head, []
    expected = tool.STAGES[index - 1]
    if current is None or current[0] != expected:
        raise ValueError('EXPECTED_PREVIOUS_STAGE_AT_HEAD:' + expected)
    verify_four(tool, generator, repo, head, current)
    chain = [(current[0], current[1], head)]
    upper = head
    for previous in reversed(tool.STAGES[:index - 1]):
        # Filter the history to commits touching the canonical version file.
        # Searching each next step from its successor also works after a merge.
        commits = tool.git(repo, 'rev-list', '--full-history', '--topo-order',
                           baseline + '..' + upper, '--', VERSION_FILES[0]).decode().splitlines()
        for commit in commits:
            identity = release_at(tool, repo, commit)
            if identity and identity[0] == previous and identity[1] < chain[-1][1]:
                verify_four(tool, generator, repo, commit, identity)
                chain.append((previous, identity[1], commit))
                upper = commit
                break
        else:
            raise ValueError('MISSING_COMMITTED_VERSION_ANCESTOR:' + previous)
    return head, chain[::-1]


def corrected_images(plan, stage, pair, work):
    updated = {}
    if stage == 'B05':
        java = pair.generate(plan, work)
        updated[pair.REL.as_posix()] = java.read_bytes()
    updated[INVALIDATION] = pair.generate_identity(plan, stage, work).read_bytes()
    if hashlib.sha256(updated[INVALIDATION]).hexdigest() != INVALIDATION_AFTER[stage]:
        raise ValueError('REVIEWED_NUMERIC_IDENTITY_DRIFT:' + stage)
    return updated


def validate_prefix(tool, repo, history, pair, target):
    required = [(name, commit) for name, _, commit in history if name >= 'B05' and name != 'P00']
    for name, commit in required:
        effective = 'B05' if name in ('B05', 'B06') else 'B07'
        for path, digest in [(pair.REL.as_posix(), pair.AFTER), (INVALIDATION, INVALIDATION_AFTER[effective])]:
            data, _ = tool.committed_file(repo, commit, path)
            allowed = {digest}
            # A partial B07 has B06's version until its final version transaction.
            # Accept either reviewed R82 image only for the active B07 input.
            if target == 'B07' and commit == history[-1][2] and path == INVALIDATION:
                allowed.add(INVALIDATION_AFTER['B07'])
            if data is None or tool.sha(data) not in allowed:
                raise ValueError('PREDECESSOR_REVIEW_BYTES_MISSING:' + name + ':' + path)
    if target in ('B06', 'B07', 'B08'):
        for name in (pair.REL.as_posix(), INVALIDATION):
            path = repo / name
            data, _ = tool.committed_file(repo, history[-1][2], name)
            if path.is_symlink() or not path.is_file() or path.read_bytes() != data:
                raise ValueError('CURRENT_B05_REVIEW_BYTES_MISSING:' + name)


def copy_stage_overlay(tool, stage, repo, head, plan, pair, work):
    # Rebuild only the active stage's images and ledger in an external directory.
    # The original installer still verifies the action, before/after and Git bytes.
    base_rows = tool.records(stage)
    originals = tool.materialize(stage, base_rows)
    images = corrected_images(plan, stage, pair, work / 'generate')
    previous = {}
    if stage == 'B07':
        previous[INVALIDATION] = pair.generate_identity(plan, 'B05', work / 'previous').read_bytes()
        if tool.sha(previous[INVALIDATION]) != INVALIDATION_AFTER['B05']:
            raise ValueError('B07_PREDECESSOR_NUMERIC_IDENTITY_DRIFT')
    manifest = json.loads((plan / 'control/APPLY_MANIFEST_R335.json').read_text())
    records = next(row['files'] for row in manifest['batches'] if row['id'] == stage)
    control = work / 'control'
    control.mkdir()
    ledger = {}
    for row in records:
        name = row['path']
        data = images.get(name, originals[name])
        before = previous.get(name)
        if before is None and row['beforeFile']:
            before = (plan / row['beforeFile']).read_bytes()
        # Support an already ported, not-yet-released original B05 as well as a fresh B04.
        if stage == 'B05' and name in images:
            committed, _ = tool.committed_file(repo, head, name)
            if committed == originals[name]:
                before = committed
        if before is not None:
            row['beforeFile'] = f'{stage}/before_full/{name}'
            old = work / row['beforeFile']
            old.parent.mkdir(parents=True, exist_ok=True)
            old.write_bytes(before)
            row['beforeSha256'] = tool.sha(before)
            row['action'] = 'REPLACE'
        path = work / row['payload']
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        row['afterSha256'] = tool.sha(data)
        ledger[name] = (row['beforeSha256'], row['afterSha256'])
    (control / 'APPLY_MANIFEST_R335.json').write_text(json.dumps(manifest, ensure_ascii=False), encoding='utf-8')
    with tool.LEDGER.open(encoding='utf-8-sig', newline='') as infile, (control / tool.LEDGER.name).open(
            'w', encoding='utf-8-sig', newline='') as outfile:
        reader = csv.DictReader(infile)
        writer = csv.DictWriter(outfile, fieldnames=reader.fieldnames)
        writer.writeheader()
        for row in reader:
            if row['批次'] == stage:
                row['前SHA256'], row['后SHA256'] = ledger[row['仓库路径']]
            writer.writerow(row)
    overlay_tool = load_module(plan / 'verification/contractor/stage.py', 'stage_r335_adjusted')
    overlay_tool.PACKAGE = work
    overlay_tool.LEDGER = control / tool.LEDGER.name
    return overlay_tool


def run_stage(tool, gen, pair, plan, repo, target, baseline, review_sha, emit=None, dest=None):
    if tool.complete_records(target) is None:
        raise ValueError('COMPLETE_R335_APPLY_MANIFEST_REQUIRED')
    head, history = ancestry(tool, gen, repo, target, baseline)
    if emit and emit.resolve().is_relative_to(repo):
        raise ValueError('PATCH_DESTINATION_MUST_BE_OUTSIDE_CHECKOUT')
    with tempfile.TemporaryDirectory(dir=emit.absolute().parent if emit else None) as temporary:
        work = Path(temporary)
        validate_prefix(tool, repo, history, pair, target)
        active = tool
        if target in ('B05', 'B07'):
            overlay = work / 'overlay'
            overlay.mkdir()
            active = copy_stage_overlay(tool, target, repo, head, plan, pair, overlay)
        draft = work / 'stage.patch'
        receipt = active.prepare(repo, target, emit=draft, baseline=baseline)
        if receipt['sourceHead'] != head:
            raise ValueError('SOURCE_HEAD_MOVED_RETRY')
        receipt['reviewManifestSha256'] = review_sha
        receipt['originalLedgerSha256'] = tool.sha(tool.LEDGER.read_bytes())
        receipt['orderedCommittedVersions'] = [
            {'stage': name, 'versionCode': version, 'commit': commit} for name, version, commit in history]
        if emit:
            os.link(draft, emit)
        elif dest:
            files = active.materialize(target, active.records(target))
            after = {name: (data, tool.committed_file(repo, head, name)[1] or '100644')
                     for name, data in files.items()}
            if target in ('B06', 'B07', 'B08'):
                for retained in (pair.REL.as_posix(), INVALIDATION):
                    if retained not in after:
                        after[retained] = tool.committed_file(repo, head, retained)
            receipt['outputStatus'] = tool.create_checkout(repo, head, dest, draft.read_bytes(), after, receipt.copy())
        return receipt


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('stage', choices=('P00', *(f'B{i:02}' for i in range(1, 9))))
    parser.add_argument('--plan', required=True, type=Path)
    parser.add_argument('--repo', required=True, type=Path)
    output = parser.add_mutually_exclusive_group()
    output.add_argument('--apply-to', type=Path)
    output.add_argument('--emit', type=Path)
    args = parser.parse_args()
    plan = args.plan.resolve()
    repo = args.repo.resolve()
    try:
        tool = pinned_module(plan, 'verification/contractor/stage.py', 'stage_r335', '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
        gen = pinned_module(plan, 'P00/reference/scripts/r926_release_manifest.py', 'identity_r926', '5de2507c327c55ef381b0c7e254de11f1fdd7dffc1109c1b971a7b59a7a58607')
        review = Path(__file__).resolve().parents[1]
        review_sha = load_module(review / 'verify_review.py', 'review_seal').verify(review)
        pair = load_module(review / 'research/b05_pair_source.py', 'b05_pair_source')
        receipt = run_stage(tool, gen, pair, plan, repo, args.stage, tool.BASE, review_sha, args.emit, args.apply_to)
        print(json.dumps(receipt, ensure_ascii=False, indent=2))
    except (OSError, ValueError, subprocess.SubprocessError) as exc:
        parser.exit(2, str(exc) + '\n')


if __name__ == '__main__':
    main()
