"""SU1 Historical Replay Engine (HRE) reference — R11.

Fast, strictly causal walk-forward replay over stored history so that training,
settlement, feedback, mutation, fusion and selection run at CPU speed instead of
waiting one real draw per minute. Every decision for target t is computed only from
draws [0, t); its label is attached only after the decision row is complete.

Contents
  load_draws            CSV -> (periods, D[T,10]) with permutation/gap checks
  ExpertBank            causal expert score tensors (frequency windows, omission,
                        same-position Markov, momentum) computed once and shared
  Genome                a complete strategy: expert weights, temperature, cross-
                        intersection feature mask, selector hyper-parameters
  evaluate              base strategy -> Top6/7/8/9 x multi-position cross-
                        intersection features -> rolling z -> walk-forward ridge
                        logistic opportunity selection at fixed coverage
  evolve                generations of mutation / crossover / fusion on DEV,
                        champion lineage chosen on VAL, SEALED opened once

Host reference only. Nothing here writes to a production DB or authorizes REAL.
"""
from __future__ import annotations

import csv
import dataclasses
import hashlib
import json
import math
import random
import time

import numpy as np

K = 3
ROLL_WINDOWS = (180, 360, 720, 1200)
EXPERTS = ("r12", "r24", "r48", "r96", "r360", "omission", "markov720", "momentum")


