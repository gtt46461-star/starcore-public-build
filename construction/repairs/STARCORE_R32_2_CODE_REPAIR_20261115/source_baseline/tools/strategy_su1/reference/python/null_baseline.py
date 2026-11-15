"""SU1 null baseline, negative-control calibration and sample-size reference (R10).

NULL_BASELINE_V1 chooses a position and three numbers from a pre-committed seed and
the target issue only. It never reads history, labels, models or the database. For
ANY draw distribution, a uniform position plus a uniform 3-subset hits with
probability exactly 3/10, so "child vs this sham" tests "better than 30%" on the
same frozen opportunities, with no assumption that the game is fair.

Host reference only: no Android, network or production database access.
"""
import hashlib
import math
import random

DOMAIN = b"SU1_NULL_BASELINE_V1"


def _stream(seed_hex, target_issue):
    if len(seed_hex) != 64 or any(c not in "0123456789abcdef" for c in seed_hex):
        raise ValueError("SEED_COMMITMENT_MUST_BE_64_LOWER_HEX")
    if not isinstance(target_issue, int) or target_issue <= 0:
        raise ValueError("TARGET_ISSUE_ORDINAL")
    block = hashlib.sha256(DOMAIN + b"|" + seed_hex.encode() + b"|" + str(target_issue).encode()).digest()
    counter = 0
    while True:
        for byte in block:
            yield byte
        counter += 1
        block = hashlib.sha256(block + counter.to_bytes(4, "big")).digest()


def _uniform(stream, m):
    """Unbiased integer in [0,m) by rejection on bytes."""
    limit = (256 // m) * m
    for byte in stream:
        if byte < limit:
            return byte % m
    raise AssertionError("unreachable")


def null_baseline_choice(seed_hex, target_issue):
    """Return (position 1..10, ascending numbers tuple of 3 distinct 1..10)."""
    s = _stream(seed_hex, target_issue)
    position = _uniform(s, 10) + 1
    pool = list(range(1, 11))
    picked = []
    for k in range(3):
        j = _uniform(s, 10 - k)
        picked.append(pool.pop(j))
    return position, tuple(sorted(picked))


def hoeffding_radius(alpha, n):
    return math.sqrt(2.0 * math.log(1.0 / alpha) / n)


def look_alpha(global_alpha, family_ordinal, claim_ordinal, look_ordinal):
    k, i, j = family_ordinal, claim_ordinal, look_ordinal
    return global_alpha / (k * (k + 1)) / (i * (i + 1) * j * (j + 1))


def required_opportunities(edge, alpha):
    """Smallest n where an observed mean equal to `edge` clears the lower bound.

    This is the break-even sample size (about 50% detection power when the true
    advantage equals `edge`); planning for higher power needs more periods.
    """
    if not (0 < edge < 1):
        raise ValueError("EDGE")
    return math.ceil(2.0 * math.log(1.0 / alpha) / (edge * edge))


def run_cohort(child, parent, draws, issues, schedule, alpha_of_look):
    """Paired SU1 confirm protocol on full-opportunity denominator.

    child/parent: callables issue -> (position, numbers) or None (missing).
    Returns (confirmed, last_look_index, lower_bound).
    """
    z_sum = 0
    done = 0
    lower = -1.0
    for j, planned in enumerate(schedule, start=1):
        while done < planned:
            issue, draw = issues[done], draws[done]
            c, p = child(issue), parent(issue)
            if c is None or p is None:
                z = -1
            else:
                hc = int(draw[c[0] - 1] in c[1])
                hp = int(draw[p[0] - 1] in p[1])
                z = hc - hp
            z_sum += z
            done += 1
        mean = z_sum / planned
        lower = max(-1.0, mean - hoeffding_radius(alpha_of_look(j), planned))
        if lower > 0:
            return True, j, lower
    return False, len(schedule), lower


def uniform_draws(rng, n):
    out = []
    for _ in range(n):
        d = list(range(1, 11))
        rng.shuffle(d)
        out.append(d)
    return out


def frequency_follower(history):
    """A typical 'hot numbers' heuristic, used only as a no-edge negative control."""
    def choose(index):
        window = history[max(0, index - 30):index]
        counts = [0] * 11
        for d in window:
            counts[d[0]] += 1
        top = sorted(range(1, 11), key=lambda x: (-counts[x], x))[:3]
        return 1, tuple(sorted(top))
    return choose


def null_calibration(cohorts=400, schedule=(200, 400, 800), seed=20261022, global_alpha=0.05):
    """Monte-Carlo: sham-vs-sham and heuristic-vs-sham on fair draws must not confirm.

    Every cohort is charged its own claim ordinal in one family, exactly like the
    production ledger, so the expected number of false confirmations is <= alpha.
    """
    rng = random.Random(seed)
    false_confirms = {"SHAM_PAIR": 0, "HOT_NUMBERS_VS_SHAM": 0}
    n = schedule[-1]
    for c in range(1, cohorts + 1):
        draws = uniform_draws(rng, n)
        issues = list(range(10_000 * c + 1, 10_000 * c + 1 + n))
        a_seed = hashlib.sha256(b"A" + str(c).encode()).hexdigest()
        b_seed = hashlib.sha256(b"B" + str(c).encode()).hexdigest()
        sham_a = lambda issue, s=a_seed: null_baseline_choice(s, issue)
        sham_b = lambda issue, s=b_seed: null_baseline_choice(s, issue)
        index = {issue: k for k, issue in enumerate(issues)}
        follower = frequency_follower(draws)
        hot = lambda issue, f=follower, ix=index: f(ix[issue])
        for name, child in (("SHAM_PAIR", sham_b), ("HOT_NUMBERS_VS_SHAM", hot)):
            claim = 2 * c - (1 if name == "SHAM_PAIR" else 0)
            ok, _, _ = run_cohort(child, sham_a, draws, issues, schedule,
                                  lambda j, i=claim: look_alpha(global_alpha, 1, i, j))
            false_confirms[name] += int(ok)
    return {"cohorts_per_kind": cohorts, "schedule": list(schedule),
            "false_confirms": false_confirms, "global_alpha": global_alpha}


def power_table(edges=(0.01, 0.02, 0.03, 0.05), global_alpha=0.05,
                points=((1, 1, 1), (1, 2, 1), (1, 5, 3), (1, 20, 5), (3, 50, 5))):
    rows = []
    for k, i, j in points:
        a = look_alpha(global_alpha, k, i, j)
        rows.append({"family": k, "claim": i, "look": j, "alpha": a,
                     "required": {f"{e:.2f}": required_opportunities(e, a) for e in edges}})
    return rows


if __name__ == "__main__":
    import json
    print(json.dumps({"null_calibration": null_calibration(),
                      "power_table": power_table()}, indent=2))
