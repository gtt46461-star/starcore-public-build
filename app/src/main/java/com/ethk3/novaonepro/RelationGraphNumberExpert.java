package com.ethk3.novaonepro;

import java.util.List;

/** Uses only relation lifts accepted by V50RelationGraphVerifier. */
public final class RelationGraphNumberExpert implements V50NumberExpert {
    public String id() {
        return "VERIFIED_POSITION_RELATION_GRAPH";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        for (int p = 1; p <= 10; p++) {
            double evidence = 0.0d;
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                if (row == null || row.windowFrequency.length < 7) continue;
                double lift = Math.max(-0.45d, Math.min(0.75d, row.relationScore));
                probability[p - 1][n - 1] =
                        Math.max(1.0e-6d, row.windowFrequency[6]) * Math.exp(lift);
                evidence += Math.abs(lift);
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] = V50ExpertSupport.clamp01(Math.min(0.76d, evidence / 3.0d));
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("STABLE_DIRECTION_PERMUTATION_VERIFIED_EDGE"),
                V50ExpertSupport.text("UNVERIFIED_RELATION_EDGE_ZERO_WEIGHT"),
                "V50_RELATION_GRAPH_VERIFIED_V1");
    }
}
