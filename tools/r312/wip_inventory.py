#!/usr/bin/env python3
"""S00 targeted custody inventory. Legacy status/ownership never means implementation PASS.

generate queries the historical 56-card ledger and only its relevant primary ownership rows.
check is read-only. sync-failures accepts only a real S00 BUILD_SUMMARY after gate build.
No source snapshots, signing, builds, downloads, or changes to historical ledgers.
"""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
INVENTORY = ROOT / 'construction/r31/r312/WIP_INVENTORY.json'
LEDGER = 'construction/r31/EXECUTION_LEDGER.json'
OWNERS = 'construction/r31/SOURCE_CHANGE_OWNERS.json'
SUMMARY = ROOT / 'verification/r31/r312/S00/BUILD_SUMMARY.json'
APP = 'app/src/main/java/com/ethk3/novaonepro/strategyv2/'


def git(*args, required=True):
    p = subprocess.run(['git', *args], cwd=ROOT, capture_output=True)
    if required and p.returncode:
        raise ValueError(p.stderr.decode(errors='replace').strip())
    return p.stdout


def revision(name):
    return git('rev-parse', '--verify', name + '^{commit}').decode().strip()


def ancestor(older, newer='HEAD'):
    return subprocess.run(['git', 'merge-base', '--is-ancestor', older, newer], cwd=ROOT).returncode == 0


def head_bytes(path, head='HEAD'):
    p = subprocess.run(['git', 'show', head + ':' + path], cwd=ROOT, capture_output=True)
    return p.stdout if p.returncode == 0 else None


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def load(path):
    return json.loads(Path(path).read_text())


def save(data):
    INVENTORY.parent.mkdir(parents=True, exist_ok=True)
    INVENTORY.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')


def active_cards():
    return [c for c in load(ROOT / LEDGER)['cards'] if c['status'] != 'NOT_STARTED']


def branch_summary(data):
    pending = [r['commit'] for r in data['prototype_review'] if r['decision']['action'] == 'PENDING_ROOT_DECISION']
    return {'state': 'ROOT_DECISION_REQUIRED' if pending else 'CURRENT_CANDIDATE_SELECTED',
            'selected_candidate': data['candidate']['capture_commit'], 'pending_decisions': pending,
            'required_application_merges_for_current_candidate': None if pending else [],
            'whole_branch_merge_proposals_abandoned': [r['commit'] for r in data['prototype_review'] if r['decision']['action'] == 'NOT_SELECTED_PRESERVED_FOR_BATCH'],
            'preservation': 'ORIGINAL_GIT_REFS_AND_PATCH_ENTRIES_RETAINED_FOR_SCOPED_S09; NOT_MERGED_OR_IMPLEMENTED'}


