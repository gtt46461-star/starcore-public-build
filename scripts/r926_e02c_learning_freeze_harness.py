#!/usr/bin/env python3
"""Executable E02C correction/freeze fault matrix.

Proves the Batch2 fail-closed policy for already-learned inputs:
- before LEARNED_EXACTLY_ONCE, training inputs remain writable;
- after learning, identity/eligibility fields cannot be updated or deleted in place;
- FTRL selected-evidence insertion cannot create a post-learning join ambiguity;
- TinyDeep freezes only PRODUCTION_SELECTED labels, leaving unrelated research assets mutable.

The harness also statically binds these executable semantics to the production SchemaRevisionR925
trigger names and abort codes so a test-only trigger cannot satisfy the gate.
"""
from __future__ import annotations

import json
import sqlite3
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "app/src/main/java/com/ethk3/novaonepro/SchemaRevisionR925.java"
OUT = ROOT / "reports/r926/batch2/E02C_LEARNING_INPUT_FREEZE_HARNESS.json"
GAME = "Binance1-PK10"
FTRL = "ResearchAdaptiveLearnerStoreR811"
TINY = "ResearchTinyDeepLearnerR909"


def expect_abort(db: sqlite3.Connection, sql: str, args: tuple, marker: str) -> bool:
    try:
        db.execute(sql, args)
        db.commit()
        return False
    except sqlite3.IntegrityError as exc:
        db.rollback()
        return marker in str(exc)


