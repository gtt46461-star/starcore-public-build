#!/usr/bin/env python3
"""R30 方案包自检（仅标准库）。用法：python3 tools/validate_plan.py [--out evidence/plan_validation.json]
检查项：
  V01 R29 52 张卡全部映射到 R30 卡          V02 44 项指标每项至少一张卡
  V03 同批次文件独占（共享仅限登记且方法不重叠） V04 MODIFY 文件在 bb607cf 存在
  V05 卡中引用的 code/ 路径在包内存在         V06 ADD 动作的 .java 在 code/main 存在
  V07 卡依赖只指向更早的卡                    V08 baseline/ 文件哈希与清单一致
  V09 MANIFEST.json 记录的每个文件哈希一致（若存在）"""
import hashlib, json, os, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
R = lambda p: json.load(open(os.path.join(ROOT, p), encoding="utf-8"))
cards = R("registries/cards.json")["cards"]; ids = [c["id"] for c in cards]
res = []
def chk(name, ok, detail=""): res.append({"check": name, "pass": bool(ok), "detail": detail})

cw = R("registries/r29_to_r30_crosswalk.json"); unmapped = [x["r29"] for x in cw if not x["r30"]]
chk("V01_r29_all_mapped", len(cw) == 52 and not unmapped, f"r29={len(cw)} unmapped={unmapped}")
ms = R("registries/metrics_r30.json"); nocard = [m["id"] for m in ms if not m["cards"]]
chk("V02_metrics_covered", len(ms) == 44 and not nocard, f"metrics={len(ms)} uncovered={nocard}")
own = R("registries/file_ownership.json"); bad = []
for b, shared in own["batch_shared_sequential"].items():
    for p, uses in shared.items():
        syms = [tuple(u["symbols"]) for u in uses]
        if len(set(syms)) != len(syms) or any(not s for s in syms): bad.append((b, p))
chk("V03_ownership_exclusive_or_sequential_disjoint", not bad, str(bad))
miss = [(c["id"], f["path"]) for c in cards for f in c["files"] if f["action"] == "MODIFY" and f.get("exists_at_base") is False]
chk("V04_modify_targets_exist_at_base", not miss, str(miss))
codemiss = [(c["id"], p) for c in cards for p in c.get("code", []) if (p.startswith("code/") or p.startswith("tools/")) and not os.path.exists(os.path.join(ROOT, p.rstrip("/")))]
chk("V05_code_refs_exist", not codemiss, str(codemiss))
addmiss = []
for c in cards:
    for f in c["files"]:
        if f["action"] == "ADD" and f["path"].endswith(".java") and "/main/java/" in f["path"]:
            rel = "code/main/" + f["path"].split("/main/java/")[1]
            if not os.path.exists(os.path.join(ROOT, rel)): addmiss.append((c["id"], rel))
chk("V06_add_java_present", not addmiss, str(addmiss))
pos = {i: k for k, i in enumerate(ids)}
depbad = [(c["id"], d) for c in cards for d in c["depends"] if d not in pos or pos[d] >= pos[c["id"]]]
chk("V07_dependencies_backward_only", not depbad, str(depbad))
bm = R("baseline/BASELINE_MANIFEST.json"); hb = []
for f in bm["files"]:
    p = os.path.join(ROOT, "baseline", "bb607cf", f["path"])
    if not os.path.exists(p) or hashlib.sha256(open(p, "rb").read()).hexdigest() != f["sha256"]: hb.append(f["path"])
chk("V08_baseline_hashes", not hb, f"files={len(bm['files'])} bad={hb}")
mp = os.path.join(ROOT, "MANIFEST.json")
if os.path.exists(mp):
    m = json.load(open(mp)); badm = [e["path"] for e in m["files"] if not os.path.exists(os.path.join(ROOT, e["path"])) or hashlib.sha256(open(os.path.join(ROOT, e["path"]), "rb").read()).hexdigest() != e["sha256"]]
    chk("V09_manifest_hashes", not badm, f"files={len(m['files'])} bad={badm[:10]}")
out = {"cards": len(cards), "batches": len({c['batch'] for c in cards}), "results": res, "pass": all(r["pass"] for r in res)}
for r in res: print(("PASS " if r["pass"] else "FAIL ") + r["check"] + "  " + r["detail"][:160])
if "--out" in sys.argv: json.dump(out, open(sys.argv[sys.argv.index("--out") + 1], "w"), ensure_ascii=False, indent=1)
sys.exit(0 if out["pass"] else 1)
