#!/usr/bin/env python3
"""从 tools/cards_r30.py 生成 registries/*.json、docs/B*.md 与 baseline/ 快照。
用法：python3 tools/build_plan.py --plan <方案包目录> --base-repo <bb607cf 工作树> --r29-plan <R29方案包目录>
（方案作者使用；施工方只读生成结果。）"""
import argparse, hashlib, importlib.util, json, os, re, shutil, subprocess, sys

ap = argparse.ArgumentParser(); ap.add_argument("--plan", required=True); ap.add_argument("--base-repo", required=True); ap.add_argument("--r29-plan", required=True)
ap.add_argument("--method-index"); a = ap.parse_args()
spec = importlib.util.spec_from_file_location("cards", os.path.join(a.plan, "tools", "cards_r30.py")); C = importlib.util.module_from_spec(spec); spec.loader.exec_module(C)
REG = os.path.join(a.plan, "registries"); os.makedirs(REG, exist_ok=True)
DOCS = os.path.join(a.plan, "docs"); os.makedirs(DOCS, exist_ok=True)

def git_show(path):
    r = subprocess.run(["git", "-C", a.base_repo, "show", f"{C.BASE}:{path}"], capture_output=True)
    return r.stdout if r.returncode == 0 else None

# ---------------------------------------------------------------- baseline snapshot of every MODIFY file (pristine bb607cf bytes)
BL = os.path.join(a.plan, "baseline"); shutil.rmtree(BL, ignore_errors=True); os.makedirs(BL)
files = []
modify_paths = sorted({f["path"] for c in C.CARDS for f in c["files"] if f["action"] == "MODIFY" and not f["path"].endswith("/")})
extra = ["app/src/main/java/com/ethk3/novaonepro/RealtimePressureGateR811.java", "app/src/main/java/com/ethk3/novaonepro/SettlementExecutionGateR819.java",
         "app/src/main/java/com/ethk3/novaonepro/ResearchWorkBudgetR927.java", "app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java",
         "app/src/main/java/com/ethk3/novaonepro/strategyv2/StatsR27.java", "app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicySpecR27.java",
         "app/src/main/java/com/ethk3/novaonepro/strategyv2/PolicyAstR27.java", "app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchWriteQueueR27.java",
         "app/src/main/java/com/ethk3/novaonepro/r952/ResearchOutboxR952.java", "app/src/main/java/com/ethk3/novaonepro/TargetIssueResolver.java",
         "tools/r29/ci_build.sh", ".github/workflows/r29.yml", "tools/r29/r29-official-build.yml", "tools/r29/r29-build.yml", "app/build.gradle"]
missing = []
for p in modify_paths + [e for e in extra if e not in modify_paths]:
    b = git_show(p)
    if b is None: missing.append(p); continue
    dst = os.path.join(BL, "bb607cf", p); os.makedirs(os.path.dirname(dst), exist_ok=True); open(dst, "wb").write(b)
    files.append({"path": p, "sha256": hashlib.sha256(b).hexdigest(), "bytes": len(b), "role": "MODIFY_TARGET" if p in modify_paths else "REFERENCE"})
json.dump({"base": C.BASE, "files": files, "missing_at_base": missing}, open(os.path.join(BL, "BASELINE_MANIFEST.json"), "w"), ensure_ascii=False, indent=1)

# ---------------------------------------------------------------- method index (for symbol locating)
midx = json.load(open(a.method_index)) if a.method_index else {}

# ---------------------------------------------------------------- registries
batches = {b["id"]: b for b in C.BATCHES}
order = {}
for c in C.CARDS:
    order.setdefault(c["batch"], []).append(c["id"])
cards_out = []
for c in C.CARDS:
    d = dict(c); d.setdefault("forbid", []); d["forbid"] = d["forbid"] + C.COMMON_FORBID
    seq = order[c["batch"]]; i = seq.index(c["id"])
    d["order_in_batch"] = i + 1; d["depends"] = [seq[i - 1]] if i > 0 else ([order[C.BATCHES[[b["id"] for b in C.BATCHES].index(c["batch"]) - 1]["id"]][-1]] if c["batch"] != "B00" else [])
    if c["batch"] == "B11": d["depends"] = (["R30-B10-01"] if i == 0 else d["depends"])
    for f in d["files"]:
        f["exists_at_base"] = git_show(f["path"]) is not None if not f["path"].endswith("/") else None
        m = midx.get(f["path"])
        if m:
            f["base_sha256"] = m["sha256"]; f["base_lines"] = m["lines"]
    d["status"] = "TODO"; d["verification"] = "NOT_RUN"
    cards_out.append(d)
