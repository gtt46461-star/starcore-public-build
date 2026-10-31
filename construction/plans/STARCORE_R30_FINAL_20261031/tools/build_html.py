#!/usr/bin/env python3
"""把 README、START_HERE 与 docs/*.md 按顺序合成一个可离线阅读的 HTML（需要 pip 包 markdown）。"""
import html, os, sys, markdown
P = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
order = ["README.md", "START_HERE_施工方必读.md"] + ["docs/" + f for f in sorted(os.listdir(os.path.join(P, "docs")), key=lambda x: (x[0] == "9", x))]
parts, toc = [], []
for i, f in enumerate(order):
    t = open(os.path.join(P, f), encoding="utf-8").read().replace("<!-- CARDS -->", "")
    title = next((l[2:].strip() for l in t.splitlines() if l.startswith("# ")), f)
    toc.append(f'<li><a href="#s{i}">{html.escape(title)}</a></li>')
    body = markdown.markdown(t, extensions=["tables", "fenced_code", "sane_lists"])
    parts.append(f'<section id="s{i}"><div class="src">{html.escape(f)}</div>{body}</section>')
css = """:root{--bg:#fbfaf7;--fg:#1f2328;--mut:#646a73;--line:#e3e0d8;--code:#f2efe8;--acc:#9a3412}
@media (prefers-color-scheme:dark){:root{--bg:#16181c;--fg:#e6e6e6;--mut:#9aa0a6;--line:#30343b;--code:#1f2228;--acc:#fb923c}}
body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.65 -apple-system,"PingFang SC","Noto Sans CJK SC",sans-serif}
main{max-width:1040px;margin:0 auto;padding:16px}nav{border:1px solid var(--line);padding:8px 16px;margin:12px 0 24px}
nav ol{margin:0;padding-left:20px}a{color:var(--acc)}h1{font-size:1.6em;border-bottom:2px solid var(--line);padding-bottom:6px}
h3{margin-top:2em;padding-top:8px;border-top:1px dashed var(--line)}section{margin-bottom:48px}
.src{color:var(--mut);font-size:12px}table{border-collapse:collapse;display:block;overflow-x:auto;margin:8px 0}
th,td{border:1px solid var(--line);padding:4px 8px;vertical-align:top}th{background:var(--code)}
code{background:var(--code);padding:1px 4px;border-radius:3px;font-size:.92em;word-break:break-all}pre{background:var(--code);padding:10px;overflow-x:auto}pre code{padding:0;word-break:normal}"""
doc = f"""<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>STARCORE R30 施工方案</title><style>{css}</style></head><body><main><h1>STARCORE R30 最终施工方案（完整可读版）</h1>
<nav><strong>目录</strong><ol>{''.join(toc)}</ol></nav>{''.join(parts)}</main></body></html>"""
open(os.path.join(P, "STARCORE_R30_完整施工方案.html"), "w", encoding="utf-8").write(doc)
print(len(doc))
