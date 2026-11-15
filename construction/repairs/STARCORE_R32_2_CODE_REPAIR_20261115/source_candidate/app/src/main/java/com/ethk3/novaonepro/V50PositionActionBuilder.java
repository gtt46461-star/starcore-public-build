package com.ethk3.novaonepro;

/** Sole owner that converts ten NumberP rows into ten fixed-K=3 actions. */
public final class V50PositionActionBuilder {
    private final V50ActionMetaFeatureEngine meta = new V50ActionMetaFeatureEngine();

    public V50Models.PositionAction[] build(
            V50Models.PredictionContext context,
            double[][] numberProbability,
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            V50Models.EffectiveConsensus[] consensus) {
        if (context == null || numberProbability == null || numberProbability.length != 10) {
            throw new IllegalArgumentException("V50_ACTION_BUILDER_INPUT_INVALID");
        }
        int[][] top4 = new int[10][4];
        for (int p = 0; p < 10; p++) {
            if (!V50PredictionContract.normalized10(numberProbability[p], 1.0e-8d)) {
                throw new IllegalArgumentException("V50_NUMBER_P_NOT_NORMALIZED_P" + (p + 1));
            }
            top4[p] = order4(numberProbability[p]);
        }
        double[][] rankFeatures = meta.build(numberProbability, top4, features);
        V50Models.PositionAction[] out = new V50Models.PositionAction[10];
        for (int p = 0; p < 10; p++) {
            int[] top3 = new int[] {top4[p][0] + 1, top4[p][1] + 1, top4[p][2] + 1};
            double raw = rankFeatures[p][0];
            double third = rankFeatures[p][3];
            double margin = rankFeatures[p][5];
            double entropy = rankFeatures[p][7];
            V50Models.StateTrajectorySnapshot state = V50ExpertSupport.state(states, p + 1);
            V50Models.EffectiveConsensus consensusValue =
                    consensus != null && p < consensus.length ? consensus[p] : null;
            String domain = state == null ? "" : state.dynamicDomain;
            V50Enums.Lifecycle lifecycle =
                    state == null ? V50Enums.Lifecycle.UNKNOWN : state.lifecycle;
            double effective = consensusValue == null ? 0.0d : consensusValue.effectiveConsensus;
            String fingerprint =
                    V50ChecksumEngine.sha256(
                            "P" + (p + 1) + '|' + top3[0] + ',' + top3[1] + ',' + top3[2]);
            String checksum =
                    checksum(
                            context.targetIssue,
                            p + 1,
                            top3,
                            raw,
                            third,
                            margin,
                            entropy,
                            effective,
                            domain,
                            lifecycle,
                            rankFeatures[p]);
            out[p] =
                    new V50Models.PositionAction(
                            context.targetIssue,
                            p + 1,
                            top3,
                            raw,
                            third,
                            margin,
                            entropy,
                            effective,
                            domain,
                            lifecycle,
                            rankFeatures[p],
                            0.0d,
                            0.0d,
                            0,
                            fingerprint,
                            checksum);
        }
        return out;
    }

    private static int[] order4(double[] probability) {
        int[] index = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                int left = index[i];
                int right = index[j];
                if (probability[right] > probability[left]
                        || (probability[right] == probability[left] && right < left)) {
                    index[i] = right;
                    index[j] = left;
                }
            }
        }
        return new int[] {index[0], index[1], index[2], index[3]};
    }

    private static String checksum(
            String issue,
            int position,
            int[] top3,
            double raw,
            double third,
            double margin,
            double entropy,
            double consensus,
            String domain,
            V50Enums.Lifecycle lifecycle,
            double[] rankFeatures) {
        return V50ChecksumEngine.sha256(
                safe(issue)
                        + '|'
                        + position
                        + '|'
                        + top3[0]
                        + ','
                        + top3[1]
                        + ','
                        + top3[2]
                        + '|'
                        + Double.doubleToLongBits(raw)
                        + '|'
                        + Double.doubleToLongBits(third)
                        + '|'
                        + Double.doubleToLongBits(margin)
                        + '|'
                        + Double.doubleToLongBits(entropy)
                        + '|'
                        + Double.doubleToLongBits(consensus)
                        + '|'
                        + safe(domain)
                        + '|'
                        + lifecycle
                        + '|'
                        + V50ChecksumEngine.hashDoubles("RANK_FEATURE", rankFeatures));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
