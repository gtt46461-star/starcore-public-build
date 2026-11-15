#!/usr/bin/env python3
"""R29 HOST checks outside JUnit (stdlib only). Each check records its own PASS/FAIL; nothing here is device or
natural-quality evidence.

  B00  plan package integrity, preserved-ledger asset equals the plan registries, 52 cards / 11 batches / 44 metrics
       present in both the plan and the app, change ledger before-hashes equal the R27 baseline blobs.
"""
import argparse, hashlib, json, re, subprocess, sys
from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'construction/plans/STARCORE_R29_FINAL_20261029'
BASE = 'f9bdddb97291d9d96b5aba940b31d03369c220e2'

def sha(b): return hashlib.sha256(b).hexdigest()
def run(*cmd): return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)

def check_plan():
    bad = []
    for f in json.loads((PLAN / 'MANIFEST.json').read_text())['files']:
        p = PLAN / f['path']
        if not p.is_file() or sha(p.read_bytes()) != f['sha256']: bad.append(f['path'])
    return not bad, {'driftedPlanFiles': bad}

def check_legacy_asset():
    r = run(sys.executable, 'tools/r29/build_legacy_asset.py', '--check')
    return r.returncode == 0, {'output': (r.stdout + r.stderr).strip()[-300:]}

def check_registries():
    cards = json.loads((PLAN / 'registries/construction_cards.json').read_text())
    batches = json.loads((PLAN / 'registries/batches.json').read_text())
    metrics = json.loads((PLAN / 'registries/acceptance_metrics.json').read_text())
    app_defs = (ROOT / 'app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricDefinitionsR29.java').read_text()
    card_map = (ROOT / 'app/src/main/java/com/ethk3/novaonepro/strategyv2/MetricCardMapR29.java').read_text()
    missing_metrics = [m['id'] for m in metrics if '\\"id\\":\\"' + m['id'] + '\\"' not in app_defs]
    in_batches = sorted(c for b in batches for c in b['cards'])
    mapping = re.findall(r'"\|([^"]*)\|"', card_map.split('VALUES', 1)[1])
    wrong_map = [c['id'] for i, c in enumerate(cards) if i >= len(mapping) or sorted(mapping[i].split('|')) != sorted(c['metrics'])]
    ok = len(cards) == 52 and len(batches) == 11 and len(metrics) == 44 and in_batches == sorted(c['id'] for c in cards) and not missing_metrics and not wrong_map
    return ok, {'cards': len(cards), 'batches': len(batches), 'metrics': len(metrics), 'metricsMissingInApp': missing_metrics, 'cardMetricMapMismatch': wrong_map}

def check_ledger():
    bad = []; rows = 0
    for ledger in sorted((ROOT / 'construction/r29/ledger').glob('*.json')):
        for row in json.loads(ledger.read_text())['files']:
            rows += 1
            r = subprocess.run(['git', 'show', BASE + ':' + row['path']], cwd=ROOT, capture_output=True)
            before = sha(r.stdout) if r.returncode == 0 else None
            if row['before_sha256'] != before: bad.append(row['path'])
    return not bad and rows > 0, {'rows': rows, 'beforeHashMismatch': bad}

def main():
    a = argparse.ArgumentParser(); a.add_argument('--out', type=Path); a = a.parse_args()
    results = {}
    for name, fn in [('B00_PLAN_PACKAGE', check_plan), ('B00_LEGACY_ASSET', check_legacy_asset), ('B00_REGISTRIES', check_registries), ('B00_CHANGE_LEDGER', check_ledger)]:
        try: ok, detail = fn()
        except Exception as e: ok, detail = False, {'error': repr(e)}
        results[name] = dict(detail, status='PASS' if ok else 'FAIL'); print(name, results[name]['status'], json.dumps(detail, ensure_ascii=False)[:300])
    out = {'schema': 'R29_HOST_CHECKS_1', 'scope': 'HOST_ONLY_NOT_DEVICE_NOT_QUALITY', 'results': results}
    if a.out: a.out.parent.mkdir(parents=True, exist_ok=True); a.out.write_text(json.dumps(out, ensure_ascii=False, indent=2) + '\n')
    return 0 if all(r['status'] == 'PASS' for r in results.values()) else 1

if __name__ == '__main__': raise SystemExit(main())
