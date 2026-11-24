"""Assign report directions to a runnable sign-off and verify the sealed assignment."""
from collections import defaultdict
import hashlib
import json


def read(path):
    return json.loads(path.read_bytes())


def direction_routes(directions, extended):
    rows = []
    def add(key, row, case):
        body = json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",",":"), allow_nan=False).encode()
        rows.append((case, {"key":key, "contract_sha256":hashlib.sha256(body).hexdigest()}))
    for group, key_name, records in (
        ("object", "key", directions["objects"]), ("need", "needKey", directions["needs"]),
        ("p50", "name", directions["storedP50"]["capabilities"]), ("probe", "name", directions["storedP50"]["familyProbeRows"])):
        for r in records:
            add(group + "/" + r[key_name], r, r["tests"][0])
    for r in directions["storedP50"]["findings"]:
        add("finding/" + r["source"]["code"], r, r["tests"][0])
    for name,r in directions["currentOperatingPrescriptions"].items():
        add("operating/" + name, r, r["tests"][0])
    # B11 verifies diagnostics against controlled observations after their business owners exist.
    # The original producer cases remain listed in the separate source-to-owner register.
    for kind, name, records in (("rule","id",extended["rules"]),("trigger","metricId",extended["metric_triggers"])):
        for r in records:
            add(kind + "/" + r[name], r, "A-B11-05b")
    if len(rows) != 258 or len({row["key"] for _,row in rows}) != len(rows):
        raise ValueError("Incomplete or duplicated report direction keys")
    return rows


def check_field3(root):
    identity = read(root / "evidence/FIELD3_IDENTITY.json")
    directions = read(root / "evidence/field3/directions.json")
    extended = read(root / "evidence/field3/extended.json")
    sources = read(root / "evidence/field3/runtime_delta.json")
    previous = read(root / "evidence/REPORT_COVERAGE.json")
    if not (directions["jobId"] == extended["job"] == identity["job_id"] and
            directions["sourceCommit"] == extended["source_commit"] == identity["source_commit"] and
            extended["snapshot_sha256"] == identity["snapshot_sha256"] and identity["job_id"] != previous["report_job"]):
        raise ValueError("Different report identities or reused snapshot")
    if sources["draw_targets"] != sum(sources["r35"].values()) or sources["draw_targets"] != 467:
        raise ValueError("New-draw coverage denominator differs")
    expected = defaultdict(list)
    for case, contract in direction_routes(directions, extended):
        expected[case].append(contract)
    matrix = read(root / "ACCEPTANCE_MATRIX.json")["cases"]
    actual = {c["id"]:c["field3_directions"] for c in matrix if c.get("field3_directions") and c["level"]=="ENGINEERING"}
    if dict(expected) != actual:
        raise ValueError("Direction obligations or receipt case assignments differ")
    return {"field3_report_members":identity["member_count"], "field3_direction_keys":sum(len(x) for x in expected.values()),
            "field3_direction_cases":len(expected), "field3_object_directions":len(directions["objects"])}