def prototype_specs():
    return [
        {'commit': '2f510469', 'card': 'R31-B06-C01', 'future_batch': 'S09',
         'purpose': 'Historical input identity/reader ABI/availability metadata and independent-confirmation references.',
         'apis': {'HistoricalInputContractR31.java': ['class HistoricalInputContractR31'],
                  'ReplayDatasetR27.java': ['inputIdentityR31(', 'assertUnmodifiedR31(', 'knownAtR31('],
                  'FrozenConfirmationR30.java': ['withInputContractR31('],
                  'FrozenConfirmationStoreR30.java': ['freezeBoundR31(', 'openContextR31(']},
         'already_in_candidate': ['Existing source pin/journal invalidation, replay legal permutations/gap segments/content hash and confirmation exposure keys remain in HEAD. This is partial foundation, not the prototype input-contract implementation.'],
         'shared_hunks': [
             {'path': APP + 'RuntimeR27.java', 'methods': 'Dataset restore/new registration; Evolution input bind; pendingConfirmation freezeBoundR31; fresh confirmation context owner close.', 'reference': 'verification/r31/p03/historical-input/C01_RUNTIME_DEFERRED_NOT_APPLIED.patch'},
             {'path': 'app/src/main/java/com/ethk3/novaonepro/SchemaDomainCoordinatorR926.java', 'methods': 'r31HistoricalInputContract + journaled migrateR31HistoricalInput; 6 typed schema objects, revision1.', 'reference': 'verification/r31/p03/historical-input/C01_SCHEMA_DEFERRED_NOT_APPLIED.patch'},
             {'path': APP + 'SchemaR27.java', 'methods': 'Startup MIGRATION call after original-input/source-journal prerequisite; no runtime CREATE.', 'reference': 'verification/r31/p03/historical-input/C01_SCHEMA_DEFERRED_NOT_APPLIED.patch'}],
         'preserve': ['Original exposure keys and confirmed policy/block/dataset accounting; do not let a new input-contract ID reset exposure.', 'ReplayDataset changes must retain current Dataset91/fork/cancel fixes rather than overwrite current Runtime.'],
         'validation_limit': 'Prototype unit/host receipts are preserved at their own commit; no claim that the full current candidate or Android integration passed.'},
        {'commit': '323ad', 'card': 'R31-B06-C01', 'future_batch': 'S09',
         'purpose': 'Documentation-only C01 paused receipt identity correction; no application source changes.',
         'apis': {}, 'already_in_candidate': [], 'shared_hunks': [], 'preserve': ['Keep exact receipt commit and its source binding; no application merge needed for this documentation-only commit.'], 'validation_limit': 'Receipt update is not a new production capability.'},
        {'commit': '07534389', 'card': 'R31-B06-C02', 'future_batch': 'S09',
         'purpose': 'Durable replay with source-fenced immutable checkpoints, parent CAS, sample/output cursor continuity and gap resets.',
         'apis': {'ReplayCheckpointR31.java': ['class ReplayCheckpointR31'], 'ReplayWorkspaceR26.java': ['ReplayCheckpointR31.commitReference'],
                  'ModelReplaySU2.java': ['replayWindowR31('], 'ModelArtifactR29.java': ['requireContinuationIdentityR31(']},
         'already_in_candidate': ['Existing CheckpointArtifacts CAS/files, checkpoint manifests, fenced candidate cursor/state hash and replay registry remain in HEAD. ReplayCheckpointR31 itself is absent from the captured candidate.'],
         'shared_hunks': [
             {'path': APP + 'LegacyGenerationR26.java', 'methods': 'Own ModelReplaySU2.Library through try-with-resources for actual replay owner lifetime.', 'reference': 'verification/r31/p03/b06-c02/ROOT_SHARED_INTEGRATION.patch'},
             {'path': APP + 'ModelEvolutionSU2.java', 'methods': 'Library immutable draws() accessor and durable.trained member for atom credit; preserve numerical formulas.', 'reference': 'verification/r31/p03/b06-c02/ROOT_SHARED_INTEGRATION.patch'},
             {'path': APP + 'FoundryCampaignR26.java', 'methods': 'Capture ReplayCheckpointR31.Input outside writer and requireSource inside original candidate CAS.', 'reference': 'verification/r31/p03/b06-c02/ROOT_SHARED_INTEGRATION.patch'}],
         'preserve': ['Original model/optimizer/codec/math/RNG; complete output-prefix plus parent/cursor atomicity.', 'Do not replace current ModelPub/Foundry/Legacy ownership protocols with the older full files.'],
         'validation_limit': 'Frozen module/native evidence does not establish current full branch integration; original SOURCE and replay prerequisites must be validated together in S09.'},
        {'commit': 'ad5884ea', 'card': 'R31-B06-C03', 'future_batch': 'S09',
         'purpose': 'Separate partial learner progress from last complete ten-position executable model, validate source and recover previous complete model; report real training epoch receipts.',
         'apis': {'AtomNetSessionR30.java': ['hasExecutableModelR31('], 'AtomNetStoreR30.java': ['ExecutableModelR31', 'requireExecutableSourceR31(', 'executableModelR31('],
                  'AtomNetRuntimeR30.java': ['publishCommittedLearnerR31('], 'AtomNetPublicationR31.java': ['hasExecutableModelR31('],
                  'ModelSurfaceStoreR30.java': ['requireExecutableSourceR31('], 'ReportSnapshotR30.java': ['atomnetCompletedTrainingEpochs']},
         'already_in_candidate': ['Existing source-fenced AtomNet publication/CAS state, complete-issue watermarks and consumption counters from prior 3402edbf/cdd9 protocols are present; they are not the prototype complete-model fallback API.'],
         'shared_hunks': [
             {'path': APP + 'RuntimeR27.java', 'methods': 'publishAtomLearnerR31(learner, ExecutableModelR31), plus hasExecutableModelR31 guard in original publishAtomState methods.', 'reference': 'verification/r31/p03/b06-c03-controls/ROOT_RUNTIME_INTEGRATION.patch'}],
         'preserve': ['Do not discard the complete-model/partial-learner distinction as mere documentation.', 'Reapply only the small ReportSnapshot epoch facts onto current timing/fault projection; do not overwrite current Runtime/Dataset/Health or reset exposure/model parameters.'],
         'validation_limit': 'Prototype own learning/partial-consumption HOST evidence is retained; branch is not an ancestor and current Runtime consumer hook is not implemented.'}
    ]


