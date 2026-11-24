"""Check the overlap of a verified continuous service run and its three-generation trace."""
import hashlib
import json
from pathlib import PurePosixPath
from continuity4 import IDENTITY, need, text

TIME_FIELD = {"train":"committed_at", "publish":"published_at", "freeze":"returned_at",
              "settle":"settled_at", "feedback":"committed_at", "research":"completed_at",
              "mutation":"committed_at", "fusion":"committed_at"}
CANDIDATE_OBSERVATIONS = {"freeze", "settle", "feedback"}
FIELDS = {"freeze":("policy_hash", "target_ordinal", "role", "position", "numbers", "model_hash", "weights_sha256"),
          "settle":("policy_hash", "target_ordinal", "freeze_ref", "draw", "hit"),
          "feedback":("policy_hash", "target_ordinal", "settlement_ref", "event_key", "committed_count")}


def canonical_hash(obj):
    return hashlib.sha256(json.dumps(obj, ensure_ascii=False, sort_keys=True, allow_nan=False,
                                     separators=(",", ":")).encode()).hexdigest()


def check_joined_lineage(run, receipt, originals):
    """Call after both existing receipt verifiers. All new nested references are declared artifacts."""
    ci, li = run["continuity_index_artifact"], run["learning_trace_index_artifact"]
    stream, trace = originals.json(ci), originals.json(li)
    need(all(stream[k] == trace[k] for k in IDENTITY) and stream["source_commit"] == receipt["source_after"],
         "lineage was not observed in the same service run and clock domain")

    def record(index_name, ref):
        rel = ref["path"]
        path = PurePosixPath(rel)
        need(not path.is_absolute() and path.as_posix() == rel and ".." not in path.parts and "\\" not in rel,
             "invalid relative lineage original")
        name = (PurePosixPath(index_name).parent / path).as_posix()
        need(name in originals.files and originals.hashes[name] == ref["sha256"], "undeclared linkage original: " + name)
        return originals.json(name)

    spec, ledger, restart = [record(ci, stream[k]) for k in ("scenario", "ledger", "restart")]
    need(run.get("id") == stream.get("case_id") == trace.get("case_id") == ledger.get("case_id") == "A-B12-03b",
         "foreign engineering test case")
    db = ledger["database_instance"]
    need(text(db) and trace["database_instance"] == db, "different service databases")
    planned = spec["lineage_target_ordinals"]
    need(isinstance(planned, list) and len(planned) == len(trace["cycles"]) and len(planned) >= 3
         and all(type(n) is int for n in planned) and planned == sorted(set(planned)),
         "missing registered ordered lineage targets")
    slots = {s["ordinal"]:s for s in spec["slots"]}
    opened = {s["ordinal"]:(spec["slots"][i-1]["close_ms"] if i else spec["start_ms"])
              for i, s in enumerate(spec["slots"])}
    need(all(n in slots for n in planned) and planned[0] <= restart["after_ordinal"] < planned[-1],
         "registered generations do not span the service restart")
    by_record, used, durable = {}, set(), set()
    # A candidate freeze is allowed to differ from the service's basic R35 forecast.
    for i, row in enumerate(ledger["rows"]):
        start = spec["slots"][i-1]["close_ms"] if i else spec["start_ms"]
        if row["process"] == restart["new_process"]:
            start = max(start, restart["resumed_ms"])
        refs = [(evt["original"], False) for evt in row["events"] if evt["kind"] == "COMMIT"]
        refs.extend((ref, True) for ref in row.get("lineage_candidate_rows", []))
        for ref, separate in refs:
            doc = record(ci, ref)
            receipt_key = (doc["stage"], doc["source_table"], doc["durable_key"])
            need(receipt_key not in durable, "candidate row aliases an already counted durable service commit")
            durable.add(receipt_key)
            key = doc.get("learning_original_sha256")
            if key is None and not separate:
                continue
            need(text(key) and key not in by_record and all(doc.get(k) == stream[k] for k in IDENTITY)
                 and doc.get("database_instance") == db and doc["process"] == row["process"]
                 and doc["scenario_sha256"] == stream["scenario"]["sha256"]
                 and type(doc["committed_ms"]) is int and start <= doc["committed_ms"] <= spec["slots"][i]["close_ms"]
                 and (not separate or doc["stage"] in CANDIDATE_OBSERVATIONS), "wrong, duplicated or out-of-window service row")
            by_record[key] = doc

    def link(stage, ref, ordinal, effect):
        data = record(li, ref)
        obs = by_record.get(ref["sha256"])
        need(obs is not None and obs["stage"] == stage and ref["sha256"] not in used,
             "missing service commit for " + stage + " at " + str(ordinal))
        used.add(ref["sha256"])
        origin = data["service_origin"]
        need(all(text(origin.get(k)) and origin[k] == obs[k] for k in ("database_instance", "process", "source_table", "durable_key"))
             and origin["work_issue"] == obs["issue"] and obs["committed_ms"] == data[TIME_FIELD[stage]]
             and obs["policy_hash"] == data.get("policy_hash", data.get("child_hash")),
             "lineage and durable service source diverge: " + stage)
        need(obs["target_ordinal"] == ordinal and obs["effect"]["sha256"] == effect,
             "different target or business payload: " + stage)
        if stage in CANDIDATE_OBSERVATIONS:
            need(obs["issue"] == slots[ordinal]["issue"], "candidate source issue differs from registered target")
            # Auxiliary candidate rows are not part of continuity4.members. Bind their payload bytes here too.
            payload = obs["effect"]
            path = PurePosixPath(payload["path"])
            need(not path.is_absolute() and ".." not in path.parts and path.as_posix() == payload["path"]
                 and "\\" not in payload["path"], "unsafe candidate payload")
            name = (PurePosixPath(ci).parent / path).as_posix()
            need(originals.hashes.get(name) == effect and name in originals.files,
                 "candidate business payload absent from receipt")
        return data

    for i, (cycle, ordinal) in enumerate(zip(trace["cycles"], planned)):
        docs = {s:record(li, ref) for s, ref in cycle.items() if s in TIME_FIELD}
        cut = slots[ordinal]
        need(docs["freeze"]["target_ordinal"] == ordinal and docs["settle"]["draw"] == cut["draw_numbers"]
             and docs["settle"]["draw_at"] == cut["draw_ms"] and docs["freeze"]["deadline_at"] <= cut["draw_ms"],
             "wrong service draw or generation target")
        before = ordinal <= restart["after_ordinal"]
        expected_process = restart["old_process"] if before else restart["new_process"]
        first_allowed = opened[ordinal] if before else max(opened[ordinal], restart["resumed_ms"])
        need(docs["freeze"]["service_origin"]["process"] == expected_process and
             docs["freeze"]["returned_at"] >= first_allowed,
             "candidate freeze outside its registered opportunity or service process")
        state = record(li, docs["train"]["state_original"])
        link("train", cycle["train"], ordinal, state["weights_original"]["sha256"])
        link("publish", cycle["publish"], ordinal, docs["publish"]["weights_sha256"])
        for stage in ("freeze", "settle", "feedback"):
            payload = {k:docs[stage][k] for k in FIELDS[stage]}
            link(stage, cycle[stage], ordinal, canonical_hash(payload))
        link("research", cycle["research"], ordinal, docs["research"]["artifact_hash"])
        if i < len(trace["births"]):
            ref = trace["births"][i]
            born = record(li, ref)
            formula = record(li, born["child_original"])
            link(born["operation"].lower(), ref, ordinal, canonical_hash(formula["program"]))
    need(used == set(by_record), "unassigned lineage observations")
    return {"status":"PASS_SERVICE_LINEAGE_JOIN", "generations":len(planned), "linked_commits":len(used),
            "scope":"ENGINEERING_FIXTURE"}
