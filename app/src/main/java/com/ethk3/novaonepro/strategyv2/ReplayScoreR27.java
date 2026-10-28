package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** R27-015 score of one policy on one split range (docs/03 3.3 / docs/01 1.4). The denominator is every scheduled
 * row: gap-skipped rows count as no-action opportunities (u=0) and are reported as skippedGap. */
public final class ReplayScoreR27 {
    public final String split;
    public final int nSched, nAct, hits, skippedGap, maxLosingStreak;
    public final double brier, logloss, ece, worstBlockHr, pairedUMean, pairedULcb, wilsonLow, wilsonHigh;

    private ReplayScoreR27(String split, int nSched, int nAct, int hits, int skippedGap, int maxLosingStreak, double brier, double logloss, double ece, double worstBlockHr, double uMean, double uLcb) {
        this.split = split; this.nSched = nSched; this.nAct = nAct; this.hits = hits; this.skippedGap = skippedGap; this.maxLosingStreak = maxLosingStreak;
        this.brier = brier; this.logloss = logloss; this.ece = ece; this.worstBlockHr = worstBlockHr; this.pairedUMean = uMean; this.pairedULcb = uLcb;
        double[] w = StatsR27.wilson(hits, nAct, StatsR27.Z95); wilsonLow = w[0]; wilsonHigh = w[1];
    }

    public double hitRate() { return nAct == 0 ? Double.NaN : hits / (double) nAct; }
    public double actRate() { return nSched == 0 ? 0 : nAct / (double) nSched; }

    public static ReplayScoreR27 of(String split, ReplayEvaluatorR27.Trace tr, int from, int to) {
        int sched = Math.max(0, to - from), act = 0, hits = 0, skipped = 0, streak = 0, maxStreak = 0, n = 0;
        double brier = 0, ll = 0; double[] p = new double[sched]; int[] y = new int[sched];
        StatsR27.Moments u = new StatsR27.Moments();
        double worst = Double.NaN; int bAct = 0, bHit = 0, bRows = 0;
        for (int t = from; t < to; t++) {
            if (tr.skipped[t]) { skipped++; u.add(0); }
            else {
                double ph = tr.pHit[t]; int h = tr.hit[t];
                brier += (ph - h) * (ph - h); ll -= Math.log(Math.max(1e-12, tr.pActual[t])); p[n] = ph; y[n] = h; n++;
                u.add(tr.u[t]);
                if (tr.act[t]) { act++; hits += h; bAct++; bHit += h; if (h == 0) { streak++; maxStreak = Math.max(maxStreak, streak); } else streak = 0; }
            }
            if (++bRows == SplitR27.BLOCK || t == to - 1) {
                if (bAct >= 20) { double hr = bHit / (double) bAct; worst = Double.isNaN(worst) ? hr : Math.min(worst, hr); }
                bRows = 0; bAct = 0; bHit = 0;
            }
        }
        return new ReplayScoreR27(split, sched, act, hits, skipped, maxStreak, n == 0 ? Double.NaN : brier / n, n == 0 ? Double.NaN : ll / n,
                StatsR27.ece(p, y, n, 10), worst, u.n == 0 ? 0 : u.mean, u.lcb(StatsR27.Z95_ONE_SIDED));
    }

    /** Paired one-sided lower bound of mean(uA − uB) over [from,to) (docs/05 5.1). */
    public static StatsR27.Moments paired(ReplayEvaluatorR27.Trace a, ReplayEvaluatorR27.Trace b, int from, int to) {
        StatsR27.Moments m = new StatsR27.Moments();
        for (int t = from; t < to; t++) m.add((a.skipped[t] ? 0 : a.u[t]) - (b.skipped[t] ? 0 : b.u[t]));
        return m;
    }

    /** Constant-0.30 Brier on the realized hits of the same rows (R1 rule: model Brier may not exceed it by >0.005). */
    public static double constantBrier(ReplayEvaluatorR27.Trace tr, int from, int to) {
        double s = 0; int n = 0;
        for (int t = from; t < to; t++) if (!tr.skipped[t]) { double d = StatsR27.BASE - tr.hit[t]; s += d * d; n++; }
        return n == 0 ? Double.NaN : s / n;
    }

    public Map<String, Object> json() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("split", split); m.put("nSched", (long) nSched); m.put("nAct", (long) nAct); m.put("hits", (long) hits); m.put("skippedGap", (long) skippedGap);
        m.put("hitRate", fin(hitRate())); m.put("wilsonLow", wilsonLow); m.put("wilsonHigh", wilsonHigh); m.put("brier", fin(brier)); m.put("logloss", fin(logloss)); m.put("ece", fin(ece));
        m.put("worstBlockHr", fin(worstBlockHr)); m.put("maxLosingStreak", (long) maxLosingStreak); m.put("pairedUMean", pairedUMean); m.put("pairedULcb", fin(pairedULcb));
        return m;
    }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }
}
