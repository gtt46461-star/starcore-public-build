"""Check a controlled multi-generation integration trace against its evidence records."""
import argparse
import hashlib
import json
import re
from pathlib import Path


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def digest(value, n=64):
    return isinstance(value, str) and re.fullmatch("[a-f0-9]{" + str(n) + "}", value) is not None


def original_bytes(root, ref, stage):
    name = Path(ref["path"])
    require(not name.is_absolute() and ".." not in name.parts, f"{stage}: invalid reference")
    file = (root / name).resolve()
    require(file.is_relative_to(root) and file.is_file(), f"{stage}: original missing")
    data = file.read_bytes()
    require(hashlib.sha256(data).hexdigest() == ref["sha256"], f"{stage}: digest mismatch")
    return data


def read_record(root, ref, stage, scope):
    doc = json.loads(original_bytes(root, ref, stage))
    require(all(doc[k] == v for k, v in scope.items()) and doc["stage"] == stage, f"{stage}: wrong scope or stage")
    return doc


def verify(index, directory):
    root = directory.resolve()
    scope = {k: index[k] for k in ("scope", "run_id", "source_commit", "apk_sha256", "time_scope")}
    require(scope["scope"] == "ENGINEERING_FIXTURE" and scope["run_id"] and
            isinstance(scope["time_scope"], str) and scope["time_scope"].strip() and
            digest(scope["source_commit"], 40) and digest(scope["apk_sha256"]), "Unbound engineering trace")
    cycles, births = index["cycles"], index["births"]
    require(len(cycles) >= 3 and len(births) + 1 == len(cycles), "Two births and consuming generations required")
    seen_assets, feedback_keys, weight_hashes = set(), set(), set()
    previous_target = -1
    next_asset = None
    prior_birth = None
    prior_child_original = None
    prior_born_at = None
    for number, cycle in enumerate(cycles):
        stages = ("train", "publish", "freeze", "settle", "feedback", "research")
        records = {s: read_record(root, cycle[s], s, scope) for s in stages}
        train, pub, freeze, settle, feedback, research = [records[s] for s in stages]
        asset, target = train["policy_hash"], freeze["target_ordinal"]
        require(digest(asset) and asset not in seen_assets and (next_asset is None or asset == next_asset),
                f"generation {number}: wrong or reused child")
        seen_assets.add(asset)
        formula = read_record(root, train["formula_original"], "formula_original", scope)
        formula_digest = train["formula_original"]["sha256"]
        require(formula["policy_hash"] == asset and formula["program"] and train["birth_ref"] == prior_birth
                and (prior_child_original is None or formula_digest == prior_child_original),
                f"train {target}: wrong birth or executable formula original")
        model_artifact = read_record(root, train["state_original"], "model_state", scope)
        require(model_artifact["policy_hash"] == asset and model_artifact["optimizer_after"] == train["optimizer_after"]
                and model_artifact["model_hash"] == pub["model_hash"], f"train {target}: unbound model state")
        weights_ref = model_artifact["weights_original"]
        weights = original_bytes(root, weights_ref, "weights")
        weight_hash = weights_ref["sha256"]
        require(weights and weight_hash not in weight_hashes and
                weight_hash == pub["weights_sha256"] == freeze["weights_sha256"],
                f"train {target}: reused or substituted controlled weights")
        weight_hashes.add(weight_hash)
        require(type(target) is int and target > previous_target and all(x["policy_hash"] == asset for x in records.values()),
                f"generation {number}: mixed asset or target order")
        require(type(train["trained_through"]) is int and train["trained_through"] < target and
                train["sample_role"] == "TRAIN" and type(train["sample_count"]) is int and train["sample_count"] > 0
                and digest(train["optimizer_before"]) and digest(train["optimizer_after"]) and
                train["optimizer_before"] != train["optimizer_after"], f"train {target}: samples, PIT or optimizer")
        require(pub["train_ref"] == cycle["train"]["sha256"] and digest(pub["model_hash"]) and
                freeze["publication_ref"] == cycle["publish"]["sha256"] and freeze["model_hash"] == pub["model_hash"]
                and freeze["role"] == "SHADOW", f"freeze {target}: publication not consumed")
        require(all(r["target_ordinal"] == target for r in (settle, feedback, research)), f"target {target}: crossed originals")
        event_times = [train["started_at"], train["committed_at"], pub["published_at"], freeze["returned_at"],
                       freeze["deadline_at"], settle["draw_at"], settle["settled_at"], feedback["committed_at"],
                       research["completed_at"]]
        require(all(type(t) is int and t >= 0 for t in event_times) and event_times == sorted(event_times)
                and freeze["returned_at"] < freeze["deadline_at"], f"target {target}: event out of order or late")
        require(prior_born_at is None or prior_born_at <= train["started_at"],
                f"train {target}: child started before parent research and birth committed")
        pos, pick, draw = freeze["position"], freeze["numbers"], settle["draw"]
        require(type(pos) is int and 1 <= pos <= 10 and len(pick) == 3 and all(type(n) is int and 1 <= n <= 10 for n in pick)
                and len(set(pick)) == 3 and len(draw) == 10 and all(type(n) is int for n in draw)
                and set(draw) == set(range(1, 11)), f"settle {target}: invalid K3/PK10")
        require(settle["freeze_ref"] == cycle["freeze"]["sha256"] and settle["hit"] is (draw[pos-1] in pick),
                f"settle {target}: wrong original or label")
        key = feedback["event_key"]
        require(key and key not in feedback_keys and type(feedback["committed_count"]) is int and feedback["committed_count"] == 1
                and feedback["settlement_ref"] == cycle["settle"]["sha256"], f"feedback {target}: duplicated or not linked")
        feedback_keys.add(key)
        require(research["feedback_ref"] == cycle["feedback"]["sha256"] and digest(research["artifact_hash"])
                and type(research["support"]) is int and research["support"] > 0 and research["role"] == "DEVELOPMENT",
                f"research {target}: no supported downstream product")
        if number < len(births):
            birth = read_record(root, births[number], "birth", scope)
            operation = "MUTATION" if number == 0 else "FUSION"
            parents = birth["parents"]
            require(birth["research_ref"] == cycle["research"]["sha256"] and birth["operation"] == operation
                    and isinstance(parents,list) and len(parents) == (1 if operation == "MUTATION" else 2)
                    and len(set(parents)) == len(parents) and all(digest(k) for k in parents) and asset in parents,
                    f"birth {number}: operation or parent lineage")
            originals = [read_record(root, ref, "formula_original", scope) for ref in birth["parent_originals"]]
            require([doc["policy_hash"] for doc in originals] == parents and all(doc["program"] for doc in originals),
                    f"birth {number}: missing original parents")
            require(birth["parent_originals"][parents.index(asset)]["sha256"] == formula_digest,
                    f"birth {number}: current parent formula substituted")
            require(type(birth["committed_at"]) is int and birth["committed_at"] >= research["completed_at"],
                    f"birth {number}: child born before research completed")
            prior_born_at = birth["committed_at"]
            prior_birth = births[number]["sha256"]
            prior_child_original = birth["child_original"]["sha256"]
            child = read_record(root, birth["child_original"], "formula_original", scope)
            require(child["policy_hash"] == birth["child_hash"] and child["program"] and
                    all(doc["program"] != child["program"] for doc in originals), f"birth {number}: reused program")
            next_asset = birth["child_hash"]
            require(digest(next_asset) and next_asset not in parents and next_asset not in seen_assets
                    and digest(birth["parent_behavior"]) and digest(birth["child_behavior"])
                    and birth["parent_behavior"] != birth["child_behavior"], f"birth {number}: no demonstrated change")
        previous_target = target
    return {"status":"PASS_FIXTURE_TRACE_LINKS","cycles":len(cycles),"births":len(births),"field_acceptance":"NOT_EVALUATED"}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--index", type=Path, required=True)
    p.add_argument("--evidence-root", type=Path, required=True)
    args = p.parse_args()
    try:
        print(json.dumps(verify(json.loads(args.index.read_bytes()), args.evidence_root)))
    except (ValueError, KeyError, OSError, TypeError) as e:
        p.exit(1, f"FAIL_TRACE: {e}\n")


if __name__ == "__main__":
    main()
