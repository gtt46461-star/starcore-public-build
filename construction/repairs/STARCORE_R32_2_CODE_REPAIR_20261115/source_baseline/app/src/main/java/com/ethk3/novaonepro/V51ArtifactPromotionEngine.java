package com.ethk3.novaonepro;

/** All-or-nothing promotion guard for the compiled V51 artifact bundle. */
public final class V51ArtifactPromotionEngine {
    public static final class Evaluation {
        public final String bundleHash, dataHash;
        public final boolean quantitative, parity, realtime, deterministic;
        public final double p95Ms;

        public Evaluation(
                String bundleHash,
                String dataHash,
                boolean quantitative,
                boolean parity,
                boolean realtime,
                boolean deterministic,
                double p95Ms) {
            this.bundleHash = s(bundleHash);
            this.dataHash = s(dataHash);
            this.quantitative = quantitative;
            this.parity = parity;
            this.realtime = realtime;
            this.deterministic = deterministic;
            this.p95Ms = Math.max(0, p95Ms);
        }
    }

    public static final class Result {
        public final boolean promoted;
        public final String code;

        Result(boolean promoted, String code) {
            this.promoted = promoted;
            this.code = code;
        }
    }

    private V51ArtifactPromotionEngine() {}

    public static Result promote(DbStore store, Evaluation evaluation) {
        if (store == null || evaluation == null)
            return new Result(false, "V51_PROMOTION_INPUT_MISSING");
        if (!V51BuildManifest.MODEL_PROVEN)
            return new Result(false, "R811_PRODUCTION_CHAMPION_LOCKED_MODEL_NOT_PROVEN");
        if (!V51ArtifactBundleManifest.BUNDLE_HASH.equals(evaluation.bundleHash))
            return new Result(false, "V51_PROMOTION_BUNDLE_MISMATCH");
        if (!V51ArtifactBundleManifest.isInternallyConsistent())
            return new Result(false, "V51_PROMOTION_BUNDLE_INCONSISTENT");
        if (!(evaluation.quantitative
                && evaluation.parity
                && evaluation.realtime
                && evaluation.deterministic))
            return new Result(false, "V51_PROMOTION_GATE_NOT_PASSED");
        if (evaluation.p95Ms > 5000.0d) return new Result(false, "V51_PROMOTION_P95_EXCEEDED");
        if (!V51BuildManifest.mayAuthorizeRealBet())
            return new Result(false, "V51_COMPILED_RELEASE_NOT_PROVEN");
        V51ChampionRegistry.mark(store, evaluation.bundleHash, "ACTIVE");
        store.setState("v51_champion_data_hash", evaluation.dataHash);
        return new Result(true, "V51_PROMOTED");
    }

    public static Result promoteResearchToShadow(
            DbStore store,
            StrategyChallengerFactoryEngine.CandidateSpec candidate,
            ResearchCertificationEngineR811.Certificate certificate,
            boolean parityPass,
            boolean pitPass) {
        ResearchPromotionGovernanceEngine.Decision decision =
                ResearchPromotionGovernanceEngine.allowResearchToShadow(
                        candidate, certificate, parityPass, pitPass);
        if (!decision.allowed) return new Result(false, "R811_RESEARCH_" + decision.reason);
        store.setState("r811_research_candidate", candidate.candidateId);
        store.setState("r811_research_shadow_state", decision.state);
        store.setState("r811_research_shadow_at", String.valueOf(TimeSourceR811.wallNowMs()));
        return new Result(true, "R811_RESEARCH_SHADOW_CHALLENGER");
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

    // V57_B13_ATOMIC_PROMOTION
    public static Result promoteAtomicBundle(DbStore store, Evaluation evaluation) {
        if (store == null || evaluation == null)
            return new Result(false, "V57_ATOMIC_INPUT_MISSING");
        if (!V51ArtifactBundleManifest.isInternallyConsistent())
            return new Result(false, "V57_ATOMIC_BUNDLE_INCONSISTENT");
        if (!V51NumberModelArtifact.PROVEN
                || !V51ExpertGateArtifact.PROVEN
                || !V51ActionRankerArtifact.PROVEN
                || !V51SelectedCalibrationArtifact.PROVEN
                || !V51PredictabilityArtifact.PROVEN
                || !V51PolicySurfaceArtifact.MODEL_GATE_PASSED)
            return new Result(false, "V57_ATOMIC_COMPONENT_UNPROVEN");
        Result r = promote(store, evaluation);
        if (r.promoted) {
            store.setState("v57_artifact_bundle_state", "ACTIVE_ATOMIC");
            store.setState("v57_artifact_bundle_hash", evaluation.bundleHash);
            store.setState("v57_artifact_data_hash", evaluation.dataHash);
        }
        return r;
    }
}
