package com.ethk3.novaonepro;

/** Typed pre-draw counter-evidence owner. */
public final class V50CounterEvidenceEngine {
    public static final class Result {
        public final V50FalsifierExpert.Assessment assessment;
        public final String maximumCounterEvidence;
        public final String falsifierCondition;
        public final String checksum;

        Result(
                V50FalsifierExpert.Assessment assessment,
                String maximumCounterEvidence,
                String falsifierCondition,
                String checksum) {
            this.assessment = assessment;
            this.maximumCounterEvidence =
                    maximumCounterEvidence == null ? "" : maximumCounterEvidence;
            this.falsifierCondition = falsifierCondition == null ? "" : falsifierCondition;
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    private final V50FalsifierExpert falsifier = new V50FalsifierExpert();

    public Result evaluate(
            V50Models.PositionAction action,
            V50Models.StateTrajectorySnapshot state,
            V50Models.EffectiveConsensus consensus,
            boolean teacherReady,
            int fingerprintRun,
            double fingerprintResidual) {
        V50FalsifierExpert.Assessment assessment =
                falsifier.assess(
                        action,
                        state,
                        consensus,
                        teacherReady,
                        fingerprintRun,
                        fingerprintResidual);
        String condition =
                assessment.primaryRisk == V50FalsifierExpert.Risk.NONE
                        ? "NO_DOMINANT_FALSIFIER"
                        : assessment.primaryRisk.name() + "_MUST_REMAIN_BELOW_0.70";
        return new Result(
                assessment,
                assessment.evidence,
                condition,
                V50ChecksumEngine.sha256(assessment.checksum + '|' + condition));
    }
}
