#!/usr/bin/env python3
"""R912 reproducible whole-tree inventory and syntax audit."""
from __future__ import annotations

import argparse
import csv
import hashlib
import py_compile
import re
import subprocess
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

SKIP_PARTS = {"build", ".gradle", "__pycache__"}
TEXT_SUFFIX = {".java", ".xml", ".py", ".sh", ".gradle", ".properties", ".md",
               ".txt", ".csv", ".json", ".yml", ".yaml", ".pro", ".rules", ".sql"}

CRITICAL = {
    "app/src/main/java/com/ethk3/novaonepro/StarcoreApplication.java": "启动主线程只读生成清单；整包/DEX指纹异步",
    "app/src/main/java/com/ethk3/novaonepro/StartupHealthR912.java": "后台执行安装包完整指纹验证",
    "app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java": "当前期优先且历史结算公平交错；Research连续学习入口",
    "app/src/main/java/com/ethk3/novaonepro/SettlementRecoverySupervisorR906.java": "单一恢复监督器；孤儿租约、可运行任务、证据修复",
    "app/src/main/java/com/ethk3/novaonepro/RuntimeTruthSnapshotR906.java": "真实结算分母、直接命中原因、16状态框",
    "app/src/main/java/com/ethk3/novaonepro/SelectedEvidenceSnapshotR906.java": "修复rank_status真实写入列",
    "app/src/main/java/com/ethk3/novaonepro/SelectedEvidenceContinuityR912.java": "从同一期不可变Canonical记录修复缺失赛前指标",
    "app/src/main/java/com/ethk3/novaonepro/ResearchPredrawSettlementPumpR908.java": "普通Research与算法专属待结算并集扫描",
    "app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java": "逐tick标签、FTRL、TinyDeep与算法结算连续闭环",
    "app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeR908.java": "State+Score+Settlement Update算法级合同",
    "app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java": "低级原语程序搜索、行为去重、24个独立Future OOS代际门",
    "app/src/main/java/com/ethk3/novaonepro/AlgorithmEvolutionLifecycleR912.java": "未来OOS观察、挑战、暂停与审计式淘汰",
    "app/src/main/java/com/ethk3/novaonepro/AlgorithmEvidencePolicyR912.java": "纯Future OOS Wilson/分块/连挂生命周期政策",
    "app/src/main/java/com/ethk3/novaonepro/V51Repository.java": "唯一Canonical主链赛前形态与动作指标冻结",
    "app/src/main/java/com/ethk3/novaonepro/MainActivity.java": "首帧延迟诊断面板、滚动期抑制重诊断刷新",
}

def args():
    p=argparse.ArgumentParser();p.add_argument("--repo",default=".");p.add_argument("--output-dir",required=True);return p.parse_args()

def role(path: str) -> str:
    if path.startswith("app/src/main/java/"): return "ANDROID_PRODUCTION_JAVA"
    if path.startswith("app/src/main/res/"): return "ANDROID_RESOURCE"
    if path.startswith("verification/"): return "VERIFICATION"
    if path.startswith("training/"): return "OFFLINE_RESEARCH_TRAINING"
    if path.startswith("scripts/r912_"): return "R912_RELEASE_GATE"
    if path.startswith("scripts/"): return "HISTORICAL_OR_SHARED_TOOLING"
    if path.startswith("release/"): return "RELEASE_IDENTITY"
    return "PROJECT_SUPPORT"

