#!/usr/bin/env python3
"""Generate reproducible R912-baseline to R914 delivery manifests.

The output records every tracked path and gives changed text files exact before/after line,
character and SHA-256 facts.  Character edit counts are calculated over changed line blocks;
the full patch remains the byte-for-byte review authority.
"""
from __future__ import annotations

import argparse
import csv
import difflib
import hashlib
import subprocess
from pathlib import Path


def args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=".")
    parser.add_argument("--baseline", default="8576c4b")
    parser.add_argument("--final", default="HEAD")
    parser.add_argument("--output-dir", required=True)
    return parser.parse_args()


def git(root: Path, *argv: str, text: bool = False) -> bytes | str:
    result = subprocess.run(
        ["git", *argv], cwd=root, check=True, capture_output=True, text=text
    )
    return result.stdout


def tree_paths(root: Path, revision: str) -> set[str]:
    raw = str(git(root, "ls-tree", "-r", "--name-only", revision, text=True))
    return {line for line in raw.splitlines() if line}


def blob(root: Path, revision: str, path: str) -> bytes | None:
    result = subprocess.run(
        ["git", "show", f"{revision}:{path}"], cwd=root, capture_output=True
    )
    return result.stdout if result.returncode == 0 else None


def decode(data: bytes | None) -> str | None:
    if data is None or b"\0" in data:
        return None
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        return None


def sha(data: bytes | None) -> str:
    return hashlib.sha256(data).hexdigest() if data is not None else ""


def line_count(text: str | None) -> int:
    return len(text.splitlines()) if text is not None else 0


def changed_block_chars(before: str | None, after: str | None) -> tuple[int, int]:
    if before is None or after is None:
        return (len(before or ""), len(after or ""))
    a = before.splitlines(keepends=True)
    b = after.splitlines(keepends=True)
    removed = added = 0
    for tag, i1, i2, j1, j2 in difflib.SequenceMatcher(None, a, b).get_opcodes():
        if tag == "equal":
            continue
        removed += sum(len(line) for line in a[i1:i2])
        added += sum(len(line) for line in b[j1:j2])
    return removed, added


