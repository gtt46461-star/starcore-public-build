package com.ethk3.novaonepro;

/** Exact 20-field grouped action feature contract used by the exported ranker. */
public final class V50ActionMetaFeatureEngine {
    public double[][] build(
            double[][] numberProbability, int[][] top4, V50Models.NumberFeatureSnapshot features) {
        if (numberProbability == null
                || numberProbability.length != 10
                || top4 == null
                || top4.length != 10) {
            throw new IllegalArgumentException("V50_ACTION_META_SHAPE_INVALID");
        }
        double[] raw = new double[10];
        double mean = 0.0d;
        for (int p = 0; p < 10; p++) {
            raw[p] =
                    numberProbability[p][top4[p][0]]
                            + numberProbability[p][top4[p][1]]
                            + numberProbability[p][top4[p][2]];
            mean += raw[p];
        }
        mean /= 10.0d;
        double variance = 0.0d;
        for (int p = 0; p < 10; p++) {
            double delta = raw[p] - mean;
            variance += delta * delta;
        }
        double std = Math.max(1.0e-6d, Math.sqrt(variance / 10.0d));
        double second = secondLargest(raw);
        double[][] out = new double[10][V50ActionRankerArtifact.FEATURE_COUNT];
        for (int p = 0; p < 10; p++) {
            double[] probability = numberProbability[p];
            double top1 = probability[top4[p][0]];
            double top2 = probability[top4[p][1]];
            double third = probability[top4[p][2]];
            double fourth = probability[top4[p][3]];
            double entropy = FeatureContractRegistry.entropy10(probability);
            double slope = 0.0d;
            double acceleration = 0.0d;
            double gapPercentile = 0.0d;
            double volatility = 0.0d;
            for (int k = 0; k < 3; k++) {
                V50Models.NumberFeatureRow row =
                        V50ExpertSupport.row(features, p + 1, top4[p][k] + 1);
                if (row == null) continue;
                slope += row.slope / 3.0d;
                acceleration += row.acceleration / 3.0d;
                gapPercentile += row.gapPercentile / 3.0d;
                volatility += row.volatility / 3.0d;
            }
            V50Models.NumberFeatureRow representative = V50ExpertSupport.row(features, p + 1, 1);
            double switchRisk =
                    representative == null || representative.modelFeatures.length <= 22
                            ? 1.0d
                            : representative.modelFeatures[22];
            double familyStrength =
                    representative == null || representative.modelFeatures.length <= 23
                            ? 0.0d
                            : representative.modelFeatures[23];
            double signal =
                    FeatureContractRegistry.clamp01(
                            (1.0d - entropy) * 1.75d + (raw[p] - 0.30d) * 1.8d);
            out[p] =
                    new double[] {
                        raw[p],
                        top1,
                        top2,
                        third,
                        fourth,
                        third - fourth,
                        top1 - top2,
                        entropy,
                        signal,
                        p / 9.0d,
                        slope,
                        acceleration,
                        gapPercentile,
                        volatility,
                        switchRisk,
                        familyStrength,
                        raw[p] - mean,
                        raw[p] - second,
                        (raw[p] - mean) / std,
                        entropy
                    };
        }
        return out;
    }

    private static double secondLargest(double[] values) {
        double first = -Double.MAX_VALUE;
        double second = -Double.MAX_VALUE;
        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (value > first) {
                second = first;
                first = value;
            } else if (value > second) {
                second = value;
            }
        }
        return second;
    }
}
