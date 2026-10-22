package com.ethk3.novaonepro;

/** Data-driven multi-window posterior; no window is declared globally optimal. */
public final class V51WindowPosteriorEngine {
    public static final class Posterior {
        public final int bestWindow, parentWindow;
        public final double shortLift, stability, drift, credibleWeight;
        public final V51Enums.WindowStatus status;

        Posterior(
                int bestWindow,
                int parentWindow,
                double shortLift,
                double stability,
                double drift,
                double credibleWeight,
                V51Enums.WindowStatus status) {
            this.bestWindow = bestWindow;
            this.parentWindow = parentWindow;
            this.shortLift = shortLift;
            this.stability = stability;
            this.drift = drift;
            this.credibleWeight = credibleWeight;
            this.status = status;
        }
    }

    public Posterior evaluate(double[] feature) {
        int[] w = V51TrajectoryFeatureEngine.WINDOWS;
        int best = 0;
        double bestQ = -1, mean = 0;
        for (int i = 0; i < w.length; i++)
            mean += feature[V51TrajectoryFeatureEngine.IDX_FREQ_START + i];
        mean /= w.length;
        for (int i = 0; i < w.length; i++) {
            double p = feature[V51TrajectoryFeatureEngine.IDX_FREQ_START + i];
            double parent =
                    feature[
                            V51TrajectoryFeatureEngine.IDX_FREQ_START
                                    + Math.min(w.length - 1, i + 2)];
            double lift = p - parent,
                    variance = Math.abs(p - mean),
                    sample = Math.min(1.0d, w[i] / 36.0d);
            double q = Math.abs(lift) * (0.45d + 0.55d * sample) / (0.05d + variance);
            if (q > bestQ) {
                bestQ = q;
                best = i;
            }
        }
        int parent = Math.min(w.length - 1, best + 2);
        double p = feature[V51TrajectoryFeatureEngine.IDX_FREQ_START + best];
        double pp = feature[V51TrajectoryFeatureEngine.IDX_FREQ_START + parent];
        double lift = p - pp;
        double drift =
                Math.min(1.0d, Math.abs(feature[V51TrajectoryFeatureEngine.IDX_SHORT_LONG]) * 4.0d);
        double stability =
                V51Math.clamp(
                        1.0d - drift - feature[V51TrajectoryFeatureEngine.IDX_REVERSAL] * 0.35d,
                        0,
                        1);
        double weight = V51Math.clamp((0.25d + Math.min(0.75d, w[best] / 96.0d)) * stability, 0, 1);
        V51Enums.WindowStatus status =
                weight >= 0.72d
                        ? V51Enums.WindowStatus.READY
                        : (weight >= 0.45d
                                ? V51Enums.WindowStatus.WATCH
                                : V51Enums.WindowStatus.DISCOVERED);
        if (drift > 0.75d) status = V51Enums.WindowStatus.DECAYING;
        return new Posterior(w[best], w[parent], lift, stability, drift, weight, status);
    }
}
