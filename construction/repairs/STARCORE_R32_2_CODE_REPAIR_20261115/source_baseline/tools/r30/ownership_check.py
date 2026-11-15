#!/usr/bin/env python3
"""R30 文件所有权检查：一张卡的提交只能改本卡登记的文件。
用法：python3 tools/r30/ownership_check.py --card R30-B01-04 --base <上一卡提交> [--head HEAD] [--registry construction/plans/STARCORE_R30_FINAL_20261031/registries/file_ownership.json]
规则：
  1. git diff --name-only base..head 中每个应用源码/测试/工具文件都必须在该卡 files 中（目录项以 / 结尾表示整个目录）；
  2. construction/r30/、evidence/r30/ 下的账本与证据文件对所有卡开放；
  3. 越界即退出码1，并列出越界文件。"""
import argparse, json, subprocess, sys
ap = argparse.ArgumentParser(); ap.add_argument("--card", required=True); ap.add_argument("--base", required=True); ap.add_argument("--head", default="HEAD")
ap.add_argument("--registry", default="construction/plans/STARCORE_R30_FINAL_20261031/registries/file_ownership.json")
a = ap.parse_args()
reg = json.load(open(a.registry))
card = reg["cards"].get(a.card)
if card is None: print("UNKNOWN_CARD", a.card); sys.exit(2)
allowed = list(card["files"])
# docs/02 §2：卡需要改未登记文件时，先在 construction/r30/SUBCARDS.json 登记子卡（文件+原因），再施工。
try:
    for sub in json.load(open("construction/r30/SUBCARDS.json"))["subcards"]:
        if sub["card"] == a.card: allowed += sub["files"]
except FileNotFoundError:
    pass
changed = subprocess.run(["git", "-c", "core.quotepath=false", "diff", "--name-only", f"{a.base}..{a.head}"], capture_output=True, text=True, check=True).stdout.split()
OPEN = ("construction/r30/", "evidence/r30/", "construction/plans/STARCORE_R30_FINAL_20261031/", "release/release_manifest.json", "release/release_identity.properties", "app/build/generated/", "app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java", "verification/r36-stubs/",
        # rebind_identity.py 产物（docs/02 §2 对所有卡开放的生成身份文件）
        "release/strategy_su1_reviewed_inputs.json", "release/r82_training_invalidation.json", "release/research_dependencies_r967.json")
viol = [f for f in changed if not f.startswith(OPEN) and not any(f == p or (p.endswith("/") and f.startswith(p)) for p in allowed)]
print(json.dumps({"card": a.card, "changed": len(changed), "violations": viol}, ensure_ascii=False, indent=1))
sys.exit(1 if viol else 0)
