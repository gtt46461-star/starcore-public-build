#!/usr/bin/env python3
"""Rebind explicit R29 implementation changes, preserving historical qualification invalidation.

Run in CI on the full checkout. Generated identities must be committed BEFORE the final assembly.
This records reviewed input bytes, not runtime or prediction-quality approval.

  rebind_identity.py                  pin reviewed inputs and release manifest (before bootstrap compilation)
  rebind_identity.py --classes DIR    refresh the compiled call index from DIR, then re-pin identities

The identity generators rewrite files that are themselves pinned (r82 invalidation proof, call index), so pinning
and generation are repeated until the pinned bytes no longer change.
"""
import argparse, hashlib, json, subprocess, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
BASE = 'f9bdddb97291d9d96b5aba940b31d03369c220e2'
REVIEW = ROOT / 'release/strategy_su1_reviewed_inputs.json'

def pin():
    review = json.loads(REVIEW.read_text())
    pins = {r['path']: r for r in review['inputs']}
    subprocess.run([sys.executable, 'scripts/r926_release_manifest.py', '--write'], cwd=ROOT, check=True)
    changed = subprocess.check_output(['git', 'diff', '--name-only', BASE], cwd=ROOT, text=True).splitlines()
    changed += subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard', 'app/src/main'], cwd=ROOT, text=True).splitlines()
    changed = sorted({name for name in changed if name in pins or name.startswith('app/src/main/') or name == 'app/build.gradle'})
    for name in changed:
        path = ROOT / name
        if not path.is_file(): raise SystemExit('R29_DELETION_REQUIRES_MIGRATION_REVIEW:' + name)
        role = pins.get(name, {}).get('role', 'NUMERIC' if name.endswith('.java') else 'BUILD_CONTRACT')
        pins[name] = dict(pins.get(name, {}), path=name, role=role, sha256=hashlib.sha256(path.read_bytes()).hexdigest(), rationale='R29.1 authorized implementation; no inherited qualification. Runtime validation remains separate.')
    review['inputs'] = [pins[k] for k in sorted(pins)]
    REVIEW.write_text(json.dumps(review, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
    return len(changed)

def snapshot():
    review = json.loads(REVIEW.read_text())
    return {r['path']: hashlib.sha256((ROOT / r['path']).read_bytes()).hexdigest() for r in review['inputs'] if (ROOT / r['path']).is_file()}

def main():
    a = argparse.ArgumentParser(); a.add_argument('--classes', type=Path); a = a.parse_args()
    if a.classes:
        subprocess.run([sys.executable, 'scripts/r982_source_call_asset.py', '--freeze', '--classes', str(a.classes)], cwd=ROOT, check=True)
    for attempt in range(4):
        n = pin(); before = snapshot()
        subprocess.run([sys.executable, 'scripts/prepare_strategy_su1_identity.py'], cwd=ROOT, check=True)
        if snapshot() == before: break
    else: raise SystemExit('R29_IDENTITY_NOT_CONVERGED')
    if a.classes: subprocess.run([sys.executable, 'scripts/r982_source_call_asset.py'], cwd=ROOT, check=True)
    print('R29_REBOUND_INPUTS', n, 'rounds', attempt + 1)

if __name__ == '__main__': main()
