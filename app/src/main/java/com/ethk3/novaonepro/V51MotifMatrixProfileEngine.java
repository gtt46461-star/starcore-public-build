package com.ethk3.novaonepro;

/** Multi-length, exclusion-zone motif successor estimator over whole permutations. */
public final class V51MotifMatrixProfileEngine {
    public static final int[] LENGTHS = new int[] {6, 12, 24, 48};

    public double score(V51Models.DrawFrame[] history, int position, int number) {
        if (history == null || history.length < 16) return 0.0d;
        return scoreSurface(history)[position][number - 1];
    }

    /** The nearest whole-permutation motif is common to all 100 output cells.
     * Compute it once. Preserve the original distance/addition order and first-tie
     * rule so this changes work, not the frozen strategy or action probabilities. */
    public double[][] scoreSurface(V51Models.DrawFrame[] history) {
        double[][] total = new double[10][10];
        if (history == null || history.length < 16) return total;
        int searchStart = Math.max(0, history.length - 720);
        int[][] inverse = new int[history.length - searchStart][];
        for (int i = searchStart; i < history.length; i++)
            inverse[i - searchStart] = inverse(history[i].permutation);
        double weight = 0;
        for (int li = 0; li < LENGTHS.length; li++) {
            int length = LENGTHS[li];
            if (history.length < length * 2 + 2) continue;
            int queryStart = history.length - length;
            double best = Double.POSITIVE_INFINITY;
            int bestEnd = -1;
            int exclusion = Math.max(2, length / 2);
            for (int start = searchStart; start + length < history.length - exclusion; start++) {
                double d = distance(inverse, start - searchStart, queryStart - searchStart, length);
                if (d < best) {
                    best = d;
                    bestEnd = start + length;
                }
            }
            if (bestEnd >= 0 && bestEnd < history.length) {
                double w = Math.exp(-best * 2.0d) * Math.sqrt(length);
                for (int position = 0; position < 10; position++)
                    for (int number = 1; number <= 10; number++) {
                        double successor = history[bestEnd].permutation[position] == number ? 1.0d : 0.0d;
                        total[position][number - 1] += w * (successor - 0.1d);
                    }
                weight += w;
            }
        }
        if (weight != 0)
            for (int p = 0; p < 10; p++)
                for (int n = 0; n < 10; n++) total[p][n] /= weight;
        return total;
    }

    private static double distance(int[][] inverse, int a, int b, int len) {
        double sum = 0;
        int count = 0;
        for (int t = 0; t < len; t++) {
            int[] ia = inverse[a + t], ib = inverse[b + t];
            for (int n = 1; n <= 10; n++) {
                double d = (ia[n] - ib[n]) / 9.0d;
                sum += d * d;
                count++;
            }
        }
        return Math.sqrt(sum / Math.max(1, count));
    }

    private static int[] inverse(int[] p) {
        int[] x = new int[11];
        for (int i = 0; i < 10; i++) x[p[i]] = i;
        return x;
    }
}
