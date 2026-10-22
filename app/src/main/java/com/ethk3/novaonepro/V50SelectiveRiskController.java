package com.ethk3.novaonepro;

/** OOS-artifact policy surface; never rescues coverage by lowering quality. */
public final class V50SelectiveRiskController {
    public static final class Result {
        public final V50Enums.Lane lane;
        public final boolean realEligible;
        public final V50Enums.RejectCode rejectCode;
        public final double threshold;
        public final String detail;

        Result(
                V50Enums.Lane lane,
                boolean realEligible,
                V50Enums.RejectCode rejectCode,
                double threshold,
                String detail) {
            this.lane = lane == null ? V50Enums.Lane.SHADOW : lane;
            this.realEligible = realEligible;
            this.rejectCode = rejectCode == null ? V50Enums.RejectCode.NONE : rejectCode;
            this.threshold = threshold;
            this.detail = detail == null ? "" : detail;
        }
    }

    public Result evaluate(
            double selectedHitP,
            V50Models.PositionAction action,
            V50Models.DomainCertificate domain,
            V50Models.EffectiveConsensus consensus,
            V50Models.UncertaintyBreakdown uncertainty,
            V50FalsifierExpert.Assessment falsifier) {
        double threshold = V50SelectivePolicyArtifact.GLOBAL_SELECTED_P_THRESHOLD;
        if (!V50BuildManifest.MODEL_PROVEN || !V50SelectivePolicyArtifact.MODEL_GATE_PASSED) {
            return shadow(
                    V50Enums.RejectCode.MODEL_ARTIFACT_UNPROVEN,
                    threshold,
                    "STRICT_OOS_MODEL_GATE_FAILED");
        }
        if (domain == null
                || domain.status == V50Enums.DomainStatus.HARD_FREEZE
                || domain.status == V50Enums.DomainStatus.RETIRED) {
            return shadow(
                    V50Enums.RejectCode.DOMAIN_HARD_FREEZE, threshold, "DOMAIN_NOT_ACTIONABLE");
        }
        double tieThreshold = V50SelectivePolicyArtifact.GLOBAL_RANK_SCORE_TIE_THRESHOLD;
        boolean belowProbability = selectedHitP < threshold - 1.0e-12d;
        boolean probabilityTie = Math.abs(selectedHitP - threshold) <= 1.0e-12d;
        boolean belowTieScore = action == null || action.rankScore < tieThreshold;
        if (belowProbability || (probabilityTie && belowTieScore)) {
            return shadow(
                    V50Enums.RejectCode.SELECTED_HIT_P_BELOW_POLICY,
                    threshold,
                    "SELECTED_ACTION_BELOW_P_AND_RANK_TIE_SURFACE");
        }
        if (action == null || action.thirdMargin < 0.002d) {
            return shadow(
                    V50Enums.RejectCode.RANK_MARGIN_WEAK, threshold, "THIRD_FOURTH_MARGIN_WEAK");
        }
        if (consensus == null || consensus.effectiveConsensus < 0.08d) {
            return shadow(
                    V50Enums.RejectCode.EFFECTIVE_CONSENSUS_WEAK,
                    threshold,
                    "EFFECTIVE_CONSENSUS_WEAK_OR_SIGNAL_FREE");
        }
        if (falsifier != null && falsifier.score > 0.70d) {
            return shadow(
                    V50Enums.RejectCode.COUNTER_EVIDENCE_STRONG, threshold, falsifier.evidence);
        }
        if (uncertainty == null || uncertainty.total > 0.64d) {
            return shadow(
                    V50Enums.RejectCode.UNCERTAINTY_HIGH, threshold, "TOTAL_UNCERTAINTY_HIGH");
        }
        V50Enums.Lane lane;
        if (domain.status == V50Enums.DomainStatus.PROVEN) {
            lane = V50Enums.Lane.CORE_PROVEN;
        } else if (domain.status == V50Enums.DomainStatus.READY) {
            lane = V50Enums.Lane.DYNAMIC_CONFIRMED;
        } else if (domain.status == V50Enums.DomainStatus.WATCH
                && domain.sample >= 30
                && selectedHitP >= threshold + 0.015d) {
            lane = V50Enums.Lane.CONTROLLED_PROBE;
        } else {
            return shadow(
                    V50Enums.RejectCode.DOMAIN_HARD_FREEZE,
                    threshold,
                    "DOMAIN_CERTIFICATE_NOT_READY");
        }
        return new Result(
                lane, true, V50Enums.RejectCode.NONE, threshold, "V50_SELECTIVE_POLICY_PASS");
    }

    private static Result shadow(V50Enums.RejectCode code, double threshold, String detail) {
        return new Result(V50Enums.Lane.SHADOW, false, code, threshold, detail);
    }
}
