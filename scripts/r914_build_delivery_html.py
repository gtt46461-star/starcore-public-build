#!/usr/bin/env python3
"""Build the offline Chinese R914 construction manual from audited sources."""

from __future__ import annotations

import csv
import html
import re
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "reports" / "r914"
OUTPUT = REPORT / "STARCORE_R914完整施工交付报告.html"

SOURCES = [
    REPORT / "R914_MASTER_DELIVERY_REPORT_ZH.md",
    REPORT / "R914_ARCHITECTURE_BLUEPRINT_ZH.md",
    REPORT / "R914_PHYSICAL_DEVICE_ACCEPTANCE_ZH.md",
    REPORT / "R914_SIGNING_AND_INSTALL_ZH.md",
]


CSS = r"""
:root { color-scheme: light; --ink:#152338; --muted:#5f6b7a; --line:#d9e2ec;
  --blue:#1d4ed8; --pale:#eef4ff; --green:#0f766e; --amber:#9a6700; --red:#b42318; }
* { box-sizing: border-box; }
html { scroll-behavior: smooth; }
body { margin:0; background:#f4f7fb; color:var(--ink); font-family:"Microsoft YaHei","PingFang SC","Noto Sans CJK SC","Source Han Sans SC",Arial,sans-serif; line-height:1.68; }
main { max-width:1180px; margin:34px auto; background:white; padding:62px 74px 90px; box-shadow:0 12px 38px rgba(18,45,78,.10); border-radius:18px; }
h1 { font-size:2.25rem; line-height:1.22; margin:1.8rem 0 .9rem; color:#0b2545; border-bottom:3px solid #2e74b5; padding-bottom:.45rem; }
h1:first-of-type { margin-top:0; font-size:2.55rem; }
h2 { font-size:1.58rem; margin:2rem 0 .65rem; color:#174f87; border-left:5px solid #2e74b5; padding-left:.65rem; }
h3 { font-size:1.19rem; margin:1.45rem 0 .45rem; color:#1f4d78; }
p, li { font-size:1rem; }
code { font-family:"Cascadia Mono","Source Code Pro",Consolas,monospace; background:#f3f6fa; border-radius:4px; padding:.08rem .30rem; overflow-wrap:anywhere; }
pre { background:#0d1b2a; color:#e6edf6; padding:18px 20px; border-radius:10px; overflow:auto; line-height:1.48; }
pre code { background:transparent; padding:0; }
table { border-collapse:collapse; width:100%; margin:1rem 0 1.55rem; table-layout:auto; font-size:.92rem; }
th { background:#e8eef5; color:#0b2545; font-weight:700; }
th, td { border:1px solid #cbd7e4; padding:9px 11px; text-align:left; vertical-align:top; overflow-wrap:anywhere; }
tr:nth-child(even) td { background:#fbfcfe; }
blockquote { margin:1.1rem 0; border-left:5px solid #0f766e; background:#effcf8; padding:10px 18px; color:#17473f; }
#TOC { background:#f5f8fc; border:1px solid var(--line); border-radius:12px; padding:20px 28px; margin:1.4rem 0 2.3rem; columns:2; }
#TOC ul { margin:.25rem 0; padding-left:1.2rem; }
#TOC a { color:#1d4ed8; text-decoration:none; }
.cover { margin:-12px 0 48px; padding:48px 42px; color:white; border-radius:16px; background:linear-gradient(135deg,#0b2545,#1d4ed8); }
.cover .kicker { letter-spacing:.18em; text-transform:uppercase; opacity:.78; font-size:.85rem; }
.cover h1 { color:white; border:0; padding:0; margin:.55rem 0 .7rem; font-size:2.55rem; }
.cover .sub { font-size:1.16rem; max-width:52rem; opacity:.92; }
.cover .meta { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; margin-top:25px; }
.cover .meta div { background:rgba(255,255,255,.12); border:1px solid rgba(255,255,255,.18); border-radius:9px; padding:10px 12px; }
.truth { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; margin:18px 0 28px; }
.truth div { padding:13px; border-radius:10px; background:#eff6ff; border:1px solid #cfe1ff; }
.truth b { display:block; color:#174f87; }
.diagram-set { margin:28px 0 38px; }
.diagram { border:1px solid #cbd7e4; border-radius:14px; padding:22px; margin:18px 0; background:linear-gradient(180deg,#fbfdff,#f6f9fd); }
.diagram h3 { margin-top:0; }
.flow { display:flex; align-items:stretch; justify-content:center; gap:9px; flex-wrap:wrap; }
.node { min-width:128px; max-width:190px; flex:1 1 128px; padding:12px 10px; border:2px solid #6c9bd2; border-radius:9px; background:white; text-align:center; font-weight:650; }
.node.green { border-color:#46a89d; background:#f0fdfa; }
.node.amber { border-color:#d6a53b; background:#fff9e8; }
.node.red { border-color:#da8078; background:#fff5f4; }
.arrow { align-self:center; color:#2e74b5; font-size:1.45rem; font-weight:bold; }
.vflow { display:grid; grid-template-columns:minmax(170px,1fr) 44px minmax(170px,1fr); gap:8px; align-items:center; max-width:720px; margin:auto; }
.vflow .arrow { text-align:center; }
.lane { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
.note { border-left:5px solid #d6a53b; padding:12px 16px; background:#fff9e8; border-radius:6px; margin:1rem 0; }
.pass { color:var(--green); font-weight:700; } .retest { color:var(--amber); font-weight:700; }
details { margin:1rem 0; border:1px solid #d9e2ec; border-radius:8px; padding:8px 12px; background:#fbfcfe; }
summary { cursor:pointer; color:#1f4d78; font-weight:650; }
footer { margin-top:55px; padding-top:18px; border-top:1px solid #d9e2ec; color:var(--muted); font-size:.88rem; }
@media (max-width:800px) { main{margin:0;padding:28px 18px;border-radius:0} #TOC{columns:1}.cover .meta,.truth,.lane{grid-template-columns:1fr}.arrow{transform:rotate(90deg)} }
@media print { body{background:white} main{box-shadow:none;margin:0;max-width:none;padding:0} .cover{break-after:page} h1,h2,h3{break-after:avoid} table,.diagram{break-inside:avoid} details{display:none} }
"""


