#!/usr/bin/env python3
"""Apply exact reviewed source bytes with a repo lock and durable recovery journal.

An invocation is NOT a filesystem-wide atomic transaction. Individual replacements
are atomic; after an interruption the same batch is resumed using exact hashes.
Receipts attest source bytes only, never business tests or Android runtime quality.
Requires Python 3.9+ on a POSIX filesystem supporting flock and fsync.
"""
import argparse
import contextlib
import datetime
import hashlib
import importlib.util
import json
import os
import pathlib
import re
import stat
import sys
import tempfile
import uuid

STATE = '.starcore-plan-receipts'
HASH = re.compile(r'^[0-9a-f]{64}$')


def digest(data):
    return hashlib.sha256(data).hexdigest()


def safe(root, name):
    if not isinstance(name, str) or '\\' in name or '\x00' in name:
        raise ValueError('UNSAFE_PATH ' + str(name))
    rel = pathlib.PurePosixPath(name)
    if rel.is_absolute() or '..' in rel.parts or not rel.parts or name != rel.as_posix():
        raise ValueError('UNSAFE_PATH ' + name)
    target = root
    for part in rel.parts:
        target = target / part
        if target.is_symlink():
            raise ValueError('SYMLINK_PATH ' + name)
    if not target.resolve().is_relative_to(root.resolve()):
        raise ValueError('SYMLINK_ESCAPE ' + name)
    return target


def current_hash(path):
    if path.is_symlink():
        raise ValueError('SYMLINK_PATH ' + str(path))
    if path.exists() and not path.is_file():
        raise ValueError('NOT_FILE ' + str(path))
    return digest(path.read_bytes()) if path.exists() else None


def declared_mode(entry, before=False):
    """Exact POSIX modes are part of a declared source state.

    Old manifests without mode fields retain their original byte-only contract.
    A reviewed mode transition must declare before_mode separately. Otherwise
    an existing source is expected to have the same mode as its declared output.
    """
    key = 'before_mode' if before and 'before_mode' in entry else 'mode'
    if key not in entry or (before and entry['before_sha256'] is None):
        return None
    return int(str(entry[key])[-3:], 8)


def file_mode(path):
    current_hash(path)  # Keep the same symlink / non-file rejection as byte reads.
    return stat.S_IMODE(path.stat().st_mode) if path.exists() else None


def matches(entry, value, mode, before=False):
    expected_hash = entry['before_sha256' if before else 'after_sha256']
    expected_mode = declared_mode(entry, before)
    return value == expected_hash and (expected_mode is None or mode == expected_mode)


def sync_dir(path):
    fd = os.open(path, os.O_RDONLY | getattr(os, 'O_DIRECTORY', 0))
    try:
        os.fsync(fd)
    finally:
        os.close(fd)


def durable_mkdir(path):
    missing = []
    cursor = path
    while not cursor.exists():
        missing.append(cursor)
        cursor = cursor.parent
    for item in reversed(missing):
        item.mkdir()
        sync_dir(item.parent)


def atomic_write(path, data, mode=0o644):
    durable_mkdir(path.parent)
    if path.is_symlink():
        raise ValueError('SYMLINK_PATH ' + str(path))
    fd, temporary = tempfile.mkstemp(dir=path.parent, prefix='.starcore-write-')
    try:
        with os.fdopen(fd, 'wb') as out:
            out.write(data)
            out.flush()
            os.fchmod(out.fileno(), mode)
            os.fsync(out.fileno())
        os.replace(temporary, path)
        sync_dir(path.parent)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)


