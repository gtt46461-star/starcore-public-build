#!/usr/bin/env python3
"""Read-only audit of actual native run, fixed sources and lossless raw evidence."""
from pathlib import Path
import argparse,gzip,hashlib,json,re
p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=Path('/workspace/r31-integration-20261105'));a=p.parse_args()
HERE=Path(__file__).resolve().parent
sha=lambda b:hashlib.sha256(b).hexdigest()
def raw(p):return p.read_bytes() if p.exists() else gzip.decompress(Path(str(p)+'.gz').read_bytes())
proof=json.loads((HERE/'PROOF_RECEIPT.json').read_text());rpath=HERE/proof['runReceipt'];r=json.loads(rpath.read_text());assert sha(rpath.read_bytes())==proof['runReceiptSha256']
assert r['status']=='NAMED_COMPLETE_OBSERVATION_HOST_PASS_NOT_ANDROID' and not r['sourceChangedAfterSnapshot']
for path,h in r['sourceSha256'].items():
    assert sha((rpath.parent/'source_snapshot'/path).read_bytes())==h
    assert sha((a.repo/path).read_bytes())==h,'CURRENT_CANDIDATE_SOURCE_CHANGED:'+path
frames=[];assertions=0
for c in r['commands']:
    data=raw(rpath.parent/(c['name']+'.log'));assert sha(data)==c['logSha256'];assert c['exit']==c['expected']
    if c['name']=='before-claim-wait':assert b'NORMAL_UNCLAIMED_WAIT_MUST_NOT_COUNT_RECEIPT_FAILURE' in data
    assertions+=sum(map(int,re.findall(rb'^PASS (\d+) ',data,re.M)))
    frames.extend(f for f in json.loads(raw(rpath.parent/(c['name']+'.frames.json'))) if str(f.get('scenario','')).startswith('NEW_'))
assert assertions==proof['assertions']==98 and len(frames)==proof['newPhysicalTransactions']==2249
assert all(f['allSqlIncludingBeginEnd']<=64 and f['bindBytes']<=262144 for f in frames)
assert max(f['allSqlIncludingBeginEnd'] for f in frames)==proof['maxSqlIncludingBeginEnd']==14
assert max(f['bindBytes'] for f in frames)==proof['maxBindBytes']==35540
for f in frames:
    assert all(not re.match(r'^(CREATE|ALTER|DROP|REINDEX|VACUUM)\b',q['sql'].strip(),re.I) for q in f['sql'])
for entry in json.loads((HERE/'ARCHIVE_INDEX.json').read_text())['files']:
    zipped=(HERE/entry['gzipPath']).read_bytes();assert sha(zipped)==entry['gzipSha256'];data=gzip.decompress(zipped);assert sha(data)==entry['originalSha256'] and len(data)==entry['originalBytes']
print('PASS B00_ACTUAL_OBSERVATION_PIPELINE_FROZEN_SELF_CHECK assertions=98 physicalTx=2249 maxAllSql=14 maxBind=35540; Android/release/cross-process scope unchanged')
