package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit V43 strategy ownership catalog. It documents and validates which modules produce
 * evidence, which module owns probability, and which module owns BET.
 */
public final class StrategyModuleCatalog {
    public static final String V46_ROLE = "V46_SINGLE_OWNER_CATALOG";

    public static final String VERSION = AppConfig.V43_MODULE_CATALOG_VERSION;

    public static final class Spec {
        public final String name;
        public final String stage;
        public final String role;
        public final boolean mayWriteProbability;
        public final boolean mayCreateBet;

        Spec(String n, String s, String r, boolean p, boolean b) {
            name = n;
            stage = s;
            role = r;
            mayWriteProbability = p;
            mayCreateBet = b;
        }

        public String text() {
            return name
                    + "@"
                    + stage
                    + ":"
                    + role
                    + ":P="
                    + (mayWriteProbability ? "1" : "0")
                    + ":BET="
                    + (mayCreateBet ? "1" : "0");
        }
    }

    private StrategyModuleCatalog() {}

    public static List<Spec> all() {
        ArrayList<Spec> x = new ArrayList<Spec>();
        add(x, "WindowFeatureEngine", "PREDRAW", "SHAPE_FEATURE", false, false);
        add(x, "ShapeStateEngine", "PREDRAW", "SHAPE_STATE", false, false);
        add(x, "PatternStateEngine", "PREDRAW", "PATTERN_STATE", false, false);
        add(x, "TrendStateMachine", "PREDRAW", "TREND_STATE", false, false);
        add(x, "MarkovEngine", "PREDRAW", "TRANSITION_FEATURE", false, false);
        add(x, "BreakRecoveryEngine", "PREDRAW", "BREAK_RECOVERY", false, false);
        add(x, "CycleRhythmEngine", "PREDRAW", "CYCLE_RHYTHM", false, false);
        add(x, "PreDrawFeatureEngine", "PREDRAW", "POINT_IN_TIME_FEATURE", false, false);
        add(x, "FullCandidateScanEngine", "CANDIDATE", "EXACT_K3_SCAN", false, false);
        add(x, "CausalFactorMatrixEngine", "EVIDENCE", "CAUSAL_FACTOR", false, false);
        add(x, "PositionCauseMatrixEngine", "EVIDENCE", "POSITION_CAUSE", false, false);
        add(x, "DynamicCauseChainEngine", "EVIDENCE", "DYNAMIC_CAUSE", false, false);
        add(x, "NeedSupplyReasonEngine", "EVIDENCE", "NEED_SUPPLY", false, false);
        add(x, "AssociationGraphEngine", "EVIDENCE", "ASSOCIATION", false, false);
        add(x, "TransitionRelationEngine", "EVIDENCE", "TRANSITION_RELATION", false, false);
        add(x, "TeacherNumberMatrixEngine", "EVIDENCE", "TEACHER_NUMBER", false, false);
        add(x, "TeacherMatrixEngine", "BACKGROUND", "TEACHER_PROFILE", false, false);
        add(x, "CounterfactualRankEngine", "EVIDENCE", "COUNTERFACTUAL_RANK", false, false);
        add(x, "CounterfactualReasonEngine", "EVIDENCE", "COUNTERFACTUAL_REASON", false, false);
        add(
                x,
                "CounterfactualDistillationEngine",
                "EVIDENCE",
                "COUNTERFACTUAL_DISTILL",
                false,
                false);
        add(x, "ContrastiveReasonEngine", "EVIDENCE", "CONTRASTIVE_REASON", false, false);
        add(x, "ThirdGuardMatrixEngine", "RISK", "THIRD_GUARD", false, false);
        add(x, "RuleHealthMatrixEngine", "RISK", "RULE_HEALTH", false, false);
        add(x, "FalseStrongFreezeEngine", "RISK", "FALSE_STRONG", false, false);
        add(x, "DynamicFreezeMatrixEngine", "RISK", "DYNAMIC_FREEZE", false, false);
        add(x, "ShadowHealthGateEngine", "RISK", "SHADOW_HEALTH", false, false);
        add(x, "ShadowPromotionVerifierEngine", "BACKGROUND", "SHADOW_PROMOTION", false, false);
        add(x, "AdaptiveLearningJudgeEngine", "EVIDENCE", "ADAPTIVE_JUDGE", false, false);
        add(x, "ModuleEffectScoreEngine", "BACKGROUND", "MODULE_EFFECT", false, false);
        add(x, "HistoricalStatePriorEngine", "EVIDENCE", "HISTORICAL_PRIOR", false, false);
        add(
                x,
                "SequenceDistributionEngine",
                "PROBABILITY_INPUT",
                "TEN_CLASS_DISTRIBUTION",
                false,
                false);
        add(x, "ComboScannerEngine", "CANDIDATE", "LEGACY_COMBO_SCAN_EVIDENCE", false, false);
        add(x, "K3RoleComboEngine", "CANDIDATE", "K3_ROLE_DIVERSITY", false, false);
        add(x, "CandidateEvidenceEngine", "EVIDENCE", "CANDIDATE_EVIDENCE", false, false);
        add(x, "EvidenceRankerEngine", "EVIDENCE", "EVIDENCE_RANKING", false, false);
        add(x, "BiasEvidenceEngine", "EVIDENCE", "BIAS_EVIDENCE", false, false);
        add(x, "DeepFeatureEngine", "EVIDENCE", "DEEP_FEATURE_PROFILE", false, false);
        add(x, "DeepLearningBridgeEngine", "EVIDENCE", "PROFILE_REWARD_BRIDGE", false, false);
        add(x, "DeepProfileEngine", "EVIDENCE", "DEEP_PROFILE", false, false);
        add(x, "NumberBehaviorEngine", "EVIDENCE", "NUMBER_BEHAVIOR", false, false);
        add(x, "PositionStrengthEngine", "EVIDENCE", "POSITION_STRENGTH", false, false);
        add(x, "SimilarShapeEngine", "EVIDENCE", "SIMILAR_SHAPE", false, false);
        add(x, "ShapeMemoryEngine", "EVIDENCE", "SHAPE_MEMORY", false, false);
        add(x, "PreDrawReasonEngine", "EVIDENCE", "PREDRAW_REASON", false, false);
        add(x, "ReasonCausalMatrixEngine", "EVIDENCE", "REASON_CAUSAL_MATRIX", false, false);
        add(x, "AttributionEngine", "EVIDENCE", "ATTRIBUTION", false, false);
        add(x, "OpportunityMinerEngine", "EVIDENCE", "OPPORTUNITY_MINING", false, false);
        add(x, "DynamicWindowClusterEngine", "EVIDENCE", "DYNAMIC_WINDOW_CLUSTER", false, false);
        add(x, "RegimeRegistryEngine", "EVIDENCE", "REGIME_REGISTRY", false, false);
        add(x, "DistributionDriftEngine", "RISK", "DISTRIBUTION_DRIFT", false, false);
        add(x, "EvidenceHealthSnapshotEngine", "RISK", "EXPERT_HEALTH", false, false);
        add(
                x,
                "AdaptiveExpertConsensusEngine",
                "BACKGROUND",
                "OOS_WEIGHTED_CHALLENGER_CONSENSUS",
                false,
                false);
        add(x, "AiEnsembleJudgeEngine", "BACKGROUND", "LEGACY_ENSEMBLE_CHALLENGER", false, false);
        add(
                x,
                "BayesianEvidenceFusionEngine",
                "BACKGROUND",
                "BAYESIAN_FUSION_CHALLENGER",
                false,
                false);
        add(
                x,
                "OnlineEvidenceForestEngine",
                "BACKGROUND",
                "ONLINE_FOREST_CHALLENGER",
                false,
                false);
        add(x, "ProbabilityEngine", "BACKGROUND", "LEGACY_PROBABILITY_CHALLENGER", false, false);
        add(x, "CalibrationEngine", "BACKGROUND", "LEGACY_CALIBRATION_AUDIT", false, false);
        add(x, "ProbabilityAuditEngine", "BACKGROUND", "PROBABILITY_AUDIT", false, false);
        add(x, "LearningEngine", "BACKGROUND", "SETTLED_LEARNING", false, false);
        add(x, "OosActionLogEngine", "BACKGROUND", "OOS_ACTION_LOG", false, false);
        add(x, "OosEligibilityEngine", "BACKGROUND", "OOS_ELIGIBILITY", false, false);
        add(x, "OosReliabilityAuditEngine", "BACKGROUND", "OOS_RELIABILITY", false, false);
        add(
                x,
                "OosTrainingCoordinatorEngine",
                "BACKGROUND",
                "OOS_TRAINING_COORDINATOR",
                false,
                false);
        add(x, "PointInTimeSnapshotEngine", "BACKGROUND", "POINT_IN_TIME_SNAPSHOT", false, false);
        add(
                x,
                "RollingCausalBacktestEngine",
                "BACKGROUND",
                "ROLLING_CAUSAL_BACKTEST",
                false,
                false);
        add(x, "MicroWalkForwardEngine", "BACKGROUND", "MICRO_WALK_FORWARD", false, false);
        add(x, "WalkForwardBacktestEngine", "BACKGROUND", "WALK_FORWARD_BACKTEST", false, false);
        add(x, "ManualDeepBacktestEngine", "BACKGROUND", "MANUAL_DEEP_BACKTEST", false, false);
        add(x, "PromotionGateEngine", "BACKGROUND", "MODEL_PROMOTION_GATE", false, false);
        add(x, "ProfileFallbackEngine", "EVIDENCE", "PROFILE_FALLBACK", false, false);
        add(x, "HierarchicalStateTaxonomyEngine", "EVIDENCE", "STATE_TAXONOMY", false, false);
        add(x, "DecisionFunnelDiagnosisEngine", "DIAGNOSIS", "OOS_FUNNEL_STABILITY", false, false);
        add(x, "RealBetDiagnosisEngine", "DIAGNOSIS", "REAL_BET_DIAGNOSIS", false, false);
        add(x, "ReasonEngine", "DIAGNOSIS", "EXPLAINABILITY", false, false);
        add(x, "SelfDiagnosisEngine", "DIAGNOSIS", "SELF_DIAGNOSIS", false, false);
        add(x, "DiagnosisFixPlanEngine", "DIAGNOSIS", "FIX_PLAN_DIAGNOSIS", false, false);
        add(x, "OnlineActionModelEngine", "PROBABILITY_INPUT", "RESIDUAL_MODEL", false, false);
        add(
                x,
                "UnifiedStrategyEvidenceEngine",
                "PROBABILITY_INPUT",
                "BOUNDED_EVIDENCE_RESIDUAL",
                false,
                false);
        add(
                x,
                "RealtimeCausalProbabilityEngine",
                "PROBABILITY",
                "ONLY_EVENT_PROBABILITY_OWNER",
                true,
                false);
        add(x, "MonotonicCalibrationEngine", "CALIBRATION", "ONLY_CALIBRATION_OWNER", true, false);
        add(x, "HierarchicalPosteriorEngine", "CALIBRATION", "POSTERIOR_SHRINKAGE", false, false);
        add(x, "ActionUtilityEngine", "POLICY_INPUT", "UTILITY", false, false);
        add(x, "RiskCoverageController", "POLICY_INPUT", "COVERAGE_CONTROL", false, false);
        add(x, "LanePolicyCompiler", "POLICY", "ONLY_LANE_OWNER", false, false);
        add(x, "DecisionGateEngine", "DECISION", "ONLY_BET_SELECTION_OWNER", false, true);
        add(
                x,
                "DecisionAuthorizationEngine",
                "AUTHORIZATION",
                "ONLY_SIGNATURE_OWNER",
                false,
                false);
        add(x, "SettlementEngine", "SETTLEMENT", "ONLY_SETTLEMENT_OWNER", false, false);
        return x;
    }

