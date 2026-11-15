package com.ethk3.novaonepro;

/** V51 state-duration expert using learned state posterior and explicit remaining life. */
public final class V51StateDurationNumberExpert implements V51NumberExpert {
    public String id() {
        return "STATE_DURATION_POSTERIOR";
    }

    public V51Models.ExpertEvidence evaluate(
            V51Models.PermutationFeatureSnapshot f,
            V51Models.StateTrajectorySnapshot[] states,
            V51Models.DrawFrame[] h) {
        double[][] raw = new double[10][10];
        for (int p = 0; p < 10; p++) {
            V51Models.StateTrajectorySnapshot state = states[p];
            double persistence = state.remainingLife / (state.remainingLife + 6.0d);
            double lifecycle =
                    state.lifecycle == V51Enums.Lifecycle.GROW
                                    || state.lifecycle == V51Enums.Lifecycle.MATURE
                            ? 1.0d
                            : (state.lifecycle == V51Enums.Lifecycle.BREAK
                                    ? -1.0d
                                    : (state.lifecycle == V51Enums.Lifecycle.RECOVER
                                            ? 0.35d
                                            : 0.0d));
            for (int n = 0; n < 10; n++) {
                double direction =
                        f.numberFeatures[p][n][V51TrajectoryFeatureEngine.IDX_SHORT_LONG];
                raw[p][n] =
                        lifecycle * persistence * direction * 3.0d
                                - state.changePointRisk * Math.abs(direction) * 1.5d;
            }
        }
        return V51ExpertSupport.evidence(
                id(),
                V51StateModelArtifact.ARTIFACT_ID,
                raw,
                0.46d,
                "state duration posterior",
                "break risk reverses persistence");
    }
}
