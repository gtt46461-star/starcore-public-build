package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * R14b (P39) the atom world, causally, one period at a time: the lifted registry atoms (AtomEvaluatorCP4 semantics,
 * reference/replay/atoms.py) and the PHASE_FREQUENCY atoms R14b adds, for the target period t from draws < t.
 *
 * Every family is recomputed from a ring of the last RING draws (integer counts, so the values are exact and equal
 * to the Python cumulative-sum form), except OMISSION / GAP_LAST_GAP, which keep last-seen times. Missing values
 * (window not yet full, number never seen) are filled with the row mean of the present numbers, then each position
 * is z-scored across its ten numbers (numpy-identical reductions) and clipped to [-4, 4].
 *
 * The catalog order is the Python order (atom_catalog_lifted.json filtered to the families below, then EXTENDED);
 * the block between the GENERATED markers is written by scripts/gen_atom_catalog_su2.py.
 */
public final class AtomStreamSU2 {
    public static final int RING = 1600;
    public static final float CLIP = 4f;

    // BEGIN GENERATED STATES
    static final String[] STATE_SPECS = {
        "ENTROPY|window=10",
        "ENTROPY|window=120",
        "ENTROPY|window=200",
        "ENTROPY|window=20",
        "ENTROPY|window=300",
        "ENTROPY|window=30",
        "ENTROPY|window=500",
        "ENTROPY|window=50",
        "ENTROPY|window=5",
        "ENTROPY|window=80",
        "CONDITIONAL_ENTROPY|lag=13,window=120",
        "CONDITIONAL_ENTROPY|lag=13,window=300",
        "CONDITIONAL_ENTROPY|lag=13,window=500",
        "CONDITIONAL_ENTROPY|lag=13,window=50",
        "CONDITIONAL_ENTROPY|lag=1,window=120",
        "CONDITIONAL_ENTROPY|lag=1,window=300",
        "CONDITIONAL_ENTROPY|lag=1,window=500",
        "CONDITIONAL_ENTROPY|lag=1,window=50",
        "CONDITIONAL_ENTROPY|lag=21,window=120",
        "CONDITIONAL_ENTROPY|lag=21,window=300",
        "CONDITIONAL_ENTROPY|lag=21,window=500",
        "CONDITIONAL_ENTROPY|lag=21,window=50",
        "CONDITIONAL_ENTROPY|lag=2,window=120",
        "CONDITIONAL_ENTROPY|lag=2,window=300",
        "CONDITIONAL_ENTROPY|lag=2,window=500",
        "CONDITIONAL_ENTROPY|lag=2,window=50",
        "CONDITIONAL_ENTROPY|lag=34,window=120",
        "CONDITIONAL_ENTROPY|lag=34,window=300",
        "CONDITIONAL_ENTROPY|lag=34,window=500",
        "CONDITIONAL_ENTROPY|lag=34,window=50",
        "CONDITIONAL_ENTROPY|lag=3,window=120",
        "CONDITIONAL_ENTROPY|lag=3,window=300",
        "CONDITIONAL_ENTROPY|lag=3,window=500",
        "CONDITIONAL_ENTROPY|lag=3,window=50",
        "CONDITIONAL_ENTROPY|lag=5,window=120",
        "CONDITIONAL_ENTROPY|lag=5,window=300",
        "CONDITIONAL_ENTROPY|lag=5,window=500",
        "CONDITIONAL_ENTROPY|lag=5,window=50",
        "CONDITIONAL_ENTROPY|lag=8,window=120",
        "CONDITIONAL_ENTROPY|lag=8,window=300",
        "CONDITIONAL_ENTROPY|lag=8,window=500",
        "CONDITIONAL_ENTROPY|lag=8,window=50",
        "ROLLING_MEAN_DIGIT|window=10",
        "ROLLING_MEAN_DIGIT|window=120",
        "ROLLING_MEAN_DIGIT|window=200",
        "ROLLING_MEAN_DIGIT|window=20",
        "ROLLING_MEAN_DIGIT|window=300",
        "ROLLING_MEAN_DIGIT|window=30",
        "ROLLING_MEAN_DIGIT|window=500",
        "ROLLING_MEAN_DIGIT|window=50",
        "ROLLING_MEAN_DIGIT|window=5",
        "ROLLING_MEAN_DIGIT|window=80",
        "ROLLING_VARIANCE_DIGIT|window=10",
        "ROLLING_VARIANCE_DIGIT|window=120",
        "ROLLING_VARIANCE_DIGIT|window=200",
        "ROLLING_VARIANCE_DIGIT|window=20",
        "ROLLING_VARIANCE_DIGIT|window=300",
        "ROLLING_VARIANCE_DIGIT|window=30",
        "ROLLING_VARIANCE_DIGIT|window=500",
        "ROLLING_VARIANCE_DIGIT|window=50",
        "ROLLING_VARIANCE_DIGIT|window=5",
        "ROLLING_VARIANCE_DIGIT|window=80",
        "DELTA_NUMERIC|lag=13",
        "DELTA_NUMERIC|lag=1",
        "DELTA_NUMERIC|lag=21",
        "DELTA_NUMERIC|lag=2",
        "DELTA_NUMERIC|lag=34",
        "DELTA_NUMERIC|lag=3",
        "DELTA_NUMERIC|lag=55",
        "DELTA_NUMERIC|lag=5",
        "DELTA_NUMERIC|lag=89",
        "DELTA_NUMERIC|lag=8",
        "DIRECTION|lag=13",
        "DIRECTION|lag=1",
        "DIRECTION|lag=21",
        "DIRECTION|lag=2",
        "DIRECTION|lag=34",
        "DIRECTION|lag=3",
        "DIRECTION|lag=55",
        "DIRECTION|lag=5",
        "DIRECTION|lag=89",
        "DIRECTION|lag=8",
        "DRIFT_JS|recent=120,reference=800",
        "DRIFT_JS|recent=20,reference=120",
        "DRIFT_JS|recent=30,reference=200",
        "DRIFT_JS|recent=50,reference=300",
        "DRIFT_JS|recent=80,reference=500",
        "DRIFT_PSI|recent=120,reference=800",
        "DRIFT_PSI|recent=20,reference=120",
        "DRIFT_PSI|recent=30,reference=200",
        "DRIFT_PSI|recent=50,reference=300",
        "DRIFT_PSI|recent=80,reference=500",
        "REGIME_SCORE|regime=DRIFTING,window=120",
        "REGIME_SCORE|regime=DRIFTING,window=300",
        "REGIME_SCORE|regime=DRIFTING,window=500",
        "REGIME_SCORE|regime=DRIFTING,window=50",
        "REGIME_SCORE|regime=HIGH_ENTROPY,window=120",
        "REGIME_SCORE|regime=HIGH_ENTROPY,window=300",
        "REGIME_SCORE|regime=HIGH_ENTROPY,window=500",
        "REGIME_SCORE|regime=HIGH_ENTROPY,window=50",
        "REGIME_SCORE|regime=LOW_ENTROPY,window=120",
        "REGIME_SCORE|regime=LOW_ENTROPY,window=300",
        "REGIME_SCORE|regime=LOW_ENTROPY,window=500",
        "REGIME_SCORE|regime=LOW_ENTROPY,window=50",
        "REGIME_SCORE|regime=MEAN_REVERTING,window=120",
        "REGIME_SCORE|regime=MEAN_REVERTING,window=300",
        "REGIME_SCORE|regime=MEAN_REVERTING,window=500",
        "REGIME_SCORE|regime=MEAN_REVERTING,window=50",
        "REGIME_SCORE|regime=TRENDING,window=120",
        "REGIME_SCORE|regime=TRENDING,window=300",
        "REGIME_SCORE|regime=TRENDING,window=500",
        "REGIME_SCORE|regime=TRENDING,window=50",
    };
    // END GENERATED STATES

