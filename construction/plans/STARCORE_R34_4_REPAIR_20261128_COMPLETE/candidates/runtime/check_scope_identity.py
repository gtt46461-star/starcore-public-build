#!/usr/bin/env python3
"""Recorded JSON truncation + source-contract/string fixture; no Java/device execution."""
import argparse
import json
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('--repo', type=Path, required=True)
p.add_argument('--report', type=Path, required=True)
p.add_argument('--patch', type=Path, required=True)
args = p.parse_args()
source = (args.repo / 'app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java').read_text()
patch = args.patch.read_text()
before = 'this.observationScope = safe(observationScope);'
after = 'this.observationScope = observationScope == null ? "" : observationScope.trim();'
assert source.count(before) == 1, 'source preimage changed'
assert '-            ' + before in patch, 'patch does not remove the exact old assignment'
assert '+            ' + after in patch, 'patch does not preserve the structured string'
assert 's.length() > 300 ? s.substring(0, 300) : s' in source, 'recorded display cap changed'
observed = json.loads((args.report / 'runtime-observation.json').read_text())
recorded = observed['r341BusinessScope']
assert len(recorded) == 300
try:
    json.loads(recorded)
except json.JSONDecodeError:
    recorded_valid = False
else:
    raise AssertionError('recorded scope unexpectedly parses; inspect new evidence')

# This is an explicit synthetic envelope assembled from known fields, not recovered bytes
# of the missing historical business envelope.
envelope = json.loads(observed['r341ProcessStartScope'])
manifest = json.loads((args.report / 'source-manifest.json').read_text())
envelope['databaseIdentity'] = manifest['r341SqlObservationScope']['databaseIdentity']
text = json.dumps(envelope, separators=(',', ':'))
assert len(text) > 300
try:
    json.loads(text[:300])
except json.JSONDecodeError:
    pass
else:
    raise AssertionError('fixture must expose structured truncation')
for value in [text, '  ' + text + '\n', None, '', '  ']:
    projected = '' if value is None else value.strip()
    if value and value.strip():
        assert json.loads(projected) == envelope
    else:
        assert projected == ''
print(json.dumps({
    'scope': 'RECORDED_BUG_SOURCE_CONTRACT_AND_SYNTHETIC_STRING_ROUND_TRIP',
    'recordedScopeLength': len(recorded), 'recordedScopeValidJson': recorded_valid,
    'candidateRoundTripLength': len(text), 'candidateRoundTripEqual': True,
    'cases': 5, 'sourceContractVerified': True,
    'androidCompiled': False, 'javaExecuted': False, 'deviceMeasured': False,
}, ensure_ascii=False, indent=2))
