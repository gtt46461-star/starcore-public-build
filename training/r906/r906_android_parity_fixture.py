#!/usr/bin/env python3
import argparse,json,math,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from r906_method_runtime import evaluate

def plan_from_spec(spec):
 m={}
 for q in spec.split("|")[1:]:
  if "=" in q:k,v=q.split("=",1);m[k]=v
 return {"representation":m["rep"],"feature":m["feature"],"learner":m["learner"],"update_rule":m["update"],"objective":m["objective"],"calibration":m["cal"],"risk":m["risk"],"falsifier":m["falsifier"]}
def main():
 ap=argparse.ArgumentParser();ap.add_argument("--artifact",required=True);ap.add_argument("--canonical-spec",required=True);ap.add_argument("--tolerance",type=float,default=1e-12);a=ap.parse_args();art=json.loads(Path(a.artifact).read_text());p=plan_from_spec(a.canonical_spec)
 diffs=[]
 for x,y in zip(art["golden"]["inputs"],art["golden"]["outputs"]):diffs.append(abs(evaluate(p,x)-float(y)))
 mx=max(diffs or [0.0]);status="PASS" if mx<=a.tolerance else "FAIL";print(json.dumps({"status":status,"max_abs_error":mx,"tolerance":a.tolerance}));raise SystemExit(0 if status=="PASS" else 2)
if __name__=="__main__":main()

