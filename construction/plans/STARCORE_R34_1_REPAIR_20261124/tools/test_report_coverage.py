"""Check that specific corruptions in the report crosswalk are rejected."""
import argparse
from copy import deepcopy
import json
from pathlib import Path
import tempfile
from verify_report_coverage import verify


def probes(data):
    original_health = data["checks"][0]["health"]
    data["checks"][0]["health"] = "PASS"
    yield "promoted_health", "checks."
    data["checks"][0]["health"] = original_health

    support = next(x for x in data["metrics"] if x["id"] == "Q01")
    original = support["minimumSample"]
    support["minimumSample"] = 120
    yield "changed_quality_sample", "minimumSample"
    support["minimumSample"] = original

    row = next(x for x in data["metrics"] if x["value"] is None)
    row["value"] = 0
    yield "null_as_zero", ".value"
    row["value"] = None

    a, b = data["objects"][:2]
    a["occurrenceId"], b["occurrenceId"] = b["occurrenceId"], a["occurrenceId"]
    yield "crossed_occurrences", "occurrenceId"
    a["occurrenceId"], b["occurrenceId"] = b["occurrenceId"], a["occurrenceId"]

    data["followups"].append(deepcopy(data["followups"][0]))
    yield "duplicate_followup", "duplicate"
    data["followups"].pop()

    data["needs"][0]["work_orders"] = ["missing_work_order"]
    yield "orphan_requirement", "work order"


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--plan", type=Path, default=Path(__file__).resolve().parents[1])
    cli.add_argument("--input", type=Path, required=True)
    args = cli.parse_args()
    source = args.plan / "evidence/REPORT_COVERAGE.json"
    verify(args.plan, args.input, source)
    data = json.loads(source.read_bytes())
    passed = []
    with tempfile.TemporaryDirectory() as temp:
        candidate = Path(temp) / "mutated.json"
        for name, expected_reason in probes(data):
            candidate.write_text(json.dumps(data, ensure_ascii=False))
            try:
                verify(args.plan, args.input, candidate)
            except ValueError as exc:
                if expected_reason.lower() not in str(exc).lower():
                    raise
                passed.append(name)
            else:
                raise ValueError("Accepted changed report row: " + name)
    print(json.dumps({"status":"PASS_REPORT_CROSSWALK_REJECTIONS", "verified_rejections":passed}))


if __name__ == "__main__":
    main()
