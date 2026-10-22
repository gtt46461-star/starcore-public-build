package com.ethk3.novaonepro;

/** V51 lag 1..12 number-position relation tensor expert. */
public final class V51RelationGraphNumberExpert implements V51NumberExpert {
    public String id() {
        return "LAGGED_RELATION_TENSOR";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        V51TransitionTensorEngine tensor = new V51TransitionTensorEngine();
        for (int p = 0; p < 10; p++)
            for (int n = 1; n <= 10; n++) raw[p][n - 1] = tensor.score(h, n, p);
        return V51ExpertSupport.evidence(
                id(),
                "V51_RELATION_LAG_1_12_FDR",
                raw,
                0.42d,
                "lagged transition lift",
                "unverified edges are shrunk to zero");
    }
}
