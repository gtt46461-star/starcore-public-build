package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * ProbabilityEngine
 *
 * <p>V20 动态因链概率主链满血版（第7/11升级优化随机版）
 *
 * <p>固定目标： 1. 保留实时快速链路：前120候选基础精算，前40候选深算。 2. 保留 TeacherMatrix / RuntimeProfile /
 * StateNumberProfile / Learning 对概率和门槛的小幅反哺。 3. 新增接入 V20 动态因链字段： DynamicCauseChain / RuleHealth /
 * ShadowHealth / FalseStrongFreeze / SeedCause / ThirdGuard / CounterfactualRank / PositionCause /
 * TeacherNumber。 4. V20新增字段只做概率、门槛、排序、解释的小幅证据修正，不创建BET。 5. 不创建 Trade，不结算，不写真实校准。 6. 不绕过 DynamicGate
 * / DecisionGate。 7. 不继承旧脏门槛，不把 CHAOS 当硬坏状态。 8. 所有新增字段均反射安全读取，避免 AIDE 编译炸。 9. enrichAllWithMode 必须
 * public，保证 WalkForwardBacktestEngine 反射直连。 10. 第12/14升级：新增诊断调整概率链，输出 FinalP
 * 诊断前后、模块调整、冻结调整、SHADOW晋级加权。 11. FIX17：Teacher NO_SAMPLE / V20 WATCH / Adaptive WATCH
 * 只做中性观察；RISK/FREEZE/强反证才折扣或冻结。 12. 红线：只计算概率/门槛/排序/解释字段，不创建BET、不创建Trade、不结算、不保存Decision。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class ProbabilityEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final String TAG = "ProbabilityEngine";

    private static final double MIN_REAL_BET_P = 0.345d;
    private static final double BASE_NUMBER_RATE = 0.10d;
    private static final double BASE_K3_RATE = AppConfig.RANDOM_K3_BASELINE;
    private static final double RAW_HARD_FLOOR = 0.300d;

    private static final double RAW_BET_FLOOR_STRONG = 0.340d;
    private static final double RAW_BET_FLOOR_NORMAL = 0.345d;
    private static final double RAW_BET_FLOOR_ELITE = 0.355d;
    private static final double RAW_BET_FLOOR = RAW_BET_FLOOR_NORMAL;

    private static final double MAX_FINAL_ABOVE_RAW = 0.018d;
    private static final double MAX_FINAL_BELOW_RAW = 0.020d;

    private static final double STALE_DYNAMIC_GATE_HIGH = 0.418d;

    private static final int RECENT_STATE_WINDOW_SMALL = 16;
    private static final int RECENT_STATE_WINDOW_MID = 32;
    private static final int RECENT_STATE_WINDOW_BIG = 64;
    private static final int RECENT_STATE_WINDOW_LONG = 128;

    private static final int REALTIME_ENRICH_HARD_LIMIT =
            AppConfig.REALTIME_PROBABILITY_AUDIT_LIMIT;
    private static final int REALTIME_DEEP_HARD_LIMIT = AppConfig.PROBABILITY_BATCH_DEEP_LIMIT;
    private static final int REALTIME_MIN_OUTPUT_BEFORE_BUDGET =
            AppConfig.REALTIME_PROBABILITY_MIN_FULL_COUNT;
    private static final long REALTIME_PROBABILITY_BUDGET_MS =
            AppConfig.PROBABILITY_BATCH_SOFT_BUDGET_MS;
    private static final long REALTIME_PROBABILITY_HARD_BUDGET_MS =
            AppConfig.PROBABILITY_BATCH_HARD_BUDGET_MS;

    private static final int TEACHER_MIN_SAMPLE = 30;
    private static final int TEACHER_STRONG_SAMPLE = 80;
    private static final int TEACHER_COMBO_MIN_SAMPLE = 12;
    private static final int TEACHER_COMBO_STRONG_SAMPLE = 28;

    private static final double MAX_TEACHER_PROB_BOOST = 0.035d;
    private static final double MAX_TEACHER_PROB_PENALTY = 0.045d;
    private static final double MAX_TEACHER_GATE_REDUCE = 0.024d;
    private static final double MAX_TEACHER_GATE_BOOST = 0.040d;

    private static final double MAX_RUNTIME_PROB_BOOST = 0.018d;
    private static final double MAX_RUNTIME_PROB_PENALTY = 0.024d;
    private static final double MAX_RUNTIME_GATE_REDUCE = 0.014d;
    private static final double MAX_RUNTIME_GATE_BOOST = 0.022d;

    private static final double MAX_STATE_NUMBER_PROB_BOOST = 0.016d;
    private static final double MAX_STATE_NUMBER_PROB_PENALTY = 0.022d;
    private static final double MAX_STATE_NUMBER_GATE_REDUCE = 0.012d;
    private static final double MAX_STATE_NUMBER_GATE_BOOST = 0.020d;

    private static final double MAX_V20_DYNAMIC_PROB_BOOST = 0.018d;
    private static final double MAX_V20_DYNAMIC_PROB_PENALTY = 0.026d;
    private static final double MAX_V20_DYNAMIC_GATE_REDUCE = 0.010d;
    private static final double MAX_V20_DYNAMIC_GATE_BOOST = 0.024d;
    private static final int V20_WINDOW_12 = 12;
    private static final int V20_WINDOW_24 = 24;
    private static final int V20_WINDOW_48 = 48;
    private static final int V20_WINDOW_96 = 96;
    private static final int V20_WINDOW_360 = 360;
    private static final double MAX_V20_WINDOW_PROB_BOOST = 0.010d;
    private static final double MAX_V20_WINDOW_PROB_PENALTY = 0.014d;
    private static final double MAX_V20_WINDOW_GATE_REDUCE = 0.006d;
    private static final double MAX_V20_WINDOW_GATE_BOOST = 0.012d;
    private static final double MAX_DIAGNOSIS_PROB_DISCOUNT = 0.036d;
    private static final double MAX_DIAGNOSIS_PROB_BOOST = 0.010d;
    private static final double MAX_MODULE_MISLEADING_DISCOUNT = 0.014d;
    private static final double MAX_FREEZE_PROB_DISCOUNT = 0.020d;
    private static final double MAX_SHADOW_PROMOTION_BOOST = 0.004d;
    private static final double DIAGNOSIS_MIN_SAMPLE_RELIABILITY = 0.26d;
    private static final double DIAGNOSIS_MIN_RECENT_VALIDITY = 0.28d;
    private static final String V20_PROBABILITY_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_PROBABILITY_UPSTREAM_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String V20_PROBABILITY_MAIN_CHAIN_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String LINK_MODIFICATION_V2_PROBABILITY_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final HashMap<String, Field> REFLECT_FIELD_CACHE = new HashMap<String, Field>();
    private static final HashSet<String> REFLECT_FIELD_MISS_CACHE = new HashSet<String>();
    private static final HashMap<String, Method> REFLECT_METHOD_CACHE =
            new HashMap<String, Method>();
    private static final HashSet<String> REFLECT_METHOD_MISS_CACHE = new HashSet<String>();
    private static final Object NULL_STORE_RESULT = new Object();
    private final HashMap<String, Object> runStoreResultCache = new HashMap<String, Object>();
    private boolean runStoreCacheActive;
    private int runStoreCacheHit;
    private int runStoreCacheMiss;
    private static final double MAIN_CHAIN_MAX_PROB_BOOST = 0.008d;
    private static final double MAIN_CHAIN_MAX_PROB_DISCOUNT = 0.024d;
    private static final double MAIN_CHAIN_NEUTRAL_WATCH_DISCOUNT =
            AppConfig.PROBABILITY_NEUTRAL_WATCH_DISCOUNT;
    private static final double MAIN_CHAIN_HARD_RISK_DISCOUNT = 0.018d;
    private static final String STATUS_NO_SAMPLE_NEUTRAL = "NO_SAMPLE_NEUTRAL";
    private static final double COVERAGE_MIN_NONEMPTY_K3_QUALITY = 0.180d;
    private static final double COVERAGE_MIN_NONEMPTY_THIRD_QUALITY = 0.055d;
    private static final double COVERAGE_SAMPLE_RISK_LINE = 0.650d;
    private static final double MAX_COVERAGE_PROB_DISCOUNT = 0.026d;
    private static final double MAX_COVERAGE_PROB_BOOST = 0.006d;
    private static final double COVERAGE_QUALITY_READY = 0.360d;
    private static final double COVERAGE_QUALITY_STRONG = 0.405d;
    private static final double THIRD_NUMBER_QUALITY_MIN = 0.088d;
    private static final double THIRD_NUMBER_QUALITY_READY = 0.108d;
    private static final double K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double PREFERRED_P_MIN = 0.380d;
    private static final double PREFERRED_P_MAX = 0.400d;
    private static final double PREFERRED_EDGE_MIN = 0.010d;
    private static final double PREFERRED_EDGE_MAX = 0.020d;

    private final SimilarShapeEngine similarEngine;
    private final ShapeMemoryEngine memoryEngine;
    private final BreakRecoveryEngine recoveryEngine;
    private final ShapeStateEngine stateEngine;

    public ProbabilityEngine() {
        similarEngine = new SimilarShapeEngine();
        memoryEngine = new ShapeMemoryEngine();
        recoveryEngine = new BreakRecoveryEngine();
        stateEngine = new ShapeStateEngine();
    }

    /** AutoFetchService 第4/6实时反射入口。 只计算概率、覆盖质量、第三号质量、K3覆盖风险和解释字段；不创建BET、不保存Decision、不结算。 */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return enrichUpstreamProbabilityCandidates(
                store, candidates, history, baseIssue, targetIssue, "applyToAll");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return enrichUpstreamProbabilityCandidates(
                store, candidates, history, baseIssue, targetIssue, "enrichCandidates");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return enrichUpstreamProbabilityCandidates(
                store, candidates, history, baseIssue, targetIssue, "analyzeCandidates");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return enrichUpstreamProbabilityCandidates(
                null, candidates, history, baseIssue, targetIssue, "applyRealtime");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return enrichUpstreamProbabilityCandidates(
                null, candidates, history, baseIssue, targetIssue, "runRealtime");
    }

    public ArrayList<Models.ComboCandidate> enrichUpstreamProbabilityCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue,
            String entryName) {
        ArrayList<Models.ComboCandidate> input =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        ArrayList<Models.ComboCandidate> out =
                enrichAllWithMode(
                        store,
                        input,
                        history,
                        new ArrayList<TrendStateMachine.TrendTrace>(),
                        new ArrayList<BreakRecoveryEngine.RecoveryResult>(),
                        AppConfig.BACKTEST_MODE_FAST,
                        normalizeEnrichLimit(
                                AppConfig.BACKTEST_MODE_FAST,
                                readAppInt(
                                        "PROB_REALTIME_ENRICH_LIMIT", REALTIME_ENRICH_HARD_LIMIT)),
                        normalizeDeepLimit(
                                AppConfig.BACKTEST_MODE_FAST,
                                readAppInt("PROB_REALTIME_DEEP_LIMIT", REALTIME_DEEP_HARD_LIMIT)),
                        true,
                        true);
        if (out == null) {
            out = new ArrayList<Models.ComboCandidate>();
        }
        int total = 0;
        int coverOk = 0;
        int thirdOk = 0;
        int riskCount = 0;
        int falseHigh = 0;
        String firstSummary = "";
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) {
                continue;
            }
            total++;
            if (baseIssue != null && baseIssue.length() > 0) c.baseIssue = baseIssue;
            if (targetIssue != null && targetIssue.length() > 0) c.targetIssue = targetIssue;
            ensureCoverageFieldsNeverEmpty(c, history, entryName);
            if (readDouble(c, "coverageQualityScore", 0.0d) > 0.0d) coverOk++;
            if (readDouble(c, "thirdNumberQualityScore", 0.0d) > 0.0d) thirdOk++;
            if (readDouble(c, "k3CoverageRisk", 0.0d) >= K3_COVERAGE_RISK_LINE) riskCount++;
            if (readInt(c, "falseHighPFlag", 0) == 1
                    || readInt(c, "probabilityFalseHighPFlag", 0) == 1) falseHigh++;
            if (firstSummary.length() == 0) {
                firstSummary = buildCoverageRuntimeSummary(c);
            }
        }
        if (store != null) {
            try {
                store.setState("probability_upstream_version", V20_PROBABILITY_UPSTREAM_VERSION);
                store.setState("probability_upstream_entry", safeText(entryName));
                store.setState("probability_upstream_total", String.valueOf(total));
                store.setState("probability_upstream_cover_ok", String.valueOf(coverOk));
                store.setState("probability_upstream_third_ok", String.valueOf(thirdOk));
                store.setState("probability_upstream_risk", String.valueOf(riskCount));
                store.setState("probability_upstream_false_high", String.valueOf(falseHigh));
                store.setState("probability_upstream_best", firstSummary);
            } catch (Throwable e) {
            }
        }
        return out;
    }

    public Models.ProbabilityResult calculate(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult) {
        Models.ModelWeight weight = loadWeightOnce(store);

        return calculateInternal(
                store,
                candidate,
                history,
                trendTrace,
                recoveryResult,
                weight,
                true,
                true,
                AppConfig.BACKTEST_MODE_FAST);
    }

    public Models.ProbabilityResult calculateWithMode(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            String mode) {
        Models.ModelWeight weight = loadWeightOnce(store);

        return calculateInternal(
                store, candidate, history, trendTrace, recoveryResult, weight, true, true, mode);
    }

    private Models.ProbabilityResult calculateInternal(
            DbStore store,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.ModelWeight weight,
            boolean deepEvaluate,
            boolean useStoreCalibration,
            String mode) {
        Models.ProbabilityResult result = new Models.ProbabilityResult();

        mode = AppConfig.normalizeBacktestMode(mode);
        result.mode = mode;
        result.enrichLimit = normalizeEnrichLimit(mode, AppConfig.getProbabilityEnrichLimit(mode));
        result.deepLimit = normalizeDeepLimit(mode, AppConfig.getProbabilityDeepLimit(mode));

        if (candidate == null || !candidate.isValidK3()) {
            result.rawP = 0.0d;
            result.calibratedP = 0.0d;
            result.finalP = 0.0d;
            result.dynamicGate = AppConfig.HIGH_ACCURACY_GATE;
            result.calibrationError = 1.0d;
            result.bucketKey = "INVALID";
            return result;
        }

        normalizeCandidateBeforeProbability(candidate);

        if (weight == null) {
            weight = new Models.ModelWeight();
        }

        try {
            weight.normalizePositive();
        } catch (Throwable e) {
        }

        Models.SimilarResult similar = null;
        ShapeMemoryEngine.MemorySignal memory = null;

        if (deepEvaluate) {
            similar = safeSimilarEvaluate(history, candidate);
            memory = safeMemoryEvaluate(candidate, history);
        }

        BreakRecoveryEngine.RecoveryResult recovery = recoveryResult;

        if (recovery == null && deepEvaluate) {
            try {
                recovery = recoveryEngine.analyze(vectorFromCandidate(candidate), trendTrace);
            } catch (Throwable e) {
                recovery = null;
            }
        }

        HistoricalStateSignal stateSignal =
                evaluateHistoricalStateSignal(candidate, history, trendTrace, recovery);
        LearningSignal learningSignal = evaluateLearningSignal(candidate);
        NumberShapeSignal numberSignal = evaluateNumberShapeSignal(candidate, stateSignal);

        RuntimeProfileSignal runtimeSignal =
                evaluateRuntimeCacheSignal(
                        store, candidate, stateSignal, history, mode, deepEvaluate);
        StateNumberProfileSignal stateNumberProfileSignal =
                evaluateStateNumberCacheSignal(
                        store, candidate, stateSignal, history, mode, deepEvaluate);
        TeacherMatrixSignal teacherSignal =
                evaluateTeacherMatrixSignal(
                        store, candidate, stateSignal, history, mode, deepEvaluate);
        V20DynamicCauseSignal v20Signal =
                evaluateV20DynamicCauseSignal(candidate, history, mode, deepEvaluate);

        double windowP = safeP(candidate.modelP);
        double shapeP = safeP(candidate.shapeP);
        double comboP = safeP(candidate.comboP);
        double recoveryP = safeP(candidate.recoveryP);

        double similarP;
        double memoryP;

        if (deepEvaluate) {
            similarP =
                    similar == null
                            ? estimateSimilarP(candidate, history)
                            : safeSimilarP(similar, candidate, history);
            memoryP =
                    memory == null
                            ? estimateMemoryP(candidate, history)
                            : safeMemoryP(memory, candidate, history);
        } else {
            similarP = estimateSimilarP(candidate, history);
            memoryP = estimateMemoryP(candidate, history);
        }

        double stateP = stateSignal == null ? AppConfig.RANDOM_K3_BASELINE : stateSignal.stateP;
        double numberShapeP =
                numberSignal == null ? AppConfig.RANDOM_K3_BASELINE : numberSignal.numberShapeP;
        double runtimeP =
                runtimeSignal == null ? AppConfig.RANDOM_K3_BASELINE : runtimeSignal.runtimeP;
        double stateNumberP =
                stateNumberProfileSignal == null
                        ? AppConfig.RANDOM_K3_BASELINE
                        : stateNumberProfileSignal.stateNumberP;
        double dynamicCauseP =
                v20Signal == null ? AppConfig.RANDOM_K3_BASELINE : v20Signal.dynamicP;

        result.modelP = windowP;
        result.shapeP = shapeP;
        result.comboP = comboP;
        result.recoveryP = recoveryP;
        result.similarP = similarP;
        result.memoryP = memoryP;

        double windowWeight = safeWeight(weight.windowWeight, 0.30d);
        double shapeWeight = safeWeight(weight.shapeWeight, 0.22d);
        double comboWeight = safeWeight(weight.comboWeight, 0.26d);
        double recoveryWeight = safeWeight(weight.recoveryWeight, 0.10d);
        double similarWeight = safeWeight(weight.similarWeight, deepEvaluate ? 0.060d : 0.035d);
        double memoryWeight = safeWeight(weight.memoryWeight, deepEvaluate ? 0.045d : 0.030d);
        double stateWeight = 0.040d;
        double numberWeight = 0.020d;
        double runtimeWeight = deepEvaluate ? 0.020d : 0.012d;
        double stateNumberWeight = deepEvaluate ? 0.020d : 0.012d;
        double dynamicCauseWeight = deepEvaluate ? 0.026d : 0.016d;

        double raw =
                windowP * windowWeight
                        + shapeP * shapeWeight
                        + comboP * comboWeight
                        + recoveryP * recoveryWeight
                        + similarP * similarWeight
                        + memoryP * memoryWeight
                        + stateP * stateWeight
                        + numberShapeP * numberWeight
                        + runtimeP * runtimeWeight
                        + stateNumberP * stateNumberWeight
                        + dynamicCauseP * dynamicCauseWeight;

        double totalWeight =
                windowWeight
                        + shapeWeight
                        + comboWeight
                        + recoveryWeight
                        + similarWeight
                        + memoryWeight
                        + stateWeight
                        + numberWeight
                        + runtimeWeight
                        + stateNumberWeight
                        + dynamicCauseWeight;

        if (totalWeight > 0.0d) {
            raw = raw / totalWeight;
        }

        raw += coreEvidenceLift(candidate, trendTrace, recovery, similar, memory);
        raw +=
                consistencyLift(
                        windowP,
                        shapeP,
                        comboP,
                        recoveryP,
                        similarP,
                        memoryP,
                        stateP,
                        numberShapeP);

        if (stateSignal != null) {
            raw += stateSignal.bonus * 0.45d;
            raw -= stateSignal.penalty * 0.65d;
        }

        if (numberSignal != null) {
            raw += numberSignal.bonus * 0.35d;
            raw -= numberSignal.penalty * 0.55d;
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            raw += runtimeSignal.bonus * 0.52d;
            raw += runtimeSignal.probAdjust * 0.55d;
            raw -= runtimeSignal.penalty * 0.60d;
            raw -= runtimeSignal.riskPenalty * 0.55d;
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            raw += stateNumberProfileSignal.bonus * 0.50d;
            raw += stateNumberProfileSignal.probAdjust * 0.52d;
            raw -= stateNumberProfileSignal.penalty * 0.58d;
            raw -= stateNumberProfileSignal.riskPenalty * 0.52d;
        }

        if (learningSignal != null) {
            raw += learningSignal.bonus * 0.45d;
            raw -= learningSignal.penalty * 0.55d;
        }

        candidate.fixCohort = AppConfig.FIX_COHORT;
        candidate.probabilityPipelineVersion = AppConfig.PROBABILITY_PIPELINE_VERSION;
        candidate.probabilityBeforeTeacher = AppConfig.clampProbability(raw);

        if (teacherSignal != null && teacherSignal.applied == 1) {
            raw += teacherSignal.bonus * 0.45d;
            raw += teacherSignal.probAdjust * 0.65d;
            raw -= teacherSignal.penalty * 0.65d;
            raw -= teacherSignal.riskPenalty * 0.70d;

            if ("STRONG".equals(teacherSignal.status)) {
                raw += 0.004d;
            } else if ("READY".equals(teacherSignal.status)) {
                raw += 0.002d;
            } else if ("RISK".equals(teacherSignal.status)) {
                raw -= 0.006d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            raw += v20Signal.probAdjust * 0.72d;
            raw += v20Signal.bonus * 0.48d;
            raw -= v20Signal.penalty * 0.70d;
            raw -= v20Signal.riskPenalty * 0.78d;

            if ("S".equals(v20Signal.grade)) {
                raw += 0.0035d;
            } else if ("A".equals(v20Signal.grade)) {
                raw += 0.0020d;
            } else if ("B".equals(v20Signal.grade)) {
                raw += 0.0008d;
            } else if ("C".equals(v20Signal.grade)) {
                raw -= 0.0045d;
            }

            if (v20Signal.hardRisk == 1) {
                raw -= 0.010d;
            }
        }

        candidate.probabilityAfterTeacher = AppConfig.clampProbability(raw);
        candidate.probabilityBeforeRisk = candidate.probabilityAfterTeacher;

        if (deepEvaluate) {
            raw += evidenceBonus(candidate, similar, memory, recovery, trendTrace);
            raw -=
                    riskPenalty(
                            candidate,
                            similar,
                            memory,
                            recovery,
                            trendTrace,
                            weight,
                            teacherSignal,
                            runtimeSignal,
                            stateNumberProfileSignal,
                            v20Signal);
            raw = lowSampleProtection(raw, candidate, similar, memory, history, mode);
        } else {
            raw += evidenceBonusLight(candidate, recovery, trendTrace);
            raw -=
                    riskPenaltyLight(
                            candidate,
                            recovery,
                            trendTrace,
                            weight,
                            teacherSignal,
                            runtimeSignal,
                            stateNumberProfileSignal,
                            v20Signal);
            raw = lowSampleProtectionLight(raw, candidate, history);
        }

        raw =
                applyHardProbabilityBounds(
                        raw,
                        candidate,
                        history,
                        deepEvaluate,
                        mode,
                        teacherSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        v20Signal);
        candidate.probabilityAfterRisk = AppConfig.clampProbability(raw);
        result.rawP = candidate.probabilityAfterRisk;

        double calibrated =
                softCalibratedProbability(store, result.rawP, useStoreCalibration && deepEvaluate);

        result.calibratedP = calibrated;
        result.calibrationError = Math.abs(calibrated - result.rawP);
        result.finalP = blendRawAndCalibrated(result.rawP, calibrated, similar, memory, candidate);

        result.finalP =
                applyFinalRiskAdjustment(
                        result.finalP,
                        candidate,
                        stateSignal,
                        learningSignal,
                        numberSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        teacherSignal,
                        v20Signal);

        result.finalP =
                applyLearningFinalAdjustment(
                        result.finalP,
                        candidate,
                        learningSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        teacherSignal,
                        v20Signal);

        result.finalP =
                auditFinalProbability(
                        store,
                        candidate,
                        result.finalP,
                        result.rawP,
                        result.calibrationError,
                        teacherSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        v20Signal);

        double finalPBeforeDiagnosis = result.finalP;
        DiagnosisProbabilitySignal diagnosisSignal =
                evaluateDiagnosisProbabilitySignal(
                        store,
                        candidate,
                        finalPBeforeDiagnosis,
                        result.rawP,
                        teacherSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        v20Signal);
        result.finalP =
                applyDiagnosisProbabilityAdjustment(
                        finalPBeforeDiagnosis, candidate, diagnosisSignal);
        writeDiagnosisProbabilityFields(
                candidate, diagnosisSignal, finalPBeforeDiagnosis, result.finalP);

        double finalPBeforeMainChain = result.finalP;
        MainChainProbabilitySignal mainChainSignal =
                evaluateMainChainProbabilitySignal(
                        store,
                        candidate,
                        finalPBeforeMainChain,
                        result.rawP,
                        teacherSignal,
                        v20Signal);
        result.finalP =
                applyMainChainProbabilityAdjustment(
                        finalPBeforeMainChain, candidate, mainChainSignal);
        writeMainChainProbabilityFields(
                candidate, mainChainSignal, finalPBeforeMainChain, result.finalP);

        double finalPBeforeCoverage = result.finalP;
        CoverageQualitySignal coverageSignal =
                evaluateK3CoverageQualitySignal(
                        candidate, history, teacherSignal, v20Signal, finalPBeforeCoverage);
        result.finalP =
                applyCoverageQualityProbabilityAdjustment(
                        finalPBeforeCoverage, candidate, coverageSignal);
        writeCoverageQualityFields(candidate, coverageSignal, finalPBeforeCoverage, result.finalP);
        ensureCoverageFieldsNeverEmpty(candidate, history, "CALCULATE_INTERNAL_AFTER_COVERAGE");

        if (deepEvaluate) {
            result.dynamicGate = calculateGate(candidate, similar, memory, recovery, trendTrace);
        } else {
            result.dynamicGate = calculateGateLight(candidate, recovery, trendTrace);
        }

        result.dynamicGate =
                applyRiskGateAdjustment(
                        result.dynamicGate,
                        candidate,
                        stateSignal,
                        learningSignal,
                        numberSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        teacherSignal,
                        v20Signal,
                        store);

        result.dynamicGate =
                applyLearningGateAdjustment(
                        result.dynamicGate,
                        candidate,
                        learningSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        teacherSignal,
                        v20Signal);

        result.dynamicGate =
                applyRealtimeGateSoftener(
                        result.dynamicGate,
                        candidate,
                        result.finalP,
                        result.rawP,
                        teacherSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        v20Signal);

        /* 所有证据只在本方法计算一次；写回候选供后续Gate/Decision复用。 */
        applyRuntimeSignalToCandidate(candidate, runtimeSignal);
        applyStateNumberProfileSignalToCandidate(candidate, stateNumberProfileSignal);
        applyTeacherMatrixSignalToCandidate(candidate, teacherSignal);
        applyV20DynamicSignalToCandidate(candidate, v20Signal);
        result.finalP =
                applyNeutralEvidenceFloorValue(
                        candidate,
                        result.finalP,
                        result.rawP,
                        teacherSignal,
                        runtimeSignal,
                        stateNumberProfileSignal,
                        v20Signal);
        applyColdStartNeutralRestoration(
                store,
                candidate,
                result,
                history,
                teacherSignal,
                runtimeSignal,
                stateNumberProfileSignal,
                v20Signal);
        result.dynamicGate = AppConfig.clampProbability(result.dynamicGate);
        result.gateAdjust = result.dynamicGate - AppConfig.DEFAULT_DYNAMIC_GATE;
        result.bucketKey = bucketKey(result.rawP);
        writeString(
                candidate,
                "probabilitySinglePassVersion",
                "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296");
        writeDouble(candidate, "probabilitySinglePassRawP", result.rawP);
        writeDouble(candidate, "probabilitySinglePassFinalP", result.finalP);
        writeString(candidate, "fixCohort", AppConfig.FIX_COHORT);
        writeString(
                candidate, "probabilityPipelineVersion", AppConfig.PROBABILITY_PIPELINE_VERSION);
        writeDouble(candidate, "probabilityBeforeTeacher", candidate.probabilityBeforeTeacher);
        writeDouble(candidate, "probabilityAfterTeacher", candidate.probabilityAfterTeacher);
        writeDouble(candidate, "probabilityBeforeRisk", candidate.probabilityBeforeRisk);
        writeDouble(candidate, "probabilityAfterRisk", candidate.probabilityAfterRisk);

        return result;
    }

    public Models.ComboCandidate enrichCandidate(
            DbStore store,
            Models.ComboCandidate source,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult) {
        Models.ModelWeight weight = loadWeightOnce(store);

        return enrichCandidateInternal(
                store,
                source,
                history,
                trendTrace,
                recoveryResult,
                weight,
                true,
                true,
                AppConfig.BACKTEST_MODE_FAST);
    }

    public Models.ComboCandidate enrichCandidateForBacktest(
            DbStore store,
            Models.ComboCandidate source,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            String mode) {
        Models.ModelWeight weight = loadWeightOnce(store);

        return enrichCandidateInternal(
                store,
                source,
                history,
                trendTrace,
                recoveryResult,
                weight,
                true,
                store != null,
                mode);
    }

    private Models.ComboCandidate enrichCandidateInternal(
            DbStore store,
            Models.ComboCandidate source,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.ModelWeight weight,
            boolean deepEvaluate,
            boolean useStoreCalibration,
            String mode) {
        if (source == null) {
            return null;
        }

        mode = AppConfig.normalizeBacktestMode(mode);

        Models.ComboCandidate c = source.copy();
        normalizeCandidateBeforeProbability(c);

        Models.ProbabilityResult p =
                calculateInternal(
                        store,
                        c,
                        history,
                        trendTrace,
                        recoveryResult,
                        weight,
                        deepEvaluate,
                        useStoreCalibration,
                        mode);

        c.modelP = p.modelP;
        c.shapeP = p.shapeP;
        c.comboP = p.comboP;
        c.recoveryP = p.recoveryP;
        c.similarP = p.similarP;
        c.memoryP = p.memoryP;
        c.calibratedP = p.calibratedP;
        c.calibrationError = p.calibrationError;
        c.rawP = p.rawP;
        c.finalP = p.finalP;
        c.dynamicGate = p.dynamicGate;
        c.edge = c.finalP - c.dynamicGate;
        c.probabilityMode = p.mode;
        c.backtestMode = mode;

        /* calculateInternal已经完成全部概率、教师、画像、V20、诊断与覆盖证据。
         * 此处禁止二次重算，避免同一证据重复查库、重复扣分和耗时翻倍。 */
        ensureCoverageFieldsNeverEmpty(c, history, "ENRICH_CANDIDATE_SINGLE_PASS");
        writeString(c, "probabilityExecutionPolicy", "SINGLE_PASS_ALL_STRATEGY_EVIDENCE");
        writeInt(c, "probabilityDuplicateEvaluationRemoved", 1);

        boolean buildFullReason =
                !AppConfig.BACKTEST_MODE_FAST.equals(mode) || c.rank <= 3 || c.rank == 0;
        if (buildFullReason) {
            c.reasonSummary =
                    appendReason(
                            c.reasonSummary,
                            "概率模式="
                                    + mode
                                    + (deepEvaluate ? "，深算" : "，基础精算")
                                    + " RawP="
                                    + UiFormatter.probability(c.rawP)
                                    + " CalP="
                                    + UiFormatter.probability(c.calibratedP)
                                    + " FinalP="
                                    + UiFormatter.probability(c.finalP)
                                    + " Gate="
                                    + UiFormatter.probability(c.dynamicGate)
                                    + " Edge="
                                    + UiFormatter.signedProbability(c.edge)
                                    + "，证据执行=历史状态/学习画像/号码形态/运行画像/状态号码/教师矩阵/V20因链均已单次完整计算"
                                    + "，Teacher="
                                    + readString(
                                            c,
                                            "teacherMatrixStatus",
                                            readString(c, "teacherStatus", ""))
                                    + "，V20="
                                    + readString(c, "v20Status", readString(c, "dynamicStatus", ""))
                                    + "，结论="
                                    + probabilityConclusion(c)
                                    + "，说明=120候选审计仍保留；正式前40完整概率/教师/V20/风险链同步执行；不绕过最终门");
        } else {
            c.reasonSummary =
                    appendReason(c.reasonSummary, "实时前40完整概率证据已计算；长原因仅对领先候选生成，数值证据和硬风险字段未删除");
        }

        return c;
    }

    public ArrayList<Models.ComboCandidate> enrichAll(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        return enrichAllRealtime(store, candidates, history, traces, recoveries);
    }

    public ArrayList<Models.ComboCandidate> enrichAllRealtime(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        /*
         * 1200候选与120审计池仍由FullCandidateScan完整保留；
         * 正式实时裁决只进入经过位置覆盖/成熟域/反事实/第三号筛选的前40。
         * 因此Probability对前40执行完整策略链，不删除任何概率/教师/V20/风险功能，
         * 仅避免对最终门不会同步使用的第41-120候选重复重算。
         */
        int realtimeLimit =
                Math.min(
                        AppConfig.PROBABILITY_BATCH_FULL_LIMIT,
                        normalizeEnrichLimit(
                                AppConfig.BACKTEST_MODE_FAST,
                                readAppInt(
                                        "PROB_REALTIME_ENRICH_LIMIT", REALTIME_ENRICH_HARD_LIMIT)));
        int realtimeDeep =
                Math.min(
                        realtimeLimit,
                        Math.min(
                                AppConfig.PROBABILITY_BATCH_DEEP_LIMIT,
                                normalizeDeepLimit(
                                        AppConfig.BACKTEST_MODE_FAST,
                                        readAppInt(
                                                "PROB_REALTIME_DEEP_LIMIT",
                                                REALTIME_DEEP_HARD_LIMIT))));
        if (store != null) {
            try {
                Models.DatabaseQueueState q = store.loadDatabaseQueueState();
                if (q != null && (q.waitingWriters + q.backgroundQueue) > 1)
                    realtimeDeep =
                            Math.min(realtimeDeep, AppConfig.PROBABILITY_MIN_DEEP_CANDIDATES);
            } catch (Throwable ignore) {
            }
        }
        return enrichAllWithMode(
                store,
                candidates,
                history,
                traces,
                recoveries,
                AppConfig.BACKTEST_MODE_FAST,
                realtimeLimit,
                realtimeDeep,
                true,
                true);
    }

    public ArrayList<Models.ComboCandidate> enrichAllForBacktest(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        return enrichAllWithMode(
                store,
                candidates,
                history,
                traces,
                recoveries,
                mode,
                AppConfig.getProbabilityEnrichLimit(mode),
                AppConfig.getProbabilityDeepLimit(mode),
                store != null,
                false);
    }

    public ArrayList<Models.ComboCandidate> enrichAllFullDeep(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        return enrichAllWithMode(
                store,
                candidates,
                history,
                traces,
                recoveries,
                AppConfig.BACKTEST_MODE_FULL,
                readAppInt("PROB_MANUAL_FULL_ENRICH_LIMIT", 120),
                readAppInt("PROB_MANUAL_FULL_DEEP_LIMIT", 60),
                store != null,
                false);
    }

    public ArrayList<Models.ComboCandidate> enrichAllWithMode(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String mode,
            int enrichLimit,
            int deepLimit,
            boolean allowStoreCalibration,
            boolean realtimeBudgetMode) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        mode = AppConfig.normalizeBacktestMode(mode);
        enrichLimit = normalizeEnrichLimit(mode, enrichLimit);
        deepLimit = normalizeDeepLimit(mode, deepLimit);

        ArrayList<Models.ComboCandidate> input = prepareCandidateList(candidates, enrichLimit);

        if (input.size() == 0) {
            return out;
        }

        beginProbabilityRunCache();
        Models.ModelWeight weight = loadWeightOnce(store);
        Models.FrequencyState linkFrequencyState = loadLinkFrequencyStateSafe(store);
        TrendStateMachine trendMachine = new TrendStateMachine();
        BreakRecoveryEngine recoveryMachine = new BreakRecoveryEngine();
        TrendStateMachine.TrendTrace[] traceByPosition = new TrendStateMachine.TrendTrace[11];
        BreakRecoveryEngine.RecoveryResult[] recoveryByPosition =
                new BreakRecoveryEngine.RecoveryResult[11];
        for (int p = 1; p <= 10; p++) {
            try {
                traceByPosition[p] = trendMachine.findTraceForPosition(traces, p);
            } catch (Throwable e) {
                traceByPosition[p] = null;
            }
            try {
                recoveryByPosition[p] = recoveryMachine.findForPosition(recoveries, p);
            } catch (Throwable e) {
                recoveryByPosition[p] = null;
            }
        }

        long startedAt = System.currentTimeMillis();
        CrashRecorder.setRuntimeContext(
                input.size() > 0 && input.get(0) != null ? input.get(0).targetIssue : "",
                "PROBABILITY_BATCH");

        boolean shallowMode = false;

        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);

            if (c == null) {
                continue;
            }

            TrendStateMachine.TrendTrace trace =
                    c.position >= 1 && c.position <= 10 ? traceByPosition[c.position] : null;
            BreakRecoveryEngine.RecoveryResult recovery =
                    c.position >= 1 && c.position <= 10 ? recoveryByPosition[c.position] : null;

            long elapsedBefore = System.currentTimeMillis() - startedAt;
            if (realtimeBudgetMode
                    && AppConfig.REALTIME_PROBABILITY_STOP_HEAVY_ON_BUDGET
                    && i >= AppConfig.REALTIME_PROBABILITY_MIN_FULL_COUNT
                    && elapsedBefore >= REALTIME_PROBABILITY_BUDGET_MS) shallowMode = true;
            boolean deep = !shallowMode && i < deepLimit;
            boolean useCal = !shallowMode && allowStoreCalibration && store != null && deep;

            Models.ComboCandidate enriched = null;
            try {
                if (shallowMode && AppConfig.REALTIME_PROBABILITY_USE_SHALLOW_AFTER_BUDGET) {
                    enriched =
                            enrichCandidateInternal(
                                    null, c, history, trace, recovery, weight, false, false, mode);
                    if (enriched != null) {
                        enriched.probabilityMode = "REALTIME_SHALLOW_AFTER_BUDGET";
                        enriched.reasonSummary =
                                appendReason(
                                        enriched.reasonSummary,
                                        "实时概率预算后使用内存/历史轻算；不查询SQLite、不生成长诊断；最终硬风险仍由DynamicGate/DecisionGate同步检查");
                    }
                } else {
                    enriched =
                            enrichCandidateInternal(
                                    store, c, history, trace, recovery, weight, deep, useCal, mode);
                }
            } catch (Throwable candidateError) {
                try {
                    if (store != null)
                        store.setState(
                                "probability_candidate_error",
                                "rank=" + i + "|" + String.valueOf(candidateError));
                } catch (Throwable ignore) {
                }
                enriched = buildDeferredAuditCandidate(c, linkFrequencyState, history);
                if (enriched != null) {
                    enriched.probabilityMode = "CANDIDATE_ERROR_SAFE_SHADOW";
                    enriched.qualifiedProbe = 0;
                    enriched.realProbeAllowed = 0;
                    enriched.probeRejectReason = "PROBABILITY_CANDIDATE_EXCEPTION";
                    enriched.reasonSummary =
                            appendReason(
                                    enriched.reasonSummary,
                                    "单候选概率异常已隔离，保留结构化SHADOW，不把整期FinalP归零，不允许BET");
                }
            }

            if (enriched != null) {
                applyLinkModificationV2ProbabilityFields(enriched, linkFrequencyState, history);
                out.add(enriched);
            }

            if (realtimeBudgetMode && out.size() >= REALTIME_MIN_OUTPUT_BEFORE_BUDGET) {
                long cost = System.currentTimeMillis() - startedAt;
                try {
                    if (store != null && cost > REALTIME_PROBABILITY_BUDGET_MS) {
                        store.setState("probability_budget_warning", "1");
                        store.setState("probability_budget_cost_ms", String.valueOf(cost));
                        store.setState(
                                "probability_budget_summary",
                                "前"
                                        + Math.min(
                                                out.size(),
                                                AppConfig.REALTIME_PROBABILITY_MIN_FULL_COUNT)
                                        + "候选完整精算；其余候选预算后内存轻算；不取消最终硬风险链");
                    }
                } catch (Throwable ignore) {
                }
                if (cost
                                >= Math.min(
                                        REALTIME_PROBABILITY_HARD_BUDGET_MS,
                                        AppConfig.PROBABILITY_BATCH_HARD_BUDGET_MS)
                        && i + 1 < input.size()) {
                    for (int j = i + 1; j < input.size(); j++) {
                        Models.ComboCandidate deferred =
                                buildDeferredAuditCandidate(
                                        input.get(j), linkFrequencyState, history);
                        if (deferred != null) out.add(deferred);
                    }
                    break;
                }
            }
        }

        sortByFinal(out);
        rerank(out);
        markChampionGapAfterSort(out);
        saveActiveCohortProbabilityDistribution(store, out, System.currentTimeMillis() - startedAt);
        try {
            if (store != null) {
                if (out.size() > 0)
                    store.saveProbabilityAuditState(
                            out.get(0), System.currentTimeMillis() - startedAt);
                store.setState(
                        "probability_run_cache_summary",
                        "hit="
                                + runStoreCacheHit
                                + "|miss="
                                + runStoreCacheMiss
                                + "|size="
                                + runStoreResultCache.size());
                store.setState(
                        "probability_execution_policy",
                        "ACTIVE_COHORT_BATCH_CONTEXT_SINGLE_PASS_ALL_STRATEGY_EVIDENCE");
                store.setState(
                        "probability_pipeline_version", AppConfig.PROBABILITY_PIPELINE_VERSION);
                store.setState("probability_fix_cohort", AppConfig.STABILITY_FIX_COHORT);
            }
        } catch (Throwable ignore) {
        }
        try {
            if (store != null) {
                store.setState("probability_stability_core", AppConfig.STABILITY_CORE_VERSION);
                store.setState(
                        "probability_deep_count",
                        String.valueOf(Math.min(deepLimit, input.size())));
                store.setState(
                        "probability_shallow_count",
                        String.valueOf(
                                Math.max(0, input.size() - Math.min(deepLimit, input.size()))));
            }
        } catch (Throwable ignore) {
        }
        endProbabilityRunCache();
        CrashRecorder.setRuntimeContext("", "PROBABILITY_IDLE");
        return out;
    }

    private Models.ComboCandidate buildDeferredAuditCandidate(
            Models.ComboCandidate source,
            Models.FrequencyState frequencyState,
            List<Models.Draw> history) {
        if (source == null) return null;
        Models.ComboCandidate c = source.copy();
        normalizeCandidateBeforeProbability(c);
        if (c.finalP <= 0.0d) {
            if (c.calibratedP > 0.0d) c.finalP = c.calibratedP;
            else if (c.rawP > 0.0d) c.finalP = c.rawP;
            else c.finalP = AppConfig.RANDOM_K3_BASELINE;
        }
        if (c.dynamicGate <= 0.0d) c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        c.edge = c.finalP - c.dynamicGate;
        c.probabilityMode = "REALTIME_DEFERRED_AUDIT";
        applyLinkModificationV2ProbabilityFields(c, frequencyState, history);
        ensureCoverageFieldsNeverEmpty(c, history, "REALTIME_DEFERRED_AUDIT");
        c.reasonSummary =
                appendReason(
                        c.reasonSummary, "概率硬预算后保留候选覆盖，使用已有预开奖证据进入审计；不直接赋予合格探针，不绕过DecisionGate");
        c.qualifiedProbe = 0;
        c.realProbeAllowed = 0;
        c.probeRejectReason = "PROBABILITY_DEFERRED_AUDIT";
        return c;
    }

    public Models.ComboCandidate pickBestAfterProbability(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries) {
        ArrayList<Models.ComboCandidate> list =
                enrichAllRealtime(store, candidates, history, traces, recoveries);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public Models.ComboCandidate pickBestAfterProbabilityForBacktest(
            DbStore store,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            String mode) {
        ArrayList<Models.ComboCandidate> list =
                enrichAllForBacktest(store, candidates, history, traces, recoveries, mode);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public double calculateGate(
            Models.ComboCandidate candidate,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace) {
        double gate = AppConfig.DEFAULT_DYNAMIC_GATE;

        if (candidate == null) {
            return AppConfig.HIGH_ACCURACY_GATE;
        }

        gate = cleanIncomingGate(candidate.dynamicGate, gate);
        gate += stateGate(candidate);

        try {
            gate += similar == null ? 0.0d : similarEngine.gateAdjustment(similar);
        } catch (Throwable e) {
        }

        try {
            gate += memory == null ? 0.0d : memoryEngine.memoryGateAdjustment(memory);
        } catch (Throwable e) {
        }

        try {
            gate += recovery == null ? 0.0d : recoveryEngine.gateAdjustment(recovery);
        } catch (Throwable e) {
        }

        try {
            TrendStateMachine trendMachine = new TrendStateMachine();
            gate += trendTrace == null ? 0.0d : trendMachine.gateAdjustment(trendTrace);
        } catch (Throwable e) {
        }

        gate += learningRiskGateFromCandidate(candidate);

        V20DynamicCauseSignal v20Signal =
                evaluateV20DynamicCauseSignal(candidate, null, AppConfig.BACKTEST_MODE_FAST, true);
        gate += v20Signal == null ? 0.0d : v20Signal.baseGateAdjust;

        if (candidate.effectiveSample < 160) {
            gate += 0.006d;
        }

        if (candidate.comboSample < 120) {
            gate += 0.004d;
        }

        if (candidate.calibrationError > AppConfig.CALIBRATION_ERROR_MAX) {
            gate += 0.003d;
        }

        if (candidate.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            gate += 0.006d;
        }

        if (candidate.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            gate += 0.006d;
        }

        if (isChaosState(candidate.stateMode)) {
            gate += 0.003d;
        }

        if (isHardState(candidate.stateMode)) {
            gate += 0.012d;
        }

        if (gate < minRealBetP()) {
            gate = minRealBetP();
        }

        if (gate > riskGateHigh()) {
            gate = riskGateHigh();
        }

        return AppConfig.clampProbability(gate);
    }

    private double calculateGateLight(
            Models.ComboCandidate candidate,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace) {
        double gate = AppConfig.DEFAULT_DYNAMIC_GATE;

        if (candidate == null) {
            return AppConfig.HIGH_ACCURACY_GATE;
        }

        gate = cleanIncomingGate(candidate.dynamicGate, gate);
        gate += stateGate(candidate);

        try {
            gate += recovery == null ? 0.0d : recoveryEngine.gateAdjustment(recovery);
        } catch (Throwable e) {
        }

        try {
            TrendStateMachine trendMachine = new TrendStateMachine();
            gate += trendTrace == null ? 0.0d : trendMachine.gateAdjustment(trendTrace);
        } catch (Throwable e) {
        }

        gate += learningRiskGateFromCandidate(candidate);

        V20DynamicCauseSignal v20Signal =
                evaluateV20DynamicCauseSignal(candidate, null, AppConfig.BACKTEST_MODE_FAST, false);
        gate += v20Signal == null ? 0.0d : v20Signal.baseGateAdjust;

        if (candidate.effectiveSample < 160) {
            gate += 0.006d;
        }

        if (candidate.comboSample < 120) {
            gate += 0.004d;
        }

        if (candidate.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            gate += 0.007d;
        }

        if (candidate.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            gate += 0.007d;
        }

        if (candidate.calibrationError > AppConfig.CALIBRATION_ERROR_MAX) {
            gate += 0.003d;
        }

        if (isChaosState(candidate.stateMode)) {
            gate += 0.003d;
        }

        if (isHardState(candidate.stateMode)) {
            gate += 0.012d;
        }

        if (gate < minRealBetP()) {
            gate = minRealBetP();
        }

        if (gate > riskGateHigh()) {
            gate = riskGateHigh();
        }

        return AppConfig.clampProbability(gate);
    }

    private double applyRealtimeGateSoftener(
            double gate,
            Models.ComboCandidate c,
            double finalP,
            double rawP,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        gate = AppConfig.clampProbability(gate);

        if (c == null) {
            return gate;
        }

        boolean hard =
                isHardState(c.stateMode)
                        || c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK
                        || c.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 18.0d
                        || (teacherSignal != null
                                && "RISK".equals(teacherSignal.status)
                                && finalP < 0.410d)
                        || (v20Signal != null && v20Signal.hardRisk == 1);

        if (hard) {
            return gate;
        }

        boolean teacherStrong = teacherSignalSupport(teacherSignal);
        boolean runtimeStrong = runtimeSignalSupport(runtimeSignal);
        boolean stateNumberStrong = stateNumberSignalSupport(stateNumberProfileSignal);
        boolean dynamicStrong = v20DynamicSupport(v20Signal);

        boolean learningStrong =
                c.profileScore >= 0.620d
                        || c.fallbackProfileScore >= 0.620d
                        || c.evidenceScore >= 0.620d
                        || c.aiJudgeScore >= 0.620d
                        || c.aPlusScore >= 0.620d
                        || c.promotionScore >= 0.620d;

        boolean shapeStrong =
                c.trendLife >= 55.0d
                        && c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                        && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                        && rawP >= RAW_BET_FLOOR_STRONG;

        if ((teacherStrong
                        || runtimeStrong
                        || stateNumberStrong
                        || learningStrong
                        || shapeStrong
                        || dynamicStrong)
                && gate > 0.398d) {
            gate = 0.398d;
        }

        if ((teacherStrong || runtimeStrong || stateNumberStrong || learningStrong || dynamicStrong)
                && shapeStrong
                && gate > 0.392d) {
            gate = 0.392d;
        }

        if (dynamicStrong && v20Signal != null && v20Signal.dynamicRisk <= 0.44d && gate > 0.390d) {
            gate = 0.390d;
        }

        if (gate < minRealBetP() - 0.010d) {
            gate = minRealBetP() - 0.010d;
        }

        if (gate < 0.345d) {
            gate = 0.345d;
        }

        return AppConfig.clampProbability(gate);
    }

    public boolean probabilityAllowsBet(Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            return false;
        }

        V20DynamicCauseSignal v20Signal =
                evaluateV20DynamicCauseSignal(candidate, null, candidate.backtestMode, true);

        if (candidate.finalP < minRealBetP() - 0.004d) {
            return false;
        }

        if (candidate.finalP + 0.0005d < candidate.dynamicGate) {
            return false;
        }

        if (candidate.edge < -0.0005d) {
            return false;
        }

        boolean strongSupport =
                candidate.trendLife >= 55.0d
                        || candidate.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                        || candidate.evidenceScore >= 0.620d
                        || candidate.profileScore >= 0.620d
                        || candidate.aPlusScore >= 0.620d
                        || isTeacherSupport(candidate)
                        || isRuntimeSupport(candidate)
                        || isStateNumberSupport(candidate)
                        || v20DynamicSupport(v20Signal);

        if (strongSupport) {
            if (candidate.rawP < RAW_BET_FLOOR_STRONG) {
                return false;
            }
        } else {
            if (candidate.rawP < RAW_BET_FLOOR_NORMAL) {
                return false;
            }
        }

        if (candidate.calibrationError > AppConfig.CALIBRATION_ERROR_HARD
                && candidate.finalP < candidate.dynamicGate + 0.006d) {
            return false;
        }

        if (candidate.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return false;
        }

        if (candidate.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 18.0d) {
            return false;
        }

        if (isHardState(candidate.stateMode)) {
            return false;
        }

        if (v20Signal != null) {
            if (v20Signal.hardRisk == 1) {
                return false;
            }

            if (v20Signal.freezeFlag == 1 && v20Signal.freezeRisk >= 0.70d) {
                return false;
            }

            if (v20Signal.dynamicRisk >= 0.72d && candidate.finalP < 0.420d) {
                return false;
            }

            if (v20Signal.shadowPass == 0
                    && v20Signal.dynamicScore < 0.58d
                    && candidate.finalP < 0.410d) {
                return false;
            }
        }

        if (candidate.profileScore < 0.280d
                && candidate.fallbackProfileScore < 0.280d
                && candidate.evidenceRisk >= 0.660d
                && candidate.finalP < 0.420d
                && !isTeacherSupport(candidate)
                && !isRuntimeSupport(candidate)
                && !isStateNumberSupport(candidate)
                && !v20DynamicSupport(v20Signal)) {
            return false;
        }

        String teacherStatus = readString(candidate, "teacherMatrixStatus", "");
        String runtimeStatus = readString(candidate, "runtimeProfileStatus", "");
        String stateNumberStatus = readString(candidate, "stateNumberProfileStatus", "");

        if ("RISK".equals(teacherStatus) && candidate.finalP < 0.410d) {
            return false;
        }

        if ("RISK".equals(runtimeStatus) && candidate.finalP < 0.410d) {
            return false;
        }

        if ("RISK".equals(stateNumberStatus) && candidate.finalP < 0.410d) {
            return false;
        }

        int teacherSample = readInt(candidate, "teacherMatrixSample", 0);
        double teacherHitRate = readDouble(candidate, "teacherMatrixHitRate", 0.0d);
        double teacherConfidence = readDouble(candidate, "teacherMatrixConfidence", 0.0d);

        if (teacherSample >= TEACHER_STRONG_SAMPLE
                && teacherConfidence >= 0.500d
                && teacherHitRate < 0.290d
                && !"STRONG".equals(teacherStatus)
                && !"READY".equals(teacherStatus)) {
            return false;
        }

        if (readInt(candidate, "falseHighPFlag", 0) == 1 && candidate.finalP < 0.425d) {
            return false;
        }
        if (readDouble(candidate, "k3CoverageRisk", 0.0d) >= 0.70d && candidate.finalP < 0.425d) {
            return false;
        }
        if (readInt(candidate, "probabilityTeacherWeakOrEmpty", 0) == 1
                && readInt(candidate, "probabilityV20UnknownOrEmpty", 0) == 1
                && !coveragePreferredDomain(candidate)
                && candidate.finalP < 0.415d
                && !isNoSampleWatchNeutralForProbability(candidate)) {
            return false;
        }
        return true;
    }

    public String explainCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return "概率候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(c.position);
        sb.append(" ");
        sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));

        sb.append(" RawP=");
        sb.append(UiFormatter.probability(c.rawP));

        sb.append(" CalP=");
        sb.append(UiFormatter.probability(c.calibratedP));

        sb.append(" FinalP=");
        sb.append(UiFormatter.probability(c.finalP));

        sb.append(" Gate=");
        sb.append(UiFormatter.probability(c.dynamicGate));

        sb.append(" Edge=");
        sb.append(UiFormatter.signedProbability(c.edge));

        sb.append(" Model=");
        sb.append(UiFormatter.probability(c.modelP));

        sb.append(" Shape=");
        sb.append(UiFormatter.probability(c.shapeP));

        sb.append(" Combo=");
        sb.append(UiFormatter.probability(c.comboP));

        sb.append(" Recovery=");
        sb.append(UiFormatter.probability(c.recoveryP));

        sb.append(" Similar=");
        sb.append(UiFormatter.probability(c.similarP));

        sb.append(" Memory=");
        sb.append(UiFormatter.probability(c.memoryP));

        sb.append(" Profile=");
        sb.append(UiFormatter.probability(c.profileScore));

        sb.append(" Promotion=");
        sb.append(UiFormatter.probability(c.promotionScore));

        int runtimeSample = readInt(c, "runtimeProfileSample", 0);
        if (runtimeSample > 0) {
            sb.append(" Runtime=");
            sb.append(runtimeSample);
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "runtimeProfileHitRate", 0.0d)));
            sb.append("/");
            sb.append(readString(c, "runtimeProfileStatus", ""));
        }

        int stateNumberSample = readInt(c, "stateNumberProfileSample", 0);
        if (stateNumberSample > 0) {
            sb.append(" StateNum=");
            sb.append(stateNumberSample);
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "stateNumberProfileHitRate", 0.0d)));
            sb.append("/");
            sb.append(readString(c, "stateNumberProfileStatus", ""));
        }

        int teacherSample = readInt(c, "teacherMatrixSample", 0);
        if (teacherSample > 0) {
            sb.append(" Teacher=");
            sb.append(teacherSample);
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "teacherMatrixHitRate", 0.0d)));
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "teacherMatrixConfidence", 0.0d)));

            String status = readString(c, "teacherMatrixStatus", "");
            if (status.length() > 0) {
                sb.append("/");
                sb.append(status);
            }
        }

        double dyn = readDouble(c, "dynamicCauseScore", 0.0d);
        if (dyn > 0.0d) {
            sb.append(" V20Dyn=");
            sb.append(UiFormatter.probability(dyn));
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "dynamicRiskScore", 0.0d)));
            String grade =
                    readString(
                            c,
                            "dynamicGrade",
                            readString(c, "v20Grade", readString(c, "v20Status", "")));
            if (grade.length() > 0) {
                sb.append("/");
                sb.append(grade);
            }
        }

        String actionAdvice = readString(c, "probabilityActionAdvice", "");
        if (actionAdvice.length() > 0) {
            sb.append(" Action=");
            sb.append(actionAdvice);
        }
        double evidenceContribution = readDouble(c, "probabilityEvidenceContribution", 0.0d);
        double counterEvidence = readDouble(c, "probabilityCounterEvidence", 0.0d);
        if (evidenceContribution > 0.0d || counterEvidence > 0.0d) {
            sb.append(" Evidence=");
            sb.append(UiFormatter.probability(evidenceContribution));
            sb.append("/");
            sb.append(UiFormatter.probability(counterEvidence));
        }
        double coverageQuality = readDouble(c, "coverageQualityScore", 0.0d);
        double coverageRisk = readDouble(c, "k3CoverageRisk", 0.0d);
        if (coverageQuality > 0.0d || coverageRisk > 0.0d) {
            sb.append(" K3Cover=");
            sb.append(UiFormatter.probability(coverageQuality));
            sb.append("/");
            sb.append(UiFormatter.probability(coverageRisk));
            sb.append(" Third=");
            sb.append(UiFormatter.probability(readDouble(c, "thirdNumberQualityScore", 0.0d)));
        }
        double seed = readDouble(c, "seedExpandScore", 0.0d);
        if (seed > 0.0d) {
            sb.append(" Seed=");
            sb.append(UiFormatter.probability(seed));
        }

        double freeze = readDouble(c, "falseStrongFreezeRisk", 0.0d);
        if (freeze > 0.0d) {
            sb.append(" FreezeRisk=");
            sb.append(UiFormatter.probability(freeze));
        }

        if (c.probabilityMode != null && c.probabilityMode.length() > 0) {
            sb.append(" Mode=");
            sb.append(c.probabilityMode);
        }

        if (c.blockCode != null && c.blockCode.length() > 0) {
            sb.append(" Block=");
            sb.append(UiFormatter.blockReasonText(c.blockCode));
        }

        sb.append(" 结论=");
        sb.append(probabilityConclusion(c));

        return sb.toString();
    }

    public String explainTop(List<Models.ComboCandidate> candidates, int limit) {
        if (candidates == null || candidates.size() == 0) {
            return "概率候选为空";
        }

        ArrayList<Models.ComboCandidate> list = new ArrayList<Models.ComboCandidate>();

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c != null) {
                list.add(c.copy());
            }
        }

        sortByFinal(list);
        rerank(list);

        int max = Math.min(limit, list.size());
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < max; i++) {
            Models.ComboCandidate c = list.get(i);

            if (i > 0) {
                sb.append(" | ");
            }

            sb.append("#");
            sb.append(c.rank);
            sb.append(" P");
            sb.append(c.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
            sb.append(" F=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append(" G=");
            sb.append(UiFormatter.probability(c.dynamicGate));
            sb.append(" E=");
            sb.append(UiFormatter.signedProbability(c.edge));

            int teacherSample = readInt(c, "teacherMatrixSample", 0);
            if (teacherSample > 0) {
                sb.append(" T=");
                sb.append(teacherSample);
                sb.append("/");
                sb.append(UiFormatter.probability(readDouble(c, "teacherMatrixHitRate", 0.0d)));
            }

            int runtimeSample = readInt(c, "runtimeProfileSample", 0);
            if (runtimeSample > 0) {
                sb.append(" R=");
                sb.append(runtimeSample);
                sb.append("/");
                sb.append(UiFormatter.probability(readDouble(c, "runtimeProfileHitRate", 0.0d)));
            }

            int stateNumberSample = readInt(c, "stateNumberProfileSample", 0);
            if (stateNumberSample > 0) {
                sb.append(" SN=");
                sb.append(stateNumberSample);
                sb.append("/");
                sb.append(
                        UiFormatter.probability(readDouble(c, "stateNumberProfileHitRate", 0.0d)));
            }

            double dyn = readDouble(c, "dynamicCauseScore", 0.0d);
            if (dyn > 0.0d) {
                sb.append(" DYN=");
                sb.append(UiFormatter.probability(dyn));
                sb.append("/");
                sb.append(readString(c, "dynamicGrade", ""));
            }
        }

        return sb.toString();
    }

    public void learnWeightAfterSettle(DbStore store, Models.Decision decision, boolean hit) {
        if (store == null || decision == null) {
            return;
        }

        Models.ModelWeight w = loadWeightOnce(store);

        if (w == null) {
            w = new Models.ModelWeight();
        }

        double target = hit ? 1.0d : 0.0d;
        double err = target - decision.finalP;

        w.recentError =
                w.recentError * AppConfig.LEARN_RECENT_DECAY
                        + Math.abs(err) * (1.0d - AppConfig.LEARN_RECENT_DECAY);

        w.updatedAt = System.currentTimeMillis();

        try {
            w.normalizePositive();
        } catch (Throwable e) {
        }

        learnTeacherWeightAfterSettle(w, decision, hit);

        invokeStore(
                store, "saveModelWeight", new Class[] {Models.ModelWeight.class}, new Object[] {w});

        try {
            store.setState("probability_weight_learning_mode", "V20_DYNAMIC_CAUSE_PROFILE_CHAIN");
            store.setState(
                    "probability_weight_learning_summary",
                    "V20概率主链：ModelWeight只记录recentError；TeacherMatrix/RuntimeProfile/StateNumberProfile/Learning/V20动态因链共同小幅反哺；不写真实校准");
            store.setState("probability_weight_learning_hit", hit ? "1" : "0");
            store.setState("probability_weight_learning_error", String.valueOf(Math.abs(err)));
        } catch (Throwable e) {
        }
    }

    private void rewardWeights(Models.ModelWeight w, Models.Decision d, double step) {}

    private void punishWeights(Models.ModelWeight w, Models.Decision d, double step) {}

    private void learnTeacherWeightAfterSettle(
            Models.ModelWeight w, Models.Decision d, boolean hit) {
        if (w == null || d == null) {
            return;
        }

        try {
            double err = (hit ? 1.0d : 0.0d) - d.finalP;
            double step = hit ? AppConfig.LEARN_REWARD_MAX_ONCE : AppConfig.LEARN_PUNISH_MAX_ONCE;
            step = Math.min(step, 0.004d);

            if (Math.abs(err) < 0.030d) {
                return;
            }

            if (hit) {
                w.comboWeight += step * 0.15d;
                w.shapeWeight += step * 0.10d;
                w.recoveryWeight += step * 0.08d;
            } else {
                w.comboWeight -= step * 0.10d;
                w.shapeWeight -= step * 0.08d;
                w.recoveryWeight -= step * 0.06d;
            }

            w.normalizePositive();
        } catch (Throwable ignore) {
        }
    }

    private Models.ModelWeight loadWeightOnce(DbStore store) {
        Models.ModelWeight weight = null;

        try {
            if (store != null) {
                Object value =
                        invokeStore(store, "getModelWeight", new Class[] {}, new Object[] {});

                if (value instanceof Models.ModelWeight) {
                    weight = (Models.ModelWeight) value;
                }
            }
        } catch (Throwable e) {
            weight = null;
        }

        if (weight == null) {
            weight = new Models.ModelWeight();
        }

        try {
            weight.normalizePositive();
        } catch (Throwable e) {
        }

        return weight;
    }

    private Models.SimilarResult safeSimilarEvaluate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        try {
            return similarEngine.evaluateCandidate(history, candidate);
        } catch (Throwable e) {
            try {
                SafeLog.e(TAG, "similar evaluate failed", e);
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    private ShapeMemoryEngine.MemorySignal safeMemoryEvaluate(
            Models.ComboCandidate candidate, List<Models.Draw> history) {
        try {
            return memoryEngine.evaluateCandidate(candidate, history);
        } catch (Throwable e) {
            try {
                SafeLog.e(TAG, "memory evaluate failed", e);
            } catch (Throwable ignore) {
            }
            return null;
        }
    }

    private ArrayList<Models.ComboCandidate> prepareCandidateList(
            List<Models.ComboCandidate> candidates, int limit) {
        ArrayList<Models.ComboCandidate> list = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return list;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c != null && c.isValidK3()) {
                Models.ComboCandidate copy = c.copy();
                normalizeCandidateBeforeProbability(copy);
                list.add(copy);
            }
        }

        sortByOriginalPreScore(list);

        while (limit > 0 && list.size() > limit) {
            list.remove(list.size() - 1);
        }

        return list;
    }

    private void sortByOriginalPreScore(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = originalPreScore(a);
                        double sb = originalPreScore(b);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return 1;
                        }

                        if (b == null) {
                            return -1;
                        }

                        if (a.rank != b.rank && a.rank > 0 && b.rank > 0) {
                            return a.rank - b.rank;
                        }

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private void sortByPreScore(List<Models.ComboCandidate> list) {
        sortByOriginalPreScore(list);
    }

    private double preScore(Models.ComboCandidate c) {
        return originalPreScore(c);
    }

    private double originalPreScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP;

        if (score <= 0.0d) {
            score = c.rawP;
        }

        if (score <= 0.0d) {
            score = c.modelP * 0.42d + c.shapeP * 0.24d + c.comboP * 0.24d + c.recoveryP * 0.10d;
        }

        score += c.edge * 0.32d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.10d;
        score += c.trendLife / 100.0d * 0.008d;
        score += c.recoveryScore / 100.0d * 0.006d;
        score -= c.driftRisk / 100.0d * 0.010d;
        score -= c.breakDepth / 100.0d * 0.010d;

        score += readDouble(c, "dynamicCauseScore", 0.0d) * 0.030d;
        score += readDouble(c, "seedExpandScore", 0.0d) * 0.014d;
        score += readDouble(c, "ruleHealthScore", 0.0d) * 0.010d;
        score += readDouble(c, "shadowHealthScore", 0.0d) * 0.010d;
        score += readDouble(c, "probabilityEvidenceContribution", 0.0d) * 0.014d;
        score += readDouble(c, "probabilityRecentValidity", 0.0d) * 0.008d;
        score += readDouble(c, "coverageQualityScore", 0.0d) * 0.012d;
        score += readDouble(c, "thirdNumberQualityScore", 0.0d) * 0.010d;
        score -= readDouble(c, "k3CoverageRisk", 0.0d) * 0.024d;
        score -= readDouble(c, "probabilityFalseHighRisk", 0.0d) * 0.014d;
        score -= readInt(c, "falseHighPFlag", 0) == 1 ? 0.020d : 0.0d;
        score -= readDouble(c, "dynamicRiskScore", 0.0d) * 0.026d;
        score -= readDouble(c, "probabilityCounterEvidence", 0.0d) * 0.018d;
        score -= readDouble(c, "probabilityFeatureDistance", 0.0d) * 0.006d;
        score += readDouble(c, "shadowPromotionBoost", 0.0d) * 0.65d;
        score += readDouble(c, "moduleAdjustedP", 0.0d) * 0.020d;
        score -= readDouble(c, "probabilityDiagnosisDiscount", 0.0d) * 0.70d;
        score -= readDouble(c, "freezeAdjustedP", 0.0d) * 0.060d;
        score -= readDouble(c, "falseStrongFreezeRisk", 0.0d) * 0.030d;

        if (isHardState(c.stateMode)) {
            score -= 0.030d;
        } else if (isChaosState(c.stateMode)) {
            score -= 0.006d;
        }

        if (c.blockCode != null
                && c.blockCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(c.blockCode)
                && !AppConfig.BLOCK_SAMPLE_WEAK.equals(c.blockCode)
                && !teacherMatrixWeakBlockCode().equals(c.blockCode)) {
            score -= 0.018d;
        }

        return score;
    }

    private double safeSimilarP(
            Models.SimilarResult similar, Models.ComboCandidate c, List<Models.Draw> history) {
        if (similar == null) {
            return estimateSimilarP(c, history);
        }

        double p = safeP(similar.similarHitRate);

        if (similar.effectiveSample < 80) {
            double est = estimateSimilarP(c, history);
            p = p * 0.35d + est * 0.65d;
        } else if (similar.effectiveSample < 160) {
            double est = estimateSimilarP(c, history);
            p = p * 0.55d + est * 0.45d;
        }

        if (p < 0.320d) {
            p = 0.320d;
        }

        return AppConfig.clampProbability(p);
    }

    private double safeMemoryP(
            ShapeMemoryEngine.MemorySignal memory,
            Models.ComboCandidate c,
            List<Models.Draw> history) {
        if (memory == null) {
            return estimateMemoryP(c, history);
        }

        double p = safeP(memory.recentMemoryRate);

        if (memory.sample128 < 80) {
            double est = estimateMemoryP(c, history);
            p = p * 0.35d + est * 0.65d;
        } else if (memory.sample128 < 120) {
            double est = estimateMemoryP(c, history);
            p = p * 0.55d + est * 0.45d;
        }

        if (p < 0.320d) {
            p = 0.320d;
        }

        return AppConfig.clampProbability(p);
    }

    private double estimateSimilarP(Models.ComboCandidate c, List<Models.Draw> history) {
        if (c == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        int size = history == null ? 0 : history.size();
        double p = AppConfig.RANDOM_K3_BASELINE;

        p += (safeP(c.modelP) - AppConfig.RANDOM_K3_BASELINE) * 0.50d;
        p += (safeP(c.comboP) - AppConfig.RANDOM_K3_BASELINE) * 0.34d;
        p += (safeP(c.shapeP) - AppConfig.RANDOM_K3_BASELINE) * 0.24d;

        if (c.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            p += 0.006d;
        } else if (c.effectiveSample < 160) {
            p -= 0.004d;
        }

        if (size < AppConfig.MIN_DRAWS_ANALYZE) {
            p = Math.min(p, 0.360d);
        }

        return AppConfig.clampProbability(p);
    }

    private double estimateMemoryP(Models.ComboCandidate c, List<Models.Draw> history) {
        if (c == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        int size = history == null ? 0 : history.size();
        double p = AppConfig.RANDOM_K3_BASELINE;

        p += (safeP(c.comboP) - AppConfig.RANDOM_K3_BASELINE) * 0.45d;
        p += (safeP(c.recoveryP) - AppConfig.RANDOM_K3_BASELINE) * 0.28d;
        p += c.comboGap * 0.10d;
        p += c.championGap * 0.08d;

        if (size < AppConfig.MIN_DRAWS_ANALYZE) {
            p = Math.min(p, 0.355d);
        }

        return AppConfig.clampProbability(p);
    }

    private HistoricalStateSignal evaluateHistoricalStateSignal(
            Models.ComboCandidate c,
            List<Models.Draw> history,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery) {
        HistoricalStateSignal s = new HistoricalStateSignal();

        if (c == null
                || !c.isValidK3()
                || history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(c.position)) {
            s.stateP = AppConfig.RANDOM_K3_BASELINE;
            s.summary = "历史状态不足";
            return s;
        }

        int total16 = 0;
        int hit16 = 0;
        int total32 = 0;
        int hit32 = 0;
        int total64 = 0;
        int hit64 = 0;
        int total128 = 0;
        int hit128 = 0;

        int zoneHit32 = 0;
        int zoneTotal32 = 0;
        int parityHit32 = 0;
        int parityTotal32 = 0;

        int lastSeenGap = 999;
        boolean seen = false;

        int start128 = Math.max(0, history.size() - RECENT_STATE_WINDOW_LONG);

        for (int i = start128; i < history.size(); i++) {
            Models.Draw d = history.get(i);

            if (d == null || !d.isValid()) {
                continue;
            }

            int number = d.getNumberAtPosition(c.position);

            if (!AppConfig.isValidNumber(number)) {
                continue;
            }

            int age = history.size() - 1 - i;
            boolean hit = containsNumber(c.k3Mask, number);

            total128++;

            if (hit) {
                hit128++;

                if (!seen) {
                    lastSeenGap = age;
                    seen = true;
                }
            }

            if (age < RECENT_STATE_WINDOW_BIG) {
                total64++;

                if (hit) {
                    hit64++;
                }
            }

            if (age < RECENT_STATE_WINDOW_MID) {
                total32++;

                if (hit) {
                    hit32++;
                }

                zoneTotal32++;

                if (candidateCoversZone(c, number)) {
                    zoneHit32++;
                }

                parityTotal32++;

                if (candidateCoversParity(c, number)) {
                    parityHit32++;
                }
            }

            if (age < RECENT_STATE_WINDOW_SMALL) {
                total16++;

                if (hit) {
                    hit16++;
                }
            }
        }

        double p16 =
                total16 > 0 ? ((double) hit16) / ((double) total16) : AppConfig.RANDOM_K3_BASELINE;
        double p32 =
                total32 > 0 ? ((double) hit32) / ((double) total32) : AppConfig.RANDOM_K3_BASELINE;
        double p64 =
                total64 > 0 ? ((double) hit64) / ((double) total64) : AppConfig.RANDOM_K3_BASELINE;
        double p128 =
                total128 > 0
                        ? ((double) hit128) / ((double) total128)
                        : AppConfig.RANDOM_K3_BASELINE;

        double zoneP =
                zoneTotal32 > 0
                        ? ((double) zoneHit32) / ((double) zoneTotal32)
                        : AppConfig.RANDOM_K3_BASELINE;
        double parityP =
                parityTotal32 > 0
                        ? ((double) parityHit32) / ((double) parityTotal32)
                        : AppConfig.RANDOM_K3_BASELINE;

        double stateP =
                p16 * 0.22d
                        + p32 * 0.30d
                        + p64 * 0.24d
                        + p128 * 0.14d
                        + zoneP * 0.06d
                        + parityP * 0.04d;

        double bonus = 0.0d;
        double penalty = 0.0d;

        if (p32 >= 0.390d && p64 >= 0.360d) {
            bonus += 0.004d;
        }

        if (p16 >= 0.430d && p32 >= 0.380d && c.driftRisk <= AppConfig.DRIFT_RISK_MAX) {
            bonus += 0.003d;
        }

        if (zoneP >= 0.650d) {
            bonus += 0.002d;
        }

        if (parityP >= 0.650d) {
            bonus += 0.001d;
        }

        if (p32 < 0.300d && p64 < 0.330d) {
            penalty += 0.008d;
        }

        if (lastSeenGap <= 2 && c.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX) {
            penalty += 0.006d;
        }

        if (lastSeenGap >= 20 && c.recoveryScore < AppConfig.RECOVERY_SCORE_MIN) {
            penalty += 0.005d;
        }

        if (trace != null && trace.hardBlock == 1) {
            penalty += 0.008d;
        }

        try {
            if (recovery != null && recoveryEngine.isHardBlock(recovery)) {
                penalty += 0.008d;
            }
        } catch (Throwable e) {
        }

        s.stateP = AppConfig.clampProbability(stateP);
        s.bonus = clamp(bonus, 0.0d, 0.012d);
        s.penalty = clamp(penalty, 0.0d, 0.022d);
        s.sample = total128;
        s.hitRate16 = p16;
        s.hitRate32 = p32;
        s.hitRate64 = p64;
        s.hitRate128 = p128;
        s.zoneSupport = zoneP;
        s.paritySupport = parityP;
        s.lastSeenGap = lastSeenGap;
        s.summary =
                "K3近16/32/64/128="
                        + UiFormatter.probability(p16)
                        + "/"
                        + UiFormatter.probability(p32)
                        + "/"
                        + UiFormatter.probability(p64)
                        + "/"
                        + UiFormatter.probability(p128)
                        + "，区间="
                        + UiFormatter.probability(zoneP)
                        + "，奇偶="
                        + UiFormatter.probability(parityP)
                        + "，间隔="
                        + lastSeenGap
                        + "，修正="
                        + UiFormatter.signedProbability(s.bonus - s.penalty);

        return s;
    }

    private LearningSignal evaluateLearningSignal(Models.ComboCandidate c) {
        LearningSignal s = new LearningSignal();

        if (c == null) {
            s.learningP = AppConfig.RANDOM_K3_BASELINE;
            s.summary = "学习候选为空";
            return s;
        }

        double support = 0.0d;
        int supportCount = 0;

        if (c.profileScore > 0.0d) {
            support += c.profileScore;
            supportCount++;
        }

        if (c.fallbackProfileScore > 0.0d) {
            support += c.fallbackProfileScore;
            supportCount++;
        }

        if (c.promotionScore > 0.0d) {
            support += c.promotionScore;
            supportCount++;
        }

        if (c.aPlusScore > 0.0d) {
            support += c.aPlusScore;
            supportCount++;
        }

        if (c.aiJudgeScore > 0.0d) {
            support += c.aiJudgeScore;
            supportCount++;
        }

        if (c.evidenceScore > 0.0d) {
            support += c.evidenceScore;
            supportCount++;
        }

        double dynamicCause = readDouble(c, "dynamicCauseScore", 0.0d);
        double ruleHealth = readDouble(c, "ruleHealthScore", 0.0d);
        double seedScore =
                readDouble(
                        c,
                        "seedExpandScore",
                        readDouble(c, "seedExpansionScore", readDouble(c, "seedScore", 0.0d)));

        if (dynamicCause > 0.0d) {
            support += dynamicCause;
            supportCount++;
        }

        if (ruleHealth > 0.0d) {
            support += ruleHealth;
            supportCount++;
        }

        if (seedScore > 0.0d) {
            support += seedScore;
            supportCount++;
        }

        double avgSupport =
                supportCount > 0 ? support / ((double) supportCount) : AppConfig.RANDOM_K3_BASELINE;
        double learningP = AppConfig.RANDOM_K3_BASELINE * 0.65d + avgSupport * 0.35d;

        double bonus = 0.0d;
        double penalty = 0.0d;

        if (avgSupport >= 0.620d && c.evidenceRisk <= 0.620d) {
            bonus += 0.005d;
        } else if (avgSupport >= 0.540d && c.evidenceRisk <= 0.600d) {
            bonus += 0.003d;
        }

        if (c.deepProfileHitRate >= 0.390d && c.deepProfileSample >= 18) {
            bonus += 0.003d;
        }

        if (dynamicCause >= 0.620d && readDouble(c, "dynamicRiskScore", 0.0d) <= 0.520d) {
            bonus += 0.002d;
        }

        if (c.evidenceRisk >= 0.620d) {
            penalty += 0.006d;
        }

        if (c.auditPenalty >= 0.250d) {
            penalty += Math.min(0.010d, c.auditPenalty * 0.025d);
        }

        if (c.profileScore > 0.0d && c.profileScore < 0.300d && c.finalP < 0.410d) {
            penalty += 0.006d;
        }

        if (c.conflictLevel >= 0.600d) {
            penalty += 0.006d;
        }

        if (readDouble(c, "dynamicRiskScore", 0.0d) >= 0.700d) {
            penalty += 0.006d;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            penalty += 0.006d;
        }

        s.learningP = AppConfig.clampProbability(learningP);
        s.bonus = clamp(bonus, 0.0d, 0.010d);
        s.penalty = clamp(penalty, 0.0d, 0.030d);
        s.avgSupport = avgSupport;
        s.supportCount = supportCount;
        s.summary =
                "profile="
                        + UiFormatter.probability(c.profileScore)
                        + "，fallback="
                        + UiFormatter.probability(c.fallbackProfileScore)
                        + "，promotion="
                        + UiFormatter.probability(c.promotionScore)
                        + "，V20Dyn="
                        + UiFormatter.probability(dynamicCause)
                        + "，Seed="
                        + UiFormatter.probability(seedScore)
                        + "，risk="
                        + UiFormatter.probability(c.evidenceRisk)
                        + "，修正="
                        + UiFormatter.signedProbability(s.bonus - s.penalty)
                        + "，说明=学习画像与V20动态因链共同参与证据，但不能绕过最终门";

        return s;
    }

    private NumberShapeSignal evaluateNumberShapeSignal(
            Models.ComboCandidate c, HistoricalStateSignal stateSignal) {
        NumberShapeSignal s = new NumberShapeSignal();

        if (c == null || !c.isValidK3()) {
            s.numberShapeP = AppConfig.RANDOM_K3_BASELINE;
            s.summary = "号码形态无效";
            return s;
        }

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int span = calcSpan(c.n1, c.n2, c.n3);

        int[] nums = new int[] {c.n1, c.n2, c.n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }
        }

        double p = AppConfig.RANDOM_K3_BASELINE;
        double bonus = 0.0d;
        double penalty = 0.0d;

        if (low > 0 && mid > 0 && high > 0) {
            bonus += 0.002d;
            p += 0.003d;
        }

        if (odd > 0 && even > 0) {
            bonus += 0.001d;
            p += 0.002d;
        }

        if (span >= 3 && span <= 7) {
            bonus += 0.001d;
            p += 0.002d;
        }

        if (span <= 1) {
            penalty += 0.004d;
            p -= 0.006d;
        }

        if ((low == 3 || high == 3) && c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            penalty += 0.005d;
            p -= 0.006d;
        }

        if (stateSignal != null) {
            if (stateSignal.zoneSupport >= 0.650d) {
                bonus += 0.001d;
                p += 0.002d;
            }

            if (stateSignal.paritySupport >= 0.650d) {
                bonus += 0.001d;
                p += 0.002d;
            }
        }

        double seedBalance = readDouble(c, "seedExpandScore", 0.0d);
        if (seedBalance >= 0.620d && readDouble(c, "seedRisk", 0.0d) <= 0.520d) {
            bonus += 0.0015d;
            p += 0.002d;
        }

        s.numberShapeP = AppConfig.clampProbability(p);
        s.bonus = clamp(bonus, 0.0d, 0.009d);
        s.penalty = clamp(penalty, 0.0d, 0.015d);
        s.summary =
                "低/中/高="
                        + low
                        + "/"
                        + mid
                        + "/"
                        + high
                        + "，奇/偶="
                        + odd
                        + "/"
                        + even
                        + "，跨度="
                        + span
                        + "，修正="
                        + UiFormatter.signedProbability(s.bonus - s.penalty);

        return s;
    }

    private RuntimeProfileSignal evaluateRuntimeCacheSignal(
            DbStore store,
            Models.ComboCandidate c,
            HistoricalStateSignal stateSignal,
            List<Models.Draw> history,
            String mode,
            boolean deepEvaluate) {
        RuntimeProfileSignal s = new RuntimeProfileSignal();

        if (store == null || c == null || !c.isValidK3()) {
            s.summary = "运行画像无输入";
            return s;
        }

        String stateKey = buildRuntimeStateKey(c);
        String profileKey = buildRuntimeProfileKey(stateKey, c.position, c.k3Mask);

        Object profile = readRuntimeProfile(store, profileKey, stateKey, c.position, c.k3Mask);

        if (profile == null) {
            s.summary = "无运行画像";
            s.stateKey = stateKey;
            s.profileKey = profileKey;
            return s;
        }

        int sample = readProfileSample(profile);
        double hitRate = readProfileHitRate(profile);
        double confidence = readProfileConfidence(profile, sample);
        double lift = readDouble(profile, "lift", 0.0d);
        double score = readDouble(profile, "profileScore", 0.0d);
        double risk = readDouble(profile, "riskScore", 0.0d);
        double probAdjust = readDouble(profile, "probAdjust", 0.0d);
        double gateAdjust = readDouble(profile, "gateAdjust", 0.0d);

        if (score <= 0.0d) {
            score = readDouble(profile, "score", 0.0d);
        }

        if (score <= 0.0d) {
            score = hitRate;
        }

        if (score > 1.0d) {
            score = score / 100.0d;
        }

        if (risk > 1.0d) {
            risk = risk / 100.0d;
        }

        if (sample <= 0 && hitRate <= 0.0d && score <= 0.0d) {
            s.summary = "运行画像样本不足";
            s.stateKey = stateKey;
            s.profileKey = profileKey;
            return s;
        }

        double sampleConfidence = clamp01(((double) sample) / 180.0d);
        if (confidence <= 0.0d) {
            confidence = sampleConfidence;
        } else {
            confidence = confidence * 0.70d + sampleConfidence * 0.30d;
        }

        double runtimeP = AppConfig.RANDOM_K3_BASELINE * 0.50d + hitRate * 0.32d + score * 0.18d;

        double bonus = 0.0d;
        double penalty = 0.0d;

        if (sample >= minCacheSampleForBet() && hitRate >= 0.370d && confidence >= 0.360d) {
            bonus += 0.003d;
            probAdjust += 0.002d;
        }

        if (sample >= 80 && hitRate >= 0.395d && confidence >= 0.500d) {
            bonus += 0.004d;
            probAdjust += 0.004d;
            gateAdjust -= 0.002d;
        }

        if (lift >= 0.030d && confidence >= 0.460d) {
            bonus += 0.002d;
            probAdjust += 0.002d;
        }

        if (sample >= minCacheSampleForBet() && hitRate > 0.0d && hitRate < 0.310d) {
            penalty += 0.006d;
            gateAdjust += 0.003d;
        }

        if (sample >= 80 && hitRate > 0.0d && hitRate < 0.290d) {
            penalty += 0.008d;
            gateAdjust += 0.005d;
        }

        if (risk >= 0.620d) {
            penalty += 0.005d;
            gateAdjust += 0.003d;
        }

        if (stateSignal != null && stateSignal.hitRate32 < 0.300d && hitRate < 0.340d) {
            penalty += 0.003d;
        }

        s.applied = 1;
        s.sample = sample;
        s.hitRate = AppConfig.clampProbability(hitRate);
        s.confidence = clamp01(confidence);
        s.runtimeP = AppConfig.clampProbability(runtimeP);
        s.bonus = clamp(bonus, 0.0d, MAX_RUNTIME_PROB_BOOST);
        s.penalty = clamp(penalty, 0.0d, MAX_RUNTIME_PROB_PENALTY);
        s.probAdjust = clamp(probAdjust, -MAX_RUNTIME_PROB_PENALTY, MAX_RUNTIME_PROB_BOOST);
        s.gateAdjust = clamp(gateAdjust, -MAX_RUNTIME_GATE_REDUCE, MAX_RUNTIME_GATE_BOOST);
        s.riskPenalty = clamp(risk, 0.0d, 0.025d);
        s.stateKey = stateKey;
        s.profileKey = profileKey;
        s.status =
                buildProfileStatus(sample, hitRate, confidence, risk, minCacheSampleForBet(), 80);
        s.summary =
                "样本="
                        + sample
                        + "，HR="
                        + UiFormatter.probability(s.hitRate)
                        + "，conf="
                        + UiFormatter.probability(s.confidence)
                        + "，P修正="
                        + UiFormatter.signedProbability(s.probAdjust + s.bonus - s.penalty)
                        + "，G修正="
                        + UiFormatter.signedProbability(s.gateAdjust)
                        + "，状态="
                        + s.status
                        + "，说明=RuntimeProfile缓存画像参与反哺";

        return s;
    }

    private StateNumberProfileSignal evaluateStateNumberCacheSignal(
            DbStore store,
            Models.ComboCandidate c,
            HistoricalStateSignal stateSignal,
            List<Models.Draw> history,
            String mode,
            boolean deepEvaluate) {
        StateNumberProfileSignal s = new StateNumberProfileSignal();

        if (store == null || c == null || !c.isValidK3()) {
            s.summary = "状态号码画像无输入";
            return s;
        }

        String stateKey = buildRuntimeStateKey(c);

        Object p1 =
                readStateNumberProfile(
                        store,
                        buildStateNumberProfileKey(stateKey, c.position, c.n1),
                        stateKey,
                        c.position,
                        c.n1);
        Object p2 =
                readStateNumberProfile(
                        store,
                        buildStateNumberProfileKey(stateKey, c.position, c.n2),
                        stateKey,
                        c.position,
                        c.n2);
        Object p3 =
                readStateNumberProfile(
                        store,
                        buildStateNumberProfileKey(stateKey, c.position, c.n3),
                        stateKey,
                        c.position,
                        c.n3);

        Object[] profiles = new Object[] {p1, p2, p3};

        int found = 0;
        int sampleSum = 0;
        double hitRateSum = 0.0d;
        double confidenceSum = 0.0d;
        double scoreSum = 0.0d;
        double riskSum = 0.0d;
        double probAdjust = 0.0d;
        double gateAdjust = 0.0d;
        String firstKey = "";

        for (int i = 0; i < profiles.length; i++) {
            Object p = profiles[i];

            if (p == null) {
                continue;
            }

            int sample = readProfileSample(p);
            double hitRate = readProfileHitRate(p);
            double confidence = readProfileConfidence(p, sample);
            double score = readDouble(p, "numberScore", 0.0d);
            double risk = readDouble(p, "riskScore", 0.0d);

            if (score <= 0.0d) {
                score = readDouble(p, "profileScore", 0.0d);
            }

            if (score <= 0.0d) {
                score = hitRate;
            }

            if (score > 1.0d) {
                score = score / 100.0d;
            }

            if (risk > 1.0d) {
                risk = risk / 100.0d;
            }

            if (sample <= 0 && hitRate <= 0.0d && score <= 0.0d) {
                continue;
            }

            found++;
            sampleSum += Math.max(0, sample);
            hitRateSum += hitRate;
            confidenceSum += confidence;
            scoreSum += score;
            riskSum += risk;

            probAdjust += readDouble(p, "probAdjust", 0.0d);
            gateAdjust += readDouble(p, "gateAdjust", 0.0d);

            if (firstKey.length() == 0) {
                firstKey = readString(p, "profileKey", "");
            }
        }

        if (found <= 0) {
            s.summary = "无状态号码画像";
            s.stateKey = stateKey;
            return s;
        }

        double avgHitRate = hitRateSum / ((double) found);
        double avgConfidence = confidenceSum / ((double) found);
        double avgScore = scoreSum / ((double) found);
        double avgRisk = riskSum / ((double) found);

        double sampleConfidence = clamp01(((double) sampleSum) / 160.0d);
        double confidence =
                avgConfidence > 0.0d
                        ? avgConfidence * 0.70d + sampleConfidence * 0.30d
                        : sampleConfidence;

        double stateNumberP =
                AppConfig.RANDOM_K3_BASELINE * 0.52d + avgHitRate * 0.32d + avgScore * 0.16d;

        double bonus = 0.0d;
        double penalty = 0.0d;

        if (sampleSum >= minStateSampleForBet() && avgHitRate >= 0.370d && confidence >= 0.360d) {
            bonus += 0.003d;
            probAdjust += 0.002d;
        }

        if (sampleSum >= 80 && avgHitRate >= 0.395d && confidence >= 0.500d) {
            bonus += 0.004d;
            probAdjust += 0.003d;
            gateAdjust -= 0.002d;
        }

        if (sampleSum >= minStateSampleForBet() && avgHitRate > 0.0d && avgHitRate < 0.310d) {
            penalty += 0.006d;
            gateAdjust += 0.003d;
        }

        if (sampleSum >= 80 && avgHitRate > 0.0d && avgHitRate < 0.290d) {
            penalty += 0.007d;
            gateAdjust += 0.004d;
        }

        if (avgRisk >= 0.620d) {
            penalty += 0.004d;
            gateAdjust += 0.003d;
        }

        if (stateSignal != null && stateSignal.hitRate32 < 0.300d && avgHitRate < 0.340d) {
            penalty += 0.003d;
        }

        s.applied = 1;
        s.found = found;
        s.sample = sampleSum;
        s.hitRate = AppConfig.clampProbability(avgHitRate);
        s.confidence = clamp01(confidence);
        s.stateNumberP = AppConfig.clampProbability(stateNumberP);
        s.bonus = clamp(bonus, 0.0d, MAX_STATE_NUMBER_PROB_BOOST);
        s.penalty = clamp(penalty, 0.0d, MAX_STATE_NUMBER_PROB_PENALTY);
        s.probAdjust =
                clamp(
                        probAdjust / Math.max(1.0d, (double) found),
                        -MAX_STATE_NUMBER_PROB_PENALTY,
                        MAX_STATE_NUMBER_PROB_BOOST);
        s.gateAdjust =
                clamp(
                        gateAdjust / Math.max(1.0d, (double) found),
                        -MAX_STATE_NUMBER_GATE_REDUCE,
                        MAX_STATE_NUMBER_GATE_BOOST);
        s.riskPenalty = clamp(avgRisk, 0.0d, 0.025d);
        s.stateKey = stateKey;
        s.profileKey = firstKey;
        s.status =
                buildProfileStatus(
                        sampleSum, avgHitRate, confidence, avgRisk, minStateSampleForBet(), 80);
        s.summary =
                "命中画像数="
                        + found
                        + "，样本="
                        + sampleSum
                        + "，HR="
                        + UiFormatter.probability(s.hitRate)
                        + "，conf="
                        + UiFormatter.probability(s.confidence)
                        + "，P修正="
                        + UiFormatter.signedProbability(s.probAdjust + s.bonus - s.penalty)
                        + "，G修正="
                        + UiFormatter.signedProbability(s.gateAdjust)
                        + "，状态="
                        + s.status
                        + "，说明=StateNumberProfile状态号码画像参与反哺";

        return s;
    }

    private Object readRuntimeProfile(
            DbStore store, String profileKey, String stateKey, int position, int k3Mask) {
        if (store == null) {
            return null;
        }

        Object out =
                invokeStore(
                        store,
                        "getRuntimeProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findRuntimeProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getRuntimeProfileCache",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findRuntimeProfileCache",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getRuntimeProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findRuntimeProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getRuntimeProfileCache",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findRuntimeProfileCache",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });

        return out;
    }

    private Object readStateNumberProfile(
            DbStore store, String profileKey, String stateKey, int position, int number) {
        if (store == null) {
            return null;
        }

        Object out =
                invokeStore(
                        store,
                        "getStateNumberProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findStateNumberProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getStateNumberProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findStateNumberProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number)
                        });

        return out;
    }

    private TeacherMatrixSignal evaluateTeacherMatrixSignal(
            DbStore store,
            Models.ComboCandidate c,
            HistoricalStateSignal stateSignal,
            List<Models.Draw> history,
            String mode,
            boolean deepEvaluate) {
        TeacherMatrixSignal s = new TeacherMatrixSignal();

        if (!readAppBoolean("TEACHER_MATRIX_ENABLE", true)) {
            s.summary = "未启用";
            return s;
        }

        if (store == null || c == null || !c.isValidK3()) {
            s.summary = "无教师矩阵输入";
            return s;
        }
        TeacherMatrixSignal bridged = buildTeacherSignalFromCandidate(c);
        if (bridged != null) return bridged;

        String stateKey = buildRuntimeStateKey(c);
        String comboKey = buildTeacherComboKey(stateKey, c.position, c.k3Mask);

        Object comboProfile =
                readTeacherComboProfile(store, comboKey, stateKey, c.position, c.k3Mask);

        Object p1 = null;
        Object p2 = null;
        Object p3 = null;

        if (deepEvaluate) {
            p1 =
                    readTeacherProfile(
                            store,
                            buildTeacherProfileKey(stateKey, c.position, c.n1),
                            stateKey,
                            c.position,
                            c.n1);
            p2 =
                    readTeacherProfile(
                            store,
                            buildTeacherProfileKey(stateKey, c.position, c.n2),
                            stateKey,
                            c.position,
                            c.n2);
            p3 =
                    readTeacherProfile(
                            store,
                            buildTeacherProfileKey(stateKey, c.position, c.n3),
                            stateKey,
                            c.position,
                            c.n3);
        }

        double hitRateSum = 0.0d;
        double confidenceSum = 0.0d;
        double scoreSum = 0.0d;
        int profileFound = 0;
        int sampleSum = 0;
        int strongProfile = 0;
        String profileKeyUsed = "";

        Object[] ps = new Object[] {p1, p2, p3};

        for (int i = 0; i < ps.length; i++) {
            Object p = ps[i];

            if (p == null) {
                continue;
            }

            int sample = readProfileSample(p);
            double hr = readProfileHitRate(p);
            double conf = readProfileConfidence(p, sample);
            double score = readDouble(p, "numberScore", 0.0d);

            if (score <= 0.0d) {
                score = readDouble(p, "profileScore", 0.0d);
            }

            if (score <= 0.0d) {
                score = hr;
            }

            if (score > 1.0d) {
                score = score / 100.0d;
            }

            if (sample <= 0 && hr <= 0.0d && score <= 0.0d) {
                continue;
            }

            profileFound++;
            sampleSum += Math.max(0, sample);
            hitRateSum += hr;
            confidenceSum += conf;
            scoreSum += score;

            if (profileKeyUsed.length() == 0) {
                profileKeyUsed = readString(p, "profileKey", "");
            }

            if (sample >= TEACHER_STRONG_SAMPLE && hr >= 0.390d && conf >= 0.460d) {
                strongProfile++;
            }
        }

        int comboSample = 0;
        double comboHitRate = 0.0d;
        double comboConfidence = 0.0d;
        double comboScore = 0.0d;
        double comboRisk = 0.0d;
        double comboProbAdjust = 0.0d;
        double comboGateAdjust = 0.0d;

        if (comboProfile != null) {
            comboSample = readProfileSample(comboProfile);
            comboHitRate = readProfileHitRate(comboProfile);
            comboConfidence = readProfileConfidence(comboProfile, comboSample);
            comboScore = readDouble(comboProfile, "comboScore", 0.0d);
            comboRisk = readDouble(comboProfile, "riskScore", 0.0d);
            comboProbAdjust = readDouble(comboProfile, "probAdjust", 0.0d);
            comboGateAdjust = readDouble(comboProfile, "gateAdjust", 0.0d);

            if (comboScore <= 0.0d) {
                comboScore = comboHitRate;
            }

            if (comboScore > 1.0d) {
                comboScore = comboScore / 100.0d;
            }

            if (comboRisk > 1.0d) {
                comboRisk = comboRisk / 100.0d;
            }
        }

        if (profileFound <= 0 && comboSample <= 0) {
            s.teacherP = AppConfig.RANDOM_K3_BASELINE;
            s.summary = "无匹配教师画像";
            s.stateKey = stateKey;
            return s;
        }

        double avgHitRate =
                profileFound > 0
                        ? hitRateSum / ((double) profileFound)
                        : AppConfig.RANDOM_K3_BASELINE;
        double avgConfidence = profileFound > 0 ? confidenceSum / ((double) profileFound) : 0.0d;
        double avgScore = profileFound > 0 ? scoreSum / ((double) profileFound) : avgHitRate;

        if (comboSample > 0) {
            avgHitRate = avgHitRate * 0.46d + comboHitRate * 0.54d;
            avgConfidence = avgConfidence * 0.46d + comboConfidence * 0.54d;
            avgScore = avgScore * 0.50d + comboScore * 0.50d;
        }

        if (avgHitRate <= 0.0d) {
            avgHitRate = avgScore > 0.0d ? avgScore : AppConfig.RANDOM_K3_BASELINE;
        }

        int totalSample = sampleSum + comboSample;
        double sampleConfidence = clamp01(((double) totalSample) / 220.0d);
        double confidence = avgConfidence > 0.0d ? avgConfidence : sampleConfidence;
        confidence = confidence * 0.70d + sampleConfidence * 0.30d;

        double teacherP =
                AppConfig.RANDOM_K3_BASELINE * 0.55d + avgHitRate * 0.30d + avgScore * 0.15d;

        double bonus = 0.0d;
        double penalty = 0.0d;
        double probAdjust = 0.0d;
        double gateAdjust = 0.0d;
        double riskPenalty = comboRisk;

        double tmProbAdjust = teacherMatrixProbAdjust();
        double tmStrongProbAdjust = teacherMatrixStrongProbAdjust();
        double tmStrongGateAdjust = teacherMatrixStrongGateAdjust();
        double tmWeakProbAdjust = teacherMatrixWeakProbAdjust();
        double tmWeakGateAdjust = teacherMatrixWeakGateAdjust();

        if (totalSample >= TEACHER_MIN_SAMPLE && avgHitRate >= 0.370d && confidence >= 0.360d) {
            bonus += tmProbAdjust * 0.35d;
            probAdjust += tmProbAdjust * 0.35d;
        }

        if (totalSample >= TEACHER_STRONG_SAMPLE && avgHitRate >= 0.400d && confidence >= 0.500d) {
            bonus += tmStrongProbAdjust * 0.45d;
            probAdjust += tmStrongProbAdjust * 0.45d;
            gateAdjust += tmStrongGateAdjust * 0.40d;
        }

        if (comboSample >= TEACHER_COMBO_STRONG_SAMPLE
                && comboHitRate >= 0.400d
                && confidence >= 0.360d) {
            bonus += tmProbAdjust * 0.30d;
            probAdjust += tmProbAdjust * 0.30d;
        }

        if (totalSample >= TEACHER_MIN_SAMPLE && avgHitRate < 0.310d) {
            penalty += Math.abs(tmWeakProbAdjust) * 0.55d;
            gateAdjust += tmWeakGateAdjust * 0.45d;
        }

        if (totalSample >= TEACHER_STRONG_SAMPLE && avgHitRate < 0.290d) {
            penalty += Math.abs(tmWeakProbAdjust) * 0.80d;
            gateAdjust += tmWeakGateAdjust * 0.65d;
        }

        if (comboSample >= TEACHER_COMBO_MIN_SAMPLE
                && comboHitRate > 0.0d
                && comboHitRate < 0.300d) {
            penalty += Math.abs(tmWeakProbAdjust) * 0.55d;
            gateAdjust += tmWeakGateAdjust * 0.45d;
        }

        if (stateSignal != null && stateSignal.hitRate32 < 0.300d && avgHitRate < 0.340d) {
            penalty += 0.003d;
        }

        if (comboProbAdjust != 0.0d) {
            probAdjust += comboProbAdjust * 0.35d;
        }

        if (comboGateAdjust != 0.0d) {
            gateAdjust += comboGateAdjust * 0.45d;
        }

        double v20TeacherScore =
                readDouble(
                        c, "teacherNumberScore", readDouble(c, "teacherMatrixNumberScore", 0.0d));
        double v20TeacherRisk = readDouble(c, "teacherNumberRisk", 0.0d);

        if (v20TeacherScore >= 0.58d && v20TeacherRisk <= 0.52d) {
            bonus += 0.002d;
            probAdjust += 0.002d;
        }

        if (v20TeacherRisk >= 0.68d) {
            penalty += 0.004d;
            gateAdjust += 0.003d;
        }

        s.applied = 1;
        s.profileFound = profileFound;
        s.strongProfile = strongProfile;
        s.sample = totalSample;
        s.numberSample = sampleSum;
        s.comboSample = comboSample;
        s.hitRate = AppConfig.clampProbability(avgHitRate);
        s.comboHitRate = AppConfig.clampProbability(comboHitRate);
        s.confidence = clamp01(confidence);
        s.teacherP = AppConfig.clampProbability(teacherP);
        s.bonus = clamp(bonus, 0.0d, MAX_TEACHER_PROB_BOOST);
        s.penalty = clamp(penalty, 0.0d, MAX_TEACHER_PROB_PENALTY);
        s.probAdjust = clamp(probAdjust, -MAX_TEACHER_PROB_PENALTY, MAX_TEACHER_PROB_BOOST);
        s.gateAdjust = clamp(gateAdjust, -MAX_TEACHER_GATE_REDUCE, MAX_TEACHER_GATE_BOOST);
        s.riskPenalty = clamp(riskPenalty, 0.0d, 0.030d);
        s.stateKey = stateKey;
        s.profileKey =
                profileKeyUsed.length() > 0
                        ? profileKeyUsed
                        : buildTeacherProfileKey(stateKey, c.position, c.n1);
        s.comboKey = comboKey;
        s.status = buildTeacherStatus(s);
        s.summary =
                "样本="
                        + totalSample
                        + "，号样本="
                        + sampleSum
                        + "，组样本="
                        + comboSample
                        + "，HR="
                        + UiFormatter.probability(s.hitRate)
                        + "，组HR="
                        + UiFormatter.probability(s.comboHitRate)
                        + "，conf="
                        + UiFormatter.probability(s.confidence)
                        + "，V20Teacher="
                        + UiFormatter.probability(v20TeacherScore)
                        + "，P修正="
                        + UiFormatter.signedProbability(s.probAdjust + s.bonus - s.penalty)
                        + "，G修正="
                        + UiFormatter.signedProbability(s.gateAdjust)
                        + "，状态="
                        + s.status
                        + "，说明=TeacherMatrix反哺强度恢复；参与共同证据，不绕过最终门";

        return s;
    }

    private TeacherMatrixSignal buildTeacherSignalFromCandidate(Models.ComboCandidate c) {
        if (c == null) return null;
        String status =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherNumberStatus", readString(c, "teacherStatus", "")));
        if (status == null || status.length() == 0) return null;
        TeacherMatrixSignal s = new TeacherMatrixSignal();
        s.status = status;
        s.sample =
                Math.max(
                        readInt(c, "teacherMatrixSample", 0), readInt(c, "teacherNumberSample", 0));
        s.numberSample = s.sample;
        s.comboSample = readInt(c, "teacherComboSample", 0);
        s.hitRate =
                readDouble(c, "teacherMatrixHitRate", readDouble(c, "teacherNumberHitRate", 0.0d));
        s.comboHitRate = readDouble(c, "teacherComboHitRate", s.hitRate);
        s.confidence =
                readDouble(
                        c,
                        "teacherMatrixConfidence",
                        readDouble(c, "teacherNumberConfidence", 0.0d));
        double score =
                readDouble(c, "teacherMatrixScore", readDouble(c, "teacherNumberScore", 0.0d));
        double risk = readDouble(c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d));
        s.teacherP =
                score > 0.0d
                        ? AppConfig.clampProbability(
                                AppConfig.RANDOM_K3_BASELINE * 0.55d
                                        + score * 0.25d
                                        + (s.hitRate > 0.0d
                                                        ? s.hitRate
                                                        : AppConfig.RANDOM_K3_BASELINE)
                                                * 0.20d)
                        : AppConfig.RANDOM_K3_BASELINE;
        s.probAdjust =
                clamp(
                        readDouble(
                                c,
                                "teacherMatrixProbAdjust",
                                readDouble(c, "teacherNumberProbAdjust", 0.0d)),
                        -MAX_TEACHER_PROB_PENALTY,
                        MAX_TEACHER_PROB_BOOST);
        s.gateAdjust =
                clamp(
                        readDouble(
                                c,
                                "teacherMatrixGateAdjust",
                                readDouble(c, "teacherNumberGateAdjust", 0.0d)),
                        -MAX_TEACHER_GATE_REDUCE,
                        MAX_TEACHER_GATE_BOOST);
        s.riskPenalty = clamp(risk, 0.0d, 0.025d);
        String u = status.toUpperCase(java.util.Locale.ROOT);
        boolean neutral = AppConfig.isNeutralEvidenceStatus(u);
        if (neutral && s.sample <= 0) {
            s.applied = 0;
            s.probAdjust = 0.0d;
            s.gateAdjust = 0.0d;
            s.riskPenalty = 0.0d;
            s.summary = "教师矩阵上游字段中性复用：" + status + "；不重复查库、不扣概率、不抬门";
        } else {
            s.applied = 1;
            if ((u.indexOf("STRONG") >= 0 || u.indexOf("READY") >= 0)
                    && s.sample >= TEACHER_MIN_SAMPLE)
                s.bonus = Math.min(MAX_TEACHER_PROB_BOOST, Math.max(0.0d, s.probAdjust));
            if (u.indexOf("RISK") >= 0 || u.indexOf("FREEZE") >= 0)
                s.penalty =
                        Math.min(
                                MAX_TEACHER_PROB_PENALTY,
                                Math.max(Math.abs(Math.min(0.0d, s.probAdjust)), 0.004d));
            s.summary =
                    "教师矩阵上游字段直接复用：status="
                            + status
                            + "｜sample="
                            + s.sample
                            + "｜HR="
                            + UiFormatter.probability(s.hitRate)
                            + "｜避免Probability二次SQLite读取";
        }
        s.stateKey = buildRuntimeStateKey(c);
        return s;
    }

    private Object readTeacherProfile(
            DbStore store, String profileKey, String stateKey, int position, int number) {
        if (store == null) {
            return null;
        }

        Object out =
                invokeStore(
                        store,
                        "getTeacherMatrixProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findTeacherMatrixProfile",
                        new Class[] {String.class},
                        new Object[] {profileKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getTeacherMatrixProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findTeacherMatrixProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(number)
                        });

        return out;
    }

    private Object readTeacherComboProfile(
            DbStore store, String comboKey, String stateKey, int position, int k3Mask) {
        if (store == null) {
            return null;
        }

        Object out =
                invokeStore(
                        store,
                        "getTeacherMatrixComboProfile",
                        new Class[] {String.class},
                        new Object[] {comboKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findTeacherMatrixComboProfile",
                        new Class[] {String.class},
                        new Object[] {comboKey});
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "getTeacherMatrixComboProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });
        if (out != null) return out;

        out =
                invokeStore(
                        store,
                        "findTeacherMatrixComboProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                        });

        return out;
    }

    private int readProfileSample(Object profile) {
        if (profile == null) {
            return 0;
        }

        int sample = readInt(profile, "sampleCount", 0);

        if (sample <= 0) {
            sample = readInt(profile, "sample", 0);
        }

        if (sample <= 0) {
            sample = readInt(profile, "totalSample", 0);
        }

        if (sample <= 0) {
            sample = readInt(profile, "recentSample", 0);
        }

        return sample;
    }

    private double readProfileHitRate(Object profile) {
        if (profile == null) {
            return 0.0d;
        }

        double hr = readDouble(profile, "hitRate", 0.0d);

        if (hr <= 0.0d) {
            hr = readDouble(profile, "recentHitRate", 0.0d);
        }

        if (hr <= 0.0d) {
            hr = readDouble(profile, "totalHitRate", 0.0d);
        }

        int hit = readInt(profile, "hitCount", 0);

        if (hit <= 0) {
            hit = readInt(profile, "positiveCount", 0);
        }

        int miss = readInt(profile, "missCount", 0);

        if (miss <= 0) {
            miss = readInt(profile, "negativeCount", 0);
        }

        if (hr <= 0.0d && hit + miss > 0) {
            hr = ((double) hit) / ((double) (hit + miss));
        }

        if (hr > 1.0d) {
            hr = hr / 100.0d;
        }

        return AppConfig.clampProbability(hr);
    }

    private double readProfileConfidence(Object profile, int sample) {
        if (profile == null) {
            return 0.0d;
        }

        double conf = readDouble(profile, "confidence", 0.0d);

        if (conf <= 0.0d) {
            conf = readDouble(profile, "stateConfidence", 0.0d);
        }

        if (conf > 1.0d) {
            conf = conf / 100.0d;
        }

        if (conf <= 0.0d) {
            conf = clamp01(((double) sample) / 180.0d);
        }

        return clamp01(conf);
    }

    private String buildProfileStatus(
            int sample,
            double hitRate,
            double confidence,
            double risk,
            int minSample,
            int strongSample) {
        if (sample < minSample) {
            return "WEAK";
        }

        if (risk >= 0.680d || (sample >= strongSample && hitRate > 0.0d && hitRate < 0.290d)) {
            return "RISK";
        }

        if (sample >= strongSample && hitRate >= 0.395d && confidence >= 0.500d) {
            return "STRONG";
        }

        if (sample >= minSample && hitRate >= 0.365d && confidence >= 0.360d) {
            return "READY";
        }

        return "WATCH";
    }

    private void applyRuntimeSignalToCandidate(Models.ComboCandidate c, RuntimeProfileSignal s) {
        if (c == null || s == null) {
            return;
        }

        writeInt(c, "runtimeProfileSample", s.sample);
        writeInt(c, "runtimeProfileApplied", s.applied);
        writeDouble(c, "runtimeProfileHitRate", s.hitRate);
        writeDouble(c, "runtimeProfileConfidence", s.confidence);
        writeDouble(c, "runtimeProfileProbAdjust", s.probAdjust + s.bonus - s.penalty);
        writeDouble(c, "runtimeProfileGateAdjust", s.gateAdjust);
        writeDouble(c, "runtimeProfileRiskPenalty", s.riskPenalty);
        writeString(c, "runtimeProfileStatus", s.status);
        writeString(c, "runtimeProfileKey", s.profileKey);
        writeString(c, "runtimeProfileSummary", s.summary);
    }

    private void applyStateNumberProfileSignalToCandidate(
            Models.ComboCandidate c, StateNumberProfileSignal s) {
        if (c == null || s == null) {
            return;
        }

        writeInt(c, "stateNumberProfileSample", s.sample);
        writeInt(c, "stateNumberProfileFound", s.found);
        writeInt(c, "stateNumberProfileApplied", s.applied);
        writeDouble(c, "stateNumberProfileHitRate", s.hitRate);
        writeDouble(c, "stateNumberProfileConfidence", s.confidence);
        writeDouble(c, "stateNumberProfileProbAdjust", s.probAdjust + s.bonus - s.penalty);
        writeDouble(c, "stateNumberProfileGateAdjust", s.gateAdjust);
        writeDouble(c, "stateNumberProfileRiskPenalty", s.riskPenalty);
        writeString(c, "stateNumberProfileStatus", s.status);
        writeString(c, "stateNumberProfileKey", s.profileKey);
        writeString(c, "stateNumberProfileSummary", s.summary);
    }

    private void applyTeacherMatrixSignalToCandidate(
            Models.ComboCandidate c, TeacherMatrixSignal s) {
        if (c == null || s == null) {
            return;
        }

        writeInt(c, "teacherMatrixSample", s.sample);
        writeInt(c, "teacherMatrixStateSample", s.numberSample);
        writeInt(c, "teacherMatrixComboSample", s.comboSample);
        writeInt(c, "teacherMatrixApplied", s.applied);
        writeDouble(c, "teacherMatrixHitRate", s.hitRate);
        writeDouble(c, "teacherMatrixRecentHitRate", s.comboHitRate);
        writeDouble(c, "teacherMatrixConfidence", s.confidence);
        writeDouble(c, "teacherMatrixProbAdjust", s.probAdjust + s.bonus - s.penalty);
        writeDouble(c, "teacherMatrixGateAdjust", s.gateAdjust);
        writeDouble(c, "teacherMatrixRiskPenalty", s.riskPenalty);
        writeDouble(
                c,
                "teacherEvidenceContribution",
                clamp01(s.bonus + s.probAdjust + s.hitRate * 0.18d + s.confidence * 0.12d));
        writeDouble(
                c,
                "teacherCounterContribution",
                clamp01(s.penalty + s.riskPenalty + (1.0d - s.confidence) * 0.10d));
        writeDouble(
                c,
                "teacherSampleReliability",
                sampleReliabilityFromSample(Math.max(s.sample, s.comboSample)));
        writeDouble(c, "teacherRecentValidity", Math.max(s.hitRate, s.comboHitRate));
        writeString(
                c,
                "teacherActionAdvice",
                teacherProbabilityActionAdvice(s.status, s.hitRate, s.confidence, s.riskPenalty));
        writeInt(c, "teacherMatrixFreezeFlag", "FREEZE".equals(s.status) ? 1 : 0);
        writeInt(
                c,
                "teacherMatrixRiskFlag",
                ("RISK".equals(s.status) || "FREEZE".equals(s.status)) ? 1 : 0);
        writeDouble(c, "teacherMatrixNumberScore", s.teacherP);
        writeDouble(c, "teacherMatrixComboScore", s.comboHitRate);
        writeDouble(c, "teacherMatrixStateScore", s.hitRate);
        writeString(c, "teacherMatrixStatus", s.status);
        writeString(c, "teacherMatrixProfileKey", s.profileKey);
        writeString(c, "teacherMatrixComboKey", s.comboKey);
        writeString(c, "teacherMatrixSummary", s.summary);

        writeDouble(
                c,
                "teacherNumberScore",
                Math.max(readDouble(c, "teacherNumberScore", 0.0d), s.teacherP));
        writeDouble(
                c,
                "teacherNumberRisk",
                Math.max(readDouble(c, "teacherNumberRisk", 0.0d), s.riskPenalty));
    }

    private String buildTeacherStatus(TeacherMatrixSignal s) {
        if (s == null || s.applied == 0) {
            return "NONE";
        }

        if (s.sample < TEACHER_MIN_SAMPLE && s.comboSample < TEACHER_COMBO_MIN_SAMPLE) {
            return "WEAK";
        }

        if (s.riskPenalty >= 0.014d || (s.sample >= TEACHER_STRONG_SAMPLE && s.hitRate < 0.290d)) {
            return "RISK";
        }

        if (s.sample >= TEACHER_STRONG_SAMPLE && s.hitRate >= 0.400d && s.confidence >= 0.500d) {
            return "STRONG";
        }

        if (s.comboSample >= TEACHER_COMBO_STRONG_SAMPLE
                && s.comboHitRate >= 0.400d
                && s.confidence >= 0.360d) {
            return "STRONG";
        }

        if (s.sample >= TEACHER_MIN_SAMPLE && s.hitRate >= 0.370d && s.confidence >= 0.360d) {
            return "READY";
        }

        if (s.comboSample >= TEACHER_COMBO_MIN_SAMPLE
                && s.comboHitRate >= 0.370d
                && s.confidence >= 0.360d) {
            return "READY";
        }

        return "WATCH";
    }

    private V20DynamicCauseSignal evaluateV20DynamicCauseSignal(
            Models.ComboCandidate c, List<Models.Draw> history, String mode, boolean deepEvaluate) {
        V20DynamicCauseSignal s = new V20DynamicCauseSignal();

        if (c == null || !c.isValidK3()) {
            s.summary = "V20动态因链无输入";
            return s;
        }

        double dynamicScore =
                readDouble(
                        c,
                        "dynamicCauseScore",
                        readDouble(c, "v20CauseScore", readDouble(c, "v20DynamicScore", 0.0d)));
        double support =
                readDouble(c, "dynamicSupportScore", readDouble(c, "v20SupportScore", 0.0d));
        double dynamicRisk = readDouble(c, "dynamicRiskScore", readDouble(c, "v20RiskScore", 0.0d));
        double confidence =
                readDouble(c, "dynamicConfidence", readDouble(c, "v20Confidence", 0.0d));

        double ruleScore = readDouble(c, "ruleHealthScore", 0.0d);
        double ruleRisk = readDouble(c, "ruleHealthRisk", 0.0d);

        double shadowScore = readDouble(c, "shadowHealthScore", 0.0d);
        double shadowRisk = readDouble(c, "shadowHealthRisk", 0.0d);

        double seedScore =
                readDouble(
                        c,
                        "seedExpandScore",
                        readDouble(c, "seedExpansionScore", readDouble(c, "seedScore", 0.0d)));
        double seedRisk = readDouble(c, "seedRisk", 0.0d);

        double freezeRisk =
                readDouble(
                        c,
                        "falseStrongFreezeRisk",
                        readDouble(
                                c, "falseStrongRisk", readDouble(c, "falseStrongRiskScore", 0.0d)));
        int freezeFlag = readInt(c, "falseStrongFreeze", 0);
        int freezeHard = readInt(c, "falseStrongHardFreeze", 0);

        double thirdScore = readDouble(c, "thirdGuardScore", 0.0d);
        double thirdRisk = readDouble(c, "thirdGuardRisk", 0.0d);

        double rankScore =
                readDouble(c, "counterfactualScore", readDouble(c, "rankCounterScore", 0.0d));
        double rankRisk =
                readDouble(c, "counterfactualRisk", readDouble(c, "rankCounterRisk", 0.0d));

        double positionScore =
                readDouble(c, "positionCauseScore", readDouble(c, "positionHealthScore", 0.0d));
        double positionRisk =
                readDouble(c, "positionWrongRisk", readDouble(c, "positionRiskScore", 0.0d));

        double teacherScore =
                readDouble(
                        c,
                        "teacherNumberScore",
                        readDouble(
                                c,
                                "teacherMatrixScore",
                                readDouble(c, "teacherMatrixNumberScore", 0.0d)));
        double teacherRisk =
                readDouble(
                        c,
                        "teacherNumberRisk",
                        readDouble(
                                c,
                                "teacherMatrixRisk",
                                readDouble(c, "teacherMatrixRiskPenalty", 0.0d)));

        int dynamicPass = readInt(c, "dynamicPass", 0);
        int dynamicHigh = readInt(c, "dynamicHighQuality", 0);
        int dynamicHard = readInt(c, "dynamicHardRisk", 0);
        int rulePass = readInt(c, "ruleHealthPass", 0);
        int shadowPass = readInt(c, "shadowPass", 0);
        int seedPass = readInt(c, "seedPass", 0);

        String grade =
                readString(
                        c,
                        "dynamicGrade",
                        readString(c, "v20Grade", readString(c, "v20Status", "")));
        V20WindowStateSignal windowState = buildV20WindowStateSignal(c, history);

        double positive = 0.0d;
        int positiveCount = 0;

        if (dynamicScore > 0.0d) {
            positive += dynamicScore * 1.35d;
            positiveCount += 135;
        }

        if (support > 0.0d) {
            positive += support * 0.80d;
            positiveCount += 80;
        }

        if (ruleScore > 0.0d) {
            positive += ruleScore * 0.72d;
            positiveCount += 72;
        }

        if (shadowScore > 0.0d) {
            positive += shadowScore * 0.64d;
            positiveCount += 64;
        }

        if (seedScore > 0.0d) {
            positive += seedScore * 0.70d;
            positiveCount += 70;
        }

        if (thirdScore > 0.0d) {
            positive += thirdScore * 0.62d;
            positiveCount += 62;
        }

        if (rankScore > 0.0d) {
            positive += rankScore * 0.54d;
            positiveCount += 54;
        }

        if (positionScore > 0.0d) {
            positive += positionScore * 0.48d;
            positiveCount += 48;
        }

        if (teacherScore > 0.0d) {
            positive += teacherScore * 0.46d;
            positiveCount += 46;
        }

        if (windowState != null
                && windowState.valid == 1
                && windowState.evidenceContribution > 0.0d) {
            positive += windowState.evidenceContribution * 0.74d;
            positiveCount += 74;
        }

        double baseSupport =
                positiveCount > 0 ? positive / ((double) positiveCount / 100.0d) / 100.0d : 0.0d;

        double risk = 0.0d;
        int riskCount = 0;

        if (dynamicRisk > 0.0d) {
            risk += dynamicRisk * 1.20d;
            riskCount += 120;
        }

        if (ruleRisk > 0.0d) {
            risk += ruleRisk * 0.80d;
            riskCount += 80;
        }

        if (shadowRisk > 0.0d) {
            risk += shadowRisk * 0.72d;
            riskCount += 72;
        }

        if (freezeRisk > 0.0d) {
            risk += freezeRisk * 0.95d;
            riskCount += 95;
        }

        if (seedRisk > 0.0d) {
            risk += seedRisk * 0.55d;
            riskCount += 55;
        }

        if (thirdRisk > 0.0d) {
            risk += thirdRisk * 0.70d;
            riskCount += 70;
        }

        if (rankRisk > 0.0d) {
            risk += rankRisk * 0.56d;
            riskCount += 56;
        }

        if (positionRisk > 0.0d) {
            risk += positionRisk * 0.46d;
            riskCount += 46;
        }

        if (teacherRisk > 0.0d) {
            risk += teacherRisk * 0.42d;
            riskCount += 42;
        }

        if (windowState != null && windowState.valid == 1 && windowState.counterEvidence > 0.0d) {
            risk += windowState.counterEvidence * 0.76d;
            riskCount += 76;
        }

        double baseRisk = riskCount > 0 ? risk / ((double) riskCount / 100.0d) / 100.0d : 0.0d;

        if (baseSupport <= 0.0d
                && baseRisk <= 0.0d
                && (windowState == null || windowState.valid == 0)) {
            s.summary = "V20动态因链字段未生成";
            return s;
        }

        s.applied = 1;
        s.dynamicScore = clamp01(baseSupport);
        s.dynamicSupport = support > 0.0d ? clamp01(support) : s.dynamicScore;
        s.dynamicRisk = clamp01(baseRisk);
        s.confidence =
                confidence > 0.0d
                        ? clamp01(confidence)
                        : clamp01(s.dynamicScore * 0.44d + (1.0d - s.dynamicRisk) * 0.30d);
        if (windowState != null && windowState.valid == 1) {
            s.confidence =
                    clamp01(
                            s.confidence * 0.72d
                                    + windowState.sampleReliability * 0.16d
                                    + windowState.recentValidity * 0.12d);
        }

        s.ruleScore = ruleScore;
        s.ruleRisk = ruleRisk;
        s.shadowScore = shadowScore;
        s.shadowRisk = shadowRisk;
        s.seedScore = seedScore;
        s.seedRisk = seedRisk;
        s.freezeRisk = freezeRisk;
        s.freezeFlag = freezeFlag;
        s.freezeHard = freezeHard;
        s.thirdGuardScore = thirdScore;
        s.thirdGuardRisk = thirdRisk;
        s.rankScore = rankScore;
        s.rankRisk = rankRisk;
        s.positionScore = positionScore;
        s.positionRisk = positionRisk;
        s.teacherScore = teacherScore;
        s.teacherRisk = teacherRisk;
        s.dynamicPass = dynamicPass;
        s.dynamicHigh = dynamicHigh;
        s.dynamicHard = dynamicHard;
        s.rulePass = rulePass;
        s.shadowPass = shadowPass;
        s.seedPass = seedPass;
        s.grade =
                grade == null || grade.length() == 0
                        ? buildV20Grade(s.dynamicScore, s.dynamicRisk, s.confidence)
                        : grade;
        if (windowState != null && windowState.valid == 1) {
            s.windowStateKey = windowState.stateKey;
            s.windowStateSummary = windowState.summary;
            s.evidenceContribution = windowState.evidenceContribution;
            s.counterEvidence = windowState.counterEvidence;
            s.sampleReliability = windowState.sampleReliability;
            s.recentValidity = windowState.recentValidity;
            s.featureDistance = windowState.featureDistance;
            s.actionAdvice = windowState.actionAdvice;
        } else {
            s.windowStateKey = "NO_WINDOW_STATE";
            s.actionAdvice = "样本不足：只保留已有概率，不因窗口状态放行";
        }

        s.hardRisk =
                dynamicHard == 1
                                || (windowState != null
                                        && windowState.valid == 1
                                        && windowState.counterEvidence >= 0.82d
                                        && windowState.recentValidity <= 0.28d)
                                || freezeHard == 1
                                || (freezeFlag == 1 && freezeRisk >= 0.70d)
                                || thirdRisk >= 0.78d
                                || ruleRisk >= 0.78d
                                || shadowRisk >= 0.78d
                                || s.dynamicRisk >= 0.80d
                        ? 1
                        : 0;

        double dynamicP = AppConfig.RANDOM_K3_BASELINE;
        dynamicP += (s.dynamicScore - 0.50d) * 0.055d;
        dynamicP += (s.dynamicSupport - 0.50d) * 0.025d;
        dynamicP += (s.confidence - 0.50d) * 0.018d;
        dynamicP -= s.dynamicRisk * 0.030d;
        if (windowState != null && windowState.valid == 1) {
            dynamicP += windowState.evidenceContribution * 0.012d;
            dynamicP -= windowState.counterEvidence * 0.016d;
        }

        s.dynamicP = AppConfig.clampProbability(dynamicP);

        double probAdjust = 0.0d;
        double gateAdjust = 0.0d;
        double bonus = 0.0d;
        double penalty = 0.0d;

        if (s.dynamicScore >= 0.66d && s.dynamicRisk <= 0.48d && s.confidence >= 0.44d) {
            bonus += 0.004d;
            probAdjust += 0.004d;
            gateAdjust -= 0.002d;
        }

        if (s.dynamicScore >= 0.72d && s.dynamicRisk <= 0.42d && s.confidence >= 0.55d) {
            bonus += 0.004d;
            probAdjust += 0.005d;
            gateAdjust -= 0.003d;
        }

        if (dynamicPass == 1 || dynamicHigh == 1 || seedPass == 1) {
            probAdjust += 0.002d;
        }

        if (rulePass == 1 && shadowPass == 1 && s.dynamicRisk <= 0.52d) {
            probAdjust += 0.002d;
        }

        if (windowState != null && windowState.valid == 1) {
            probAdjust +=
                    clamp(
                            windowState.evidenceContribution * 0.010d,
                            0.0d,
                            MAX_V20_WINDOW_PROB_BOOST);
            probAdjust -=
                    clamp(windowState.counterEvidence * 0.012d, 0.0d, MAX_V20_WINDOW_PROB_PENALTY);
            if (windowState.evidenceContribution >= 0.62d
                    && windowState.counterEvidence <= 0.42d
                    && windowState.recentValidity >= 0.48d) {
                bonus += 0.002d;
                gateAdjust -= 0.0015d;
            }
            if (windowState.counterEvidence >= 0.66d || windowState.recentValidity <= 0.22d) {
                penalty += 0.004d;
                gateAdjust += 0.003d;
            }
        }

        if (s.dynamicRisk >= 0.64d) {
            penalty += 0.006d;
            gateAdjust += 0.004d;
        }

        if (freezeFlag == 1 || freezeRisk >= 0.62d) {
            penalty += 0.006d;
            gateAdjust += 0.006d;
        }

        if (s.hardRisk == 1) {
            penalty += 0.010d;
            gateAdjust += 0.010d;
        }

        s.bonus = clamp(bonus, 0.0d, MAX_V20_DYNAMIC_PROB_BOOST);
        s.penalty = clamp(penalty, 0.0d, MAX_V20_DYNAMIC_PROB_PENALTY);
        s.probAdjust = clamp(probAdjust, -MAX_V20_DYNAMIC_PROB_PENALTY, MAX_V20_DYNAMIC_PROB_BOOST);
        s.gateAdjust = clamp(gateAdjust, -MAX_V20_DYNAMIC_GATE_REDUCE, MAX_V20_DYNAMIC_GATE_BOOST);
        s.baseGateAdjust = s.gateAdjust;
        s.riskPenalty =
                clamp(
                        s.dynamicRisk * 0.020d + freezeRisk * 0.016d + s.counterEvidence * 0.010d,
                        0.0d,
                        0.030d);

        s.summary =
                "DYN="
                        + UiFormatter.probability(s.dynamicScore)
                        + "，Risk="
                        + UiFormatter.probability(s.dynamicRisk)
                        + "，Conf="
                        + UiFormatter.probability(s.confidence)
                        + "，Grade="
                        + s.grade
                        + "，Rule="
                        + UiFormatter.probability(ruleScore)
                        + "，Shadow="
                        + UiFormatter.probability(shadowScore)
                        + "，Seed="
                        + UiFormatter.probability(seedScore)
                        + "，Freeze="
                        + UiFormatter.probability(freezeRisk)
                        + "，P修正="
                        + UiFormatter.signedProbability(s.probAdjust + s.bonus - s.penalty)
                        + "，G修正="
                        + UiFormatter.signedProbability(s.gateAdjust)
                        + "，窗口="
                        + safeText(s.windowStateKey)
                        + "，证据="
                        + UiFormatter.probability(s.evidenceContribution)
                        + "，反证="
                        + UiFormatter.probability(s.counterEvidence)
                        + "，样本可信="
                        + UiFormatter.probability(s.sampleReliability)
                        + "，近期有效="
                        + UiFormatter.probability(s.recentValidity)
                        + "，动作="
                        + safeText(s.actionAdvice)
                        + "，说明=V20动态因链只做概率/门槛/排序/解释小幅修正，不创建BET";

        return s;
    }

    private V20WindowStateSignal buildV20WindowStateSignal(
            Models.ComboCandidate c, List<Models.Draw> history) {
        V20WindowStateSignal s = new V20WindowStateSignal();

        if (c == null
                || !c.isValidK3()
                || history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(c.position)) {
            s.valid = 0;
            s.stateKey = "NO_HISTORY";
            s.actionAdvice = "样本不足：不因窗口状态放行";
            s.summary = "窗口状态样本不足";
            return s;
        }

        WindowBucket w12 = calcWindowBucket(c, history, V20_WINDOW_12);
        WindowBucket w24 = calcWindowBucket(c, history, V20_WINDOW_24);
        WindowBucket w48 = calcWindowBucket(c, history, V20_WINDOW_48);
        WindowBucket w96 = calcWindowBucket(c, history, V20_WINDOW_96);
        WindowBucket w360 = calcWindowBucket(c, history, V20_WINDOW_360);

        int available = w360.sample;
        if (available <= 0) {
            s.valid = 0;
            s.stateKey = "NO_VALID_DRAW";
            s.actionAdvice = "有效开奖不足：保持中性";
            s.summary = "窗口状态无有效开奖";
            return s;
        }

        double hotColdSlope =
                clampSigned(
                        (w12.hitRate - w48.hitRate) * 0.70d
                                + (w24.hitRate - w96.hitRate) * 0.50d
                                + (w48.hitRate - w360.hitRate) * 0.30d);
        double omissionPower =
                omissionReturnPower(w12.lastSeenGap, w24.lastSeenGap, w96.lastSeenGap);
        double density =
                clamp01(
                        w12.density * 0.36d
                                + w24.density * 0.30d
                                + w48.density * 0.20d
                                + w96.density * 0.14d);
        double repeat =
                clamp01(
                        w12.repeatScore * 0.44d
                                + w24.repeatScore * 0.28d
                                + w48.repeatScore * 0.18d
                                + w96.repeatScore * 0.10d);
        double parityBias =
                clamp01(w24.parityBias * 0.42d + w48.parityBias * 0.32d + w96.parityBias * 0.26d);
        double bigSmallBias =
                clamp01(
                        w24.bigSmallBias * 0.42d
                                + w48.bigSmallBias * 0.32d
                                + w96.bigSmallBias * 0.26d);
        double neighborTail =
                clamp01(
                        w12.neighborTail * 0.42d
                                + w24.neighborTail * 0.32d
                                + w48.neighborTail * 0.16d
                                + w96.neighborTail * 0.10d);
        double positionCrowd =
                clamp01(
                        w24.positionCrowd * 0.48d
                                + w48.positionCrowd * 0.30d
                                + w96.positionCrowd * 0.22d);
        double crossPositionSync =
                clamp01(
                        w24.crossPositionSync * 0.50d
                                + w48.crossPositionSync * 0.30d
                                + w96.crossPositionSync * 0.20d);
        double recentLose = clamp01(((double) w24.recentLoseStreak) / 24.0d);
        double timingOffset =
                clamp01(
                        w24.timingOffset * 0.42d
                                + w48.timingOffset * 0.32d
                                + w96.timingOffset * 0.26d);

        double hitShort = w24.sample > 0 ? w24.hitRate : AppConfig.RANDOM_K3_BASELINE;
        double hitMid = w96.sample > 0 ? w96.hitRate : hitShort;
        double hitLong = w360.sample > 0 ? w360.hitRate : hitMid;
        double liftShort = hitShort - AppConfig.RANDOM_K3_BASELINE;
        double liftMid = hitMid - AppConfig.RANDOM_K3_BASELINE;
        double liftLong = hitLong - AppConfig.RANDOM_K3_BASELINE;

        double evidence = 0.0d;
        double counter = 0.0d;

        evidence += positiveOnly(liftShort * 1.60d);
        evidence += positiveOnly(liftMid * 1.12d);
        evidence += positiveOnly(liftLong * 0.72d);
        evidence += positiveOnly(hotColdSlope * 0.88d);
        evidence += density * 0.16d;
        evidence += omissionPower * 0.12d;
        evidence += neighborTail * 0.08d;
        evidence += (1.0d - Math.abs(parityBias - 0.50d) * 2.0d) * 0.035d;
        evidence += (1.0d - Math.abs(bigSmallBias - 0.50d) * 2.0d) * 0.035d;

        counter += negativeOnly(liftShort * 1.72d);
        counter += negativeOnly(liftMid * 1.20d);
        counter += negativeOnly(liftLong * 0.80d);
        counter += negativeOnly(hotColdSlope * 0.95d);
        counter += clamp01(positionCrowd * 0.12d + crossPositionSync * 0.12d);
        counter += clamp01(repeat * 0.10d);
        counter += timingOffset * 0.10d;
        if (w24.recentLoseStreak >= 18 && hitShort < AppConfig.RANDOM_K3_BASELINE) {
            counter += 0.16d;
        }
        if (w12.hitRate <= 0.18d && w24.hitRate <= 0.24d) {
            counter += 0.14d;
        }

        s.valid = 1;
        s.sample = available;
        s.hitRate12 = w12.hitRate;
        s.hitRate24 = w24.hitRate;
        s.hitRate48 = w48.hitRate;
        s.hitRate96 = w96.hitRate;
        s.hitRate360 = w360.hitRate;
        s.hotColdSlope = hotColdSlope;
        s.omissionBucket = buildOmissionBucket(w24.lastSeenGap, w96.lastSeenGap);
        s.density = density;
        s.repeatScore = repeat;
        s.parityBias = parityBias;
        s.bigSmallBias = bigSmallBias;
        s.neighborTail = neighborTail;
        s.positionCrowd = positionCrowd;
        s.crossPositionSync = crossPositionSync;
        s.recentLoseStreak = w24.recentLoseStreak;
        s.timingOffset = timingOffset;
        s.sampleReliability =
                clamp01(((double) Math.min(available, V20_WINDOW_360)) / ((double) V20_WINDOW_360));
        s.recentValidity =
                clamp01(
                        0.36d
                                + positiveOnly(liftShort * 1.4d)
                                + positiveOnly(liftMid * 0.8d)
                                - negativeOnly(liftShort * 1.2d)
                                - recentLose * 0.24d
                                - timingOffset * 0.18d);
        s.featureDistance = calcWindowFeatureDistance(s);
        s.evidenceContribution =
                clamp01(
                        evidence * 0.58d
                                + s.recentValidity * 0.20d
                                + s.sampleReliability * 0.12d
                                - s.featureDistance * 0.18d);
        s.counterEvidence =
                clamp01(
                        counter * 0.62d
                                + s.featureDistance * 0.16d
                                + (1.0d - s.recentValidity) * 0.16d);
        s.stateKey = buildWindowStateKey(c, s);
        s.actionAdvice = buildWindowActionAdvice(s);
        s.summary =
                "状态="
                        + s.stateKey
                        + "｜12/24/48/96/360="
                        + UiFormatter.probability(s.hitRate12)
                        + "/"
                        + UiFormatter.probability(s.hitRate24)
                        + "/"
                        + UiFormatter.probability(s.hitRate48)
                        + "/"
                        + UiFormatter.probability(s.hitRate96)
                        + "/"
                        + UiFormatter.probability(s.hitRate360)
                        + "｜热冷斜率="
                        + UiFormatter.signedProbability(s.hotColdSlope)
                        + "｜遗漏="
                        + s.omissionBucket
                        + "｜密集="
                        + UiFormatter.probability(s.density)
                        + "｜重复="
                        + UiFormatter.probability(s.repeatScore)
                        + "｜单双="
                        + UiFormatter.probability(s.parityBias)
                        + "｜大小="
                        + UiFormatter.probability(s.bigSmallBias)
                        + "｜邻尾="
                        + UiFormatter.probability(s.neighborTail)
                        + "｜拥挤="
                        + UiFormatter.probability(s.positionCrowd)
                        + "｜同步="
                        + UiFormatter.probability(s.crossPositionSync)
                        + "｜连挂="
                        + s.recentLoseStreak
                        + "｜偏移="
                        + UiFormatter.probability(s.timingOffset)
                        + "｜证据="
                        + UiFormatter.probability(s.evidenceContribution)
                        + "｜反证="
                        + UiFormatter.probability(s.counterEvidence)
                        + "｜样本可信="
                        + UiFormatter.probability(s.sampleReliability)
                        + "｜近期有效="
                        + UiFormatter.probability(s.recentValidity)
                        + "｜动作="
                        + s.actionAdvice;
        return s;
    }

    private WindowBucket calcWindowBucket(
            Models.ComboCandidate c, List<Models.Draw> history, int windowSize) {
        WindowBucket b = new WindowBucket();
        b.windowSize = windowSize;
        if (c == null || history == null || history.size() == 0 || windowSize <= 0) {
            return b;
        }

        int start = history.size() - windowSize;
        if (start < 0) {
            start = 0;
        }
        int prevNumber = 0;
        int lastHitAge = 999;
        int consecutiveMiss = 0;
        int candidateOdd = countCandidateOdd(c);
        int candidateBig = countCandidateBig(c);
        int positionCrowdCount = 0;
        int syncCount = 0;
        int repeatCount = 0;
        int neighborCount = 0;
        int tailCount = 0;
        int parityMatch = 0;
        int bigSmallMatch = 0;
        int valid = 0;
        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }
            int n = d.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(n)) {
                continue;
            }
            valid++;
            boolean currentHit = containsNumber(c.k3Mask, n);
            if (currentHit) {
                hit++;
                lastHitAge = history.size() - 1 - i;
                consecutiveMiss = 0;
            } else {
                consecutiveMiss++;
            }
            if (prevNumber == n && prevNumber > 0) {
                repeatCount++;
            }
            if (isNeighborOfCandidate(c, n)) {
                neighborCount++;
            }
            if (isSameTailAsCandidate(c, n)) {
                tailCount++;
            }
            if ((n % 2 == 1 && candidateOdd > 0) || (n % 2 == 0 && candidateOdd < 3)) {
                parityMatch++;
            }
            if ((n >= 6 && candidateBig > 0) || (n < 6 && candidateBig < 3)) {
                bigSmallMatch++;
            }
            int sync = countCandidateMaskAcrossPositions(d, c.k3Mask);
            if (sync >= 3) {
                syncCount++;
            }
            if (sync >= 5) {
                positionCrowdCount++;
            }
            prevNumber = n;
        }

        b.sample = valid;
        b.hit = hit;
        b.hitRate =
                valid <= 0
                        ? AppConfig.RANDOM_K3_BASELINE
                        : clamp01(((double) hit) / ((double) valid));
        b.lastSeenGap = lastHitAge;
        b.recentLoseStreak = consecutiveMiss;
        b.density =
                valid <= 0
                        ? 0.0d
                        : clamp01(
                                ((double) hit)
                                        / ((double) Math.max(1, Math.min(valid, windowSize))));
        b.repeatScore =
                valid <= 1 ? 0.0d : clamp01(((double) repeatCount) / ((double) (valid - 1)));
        b.neighborTail =
                valid <= 0
                        ? 0.0d
                        : clamp01(
                                (((double) neighborCount) * 0.55d + ((double) tailCount) * 0.45d)
                                        / ((double) valid));
        b.parityBias = valid <= 0 ? 0.50d : clamp01(((double) parityMatch) / ((double) valid));
        b.bigSmallBias = valid <= 0 ? 0.50d : clamp01(((double) bigSmallMatch) / ((double) valid));
        b.positionCrowd =
                valid <= 0 ? 0.0d : clamp01(((double) positionCrowdCount) / ((double) valid));
        b.crossPositionSync = valid <= 0 ? 0.0d : clamp01(((double) syncCount) / ((double) valid));
        b.timingOffset = calcTimingOffset(lastHitAge, windowSize, b.hitRate);
        return b;
    }

    private int countCandidateMaskAcrossPositions(Models.Draw d, int k3Mask) {
        if (d == null || !d.isValid()) {
            return 0;
        }
        int count = 0;
        for (int p = 1; p <= 10; p++) {
            int n = d.getNumberAtPosition(p);
            if (containsNumber(k3Mask, n)) {
                count++;
            }
        }
        return count;
    }

    private int countCandidateOdd(Models.ComboCandidate c) {
        if (c == null) {
            return 0;
        }
        int odd = 0;
        if (c.n1 % 2 == 1) odd++;
        if (c.n2 % 2 == 1) odd++;
        if (c.n3 % 2 == 1) odd++;
        return odd;
    }

    private int countCandidateBig(Models.ComboCandidate c) {
        if (c == null) {
            return 0;
        }
        int big = 0;
        if (c.n1 >= 6) big++;
        if (c.n2 >= 6) big++;
        if (c.n3 >= 6) big++;
        return big;
    }

    private boolean isNeighborOfCandidate(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }
        return isNeighbor(c.n1, n) || isNeighbor(c.n2, n) || isNeighbor(c.n3, n);
    }

    private boolean isNeighbor(int a, int b) {
        if (!AppConfig.isValidNumber(a) || !AppConfig.isValidNumber(b)) {
            return false;
        }
        return Math.abs(a - b) == 1;
    }

    private boolean isSameTailAsCandidate(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }
        int tail = n % 10;
        return c.n1 % 10 == tail || c.n2 % 10 == tail || c.n3 % 10 == tail;
    }

    private double calcTimingOffset(int gap, int windowSize, double hitRate) {
        if (gap >= 999 || windowSize <= 0) {
            return 0.50d;
        }
        double expected = hitRate > 0.02d ? 1.0d / hitRate : ((double) windowSize) * 0.50d;
        if (expected < 1.0d) {
            expected = 1.0d;
        }
        return clamp01(Math.abs(((double) gap) - expected) / Math.max(1.0d, expected * 2.0d));
    }

    private double omissionReturnPower(int gap12, int gap24, int gap96) {
        int gap = gap24 < 999 ? gap24 : gap96;
        if (gap >= 999) {
            return 0.0d;
        }
        if (gap >= 8 && gap <= 22) {
            return 0.72d;
        }
        if (gap >= 4 && gap <= 28) {
            return 0.46d;
        }
        if (gap <= 2 && gap12 <= 2) {
            return 0.12d;
        }
        return 0.28d;
    }

    private String buildOmissionBucket(int gap24, int gap96) {
        int gap = gap24 < 999 ? gap24 : gap96;
        if (gap >= 999) return "MISS_UNKNOWN";
        if (gap <= 2) return "MISS_0_2_RECENT_HIT";
        if (gap <= 7) return "MISS_3_7_SHORT";
        if (gap <= 15) return "MISS_8_15_RETURN_ZONE";
        if (gap <= 29) return "MISS_16_29_DEEP";
        return "MISS_30_PLUS_COLD";
    }

    private double calcWindowFeatureDistance(V20WindowStateSignal s) {
        if (s == null || s.valid == 0) {
            return 1.0d;
        }
        double d = 0.0d;
        d += Math.abs(s.hitRate24 - AppConfig.RANDOM_K3_BASELINE) * 0.28d;
        d += Math.abs(s.hitRate96 - AppConfig.RANDOM_K3_BASELINE) * 0.20d;
        d += Math.abs(s.hotColdSlope) * 0.18d;
        d += Math.abs(s.parityBias - 0.50d) * 0.10d;
        d += Math.abs(s.bigSmallBias - 0.50d) * 0.10d;
        d += s.positionCrowd * 0.07d;
        d += s.crossPositionSync * 0.07d;
        return clamp01(d * 2.60d);
    }

    private String buildWindowStateKey(Models.ComboCandidate c, V20WindowStateSignal s) {
        StringBuilder sb = new StringBuilder();
        sb.append("P");
        sb.append(c == null ? 0 : c.position);
        sb.append("_W12_24_48_96_360");
        sb.append("_HC_");
        sb.append(bucketSignedState(s.hotColdSlope));
        sb.append("_");
        sb.append(s.omissionBucket);
        sb.append("_DEN_");
        sb.append(bucket01("D", s.density));
        sb.append("_REP_");
        sb.append(bucket01("R", s.repeatScore));
        sb.append("_PAR_");
        sb.append(bucketBias(s.parityBias));
        sb.append("_BS_");
        sb.append(bucketBias(s.bigSmallBias));
        sb.append("_NT_");
        sb.append(bucket01("N", s.neighborTail));
        sb.append("_CROWD_");
        sb.append(bucket01("C", s.positionCrowd));
        sb.append("_SYNC_");
        sb.append(bucket01("S", s.crossPositionSync));
        sb.append("_LOSE_");
        sb.append(bucketLose(s.recentLoseStreak));
        sb.append("_TIME_");
        sb.append(bucket01("T", s.timingOffset));
        return sb.toString();
    }

    private String buildWindowActionAdvice(V20WindowStateSignal s) {
        if (s == null || s.valid == 0) {
            return "HOLD：窗口状态不足";
        }
        if (s.counterEvidence >= 0.72d && s.recentValidity <= 0.32d) {
            return "HOLD_STRICT：反证强，等待DecisionGate复核";
        }
        if (s.evidenceContribution >= 0.62d
                && s.counterEvidence <= 0.42d
                && s.sampleReliability >= 0.32d) {
            return "ALLOW_REVIEW：证据可用，但仍由DecisionGate最终决定";
        }
        if (s.evidenceContribution >= 0.50d && s.recentValidity >= 0.42d) {
            return "WATCH_ALLOW：可进入候选排序，不直接BET";
        }
        return "WATCH_HOLD：证据不够强，保持候选观察";
    }

    private String bucketSignedState(double v) {
        if (v >= 0.120d) return "UP_STRONG";
        if (v >= 0.040d) return "UP";
        if (v <= -0.120d) return "DOWN_STRONG";
        if (v <= -0.040d) return "DOWN";
        return "FLAT";
    }

    private String bucket01(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }
        if (v >= 0.70d) return prefix + "_HIGH";
        if (v >= 0.42d) return prefix + "_MID";
        if (v >= 0.18d) return prefix + "_LOW";
        return prefix + "_TINY";
    }

    private String bucketBias(double v) {
        if (v >= 0.72d) return "BIAS_HIGH";
        if (v >= 0.58d) return "BIAS_UP";
        if (v <= 0.28d) return "BIAS_LOW";
        if (v <= 0.42d) return "BIAS_DOWN";
        return "BIAS_BAL";
    }

    private String bucketLose(int lose) {
        if (lose >= 18) return "18_PLUS";
        if (lose >= 10) return "10_17";
        if (lose >= 4) return "4_9";
        return "0_3";
    }

    private double positiveOnly(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0d) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private double negativeOnly(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v >= 0.0d) {
            return 0.0d;
        }
        v = -v;
        if (v > 1.0d) {
            return 1.0d;
        }
        return v;
    }

    private double clampSigned(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v > 1.0d) {
            return 1.0d;
        }
        if (v < -1.0d) {
            return -1.0d;
        }
        return v;
    }

    private String safeText(String text) {
        return text == null ? "" : text;
    }

    private String buildV20Grade(double score, double risk, double confidence) {
        if (risk >= 0.78d) {
            return "C";
        }

        if (score >= 0.72d && risk <= 0.44d && confidence >= 0.55d) {
            return "S";
        }

        if (score >= 0.62d && risk <= 0.52d) {
            return "A";
        }

        if (score >= 0.50d && risk <= 0.64d) {
            return "B";
        }

        return "C";
    }

    private void applyV20DynamicSignalToCandidate(
            Models.ComboCandidate c, V20DynamicCauseSignal s) {
        if (c == null || s == null || s.applied == 0) {
            return;
        }

        writeInt(c, "v20ProbabilityApplied", s.applied);
        writeDouble(c, "v20ProbabilityDynamicP", s.dynamicP);
        writeDouble(c, "v20ProbabilityProbAdjust", s.probAdjust + s.bonus - s.penalty);
        writeDouble(c, "v20ProbabilityGateAdjust", s.gateAdjust);
        writeDouble(c, "v20ProbabilityRiskPenalty", s.riskPenalty);
        writeDouble(c, "v20ProbabilityScore", s.dynamicScore);
        writeDouble(c, "v20ProbabilityRisk", s.dynamicRisk);
        writeDouble(c, "v20ProbabilityConfidence", s.confidence);
        writeInt(c, "v20ProbabilityHardRisk", s.hardRisk);
        writeString(c, "v20ProbabilityGrade", s.grade);
        writeString(c, "v20ProbabilitySummary", s.summary);
        writeString(c, "windowStateKey", s.windowStateKey);
        writeString(c, "probabilityWindowStateKey", s.windowStateKey);
        writeString(c, "probabilityWindowSummary", s.windowStateSummary);
        writeDouble(c, "probabilityEvidenceContribution", s.evidenceContribution);
        writeDouble(c, "probabilityCounterEvidence", s.counterEvidence);
        writeDouble(c, "probabilitySampleReliability", s.sampleReliability);
        writeDouble(c, "probabilityRecentValidity", s.recentValidity);
        writeDouble(c, "probabilityFeatureDistance", s.featureDistance);
        writeDouble(c, "v20SupportScore", s.dynamicSupport);
        writeDouble(c, "v20RiskScore", s.dynamicRisk);
        writeDouble(c, "v20Confidence", s.confidence);
        writeString(c, "v20Status", s.grade);
        writeDouble(c, "v20EvidenceContribution", s.evidenceContribution);
        writeDouble(c, "v20CounterContribution", s.counterEvidence);
        writeDouble(c, "v20SampleReliability", s.sampleReliability);
        writeDouble(c, "v20RecentValidity", s.recentValidity);
        writeString(c, "probabilityActionAdvice", s.actionAdvice);
        writeString(c, "actionAdvice", s.actionAdvice);
    }

    private boolean v20DynamicSupport(V20DynamicCauseSignal s) {
        if (s == null || s.applied == 0 || s.hardRisk == 1) {
            return false;
        }

        if ("S".equals(s.grade) || "A".equals(s.grade)) {
            return s.dynamicScore >= 0.60d && s.dynamicRisk <= 0.54d;
        }

        if (s.dynamicPass == 1 && s.dynamicScore >= 0.58d && s.dynamicRisk <= 0.56d) {
            return true;
        }

        return s.seedPass == 1 && s.rulePass == 1 && s.shadowPass == 1 && s.dynamicRisk <= 0.56d;
    }

    private double coreEvidenceLift(
            Models.ComboCandidate candidate,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory) {
        if (candidate == null) {
            return 0.0d;
        }

        double lift = 0.0d;

        if (candidate.trendLife >= 70.0d) {
            lift += 0.003d;
        }

        if (candidate.trendLife >= AppConfig.TREND_LIFE_HIGH) {
            lift += 0.003d;
        }

        if (candidate.driftRisk <= 35.0d) {
            lift += 0.003d;
        }

        if (candidate.breakDepth <= 38.0d) {
            lift += 0.003d;
        }

        if (candidate.recoveryScore >= 62.0d) {
            lift += 0.003d;
        }

        if (candidate.comboGap >= AppConfig.COMBO_GAP_MIN) {
            lift += 0.002d;
        }

        if (candidate.championGap >= AppConfig.CHAMPION_GAP_MIN) {
            lift += 0.002d;
        }

        if (trace != null && trace.hardBlock == 0 && trace.modeSwitchRisk <= 35.0d) {
            lift += 0.002d;
        }

        try {
            if (recovery != null && recoveryEngine.isSafeRecovery(recovery)) {
                lift += 0.002d;
            }
        } catch (Throwable e) {
        }

        if (similar != null && similar.effectiveSample >= 120 && similar.similarHitRate >= 0.380d) {
            lift += 0.002d;
        }

        if (memory != null && memory.usable == 1 && memory.memoryScore >= 50.0d) {
            lift += 0.001d;
        }

        if (readDouble(candidate, "dynamicCauseScore", 0.0d) >= 0.66d
                && readDouble(candidate, "dynamicRiskScore", 0.0d) <= 0.52d) {
            lift += 0.002d;
        }

        return clamp(lift, 0.0d, 0.022d);
    }

    private double consistencyLift(
            double windowP,
            double shapeP,
            double comboP,
            double recoveryP,
            double similarP,
            double memoryP,
            double stateP,
            double numberShapeP) {
        int support = 0;

        if (windowP >= RAW_BET_FLOOR_NORMAL) support++;
        if (shapeP >= RAW_BET_FLOOR_NORMAL) support++;
        if (comboP >= RAW_BET_FLOOR_NORMAL) support++;
        if (recoveryP >= RAW_BET_FLOOR_NORMAL) support++;
        if (similarP >= RAW_BET_FLOOR_NORMAL) support++;
        if (memoryP >= RAW_BET_FLOOR_NORMAL) support++;
        if (stateP >= RAW_BET_FLOOR_NORMAL) support++;
        if (numberShapeP >= RAW_BET_FLOOR_NORMAL) support++;

        double lift = 0.0d;

        if (support >= 7) {
            lift += 0.006d;
        } else if (support >= 5) {
            lift += 0.004d;
        } else if (support >= 4) {
            lift += 0.002d;
        } else if (support <= 2) {
            lift -= 0.006d;
        }

        return clamp(lift, -0.010d, 0.008d);
    }

    private double evidenceBonus(
            Models.ComboCandidate candidate,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace) {
        double bonus = 0.0d;

        if (candidate == null) {
            return 0.0d;
        }

        if (candidate.championGap >= AppConfig.CHAMPION_GAP_HIGH) {
            bonus += 0.003d;
        }

        if (candidate.comboGap >= AppConfig.COMBO_GAP_HIGH) {
            bonus += 0.004d;
        }

        if (candidate.trendLife >= AppConfig.TREND_LIFE_HIGH
                && candidate.driftRisk <= 28.0d
                && candidate.breakDepth <= 32.0d) {
            bonus += 0.004d;
        }

        if (similar != null
                && similar.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && similar.similarHitRate >= 0.420d
                && similar.avgDistance <= 34.0d) {
            bonus += 0.004d;
        }

        if (memory != null
                && memory.usable == 1
                && memory.memoryScore >= 58.0d
                && memory.recentMemoryRate >= 0.380d) {
            bonus += 0.003d;
        }

        try {
            if (recovery != null
                    && recoveryEngine.isSafeRecovery(recovery)
                    && BreakRecoveryEngine.RECOVERY_STRONG.equals(recovery.recoveryType)) {
                bonus += 0.003d;
            }
        } catch (Throwable e) {
        }

        if (trendTrace != null
                && trendTrace.trendContinuity >= 74.0d
                && trendTrace.modeSwitchRisk <= 30.0d
                && trendTrace.confirmCount >= 2) {
            bonus += 0.003d;
        }

        if (readDouble(candidate, "seedExpandScore", 0.0d) >= 0.66d
                && readDouble(candidate, "seedRisk", 0.0d) <= 0.46d) {
            bonus += 0.002d;
        }

        return clamp(bonus, 0.0d, 0.020d);
    }

    private double evidenceBonusLight(
            Models.ComboCandidate candidate,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace) {
        double bonus = 0.0d;

        if (candidate == null) {
            return 0.0d;
        }

        if (candidate.championGap >= AppConfig.CHAMPION_GAP_HIGH) {
            bonus += 0.002d;
        }

        if (candidate.comboGap >= AppConfig.COMBO_GAP_HIGH) {
            bonus += 0.003d;
        }

        if (candidate.trendLife >= AppConfig.TREND_LIFE_HIGH
                && candidate.driftRisk <= 28.0d
                && candidate.breakDepth <= 32.0d) {
            bonus += 0.003d;
        }

        try {
            if (recovery != null
                    && recoveryEngine.isSafeRecovery(recovery)
                    && BreakRecoveryEngine.RECOVERY_STRONG.equals(recovery.recoveryType)) {
                bonus += 0.002d;
            }
        } catch (Throwable e) {
        }

        if (trendTrace != null
                && trendTrace.trendContinuity >= 74.0d
                && trendTrace.modeSwitchRisk <= 30.0d
                && trendTrace.confirmCount >= 2) {
            bonus += 0.002d;
        }

        if (readDouble(candidate, "dynamicCauseScore", 0.0d) >= 0.66d
                && readDouble(candidate, "dynamicRiskScore", 0.0d) <= 0.52d) {
            bonus += 0.0015d;
        }

        return clamp(bonus, 0.0d, 0.014d);
    }

    private double riskPenalty(
            Models.ComboCandidate candidate,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace,
            Models.ModelWeight weight,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        double penalty = 0.0d;

        if (candidate == null) {
            return 0.050d;
        }

        double driftWeight = weight == null ? 1.0d : weight.driftPenaltyWeight;
        double falseWeight = weight == null ? 1.0d : weight.falseStrongPenaltyWeight;
        double hotWeight = weight == null ? 1.0d : weight.hotReversePenaltyWeight;
        double breakWeight = weight == null ? 1.0d : readDouble(weight, "breakPenaltyWeight", 1.0d);

        if (candidate.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            penalty +=
                    (candidate.driftRisk - AppConfig.DRIFT_RISK_MAX)
                            / 100.0d
                            * 0.050d
                            * driftWeight;
        }

        if (candidate.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            penalty +=
                    (candidate.breakDepth - AppConfig.BREAK_DEPTH_MAX)
                            / 100.0d
                            * 0.052d
                            * breakWeight;
        }

        if (candidate.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            penalty +=
                    (candidate.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX)
                            / 100.0d
                            * 0.040d
                            * hotWeight;
        }

        if (candidate.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            penalty +=
                    (candidate.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX)
                            / 100.0d
                            * 0.040d
                            * falseWeight;
        }

        if (similar != null && similar.effectiveSample >= 160 && similar.similarHitRate < 0.330d) {
            penalty += 0.008d;
        }

        if (memory != null
                && memory.usable == 1
                && memory.memoryScore < 36.0d
                && memory.recentMemoryRate < 0.310d) {
            penalty += 0.008d;
        }

        try {
            if (recovery != null) {
                if (recoveryEngine.isHardBlock(recovery)) {
                    penalty += 0.024d;
                }

                if (recovery.falseRecoveryRisk >= 55.0d) {
                    penalty += 0.010d;
                }
            }
        } catch (Throwable e) {
        }

        if (trendTrace != null) {
            if (trendTrace.hardBlock == 1) {
                penalty += 0.028d;
            }

            if (trendTrace.modeSwitchRisk >= 55.0d) {
                penalty += 0.010d;
            }

            if (trendTrace.earlyBreakRisk >= 55.0d) {
                penalty += 0.010d;
            }
        }

        if (isHardState(candidate.stateMode)) {
            penalty += 0.020d;
        } else if (isChaosState(candidate.stateMode)) {
            penalty += 0.004d;
        }

        if (candidate.profileScore < 0.300d
                && candidate.fallbackProfileScore < 0.300d
                && candidate.evidenceRisk >= 0.620d) {
            penalty += 0.010d;
        }

        if (candidate.auditPenalty >= 0.300d) {
            penalty += 0.008d;
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            if ("RISK".equals(teacherSignal.status)) {
                penalty += 0.008d;
            }

            if (teacherSignal.riskPenalty > 0.010d) {
                penalty += Math.min(0.008d, teacherSignal.riskPenalty);
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            if ("RISK".equals(runtimeSignal.status)) {
                penalty += 0.006d;
            }

            if (runtimeSignal.riskPenalty > 0.010d) {
                penalty += Math.min(0.006d, runtimeSignal.riskPenalty);
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            if ("RISK".equals(stateNumberProfileSignal.status)) {
                penalty += 0.006d;
            }

            if (stateNumberProfileSignal.riskPenalty > 0.010d) {
                penalty += Math.min(0.006d, stateNumberProfileSignal.riskPenalty);
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20Signal.hardRisk == 1) {
                penalty += 0.018d;
            }

            if (v20Signal.dynamicRisk >= 0.64d) {
                penalty += 0.008d;
            }

            if (v20Signal.freezeFlag == 1 || v20Signal.freezeRisk >= 0.62d) {
                penalty += 0.010d;
            }

            if ("C".equals(v20Signal.grade) && v20Signal.dynamicScore < 0.50d) {
                penalty += 0.006d;
            }
        }

        return clamp(penalty, 0.0d, 0.070d);
    }

    private double riskPenaltyLight(
            Models.ComboCandidate candidate,
            BreakRecoveryEngine.RecoveryResult recovery,
            TrendStateMachine.TrendTrace trendTrace,
            Models.ModelWeight weight,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        double penalty = 0.0d;

        if (candidate == null) {
            return 0.045d;
        }

        double driftWeight = weight == null ? 1.0d : weight.driftPenaltyWeight;
        double falseWeight = weight == null ? 1.0d : weight.falseStrongPenaltyWeight;
        double hotWeight = weight == null ? 1.0d : weight.hotReversePenaltyWeight;
        double breakWeight = weight == null ? 1.0d : readDouble(weight, "breakPenaltyWeight", 1.0d);

        if (candidate.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            penalty +=
                    (candidate.driftRisk - AppConfig.DRIFT_RISK_MAX)
                            / 100.0d
                            * 0.045d
                            * driftWeight;
        }

        if (candidate.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            penalty +=
                    (candidate.breakDepth - AppConfig.BREAK_DEPTH_MAX)
                            / 100.0d
                            * 0.048d
                            * breakWeight;
        }

        if (candidate.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            penalty +=
                    (candidate.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX)
                            / 100.0d
                            * 0.035d
                            * hotWeight;
        }

        if (candidate.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            penalty +=
                    (candidate.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX)
                            / 100.0d
                            * 0.035d
                            * falseWeight;
        }

        if (candidate.effectiveSample < 120) {
            penalty += 0.005d;
        }

        try {
            if (recovery != null) {
                if (recoveryEngine.isHardBlock(recovery)) {
                    penalty += 0.022d;
                }

                if (recovery.falseRecoveryRisk >= 55.0d) {
                    penalty += 0.008d;
                }
            }
        } catch (Throwable e) {
        }

        if (trendTrace != null) {
            if (trendTrace.hardBlock == 1) {
                penalty += 0.026d;
            }

            if (trendTrace.modeSwitchRisk >= 55.0d) {
                penalty += 0.008d;
            }

            if (trendTrace.earlyBreakRisk >= 55.0d) {
                penalty += 0.008d;
            }
        }

        if (isHardState(candidate.stateMode)) {
            penalty += 0.016d;
        } else if (isChaosState(candidate.stateMode)) {
            penalty += 0.004d;
        }

        if (candidate.profileScore < 0.300d && candidate.evidenceRisk >= 0.620d) {
            penalty += 0.008d;
        }

        if (teacherSignal != null && "RISK".equals(teacherSignal.status)) {
            penalty += 0.006d;
        }

        if (runtimeSignal != null && "RISK".equals(runtimeSignal.status)) {
            penalty += 0.005d;
        }

        if (stateNumberProfileSignal != null && "RISK".equals(stateNumberProfileSignal.status)) {
            penalty += 0.005d;
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20Signal.hardRisk == 1) {
                penalty += 0.014d;
            }

            if (v20Signal.freezeFlag == 1 || v20Signal.freezeRisk >= 0.64d) {
                penalty += 0.008d;
            }

            if (v20Signal.dynamicRisk >= 0.70d) {
                penalty += 0.006d;
            }
        }

        return clamp(penalty, 0.0d, 0.064d);
    }

    private double lowSampleProtection(
            double raw,
            Models.ComboCandidate candidate,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            List<Models.Draw> history,
            String mode) {
        int historySize = history == null ? 0 : history.size();

        if (historySize < AppConfig.MIN_DRAWS_ANALYZE) {
            return Math.min(raw, 0.365d);
        }

        if (historySize < AppConfig.MIN_DRAWS_SHADOW_ONLY) {
            if (candidate != null && isStrongCandidate(candidate, null, null)) {
                return Math.min(raw, 0.430d);
            }

            return Math.min(raw, 0.415d);
        }

        if (candidate != null && candidate.effectiveSample < 100) {
            raw = Math.min(raw, 0.410d);
        }

        if (similar != null && similar.effectiveSample < 60) {
            raw = Math.min(raw, 0.418d);
        }

        if (memory != null && memory.sample128 < 50) {
            raw = Math.min(raw, 0.420d);
        }

        if (candidate != null && candidate.calibrationError > AppConfig.CALIBRATION_ERROR_HARD) {
            raw = Math.min(raw, 0.410d);
        }

        return raw;
    }

    private double lowSampleProtectionLight(
            double raw, Models.ComboCandidate candidate, List<Models.Draw> history) {
        int historySize = history == null ? 0 : history.size();

        if (historySize < AppConfig.MIN_DRAWS_ANALYZE) {
            return Math.min(raw, 0.365d);
        }

        if (historySize < AppConfig.MIN_DRAWS_SHADOW_ONLY) {
            if (candidate != null && isStrongCandidate(candidate, null, null)) {
                return Math.min(raw, 0.425d);
            }

            return Math.min(raw, 0.410d);
        }

        if (candidate != null && candidate.effectiveSample < 100) {
            raw = Math.min(raw, 0.410d);
        }

        return raw;
    }

    private double blendRawAndCalibrated(
            double raw,
            double calibrated,
            Models.SimilarResult similar,
            ShapeMemoryEngine.MemorySignal memory,
            Models.ComboCandidate candidate) {
        raw = AppConfig.clampProbability(raw);
        calibrated = AppConfig.clampProbability(calibrated);

        double calibratedWeight = 0.26d;

        if (similar != null && similar.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            calibratedWeight += 0.04d;
        }

        if (memory != null && memory.usable == 1 && memory.memoryScore >= 60.0d) {
            calibratedWeight += 0.03d;
        }

        if (Math.abs(calibrated - raw) > AppConfig.CALIBRATION_ERROR_MAX) {
            calibratedWeight -= 0.06d;
        }

        if (candidate != null && candidate.auditPenalty >= 0.250d) {
            calibratedWeight += 0.03d;
        }

        calibratedWeight = clamp(calibratedWeight, 0.18d, 0.38d);

        double finalP = raw * (1.0d - calibratedWeight) + calibrated * calibratedWeight;

        if (candidate != null
                && isStrongCandidate(candidate, null, null)
                && raw >= RAW_BET_FLOOR_STRONG) {
            finalP += 0.002d;
        }

        if (candidate != null && isTeacherSupport(candidate) && raw >= RAW_BET_FLOOR_STRONG) {
            finalP += 0.002d;
        }

        if (candidate != null && isRuntimeSupport(candidate) && raw >= RAW_BET_FLOOR_STRONG) {
            finalP += 0.0015d;
        }

        if (candidate != null && isStateNumberSupport(candidate) && raw >= RAW_BET_FLOOR_STRONG) {
            finalP += 0.0015d;
        }

        if (candidate != null
                && readDouble(candidate, "dynamicCauseScore", 0.0d) >= 0.70d
                && readDouble(candidate, "dynamicRiskScore", 0.0d) <= 0.46d
                && raw >= RAW_BET_FLOOR_STRONG) {
            finalP += 0.0015d;
        }

        if (finalP > raw + MAX_FINAL_ABOVE_RAW) {
            finalP = raw + MAX_FINAL_ABOVE_RAW;
        }

        if (finalP < raw - MAX_FINAL_BELOW_RAW) {
            finalP = raw - MAX_FINAL_BELOW_RAW;
        }

        return AppConfig.clampProbability(finalP);
    }

    private double softCalibratedProbability(
            DbStore store, double rawP, boolean useStoreCalibration) {
        rawP = AppConfig.clampProbability(rawP);

        double fallback = fallbackCalibrated(rawP);
        double calibrated = fallback;

        if (store != null && useStoreCalibration) {
            Object obj =
                    invokeStore(
                            store,
                            "getCalibratedProbability",
                            new Class[] {Double.TYPE},
                            new Object[] {Double.valueOf(rawP)});

            if (obj instanceof Number) {
                double stored = ((Number) obj).doubleValue();

                if (!Double.isNaN(stored) && !Double.isInfinite(stored) && stored > 0.0d) {
                    calibrated = AppConfig.clampProbability(stored);
                }
            }
        }

        double downLimit = softDownLimit(rawP);
        double upLimit = 0.014d;

        if (calibrated < rawP - downLimit) {
            calibrated = rawP - downLimit;
        }

        if (calibrated > rawP + upLimit) {
            calibrated = rawP + upLimit;
        }

        if (calibrated < fallback && fallback >= rawP - downLimit) {
            calibrated = fallback;
        }

        return AppConfig.clampProbability(calibrated);
    }

    private double fallbackCalibrated(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.360d) {
            return rawP;
        }

        if (rawP < 0.380d) {
            return rawP - 0.002d;
        }

        if (rawP < 0.420d) {
            return rawP - 0.004d;
        }

        if (rawP < 0.460d) {
            return rawP - 0.006d;
        }

        return rawP - 0.008d;
    }

    private double softDownLimit(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.360d) {
            return 0.008d;
        }

        if (rawP < 0.400d) {
            return 0.012d;
        }

        if (rawP < 0.440d) {
            return 0.016d;
        }

        return 0.020d;
    }

    private double applyFinalRiskAdjustment(
            double finalP,
            Models.ComboCandidate c,
            HistoricalStateSignal stateSignal,
            LearningSignal learningSignal,
            NumberShapeSignal numberSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal) {
        finalP = AppConfig.clampProbability(finalP);

        double delta = 0.0d;

        if (stateSignal != null) {
            delta += stateSignal.bonus * 0.20d;
            delta -= stateSignal.penalty * 0.45d;
        }

        if (learningSignal != null) {
            delta += learningSignal.bonus * 0.35d;
            delta -= learningSignal.penalty * 0.40d;
        }

        if (numberSignal != null) {
            delta += numberSignal.bonus * 0.12d;
            delta -= numberSignal.penalty * 0.30d;
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            delta += runtimeSignal.bonus * 0.30d;
            delta += runtimeSignal.probAdjust * 0.30d;
            delta -= runtimeSignal.penalty * 0.40d;
            delta -= runtimeSignal.riskPenalty * 0.40d;

            if ("STRONG".equals(runtimeSignal.status)) {
                delta += 0.0015d;
            } else if ("READY".equals(runtimeSignal.status)) {
                delta += 0.001d;
            } else if ("RISK".equals(runtimeSignal.status)) {
                delta -= 0.004d;
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            delta += stateNumberProfileSignal.bonus * 0.28d;
            delta += stateNumberProfileSignal.probAdjust * 0.28d;
            delta -= stateNumberProfileSignal.penalty * 0.38d;
            delta -= stateNumberProfileSignal.riskPenalty * 0.38d;

            if ("STRONG".equals(stateNumberProfileSignal.status)) {
                delta += 0.0015d;
            } else if ("READY".equals(stateNumberProfileSignal.status)) {
                delta += 0.001d;
            } else if ("RISK".equals(stateNumberProfileSignal.status)) {
                delta -= 0.004d;
            }
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            delta += teacherSignal.bonus * 0.35d;
            delta += teacherSignal.probAdjust * 0.35d;
            delta -= teacherSignal.penalty * 0.45d;
            delta -= teacherSignal.riskPenalty * 0.45d;

            if ("STRONG".equals(teacherSignal.status)) {
                delta += 0.002d;
            } else if ("READY".equals(teacherSignal.status)) {
                delta += 0.001d;
            } else if ("RISK".equals(teacherSignal.status)) {
                delta -= 0.005d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            delta += v20Signal.bonus * 0.34d;
            delta += v20Signal.probAdjust * 0.38d;
            delta -= v20Signal.penalty * 0.48d;
            delta -= v20Signal.riskPenalty * 0.50d;

            if ("S".equals(v20Signal.grade)) {
                delta += 0.002d;
            } else if ("A".equals(v20Signal.grade)) {
                delta += 0.001d;
            } else if ("C".equals(v20Signal.grade)) {
                delta -= 0.004d;
            }

            if (v20Signal.hardRisk == 1) {
                delta -= 0.008d;
            }
        }

        if (c != null) {
            boolean support =
                    teacherSignalSupport(teacherSignal)
                            || runtimeSignalSupport(runtimeSignal)
                            || stateNumberSignalSupport(stateNumberProfileSignal)
                            || v20DynamicSupport(v20Signal)
                            || isTeacherSupport(c)
                            || isRuntimeSupport(c)
                            || isStateNumberSupport(c);

            if (c.auditPenalty >= 0.320d && c.finalP < 0.430d) {
                delta -= 0.003d;
            }

            if (c.evidenceRisk >= 0.660d && c.profileScore < 0.500d && !support) {
                delta -= 0.003d;
            }
        }

        delta = clamp(delta, -0.024d, 0.016d);

        return AppConfig.clampProbability(finalP + delta);
    }

    private double applyLearningFinalAdjustment(
            double finalP,
            Models.ComboCandidate c,
            LearningSignal learningSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal) {
        finalP = AppConfig.clampProbability(finalP);

        if (c == null) {
            return finalP;
        }

        double adjust = 0.0d;

        boolean profileStrong =
                c.profileScore >= 0.620d
                        || c.fallbackProfileScore >= 0.620d
                        || c.evidenceScore >= 0.620d
                        || c.deepProfileHitRate >= 0.390d
                        || v20DynamicSupport(v20Signal);

        boolean riskHigh =
                c.evidenceRisk >= 0.660d
                        || c.conflictLevel >= 0.660d
                        || c.auditPenalty >= 0.320d
                        || (v20Signal != null && v20Signal.dynamicRisk >= 0.68d);

        if (profileStrong && !riskHigh) {
            adjust += 0.003d;
        }

        if (learningSignal != null) {
            if (learningSignal.avgSupport >= 0.620d
                    && learningSignal.penalty <= learningSignal.bonus) {
                adjust += 0.002d;
            }

            if (learningSignal.penalty > learningSignal.bonus) {
                adjust -= 0.003d;
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            if ("STRONG".equals(runtimeSignal.status)) {
                adjust += 0.002d;
            } else if ("RISK".equals(runtimeSignal.status)) {
                adjust -= 0.004d;
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            if ("STRONG".equals(stateNumberProfileSignal.status)) {
                adjust += 0.002d;
            } else if ("RISK".equals(stateNumberProfileSignal.status)) {
                adjust -= 0.004d;
            }
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            if ("STRONG".equals(teacherSignal.status)) {
                adjust += 0.002d;
            } else if ("RISK".equals(teacherSignal.status)) {
                adjust -= 0.004d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20DynamicSupport(v20Signal)) {
                adjust += 0.002d;
            }

            if (v20Signal.hardRisk == 1 || v20Signal.freezeFlag == 1) {
                adjust -= 0.006d;
            }
        }

        boolean support =
                teacherSignalSupport(teacherSignal)
                        || runtimeSignalSupport(runtimeSignal)
                        || stateNumberSignalSupport(stateNumberProfileSignal)
                        || v20DynamicSupport(v20Signal)
                        || isTeacherSupport(c)
                        || isRuntimeSupport(c)
                        || isStateNumberSupport(c);

        if (riskHigh && !support) {
            adjust -= 0.004d;
        }

        adjust = clamp(adjust, -0.014d, 0.010d);

        return AppConfig.clampProbability(finalP + adjust);
    }

    private double applyRiskGateAdjustment(
            double gate,
            Models.ComboCandidate c,
            HistoricalStateSignal stateSignal,
            LearningSignal learningSignal,
            NumberShapeSignal numberSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal,
            DbStore store) {
        gate = AppConfig.clampProbability(gate);

        double adjust = 0.0d;

        if (stateSignal != null) {
            if (stateSignal.hitRate32 < 0.300d && stateSignal.hitRate64 < 0.330d) {
                adjust += 0.004d;
            }
        }

        if (learningSignal != null) {
            if (learningSignal.penalty > learningSignal.bonus) {
                adjust += 0.004d;
            } else if (learningSignal.bonus > learningSignal.penalty
                    && c != null
                    && !isHardState(c.stateMode)) {
                adjust -= 0.002d;
            }
        }

        if (numberSignal != null && numberSignal.penalty > numberSignal.bonus) {
            adjust += 0.002d;
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            adjust += runtimeSignal.gateAdjust;

            if ("RISK".equals(runtimeSignal.status)) {
                adjust += 0.004d;
            } else if ("STRONG".equals(runtimeSignal.status)
                    || "READY".equals(runtimeSignal.status)) {
                adjust -= 0.002d;
            }

            if (runtimeSignal.penalty > runtimeSignal.bonus) {
                adjust += 0.002d;
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            adjust += stateNumberProfileSignal.gateAdjust;

            if ("RISK".equals(stateNumberProfileSignal.status)) {
                adjust += 0.004d;
            } else if ("STRONG".equals(stateNumberProfileSignal.status)
                    || "READY".equals(stateNumberProfileSignal.status)) {
                adjust -= 0.002d;
            }

            if (stateNumberProfileSignal.penalty > stateNumberProfileSignal.bonus) {
                adjust += 0.002d;
            }
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            adjust += teacherSignal.gateAdjust;

            if ("RISK".equals(teacherSignal.status)) {
                adjust += 0.004d;
            } else if ("STRONG".equals(teacherSignal.status)
                    || "READY".equals(teacherSignal.status)) {
                adjust -= 0.002d;
            }

            if (teacherSignal.penalty > teacherSignal.bonus) {
                adjust += 0.002d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            adjust += v20Signal.gateAdjust;

            if (v20Signal.hardRisk == 1) {
                adjust += 0.010d;
            } else if (v20DynamicSupport(v20Signal)) {
                adjust -= 0.002d;
            }

            if (v20Signal.freezeFlag == 1 || v20Signal.freezeRisk >= 0.64d) {
                adjust += 0.006d;
            }
        }

        if (c != null) {
            if (c.profileScore < 0.300d && c.evidenceRisk >= 0.660d) {
                adjust += 0.005d;
            }
        }

        adjust = clamp(adjust, -0.016d, 0.026d);
        gate += adjust;

        if (gate < minRealBetP() - 0.010d) {
            gate = minRealBetP() - 0.010d;
        }

        if (gate < 0.345d) {
            gate = 0.345d;
        }

        if (gate > riskGateHigh()) {
            gate = riskGateHigh();
        }

        return AppConfig.clampProbability(gate);
    }

    private double applyLearningGateAdjustment(
            double gate,
            Models.ComboCandidate c,
            LearningSignal learningSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal) {
        gate = AppConfig.clampProbability(gate);

        if (c == null) {
            return gate;
        }

        double adjust = 0.0d;

        boolean hard =
                isHardState(c.stateMode)
                        || c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK
                        || c.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 18.0d
                        || (v20Signal != null && v20Signal.hardRisk == 1);

        if (hard) {
            return gate;
        }

        boolean strong =
                c.profileScore >= 0.620d
                        || c.fallbackProfileScore >= 0.620d
                        || c.evidenceScore >= 0.620d
                        || v20DynamicSupport(v20Signal);

        boolean risk =
                c.evidenceRisk >= 0.660d
                        || c.conflictLevel >= 0.660d
                        || c.auditPenalty >= 0.320d
                        || (v20Signal != null && v20Signal.dynamicRisk >= 0.68d);

        if (strong && !risk) {
            adjust -= 0.003d;
        }

        if (learningSignal != null) {
            if (learningSignal.bonus > learningSignal.penalty
                    && learningSignal.avgSupport >= 0.600d) {
                adjust -= 0.002d;
            } else if (learningSignal.penalty > learningSignal.bonus) {
                adjust += 0.003d;
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            if ("STRONG".equals(runtimeSignal.status)) {
                adjust -= 0.002d;
            } else if ("RISK".equals(runtimeSignal.status)) {
                adjust += 0.004d;
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            if ("STRONG".equals(stateNumberProfileSignal.status)) {
                adjust -= 0.002d;
            } else if ("RISK".equals(stateNumberProfileSignal.status)) {
                adjust += 0.004d;
            }
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            if ("STRONG".equals(teacherSignal.status)) {
                adjust -= 0.002d;
            } else if ("RISK".equals(teacherSignal.status)) {
                adjust += 0.004d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20DynamicSupport(v20Signal)) {
                adjust -= 0.002d;
            }

            if (v20Signal.freezeFlag == 1 || v20Signal.dynamicRisk >= 0.66d) {
                adjust += 0.005d;
            }
        }

        boolean support =
                teacherSignalSupport(teacherSignal)
                        || runtimeSignalSupport(runtimeSignal)
                        || stateNumberSignalSupport(stateNumberProfileSignal)
                        || v20DynamicSupport(v20Signal)
                        || isTeacherSupport(c)
                        || isRuntimeSupport(c)
                        || isStateNumberSupport(c);

        if (risk && !support) {
            adjust += 0.004d;
        }

        adjust = clamp(adjust, -0.012d, 0.018d);

        gate += adjust;

        if (gate < 0.345d) {
            gate = 0.345d;
        }

        if (gate > riskGateHigh()) {
            gate = riskGateHigh();
        }

        return AppConfig.clampProbability(gate);
    }

    private double learningRiskGateFromCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double adjust = 0.0d;

        if (c.profileScore < 0.300d
                && c.fallbackProfileScore < 0.300d
                && c.evidenceRisk >= 0.620d) {
            adjust += 0.006d;
        }

        if (c.auditPenalty >= 0.300d) {
            adjust += 0.004d;
        }

        int teacherSample = readInt(c, "teacherMatrixSample", 0);
        double teacherHitRate = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double teacherConfidence = readDouble(c, "teacherMatrixConfidence", 0.0d);

        if (teacherSample >= TEACHER_STRONG_SAMPLE
                && teacherHitRate > 0.0d
                && teacherHitRate < 0.300d
                && teacherConfidence >= 0.460d) {
            adjust += 0.004d;
        }

        int runtimeSample = readInt(c, "runtimeProfileSample", 0);
        double runtimeHitRate = readDouble(c, "runtimeProfileHitRate", 0.0d);
        double runtimeConfidence = readDouble(c, "runtimeProfileConfidence", 0.0d);

        if (runtimeSample >= 80
                && runtimeHitRate > 0.0d
                && runtimeHitRate < 0.300d
                && runtimeConfidence >= 0.460d) {
            adjust += 0.004d;
        }

        int stateNumberSample = readInt(c, "stateNumberProfileSample", 0);
        double stateNumberHitRate = readDouble(c, "stateNumberProfileHitRate", 0.0d);
        double stateNumberConfidence = readDouble(c, "stateNumberProfileConfidence", 0.0d);

        if (stateNumberSample >= 80
                && stateNumberHitRate > 0.0d
                && stateNumberHitRate < 0.300d
                && stateNumberConfidence >= 0.460d) {
            adjust += 0.004d;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1 || readInt(c, "falseStrongFreeze", 0) == 1) {
            adjust += 0.006d;
        }

        if (readDouble(c, "dynamicRiskScore", 0.0d) >= 0.70d) {
            adjust += 0.005d;
        }

        return clamp(adjust, 0.0d, 0.018d);
    }

    private double learningGateFromCandidate(Models.ComboCandidate c) {
        return learningRiskGateFromCandidate(c);
    }

    private double applyHardProbabilityBounds(
            double raw,
            Models.ComboCandidate candidate,
            List<Models.Draw> history,
            boolean deepEvaluate,
            String mode,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        if (Double.isNaN(raw) || Double.isInfinite(raw)) {
            raw = AppConfig.RANDOM_K3_BASELINE;
        }

        if (raw < RAW_HARD_FLOOR) {
            raw = RAW_HARD_FLOOR;
        }

        if (candidate == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        int historySize = history == null ? 0 : history.size();
        double cap = 0.465d;

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            cap = 0.475d;
        }

        if (!deepEvaluate) {
            cap = 0.445d;
        }

        if (historySize < AppConfig.MIN_DRAWS_SHADOW_ONLY) {
            cap = Math.min(cap, 0.430d);
        }

        if (historySize < AppConfig.MIN_DRAWS_ANALYZE) {
            cap = Math.min(cap, 0.365d);
        }

        boolean support =
                teacherSignalSupport(teacherSignal)
                        || runtimeSignalSupport(runtimeSignal)
                        || stateNumberSignalSupport(stateNumberProfileSignal)
                        || v20DynamicSupport(v20Signal)
                        || isTeacherSupport(candidate)
                        || isRuntimeSupport(candidate)
                        || isStateNumberSupport(candidate);

        if (!isStrongCandidate(candidate, null, null) && !support) {
            cap = Math.min(cap, 0.440d);
        }

        if (isHardState(candidate.stateMode)) {
            cap = Math.min(cap, 0.405d);
        }

        if (candidate.evidenceRisk >= 0.700d && candidate.profileScore < 0.500d && !support) {
            cap = Math.min(cap, 0.415d);
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            if ("RISK".equals(teacherSignal.status)) {
                cap = Math.min(cap, 0.410d);
            } else if ("STRONG".equals(teacherSignal.status) && !isHardState(candidate.stateMode)) {
                cap = Math.max(cap, 0.450d);
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            if ("RISK".equals(runtimeSignal.status)) {
                cap = Math.min(cap, 0.410d);
            } else if ("STRONG".equals(runtimeSignal.status) && !isHardState(candidate.stateMode)) {
                cap = Math.max(cap, 0.448d);
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            if ("RISK".equals(stateNumberProfileSignal.status)) {
                cap = Math.min(cap, 0.410d);
            } else if ("STRONG".equals(stateNumberProfileSignal.status)
                    && !isHardState(candidate.stateMode)) {
                cap = Math.max(cap, 0.448d);
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20Signal.hardRisk == 1 || v20Signal.freezeFlag == 1) {
                cap = Math.min(cap, 0.405d);
            } else if (v20DynamicSupport(v20Signal)) {
                cap = Math.max(cap, 0.450d);
            }

            if (v20Signal.dynamicRisk >= 0.72d) {
                cap = Math.min(cap, 0.410d);
            }
        }

        if (raw > cap) {
            raw = cap;
        }

        return raw;
    }

    private double auditFinalProbability(
            DbStore store,
            Models.ComboCandidate candidate,
            double finalP,
            double rawP,
            double calibrationError,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        finalP = AppConfig.clampProbability(finalP);

        if (candidate == null) {
            return finalP;
        }

        boolean support =
                teacherSignalSupport(teacherSignal)
                        || runtimeSignalSupport(runtimeSignal)
                        || stateNumberSignalSupport(stateNumberProfileSignal)
                        || v20DynamicSupport(v20Signal)
                        || isTeacherSupport(candidate)
                        || isRuntimeSupport(candidate)
                        || isStateNumberSupport(candidate);

        if (calibrationError > AppConfig.CALIBRATION_ERROR_HARD) {
            finalP -= 0.006d;
        } else if (calibrationError > AppConfig.CALIBRATION_ERROR_MAX) {
            finalP -= 0.003d;
        }

        if (candidate.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            finalP = Math.min(finalP, 0.395d);
        }

        if (candidate.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 18.0d) {
            finalP = Math.min(finalP, 0.398d);
        }

        if (isHardState(candidate.stateMode)) {
            finalP = Math.min(finalP, 0.405d);
        }

        if (rawP < RAW_BET_FLOOR_STRONG) {
            finalP = Math.min(finalP, rawP + 0.006d);
        }

        if (candidate.auditPenalty >= 0.360d && finalP < 0.430d) {
            finalP -= 0.006d;
        }

        if (candidate.evidenceRisk >= 0.700d && candidate.profileScore < 0.500d && !support) {
            finalP -= 0.006d;
        }

        if (teacherSignal != null && teacherSignal.applied == 1) {
            if ("RISK".equals(teacherSignal.status)) {
                finalP -= 0.005d;
            } else if ("STRONG".equals(teacherSignal.status)
                    && rawP >= RAW_BET_FLOOR_STRONG
                    && candidate.evidenceRisk <= 0.650d) {
                finalP += 0.002d;
            } else if ("READY".equals(teacherSignal.status)
                    && rawP >= RAW_BET_FLOOR_NORMAL
                    && candidate.evidenceRisk <= 0.640d) {
                finalP += 0.001d;
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            if ("RISK".equals(runtimeSignal.status)) {
                finalP -= 0.004d;
            } else if ("STRONG".equals(runtimeSignal.status)
                    && rawP >= RAW_BET_FLOOR_STRONG
                    && candidate.evidenceRisk <= 0.650d) {
                finalP += 0.0015d;
            } else if ("READY".equals(runtimeSignal.status)
                    && rawP >= RAW_BET_FLOOR_NORMAL
                    && candidate.evidenceRisk <= 0.640d) {
                finalP += 0.001d;
            }
        }

        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            if ("RISK".equals(stateNumberProfileSignal.status)) {
                finalP -= 0.004d;
            } else if ("STRONG".equals(stateNumberProfileSignal.status)
                    && rawP >= RAW_BET_FLOOR_STRONG
                    && candidate.evidenceRisk <= 0.650d) {
                finalP += 0.0015d;
            } else if ("READY".equals(stateNumberProfileSignal.status)
                    && rawP >= RAW_BET_FLOOR_NORMAL
                    && candidate.evidenceRisk <= 0.640d) {
                finalP += 0.001d;
            }
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20Signal.hardRisk == 1) {
                finalP -= 0.010d;
            } else if (v20DynamicSupport(v20Signal) && rawP >= RAW_BET_FLOOR_STRONG) {
                finalP += 0.0015d;
            }

            if (v20Signal.freezeFlag == 1 || v20Signal.freezeRisk >= 0.66d) {
                finalP -= 0.006d;
            }

            if (v20Signal.dynamicRisk >= 0.72d) {
                finalP -= 0.006d;
            }
        }

        if (store != null) {
            Object v =
                    invokeStore(
                            store,
                            "getState",
                            new Class[] {String.class, String.class},
                            new Object[] {"probability_audit_status", ""});

            if (v instanceof String) {
                String audit = (String) v;

                if ("BLOCK".equals(audit) || "ERROR".equals(audit)) {
                    finalP -= 0.006d;
                } else if ("WARN".equals(audit)) {
                    finalP -= 0.003d;
                }
            }
        }

        return AppConfig.clampProbability(finalP);
    }

    private boolean isStrongCandidate(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trace,
            BreakRecoveryEngine.RecoveryResult recovery) {
        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (isHardState(c.stateMode)) {
            return false;
        }

        if (c.trendLife < 55.0d
                && !isTeacherSupport(c)
                && !isRuntimeSupport(c)
                && !isStateNumberSupport(c)
                && readDouble(c, "dynamicCauseScore", 0.0d) < 0.62d) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return false;
        }

        if (c.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && c.trendLife < 70.0d
                && !isTeacherSupport(c)
                && !isRuntimeSupport(c)
                && !isStateNumberSupport(c)
                && readDouble(c, "dynamicCauseScore", 0.0d) < 0.62d) {
            return false;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1 || readInt(c, "falseStrongFreeze", 0) == 1) {
            return false;
        }

        if (trace != null && trace.hardBlock == 1) {
            return false;
        }

        try {
            if (recovery != null && recoveryEngine.isHardBlock(recovery)) {
                return false;
            }
        } catch (Throwable e) {
        }

        return true;
    }

    private double stateGate(Models.ComboCandidate c) {
        if (c == null) {
            return 0.020d;
        }

        double adj = 0.0d;

        try {
            Models.ShapeVector v = vectorFromCandidate(c);
            adj = stateEngine.stateGateAdjustment(v);
        } catch (Throwable e) {
            adj = 0.0d;
        }

        return clamp(adj, -0.010d, 0.026d);
    }

    private Models.ShapeVector vectorFromCandidate(Models.ComboCandidate c) {
        Models.ShapeVector v = new Models.ShapeVector();

        if (c == null) {
            return v;
        }

        v.position = c.position;
        v.baseIssue = c.baseIssue;
        v.targetIssue = c.targetIssue;
        v.createdAt = c.createdAt;
        v.stateMode = c.stateMode;
        v.shapeKey = c.shapeKey;
        v.trendLife = c.trendLife;
        v.driftRisk = c.driftRisk;
        v.breakDepth = c.breakDepth;
        v.recoveryScore = c.recoveryScore;
        v.championGap = c.championGap;
        v.comboGap = c.comboGap;
        v.hotReverseRisk = c.hotReverseRisk;
        v.falseStrongRisk = c.falseStrongRisk;
        v.top3Mass64 = c.comboP;

        writeDouble(v, "teacherMatrixScore", readDouble(c, "teacherMatrixHitRate", 0.0d));
        writeDouble(v, "runtimeProfileScore", readDouble(c, "runtimeProfileHitRate", 0.0d));
        writeDouble(v, "stateNumberProfileScore", readDouble(c, "stateNumberProfileHitRate", 0.0d));
        writeDouble(v, "dynamicCauseScore", readDouble(c, "dynamicCauseScore", 0.0d));
        writeDouble(v, "dynamicRiskScore", readDouble(c, "dynamicRiskScore", 0.0d));

        return v;
    }

    private double safeRate(int hit, int total) {
        if (total <= 0) {
            return 0.0d;
        }
        double v = ((double) hit) / ((double) total);
        return clamp01(v);
    }

    private MainChainProbabilitySignal evaluateMainChainProbabilitySignal(
            DbStore store,
            Models.ComboCandidate c,
            double currentFinalP,
            double rawP,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal) {
        MainChainProbabilitySignal s = new MainChainProbabilitySignal();
        s.beforeP = AppConfig.clampProbability(currentFinalP);
        s.rawP = AppConfig.clampProbability(rawP);
        if (c == null) {
            s.actionAdvice = "WATCH：主链候选为空";
            s.rejectCode = "PROB_MAIN_CHAIN_EMPTY";
            return s;
        }
        double positionNeed =
                maxRead(
                        c,
                        "positionNeedScore",
                        "positionCauseScore",
                        "positionEvidenceScore",
                        "positionHealthScore");
        double numberSupply =
                maxRead(
                        c,
                        "numberSupplyScore",
                        "needSupplyMatchScore",
                        "numberSupplyStrength",
                        "supplyScore");
        double coverage =
                maxRead(
                        c,
                        "coverageQuality",
                        "coverageQualityScore",
                        "finalDecisionCoverageQuality",
                        "k3CoverageQuality");
        double third =
                maxRead(
                        c,
                        "thirdQuality",
                        "thirdNumberQualityScore",
                        "needSupplyThirdQuality",
                        "finalDecisionThirdNumberQuality");
        double ruleHealth = maxRead(c, "ruleHealthScore", "ruleHealth", "ruleScore", "healthScore");
        double shadowScore =
                maxRead(
                        c,
                        "shadowPromoteScore",
                        "shadowPromotionScore",
                        "shadowPromotionBoost",
                        "promotionScore");
        double adaptiveScore =
                maxRead(
                        c,
                        "adaptiveJudgeScore",
                        "adaptiveCauseScore",
                        "adaptivePromoteScore",
                        "adaptiveScore");
        double sampleRel =
                maxRead(
                        c,
                        "sampleReliability",
                        "teacherSampleReliability",
                        "probabilitySampleReliability",
                        "decisionSampleReliability");
        double recentVal =
                maxRead(
                        c,
                        "recentValidity",
                        "teacherRecentValidity",
                        "probabilityRecentValidity",
                        "decisionRecentValidity");
        double v20Evidence =
                maxRead(
                        c,
                        "evidenceScore",
                        "evidenceTotal",
                        "v20EvidenceContribution",
                        "probabilityEvidenceContribution");
        double v20Net =
                readDouble(c, "netEvidenceScore", readDouble(c, "v20NetEvidenceScore", 0.0d));

        double counter =
                maxRead(
                        c,
                        "counterEvidenceScore",
                        "antiEvidenceScore",
                        "probabilityCounterEvidence",
                        "counterfactualRisk");
        double ruleDecay =
                maxRead(c, "ruleDecayRisk", "ruleHealthRisk", "ruleRiskScore", "decayRisk");
        double thirdDrag =
                maxRead(
                        c,
                        "thirdDragRisk",
                        "thirdWeakness",
                        "thirdDragScore",
                        "thirdFalseStrongRisk");
        double counterRisk =
                maxRead(
                        c,
                        "counterfactualRisk",
                        "rankCounterEdge",
                        "replaceGain",
                        "rankConflictRisk");
        double freezeRisk =
                maxRead(
                        c,
                        "freezeRisk",
                        "falseStrongFreezeRisk",
                        "falseStrongRisk",
                        "v20FreezeRisk");

        s.positionNeedScore = clamp01(positionNeed);
        s.numberSupplyScore = clamp01(numberSupply);
        s.coverageQuality = clamp01(coverage);
        s.thirdQuality = clamp01(third);
        s.ruleHealthScore = clamp01(ruleHealth);
        s.shadowPromoteScore = clamp01(shadowScore);
        s.adaptiveScore = clamp01(adaptiveScore);
        s.sampleReliability = clamp01(sampleRel);
        s.recentValidity = clamp01(recentVal);
        s.evidenceScore =
                clamp01(
                        (s.positionNeedScore * 0.16d)
                                + (s.numberSupplyScore * 0.16d)
                                + (s.coverageQuality * 0.14d)
                                + (s.thirdQuality * 0.12d)
                                + (s.ruleHealthScore * 0.12d)
                                + (s.shadowPromoteScore * 0.08d)
                                + (s.adaptiveScore * 0.08d)
                                + (s.sampleReliability * 0.07d)
                                + (s.recentValidity * 0.07d));
        if (v20Evidence > 0.0d) {
            s.evidenceScore = clamp01(s.evidenceScore * 0.82d + clamp01(v20Evidence) * 0.18d);
        }
        if (v20Net > 0.0d) {
            s.evidenceScore = clamp01(s.evidenceScore + Math.min(0.040d, v20Net * 0.040d));
        }
        s.counterScore =
                clamp01(
                        counter * 0.30d
                                + ruleDecay * 0.18d
                                + thirdDrag * 0.18d
                                + counterRisk * 0.18d
                                + freezeRisk * 0.16d);
        /* Gate差不能反向成为Probability反证，否则会形成“门高→P更低→Edge更差”的循环。 */
        if (!AppConfig.PROBABILITY_NEGATIVE_EDGE_IS_NOT_EVIDENCE && c.edge < -0.0005d) {
            s.counterScore = clamp01(s.counterScore + 0.18d);
        }

        String teacherStatus =
                readString(
                        c,
                        "teacherStatus",
                        readString(
                                c,
                                "teacherMatrixStatus",
                                readString(c, "teacherNumberStatus", "")));
        String v20Status =
                readString(
                        c,
                        "v20Status",
                        readString(
                                c,
                                "v20Action",
                                readString(
                                        c, "dynamicAction", readString(c, "dynamicStatus", ""))));
        String adaptiveStatus =
                readString(c, "adaptiveStatus", readString(c, "adaptiveJudgeStatus", ""));
        String shadowStatus = readString(c, "shadowPromotionStatus", "");

        if (isRiskWord(teacherStatus)
                || (teacherSignal != null
                        && ("RISK".equals(teacherSignal.status)
                                || "FREEZE".equals(teacherSignal.status)))) {
            s.counterScore = clamp01(s.counterScore + 0.16d);
            s.rejectCode = "PROB_MAIN_TEACHER_RISK";
        } else if (isTeacherNoSampleNeutral(c) || isNoSampleStatus(teacherStatus)) {
            s.noSampleNeutral = 1;
            s.neutralReasons = appendDiagReason(s.neutralReasons, "Teacher无样本中性");
        }
        if (isRiskWord(v20Status) || (v20Signal != null && v20Signal.hardRisk == 1)) {
            s.counterScore = clamp01(s.counterScore + 0.18d);
            if (s.rejectCode.length() == 0) s.rejectCode = "PROB_MAIN_V20_RISK";
        } else if (isV20WatchOrEmptyNeutral(c)) {
            s.watchNeutral = 1;
            s.neutralReasons = appendDiagReason(s.neutralReasons, "V20 WATCH/空中性");
        }
        if (isRiskWord(adaptiveStatus)) {
            s.counterScore = clamp01(s.counterScore + 0.14d);
            if (s.rejectCode.length() == 0) s.rejectCode = "PROB_MAIN_ADAPTIVE_RISK";
        } else if (isAdaptiveWatchNeutral(c)) {
            s.watchNeutral = 1;
            s.neutralReasons = appendDiagReason(s.neutralReasons, "Adaptive WATCH中性");
        }
        if (containsPromotion(shadowStatus)
                || containsPromotion(readString(c, "shadowPromotionActionAdvice", ""))) {
            s.shadowPromoteScore = Math.max(s.shadowPromoteScore, 0.32d);
            s.evidenceScore =
                    clamp01(s.evidenceScore + Math.min(0.050d, s.shadowPromoteScore * 0.050d));
        }

        s.netEvidenceScore = clamp01(s.evidenceScore - s.counterScore + 0.50d);
        if (s.counterScore >= 0.72d || freezeRisk >= 0.72d) {
            s.hardRisk = 1;
            if (s.rejectCode.length() == 0) s.rejectCode = "PROB_MAIN_HARD_RISK";
        }
        if (s.rejectCode.length() == 0) {
            if (s.noSampleNeutral == 1 || s.watchNeutral == 1)
                s.rejectCode = "PROB_MAIN_NO_SAMPLE_WATCH_NEUTRAL";
            else if (s.evidenceScore >= s.counterScore) s.rejectCode = "PROB_MAIN_OK";
            else s.rejectCode = "PROB_MAIN_COUNTER_DOMINANT";
        }
        double boostBase =
                Math.max(0.0d, s.evidenceScore - 0.58d) * 0.020d
                        + Math.max(0.0d, s.shadowPromoteScore - 0.32d) * 0.010d;
        double discountBase = Math.max(0.0d, s.counterScore - 0.50d) * 0.040d;
        if (s.hardRisk == 1) {
            discountBase = Math.max(discountBase, MAIN_CHAIN_HARD_RISK_DISCOUNT);
        }
        if ((s.noSampleNeutral == 1 || s.watchNeutral == 1) && s.counterScore < 0.62d) {
            discountBase = Math.min(discountBase, MAIN_CHAIN_NEUTRAL_WATCH_DISCOUNT);
        }
        s.probBoost = clamp(boostBase, 0.0d, MAIN_CHAIN_MAX_PROB_BOOST);
        s.probDiscount = clamp(discountBase, 0.0d, MAIN_CHAIN_MAX_PROB_DISCOUNT);
        s.actionAdvice =
                buildMainChainProbabilityAdvice(s, teacherStatus, v20Status, adaptiveStatus);
        s.summary =
                "主链概率融合FIX17｜证据="
                        + probText(s.evidenceScore)
                        + "｜反证="
                        + probText(s.counterScore)
                        + "｜位置="
                        + probText(s.positionNeedScore)
                        + "｜供给="
                        + probText(s.numberSupplyScore)
                        + "｜覆盖="
                        + probText(s.coverageQuality)
                        + "｜第三="
                        + probText(s.thirdQuality)
                        + "｜规则衰减="
                        + probText(ruleDecay)
                        + "｜第三拖拽="
                        + probText(thirdDrag)
                        + "｜SHADOW="
                        + probText(s.shadowPromoteScore)
                        + "｜boost="
                        + probText(s.probBoost)
                        + "｜discount="
                        + probText(s.probDiscount)
                        + "｜"
                        + s.actionAdvice
                        + "｜只修正概率字段，不生成BET";
        return s;
    }

    private double applyMainChainProbabilityAdjustment(
            double finalP, Models.ComboCandidate c, MainChainProbabilitySignal s) {
        finalP = AppConfig.clampProbability(finalP);
        if (s == null) return finalP;
        double adjusted = finalP - s.probDiscount + s.probBoost;
        if (s.hardRisk == 1 && finalP < 0.430d) {
            adjusted =
                    Math.min(
                            adjusted,
                            finalP
                                    - Math.min(
                                            MAIN_CHAIN_MAX_PROB_DISCOUNT,
                                            Math.max(0.010d, s.probDiscount)));
        }
        if ((s.noSampleNeutral == 1 || s.watchNeutral == 1) && s.counterScore < 0.62d) {
            adjusted = Math.max(adjusted, finalP - MAIN_CHAIN_NEUTRAL_WATCH_DISCOUNT);
        }
        if (adjusted > finalP + MAIN_CHAIN_MAX_PROB_BOOST)
            adjusted = finalP + MAIN_CHAIN_MAX_PROB_BOOST;
        if (adjusted < finalP - MAIN_CHAIN_MAX_PROB_DISCOUNT)
            adjusted = finalP - MAIN_CHAIN_MAX_PROB_DISCOUNT;
        return AppConfig.clampProbability(adjusted);
    }

    private void writeMainChainProbabilityFields(
            Models.ComboCandidate c, MainChainProbabilitySignal s, double beforeP, double afterP) {
        if (c == null || s == null) return;
        writeDouble(c, "finalPBeforeMainChain", beforeP);
        writeDouble(c, "finalPAfterMainChain", afterP);
        writeDouble(c, "mainChainAdjustedP", afterP);
        writeDouble(c, "mainChainProbBoost", s.probBoost);
        writeDouble(c, "mainChainProbDiscount", s.probDiscount);
        writeDouble(c, "mainChainEvidenceScore", s.evidenceScore);
        writeDouble(c, "mainChainCounterScore", s.counterScore);
        writeDouble(c, "mainChainNetEvidenceScore", s.netEvidenceScore);
        writeDouble(c, "positionNeedScore", s.positionNeedScore);
        writeDouble(c, "numberSupplyScore", s.numberSupplyScore);
        writeDouble(c, "coverageQuality", s.coverageQuality);
        writeDouble(c, "thirdQuality", s.thirdQuality);
        writeDouble(
                c,
                "probabilitySampleReliability",
                Math.max(readDouble(c, "probabilitySampleReliability", 0.0d), s.sampleReliability));
        writeDouble(
                c,
                "probabilityRecentValidity",
                Math.max(readDouble(c, "probabilityRecentValidity", 0.0d), s.recentValidity));
        writeDouble(
                c,
                "antiEvidenceScore",
                Math.max(readDouble(c, "antiEvidenceScore", 0.0d), s.counterScore));
        writeDouble(
                c,
                "counterEvidenceScore",
                Math.max(readDouble(c, "counterEvidenceScore", 0.0d), s.counterScore));
        writeDouble(
                c,
                "netEvidenceScore",
                Math.max(readDouble(c, "netEvidenceScore", 0.0d), s.netEvidenceScore));
        writeInt(c, "probabilityNoSampleNeutral", s.noSampleNeutral);
        writeInt(c, "probabilityWatchNeutral", s.watchNeutral);
        writeInt(c, "probabilityHardRisk", s.hardRisk);
        writeString(c, "mainChainRejectCode", s.rejectCode);
        writeString(c, "probabilityRejectCode", s.rejectCode);
        writeString(c, "probabilityMainChainSummary", s.summary);
        writeString(c, "probabilityActionAdvice", s.actionAdvice);
        writeString(
                c, "probabilityMainChainVersion", V20_PROBABILITY_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        writeString(c, "fieldChainVersion", V20_PROBABILITY_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        if (s.hardRisk == 1 || s.counterScore >= 0.66d) {
            writeString(c, "rejectCode", s.rejectCode);
            writeString(c, "finalDecisionRejectCode", s.rejectCode);
        }
    }

    private String buildMainChainProbabilityAdvice(
            MainChainProbabilitySignal s,
            String teacherStatus,
            String v20Status,
            String adaptiveStatus) {
        if (s == null) return "WATCH：主链概率为空";
        if (s.hardRisk == 1) return "DOWNGRADE_TO_SHADOW：主链硬风险，只能交最终门收紧";
        if (s.counterScore >= 0.66d) return "WATCH_RISK：反证偏高，Probability只降权不BET";
        if (s.noSampleNeutral == 1 || s.watchNeutral == 1)
            return "WATCH_NEUTRAL：Teacher无样本/V20 WATCH/Adaptive WATCH不硬杀";
        if (s.evidenceScore >= 0.62d && s.counterScore <= 0.48d)
            return "PASS_TO_GATE：主链证据完整，送DecisionGate复核";
        if (s.shadowPromoteScore > 0.0d) return "PROMOTE_WATCH：SHADOW晋级仅小幅加权，仍需最终门";
        return "WATCH：主链概率中性，保持最终门复核";
    }

    private double maxRead(Object obj, String a, String b, String c, String d) {
        double out = 0.0d;
        out = Math.max(out, readDouble(obj, a, 0.0d));
        out = Math.max(out, readDouble(obj, b, 0.0d));
        out = Math.max(out, readDouble(obj, c, 0.0d));
        out = Math.max(out, readDouble(obj, d, 0.0d));
        return clamp01(out);
    }

    private String upper(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
    }

    private boolean isNoSampleWatchNeutralForProbability(Models.ComboCandidate c) {
        return isTeacherNoSampleNeutral(c)
                || isV20WatchOrEmptyNeutral(c)
                || isAdaptiveWatchNeutral(c);
    }

    private boolean isTeacherNoSampleNeutral(Models.ComboCandidate c) {
        if (c == null) return false;
        String s =
                upper(
                        readString(
                                c,
                                "teacherStatus",
                                readString(
                                        c,
                                        "teacherMatrixStatus",
                                        readString(c, "teacherNumberStatus", ""))));
        int sample =
                Math.max(readInt(c, "teacherMatrixSample", 0), readInt(c, "teacherSampleCount", 0));
        if (sample <= 0 && (s.length() == 0 || "空".equals(s))) return true;
        return isNoSampleStatus(s);
    }

    private boolean isNoSampleStatus(String status) {
        String s = upper(status);
        return s.length() == 0
                || "空".equals(s)
                || s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("NO SAMPLE") >= 0
                || s.indexOf("NONE") >= 0
                || s.indexOf("EMPTY") >= 0;
    }

    private boolean isV20WatchOrEmptyNeutral(Models.ComboCandidate c) {
        if (c == null) return false;
        String s =
                upper(
                        readString(
                                c,
                                "v20Status",
                                readString(
                                        c,
                                        "v20Action",
                                        readString(
                                                c,
                                                "dynamicAction",
                                                readString(c, "dynamicStatus", "")))));
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("HARD") >= 0
                || s.indexOf("冻结") >= 0
                || s.indexOf("风险") >= 0) return false;
        return s.length() == 0
                || "空".equals(s)
                || s.indexOf("WATCH") >= 0
                || s.indexOf("UNKNOWN") >= 0
                || s.indexOf("NEUTRAL") >= 0;
    }

    private boolean isAdaptiveWatchNeutral(Models.ComboCandidate c) {
        if (c == null) return false;
        String s =
                upper(
                        readString(
                                c,
                                "adaptiveStatus",
                                readString(
                                        c,
                                        "adaptiveJudgeStatus",
                                        readString(c, "adaptiveAction", ""))));
        if (s.indexOf("RISK") >= 0 || s.indexOf("FREEZE") >= 0 || s.indexOf("REJECT") >= 0)
            return false;
        return s.length() == 0
                || s.indexOf("WATCH") >= 0
                || s.indexOf("NEUTRAL") >= 0
                || s.indexOf("PENDING") >= 0;
    }

    private boolean isRiskWord(String status) {
        return AppConfig.isExplicitHardBlockStatus(status);
    }

    private CoverageQualitySignal evaluateK3CoverageQualitySignal(
            Models.ComboCandidate c,
            List<Models.Draw> history,
            TeacherMatrixSignal teacherSignal,
            V20DynamicCauseSignal v20Signal,
            double currentFinalP) {
        CoverageQualitySignal s = new CoverageQualitySignal();
        s.beforeP = AppConfig.clampProbability(currentFinalP);
        if (c == null || !c.isValidK3()) {
            s.coverageSummary = "K3覆盖质量：候选无效";
            s.actionAdvice = "HOLD";
            s.falseHighPFlag = 1;
            s.k3CoverageRisk = 1.0d;
            return s;
        }

        double p32 = recentK3HitRate(c, history, 32);
        double p64 = recentK3HitRate(c, history, 64);
        double p128 = recentK3HitRate(c, history, 128);
        double n1Rate = recentNumberHitRate(c.position, c.n1, history, 96);
        double n2Rate = recentNumberHitRate(c.position, c.n2, history, 96);
        double n3Rate = recentNumberHitRate(c.position, c.n3, history, 96);
        int sample128 = recentK3Sample(c, history, 128);
        double n1Quality = nonEmptyNumberQuality(n1Rate);
        double n2Quality = nonEmptyNumberQuality(n2Rate);
        double n3Quality = nonEmptyNumberQuality(n3Rate);
        double third = Math.min(n1Quality, Math.min(n2Quality, n3Quality));
        double avgNumber = clamp01((n1Quality + n2Quality + n3Quality) / 3.0d);
        double k3Blend = clamp01(p32 * 0.42d + p64 * 0.36d + p128 * 0.22d);
        double coverageBase = Math.max(COVERAGE_MIN_NONEMPTY_K3_QUALITY, k3Blend);
        double balance = numberBalanceScore(n1Quality, n2Quality, n3Quality);
        double edge = currentFinalP - readDouble(c, "dynamicGate", c.dynamicGate);
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = readDouble(c, "edge", c.edge);
        }

        s.k3HitRate32 = p32;
        s.k3HitRate64 = p64;
        s.k3HitRate128 = p128;
        s.numberRate1 = n1Rate;
        s.numberRate2 = n2Rate;
        s.numberRate3 = n3Rate;
        s.thirdNumberQualityScore = clamp01(third);
        s.coverageQualityScore =
                clamp01(coverageBase * 0.46d + avgNumber * 1.35d + third * 1.35d + balance * 0.18d);
        if (s.coverageQualityScore <= 0.0001d) {
            s.coverageQualityScore = COVERAGE_MIN_NONEMPTY_K3_QUALITY;
        }
        if (s.thirdNumberQualityScore <= 0.0001d) {
            s.thirdNumberQualityScore = COVERAGE_MIN_NONEMPTY_THIRD_QUALITY;
        }
        s.preferredEdgeBand = edge >= PREFERRED_EDGE_MIN && edge <= PREFERRED_EDGE_MAX ? 1 : 0;
        s.highEdgeFlag = edge > PREFERRED_EDGE_MAX ? 1 : 0;
        s.preferredProbabilityBand =
                currentFinalP >= PREFERRED_P_MIN && currentFinalP <= PREFERRED_P_MAX ? 1 : 0;
        s.teacherWeakOrEmpty = teacherWeakOrEmptyForCoverage(c, teacherSignal) ? 1 : 0;
        s.v20UnknownOrEmpty = v20UnknownOrEmptyForCoverage(c, v20Signal) ? 1 : 0;
        s.hasStrongCause = hasTeacherOrV20StrongForCoverage(c, teacherSignal, v20Signal) ? 1 : 0;

        double risk = 0.0d;
        risk += clamp01((COVERAGE_QUALITY_READY - s.coverageQualityScore) * 2.0d) * 0.30d;
        risk += clamp01((THIRD_NUMBER_QUALITY_MIN - third) * 8.0d) * 0.22d;
        risk += clamp01((BASE_K3_RATE - k3Blend) * 2.0d) * 0.16d;
        if (sample128 < 30) risk += 0.18d;
        else if (sample128 < 80) risk += 0.08d;
        if (s.teacherWeakOrEmpty == 1
                && s.v20UnknownOrEmpty == 1
                && !isNoSampleWatchNeutralForProbability(c)) risk += 0.10d;
        if (s.highEdgeFlag == 1 && s.hasStrongCause == 0) risk += 0.18d;
        if (currentFinalP >= 0.400d && s.hasStrongCause == 0) risk += 0.16d;
        if (readDouble(c, "probabilityCounterEvidence", 0.0d) >= 0.62d) risk += 0.08d;
        s.k3CoverageRisk = clamp01(risk);

        if ((currentFinalP >= 0.400d && s.hasStrongCause == 0)
                || (s.highEdgeFlag == 1
                        && s.teacherWeakOrEmpty == 1
                        && s.v20UnknownOrEmpty == 1
                        && !isNoSampleWatchNeutralForProbability(c))
                || (s.k3CoverageRisk >= K3_COVERAGE_RISK_LINE && currentFinalP >= 0.380d)) {
            s.falseHighPFlag = 1;
        }

        double discount = 0.0d;
        if (s.thirdNumberQualityScore < THIRD_NUMBER_QUALITY_MIN) discount += 0.006d;
        if (sample128 < 30) discount += 0.006d;
        if (s.k3CoverageRisk >= K3_COVERAGE_RISK_LINE) discount += 0.008d;
        if (s.falseHighPFlag == 1) discount += 0.010d;
        if (s.teacherWeakOrEmpty == 1
                && s.v20UnknownOrEmpty == 1
                && currentFinalP >= 0.380d
                && !isNoSampleWatchNeutralForProbability(c)) discount += 0.004d;
        if (currentFinalP >= 0.400d && s.hasStrongCause == 0) discount += 0.006d;
        s.coverageDiscount = clamp(discount, 0.0d, MAX_COVERAGE_PROB_DISCOUNT);

        double boost = 0.0d;
        if (s.preferredProbabilityBand == 1
                && s.preferredEdgeBand == 1
                && s.k3CoverageRisk < 0.46d
                && s.thirdNumberQualityScore >= THIRD_NUMBER_QUALITY_READY
                && (s.hasStrongCause == 1 || readDouble(c, "shadowPromotionBoost", 0.0d) > 0.0d)) {
            boost += 0.004d;
        }
        if (s.coverageQualityScore >= COVERAGE_QUALITY_STRONG
                && s.k3CoverageRisk < 0.42d
                && s.hasStrongCause == 1) boost += 0.002d;
        s.coverageBoost = clamp(boost, 0.0d, MAX_COVERAGE_PROB_BOOST);
        s.coverageSummary = buildCoverageQualitySummary(c, s, edge);
        s.actionAdvice = buildCoverageActionAdvice(s);
        return s;
    }

    private double applyCoverageQualityProbabilityAdjustment(
            double finalP, Models.ComboCandidate c, CoverageQualitySignal s) {
        finalP = AppConfig.clampProbability(finalP);
        if (s == null) return finalP;
        double adjusted = finalP - s.coverageDiscount + s.coverageBoost;
        if (s.falseHighPFlag == 1 && finalP >= 0.400d && s.hasStrongCause == 0)
            adjusted = Math.min(adjusted, 0.396d);
        if (s.teacherWeakOrEmpty == 1
                && s.v20UnknownOrEmpty == 1
                && s.highEdgeFlag == 1
                && !isNoSampleWatchNeutralForProbability(c))
            adjusted = Math.min(adjusted, finalP - 0.008d);
        if (s.k3CoverageRisk >= 0.70d && finalP < 0.425d)
            adjusted = Math.min(adjusted, finalP - 0.014d);
        double maxUp = finalP + MAX_COVERAGE_PROB_BOOST;
        double maxDown = finalP - MAX_COVERAGE_PROB_DISCOUNT;
        if (adjusted > maxUp) adjusted = maxUp;
        if (adjusted < maxDown) adjusted = maxDown;
        return AppConfig.clampProbability(adjusted);
    }

    private void writeCoverageQualityFields(
            Models.ComboCandidate c, CoverageQualitySignal s, double beforeP, double afterP) {
        if (c == null || s == null) return;
        writeDouble(c, "finalPBeforeCoverage", beforeP);
        writeDouble(c, "finalPAfterCoverage", afterP);
        writeDouble(c, "coverageAdjustedP", afterP);
        writeDouble(c, "coverageQualityScore", s.coverageQualityScore);
        writeDouble(c, "thirdNumberQualityScore", s.thirdNumberQualityScore);
        writeDouble(c, "k3CoverageRisk", s.k3CoverageRisk);
        writeDouble(c, "dynamicGateCoverageQualityScore", s.coverageQualityScore);
        writeDouble(c, "dynamicGateThirdNumberQuality", s.thirdNumberQualityScore);
        writeDouble(c, "dynamicGateCoverageRisk", s.k3CoverageRisk);
        writeDouble(c, "finalDecisionCoverageQuality", s.coverageQualityScore);
        writeDouble(c, "finalDecisionThirdNumberQuality", s.thirdNumberQualityScore);
        writeDouble(c, "finalDecisionCoverageRisk", s.k3CoverageRisk);
        writeDouble(c, "probabilityFalseHighRisk", s.k3CoverageRisk);
        writeInt(c, "falseHighPFlag", s.falseHighPFlag);
        writeInt(c, "probabilityFalseHighPFlag", s.falseHighPFlag);
        writeInt(c, "dynamicGateFalseHighPFlag", s.falseHighPFlag);
        writeInt(c, "finalDecisionFalseHighPFlag", s.falseHighPFlag);
        writeInt(c, "probabilityPreferredEdgeBand", s.preferredEdgeBand);
        writeInt(c, "dynamicGatePreferredEdgeBand", s.preferredEdgeBand);
        writeInt(c, "probabilityHighEdgeFlag", s.highEdgeFlag);
        writeInt(c, "probabilityPreferredPBand", s.preferredProbabilityBand);
        writeInt(c, "probabilityTeacherWeakOrEmpty", s.teacherWeakOrEmpty);
        writeInt(c, "probabilityV20UnknownOrEmpty", s.v20UnknownOrEmpty);
        writeInt(c, "probabilityStrongCauseFlag", s.hasStrongCause);
        writeDouble(c, "coverageProbDiscount", s.coverageDiscount);
        writeDouble(c, "coverageProbBoost", s.coverageBoost);
        writeDouble(c, "coverageK3HitRate32", s.k3HitRate32);
        writeDouble(c, "coverageK3HitRate64", s.k3HitRate64);
        writeDouble(c, "coverageK3HitRate128", s.k3HitRate128);
        writeDouble(c, "coverageNumberRate1", s.numberRate1);
        writeDouble(c, "coverageNumberRate2", s.numberRate2);
        writeDouble(c, "coverageNumberRate3", s.numberRate3);
        writeString(c, "probabilityCoverageSummary", s.coverageSummary);
        writeString(c, "coverageQualitySummary", s.coverageSummary);
        writeString(c, "probabilityCoverageActionAdvice", s.actionAdvice);
        String oldAdvice = readString(c, "probabilityActionAdvice", "");
        if (oldAdvice.length() == 0
                || s.falseHighPFlag == 1
                || s.k3CoverageRisk >= K3_COVERAGE_RISK_LINE) {
            writeString(c, "probabilityActionAdvice", s.actionAdvice);
            writeString(c, "actionAdvice", s.actionAdvice);
        }
        writeString(c, "probabilityFieldVersion", V20_PROBABILITY_UPSTREAM_VERSION);
        writeString(c, "probabilityUpstreamVersion", V20_PROBABILITY_UPSTREAM_VERSION);
    }

    private void ensureCoverageFieldsNeverEmpty(
            Models.ComboCandidate c, List<Models.Draw> history, String stage) {
        if (c == null || !c.isValidK3()) {
            return;
        }
        double cover = readDouble(c, "coverageQualityScore", 0.0d);
        double risk = readDouble(c, "k3CoverageRisk", 0.0d);
        double third = readDouble(c, "thirdNumberQualityScore", 0.0d);
        if (cover > 0.0001d && third > 0.0001d && risk > 0.0001d) {
            writeCoverageAliases(c, cover, third, risk);
            return;
        }
        double p32 = recentK3HitRate(c, history, 32);
        double p64 = recentK3HitRate(c, history, 64);
        double p128 = recentK3HitRate(c, history, 128);
        double n1 = recentNumberHitRate(c.position, c.n1, history, 96);
        double n2 = recentNumberHitRate(c.position, c.n2, history, 96);
        double n3 = recentNumberHitRate(c.position, c.n3, history, 96);
        double n1q = nonEmptyNumberQuality(n1);
        double n2q = nonEmptyNumberQuality(n2);
        double n3q = nonEmptyNumberQuality(n3);
        double thirdQ = clamp01(Math.min(n1q, Math.min(n2q, n3q)));
        double avg = clamp01((n1q + n2q + n3q) / 3.0d);
        double k3 = clamp01(p32 * 0.42d + p64 * 0.36d + p128 * 0.22d);
        double coverQ =
                clamp01(
                        Math.max(COVERAGE_MIN_NONEMPTY_K3_QUALITY, k3) * 0.46d
                                + avg * 1.35d
                                + thirdQ * 1.35d
                                + numberBalanceScore(n1q, n2q, n3q) * 0.18d);
        if (coverQ <= 0.0001d) coverQ = COVERAGE_MIN_NONEMPTY_K3_QUALITY;
        if (thirdQ <= 0.0001d) thirdQ = COVERAGE_MIN_NONEMPTY_THIRD_QUALITY;
        int sample = recentK3Sample(c, history, 128);
        double riskQ = risk > 0.0001d ? risk : buildCoverageFallbackRisk(sample, coverQ, thirdQ);
        writeCoverageAliases(c, coverQ, thirdQ, riskQ);
        writeDouble(c, "coverageK3HitRate32", p32);
        writeDouble(c, "coverageK3HitRate64", p64);
        writeDouble(c, "coverageK3HitRate128", p128);
        writeDouble(c, "coverageNumberRate1", n1);
        writeDouble(c, "coverageNumberRate2", n2);
        writeDouble(c, "coverageNumberRate3", n3);
        String summary =
                "Probability覆盖补实"
                        + (stage == null ? "" : stage)
                        + "：K3近32/64/128="
                        + probText(p32)
                        + "/"
                        + probText(p64)
                        + "/"
                        + probText(p128)
                        + "，三号率="
                        + probText(n1)
                        + "/"
                        + probText(n2)
                        + "/"
                        + probText(n3)
                        + "，Cover="
                        + probText(coverQ)
                        + "，Third="
                        + probText(thirdQ)
                        + "，Risk="
                        + probText(riskQ)
                        + "；只补字段，不直接BET";
        writeString(c, "probabilityCoverageSummary", summary);
        writeString(c, "coverageQualitySummary", summary);
        writeString(
                c,
                "probabilityCoverageActionAdvice",
                riskQ >= K3_COVERAGE_RISK_LINE
                        ? "DOWNGRADE_TO_SHADOW：覆盖风险偏高"
                        : "WATCH_TO_GATE：覆盖字段已补实，交最终门复核");
        writeString(
                c, "probabilityActionAdvice", readString(c, "probabilityCoverageActionAdvice", ""));
        writeString(c, "probabilityFieldVersion", V20_PROBABILITY_UPSTREAM_VERSION);
        if (shouldMarkFallbackFalseHigh(c, coverQ, thirdQ, riskQ)) {
            writeInt(c, "falseHighPFlag", 1);
            writeInt(c, "probabilityFalseHighPFlag", 1);
            writeInt(c, "dynamicGateFalseHighPFlag", 1);
            writeInt(c, "finalDecisionFalseHighPFlag", 1);
            writeDouble(c, "probabilityFalseHighRisk", Math.max(0.70d, riskQ));
        }
    }

    private void writeCoverageAliases(
            Models.ComboCandidate c, double cover, double third, double risk) {
        if (c == null) return;
        cover = clamp01(cover);
        third = clamp01(third);
        risk = clamp01(risk);
        writeDouble(c, "coverageQualityScore", cover);
        writeDouble(c, "thirdNumberQualityScore", third);
        writeDouble(c, "k3CoverageRisk", risk);
        writeDouble(c, "dynamicGateCoverageQualityScore", cover);
        writeDouble(c, "dynamicGateThirdNumberQuality", third);
        writeDouble(c, "dynamicGateCoverageRisk", risk);
        writeDouble(c, "finalDecisionCoverageQuality", cover);
        writeDouble(c, "finalDecisionThirdNumberQuality", third);
        writeDouble(c, "finalDecisionCoverageRisk", risk);
    }

    private double nonEmptyNumberQuality(double rate) {
        if (Double.isNaN(rate) || Double.isInfinite(rate))
            return COVERAGE_MIN_NONEMPTY_THIRD_QUALITY;
        if (rate <= 0.0001d) return COVERAGE_MIN_NONEMPTY_THIRD_QUALITY;
        return clamp01(rate);
    }

    private int recentK3Sample(Models.ComboCandidate c, List<Models.Draw> history, int window) {
        if (c == null || history == null || history.size() == 0 || window <= 0) return 0;
        int total = 0;
        int start = history.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(n)) continue;
            total++;
        }
        return total;
    }

    private double buildCoverageFallbackRisk(int sample, double cover, double third) {
        double risk = 0.0d;
        if (sample < 30) risk += COVERAGE_SAMPLE_RISK_LINE;
        else if (sample < 80) risk += 0.48d;
        else risk += 0.30d;
        if (cover < COVERAGE_QUALITY_READY) risk += 0.12d;
        if (third < THIRD_NUMBER_QUALITY_MIN) risk += 0.18d;
        return clamp01(risk);
    }

    private boolean shouldMarkFallbackFalseHigh(
            Models.ComboCandidate c, double cover, double third, double risk) {
        if (c == null) return false;
        double edge = c.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge) || Math.abs(edge) > 1.0d) {
            edge = c.finalP - c.dynamicGate;
        }
        int strong = readInt(c, "probabilityStrongCauseFlag", 0);
        if (strong <= 0 && c.finalP >= 0.400d) return true;
        if (edge > PREFERRED_EDGE_MAX && strong <= 0) return true;
        if (risk >= K3_COVERAGE_RISK_LINE && c.finalP >= 0.380d) return true;
        return cover <= COVERAGE_MIN_NONEMPTY_K3_QUALITY || third < THIRD_NUMBER_QUALITY_MIN;
    }

    private String buildCoverageRuntimeSummary(Models.ComboCandidate c) {
        if (c == null) return "";
        return "P"
                + c.position
                + " Cover="
                + probText(readDouble(c, "coverageQualityScore", 0.0d))
                + "/"
                + probText(readDouble(c, "k3CoverageRisk", 0.0d))
                + " Third="
                + probText(readDouble(c, "thirdNumberQualityScore", 0.0d))
                + " FalseHigh="
                + readInt(c, "falseHighPFlag", 0)
                + " V="
                + V20_PROBABILITY_UPSTREAM_VERSION;
    }

    private String buildCoverageQualitySummary(
            Models.ComboCandidate c, CoverageQualitySignal s, double edge) {
        StringBuilder sb = new StringBuilder();
        sb.append("K3覆盖质量：P").append(c == null ? 0 : c.position);
        sb.append("｜K3近32/64/128=")
                .append(probText(s.k3HitRate32))
                .append("/")
                .append(probText(s.k3HitRate64))
                .append("/")
                .append(probText(s.k3HitRate128));
        sb.append("｜三号率=")
                .append(probText(s.numberRate1))
                .append("/")
                .append(probText(s.numberRate2))
                .append("/")
                .append(probText(s.numberRate3));
        sb.append("｜第三号质量=").append(probText(s.thirdNumberQualityScore));
        sb.append("｜覆盖质=").append(probText(s.coverageQualityScore));
        sb.append("｜覆盖风险=").append(probText(s.k3CoverageRisk));
        sb.append("｜Edge=").append(probText(edge));
        if (s.preferredProbabilityBand == 1 && s.preferredEdgeBand == 1)
            sb.append("｜优先域=P38-40/Edge1-2");
        if (s.highEdgeFlag == 1) sb.append("｜Edge>2需假强复核");
        if (s.falseHighPFlag == 1) sb.append("｜假高P");
        if (s.teacherWeakOrEmpty == 1 && s.v20UnknownOrEmpty == 1) sb.append("｜Teacher弱且V20未知");
        return sb.toString();
    }

    private String buildCoverageActionAdvice(CoverageQualitySignal s) {
        if (s == null) return "HOLD：覆盖质量为空";
        if (s.falseHighPFlag == 1 || s.k3CoverageRisk >= 0.70d)
            return "DOWNGRADE_TO_SHADOW：FinalP/Edge疑似假强，K3覆盖质量不足";
        if (s.teacherWeakOrEmpty == 1 && s.v20UnknownOrEmpty == 1)
            return "WATCH_NO_SAMPLE_NEUTRAL：Teacher/V20弱或空只做观察，不由Probability硬杀，交最终门复核";
        if (s.preferredProbabilityBand == 1 && s.preferredEdgeBand == 1 && s.k3CoverageRisk < 0.50d)
            return "WATCH_TO_GATE：P38-40且Edge1-2，覆盖风险可控，交最终门复核";
        if (s.highEdgeFlag == 1) return "WATCH_FALSE_HIGH：Edge>=2不自动强，需V20/Teacher/SHADOW确认";
        return "WATCH：覆盖质量中性，保持最终门复核";
    }

    private double recentK3HitRate(Models.ComboCandidate c, List<Models.Draw> history, int window) {
        if (c == null || history == null || history.size() == 0 || window <= 0)
            return AppConfig.RANDOM_K3_BASELINE;
        int total = 0, hit = 0, start = history.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(c.position);
            if (!AppConfig.isValidNumber(n)) continue;
            total++;
            if (containsNumber(c.k3Mask, n)) hit++;
        }
        return total <= 0 ? AppConfig.RANDOM_K3_BASELINE : safeRate(hit, total);
    }

    private double recentNumberHitRate(
            int position, int number, List<Models.Draw> history, int window) {
        if (!AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)
                || history == null
                || history.size() == 0
                || window <= 0) return BASE_NUMBER_RATE;
        int total = 0, hit = 0, start = history.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) continue;
            int n = d.getNumberAtPosition(position);
            if (!AppConfig.isValidNumber(n)) continue;
            total++;
            if (n == number) hit++;
        }
        return total <= 0 ? BASE_NUMBER_RATE : safeRate(hit, total);
    }

    private double numberBalanceScore(double a, double b, double c) {
        double max = Math.max(a, Math.max(b, c));
        double min = Math.min(a, Math.min(b, c));
        return clamp01(1.0d - (max - min) * 4.0d);
    }

    private boolean teacherWeakOrEmptyForCoverage(Models.ComboCandidate c, TeacherMatrixSignal t) {
        String status = t == null ? "" : t.status;
        if (status == null || status.length() == 0 || "NONE".equals(status))
            status = readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""));
        if (status == null || status.length() == 0) return true;
        status = status.toUpperCase(java.util.Locale.ROOT);
        return status.indexOf("STRONG") < 0 && status.indexOf("READY") < 0;
    }

    private boolean v20UnknownOrEmptyForCoverage(Models.ComboCandidate c, V20DynamicCauseSignal v) {
        if (v == null || v.applied == 0) {
            String status =
                    readString(
                            c,
                            "v20Status",
                            readString(c, "dynamicStatus", readString(c, "dynamicGrade", "")));
            if (status == null || status.length() == 0) return true;
            String s = status.toUpperCase(java.util.Locale.ROOT);
            return s.indexOf("STRONG") < 0
                    && s.indexOf("READY") < 0
                    && s.indexOf("PASS") < 0
                    && s.indexOf("V20_STRONG") < 0
                    && s.indexOf("V20_READY") < 0;
        }
        if (v.grade == null || v.grade.length() == 0)
            return v.dynamicScore < 0.58d || v.counterEvidence >= 0.58d;
        String g = v.grade.toUpperCase(java.util.Locale.ROOT);
        if (g.indexOf("S") >= 0
                || g.indexOf("A") >= 0
                || g.indexOf("STRONG") >= 0
                || g.indexOf("READY") >= 0) return false;
        return v.dynamicScore < 0.58d || v.counterEvidence >= 0.58d;
    }

    private boolean hasTeacherOrV20StrongForCoverage(
            Models.ComboCandidate c, TeacherMatrixSignal t, V20DynamicCauseSignal v) {
        if (teacherSignalSupport(t) || v20DynamicSupport(v)) return true;
        String teacher = readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""));
        String v20 =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicGrade", "")));
        if (teacher != null) {
            String s = teacher.toUpperCase(java.util.Locale.ROOT);
            if (s.indexOf("STRONG") >= 0 || s.indexOf("READY") >= 0) return true;
        }
        if (v20 != null) {
            String s = v20.toUpperCase(java.util.Locale.ROOT);
            if (s.indexOf("STRONG") >= 0
                    || s.indexOf("READY") >= 0
                    || s.indexOf("PASS") >= 0
                    || "S".equals(s)
                    || "A".equals(s)) return true;
        }
        return readDouble(c, "shadowPromotionBoost", 0.0d) > 0.0d
                && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.55d;
    }

    private boolean coveragePreferredDomain(Models.ComboCandidate c) {
        if (c == null) return false;
        return readInt(c, "probabilityPreferredPBand", 0) == 1
                && readInt(c, "probabilityPreferredEdgeBand", 0) == 1
                && readDouble(c, "k3CoverageRisk", 1.0d) < 0.50d
                && readDouble(c, "thirdNumberQualityScore", 0.0d) >= THIRD_NUMBER_QUALITY_READY;
    }

    private DiagnosisProbabilitySignal evaluateDiagnosisProbabilitySignal(
            DbStore store,
            Models.ComboCandidate c,
            double currentFinalP,
            double rawP,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        DiagnosisProbabilitySignal s = new DiagnosisProbabilitySignal();
        s.beforeP = AppConfig.clampProbability(currentFinalP);
        s.rawP = AppConfig.clampProbability(rawP);
        if (c == null) {
            s.summary = "概率诊断：候选为空";
            s.actionAdvice = "HOLD";
            return s;
        }

        double evidence = 0.0d;
        double counter = 0.0d;
        int evidenceCount = 0;
        int counterCount = 0;

        if (teacherSignal != null
                && teacherSignal.applied == 1
                && !isTeacherSignalNoSampleNeutral(teacherSignal)) {
            double teacherReliability =
                    sampleReliabilityFromSample(
                            Math.max(teacherSignal.sample, teacherSignal.comboSample));
            double teacherRecent = Math.max(teacherSignal.hitRate, teacherSignal.comboHitRate);
            s.sampleReliability = Math.max(s.sampleReliability, teacherReliability);
            s.recentValidity = Math.max(s.recentValidity, teacherRecent);
            evidence +=
                    clamp01(
                            teacherSignal.bonus
                                    + teacherSignal.probAdjust
                                    + teacherSignal.hitRate * 0.18d
                                    + teacherSignal.confidence * 0.10d);
            evidenceCount++;
            counter +=
                    clamp01(
                            teacherSignal.penalty
                                    + teacherSignal.riskPenalty
                                    + (1.0d - teacherSignal.confidence) * 0.12d);
            counterCount++;
            if ("FREEZE".equals(teacherSignal.status)) {
                s.teacherDiscount += 0.018d;
                s.freezeDiscount += 0.010d;
                s.riskReasons = appendDiagReason(s.riskReasons, "Teacher=FREEZE");
            } else if ("RISK".equals(teacherSignal.status)) {
                s.teacherDiscount += 0.012d;
                s.riskReasons = appendDiagReason(s.riskReasons, "Teacher=RISK");
            } else if ("NO_SAMPLE".equals(teacherSignal.status)
                    || "NONE".equals(teacherSignal.status)
                    || STATUS_NO_SAMPLE_NEUTRAL.equals(teacherSignal.status)) {
                s.evidenceReasons = appendDiagReason(s.evidenceReasons, "Teacher无样本中性");
            } else if ("WEAK".equals(teacherSignal.status)
                    || "WATCH".equals(teacherSignal.status)) {
                if (currentFinalP >= 0.400d) {
                    s.teacherDiscount += 0.003d;
                    s.riskReasons = appendDiagReason(s.riskReasons, "Teacher弱观察");
                }
            } else if ("STRONG".equals(teacherSignal.status)) {
                s.moduleBoost += 0.002d;
                s.evidenceReasons = appendDiagReason(s.evidenceReasons, "Teacher=STRONG");
            } else if ("READY".equals(teacherSignal.status)) {
                s.moduleBoost += 0.001d;
                s.evidenceReasons = appendDiagReason(s.evidenceReasons, "Teacher=READY");
            }
            if (teacherRecent < DIAGNOSIS_MIN_RECENT_VALIDITY && currentFinalP >= 0.390d) {
                s.recentDiscount += 0.005d;
                s.riskReasons = appendDiagReason(s.riskReasons, "Teacher近期无效");
            }
            if (teacherReliability < DIAGNOSIS_MIN_SAMPLE_RELIABILITY && currentFinalP >= 0.390d) {
                s.sampleDiscount += 0.004d;
                s.riskReasons = appendDiagReason(s.riskReasons, "Teacher样本可信低");
            }
        }

        if (teacherSignal != null
                && teacherSignal.applied == 1
                && isTeacherSignalNoSampleNeutral(teacherSignal)) {
            s.evidenceReasons =
                    appendDiagReason(s.evidenceReasons, "Teacher无样本中性：不增加证据、不增加反证、不扣概率");
        }

        if (v20Signal == null || v20Signal.applied == 0) {
            s.v20Empty = 1;
            s.evidenceReasons = appendDiagReason(s.evidenceReasons, "V20空/WATCH中性观察");
            if (currentFinalP >= 0.420d && !isV20WatchOrEmptyNeutral(c)) {
                s.v20Discount += MAIN_CHAIN_NEUTRAL_WATCH_DISCOUNT;
                s.riskReasons = appendDiagReason(s.riskReasons, "V20空但FinalP过高，仅轻折扣");
            }
        } else {
            s.sampleReliability = Math.max(s.sampleReliability, v20Signal.sampleReliability);
            s.recentValidity = Math.max(s.recentValidity, v20Signal.recentValidity);
            evidence +=
                    clamp01(
                            v20Signal.evidenceContribution
                                    + v20Signal.dynamicScore * 0.15d
                                    + v20Signal.confidence * 0.08d);
            evidenceCount++;
            counter +=
                    clamp01(
                            v20Signal.counterEvidence
                                    + v20Signal.dynamicRisk * 0.18d
                                    + v20Signal.freezeRisk * 0.16d);
            counterCount++;
            if (v20Signal.hardRisk == 1) {
                s.v20Discount += 0.018d;
                s.freezeDiscount += 0.006d;
                s.riskReasons = appendDiagReason(s.riskReasons, "V20硬风险");
            } else if (v20Signal.counterEvidence >= 0.66d || v20Signal.dynamicRisk >= 0.70d) {
                s.v20Discount += 0.010d;
                s.riskReasons = appendDiagReason(s.riskReasons, "V20反证高");
            }
            if (v20Signal.sampleReliability < DIAGNOSIS_MIN_SAMPLE_RELIABILITY
                    && currentFinalP >= 0.390d) {
                s.sampleDiscount += 0.004d;
                s.riskReasons = appendDiagReason(s.riskReasons, "V20样本可信低");
            }
            if (v20Signal.recentValidity < DIAGNOSIS_MIN_RECENT_VALIDITY
                    && currentFinalP >= 0.390d) {
                s.recentDiscount += 0.005d;
                s.riskReasons = appendDiagReason(s.riskReasons, "V20近期有效低");
            }
            if (v20DynamicSupport(v20Signal)
                    && v20Signal.counterEvidence <= 0.46d
                    && v20Signal.recentValidity >= 0.36d) {
                s.moduleBoost += 0.002d;
                s.evidenceReasons = appendDiagReason(s.evidenceReasons, "V20有效");
            }
        }

        if (runtimeSignal != null && runtimeSignal.applied == 1) {
            evidence +=
                    clamp01(
                            runtimeSignal.bonus
                                    + runtimeSignal.probAdjust
                                    + runtimeSignal.hitRate * 0.12d);
            evidenceCount++;
            counter += clamp01(runtimeSignal.penalty + runtimeSignal.riskPenalty);
            counterCount++;
            if ("RISK".equals(runtimeSignal.status) && currentFinalP >= 0.390d) {
                s.moduleDiscount += 0.005d;
                s.riskReasons = appendDiagReason(s.riskReasons, "Runtime=RISK");
            }
        }
        if (stateNumberProfileSignal != null && stateNumberProfileSignal.applied == 1) {
            evidence +=
                    clamp01(
                            stateNumberProfileSignal.bonus
                                    + stateNumberProfileSignal.probAdjust
                                    + stateNumberProfileSignal.hitRate * 0.12d);
            evidenceCount++;
            counter +=
                    clamp01(
                            stateNumberProfileSignal.penalty
                                    + stateNumberProfileSignal.riskPenalty);
            counterCount++;
            if ("RISK".equals(stateNumberProfileSignal.status) && currentFinalP >= 0.390d) {
                s.moduleDiscount += 0.005d;
                s.riskReasons = appendDiagReason(s.riskReasons, "StateNumber=RISK");
            }
        }

        double freezeRisk =
                Math.max(
                        readDouble(
                                c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d)),
                        readDouble(c, "freezeRisk", 0.0d));
        int freezeFlag =
                Math.max(
                        readInt(c, "falseStrongFreeze", 0),
                        readInt(c, "teacherMatrixFreezeFlag", 0));
        if (freezeFlag == 1
                || freezeRisk >= 0.62d
                || containsRiskWord(readStoreState(store, "dynamic_freeze_status"))
                || containsRiskWord(readStoreState(store, "dynamic_freeze_summary"))) {
            s.freezeDiscount +=
                    clamp(
                            freezeRisk * 0.014d + (freezeFlag == 1 ? 0.008d : 0.0d),
                            0.006d,
                            MAX_FREEZE_PROB_DISCOUNT);
            s.riskReasons = appendDiagReason(s.riskReasons, "冻结矩阵/伪强风险");
        }

        String moduleEffectStatus = readStoreState(store, "module_effect_status");
        String moduleEffectSummary = readStoreState(store, "module_effect_summary");
        if (containsMisleading(moduleEffectStatus) || containsMisleading(moduleEffectSummary)) {
            s.moduleDiscount += MAX_MODULE_MISLEADING_DISCOUNT;
            s.riskReasons = appendDiagReason(s.riskReasons, "ModuleEffect=MISLEADING");
        }

        String shadowPromotionStatus = readStoreState(store, "shadow_promotion_status");
        String shadowPromotionSummary = readStoreState(store, "shadow_promotion_summary");
        if (containsPromotion(shadowPromotionStatus) || containsPromotion(shadowPromotionSummary)) {
            s.shadowPromotionBoost = MAX_SHADOW_PROMOTION_BOOST;
            s.evidenceReasons = appendDiagReason(s.evidenceReasons, "SHADOW晋级强域");
        }

        if (s.sampleReliability <= 0.0d) {
            s.sampleReliability =
                    Math.max(
                            readDouble(c, "teacherSampleReliability", 0.0d),
                            readDouble(c, "probabilitySampleReliability", 0.0d));
        }
        if (s.recentValidity <= 0.0d) {
            s.recentValidity =
                    Math.max(
                            readDouble(c, "teacherRecentValidity", 0.0d),
                            readDouble(c, "probabilityRecentValidity", 0.0d));
        }
        if (evidenceCount > 0) {
            s.evidenceContribution = clamp01(evidence / ((double) evidenceCount));
        }
        if (counterCount > 0) {
            s.counterContribution = clamp01(counter / ((double) counterCount));
        }
        if (s.counterContribution >= s.evidenceContribution + 0.18d && currentFinalP >= 0.390d) {
            s.moduleDiscount += 0.006d;
            s.riskReasons = appendDiagReason(s.riskReasons, "反证明显大于证据");
        }
        s.moduleAdjustedP =
                clamp(
                        s.moduleBoost - s.moduleDiscount,
                        -MAX_MODULE_MISLEADING_DISCOUNT,
                        MAX_DIAGNOSIS_PROB_BOOST);
        s.freezeAdjustedP = -clamp(s.freezeDiscount, 0.0d, MAX_FREEZE_PROB_DISCOUNT);
        s.totalDiscount =
                clamp(
                        s.teacherDiscount
                                + s.v20Discount
                                + s.sampleDiscount
                                + s.recentDiscount
                                + s.moduleDiscount
                                + s.freezeDiscount,
                        0.0d,
                        MAX_DIAGNOSIS_PROB_DISCOUNT);
        s.totalBoost =
                clamp(s.moduleBoost + s.shadowPromotionBoost, 0.0d, MAX_DIAGNOSIS_PROB_BOOST);
        s.summary = buildDiagnosisProbabilitySummary(s, currentFinalP);
        if (s.freezeDiscount > 0.0d || s.totalDiscount >= 0.018d) {
            s.actionAdvice = "DOWNGRADE_TO_SHADOW：诊断反证或冻结风险高";
        } else if (s.v20Empty == 1 && currentFinalP < 0.410d) {
            s.actionAdvice = "WATCH：V20因链为空，普通高P不直接增强";
        } else if (s.shadowPromotionBoost > 0.0d && s.counterContribution <= 0.55d) {
            s.actionAdvice = "PROMOTE_WATCH：SHADOW强域小幅加权，仍需最终门";
        } else if (s.totalDiscount <= 0.006d && s.evidenceContribution >= s.counterContribution) {
            s.actionAdvice = "PASS_TO_GATE：诊断风险可控，送最终门";
        } else {
            s.actionAdvice = "WATCH：诊断风险中性，保持最终门复核";
        }
        return s;
    }

    private double applyDiagnosisProbabilityAdjustment(
            double finalP, Models.ComboCandidate c, DiagnosisProbabilitySignal s) {
        finalP = AppConfig.clampProbability(finalP);
        if (s == null) {
            return finalP;
        }
        double adjusted = finalP - s.totalDiscount + s.totalBoost;
        if (c != null
                && s.v20Empty == 1
                && finalP < 0.410d
                && s.evidenceContribution < 0.54d
                && !isV20WatchOrEmptyNeutral(c)) {
            adjusted = Math.min(adjusted, finalP - MAIN_CHAIN_NEUTRAL_WATCH_DISCOUNT);
        }
        if (s.freezeDiscount > 0.0d && finalP < 0.430d) {
            adjusted = Math.min(adjusted, 0.418d);
        }
        if (s.counterContribution >= 0.72d && finalP < 0.430d) {
            adjusted = Math.min(adjusted, finalP - 0.010d);
        }
        double maxUp = finalP + MAX_DIAGNOSIS_PROB_BOOST;
        double maxDown = finalP - MAX_DIAGNOSIS_PROB_DISCOUNT;
        if (adjusted > maxUp) adjusted = maxUp;
        if (adjusted < maxDown) adjusted = maxDown;
        return AppConfig.clampProbability(adjusted);
    }

    private void writeDiagnosisProbabilityFields(
            Models.ComboCandidate c, DiagnosisProbabilitySignal s, double beforeP, double afterP) {
        if (c == null || s == null) {
            return;
        }
        writeDouble(c, "finalPBeforeDiagnosis", beforeP);
        writeDouble(c, "finalPAfterDiagnosis", afterP);
        writeDouble(c, "diagnosisAdjustedP", afterP);
        writeDouble(c, "moduleAdjustedP", s.moduleAdjustedP);
        writeDouble(c, "freezeAdjustedP", s.freezeAdjustedP);
        writeDouble(c, "shadowPromotionBoost", s.shadowPromotionBoost);
        writeDouble(c, "probabilityDiagnosisDiscount", s.totalDiscount);
        writeDouble(c, "probabilityDiagnosisBoost", s.totalBoost);
        writeDouble(c, "probabilityEvidenceContribution", s.evidenceContribution);
        writeDouble(c, "probabilityCounterEvidence", s.counterContribution);
        writeDouble(c, "probabilitySampleReliability", s.sampleReliability);
        writeDouble(c, "probabilityRecentValidity", s.recentValidity);
        writeInt(c, "probabilityV20Empty", s.v20Empty);
        writeString(c, "probabilityDiagnosisSummary", s.summary);
        writeString(c, "probabilityRiskReasons", s.riskReasons);
        writeString(c, "probabilityEvidenceReasons", s.evidenceReasons);
        writeString(c, "probabilityActionAdvice", s.actionAdvice);
        writeString(c, "actionAdvice", s.actionAdvice);
    }

    private String buildDiagnosisProbabilitySummary(
            DiagnosisProbabilitySignal s, double currentFinalP) {
        if (s == null) {
            return "概率诊断为空";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("诊断概率链：FinalP前=").append(probText(currentFinalP));
        sb.append("｜折扣=").append(probText(s.totalDiscount));
        sb.append("｜加权=").append(probText(s.totalBoost));
        sb.append("｜证据=").append(probText(s.evidenceContribution));
        sb.append("｜反证=").append(probText(s.counterContribution));
        sb.append("｜样本可信=").append(probText(s.sampleReliability));
        sb.append("｜近期有效=").append(probText(s.recentValidity));
        if (s.v20Empty == 1) sb.append("｜V20=空");
        if (s.riskReasons.length() > 0) sb.append("｜风险=").append(s.riskReasons);
        if (s.evidenceReasons.length() > 0) sb.append("｜支持=").append(s.evidenceReasons);
        return sb.toString();
    }

    private String teacherProbabilityActionAdvice(
            String status, double hitRate, double confidence, double riskPenalty) {
        if ("FREEZE".equals(status)) return "FREEZE：教师矩阵冻结，只能SHADOW复核";
        if ("RISK".equals(status)) return "DOWNGRADE：教师矩阵风险高";
        if ("STRONG".equals(status) && hitRate >= 0.345d && confidence >= 0.40d)
            return "PASS_TO_GATE：教师强域，仍走最终门";
        if ("READY".equals(status)) return "WATCH_TO_GATE：教师READY，轻支持";
        return "WATCH：教师样本或近期有效性不足";
    }

    private double sampleReliabilityFromSample(int sample) {
        if (sample <= 0) return 0.0d;
        return clamp01(((double) sample) / 160.0d);
    }

    private String readStoreState(DbStore store, String key) {
        if (store == null || key == null || key.length() == 0) return "";
        try {
            Object v =
                    invokeStore(
                            store,
                            "getState",
                            new Class[] {String.class, String.class},
                            new Object[] {key, ""});
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private boolean containsMisleading(String text) {
        if (text == null) return false;
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("MISLEADING") >= 0
                || s.indexOf("误导") >= 0
                || s.indexOf("亏") >= 0
                || s.indexOf("偏弱") >= 0;
    }

    private boolean containsPromotion(String text) {
        if (text == null) return false;
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("PROMOTE") >= 0 || s.indexOf("晋级") >= 0 || s.indexOf("强域") >= 0;
    }

    private boolean containsRiskWord(String text) {
        return AppConfig.isExplicitHardBlockStatus(text);
    }

    private String appendDiagReason(String old, String add) {
        if (add == null || add.length() == 0) return old == null ? "" : old;
        if (old == null || old.length() == 0) return add;
        if (old.indexOf(add) >= 0) return old;
        return old + "，" + add;
    }

    private String probText(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(v);
        }
    }

    /*
     * V21.2 新数据库冷启动修复。
     *
     * 旧链在Teacher/Dynamic均为0样本时，仍把WATCH/缺样本层的风险折扣叠加到
     * FinalP，典型链路为35.6%→35.2%→33.5%，随后35%合格补频永远没有候选。
     * 本方法不抬高原始策略上限，只在“无已结算Teacher/Dynamic证据且无结构硬风险”
     * 时撤销无样本折扣，最高恢复到概率风险前值，并保留原probabilityAfterRisk审计。
     */
    private void applyColdStartNeutralRestoration(
            DbStore store,
            Models.ComboCandidate c,
            Models.ProbabilityResult result,
            List<Models.Draw> history,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateSignal,
            V20DynamicCauseSignal v20Signal) {
        if (!AppConfig.COLD_START_NEUTRAL_RESTORE_ENABLE || c == null || result == null) return;
        int historySize = history == null ? 0 : history.size();
        if (historySize < AppConfig.COLD_START_NEUTRAL_MIN_HISTORY) return;

        int teacherHistory =
                Math.max(
                        readInt(c, "teacherNumberHistorySample", 0),
                        Math.max(
                                readInt(c, "teacherMatrixSample", 0),
                                readInt(c, "teacherNumberSample", 0)));
        int teacherSettled =
                Math.max(0, readInt(c, "teacherDomainShadowSample", 0))
                        + Math.max(0, readInt(c, "teacherDomainRealSample", 0));
        int dynamicSettled =
                Math.max(0, readInt(c, "dynamicShadowSample", 0))
                        + Math.max(0, readInt(c, "dynamicRealSample", 0));
        int dynamicClusterSample = Math.max(0, readInt(c, "dynamicClusterSample", 0));

        /* 号码历史画像样本可以存在；真正需要保持中性的，是尚无SHADOW/REAL同域结算证据。
         * 否则WARM部分矩阵一发布，teacherHistory>0反而会关闭冷启动修复，再次形成0出手。 */
        String teacherStatus = teacherSignal == null ? "" : safeText(teacherSignal.status);
        boolean teacherNeutral =
                teacherSettled <= 0
                        && (teacherSignal == null
                                || teacherSignal.applied == 0
                                || AppConfig.isNeutralEvidenceStatus(teacherStatus)
                                || "WATCH".equalsIgnoreCase(teacherStatus)
                                || "NO_SAMPLE_NEUTRAL".equalsIgnoreCase(teacherStatus));
        boolean dynamicNoSettledEvidence = dynamicSettled <= 0 && dynamicClusterSample <= 0;
        boolean dynamicSafe =
                v20Signal == null
                        || v20Signal.hardRisk == 0
                                && v20Signal.freezeFlag == 0
                                && v20Signal.dynamicRisk
                                        <= AppConfig.COLD_START_NEUTRAL_MAX_DYNAMIC_RISK;
        boolean runtimeSafe = runtimeSignal == null || !"RISK".equals(runtimeSignal.status);
        boolean stateSafe = stateSignal == null || !"RISK".equals(stateSignal.status);

        String hardRiskCode = readString(c, "hardRiskCode", "");
        boolean explicitHard =
                readInt(c, "hardBlockFlag", 0) == 1
                        || AppConfig.isNonOverridableFloorBlock(hardRiskCode)
                        || AppConfig.isExplicitHardBlockStatus(readString(c, "riskSeverity", ""))
                        || readInt(c, "dynamicHardRisk", 0) == 1
                        || readInt(c, "falseStrongFreeze", 0) == 1;
        double coverage =
                readDouble(c, "coverageQuality", readDouble(c, "coverageQualityScore", 0.0d));
        double third =
                readDouble(c, "thirdQuality", readDouble(c, "thirdNumberQualityScore", 0.0d));
        double counter = readDouble(c, "counterRisk", readDouble(c, "counterEvidenceScore", 0.0d));
        int coverageAvailable = readInt(c, "coverageAvailable", 0);
        int thirdAvailable = readInt(c, "thirdQualityAvailable", 0);
        double beforeTeacher = AppConfig.clampProbability(c.probabilityBeforeTeacher);
        double afterTeacher = AppConfig.clampProbability(c.probabilityAfterTeacher);
        double beforeRisk = AppConfig.clampProbability(c.probabilityBeforeRisk);

        if (!dynamicNoSettledEvidence
                || !AppConfig.isColdStartNeutralRestorationEligible(
                        historySize,
                        teacherSettled,
                        dynamicSettled,
                        dynamicClusterSample,
                        teacherNeutral,
                        dynamicSafe,
                        runtimeSafe,
                        stateSafe,
                        explicitHard,
                        coverageAvailable,
                        thirdAvailable,
                        coverage,
                        third,
                        counter,
                        beforeTeacher,
                        afterTeacher,
                        beforeRisk)) return;

        double restored = AppConfig.coldStartNeutralRestoredProbability(beforeTeacher, beforeRisk);
        if (restored <= 0.0d) return;
        if (result.finalP >= restored && result.rawP >= restored) return;

        double oldFinal = result.finalP;
        double oldRaw = result.rawP;
        result.rawP = Math.max(result.rawP, restored);
        result.finalP = Math.max(result.finalP, restored);
        writeString(c, "probabilityNeutralPolicy", "COLD_START_NO_SETTLED_EVIDENCE_RESTORE");
        writeDouble(c, "probabilityNeutralFloor", restored);
        writeDouble(c, "probabilityNeutralAnchor", beforeRisk);
        c.reasonSummary =
                appendReason(
                        c.reasonSummary,
                        "冷启动中性恢复：Teacher/Dynamic已结算样本均为0且无结构硬风险，撤销无样本折扣；"
                                + "Raw="
                                + UiFormatter.probability(oldRaw)
                                + "→"
                                + UiFormatter.probability(result.rawP)
                                + "，FinalP="
                                + UiFormatter.probability(oldFinal)
                                + "→"
                                + UiFormatter.probability(result.finalP)
                                + "；不超过风险前概率，不绕过Coverage/Third/Counter硬质量线");
        try {
            if (store != null) {
                store.setState(
                        "cold_start_neutral_restore_last",
                        "target="
                                + safeText(c.targetIssue)
                                + "|position="
                                + c.position
                                + "|oldRaw="
                                + oldRaw
                                + "|oldFinal="
                                + oldFinal
                                + "|restored="
                                + restored
                                + "|coverage="
                                + coverage
                                + "|third="
                                + third
                                + "|counter="
                                + counter
                                + "|teacherHistory="
                                + teacherHistory
                                + "|teacherSettled="
                                + teacherSettled
                                + "|teacherStatus="
                                + teacherStatus
                                + "|dynamicSettled="
                                + dynamicSettled);
                store.setState(
                        "cold_start_neutral_restore_version",
                        AppConfig.ACTIVE_QUALITY_BUILD_VERSION);
            }
        } catch (Throwable ignore) {
        }
    }

    private double applyNeutralEvidenceFloorValue(
            Models.ComboCandidate c,
            double finalP,
            double rawP,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateSignal,
            V20DynamicCauseSignal v20Signal) {
        boolean teacherNeutral =
                teacherSignal == null
                        || teacherSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(teacherSignal.status);
        boolean runtimeNeutral =
                runtimeSignal == null
                        || runtimeSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(runtimeSignal.status);
        boolean stateNeutral =
                stateSignal == null
                        || stateSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(stateSignal.status);
        boolean v20Neutral =
                v20Signal == null
                        || v20Signal.applied == 0
                        || (v20Signal.hardRisk == 0
                                && v20Signal.freezeFlag == 0
                                && isV20WatchOrEmptyNeutral(c));
        boolean explicitHard =
                readInt(c, "dynamicHardRisk", 0) == 1
                        || readInt(c, "falseStrongFreeze", 0) == 1
                        || readDouble(c, "counterEvidenceScore", 0.0d) >= 0.72d
                        || readDouble(c, "k3CoverageRisk", 0.0d) >= 0.70d;
        double out = AppConfig.clampProbability(finalP);
        if (teacherNeutral && runtimeNeutral && stateNeutral && v20Neutral && !explicitHard) {
            double preDrawPrior =
                    firstPositiveProbability(
                            readDouble(c, "probabilitySinglePassRawP", 0.0d),
                            c == null ? 0.0d : c.rawP,
                            c == null ? 0.0d : c.modelP,
                            AppConfig.RANDOM_K3_BASELINE);
            double anchor =
                    rawP * (1.0d - AppConfig.PROBABILITY_PREDRAW_PRIOR_WEIGHT)
                            + preDrawPrior * AppConfig.PROBABILITY_PREDRAW_PRIOR_WEIGHT;
            double floor =
                    AppConfig.clampProbability(
                            anchor - AppConfig.PROBABILITY_NEUTRAL_ANCHOR_MAX_BELOW_PRIOR);
            if (out < floor) out = floor;
            writeString(
                    c, "probabilityNeutralPolicy", "NO_SAMPLE_WATCH_TRUE_NEUTRAL_PREDRAW_ANCHOR");
            writeDouble(c, "probabilityNeutralFloor", floor);
            writeDouble(c, "probabilityNeutralAnchor", anchor);
        }
        return out;
    }

    private void applyNeutralEvidenceFloor(
            Models.ComboCandidate c,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateSignal,
            V20DynamicCauseSignal v20Signal) {
        if (c == null) return;
        boolean teacherNeutral =
                teacherSignal == null
                        || teacherSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(teacherSignal.status);
        boolean runtimeNeutral =
                runtimeSignal == null
                        || runtimeSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(runtimeSignal.status);
        boolean stateNeutral =
                stateSignal == null
                        || stateSignal.applied == 0
                        || AppConfig.isNeutralEvidenceStatus(stateSignal.status);
        boolean v20Neutral =
                v20Signal == null
                        || v20Signal.applied == 0
                        || (v20Signal.hardRisk == 0
                                && v20Signal.freezeFlag == 0
                                && isV20WatchOrEmptyNeutral(c));
        boolean explicitHard =
                readInt(c, "dynamicHardRisk", 0) == 1
                        || readInt(c, "falseStrongFreeze", 0) == 1
                        || readDouble(c, "counterEvidenceScore", 0.0d) >= 0.72d
                        || readDouble(c, "k3CoverageRisk", 0.0d) >= 0.70d;
        if (teacherNeutral && runtimeNeutral && stateNeutral && v20Neutral && !explicitHard) {
            double raw = c.rawP > 0.0d ? c.rawP : AppConfig.RANDOM_K3_BASELINE;
            double floor = raw - AppConfig.PROBABILITY_NEUTRAL_MAX_BELOW_RAW;
            if (c.finalP < floor) c.finalP = AppConfig.clampProbability(floor);
            writeString(c, "probabilityNeutralPolicy", "NO_SAMPLE_WATCH_TRUE_NEUTRAL");
            writeDouble(c, "probabilityNeutralFloor", floor);
        }
        if (c.dynamicGate <= 0.0d) c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        c.edge = c.finalP - c.dynamicGate;
    }

    private void applyFinalProbabilityBlock(
            Models.ComboCandidate c,
            Models.ProbabilityResult p,
            TeacherMatrixSignal teacherSignal,
            RuntimeProfileSignal runtimeSignal,
            StateNumberProfileSignal stateNumberProfileSignal,
            V20DynamicCauseSignal v20Signal) {
        if (c == null || p == null) {
            return;
        }

        if (c.finalP < 0.335d) {
            c.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (p.calibrationError > AppConfig.CALIBRATION_ERROR_HARD
                && c.finalP < c.dynamicGate
                && c.finalP < 0.370d) {
            c.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (c.edge < -0.030d && c.finalP < 0.370d) {
            c.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            c.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
            return;
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX + 18.0d) {
            c.blockCode = AppConfig.BLOCK_BREAK_HIGH;
            return;
        }

        if (teacherSignal != null
                && teacherSignal.applied == 1
                && "FREEZE".equals(teacherSignal.status)) {
            c.blockCode = "TEACHER_MATRIX_FREEZE";
            return;
        }

        if (teacherSignal != null
                && teacherSignal.applied == 1
                && "RISK".equals(teacherSignal.status)
                && c.finalP < 0.425d) {
            c.blockCode = teacherMatrixWeakBlockCode();
            return;
        }

        if (readInt(c, "teacherMatrixFreezeFlag", 0) == 1 && c.finalP < 0.430d) {
            c.blockCode = "TEACHER_MATRIX_FREEZE";
            return;
        }

        if (runtimeSignal != null
                && runtimeSignal.applied == 1
                && "RISK".equals(runtimeSignal.status)
                && c.finalP < 0.410d) {
            c.blockCode = runtimeProfileWeakBlockCode();
            return;
        }

        if (stateNumberProfileSignal != null
                && stateNumberProfileSignal.applied == 1
                && "RISK".equals(stateNumberProfileSignal.status)
                && c.finalP < 0.410d) {
            c.blockCode = runtimeProfileWeakBlockCode();
            return;
        }

        if (readDouble(c, "probabilityCounterEvidence", 0.0d) >= 0.72d
                && readDouble(c, "probabilityEvidenceContribution", 0.0d) <= 0.46d
                && c.finalP < 0.425d) {
            c.blockCode = "PROBABILITY_DIAG_COUNTER_HIGH";
            return;
        }

        if (readDouble(c, "probabilityRecentValidity", 1.0d) <= 0.18d
                && readDouble(c, "probabilitySampleReliability", 1.0d) <= 0.26d
                && c.finalP < 0.420d) {
            c.blockCode = "PROBABILITY_DIAG_RECENT_INVALID";
            return;
        }

        if (readInt(c, "falseHighPFlag", 0) == 1 && c.finalP < 0.425d) {
            c.blockCode = "PROBABILITY_FALSE_HIGH_P";
            return;
        }

        if (readDouble(c, "k3CoverageRisk", 0.0d) >= 0.70d && c.finalP < 0.425d) {
            c.blockCode = "K3_COVERAGE_RISK";
            return;
        }

        if (readDouble(c, "thirdNumberQualityScore", 1.0d) < THIRD_NUMBER_QUALITY_MIN
                && c.finalP < 0.420d) {
            c.blockCode = "K3_THIRD_NUMBER_WEAK";
            return;
        }

        if (readInt(c, "probabilityTeacherWeakOrEmpty", 0) == 1
                && readInt(c, "probabilityV20UnknownOrEmpty", 0) == 1
                && !coveragePreferredDomain(c)
                && c.finalP < 0.415d
                && !isNoSampleWatchNeutralForProbability(c)) {
            c.blockCode = "TEACHER_WEAK_V20_UNKNOWN_PROB";
            return;
        }

        if (v20Signal != null
                && v20Signal.applied == 0
                && c.finalP < 0.410d
                && !teacherSignalSupport(teacherSignal)
                && !runtimeSignalSupport(runtimeSignal)
                && !stateNumberSignalSupport(stateNumberProfileSignal)
                && !isV20WatchOrEmptyNeutral(c)) {
            c.blockCode = "V20_EMPTY_STILL_WEAK";
            return;
        }

        if (v20Signal != null && v20Signal.applied == 1) {
            if (v20Signal.hardRisk == 1) {
                c.blockCode = "V20_DYNAMIC_HARD_RISK";
                return;
            }

            if (v20Signal.freezeFlag == 1 && c.finalP < 0.420d) {
                c.blockCode = "V20_FALSE_STRONG_FREEZE";
                return;
            }

            if (v20Signal.dynamicRisk >= 0.72d && c.finalP < 0.420d) {
                c.blockCode = "V20_DYNAMIC_RISK";
                return;
            }
        }

        boolean support =
                teacherSignalSupport(teacherSignal)
                        || runtimeSignalSupport(runtimeSignal)
                        || stateNumberSignalSupport(stateNumberProfileSignal)
                        || v20DynamicSupport(v20Signal)
                        || isTeacherSupport(c)
                        || isRuntimeSupport(c)
                        || isStateNumberSupport(c);

        if (c.profileScore < 0.280d
                && c.fallbackProfileScore < 0.280d
                && c.evidenceRisk >= 0.660d
                && c.finalP < 0.420d
                && !support) {
            c.blockCode = AppConfig.BLOCK_BACKTEST_WEAK;
        }
    }

    private void sortByFinal(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = scoreForSort(a);
                        double sb = scoreForSort(b);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return 1;
                        }

                        if (b == null) {
                            return -1;
                        }

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private double scoreForSort(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP + c.edge * 0.50d;

        score += c.rawP * 0.18d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.12d;
        score += c.similarP * 0.020d;
        score += c.memoryP * 0.018d;

        score += c.profileScore * 0.004d;
        score += c.fallbackProfileScore * 0.003d;
        score += c.evidenceScore * 0.005d;
        score += c.aiJudgeScore * 0.003d;
        score += c.aPlusScore * 0.003d;
        score += c.promotionScore * 0.003d;

        score -= c.auditPenalty * 0.030d;
        score -= c.evidenceRisk * 0.025d;
        score -= c.driftRisk / 100.0d * 0.014d;
        score -= c.breakDepth / 100.0d * 0.014d;

        String teacherStatus = readString(c, "teacherMatrixStatus", "");
        double teacherHit = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double teacherConf = readDouble(c, "teacherMatrixConfidence", 0.0d);
        double teacherRisk = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);

        if ("STRONG".equals(teacherStatus)) {
            score += 0.018d;
        } else if ("READY".equals(teacherStatus)) {
            score += 0.010d;
        } else if ("RISK".equals(teacherStatus)) {
            score -= 0.030d;
        }

        score += teacherHit * 0.010d;
        score += teacherConf * 0.004d;
        score -= teacherRisk * 0.060d;

        String runtimeStatus = readString(c, "runtimeProfileStatus", "");
        double runtimeHit = readDouble(c, "runtimeProfileHitRate", 0.0d);
        double runtimeConf = readDouble(c, "runtimeProfileConfidence", 0.0d);
        double runtimeRisk = readDouble(c, "runtimeProfileRiskPenalty", 0.0d);

        if ("STRONG".equals(runtimeStatus)) {
            score += 0.012d;
        } else if ("READY".equals(runtimeStatus)) {
            score += 0.007d;
        } else if ("RISK".equals(runtimeStatus)) {
            score -= 0.022d;
        }

        score += runtimeHit * 0.008d;
        score += runtimeConf * 0.003d;
        score -= runtimeRisk * 0.050d;

        String stateNumberStatus = readString(c, "stateNumberProfileStatus", "");
        double stateNumberHit = readDouble(c, "stateNumberProfileHitRate", 0.0d);
        double stateNumberConf = readDouble(c, "stateNumberProfileConfidence", 0.0d);
        double stateNumberRisk = readDouble(c, "stateNumberProfileRiskPenalty", 0.0d);

        if ("STRONG".equals(stateNumberStatus)) {
            score += 0.012d;
        } else if ("READY".equals(stateNumberStatus)) {
            score += 0.007d;
        } else if ("RISK".equals(stateNumberStatus)) {
            score -= 0.022d;
        }

        score += stateNumberHit * 0.008d;
        score += stateNumberConf * 0.003d;
        score -= stateNumberRisk * 0.050d;

        V20DynamicCauseSignal v20 = evaluateV20DynamicCauseSignal(c, null, c.backtestMode, true);

        if (v20 != null && v20.applied == 1) {
            score += v20.dynamicScore * 0.030d;
            score += v20.seedScore * 0.012d;
            score += v20.ruleScore * 0.010d;
            score += v20.shadowScore * 0.010d;
            score -= v20.dynamicRisk * 0.030d;
            score -= v20.freezeRisk * 0.040d;

            if ("S".equals(v20.grade)) {
                score += 0.016d;
            } else if ("A".equals(v20.grade)) {
                score += 0.010d;
            } else if ("C".equals(v20.grade)) {
                score -= 0.018d;
            }

            if (v20.hardRisk == 1) {
                score -= 0.060d;
            }
        }

        if (isHardState(c.stateMode)) {
            score -= 0.030d;
        } else if (isChaosState(c.stateMode)) {
            score -= 0.006d;
        }

        if (c.blockCode != null
                && c.blockCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)) {
            score -= 0.030d;
        }

        if (c.finalP >= c.dynamicGate && c.finalP >= minRealBetP() - 0.004d) {
            score += 0.008d;
        }

        return score;
    }

    private void rerank(List<Models.ComboCandidate> list) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private void markChampionGapAfterSort(List<Models.ComboCandidate> list) {
        if (list == null || list.size() < 2) {
            return;
        }

        Models.ComboCandidate first = list.get(0);
        Models.ComboCandidate second = list.get(1);

        if (first == null || second == null) {
            return;
        }

        double gap = first.finalP - second.finalP;

        if (gap < 0.0d) {
            gap = 0.0d;
        }

        first.championGap = Math.max(first.championGap, gap);
        first.edge = first.finalP - first.dynamicGate;
    }

    private void normalizeCandidateBeforeProbability(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            c.k3Mask = buildK3Mask(c.n1, c.n2, c.n3);
        }

        c.dynamicGate = cleanIncomingGate(c.dynamicGate, AppConfig.DEFAULT_DYNAMIC_GATE);

        if (c.edge == 0.0d && c.finalP > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }

        c.modelP = safeP(c.modelP);
        c.shapeP = safeP(c.shapeP);
        c.comboP = safeP(c.comboP);
        c.recoveryP = safeP(c.recoveryP);
    }

    private double cleanIncomingGate(double gate, double fallback) {
        if (fallback <= 0.0d || Double.isNaN(fallback) || Double.isInfinite(fallback)) {
            fallback = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        if (fallback <= 0.0d || Double.isNaN(fallback) || Double.isInfinite(fallback)) {
            fallback = 0.385d;
        }

        if (gate <= 0.0d || Double.isNaN(gate) || Double.isInfinite(gate)) {
            return fallback;
        }

        if (gate > STALE_DYNAMIC_GATE_HIGH) {
            return fallback;
        }

        if (gate < 0.330d) {
            return fallback;
        }

        return AppConfig.clampProbability(gate);
    }

    private String probabilityConclusion(Models.ComboCandidate c) {
        if (c == null) {
            return "候选为空";
        }

        V20DynamicCauseSignal v20 = evaluateV20DynamicCauseSignal(c, null, c.backtestMode, true);

        if (probabilityAllowsBet(c)) {
            return "概率允许进入BET候选";
        }

        if (c.finalP < minRealBetP() - 0.004d) {
            return "最终概率低于真实BET最低线";
        }

        if (c.finalP < c.dynamicGate) {
            return "最终概率未过动态门槛";
        }

        if (c.edge < 0.0d) {
            return "概率优势为负";
        }

        if (c.rawP < RAW_BET_FLOOR_STRONG) {
            return "原始概率支撑不足";
        }

        if (c.calibrationError > AppConfig.CALIBRATION_ERROR_HARD) {
            return "校准误差过高";
        }

        if (v20 != null && v20.applied == 1) {
            if (v20.hardRisk == 1) {
                return "V20动态因链硬风险";
            }

            if (v20.freezeFlag == 1) {
                return "V20伪强冻结";
            }

            if (v20.dynamicRisk >= 0.72d) {
                return "V20动态因链风险偏高";
            }
        }

        String teacherStatus = readString(c, "teacherMatrixStatus", "");
        String runtimeStatus = readString(c, "runtimeProfileStatus", "");
        String stateNumberStatus = readString(c, "stateNumberProfileStatus", "");

        if ("RISK".equals(teacherStatus)) {
            return "教师矩阵判定风险";
        }

        if ("RISK".equals(runtimeStatus)) {
            return "运行画像判定风险";
        }

        if ("RISK".equals(stateNumberStatus)) {
            return "状态号码画像判定风险";
        }

        if (c.profileScore < 0.280d
                && c.evidenceRisk >= 0.660d
                && !isTeacherSupport(c)
                && !isRuntimeSupport(c)
                && !isStateNumberSupport(c)
                && !v20DynamicSupport(v20)) {
            return "学习画像弱且证据风险高";
        }

        if (c.blockCode != null && c.blockCode.length() > 0) {
            return UiFormatter.blockReasonText(c.blockCode);
        }

        return "概率不足，HOLD倾向";
    }

    private String buildRuntimeStateKey(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(c.position);
        sb.append("|M=");
        sb.append(
                cleanKeyPart(
                        c.stateMode == null || c.stateMode.length() == 0
                                ? "UNKNOWN"
                                : c.stateMode));
        sb.append("|S=");
        sb.append(cleanKeyPart(c.shapeKey));
        sb.append("|");
        sb.append(bucketScoreText("DRIFT", c.driftRisk));
        sb.append("|");
        sb.append(bucketScoreText("BREAK", c.breakDepth));
        sb.append("|");
        sb.append(bucketScoreText("RECOVERY", c.recoveryScore));
        sb.append("|");
        sb.append(bucketProbabilityText("FP", c.finalP));
        sb.append("|");
        sb.append(bucketProbabilityText("GATE", c.dynamicGate));
        sb.append("|DYN=");
        sb.append(bucketProbabilityText("DYN", readDouble(c, "dynamicCauseScore", 0.0d)));
        sb.append("|DR=");
        sb.append(bucketProbabilityText("DR", readDouble(c, "dynamicRiskScore", 0.0d)));

        return sb.toString();
    }

    private String buildRuntimeProfileKey(String stateKey, int position, int k3Mask) {
        return "RP|P" + position + "|K" + k3Mask + "|" + safeKey(stateKey);
    }

    private String buildStateNumberProfileKey(String stateKey, int position, int number) {
        return "SNP|P" + position + "|N" + formatNumber(number) + "|" + safeKey(stateKey);
    }

    private String buildTeacherProfileKey(String stateKey, int position, int number) {
        return "TM|P" + position + "|N" + formatNumber(number) + "|" + safeKey(stateKey);
    }

    private String buildTeacherComboKey(String stateKey, int position, int k3Mask) {
        return "TMC|P" + position + "|K" + k3Mask + "|" + safeKey(stateKey);
    }

    private String safeKey(String key) {
        if (key == null) {
            return "";
        }

        String out = key.replace('|', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');

        if (out.length() > 220) {
            out = out.substring(0, 220);
        }

        return out;
    }

    private String bucketScoreText(String prefix, double value) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (value <= 0.0d) {
            return prefix + "_0";
        }

        double v = value;

        if (v <= 1.0d) {
            v = v * 100.0d;
        }

        if (v < 30.0d) {
            return prefix + "_LOW";
        }

        if (v < 55.0d) {
            return prefix + "_MID";
        }

        if (v < 75.0d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private String bucketProbabilityText(String prefix, double p) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "P";
        }

        if (p <= 0.0d) {
            return prefix + "_0";
        }

        if (p < 0.340d) {
            return prefix + "_LOW";
        }

        if (p < 0.375d) {
            return prefix + "_SAFE";
        }

        if (p < 0.400d) {
            return prefix + "_BASE";
        }

        if (p < 0.430d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private String cleanKeyPart(String value) {
        if (value == null) {
            return "";
        }

        String out = value.replace('|', '_');
        out = out.replace(' ', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');

        if (out.length() > 80) {
            out = out.substring(0, 80);
        }

        return out;
    }

    private boolean candidateCoversZone(Models.ComboCandidate c, int number) {
        if (c == null || !AppConfig.isValidNumber(number)) {
            return false;
        }

        int z = numberZone(number);

        return numberZone(c.n1) == z || numberZone(c.n2) == z || numberZone(c.n3) == z;
    }

    private boolean candidateCoversParity(Models.ComboCandidate c, int number) {
        if (c == null || !AppConfig.isValidNumber(number)) {
            return false;
        }

        int p = number % 2;

        return validParity(c.n1, p) || validParity(c.n2, p) || validParity(c.n3, p);
    }

    private boolean validParity(int n, int p) {
        return AppConfig.isValidNumber(n) && n % 2 == p;
    }

    private int numberZone(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return 0;
        }

        if (n <= 3) {
            return 1;
        }

        if (n <= 7) {
            return 2;
        }

        return 3;
    }

    private int calcSpan(int n1, int n2, int n3) {
        int min = 99;
        int max = 0;
        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        if (max <= 0 || min == 99) {
            return 0;
        }

        return max - min;
    }

    private boolean containsNumber(int mask, int number) {
        if (!AppConfig.isValidNumber(number)) {
            return false;
        }

        int bit = 1 << number;
        return (mask & bit) != 0;
    }

    private int buildK3Mask(int n1, int n2, int n3) {
        int mask = 0;

        if (AppConfig.isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (AppConfig.isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (AppConfig.isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private boolean isHardState(String mode) {
        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode);
    }

    private boolean isChaosState(String mode) {
        return AppConfig.MODE_CHAOS.equals(mode);
    }

    private boolean teacherSignalSupport(TeacherMatrixSignal s) {
        if (s == null || s.applied == 0) {
            return false;
        }

        if ("STRONG".equals(s.status) || "READY".equals(s.status)) {
            return true;
        }

        if (s.sample >= TEACHER_MIN_SAMPLE && s.hitRate >= 0.370d && s.confidence >= 0.360d) {
            return true;
        }

        if (s.comboSample >= TEACHER_COMBO_MIN_SAMPLE
                && s.comboHitRate >= 0.370d
                && s.confidence >= 0.360d) {
            return true;
        }

        return false;
    }

    private boolean runtimeSignalSupport(RuntimeProfileSignal s) {
        if (s == null || s.applied == 0) {
            return false;
        }

        if ("STRONG".equals(s.status) || "READY".equals(s.status)) {
            return true;
        }

        return s.sample >= minCacheSampleForBet() && s.hitRate >= 0.370d && s.confidence >= 0.360d;
    }

    private boolean stateNumberSignalSupport(StateNumberProfileSignal s) {
        if (s == null || s.applied == 0) {
            return false;
        }

        if ("STRONG".equals(s.status) || "READY".equals(s.status)) {
            return true;
        }

        return s.sample >= minStateSampleForBet() && s.hitRate >= 0.370d && s.confidence >= 0.360d;
    }

    private boolean isTeacherSupport(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        String status = readString(c, "teacherMatrixStatus", "");
        if ("STRONG".equals(status) || "READY".equals(status)) {
            return true;
        }

        int sample = readInt(c, "teacherMatrixSample", 0);
        int comboSample = readInt(c, "teacherMatrixComboSample", 0);
        double hitRate = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double comboHitRate = readDouble(c, "teacherMatrixRecentHitRate", 0.0d);
        double conf = readDouble(c, "teacherMatrixConfidence", 0.0d);

        if (sample >= TEACHER_MIN_SAMPLE && hitRate >= 0.370d && conf >= 0.360d) {
            return true;
        }

        if (comboSample >= TEACHER_COMBO_MIN_SAMPLE && comboHitRate >= 0.370d && conf >= 0.360d) {
            return true;
        }

        return false;
    }

    private boolean isRuntimeSupport(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        String status = readString(c, "runtimeProfileStatus", "");
        if ("STRONG".equals(status) || "READY".equals(status)) {
            return true;
        }

        int sample = readInt(c, "runtimeProfileSample", 0);
        double hitRate = readDouble(c, "runtimeProfileHitRate", 0.0d);
        double conf = readDouble(c, "runtimeProfileConfidence", 0.0d);

        return sample >= minCacheSampleForBet() && hitRate >= 0.370d && conf >= 0.360d;
    }

    private boolean isStateNumberSupport(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        String status = readString(c, "stateNumberProfileStatus", "");
        if ("STRONG".equals(status) || "READY".equals(status)) {
            return true;
        }

        int sample = readInt(c, "stateNumberProfileSample", 0);
        double hitRate = readDouble(c, "stateNumberProfileHitRate", 0.0d);
        double conf = readDouble(c, "stateNumberProfileConfidence", 0.0d);

        return sample >= minStateSampleForBet() && hitRate >= 0.370d && conf >= 0.360d;
    }

    private double safeP(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        if (value <= 0.0d) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        return AppConfig.clampProbability(value);
    }

    private double safeWeight(double value, double def) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0d) {
            return def;
        }

        if (value > 1.0d) {
            return 1.0d;
        }

        return value;
    }

    private String bucketKey(double rawP) {
        if (rawP < 0.32d) return "P00_32";
        if (rawP < 0.34d) return "P32_34";
        if (rawP < 0.36d) return "P34_36";
        if (rawP < 0.38d) return "P36_38";
        if (rawP < 0.40d) return "P38_40";
        if (rawP < 0.42d) return "P40_42";
        if (rawP < 0.44d) return "P42_44";
        if (rawP < 0.46d) return "P44_46";
        if (rawP < 0.48d) return "P46_48";
        return "P48_PLUS";
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        if (oldText.indexOf(add) >= 0) {
            return oldText;
        }

        return oldText + "；" + add;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static double clamp(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }

        if (v < min) {
            return min;
        }

        if (v > max) {
            return max;
        }

        return v;
    }

    private String formatNumber(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private int normalizeEnrichLimit(String mode, int value) {
        if (value <= 0) {
            value = AppConfig.getProbabilityEnrichLimit(mode);
        }

        if (value <= 0) {
            value = REALTIME_ENRICH_HARD_LIMIT;
        }

        if (AppConfig.BACKTEST_MODE_FAST.equals(mode) && value < REALTIME_ENRICH_HARD_LIMIT) {
            value = REALTIME_ENRICH_HARD_LIMIT;
        }

        if (AppConfig.BACKTEST_MODE_FAST.equals(mode) && value > REALTIME_ENRICH_HARD_LIMIT) {
            value = REALTIME_ENRICH_HARD_LIMIT;
        }

        return value;
    }

    private int normalizeDeepLimit(String mode, int value) {
        if (value < 0) {
            value = 0;
        }

        if (value <= 0 && AppConfig.BACKTEST_MODE_FAST.equals(mode)) {
            value = REALTIME_DEEP_HARD_LIMIT;
        }

        if (AppConfig.BACKTEST_MODE_FAST.equals(mode) && value > REALTIME_DEEP_HARD_LIMIT) {
            value = REALTIME_DEEP_HARD_LIMIT;
        }

        return value;
    }

    /*
     * 链路修改升级V2：Probability只输出证据与探针资格素材。
     * 频率债务绝不写回finalP，也不在本层产生BET。
     */
    private Models.FrequencyState loadLinkFrequencyStateSafe(DbStore store) {
        Models.FrequencyState state = null;
        if (store != null) {
            try {
                state = store.loadFrequencyState();
            } catch (Throwable e) {
                state = null;
            }
        }
        if (state == null) {
            state = new Models.FrequencyState();
        }
        return state;
    }

    private void applyLinkModificationV2ProbabilityFields(
            Models.ComboCandidate c, Models.FrequencyState state, List<Models.Draw> history) {
        if (c == null) return;
        if (state == null) state = new Models.FrequencyState();

        double coverage =
                maxRead(
                        c,
                        "coverageQuality",
                        "coverageQualityScore",
                        "finalDecisionCoverageQuality",
                        "k3CoverageQuality");
        double third =
                maxRead(
                        c,
                        "thirdQuality",
                        "thirdNumberQualityScore",
                        "needSupplyThirdQuality",
                        "finalDecisionThirdNumberQuality");
        double need =
                Math.max(
                        readDouble(c, "needSupplyMatchScore", 0.0d),
                        Math.max(
                                readDouble(c, "positionNeedScore", 0.0d),
                                readDouble(c, "numberSupplyScore", 0.0d)));
        double counter =
                Math.max(
                        readDouble(c, "counterRisk", 0.0d),
                        Math.max(
                                readDouble(c, "counterfactualRisk", 0.0d),
                                Math.max(
                                        readDouble(c, "counterEvidenceScore", 0.0d),
                                        readDouble(c, "probabilityCounterEvidence", 0.0d))));
        double promotionScore =
                Math.max(
                        readDouble(c, "shadowPromotionScore", 0.0d),
                        Math.max(
                                readDouble(c, "shadowPromoteScore", 0.0d),
                                readDouble(c, "promotionScore", 0.0d)));
        String promotionStatus =
                readString(
                        c,
                        "shadowPromotionStatus",
                        readString(c, "promotionStatus", AppConfig.PROMOTION_NO_SAMPLE));
        String promotionDomain =
                readString(c, "shadowPromotionDomain", readString(c, "promotionDomain", ""));
        String ruleStatus = readString(c, "ruleHealthStatus", readString(c, "ruleStatus", "WATCH"));

        c.realBetFrequency50 = AppConfig.clampProbability(state.frequency50);
        c.realBetFrequency100 = AppConfig.clampProbability(state.frequency100);
        c.realBetFrequency300 = AppConfig.clampProbability(state.frequency300);
        c.frequencyDebtScore = AppConfig.clampFrequencyDebt(state.frequencyDebt);
        c.frequencyState = state.status == null ? AppConfig.FREQ_STATE_NORMAL : state.status;
        c.frequencyStateVersion = LINK_MODIFICATION_V2_PROBABILITY_VERSION;
        c.frequencyStateUpdatedAt = state.updatedAt;

        c.coverageQuality = AppConfig.clampScore01(coverage);
        c.thirdQuality = AppConfig.clampScore01(third);
        c.needSupplyScore = AppConfig.clampScore01(need);
        c.counterRisk = AppConfig.clampScore01(counter);
        c.shadowPromotionScore = AppConfig.clampScore01(promotionScore);
        c.shadowPromotionStatus =
                promotionStatus == null ? AppConfig.PROMOTION_NO_SAMPLE : promotionStatus;
        c.shadowPromotionDomain = promotionDomain == null ? "" : promotionDomain;
        c.ruleHealthStatus = ruleStatus == null ? "WATCH" : ruleStatus;
        c.coverageAvailable = coverage > 0.000001d ? 1 : 0;
        c.thirdQualityAvailable = third > 0.000001d ? 1 : 0;
        c.promotionDomainAvailable =
                promotionDomain != null && promotionDomain.length() > 0 ? 1 : 0;

        /* 正常门与探针门此处保持一致；真正频率软调门交给DynamicGateEngine。 */
        double gate = c.dynamicGate > 0.0d ? c.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE;
        c.baseGate = AppConfig.clampProbability(gate);
        c.riskAdjustedGate = c.baseGate;
        c.frequencyGateAdjust = 0.0d;
        c.effectiveProbeGate = c.riskAdjustedGate;
        c.rawEdge = c.finalP - c.riskAdjustedGate;
        c.effectiveProbeEdge = c.rawEdge;
        c.qualifiedProbe = 0;

        double quality =
                c.coverageQuality * 0.22d
                        + c.thirdQuality * 0.18d
                        + c.needSupplyScore * 0.20d
                        + (1.0d - c.counterRisk) * 0.20d
                        + c.shadowPromotionScore * 0.20d;
        double closeness = 1.0d - Math.min(1.0d, Math.abs(c.riskAdjustedGate - c.finalP) / 0.050d);
        c.qualifiedProbeScore = AppConfig.clampScore01(quality * 0.75d + closeness * 0.25d);
        /* 仅作为独立支持分，不改变finalP。 */
        c.frequencySupportScore =
                AppConfig.clampScore01(
                        (state.isLowFrequencyPressure()
                                                ? c.frequencyDebtScore
                                                        / Math.max(
                                                                0.000001d, AppConfig.FREQ_DEBT_MAX)
                                                : 0.0d)
                                        * 0.35d
                                + c.qualifiedProbeScore * 0.65d);

        if (c.snapshotFreshness == null || c.snapshotFreshness.length() == 0) {
            c.snapshotFreshness = "HOT_ONLY";
        }
        if (c.evidenceAsOfIssue == null || c.evidenceAsOfIssue.length() == 0) {
            c.evidenceAsOfIssue = c.baseIssue == null ? "" : c.baseIssue;
        }
        c.probeEligibilityVersion = LINK_MODIFICATION_V2_PROBABILITY_VERSION;
        c.decisionVersionV2 = LINK_MODIFICATION_V2_PROBABILITY_VERSION;
        writeString(c, "probabilityFieldVersion", LINK_MODIFICATION_V2_PROBABILITY_VERSION);
        writeString(
                c,
                "probabilityActionAdvice",
                "Probability只输出FinalP与合格探针素材；频率债务不抬高FinalP，最终BET仍由DecisionGateEngine唯一裁决");
    }

    private static Field cachedPublicField(Class cls, String fieldName) {
        if (cls == null || fieldName == null || fieldName.length() == 0) return null;
        String key = cls.getName() + "#" + fieldName;
        synchronized (REFLECT_FIELD_CACHE) {
            if (REFLECT_FIELD_CACHE.containsKey(key)) return REFLECT_FIELD_CACHE.get(key);
            if (REFLECT_FIELD_MISS_CACHE.contains(key)) return null;
            try {
                Field f = cls.getField(fieldName);
                REFLECT_FIELD_CACHE.put(key, f);
                return f;
            } catch (Throwable e) {
                REFLECT_FIELD_MISS_CACHE.add(key);
                return null;
            }
        }
    }

    private void beginProbabilityRunCache() {
        runStoreResultCache.clear();
        runStoreCacheActive = true;
        runStoreCacheHit = 0;
        runStoreCacheMiss = 0;
    }

    private void endProbabilityRunCache() {
        runStoreCacheActive = false;
    }

    private String buildStoreCallKey(
            DbStore store, String methodName, Class[] types, Object[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(store == null ? "null" : store.getClass().getName())
                .append('#')
                .append(methodName);
        if (types != null)
            for (int i = 0; i < types.length; i++)
                sb.append('|').append(types[i] == null ? "?" : types[i].getName());
        if (args != null)
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a instanceof Models.ComboCandidate) {
                    Models.ComboCandidate c = (Models.ComboCandidate) a;
                    sb.append("=C:")
                            .append(c.targetIssue)
                            .append(':')
                            .append(c.position)
                            .append(':')
                            .append(c.k3Mask)
                            .append(':')
                            .append(c.stateKey == null ? "" : c.stateKey);
                } else if (a instanceof Models.Decision) {
                    Models.Decision d = (Models.Decision) a;
                    sb.append("=D:")
                            .append(d.targetIssue)
                            .append(':')
                            .append(d.position)
                            .append(':')
                            .append(d.k3Mask);
                } else {
                    sb.append('=').append(a == null ? "null" : String.valueOf(a));
                }
            }
        return sb.toString();
    }

    private Method cachedStoreMethod(Class cls, String methodName, Class[] types) {
        if (cls == null || methodName == null) return null;
        StringBuilder sb = new StringBuilder(cls.getName()).append('#').append(methodName);
        if (types != null)
            for (int i = 0; i < types.length; i++)
                sb.append('|').append(types[i] == null ? "?" : types[i].getName());
        String key = sb.toString();
        synchronized (REFLECT_METHOD_CACHE) {
            if (REFLECT_METHOD_CACHE.containsKey(key)) return REFLECT_METHOD_CACHE.get(key);
            if (REFLECT_METHOD_MISS_CACHE.contains(key)) return null;
            try {
                Method m = cls.getMethod(methodName, types);
                REFLECT_METHOD_CACHE.put(key, m);
                return m;
            } catch (Throwable e) {
                REFLECT_METHOD_MISS_CACHE.add(key);
                return null;
            }
        }
    }

    private Object invokeStore(DbStore store, String methodName, Class[] types, Object[] args) {
        if (store == null || methodName == null || methodName.length() == 0) return null;
        String callKey =
                runStoreCacheActive ? buildStoreCallKey(store, methodName, types, args) : "";
        if (runStoreCacheActive && runStoreResultCache.containsKey(callKey)) {
            runStoreCacheHit++;
            Object cached = runStoreResultCache.get(callKey);
            return cached == NULL_STORE_RESULT ? null : cached;
        }
        Object out = null;
        try {
            Method m = cachedStoreMethod(store.getClass(), methodName, types);
            if (m != null) out = m.invoke(store, args);
        } catch (Throwable e) {
            out = null;
        }
        if (runStoreCacheActive) {
            runStoreCacheMiss++;
            runStoreResultCache.put(callKey, out == null ? NULL_STORE_RESULT : out);
        }
        return out;
    }

    private static int readInt(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private static double readDouble(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private static String readString(Object obj, String fieldName, String def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            Object v = f.get(obj);

            if (v == null) {
                return def;
            }

            return String.valueOf(v);
        } catch (Throwable e) {
        }

        return def;
    }

    private static void writeInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            f.set(obj, Integer.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private static void writeDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            f.set(obj, Double.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private static void writeString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = cachedPublicField(obj.getClass(), fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private boolean readAppBoolean(String fieldName, boolean def) {
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Boolean) {
                return ((Boolean) v).booleanValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue() != 0;
            }

            if (v instanceof String) {
                return "1".equals(v) || "true".equalsIgnoreCase((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private int readAppInt(String fieldName, int def) {
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readAppDouble(String fieldName, double def) {
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);

                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readAppString(String fieldName, String def) {
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double minRealBetP() {
        return readAppDouble(
                "MIN_REAL_BET_PROBABILITY", readAppDouble("MIN_REAL_BET_PROB", MIN_REAL_BET_P));
    }

    private double riskGateHigh() {
        return readAppDouble("RISK_GATE_HIGH", 0.560d);
    }

    private int minCacheSampleForBet() {
        return readAppInt("MIN_CACHE_SAMPLE_FOR_BET", 18);
    }

    private int minStateSampleForBet() {
        return readAppInt("MIN_STATE_SAMPLE_FOR_BET", 18);
    }

    private String runtimeProfileWeakBlockCode() {
        return readAppString("BLOCK_RUNTIME_PROFILE_WEAK", "RUNTIME_PROFILE_WEAK");
    }

    private String teacherMatrixWeakBlockCode() {
        return readAppString("BLOCK_TEACHER_MATRIX_WEAK", "TEACHER_MATRIX_WEAK");
    }

    private double teacherMatrixProbAdjust() {
        return readAppDouble("TEACHER_MATRIX_PROB_ADJUST", 0.012d);
    }

    private double teacherMatrixStrongProbAdjust() {
        return readAppDouble("TEACHER_MATRIX_STRONG_PROB_ADJUST", 0.020d);
    }

    private double teacherMatrixStrongGateAdjust() {
        return readAppDouble("TEACHER_MATRIX_STRONG_GATE_ADJUST", -0.010d);
    }

    private double teacherMatrixWeakProbAdjust() {
        return readAppDouble("TEACHER_MATRIX_WEAK_PROB_ADJUST", -0.018d);
    }

    private double teacherMatrixWeakGateAdjust() {
        return readAppDouble("TEACHER_MATRIX_WEAK_GATE_ADJUST", 0.014d);
    }

    private static final class HistoricalStateSignal {
        public double stateP;
        public double bonus;
        public double penalty;
        public int sample;
        public double hitRate16;
        public double hitRate32;
        public double hitRate64;
        public double hitRate128;
        public double zoneSupport;
        public double paritySupport;
        public int lastSeenGap;
        public String summary;

        public HistoricalStateSignal() {
            stateP = AppConfig.RANDOM_K3_BASELINE;
            bonus = 0.0d;
            penalty = 0.0d;
            sample = 0;
            hitRate16 = 0.0d;
            hitRate32 = 0.0d;
            hitRate64 = 0.0d;
            hitRate128 = 0.0d;
            zoneSupport = 0.0d;
            paritySupport = 0.0d;
            lastSeenGap = 999;
            summary = "";
        }
    }

    private static final class LearningSignal {
        public double learningP;
        public double bonus;
        public double penalty;
        public double avgSupport;
        public int supportCount;
        public String summary;

        public LearningSignal() {
            learningP = AppConfig.RANDOM_K3_BASELINE;
            bonus = 0.0d;
            penalty = 0.0d;
            avgSupport = 0.0d;
            supportCount = 0;
            summary = "";
        }
    }

    private static final class NumberShapeSignal {
        public double numberShapeP;
        public double bonus;
        public double penalty;
        public String summary;

        public NumberShapeSignal() {
            numberShapeP = AppConfig.RANDOM_K3_BASELINE;
            bonus = 0.0d;
            penalty = 0.0d;
            summary = "";
        }
    }

    private static final class CoverageQualitySignal {
        public double beforeP;
        public double coverageQualityScore;
        public double thirdNumberQualityScore;
        public double k3CoverageRisk;
        public int falseHighPFlag;
        public int preferredEdgeBand;
        public int highEdgeFlag;
        public int preferredProbabilityBand;
        public int teacherWeakOrEmpty;
        public int v20UnknownOrEmpty;
        public int hasStrongCause;
        public double coverageDiscount;
        public double coverageBoost;
        public double k3HitRate32;
        public double k3HitRate64;
        public double k3HitRate128;
        public double numberRate1;
        public double numberRate2;
        public double numberRate3;
        public String coverageSummary;
        public String actionAdvice;

        public CoverageQualitySignal() {
            beforeP = 0.0d;
            coverageQualityScore = 0.0d;
            thirdNumberQualityScore = 0.0d;
            k3CoverageRisk = 0.0d;
            falseHighPFlag = 0;
            preferredEdgeBand = 0;
            highEdgeFlag = 0;
            preferredProbabilityBand = 0;
            teacherWeakOrEmpty = 0;
            v20UnknownOrEmpty = 0;
            hasStrongCause = 0;
            coverageDiscount = 0.0d;
            coverageBoost = 0.0d;
            k3HitRate32 = AppConfig.RANDOM_K3_BASELINE;
            k3HitRate64 = AppConfig.RANDOM_K3_BASELINE;
            k3HitRate128 = AppConfig.RANDOM_K3_BASELINE;
            numberRate1 = BASE_NUMBER_RATE;
            numberRate2 = BASE_NUMBER_RATE;
            numberRate3 = BASE_NUMBER_RATE;
            coverageSummary = "";
            actionAdvice = "";
        }
    }

    private static final class MainChainProbabilitySignal {
        double beforeP;
        double rawP;
        double probBoost;
        double probDiscount;
        double evidenceScore;
        double counterScore;
        double netEvidenceScore;
        double positionNeedScore;
        double numberSupplyScore;
        double coverageQuality;
        double thirdQuality;
        double ruleHealthScore;
        double shadowPromoteScore;
        double adaptiveScore;
        double sampleReliability;
        double recentValidity;
        int noSampleNeutral;
        int watchNeutral;
        int hardRisk;
        String rejectCode;
        String actionAdvice;
        String summary;
        String neutralReasons;

        MainChainProbabilitySignal() {
            beforeP = 0.0d;
            rawP = 0.0d;
            probBoost = 0.0d;
            probDiscount = 0.0d;
            evidenceScore = 0.0d;
            counterScore = 0.0d;
            netEvidenceScore = 0.0d;
            positionNeedScore = 0.0d;
            numberSupplyScore = 0.0d;
            coverageQuality = 0.0d;
            thirdQuality = 0.0d;
            ruleHealthScore = 0.0d;
            shadowPromoteScore = 0.0d;
            adaptiveScore = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            noSampleNeutral = 0;
            watchNeutral = 0;
            hardRisk = 0;
            rejectCode = "PROB_MAIN_UNKNOWN";
            actionAdvice = "WATCH";
            summary = "";
            neutralReasons = "";
        }
    }

    private static final class DiagnosisProbabilitySignal {
        public double beforeP;
        public double rawP;
        public double moduleAdjustedP;
        public double freezeAdjustedP;
        public double shadowPromotionBoost;
        public double totalDiscount;
        public double totalBoost;
        public double teacherDiscount;
        public double v20Discount;
        public double sampleDiscount;
        public double recentDiscount;
        public double moduleDiscount;
        public double moduleBoost;
        public double freezeDiscount;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public int v20Empty;
        public String summary;
        public String actionAdvice;
        public String riskReasons;
        public String evidenceReasons;

        public DiagnosisProbabilitySignal() {
            beforeP = 0.0d;
            rawP = 0.0d;
            moduleAdjustedP = 0.0d;
            freezeAdjustedP = 0.0d;
            shadowPromotionBoost = 0.0d;
            totalDiscount = 0.0d;
            totalBoost = 0.0d;
            teacherDiscount = 0.0d;
            v20Discount = 0.0d;
            sampleDiscount = 0.0d;
            recentDiscount = 0.0d;
            moduleDiscount = 0.0d;
            moduleBoost = 0.0d;
            freezeDiscount = 0.0d;
            evidenceContribution = 0.0d;
            counterContribution = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            v20Empty = 0;
            summary = "";
            actionAdvice = "";
            riskReasons = "";
            evidenceReasons = "";
        }
    }

    private static final class RuntimeProfileSignal {
        public int applied;
        public int sample;
        public double runtimeP;
        public double hitRate;
        public double confidence;
        public double bonus;
        public double penalty;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;
        public String stateKey;
        public String profileKey;
        public String status;
        public String summary;

        public RuntimeProfileSignal() {
            applied = 0;
            sample = 0;
            runtimeP = AppConfig.RANDOM_K3_BASELINE;
            hitRate = 0.0d;
            confidence = 0.0d;
            bonus = 0.0d;
            penalty = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            stateKey = "";
            profileKey = "";
            status = "NONE";
            summary = "";
        }
    }

    private static final class StateNumberProfileSignal {
        public int applied;
        public int found;
        public int sample;
        public double stateNumberP;
        public double hitRate;
        public double confidence;
        public double bonus;
        public double penalty;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;
        public String stateKey;
        public String profileKey;
        public String status;
        public String summary;

        public StateNumberProfileSignal() {
            applied = 0;
            found = 0;
            sample = 0;
            stateNumberP = AppConfig.RANDOM_K3_BASELINE;
            hitRate = 0.0d;
            confidence = 0.0d;
            bonus = 0.0d;
            penalty = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            stateKey = "";
            profileKey = "";
            status = "NONE";
            summary = "";
        }
    }

    private static final class TeacherMatrixSignal {
        public int applied;
        public int profileFound;
        public int strongProfile;
        public int sample;
        public int numberSample;
        public int comboSample;
        public double teacherP;
        public double hitRate;
        public double comboHitRate;
        public double confidence;
        public double bonus;
        public double penalty;
        public double probAdjust;
        public double gateAdjust;
        public double riskPenalty;
        public String stateKey;
        public String profileKey;
        public String comboKey;
        public String status;
        public String summary;

        public TeacherMatrixSignal() {
            applied = 0;
            profileFound = 0;
            strongProfile = 0;
            sample = 0;
            numberSample = 0;
            comboSample = 0;
            teacherP = AppConfig.RANDOM_K3_BASELINE;
            hitRate = 0.0d;
            comboHitRate = 0.0d;
            confidence = 0.0d;
            bonus = 0.0d;
            penalty = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;
            stateKey = "";
            profileKey = "";
            comboKey = "";
            status = "NONE";
            summary = "";
        }

        public boolean statusStrong() {
            return "STRONG".equals(status) || "READY".equals(status);
        }
    }

    private static final class WindowBucket {
        public int windowSize;
        public int sample;
        public int hit;
        public int lastSeenGap;
        public int recentLoseStreak;
        public double hitRate;
        public double density;
        public double repeatScore;
        public double parityBias;
        public double bigSmallBias;
        public double neighborTail;
        public double positionCrowd;
        public double crossPositionSync;
        public double timingOffset;

        public WindowBucket() {
            windowSize = 0;
            sample = 0;
            hit = 0;
            lastSeenGap = 999;
            recentLoseStreak = 0;
            hitRate = AppConfig.RANDOM_K3_BASELINE;
            density = 0.0d;
            repeatScore = 0.0d;
            parityBias = 0.50d;
            bigSmallBias = 0.50d;
            neighborTail = 0.0d;
            positionCrowd = 0.0d;
            crossPositionSync = 0.0d;
            timingOffset = 0.50d;
        }
    }

    private static final class V20WindowStateSignal {
        public int valid;
        public int sample;
        public String stateKey;
        public String omissionBucket;
        public double hitRate12;
        public double hitRate24;
        public double hitRate48;
        public double hitRate96;
        public double hitRate360;
        public double hotColdSlope;
        public double density;
        public double repeatScore;
        public double parityBias;
        public double bigSmallBias;
        public double neighborTail;
        public double positionCrowd;
        public double crossPositionSync;
        public int recentLoseStreak;
        public double timingOffset;
        public double evidenceContribution;
        public double counterEvidence;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public String actionAdvice;
        public String summary;

        public V20WindowStateSignal() {
            valid = 0;
            sample = 0;
            stateKey = "";
            omissionBucket = "MISS_UNKNOWN";
            hitRate12 = AppConfig.RANDOM_K3_BASELINE;
            hitRate24 = AppConfig.RANDOM_K3_BASELINE;
            hitRate48 = AppConfig.RANDOM_K3_BASELINE;
            hitRate96 = AppConfig.RANDOM_K3_BASELINE;
            hitRate360 = AppConfig.RANDOM_K3_BASELINE;
            hotColdSlope = 0.0d;
            density = 0.0d;
            repeatScore = 0.0d;
            parityBias = 0.50d;
            bigSmallBias = 0.50d;
            neighborTail = 0.0d;
            positionCrowd = 0.0d;
            crossPositionSync = 0.0d;
            recentLoseStreak = 0;
            timingOffset = 0.50d;
            evidenceContribution = 0.0d;
            counterEvidence = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            featureDistance = 1.0d;
            actionAdvice = "";
            summary = "";
        }
    }

    private static final class V20DynamicCauseSignal {
        public int applied;

        public double dynamicP;
        public double dynamicScore;
        public double dynamicSupport;
        public double dynamicRisk;
        public double confidence;

        public double ruleScore;
        public double ruleRisk;
        public double shadowScore;
        public double shadowRisk;
        public double seedScore;
        public double seedRisk;
        public double freezeRisk;
        public int freezeFlag;
        public int freezeHard;
        public double thirdGuardScore;
        public double thirdGuardRisk;
        public double rankScore;
        public double rankRisk;
        public double positionScore;
        public double positionRisk;
        public double teacherScore;
        public double teacherRisk;

        public int dynamicPass;
        public int dynamicHigh;
        public int dynamicHard;
        public int rulePass;
        public int shadowPass;
        public int seedPass;
        public int hardRisk;

        public double bonus;
        public double penalty;
        public double probAdjust;
        public double gateAdjust;
        public double baseGateAdjust;
        public double riskPenalty;
        public double evidenceContribution;
        public double counterEvidence;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;

        public String windowStateKey;
        public String windowStateSummary;
        public String actionAdvice;
        public String grade;
        public String summary;

        public V20DynamicCauseSignal() {
            applied = 0;
            dynamicP = AppConfig.RANDOM_K3_BASELINE;
            dynamicScore = 0.0d;
            dynamicSupport = 0.0d;
            dynamicRisk = 0.0d;
            confidence = 0.0d;

            ruleScore = 0.0d;
            ruleRisk = 0.0d;
            shadowScore = 0.0d;
            shadowRisk = 0.0d;
            seedScore = 0.0d;
            seedRisk = 0.0d;
            freezeRisk = 0.0d;
            freezeFlag = 0;
            freezeHard = 0;
            thirdGuardScore = 0.0d;
            thirdGuardRisk = 0.0d;
            rankScore = 0.0d;
            rankRisk = 0.0d;
            positionScore = 0.0d;
            positionRisk = 0.0d;
            teacherScore = 0.0d;
            teacherRisk = 0.0d;

            dynamicPass = 0;
            dynamicHigh = 0;
            dynamicHard = 0;
            rulePass = 0;
            shadowPass = 0;
            seedPass = 0;
            hardRisk = 0;

            bonus = 0.0d;
            penalty = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            baseGateAdjust = 0.0d;
            riskPenalty = 0.0d;
            evidenceContribution = 0.0d;
            counterEvidence = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            featureDistance = 0.0d;

            windowStateKey = "";
            windowStateSummary = "";
            actionAdvice = "";
            grade = "NONE";
            summary = "";
        }
    }

    private double firstPositiveProbability(double a, double b, double c, double d) {
        if (a > 0.0d) return AppConfig.clampProbability(a);
        if (b > 0.0d) return AppConfig.clampProbability(b);
        if (c > 0.0d) return AppConfig.clampProbability(c);
        return AppConfig.clampProbability(d);
    }

    /** 只记录当前目标期的概率分布，不参与FinalP、Gate或BET。 用于验证命中样本是否真正集中在高P段，避免只看单个冠军。 */
    private void saveActiveCohortProbabilityDistribution(
            DbStore store, List<Models.ComboCandidate> list, long costMs) {
        if (store == null || list == null) return;
        try {
            int total = 0;
            int lt30 = 0;
            int p30_34 = 0;
            int p34_36 = 0;
            int p36_38 = 0;
            int ge38 = 0;
            double min = 1.0d;
            double max = 0.0d;
            double sum = 0.0d;
            StringBuilder top = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                Models.ComboCandidate c = list.get(i);
                if (c == null || !c.isValidK3()) continue;
                double p = AppConfig.clampProbability(c.finalP);
                total++;
                sum += p;
                if (p < min) min = p;
                if (p > max) max = p;
                if (p < 0.300d) lt30++;
                else if (p < 0.340d) p30_34++;
                else if (p < 0.360d) p34_36++;
                else if (p < 0.380d) p36_38++;
                else ge38++;
                if (i < 8) {
                    if (top.length() > 0) top.append('|');
                    top.append(i + 1)
                            .append(':')
                            .append(c.position)
                            .append('/')
                            .append(c.n1)
                            .append(',')
                            .append(c.n2)
                            .append(',')
                            .append(c.n3)
                            .append('@')
                            .append(Math.round(p * 10000.0d) / 100.0d)
                            .append('%');
                }
            }
            if (total <= 0) min = 0.0d;
            HashMap<String, String> state = new HashMap<String, String>();
            state.put("probability_active_cohort", AppConfig.ACTIVE_FIX_COHORT);
            state.put("probability_active_pipeline", AppConfig.ACTIVE_PROBABILITY_PIPELINE_VERSION);
            state.put("probability_distribution_total", String.valueOf(total));
            state.put("probability_distribution_lt30", String.valueOf(lt30));
            state.put("probability_distribution_30_34", String.valueOf(p30_34));
            state.put("probability_distribution_34_36", String.valueOf(p34_36));
            state.put("probability_distribution_36_38", String.valueOf(p36_38));
            state.put("probability_distribution_ge38", String.valueOf(ge38));
            state.put("probability_distribution_min", String.valueOf(min));
            state.put("probability_distribution_max", String.valueOf(max));
            state.put(
                    "probability_distribution_avg",
                    String.valueOf(total <= 0 ? 0.0d : sum / total));
            state.put("probability_distribution_top", top.toString());
            state.put("probability_distribution_cost_ms", String.valueOf(costMs));
            state.put(
                    "probability_distribution_updated_at",
                    String.valueOf(System.currentTimeMillis()));
            store.setStates(state);
        } catch (Throwable ignore) {
        }
    }

    private void writeFullClosedLoopProbabilityState(DbStore store, int candidateCount) {
        try {
            if (store != null) {
                store.setState(
                        AppConfig.STATE_FULL_CLOSED_LOOP_VERSION,
                        AppConfig.FULL_CLOSED_LOOP_18_VERSION);
                store.setState(
                        "probability_candidate_domain_count", String.valueOf(candidateCount));
            }
        } catch (Throwable ignore) {
        }
    }

    private boolean isTeacherSignalNoSampleNeutral(TeacherMatrixSignal s) {
        if (s == null) return true;
        String st = upper(s.status);
        if (isNoSampleStatus(st) || st.indexOf("NEUTRAL") >= 0) return true;
        return s.sample <= 0
                && s.comboSample <= 0
                && s.hitRate <= 0.0d
                && s.comboHitRate <= 0.0d
                && s.riskPenalty <= 0.0d
                && s.penalty <= 0.0d;
    }

    private void applyNoSampleNeutral73487246(Models.ComboCandidate c) {
        if (c == null) return;
        int teacherSample =
                Math.max(
                        readInt(c, "teacherMatrixSample", 0), readInt(c, "teacherNumberSample", 0));
        int dynamicSample = readInt(c, "dynamicClusterSample", 0);
        if (teacherSample <= 0) {
            writeDouble(c, "teacherMatrixProbAdjust", 0.0d);
            writeDouble(c, "teacherProbAdjust", 0.0d);
            writeDouble(c, "teacherMatrixGateAdjust", 0.0d);
        }
        if (dynamicSample <= 0) {
            writeDouble(c, "v20EvidenceContribution", 0.0d);
            writeDouble(c, "v20CounterContribution", 0.0d);
        }
    }
}
