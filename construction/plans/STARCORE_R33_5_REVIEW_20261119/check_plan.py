"""Rerun the repository corrections against an assembled plan; no Android device required."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import tempfile
from verify_review import verify

review = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('plan', type=Path)
args = parser.parse_args()
plan = args.plan.resolve(strict=True)
seal = verify(review)
with tempfile.TemporaryDirectory(prefix='r335-reviewed-host-') as temporary:
    runtime = Path(temporary) / 'tools'
    commands = [
        [plan/'verification/verify_package.py'],
        [review/'source/check_source_chain.py', plan],
        [review/'reports/verify_dual_reports.py', plan],
        [review/'runtime/prepare_tools.py', plan, runtime],
        [review/'runtime/test_review.py', runtime],
        [review/'runtime/test_settlement_window.py', runtime],
        [review/'research/test_research.py', plan],
        [review/'research/test_b05_java.py', plan],
        [review/'source/test_verify_delivery.py'],
        [review/'source/test_preflight_delivery.py'],
        [review/'research/b04/run_review.py', plan, review/'research/b04/AtomNetSessionR30.java', '--mode', 'fixed'],
        [review/'replay/test_stage_checked.py', '--plan', plan],
        [review/'replay/test_registered_prefix.py', '--plan', plan],
        [review/'replay/test_production_overlay.py', '--plan', plan],
        [review/'replay/test_export.py', '--plan', plan],
        [review/'replay/test_final_review.py'],
    ]
    for command in commands:
        print('RUN', Path(command[0]).name, flush=True)
        subprocess.run([sys.executable, '-B', *command], check=True)
    print(json.dumps({'status': 'PASS', 'scope': 'assembled package and host corrections',
                      'commands': len(commands), 'reviewManifestSha256': seal}))
