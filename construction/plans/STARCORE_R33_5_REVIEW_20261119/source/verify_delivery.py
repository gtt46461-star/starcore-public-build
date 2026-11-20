"""Resolve the release verification inputs from the candidate receipt and adb evidence."""
import argparse
import hashlib
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

import check_source_delivery
import received_build

PACKAGE = 'com.starcore.research.r13'


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'))


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def installed_code(raw):
    blocks = re.split(r'^  Package \[', raw, flags=re.M)[1:]
    matches = [b for b in blocks if b.startswith(PACKAGE + ']')]
    codes = set()
    for block in matches:
        # Package dumps may include a later hidden/disabled packages section.
        block = re.split(r'^\S', block.partition('\n')[2], maxsplit=1, flags=re.M)[0]
        codes.update(int(code) for code in re.findall(r'^\s+versionCode=(\d+)\b', block, re.M))
    if len(codes) != 1 or min(codes) <= 0:
        raise ValueError('No unambiguous installed package version in dumpsys evidence')
    return codes.pop()


def inputs(candidate, repo, dump):
    delivery = read_json(candidate / 'DELIVERY.json')
    identity = delivery['apk']
    filename = identity['file']
    if Path(filename).name != filename or not filename.endswith('.apk'):
        raise ValueError('Invalid release APK name')
    apk = candidate / filename
    if apk.is_symlink() or digest(apk) != identity['sha256']:
        raise ValueError('Release APK does not match candidate receipt')
    commit = subprocess.check_output(['git', '-C', str(repo), 'rev-parse', 'HEAD'], text=True).strip()
    manifest = json.loads(subprocess.check_output(
        ['git', '-C', str(repo), 'show', commit + ':release/release_manifest.json'], text=True
    ))
    if delivery['sourceCommit'] != commit or manifest['applicationId'] != PACKAGE:
        raise ValueError('Candidate and source identities differ')
    if (identity['versionCode'], identity['versionName']) != (manifest['versionCode'], manifest['versionName']):
        raise ValueError('Source and APK version identities differ')
    installed = installed_code(dump.read_text(encoding='utf-8'))
    return ['--apk', str(apk), '--source-commit', commit, '--version-code', str(manifest['versionCode']),
            '--version-name', manifest['versionName'], '--installed-version', str(installed),
            '--build-receipt', str(candidate / 'CI_BUILD_RECEIPT.json')]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ['plan', 'repo', 'candidate', 'dumpsys', 'baseline-apk', 'aapt', 'apksigner', 'out']:
        parser.add_argument('--' + name, required=True, type=Path)
    parser.add_argument('--delivery-zip', type=Path, help='Exact downloadable APK/SOURCE/EVIDENCE ZIP if stored separately')
    parser.add_argument('--source-out', type=Path, help='Optional receipt for the received ZIP and bundle')
    args = parser.parse_args()
    if args.source_out and (args.source_out.exists() or args.source_out.resolve() == args.out.resolve()):
        parser.error('Use a separate fresh source receipt path')
    if args.out.exists():
        parser.error('Use a new output path for this build and device observation')
    derived = inputs(args.candidate, args.repo, args.dumpsys)
    packager = check_source_delivery.packager(args.plan)
    sources = check_source_delivery.verify(packager, args.repo, args.candidate, packager.binding.BASE)
    verify = args.plan / 'B08/code/verify_release_artifacts.py'
    if digest(verify) != received_build.ORIGINAL_SIGNED_CHECKER:
        raise ValueError('UNREVIEWED_SIGNED_APK_VERIFIER')
    sources['receivedBuild'] = received_build.verify(
        packager, args.plan, args.repo, args.candidate, sources, args.delivery_zip)
    with tempfile.TemporaryDirectory(prefix='r335-signed-intake-') as tmp:
        verified = Path(tmp) / 'release.json'
        subprocess.run([sys.executable, '-B', str(verify), *derived, '--baseline-apk', str(args.baseline_apk),
                        '--aapt', str(args.aapt), '--apksigner', str(args.apksigner), '--out', str(verified)], check=True)
        received_build.match_signature(read_json(verified), read_json(args.candidate / 'DELIVERY.json'),
                                       sources['receivedBuild'])
        args.out.parent.mkdir(parents=True, exist_ok=True)
        with args.out.open('xb') as stream:
            stream.write(verified.read_bytes())
    if args.source_out:
        with args.source_out.open('x', encoding='utf-8') as stream:
            json.dump(sources, stream, indent=2)
            stream.write('\n')
    print(json.dumps({'receivedSources': sources}))


if __name__ == '__main__':
    main()
