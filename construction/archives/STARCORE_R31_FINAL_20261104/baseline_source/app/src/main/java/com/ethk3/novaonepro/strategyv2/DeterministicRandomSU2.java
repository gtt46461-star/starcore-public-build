package com.ethk3.novaonepro.strategyv2;

/** SplitMix64 normals shared bit-for-bit with models.splitmix64_normals (Python reference). */
public final class DeterministicRandomSU2 {
    private DeterministicRandomSU2() {}
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    public static double[] normals(long seed, int n) {
        if (n < 0 || n > 1_000_000) throw new IllegalArgumentException("SU2_RANDOM_COUNT");
        long[] x = {seed * GOLDEN + 0x632BE59BD9B4E019L};
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double u1 = 1.0 - next(x), u2 = next(x);
            out[i] = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
        }
        return out;
    }

    private static double next(long[] x) {
        x[0] += GOLDEN;
        long z = x[0];
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z = z ^ (z >>> 31);
        return (z >>> 11) * (1.0 / (1L << 53));
    }
}
