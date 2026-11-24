"""Verify scheduled work across a real service restart in a controlled integration run."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

SCENARIO_PATH = "tools/r341/fixtures/continuity_b12.json"
STAGES = ("create", "train", "publish", "freeze", "settle", "feedback", "research", "mutation", "fusion")
PER_DRAW = {"freeze", "settle", "feedback", "train"}
DISTINCT = {"create", "train", "publish", "research", "mutation", "fusion"}
IDENTITY = ("scope", "run_id", "source_commit", "apk_sha256", "time_scope")


def need(condition, reason):
    if not condition:
        raise ValueError(reason)


def text(value):
    return isinstance(value, str) and bool(value.strip())


def digest(value, length=64):
    return isinstance(value, str) and re.fullmatch("[0-9a-f]{%d}" % length, value) is not None


def number(value):
    return type(value) is int and value >= 0


def members(index, directory):
    """Return checked references for the enclosing receipt to bind to its originals."""
    root = Path(directory).resolve()
    found = {}

    def read(ref, binary=False):
        name, expected = ref["path"], ref["sha256"]
        need(text(name) and name == Path(name).as_posix() and not Path(name).is_absolute()
             and ".." not in Path(name).parts and "\\" not in name and digest(expected), "unsafe original reference")
        path = root / name
        need(path.resolve() == path and path.is_file(), "missing original: " + name)
        raw = path.read_bytes()
        need(raw and hashlib.sha256(raw).hexdigest() == expected, "original digest mismatch: " + name)
        found[name] = expected
        return raw if binary else json.loads(raw)

    scenario, ledger, restart = (read(index[k]) for k in ("scenario", "ledger", "restart"))
    products = {}
    for row in ledger["rows"]:
        for event in row["events"]:
            ref = event["original"]
            if ref["sha256"] not in products:
                record = read(ref)
                read(record["effect"], binary=True)
                products[ref["sha256"]] = record
            else:
                read(ref)
        read(row["input_original"], binary=True)
        draw = read(row["draw_original"])
        need(draw["issue"] == row["issue"] and draw["ordinal"] == row["ordinal"]
             and type(draw["numbers"]) is list and all(type(x) is int for x in draw["numbers"])
             and sorted(draw["numbers"]) == list(range(1, 11)), "wrong PK10 draw original: " + row["issue"])
        need(all(draw[k] == index[k] for k in IDENTITY), "cross-run draw original")
        row["verified_draw"] = draw["numbers"]
    return found, scenario, ledger, restart, products


def referenced_files(index, directory):
    return members(index, directory)[0]


def verify(index, directory):
    need(index["scope"] == "ENGINEERING_FIXTURE" and text(index["run_id"]) and text(index["time_scope"])
         and digest(index["source_commit"], 40) and digest(index["apk_sha256"]), "unbound controlled run")
    _, spec, ledger, restart, records = members(index, directory)
    need(all(doc[k] == index[k] for k in IDENTITY for doc in (ledger, restart)), "scope mismatch")
    scenario_sha = index["scenario"]["sha256"]
    need(ledger["scenario_sha256"] == restart["scenario_sha256"] == scenario_sha
         and spec["kind"] == "CONTINUITY_CONTROLLED_STREAM_V1", "wrong registered scenario")
    rows, slots, boundary = ledger["rows"], spec["slots"], spec["restart_after"]
    need(len(rows) == len(slots) and number(boundary) and 0 <= boundary < len(rows)-1, "incomplete registered calendar")
    need(number(spec["start_ms"]) and number(spec["end_ms"]) and
         number(ledger["started_ms"]) and number(ledger["finished_ms"]) and
         ledger["started_ms"] <= spec["start_ms"] < spec["end_ms"] <= ledger["finished_ms"],
         "window not observed through planned end")
    need(ledger["activity_events"] == [] and ledger["lifecycle_listener_from_ms"] <= spec["start_ms"]
         and ledger["lifecycle_listener_through_ms"] >= spec["end_ms"],
         "unobserved lifecycle interval or Activity in service-only fixture")
    owners = [restart["old_process"], restart["new_process"]]
    need(all(text(p) for p in owners) and owners[0] != owners[1]
         and restart["after_ordinal"] == slots[boundary]["ordinal"], "restart identity or boundary")
    stopped, resumed = restart["stopped_ms"], restart["resumed_ms"]
    need(number(stopped) and number(resumed) and slots[boundary]["close_ms"] <= stopped < resumed
         < slots[boundary+1]["draw_ms"], "restart not between registered slots")
    for kind, process, stamp in (("PROCESS_DIED", owners[0], stopped), ("PROCESS_STARTED", owners[1], resumed)):
        observation = restart[kind]
        need(observation["kind"] == kind and observation["process_identity"] == process and
             number(observation["pid"]) and observation["pid"] > 0 and
             number(observation["process_start_ms"]) and observation["process_start_ms"] <= stamp and
             observation["at_ms"] == stamp and text(observation["record_key"]), "unbound OS process lifecycle: " + kind)
    need(restart["PROCESS_DIED"]["process_start_ms"] <= spec["start_ms"] and
         restart["PROCESS_STARTED"]["process_start_ms"] >= stopped, "service recreation without process restart")
    pending, completed, durable = {}, {}, set()
    ends = spec["cycle_ends"]
    need(isinstance(ends, list) and all(number(e) and e < len(slots) for e in ends) and
         ends == sorted(set(ends)) and ends and ends[-1] == len(slots)-1 and boundary in ends and
         sum(e <= boundary for e in ends) >= 2 and sum(e > boundary for e in ends) >= 2,
         "register at least two complete dispatch cycles on either side of restart")
    produced = {s:set() for s in STAGES}
    previous_effects = {s:set() for s in DISTINCT}
    on_restart, replayed, recovered = set(), False, False
    target_by_issue = {}
    draw_refs = {}
    last_close = spec["start_ms"]
    for i, (slot, row) in enumerate(zip(slots, rows)):
        epoch = int(i > boundary)
        issue, ordinal = slot["issue"], slot["ordinal"]
        need(text(issue) and issue not in target_by_issue and number(ordinal) and (not i or ordinal == slots[i-1]["ordinal"]+1)
             and (issue, ordinal) == (row["issue"], row["ordinal"]), "missing, reordered or duplicate calendar slot")
        target_by_issue[issue] = slot
        draw_refs[issue] = row["draw_original"]["sha256"]
        need(row["verified_draw"] == slot["draw_numbers"] and row["input_original"]["sha256"] == slot["input_sha256"],
             "controlled draw or pre-draw inputs differ from registered opportunity: " + issue)
        draw_ms, close_ms = slot["draw_ms"], slot["close_ms"]
        need(number(draw_ms) and number(close_ms) and last_close < draw_ms <= close_ms <= spec["end_ms"]
             and (i+1 < len(rows) or close_ms == spec["end_ms"]), "incorrect registered interval: " + issue)
        owner = owners[epoch]
        need(row["process"] == owner and row["origin"] == "SERVICE" and row["activity_open"] is False,
             "wrong process or UI-assisted run: " + issue)
        work = slot["work_due"]
        need(isinstance(work, dict) and PER_DRAW <= work.keys() <= set(STAGES), "unregistered work or omitted opportunity: " + issue)
        for stage, due in work.items():
            need(number(due) and i <= due < len(slots), "work due outside registered window")
            pending[(stage, issue)] = due
        when = max(last_close, resumed if epoch else last_close)
        for event in row["events"]:
            stage, target = event["stage"], event["issue"]
            key = (stage, target)
            ref = event["original"]["sha256"]
            record = records[ref]
            observed = event["observed_ms"]
            need(number(observed) and when <= observed <= close_ms, "out-of-order event or late observation")
            when = observed
            if event["kind"] == "REPLAY":
                need(completed.get(key) == ref, "replay changed a committed original: " + str(key))
                if epoch and record["process"] == owners[0]:
                    replayed = True
                continue
            need(event["kind"] == "COMMIT" and key in pending, "duplicate or unscheduled commit: " + str(key))
            need(all(record[k] == index[k] for k in IDENTITY) and
                 (record["stage"], record["issue"], record["process"]) == (stage, target, owner)
                 and record["scenario_sha256"] == scenario_sha, "wrong original for " + str(key))
            need(text(record["source_table"]) and text(record["durable_key"]) and
                 (stage, record["source_table"], record["durable_key"]) not in durable, "reused durable business key")
            committed = record["committed_ms"]
            need(number(committed) and max(last_close, resumed if epoch else last_close) <= committed <= observed,
                 "stale or future commit: " + str(key))
            target_slot = target_by_issue[target]
            need(record["opportunity_input_sha256"] == target_slot["input_sha256"], "wrong controlled input: " + target)
            if stage == "freeze":
                need(committed < target_slot["draw_ms"], "post-draw freeze: " + target)
            if stage in ("settle", "feedback"):
                need(committed >= target_slot["draw_ms"], "pre-draw outcome: " + target)
            if stage == "settle":
                need(record["draw_ref"] == draw_refs[target], "wrong settlement draw: " + target)
            if stage in ("settle", "feedback"):
                parent = "freeze" if stage == "settle" else "settle"
                need(completed.get((parent, target)) == record["parent_ref"] and digest(record["parent_ref"]),
                     "missing or crossed settlement/feedback source: " + target)
                need(committed >= records[record["parent_ref"]]["committed_ms"], "commit earlier than parent: " + target)
            durable.add((stage, record["source_table"], record["durable_key"]))
            completed[key] = ref
            del pending[key]
            produced[stage].add(record["effect"]["sha256"])
            recovered |= bool(epoch and key in on_restart)
        need(row["pending_after"] == sorted([list(k) for k in pending]), "backlog mismatch at " + issue)
        overdue = [k for k,due in pending.items() if due <= i]
        need(not overdue, "first unsettled work at registered deadline " + issue + ": " + str(overdue[:4]))
        if i == boundary:
            on_restart = set(pending)
            need(on_restart and restart["before_pending"] == restart["after_pending"] == row["pending_after"],
                 "restart lost work or carryover not exercised")
        if i in ends:
            for stage in STAGES:
                effects = produced[stage]
                need(effects, "no committed " + stage + " in cycle ending " + issue)
                if stage in DISTINCT:
                    need(effects - previous_effects[stage], "controlled repeat without new " + stage + " effect at " + issue)
                    previous_effects[stage].update(effects)
                effects.clear()
        last_close = close_ms
    need(not pending and replayed and recovered, "missing replay or resumed pending commit")
    return {"status":"PASS_ENGINEERING_CONTINUITY","periods":len(rows),"completed_work":len(completed),
            "carried_work":len(on_restart),"natural_quality":"NOT_EVALUATED"}


def check_continuity_receipt(run, receipt, originals, repo):
    name, linked = run.get("continuity_index_artifact"), run.get("learning_trace_index_artifact")
    need(text(name) and text(linked) and name in originals.files and linked in originals.files and name != linked,
         "missing declared continuity or learning index")
    index, learning = originals.json(name), originals.json(linked)
    source = receipt["source_after"]
    need(digest(source, 40) and index["source_commit"] == learning["source_commit"] == source,
         "wrong continuity source commit")
    apk = receipt.get("apk_artifact") or run.get("apk_artifact")
    need(apk in originals.hashes and index["apk_sha256"] == learning["apk_sha256"] == originals.hashes[apk],
         "continuity and learning indexes must match actual APK")
    declared = [obj.get("apk_sha256") for obj in (run, receipt) if obj.get("apk_sha256") is not None]
    need(all(value == index["apk_sha256"] for value in declared), "conflicting APK digest")
    need(index["scenario_source_path"] == SCENARIO_PATH, "unregistered controlled scenario source path")
    raw = subprocess.run(["git", "-C", str(repo), "cat-file", "blob", source + ":" + SCENARIO_PATH],
                         capture_output=True)
    need(raw.returncode == 0 and hashlib.sha256(raw.stdout).hexdigest() == index["scenario"]["sha256"],
         "registered scenario bytes absent or changed in tested source")
    directory = originals.files[name].parent
    prefix = Path(name).parent
    for ref, expected in referenced_files(index, directory).items():
        declared_path = (prefix / ref).as_posix()
        need(originals.hashes.get(declared_path) == expected, "undeclared continuity original: " + declared_path)
    return verify(index, directory)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--index", type=Path, required=True)
    parser.add_argument("--evidence-root", type=Path, required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(verify(json.loads(args.index.read_bytes()), args.evidence_root)))
    except (ValueError, OSError, KeyError, TypeError, IndexError) as error:
        parser.exit(1, "FAIL_CONTINUITY: %s\n" % error)
