package com.ethk3.novaonepro;

import java.util.List;

/** Host contracts for the R817 memory, provider-time and executable Research closure. */
public final class R817RuntimeResearchClosureHarness {
    private static int assertions;

    public static void main(String[] args) {
        releaseIdentity();
        projectionContract();
        providerReconciliation();
        settlementSingleFlight();
        deferredContract();
        researchIncubatorContract();
        adaptivePredrawContract();
        System.out.println(
                "R817_RUNTIME_RESEARCH_CLOSURE=PASS|ASSERTIONS="
                        + assertions
                        + "|IDENTITY,NO_RAW_JSON_HISTORY,PROVIDER_SELF_HEAL,SETTLEMENT_SINGLE_FLIGHT,DEFERRED_120x10,"
                        + "FORMULA60,INCUBATOR_N12,FULL_JAVA_CODEGEN,DYNAMIC_FTRL_PIT_CONSUMER,SHADOW_REAL_LOCK");
    }

    private static void releaseIdentity() {
        require("V57.16.4-R4.0.4-904".equals(ReleaseIdentityR817.VERSION_NAME), "VERSION");
        require(ReleaseIdentityR817.VERSION_CODE == 577200904, "VERSION_CODE");
        require(ReleaseIdentityR817.isConsistent(), "IDENTITY_CONSISTENT");
    }

    private static void projectionContract() {
        String[] columns = DrawHistoryProjectionR817.COLUMNS;
        require(columns.length == 17, "HISTORY_PROJECTION_17");
        for (int i = 0; i < columns.length; i++)
            require(!"raw_json".equals(columns[i]), "RAW_JSON_EXCLUDED_" + i);
    }

    private static void providerReconciliation() {
        long now = 1787628000000L;
        long stale = now - 8L * 60L * 60L * 1000L;
        long current = now - 30000L;
        require(
                ProviderTimeReconcilerR817.prefer(stale, current, stale, now) == current,
                "CURRENT_REPLACES_ZONE_STALE");
        require(
                ProviderTimeReconcilerR817.prefer(current, stale, current, now) == current,
                "CURRENT_NOT_REGRESSED");
        long historicalReceipt = 1700001000000L;
        long historical = 1700000000000L;
        require(
                ProviderTimeReconcilerR817.prefer(
                                historical, historical + 60000L, historical, historicalReceipt)
                        == ProviderTimeCodecR816.preferCorrectedOpenTime(
                                historical, historical + 60000L, historical),
                "HISTORICAL_CONSERVATIVE");
    }

    private static void deferredContract() {
        require(AppConfig.R36_DEFERRED_ACTION_CHUNK == 120, "DEFERRED_CHUNK_120");
        require(
                K3ActionSpaceEngine.TOTAL_ACTIONS / AppConfig.R36_DEFERRED_ACTION_CHUNK == 10,
                "DEFERRED_TEN_SLICES");
    }

    private static void settlementSingleFlight() {
        SettlementExecutionGateR819.resetForTests();
        require(!SettlementExecutionGateR819.isActive(), "SETTLEMENT_GATE_INITIAL_IDLE");
        require(SettlementExecutionGateR819.tryEnter(), "SETTLEMENT_GATE_FIRST_OWNER");
        require(SettlementExecutionGateR819.isActive(), "SETTLEMENT_GATE_ACTIVE");
        require(!SettlementExecutionGateR819.tryEnter(), "SETTLEMENT_GATE_REJECTS_SECOND_OWNER");
        SettlementExecutionGateR819.exit();
        require(!SettlementExecutionGateR819.isActive(), "SETTLEMENT_GATE_RELEASED");
        require(SettlementExecutionGateR819.tryEnter(), "SETTLEMENT_GATE_REUSABLE");
        SettlementExecutionGateR819.exit();
    }

