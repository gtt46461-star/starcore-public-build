"""SU1 atomic-grammar program synthesis (R12 GRAMMAR family).

Strategies here are not instances of a hand-written template. The system assembles
typed expression trees from atomic operations, so new kinds of ideas can appear:

  SCORE nodes return a [T,10,10] tensor (time, position, number):
    EXPERT(e)                 causal expert score (frequency windows, omission, Markov, momentum)
    REL(kind,lag,off,off2)    relation primitive: number at (p+off) lag l, SAME/+1/-1/MIRROR/SUM
    XT(off)                   cross-position transition matrix row (atomic world matrix)
    NUMIS(k)                  constant preference for number k (bias atom)
    ADD/SUB/MUL/MAX(a,b), NEG(a), TANH(a), RANK(a)  (RANK = within-position rank, 0..1)
    IF(cond, a, b)            per-period branch chosen BEFORE the draw
  COND nodes return a [T,1,1] boolean from the past only:
    GT(q1,l1,q2,l2)           number at position q1 lag l1  >  number at position q2 lag l2
    ODD(q,l)                  number at position q lag l is odd
    BIG(q,l)                  number at position q lag l >= 6
    HOT(e)                    expert e's settled top-3 hit share over the last 30 periods > 30%
    HOTT(w, sub)              PER POSITION: sub-program's own top-3 at this position hit > 36% over the
                              last w settled periods (online regime tracking; [T,10,1])

Every tree is turned into readable formula text. All inputs at target t come from
rows < t; HOT uses settled feedback from periods < t.
"""
from __future__ import annotations

import collections
import hashlib
import random

import numpy as np

import atoms

# Lifted registry atoms (set by set_catalog); empty means built-in atoms only.
CATALOG = {"score": [], "cond": []}


def set_catalog(score_specs, cond_specs):
    CATALOG["score"] = [tuple(x) for x in score_specs]
    CATALOG["cond"] = [tuple(x) for x in cond_specs]


# ------------------------------------------------------------------ macro library (learned)
# Sub-programs that keep recurring inside the best programs are distilled into reusable
# macros. A macro with enough support becomes an EMERGENT family: the director gives it
# its own arm and new programs are grown around it. Families are therefore created by
# the system, not fixed by hand.
MACROS = {}            # tree -> {"id", "support", "best_fitness", "born", "last"}


def macro_id(tree):
    return "M" + hashlib.sha256(repr(tree).encode()).hexdigest()[:10]


def score_subtrees(tree):
    for path in _paths(tree):
        node = _get(tree, path)
        if node[0] in SCORE_BIN or node[0] in SCORE_UN or node[0] == "IF":
            yield node


def learn_macros(ranked_trees, generation, min_size=2, max_size=7, min_support=2, cap=40):
    """ranked_trees: [(fitness, tree)] best first. Returns ids of newly created macros."""
    support = collections.Counter()
    best = {}
    for fit, t in ranked_trees:
        for sub in set(score_subtrees(t)):
            if is_constant(sub):
                continue                      # e.g. SUB(x,x): the screening "zero branch" is not knowledge
            if min_size <= size(sub) <= max_size:
                support[sub] += 1
                best[sub] = max(best.get(sub, -1.0), fit)
    new = []
    for sub, c in support.items():
        if c < min_support:
            continue
        if sub in MACROS:
            m = MACROS[sub]
            m["support"] = max(m["support"], c)
            m["best_fitness"] = max(m["best_fitness"], best[sub])
            m["last"] = generation
        else:
            MACROS[sub] = {"id": macro_id(sub), "support": c, "best_fitness": best[sub],
                           "born": generation, "last": generation}
            new.append(MACROS[sub]["id"])
    if len(MACROS) > cap:
        keep = sorted(MACROS.items(), key=lambda kv: (-kv[1]["support"], -kv[1]["best_fitness"]))[:cap]
        MACROS.clear()
        MACROS.update(keep)
    return new


def is_constant(tree):
    """Provably constant sub-programs that must never become macros."""
    op = tree[0]
    if op == "SUB" and tree[1] == tree[2]:
        return True
    if op in ("NEG", "TANH", "RANK") and is_constant(tree[1]):
        return op != "RANK"
    if op in ("ADD", "MUL", "MAX", "SUB") and is_constant(tree[1]) and is_constant(tree[2]):
        return True
    return False


def emergent_families(min_support=3):
    return {"EMERGENT:" + m["id"]: t for t, m in MACROS.items() if m["support"] >= min_support}


