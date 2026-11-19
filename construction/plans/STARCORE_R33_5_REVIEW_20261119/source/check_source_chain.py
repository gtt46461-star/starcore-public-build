"""Compare the plan's stage inputs to the independently read formal 033 Git tree."""
import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path

BASE = 'bb7b49bbcc9266bad548ec37e71403256fc06a4d'
TREE = '91481826c9db1fb1d39439b799770c7336c83fea'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def require(condition, reason):
    if not condition:
        raise ValueError(reason)

def verify(plan, pins):
    manifest = json.loads((plan / 'control/APPLY_MANIFEST_R335.json').read_text())
    checks = json.loads((plan / 'evidence/R335_SOURCE_BASELINE_VERIFICATION.json').read_text())
    require(checks['sourceCommit'] == BASE and manifest['sourceBaseline'] == BASE, 'source identity mismatch')
    for row in checks['bindingChecks']:
        path = row['path']
        require(pins.get(path) and pins[path]['sha'] == row['expectedGitBlob'] == row['actualGitBlob'], 'source pin mismatch: ' + path)
    current = dict(pins)
    changed, stages = set(), []
    action_counts = Counter()
    for stage in manifest['batches']:
        rows = stage['files']
        for row in rows:
            path = row['path']
            require(path in pins, 'path missing in independent inventory: ' + path)
            prior = current[path]
            if row['action'] == 'ADD':
                require(prior is None, 'existing target for ADD: ' + path)
            else:
                require(row['action'] == 'REPLACE' and prior is not None, 'missing input or invalid action: ' + path)
                before = (plan / row['beforeFile']).read_bytes()
                require(blob(before) == prior['sha'] and digest(before) == row['beforeSha256'], 'preimage mismatch: ' + path)
            after = (plan / row['payload']).read_bytes()
            require(digest(after) == row['afterSha256'], 'postimage mismatch: ' + path)
            current[path] = {'sha':blob(after), 'mode':row['mode']}
            changed.add(path)
            action_counts[row['action']] += 1
        stages.append({'stage':stage['id'], 'edits':len(rows)})
    return {'sourceCommit':BASE, 'sourceTree':TREE, 'scope':'registered source blob bindings and before/after contents across every stage', 'sourceBlobBindingsVerified':len(checks['bindingChecks']), 'distinctInstallationPaths':len(changed), 'stageOperations':sum(action_counts.values()), 'actions':dict(action_counts), 'stages':stages, 'status':'PASS'}

if __name__ == '__main__':
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('plan', type=Path)
    args = cli.parse_args()
    pins = json.loads(Path(__file__).with_name('BASELINE_TREE_PATHS.json').read_text())
    print(json.dumps(verify(args.plan, pins), indent=2))
