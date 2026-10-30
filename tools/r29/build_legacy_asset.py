#!/usr/bin/env python3
"""R29-002: compact, lossless-identity copy of the preserved 1832 requirements / 299 file cards for the app.

Each requirement keeps its id, original status, owner, disposition, closure, target and the SHA-256 of its exact
text; the full text stays in the plan registry (hash-bound). --check fails when the asset drifts from the registry.
"""
import argparse, ast, hashlib, json, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'construction/plans/STARCORE_R29_FINAL_20261029/registries'
OUT = ROOT / 'app/src/main/assets/r29/legacy_preservation.json'

def sha(b): return hashlib.sha256(b).hexdigest()

def build():
    req_bytes = (PLAN / 'legacy_requirements_preserved.json').read_bytes(); card_bytes = (PLAN / 'legacy_file_cards_preserved.json').read_bytes()
    reqs = json.loads(req_bytes); cards = json.loads(card_bytes)
    r = [{'id': x['requirement_id'], 'textSha256': sha(x['exact_requirement'].encode('utf-8')), 'originalStatus': x['original_status'],
          'owner': x['r29_preservation_owner'], 'disposition': x['disposition'], 'closure': x['closure'], 'target': x['target_file'],
          'ledgerSha256': x['original_ledger_sha256']} for x in reqs]
    c = [{'id': x['card_id'], 'path': x['path'], 'disposition': x['disposition'], 'status': x['current_status'],
          'requirements': ast.literal_eval(x['requirement_ids']) if x['requirement_ids'] else []} for x in cards]
    return {'schema': 'R29_LEGACY_PRESERVATION_1', 'requirementsRegistrySha256': sha(req_bytes), 'cardsRegistrySha256': sha(card_bytes),
            'requirements': r, 'fileCards': c}

def main():
    a = argparse.ArgumentParser(); a.add_argument('--check', action='store_true'); a = a.parse_args()
    data = (json.dumps(build(), ensure_ascii=False, sort_keys=True, separators=(',', ':')) + '\n').encode('utf-8')
    if a.check:
        if not OUT.is_file() or OUT.read_bytes() != data: raise SystemExit('R29_LEGACY_ASSET_DRIFT')
    else:
        OUT.parent.mkdir(parents=True, exist_ok=True); OUT.write_bytes(data)
    print('R29_LEGACY_ASSET', 'CHECKED' if a.check else 'WRITTEN', len(data), sha(data))

if __name__ == '__main__': main()
