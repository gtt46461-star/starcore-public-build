"""Rebuild all source prefixes with the same effective images used by the installer."""
import argparse
import hashlib
import json
from pathlib import Path
import runpy
import sys
from tempfile import TemporaryDirectory

review = Path(__file__).resolve().parents[1]
cli = argparse.ArgumentParser(add_help=False)
cli.add_argument('--plan-root', type=Path, required=True)
cli.add_argument('--work-dir', type=Path, required=True)
args, _ = cli.parse_known_args()
plan = args.plan_root.resolve()
runner = plan / 'verification/run_stage_prefix_r335.py'
if hashlib.sha256(runner.read_bytes()).hexdigest() != '9d4729a5e369c2c2da9aff35902969040b954958c2f9997100880176e891ec84':
    raise SystemExit('Unexpected release prefix checker')
sys.path[:0] = [str(plan / 'verification'), str(review), str(review / 'replay'), str(review / 'research')]
import verify_baseline_source_r335 as baseline
import production_overlay
import stage_checked
import b05_pair_source as pair
from verify_review import verify

seal = verify(review)
tool = stage_checked.pinned_module(plan, 'verification/contractor/stage.py', 'prefix_stage_original',
    '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
apply_original = baseline.apply_stage_files
with TemporaryDirectory(prefix='r335-reviewed-prefix-images-') as temporary:
    effective = Path(temporary)
    composition = production_overlay.prepare(tool, plan, pair, review, effective)
    manifest = json.loads((effective / 'control/APPLY_MANIFEST_R335.json').read_bytes())
    baseline.validate_install_manifest(effective, manifest)
    stages = {row['id']: row['files'] for row in manifest['batches']}

    def apply_reviewed(original_plan, candidate, stage, original_files):
        if stage == 'BASELINE033':
            return apply_original(original_plan, candidate, stage, original_files)
        return apply_original(effective, candidate, stage, stages[stage])

    baseline.apply_stage_files = apply_reviewed
    runpy.run_path(str(runner), run_name='__main__')
    result = args.work_dir.resolve() / 'prefix_final'
    (result / 'EFFECTIVE_APPLY_MANIFEST.json').write_bytes((effective / 'control/APPLY_MANIFEST_R335.json').read_bytes())
    (result / 'REVIEW_BINDING.json').write_text(json.dumps({
        'reviewManifestSha256': seal, 'productionReview': composition,
        'scope': 'ORIGINAL_GENERATORS_AND_ALL_JAVA_TYPES_FOR_EVERY_EFFECTIVE_PREFIX'}, indent=2) + '\n')
