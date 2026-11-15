package com.ethk3.novaonepro;

import java.util.List;

/** Number evidence conditioned on reconstructed duration and lifecycle. */
public final class StateDurationNumberExpert implements V50NumberExpert {
    public String id() {
        return "EXPLICIT_DURATION_STATE";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        for (int p = 1; p <= 10; p++) {
            V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, p);
            double continuation = state == null ? 0.0d : (1.0d - state.hazard) * state.confidence;
            double reversal = state == null ? 1.0d : state.hazard;
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                if (row == null || row.windowFrequency.length < 7) continue;
                double continuationP =
                        0.55d * row.windowFrequency[1] + 0.45d * row.windowFrequency[3];
                double reversalP =
                        0.65d * row.windowFrequency[6]
                                + 0.35d * (0.2d - Math.min(0.2d, row.windowFrequency[0]));
                probability[p - 1][n - 1] =
                        0.1d + continuation * continuationP + reversal * reversalP;
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] =
                    state == null
                            ? 0.0d
                            : V50ExpertSupport.clamp01(
                                    0.24d
                                            + 0.58d * state.confidence
                                            - 0.26d * state.transitionRisk);
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("REAL_STATE_RUN_AND_HAZARD"),
                V50ExpertSupport.text("STATE_TRANSITION_RISK"),
                "V50_DURATION_STATE_V1");
    }
}
