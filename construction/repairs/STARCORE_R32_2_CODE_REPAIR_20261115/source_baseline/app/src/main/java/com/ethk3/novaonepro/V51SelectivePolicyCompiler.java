package com.ethk3.novaonepro;

/** B11 final policy proof compiler. Research scores/frequency never bypass missing proof. */
public final class V51SelectivePolicyCompiler {
    public static final String V57_B11_OWNER = "POLICY_PROOF_COMPILER";

    public static final class Result {
        public final boolean realEligible;
        public final V51Enums.RejectCode reject;
        public final String reason, checksum;

        Result(boolean e, V51Enums.RejectCode r, String why) {
            realEligible = e;
            reject = r;
            reason = why;
            checksum = V51ChecksumEngine.sha256(String.valueOf(e) + '|' + r + '|' + why);
        }
    }

    public Result compile(
            V51Models.SelectedActionEnvelope e, V51RankStabilityEngine.Result stability) {
        if (e == null || e.action == null || e.domain == null)
            return new Result(false, V51Enums.RejectCode.DATA_INCOMPLETE, "INPUT");
        if (!ModelBundleRuntimeR932.number().PROVEN || !ModelBundleRuntimeR932.gate().PROVEN)
            return new Result(
                    false, V51Enums.RejectCode.MODEL_NOT_PROVEN, "NUMBER_OR_GATE_UNPROVEN");
        if (!ModelBundleRuntimeR932.ranker().PROVEN
                || !ModelBundleRuntimeR932.ranker().RANK_ORDER_PASSED
                || ModelBundleRuntimeR932.ranker().STABLE_FOLDS < 4)
            return new Result(false, V51Enums.RejectCode.RANK_ORDER_NOT_PROVEN, "RANKER_UNPROVEN");
        if (!ModelBundleRuntimeR932.calibration().PROVEN
                || Double.isNaN(e.selectedHitP)
                || Double.isInfinite(e.selectedHitP))
            return new Result(
                    false,
                    V51Enums.RejectCode.CALIBRATION_NOT_PROVEN,
                    "SELECTED_CALIBRATION_UNPROVEN");
        if (e.domain.status != V51Enums.DomainStatus.PROVEN)
            return new Result(false, V51Enums.RejectCode.DOMAIN_NOT_PROVEN, "DOMAIN_UNPROVEN");
        if (stability == null || !stability.stable)
            return new Result(false, V51Enums.RejectCode.RANK_UNSTABLE, "RANK_UNSTABLE");
        if (!ModelBundleRuntimeR932.policy().MODEL_GATE_PASSED)
            return new Result(false, V51Enums.RejectCode.POLICY_CELL_NOT_PROVEN, "POLICY_UNPROVEN");
        return new Result(true, V51Enums.RejectCode.NONE, "ALL_PROOFS_PASS");
    }
}
