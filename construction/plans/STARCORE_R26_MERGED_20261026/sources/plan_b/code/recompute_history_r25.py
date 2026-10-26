"""Recompute EXISTING historical decisions against supplied draws. No new OOS claim."""
from pathlib import Path
import csv,json,math
from collections import defaultdict
P=Path(__file__).resolve().parents[1]
rows=list(csv.DictReader((P/'baseline_R24/evidence/draws_4836_sorted.csv').open()))
y={r['issue']:[int(r['n'+str(i)]) for i in range(1,11)] for r in rows}
if len(y)!=len(rows) or any(sorted(v)!=list(range(1,11)) for v in y.values()):raise ValueError('INVALID_DRAW_SOURCE')
policies=defaultdict(list)
for line in (P/'baseline_R23/predraw_settlement.jsonl').open():
 r=json.loads(line);policies[r['policy']].append(r)
out={}
for name,rs in policies.items():
 acts=hits=mx=streak=0;brier=0.;mismatch=[]
 for r in rs:
  h=int(y[r['issue']][r['position']-1] in r['numbers'])
  if h!=r['settled_hit']:mismatch.append(r['issue'])
  if r['research_act']:
   acts+=1;hits+=h;streak=0 if h else streak+1;mx=max(mx,streak);brier+=(r['raw_probability']-h)**2
 out[name]=dict(opportunities=len(rs),actions=acts,hits=hits,rate=hits/acts if acts else None,coverage=acts/len(rs),max_losing_streak=mx,brier=brier/acts if acts else None,settlement_mismatches=mismatch,scope='EXISTING_EXPOSED_HISTORY_NOT_NEW_OOS')
print(json.dumps(out,ensure_ascii=False,indent=2))