def review_prototype(spec, candidate, previous):
    commit = revision(spec['commit'])
    paths = git('diff-tree', '--no-commit-id', '--name-only', '-r', '-z', commit, '--', 'app').decode().split('\0')
    files = []
    for path in filter(None, paths):
        old, current = head_bytes(path, commit), head_bytes(path, candidate)
        tokens = spec['apis'].get(Path(path).name, [])
        text = '' if current is None else current.decode('utf-8')
        if any(token not in old.decode('utf-8') for token in tokens):
            raise ValueError('review API token not present in prototype: ' + path)
        files.append({'path': path, 'prototype_blob': git('rev-parse', commit + ':' + path).decode().strip(),
                      'prototype_sha256': digest(old), 'candidate_exists': current is not None,
                      'candidate_sha256': digest(current) if current is not None else None,
                      'candidate_bytes_identical': current == old,
                      'new_api_tokens_present_in_candidate': {token: token in text for token in tokens},
                      'diff_entry': ['git', 'diff', commit, candidate, '--', path]})
    result = {k: v for k, v in spec.items() if k not in ('apis', 'commit')}
    result.update({'commit': commit, 'commit_subject': git('show', '-s', '--format=%s', commit).decode().strip(),
                   'is_candidate_ancestor': ancestor(commit, candidate), 'app_files': files,
                   'retained_git_refs': git('for-each-ref', '--contains=' + commit, '--format=%(refname)').decode().splitlines(),
                   'retention_scope': 'EXISTING_GIT_REFS; REMOTE_AVAILABILITY_NOT_QUERIED',
                   'shared_patch_source': 'git show COMMIT:reference_path (not applied automatically)',
                   'implementation_acceptance': 'NOT_INFERRED_FROM_REVIEW_OR_OWNERSHIP'})
    result['decision'] = previous.get('decision', {'action': 'PENDING_ROOT_DECISION', 'authority': None, 'reason': None})
    return result


