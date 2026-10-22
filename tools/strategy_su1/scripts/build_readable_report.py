#!/usr/bin/env python3
"""Build a self-contained offline report using only Python standard library."""
from pathlib import Path
import html
import re

ROOT = Path(__file__).resolve().parents[1]

def inline(s):
    code=[]
    def keep(m):
        code.append('<code>'+html.escape(m.group(1))+'</code>')
        return '\x01'+str(len(code)-1)+'\x02'
    s=re.sub(r'`([^`]+)`',keep,s)
    s=html.escape(s)
    s=re.sub(r'\*\*(.+?)\*\*',r'<strong>\1</strong>',s)
    s=re.sub(r'\[([^\]]+)\]\((https?://[^\s)]+)\)',r'<a href="\2">\1</a>',s)
    for i,v in enumerate(code): s=s.replace('\x01'+str(i)+'\x02',v)
    return s

def render(text):
    lines=text.splitlines(); out=[]; i=0
    def boundary(s):
        return not s.strip() or bool(re.match(r'^(#{1,6} |```|\||[-*] |\d+\. |> |\\\[)',s))
    while i<len(lines):
        s=lines[i]
        if not s.strip(): i+=1; continue
        if s.startswith('```'):
            lang=s[3:].strip(); code=[]; i+=1
            while i<len(lines) and not lines[i].startswith('```'):
                code.append(lines[i]); i+=1
            out.append('<pre data-language="'+html.escape(lang)+'"><code>'+html.escape('\n'.join(code))+'</code></pre>'); i+=1
        elif s.strip()=='\\[':
            code=[]; i+=1
            while i<len(lines) and lines[i].strip()!='\\]': code.append(lines[i]); i+=1
            out.append('<pre class="math">'+html.escape('\n'.join(code))+'</pre>'); i+=1
        elif re.match(r'^#{1,6} ',s):
            m=re.match(r'^(#{1,6}) (.*)',s); n=min(len(m[1])+1,6)
            out.append(f'<h{n}>'+inline(m[2])+f'</h{n}>'); i+=1
        elif s.startswith('|'):
            rows=[]
            while i<len(lines) and lines[i].startswith('|'):
                row=lines[i].strip().strip('|').split('|')
                if not all(re.fullmatch(r'\s*:?-+:?\s*',x) for x in row): rows.append(row)
                i+=1
            out.append('<div class="table"><table>')
            for n,row in enumerate(rows):
                tag='th' if n==0 else 'td'; out.append('<tr>'+''.join(f'<{tag}>'+inline(x.strip())+f'</{tag}>' for x in row)+'</tr>')
            out.append('</table></div>')
        elif re.match(r'^([-*] |\d+\. )',s):
            ordered=bool(re.match(r'^\d+\. ',s)); tag='ol' if ordered else 'ul'; out.append('<'+tag+'>')
            pat=r'^\d+\. ' if ordered else r'^[-*] '
            while i<len(lines) and re.match(pat,lines[i]):
                out.append('<li>'+inline(re.sub(pat,'',lines[i]))+'</li>'); i+=1
            out.append('</'+tag+'>')
        elif s.startswith('> '):
            parts=[]
            while i<len(lines) and lines[i].startswith('> '): parts.append(lines[i][2:]); i+=1
            out.append('<blockquote>'+inline(' '.join(parts))+'</blockquote>')
        else:
            parts=[s]; i+=1
            while i<len(lines) and not boundary(lines[i]): parts.append(lines[i]); i+=1
            out.append('<p>'+inline(' '.join(parts))+'</p>')
    return '\n'.join(out)

def main():
    files=sorted((ROOT/'docs').glob('*.md'))
    titles=[p.read_text().splitlines()[0].lstrip('# ').strip() for p in files]
    toc=''.join(f'<a href="#chapter-{i}">{html.escape(t)}</a>' for i,t in enumerate(titles))
    sections=''.join(f'<article id="chapter-{i}">'+render(p.read_text())+'</article>' for i,p in enumerate(files))
    import csv, json
    nbatch=len(json.loads((ROOT/'manifests/batches.json').read_text())['batches'])
    with (ROOT/'manifests/requirements.csv').open(encoding='utf-8-sig',newline='') as f: nreq=sum(1 for _ in csv.DictReader(f))
    css='''html{scroll-behavior:smooth}body{margin:0;background:#f5f6f8;color:#182536;font:17px/1.8 system-ui,-apple-system,"Noto Sans CJK SC",sans-serif}header{background:#102c43;color:white;padding:40px max(20px,calc((100vw - 1080px)/2))}header p{color:#d2e3ef}main{max-width:1080px;margin:auto;padding:20px}nav,article{background:white;padding:28px;border:1px solid #dce3e9;border-radius:12px;margin-bottom:24px}nav a{display:block;padding:7px 0;color:#0c557d}h1{font-size:30px;line-height:1.4}h2{font-size:27px;border-bottom:2px solid #d8e5ee;padding-bottom:12px}h3{font-size:22px;margin-top:30px}h4{font-size:19px}p{overflow-wrap:anywhere}pre{background:#132638;color:#eef5fb;padding:18px;overflow:auto;font:14px/1.6 ui-monospace,monospace;border-radius:8px;white-space:pre}code{font:0.88em/1.6 ui-monospace,monospace;background:#edf2f6;padding:2px 4px;border-radius:3px;overflow-wrap:anywhere}pre code{background:none;padding:0;color:inherit;font-size:inherit;overflow-wrap:normal}.table{overflow:auto}table{border-collapse:collapse;min-width:650px;width:100%;font-size:15px}td,th{border:1px solid #d4dfe7;padding:11px;vertical-align:top;text-align:left}th{background:#eaf1f7}tr:nth-child(even){background:#f8fafc}blockquote{border-left:4px solid #3a7c9f;margin:18px 0;padding:12px 20px;background:#f1f6fa}li{margin:8px 0}.badge{font-size:14px;display:inline-block;background:#28516b;padding:4px 10px;border-radius:20px}footer{text-align:center;padding:20px;color:#657583}@media(max-width:600px){body{font-size:16px}main{padding:10px}nav,article{padding:18px}h1{font-size:25px}h2{font-size:24px}h3{font-size:20px}header{padding:26px 20px}}@media print{body{background:white;font-size:11pt}header{background:white;color:black;padding:0}header p{color:black}nav{page-break-after:always}article{border:0;padding:0;border-radius:0;page-break-before:always}.table{overflow:visible}table{min-width:0;font-size:9pt}pre{white-space:pre-wrap;background:#f5f5f5;color:black;font-size:9pt}a{color:black}}'''
    page='<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>STARCORE 详细落实设计方案</title><style>'+css+'</style></head><body><header><span class="badge">2026-10-22 · SU1</span><h1>STARCORE 策略创造能力<br>详细落实设计方案</h1><p>'+str(nbatch)+'个施工批次 · '+str(nreq)+'条需求 · 源码接入 · 可执行参考 · 数据库与验收 · R10 审阅修订</p><p>基线 9ec590cafbd7cbcd1265a11b63a59df53ba78fcd</p></header><main><nav><strong>完整目录</strong>'+toc+'</nav>'+sections+'</main><footer>离线文档，无外部脚本或网络依赖。测试范围与施工状态以正文及回执为准。</footer></body></html>'
    (ROOT/'README.html').write_text(page)
    (ROOT/'FULL_DESIGN.md').write_text('\n\n---\n\n'.join(p.read_text() for p in files))
    print('Built README.html and FULL_DESIGN.md:',len(files),'chapters')

if __name__=='__main__': main()
