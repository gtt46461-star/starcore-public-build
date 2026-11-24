"""Bind already validated runner assertions to individual field-report directions."""
import re


def require(condition, message):
    if not condition:
        raise ValueError(message)


def measurement_record(document, address):
    """Find the parent of a raw measured field, preserving JSON Pointer escaping."""
    require(isinstance(address, str) and address.startswith("/"), "Direction assertion lacks a JSON pointer")
    path = address[1:].split("/")
    require(all(not re.search(r"~(?![01])", s) for s in path), "Malformed direction measurement pointer")
    tokens = tuple(s.replace("~1", "/").replace("~0", "~") for s in path)
    for token in tokens[:-1]:
        if isinstance(document, list):
            require(re.fullmatch(r"0|[1-9][0-9]*", token) and int(token) < len(document),
                    "Missing direction measurement row")
            document = document[int(token)]
        else:
            require(isinstance(document, dict) and token in document, "Missing direction measurement parent")
            document = document[token]
    field = tokens[-1]
    metadata = {"direction_key", "contract_sha256", "polarity", "case_id", "source_commit"}
    require(isinstance(document, dict) and field and field in document and field not in metadata,
            "Direction evidence must point at a business measurement in a labeled record")
    return document, tokens[:-1]


def verify_direction_measurements(case, run, observations, originals):
    """Call after check_result has authenticated the runner, measured values and oracle."""
    planned = case.get("field3_directions", [])
    results = run.get("direction_results", [])
    require(isinstance(planned, list) and isinstance(results, list), "Direction contract/results must be lists")
    require(len(planned) == len(results), "Missing or surplus direction result")
    if not planned:
        return

    contracts = {}
    for contract in planned:
        key = contract["key"]
        digest = contract["contract_sha256"]
        require(isinstance(key, str) and key and key not in contracts and isinstance(digest, str) and
                re.fullmatch(r"[0-9a-f]{64}", digest), "Invalid or duplicate sealed direction")
        contracts[key] = digest

    indexed = {obs["metric"]: obs for obs in observations}
    require(len(indexed) == len(observations), "Duplicate measured metric")
    used_metrics, used_records, reported = set(), set(), set()
    for item in results:
        key = item["key"]
        require(key in contracts and key not in reported and item.get("contract_sha256") == contracts[key],
                "Wrong, duplicate or stale direction result: " + str(key))
        reported.add(key)
        for polarity in ("positive", "negative"):
            metric = item.get(polarity + "_metric")
            require(isinstance(metric, str) and metric in indexed and metric not in used_metrics,
                    "Missing or reused direction metric: " + str(metric))
            used_metrics.add(metric)
            obs = indexed[metric]
            name = obs["evidence_artifact"]
            record, row_path = measurement_record(originals.json(name), obs["json_pointer"])
            identity = (originals.hashes[name], row_path)
            require(identity not in used_records, "Reused direction observation row")
            used_records.add(identity)
            expected = {"direction_key": key, "contract_sha256": contracts[key], "polarity": polarity,
                        "case_id": case["id"], "source_commit": run["source_after"]}
            require(all(record.get(k) == v for k, v in expected.items()),
                    "Wrong direction, polarity, case, source or contract in " + name + obs["json_pointer"])
