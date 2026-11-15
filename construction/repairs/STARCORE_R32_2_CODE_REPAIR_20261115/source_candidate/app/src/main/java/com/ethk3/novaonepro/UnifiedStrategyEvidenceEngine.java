package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * V46 final evidence owner.
 *
 * <p>EventP is a calibrated event estimate and is never reduced merely to express risk. Final
 * evidence instead produces a separate ProbabilityUncertainty and ActionLCB. All three values are
 * frozen together at the final pre-lane boundary.
 */
public final class UnifiedStrategyEvidenceEngine {
    public static final String MODULE_VERSION = "B07R3_UNIFIED_EVIDENCE_KERNEL_BOUND_20260807";
    private final StarcoreEvidenceKernel starcoreKernel = new StarcoreEvidenceKernel();

    public ArrayList<Models.ComboCandidate> applyPreProbability(
            DbStore store,
            List<Models.ComboCandidate> input,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> out = copy(input);
        long started = System.currentTimeMillis();
        int hard = 0, complete = 0;
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) continue;
            StrategyEvidenceContract.Snapshot s = StrategyEvidenceContract.capture(c);
            StrategyEvidenceContract.write(c, s, "PRE_PROBABILITY");
            if (AppConfig.B07R3_KERNEL_REALTIME_ENABLED) {
                StarcoreEvidenceKernel.EvidenceBundle kb =
                        starcoreKernel.evaluate(store, c, "PRE_PROBABILITY");
                starcoreKernel.applyEvidenceOnly(c, kb);
            }
            c.v43InvariantStatus = PipelineInvariantGuard.STATUS_PENDING;
            c.v43PipelineStage = "V46_PRE_PROBABILITY";
            c.v46Version = AppConfig.V46_VERSION;
            if (s.hardRisk == 1) hard++;
            if (s.availableGroups >= AppConfig.V43_MIN_EVIDENCE_GROUPS) complete++;
        }
        persist(
                store,
                "PRE_PROBABILITY",
                out.size(),
                complete,
                hard,
                System.currentTimeMillis() - started);
        return out;
    }

    /** Last evidence stage: EventP is preserved; uncertainty and ActionLCB are produced. */
    public ArrayList<Models.ComboCandidate> applyPreDecision(
            DbStore store,
            List<Models.ComboCandidate> input,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> out = copy(input);
        long started = System.currentTimeMillis();
        int hard = 0, complete = 0;
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) continue;
            StrategyEvidenceContract.Snapshot s = StrategyEvidenceContract.capture(c);
            StrategyAblationAuditEngine.Result ab = StrategyAblationAuditEngine.evaluate(c, s);
            s.fragility = Math.max(s.fragility, ab.fragility);
            if (ab.dominantGroup.length() > 0) s.dominantGroup = ab.dominantGroup;
            StrategyEvidenceContract.write(c, s, "V46_FINAL_EVIDENCE_PRE_LOCK");
            if (AppConfig.B07R3_KERNEL_REALTIME_ENABLED) {
                StarcoreEvidenceKernel.EvidenceBundle kb =
                        starcoreKernel.evaluate(store, c, "PRE_DECISION");
                starcoreKernel.applyEvidenceOnly(c, kb);
                s = StrategyEvidenceContract.capture(c);
            }
            c.v43AblationFragility = s.fragility;
            c.v43DominantEvidence = s.dominantGroup;

            double eventP = eventProbability(c);
            double uncertainty = probabilityUncertainty(c, s);
            double actionLcb = actionLowerBound(c, s, eventP, uncertainty);
            c.v46EventProbability = eventP;
            c.v46ProbabilityUncertainty = uncertainty;
            c.v46ActionLcb = actionLcb;
            c.v46ExpectedValue = AppConfig.ODDS * eventP - AppConfig.BET_COST;
            c.v43SafeProbability = eventP; // legacy display bridge only
            c.honestProbability = eventP;
            c.calibratedHitProbability = eventP;
            c.calibratedP = eventP;
            c.finalP = eventP;
            c.finalPAfterFusion = eventP;
            c.finalPAfterDiagnosis = eventP;
            c.v43HardRisk = s.hardRisk;
            c.v43InvariantStatus = PipelineInvariantGuard.STATUS_PENDING;
            c.v43PipelineStage = "V46_FINAL_EVENT_AND_ACTION_BOUND_READY";
            c.v46ProbabilityTrace =
                    append(
                            c.v46ProbabilityTrace,
                            "FinalEvidence EventP="
                                    + round(eventP)
                                    + "|Uncertainty="
                                    + round(uncertainty)
                                    + "|ActionLCB="
                                    + round(actionLcb));
            c.v46Version = AppConfig.V46_VERSION;
            if (s.hardRisk == 1) hard++;
            if (s.availableGroups >= AppConfig.V43_MIN_EVIDENCE_GROUPS) complete++;
        }
        persist(
                store,
                "FINAL_EVENT_AND_ACTION_BOUND",
                out.size(),
                complete,
                hard,
                System.currentTimeMillis() - started);
        return out;
    }

    public static double adjustEventProbability(Models.ComboCandidate c, double baseP) {
        double p = clamp(baseP, 0.001d, 0.999d);
        if (c == null) return p;
        StrategyEvidenceContract.Snapshot s = StrategyEvidenceContract.capture(c);
        StrategyEvidenceContract.write(c, s, "PROBABILITY_RESIDUAL");
        double centeredSupport = s.support - 0.50d;
        double net =
                centeredSupport * 0.85d
                        - s.risk * 0.75d
                        - s.fragility * 0.30d
                        + (s.diversity - 0.50d) * 0.20d;
        double residual =
                clamp(
                        net * s.confidence,
                        -AppConfig.V43_MAX_NEGATIVE_PROBABILITY_RESIDUAL,
                        AppConfig.V43_MAX_POSITIVE_PROBABILITY_RESIDUAL);
        if (s.availableGroups < AppConfig.V43_MIN_EVIDENCE_GROUPS) {
            residual = Math.min(0.0d, residual) - AppConfig.V43_MISSING_EVIDENCE_PENALTY;
        }
        if (s.hardRisk == 1) residual = -AppConfig.V43_HARD_RISK_PROBABILITY_PENALTY;
        double adjusted = logistic(logit(p) + residual);
        adjusted =
                clamp(
                        adjusted,
                        p - AppConfig.V43_MAX_EVENT_PROBABILITY_DELTA,
                        p + AppConfig.V43_MAX_EVENT_PROBABILITY_DELTA);
        c.v43ProbabilityResidual = adjusted - p;
        c.v43StrategySupport = s.support;
        c.v43StrategyRisk = s.risk;
        c.v43StrategyConfidence = s.confidence;
        c.v43StrategyDiversity = s.diversity;
        c.v43StrategyMaturity = s.maturity;
        c.v43AblationFragility = s.fragility;
        c.v43EvidenceGroupCount = s.availableGroups;
        c.v43HardRisk = s.hardRisk;
        c.v43DominantEvidence = s.dominantGroup;
        c.v43MissingEvidence = s.missingGroups;
        c.v43EvidenceFingerprint = s.fingerprint;
        c.v43StrategyVersion = MODULE_VERSION;
        return clamp(adjusted, 0.001d, 0.999d);
    }

    /** Probability model completion only; final risk bound is calculated later. */
    public static void finalizeProbability(Models.ComboCandidate c) {
        if (c == null) return;
        double eventP = eventProbability(c);
        c.v46EventProbability = eventP;
        c.v43SafeProbability = eventP;
        c.honestProbability = eventP;
        c.calibratedHitProbability = eventP;
        c.calibratedP = eventP;
        c.finalP = eventP;
        c.v43InvariantStatus = PipelineInvariantGuard.STATUS_PENDING;
        c.v43PipelineStage = "V46_EVENT_PROBABILITY_COMPLETE_LOCK_PENDING";
        StrategyEvidenceContract.Snapshot s = StrategyEvidenceContract.capture(c);
        StrategyEvidenceContract.write(c, s, "V46_EVENT_PROBABILITY_COMPLETE");
    }

    /** Compatibility: SafeP now means EventP; action conservatism lives in ActionLCB. */
    public static double safeProbability(
            Models.ComboCandidate c, StrategyEvidenceContract.Snapshot s) {
        return eventProbability(c);
    }

    public static double probabilityUncertainty(
            Models.ComboCandidate c, StrategyEvidenceContract.Snapshot s) {
        if (c == null) return 1.0d;
        if (s == null) s = StrategyEvidenceContract.capture(c);
        double calibration =
                c.calibrationBrier > 0.0d ? clamp01(c.calibrationBrier / 0.25d) : 0.50d;
        double sample =
                Math.max(c.calibrationSample, Math.max(c.posteriorSample, c.v46ConditionalSample));
        double sampleRisk = 1.0d - Math.min(1.0d, sample / 240.0d);
        double u =
                0.22d * (1.0d - s.confidence)
                        + 0.14d * (1.0d - s.diversity)
                        + 0.18d * s.fragility
                        + 0.20d * s.risk
                        + 0.14d * calibration
                        + 0.12d * sampleRisk;
        return clamp(u, 0.0d, 1.0d);
    }

    public static double actionLowerBound(
            Models.ComboCandidate c,
            StrategyEvidenceContract.Snapshot s,
            double eventP,
            double uncertainty) {
        if (c == null) return 0.0d;
        if (s == null) s = StrategyEvidenceContract.capture(c);
        double penalty = AppConfig.V46_MAX_ACTION_BOUND_PENALTY * clamp01(uncertainty);
        if (s.availableGroups < AppConfig.V43_MIN_EVIDENCE_GROUPS) penalty += 0.010d;
        if (s.hardRisk == 1) penalty = Math.max(penalty, 0.070d);
        penalty = Math.min(0.090d, penalty);
        return clamp(eventP - penalty, 0.001d, 0.999d);
    }

    private static double eventProbability(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        if (valid(c.v46EventProbability)) return c.v46EventProbability;
        if (valid(c.calibratedHitProbability)) return c.calibratedHitProbability;
        if (valid(c.calibratedP)) return c.calibratedP;
        if (valid(c.finalP)) return c.finalP;
        if (valid(c.modelP)) return c.modelP;
        return AppConfig.RANDOM_K3_BASELINE;
    }

    private void persist(
            DbStore store, String stage, int count, int complete, int hard, long cost) {
        if (store == null) return;
        try {
            store.setState("v46_strategy_stage", stage);
            store.setState("v46_strategy_version", MODULE_VERSION);
            store.setState("v46_strategy_candidate_count", String.valueOf(count));
            store.setState("v46_strategy_complete_count", String.valueOf(complete));
            store.setState("v46_strategy_hard_risk_count", String.valueOf(hard));
            store.setState("v46_strategy_cost_ms", String.valueOf(cost));
            store.setState(
                    "v46_strategy_policy",
                    "EventP保持真实概率语义；风险单独生成Uncertainty和ActionLCB；三者在Lane前共同冻结");
            store.registerModuleBuildV20(
                    "UnifiedStrategyEvidenceEngine", MODULE_VERSION, stage + "_OK");
        } catch (Throwable ignore) {
        }
        persistStagePlanR3(store, stage, count, complete, hard, cost);
    }

    private static ArrayList<Models.ComboCandidate> copy(List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (input != null)
            for (int i = 0; i < input.size(); i++) if (input.get(i) != null) out.add(input.get(i));
        return out;
    }

    private static double logit(double p) {
        return Math.log(p / (1.0d - p));
    }

    private static double logistic(double x) {
        if (x >= 35.0d) return 1.0d;
        if (x <= -35.0d) return 0.0d;
        return 1.0d / (1.0d + Math.exp(-x));
    }

    private static boolean valid(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "|" + b;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final String B07R3_ORCHESTRATION_FULLBLOOD_MARKER = "B07R3_ORCHESTRATION_PLAN";

    public static final class StageContract {
        public final String module;
        public final String stage;
        public final String group;
        public final String input;
        public final String output;
        public final boolean mayAffectProbability, mayAffectLane, mayCreateBet;
        public final int timeoutMs;

        StageContract(
                String m,
                String s,
                String g,
                String i,
                String o,
                boolean p,
                boolean l,
                boolean b,
                int t) {
            module = m;
            stage = s;
            group = g;
            input = i;
            output = o;
            mayAffectProbability = p;
            mayAffectLane = l;
            mayCreateBet = b;
            timeoutMs = t;
        }

        public String text() {
            return module
                    + "@"
                    + stage
                    + "#"
                    + group
                    + "|"
                    + input
                    + "->"
                    + output
                    + "|P="
                    + mayAffectProbability
                    + "|L="
                    + mayAffectLane
                    + "|BET="
                    + mayCreateBet
                    + "|T="
                    + timeoutMs;
        }
    }

    public static final class StageAudit {
        public int modules, evidenceOnly, probabilityOwners, laneOwners, betOwners, unsafe;
        public String hash = "";

        public String summary() {
            return "modules="
                    + modules
                    + "|evidenceOnly="
                    + evidenceOnly
                    + "|P="
                    + probabilityOwners
                    + "|Lane="
                    + laneOwners
                    + "|BET="
                    + betOwners
                    + "|unsafe="
                    + unsafe
                    + "|hash="
                    + hash;
        }
    }

    public static List<StageContract> b07R3StagePlan() {
        ArrayList<StageContract> x = new ArrayList<StageContract>();
        x.add(
                new StageContract(
                        "WindowFeatureEngine",
                        "PREDRAW",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "ShapeStateEngine",
                        "PREDRAW",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "PatternStateEngine",
                        "PREDRAW",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "TrendStateMachine",
                        "PREDRAW",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "MarkovEngine",
                        "PREDRAW",
                        "SEQUENCE_TRANSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "BreakRecoveryEngine",
                        "PREDRAW",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "CycleRhythmEngine",
                        "PREDRAW",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "PreDrawFeatureEngine",
                        "PREDRAW",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "FullCandidateScanEngine",
                        "CANDIDATE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "CausalFactorMatrixEngine",
                        "EVIDENCE",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "PositionCauseMatrixEngine",
                        "EVIDENCE",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "DynamicCauseChainEngine",
                        "EVIDENCE",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "NeedSupplyReasonEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "AssociationGraphEngine",
                        "EVIDENCE",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "TransitionRelationEngine",
                        "EVIDENCE",
                        "SEQUENCE_TRANSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "TeacherNumberMatrixEngine",
                        "EVIDENCE",
                        "TEACHER_LEARNING",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "TeacherMatrixEngine",
                        "BACKGROUND",
                        "TEACHER_LEARNING",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "CounterfactualRankEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "CounterfactualReasonEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "CounterfactualDistillationEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "ContrastiveReasonEngine",
                        "EVIDENCE",
                        "COUNTERFACTUAL",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "ThirdGuardMatrixEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "RuleHealthMatrixEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "FalseStrongFreezeEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "DynamicFreezeMatrixEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "ShadowHealthGateEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "ShadowPromotionVerifierEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "AdaptiveLearningJudgeEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "ModuleEffectScoreEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "HistoricalStatePriorEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "SequenceDistributionEngine",
                        "PROBABILITY_INPUT",
                        "SEQUENCE_TRANSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "ComboScannerEngine",
                        "CANDIDATE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "K3RoleComboEngine",
                        "CANDIDATE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "CandidateEvidenceEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "EvidenceRankerEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "BiasEvidenceEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "DeepFeatureEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "DeepLearningBridgeEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "DeepProfileEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "NumberBehaviorEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "PositionStrengthEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "SimilarShapeEngine",
                        "EVIDENCE",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "ShapeMemoryEngine",
                        "EVIDENCE",
                        "SHAPE_TREND",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "PreDrawReasonEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "ReasonCausalMatrixEngine",
                        "EVIDENCE",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "AttributionEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "OpportunityMinerEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "DynamicWindowClusterEngine",
                        "EVIDENCE",
                        "DYNAMIC_CLUSTER",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "RegimeRegistryEngine",
                        "EVIDENCE",
                        "DYNAMIC_CLUSTER",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "DistributionDriftEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "EvidenceHealthSnapshotEngine",
                        "RISK",
                        "RISK_HEALTH",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "AdaptiveExpertConsensusEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "AiEnsembleJudgeEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "BayesianEvidenceFusionEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "OnlineEvidenceForestEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "ProbabilityEngine",
                        "BACKGROUND",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "CalibrationEngine",
                        "BACKGROUND",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "ProbabilityAuditEngine",
                        "BACKGROUND",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "LearningEngine",
                        "BACKGROUND",
                        "TEACHER_LEARNING",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "OosActionLogEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "OosEligibilityEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "OosReliabilityAuditEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "OosTrainingCoordinatorEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "PointInTimeSnapshotEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "RollingCausalBacktestEngine",
                        "BACKGROUND",
                        "CAUSAL_POSITION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "MicroWalkForwardEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "WalkForwardBacktestEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "ManualDeepBacktestEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "PromotionGateEngine",
                        "BACKGROUND",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "ProfileFallbackEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "HierarchicalStateTaxonomyEngine",
                        "EVIDENCE",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "DecisionFunnelDiagnosisEngine",
                        "DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "RealBetDiagnosisEngine",
                        "DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "ReasonEngine",
                        "DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "SelfDiagnosisEngine",
                        "DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "DiagnosisFixPlanEngine",
                        "DIAGNOSIS",
                        "AUXILIARY_EVIDENCE",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "OnlineActionModelEngine",
                        "PROBABILITY_INPUT",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "UnifiedStrategyEvidenceEngine",
                        "PROBABILITY_INPUT",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "RealtimeCausalProbabilityEngine",
                        "PROBABILITY",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "TYPED_PROBABILITY",
                        true,
                        false,
                        false,
                        5));
        x.add(
                new StageContract(
                        "MonotonicCalibrationEngine",
                        "CALIBRATION",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "TYPED_PROBABILITY",
                        true,
                        false,
                        false,
                        6));
        x.add(
                new StageContract(
                        "HierarchicalPosteriorEngine",
                        "CALIBRATION",
                        "OOS_CALIBRATION",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "ActionUtilityEngine",
                        "POLICY_INPUT",
                        "POLICY_BOUNDARY",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        3));
        x.add(
                new StageContract(
                        "RiskCoverageController",
                        "POLICY_INPUT",
                        "POLICY_BOUNDARY",
                        "PIT_FEATURE_GRAPH",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        4));
        x.add(
                new StageContract(
                        "LanePolicyCompiler",
                        "POLICY",
                        "POLICY_BOUNDARY",
                        "FROZEN_DECISION_CONTRACT",
                        "TYPED_LANE",
                        false,
                        true,
                        false,
                        5));
        x.add(
                new StageContract(
                        "DecisionGateEngine",
                        "DECISION",
                        "POLICY_BOUNDARY",
                        "FROZEN_DECISION_CONTRACT",
                        "BET_CANDIDATE_SELECTION",
                        false,
                        false,
                        true,
                        6));
        x.add(
                new StageContract(
                        "DecisionAuthorizationEngine",
                        "AUTHORIZATION",
                        "POLICY_BOUNDARY",
                        "FROZEN_DECISION_CONTRACT",
                        "EVIDENCE_PACKET",
                        false,
                        false,
                        false,
                        2));
        x.add(
                new StageContract(
                        "SettlementEngine",
                        "SETTLEMENT",
                        "AUXILIARY_EVIDENCE",
                        "FROZEN_DECISION_CONTRACT",
                        "SETTLEMENT_ONLY",
                        false,
                        false,
                        false,
                        3));
        return x;
    }

    public static StageAudit auditStagePlanR3() {
        StageAudit a = new StageAudit();
        StringBuilder raw = new StringBuilder(24000);
        List<StageContract> x = b07R3StagePlan();
        for (int i = 0; i < x.size(); i++) {
            StageContract s = x.get(i);
            a.modules++;
            if (!s.mayAffectProbability && !s.mayAffectLane && !s.mayCreateBet) a.evidenceOnly++;
            if (s.mayAffectProbability) a.probabilityOwners++;
            if (s.mayAffectLane) a.laneOwners++;
            if (s.mayCreateBet) a.betOwners++;
            if (s.timeoutMs <= 0 || s.timeoutMs > 20) a.unsafe++;
            if (s.mayCreateBet && !"DecisionGateEngine".equals(s.module)) a.unsafe++;
            if (s.mayAffectLane && !"LanePolicyCompiler".equals(s.module)) a.unsafe++;
            raw.append(s.text()).append('\n');
        }
        a.hash = shaPlanR3(raw.toString());
        return a;
    }

    public static String renderStagePlanR3() {
        StringBuilder s = new StringBuilder(26000);
        List<StageContract> x = b07R3StagePlan();
        for (int i = 0; i < x.size(); i++) s.append(x.get(i).text()).append('\n');
        s.append(auditStagePlanR3().summary());
        return s.toString();
    }

    public static void persistStagePlanR3(
            DbStore store, String runtimeStage, int count, int complete, int hard, long cost) {
        if (store == null) return;
        try {
            StageAudit a = auditStagePlanR3();
            store.setState("b07r3_orchestration_stage", safe(runtimeStage));
            store.setState("b07r3_orchestration_candidates", String.valueOf(count));
            store.setState("b07r3_orchestration_complete", String.valueOf(complete));
            store.setState("b07r3_orchestration_hard", String.valueOf(hard));
            store.setState("b07r3_orchestration_cost_ms", String.valueOf(cost));
            store.setState("b07r3_orchestration_audit", a.summary());
            store.registerModuleBuildV20(
                    "UnifiedStrategyEvidenceEngine",
                    AppConfig.B07R3_VERSION,
                    a.unsafe == 0 ? "ORCHESTRATION_OK" : "ORCHESTRATION_UNSAFE");
        } catch (Throwable ignore) {
        }
    }

    private static String shaPlanR3(String raw) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(safe(raw).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                String h = Integer.toHexString(b[i] & 255);
                if (h.length() == 1) s.append('0');
                s.append(h);
            }
            return s.toString();
        } catch (Throwable e) {
            return Integer.toHexString(safe(raw).hashCode());
        }
    }
}