# ----------------------------------------------------------------------------- data
def load_draws(path):
    """Return periods (int array), open_ms (int array), D (int8 [T,10], numbers 1..10)."""
    rows = []
    with open(path, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            nums = [int(x) for x in r["open_number"].replace('"', "").split(",")]
            if sorted(nums) != list(range(1, 11)):
                raise ValueError("NOT_A_PERMUTATION " + r["period"])
            if r.get("valid", "1") not in ("1", "true", "True"):
                continue
            rows.append((int(r["open_time_utc_ms"]), int(r["period"]), nums))
    rows.sort()
    periods = [p for _, p, _ in rows]
    if len(set(periods)) != len(periods):
        raise ValueError("DUPLICATE_PERIOD")
    ms = np.array([t for t, _, _ in rows], dtype=np.int64)
    D = np.array([n for _, _, n in rows], dtype=np.int8)
    return np.array(periods, dtype=np.int64), ms, D


def data_report(ms, D):
    gaps = int(np.sum(np.diff(ms) != 60000)) if len(ms) > 1 else 0
    counts = np.zeros((10, 10))
    for p in range(10):
        counts[p] = np.bincount(D[:, p] - 1, minlength=10)
    exp = len(D) / 10.0
    chi2 = float(((counts - exp) ** 2 / exp).sum())
    return {"periods": int(len(D)), "minute_gaps": gaps, "position_number_chi2": chi2,
            "chi2_df": 90, "chi2_note": "df=90; values near 90 are what uniform draws produce"}


# ------------------------------------------------------------------------- experts
class ExpertBank:
    """Causal per-(t, position, number) expert scores, z-normalised across numbers.

    Z[e, t, p, n] uses only rows < t. Rank[e, t, p, n] is the 0-based rank of n
    (0 = best) inside position p for expert e, ties broken by lower number.
    """

    def __init__(self, D):
        T = len(D)
        self.T = T
        X = np.zeros((T, 10, 10), dtype=np.float32)
        X[np.arange(T)[:, None], np.arange(10)[None, :], D.astype(int) - 1] = 1.0
        C = np.zeros((T + 1, 10, 10), dtype=np.float32)
        np.cumsum(X, axis=0, out=C[1:])
        raw = {}
        for w in (12, 24, 48, 96, 360):
            lo = np.maximum(np.arange(T) - w, 0)
            raw["r%d" % w] = (C[np.arange(T)] - C[lo]) / np.maximum(np.arange(T) - lo, 1)[:, None, None]
        # omission (periods since last seen at p), causal
        gap = np.zeros((T, 10, 10), dtype=np.float32)
        last = np.full((10, 10), -1, dtype=np.int64)
        for t in range(T):
            gap[t] = np.where(last >= 0, t - last, t + 1)
            last[np.arange(10), D[t] - 1] = t
        raw["omission"] = np.log1p(gap)
        # same-position Markov over a sliding 720-transition window
        mk = np.zeros((T, 10, 10), dtype=np.float32)
        M = np.zeros((10, 10, 10), dtype=np.float32)
        W = 720
        for t in range(T):
            if t >= 2:
                a, b = D[t - 2] - 1, D[t - 1] - 1
                M[np.arange(10), a, b] += 1
                if t - 2 - W >= 0:
                    a0, b0 = D[t - 2 - W] - 1, D[t - 1 - W] - 1
                    M[np.arange(10), a0, b0] -= 1
            if t >= 1:
                prev = D[t - 1] - 1
                rows = M[np.arange(10), prev]  # [10 positions, 10 next numbers]
                mk[t] = (rows + 1.0) / (rows.sum(axis=1, keepdims=True) + 10.0)
            else:
                mk[t] = 0.1
        raw["markov720"] = mk
        raw["momentum"] = raw["r24"] - raw["r360"]
        Z = np.zeros((len(EXPERTS), T, 10, 10), dtype=np.float32)
        for i, e in enumerate(EXPERTS):
            v = raw[e]
            mu = v.mean(axis=2, keepdims=True)
            sd = v.std(axis=2, keepdims=True) + 1e-6
            Z[i] = (v - mu) / sd
        self.Z = Z
        # rank with deterministic tie-break toward the lower number
        tie = (np.arange(10, dtype=np.float32) * 1e-4)[None, None, None, :]
        order = np.argsort(-(Z - tie), axis=3, kind="stable")
        self.Rank = np.argsort(order, axis=3, kind="stable").astype(np.int8)
        self.X = X


# -------------------------------------------------------------------------- genome
FEATURES = (
    "avg6", "avg7", "avg8", "avg9", "min6", "min7", "min8", "min9", "tighten",
    "dest_margin", "pos_clarity", "pos_ambiguity", "third_support", "third_gap",
    "disagreement", "predictability", "rank1_margin", "top6_x_third", "set_x_pos")


@dataclasses.dataclass
class Genome:
    weights: tuple            # one weight per expert, sign allowed
    temperature: float        # softmax temperature for position mass
    feature_mask: tuple       # 0/1 per FEATURES entry
    use_rolling_z: bool
    C: float                  # inverse ridge strength (user's historical version: 0.20)
    pool_frac: float          # base-score pool (user's version: 0.25)
    coverage: float           # final action coverage (user's version: 0.15)
    train_window: int         # user's version: 2500
    retrain_every: int
    origin: str = "seed"

    def key(self):
        blob = json.dumps(dataclasses.asdict(self) | {"origin": ""}, sort_keys=True)
        return hashlib.sha256(blob.encode()).hexdigest()[:16]


def seed_cross_intersection():
    """The user's historical cross-intersection version as a starting genome."""
    return Genome(weights=(0.0, 0.3, 0.3, 0.2, 0.2, -0.2, 0.3, 0.1), temperature=1.0,
                  feature_mask=tuple([1] * len(FEATURES)), use_rolling_z=True, C=0.20,
                  pool_frac=0.25, coverage=0.15, train_window=2500, retrain_every=250,
                  origin="seed:XI_FIXED_RANK1_K3")


# ----------------------------------------------------------------------- features
def _softmax(x, axis, temp):
    z = x / temp
    z = z - z.max(axis=axis, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=axis, keepdims=True)


def base_and_features(bank, g):
    Z, Rank = bank.Z, bank.Rank
    T = bank.T
    w = np.asarray(g.weights, dtype=np.float32)[:, None, None, None]
    S = (w * Z).sum(axis=0)                              # [T,10,10]
    P = _softmax(S, 2, g.temperature)                    # per-position number distribution
    order = np.argsort(-(S - np.arange(10) * 1e-6), axis=2, kind="stable")
    top = order[:, :, :4]                                # top4 per position
    k3 = top[:, :, :3]
    mass = np.take_along_axis(P, k3, axis=2).sum(axis=2) # [T,10] top3 mass per position
    pos_rank = np.argsort(-(mass - np.arange(10) * 1e-9), axis=1, kind="stable")
    p1, p2 = pos_rank[:, 0], pos_rank[:, 1]
    ti = np.arange(T)
    K3 = k3[ti, p1]                                      # [T,3] numbers 0..9
    base_score = mass[ti, p1]
    f = {}
    rk = Rank[:, ti[:, None], p1[:, None], K3]           # [E,T,3] expert ranks of the K3 numbers
    for kk in (6, 7, 8, 9):
        sup = (rk < kk).mean(axis=2)                     # [E,T]
        f["avg%d" % kk] = sup.mean(axis=0)
        f["min%d" % kk] = sup.min(axis=0)
    f["tighten"] = f["avg6"] / np.maximum(f["avg9"], 1e-3)
    # same K3 projected onto every position
    Pk = P[:, :, :]
    proj = np.take_along_axis(Pk, np.repeat(K3[:, None, :], 10, axis=1), axis=2).sum(axis=2)  # [T,10]
    own = proj[ti, p1]
    proj_other = proj.copy()
    proj_other[ti, p1] = -1
    f["dest_margin"] = own - proj_other.max(axis=1)
    f["pos_clarity"] = mass[ti, p1] - mass[ti, p2]
    f["pos_ambiguity"] = (mass >= (mass[ti, p1] - 0.01)[:, None]).sum(axis=1).astype(np.float32)
    third = K3[:, 2]
    f["third_support"] = (Rank[:, ti, p1, third] < 6).mean(axis=0)
    s_sorted = np.take_along_axis(S[ti, p1], top[ti, p1], axis=1)  # [T,4]
    f["third_gap"] = (s_sorted[:, 1] - s_sorted[:, 2]) - (s_sorted[:, 2] - s_sorted[:, 3])
    f["disagreement"] = (rk < 6).mean(axis=2).std(axis=0)
    prow = P[ti, p1]
    f["predictability"] = 1.0 + (prow * np.log(prow + 1e-12)).sum(axis=1) / math.log(10)
    f["rank1_margin"] = s_sorted[:, 2] - s_sorted[:, 3]
    f["top6_x_third"] = f["avg6"] * f["third_support"]
    f["set_x_pos"] = f["avg6"] * f["dest_margin"]
    cols = [f[n].astype(np.float64) for n, m in zip(FEATURES, g.feature_mask) if m]
    F = np.stack(cols, axis=1) if cols else np.zeros((T, 0))
    if g.use_rolling_z and F.shape[1]:
        F = np.concatenate([F] + [_rolling_z(F, w) for w in ROLL_WINDOWS], axis=1)
    return p1, K3 + 1, base_score.astype(np.float64), F


def _rolling_z(F, w):
    """z of row t against rows [t-w, t) only (strictly causal)."""
    T = len(F)
    c1 = np.vstack([np.zeros((1, F.shape[1])), np.cumsum(F, axis=0)])
    c2 = np.vstack([np.zeros((1, F.shape[1])), np.cumsum(F * F, axis=0)])
    t = np.arange(T)
    lo = np.maximum(t - w, 0)
    n = np.maximum(t - lo, 1)[:, None]
    mu = (c1[t] - c1[lo]) / n
    var = np.maximum((c2[t] - c2[lo]) / n - mu * mu, 1e-9)
    z = (F - mu) / np.sqrt(var)
    z[t < 30] = 0.0
    return np.clip(z, -6, 6)


# ------------------------------------------------------------------------ learner
def _fit_logistic(X, y, C, iters=12):
    """Ridge logistic by Newton; penalty 1/(2C)|w|^2 on non-intercept terms."""
    n, d = X.shape
    Xb = np.hstack([np.ones((n, 1)), X])
    w = np.zeros(d + 1)
    lam = np.full(d + 1, 1.0 / max(C, 1e-6))
    lam[0] = 0.0
    for _ in range(iters):
        z = np.clip(Xb @ w, -30, 30)
        p = 1.0 / (1.0 + np.exp(-z))
        g = Xb.T @ (p - y) + lam * w
        H = (Xb * (p * (1 - p))[:, None]).T @ Xb + np.diag(lam + 1e-9)
        step = np.linalg.solve(H, g)
        w -= step
        if np.abs(step).max() < 1e-8:
            break
    return w


def _causal_quantile(values, valid, t, q, window):
    lo = max(0, t - window)
    v = values[lo:t][valid[lo:t]]
    if len(v) < 30:
        return None
    return float(np.quantile(v, q))


# ----------------------------------------------------------------------- evaluate
@dataclasses.dataclass
class Replay:
    act: np.ndarray        # bool [T]  final action taken
    hit: np.ndarray        # bool [T]  K3 hit at chosen position (defined for every t)
    pool: np.ndarray       # bool [T]  in base-score candidate pool
    position: np.ndarray
    numbers: np.ndarray
    seconds: float


def evaluate(bank, D, g, start, end, warmup=1500):
    """Walk-forward replay for targets in [start, end). Decision at t uses rows < t only."""
    t0 = time.perf_counter()
    T = bank.T
    p1, K3, base, F = base_and_features(bank, g)
    hit = (K3 == D[np.arange(T), p1][:, None]).any(axis=1)
    pool = np.zeros(T, dtype=bool)
    act = np.zeros(T, dtype=bool)
    prob = np.full(T, np.nan)
    keep_frac = min(1.0, g.coverage / g.pool_frac)
    in_pool = causal_pool(base, g.pool_frac, end)
    w = None
    mu = sd = None
    last_fit = None
    for t in range(max(start, warmup), end):
        if not in_pool[t]:
            continue
        pool[t] = True
        if w is None or t - last_fit >= g.retrain_every:
            last_fit = t
            lo = max(0, t - g.train_window)
            idx = np.arange(lo, t)
            idx = idx[in_pool[lo:t]]   # labels of rows < t are already settled
            if len(idx) >= 60 and F.shape[1]:
                Xtr = F[idx]
                mu = Xtr.mean(axis=0)
                sd = Xtr.std(axis=0) + 1e-9
                w = _fit_logistic((Xtr - mu) / sd, hit[idx].astype(float), g.C)
        if w is None:
            continue
        x = (F[t] - mu) / sd
        prob[t] = 1.0 / (1.0 + math.exp(-float(np.clip(w[0] + x @ w[1:], -30, 30))))
        q = _causal_quantile(prob, ~np.isnan(prob), t, 1.0 - keep_frac, 600)
        act[t] = q is not None and prob[t] >= q
    return Replay(act=act, hit=hit, pool=pool, position=p1 + 1, numbers=K3,
                  seconds=time.perf_counter() - t0)


def causal_pool(base, pool_frac, end):
    """pool[t] = base[t] >= (1-pool_frac) quantile of base[t-720:t]; strictly causal."""
    out = np.zeros(len(base), dtype=bool)
    for t in range(30, end):
        lo = max(0, t - 720)
        out[t] = base[t] >= np.quantile(base[lo:t], 1.0 - pool_frac)
    return out


def summarize(r, start, end, blocks=4):
    a = r.act[start:end]
    h = r.hit[start:end]
    n = int(a.sum())
    k = int((a & h).sum())
    base_all = float(h.mean()) if end > start else float("nan")
    out = {"periods": end - start, "actions": n, "hits": k,
           "hit_rate": k / n if n else None, "coverage": n / (end - start),
           "always_rank1_k3_hit_rate": base_all,
           "wilson_lower_95": wilson_lower(k, n), "binomial_p_vs_30": binom_sf(k, n, 0.3)}
    edges = np.linspace(start, end, blocks + 1).astype(int)
    out["block_hit_rates"] = []
    for b0, b1 in zip(edges[:-1], edges[1:]):
        aa, hh = r.act[b0:b1], r.hit[b0:b1]
        nn = int(aa.sum())
        out["block_hit_rates"].append(round(int((aa & hh).sum()) / nn, 4) if nn else None)
    return out


def wilson_lower(k, n, z=1.6449):
    if n == 0:
        return 0.0
    p = k / n
    den = 1 + z * z / n
    c = p + z * z / (2 * n)
    r = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n))
    return (c - r) / den


