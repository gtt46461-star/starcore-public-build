#!/usr/bin/env python3
"""Check execution evidence against sealed case contracts. Never execute receipt argv."""
import argparse
import hashlib
import json
import math
import re
import subprocess
import zipfile
from datetime import datetime, timedelta, timezone
from pathlib import Path
from verify_plan import verify_seal
from verify_learning_trace import verify as verify_learning_trace
from direction_receipt import verify_direction_measurements


def need(value, message):
    if not value:
        raise ValueError(message)


def unique_keys(pairs):
    obj = {}
    for key, value in pairs:
        need(key not in obj, "Duplicate JSON key: " + key)
        obj[key] = value
    return obj


def decode(data):
    return json.loads(data, object_pairs_hook=unique_keys,
                      parse_constant=lambda value: need(False, "Nonfinite number: " + value))


def read(path):
    return decode(path.read_text(encoding="utf-8"))


def sha(path):
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True).stdout.rstrip("\n")


def safe_name(name):
    need(isinstance(name, str) and name and not Path(name).is_absolute() and
         name == Path(name).as_posix() and ".." not in Path(name).parts and
         name != "." and "\\" not in name, "Noncanonical relative path")
    return name


class Originals:
    def __init__(self, root, entries):
        root = root.resolve()
        self.files = {}
        self.hashes = {}
        self.documents = {}
        for item in entries:
            name = safe_name(item["path"])
            path = root / name
            need(path.resolve() == path and path.is_file(), "Missing file or path alias: " + name)
            need(name not in self.files, "Duplicate artifact: " + name)
            need(re.fullmatch(r"[0-9a-f]{64}", item["sha256"]) and
                 sha(path) == item["sha256"], "Original digest mismatch: " + name)
            self.files[name] = path
            self.hashes[name] = item["sha256"]
        need(self.files, "No original artifacts")

    def json(self, name):
        need(name in self.files, "Missing JSON original: " + str(name))
        if name not in self.documents:
            self.documents[name] = read(self.files[name])
        return self.documents[name]


def exact(a, b):
    return json.dumps(a, sort_keys=True, allow_nan=False) == json.dumps(b, sort_keys=True, allow_nan=False)


def pointer(data, key):
    need(isinstance(key, str) and key.startswith("/"), "Expected a non-root JSON pointer")
    for part in key[1:].split("/"):
        need(not re.search(r"~(?![01])", part), "Invalid JSON pointer escape")
        token = part.replace("~1", "/").replace("~0", "~")
        if isinstance(data, list):
            need(re.fullmatch(r"0|[1-9][0-9]*", token) and int(token) < len(data), "Missing array element")
            data = data[int(token)]
        else:
            need(isinstance(data, dict) and token in data, "Missing observation field " + token)
            data = data[token]
    return data


def assert_measured(row):
    need(row.get("metric") and row.get("evidence_artifact"), "Measurement lacks name or original")
    actual, expected = row["actual"], row["expected"]
    op = row.get("operator", "eq")
    for value in (actual, expected):
        if type(value) is float:
            need(math.isfinite(value), "Nonfinite measurement")
    if op == "is_null":
        ok = actual is None and expected is None
    elif op == "eq":
        ok = actual is not None and exact(actual, expected)
    elif op == "in":
        ok = actual is not None and isinstance(expected, list) and any(exact(actual, item) for item in expected)
    else:
        need(op in ("le", "lt", "ge", "gt") and type(actual) in (int, float)
             and type(expected) in (int, float), "Invalid numeric comparison")
        ok = {"ge": actual >= expected, "gt": actual > expected,
              "le": actual <= expected, "lt": actual < expected}[op]
    need(ok, "Measured assertion failed: " + str(row["metric"]))


def product_paths(paths):
    return {p for p in paths if p.startswith(("app/", "tools/", "scripts/", "gradle/"))
            and not p.startswith(("app/src/test/", "app/src/androidTest/", "app/build/"))
            and not p.endswith("/ReleaseManifestR926.java") and not Path(p).name.startswith("test_")}


def timestamp(s):
    instant = datetime.fromisoformat(s)
    need(instant.tzinfo is not None, "Observation lacks timezone")
    return instant.astimezone(timezone.utc)


def background_window_seconds(start, end, required=259200):
    seconds = (timestamp(end) - timestamp(start)).total_seconds()
    need(seconds >= required, "Window shorter than required real seconds: " + str(required))
    return seconds


def case_digest(case):
    return hashlib.sha256(json.dumps(case, ensure_ascii=False, sort_keys=True,
                                    separators=(",", ":")).encode()).hexdigest()


def check_command(actual, expected):
    need(isinstance(actual, list) and actual and all(isinstance(x, str) and x.strip() for x in actual),
         "Missing actual argv")
    if expected is None:
        return
    need(len(actual) == len(expected), "Wrong argv length")
    for observed, planned in zip(actual, expected):
        if planned in ("<ISOLATED_CURRENT_SOURCE>", "<CASE_EVIDENCE>"):
            need(not observed.startswith(("<", "-")), "Unresolved or malformed command path")
        else:
            need(observed == planned, "Wrong sealed command or test selector")


def oracle_rules(source, case_id):
    """Read exact acceptance predicates from a committed JSON suite or test-source comment."""
    if source.lstrip().startswith("{"):
        predicates = decode(source)[case_id]
    else:
        marker = "STARCORE_EXPECT " + case_id + " "
        rows = [line.split(marker, 1)[1].strip() for line in source.splitlines() if marker in line]
        need(len(rows) == 1, "Missing or duplicated source oracle for " + case_id)
        predicates = decode(rows[0])
    need(isinstance(predicates, dict) and predicates, "Empty source oracle")
    return predicates


