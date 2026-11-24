"""Bind independent roots and effective parents to the B12 service and frozen TRAIN panel."""
from pathlib import PurePosixPath

from continuity4 import IDENTITY, need, text
from mechanism_probe6 import Node, actions, check_vectors, panel, parse, replay, separated


def referenced(originals, index_name, ref):
    path = PurePosixPath(ref["path"])
    need(not path.is_absolute() and path.as_posix() == ref["path"] and ".." not in path.parts
         and "\\" not in ref["path"], "invalid mechanism original path")
    name = (PurePosixPath(index_name).parent / path).as_posix()
    need(name in originals.files and originals.hashes[name] == ref["sha256"], "unregistered or changed mechanism original")
    return name


def check_diversity(run, receipt, originals):
    stream_name, trace_name, index_name = (run[k] for k in
        ("continuity_index_artifact", "learning_trace_index_artifact", "mechanism6_index_artifact"))
    stream, trace, index = (originals.json(n) for n in (stream_name, trace_name, index_name))

    def record(name, ref):
        return originals.json(referenced(originals, name, ref))

    schedule, ledger = (record(stream_name, stream[k]) for k in ("scenario", "ledger"))
    scope = {k: stream[k] for k in IDENTITY}
    scope.update(case_id="A-B12-03b", database_instance=ledger["database_instance"],
                 scenario_sha256=stream["scenario"]["sha256"])
    need(all(index.get(k) == v for k, v in scope.items()) and index["scope"] == "ENGINEERING_FIXTURE"
         and index["source_commit"] == receipt["source_after"] and trace["database_instance"] == scope["database_instance"],
         "mechanism panel differs from the installed service run")
    spec = schedule["mechanism_probe6"]
    bench = panel(spec, schedule["slots"][0]["ordinal"])
    root_slots = spec["root_slots"]
    ends = schedule["cycle_ends"]
    start = 0
    need(isinstance(root_slots, list) and len(root_slots) == len(ends) >= 4, "one registered root per controlled cycle")
    for end, index_in_calendar in zip(ends, root_slots):
        need(type(index_in_calendar) is int and start <= index_in_calendar <= end, "root outside registered cycle")
        start = end + 1

    programs, trees, signatures, measurements = {}, {}, {}, {}
    cache = {}
    need(isinstance(index["formulas"], list) and 1 <= len(index["formulas"]) <= 128, "invalid controlled panel size")
    for measurement in index["formulas"]:
        canon = measurement["canonical"]
        asset = canon["sha256"]
        doc = record(index_name, canon)
        obs = record(index_name, measurement["scores"])
        need(asset not in programs and doc.get("schema") == "R27_POLICY_SPEC_1"
             and set(doc) == {"schema","inputs","model","readout","selector","updater"}
             and type(doc["selector"]["position"]) is int and doc["selector"]["position"] == bench[2]
             and type(doc["selector"]["k"]) is int and doc["selector"]["k"] == 3, "duplicate, incomplete or different target policy")
        need(all(obs.get(k) == v for k, v in scope.items()) and obs["role"] == "TRAIN"
             and obs["policy_hash"] == asset, "unbound scorer or probe role")
        tree = parse(doc["model"]["ast"])
        rows = replay(tree, bench, cache)
        check_vectors(obs["score_rows"], rows)
        programs[asset], trees[asset] = doc, tree
        signatures[asset], measurements[asset] = actions(rows), obs

    events = {}
    trained = {}
    for i, row in enumerate(ledger["rows"]):
        for event in row["events"]:
            if event["kind"] != "COMMIT" or event["stage"] not in ("create", "train"):
                continue
            doc = record(stream_name, event["original"])
            asset = doc.get("policy_hash")
            if asset not in programs:
                continue
            need(doc["database_instance"] == scope["database_instance"] and doc["process"] == row["process"]
                 and all(doc[k] == v for k,v in scope.items() if k not in ("case_id", "database_instance"))
                 and doc["stage"] == event["stage"] and doc["issue"] == event["issue"], "foreign service owner or work key")
            if event["stage"] == "create" and i in root_slots and doc["issue"] == row["issue"]:
                need(i not in events and doc["mechanism_parents"] == [] and text(doc["generator_method"])
                     and doc["origin_kind"] == "GENERATED_ROOT" and
                     doc["effect"]["sha256"] == asset, "root is reused, derived or unbound to its canonical program")
                referenced(originals, stream_name, doc["effect"])
                events[i] = (asset, doc["committed_ms"])
            if event["stage"] == "train":
                # The two hashes name actual optimizer/weight bytes, measured before and after.
                if "before_state" in doc:
                    referenced(originals, stream_name, doc["before_state"])
                    referenced(originals, stream_name, doc["effect"])
                    need(doc["before_state"]["sha256"] != doc["effect"]["sha256"], "root training did not update state")
                    trained.setdefault(asset, []).append(doc["committed_ms"])

    need(set(events) == set(root_slots), "registered service root is missing")
    roots = [events[i][0] for i in root_slots]
    catalog = record(stream_name, ledger["mechanism_seed_snapshot"])
    need(all(catalog.get(k) == v for k, v in scope.items()) and text(catalog["source_query"])
         and text(catalog["registry_version"]) and type(catalog["observed_ms"]) is int
         and 0 <= catalog["observed_ms"] < min(stamp for _, stamp in events.values())
         and catalog["existing_born_keys"] == [] and isinstance(spec["fixed_seed_asts"], list)
         and catalog["fixed_seed_asts"] == spec["fixed_seed_asts"],
         "unverified seed catalog or nonempty controlled birth scope")
    seed_shapes = {parse(ast).shape() for ast in catalog["fixed_seed_asts"]}
    pure = set()
    post_composite = False
    for n, slot in enumerate(root_slots):
        asset, born = events[slot]
        tree = trees[asset]
        need(any(t >= born for t in trained.get(asset, [])), "root lacks subsequent training in the same service")
        for other in roots[:n]:
            need(tree.shape() != trees[other].shape() and separated(signatures[asset], signatures[other]),
                 "repeated structure or near-identical root behavior")
        if len(tree.leaves()) == 1:
            pure.update(tree.leaves())
        if slot > schedule["restart_after"] and len(tree.leaves()) >= 2 and tree.shape() not in seed_shapes:
            post_composite = True
    need(len(pure) >= 3 and post_composite, "insufficient independent mechanisms or no new post-restart composition")

    def trace_formula(ref):
        formula = record(trace_name, ref)
        asset = formula["policy_hash"]
        need(asset in programs and formula["program"] == programs[asset], "lineage formula differs from executable bytes")
        return asset

    first_training = record(trace_name, trace["cycles"][0]["train"])
    first = trace_formula(first_training["formula_original"])
    need(first in roots and events[root_slots[roots.index(first)]][1] <= first_training["started_at"],
         "trained lineage does not start at a new root")
    included = set(roots)
    born_at = {asset: stamp for asset, stamp in events.values()}
    for cycle in trace["cycles"]:
        tr = record(trace_name, cycle["train"])
        trained.setdefault(tr["policy_hash"], []).append(tr["committed_at"])
    edits = []
    for ref in trace["births"]:
        birth = record(trace_name, ref)
        child = trace_formula(birth["child_original"])
        parents = [trace_formula(r) for r in birth["parent_originals"]]
        need(birth["parents"] == parents and child == birth["child_hash"] and child not in born_at and
             all(p in born_at and born_at[p] <= birth["committed_at"] for p in parents), "unborn, substituted or reissued lineage")
        need(all(any(born_at[p] <= t <= birth["committed_at"] for t in trained.get(p, [])) for p in parents),
             "parent was not trained before consumption")
        tree = trees[child]
        need(all(tree.shape() != trees[p].shape() and separated(signatures[child], signatures[p]) for p in parents),
             "parameter-only, equivalent or behaviorally duplicate descendant")
        controls = measurements[child].get("drop_parent", {})
        if birth["operation"] == "MUTATION":
            # A discriminating witness: graft a fresh component onto the exact retained parent.
            need(len(parents) == 1 and tree.op == "add" and tree.kids[0] == Node("rank", kids=(trees[parents[0]],))
                 and tree.kids[1].op == "rank" and tree.kids[1].leaves() - trees[parents[0]].leaves(),
                 "no retained parent and effective new input mechanism in registered mutation")
            old = replay(tree.kids[0], bench, cache)
            check_vectors(measurements[child]["drop_graft"], old)
            need(separated(signatures[child], actions(old)), "added component has no measurable effect")
        else:
            need(birth["operation"] == "FUSION" and len(parents) == len(set(parents)) == 2
                 and tree.op == "add" and set(tree.kids) == {Node("rank", kids=(trees[p],)) for p in parents}
                 and trees[parents[0]].shape() != trees[parents[1]].shape()
                 and separated(signatures[parents[0]], signatures[parents[1]]), "fusion lacks two distinct executed parents")
            need(set(controls) == set(parents), "fusion lacks both parent ablations")
            for removed, other in ((parents[0], parents[1]), (parents[1], parents[0])):
                left = replay(Node("rank", kids=(trees[other],)), bench, cache)
                check_vectors(controls[removed], left)
                need(separated(signatures[child], actions(left)), "one listed parent has no effect")
        included.update(parents + [child])
        born_at[child] = birth["committed_at"]
        edits.append(birth["operation"])
    need(edits and edits[0] == "MUTATION" and "FUSION" in edits and set(programs) == included,
         "missing witnessed operation or unassigned formula")
    return {"status":"PASS_CONTROLLED_MECHANISM_BREADTH", "root_births":len(roots),
            "pure_mechanisms":len(pure), "probe_rows":len(bench[0])-bench[1], "natural_quality":"NOT_EVALUATED"}
