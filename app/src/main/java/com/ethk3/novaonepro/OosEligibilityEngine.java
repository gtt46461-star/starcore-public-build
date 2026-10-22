package com.ethk3.novaonepro;

/**
 * V45 live-action eligibility adapter.
 *
 * <p>It verifies prediction identity and typed hard risk, asks LanePolicyCompiler for the single
 * immutable contract, and copies that exact probability/gate/edge into the candidate. It never
 * creates BET and never invents a second threshold set.
 */
public final class OosEligibilityEngine {
    public static final String MODULE_VERSION = "V46_OOS_EVENT_LCB_ADAPTER_20260721_46072130";
    public static final String STATUS_READY = "OOS_EVIDENCE_ELIGIBLE";
    public static final String STATUS_WATCH = "OOS_EVIDENCE_WATCH";

    public static final class Evaluation {
        public boolean eligible;
        public boolean qualifiedFloor;
        public boolean realProbe;
        public String lane = "SHADOW";
        public String code = "OOS_NOT_EVALUATED";
        public String primaryReject = "";
        public String secondaryReject = "";
        public String tertiaryReject = "";
        public String regimeFamily = "";
        public String riskCoverageMode = "";
        public double probability;
        public double actionLcb;
        public double probabilityUncertainty;
        public int startupSample;
        public double rawProbability;
        public double modelProbability;
        public double rankScore;
        public double gate;
        public double edge;
        public double coverage;
        public double thirdQuality;
        public double familyLcb;
        public double actionUtility;
        public double acceptScore;
        public double uncertaintyPenalty;
        public double structureScore;
        public int posteriorSample;
        public int microSample;
        public int supportVotes;
        public int qualityVotes;
        public int evidenceGroups;
        public int positiveGroups;
        public int compileCount;
        public String contractHash = "";
        public String teacherPolicy = DecisionAuthorizationEngine.POLICY_UNKNOWN;
        public String dynamicPolicy = DecisionAuthorizationEngine.POLICY_UNKNOWN;
        public String adaptivePolicy = DecisionAuthorizationEngine.POLICY_UNKNOWN;

        public String summary() {
            return "OOS="
                    + (eligible ? lane : "SHADOW")
                    + "｜code="
                    + safe(code)
                    + "｜EventP="
                    + round(probability)
                    + "｜ActionLCB="
                    + round(actionLcb)
                    + "｜ProbU="
                    + round(probabilityUncertainty)
                    + "｜Startup="
                    + startupSample
                    + "｜ModelP="
                    + round(modelProbability)
                    + "｜Utility="
                    + round(actionUtility)
                    + "｜Accept="
                    + round(acceptScore)
                    + "｜Uncertainty="
                    + round(uncertaintyPenalty)
                    + "｜Structure="
                    + round(structureScore)
                    + "｜Rank="
                    + round(rankScore)
                    + "｜Gate="
                    + round(gate)
                    + "｜Edge="
                    + round(edge)
                    + "｜LCB="
                    + round(familyLcb)
                    + "｜Votes="
                    + supportVotes
                    + "/Q="
                    + qualityVotes
                    + "｜Groups="
                    + evidenceGroups
                    + "/Positive="
                    + positiveGroups
                    + "｜Compile="
                    + compileCount
                    + "｜Contract="
                    + shortHash(contractHash)
                    + "｜Regime="
                    + safe(regimeFamily)
                    + "｜RC="
                    + safe(riskCoverageMode)
                    + "｜Reject="
                    + safe(primaryReject)
                    + "/"
                    + safe(secondaryReject)
                    + "/"
                    + safe(tertiaryReject)
                    + "｜T/D/A="
                    + teacherPolicy
                    + "/"
                    + dynamicPolicy
                    + "/"
                    + adaptivePolicy;
        }
    }

    private OosEligibilityEngine() {}

