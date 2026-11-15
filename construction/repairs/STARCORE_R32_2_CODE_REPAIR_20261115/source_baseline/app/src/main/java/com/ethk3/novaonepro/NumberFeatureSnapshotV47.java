package com.ethk3.novaonepro;

import java.util.List;

/** Explicit 10x10 current number-state snapshot shared by all 1200 K3 combinations. */
public final class NumberFeatureSnapshotV47 {
    public static final String MODULE_VERSION = "V47_NUMBER_FEATURE_100_STATE_20260721_47072147";
    public final double[][] probability = new double[10][10];
    public final double[][] uncertainty = new double[10][10];
    public int historySize;
    public String baseIssue = "";

    private NumberFeatureSnapshotV47() {}

    public static NumberFeatureSnapshotV47 build(
            List<Models.Draw> history, CrossFittedNumberModelEngine.Snapshot model) {
        NumberFeatureSnapshotV47 s = new NumberFeatureSnapshotV47();
        s.historySize = model == null ? 0 : model.historySize;
        s.baseIssue = model == null ? "" : model.baseIssue;
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                s.probability[p][n] = model == null ? 0.10d : model.probability[p][n];
                s.uncertainty[p][n] = model == null ? 1.0d : (model.champion ? 0.25d : 0.65d);
            }
        return s;
    }
}
