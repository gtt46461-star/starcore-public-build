package com.ethk3.novaonepro;

import java.util.List;

/** Variable-order context tree with automatic backoff to lower orders. */
public final class ContextTreeNumberExpert implements V50NumberExpert {
    public String id() {
        return "VARIABLE_ORDER_CONTEXT_TREE";
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
            int[] count0 = new int[10];
            int[] count1 = new int[10];
            int[] count2 = new int[10];
            int sample0 = 0, sample1 = 0, sample2 = 0;
            int last1 = V50ExpertSupport.historyNumber(history, size - 1, p);
            int last2 = V50ExpertSupport.historyNumber(history, size - 2, p);
            int start = Math.max(2, size - 720);
            for (int i = start; i < size; i++) {
                int next = V50ExpertSupport.historyNumber(history, i, p);
                if (next < 1 || next > 10) continue;
                count0[next - 1]++;
                sample0++;
                int previous1 = V50ExpertSupport.historyNumber(history, i - 1, p);
                if (previous1 == last1) {
                    count1[next - 1]++;
                    sample1++;
                    int previous2 = V50ExpertSupport.historyNumber(history, i - 2, p);
                    if (previous2 == last2) {
                        count2[next - 1]++;
                        sample2++;
                    }
                }
            }
            double weight2 = sample2 / (sample2 + 18.0d);
            double weight1 = (1.0d - weight2) * sample1 / (sample1 + 28.0d);
            double weight0 = 1.0d - weight2 - weight1;
            for (int n = 0; n < 10; n++) {
                double p0 = (count0[n] + 0.5d) / (sample0 + 5.0d);
                double p1 = (count1[n] + 0.5d) / (sample1 + 5.0d);
                double p2 = (count2[n] + 0.5d) / (sample2 + 5.0d);
                probability[p - 1][n] = weight0 * p0 + weight1 * p1 + weight2 * p2;
            }
            FeatureContractRegistry.normalize10(probability[p - 1]);
            trust[p - 1] =
                    V50ExpertSupport.clamp01(
                            0.28d
                                    + Math.min(0.42d, sample1 / 90.0d)
                                    + Math.min(0.18d, sample2 / 70.0d));
        }
        return V50ExpertSupport.evidence(
                id(),
                probability,
                trust,
                V50ExpertSupport.text("CONTEXT_ORDER_0_1_2_BACKOFF"),
                V50ExpertSupport.text("CONTEXT_SAMPLE_SHRINKAGE"),
                "V50_CONTEXT_TREE_V1");
    }
}
