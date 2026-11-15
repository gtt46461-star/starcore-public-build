package com.ethk3.novaonepro;

/** Freezes support, conflict, alternative and falsifier hypotheses pre-draw. */
public final class V50HypothesisGraphEngine {
    public static final class Result {
        public final String primary;
        public final String secondary;
        public final String tertiary;
        public final String alternative;
        public final String necessaryCondition;
        public final String falsifierCondition;
        public final String checksum;

        Result(
                String primary,
                String secondary,
                String tertiary,
                String alternative,
                String necessaryCondition,
                String falsifierCondition,
                String checksum) {
            this.primary = safe(primary);
            this.secondary = safe(secondary);
            this.tertiary = safe(tertiary);
            this.alternative = safe(alternative);
            this.necessaryCondition = safe(necessaryCondition);
            this.falsifierCondition = safe(falsifierCondition);
            this.checksum = safe(checksum);
        }
    }

    public Result build(
            V50Models.PositionAction action,
            V50Models.StateTrajectorySnapshot state,
            V50Models.EffectiveConsensus consensus,
            boolean teacherReady) {
        String primary = action == null ? "NO_ACTION" : "RANK1_EXPORTED_ACTION_MODEL";
        String secondary =
                state == null || state.prototypes.length == 0
                        ? "STATE_EVIDENCE_MISSING"
                        : "STATE_PROTOTYPE:" + state.prototypes[0].prototypeId;
        String tertiary =
                consensus == null
                        ? "CONSENSUS_MISSING"
                        : "EFFECTIVE_CONSENSUS:" + round(consensus.effectiveConsensus);
        String alternative =
                teacherReady ? "TEACHER_TRANSFERABLE_ALTERNATIVE" : "TEACHER_NO_PROVEN_SAMPLE";
        String necessary =
                action == null
                        ? "VALID_FIXED_K3_REQUIRED"
                        : "RANK1_STABLE_AND_THIRD_MARGIN_GE_0.002";
        String falsifier =
                state != null && state.transitionRisk > 0.60d
                        ? "REGIME_SWITCH_HIGH"
                        : "TOP3_BOUNDARY_OR_STATE_SWITCH";
        String checksum =
                V50ChecksumEngine.sha256(
                        primary
                                + '|'
                                + secondary
                                + '|'
                                + tertiary
                                + '|'
                                + alternative
                                + '|'
                                + necessary
                                + '|'
                                + falsifier);
        return new Result(
                primary, secondary, tertiary, alternative, necessary, falsifier, checksum);
    }

    private static String round(double value) {
        return String.valueOf(Math.round(value * 10000.0d) / 10000.0d);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
