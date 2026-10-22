#!/usr/bin/env python3
"""Generate exact second-batch source change evidence against the frozen batch1 handoff.

Line additions/deletions come from git --numstat. UTF-8 character additions/deletions use a
repeatable line-aligned Unicode edit decomposition over changed regions. Binary files retain exact
byte counts, Git blob ids and SHA-256 values while character metrics stay null.
"""
from __future__ import annotations

import csv
import difflib
import hashlib
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASE = os.environ.get("BATCH2_BASE", "570e615571698c850bf62ec188144b360b9400a3")
HEAD = os.environ.get("BATCH2_HEAD", "HEAD")
OUT = ROOT / "reports" / "r926" / "batch2"


def git_bytes(*args: str, check: bool = True) -> bytes:
    p = subprocess.run(["git", *args], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if check and p.returncode != 0:
        raise SystemExit("GIT_FAILED|" + " ".join(args) + "|" + p.stderr.decode("utf-8", "replace")[:500])
    return p.stdout


def object_bytes(rev: str, path: str) -> bytes | None:
    p = subprocess.run(["git", "show", f"{rev}:{path}"], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return p.stdout if p.returncode == 0 else None


def blob_sha(rev: str, path: str) -> str | None:
    p = subprocess.run(["git", "rev-parse", f"{rev}:{path}"], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL)
    return p.stdout.decode().strip() if p.returncode == 0 else None


def sha256(data: bytes | None) -> str | None:
    return hashlib.sha256(data).hexdigest() if data is not None else None


def utf8(data: bytes | None) -> str | None:
    if data is None:
        return ""
    try:
        return data.decode("utf-8")
    except UnicodeDecodeError:
        return None


def char_edit_counts(old: str, new: str) -> tuple[int, int]:
    from r927_change_ledger import characters
    return characters(old, new)


def changed_rows() -> list[tuple[str, str, str]]:
    raw = git_bytes("diff", "--numstat", "-z", "--no-renames", f"{BASE}..{HEAD}")
    rows = []
    for item in raw.split(b"\0"):
        if not item:
            continue
        parts = item.decode("utf-8", "surrogateescape").split("\t", 2)
        if len(parts) != 3:
            raise SystemExit("NUMSTAT_PARSE_FAILED|" + repr(parts))
        rows.append((parts[0], parts[1], parts[2]))
    return rows


def main() -> None:
    git_bytes("cat-file", "-e", f"{BASE}^{{commit}}")
    head_sha = git_bytes("rev-parse", HEAD).decode().strip()
    records = []
    totals = {
        "files_changed": 0,
        "lines_added": 0,
        "lines_deleted": 0,
        "utf8_text_files": 0,
        "binary_files": 0,
        "characters_added": 0,
        "characters_deleted": 0,
        "old_bytes": 0,
        "new_bytes": 0,
        "byte_delta": 0,
    }
    for add_s, del_s, path in changed_rows():
        old_b, new_b = object_bytes(BASE, path), object_bytes(HEAD, path)
        old_t, new_t = utf8(old_b), utf8(new_b)
        is_text = old_t is not None and new_t is not None
        char_added = char_deleted = old_chars = new_chars = None
        if is_text:
            old_chars, new_chars = len(old_t), len(new_t)
            char_added, char_deleted = char_edit_counts(old_t, new_t)
            totals["utf8_text_files"] += 1
            totals["characters_added"] += char_added
            totals["characters_deleted"] += char_deleted
        else:
            totals["binary_files"] += 1
        line_added = int(add_s) if add_s.isdigit() else None
        line_deleted = int(del_s) if del_s.isdigit() else None
        if line_added is not None:
            totals["lines_added"] += line_added
        if line_deleted is not None:
            totals["lines_deleted"] += line_deleted
        old_size, new_size = len(old_b) if old_b is not None else 0, len(new_b) if new_b is not None else 0
        totals["old_bytes"] += old_size
        totals["new_bytes"] += new_size
        records.append({
            "path": path,
            "status": "ADDED" if old_b is None else "DELETED" if new_b is None else "MODIFIED",
            "lines_added": line_added,
            "lines_deleted": line_deleted,
            "utf8_text": is_text,
            "old_characters": old_chars,
            "new_characters": new_chars,
            "characters_added": char_added,
            "characters_deleted": char_deleted,
            "character_delta": (new_chars - old_chars) if is_text else None,
            "old_bytes": old_size,
            "new_bytes": new_size,
            "byte_delta": new_size - old_size,
            "old_git_blob_sha": blob_sha(BASE, path),
            "new_git_blob_sha": blob_sha(HEAD, path),
            "old_sha256": sha256(old_b),
            "new_sha256": sha256(new_b),
        })
    totals["files_changed"] = len(records)
    totals["byte_delta"] = totals["new_bytes"] - totals["old_bytes"]
    payload = {
        "format": 1,
        "base_commit": BASE,
        "head_commit": head_sha,
        "character_count_method": "Line-aligned Unicode codepoint edit decomposition, SequenceMatcher autojunk=false",
        "binary_character_metrics": None,
        "totals": totals,
        "files": records,
    }
    OUT.mkdir(parents=True, exist_ok=True)
    json_path, csv_path = OUT / "BATCH2_CHANGE_STATS.json", OUT / "BATCH2_CHANGE_STATS.csv"
    json_path.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    fields = list(records[0].keys()) if records else ["path"]
    with csv_path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fields)
        writer.writeheader()
        writer.writerows(records)
    print(json.dumps({"status": "PASS", "json": str(json_path.relative_to(ROOT)), "csv": str(csv_path.relative_to(ROOT)), "totals": totals}, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()
