package com.ethk3.novaonepro;

/** Separates aleatoric, expert, state, calibration and domain uncertainty. */
public final class V50UncertaintyEngine {
    public V50Models.UncertaintyBreakdown evaluate(
            V50Models.PositionAction rankOne,
            V50Models.EffectiveConsensus consensus,
            V50Models.StateTrajectorySnapshot state,
            V50Models.DomainCertificate certificate,
            double calibrationOosEce) {
        double aleatoric = rankOne == null ? 1.0d : rankOne.entropy;
        double disagreement = consensus == null ? 1.0d : 1.0d - consensus.distributionAgreement;
        double stateUncertainty =
                state == null
                        ? 1.0d
                        : FeatureContractRegistry.clamp01(
                                0.55d * state.transitionRisk + 0.45d * (1.0d - state.confidence));
        double calibration = FeatureContractRegistry.clamp01(calibrationOosEce / 0.10d);
        double domain =
                certificate == null
                        ? 1.0d
                        : FeatureContractRegistry.clamp01(1.0d - certificate.sample / 160.0d);
        double total =
                FeatureContractRegistry.clamp01(
                        0.20d * aleatoric
                                + 0.24d * disagreement
                                + 0.22d * stateUncertainty
                                + 0.18d * calibration
                                + 0.16d * domain);
        return new V50Models.UncertaintyBreakdown(
                aleatoric, disagreement, stateUncertainty, calibration, domain, total);
    }
}
