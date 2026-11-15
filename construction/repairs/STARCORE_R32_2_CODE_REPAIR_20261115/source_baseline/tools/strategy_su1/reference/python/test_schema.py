"""SU1 host SQLite contract tests; these are not Android/device acceptance tests.

Run: python -m unittest discover -s reference/python -p test_schema.py -v
The small helpers model the REQUIRED repository transactions, not product code.
"""
import hashlib
import math
from pathlib import Path
import sqlite3
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
DDL = ROOT / "schema" / "strategy_su1.sql"


def h(text):
    return hashlib.sha256(str(text).encode("utf-8")).hexdigest()


def radius(alpha, n):
    """Hoeffding-Azuma radius for Z in [-1,1]; adapter must recompute, SQL cannot."""
    return math.sqrt(2.0 * math.log(1.0 / alpha) / n)


def units(sql):
    """Preserve trigger bodies; splitting SQL on semicolons is incorrect."""
    pending = ""
    for line in sql.splitlines(keepends=True):
        pending += line
        if sqlite3.complete_statement(pending):
            yield pending
            pending = ""
    if pending.strip() and not all(x.lstrip().startswith("--") or not x.strip()
                                   for x in pending.splitlines()):
        raise ValueError("incomplete SQL unit")


def connect(path=":memory:"):
    db = sqlite3.connect(path, isolation_level=None)
    db.row_factory = sqlite3.Row
    db.execute("PRAGMA foreign_keys=ON")
    return db


def migrate(db, fail_after=None):
    """Host-only all-or-nothing harness; Android registers bounded units instead."""
    db.execute("BEGIN IMMEDIATE")
    try:
        for i, statement in enumerate(units(DDL.read_text(encoding="utf-8"))):
            db.execute(statement)
            if i == fail_after:
                raise RuntimeError("simulated migration crash")
        if db.execute("PRAGMA foreign_key_check").fetchall():
            raise RuntimeError("foreign key check failed")
        db.execute("COMMIT")
    except BaseException:
        db.execute("ROLLBACK")
        raise


def insert(db, table, values):
    names = list(values)
    return db.execute("INSERT INTO " + table + "(" + ",".join(names) + ") VALUES("
                      + ",".join("?" for _ in names) + ")", [values[x] for x in names])


def exact_replay_or_insert(db, table, key, values):
    """Caller owns shared-writer transaction. No REPLACE and no silent conflict."""
    row = db.execute("SELECT * FROM " + table + " WHERE " + key + "=?",
                     (values[key],)).fetchone()
    if row is None:
        insert(db, table, values)
        return True
    if any(row[k] != v for k, v in values.items()):
        raise ValueError("conflicting replay")
    return False


def claim(db, event_id, owner, now, duration=50):
    # A generation is a fencing token; wall time alone is not an ownership token.
    cur = db.execute("""UPDATE su1_outbox SET state='LEASED',lease_owner=?,lease_until=?,
        lease_generation=lease_generation+1,attempt_count=attempt_count+1,updated_at=?
        WHERE event_id=? AND ((state='PENDING' AND retry_at<=?) OR
        (state='LEASED' AND lease_until<=?))""",
        (owner, now + duration, now, event_id, now, now))
    if cur.rowcount != 1:
        return None
    return db.execute("SELECT lease_generation FROM su1_outbox WHERE event_id=?",
                      (event_id,)).fetchone()[0]


def ack(db, event_id, owner, generation, now):
    return db.execute("""UPDATE su1_outbox SET state='DONE',lease_owner=NULL,lease_until=NULL,
        updated_at=? WHERE event_id=? AND state='LEASED' AND lease_owner=?
        AND lease_generation=? AND lease_until>?""",
        (now, event_id, owner, generation, now)).rowcount


def settle_authorized(db, values):
    """Model the bridge's legacy-terminal guard; fixture table is NOT SU1 DDL.

    Product adapter must also re-read canonical draw/provenance and compare its
    hash/actual position value. No model is invoked here or by the SQL labeler.
    """
    row = db.execute("SELECT status,settlement_hash FROM v57_settlement_task_r36 WHERE target_issue=?",
                     (values["target_issue"],)).fetchone()
    if row is None or row["status"] != "COMPLETED" or row["settlement_hash"] != values["legacy_settlement_hash"]:
        raise ValueError("no matching immutable COMPLETED terminal")
    return exact_replay_or_insert(db, "su1_settlement", "settlement_id", values)


