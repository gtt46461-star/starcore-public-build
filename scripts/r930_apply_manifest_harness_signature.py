#!/usr/bin/env python3
"""Persist checked-exception contracts on all R930 SQLite acceptance harnesses exactly once."""
from __future__ import annotations

import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
OLD = "public static void main(String[] args){"
NEW = "public static void main(String[] args) throws Exception {"
TARGETS = (
    ("MANIFEST", ROOT / "verification/r930/Batch34ManifestHarnessR930.java"),
    ("DIAGNOSTIC", ROOT / "verification/r930/Batch34DiagnosticCardsHarnessR930.java"),
)


def patch(label: str, path: pathlib.Path) -> bool:
    if not path.is_file():
        raise SystemExit(f"R930_{label}_SIGNATURE_PATCH_FAIL|SOURCE_MISSING")
    text = path.read_text(encoding="utf-8")
    if NEW in text:
        print(f"R930_{label}_SIGNATURE_PATCH=ALREADY_APPLIED")
        return False
    count = text.count(OLD)
    if count != 1:
        raise SystemExit(f"R930_{label}_SIGNATURE_PATCH_FAIL|MATCH_COUNT={count}")
    path.write_text(text.replace(OLD, NEW, 1), encoding="utf-8")
    print(f"R930_{label}_SIGNATURE_PATCH=APPLIED|main_throws=Exception")
    return True


def main() -> int:
    changed = 0
    for label, path in TARGETS:
        if patch(label, path):
            changed += 1
    print(f"R930_SQLITE_HARNESS_SIGNATURES=PASS|targets={len(TARGETS)}|changed={changed}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
