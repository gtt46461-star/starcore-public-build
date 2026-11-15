"""SU1 Strategy Foundry on the Historical Replay Engine (R11).

Creates MANY complete strategies from several families, evolves them generation by
generation on fast causal historical replay, and lets a research director allocate
effort to whichever family / operator actually produces improvements.

Families (each genome is a complete program: scores -> position -> K3 -> act/skip):
  EXPERT_MIX    weighted causal experts (frequency windows, omission, Markov, momentum)
  RELATION      discovered relation programs: number at (p+offset) lag l, transformed
                (SAME / +1 / -1 / MIRROR / SUM-with-another-position) predicts position p
  XI_SELECTOR   multi-set x multi-position cross-intersection features + walk-forward
                logistic opportunity selection (the user's historical method, one family)
  DEEP          a small neural scorer (tanh hidden layer, softmax over 10 numbers per
                position) re-trained walk-forward on past rows only

Research director: per-family UCB over realised DEV improvements; operator memory
(mutate / structural / crossover / fusion) whose sampling weights follow measured
success. Champions are chosen on VAL; SEALED is opened once after the run.

Host reference only; nothing here authorizes REAL.
"""
from __future__ import annotations

import dataclasses
import hashlib
import json
import math
import random
import time

import numpy as np

import grammar
import hre

FAMILIES = ("EXPERT_MIX", "RELATION", "XI_SELECTOR", "DEEP", "MATRIX", "HEDGE", "REGIME", "GRAMMAR")
REL_KINDS = ("SAME", "PLUS1", "MINUS1", "MIRROR", "SUM", "XTRANS")


# ------------------------------------------------------------------ genome
@dataclasses.dataclass(frozen=True)
class Strategy:
    family: str
    expert_w: tuple                 # weight per hre.EXPERTS
    relations: tuple                # ((kind, lag, offset, offset2, weight), ...)
    coverage: float                 # fraction of periods acted on (1.0 = every period)
    gate: str                       # "MASS" (top3 mass) or "MARGIN" (position clarity)
    hidden: int = 0                 # DEEP only
    xi: object = None               # hre.Genome for XI_SELECTOR
    eta: float = 0.0                # HEDGE: learning rate of per-period settled feedback
    decay: float = 0.98             # HEDGE: memory of past feedback
    alt_w: tuple = ()               # REGIME: second program's expert weights
    regime_expert: int = 0          # REGIME: expert whose recent form selects the program
    tree: tuple = ()                # GRAMMAR: program tree synthesised from atomic operations
    arm: str = ""                   # director arm that produced it (e.g. EMERGENT:<macro>)
    origin: str = "seed"

    def key(self):
        d = dataclasses.asdict(self)
        d.pop("origin")
        d.pop("arm")
        return hashlib.sha256(json.dumps(d, sort_keys=True, default=str).encode()).hexdigest()[:16]

    def describe(self):
        if self.family == "GRAMMAR":
            return "GRAMMAR | 评分 = %s | cover=%.2f gate=%s | 节点=%d" % (
                grammar.text(self.tree), self.coverage, self.gate, grammar.size(self.tree))
        rel = ["%s(lag=%d,off=%d%s)x%.2f" % (k, l, o, ",+%d" % o2 if k == "SUM" else "", w)
               for k, l, o, o2, w in self.relations]
        ex = ["%s=%.2f" % (e, w) for e, w in zip(hre.EXPERTS, self.expert_w) if abs(w) > 1e-9]
        return "%s | experts[%s] | relations[%s] | cover=%.2f gate=%s%s" % (
            self.family, ", ".join(ex), "; ".join(rel), self.coverage, self.gate,
            " hidden=%d" % self.hidden if self.family == "DEEP" else "")


def random_relation(rng):
    return (rng.choice(REL_KINDS), rng.randint(1, 3), rng.randrange(10), rng.randrange(1, 10),
            round(rng.choice((-1, 1)) * rng.uniform(0.2, 1.0), 3))


