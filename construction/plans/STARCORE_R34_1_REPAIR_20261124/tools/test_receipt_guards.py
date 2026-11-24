#!/usr/bin/env python3
"""Host tests of receipt rejection rules; these are not Android or quality tests."""
import copy
import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from verify_receipt import (Originals, campaign, case_digest, decode, natural, pointer,
                            validate, oracle_rules, natural_status, quality_projection, wilson_pair, cp_upper)
from verify_receipt import assert_measured, product_paths, background_window_seconds


class MeasuredAssertionGuards(unittest.TestCase):
    def test_missing_original_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 1, "expected": 1})

    def test_false_success_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 0, "expected": 1,
                             "evidence_artifact": "commits.json"})

    def test_unknown_cannot_become_zero(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "training", "actual": None, "expected": 0,
                             "evidence_artifact": "training.json"})

    def test_boolean_is_not_counter(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "trained", "actual": True, "expected": 1,
                             "operator": "ge", "evidence_artifact": "training.json"})

    def test_nan_is_not_passing_measurement(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "loss", "actual": float("nan"), "expected": 1,
                             "operator": "le", "evidence_artifact": "training.json"})

    def test_measured_bound_passes(self):
        assert_measured({"metric": "commits", "actual": 2, "expected": 1,
                         "operator": "ge", "evidence_artifact": "commits.json"})

    def test_unknown_operator_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 1, "expected": 1,
                             "operator": "ASSUME_PASS", "evidence_artifact": "commits.json"})

    def test_equal_infinities_do_not_pass(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "loss", "actual": float("inf"), "expected": float("inf"),
                             "evidence_artifact": "loss.json"})

    def test_membership_cannot_turn_boolean_into_count(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "count", "actual": True, "expected": [1],
                             "operator": "in", "evidence_artifact": "count.json"})

    def test_version_and_tests_alone_are_not_product_implementation(self):
        self.assertFalse(product_paths({"release/release_manifest.json",
                                       "tools/r341/fixtures/continuity_b12.json",
            "app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java",
            "app/src/androidTest/java/com/ethk3/novaonepro/Test.java", "tools/r341/test_identity.py"}))

    def test_production_owner_change_is_visible_for_review(self):
        path="app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java"
        self.assertEqual(product_paths({path,"release/release_manifest.json"}),{path})

    def test_five_minute_window_cannot_sign_72_hours(self):
        with self.assertRaises(ValueError):
            background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-24T10:05:00+08:00")

    def test_24_hour_checkpoint_is_not_final_72_hour_pass(self):
        with self.assertRaises(ValueError):
            background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-25T10:00:00+08:00")

    def test_real_72_hour_span_can_be_structurally_checked(self):
        self.assertEqual(background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-27T10:00:00+08:00"),259200)


class ReceiptIntegration(unittest.TestCase):
    """Tiny real Git history and synthetic originals for the host validator, never app acceptance."""

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        self.plan = self.root / "plan"
        self.ev = self.root / "evidence"
        self.ev.mkdir()
        self.git("init", "-q")
        self.git("config", "user.name", "Host fixture")
        self.git("config", "user.email", "fixture@localhost")
        self.save(self.repo / "app/owner.py", "def value(): return 0\n")
        self.git("add", ".")
        self.git("commit", "-qm", "before")
        self.before = self.git("rev-parse", "HEAD")
        self.save(self.repo / "app/owner.py", "def value(): return 3\n")
        self.issue = "ISSUE-1"
        self.case = {"id": "CASE1", "batch": "B01", "level": "ENGINEERING", "issue_id": self.issue,
                     "required_review_ids": ["REV-1"], "test_contract": {"path": "tools/run_test.py",
                     "harness": "PYTHON", "method": "test_case", "command_argv": ["python3", "tools/run_test.py", "CASE1"]}}
        self.save(self.repo / "tools/run_test.py", '# STARCORE_EXPECT CASE1 {"result":{"operator":"ge","expected":2,"review_id":"REV-1"}}\n')
        self.git("add", ".")
        self.git("commit", "-qm", "after")
        self.after = self.git("rev-parse", "HEAD")
        issue = {"id": self.issue, "files": [{"path": "app/owner.py", "action": "MODIFY"}]}
        self.save(self.plan / "WORK_ORDERS.json", {"batches": [{"id": "B01", "issues": [issue]}]})
        self.save(self.plan / "ACCEPTANCE_MATRIX.json", {"cases": [self.case]})
        self.save(self.plan / "control/PLAN_IDENTITY.json", {"application_baseline_commit": self.before})
        self.log = self.save(self.ev / "log", "one recorded command")
        self.save(self.ev / "raw.json", {"rows": [{"n": 3}]})
        obs = {"metric": "result", "actual": 3, "operator": "ge", "expected": 2, "review_id": "REV-1",
               "evidence_artifact": "raw.json", "json_pointer": "/rows/0/n"}
        self.result = {"id": "CASE1", "exit_code": 0, "status": "PASS", "command": self.case["test_contract"]["command_argv"],
                       "log": "log", "execution_artifact": "run.json", "assertions": [obs]}
        self.run = {"id": "CASE1", "batch": "B01", "level": "ENGINEERING", "issue_id": self.issue,
                    "plan_manifest_sha256": "a"*64, "source_after": self.after,
                    "case_contract_sha256": case_digest(self.case), "harness": "PYTHON",
                    "exit_code": 0, "log_sha256": self.log, "owner_paths": ["app/owner.py"],
                    "runner_path": "tools/run_test.py", "runner_method": "test_case",
                    "runner_blob_sha": self.git("rev-parse", self.after+":tools/run_test.py"),
                    "command": self.result["command"], "assertions": [obs], "oracle_path": "tools/run_test.py"}
        self.receipt = {"schema": "STARCORE_R341_EXECUTION_RECEIPT_2", "level": "ENGINEERING", "batch": "B01",
                        "plan_manifest_sha256": "a"*64, "status": "PASS", "template_only": False,
                        "issue_ids": [self.issue], "source_before": self.before, "source_after": self.after,
                        "test_results": [self.result], "revalidation_only": False}

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.repo), *args], check=True,
                              capture_output=True, text=True).stdout.strip()

    @staticmethod
    def save(path, value):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(value if isinstance(value, str) else json.dumps(value))
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def evaluate(self):
        self.save(self.ev / "run.json", self.run)
        self.receipt["artifacts"] = [{"path": p.name, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                                     for p in self.ev.iterdir()]
        with patch("verify_receipt.verify_seal", return_value={"manifest_sha256": "a"*64}):
            return validate(self.plan, self.receipt, self.ev, self.repo, "ENGINEERING")

    def test_bound_case(self):
        self.assertEqual(self.evaluate()["status"], "PASS_RECEIPT_STRUCTURE_AND_DIGESTS")

    def test_template_and_stale_original_rejected(self):
        self.receipt["template_only"] = True
        with self.assertRaises(ValueError):
            self.evaluate()
        self.receipt["template_only"] = False
        self.run["source_after"] = self.before
        with self.assertRaises(ValueError):
            self.evaluate()

    def test_pending_no_pass(self):
        self.receipt.update(status="NOT_RUN", reason="device unavailable", source_after=None)
        self.assertEqual(self.evaluate()["status"], "RECORDED_NONPASS")

    def test_original_values_cannot_be_changed_in_receipt_and_runner(self):
        self.save(self.ev / "raw.json", {"rows": [{"n": 0}]})
        with self.assertRaisesRegex(ValueError, "differs from raw"):
            self.evaluate()

    def test_changed_cutoff_does_not_pass_even_when_raw_is_real(self):
        self.result["assertions"][0]["expected"] = 1
        with self.assertRaisesRegex(ValueError, "Expected value"):
            self.evaluate()

    def test_level_owner_harness_selector_and_review_enforced(self):
        for key, value in (("issue_id", "OTHER"), ("level", "NATURAL"), ("owner_paths", ["tools/run_test.py"]),
                           ("runner_blob_sha", "1"*40), ("runner_method", "other")):
            with self.subTest(key=key):
                previous = self.run[key]
                self.run[key] = value
                with self.assertRaises(ValueError):
                    self.evaluate()
                self.run[key] = previous
        self.run["command"] = self.result["command"] = ["true"]
        with self.assertRaises(ValueError):
            self.evaluate()
        self.run["command"] = self.result["command"] = self.case["test_contract"]["command_argv"]
        self.result["assertions"][0].pop("review_id")
        with self.assertRaises(ValueError):
            self.evaluate()

    def test_cross_batch_change_is_not_implementation(self):
        self.receipt.update(source_before=self.after)
        self.save(self.repo / "app/unrelated.py", "42")
        self.git("add", ".")
        self.git("commit", "-qm", "unrelated")
        self.receipt["source_after"] = self.git("rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "No planned production"):
            self.evaluate()

    def test_duplicate_path_and_symlink_rejected(self):
        item = {"path": "log", "sha256": self.log}
        with self.assertRaises(ValueError):
            Originals(self.ev, [item, item])
        (self.ev / "alias").symlink_to(self.ev / "log")
        for name in ["alias", "./log", "../log", str(self.ev/"log")]:
            with self.subTest(name=name), self.assertRaises(ValueError):
                Originals(self.ev, [dict(item, path=name)])

    def test_arbitrary_revalidation_bytes_rejected(self):
        self.receipt.update(revalidation_only=True, original_implementation_receipt_artifact="raw.json")
        with self.assertRaises((ValueError, KeyError)):
            self.evaluate()


class FieldGuards(unittest.TestCase):
    def test_source_and_observation_primitives(self):
        for bad in ['{"a":1,"a":2}', '{"x":NaN}']:
            with self.assertRaises(ValueError):
                decode(bad)
        self.assertEqual(pointer({"a/b": [None]}, "/a~1b/0"), None)
        self.assertEqual(oracle_rules('# STARCORE_EXPECT A {"hits":{"expected":1}}', "A")["hits"]["expected"], 1)
        with self.assertRaises(ValueError):
            pointer({"a": []}, "/a/0")
        with self.assertRaises(ValueError):
            assert_measured({"evidence_artifact":"rows", "metric":"missing", "actual":None, "expected":None})
        assert_measured({"evidence_artifact":"rows", "metric":"explicit_null", "actual":None,
                         "expected":None, "operator":"is_null"})

    def test_natural_pending_stays_pending_and_inflated_support_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            receipt = {"window_start":"2026-01-01T10:00:00Z", "window_end":"2026-01-01T11:00:00Z",
                       "source_after":"c"*40, "apk_sha256":"a"*64, "preregistration_artifact":"p",
                       "future_calendar_artifact":"f", "natural_evaluator_artifact":"r"}
            pre = {"candidate_hash":"candidate", "protocol_hash":"protocol", "freeze_at":receipt["window_start"]}
            future = dict(candidate_hash="candidate", protocol_hash="protocol", targets=[])
            digests = {k: ReceiptIntegration.save(root/k, value) for k, value in (("p",pre),("f",future))}
            result = dict(candidate_hash="candidate", protocol_hash="protocol", verdict="NATURAL_PENDING",
                          fixture=False, future_act_count=0, opportunity_count=0, quality_projection_status="NOT_OBSERVED",
                          math_verdict="NOT_EVALUATED", issuance_status="NOT_EVALUATED", source_commit=receipt["source_after"],
                          apk_sha256=receipt["apk_sha256"], preregistration_sha256=digests["p"], calendar_sha256=digests["f"])
            def check():
                digests["r"] = ReceiptIntegration.save(root/"r", result)
                originals = Originals(root, [{"path": k, "sha256": v} for k, v in digests.items()])
                return natural(receipt, originals)
            self.assertEqual(check()["verdict"], "NATURAL_PENDING")
            result["verdict"] = "QUALIFIED"
            with self.assertRaises(ValueError):
                check()
            result.update(verdict="NATURAL_PENDING", future_act_count=250)
            with self.assertRaises(ValueError):
                check()

    def test_final_natural_case_cannot_sign_insufficient_acts(self):
        self.assertEqual(natural_status("B12", "REJECTED", 249), "RECORDED_NONPASS")
        self.assertEqual(natural_status("B12", "PENDING_NATURAL", 250), "RECORDED_NONPASS")
        self.assertEqual(natural_status("B09", "NATURAL_PENDING", 0), "PASS_RECEIPT_STRUCTURE_AND_DIGESTS")

    def test_qualified_issuance_requires_all_live_guards(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            t0, t1, t2, t3, t4 = ["2026-01-01T%02d:00:00Z" % h for h in range(10, 15)]
            cand = {"candidate_hash": "one", "protocol_hash": "v1"}
            receipt = dict(window_start=t0, window_end=t4, source_after="c"*40, apk_sha256="a"*64,
                           preregistration_artifact="pre", future_calendar_artifact="cal", natural_evaluator_artifact="eval")
            future = dict(opportunities=1666, acts=250, hits=100, winNetProfit=2, missLoss=1, netRecordedPayoff=50,
                          coveragePass=True, expectedPayoffPositive=True)
            issuing = dict(issued=True, model_binding="BOUND", calibration_binding="BOUND", qualification_binding="BOUND",
                           source_revoked=False, qualification_gap_or_conflict=False, failing=[], registeredFutureR34=future)
            cert = dict(cand, status="ACTIVE", issuance=issuing, issued_at=t3, expires_at=t4)
            values = {"pre": dict(cand, freeze_at=t0, draw_source_id="registered-feed"),
                      "cal": dict(cand, targets=[dict(target_key=str(i), opportunity_at=t1, deadline_at=t2) for i in range(1666)]),
                      "acts": dict(cand, role="PREREGISTERED_FUTURE", fixture=False, source_commit=receipt["source_after"],
                                   settled_acts=[dict(target_key=str(i), freeze_return_at=t1, draw_at=t2, settled_at=t3,
                                                      hit=i % 5 < 2) for i in range(250)]), "cert": cert}
            values["pred"] = dict(cand, source_commit=receipt["source_after"], fixture=False, role="PREREGISTERED_FUTURE",
                                  predictions=[dict(target_key=str(i), freeze_return_at=t1, position=2,
                                                    numbers=[1,2,3]) for i in range(250)])
            values["labels"] = dict(source_id="registered-feed", fixture=False, draws=[
                dict(target_key=str(i), draw_at=t2, numbers=list(range(1,11)) if i%5<2 else list(range(10,0,-1)))
                for i in range(250)])
            result = dict(cand, frozen_predictions_artifact="pred", authoritative_draws_artifact="labels", verdict="QUALIFIED", math_verdict="PASS", issuance_status="ACTIVE", v1_pass=True, opportunity_count=1666, future_act_count=250,
                          future_act_ledger_artifact="acts", evaluated_at=t3, certificate_artifact="cert",
                          fixture=False, source_commit=receipt["source_after"], apk_sha256=receipt["apk_sha256"],
                          v1_metrics=dict(hr=.40, ece=.03, worst720Block=.3, maxLosingStreak=3, pairedLcbVsI00=.01,
                                          pairedLcbVsNull=.01, abi_equivalent=True, sequential_pass=True),
                          quality_projection_status="NOT_OBSERVED")
            def check():
                digests = {k: ReceiptIntegration.save(root/k, v) for k,v in values.items()}
                result.update(preregistration_sha256=digests["pre"], calendar_sha256=digests["cal"])
                digests["eval"] = ReceiptIntegration.save(root/"eval", result)
                return natural(receipt, Originals(root, [{"path": k, "sha256": v} for k,v in digests.items()]))
            self.assertEqual(check()["verdict"], "QUALIFIED")
            for collection, row_key, key, value in [
                    ("pred","predictions","target_key","not-registered"), ("labels","draws","target_key","wrong-draw"),
                    ("pred","predictions","freeze_return_at",t2), ("labels","draws","draw_at",t1),
                    ("cal","targets","deadline_at",t1), ("pred","predictions","numbers",[1,1,2]),
                    ("acts","settled_acts","hit",False), ("labels","draws","numbers",list(range(10,0,-1)))]:
                with self.subTest(collection=collection, key=key):
                    row = values[collection][row_key][0]
                    old = row[key]
                    row[key] = value
                    with self.assertRaises(ValueError):
                        check()
                    row[key] = old
            for key, value in (("issued", False), ("source_revoked", True), ("qualification_gap_or_conflict", True),
                               ("calibration_binding", "MISSING"), ("model_binding", "UNBOUND")):
                with self.subTest(key=key):
                    old = issuing[key]
                    issuing[key] = value
                    with self.assertRaises(ValueError):
                        check()
                    issuing[key] = old
            future["winNetProfit"] = 1
            with self.assertRaisesRegex(ValueError, "payoff"):
                check()
            future["winNetProfit"] = 2
            values["cal"]["targets"].append(dict(target_key="1666", opportunity_at=t1, deadline_at=t2))
            result["opportunity_count"] = future["opportunities"] = 1667
            with self.assertRaisesRegex(ValueError, "coverage"):
                check()

    def test_checkpoint_must_be_actual_first_day_prefix(self):
        from datetime import datetime, timedelta, timezone
        start = datetime(2026,1,1,tzinfo=timezone.utc)
        t = lambda hours: (start+timedelta(hours=hours)).isoformat()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            r = dict(apk_sha256="a"*64, device_identity="unit", installed_version_code=2, fixture=False,
                     source_after="c"*40, window_start=t(0), window_end=t(72), background_campaign_artifact="run")
            dt = {k:r[k] for k in ("device_identity","installed_version_code","apk_sha256")}
            keys = ("preregistered_calendar_artifact","opportunity_ledger_artifact","boot_segments_artifact",
                    "interventions_artifact","raw_events_artifact","checkpoint_24h_artifact","coverage_evaluator_artifact")
            names = dict(zip(keys,("cal","ledger","boot","actions","events","cut","out")))
            run = dict(r, source_commit=r["source_after"], **names)
            targets = [{"target_key":str(h),"opportunity_at":t(h)} for h in (0,23,24,71)]
            rows = [{"target_key":str(h),"observed_at":t(h),"role":"shadow","reason":"known","terminal_state":"frozen"}
                    for h in (0,23,24,71)]
            cut = dict(dt, source_commit=r["source_after"], window_start=t(0), window_end=t(24),
                       exported_at=t(75), opportunities=rows[:2])
            def check():
                values = {"run":run,"cal":{"targets":targets},"ledger":{"opportunities":rows},"boot":{"kind":"boot"},
                          "actions":{"kind":"interventions"},"events":{"opportunity_events":[dict(row,sequence=i) for i,row in enumerate(rows)]}}
                dig = {k: ReceiptIntegration.save(root/k,v) for k,v in values.items()}
                cut["calendar_sha256"] = dig["cal"]
                cut["raw_events_sha256"] = dig["events"]
                dig["cut"] = ReceiptIntegration.save(root/"cut", cut)
                dig["out"] = ReceiptIntegration.save(root/"out",dict(verdict="PASS", unattended=True, opportunity_count=4,
                                                                     calendar_sha256=dig["cal"], ledger_sha256=dig["ledger"]))
                campaign(r, Originals(root, [{"path":k,"sha256":v} for k,v in dig.items()]))
            check()
            for k, value in (("window_end",t(72)),("opportunities",rows),("source_commit","d"*40),("exported_at",t(23))):
                with self.subTest(k=k):
                    old=cut[k]; cut[k]=value
                    with self.assertRaises(ValueError):
                        check()
                    cut[k]=old

    def test_quality_pass_requires_eight_scoped_components_and_real_q05_samples(self):
        ident=dict(candidate_hash="candidate",protocol_hash="protocol",source_commit="c"*40)
        result=dict(ident, quality_projection_status="PASS", issuance_status="ACTIVE", quality_projection_artifact="q")
        qvals = {"Q01":{"hit_rate":.4},"Q02":{"future_act_rate":.25,"real_act_rate":.2},
                 "Q03":{"sequential_pass":True,"log_e_up":4.0},"Q04":{"worst720Block":.3,"maxLosingStreak":3},
                 "Q05":{"support":200,"eceEqualMass":0.,"eceEqualWidth":0.,"brier":.25,"baselineBrier":.29,
                        "bss":1-.25/.29,"supportCoverage":1.0},"Q06":{"paired_lcb_vs_parent":.01},
                 "Q07":{"independently_qualified_assets":1}}
        with tempfile.TemporaryDirectory() as temp:
            root=Path(temp)
            pre={"allocated_alpha":.05,"stability_window_rule_hash":"win","factory_protocol_hash":"91b63a645fc0fcbfd0dbb6488dd252d2d6b5ca28ce941211dac8b71c6fa0a930"}
            rows={"q":dict(ident,components={})}
            for k in [*qvals,"Q08"]:
                rows["q"]["components"][k]={"status":"PASS","evidence_artifact":k}
                rows[k]=dict(ident,id=k,values=qvals.get(k,{}))
            rows["Q02"]["deployed_opportunities"]=[dict(target_key=str(i),real_act=i%5==0) for i in range(250)]
            rows["Q04"]["window_rule_hash"]="win"
            rows["Q05"]["samples"]=[dict(ordinal=i,position=1,fitThroughOrdinal=i-2,p=.5,rawProbability=.5,hit=i%2,
                                         supportLo=.3,supportHi=.6,supported=True,fitRows=120,fitHash="prior",
                                         fitRole="H_SELECT",evaluationRole="FUTURE_READ_ONLY") for i in range(200)]
            rows["Q05"]["calibratedBssBlockInterval95"]={"blocks":3,"mean":None,"lower95":None,"upper95":None,
                                                        "status":"INSUFFICIENT_FIXED_BLOCKS"}
            rows["Q06"]["interval_scope"]="ONE_SIDED_95_PAIRED_SAME_OPPORTUNITY"
            rows["Q07"]["qualified_candidate_hashes"]=["candidate"]
            arms=["NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL","SIGNAL_DISABLED","DRIFT","GAPS"]
            def interval(k,n):
                lower,upper=wilson_pair(k,n)
                return dict(events=k,independentReplicates=n,rate=k/n,lower95=lower,upper95=upper)
            trials=[dict(sourceCommit=ident["source_commit"],protocolHash=pre["factory_protocol_hash"],namespace="SYNTHETIC_FULL_FACTORY_R31",
                         arm=a,seed=i,realPermission=False,naturalQualityEligible=False,
                         status="DETECTED" if a=="SIGNAL" else "NOT_DETECTED") for a in arms for i in range(710000,710020)]
            groups=[dict(arm=a,completed=20,failures=0,detected=20 if a=="SIGNAL" else 0,
                         independentTrialInterval=interval(20 if a=="SIGNAL" else 0,20)) for a in arms]
            # Frozen protocol fields are copied from the 122 report; the measured trial outcomes below are host fixtures.
            protocol = {'arms': ['NULL_UNIFORM', 'NULL_TIME_SHUFFLED', 'SIGNAL', 'SIGNAL_DISABLED', 'DRIFT', 'GAPS'],
             'births': 128,
             'certificateContract': {'MAX_ECE': 0.04,
                                     'MAX_LOSS_STREAK': 6,
                                     'MIN_ACTS': 250,
                                     'MIN_HR': 0.34,
                                     'MIN_WORST_720_BLOCK': 0.26,
                                     'activeGate': 'R27_GATE_V1',
                                     'contractHash': 'ef440c1499d9da6bac7d2cf32aecb6dfdca5af09d903d1f11fd4c97773df0a10',
                                     'scope': 'Original immutable V1 thresholds; no V2 report can issue a '
                                              'certificate'},
             'confirmationRule': 'ORIGINAL_FROZEN_CONFIRMATION_OUTSIDE_EVOLUTION',
             'creationCostUnits': 1099511627776,
             'creationSliceUnits': 4294967296,
             'factoryFamilies': ['com.ethk3.novaonepro.strategyv2.IdeaI00RandomBaselineR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI01FrequencyReversionR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI02GapCycleR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI03TransitionChainR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI04CrossPositionR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI05PermutationConsistencyR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI06RegimeSwitchR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI07FragmentRetrievalR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI08NeuralRepresentationR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI09SymbolicFormulaR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI10FailureRepairR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI11ComplementaryEnsembleR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI12LegacyAssetR27',
                                 'com.ethk3.novaonepro.strategyv2.IdeaI13SelectorOnlyR27',
                                 'com.ethk3.novaonepro.strategyv2.SetIntersectionPolicyR30',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$WindowDifference',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$PositionRotation',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$GapRebound',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$NeighborTransition',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$ParitySizeState',
                                 'com.ethk3.novaonepro.strategyv2.StructureGeneratorRegistryR30$ConditionalIntersection'],
             'familyMinimum': 2,
             'farUpper95Budget': 0.15,
             'futureRows': 6000,
             'futureWindowRule': 'FIXED_PRE_REGISTERED_6000_PERIODS;E_PROCESS_TERMINAL_STATE_NEVER_SHORTENS_RISK_OR_MIN_SUPPORT_EVALUATION',
             'generations': 3,
             'i00PerGeneration': 8,
             'namespace': 'SYNTHETIC_FULL_FACTORY_R31',
             'populationCap': 256,
             'powerLower95Floor': 0.7,
             'realPermission': False,
             'recursiveNullControl': True,
             'replicatesPerArm': 20,
             'seedBase': 710000,
             'semantics': '72c5ce1b7a1a3f47b62997f9f26516c7f5d7157a99f0938e4b12f4e91da4ff34',
             'signalPosition': 3,
             'signalStrength': 0.3,
             'version': 'R31_FULL_FACTORY_EXPERIMENT_2_FIXED_FUTURE',
             'warmRows': 3000}
            factory=dict(protocol=protocol,sourceCommit=ident["source_commit"],protocolHash=pre["factory_protocol_hash"],realPermission=False,
                         namespace="SYNTHETIC_FULL_FACTORY_R31",plannedTrials=120,completedTrials=120,failedTrials=0,
                         trialResults=trials,arms=groups,meetsRegisteredExperiment=True,falseAlarmUpper95=cp_upper(0,60),
                         signalPowerLower95=1-cp_upper(0,20))
            rows["Q08"].update(factory=factory,power_source_commit=ident["source_commit"],
                               power_trials=[dict(trueHitProbability=e,rep=i,detected=i<10,periods=2000)
                                             for e in (.34,.37,.40) for i in range(20)],
                               power_intervals=[dict(trueHitProbability=e,detectionProbabilityInterval95=interval(10,20))
                                                for e in (.34,.37,.40)])
            def check():
                orig=Originals(root,[{"path":k,"sha256":ReceiptIntegration.save(root/k,v)} for k,v in rows.items()])
                return quality_projection(pre,result,orig,250,100,1000)
            self.assertEqual(check(),"PASS")
            result["quality_projection_status"]="MANUALLY_APPROVED"
            with self.assertRaises(ValueError):
                check()
            result["quality_projection_status"]="PASS"
            for obj,k,value in ((factory,"trialResults",trials[:50]),(factory,"signalPowerLower95",1.0),
                                (rows["Q08"],"power_trials",rows["Q08"]["power_trials"][:-1]),
                                (rows["Q05"],"samples",rows["Q05"]["samples"][:-1]),
                                (qvals["Q05"],"eceEqualWidth",.03),(qvals["Q05"],"bss",0.)):
                with self.subTest(key=k):
                    old=obj[k];obj[k]=value
                    with self.assertRaises(ValueError):
                        check()
                    obj[k]=old
            samples=rows["Q05"]["samples"]
            for row in samples[:21]:
                row.update(supportHi=.4,supported=False)
            qvals["Q05"]["supportCoverage"]=179/200
            with self.assertRaisesRegex(ValueError,"coverage"):
                check()
            for row in samples[:21]:
                row.update(supportHi=.6,supported=True)
            qvals["Q05"]["supportCoverage"]=1.
            # Tied probabilities preserve chronological order. Width error is zero, mass bins have error .5.
            for i,row in enumerate(samples):
                row["hit"]=int(i>=100)
            qvals["Q05"]["eceEqualMass"]=.5
            with self.assertRaisesRegex(ValueError,"equal-mass"):
                check()
            rows["q"]["components"].pop("Q08")
            with self.assertRaisesRegex(ValueError,"components"):
                check()

    def test_b05_and_b12_campaign_require_real_72h(self):
        with self.assertRaises(ValueError):
            campaign({"window_start":"2026-01-01T00:00:00Z","window_end":"2026-01-01T00:01:00Z"}, None)


if __name__ == "__main__":
    unittest.main()
