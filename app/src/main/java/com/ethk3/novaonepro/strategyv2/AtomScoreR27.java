package com.ethk3.novaonepro.strategyv2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** R27-043 atom replay scoring (docs/07 7.1). For every scorable atom a prequential binned forecaster (the
 * AtomicToolWorldR943 Quality rule: 5 bins, (hits+3)/(n+10)) is run over the replay frame, and the per-row Brier gain
 * against the running base rate is accumulated separately on H_TRAIN+H_SELECT and on H_HOLDOUT (counts continue).
 * States: USEFUL (both lower bounds &gt; 0), HARMFUL (upper bound &lt; 0), MASKED_MISSING (missing &gt; 10%), else
 * NEUTRAL. Generator weights: USEFUL ×2, NEUTRAL ×1, HARMFUL ×0.25 (never 0: kept as a control). */
public final class AtomScoreR27 {
    private AtomScoreR27() {}
    public static final String USEFUL = "USEFUL", NEUTRAL = "NEUTRAL", HARMFUL = "HARMFUL", MASKED_MISSING = "MASKED_MISSING";

    public static final class Score {
        public final int atom; public long nSelect, nHoldout, missing; public final StatsR27.Moments select = new StatsR27.Moments(), holdout = new StatsR27.Moments(); public String state;
        Score(int atom) { this.atom = atom; }
        public double missingRate() { return missing / (double) Math.max(1, nSelect + nHoldout + missing); }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("atom", (long) atom); m.put("nSelect", nSelect); m.put("gainSelect", select.mean); m.put("lcbSelect", fin(select.lcb(StatsR27.Z95)));
            m.put("nHoldout", nHoldout); m.put("gainHoldout", holdout.mean); m.put("lcbHoldout", fin(holdout.lcb(StatsR27.Z95))); m.put("missingRate", missingRate()); m.put("state", state); return m;
        }
    }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }

    /** @param values [row][atom] in [0,1] (row index = frame row), present mask, hit label per row;
     *  rows [0,selectEnd) are train+select, [selectEnd, n) holdout. */
    public static Score score(int atom, double[][] values, boolean[][] present, int[] hit, int selectEnd) {
        Score s = new Score(atom); double[] counts = new double[10]; double tn = 0, th = 0;
        for (int t = 0; t < values.length; t++) {if((t&31)==0)CooperativeContinuationR26.poll();
            if (values[t] == null) continue;
            if (!present[t][atom]) { s.missing++; continue; }
            double v = values[t][atom]; int bin = Math.min(4, Math.max(0, (int) (v * 5))), y = hit[t];
            double p = (counts[bin + 5] + 3) / (counts[bin] + 10), base = (th + 3) / (tn + 10);
            double gain = (base - y) * (base - y) - (p - y) * (p - y);
            if (t < selectEnd) { s.select.add(gain); s.nSelect++; } else { s.holdout.add(gain); s.nHoldout++; }
            counts[bin]++; counts[bin + 5] += y; tn++; th += y;
        }
        double ls = s.select.lcb(StatsR27.Z95), us = s.select.ucb(StatsR27.Z95), lh = s.holdout.lcb(StatsR27.Z95);
        s.state = s.missingRate() > 0.10 ? MASKED_MISSING : ls > 0 && lh > 0 ? USEFUL : us < 0 ? HARMFUL : NEUTRAL;
        return s;
    }

    /** Atom group (A0..A11) → AST leaf kinds that read the same information (generator prior mapping). */
    static final String[][] GROUP_LEAVES = {
        {}, {"cross", "crosscond", "neighbor"}, {"gap", "gaprank", "meangap", "fdecay"}, {"trans1", "trans2", "crosscond", "cross"},
        {"freq", "fdecay"}, {"freq", "freqnb", "oddeven", "bigsmall", "sumbucket"}, {"regime", "knn"}, {"regime"}, {"freq", "gap"}, {}, {}, {}};

    public static double weight(String state) { return USEFUL.equals(state) ? 2.0 : HARMFUL.equals(state) ? 0.25 : 1.0; }

    /** Leaf-kind weights for IdeaFamilyR27.Context from atom states (mean weight of the atoms mapped to each kind). */
    public static Map<String, Double> leafWeights(Map<Integer, String> atomStates) {
        Map<String, double[]> acc = new HashMap<>();
        for (Map.Entry<Integer, String> e : atomStates.entrySet()) {
            int g = e.getKey() / 20; if (g >= GROUP_LEAVES.length || MASKED_MISSING.equals(e.getValue())) continue;
            for (String k : GROUP_LEAVES[g]) { double[] a = acc.computeIfAbsent(k, x -> new double[2]); a[0] += weight(e.getValue()); a[1]++; }
        }
        Map<String, Double> out = new HashMap<>(); for (Map.Entry<String, double[]> e : acc.entrySet()) out.put(e.getKey(), e.getValue()[0] / e.getValue()[1]);
        return out;
    }
}