def write_json(path, value):
    atomic_write(path, (json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode())


def durable_unlink(path):
    if path.exists():
        path.unlink()
        sync_dir(path.parent)


def validate_manifest(manifest):
    batches = manifest.get('batches')
    if not isinstance(batches, dict) or not batches or not manifest.get('revision'):
        raise ValueError('MANIFEST_SCHEMA')
    owners = {}
    derived_owners = manifest.get('derived_file_owners', {})
    if not isinstance(derived_owners, dict):
        raise ValueError('DERIVED_OWNER_SCHEMA')
    derived_latest = {}
    for bid in sorted(batches, key=lambda b: int(b[1:]) if re.fullmatch(r'P[1-9][0-9]*', b) else -1):
        batch = batches[bid]
        if not re.fullmatch(r'P[1-9][0-9]*', bid):
            raise ValueError('BAD_BATCH_ID ' + bid)
        requires = batch.get('prerequisites')
        if not isinstance(requires, list) or len(set(requires)) != len(requires):
            raise ValueError('BAD_PREREQUISITES ' + bid)
        for requirement in requires:
            if requirement not in batches or int(requirement[1:]) >= int(bid[1:]):
                raise ValueError('FUTURE_OR_UNKNOWN_PREREQUISITE ' + bid)
        local_paths = set()
        for derived, entry in [(False, e) for e in batch.get('files', [])] + [(True, e) for e in batch.get('derived_files', [])]:
            path = entry['path']
            # These calls validate path syntax without assuming files exist.
            for name in (path, entry['source']):
                rel = pathlib.PurePosixPath(name)
                if ('\\' in name or '\x00' in name or rel.is_absolute() or '..' in rel.parts
                        or not rel.parts or name != rel.as_posix()):
                    raise ValueError('UNSAFE_PATH ' + name)
            if path == STATE or path.startswith(STATE + '/') or path == '.git' or path.startswith('.git/'):
                raise ValueError('RESERVED_PATH ' + path)
            if path in local_paths:
                raise ValueError('DUPLICATE_BATCH_PATH ' + path)
            local_paths.add(path)
            if derived:
                contract = derived_owners.get(path)
                if (not isinstance(contract, dict) or not contract.get('generator')
                        or contract.get('owner') not in batches or entry.get('owner') != contract['owner']
                        or int(contract['owner'][1:]) > int(bid[1:]) or path in owners):
                    raise ValueError('DERIVED_OWNER_MISMATCH ' + path)
                if path in derived_latest:
                    previous_bid, previous_hash = derived_latest[path]
                    if entry.get('before_sha256') != previous_hash or previous_bid not in ancestors(manifest, bid):
                        raise ValueError('DERIVED_CHAIN_BREAK ' + path)
                derived_latest[path] = (bid, entry.get('after_sha256'))
            elif path in owners or path in derived_owners:
                raise ValueError('DUPLICATE_OWNER ' + path)
            elif entry.get('owner', bid) != bid:
                raise ValueError('OWNER_MISMATCH ' + path)
            else:
                owners[path] = bid
            before, after, action = entry.get('before_sha256'), entry.get('after_sha256'), entry.get('action')
            if not isinstance(after, str) or not HASH.fullmatch(after):
                raise ValueError('BAD_AFTER_HASH ' + path)
            if before is not None and (not isinstance(before, str) or not HASH.fullmatch(before)):
                raise ValueError('BAD_BEFORE_HASH ' + path)
            if (action == 'ADD' and before is not None
                    or action == 'REPLACE' and (before is None or before == after)
                    or action == 'PRESERVE_REFERENCE' and before != after
                    or action not in ('ADD', 'REPLACE', 'PRESERVE_REFERENCE')):
                raise ValueError('ACTION_HASH_CONTRADICTION ' + path)
            for key in ('mode', 'before_mode'):
                if key in entry and str(entry[key]) not in ('644', '755', '100644', '100755'):
                    raise ValueError('UNSUPPORTED_MODE ' + path)
            if 'before_mode' in entry and (before is None or 'mode' not in entry):
                raise ValueError('BEFORE_MODE_WITHOUT_SOURCE_OR_AFTER_MODE ' + path)
    if set(derived_owners) != set(derived_latest):
        raise ValueError('DERIVED_OWNER_WITHOUT_STAGE')
    return owners


def batch_entries(manifest, batch):
    row = manifest['batches'][batch]
    return row['files'] + row.get('derived_files', [])


def verify_stage_identity(package, repo, manifest, batch, projected=None):
    contract = manifest['batches'][batch].get('stage_identity')
    if contract is None:
        return
    raw = safe(package, contract['snapshot']).read_bytes()
    if digest(raw) != contract['sha256']:
        raise ValueError('IDENTITY_SNAPSHOT_CORRUPT')
    snapshot = json.loads(raw)
    if (snapshot.get('schema') != 'R32_EXACT_STAGE_IDENTITY_V1'
            or snapshot.get('batch') != batch or snapshot.get('qualification_inheritance') is not False):
        raise ValueError('IDENTITY_SNAPSHOT_SCOPE')
    pins = snapshot['controlled_inputs']
    if len({p['path'] for p in pins}) != len(pins):
        raise ValueError('IDENTITY_PIN_DUPLICATE')
    for pin in pins:
        data = projected.get(pin['path']) if projected and pin['path'] in projected else safe(repo, pin['path']).read_bytes()
        if digest(data) != pin['sha256']:
            raise ValueError('UNREVIEWED_STAGE_INPUT ' + pin['path'])
    release_pins = snapshot.get('release_inputs', [])
    if len({p['path'] for p in release_pins}) != len(release_pins):
        raise ValueError('RELEASE_STAGE_PIN_DUPLICATE')
    for pin in release_pins:
        data = projected.get(pin['path']) if projected and pin['path'] in projected else safe(repo, pin['path']).read_bytes()
        if digest(data) != pin['sha256']:
            raise ValueError('UNREVIEWED_RELEASE_STAGE_INPUT ' + pin['path'])
    # Run the already byte-verified original path enumerator in memory. Loading
    # with exec avoids creating __pycache__ in a CHECK-only source worktree.
    # CHECK must reject an extra production input before any source replacement,
    # not first discover it after writing and rolling the batch back.
    generator_path = 'scripts/r967_semantic_identity.py'
    generator_file = safe(repo, generator_path)
    generator_bytes = (projected[generator_path] if projected is not None
                       and generator_path in projected else generator_file.read_bytes())
    namespace = {'__file__': str(generator_file), '__name__': 'starcore_stage_semantics'}
    exec(compile(generator_bytes, str(generator_file), 'exec'), namespace)
    enumerated = set(namespace['controlled_paths'](repo))
    expected_paths = {p['path'] for p in pins}
    if projected is not None:
        # ADD files are not physically present yet. Every declared future byte was
        # checked above; only its declared controlled paths join this projection.
        enumerated.update(path for path in projected if path in expected_paths)
    if enumerated != expected_paths:
        raise ValueError('UNREVIEWED_STAGE_PATH_SET')
    if projected is not None:
        return
    actual = namespace['build'](repo)
    if (actual['training_rule_sha256'] != snapshot['training_rule_sha256']
            or actual.get('compatible_predecessor_rules_sha256') != []):
        raise ValueError('STAGE_IDENTITY_OR_QUALIFICATION_MISMATCH')


def ancestors(manifest, batch):
    found = set()
    def visit(bid):
        for parent in manifest['batches'][bid]['prerequisites']:
            if parent not in found:
                found.add(parent)
                visit(parent)
    visit(batch)
    return sorted(found, key=lambda b: int(b[1:]))


@contextlib.contextmanager
def repo_lock(repo):
    import fcntl
    state = safe(repo, STATE)
    durable_mkdir(state)
    lock_path = safe(repo, STATE + '/.apply.lock')
    fd = os.open(lock_path, os.O_CREAT | os.O_RDWR | getattr(os, 'O_NOFOLLOW', 0), 0o600)
    try:
        try:
            fcntl.flock(fd, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('REPO_BUSY_ANOTHER_APPLY') from None
        yield state
    finally:
        os.close(fd)


def execute(package, repo, batch, apply=False):
    package, repo = pathlib.Path(package).resolve(), pathlib.Path(repo).resolve()
    if not package.is_dir() or not repo.is_dir():
        raise ValueError('DIRECTORY_MISSING')
    if apply:
        with repo_lock(repo) as state:
            return _execute(package, repo, batch, state)
    return _execute(package, repo, batch, None)


def _execute(package, repo, batch, state):
    manifest_path = safe(package, 'code_manifest.json')
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    validate_manifest(manifest)
    manifest_hash = digest(manifest_bytes)
    if batch not in manifest['batches']:
        raise ValueError('UNKNOWN_BATCH ' + batch)
    report = {'batch': batch, 'mode': 'APPLY' if state else 'CHECK', 'files': [],
              'scope': 'SOURCE_BYTES_ONLY_NOT_BUSINESS_ACCEPTANCE',
              'plan': manifest['revision'], 'manifest_sha256': manifest_hash,
              'application_tool_sha256': digest(pathlib.Path(__file__).read_bytes()),
              'applicator_contract': 'R32_5_1_BYTES_AND_EXPLICIT_POSIX_MODES_V1',
              'declared_modes_checked': sum('mode' in e for e in batch_entries(manifest, batch))}
    active = safe(repo, STATE + '/active.json') if state else None
    transaction, journal = None, None
    if active and active.exists():
        pointer = json.loads(active.read_text())
        transaction = safe(repo, STATE + '/transactions/' + pointer['transaction'])
        journal = json.loads(safe(transaction, 'journal.json').read_text())
        if journal['manifest_sha256'] != manifest_hash or journal['batch'] != batch:
            raise ValueError('UNFINISHED_TRANSACTION_RESUME ' + journal['batch'])
        if journal['state'] not in ('PREPARED', 'APPLYING', 'APPLIED', 'COMMITTED'):
            raise ValueError('RECOVERY_REQUIRES_REVIEW ' + journal['state'])
    for parent in ancestors(manifest, batch):
        for entry in manifest['batches'][parent]['files']:
            target = safe(repo, entry['path'])
            if not matches(entry, current_hash(target), file_mode(target)):
                raise ValueError('PREREQUISITE_DRIFT ' + parent + ' ' + entry['path'])
    # Derived identities have one writer and a serial hash chain; earlier stage
    # outputs are superseded deliberately, rather than checked as immutable source.
    previous_derived = {}
    for parent in ancestors(manifest, batch):
        for entry in manifest['batches'][parent].get('derived_files', []):
            previous_derived[entry['path']] = entry
    own_derived = {e['path']: e for e in manifest['batches'][batch].get('derived_files', [])}
    for path, expected in previous_derived.items():
        allowed = [expected]
        if path in own_derived:
            allowed.append(own_derived[path])
        target = safe(repo, path)
        value, mode = current_hash(target), file_mode(target)
        if not any(matches(entry, value, mode) for entry in allowed):
            raise ValueError('DERIVED_PREREQUISITE_DRIFT ' + path)
    entries, payloads, pending = {}, {}, []
    for entry in batch_entries(manifest, batch):
        target = safe(repo, entry['path'])
        data = safe(package, entry['source']).read_bytes()
        if digest(data) != entry['after_sha256']:
            raise ValueError('PACKAGE_CORRUPT ' + entry['path'])
        value = current_hash(target)
        mode = file_mode(target)
        if matches(entry, value, mode):
            status = 'PRESERVED' if matches(entry, value, mode, before=True) else 'ALREADY_APPLIED'
        elif matches(entry, value, mode, before=True):
            status = 'READY'
            pending.append(entry)
        else:
            reason = 'SOURCE_MODE_DRIFT ' if value in (entry['before_sha256'], entry['after_sha256']) else 'SOURCE_DRIFT '
            raise ValueError(reason + entry['path'])
        entries[entry['path']], payloads[entry['path']] = entry, data
        report['files'].append({'path': entry['path'], 'status': status, 'code_action': entry['action'],
                                'observed_mode': None if mode is None else oct(mode),
                                'declared_after_mode': None if declared_mode(entry) is None else oct(declared_mode(entry))})
    verify_stage_identity(package, repo, manifest, batch, projected=payloads)
    report.update(changed_files=0, pending_files=len(pending), resumed=journal is not None)
    if state:
        if journal is None and pending:
            transaction = safe(repo, STATE + '/transactions/' + batch + '-' + uuid.uuid4().hex)
            durable_mkdir(transaction)
            rows = []
            for index, entry in enumerate(pending):
                target = safe(repo, entry['path'])
                old = target.read_bytes() if target.exists() else None
                old_mode = file_mode(target)
                if not matches(entry, digest(old) if old is not None else None, old_mode, before=True):
                    raise ValueError('SOURCE_CHANGED_AFTER_CHECK ' + entry['path'])
                after_mode = declared_mode(entry)
                if after_mode is None:
                    after_mode = old_mode if old_mode is not None else 0o644
                backup = str(index) + '.before' if old is not None else None
                if backup:
                    atomic_write(safe(transaction, backup), old, old_mode)
                rows.append({'path': entry['path'], 'before_sha256': entry['before_sha256'],
                             'after_sha256': entry['after_sha256'], 'backup': backup,
                             'mode': old_mode if old_mode is not None else after_mode,
                             'before_mode': old_mode, 'after_mode': after_mode})
            journal = {'schema': 1, 'state': 'PREPARED', 'batch': batch,
                       'manifest_sha256': manifest_hash, 'rows': rows}
            write_json(safe(transaction, 'journal.json'), journal)
            write_json(active, {'transaction': transaction.name})
        if journal is not None:
            # Validate the recovery record before using it to restore any bytes.
            seen = set()
            for row in journal['rows']:
                if row['path'] in seen:
                    raise ValueError('JOURNAL_DUPLICATE_PATH')
                seen.add(row['path'])
                entry = entries.get(row['path'])
                if (entry is None or row['before_sha256'] != entry['before_sha256']
                        or row['after_sha256'] != entry['after_sha256']):
                    raise ValueError('JOURNAL_MANIFEST_MISMATCH')
                # Legacy R32.5 journals keep the old mode in `mode`. They can
                # resume only when those original modes fit the reviewed entry.
                old_mode = row.get('before_mode', row['mode'] if row['before_sha256'] is not None else None)
                new_mode = row.get('after_mode', row['mode'])
                if (type(new_mode) is not int or not 0 <= new_mode <= 0o777
                        or old_mode is not None and (type(old_mode) is not int or not 0 <= old_mode <= 0o777)
                        or declared_mode(entry) is not None and new_mode != declared_mode(entry)
                        or declared_mode(entry, True) is not None and old_mode != declared_mode(entry, True)
                        or old_mode is not None and row['mode'] != old_mode):
                    raise ValueError('JOURNAL_MODE_MISMATCH ' + row['path'])
                row['before_mode'], row['after_mode'] = old_mode, new_mode
                if row['backup']:
                    if digest(safe(transaction, row['backup']).read_bytes()) != row['before_sha256']:
                        raise ValueError('JOURNAL_BACKUP_CORRUPT ' + row['path'])
                elif row['before_sha256'] is not None:
                    raise ValueError('JOURNAL_BACKUP_MISSING ' + row['path'])
            try:
                journal['state'] = 'APPLYING'
                write_json(safe(transaction, 'journal.json'), journal)
                for row in journal['rows']:
                    target = safe(repo, row['path'])
                    value = current_hash(target)
                    mode = file_mode(target)
                    if value == row['after_sha256'] and mode == row['after_mode']:
                        continue
                    if value != row['before_sha256'] or mode != row['before_mode']:
                        raise ValueError('SOURCE_CHANGED_AFTER_CHECK ' + row['path'])
                    atomic_write(target, payloads[row['path']], row['after_mode'])
                    report['changed_files'] += 1
                for entry in entries.values():
                    target = safe(repo, entry['path'])
                    if not matches(entry, current_hash(target), file_mode(target)):
                        raise ValueError('WRITE_VERIFY ' + entry['path'])
                if digest(manifest_path.read_bytes()) != manifest_hash:
                    raise ValueError('PACKAGE_MANIFEST_CHANGED')
                verify_stage_identity(package, repo, manifest, batch)
                journal['state'] = 'APPLIED'
                write_json(safe(transaction, 'journal.json'), journal)
            except BaseException:
                conflicts = []
                for row in reversed(journal['rows']):
                    try:
                        target = safe(repo, row['path'])
                        value = current_hash(target)
                        mode = file_mode(target)
                        if value == row['before_sha256'] and mode == row['before_mode']:
                            continue
                        if value != row['after_sha256'] or mode != row['after_mode']:
                            conflicts.append(row['path'])
                            continue
                        if row['backup']:
                            atomic_write(target, safe(transaction, row['backup']).read_bytes(), row['before_mode'])
                        else:
                            durable_unlink(target)
                    except Exception:
                        conflicts.append(row['path'])
                journal['state'] = 'RECOVERY_CONFLICT' if conflicts else 'ROLLED_BACK'
                journal['conflicts'] = conflicts
                write_json(safe(transaction, 'journal.json'), journal)
                if not conflicts:
                    durable_unlink(active)
                raise
        for entry in entries.values():
            target = safe(repo, entry['path'])
            if not matches(entry, current_hash(target), file_mode(target)):
                raise ValueError('FINAL_WRITE_VERIFY ' + entry['path'])
        if digest(manifest_path.read_bytes()) != manifest_hash:
            raise ValueError('PACKAGE_MANIFEST_CHANGED')
        verify_stage_identity(package, repo, manifest, batch)
        report['at'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
        # A receipt is published only after the entire batch's post-hashes match.
        write_json(safe(repo, STATE + '/' + batch + '.json'), report)
        if journal:
            journal['state'] = 'COMMITTED'
            write_json(safe(transaction, 'journal.json'), journal)
            durable_unlink(active)
    else:
        report['at'] = datetime.datetime.now(datetime.timezone.utc).isoformat()
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--package', type=pathlib.Path, default=pathlib.Path(__file__).resolve().parents[1])
    parser.add_argument('--repo', type=pathlib.Path, required=True)
    parser.add_argument('--batch', required=True)
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    try:
        print(json.dumps(execute(args.package, args.repo, args.batch, args.apply), ensure_ascii=False, indent=2))
    except Exception as error:
        print(json.dumps({'status': 'REFUSED', 'reason': str(error)}, ensure_ascii=False), file=sys.stderr)
        sys.exit(2)
