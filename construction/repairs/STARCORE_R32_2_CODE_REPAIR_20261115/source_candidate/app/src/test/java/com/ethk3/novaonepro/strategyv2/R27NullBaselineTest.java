package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;

/** R27-056 Java isomorph of tools/r27_null_baseline.py (parts A–F) on the repository CSV, compared with
 * construction/plans/STARCORE_R27_FINAL_20261028/results/r27_null_baseline_results.json. The Python run itself
 * (H18, ±1 percentage point) is tools/r27/run_host_tests.sh. Random streams differ between numpy and SplitMix, so
 * the comparison bands are: A ±1pp per strategy (only tie-breaking is random), D exact to one count, E ±0.01,
 * B/C/F evolution ±2.5pp ("same magnitude", docs/11 R27-056) plus the qualitative facts the plan relies on. */
public final class R27NullBaselineTest {
    static final long SEED = 20261028L;
    int[][] d; int T; StatsR27.SplitMix rng = new StatsR27.SplitMix(SEED);

    int[] top3(double[] s) {
        double[] x = new double[10]; for (int k = 0; k < 10; k++) x[k] = s[k] + 1e-9 * rng.nextDouble();
        Integer[] o = new Integer[10]; for (int k = 0; k < 10; k++) o[k] = k;
        Arrays.sort(o, (a, b) -> Double.compare(x[b], x[a]));
        return new int[] {o[0], o[1], o[2]};
    }
    static boolean in(int v, int[] t) { return v == t[0] || v == t[1] || v == t[2]; }

    interface Score { double[] at(int t); }
    double run(Score s, int start) { int h = 0; for (int t = start; t < T; t++) if (in(d[t][0], top3(s.at(t)))) h++; return h / (double) (T - start); }

    Map<String, Double> partA() {
        int start = 500; Map<String, Double> out = new LinkedHashMap<>();
        out.put("random", run(t -> { double[] v = new double[10]; for (int k = 0; k < 10; k++) v[k] = rng.nextDouble(); return v; }, start));
        for (int w : new int[] {20, 60, 240, 1000}) {
            out.put("hot_W" + w, run(t -> count(t, w, 0, 1), start));
            out.put("cold_W" + w, run(t -> count(t, w, 0, -1), start));
        }
        double[][] gaps = new double[T][10]; int[] last = new int[10]; Arrays.fill(last, -1);
        for (int t = 0; t < T; t++) { for (int k = 0; k < 10; k++) gaps[t][k] = t - last[k]; last[d[t][0]] = t; }
        out.put("longest_gap", run(t -> gaps[t], start));
        out.put("shortest_gap", run(t -> { double[] v = new double[10]; for (int k = 0; k < 10; k++) v[k] = -gaps[t][k]; return v; }, start));
        double[][] tr = new double[10][10], m = new double[T][10]; for (double[] r : tr) Arrays.fill(r, 1);
        for (int t = 1; t < T; t++) { m[t] = tr[d[t - 1][0]].clone(); tr[d[t - 1][0]][d[t][0]]++; }
        out.put("markov1", run(t -> m[t], start));
        double[][] cr = new double[10][10], c = new double[T][10]; for (double[] r : cr) Arrays.fill(r, 1);
        for (int t = 1; t < T; t++) { c[t] = cr[d[t - 1][1]].clone(); cr[d[t - 1][1]][d[t][0]]++; }
        out.put("cross_pos2_lag1", run(t -> c[t], start));
        return out;
    }
    double[] count(int t, int w, int pos, int sign) { double[] v = new double[10]; for (int i = Math.max(0, t - w); i < t; i++) v[d[i][pos]] += sign; return v; }

