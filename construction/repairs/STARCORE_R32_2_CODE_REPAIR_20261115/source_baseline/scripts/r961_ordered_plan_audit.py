#!/usr/bin/env python3
"""Bind every ordered-plan record to the current tree without inventing acceptance."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path


EQUIVALENTS = {
    "app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAudit.java": [
        "app/src/main/java/com/ethk3/novaonepro/RuntimeResourceAuditR927.java",
    ],
}


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--plan", type=Path, required=True)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    inventory = args.plan / "01_SCOPE" / "ORDERED_FILE_LIST.csv"
    with inventory.open(encoding="utf-8-sig", newline="") as stream:
        planned = list(csv.DictReader(stream))
    if len(planned) != 249:
        raise SystemExit(f"R961_PLAN_COUNT_DRIFT:{len(planned)}")

    rows = []
    counts = {"DIRECT": 0, "EQUIVALENT": 0, "OPEN_GAP": 0}
    for item in planned:
        requested = item["path"]
        direct = args.source / requested
        card = args.plan / item["card"]
        bound = []
        status = "OPEN_GAP"
        note = "PLANNED_PATH_ABSENT_AND_NO_VERIFIED_EQUIVALENT"
        if direct.is_file():
            bound = [requested]
            status = "DIRECT"
            note = "CURRENT_PATH_PRESENT"
        else:
            equivalents = EQUIVALENTS.get(requested, [])
            if equivalents and all((args.source / path).is_file() for path in equivalents):
                bound = equivalents
                status = "EQUIVALENT"
                note = "EXISTING_SINGLE_OWNER_REUSED_NO_DUPLICATE_CLASS"
        counts[status] += 1
        dependencies = [p for p in item.get("dependencies", "").split(";") if p]
        dependency_missing = [p for p in dependencies if not (args.source / p).exists() and p not in EQUIVALENTS]
        rows.append({
            "order": item["order"],
            "requested_path": requested,
            "phase": item["phase"],
            "planned_disposition": item["planned_disposition"],
            "requirements": item["requirements"],
            "binding_status": status,
            "bound_paths": ";".join(bound),
            "bound_sha256": ";".join(digest(args.source / path) for path in bound),
            "bytes": str(sum((args.source / path).stat().st_size for path in bound)),
            "card_present": "1" if card.is_file() else "0",
            "card_sha256": digest(card) if card.is_file() else "",
            "declared_dependencies": str(len(dependencies)),
            "unbound_dependencies": ";".join(dependency_missing),
            "inspection_note": note,
            "acceptance_scope": "SOURCE_BINDING_ONLY_NOT_DEVICE_OR_FUTURE_EFFECT",
        })

    output = args.out / "249_FILE_BINDING_AUDIT.csv"
    with output.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0]))
        writer.writeheader()
        writer.writerows(rows)
    summary = {
        "schema": "STARCORE_R961_ORDERED_PLAN_BINDING_V1",
        "orderedRecords": len(rows),
        "pathsChecked": len(rows),
        "pathBindingCoveragePercent": 100.0,
        "directCurrentPaths": counts["DIRECT"],
        "verifiedEquivalentPaths": counts["EQUIVALENT"],
        "unboundPathCount": counts["OPEN_GAP"],
        "functionalAcceptancePercent": None,
        "deviceAcceptance": "NOT_RUN",
        "futureEffectAcceptance": "NOT_RUN",
        "ledgerSha256": digest(output),
    }
    (args.out / "249_FILE_BINDING_SUMMARY.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(summary, ensure_ascii=False))


if __name__ == "__main__":
    main()
