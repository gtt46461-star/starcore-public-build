package com.ethk3.novaonepro;

/** Predicts failure risk only; it never proposes numbers. */
public final class V51FalsifierExpert {
    public double score(
            V51Models.StateTrajectorySnapshot state,
            double expertDisagreement,
            double rankInstability,
            double thirdBoundaryRisk,
            double motifDistanceRisk,
            double teacherMisleadRisk) {
        double logit =
                -2.0d
                        + 2.6d * state.changePointRisk
                        + 1.8d * expertDisagreement
                        + 1.7d * rankInstability
                        + 1.4d * thirdBoundaryRisk
                        + 1.1d * motifDistanceRisk
                        + 1.2d * teacherMisleadRisk
                        - 0.9d * state.confidence;
        return V51Math.sigmoid(logit);
    }
}
