package com.ethk3.novaonepro;

/** V51 multi-length matrix-profile successor expert. */
public final class V51MotifNumberExpert implements V51NumberExpert {
    public String id() {
        return "MULTILENGTH_MATRIX_PROFILE";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        V51MotifMatrixProfileEngine engine = new V51MotifMatrixProfileEngine();
        double[][] surface = engine.scoreSurface(h);
        for (int p = 0; p < 10; p++)
            for (int n = 1; n <= 10; n++) raw[p][n - 1] = surface[p][n - 1] * 4.0d;
        return V51ExpertSupport.evidence(
                id(),
                "V51_MATRIX_PROFILE_6_12_24_48",
                raw,
                0.43d,
                "nearest motif successor",
                "distance and exclusion zone limit false neighbors");
    }
}
