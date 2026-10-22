#!/usr/bin/env python3
"""Deterministic R906 reference trainer/artifact builder.

For light families this emits the exact signature-derived reference runtime used for golden parity.
For heavy families it refuses to label an artifact usable unless a supported trainer is present.
"""
import argparse,json,hashlib,sys
from pathlib import Path
sys.path.insert(0,str(Path(__file__).parent))
from r906_method_runtime import method_signature,family,parameters,SCHEMA,evaluate

def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    ap=argparse.ArgumentParser();ap.add_argument("--job",required=True);ap.add_argument("--out",required=True);a=ap.parse_args()
    job=json.loads(Path(a.job).read_text(encoding="utf-8")); spec=job["canonical_method_spec"]
    fields={}
    for part in spec.split("|")[1:]:
        if "=" in part:k,v=part.split("=",1);fields[k]=v
    plan={"representation":fields["rep"],"feature":fields["feature"],"learner":fields["learner"],"update_rule":fields["update"],"objective":fields["objective"],"calibration":fields["cal"],"risk":fields["risk"],"falsifier":fields["falsifier"]}
    fam=family(plan)
    supported=fam in {"BAYESIAN_TRANSITION","METRIC_RETRIEVAL","COMPACT_FOREST","LINEAR_GRAPH"} and fields.get("exec")=="1"
    if not supported:raise SystemExit("WAITING_OFFLINE_TRAINER_UNSUPPORTED_FAMILY:"+fam)
    mh=method_signature(plan);params=parameters(mh,fam)
    fixtures=[[.30,.10,.02,.85,.62,.55,.48],[.42,1.8,.04,.65,.78,.72,.69],[.19,-1.2,-.03,.93,.35,.28,.31]]
    outputs=[evaluate(plan,x) for x in fixtures]
    artifact={"format":"R906_REFERENCE_ARTIFACT_V1","job_id":job["job_id"],"asset_id":job["asset_id"],"family":fam,"method_signature":mh,"matrix_schema":SCHEMA,"params":params,"trained_until_issue":job["data_until_issue"],"earliest_use_contract":"strictly_after_trained_until","may_authorize_real":False,"golden":{"inputs":fixtures,"outputs":outputs}}
    raw=json.dumps(artifact,sort_keys=True,separators=(",",":")).encode();artifact["artifact_sha256"]=sha(raw)
    Path(a.out).write_text(json.dumps(artifact,ensure_ascii=False,sort_keys=True,indent=2),encoding="utf-8")
    print(json.dumps({"status":"PASS","family":fam,"artifact_sha256":artifact["artifact_sha256"]}))
if __name__=="__main__":main()