def apk_build_identity(originals, apk, member):
    member = safe_name(member)
    need(member.startswith("assets/"), "Build identity must reside in APK assets")
    with zipfile.ZipFile(originals.files[apk]) as package:
        entries = package.namelist()
        need("AndroidManifest.xml" in entries and "classes.dex" in entries and entries.count(member) == 1,
             "APK lacks Android program or uniquely identified build asset")
        return decode(package.read(member))


def check_learning_trace_receipt(run, receipt, originals):
    name = safe_name(run.get("learning_trace_index_artifact"))
    index = originals.json(name)
    need(index["source_commit"] == receipt["source_after"], "Learning trace source differs from receipt")
    def declared(field):
        batch_value, run_value = receipt.get(field), run.get(field)
        if batch_value is not None and run_value is not None:
            need(batch_value == run_value, "Conflicting learning APK identity: " + field)
        return batch_value if batch_value is not None else run_value

    apk = declared("apk_artifact")
    member = declared("apk_identity_member")
    apk_hash = declared("apk_sha256")
    need(apk in originals.files and originals.hashes[apk] == index["apk_sha256"] and
         (apk_hash is None or apk_hash == index["apk_sha256"]), "Learning trace APK differs from declared original")
    identity = apk_build_identity(originals, apk, member)
    need(identity["sourceCommit"] == receipt["source_after"] and
         identity["planSha256"] == receipt["plan_manifest_sha256"], "Learning APK build identity differs")

    # Trace references are index-relative. Bind every consumed record and binary payload
    # to the checked receipt inventory, including when the index lives in a subdirectory.
    prefix = Path(name).parent
    def registered(ref):
        path = str(prefix / safe_name(ref["path"]))
        need(path in originals.hashes and originals.hashes[path] == ref["sha256"],
             "Undeclared or changed learning trace original: " + path)
        return path

    for cycle in index["cycles"]:
        for stage in ("train", "publish", "freeze", "settle", "feedback", "research"):
            originals.json(registered(cycle[stage]))
        train = originals.json(registered(cycle["train"]))
        originals.json(registered(train["formula_original"]))
        state = originals.json(registered(train["state_original"]))
        registered(state["weights_original"])
    for ref in index["births"]:
        birth = originals.json(registered(ref))
        for formula in [*birth["parent_originals"], birth["child_original"]]:
            originals.json(registered(formula))
    verify_learning_trace(index, originals.files[name].parent)


def check_result(result, case, receipt, originals, repo, owners):
    case_id = case["id"]
    need(result["status"] == "PASS" and type(result["exit_code"]) is int and result["exit_code"] == 0,
         "Runner failed " + case_id)
    log = result["log"]
    run_name = result["execution_artifact"]
    run = originals.json(run_name)
    need(log != run_name and log in originals.files and originals.files[log].stat().st_size > 0,
         "Missing original runner log")
    expected_identity = {"id": case_id, "issue_id": case["issue_id"], "batch": case["batch"],
                         "level": case["level"], "source_after": receipt["source_after"],
                         "plan_manifest_sha256": receipt["plan_manifest_sha256"],
                         "case_contract_sha256": case_digest(case), "harness": case["test_contract"]["harness"],
                         "exit_code": 0, "command": result["command"], "log_sha256": originals.hashes[log]}
    for k, value in expected_identity.items():
        need(k in run and exact(run[k], value), "Wrong run origin: " + case_id + "/" + k)
    if case["level"] in ("DEVICE", "NATURAL"):
        for k in ("apk_sha256", "device_identity", "installed_version_code", "window_start", "window_end", "fixture"):
            need(k in run and exact(run[k], receipt[k]), "Wrong field runner context: " + k)
    contract = case["test_contract"]
    command = contract["command_argv"]
    if contract.get("path"):
        path, method = contract["path"], contract["method"]
        if run.get("equivalent_mapping_path"):
            # The mapping exists in the immutable source commit and is subject to review.
            mapping = decode(git(repo, "show", receipt["source_after"] + ":" +
                                 safe_name(run["equivalent_mapping_path"])))[case_id]
            need(mapping["case_contract_sha256"] == case_digest(case), "Stale equivalent runner mapping")
            path, method, command = safe_name(mapping["path"]), mapping["method"], mapping["command_argv"]
            need(command and method and (case_id in command or any(method in a for a in command)),
                 "Equivalent runner does not select this case")
        blob = git(repo, "rev-parse", receipt["source_after"] + ":" + path)
        need(run["runner_path"] == path and run["runner_method"] == method and
             run["runner_blob_sha"] == blob and git(repo, "cat-file", "-t", blob) == "blob",
             "Harness source identity mismatch")
    check_command(result["command"], command)
    need(isinstance(run.get("owner_paths"), list) and set(run["owner_paths"]).intersection(owners),
         "Run lacks a production owner declared on its issue")
    for path in run["owner_paths"]:
        need(path in owners and git(repo, "cat-file", "-t", receipt["source_after"] + ":" + path) == "blob",
             "Owner outside issue scope or absent from source")
    need(log in originals.files and originals.hashes[run_name] != originals.hashes[log], "Run aliases its log")
    expected_review = set(case.get("required_review_ids", []))
    actual_review = set()
    observations = run.get("assertions")
    need(isinstance(observations, list) and observations and exact(observations, result.get("assertions")),
         "Receipt measurements differ from recorded runner assertions")
    oracle_path = safe_name(run["oracle_path"])
    source = git(repo, "show", receipt["source_after"] + ":" + oracle_path)
    rules = oracle_rules(source, case_id)
    metrics = set()
    for obs in observations:
        need(obs["metric"] not in metrics, "Duplicate metric within case")
        metrics.add(obs["metric"])
        rule = rules[obs["metric"]]
        need(rule["operator"] == obs.get("operator", "eq") and rule.get("review_id") == obs.get("review_id"),
             "Assertion differs from source predicate")
        if "expected_pointer" in rule:
            expectation = pointer(originals.json(obs["expected_artifact"]), rule["expected_pointer"])
            need((obs["expected_artifact"], rule["expected_pointer"]) != (obs["evidence_artifact"], obs["json_pointer"]),
                 "Expected and actual reference the same observation")
        else:
            expectation = rule["expected"]
        need(exact(expectation, obs["expected"]), "Expected value differs from committed rule or reference")
        name = obs["evidence_artifact"]
        need(name in originals.files and originals.hashes[name] != originals.hashes[run_name],
             "Run cannot attest to itself as the raw observation")
        measured = pointer(originals.json(name), obs["json_pointer"])
        need(exact(measured, obs["actual"]), "Measurement differs from raw observation: " + obs["metric"])
        assert_measured(obs)
        if obs.get("review_id"):
            actual_review.add(obs["review_id"])
    need(metrics == rules.keys(), "Missing or surplus committed measurement for " + case_id)
    need(expected_review == actual_review, "Review obligations differ for " + case_id)
    verify_direction_measurements(case, run, observations, originals)
    if case_id == "A-B12-03b" and "F3-B12-01" in expected_review:
        check_learning_trace_receipt(run, receipt, originals)


