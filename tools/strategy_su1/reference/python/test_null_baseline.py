"""R10 null-baseline, negative-control and promotion-gate tests (host reference only)."""
import hashlib
import json
import math
from pathlib import Path
import random
import sqlite3
import unittest

import null_baseline as nb
from test_schema import Fixture, connect, h, insert, migrate, radius

ROOT = Path(__file__).resolve().parents[2]
GOLDEN = ROOT / "reference" / "fixtures" / "null_baseline_golden.json"


class NullBaselineChoiceTests(unittest.TestCase):
    def test_golden_vectors_match_committed_fixture(self):
        g = json.loads(GOLDEN.read_text(encoding="utf-8"))
        self.assertEqual("SU1_NULL_BASELINE_V1", g["protocol"])
        for row in g["vectors"]:
            p, nums = nb.null_baseline_choice(g["seed"], row["issue"])
            self.assertEqual((row["position"], tuple(row["numbers"])), (p, nums))

    def test_domain_and_inputs(self):
        with self.assertRaises(ValueError):
            nb.null_baseline_choice("ABC", 1)
        with self.assertRaises(ValueError):
            nb.null_baseline_choice("0" * 64, 0)
        p, nums = nb.null_baseline_choice("1" * 64, 7)
        self.assertTrue(1 <= p <= 10)
        self.assertEqual(3, len(set(nums)))
        self.assertEqual(list(nums), sorted(nums))

    def test_uniform_marginals_and_exact_thirty_percent_on_biased_draws(self):
        seed = hashlib.sha256(b"uniformity").hexdigest()
        n = 30000
        pos = [0] * 11
        num = [0] * 11
        hits = 0
        for issue in range(1, n + 1):
            p, nums = nb.null_baseline_choice(seed, issue)
            pos[p] += 1
            for x in nums:
                num[x] += 1
            # A maximally biased game: every position always draws the identity permutation.
            hits += int(p in nums)
        for k in range(1, 11):
            self.assertAlmostEqual(0.1, pos[k] / n, delta=0.01)
            self.assertAlmostEqual(0.3, num[k] / n, delta=0.012)
        self.assertAlmostEqual(0.3, hits / n, delta=0.012)


class NegativeControlTests(unittest.TestCase):
    def test_no_false_confirmations_under_fair_draws(self):
        r = nb.null_calibration(cohorts=150, schedule=(200, 400, 800))
        self.assertEqual(0, r["false_confirms"]["SHAM_PAIR"])
        self.assertEqual(0, r["false_confirms"]["HOT_NUMBERS_VS_SHAM"])

    def test_real_synthetic_edge_is_detectable(self):
        """Proves the gate is not 'never pass': a genuine 40% vs 30% edge confirms."""
        rng = random.Random(5)
        n = 2000
        draws = []
        for _ in range(n):
            d = list(range(1, 11))
            rng.shuffle(d)
            if rng.random() < 0.143 and d[0] not in (1, 2, 3):
                j = d.index(rng.choice((1, 2, 3)))
                d[0], d[j] = d[j], d[0]
            draws.append(d)
        issues = list(range(1, n + 1))
        sham = lambda i: nb.null_baseline_choice(hashlib.sha256(b"base").hexdigest(), i)
        child = lambda i: (1, (1, 2, 3))
        ok, look, lower = nb.run_cohort(child, sham, draws, issues, (500, 1000, 2000),
                                        lambda j: nb.look_alpha(0.05, 1, 1, j))
        self.assertTrue(ok, lower)

    def test_power_table_is_monotone_in_claims(self):
        rows = nb.power_table()
        twos = [r["required"]["0.02"] for r in rows]
        self.assertEqual(sorted(twos), twos)
        self.assertGreater(twos[0], 20000)