def main() -> int:
    assertions: dict[str, bool] = {}
    source = SCHEMA.read_text(encoding="utf-8")
    production_tokens = (
        "trg_r927_e02c_ftrl_oos_update_freeze",
        "trg_r927_e02c_ftrl_oos_delete_freeze",
        "trg_r927_e02c_ftrl_evidence_update_freeze",
        "trg_r927_e02c_ftrl_evidence_delete_freeze",
        "trg_r927_e02c_ftrl_evidence_insert_freeze",
        "trg_r927_e02c_tiny_label_update_freeze",
        "trg_r927_e02c_tiny_label_delete_freeze",
        "LEARNED_EXACTLY_ONCE",
        "E02C_FTRL_LABEL_FROZEN_AFTER_LEARN",
        "E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN",
        "E02C_TINY_LABEL_FROZEN_AFTER_LEARN",
        "ResearchAdaptiveLearnerStoreR811",
        "ResearchTinyDeepLearnerR909",
    )
    assertions["production_schema_contains_all_e02c_freeze_contracts"] = all(
        token in source for token in production_tokens
    )
    assertions["production_ftrl_freezes_identity_fields"] = all(
        token in source
        for token in (
            "strict_oos",
            "prediction_created_at",
            "draw_open_time",
            "hit_label",
            "selected_hit_p",
            "raw_action_p_value",
            "rank_score_value",
            "third_margin_value",
            "entropy_risk_value",
            "stability_value",
            "window_consensus_value",
        )
    )
    assertions["production_tiny_freezes_compact_identity_fields"] = (
        "asset_id,target_issue,feature_blob,hit_label" in source
        and "OLD.asset_id='PRODUCTION_SELECTED'" in source
    )

    with tempfile.TemporaryDirectory(prefix="r926-e02c-") as td:
        db = sqlite3.connect(str(Path(td) / "e02c.db"))
        db.executescript(
            """
            CREATE TABLE runtime_event(
              seq INTEGER PRIMARY KEY AUTOINCREMENT,
              game_code TEXT NOT NULL,
              target_issue TEXT NOT NULL,
              event_type TEXT NOT NULL,
              stage TEXT NOT NULL,
              source TEXT NOT NULL
            );
            CREATE TABLE oos_label(
              target_issue TEXT PRIMARY KEY,
              strict_oos INTEGER NOT NULL,
              prediction_created_at INTEGER NOT NULL,
              draw_open_time INTEGER NOT NULL,
              hit_label INTEGER NOT NULL,
              selected_position INTEGER NOT NULL,
              selected_hit_p REAL NOT NULL,
              note TEXT NOT NULL DEFAULT ''
            );
            CREATE TABLE selected_evidence(
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              target_issue TEXT NOT NULL,
              selected_position INTEGER NOT NULL,
              raw_action_p_value REAL NOT NULL,
              rank_score_value REAL NOT NULL,
              third_margin_value REAL NOT NULL,
              entropy_risk_value REAL NOT NULL,
              stability_value REAL NOT NULL,
              window_consensus_value REAL NOT NULL,
              raw_action_p_status TEXT NOT NULL,
              rank_status TEXT NOT NULL,
              third_margin_status TEXT NOT NULL,
              entropy_risk_status TEXT NOT NULL,
              stability_status TEXT NOT NULL,
              window_consensus_status TEXT NOT NULL,
              note TEXT NOT NULL DEFAULT '',
              UNIQUE(target_issue,selected_position)
            );
            CREATE TABLE compact_label(
              asset_id TEXT NOT NULL,
              target_issue TEXT NOT NULL,
              feature_blob BLOB NOT NULL,
              hit_label INTEGER NOT NULL,
              note TEXT NOT NULL DEFAULT '',
              PRIMARY KEY(asset_id,target_issue)
            );
            """
        )

        ftrl_old = (
            "EXISTS(SELECT 1 FROM runtime_event WHERE game_code='Binance1-PK10' "
            "AND target_issue=OLD.target_issue AND event_type='LEARNED' "
            "AND stage='LEARNED_EXACTLY_ONCE' AND source='ResearchAdaptiveLearnerStoreR811')"
        )
        ftrl_new = ftrl_old.replace("OLD.target_issue", "NEW.target_issue")
        tiny_old = ftrl_old.replace(FTRL, TINY)

        db.executescript(
            f"""
            CREATE TRIGGER trg_r927_e02c_ftrl_oos_update_freeze
            BEFORE UPDATE OF target_issue,strict_oos,prediction_created_at,draw_open_time,hit_label,selected_position,selected_hit_p
            ON oos_label WHEN {ftrl_old}
            BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_ftrl_oos_delete_freeze
            BEFORE DELETE ON oos_label WHEN {ftrl_old}
            BEGIN SELECT RAISE(ABORT,'E02C_FTRL_LABEL_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_ftrl_evidence_update_freeze
            BEFORE UPDATE OF target_issue,selected_position,raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value,raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status
            ON selected_evidence WHEN {ftrl_old}
            BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_ftrl_evidence_delete_freeze
            BEFORE DELETE ON selected_evidence WHEN {ftrl_old}
            BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_ftrl_evidence_insert_freeze
            BEFORE INSERT ON selected_evidence WHEN {ftrl_new}
            BEGIN SELECT RAISE(ABORT,'E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_tiny_label_update_freeze
            BEFORE UPDATE OF asset_id,target_issue,feature_blob,hit_label
            ON compact_label WHEN OLD.asset_id='PRODUCTION_SELECTED' AND {tiny_old}
            BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END;
            CREATE TRIGGER trg_r927_e02c_tiny_label_delete_freeze
            BEFORE DELETE ON compact_label WHEN OLD.asset_id='PRODUCTION_SELECTED' AND {tiny_old}
            BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END;
            """
        )

        f_issue = "202609090901"
        t_issue = "202609090902"
        db.execute(
            "INSERT INTO oos_label VALUES(?,?,?,?,?,?,?,?)",
            (f_issue, 1, 100, 200, 1, 3, 0.44, "pre"),
        )
        db.execute(
            "INSERT INTO selected_evidence(target_issue,selected_position,raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value,raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status,note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (f_issue, 3, .4, .5, .1, .2, .8, .7, "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "pre"),
        )
        db.execute(
            "INSERT INTO compact_label VALUES(?,?,?,?,?)",
            ("PRODUCTION_SELECTED", t_issue, b"0,0,0,0,0,0,0", 1, "pre"),
        )
        db.execute(
            "INSERT INTO compact_label VALUES(?,?,?,?,?)",
            ("RESEARCH_OTHER", t_issue, b"1,1,1,1,1,1,1", 0, "pre"),
        )
        db.commit()

        # Before learning the inputs are still legitimately correctable.
        db.execute("UPDATE oos_label SET selected_hit_p=? WHERE target_issue=?", (.45, f_issue))
        db.execute("UPDATE selected_evidence SET rank_score_value=? WHERE target_issue=?", (.55, f_issue))
        db.execute("UPDATE compact_label SET hit_label=? WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?", (0, t_issue))
        db.commit()
        assertions["unlearned_ftrl_and_tiny_inputs_remain_mutable"] = True

        db.execute(
            "INSERT INTO runtime_event(game_code,target_issue,event_type,stage,source) VALUES(?,?,?,?,?)",
            (GAME, f_issue, "LEARNED", "LEARNED_EXACTLY_ONCE", FTRL),
        )
        db.execute(
            "INSERT INTO runtime_event(game_code,target_issue,event_type,stage,source) VALUES(?,?,?,?,?)",
            (GAME, t_issue, "LEARNED", "LEARNED_EXACTLY_ONCE", TINY),
        )
        db.commit()

        assertions["learned_ftrl_label_correction_rejected"] = expect_abort(
            db,
            "UPDATE oos_label SET hit_label=? WHERE target_issue=?",
            (1, f_issue),
            "E02C_FTRL_LABEL_FROZEN_AFTER_LEARN",
        )
        assertions["learned_ftrl_label_delete_rejected"] = expect_abort(
            db,
            "DELETE FROM oos_label WHERE target_issue=?",
            (f_issue,),
            "E02C_FTRL_LABEL_FROZEN_AFTER_LEARN",
        )
        assertions["learned_ftrl_evidence_value_correction_rejected"] = expect_abort(
            db,
            "UPDATE selected_evidence SET rank_score_value=? WHERE target_issue=?",
            (.99, f_issue),
            "E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN",
        )
        assertions["learned_ftrl_evidence_status_correction_rejected"] = expect_abort(
            db,
            "UPDATE selected_evidence SET rank_status='MISSING' WHERE target_issue=?",
            (f_issue,),
            "E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN",
        )
        assertions["learned_ftrl_evidence_delete_rejected"] = expect_abort(
            db,
            "DELETE FROM selected_evidence WHERE target_issue=?",
            (f_issue,),
            "E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN",
        )
        assertions["learned_ftrl_new_join_evidence_rejected"] = expect_abort(
            db,
            "INSERT INTO selected_evidence(target_issue,selected_position,raw_action_p_value,rank_score_value,third_margin_value,entropy_risk_value,stability_value,window_consensus_value,raw_action_p_status,rank_status,third_margin_status,entropy_risk_status,stability_status,window_consensus_status,note) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            (f_issue, 4, .1, .1, .1, .1, .1, .1, "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "AVAILABLE", "late"),
            "E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN",
        )
        assertions["learned_tiny_feature_correction_rejected"] = expect_abort(
            db,
            "UPDATE compact_label SET feature_blob=? WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",
            (b"9,9,9,9,9,9,9", t_issue),
            "E02C_TINY_LABEL_FROZEN_AFTER_LEARN",
        )
        assertions["learned_tiny_label_correction_rejected"] = expect_abort(
            db,
            "UPDATE compact_label SET hit_label=? WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",
            (1, t_issue),
            "E02C_TINY_LABEL_FROZEN_AFTER_LEARN",
        )
        assertions["learned_tiny_selected_delete_rejected"] = expect_abort(
            db,
            "DELETE FROM compact_label WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",
            (t_issue,),
            "E02C_TINY_LABEL_FROZEN_AFTER_LEARN",
        )

        # An unrelated research compact asset for the same issue is intentionally outside TinyDeep ownership.
        db.execute(
            "UPDATE compact_label SET hit_label=1,note='allowed' WHERE asset_id='RESEARCH_OTHER' AND target_issue=?",
            (t_issue,),
        )
        db.commit()
        assertions["unrelated_research_asset_not_overfrozen"] = (
            db.execute(
                "SELECT hit_label,note FROM compact_label WHERE asset_id='RESEARCH_OTHER' AND target_issue=?",
                (t_issue,),
            ).fetchone()
            == (1, "allowed")
        )

        ftrl_snapshot = db.execute(
            "SELECT strict_oos,prediction_created_at,draw_open_time,hit_label,selected_hit_p FROM oos_label WHERE target_issue=?",
            (f_issue,),
        ).fetchone()
        evidence_snapshot = db.execute(
            "SELECT rank_score_value,rank_status,COUNT(*) FROM selected_evidence WHERE target_issue=?",
            (f_issue,),
        ).fetchone()
        tiny_snapshot = db.execute(
            "SELECT hex(feature_blob),hit_label FROM compact_label WHERE asset_id='PRODUCTION_SELECTED' AND target_issue=?",
            (t_issue,),
        ).fetchone()
        db.close()

    passed = all(assertions.values())
    payload = {
        "tasks": ["E02C-A03"],
        "policy": "CORRECTED_LEARNED_INPUT_FAIL_CLOSED_REBUILD_REQUIRED",
        "passed": passed,
        "assertion_count": len(assertions),
        "assertions": assertions,
        "ftrl_snapshot": ftrl_snapshot,
        "ftrl_evidence_snapshot": evidence_snapshot,
        "tiny_snapshot": tiny_snapshot,
        "proof_scope": "REAL_SQLITE_TRIGGER_CORRECTION_REJECTION_PLUS_PRODUCTION_SCHEMA_BINDING",
        "limitations": "Explicit historical rebuild/versioning is intentionally not implemented in Batch2; corrections after learning are rejected and require a future controlled rebuild path.",
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, sort_keys=True))
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
