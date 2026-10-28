package com.ethk3.novaonepro.strategyv2;

/** R27-036 linear adaptive learner (docs/06 6.5): logistic ridge pre-training on H_TRAIN through the existing
 * LogisticRidgeSU2 Newton solver, then FTRL-proximal online updates initialised from those weights; scored with the
 * same Brier-skill charts as AtomNet (r27_brier_pair, model 'FTRL'). */
public final class FtrlR27 {
    public final int d; final double[] z, n, w; final double alpha, beta, l1, l2; public long updates;
    public FtrlR27(int d, double alpha, double beta, double l1, double l2) { this.d = d; z = new double[d + 1]; n = new double[d + 1]; w = new double[d + 1]; this.alpha = alpha; this.beta = beta; this.l1 = l1; this.l2 = l2; }
    public static FtrlR27 pretrain(double[][] x, int[] y, int rows) {
        FtrlR27 f = new FtrlR27(x[0].length, 0.05, 1, 1e-4, 1e-3);
        if (rows >= 50) {
            double[] yy = new double[rows]; for (int i = 0; i < rows; i++) yy[i] = y[i];
            double[] w0 = LogisticRidgeSU2.fit(x, yy, rows, 1.0, 25);
            // Warm start: choose z so that the FTRL closed form reproduces w0 with n = 0 (|z| > l1 branch).
            for (int j = 0; j < w0.length && j < f.w.length; j++) { f.w[j] = w0[j]; f.z[j] = -w0[j] * (f.beta / f.alpha + f.l2) - Math.signum(w0[j]) * f.l1; }
        }
        return f;
    }
    public double predict(double[] x) {
        double s = w[0]; for (int j = 0; j < d; j++) s += w[j + 1] * x[j];
        return 1 / (1 + Math.exp(-StatsR27.clamp(s, -30, 30)));
    }
    public void update(double[] x, int y) {
        for (int j = 0; j <= d; j++) {
            double zi = z[j]; w[j] = Math.abs(zi) <= l1 ? 0 : -(zi - Math.signum(zi) * l1) / ((beta + Math.sqrt(n[j])) / alpha + l2);
        }
        double p = predict(x), g0 = p - y;
        for (int j = 0; j <= d; j++) {
            double xi = j == 0 ? 1 : x[j - 1], g = g0 * xi, sigma = (Math.sqrt(n[j] + g * g) - Math.sqrt(n[j])) / alpha;
            z[j] += g - sigma * w[j]; n[j] += g * g;
        }
        updates++;
    }
}
