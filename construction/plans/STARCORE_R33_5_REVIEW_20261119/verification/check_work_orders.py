"""Rebuild the current issue-to-source index using the same installer as the stage tool."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory

REVIEW = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(REVIEW / 'replay'), str(REVIEW / 'research'), str(REVIEW / 'runtime')]
import b05_pair_source as pair
import production_overlay
import prepare_tools
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

# These wrappers call the original tools with the current stage, candidate and evidence.
CURRENT_ENTRY = {
    'verification/contractor/stage.py': 'replay/stage_checked.py',
    'verification/score_final.py': 'replay/final_review.py',
    'verification/run_stage_prefix_r335.py': 'verification/verify_reviewed_prefix.py',
    'B08/code/verify_release_artifacts.py': 'source/verify_delivery.py',
}
STAGE_ENTRIES = {
    'P00': ('REVIEW/source/preflight_delivery.py',),
    'B01': ('TOOLS/B01/recovery/prepare_exact_intake.py', 'REVIEW/verification/check_reviewed_custody.py'),
    'B02': ('TOOLS/B02/enabled_window_r335/collect_enabled_window.py', 'REVIEW/runtime/check_window.py',
            'REVIEW/runtime/run_source_probes.py'),
    'B03': ('PLAN/B03/tests/run_replay_parity.py', 'PLAN/B03/tests/test_birth_contract.py'),
    'B04': ('TOOLS/B04/tests/run_session_numerics.py', 'REVIEW/research/b04/run_review.py'),
    'B05': ('REVIEW/research/check_research.py',),
    'B06': ('PLAN/B06/B06_自然资格与真实主链.md', 'PLAN/B06/host/run_host.py'),
    'B07': ('REVIEW/reports/closure/run_report_closure.py', 'REVIEW/research/check_research.py'),
    'B08': ('REVIEW/replay/verify_completed_source.py', 'REVIEW/replay/final_review.py',
            'REVIEW/source/verify_delivery.py'),
}
REGISTER = 'control/ISSUE_REGISTER_R335.json'
REGISTER_SHA256 = '722917671eed8369b0c478da962f3a151d709e2ea475c0318486c5cfefb8784c'


def stage_entries(stage, plan):
    result = []
    for entry in (f'PLAN/{stage}/README_R335.md', *STAGE_ENTRIES[stage]):
        root, name = entry.split('/', 1)
        row = {'root': root, 'file': name}
        if root == 'TOOLS':
            production_overlay.read_image(plan, name, prepare_tools.PINS[name])
            row['sha256'] = prepare_tools.AFTER[name]
            row['prepareFromReview'] = 'runtime/prepare_tools.py'
        elif root == 'PLAN':
            row['sha256'] = hashlib.sha256(production_overlay.safe(plan, name).read_bytes()).hexdigest()
        elif not production_overlay.safe(REVIEW, name).is_file():
            raise ValueError('Missing stage entry: ' + entry)
        result.append(row)
    return result


def material_index(plan, manifest):
    register = json.loads(production_overlay.read_image(plan, REGISTER, REGISTER_SHA256))
    active = {}
    snapshots = {}
    for batch in manifest['batches']:
        for item in batch['files']:
            active[item['path']] = (batch['id'], item)
        snapshots[batch['id']] = active.copy()

    by_issue = {}
    kinds = Counter()
    for issue in register['issues']:
        key = issue['id']
        if key in by_issue:
            raise ValueError('Duplicate work order: ' + key)
        by_issue[key] = []
        for item in issue['artifacts']:
            kind, path, digest = item['kind'], item['path'], item['sha256']
            # Verify the independent issue copy even for material stored elsewhere in PLAN.
            production_overlay.read_image(plan, item['issueCopy'], digest)
            if item.get('source'):
                production_overlay.read_image(plan, item['source'], digest)
            if item.get('beforeFile'):
                production_overlay.read_image(plan, item['beforeFile'], item['beforeSha256'])
            current = {'root': 'PLAN', 'file': item.get('source', item['issueCopy']), 'sha256': digest}
            if kind in ('PRODUCTION', 'CROSS_STAGE_REFERENCE'):
                # A later change, including a change in the issue's own batch, wins over the old reference.
                stage, record = snapshots[issue['stage']][path]
                current = {'root': 'EFFECTIVE', 'stage': stage, 'file': record['payload'],
                           'sha256': record['afterSha256']}
            elif kind == 'PRESERVED_BASELINE':
                if path in snapshots[issue['stage']]:
                    raise ValueError('Preserved baseline already has a stage operation: ' + path)
                current.update(baselinePath=item['sourceBaselinePath'], baseline=manifest['sourceBaseline'])
            elif kind == 'PLAN_TOOL' and path in prepare_tools.AFTER:
                if prepare_tools.PINS[path] != digest:
                    raise ValueError('Prepared tool input differs from the issue: ' + path)
                current = {'root': 'TOOLS', 'file': path, 'sha256': prepare_tools.AFTER[path],
                           'prepareFromReview': 'runtime/prepare_tools.py'}
            elif kind not in ('PLAN_TOOL', 'CONTRACT_OR_EVIDENCE'):
                raise ValueError('Unknown work-order artifact: ' + kind)
            if path in CURRENT_ENTRY:
                entry = CURRENT_ENTRY[path]
                if not (REVIEW / entry).is_file():
                    raise ValueError('Current review entry missing: ' + entry)
                current['runThroughReview'] = entry
            original = {'issueCopy': item['issueCopy'], 'sha256': digest}
            if item.get('source'):
                original['source'] = item['source']
            if item['installStage']:
                original['installStage'] = item['installStage']
            by_issue[key].append({'path': path, 'kind': kind, 'original': original, 'current': current})
            kinds[kind] += 1
    return register['issues'], by_issue, dict(sorted(kinds.items()))


def original_installer(plan):
    return stage_checked.pinned_module(plan, 'verification/contractor/stage.py',
        'work_order_original_stage', '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')


def build(plan, effective):
    binding = production_overlay.prepare(original_installer(plan), plan, pair, REVIEW, effective)
    manifest = json.loads((effective / 'control/APPLY_MANIFEST_R335.json').read_bytes())
    original, materials, kind_counts = material_index(plan, manifest)
    issues = []
    owners = defaultdict(set)
    for card in original:
        path = plan / card['card']
        if not path.is_file():
            raise ValueError('Original work order missing: ' + card['card'])
        issues.append({k: card[k] for k in ('id', 'stage', 'title', 'card')} |
                      {'originalCardSha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                       'materials': materials[card['id']]})
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
                       'operations': operations, 'acceptanceEntries': stage_entries(stage, plan)})
    return {'schema': 'R335_CURRENT_WORK_ORDERS_2', 'sourceBaseline': manifest['sourceBaseline'],
            'productionReview': binding, 'cardRoot': 'PLAN', 'imageRoot': 'EFFECTIVE',
            'materialCounts': kind_counts, 'originalIssueRegisterSha256': REGISTER_SHA256,
            'toolRoot': 'TOOLS from REVIEW/runtime/prepare_tools.py',
            'scope': 'Resolve current files at the issue stage. For runThroughReview use that entry with the linked original tool. Historical receipts and schemas do not sign a stage.',
            'issues': issues, 'batches': stages}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('--emit-to', type=Path, help='Retain the complete images in a new external directory')
    parser.add_argument('--write-index', action='store_true', help='Regenerate the reviewed index during plan authoring')
    args = parser.parse_args()
    plan = args.plan.resolve(strict=True)
    destination = None
    if args.emit_to:
        destination = args.emit_to.absolute()
        if destination.is_symlink() or destination.exists():
            raise ValueError('Choose a fresh output directory')
        destination = destination.resolve()
        if destination.is_relative_to(plan) or destination.is_relative_to(REVIEW):
            raise ValueError('Use a new directory outside the plan and review')
        destination.parent.mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(prefix='r335-current-orders-', dir=destination.parent if destination else None) as temp:
        effective = Path(temp) / 'effective'
        effective.mkdir()
        result = build(plan, effective)
        current = REVIEW / 'WORK_ORDERS.json'
        serialized = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
        if args.write_index:
            current.write_text(serialized, encoding='utf-8')
        if current.read_bytes() != serialized.encode('utf-8'):
            raise ValueError('Current work-order index differs from the installer')
        if destination:
            original_installer(plan).publish_no_replace(effective, destination)
        print(json.dumps({'status': 'PASS', 'issues': len(result['issues']),
                          'originalArtifactLinksVerified': sum(result['materialCounts'].values()),
                          **result['productionReview']['manifestValidation']}))

if __name__ == '__main__':
    main()
