"""Recheck missing SU1 objects from bundled SQL catalog and APK-extracted constants.
This does not independently re-extract the APK: its hash and extraction method are in evidence.
"""
from pathlib import Path
import re,json
p=Path(__file__).resolve().parents[1]/'evidence'
s=(p/'schema_actual.sql').read_text(encoding='utf-8');actual={}
for name,body in re.findall(r'^-- ([^\n]+)\n(.*?)(?=^-- |\Z)',s,re.M|re.S):actual[name]=body.strip()
expected=json.loads((p/'apk_su1_sql_constants.json').read_text(encoding='utf-8'))
missing=sorted(n for n in expected if n not in actual)
original=sorted(x['name'] for x in json.loads((p/'apk_sql_candidate_differences.json').read_text())['differences'] if x['status']=='ABSENT')
assert missing==original
print(json.dumps({'candidate_objects':len(expected),'missing_count':len(missing),'missing':missing,'scope':'catalog comparison, not full source/Android migration execution'},ensure_ascii=False,indent=2))