    // BEGIN GENERATED CATALOG
    static final String[] SPECS = {
        "FREQUENCY|window=5",
        "FREQUENCY|window=10",
        "FREQUENCY|window=20",
        "FREQUENCY|window=30",
        "FREQUENCY|window=50",
        "FREQUENCY|window=80",
        "FREQUENCY|window=120",
        "FREQUENCY|window=200",
        "FREQUENCY|window=300",
        "FREQUENCY|window=500",
        "FREQUENCY_SLOPE|window=5",
        "FREQUENCY_SLOPE|window=10",
        "FREQUENCY_SLOPE|window=20",
        "FREQUENCY_SLOPE|window=30",
        "FREQUENCY_SLOPE|window=50",
        "FREQUENCY_SLOPE|window=80",
        "FREQUENCY_SLOPE|window=120",
        "FREQUENCY_SLOPE|window=200",
        "FREQUENCY_SLOPE|window=300",
        "FREQUENCY_SLOPE|window=500",
        "GAP_LAST_GAP|window=20",
        "GAP_LAST_GAP|window=50",
        "GAP_LAST_GAP|window=120",
        "GAP_LAST_GAP|window=300",
        "GAP_LAST_GAP|window=500",
        "HOT_COLD_Z|recent=10,reference=50",
        "HOT_COLD_Z|recent=20,reference=120",
        "HOT_COLD_Z|recent=30,reference=200",
        "HOT_COLD_Z|recent=50,reference=300",
        "HOT_COLD_Z|recent=80,reference=500",
        "HOT_COLD_Z|recent=120,reference=800",
        "LAG_DIGIT_EQ|lag=1",
        "LAG_DIGIT_EQ|lag=2",
        "LAG_DIGIT_EQ|lag=3",
        "LAG_DIGIT_EQ|lag=5",
        "LAG_DIGIT_EQ|lag=8",
        "LAG_DIGIT_EQ|lag=13",
        "LAG_DIGIT_EQ|lag=21",
        "LAG_DIGIT_EQ|lag=34",
        "LAG_DIGIT_EQ|lag=55",
        "LAG_DIGIT_EQ|lag=89",
        "MARKOV_SCORE|alpha=0.5,order=1,window=50",
        "MARKOV_SCORE|alpha=0.5,order=1,window=120",
        "MARKOV_SCORE|alpha=0.5,order=1,window=300",
        "MARKOV_SCORE|alpha=0.5,order=1,window=500",
        "MARKOV_SCORE|alpha=1.0,order=1,window=50",
        "MARKOV_SCORE|alpha=1.0,order=1,window=120",
        "MARKOV_SCORE|alpha=1.0,order=1,window=300",
        "MARKOV_SCORE|alpha=1.0,order=1,window=500",
        "OMISSION|",
        "OMISSION_RATE|window=5",
        "OMISSION_RATE|window=10",
        "OMISSION_RATE|window=20",
        "OMISSION_RATE|window=30",
        "OMISSION_RATE|window=50",
        "OMISSION_RATE|window=80",
        "OMISSION_RATE|window=120",
        "OMISSION_RATE|window=200",
        "OMISSION_RATE|window=300",
        "OMISSION_RATE|window=500",
        "PERIODICITY|period=2,window=50",
        "PERIODICITY|period=2,window=120",
        "PERIODICITY|period=2,window=300",
        "PERIODICITY|period=3,window=50",
        "PERIODICITY|period=3,window=120",
        "PERIODICITY|period=3,window=300",
        "PERIODICITY|period=4,window=50",
        "PERIODICITY|period=4,window=120",
        "PERIODICITY|period=4,window=300",
        "PERIODICITY|period=5,window=50",
        "PERIODICITY|period=5,window=120",
        "PERIODICITY|period=5,window=300",
        "PERIODICITY|period=6,window=50",
        "PERIODICITY|period=6,window=120",
        "PERIODICITY|period=6,window=300",
        "PERIODICITY|period=7,window=50",
        "PERIODICITY|period=7,window=120",
        "PERIODICITY|period=7,window=300",
        "PERIODICITY|period=8,window=50",
        "PERIODICITY|period=8,window=120",
        "PERIODICITY|period=8,window=300",
        "PERIODICITY|period=9,window=50",
        "PERIODICITY|period=9,window=120",
        "PERIODICITY|period=9,window=300",
        "PERIODICITY|period=10,window=50",
        "PERIODICITY|period=10,window=120",
        "PERIODICITY|period=10,window=300",
        "PERIODICITY|period=12,window=50",
        "PERIODICITY|period=12,window=120",
        "PERIODICITY|period=12,window=300",
        "PERIODICITY|period=15,window=50",
        "PERIODICITY|period=15,window=120",
        "PERIODICITY|period=15,window=300",
        "PERIODICITY|period=20,window=50",
        "PERIODICITY|period=20,window=120",
        "PERIODICITY|period=20,window=300",
        "PERIODICITY|period=24,window=50",
        "PERIODICITY|period=24,window=120",
        "PERIODICITY|period=24,window=300",
        "PERIODICITY|period=30,window=50",
        "PERIODICITY|period=30,window=120",
        "PERIODICITY|period=30,window=300",
        "PERIODICITY|period=40,window=50",
        "PERIODICITY|period=40,window=120",
        "PERIODICITY|period=40,window=300",
        "PERIODICITY|period=50,window=50",
        "PERIODICITY|period=50,window=120",
        "PERIODICITY|period=50,window=300",
        "ROLLING_VARIANCE_INDICATOR|window=5",
        "ROLLING_VARIANCE_INDICATOR|window=10",
        "ROLLING_VARIANCE_INDICATOR|window=20",
        "ROLLING_VARIANCE_INDICATOR|window=30",
        "ROLLING_VARIANCE_INDICATOR|window=50",
        "ROLLING_VARIANCE_INDICATOR|window=80",
        "ROLLING_VARIANCE_INDICATOR|window=120",
        "ROLLING_VARIANCE_INDICATOR|window=200",
        "ROLLING_VARIANCE_INDICATOR|window=300",
        "ROLLING_VARIANCE_INDICATOR|window=500",
        "PHASE_FREQUENCY|period=2,window=20",
        "PHASE_FREQUENCY|period=2,window=60",
        "PHASE_FREQUENCY|period=3,window=20",
        "PHASE_FREQUENCY|period=3,window=60",
        "PHASE_FREQUENCY|period=4,window=20",
        "PHASE_FREQUENCY|period=4,window=60",
        "PHASE_FREQUENCY|period=5,window=20",
        "PHASE_FREQUENCY|period=5,window=60",
        "PHASE_FREQUENCY|period=6,window=20",
        "PHASE_FREQUENCY|period=6,window=60",
        "PHASE_FREQUENCY|period=7,window=20",
        "PHASE_FREQUENCY|period=7,window=60",
        "PHASE_FREQUENCY|period=8,window=20",
        "PHASE_FREQUENCY|period=8,window=60",
        "PHASE_FREQUENCY|period=10,window=20",
        "PHASE_FREQUENCY|period=10,window=60",
        "PHASE_FREQUENCY|period=12,window=20",
        "PHASE_FREQUENCY|period=12,window=60",
        "PHASE_FREQUENCY|period=24,window=20",
        "PHASE_FREQUENCY|period=24,window=60",
        "GAP_MEAN_GAP|window=20",
        "GAP_MEAN_GAP|window=50",
        "GAP_MEAN_GAP|window=120",
        "GAP_MEAN_GAP|window=300",
        "GAP_MEAN_GAP|window=500",
        "GAP_VAR_GAP|window=20",
        "GAP_VAR_GAP|window=50",
        "GAP_VAR_GAP|window=120",
        "GAP_VAR_GAP|window=300",
        "GAP_VAR_GAP|window=500",
        "GAP_GAP_Z|window=20",
        "GAP_GAP_Z|window=50",
        "GAP_GAP_Z|window=120",
        "GAP_GAP_Z|window=300",
        "GAP_GAP_Z|window=500",
        "XI_EXPERT|expert=0",
        "XI_EXPERT|expert=1",
        "XI_EXPERT|expert=2",
        "XI_EXPERT|expert=3",
        "XI_EXPERT|expert=4",
        "XI_EXPERT|expert=5",
        "XI_EXPERT|expert=6",
        "XI_EXPERT|expert=7",
        "XI_SUPPORT|k=6",
        "XI_SUPPORT|k=7",
        "XI_SUPPORT|k=8",
        "XI_SUPPORT|k=9",
    };
    // END GENERATED CATALOG