def campaign(receipt, originals):
    background_window_seconds(receipt["window_start"], receipt["window_end"])
    run = originals.json(receipt["background_campaign_artifact"])
    for key in ("apk_sha256", "device_identity", "window_start", "window_end", "installed_version_code", "fixture"):
        need(exact(run[key], receipt[key]), "Wrong campaign " + key)
    need(run["source_commit"] == receipt["source_after"], "Wrong campaign source")
    keys = ("preregistered_calendar_artifact", "opportunity_ledger_artifact", "boot_segments_artifact",
            "interventions_artifact", "raw_events_artifact", "checkpoint_24h_artifact", "coverage_evaluator_artifact")
    names = [run[k] for k in keys]
    need(len(set(names)) == len(names) and all(n in originals.files for n in names),
         "Missing or aliased campaign input")
    calendar, ledger = originals.json(names[0]), originals.json(names[1])
    targets = [x["target_key"] for x in calendar["targets"]]
    rows = ledger["opportunities"]
    seen = [x["target_key"] for x in rows]
    set_targets = set(targets)
    need(targets and all(targets) and len(set(targets)) == len(targets) and len(set(seen)) == len(seen)
         and set(targets) == set(seen), "Calendar and terminal opportunity keys differ")
    need(all(x.get("terminal_state") and x.get("reason") and x.get("role") for x in rows), "Missing terminal reason or role")
    checkpoint = originals.json(names[5])
    start, end = timestamp(receipt["window_start"]), timestamp(receipt["window_end"])
    cutoff = start + timedelta(hours=24)
    need(timestamp(checkpoint["window_start"]) == start and timestamp(checkpoint["window_end"]) == cutoff,
         "24h checkpoint must end exactly at start+24h")
    need(checkpoint["source_commit"] == receipt["source_after"] and
         all(checkpoint[key] == receipt[key] for key in ("apk_sha256", "device_identity", "installed_version_code"))
         and checkpoint["calendar_sha256"] == originals.hashes[names[0]], "Foreign checkpoint")
    first_day = set()
    for target in calendar["targets"]:
        when = timestamp(target["opportunity_at"])
        need(start <= when < end, "Campaign opportunity outside registered window")
        if when < cutoff:
            first_day.add(target["target_key"])
    prefix = checkpoint["opportunities"]
    need(first_day and len(prefix) == len(first_day) and {r["target_key"] for r in prefix} == first_day,
         "24h opportunity set differs from the campaign calendar prefix")
    event_document = originals.json(names[4])
    need(checkpoint["raw_events_sha256"] == originals.hashes[names[4]], "24h prefix uses a different event series")
    latest = {}
    event_keys = set()
    for event in event_document["opportunity_events"]:
        when = timestamp(event["observed_at"])
        k = event["target_key"]
        sequence = event["sequence"]
        need(k in set_targets and type(sequence) is int and sequence >= 0, "Unknown raw event target or order")
        if k in first_day and start <= when <= cutoff:
            stamp = (when, sequence)
            need((k, stamp) not in event_keys, "Ambiguous 24h event order")
            event_keys.add((k, stamp))
            previous = latest.get(k)
            if previous is None or stamp > previous[0]:
                latest[k] = stamp, event
    for row in prefix:
        k = row["target_key"]
        if k in latest:
            event = latest[k][1]
            need(all(event[x] == row[x] for x in ("observed_at", "role", "terminal_state", "reason")),
                 "24h row differs from last observation available by cutoff")
        else:
            need(row["terminal_state"] == "NOT_OBSERVED_AS_OF_CUTOFF" and
                 timestamp(row["observed_at"]) == cutoff, "Unobserved 24h target lacks explicit cutoff gap")
        need(row.get("terminal_state") and row.get("reason") and row.get("role") and
             start <= timestamp(row["observed_at"]) <= cutoff,
             "Checkpoint fact recorded outside the first 24h")
    if checkpoint.get("exported_at"):
        need(timestamp(checkpoint["exported_at"]) >= cutoff, "Checkpoint exported before cutoff")
    outcome = originals.json(names[6])
    need(outcome["verdict"] == "PASS" and outcome["unattended"] is True
         and type(outcome["opportunity_count"]) is int and outcome["opportunity_count"] == len(targets)
         and outcome["calendar_sha256"] == originals.hashes[names[0]]
         and outcome["ledger_sha256"] == originals.hashes[names[1]],
         "Background evaluator failed or discarded opportunities")