    public static String validateAndPersist(DbStore store) {
        List<Spec> all = all();
        int missing = 0;
        int missingPreservedV42 = 0;
        int probabilityOwners = 0;
        int betOwners = 0;
        StringBuilder miss = new StringBuilder();
        StringBuilder missPreserved = new StringBuilder();
        for (int i = 0; i < all.size(); i++) {
            Spec s = all.get(i);
            try {
                Class.forName(
                        "com.ethk3.novaonepro." + s.name,
                        false,
                        StrategyModuleCatalog.class.getClassLoader());
            } catch (Throwable e) {
                missing++;
                if (miss.length() > 0) miss.append(',');
                miss.append(s.name);
            }
            if (s.mayWriteProbability && "PROBABILITY".equals(s.stage)) probabilityOwners++;
            if (s.mayCreateBet) betOwners++;
        }
        String[] preserved = preservedV42Classes();
        for (int i = 0; i < preserved.length; i++) {
            try {
                Class.forName(
                        "com.ethk3.novaonepro." + preserved[i],
                        false,
                        StrategyModuleCatalog.class.getClassLoader());
            } catch (Throwable e) {
                missingPreservedV42++;
                if (missPreserved.length() > 0) missPreserved.append(',');
                missPreserved.append(preserved[i]);
            }
        }
        String status =
                missing == 0 && missingPreservedV42 == 0 && probabilityOwners == 1 && betOwners == 1
                        ? "OK"
                        : "ERROR:missingCatalog="
                                + missing
                                + ",missingV42="
                                + missingPreservedV42
                                + ",probabilityOwners="
                                + probabilityOwners
                                + ",betOwners="
                                + betOwners;
        if (store != null)
            try {
                store.setState("v43_module_catalog_version", VERSION);
                store.setState("v43_module_catalog_status", status);
                store.setState("v43_module_catalog_count", String.valueOf(all.size()));
                store.setState("v43_module_catalog_missing", miss.toString());
                store.setState("v43_preserved_v42_count", String.valueOf(preserved.length));
                store.setState(
                        "v43_preserved_v42_missing_count", String.valueOf(missingPreservedV42));
                store.setState("v43_preserved_v42_missing", missPreserved.toString());
                store.setState("v43_probability_owner", "RealtimeCausalProbabilityEngine");
                store.setState("v43_lane_owner", "LanePolicyCompiler");
                store.setState("v43_bet_owner", "DecisionGateEngine");
                store.setState("v43_authorization_owner", "DecisionAuthorizationEngine");
            } catch (Throwable ignore) {
            }
        try {
            persistEvidenceContractAudit(store);
        } catch (Throwable ignore) {
        }
        return status;
    }

