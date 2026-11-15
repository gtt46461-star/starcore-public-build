#!/usr/bin/env python3
"""R30-B00-05 复核原需求/原文件卡保全计数（数据来自 R29 方案包，R30 不改其内容）。"""
import json, sys
base = sys.argv[1] if len(sys.argv) > 1 else "construction/plans/STARCORE_R29_FINAL_20261029/registries"
req = json.load(open(f"{base}/legacy_requirements_preserved.json")); cards = json.load(open(f"{base}/legacy_file_cards_preserved.json"))
def n(x):
    if isinstance(x, list): return len(x)
    for k in ("requirements", "cards", "items", "rows"):
        if isinstance(x, dict) and k in x: return len(x[k])
    return -1
print(json.dumps({"requirements": n(req), "fileCards": n(cards)}))
