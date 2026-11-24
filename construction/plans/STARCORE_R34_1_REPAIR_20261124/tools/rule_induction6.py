"""Reconstruct a reusable typed rule and check its independent development applications."""
import hashlib
import json

from continuity4 import digest, need, number, text
from diversity6 import referenced
from mechanism_probe6 import parse

CASE = "R341-B08-04-A01"


def typed(node):
    if node.op in ("freq", "trans1", "trans2"):
        return "PROB"
    if node.op == "gap":
        return "RISK"
    if node.op in ("cross", "repeat"):
        return "FLAG"
    if node.op == "c":
        return "CONST"
    if node.op == "mix":
        a, b = map(typed, node.kids)
        return a if a == b else "SCORE"
    if node.op in ("add", "sub"):
        a, b = map(typed, node.kids)
        if a == "CONST": return b
        if b == "CONST" or a == b: return a
    return "SCORE"


def literal(node):
    return [node.op, list(node.args), [literal(c) for c in node.kids]]


def pattern(a, b, slots):
    if a == b:
        return literal(a)
    need(a.shape() != b.shape(), "numeric variation is not a new rule slot")
    if a.op == b.op and a.args == b.args:
        return [a.op,list(a.args),[pattern(x,y,slots) for x,y in zip(a.kids,b.kids)]]
    need(typed(a) == typed(b) and typed(a) != "CONST", "incompatible or constant slot")
    slot = ["SLOT",len(slots),typed(a)]
    slots.append(slot)
    return slot


def matches(rule, node):
    if rule[0] == "SLOT":
        return typed(node) == rule[2] and bool(node.leaves())
    return (node.op == rule[0] and list(node.args) == rule[1] and len(node.kids) == len(rule[2])
            and all(matches(r, c) for r,c in zip(rule[2],node.kids)))


def mechanism(rule):
    if rule[0] == "SLOT":
        return ["SLOT",rule[2]]
    op = "cross" if rule[0] == "repeat" else rule[0]
    kids = [mechanism(child) for child in rule[2]]
    if op in ("add","mul"):
        kids.sort(key=lambda c: json.dumps(c, separators=(",",":")))
    return [op,kids]


def canonical_hash(rule):
    return hashlib.sha256(json.dumps(rule,sort_keys=True,separators=(",",":")).encode()).hexdigest()


