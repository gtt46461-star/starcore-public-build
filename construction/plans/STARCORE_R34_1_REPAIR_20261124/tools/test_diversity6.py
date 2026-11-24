"""Exercise v6 on the same synthetic service, training and birth records that pass v3–5."""
import random
import tempfile
import unittest
from pathlib import Path

from continuity4 import IDENTITY, verify as verify_continuity
from diversity6 import check_diversity
from joined_lineage5 import check_joined_lineage
from mechanism_probe6 import Node, actions, parse, replay, scores
from test_joined_lineage5 import dump, make, put, sha
from test_learning_trace import hashed
from verify_learning_trace import verify as verify_lineage


def policy(ast):
    tree = parse(ast)
    terms = []
    def visit(node):
        terms.append(node)
        for k in node.kids:
            visit(k)
    visit(tree)
    inputs = {"leaves":sorted({n.op for n in terms if n.op != "c" and not n.kids}),
              "windows":sorted({n.args[1] for n in terms if n.op == "freq"}), "missing":"SKIPPED_GAP"}
    return dict(schema="R27_POLICY_SPEC_1", inputs=inputs, model={"ast":ast},
                readout=dict(temperature=1.0, fitTemperature=True, surface="TARGET_POSITION_SOFTMAX_OTHERS_UNIFORM"),
                selector=dict(position=1, rule="MARGIN", threshold=0.0, rate=0.3, k=3),
                updater=dict(lr=0.02, forget=0.0, rule="PER_NUMBER_BIAS_SGD_V1"))


class Witness:
    def __init__(self, fault):
        self.fault = fault
        rnd = random.Random(61)
        history = [rnd.sample(range(1,11), 10) for _ in range(64)]
        self.panel = {"role":"TRAIN", "first_ordinal":100, "probe_from_index":32, "target_position":1,
                      "draws":history, "root_slots":[0,2,4,6], "fixed_seed_asts":["(freq 1 8)", "(gap 1)", "(trans1)"]}
        base, donor, third = "(freq 1 8)", "(gap 1)", "(trans1)"
        child = "(add (rank "+base+") (rank (cross 2 1)))"
        if fault == "parameter_as_mutation": child = "(freq 1 32)"
        fused = "(add (rank "+child+") (rank "+donor+"))"
        if fault == "zero_parent": fused = "(add (rank "+child+") (mul (c 0.0) (rank "+donor+")))"
        if fault == "unrelated_child": fused = "(cross 3 1)"
        fourth = "(add (rank (cross 3 1)) (rank (trans2)))"
        if fault == "fourth_is_tune": fourth = "(freq 1 32)"
        if fault == "same_votes": fourth = "(add "+base+" (mul (c 0.0) (cross 3 1)))"
        if fault == "old_seed_relabel": self.panel["fixed_seed_asts"].append(fourth)
        if fault == "late_history": self.panel["first_ordinal"] = 951
        if fault == "bad_draw": history[1] = [2]*10
        self.programs = list(map(policy, [base, child, fused, donor, third, fourth]))
        self.ids = [sha(dump(p)) for p in self.programs]

    def bind_programs(self, docs):
        # Previous fixtures deliberately use opaque asset names and arbitrary program tokens.
        mapping = {hashed("asset"+str(i)):self.ids[i] for i in range(4)}
        def rewrite(item):
            if isinstance(item, dict):
                return {k:rewrite(v) for k,v in item.items()}
            if isinstance(item, list):
                return [rewrite(v) for v in item]
            return mapping.get(item,item) if isinstance(item,str) else item
        for name, doc in list(docs.items()):
            docs[name] = rewrite(doc)
        for i, program in enumerate(self.programs[:4]):
            docs["formula"+str(i)]["program"] = program

    def bind_service(self, base, stream, ledger, events):
        root = base/"mechanism"
        root.mkdir()
        scope = {k:stream[k] for k in IDENTITY}
        scope.update(case_id="A-B12-03b", database_instance="db1", scenario_sha256=stream["scenario"]["sha256"])
        rows, start, pos = self.panel["draws"], self.panel["probe_from_index"], self.panel["target_position"]
        bench = (rows,start,pos)
        initial = dict(**scope, source_query="fixture:scoped birth query", registry_version="fixture/6",
                       observed_ms=1, existing_born_keys=[], fixed_seed_asts=self.panel["fixed_seed_asts"])
        if self.fault == "hidden_archive": initial["existing_born_keys"]=[self.ids[0]]
        if self.fault == "omitted_seed": initial["fixed_seed_asts"]=[]
        ledger["mechanism_seed_snapshot"] = put(base/"stream","initial-catalog",initial)
        programs, cache = [], {}
        for i, (doc, asset) in enumerate(zip(self.programs,self.ids)):
            tree = parse(doc["model"]["ast"])
            vectors = [list(r) for r in replay(tree,bench,cache)]
            if self.fault == "invented_score" and i==1: vectors[0][0] += 0.9
            if self.fault == "omit_probe" and i==1: vectors.pop()
            source = dict(**scope, role="TRAIN", policy_hash=asset, score_rows=vectors)
            if self.fault == "future_role" and i==1: source["role"] = "FUTURE"
            if i==1:
                source["drop_graft"] = replay(Node("rank", kids=(parse(self.programs[0]["model"]["ast"]),)),bench,cache)
            if i==2:
                source["drop_parent"] = {}
                for left,right in ((1,3),(3,1)):
                    other = Node("rank", kids=(parse(self.programs[right]["model"]["ast"]),))
                    source["drop_parent"][self.ids[left]] = replay(other,bench,cache)
                if self.fault == "false_ablation": source["drop_parent"][self.ids[3]]=source["score_rows"]
            programs.append({"canonical":put(root,f"policy{i}",doc),"scores":put(root,f"scores{i}",source)})
        if self.fault == "wrong_asset":
            programs[0]["canonical"]["sha256"] = "1"*64
        put(root,"index",{**scope, "formulas":programs})
        for stage_no, prog_index in enumerate((0,3,4,5)):
            i = 2*stage_no
            service = base/"stream"
            create = events[i,"create",ledger["rows"][i]["issue"]]
            asset = self.ids[prog_index]
            create.update(database_instance="db1", policy_hash=asset, mechanism_parents=[],
                          origin_kind="GENERATED_ROOT", generator_method="fixture.Generator.generate",
                          effect=put(service,f"root-formula{i}",self.programs[prog_index]),committed_ms=i*200+12)
            if self.fault == "implicit_parent" and i==6: create["mechanism_parents"]=[self.ids[0]]
            if self.fault == "imported_root" and i==4: create["origin_kind"]="RESTORED"
            if self.fault == "foreign_db" and i==6: create["database_instance"]="db2"
            if self.fault == "wrong_create_effect" and i==2: create["effect"]=put(service,"foreign-root",policy("(cross 6 2)"))
            tr = events[i+1,"train",ledger["rows"][i+1]["issue"]]
            tr.update(database_instance="db1",policy_hash=asset, before_state=put(service,f"before{i}",b"old"+bytes([i])))
            if self.fault == "silent_root" and i==6: del tr["before_state"]
            if self.fault == "no_update" and i==6: tr["before_state"] = tr["effect"]


