#!/usr/bin/env python3
"""Read-only final-batch proof verification, with losslessly archived raw evidence."""
from pathlib import Path
import gzip,hashlib,json,re,subprocess
ROOT=Path(__file__).resolve().parents[3]
HERE=Path(__file__).resolve().parent
sha=lambda b:hashlib.sha256(b).hexdigest()
def raw(path):
    return path.read_bytes() if path.exists() else gzip.decompress(Path(str(path)+'.gz').read_bytes())
r=json.loads((HERE/'BATCH_RECEIPT.json').read_text())
assert subprocess.check_output(['git','show','-s','--format=%H',r['productionCommit']],cwd=ROOT,text=True).strip()==r['productionCommit']
for p,h in r['ownedSources'].items():assert sha((ROOT/p).read_bytes())==h,p
assert sha((HERE/'ROOT_CALLERS.patch').read_bytes())==r['rootPatchSha256']
for p,f in r['rootCallers']['sources'].items():assert sha((ROOT/p).read_bytes())==f['originalSha256'],p
for group in r['groups']:
    p=ROOT/group['receipt'];assert sha(p.read_bytes())==group['receiptSha256'];receipt=json.loads(p.read_text());frames=[];count=0
    for c in receipt['commands']:
        data=raw(p.parent/(c['name']+'.log'))
        assert c['exit']==c.get('expected',0),(p,c['name'])
        if 'logSha256' in c:assert sha(data)==c['logSha256'],c['name']
        count+=sum(int(x) for x in re.findall(rb'^PASS (\d+) ',data,re.M))
        frames.extend(f for f in json.loads(raw(p.parent/(c['name']+'.frames.json'))) if str(f.get('scenario','')).startswith('NEW_'))
    assert count==group['namedAssertions']
    assert len(frames)==group['physicalNewTransactions']
    assert max(f['sqlCalls'] for f in frames)==group['maxSql']
    assert max(f['bindBytes'] for f in frames)==group['maxBindBytes']
    assert max(f['totalChangedRowsIncludingTriggersAndAttemptedRollback'] for f in frames)==group['maxChangedRowsIncludingTriggersAndAttemptedRollback']
    assert all(f['allSqlIncludingBeginEnd']<=64 for f in frames)
    assert group['businessDdl0'] and group['allSql64'] and group['allDynamicBind'] and not group['sourceChangedDuringRun']
    print(group['name'],count,'assertions',len(frames),'physical tx','P99_HOST_NS',group['hostP99Nanos'])
scope=json.loads((HERE/'SOURCE_SCOPE_PROOF.json').read_text())
for p,f in scope['unchangedFiles'].items():assert sha((ROOT/p).read_bytes())==f['sha256'],p
contract=json.loads((HERE/'SCHEMA_CONTRACT.json').read_text());assert contract['exit']==0
assert sha(raw(HERE/'runs/20261105T020254Z/contract-api.log'))==contract['logSha256']
if (HERE/'ARCHIVE_INDEX.json').exists():
    archive=json.loads((HERE/'ARCHIVE_INDEX.json').read_text())
    for f in archive['files']:
        zipped=(HERE/f['gzipPath']).read_bytes();assert sha(zipped)==f['gzipSha256'];data=gzip.decompress(zipped);assert len(data)==f['originalBytes'] and sha(data)==f['originalSha256']
    print('LOSSLESS_ARCHIVE_VERIFIED',len(archive['files']))
print('PASS FINAL_FROZEN_BATCH_SELF_CHECK; unified build/lint and timing gates remain OPEN')
