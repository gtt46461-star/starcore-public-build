#!/usr/bin/env python3
"""Persistent R31 evidence ledger; missing execution never becomes acceptance."""
import argparse
import datetime as dt
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PLAN = ROOT / 'construction/STARCORE_R31_FINAL_20261104'
DEST = ROOT / 'construction/r31'
STATES = ('NOT_STARTED', 'IMPLEMENTING', 'CODE_COMPLETE', 'BUILD_PASS',
          'INTEGRATION_PASS', 'DEVICE_PASS', 'BLOCKED', 'FAILED')


def now():
    return dt.datetime.now(dt.timezone.utc).isoformat()


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def contract_verified(card):
    acceptance = card.get('acceptance', [])
    return bool(acceptance) and all(item.get('status') == 'PASS' and item.get('evidence')
                                    for item in acceptance)


def save(data):
    DEST.mkdir(parents=True, exist_ok=True)
    target = DEST / 'EXECUTION_LEDGER.json'
    temporary = target.with_suffix('.tmp')
    temporary.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    temporary.replace(target)


def initialize():
    target = DEST / 'EXECUTION_LEDGER.json'
    if target.exists():
        raise SystemExit('LEDGER_EXISTS: refusing to reset evidence')
    cards = json.loads((PLAN / 'ledgers/cards.json').read_text())
    data = {
        'schema': 'R31_CONSTRUCTION_EVIDENCE_V1', 'created_at': now(),
        'baseline': '5237b265a27fe5ad27fd842901c445d5f8c27749',
        'branch': git('branch', '--show-current'),
        'plan_manifest_sha256': digest(PLAN / 'MANIFEST_SHA256.json'),
        'state_values': STATES,
        'completion_rule': 'Card source acceptance requires every contract item with execution evidence; build milestones alone do not close a card. Physical/endurance/natural evidence is recorded separately.',
        'device': {'state': 'NOT_RUN', 'initial_24h': 'NOT_RUN',
                   'final_72h': 'NOT_RUN', 'observed_devices': [],
                   'blocker': 'adb devices -l returned no connected device; /dev/kvm absent'},
        'natural_quality': {'state': 'NOT_PROVEN', 'future_samples_this_build': 0,
                            'reason': 'No prospective run of this APK; historical/SHADOW/synthetic evidence does not establish natural quality.'},
        'cards': []}
    for card in cards:
        data['cards'].append({
            'id': card['id'], 'title': card['title'], 'phase': card['execution_phase'],
            'baseline': data['baseline'], 'status': 'NOT_STARTED',
            'baseline_files': [{k: f[k] for k in ('path', 'baseline_state', 'baseline_git_blob', 'baseline_sha256') if k in f} for f in card['files']],
            'actual_changes': [], 'commits': [], 'migration_versions': [],
            'tests': [], 'apk_identity': None, 'device_evidence': [],
            'acceptance': [{'contract': text, 'status': 'NOT_RUN', 'evidence': []} for text in card['acceptance']],
            'remaining': list(card['acceptance']),
            'next_step': 'Read card and source evidence; implement after predecessor integration.',
            'history': [{'at': now(), 'state': 'NOT_STARTED', 'reason': 'Initialized from immutable plan; no inherited completion claims.'}]})
    save(data)


def update(card_id, state, receipt_path):
    target = DEST / 'EXECUTION_LEDGER.json'
    data = json.loads(target.read_text())
    receipt = json.loads(Path(receipt_path).read_text())
    card = next(x for x in data['cards'] if x['id'] == card_id)
    for key in ('actual_changes', 'commits', 'migration_versions', 'tests', 'apk_identity',
                'device_evidence', 'remaining', 'next_step', 'acceptance'):
        if key in receipt:
            card[key] = receipt[key]
    if state == 'CODE_COMPLETE' and (not card['commits'] or not contract_verified(card)):
        raise SystemExit('CANNOT_DECLARE_CODE_COMPLETE_WITH_UNPROVEN_CARD_ACCEPTANCE')
    if state == 'DEVICE_PASS':
        if not card['device_evidence'] or not card['apk_identity'] or not card['commits']:
            raise SystemExit('CANNOT_CLOSE_WITHOUT_DEVICE_APK_COMMIT')
        if any(a['status'] != 'PASS' or not a['evidence'] for a in card['acceptance']):
            raise SystemExit('CANNOT_CLOSE_WITH_UNPROVEN_ACCEPTANCE')
    card['status'] = state
    card['history'].append({'at': now(), 'state': state,
                            'receipt': str(Path(receipt_path)),
                            'receipt_sha256': digest(Path(receipt_path))})
    save(data)