def future_act_rows(receipt, pre, calendar, result, originals):
    rows = originals.json(result["future_act_ledger_artifact"])
    for key in ("candidate_hash", "protocol_hash"):
        need(rows[key] == pre[key], "Future ACT ledger identity mismatch")
    need(rows["role"] == "PREREGISTERED_FUTURE" and rows["fixture"] is False and
         rows["source_commit"] == receipt["source_after"], "Wrong ACT evidence role or source")
    freeze_file, draw_file = result["frozen_predictions_artifact"], result["authoritative_draws_artifact"]
    need(len({freeze_file, draw_file, result["future_act_ledger_artifact"], receipt["future_calendar_artifact"]}) == 4,
         "Freeze, draw, settlement and calendar originals must be independent")
    freezes, draws = originals.json(freeze_file), originals.json(draw_file)
    need(all(freezes[k] == pre[k] for k in ("candidate_hash", "protocol_hash")) and
         freezes["source_commit"] == receipt["source_after"] and freezes["fixture"] is False
         and freezes["role"] == "PREREGISTERED_FUTURE", "Foreign freeze original")
    need(pre.get("draw_source_id") and draws["source_id"] == pre["draw_source_id"] and draws["fixture"] is False,
         "Draw source differs from preregistration")
    def index(records):
        table = {r["target_key"]: r for r in records}
        need(len(table) == len(records), "Duplicate target in originals")
        return table

    targets, forecasts, labels = index(calendar["targets"]), index(freezes["predictions"]), index(draws["draws"])
    seen, hits = set(), 0
    for act in rows["settled_acts"]:
        k = act["target_key"]
        need(k in targets and k in forecasts and k in labels and k not in seen and type(act["hit"]) is bool,
             "Invalid, mismatched or duplicate ACT target")
        prediction, label = forecasts[k], labels[k]
        frozen = timestamp(prediction["freeze_return_at"])
        draw, settled = timestamp(label["draw_at"]), timestamp(act["settled_at"])
        opening, deadline = (timestamp(targets[k][x]) for x in ("opportunity_at", "deadline_at"))
        need(timestamp(pre["freeze_at"]) < frozen and opening <= frozen < deadline and
             frozen < draw <= settled <= timestamp(receipt["window_end"]) and
             timestamp(act["freeze_return_at"]) == frozen and timestamp(act["draw_at"]) == draw,
             "ACT misses preregistered opening/deadline or authoritative draw time")
        pos, numbers, order = prediction["position"], prediction["numbers"], label["numbers"]
        need(type(pos) is int and 1 <= pos <= 10 and len(numbers) == len(set(numbers)) == 3 and
             all(type(v) is int and 1 <= v <= 10 for v in numbers) and
             len(order) == 10 and all(type(v) is int for v in order) and set(order) == set(range(1, 11)),
             "Invalid single position, K3 prediction, or PK10 draw")
        hit = order[pos - 1] in numbers
        need(hit == act["hit"], "Settlement hit differs from frozen position and authoritative draw")
        seen.add(k)
        hits += int(hit)
    need(len(seen) == result["future_act_count"], "ACT support differs from unique settled originals")
    return hits


def natural_status(batch, verdict, acts):
    if batch == "B12" and (acts < 250 or verdict in {"PENDING_NATURAL", "NATURAL_PENDING"}):
        return "RECORDED_NONPASS"
    return "PASS_RECEIPT_STRUCTURE_AND_DIGESTS"


def near(actual, expected, label):
    need(type(actual) in (int, float) and math.isfinite(actual) and
         math.isclose(actual, expected, rel_tol=1e-10, abs_tol=1e-10), label)


