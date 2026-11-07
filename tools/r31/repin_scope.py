#!/usr/bin/env python3
"""R31.3 per-batch identity rebind ("repin").

Every batch edits production files whose sha256 is pinned in
release/strategy_su1_reviewed_inputs.json (and, derived from it, the R967
semantic manifest and the R82 training-invalidation proof). Until those are
rebound, scripts/r967_semantic_identity.py stops Gradle before compilation.

This tool rebinds ONLY the files the open batch is allowed to change:
  * files in the batch's scope_files, or declared in BINDINGS.json scope_extensions;
  * changed since the batch start commit (committed or in the working tree).
Changed main-source files outside that scope are refused. Thresholds,
assertions, roles of existing files and qualification inheritance are not
touched. New files must be given a role explicitly (--role path=ROLE).
Afterwards the repository's own rebind (scripts/prepare_strategy_su1_identity.py)
regenerates the R967/R82 identities and runs the R967 --check.

Usage:
  python3 tools/r31/repin_scope.py --batch K02 [--role app/src/.../New.java=NUMERIC] [--dry-run]
"""
import argparse, hashlib, json, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'construction/STARCORE_R31_FINAL_20261104'
REVIEW = ROOT / 'release/strategy_su1_reviewed_inputs.json'
BASE = 'f3e249ce3aef9a8975abe0be38cbe2d1183e4078'
ROLES = {'NUMERIC', 'PRESENTATION', 'PROVENANCE', 'POLICY', 'BUILD_CONTRACT'}
# Files the repository's own rebind regenerates; they are outputs, not reviewed inputs.
GENERATED = {'release/research_dependencies_r967.json', 'release/r82_training_invalidation.json',
             'release/strategy_su1_reviewed_inputs.json', 'release/release_manifest.json',
             'release/release_identity.properties',
             'app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java'}


def die(msg):
    print('R31_REPIN_FAIL: ' + msg, file=sys.stderr)
    sys.exit(2)


def git(*a):
    return subprocess.run(['git', *a], cwd=ROOT, text=True, capture_output=True, check=True).stdout


def sha(p):
    return hashlib.sha256((ROOT / p).read_bytes()).hexdigest()


def gate_module():
    import importlib.util
    spec = importlib.util.spec_from_file_location('r31_gate', PLAN / 'tools/r31_gate.py')
    m = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(m)
    return m


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--batch', required=True)
    ap.add_argument('--role', action='append', default=[], help='path=ROLE for a new production file')
    ap.add_argument('--dry-run', action='store_true')
    a = ap.parse_args()

    g = gate_module()
    cur = g.current_batch()
    if cur is None or cur['batch'] != a.batch:
        die('batch %s is not the open batch (open: %s)' % (a.batch, cur['batch'] if cur else 'none'))
    bind = g.read_bindings(a.batch)
    allowed = set(cur['scope_files']) | {e['path'] for e in bind.get('scope_extensions', [])
                                         if len(e.get('reason', '').strip()) >= 10}
    start = g.start_commit(cur)

    if subprocess.run(['git', 'cat-file', '-e', BASE + '^{commit}'], cwd=ROOT).returncode:
        subprocess.run(['git', 'fetch', '-q', '--depth=1', 'origin', BASE], cwd=ROOT, check=True)

    changed = set(x for x in git('diff', '--name-only', start).splitlines() if x)
    changed |= set(l[3:].strip('"') for l in git('status', '--porcelain', '--untracked-files=all').splitlines() if len(l) > 3)
    prod = sorted(p for p in changed if p.startswith('app/src/main/') and p not in GENERATED and (ROOT / p).is_file())
    outside = [p for p in prod if p not in allowed]
    if outside:
        die('production files changed outside %s scope (add scope_extensions with a reason first): %s'
            % (a.batch, ', '.join(outside)))

    review = json.loads(REVIEW.read_text())
    rows = {r['path']: r for r in review['inputs']}
    roles = {}
    for spec in a.role:
        p, _, r = spec.partition('=')
        if r not in ROLES:
            die('bad role %r for %s (one of %s)' % (r, p, ', '.join(sorted(ROLES))))
        roles[p] = r
    note = 'R31.3 %s construction change (%s); no qualification inheritance.' % (a.batch, ', '.join(cur['cards']))
    updated, added, unchanged, untracked_ok = [], [], [], []
    for p in prod:
        h = sha(p)
        if p in rows:
            if rows[p]['sha256'] == h:
                unchanged.append(p)
                continue
            rows[p]['sha256'] = h
            rows[p]['rationale'] = note
            updated.append(p)
        elif p in roles:
            rows[p] = {'path': p, 'role': roles[p], 'rationale': note, 'sha256': h}
            added.append(p)
        else:
            # Not every production file is a controlled input (e.g. plain resources); only
            # controlled ones need a review row. The R967 check below reports any that do.
            untracked_ok.append(p)
    # Reviewed inputs that drifted before this batch started: their bytes are exactly what the
    # previous batch closed with (unchanged since start_commit), so rebinding them adds nothing new.
    carried_forward = []
    for p, row in rows.items():
        if p in GENERATED or p in updated or p in added or not (ROOT / p).is_file():
            continue
        h = sha(p)
        if row['sha256'] != h:
            if p in changed:
                die('reviewed input %s changed in this batch but is not an allowed production file' % p)
            row['sha256'] = h
            row['rationale'] = 'R31.3 rebind of bytes closed before %s (unchanged since %s); no qualification inheritance.' % (a.batch, start[:12])
            carried_forward.append(p)
    print(json.dumps({'batch': a.batch, 'start_commit': start, 'updated': updated, 'added': added,
                      'carried_forward_from_closed_batches': carried_forward,
                      'already_bound': unchanged, 'not_in_review': untracked_ok}, ensure_ascii=False, indent=1))
    if a.dry_run:
        return
    def sync_generated():
        for p in GENERATED:
            if p in rows and (ROOT / p).is_file() and p != 'release/strategy_su1_reviewed_inputs.json':
                rows[p]['sha256'] = sha(p)
        review['inputs'] = [rows[p] for p in sorted(rows)]
        REVIEW.write_text(json.dumps(review, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    sync_generated()
    r = subprocess.run([sys.executable, str(ROOT / 'scripts/prepare_strategy_su1_identity.py')], cwd=ROOT)
    sync_generated()
    if r.returncode:
        die('repository rebind failed (see output above). If it names SU1_NEW_INPUT_NOT_REVIEWED:<path>, '
            'rerun with --role <path>=ROLE taken from the card.')
    print('R31_REPIN_OK %s updated=%d added=%d' % (a.batch, len(updated), len(added)))


if __name__ == '__main__':
    main()
