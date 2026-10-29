#!/usr/bin/env python3
"""R28 independent audit of the draw data and of the win-rate economics.

Usage:
  python3 r28_independence_audit.py <draws.csv> [out.json]

Input: the repo's research_data/ethpk10_draws_*.csv (column open_number = "03,08,...").
Only numpy is required. Everything is deterministic (fixed seeds).

What it measures (all walk-forward / no future leakage):
  1. Per-position uniformity (chi-square, df=9).
  2. Lag-1 dependence between every (position a at t-1, position b at t) pair:
     100 contingency chi-squares; null distribution by shuffling time order (permutation test).
  3. Replication check: pick the strongest pair on the first half, use it walk-forward on the second half.
  4. Duplicate permutations vs the birthday-problem expectation.
  5. Payout economics: break-even hit rate for fixed K3 at +6.9 / -3.0.
  6. Power: acted periods needed to prove a true hit rate above break-even (single test and 1000-way Bonferroni).
  7. Luck ceiling: median best hit rate among k zero-edge strategies each acting N times.
"""
import csv, json, math, sys
import numpy as np


def load(path):
    rows = [r for r in csv.DictReader(open(path, encoding="utf-8")) if r.get("valid", "1") == "1"]
    draws = np.array([[int(x) for x in r["open_number"].split(",")] for r in rows]) - 1
    return rows, draws


def lag1_stats(d):
    xs = []
    for a in range(10):
        for b in range(10):
            m = np.zeros((10, 10))
            np.add.at(m, (d[:-1, a], d[1:, b]), 1)
            e = m.sum(1, keepdims=True) * m.sum(0, keepdims=True) / m.sum()
            xs.append(float(((m - e) ** 2 / e).sum()))
    return np.array(xs)


def main():
    path = sys.argv[1]
    out_path = sys.argv[2] if len(sys.argv) > 2 else None
    rows, d = load(path)
    n = len(d)
    res = {"input": path, "draws": n}

    # 1 uniformity
    res["position_chi2_df9"] = [round(float(((np.bincount(d[:, p], minlength=10) - n / 10) ** 2 / (n / 10)).sum()), 2) for p in range(10)]
    res["position_chi2_crit5pct"] = 16.92

    # 2 lag-1 dependence + permutation null
    obs = lag1_stats(d)
    rng = np.random.default_rng(7)
    perms = 200
    null_mean, null_exc, null_max = [], [], []
    for _ in range(perms):
        s = lag1_stats(d[rng.permutation(n)])
        null_mean.append(s.mean()); null_exc.append((s > 103.0).sum()); null_max.append(s.max())
    res["lag1"] = {
        "pairs": 100, "df": 81, "crit5pct": 103.0,
        "observed_mean": round(float(obs.mean()), 2), "observed_exceed5pct": int((obs > 103.0).sum()), "observed_max": round(float(obs.max()), 1),
        "perm_null_median_mean": round(float(np.median(null_mean)), 2), "perm_null_median_exceed": float(np.median(null_exc)),
        "perm_p_mean": float(np.mean(np.array(null_mean) >= obs.mean())),
        "perm_p_exceed": float(np.mean(np.array(null_exc) >= (obs > 103.0).sum())),
        "perm_p_max": float(np.mean(np.array(null_max) >= obs.max())),
        "permutations": perms,
    }
    half = n // 2
    res["lag1_by_half"] = {
        "first_exceed5pct": int((lag1_stats(d[:half]) > 103.0).sum()),
        "second_exceed5pct": int((lag1_stats(d[half:]) > 103.0).sum()),
    }

    # 3 replication: strongest pair on first half, walk-forward K3 on second half
    first = lag1_stats(d[:half]).reshape(10, 10)
    a, b = np.unravel_index(int(np.argmax(first)), first.shape)
    m = np.ones((10, 10))
    np.add.at(m, (d[:half - 1, a], d[1:half, b]), 1)
    hits = 0
    for t in range(half, n):
        top = np.argsort(-m[d[t - 1, a]])[:3]
        hits += int(d[t, b] in top)
        m[d[t - 1, a], d[t, b]] += 1
    res["replication"] = {"pair_prev_pos": int(a) + 1, "pair_target_pos": int(b) + 1,
                          "first_half_chi2": round(float(first[a, b]), 1),
                          "second_half_walk_forward_hit_rate": round(hits / (n - half), 4),
                          "second_half_n": n - half, "se": round(math.sqrt(0.3 * 0.7 / (n - half)), 4)}

    # 3b walk-forward "3 most frequent so far" per position, from period 1001
    wf = []
    for p in range(10):
        cnt = np.ones(10); np.add.at(cnt, d[:1000, p], 1); h = 0
        for t in range(1000, n):
            h += int(d[t, p] in np.argsort(-cnt)[:3]); cnt[d[t, p]] += 1
        wf.append(round(h / (n - 1000), 4))
    res["walk_forward_cumulative_top3_by_position"] = {"hit_rate": wf, "mean": round(float(np.mean(wf)), 4),
                                                       "n_each": n - 1000, "se": round(math.sqrt(0.3 * 0.7 / (n - 1000)), 4)}

    # 4 duplicates vs birthday expectation over 10! permutations
    nums = [r["open_number"] for r in rows]
    res["duplicates"] = {"observed": len(nums) - len(set(nums)),
                         "expected_if_random": round(n * (n - 1) / 2 / math.factorial(10), 1)}

    # 5 economics
    stake, gross = 3.0, 9.9
    be = stake / gross
    res["economics"] = {"stake_per_bet": stake, "gross_return_on_hit": gross, "net_hit": gross - stake, "net_miss": -stake,
                        "breakeven_hit_rate": round(be, 4), "ev_per_bet_at_30pct": round(0.3 * gross - stake, 3),
                        "ev_pct_of_stake_at_30pct": round((0.3 * gross - stake) / stake * 100, 2)}

    # 6 power (one-sided, normal approximation, power 0.8)
    z_b = 0.8416
    def need(h, z_a):
        return ((z_a * math.sqrt(be * (1 - be)) + z_b * math.sqrt(h * (1 - h))) / (h - be)) ** 2
    res["power_acted_periods_needed"] = [
        {"true_hit_rate": h, "single_test_alpha5pct": round(need(h, 1.6449)), "bonferroni_1000_candidates": round(need(h, 3.7190))}
        for h in (0.31, 0.32, 0.33, 0.34, 0.36, 0.40)]

    # 7 luck ceiling
    rng = np.random.default_rng(1)
    ceil = []
    for k, acts in ((24, 30), (60, 30), (60, 60), (128, 120), (24, 120), (1000, 500), (33769, 512)):
        reps = 400 if k < 5000 else 40
        best = rng.binomial(acts, 0.3, size=(reps, k)).max(1) / acts
        ceil.append({"strategies": k, "acts_each": acts, "median_best_hit_rate": round(float(np.median(best)), 3)})
    res["luck_ceiling"] = ceil

    txt = json.dumps(res, ensure_ascii=False, indent=1)
    print(txt)
    if out_path:
        open(out_path, "w", encoding="utf-8").write(txt + "\n")


if __name__ == "__main__":
    main()
