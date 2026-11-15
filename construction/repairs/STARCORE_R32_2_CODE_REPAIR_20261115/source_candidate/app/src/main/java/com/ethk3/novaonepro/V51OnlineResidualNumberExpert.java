package com.ethk3.novaonepro;

/** V51 bounded online FTRL residual; shadow-first and unable to replace offline owner. */
public final class V51OnlineResidualNumberExpert implements V51NumberExpert {
    private final double[] z = new double[V51TrajectoryFeatureEngine.FEATURE_COUNT];
    private final double[] n = new double[V51TrajectoryFeatureEngine.FEATURE_COUNT];
    private int updates;

    public String id() {
        return "BOUNDED_ONLINE_FTRL";
    }

    public synchronized V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[] w = weights();
        double[][] raw = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int number = 0; number < 10; number++) {
                double score = 0;
                double[] x = f.numberFeatures[p][number];
                for (int i = 0; i < x.length && i < w.length; i++) score += w[i] * x[i];
                raw[p][number] = V51Math.clamp(score, -0.35d, 0.35d);
            }
        double trust = updates < 300 ? 0.0d : Math.min(0.25d, updates / 5000.0d);
        return V51ExpertSupport.evidence(
                id(),
                "V51_FTRL_RUNTIME",
                raw,
                trust,
                "bounded online correction",
                "warmup and drift reset keep real weight zero initially");
    }

    public synchronized void update(double[] x, int label, double predicted) {
        if (x == null) return;
        double g = V51Math.clamp(predicted, 0.001d, 0.999d) - (label > 0 ? 1.0d : 0.0d);
        double[] w = weights();
        for (int i = 0; i < x.length && i < z.length; i++) {
            double gi = g * V51Math.clamp(x[i], -5, 5);
            double sigma = (Math.sqrt(n[i] + gi * gi) - Math.sqrt(n[i])) / 0.08d;
            z[i] += gi - sigma * w[i];
            n[i] += gi * gi;
        }
        updates++;
        if (updates % 2000 == 0)
            for (int i = 0; i < z.length; i++) {
                z[i] *= 0.92d;
                n[i] *= 0.92d;
            }
    }

    private double[] weights() {
        double[] w = new double[z.length];
        for (int i = 0; i < z.length; i++) {
            double sign = z[i] < 0 ? -1 : 1;
            if (Math.abs(z[i]) <= 1.0d) w[i] = 0;
            else w[i] = -(z[i] - sign) / (0.1d + (Math.sqrt(n[i]) / 0.08d));
            w[i] = V51Math.clamp(w[i], -0.25d, 0.25d);
        }
        return w;
    }
}
