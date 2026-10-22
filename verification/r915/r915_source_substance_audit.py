#!/usr/bin/env python3
"""Deterministic per-file substance and exact change audit for the R915 delivery.

The audit deliberately does not equate file length with quality.  A narrow interface, immutable
contract, compatibility facade or fail-closed untrained artifact can be complete in a few lines;
an executable owner must contain real behavior.  Empty sources, unresolved TODO/FIXME markers,
missing Java types and unexplained short concrete owners fail the release.
"""

from __future__ import annotations

import csv
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys
from typing import Dict, Iterable, List, Sequence, Tuple


ROOT = Path(__file__).resolve().parents[2]
JAVA_ROOT = ROOT / "app/src/main/java/com/ethk3/novaonepro"
BASE = os.environ.get("R915_BASE_COMMIT", "fd1a850")
OUT = Path(os.environ.get("R915_SOURCE_AUDIT_DIR", ROOT / "build/r915-verification/source-audit"))
REPORT_PREFIX = os.environ.get("SOURCE_AUDIT_PREFIX", "R915").strip() or "R915"


def git(*args: str, check: bool = True) -> bytes:
    run = subprocess.run(
        ["git", *args], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False
    )
    if check and run.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed({run.returncode}):"
            f" {run.stderr.decode('utf-8', 'replace').strip()}"
        )
    return run.stdout


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def text_metrics(data: bytes) -> Tuple[int, int, int]:
    text = data.decode("utf-8", "replace")
    lines = len(text.splitlines())
    nonblank = sum(1 for line in text.splitlines() if line.strip())
    return lines, nonblank, len(text)


def strip_comments(text: str) -> str:
    no_blocks = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//[^\n]*", "", no_blocks)


def java_kind(text: str, lines: int, method_count: int) -> str:
    if re.search(r"\bpublic\s+@interface\b", text):
        return "ANNOTATION_CONTRACT"
    if re.search(r"\bpublic\s+interface\b", text):
        return "FOCUSED_INTERFACE"
    if re.search(r"\bpublic\s+(?:final\s+)?enum\b", text):
        return "ENUM_CONTRACT"
    if re.search(r"\bextends\s+(?:Runtime)?Exception\b", text):
        return "EXCEPTION_CONTRACT"
    if ("UNTRAINED" in text or re.search(r"\bPROVEN\s*=\s*false\b", text)) and lines <= 80:
        return "FAIL_CLOSED_ARTIFACT_CONTRACT"
    if ("Compatibility facade" in text or "compatibility facade" in text
            or "Compatibility alias" in text or "delegates" in text.lower()) and lines <= 100:
        return "COMPATIBILITY_FACADE"
    if re.search(r"\b(?:class|record)\s+\w*(?:Model|Models|Result|Snapshot|Envelope|Contract)\b", text) \
            and lines <= 160:
        return "DATA_OR_CONTRACT_TYPE"
    if lines <= 30 and method_count >= 1:
        return "FOCUSED_ADAPTER_OR_UTILITY"
    if lines >= 180 or method_count >= 8:
        return "SUBSTANTIVE_OWNER"
    return "FOCUSED_COMPONENT"


def java_row(path: Path) -> Dict[str, object]:
    data = path.read_bytes()
    text = data.decode("utf-8", "replace")
    clean = strip_comments(text)
    lines, nonblank, chars = text_metrics(data)
    code_chars = len(re.sub(r"\s+", "", clean))
    type_count = len(re.findall(r"\b(?:class|interface|enum|record|@interface)\s+\w+", clean))
    method_count = len(re.findall(
        r"\b(?:public|protected|private|static|final|synchronized|native|abstract|default)"
        r"(?:\s+(?:public|protected|private|static|final|synchronized|native|abstract|default))*"
        r"\s+[\w<>\[\],.?]+\s+\w+\s*\([^;{}]*\)\s*(?:throws\s+[^{]+)?[;{]",
        clean,
    ))
    sql_count = len(re.findall(r"\b(?:rawQuery|execSQL|insert|update|delete)\s*\(", clean))
    todo_count = len(re.findall(r"\b(?:TODO|FIXME|XXX)\b", clean))
    kind = java_kind(text, lines, method_count)
    reasons: List[str] = []
    status = "PASS"
    if code_chars < 24:
        status, reasons = "FAIL", ["EMPTY_OR_COMMENT_ONLY"]
    if type_count == 0:
        status, reasons = "FAIL", reasons + ["NO_JAVA_TYPE"]
    if todo_count:
        status, reasons = "FAIL", reasons + [f"UNRESOLVED_MARKERS={todo_count}"]
    if lines <= 12 and method_count == 0 and kind not in {
        "FOCUSED_INTERFACE", "ENUM_CONTRACT", "ANNOTATION_CONTRACT",
        "FAIL_CLOSED_ARTIFACT_CONTRACT", "EXCEPTION_CONTRACT",
    }:
        status, reasons = "FAIL", reasons + ["UNEXPLAINED_SHORT_CONCRETE_OWNER"]
    if status == "PASS":
        reasons.append(kind)
        if lines <= 30:
            reasons.append("NARROW_RESPONSIBILITY_NOT_PADDED")
        if sql_count:
            reasons.append(f"PERSISTENCE_OPS={sql_count}")
    return {
        "path": path.relative_to(ROOT).as_posix(),
        "status": status,
        "classification": kind,
        "lines": lines,
        "nonblank_lines": nonblank,
        "utf8_characters": chars,
        "code_characters": code_chars,
        "java_types": type_count,
        "executable_members": method_count,
        "persistence_ops": sql_count,
        "todo_fixme": todo_count,
        "sha256": sha256(data),
        "reason": ";".join(reasons),
    }


