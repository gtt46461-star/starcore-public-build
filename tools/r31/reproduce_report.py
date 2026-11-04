#!/usr/bin/env python3
"""Recount original device report. This does not execute the Android product."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'construction/STARCORE_R31_FINAL_20261104'


def recount():
    source=PLAN/'evidence/original_report'
    report=json.loads((source/'R30_REPORT.json').read_text())
    diagnostic=json.loads((source/'DIAGNOSTIC_SCOPE.json').read_text())
    tasks=report['chapters'][3]['data']['tasks']
    r27=[r for r in tasks if r['task_id'].startswith('r27-')]
    checks=[]
    for line in (source/'REPORT_DETAILS.txt').read_text().splitlines():
        if line.startswith('{"kind":"check_result"'):
            item=json.loads(line)
            checks.append({k:item[k] for k in ('id','execution_state','domain_outcome')})
    index=report['objectIndex']
    result={
        'scope':'ORIGINAL_DEVICE_REPORT_RECOUNT_NOT_NEW_APK_EXECUTION',
        'source_commit':'5237b265a27fe5ad27fd842901c445d5f8c27749',
        'source_sha256':{f:hashlib.sha256((source/f).read_bytes()).hexdigest()
                         for f in ('R30_REPORT.json','DIAGNOSTIC_SCOPE.json','REPORT_DETAILS.txt')},
        'tables':len(diagnostic['tables']), 'tasks':len(tasks),
        'task_states':dict(Counter(t['status'] for t in tasks)),
        'r27_tasks':len(r27),
        'r27_quarantined':sum(t['status']=='QUARANTINED' for t in r27),
        'r27_claim_failure':sum('R30_PREPARED_TASK_CLAIM' in t['blocked_reason'] for t in r27),
        'r27_cancelled':sum('R27_CANCELLED' in t['blocked_reason'] for t in r27),
        'r27_missing_student':sum('MATRIX_ORIGINAL_STUDENT_UNAVAILABLE' in t['blocked_reason'] for t in r27),
        'metrics':len(report['metricResults']),
        'checks':len(checks),'check_execution_states':dict(Counter(t['execution_state'] for t in checks)),
        'check_domain_outcomes':dict(Counter(t['domain_outcome'] for t in checks)),
        'legacy_charts':len(index['legacyCharts']['objects']), 'research_charts':len(index['researchCharts']['objects']),
        'original_overall_acceptance':report['overallAcceptance'],
        'new_apk_quality':'NOT_PROVEN', 'new_apk_device':'NOT_RUN'
    }
    expected={'tables':713,'tasks':60,'r27_tasks':13,'r27_quarantined':13,
              'r27_claim_failure':11,'r27_cancelled':1,'r27_missing_student':1,
              'metrics':44,'checks':96,'legacy_charts':29,'research_charts':18}
    failures={k:{'expected':v,'observed':result[k]} for k,v in expected.items() if result[k]!=v}
    if result['task_states'].get('QUARANTINED')!=14:
        failures['all_quarantined']=result['task_states']
    if result['check_domain_outcomes'].get('PASS')!=4:
        failures['business_check_pass']=result['check_domain_outcomes']
    result['recount_status']='FAIL' if failures else 'PASS'
    result['failures']=failures
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path)
    args=parser.parse_args()
    result=recount();text=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
    if args.output:
        args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text(text)
    print(text,end='')
    raise SystemExit(0 if result['recount_status']=='PASS' else 1)
