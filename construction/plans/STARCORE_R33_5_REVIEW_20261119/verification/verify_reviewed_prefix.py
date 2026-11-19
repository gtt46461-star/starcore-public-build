"""Run the original full-source prefix checks with the reviewed B05 source and R82 bindings."""
import argparse
import hashlib
from pathlib import Path
import runpy
import sys

review = Path(__file__).resolve().parents[1]
cli = argparse.ArgumentParser(add_help=False)
cli.add_argument('--plan-root', type=Path, required=True)
args, _ = cli.parse_known_args()
plan = args.plan_root.resolve()
runner = plan / 'verification/run_stage_prefix_r335.py'
if hashlib.sha256(runner.read_bytes()).hexdigest() != '9d4729a5e369c2c2da9aff35902969040b954958c2f9997100880176e891ec84':
    raise SystemExit('Unexpected release prefix checker')
sys.path[:0] = [str(plan/'verification'), str(review/'research')]
import verify_baseline_source_r335 as baseline
from b05_pair_source import generate, REL, BEFORE, AFTER
from tempfile import TemporaryDirectory
import shutil

apply_original = baseline.apply_stage_files


def apply_reviewed(plan, candidate, stage, files):
    if stage == 'B07':
        # Translate only the proven B06 identity into the original installer's
        # expected predecessor inside this disposable complete-source rehearsal.
        from b05_pair_source import generate_identity
        with TemporaryDirectory() as tmp:
            expected = generate_identity(plan, 'B06', Path(tmp))
            identity = candidate / 'release/r82_training_invalidation.json'
            if identity.read_bytes() != expected.read_bytes():
                raise ValueError('Unexpected B06 research identity')
            shutil.copyfile(plan / 'B07/before_full/release/r82_training_invalidation.json', identity)
    apply_original(plan, candidate, stage, files)
    if stage == 'B05':
        target = candidate / REL
        if hashlib.sha256(target.read_bytes()).hexdigest() != BEFORE:
            raise ValueError('B05 canonical preimage mismatch')
        with TemporaryDirectory() as tmp:
            shutil.copyfile(generate(plan, Path(tmp)), target)
    if stage in ('B05', 'B06', 'B07', 'B08'):
        if hashlib.sha256((candidate/REL).read_bytes()).hexdigest() != AFTER:
            raise ValueError('B05 reviewed source was not preserved')
        from b05_pair_source import generate_identity
        with TemporaryDirectory() as tmp:
            corrected = generate_identity(plan, stage, Path(tmp))
            identity = candidate / 'release/r82_training_invalidation.json'
            if stage in ('B06', 'B08') and identity.read_bytes() != corrected.read_bytes():
                raise ValueError('Previously committed research identity was not preserved')
            shutil.copyfile(corrected, identity)


baseline.apply_stage_files = apply_reviewed
runpy.run_path(str(runner), run_name='__main__')
