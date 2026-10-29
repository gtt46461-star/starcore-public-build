#!/usr/bin/env python3
"""Rebind explicit R29 implementation changes, preserving historical qualification invalidation.

Run in CI on the full checkout. Generated identities must be committed BEFORE compiling.
This records reviewed input bytes, not runtime or prediction-quality approval.
"""
import hashlib, json, subprocess, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
BASE = 'f9bdddb97291d9d96b5aba940b31d03369c220e2'
review_path = ROOT / 'release/strategy_su1_reviewed_inputs.json'
review = json.loads(review_path.read_text())
pins = {r['path']: r for r in review['inputs']}
subprocess.run([sys.executable, 'scripts/r926_release_manifest.py', '--write'], cwd=ROOT, check=True)
changed = subprocess.check_output(['git','diff','--name-only',BASE], cwd=ROOT, text=True).splitlines()
changed = [name for name in changed if name in pins or name.startswith('app/src/main/') or name == 'app/build.gradle']
for name in changed:
    path = ROOT / name
    if not path.is_file(): raise SystemExit('R29_DELETION_REQUIRES_MIGRATION_REVIEW:' + name)
    role = pins.get(name, {}).get('role', 'NUMERIC' if name.endswith('.java') else 'BUILD_CONTRACT')
    pins[name] = dict(pins.get(name, {}), path=name, role=role, sha256=hashlib.sha256(path.read_bytes()).hexdigest(), rationale='R29.1 authorized implementation; no inherited qualification. Runtime validation remains separate.')
review['inputs'] = [pins[k] for k in sorted(pins)]
review_path.write_text(json.dumps(review, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
subprocess.run([sys.executable, 'scripts/prepare_strategy_su1_identity.py'], cwd=ROOT, check=True)
subprocess.run([sys.executable, 'scripts/r982_source_call_asset.py'], cwd=ROOT, check=True)
print('R29_REBOUND_INPUTS', len(changed))
