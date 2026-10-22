package com.ethk3.novaonepro;

/** R974 qualified-model gate. Historical raw-score thresholds are diagnostic constants only.
 * A quality certificate and exact immutable prediction proof are required; no raw-score fallback
 * may create REAL. DecisionGateEngine and the canonical DbStore transaction remain the owners.
 */
public final class V51CandidateRealGateR915 {
    public static final String VERSION = "R974_CERTIFIED_EXACT_ONLY_V1";
    public static final double MIN_RAW_ACTION_P = 0.320d;
    public static final double MIN_THIRD_MARGIN = 0.005d;
    public static final double MIN_ACTION_STABILITY = 0.480d;
    public static final double MAX_ENTROPY = 0.920d;
    public static final double MIN_ASSIGNMENT_SUPPORT = 0.450d;
    public static final double MAX_FALSIFIER = 0.750d;
    public static final double MIN_RANK_RETENTION = 0.500d;
    public static final double MIN_THIRD_ROBUSTNESS = 0.500d;
    public static final double MIN_MARGIN_ROBUSTNESS = 0.250d;

    public static final class Evaluation {
        public final boolean eligible;
        public final String code, detail, checksum;

        Evaluation(boolean eligible, String code, String detail) {
            this.eligible = eligible;
            this.code = safe(code);
            this.detail = safe(detail);
            this.checksum = V51ChecksumEngine.sha256(
                    VERSION + '|' + (eligible ? '1' : '0') + '|' + this.code + '|' + this.detail);
        }
    }

    private V51CandidateRealGateR915() {}

    public static Evaluation evaluate(
            DbStore store,
            V51Models.SelectedActionEnvelope selected,
            V51RankStabilityEngine.Result stability) {
        return fail("NO_QUALIFIED_MODEL_PROOF", "RAW_SCORE_OR_OLD_ENGINEERING_CANDIDATE_IS_NOT_INDEPENDENT_QUALITY_EVIDENCE");
    }

    public static Evaluation evaluate(DbStore store, V51CompiledActionContract compiled) {
        if (compiled == null) return fail("INPUT", "COMPILED_NULL");
        if (compiled.policy == null
                || compiled.policy.lane != V51Enums.Lane.CONTROLLED_PROBE
                || !compiled.policy.realEligible)
            return fail("POLICY", "CONTROLLED_PROBE_REAL_ELIGIBLE_REQUIRED");
        if (compiled.checksum.length() != 64
                || compiled.featureHash.length() == 0
                || compiled.stateHash.length() == 0
                || compiled.expertHash.length() == 0
                || compiled.probabilityHash.length() == 0
                || compiled.actionHash.length() == 0)
            return fail("COMPILED_HASH", "COMPILED_CHAIN_INCOMPLETE");
        if(compiled.certifiedPrediction!=null)return CertifiedPredictionR974.evaluate(store,compiled.selected,compiled.certifiedPrediction);
        return evaluate(store, compiled.selected, compiled.stability);
    }

    public static boolean verifyAuthorized(
            DbStore store,
            V51CompiledActionContract compiled,
            V51Models.AuthorizedDecision authorized) {
        if (authorized == null
                || authorized.action != V51Enums.DecisionAction.BET
                || authorized.rejectCode != V51Enums.RejectCode.NONE
                || authorized.selected != (compiled == null ? null : compiled.selected)) return false;
        Evaluation gate = evaluate(store, compiled);
        return gate.eligible && V51AuthorizationCore.verify(authorized, compiled);
    }

    public static boolean releaseBoundary() {
        return V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED
                && !V51BuildManifest.SHADOW_ONLY
                && !V51BuildManifest.REAL_BET_HARD_DISABLED
                && !V51BuildManifest.MODEL_PROVEN
                && !V51BuildManifest.REALTIME_PROVEN
                && !AppConfig.R815_SHADOW_ONLY
                && !AppConfig.R815_REAL_BET_HARD_DISABLED
                && !AppConfig.R815_MODEL_PROVEN
                && V51BuildManifest.mayAuthorizeRealBet();
    }

    private static Evaluation fail(String code, String detail) {
        return new Evaluation(false, code, detail);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
