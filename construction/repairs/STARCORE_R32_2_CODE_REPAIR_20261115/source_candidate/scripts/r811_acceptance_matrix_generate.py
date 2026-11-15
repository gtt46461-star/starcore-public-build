#!/usr/bin/env python3
"""Build the truthful T01-T210 matrix from the locked plan and current evidence classes."""

import argparse
import csv
import re
from pathlib import Path


PLAN_FILES = (
    "24_T01_T80_AND_DEVICE_ACCEPTANCE_HARD_GATES.md",
    "46_T81_T120_AUTONOMOUS_RESEARCH_ACCEPTANCE_MATRIX.md",
    "61_T121_T160_FINAL_AUDIT_ACCEPTANCE_MATRIX.md",
    "71_T161_T180_DIRECTION_CORRECTION_GATES.md",
    "83_T181_T210_FINAL_STRATEGY_RESEARCH_GATES.md",
)

HOST = {
    81, 82, 83, 84, 85, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97,
    98, 99, 100, 101, 102, 103, 104, 105, 106, 108, 109, 110, 111, 112,
    114, 115, 116, 121, 132, 138, 139, 140, 141, 151, 154, 155, 156,
    157, 158, 159, 161, 166, 167, 171, 173, 174, 175, 176, 177, 179,
    184, 185, 186, 188, 189, 190, 191, 192, 194, 195, 196, 197, 200,
    201, 202, 204, 205, 206, 207, 208,
}
STATIC = {
    86, 107, 118, 123, 124, 134, 150, 163, 168, 180, 181, 193, 198,
    199, 203, 210,
}
BYTE_EXACT = {122, 164, 165, 172}
STRATEGY_PARITY = {169, 170, 183, 209}
BUILD = {135}
OPTIONAL = {125, 126, 127, 128, 129, 130, 178}
DEVICE = {120, 131, 133, 136, 137, 147, 148, 149, 160, 162}
LIVE_DATA = {113, 119, 152, 153, 182}
SQLITE_FAULT = {117, 142, 143, 144, 145, 146, 187}


def parse_plan(plan_root: Path) -> dict[int, str]:
    descriptions: dict[int, str] = {}
    # A range summary such as ``T01-T120仍全部保留`` is not an individual
    # acceptance case. Keep it from overwriting a real per-gate description.
    pattern = re.compile(r"^\s*(?:-\s*)?T(\d{2,3})(?!-)\s*(.+?)\s*$")
    for filename in PLAN_FILES:
        path = plan_root / filename
        if not path.is_file():
            raise SystemExit(f"missing plan file: {path}")
        for line in path.read_text(encoding="utf-8-sig").splitlines():
            match = pattern.match(line)
            if match:
                gate = int(match.group(1))
                if 1 <= gate <= 210:
                    descriptions[gate] = match.group(2).strip()
    return descriptions


def read_existing(path: Path) -> dict[int, dict[str, str]]:
    if not path.is_file():
        return {}
    rows: dict[int, dict[str, str]] = {}
    with path.open(newline="", encoding="utf-8-sig") as stream:
        for row in csv.DictReader(stream):
            match = re.fullmatch(r"T(\d{2,3})", row.get("gate", ""))
            if match:
                rows[int(match.group(1))] = row
    return rows


def classification(gate: int) -> tuple[str, str, str]:
    if gate in HOST:
        return (
            "PASS_HOST",
            "R811_HOST_CONTRACTS.log",
            "deterministic host fixture and official Android-36 full compile passed",
        )
    if gate in STATIC:
        evidence = (
            "R811_RESEARCH_REPORT_SCHEMA.log"
            if gate in {107, 118, 181, 210}
            else (
                "R811_PLAN_CONTRADICTION_GATE.log"
                if gate == 180
                else "R811_OFFICIAL_ANDROID36_JAVAC.log"
            )
        )
        return (
            "PASS_STATIC_CONTRACT",
            evidence,
            "source contract/static audit passed; no claim of future-live effectiveness",
        )
    if gate in BYTE_EXACT:
        return (
            "PASS_BYTE_EXACT",
            "R811_FILE_DISPOSITION_SUMMARY.txt",
            "baseline frozen owner/file remained byte exact",
        )
    if gate in STRATEGY_PARITY:
        return (
            "PASS_STRATEGY_PARITY",
            "R811_STRATEGY_PARITY_SUMMARY.txt",
            "100 cases x 11 strategy fields exact byte comparison passed",
        )
    if gate in BUILD:
        return (
            "PASS_BUILD_ARTIFACT",
            "R811_RELEASE_BUILD_SUMMARY.txt",
            "signed APK build metadata distinguishes compileSdk36 and targetSdk28",
        )
    if gate in OPTIONAL:
        return (
            "POST_CLOSURE_NOT_APPLICABLE",
            "R811_IMPLEMENTATION_REPORT.md",
            "locked plan classifies action archive/deletion as P2 and disables it in R811",
        )
    if gate in DEVICE:
        suffix = "_24H" if gate in {120, 160} else ""
        return (
            "NOT_RUN_NO_DEVICE" + suffix,
            "",
            "requires Android device/runtime or soak evidence and is not fabricated",
        )
    if gate in LIVE_DATA:
        return (
            "PARTIAL_CODE_PROVEN_LIVE_DATA_PENDING",
            "R811_OFFICIAL_ANDROID36_JAVAC.log",
            "implementation is present; future strict OOS/live evidence is still required",
        )
    if gate in SQLITE_FAULT:
        return (
            "PARTIAL_CODE_PROVEN_SQLITE_FAULT_PENDING",
            "R811_OFFICIAL_ANDROID36_JAVAC.log",
            "persistent fail-closed path compiles; real SQLite/fault-injection proof remains",
        )
    return (
        "PARTIAL_CODE_PROVEN_EXTERNAL_EVIDENCE_PENDING",
        "R811_OFFICIAL_ANDROID36_JAVAC.log",
        "compiled implementation exists but the exact external acceptance fixture was not run",
    )


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--plan-root", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    descriptions = parse_plan(args.plan_root)
    existing = read_existing(args.output)
    rows: list[dict[str, str]] = []
    for gate in range(1, 211):
        if gate <= 80 and gate in existing:
            row = dict(existing[gate])
            if gate in descriptions:
                row["plan_requirement"] = descriptions[gate]
            else:
                row["plan_requirement"] = row.get("note", "")
        else:
            status, evidence, note = classification(gate)
            row = {
                "gate": f"T{gate:02d}",
                "category": "locked-plan-final-audit",
                "status": status,
                "evidence": evidence,
                "note": note,
                "plan_requirement": descriptions.get(gate, ""),
            }
        row.setdefault("plan_requirement", descriptions.get(gate, ""))
        rows.append(row)

    missing = [row["gate"] for row in rows if not row.get("plan_requirement", "").strip()]
    if missing:
        raise SystemExit("missing gate descriptions: " + ",".join(missing))
    args.output.parent.mkdir(parents=True, exist_ok=True)
    fields = ("gate", "category", "status", "evidence", "note", "plan_requirement")
    with args.output.open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)
    print(f"R811_ACCEPTANCE_MATRIX=GENERATED|ROWS={len(rows)}|OUTPUT={args.output}")


if __name__ == "__main__":
    main()
