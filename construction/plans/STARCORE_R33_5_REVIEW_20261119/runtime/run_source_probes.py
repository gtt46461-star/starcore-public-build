"""Recompile the reviewed B02 owners and run the clock and capture probes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('prefix','android-jar','java'):
        parser.add_argument('--' + key, required=True, type=Path)
    args = parser.parse_args()
    review = Path(__file__).resolve().parents[1]
    result = args.prefix.resolve() / 'prefix_final'
    stage = result / 'B02'
    receipt = json.loads((stage / 'RECEIPT.json').read_bytes())
    binding = json.loads((result / 'REVIEW_BINDING.json').read_bytes())['productionReview']
    api = args.android_jar.resolve()
    if (receipt['status'] != 'PASS' or sha(api) != receipt['androidJarSha256']
            or binding['overlaySha256'] != sha(review / 'production/OVERLAY.json')):
        raise ValueError('B02_PREFIX_OR_SDK_IDENTITY')
    inputs = json.loads((review / 'production/REVIEW_INPUTS.json').read_bytes())
    owners = []
    for row in inputs['files']:
        if row['stage'] != 'B02' or not row['path'].endswith('.java'):
            continue
        source = review / row['payload']
        if sha(source) != row['sha256'] or receipt['inputs'][row['path']] != row['sha256']:
            raise ValueError('SOURCE_OR_PREFIX_DIFFERS:' + row['path'])
        owners.append(str(source))
    java = args.java.resolve()
    with tempfile.TemporaryDirectory() as tmp:
        classes = Path(tmp) / 'owners'
        adapters = Path(tmp) / 'probe'
        classes.mkdir()
        adapters.mkdir()
        base = [str(stage/'classes'), str(api)]
        javac = [str(java.with_name('javac')), '-encoding', 'UTF-8', '--release', '17']
        subprocess.run([*javac, '-cp', os.pathsep.join(base), '-d', str(classes), *owners], check=True)
        path = [str(classes), *base]
        fixtures = sorted(str(x) for x in (review/'runtime/source_tests').rglob('*.java'))
        subprocess.run([*javac, '-cp', os.pathsep.join(path), '-d', str(adapters), *fixtures], check=True)
        subprocess.run([str(java), '-cp', os.pathsep.join([str(adapters), *path]),
                        'com.ethk3.novaonepro.SourceClockProbe'], check=True)


if __name__ == '__main__':
    main()
