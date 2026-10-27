"""Read-only recomputation from an extracted diagnostic report and the retained CSV.

Usage: python3 recheck_report_r25.py REPORT_DIRECTORY BASELINE_R24_DIRECTORY
This checks recorded evidence; it does not qualify any strategy.
"""
import csv, datetime, json, sys
from pathlib import Path

def ordinal(issue):
    value=str(issue)
    if len(value)!=12 or not value.isdigit():raise ValueError('ISSUE_FORMAT')
    day=datetime.date(int(value[:4]),int(value[4:6]),int(value[6:8]))
    seq=int(value[8:])
    if not 1<=seq<=1440:raise ValueError('ISSUE_SEQUENCE')
    return day.toordinal()*1440+seq

def main(report,baseline):
    j=json.loads((report/'DIAGNOSTIC_SCOPE.json').read_text(encoding='utf-8'))
    rows=sorted(next(x['rows'] for x in j['recentEvidence'] if x['table']=='draws'),key=lambda r:ordinal(r['issue']))
    ids=[str(r['issue']) for r in rows]
    clean=lambda r:(str(r['issue']),tuple(int(r['n'+str(i)]) for i in range(1,11)))
    normalized=[clean(r) for r in rows]
    candidates=list(baseline.rglob('draws_4836_sorted.csv'))
    if len(candidates)!=1:raise ValueError('BASELINE_CSV_AMBIGUOUS')
    with candidates[0].open(encoding='utf-8-sig',newline='') as f:
        prior=[clean(r) for r in csv.DictReader(f)]
    result={
        'rows':len(rows),'unique_issues':len(set(ids)),
        'invalid_permutations':sum(sorted(r[1])!=list(range(1,11)) for r in normalized),
        'gaps':[{'previous':a,'next':b,'missing':ordinal(b)-ordinal(a)-1} for a,b in zip(ids,ids[1:]) if ordinal(b)-ordinal(a)!=1],
        'tables':len(j['tables']),
        'su2_tables':sum(x['table'].startswith('su2_') for x in j['tables']),
        'all_checks_declared':j['allChecks'],
        'matches_R24_csv':normalized==prior,
        'scope':'RECORDED_REPORT_COUNTS_ONLY'
    }
    print(json.dumps(result,ensure_ascii=False,indent=2))
    if result['unique_issues']!=result['rows'] or result['invalid_permutations'] or not result['matches_R24_csv']:
        raise SystemExit(1)

if __name__=='__main__':main(Path(sys.argv[1]),Path(sys.argv[2]))
