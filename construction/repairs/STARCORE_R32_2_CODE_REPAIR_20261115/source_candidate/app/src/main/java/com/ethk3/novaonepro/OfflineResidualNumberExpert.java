package com.ethk3.novaonepro;

import java.util.List;

/** Exact Java serving implementation of the exported 24-field number artifact. */
public final class OfflineResidualNumberExpert implements V50NumberExpert {
    public String id() {
        return "OFFLINE_EXPORTED_NUMBER_MODEL";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        for (int p = 1; p <= 10; p++) {
            double max = -Double.MAX_VALUE;
            double[] logits = new double[10];
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                double score =
                        V50NumberModelArtifact.NUMBER_BIAS[n - 1]
                                + V50NumberModelArtifact.POSITION_NUMBER_BIAS[p - 1][n - 1];
                if (row != null
                        && row.modelFeatures.length == V50NumberModelArtifact.FEATURE_COUNT) {
                    for (int f = 0; f < V50NumberModelArtifact.FEATURE_COUNT; f++) {
                        double scale = Math.max(1.0e-12d, V50NumberModelArtifact.SCALE[f]);
                        double z = (row.modelFeatures[f] - V50NumberModelArtifact.MEAN[f]) / scale;
                        score += z * V50NumberModelArtifact.WEIGHT[f];
                    }
                }
                logits[n - 1] = score;
                max = Math.max(max, score);
            }
            double sum = 0.0d;
            for (int n = 0; n < 10; n++) {
                probability[p - 1][n] =
                        Math.exp(Math.max(-40.0d, Math.min(40.0d, logits[n] - max)));
                sum += probability[p - 1][n];
            }
            if (sum > 0.0d) {
                for (int n = 0; n < 10; n++) probability[p - 1][n] /= sum;
            }
            trust[p - 1] = 0.78d;
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("EXPORTED_24_FIELD_PIT_ARTIFACT"),
                V50ExpertSupport.text("STRICT_OOS_NUMBER_RATE_BELOW_RELEASE_GATE"),
                V50NumberModelArtifact.MODEL_ID + ':' + V50NumberModelArtifact.PARAMETER_SHA256);
    }
}
