#!/usr/bin/env python3
"""R30-B00-01 基线锁：校验仓库工作树中每个“方案要改的文件”与本包 baseline/ 下保存的 bb607cf 版本字节一致。
用法：python3 tools/r30/baseline_lock.py --plan construction/plans/STARCORE_R30_FINAL_20261031 --out construction/r30/BASELINE.json [--repo .]
退出码：0=全部一致；1=有不一致（说明基线被改过，必须先查清再施工）。"""
import argparse, hashlib, json, os, sys
ap = argparse.ArgumentParser(); ap.add_argument("--plan", required=True); ap.add_argument("--out", required=True); ap.add_argument("--repo", default=".")
a = ap.parse_args()
manifest = json.load(open(os.path.join(a.plan, "baseline", "BASELINE_MANIFEST.json")))
rows, bad = [], 0
for f in manifest["files"]:
    p = os.path.join(a.repo, f["path"])
    cur = hashlib.sha256(open(p, "rb").read()).hexdigest() if os.path.isfile(p) else None
    ok = cur == f["sha256"]; bad += 0 if ok else 1
    rows.append({"path": f["path"], "expected": f["sha256"], "actual": cur, "ok": ok})
out = {"base": manifest["base"], "files": len(rows), "mismatches": bad, "rows": rows}
os.makedirs(os.path.dirname(a.out) or ".", exist_ok=True); json.dump(out, open(a.out, "w"), indent=1)
print(f"files={len(rows)} mismatches={bad}"); sys.exit(0 if bad == 0 else 1)
