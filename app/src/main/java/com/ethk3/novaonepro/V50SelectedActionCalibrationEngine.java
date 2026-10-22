package com.ethk3.novaonepro;

/** Selection-conditioned isotonic calibration; accepts rank-one raw score only. */
public final class V50SelectedActionCalibrationEngine {
    public double calibrate(V50Models.PositionAction rankOne) {
        if (rankOne == null || rankOne.rank != 1) {
            throw new IllegalArgumentException("V50_CALIBRATION_REQUIRES_RANK1");
        }
        double value = rankOne.rankScore;
        double[] x = V50SelectedCalibrationArtifact.X;
        double[] y = V50SelectedCalibrationArtifact.Y;
        if (x.length == 0 || x.length != y.length) return 0.0d;
        if (value <= x[0]) return y[0];
        if (value >= x[x.length - 1]) return y[y.length - 1];
        for (int i = 1; i < x.length; i++) {
            if (value > x[i]) continue;
            double width = x[i] - x[i - 1];
            if (Math.abs(width) <= 1.0e-15d) return y[i];
            double ratio = (value - x[i - 1]) / width;
            return FeatureContractRegistry.clamp01(y[i - 1] + ratio * (y[i] - y[i - 1]));
        }
        return y[y.length - 1];
    }
}
