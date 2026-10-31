package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** R27-014 shared point-in-time feature cache. A leaf value at row t is computed only from rows &lt; t
 * (ReplayLeakageTestR27 / H01 checks this for every leaf kind). Values are flattened [t*10 + (number-1)].
 * Lag-based leaves are NaN when the lag window crosses a missing-issue gap; the evaluator records that
 * row as SKIPPED_GAP instead of inventing continuity. One cache serves every candidate of a dataset. */
public final class FeatureCacheR27 {
    public static final int[] WINDOWS = {8, 32, 128, 512, 2048, 5000};
    public static final int[] HALF_LIVES = {8, 32, 128};
    public final ReplayDatasetR27 data;
    private final int capacity;
    private final Map<String, double[]> cache;
    private long computed, hits;

    public FeatureCacheR27(ReplayDatasetR27 data) { this(data, 48); }
    public FeatureCacheR27(ReplayDatasetR27 data, int capacity) {
        this.data = data; this.capacity = Math.max(4, capacity);
        cache = new LinkedHashMap<String, double[]>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, double[]> e) { return size() > FeatureCacheR27.this.capacity; }
        };
    }
    public synchronized long computedLeaves() { return computed; }
    public synchronized long cacheHits() { return hits; }

    /** Leaf kinds; the int parameters are documented per kind. Every kind is PREDRAW_HISTORY (rows &lt; t). */
    public static final String[] KINDS = {"freq", "fdecay", "freqnb", "gap", "gaprank", "meangap", "trans1", "trans2", "cross", "crosscond",
            "repeat", "neighbor", "oddeven", "bigsmall", "sumbucket", "regime", "knn", "family", "randu"};

    /** @param tp target position 1..10 the policy predicts. */
    public double[] leaf(String kind, int tp, int a, int b) {
        if (!targetDependent(kind)) tp = 0;
        String key = kind + '|' + tp + '|' + a + '|' + b;
        synchronized (this) { double[] v = cache.get(key); if (v != null) { hits++; return v; } }
        // Computed outside the lock so research threads never serialize on one long leaf.
        double[] v = compute(kind, tp, a, b);
        synchronized (this) { double[] raced = cache.get(key); if (raced != null) return raced; computed++; cache.put(key, v); }
        return v;
    }

    static boolean targetDependent(String kind) {
        switch (kind) { case "trans1": case "trans2": case "crosscond": case "repeat": case "sumbucket": case "knn": case "family": return true; default: return false; }
    }

    /** Test/ABI hook: replaces one leaf kind's implementation (used by H11 to simulate a changed operator). */
    public interface LeafOverride { double[] compute(FeatureCacheR27 f, int tp, int a, int b); }
    private final Map<String, LeafOverride> overrides = new java.util.HashMap<>();
    public synchronized void override(String kind, LeafOverride o) { overrides.put(kind, o); cache.clear(); }

    private double[] compute(String kind, int tp, int a, int b) {
        LeafOverride o;
        synchronized (this) { o = overrides.get(kind); }
        if (o != null) return o.compute(this, tp, a, b);
        switch (kind) {
            case "freq": return freq(a, b);
            case "fdecay": return decay(a, b);
            case "freqnb": return neighborFreq(a, b);
            case "gap": return gap(a, false);
            case "gaprank": return gap(a, true);
            case "meangap": return meanGap(a, b);
            case "trans1": return transition(tp, 1);
            case "trans2": return transition(tp, 2);
            case "cross": return cross(a, b);
            case "crosscond": return crossCond(a, tp);
            case "repeat": return cross(tp, 1);
            case "neighbor": return neighbor(a);
            case "oddeven": return parity(a, b, true);
            case "bigsmall": return parity(a, b, false);
            case "sumbucket": return sumBucket(tp);
            case "regime": return regime(b);
            case "knn": return knn(tp, a, b);
            case "family": return family(tp, a);
            case "randu": return randomUniform(a);
            default: throw new IllegalArgumentException("R27_UNKNOWN_LEAF:" + kind);
        }
    }

    private int t() { return data.size(); }
    private double[] nan() { double[] v = new double[t() * 10]; Arrays.fill(v, Double.NaN); return v; }

    /** freq(src,W): share of rows in [t-W,t) where number n sat at position src. */
    double[] freq(int src, int w) {
        int n = t(); double[] v = nan(); int[] count = new int[10];
        for (int t = 0; t < n; t++) {
            int len = Math.min(w, t);
            if (len > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = count[k] / (double) len;
            count[data.draws[t][src - 1] - 1]++;
            if (t - w >= 0) count[data.draws[t - w][src - 1] - 1]--;
        }
        return v;
    }

    /** fdecay(src,halfLife): exponentially weighted occurrence rate. */
    double[] decay(int src, int halfLife) {
        int n = t(); double[] v = nan(); double[] s = new double[10]; double lambda = Math.pow(0.5, 1.0 / halfLife), mass = 0;
        for (int t = 0; t < n; t++) {
            if (mass > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = s[k] / mass;
            for (int k = 0; k < 10; k++) s[k] *= lambda;
            mass = mass * lambda + 1;
            s[data.draws[t][src - 1] - 1] += 1;
        }
        return v;
    }

    /** freqnb(src,W): mean occurrence share at the two neighbouring positions of src. */
    double[] neighborFreq(int src, int w) {
        int left = src == 1 ? 10 : src - 1, right = src == 10 ? 1 : src + 1;
        double[] l = leaf("freq", 0, left, w), r = leaf("freq", 0, right, w), v = new double[l.length];
        for (int i = 0; i < v.length; i++) v[i] = 0.5 * (l[i] + r[i]);
        return v;
    }

    /** gap(src): rows since n last appeared at src, scaled by 1/100 and capped at 1; gaprank: rank of that gap / 9. */
    double[] gap(int src, boolean rank) {
        int n = t(); double[] v = nan(); int[] last = new int[10]; Arrays.fill(last, -1);
        double[] g = new double[10];
        for (int t = 0; t < n; t++) {
            if (t > 0) {
                for (int k = 0; k < 10; k++) g[k] = last[k] < 0 ? 1.0 : Math.min(1.0, (t - last[k]) / 100.0);
                if (!rank) for (int k = 0; k < 10; k++) v[t * 10 + k] = g[k];
                else for (int k = 0; k < 10; k++) { int r = 0; for (int j = 0; j < 10; j++) if (g[j] < g[k] || (g[j] == g[k] && j < k)) r++; v[t * 10 + k] = r / 9.0; }
            }
            last[data.draws[t][src - 1] - 1] = t;
        }
        return v;
    }

    /** meangap(src,W): mean inter-arrival of n at src within the window, scaled 1/30 and capped at 1. */
    double[] meanGap(int src, int w) {
        double[] f = leaf("freq", 0, src, w), v = new double[f.length];
        for (int t = 0; t < t(); t++) {
            int len = Math.min(w, t);
            for (int k = 0; k < 10; k++) { double x = f[t * 10 + k]; v[t * 10 + k] = Double.isNaN(x) ? Double.NaN : Math.min(1.0, len / (x * len + 1.0) / 30.0); }
        }
        return v;
    }

    /** trans{lag}(tp): Laplace-smoothed P(n at tp at t | number at tp at t-lag), counted from consecutive pairs before t. */
    double[] transition(int tp, int lag) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) {
            if (data.consecutive(t, lag)) {
                int x = data.draws[t - lag][tp - 1] - 1;
                for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
                int y = data.draws[t][tp - 1] - 1; c[x][y]++; tot[x]++;
            }
        }
        return v;
    }

    /** cross(src,lag): indicator n == number at src in row t-lag. NaN across a gap. lag must be ≥1. */
    double[] cross(int src, int lag) {
        if (lag < 1) throw new IllegalArgumentException("R27_CAUSAL_LAG");
        int n = t(); double[] v = nan();
        for (int t = 0; t < n; t++) if (data.consecutive(t, lag)) { int x = data.draws[t - lag][src - 1]; for (int k = 0; k < 10; k++) v[t * 10 + k] = k + 1 == x ? 1 : 0; }
        return v;
    }

    /** crosscond(src): Laplace P(n at tp at t | number at src at t-1). */
    double[] crossCond(int src, int tp) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) {
            if (data.consecutive(t, 1)) {
                int x = data.draws[t - 1][src - 1] - 1;
                for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
                int y = data.draws[t][tp - 1] - 1; c[x][y]++; tot[x]++;
            }
        }
        return v;
    }

    /** neighbor(src): indicator that n is cyclically adjacent (±1) to the number at src in row t-1. */
    double[] neighbor(int src) {
        int n = t(); double[] v = nan();
        for (int t = 0; t < n; t++) if (data.consecutive(t, 1)) {
            int x = data.draws[t - 1][src - 1];
            for (int k = 1; k <= 10; k++) { int d = Math.abs(k - x); v[t * 10 + k - 1] = d == 1 || d == 9 ? 1 : 0; }
        }
        return v;
    }

    /** oddeven / bigsmall(src,W): share of window rows whose number at src shares n's parity / size class. */
    double[] parity(int src, int w, boolean odd) {
        int n = t(); double[] v = nan(); int cls1 = 0; // count of class-1 rows in window
        for (int t = 0; t < n; t++) {
            int len = Math.min(w, t);
            if (len > 0) for (int k = 1; k <= 10; k++) { boolean c = odd ? (k & 1) == 1 : k > 5; double share = cls1 / (double) len; v[t * 10 + k - 1] = c ? share : 1 - share; }
            int x = data.draws[t][src - 1]; if (odd ? (x & 1) == 1 : x > 5) cls1++;
            if (t - w >= 0) { int y = data.draws[t - w][src - 1]; if (odd ? (y & 1) == 1 : y > 5) cls1--; }
        }
        return v;
    }

    /** sumbucket(tp): P(n at tp | (sum of positions 1..3 at t-1) mod 10), Laplace-smoothed counts. */
    double[] sumBucket(int tp) {
        int n = t(); double[] v = nan(); double[][] c = new double[10][10];
        for (double[] row : c) Arrays.fill(row, 1.0);
        double[] tot = new double[10]; Arrays.fill(tot, 10.0);
        for (int t = 0; t < n; t++) if (data.consecutive(t, 1)) {
            int[] p = data.draws[t - 1]; int x = (p[0] + p[1] + p[2]) % 10;
            for (int k = 0; k < 10; k++) v[t * 10 + k] = c[x][k] / tot[x];
            c[x][data.draws[t][tp - 1] - 1]++; tot[x]++;
        }
        return v;
    }

    /** regime(W): share of position repeats (x_t,p == x_{t-1},p) over the last W consecutive pairs; same for every n. */
    double[] regime(int w) {
        int n = t(); double[] v = nan(); int[] rep = new int[n]; int sum = 0, len = 0;
        for (int t = 0; t < n; t++) {
            if (len > 0) for (int k = 0; k < 10; k++) v[t * 10 + k] = sum / (10.0 * len);
            if (data.consecutive(t, 1)) { int r = 0; for (int p = 0; p < 10; p++) if (data.draws[t][p] == data.draws[t - 1][p]) r++; rep[t] = r; sum += r; len++; }
            else rep[t] = -1;
            if (t - w >= 0 && rep[t - w] >= 0) { sum -= rep[t - w]; len--; }
        }
        return v;
    }

    /** knn(W,k): next-number distribution at tp after the k most similar past windows of length W (Hamming at tp),
     * searched over at most the 1000 previous rows. NaN when the current W-window crosses a gap. */
    double[] knn(int tp, int w, int k) {
        int n = t(); double[] v = nan(); int col = tp - 1;
        for (int t = w; t < n; t++) {
            if (!data.consecutive(t, w)) continue;
            int from = Math.max(w, t - 1000);
            int[] dist = new int[Math.max(0, t - from)]; int[] at = new int[dist.length]; int m = 0;
            for (int s = from; s < t; s++) {
                if (!data.consecutive(s, w)) continue;
                int d = 0; for (int j = 1; j <= w; j++) if (data.draws[s - j][col] != data.draws[t - j][col]) d++;
                dist[m] = d; at[m] = s; m++;
            }
            if (m == 0) continue;
            double[] h = new double[10]; Arrays.fill(h, 0.5); double tot = 5;
            // partial selection by distance, then by recency (deterministic)
            for (int d = 0; d <= w && tot - 5 < k; d++)
                for (int i = m - 1; i >= 0 && tot - 5 < k; i--) if (dist[i] == d) { h[data.draws[at[i]][col] - 1]++; tot++; }
            for (int j = 0; j < 10; j++) v[t * 10 + j] = h[j] / tot;
        }
        return v;
    }

    /** family(tp, familyIndex): R26 family model run strictly online (predict before update), reset at gaps.
     * One stream per family serves all ten positions, and it is extended incrementally when rows are appended
     * (see {@link #extendFrom}); the last row of the dataset is never fed to the model, so a live placeholder row
     * cannot leak into any value. */
    double[] family(int tp, int index) {
        FamilyStream st;
        synchronized (this) { st = streams.get(index); if (st == null) { st = new FamilyStream(index); streams.put(index, st); } }
        double[] all;
        synchronized (st) { all = st.advance(data); }
        int n = t(); double[] v = new double[n * 10];
        for (int t = 0; t < n; t++) System.arraycopy(all, t * 100 + (tp - 1) * 10, v, t * 10, 10);
        return v;
    }

    private final Map<Integer, FamilyStream> streams = new java.util.HashMap<>();

    static final class FamilyStream {
        final int index; final OnlineModelsSU2.Model model; int fed; double[] values = new double[0]; long lastFedOrdinal = Long.MIN_VALUE;
        final float[][] p = new float[10][10]; final float[] st;
        FamilyStream(int index) { this.index = index; model = FamilyRegistryR26.build(String.format(java.util.Locale.ROOT, "F%02d", index)); st = new float[Math.max(1, model.stateWidth())]; }
        int[] lastFedDraw;
        /** Values for every row of d: prediction at row t uses only rows &lt; t. Rows [0,fed) must be unchanged in d. */
        double[] advance(ReplayDatasetR27 d) {
            int n = d.size();
            if (fed > 0 && (n < fed || d.ordinals[fed - 1] != lastFedOrdinal || !java.util.Arrays.equals(d.draws[fed - 1], lastFedDraw)))
                throw new IllegalStateException("R27_FAMILY_STREAM_PREFIX_CHANGED");
            int known = values.length / 100;
            if (known > n) throw new IllegalStateException("R27_FAMILY_STREAM_SHRUNK");
            double[] out = java.util.Arrays.copyOf(values, n * 100);
            for (int t = fed; t < n; t++) {
                if (t >= known) {
                    if (t == 0) java.util.Arrays.fill(out, 0, 100, Double.NaN);
                    else {
                        if (!d.consecutive(t, 1) && model instanceof TemporalModelR26) ((TemporalModelR26) model).resetTemporalContext();
                        model.predict(p, st);
                        for (int q = 0; q < 10; q++) for (int k = 0; k < 10; k++) { double x = p[q][k]; out[t * 100 + q * 10 + k] = Double.isFinite(x) ? x : Double.NaN; }
                    }
                }
                if (t < n - 1) { model.update(d.draws[t]); fed = t + 1; lastFedOrdinal = d.ordinals[t]; lastFedDraw = d.draws[t].clone(); }
            }
            values = out;
            return out;
        }
    }

    /** Reuses family streams of an older cache whose dataset is a prefix of this one (live incremental path). */
    public void extendFrom(FeatureCacheR27 older) {
        synchronized (older) { for (Map.Entry<Integer, FamilyStream> e : older.streams.entrySet()) synchronized (this) { streams.put(e.getKey(), e.getValue()); } }
    }

    /** randu(seed): deterministic uniform noise per (t,n) — the I00 random-baseline input, trivially causal. */
    double[] randomUniform(int seed) {
        int n = t(); double[] v = new double[n * 10];
        for (int t = 0; t < n; t++) for (int k = 0; k < 10; k++) {
            long z = (data.ordinals[t] * 0x9E3779B97F4A7C15L) ^ ((long) seed * 0xBF58476D1CE4E5B9L) ^ (k * 0x94D049BB133111EBL);
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; z ^= z >>> 31;
            v[t * 10 + k] = (z >>> 11) * 0x1.0p-53;
        }
        return v;
    }
}
