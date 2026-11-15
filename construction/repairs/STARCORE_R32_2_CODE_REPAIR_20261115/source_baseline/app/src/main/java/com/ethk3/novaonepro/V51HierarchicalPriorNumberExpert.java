package com.ethk3.novaonepro;

/** V51 hierarchical permutation prior with automatic short-to-parent shrinkage. */
public final class V51HierarchicalPriorNumberExpert implements V51NumberExpert {
    public String id() {
        return "HIERARCHICAL_PERMUTATION_PRIOR";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                double[] x = f.numberFeatures[p][n];
                double shortP = x[V51TrajectoryFeatureEngine.IDX_FREQ_START + 3];
                double medium = x[V51TrajectoryFeatureEngine.IDX_FREQ_START + 5];
                double parent = x[V51TrajectoryFeatureEngine.IDX_FREQ_START + 9];
                double sample = Math.min(1.0d, (h == null ? 0 : h.length) / 360.0d);
                double pooled =
                        (0.25d + 0.35d * sample) * shortP
                                + 0.25d * medium
                                + (0.50d - 0.35d * sample) * parent;
                raw[p][n] = V51ExpertSupport.logFrequency(pooled);
            }
        return V51ExpertSupport.evidence(
                id(),
                "V51_DIRICHLET_POOLING",
                raw,
                0.72d,
                "hierarchical partial pooling",
                "small cells shrink to parent");
    }
}
