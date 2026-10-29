#!/usr/bin/env python3
"""Recompute selected report facts. Input may be outer ZIP, FULL_REPORT ZIP, or extracted folder."""
import argparse, collections, hashlib, io, json, re, zipfile
from pathlib import Path

def reader(path):
    if path.is_dir(): return lambda n:(path/n).read_bytes()
    z=zipfile.ZipFile(path)
    nested=[n for n in z.namelist() if n.endswith('STARCORE_FULL_REPORT.zip')]
    if nested:z=zipfile.ZipFile(io.BytesIO(z.read(nested[0])))
    bad=z.testzip()
    if bad:raise ValueError('Corrupt ZIP member: '+bad)
    return z.read

def run(path):
    read=reader(path)
    r=json.loads(read('R26_REPORT.json')); s=json.loads(read('DIAGNOSTIC_SCOPE.json'))
    chapters={}
    for m in re.finditer(r'## (.*?)\n(.*?)(?=\n## |\Z)',read('R27_REPORT.md').decode(),re.S):
        j=re.search(r'```json\n(.*?)\n```',m[2],re.S)
        if j:chapters[m[1].split()[0]]=json.loads(j[1])
    obs=chapters['R27_01_WIN_RATE_TRUTH']['observers']['rows']
    draws=next(e['rows'] for e in s['recentEvidence'] if e['table']=='draws')
    invalid=sum(sorted(int(x['n'+str(i)]) for i in range(1,11))!=list(range(1,11)) for x in draws)
    counts=collections.Counter(x['issue'] for x in draws)
    out={'source_commit':r['sourceCommit'],'snapshot_hash':r['snapshotHash'],
         'observer_exported_rows':len(obs),'observer_totals':{k:sum(x.get(k,0) for x in obs) for k in ['periods','hits','acts','actHits']},
         'observer_scope':'exported strategy rows; correlated same-issue predictions; NOT independent samples or REAL bets',
         'section_status_counts':dict(collections.Counter(x.get('status','NO_STATUS') for x in r['sections'])),
         'draws':{'rows':len(draws),'unique_issues':len(counts),'duplicate_issue_rows':sum(v-1 for v in counts.values()),'invalid_permutations':invalid,'dirty_rows':sum(int(x.get('dirty',0))!=0 for x in draws)},
         'tables':{t['table']:{'rows':t['rows'],'states':t.get('states',[])} for t in s['tables'] if t['table'].startswith('r27_')},
         'matrix_snapshots':len(json.loads(read('STRATEGY_MATRICES.json'))['snapshots']),
         'counters':chapters['R27_02_EVOLUTION_TRUTH']['sevenCounters'],
         'runtime':chapters['R27_03_CONTINUOUS_RUNTIME'],
         'raw_database_included':False,
         'validation_scope':'report arithmetic and metadata validation only; no APK execution or model retraining'}
    return out

if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('report',type=Path);a.add_argument('--out',type=Path,required=True);args=a.parse_args()
    out=run(args.report);args.out.parent.mkdir(parents=True,exist_ok=True);args.out.write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'output':str(args.out),'draws':out['draws'],'observer_totals':out['observer_totals'],'scope':out['validation_scope']},ensure_ascii=False))