    /** One atom: family + integer/double parameters parsed from "FAMILY|k=v,k=v". */
    public static final class Spec {
        public final String family; final Map<String, Double> p = new TreeMap<>(); final Map<String, String> str = new TreeMap<>();
        Spec(String text) {
            int bar = text.indexOf('|'); family = bar < 0 ? text : text.substring(0, bar);
            if (bar >= 0 && bar + 1 < text.length()) for (String kv : text.substring(bar + 1).split(",")) {
                String[] a = kv.split("=");
                try { p.put(a[0], Double.parseDouble(a[1])); } catch (NumberFormatException e) { str.put(a[0], a[1]); }
            }
        }
        int i(String k) { return (int) Math.round(p.get(k)); }
        double d(String k) { return p.get(k); }
        /** Python atom_name / state_name: parameters in key order (string and numeric merged), "order" omitted. */
        public String name() {
            TreeMap<String, String> all = new TreeMap<>(str);
            for (Map.Entry<String, Double> e : p.entrySet()) { double v = e.getValue(); all.put(e.getKey(), v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v)); }
            StringBuilder b = new StringBuilder(family).append('(');
            boolean first = true;
            for (Map.Entry<String, String> e : all.entrySet()) {
                if (e.getKey().equals("order")) continue;
                if (!first) b.append(','); first = false;
                b.append(e.getKey()).append('=').append(e.getValue());
            }
            return b.append(')').toString();
        }
    }

    public static final Spec[] CATALOG, STATES;
    static {
        CATALOG = new Spec[SPECS.length]; for (int i = 0; i < SPECS.length; i++) CATALOG[i] = new Spec(SPECS[i]);
        STATES = new Spec[STATE_SPECS.length]; for (int i = 0; i < STATE_SPECS.length; i++) STATES[i] = new Spec(STATE_SPECS[i]);
    }
    public static int size() { return CATALOG.length; }
    public static int states() { return STATES.length; }
    /** Atoms of the R14b catalog (registry + phase); R15 adds GAP statistics and the decomposed XI atoms after them. */
    public static int r14bSize() { int n = 0; for (Spec s : CATALOG) if (!s.family.startsWith("XI_") && !s.family.equals("GAP_MEAN_GAP") && !s.family.equals("GAP_VAR_GAP") && !s.family.equals("GAP_GAP_Z")) n++; return n; }

    final int[][] ring = new int[RING][10];          // 0-based numbers
    long t;                                           // number of draws seen = index of the next target
    final long[][] last = new long[10][10], prev = new long[10][10];   // last two occurrences (GAP / OMISSION)

    public AtomStreamSU2() { for (long[] r : last) Arrays.fill(r, -1_000_000_000L); for (long[] r : prev) Arrays.fill(r, -1_000_000_000L); }

    public long time() { return t; }
    int x(long u, int p) { return ring[(int) (u % RING)][p]; }
    public int lastDraw(int p, int lag) { return t - lag >= 0 ? x(t - lag, p) : -1; }

    public void update(int[] draw) {
        OnlineModelsSU2.requireDraw(draw);
        int[] r = ring[(int) (t % RING)];
        for (int p = 0; p < 10; p++) { int d = draw[p] - 1; r[p] = d; prev[p][d] = last[p][d]; last[p][d] = t; }
        t++;
    }

    // ------------------------------------------------------------------ raw values (NaN = missing)
    double[][] freq(int w) {
        double[][] v = new double[10][10];
        if (t < w) { for (double[] r : v) Arrays.fill(r, Double.NaN); return v; }
        int[][] c = new int[10][10];
        for (long u = t - w; u < t; u++) for (int p = 0; p < 10; p++) c[p][x(u, p)]++;
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = c[p][n] / (double) w;
        return v;
    }

    double[][] raw(Spec s) {
        double[][] v = new double[10][10];
        switch (s.family) {
            case "FREQUENCY": return freq(s.i("window"));
            case "ROLLING_VARIANCE_INDICATOR": {
                double[][] f = freq(s.i("window"));
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = f[p][n] * (1 - f[p][n]);
                return v;
            }
            case "FREQUENCY_SLOPE": {
                int w = s.i("window");
                if (t < w) { for (double[] r : v) Arrays.fill(r, Double.NaN); return v; }
                long lo = t - w; long[][] bins = new long[10][10], wt = new long[10][10];
                for (long u = lo; u < t; u++) for (int p = 0; p < 10; p++) { int a = x(u, p); bins[p][a]++; wt[p][a] += u - lo; }
                double den = w * (w * (double) w - 1.0) / 12;
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = ((double) wt[p][n] - (w - 1) * (double) bins[p][n] / 2) / den;
                return v;
            }
            case "OMISSION":
            case "OMISSION_RATE": {
                boolean rate = s.family.equals("OMISSION_RATE"); int w = rate ? s.i("window") : 0;
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    double g = last[p][n] >= 0 ? (double) (t - last[p][n]) : Double.NaN;
                    double o = g - 1;
                    v[p][n] = !rate ? o : (t < w ? Double.NaN : (Double.isNaN(o) ? Double.NaN : Math.min(o, w) / w));
                }
                return v;
            }
            case "HOT_COLD_Z": {
                int r = s.i("recent"), R = s.i("reference");
                double[][] a = freq(r), z = freq(R);
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    double var = z[p][n] * (1 - z[p][n]) * (1.0 / r - 1.0 / R);
                    v[p][n] = var > 1e-12 ? (a[p][n] - z[p][n]) / Math.sqrt(Math.max(var, 1e-12)) : Double.NaN;
                }
                return v;
            }
            case "MARKOV_SCORE": {
                int w = s.i("window"); double alpha = s.d("alpha");
                if (t < w + 1) { for (double[] r : v) Arrays.fill(r, Double.NaN); return v; }
                for (int p = 0; p < 10; p++) {
                    int from = x(t - 1, p); long[] row = new long[10];
                    for (long j = Math.max(0, t - w - 1); j <= t - 2; j++) if (x(j, p) == from) row[x(j + 1, p)]++;
                    long sum = 0; for (long c : row) sum += c;
                    for (int n = 0; n < 10; n++) v[p][n] = (row[n] + alpha) / (sum + 10 * alpha);
                }
                return v;
            }
            case "LAG_DIGIT_EQ": {
                int l = s.i("lag");
                if (t >= l) for (int p = 0; p < 10; p++) v[p][x(t - l, p)] = 1.0;
                return v;
            }
            case "GAP_LAST_GAP": {
                int w = s.i("window");
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++)
                    v[p][n] = t >= w && prev[p][n] >= t - w ? (double) (last[p][n] - prev[p][n]) : Double.NaN;
                return v;
            }
            case "PERIODICITY": {
                int w = s.i("window"), L = s.i("period");
                if (t < w + L) { for (double[] r : v) Arrays.fill(r, Double.NaN); return v; }
                long lo = t - w; double nn = w;
                long[][] A = new long[10][10], B = new long[10][10], AB = new long[10][10];
                for (long u = lo; u < t; u++) for (int p = 0; p < 10; p++) { int cur = x(u, p), lg = x(u - L, p); A[p][lg]++; B[p][cur]++; if (cur == lg) AB[p][cur]++; }
                for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                    double a = A[p][n], b = B[p][n], ab = AB[p][n];
                    double den = Math.sqrt(Math.max((a - a * a / nn) * (b - b * b / nn), 0));
                    v[p][n] = den > 1e-12 ? (ab - a * b / nn) / Math.max(den, 1e-12) : Double.NaN;
                }
                return v;
            }
            case "GAP_MEAN_GAP": case "GAP_VAR_GAP": case "GAP_GAP_Z": return gapStats(s.family, s.i("window"));
            case "XI_SUPPORT": {
                float[][][] Z = experts(); int k = s.i("k");
                for (int p = 0; p < 10; p++) {
                    int[][] rank = new int[8][];
                    for (int e = 0; e < 8; e++) rank[e] = ranks(Z[e][p]);
                    for (int n = 0; n < 10; n++) { double[] in = new double[8]; for (int e = 0; e < 8; e++) in[e] = rank[e][n] < k ? 1.0 : 0.0; v[p][n] = ModelOpsSU2.pairwise(in, 0, 8) / 8; }
                }
                return v;
            }
            default: throw new IllegalArgumentException("SU2_ATOM_FAMILY " + s.family);
        }
    }

    /** AtomWindowCP4.Window.gap over the occurrences of each number inside [t-w, t) (NaN = Missing). */
    double[][] gapStats(String family, int w) {
        double[][] v = new double[10][10];
        for (double[] r : v) Arrays.fill(r, Double.NaN);
        if (t < w) return v;
        for (int p = 0; p < 10; p++) {
            long[][] occ = new long[10][w]; int[] cnt = new int[10];
            for (long u = t - w; u < t; u++) { int a = x(u, p); occ[a][cnt[a]++] = u; }
            for (int k = 0; k < 10; k++) {
                int n = cnt[k] - 1; if (n < 1) continue;
                long s1 = 0, s2 = 0; for (int i = 1; i < cnt[k]; i++) { long g = occ[k][i] - occ[k][i - 1]; s1 += g; s2 += g * g; }
                double mean = s1 / (double) n, var = Math.max(0.0, s2 / (double) n - mean * mean);
                if (family.equals("GAP_MEAN_GAP")) v[p][k] = mean;
                else if (family.equals("GAP_VAR_GAP")) v[p][k] = var;
                else if (n >= 2 && var >= 1e-12) v[p][k] = ((double) (occ[k][cnt[k] - 1] - occ[k][cnt[k] - 2]) - mean) / Math.sqrt(var);
            }
        }
        return v;
    }

    // ------------------------------------------------------------------ the 8 experts of the user's XI method (hre.ExpertBank)
    private long expT = -1; private float[][][] expZ;
    /** Z[e][p][n] at the current target, hre.ExpertBank dtypes and numpy reductions (windows 12..360, omission, markov720, momentum). */
    public float[][][] experts() {
        if (expT == t) return expZ;
        float[][][] Z = new float[8][10][10];
        int[] wins = {12, 24, 48, 96, 360};
        double[][][] r64 = new double[6][10][10];
        for (int wi = 0; wi < 5; wi++) {
            long lo = Math.max(t - wins[wi], 0); long den = Math.max(t - lo, 1);
            float[][] c = new float[10][10];
            for (long u = lo; u < t; u++) for (int p = 0; p < 10; p++) c[p][x(u, p)] += 1f;
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) r64[wi][p][n] = (double) c[p][n] / (double) den;
        }
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) r64[5][p][n] = r64[1][p][n] - r64[4][p][n];
        float[][] om = new float[10][10], mk = new float[10][10];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) om[p][n] = (float) Math.log1p((float) (last[p][n] >= 0 ? t - last[p][n] : t + 1));
        if (t >= 1) {
            for (int p = 0; p < 10; p++) {
                float[] row = new float[10]; int prev = x(t - 1, p);
                for (long j = Math.max(0, t - 2 - 719); j <= t - 2; j++) if (x(j, p) == prev) row[x(j + 1, p)] += 1f;
                float sm = XiIncumbentSU2.pairwise10(row);
                for (int n = 0; n < 10; n++) mk[p][n] = (row[n] + 1.0f) / (sm + 10.0f);
            }
        } else for (float[] r : mk) Arrays.fill(r, 0.1f);
        int[] map = {0, 1, 2, 3, 4, -1, -2, 5};
        for (int e = 0; e < 8; e++) for (int p = 0; p < 10; p++) {
            if (map[e] >= 0) {
                double[] v = r64[map[e]][p]; double mu = ModelOpsSU2.pairwise(v, 0, 10) / 10;
                double[] q = new double[10]; for (int n = 0; n < 10; n++) { double d = v[n] - mu; q[n] = d * d; }
                double sd = Math.sqrt(ModelOpsSU2.pairwise(q, 0, 10) / 10) + 1e-6;
                for (int n = 0; n < 10; n++) Z[e][p][n] = (float) ((v[n] - mu) / sd);
            } else {
                float[] v = map[e] == -1 ? om[p] : mk[p]; float mu = XiIncumbentSU2.pairwise10(v) / 10f;
                float[] q = new float[10]; for (int n = 0; n < 10; n++) { float d = v[n] - mu; q[n] = d * d; }
                float sd = (float) Math.sqrt(XiIncumbentSU2.pairwise10(q) / 10f) + 1e-6f;
                for (int n = 0; n < 10; n++) Z[e][p][n] = (v[n] - mu) / sd;
            }
        }
        expT = t; expZ = Z; return Z;
    }

    /** hre.ExpertBank.Rank: stable argsort of -(Z - n*1e-4) (float32), then the rank of every number. */
    static int[] ranks(float[] z) {
        Integer[] ord = new Integer[10]; float[] key = new float[10];
        for (int n = 0; n < 10; n++) { ord[n] = n; key[n] = -(z[n] - (float) n * 1e-4f); }
        Arrays.sort(ord, (a, b) -> Float.compare(key[a], key[b]));
        int[] r = new int[10]; for (int i = 0; i < 10; i++) r[ord[i]] = i; return r;
    }

    // ------------------------------------------------------------------ R15 state atoms (atoms_r15.state_series / state_indicator)
    static double entropy(double[] c) { double n = ModelOpsSU2.pairwise(c, 0, c.length); if (n == 0) return Double.NaN; double h = 0; for (double v : c) if (v > 0) { double q = v / n; h -= q * Math.log(q); } return h; }
    static double[] probs(double[] c) { double n = 0.5 * c.length; for (double v : c) n += v; double[] o = new double[c.length]; for (int i = 0; i < c.length; i++) o[i] = (c[i] + 0.5) / n; return o; }
    static double js(double[] a, double[] b) {
        double out = 0;
        for (int i = 0; i < a.length; i++) { double hi = Math.max(a[i], b[i]); if (hi == 0) continue; double lm = Math.log(hi) + Math.log1p(Math.min(a[i], b[i]) / hi) - Math.log(2);
            if (a[i] > 0) out += a[i] * (Math.log(a[i]) - lm) / 2; if (b[i] > 0) out += b[i] * (Math.log(b[i]) - lm) / 2; }
        return Math.max(0.0, out);
    }
    static double psi(double[] a, double[] b) { double out = 0; for (int i = 0; i < a.length; i++) out += (a[i] - b[i]) * (Math.log(a[i]) - Math.log(b[i])); return Math.max(0.0, out); }
    double[] countsAt(long u, int w, int p) { double[] c = new double[10]; for (long v = u - w; v < u; v++) c[x(v, p)] += 1; return c; }
    static final String[] REGIMES = {"LOW_ENTROPY", "HIGH_ENTROPY", "TRENDING", "MEAN_REVERTING", "DRIFTING"};

    /** State series value at target u (draws < u), all positions; NaN = Missing. */
    double[] stateAt(Spec s, long u) {
        double[] v = new double[10]; Arrays.fill(v, Double.NaN);
        switch (s.family) {
            case "DELTA_NUMERIC": case "DIRECTION": {
                int l = s.i("lag"); if (u < l + 1) return v;
                for (int p = 0; p < 10; p++) { double d = x(u - 1, p) - x(u - 1 - l, p); v[p] = s.family.equals("DIRECTION") ? Math.signum(d) : d; }
                return v;
            }
            case "ENTROPY": case "ROLLING_MEAN_DIGIT": case "ROLLING_VARIANCE_DIGIT": {
                int w = s.i("window"); if (u < w) return v;
                for (int p = 0; p < 10; p++) {
                    double[] c = countsAt(u, w, p);
                    if (s.family.equals("ENTROPY")) { v[p] = entropy(c); continue; }
                    double m = 0; for (int d = 0; d < 10; d++) m += d * c[d]; m /= w;
                    if (s.family.equals("ROLLING_MEAN_DIGIT")) { v[p] = m; continue; }
                    double q = 0; for (int d = 0; d < 10; d++) q += (d - m) * (d - m) * c[d]; v[p] = q / w;
                }
                return v;
            }
            case "CONDITIONAL_ENTROPY": {
                int w = s.i("window"), l = s.i("lag"); if (u < w + l) return v;
                for (int p = 0; p < 10; p++) {
                    double[][] J = new double[10][10]; for (long q = u - w; q < u; q++) J[x(q - l, p)][x(q, p)] += 1;
                    double h = 0;
                    for (int a = 0; a < 10; a++) { double n = 0; for (int b = 0; b < 10; b++) n += J[a][b]; for (int b = 0; b < 10; b++) { double c = J[a][b]; if (c > 0) h -= c / w * Math.log(c / n); } }
                    v[p] = h;
                }
                return v;
            }
            case "DRIFT_JS": case "DRIFT_PSI": {
                int r = s.i("recent"), R = s.i("reference"); if (u < r + R) return v;
                for (int p = 0; p < 10; p++) {
                    double[] rc = countsAt(u, r, p), oc = countsAt(u, r + R, p); for (int k = 0; k < 10; k++) oc[k] -= rc[k];
                    double[] a = probs(rc), b = probs(oc); v[p] = s.family.equals("DRIFT_JS") ? js(a, b) : psi(a, b);
                }
                return v;
            }
            case "REGIME_SCORE": {
                int w = s.i("window"); if (u < 2L * w) return v;
                int ri = Arrays.asList(REGIMES).indexOf(s.str.get("regime"));
                for (int p = 0; p < 10; p++) {
                    double[] c = countsAt(u, w, p), old = countsAt(u, 2 * w, p); for (int k = 0; k < 10; k++) old[k] -= c[k];
                    double h = entropy(c) / Math.log(10), j = js(probs(c), probs(old));
                    double[][] J = new double[10][10]; for (long q = u - (w - 1); q < u; q++) J[x(q - 1, p)][x(q, p)] += 1;
                    double a = 0, b = 0, aa = 0, bb = 0, ab = 0;
                    for (int i = 0; i < 10; i++) for (int k = 0; k < 10; k++) { double n = J[i][k]; a += n * i; b += n * k; aa += n * i * i; bb += n * k * k; ab += n * i * k; }
                    double cnt = w - 1, den = Math.sqrt(Math.max(0.0, (aa - a * a / cnt) * (bb - b * b / cnt)));
                    double corr = den <= 1e-12 ? 0.0 : (ab - a * b / cnt) / den;
                    double[] lg = {2 * (1 - h), 2 * h, 2 * Math.max(0.0, corr), 2 * Math.max(0.0, -corr), 4 * j};
                    double[] e = new double[5]; double sm = 0; for (int i = 0; i < 5; i++) { e[i] = Math.exp(lg[i]); sm += e[i]; }
                    v[p] = e[ri] / sm;
                }
                return v;
            }
            default: throw new IllegalArgumentException("SU2_STATE_FAMILY " + s.family);
        }
    }

    public static final int MEDIAN_PAST = 300, MEDIAN_MIN = 100;
    private final Map<Integer, double[][]> stateRing = new HashMap<>();
    private final Map<Integer, long[]> stateTime = new HashMap<>();
    private final Map<Integer, float[]> stateNow = new HashMap<>();
    private final Map<Integer, Long> stateNowT = new HashMap<>();

    /** State indicator at the current target: +1 above / -1 not above the median of the previous 300 present values, 0 otherwise. */
    public float[] state(int j) {
        Long nt = stateNowT.get(j); if (nt != null && nt == t) return stateNow.get(j);
        Spec s = STATES[j];
        double[][] ring = stateRing.computeIfAbsent(j, k -> new double[MEDIAN_PAST][]);
        long[] when = stateTime.computeIfAbsent(j, k -> { long[] a = new long[MEDIAN_PAST]; Arrays.fill(a, -1); return a; });
        long lo = Math.max(0, t - MEDIAN_PAST);
        for (long u = lo; u < t; u++) { int k = (int) (u % MEDIAN_PAST); if (when[k] != u) { ring[k] = stateAt(s, u); when[k] = u; } }
        double[] cur = stateAt(s, t);
        float[] out = new float[10];
        for (int p = 0; p < 10; p++) {
            if (Double.isNaN(cur[p]) || Double.isInfinite(cur[p])) continue;
            double[] col = new double[(int) (t - lo)]; int n = 0;
            for (long u = lo; u < t; u++) { double y = ring[(int) (u % MEDIAN_PAST)][p]; if (!Double.isNaN(y) && !Double.isInfinite(y)) col[n++] = y; }
            if (n < MEDIAN_MIN) continue;
            double[] c2 = Arrays.copyOf(col, n); Arrays.sort(c2);
            double med = n % 2 == 1 ? c2[n / 2] : (c2[n / 2 - 1] + c2[n / 2]) / 2;
            out[p] = cur[p] > med ? 1f : -1f;
        }
        int k = (int) (t % MEDIAN_PAST);
        // the slot of t-300 is no longer needed after this target
        ring[k] = cur; when[k] = t;
        stateNow.put(j, out); stateNowT.put(j, t);
        return out;
    }

    /** Missing -> row mean of present values (all missing -> 0), z-score across numbers; double result. */
    static double[] zrow(double[] row, boolean fill) {
        double[] v = row.clone();
        if (fill) {
            double[] tmp = new double[10]; int cnt = 0;
            for (int n = 0; n < 10; n++) { boolean ok = !Double.isNaN(v[n]) && !Double.isInfinite(v[n]); tmp[n] = ok ? v[n] : 0.0; if (ok) cnt++; }
            double mean = cnt > 0 ? ModelOpsSU2.pairwise(tmp, 0, 10) / cnt : Double.NaN;
            for (int n = 0; n < 10; n++) { boolean ok = !Double.isNaN(v[n]) && !Double.isInfinite(v[n]); if (!ok) v[n] = mean; if (Double.isNaN(v[n])) v[n] = 0.0; }
        }
        double mu = ModelOpsSU2.pairwise(v, 0, 10) / 10;
        double[] sq = new double[10]; for (int n = 0; n < 10; n++) { double d = v[n] - mu; sq[n] = d * d; }
        double sd = Math.sqrt(ModelOpsSU2.pairwise(sq, 0, 10) / 10) + 1e-6;
        double[] out = new double[10]; for (int n = 0; n < 10; n++) out[n] = (v[n] - mu) / sd;
        return out;
    }

    static float clipF(float f) { return f < -CLIP ? -CLIP : (f > CLIP ? CLIP : f); }

    /** PHASE_FREQUENCY(period m, window w): share of n at p among the last w draws u < t with u = t (mod m). */
    double[][] phase(int m, int w) {
        double[][] v = new double[10][10];
        long j = t / m, lo = Math.max(j - w, 0), nCnt = j - lo;
        int[][] c = new int[10][10];
        for (long k = lo; k < j; k++) { long u = (t % m) + k * m; for (int p = 0; p < 10; p++) c[p][x(u, p)]++; }
        double den = Math.max(nCnt, 1);
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) v[p][n] = c[p][n] / den;
        return v;
    }

    /** Atom i for the current target: float [10][10], z-scored per position, clipped. */
    public float[][] atom(int i) {
        Spec s = CATALOG[i]; float[][] out = new float[10][10];
        if (s.family.equals("XI_EXPERT")) {
            float[][] z = experts()[s.i("expert")];
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) out[p][n] = clipF(z[p][n]);
            return out;
        }
        if (s.family.equals("PHASE_FREQUENCY")) {
            double[][] v = phase(s.i("period"), s.i("window"));
            for (int p = 0; p < 10; p++) { double[] z = zrow(v[p], false); for (int n = 0; n < 10; n++) { double c = z[n] < -CLIP ? -CLIP : (z[n] > CLIP ? CLIP : z[n]); out[p][n] = (float) c; } }
            return out;
        }
        double[][] v = raw(s);
        for (int p = 0; p < 10; p++) { double[] z = zrow(v[p], true); for (int n = 0; n < 10; n++) out[p][n] = clipF((float) z[n]); }
        return out;
    }

    /** Atom x atom interaction, re-z-scored per position (atomnet.AtomSource.pair). */
    public static float[][] pair(float[][] a, float[][] b) {
        float[][] out = new float[10][10];
        for (int p = 0; p < 10; p++) {
            double[] v = new double[10]; for (int n = 0; n < 10; n++) v[n] = (double) a[p][n] * (double) b[p][n];
            double[] z = zrow(v, false);
            for (int n = 0; n < 10; n++) { double c = z[n] < -CLIP ? -CLIP : (z[n] > CLIP ? CLIP : z[n]); out[p][n] = (float) c; }
        }
        return out;
    }

    public static final int[] GROUP_WINDOWS = {10, 50, 200};
    public static final int N_GROUP = 2 * GROUP_WINDOWS.length + 2;

    /** Group atoms [10][10][N_GROUP]: odd/even and big/small share at 3 scales and the last value (atomnet.group_features). */
    public float[][][] group() {
        float[][][] out = new float[10][10][N_GROUP];
        for (int gi = 0; gi < 2; gi++) {
            for (int wi = 0; wi < GROUP_WINDOWS.length; wi++) {
                int w = GROUP_WINDOWS[wi]; long lo = Math.max(t - w, 0); double n = Math.max(t - lo, 1);
                for (int p = 0; p < 10; p++) {
                    long sum = 0; for (long u = lo; u < t; u++) sum += sign(gi, x(u, p));
                    double share = t < 1 ? 0.0 : sum / n;
                    for (int k = 0; k < 10; k++) out[p][k][gi * (GROUP_WINDOWS.length + 1) + wi] = (float) (share * sign(gi, k));
                }
            }
            for (int p = 0; p < 10; p++) {
                double lastS = t >= 1 ? sign(gi, x(t - 1, p)) : 0.0;
                for (int k = 0; k < 10; k++) out[p][k][gi * (GROUP_WINDOWS.length + 1) + GROUP_WINDOWS.length] = (float) (lastS * sign(gi, k));
            }
        }
        return out;
    }

    /** +1 for odd (gi=0) / big 6-10 (gi=1) numbers, -1 otherwise; k is 0-based. */
    static int sign(int gi, int k) { int v = k + 1; return gi == 0 ? (v % 2 == 1 ? 1 : -1) : (v >= 6 ? 1 : -1); }

    public void write(DataOutput o) throws IOException {
        o.writeLong(t); int keep = (int) Math.min(t, RING); o.writeInt(keep);
        for (long u = t - keep; u < t; u++) for (int p = 0; p < 10; p++) o.writeByte(x(u, p));
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { o.writeLong(last[p][n]); o.writeLong(prev[p][n]); }
    }

    public void read(DataInput in) throws IOException {
        t = in.readLong(); int keep = in.readInt(); if (keep < 0 || keep > RING || keep > t) throw new IOException("SU2_ATOM_STATE");
        for (long u = t - keep; u < t; u++) { int[] r = ring[(int) (u % RING)]; for (int p = 0; p < 10; p++) r[p] = in.readByte(); }
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { last[p][n] = in.readLong(); prev[p][n] = in.readLong(); }
    }
}