def random_strategy(family, rng):
    ew = tuple(round(rng.uniform(-1, 1), 3) if rng.random() < 0.5 else 0.0 for _ in hre.EXPERTS)
    rels = tuple(random_relation(rng) for _ in range(rng.randint(1, 3)))
    cov = rng.choice((0.10, 0.15, 0.25, 1.0))
    if family == "EXPERT_MIX":
        return Strategy(family, ew if any(ew) else (0, 0.5, 0, 0, 0, 0, 0, 0), (), cov, rng.choice(("MASS", "MARGIN")))
    if family == "RELATION":
        return Strategy(family, tuple(0.0 for _ in hre.EXPERTS), rels, cov, rng.choice(("MASS", "MARGIN")))
    if family == "GRAMMAR":
        return Strategy(family, tuple(0.0 for _ in hre.EXPERTS), (), rng.choice((0.10, 0.15, 0.25, 1.0)),
                        rng.choice(("MASS", "MARGIN")), tree=grammar.random_tree(rng, rng.randint(2, 4)))
    if family == "MATRIX":
        mats = tuple(("XTRANS", 1, rng.randrange(10), 1, round(rng.choice((-1, 1)) * rng.uniform(0.2, 1.0), 3))
                     for _ in range(rng.randint(1, 3)))
        return Strategy(family, ew, mats, cov, rng.choice(("MASS", "MARGIN")))
    if family == "HEDGE":
        return Strategy(family, tuple(1.0 for _ in hre.EXPERTS), rels, cov, "MASS",
                        eta=round(rng.uniform(0.5, 8.0), 3), decay=round(rng.uniform(0.9, 0.999), 4))
    if family == "REGIME":
        alt = tuple(round(rng.uniform(-1, 1), 3) if rng.random() < 0.5 else 0.0 for _ in hre.EXPERTS)
        return Strategy(family, ew, rels, cov, rng.choice(("MASS", "MARGIN")), alt_w=alt,
                        regime_expert=rng.randrange(len(hre.EXPERTS)))
    if family == "XI_SELECTOR":
        g = hre.mutate(hre.seed_cross_intersection(), rng)
        return Strategy(family, g.weights, (), g.coverage, "XI", xi=g)
    return Strategy(family, ew, rels, cov, "MASS", hidden=rng.choice((0, 8)))


# ------------------------------------------------------------------ scoring
class Library:
    """Shared, cached tensors so thousands of programs evaluate in milliseconds."""

    def __init__(self, D, bank=None):
        self.D = D.astype(np.int64)
        self.T = len(D)
        self.bank = bank or hre.ExpertBank(D)
        self._rel = {}

    def xtrans(self, off, window=1000):
        """Causal cross-position transition matrix: P(D[t,p]=n | D[t-1,(p+off)%10]=a),
        estimated from transitions completed before t inside a sliding window."""
        T, D = self.T, self.D - 1
        p = np.arange(10)
        q = (p + off) % 10
        M = np.zeros((10, 10, 10), np.float32)     # [target p, source number a, target number n]
        out = np.zeros((T, 10, 10), np.float32)
        for t in range(T):
            if t >= 2:
                M[p, D[t - 2, q], D[t - 1, p]] += 1
                if t - 2 - window >= 0:
                    u = t - 2 - window
                    M[p, D[u, q], D[u + 1, p]] -= 1
            if t >= 1:
                rows = M[p, D[t - 1, q]]
                out[t] = (rows + 1.0) / (rows.sum(axis=1, keepdims=True) + 10.0)
            else:
                out[t] = 0.1
        mu = out.mean(axis=2, keepdims=True)
        sd = out.std(axis=2, keepdims=True) + 1e-6
        return (out - mu) / sd

    def relation(self, kind, lag, off, off2):
        k = (kind, lag, off, off2) if kind != "XTRANS" else ("XTRANS", off)
        if k in self._rel:
            return self._rel[k]
        if kind == "XTRANS":
            self._rel[k] = self.xtrans(off)
            return self._rel[k]
        T, D = self.T, self.D
        out = np.zeros((T, 10, 10), dtype=np.float32)
        p = np.arange(10)
        src = np.zeros((T, 10), dtype=np.int64)
        src[lag:] = D[:-lag][:, (p + off) % 10]
        if kind == "SAME":
            tgt = src
        elif kind == "PLUS1":
            tgt = src % 10 + 1
        elif kind == "MINUS1":
            tgt = (src - 2) % 10 + 1
        elif kind == "MIRROR":
            tgt = 11 - src
        else:  # SUM with a second position, mod 10 into 1..10
            src2 = np.zeros((T, 10), dtype=np.int64)
            src2[lag:] = D[:-lag][:, (p + off + off2) % 10]
            tgt = (src + src2 - 1) % 10 + 1
        valid = np.arange(T) >= lag
        tt, pp = np.nonzero(valid[:, None] & np.ones((1, 10), bool))
        out[tt, pp, tgt[tt, pp] - 1] = 1.0
        out = (out - 0.1) / 0.3
        if len(self._rel) > 120:
            for key in [x for x in self._rel if x[0] != "XTRANS"]:
                del self._rel[key]
            if len(self._rel) > 60:            # XTRANS tensors are costly but bounded (10 offsets)
                self._rel.clear()
        self._rel[k] = out
        return out

    def linear_scores(self, s):
        S = np.tensordot(np.asarray(s.expert_w, np.float32), self.bank.Z, axes=(0, 0))
        for kind, lag, off, off2, w in s.relations:
            S = S + w * self.relation(kind, lag, off, off2)
        return S


