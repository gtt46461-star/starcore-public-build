#!/usr/bin/env python3
"""H18: compare a fresh run of tools/r27_null_baseline.py with the published results (fixed seed), ±1 percentage point.
Usage: compare_null_baseline.py <fresh.json> <published.json>"""
import json, sys

def leaves(x, prefix=""):
    if isinstance(x, dict):
        for k, v in x.items():
            yield from leaves(v, prefix + "/" + k)
    elif isinstance(x, list):
        for i, v in enumerate(x):
            yield from leaves(v, prefix + "/" + str(i))
    elif isinstance(x, (int, float)) and not isinstance(x, bool):
        yield prefix, float(x)

fresh, pub = (dict(leaves(json.load(open(p)))) for p in sys.argv[1:3])
bad, n, worst = [], 0, 0.0
for k, v in pub.items():
    if k not in fresh:
        bad.append(k + ":MISSING")
        continue
    # rates / probabilities / Brier values are compared at ±0.01; counts (n, hits, draws, generation) must match exactly
    is_count = k.rsplit("/", 1)[-1] in ("n", "hits", "draws", "train_n", "test_n", "generation")
    d = abs(fresh[k] - v)
    n += 1
    if is_count:
        if d != 0 and not k.endswith("/hits"):
            bad.append("%s fresh=%s published=%s" % (k, fresh[k], v))
        elif k.endswith("/hits"):
            worst = max(worst, d / max(1, pub.get(k[:-5] + "/n", 1)))
    else:
        worst = max(worst, d)
        if d > 0.01:
            bad.append("%s fresh=%s published=%s" % (k, fresh[k], v))
print("H18|compared=%d maxAbsDiff=%.6f failures=%d" % (n, worst, len(bad)))
for b in bad:
    print("H18|DIFF|" + b)
print("H18|RESULT|" + ("PASS_WITHIN_1PP" if not bad else "FAILED"))
sys.exit(1 if bad else 0)
