#!/usr/bin/env python3
"""Holm step-down correction for preregistered candidate p-values."""
import argparse,json
from pathlib import Path

def main():
 ap=argparse.ArgumentParser();ap.add_argument("--input",required=True);ap.add_argument("--out",required=True);ap.add_argument("--alpha",type=float,default=.05);a=ap.parse_args();rows=json.loads(Path(a.input).read_text());rows=sorted(rows,key=lambda r:float(r["p_value"]));m=len(rows);passed=True;out=[]
 for i,r in enumerate(rows):
  threshold=a.alpha/(m-i) if m>i else a.alpha; reject=passed and float(r["p_value"])<=threshold
  if not reject:passed=False
  z=dict(r);z["holm_threshold"]=threshold;z["reject_null"]=reject;out.append(z)
 Path(a.out).write_text(json.dumps({"alpha":a.alpha,"method":"HOLM_STEP_DOWN","results":out},ensure_ascii=False,sort_keys=True,indent=2));print(json.dumps({"status":"PASS","candidates":m,"rejections":sum(1 for x in out if x["reject_null"])}))
if __name__=="__main__":main()