def _act_from_scores(lib, S, coverage, gate, start, end):
    T = lib.T
    S64 = S.astype(np.float64)
    P = np.exp(S64 - S64.max(axis=2, keepdims=True))
    P /= P.sum(axis=2, keepdims=True)
    order = np.argsort(-(S64 - np.arange(10) * 1e-6), axis=2, kind="stable")
    k3 = order[:, :, :3]
    # Position rule (SU1_ACTION_HEAD_V2): float64 mass rounded to 1e-9; exact ties -> lowest position.
    # (V1 used float32 mass, whose ulp-level noise exceeded the 1e-9 tie-break: not reproducible.)
    mass = np.round(np.take_along_axis(P, k3, axis=2).sum(axis=2), 9)
    pr = np.argsort(-mass, axis=1, kind="stable")
    ti = np.arange(T)
    p1 = pr[:, 0]
    K3 = k3[ti, p1]
    hit = (K3 == (lib.D[ti, p1] - 1)[:, None]).any(axis=1)
    g = mass[ti, p1] if gate == "MASS" else mass[ti, p1] - mass[ti, pr[:, 1]]
    act = np.zeros(T, bool)
    if coverage >= 0.999:
        act[start:end] = True
    else:
        step = 50          # block-causal threshold: refreshed every 50 periods from the past 720
        for b in range(start, end, step):
            lo = max(0, b - 720)
            thr = np.quantile(g[lo:b], 1.0 - coverage)
            sl = slice(b, min(b + step, end))
            act[sl] = g[sl] >= thr
    return hre.Replay(act=act, hit=hit, pool=act.copy(), position=p1 + 1, numbers=K3 + 1, seconds=0.0)


def _deep_scores(lib, s, start, end, refit=1000, window=2000, epochs=6, seed=0):
    """Walk-forward neural scorer: features per (t,p,n) -> logit; softmax over n."""
    feats = [lib.bank.Z[i] for i, w in enumerate(s.expert_w) if abs(w) > 1e-9]
    feats += [lib.relation(k, l, o, o2) for k, l, o, o2, _ in s.relations]
    if not feats:
        feats = [lib.bank.Z[1]]
    X = np.stack(feats, axis=3).astype(np.float32)          # [T,10,10,d]
    T, d = lib.T, X.shape[3]
    y = lib.D - 1                                           # [T,10]
    S = np.zeros((T, 10, 10), np.float32)
    rng = np.random.default_rng(seed)
    h = s.hidden
    for b in range(start, end, refit):
        lo = max(hre_warm(), b - window)
        Xt, yt = X[lo:b].reshape(-1, 10, d), y[lo:b].reshape(-1)
        if h:
            W1 = rng.normal(0, 0.3, (d, h)).astype(np.float32); b1 = np.zeros(h, np.float32)
            w2 = rng.normal(0, 0.3, h).astype(np.float32)
        else:
            W1 = None; w2 = np.zeros(d, np.float32)
        params = [p for p in (W1, b1 if h else None, w2) if p is not None]
        m = [np.zeros_like(p) for p in params]; v = [np.zeros_like(p) for p in params]
        step = 0
        n = len(Xt)
        for _ in range(epochs):
            idx = rng.permutation(n)
            for j in range(0, n, 512):
                bi = idx[j:j + 512]
                xb, yb = Xt[bi], yt[bi]
                if h:
                    a = np.tanh(xb @ W1 + b1)                 # [B,10,h]
                    z = a @ w2
                else:
                    z = xb @ w2
                z = z - z.max(axis=1, keepdims=True)
                p = np.exp(z); p /= p.sum(axis=1, keepdims=True)
                gz = p; gz[np.arange(len(bi)), yb] -= 1; gz /= len(bi)
                if h:
                    gw2 = np.einsum("bnh,bn->h", a, gz)
                    ga = gz[:, :, None] * w2[None, None, :] * (1 - a * a)
                    grads = [np.einsum("bnd,bnh->dh", xb, ga), ga.sum(axis=(0, 1)), gw2]
                else:
                    grads = [np.einsum("bnd,bn->d", xb, gz)]
                step += 1
                for i, (pp, gg) in enumerate(zip(params, grads)):
                    gg = gg + 1e-3 * pp
                    m[i] = 0.9 * m[i] + 0.1 * gg; v[i] = 0.999 * v[i] + 0.001 * gg * gg
                    pp -= 0.02 * (m[i] / (1 - 0.9 ** step)) / (np.sqrt(v[i] / (1 - 0.999 ** step)) + 1e-8)
        hi = min(b + refit, end)
        xb = X[b:hi]
        S[b:hi] = (np.tanh(xb @ W1 + b1) @ w2) if h else (xb @ w2)
    return S


