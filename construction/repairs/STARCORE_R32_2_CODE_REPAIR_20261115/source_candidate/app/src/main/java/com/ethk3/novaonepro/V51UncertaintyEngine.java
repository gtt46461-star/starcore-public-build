package com.ethk3.novaonepro;

/** Six independent uncertainty channels. */
public final class V51UncertaintyEngine {
    public static final class Breakdown {
        public final double aleatoric, expert, state, calibration, domain, permutation, total;

        Breakdown(double a, double b, double c, double d, double e, double f) {
            aleatoric = a;
            expert = b;
            state = c;
            calibration = d;
            domain = e;
            permutation = f;
            total =
                    V51Math.clamp(
                            0.22 * a + 0.20 * b + 0.18 * c + 0.14 * d + 0.14 * e + 0.12 * f, 0, 1);
        }
    }

    public Breakdown evaluate(
            double[] probability,
            V51EffectiveConsensusEngine.Consensus consensus,
            V51Models.StateTrajectorySnapshot state,
            V51Models.DomainCertificate domain,
            V51Models.NumberProbabilityMatrix matrix) {
        double aleatoric = V51Math.entropy(probability);
        double expert = 1.0d - consensus.effective;
        double stateU = 1.0d - state.confidence;
        double calibration = V51SelectedCalibrationArtifact.OOS_ECE;
        double domainU =
                domain == null
                        ? 1.0d
                        : V51Math.clamp(1.0d - domain.credibleLow + domain.driftRisk, 0, 1);
        double permutation =
                V51Math.clamp(
                        matrix.maxRowError
                                + matrix.maxColumnError
                                + (matrix.usedFallback ? 1.0d : 0.0d),
                        0,
                        1);
        return new Breakdown(aleatoric, expert, stateU, calibration, domainU, permutation);
    }
}
