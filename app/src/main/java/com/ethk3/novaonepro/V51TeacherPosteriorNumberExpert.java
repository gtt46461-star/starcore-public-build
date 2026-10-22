package com.ethk3.novaonepro;

/** V51 hierarchical teacher posterior; evidence only and never a BET owner. */
public final class V51TeacherPosteriorNumberExpert implements V51NumberExpert {
    public String id() {
        return "TEACHER_POSTERIOR";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        int total = h == null ? 0 : h.length, recent = Math.min(180, total);
        for (int p = 0; p < 10; p++)
            for (int n = 1; n <= 10; n++) {
                int allHit = 0, recentHit = 0;
                for (int t = 0; t < total; t++) {
                    if (h[t].permutation[p] == n) {
                        allHit++;
                        if (t >= total - recent) recentHit++;
                    }
                }
                double global = (allHit + 1.0d) / (total + 10.0d),
                        local = (recentHit + 0.5d) / (recent + 5.0d),
                        transfer = states[p].confidence;
                raw[p][n - 1] =
                        V51ExpertSupport.logFrequency(
                                global * (1.0d - transfer) + local * transfer);
            }
        return V51ExpertSupport.evidence(
                id(),
                "V51_TEACHER_HIERARCHICAL",
                raw,
                total < 240 ? 0.12d : 0.36d,
                "posterior with parent shrinkage",
                "NO_SAMPLE and recent mislead risk reduce trust");
    }
}
