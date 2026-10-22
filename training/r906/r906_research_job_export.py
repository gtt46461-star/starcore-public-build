#!/usr/bin/env python3
"""Export immutable R906 artifact jobs and PIT labels from an app SQLite database."""
import argparse,csv,hashlib,json,sqlite3,sys
from pathlib import Path

def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    ap=argparse.ArgumentParser();ap.add_argument("--db",required=True);ap.add_argument("--out",required=True);ap.add_argument("--job-id")
    a=ap.parse_args(); out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
    con=sqlite3.connect(a.db);con.row_factory=sqlite3.Row
    where=" WHERE job_id=?" if a.job_id else ""; args=(a.job_id,) if a.job_id else ()
    jobs=[dict(r) for r in con.execute("SELECT * FROM v57_research_artifact_job_r906"+where+" ORDER BY created_at",args)]
    if not jobs:raise SystemExit("NO_R906_ARTIFACT_JOB")
    for j in jobs:
        jd=out/j["job_id"];jd.mkdir(exist_ok=True)
        labels=[dict(r) for r in con.execute("SELECT * FROM v57_research_compact_label_r906 WHERE target_issue<=? ORDER BY target_issue,asset_id",(j["data_until_issue"],))]
        job_bytes=json.dumps(j,ensure_ascii=False,sort_keys=True,separators=(",",":")).encode();(jd/"job.json").write_bytes(job_bytes)
        with (jd/"pit_labels.jsonl").open("w",encoding="utf-8") as f:
            for r in labels:f.write(json.dumps(r,ensure_ascii=False,sort_keys=True,separators=(",",":"),default=lambda x:x.hex() if isinstance(x,bytes) else x)+"\n")
        manifest={"job_id":j["job_id"],"asset_id":j["asset_id"],"dataset_rows":len(labels),"job_sha256":sha(job_bytes),"labels_sha256":sha((jd/"pit_labels.jsonl").read_bytes()),"data_until_issue":j["data_until_issue"],"future_fields_forbidden":["future_draw","target_actual","post_draw_recompute"]}
        (jd/"export_manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,sort_keys=True,indent=2),encoding="utf-8")
    print(json.dumps({"status":"PASS","jobs":len(jobs),"out":str(out)},ensure_ascii=False))
if __name__=="__main__":main()