    /** Exact runtime preservation manifest for every Java class shipped by V42. */
    public static String[] preservedV42Classes() {
        return new String[] {
            "ActionUtilityEngine", "AdaptiveExpertConsensusEngine", "AdaptiveLearningJudgeEngine",
                    "AiEnsembleJudgeEngine", "ApiClient", "ApiParser",
            "AppConfig", "AssociationGraphEngine", "AttributionEngine", "AutoFetchService",
                    "BayesianEvidenceFusionEngine", "BiasEvidenceEngine",
            "BootReceiver", "BreakRecoveryEngine", "BuildIdentity", "CalibrationEngine",
                    "CandidateEvidenceEngine", "CausalFactorMatrixEngine",
            "ComboScannerEngine", "ContrastiveReasonEngine", "CountdownClock",
                    "CounterfactualDistillationEngine", "CounterfactualRankEngine",
                    "CounterfactualReasonEngine",
            "CrashRecorder", "CycleRhythmEngine", "DataGuard", "DatabaseCoordinator", "DbHelper",
                    "DbStore",
            "DecisionAuthorizationEngine", "DecisionGateEngine", "DeepFeatureEngine",
                    "DeepLearningBridgeEngine", "DeepProfileEngine", "DiagnosisFixPlanEngine",
            "DistributionDriftEngine", "DynamicCauseChainEngine", "DynamicFreezeMatrixEngine",
                    "DynamicGateEngine", "DynamicWindowClusterEngine",
                    "EvidenceHealthSnapshotEngine",
            "EvidenceRankerEngine", "FalseStrongFreezeEngine", "FullCandidateScanEngine",
                    "HierarchicalPosteriorEngine", "HierarchicalStateTaxonomyEngine",
                    "HistoricalStatePriorEngine",
            "K3RoleComboEngine", "KeyProvider", "LanePolicyCompiler", "LearningEngine",
                    "LiveActionBudgetEngine", "MainActivity",
            "ManualDeepBacktestEngine", "MarkovEngine", "MicroWalkForwardEngine", "Models",
                    "ModuleEffectScoreEngine", "MonotonicCalibrationEngine",
            "NeedSupplyReasonEngine", "NumberBehaviorEngine", "OnlineActionModelEngine",
                    "OnlineEvidenceForestEngine", "OosActionLogEngine", "OosEligibilityEngine",
            "OosReliabilityAuditEngine", "OosTrainingCoordinatorEngine", "OpportunityMinerEngine",
                    "PatternStateEngine", "PointInTimeSnapshotEngine", "PositionCauseMatrixEngine",
            "PositionStrengthEngine", "PreDrawFeatureEngine", "PreDrawReasonEngine",
                    "PredictionContractEngine", "ProbabilityAuditEngine", "ProbabilityEngine",
            "ProbeLanePolicyEngine", "ProfileFallbackEngine", "PromotionGateEngine",
                    "RealBetDiagnosisEngine", "RealtimeCausalProbabilityEngine",
                    "RealtimeTaskScheduler",
            "ReasonCausalMatrixEngine", "ReasonEngine", "RegimeRegistryEngine",
                    "RiskCoverageController", "RollingCausalBacktestEngine",
                    "RuleHealthMatrixEngine",
            "SafeLog", "SeedCauseExpansionEngine", "SelfDiagnosisEngine",
                    "SequenceDistributionEngine", "ServiceHeartbeatStore",
                    "ServiceWatchdogJobService",
            "SettlementEngine", "ShadowHealthGateEngine", "ShadowPromotionVerifierEngine",
                    "ShapeMath", "ShapeMemoryEngine", "ShapeStateEngine",
            "SimilarShapeEngine", "TeacherMatrixEngine", "TeacherNumberMatrixEngine",
                    "ThirdGuardMatrixEngine", "TransitionRelationEngine", "TrendStateMachine",
            "UiFormatter", "WalkForwardBacktestEngine", "WindowFeatureEngine"
        };
    }

