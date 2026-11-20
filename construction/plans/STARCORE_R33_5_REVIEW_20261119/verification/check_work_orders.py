"""Rebuild the current issue-to-source index using the same installer as the stage tool."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory

REVIEW = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(REVIEW / 'replay'), str(REVIEW / 'research')]
import b05_pair_source as pair
import production_overlay
import stage_checked

# Supplemental owners cover paths introduced or expanded by a review.
SUPPLEMENTAL = {
    'B02': {
        'AndroidManifest.xml': ['RE21'],
        'EnabledWindowCaptureProviderR335.java': ['RE21'],
        'OosAfterDrawWriterR36.java': ['RT05', 'RT06', 'C01'],
        'SettlementEngine.java': ['RT05', 'RE21'],
        'V51RuntimeOrchestrator.java': ['C01', 'RE21'],
        'FrozenFeedbackProofR335.java': ['RT05'],
        'RuntimeSchemaR26.java': ['RT05'],
        'DrawDependencyInputCP12.java': ['RT08'],
    },
    'B07': {
        'CapabilityNeedDiagnosticsR335.java': ['D10', 'D11'],
        'CapabilityNeedRegistryR335.java': ['D11'],
        'DiagnosisTraceReaderR33.java': ['D10', 'RT09'],
        'ReportObjectIndexR30.java': ['D11', 'RT09'],
        'ReportSnapshotR30.java': ['D10', 'RT09'],
        'DiagnosisProofR33.java': ['D10'],
        'RepairDiagnosisR32.java': ['D11'],
    },
}
GENERATED = {
    'app/build/generated/r967/semantic_manifest.json',
    'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java',
}

def build(plan, effective):
    tool = stage_checked.pinned_module(plan, 'verification/contractor/stage.py',
        'work_order_original_stage', '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
    binding = production_overlay.prepare(tool, plan, pair, REVIEW, effective)
    manifest = json.loads((effective / 'control/APPLY_MANIFEST_R335.json').read_bytes())
    original = json.loads((plan / 'control/ISSUE_REGISTER_R335.json').read_bytes())['issues']
    issues = []
    owners = defaultdict(set)
    for card in original:
        path = plan / card['card']
        if not path.is_file():
            raise ValueError('Original work order missing: ' + card['card'])
        issues.append({k: card[k] for k in ('id', 'stage', 'title', 'card')} |
                      {'originalCardSha256': hashlib.sha256(path.read_bytes()).hexdigest()})
        for item in card['artifacts']:
            if item['kind'] == 'PRODUCTION':
                owners[item['installStage'], item['path']].add(card['id'])
    issue_stages = {row['id']: row['stage'] for row in issues}
    stages = []
    for batch in manifest['batches']:
        stage = batch['id']
        operations = []
        for item in batch['files']:
            path = item['path']
            assigned = set(owners[stage, path])
            assigned.update('R335-' + suffix for suffix in SUPPLEMENTAL.get(stage, {}).get(Path(path).name, ()))
            if path in GENERATED:
                assigned.add('R335-ID-' + stage)
            if not assigned or not all(issue_stages.get(owner) == stage for owner in assigned):
                raise ValueError('Unowned or cross-stage operation: ' + stage + ' ' + path)
            operations.append({k: item[k] for k in ('path', 'action', 'mode', 'beforeFile', 'beforeSha256',
                                                      'payload', 'afterSha256')} | {'workOrders': sorted(assigned)})
        stages.append({'stage': stage, 'workOrders': [i['id'] for i in issues if i['stage'] == stage],
                       'operations': operations})
    return {'schema': 'R335_CURRENT_WORK_ORDERS_1', 'sourceBaseline': manifest['sourceBaseline'],
            'productionReview': binding, 'cardRoot': 'PLAN', 'imageRoot': 'EFFECTIVE',
            'scope': 'Current install images. Each original issue card also specifies its contract, tools and field acceptance.',
            'issues': issues, 'batches': stages}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('--emit-to', type=Path, help='Retain the complete images in a new external directory')
    parser.add_argument('--write-index', action='store_true', help='Regenerate the reviewed index during plan authoring')
    args = parser.parse_args()
    plan = args.plan.resolve(strict=True)
    with TemporaryDirectory(prefix='r335-current-orders-') as temp:
        effective = Path(temp)
        if args.emit_to:
            effective = args.emit_to.resolve()
            if effective.is_relative_to(plan) or effective.is_relative_to(REVIEW):
                raise ValueError('Use a new directory outside the plan and review')
            effective.mkdir(parents=True, exist_ok=False)
        result = build(plan, effective)
        current = REVIEW / 'WORK_ORDERS.json'
        serialized = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
        if args.write_index:
            current.write_text(serialized, encoding='utf-8')
        if current.read_bytes() != serialized.encode('utf-8'):
            raise ValueError('Current work-order index differs from the installer')
        print(json.dumps({'status': 'PASS', 'issues': len(result['issues']),
                          **result['productionReview']['manifestValidation']}))

if __name__ == '__main__':
    main()