    // ------------------------------------------------------------------ B/C/F: features + evolution, same as the script
    static final int[] WS = {10, 20, 60, 240}; static final int K = WS.length * 2 + 3;
    static double[][][] features(int[][] d) {
        int T = d.length; double[][][] f = new double[T][10][K]; int[] last = new int[10]; Arrays.fill(last, -1);
        double[][] tr = new double[10][10], cr = new double[10][10]; for (int i = 0; i < 10; i++) { Arrays.fill(tr[i], 1); Arrays.fill(cr[i], 1); }
        for (int t = 1; t < T; t++) {
            int j = 0;
            for (int w : WS) {
                int from = Math.max(0, t - w), den = Math.min(t, w);
                for (int i = from; i < t; i++) { f[t][d[i][0]][j] += 1.0 / den; f[t][d[i][1]][j + 1] += 1.0 / den; }
                j += 2;
            }
            for (int k = 0; k < 10; k++) f[t][k][j] = Math.min(t - 1 - last[k], 100) / 100.0; j++;
            double st = 0, sc = 0; for (int k = 0; k < 10; k++) { st += tr[d[t - 1][0]][k]; sc += cr[d[t - 1][1]][k]; }
            for (int k = 0; k < 10; k++) { f[t][k][j] = tr[d[t - 1][0]][k] / st; f[t][k][j + 1] = cr[d[t - 1][1]][k] / sc; }
            tr[d[t - 1][0]][d[t][0]]++; cr[d[t - 1][1]][d[t][0]]++; last[d[t - 1][0]] = t - 1;
        }
        return f;
    }
    double hit(double[] w, double[][][] f, int[] y, int from, int to) {
        int h = 0; double[] s = new double[10];
        for (int t = from; t < to; t++) { for (int k = 0; k < 10; k++) { double z = 0; for (int j = 0; j < K; j++) z += f[t][k][j] * w[j]; s[k] = z; } if (in(y[t], top3(s))) h++; }
        return h / (double) (to - from);
    }
    double[] evolve(double[][][] f, int[] y) {
        int n = y.length, half = n / 2, popN = 64; double[][] pop = new double[popN][K];
        for (double[] w : pop) for (int j = 0; j < K; j++) w[j] = rng.gaussian();
        double[] fit = new double[popN]; double first = Double.NaN;
        for (int g = 0; g < 30; g++) {
            for (int i = 0; i < popN; i++) fit[i] = hit(pop[i], f, y, 0, half);
            Integer[] o = new Integer[popN]; for (int i = 0; i < popN; i++) o[i] = i; final double[] ff = fit.clone(); Arrays.sort(o, (a, b) -> Double.compare(ff[b], ff[a]));
            if (g == 0) first = ff[o[0]];
            double[][] next = new double[popN][]; for (int i = 0; i < 16; i++) next[i] = pop[o[i]];
            for (int i = 16; i < popN; i++) { double[] a = next[rng.nextInt(16)], b = next[rng.nextInt(16)], c = new double[K]; for (int j = 0; j < K; j++) c[j] = (rng.nextDouble() < .5 ? a[j] : b[j]) + 0.3 * rng.gaussian(); next[i] = c; }
            pop = next;
        }
        int best = 0; double bf = -1; for (int i = 0; i < popN; i++) { double h = hit(pop[i], f, y, 0, half); if (h > bf) { bf = h; best = i; } }
        return new double[] {first, bf, hit(pop[best], f, y, half, n)};
    }
    int[][] plant(double q) {
        int[][] p = new int[T][]; for (int t = 0; t < T; t++) p[t] = d[t].clone();
        for (int t = 1; t < T; t++) if (rng.nextDouble() < q) { int v = p[t - 1][1], j = 0; while (p[t][j] != v) j++; int x = p[t][0]; p[t][0] = p[t][j]; p[t][j] = x; }
        return p;
    }
    static int[][] slice(int[][] a, int from) { return Arrays.copyOfRange(a, from, a.length); }
    static int[] col(int[][] a, int c) { int[] y = new int[a.length]; for (int i = 0; i < a.length; i++) y[i] = a[i][c]; return y; }

