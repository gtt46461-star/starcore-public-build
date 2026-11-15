package com.ethk3.novaonepro;

/** V51 hazard ingredients: release, continuation, failure and exhaustion evidence. */
public final class V51MultiScaleTrendNumberExpert implements V51NumberExpert {
    public String id() {
        return "MULTISCALE_GAP_HAZARD";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        V51WindowPosteriorEngine windows = new V51WindowPosteriorEngine();
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                double[] x = f.numberFeatures[p][n];
                V51WindowPosteriorEngine.Posterior w = windows.evaluate(x);
                double gap = x[V51TrajectoryFeatureEngine.IDX_GAP],
                        trend = x[V51TrajectoryFeatureEngine.IDX_SHORT_LONG];
                double velocity = x[V51TrajectoryFeatureEngine.IDX_VELOCITY],
                        acc = x[V51TrajectoryFeatureEngine.IDX_ACCELERATION];
                double release = gap * (0.5d + 0.5d * Math.max(0, trend));
                double continuation = Math.max(0, trend) * (1.0d - w.drift);
                double exhaustion = Math.max(0, -acc) * Math.abs(velocity) + w.drift * 0.35d;
                raw[p][n] =
                        1.8d * release
                                + 2.1d * continuation
                                - 1.6d * exhaustion
                                + 0.8d * w.shortLift;
            }
        return V51ExpertSupport.evidence(
                id(),
                "V51_HAZARD_ENGINEERING",
                raw,
                0.58d,
                "multi-window hazard",
                "drift and exhaustion suppress score");
    }
}