    public static String manifestText() {
        StringBuilder sb = new StringBuilder();
        List<Spec> all = all();
        for (int i = 0; i < all.size(); i++) sb.append(all.get(i).text()).append('\n');
        return sb.toString();
    }

    private static void add(List<Spec> x, String n, String s, String r, boolean p, boolean b) {
        x.add(new Spec(n, s, r, p, b));
    }

    public static final String B07R3_FULLBLOOD_MARKER = "B07R3_CATALOG_FULLBLOOD";

    public static final class EvidenceDescriptor {
        public final String module;
        public final String stage;
        public final String role;
        public final String group;
        public final String inputSchema;
        public final String outputSchema;
        public final String allowedWrites;
        public final String forbiddenWrites;
        public final boolean replayRequired;
        public final int timeoutMs;

        EvidenceDescriptor(
                String m,
                String s,
                String r,
                String g,
                String in,
                String out,
                String allow,
                String deny,
                boolean replay,
                int timeout) {
            module = m;
            stage = s;
            role = r;
            group = g;
            inputSchema = in;
            outputSchema = out;
            allowedWrites = allow;
            forbiddenWrites = deny;
            replayRequired = replay;
            timeoutMs = timeout;
        }

        public String identity() {
            return module + "@" + stage + "#" + role + "#" + group;
        }