    @SuppressWarnings("unchecked")
    @Test public void javaIsomorphMatchesPublishedResults() throws Exception {
        ReplayDatasetR27 data = R27TestSupport.csv(Integer.MAX_VALUE);
        T = data.size(); d = new int[T][10]; for (int t = 0; t < T; t++) for (int k = 0; k < 10; k++) d[t][k] = data.draws[t][k] - 1;
        Map<String, Object> ref = (Map<String, Object>) JsonR27.parse(new String(Files.readAllBytes(new File(R27TestSupport.root(), "construction/plans/STARCORE_R27_FINAL_20261028/results/r27_null_baseline_results.json").toPath()), "UTF-8"));
        assertEquals(((Number) ref.get("draws")).intValue(), T);
        StringBuilder log = new StringBuilder("draws=" + T);
        // A
        Map<String, Object> ra = (Map<String, Object>) ref.get("A_simple_strategies_walk_forward"); double worstA = 0;
        for (Map.Entry<String, Double> e : partA().entrySet()) {
            double r = ((Number) ((Map<String, Object>) ra.get(e.getKey())).get("rate")).doubleValue(), diff = Math.abs(e.getValue() - r); worstA = Math.max(worstA, diff);
            assertTrue(e.getKey() + " java=" + e.getValue() + " python=" + r, diff <= 0.01);
            assertTrue("no simple strategy is far above 30%: " + e.getKey(), e.getValue() < 0.3 + 3 * 0.0047);
        }
        log.append(" A_maxAbsDiff=").append(String.format(Locale.ROOT, "%.4f", worstA));
        // B / C
        int s = 300; double[][][] f = features(d); double[][][] fs = Arrays.copyOfRange(f, s, T); int[] y = col(slice(d, s), 0);
        double[] b = evolve(fs, y);
        int[] shuffled = y.clone(); for (int i = shuffled.length - 1; i > 0; i--) { int j = rng.nextInt(i + 1); int x = shuffled[i]; shuffled[i] = shuffled[j]; shuffled[j] = x; }
        double[] c = evolve(fs, shuffled);
        check(ref, "B_evolution_real_labels", b, log); check(ref, "C_evolution_shuffled_labels", c, log);
        assertTrue("B: training rises across generations", b[1] > b[0]); assertTrue("C: shuffled labels also rise on training", c[1] > c[0]);
        assertTrue("B: held-out future near 30%", Math.abs(b[2] - 0.30) < 0.02); assertTrue("C: held-out future near 30%", Math.abs(c[2] - 0.30) < 0.02);
        // D
        for (int n : new int[] {20, 32, 120}) {
            double[] m = new double[100_000];
            for (int i = 0; i < m.length; i++) { int best = 0; for (int a = 0; a < 43; a++) { int h = 0; for (int k = 0; k < n; k++) if (rng.nextDouble() < 0.3) h++; best = Math.max(best, h); } m[i] = best / (double) n; }
            Arrays.sort(m); Map<String, Object> rd = (Map<String, Object>) ref.get("D_best_of_43_null_N" + n);
            double med = m[m.length / 2], p95 = m[(int) (0.95 * m.length)];
            assertEquals("D median N" + n, ((Number) rd.get("median")).doubleValue(), med, 1.0 / n + 1e-12);
            assertEquals("D p95 N" + n, ((Number) rd.get("p95")).doubleValue(), p95, 1.0 / n + 1e-12);
            log.append(" D").append(n).append("[median=").append(med).append(",p95=").append(p95).append(']');
            assertEquals("luckCeiling (expected best of 43) agrees with the median", med, StatsR27.luckCeiling(n, 43, 7, 4000), 1.5 / n);
        }
        // E
        double[] mins = new double[4000], maxs = new double[4000];
        for (int it = 0; it < 4000; it++) {
            boolean[] h = new boolean[88]; double[] p = new double[88];
            for (int i = 0; i < 88; i++) h[i] = rng.nextDouble() < .3;
            for (int i = 0; i < 88; i++) p[i] = Math.min(.99, Math.max(.01, .3 + .08 * rng.gaussian()));
            double lo = 9, hi = -9;
            for (int i = 20; i <= 88; i++) { double x = 0; for (int k = i - 20; k < i; k++) x += (p[k] - (h[k] ? 1 : 0)) * (p[k] - (h[k] ? 1 : 0)); x /= 20; lo = Math.min(lo, x); hi = Math.max(hi, x); }
            mins[it] = lo; maxs[it] = hi;
        }
        Arrays.sort(mins); Arrays.sort(maxs); Map<String, Object> re = (Map<String, Object>) ref.get("E_rolling20_brier_pure_noise_88");
        assertEquals(((Number) re.get("median_min")).doubleValue(), mins[2000], 0.01); assertEquals(((Number) re.get("median_max")).doubleValue(), maxs[2000], 0.01);
        log.append(" E[medianMin=").append(String.format(Locale.ROOT, "%.4f", mins[2000])).append(",medianMax=").append(String.format(Locale.ROOT, "%.4f", maxs[2000])).append(']');
        // F
        for (double q : new double[] {0.05, 0.10}) {
            int[][] dp = plant(q); double[][][] fp = Arrays.copyOfRange(features(dp), s, T);
            double[] r = evolve(fp, col(slice(dp, s), 0));
            check(ref, "F_planted_signal_q" + q, r, log);
            if (q == 0.10) assertTrue("F q=0.10: planted signal found on held-out future", r[2] > 0.33);
        }
        R27TestSupport.log("R27_056_JAVA_ISOMORPH_PASS " + log);
    }
    @SuppressWarnings("unchecked")
    static void check(Map<String, Object> ref, String key, double[] got, StringBuilder log) {
        Map<String, Object> r = (Map<String, Object>) ref.get(key);
        double train = ((Number) r.get("best_train")).doubleValue(), fut = ((Number) r.get("best_heldout_future")).doubleValue();
        assertEquals(key + " best_train", train, got[1], 0.025); assertEquals(key + " best_heldout_future", fut, got[2], 0.025);
        log.append(' ').append(key).append("[train=").append(String.format(Locale.ROOT, "%.4f", got[1])).append("/py ").append(String.format(Locale.ROOT, "%.4f", train))
           .append(",future=").append(String.format(Locale.ROOT, "%.4f", got[2])).append("/py ").append(String.format(Locale.ROOT, "%.4f", fut)).append(']');
    }
}
