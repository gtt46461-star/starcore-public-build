package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** R27 shared statistics. Pure Java; no Android or database dependency.
 * Every interval here is descriptive; only SequentialQualificationR27 grants qualification. */
public final class StatsR27 {
    private StatsR27() {}
    public static final double BASE = 0.30;
    public static final double Z95 = 1.959963984540054, Z95_ONE_SIDED = 1.6448536269514722;

    /** Wilson score interval for k successes in n trials; n==0 gives [0,1]. */
    public static double[] wilson(long k, long n, double z) {
        if (n <= 0) return new double[] {0, 1};
        double p = k / (double) n, z2 = z * z, d = 1 + z2 / n, c = (p + z2 / (2.0 * n)) / d;
        double h = z * Math.sqrt(p * (1 - p) / n + z2 / (4.0 * n * n)) / d;
        return new double[] {Math.max(0, c - h), Math.min(1, c + h)};
    }

    /** Running mean/variance accumulator (Welford). */
    public static final class Moments {
        public long n; public double mean, m2;
        public void add(double x) { n++; double d = x - mean; mean += d / n; m2 += d * (x - mean); }
        public double variance() { return n < 2 ? 0 : m2 / (n - 1); }
        public double se() { return n < 2 ? Double.POSITIVE_INFINITY : Math.sqrt(variance() / n); }
        /** One-sided lower confidence bound of the mean. */
        public double lcb(double z) { return n < 2 ? Double.NEGATIVE_INFINITY : mean - z * se(); }
        public double ucb(double z) { return n < 2 ? Double.POSITIVE_INFINITY : mean + z * se(); }
    }

    public static Moments moments(double[] xs, int from, int to) {
        Moments m = new Moments();
        for (int i = from; i < to; i++) if (!Double.isNaN(xs[i])) m.add(xs[i]);
        return m;
    }

    /** Expected-calibration error with equal-width bins over [0,1]. */
    public static double ece(double[] p, int[] y, int n, int bins) {
        if (n == 0) return Double.NaN;
        double[] sp = new double[bins], sy = new double[bins]; int[] c = new int[bins];
        for (int i = 0; i < n; i++) { int b = Math.min(bins - 1, Math.max(0, (int) (p[i] * bins))); sp[b] += p[i]; sy[b] += y[i]; c[b]++; }
        double e = 0;
        for (int b = 0; b < bins; b++) if (c[b] > 0) e += Math.abs(sp[b] - sy[b]) / n;
        return e;
    }

    /** E_max(N,k): expected best hit rate among k zero-edge strategies with N samples each (docs/01 1.2-D). */
    public static double luckCeiling(int n, int k, long seed, int sims) {
        if (n <= 0 || k <= 0) return Double.NaN;
        SplitMix rng = new SplitMix(seed);
        double sum = 0;
        for (int s = 0; s < sims; s++) {CooperativeContinuationR26.poll();
            int best = 0;
            for (int j = 0; j < k; j++) { int h = 0; for (int i = 0; i < n; i++) {if((i&511)==0)CooperativeContinuationR26.poll();if (rng.nextDouble() < BASE) h++;} if (h > best) best = h; }
            sum += best / (double) n;
        }
        return sum / sims;
    }

    public static double clamp(double x, double lo, double hi) { return x < lo ? lo : x > hi ? hi : x; }

    public static String sha256(String text) { return sha256(text.getBytes(StandardCharsets.UTF_8)); }
    public static String sha256(byte[] bytes) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder b = new StringBuilder(64);
            for (byte x : d) b.append(Character.forDigit((x >> 4) & 15, 16)).append(Character.forDigit(x & 15, 16));
            return b.toString();
        } catch (Exception e) { throw new IllegalStateException("R27_SHA256", e); }
    }

    /** Deterministic, serializable RNG (SplitMix64). State is one long, so evolution cursors can persist it. */
    public static final class SplitMix {
        public long state;
        public SplitMix(long seed) { state = seed; }
        public long nextLong() { long z = (state += 0x9E3779B97F4A7C15L); z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L; z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL; return z ^ (z >>> 31); }
        public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }
        public int nextInt(int bound) { if (bound <= 0) throw new IllegalArgumentException("R27_RNG_BOUND"); return (int) Math.floorMod(nextLong(), (long) bound); }
        public double gaussian() { double u = Math.max(1e-300, nextDouble()), v = nextDouble(); return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v); }
        public boolean chance(double p) { return nextDouble() < p; }
    }
}