    private static void researchIncubatorContract() {
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "THIRD",
                        "selected-vs-best-excluded margin",
                        "future-live counter evidence required",
                        "PRODUCTION_FROZEN",
                        "ResearchDslCandidateRuntimeR811,ResearchModelCandidateRuntimeR811",
                        "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");
        StrategyChallengerFactoryEngine.ResearchDataset dataset =
                new StrategyChallengerFactoryEngine.ResearchDataset(
                        V51ChecksumEngine.sha256("R817_DATASET"), "202608250099", 25);
        ResearchCandidateFormulaLibraryR3611.validateAll();
        require(ResearchCandidateFormulaLibraryR3611.SEEDS.length == 60, "FORMULA_LIBRARY_EXACT_60");
        require(ResearchStrategyCatalogR820.FORMULA_COUNT == 60, "FORMULA_CATALOG_EXACT_60");
        require(ResearchStrategyCatalogR820.fullCatalog().indexOf("F060|ENSEMBLE_BALANCED|") >= 0, "FORMULA_F060_VISIBLE");
        List<StrategyChallengerFactoryEngine.CandidateSpec> candidates =
                StrategyChallengerFactoryEngine.generate(finding, dataset);
        require(candidates.size() >= 60, "FORMULA_POPULATION_GENERATED_60_PLUS");
        int runtime = 0;
        for (int i = 0; i < candidates.size(); i++) {
            StrategyChallengerFactoryEngine.CandidateSpec candidate = candidates.get(i);
            if (!candidate.runtimeExecutable) continue;
            runtime++;
            require(
                    "EXPLORATORY_COLLECT_ONLY".equals(candidate.status),
                    "INCUBATOR_NOT_PROMOTION_" + i);
            CandidateCodeSynthesisEngine.Bundle bundle =
                    CandidateCodeSynthesisEngine.synthesize(candidate);
            require(
                    "SOURCE_READY_RUNTIME_DSL_VERIFIED".equals(bundle.status),
                    "CODEGEN_RUNTIME_STATUS_" + i);
            require(bundle.javaSource.indexOf("MAY_AUTHORIZE_REAL=false") >= 0, "JAVA_REAL_LOCK_" + i);
            require(bundle.javaSource.indexOf("public static double score") >= 0, "JAVA_EXECUTABLE_" + i);
            require(bundle.bundleHash.length() == 64, "BUNDLE_HASH_" + i);
        }
        require(runtime >= 60, "RUNTIME_FORMULAS_PRESENT_60_PLUS");
        require(
                AppConfig.R815_SHADOW_ONLY
                        && AppConfig.R815_REAL_BET_HARD_DISABLED
                        && !AppConfig.R36_REAL_BET_UNLOCK
                        && !ResearchShadowChallengerRuntimeR817.MAY_AUTHORIZE_REAL,
                "RESEARCH_PRODUCTION_BOUNDARY");
    }

    private static void adaptivePredrawContract() {
        require(ResearchShadowChallengerRuntimeR817.adaptiveWeight(0L) == 0.0d, "FTRL_WEIGHT_ZERO");
        double early = ResearchShadowChallengerRuntimeR817.adaptiveWeight(12L);
        double mature = ResearchShadowChallengerRuntimeR817.adaptiveWeight(120L);
        require(early > 0.0d && early < mature, "FTRL_WEIGHT_MATURITY");
        require(Math.abs(mature - 0.35d) < 1.0e-12d, "FTRL_WEIGHT_CAP");
        require(
                ResearchShadowChallengerRuntimeR817.adaptiveWeight(10000L) == mature,
                "FTRL_WEIGHT_STAYS_CAPPED");
        require(
                Math.abs(ResearchShadowChallengerRuntimeR817.blend(0.8d, 0.2d, 0.0d) - 0.8d)
                        < 1.0e-12d,
                "FORMULA_ONLY_BEFORE_ARTIFACT");
        double blended = ResearchShadowChallengerRuntimeR817.blend(0.8d, 0.2d, mature);
        require(Math.abs(blended - 0.59d) < 1.0e-12d, "MATURE_FTRL_BLEND");
        require(blended >= 0.0d && blended <= 1.0d, "FTRL_BLEND_BOUNDED");
        require(
                ResearchAdaptiveLearnerStoreR811.FEATURE_SCHEMA.indexOf("selected_hit_p") >= 0,
                "FTRL_FEATURE_SCHEMA_FROZEN");
    }

    private static void require(boolean condition, String code) {
        assertions++;
        if (!condition) throw new AssertionError("R817_ASSERTION_FAILED:" + code);
    }
}
