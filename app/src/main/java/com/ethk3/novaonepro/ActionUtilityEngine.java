package com.ethk3.novaonepro;

/**
 * V43 full-strategy feature-to-utility projection.
 *
 * <p>Probability remains an event probability. Utility is only an ordering feature used by
 * LanePolicyCompiler. This class does not classify lanes, does not create BET and does not inflate
 * probability to repay frequency debt.
 */
public final class ActionUtilityEngine {
    public static final String V46_ROLE = "ACTION_UTILITY_EVIDENCE_NOT_EVENT_PROBABILITY";

    public static final String MODULE_VERSION = AppConfig.V46_VERSION;

    public static final class Result {
        public double honestProbability;
        public double probabilitySignal;
        public double structureScore;
        public double discriminationScore;
        public double maturityScore;
        public double uncertaintyPenalty;
        public double frequencySupport;
        public double regimeScore;
        public double associationScore;
        public double distillationRisk;
        public double utility;
        public String laneCandidate = "COMPILE_REQUIRED";
        public String summary = "";
    }

    private ActionUtilityEngine() {}

    public static Result evaluate(DbStore store, Models.ComboCandidate c) {
        Result r = new Result();
        if (c == null) return r;

        r.honestProbability = probability(c);
        r.probabilitySignal = clamp01((r.honestProbability - 0.285d) / 0.115d);

        RegimeRegistryEngine.Result regime = RegimeRegistryEngine.evaluate(c);
        AssociationGraphEngine.Result association = AssociationGraphEngine.evaluate(c);
        CounterfactualDistillationEngine.Result distillation =
                CounterfactualDistillationEngine.evaluate(c);
        r.regimeScore = regime.quality;
        r.associationScore = association.score;
        r.distillationRisk = distillation.falseStrongRisk;

        double coverage =
                clamp01(
                        max(
                                c.coverageQuality,
                                c.coverageQualityScore,
                                c.dynamicGateCoverageQualityScore));
        double third =
                clamp01(
                        max(
                                c.thirdQuality,
                                c.thirdNumberQualityScore,
                                c.dynamicGateThirdNumberQuality));
        double balance = clamp01(max(c.balanceScore, c.comboRoleDiversity, c.weakestNumberScore));
        double support =
                normalizedEvidence(
                        max(c.evidenceScore, c.numberSupplyScore, c.needSupplyMatchScore));
        double agreement =
                clamp01(
                        max(
                                c.evidenceForestConfidence,
                                c.historicalPriorConfidence,
                                1.0d - normalizedRisk(c.deepFeatureRisk)));
        double v43Support = clamp01(c.v43StrategySupport);
        double v43Confidence = clamp01(c.v43StrategyConfidence);
        double v43Diversity = clamp01(c.v43StrategyDiversity);
        double v43Risk = clamp01(c.v43StrategyRisk);
        double v43Fragility = clamp01(c.v43AblationFragility);
        r.structureScore =
                clamp01(
                        0.14d * coverage
                                + 0.11d * third
                                + 0.09d * balance
                                + 0.10d * support
                                + 0.08d * agreement
                                + 0.12d * association.score
                                + 0.10d * regime.quality
                                + 0.16d * v43Support
                                + 0.10d * v43Diversity);

        double rank = clamp01(c.rawRankScore);
        double model = probabilityModel(c);
        double modelLift = clamp01((model - 0.285d) / 0.115d);
        double championGap = clamp01(max(c.championGap, c.comboGap, c.positionNeedScore));
        double posteriorLift =
                clamp01(
                        (max(c.posteriorMean, c.posteriorRecentRate, c.familyWilsonLower) - 0.275d)
                                / 0.145d);
        r.discriminationScore =
                clamp01(
                        0.30d * rank
                                + 0.20d * modelLift
                                + 0.09d * championGap
                                + 0.12d * posteriorLift
                                + 0.09d * association.score
                                + 0.06d * regime.quality
                                + 0.08d * v43Support
                                + 0.06d * v43Confidence);

        int micro =
                Math.max(
                        c.microWalkForwardSample,
                        stateInt(store, MicroWalkForwardEngine.STATE_SAMPLE, 0));
        int calLocal = Math.max(c.calibrationSample, 0);
        int posterior = Math.max(0, c.posteriorSample);
        int sequenceHistory = Math.max(c.evidenceForestSample, c.historicalPriorSample);
        double microMaturity =
                clamp01(micro / (double) Math.max(1, AppConfig.MICRO_WALK_FORWARD_READY_SAMPLE));
        double calMaturity =
                clamp01(calLocal / (double) Math.max(1, AppConfig.V42_NORMAL_MIN_CALIBRATION));
        double posteriorMaturity =
                clamp01(posterior / (double) Math.max(1, AppConfig.V42_NORMAL_MIN_POSTERIOR));
        double sequenceMaturity =
                clamp01(sequenceHistory / (double) Math.max(1, AppConfig.V42_NORMAL_MIN_SEQUENCE));
        r.maturityScore =
                clamp01(
                        0.18d * microMaturity
                                + 0.19d * calMaturity
                                + 0.21d * posteriorMaturity
                                + 0.24d * sequenceMaturity
                                + 0.18d * clamp01(c.v43StrategyMaturity));

        RiskCoverageController.State rc = RiskCoverageController.evaluate(store);
        Models.FrequencyState fs = rc == null ? null : rc.frequency;
        double debt =
                fs == null
                        ? clamp01(c.frequencyDebtScore / 0.20d)
                        : clamp01(fs.frequencyDebt / 0.20d);
        double pacing =
                fs == null
                        ? 0.0d
                        : clamp01(
                                fs.issuesSinceLastRealBet
                                        / (double) Math.max(1, AppConfig.V42_PROBE_MIN_GAP * 3));
        r.frequencySupport = clamp01(0.60d * debt + 0.40d * pacing);

        /* Maturity is not punished twice.  It is a modest uncertainty component;
         * exact sample requirements are compiled once by LanePolicyCompiler. */
        double sampleUncertainty = 1.0d - r.maturityScore;
        double calibrationDisagreement = clamp01(Math.abs(model - r.honestProbability) / 0.10d);
        double componentDisagreement = clamp01(Math.max(0.0d, c.calibrationError) / 0.12d);
        double drift = normalizedRisk(max(c.posteriorDrift, c.driftRisk, c.deepFeatureRisk));
        double counter = normalizedRisk(max(c.counterRisk, c.counterEvidenceScore, c.evidenceRisk));
        double conflict = normalizedRisk(max(c.comboConflict, c.conflictLevel, c.falseStrongRisk));
        double missing = 0.0d;
        if (!(c.coverageAvailable == 1 || coverage > 0.000001d)) missing += 0.05d;
        if (!(c.thirdQualityAvailable == 1 || third > 0.000001d)) missing += 0.05d;
        r.uncertaintyPenalty =
                clamp01(
                        0.10d * sampleUncertainty
                                + 0.12d * calibrationDisagreement
                                + 0.10d * componentDisagreement
                                + 0.11d * drift
                                + 0.10d * counter
                                + 0.07d * conflict
                                + 0.10d * distillation.falseStrongRisk
                                + 0.14d * v43Risk
                                + 0.10d * v43Fragility
                                + 0.06d * (1.0d - v43Confidence)
                                + missing);

        /* Frequency is intentionally absent from utility.  It is consumed only by
         * RiskCoverageController as an acceptance-threshold pacing constraint. */
        r.utility =
                clamp01(
                        0.21d * r.probabilitySignal
                                + 0.18d * r.structureScore
                                + 0.16d * r.discriminationScore
                                + 0.08d * r.maturityScore
                                + 0.10d * r.regimeScore
                                + 0.08d * r.associationScore
                                + 0.12d * v43Support
                                + 0.07d * v43Confidence
                                - 0.18d * r.uncertaintyPenalty
                                - 0.07d * r.distillationRisk
                                - 0.06d * v43Fragility);

        if (c.hardBlockFlag == 1
                || safe(c.hardBlockSource).length() > 0
                || regime.hardChaos
                || distillation.hardRisk
                || c.v43HardRisk == 1) {
            r.utility = 0.0d;
        }

        c.honestProbability = r.honestProbability;
        c.actionUtility = r.utility;
        c.actionStructureScore = r.structureScore;
        c.actionDiscriminationScore = r.discriminationScore;
        c.actionMaturityScore = r.maturityScore;
        c.actionUncertaintyPenalty = r.uncertaintyPenalty;
        c.actionFrequencySupport = r.frequencySupport;
        c.actionUtilityVersion = MODULE_VERSION;
        c.laneCandidate = r.laneCandidate;
        r.summary =
                "V43全策略统一行动特征：HonestP="
                        + round(r.honestProbability)
                        + "｜Utility="
                        + round(r.utility)
                        + "｜Structure="
                        + round(r.structureScore)
                        + "｜Discrimination="
                        + round(r.discriminationScore)
                        + "｜Maturity="
                        + round(r.maturityScore)
                        + "｜Regime="
                        + round(r.regimeScore)
                        + "｜Association="
                        + round(r.associationScore)
                        + "｜DistillRisk="
                        + round(r.distillationRisk)
                        + "｜Uncertainty="
                        + round(r.uncertaintyPenalty)
                        + "｜V43Support="
                        + round(v43Support)
                        + "｜V43Risk="
                        + round(v43Risk)
                        + "｜V43Confidence="
                        + round(v43Confidence)
                        + "｜V43Fragility="
                        + round(v43Fragility)
                        + "｜纪律=频率不进入概率或效用，只由风险覆盖率控制器管理";
        try {
            if (store != null) {
                store.setState("v46_action_utility_version", MODULE_VERSION);
                store.setState("v43_action_utility_last", r.summary);
            }
        } catch (Throwable ignore) {
        }
        return r;
    }

