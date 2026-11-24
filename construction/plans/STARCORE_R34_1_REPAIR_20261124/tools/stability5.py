"""Recompute the R27 stability projection from independent, already checked natural ACT rows."""
from collections import defaultdict
from datetime import date
from hashlib import sha256
import json
import math
import re


RULE = {
    "source": "LiveShadowsR27.blocks",
    "calendar": "SU1_2020_UTC_1440_1",
    "block_ordinals": 720,
    "minimum_acts_in_block": 20,
    "streak_scope": "last_settled_act_minus_target_ordinal_lt_720",
    "observations": "same_policy_preregistered_frozen_settled_ACT"
}
RULE_HASH = sha256(json.dumps(RULE, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def need(test, message):
    if not test:
        raise ValueError("Q04: " + message)


def ordinal(target):
    need(isinstance(target, str) and re.fullmatch(r"[0-9]{12}", target), "invalid registered issue")
    day = date(int(target[:4]), int(target[4:6]), int(target[6:8]))
    seq = int(target[8:])
    need(1 <= seq <= 1440, "issue outside canonical day")
    return (day - date(2020, 1, 1)).days * 1440 + seq


def reconstruct(rows):
    blocks = defaultdict(lambda: [0, 0])
    ordered = []
    for row in rows:
        need(type(row["hit"]) is bool, "invalid settlement outcome")
        target = ordinal(row["target_key"])
        counts = blocks[target // 720]
        counts[0] += 1
        counts[1] += int(row["hit"])
        ordered.append((target, row["hit"]))
    ordered.sort()
    need(len({target for target, hit in ordered}) == len(ordered), "duplicate settled target")
    eligible = [wins / n for n, wins in blocks.values() if n >= 20]
    longest = current = 0
    if ordered:
        last = ordered[-1][0]
        for target, hit in ordered:
            if last - target < 720:
                current = 0 if hit else current + 1
                longest = max(longest, current)
    return min(eligible) if eligible else None, longest


def verify_stability(pre, result, component, originals):
    """Called after future_act_rows and Q06 have authenticated the forecast, draw and settlement set."""
    need(component["window_rule_hash"] == pre["stability_window_rule_hash"] == RULE_HASH,
         "wrong or unregistered block/streak rule")
    settled = originals.json(result["future_act_ledger_artifact"])
    need(settled["candidate_hash"] == result["candidate_hash"] and
         settled["source_commit"] == result["source_commit"] and settled["protocol_hash"] == result["protocol_hash"] and
         settled["role"] == "PREREGISTERED_FUTURE" and settled["fixture"] is False, "settlement scope differs")
    rows = settled["settled_acts"]
    need(len(rows) == result["future_act_count"], "settlement count differs from the independent calendar")
    worst, longest = reconstruct(rows)
    values = component["values"]
    need(worst is not None and type(values["worst720Block"]) in (int, float) and
         math.isclose(worst, values["worst720Block"], rel_tol=0, abs_tol=1e-12), "worst eligible block differs")
    need(type(values["maxLosingStreak"]) is int and longest == values["maxLosingStreak"],
         "longest recent losing streak differs")
    need(worst >= .26 and longest <= 6, "independent stability projection below the existing thresholds")