def generate():
    old = load(INVENTORY) if INVENTORY.exists() else {}
    candidate = revision('HEAD')
    ledger_bytes, owners_bytes = (ROOT / LEDGER).read_bytes(), (ROOT / OWNERS).read_bytes()
    cards = active_cards(); ids = {c['id'] for c in cards}
    # Targeted selection only: no auxiliary custody rows, snapshots, or full ownership export.
    primary = [f for f in json.loads(owners_bytes)['files'] if ids.intersection(f['cards'])]
    by_card = {cid: {} for cid in ids}
    for row in primary:
        for cid in ids.intersection(row['cards']):
            by_card[cid][row['path']] = {'source': OWNERS, 'purpose': row['modification_purpose'], 'declared_commits': row['source_commits']}
    files, result_cards = {}, []
    for card in cards:
        refs = by_card[card['id']]
        for change in card['actual_changes']:
            if 'path' in change:
                refs.setdefault(change['path'], {'source': LEDGER, 'purpose': change.get('methods', ''), 'declared_commits': card['commits']})
        present, absent = [], []
        for path, provenance in sorted(refs.items()):
            raw = head_bytes(path, candidate)
            if raw is None:
                absent.append({'path': path, 'record_role': 'HISTORICAL_REFERENCE_ABSENT_NOT_IMPLEMENTED', 'provenance': provenance}); continue
            present.append(path)
            if path not in files:
                files[path] = {'candidate_blob': git('rev-parse', candidate + ':' + path).decode().strip(), 'candidate_sha256': digest(raw),
                               'last_source_commit_in_candidate': git('log', '-1', '--format=%H', candidate, '--', path).decode().strip(),
                               'history_cards': [], 'implementation_acceptance': 'NOT_INFERRED_FROM_PRESENCE_OR_OWNERSHIP'}
            files[path]['history_cards'].append(card['id'])
        declared = [{'commit': value, 'resolved_commit': revision(value), 'is_candidate_ancestor': ancestor(revision(value), candidate)} for value in card['commits']]
        result_cards.append({'card_id': card['id'], 'title': card['title'], 'legacy_phase': card['phase'], 'status_at_legacy_record': card['status'],
                             'candidate_acceptance': 'NOT_INFERRED_FROM_LEGACY_STATUS', 'files': present,
                             'source_changes_at_legacy_record': card['actual_changes'], 'declared_commits_at_legacy_record': declared,
                             'unmerged_or_missing_historical_paths': absent,
                             'unimplemented_proposed_paths': [r['path'] for r in card['baseline_files'] if head_bytes(r['path'], candidate) is None],
                             'next_action_at_legacy_record': card['next_step']})
    policy = load(ROOT / 'construction/r31/NEXT_ACTION.json'); resume = policy['resume_commit']
    old_review = {r['commit']: r for r in old.get('prototype_review', [])}
    reviews = [review_prototype(s, candidate, old_review.get(revision(s['commit']), {})) for s in prototype_specs()]
    inventory = {'format': 'R312_WIP_INVENTORY_V1', 'batch': 'S00', 'scope': 'TARGETED_LEGACY_ACTIVE_CARD_CUSTODY_NOT_ALL_REPOSITORY_OWNERSHIP_OR_IMPLEMENTATION_ACCEPTANCE', 'captured_at': dt.datetime.now(dt.timezone.utc).isoformat(),
                 'candidate': {'branch': git('branch', '--show-current').decode().strip(), 'capture_commit': candidate, 'resume_commit': resume,
                               'resume_is_ancestor': ancestor(resume, candidate), 'main_source_tree': git('rev-parse', candidate + ':app/src/main').decode().strip()},
                 'historical_inputs': {'execution_ledger': LEDGER, 'execution_ledger_sha256': digest(ledger_bytes),
                                       'ownership_registry': OWNERS, 'ownership_registry_sha256': digest(owners_bytes),
                                       'legacy_statuses_are_read_only_not_r312_acceptance': True, 'active_card_count': len(cards)},
                 'cards': result_cards, 'files': files, 'prototype_review': reviews,
                 'failures': None, 'build_failures': None, 'failure_capture': {'state': 'NOT_CAPTURED_S00_BUILD_NOT_IMPORTED', 'summary_path': str(SUMMARY.relative_to(ROOT))},
                 'claims_not_made': ['Legacy BUILD_PASS means S00 accepted', 'Whole current suite passes', 'Prototype source merged', 'APK/device/endurance/natural quality PASS']}
    if old.get('failure_capture', {}).get('state') == 'CAPTURED_FROM_REAL_S00_BUILD':
        for k in ('failures', 'build_failures', 'failure_capture'): inventory[k] = old[k]
    inventory['branch_review'] = branch_summary(inventory)
    save(inventory)
    return {'cards': len(result_cards), 'unique_existing_head_files': len(files), 'prototypes': len(reviews), 'failure_capture': inventory['failure_capture']['state']}


