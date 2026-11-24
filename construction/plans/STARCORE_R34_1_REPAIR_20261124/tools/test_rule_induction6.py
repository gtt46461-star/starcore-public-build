import json
import tempfile
import unittest
from pathlib import Path

from rule_induction6 import CASE, canonical_hash, check_induction
from test_diversity6 import policy
from test_joined_lineage5 import put, sha
from verify_receipt import Originals


def fixture(root, fault=None):
    scope = dict(source_commit="a"*40,case_id=CASE,r30Sandbox="r341-fixture",database_instance="db",
                 scope="ENGINEERING_ONLY")
    a, b = "1"*64, "2"*64
    rule = ["zscore",[],[["SLOT",0,"PROB"]]]
    rid = canonical_hash(rule)
    sh = canonical_hash(["zscore",[["SLOT","PROB"]]])
    known = [sh] if fault=="existing_pattern" else []
    reg = dict(discovery_pin=a,transfer_pin=b,discovery_through=120,transfer_through=180,
               preexisting_rule_shapes=known,target_position=1,seed="seed-set",budget_units=100)
    statement = "// STARCORE_RULE_PROBE "+CASE+" "+json.dumps(reg)
    def event(name,kind,pin,stamp,**fields):
        return put(root,name,{**scope,"kind":kind,"pin":pin,"role":"DEVELOPMENT","source_table":"fixture",
                              "durable_key":name,"committed_at":stamp,**fields})
    registry = event("reg","RULE_REGISTRY",a,0,source_query="fixture source query",known_rule_shapes=known)
    refs, hashes = [], []
    asts = ["(zscore (freq 1 8))","(zscore (trans1))","(zscore (trans2))",
            "(zscore (mix 0.4 (freq 1 8) (trans1)))"]
    if fault=="only_tuning": asts[1]="(zscore (freq 1 32))"
    if fault=="reuse_development": asts[2]=asts[0]
    if fault=="wrong_grammar": asts[2]="(rank (trans2))"
    if fault=="two_parameter_children": asts[2]="(zscore (mix 0.9 (freq 1 8) (trans1)))"
    for i,ast in enumerate(asts):
        domain = a if i<2 else b
        if fault=="foreign_pin" and i==2: domain=a
        canon = put(root,"program"+str(i),policy(ast))
        asset = canon["sha256"]
        hashes.append(asset)
        image = put(root,"replay"+str(i),b"measured"+bytes([i]))
        at = i*10 + 4
        evaluation=event("eval"+str(i),"REPLAY_OR_TRAIN",domain,at,output=image,
                         support=0 if fault=="no_training" and i==3 else 12,policy_hash=asset)
        attrs = dict(canonical=canon, policy_hash=asset, evaluation=evaluation, born_at=at-1,
                     first_ordinal=10 if i<2 else 121, last_ordinal=119 if i<2 else 180,
                     worker_owner="worker2" if i==3 else "worker1", seed="seed-set",budget_units=100)
        if fault=="overlap" and i==3: attrs["first_ordinal"]=120
        if i>=2: attrs.update(rule_id=rid, publication_sha256=pub["sha256"])
        refs.append(event("row"+str(i),"EVALUATED_FORMULA",domain,at+1,**attrs))
        if i==1:
            pub=event("publication","PUBLISH_RULE",a,17,rule_id=rid,structure_hash=sh,
                      example_hashes=list(hashes),pattern=["fake"] if fault=="fabricated_rule" else rule)
    restore = event("restore","RESTORE_RULE",b,28,rule_id=rid,publication_sha256=pub["sha256"],
                    old_owner="worker1",new_owner="worker1" if fault=="no_restore" else "worker2")
    ctrl = event("control","RULE_DISABLED_DISPATCH",b,40,disabled_rule=rid,dispatched_rules=[rid] if fault=="enabled_control" else [],
                 opportunities=60,first_ordinal=121,last_ordinal=180,seed="seed-set",budget_units=50 if fault=="unequal_budget" else 100)
    put(root,"index",{**scope,"discovery":refs[:2],"transfer":refs[2:],"publication":pub,"catalog_before":registry,
                      "restore":restore, "disabled_control":ctrl})
    put(root,"android",{"r30Sandbox":"other" if fault=="foreign_test" else scope["r30Sandbox"]})
    originals=Originals(root,[{"path":f.name,"sha256":sha(f.read_bytes())} for f in root.iterdir()])
    return check_induction({"rule6_index_artifact":"index","instrumentation_artifact":"android"},
                           {"source_after":scope["source_commit"]},originals,statement)


class RuleTests(unittest.TestCase):
    def test_two_development_blocks_and_restored_owner(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(fixture(Path(tmp))["applications"],2)

    def test_rejections(self):
        for fault in ("only_tuning","reuse_development","wrong_grammar","two_parameter_children","foreign_pin",
                      "no_training","overlap","fabricated_rule","no_restore","enabled_control","unequal_budget",
                      "foreign_test","existing_pattern"):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as tmp, self.assertRaises(ValueError):
                fixture(Path(tmp),fault)


if __name__=="__main__":
    unittest.main()
