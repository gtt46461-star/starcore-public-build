#!/usr/bin/env python3
"""Run the dual-track loop (history replay + live shadow) over the last span of history.

  python3 scripts/run_dual_track.py --history research_data/ethpk10_draws_20260316_201059.csv

Periods [start, end) are treated as arriving one per minute: every pool program freezes
and settles every period; every --refresh periods a short campaign on data < t creates
children that join the pool at once. Output: validation/dual_track_results.json.
"""
import argparse
import gc
import json
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "reference" / "replay"))
sys.path.insert(0, str(ROOT / "scripts"))
import grammar  # noqa: E402
import hre  # noqa: E402
import live  # noqa: E402
from run_historical_replay import planted_conditional, planted_regime  # noqa: E402


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--history", required=True)
    ap.add_argument("--start", type=int, default=7875)
    ap.add_argument("--refresh", type=int, default=300)
    ap.add_argument("--pool", type=int, default=12)
    ap.add_argument("--atom-catalog", default=str(ROOT / "reference" / "fixtures" / "atom_catalog_lifted.json"))
    ap.add_argument("--out", default=str(ROOT / "validation" / "dual_track_results.json"))
    a = ap.parse_args()
    if a.atom_catalog:
        cat = json.loads(Path(a.atom_catalog).read_text(encoding="utf-8"))
        grammar.set_catalog([(f, tuple(map(tuple, pk))) for f, pk in cat["score"]],
                            [(f, q, tuple(map(tuple, pk))) for f, q, pk in cat["cond"]])
    _, _, D = hre.load_draws(a.history)
    sets = [("REAL", D), ("PLANTED_CONDITIONAL", planted_conditional(len(D))),
            ("PLANTED_REGIME_CROSS", planted_regime(len(D)))]
    results = []
    for name, d in sets:
        grammar.MACROS.clear()
        r = live.dual_track(d, a.start, len(d), refresh_every=a.refresh, pool_size=a.pool, seed=5,
                            log=lambda x, n=name: print(n, json.dumps(x, ensure_ascii=False)[:220], flush=True))
        r["dataset"] = name
        results.append(r)
        s = r["system_output"]
        print(name, "SYSTEM OUTPUT", s["hits"], "/", s["actions"], s["hit_rate"], "p=", round(s["binomial_p_vs_30"], 4),
              "blocks", s["block_hit_rates"], r["seconds"], "s", flush=True)
        gc.collect()
    Path(a.out).write_text(json.dumps({"engine": "SU1_DUAL_TRACK_V1", "span": [a.start, len(D)],
                                       "refresh_every": a.refresh, "pool": a.pool, "results": results},
                                      ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
