"""Reopen nine actual stage deliveries against the completed source lineage."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys
import tempfile

from stage_checked import VERSION_FILES
from verify_completed_source import verify as completed_source

REVIEW = Path(__file__).resolve().parents[1]
STAGES = ['P00', *(f'B{i:02}' for i in range(1, 9))]
def read(path):
    return json.loads(path.read_bytes())

def sha(path):
    with path.open('rb') as f:
        return hashlib.file_digest(f, 'sha256').hexdigest()

def require(ok, reason):
    if not ok:
        raise ValueError(reason)

def git(repo, *args):
    return subprocess.check_output(['git', '-C', str(repo), *map(str, args)], stderr=subprocess.PIPE)

def validate_rows(index):
    rows = index['stages']
    require(isinstance(rows, list) and [row['stage'] for row in rows] == STAGES, 'NINE_ORDERED_DELIVERIES_REQUIRED')
    return rows

def check_stage_source(repo, stage, delivery, current, following, final, protected):
    commit = delivery['sourceCommit']
    require(isinstance(commit, str) and re.fullmatch('[0-9a-f]{40}', commit), 'INVALID_DELIVERY_COMMIT')
    git(repo, 'merge-base', '--is-ancestor', current['versionCommit'], commit)
    if following is None:
        require(commit == final, 'B08_MUST_MATCH_FINAL_HEAD')
    else:
        require(commit != following['versionCommit'], 'DELIVERY_FROM_NEXT_STAGE')
        git(repo, 'merge-base', '--is-ancestor', commit, following['versionCommit'])
    expected = json.loads(git(repo, 'show', current['versionCommit'] + ':' + VERSION_FILES[0]))
    require(expected['versionName'].startswith('R33.5-' + stage + '-')
            and (expected['versionCode'], expected['versionName']) ==
            (delivery['apk']['versionCode'], delivery['apk']['versionName']), 'DELIVERY_STAGE_VERSION:' + stage)
    if commit != current['versionCommit']:
        git(repo, 'diff', '--exit-code', '--quiet', current['versionCommit'], commit, '--', *sorted(protected))

def verify(args, work):
    repo = args.repo.resolve(strict=True)
    require(git(repo, 'rev-parse', '--is-shallow-repository').strip() == b'false', 'FULL_RELEASE_HISTORY_REQUIRED')
    plan = args.plan.resolve(strict=True)
    root = args.deliveries.resolve(strict=True).parent
    indexed = validate_rows(read(args.deliveries))
    source = completed_source(plan, repo, REVIEW)
    history = source['registeredPredecessorChecks']
    orders = read(REVIEW / 'WORK_ORDERS.json')
    require(source['productionReview'] == orders['productionReview'] and
            [row['stage'] for row in history] == STAGES, 'WORK_ORDERS_OR_VERSION_CHAIN_MISMATCH')
    common = git(repo, 'rev-parse', '--path-format=absolute', '--git-common-dir').decode().strip()
    checkout = work / 'source'
    git(work, 'clone', '--quiet', '--local', '--shared', '--no-checkout', '--no-tags', '--', common, checkout)
    protected = set(VERSION_FILES)
    seen = set()
    result = []
    for i, row in enumerate(indexed):
        stage = row['stage']
        batch = orders['batches'][i]
        require(batch['stage'] == stage, 'INDEX_STAGE_MISMATCH')
        protected.update(op['path'] for op in batch['operations'])
        candidate = (root / row['candidate']).resolve(strict=True)
        dump = (root / row['dumpsys']).resolve(strict=True)
        delivery_file = candidate / 'DELIVERY.json'
        original_sha = sha(delivery_file)
        delivery = read(delivery_file)
        check_stage_source(repo, stage, delivery, history[i], history[i+1] if i+1 < len(history) else None,
                           source['sourceCommit'], protected)
        commit = delivery['sourceCommit']
        git(checkout, 'checkout', '--quiet', '--detach', commit)
        signed, received = work / (stage + '-apk.json'), work / (stage + '-source.json')
        outer = ['--delivery-zip', str((root / row['deliveryZip']).resolve(strict=True))] if row.get('deliveryZip') else []
        subprocess.run([sys.executable, '-B', str(REVIEW / 'source/verify_delivery.py'), '--plan', str(plan),
                        '--repo', str(checkout), '--candidate', str(candidate), '--dumpsys', str(dump),
                        '--baseline-apk', str(args.baseline_apk), '--aapt', str(args.aapt),
                        '--apksigner', str(args.apksigner), '--out', str(signed), '--source-out', str(received), *outer],
                       stdout=subprocess.PIPE, check=True)
        apk = read(signed)
        artifacts = read(received)
        apk_sha = apk['apk']['sha256']
        require(sha(delivery_file) == original_sha and
                commit == apk['sourceCommit'] == artifacts['sourceCommit'] and
                apk_sha == delivery['apk']['sha256'] and apk_sha not in seen,
                'DUPLICATE_OR_MIXED_DELIVERY:' + stage)
        seen.add(apk_sha)
        result.append({'stage':stage, 'sourceCommit':commit, 'versionCode':delivery['apk']['versionCode'],
                       'apkSha256':apk_sha, 'candidateReceiptSha256':original_sha,
                       'deliveryZipSha256':artifacts['receivedBuild']['downloadSha256'],
                       **{k:artifacts[k] for k in ('sourceTree','sourceArchiveSha256','gitBundleSha256')}})
    return {'status':'VERIFIED_NINE_RECEIVED_DELIVERIES', 'stages':result}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('plan','repo','deliveries','baseline-apk','aapt','apksigner','out'):
        p.add_argument('--' + name, required=True, type=Path)
    args = p.parse_args()
    with tempfile.TemporaryDirectory(prefix='r335-nine-received-') as tmp:
        result = verify(args, Path(tmp))
    with args.out.open('x', encoding='utf-8') as f:
        json.dump(result, f, indent=2)
        f.write('\n')
    print(result['status'])

if __name__ == '__main__':
    main()
