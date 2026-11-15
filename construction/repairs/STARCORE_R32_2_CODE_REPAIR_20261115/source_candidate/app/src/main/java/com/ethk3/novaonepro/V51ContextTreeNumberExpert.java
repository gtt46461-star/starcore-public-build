package com.ethk3.novaonepro;

/** V51 variable-order Bayesian context tree, orders 0..6 with MDL shrinkage. */
public final class V51ContextTreeNumberExpert implements V51NumberExpert {
    public String id() {
        return "VARIABLE_ORDER_CONTEXT_TREE";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        int total = h == null ? 0 : h.length;
        for (int p = 0; p < 10; p++)
            for (int candidate = 1; candidate <= 10; candidate++) {
                double weighted = 0, weight = 0;
                for (int order = 0; order <= 6; order++) {
                    int matches = 0, hits = 0;
                    if (order == 0) {
                        matches = Math.max(0, total - 1);
                        for (int t = 1; t < total; t++)
                            if (h[t].permutation[p] == candidate) hits++;
                    } else if (total > order) {
                        for (int t = order; t < total; t++) {
                            boolean same = true;
                            for (int k = 1; k <= order; k++)
                                if (h[t - k].permutation[p] != h[total - k].permutation[p]) {
                                    same = false;
                                    break;
                                }
                            if (same) {
                                matches++;
                                if (h[t].permutation[p] == candidate) hits++;
                            }
                        }
                    }
                    double posterior = (hits + 0.5d) / (matches + 5.0d);
                    double complexity = Math.exp(-0.55d * order);
                    double support = matches / (matches + 18.0d);
                    double w = complexity * support;
                    weighted += w * V51ExpertSupport.logFrequency(posterior);
                    weight += w;
                }
                raw[p][candidate - 1] = weight == 0 ? Math.log(0.1d) : weighted / weight;
            }
        return V51ExpertSupport.evidence(
                id(),
                "V51_CTW_0_6",
                raw,
                0.52d,
                "Bayesian context suffixes",
                "MDL penalty prevents rare high-order dominance");
    }
}
