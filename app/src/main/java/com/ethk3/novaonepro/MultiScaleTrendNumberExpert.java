package com.ethk3.novaonepro;

import java.util.List;

/** Multi-scale frequency, omission, release and exhaustion evidence. */
public final class MultiScaleTrendNumberExpert implements V50NumberExpert {
    public String id() {
        return "MULTISCALE_TREND_GAP";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        for (int p = 1; p <= 10; p++) {
            double stateRisk = stateRisk(states, p);
            for (int n = 1; n <= 10; n++) {
                V50Models.NumberFeatureRow row = V50ExpertSupport.row(features, p, n);
                if (row == null || row.windowFrequency.length < 5) continue;
                double score =
                        0.45d * row.windowFrequency[0]
                                + 0.30d * row.windowFrequency[2]
                                + 0.25d * row.windowFrequency[4]
                                + 0.12d * row.slope
                                + 0.08d * row.acceleration
                                + 0.035d * row.gapHazard
                                - 0.025d * row.releaseFailureRate
                                - 0.02d * row.volatility;
                probability[p - 1][n - 1] = Math.exp(Math.max(-8.0d, Math.min(8.0d, score * 7.0d)));
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] = V50ExpertSupport.clamp01(0.68d - 0.35d * stateRisk);
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("W6_W24_W96_TREND_GAP"),
                V50ExpertSupport.text("TREND_EXHAUSTION_OR_RELEASE_FAILURE"),
                "V50_TREND_ANALYTIC_V1");
    }

    private static double stateRisk(V50Models.StateTrajectorySnapshot[] states, int position) {
        V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, position);
        return state == null ? 1.0d : state.transitionRisk;
    }
}
