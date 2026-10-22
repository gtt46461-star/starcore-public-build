package com.ethk3.novaonepro;

import java.util.List;

/** Long-horizon, position-aware Dirichlet prior with small-sample shrinkage. */
public final class HierarchicalPriorNumberExpert implements V50NumberExpert {
    public String id() {
        return "HIERARCHICAL_DIRICHLET_PRIOR";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        for (int p = 1; p <= 10; p++) {
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                if (row == null || row.windowFrequency.length < 9) continue;
                probability[p - 1][n - 1] =
                        0.15d * 0.1d
                                + 0.25d * row.windowFrequency[5]
                                + 0.25d * row.windowFrequency[6]
                                + 0.35d * row.windowFrequency[8];
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] =
                    Math.min(
                            0.86d,
                            0.44d
                                    + Math.min(
                                                    1.0d,
                                                    (history == null ? 0 : history.size()) / 720.0d)
                                            * 0.32d);
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("LONG_HORIZON_POSITION_PRIOR"),
                V50ExpertSupport.text("PRIOR_CAN_LAG_REGIME_SHIFT"),
                "V50_PRIOR_ANALYTIC_V1");
    }
}
