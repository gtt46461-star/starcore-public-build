package com.ethk3.novaonepro;

/** Deterministic Hungarian maximum-weight assignment decoder. Returns numbers 1..10 by position. */
public final class V51AssignmentDecoder {
    private V51AssignmentDecoder() {}

    public static int[] decode(double[][] probability) {
        int n = 10;
        if (probability == null || probability.length != n) return new int[0];
        double[][] cost = new double[n + 1][n + 1];
        for (int i = 1; i <= n; i++) {
            if (probability[i - 1] == null || probability[i - 1].length != n) return new int[0];
            for (int j = 1; j <= n; j++)
                cost[i][j] = -V51Math.safeLog(probability[i - 1][j - 1]) + j * 1.0e-13d;
        }
        double[] u = new double[n + 1], v = new double[n + 1];
        int[] p = new int[n + 1], way = new int[n + 1];
        for (int i = 1; i <= n; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[n + 1];
            boolean[] used = new boolean[n + 1];
            for (int j = 1; j <= n; j++) minv[j] = Double.POSITIVE_INFINITY;
            do {
                used[j0] = true;
                int i0 = p[j0], j1 = 0;
                double delta = Double.POSITIVE_INFINITY;
                for (int j = 1; j <= n; j++)
                    if (!used[j]) {
                        double cur = cost[i0][j] - u[i0] - v[j];
                        if (cur < minv[j] - 1.0e-15d) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta - 1.0e-15d
                                || (Math.abs(minv[j] - delta) <= 1.0e-15d && j < j1)) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                for (int j = 0; j <= n; j++)
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else if (j > 0) minv[j] -= delta;
                j0 = j1;
            } while (p[j0] != 0);
            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }
        int[] assignment = new int[n];
        for (int j = 1; j <= n; j++) if (p[j] > 0) assignment[p[j] - 1] = j;
        return V51Math.validPermutation(assignment) ? assignment : new int[0];
    }
}
