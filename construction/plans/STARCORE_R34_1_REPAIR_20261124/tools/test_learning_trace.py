"""Isolated synthetic trace probes; these never run the Android pipeline."""
import hashlib
import json
import struct
import tempfile
import unittest
import zipfile
from unittest.mock import patch
from pathlib import Path
from verify_learning_trace import verify
from verify_receipt import Originals, case_digest, check_result, sha


def hashed(tag):
    return hashlib.sha256(str(tag).encode()).hexdigest()


def fixture(root, fault=None, source_commit="a"*40, apk_sha256="b"*64):
    scope = dict(scope="ENGINEERING_FIXTURE", run_id="reference-test", source_commit=source_commit,
                 apk_sha256=apk_sha256, time_scope="device-boot-1/elapsed-ms")
    cycles, births = [], []

    def put_bytes(name, raw):
        (root / name).write_bytes(raw)
        return {"path": name, "sha256": hashlib.sha256(raw).hexdigest()}

    def put(name, stage, **value):
        raw = json.dumps({**scope, "stage": stage, **value}, sort_keys=True).encode()
        return put_bytes(name, raw)

    programs = [put(f"formula{i}", "formula_original", policy_hash=hashed(f"asset{i}"),
                    program=["fixture_formula", i]) for i in range(4)]
    for i in range(3):
        target, asset, model = 101+i, hashed(f"asset{i}"), hashed(f"model{i}")
        base = 0 if fault == "child_before_parent_settled" and i == 1 else i * 100
        pred = dict(policy_hash=asset, target_ordinal=target)
        cycle = {}
        role = "FUTURE" if fault == "label_leak" and i == 1 else "TRAIN"
        prior = hashed(f"optimizer{i}")
        post = prior if fault == "no_training" else hashed(f"updated{i}")
        weights = put_bytes(f"weights{i}", struct.pack("!d", 1.5 if fault == "reused_weights" else 1.5+i))
        weight_hash = weights["sha256"]
        state = put(f"model{i}", "model_state", policy_hash=asset, optimizer_after=post, model_hash=model,
                    weights_original=weights)
        birth_ref = births[-1]["sha256"] if i else None
        if fault == "swapped_birth" and i == 2:
            birth_ref = births[0]["sha256"]
        train_formula = programs[i]
        if fault == "swapped_child_program" and i == 1:
            train_formula = put("other-formula", "formula_original", policy_hash=asset, program=["other", i])
        cycle["train"] = put(f"train{i}", "train", policy_hash=asset, trained_through=target-1, sample_count=5,
                             sample_role=role, optimizer_before=prior, optimizer_after=post, state_original=state,
                             formula_original=train_formula, birth_ref=birth_ref, started_at=base+65, committed_at=base+70)
        cycle["publish"] = put(f"publish{i}", "publish", policy_hash=asset, model_hash=model,
                               train_ref=cycle["train"]["sha256"], weights_sha256=weight_hash, published_at=base+71)
        cycle["freeze"] = put(f"freeze{i}", "freeze", **pred, role="SHADOW",
                              model_hash=hashed("old") if fault == "old_model" else model,
                              weights_sha256=hashed("elsewhere") if fault == "substituted_weights" else weight_hash,
                              publication_ref=cycle["publish"]["sha256"], returned_at=base+(81 if fault=="late" else 79),
                              deadline_at=base+80, position=1, numbers=[1,2,3])
        cycle["settle"] = put(f"settle{i}", "settle", policy_hash=asset, target_ordinal=target+int(fault=="wrong_period"),
                              freeze_ref=cycle["freeze"]["sha256"], draw=list(range(1,11)), hit=fault!="wrong_hit",
                              draw_at=base+81, settled_at=base+82)
        cycle["feedback"] = put(f"feedback{i}", "feedback", **pred, event_key="same" if fault=="duplicate" else str(i),
                                committed_count=1, settlement_ref=cycle["settle"]["sha256"], committed_at=base+83)
        cycle["research"] = put(f"research{i}", "research", **pred, support=8, artifact_hash=hashed(f"frame{i}"),
                                role="DEVELOPMENT", feedback_ref=cycle["feedback"]["sha256"], completed_at=base+84)
        cycles.append(cycle)
        if i < 2:
            parent_ids = [i] if i == 0 else [i, i if fault=="same_parent" else 3]
            births.append(put(f"birth{i}", "birth", operation="MUTATION" if i==0 else "FUSION",
                              parents=[hashed(f"asset{j}") for j in parent_ids], parent_originals=[programs[j] for j in parent_ids],
                              child_hash=hashed(f"asset{i+1}"), child_original=programs[i+1],
                              parent_behavior=hashed(f"parent{i}"), child_behavior=hashed(f"child{i}"),
                              time_scope="different-clock" if fault == "cross_clock" else scope["time_scope"],
                              research_ref=cycle["research"]["sha256"], committed_at=base+(82 if fault=="early_birth" else 85)))
    if fault=="one_generation":
        cycles.pop()
        births.pop()
    return dict(**scope, cycles=cycles, births=births)


