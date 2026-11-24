"""Reconcile a single deployed candidate against the complete app trading book."""
import math
import re
import sqlite3
from contextlib import closing
from datetime import timedelta

from background4 import instant, ordinal


def need(ok, reason):
    if not ok:
        raise ValueError("REAL_APPLICATION: " + reason)


def hashed(value):
    return isinstance(value, str) and re.fullmatch(r"[0-9a-f]{64}", value) is not None


def match(doc, expected, label):
    need(all(k in doc and type(doc[k]) is type(v) and doc[k] == v for k, v in expected.items()),
         "identity differs in " + label)


def by_target(rows, allowed):
    need(isinstance(rows, list), "missing source rows")
    out = {}
    for row in rows:
        key = row["target_key"]
        need(key in allowed and key not in out, "foreign or duplicate source target")
        out[key] = row
    return out


TABLES = {
    "calendar": "REGISTERED_DEPLOYMENT_CALENDAR",
    "choices": "research_consumer_choice_r932",
    "decisions": "DecisionGateEngine.decideV51B12",
    "trades": "trades",
    "exact": "r974_exact_real_receipt",
    "probabilities": "research_exact_probability_f05",
    "draws": "AUTHORITATIVE_DRAWS",
    "settlements": "SettlementEngine.settleV51ByDraw",
    "revocations": "CertificateRevocationR30",
}


def verify_native_book(originals, index, keys, books):
    name = index["trading_database_artifact"]
    need(name in originals.files and originals.hashes[name] == index["trading_database_sha256"],
         "missing source database or wrong digest")
    path = originals.files[name].resolve()
    # Read a consistent closed snapshot including its committed WAL. All SQL here is fixed.
    with closing(sqlite3.connect(path.as_uri() + "?mode=ro&immutable=1", uri=True)) as db:
        db.execute("PRAGMA query_only=ON")
        db.execute("PRAGMA trusted_schema=OFF")
        schema = db.execute("SELECT type,sql FROM sqlite_schema WHERE name='trades'").fetchone()
        need(schema and schema[0] == "table" and schema[1].upper().lstrip().startswith("CREATE TABLE"),
             "native trades table absent")
        sql = """SELECT target_issue,_id,position,n1,n2,n3,authorization_id,action,stat_scope,settled,hit
                   FROM trades WHERE target_issue BETWEEN ? AND ?
                     AND (action='BET' OR stat_scope='REAL_TRADE')"""
        seen = set()
        for key, tid, pos, n1, n2, n3, auth, action, role, settled, hit in db.execute(sql,(min(keys),max(keys))):
            need(key in keys and key not in seen and key in books["trades"], "hidden or duplicate native trade")
            seen.add(key)
            row = books["trades"][key]
            need(action == "BET" and role == "REAL_TRADE" and tid == row["trade_id"] and
                 [pos,n1,n2,n3] == row["action"] and auth == row["authorization"], "native trade payload mismatch")
            settlement = books["settlements"].get(key)
            need(settled in (0,1) and (settled == 1) == (settlement is not None), "native settlement mismatch")
            if settled:
                need(hit in (0,1) and settlement["hit"] is bool(hit), "native hit mismatch")
        need(seen == set(books["trades"]), "projected REAL not in source database")