json.dump({"base": C.BASE, "branch": C.BRANCH, "cards": cards_out}, open(os.path.join(REG, "cards.json"), "w"), ensure_ascii=False, indent=1)
json.dump({"batches": [dict(b, cards=order[b["id"]]) for b in C.BATCHES]}, open(os.path.join(REG, "batches.json"), "w"), ensure_ascii=False, indent=1)

# ownership: per card files; per batch shared-file list (allowed only sequentially with disjoint symbols)
own = {"cards": {}, "batch_shared_sequential": {}}
for c in cards_out: own["cards"][c["id"]] = {"batch": c["batch"], "files": [f["path"] for f in c["files"]] + [p for p in c.get("code", []) if p.startswith("code/main")]}
for b in C.BATCHES:
    seen = {}
    for cid in order[b["id"]]:
        c = next(x for x in cards_out if x["id"] == cid)
        for f in c["files"]: seen.setdefault(f["path"], []).append({"card": cid, "symbols": f["symbols"]})
    shared = {p: v for p, v in seen.items() if len(v) > 1}
    if shared: own["batch_shared_sequential"][b["id"]] = shared
# map code/main paths to app paths for ownership_check
for cid, v in own["cards"].items():
    v["files"] = sorted({("app/src/main/java/" + p[len("code/main/"):]) if p.startswith("code/main/") else p for p in v["files"]})
json.dump(own, open(os.path.join(REG, "file_ownership.json"), "w"), ensure_ascii=False, indent=1)

# metrics with R30 measurement source
MEAS = {
 "M01": "构建身份：release_manifest.json + APK 签名指纹 + 源码提交（tools/r29/package_receipt.py）",
 "M02": "保全清单 1832/299（legacy_count.py）+ 生命周期回归 androidTest",
 "M03": "r30_issue_timeline：request_to_frozen 与 settled_to_frozen 段 P95（IssueTimelineR30.summarize）",
 "M04": "r30_issue_timeline：observe_to_settled 段 P95",
 "M05": "r30_issue_timeline：NEXT_DEADLINE−NEXT_FROZEN ≥5000ms 的期数占比",
 "M06": "结算表唯一键冲突计数 + 同 issue REAL 意向计数（SQL 见 docs/90）",
 "M07": "r30_writer_hold critical=1 的 wait_p95 最大值；24h 失败数=sum(failures)",
 "M08": "单库模式：SCHEDULER_OWNERS.json 中同表双写者数；B11 启用后：日志 R30_RESEARCH_TABLE_FROZEN 次数",
 "M09": "r30_copy_log COUNT_MISMATCH 行数 + B11 ContentHash 不一致数",
 "M10": "每期训练投递：r952_research_outbox 状态分布 + QualificationInboxR29 连续游标",
 "M11": "TaskLivenessR30：最老积压期数（pending 最大任务）",
 "M12": "TaskLivenessR30.summarize.m12（null=有UNKNOWN）",
 "M13": "B10 场景：前后台切换前后已投递任务数差",
 "M14": "进程崩溃/ANR 计数（logcat + ApplicationExitInfo）",
 "M15": "draws 热库行数 + 应用数据目录总字节",
 "M16": "ReportTaskControlR69 取消到释放耗时",
 "M17": "报告索引覆盖率 + ZIP 大小/分卷",
 "M18": "r30_pit_violation 非注入行数",
 "M19": "HoldoutLedgerR27 R30_CONFIRM_REUSED 计数",
 "M20": "ModelArtifactR29 中断/恢复一致性测试结果",
 "M21": "BirthGateR27 R30_CONTRACT: 拒绝码分布；入池策略合同违规=0",
 "M22": "r30_behavior_dup 行数与报告去重计数一致",
 "M23": "CapabilityRegistryR30 对象可执行率",
 "M24": "r30_model_call 与冻结行匹配率",
 "M25": "MatrixRegistryR30 20 项可追溯率",
 "M26": "校准样本时间隔离检查通过率",
 "M27": "SequentialQualificationR27 全局序号连续、无重置",
 "M28": "androidTest 正对照贯通 + 非法/过期拦截",
 "M29": "确认队列被挤出次数",
 "M30": "r30_decision_explain：selectorConfirmed=FAIL 时 REAL 次数（必须0）",
 "M31": "DiagnosisRulesR30 规则行动覆盖率",
 "M32": "同快照两章同指标冲突数",
 "M33": "图表页面 androidTest 截图通过数",
 "M34": "覆盖安装首启/二启无崩溃（两证书）",
 "M35": "证书撤销到 DecisionGate 拒绝的耗时",
 "M36": "NullEvolutionR27 同池对照输出可复算",
 "Q01": "真机 FUTURE ACT 命中率（事前登记策略）",
 "Q02": "真机实际出手频率（不补配额）",
 "Q03": "SequentialQualificationR27 logEUp ≥ log(1/α_i)",
 "Q04": "事前冻结窗口规则下各块命中率",
 "Q05": "CalibrationR30：等质量ECE、BSS、支持覆盖（与等宽ECE并列）",
 "Q06": "父子配对差单侧95%下界",
 "Q07": "ACTIVE 证书资产数",
 "Q08": "R30SequentialNullSimTest 空信号/功效 + NullEvolutionR27",
}
r29m = json.load(open(os.path.join(a.r29_plan, "registries", "acceptance_metrics.json")))
metrics = []
for m in r29m:
    d = {k: m[k] for k in ("id", "name", "definition", "target", "kind", "unit")}
    d["r30_measurement"] = MEAS[m["id"]]; d["cards"] = [c["id"] for c in cards_out if m["id"] in c["metrics"]]
    d["environment"] = "DEVICE" if m["id"] in ("M03", "M04", "M05", "M07", "M13", "M14", "M15", "M34", "Q01", "Q02", "Q03", "Q04", "Q05", "Q07") else "HOST_OR_EMULATOR_ALLOWED"
    d["missing_policy"] = "null/INSUFFICIENT；不得写0或PASS"
    metrics.append(d)