def seed_with_macro(macro, rng):
    """Grow a new program around a macro: wrap, branch or combine it."""
    u = rng.random()
    if u < 0.35:
        return macro
    if u < 0.6:
        return ("IF", random_cond_for(macro, rng), macro, random_tree(rng, 1))
    if u < 0.85:
        return (rng.choice(SCORE_BIN), macro, random_tree(rng, 1))
    return (rng.choice(SCORE_UN), macro)


def macro_report(limit=12):
    rows = sorted(MACROS.items(), key=lambda kv: (-kv[1]["support"], -kv[1]["best_fitness"]))[:limit]
    return [{"id": m["id"], "support": m["support"], "best_dev_fitness": round(m["best_fitness"], 5),
             "born_generation": m["born"], "formula": text(t)} for t, m in rows]

KINDS = ("SAME", "PLUS1", "MINUS1", "MIRROR", "SUM")
SCORE_BIN = ("ADD", "SUB", "MUL", "MAX")
SCORE_UN = ("NEG", "TANH", "RANK")
N_EXPERTS = 8
EXPERT_NAMES = ("近12期频率", "近24期频率", "近48期频率", "近96期频率", "近360期频率", "遗漏", "马尔可夫", "动量")
POS = "位置"


# ------------------------------------------------------------------ generation
def random_cond(rng):
    if CATALOG["cond"] and rng.random() < 0.3:
        f, q, pk = rng.choice(CATALOG["cond"])
        return ("ATOMC", f, q, pk)
    u = rng.random()
    if u < 0.4:
        return ("GT", rng.randrange(10), rng.randint(1, 3), rng.randrange(10), rng.randint(1, 3))
    if u < 0.6:
        return ("ODD", rng.randrange(10), rng.randint(1, 3))
    if u < 0.8:
        return ("BIG", rng.randrange(10), rng.randint(1, 3))
    return ("HOT", rng.randrange(N_EXPERTS))


def random_cond_for(sub, rng):
    """Condition that may refer to the branch it guards (HOTT tracks whether `sub` works lately)."""
    if rng.random() < 0.35:
        return ("HOTT", rng.choice((30, 60, 120)), sub)
    return random_cond(rng)


def random_terminal(rng):
    if MACROS and rng.random() < 0.2:
        trees = list(MACROS)
        w = [MACROS[t]["support"] for t in trees]
        return rng.choices(trees, weights=w)[0]
    if CATALOG["score"] and rng.random() < 0.5:
        f, pk = rng.choice(CATALOG["score"])
        return ("ATOM", f, pk)
    u = rng.random()
    if u < 0.4:
        return ("EXPERT", rng.randrange(N_EXPERTS))
    if u < 0.75:
        return ("REL", rng.choice(KINDS), rng.randint(1, 3), rng.randrange(10), rng.randrange(1, 10))
    if u < 0.92:
        return ("XT", rng.randrange(10))
    return ("NUMIS", rng.randrange(10))


def random_tree(rng, depth=3):
    if depth <= 0 or rng.random() < 0.3:
        return random_terminal(rng)
    u = rng.random()
    if u < 0.55:
        return (rng.choice(SCORE_BIN), random_tree(rng, depth - 1), random_tree(rng, depth - 1))
    if u < 0.75:
        return (rng.choice(SCORE_UN), random_tree(rng, depth - 1))
    return ("IF", random_cond(rng), random_tree(rng, depth - 1), random_tree(rng, depth - 1))


def size(tree):
    if tree[0] in SCORE_BIN:
        return 1 + size(tree[1]) + size(tree[2])
    if tree[0] in SCORE_UN:
        return 1 + size(tree[1])
    if tree[0] == "IF":
        return 1 + size(tree[2]) + size(tree[3])
    return 1


def depth(tree):
    if tree[0] in SCORE_BIN:
        return 1 + max(depth(tree[1]), depth(tree[2]))
    if tree[0] in SCORE_UN:
        return 1 + depth(tree[1])
    if tree[0] == "IF":
        return 1 + max(depth(tree[2]), depth(tree[3]))
    return 0


def _paths(tree, path=()):
    yield path
    if tree[0] in SCORE_BIN:
        yield from _paths(tree[1], path + (1,))
        yield from _paths(tree[2], path + (2,))
    elif tree[0] in SCORE_UN:
        yield from _paths(tree[1], path + (1,))
    elif tree[0] == "IF":
        yield from _paths(tree[2], path + (2,))
        yield from _paths(tree[3], path + (3,))


