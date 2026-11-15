#!/usr/bin/env python3
"""Read-only identification of the exact source batch to resume after interruption.

This tool never applies files, changes source identity, signs business acceptance,
creates a receipt, or treats an old PASS as current evidence. Its commands are
returned as argv arrays to avoid shell quoting ambiguities.
"""
from __future__ import annotations
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import sys
import types


def digest(data):
    return hashlib.sha256(data).hexdigest()


def load_apply(package):
    path = package/'tools/apply_batch.py'
    module = types.ModuleType('reviewed_apply_batch')
    module.__file__ = str(path)
    exec(compile(path.read_bytes(), str(path), 'exec'), module.__dict__)
    return module


@contextlib.contextmanager
def existing_read_lock(repo, apply):
    """Cooperate with an existing applicator lock without creating any file."""
    import fcntl
    path = apply.safe(repo, apply.STATE+'/.apply.lock')
    descriptor = None
    if path.exists():
        descriptor = os.open(path, os.O_RDONLY | getattr(os, 'O_NOFOLLOW', 0))
        try:
            fcntl.flock(descriptor, fcntl.LOCK_SH | fcntl.LOCK_NB)
        except BlockingIOError:
            os.close(descriptor)
            raise ValueError('REPO_BUSY_ANOTHER_APPLY') from None
    try:
        yield
    finally:
        if descriptor is not None:
            os.close(descriptor)


def _snapshot_plan(apply, manifest):
    batches = sorted(manifest['batches'], key=lambda b: int(b[1:]))
    initial, payload_entries = {}, {}
    for batch in batches:
        for entry in apply.batch_entries(manifest, batch):
            initial.setdefault(entry['path'], (entry['before_sha256'], apply.declared_mode(entry, before=True)))
            payload_entries[(batch, entry['path'])] = entry
    states = [dict(initial)]
    for batch in batches:
        state = dict(states[-1])
        for entry in apply.batch_entries(manifest, batch):
            state[entry['path']] = (entry['after_sha256'], apply.declared_mode(entry))
        states.append(state)
    return batches, states, payload_entries


def _matches_state(actual, expected):
    return actual[0] == expected[0] and (expected[1] is None or actual[1] == expected[1])


def _journal(package, repo, apply, manifest, manifest_hash):
    pointer_path = apply.safe(repo, apply.STATE+'/active.json')
    if not pointer_path.exists():
        return None, None
    pointer_bytes = pointer_path.read_bytes()
    pointer = json.loads(pointer_bytes)
    transaction = apply.safe(repo, apply.STATE+'/transactions/'+pointer['transaction'])
    journal = json.loads(apply.safe(transaction, 'journal.json').read_bytes())
    if journal.get('manifest_sha256') != manifest_hash:
        raise ValueError('UNFINISHED_OTHER_PLAN_REQUIRES_ORIGINAL_PACKAGE')
    batch = journal.get('batch')
    if batch not in manifest['batches']:
        raise ValueError('JOURNAL_UNKNOWN_BATCH')
    if journal.get('state') not in ('PREPARED', 'APPLYING', 'APPLIED', 'COMMITTED'):
        raise ValueError('RECOVERY_REQUIRES_REVIEW '+str(journal.get('state')))
    entries = {e['path']: e for e in apply.batch_entries(manifest, batch)}
    seen = set()
    for row in journal['rows']:
        path = row.get('path')
        if path in seen:
            raise ValueError('JOURNAL_DUPLICATE_PATH')
        seen.add(path)
        entry = entries.get(path)
        if (entry is None or row.get('before_sha256') != entry['before_sha256']
                or row.get('after_sha256') != entry['after_sha256']):
            raise ValueError('JOURNAL_MANIFEST_MISMATCH')
        old_mode = row.get('before_mode', row.get('mode') if row['before_sha256'] is not None else None)
        new_mode = row.get('after_mode', row.get('mode'))
        if (type(new_mode) is not int or not 0 <= new_mode <= 0o777
                or old_mode is not None and (type(old_mode) is not int or not 0 <= old_mode <= 0o777)
                or apply.declared_mode(entry) is not None and new_mode != apply.declared_mode(entry)
                or apply.declared_mode(entry, True) is not None and old_mode != apply.declared_mode(entry, True)
                or old_mode is not None and row.get('mode') != old_mode):
            raise ValueError('JOURNAL_MODE_MISMATCH '+path)
        if row.get('backup'):
            if digest(apply.safe(transaction, row['backup']).read_bytes()) != row['before_sha256']:
                raise ValueError('JOURNAL_BACKUP_CORRUPT '+path)
        elif row['before_sha256'] is not None:
            raise ValueError('JOURNAL_BACKUP_MISSING '+path)
    return journal, pointer_bytes