def verify_deployment(receipt, result, originals):
    """The caller has checked the natural issuance. Deployments are optional at first issuance."""
    name = result.get("deployment_evidence_artifact")
    if not name:
        return None
    need(result["fixture"] is False and result["verdict"] == "QUALIFIED" and result["issuance_status"] == "ACTIVE",
         "deployment claim needs the independently verified natural issue receipt")
    index = originals.json(name)
    source = receipt["source_after"]
    candidate = result["candidate_hash"]
    scope = dict(source_commit=source, apk_sha256=receipt["apk_sha256"], device_identity=receipt["device_identity"],
                 candidate_hash=candidate, protocol_hash=result["protocol_hash"], fixture=False)
    match(index, dict(scope, schema="R341_DEPLOYMENT_BOOK_5"), "deployment index")
    cert_name = result["certificate_artifact"]
    cert = originals.json(cert_name)
    match(cert, {"candidate_hash": candidate, "protocol_hash": result["protocol_hash"]}, "natural issuance")
    need(index["issuance_artifact_sha256"] == originals.hashes[cert_name], "wrong issuance original")
    certificate, model = cert["certificate_hash"], cert["model_hash"]
    gate = cert["gate_bytes_artifact"]
    gate_doc = originals.json(gate)
    need(gate_doc["authority"] == "R27_GATE_V1" and hashed(model) and hashed(certificate) and originals.hashes[gate] == certificate and
         gate_doc["gateEvidenceR31"]["policyHash"] == candidate and gate_doc["failing"] == [],
         "wrong issuer bytes or candidate/model")
    model_binding, calibration = gate_doc["modelStateBindingR31"], gate_doc["calibrationBindingR31"]
    need("inputHash" not in gate_doc and "evaluatedAt" not in gate_doc and gate_doc["gateVersion"] == "R27_GATE_V1"
         and model_binding["state"] == "BOUND" and model_binding["policyHash"] == candidate and
         model_binding.get("modelHash", model_binding["bindingSha256"]) == model and
         calibration["state"] == "BOUND" and calibration["policyHash"] == candidate and
         hashed(calibration["calibrationSha256"]), "model/calibration not bound by the original issuer")
    issued, expires = instant(cert["issued_at"]), instant(cert["expires_at"])
    snapshot = instant(index["snapshot_at"])
    need(issued <= snapshot <= instant(receipt["window_end"]), "deployment observation outside receipt")
    refs = index["originals"]
    need(set(refs) == TABLES.keys() and len({originals.hashes[n] for n in [name, cert_name, gate, *refs.values()]})
         == len(refs) + 3, "missing or aliased deployment originals")
    docs = {}
    for k, source_table in TABLES.items():
        doc = originals.json(refs[k])
        match(doc, dict(scope, source=source_table), source_table)
        if k != "calendar":
            match(doc, {"snapshot_at": index["snapshot_at"], "calendar_sha256": originals.hashes[refs["calendar"]],
                        "trading_database_sha256": index["trading_database_sha256"]},
                  source_table)
        docs[k] = doc
    cal = docs["calendar"]
    start, end = instant(cal["window_start"]), instant(cal["window_end"])
    need(instant(cal["registered_at"]) <= start and issued <= start < end <= snapshot
         and start >= instant(receipt["window_start"]), "unregistered or out-of-scope deployment window")
    slots = cal["targets"]
    ticks = [cal["before_window"], *slots, cal["after_window"]]
    times = [instant(r["opportunity_at"]) for r in ticks]
    ords = [ordinal(r["target_key"]) for r in ticks]
    need(slots and times[0] < start <= times[1] and times[-2] < end <= times[-1] and
         all(y-x == timedelta(minutes=1) for x,y in zip(times,times[1:])) and
         all(b == a+1 for a,b in zip(ords,ords[1:])), "deployment calendar truncation or gap")
    keys = {r["target_key"] for r in slots}
    books = {k: by_target(docs[k]["rows"], keys) for k in
             ("choices", "decisions", "trades", "exact", "probabilities", "draws", "settlements")}
    need(set(books["choices"]) == keys, "unaccounted deployment opportunity")
    need(set(books["decisions"]) <= keys and set(books["trades"]) <= set(books["decisions"])
         and set(books["settlements"]) <= set(books["trades"]), "orphan real entry")
    verify_native_book(originals, index, keys, books)
    revocations = docs["revocations"]["rows"]
    need(all(r["certificate_hash"] == certificate and issued <= instant(r["revoked_at"]) <= snapshot
             for r in revocations), "wrong revocation ledger")
    revoked = min((instant(r["revoked_at"]) for r in revocations), default=expires)
    acts = []
    hit_count = settled = 0
    missing = []
    missing_authorized = []
    pending_draw = []
    unsettled_after_draw = []
    seen_ids = set()
    for slot in slots:
        k = slot["target_key"]
        op, deadline = instant(slot["opportunity_at"]), instant(slot["deadline_at"])
        need(op < deadline, "invalid deployment opportunity")
        pick = books["choices"][k]
        verdict = pick["terminal"]
        need(verdict in {"SELECTED", "OTHER_WINNER", "ABSTAIN", "USER_DISABLED", "CERT_INVALID",
                         "DEADLINE_MISSED", "MISSING_SOURCE", "NOT_RUN"}, "unknown choice terminal")
        choice_policy = pick["selected_policy"]
        need((verdict == "SELECTED" and choice_policy == candidate) or
             (verdict == "OTHER_WINNER" and isinstance(choice_policy, str) and choice_policy and choice_policy != candidate) or
             (verdict not in {"SELECTED", "OTHER_WINNER"} and choice_policy is None), "wrong selected owner")
        decision, trade = books["decisions"].get(k), books["trades"].get(k)
        is_real = trade is not None and trade["policy"] == candidate
        if decision and decision["action_type"] == "BET":
            need(choice_policy and decision["policy"] == choice_policy and
                 verdict in {"SELECTED", "OTHER_WINNER"}, "BET conflicts with the recorded choice")
            if decision["policy"] == candidate:
                action = decision["action"]
                need(isinstance(action, list) and len(action) == 4 and
                     all(type(x) is int and 1 <= x <= 10 for x in action) and len(set(action[1:])) == 3,
                     "invalid authorized K3")
                bound = dict(policy=candidate, certificate_hash=certificate, model_hash=model, action=action,
                             proof_hash=pick["proof_hash"])
                match(pick, dict(bound, user_enabled=True), "selection before BET")
                match(decision, dict(bound, lane="CONTROLLED_PROBE"), "authorized BET")
                auth_time = instant(decision["authorized_at"])
                need(hashed(decision["authorization"]) and hashed(pick["proof_hash"]) and
                     isinstance(decision["decision_id"], str) and decision["decision_id"].strip() and
                     issued <= instant(pick["frozen_at"]) and
                     op <= instant(pick["frozen_at"]) <= auth_time <= snapshot and
                     auth_time < min(deadline, expires, revoked), "unbound or late authorization")
                if trade is None:
                    missing_authorized.append(k)
        if trade is not None:
            trade_id = trade["trade_id"]
            need((type(trade_id) is int and trade_id > 0) and trade_id not in seen_ids
                 and decision and decision["action_type"] == "BET" and trade["role"] == "REAL_TRADE" and
                 trade["policy"] == choice_policy and decision["policy"] == choice_policy and
                 trade["decision_id"] == decision["decision_id"] and trade["authorization"] == decision["authorization"],
                 "wrong, duplicate or unbound real trade")
            seen_ids.add(trade_id)
        acts.append({"target_key": k, "real_act": is_real})
        if not is_real:
            need(k not in books["exact"] and k not in books["probabilities"], "orphan candidate exact receipt")
            if verdict == "SELECTED":
                missing.append(k)
            continue
        auth = trade["authorization"]
        action = trade["action"]
        # The decision's K3 and certificate were authenticated above even if no trade existed.
        need(action == decision["action"], "booked K3 differs from authorized BET")
        bound = dict(policy=candidate, certificate_hash=certificate, model_hash=model, action=action,
                     proof_hash=pick["proof_hash"])
        for table in ("exact", "probabilities"):
            need(k in books[table], "missing deployed " + table)
            match(books[table][k], dict(bound, authorization=auth, trade_id=trade_id), table)
        prob = books["probabilities"][k]
        need(type(prob["selected_probability"]) in (float,int) and 0 < prob["selected_probability"] < 1 and
             prob["calibration_hash"] == calibration["calibrationSha256"], "missing or wrong exact probability")
        frozen, authorized, committed = [instant(t) for t in (pick["frozen_at"], decision["authorized_at"],trade["committed_at"])]
        need(op <= frozen <= authorized <= committed < min(deadline, expires, revoked) and
             issued <= frozen and committed <= snapshot, "expired, revoked or late deployed choice")
        draw, outcome = books["draws"].get(k), books["settlements"].get(k)
        if draw is None:
            need(outcome is None, "settlement without draw")
            pending_draw.append(k)
            continue
        d = draw["numbers"]
        need(draw["source_id"] == cal["draw_source_id"] and isinstance(d, list) and len(d) == 10 and
             all(type(x) is int for x in d) and set(d) == set(range(1,11)) and
             deadline <= instant(draw["draw_at"]) <= snapshot, "invalid authoritative issue")
        if outcome is None:
            unsettled_after_draw.append(k)
            continue
        hit = d[action[0]-1] in action[1:]
        match(outcome, dict(role="REAL_TRADE", policy=candidate, trade_id=trade_id, authorization=auth,
                            hit=hit, draw_id=draw["draw_id"]), "real settlement")
        need(instant(draw["draw_at"]) <= instant(outcome["settled_at"]) <= snapshot, "early real settlement")
        hit_count += hit
        settled += 1
    return dict(rows=acts, real_acts=sum(r["real_act"] for r in acts), settled=settled, hits=hit_count,
                missing_selected=missing, missing_authorized=missing_authorized, pending_draw=pending_draw,
                unsettled_after_draw=unsettled_after_draw, pending_settlement=sorted(pending_draw + unsettled_after_draw))


def verify_q02_q07(result, q02, q07, deployment):
    """Run only on a completed Q01–Q08 candidate projection, after that candidate's natural V1 is verified."""
    need(deployment is not None, "Q02 lacks actual deployment originals")
    rows = deployment["rows"]
    observed = q02["deployed_opportunities"]
    need(rows == observed and all(type(r.get("real_act")) is bool for r in observed),
         "Q02 booleans differ from actual candidate trades")
    rate = deployment["real_acts"] / len(rows)
    need(len(rows) >= 250 and rate >= .15 and math.isclose(q02["values"]["real_act_rate"], rate,
         rel_tol=0, abs_tol=1e-12), "Q02 deployment rate mismatch")
    need(result["issuance_status"] == "ACTIVE" and result["verdict"] == "QUALIFIED" and
         q07["scope"] == "ONE_REGISTERED_CANDIDATE" and q07["qualified_candidate_hashes"] == [result["candidate_hash"]]
         and type(q07["values"]["independently_qualified_assets"]) is int and
         q07["values"]["independently_qualified_assets"] == 1, "Q07 candidate-level asset count mismatches issuance")
    need(deployment["settled"] > 0, "No settled natural mainline use for Q07")
