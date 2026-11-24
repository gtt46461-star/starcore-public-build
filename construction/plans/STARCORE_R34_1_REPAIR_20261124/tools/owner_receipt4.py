"""Bind inherited file obligations to measured callers and consumers at the actual source commit."""
import re
import subprocess
from pathlib import PurePosixPath

from direction_receipt import measurement_record, require, verify_direction_measurements


LINK_FIELDS = {"effective_path", "effective_blob_sha", "business_id", "producer", "consumer",
               "payload_artifact", "event_id", "input_digest", "output_digest"}


def nonblank(value):
    return isinstance(value, str) and bool(value.strip())


def endpoint(ref, originals):
    address = ref["json_pointer"]
    require(isinstance(address, str) and address.endswith("/event_id"), "Owner endpoint must select event_id")
    artifact = ref["artifact"]
    record, tokens = measurement_record(originals.json(artifact), address)
    require(nonblank(record["event_id"]), "Owner endpoint lacks its observed event identity")
    return record, (originals.hashes[artifact], tokens)


def check_owner_probes(case, run, observations, originals, repo):
    """Run after check_result has matched observations to original bytes and the tracked oracle."""
    contracts = case.get("inherited_path_probes", [])
    results = run.get("owner_probe_results", [])
    verify_direction_measurements({"id": case["id"], "field3_directions": contracts},
                                  {"source_after": run.get("source_after"), "direction_results": results},
                                  observations, originals)
    if not contracts:
        return

    by_key = {entry["key"]: entry for entry in contracts}
    by_metric = {obs["metric"]: obs for obs in observations}
    commit = run["source_after"]
    require(isinstance(commit, str) and re.fullmatch(r"[0-9a-f]{40}", commit), "Invalid owner source commit")
    direction_metrics = {item[label] for item in run.get("direction_results", [])
                         for label in ("positive_metric", "negative_metric")}
    previously_linked = set()

    for result in results:
        rows = {}
        for kind in ("positive", "negative"):
            metric = result[kind + "_metric"]
            require(metric not in direction_metrics, "Owner measurement reused as diagnosis measurement")
            obs = by_metric[metric]
            raw, loc = measurement_record(originals.json(obs["evidence_artifact"]), obs["json_pointer"])
            measured_field = obs["json_pointer"].rsplit("/", 1)[-1].replace("~1", "/").replace("~0", "~")
            require(measured_field not in LINK_FIELDS, "Owner assertion must measure a business outcome")
            rows[kind] = (raw, (originals.hashes[obs["evidence_artifact"]], loc))

        positive, measurement_location = rows["positive"]
        expected_path = by_key[result["key"]]["effective_path"]
        require(nonblank(expected_path) and not PurePosixPath(expected_path).is_absolute()
                and ".." not in PurePosixPath(expected_path).parts and "\\" not in expected_path
                and PurePosixPath(expected_path).as_posix() == expected_path, "Invalid effective owner path")
        object_name = commit + ":" + expected_path
        def git(*args):
            return subprocess.run(["git", "-C", str(repo), *args], check=True,
                                  stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True).stdout.strip()
        require(git("cat-file", "-t", object_name) == "blob", "Effective owner is not a tracked file")
        blob = git("rev-parse", object_name)
        require(positive.get("effective_path") == expected_path and positive.get("effective_blob_sha") == blob,
                "Effective owner differs from measured source blob")
        business_id = positive.get("business_id")
        require(nonblank(business_id), "Missing owner business identity")
        produced, producer_location = endpoint(positive["producer"], originals)
        consumed, consumer_location = endpoint(positive["consumer"], originals)
        require(len({producer_location, consumer_location, measurement_location, rows["negative"][1]}) == 4 and
                produced["event_id"] != consumed["event_id"], "Owner transfer reuses a row or event")
        pair = (producer_location, consumer_location)
        require(pair not in previously_linked, "Reused owner transfer")
        previously_linked.add(pair)
        for record in (produced, consumed):
            require(record.get("business_id") == business_id and record.get("case_id") == case["id"] and
                    record.get("source_commit") == commit, "Owner transfer changed business identity or source")
        require(any(record.get("effective_path") == expected_path and record.get("effective_blob_sha") == blob
                    for record in (produced, consumed)), "Owner not recorded at a transfer endpoint")
        payload = positive["payload_artifact"]
        require(payload in originals.hashes and originals.files[payload].stat().st_size > 0,
                "No original bytes for the transferred payload")
        digest = originals.hashes[payload]
        require(digest not in {producer_location[0], consumer_location[0], measurement_location[0], rows["negative"][1][0]}
                and produced.get("output_digest") == digest == consumed.get("input_digest"),
                "Produced and consumed bytes differ")
