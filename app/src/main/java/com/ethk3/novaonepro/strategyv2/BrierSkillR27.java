package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27-035 error charts done right (docs/06 6.4). Chart A: cumulative Brier skill score BSS = 1 − Brier_model /
 * Brier_base against the constant-0.30 forecast, its EWMA (half-life 200) and a 95% band from the paired
 * differences d_t = (0.30−y)² − (p−y)². Chart B: rolling-200 Brier with the band a no-skill model would produce on
 * the same probabilities (labels re-drawn at the window's realized hit frequency — the docs/01 E method). With fewer
 * than 200 samples the charts report INSUFFICIENT instead of drawing a line. The conclusion "better than baseline"
 * is allowed only when the whole band is above 0. */
public final class BrierSkillR27 {
    private BrierSkillR27() {}
    public static final int MIN_N = 200, WINDOW = 200; public static final double HALF_LIFE = 200;

    public static final class Point { public final int index; public final double bss, low, high, ewma; Point(int i, double b, double l, double h, double e) { index = i; bss = b; low = l; high = h; ewma = e; } }

    /** @return cumulative points (one per sample after MIN_N) or empty when N &lt; MIN_N. */
    public static List<Point> cumulative(double[] p, int[] y) {
        List<Point> out = new ArrayList<>(); int n = p.length; if (n < MIN_N) return out;
        double sd = 0, sd2 = 0, sb = 0, ew = 0, ewMass = 0, lambda = Math.pow(0.5, 1 / HALF_LIFE);
        for (int t = 0; t < n; t++) {if((t&31)==0)CooperativeContinuationR26.poll();
            double base = (StatsR27.BASE - y[t]) * (StatsR27.BASE - y[t]), model = (p[t] - y[t]) * (p[t] - y[t]), d = base - model;
            sd += d; sd2 += d * d; sb += base; ew = ew * lambda + d; ewMass = ewMass * lambda + 1;
            int k = t + 1;
            if (k >= MIN_N) {
                double mean = sd / k, var = Math.max(0, (sd2 - k * mean * mean) / (k - 1)), se = Math.sqrt(var / k), mb = sb / k;
                out.add(new Point(t, mean / mb, (mean - StatsR27.Z95 * se) / mb, (mean + StatsR27.Z95 * se) / mb, (ew / ewMass) / mb));
            }
        }
        return out;
    }

    /** Verdict from the last cumulative band (docs/06 6.6). */
    public static String verdict(List<Point> pts) {
        if (pts.isEmpty()) return "INSUFFICIENT";
        Point last = pts.get(pts.size() - 1);
        return last.low > 0 ? "BETTER_THAN_BASELINE" : last.high < 0 ? "WORSE_THAN_BASELINE" : "NOT_DISTINGUISHABLE_FROM_BASELINE";
    }

    /** Rolling-200 Brier and its no-skill 95% band; points every `stride` samples. Each row: {index, brier, bandLow, bandHigh}. */
    public static List<double[]> rolling(double[] p, int[] y, int stride, long seed) {
        List<double[]> out = new ArrayList<>(); int n = p.length; if (n < WINDOW) return out;
        StatsR27.SplitMix r = new StatsR27.SplitMix(seed);
        for (int t = WINDOW - 1; t < n; t += Math.max(1, stride)) {CooperativeContinuationR26.poll();
            double b = 0; int hits = 0;
            for (int i = t - WINDOW + 1; i <= t; i++) { b += (p[i] - y[i]) * (p[i] - y[i]); hits += y[i]; }
            double f = hits / (double) WINDOW; double[] sims = new double[300];
            for (int s = 0; s < sims.length; s++) {if((s&15)==0)CooperativeContinuationR26.poll(); double x = 0; for (int i = t - WINDOW + 1; i <= t; i++) { int h = r.nextDouble() < f ? 1 : 0; x += (p[i] - h) * (p[i] - h); } sims[s] = x / WINDOW; }
            java.util.Arrays.sort(sims);
            out.add(new double[] {t, b / WINDOW, sims[7], sims[292]});
        }
        return out;
    }

    public static Map<String, Object> summary(double[] p, int[] y) {
        Map<String, Object> m = new LinkedHashMap<>(); List<Point> c = cumulative(p, y);
        m.put("n", (long) p.length); m.put("verdict", verdict(c));
        if (!c.isEmpty()) { Point l = c.get(c.size() - 1); m.put("bss", l.bss); m.put("bandLow", l.low); m.put("bandHigh", l.high); m.put("ewma", l.ewma); }
        return m;
    }
}