class Fixture:
    def __init__(self, db):
        self.db = db
        self.parent, self.child = h("parent-strategy"), h("child-strategy")
        self.cohort = h("confirm-cohort")
        self.claim_id = h("claim")
        self.eval_protocol = h("eval-protocol")
        self.blob = h("shared-payload")
        insert(db, "su1_blob", dict(blob_hash=self.blob, byte_count=10, codec="TEST",
                                   storage_state="LOCAL", local_locator="fixture/payload", created_at=1))
        self.program = h("program")
        insert(db, "su1_program", dict(program_hash=self.program, grammar_hash=h("grammar"),
              input_schema_hash=h("input"), typed_ir_hash=self.blob, state_schema_hash=h("state"),
              state_byte_limit=1024, operation_limit=1000, created_at=1))
        self.training = h("training-protocol")
        insert(db, "su1_train_protocol", dict(protocol_hash=self.training, program_hash=self.program,
              dataset_hash=h("data"), split_hash=h("split"), protocol_json="{}",
              train_first_sequence=1, train_last_sequence=50, validation_first_sequence=51,
              validation_last_sequence=70, target_contract_hash=h("K3-position"), created_at=2))
        self.artifact = h("artifact")
        insert(db, "su1_model_artifact", dict(artifact_hash=self.artifact, protocol_hash=self.training,
              program_hash=self.program, model_kind="REFERENCE", encoder_hash=h("encoder"),
              weights_hash=self.blob, trained_through_sequence=70, training_report_hash=self.blob, created_at=3))
        for strategy in (self.parent, self.child):
            insert(db, "su1_strategy_version", dict(strategy_id=strategy, codec="SU1", program_hash=self.program,
                  artifact_hash=self.artifact, selector_hash=h(strategy+"selector"), calibration_state="MISSING",
                  action_contract_hash=h("K3-position"), state_initial_hash=h("state0"), created_sequence=80, created_at=4))
        # Family #1 may draw at most global_alpha/(1*2) = .025 from the lifetime root.
        insert(db, "su1_family_budget", dict(family_root_hash=h("family"), family_alpha=.025,
              family_ordinal=1, registration_hash=h("family-registration"), created_at=4))
        insert(db, "su1_eval_cohort", dict(cohort_id=self.cohort, strategy_id=self.child,
              parent_strategy_id=self.parent, claim_id=self.claim_id, evidence_role="CONFIRM",
              claim_kind="PARENT_CHILD", registered_sequence=90, first_issue=1001, planned_opportunities=2,
              protocol_hash=self.eval_protocol, protocol_json="{}", family_root_hash=h("family"),
              family_alpha=.025, global_claim_ordinal=1, look_schedule_json="[2]",
              baseline_hash=h("baseline"), created_at=5))
        insert(db, "su1_look_plan", dict(cohort_id=self.cohort, look_ordinal=1,
              planned_opportunities=2, schedule_hash=h("[2]")))
        db.execute("UPDATE su1_runtime_control SET mode='RESEARCH',updated_at=5 WHERE singleton=1")
        for number in (1, 2):
            self.opportunity(number)

    def opportunity(self, number):
        target = 1000 + number
        insert(self.db, "su1_blob", dict(blob_hash=h("period"+str(number)), byte_count=10, codec="SU1_STATE",
               storage_state="LOCAL", local_locator="fixture/state"+str(number), created_at=9))
        insert(self.db, "su1_opportunity", dict(opportunity_id=h("opp"+str(number)), cohort_id=self.cohort,
              target_issue=target, sequence_ordinal=100+number, opportunity_ordinal=number,
              declared_at=10, deadline_at=1000, source_epoch="epoch-A", eligibility_hash=h("eligible"), role="CONFIRM"))
        insert(self.db, "su1_snapshot", dict(snapshot_hash=h("snapshot"+str(number)), game="TEST",
              target_issue=target, target_open_time=1000, source_max_issue=target-1,
              source_available_at=20, captured_at=30, source_epoch="epoch-A", source_version_hash=h("source"),
              payload_hash=self.blob))

    def prediction(self, number=1, strategy=None, **changes):
        strategy = strategy or self.child
        v = dict(prediction_id=h(str(number)+strategy), opportunity_id=h("opp"+str(number)),
                 strategy_id=strategy, snapshot_hash=h("snapshot"+str(number)), target_issue=1000+number,
                 captured_at=40, position=1, n1=1, n2=2, n3=3, prediction_mask=7, raw_k3_score=.4,
                 probability_hash=self.blob, period_state_hash=h("period"+str(number)),
                 trained_through_sequence=70, input_through_sequence=100,
                 record_hash=h("record"+str(number)+strategy))
        v.update(changes)
        return v

    def freeze(self, number=1, strategy=None, **changes):
        values = self.prediction(number, strategy, **changes)
        insert(self.db, "su1_prediction", values)
        return values["prediction_id"]

    def settlement(self, prediction_id, actual=1, **changes):
        p = self.db.execute("SELECT * FROM su1_prediction WHERE prediction_id=?", (prediction_id,)).fetchone()
        self.outcome(p["opportunity_id"], p["target_issue"])
        v = dict(settlement_id=h("settled"+prediction_id), prediction_id=prediction_id,
                 target_issue=p["target_issue"], canonical_draw_hash=h("draw"+str(p["target_issue"])),
                 legacy_settlement_hash=h("terminal"+str(p["target_issue"])), actual=actual,
                 hit=int(actual in (p["n1"], p["n2"], p["n3"])), label_hash=h("label"+prediction_id), committed_at=1100)
        v.update(changes)
        return v

    def outcome(self, opportunity_id, target_issue):
        draw = h("draw"+str(target_issue))
        if self.db.execute("SELECT 1 FROM su1_opportunity_outcome WHERE opportunity_id=?", (opportunity_id,)).fetchone():
            return
        if not self.db.execute("SELECT 1 FROM su1_blob WHERE blob_hash=?", (draw,)).fetchone():
            insert(self.db, "su1_blob", dict(blob_hash=draw, byte_count=20, codec="CANONICAL_DRAW",
                   storage_state="LOCAL", local_locator="fixture/draw"+str(target_issue), created_at=1050))
        insert(self.db, "su1_opportunity_outcome", dict(opportunity_id=opportunity_id, target_issue=target_issue,
               canonical_draw_hash=draw, legacy_settlement_hash=h("terminal"+str(target_issue)),
               source_epoch="epoch-A", opened_at=1000, completed_at=1050))

    def look(self, **changes):
        for number in (1, 2):
            self.outcome(h("opp"+str(number)), 1000+number)
        v = dict(evidence_hash=h("look1"), cohort_id=self.cohort, claim_id=self.claim_id,
                 global_claim_ordinal=1, look_ordinal=1, protocol_hash=self.eval_protocol,
                 planned_opportunities=2, paired_n=1, parent_missing_n=1, child_missing_n=1,
                 unpaired_n=1, parent_hits=0, child_hits=1, child_only_wins=1, parent_only_wins=0,
                 paired_delta=1., conservative_mean=0., bound_radius=radius(.00625, 2),
                 lower_bound=-1., allocated_alpha=.00625,
                 cumulative_alpha_spent=.00625, coverage=.5, result_state="FAIL",
                 source_chain_hash=h("chain"), metrics_json="{}", recorded_at=1200)
        v.update(changes)
        return v