def _component_hits(lib, comps):
    """Settled per-period feedback for each component: share of the 10 positions whose
    actual number was inside that component's own top-3 (defined after draw t)."""
    T = lib.T
    hits = []
    actual = (lib.D - 1)[:, :, None]
    for Z in comps:
        top3 = np.argsort(-(Z - np.arange(10) * 1e-6), axis=2, kind="stable")[:, :, :3]
        hits.append((top3 == actual).any(axis=2).mean(axis=1))
    return np.stack(hits, axis=1)                               # [T, C]


def _hedge_scores(lib, s):
    """Online multiplicative weights: after every settled period each component's weight
    moves by eta*(hit-0.3); the decision for t uses feedback from periods < t only."""
    comps = [lib.bank.Z[i] for i, w in enumerate(s.expert_w) if abs(w) > 1e-9]
    comps += [lib.relation(k, l, o, o2) * np.sign(w) for k, l, o, o2, w in s.relations]
    H = _component_hits(lib, comps) - 0.3
    T, C = H.shape
    logw = np.zeros((T, C))
    acc = np.zeros(C)
    for t in range(1, T):
        acc = s.decay * acc + s.eta * H[t - 1]
        logw[t] = acc
    W = np.exp(logw - logw.max(axis=1, keepdims=True))
    W /= W.sum(axis=1, keepdims=True)
    S = np.zeros((T, 10, 10), np.float32)
    for c, Z in enumerate(comps):
        S += W[:, c][:, None, None].astype(np.float32) * Z
    return S


def _regime_scores(lib, s):
    """Two programs; the recent settled form (EMA) of one expert selects which runs."""
    A = np.tensordot(np.asarray(s.expert_w, np.float32), lib.bank.Z, axes=(0, 0))
    for k, l, o, o2, w in s.relations:
        A = A + w * lib.relation(k, l, o, o2)
    B = np.tensordot(np.asarray(s.alt_w or s.expert_w, np.float32), lib.bank.Z, axes=(0, 0))
    h = _component_hits(lib, [lib.bank.Z[s.regime_expert]])[:, 0]
    ema = np.zeros(lib.T)
    for t in range(1, lib.T):
        ema[t] = 0.97 * ema[t - 1] + 0.03 * (h[t - 1] - 0.3)
    use_a = (ema > 0)[:, None, None]
    return np.where(use_a, A, B)


def fitness_folds(r, start, end, folds=3):
    """Stability-first DEV objective: mean fold Wilson bound minus half the fold spread."""
    edges = np.linspace(start, end, folds + 1).astype(int)
    lbs = []
    for a, b in zip(edges[:-1], edges[1:]):
        f, _ = hre.fitness(r, a, b)
        lbs.append(f)
    whole, summ = hre.fitness(r, start, end)
    if whole <= -1.0:
        return whole, summ
    return float(np.mean(lbs) - 0.5 * (max(lbs) - min(lbs))), summ


def hre_warm():
    return 1500


def evaluate(lib, s, start, end):
    t0 = time.perf_counter()
    if s.family == "XI_SELECTOR":
        r = hre.evaluate(lib.bank, lib.D, s.xi, start, end)
    elif s.family == "DEEP":
        r = _act_from_scores(lib, _deep_scores(lib, s, start, end), s.coverage, s.gate, start, end)
    elif s.family == "GRAMMAR":
        if not hasattr(lib, "grammar"):
            lib.grammar = grammar.Evaluator(lib)
        r = _act_from_scores(lib, lib.grammar.score(s.tree), s.coverage, s.gate, start, end)
    elif s.family == "HEDGE":
        r = _act_from_scores(lib, _hedge_scores(lib, s), s.coverage, s.gate, start, end)
    elif s.family == "REGIME":
        r = _act_from_scores(lib, _regime_scores(lib, s), s.coverage, s.gate, start, end)
    else:
        r = _act_from_scores(lib, lib.linear_scores(s), s.coverage, s.gate, start, end)
    r.seconds = time.perf_counter() - t0
    return r