DIAGRAMS = r"""
<section class="diagram-set" aria-label="R914关键架构图">
<h1>R914 关键架构图（离线可视版）</h1>
<div class="diagram"><h3>唯一实时主链</h3><div class="flow">
  <div class="node">抓取 exact draw</div><div class="arrow">→</div>
  <div class="node">原子写 draw + task</div><div class="arrow">→</div>
  <div class="node green">独立结算 lane</div><div class="arrow">+</div>
  <div class="node green">下一期分析 lane</div><div class="arrow">→</div>
  <div class="node">冻结 PIT + Canonical</div>
</div></div>
<div class="diagram"><h3>结算双分母</h3><div class="lane">
  <div><div class="node">所有合法开奖</div><div class="arrow">↓</div><div class="node green">任务终态 / 总任务</div></div>
  <div><div class="node">有合法赛前 K3</div><div class="arrow">↓</div><div class="node green">已结算 hit/miss / eligible</div><div class="node amber">无赛前预测：明确排除</div></div>
</div></div>
<div class="diagram"><h3>算法家族创造与进化</h3><div class="flow">
  <div class="node">历史原子输入</div><div class="arrow">→</div>
  <div class="node">Score Program</div><div class="arrow">+</div>
  <div class="node">Update Program</div><div class="arrow">→</div>
  <div class="node">局部/全局/跨位记忆</div><div class="arrow">→</div>
  <div class="node green">Future OOS</div><div class="arrow">→</div>
  <div class="node amber">晋级 / 变异 / 淘汰</div>
</div></div>
<div class="diagram"><h3>UI 真值与流畅度</h3><div class="flow">
  <div class="node">一致性真值快照</div><div class="arrow">→</div>
  <div class="node">4 张 Why 卡</div><div class="arrow">+</div>
  <div class="node">16 个状态框</div><div class="arrow">→</div>
  <div class="node green">Row Diff</div><div class="arrow">→</div>
  <div class="node amber">滚动时延迟重绑定</div>
</div></div>
</section>
"""


