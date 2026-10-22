package com.ethk3.novaonepro;

/** Lagged number-position transition tensor with Dirichlet shrinkage. */
public final class V51TransitionTensorEngine {
    public static final int MAX_LAG = 12;

    public double score(V51Models.DrawFrame[] history, int number, int targetPosition) {
        if (history == null || history.length < 24) return 0.0d;
        double weighted = 0, weightSum = 0;
        int limit = Math.min(history.length - 1, 720);
        for (int lag = 1; lag <= MAX_LAG; lag++) {
            int[][] count = new int[10][10];
            int[] total = new int[10];
            for (int t = Math.max(lag, history.length - limit); t < history.length; t++) {
                int from = find(history[t - lag].permutation, number),
                        to = find(history[t].permutation, number);
                count[from][to]++;
                total[from]++;
            }
            int current = find(history[history.length - lag].permutation, number);
            double p = (count[current][targetPosition] + 1.0d) / (total[current] + 10.0d);
            double reliability = total[current] / (total[current] + 40.0d);
            double w = reliability / lag;
            weighted += w * V51Math.safeLog(p / 0.1d);
            weightSum += w;
        }
        return weightSum == 0 ? 0 : weighted / weightSum;
    }

    private static int find(int[] p, int number) {
        for (int i = 0; i < p.length; i++) if (p[i] == number) return i;
        return 0;
    }
}
