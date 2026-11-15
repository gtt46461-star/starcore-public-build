package com.ethk3.novaonepro.strategyv2;

/** R27-045 100-surface / 1,200-action consistency (docs/07 7.3). A surface is 10 positions × 10 numbers with each
 * position summing to 1; an action is (position, K3) with probability = sum of the three marginals (R26 rule kept);
 * Rank1/2/3 are the three largest numbers at the position. */
public final class MatrixConsistencyR27 {
    private MatrixConsistencyR27() {}
    public static final double TOLERANCE = 1e-9;

    /** The R27 readout surface: target position from the policy probabilities, all other positions uniform. */
    public static double[][] surface(int position, double[] probs) {
        double[][] s = new double[10][10];
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) s[p][n] = p == position - 1 ? probs[n] : 0.1;
        return s;
    }

    /** 1,200 action probabilities in canonical order (position-major, combinations in lexicographic order). */
    public static double[] actions(double[][] s) {
        double[] a = new double[1200]; int k = 0;
        for (int p = 0; p < 10; p++) for (int i = 0; i < 10; i++) for (int j = i + 1; j < 10; j++) for (int l = j + 1; l < 10; l++) a[k++] = s[p][i] + s[p][j] + s[p][l];
        return a;
    }

    /** Max absolute deviation of a surface/action pair from the rules (0 when consistent). */
    public static double error(double[][] s, double[] a) {
        double e = 0;
        for (int p = 0; p < 10; p++) { double sum = 0; for (int n = 0; n < 10; n++) { if (!(s[p][n] >= 0)) return Double.POSITIVE_INFINITY; sum += s[p][n]; } e = Math.max(e, Math.abs(sum - 1)); }
        double[] expect = actions(s);
        if (a.length != 1200) return Double.POSITIVE_INFINITY;
        for (int i = 0; i < 1200; i++) e = Math.max(e, Math.abs(expect[i] - a[i]));
        // each position's 120 actions sum to C(9,2)=36 times the position total (each number is in 36 triples)
        for (int p = 0; p < 10; p++) { double sum = 0; for (int i = 0; i < 120; i++) sum += a[p * 120 + i]; e = Math.max(e, Math.abs(sum - 36)); }
        return e;
    }

    public static int[] rank3(double[][] s, int position) {
        int[] o = new int[10]; ReplayEvaluatorR27.top3(s[position - 1], o); return new int[] {o[0] + 1, o[1] + 1, o[2] + 1};
    }
}
