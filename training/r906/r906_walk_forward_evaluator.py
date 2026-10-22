#!/usr/bin/env python3
"""Strict chronological evaluation with fixed folds; never trains on the evaluation issue."""
import argparse,json,math
from pathlib import Path

def wilson(h,n):
 if n<=0:return [0,1]
 z=1.959963984540054;p=h/n;z2=z*z;d=1+z2/n;c=(p+z2/(2*n))/d;half=z*math.sqrt((p*(1-p)+z2/(4*n))/n)/d;return [max(0,c-half),min(1,c+half)]
def main():
 ap=argparse.ArgumentParser();ap.add_argument("--predictions",required=True);ap.add_argument("--out",required=True);a=ap.parse_args();rows=[]
 with open(a.predictions,encoding="utf-8") as f:
  for line in f:
   if line.strip():rows.append(json.loads(line))
 rows.sort(key=lambda r:str(r["target_issue"])); last="";h=0;b=0.;bins=[[0,0.,0.] for _ in range(10)];blocks=[];cur=[]
 for r in rows:
  issue=str(r["target_issue"])
  if last and issue<=last:raise SystemExit("PIT_ORDER_VIOLATION")
  trained=str(r.get("trained_until_issue",""))
  if trained and trained>=issue:raise SystemExit("FUTURE_LEAKAGE:"+issue)
  p=max(0,min(1,float(r["probability"])));y=int(r["hit_label"]);h+=y;b+=(p-y)**2;bi=min(9,int(p*10));bins[bi][0]+=1;bins[bi][1]+=p;bins[bi][2]+=y;cur.append(y)
  if len(cur)==60:blocks.append(sum(cur)/len(cur));cur=[]
  last=issue
 if cur:blocks.append(sum(cur)/len(cur))
 n=len(rows);ece=sum((c/n)*abs(ps/c-ys/c) for c,ps,ys in bins if c and n) if n else 0
 report={"status":"PASS","n":n,"hit":h,"hit_rate":h/n if n else 0,"wilson95":wilson(h,n),"brier":b/n if n else None,"ece":ece,"block_rates":blocks,"block_stability":1-(max(blocks)-min(blocks)) if blocks else 0,"first_issue":rows[0]["target_issue"] if rows else "","last_issue":last}
 Path(a.out).write_text(json.dumps(report,ensure_ascii=False,sort_keys=True,indent=2));print(json.dumps(report))
if __name__=="__main__":main()