    public static Result evaluateDecision(DbStore store, Models.Decision d) {
        Result r = new Result();
        if (d == null) return r;
        r.honestProbability =
                d.calibratedHitProbability > 0.0d ? d.calibratedHitProbability : d.finalP;
        r.utility = clamp01(d.actionUtility);
        r.structureScore = clamp01(d.actionStructureScore);
        r.discriminationScore = clamp01(d.actionDiscriminationScore);
        r.maturityScore = clamp01(d.actionMaturityScore);
        r.uncertaintyPenalty = clamp01(d.actionUncertaintyPenalty);
        r.frequencySupport = clamp01(d.actionFrequencySupport);
        r.laneCandidate = safe(d.laneCandidate).length() == 0 ? "SHADOW" : d.laneCandidate;
        return r;
    }

    private static double probability(Models.ComboCandidate c) {
        double p =
                c.calibratedHitProbability > 0.0d
                        ? c.calibratedHitProbability
                        : (c.finalP > 0.0d ? c.finalP : probabilityModel(c));
        return clamp(p, 0.0d, 1.0d);
    }

    private static double probabilityModel(Models.ComboCandidate c) {
        double p =
                c.modelP > 0.0d
                        ? c.modelP
                        : (c.rawP > 0.0d ? c.rawP : AppConfig.RANDOM_K3_BASELINE);
        return clamp(p, 0.0d, 1.0d);
    }

    private static double normalizedEvidence(double v) {
        return v > 1.0d ? clamp01(v / 100.0d) : clamp01(v);
    }

    private static double normalizedRisk(double v) {
        return v > 1.0d ? clamp01(v / 100.0d) : clamp01(v);
    }

    private static int stateInt(DbStore store, String key, int def) {
        try {
            return store == null ? def : Integer.parseInt(store.getState(key, String.valueOf(def)));
        } catch (Throwable e) {
            return def;
        }
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static double max(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