def _get(tree, path):
    for i in path:
        tree = tree[i]
    return tree


def _put(tree, path, sub):
    if not path:
        return sub
    i = path[0]
    lst = list(tree)
    lst[i] = _put(tree[i], path[1:], sub)
    return tuple(lst)


def mutate(tree, rng, max_depth=5):
    """Subtree replacement, branch insertion, atom swap or condition rewrite."""
    for _ in range(8):
        path = rng.choice(list(_paths(tree)))
        node = _get(tree, path)
        u = rng.random()
        if u < 0.35:
            new = random_tree(rng, 2)
        elif u < 0.55:
            new = ("IF", random_cond_for(node, rng), node, random_tree(rng, 1))
        elif u < 0.75 and node[0] in ("EXPERT", "REL", "XT", "NUMIS", "ATOM"):
            new = random_terminal(rng)
        elif u < 0.9 and node[0] == "IF":
            new = ("IF", random_cond(rng), node[2], node[3])
        else:
            new = (rng.choice(SCORE_UN), node)
        out = _put(tree, path, new)
        if depth(out) <= max_depth:
            return out
    return tree


def crossover(a, b, rng, max_depth=5):
    """Graft a random subtree of b into a random point of a."""
    for _ in range(8):
        pa = rng.choice(list(_paths(a)))
        pb = rng.choice(list(_paths(b)))
        out = _put(a, pa, _get(b, pb))
        if depth(out) <= max_depth:
            return out
    return a


# ------------------------------------------------------------------ evaluation
class Evaluator:
    def __init__(self, lib):
        self.lib = lib
        self.D = lib.D                    # int64 [T,10], numbers 1..10
        self.cache = {}
        self._hot = {}
        self.atoms = None

    def _lag(self, q, l):
        v = np.zeros(self.lib.T, dtype=np.int64)
        v[l:] = self.D[:-l, q]
        return v

    def _atoms(self):
        if self.atoms is None:
            self.atoms = atoms.AtomLibrary(self.lib.D, self.lib.bank)
        return self.atoms

    def cond(self, c):
        if c[0] == "HOTT":
            key = ("HOTT", c[1], c[2])
            if key not in self._hot:
                w, sub = c[1], c[2]
                S = self.score(sub)
                top3 = np.argsort(-(S.astype(np.float64) - np.arange(10) * 1e-6), axis=2, kind="stable")[:, :, :3]
                hit = (top3 == (self.D - 1)[:, :, None]).any(axis=2).astype(np.float64)   # [T,10], settled at t
                c1 = np.vstack([np.zeros((1, 10)), np.cumsum(hit, axis=0)])
                t = np.arange(self.lib.T)
                lo = np.maximum(t - w, 0)
                share = (c1[t] - c1[lo]) / np.maximum(t - lo, 1)[:, None]             # rows < t only
                self._hot[key] = ((share > 0.36) & (t >= w)[:, None])[:, :, None]
            if len(self._hot) > 64:
                self._hot.clear()
                return self.cond(c)
            return self._hot[key]
        if c[0] == "ATOMC":
            return self._atoms().cond(c[1], c[2], c[3])
        if c[0] == "GT":
            out = self._lag(c[1], c[2]) > self._lag(c[3], c[4])
        elif c[0] == "ODD":
            out = self._lag(c[1], c[2]) % 2 == 1
        elif c[0] == "BIG":
            out = self._lag(c[1], c[2]) >= 6
        else:  # HOT: settled feedback of an expert over the previous 30 periods
            e = c[1]
            if e not in self._hot:
                Z = self.lib.bank.Z[e]
                top3 = np.argsort(-(Z - np.arange(10) * 1e-6), axis=2, kind="stable")[:, :, :3]
                h = (top3 == (self.D - 1)[:, :, None]).any(axis=2).mean(axis=1)
                c1 = np.concatenate([[0.0], np.cumsum(h)])
                t = np.arange(self.lib.T)
                lo = np.maximum(t - 30, 0)
                past = (c1[t] - c1[lo]) / np.maximum(t - lo, 1)    # rows < t only
                self._hot[e] = (past > 0.3) & (t >= 30)
            out = self._hot[e]
        return out[:, None, None]

    def score(self, tree):
        key = tree
        if key in self.cache:
            return self.cache[key]
        op = tree[0]
        if op == "EXPERT":
            out = self.lib.bank.Z[tree[1]]
        elif op == "ATOM":
            out = self._atoms().score(tree[1], tree[2])
        elif op == "REL":
            out = self.lib.relation(tree[1], tree[2], tree[3], tree[4])
        elif op == "XT":
            out = self.lib.relation("XTRANS", 1, tree[1], 1)
        elif op == "NUMIS":
            out = np.zeros((self.lib.T, 10, 10), np.float32)
            out[:, :, tree[1]] = 1.0
        elif op == "ADD":
            out = self.score(tree[1]) + self.score(tree[2])
        elif op == "SUB":
            out = self.score(tree[1]) - self.score(tree[2])
        elif op == "MUL":
            out = self.score(tree[1]) * self.score(tree[2])
        elif op == "MAX":
            out = np.maximum(self.score(tree[1]), self.score(tree[2]))
        elif op == "NEG":
            out = -self.score(tree[1])
        elif op == "TANH":
            out = np.tanh(self.score(tree[1]))
        elif op == "RANK":
            x = self.score(tree[1])
            out = (np.argsort(np.argsort(x, axis=2, kind="stable"), axis=2, kind="stable") / 9.0).astype(np.float32)
        elif op == "IF":
            out = np.where(self.cond(tree[1]), self.score(tree[2]), self.score(tree[3]))
        else:
            raise ValueError("UNKNOWN_OP " + str(op))
        out = np.nan_to_num(out.astype(np.float32), nan=0.0, posinf=6.0, neginf=-6.0)
        if len(self.cache) > 48:
            self.cache.clear()
        self.cache[key] = out
        return out