def extract_failures(summary):
    if summary.get('format') != 'R312_BUILD_SUMMARY_V1' or summary.get('batch') != 'S00':
        raise ValueError('real R312 S00 BUILD_SUMMARY required')
    unit = [{'test': key, 'status': value['status'], 'message': value.get('message', '')}
            for key, value in sorted(summary['tests'].items()) if value['status'] in ('FAIL', 'ERROR')]
    if sum(x['status'] == 'FAIL' for x in unit) != summary['unit']['failures'] or sum(x['status'] == 'ERROR' for x in unit) != summary['unit']['errors']:
        raise ValueError('BUILD_SUMMARY unit failure counts and named cases disagree')
    build = []
    gradle = summary['gradle']
    if gradle['state'] != 'PASS':
        build.append({'kind': 'GRADLE', 'state': gradle['state'], 'exit': gradle.get('exit'), 'failed_tasks': gradle.get('failed_tasks', [])})
    for row in summary['host']:
        if row['status'] not in ('PASS', 'RETIRED'): build.append({'kind': 'HOST', **row})
    for row in summary.get('pre_build', []):
        if row.get('exit', 0): build.append({'kind': 'PRE_BUILD', **row})
    return unit, build


def stale_summary(summary):
    source = revision(summary['source_head'])
    if not ancestor(source): raise ValueError('BUILD_SUMMARY source is not a HEAD ancestor')
    paths = git('diff', '--name-only', '-z', source, 'HEAD').decode().split('\0')
    changed = [p for p in paths if p and not p.startswith(('construction/r31/r312/', 'verification/r31/r312/'))]
    if changed: raise ValueError('source changed after BUILD_SUMMARY: ' + ', '.join(changed[:8]))


def sync_failures(path):
    summary = load(path); stale_summary(summary); unit, build = extract_failures(summary); data = load(INVENTORY)
    data['failures'], data['build_failures'] = unit, build
    data['failure_capture'] = {'state': 'CAPTURED_FROM_REAL_S00_BUILD', 'summary_path': str(Path(path).resolve().relative_to(ROOT)),
                               'summary_sha256': digest(Path(path).read_bytes()), 'source_head': summary['source_head'],
                               'finished_at': summary['finished_at'], 'unit_counts': summary['unit'], 'unit_failure_count': len(unit), 'build_failure_count': len(build)}
    save(data); return {'unit_failures_captured': len(unit), 'build_failures_captured': len(build)}