# ------------------------------------------------------------------ operators
def op_mutate(s, rng):
    if s.family == "GRAMMAR":
        cov = s.coverage if rng.random() > 0.2 else rng.choice((0.10, 0.15, 0.25, 1.0))
        return dataclasses.replace(s, tree=grammar.mutate(s.tree, rng), coverage=cov, origin="mutate<" + s.key())
    ew = tuple(round(float(np.clip(w + rng.gauss(0, 0.25), -1.5, 1.5)), 3) if rng.random() < 0.35 else w
               for w in s.expert_w)
    rels = tuple((k, l, o, o2, round(float(np.clip(w + rng.gauss(0, 0.2), -1.5, 1.5)), 3))
                 for k, l, o, o2, w in s.relations)
    cov = s.coverage if rng.random() > 0.2 else rng.choice((0.10, 0.15, 0.25, 1.0))
    xi = hre.mutate(s.xi, rng) if s.xi is not None else None
    extra = {}
    if s.family == "HEDGE":
        extra = dict(eta=round(float(np.clip(s.eta * math.exp(rng.gauss(0, 0.3)), 0.1, 20)), 3),
                     decay=round(float(np.clip(s.decay + rng.gauss(0, 0.01), 0.8, 0.9995)), 4))
    if s.family == "REGIME":
        extra = dict(alt_w=tuple(round(float(np.clip(w + rng.gauss(0, 0.25), -1.5, 1.5)), 3) if rng.random() < 0.35 else w
                                 for w in s.alt_w),
                     regime_expert=s.regime_expert if rng.random() > 0.2 else rng.randrange(len(hre.EXPERTS)))
    return dataclasses.replace(s, expert_w=ew, relations=rels, coverage=cov, xi=xi,
                               origin="mutate<" + s.key(), **extra)


def op_structural(s, rng):
    if s.family == "GRAMMAR":
        t = grammar.mutate(grammar.mutate(s.tree, rng), rng)
        gate = s.gate if rng.random() > 0.2 else rng.choice(("MASS", "MARGIN"))
        return dataclasses.replace(s, tree=t, gate=gate, origin="struct<" + s.key())
    rels = list(s.relations)
    ew = list(s.expert_w)
    u = rng.random()
    if s.family == "MATRIX" and u < 0.5:
        rels.append(("XTRANS", 1, rng.randrange(10), 1, round(rng.choice((-1, 1)) * rng.uniform(0.2, 1.0), 3)))
    elif s.family in ("RELATION", "DEEP", "HEDGE", "REGIME", "MATRIX") and (u < 0.4 or not rels):
        rels.append(random_relation(rng))                     # add a new relation primitive
    elif s.family in ("RELATION", "DEEP", "HEDGE", "REGIME", "MATRIX") and u < 0.6 and len(rels) > 1:
        rels.pop(rng.randrange(len(rels)))                    # delete one
    elif rels and u < 0.8:
        i = rng.randrange(len(rels)); k, l, o, o2, w = rels[i]
        rels[i] = (rng.choice(REL_KINDS), rng.randint(1, 3), o, o2, w)   # rebind kind/lag
    else:
        i = rng.randrange(len(ew)); ew[i] = 0.0 if ew[i] else round(rng.choice((-0.5, 0.5)), 3)
    hidden = s.hidden if s.family != "DEEP" or rng.random() > 0.3 else (8 if s.hidden == 0 else 0)
    gate = s.gate if s.family == "XI_SELECTOR" or rng.random() > 0.2 else rng.choice(("MASS", "MARGIN"))
    xi = hre.mutate(s.xi, rng) if s.xi is not None else None
    return dataclasses.replace(s, expert_w=tuple(ew), relations=tuple(rels[:6]), hidden=hidden,
                               gate=gate, xi=xi, origin="struct<" + s.key())


