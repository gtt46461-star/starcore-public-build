"""R12 foundry DDL tests: campaigns, eras, sealed-once, research pool, live shadow, macros."""
from pathlib import Path
import sqlite3
import unittest

from test_schema import Fixture, connect, h, insert, migrate, units

ROOT = Path(__file__).resolve().parents[2]
FOUNDRY = ROOT / "schema" / "strategy_su1_foundry.sql"


def migrate_foundry(db):
    db.execute("BEGIN IMMEDIATE")
    try:
        for st in units(FOUNDRY.read_text(encoding="utf-8")):
            db.execute(st)
        db.execute("COMMIT")
    except BaseException:
        db.execute("ROLLBACK")
        raise


class FoundrySchemaTests(unittest.TestCase):
    def setUp(self):
        self.db = connect()
        migrate(self.db)
        migrate_foundry(self.db)
        self.f = Fixture(self.db)
        self.camp = h("campaign-1")
        insert(self.db, "su1_campaign", dict(campaign_id=self.camp, kind="HISTORY_REPLAY", data_through_sequence=10000,
               config_hash=h("cfg"), config_json="{}", started_at=1, status="RUNNING"))

    def tearDown(self):
        self.db.close()

    def era(self, kind, a, b, camp=None, name=None):
        insert(self.db, "su1_era", dict(era_id=h(name or kind + str(a)), campaign_id=camp or self.camp,
               kind=kind, first_sequence=a, last_sequence=b))
        return h(name or kind + str(a))

    def test_idempotent_install(self):
        before = self.db.execute("SELECT count(*) FROM sqlite_master").fetchone()[0]
        migrate_foundry(self.db)
        self.assertEqual(before, self.db.execute("SELECT count(*) FROM sqlite_master").fetchone()[0])

    def test_era_order_and_sealed_opened_once(self):
        self.era("DEV", 1500, 6174)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_ERA_ORDER"):
            self.era("VAL", 6000, 7874)
        self.era("VAL", 6175, 7874)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_ERA_BEYOND_DATA"):
            self.era("SEALED", 7875, 12000, name="too-far")
        sealed = self.era("SEALED", 7875, 10000)
        insert(self.db, "su1_sealed_open", dict(era_id=sealed, campaign_id=self.camp, strategy_id=self.f.child,
               opened_at=5, result_hash=self.f.blob))
        with self.assertRaises(sqlite3.IntegrityError):          # same era twice
            insert(self.db, "su1_sealed_open", dict(era_id=sealed, campaign_id=self.camp, strategy_id=self.f.parent,
                   opened_at=6, result_hash=self.f.blob))
        cam2 = h("campaign-2")
        insert(self.db, "su1_campaign", dict(campaign_id=cam2, kind="HISTORY_REPLAY", data_through_sequence=12000,
               config_hash=h("cfg2"), config_json="{}", started_at=7, status="RUNNING"))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_SEALED_SPAN_ALREADY_OPENED"):
            self.era("SEALED", 9000, 12000, camp=cam2, name="reuse")
        self.era("SEALED", 10001, 12000, camp=cam2, name="fresh")      # newest data only

    def pool(self, strategy, born=100, entry=None):
        insert(self.db, "su1_research_pool", dict(entry_id=h(entry or strategy), strategy_id=strategy,
               campaign_id=self.camp, arm="GRAMMAR", born_sequence=born, prior_fitness=.25, state="ACTIVE",
               created_at=1))

    def shadow(self, strategy, seq, **kw):
        v = dict(strategy_id=strategy, sequence_ordinal=seq, day_key=20261022, position=3, number_mask=7,
                 captured_at=10, target_open_time=60)
        v.update(kw)
        insert(self.db, "su1_live_shadow", v)

    def test_pool_single_active_and_retire_only(self):
        self.pool(self.f.child)
        with self.assertRaises(sqlite3.IntegrityError):
            self.pool(self.f.child, entry="dup")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_POOL_TRANSITION"):
            self.db.execute("UPDATE su1_research_pool SET prior_fitness=.9 WHERE strategy_id=?", (self.f.child,))
        self.db.execute("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=200,retire_reason='LIVE_LB_LOW'")
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE su1_research_pool SET state='ACTIVE',retired_sequence=NULL,retire_reason=NULL")
        self.pool(self.f.child, entry="second-life")                   # may re-enter as a new entry

    def test_live_shadow_freeze_settle_compact(self):
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_NOT_IN_ACTIVE_POOL"):
            self.shadow(self.f.child, 101)
        self.pool(self.f.child, born=100)
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_NOT_IN_ACTIVE_POOL"):
            self.shadow(self.f.child, 99)                              # before birth
        for bad in (dict(captured_at=60), dict(number_mask=15), dict(number_mask=3), dict(hit=1, settled_at=70)):
            with self.subTest(bad=bad), self.assertRaises(sqlite3.IntegrityError):
                self.shadow(self.f.child, 150, **bad)
        self.shadow(self.f.child, 101)
        self.shadow(self.f.child, 102)
        with self.assertRaises(sqlite3.IntegrityError):
            self.shadow(self.f.child, 101)                             # duplicate freeze
        with self.assertRaises(sqlite3.IntegrityError):
            self.db.execute("UPDATE su1_live_shadow SET hit=1,settled_at=50 WHERE sequence_ordinal=101")  # before open
        self.db.execute("UPDATE su1_live_shadow SET hit=1,settled_at=61 WHERE sequence_ordinal=101")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_SETTLE_ONCE"):
            self.db.execute("UPDATE su1_live_shadow SET hit=0 WHERE sequence_ordinal=101")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_DAY_HAS_UNSETTLED"):
            insert(self.db, "su1_live_daily", dict(strategy_id=self.f.child, day_key=20261022, actions=2, hits=1,
                   periods_observed=1440, closed_at=99))
        self.db.execute("UPDATE su1_live_shadow SET hit=0,settled_at=62 WHERE sequence_ordinal=102")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_COMPACT_REQUIRES_DAILY"):
            self.db.execute("DELETE FROM su1_live_shadow")
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_LIVE_DAILY_MISMATCH"):
            insert(self.db, "su1_live_daily", dict(strategy_id=self.f.child, day_key=20261022, actions=2, hits=2,
                   periods_observed=1440, closed_at=99))
        insert(self.db, "su1_live_daily", dict(strategy_id=self.f.child, day_key=20261022, actions=2, hits=1,
               periods_observed=1440, closed_at=99))
        self.db.execute("DELETE FROM su1_live_shadow")                 # compaction now allowed
        self.assertEqual((2, 1), tuple(self.db.execute("SELECT actions,hits FROM su1_live_daily").fetchone()))

    def test_macro_and_emergent_family(self):
        insert(self.db, "su1_macro", dict(macro_id="M0123456789", canonical_text="RANK(XT(3))", support=3,
               best_fitness=.27, born_campaign=self.camp, last_campaign=self.camp, state="ACTIVE"))
        insert(self.db, "su1_emergent_family", dict(family_id="EMERGENT:M0123456789", macro_id="M0123456789",
               created_campaign=self.camp, created_at=2))
        with self.assertRaises(sqlite3.IntegrityError):
            insert(self.db, "su1_emergent_family", dict(family_id="EMERGENT:Mfake", macro_id="M0123456789",
                   created_campaign=self.camp, created_at=2))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_MACRO_IDENTITY_OR_MONOTONE"):
            self.db.execute("UPDATE su1_macro SET support=2")
        self.db.execute("UPDATE su1_macro SET support=5,last_campaign=?", (self.camp,))

    def test_research_cohorts_do_not_dilute_confirm_ordinals(self):
        insert(self.db, "su1_eval_cohort", dict(cohort_id=h("rf"), strategy_id=self.f.child, parent_strategy_id=None,
               claim_id=h("rf-claim"), evidence_role="RESEARCH_FUTURE", claim_kind="PARENT_CHILD",
               registered_sequence=90, first_issue=1001, planned_opportunities=10, protocol_hash=h("p"),
               protocol_json="{}", family_root_hash=h("family"), family_alpha=.025, global_claim_ordinal=1000000,
               look_schedule_json="[10]", baseline_hash=h("b"), created_at=5))
        insert(self.db, "su1_eval_cohort", dict(cohort_id=h("c2"), strategy_id=self.f.child, parent_strategy_id=self.f.parent,
               claim_id=h("c2-claim"), evidence_role="CONFIRM", claim_kind="PARENT_CHILD", registered_sequence=90,
               first_issue=1001, planned_opportunities=10, protocol_hash=h("p2"), protocol_json="{}",
               family_root_hash=h("family"), family_alpha=.025, global_claim_ordinal=2,
               look_schedule_json="[10]", baseline_hash=h("b"), created_at=5))
        with self.assertRaisesRegex(sqlite3.IntegrityError, "SU1_RESEARCH_CLAIM_ORDINAL"):
            insert(self.db, "su1_eval_cohort", dict(cohort_id=h("rf2"), strategy_id=self.f.child, parent_strategy_id=None,
                   claim_id=h("rf2-claim"), evidence_role="RESEARCH_FUTURE", claim_kind="PARENT_CHILD",
                   registered_sequence=90, first_issue=1001, planned_opportunities=10, protocol_hash=h("p3"),
                   protocol_json="{}", family_root_hash=h("family"), family_alpha=.025, global_claim_ordinal=3,
                   look_schedule_json="[10]", baseline_hash=h("b"), created_at=5))


if __name__ == "__main__":
    unittest.main(verbosity=2)
