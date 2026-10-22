#!/usr/bin/env python3
"""Build reference/fixtures/grammar_parity.txt from real draws for ProgramTreeSU1Test."""
from pathlib import Path
import random
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "reference" / "replay"))
import forge  # noqa: E402
import grammar  # noqa: E402
import hre  # noqa: E402

N = 1700
TARGETS = (31, 400, 999, 1500, 1699)
HAND = [
    ("ADD", ("EXPERT", 1), ("IF", ("GT", 3, 1, 7, 2), ("REL", "SAME", 1, 0, 1), ("NEG", ("XT", 3)))),
    ("MUL", ("TANH", ("EXPERT", 6)), ("RANK", ("EXPERT", 5))),
    ("MAX", ("REL", "SUM", 2, 4, 3), ("REL", "MINUS1", 3, 9, 1)),
    ("IF", ("HOT", 2), ("SUB", ("EXPERT", 7), ("NUMIS", 4)), ("REL", "MIRROR", 1, 2, 5)),
    ("IF", ("ODD", 0, 1), ("REL", "PLUS1", 1, 7, 1), ("IF", ("BIG", 5, 2), ("EXPERT", 0), ("EXPERT", 4))),
    ("RANK", ("XT", 0)),
    ("IF", ("HOTT", 30, ("REL", "PLUS1", 1, 5, 1)), ("REL", "PLUS1", 1, 5, 1), ("EXPERT", 2)),
    ("IF", ("ODD", 0, 1), ("XT", 7), ("SUB", ("XT", 7), ("XT", 7))),
]


def main():
    grammar.CATALOG["score"], grammar.CATALOG["cond"] = [], []
    grammar.MACROS.clear()
    _, _, D = hre.load_draws(str(ROOT / "research_data" / "ethpk10_draws_20260316_201059.csv"))
    D = D[:N]
    lib = forge.Library(D)
    ev = grammar.Evaluator(lib)
    rng = random.Random(20261022)
    trees = HAND + [grammar.random_tree(rng, rng.randint(2, 4)) for _ in range(24)]
    lines = ["# SU1 grammar parity fixture: real draws rows 0..%d; S[t] uses rows < t only" % (N - 1), "DRAWS %d" % N]
    lines += [" ".join(str(int(x)) for x in row) for row in D]
    for i, t in enumerate(trees):
        lines.append("TREE %d %s" % (i, grammar.canonical(t)))
        S = ev.score(t)
        r = forge._act_from_scores(lib, S, 1.0, "MASS", 0, N)
        for target in TARGETS:
            vals = " ".join(repr(float(v)) for v in S[target].reshape(-1))
            act = "%d %d %d %d" % (r.position[target], *r.numbers[target])
            lines.append("EXPECT %d %d %s %s" % (i, target, act, vals))
    out = ROOT / "reference" / "fixtures" / "grammar_parity.txt"
    out.write_text("\n".join(lines) + "\n")
    print(out, len(trees), "trees")


if __name__ == "__main__":
    main()
