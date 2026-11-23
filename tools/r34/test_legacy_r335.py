"""Resolve all R33.5 issue IDs against the current R34 owner and assertion registries."""
import argparse
import json
from pathlib import Path


def check(plan, legacy):
    old = json.loads((legacy / 'WORK_ORDERS.json').read_bytes())
    owners = json.loads((plan / 'WORK_ORDERS.json').read_bytes())['issues']
    new_ids = {r['id'] for r in owners}
    entries = json.loads((plan / 'R335_CROSSWALK.json').read_bytes())['requirements']
    old_ids = {r['id'] for r in old['issues']}
    if len(entries) != 66 or len(old_ids) != 66 or {r['legacy_id'] for r in entries} != old_ids:
        raise ValueError('R34_LEGACY_ISSUE_SET')
    for r in entries:
        if not r['new_issue_ids'] or not set(r['new_issue_ids']) <= new_ids:
            raise ValueError('R34_LEGACY_OWNER:' + r['legacy_id'])
        if not r['mandatory_assertion_ids'] or not r['test_contracts'] or not r['retained_contract']:
            raise ValueError('R34_LEGACY_TEST_CONTRACT:' + r['legacy_id'])
    return {'retainedIssueMappings':len(entries),'newOwnerIssues':len(new_ids),
            'scope':'CHECKED_SOURCE_CONTRACT_LINKS'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', required=True, type=Path)
    parser.add_argument('--legacy', required=True, type=Path)
    args = parser.parse_args()
    print(json.dumps(check(args.plan,args.legacy)))