def check_induction(run, receipt, originals, runner_source):
    marker = "STARCORE_RULE_PROBE "+CASE+" "
    markers = [line.split(marker,1)[1].strip() for line in runner_source.splitlines() if marker in line]
    need(len(markers) == 1, "missing committed rule test registration")
    registration = json.loads(markers[0])
    name = run["rule6_index_artifact"]
    index = originals.json(name)
    android = originals.json(run["instrumentation_artifact"])
    scope = dict(scope="ENGINEERING_ONLY", case_id=CASE, source_commit=receipt["source_after"],
                 r30Sandbox=android["r30Sandbox"], database_instance=index["database_instance"])
    need(text(scope["database_instance"]) and all(index[k] == v for k,v in scope.items()), "wrong rule execution identity")
    train, transfer = registration["discovery_pin"], registration["transfer_pin"]
    split = registration["discovery_through"]
    need(digest(train) and digest(transfer) and train != transfer and number(split)
         and number(registration["transfer_through"]) and registration["transfer_through"] > split,
         "invalid independent development partitions")

    def source(ref):
        return originals.json(referenced(originals, name, ref))

    used = set()
    def read_event(ref, kind, pin):
        ev = source(ref)
        key = (ev["source_table"], ev["durable_key"])
        need(all(ev[k] == v for k,v in scope.items()) and ev["kind"] == kind and ev["pin"] == pin
             and ev["role"] == "DEVELOPMENT" and all(map(text,key)) and key not in used
             and number(ev["committed_at"]), "foreign, duplicate or invalid rule event")
        used.add(key)
        return ev

    def program(ref, pin):
        row = read_event(ref, "EVALUATED_FORMULA", pin)
        canon = row["canonical"]
        policy = source(canon)
        need(row["policy_hash"] == canon["sha256"] and policy["schema"] == "R27_POLICY_SPEC_1"
             and policy["selector"]["k"] == 3 and policy["selector"]["position"] == registration["target_position"],
             "wrong rule program input or target")
        measured = read_event(row["evaluation"], "REPLAY_OR_TRAIN", pin)
        need(measured["policy_hash"] == row["policy_hash"] and number(measured["support"]) and measured["support"] > 0
             and number(row["born_at"]) and number(row["first_ordinal"]) and number(row["last_ordinal"])
             and row["born_at"] <= measured["committed_at"] <= row["committed_at"]
             and digest(measured["output"]["sha256"]), "formula was not subsequently evaluated")
        referenced(originals,name,measured["output"])
        return row,parse(policy["model"]["ast"])

    catalog = read_event(index["catalog_before"], "RULE_REGISTRY", train)
    known = registration["preexisting_rule_shapes"]
    need(isinstance(known,list) and all(digest(h) for h in known) and catalog["known_rule_shapes"] == known
         and text(catalog["source_query"]), "unverified initial registered and archived rule structures")
    examples = [program(ref,train) for ref in index["discovery"]]
    need(len(examples) == 2 and examples[0][0]["policy_hash"] != examples[1][0]["policy_hash"]
         and all(0 <= r["first_ordinal"] <= r["last_ordinal"] <= split for r,_ in examples),
         "discovery requires two executed development examples")
    slots = []
    rule = pattern(examples[0][1],examples[1][1],slots)
    rid = canonical_hash(rule)
    published = read_event(index["publication"], "PUBLISH_RULE", train)
    need(rule[0] != "SLOT" and slots and rid == published["rule_id"] and published["pattern"] == rule
         and published["structure_hash"] == canonical_hash(mechanism(rule))
         and published["structure_hash"] not in known and catalog["committed_at"] <= min(r["born_at"] for r,_ in examples)
         and published["example_hashes"] == [r["policy_hash"] for r,_ in examples]
         and max(r["committed_at"] for r,_ in examples) <= published["committed_at"],
         "no new reusable common executable structure")
    shapes = {n.shape() for _,n in examples}
    uses = [program(ref,transfer) for ref in index["transfer"]]
    need(len(uses) >= 2, "too few independent rule applications")
    for row, ast in uses:
        need(matches(rule,ast) and row["rule_id"] == rid and row["publication_sha256"] == index["publication"]["sha256"]
             and published["committed_at"] <= row["born_at"] and split < row["first_ordinal"] <= row["last_ordinal"] <= registration["transfer_through"]
             and ast.shape() not in shapes, "unmatched, reused or parameter-only application on transfer block")
        shapes.add(ast.shape())
    restored = read_event(index["restore"], "RESTORE_RULE", transfer)
    need(restored["rule_id"] == rid and restored["publication_sha256"] == index["publication"]["sha256"] and
         text(restored["old_owner"]) and text(restored["new_owner"]) and restored["old_owner"] != restored["new_owner"]
         and any(r["worker_owner"] == restored["old_owner"] and r["committed_at"] <= restored["committed_at"] for r,_ in uses)
         and any(r["worker_owner"] == restored["new_owner"] and r["born_at"] >= restored["committed_at"] for r,_ in uses),
         "published rule not reused across worker re-open")
    control = read_event(index["disabled_control"],"RULE_DISABLED_DISPATCH",transfer)
    need(control["disabled_rule"] == rid and isinstance(control["dispatched_rules"],list)
         and number(control["opportunities"]) and control["opportunities"] > 0 and rid not in control["dispatched_rules"] and text(registration["seed"])
         and number(registration["budget_units"]) and registration["budget_units"] > 0
         and all(r["budget_units"] == control["budget_units"] == registration["budget_units"]
                 and r["seed"] == control["seed"] == registration["seed"] for r,_ in uses)
         and control["first_ordinal"] == split+1 and control["last_ordinal"] == registration["transfer_through"],
         "wrong disabled-rule control, budget, seed or period")
    return {"status":"PASS_DEVELOPMENT_RULE_REUSE","slots":len(slots),"applications":len(uses)}