def binom_sf(k, n, p):
    """P[X >= k] for X~Bin(n,p) (exact, log-space)."""
    if n == 0:
        return 1.0
    tot = 0.0
    for i in range(k, n + 1):
        tot += math.exp(math.lgamma(n + 1) - math.lgamma(i + 1) - math.lgamma(n - i + 1)
                        + i * math.log(p) + (n - i) * math.log(1 - p))
    return min(1.0, tot)


def fitness(r, start, end):
    """DEV objective: one-sided 95% Wilson lower bound of hit rate at fixed coverage,
    minus a stability penalty (worst of 4 blocks below the mean)."""
    s = summarize(r, start, end)
    if s["actions"] < 30:
        return -1.0, s
    blocks = [b for b in s["block_hit_rates"] if b is not None]
    instab = max(0.0, (s["hit_rate"] - min(blocks))) if blocks else 0.0
    return s["wilson_lower_95"] - 0.25 * instab, s


# ------------------------------------------------------------------------ evolution
def mutate(g, rng):
    w = list(g.weights)
    for i in range(len(w)):
        if rng.random() < 0.4:
            w[i] = float(np.clip(w[i] + rng.gauss(0, 0.25), -1.5, 1.5))
    if rng.random() < 0.15:                      # structural: switch an expert off/on
        i = rng.randrange(len(w))
        w[i] = 0.0 if w[i] else rng.choice((-0.5, 0.5))
    mask = list(g.feature_mask)
    if rng.random() < 0.5:                       # structural: toggle a cross-intersection feature
        i = rng.randrange(len(mask))
        mask[i] = 1 - mask[i]
        if sum(mask) == 0:
            mask[i] = 1
    return dataclasses.replace(
        g, weights=tuple(round(x, 4) for x in w), feature_mask=tuple(mask),
        temperature=float(np.clip(g.temperature * math.exp(rng.gauss(0, 0.2)), 0.2, 5)),
        C=float(np.clip(g.C * math.exp(rng.gauss(0, 0.4)), 0.01, 5)),
        use_rolling_z=g.use_rolling_z if rng.random() > 0.1 else not g.use_rolling_z,
        origin="mutate:" + g.key())


