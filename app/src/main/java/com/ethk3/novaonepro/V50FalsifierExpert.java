package com.ethk3.novaonepro;

/** Counter-evidence expert; deliberately emits no number prediction. */
public final class V50FalsifierExpert {
    public enum Risk {
        NONE,
        REGIME_SWITCH,
        FALSE_CONSENSUS,
        TEACHER_UNPROVEN,
        THIRD_MARGIN_COLLAPSE,
        POSITION_RANK_UNSTABLE,
        ACTION_STALE
    }

    public static final class Assessment {
        public final Risk primaryRisk;
        public final double score;
        public final String evidence;
        public final String checksum;

        Assessment(Risk primaryRisk, double score, String evidence, String checksum) {
            this.primaryRisk = primaryRisk == null ? Risk.NONE : primaryRisk;
            this.score = FeatureContractRegistry.clamp01(score);
            this.evidence = evidence == null ? "" : evidence;
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    public Assessment assess(
            V50Models.PositionAction action,
            V50Models.StateTrajectorySnapshot state,
            V50Models.EffectiveConsensus consensus,
            boolean teacherReady,
            int fingerprintRun,
            double fingerprintResidual) {
        Risk risk = Risk.NONE;
        double score = 0.0d;
        if (state != null && state.transitionRisk > score) {
            score = state.transitionRisk;
            risk = Risk.REGIME_SWITCH;
        }
        if (consensus != null) {
            double falseConsensus =
                    consensus.distributionAgreement * (1.0d - consensus.signalStrength);
            if (falseConsensus > score) {
                score = falseConsensus;
                risk = Risk.FALSE_CONSENSUS;
            }
        }
        if (!teacherReady && 0.52d > score) {
            score = 0.52d;
            risk = Risk.TEACHER_UNPROVEN;
        }
        if (action != null) {
            double marginRisk = 1.0d - Math.min(1.0d, Math.max(0.0d, action.thirdMargin) / 0.025d);
            if (marginRisk > score) {
                score = marginRisk;
                risk = Risk.THIRD_MARGIN_COLLAPSE;
            }
        }
        double stale =
                fingerprintRun < 3
                        ? 0.0d
                        : FeatureContractRegistry.clamp01(
                                (fingerprintRun - 2) / 5.0d + Math.max(0.0d, -fingerprintResidual));
        if (stale > score) {
            score = stale;
            risk = Risk.ACTION_STALE;
        }
        String evidence = risk.name() + "|score=" + score;
        return new Assessment(risk, score, evidence, V50ChecksumEngine.sha256(evidence));
    }
}
