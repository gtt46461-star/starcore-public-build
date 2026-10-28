package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

/** H06 (a planted signal is found and VERIFIED on the holdout), H07 (no signal: VERIFIED_IMPROVEMENT rate ≤ 5%),
 * H12 (kill after every one of the nine steps and resume: no duplicate births/lineage, identical outcome),
 * R27-029/030 (holdout test against previous, I00 and shuffled-label champions). */
public class R27EvolutionTest {
    static final class Counter extends R27BirthGateTest.Recorder {
        int verified, comparisons; double bestHoldout = Double.NaN;
        @SuppressWarnings("unchecked") @Override public void generation(Map<String, Object> r) {
            super.generation(r); Map<String, Object> h = (Map<String, Object>) r.get("holdout");
            if (h == null || !(h.get("championHoldout") instanceof Map)) return;
            comparisons++; if (Boolean.TRUE.equals(h.get("verified"))) {
                verified++; Object hr = ((Map<String, Object>) h.get("championHoldout")).get("hitRate");
                if (hr instanceof Number) bestHoldout = Double.isNaN(bestHoldout) ? ((Number) hr).doubleValue() : Math.max(bestHoldout, ((Number) hr).doubleValue());
            }
        }
    }

    @Test public void plantedSignalIsFoundAndVerifiedWithin50Generations() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(5000, 11, new int[] {700, 1900}).planted(0.4, 5);
        Counter c = new Counter(); EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 64; cfg.seed = 6;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, new FeatureCacheR27(d), c, () -> 0L);
        int g = 0; while (g < 50 && !(c.verified > 0 && c.bestHoldout >= 0.36)) { loop.runGeneration(); g++; }
        assertTrue("verified=" + c.verified + " bestHoldout=" + c.bestHoldout, c.verified > 0 && c.bestHoldout >= 0.36);
        R27TestSupport.log("H06_PASS generations=" + g + " verified=" + c.verified + " championHoldoutHR=" + c.bestHoldout + " champion=" + loop.champion.ast);
    }

    @Test public void noSignalRarelyClaimsImprovement() {
        int runs = 100, verified = 0, comparisons = 0, runsWithAny = 0;
        for (int r = 0; r < runs; r++) {
            ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 1000 + r, new int[] {1000});
            Counter c = new Counter(); EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 32; cfg.seed = 77 + r; cfg.i00PerGeneration = 4;
            EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, new FeatureCacheR27(d), c, () -> 0L);
            for (int g = 0; g < 3; g++) loop.runGeneration();
            verified += c.verified; comparisons += c.comparisons; if (c.verified > 0) runsWithAny++;
        }
        double rate = verified / (double) Math.max(1, comparisons);
        R27TestSupport.log("H07 runs=" + runs + " comparisons=" + comparisons + " verified=" + verified + " rate=" + rate + " runsWithAnyVerified=" + runsWithAny);
        assertTrue("VERIFIED_IMPROVEMENT rate " + rate, rate <= 0.05);
        R27TestSupport.log("H07_PASS");
    }

    @Test public void killAfterEveryStepAndResume() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 42, new int[] {900});
        FeatureCacheR27 cache = new FeatureCacheR27(d);
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 24; cfg.seed = 9; cfg.i00PerGeneration = 3;
        R27BirthGateTest.Recorder straight = new R27BirthGateTest.Recorder();
        EvolutionLoopR27 a = new EvolutionLoopR27(cfg, cache, straight, () -> 0L);
        for (int i = 0; i < 18; i++) a.step();
        R27BirthGateTest.Recorder resumed = new R27BirthGateTest.Recorder();
        EvolutionLoopR27 b = new EvolutionLoopR27(cfg, cache, resumed, () -> 0L);
        Map<String, Integer> cursor = new LinkedHashMap<>();
        for (int i = 0; i < 18; i++) {
            b.step();
            String snap = b.snapshot();          // persisted cursor (process killed here)
            b = EvolutionLoopR27.restore(snap, cfg, new FeatureCacheR27(d), resumed, () -> 0L);
            cursor.merge(b.generation + ":" + b.step, 1, Integer::sum);
        }
        assertEquals(2, b.generation); assertEquals(1, b.step);
        for (int i = 0; i < Math.min(straight.events.size(), resumed.events.size()); i++)
            if (!straight.events.get(i).equals(resumed.events.get(i))) fail("first divergence at event " + i + "\nstraight=" + straight.events.get(i) + "\nresumed =" + resumed.events.get(i));
        assertEquals("same events after kill/resume at every step", straight.events, resumed.events);
        Map<String, Integer> births = new HashMap<>();
        for (String e : resumed.events) if (e.startsWith("P|") && e.endsWith("|BORN") || e.startsWith("L|")) births.merge(e, 1, Integer::sum);
        for (Map.Entry<String, Integer> e : births.entrySet()) assertEquals("duplicate " + e.getKey(), 1, (int) e.getValue());
        for (int v : cursor.values()) assertEquals(1, v); // cursor strictly advanced
        R27TestSupport.log("H12_PASS steps=18 events=" + resumed.events.size() + " uniqueBirthsAndEdges=" + births.size());
    }
}
