package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * V46 unique realtime probability owner.
 *
 * <p>NumberDistributionEngineV46 predicts a normalized ten-number distribution for each position.
 * K3 EventP is derived from that distribution. Global/conditional calibration produces EventP;
 * ActionLCB is separate. No OOS row is written here because final evidence and the one final
 * ranking have not happened yet.
 */
public final class RealtimeCausalProbabilityEngine {
    public static final String MODULE_VERSION = V49RealtimePipelineEngine.MODULE_VERSION;

    private final NumberDistributionEngineV46 numberEngine = new NumberDistributionEngineV46();
    private final HierarchicalPosteriorEngine posteriorEngine = new HierarchicalPosteriorEngine();
    private final MonotonicCalibrationEngine calibrationEngine = new MonotonicCalibrationEngine();
    private final ConditionalCalibrationEngine conditionalEngine =
            new ConditionalCalibrationEngine();
    private final CrossFittedNumberModelEngine crossFitEngine = new CrossFittedNumberModelEngine();
    private final PointInTimeNumberFeatureEngine pointInTimeFeatureEngine =
            new PointInTimeNumberFeatureEngine();

    public ArrayList<Models.ComboCandidate> apply(
            DbStore store, List<Models.ComboCandidate> input, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (AppConfig.ENABLE_V49_FULL_MOE) {
            String target = "";
            if (input != null && input.size() > 0 && input.get(0) != null)
                target = input.get(0).targetIssue;
            V49RealtimePipelineEngine.Result v49 =
                    new V49RealtimePipelineEngine().run(store, history, target);
            return v49 == null ? out : v49.candidates;
        }
        if (input == null) return out;
        long started = System.currentTimeMillis();
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "RealtimeCausalProbabilityEngine",
                        MODULE_VERSION,
                        "V47_CROSSFIT_EVENTP_ACTION_LCB_SEPARATED");
        } catch (Throwable ignore) {
        }

        NumberDistributionEngineV46.Snapshot numberSnapshot = numberEngine.build(history);
        PointInTimeNumberFeatureEngine.Snapshot v48Features =
                pointInTimeFeatureEngine.build(history);
        LightweightModelInferenceEngine.Snapshot v48Inference =
                LightweightModelInferenceEngine.predict(
                        v48Features, ModelArtifactManifest.current());
        WalkForwardChampionEngine.Result champion =
                WalkForwardChampionEngine.evaluate(store, history);
        RankConditionalCalibrationEngine.Snapshot scopedCalibration =
                RankConditionalCalibrationEngine.load(store);
        try {
            if (store != null)
                store.setState(
                        V47CandidateSurfaceContext.STATE_VISIBLE_HISTORY,
                        String.valueOf(v48Features.historySize));
        } catch (Throwable ignore) {
        }
        HierarchicalPosteriorEngine.Snapshot posteriorSnapshot =
                posteriorEngine.loadSnapshot(store);
        MonotonicCalibrationEngine.Snapshot calibrationSnapshot =
                calibrationEngine.loadSnapshot(store);
        ConditionalCalibrationEngine.Snapshot conditionalSnapshot = conditionalEngine.load(store);
        OnlineActionModelEngine.Snapshot onlineSnapshot = OnlineActionModelEngine.load(store);

        boolean microReady = MicroWalkForwardEngine.isReady(store);
        int microSample =
                parseInt(
                        store == null
                                ? "0"
                                : store.getState(MicroWalkForwardEngine.STATE_SAMPLE, "0"),
                        0);
        String microStatus =
                store == null
                        ? MicroWalkForwardEngine.STATUS_WATCH
                        : store.getState(
                                MicroWalkForwardEngine.STATE_STATUS,
                                MicroWalkForwardEngine.STATUS_WATCH);
        String microAsOf =
                store == null ? "" : store.getState(MicroWalkForwardEngine.STATE_LAST_TARGET, "");

        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);
            if (c == null || !c.isValidK3()) continue;

            /* V48 challenger always produces a diagnostic probability, but live weight is zero
             * until the strict single-action Champion contract is satisfied. */
            numberEngine.apply(numberSnapshot, c);
            double incumbentP =
                    validProbability(c.v46EventProbability)
                            ? c.v46EventProbability
                            : AppConfig.RANDOM_K3_BASELINE;
            double challengerP = LightweightModelInferenceEngine.eventProbability(v48Inference, c);
            c.modelP =
                    challengerP; // diagnostic challenger probability; never implicit authorization
            if (champion != null
                    && champion.champion
                    && ModelArtifactManifest.MODEL_ID.equals(champion.modelId)) {
                c.basePBeforeFusion = challengerP;
                c.rawP = challengerP;
                c.v46EventProbability = challengerP;
                c.probabilityMode = "V48_PROMOTED_SINGLE_ACTION_CHAMPION";
                c.v46ProbabilityTrace = append(c.v46ProbabilityTrace, "V48Champion=" + challengerP);
            } else {
                c.basePBeforeFusion = incumbentP;
                c.rawP = incumbentP;
                c.v46EventProbability = incumbentP;
                c.probabilityMode = "V48_CHALLENGER_ZERO_INFLUENCE";
                c.v46ProbabilityTrace =
                        append(
                                c.v46ProbabilityTrace,
                                "V48Challenger=" + challengerP + "/LiveWeight=0");
            }
            StartupContextEngine.resolve(store, c);
            double distributionP = c.v46EventProbability;
            /* V47 online residual is a background challenger only in V48. */
            double blended = distributionP;
            c.rawP = distributionP;
            c.rawRankScore = computeRawRankScore(c);
            PredictionContractEngine.stampCandidate(c, history);

            HierarchicalPosteriorEngine.Posterior posterior =
                    posteriorEngine.apply(posteriorSnapshot, c);
            MonotonicCalibrationEngine.CalibrationResult cal =
                    calibrationEngine.apply(calibrationSnapshot, c, posterior, c.modelP);
            UnifiedStrategyEvidenceEngine.finalizeProbability(c);

            c.microWalkForwardStatus = microStatus;
            c.microWalkForwardVersion = AppConfig.MICRO_WALK_FORWARD_VERSION;
            c.microWalkForwardAsOfIssue = microAsOf;
            c.microWalkForwardSample = microSample;
            c.probabilityMode =
                    champion != null && champion.champion
                            ? "V48_SINGLE_ACTION_CHAMPION_CALIBRATED"
                            : "V48_CHALLENGER_ZERO_INFLUENCE";
            c.probabilityFieldVersion = MODULE_VERSION;
            c.probabilityPipelineVersion = AppConfig.PIPELINE_VERSION;
            c.moduleBuildVersion = MODULE_VERSION;
            c.v46GlobalCalibratedProbability = c.calibratedHitProbability;
            c.v46Version = AppConfig.V48_VERSION;

            ActionUtilityEngine.Result utility = ActionUtilityEngine.evaluate(store, c);
            c.fusionSummary =
                    append(c.fusionSummary, summary(c, posterior, cal, numberSnapshot, microReady));
            c.reasonSummary = append(c.reasonSummary, c.fusionSummary);
            c.reasonSummary = append(c.reasonSummary, utility.summary);
            out.add(c);
        }

        /* Preliminary order exists only to provide a rank-slot conditional calibration.
         * It is not the final action order and is never persisted to OOS. */
        Collections.sort(out, probabilityComparator());
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            RankConditionalCalibrationEngine.apply(scopedCalibration, c);
            c.rawRankScore = computeRawRankScore(c);
        }
        Collections.sort(out, probabilityComparator());
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            c.rank = i + 1;
            c.oosRankSlot = 0; // final rank owner assigns this after final evidence
            c.oosCandidateCount = 0;
        }

        long totalCost = System.currentTimeMillis() - started;
        try {
            if (store != null) {
                store.setState("v46_realtime_probability_version", MODULE_VERSION);
                store.setState(
                        "v46_number_distribution_version",
                        NumberDistributionEngineV46.MODULE_VERSION);
                store.setState("v46_number_distribution", numberSnapshot.summary());
                store.setState(
                        "v46_conditional_calibration_version",
                        ConditionalCalibrationEngine.MODULE_VERSION);
                store.setState("v46_realtime_probability_count", String.valueOf(out.size()));
                V47CandidateSurfaceContext.probability(store, out.size());
                store.setState(
                        "v46_realtime_probability_best",
                        out.size() == 0
                                ? "EMPTY"
                                : summary(out.get(0), null, null, numberSnapshot, microReady));
                store.setState(
                        "v46_probability_policy",
                        "V48 Point-in-Time特征->真实单动作走前Champion；未晋级Challenger实时权重=0；每目标期一条Rank1条件校准");
                store.setState("v48_feature_snapshot", v48Features.summary());
                store.setState(
                        "v48_champion_runtime", champion == null ? "EMPTY" : champion.summary());
                store.setState(
                        "v48_legacy_online_residual", "BACKGROUND_DIAGNOSTIC_ZERO_LIVE_WEIGHT");
                store.setState("v46_probability_total_cost_ms", String.valueOf(totalCost));
                store.setState(
                        "probability_audit_model",
                        out.size() == 0 ? "--" : String.valueOf(out.get(0).modelP));
                store.setState(
                        "probability_audit_calibrated",
                        out.size() == 0 ? "--" : String.valueOf(out.get(0).v46EventProbability));
                store.setState(
                        "probability_audit_safe",
                        out.size() == 0 ? "--" : String.valueOf(out.get(0).v46ActionLcb));
                store.setState("probability_audit_lock_status", "FINAL_EVIDENCE_AND_RANK_PENDING");
            }
        } catch (Throwable ignore) {
        }
        ModelChampionChallengerRegistry.persist(store);
        return out;
    }

    private static Comparator<Models.ComboCandidate> probabilityComparator() {
        return new Comparator<Models.ComboCandidate>() {
            public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                int x = Double.compare(probability(b), probability(a));
                if (x != 0) return x;
                x =
                        Double.compare(
                                RankingAuditEngine.expectedValue(probability(b)),
                                RankingAuditEngine.expectedValue(probability(a)));
                if (x != 0) return x;
                x = Double.compare(b.rawRankScore, a.rawRankScore);
                if (x != 0) return x;
                x = a.position - b.position;
                if (x != 0) return x;
                return a.k3Mask - b.k3Mask;
            }
        };
    }

    public static double computeDeterministicRankScore(Models.ComboCandidate c) {
        return computeRawRankScore(c);
    }

    static double computeRawRankScore(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        double eventP = probability(c);
        double pRank =
                clamp(
                        0.50d
                                + (eventP - AppConfig.RANDOM_K3_BASELINE)
                                        / AppConfig.SEQUENCE_RANK_PROBABILITY_SCALE,
                        0.0d,
                        1.0d);
        double quality =
                0.5d
                                * clamp(
                                        max(
                                                c.coverageQuality,
                                                c.coverageQualityScore,
                                                c.dynamicGateCoverageQualityScore),
                                        0.0d,
                                        1.0d)
                        + 0.5d
                                * clamp(
                                        max(
                                                c.thirdQuality,
                                                c.thirdNumberQualityScore,
                                                c.dynamicGateThirdNumberQuality),
                                        0.0d,
                                        1.0d);
        double risk =
                clamp(
                        max(
                                c.counterRisk,
                                c.counterEvidenceScore,
                                Math.max(c.evidenceRisk, c.falseStrongRisk)),
                        0.0d,
                        1.0d);
        double structural = clamp(quality - 0.30d * risk, 0.0d, 1.0d);
        double score = 0.88d * pRank + 0.12d * structural;
        if (c.hardBlockFlag == 1 || safe(c.hardBlockSource).length() > 0) score -= 0.30d;
        return clamp(score, 0.0d, 1.0d);
    }

    private static double probability(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        if (validProbability(c.v46EventProbability)) return c.v46EventProbability;
        if (validProbability(c.calibratedHitProbability)) return c.calibratedHitProbability;
        if (validProbability(c.finalP)) return c.finalP;
        if (validProbability(c.modelP)) return c.modelP;
        return AppConfig.RANDOM_K3_BASELINE;
    }

    private String summary(
            Models.ComboCandidate c,
            HierarchicalPosteriorEngine.Posterior p,
            MonotonicCalibrationEngine.CalibrationResult cal,
            NumberDistributionEngineV46.Snapshot number,
            boolean microReady) {
        if (c == null) return "";
        return "V46事件概率：ModelP="
                + pct(c.modelP)
                + "｜GlobalCal="
                + pct(c.v46GlobalCalibratedProbability)
                + "｜EventP="
                + pct(c.v46EventProbability)
                + "｜ActionLCB="
                + pct(c.v46ActionLcb)
                + "｜EV="
                + fmt(c.v46ExpectedValue)
                + "｜Conditional="
                + signed(c.v46PositionAdjustment + c.v46RankAdjustment + c.v46RegimeAdjustment)
                + "｜Cal="
                + safe(c.calibrationStatus)
                + "(n="
                + c.calibrationSample
                + ")"
                + "｜Posterior="
                + safe(c.posteriorMatchedLevel)
                + "/"
                + safe(c.posteriorPolicy)
                + " n="
                + c.posteriorSample
                + " mean="
                + pct(c.posteriorMean)
                + "｜Micro="
                + (microReady ? "READY" : "WATCH")
                + "/"
                + c.microWalkForwardSample
                + "｜Distribution="
                + (number == null ? "--" : number.status)
                + "｜FinalRank=PENDING";
    }

    private static boolean validProbability(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static double max(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "｜" + b;
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.4f", v);
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.1f%%", 100.0d * v);
    }

    private static String signed(double v) {
        return String.format(Locale.US, "%+.2f%%", 100.0d * v);
    }
}