        public String audit() {
            return identity()
                    + "|in="
                    + inputSchema
                    + "|out="
                    + outputSchema
                    + "|allow="
                    + allowedWrites
                    + "|deny="
                    + forbiddenWrites
                    + "|replay="
                    + replayRequired
                    + "|timeout="
                    + timeoutMs;
        }
    }

    public static List<EvidenceDescriptor> b07EvidenceDescriptors() {
        ArrayList<EvidenceDescriptor> x = new ArrayList<EvidenceDescriptor>();
        x.add(
                new EvidenceDescriptor(
                        "WindowFeatureEngine",
                        "PREDRAW",
                        "SHAPE_FEATURE",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "ShapeStateEngine",
                        "PREDRAW",
                        "SHAPE_STATE",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "PatternStateEngine",
                        "PREDRAW",
                        "PATTERN_STATE",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "TrendStateMachine",
                        "PREDRAW",
                        "TREND_STATE",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "MarkovEngine",
                        "PREDRAW",
                        "TRANSITION_FEATURE",
                        "SEQUENCE_TRANSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "BreakRecoveryEngine",
                        "PREDRAW",
                        "BREAK_RECOVERY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "CycleRhythmEngine",
                        "PREDRAW",
                        "CYCLE_RHYTHM",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "PreDrawFeatureEngine",
                        "PREDRAW",
                        "POINT_IN_TIME_FEATURE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "FullCandidateScanEngine",
                        "CANDIDATE",
                        "EXACT_K3_SCAN",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "CausalFactorMatrixEngine",
                        "EVIDENCE",
                        "CAUSAL_FACTOR",
                        "CAUSAL_POSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "PositionCauseMatrixEngine",
                        "EVIDENCE",
                        "POSITION_CAUSE",
                        "CAUSAL_POSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "DynamicCauseChainEngine",
                        "EVIDENCE",
                        "DYNAMIC_CAUSE",
                        "CAUSAL_POSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "NeedSupplyReasonEngine",
                        "EVIDENCE",
                        "NEED_SUPPLY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "AssociationGraphEngine",
                        "EVIDENCE",
                        "ASSOCIATION",
                        "CAUSAL_POSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "TransitionRelationEngine",
                        "EVIDENCE",
                        "TRANSITION_RELATION",
                        "SEQUENCE_TRANSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "TeacherNumberMatrixEngine",
                        "EVIDENCE",
                        "TEACHER_NUMBER",
                        "TEACHER_LEARNING",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "TeacherMatrixEngine",
                        "BACKGROUND",
                        "TEACHER_PROFILE",
                        "TEACHER_LEARNING",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "CounterfactualRankEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL_RANK",
                        "COUNTERFACTUAL",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "CounterfactualReasonEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL_REASON",
                        "COUNTERFACTUAL",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "CounterfactualDistillationEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL_DISTILL",
                        "COUNTERFACTUAL",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "ContrastiveReasonEngine",
                        "EVIDENCE",
                        "CONTRASTIVE_REASON",
                        "COUNTERFACTUAL",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "ThirdGuardMatrixEngine",
                        "RISK",
                        "THIRD_GUARD",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "RuleHealthMatrixEngine",
                        "RISK",
                        "RULE_HEALTH",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "FalseStrongFreezeEngine",
                        "RISK",
                        "FALSE_STRONG",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "DynamicFreezeMatrixEngine",
                        "RISK",
                        "DYNAMIC_FREEZE",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "ShadowHealthGateEngine",
                        "RISK",
                        "SHADOW_HEALTH",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "ShadowPromotionVerifierEngine",
                        "BACKGROUND",
                        "SHADOW_PROMOTION",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "AdaptiveLearningJudgeEngine",
                        "EVIDENCE",
                        "ADAPTIVE_JUDGE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "ModuleEffectScoreEngine",
                        "BACKGROUND",
                        "MODULE_EFFECT",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "HistoricalStatePriorEngine",
                        "EVIDENCE",
                        "HISTORICAL_PRIOR",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "SequenceDistributionEngine",
                        "PROBABILITY_INPUT",
                        "TEN_CLASS_DISTRIBUTION",
                        "SEQUENCE_TRANSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "ComboScannerEngine",
                        "CANDIDATE",
                        "LEGACY_COMBO_SCAN_EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "K3RoleComboEngine",
                        "CANDIDATE",
                        "K3_ROLE_DIVERSITY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "CandidateEvidenceEngine",
                        "EVIDENCE",
                        "CANDIDATE_EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "EvidenceRankerEngine",
                        "EVIDENCE",
                        "EVIDENCE_RANKING",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "BiasEvidenceEngine",
                        "EVIDENCE",
                        "BIAS_EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "DeepFeatureEngine",
                        "EVIDENCE",
                        "DEEP_FEATURE_PROFILE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "DeepLearningBridgeEngine",
                        "EVIDENCE",
                        "PROFILE_REWARD_BRIDGE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "DeepProfileEngine",
                        "EVIDENCE",
                        "DEEP_PROFILE",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "NumberBehaviorEngine",
                        "EVIDENCE",
                        "NUMBER_BEHAVIOR",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "PositionStrengthEngine",
                        "EVIDENCE",
                        "POSITION_STRENGTH",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "SimilarShapeEngine",
                        "EVIDENCE",
                        "SIMILAR_SHAPE",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "ShapeMemoryEngine",
                        "EVIDENCE",
                        "SHAPE_MEMORY",
                        "SHAPE_TREND",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "PreDrawReasonEngine",
                        "EVIDENCE",
                        "PREDRAW_REASON",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "ReasonCausalMatrixEngine",
                        "EVIDENCE",
                        "REASON_CAUSAL_MATRIX",
                        "CAUSAL_POSITION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "AttributionEngine",
                        "EVIDENCE",
                        "ATTRIBUTION",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "OpportunityMinerEngine",
                        "EVIDENCE",
                        "OPPORTUNITY_MINING",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "DynamicWindowClusterEngine",
                        "EVIDENCE",
                        "DYNAMIC_WINDOW_CLUSTER",
                        "DYNAMIC_CLUSTER",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "RegimeRegistryEngine",
                        "EVIDENCE",
                        "REGIME_REGISTRY",
                        "DYNAMIC_CLUSTER",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "DistributionDriftEngine",
                        "RISK",
                        "DISTRIBUTION_DRIFT",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "EvidenceHealthSnapshotEngine",
                        "RISK",
                        "EXPERT_HEALTH",
                        "RISK_HEALTH",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "AdaptiveExpertConsensusEngine",
                        "BACKGROUND",
                        "OOS_WEIGHTED_CHALLENGER_CONSENSUS",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "AiEnsembleJudgeEngine",
                        "BACKGROUND",
                        "LEGACY_ENSEMBLE_CHALLENGER",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "BayesianEvidenceFusionEngine",
                        "BACKGROUND",
                        "BAYESIAN_FUSION_CHALLENGER",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "OnlineEvidenceForestEngine",
                        "BACKGROUND",
                        "ONLINE_FOREST_CHALLENGER",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "ProbabilityEngine",
                        "BACKGROUND",
                        "LEGACY_PROBABILITY_CHALLENGER",
                        "OOS_CALIBRATION",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "CalibrationEngine",
                        "BACKGROUND",
                        "LEGACY_CALIBRATION_AUDIT",
                        "OOS_CALIBRATION",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "ProbabilityAuditEngine",
                        "BACKGROUND",
                        "PROBABILITY_AUDIT",
                        "OOS_CALIBRATION",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "LearningEngine",
                        "BACKGROUND",
                        "SETTLED_LEARNING",
                        "TEACHER_LEARNING",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "OosActionLogEngine",
                        "BACKGROUND",
                        "OOS_ACTION_LOG",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "OosEligibilityEngine",
                        "BACKGROUND",
                        "OOS_ELIGIBILITY",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "OosReliabilityAuditEngine",
                        "BACKGROUND",
                        "OOS_RELIABILITY",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "OosTrainingCoordinatorEngine",
                        "BACKGROUND",
                        "OOS_TRAINING_COORDINATOR",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "PointInTimeSnapshotEngine",
                        "BACKGROUND",
                        "POINT_IN_TIME_SNAPSHOT",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "RollingCausalBacktestEngine",
                        "BACKGROUND",
                        "ROLLING_CAUSAL_BACKTEST",
                        "CAUSAL_POSITION",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "MicroWalkForwardEngine",
                        "BACKGROUND",
                        "MICRO_WALK_FORWARD",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "WalkForwardBacktestEngine",
                        "BACKGROUND",
                        "WALK_FORWARD_BACKTEST",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "ManualDeepBacktestEngine",
                        "BACKGROUND",
                        "MANUAL_DEEP_BACKTEST",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "PromotionGateEngine",
                        "BACKGROUND",
                        "MODEL_PROMOTION_GATE",
                        "AUXILIARY_EVIDENCE",
                        "SettledLedger+PITSnapshot+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "ProfileFallbackEngine",
                        "EVIDENCE",
                        "PROFILE_FALLBACK",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "HierarchicalStateTaxonomyEngine",
                        "EVIDENCE",
                        "STATE_TAXONOMY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "DecisionFunnelDiagnosisEngine",
                        "DIAGNOSIS",
                        "OOS_FUNNEL_STABILITY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "RealBetDiagnosisEngine",
                        "DIAGNOSIS",
                        "REAL_BET_DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "ReasonEngine",
                        "DIAGNOSIS",
                        "EXPLAINABILITY",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "SelfDiagnosisEngine",
                        "DIAGNOSIS",
                        "SELF_DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "DiagnosisFixPlanEngine",
                        "DIAGNOSIS",
                        "FIX_PLAN_DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "OnlineActionModelEngine",
                        "PROBABILITY_INPUT",
                        "RESIDUAL_MODEL",
                        "OOS_CALIBRATION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "UnifiedStrategyEvidenceEngine",
                        "PROBABILITY_INPUT",
                        "BOUNDED_EVIDENCE_RESIDUAL",
                        "OOS_CALIBRATION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "RealtimeCausalProbabilityEngine",
                        "PROBABILITY",
                        "ONLY_EVENT_PROBABILITY_OWNER",
                        "OOS_CALIBRATION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EventProbability",
                        "PROBABILITY_OWNER",
                        "Lane,BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "MonotonicCalibrationEngine",
                        "CALIBRATION",
                        "ONLY_CALIBRATION_OWNER",
                        "OOS_CALIBRATION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "CalibratedProbability",
                        "PROBABILITY_OWNER",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "HierarchicalPosteriorEngine",
                        "CALIBRATION",
                        "POSTERIOR_SHRINKAGE",
                        "OOS_CALIBRATION",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "CalibratedProbability",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "ActionUtilityEngine",
                        "POLICY_INPUT",
                        "UTILITY",
                        "POLICY_BOUNDARY",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        3));
        x.add(
                new EvidenceDescriptor(
                        "RiskCoverageController",
                        "POLICY_INPUT",
                        "COVERAGE_CONTROL",
                        "POLICY_BOUNDARY",
                        "FeatureSnapshot+StateLifecycle+Candidate",
                        "EvidencePacket",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        true,
                        4));
        x.add(
                new EvidenceDescriptor(
                        "LanePolicyCompiler",
                        "POLICY",
                        "ONLY_LANE_OWNER",
                        "POLICY_BOUNDARY",
                        "FrozenContract+Candidate",
                        "LanePolicy",
                        "EVIDENCE_ONLY",
                        "BET,Settlement",
                        true,
                        5));
        x.add(
                new EvidenceDescriptor(
                        "DecisionGateEngine",
                        "DECISION",
                        "ONLY_BET_SELECTION_OWNER",
                        "POLICY_BOUNDARY",
                        "FrozenContract+Candidate",
                        "Decision",
                        "BET_SELECTION_OWNER",
                        "Probability,Lane,Settlement",
                        false,
                        6));
        x.add(
                new EvidenceDescriptor(
                        "DecisionAuthorizationEngine",
                        "AUTHORIZATION",
                        "ONLY_SIGNATURE_OWNER",
                        "POLICY_BOUNDARY",
                        "FrozenContract+Candidate",
                        "AuthorizationSignature",
                        "EVIDENCE_ONLY",
                        "FinalP,Lane,BET,Settlement",
                        false,
                        2));
        x.add(
                new EvidenceDescriptor(
                        "SettlementEngine",
                        "SETTLEMENT",
                        "ONLY_SETTLEMENT_OWNER",
                        "AUXILIARY_EVIDENCE",
                        "FrozenContract+Candidate",
                        "SettlementRecord",
                        "EVIDENCE_ONLY",
                        "Probability,Lane,BET_CREATE",
                        false,
                        3));
        return x;
    }

