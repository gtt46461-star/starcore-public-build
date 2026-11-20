"""Verify all completed version snapshots and retained files through the final stage."""
import argparse
import json
from pathlib import Path
from types import SimpleNamespace
from tempfile import TemporaryDirectory

import stage_checked as checked
import production_overlay


def verify(plan, repo, review, receipt=None):
    plan, repo, review = (Path(p).resolve() for p in (plan, repo, review))
    tool = checked.pinned_module(plan, 'verification/contractor/stage.py', 'completed_original_stage',
        '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
    generator = checked.pinned_module(plan, 'P00/reference/scripts/r926_release_manifest.py', 'completed_versions',
        '5de2507c327c55ef381b0c7e254de11f1fdd7dffc1109c1b971a7b59a7a58607')
    pair = checked.load_module(review / 'research/b05_pair_source.py', 'completed_pair')
    history_reader = SimpleNamespace(STAGES=(*tool.STAGES, 'FINAL'), git=tool.git,
                                     committed_file=tool.committed_file)
    head, history = checked.ancestry(history_reader, generator, repo, 'FINAL', tool.BASE)
    with TemporaryDirectory(prefix='r335-final-source-') as temporary:
        effective = Path(temporary)
        composition = production_overlay.prepare(tool, plan, pair, review, effective)
        if receipt is not None and receipt.get('productionReview') != composition:
            raise ValueError('FINAL_PRODUCTION_REVIEW_DIFFERS_FROM_STAGE_RECEIPT')
        active = checked.load_module(plan / 'verification/contractor/stage.py', 'completed_effective_stage')
        active.PACKAGE = effective
        active.LEDGER = effective / 'control' / tool.LEDGER.name
        rows = checked.registered_prefix(active, plan, repo, history, pair, 'B08', effective=True)
        return {'sourceCommit': head, 'productionReview': composition,
                'completedVersions': [{'stage': s, 'versionCode': v, 'commit': c} for s, v, c in history],
                'registeredPredecessorChecks': rows, 'scope': 'COMMITTED_SOURCE_ONLY_NOT_APK_DEVICE_OR_QUALIFICATION'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('plan', 'repo', 'review'):
        parser.add_argument('--' + name, required=True, type=Path)
    args = parser.parse_args()
    seal = checked.load_module(args.review / 'verify_review.py', 'completed_seal').verify(args.review)
    result = verify(args.plan, args.repo, args.review)
    result['reviewManifestSha256'] = seal
    print(json.dumps(result, ensure_ascii=False, indent=2))