def changed_paths() -> List[str]:
    tracked = git("diff", "--name-only", BASE, "--").decode("utf-8", "replace").splitlines()
    untracked = git("ls-files", "--others", "--exclude-standard").decode(
        "utf-8", "replace"
    ).splitlines()
    return sorted({path for path in tracked + untracked if path})


def base_bytes(path: str) -> bytes | None:
    out = subprocess.run(
        ["git", "show", f"{BASE}:{path}"],
        cwd=ROOT,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    return out.stdout if out.returncode == 0 else None


def numstat() -> Dict[str, Tuple[str, str]]:
    rows: Dict[str, Tuple[str, str]] = {}
    raw = git("diff", "--numstat", BASE, "--").decode("utf-8", "replace")
    for line in raw.splitlines():
        parts = line.split("\t", 2)
        if len(parts) == 3:
            rows[parts[2]] = (parts[0], parts[1])
    return rows


def change_row(path: str, stats: Dict[str, Tuple[str, str]]) -> Dict[str, object]:
    current_path = ROOT / path
    current = current_path.read_bytes() if current_path.is_file() else b""
    prior = base_bytes(path)
    current_lines, current_nonblank, current_chars = text_metrics(current) if current else (0, 0, 0)
    base_lines, _, base_chars = text_metrics(prior) if prior is not None else (0, 0, 0)
    if path in stats:
        added, deleted = stats[path]
    elif prior is None:
        added, deleted = str(current_lines), "0"
    else:
        added, deleted = "0", "0"
    if prior is None and current:
        kind = "ADDED"
    elif not current:
        kind = "DELETED"
    else:
        kind = "MODIFIED"
    return {
        "path": path,
        "change_kind": kind,
        "base_lines": base_lines,
        "current_lines": current_lines,
        "git_insertions": added,
        "git_deletions": deleted,
        "base_utf8_characters": base_chars,
        "current_utf8_characters": current_chars,
        "character_delta": current_chars - base_chars,
        "current_nonblank_lines": current_nonblank,
        "current_bytes": len(current),
        "sha256": sha256(current) if current else "DELETED",
    }


def legacy_source_rows() -> Tuple[List[Dict[str, object]], List[str]]:
    manifest_path = JAVA_ROOT / "LegacySourceLockManifestR915.java"
    manifest = manifest_path.read_text(encoding="utf-8")
    locks = dict(re.findall(r'lock\(out,"([^"]+)","([0-9a-f]{64})"\)', manifest))
    if len(locks) != 133:
        return [], [f"LEGACY_LOCK_COUNT={len(locks)}"]
    allowed_control_owner_changes = {
        "DecisionGateEngine": "唯一DecisionGate受控REAL候选授权修复",
        "SettlementEngine": "唯一Settlement原子OOS/REAL/任务终态修复",
    }
    rows: List[Dict[str, object]] = []
    failures: List[str] = []
    for owner in sorted(locks):
        relative = f"app/src/main/java/com/ethk3/novaonepro/{owner}.java"
        current_path = ROOT / relative
        current = current_path.read_bytes() if current_path.is_file() else b""
        prior = base_bytes(relative)
        current_hash = sha256(current) if current else "MISSING"
        base_hash = sha256(prior) if prior is not None else "MISSING_IN_BASE"
        delivery_match = current_hash == locks[owner]
        changed = prior is None or current != prior
        if not delivery_match:
            status = "FAIL_DELIVERY_LOCK_MISMATCH"
            failures.append(owner + ":DELIVERY_LOCK_MISMATCH")
        elif changed and owner not in allowed_control_owner_changes:
            status = "FAIL_UNDECLARED_BASELINE_CHANGE"
            failures.append(owner + ":UNDECLARED_BASELINE_CHANGE")
        elif changed:
            status = "INTENTIONAL_CONTROL_OWNER_CHANGE_RELOCKED"
        else:
            status = "UNCHANGED_FROM_BASE"
        rows.append({
            "owner": owner,
            "path": relative,
            "baseline_sha256": base_hash,
            "delivery_sha256": current_hash,
            "manifest_sha256": locks[owner],
            "status": status,
            "decision": allowed_control_owner_changes.get(owner, "Legacy/critical owner byte-identical to base"),
        })
    return rows, failures


def write_csv(path: Path, rows: Sequence[Dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        raise RuntimeError(f"no rows for {path}")
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    java_files = sorted(JAVA_ROOT.glob("*.java"))
    java_rows = [java_row(path) for path in java_files]
    failures = [row for row in java_rows if row["status"] != "PASS"]
    stats = numstat()
    changes = [change_row(path, stats) for path in changed_paths()]
    legacy_rows, legacy_failures = legacy_source_rows()
    write_csv(OUT / f"{REPORT_PREFIX}_JAVA_FILE_SUBSTANCE_AUDIT.csv", java_rows)
    write_csv(OUT / f"{REPORT_PREFIX}_ALL_CHANGED_FILE_METRICS.csv", changes)
    write_csv(OUT / f"{REPORT_PREFIX}_LEGACY_SOURCE_BASELINE_COMPARISON.csv", legacy_rows)

    classifications: Dict[str, int] = {}
    for row in java_rows:
        key = str(row["classification"])
        classifications[key] = classifications.get(key, 0) + 1
    changed_java = sum(1 for row in changes if str(row["path"]).endswith(".java"))
    total_lines = sum(int(row["lines"]) for row in java_rows)
    total_chars = sum(int(row["utf8_characters"]) for row in java_rows)
    control_changes = sum(1 for row in legacy_rows if row["status"] == "INTENTIONAL_CONTROL_OWNER_CHANGE_RELOCKED")
    unchanged_legacy = sum(1 for row in legacy_rows if row["status"] == "UNCHANGED_FROM_BASE")
    failures.extend({"path": failure, "reason": failure} for failure in legacy_failures)
    summary = [
        f"# {REPORT_PREFIX} 逐文件源码实质性审计",
        "",
        f"- 基线提交：`{BASE}`",
        f"- Java 文件：{len(java_rows)}",
        f"- Java 总行数：{total_lines}",
        f"- Java UTF-8 字符：{total_chars}",
        f"- 本版本变更文件：{len(changes)}（Java {changed_java}）",
        f"- 133 个锁定 Owner 与基线相同：{unchanged_legacy}",
        f"- 明示重锁的唯一控制 Owner：{control_changes}（DecisionGateEngine、SettlementEngine）",
        f"- 发布失败项：{len(failures)}",
        "",
        "## 分类统计",
        "",
        "| 分类 | 文件数 |",
        "| --- | ---: |",
    ]
    for key in sorted(classifications):
        summary.append(f"| {key} | {classifications[key]} |")
    summary.extend([
        "",
        "短文件不会因为行数少而自动失败：接口、契约、兼容门面、只读适配器和显式未训练工件按职责审计；",
        "具体 Owner 必须有真实可执行成员。空文件、无 Java 类型、TODO/FIXME、无法解释的短具体 Owner 均为 FAIL。",
        "",
        f"结论：{'PASS' if not failures else 'FAIL'}",
        "",
    ])
    if failures:
        summary.append("## 失败项")
        summary.append("")
        for row in failures:
            summary.append(f"- `{row['path']}`：{row['reason']}")
    (OUT / f"{REPORT_PREFIX}_SOURCE_SUBSTANCE_SUMMARY.md").write_text(
        "\n".join(summary), encoding="utf-8"
    )
    print(
        f"{REPORT_PREFIX}_SOURCE_SUBSTANCE_AUDIT=" + ("PASS" if not failures else "FAIL")
        + f"|JAVA={len(java_rows)}|LINES={total_lines}|CHARS={total_chars}"
        + f"|CHANGED={len(changes)}|CHANGED_JAVA={changed_java}|FAIL={len(failures)}"
    )
    return 0 if not failures else 1


if __name__ == "__main__":
    sys.exit(main())
