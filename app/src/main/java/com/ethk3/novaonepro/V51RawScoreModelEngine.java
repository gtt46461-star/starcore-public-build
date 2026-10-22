package com.ethk3.novaonepro;

/** Weighted raw-score owner before the permutation projection. */
public final class V51RawScoreModelEngine {
    public double[][] fuse(V51Models.ExpertEvidence[] experts, double[][] weights) {
        double[][] raw = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                double value = 0;
                for (int e = 0; e < experts.length; e++)
                    value += weights[p][e] * experts[e].rawScore[p][n];
                raw[p][n] = V51Math.clamp(value, -30, 30);
            }
        return raw;
    }
}
