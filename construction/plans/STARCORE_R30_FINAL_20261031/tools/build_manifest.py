#!/usr/bin/env python3
"""生成 MANIFEST.json：包内每个文件的 sha256 与字节数（不含 MANIFEST.json 自身）。"""
import hashlib, json, os
P = os.path.dirname(os.path.dirname(os.path.abspath(__file__))); files = []
for d, _, fs in os.walk(P):
    for f in sorted(fs):
        p = os.path.join(d, f); rel = os.path.relpath(p, P)
        if rel == "MANIFEST.json" or "__pycache__" in rel: continue
        b = open(p, "rb").read(); files.append({"path": rel, "sha256": hashlib.sha256(b).hexdigest(), "bytes": len(b)})
files.sort(key=lambda x: x["path"])
json.dump({"plan": "STARCORE_R30_FINAL_20261031", "base": "bb607cf5ab69f7286f859e43f87ada85a5bc456d", "files": files,
           "totalBytes": sum(f["bytes"] for f in files), "fileCount": len(files)}, open(os.path.join(P, "MANIFEST.json"), "w"), ensure_ascii=False, indent=1)
print(len(files), sum(f["bytes"] for f in files))
