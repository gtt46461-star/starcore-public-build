"""Run the reviewed P00 build checks and distinguish a build host from a delivery host."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys

ORIGINAL = 'abb96a72c0e6aa44baac049273ae606eeeb4ddbf88166a987adf124e6c55227a'
R967 = '60e4a4f535dd79126a4ab4ba1580aa8afba3cda3b53d8347dd3ee59e8379d85e'
OUTPUTS = ('app/build/generated/r967/semantic_manifest.json',
           'app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java')
APK_033 = '8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def history(repo):
    result = subprocess.run(['git', '-C', str(repo), 'rev-parse', '--is-shallow-repository'],
                            text=True, capture_output=True, check=True)
    value = result.stdout.strip()
    if value not in ('true', 'false'):
        raise ValueError('Unrecognized Git shallow status')
    return 'SHALLOW_CHECKOUT' if value == 'true' else 'FULL_HISTORY_REPORTED'


def baseline(apk):
    if apk is None or not apk.is_file():
        return 'ORIGINAL_APK_MISSING'
    return 'MATCH' if digest(apk) == APK_033 else 'ORIGINAL_APK_DIGEST_MISMATCH'


def generated(repo):
    program = repo / 'scripts/r967_semantic_identity.py'
    if digest(program) != R967:
        return 'UNREVIEWED_GENERATOR'
    verified = subprocess.run([sys.executable, '-B', str(program), '--check'],
                              cwd=repo, capture_output=True)
    if verified.returncode:
        return 'STALE_OR_INVALID_R967'
    for name in OUTPUTS:
        file = repo / name
        head = subprocess.run(['git', '-C', str(repo), 'show', 'HEAD:' + name], capture_output=True)
        if head.returncode or head.stdout != file.read_bytes():
            return 'GENERATED_BYTES_NOT_COMMITTED'
    return 'VERIFIED_AND_COMMITTED'


def run(repo, sdk, apk):
    path = repo / 'tools/r33/preflight_build.py'
    if digest(path) != ORIGINAL:
        raise ValueError('P00 preflight source differs from reviewed input')
    spec = importlib.util.spec_from_file_location('p00_original_preflight', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    checks = module.collect(repo, sdk)
    blocked = [r['id'] for r in checks['checks'] if r['status'] != 'PASS']
    git_history, original_apk = history(repo), baseline(apk)
    semantic = generated(repo)
    delivery_inputs_ready = semantic == 'VERIFIED_AND_COMMITTED' and not blocked and git_history == 'FULL_HISTORY_REPORTED' and original_apk == 'MATCH'
    return {'buildPrerequisites':checks['status'], 'blockedBuildInputs':blocked,
            'trackedR967Outputs':semantic, 'sourceBundleHistory':git_history, 'baseline033Apk':original_apk,
            'result':'READY_TO_ATTEMPT_DELIVERY' if delivery_inputs_ready else 'DELIVERY_INPUTS_INCOMPLETE',
            'signingAndDevice':'VERIFY_IN_AUTHORIZED_BUILD_AND_DEVICE_STEPS'}


if __name__ == '__main__':
    import os
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo', type=Path, required=True)
    p.add_argument('--android-sdk', type=Path, default=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT'))
    p.add_argument('--baseline-apk', type=Path)
    args = p.parse_args()
    result = run(args.repo.resolve(), args.android_sdk, args.baseline_apk)
    print(json.dumps(result, ensure_ascii=False, indent=2))
    sys.exit(0 if result['result'] == 'READY_TO_ATTEMPT_DELIVERY' else 2)
