#!/usr/bin/env python3
"""R952 PIT dataset builder and five-stage chronological split CLI."""
from __future__ import annotations
import argparse, csv, hashlib, json
from pathlib import Path

STAGES=("TRAIN","VALIDATION","CALIBRATION","POLICY","SEALED")
def digest(value: object)->str:return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(",",":")).encode()).hexdigest()
def load_rows(path: Path)->list[dict]:
    if path.suffix==".jsonl":return [json.loads(x) for x in path.read_text(encoding="utf-8").splitlines() if x.strip()]
    with path.open(encoding="utf-8-sig",newline="") as f:return list(csv.DictReader(f))
def validate(rows:list[dict])->None:
    seen=set()
    for row in rows:
        source=str(row["max_source_issue"]);target=str(row["target_issue"])
        if source>=target:raise ValueError("R952_PIT_SOURCE_NOT_BEFORE_TARGET")
        key=(target,str(row.get("ordinal","")))
        if key in seen:raise ValueError("R952_DUPLICATE_TARGET_ACTION")
        seen.add(key)
def split(rows:list[dict])->dict[str,list[dict]]:
    ordered=sorted(rows,key=lambda r:(str(r["target_issue"]),int(r.get("ordinal",0))))
    issues=sorted({str(r["target_issue"]) for r in ordered});n=len(issues)
    if n<20:raise ValueError("R952_SPLIT_NEEDS_20_ISSUES")
    cuts=(max(1,int(n*.55)),max(2,int(n*.70)),max(3,int(n*.80)),max(4,int(n*.90)),n)
    out={};start=0
    for stage,end in zip(STAGES,cuts):allowed=set(issues[start:end]);out[stage]=[r for r in ordered if str(r["target_issue"]) in allowed];start=end
    if any(not out[s] for s in STAGES):raise ValueError("R952_EMPTY_SPLIT_STAGE")
    return out
def main()->None:
    p=argparse.ArgumentParser();p.add_argument("input",type=Path);p.add_argument("--out",type=Path,required=True);a=p.parse_args()
    rows=load_rows(a.input);validate(rows);parts=split(rows);a.out.mkdir(parents=True,exist_ok=True)
    manifest={"schema":"R952_DATASET_V1","source_hash":hashlib.sha256(a.input.read_bytes()).hexdigest(),"stages":{}}
    for name,data in parts.items():
        payload="\n".join(json.dumps(x,sort_keys=True,separators=(",",":")) for x in data)+"\n";(a.out/(name.lower()+".jsonl")).write_text(payload,encoding="utf-8");manifest["stages"][name]={"rows":len(data),"hash":hashlib.sha256(payload.encode()).hexdigest(),"first":data[0]["target_issue"],"last":data[-1]["target_issue"]}
    manifest["dataset_hash"]=digest(manifest);(a.out/"dataset_manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"status":"PASS","dataset_hash":manifest["dataset_hash"]}))
if __name__=="__main__":main()
