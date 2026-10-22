#!/usr/bin/env python3
import argparse,hashlib,json
from pathlib import Path

def h(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument("--artifact",required=True);ap.add_argument("--out",required=True);a=ap.parse_args();d=json.loads(Path(a.artifact).read_text())
 required=["job_id","asset_id","family","method_signature","matrix_schema","trained_until_issue","may_authorize_real","golden"]
 missing=[k for k in required if k not in d]
 if missing:raise SystemExit("MISSING:"+",".join(missing))
 if d["may_authorize_real"]:raise SystemExit("REAL_AUTHORIZATION_FORBIDDEN")
 m={"format":"R906_ARTIFACT_MANIFEST_V1","artifact_file":Path(a.artifact).name,"artifact_file_sha256":h(a.artifact),"job_id":d["job_id"],"asset_id":d["asset_id"],"family":d["family"],"method_signature":d["method_signature"],"trained_until_issue":d["trained_until_issue"],"may_authorize_real":False,"parity_required":True}
 Path(a.out).write_text(json.dumps(m,ensure_ascii=False,sort_keys=True,indent=2));print(json.dumps({"status":"PASS","manifest_sha256":h(a.out)}))
if __name__=="__main__":main()