def summary():
    data = json.loads((DEST / 'EXECUTION_LEDGER.json').read_text())
    counts = {state: sum(x['status'] == state for x in data['cards']) for state in STATES}
    total = len(data['cards'])
    result = {'cards': total, 'states': counts,
              'mandatory_closed_cards': counts['DEVICE_PASS'],
              'mandatory_completion_rate': counts['DEVICE_PASS'] / total,
              'device': data['device'], 'natural_quality': data['natural_quality']}
    source_done = sum(x['status'] in ('CODE_COMPLETE','BUILD_PASS','INTEGRATION_PASS','DEVICE_PASS')
                      and contract_verified(x) for x in data['cards'])
    result.update(source_complete_cards=source_done, source_completion_rate=source_done/total,
                  build_milestone_cards=sum(c['status'] in ('BUILD_PASS','INTEGRATION_PASS','DEVICE_PASS') or any(h.get('state') in ('BUILD_PASS','INTEGRATION_PASS','DEVICE_PASS') for h in c.get('history',[])) for c in data['cards']),
                  engineering_integration_cards=counts['INTEGRATION_PASS']+counts['DEVICE_PASS'],
                  device_verified_cards=counts['DEVICE_PASS'],
                  completion_policy='Source milestones and engineering integration are separate from physical/endurance/natural evidence; see PROCESS_ADJUSTMENT_20261105.md')
    current_path = DEST / 'CURRENT_EXECUTION.json'
    current = json.loads(current_path.read_text()) if current_path.exists() else {}
    batches = sorted({card['id'].split('-')[1] for card in data['cards']})
    result['batch_source_acceptance'] = {
        batch: {'cards': len(group), 'contract_verified_cards': sum(contract_verified(c) for c in group),
                'state': 'PASS' if all(contract_verified(c) and c['status'] in
                       ('CODE_COMPLETE','BUILD_PASS','INTEGRATION_PASS','DEVICE_PASS') for c in group)
                       else 'OPEN'}
        for batch in batches
        for group in [[c for c in data['cards'] if c['id'].split('-')[1] == batch]]}
    started = [card for card in data['cards'] if card['status'] != 'NOT_STARTED']
    result.update(recorded_at=now(), plan_batches=len(batches),
                  started_cards=len(started), started_is_not_completed=True,
                  batches_touched=sorted({card['id'].split('-')[1] for card in started}),
                  current_phase=current.get('current_phase', 'UNKNOWN'),
                  current_phase_ordinal=current.get('current_phase_ordinal'),
                  current_scope=current.get('current_scope', []),
                  latest_pushed_checkpoint=current.get('latest_pushed_source'),
                  phases={phase: {'cards': sum(c['phase'] == phase for c in data['cards']),
                                  'states': {state: sum(c['phase'] == phase and c['status'] == state
                                                       for c in data['cards']) for state in STATES}}
                          for phase in sorted({c['phase'] for c in data['cards']})})
    print(json.dumps(result, ensure_ascii=False, indent=2))
    (DEST / 'SUMMARY.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    progress = dict(result, latest_full_unit=current.get('latest_full_unit'),
                    apk_delivery=current.get('apk_delivery'),
                    android_emulator=current.get('android_emulator'),
                    current_failures=current.get('current_failures', []))
    (DEST / 'PROGRESS.json').write_text(json.dumps(progress, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('init', 'update', 'summary'))
    parser.add_argument('--card')
    parser.add_argument('--state', choices=STATES)
    parser.add_argument('--receipt')
    args = parser.parse_args()
    if args.action == 'init':
        initialize()
    elif args.action == 'update':
        if not all((args.card, args.state, args.receipt)):
            parser.error('update needs --card --state --receipt')
        update(args.card, args.state, args.receipt)
    else:
        summary()
