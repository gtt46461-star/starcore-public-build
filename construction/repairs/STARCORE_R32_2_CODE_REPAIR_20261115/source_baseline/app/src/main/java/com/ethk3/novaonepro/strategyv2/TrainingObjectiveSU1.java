package com.ethk3.novaonepro.strategyv2;

/** Coefficients are fixed before fitting; resource cost is constant for a fixed architecture. */
public final class TrainingObjectiveSU1 {
    public final double brier, smoothness, l2, resource;
    public final String hash;
    public TrainingObjectiveSU1(double brier, double smoothness, double l2, double resource) {
        for (double v : new double[]{brier, smoothness, l2, resource})
            if (!Double.isFinite(v) || v < 0 || v > 1) throw new IllegalArgumentException("SU1_OBJECTIVE_COEFFICIENT");
        this.brier = brier; this.smoothness = smoothness; this.l2 = l2; this.resource = resource;
        hash = HashingSU1.fields("SU1_JOINT_LOSS_V2|CE_DRAW_MEAN|BRIER_MULTICLASS_DRAW_MEAN|SMOOTH_LAYER_MEAN|L2_PARAM_MEAN|RESOURCE_PARAM_COUNT",
                Double.toHexString(brier), Double.toHexString(smoothness), Double.toHexString(l2), Double.toHexString(resource));
    }
    public static TrainingObjectiveSU1 crossEntropy() { return new TrainingObjectiveSU1(0, 0, 0, 0); }
    public static TrainingObjectiveSU1 research() { return new TrainingObjectiveSU1(.05, .001, .00001, .000001); }
}
