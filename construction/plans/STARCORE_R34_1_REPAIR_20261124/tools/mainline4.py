"""Read back the R341 B09 engineering campaign against declared originals."""
import hashlib
import re
import json
import math
import subprocess

SCENARIOS = ("PROOF_FIRST", "PENDING_THEN_PROOF", "LATE_TERMINAL", "CONCURRENT_RETRY", "NEXT_ELIGIBLE")
SOURCES = {"qualification": "r27_certificate", "act": "r31_mainline_freeze",
           "choice": "research_consumer_choice_r932", "decision": "DecisionGateEngine.decideV51B12",
           "ledger": "V51Repository.commitCanonicalBundleVerifiedR368",
           "settlement": "SettlementEngine.settleV51ByDraw",
           "schedule": "V51RuntimeOrchestrator"}


def need(condition, message):
    if not condition:
        raise ValueError(message)


def h(value):
    return isinstance(value, str) and re.fullmatch("[0-9a-f]{64}", value)


def k3(action):
    return (isinstance(action, list) and len(action) == 4 and
            all(type(x) is int and 1 <= x <= 10 for x in action) and len(set(action[1:])) == 3)


def ticks(*values):
    return all(type(x) is int and x >= 0 for x in values) and list(values) == sorted(values)


def registered(originals, ref):
    need(isinstance(ref, dict) and isinstance(ref.get("path"), str) and
         ref["path"] in originals.files and h(ref.get("sha256")) and
         originals.hashes[ref["path"]] == ref["sha256"], "Undeclared or changed mainline original")
    return ref["path"]


def linked(row, fields, label):
    need(all(k in row and type(row[k]) is type(v) and row[k] == v for k, v in fields.items()),
         "Mainline identity mismatch: " + label)


TIMING_RUNNER = "app/src/androidTest/java/com/ethk3/novaonepro/r341/R341B09ContractTest.java"


def timing_rule(repo, source):
    raw = subprocess.run(["git", "-C", str(repo), "show", source + ":" + TIMING_RUNNER],
                         stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True).stdout
    marker = b"// R341_MAINLINE_TIMING_V1 "
    matches = [line.strip()[len(marker):] for line in raw.splitlines() if line.strip().startswith(marker)]
    need(len(matches) == 1, "Missing or ambiguous timing registration in tested source")
    rule = json.loads(matches[0])
    need(rule.get("unit") == "ms" and type(rule.get("reserve_ms")) is int and rule["reserve_ms"] > 0
         and type(rule.get("first_read_before_cutoff_ms")) is int and rule["first_read_before_cutoff_ms"] > 1,
         "Invalid registered closing reserve or first read")
    return rule