# ------------------------------------------------------------------ readable formula
def _cond_text(c):
    if c[0] == "HOTT":
        return "该子程序[%s]在本位置近%d期命中率>36%%" % (text(c[2]), c[1])
    if c[0] == "ATOMC":
        return atoms.cond_text(c[1], c[2], c[3])
    if c[0] == "GT":
        return "%s%d前%d期号码 > %s%d前%d期号码" % (POS, c[1] + 1, c[2], POS, c[3] + 1, c[4])
    if c[0] == "ODD":
        return "%s%d前%d期号码为奇数" % (POS, c[1] + 1, c[2])
    if c[0] == "BIG":
        return "%s%d前%d期号码≥6" % (POS, c[1] + 1, c[2])
    return "%s近30期已结算命中率>30%%" % EXPERT_NAMES[c[1]]


def text(tree):
    op = tree[0]
    if op == "EXPERT":
        return EXPERT_NAMES[tree[1]]
    if op == "ATOM":
        return atoms.atom_text(tree[1], tree[2])
    if op == "REL":
        kind, lag, off, off2 = tree[1:]
        src = "本位置" if off == 0 else "位置+%d" % off
        if kind == "SUM":
            return "[%s与位置+%d的前%d期号码之和mod10]" % (src, (off + off2) % 10, lag)
        name = {"SAME": "", "PLUS1": "+1", "MINUS1": "−1", "MIRROR": "的镜像(11−x)"}[kind]
        return "[%s前%d期号码%s]" % (src, lag, name)
    if op == "XT":
        return "[位置+%d→本位置转移矩阵]" % tree[1]
    if op == "NUMIS":
        return "[号码%d偏好]" % (tree[1] + 1)
    if op in SCORE_BIN:
        sym = {"ADD": "+", "SUB": "−", "MUL": "×", "MAX": " max "}[op]
        return "(%s %s %s)" % (text(tree[1]), sym, text(tree[2]))
    if op == "NEG":
        return "−%s" % text(tree[1])
    if op == "TANH":
        return "tanh%s" % text(tree[1])
    if op == "RANK":
        return "排名%s" % text(tree[1])
    return "若{%s}则%s否则%s" % (_cond_text(tree[1]), text(tree[2]), text(tree[3]))


# ------------------------------------------------------------------ canonical text (Java parity)
def canonical(tree):
    """Language-neutral program text, e.g. ADD(EXPERT(1),IF(GT(3,1,7,2),REL(SAME,1,0,1),NEG(XT(3))))."""
    op = tree[0]
    if op == "ATOM":
        return "ATOM(%s,%s)" % (tree[1], ";".join("%s=%s" % kv for kv in tree[2]))
    if op == "ATOMC":
        return "ATOMC(%s,%d,%s)" % (tree[1], tree[2], ";".join("%s=%s" % kv for kv in tree[3]))
    args = []
    for x in tree[1:]:
        args.append(canonical(x) if isinstance(x, tuple) else str(x))
    return "%s(%s)" % (op, ",".join(args))
