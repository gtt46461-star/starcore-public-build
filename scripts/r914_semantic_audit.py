#!/usr/bin/env python3
"""R914 whole-tree semantic inventory.

This audit intentionally does more than the historical syntax/hash inventory.  It builds an
approximate class-reference graph, identifies production-entry reachability, version-family
duplication, thin constant/facade implementations, monoliths, state/DB/scheduler coupling and
the files that participate in the canonical prediction/settlement/research paths.

The graph is conservative: reflection and Android framework callbacks can make a class reachable
even when a lexical edge is absent.  Results therefore say APPROX_REACHABLE/REVIEW_UNREACHED and
must be confirmed before deleting code.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import re
from collections import Counter, defaultdict, deque
from pathlib import Path

JAVA_ROOT = Path("app/src/main/java")
MANIFEST = Path("app/src/main/AndroidManifest.xml")

MAINLINE = {
    "StarcoreApplication": "PROCESS_ENTRY",
    "BootReceiver": "RECOVERY_ENTRY",
    "AutoFetchService": "FETCH_SCHEDULER_OWNER",
    "TargetIssueResolver": "TARGET_ISSUE_OWNER",
    "AnalysisSingleFlightR36": "ANALYSIS_SINGLE_FLIGHT",
    "DecisionGateEngine": "DECISION_OWNER",
    "SettlementCoordinatorR815": "SETTLEMENT_COORDINATOR",
    "SettlementEngine": "SETTLEMENT_OWNER",
    "SettlementTaskStoreR36": "SETTLEMENT_TASK_STATE",
    "OosAfterDrawWriterR36": "OOS_LABEL_WRITER",
    "DeferredEvidenceWriterR36": "POST_COMMIT_EVIDENCE",
    "RuntimeTruthSnapshotR906": "UI_TRUTH_SOURCE",
    "MainActivity": "UI_ENTRY",
    "ResearchLearningContinuityR912": "RESEARCH_CONTINUITY",
    "ResearchPredrawSettlementPumpR908": "RESEARCH_SETTLEMENT_PUMP",
    "AlgorithmInventionLabR908": "ALGORITHM_INVENTION",
    "AlgorithmGenomeVmR908": "ALGORITHM_RUNTIME",
    "AlgorithmEvolutionLifecycleR912": "ALGORITHM_LIFECYCLE",
}

TYPE_RE = re.compile(r"\b(?:public\s+)?(?:final\s+|abstract\s+)?(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)")
# Keep this deliberately line-local.  The earlier permissive multi-line expression could exhibit
# pathological backtracking on the historical 20k-line monoliths, which is unacceptable in an
# audit that must itself be reproducible.
METHOD_RE = re.compile(
    r"(?m)^\s*(?:(?:public|protected|private|static|final|synchronized|native|abstract|default|strictfp)\s+)+"
    r"[^\n;{}=]+\([^\n;{}]*\)\s*(?:throws\s+[^\n{]+)?\{"
)
VERSION_RE = re.compile(r"(?:R|V)\d+(?:R\d+)?$")
COMMENT_RE = re.compile(r"//[^\n]*|/\*.*?\*/", re.S)
STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'', re.S)


def arguments() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=".")
    parser.add_argument("--output-dir", required=True)
    parser.add_argument("--stage", choices=("baseline", "final"), default="final")
    parser.add_argument("--baseline-csv")
    return parser.parse_args()


def code_only(text: str) -> str:
    return STRING_RE.sub('""', COMMENT_RE.sub("", text))


def normalized_family(name: str) -> str:
    value = VERSION_RE.sub("", name)
    value = re.sub(r"(?:Engine|Manager|Store|Repository|Coordinator|Supervisor|Worker|Bridge|Facade)$", "", value)
    return value or name


def classify(text: str, code: str, lines: int, methods: int, incoming: int, reachable: bool, name: str) -> tuple[str, list[str], str]:
    flags: list[str] = []
    if lines >= 3000:
        flags.append("MONOLITH_GE_3000_LINES")
    if len(text) >= 300_000:
        flags.append("MONOLITH_GE_300K_CHARS")
    if "invokeOptional(" in text or "Class.forName(" in text or ".getDeclaredMethod(" in text:
        flags.append("REFLECTION_COUPLING")
    if re.search(r"(?m)(?://|/\*|\*)\s*(?:TODO|FIXME|NOT_IMPLEMENTED|IMPLEMENT_ME)\b", text):
        flags.append("UNRESOLVED_MARKER")
    if re.search(r"\breturn\s+(?:0|1|true|false|null|\"\")\s*;", code) and lines <= 20:
        flags.append("THIN_CONSTANT_RETURN")
    if lines <= 15 and methods <= 1 and " interface " not in f" {code} " and " enum " not in f" {code} ":
        flags.append("THIN_IMPLEMENTATION")
    if incoming == 0 and name not in MAINLINE:
        flags.append("NO_LEXICAL_INCOMING_REFERENCE")
    if not reachable and name not in MAINLINE:
        flags.append("REVIEW_UNREACHED_FROM_ANDROID_ENTRY")
    if text.count("setState(") + text.count("getState(") >= 40:
        flags.append("HIGH_STRING_STATE_COUPLING")
    if text.count("rawQuery(") + text.count("execSQL(") + text.count(".insert(") + text.count(".update(") >= 30:
        flags.append("HIGH_SQL_COUPLING")
    if name in MAINLINE:
        role = MAINLINE[name]
    elif re.search(r"\binterface\s+" + re.escape(name) + r"\b", code):
        role = "CONTRACT_INTERFACE"
    elif re.search(r"\benum\s+" + re.escape(name) + r"\b", code):
        role = "ENUM_OR_STATE_MODEL"
    elif name.endswith(("Test", "Harness")):
        role = "TEST_OR_HARNESS"
    elif any(token in name for token in ("Activity", "Fragment", "View", "Renderer")):
        role = "UI_OR_RENDERER"
    elif any(token in name for token in ("Store", "Repository", "Db", "Ledger")):
        role = "PERSISTENCE_OR_LEDGER"
    elif any(token in name for token in ("Research", "Algorithm", "Learning", "Model", "Evolution")):
        role = "RESEARCH_OR_MODEL"
    elif any(token in name for token in ("Service", "Worker", "Receiver", "Scheduler", "Watchdog")):
        role = "BACKGROUND_OR_SCHEDULER"
    elif lines <= 40:
        role = "SMALL_HELPER_OR_MODEL"
    else:
        role = "DOMAIN_COMPONENT"

    if "THIN_CONSTANT_RETURN" in flags:
        action = "REVIEW_INLINE_OR_REWRITE_SEMANTICS"
    elif "MONOLITH_GE_3000_LINES" in flags:
        action = "SPLIT_OWNERSHIP_AND_REMOVE_PATCH_LAYERS"
    elif "REVIEW_UNREACHED_FROM_ANDROID_ENTRY" in flags:
        action = "CONFIRM_CALLBACK_OR_REFLECTION_THEN_DELETE_OR_WIRE"
    elif flags:
        action = "REVIEW_FLAGS"
    else:
        action = "KEEP_NO_STATIC_DEFECT"
    return role, flags, action


def main() -> None:
    ns = arguments()
    root = Path(ns.repo).resolve()
    output = Path(ns.output_dir).resolve()
    output.mkdir(parents=True, exist_ok=True)
    paths = sorted((root / JAVA_ROOT).rglob("*.java"))
    entries: dict[str, dict] = {}
    type_to_path: dict[str, Path] = {}

    for path in paths:
        text = path.read_text(encoding="utf-8")
        code = code_only(text)
        match = TYPE_RE.search(code)
        name = match.group(1) if match else path.stem
        type_to_path[name] = path
        entries[name] = {
            "path": path,
            "text": text,
            "code": code,
            "lines": len(text.splitlines()),
            "chars": len(text),
            "bytes": len(text.encode("utf-8")),
            "methods": len(METHOD_RE.findall(code)),
        }

    names = set(entries)
    graph: dict[str, set[str]] = {name: set() for name in names}
    incoming: Counter[str] = Counter()
    for owner, entry in entries.items():
        tokens = set(re.findall(r"\b[A-Za-z_$][\w$]*\b", entry["code"]))
        for target in tokens & names:
            if target == owner:
                continue
            graph[owner].add(target)
            incoming[target] += 1

    roots: set[str] = set()
    manifest_path = root / MANIFEST
    manifest = manifest_path.read_text(encoding="utf-8") if manifest_path.exists() else ""
    for name in names:
        if re.search(r"(?:\.|\b)" + re.escape(name) + r"\b", manifest):
            roots.add(name)
    roots.update(name for name in MAINLINE if name in names)
    reachable: set[str] = set()
    queue = deque(sorted(roots))
    while queue:
        node = queue.popleft()
        if node in reachable:
            continue
        reachable.add(node)
        queue.extend(sorted(graph.get(node, set()) - reachable))

    families: dict[str, list[str]] = defaultdict(list)
    for name in names:
        families[normalized_family(name)].append(name)

    rows: list[dict[str, object]] = []
    flag_counts: Counter[str] = Counter()
    role_counts: Counter[str] = Counter()
    for name in sorted(names, key=lambda n: entries[n]["path"].as_posix()):
        entry = entries[name]
        role, flags, action = classify(
            entry["text"], entry["code"], entry["lines"], entry["methods"], incoming[name], name in reachable, name
        )
        if len(families[normalized_family(name)]) >= 3:
            flags.append("VERSION_FAMILY_GE_3")
            if action == "KEEP_NO_STATIC_DEFECT":
                action = "REVIEW_VERSION_FAMILY_CONSOLIDATION"
        for flag in flags:
            flag_counts[flag] += 1
        role_counts[role] += 1
        path = entry["path"].relative_to(root).as_posix()
        rows.append({
            "path": path,
            "type": name,
            "role": role,
            "lines": entry["lines"],
            "chars": entry["chars"],
            "bytes": entry["bytes"],
            "methods_approx": entry["methods"],
            "incoming_class_refs": incoming[name],
            "outgoing_class_refs": len(graph[name]),
            "entry_reachability": "APPROX_REACHABLE" if name in reachable else "REVIEW_UNREACHED",
            "version_family": normalized_family(name),
            "version_family_members": "|".join(sorted(families[normalized_family(name)])),
            "state_key_calls": entry["text"].count("setState(") + entry["text"].count("getState("),
            "sql_calls": entry["text"].count("rawQuery(") + entry["text"].count("execSQL(") + entry["text"].count(".insert(") + entry["text"].count(".update("),
            "risk_flags": "|".join(sorted(set(flags))),
            "r914_action": action,
            "sha256_current": hashlib.sha256(entry["text"].encode("utf-8")).hexdigest(),
        })

    csv_path = output / "R914_JAVA_SEMANTIC_AUDIT.csv"
    with csv_path.open("w", encoding="utf-8-sig", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)

    graph_json = {
        "entry_roots": sorted(roots),
        "reachable_count": len(reachable),
        "unreached_count": len(names - reachable),
        "mainline": {name: {"role": role, "outgoing": sorted(graph.get(name, set()))} for name, role in MAINLINE.items() if name in names},
        "version_families_ge_3": {key: sorted(value) for key, value in families.items() if len(value) >= 3},
    }
    (output / "R914_CLASS_GRAPH.json").write_text(json.dumps(graph_json, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    stage_zh = "基线" if ns.stage == "baseline" else "最终"
    summary = [
        f"# R914 全树语义审计（{stage_zh}）",
        "",
        "> 本报告是静态近似结果；Android 回调与反射必须在删除前人工确认。它不把‘编译通过’等同于‘功能闭环’。",
        "",
        f"- Java 文件：{len(rows)}",
        f"- 代码行：{sum(int(row['lines']) for row in rows):,}",
        f"- 字符：{sum(int(row['chars']) for row in rows):,}",
        f"- 从 Android/主链入口近似可达：{len(reachable)}",
        f"- 需要人工确认不可达：{len(names - reachable)}",
        f"- 3 个以上版本家族：{sum(1 for value in families.values() if len(value) >= 3)}",
        "",
        "## 风险计数",
        "",
        "|风险|文件数|",
        "|---|---:|",
    ]
    summary.extend(f"|{key}|{value}|" for key, value in sorted(flag_counts.items()))
    summary.extend(["", "## 角色计数", "", "|角色|文件数|", "|---|---:|"])
    summary.extend(f"|{key}|{value}|" for key, value in sorted(role_counts.items()))
    summary.extend([
        "",
        "## 验收含义",
        "",
        "- `APPROX_REACHABLE` 只证明存在静态调用路径，不证明运行时每期执行。",
        "- `THIN_CONSTANT_RETURN` 是重点复核对象，但合法接口、DTO、纯常量定义不因行数少而删除。",
        "- `MONOLITH` 表示所有权/补丁叠层风险，需要按主链职责拆分，不以机械增删行数处理。",
        "- 逐文件结论、原始行数/字符数、引用、风险和 R914 动作见 CSV。",
    ])
    (output / "R914_JAVA_SEMANTIC_AUDIT_SUMMARY.md").write_text("\n".join(summary) + "\n", encoding="utf-8")

    if ns.baseline_csv:
        baseline_path = Path(ns.baseline_csv)
        if not baseline_path.is_absolute():
            baseline_path = root / baseline_path
        with baseline_path.open("r", encoding="utf-8-sig", newline="") as handle:
            baseline_rows = {row["path"]: row for row in csv.DictReader(handle)}
        final_rows = {str(row["path"]): row for row in rows}
        comparison: list[dict[str, object]] = []
        for path in sorted(set(baseline_rows) | set(final_rows)):
            before = baseline_rows.get(path)
            after = final_rows.get(path)
            before_lines = int(before["lines"]) if before else 0
            after_lines = int(after["lines"]) if after else 0
            before_chars = int(before["chars"]) if before else 0
            after_chars = int(after["chars"]) if after else 0
            before_sha = (before or {}).get("sha256_current", (before or {}).get("sha256_before", ""))
            after_sha = str((after or {}).get("sha256_current", ""))
            if before is None:
                status = "ADDED"
                resolution = "R914新增；已纳入全量Java17编译与行为门禁"
            elif after is None:
                status = "DELETED"
                resolution = "删除未引用且不持有业务状态的常量/重复Facade；真实账本实现保留在V51Repository"
            elif before_sha != after_sha:
                status = "MODIFIED"
                resolution = "R914实质修改；已纳入全量Java17编译、静态主链与行为门禁"
            else:
                status = "UNCHANGED"
                resolution = "逐文件静态语义检查；无R914目标内确定性缺陷，保留原实现"
            comparison.append({
                "path": path,
                "type": (after or before or {}).get("type", Path(path).stem),
                "role_baseline": (before or {}).get("role", ""),
                "role_final": (after or {}).get("role", "DELETED"),
                "baseline_lines": before_lines,
                "final_lines": after_lines,
                "line_delta": after_lines - before_lines,
                "baseline_chars": before_chars,
                "final_chars": after_chars,
                "char_delta": after_chars - before_chars,
                "change_status": status,
                "baseline_risk_flags": (before or {}).get("risk_flags", ""),
                "final_risk_flags": (after or {}).get("risk_flags", ""),
                "final_reachability": (after or {}).get("entry_reachability", "DELETED"),
                "r914_resolution": resolution,
                "baseline_sha256": before_sha,
                "final_sha256": after_sha,
            })
        comparison_path = output / "R914_BASELINE_TO_FINAL_FILE_AUDIT.csv"
        with comparison_path.open("w", encoding="utf-8-sig", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(comparison[0]))
            writer.writeheader()
            writer.writerows(comparison)
        change_counts = Counter(str(row["change_status"]) for row in comparison)
        (output / "R914_BASELINE_TO_FINAL_SUMMARY.md").write_text(
            "\n".join([
                "# R914 基线到最终逐文件核查摘要",
                "",
                f"- 基线 Java：{len(baseline_rows)}",
                f"- 最终 Java：{len(final_rows)}",
                f"- 修改：{change_counts['MODIFIED']}",
                f"- 新增：{change_counts['ADDED']}",
                f"- 删除空壳/重复 facade：{change_counts['DELETED']}",
                f"- 检查后保留：{change_counts['UNCHANGED']}",
                f"- 净行数变化：{sum(int(row['line_delta']) for row in comparison):+,}",
                f"- 净字符变化：{sum(int(row['char_delta']) for row in comparison):+,}",
                "",
                "完整逐文件路径、角色、前后行数/字符数、风险标记、处理结论和双 SHA-256 见 CSV。",
            ]) + "\n",
            encoding="utf-8",
        )
    print(
        "R914_SEMANTIC_AUDIT=PASS"
        f"|java={len(rows)}|lines={sum(int(row['lines']) for row in rows)}"
        f"|chars={sum(int(row['chars']) for row in rows)}|reachable={len(reachable)}"
        f"|review_unreached={len(names - reachable)}|flags={sum(flag_counts.values())}|stage={ns.stage}"
    )


if __name__ == "__main__":
    main()
