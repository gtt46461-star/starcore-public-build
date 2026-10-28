package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import org.junit.Test;

/** H02 (replay vs live identical), H03 (gap segments and SKIPPED_GAP), R27-013/015/017 and the replay speed. */
public class R27ReplayTest {
    static PolicySpecR27 spec(PolicyAstR27.Node ast, int tp) { return new PolicySpecR27(ast, tp, PolicySpecR27.Rule.QUANTILE, 0, 0.2, 1.0, false, 0.02, 0.01, null); }

    @Test public void replayAndLiveAreBitIdenticalOverLast200() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(1400);
        PolicyAstR27.Node ast = PolicyAstR27.op("mix", 0.6, PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 3, 128)),
                PolicyAstR27.op("add", PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans1")), PolicyAstR27.op("zscore", PolicyAstR27.leaf("family", 12))));
        PolicySpecR27 s = spec(ast, 3); double tau = 2.0;
        FeatureCacheR27 full = new FeatureCacheR27(d);
        FeatureCacheR27 previous = null; int compared = 0;
        for (int t = d.size() - 200; t < d.size(); t++) {
            ReplayEvaluatorR27.Trace replay = ReplayEvaluatorR27.run(s, full, d.draws, tau, t, t + 1);
            // Live: only rows < t exist; a placeholder occupies the target slot.
            ReplayDatasetR27 prefix = ReplayDatasetR27.of(rows(d, t)).append(new ReplayDatasetR27.Row("PLACEHOLDER", d.ordinals[t], new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
            FeatureCacheR27 live = new FeatureCacheR27(prefix); if (previous != null) live.extendFrom(previous); previous = live;
            double[] out = ReplayEvaluatorR27.predictNext(s, live, tau);
            if (replay.skipped[t]) { assertTrue(Double.isNaN(out[0])); continue; }
            for (int k = 0; k < 10; k++) assertEquals("row " + t + " p[" + k + "]", Double.doubleToLongBits(replay.lastProbs[k]), Double.doubleToLongBits(out[k]));
            assertEquals(replay.k3[t * 3], (int) out[10]); assertEquals(replay.k3[t * 3 + 1], (int) out[11]); assertEquals(replay.k3[t * 3 + 2], (int) out[12]);
            assertEquals(replay.act[t] ? 1.0 : 0.0, out[13], 0); compared++;
        }
        assertTrue(compared >= 190);
        R27TestSupport.log("H02_PASS compared=" + compared + " bitIdentical=true");
    }
    static java.util.List<ReplayDatasetR27.Row> rows(ReplayDatasetR27 d, int to) {
        java.util.List<ReplayDatasetR27.Row> l = new java.util.ArrayList<>(); for (int i = 0; i < to; i++) l.add(new ReplayDatasetR27.Row(d.issues[i], d.ordinals[i], d.draws[i])); return l;
    }

    @Test public void threeGapsGiveFourSegmentsAndCountedSkips() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 3, new int[] {700, 1500, 2200});
        assertEquals(4, d.segments()); assertEquals(3, d.gaps());
        PolicySpecR27 lag2 = spec(PolicyAstR27.op("zscore", PolicyAstR27.leaf("trans2")), 1);
        ReplayEvaluatorR27.Trace tr = ReplayEvaluatorR27.run(lag2, new FeatureCacheR27(d), d.draws, 1.0, 0, d.size());
        ReplayScoreR27 sc = ReplayScoreR27.of("H_SELECT", tr, 0, d.size());
        // trans2 needs rows t-1 and t-2 in the same segment: 2 rows at the start of each of the 4 segments.
        assertEquals(8, sc.skippedGap); assertEquals(3000, sc.nSched);
        R27TestSupport.log("H03_PASS segments=4 skippedGap=" + sc.skippedGap + " nSched=" + sc.nSched);
    }

    @Test public void su1SegmentedHistoryDoesNotThrowOnGaps() {
        java.util.List<com.ethk3.novaonepro.Models.Draw> rows = new java.util.ArrayList<>(); StatsR27.SplitMix r = new StatsR27.SplitMix(5);
        int n = 0;
        for (int i = 1; i <= 400; i++) {
            if (i == 100 || i == 250 || i == 333) continue; // three missing issues
            com.ethk3.novaonepro.Models.Draw d = new com.ethk3.novaonepro.Models.Draw();
            d.issue = String.format(java.util.Locale.ROOT, "20260301%04d", i); d.numbers = ReplayDatasetR27.randomPermutation(r); d.openTime = 1772300000000L + i * 60000L; d.fetchTime = d.openTime + 3000; rows.add(d); n++;
        }
        CampaignStateSU1.Segmented s = CampaignStateSU1.segmentedHistory(rows);
        assertEquals(4, s.segments); assertEquals(400 - 333, s.rows.size()); assertEquals(n - s.rows.size(), s.skippedGap);
        R27TestSupport.log("R27-017_PASS segments=" + s.segments + " newestSegment=" + s.rows.size() + " SKIPPED_GAP=" + s.skippedGap);
    }

    @Test public void datasetHashAndIncrementalAppend() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(1000, 9, new int[] {400});
        ReplayDatasetR27 head = ReplayDatasetR27.of(rows(d, 999)).append(new ReplayDatasetR27.Row(d.issues[999], d.ordinals[999], d.draws[999]));
        assertEquals(d.datasetHash, head.datasetHash); assertEquals(d.segments(), head.segments());
        R27TestSupport.log("R27-013_PASS hash=" + d.datasetHash.substring(0, 16) + " gaps=" + d.gaps() + "=segments-1");
    }

    @Test public void linearCandidateFullReplaySpeed() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(5000); FeatureCacheR27 f = new FeatureCacheR27(d);
        PolicySpecR27 s = spec(PolicyAstR27.op("sub", PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32)), PolicyAstR27.op("zscore", PolicyAstR27.leaf("gap", 1))), 1);
        ReplayEvaluatorR27.run(s, f, d.draws, 1.0, 0, d.size()); // warm leaves
        long best = Long.MAX_VALUE;
        for (int i = 0; i < 5; i++) { long t0 = System.nanoTime(); double[] sc = ReplayEvaluatorR27.scores(s, f); ReplayEvaluatorR27.run(s, sc, d, d.draws, 1.0, 0, d.size()); best = Math.min(best, System.nanoTime() - t0); }
        R27TestSupport.log("REPLAY_SPEED linear5000ms=" + best / 1e6 + " (phone target ≤50ms; host measurement)");
        assertTrue("host linear 5000-row walk-forward should be well under 50 ms", best / 1e6 < 50);
    }
}
