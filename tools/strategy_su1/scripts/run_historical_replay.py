#!/usr/bin/env python3
"""Run the SU1 Historical Replay Engine on real history plus two controls.

  python3 scripts/run_historical_replay.py --history research_data/ethpk10_draws.csv

Outputs validation/historical_replay_results.json. Three datasets go through the
identical engine, evolution budget and seed:
  REAL            the supplied history in time order
  TIME_SHUFFLED   the same draws in random order (destroys any temporal pattern;
                  anything the engine "finds" here is overfitting)
  PLANTED_SIGNAL  synthetic uniform draws where position 4 repeats its previous
                  number 15% of the time (a real, learnable pattern)
SEALED is opened exactly once per dataset, after evolution has finished.
"""
import argparse
import gc
import json
from pathlib import Path
import sys
import time

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "reference" / "replay"))
import hre  # noqa: E402
import forge  # noqa: E402
import grammar  # noqa: E402


def planted(T, seed=3, rate=0.15, position=3):
    rng = np.random.default_rng(seed)
    D = np.empty((T, 10), dtype=np.int8)
    for t in range(T):
        d = rng.permutation(10) + 1
        if t and rng.random() < rate:
            want = D[t - 1, position]
            j = int(np.where(d == want)[0][0])
            d[position], d[j] = d[j], d[position]
        D[t] = d
    return D


def planted_regime(T, seed=4, rate=0.30, src=1, dst=6, period=600):
    """Non-stationary cross-position pattern: during 'on' blocks the number at position
    dst tends to equal (previous draw at position src)+1; it switches off every period."""
    rng = np.random.default_rng(seed)
    D = np.empty((T, 10), dtype=np.int8)
    for t in range(T):
        d = rng.permutation(10) + 1
        if t and (t // period) % 2 == 0 and rng.random() < rate:
            want = D[t - 1, src] % 10 + 1
            j = int(np.where(d == want)[0][0])
            d[dst], d[j] = d[j], d[dst]
        D[t] = d
    return D


def planted_conditional(T, seed=6, rate=0.35):
    """A rule no template encodes: IF previous position-1 number is odd THEN position 5
    tends to repeat the previous position-2 number; otherwise draws are plain random."""
    rng = np.random.default_rng(seed)
    D = np.empty((T, 10), dtype=np.int8)
    for t in range(T):
        d = rng.permutation(10) + 1
        if t and D[t - 1, 0] % 2 == 1 and rng.random() < rate:
            want = D[t - 1, 1]
            j = int(np.where(d == want)[0][0])
            d[4], d[j] = d[j], d[4]
        D[t] = d
    return D


def run(name, D, generations, population, seed):
    t0 = time.perf_counter()
    rep = forge.campaign(D, generations=generations, population=population, seed=seed, screen=True,
                         log=lambda h: print(name, json.dumps({k: h[k] for k in ("generation", "dev_best_hit_rate", "dev_best_family", "champion_val_hit_rate", "champion_family", "elapsed_s")}), flush=True))
    screening = rep.get("screening")
    sealed = forge.open_sealed(rep)
    led = sorted(rep["ledger"], key=lambda x: -x["dev_fitness"])
    created, macros = rep.get("created_families", []), rep.get("macros", [])
    for k in ("lib", "programs", "cache"):
        rep.pop(k, None)
    gc.collect()
    return {"dataset": name, "seconds": round(time.perf_counter() - t0, 1),
            "periods": int(len(D)), "eras": rep["eras"], "evaluated_programs": rep["evaluated_programs"],
            "history": rep["history"], "champion": rep["champion_description"],
            "director_final": rep["director"], "top10_dev_programs": led[:10],
            "created_families": created, "learned_macros": macros, "screening": screening,
            "sealed_once": sealed}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--history", required=True)
    ap.add_argument("--generations", type=int, default=10)
    ap.add_argument("--population", type=int, default=32)
    ap.add_argument("--seed", type=int, default=11)
    ap.add_argument("--atom-catalog", default=str(ROOT / "reference" / "fixtures" / "atom_catalog_lifted.json"),
                    help="lifted registry atoms for GRAMMAR; pass '' to use built-in atoms only")
    ap.add_argument("--out", default=str(ROOT / "validation" / "historical_replay_results.json"))
    a = ap.parse_args()
    atom_cov = None
    if a.atom_catalog:
        cat = json.loads(Path(a.atom_catalog).read_text(encoding="utf-8"))
        grammar.set_catalog([(f, tuple(map(tuple, pk))) for f, pk in cat["score"]],
                            [(f, q, tuple(map(tuple, pk))) for f, q, pk in cat["cond"]])
        atom_cov = cat["coverage"]
    periods, ms, D = hre.load_draws(a.history)
    rng = np.random.default_rng(a.seed)
    sets = [("REAL", D), ("TIME_SHUFFLED", D[rng.permutation(len(D))]), ("PLANTED_SIGNAL", planted(len(D))),
            ("PLANTED_REGIME_CROSS", planted_regime(len(D))),
            ("PLANTED_CONDITIONAL", planted_conditional(len(D)))]
    out = {"engine": "SU1_HRE_V1 + SU1_FOUNDRY_V2 (R12: atom-world screening + 8 seed families + system-created EMERGENT families; GRAMMAR over lifted registry atoms; per-position HOTT; macro distillation; fold-stability fitness)", "history_file": Path(a.history).name,
           "first_period": int(periods[0]), "last_period": int(periods[-1]),
           "data_check": hre.data_report(ms, D), "atom_catalog": atom_cov,
           "budget": {"generations": a.generations, "population": a.population, "seed": a.seed},
           "results": [run(n, d, a.generations, a.population, a.seed) for n, d in sets]}
    Path(a.out).write_text(json.dumps(out, ensure_ascii=False, indent=2) + "\n")
    for r in out["results"]:
        c, s = r["sealed_once"]["champion"], r["sealed_once"]["seed_cross_intersection"]
        print(r["dataset"], "sealed champion", c["hits"], "/", c["actions"], c["hit_rate"],
              "p=", round(c["binomial_p_vs_30"], 4), "| seed XI", s["hit_rate"], "|", r["seconds"], "s")


if __name__ == "__main__":
    main()
