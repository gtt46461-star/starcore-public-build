package com.ethk3.novaonepro;

/** Sole action-rank owner consuming the exported grouped pairwise artifact. */
public final class V50ActionRankerEngine {
    public V50Models.PositionAction[] rank(V50Models.PositionAction[] input) {
        if (input == null || input.length != 10) {
            throw new IllegalArgumentException("V50_RANKER_REQUIRES_TEN_ACTIONS");
        }
        double[] score = new double[10];
        for (int i = 0; i < 10; i++) {
            if (input[i] == null
                    || input[i].rankFeatures.length != V50ActionRankerArtifact.FEATURE_COUNT) {
                throw new IllegalArgumentException("V50_RANK_FEATURE_SHAPE_P" + (i + 1));
            }
            double value = V50ActionRankerArtifact.BIAS;
            for (int f = 0; f < V50ActionRankerArtifact.FEATURE_COUNT; f++) {
                double scale = Math.max(1.0e-12d, V50ActionRankerArtifact.SCALE[f]);
                double z = (input[i].rankFeatures[f] - V50ActionRankerArtifact.MEAN[f]) / scale;
                value += z * V50ActionRankerArtifact.WEIGHT[f];
            }
            score[i] =
                    Math.rint(value * V50ActionRankerArtifact.SCORE_SCALE)
                            / V50ActionRankerArtifact.SCORE_SCALE;
        }
        int[] order = order(score);
        int[] rank = new int[10];
        for (int i = 0; i < 10; i++) rank[order[i]] = i + 1;
        V50Models.PositionAction[] out = new V50Models.PositionAction[10];
        for (int i = 0; i < 10; i++) {
            V50Models.PositionAction source = input[i];
            double rankAdjusted = sigmoid(score[i]);
            String checksum =
                    V50ChecksumEngine.sha256(
                            source.checksum
                                    + '|'
                                    + V50ActionRankerArtifact.PARAMETER_SHA256
                                    + '|'
                                    + Double.doubleToLongBits(score[i])
                                    + '|'
                                    + rank[i]);
            out[i] =
                    new V50Models.PositionAction(
                            source.targetIssue,
                            source.position,
                            source.top3,
                            source.rawActionP,
                            source.thirdP,
                            source.thirdMargin,
                            source.entropy,
                            source.effectiveConsensus,
                            source.dynamicDomain,
                            source.lifecycle,
                            source.rankFeatures,
                            score[i],
                            rankAdjusted,
                            rank[i],
                            source.fingerprint,
                            checksum);
        }
        return out;
    }

    public V50Models.PositionAction rankOne(V50Models.PositionAction[] actions) {
        if (actions == null) return null;
        for (int i = 0; i < actions.length; i++) {
            if (actions[i] != null && actions[i].rank == 1) return actions[i];
        }
        return null;
    }

    private static int[] order(double[] score) {
        int[] out = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < out.length; i++) {
            for (int j = i + 1; j < out.length; j++) {
                int left = out[i];
                int right = out[j];
                if (score[right] > score[left] || (score[right] == score[left] && right < left)) {
                    out[i] = right;
                    out[j] = left;
                }
            }
        }
        return out;
    }

    private static double sigmoid(double value) {
        double clipped = Math.max(-30.0d, Math.min(30.0d, value));
        return 1.0d / (1.0d + Math.exp(-clipped));
    }
}
