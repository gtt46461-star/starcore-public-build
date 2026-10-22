package com.ethk3.novaonepro;

import java.util.List;

/**
 * Bounded point-in-time motif successor expert. It scans only prior sequences and never includes
 * the target draw.
 */
public final class MotifNumberExpert implements V50NumberExpert {
    private static final int MOTIF_LENGTH = 6;
    private static final int MAX_LOOKBACK = 720;

    public String id() {
        return "GOLDEN_WINDOW_MOTIF";
    }

    public V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> history,
            DbStore store) {
        double[][] probability = V50ExpertSupport.emptyMatrix();
        double[] trust = new double[10];
        int size = history == null ? 0 : history.size();
        for (int p = 1; p <= 10; p++) {
            int[] weighted = new int[10];
            int totalWeight = 0;
            int start = Math.max(MOTIF_LENGTH, size - MAX_LOOKBACK);
            for (int successor = start; successor < size; successor++) {
                int distance = 0;
                for (int k = 0; k < MOTIF_LENGTH; k++) {
                    int historic =
                            V50ExpertSupport.historyNumber(
                                    history, successor - MOTIF_LENGTH + k, p);
                    int recent =
                            V50ExpertSupport.historyNumber(history, size - MOTIF_LENGTH + k, p);
                    if (historic != recent) distance++;
                }
                if (distance > 2) continue;
                int next = V50ExpertSupport.historyNumber(history, successor, p);
                if (next < 1 || next > 10) continue;
                int weight = distance == 0 ? 5 : (distance == 1 ? 3 : 1);
                weighted[next - 1] += weight;
                totalWeight += weight;
            }
            for (int n = 0; n < 10; n++) {
                probability[p - 1][n] = (weighted[n] + 0.5d) / (totalWeight + 5.0d);
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] = V50ExpertSupport.clamp01(Math.min(0.74d, totalWeight / 75.0d));
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("PIT_MOTIF_SUCCESSOR_DISTANCE_LE_2"),
                V50ExpertSupport.text("MOTIF_DISTANCE_OR_SAMPLE_WEAK"),
                "V50_MOTIF_PIT_V1");
    }
}
