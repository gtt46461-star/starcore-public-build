#!/usr/bin/env python3
"""Differentially execute the real R31 PIT guard against its immutable Git baseline."""
import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
BASELINE = '5237b265a27fe5ad27fd842901c445d5f8c27749'
GUARD = Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/PitGuardR30.java')
HOST = ROOT / 'tools/r31/pitguard_host/PitGuardAllocationR31Host.java'
OUT = ROOT / 'verification/r31/p02/pitguard-allocation'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    started = datetime.datetime.now(datetime.timezone.utc).isoformat()
    original = subprocess.check_output(['git', 'show', f'{BASELINE}:{GUARD}'], cwd=ROOT)
    current = (ROOT / GUARD).read_bytes()
    host = HOST.read_bytes()
    java = shutil.which('java')
    javac = shutil.which('javac')
    if not java or not javac:
        raise SystemExit('JDK_NOT_IN_PATH: activate the configured build environment')
    observations = {}
    with tempfile.TemporaryDirectory(prefix='r31-pitguard-') as directory:
        for label, data in [('baseline', original), ('current', current)]:
            folder = Path(directory) / label
            folder.mkdir()
            source = folder / 'PitGuardR30.java'
            source.write_bytes(data)
            command = [javac, '-d', str(folder), str(source), str(HOST)]
            result = subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
            (OUT / f'{label}-javac.log').write_text(result.stdout)
            result.check_returncode()
            command = [java, '-Xmx512m', '-cp', str(folder), 'com.ethk3.novaonepro.strategyv2.PitGuardAllocationR31Host']
            result = subprocess.run(command, cwd=ROOT, text=True, capture_output=True)
            (OUT / f'{label}.log').write_text(result.stdout + result.stderr)
            result.check_returncode()
            observations[label] = json.loads(result.stdout)
    assert current == (ROOT / GUARD).read_bytes(), 'GUARD_CHANGED_DURING_EXECUTION'
    assert host == HOST.read_bytes(), 'HARNESS_CHANGED_DURING_EXECUTION'
    for key in ('cases', 'valid', 'violations', 'orderedViolationSha256'):
        assert observations['baseline'][key] == observations['current'][key], (key, observations)
    receipt = {
        'scope': 'P02_ACTUAL_PRODUCTION_PIT_CHECK_SEMANTICS_AND_ALLOCATION_ONLY',
        'baseline': BASELINE, 'started_utc': started,
        'finished_utc': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'production_file': str(GUARD), 'baseline_sha256': sha(original),
        'production_sha256': sha(current), 'harness_sha256': sha(host),
        'java': java, 'javac': javac, 'observations': observations,
        'ordered_semantics': 'PASS',
        'allocation_measurement': 'ACTUAL_HOST_THREAD_BYTES; NO_DEVICE_LIVE_SET_OR_RELEASE_CLAIM',
        'full_android_build': 'PENDING_ROOT_JOINT', 'device': 'NOT_RUN',
        'natural_quality': 'NOT_EVALUATED', 'forced_gc': False,
    }
    (OUT / 'RECEIPT.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(receipt, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