    public static Evaluation applyAndEvaluate(DbStore store, Models.ComboCandidate c) {
        Evaluation out = new Evaluation();
        if (c == null) return reject(out, "OOS_CANDIDATE_NULL");
        if (!c.isValidK3()) return reject(out, "OOS_ILLEGAL_K3");
        if (c.contractValid != 1
                || !PredictionContractEngine.STATUS_VALID.equals(
                        PredictionContractEngine.verifyCandidate(c)))
            return reject(out, "OOS_CONTRACT_MISMATCH");
        if (!AppConfig.BUILD_ID.equals(c.buildId)
                || !AppConfig.COHORT_ID.equals(c.cohortId)
                || !AppConfig.SCHEMA_VERSION.equals(c.schemaVersion)
                || !AppConfig.PIPELINE_VERSION.equals(c.pipelineVersion)
                || !AppConfig.CALIBRATION_VERSION.equals(c.calibrationVersion))
            return reject(out, "OOS_VERSION_MISMATCH");
        if (c.hardBlockFlag == 1 || safe(c.hardBlockSource).length() > 0)
            return reject(out, "OOS_STRUCTURED_HARD_BLOCK");

        out.teacherPolicy =
                groupPolicy(
                        new String[] {
                            c.teacherOrdinaryBetPolicy,
                            c.teacherMatrixOrdinaryBetPolicy,
                            c.teacherMatrixStatus,
                            c.teacherStatus,
                            c.teacherNumberStatus
                        });
        out.dynamicPolicy =
                groupPolicy(
                        new String[] {
                            c.dynamicClusterStatus,
                            c.v20Status,
                            c.dynamicStatus,
                            c.dynamicCauseStatus,
                            c.finalDecisionV20Policy
                        });
        out.adaptivePolicy =
                groupPolicy(
                        new String[] {
                            c.adaptiveJudgeAction,
                            c.adaptiveStatus,
                            c.adaptiveJudgeStatus,
                            c.ruleHealthStatus,
                            c.shadowPromotionStatus
                        });
        if (DecisionAuthorizationEngine.POLICY_HARD_RISK.equals(out.teacherPolicy))
            return reject(out, "OOS_TEACHER_HARD_RISK");
        if (DecisionAuthorizationEngine.POLICY_HARD_RISK.equals(out.dynamicPolicy))
            return reject(out, "OOS_DYNAMIC_HARD_RISK");
        if (DecisionAuthorizationEngine.POLICY_HARD_RISK.equals(out.adaptivePolicy))
            return reject(out, "OOS_ADAPTIVE_HARD_RISK");

        ActionUtilityEngine.Result utility = ActionUtilityEngine.evaluate(store, c);
        out.modelProbability = c.modelP > 0.0d ? c.modelP : c.rawP;
        out.rawProbability = c.rawP > 0.0d ? c.rawP : out.modelProbability;
        out.rankScore = c.rawRankScore;
        out.coverage =
                max3(c.coverageQuality, c.coverageQualityScore, c.dynamicGateCoverageQualityScore);
        out.thirdQuality =
                max3(c.thirdQuality, c.thirdNumberQualityScore, c.dynamicGateThirdNumberQuality);
        out.actionUtility = utility.utility;
        out.uncertaintyPenalty = utility.uncertaintyPenalty;
        out.structureScore = utility.structureScore;
        out.posteriorSample = c.posteriorSample;
        out.microSample =
                Math.max(
                        c.microWalkForwardSample,
                        parseInt(
                                store == null
                                        ? "0"
                                        : store.getState(MicroWalkForwardEngine.STATE_SAMPLE, "0"),
                                0));
        if (out.coverage > 0.0d) c.coverageAvailable = 1;
        if (out.thirdQuality > 0.0d) c.thirdQualityAvailable = 1;

        LanePolicyCompiler.Result policy = LanePolicyCompiler.compile(store, c, utility);
        copyPolicy(out, policy);
        copyPolicyToCandidate(c, policy);
        if (policy == null || !policy.eligible) {
            PredictionLifecycleTraceEngine.record(
                    store,
                    c,
                    "OOS_ELIGIBILITY_REJECT",
                    policy == null ? "COMPILER_NULL" : policy.code);
            return reject(out, policy == null ? "OOS_LANE_COMPILER_NULL" : policy.code);
        }

        c.laneCandidate = policy.lane;
        applyEffectiveGate(c, out, policy.probability, policy.gate);
        out.eligible = true;
        out.lane = policy.lane;
        out.qualifiedFloor = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR.equals(policy.lane);
        out.realProbe = AppConfig.BET_DOMAIN_REAL_BET_PROBE.equals(policy.lane);
        out.code = "OOS_V46_" + policy.lane + "_READY";
        PredictionLifecycleTraceEngine.record(store, c, "OOS_ELIGIBILITY_READY", out.summary());
        try {
            if (store != null) {
                store.setState("v46_oos_eligibility_version", MODULE_VERSION);
                store.setState("v46_oos_last_eligible", out.summary());
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static void copyPolicy(Evaluation out, LanePolicyCompiler.Result policy) {
        if (out == null || policy == null) return;
        out.probability = policy.probability;
        out.actionLcb = policy.actionLcb;
        out.probabilityUncertainty = policy.uncertainty;
        out.startupSample = policy.startupSample;
        out.familyLcb = policy.familyLcb;
        out.acceptScore = policy.acceptScore;
        out.supportVotes = policy.supportVotes;
        out.qualityVotes = policy.qualityVotes;
        out.evidenceGroups = policy.evidenceGroups;
        out.positiveGroups = policy.positiveGroups;
        out.compileCount = policy.compileCount;
        out.contractHash = safe(policy.contractHash);
        out.gate = policy.gate;
        out.edge = policy.edge;
        out.primaryReject = safe(policy.primaryReject);
        out.secondaryReject = safe(policy.secondaryReject);
        out.tertiaryReject = safe(policy.tertiaryReject);
        out.regimeFamily = policy.regime == null ? "" : safe(policy.regime.family);
        out.riskCoverageMode = policy.riskCoverage == null ? "" : safe(policy.riskCoverage.mode);
    }

    private static void copyPolicyToCandidate(
            Models.ComboCandidate c, LanePolicyCompiler.Result policy) {
        if (c == null || policy == null) return;
        c.v42AcceptScore = policy.acceptScore;
        c.v42FamilyLcb = policy.familyLcb;
        c.v42RegimeQuality = policy.regime == null ? 0.0d : policy.regime.quality;
        c.v42AssociationScore = policy.association == null ? 0.0d : policy.association.score;
        c.v42DistillationRisk =
                policy.distillation == null ? 0.0d : policy.distillation.falseStrongRisk;
        c.v42SupportVotes = policy.supportVotes;
        c.v42RegimeFamily = policy.regime == null ? "" : safe(policy.regime.family);
        c.v42RiskCoverageMode = policy.riskCoverage == null ? "" : safe(policy.riskCoverage.mode);
        c.v42PrimaryReject = safe(policy.primaryReject);
        c.v42SecondaryReject = safe(policy.secondaryReject);
        c.v42TertiaryReject = safe(policy.tertiaryReject);
        c.v42LanePolicyVersion = LanePolicyCompiler.MODULE_VERSION;
        c.reasonSummary =
                append(
                        c.reasonSummary,
                        "V46合同证据组="
                                + policy.evidenceGroups
                                + "/正向="
                                + policy.positiveGroups
                                + "；compile="
                                + policy.compileCount
                                + "；hash="
                                + shortHash(policy.contractHash));
    }

    public static void applyMaturityBridge(DbStore store, Models.ComboCandidate c) {
        if (c == null) return;
        c.reasonSummary =
                append(
                        c.reasonSummary,
                        "V46_POLICY：30期仅开启受控探针；EventP与ActionLCB分离；60候选完成证据后最终40候选只重排一次；授权只签合同；频率不得制造概率");
    }

    public static boolean matureEvidence(DbStore store, Models.ComboCandidate c) {
        if (c == null) return false;
        int history = Math.max(c.evidenceForestSample, c.historicalPriorSample);
        int votes = 0;
        if (history >= AppConfig.V42_NORMAL_MIN_SEQUENCE) votes++;
        if (c.calibrationSample >= AppConfig.V42_NORMAL_MIN_CALIBRATION
                || MonotonicCalibrationEngine.isReadyStatus(c.calibrationStatus)) votes++;
        if (c.posteriorSample >= AppConfig.V42_NORMAL_MIN_POSTERIOR) votes++;
        if (c.microWalkForwardSample >= AppConfig.V42_FLOOR_MIN_MICRO) votes++;
        return votes >= 3;
    }

    public static String groupPolicy(String[] values) {
        boolean any = false, ready = false, strong = false, soft = false;
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (safe(values[i]).length() == 0) continue;
                any = true;
                String p = DecisionAuthorizationEngine.resolvePolicy(values[i]);
                if (DecisionAuthorizationEngine.POLICY_HARD_RISK.equals(p)) return p;
                if (DecisionAuthorizationEngine.POLICY_SOFT_RISK.equals(p)) soft = true;
                if (DecisionAuthorizationEngine.POLICY_STRONG.equals(p)) strong = true;
                if (DecisionAuthorizationEngine.POLICY_READY.equals(p)) ready = true;
            }
        if (strong) return DecisionAuthorizationEngine.POLICY_STRONG;
        if (ready) return DecisionAuthorizationEngine.POLICY_READY;
        if (soft) return DecisionAuthorizationEngine.POLICY_SOFT_RISK;
        return any
                ? DecisionAuthorizationEngine.POLICY_WATCH
                : DecisionAuthorizationEngine.POLICY_UNKNOWN;
    }

    private static void applyEffectiveGate(
            Models.ComboCandidate c, Evaluation e, double probability, double gate) {
        double cleanP = clamp01(probability);
        double cleanGate = clamp01(gate);
        c.v46EventProbability = cleanP;
        c.v46ActionLcb = e.actionLcb;
        c.v46ProbabilityUncertainty = e.probabilityUncertainty;
        c.finalP = cleanP;
        c.calibratedP = cleanP;
        c.calibratedHitProbability = cleanP;
        c.honestProbability = cleanP;
        c.v43SafeProbability = cleanP;
        c.dynamicGate = cleanGate;
        c.riskAdjustedGate = cleanGate;
        c.gate = cleanGate;
        c.edge = cleanP - cleanGate;
        e.probability = cleanP;
        e.gate = cleanGate;
        e.edge = c.edge;
    }

    private static Evaluation reject(Evaluation e, String code) {
        e.eligible = false;
        e.lane = "SHADOW";
        e.code = safe(code);
        return e;
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static double max3(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "｜" + b;
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String shortHash(String s) {
        return safe(s).length() <= 12 ? safe(s) : s.substring(0, 12);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
