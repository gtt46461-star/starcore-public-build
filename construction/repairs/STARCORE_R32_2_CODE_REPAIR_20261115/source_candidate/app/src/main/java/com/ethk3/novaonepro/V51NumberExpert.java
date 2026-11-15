package com.ethk3.novaonepro;

/** Experts emit raw scores and evidence only. They never own final NumberP or BET. */
public interface V51NumberExpert {
    String id();

    V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.DrawFrame[] history);
}
