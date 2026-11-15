#!/usr/bin/env python3
"""Write the per-batch change ledger: every file changed since the R27 baseline f9bdddb with before/after SHA-256,
the cards that own it and the reason. Before-hashes are read from git, never typed by hand.

  change_ledger.py B00 --cards R29-001,R29-002,R29-003 --reason "..."   (paths come from git diff against the
  previous batch ledger's commit, or the baseline for B00)
"""
import argparse, hashlib, json, subprocess
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
BASE = 'f9bdddb97291d9d96b5aba940b31d03369c220e2'
GENERATED = ('release/', 'app/build/generated/', 'verification/r36-stubs/', 'app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java', 'evidence/')

def sha(b): return hashlib.sha256(b).hexdigest()

def main():
    a = argparse.ArgumentParser(); a.add_argument('batch'); a.add_argument('--cards', required=True); a.add_argument('--reason', required=True)
    a.add_argument('--since', default=BASE); a = a.parse_args()
    names = subprocess.check_output(['git', 'diff', '--name-only', a.since], cwd=ROOT, text=True).splitlines()
    names += subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard'], cwd=ROOT, text=True).splitlines()
    rows = []
    for name in sorted(set(names)):
        if name.startswith(GENERATED) or name.startswith('construction/r29/ledger/'): continue
        r = subprocess.run(['git', 'show', BASE + ':' + name], cwd=ROOT, capture_output=True)
        p = ROOT / name
        rows.append({'path': name, 'before_sha256': sha(r.stdout) if r.returncode == 0 else None,
                     'after_sha256': sha(p.read_bytes()) if p.is_file() else None,
                     'action': 'ADD' if r.returncode != 0 else 'DELETE' if not p.is_file() else 'MODIFY'})
    out = {'schema': 'R29_CHANGE_LEDGER_1', 'batch': a.batch, 'baseline': BASE, 'since': a.since, 'cards': a.cards.split(','), 'reason': a.reason,
           'note': 'Generated identity files are rebound by CI and excluded here. Hashes are bytes on disk at ledger time.', 'files': rows}
    path = ROOT / 'construction/r29/ledger' / (a.batch + '.json'); path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(out, ensure_ascii=False, indent=2) + '\n'); print(path, len(rows))

if __name__ == '__main__': main()
