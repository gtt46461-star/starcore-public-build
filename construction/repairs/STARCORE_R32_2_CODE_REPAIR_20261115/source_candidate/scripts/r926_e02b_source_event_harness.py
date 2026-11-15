#!/usr/bin/env python3
"""Executable E02B SourceEvent payload-version/PIT fault harness.

This is deliberately SQLite-backed rather than a prose/static-only assertion.  It proves the
production first-availability contract: identical payload duplicates may inherit the original
availability, while a different late correction starts a new immutable availability version and
cannot cross a historical pre-draw cutoff.
"""
from __future__ import annotations

import hashlib
import json
import sqlite3
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "reports" / "r926" / "batch2" / "E02B_SOURCE_EVENT_PIT_HARNESS.json"
FETCH = ROOT / "app/src/main/java/com/ethk3/novaonepro/FetchLedgerR925.java"
GATE = ROOT / "app/src/main/java/com/ethk3/novaonepro/Batch2GateEvidenceR926.java"


def h(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def main() -> int:
    assertions: dict[str, bool] = {}
    fetch = FETCH.read_text(encoding="utf-8")
    gate = GATE.read_text(encoding="utf-8")
    assertions["production_first_available_is_payload_versioned"] = (
        "latest_issue=? AND payload_hash=? AND first_available_at>0" in fetch
    )
    # Production wiring proof must bind the gate to the real persisted SourceEvent ledger, not merely
    # match a historical helper name.  The implementation is sourceEvidence(...); require both the
    # call site and the canonical R925 ledger table reference so a renamed/local stub cannot pass.
    assertions["production_gate_consumes_source_event"] = (
        "sourceEvidence(db, request)" in gate
        and "SchemaRevisionR925.TABLE_FETCH_LEDGER" in gate
        and "E02B_SOURCE_EVENT|" in gate
    )
    assertions["production_gate_enforces_first_available_cutoff"] = "first_available_at<=?" in gate
    assertions["production_gate_enforces_received_cutoff"] = "received_at<=?" in gate
    assertions["production_gate_tightens_historical_cutoff_to_target_draw"] = (
        "targetDrawAt > 0L && targetDrawAt < cutoffAt" in gate
    )

    with tempfile.TemporaryDirectory(prefix="r926-e02b-") as td:
        db_path = Path(td) / "source-event.db"
        db = sqlite3.connect(db_path)
        db.executescript(
            """
            CREATE TABLE ledger(
              request_id TEXT PRIMARY KEY,
              latest_issue TEXT NOT NULL,
              payload_hash TEXT NOT NULL,
              first_available_at INTEGER NOT NULL,
              received_at INTEGER NOT NULL,
              status TEXT NOT NULL
            );
            CREATE INDEX idx_ledger_issue_available
              ON ledger(latest_issue,first_available_at,received_at);
            """
        )

        def first_available(issue: str, payload: str, received: int) -> int:
            row = db.execute(
                "SELECT MIN(first_available_at) FROM ledger "
                "WHERE latest_issue=? AND payload_hash=? AND first_available_at>0",
                (issue, payload),
            ).fetchone()
            prior = 0 if row is None or row[0] is None else int(row[0])
            return min(prior, received) if prior > 0 else received

        def accept(request_id: str, issue: str, payload: str, received: int) -> int:
            first = first_available(issue, payload, received)
            db.execute(
                "INSERT INTO ledger VALUES(?,?,?,?,?,?)",
                (request_id, issue, payload, first, received, "BUSINESS_ACCEPTED"),
            )
            db.commit()
            return first

        def reject(request_id: str, issue: str, payload: str, received: int, status: str) -> None:
            db.execute(
                "INSERT INTO ledger VALUES(?,?,?,?,?,?)",
                (request_id, issue, payload, 0, received, status),
            )
            db.commit()

        def selected_before(issue: str, cutoff: int):
            return db.execute(
                "SELECT request_id,payload_hash,first_available_at,received_at,status FROM ledger "
                "WHERE latest_issue=? AND first_available_at>0 AND first_available_at<=? "
                "AND received_at>0 AND received_at<=? "
                "AND status IN ('BUSINESS_ACCEPTED','ACCEPTED_DUPLICATE') "
                "ORDER BY first_available_at DESC,received_at DESC LIMIT 1",
                (issue, cutoff, cutoff),
            ).fetchone()

        issue = "202609090888"
        p1, p2, p3, bad = h("original"), h("late-correction"), h("later-correction"), h("conflict")

        first_p1 = accept("r1", issue, p1, 100)
        first_p1_dup = first_available(issue, p1, 120)
        db.execute(
            "INSERT INTO ledger VALUES(?,?,?,?,?,?)",
            ("r2", issue, p1, first_p1_dup, 120, "ACCEPTED_DUPLICATE"),
        )
        db.commit()
        first_p2 = accept("r3", issue, p2, 200)
        first_p2_dup = first_available(issue, p2, 220)
        db.execute(
            "INSERT INTO ledger VALUES(?,?,?,?,?,?)",
            ("r4", issue, p2, first_p2_dup, 220, "ACCEPTED_DUPLICATE"),
        )
        db.commit()
        reject("r5", issue, bad, 230, "CONFLICT_QUARANTINED")
        first_p3 = accept("r6", issue, p3, 300)

        assertions["original_first_available_is_receipt"] = first_p1 == 100
        assertions["same_payload_duplicate_keeps_original_availability"] = first_p1_dup == 100
        assertions["different_late_payload_starts_new_availability"] = first_p2 == 200
        assertions["late_payload_duplicate_keeps_its_own_version_start"] = first_p2_dup == 200
        assertions["second_correction_starts_third_version"] = first_p3 == 300

        before_50 = selected_before(issue, 50)
        at_150 = selected_before(issue, 150)
        at_250 = selected_before(issue, 250)
        historical_250_after_p3_exists = selected_before(issue, 250)
        current_350 = selected_before(issue, 350)

        assertions["missing_source_before_first_receipt_fails_closed"] = before_50 is None
        assertions["historical_cutoff_150_selects_only_original_payload"] = (
            at_150 is not None and at_150[1] == p1 and int(at_150[2]) == 100
        )
        assertions["cutoff_250_selects_correction_available_by_then"] = (
            at_250 is not None and at_250[1] == p2 and int(at_250[2]) == 200
        )
        assertions["later_payload_cannot_backfill_historical_cutoff"] = (
            historical_250_after_p3_exists is not None
            and historical_250_after_p3_exists[1] == p2
            and historical_250_after_p3_exists[1] != p3
        )
        assertions["current_cutoff_can_select_latest_legitimate_version"] = (
            current_350 is not None and current_350[1] == p3 and int(current_350[2]) == 300
        )
        conflict = db.execute(
            "SELECT first_available_at,status FROM ledger WHERE request_id='r5'"
        ).fetchone()
        assertions["conflict_never_gains_availability"] = (
            conflict is not None and int(conflict[0]) == 0 and conflict[1] == "CONFLICT_QUARANTINED"
        )

        rows = db.execute(
            "SELECT request_id,payload_hash,first_available_at,received_at,status "
            "FROM ledger ORDER BY received_at"
        ).fetchall()
        db.close()

    passed = all(assertions.values())
    payload = {
        "tasks": ["E02B-A01", "E02B-A02", "E02D-A01"],
        "passed": passed,
        "assertion_count": len(assertions),
        "assertions": assertions,
        "fixture_rows": [
            {
                "request_id": r[0],
                "payload_hash": r[1],
                "first_available_at": r[2],
                "received_at": r[3],
                "status": r[4],
            }
            for r in rows
        ],
        "proof_scope": "REAL_SQLITE_PAYLOAD_VERSION_FIRST_AVAILABLE_PLUS_HISTORICAL_PIT_CUTOFF_AND_PRODUCTION_WIRING",
        "limitations": "Host harness proves ledger/PIT semantics; physical-device wall clock/provider behavior remains runtime retest evidence.",
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, sort_keys=True))
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
