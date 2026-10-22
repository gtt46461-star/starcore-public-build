#!/usr/bin/env python3
"""Strict time-ordered no-leakage sanity replay for the number-first design.
This is an offline challenger diagnostic, not a claim that the Android implementation
will reproduce the same results or achieve the user's acceptance target.
"""
from pathlib import Path
import csv,json,math,itertools
root=Path(__file__).resolve().parents[1]
path=root/'research_data/ethpk10_draws_20260316_201059.csv'
rows=[]
with path.open(encoding='utf-8') as f:
    for r in csv.DictReader(f):
        if r.get('valid')!='1': continue
        nums=[int(x) for x in r['open_number'].split(',')]
        if len(nums)==10 and sorted(nums)==list(range(1,11)): rows.append((r['period'],nums))

def dist(history,pos):
    # weighted windows; only rows strictly before target are used
    weights=((12,0.34),(24,0.25),(48,0.18),(96,0.13),(240,0.10))
    score=[0.8]*10
    for w,alpha in weights:
        tail=history[-min(w,len(history)):]
        if not tail: continue
        cnt=[0]*10
        for _,a in tail: cnt[a[pos]-1]+=1
        den=len(tail)+10.0
        for n in range(10): score[n]+=alpha*((cnt[n]+1.0)/den)*10.0
    s=sum(score)
    return [x/s for x in score]

pred=[]
for i in range(30,len(rows)):
    hist=rows[:i]
    issue,actual=rows[i]
    candidates=[]
    for pos in range(10):
        p=dist(hist,pos)
        ordered=sorted(range(10),key=lambda n:(-p[n],n))
        top=ordered[:3]
        event=sum(p[n] for n in top)
        margin=p[ordered[2]]-p[ordered[3]]
        confidence=max(0.0,min(1.0,0.5+8.0*margin))
        candidates.append((event,confidence,pos,top))
    candidates.sort(key=lambda x:(-x[0],-x[1],x[2],x[3]))
    event,conf,pos,top=candidates[0]
    hit=1 if actual[pos]-1 in top else 0
    pred.append({'issue':issue,'event_p':event,'confidence':conf,'hit':hit,'position':pos+1})

pred.sort(key=lambda x:x['issue'])
rate=sum(x['hit'] for x in pred)/len(pred)
brier=sum((x['event_p']-x['hit'])**2 for x in pred)/len(pred)
# confidence-selected coverage frontier, preserving OOS outcomes
ranked=sorted(pred,key=lambda x:(-x['confidence'],-x['event_p'],x['issue']))
frontier=[]
for cov in (5,10,15,20,25):
    n=max(1,int(len(ranked)*cov/100.0)); ss=ranked[:n]; h=sum(x['hit'] for x in ss)
    pnl=h*6.9+(n-h)*-3.0
    frontier.append({'coverage_pct':cov,'n':n,'hits':h,'rate':h/n,'pnl':pnl})
# four chronological blocks
blocks=[]
for b in range(4):
    lo=len(pred)*b//4; hi=len(pred)*(b+1)//4; ss=pred[lo:hi]; h=sum(x['hit'] for x in ss)
    blocks.append({'block':b+1,'n':len(ss),'rate':h/len(ss) if ss else 0.0})
out={'status':'DIAGNOSTIC_NOT_ACCEPTANCE','issues':len(pred),'start_history':30,
     'top1_rate':rate,'brier':brier,'frontier':frontier,'time_blocks':blocks,
     'acceptance_note':'Only strict Android-runtime OOS and real settlement can prove 37% at 15-20% coverage.'}
(root/'reports').mkdir(exist_ok=True)
(root/'reports/V46_OOS_SANITY_REPLAY_46072130.json').write_text(json.dumps(out,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(out,ensure_ascii=False,indent=2))