json.dump(metrics, open(os.path.join(REG, "metrics_r30.json"), "w"), ensure_ascii=False, indent=1)

# crosswalk R29 -> R30
r29c = json.load(open(os.path.join(a.r29_plan, "registries", "construction_cards.json")))
cw = []
for c in r29c:
    tgt = [x["id"] for x in cards_out if c["id"] in x["r29"]]
    cw.append({"r29": c["id"], "r29_batch": c["batch"], "r29_title": c["title"], "r30": tgt})
json.dump(cw, open(os.path.join(REG, "r29_to_r30_crosswalk.json"), "w"), ensure_ascii=False, indent=1)

# ---------------------------------------------------------------- per-batch card docs
def md_card(c):
    L = [f"### {c['id']} {c['title']}", "", f"- 批次内顺序：第 {c['order_in_batch']} 张；前置：{', '.join(c['depends']) or '无'}；对应R29：{', '.join(c['r29'])}；指标：{', '.join(c['metrics'])}", "", "**独占文件**", "", "|文件|动作|改动范围|bb607cf 行数|", "|---|---|---|---|"]
    for f in c["files"]:
        L.append(f"|`{f['path']}`|{f['action']}|{'；'.join(f['symbols']) or '—'}|{f.get('base_lines', '—')}|")
    L += ["", "**施工步骤**", ""] + [f"{i+1}. {s}" for i, s in enumerate(c["steps"])]
    if c.get("code"): L += ["", "**本包代码**：" + "；".join(f"`{x}`" for x in c["code"])]
    if c.get("tests"): L += ["", "**测试**：" + "；".join(c["tests"])]
    L += ["", "**验收（全部满足才可标DONE）**", ""] + [f"- {x}" for x in c["accept"]]
    L += ["", "**禁止**：" + "；".join(c["forbid"]), "", f"**提交信息格式**：`{c['id']} <一句话>`；提交后运行 `python3 tools/r30/ownership_check.py --card {c['id']} --base <上一卡提交>`。", ""]
    return "\n".join(L)

for b in C.BATCHES:
    name = re.sub(r"[【】（）()：:，, /]", "_", b["name"]).strip("_")
    path = os.path.join(DOCS, f"{b['id']}_{name}.md")
    head = open(path, encoding="utf-8").read().split("<!-- CARDS -->")[0] if os.path.exists(path) and "<!-- CARDS -->" in open(path, encoding="utf-8").read() else f"# {b['id']} {b['name']}\n\n"
    if "<!-- CARDS -->" not in head:
        head = head.rstrip() + "\n\n"
    body = [head.rstrip() + "\n\n<!-- CARDS -->", "", f"**目标**：{b['goal']}", "", f"**批次出口**：{b['exit']}", "", f"**versionCode**：{b['versionCode']}（批次内每次重新出包 +1，不得回退）", ""]
    sh = own["batch_shared_sequential"].get(b["id"])
    if sh:
        body += ["**本批次顺序共享文件**（同一文件被本批次两张卡改动：只能按卡序先后施工，改动方法互不重叠）：", ""]
        for p, v in sh.items(): body.append(f"- `{p}`：" + " → ".join(f"{x['card']}（{'；'.join(x['symbols'])}）" for x in v))
        body.append("")
    body += [md_card(c) for c in cards_out if c["batch"] == b["id"]]
    open(path, "w", encoding="utf-8").write("\n".join(body))
print(json.dumps({"cards": len(cards_out), "baseline_files": len(files), "missing_at_base": missing, "shared": {k: list(v) for k, v in own["batch_shared_sequential"].items()}}, ensure_ascii=False, indent=1))
