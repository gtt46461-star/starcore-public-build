"""SU1 dual-track research loop reference (R12): history replay + live shadow, together.

Simulates what the phone does once draws arrive one per minute:

  every period t (live track)
    1. every program in the research pool freezes its position + K3 decision for t
       (computed from rows < t only)
    2. the draw for t settles every frozen decision (hit / miss)
    3. feedback: each program's live ledger, the director's memory and the macro
       library are updated; the system's own output for t+1 is chosen from live records
  every `refresh_every` periods (history track, background)
    4. a short evolution campaign runs on the newest history (< t), seeded with the
       current survivors; its best children join the pool immediately and from then
       on also freeze/settle live every period

The system-level output is the decision of the live champion (best live Wilson bound
so far, causal). Its hit rate over a span it never trained on is the end-to-end
measure of "keeps creating, keeps improving".
"""
from __future__ import annotations

import math
import time

import numpy as np

import forge
import grammar
import hre


def _live_score(m, t, min_live=60):
    lo = m["born"]
    a = m["act"][lo:t]
    h = m["hit"][lo:t]
    n = int(a.sum())
    k = int((a & h).sum())
    if n < min_live:
        return m["prior"] - 0.05, n, k
    return hre.wilson_lower(k, n), n, k


def dual_track(D, start, end, refresh_every=300, pool_size=12, seed=0, log=None,
               first_generations=6, first_population=32, mini_generations=3, mini_population=16):
    t_begin = time.perf_counter()
    lib = forge.Library(D)
    director = forge.Director(forge.random.Random(seed))
    pool = []
    refreshes = []

    def admit(rep, born, keep):
        cache, progs = rep["cache"], rep["programs"]
        ranked = sorted(progs.items(), key=lambda kv: -cache[kv[0]][0])
        have = {m["key"] for m in pool}
        added = 0
        for k, s in ranked:
            if added >= keep:
                break
            if k in have:
                continue
            r = forge.evaluate(lib, s, born, end)        # causal: decisions at t use rows < t
            pool.append({"key": k, "strategy": s, "born": born, "prior": cache[k][0],
                         "act": r.act.copy(), "hit": r.hit.copy(), "family": s.arm or s.family})
            added += 1
        return added

    def eras_at(t):
        return {"dev": (max(1500, t - 4200), t - 1000), "val": (t - 1000, t), "sealed": (t, end)}

    rep = forge.campaign(D, generations=first_generations, population=first_population, seed=seed,
                         eras=eras_at(start), lib=lib, director=director, screen=True)
    admit(rep, start, pool_size)
    refreshes.append({"t": start, "added": len(pool), "macros": len(grammar.MACROS),
                      "director_arms": len(director.fam)})
    sys_act = np.zeros(len(D), bool)
    sys_hit = np.zeros(len(D), bool)
    champ_trace = []
    for t in range(start, end):
        if t > start and (t - start) % refresh_every == 0:
            scored = sorted(pool, key=lambda m: -_live_score(m, t)[0])
            survivors = scored[: max(4, pool_size // 2)]
            pool[:] = survivors
            rep = forge.campaign(D, generations=mini_generations, population=mini_population,
                                 seed=seed + t, eras=eras_at(t), lib=lib, director=director,
                                 seed_pop=[m["strategy"] for m in survivors], fresh_library=False)
            added = admit(rep, t, pool_size - len(pool))
            refreshes.append({"t": t, "added": added, "pool": len(pool), "macros": len(grammar.MACROS),
                              "director_arms": len(director.fam),
                              "created_families": [c["family"] for c in rep["created_families"]]})
            if log:
                log(refreshes[-1])
        best = max(pool, key=lambda m: _live_score(m, t)[0])
        sys_act[t] = best["act"][t]
        sys_hit[t] = best["hit"][t]
        champ_trace.append(best["key"])
    r = hre.Replay(act=sys_act, hit=sys_hit, pool=sys_act, position=None, numbers=None, seconds=0.0)
    members = []
    for m in pool:
        sc, n, k = _live_score(m, end)
        members.append({"key": m["key"], "family": m["family"], "born": m["born"], "live_n": n,
                        "live_hits": k, "live_rate": round(k / n, 4) if n else None,
                        "program": m["strategy"].describe()[:240]})
    switches = sum(1 for a, b in zip(champ_trace, champ_trace[1:]) if a != b)
    return {"span": [start, end], "system_output": hre.summarize(r, start, end),
            "champion_switches": switches, "refreshes": refreshes, "final_pool": members,
            "learned_macros": grammar.macro_report(8), "director": director.snapshot(),
            "seconds": round(time.perf_counter() - t_begin, 1)}