def crossover(a, b, rng):
    w = tuple(x if rng.random() < 0.5 else y for x, y in zip(a.weights, b.weights))
    m = tuple(x if rng.random() < 0.5 else y for x, y in zip(a.feature_mask, b.feature_mask))
    if sum(m) == 0:
        m = a.feature_mask
    return dataclasses.replace(a, weights=w, feature_mask=m,
                               C=math.sqrt(a.C * b.C), origin="cross:%s+%s" % (a.key(), b.key()))


def fuse(a, b):
    """Score fusion: average the two parents' expert weightings (one new program)."""
    w = tuple(round((x + y) / 2, 4) for x, y in zip(a.weights, b.weights))
    m = tuple(max(x, y) for x, y in zip(a.feature_mask, b.feature_mask))
    return dataclasses.replace(a, weights=w, feature_mask=m,
                               temperature=(a.temperature + b.temperature) / 2,
                               origin="fuse:%s+%s" % (a.key(), b.key()))


def eras(T, warmup=1500, dev=0.55, val=0.20):
    usable = T - warmup
    d1 = warmup + int(usable * dev)
    v1 = d1 + int(usable * val)
    return {"warmup": (0, warmup), "dev": (warmup, d1), "val": (d1, v1), "sealed": (v1, T)}


def evolve(D, generations=8, population=16, seed=11, log=None, bank=None, seed_genomes=None):
    """Mutation/crossover/fusion on DEV; champion lineage chosen on VAL; SEALED untouched.

    Returns a report with per-generation DEV best, champion VAL, and the ledger of all
    evaluated genomes (no winner-only logging). Call open_sealed() exactly once after.
    """
    rng = random.Random(seed)
    bank = bank or ExpertBank(D)
    E = eras(len(D))
    ds, de = E["dev"]
    vs, ve = E["val"]
    pop = list(seed_genomes or [seed_cross_intersection()])
    while len(pop) < population:
        pop.append(mutate(pop[rng.randrange(len(pop))], rng))
    ledger, history = [], []
    champion, champion_val = None, -1.0
    t_start = time.perf_counter()
    cache = {}
    for gen in range(generations):
        scored = []
        for g in pop:
            k = g.key()
            if k not in cache:
                r = evaluate(bank, D, g, ds, de)
                fit, s = fitness(r, ds, de)
                cache[k] = (fit, s)
                ledger.append({"generation": gen, "genome": k, "origin": g.origin,
                               "dev_fitness": round(fit, 5), "dev_hit_rate": s["hit_rate"],
                               "dev_actions": s["actions"]})
            scored.append((cache[k][0], g))
        scored.sort(key=lambda x: -x[0])
        # VAL used only to choose among the top-3 DEV genomes for the lineage champion
        best_val = None
        for fit, g in scored[:3]:
            r = evaluate(bank, D, g, vs, ve)
            fv, sv = fitness(r, vs, ve)
            if best_val is None or fv > best_val[0]:
                best_val = (fv, g, sv)
        promoted = best_val[0] > champion_val
        if promoted:
            champion, champion_val = best_val[1], best_val[0]
        history.append({"generation": gen, "dev_best_fitness": round(scored[0][0], 5),
                        "dev_best_hit_rate": cache[scored[0][1].key()][1]["hit_rate"],
                        "val_candidate_hit_rate": best_val[2]["hit_rate"],
                        "val_candidate_fitness": round(best_val[0], 5),
                        "champion_replaced": promoted, "champion": champion.key()})
        if log:
            log(history[-1])
        elite = [g for _, g in scored[: max(2, population // 4)]]
        nxt = list(elite)
        while len(nxt) < population:
            u = rng.random()
            a, b = rng.sample(elite, 2) if len(elite) >= 2 else (elite[0], elite[0])
            nxt.append(mutate(a, rng) if u < 0.6 else crossover(a, b, rng) if u < 0.85 else fuse(a, b))
        pop = nxt
    return {"eras": E, "history": history, "ledger": ledger, "champion": champion,
            "champion_genome": dataclasses.asdict(champion), "evaluated_genomes": len(cache),
            "seconds": round(time.perf_counter() - t_start, 2), "bank": bank}


def open_sealed(report, D):
    """Evaluate the single pre-chosen champion on SEALED. Call once; result is final."""
    s0, s1 = report["eras"]["sealed"]
    r = evaluate(report["bank"], D, report["champion"], s0, s1)
    seed = evaluate(report["bank"], D, seed_cross_intersection(), s0, s1)
    return {"champion": summarize(r, s0, s1), "seed_cross_intersection": summarize(seed, s0, s1)}