class PromotionGateSchemaTests(unittest.TestCase):
    """SQL gates: lifetime alpha root, mandatory null-baseline claim, null alarm, expiry."""

    def setUp(self):
        self.db = connect()
        migrate(self.db)
        self.f = Fixture(self.db)
        self.sham = h("null-baseline")
        insert(self.db, "su1_strategy_version", dict(strategy_id=self.sham, codec="SU1",
               program_hash=self.f.program, artifact_hash=self.f.artifact, selector_hash=h("sham-selector"),
               calibration_state="MISSING", action_contract_hash=h("K3-position"),
               state_initial_hash=h("state0"), created_sequence=80, created_at=4,
               strategy_role="NULL_BASELINE"))
        self.next_claim = 2
        self.next_seq = 1000

    def tearDown(self):
        self.db.close()

    def cohort(self, child, parent, kind, n, child_hits=True, parent_hits=False):
        """Register a CONFIRM cohort with n opportunities and settle every one."""
        i = self.next_claim
        cid = h("cohort" + str(i))
        fam_alpha = .025
        insert(self.db, "su1_eval_cohort", dict(cohort_id=cid, strategy_id=child,
               parent_strategy_id=parent, claim_id=h("claim" + str(i)), evidence_role="CONFIRM",
               claim_kind=kind, registered_sequence=90, first_issue=1001, planned_opportunities=n,
               protocol_hash=h("proto" + str(i)), protocol_json="{}", family_root_hash=h("family"),
               family_alpha=fam_alpha, global_claim_ordinal=i, look_schedule_json="[%d]" % n,
               baseline_hash=h("baseline"), created_at=5))
        self.next_claim += 1  # only a committed registration consumes a claim ordinal
        insert(self.db, "su1_look_plan", dict(cohort_id=cid, look_ordinal=1,
               planned_opportunities=n, schedule_hash=h("s" + str(i))))
        for k in range(1, n + 1):
            seq = self.next_seq
            self.next_seq += 1
            target = 5000 + seq
            oid = h("o%d-%d" % (i, k))
            insert(self.db, "su1_opportunity", dict(opportunity_id=oid, cohort_id=cid, target_issue=target,
                   sequence_ordinal=seq, opportunity_ordinal=k, declared_at=10, deadline_at=1000,
                   source_epoch="epoch-A", eligibility_hash=h("eligible"), role="CONFIRM"))
            snap = h("snap%d" % target)
            if not self.db.execute("SELECT 1 FROM su1_snapshot WHERE snapshot_hash=?", (snap,)).fetchone():
                insert(self.db, "su1_snapshot", dict(snapshot_hash=snap, game="TEST", target_issue=target,
                       target_open_time=1000, source_max_issue=target - 1, source_available_at=20,
                       captured_at=30, source_epoch="epoch-A", source_version_hash=h("source"),
                       payload_hash=self.f.blob))
            for strat, hit in ((child, child_hits), (parent, parent_hits)):
                pid = h("p%s%d%d" % (strat, i, k))
                insert(self.db, "su1_prediction", dict(prediction_id=pid, opportunity_id=oid,
                       strategy_id=strat, snapshot_hash=snap, target_issue=target, captured_at=40,
                       position=1, n1=1, n2=2, n3=3, prediction_mask=7, raw_k3_score=.3,
                       probability_hash=self.f.blob, period_state_hash=self.f.blob,
                       trained_through_sequence=70, input_through_sequence=100,
                       record_hash=h("r" + pid)))
            draw = h("draw%d" % target)
            insert(self.db, "su1_blob", dict(blob_hash=draw, byte_count=20, codec="CANONICAL_DRAW",
                   storage_state="LOCAL", local_locator="d", created_at=1050))
            insert(self.db, "su1_opportunity_outcome", dict(opportunity_id=oid, target_issue=target,
                   canonical_draw_hash=draw, legacy_settlement_hash=h("t%d" % target),
                   source_epoch="epoch-A", opened_at=1000, completed_at=1050))
            for strat, hit in ((child, child_hits), (parent, parent_hits)):
                pid = h("p%s%d%d" % (strat, i, k))
                insert(self.db, "su1_settlement", dict(settlement_id=h("s" + pid), prediction_id=pid,
                       target_issue=target, canonical_draw_hash=draw, legacy_settlement_hash=h("t%d" % target),
                       actual=1 if hit else 9, hit=int(hit), label_hash=h("l" + pid), committed_at=1100))
        a = fam_alpha / (i * (i + 1) * 1 * 2)
        cw = n if (child_hits and not parent_hits) else 0
        pw = n if (parent_hits and not child_hits) else 0
        mean = (cw - pw) / n
        r = radius(a, n)
        lower = max(-1.0, mean - r)
        ev = h("look" + str(i))
        insert(self.db, "su1_evidence_look", dict(evidence_hash=ev, cohort_id=cid, claim_id=h("claim" + str(i)),
               global_claim_ordinal=i, look_ordinal=1, protocol_hash=h("proto" + str(i)),
               planned_opportunities=n, paired_n=n, parent_missing_n=0, child_missing_n=0, unpaired_n=0,
               parent_hits=n if parent_hits else 0, child_hits=n if child_hits else 0,
               child_only_wins=cw, parent_only_wins=pw, paired_delta=mean, conservative_mean=mean,
               bound_radius=r, lower_bound=lower, allocated_alpha=a, cumulative_alpha_spent=a,
               coverage=1., result_state="PASS" if lower > 0 else "FAIL",
               source_chain_hash=h("chain" + str(i)), metrics_json="{}", recorded_at=1200))
        return ev, lower

    def certificate(self, evidence, baseline, **changes):
        v = dict(certificate_id=h("cert" + evidence), strategy_id=self.f.child, evidence_hash=evidence,
                 baseline_evidence_hash=baseline, policy_hash=h("policy"),
                 source_chain_hash=self.db.execute("SELECT source_chain_hash FROM su1_evidence_look WHERE evidence_hash=?",
                                                   (evidence,)).fetchone()[0], issued_at=1300, expires_at=5000)
        v.update(changes)
        return insert(self.db, "su1_certificate", v)

    def test_new_family_cannot_mint_fresh_alpha(self):
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_FAMILY_ALPHA_EXCEEDS_GLOBAL_ROOT"):
            insert(self.db, "su1_family_budget", dict(family_root_hash=h("family-2"), family_alpha=.05,
                   family_ordinal=2, registration_hash=h("r2"), created_at=6))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_FAMILY_ORDINAL"):
            insert(self.db, "su1_family_budget", dict(family_root_hash=h("family-2"), family_alpha=.001,
                   family_ordinal=1, registration_hash=h("r2"), created_at=6))
        insert(self.db, "su1_family_budget", dict(family_root_hash=h("family-2"), family_alpha=.05 / 6,
               family_ordinal=2, registration_hash=h("r2"), created_at=6))
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE su1_alpha_root SET global_alpha=.5")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM su1_alpha_root")

    def test_claim_kind_binds_roles(self):
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CLAIM_KIND_ROLE_MISMATCH"):
            self.cohort(self.f.child, self.f.parent, "VS_NULL_BASELINE", 2)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CLAIM_KIND_ROLE_MISMATCH"):
            self.cohort(self.f.child, self.sham, "PARENT_CHILD", 2)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CLAIM_KIND_ROLE_MISMATCH"):
            self.cohort(self.f.child, self.sham, "NULL_CONTROL", 2)

    def test_forged_radius_cannot_shift_lower_bound(self):
        ev, _ = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        row = dict(self.db.execute("SELECT * FROM su1_evidence_look WHERE evidence_hash=?", (ev,)).fetchone())
        row.update(evidence_hash=h("forged"), look_ordinal=1)
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_evidence_look", dict(row, bound_radius=row["bound_radius"] / 4))

    def test_parent_win_alone_cannot_certify(self):
        parent_ev, lower = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        self.assertGreater(lower, 0)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CERTIFICATE_NULL_BASELINE_REQUIRED"):
            self.certificate(parent_ev, parent_ev)

    def test_failed_null_baseline_claim_cannot_certify(self):
        parent_ev, _ = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        base_ev, lower = self.cohort(self.f.child, self.sham, "VS_NULL_BASELINE", 24,
                                     child_hits=True, parent_hits=True)
        self.assertLessEqual(lower, 0)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CERTIFICATE_NULL_BASELINE_REQUIRED"):
            self.certificate(parent_ev, base_ev)

    def future_prediction(self, target):
        """A fresh pre-draw freeze for a later target (RESEARCH_FUTURE queue)."""
        self.next_research = getattr(self, "next_research", 999999) + 1
        i = self.next_research                      # research cohorts: separate ordinal namespace
        cid = h("future" + str(i))
        insert(self.db, "su1_eval_cohort", dict(cohort_id=cid, strategy_id=self.f.child,
               parent_strategy_id=self.f.parent, claim_id=h("fclaim" + str(i)), evidence_role="RESEARCH_FUTURE",
               claim_kind="PARENT_CHILD", registered_sequence=90, first_issue=1001, planned_opportunities=1,
               protocol_hash=h("fproto" + str(i)), protocol_json="{}", family_root_hash=h("family"),
               family_alpha=.025, global_claim_ordinal=i, look_schedule_json="[1]",
               baseline_hash=h("baseline"), created_at=5))
        oid = h("fo" + str(target))
        seq = self.next_seq
        self.next_seq += 1
        insert(self.db, "su1_opportunity", dict(opportunity_id=oid, cohort_id=cid, target_issue=target,
               sequence_ordinal=seq, opportunity_ordinal=1, declared_at=1300, deadline_at=4000,
               source_epoch="epoch-A", eligibility_hash=h("eligible"), role="RESEARCH_FUTURE"))
        snap = h("fsnap" + str(target))
        insert(self.db, "su1_snapshot", dict(snapshot_hash=snap, game="TEST", target_issue=target,
               target_open_time=5000, source_max_issue=target - 1, source_available_at=1340,
               captured_at=1350, source_epoch="epoch-A", source_version_hash=h("source"), payload_hash=self.f.blob))
        pid = h("fp" + str(target))
        insert(self.db, "su1_prediction", dict(prediction_id=pid, opportunity_id=oid, strategy_id=self.f.child,
               snapshot_hash=snap, target_issue=target, captured_at=1400, position=1, n1=1, n2=2, n3=3,
               prediction_mask=7, raw_k3_score=.3, probability_hash=self.f.blob, period_state_hash=self.f.blob,
               trained_through_sequence=70, input_through_sequence=100, record_hash=h("fr" + pid)))
        return pid

    def binding(self, cert, pid, target, bound_at=1500):
        return dict(target_issue=target, strategy_id=self.f.child, certificate_id=cert, prediction_id=pid,
                    adapter_contract_hash=h("adapter"), original_owner_receipt_hash=h("owner"), bound_at=bound_at)

    def test_both_claims_certify_then_expiry_and_null_alarm_block_binding(self):
        parent_ev, _ = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        base_ev, lower = self.cohort(self.f.child, self.sham, "VS_NULL_BASELINE", 24)
        self.assertGreater(lower, 0)
        with self.assertRaises(sqlite3.IntegrityError):
            self.certificate(parent_ev, base_ev, issued_at=1300, expires_at=1300)
        self.certificate(parent_ev, base_ev)
        cert = h("cert" + parent_ev)
        ev2, _ = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        self.certificate(ev2, base_ev, certificate_id=h("short"), expires_at=1450)
        p1, p2, p3 = (self.future_prediction(t) for t in (90001, 90002, 90003))
        self.db.execute("UPDATE su1_runtime_control SET mode='MAINLINE_ADAPTER' WHERE singleton=1")
        insert(self.db, "su1_mainline_binding", self.binding(cert, p1, 90001))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_MAINLINE_CERTIFICATE_MISMATCH"):
            insert(self.db, "su1_mainline_binding", self.binding(h("short"), p2, 90002))
        # A sham-vs-sham control that "passes" means the pipeline is broken: block everything.
        self.db.execute("UPDATE su1_runtime_control SET mode='RESEARCH' WHERE singleton=1")
        sham2 = h("null-control")
        insert(self.db, "su1_strategy_version", dict(strategy_id=sham2, codec="SU1", program_hash=self.f.program,
               artifact_hash=self.f.artifact, selector_hash=h("sham2"), calibration_state="MISSING",
               action_contract_hash=h("K3-position"), state_initial_hash=h("state0"), created_sequence=80,
               created_at=4, strategy_role="NULL_CONTROL"))
        _, control_lower = self.cohort(sham2, self.sham, "NULL_CONTROL", 24)
        self.assertGreater(control_lower, 0)
        self.db.execute("UPDATE su1_runtime_control SET mode='MAINLINE_ADAPTER' WHERE singleton=1")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_NULL_CONTROL_ALARM"):
            insert(self.db, "su1_mainline_binding", self.binding(cert, p3, 90003))
        ev3, _ = self.cohort(self.f.child, self.f.parent, "PARENT_CHILD", 24)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_NULL_CONTROL_ALARM"):
            self.certificate(ev3, base_ev, certificate_id=h("after-alarm"))

    def test_null_strategies_never_hold_certificates(self):
        sham2 = h("null-control-2")
        insert(self.db, "su1_strategy_version", dict(strategy_id=sham2, codec="SU1", program_hash=self.f.program,
               artifact_hash=self.f.artifact, selector_hash=h("sham3"), calibration_state="MISSING",
               action_contract_hash=h("K3-position"), state_initial_hash=h("state0"), created_sequence=80,
               created_at=4, strategy_role="NULL_CONTROL"))
        ev, _ = self.cohort(sham2, self.sham, "NULL_CONTROL", 24)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_CERTIFICATE_EVIDENCE"):
            self.certificate(ev, ev, strategy_id=sham2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