    public static String validateEvidenceContract() {
        List<EvidenceDescriptor> all = b07EvidenceDescriptors();
        java.util.HashSet<String> seen = new java.util.HashSet<String>();
        java.util.LinkedHashMap<String, Integer> groups =
                new java.util.LinkedHashMap<String, Integer>();
        int duplicate = 0, unsafe = 0, probabilityOwners = 0, betOwners = 0, replay = 0;
        for (int i = 0; i < all.size(); i++) {
            EvidenceDescriptor d = all.get(i);
            if (!seen.add(d.module)) duplicate++;
            Integer old = groups.get(d.group);
            groups.put(d.group, Integer.valueOf(old == null ? 1 : old.intValue() + 1));
            if (d.replayRequired) replay++;
            if ("PROBABILITY_OWNER".equals(d.allowedWrites)) probabilityOwners++;
            if ("BET_SELECTION_OWNER".equals(d.allowedWrites)) betOwners++;
            if (d.module.length() == 0
                    || d.group.length() == 0
                    || d.inputSchema.length() == 0
                    || d.outputSchema.length() == 0) unsafe++;
            if (d.timeoutMs <= 0 || d.timeoutMs > 20) unsafe++;
            if (d.forbiddenWrites.indexOf("BET") < 0
                    && !"BET_SELECTION_OWNER".equals(d.allowedWrites)) unsafe++;
        }
        return duplicate == 0
                        && unsafe == 0
                        && probabilityOwners == 1
                        && betOwners == 1
                        && groups.size() >= 7
                        && replay >= 40
                ? "OK"
                : "ERROR:duplicate="
                        + duplicate
                        + ",unsafe="
                        + unsafe
                        + ",P="
                        + probabilityOwners
                        + ",BET="
                        + betOwners
                        + ",groups="
                        + groups.size()
                        + ",replay="
                        + replay;
    }