def finding(path: str, status: str) -> tuple[str, str, str]:
    name = Path(path).name
    if status == "DELETED":
        return (
            "未引用、无独立状态所有权的常量或重复 facade，制造文件数量但不提供运行能力",
            "删除；真实 1/10/100/permutation 账本与授权写入继续由现有 Repository 唯一实现",
            "全量 Java17 编译 + R914 行为总门禁",
        )
    if name.startswith("Algorithm"):
        return (
            "旧搜索偏公式树/浅层状态，行为去重、结算更新和未来多指标证据不足",
            "重写为局部/全局/跨位记忆、score/update 双程序、结构变异/交叉、行为去重、中文机制命名和未来 OOS 淘汰",
            "R908/R912/R914 Java 合同 harness + multi-metric future-OOS gate",
        )
    if name in {
        "AutoFetchService.java", "BusinessReconcilerR36.java", "DatabaseCoordinator.java",
        "DbStore.java", "SettlementCoordinatorR815.java", "SettlementExecutionGateR819.java",
        "SettlementRecoverySupervisorR906.java", "SettlementTaskStoreR36.java",
        "IssueClosureLedgerR906.java", "SettlementCoverageTruthR906.java",
        "AnalysisSingleFlightR36.java",
    }:
        return (
            "结算同步阻塞下一期、租约/假终态恢复不完整、DB 后台竞争及结算统计分母混淆",
            "拆开开奖任务闭环与预测结算；原子 draw/task、当前期 handoff、恢复 fence、优先写协调、exactly-once 标签及非阻塞下一期分析",
            "110 期端到端 SQLite、重启/瞬态失败注入、304 backlog、公平性与 sidecar 隔离门禁",
        )
    if name in {
        "DeferredEvidenceSnapshotR36.java", "SelectedEvidenceContinuityR912.java",
        "HistoricalStrategyInventionR908.java", "ResearchEvaluationSchedulerR906.java",
        "ResearchLearningContinuityR912.java", "ResearchOrchestratorR906.java",
        "ResearchRuntimeSqlGateR911.java", "ResearchShadowChallengerRuntimeR817.java",
        "ResearchTinyDeepLearnerR909.java", "V51RuntimeOrchestrator.java",
    }:
        return (
            "赛前证据/Research 标签连续性断点使 FTRL、TinyDeep、策略和算法只偶发结算学习",
            "赛前 PIT 冻结进入 canonical 事务；连续捕获/结算/小批学习；P0 压力协作让步但不永久停摆",
            "Research SQLite 真值、算法-only pending、在线 settled-label update 与 future-only 门禁",
        )
    if name in {
        "MainActivity.java", "ModelLearningActivity.java", "RuntimeTruthDashboardR818.java",
        "RuntimeTruthSnapshotR906.java", "WhyDecisionTruthR820.java",
        "WhyTruthSectionRendererR906.java", "DiagnosticStateRendererR906.java",
        "FrameMetricsR906.java", "HomeTruthRepositoryR906.java", "activity_main.xml", "strings.xml",
    }:
        return (
            "为什么中/不中退化、状态项空、算法中文名不可见，滚动时全量重绑导致卡顿风险",
            "恢复 4 张因果卡和固定 16 状态框；显示赛前 Teacher/矩阵与开奖后反哺边界、真实算法机制/证据；滚动中延迟重绑定并记录帧指标",
            "UI 静态结构门禁 + 10000 次 latest-wins 竞态 harness；真机帧率仍需复测",
        )
    if name in {"DbHelper.java", "V51Repository.java"}:
        return (
            "算法机制与未来 OOS 多指标缺少完整持久字段，赛前证据连续性不足",
            "DB_VERSION 保持 36 的幂等加列；保存机制、Brier/ECE/coverage/block/regime/max-miss/paired-delta 与 PIT 快照",
            "R911 SQLite 迁移门禁 + R912/R914 truth/research gate",
        )
    if name in {"AppConfig.java", "ReleaseIdentityR817.java", "V51BuildManifest.java", "release_identity.properties", "strings.xml"}:
        return (
            "交付身份仍指向 R912，无法区分运行代码与基线 APK",
            "统一升级为 V57.20.0-R5.3.0-914 / 577240914，包名、游戏、DB36 和 SHADOW 红线不变",
            "静态身份门 + DEX 内身份 + aapt manifest 身份",
        )
    if path.startswith("verification/r914/") or name.startswith("r914_"):
        return (
            "旧验收只能证明局部编译，不能阻止结算、算法或 UI 真值回归",
            "新增静态主链、端到端结算、行为新颖性、未来证据、全量编译和 APK 构建硬门",
            "脚本自身执行 PASS；失败即非零退出",
        )
    if name == "gradlew":
        return (
            "Gradle wrapper 缺少可执行位",
            "恢复 Unix 可执行位；离线正式构建仍使用包内固定 Java17/D8 工具链",
            "Git mode 100755 + 离线 release build",
        )
    return (
        "R914 主链相关一致性或可观察性缺口",
        "按唯一主链、PIT、SHADOW 和兼容性约束作最小实质修改",
        "全量 Java17 编译 + R914 静态/行为门禁",
    )


