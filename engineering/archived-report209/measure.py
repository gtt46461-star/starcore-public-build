import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path


def describe(archive):
    members = archive.infolist()
    print("FILES", json.dumps([m.filename for m in members], ensure_ascii=False))
    print("LARGEST", [(m.filename, m.file_size, m.compress_size) for m in sorted(members, key=lambda m: -m.compress_size)[:12]])
    for m in members:
        if m.filename.rsplit("/", 1)[-1] in {"R26_REPORT.json", "R27_REPORT.json", "R30_REPORT.json", "DIAGNOSTIC_SCOPE.json"}:
            doc = json.loads(archive.read(m))
            print("STRUCTURE", m.filename, list(doc) if isinstance(doc, dict) else type(doc).__name__)
            if isinstance(doc, dict):
                for key, items in doc.items():
                    if isinstance(items, list) and items and isinstance(items[0], dict):
                        print("COLLECTION", m.filename, key, len(items), list(items[0])[:25])

path = Path(sys.argv[1])
assert hashlib.sha256(path.read_bytes()).hexdigest() == "3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86"
with zipfile.ZipFile(path) as outer:
    print("OUTER", path.stat().st_size, [(m.filename, m.file_size) for m in outer.infolist()])
    inner = outer.read(outer.namelist()[0])
    with zipfile.ZipFile(io.BytesIO(inner)) as original:
        assert original.testzip() is None
        print("HISTORICAL_REPORT", len(inner), hashlib.sha256(inner).hexdigest(), len(original.namelist()))
        describe(original)

with zipfile.ZipFile(path) as outer, zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as original:
    scope=json.loads(original.read('DIAGNOSTIC_SCOPE.json'))
    for row in scope['tables']:
        n=row['table']
        if n in {'trades','draws','app_state'} or any(s in n for s in ('settlement_task','exact_probability','predraw','authorization','genome','certificate','r922_opportunity','atom_quality','r26','r967','capture_ledger','candidate_cal_','r943_')) and row['rows']>0:
            print('FIELD_TABLE',n,row['rows'])
    for file in original.namelist():
        if file.startswith('catalog-'):
            for line in original.open(file):
                record=json.loads(line)
                if record.get('kind')!='check_result' or record.get('id') not in {'RSEC-009','RSEC-010','RSEC-011','RSEC-012'}:
                    continue
                ref=str(record.get('evidence'))
                print('FIELD_CHECK',record['id'],record.get('execution_state'),record.get('reason'),len(ref),ref[:500])
                import re
                for name in re.findall(r'report-text-[a-f0-9]{64}\.txt',ref):
                    m=original.getinfo(name)
                    print('FIELD_CHECK_TEXT',m.filename,m.file_size,m.compress_size)
