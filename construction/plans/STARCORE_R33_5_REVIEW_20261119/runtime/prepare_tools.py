"""Prepare a separate copy of the B01/B02/B03/B04 verification tools."""
import argparse
import hashlib
import os
import shutil
import subprocess
import tempfile
from pathlib import Path

PINS = {
    "B02/enabled_window_r335/collect_enabled_window.py": "92fbac1ecc0b20f09cc71aec167c6a7652bdf80fd5711499a195eadbe0f15dd9",
    "B01/recovery/prepare_exact_intake.py": "763ead5fcbcf47be5dffe5e36324e2db4e06093d3f86dc7d5ce001082ac238b0",
    "verification/mainline/check_trace.py": "e00d97def482c811a3cdb2de04f6481b079d5732085fdbaa58f0fbe86b6fce6a",
    "B04/tests/run_session_numerics.py": "ef7f6ed3631d6665144aa7142c134a26c10191e1becb708f9afa3abcc80a287e",
}
AFTER = {
    "B02/enabled_window_r335/collect_enabled_window.py": "7cf9bf7a01637403abcc79cd9407464bd791e4cd9e9ffbf4a40da45e62feb60c",
    "B01/recovery/prepare_exact_intake.py": "4cff9d1ee5bf181cbba329773aaf31abfce9e01ce1d0b013d43847f7f89d6a82",
    "verification/mainline/check_trace.py": "5e1b70f20d9f4f1d0db93578058e981e894fe4baa0dee00809792bc942bece7f",
    "B04/tests/run_session_numerics.py": "396e8bd4e765d287f80257e05ed79e598b595d29e069498ccdf8902c83178c75"
}
DIRECTORIES = (
    "B01/recovery", "B02/enabled_window_r335", "B03/baseline", "B03/tests",
    "B03/payload", "B03/after_full", "B04", "verification/mainline", "reference/tools/r31",
)

CALENDAR = 'reference/tools/r31/collect_device_receipts.py'
CALENDAR_SHA256 = 'c83a9e29c6893d083f20a231ff02b0e48c1231f721dfdeb8b7b81658adbdc1e7'
TRACE = 'verification/mainline/check_trace.py'
COLLECTOR = 'B02/enabled_window_r335/collect_enabled_window.py'


def verified_tool(tools, relative, expected, message):
    path = Path(tools).resolve(strict=True) / relative
    if path.resolve(strict=True) != path or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        raise ValueError(message)
    return path


def verified_trace_path(tools):
    checker = verified_tool(tools, TRACE, AFTER[TRACE], 'REVIEWED_FIXED_DEADLINE_CHECKER_REQUIRED')
    verified_tool(tools, CALENDAR, CALENDAR_SHA256, 'REVIEWED_CALENDAR_REQUIRED')
    return checker


def verified_window_path(tools):
    collector = verified_tool(tools, COLLECTOR, AFTER[COLLECTOR], 'REVIEWED_WINDOW_COLLECTOR_REQUIRED')
    verified_trace_path(tools)
    return collector


def prepare(plan, destination):
    plan = plan.resolve(strict=True)
    destination = destination.resolve()
    if destination.exists() or destination.is_relative_to(plan):
        raise ValueError("Supply a new output directory outside the sealed plan")
    for name, expected in PINS.items():
        if hashlib.sha256((plan / name).read_bytes()).hexdigest() != expected:
            raise ValueError("Wrong original tool revision: " + name)
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=destination.parent) as tmp:
        working = Path(tmp) / 'tools'
        for name in DIRECTORIES:
            shutil.copytree(plan / name, working / name, ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
        patch = Path(__file__).with_name('owner-tools.patch').resolve()
        for options in (['--check'], []):
            subprocess.run(['git', 'apply', *options, str(patch)], cwd=working, check=True,
                           env={**os.environ, 'GIT_CEILING_DIRECTORIES': str(working.parent)})
        for name, expected in AFTER.items():
            if hashlib.sha256((working / name).read_bytes()).hexdigest() != expected:
                raise ValueError('Patch did not reach ' + name)
        verified_window_path(working)
        working.rename(destination)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('destination', type=Path)
    args = parser.parse_args()
    prepare(args.plan, args.destination)
