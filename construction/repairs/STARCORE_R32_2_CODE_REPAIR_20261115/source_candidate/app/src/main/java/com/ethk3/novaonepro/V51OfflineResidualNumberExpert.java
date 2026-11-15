package com.ethk3.novaonepro;

/** V51 distilled offline residual; unproven seed artifact has zero live trust. */
public final class V51OfflineResidualNumberExpert implements V51NumberExpert {
    public String id() {
        return "OFFLINE_RESIDUAL";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] s,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        for (int p = 0; p < 10; p++)
            for (int n = 0; n < 10; n++) {
                double z = V51OfflineResidualArtifact.POSITION_NUMBER_BIAS[p][n];
                double[] x = f.numberFeatures[p][n];
                for (int i = 0; i < x.length && i < V51OfflineResidualArtifact.WEIGHT.length; i++)
                    z += x[i] * V51OfflineResidualArtifact.WEIGHT[i];
                raw[p][n] = z;
            }
        return V51ExpertSupport.evidence(
                id(),
                V51OfflineResidualArtifact.ARTIFACT_ID,
                raw,
                V51OfflineResidualArtifact.PROVEN ? 0.70d : 0.0d,
                "distilled tree/logistic residual",
                "unproven artifact weight is zero");
    }
}