class SchemaTests(unittest.TestCase):
    def setUp(self):
        self.db = connect()
        migrate(self.db)
        self.f = Fixture(self.db)

    def tearDown(self):
        self.db.close()

    def test_migration_idempotent_preserves_legacy_and_default_closed(self):
        db = connect()
        try:
            db.execute("CREATE TABLE legacy_sentinel(k TEXT PRIMARY KEY,v TEXT)")
            db.execute("INSERT INTO legacy_sentinel VALUES('old','unchanged')")
            db.execute("PRAGMA user_version=624")
            migrate(db)
            before = db.execute("SELECT count(*) FROM sqlite_master").fetchone()[0]
            migrate(db)
            self.assertEqual(before, db.execute("SELECT count(*) FROM sqlite_master").fetchone()[0])
            self.assertEqual("unchanged", db.execute("SELECT v FROM legacy_sentinel").fetchone()[0])
            self.assertEqual(624, db.execute("PRAGMA user_version").fetchone()[0])
            self.assertEqual("DISABLED", db.execute("SELECT mode FROM su1_runtime_control").fetchone()[0])
            self.assertEqual([], db.execute("PRAGMA foreign_key_check").fetchall())
        finally:
            db.close()

    def test_migration_interruption_rolls_back_units_in_host_harness(self):
        db = connect()
        try:
            with self.assertRaisesRegex(RuntimeError, "simulated"):
                migrate(db, fail_after=12)
            self.assertEqual(0, db.execute("SELECT count(*) FROM sqlite_master WHERE name LIKE 'su1_%'").fetchone()[0])
            migrate(db)
            self.assertEqual(1, db.execute("SELECT revision FROM su1_schema_revision").fetchone()[0])
        finally:
            db.close()

    def test_foreign_keys_and_fixed_strategy_identity(self):
        bad = self.f.prediction(strategy=h("unknown"))
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_prediction", bad)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE su1_strategy_version SET selector_hash=? WHERE strategy_id=?", (h("new"), self.f.child))
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_prediction").fetchone()[0])

    def test_freeze_and_outbox_are_atomic_and_exact_replay_is_noop(self):
        values = self.f.prediction()
        self.db.execute("BEGIN IMMEDIATE")
        self.assertTrue(exact_replay_or_insert(self.db, "su1_prediction", "prediction_id", values))
        self.db.execute("ROLLBACK")
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_outbox").fetchone()[0])
        self.db.execute("BEGIN IMMEDIATE")
        self.assertTrue(exact_replay_or_insert(self.db, "su1_prediction", "prediction_id", values))
        self.assertFalse(exact_replay_or_insert(self.db, "su1_prediction", "prediction_id", values))
        self.db.execute("COMMIT")
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM su1_outbox").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM su1_pending_settlement").fetchone()[0])
        bad = dict(values, raw_k3_score=.99)
        with self.assertRaisesRegex(ValueError, "conflicting"):
            exact_replay_or_insert(self.db, "su1_prediction", "prediction_id", bad)
        columns = list(values)
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("INSERT OR REPLACE INTO su1_prediction("+",".join(columns)+") VALUES("+
                            ",".join("?" for _ in columns)+")", [bad[x] for x in columns])
        self.assertEqual(.4, self.db.execute("SELECT raw_k3_score FROM su1_prediction").fetchone()[0])

    def test_late_or_future_input_invalid_k3_and_uncalibrated_probability_rejected(self):
        variants = (dict(captured_at=1000), dict(trained_through_sequence=101),
                    dict(input_through_sequence=101), dict(prediction_mask=15),
                    dict(n1=2, n2=1), dict(selected_hit_p=.7, calibration_hash=None))
        for changes in variants:
            with self.subTest(changes=changes), self.assertRaises(sqlite3.IntegrityError):
                self.f.freeze(**changes)
        self.db.execute("UPDATE su1_runtime_control SET mode='DISABLED' WHERE singleton=1")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_DISABLED"):
            self.f.freeze()

    def test_settle_exact_label_no_recompute_duplicate_conflict_and_transaction(self):
        p = self.f.freeze()
        for changes in (dict(hit=0), dict(target_issue=999), dict(committed_at=900)):
            with self.subTest(changes=changes), self.assertRaises(sqlite3.IntegrityError):
                insert(self.db, "su1_settlement", self.f.settlement(p, **changes))
        v = self.f.settlement(p)
        self.db.execute("BEGIN IMMEDIATE")
        insert(self.db, "su1_settlement", v)
        self.db.execute("ROLLBACK")
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_outbox WHERE event_type='SETTLED'").fetchone()[0])
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM su1_pending_settlement").fetchone()[0])
        self.assertTrue(exact_replay_or_insert(self.db, "su1_settlement", "settlement_id", v))
        self.assertFalse(exact_replay_or_insert(self.db, "su1_settlement", "settlement_id", v))
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_pending_settlement").fetchone()[0])
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_settlement", dict(v, settlement_id=h("other-settlement"), label_hash=h("other-label")))
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM su1_prediction WHERE prediction_id=?", (p,))

    def test_bridge_requires_completed_terminal_exact_hash(self):
        self.db.execute("CREATE TABLE v57_settlement_task_r36(target_issue INTEGER PRIMARY KEY,status TEXT,settlement_hash TEXT)")
        p = self.f.freeze()
        v = self.f.settlement(p)
        with self.assertRaisesRegex(ValueError, "COMPLETED"):
            settle_authorized(self.db, v)
        self.db.execute("INSERT INTO v57_settlement_task_r36 VALUES(?, 'RUNNING', ?)",
                        (v["target_issue"], v["legacy_settlement_hash"]))
        with self.assertRaises(ValueError):
            settle_authorized(self.db, v)
        self.db.execute("UPDATE v57_settlement_task_r36 SET status='COMPLETED'")
        with self.assertRaises(ValueError):
            settle_authorized(self.db, dict(v, legacy_settlement_hash=h("wrong-hash")))
        self.db.execute("BEGIN IMMEDIATE")
        self.assertTrue(settle_authorized(self.db, v))
        self.db.execute("COMMIT")
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM su1_settlement").fetchone()[0])

    def test_lease_reclaim_stale_ack_and_done_is_terminal(self):
        p = self.f.freeze()
        event = "FROZEN:"+p
        gen1 = claim(self.db, event, "bootA:worker1", 100)
        self.assertEqual(1, gen1)
        self.assertIsNone(claim(self.db, event, "bootA:worker2", 120))
        gen2 = claim(self.db, event, "bootB:worker2", 151)
        self.assertEqual(2, gen2)
        self.assertEqual(0, ack(self.db, event, "bootA:worker1", gen1, 152))
        self.assertEqual(1, ack(self.db, event, "bootB:worker2", gen2, 152))
        self.assertIsNone(claim(self.db, event, "worker3", 300))
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE su1_outbox SET state='PENDING' WHERE event_id=?", (event,))

    def test_effect_and_ack_same_transaction_crash_then_retry(self):
        p = self.f.freeze()
        s = self.f.settlement(p)
        insert(self.db, "su1_settlement", s)
        event = "SETTLED:"+s["settlement_id"]
        gen = claim(self.db, event, "worker", 1200)
        feedback = dict(feedback_hash=h("residual"), settlement_id=s["settlement_id"], kind="RESIDUAL",
                        diagnostic_contract_hash=h("diag-v1"), payload_hash=self.f.blob, created_at=1201)
        self.db.execute("BEGIN IMMEDIATE")
        insert(self.db, "su1_feedback", feedback)
        self.assertEqual(1, ack(self.db, event, "worker", gen, 1201))
        self.db.execute("ROLLBACK")
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_feedback").fetchone()[0])
        self.assertEqual("LEASED", self.db.execute("SELECT state FROM su1_outbox WHERE event_id=?", (event,)).fetchone()[0])
        self.db.execute("BEGIN IMMEDIATE")
        exact_replay_or_insert(self.db, "su1_feedback", "feedback_hash", feedback)
        self.assertEqual(1, ack(self.db, event, "worker", gen, 1202))
        self.db.execute("COMMIT")
        self.assertEqual(1, self.db.execute("SELECT count(*) FROM su1_feedback").fetchone()[0])

    def test_pair_denominator_missing_periods_and_alpha_cannot_be_discarded(self):
        parent = self.f.freeze(strategy=self.f.parent, n1=4, n2=5, n3=6, prediction_mask=56)
        child = self.f.freeze()
        insert(self.db, "su1_settlement", self.f.settlement(parent))
        insert(self.db, "su1_settlement", self.f.settlement(child))
        # Opportunity 2 was declared but neither side produced a result: retained as -1.
        v = self.f.look()
        insert(self.db, "su1_evidence_look", v)
        row = self.db.execute("SELECT paired_delta,conservative_mean,coverage FROM su1_evidence_look").fetchone()
        self.assertEqual((1., 0., .5), tuple(row))
        for changes in (dict(evidence_hash=h("forged"), look_ordinal=2, paired_n=2, unpaired_n=0,
                             parent_missing_n=0, child_missing_n=0, coverage=1.),
                        dict(evidence_hash=h("overspend"), look_ordinal=2, allocated_alpha=.02),
                        dict(evidence_hash=h("drop"), look_ordinal=2, planned_opportunities=1)):
            with self.subTest(changes=changes), self.assertRaises(sqlite3.IntegrityError):
                insert(self.db, "su1_evidence_look", self.f.look(**changes))
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_certificate", dict(certificate_id=h("invalid-cert"), strategy_id=self.f.child,
                   evidence_hash=v["evidence_hash"], policy_hash=h("policy"), source_chain_hash=h("chain"), issued_at=1300))

    def test_child_and_legacy_counts_never_seed_future_ledger(self):
        p = self.f.freeze(strategy=self.f.parent)
        insert(self.db, "su1_settlement", self.f.settlement(p))
        count = self.db.execute("""SELECT count(*) FROM su1_settlement s JOIN su1_prediction p
            ON p.prediction_id=s.prediction_id WHERE p.strategy_id=?""", (self.f.child,)).fetchone()[0]
        self.assertEqual(0, count)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_MAINLINE_DISABLED"):
            insert(self.db, "su1_mainline_binding", dict(target_issue=1001, strategy_id=self.f.child,
                   certificate_id=h("none"), prediction_id=p, adapter_contract_hash=h("adapter"),
                   original_owner_receipt_hash=h("owner"), bound_at=100))

    def test_no_posthoc_look_schedule_or_complete_case_look(self):
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LOOK_PLAN_MUST_PRECEDE"):
            insert(self.db, "su1_look_plan", dict(cohort_id=self.f.cohort, look_ordinal=2,
                   planned_opportunities=1, schedule_hash=h("changed")))
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_evidence_look", self.f.look(planned_opportunities=1,
                   paired_n=0, unpaired_n=1, parent_hits=0, child_hits=0,
                   child_only_wins=0, paired_delta=None, conservative_mean=-1., coverage=0.))

    def test_look_waits_for_legal_frozen_predictions_to_settle(self):
        self.f.freeze()
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LOOK_PENDING_SETTLEMENT"):
            insert(self.db, "su1_evidence_look", self.f.look(paired_n=0, unpaired_n=2,
                   parent_missing_n=2, child_missing_n=2, parent_hits=0, child_hits=0,
                   child_only_wins=0, paired_delta=None, conservative_mean=-1., coverage=0.))

    def test_both_missing_still_requires_and_preserves_canonical_outcome(self):
        # Prepare a valid zero-coverage look, then simulate its outcome transaction
        # never having committed. Even with no predictions, this look must wait.
        self.db.execute("BEGIN IMMEDIATE")
        v = self.f.look(paired_n=0, unpaired_n=2, parent_missing_n=2, child_missing_n=2,
                        parent_hits=0, child_hits=0, child_only_wins=0, paired_delta=None,
                        conservative_mean=-1., coverage=0., result_state="INSUFFICIENT")
        self.db.execute("ROLLBACK")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LOOK_CANONICAL_PREFIX_NOT_COMPLETE"):
            insert(self.db, "su1_evidence_look", v)
        for number in (1, 2):
            self.f.outcome(h("opp"+str(number)), 1000+number)
        insert(self.db, "su1_evidence_look", v)
        self.assertEqual(2, self.db.execute("SELECT count(*) FROM su1_opportunity_outcome").fetchone()[0])
        self.assertEqual(0, self.db.execute("SELECT count(*) FROM su1_prediction").fetchone()[0])
        self.assertEqual((2, 2, -1.), tuple(self.db.execute("SELECT planned_opportunities,unpaired_n,conservative_mean FROM su1_evidence_look").fetchone()))
        with self.assertRaises(sqlite3.IntegrityError):
            self.f.freeze()  # Even a backdated timestamp cannot freeze after outcome.
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("DELETE FROM su1_opportunity_outcome")

    def test_archive_requires_receipt_and_released_pin(self):
        pin = h("pin")
        insert(self.db, "su1_retention_pin", dict(pin_id=pin, object_kind="BLOB", object_id=self.f.blob,
               reason="OPEN_COHORT", created_at=1))
        receipt = h("archive")
        insert(self.db, "su1_archive_receipt", dict(receipt_id=receipt, archive_locator="archive/test",
               manifest_hash=h("manifest"), dependency_graph_hash=h("graph"), verified_at=2000,
               replay_verified=1, manifest_json="{}"))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_BLOB_PINNED"):
            self.db.execute("UPDATE su1_blob SET storage_state='ARCHIVED',local_locator=NULL,archive_receipt_id=?",
                            (receipt,))
        self.db.execute("UPDATE su1_retention_pin SET released_at=2001 WHERE pin_id=?", (pin,))
        self.db.execute("UPDATE su1_blob SET storage_state='ARCHIVED',local_locator=NULL,archive_receipt_id=?", (receipt,))
        self.assertEqual("ARCHIVED", self.db.execute("SELECT storage_state FROM su1_blob").fetchone()[0])
        self.assertEqual([], self.db.execute("PRAGMA foreign_key_check").fetchall())
        # The actual archive bytes/hash verification is an adapter duty, not proved by this row.

    def test_pending_event_survives_process_reopen(self):
        with tempfile.TemporaryDirectory() as directory:
            path = str(Path(directory)/"crash.db")
            db = connect(path)
            migrate(db)
            f = Fixture(db)
            p = f.freeze()
            event = "FROZEN:"+p
            claim(db, event, "boot-old", 100)
            db.close()
            db = connect(path)
            try:
                self.assertEqual(2, claim(db, event, "boot-new", 151))
                self.assertEqual(1, ack(db, event, "boot-new", 2, 152))
            finally:
                db.close()


if __name__ == "__main__":
    unittest.main(verbosity=2)