class DiversityTests(unittest.TestCase):
    def run_witness(self, fault=None):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            stream, learning, run, receipt, originals = make(root, witness=Witness(fault))
            self.assertEqual(verify_continuity(stream,root/"stream")["periods"],8)
            self.assertEqual(verify_lineage(learning,root/"learning")["cycles"],3)
            self.assertEqual(check_joined_lineage(run,receipt,originals)["linked_commits"],20)
            run["mechanism6_index_artifact"] = "mechanism/index"
            return check_diversity(run,receipt,originals)

    def test_combined(self):
        result = self.run_witness()
        self.assertEqual((result["root_births"],result["pure_mechanisms"],result["probe_rows"]),(4,3,32))

    def test_previous_pass_new_rejections(self):
        faults = {"parameter_as_mutation":"parameter-only", "zero_parent":"fusion lacks",
                  "unrelated_child":"fusion lacks", "fourth_is_tune":"repeated structure",
                  "same_votes":"near-identical root", "old_seed_relabel":"post-restart",
                  "late_history":"late TRAIN", "bad_draw":"PK10", "future_role":"probe role",
                  "omit_probe":"calendar", "invented_score":"numerical", "false_ablation":"numerical",
                  "wrong_asset":"original", "implicit_parent":"root is reused", "imported_root":"root is reused",
                  "foreign_db":"foreign service", "wrong_create_effect":"unbound", "silent_root":"lacks subsequent",
                  "no_update":"did not update", "hidden_archive":"birth scope", "omitted_seed":"seed catalog"}
        for case, reason in faults.items():
            with self.subTest(case=case), self.assertRaisesRegex(ValueError,reason):
                self.run_witness(case)

    def test_reference_numbers_and_future_independence(self):
        history = [list(range(1,11)),list(range(2,11))+[1]]*4
        history += [list(range(1,11)),list(range(2,11))+[1]]
        at = 8
        def calc(ast): return scores(parse(ast),history,at,1,{})
        self.assertEqual(calc("(freq 1 8)")[:3], [0.5,0.5,0.0])
        self.assertEqual(calc("(gap 1)")[:3], [0.02,0.01,1.0])
        self.assertEqual(calc("(cross 2 1)"), [0,0,1,0,0,0,0,0,0,0])
        self.assertEqual(calc("(trans1)")[:2], [4/13,1/13])
        self.assertEqual(calc("(trans2)")[:2], [4/13,1/13])
        self.assertEqual(actions([calc("(rank (cross 2 1))")]),{(0,(3,9,10))})
        vectors = calc("(mix 0.5 (freq 1 8) (trans1))")
        history[at:] = [list(range(10,0,-1))]*2
        self.assertEqual(calc("(mix 0.5 (freq 1 8) (trans1))"), vectors)
        self.assertEqual(parse("(mix 0.1 (freq 1 8) (trans1))").shape(),parse("(mix 0.9 (freq 7 32) (trans1))").shape())
        with self.assertRaises(ValueError): parse("(cross 1 0)")


if __name__ == "__main__":
    unittest.main()