def op_crossover(a, b, rng):
    if a.family == "GRAMMAR" and b.family == "GRAMMAR":
        return dataclasses.replace(a, tree=grammar.crossover(a.tree, b.tree, rng),
                                   origin="cross<%s+%s" % (a.key(), b.key()))
    if a.family != b.family or a.family == "XI_SELECTOR":
        return op_mutate(a, rng)
    ew = tuple(x if rng.random() < 0.5 else y for x, y in zip(a.expert_w, b.expert_w))
    pool = list(a.relations) + list(b.relations)
    rels = tuple(rng.sample(pool, min(len(pool), max(1, (len(a.relations) + len(b.relations)) // 2)))) if pool else ()
    return dataclasses.replace(a, expert_w=ew, relations=rels, origin="cross<%s+%s" % (a.key(), b.key()))


def op_fuse(a, b, rng):
    """Cross-family fusion into one linear program (scores summed, relations united)."""
    if "XI_SELECTOR" in (a.family, b.family):
        return op_mutate(a, rng)
    if "GRAMMAR" in (a.family, b.family):
        def as_tree(x):
            if x.family == "GRAMMAR":
                return x.tree
            parts = [("EXPERT", i) for i, w in enumerate(x.expert_w) if abs(w) > 1e-9][:2]
            parts += [("REL", k, l, o, o2) if k != "XTRANS" else ("XT", o) for k, l, o, o2, _ in x.relations][:2]
            t = parts[0] if parts else ("EXPERT", 1)
            for q in parts[1:]:
                t = ("ADD", t, q)
            return t
        g = a if a.family == "GRAMMAR" else b
        t = ("ADD", as_tree(a), as_tree(b))
        if grammar.depth(t) > 5:
            t = grammar.crossover(as_tree(a), as_tree(b), rng)
        return dataclasses.replace(g, tree=t, coverage=min(a.coverage, b.coverage),
                                   origin="fuse<%s+%s" % (a.key(), b.key()))
    ew = tuple(round((x + y) / 2, 3) for x, y in zip(a.expert_w, b.expert_w))
    rels = tuple((list(a.relations) + list(b.relations))[:6])
    fam = ("DEEP" if "DEEP" in (a.family, b.family) else
           "HEDGE" if "HEDGE" in (a.family, b.family) else
           "MATRIX" if rels and all(r[0] == "XTRANS" for r in rels) else
           "RELATION" if rels else "EXPERT_MIX")
    hedge = a if a.family == "HEDGE" else b
    return Strategy(fam, ew if fam != "HEDGE" else tuple(1.0 if (x or y) else 0.0 for x, y in zip(a.expert_w, b.expert_w)),
                    rels, min(a.coverage, b.coverage), a.gate if a.gate != "XI" else "MASS",
                    hidden=max(a.hidden, b.hidden) if fam == "DEEP" else 0,
                    eta=hedge.eta if fam == "HEDGE" else 0.0, decay=hedge.decay if fam == "HEDGE" else 0.98,
                    origin="fuse<%s+%s" % (a.key(), b.key()))


OPERATORS = ("mutate", "structural", "crossover", "fuse")


# ------------------------------------------------------------------ director
class Director:
    """Research coordinator: UCB over families, success-weighted operator choice."""

    def __init__(self, rng):
        self.rng = rng
        self.fam = {f: {"n": 1, "gain": 0.0} for f in FAMILIES}
        self.emergent = {}          # arm name -> macro tree
        self.ops = {o: {"n": 1, "win": 1} for o in OPERATORS}
        self.memory = []

    def pick_family(self, total):
        def ucb(f):
            s = self.fam[f]
            return s["gain"] / s["n"] + 0.02 * math.sqrt(2 * math.log(total + 1) / s["n"])
        return max(list(self.fam), key=ucb)

    def add_emergent(self, arms):
        """Register system-created families; each starts with an optimistic prior."""
        added = []
        for name, tree in arms.items():
            if name not in self.fam:
                self.fam[name] = {"n": 1, "gain": 0.01}
                self.emergent[name] = tree
                added.append(name)
        return added

    def pick_op(self):
        w = [self.ops[o]["win"] / self.ops[o]["n"] + 0.05 for o in OPERATORS]
        return self.rng.choices(OPERATORS, weights=w)[0]

    def record(self, family, op, parent_fit, child_fit):
        gain = max(0.0, child_fit - parent_fit)
        self.fam.setdefault(family, {"n": 1, "gain": 0.0})
        self.fam[family]["n"] += 1
        self.fam[family]["gain"] += gain
        self.ops[op]["n"] += 1
        self.ops[op]["win"] += int(child_fit > parent_fit)
        self.memory.append((family, op, round(child_fit - parent_fit, 5)))

    def snapshot(self):
        return {"family_mean_gain": {f: round(v["gain"] / v["n"], 5) for f, v in self.fam.items()},
                "family_trials": {f: v["n"] - 1 for f, v in self.fam.items()},
                "operator_success": {o: round((v["win"] - 1) / max(1, v["n"] - 1), 3) for o, v in self.ops.items()}}


# ------------------------------------------------------------------ campaign
def screening_programs(rng, cond_per_atom=60):
    """Atom-world screening, stage 1: every single atom / relation / matrix / expert alone."""
    terms = [("EXPERT", e) for e in range(len(hre.EXPERTS))]
    terms += [("NEG", ("EXPERT", e)) for e in range(len(hre.EXPERTS))]
    terms += [("REL", k, l, o, 1) for k in ("SAME", "PLUS1", "MINUS1", "MIRROR") for l in (1, 2, 3) for o in range(10)]
    terms += [("REL", "SUM", l, o, o2) for l in (1, 2) for o in range(10) for o2 in range(1, 10)]
    terms += [("XT", o) for o in range(10)]
    terms += [("ATOM", f, pk) for f, pk in grammar.CATALOG["score"]]
    terms += [("NEG", ("ATOM", f, pk)) for f, pk in grammar.CATALOG["score"]]
    return terms


def conditional_programs(atom, rng, n=60):
    """Atom-world screening, stage 2: the same atom gated by systematic conditions."""
    zero = ("SUB", atom, atom)
    conds = [("HOTT", w, atom) for w in (30, 60, 120)]
    conds += [("ODD", q, l) for q in range(10) for l in (1, 2)]
    conds += [("BIG", q, l) for q in range(10) for l in (1, 2)]
    conds += [("HOT", e) for e in range(len(hre.EXPERTS))]
    extra = [("GT", rng.randrange(10), rng.randint(1, 2), rng.randrange(10), rng.randint(1, 2)) for _ in range(n // 3)]
    extra += [("ATOMC", f, q, pk) for f, q, pk in rng.sample(grammar.CATALOG["cond"], min(n // 3, len(grammar.CATALOG["cond"])))]
    out = []
    for c in conds + extra:
        out.append(("IF", c, atom, zero))
        out.append(("IF", c, zero, atom))         # the complement branch is a different hypothesis
    return out


def campaign(D, generations=10, population=24, seed=7, log=None, deep_share=0.25,
             eras=None, lib=None, seed_pop=None, learn_families=True, director=None,
             fresh_library=True, screen=False, screen_top=8, arm_cap=0.35):
    """fresh_library=True starts with an empty macro library (independent experiments);
    the live loop passes False so research memory accumulates across refreshes."""
    rng = random.Random(seed)
    if fresh_library:
        grammar.MACROS.clear()
    lib = lib or Library(D)
    E = eras or hre.eras(len(D))
    ds, de = E["dev"]
    vs, ve = E["val"]
    director = director or Director(rng)      # pass one in to keep research memory across campaigns
    cache, ledger, history = {}, [], []

    def score(s):
        k = s.key()
        if k not in cache:
            r = evaluate(lib, s, ds, de)
            fit, summ = fitness_folds(r, ds, de)
            cache[k] = (fit, summ, r.seconds)
            ledger.append({"key": k, "family": s.family, "origin": s.origin, "dev_fitness": round(fit, 5),
                           "dev_hit_rate": summ["hit_rate"], "dev_actions": summ["actions"],
                           "seconds": round(r.seconds, 3)})
        return cache[k][0]

    progs = {}
    screened = {"singles": 0, "conditionals": 0, "top": []}
    seeds_from_screen = []
    if screen:
        def gp(tree, origin):
            return Strategy("GRAMMAR", tuple(0.0 for _ in hre.EXPERTS), (), 0.25, "MASS", tree=tree, origin=origin)
        singles = [gp(t, "screen:atom") for t in screening_programs(rng)]
        ranked = sorted(((score(s), s) for s in singles), key=lambda x: -x[0])
        screened["singles"] = len(singles)
        conds = []
        for _, s in ranked[:screen_top]:
            conds += [gp(t, "screen:cond") for t in conditional_programs(s.tree, rng)]
        ranked2 = sorted(((score(s), s) for s in conds), key=lambda x: -x[0])
        screened["conditionals"] = len(conds)
        best = sorted(ranked[:screen_top] + ranked2[:screen_top * 2], key=lambda x: -x[0])
        seeds_from_screen = [s for _, s in best[: max(4, population // 3)]]
        screened["top"] = [{"dev_fitness": round(f, 5), "formula": grammar.text(s.tree)} for f, s in best[:6]]
        for _, s in ranked + ranked2:
            progs[s.key()] = s
    pop = list(seed_pop or []) + seeds_from_screen + [hre_seed()]
    pop += [random_strategy(FAMILIES[i % len(FAMILIES)], rng) for i in range(population - len(pop))]
    created_families = []
    champion, champion_val, champion_vs = None, -1.0, None
    t0 = time.perf_counter()
    for gen in range(generations):
        scored = sorted(((score(s), s) for s in pop), key=lambda x: -x[0])
        for _, s in scored:
            progs[s.key()] = s
        if learn_families:
            ranked = sorted(((cache[k][0], p.tree) for k, p in progs.items() if p.family == "GRAMMAR"),
                            key=lambda x: -x[0])[:12]
            grammar.learn_macros(ranked, gen)
            for name in director.add_emergent(grammar.emergent_families()):
                created_families.append({"generation": gen, "family": name,
                                         "formula": grammar.text(director.emergent[name])})
        # VAL chooses the lineage champion among the DEV top-4 (one champion line)
        cands = []
        for fit, s in scored[:4]:
            r = evaluate(lib, s, vs, ve)
            fv, sv = hre.fitness(r, vs, ve)
            cands.append((fv, s, sv))
        cands.sort(key=lambda x: -x[0])
        replaced = cands[0][0] > champion_val
        if replaced:
            champion_val, champion, champion_vs = cands[0][0], cands[0][1], cands[0][2]
        fam_count = {f: sum(1 for _, s in scored if (s.arm or s.family) == f) for f in director.fam}
        history.append({"generation": gen, "dev_best_fitness": round(scored[0][0], 5),
                        "dev_best_hit_rate": cache[scored[0][1].key()][1]["hit_rate"],
                        "dev_best_family": scored[0][1].family,
                        "champion_val_fitness": round(champion_val, 5),
                        "champion_val_hit_rate": champion_vs["hit_rate"],
                        "champion_family": champion.family, "champion_replaced": replaced,
                        "population_by_family": fam_count, "director": director.snapshot(),
                        "elapsed_s": round(time.perf_counter() - t0, 1)})
        if log:
            log(history[-1])
        elite = [s for _, s in scored[: max(4, population // 4)]]
        nxt = list(elite)
        arm_n = {}
        while len(nxt) < population:
            fam = director.pick_family(len(ledger))
            if arm_n.get(fam, 0) >= max(2, int(arm_cap * population)):   # keep exploring other arms
                fam = rng.choice([f for f in director.fam if arm_n.get(f, 0) < max(2, int(arm_cap * population))])
            arm_n[fam] = arm_n.get(fam, 0) + 1
            if fam.startswith("EMERGENT:"):
                macro = director.emergent[fam]
                same = [s for s in elite if s.arm == fam]
                parent = rng.choice(same) if same else Strategy(
                    "GRAMMAR", tuple(0.0 for _ in hre.EXPERTS), (), rng.choice((0.10, 0.15, 0.25, 1.0)),
                    rng.choice(("MASS", "MARGIN")), tree=grammar.seed_with_macro(macro, rng), arm=fam,
                    origin="emergent<" + fam)
            else:
                same = [s for s in elite if s.family == fam]
                parent = rng.choice(same) if same else (random_strategy(fam, rng) if rng.random() < 0.5 else rng.choice(elite))
            op = director.pick_op()
            other = rng.choice(elite)
            if op == "mutate":
                child = op_mutate(parent, rng)
            elif op == "structural":
                child = op_structural(parent, rng)
            elif op == "crossover":
                child = op_crossover(parent, other, rng)
            else:
                child = op_fuse(parent, other, rng)
            if child.family == "DEEP" and sum(1 for s in nxt if s.family == "DEEP") >= deep_share * population:
                child = op_mutate(rng.choice([s for s in elite if s.family != "DEEP"] or elite), rng)
            if parent.arm and not child.arm:
                child = dataclasses.replace(child, arm=parent.arm)
            director.record(child.arm or child.family, op, score(parent), score(child))
            nxt.append(child)
        pop = nxt
    return {"eras": E, "history": history, "ledger": ledger, "champion": champion,
            "champion_description": champion.describe(), "evaluated_programs": len(cache),
            "director": director.snapshot(), "seconds": round(time.perf_counter() - t0, 1), "lib": lib,
            "created_families": created_families, "macros": grammar.macro_report(), "screening": screened,
            "programs": progs, "cache": cache}


def hre_seed():
    g = hre.seed_cross_intersection()
    return Strategy("XI_SELECTOR", g.weights, (), g.coverage, "XI", xi=g, origin="seed:XI")


def open_sealed(rep):
    lib = rep["lib"]
    s0, s1 = rep["eras"]["sealed"]
    r = evaluate(lib, rep["champion"], s0, s1)
    base = evaluate(lib, hre_seed(), s0, s1)
    return {"champion": hre.summarize(r, s0, s1), "seed_cross_intersection": hre.summarize(base, s0, s1)}
