package com.ethk3.novaonepro.strategyv2;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** R27-043 atom replay scoring (docs/07 7.1). For every scorable atom a prequential binned forecaster (the
 * AtomicToolWorldR943 Quality rule: 5 bins, (hits+3)/(n+10)) is run over the replay frame, and the per-row Brier gain
 * against the running base rate is accumulated on H_TRAIN+H_SELECT and, separately, on its H_SELECT part; H_CONFIRM
 * (H_HOLDOUT) is never read (R30-B03-03; the holdout fields stay empty). States: USEFUL (both lower bounds &gt; 0), HARMFUL (upper bound &lt; 0), MASKED_MISSING (missing &gt; 10%), else
 * NEUTRAL. Generator weights: USEFUL ×2, NEUTRAL ×1, HARMFUL ×0.25 (never 0: kept as a control). */
public final class AtomScoreR27 {
    private AtomScoreR27() {}
    public static final String USEFUL = "USEFUL", NEUTRAL = "NEUTRAL", HARMFUL = "HARMFUL", MASKED_MISSING = "MASKED_MISSING";

    public static final String CONTRIBUTION_DDL_R30="CREATE TABLE IF NOT EXISTS r30_atom_contribution(atom_id TEXT NOT NULL,dataset_hash TEXT NOT NULL,from_row INTEGER NOT NULL,to_row INTEGER NOT NULL,n_periods INTEGER NOT NULL,paired_comparisons INTEGER NOT NULL,mean_gain REAL,lcb95 REAL,source TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(atom_id,dataset_hash))";
    public static final class Contribution {
        public final StatsR27.Moments gains;public long comparisons;
        public Contribution(){this(new StatsR27.Moments(),0);}
        Contribution(StatsR27.Moments values,long pairs){gains=values;comparisons=pairs;}
        public double mean(){return gains.n>0?gains.mean:Double.NaN;}
        public double lower95(){return gains.lcb(StatsR27.Z95_ONE_SIDED);}
        public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("periods",gains.n);m.put("pairedComparisons",comparisons);m.put("mean",gains.n>0?gains.mean:null);m.put("lower95",fin(lower95()));m.put("source","H_SELECT_SAME_PERIOD_PAIRED_BRIER");m.put("status",gains.n<2?"INSUFFICIENT":"MEASURED");return m;}
    }
    public static double pairedBrier(double with,double without,int hit){return (without-hit)*(without-hit)-(with-hit)*(with-hit);}

    public static final class Score {
        public final int atom; public long nSelect, nHoldout, nLateSelect, missing; public final StatsR27.Moments select = new StatsR27.Moments(), holdout = new StatsR27.Moments(), lateSelect = new StatsR27.Moments(); public String state;
        Score(int atom) { this.atom = atom; }
        public Contribution contribution(){return new Contribution(lateSelect,lateSelect.n);}
        public double missingRate() { return missing / (double) Math.max(1, nSelect + nHoldout + missing); }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("atom", (long) atom); m.put("nSelect", nSelect); m.put("gainSelect", select.mean); m.put("lcbSelect", fin(select.lcb(StatsR27.Z95)));
            m.put("nHoldout", nHoldout); m.put("gainHoldout", holdout.mean); m.put("lcbHoldout", fin(holdout.lcb(StatsR27.Z95))); m.put("nLateSelect", nLateSelect); m.put("lcbLateSelect", fin(lateSelect.lcb(StatsR27.Z95))); m.put("contributionR30",contribution().json()); m.put("confirmRead", false); m.put("missingRate", missingRate()); m.put("state", state); return m;
        }
    }
    static Object fin(double d) { return Double.isFinite(d) ? (Object) d : null; }

    /** @param values [row][atom] in [0,1] (row index = frame row), present mask, hit label per row;
     *  rows [0,selectEnd) are H_TRAIN+H_SELECT. R30-B03-03: rows at or after selectEnd (H_CONFIRM) are never read; the
     *  second-stage check uses the H_SELECT part of the same prefix (trainEnd per SplitR27: 60/85 of the prefix). */
    public static Score score(int atom, double[][] values, boolean[][] present, int[] hit, int selectEnd) {
        return score(atom, values, present, hit, (int) Math.round(selectEnd * (60.0 / 85.0)), selectEnd);
    }
    public static Score score(int atom, double[][] values, boolean[][] present, int[] hit, int trainEnd, int selectEnd) {
        Score s = new Score(atom); double[] counts = new double[10]; double tn = 0, th = 0;
        int end = Math.min(values.length, selectEnd);
        for (int t = 0; t < end; t++) {
            if (t >= selectEnd) throw new IllegalStateException("R30_ATOM_SCORE_READS_CONFIRM");
            if (values[t] == null) continue;
            if (!present[t][atom]) { s.missing++; continue; }
            double v = values[t][atom]; int bin = Math.min(4, Math.max(0, (int) (v * 5))), y = hit[t];
            double p = (counts[bin + 5] + 3) / (counts[bin] + 10), base = (th + 3) / (tn + 10);
            double gain = (base - y) * (base - y) - (p - y) * (p - y);
            s.select.add(gain); s.nSelect++;
            if (t >= trainEnd) { s.lateSelect.add(gain); s.nLateSelect++; }
            counts[bin]++; counts[bin + 5] += y; tn++; th += y;
        }
        double ls = s.select.lcb(StatsR27.Z95), us = s.select.ucb(StatsR27.Z95), ll = s.lateSelect.lcb(StatsR27.Z95);
        s.state = s.missingRate() > 0.10 ? MASKED_MISSING : ls > 0 && ll > 0 ? USEFUL : us < 0 ? HARMFUL : NEUTRAL;
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