def calibration_quality(src, values):
    # Same validation-row order, ties, inclusive per-row support and ten bins as fitRolling.
    samples = src["samples"]
    n = len(samples)
    need(type(values["support"]) is int and values["support"] == n and 200 <= n <= 5000, "Q05 support outside production window")
    keys = [(r["ordinal"], r["position"]) for r in samples]
    need(len(set(keys)) == n and keys == sorted(keys), "Q05 duplicate or unordered FUTURE rows")
    cells = []
    in_support, width, issues = 0, [0.0]*10, {}
    for row in samples:
        p, y, raw = row["p"], row["hit"], row["rawProbability"]
        lo, hi = row["supportLo"], row["supportHi"]
        need(all(type(x) in (int, float) and 0 <= x <= 1 for x in (p, raw, lo, hi)) and lo <= hi
             and type(y) is int and y in (0,1) and type(row["position"]) is int and 1 <= row["position"] <= 10
             and type(row["ordinal"]) is int and type(row["fitThroughOrdinal"]) is int and
             row["fitThroughOrdinal"] < row["ordinal"] - 1 and row["fitRows"] == 120
             and row["fitRole"] == "H_SELECT" and row["evaluationRole"] == "FUTURE_READ_ONLY" and row["fitHash"],
             "Q05 lacks the declared prior fit, embargo, support range or FUTURE observation")
        supported = lo <= p <= hi
        need(row["supported"] is supported, "Q05 per-row support differs from its prior fit")
        in_support += supported
        cells.append((p,y))
        width[min(9, int(p*10))] += p-y
        issues.setdefault(row["ordinal"], []).append(((p-y)**2, (raw-y)**2, (.30-y)**2))
    ranked = sorted(cells, key=lambda pair: pair[0])
    size = n//10
    mass = sum(abs(sum(p for p,y in ranked[b*size:(b+1)*size if b<9 else n]) -
                   sum(y for p,y in ranked[b*size:(b+1)*size if b<9 else n])) / n for b in range(10))
    brier = sum((p-y)**2 for p,y in cells)/n
    base = sum((.30-y)**2 for _,y in cells)/n
    actual = {"eceEqualMass":mass,"eceEqualWidth":sum(abs(v) for v in width)/n,"brier":brier,
              "baselineBrier":base,"bss":1-brier/base,"supportCoverage":in_support/n}
    for k,v in actual.items():
        near(values[k], v, "Q05 mismatch: " + k)
    need(mass <= .04 and in_support/n >= .90, "Q05 equal-mass ECE or support coverage failed")
    # Fixed 64-period blocks cluster all positions of a period before averaging; retain incomplete blocks.
    grouped = {}
    for ordinal, positions in issues.items():
        block = grouped.setdefault(ordinal//64, [])
        block.append([sum(x[i] for x in positions)/len(positions) for i in range(3)])
    scores = [1 - sum(x[0] for x in rows)/sum(x[2] for x in rows)
              for rows in grouped.values() if len(rows)==64]
    block = src["calibratedBssBlockInterval95"]
    need(block["blocks"] == len(scores), "Q05 interval block count mismatch")
    if len(scores)<4:
        need(block["status"] == "INSUFFICIENT_FIXED_BLOCKS" and all(block[k] is None for k in ("mean","lower95","upper95")),
             "Q05 interval fabricated under four complete blocks")
    else:
        t = (0,12.706,4.303,3.182,2.776,2.571,2.447,2.365,2.306,2.262,2.228,2.201,2.179,2.160,2.145,2.131,2.120,
             2.110,2.101,2.093,2.086,2.080,2.074,2.069,2.064,2.060,2.056,2.052,2.048,2.045,2.042)
        count=len(scores); mean=sum(scores)/count
        radius=t[min(count-1,30)]*math.sqrt(sum((x-mean)**2 for x in scores)/((count-1)*count))
        for key,val in (("mean",mean),("lower95",mean-radius),("upper95",mean+radius)):
            near(block[key],val,"Q05 time-block interval mismatch")


def wilson_pair(x, n):
    z = 1.959963984540054
    p, d = x/n, 1 + z*z/n
    c, h = (p+z*z/(2*n))/d, z*math.sqrt(p*(1-p)/n + z*z/(4*n*n))/d
    return max(0, c-h), min(1, c+h)


def verify_wilson(ci, detected, n):
    need(ci["events"] == detected and ci["independentReplicates"] == n, "Wrong arm CI support")
    lo, hi = wilson_pair(detected,n)
    for key,val in (("lower95",lo),("upper95",hi),("rate",detected/n)):
        near(ci[key],val,"Q08 Wilson interval differs from trial originals")


def cp_upper(x,n):
    if x==n:
        return 1.0
    lo, hi = x/n, 1.0
    for _ in range(200):
        mid = (lo+hi)/2
        cdf = sum(math.comb(n,j)*mid**j*(1-mid)**(n-j) for j in range(x+1))
        if cdf>.05:
            lo=mid
        else:
            hi=mid
    return hi


def factory_quality(src, pre, result):
    data = src["factory"]
    arms = ("NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL","SIGNAL_DISABLED","DRIFT","GAPS")
    protocol = data["protocol"]
    canonical = json.dumps(protocol, sort_keys=True, separators=(",", ":"), ensure_ascii=False)
    need(hashlib.sha256(canonical.encode()).hexdigest() == data["protocolHash"] ==
         "91b63a645fc0fcbfd0dbb6488dd252d2d6b5ca28ce941211dac8b71c6fa0a930",
         "Factory protocol differs from original R34 field registration; assess a new protocol separately")
    need(protocol["version"] == "R31_FULL_FACTORY_EXPERIMENT_2_FIXED_FUTURE" and
         protocol["arms"] == list(arms) and protocol["replicatesPerArm"] == 20 and protocol["seedBase"] == 710000
         and protocol["warmRows"] == 3000 and protocol["futureRows"] == 6000 and protocol["signalStrength"] == .30
         and protocol["farUpper95Budget"] == .15 and protocol["powerLower95Floor"] == .70
         and data["protocolHash"] == pre["factory_protocol_hash"] and data["sourceCommit"] == result["source_commit"]
         and data["realPermission"] is False and data["namespace"] == "SYNTHETIC_FULL_FACTORY_R31",
         "Q08 protocol, role or source mismatch")
    expected = {(arm,seed) for arm in arms for seed in range(710000,710020)}
    rows = data["trialResults"]
    need(data["plannedTrials"] == data["completedTrials"] == len(rows) == 120 and data["failedTrials"] == 0
         and {(r["arm"],r["seed"]) for r in rows} == expected, "Q08 requires the 120 registered arm/seed originals")
    counts = {arm:0 for arm in arms}
    for row in rows:
        need(row["protocolHash"] == data["protocolHash"] and row["namespace"] == data["namespace"] and
             row["sourceCommit"] == data["sourceCommit"] and row["naturalQualityEligible"] is False and
             row["realPermission"] is False and row["status"] in ("DETECTED","NOT_DETECTED"), "Unverified synthetic trial")
        counts[row["arm"]] += int(row["status"] == "DETECTED")
    groups = data["arms"]
    need(len(groups) == 6 and {g["arm"] for g in groups} == set(arms), "Missing factory arm interval")
    for g in groups:
        detected = counts[g["arm"]]
        need(g["completed"] == 20 and g["failures"] == 0 and g["detected"] == detected, "Arm result count mismatch")
        verify_wilson(g["independentTrialInterval"],detected,20)
    far = cp_upper(sum(counts[k] for k in ("NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL_DISABLED")),60)
    power = 1 - cp_upper(20-counts["SIGNAL"],20)
    near(data["falseAlarmUpper95"], far, "Wrong pooled false-alarm bound")
    near(data["signalPowerLower95"], power, "Wrong registered power bound")
    need(data["meetsRegisteredExperiment"] is True and far <= .15 and power >= .70,"Factory CI gates failed")
    # Independent R30 injected-edge diagnostic is separate from the 120 factory trials.
    powers = src["power_trials"]
    edges=(.34,.37,.40)
    need(src["power_source_commit"] == data["sourceCommit"] and len(powers)==60 and
         {(r["trueHitProbability"],r["rep"]) for r in powers} == {(edge,i) for edge in edges for i in range(20)},
         "Missing 34/37/40 percent diagnostic replicates")
    intervals=src["power_intervals"]
    need(len(intervals)==3 and {r["trueHitProbability"] for r in intervals}==set(edges), "Missing edge diagnostic interval")
    for group in intervals:
        cases=[r for r in powers if r["trueHitProbability"]==group["trueHitProbability"]]
        need(all(type(r["detected"]) is bool and type(r["periods"]) is int and 0<r["periods"]<=5000 for r in cases),
             "Invalid injected signal replicate")
        verify_wilson(group["detectionProbabilityInterval95"], sum(r["detected"] for r in cases),20)


def quality_projection(pre, result, originals, n, hits, opportunities):
    status = result["quality_projection_status"]
    need(status in {"PASS", "FAIL", "NOT_OBSERVED", "INSUFFICIENT", "PENDING", "BLOCKED"}, "Unknown quality projection status")
    if status != "PASS":
        return status
    need(result["issuance_status"] == "ACTIVE", "Q07 lacks a legally issued certificate")
    qset = {f"Q{i:02d}" for i in range(1, 9)}
    body = originals.json(result["quality_projection_artifact"])
    need(all(body[k] == result[k] for k in ("candidate_hash", "protocol_hash", "source_commit"))
         and set(body["components"]) == qset, "Quality projection lacks matching components")
    docs = {}
    for q in sorted(qset):
        c = body["components"][q]
        src = originals.json(c["evidence_artifact"])
        need(c["status"] == "PASS" and src["id"] == q and src["source_commit"] == result["source_commit"],
             "Quality component missing or foreign: " + q)
        if q != "Q08":
            need(all(src[k] == result[k] for k in ("candidate_hash","protocol_hash")), "Foreign natural quality scope")
        docs[q] = src
    vals={q:src["values"] for q,src in docs.items() if q!="Q08"}
    need(n>=250 and hits/n >= .34 and opportunities>0, "Q01 natural support or hit rate below threshold")
    near(vals["Q01"]["hit_rate"],hits/n,"Q01 hit-rate mismatch")
    near(vals["Q02"]["future_act_rate"],n/opportunities,"Q02 future rate mismatch")
    real=docs["Q02"]["deployed_opportunities"]
    need(len(real)>=250 and len({r["target_key"] for r in real})==len(real) and all(type(r["real_act"]) is bool for r in real),
         "Missing post-deployment REAL opportunity ledger")
    realrate=sum(r["real_act"] for r in real)/len(real)
    near(vals["Q02"]["real_act_rate"],realrate,"Q02 deployed rate mismatch")
    need(realrate >= .15, "Q02 actual deployed frequency below target")
    a, loge=pre["allocated_alpha"],vals["Q03"]["log_e_up"]
    need(type(a) in (float,int) and 0<a<1 and math.isfinite(loge) and loge>=math.log(1/a)
         and vals["Q03"]["sequential_pass"] is True, "Q03 sequential gate failed")
    need(vals["Q04"]["worst720Block"] >= .26 and 0 <= vals["Q04"]["maxLosingStreak"] <= 6
         and docs["Q04"]["window_rule_hash"] == pre["stability_window_rule_hash"], "Q04 failed frozen block rules")
    calibration_quality(docs["Q05"], vals["Q05"])
    need(vals["Q06"]["paired_lcb_vs_parent"] > 0 and docs["Q06"]["interval_scope"] == "ONE_SIDED_95_PAIRED_SAME_OPPORTUNITY",
         "Q06 requires a positive parent/child paired lower bound")
    need(vals["Q07"]["independently_qualified_assets"] >= 1 and
         result["candidate_hash"] in docs["Q07"]["qualified_candidate_hashes"], "Q07 lacks independently audited active asset")
    factory_quality(docs["Q08"],pre,result)
    return status


def natural(receipt, originals):
    fields = ("preregistration_artifact", "future_calendar_artifact", "natural_evaluator_artifact")
    names = [receipt[k] for k in fields]
    need(len(set(names)) == 3, "Natural evidence inputs alias each other")
    pre, calendar, result = (originals.json(n) for n in names)
    for k in ("candidate_hash", "protocol_hash"):
        need(isinstance(pre[k], str) and pre[k] and pre[k] == calendar[k] == result[k], "Natural identity mismatch: " + k)
    need(result["source_commit"] == receipt["source_after"] and result["apk_sha256"] == receipt["apk_sha256"]
         and result["fixture"] is False and result["preregistration_sha256"] == originals.hashes[names[0]]
         and result["calendar_sha256"] == originals.hashes[names[1]], "Natural evaluator provenance mismatch")
    start, end = timestamp(receipt["window_start"]), timestamp(receipt["window_end"])
    freeze = timestamp(pre["freeze_at"])
    need(freeze <= start, "Future observation precedes preregistration")
    keys = []
    for row in calendar["targets"]:
        need(start <= timestamp(row["opportunity_at"]) <= end and freeze < timestamp(row["opportunity_at"]) < timestamp(row["deadline_at"]),
             "Opportunity outside registered future window")
        keys.append(row["target_key"])
    need(all(keys) and len(keys) == len(set(keys)) and type(result["opportunity_count"]) is int
         and result["opportunity_count"] == len(keys), "Natural opportunity denominator mismatch")
    verdict = result["verdict"]
    need(verdict in {"QUALIFIED", "REJECTED", "REVOKED", "NATURAL_PENDING", "PENDING_NATURAL"},
         "Unknown qualification state")
    n = result["future_act_count"]
    need(type(n) is int and 0 <= n <= len(keys) and
         (keys or verdict in {"NATURAL_PENDING", "PENDING_NATURAL"}), "Inconsistent natural support")
    hits = future_act_rows(receipt, pre, calendar, result, originals) if n else 0
    math_verdict, issuance_status = result["math_verdict"], result["issuance_status"]
    need(math_verdict in {"PASS", "FAIL", "NOT_EVALUATED"} and
         issuance_status in {"ACTIVE", "NOT_ISSUED", "REVOKED", "NOT_EVALUATED"},
         "Unknown math verdict or issuance status")
    need((verdict == "QUALIFIED") == (issuance_status == "ACTIVE") and
         (verdict == "REVOKED") == (issuance_status == "REVOKED"), "Conflicting issuance status")
    if verdict == "QUALIFIED":
        need(math_verdict == "PASS", "Active certificate failed math verdict")
        gates = result["v1_metrics"]
        for metric, op, bound in (("hr", "ge", 0.34), ("ece", "le", 0.04), ("worst720Block", "ge", 0.26),
                                  ("maxLosingStreak", "le", 6), ("pairedLcbVsI00", "gt", 0),
                                  ("pairedLcbVsNull", "gt", 0)):
            assert_measured({"metric": metric, "actual": gates[metric], "operator": op, "expected": bound,
                             "evidence_artifact": names[2]})
        need(all(gates[key] is True for key in ("abi_equivalent", "sequential_pass")),
             "Certificate fails ABI or sequential gate")
        need(n >= 250 and result["v1_pass"] is True and result.get("certificate_artifact") in originals.files,
             "Qualified result lacks original certificate or V1 support")
        need(100 * n >= 15 * len(keys) and math.isclose(gates["hr"], hits / n, rel_tol=1e-12),
             "Independent future coverage below 15% or hit rate differs from ACT rows")
        need(0 <= gates["ece"] <= 1 and 0 <= gates["worst720Block"] <= 1 and
             type(gates["maxLosingStreak"]) is int and gates["maxLosingStreak"] >= 0, "Invalid V1 metric")
        cert = originals.json(result["certificate_artifact"])
        issuing = cert["issuance"]
        need(cert["status"] == "ACTIVE" and issuing["issued"] is True and
             all(issuing[k] == "BOUND" for k in ("model_binding", "calibration_binding", "qualification_binding"))
             and issuing["source_revoked"] is False and issuing["qualification_gap_or_conflict"] is False
             and not issuing["failing"], "No effective issued certificate or invalid bindings")
        future = issuing["registeredFutureR34"]
        need(all(type(future[k]) is int for k in ("opportunities", "acts", "hits")) and
             (future["opportunities"], future["acts"], future["hits"]) == (len(keys), n, hits),
             "Issuance differs from independent future rows")
        win, miss = future["winNetProfit"], future["missLoss"]
        need(all(type(x) in (int, float) and math.isfinite(x) and x > 0 for x in (win, miss)),
             "Invalid recorded payoff definition")
        profit = hits * win - (n - hits) * miss
        need(profit > 0 and type(future["netRecordedPayoff"]) in (int, float) and
             math.isclose(profit, future["netRecordedPayoff"], rel_tol=1e-12, abs_tol=1e-12)
             and future["coveragePass"] is True and future["expectedPayoffPositive"] is True,
             "Non-positive or inconsistent recorded payoff")

        need(cert["candidate_hash"] == pre["candidate_hash"] and cert["protocol_hash"] == pre["protocol_hash"],
             "Certificate identity mismatch")
        issued, expires, evaluated = (timestamp(cert["issued_at"]), timestamp(cert["expires_at"]),
                                      timestamp(result["evaluated_at"]))
        need(start <= issued <= evaluated < expires and evaluated <= end
             and (expires - issued).total_seconds() <= 86400, "Certificate outside validity window")
    # A research-quality projection and post-deployment ACT may still be unavailable at first issuance.
    quality = quality_projection(pre, result, originals, n, hits, len(keys))
    return {"verdict": verdict, "future_act_count": n, "math_verdict": math_verdict,
            "issuance_status": issuance_status, "quality_projection_status": quality}


def validate(plan, receipt, evidence_root, repo, level, revalidating=False):
    work = read(plan / "WORK_ORDERS.json")
    ident = read(plan / "control/PLAN_IDENTITY.json")
    seal = verify_seal(plan)
    need(receipt["schema"] == "STARCORE_R341_EXECUTION_RECEIPT_2" and receipt.get("template_only") is False,
         "Wrong schema or unsigned template; historical receipts retain their original seal")
    need(receipt["plan_manifest_sha256"] == seal["manifest_sha256"] and receipt["level"] == level,
         "Wrong plan seal or receipt level")
    batches = {b["id"]: b for b in work["batches"]}
    batch = batches[receipt["batch"]]
    issue_map = {i["id"]: i for i in batch["issues"]}
    need(sorted(receipt["issue_ids"]) == sorted(issue_map), "Batch issue identities differ")
    if receipt["status"] != "PASS":
        allowed = {"NOT_RUN", "BLOCKED", "FAILED", "FAIL", "NOT_APPLICABLE", "NOT_OBSERVED", "NATURAL_PENDING"}
        need(receipt["status"] in allowed and isinstance(receipt.get("reason"), str) and receipt["reason"].strip(),
             "Pending or failed record requires an explicit reason")
        return {"status": "RECORDED_NONPASS", "reported_status": receipt["status"], "batch": batch["id"], "level": level}
    before, after = receipt["source_before"], receipt["source_after"]
    for commit in (before, after):
        need(isinstance(commit, str) and re.fullmatch("[0-9a-f]{40}", commit), "Exact Git commit required")
        need(git(repo, "cat-file", "-t", commit) == "commit", "Git object is not a commit")
    git(repo, "merge-base", "--is-ancestor", ident["application_baseline_commit"], before)
    git(repo, "merge-base", "--is-ancestor", before, after)
    originals = Originals(evidence_root, receipt["artifacts"])
    owners = {i: {f["path"] for f in order["files"] if f.get("kind") != "ACCEPTANCE_HARNESS_CONTRACT"}
              for i, order in issue_map.items()}
    if level == "ENGINEERING":
        diff = set(git(repo, "diff", "--no-renames", "--diff-filter=AM", "--name-only", "-z", before, after).split("\0"))
        planned_edits = {f["path"] for order in issue_map.values() for f in order["files"]
                         if f["action"] in {"NEW", "MODIFY"} and f.get("kind") != "ACCEPTANCE_HARNESS_CONTRACT"}
        if receipt.get("revalidation_only") is True:
            need(not revalidating, "Nested revalidation cannot prove implementation")
            ref = receipt["original_implementation_receipt_artifact"]
            original = originals.json(ref)
            need(original["status"] == "PASS" and original["batch"] == batch["id"] and
                 original["level"] == level and original.get("revalidation_only") is False and
                 originals.hashes[ref] == receipt["original_implementation_receipt_sha256"],
                 "Wrong original implementation receipt")
            git(repo, "merge-base", "--is-ancestor", original["source_after"], before)
            validate(plan, original, originals.files[ref].parent, repo, level, True)
        else:
            need(product_paths(diff & planned_edits), "No planned production owner changed in this batch")
    cases = {c["id"]: c for c in read(plan / "ACCEPTANCE_MATRIX.json")["cases"]
             if c["batch"] == batch["id"] and c["level"] == level}
    need(cases and sorted(x["id"] for x in receipt["test_results"]) == sorted(cases), "Wrong level/case set")
    if level in ("DEVICE", "NATURAL"):
        apk = receipt["apk_artifact"]
        need(apk in originals.files and originals.hashes[apk] == receipt["apk_sha256"] and
             isinstance(receipt.get("device_identity"), str) and receipt["device_identity"].strip() and
             type(receipt["installed_version_code"]) is int and
             receipt["installed_version_code"] > ident["application_baseline_version_code"]
             and receipt["fixture"] is False, "Field evidence lacks installed identity or contains a fixture")
        identity = apk_build_identity(originals, apk, receipt["apk_identity_member"])
        need(identity["sourceCommit"] == after and identity["planSha256"] == receipt["plan_manifest_sha256"]
             and type(identity["versionCode"]) is int and identity["versionCode"] == receipt["installed_version_code"],
             "Embedded APK identity differs from observed installation")
        need(timestamp(receipt["window_start"]) < timestamp(receipt["window_end"]) <= datetime.now(timezone.utc),
             "Invalid observation window or window in the future")
    for result in receipt["test_results"]:
        case = cases[result["id"]]
        check_result(result, case, receipt, originals, repo, owners[case["issue_id"]])
    if level == "DEVICE" and cases.keys() & {"A-B12-04a", "R341-B05-03-T02"}:
        campaign(receipt, originals)
    outcome = natural(receipt, originals) if level == "NATURAL" else None
    status = natural_status(batch["id"], outcome["verdict"], outcome["future_act_count"]) if outcome else "PASS_RECEIPT_STRUCTURE_AND_DIGESTS"
    return {"status": status, "batch": batch["id"], "level": level,
            "cases": len(cases), "natural_evaluator_outcome": outcome,
            "boundary": "Review runner implementation, field authenticity, thresholds and business semantics separately."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--plan", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--evidence-root", type=Path, required=True)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--level", choices=["ENGINEERING", "DEVICE", "NATURAL"], required=True)
    args = parser.parse_args()
    try:
        result = validate(args.plan, read(args.receipt), args.evidence_root, args.repo, args.level)
        print(json.dumps(result, ensure_ascii=False))
        if result["status"] == "RECORDED_NONPASS":
            raise SystemExit(2)
    except (ValueError, TypeError, KeyError, OSError, IndexError, OverflowError, zipfile.BadZipFile, subprocess.CalledProcessError) as error:
        raise SystemExit("FAIL_RECEIPT: " + str(error))


if __name__ == "__main__":
    main()