def mermaid_to_details(markdown: str) -> str:
    pattern = re.compile(r"```mermaid\s*\n(.*?)```", re.S)

    def repl(match: re.Match[str]) -> str:
        source = html.escape(match.group(1).strip())
        return (
            "<details><summary>Mermaid 权威图源（可复制到支持 Mermaid 的查看器）</summary>"
            f"<pre><code>{source}</code></pre></details>"
        )

    return pattern.sub(repl, markdown)


def traceability_markdown() -> str:
    path = REPORT / "R914_REQUIREMENT_TRACEABILITY.csv"
    with path.open(encoding="utf-8-sig", newline="") as handle:
        rows = list(csv.reader(handle))
    lines = ["# 附录：24 条需求—实现—证据追踪表", ""]
    lines.append("|" + "|".join(rows[0]) + "|")
    lines.append("|" + "|".join("---" for _ in rows[0]) + "|")
    for row in rows[1:]:
        lines.append("|" + "|".join(cell.replace("|", "\\|").replace("\n", " ") for cell in row) + "|")
    return "\n".join(lines)


def build() -> None:
    missing = [str(path) for path in SOURCES if not path.is_file()]
    if missing:
        raise SystemExit("Missing report inputs: " + ", ".join(missing))

    cover = r"""
<section class="cover">
  <div class="kicker">STARCORE · TIANSHU · R914</div>
  <h1>V57.20.0-R5.3.0-914 完整施工交付手册</h1>
  <p class="sub">结算连续性、唯一主链、教师/矩阵/反哺真值 UI、深度学习、动态迭代、算法家族创造、逐文件审计、签名覆盖安装与真机验收。</p>
  <div class="meta"><div><b>包名</b><br>com.ethk3.novaonepro</div><div><b>数据库</b><br>DB_VERSION=36</div><div><b>发布边界</b><br>SHADOW ONLY</div></div>
</section>
<div class="truth"><div><b>512 Java</b>完整 Java17 编译</div><div><b>110 / 110</b>期开奖任务主机闭环</div><div><b>100 / 100</b>合法赛前预测主机结算</div><div><b>真机</b>RETEST_REQUIRED</div></div>
<div class="note"><b>真实性边界：</b>R914 不把算法创造能力写成已经证明的 70%–80% 胜率；不把主机测试冒充真机 24 小时证据；REAL 继续硬禁用。</div>
"""
    chunks = [cover]
    for index, path in enumerate(SOURCES):
        text = path.read_text(encoding="utf-8")
        if index == 1:
            chunks.append(DIAGRAMS)
        chunks.append(mermaid_to_details(text))
    chunks.append(traceability_markdown())
    chunks.append(
        "<footer>生成来源：Git 分支 agent/starcore-r914-total-rebuild-20260828；"
        "逐文件权威数据见 delivery/final CSV，逐字符权威差异见 R912_TO_R914_FULL.patch。</footer>"
    )
    combined = "\n\n".join(chunks)

    with tempfile.TemporaryDirectory(prefix="r914-html-") as temp_dir:
        temp = Path(temp_dir)
        source = temp / "combined.md"
        interim = temp / "interim.html"
        source.write_text(combined, encoding="utf-8")
        subprocess.run(
            [
                "pandoc", str(source), "--from=gfm+raw_html", "--to=html5", "--standalone",
                "--toc", "--toc-depth=3", "--metadata", "lang=zh-CN",
                "--metadata", "title=STARCORE R914完整施工交付手册", "--output", str(interim),
            ],
            check=True,
            cwd=ROOT,
        )
        rendered = interim.read_text(encoding="utf-8")
        rendered = rendered.replace("</head>", f"<style>{CSS}</style>\n</head>")
        rendered = rendered.replace("<body>", "<body><main>", 1).replace("</body>", "</main></body>", 1)
        OUTPUT.write_text(rendered, encoding="utf-8")

    if "R914 关键架构图" not in OUTPUT.read_text(encoding="utf-8"):
        raise SystemExit("HTML postcondition failed: architecture diagrams missing")
    print(f"R914_DELIVERY_HTML=PASS|PATH={OUTPUT}|BYTES={OUTPUT.stat().st_size}")


if __name__ == "__main__":
    try:
        build()
    except subprocess.CalledProcessError as exc:
        print(f"R914_DELIVERY_HTML=FAIL|COMMAND={exc.cmd}|CODE={exc.returncode}", file=sys.stderr)
        raise
