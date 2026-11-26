#!/usr/bin/env python3
"""Apply sealed groups and R967 checks with individually reviewed source corrections."""

import argparse
from pathlib import Path
import sys

import rebind_semantic_inputs as sealed

REVIEW = 'verification/r343/corrections/INDEX.json'


def load_reviews(repo):
    catalog = sealed.unique(sealed.regular(repo, REVIEW).read_bytes())
    sealed.need(catalog['schema'] == 'STARCORE_FIELD173_SOURCE_CORRECTIONS_1',
                'FIELD173_CORRECTION_CATALOG')
    reviews = catalog['reviews']
    names = set()
    previous_batch = ''
    for review in reviews:
        path = review['path']
        key = (review['batch'], path, review['afterSha256'])
        sealed.need(path.startswith(sealed.JAVA) and path.endswith('.java') and key not in names,
                    'FIELD173_CORRECTION_PATH:' + path)
        sealed.need(review['batch'] >= previous_batch, 'FIELD173_CORRECTION_BATCH_ORDER')
        previous_batch = review['batch']
        names.add(key)
        for name, digest in review['originals'].items():
            body = sealed.regular(repo, 'verification/r343/corrections/' + name).read_bytes()
            sealed.need(sealed.sha(body) == digest, 'FIELD173_CORRECTION_ORIGINAL:' + name)
        sealed.need(bool(review['reason']) and bool(review['issue']), 'FIELD173_CORRECTION_REVIEW:' + path)
        if review.get('afterFile'):
            sealed.need(review['originals'].get(review['afterFile']) == review['afterSha256'],
                        'FIELD173_CORRECTION_AFTER_NOT_RETAINED:' + path)
    return reviews


def reviewed_after(repo, review):
    path = review['path']
    body = sealed.regular(repo, path).read_bytes()
    sealed.need(sealed.sha(body) == review['afterSha256'], 'FIELD173_CORRECTION_AFTER:' + path)
    return body


def apply_group(args):
    plan, repo = args.plan.resolve(), args.repo.resolve()
    spec = sealed.unique(sealed.regular(plan, 'PATCHSET.json').read_bytes())
    group = next(group for group in spec['groups'] if group['id'] == args.group)
    current_paths = {row['path'] for row in group['files']}
    approved = {}
    anchors = {}
    for entry in load_reviews(repo):
        if entry['batch'] >= group['batch']:
            continue
        name = entry['path']
        sealed.need(name not in current_paths, 'FIELD173_CORRECTION_NEEDS_REBASE:' + name)
        anchors.setdefault(name, entry['beforeSha256'])
        approved[name] = entry
    for entry in approved.values():
        reviewed_after(repo, entry)

    # Retain every original group check. Only its prior-group byte check has an
    # additional permitted result, independently matched to a reviewed after hash.
    sys.path.insert(0, str(plan / 'tools'))
    owner = sealed.module(plan / 'tools' / 'apply_patchset.py', 'field173_sealed_patch')
    base_require = owner.require
    prefix = 'PRIOR_GROUP_NOT_APPLIED:'

    def require(condition, failure):
        if not condition and failure.startswith(prefix):
            name = failure.removeprefix(prefix)
            if name in approved:
                prior = [row['after_sha256'] for item in spec['groups'] if item['batch'] < group['batch']
                         for row in item['files'] if row['path'] == name]
                if prior and prior[-1] == anchors[name]:
                    reviewed_after(repo, approved[name])
                    return
        base_require(condition, failure)

    owner.require = require
    sys.argv[1:] = ['--plan', str(plan), '--repo', str(repo), '--group', args.group]
    if args.apply:
        sys.argv.append('--apply')
    owner.main()


def rebind(args):
    repo = args.repo.resolve()
    reviews = [entry for entry in load_reviews(repo) if entry['batch'] <= args.batch]
    originals = sealed.reviewed_states
    original_pair = sealed.candidate_pair
    corrections = {(row['path'], row['afterSha256']): row for row in reviews}

    def reviewed_states(plan, baseline, batch):
        expected, states, groups = originals(plan, baseline, batch)
        patchset = sealed.unique(sealed.regular(plan, 'PATCHSET.json').read_bytes())
        stages = {group['id']: group['batch'] for group in patchset['groups']}
        after = {}
        for key, entry in corrections.items():
            if entry.get('afterFile'):
                after[key] = sealed.regular(repo, 'verification/r343/corrections/' + entry['afterFile']).read_bytes()
            else:
                after[key] = reviewed_after(repo, entry)

        def corrected(state, through):
            updated = dict(state)
            for row in reviews:
                if row['batch'] <= through:
                    name = row['path']
                    digest = sealed.sha(updated[name]) if name in updated else None
                    sealed.need(digest == row['beforeSha256'], 'FIELD173_CORRECTION_BEFORE:' + name)
                    updated[name] = after[(name, row['afterSha256'])]
            return updated

        permitted = list(states)
        for stage in sorted({stages[group] for group in groups} | {review['batch'] for review in reviews}):
            group_count = sum(stages[group] <= stage for group in groups)
            permitted.append(corrected(states[group_count], stage))
        corrected_expected = corrected(expected, batch)
        permitted.append(corrected_expected)
        return corrected_expected, permitted, groups

    def reviewed_pair(bodies, baseline):
        candidate, proof = original_pair(bodies, baseline)
        proof_record = sealed.unique(proof)
        changed = False
        for row in proof_record['controlled_changes']:
            review = corrections.get((row['path'], row['after_sha256']))
            if review:
                row['reviewer'] = 'STARCORE R34.3-173 construction correction ' + review['issue']
                row['rationale'] = review['reason']
                changed = True
        return candidate, sealed.encode(proof_record) if changed else proof

    for review in reviews:
        role_catalog = sealed.NEW_ROLES if review['beforeSha256'] is None else sealed.REVIEWED_EXISTING
        role, _ = role_catalog.setdefault(review['path'], (review['role'], review['reason']))
        sealed.need(role == review['role'], 'FIELD173_CORRECTION_ROLE_CHANGED:' + review['path'])
    sealed.reviewed_states = reviewed_states
    sealed.candidate_pair = reviewed_pair
    sys.argv[1:] = ['--repo', str(repo), '--plan', str(args.plan.resolve()), '--batch', args.batch]
    for operation in ('apply', 'check', 'resume'):
        if getattr(args, operation):
            sys.argv.append('--' + operation)
    if args.journal:
        sys.argv.extend(['--journal', str(args.journal)])
    sealed.main()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--plan', type=Path, required=True)
    commands = parser.add_subparsers(required=True)
    group = commands.add_parser('group')
    group.add_argument('--group', required=True)
    group.add_argument('--apply', action='store_true')
    group.set_defaults(execute=apply_group)
    semantic = commands.add_parser('semantic')
    semantic.add_argument('--batch', required=True)
    semantic.add_argument('--journal', type=Path)
    options = semantic.add_mutually_exclusive_group()
    for name in ('apply', 'check', 'resume'):
        options.add_argument('--' + name, action='store_true')
    semantic.set_defaults(execute=rebind)
    args = parser.parse_args()
    args.execute(args)


if __name__ == '__main__':
    main()
