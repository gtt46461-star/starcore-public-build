#!/usr/bin/env python3
"""Refresh the offline reader from the authoritative Markdown; requires pandoc."""
from pathlib import Path
import re
import subprocess

P=Path(__file__).resolve().parents[1]
target=P/'START_HERE.html'
html=target.read_text()
docs=sorted((P/'docs').glob('*.md'))
for index,path in enumerate(docs):
    rendered=subprocess.run(['pandoc','--from=gfm-gfm_auto_identifiers','--to=html','--wrap=none',str(path)],capture_output=True,text=True,check=True).stdout
    pattern=rf'(<section id="doc-{index}" class="chapter">.*?<article>).*?(</article></details></section>)'
    if re.search(pattern,html,re.S):
        html,count=re.subn(pattern,lambda m:m[1]+rendered+m[2],html,count=1,flags=re.S)
        assert count==1
    else:
        assert path.name.startswith(('14_','15_','16_','17_'))
        section=f'<section id="doc-{index}" class="chapter"><h2>{path.stem}</h2><details open><summary>展开 / 收起本章</summary><article>{rendered}</article></details></section>'
        needle='<section id="cards"'
        assert html.count(needle)==1
        html=html.replace(needle,section+needle,1)
    nav=f'<a href="#doc-{index}">{path.stem}</a>'
    if nav not in html:
        html=html.replace('<a href="#cards">',nav+'<a href="#cards">',1)
for a,b in [('<title>STARCORE R31 最终返修施工总包</title>','<title>STARCORE R31.3 最终返修施工总包</title>'),
            ('<title>STARCORE R31.1 最终返修施工总包</title>','<title>STARCORE R31.3 最终返修施工总包</title>'),
            ('<h1>最终返修与能力升级施工总包</h1>','<h1>R31.3 最终返修与能力升级施工总包</h1>'),
            ('<h1>R31.1 最终返修与能力升级施工总包</h1>','<h1>R31.3 最终返修与能力升级施工总包</h1>'),
            ('<h2>先读总纲，再按无环阶段施工</h2>','<h2>按批次序号K01→K21逐批施工</h2>'),
            ('<h2>接续已有工程，先完成当前阶段</h2>','<h2>按批次序号K01→K21逐批施工</h2>'),
            ('本包固定到源码提交 <code>5237b265…7749</code>，保留原报告、参考方案和源码锚点。','5237b265是报告追溯基线。已有R31工程保留并接续；读16分册，在施工分支运行tools/r31_gate.py status，按它给出的批次先改代码、批末机器关门。保留全部56卡、原报告、参考方案和源码锚点。'),
            ('5237b265是报告追溯基线。已有R31工程须保留并接续；先读14分册和execution_policy，再按实时账本确定当前阶段。保留全部56卡、原报告、参考方案和源码锚点。','5237b265是报告追溯基线。已有R31工程保留并接续；读16分册，在施工分支运行tools/r31_gate.py status，按它给出的批次先改代码、批末机器关门。保留全部56卡、原报告、参考方案和源码锚点。'),
            ('版本 R31-FINAL-20261104 · 阈值','版本 R31.3-CONSTRUCTION-20261105 · 阈值'),
            ('版本 R31.1-EXECUTION-20261105 · 阈值','版本 R31.3-CONSTRUCTION-20261105 · 阈值'),
            ('<title>STARCORE R31.2 最终返修施工总包</title>','<title>STARCORE R31.3 最终返修施工总包</title>'),
            ('<h1>R31.2 最终返修与能力升级施工总包</h1>','<h1>R31.3 最终返修与能力升级施工总包</h1>'),
            ('<h2>按批次序号S00→S21逐批施工</h2>','<h2>按批次序号K01→K21逐批施工</h2>'),
            ('读15分册，在施工分支运行tools/r312_gate.py status，按它给出的批次逐批施工、机器关门。','读16分册，在施工分支运行tools/r31_gate.py status，按它给出的批次先改代码、批末机器关门。'),
            ('版本 R31.2-SEQUENTIAL-20261105 · 阈值','版本 R31.3-CONSTRUCTION-20261105 · 阈值')]:
    html=html.replace(a,b)
target.write_text(html)
print(f'Refreshed {len(docs)} Markdown chapters; card and chart sections retained.')
