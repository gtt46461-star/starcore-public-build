#!/usr/bin/env python3
"""Export a closed grammar research asset. Android independently checks syntax, hash and limits."""
import argparse
import hashlib
import json
from pathlib import Path

def portable(program, coverage, margin, dataset_sha, through_issue):
    payload = {"format": "SU1_PORTABLE_GRAMMAR_V1", "program": program,
               "coverageHex": float(coverage).hex(), "marginGate": bool(margin),
               "datasetSha256": dataset_sha, "trainedThroughIssue": str(through_issue),
               "numericProtocol": "SU1_ATOM_CONDITION_V2_1E12"}
    wire = json.dumps(payload, sort_keys=True, ensure_ascii=False, separators=(",", ":"))
    return {"payload": payload, "sha256": hashlib.sha256(wire.encode()).hexdigest()}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--program", required=True)
    p.add_argument("--history", required=True)
    p.add_argument("--trained-through-issue", required=True)
    p.add_argument("--coverage", type=float, default=1.0)
    p.add_argument("--margin-gate", action="store_true")
    p.add_argument("--out", required=True)
    a = p.parse_args()
    if not .10 <= a.coverage <= 1 or len(a.program) > 4096:
        p.error("coverage/program limits")
    result = portable(a.program, a.coverage, a.margin_gate,
                      hashlib.sha256(Path(a.history).read_bytes()).hexdigest(), a.trained_through_issue)
    Path(a.out).write_text(json.dumps(result, sort_keys=True, ensure_ascii=False, separators=(",", ":")) + "\n")
    print("EXPORTED_RESEARCH_ONLY", result["sha256"])

if __name__ == "__main__":
    main()
