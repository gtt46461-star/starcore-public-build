"""Read-only audit. Run against extracted R23 plan and latest FULL_REPORT directory.
No network, no database mutation, no interpretation of missing evidence as PASS.
"""
from __future__ import annotations
import argparse,csv,datetime,hashlib,json,math,statistics
from collections import Counter
from pathlib import Path

def dump(path, data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
def ordinal(issue:str)->int:
    if len(issue)!=12 or not issue.isdigit():raise ValueError('ISSUE_FORMAT')
    d=datetime.date(int(issue[:4]),int(issue[4:6]),int(issue[6:8]));n=int(issue[8:])
    if not 1<=n<=1440:raise ValueError('ISSUE_SEQUENCE')
    return d.toordinal()*1440+n

def wilson(k:int,n:int,z:float=1.959963984540054):
    if n==0:return [None,None]
    p=k/n; den=1+z*z/n;c=(p+z*z/(2*n))/den;h=z*math.sqrt(p*(1-p)/n+z*z/(4*n*n))/den
    return [c-h,c+h]

def metrics(rows):
    a=[r for r in rows if r['research_act']];n=len(a);k=sum(int(r['settled_hit']) for r in a)
    run=longest=0
    for r in a:
        run=0 if int(r['settled_hit']) else run+1;longest=max(longest,run)
    out={'opportunities':len(rows),'actions':n,'hits':k,'coverage':n/len(rows) if rows else None,
         'hit_rate':k/n if n else None,'wilson95_descriptive':wilson(k,n),'max_losing_streak':longest,
         'qualification':'NOT_PROSPECTIVE_RELEASE_EVIDENCE'}
    if n:
        out['brier']=sum((r['raw_probability']-int(r['settled_hit']))**2 for r in a)/n
        out['matched_constant_0p30_brier']=sum((.3-int(r['settled_hit']))**2 for r in a)/n
        out['mean_probability']=sum(r['raw_probability'] for r in a)/n
        out['calibration_gap']=out['mean_probability']-k/n
        # Fixed bins. ECE is descriptive, not a certificate or an unbiased precision guarantee.
        bins=[]
        for i in range(10):
            b=[r for r in a if min(9,int(r['raw_probability']*10))==i]
            if b:bins.append({'bin':i,'n':len(b),'p':sum(x['raw_probability'] for x in b)/len(b),'hit':sum(int(x['settled_hit']) for x in b)/len(b)})
        out['ece_fixed10']=sum(b['n']*abs(b['p']-b['hit']) for b in bins)/n;out['bins']=bins
    return out

def audit(plan:Path,report:Path,out:Path):
    out.mkdir(parents=True,exist_ok=True)
    j=json.loads((report/'DIAGNOSTIC_SCOPE.json').read_text());tab={x['table']:x for x in j['tables']}
    evid={x['table']:x for x in j['recentEvidence']};new=sorted(evid['draws']['rows'],key=lambda x:ordinal(x['issue']))
    old=list(csv.DictReader((plan/'data/report_4743_draws.csv').open(encoding='utf-8-sig')))
    ids=[r['issue'] for r in new];assert len(ids)==len(set(ids))
    for r in new:assert sorted(int(r[f'n{i}']) for i in range(1,11))==list(range(1,11))
    oldmap={r['issue']:r for r in old};newmap={r['issue']:r for r in new}
    changed=[i for i in oldmap.keys()&newmap.keys() if any(oldmap[i][f'n{k}']!=newmap[i][f'n{k}'] for k in range(1,11))]
    gaps=[{'previous':a,'next':b,'missing':ordinal(b)-ordinal(a)-1} for a,b in zip(ids,ids[1:]) if ordinal(b)-ordinal(a)!=1]
    tail=[r for r in new if r['issue'] not in oldmap]
    valid=[];invalid=[]
    for r in tail:
        t=ids.index(r['issue']);window=new[max(0,t-32):t+1]
        ok=len(window)==33 and all(ordinal(y['issue'])-ordinal(x['issue'])==1 for x,y in zip(window,window[1:]))
        (valid if ok else invalid).append(r['issue'])
    with (out/'draws_4836_sorted.csv').open('w',newline='',encoding='utf-8') as f:
        wr=csv.DictWriter(f,fieldnames=['issue']+[f'n{i}' for i in range(1,11)]);wr.writeheader();wr.writerows({k:r[k] for k in wr.fieldnames} for r in new)
    data={'old_rows':len(old),'new_rows':len(new),'overlap':len(oldmap.keys()&newmap.keys()),'old_missing':sorted(oldmap.keys()-newmap.keys()),'changed_draws':changed,'new_rows_added':len(tail),'first':ids[0],'last':ids[-1],'gaps':gaps,'extension_eligible_32_contiguous':valid,'extension_excluded_32_gap':invalid,'semantics':'one period is one statistical opportunity, ten positions are not ten independent issues'}
    dump(out/'data_integrity.json',data)
    inv=[{k:x.get(k) for k in ['table','rows','states']} for x in j['tables']];dump(out/'table_census.json',inv)
    comp=[{'table':x['table'],'total':x.get('totalRows'),'exported':x.get('exportedRows'),'complete':x.get('complete')} for x in j['recentEvidence']];dump(out/'report_evidence_completeness.json',comp)
    for label,pat in [('requirements_1832','construction-current-952d*'),('cards_299','construction-current-09937*'),('prior_subitems','construction-current-04aa*')]:
        f=next(report.glob(pat));rs=list(csv.DictReader(f.open(encoding='utf-8-sig')));dump(out/(label+'.json'),rs)
    identity=json.loads(next(report.glob('construction-current-a744*')).read_text());dump(out/'historical_requirement_status.json',identity)
    allrows=[json.loads(l) for l in (plan/'results/replay/predraw_settlement.jsonl').read_text().splitlines()];policies=sorted(set(x['policy'] for x in allrows))
    stats={k:metrics([r for r in allrows if r['policy']==k]) for k in policies};dump(out/'r23_recomputed_metrics.json',stats)
    tables_by_group={p:{'tables':len([t for t in tab if t.startswith(p)]),'rows':sum(int(x['rows']) for t,x in tab.items() if t.startswith(p))} for p in ['su1_','su2_','r952_']}
    dump(out/'audit_summary.json',{'data':data,'report_all_checks':j['allChecks'],'tables':len(tab),'table_rows':sum(int(x['rows']) for x in tab.values()),'recent_evidence_tables':len(comp),'incomplete_evidence':[x for x in comp if not x['complete']],'groups':tables_by_group,'upgrade_diagnosis':json.loads((report/'UPGRADE_DIAGNOSIS.json').read_text()),'strategy_matrices':json.loads((report/'STRATEGY_MATRICES.json').read_text()),'legacy_requirement_status':identity,'r23_recomputed':stats})
    for name in ['source-manifest.json','runtime-observation.json','runtime-observation-heartbeatSummary.txt','runtime-observation-crashSummary.txt','STORAGE_CENSUS.txt','UPGRADE_DIAGNOSIS.json','STRATEGY_MATRICES.json']:
        (out/name).write_bytes((report/name).read_bytes())
    print(json.dumps({'old':len(old),'new':len(new),'gaps':gaps,'eligible_extension':len(valid),'excluded_extension':len(invalid),'table_groups':tables_by_group,'primary':stats['equal_marginal_mixture']},ensure_ascii=False,indent=2))
if __name__=='__main__':
    a=argparse.ArgumentParser();a.add_argument('--plan',type=Path,required=True);a.add_argument('--report',type=Path,required=True);a.add_argument('--out',type=Path,required=True);x=a.parse_args();audit(x.plan,x.report,x.out)