def check_campaign(index, originals, rule):
    scope = {k: index[k] for k in ("source_commit", "apk_sha256", "run_id", "time_scope", "scope")}
    need(re.fullmatch("[0-9a-f]{40}", scope["source_commit"]) and h(scope["apk_sha256"]) and
         scope["scope"] == "ENGINEERING_FIXTURE" and all(isinstance(scope[k], str) and scope[k]
         for k in ("run_id", "time_scope")), "Invalid mainline campaign identity")
    prereg_ref = index["timing_registration"]
    prereg = originals.json(registered(originals, prereg_ref))
    linked(prereg, dict(scope, runner=TIMING_RUNNER, rule=rule), "source timing registration")
    cases = index["cases"]
    need(set(cases) == set(SCENARIOS), "Missing or extra mainline interleaving")
    lineage = set()
    previous = None
    first_commit = first_trade = first_auth = None
    for name in SCENARIOS:
        case = cases[name]
        input_path = registered(originals, case["input"])
        data = originals.json(input_path)
        action, target = data["action"], data["target"]
        need(k3(action) and type(target) is int and target > 0 and h(data["policy"]) and
             data["user_switch"] is True, "Invalid registered fixture input")
        current = (target, case["input"]["sha256"], data["policy"], action)
        if previous is None:
            previous = current
        if name == "NEXT_ELIGIBLE":
            need(target == previous[0] + 1 and data["policy"] == previous[2], "Missing consecutive target")
        else:
            need(current == previous, "Interleavings did not start with the same input")
        # The consecutive target must run on the first database while the three
        # alternative interleavings use their own copies of the same input.
        db = case["db_lineage"]
        need(isinstance(db, str) and db and (db == cases["PROOF_FIRST"]["db_lineage"]
             if name == "NEXT_ELIGIBLE" else db not in lineage), "Invalid fixture database lineage")
        lineage.add(db)
        common = dict(scope, target=target, db_lineage=db, input_sha256=case["input"]["sha256"], scenario=name)
        rows = {}
        for stage, source in SOURCES.items():
            doc = originals.json(registered(originals, case[stage]))
            linked(doc, dict(common, producer=source), stage)
            rows[stage] = doc
        q, a, c, d, ledger, settlement, schedule = [rows[k] for k in SOURCES]
        linked(schedule, {"timing_registration_sha256": prereg_ref["sha256"],
                          "observed_execution_reserve_ms": rule["reserve_ms"]}, "active production time budget")
        need(type(a["deadline"]) is int and type(data["deadline"]) is int and a["deadline"] == data["deadline"],
             "ACT deadline does not match the registered input")
        cutoff = a["deadline"] - rule["reserve_ms"]
        need(type(schedule["decision_cutoff"]) is int and schedule["decision_cutoff"] == cutoff and
             ticks(prereg["registered_at"], schedule["first_read"], schedule["selected_at"], c["committed_at"])
             and prereg["registered_at"] < schedule["first_read"] and
             schedule["selected_at"] <= cutoff, "Choice precedes registration or violates fixed cutoff")
        if name in ("PENDING_THEN_PROOF","LATE_TERMINAL"):
            linked(schedule, {"first_read": cutoff-rule["first_read_before_cutoff_ms"],
                              "first_read_outcome": "PENDING", "final_rows_at_first_read": 0}, "first empty read")
        linked(q, {"policy": data["policy"], "status": "ACTIVE", "gate_version": "R27_GATE_V1"}, "certificate")
        proof_input = registered(originals, q["gate_bytes"])
        raw = originals.files[proof_input].read_bytes()
        cert = hashlib.sha256(raw).hexdigest()
        need(q["certificate"] == cert, "Certificate digest not bound to original bytes")
        evidence = originals.json(proof_input)
        linked(evidence, {"authority": "R27_GATE_V1", "failing": []}, "issuer gates")
        linked(evidence["gateEvidenceR31"], {"policyHash": data["policy"]}, "issuer policy")
        linked(evidence["registeredFutureR34"], {"coveragePass": True, "expectedPayoffPositive": True}, "issuer future")
        need(h(a["proof"]) and h(a["model"]) and h(c["bundle"]), "Incomplete identifiers")
        need(type(a["selected_hit_p"]) in (float, int) and math.isfinite(a["selected_hit_p"])
             and 0 < a["selected_hit_p"] < 1 and h(a["calibration_hash"]), "Missing calibrated ACT")
        base = dict(policy=data["policy"], certificate=cert, model=a["model"], proof=a["proof"], action=action)
        linked(a, base, "original ACT")
        need(a["act"] is True and ticks(q["issued_at"], a["frozen_at"], a["deadline"]) and
             a["frozen_at"] < a["deadline"] <= settlement["draw_at"], "ACT is late or pre-certificate")
        linked(schedule, {"choice_rows": 1, "returned_choice": c["row_hash"]}, "first-write choice")
        need(h(c["row_hash"]), "Missing immutable selected row")
        if name == "LATE_TERMINAL":
            need(schedule["selected_at"] == cutoff and c["committed_at"] >= cutoff and
                 schedule["proof_visible_at"] == cutoff + 1 and
                 a["frozen_at"] <= schedule["proof_visible_at"] < a["deadline"] and
                 schedule["final_rows_before_cutoff"] == 0 and
                 schedule["after_late_delivery_choice"] == c["row_hash"],
                 "Premature terminal, changed choice or missing cutoff-plus-one delivery")
            linked(c, {"consumer": "", "candidate": ""}, "immutable terminal")
            need(d["action_type"] in ("HOLD", "SHADOW") and d["choice_row"] == c["row_hash"]
                 and all(ledger[k] == [] for k in ("trade_rows","exact_rows","probability_rows"))
                 and settlement["real_rows"] == [] and ticks(c["committed_at"], d["authorized_at"], ledger["committed_at"], a["deadline"]) and ledger["committed_at"] < a["deadline"], "Late proof created or replaced real activity")
            continue
        if name == "PENDING_THEN_PROOF":
            need(schedule["proof_visible_at"] == cutoff - 1 and
                 schedule["proof_visible_at"] <= schedule["selected_at"] <= cutoff,
                 "Missing accepted cutoff-minus-one delivery")
        elif name in ("PROOF_FIRST","NEXT_ELIGIBLE"):
            need(schedule["proof_visible_at"] <= schedule["first_read"], "Proof was not ready first")
        need(schedule["proof_visible_at"] <= schedule["selected_at"], "Selection preceded proof availability")
        linked(c, dict(policy=base["policy"], certificate=cert, candidate="R31-R27-"+base["policy"],
                       consumer="INVENTED_ALGORITHM"), "unified choice")
        linked(d, dict(base, choice_row=c["row_hash"], lane="CONTROLLED_PROBE", action_type="BET"), "unique BET")
        linked(d, {"selected_hit_p": a["selected_hit_p"]}, "consumed calibrated probability")
        need(h(d["action_hash"]) and h(d["authorization"]) and isinstance(d["decision_id"], str) and d["decision_id"], "Missing authorization")
        need(ticks(a["frozen_at"], schedule["proof_visible_at"], c["committed_at"], d["authorized_at"],
                   ledger["committed_at"], a["deadline"], settlement["draw_at"], settlement["settled_at"])
             and ledger["committed_at"] < a["deadline"] and q["expires_at"] > ledger["committed_at"]
             and schedule["first_read"] <= c["committed_at"], "Authorization missed legal interval")
        need(all(isinstance(ledger[k], list) and len(ledger[k]) == 1 for k in
                 ("trade_rows","exact_rows","probability_rows")) and len(settlement["real_rows"]) == 1,
             "Real originals not unique for target")
        trade, exact, probability = [ledger[k][0] for k in ("trade_rows","exact_rows","probability_rows")]
        trade_id = trade["trade_id"]
        ref = dict(trade_id=trade_id, target=target, authorization=d["authorization"])
        need(type(trade_id) is int and trade_id > 0, "Invalid trade identity")
        linked(trade, dict(ref, action=action, role="REAL_TRADE", decision_id=d["decision_id"]), "trade")
        if name == "PROOF_FIRST":
            first_commit, first_trade, first_auth = ledger["committed_at"], trade_id, d["authorization"]
        if name == "NEXT_ELIGIBLE":
            need(ledger["committed_at"] > first_commit and trade_id != first_trade and d["authorization"] != first_auth,
                 "Next target reused the first transaction")
        for row in (exact, probability):
            linked(row, dict(ref, candidate=c["candidate"], model=a["model"], certificate=cert, proof=a["proof"]), "receipt")
        for row in (exact, probability):
            linked(row, {"action_hash": d["action_hash"]}, "authorized action")
        linked(probability, {"calibration_hash": a["calibration_hash"], "selected_hit_p": a["selected_hit_p"]},
               "persisted selected probability")
        draw = settlement["draw"]
        need(isinstance(draw,list) and len(draw)==10 and all(type(v) is int for v in draw)
             and set(draw)==set(range(1,11)), "Invalid draw")
        linked(settlement["real_rows"][0], dict(ref, role="REAL_TRADE", hit=draw[action[0]-1] in action[1:]), "settlement")
        if name == "CONCURRENT_RETRY":
            attempts = schedule["attempts"]
            need(isinstance(attempts,list) and len(attempts)==2 and attempts[0]["id"] != attempts[1]["id"]
                 and all(isinstance(o["id"],str) and o["id"] for o in attempts) and
                 all(ticks(o["start"],o["end"]) for o in attempts) and
                 max(x["start"] for x in attempts) < ledger["committed_at"] <= min(x["end"] for x in attempts)
                 and sorted(x["result"] for x in attempts) == ["COMMITTED","REPLAY_EXISTING"]
                 and all(x["choice_row"]==c["row_hash"] and x["trade_id"]==trade_id for x in attempts),
                 "Concurrent replay did not resolve to the same originals")
    return {"campaign":"PASS_ENGINEERING_MAINLINE_INTERLEAVINGS","scenarios":len(cases)}


def check_mainline_receipt(run, receipt, originals, repo):
    # Import at invocation so verify_receipt can route the case to this module.
    from verify_receipt import apk_build_identity
    path = registered(originals, run["mainline_campaign"])
    index = originals.json(path)
    need(index["source_commit"] == receipt["source_after"], "Campaign source does not match receipt")
    for field in ("apk_artifact","apk_identity_member","apk_sha256"):
        if run.get(field) is not None and receipt.get(field) is not None:
            need(run[field] == receipt[field], "Conflicting APK identity: "+field)
    apk = receipt.get("apk_artifact") or run.get("apk_artifact")
    member = receipt.get("apk_identity_member") or run.get("apk_identity_member")
    need(apk in originals.files and originals.hashes[apk] == index["apk_sha256"] and
         (receipt.get("apk_sha256") or run.get("apk_sha256") or index["apk_sha256"]) == index["apk_sha256"],
         "Campaign APK original mismatch")
    identity = apk_build_identity(originals,apk,member)
    linked(identity, {"sourceCommit":receipt["source_after"],"planSha256":receipt["plan_manifest_sha256"]}, "APK build")
    return check_campaign(index, originals, timing_rule(repo, receipt["source_after"]))
