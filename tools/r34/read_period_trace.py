"""Join actual baseline freeze and settlement rows to a hashed enabled calendar and a SQLite snapshot."""
import argparse
import hashlib
import json
import sqlite3
import xml.etree.ElementTree as ET
from datetime import date, timedelta
from pathlib import Path

GAME = "Binance1-PK10"
DAY_ONE = date(2020, 1, 1)
PERIOD = 60000


def issue(ordinal):
    days, offset = divmod(ordinal - 1, 1440)
    return (DAY_ONE + timedelta(days=days)).strftime("%Y%m%d") + f"{offset + 1:04d}"


def ordinal(text):
    if len(text) != 12 or not text.isdigit() or not 1 <= int(text[8:]) <= 1440:
        raise ValueError("ISSUE_FORMAT")
    day = date(int(text[:4]), int(text[4:6]), int(text[6:8]))
    return (day - DAY_ONE).days * 1440 + int(text[8:])


def assignments(path):
    xml = ET.parse(path).getroot()
    data = xml.find("./string[@name='r335_enabled_assignment_journal']")
    status = xml.find("./string[@name='r335_enabled_assignment_status']")
    if data is None or status is None or status.text != "RECORDED":
        raise ValueError("ENABLED_ORIGINAL_UNAVAILABLE")
    lines = data.text.rstrip().splitlines()
    fmt, dropped, previous = lines[0].split("|")
    if fmt != "R335_ENABLED_ASSIGNMENTS_V1":
        raise ValueError("ENABLED_FORMAT")
    if dropped == "0" and previous != "0" * 64:
        raise ValueError("ENABLED_PREFIX")
    sequence = int(dropped)
    result = []
    for line in lines[1:]:
        body, digest = line.rsplit("|", 1)
        n, wall, elapsed, process, state, source, prior = body.split("|")
        sequence += 1
        if (hashlib.sha256(body.encode()).hexdigest() != digest or prior != previous
                or int(n) != sequence or state not in ("1", "0", "R")
                or (result and int(wall) < result[-1][0])):
            raise ValueError("ENABLED_HASH_OR_CLOCK")
        result.append((int(wall), state == "1", digest))
        previous = digest
    return result


def build(db, journal, start, end, anchor, evidence_role):
    if start >= end or end - start > 72 * 3600000:
        raise ValueError("WINDOW_RANGE")
    anchor_row = db.execute("SELECT open_time FROM draw_dependency_source_cp12 WHERE issue=? AND dirty=0", (anchor,)).fetchone()
    if not anchor_row or anchor_row[0] <= 0:
        raise ValueError("ANCHOR_MISSING")
    anchor_wall, anchor_ordinal = anchor_row[0], ordinal(anchor)
    pos = anchor_ordinal + -((anchor_wall - start) // PERIOD)
    events = iter(journal)
    next_event = next(events, None)
    active = None
    result = dict(evidence_role=evidence_role, opportunities=[], predictions=[], draws=[], settlements=[],
                  certificates=[], real=[], clock_source=dict(anchor=anchor, scheduled_open_ms=anchor_wall))
    while (wall := anchor_wall + (pos - anchor_ordinal) * PERIOD) < end:
        while next_event and next_event[0] <= wall - PERIOD:
            active = next_event
            next_event = next(events, None)
        if active is None:
            raise ValueError("ENABLED_ASSIGNMENT_UNKNOWN_AT_DECISION_BOUNDARY")
        target = issue(pos)
        pos += 1
        if not active[1]:
            continue
        row = db.execute("SELECT * FROM r34_s0_shadow WHERE game_code=? AND target_issue=?", (GAME, target)).fetchone()
        draw = db.execute("SELECT draw_hash_v2,first_seen_at,open_time FROM draw_dependency_source_cp12 "
                          "WHERE issue=? AND dirty=0", (target,)).fetchone()
        if draw and draw["open_time"] != wall:
            raise ValueError("PROVIDER_SCHEDULE_DISAGREES:" + target)
        if draw:
            result["draws"].append(dict(game=GAME, target=target, hash=draw[0],
                                        source_result_wall=draw[2], first_available_wall=draw[1],
                                        publication_status="PROVIDER_PUBLICATION_UNCONFIRMED"))
        verified = row and row["state"] in ("FROZEN", "SETTLED")
        if verified and row["deadline_wall"] != wall:
            raise ValueError("PERSISTED_DEADLINE_DISAGREES:" + target)
        result["opportunities"].append(dict(game=GAME, target=target, fixed_deadline_wall=wall,
            fixed_deadline_mono=row["deadline_elapsed"] if row else None, boot_id=row["boot_id"] if row else None,
            enabled_assignment_sha256=active[2], state="PREDICTED" if verified else "MISSED",
            reason=None if verified else (row["state"] if row else "NO_DURABLE_S0_OBSERVATION")))
        if not verified:
            continue
        freeze_id = hashlib.sha256((GAME + "|" + target + "|" + row["input_hash"]).encode()).hexdigest()
        result["predictions"].append(dict(game=GAME, target=target, lane="SHADOW", kind=row["role"],
            freeze_id=freeze_id, policy_hash="FREQ3_POS1_V1", model_hash="FREQ3_POS1_V1", abi=row["numerical_abi"], source_commit=row["source_commit"],
            numbers=[n for n in range(1, 11) if row["mask"] & (1 << n)], position=row["position"],
            input_ordinal=ordinal(row["base_issue"]), target_ordinal=ordinal(target), boot_id=row["boot_id"],
            durable_wall=row["returned_wall"], durable_mono=row["returned_elapsed"]))
        if row["state"] == "SETTLED":
            result["settlements"].append(dict(freeze_id=freeze_id, draw_hash=row["label_hash"],
                                               label_wall=row["settled_wall"], hit=row["hit"]))
    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("snapshot", type=Path)
    p.add_argument("assignments", type=Path)
    p.add_argument("--start-ms", type=int, required=True)
    p.add_argument("--end-ms", type=int, required=True)
    p.add_argument("--anchor-issue", required=True)
    p.add_argument("--evidence-role", choices=["SYNTHETIC", "REPLAY", "DEVICE_SQL_READBACK"], required=True)
    args = p.parse_args()
    with sqlite3.connect(args.snapshot.resolve(strict=True).as_uri() + "?mode=ro", uri=True) as db:
        db.row_factory = sqlite3.Row
        db.execute("BEGIN")
        output = build(db, assignments(args.assignments), args.start_ms, args.end_ms,
                       args.anchor_issue, args.evidence_role)
    print(json.dumps(output, ensure_ascii=False, indent=2))