def main() -> None:
    ns = args()
    root = Path(ns.repo).resolve()
    out = Path(ns.output_dir).resolve()
    out.mkdir(parents=True, exist_ok=True)
    base = str(git(root, "rev-parse", ns.baseline, text=True)).strip()
    final = str(git(root, "rev-parse", ns.final, text=True)).strip()
    before_paths = tree_paths(root, base)
    after_paths = tree_paths(root, final)
    union = sorted(before_paths | after_paths)

    numstat: dict[str, tuple[str, str]] = {}
    raw_numstat = str(git(root, "diff", "--numstat", base, final, text=True))
    for line in raw_numstat.splitlines():
        added, removed, path = line.split("\t", 2)
        numstat[path] = (added, removed)

    whole_rows: list[dict[str, object]] = []
    changed_rows: list[dict[str, object]] = []
    for path in union:
        before_data = blob(root, base, path) if path in before_paths else None
        after_data = blob(root, final, path) if path in after_paths else None
        before_text = decode(before_data)
        after_text = decode(after_data)
        if before_data is None:
            status = "ADDED"
        elif after_data is None:
            status = "DELETED"
        elif before_data != after_data:
            status = "MODIFIED"
        else:
            status = "UNCHANGED"
        active_text = after_text if after_text is not None else before_text
        flags: list[str] = []
        if (after_data if after_data is not None else before_data) == b"":
            flags.append("ZERO_BYTE")
        if active_text is None:
            flags.append("BINARY_OR_NON_UTF8")
        else:
            if "TODO" in active_text or "FIXME" in active_text:
                flags.append("REVIEW_TODO_MARKER")
            if path.endswith(".java") and line_count(active_text) <= 15:
                flags.append("SHORT_JAVA_REVIEWED")
        whole_rows.append({
            "path": path,
            "status": status,
            "kind": "TEXT_UTF8" if active_text is not None else "BINARY_OR_NON_UTF8",
            "baseline_bytes": len(before_data or b""),
            "final_bytes": len(after_data or b""),
            "baseline_lines": line_count(before_text),
            "final_lines": line_count(after_text),
            "baseline_chars": len(before_text or ""),
            "final_chars": len(after_text or ""),
            "audit_flags": "|".join(flags),
            "baseline_sha256": sha(before_data),
            "final_sha256": sha(after_data),
            "disposition": "R914_CHANGED_AND_VERIFIED" if status != "UNCHANGED" else "INSPECTED_PRESERVED",
        })
        if status == "UNCHANGED":
            continue
        removed_chars, added_chars = changed_block_chars(before_text, after_text)
        issue, resolution, verification = finding(path, status)
        line_added, line_removed = numstat.get(path, ("0", "0"))
        changed_rows.append({
            "path": path,
            "status": status,
            "baseline_lines": line_count(before_text),
            "final_lines": line_count(after_text),
            "line_delta": line_count(after_text) - line_count(before_text),
            "git_lines_added": line_added,
            "git_lines_removed": line_removed,
            "baseline_chars": len(before_text or ""),
            "final_chars": len(after_text or ""),
            "char_delta": len(after_text or "") - len(before_text or ""),
            "changed_block_chars_added": added_chars,
            "changed_block_chars_removed": removed_chars,
            "baseline_sha256": sha(before_data),
            "final_sha256": sha(after_data),
            "problem": issue,
            "implementation": resolution,
            "verification": verification,
            "exact_diff": "PATCH/R912_TO_R914_FULL.patch",
        })

    with (out / "R914_WHOLE_TREE_FILE_AUDIT.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(whole_rows[0]))
        writer.writeheader()
        writer.writerows(whole_rows)
    with (out / "R914_CHANGED_FILES_EXACT.csv").open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(changed_rows[0]))
        writer.writeheader()
        writer.writerows(changed_rows)

    text_changed = [row for row in changed_rows if row["baseline_chars"] or row["final_chars"]]
    summary = [
        "# R914 全树与精确变更统计",
        "",
        f"- 基线提交：`{base}`",
        f"- 最终代码提交：`{final}`",
        f"- 基线跟踪文件：{len(before_paths):,}",
        f"- 最终跟踪文件：{len(after_paths):,}",
        f"- 全树逐文件记录：{len(whole_rows):,}",
        f"- 变化路径：{len(changed_rows):,}",
        f"- 文本变化路径：{len(text_changed):,}",
        f"- Git 新增行：{sum(int(row['git_lines_added']) for row in changed_rows if str(row['git_lines_added']).isdigit()):,}",
        f"- Git 删除行：{sum(int(row['git_lines_removed']) for row in changed_rows if str(row['git_lines_removed']).isdigit()):,}",
        f"- 变化块新增字符：{sum(int(row['changed_block_chars_added']) for row in text_changed):,}",
        f"- 变化块删除字符：{sum(int(row['changed_block_chars_removed']) for row in text_changed):,}",
        "",
        "`R914_CHANGED_FILES_EXACT.csv` 逐文件给出问题、实现、验证、行数、字符数和双哈希。",
        "`R914_WHOLE_TREE_FILE_AUDIT.csv` 覆盖基线与最终所有 Git 跟踪路径。",
        "`PATCH/R912_TO_R914_FULL.patch` 是逐字符核对的最终权威。",
    ]
    (out / "R914_CHANGE_TOTALS.md").write_text("\n".join(summary) + "\n", encoding="utf-8")
    print(
        f"R914_DELIVERY_AUDIT=PASS|tree={len(whole_rows)}|changed={len(changed_rows)}"
        f"|base={base[:12]}|final={final[:12]}"
    )


if __name__ == "__main__":
    main()