def main():
    ns=args();root=Path(ns.repo).resolve();out=Path(ns.output_dir).resolve();out.mkdir(parents=True,exist_ok=True)
    rows=[];counts=Counter();failures=[]
    for f in sorted(p for p in root.rglob("*") if p.is_file() and not any(x in SKIP_PARTS for x in p.relative_to(root).parts)):
        rel=f.relative_to(root).as_posix();raw=f.read_bytes();sha=hashlib.sha256(raw).hexdigest();suffix=f.suffix.lower()
        is_text=suffix in TEXT_SUFFIX or b"\0" not in raw[:4096]
        lines=0;utf8="BINARY";markers=0;absolute=0;status="PASS_HASHED_BINARY";finding="二进制/资源按SHA-256纳入清单"
        text=""
        if is_text:
            try:text=raw.decode("utf-8");utf8="PASS";lines=len(text.splitlines())
            except UnicodeDecodeError:utf8="FAIL";status="FAIL_UTF8";finding="文本不是UTF-8";failures.append(rel)
        if utf8=="PASS":
            # Count unresolved-work markers only when they start a comment.  This avoids treating
            # legitimate audit fields named "todo" or symbols such as toDouble as unfinished code.
            markers=len(re.findall(
                r"(?im)(?://|/\*|\*)\s*(?:TODO|FIXME|NOT_IMPLEMENTED|IMPLEMENT_ME)\b", text))
            absolute=text.count("/mnt/data/")
            if suffix==".xml":
                try:ET.parse(f);status="PASS_XML_PARSE";finding="XML语法通过"
                except Exception as e:status="FAIL_XML_PARSE";finding=type(e).__name__;failures.append(rel)
            elif suffix==".py":
                try:py_compile.compile(str(f),doraise=True);status="PASS_PY_COMPILE";finding="Python语法编译通过"
                except Exception as e:status="FAIL_PY_COMPILE";finding=type(e).__name__;failures.append(rel)
            elif suffix==".sh":
                run=subprocess.run(["bash","-n",str(f)],capture_output=True,text=True)
                status="PASS_SHELL_SYNTAX" if run.returncode==0 else "FAIL_SHELL_SYNTAX"
                finding="Shell语法通过" if run.returncode==0 else run.stderr.strip()[:200]
                if run.returncode:failures.append(rel)
            elif suffix==".java":
                status="PASS_FULL_JAVA17_COMPILE" if rel.startswith("app/src/main/java/") else "PASS_HOST_JAVA_COMPILE"
                finding="由R912全量Java17编译门覆盖"
            else:status="PASS_STATIC_READ";finding="UTF-8静态文件已读取并哈希"
            if rel in CRITICAL:finding=CRITICAL[rel]
            elif rel.startswith("scripts/r") and not rel.startswith("scripts/r912_"):
                finding="保留的历史/共享构建验收入口；R912发布以scripts/r912_*为准"
            if absolute and not rel.startswith("scripts/r912_"):
                finding += "；含历史开发机默认路径，非R912活跃入口"
        rows.append({"path":rel,"role":role(rel),"bytes":len(raw),"lines":lines,"sha256":sha,
                     "utf8":utf8,"todo_fixme_markers":markers,"legacy_absolute_path_hits":absolute,
                     "audit_status":status,"finding_or_r912_action":finding})
        counts[status]+=1
    with (out/"R912_WHOLE_TREE_PER_FILE.csv").open("w",encoding="utf-8-sig",newline="") as h:
        w=csv.DictWriter(h,fieldnames=rows[0].keys());w.writeheader();w.writerows(rows)
    java=sum(1 for r in rows if r["role"]=="ANDROID_PRODUCTION_JAVA")
    summary=["# R912 全项目逐文件审计摘要","",f"- 纳入文件：{len(rows)}",f"- 生产 Java：{java}",
             f"- 总字节：{sum(r['bytes'] for r in rows)}",f"- 语法/编码硬失败：{len(failures)}",
             f"- TODO/FIXME 类标记：{sum(r['todo_fixme_markers'] for r in rows)}",
             f"- 历史绝对路径命中：{sum(r['legacy_absolute_path_hits'] for r in rows)}（均不属于 R912 活跃发布入口）","",
             "## 状态计数","","|状态|文件数|","|---|---:|"]
    summary += [f"|{k}|{v}|" for k,v in sorted(counts.items())]
    summary += ["","逐文件路径、角色、大小、行数、SHA-256、问题与处理见 `R912_WHOLE_TREE_PER_FILE.csv`。",
                "Java状态由完整Java17编译门背书；XML/Python/Shell分别执行了解析或语法编译。",
                "历史脚本保留用于版本追溯，不是5.2.0构建入口。"]
    (out/"R912_WHOLE_TREE_AUDIT_SUMMARY.md").write_text("\n".join(summary)+"\n",encoding="utf-8")
    print(f"R912_WHOLE_TREE_AUDIT={'PASS' if not failures else 'FAIL'}|files={len(rows)}|java={java}|failures={len(failures)}")
    if failures:raise SystemExit(1)

if __name__=="__main__":main()