def inspect(package, repo):
    package, repo = Path(package).resolve(), Path(repo).resolve()
    if not package.is_dir() or not repo.is_dir():
        raise ValueError('DIRECTORY_MISSING')
    apply = load_apply(package)
    with existing_read_lock(repo, apply):
        manifest_path = apply.safe(package, 'code_manifest.json')
        manifest_bytes = manifest_path.read_bytes()
        manifest_hash = digest(manifest_bytes)
        manifest = json.loads(manifest_bytes)
        apply.validate_manifest(manifest)
        batches, states, entries = _snapshot_plan(apply, manifest)
        for entry in entries.values():
            if digest(apply.safe(package, entry['source']).read_bytes()) != entry['after_sha256']:
                raise ValueError('PACKAGE_CORRUPT '+entry['path'])
        current = {path: (apply.current_hash(apply.safe(repo, path)), apply.file_mode(apply.safe(repo, path))) for path in states[0]}
        exact = [index for index, state in enumerate(states) if all(_matches_state(current[p], state[p]) for p in current)]
        transitions = []
        if not exact:
            for index, batch in enumerate(batches, 1):
                before, after = states[index-1], states[index]
                if all(_matches_state(current[p], before[p]) or _matches_state(current[p], after[p]) for p in current):
                    transitions.append(index)
        journal, pointer_bytes = _journal(package, repo, apply, manifest, manifest_hash)
        if journal:
            index = batches.index(journal['batch'])+1
            if not (index in transitions or index in exact or index-1 in exact):
                raise ValueError('JOURNAL_SOURCE_STATE_CONFLICT')
            state = 'INTERRUPTED_BATCH'
            resume_batch = journal['batch']
            verify_batch = resume_batch
            applied_through = batches[index-1] if index in exact else (batches[index-2] if index>1 else None)
            next_batch = resume_batch
        elif exact:
            # A stage with no source changes is byte-equivalent. This is only the
            # furthest matching source state, never evidence that tests were run.
            index = max(exact)
            state = 'SOURCE_COMPLETE' if index == len(batches) else ('BASELINE_READY' if index==0 else 'STAGE_BYTES_MATCH')
            resume_batch = None
            applied_through = batches[index-1] if index else None
            next_batch = batches[index] if index<len(batches) else None
            verify_batch = applied_through or next_batch
        elif len(transitions)==1:
            index = transitions[0]
            state = 'PARTIAL_REVIEWED_BATCH_WITHOUT_JOURNAL'
            resume_batch = batches[index-1]
            verify_batch = resume_batch
            applied_through = batches[index-2] if index>1 else None
            next_batch = resume_batch
        else:
            mismatch=[]
            for path,value in current.items():
                if not any(_matches_state(value, row[path]) for row in states):
                    mismatch.append(path)
            raise ValueError('SOURCE_STATE_UNKNOWN_OR_DRIFT '+','.join(mismatch[:12]))
        # The actual packaged checker validates prerequisite source bytes,
        # payloads and controlled-input identity for the identified current batch.
        checked = apply.execute(package, repo, verify_batch, False)
        receipt_states = {}
        for batch in batches:
            receipt_path = apply.safe(repo, apply.STATE+'/'+batch+'.json')
            if not receipt_path.exists():
                receipt_states[batch]='ABSENT'
                continue
            receipt=json.loads(receipt_path.read_bytes())
            receipt_states[batch]=('MATCHING_SOURCE_RECEIPT_NOT_BUSINESS_SIGNOFF'
                if receipt.get('manifest_sha256')==manifest_hash
                and receipt.get('scope')=='SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE'
                else 'STALE_OR_DIFFERENT_SCOPE_NOT_EVIDENCE')
        after={path: (apply.current_hash(apply.safe(repo,path)),apply.file_mode(apply.safe(repo,path))) for path in current}
        if current != after or digest(manifest_path.read_bytes()) != manifest_hash:
            raise ValueError('SOURCE_OR_PACKAGE_CHANGED_DURING_INSPECTION')
        active_path=apply.safe(repo,apply.STATE+'/active.json')
        after_pointer=active_path.read_bytes() if active_path.exists() else None
        if after_pointer!=pointer_bytes:
            raise ValueError('TRANSACTION_CHANGED_DURING_INSPECTION')
        commands=[]
        if resume_batch or state=='BASELINE_READY':
            batch=resume_batch or next_batch
            base=[sys.executable,str(package/'tools/apply_batch.py'),'--package',str(package),'--repo',str(repo),'--batch',batch]
            commands.extend([{'purpose':'CHECK_IDENTIFIED_BATCH','argv':base},
                             {'purpose':'APPLY_OR_RESUME_IDENTIFIED_BATCH','argv':base+['--apply']}])
        if verify_batch:
            commands.append({'purpose':'REGENERATE_AND_VERIFY_CURRENT_STAGE_BEFORE_TESTS',
                'argv':[sys.executable,str(package/'tools/verify_stage_identity.py'),'--package',str(package),
                        '--repo',str(repo),'--batch',verify_batch,'--generate']})
        return {'schema':'R32_READ_ONLY_RESUME_STATUS_V1','status':state,
            'scope':'SOURCE_BYTES_DECLARED_MODES_AND_DURABLE_TRANSACTION_ONLY','manifest_sha256':manifest_hash,
            'plan_revision':manifest['revision'],'furthest_matching_source_batch':applied_through,
            'byte_equivalent_stage_matches':[batches[i-1] if i else 'BASELINE' for i in exact],
            'resume_batch':resume_batch,'next_source_batch':next_batch,
            'verify_current_batch_before_advancing':verify_batch,'current_batch_contract':'batches/'+verify_batch+'.md',
            'pending_source_files_for_identified_batch':checked['pending_files'],
            'transaction_state':journal['state'] if journal else None,
            'source_receipts':receipt_states,'commands':commands,
            'next_step':'Run the identified current-stage identity and batch regression contract. Advance only after recording its actual result; old receipts do not sign new business acceptance.',
            'writes_performed':0,'business_acceptance':'NOT_EVALUATED',
            'android_device_status':'NOT_EVALUATED','natural_quality':'NOT_EVALUATED'}


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package',type=Path,required=True)
    parser.add_argument('--repo',type=Path,required=True)
    args=parser.parse_args()
    try:
        print(json.dumps(inspect(args.package,args.repo),ensure_ascii=False,indent=2))
        return 0
    except Exception as error:
        print(json.dumps({'status':'REFUSED','reason':str(error),'writes_performed':0,
                          'scope':'READ_ONLY_SOURCE_RESUME_INSPECTION'},ensure_ascii=False),file=sys.stderr)
        return 2


if __name__=='__main__':
    sys.exit(main())
