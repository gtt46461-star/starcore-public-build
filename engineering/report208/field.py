import json,zipfile,io,hashlib
from pathlib import Path
p=next(Path('construction/plans').rglob('ORIGINAL_DIAGNOSTIC_173.zip'))
assert hashlib.sha256(p.read_bytes()).hexdigest()=='3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'
with zipfile.ZipFile(p) as outer, zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as z:
    scope=json.loads(z.read('DIAGNOSTIC_SCOPE.json'))
    for row in scope['recentEvidence']:
        n=row['table']
        if 'rows' in row and ('probe_' in n or 'long_' in n):
            sizes=[len(json.dumps(x,ensure_ascii=False)) for x in row['rows']]
            print('TABLE',n,row['totalRows'],len(sizes),max(sizes,default=0),sum(sizes))
    print('PROBE',[(k,v['frozenTargets'],v['settledTargets']) for k,v in scope['multiHorizonProbeResearch'].get('separateTargetDenominators',{}).items()])
    print('FILE_COUNT',len(z.read('FILE_STATE.jsonl').splitlines()))
    print('FAT_MEMBERS',sorted([(x.filename,x.file_size) for x in z.infolist()],key=lambda x:-x[1])[:12])