class MultiGenerationTest(unittest.TestCase):
    def test_complete(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)
            self.assertEqual(verify(fixture(root),root)["cycles"], 3)

    def test_refusals(self):
        for fault in ("label_leak","no_training","old_model","late","wrong_period","wrong_hit","duplicate","same_parent","one_generation"):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as temp:
                root=Path(temp)
                with self.assertRaises(ValueError):
                    verify(fixture(root, fault),root)

    def test_cross_generation_causality(self):
        for fault, reason in (
            ("child_before_parent_settled", "child started before"),
            ("early_birth", "born before research"),
            ("cross_clock", "wrong scope"),
            ("swapped_birth", "wrong birth"),
            ("swapped_child_program", "wrong birth or executable formula"),
            ("reused_weights", "reused or substituted"),
            ("substituted_weights", "reused or substituted"),
        ):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as temp:
                root = Path(temp)
                with self.assertRaisesRegex(ValueError, reason):
                    verify(fixture(root, fault), root)

    def test_changed_file(self):
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp); manifest=fixture(root)
            (root/"research1").write_text("{}")
            with self.assertRaisesRegex(ValueError,"digest"):
                verify(manifest,root)


class TraceReceiptTest(unittest.TestCase):
    def evaluate(self, root, fault=None):
        case_id = "OTHER" if fault == "other_case" else "A-B12-03b"
        review = "OLDER" if fault == "historical_case" else "F3-B12-01"
        case = dict(id=case_id, issue_id="issue", batch="B12", level="ENGINEERING",
                    required_review_ids=[review], test_contract=dict(harness="fixture", command_argv=["run"]))
        receipt = dict(source_after="a"*40, plan_manifest_sha256="c"*64)
        member = "assets/identity.json"
        with zipfile.ZipFile(root/"build.apk", "w") as package:
            package.writestr("AndroidManifest.xml", b"manifest")
            package.writestr("classes.dex", b"dex")
            package.writestr(member, json.dumps(dict(sourceCommit="e"*40 if fault=="wrong_build" else receipt["source_after"],
                                                      planSha256=receipt["plan_manifest_sha256"])))
        apk_hash = sha(root/"build.apk")
        trace = root/"trace"
        trace.mkdir()
        index = fixture(trace, fault="child_before_parent_settled" if fault=="invalid_chain" else None,
                        source_commit="d"*40 if fault=="foreign_source" else receipt["source_after"],
                        apk_sha256="d"*64 if fault=="foreign_apk" else apk_hash)
        (trace/"index.json").write_text(json.dumps(index))
        (root/"runner.log").write_text("isolated host test")
        (root/"measured.json").write_text('{"count": 3}')
        obs = dict(metric="count", expected=3, actual=3, operator="eq", review_id=review,
                   evidence_artifact="measured.json", json_pointer="/count")
        run = dict(id=case_id, issue_id=case["issue_id"], batch="B12", level="ENGINEERING",
                   source_after=receipt["source_after"], plan_manifest_sha256=receipt["plan_manifest_sha256"],
                   case_contract_sha256=case_digest(case), harness="fixture", exit_code=0, command=["run"],
                   log_sha256=sha(root/"runner.log"), owner_paths=["app/owner"], assertions=[obs],
                   oracle_path="tools/expect.json", apk_artifact="build.apk", apk_identity_member=member)
        if fault=="receipt_apk":
            receipt.update(apk_artifact=run.pop("apk_artifact"), apk_identity_member=run.pop("apk_identity_member"),
                           apk_sha256=apk_hash)
        if fault=="template_null":
            receipt.update(apk_artifact=None, apk_identity_member=None, apk_sha256=None)
            run["apk_sha256"] = apk_hash
        if fault=="template_null_no_hash":
            receipt.update(apk_artifact=None, apk_identity_member=None, apk_sha256=None)
        if fault=="conflicting_apk":
            receipt["apk_sha256"] = "e"*64
            run["apk_sha256"] = apk_hash
        if fault not in {"missing_index", "historical_case", "other_case"}:
            run["learning_trace_index_artifact"] = "trace/index.json"
        (root/"run.json").write_text(json.dumps(run))
        missing = {"unlisted_index": "trace/index.json", "unlisted_raw": "trace/weights1",
                   "unlisted_formula": "trace/formula3", "unlisted_apk":"build.apk"}.get(fault)
        artifacts = [{"path": p.relative_to(root).as_posix(), "sha256": sha(p)} for p in root.rglob("*")
                     if p.is_file() and p.relative_to(root).as_posix() != missing]
        originals = Originals(root, artifacts)
        result = dict(status="PASS", exit_code=0, log="runner.log", execution_artifact="run.json",
                      command=["run"], assertions=[obs])
        oracle = json.dumps({case_id: {"count": {"expected": 3, "operator": "eq", "review_id":review}}})
        with patch("verify_receipt.git", side_effect=["blob", oracle]):
            check_result(result, case, receipt, originals, root, {"app/owner"})

    def test_registered_trace_and_historical_cases(self):
        for case in (None, "receipt_apk", "template_null", "template_null_no_hash", "historical_case", "other_case"):
            with self.subTest(case=case), tempfile.TemporaryDirectory() as temp:
                self.evaluate(Path(temp), case)

    def test_missing_substituted_and_invalid_evidence(self):
        for fault, reason in (
            ("missing_index", "Noncanonical"), ("unlisted_index", "Missing JSON original"),
            ("unlisted_raw", "Undeclared"), ("unlisted_formula", "Undeclared"),
            ("unlisted_apk", "APK differs"), ("foreign_source", "source differs"),
            ("foreign_apk", "APK differs"), ("conflicting_apk", "Conflicting"),
            ("wrong_build", "build identity differs"), ("invalid_chain", "child started before"),
        ):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as temp:
                with self.assertRaisesRegex(ValueError, reason):
                    self.evaluate(Path(temp), fault)


if __name__=="__main__":
    unittest.main()
