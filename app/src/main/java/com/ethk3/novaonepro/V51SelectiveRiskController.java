package com.ethk3.novaonepro;

/**
 * B11 policy selector. No controlled-probe bypass when model/calibration/ranker/domain proof is
 * missing.
 */
public final class V51SelectiveRiskController {
    public static final String V57_B11_OWNER = "SOLE_LANE_OWNER";

    public static final class Result {
        public final V51Enums.Lane lane;
        public final V51Enums.RejectCode rejectCode;
        public final V51PolicyCell cell;
        public final boolean realEligible;
        public final String proofReason;

        Result(V51Enums.Lane lane, V51Enums.RejectCode reject, V51PolicyCell cell) {
            this(lane, reject, cell, false, "");
        }

        Result(
                V51Enums.Lane lane,
                V51Enums.RejectCode reject,
                V51PolicyCell cell,
                boolean eligible,
                String reason) {
            this.lane = lane;
            rejectCode = reject;
            this.cell = cell;
            realEligible = eligible;
            proofReason = reason;
        }
    }

    private final V51PolicySurfaceEngine surface = new V51PolicySurfaceEngine();
    private final V51SelectivePolicyCompiler compiler = new V51SelectivePolicyCompiler();

    public Result decide(
            V51Models.SelectedActionEnvelope e,
            V51EffectiveConsensusEngine.Consensus consensus,
            V51RankStabilityEngine.Result stability) {
        if (!V51ArtifactBundleManifest.isInternallyConsistent())
            return new Result(
                    V51Enums.Lane.SHADOW,
                    V51Enums.RejectCode.ARTIFACT_BUNDLE_MISMATCH,
                    null,
                    false,
                    "BUNDLE");
        if (e == null
                || e.action == null
                || e.domain == null
                || e.predictability == null
                || consensus == null
                || stability == null)
            return new Result(
                    V51Enums.Lane.HOLD, V51Enums.RejectCode.DATA_INCOMPLETE, null, false, "INPUT");
        V51SelectivePolicyCompiler.Result proof = compiler.compile(e, stability);
        if (!proof.realEligible)
            return new Result(V51Enums.Lane.SHADOW, proof.reject, null, false, proof.reason);
        if (!e.predictability.qualified)
            return new Result(
                    V51Enums.Lane.SHADOW,
                    V51Enums.RejectCode.PREDICTABILITY_NOT_PROVEN,
                    null,
                    false,
                    "PREDICTABILITY");
        V51PolicyCell cell = surface.match(e, consensus);
        if (cell == null)
            return new Result(
                    V51Enums.Lane.SHADOW,
                    V51Enums.RejectCode.POLICY_CELL_NOT_PROVEN,
                    null,
                    false,
                    "CELL");
        V51Enums.Lane lane =
                e.domain.sample >= 240 && e.domain.credibleLow >= 0.38d
                        ? V51Enums.Lane.CORE_PROVEN
                        : V51Enums.Lane.DYNAMIC_CONFIRMED;
        return new Result(lane, V51Enums.RejectCode.NONE, cell, true, "ALL_PROOFS_PASS");
    }
}
