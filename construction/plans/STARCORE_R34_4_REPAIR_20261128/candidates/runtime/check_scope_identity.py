#!/usr/bin/env python3
"""Public-source and fully synthetic structured-string contract. No user report is read."""
import argparse,json
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--repo',type=Path,required=True)
p.add_argument('--patch',type=Path,required=True)
a=p.parse_args()
s=(a.repo/'app/src/main/java/com/ethk3/novaonepro/ServiceHeartbeatStore.java').read_text();patch=a.patch.read_text()
before='this.observationScope = safe(observationScope);'
after='this.observationScope = observationScope == null ? "" : observationScope.trim();'
assert s.count(before)==1
assert '-            '+before in patch and '+            '+after in patch
assert 's.length() > 300 ? s.substring(0, 300) : s' in s
obj={'sourceCommit':'SYNTHETIC_SOURCE','databaseIdentity':'SYNTHETIC_DATABASE_'+('X'*384),'boot':'SYNTHETIC_BOOT','scope':'SYNTHETIC_FIXTURE_NOT_USER_DATA'}
value=json.dumps(obj,separators=(',',':'))
assert len(value)>300
try:json.loads(value[:300])
except json.JSONDecodeError:pass
else:raise AssertionError('Synthetic truncation must be invalid JSON')
for v in [value,'  '+value+'\n',None,'','  ']:
 out='' if v is None else v.strip()
 if v and v.strip():assert json.loads(out)==obj
 else:assert out==''
print(json.dumps({'scope':'PUBLIC_SOURCE_AND_SYNTHETIC_STRING_ONLY','cases':5,'passed':True,'user_report_read':False,'source_preimage_verified':True,'androidCompiled':False,'javaExecuted':False,'deviceMeasured':False},indent=2))