    public static String evidenceContractManifest() {
        StringBuilder s = new StringBuilder(24000);
        List<EvidenceDescriptor> x = b07EvidenceDescriptors();
        for (int i = 0; i < x.size(); i++) s.append(x.get(i).audit()).append('\n');
        s.append("status=").append(validateEvidenceContract()).append('\n');
        return s.toString();
    }

    public static String evidenceContractFingerprint() {
        return sha256R3(evidenceContractManifest());
    }

    private static String sha256R3(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(raw.getBytes("UTF-8"));
            StringBuilder s = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                String h = Integer.toHexString(b[i] & 255);
                if (h.length() == 1) s.append('0');
                s.append(h);
            }
            return s.toString();
        } catch (Throwable e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    public static void persistEvidenceContractAudit(DbStore store) {
        if (store == null) return;
        try {
            String status = validateEvidenceContract();
            store.setState("b07r3_evidence_catalog_status", status);
            store.setState(
                    "b07r3_evidence_catalog_count",
                    String.valueOf(b07EvidenceDescriptors().size()));
            store.setState("b07r3_evidence_catalog_hash", evidenceContractFingerprint());
            store.setState(
                    "b07r3_evidence_catalog_policy", "EVIDENCE_ONLY_EXCEPT_TYPED_SINGLE_OWNERS");
            store.registerModuleBuildV20("StrategyModuleCatalog", AppConfig.B07R3_VERSION, status);
        } catch (Throwable ignore) {
        }
    }
}