def check(which):
    data = load(INVENTORY); errors = []
    if which in ('coverage', 'all'):
        expected = {c['id'] for c in active_cards()}; actual = {c['card_id'] for c in data['cards']}
        if expected != actual: errors.append('active card coverage differs: ' + str(sorted(expected ^ actual)))
        if len(actual) != len(data['cards']): errors.append('duplicate card record')
        for card in data['cards']:
            if not card['files']: errors.append('card has no current HEAD file evidence: ' + card['card_id'])
            for path in card['files']:
                raw = head_bytes(path)
                if raw is None: errors.append('file absent from HEAD: ' + path)
                elif path not in data['files'] or digest(raw) != data['files'][path]['candidate_sha256']: errors.append('candidate file bytes changed; regenerate inventory: ' + path)
    if which in ('candidate', 'all'):
        candidate = data['candidate']
        if git('branch', '--show-current').decode().strip() != candidate['branch']: errors.append('current branch differs from selected candidate branch')
        if not ancestor(candidate['resume_commit']): errors.append('resume commit is not current HEAD ancestor')
        if not ancestor(candidate['capture_commit']): errors.append('inventory capture commit is not current HEAD ancestor')
        if git('rev-parse', 'HEAD:app/src/main').decode().strip() != candidate['main_source_tree']: errors.append('application source changed since inventory capture')
        for row in data['prototype_review']:
            decision = row['decision']; action = decision['action']
            if action == 'MERGED_IN_CANDIDATE':
                if not ancestor(row['commit']): errors.append('prototype claimed merged but not ancestor: ' + row['commit'])
            elif action not in ('NOT_SELECTED_PRESERVED_FOR_BATCH', 'SUPERSEDED_BY_CURRENT_IMPLEMENTATION') or decision.get('authority') != 'ROOT_INTEGRATION_OWNER' or not decision.get('reason'):
                errors.append('prototype needs explicit integration-owner disposition: ' + row['commit'])
            elif action == 'NOT_SELECTED_PRESERVED_FOR_BATCH' and not git('for-each-ref', '--contains=' + row['commit'], '--format=%(refname)').strip():
                errors.append('preserved prototype has no live Git ref: ' + row['commit'])
    if which in ('failures', 'all'):
        capture = data['failure_capture']
        if capture['state'] != 'CAPTURED_FROM_REAL_S00_BUILD': errors.append('current real S00 BUILD_SUMMARY not captured')
        else:
            path = ROOT / capture['summary_path']; summary = load(path); stale_summary(summary); unit, build = extract_failures(summary)
            if digest(path.read_bytes()) != capture['summary_sha256'] or unit != data['failures'] or build != data['build_failures']: errors.append('inventory failures differ from S00 BUILD_SUMMARY')
    if errors: raise ValueError('; '.join(errors))
    return {'check': which, 'status': 'PASS', 'scope': 'S00_CUSTODY_OR_SUMMARY_CONSISTENCY_ONLY_NOT_IMPLEMENTATION_ACCEPTANCE', 'cards': len(data['cards']), 'files': len(data['files'])}


def decision(args):
    data = load(INVENTORY); commit = revision(args.commit); row = next(x for x in data['prototype_review'] if x['commit'] == commit)
    row['decision'] = {'action': args.action, 'authority': 'ROOT_INTEGRATION_OWNER', 'reason': args.reason,
                       'future_batch': args.future_batch, 'preserved_git_commit': commit}
    data['branch_review'] = branch_summary(data)
    save(data); return {'commit': commit, 'action': args.action}


def main():
    p = argparse.ArgumentParser(description=__doc__); sub = p.add_subparsers(dest='command', required=True)
    sub.add_parser('generate')
    c = sub.add_parser('check'); c.add_argument('--check', choices=['coverage', 'candidate', 'failures', 'all'], default='all')
    s = sub.add_parser('sync-failures'); s.add_argument('--summary', type=Path, default=SUMMARY)
    d = sub.add_parser('record-decision'); d.add_argument('--commit', required=True); d.add_argument('--action', required=True, choices=['MERGED_IN_CANDIDATE', 'NOT_SELECTED_PRESERVED_FOR_BATCH', 'SUPERSEDED_BY_CURRENT_IMPLEMENTATION']); d.add_argument('--reason', required=True); d.add_argument('--future-batch', default='S09')
    a = p.parse_args()
    try:
        result = generate() if a.command == 'generate' else check(a.check) if a.command == 'check' else sync_failures(a.summary) if a.command == 'sync-failures' else decision(a)
        print(json.dumps(result, ensure_ascii=False, indent=2))
    except (ValueError, KeyError, StopIteration, FileNotFoundError, json.JSONDecodeError) as e:
        print(json.dumps({'status': 'FAIL', 'error': str(e)}, ensure_ascii=False), file=sys.stderr); sys.exit(1)


if __name__ == '__main__': main()
