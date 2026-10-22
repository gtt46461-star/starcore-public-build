package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * DynamicGateEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环 · 质量风险动态门槛满血版（第13/14）
 *
 * <p>核心原则： 1. 不继承候选残留的异常高门槛。 2. 恢复老版频率动态门：高频软收紧/抬门槛，不在本层直接按频率硬拦截。 3.
 * 低频不能单独救援，只在强证据、低风险、样本/教师/V20因链支持时极小幅放松。 4. TeacherMatrix / Learning / Profile / Promotion / A+ /
 * V20因链 都不能绕过最终门直接救援 BET。 5. V20动态因链、规则健康、第三防线、影子健康、伪强冻结、反事实排名、教师号码、位置健康、种子扩展只参与： - 门槛微调 - 排序 -
 * 风险硬块 - 解释 6. 所有候选都返回，不砍候选。 7. edge >= 0 表示通过动态门；最终是否 BET 交给 DecisionGateEngine。 8. 第13/14接入
 * Probability诊断字段、ModuleEffect、ShadowPromotion、DynamicFreeze、AdaptiveJudge 缓存。 9. Java7 / AIDE 兼容。
 */
public final class DynamicGateEngine {
    public static final String V46_ROLE = "EVIDENCE_GATE_PRODUCER_NOT_BET_OWNER";

    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD = AppConfig.V46_VERSION;

    private static final int REALTIME_DEEP_GATE_EVAL_LIMIT = 32;
    private static final int REALTIME_HEAVY_EVAL_LIMIT = 12;
    private static final int BACKTEST_HEAVY_EVAL_LIMIT = 18;

    private static final long REALTIME_GATE_BUDGET_MS = AppConfig.REALTIME_DYNAMIC_GATE_TARGET_MS;
    private static final String REALTIME_VALID_DOMAIN_DYNAMIC_GATE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String BACKGROUND_EVIDENCE_DYNAMIC_GATE_VERSION =
            AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final long BACKTEST_GATE_BUDGET_MS = 8500L;
    private static final long FREQ_CACHE_TTL_MS = 2500L;

    private static final double ORIGINAL_BASE_GATE = 0.385d;
    private static final double ORIGINAL_MIN_GATE = 0.345d;
    private static final double ORIGINAL_NORMAL_CAP = 0.405d;
    private static final double ORIGINAL_STRONG_CAP = 0.398d;
    private static final double ORIGINAL_VERY_STRONG_CAP = 0.392d;
    private static final double ORIGINAL_RISK_CAP = 0.425d;
    private static final double ORIGINAL_HARD_RISK_CAP = 0.455d;

    private static final double NORMAL_SOFT_FLOOR = 0.375d;
    private static final double STRONG_SOFT_FLOOR = 0.365d;
    private static final double VERY_STRONG_SOFT_FLOOR = 0.355d;

    private static final double STALE_GATE_HIGH = 0.418d;
    private static final double EDGE_SOFT_MIN = 0.000d;
    private static final double EDGE_STRONG_MIN = 0.004d;

    private static final double FREQ_NEUTRAL_WHEN_UNKNOWN = 0.10d;

    private static final double FREQ_LOW_STRONG_RELAX = -0.004d;
    private static final double FREQ_LOW_STRONG_RELAX_LIGHT = -0.002d;
    private static final double FREQ_WARN_GATE_BOOST = 0.004d;
    private static final double FREQ_HIGH_GATE_BOOST = 0.007d;
    private static final double FREQ_HIGH_WEAK_GATE_BOOST = 0.012d;
    private static final double FREQ_OVERHEAT_GATE_BOOST = 0.018d;
    private static final double FREQ_HARD_OVERHEAT_GATE_BOOST = 0.024d;

    private static final int TEACHER_MATRIX_MIN_SAMPLE = 30;
    private static final int TEACHER_MATRIX_STRONG_SAMPLE = 80;
    private static final int TEACHER_MATRIX_COMBO_MIN_SAMPLE = 12;
    private static final int TEACHER_MATRIX_COMBO_STRONG_SAMPLE = 28;

    private static final int PROFILE_SAMPLE_MIN = 8;
    private static final int PROFILE_SAMPLE_STRONG = 18;
    private static final int V20_GATE_WINDOW_12 = 12;
    private static final int V20_GATE_WINDOW_24 = 24;
    private static final int V20_GATE_WINDOW_48 = 48;
    private static final int V20_GATE_WINDOW_96 = 96;
    private static final int V20_GATE_WINDOW_360 = 360;
    private static final double V20_GATE_MAX_WINDOW_RELAX = -0.006d;
    private static final double V20_GATE_MAX_WINDOW_BOOST = 0.012d;
    private static final double V20_GATE_MAX_ACTION_RELAX = -0.004d;
    private static final double V20_GATE_MAX_ACTION_BOOST = 0.010d;
    private static final double V20_GATE_HARD_COUNTER_LINE = 0.720d;
    private static final double V20_GATE_STRONG_EVIDENCE_LINE = 0.620d;
    private static final double QUALITY_GATE_MAX_RELAX = -0.010d;
    private static final double QUALITY_GATE_MAX_BOOST = 0.026d;
    private static final double DIAG_GATE_MAX_BOOST = 0.020d;
    private static final double MODULE_EFFECT_GATE_MAX_BOOST = 0.014d;
    private static final double FREEZE_GATE_MAX_BOOST = 0.026d;
    private static final double SHADOW_PROMOTION_GATE_MAX_RELAX = -0.004d;
    private static final double QUALITY_EVIDENCE_STRONG = 0.600d;
    private static final double QUALITY_COUNTER_HIGH = 0.660d;
    private static final double QUALITY_SAMPLE_MIN = 0.260d;
    private static final double QUALITY_RECENT_MIN = 0.280d;
    private static final String V20_DYNAMIC_GATE_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_DYNAMIC_GATE_UPSTREAM_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String V20_DYNAMIC_GATE_MAIN_CHAIN_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String LINK_MODIFICATION_V2_DYNAMIC_GATE_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String STATUS_NO_SAMPLE_NEUTRAL = "NO_SAMPLE_NEUTRAL";
    private static final String STATUS_V20_WATCH_NEUTRAL = "V20_WATCH_NEUTRAL";
    private static final String STATUS_ADAPTIVE_WATCH_NEUTRAL = "ADAPTIVE_WATCH_NEUTRAL";
    private static final double MAIN_CHAIN_GATE_MAX_RELAX = -0.004d;
    private static final double MAIN_CHAIN_GATE_MAX_BOOST = 0.018d;
    private static final double UPSTREAM_GATE_MISSING_COVERAGE_RISK = 0.720d;
    private static final double UPSTREAM_GATE_MIN_COVERAGE_QUALITY = 0.180d;
    private static final double UPSTREAM_GATE_MIN_THIRD_QUALITY = 0.055d;
    private static final double UPSTREAM_GATE_READY_COVERAGE_MAX_RISK = 0.500d;
    private static final double UPSTREAM_GATE_READY_COUNTER_MAX = 0.580d;
    private static final double PREFERRED_P_MIN = 0.380d;
    private static final double PREFERRED_P_MAX = 0.400d;
    private static final double PREFERRED_EDGE_MIN = 0.010d;
    private static final double PREFERRED_EDGE_MAX = 0.020d;
    private static final double EDGE_FALSE_STRONG_LINE = 0.020d;
    private static final double COVERAGE_GATE_MAX_BOOST = 0.020d;
    private static final double EDGE_QUALITY_GATE_MAX_BOOST = 0.018d;
    private static final double EDGE_QUALITY_GATE_MAX_RELAX = -0.003d;
    private static final double K3_COVERAGE_RISK_LINE = 0.620d;
    private static final double THIRD_NUMBER_QUALITY_MIN = 0.088d;

    private final TrendStateMachine trendStateMachine;
    private final BreakRecoveryEngine breakRecoveryEngine;
    private final SimilarShapeEngine similarShapeEngine;
    private final ShapeMemoryEngine memoryEngine;
    private final CalibrationEngine calibrationEngine;

    private long freqCacheAt100;
    private long freqCacheAt500;
    private long freqCacheAt1000;
    private double freqCache100;
    private double freqCache500;
    private double freqCache1000;

    public DynamicGateEngine() {
        trendStateMachine = new TrendStateMachine();
        breakRecoveryEngine = new BreakRecoveryEngine();
        similarShapeEngine = new SimilarShapeEngine();
        memoryEngine = new ShapeMemoryEngine();
        calibrationEngine = new CalibrationEngine();

        freqCacheAt100 = 0L;
        freqCacheAt500 = 0L;
        freqCacheAt1000 = 0L;
        freqCache100 = FREQ_NEUTRAL_WHEN_UNKNOWN;
        freqCache500 = FREQ_NEUTRAL_WHEN_UNKNOWN;
        freqCache1000 = FREQ_NEUTRAL_WHEN_UNKNOWN;
    }

    /** AutoFetchService 第6/6 实时反射入口。 只做动态质量门字段写回，不创建BET、不保存Decision、不结算。 */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeUpstreamGate(
                store, candidates, history, baseIssue, targetIssue, "applyToAll");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeUpstreamGate(
                store, candidates, history, baseIssue, targetIssue, "enrichCandidates");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeUpstreamGate(
                store, candidates, history, baseIssue, targetIssue, "analyzeCandidates");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeUpstreamGate(
                null, candidates, history, baseIssue, targetIssue, "applyRealtime");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyRealtimeUpstreamGate(
                null, candidates, history, baseIssue, targetIssue, "runRealtime");
    }

    public ArrayList<Models.ComboCandidate> applyRealtimeUpstreamGate(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue,
            String entryName) {
        ArrayList<Models.ComboCandidate> in =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        if (in.size() == 0) {
            return in;
        }
        ArrayList<Models.ComboCandidate> out =
                applyToCandidatesInternal(
                        store,
                        in,
                        new ArrayList<TrendStateMachine.TrendTrace>(),
                        new ArrayList<BreakRecoveryEngine.RecoveryResult>(),
                        history,
                        AppConfig.BACKTEST_MODE_FAST,
                        REALTIME_DEEP_GATE_EVAL_LIMIT);
        if (out == null) {
            out = new ArrayList<Models.ComboCandidate>();
        }
        int total = 0;
        int preferred = 0;
        int missing = 0;
        int falseStrong = 0;
        int risk = 0;
        String first = "";
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) continue;
            total++;
            if (baseIssue != null && baseIssue.length() > 0) c.baseIssue = baseIssue;
            if (targetIssue != null && targetIssue.length() > 0) c.targetIssue = targetIssue;
            ensureUpstreamQualityBeforeGate(c, history, "AFTER_REALTIME_GATE_" + entryName);
            String qs = readString(c, "dynamicGateQualityStatus", "");
            if ("PREFERRED_REVIEW".equals(qs)) preferred++;
            if ("MISSING_UPSTREAM".equals(qs) || "V20_TEACHER_COVER_MISSING".equals(qs)) missing++;
            if ("EDGE_FALSE_STRONG".equals(qs)
                    || readDouble(c, "dynamicGateVirtualEdgeRisk", 0.0d) >= 0.50d) falseStrong++;
            if ("HARD_RISK".equals(qs)
                    || readDouble(c, "dynamicGateCoverageRisk", 0.0d) >= K3_COVERAGE_RISK_LINE)
                risk++;
            if (first.length() == 0) {
                first = buildUpstreamGateRuntimeSummary(c);
            }
        }
        if (store != null) {
            try {
                store.setState("dynamic_gate_upstream_version", V20_DYNAMIC_GATE_UPSTREAM_VERSION);
                store.setState("dynamic_gate_upstream_entry", safeLocal(entryName));
                store.setState("dynamic_gate_upstream_total", String.valueOf(total));
                store.setState("dynamic_gate_upstream_preferred", String.valueOf(preferred));
                store.setState("dynamic_gate_upstream_missing", String.valueOf(missing));
                store.setState("dynamic_gate_upstream_false_strong", String.valueOf(falseStrong));
                store.setState("dynamic_gate_upstream_risk", String.valueOf(risk));
                store.setState("dynamic_gate_upstream_best", first);
            } catch (Throwable e) {
            }
        }
        return out;
    }

    public GateSignal calculate(
            DbStore store,
            Models.ComboCandidate candidate,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CalibrationEngine.CalibrationSignal calibrationSignal) {

        GateBatchContext ctx =
                buildBatchContext(store, AppConfig.BACKTEST_MODE_FAST, REALTIME_GATE_BUDGET_MS);
        return calculateWithContext(
                ctx,
                store,
                candidate,
                trendTrace,
                recoveryResult,
                similarResult,
                memorySignal,
                calibrationSignal);
    }

    public GateSignal calculateSimple(DbStore store, Models.ComboCandidate candidate) {
        CalibrationEngine.CalibrationSignal cal = null;

        try {
            if (candidate != null) {
                cal = calibrationEngine.inspect(store, candidate.rawP);
            }
        } catch (Throwable e) {
            cal = null;
        }

        return calculate(store, candidate, null, null, null, null, cal);
    }

    public Models.ComboCandidate applyToCandidate(
            DbStore store,
            Models.ComboCandidate source,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CalibrationEngine.CalibrationSignal calibrationSignal) {

        GateBatchContext ctx =
                buildBatchContext(store, AppConfig.BACKTEST_MODE_FAST, REALTIME_GATE_BUDGET_MS);

        return applyToCandidateWithContext(
                ctx,
                store,
                source,
                trendTrace,
                recoveryResult,
                similarResult,
                memorySignal,
                calibrationSignal,
                (List<Models.Draw>) null);
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            DbStore store,
            List<Models.ComboCandidate> input,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            List<Models.Draw> history) {

        return applyToCandidatesInternal(
                store,
                input,
                traces,
                recoveries,
                history,
                AppConfig.BACKTEST_MODE_FAST,
                REALTIME_DEEP_GATE_EVAL_LIMIT);
    }

    public ArrayList<Models.ComboCandidate> applyToCandidatesForBacktest(
            DbStore store,
            List<Models.ComboCandidate> input,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            List<Models.Draw> history,
            String mode) {

        mode = AppConfig.normalizeBacktestMode(mode);

        int deepLimit = 0;

        try {
            deepLimit = AppConfig.getProbabilityDeepLimit(mode);
        } catch (Throwable e) {
            deepLimit = 0;
        }

        if (deepLimit <= 0) {
            if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
                deepLimit = 64;
            } else if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
                deepLimit = 48;
            } else {
                deepLimit = REALTIME_DEEP_GATE_EVAL_LIMIT;
            }
        }

        return applyToCandidatesInternal(
                store, input, traces, recoveries, history, mode, deepLimit);
    }

    private ArrayList<Models.ComboCandidate> applyToCandidatesInternal(
            DbStore store,
            List<Models.ComboCandidate> input,
            List<TrendStateMachine.TrendTrace> traces,
            List<BreakRecoveryEngine.RecoveryResult> recoveries,
            List<Models.Draw> history,
            String mode,
            int deepLimit) {

        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (input == null || input.size() == 0) {
            return out;
        }

        mode = AppConfig.normalizeBacktestMode(mode);

        if (deepLimit < 0) {
            deepLimit = 0;
        }

        boolean realtime = AppConfig.BACKTEST_MODE_FAST.equals(mode);
        long budgetMs = realtime ? REALTIME_GATE_BUDGET_MS : BACKTEST_GATE_BUDGET_MS;

        GateBatchContext ctx = buildBatchContext(store, mode, budgetMs);
        ctx.inputCount = input.size();

        ArrayList<Models.ComboCandidate> prepared = prepareInput(input);

        int heavyLimit = realtime ? REALTIME_HEAVY_EVAL_LIMIT : BACKTEST_HEAVY_EVAL_LIMIT;
        if (realtime && AppConfig.REALTIME_DYNAMIC_GATE_USE_PRECOMPUTED_EVIDENCE) {
            heavyLimit =
                    0; // realtime v3: consume precomputed candidate evidence; no repeated
                       // Similar/Memory/Calibration SQLite work
        }

        if (deepLimit > 0 && heavyLimit > deepLimit) {
            heavyLimit = deepLimit;
        }

        if (heavyLimit > prepared.size()) {
            heavyLimit = prepared.size();
        }

        for (int i = 0; i < prepared.size(); i++) {
            Models.ComboCandidate c = prepared.get(i);

            if (c == null) {
                continue;
            }

            TrendStateMachine.TrendTrace trace = null;
            BreakRecoveryEngine.RecoveryResult recovery = null;
            Models.SimilarResult similar = null;
            ShapeMemoryEngine.MemorySignal memory = null;
            CalibrationEngine.CalibrationSignal calibration = null;

            try {
                trace = trendStateMachine.findTraceForPosition(traces, c.position);
            } catch (Throwable e) {
                trace = null;
            }

            try {
                recovery = breakRecoveryEngine.findForPosition(recoveries, c.position);
            } catch (Throwable e) {
                recovery = null;
            }

            if (i < heavyLimit && !ctx.isOverBudget()) {
                try {
                    similar = similarShapeEngine.evaluateCandidate(history, c);
                } catch (Throwable e) {
                    similar = null;
                }

                if (!ctx.isOverBudget()) {
                    try {
                        memory = memoryEngine.evaluateCandidate(c, history);
                    } catch (Throwable e) {
                        memory = null;
                    }
                }

                if (!ctx.isOverBudget()) {
                    try {
                        calibration = calibrationEngine.inspect(store, c.rawP);
                    } catch (Throwable e) {
                        calibration = null;
                    }
                }
            }

            Models.ComboCandidate gated =
                    applyToCandidateWithContext(
                            ctx, store, c, trace, recovery, similar, memory, calibration, history);

            if (gated != null) {
                gated.backtestMode = mode;
                out.add(gated);
            }
        }

        sortByFinal(out);
        rerank(out);
        markTopGap(out);
        saveDynamicGateRuntimeState(store, ctx, out);
        if (store != null) {
            try {
                store.setState(
                        "dynamic_gate_valid_domain_version",
                        REALTIME_VALID_DOMAIN_DYNAMIC_GATE_VERSION);
                store.setState(
                        "dynamic_gate_precomputed_only",
                        realtime && AppConfig.REALTIME_DYNAMIC_GATE_USE_PRECOMPUTED_EVIDENCE
                                ? "1"
                                : "0");
            } catch (Throwable e) {
            }
        }
        return out;
    }

    private Models.ComboCandidate applyToCandidateWithContext(
            GateBatchContext ctx,
            DbStore store,
            Models.ComboCandidate source,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CalibrationEngine.CalibrationSignal calibrationSignal,
            List<Models.Draw> history) {

        if (source == null) {
            return null;
        }

        Models.ComboCandidate c = source.copy();
        normalizeCandidate(c);
        ensureUpstreamQualityBeforeGate(c, history, "BEFORE_BUILD_V20_EVIDENCE");
        V20GateEvidence gateEvidence = buildV20GateEvidence(c, history, ctx);
        applyV20GateEvidenceToCandidate(c, gateEvidence);
        ensureUpstreamQualityBeforeGate(c, history, "AFTER_BUILD_V20_EVIDENCE");

        GateSignal gate =
                calculateWithContext(
                        ctx,
                        store,
                        c,
                        trendTrace,
                        recoveryResult,
                        similarResult,
                        memorySignal,
                        calibrationSignal);

        c.dynamicGate = gate.finalGate;
        c.edge = c.finalP - c.dynamicGate;
        writeString(c, "dynamicGateEvidenceVersion", BACKGROUND_EVIDENCE_DYNAMIC_GATE_VERSION);
        writeDouble(c, "noSampleGateAdjust", 0.0d);
        writeString(c, "noSampleGatePolicy", "NO_SAMPLE_WATCH_DO_NOT_RAISE_GATE");
        if (isNeutralEvidenceStatusV6(readString(c, "teacherMatrixStatus", ""))
                && isNeutralEvidenceStatusV6(readString(c, "v20Status", ""))
                && readInt(c, "dynamicHardRisk", 0) == 0) {
            double neutralBase = readBaseGate();
            if (c.dynamicGate > neutralBase) c.dynamicGate = neutralBase;
        }
        c.freq100 = gate.freqRate100;
        c.freq500 = gate.freqRate500;
        c.freq1000 = gate.freqRate1000;

        if (gate.hardBlock == 1) {
            c.blockCode = safeBlock(gate.blockCode, AppConfig.BLOCK_LOW_PROBABILITY);
        } else if (gate.allowedByGate == 0) {
            if (c.blockCode == null || c.blockCode.length() == 0) {
                c.blockCode = safeBlock(gate.blockCode, AppConfig.BLOCK_LOW_PROBABILITY);
            }
        } else {
            if (isSoftBlock(c.blockCode)) {
                c.blockCode = "";
            }
        }

        c.reasonSummary = appendReason(c.reasonSummary, gate.detail);

        writeDouble(c, "v20DynamicGate", gate.finalGate);
        writeDouble(c, "v20DynamicEdge", gate.edge);
        writeDouble(c, "v20DynamicStrengthScore", gate.strengthScore);
        writeDouble(c, "v20DynamicRiskScore", gate.riskScore);
        writeDouble(c, "v20DynamicSampleScore", gate.sampleScore);
        writeString(c, "v20DynamicGateSummary", gate.detail);
        writeString(c, "dynamicGateWindowStateKey", gate.v20WindowStateKey);
        writeString(c, "dynamicGateEvidenceText", gate.v20EvidenceText);
        writeString(c, "dynamicGateCounterEvidenceText", gate.v20CounterEvidenceText);
        writeString(c, "dynamicGateActionAdvice", gate.v20ActionAdvice);
        writeString(c, "dynamicGateAuditSummary", gate.v20AuditSummary);
        writeDouble(c, "dynamicGateEvidenceContribution", gate.v20EvidenceContribution);
        writeDouble(c, "dynamicGateCounterContribution", gate.v20CounterContribution);
        writeDouble(c, "dynamicGateSampleReliability", gate.v20SampleReliability);
        writeDouble(c, "dynamicGateRecentValidity", gate.v20RecentValidity);
        writeDouble(c, "dynamicGateFeatureDistance", gate.v20FeatureDistance);
        writeDouble(c, "dynamicGateWindowAdjust", gate.v20WindowAdjust);
        writeDouble(c, "dynamicGateActionAdjust", gate.v20ActionAdjust);

        writeDouble(c, "qualityGateAdjust", gate.qualityGateAdjust);
        writeDouble(c, "diagnosisGateAdjust", gate.diagnosisGateAdjust);
        writeDouble(c, "moduleEffectGateAdjust", gate.moduleEffectGateAdjust);
        writeDouble(c, "freezeGateAdjust", gate.freezeGateAdjust);
        writeDouble(c, "shadowPromotionGateAdjust", gate.shadowPromotionGateAdjust);
        writeDouble(c, "coverageGateAdjust", gate.coverageGateAdjust);
        writeDouble(c, "edgeQualityGateAdjust", gate.edgeQualityGateAdjust);
        writeDouble(
                c, "dynamicGateCoverageQualityScore", readDouble(c, "coverageQualityScore", 0.0d));
        writeDouble(c, "dynamicGateCoverageRisk", readDouble(c, "k3CoverageRisk", 0.0d));
        writeDouble(
                c, "dynamicGateThirdNumberQuality", readDouble(c, "thirdNumberQualityScore", 0.0d));
        writeInt(c, "dynamicGateFalseHighPFlag", readInt(c, "falseHighPFlag", 0));
        writeInt(c, "dynamicGatePreferredEdgeBand", isPreferredEdgeBand(c, gate) ? 1 : 0);
        writeDouble(c, "dynamicGateVirtualEdgeRisk", dynamicGateVirtualEdgeRisk(c, gate));
        writeString(c, "dynamicGateEdgePolicy", buildEdgePolicyText(c, gate));
        writeString(
                c,
                "dynamicGateQualityStatus",
                buildUpstreamGateQualityStatus(c, gate, gateEvidence));
        writeInt(
                c,
                "dynamicGateTeacherV20WeakFlag",
                (teacherWeakOrEmptyForGate(c, null) && v20UnknownOrWeakForGate(c, gateEvidence))
                        ? 1
                        : 0);
        writeString(c, "dynamicGateFieldVersion", V20_DYNAMIC_GATE_UPSTREAM_VERSION);
        writeString(c, "dynamicGateVersion", V20_DYNAMIC_GATE_UPSTREAM_VERSION);
        writeString(c, "dynamicGateQualitySummary", gate.qualitySummary);
        writeString(c, "dynamicGateFrequencyPolicy", gate.frequencyPolicy);
        writeString(
                c, "dynamicGateActionAdvice", buildUpstreamGateActionAdvice(c, gate, gateEvidence));
        writeString(
                c,
                "dynamicGateDecisionAdvice",
                readString(c, "dynamicGateActionAdvice", gate.actionAdvice));
        writeDouble(c, "mainChainGateAdjust", gate.mainChainGateAdjust);
        writeDouble(c, "dynamicGateMainChainAdjust", gate.mainChainGateAdjust);
        writeDouble(c, "dynamicGateMainChainEvidenceScore", gate.mainChainEvidenceScore);
        writeDouble(c, "dynamicGateMainChainCounterScore", gate.mainChainCounterScore);
        writeDouble(c, "dynamicGateMainChainNetScore", gate.mainChainNetScore);
        writeString(c, "dynamicGateMainChainRejectCode", gate.mainChainRejectCode);
        writeString(c, "dynamicGateMainChainActionAdvice", gate.mainChainActionAdvice);
        writeString(c, "dynamicGateMainChainSummary", gate.mainChainSummary);
        writeString(
                c, "dynamicGateMainChainVersion", V20_DYNAMIC_GATE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        writeString(c, "fieldChainVersion", V20_DYNAMIC_GATE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        applyQualifiedFrequencyFloorFields(c, gate, ctx);
        return c;
    }

    private Models.FrequencyState loadFrequencyStateSafe(DbStore store) {
        Models.FrequencyState state = null;
        if (store != null) {
            try {
                state = store.loadFrequencyState();
            } catch (Throwable e) {
                state = null;
            }
        }
        if (state == null) state = new Models.FrequencyState();
        return state;
    }

    /*
     * 频率债务只生成独立探针门，不改finalP、不替换正常dynamicGate、不直接BET。
     */
    private void applyQualifiedFrequencyFloorFields(
            Models.ComboCandidate c, GateSignal gate, GateBatchContext ctx) {
        if (c == null) return;
        if (!V51BuildManifest.MODEL_PROVEN || !AppConfig.R35_REAL_AUTO_UNLOCK_ENABLED) {
            c.frequencyDebtScore = 0.0d;
            c.frequencyGateAdjust = 0.0d;
            c.qualifiedProbe = 0;
            c.realProbeAllowed = 0;
            c.frequencySupportScore = 0.0d;
            c.frequencyState = "NOT_APPLICABLE_MODEL_NOT_PROVEN";
            writeString(
                    c,
                    "dynamicGateFrequencyPolicy",
                    "NOT_APPLICABLE_MODEL_NOT_PROVEN;NO_FREQUENCY_DEBT");
            return;
        }
        Models.FrequencyState fs =
                ctx == null || ctx.frequencyState == null
                        ? new Models.FrequencyState()
                        : ctx.frequencyState;
        double riskGate =
                gate == null || gate.finalGate <= 0.0d
                        ? (c.dynamicGate > 0.0d ? c.dynamicGate : AppConfig.DEFAULT_DYNAMIC_GATE)
                        : gate.finalGate;
        double baseGate = gate == null || gate.baseGate <= 0.0d ? riskGate : gate.baseGate;
        double coverage =
                Math.max(
                        readDouble(c, "coverageQuality", 0.0d),
                        readDouble(c, "coverageQualityScore", 0.0d));
        double third =
                Math.max(
                        readDouble(c, "thirdQuality", 0.0d),
                        readDouble(c, "thirdNumberQualityScore", 0.0d));
        double need =
                Math.max(
                        readDouble(c, "needSupplyScore", 0.0d),
                        Math.max(
                                readDouble(c, "needSupplyMatchScore", 0.0d),
                                Math.max(
                                        readDouble(c, "positionNeedScore", 0.0d),
                                        readDouble(c, "numberSupplyScore", 0.0d))));
        double counter =
                Math.max(
                        readDouble(c, "counterRisk", 0.0d),
                        Math.max(
                                readDouble(c, "counterfactualRisk", 0.0d),
                                Math.max(
                                        readDouble(c, "counterEvidenceScore", 0.0d),
                                        readDouble(c, "probabilityCounterEvidence", 0.0d))));
        String promotion =
                readString(
                        c,
                        "shadowPromotionStatus",
                        readString(c, "promotionStatus", AppConfig.PROMOTION_NO_SAMPLE));
        String adaptive =
                readString(c, "adaptiveStatus", readString(c, "adaptiveJudgeStatus", "WATCH"));
        String rule = readString(c, "ruleHealthStatus", readString(c, "ruleStatus", "WATCH"));
        String freshness = readString(c, "snapshotFreshness", "HOT_ONLY");
        boolean hardStale = upperLocal(freshness).indexOf("HARD_STALE") >= 0;
        boolean hardRisk =
                (gate != null && gate.hardBlock == 1)
                        || isTeacherHardRiskForGate(c)
                        || isV20HardRiskForGate(c)
                        || isAdaptiveHardRiskForGate(c)
                        || upperLocal(rule).indexOf("HARD_RISK") >= 0
                        || upperLocal(rule).indexOf("FREEZE") >= 0
                        || readInt(c, "thirdHardRisk", 0) == 1
                        || readDouble(c, "k3CoverageRisk", 0.0d) >= 0.70d
                        || counter > AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK;
        boolean recentLoseRiskHigh = ctx != null && ctx.recentLoseCount >= 4;

        double discount = 0.0d;
        boolean qualityPrecheck =
                fs.isLowFrequencyPressure()
                        && c.finalP >= AppConfig.REAL_BET_PROBE_MIN_P
                        && (c.finalP - riskGate) >= AppConfig.REAL_BET_PROBE_MAX_NEG_EDGE
                        && coverage >= AppConfig.REAL_BET_PROBE_MIN_COVERAGE
                        && third >= AppConfig.REAL_BET_PROBE_MIN_THIRD
                        && need >= AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY
                        && counter <= AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK;
        if (qualityPrecheck) {
            discount =
                    AppConfig.buildFrequencyGateDiscount(
                            fs.frequencyDebt, promotion, hardRisk, hardStale);
        }
        double effectiveProbeGate =
                AppConfig.clampProbability(
                        Math.max(AppConfig.REAL_BET_PROBE_MIN_P, riskGate - discount));
        double rawEdge = c.finalP - riskGate;
        double probeEdge = c.finalP - effectiveProbeGate;
        boolean qualified =
                fs.isLowFrequencyPressure()
                        && discount > 0.0d
                        && AppConfig.isQualifiedFrequencyProbe(
                                c.finalP,
                                riskGate,
                                rawEdge,
                                coverage,
                                third,
                                need,
                                counter,
                                adaptive,
                                promotion,
                                rule,
                                recentLoseRiskHigh,
                                hardStale);

        c.realBetFrequency50 = AppConfig.clampProbability(fs.frequency50);
        c.realBetFrequency100 = AppConfig.clampProbability(fs.frequency100);
        c.realBetFrequency300 = AppConfig.clampProbability(fs.frequency300);
        c.frequencyDebtScore = AppConfig.clampFrequencyDebt(fs.frequencyDebt);
        c.frequencyState = fs.status == null ? AppConfig.FREQ_STATE_NORMAL : fs.status;
        c.frequencyStateVersion = LINK_MODIFICATION_V2_DYNAMIC_GATE_VERSION;
        c.frequencyStateUpdatedAt = fs.updatedAt;
        c.baseGate = AppConfig.clampProbability(baseGate);
        c.riskAdjustedGate = AppConfig.clampProbability(riskGate);
        c.frequencyGateAdjust = -discount;
        c.effectiveProbeGate = effectiveProbeGate;
        c.rawEdge = rawEdge;
        c.effectiveProbeEdge = probeEdge;
        c.coverageQuality = AppConfig.clampScore01(coverage);
        c.thirdQuality = AppConfig.clampScore01(third);
        c.needSupplyScore = AppConfig.clampScore01(need);
        c.counterRisk = AppConfig.clampScore01(counter);
        c.qualifiedProbe = qualified ? 1 : 0;
        c.realProbeAllowed = qualified ? 1 : 0;
        c.probeEligibilityVersion = LINK_MODIFICATION_V2_DYNAMIC_GATE_VERSION;
        c.decisionVersionV2 = LINK_MODIFICATION_V2_DYNAMIC_GATE_VERSION;
        double gapScore =
                1.0d
                        - Math.min(
                                1.0d,
                                Math.max(0.0d, riskGate - c.finalP)
                                        / AppConfig.REAL_BET_PROBE_MAX_GATE_GAP);
        c.qualifiedProbeScore =
                AppConfig.clampScore01(
                        coverage * 0.20d
                                + third * 0.16d
                                + need * 0.20d
                                + (1.0d - counter) * 0.22d
                                + gapScore * 0.22d);
        c.frequencySupportScore =
                AppConfig.clampScore01(
                        (fs.isLowFrequencyPressure()
                                                ? fs.frequencyDebt
                                                        / Math.max(
                                                                0.000001d, AppConfig.FREQ_DEBT_MAX)
                                                : 0.0d)
                                        * 0.40d
                                + c.qualifiedProbeScore * 0.60d);
        c.probeRejectReason =
                qualified
                        ? ""
                        : buildProbeRejectReason(
                                c,
                                fs,
                                hardRisk,
                                hardStale,
                                recentLoseRiskHigh,
                                promotion,
                                adaptive,
                                rule,
                                coverage,
                                third,
                                need,
                                counter,
                                riskGate);
        writeString(c, "dynamicGateVersion", LINK_MODIFICATION_V2_DYNAMIC_GATE_VERSION);
        writeString(
                c,
                "dynamicGateFrequencyPolicy",
                qualified ? "LOW_FREQ_PRESSURE仅开放合格边缘域探针门；正常Gate不降" : "频率不强制BET；当前候选不满足合格探针域");
    }

    private String buildProbeRejectReason(
            Models.ComboCandidate c,
            Models.FrequencyState fs,
            boolean hardRisk,
            boolean hardStale,
            boolean recentLoseRiskHigh,
            String promotion,
            String adaptive,
            String rule,
            double coverage,
            double third,
            double need,
            double counter,
            double riskGate) {
        if (fs == null || !fs.isLowFrequencyPressure()) return "NO_LOW_FREQ_PRESSURE";
        if (hardRisk) return "HARD_RISK";
        if (hardStale) return "SNAPSHOT_HARD_STALE";
        if (recentLoseRiskHigh) return "RECENT_LOSE_RISK_HIGH";
        if (c.finalP < AppConfig.REAL_BET_PROBE_MIN_P) return "FINAL_P_BELOW_PROBE_MIN";
        if (c.finalP - riskGate < AppConfig.REAL_BET_PROBE_MAX_NEG_EDGE)
            return "NEG_EDGE_TOO_LARGE";
        if (riskGate - c.finalP > AppConfig.REAL_BET_PROBE_MAX_GATE_GAP)
            return "GATE_GAP_TOO_LARGE";
        if (coverage < AppConfig.REAL_BET_PROBE_MIN_COVERAGE) return "COVERAGE_WEAK";
        if (third < AppConfig.REAL_BET_PROBE_MIN_THIRD) return "THIRD_WEAK";
        if (need < AppConfig.REAL_BET_PROBE_MIN_NEED_SUPPLY) return "NEED_SUPPLY_WEAK";
        if (counter > AppConfig.REAL_BET_PROBE_MAX_COUNTER_RISK) return "COUNTER_RISK_HIGH";
        String p = upperLocal(promotion);
        boolean explicitProbeEligible =
                readInt(c, "shadowProbeEligible", readInt(c, "probeEligible", 0)) == 1;
        if (!explicitProbeEligible
                && !(p.indexOf("PROMOTE_WATCH") >= 0
                        || p.indexOf("READY") >= 0
                        || p.indexOf("STRONG") >= 0)) return "PROMOTION_NOT_MATURE";
        String a = upperLocal(adaptive);
        if (a.indexOf("FREEZE") >= 0 || a.indexOf("REJECT") >= 0) return "ADAPTIVE_BLOCK";
        String r = upperLocal(rule);
        if (r.indexOf("HARD_RISK") >= 0 || r.indexOf("FREEZE") >= 0) return "RULE_HEALTH_BLOCK";
        return "FREQUENCY_DISCOUNT_NOT_AVAILABLE";
    }

    private GateSignal calculateWithContext(
            GateBatchContext ctx,
            DbStore store,
            Models.ComboCandidate candidate,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CalibrationEngine.CalibrationSignal calibrationSignal) {

        GateSignal signal = new GateSignal();

        if (candidate == null || !candidate.isValidK3()) {
            signal.baseGate = readBaseGate();
            signal.finalGate = signal.baseGate;
            signal.blockCode = AppConfig.BLOCK_DATA_ERROR;
            signal.detail = "动态门槛：候选为空或K3非法，强制HOLD";
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            return signal;
        }

        normalizeCandidate(candidate);

        if (ctx == null) {
            ctx = buildBatchContext(store, AppConfig.BACKTEST_MODE_FAST, REALTIME_GATE_BUDGET_MS);
        }

        signal.position = candidate.position;
        signal.k3Mask = candidate.k3Mask;
        signal.baseGate = initialGate(candidate);
        signal.freqRate100 = ctx.freqRate100;
        signal.freqRate500 = ctx.freqRate500;
        signal.freqRate1000 = ctx.freqRate1000;

        candidate.freq100 = signal.freqRate100;
        candidate.freq500 = signal.freqRate500;
        candidate.freq1000 = signal.freqRate1000;

        CacheGateSignal cacheSignal = evaluateCacheGateSignalFast(candidate);
        TeacherMatrixGateSignal teacherMatrixSignal =
                evaluateTeacherMatrixGateSignalFast(candidate, cacheSignal);
        V20GateEvidence v20GateEvidence = readV20GateEvidenceFast(candidate);

        double qualityGateAdj =
                qualityGateAdjustment(candidate, teacherMatrixSignal, v20GateEvidence, ctx);
        double diagnosisGateAdj = diagnosisGateAdjustment(candidate);
        double moduleEffectGateAdj = moduleEffectGateAdjustment(ctx, candidate);
        double freezeMatrixGateAdj = freezeMatrixGateAdjustment(ctx, candidate);
        double shadowPromotionGateAdj = shadowPromotionGateAdjustment(ctx, candidate);
        double coverageGateAdj = coverageQualityGateAdjustment(candidate);
        double edgeQualityGateAdj =
                edgeVirtualStrongGateAdjustment(candidate, teacherMatrixSignal, v20GateEvidence);
        MainChainGateSignal mainChainSignal =
                evaluateMainChainGateSignal(candidate, teacherMatrixSignal, v20GateEvidence, ctx);
        double mainChainGateAdj = mainChainSignal == null ? 0.0d : mainChainSignal.gateAdjust;

        double stateAdj = stateAdjustment(candidate);
        double riskAdj = riskAdjustment(candidate, teacherMatrixSignal);
        double sampleAdj = sampleAdjustment(candidate, teacherMatrixSignal);
        double freqAdj =
                frequencyAdjustment(
                        signal.freqRate100,
                        signal.freqRate500,
                        signal.freqRate1000,
                        candidate,
                        teacherMatrixSignal);
        double auditAdj = auditAdjustmentFromContext(ctx, candidate);
        double teacherAdj = teacherAdjustmentFromContext(ctx);
        double teacherMatrixAdj = teacherMatrixAdjustment(candidate, teacherMatrixSignal);
        double validationAdj = realtimeValidationAdjustmentFromContext(ctx);
        double profileAdj = profileAdjustment(candidate, cacheSignal);
        double promotionAdj = promotionAdjustment(candidate);
        double aPlusAdj = aPlusAdjustment(candidate);

        double v20CauseAdj = v20CauseChainAdjustment(candidate);
        double v20RuleAdj = v20RuleHealthAdjustment(candidate);
        double v20GuardAdj = v20ThirdGuardAdjustment(candidate);
        double v20ShadowAdj = v20ShadowHealthAdjustment(candidate);
        double v20FreezeAdj = v20FalseStrongFreezeAdjustment(candidate);
        double v20RankAdj = v20CounterfactualAdjustment(candidate);
        double v20SeedAdj = v20SeedAdjustment(candidate);
        double v20TeacherNumberAdj = v20TeacherNumberAdjustment(candidate);
        double v20PositionAdj = v20PositionHealthAdjustment(candidate);

        double trendAdj = 0.0d;
        double recoveryAdj = 0.0d;
        double similarAdj = 0.0d;
        double memoryAdj = 0.0d;
        double calibrationAdj = 0.0d;

        try {
            trendAdj = trendTrace == null ? 0.0d : trendStateMachine.gateAdjustment(trendTrace);
        } catch (Throwable e) {
            trendAdj = 0.0d;
        }

        try {
            recoveryAdj =
                    recoveryResult == null
                            ? 0.0d
                            : breakRecoveryEngine.gateAdjustment(recoveryResult);
        } catch (Throwable e) {
            recoveryAdj = 0.0d;
        }

        try {
            similarAdj =
                    similarResult == null ? 0.0d : similarShapeEngine.gateAdjustment(similarResult);
        } catch (Throwable e) {
            similarAdj = 0.0d;
        }

        try {
            memoryAdj =
                    memorySignal == null ? 0.0d : memoryEngine.memoryGateAdjustment(memorySignal);
        } catch (Throwable e) {
            memoryAdj = 0.0d;
        }

        if (calibrationSignal != null) {
            calibrationAdj = calibrationSignal.gateAdjust;
        }

        stateAdj = clamp(stateAdj, -0.012d, 0.030d);
        riskAdj = clamp(riskAdj, -0.006d, 0.035d);
        sampleAdj = clamp(sampleAdj, -0.008d, 0.012d);
        freqAdj = clamp(freqAdj, -0.006d, 0.026d);
        auditAdj = clamp(auditAdj, 0.000d, 0.010d);
        teacherAdj = clamp(teacherAdj, -0.006d, 0.010d);
        teacherMatrixAdj = clamp(teacherMatrixAdj, -0.008d, 0.014d);
        validationAdj = clamp(validationAdj, -0.004d, 0.008d);
        profileAdj = clamp(profileAdj, -0.008d, 0.010d);
        promotionAdj = clamp(promotionAdj, -0.004d, 0.006d);
        aPlusAdj = clamp(aPlusAdj, -0.004d, 0.005d);

        v20CauseAdj = clamp(v20CauseAdj, -0.006d, 0.010d);
        v20RuleAdj = clamp(v20RuleAdj, -0.004d, 0.010d);
        v20GuardAdj = clamp(v20GuardAdj, -0.004d, 0.012d);
        v20ShadowAdj = clamp(v20ShadowAdj, -0.004d, 0.010d);
        v20FreezeAdj = clamp(v20FreezeAdj, 0.000d, 0.018d);
        v20RankAdj = clamp(v20RankAdj, -0.004d, 0.010d);
        v20SeedAdj = clamp(v20SeedAdj, -0.003d, 0.006d);
        v20TeacherNumberAdj = clamp(v20TeacherNumberAdj, -0.005d, 0.008d);
        v20PositionAdj = clamp(v20PositionAdj, -0.004d, 0.008d);

        trendAdj = clamp(trendAdj, -0.010d, 0.014d);
        recoveryAdj = clamp(recoveryAdj, -0.010d, 0.014d);
        similarAdj = clamp(similarAdj, -0.008d, 0.010d);
        memoryAdj = clamp(memoryAdj, -0.008d, 0.010d);
        calibrationAdj = clamp(calibrationAdj, -0.006d, 0.010d);

        qualityGateAdj = clamp(qualityGateAdj, QUALITY_GATE_MAX_RELAX, QUALITY_GATE_MAX_BOOST);
        diagnosisGateAdj = clamp(diagnosisGateAdj, -0.004d, DIAG_GATE_MAX_BOOST);
        moduleEffectGateAdj = clamp(moduleEffectGateAdj, -0.003d, MODULE_EFFECT_GATE_MAX_BOOST);
        freezeMatrixGateAdj = clamp(freezeMatrixGateAdj, 0.000d, FREEZE_GATE_MAX_BOOST);
        shadowPromotionGateAdj =
                clamp(shadowPromotionGateAdj, SHADOW_PROMOTION_GATE_MAX_RELAX, 0.000d);
        mainChainGateAdj =
                clamp(mainChainGateAdj, MAIN_CHAIN_GATE_MAX_RELAX, MAIN_CHAIN_GATE_MAX_BOOST);
        signal.stateAdjust = stateAdj;
        signal.riskAdjust = riskAdj;
        signal.sampleAdjust = sampleAdj;
        signal.frequencyAdjust = freqAdj;
        signal.trendAdjust = trendAdj;
        signal.recoveryAdjust = recoveryAdj;
        signal.similarAdjust = similarAdj;
        signal.memoryAdjust = memoryAdj;
        signal.calibrationAdjust = calibrationAdj;
        signal.auditAdjust = auditAdj;
        signal.teacherAdjust = teacherAdj;
        signal.teacherMatrixAdjust = teacherMatrixAdj;
        signal.validationAdjust = validationAdj;
        signal.profileAdjust = profileAdj;
        signal.promotionAdjust = promotionAdj;
        signal.aPlusAdjust = aPlusAdj;

        signal.qualityGateAdjust = qualityGateAdj;
        signal.diagnosisGateAdjust = diagnosisGateAdj;
        signal.moduleEffectGateAdjust = moduleEffectGateAdj;
        signal.freezeGateAdjust = freezeMatrixGateAdj;
        signal.shadowPromotionGateAdjust = shadowPromotionGateAdj;
        signal.coverageGateAdjust = coverageGateAdj;
        signal.edgeQualityGateAdjust = edgeQualityGateAdj;
        signal.mainChainGateAdjust = mainChainGateAdj;
        if (mainChainSignal != null) {
            signal.mainChainEvidenceScore = mainChainSignal.evidenceScore;
            signal.mainChainCounterScore = mainChainSignal.counterScore;
            signal.mainChainNetScore = mainChainSignal.netScore;
            signal.mainChainRejectCode = mainChainSignal.rejectCode;
            signal.mainChainActionAdvice = mainChainSignal.actionAdvice;
            signal.mainChainSummary = mainChainSignal.summary;
        }
        signal.v20CauseAdjust = v20CauseAdj;
        signal.v20RuleAdjust = v20RuleAdj;
        signal.v20GuardAdjust = v20GuardAdj;
        signal.v20ShadowAdjust = v20ShadowAdj;
        signal.v20FreezeAdjust = v20FreezeAdj;
        signal.v20RankAdjust = v20RankAdj;
        signal.v20SeedAdjust = v20SeedAdj;
        signal.v20TeacherNumberAdjust = v20TeacherNumberAdj;
        signal.v20PositionAdjust = v20PositionAdj;

        signal.cacheSample = cacheSignal.sample;
        signal.cacheHitRate = cacheSignal.hitRate;
        signal.cacheRisk = cacheSignal.risk;
        signal.cacheDetail = cacheSignal.summary;

        signal.teacherMatrixSample = teacherMatrixSignal.sample;
        signal.teacherMatrixComboSample = teacherMatrixSignal.comboSample;
        signal.teacherMatrixHitRate = teacherMatrixSignal.hitRate;
        signal.teacherMatrixComboHitRate = teacherMatrixSignal.comboHitRate;
        signal.teacherMatrixConfidence = teacherMatrixSignal.confidence;
        signal.teacherMatrixRisk = teacherMatrixSignal.riskPenalty;
        signal.teacherMatrixStatus = teacherMatrixSignal.status;
        signal.teacherMatrixDetail = teacherMatrixSignal.summary;

        signal.strengthScore =
                strengthScore(
                        candidate,
                        trendTrace,
                        recoveryResult,
                        similarResult,
                        memorySignal,
                        cacheSignal,
                        teacherMatrixSignal);

        signal.riskScore =
                riskScore(
                        candidate,
                        trendTrace,
                        recoveryResult,
                        calibrationSignal,
                        cacheSignal,
                        teacherMatrixSignal);

        signal.sampleScore =
                sampleScore(
                        candidate, similarResult, memorySignal, cacheSignal, teacherMatrixSignal);

        double gate = signal.baseGate;
        gate += stateAdj;
        gate += riskAdj;
        gate += sampleAdj;
        gate += freqAdj;
        gate += trendAdj;
        gate += recoveryAdj;
        gate += similarAdj;
        gate += memoryAdj;
        gate += calibrationAdj;
        gate += auditAdj;
        gate += teacherAdj;
        gate += teacherMatrixAdj;
        gate += validationAdj;
        gate += profileAdj;
        gate += promotionAdj;
        gate += aPlusAdj;

        gate += qualityGateAdj;
        gate += diagnosisGateAdj;
        gate += moduleEffectGateAdj;
        gate += freezeMatrixGateAdj;
        gate += shadowPromotionGateAdj;
        gate += coverageGateAdj;
        gate += edgeQualityGateAdj;
        gate += mainChainGateAdj;
        gate += v20CauseAdj;
        gate += v20RuleAdj;
        gate += v20GuardAdj;
        gate += v20ShadowAdj;
        gate += v20FreezeAdj;
        gate += v20RankAdj;
        gate += v20SeedAdj;
        gate += v20TeacherNumberAdj;
        gate += v20PositionAdj;

        gate = applyOriginalMainlineCap(gate, candidate, signal, teacherMatrixSignal);
        gate = applyHardFloorAndCeiling(gate, candidate, signal, teacherMatrixSignal);

        signal.finalGate = gate;
        signal.edge = candidate.finalP - signal.finalGate;
        signal.allowedByGate = signal.edge >= EDGE_SOFT_MIN ? 1 : 0;

        applySoftEdgeRules(signal, candidate, teacherMatrixSignal);
        applyV20SoftRules(signal, candidate);
        applyHardBlocks(
                signal,
                candidate,
                trendTrace,
                recoveryResult,
                similarResult,
                memorySignal,
                calibrationSignal,
                cacheSignal,
                teacherMatrixSignal,
                ctx);
        applyV20HardBlocks(signal, candidate);

        if (signal.hardBlock == 1) {
            signal.allowedByGate = 0;
        }

        if (signal.allowedByGate == 0
                && (signal.blockCode == null || signal.blockCode.length() == 0)) {
            signal.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
        }

        applyTeacherMatrixToCandidate(candidate, teacherMatrixSignal);

        signal.qualitySummary = buildQualityGateSummary(signal, candidate, ctx);
        signal.frequencyPolicy = buildFrequencyPolicyText(signal, candidate);
        signal.actionAdvice = buildQualityActionAdvice(signal, candidate);
        signal.detail = buildDetail(signal, candidate, ctx);

        return signal;
    }

    private GateBatchContext buildBatchContext(DbStore store, String mode, long budgetMs) {
        GateBatchContext ctx = new GateBatchContext();
        ctx.mode = AppConfig.normalizeBacktestMode(mode);
        ctx.startedAt = System.currentTimeMillis();
        ctx.budgetMs = budgetMs <= 0L ? REALTIME_GATE_BUDGET_MS : budgetMs;

        ctx.frequencyState = loadFrequencyStateSafe(store);
        ctx.freqRate50 = ctx.frequencyState.frequency50;
        ctx.freqRate100 = ctx.frequencyState.frequency100;
        ctx.freqRate300 = ctx.frequencyState.frequency300;
        /* 保留旧字段别名供旧UI/旧诊断读取。 */
        ctx.freqRate500 = ctx.freqRate50;
        ctx.freqRate1000 = ctx.freqRate300;
        if (store != null) {
            try {
                ctx.probeFrequency20 = store.getRecentProbeFrequency(AppConfig.PROBE_RECENT_WINDOW);
            } catch (Throwable e) {
                ctx.probeFrequency20 = 0.0d;
            }
            try {
                ctx.probeFrequency100 =
                        store.getRecentProbeFrequency(AppConfig.FREQ_LOOKBACK_ISSUES);
            } catch (Throwable e) {
                ctx.probeFrequency100 = 0.0d;
            }
            try {
                ctx.recentLoseCount = store.getRecentRealLoseCount(6);
            } catch (Throwable e) {
                ctx.recentLoseCount = 0;
            }
        }

        if (store == null) {
            return ctx;
        }

        try {
            ctx.auditStatus = store.getState("probability_audit_status", "");
        } catch (Throwable e) {
            ctx.auditStatus = "";
        }

        try {
            Object v =
                    invokeStore(
                            store, "getLatestTeacherGateAdjust", new Class[] {}, new Object[] {});
            if (v instanceof Number) {
                ctx.teacherAdjust = ((Number) v).doubleValue();
            } else {
                ctx.teacherAdjust = 0.0d;
            }
            ctx.teacherAdjust = clamp(ctx.teacherAdjust, -0.006d, 0.010d);
        } catch (Throwable e) {
            ctx.teacherAdjust = 0.0d;
        }

        try {
            Object obj =
                    invokeStore(
                            store,
                            "getLatestRealtimeValidationReport",
                            new Class[] {},
                            new Object[] {});

            if (obj instanceof Models.BacktestReport) {
                Models.BacktestReport r = (Models.BacktestReport) obj;
                r.recalc();
                ctx.validationBetCount = r.betCount;
                ctx.validationFrequency = r.frequency;
                ctx.validationHitRate = r.hitRate;
                ctx.validationPnl = r.pnl;
                ctx.validationMaxDrawdown = r.maxDrawdown;
                ctx.validationTeacherSample = readInt(r, "teacherMatrixSample", 0);
                ctx.validationTeacherHitRate = readDouble(r, "teacherMatrixHitRate", 0.0d);
            }
        } catch (Throwable e) {
        }

        return ctx;
    }

    private void saveDynamicGateRuntimeState(
            DbStore store, GateBatchContext ctx, List<Models.ComboCandidate> out) {
        if (store == null || ctx == null) {
            return;
        }

        try {
            long cost = System.currentTimeMillis() - ctx.startedAt;

            store.setState("dynamic_gate_status", "OK");
            store.setState("dynamic_gate_cost_ms", String.valueOf(cost));
            store.setState("dynamic_gate_input_count", String.valueOf(ctx.inputCount));
            store.setState(
                    "dynamic_gate_output_count", String.valueOf(out == null ? 0 : out.size()));
            store.setState("dynamic_gate_mode", ctx.mode == null ? "" : ctx.mode);
            store.setState("dynamic_gate_budget_ms", String.valueOf(ctx.budgetMs));
            store.setState("dynamic_gate_speed_fix", "V20_QUALITY_RISK_DYNAMIC_GATE_13OF14");
            store.setState("dynamic_gate_upstream_version", V20_DYNAMIC_GATE_UPSTREAM_VERSION);
            store.setState(
                    "dynamic_gate_summary",
                    "V20动态门槛：保留老版频率动态门与教师矩阵软调门；新增动态因链、规则健康、第三防线、影子健康、伪强冻结、反事实排名、教师号码、位置健康、种子扩展等软证据；不创建BET，不结算，不写真实校准，最终仍由DecisionGateEngine裁决；耗时="
                            + cost
                            + "ms，输入="
                            + ctx.inputCount
                            + "，输出="
                            + (out == null ? 0 : out.size()));
            store.setState("dynamic_gate_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
        }
    }

    public boolean gateAllowsBet(GateSignal signal) {
        if (signal == null) {
            return false;
        }

        if (signal.hardBlock == 1) {
            return false;
        }

        if (signal.allowedByGate == 0) {
            return false;
        }

        if (signal.edge < EDGE_SOFT_MIN) {
            return false;
        }

        if (signal.finalGate < readMinGate()) {
            return false;
        }

        return true;
    }

    public boolean candidateAllowsBet(DbStore store, Models.ComboCandidate candidate) {
        GateSignal signal = calculateSimple(store, candidate);
        return gateAllowsBet(signal);
    }

    public double calculateFinalGateOnly(DbStore store, Models.ComboCandidate candidate) {
        GateSignal signal = calculateSimple(store, candidate);
        return signal.finalGate;
    }

    public double recentBetFrequency(DbStore store, int window) {
        if (store == null) {
            return FREQ_NEUTRAL_WHEN_UNKNOWN;
        }

        if (window <= 0) {
            window = AppConfig.FREQ_WINDOW_SHORT;
        }

        long now = System.currentTimeMillis();

        if (window == AppConfig.FREQ_WINDOW_SHORT && now - freqCacheAt100 <= FREQ_CACHE_TTL_MS) {
            return freqCache100;
        }

        if (window == AppConfig.FREQ_WINDOW_MID && now - freqCacheAt500 <= FREQ_CACHE_TTL_MS) {
            return freqCache500;
        }

        if (window == AppConfig.FREQ_WINDOW_LONG && now - freqCacheAt1000 <= FREQ_CACHE_TTL_MS) {
            return freqCache1000;
        }

        double real = readRealFrequencyFromRuntimeStats(store, window);

        if (real >= 0.0d) {
            real = AppConfig.clampProbability(real);
            saveFreqCache(window, now, real);
            return real;
        }

        double fallback = fallbackDecisionFrequency(store, window);
        fallback = AppConfig.clampProbability(fallback);
        saveFreqCache(window, now, fallback);
        return fallback;
    }

    private double readRealFrequencyFromRuntimeStats(DbStore store, int window) {
        if (store == null) {
            return -1.0d;
        }

        try {
            Object stats = invokeStore(store, "getRuntimeStats", new Class[] {}, new Object[] {});

            if (stats == null) {
                return -1.0d;
            }

            if (window == AppConfig.FREQ_WINDOW_SHORT) {
                return readDouble(stats, "freq100", readDouble(stats, "realFreq100", -1.0d));
            }

            if (window == AppConfig.FREQ_WINDOW_MID) {
                return readDouble(stats, "freq500", readDouble(stats, "realFreq500", -1.0d));
            }

            if (window == AppConfig.FREQ_WINDOW_LONG) {
                return readDouble(stats, "freq1000", readDouble(stats, "realFreq1000", -1.0d));
            }

            return readDouble(stats, "frequency", readDouble(stats, "realFrequency", -1.0d));
        } catch (Throwable e) {
            return -1.0d;
        }
    }

    private double fallbackDecisionFrequency(DbStore store, int window) {
        List<Models.Decision> list = null;

        try {
            int limit = window * 4;

            if (limit > 1200) {
                limit = 1200;
            }

            if (limit < 100) {
                limit = 100;
            }

            Object obj =
                    invokeStore(
                            store,
                            "getRecentDecisions",
                            new Class[] {Integer.TYPE},
                            new Object[] {Integer.valueOf(limit)});

            if (obj instanceof List) {
                list = (List<Models.Decision>) obj;
            }
        } catch (Throwable e) {
            list = null;
        }

        if (list == null || list.size() == 0) {
            return FREQ_NEUTRAL_WHEN_UNKNOWN;
        }

        ArrayList<String> totalIssues = new ArrayList<String>();
        ArrayList<String> betIssues = new ArrayList<String>();

        for (int i = 0; i < list.size(); i++) {
            Models.Decision d = list.get(i);

            if (d == null || d.targetIssue == null || d.targetIssue.length() == 0) {
                continue;
            }

            String decisionText = normalizeDecisionText(d.decision, d.action);

            if (isWeakPlaceholderDecision(d)) {
                continue;
            }

            if (AppConfig.DECISION_SHADOW.equals(decisionText)) {
                if (!totalIssues.contains(d.targetIssue)) {
                    totalIssues.add(d.targetIssue);
                }

                if (totalIssues.size() >= window) {
                    break;
                }

                continue;
            }

            if (!totalIssues.contains(d.targetIssue)) {
                totalIssues.add(d.targetIssue);
            }

            if (AppConfig.DECISION_BET.equals(decisionText)
                    && isValidRealBetDecision(d)
                    && !betIssues.contains(d.targetIssue)) {
                betIssues.add(d.targetIssue);
            }

            if (totalIssues.size() >= window) {
                break;
            }
        }

        if (totalIssues.size() <= 0) {
            return FREQ_NEUTRAL_WHEN_UNKNOWN;
        }

        return ((double) betIssues.size()) / ((double) totalIssues.size());
    }

    private boolean isValidRealBetDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        String decisionText = normalizeDecisionText(d.decision, d.action);

        if (!AppConfig.DECISION_BET.equals(decisionText)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        repairDecisionK3(d);

        if (!AppConfig.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        String scope = d.statScope == null ? "" : d.statScope;

        if (scope.length() > 0 && !AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope)) {
            return false;
        }

        return true;
    }

    private void saveFreqCache(int window, long now, double value) {
        value = AppConfig.clampProbability(value);

        if (window == AppConfig.FREQ_WINDOW_SHORT) {
            freqCacheAt100 = now;
            freqCache100 = value;
            return;
        }

        if (window == AppConfig.FREQ_WINDOW_MID) {
            freqCacheAt500 = now;
            freqCache500 = value;
            return;
        }

        if (window == AppConfig.FREQ_WINDOW_LONG) {
            freqCacheAt1000 = now;
            freqCache1000 = value;
        }
    }

    public String explain(GateSignal signal) {
        if (signal == null) {
            return "动态门槛为空";
        }

        return signal.detail == null ? "" : signal.detail;
    }

    private double initialGate(Models.ComboCandidate c) {
        double gate = readBaseGate();

        if (c == null) {
            return gate;
        }

        if (c.dynamicGate > 0.0d
                && c.dynamicGate >= readMinGate()
                && c.dynamicGate <= STALE_GATE_HIGH) {
            double incoming = c.dynamicGate;

            if (incoming <= ORIGINAL_NORMAL_CAP) {
                gate = (gate * 0.65d) + (incoming * 0.35d);
            }
        }

        if (gate < readMinGate()) {
            gate = readMinGate();
        }

        if (gate > ORIGINAL_NORMAL_CAP) {
            gate = ORIGINAL_BASE_GATE;
        }

        return AppConfig.clampProbability(gate);
    }

    private double stateAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.010d;
        }

        String mode = c.stateMode == null ? AppConfig.MODE_CHAOS : c.stateMode;

        if (AppConfig.MODE_ACCELERATING.equals(mode)) {
            if (lowRisk(c)) {
                return -0.010d;
            }
            return -0.002d;
        }

        if (AppConfig.MODE_CONFIRMED.equals(mode)) {
            if (c.trendLife >= trendLifeMin() && lowRisk(c)) {
                return -0.008d;
            }
            return -0.002d;
        }

        if (AppConfig.MODE_RECOVERING.equals(mode)) {
            if (c.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG && lowRisk(c)) {
                return -0.006d;
            }

            if (c.recoveryScore < AppConfig.RECOVERY_SCORE_MIN) {
                return 0.004d;
            }

            return -0.001d;
        }

        if (AppConfig.MODE_FORMING.equals(mode)) {
            if (lowRisk(c) && c.finalP >= 0.390d) {
                return -0.001d;
            }
            return 0.002d;
        }

        if (AppConfig.MODE_PULLBACK.equals(mode)) {
            return 0.004d;
        }

        if (AppConfig.MODE_WEAKENING.equals(mode)) {
            return 0.010d;
        }

        if (AppConfig.MODE_BROKEN.equals(mode)) {
            return 0.028d;
        }

        if (AppConfig.MODE_REVERSING.equals(mode)) {
            return 0.030d;
        }

        if ("REVERSAL".equals(mode)) {
            return 0.030d;
        }

        if ("STABLE".equals(mode)) {
            if (lowRisk(c)) {
                return -0.008d;
            }
            return -0.003d;
        }

        if ("TRANSITION".equals(mode)) {
            return 0.004d;
        }

        if (AppConfig.MODE_CHAOS.equals(mode)) {
            return 0.006d;
        }

        return 0.002d;
    }

    private double riskAdjustment(
            Models.ComboCandidate c, TeacherMatrixGateSignal teacherMatrixSignal) {
        if (c == null) {
            return 0.020d;
        }

        double adj = 0.0d;

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            adj += (c.driftRisk - AppConfig.DRIFT_RISK_MAX) / 100.0d * 0.045d;
        } else if (c.driftRisk <= 30.0d) {
            adj -= 0.002d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            adj += (c.breakDepth - AppConfig.BREAK_DEPTH_MAX) / 100.0d * 0.045d;
        } else if (c.breakDepth <= 30.0d) {
            adj -= 0.002d;
        }

        if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            adj += (c.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) / 100.0d * 0.030d;
        }

        if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            adj += (c.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) / 100.0d * 0.030d;
        }

        if (c.evidenceRisk >= 0.700d) {
            adj += 0.006d;
        } else if (c.evidenceRisk >= 0.660d) {
            adj += 0.003d;
        } else if (c.evidenceRisk > 0.0d && c.evidenceRisk <= 0.520d && lowRisk(c)) {
            adj -= 0.002d;
        }

        if (c.weakestNumberScore > 0.0d && c.weakestNumberScore < 0.260d) {
            adj += 0.003d;
        }

        if (c.balanceScore > 0.0d && c.balanceScore < 0.240d) {
            adj += 0.002d;
        }

        if (c.championGap < AppConfig.CHAMPION_GAP_MIN && c.finalP < 0.400d) {
            adj += 0.002d;
        }

        if (c.comboGap < AppConfig.COMBO_GAP_MIN && c.finalP < 0.400d) {
            adj += 0.002d;
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if ("RISK".equals(teacherMatrixSignal.status)) {
                adj += 0.006d;
            }

            if (teacherMatrixSignal.statusStrong()
                    && teacherMatrixSignal.riskPenalty <= 0.006d
                    && lowRisk(c)) {
                adj -= 0.002d;
            }

            if (teacherMatrixSignal.riskPenalty > 0.010d) {
                adj += Math.min(0.005d, teacherMatrixSignal.riskPenalty * 0.40d);
            }
        }

        return clamp(adj, -0.006d, 0.035d);
    }

    private double sampleAdjustment(
            Models.ComboCandidate c, TeacherMatrixGateSignal teacherMatrixSignal) {
        if (c == null) {
            return 0.008d;
        }

        double adj = 0.0d;

        if (c.effectiveSample <= 0) {
            adj += 0.008d;
        } else if (c.effectiveSample < 80) {
            adj += 0.006d;
        } else if (c.effectiveSample < 120) {
            adj += 0.003d;
        } else if (c.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && c.similarHitRate >= 0.370d) {
            adj -= 0.004d;
        } else if (c.effectiveSample >= 180 && c.similarHitRate >= 0.350d) {
            adj -= 0.002d;
        }

        if (c.comboSample <= 0) {
            adj += 0.005d;
        } else if (c.comboSample < 70) {
            adj += 0.004d;
        } else if (c.comboSample < 100) {
            adj += 0.002d;
        } else if (c.comboSample >= AppConfig.COMBO_SAMPLE_HIGH && c.comboHitRate >= 0.370d) {
            adj -= 0.004d;
        } else if (c.comboSample >= 160 && c.comboHitRate >= 0.350d) {
            adj -= 0.002d;
        }

        if (c.similarHitRate > 0.0d && c.similarHitRate < 0.300d && c.effectiveSample >= 160) {
            adj += 0.003d;
        }

        if (c.comboHitRate > 0.0d && c.comboHitRate < 0.300d && c.comboSample >= 160) {
            adj += 0.003d;
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if (teacherMatrixSignal.sample < TEACHER_MATRIX_MIN_SAMPLE
                    && teacherMatrixSignal.comboSample < TEACHER_MATRIX_COMBO_MIN_SAMPLE
                    && c.finalP < 0.390d) {
                adj += 0.002d;
            }

            if (teacherMatrixSignal.statusStrong()
                    && teacherMatrixSignal.sample >= TEACHER_MATRIX_MIN_SAMPLE
                    && teacherMatrixSignal.hitRate >= 0.370d
                    && teacherMatrixSignal.confidence >= 0.360d) {
                adj -= 0.002d;
            }

            if (teacherMatrixSignal.comboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                    && teacherMatrixSignal.comboHitRate >= 0.370d) {
                adj -= 0.002d;
            }
        }

        return clamp(adj, -0.008d, 0.012d);
    }

    private double frequencyAdjustment(
            double freq100,
            double freq500,
            double freq1000,
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal) {
        if (c == null || !c.isValidK3()) {
            return 0.0d;
        }
        if (Double.isNaN(freq100) || Double.isInfinite(freq100)) {
            freq100 = FREQ_NEUTRAL_WHEN_UNKNOWN;
        }
        if (Double.isNaN(freq500) || Double.isInfinite(freq500)) {
            freq500 = FREQ_NEUTRAL_WHEN_UNKNOWN;
        }
        if (Double.isNaN(freq1000) || Double.isInfinite(freq1000)) {
            freq1000 = FREQ_NEUTRAL_WHEN_UNKNOWN;
        }
        freq100 = AppConfig.clampProbability(freq100);
        freq500 = AppConfig.clampProbability(freq500);
        freq1000 = AppConfig.clampProbability(freq1000);
        double maxFreq = Math.max(freq100, Math.max(freq500, freq1000));

        boolean teacherStrong =
                teacherMatrixSignal != null
                        && teacherMatrixSignal.statusStrong()
                        && teacherMatrixSignal.hitRate >= 0.370d
                        && teacherMatrixSignal.confidence >= 0.360d
                        && teacherMatrixSignal.riskPenalty <= 0.010d;

        boolean v20Strong =
                readDouble(c, "dynamicCauseScore", readDouble(c, "v20CauseScore", 0.0d)) >= 0.620d
                        && readDouble(c, "dynamicRiskScore", readDouble(c, "v20RiskScore", 0.0d))
                                <= 0.520d
                        && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.560d
                        && readInt(c, "dynamicHardRisk", 0) == 0
                        && readInt(c, "falseStrongHardFreeze", 0) == 0;

        boolean probabilityDiagStrong =
                readDouble(c, "probabilityEvidenceContribution", 0.0d) >= QUALITY_EVIDENCE_STRONG
                        && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.560d
                        && readDouble(c, "probabilitySampleReliability", 0.0d) >= QUALITY_SAMPLE_MIN
                        && readDouble(c, "probabilityRecentValidity", 0.0d) >= QUALITY_RECENT_MIN;

        boolean preferredEdgeDomain =
                c.finalP >= PREFERRED_P_MIN
                        && c.finalP <= PREFERRED_P_MAX
                        && readDouble(c, "edge", c.edge) >= PREFERRED_EDGE_MIN
                        && readDouble(c, "edge", c.edge) <= PREFERRED_EDGE_MAX
                        && readDouble(c, "k3CoverageRisk", 0.0d) < 0.50d;
        boolean strongQuality =
                c.finalP >= 0.395d
                        && c.rawP >= 0.345d
                        && lowRisk(c)
                        && readInt(c, "falseHighPFlag", 0) == 0
                        && (preferredEdgeDomain
                                || c.trendLife >= trendLifeMin()
                                || c.effectiveSample >= 160
                                || c.comboSample >= 140
                                || c.profileScore >= 0.620d
                                || c.aPlusScore >= 0.620d
                                || teacherStrong
                                || v20Strong
                                || probabilityDiagStrong);

        boolean fakeStrongCluster =
                readInt(c, "teacherMatrixFreezeFlag", 0) == 1
                        || "FREEZE".equals(readString(c, "teacherMatrixStatus", ""))
                        || "RISK".equals(readString(c, "teacherMatrixStatus", ""))
                        || readDouble(c, "probabilityCounterEvidence", 0.0d) >= QUALITY_COUNTER_HIGH
                        || readDouble(c, "freezeAdjustedP", 0.0d) < -0.006d
                        || readDouble(
                                        c,
                                        "falseStrongFreezeRisk",
                                        readDouble(c, "falseStrongRisk", 0.0d))
                                >= 0.620d;

        if (maxFreq >= AppConfig.TARGET_FREQ_HARD_MAX) {
            if (strongQuality && !fakeStrongCluster) {
                return 0.0d;
            }
            return FREQ_OVERHEAT_GATE_BOOST;
        }
        if (maxFreq >= AppConfig.SMART_FREQ_OVERHEAT) {
            if (strongQuality && !fakeStrongCluster) {
                return 0.0d;
            }
            return fakeStrongCluster ? FREQ_OVERHEAT_GATE_BOOST : FREQ_HIGH_GATE_BOOST;
        }
        if (freq100 >= AppConfig.TARGET_FREQ_MAX || freq500 >= AppConfig.TARGET_FREQ_MAX) {
            if (strongQuality && !fakeStrongCluster) {
                return 0.0d;
            }
            return fakeStrongCluster ? FREQ_HIGH_WEAK_GATE_BOOST : FREQ_WARN_GATE_BOOST;
        }
        if (freq100 >= AppConfig.TARGET_FREQ_IDEAL_HIGH
                || freq500 >= AppConfig.TARGET_FREQ_IDEAL_HIGH) {
            if (!strongQuality || fakeStrongCluster) {
                return FREQ_WARN_GATE_BOOST;
            }
            return 0.0d;
        }

        if (!strongQuality || fakeStrongCluster) {
            return 0.0d;
        }
        /* 链路修改升级V2：低频不再修改正常Gate，只生成独立effectiveProbeGate。 */
        return 0.0d;
    }

    private double qualityGateAdjustment(
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal,
            V20GateEvidence evidence,
            GateBatchContext ctx) {
        if (c == null) {
            return 0.012d;
        }

        double ev =
                readDouble(
                        c,
                        "probabilityEvidenceContribution",
                        evidence == null ? 0.0d : evidence.evidenceContribution);
        double counter =
                readDouble(
                        c,
                        "probabilityCounterEvidence",
                        evidence == null ? 0.0d : evidence.counterContribution);
        double sampleReliability =
                readDouble(
                        c,
                        "probabilitySampleReliability",
                        evidence == null ? 0.0d : evidence.sampleReliability);
        double recentValidity =
                readDouble(
                        c,
                        "probabilityRecentValidity",
                        evidence == null ? 0.0d : evidence.recentValidity);
        double adj = 0.0d;

        if (ev >= QUALITY_EVIDENCE_STRONG
                && counter <= 0.520d
                && sampleReliability >= QUALITY_SAMPLE_MIN
                && recentValidity >= QUALITY_RECENT_MIN
                && lowRisk(c)) {
            adj -= 0.006d;
        }

        if (counter >= QUALITY_COUNTER_HIGH) {
            adj += 0.010d;
        } else if (counter >= 0.580d && ev < counter) {
            adj += 0.006d;
        }

        if (sampleReliability > 0.0d
                && sampleReliability < QUALITY_SAMPLE_MIN
                && c.finalP >= 0.390d) {
            adj += 0.004d;
        }
        if (recentValidity > 0.0d && recentValidity < QUALITY_RECENT_MIN && c.finalP >= 0.390d) {
            adj += 0.005d;
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if ("FREEZE".equals(teacherMatrixSignal.status)) {
                adj += 0.016d;
            } else if ("RISK".equals(teacherMatrixSignal.status)) {
                adj += 0.010d;
            } else if (teacherMatrixSignal.statusStrong()
                    && teacherMatrixSignal.riskPenalty <= 0.010d
                    && counter <= 0.520d
                    && lowRisk(c)) {
                adj -= 0.003d;
            }
        }

        if (ctx != null) {
            if (containsRiskWord(ctx.adaptiveJudgeStatus)
                    || containsRiskWord(ctx.adaptiveJudgeSummary)) {
                adj += 0.006d;
            }
            if (containsPassWord(ctx.adaptiveJudgeStatus)
                    || containsPassWord(ctx.adaptiveJudgeSummary)) {
                if (counter <= 0.540d && lowRisk(c)) {
                    adj -= 0.002d;
                }
            }
        }

        return clamp(adj, QUALITY_GATE_MAX_RELAX, QUALITY_GATE_MAX_BOOST);
    }

    private double diagnosisGateAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }
        double discount = readDouble(c, "probabilityDiagnosisDiscount", 0.0d);
        double boost = readDouble(c, "probabilityDiagnosisBoost", 0.0d);
        double freezeAdjusted = readDouble(c, "freezeAdjustedP", 0.0d);
        double moduleAdjusted = readDouble(c, "moduleAdjustedP", 0.0d);
        double counter = readDouble(c, "probabilityCounterEvidence", 0.0d);
        double recent = readDouble(c, "probabilityRecentValidity", 0.0d);
        double sample = readDouble(c, "probabilitySampleReliability", 0.0d);
        double adj = 0.0d;
        adj += clamp(discount * 0.55d, 0.0d, DIAG_GATE_MAX_BOOST);
        adj -= clamp(boost * 0.35d, 0.0d, 0.004d);
        if (freezeAdjusted < 0.0d) {
            adj += clamp(-freezeAdjusted * 0.55d, 0.0d, FREEZE_GATE_MAX_BOOST);
        }
        if (moduleAdjusted < 0.0d) {
            adj += clamp(-moduleAdjusted * 0.45d, 0.0d, MODULE_EFFECT_GATE_MAX_BOOST);
        }
        if (counter >= 0.720d && c.finalP < 0.430d) {
            adj += 0.006d;
        }
        if (sample > 0.0d
                && sample < QUALITY_SAMPLE_MIN
                && recent > 0.0d
                && recent < QUALITY_RECENT_MIN) {
            adj += 0.004d;
        }
        return clamp(adj, -0.004d, DIAG_GATE_MAX_BOOST);
    }

    private double moduleEffectGateAdjustment(GateBatchContext ctx, Models.ComboCandidate c) {
        if (ctx == null) {
            return 0.0d;
        }
        double adj = 0.0d;
        if (containsMisleading(ctx.moduleEffectStatus)
                || containsMisleading(ctx.moduleEffectSummary)) {
            adj += MODULE_EFFECT_GATE_MAX_BOOST;
        } else if (containsEffective(ctx.moduleEffectStatus)
                || containsEffective(ctx.moduleEffectSummary)) {
            if (c != null
                    && lowRisk(c)
                    && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.540d) {
                adj -= 0.002d;
            }
        }
        return clamp(adj, -0.003d, MODULE_EFFECT_GATE_MAX_BOOST);
    }

    private double freezeMatrixGateAdjustment(GateBatchContext ctx, Models.ComboCandidate c) {
        double adj = 0.0d;
        if (c != null) {
            double freezeRisk =
                    readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d));
            if (readInt(c, "falseStrongFreeze", 0) == 1
                    || readInt(c, "teacherMatrixFreezeFlag", 0) == 1) {
                adj += 0.016d;
            }
            if (freezeRisk >= 0.700d) {
                adj += 0.014d;
            } else if (freezeRisk >= 0.560d) {
                adj += 0.007d;
            }
        }
        if (ctx != null
                && (containsRiskWord(ctx.dynamicFreezeStatus)
                        || containsRiskWord(ctx.dynamicFreezeSummary))) {
            adj += 0.010d;
        }
        return clamp(adj, 0.0d, FREEZE_GATE_MAX_BOOST);
    }

    private double shadowPromotionGateAdjustment(GateBatchContext ctx, Models.ComboCandidate c) {
        if (ctx == null) {
            return 0.0d;
        }
        if (containsPromotion(ctx.shadowPromotionStatus)
                || containsPromotion(ctx.shadowPromotionSummary)) {
            if (c != null
                    && lowRisk(c)
                    && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.560d
                    && readDouble(c, "probabilityRecentValidity", 0.0d) >= QUALITY_RECENT_MIN) {
                return SHADOW_PROMOTION_GATE_MAX_RELAX;
            }
        }
        return 0.0d;
    }

    private void ensureUpstreamQualityBeforeGate(
            Models.ComboCandidate c, List<Models.Draw> history, String stage) {
        if (c == null) {
            return;
        }
        if (readString(
                                c,
                                "v20Status",
                                readString(
                                        c,
                                        "dynamicStatus",
                                        readString(c, "dynamicCauseStatus", "")))
                        .length()
                == 0) {
            writeString(c, "v20Status", STATUS_V20_WATCH_NEUTRAL);
            writeString(c, "dynamicStatus", STATUS_V20_WATCH_NEUTRAL);
            writeString(c, "dynamicCauseStatus", STATUS_V20_WATCH_NEUTRAL);
            writeInt(c, "v20EmptyFlag", 1);
            writeInt(c, "v20WatchNeutralFlag", 1);
            writeString(
                    c,
                    "v20ActionAdvice",
                    "DynamicGate兜底：V20字段缺失，按WATCH中性处理；不硬杀、不救援BET，最终仍由DecisionGate裁决");
        }
        String window =
                readString(
                        c,
                        "windowStateKey",
                        readString(
                                c,
                                "v20WindowStateKey",
                                readString(c, "dynamicWindowStateKey", "")));
        if (window.length() == 0) {
            window = buildDynamicGateFallbackWindowKey(c, stage);
            writeString(c, "windowStateKey", window);
            writeString(c, "v20WindowStateKey", window);
            writeString(c, "dynamicWindowStateKey", window);
            writeString(c, "probabilityWindowStateKey", window);
            writeString(c, "finalDecisionWindowStateKey", window);
        }
        double cover = upstreamCoverageQuality(c);
        double risk = upstreamCoverageRisk(c);
        double third = upstreamThirdQuality(c);
        if (cover <= 0.0001d) {
            cover = UPSTREAM_GATE_MIN_COVERAGE_QUALITY;
            writeDouble(c, "coverageQualityScore", cover);
        }
        if (third <= 0.0001d) {
            third = UPSTREAM_GATE_MIN_THIRD_QUALITY;
            writeDouble(c, "thirdNumberQualityScore", third);
        }
        if (risk <= 0.0001d) {
            risk = UPSTREAM_GATE_MISSING_COVERAGE_RISK;
            writeDouble(c, "k3CoverageRisk", risk);
        }
        writeCoverageAliasForGate(c, cover, third, risk);
        if (readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", "")).length()
                == 0) {
            writeString(c, "teacherMatrixStatus", "NO_SAMPLE");
            writeString(c, "teacherStatus", "NO_SAMPLE");
            writeString(c, "teacherNumberStatus", "NO_SAMPLE");
            writeInt(c, "teacherNoSampleNeutralFlag", 1);
            writeInt(c, "teacherMatrixNoSampleNeutralFlag", 1);
            writeInt(c, "teacherWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixWeakNoBetFlag", 0);
            writeInt(c, "teacherMatrixOrdinaryBetBlockFlag", 0);
            writeString(c, "teacherOrdinaryBetPolicy", "NO_SAMPLE_NEUTRAL_WATCH");
            writeString(c, "teacherMatrixOrdinaryBetPolicy", "NO_SAMPLE_NEUTRAL_WATCH");
        }
        writeString(c, "dynamicGateQualityStatus", buildUpstreamGateQualityStatus(c, null, null));
        writeString(c, "dynamicGateFieldVersion", V20_DYNAMIC_GATE_UPSTREAM_VERSION);
        writeString(
                c, "dynamicGateMainChainVersion", V20_DYNAMIC_GATE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
        writeString(c, "fieldChainVersion", V20_DYNAMIC_GATE_MAIN_CHAIN_CLOSED_LOOP_VERSION);
    }

    private void writeCoverageAliasForGate(
            Models.ComboCandidate c, double cover, double third, double risk) {
        if (c == null) return;
        cover = clamp(cover, 0.0d, 1.0d);
        third = clamp(third, 0.0d, 1.0d);
        risk = clamp(risk, 0.0d, 1.0d);
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

    private String buildDynamicGateFallbackWindowKey(Models.ComboCandidate c, String stage) {
        if (c == null) {
            return "DG_UPSTREAM_WINDOW_MISSING_P0";
        }
        return "DG_UPSTREAM_WINDOW_MISSING_P"
                + c.position
                + "_K"
                + c.n1
                + "_"
                + c.n2
                + "_"
                + c.n3
                + "_"
                + safeLocal(stage);
    }

    private String buildUpstreamGateQualityStatus(
            Models.ComboCandidate c, GateSignal signal, V20GateEvidence evidence) {
        if (c == null) return "INVALID";
        boolean teacherWeak = teacherWeakOrEmptyForGate(c, null);
        boolean v20Weak = v20UnknownOrWeakForGate(c, evidence);
        double risk = upstreamCoverageRisk(c);
        double cover = upstreamCoverageQuality(c);
        double third = upstreamThirdQuality(c);
        int falseHigh = upstreamFalseHighFlag(c);
        double edge = signal == null ? c.edge : signal.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = c.finalP - c.dynamicGate;
        }
        if (isMissingCoverageForGate(c) || cover <= 0.0001d || third <= 0.0001d) {
            return "MISSING_UPSTREAM";
        }
        if (risk >= 0.700d
                || falseHigh == 1
                || isTeacherHardRiskForGate(c)
                || isV20HardRiskForGate(c)
                || isAdaptiveHardRiskForGate(c)) {
            return "HARD_RISK";
        }
        if (edge > EDGE_FALSE_STRONG_LINE && !hasStrongCauseForEdgeGate(c, null, evidence)) {
            return "EDGE_FALSE_STRONG";
        }
        if (teacherWeak && v20Weak && c.finalP >= 0.360d) {
            return "V20_TEACHER_WEAK_WATCH";
        }
        if (isPreferredEdgeBand(c, signal)
                && !teacherWeak
                && !v20Weak
                && risk < UPSTREAM_GATE_READY_COVERAGE_MAX_RISK
                && third >= THIRD_NUMBER_QUALITY_MIN) {
            return "PREFERRED_REVIEW";
        }
        if (hasStrongCauseForEdgeGate(c, null, evidence)
                && risk < UPSTREAM_GATE_READY_COVERAGE_MAX_RISK
                && third >= THIRD_NUMBER_QUALITY_MIN) {
            return "READY_REVIEW";
        }
        return "WATCH_REVIEW";
    }

    private String buildUpstreamGateActionAdvice(
            Models.ComboCandidate c, GateSignal gate, V20GateEvidence evidence) {
        String status = buildUpstreamGateQualityStatus(c, gate, evidence);
        if ("MISSING_UPSTREAM".equals(status)) {
            return "动态门建议：上游字段仍缺失，按高风险SHADOW/HOLD复核，不支持普通BET";
        }
        if ("HARD_RISK".equals(status)) {
            return "动态门建议：覆盖/教师/假高P存在硬风险，必须转SHADOW或HOLD，最终门不可放行普通BET";
        }
        if ("EDGE_FALSE_STRONG".equals(status)) {
            return "动态门建议：Edge虚强，抬门并交DecisionGate降级SHADOW";
        }
        if ("PREFERRED_REVIEW".equals(status)) {
            return "动态门建议：P38-40且Edge1-2优先复核域，仍由DecisionGate最终裁决";
        }
        if ("READY_REVIEW".equals(status)) {
            return "动态门建议：Teacher/V20/Cover已具备复核条件，仍只作动态门支持，不直接BET";
        }
        if ("V20_TEACHER_WEAK_WATCH".equals(status)) {
            return "动态门建议：Teacher或V20弱证据，只作WATCH/SHADOW观察；NO_SAMPLE/WATCH中性不硬杀，最终门复核";
        }
        return "动态门建议：普通观察域，继续由最终门审判";
    }

    private String buildUpstreamGateRuntimeSummary(Models.ComboCandidate c) {
        if (c == null) return "";
        return "P"
                + c.position
                + " V20="
                + readString(c, "v20Status", readString(c, "dynamicStatus", ""))
                + " Teacher="
                + readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""))
                + " Cover="
                + UiFormatter.probability(upstreamCoverageQuality(c))
                + "/"
                + UiFormatter.probability(upstreamCoverageRisk(c))
                + " Third="
                + UiFormatter.probability(upstreamThirdQuality(c))
                + " Q="
                + readString(c, "dynamicGateQualityStatus", "")
                + " EdgePolicy="
                + readString(c, "dynamicGateEdgePolicy", "");
    }

    private boolean isMissingCoverageForGate(Models.ComboCandidate c) {
        if (c == null) return true;
        return readDouble(
                                c,
                                "coverageQualityScore",
                                readDouble(c, "dynamicGateCoverageQualityScore", 0.0d))
                        <= 0.0001d
                || readDouble(
                                c,
                                "thirdNumberQualityScore",
                                readDouble(c, "dynamicGateThirdNumberQuality", 0.0d))
                        <= 0.0001d
                || readDouble(c, "k3CoverageRisk", readDouble(c, "dynamicGateCoverageRisk", 0.0d))
                        <= 0.0001d;
    }

    private double upstreamCoverageQuality(Models.ComboCandidate c) {
        return c == null
                ? 0.0d
                : readDouble(
                        c,
                        "coverageQualityScore",
                        readDouble(
                                c,
                                "dynamicGateCoverageQualityScore",
                                readDouble(c, "finalDecisionCoverageQuality", 0.0d)));
    }

    private double upstreamCoverageRisk(Models.ComboCandidate c) {
        return c == null
                ? 0.0d
                : readDouble(
                        c,
                        "k3CoverageRisk",
                        readDouble(
                                c,
                                "dynamicGateCoverageRisk",
                                readDouble(c, "finalDecisionCoverageRisk", 0.0d)));
    }

    private double upstreamThirdQuality(Models.ComboCandidate c) {
        return c == null
                ? 0.0d
                : readDouble(
                        c,
                        "thirdNumberQualityScore",
                        readDouble(
                                c,
                                "dynamicGateThirdNumberQuality",
                                readDouble(c, "finalDecisionThirdNumberQuality", 0.0d)));
    }

    private int upstreamFalseHighFlag(Models.ComboCandidate c) {
        if (c == null) return 0;
        return readInt(
                c,
                "falseHighPFlag",
                readInt(
                        c,
                        "probabilityFalseHighPFlag",
                        readInt(
                                c,
                                "dynamicGateFalseHighPFlag",
                                readInt(c, "finalDecisionFalseHighPFlag", 0))));
    }

    private String safeLocal(String s) {
        return s == null ? "" : s;
    }

    private double coverageQualityGateAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.012d;
        }
        double coverageRisk = upstreamCoverageRisk(c);
        double coverageQuality = upstreamCoverageQuality(c);
        double thirdQuality = upstreamThirdQuality(c);
        int falseHigh = upstreamFalseHighFlag(c);
        boolean missingCoverage = isMissingCoverageForGate(c);
        boolean teacherWeak = teacherWeakOrEmptyForGate(c, null);
        boolean v20Weak = v20UnknownOrWeakForGate(c, null);
        double adj = 0.0d;

        if (missingCoverage) {
            adj += 0.014d;
        }
        if (falseHigh == 1) {
            adj += 0.012d;
        }
        if (coverageRisk >= 0.700d) {
            adj += 0.014d;
        } else if (coverageRisk >= K3_COVERAGE_RISK_LINE) {
            adj += 0.008d;
        }
        if (thirdQuality <= 0.0001d || thirdQuality < THIRD_NUMBER_QUALITY_MIN) {
            adj += 0.006d;
        }
        if (coverageQuality <= 0.0001d || (coverageQuality < 0.330d && c.finalP >= 0.380d)) {
            adj += 0.005d;
        }
        if (teacherWeak && v20Weak && c.finalP >= 0.360d) {
            adj += 0.008d;
        }
        if (c.finalP >= 0.400d && !hasStrongCauseForEdgeGate(c, null, null)) {
            adj += 0.008d;
        }
        return clamp(adj, 0.0d, COVERAGE_GATE_MAX_BOOST);
    }

    private double edgeVirtualStrongGateAdjustment(
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal,
            V20GateEvidence evidence) {
        if (c == null) {
            return 0.008d;
        }
        double gate = c.dynamicGate > 0.0d ? c.dynamicGate : ORIGINAL_BASE_GATE;
        double edge = c.finalP - gate;
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = c.edge;
        }
        boolean preferred =
                c.finalP >= PREFERRED_P_MIN
                        && c.finalP <= PREFERRED_P_MAX
                        && edge >= PREFERRED_EDGE_MIN
                        && edge <= PREFERRED_EDGE_MAX;
        boolean highEdge = edge > EDGE_FALSE_STRONG_LINE;
        boolean strongCause = hasStrongCauseForEdgeGate(c, teacherMatrixSignal, evidence);
        boolean weakTeacherV20 =
                teacherWeakOrEmptyForGate(c, teacherMatrixSignal)
                        && v20UnknownOrWeakForGate(c, evidence);
        double coverageRisk = upstreamCoverageRisk(c);
        double coverageQuality = upstreamCoverageQuality(c);
        double thirdQuality = upstreamThirdQuality(c);
        int falseHigh = upstreamFalseHighFlag(c);
        double counter =
                readDouble(
                        c,
                        "probabilityCounterEvidence",
                        evidence == null ? 0.0d : evidence.counterContribution);
        double adj = 0.0d;

        if (preferred
                && !weakTeacherV20
                && coverageRisk < UPSTREAM_GATE_READY_COVERAGE_MAX_RISK
                && coverageQuality >= 0.300d
                && thirdQuality >= THIRD_NUMBER_QUALITY_MIN
                && falseHigh == 0
                && counter <= UPSTREAM_GATE_READY_COUNTER_MAX
                && lowRisk(c)) {
            adj -= 0.002d;
        }

        if (highEdge && !strongCause) {
            adj += 0.010d;
        }
        if (highEdge && weakTeacherV20) {
            adj += 0.012d;
        }
        if (highEdge
                && (falseHigh == 1
                        || coverageRisk >= K3_COVERAGE_RISK_LINE
                        || thirdQuality < THIRD_NUMBER_QUALITY_MIN)) {
            adj += 0.012d;
        }
        if (c.finalP >= 0.400d && !strongCause) {
            adj += 0.008d;
        }
        if (edge >= 0.040d && counter >= 0.58d && c.finalP < 0.430d) {
            adj += 0.006d;
        }
        return clamp(adj, EDGE_QUALITY_GATE_MAX_RELAX, EDGE_QUALITY_GATE_MAX_BOOST);
    }

    private boolean isPreferredEdgeBand(Models.ComboCandidate c, GateSignal signal) {
        if (c == null) {
            return false;
        }
        double edge = signal == null ? c.edge : signal.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = c.finalP - c.dynamicGate;
        }
        return c.finalP >= PREFERRED_P_MIN
                && c.finalP <= PREFERRED_P_MAX
                && edge >= PREFERRED_EDGE_MIN
                && edge <= PREFERRED_EDGE_MAX;
    }

    private double dynamicGateVirtualEdgeRisk(Models.ComboCandidate c, GateSignal signal) {
        if (c == null) {
            return 0.0d;
        }
        double edge = signal == null ? c.edge : signal.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = c.finalP - c.dynamicGate;
        }
        double risk = 0.0d;
        if (edge > EDGE_FALSE_STRONG_LINE) {
            risk += 0.30d;
        }
        if (upstreamFalseHighFlag(c) == 1) {
            risk += 0.28d;
        }
        if (upstreamCoverageRisk(c) >= K3_COVERAGE_RISK_LINE) {
            risk += 0.24d;
        }
        if (upstreamThirdQuality(c) <= 0.0001d
                || upstreamThirdQuality(c) < THIRD_NUMBER_QUALITY_MIN) {
            risk += 0.12d;
        }
        if (teacherWeakOrEmptyForGate(c, null) && v20UnknownOrWeakForGate(c, null)) {
            risk += 0.22d;
        }
        if (c.finalP >= 0.400d && readInt(c, "probabilityStrongCauseFlag", 0) == 0) {
            risk += 0.18d;
        }
        return clamp(risk, 0.0d, 1.0d);
    }

    private String buildEdgePolicyText(Models.ComboCandidate c, GateSignal signal) {
        if (c == null) {
            return "EDGE_POLICY：候选为空";
        }
        double edge = signal == null ? c.edge : signal.edge;
        if (Double.isNaN(edge) || Double.isInfinite(edge)) {
            edge = c.finalP - c.dynamicGate;
        }
        double coverRisk = upstreamCoverageRisk(c);
        double coverQuality = upstreamCoverageQuality(c);
        double third = upstreamThirdQuality(c);
        boolean teacherWeak = teacherWeakOrEmptyForGate(c, null);
        boolean v20Weak = v20UnknownOrWeakForGate(c, null);
        int falseHigh = upstreamFalseHighFlag(c);
        if (isPreferredEdgeBand(c, signal)) {
            if (!teacherWeak
                    && !v20Weak
                    && coverRisk < UPSTREAM_GATE_READY_COVERAGE_MAX_RISK
                    && third >= THIRD_NUMBER_QUALITY_MIN
                    && falseHigh == 0) {
                return "EDGE_POLICY：P38-40 且 Edge1-2，Teacher/V20/Cover有效，进入优先复核域，最终仍由 DecisionGate"
                           + " 裁决";
            }
            return "EDGE_POLICY：P38-40 且 Edge1-2，但Teacher/V20/Cover不足，只能SHADOW复核";
        }
        if (edge > EDGE_FALSE_STRONG_LINE) {
            if (falseHigh == 1
                    || coverRisk >= K3_COVERAGE_RISK_LINE
                    || teacherWeak
                    || v20Weak
                    || third < THIRD_NUMBER_QUALITY_MIN) {
                return "EDGE_POLICY：Edge>=2 但疑似虚强，动态门抬门并建议 SHADOW 复核";
            }
            return "EDGE_POLICY：Edge>=2 不自动强，必须 Teacher/V20/覆盖质量共同确认";
        }
        if (edge < PREFERRED_EDGE_MIN) {
            return "EDGE_POLICY：Edge 低于 1%，只作弱通过观察";
        }
        return "EDGE_POLICY：普通边缘，Cover="
                + UiFormatter.probability(coverQuality)
                + "/"
                + UiFormatter.probability(coverRisk)
                + "，保持质量门复核";
    }

    private boolean hasStrongCauseForEdgeGate(
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal,
            V20GateEvidence evidence) {
        if (c == null) {
            return false;
        }
        boolean teacherStrong =
                teacherMatrixSignal != null
                        && teacherMatrixSignal.statusStrong()
                        && teacherMatrixSignal.riskPenalty <= 0.012d;
        boolean v20Strong =
                evidence != null
                        && evidence.evidenceContribution >= QUALITY_EVIDENCE_STRONG
                        && evidence.counterContribution <= 0.56d
                        && evidence.sampleReliability >= QUALITY_SAMPLE_MIN
                        && evidence.recentValidity >= QUALITY_RECENT_MIN;
        if (teacherStrong || v20Strong) {
            return true;
        }
        if (readInt(c, "probabilityStrongCauseFlag", 0) == 1) {
            return true;
        }
        String ts = readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""));
        String vs =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicGrade", "")));
        if (containsStrongReady(ts) || containsStrongReady(vs)) {
            return true;
        }
        return readDouble(c, "shadowPromotionBoost", 0.0d) > 0.0d
                && readDouble(c, "probabilityCounterEvidence", 0.0d) <= 0.55d
                && readDouble(c, "k3CoverageRisk", 0.0d) < 0.55d;
    }

    private boolean teacherWeakOrEmptyForGate(
            Models.ComboCandidate c, TeacherMatrixGateSignal teacherMatrixSignal) {
        if (teacherMatrixSignal != null && teacherMatrixSignal.statusStrong()) {
            return false;
        }
        if (teacherMatrixSignal != null
                && STATUS_NO_SAMPLE_NEUTRAL.equals(teacherMatrixSignal.status)) {
            return false;
        }
        if (isTeacherNoSampleNeutralForGate(c)) {
            return false;
        }
        String s = readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""));
        String u = upperLocal(s);
        if (containsStrongReady(s)) {
            return false;
        }
        return u.indexOf("WEAK") >= 0
                || u.indexOf("LOW") >= 0
                || u.indexOf("FAIL") >= 0
                || u.indexOf("弱") >= 0;
    }

    private boolean v20UnknownOrWeakForGate(Models.ComboCandidate c, V20GateEvidence evidence) {
        if (evidence != null
                && evidence.evidenceContribution >= QUALITY_EVIDENCE_STRONG
                && evidence.counterContribution <= 0.56d
                && evidence.sampleReliability >= QUALITY_SAMPLE_MIN
                && evidence.recentValidity >= QUALITY_RECENT_MIN) {
            return false;
        }
        if (isV20WatchOrEmptyNeutralForGate(c)) {
            return false;
        }
        String s =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicGrade", "")));
        String u = upperLocal(s);
        if (containsStrongReady(s)) {
            return false;
        }
        return u.indexOf("WEAK") >= 0
                || u.indexOf("LOW") >= 0
                || u.indexOf("NO_SUPPORT") >= 0
                || u.indexOf("弱") >= 0;
    }

    private boolean isNoSampleWatchNeutralForDynamicGate(Models.ComboCandidate c) {
        return isTeacherNoSampleNeutralForGate(c)
                || isV20WatchOrEmptyNeutralForGate(c)
                || isAdaptiveWatchNeutralForGate(c);
    }

    private boolean isTeacherNoSampleNeutralForGate(Models.ComboCandidate c) {
        String s =
                readString(
                        c,
                        "teacherMatrixStatus",
                        readString(c, "teacherStatus", readString(c, "teacherNumberStatus", "")));
        String u = upperLocal(s);
        int sample = readInt(c, "teacherMatrixSample", readInt(c, "teacherSample", 0));
        int comboSample = readInt(c, "teacherMatrixComboSample", 0);
        if (u.length() == 0) {
            return true;
        }
        if (u.indexOf("NO_SAMPLE") >= 0
                || u.indexOf("NONE") >= 0
                || u.indexOf("EMPTY") >= 0
                || u.indexOf("UNKNOWN") >= 0
                || STATUS_NO_SAMPLE_NEUTRAL.equals(u)) {
            return sample <= 0 && comboSample <= 0;
        }
        return false;
    }

    private boolean isV20WatchOrEmptyNeutralForGate(Models.ComboCandidate c) {
        String s =
                readString(
                        c,
                        "v20Status",
                        readString(
                                c,
                                "dynamicStatus",
                                readString(
                                        c, "dynamicCauseStatus", readString(c, "v20Action", ""))));
        String u = upperLocal(s);
        if (u.length() == 0
                || u.indexOf("UNKNOWN") >= 0
                || u.indexOf("WATCH") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || STATUS_V20_WATCH_NEUTRAL.equals(u)) {
            return true;
        }
        return false;
    }

    private boolean isAdaptiveWatchNeutralForGate(Models.ComboCandidate c) {
        String s =
                readString(
                        c,
                        "adaptiveStatus",
                        readString(c, "adaptiveJudgeStatus", readString(c, "adaptiveAction", "")));
        String u = upperLocal(s);
        return u.length() == 0
                || u.indexOf("WATCH") >= 0
                || u.indexOf("PENDING") >= 0
                || u.indexOf("NEUTRAL") >= 0
                || STATUS_ADAPTIVE_WATCH_NEUTRAL.equals(u);
    }

    private boolean isTeacherHardRiskForGate(Models.ComboCandidate c) {
        if (c == null) return false;
        if (readInt(c, "hardBlockFlag", 0) == 1
                && "TeacherNumberMatrixEngine".equals(readString(c, "hardBlockSource", "")))
            return true;
        String status =
                upperLocal(
                        readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", "")));
        int sample =
                Math.max(
                        readInt(c, "teacherMatrixSample", 0), readInt(c, "teacherNumberSample", 0));
        double risk =
                Math.max(
                        readDouble(c, "teacherMatrixRisk", 0.0d),
                        readDouble(c, "teacherNumberRisk", 0.0d));
        return sample >= 30
                && (status.indexOf("FREEZE") >= 0
                        || status.indexOf("HARD_BLOCK") >= 0
                        || risk >= 0.72d);
    }

    private boolean isV20HardRiskForGate(Models.ComboCandidate c) {
        if (c == null) return false;
        if (readInt(c, "hardBlockFlag", 0) == 1
                && AppConfig.isExplicitHardBlockStatus(readString(c, "riskSeverity", "")))
            return true;
        String status =
                upperLocal(
                        readString(
                                c,
                                "v20Status",
                                readString(c, "dynamicStatus", readString(c, "v20Action", ""))));
        double counter =
                readDouble(
                        c,
                        "counterEvidenceScore",
                        readDouble(c, "probabilityCounterEvidence", 0.0d));
        return readInt(c, "dynamicHardRisk", 0) == 1
                || (AppConfig.isExplicitHardBlockStatus(status)
                        && counter >= V20_GATE_HARD_COUNTER_LINE)
                || counter >= 0.78d;
    }

    private boolean isAdaptiveHardRiskForGate(Models.ComboCandidate c) {
        if (c == null) return false;
        if (readInt(c, "hardBlockFlag", 0) == 1) return true;
        String severity = readString(c, "riskSeverity", "");
        if (AppConfig.isExplicitHardBlockStatus(severity)) return true;
        String status =
                upperLocal(
                        readString(
                                c,
                                "adaptiveStatus",
                                readString(
                                        c,
                                        "adaptiveJudgeStatus",
                                        readString(c, "adaptiveAction", ""))));
        if (status.indexOf("WATCH") >= 0
                || status.indexOf("NEUTRAL") >= 0
                || status.indexOf("NO_SAMPLE") >= 0) return false;
        return AppConfig.isExplicitHardBlockStatus(status);
    }

    private String upperLocal(String text) {
        return text == null ? "" : text.toUpperCase(java.util.Locale.ROOT);
    }

    private boolean containsStrongReady(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("STRONG") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("PASS") >= 0
                || "S".equals(s)
                || "A".equals(s)
                || s.indexOf("V20_STRONG") >= 0
                || s.indexOf("V20_READY") >= 0;
    }

    private MainChainGateSignal evaluateMainChainGateSignal(
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal,
            V20GateEvidence evidence,
            GateBatchContext ctx) {
        MainChainGateSignal s = new MainChainGateSignal();
        if (c == null) {
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_EMPTY";
            s.actionAdvice = "MAIN_CHAIN_WATCH_EMPTY";
            s.summary = "主链门槛：候选为空，中性观察";
            return s;
        }
        double positionNeed =
                readDouble(c, "positionNeedScore", readDouble(c, "positionCauseScore", 0.0d));
        double numberSupply =
                readDouble(c, "numberSupplyScore", readDouble(c, "numberSupplyStrength", 0.0d));
        double coverage =
                readDouble(
                        c,
                        "coverageQuality",
                        readDouble(c, "coverageQualityScore", upstreamCoverageQuality(c)));
        double third =
                readDouble(
                        c,
                        "thirdQuality",
                        readDouble(c, "thirdNumberQuality", upstreamThirdQuality(c)));
        double ruleHealth = readDouble(c, "ruleHealthScore", 0.0d);
        double ruleDecay = readDouble(c, "ruleDecayRisk", 0.0d);
        double thirdDrag = readDouble(c, "thirdDragRisk", readDouble(c, "thirdWeakness", 0.0d));
        double counterRisk =
                readDouble(c, "counterfactualRisk", readDouble(c, "rankCounterEdge", 0.0d));
        double shadowPromote =
                readDouble(c, "shadowPromoteScore", readDouble(c, "shadowPromotionScore", 0.0d));
        double sample =
                readDouble(
                        c,
                        "sampleReliability",
                        readDouble(c, "probabilitySampleReliability", 0.0d));
        double recent =
                readDouble(c, "recentValidity", readDouble(c, "probabilityRecentValidity", 0.0d));
        double anti =
                readDouble(
                        c,
                        "antiEvidenceScore",
                        readDouble(
                                c,
                                "counterEvidenceScore",
                                readDouble(c, "probabilityCounterEvidence", 0.0d)));
        double net =
                readDouble(c, "netEvidenceScore", readDouble(c, "mainChainNetEvidenceScore", 0.0d));
        String adaptive =
                readString(
                        c,
                        "adaptiveStatus",
                        readString(c, "adaptiveJudgeStatus", readString(c, "adaptiveAction", "")));
        String teacher = readString(c, "teacherMatrixStatus", readString(c, "teacherStatus", ""));
        String v20 =
                readString(
                        c,
                        "v20Status",
                        readString(c, "dynamicStatus", readString(c, "dynamicCauseStatus", "")));

        s.evidenceScore =
                clamp(
                        positionNeed * 0.18d
                                + numberSupply * 0.18d
                                + coverage * 0.16d
                                + third * 0.12d
                                + ruleHealth * 0.12d
                                + shadowPromote * 0.10d
                                + sample * 0.07d
                                + recent * 0.07d,
                        0.0d,
                        1.0d);
        s.counterScore =
                clamp(
                        anti * 0.20d
                                + ruleDecay * 0.18d
                                + thirdDrag * 0.18d
                                + counterRisk * 0.18d
                                + upstreamCoverageRisk(c) * 0.12d
                                + (isTeacherHardRiskForGate(c) ? 0.08d : 0.0d)
                                + (isV20HardRiskForGate(c) ? 0.08d : 0.0d),
                        0.0d,
                        1.0d);
        if (net == 0.0d) {
            net = s.evidenceScore - s.counterScore;
        }
        s.netScore = clamp((net + 1.0d) / 2.0d, 0.0d, 1.0d);

        boolean neutralTeacher = isTeacherNoSampleNeutralForGate(c);
        boolean neutralV20 = isV20WatchOrEmptyNeutralForGate(c);
        boolean neutralAdaptive = isAdaptiveWatchNeutralForGate(c);
        boolean hardRisk =
                isTeacherHardRiskForGate(c)
                        || isV20HardRiskForGate(c)
                        || isAdaptiveHardRiskForGate(c)
                        || ruleDecay >= 0.72d
                        || thirdDrag >= 0.72d
                        || counterRisk >= 0.72d;

        if (hardRisk) {
            s.gateAdjust += Math.min(MAIN_CHAIN_GATE_MAX_BOOST, 0.006d + s.counterScore * 0.014d);
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_RISK_FREEZE";
            s.actionAdvice = "MAIN_CHAIN_FREEZE_OR_SHADOW";
        } else if (s.evidenceScore >= 0.60d
                && s.counterScore <= 0.50d
                && coverage >= 0.30d
                && third >= THIRD_NUMBER_QUALITY_MIN
                && c.edge >= 0.0d) {
            s.gateAdjust -= 0.003d;
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_READY";
            s.actionAdvice = "MAIN_CHAIN_PASS_TO_DECISION_GATE_REVIEW";
        } else if (neutralTeacher || neutralV20 || neutralAdaptive) {
            s.gateAdjust += 0.000d;
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_NEUTRAL_WATCH";
            s.actionAdvice = "MAIN_CHAIN_WATCH_NEUTRAL_NO_HARD_KILL";
        } else if (s.counterScore > s.evidenceScore + 0.10d) {
            s.gateAdjust += Math.min(0.010d, (s.counterScore - s.evidenceScore) * 0.030d);
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_COUNTER_STRONG";
            s.actionAdvice = "MAIN_CHAIN_DOWNGRADE_TO_SHADOW";
        } else {
            s.rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_WATCH";
            s.actionAdvice = "MAIN_CHAIN_WATCH";
        }

        s.gateAdjust = clamp(s.gateAdjust, MAIN_CHAIN_GATE_MAX_RELAX, MAIN_CHAIN_GATE_MAX_BOOST);
        s.summary =
                "主链动态门｜证据="
                        + UiFormatter.probability(s.evidenceScore)
                        + "｜反证="
                        + UiFormatter.probability(s.counterScore)
                        + "｜调门="
                        + UiFormatter.signedProbability(s.gateAdjust)
                        + "｜Teacher="
                        + safeLocal(teacher)
                        + "｜V20="
                        + safeLocal(v20)
                        + "｜Adaptive="
                        + safeLocal(adaptive)
                        + "｜拒绝码="
                        + s.rejectCode
                        + "｜说明=NO_SAMPLE/WATCH中性不硬杀，RISK/FREEZE才收紧，最终仍由DecisionGate裁决";
        return s;
    }

    private String buildQualityGateSummary(
            GateSignal signal, Models.ComboCandidate c, GateBatchContext ctx) {
        if (signal == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("质量门=");
        sb.append(UiFormatter.signedProbability(signal.qualityGateAdjust));
        sb.append(" 诊断门=");
        sb.append(UiFormatter.signedProbability(signal.diagnosisGateAdjust));
        sb.append(" 模块=");
        sb.append(UiFormatter.signedProbability(signal.moduleEffectGateAdjust));
        sb.append(" 冻结=");
        sb.append(UiFormatter.signedProbability(signal.freezeGateAdjust));
        sb.append(" SHADOW=");
        sb.append(UiFormatter.signedProbability(signal.shadowPromotionGateAdjust));
        sb.append(" 覆盖=");
        sb.append(UiFormatter.signedProbability(signal.coverageGateAdjust));
        sb.append(" Edge质检=");
        sb.append(UiFormatter.signedProbability(signal.edgeQualityGateAdjust));
        if (c != null) {
            sb.append(" EdgePolicy=");
            sb.append(buildEdgePolicyText(c, signal));
            sb.append(" 覆盖质/风险=");
            sb.append(UiFormatter.probability(readDouble(c, "coverageQualityScore", 0.0d)));
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "k3CoverageRisk", 0.0d)));
            sb.append(" 证据/反证=");
            sb.append(
                    UiFormatter.probability(
                            readDouble(c, "probabilityEvidenceContribution", 0.0d)));
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "probabilityCounterEvidence", 0.0d)));
            sb.append(" 样本/近期=");
            sb.append(UiFormatter.probability(readDouble(c, "probabilitySampleReliability", 0.0d)));
            sb.append("/");
            sb.append(UiFormatter.probability(readDouble(c, "probabilityRecentValidity", 0.0d)));
        }
        return sb.toString();
    }

    private String buildFrequencyPolicyText(GateSignal signal, Models.ComboCandidate c) {
        if (signal == null) {
            return "";
        }
        if (c != null && dynamicGateVirtualEdgeRisk(c, signal) >= 0.50d) {
            return "Edge虚强风险优先：Edge>=2不自动强，先看Teacher/V20/覆盖质量";
        }
        if (signal.frequencyAdjust > 0.010d) {
            return "频率高且质量/假强风险不纯，按同类风险抬门，不做硬上限";
        }
        if (signal.frequencyAdjust > 0.0d) {
            return "频率偏高，仅软提醒抬门，不硬拦";
        }
        if (signal.frequencyAdjust < 0.0d) {
            return "低频不救援，仅强证据低反证小幅放松";
        }
        return "频率只监控：质量合格则不因频率高硬压，质量不足也不因频率低救援";
    }

    private String buildQualityActionAdvice(GateSignal signal, Models.ComboCandidate c) {
        if (signal == null) {
            return "WATCH";
        }
        if (signal.hardBlock == 1) {
            return "FREEZE/HOLD：命中硬风险";
        }
        if (signal.freezeGateAdjust >= 0.010d || signal.diagnosisGateAdjust >= 0.014d) {
            return "DOWNGRADE_TO_SHADOW：诊断反证或冻结风险高";
        }
        if (signal.allowedByGate == 1
                && signal.edge >= EDGE_STRONG_MIN
                && signal.qualityGateAdjust <= 0.0d) {
            return "PASS_TO_DECISION_GATE：质量门通过，仍由DecisionGate最终裁决";
        }
        if (signal.shadowPromotionGateAdjust < 0.0d) {
            return "PROMOTE_WATCH：SHADOW晋级强域小幅放松，仍需FinalP/Gate/DecisionGate";
        }
        return "WATCH：动态门仅给质量风险建议";
    }

    private boolean containsMisleading(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("MISLEADING") >= 0
                || s.indexOf("误导") >= 0
                || s.indexOf("亏") >= 0
                || s.indexOf("偏弱") >= 0;
    }

    private boolean containsEffective(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("EFFECTIVE") >= 0 || s.indexOf("有效") >= 0 || s.indexOf("强域") >= 0;
    }

    private boolean containsPromotion(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        return s.indexOf("PROMOTE") >= 0 || s.indexOf("晋级") >= 0 || s.indexOf("强域") >= 0;
    }

    private boolean containsRiskWord(String text) {
        return AppConfig.isExplicitHardBlockStatus(text);
    }

    private boolean containsPassWord(String text) {
        if (text == null) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        if (metricPositive(s, "PASS")
                || metricPositive(s, "PROMOTE")
                || metricPositive(s, "通过")
                || metricPositive(s, "晋级")) {
            return true;
        }
        return s.indexOf("PASS_TO_GATE") >= 0
                || s.indexOf("PASS_TO_BET") >= 0
                || s.indexOf("PROMOTE_WATCH") >= 0
                || s.indexOf("允许进入") >= 0;
    }

    private boolean metricPositive(String text, String key) {
        if (text == null || key == null || key.length() == 0) {
            return false;
        }
        String s = text.toUpperCase(java.util.Locale.ROOT);
        String k = key.toUpperCase(java.util.Locale.ROOT);
        int from = 0;
        while (true) {
            int idx = s.indexOf(k, from);
            if (idx < 0) {
                return false;
            }
            int p = idx + k.length();
            while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
                p++;
            }
            if (p < s.length()) {
                char sep = s.charAt(p);
                if (sep == '=' || sep == ':' || sep == '：') {
                    p++;
                    while (p < s.length() && Character.isWhitespace(s.charAt(p))) {
                        p++;
                    }
                    int start = p;
                    boolean dot = false;
                    while (p < s.length()) {
                        char ch = s.charAt(p);
                        if (ch >= '0' && ch <= '9') {
                            p++;
                            continue;
                        }
                        if (ch == '.' && !dot) {
                            dot = true;
                            p++;
                            continue;
                        }
                        break;
                    }
                    if (p > start) {
                        try {
                            double v = Double.parseDouble(s.substring(start, p));
                            if (v > 0.000001d) {
                                return true;
                            }
                        } catch (Throwable e) {
                        }
                    }
                }
            }
            from = idx + k.length();
        }
    }

    private double auditAdjustmentFromContext(GateBatchContext ctx, Models.ComboCandidate c) {
        double adj = 0.0d;

        if (ctx != null) {
            if ("BLOCK".equals(ctx.auditStatus) || "ERROR".equals(ctx.auditStatus)) {
                adj += 0.008d;
            } else if ("WARN".equals(ctx.auditStatus)) {
                adj += 0.003d;
            }
        }

        if (c != null && c.auditPenalty > 0.0d) {
            adj += Math.min(0.006d, c.auditPenalty * 0.018d);
        }

        return clamp(adj, 0.000d, 0.010d);
    }

    private double teacherAdjustmentFromContext(GateBatchContext ctx) {
        if (ctx == null) {
            return 0.0d;
        }

        return clamp(ctx.teacherAdjust, -0.006d, 0.010d);
    }

    private double teacherMatrixAdjustment(Models.ComboCandidate c, TeacherMatrixGateSignal t) {
        if (c == null || t == null || t.applied == 0) {
            return 0.0d;
        }
        /* Teacher NO_SAMPLE/NEUTRAL是真中性，禁止因0置信度重复抬门。 */
        if (isTeacherNoSampleNeutralForGate(c)) {
            return 0.0d;
        }

        double adj = 0.0d;

        if ("RISK".equals(t.status)) {
            adj += 0.006d;
        }

        if ("WEAK".equals(t.status) && c.finalP < 0.390d) {
            adj += 0.001d;
        }

        if ("STRONG".equals(t.status)
                && t.sample >= TEACHER_MATRIX_STRONG_SAMPLE
                && t.hitRate >= 0.400d
                && t.confidence >= 0.500d
                && lowRisk(c)) {
            adj -= 0.006d;
        } else if ("READY".equals(t.status)
                && t.sample >= TEACHER_MATRIX_MIN_SAMPLE
                && t.hitRate >= 0.370d
                && t.confidence >= 0.360d
                && lowRisk(c)) {
            adj -= 0.003d;
        }

        if (t.comboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                && t.comboHitRate >= 0.400d
                && lowRisk(c)) {
            adj -= 0.004d;
        } else if (t.comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && t.comboHitRate >= 0.370d
                && lowRisk(c)) {
            adj -= 0.002d;
        }

        if (t.gateAdjust != 0.0d) {
            adj += clamp(t.gateAdjust, -0.006d, 0.006d);
        }

        if (t.sample >= TEACHER_MATRIX_STRONG_SAMPLE
                && t.hitRate < 0.290d
                && t.confidence >= 0.460d) {
            adj += 0.005d;
        }

        if (t.comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && t.comboHitRate > 0.0d
                && t.comboHitRate < 0.290d) {
            adj += 0.003d;
        }

        if (t.riskPenalty > 0.010d) {
            adj += Math.min(0.005d, t.riskPenalty * 0.40d);
        }

        return clamp(adj, -0.008d, 0.014d);
    }

    private double realtimeValidationAdjustmentFromContext(GateBatchContext ctx) {
        if (ctx == null) {
            return 0.0d;
        }

        double adj = 0.0d;

        if (ctx.validationBetCount <= 0) {
            return 0.0d;
        }

        if (ctx.validationHitRate > 0.0d && ctx.validationHitRate < 0.300d) {
            adj += 0.008d;
        } else if (ctx.validationBetCount >= 30
                && ctx.validationHitRate >= 0.400d
                && ctx.validationPnl >= 0.0d) {
            adj -= 0.003d;
        } else if (ctx.validationBetCount >= 60
                && ctx.validationHitRate >= 0.380d
                && ctx.validationPnl >= 0.0d) {
            adj -= 0.002d;
        }

        if (ctx.validationPnl < 0.0d
                && ctx.validationMaxDrawdown > Math.abs(AppConfig.LOSE_NET_PROFIT) * 5.0d) {
            adj += 0.004d;
        }

        if (ctx.validationTeacherSample >= 50
                && ctx.validationTeacherHitRate > 0.0d
                && ctx.validationTeacherHitRate < 0.290d) {
            adj += 0.002d;
        }

        if (ctx.validationTeacherSample >= 80
                && ctx.validationTeacherHitRate >= 0.400d
                && ctx.validationPnl >= 0.0d) {
            adj -= 0.002d;
        }

        return clamp(adj, -0.004d, 0.008d);
    }

    private double profileAdjustment(Models.ComboCandidate c, CacheGateSignal cacheSignal) {
        if (c == null) {
            return 0.0d;
        }

        double adj = 0.0d;

        if (c.deepProfileSample >= PROFILE_SAMPLE_MIN) {
            if (c.deepProfileHitRate > 0.0d && c.deepProfileHitRate < 0.290d && c.finalP < 0.400d) {
                adj += 0.005d;
            }

            if (c.deepProfileHitRate >= 0.400d
                    && c.deepProfileSample >= PROFILE_SAMPLE_STRONG
                    && lowRisk(c)) {
                adj -= 0.004d;
            } else if (c.deepProfileHitRate >= 0.370d
                    && c.deepProfileSample >= PROFILE_SAMPLE_MIN
                    && lowRisk(c)) {
                adj -= 0.002d;
            }
        }

        if (c.profileScore > 0.0d && c.profileScore < 0.250d && c.finalP < 0.400d) {
            adj += 0.004d;
        }

        if (c.profileScore >= 0.620d && lowRisk(c)) {
            adj -= 0.004d;
        } else if (c.profileScore >= 0.540d && lowRisk(c) && c.finalP >= 0.390d) {
            adj -= 0.002d;
        }

        if (c.fallbackProfileScore > 0.0d
                && c.fallbackProfileScore < 0.250d
                && c.profileScore < 0.320d
                && c.finalP < 0.400d) {
            adj += 0.003d;
        }

        if (c.fallbackProfileScore >= 0.620d && lowRisk(c)) {
            adj -= 0.003d;
        }

        if (c.deepFallbackLevel >= 3 && c.finalP < 0.395d) {
            adj += 0.002d;
        }

        if (cacheSignal != null && cacheSignal.sample >= 8) {
            if (cacheSignal.hitRate > 0.0d && cacheSignal.hitRate < 0.290d && c.finalP < 0.400d) {
                adj += 0.003d;
            }

            if (cacheSignal.hitRate >= 0.390d
                    && cacheSignal.sample >= PROFILE_SAMPLE_STRONG
                    && cacheSignal.risk <= 0.560d
                    && lowRisk(c)) {
                adj -= 0.003d;
            }

            if (cacheSignal.weakestNumberSupport > 0.0d
                    && cacheSignal.weakestNumberSupport < 0.250d
                    && c.finalP < 0.400d) {
                adj += 0.002d;
            }

            if (cacheSignal.stateNumberSample >= 18
                    && cacheSignal.avgNumberSupport >= 0.370d
                    && cacheSignal.weakestNumberSupport >= 0.300d
                    && lowRisk(c)) {
                adj -= 0.002d;
            }
        }

        return clamp(adj, -0.008d, 0.010d);
    }

    private double promotionAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double adj = 0.0d;

        if (c.promotionScore > 0.0d && c.promotionScore < 0.250d && c.finalP < 0.395d) {
            adj += 0.003d;
        }

        if (c.promotionScore >= 0.620d && lowRisk(c) && c.finalP >= 0.390d) {
            adj -= 0.003d;
        } else if (c.promotionScore >= 0.540d && lowRisk(c) && c.finalP >= 0.395d) {
            adj -= 0.0015d;
        }

        if (c.promotionStatus != null) {
            if (c.promotionStatus.indexOf("REJECT") >= 0
                    || c.promotionStatus.indexOf("BLOCK") >= 0) {
                adj += 0.003d;
            }

            if (c.promotionStatus.indexOf("PASS") >= 0
                    || c.promotionStatus.indexOf("STRONG") >= 0
                    || c.promotionStatus.indexOf("READY") >= 0) {
                if (lowRisk(c) && c.finalP >= 0.390d) {
                    adj -= 0.002d;
                }
            }
        }

        return clamp(adj, -0.004d, 0.006d);
    }

    private double aPlusAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double adj = 0.0d;

        if (c.aPlusScore > 0.0d && c.aPlusScore < 0.250d && c.finalP < 0.395d) {
            adj += 0.002d;
        }

        if (c.aPlusScore >= 0.620d && lowRisk(c) && c.finalP >= 0.390d) {
            adj -= 0.003d;
        } else if (c.aPlusScore >= 0.540d && lowRisk(c) && c.finalP >= 0.395d) {
            adj -= 0.0015d;
        }

        if (c.aPlusStatus != null) {
            if (c.aPlusStatus.indexOf("REJECT") >= 0 || c.aPlusStatus.indexOf("BLOCK") >= 0) {
                adj += 0.002d;
            }

            if (c.aPlusStatus.indexOf("PASS") >= 0
                    || c.aPlusStatus.indexOf("STRONG") >= 0
                    || c.aPlusStatus.indexOf("READY") >= 0) {
                if (lowRisk(c) && c.finalP >= 0.390d) {
                    adj -= 0.002d;
                }
            }
        }

        return clamp(adj, -0.004d, 0.005d);
    }

    private double v20CauseChainAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = readDouble(c, "dynamicCauseScore", readDouble(c, "causeScore", 0.0d));
        double support = readDouble(c, "dynamicSupportScore", readDouble(c, "supportScore", 0.0d));
        double risk = readDouble(c, "dynamicRiskScore", readDouble(c, "riskScore", 0.0d));
        double conf = readDouble(c, "dynamicConfidence", readDouble(c, "confidence", 0.0d));
        int pass = readInt(c, "dynamicPass", 0);
        int hard = readInt(c, "dynamicHardRisk", 0);

        if (hard == 1) {
            return 0.010d;
        }

        if (risk >= 0.700d) {
            return 0.007d;
        }

        if (pass == 1
                && score >= 0.620d
                && support >= 0.560d
                && risk <= 0.500d
                && conf >= 0.420d
                && lowRisk(c)) {
            return -0.005d;
        }

        if (score >= 0.540d && risk <= 0.560d && c.finalP >= 0.390d && lowRisk(c)) {
            return -0.002d;
        }

        if (score < 0.360d && c.finalP >= 0.390d) {
            return 0.004d;
        }

        return 0.0d;
    }

    private double v20RuleHealthAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double health = readDouble(c, "ruleHealthScore", 0.0d);
        double risk = readDouble(c, "ruleHealthRisk", readDouble(c, "ruleRiskScore", 0.0d));
        int pass = readInt(c, "ruleHealthPass", readInt(c, "rulePass", 0));
        int hard = readInt(c, "ruleHealthHardCount", readInt(c, "hardBlock", 0));

        if (hard > 0) {
            return 0.010d;
        }

        if (risk >= 0.720d) {
            return 0.008d;
        }

        if (pass == 1 && health >= 0.620d && risk <= 0.460d && lowRisk(c)) {
            return -0.003d;
        }

        if (health > 0.0d && health < 0.360d && c.finalP < 0.410d) {
            return 0.003d;
        }

        return 0.0d;
    }

    private double v20ThirdGuardAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = readDouble(c, "thirdGuardScore", readDouble(c, "guardScore", 0.0d));
        double risk = readDouble(c, "thirdGuardRisk", readDouble(c, "guardRisk", 0.0d));
        int pass = readInt(c, "thirdGuardPass", 0);
        int hard = readInt(c, "thirdGuardHardBlock", readInt(c, "hardBlock", 0));

        if (hard == 1) {
            return 0.012d;
        }

        if (risk >= 0.700d) {
            return 0.008d;
        }

        if (pass == 1 && score >= 0.620d && risk <= 0.460d && lowRisk(c)) {
            return -0.003d;
        }

        return 0.0d;
    }

    private double v20ShadowHealthAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double health = readDouble(c, "shadowHealthScore", 0.0d);
        double risk = readDouble(c, "shadowHealthRisk", 0.0d);
        int pass = readInt(c, "shadowPass", readInt(c, "shadowHealthPass", 0));
        int hard = readInt(c, "shadowHardBlock", 0);
        int sample = readInt(c, "shadowSample", 0);

        if (hard == 1) {
            return 0.010d;
        }

        if (risk >= 0.700d) {
            return 0.007d;
        }

        if (sample < readWarmupIssues() && health >= 0.560d && c.finalP < 0.400d) {
            return 0.002d;
        }

        if (pass == 1
                && health >= 0.620d
                && risk <= 0.460d
                && sample >= readWarmupIssues()
                && lowRisk(c)) {
            return -0.003d;
        }

        return 0.0d;
    }

    private double v20FalseStrongFreezeAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double risk =
                readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d));
        int freeze = readInt(c, "falseStrongFreeze", 0);
        int hard = readInt(c, "falseStrongHardFreeze", 0);

        if (hard == 1) {
            return 0.018d;
        }

        if (freeze == 1) {
            return 0.014d;
        }

        if (risk >= 0.720d) {
            return 0.012d;
        }

        if (risk >= 0.560d) {
            return 0.006d;
        }

        return 0.0d;
    }

    private double v20CounterfactualAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score =
                readDouble(
                        c, "counterfactualScore", readDouble(c, "counterfactualRankScore", 0.0d));
        double risk = readDouble(c, "counterfactualRisk", 0.0d);
        double missed = readDouble(c, "missedStrongRisk", 0.0d);
        double falseChampion = readDouble(c, "falseChampionRisk", 0.0d);
        double nearConflict = readDouble(c, "nearConflictRisk", 0.0d);

        double totalRisk = Math.max(risk, Math.max(missed, Math.max(falseChampion, nearConflict)));

        if (totalRisk >= 0.700d) {
            return 0.010d;
        }

        if (score >= 0.620d && totalRisk <= 0.460d && lowRisk(c)) {
            return -0.003d;
        }

        if (score > 0.0d && score < 0.360d && c.finalP >= 0.390d) {
            return 0.003d;
        }

        return 0.0d;
    }

    private double v20SeedAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = readDouble(c, "seedExpandScore", readDouble(c, "seedScore", 0.0d));
        double risk = readDouble(c, "seedRisk", 0.0d);
        int pass = readInt(c, "seedPass", 0);
        int hard = readInt(c, "seedHardRisk", 0);

        if (hard == 1) {
            return 0.006d;
        }

        if (risk >= 0.660d) {
            return 0.004d;
        }

        if (pass == 1 && score >= 0.600d && risk <= 0.460d && lowRisk(c)) {
            return -0.002d;
        }

        return 0.0d;
    }

    private double v20TeacherNumberAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score =
                readDouble(c, "teacherNumberScore", readDouble(c, "teacherComboScore", 0.0d));
        double risk = readDouble(c, "teacherNumberRisk", 0.0d);
        double confidence = readDouble(c, "teacherNumberConfidence", 0.0d);
        int ready = readInt(c, "numberTeacherReady", 0);

        if (risk >= 0.660d) {
            return 0.006d;
        }

        if ((ready == 1 || score >= 0.600d)
                && risk <= 0.460d
                && confidence >= 0.360d
                && lowRisk(c)) {
            return -0.004d;
        }

        if (score > 0.0d && score < 0.300d && c.finalP >= 0.390d) {
            return 0.003d;
        }

        return 0.0d;
    }

    private double v20PositionHealthAdjustment(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double health =
                readDouble(c, "positionHealthScore", readDouble(c, "positionCauseScore", 0.0d));
        double risk = readDouble(c, "positionWrongRisk", readDouble(c, "positionRiskScore", 0.0d));
        double drift = readDouble(c, "positionDriftRisk", 0.0d);
        double falseRisk = readDouble(c, "positionFalseStrongRisk", 0.0d);

        double maxRisk = Math.max(risk, Math.max(drift, falseRisk));

        if (maxRisk >= 0.700d) {
            return 0.008d;
        }

        if (health >= 0.620d && maxRisk <= 0.460d && lowRisk(c)) {
            return -0.003d;
        }

        if (health > 0.0d && health < 0.320d && c.finalP >= 0.390d) {
            return 0.003d;
        }

        return 0.0d;
    }

    private double strengthScore(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CacheGateSignal cacheSignal,
            TeacherMatrixGateSignal teacherMatrixSignal) {

        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            score += 18.0d;
        }

        if (c.finalP >= 0.390d) {
            score += 9.0d;
        }

        if (c.finalP >= 0.400d) {
            score += 10.0d;
        }

        if (c.finalP >= 0.410d) {
            score += 8.0d;
        }

        if (c.edge >= EDGE_STRONG_MIN) {
            score += 8.0d;
        }

        if (c.trendLife >= trendLifeMin()) {
            score += 10.0d;
        }

        if (c.trendLife >= AppConfig.TREND_LIFE_HIGH) {
            score += 8.0d;
        }

        if (c.driftRisk <= AppConfig.DRIFT_RISK_MAX) {
            score += 8.0d;
        }

        if (c.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
            score += 8.0d;
        }

        if (c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
            score += 6.0d;
        }

        if (c.championGap >= AppConfig.CHAMPION_GAP_MIN) {
            score += 5.0d;
        }

        if (c.comboGap >= AppConfig.COMBO_GAP_MIN) {
            score += 5.0d;
        }

        if (c.effectiveSample >= 180) {
            score += 6.0d;
        } else if (c.effectiveSample >= 120) {
            score += 4.0d;
        }

        if (c.comboHitRate >= 0.380d) {
            score += 5.0d;
        }

        if (similarResult != null
                && similarResult.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_MIN
                && similarResult.similarHitRate >= 0.370d) {
            score += 5.0d;
        }

        if (memorySignal != null && memorySignal.usable == 1 && memorySignal.memoryScore >= 55.0d) {
            score += 5.0d;
        }

        if (cacheSignal != null) {
            if (cacheSignal.sample >= 18
                    && cacheSignal.hitRate >= 0.370d
                    && cacheSignal.risk <= 0.560d) {
                score += 4.0d;
            }

            if (cacheSignal.stateNumberSample >= 18
                    && cacheSignal.avgNumberSupport >= 0.370d
                    && cacheSignal.weakestNumberSupport >= 0.300d) {
                score += 3.0d;
            }
        }

        if (c.profileScore >= 0.620d) {
            score += 4.0d;
        } else if (c.profileScore >= 0.540d) {
            score += 2.0d;
        }

        if (c.promotionScore >= 0.620d) {
            score += 2.0d;
        }

        if (c.aPlusScore >= 0.620d) {
            score += 2.0d;
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if ("READY".equals(teacherMatrixSignal.status)) {
                score += 4.0d;
            }

            if ("STRONG".equals(teacherMatrixSignal.status)) {
                score += 6.0d;
            }

            if (teacherMatrixSignal.comboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                    && teacherMatrixSignal.comboHitRate >= 0.380d) {
                score += 3.0d;
            }

            if ("RISK".equals(teacherMatrixSignal.status)) {
                score -= 12.0d;
            }
        }

        score += v20StrengthBonus(c);

        if (trendTrace != null && trendTrace.hardBlock == 1) {
            score -= 30.0d;
        }

        if (recoveryResult != null && breakRecoveryEngine.isHardBlock(recoveryResult)) {
            score -= 25.0d;
        }

        return clamp(score, 0.0d, 100.0d);
    }

    private double riskScore(
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            CalibrationEngine.CalibrationSignal calibrationSignal,
            CacheGateSignal cacheSignal,
            TeacherMatrixGateSignal teacherMatrixSignal) {

        if (c == null) {
            return 100.0d;
        }

        double score = 0.0d;

        score += Math.max(0.0d, c.driftRisk - AppConfig.DRIFT_RISK_MAX) * 0.70d;
        score += Math.max(0.0d, c.breakDepth - AppConfig.BREAK_DEPTH_MAX) * 0.70d;
        score += Math.max(0.0d, c.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.35d;
        score += Math.max(0.0d, c.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) * 0.35d;
        score += c.evidenceRisk * 16.0d;
        score += c.auditPenalty * 10.0d;

        if (c.championGap < AppConfig.CHAMPION_GAP_MIN && c.finalP < 0.400d) {
            score += 4.0d;
        }

        if (c.comboGap < AppConfig.COMBO_GAP_MIN && c.finalP < 0.400d) {
            score += 4.0d;
        }

        if (trendTrace != null) {
            score += Math.max(0.0d, trendTrace.modeSwitchRisk - 45.0d) * 0.25d;
            score += Math.max(0.0d, trendTrace.earlyBreakRisk - 45.0d) * 0.25d;
        }

        if (recoveryResult != null && breakRecoveryEngine.isHardBlock(recoveryResult)) {
            score += 24.0d;
        }

        if (calibrationSignal != null
                && calibrationSignal.error > AppConfig.CALIBRATION_ERROR_MAX) {
            score += calibrationSignal.error * 45.0d;
        }

        if (cacheSignal != null) {
            if (cacheSignal.sample >= 10
                    && cacheSignal.hitRate > 0.0d
                    && cacheSignal.hitRate < 0.290d) {
                score += 8.0d;
            }

            if (cacheSignal.risk >= 0.700d) {
                score += 8.0d;
            }

            if (cacheSignal.weakestNumberSupport > 0.0d
                    && cacheSignal.weakestNumberSupport < 0.250d) {
                score += 6.0d;
            }
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if ("RISK".equals(teacherMatrixSignal.status)) {
                score += 14.0d;
            }

            if (teacherMatrixSignal.sample >= TEACHER_MATRIX_STRONG_SAMPLE
                    && teacherMatrixSignal.hitRate > 0.0d
                    && teacherMatrixSignal.hitRate < 0.290d
                    && teacherMatrixSignal.confidence >= 0.460d) {
                score += 10.0d;
            }

            if (teacherMatrixSignal.riskPenalty >= 0.014d) {
                score += 7.0d;
            }

            if (teacherMatrixSignal.statusStrong()
                    && teacherMatrixSignal.riskPenalty <= 0.006d
                    && teacherMatrixSignal.hitRate >= 0.370d) {
                score -= 3.0d;
            }
        }

        score += v20RiskPenaltyScore(c);

        if (score < 0.0d) {
            score = 0.0d;
        }

        return clamp(score, 0.0d, 100.0d);
    }

    private double sampleScore(
            Models.ComboCandidate c,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CacheGateSignal cacheSignal,
            TeacherMatrixGateSignal teacherMatrixSignal) {

        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (c.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            score += 30.0d;
        } else if (c.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_MIN) {
            score += 20.0d;
        } else if (c.effectiveSample >= 100) {
            score += 10.0d;
        }

        if (c.comboSample >= AppConfig.COMBO_SAMPLE_HIGH) {
            score += 24.0d;
        } else if (c.comboSample >= AppConfig.COMBO_SAMPLE_MIN) {
            score += 16.0d;
        } else if (c.comboSample >= 90) {
            score += 8.0d;
        }

        if (similarResult != null
                && similarResult.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_MIN) {
            score += 14.0d;
        }

        if (memorySignal != null && memorySignal.usable == 1) {
            score += 14.0d;
        }

        if (c.deepProfileSample >= PROFILE_SAMPLE_MIN) {
            score += 8.0d;
        }

        if (cacheSignal != null) {
            if (cacheSignal.sample >= 10) {
                score += 8.0d;
            }

            if (cacheSignal.stateNumberSample >= 12) {
                score += 6.0d;
            }
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if (teacherMatrixSignal.sample >= TEACHER_MATRIX_MIN_SAMPLE) {
                score += 5.0d;
            }

            if (teacherMatrixSignal.comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE) {
                score += 4.0d;
            }

            if (teacherMatrixSignal.statusStrong()) {
                score += 3.0d;
            }
        }

        score += v20SampleBonus(c);

        return clamp(score, 0.0d, 100.0d);
    }

    private V20GateEvidence buildV20GateEvidence(
            Models.ComboCandidate c, List<Models.Draw> history, GateBatchContext ctx) {
        V20GateEvidence e = new V20GateEvidence();
        if (c == null || !c.isValidK3()) {
            e.stateKey = "DG_INVALID";
            e.actionAdvice = "HOLD：候选非法";
            e.counterEvidenceText = "候选为空或K3非法";
            e.hardRisk = 1;
            return e;
        }

        e.position = c.position;
        e.k3Mask = c.k3Mask;
        e.baseIssue = "";
        e.targetIssue = "";

        if (history == null || history.size() == 0) {
            e.stateKey = buildV20GateFallbackStateKey(c);
            e.evidenceContribution =
                    readDouble(
                            c,
                            "probabilityEvidenceContribution",
                            readDouble(c, "dynamicCauseScore", 0.0d));
            e.counterContribution =
                    readDouble(
                            c,
                            "probabilityCounterEvidence",
                            readDouble(c, "dynamicRiskScore", 0.0d));
            e.sampleReliability =
                    readDouble(
                            c, "probabilitySampleReliability", sampleReliabilityFromCandidate(c));
            e.recentValidity =
                    readDouble(c, "probabilityRecentValidity", recentValidityFromCandidate(c));
            e.featureDistance =
                    readDouble(c, "probabilityFeatureDistance", 1.0d - e.sampleReliability);
            e.actionAdvice = buildV20GateAdvice(e, c);
            e.evidenceText = "无历史窗口输入，读取概率/因链/教师矩阵预写字段作为门槛证据";
            e.counterEvidenceText = "历史窗口缺失，不能放大低频救援，门槛保持保守";
            e.summary = buildV20GateEvidenceSummary(e);
            return e;
        }

        int total = 0;
        int hit = 0;
        int recentTotal = 0;
        int recentHit = 0;
        int gap = 999;
        int lastActual = 0;
        int odd = 0;
        int even = 0;
        int low = 0;
        int mid = 0;
        int high = 0;
        int neighbor = 0;
        int sameTail = 0;
        int repeatHit = 0;
        int crowded = 0;
        int sync = 0;
        int lastHitAge = 999;
        double density12 = windowHitRate(c, history, V20_GATE_WINDOW_12);
        double density24 = windowHitRate(c, history, V20_GATE_WINDOW_24);
        double density48 = windowHitRate(c, history, V20_GATE_WINDOW_48);
        double density96 = windowHitRate(c, history, V20_GATE_WINDOW_96);
        double density360 = windowHitRate(c, history, V20_GATE_WINDOW_360);

        int start = history.size() - V20_GATE_WINDOW_360;
        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = getDrawNumberSafe(d, c.position);
            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            int age = history.size() - 1 - i;
            total++;
            lastActual = n;

            if (age < V20_GATE_WINDOW_24) {
                recentTotal++;
            }

            if ((n % 2) == 0) {
                even++;
            } else {
                odd++;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (containsK3Number(c, n)) {
                hit++;
                if (age < V20_GATE_WINDOW_24) {
                    recentHit++;
                }
                if (lastHitAge != 999 && lastHitAge - age <= 2) {
                    repeatHit++;
                }
                lastHitAge = age;
                if (age < gap) {
                    gap = age;
                }
            } else {
                if (isNeighborOfK3(c, n)) {
                    neighbor++;
                }
                if (isSameTailOfK3(c, n)) {
                    sameTail++;
                }
            }

            if (age < V20_GATE_WINDOW_24) {
                if (positionCrowded(d, c.position)) {
                    crowded++;
                }
                if (crossPositionSync(d, c.position, c)) {
                    sync++;
                }
            }
        }

        double hitRate = total <= 0 ? 0.0d : ((double) hit) / ((double) total);
        double recentRate =
                recentTotal <= 0 ? hitRate : ((double) recentHit) / ((double) recentTotal);
        double slope = density12 - density96;
        double mediumSlope = density24 - density360;
        double omissionScore = gap >= 999 ? 0.50d : clamp(((double) gap) / 48.0d, 0.0d, 1.0d);
        double densityScore = clamp(density24 * 1.25d + density48 * 0.35d, 0.0d, 1.0d);
        double repeatScore = clamp(((double) repeatHit) / 4.0d, 0.0d, 1.0d);
        double parityBias =
                total <= 0 ? 0.0d : Math.abs(((double) odd) - ((double) even)) / ((double) total);
        double zoneBias =
                total <= 0
                        ? 0.0d
                        : (max3(low, mid, high) - min3(low, mid, high))
                                / Math.max(1.0d, (double) total);
        double neighborScore =
                total <= 0
                        ? 0.0d
                        : clamp(((double) neighbor) / Math.max(1.0d, (double) total), 0.0d, 1.0d);
        double sameTailScore =
                total <= 0
                        ? 0.0d
                        : clamp(((double) sameTail) / Math.max(1.0d, (double) total), 0.0d, 1.0d);
        double crowdedScore =
                recentTotal <= 0
                        ? 0.0d
                        : clamp(
                                ((double) crowded) / Math.max(1.0d, (double) recentTotal),
                                0.0d,
                                1.0d);
        double syncScore =
                recentTotal <= 0
                        ? 0.0d
                        : clamp(((double) sync) / Math.max(1.0d, (double) recentTotal), 0.0d, 1.0d);
        double timeShift = clamp(Math.abs(slope - mediumSlope) * 2.0d, 0.0d, 1.0d);

        e.sample = total;
        e.recentSample = recentTotal;
        e.hitRate = hitRate;
        e.recentHitRate = recentRate;
        e.hotColdSlope = slope;
        e.omissionScore = omissionScore;
        e.densityScore = densityScore;
        e.repeatScore = repeatScore;
        e.parityBias = parityBias;
        e.zoneBias = zoneBias;
        e.neighborScore = neighborScore;
        e.sameTailScore = sameTailScore;
        e.crowdedScore = crowdedScore;
        e.syncScore = syncScore;
        e.timingShift = timeShift;
        e.sampleReliability = clamp(((double) total) / 180.0d, 0.0d, 1.0d);
        e.recentValidity =
                clamp(
                        recentRate * 0.56d
                                + density24 * 0.26d
                                + Math.max(0.0d, slope) * 1.60d
                                + e.sampleReliability * 0.18d,
                        0.0d,
                        1.0d);
        e.featureDistance =
                clamp(
                        Math.abs(density12 - density24) * 0.55d
                                + Math.abs(density24 - density48) * 0.25d
                                + Math.abs(density48 - density96) * 0.12d
                                + timeShift * 0.08d,
                        0.0d,
                        1.0d);

        double teacherScore =
                readDouble(c, "teacherMatrixScore", readDouble(c, "teacherNumberScore", 0.0d));
        double teacherRisk =
                readDouble(c, "teacherMatrixRisk", readDouble(c, "teacherNumberRisk", 0.0d));
        double dynamicScore = readDouble(c, "dynamicCauseScore", 0.0d);
        double dynamicRisk = readDouble(c, "dynamicRiskScore", readDouble(c, "v20RiskScore", 0.0d));
        double probabilityEvidence = readDouble(c, "probabilityEvidenceContribution", 0.0d);
        double probabilityCounter = readDouble(c, "probabilityCounterEvidence", 0.0d);

        e.evidenceContribution =
                clamp(
                        densityScore * 0.25d
                                + e.recentValidity * 0.22d
                                + Math.max(0.0d, slope) * 1.35d
                                + teacherScore * 0.14d
                                + dynamicScore * 0.15d
                                + probabilityEvidence * 0.12d
                                + e.sampleReliability * 0.12d,
                        0.0d,
                        1.0d);

        e.counterContribution =
                clamp(
                        Math.max(0.0d, -slope) * 1.30d
                                + teacherRisk * 0.16d
                                + dynamicRisk * 0.18d
                                + probabilityCounter * 0.15d
                                + crowdedScore * 0.10d
                                + syncScore * 0.08d
                                + timeShift * 0.10d
                                + (1.0d - e.sampleReliability) * 0.12d,
                        0.0d,
                        1.0d);

        e.stateKey = buildV20GateStateKey(c, e, gap, lastActual);
        e.evidenceText =
                buildV20GateEvidenceText(
                        c, e, density12, density24, density48, density96, density360);
        e.counterEvidenceText = buildV20GateCounterText(c, e, crowdedScore, syncScore, timeShift);
        e.actionAdvice = buildV20GateAdvice(e, c);
        e.hardRisk =
                e.counterContribution >= V20_GATE_HARD_COUNTER_LINE && c.finalP < 0.420d ? 1 : 0;
        e.summary = buildV20GateEvidenceSummary(e);

        return e;
    }

    private V20GateEvidence readV20GateEvidenceFast(Models.ComboCandidate c) {
        V20GateEvidence e = new V20GateEvidence();
        if (c == null) {
            return e;
        }

        e.position = c.position;
        e.k3Mask = c.k3Mask;
        e.stateKey =
                readString(
                        c,
                        "dynamicGateWindowStateKey",
                        readString(
                                c,
                                "probabilityWindowStateKey",
                                readString(c, "windowStateKey", "")));
        e.evidenceContribution =
                readDouble(
                        c,
                        "dynamicGateEvidenceContribution",
                        readDouble(c, "probabilityEvidenceContribution", 0.0d));
        e.counterContribution =
                readDouble(
                        c,
                        "dynamicGateCounterContribution",
                        readDouble(c, "probabilityCounterEvidence", 0.0d));
        e.sampleReliability =
                readDouble(
                        c,
                        "dynamicGateSampleReliability",
                        readDouble(
                                c,
                                "probabilitySampleReliability",
                                sampleReliabilityFromCandidate(c)));
        e.recentValidity =
                readDouble(
                        c,
                        "dynamicGateRecentValidity",
                        readDouble(c, "probabilityRecentValidity", recentValidityFromCandidate(c)));
        e.featureDistance =
                readDouble(
                        c,
                        "dynamicGateFeatureDistance",
                        readDouble(c, "probabilityFeatureDistance", 1.0d - e.sampleReliability));
        e.actionAdvice =
                readString(
                        c, "dynamicGateActionAdvice", readString(c, "probabilityActionAdvice", ""));
        e.evidenceText =
                readString(
                        c, "dynamicGateEvidenceText", readString(c, "probabilityEvidenceText", ""));
        e.counterEvidenceText =
                readString(
                        c,
                        "dynamicGateCounterEvidenceText",
                        readString(c, "probabilityCounterEvidenceText", ""));
        e.summary = readString(c, "dynamicGateAuditSummary", "");
        if (e.summary.length() == 0) {
            e.summary = buildV20GateEvidenceSummary(e);
        }
        return e;
    }

    private double v20WindowStateAdjustment(Models.ComboCandidate c, V20GateEvidence e) {
        if (c == null || e == null) {
            return 0.0d;
        }

        double diff = e.counterContribution - e.evidenceContribution;
        double adj = diff * 0.016d;

        if (e.evidenceContribution >= V20_GATE_STRONG_EVIDENCE_LINE
                && e.counterContribution <= 0.460d
                && e.sampleReliability >= 0.360d
                && e.recentValidity >= 0.360d
                && e.featureDistance <= 0.420d
                && lowRisk(c)) {
            adj -= 0.004d;
        }

        if (e.counterContribution >= 0.620d && c.finalP < 0.410d) {
            adj += 0.006d;
        }

        if (e.featureDistance >= 0.620d && c.finalP < 0.420d) {
            adj += 0.004d;
        }

        if (e.sampleReliability < 0.240d && c.finalP < 0.400d) {
            adj += 0.003d;
        }

        return adj;
    }

    private double v20ActionAdviceAdjustment(Models.ComboCandidate c, V20GateEvidence e) {
        if (c == null || e == null || e.actionAdvice == null) {
            return 0.0d;
        }

        String a = e.actionAdvice;
        if (a.indexOf("HARD_HOLD") >= 0 || a.indexOf("RISK_HOLD") >= 0) {
            return 0.010d;
        }
        if (a.indexOf("SHADOW") >= 0 || a.indexOf("OBSERVE") >= 0) {
            return 0.004d;
        }
        if (a.indexOf("BET_READY") >= 0 && lowRisk(c) && c.finalP >= 0.390d) {
            return -0.004d;
        }
        if (a.indexOf("STRONG_WATCH") >= 0 && lowRisk(c)) {
            return -0.002d;
        }
        return 0.0d;
    }

    private void applyV20GateEvidenceToCandidate(Models.ComboCandidate c, V20GateEvidence e) {
        if (c == null || e == null) {
            return;
        }

        writeString(c, "dynamicGateWindowStateKey", e.stateKey);
        if (readString(c, "windowStateKey", "").length() == 0) {
            writeString(c, "windowStateKey", e.stateKey);
        }
        if (readString(c, "v20WindowStateKey", "").length() == 0) {
            writeString(c, "v20WindowStateKey", e.stateKey);
        }
        if (readString(c, "dynamicWindowStateKey", "").length() == 0) {
            writeString(c, "dynamicWindowStateKey", e.stateKey);
        }
        if (readString(c, "probabilityWindowStateKey", "").length() == 0) {
            writeString(c, "probabilityWindowStateKey", e.stateKey);
        }
        if (readString(c, "finalDecisionWindowStateKey", "").length() == 0) {
            writeString(c, "finalDecisionWindowStateKey", e.stateKey);
        }
        if (readString(c, "v20Status", readString(c, "dynamicStatus", "")).length() == 0) {
            writeString(c, "v20Status", e.hardRisk == 1 ? "V20_RISK" : "V20_WATCH");
            writeString(c, "dynamicStatus", e.hardRisk == 1 ? "V20_RISK" : "V20_WATCH");
            writeString(c, "dynamicCauseStatus", e.hardRisk == 1 ? "V20_RISK" : "V20_WATCH");
        }
        writeString(c, "dynamicGateEvidenceText", e.evidenceText);
        writeString(c, "dynamicGateCounterEvidenceText", e.counterEvidenceText);
        writeString(c, "dynamicGateActionAdvice", e.actionAdvice);
        writeString(c, "dynamicGateAuditSummary", e.summary);
        writeDouble(c, "dynamicGateEvidenceContribution", e.evidenceContribution);
        writeDouble(c, "dynamicGateCounterContribution", e.counterContribution);
        writeDouble(c, "dynamicGateSampleReliability", e.sampleReliability);
        writeDouble(c, "dynamicGateRecentValidity", e.recentValidity);
        writeDouble(c, "dynamicGateFeatureDistance", e.featureDistance);

        writeDouble(c, "dynamicGateQualityEvidence", e.evidenceContribution);
        writeDouble(c, "dynamicGateQualityCounter", e.counterContribution);
        writeDouble(c, "v20SupportScore", readDouble(c, "v20SupportScore", e.evidenceContribution));
        writeDouble(c, "v20RiskScore", readDouble(c, "v20RiskScore", e.counterContribution));
        writeDouble(c, "v20SampleReliability", e.sampleReliability);
        writeDouble(c, "v20RecentValidity", e.recentValidity);

        if (readString(c, "windowStateKey", "").length() == 0) {
            writeString(c, "windowStateKey", e.stateKey);
        }
    }

    private void applyV20GateEvidenceToSignal(GateSignal signal, V20GateEvidence e) {
        if (signal == null || e == null) {
            return;
        }

        signal.v20WindowStateKey = e.stateKey;
        signal.v20EvidenceText = e.evidenceText;
        signal.v20CounterEvidenceText = e.counterEvidenceText;
        signal.v20ActionAdvice = e.actionAdvice;
        signal.v20AuditSummary = e.summary;
        signal.v20EvidenceContribution = e.evidenceContribution;
        signal.v20CounterContribution = e.counterContribution;
        signal.v20SampleReliability = e.sampleReliability;
        signal.v20RecentValidity = e.recentValidity;
        signal.v20FeatureDistance = e.featureDistance;
    }

    private double windowHitRate(Models.ComboCandidate c, List<Models.Draw> history, int window) {
        if (c == null || history == null || history.size() == 0 || window <= 0) {
            return 0.0d;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }
            int n = getDrawNumberSafe(d, c.position);
            if (!AppConfig.isValidNumber(n)) {
                continue;
            }
            total++;
            if (containsK3Number(c, n)) {
                hit++;
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        return clamp(((double) hit) / ((double) total), 0.0d, 1.0d);
    }

    private int getDrawNumberSafe(Models.Draw d, int position) {
        if (d == null || !AppConfig.isValidPosition(position)) {
            return 0;
        }

        try {
            return d.getNumberAtPosition(position);
        } catch (Throwable e) {
        }

        return 0;
    }

    private boolean containsK3Number(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }

        return c.n1 == n || c.n2 == n || c.n3 == n;
    }

    private boolean isNeighborOfK3(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }

        return Math.abs(c.n1 - n) == 1 || Math.abs(c.n2 - n) == 1 || Math.abs(c.n3 - n) == 1;
    }

    private boolean isSameTailOfK3(Models.ComboCandidate c, int n) {
        if (c == null || !AppConfig.isValidNumber(n)) {
            return false;
        }

        int tail = n % 10;
        return c.n1 % 10 == tail || c.n2 % 10 == tail || c.n3 % 10 == tail;
    }

    private boolean positionCrowded(Models.Draw d, int position) {
        if (d == null || !AppConfig.isValidPosition(position)) {
            return false;
        }

        int n = getDrawNumberSafe(d, position);
        if (!AppConfig.isValidNumber(n)) {
            return false;
        }

        int near = 0;
        for (int p = 1; p <= 10; p++) {
            if (p == position) {
                continue;
            }
            int other = getDrawNumberSafe(d, p);
            if (AppConfig.isValidNumber(other) && Math.abs(other - n) <= 1) {
                near++;
            }
        }

        return near >= 3;
    }

    private boolean crossPositionSync(Models.Draw d, int position, Models.ComboCandidate c) {
        if (d == null || c == null || !AppConfig.isValidPosition(position)) {
            return false;
        }

        int sync = 0;
        for (int p = 1; p <= 10; p++) {
            if (p == position) {
                continue;
            }
            int other = getDrawNumberSafe(d, p);
            if (containsK3Number(c, other) || isNeighborOfK3(c, other)) {
                sync++;
            }
        }

        return sync >= 4;
    }

    private String buildV20GateStateKey(
            Models.ComboCandidate c, V20GateEvidence e, int gap, int lastActual) {
        StringBuilder sb = new StringBuilder();
        sb.append("DG_P");
        sb.append(c == null ? 0 : c.position);
        sb.append("_W12");
        sb.append(bucketRate("D", windowBucket(e.densityScore)));
        sb.append("_S");
        sb.append(bucketSignedValue(e.hotColdSlope));
        sb.append("_G");
        sb.append(bucketGap(gap));
        sb.append("_REP");
        sb.append(windowBucket(e.repeatScore));
        sb.append("_PAR");
        sb.append(windowBucket(e.parityBias));
        sb.append("_ZONE");
        sb.append(windowBucket(e.zoneBias));
        sb.append("_NEI");
        sb.append(windowBucket(e.neighborScore));
        sb.append("_TAIL");
        sb.append(windowBucket(e.sameTailScore));
        sb.append("_CROWD");
        sb.append(windowBucket(e.crowdedScore));
        sb.append("_SYNC");
        sb.append(windowBucket(e.syncScore));
        sb.append("_SHIFT");
        sb.append(windowBucket(e.timingShift));
        sb.append("_A");
        sb.append(lastActual);
        return sb.toString();
    }

    private String buildV20GateFallbackStateKey(Models.ComboCandidate c) {
        String key =
                readString(c, "probabilityWindowStateKey", readString(c, "windowStateKey", ""));
        if (key != null && key.length() > 0) {
            return "DG_FALLBACK_" + cleanKeyPart(key);
        }
        return "DG_FALLBACK_P" + (c == null ? 0 : c.position) + "_K" + (c == null ? 0 : c.k3Mask);
    }

    private String buildV20GateEvidenceText(
            Models.ComboCandidate c,
            V20GateEvidence e,
            double d12,
            double d24,
            double d48,
            double d96,
            double d360) {
        return "动态门槛证据：12/24/48/96/360窗="
                + UiFormatter.probability(d12)
                + "/"
                + UiFormatter.probability(d24)
                + "/"
                + UiFormatter.probability(d48)
                + "/"
                + UiFormatter.probability(d96)
                + "/"
                + UiFormatter.probability(d360)
                + "，热冷斜率="
                + UiFormatter.signedProbability(e.hotColdSlope)
                + "，密集="
                + UiFormatter.probability(e.densityScore)
                + "，样本可信="
                + UiFormatter.probability(e.sampleReliability)
                + "，近期有效="
                + UiFormatter.probability(e.recentValidity)
                + "，说明=证据只软调门槛，不直接BET";
    }

    private String buildV20GateCounterText(
            Models.ComboCandidate c,
            V20GateEvidence e,
            double crowdedScore,
            double syncScore,
            double timeShift) {
        return "动态门槛反证：反证="
                + UiFormatter.probability(e.counterContribution)
                + "，窗口特征距离="
                + UiFormatter.probability(e.featureDistance)
                + "，位置拥挤="
                + UiFormatter.probability(crowdedScore)
                + "，跨位置同步="
                + UiFormatter.probability(syncScore)
                + "，时机偏移="
                + UiFormatter.probability(timeShift)
                + "，说明=反证强时只抬门槛或转最终门HOLD";
    }

    private String buildV20GateAdvice(V20GateEvidence e, Models.ComboCandidate c) {
        if (e == null) {
            return "OBSERVE：无窗口证据";
        }
        if (e.hardRisk == 1 || e.counterContribution >= V20_GATE_HARD_COUNTER_LINE) {
            return "HARD_HOLD：反证过强，交由最终门禁止出手";
        }
        if (e.counterContribution >= 0.620d && (c == null || c.finalP < 0.410d)) {
            return "RISK_HOLD：反证强于证据，动态门槛收紧";
        }
        if (e.evidenceContribution >= V20_GATE_STRONG_EVIDENCE_LINE
                && e.counterContribution <= 0.460d
                && e.sampleReliability >= 0.360d
                && e.recentValidity >= 0.360d) {
            return "BET_READY：证据大于反证，可进入最终DecisionGate判断";
        }
        if (e.evidenceContribution >= 0.540d && e.counterContribution <= 0.560d) {
            return "STRONG_WATCH：有支撑但仍需最终门确认";
        }
        if (e.sampleReliability < 0.240d) {
            return "SHADOW_OBSERVE：样本可信不足，不救援真实BET";
        }
        return "OBSERVE：证据中性，保持原动态门槛";
    }

    private String buildV20GateEvidenceSummary(V20GateEvidence e) {
        if (e == null) {
            return "";
        }
        return "V20门槛窗口状态="
                + e.stateKey
                + "｜证据="
                + UiFormatter.probability(e.evidenceContribution)
                + "｜反证="
                + UiFormatter.probability(e.counterContribution)
                + "｜样本可信="
                + UiFormatter.probability(e.sampleReliability)
                + "｜近期有效="
                + UiFormatter.probability(e.recentValidity)
                + "｜距离="
                + UiFormatter.probability(e.featureDistance)
                + "｜动作="
                + e.actionAdvice;
    }

    private double sampleReliabilityFromCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }
        int sample = Math.max(c.effectiveSample, c.comboSample);
        sample = Math.max(sample, readInt(c, "teacherMatrixSample", 0));
        sample = Math.max(sample, readInt(c, "runtimeProfileSample", 0));
        return clamp(((double) sample) / 180.0d, 0.0d, 1.0d);
    }

    private double recentValidityFromCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }
        double v = 0.0d;
        v += readDouble(c, "probabilityRecentValidity", 0.0d) * 0.36d;
        v += readDouble(c, "dynamicCauseScore", 0.0d) * 0.20d;
        v += readDouble(c, "teacherMatrixScore", 0.0d) * 0.18d;
        v += c.similarHitRate * 0.14d;
        v += c.comboHitRate * 0.12d;
        return clamp(v, 0.0d, 1.0d);
    }

    private int max3(int a, int b, int c) {
        return Math.max(a, Math.max(b, c));
    }

    private int min3(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    private int windowBucket(double v) {
        if (v < 0.18d) {
            return 0;
        }
        if (v < 0.32d) {
            return 1;
        }
        if (v < 0.48d) {
            return 2;
        }
        if (v < 0.64d) {
            return 3;
        }
        return 4;
    }

    private String bucketRate(String prefix, int bucket) {
        return prefix + String.valueOf(bucket);
    }

    private String bucketSignedValue(double v) {
        if (v >= 0.080d) {
            return "UP2";
        }
        if (v >= 0.030d) {
            return "UP1";
        }
        if (v <= -0.080d) {
            return "DN2";
        }
        if (v <= -0.030d) {
            return "DN1";
        }
        return "FLAT";
    }

    private String bucketGap(int gap) {
        if (gap >= 999) {
            return "NONE";
        }
        if (gap <= 2) {
            return "G0";
        }
        if (gap <= 6) {
            return "G1";
        }
        if (gap <= 12) {
            return "G2";
        }
        if (gap <= 24) {
            return "G3";
        }
        return "G4";
    }

    private double v20StrengthBonus(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        double dynamic = readDouble(c, "dynamicCauseScore", 0.0d);
        double dynamicRisk = readDouble(c, "dynamicRiskScore", 0.0d);
        double dynamicConf = readDouble(c, "dynamicConfidence", 0.0d);
        int dynamicPass = readInt(c, "dynamicPass", 0);

        if (dynamicPass == 1 && dynamic >= 0.600d && dynamicRisk <= 0.520d) {
            score += 6.0d;
        }

        if (dynamic >= 0.700d && dynamicConf >= 0.500d) {
            score += 4.0d;
        }

        if (readDouble(c, "ruleHealthScore", 0.0d) >= 0.620d
                && readDouble(c, "ruleHealthRisk", 0.0d) <= 0.460d) {
            score += 4.0d;
        }

        if (readDouble(c, "thirdGuardScore", 0.0d) >= 0.620d
                && readDouble(c, "thirdGuardRisk", 0.0d) <= 0.460d) {
            score += 4.0d;
        }

        if (readDouble(c, "shadowHealthScore", 0.0d) >= 0.620d
                && readDouble(c, "shadowHealthRisk", 0.0d) <= 0.460d) {
            score += 3.0d;
        }

        if (readDouble(c, "teacherNumberScore", 0.0d) >= 0.600d
                && readDouble(c, "teacherNumberRisk", 0.0d) <= 0.460d) {
            score += 4.0d;
        }

        if (readDouble(c, "counterfactualScore", 0.0d) >= 0.620d
                && readDouble(c, "counterfactualRisk", 0.0d) <= 0.460d) {
            score += 3.0d;
        }

        if (readDouble(c, "positionHealthScore", 0.0d) >= 0.620d
                && readDouble(c, "positionWrongRisk", 0.0d) <= 0.460d) {
            score += 3.0d;
        }

        if (readDouble(c, "seedExpandScore", readDouble(c, "seedScore", 0.0d)) >= 0.600d
                && readDouble(c, "seedRisk", 0.0d) <= 0.460d) {
            score += 2.0d;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1
                || readInt(c, "falseStrongHardFreeze", 0) == 1) {
            score -= 14.0d;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1) {
            score -= 12.0d;
        }

        return score;
    }

    private double v20RiskPenaltyScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += readDouble(c, "dynamicRiskScore", 0.0d) * 10.0d;
        score += readDouble(c, "ruleHealthRisk", 0.0d) * 9.0d;
        score += readDouble(c, "thirdGuardRisk", 0.0d) * 10.0d;
        score += readDouble(c, "shadowHealthRisk", 0.0d) * 8.0d;
        score +=
                readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d))
                        * 14.0d;
        score += readDouble(c, "counterfactualRisk", 0.0d) * 8.0d;
        score += readDouble(c, "teacherNumberRisk", 0.0d) * 7.0d;
        score += readDouble(c, "positionWrongRisk", 0.0d) * 7.0d;
        score += readDouble(c, "seedRisk", 0.0d) * 5.0d;

        if (readInt(c, "dynamicHardRisk", 0) == 1) {
            score += 18.0d;
        }

        if (readInt(c, "ruleHealthHardCount", 0) > 0) {
            score += 16.0d;
        }

        if (readInt(c, "thirdGuardHardBlock", 0) == 1) {
            score += 18.0d;
        }

        if (readInt(c, "shadowHardBlock", 0) == 1) {
            score += 14.0d;
        }

        if (readInt(c, "falseStrongHardFreeze", 0) == 1) {
            score += 22.0d;
        }

        return score;
    }

    private double v20SampleBonus(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        int teacherSample = readInt(c, "teacherSample", readInt(c, "teacherNumberSample", 0));
        int shadowSample = readInt(c, "shadowSample", 0);
        int chainSample = readInt(c, "chainSample", 0);
        int ruleCount = readInt(c, "ruleHealthRuleCount", readInt(c, "ruleCount", 0));

        if (teacherSample >= 30) {
            score += 4.0d;
        }

        if (teacherSample >= 80) {
            score += 3.0d;
        }

        if (shadowSample >= readWarmupIssues()) {
            score += 4.0d;
        }

        if (chainSample >= 12) {
            score += 3.0d;
        }

        if (ruleCount >= 6) {
            score += 3.0d;
        }

        return score;
    }

    private double applyOriginalMainlineCap(
            double gate,
            Models.ComboCandidate c,
            GateSignal signal,
            TeacherMatrixGateSignal teacherMatrixSignal) {

        if (c == null || signal == null) {
            return gate;
        }

        boolean hardRisk =
                c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK - 3.0d
                        || c.breakDepth >= breakDepthHardBlock() - 3.0d
                        || c.evidenceRisk >= 0.780d
                        || (teacherMatrixSignal != null
                                && "RISK".equals(teacherMatrixSignal.status))
                        || readInt(c, "dynamicHardRisk", 0) == 1
                        || readInt(c, "falseStrongHardFreeze", 0) == 1
                        || readInt(c, "thirdGuardHardBlock", 0) == 1;

        if (hardRisk) {
            if (gate > ORIGINAL_HARD_RISK_CAP) {
                gate = ORIGINAL_HARD_RISK_CAP;
            }
            return gate;
        }

        boolean mediumRisk =
                c.driftRisk > AppConfig.DRIFT_RISK_MAX
                        || c.breakDepth > AppConfig.BREAK_DEPTH_MAX
                        || c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX
                        || c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX
                        || c.evidenceRisk >= 0.660d
                        || signal.riskScore >= 55.0d
                        || readDouble(c, "dynamicRiskScore", 0.0d) >= 0.660d
                        || readDouble(c, "ruleHealthRisk", 0.0d) >= 0.660d
                        || readDouble(c, "falseStrongFreezeRisk", 0.0d) >= 0.660d;

        if (mediumRisk) {
            if (gate > ORIGINAL_RISK_CAP) {
                gate = ORIGINAL_RISK_CAP;
            }
            return gate;
        }

        boolean strongShape =
                (AppConfig.MODE_CONFIRMED.equals(c.stateMode)
                                || AppConfig.MODE_ACCELERATING.equals(c.stateMode)
                                || "STABLE".equals(c.stateMode))
                        && c.trendLife >= trendLifeMin()
                        && lowRisk(c)
                        && c.effectiveSample >= 120;

        boolean teacherStrong =
                teacherMatrixSignal != null
                        && teacherMatrixSignal.statusStrong()
                        && teacherMatrixSignal.hitRate >= 0.370d
                        && teacherMatrixSignal.confidence >= 0.360d
                        && lowRisk(c);

        boolean profileStrong =
                (c.profileScore >= 0.620d
                                || c.fallbackProfileScore >= 0.620d
                                || c.aPlusScore >= 0.620d
                                || c.promotionScore >= 0.620d)
                        && lowRisk(c);

        boolean v20Strong =
                readInt(c, "dynamicPass", 0) == 1
                        && readDouble(c, "dynamicCauseScore", 0.0d) >= 0.620d
                        && readDouble(c, "dynamicRiskScore", 0.0d) <= 0.500d
                        && readDouble(c, "ruleHealthScore", 0.0d) >= 0.560d
                        && readDouble(c, "falseStrongFreezeRisk", 0.0d) <= 0.500d
                        && readInt(c, "dynamicHardRisk", 0) == 0
                        && readInt(c, "falseStrongHardFreeze", 0) == 0
                        && lowRisk(c);

        boolean veryStrong =
                (strongShape || teacherStrong || profileStrong || v20Strong)
                        && c.finalP >= 0.400d
                        && c.effectiveSample >= 160
                        && (c.comboHitRate >= 0.370d
                                || c.similarHitRate >= 0.370d
                                || teacherStrong
                                || profileStrong
                                || v20Strong)
                        && c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN;

        if (veryStrong) {
            if (gate > ORIGINAL_VERY_STRONG_CAP) {
                gate = ORIGINAL_VERY_STRONG_CAP;
            }
            return gate;
        }

        if (strongShape
                || teacherStrong
                || profileStrong
                || v20Strong
                || signal.strengthScore >= 72.0d) {
            if (gate > ORIGINAL_STRONG_CAP) {
                gate = ORIGINAL_STRONG_CAP;
            }
            return gate;
        }

        if (gate > ORIGINAL_NORMAL_CAP) {
            gate = ORIGINAL_NORMAL_CAP;
        }

        return gate;
    }

    private double applyHardFloorAndCeiling(
            double gate,
            Models.ComboCandidate candidate,
            GateSignal signal,
            TeacherMatrixGateSignal teacherMatrixSignal) {

        double minGate = readMinGate();

        double floor = NORMAL_SOFT_FLOOR;
        double cap = ORIGINAL_NORMAL_CAP;

        if (candidate != null) {
            boolean hardRisk =
                    candidate.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK - 3.0d
                            || candidate.breakDepth >= breakDepthHardBlock() - 3.0d
                            || candidate.evidenceRisk >= 0.780d
                            || (teacherMatrixSignal != null
                                    && "RISK".equals(teacherMatrixSignal.status))
                            || readInt(candidate, "dynamicHardRisk", 0) == 1
                            || readInt(candidate, "falseStrongHardFreeze", 0) == 1
                            || readInt(candidate, "thirdGuardHardBlock", 0) == 1;

            boolean mediumRisk =
                    candidate.driftRisk > AppConfig.DRIFT_RISK_MAX
                            || candidate.breakDepth > AppConfig.BREAK_DEPTH_MAX
                            || candidate.evidenceRisk >= 0.660d
                            || (signal != null && signal.riskScore >= 55.0d)
                            || readDouble(candidate, "dynamicRiskScore", 0.0d) >= 0.660d
                            || readDouble(candidate, "ruleHealthRisk", 0.0d) >= 0.660d
                            || readDouble(candidate, "falseStrongFreezeRisk", 0.0d) >= 0.660d;

            boolean veryStrong =
                    signal != null
                            && signal.strengthScore >= 78.0d
                            && signal.riskScore <= 42.0d
                            && signal.sampleScore >= 42.0d
                            && lowRisk(candidate);

            boolean strong =
                    signal != null
                            && signal.strengthScore >= 68.0d
                            && signal.riskScore <= 52.0d
                            && lowRisk(candidate);

            if (hardRisk) {
                cap = ORIGINAL_HARD_RISK_CAP;
                floor = ORIGINAL_BASE_GATE;
            } else if (mediumRisk) {
                cap = ORIGINAL_RISK_CAP;
                floor = ORIGINAL_BASE_GATE;
            } else if (veryStrong) {
                cap = ORIGINAL_VERY_STRONG_CAP;
                floor = VERY_STRONG_SOFT_FLOOR;
            } else if (strong) {
                cap = ORIGINAL_STRONG_CAP;
                floor = STRONG_SOFT_FLOOR;
            } else {
                cap = ORIGINAL_NORMAL_CAP;
                floor = NORMAL_SOFT_FLOOR;
            }
        }

        if (floor < minGate) {
            floor = minGate;
        }

        if (gate < floor) {
            gate = floor;
        }

        if (gate > cap) {
            gate = cap;
        }

        return AppConfig.clampProbability(gate);
    }

    private void applySoftEdgeRules(
            GateSignal signal,
            Models.ComboCandidate c,
            TeacherMatrixGateSignal teacherMatrixSignal) {
        if (signal == null || c == null) {
            return;
        }

        if (signal.allowedByGate == 0) {
            return;
        }

        if (readInt(c, "falseHighPFlag", 0) == 1 && c.finalP < 0.425d) {
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_GATE_FALSE_HIGH_P";
            return;
        }

        if (readDouble(c, "k3CoverageRisk", 0.0d) >= 0.70d && c.finalP < 0.425d) {
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_GATE_K3_COVERAGE_RISK";
            return;
        }

        if (readInt(c, "probabilityTeacherWeakOrEmpty", 0) == 1
                && readInt(c, "probabilityV20UnknownOrEmpty", 0) == 1
                && !isNoSampleWatchNeutralForDynamicGate(c)
                && !isPreferredEdgeBand(c, signal)
                && c.finalP < 0.415d) {
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_GATE_TEACHER_WEAK_V20_UNKNOWN";
            return;
        }

        if (signal.edge > EDGE_FALSE_STRONG_LINE
                && readInt(c, "probabilityStrongCauseFlag", 0) == 0
                && c.finalP < 0.430d
                && (readDouble(c, "k3CoverageRisk", 0.0d) >= K3_COVERAGE_RISK_LINE
                        || (readInt(c, "probabilityTeacherWeakOrEmpty", 0) == 1
                                && !isTeacherNoSampleNeutralForGate(c))
                        || (readInt(c, "probabilityV20UnknownOrEmpty", 0) == 1
                                && !isV20WatchOrEmptyNeutralForGate(c)))) {
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_GATE_EDGE_FALSE_STRONG";
            return;
        }

        boolean teacherSupport =
                teacherMatrixSignal != null
                        && teacherMatrixSignal.statusStrong()
                        && teacherMatrixSignal.hitRate >= 0.370d
                        && teacherMatrixSignal.confidence >= 0.360d;

        boolean profileSupport =
                c.profileScore >= 0.620d
                        || c.fallbackProfileScore >= 0.620d
                        || c.aPlusScore >= 0.620d
                        || c.promotionScore >= 0.620d;

        boolean v20Support =
                readInt(c, "dynamicPass", 0) == 1
                        && readDouble(c, "dynamicCauseScore", 0.0d) >= 0.600d
                        && readDouble(c, "dynamicRiskScore", 0.0d) <= 0.520d;

        if (signal.edge < EDGE_STRONG_MIN
                && signal.strengthScore < 55.0d
                && c.finalP < 0.385d
                && !teacherSupport
                && !profileSupport
                && !v20Support) {
            signal.allowedByGate = 0;
            signal.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (signal.sampleScore < 16.0d
                && c.finalP < 0.390d
                && c.profileScore < 0.620d
                && !teacherSupport
                && !v20Support) {
            signal.allowedByGate = 0;
            signal.blockCode = AppConfig.BLOCK_SAMPLE_WEAK;
            return;
        }

        if (signal.riskScore >= 75.0d && signal.edge < 0.014d && c.finalP < 0.420d) {
            signal.allowedByGate = 0;
            signal.blockCode = trendWeakBlockCode();
            return;
        }

        if (teacherMatrixSignal != null
                && "RISK".equals(teacherMatrixSignal.status)
                && c.finalP < 0.420d
                && signal.edge < 0.014d) {
            signal.allowedByGate = 0;
            signal.blockCode = teacherMatrixBlockCode();
        }
    }

    private void applyV20SoftRules(GateSignal signal, Models.ComboCandidate c) {
        if (signal == null || c == null) {
            return;
        }

        if (signal.allowedByGate == 0) {
            return;
        }

        double dynamicRisk = readDouble(c, "dynamicRiskScore", 0.0d);
        double ruleRisk = readDouble(c, "ruleHealthRisk", 0.0d);
        double guardRisk = readDouble(c, "thirdGuardRisk", 0.0d);
        double shadowRisk = readDouble(c, "shadowHealthRisk", 0.0d);
        double freezeRisk =
                readDouble(c, "falseStrongFreezeRisk", readDouble(c, "falseStrongRisk", 0.0d));
        double rankRisk = readDouble(c, "counterfactualRisk", 0.0d);
        double teacherRisk = readDouble(c, "teacherNumberRisk", 0.0d);

        double maxRisk =
                Math.max(
                        dynamicRisk,
                        Math.max(
                                ruleRisk,
                                Math.max(
                                        guardRisk,
                                        Math.max(
                                                shadowRisk,
                                                Math.max(
                                                        freezeRisk,
                                                        Math.max(rankRisk, teacherRisk))))));

        if (maxRisk >= 0.720d && c.finalP < 0.420d && signal.edge < 0.016d) {
            signal.allowedByGate = 0;
            signal.blockCode = "V20_CAUSE_RISK";
            return;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1 && c.finalP < 0.425d && signal.edge < 0.018d) {
            signal.allowedByGate = 0;
            signal.blockCode = "FALSE_STRONG_FREEZE";
            return;
        }

        if (readInt(c, "dynamicPass", 0) == 0
                && readDouble(c, "dynamicCauseScore", 0.0d) > 0.0d
                && readDouble(c, "dynamicCauseScore", 0.0d) < 0.440d
                && c.finalP < 0.410d
                && signal.edge < 0.012d) {
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_CAUSE_WEAK";
        }
    }

    private void applyHardBlocks(
            GateSignal signal,
            Models.ComboCandidate c,
            TrendStateMachine.TrendTrace trendTrace,
            BreakRecoveryEngine.RecoveryResult recoveryResult,
            Models.SimilarResult similarResult,
            ShapeMemoryEngine.MemorySignal memorySignal,
            CalibrationEngine.CalibrationSignal calibrationSignal,
            CacheGateSignal cacheSignal,
            TeacherMatrixGateSignal teacherMatrixSignal,
            GateBatchContext ctx) {

        if (signal == null) {
            return;
        }

        signal.hardBlock = 0;

        if (signal.blockCode == null) {
            signal.blockCode = "";
        }

        if (c == null || !c.isValidK3()) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_DATA_ERROR;
            return;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
            return;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_BREAK_HIGH;
            return;
        }

        if (isTerminalBadState(c.stateMode)) {
            if (c.finalP < 0.430d || c.evidenceRisk >= 0.560d) {
                signal.hardBlock = 1;
                signal.blockCode = trendWeakBlockCode();
                return;
            }
        }

        if (trendTrace != null && trendTrace.hardBlock == 1) {
            signal.hardBlock = 1;
            signal.blockCode = trendWeakBlockCode();
            return;
        }

        if (recoveryResult != null && breakRecoveryEngine.isHardBlock(recoveryResult)) {
            signal.hardBlock = 1;
            signal.blockCode =
                    recoveryResult.blockCode == null || recoveryResult.blockCode.length() == 0
                            ? AppConfig.BLOCK_RECOVERY_WEAK
                            : recoveryResult.blockCode;
            return;
        }

        if (similarResult != null
                && similarResult.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH
                && similarResult.similarHitRate < 0.285d
                && c.finalP < 0.415d
                && signal.strengthScore < 68.0d) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_SAMPLE_WEAK;
            return;
        }

        if (memorySignal != null
                && memorySignal.usable == 1
                && memorySignal.memoryScore < 28.0d
                && memorySignal.recentMemoryRate < 0.280d
                && c.finalP < 0.415d
                && signal.strengthScore < 68.0d) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            return;
        }

        if (calibrationSignal != null
                && calibrationSignal.error > AppConfig.CALIBRATION_ERROR_HARD
                && signal.edge < 0.010d
                && c.finalP < 0.415d
                && signal.strengthScore < 70.0d) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (cacheSignal != null) {
            if (cacheSignal.sample >= PROFILE_SAMPLE_STRONG
                    && cacheSignal.hitRate > 0.0d
                    && cacheSignal.hitRate < 0.280d
                    && c.finalP < 0.415d
                    && signal.strengthScore < 70.0d) {
                signal.hardBlock = 1;
                signal.blockCode = AppConfig.BLOCK_BACKTEST_WEAK;
                return;
            }

            if (cacheSignal.stateNumberSample >= 18
                    && cacheSignal.weakestNumberSupport > 0.0d
                    && cacheSignal.weakestNumberSupport < 0.240d
                    && c.finalP < 0.415d
                    && signal.strengthScore < 70.0d) {
                signal.hardBlock = 1;
                signal.blockCode = AppConfig.BLOCK_COMBO_WEAK;
                return;
            }
        }

        if (teacherMatrixSignal != null && teacherMatrixSignal.applied == 1) {
            if ("RISK".equals(teacherMatrixSignal.status)
                    && c.finalP < 0.420d
                    && signal.strengthScore < 70.0d) {
                signal.hardBlock = 1;
                signal.blockCode = teacherMatrixBlockCode();
                return;
            }

            if (teacherMatrixSignal.sample >= TEACHER_MATRIX_STRONG_SAMPLE
                    && teacherMatrixSignal.hitRate > 0.0d
                    && teacherMatrixSignal.hitRate < 0.280d
                    && teacherMatrixSignal.confidence >= 0.500d
                    && c.finalP < 0.420d
                    && signal.strengthScore < 70.0d) {
                signal.hardBlock = 1;
                signal.blockCode = teacherMatrixBlockCode();
                return;
            }
        }

        if (c.evidenceRisk >= 0.800d && c.finalP < 0.430d && signal.edge < 0.016d) {
            signal.hardBlock = 1;
            signal.blockCode = trendWeakBlockCode();
            return;
        }

        if (c.auditPenalty >= 0.560d && c.finalP < 0.430d && signal.edge < 0.016d) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
            return;
        }

        if (c.deepProfileSample >= PROFILE_SAMPLE_STRONG
                && c.deepProfileHitRate > 0.0d
                && c.deepProfileHitRate < 0.280d
                && c.finalP < 0.415d
                && signal.strengthScore < 70.0d) {
            signal.hardBlock = 1;
            signal.blockCode = AppConfig.BLOCK_BACKTEST_WEAK;
            return;
        }

        if (ctx != null) {
            if ("BLOCK".equals(ctx.auditStatus) || "ERROR".equals(ctx.auditStatus)) {
                if (c.finalP < 0.425d && signal.edge < 0.016d && signal.strengthScore < 70.0d) {
                    signal.hardBlock = 1;
                    signal.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
                }
            }
        }
    }

    private void applyV20HardBlocks(GateSignal signal, Models.ComboCandidate c) {
        if (signal == null || c == null) {
            return;
        }

        if (signal.hardBlock == 1) {
            return;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1 && c.finalP < 0.430d && signal.edge < 0.018d) {
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            signal.blockCode = "DYNAMIC_CAUSE_HARD_RISK";
            return;
        }

        if (readInt(c, "falseStrongHardFreeze", 0) == 1
                && c.finalP < 0.435d
                && signal.edge < 0.020d) {
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            signal.blockCode = "FALSE_STRONG_HARD_FREEZE";
            return;
        }

        if (readInt(c, "thirdGuardHardBlock", 0) == 1
                && c.finalP < 0.430d
                && signal.edge < 0.018d) {
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            signal.blockCode = "THIRD_GUARD_HARD_BLOCK";
            return;
        }

        if (readInt(c, "shadowHardBlock", 0) == 1 && c.finalP < 0.425d && signal.edge < 0.016d) {
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            signal.blockCode = "SHADOW_HEALTH_HARD_BLOCK";
            return;
        }

        if (readInt(c, "ruleHealthHardCount", 0) > 0
                && readDouble(c, "ruleHealthRisk", 0.0d) >= 0.720d
                && c.finalP < 0.430d
                && signal.edge < 0.018d) {
            signal.hardBlock = 1;
            signal.allowedByGate = 0;
            signal.blockCode = "RULE_HEALTH_HARD_RISK";
        }
    }

    private CacheGateSignal evaluateCacheGateSignalFast(Models.ComboCandidate c) {
        CacheGateSignal signal = new CacheGateSignal();

        if (c == null || !c.isValidK3()) {
            signal.summary = "缓存画像无效，中性处理";
            return signal;
        }

        signal.stateKey = buildRuntimeStateKey(c);

        if (c.deepProfileSample > 0) {
            signal.sample = c.deepProfileSample;
            signal.hitRate = AppConfig.clampProbability(c.deepProfileHitRate);
            signal.risk = AppConfig.clampProbability(c.evidenceRisk);
        }

        if (c.weakestNumberScore > 0.0d) {
            signal.stateNumberFound = 3;
            signal.stateNumberSample = c.comboSample > 0 ? c.comboSample : c.effectiveSample;
            signal.avgNumberSupport = AppConfig.clampProbability(c.balanceScore);
            signal.weakestNumberSupport = AppConfig.clampProbability(c.weakestNumberScore);
            signal.avgNumberRisk = AppConfig.clampProbability(c.evidenceRisk);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("缓存画像快照 state=");
        sb.append(shortKey(signal.stateKey));
        sb.append(" sample=");
        sb.append(signal.sample);
        sb.append(" HR=");
        sb.append(UiFormatter.probability(signal.hitRate));
        sb.append(" risk=");
        sb.append(UiFormatter.probability(signal.risk));
        sb.append(" numberFound=");
        sb.append(signal.stateNumberFound);
        sb.append(" numberSample=");
        sb.append(signal.stateNumberSample);
        sb.append(" numberAvg=");
        sb.append(UiFormatter.probability(signal.avgNumberSupport));
        sb.append(" weakest=");
        sb.append(UiFormatter.probability(signal.weakestNumberSupport));
        sb.append("；缓存画像参与风险与门槛微调，不绕过最终门");

        signal.summary = sb.toString();

        return signal;
    }

    private TeacherMatrixGateSignal evaluateTeacherMatrixGateSignalFast(
            Models.ComboCandidate c, CacheGateSignal cacheSignal) {
        TeacherMatrixGateSignal signal = new TeacherMatrixGateSignal();

        if (!readAppBoolean("TEACHER_MATRIX_ENABLE", true)) {
            signal.summary = "教师矩阵未启用，中性处理";
            return signal;
        }

        if (c == null || !c.isValidK3()) {
            signal.summary = "教师矩阵无效，中性处理";
            return signal;
        }

        String stateKey = buildRuntimeStateKey(c);
        String comboKey = buildTeacherComboKey(stateKey, c.position, c.k3Mask);

        int sample = readInt(c, "teacherMatrixSample", 0);
        int numberSample = readInt(c, "teacherMatrixStateSample", 0);

        if (numberSample <= 0) {
            numberSample = readInt(c, "teacherMatrixNumberSample", 0);
        }

        int comboSample = readInt(c, "teacherMatrixComboSample", 0);

        double hitRate = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double comboHitRate = readDouble(c, "teacherMatrixRecentHitRate", 0.0d);

        if (comboHitRate <= 0.0d) {
            comboHitRate = readDouble(c, "teacherMatrixComboHitRate", 0.0d);
        }

        double confidence = readDouble(c, "teacherMatrixConfidence", 0.0d);
        double gateAdjust = readDouble(c, "teacherMatrixGateAdjust", 0.0d);
        double risk = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);

        String status = readString(c, "teacherMatrixStatus", "");

        if (sample <= 0
                && numberSample <= 0
                && comboSample <= 0
                && hitRate <= 0.0d
                && comboHitRate <= 0.0d) {
            signal.stateKey = stateKey;
            signal.comboKey = comboKey;
            signal.status = STATUS_NO_SAMPLE_NEUTRAL;
            signal.summary = "教师矩阵候选字段为空，NO_SAMPLE中性处理：不硬杀、不救援BET";
            return signal;
        }

        int totalSample = sample;

        if (numberSample > totalSample) {
            totalSample = numberSample;
        }

        totalSample += comboSample;

        if (hitRate <= 0.0d) {
            hitRate = comboHitRate;
        }

        if (hitRate <= 0.0d) {
            hitRate = readAppDouble("RANDOM_K3_BASELINE", 0.300d);
        }

        if (confidence <= 0.0d) {
            confidence = clamp(totalSample / 220.0d, 0.0d, 1.0d);
        }

        if (risk > 1.0d) {
            risk = risk / 100.0d;
        }

        gateAdjust = clamp(gateAdjust, -0.008d, 0.014d);

        if (totalSample >= TEACHER_MATRIX_MIN_SAMPLE && hitRate < 0.300d) {
            gateAdjust += 0.003d;
        }

        if (totalSample >= TEACHER_MATRIX_STRONG_SAMPLE
                && hitRate < 0.285d
                && confidence >= 0.460d) {
            gateAdjust += 0.005d;
        }

        if (comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && comboHitRate > 0.0d
                && comboHitRate < 0.290d) {
            gateAdjust += 0.003d;
        }

        if (totalSample >= TEACHER_MATRIX_STRONG_SAMPLE
                && hitRate >= 0.400d
                && confidence >= 0.500d
                && risk <= 0.008d) {
            gateAdjust -= 0.005d;
        } else if (totalSample >= TEACHER_MATRIX_MIN_SAMPLE
                && hitRate >= 0.370d
                && confidence >= 0.360d
                && risk <= 0.010d) {
            gateAdjust -= 0.003d;
        }

        if (comboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                && comboHitRate >= 0.400d
                && risk <= 0.008d) {
            gateAdjust -= 0.003d;
        } else if (comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && comboHitRate >= 0.370d
                && risk <= 0.010d) {
            gateAdjust -= 0.002d;
        }

        if (cacheSignal != null
                && cacheSignal.sample >= 18
                && cacheSignal.hitRate > 0.0d
                && cacheSignal.hitRate < 0.300d
                && hitRate < 0.330d) {
            gateAdjust += 0.002d;
        }

        if (cacheSignal != null
                && cacheSignal.sample >= 18
                && cacheSignal.hitRate >= 0.390d
                && hitRate >= 0.370d
                && risk <= 0.010d) {
            gateAdjust -= 0.002d;
        }

        if (risk > 0.010d) {
            gateAdjust += Math.min(0.004d, risk * 0.35d);
        }

        signal.applied = 1;
        signal.profileFound = numberSample > 0 ? 3 : 0;
        signal.sample = totalSample;
        signal.numberSample = numberSample;
        signal.comboSample = comboSample;
        signal.hitRate = AppConfig.clampProbability(hitRate);
        signal.comboHitRate = AppConfig.clampProbability(comboHitRate);
        signal.confidence = clamp(confidence, 0.0d, 1.0d);
        signal.gateAdjust = clamp(gateAdjust, -0.008d, 0.014d);
        signal.riskPenalty = clamp(risk, 0.0d, 0.030d);
        signal.stateKey = stateKey;
        signal.comboKey = comboKey;
        signal.profileKey = buildTeacherProfileKey(stateKey, c.position, c.n1);

        if (status == null || status.length() == 0 || "NONE".equals(status)) {
            signal.status = buildTeacherMatrixStatus(signal);
        } else {
            signal.status = status;
        }

        signal.summary =
                "教师矩阵快照 state="
                        + shortKey(stateKey)
                        + " sample="
                        + signal.sample
                        + " numSample="
                        + signal.numberSample
                        + " comboSample="
                        + signal.comboSample
                        + " HR="
                        + UiFormatter.probability(signal.hitRate)
                        + " comboHR="
                        + UiFormatter.probability(signal.comboHitRate)
                        + " conf="
                        + UiFormatter.probability(signal.confidence)
                        + " risk="
                        + UiFormatter.probability(signal.riskPenalty)
                        + " gateAdjust="
                        + UiFormatter.signedProbability(signal.gateAdjust)
                        + " status="
                        + signal.status
                        + "；TeacherMatrix参与门槛微调/风险判断/排序解释，但不能绕过最终门直接救援BET";

        return signal;
    }

    private void applyTeacherMatrixToCandidate(Models.ComboCandidate c, TeacherMatrixGateSignal t) {
        if (c == null || t == null) {
            return;
        }

        writeInt(c, "teacherMatrixSample", t.sample);
        writeInt(c, "teacherMatrixStateSample", t.numberSample);
        writeInt(c, "teacherMatrixComboSample", t.comboSample);
        writeInt(c, "teacherMatrixApplied", t.applied);
        writeDouble(c, "teacherMatrixHitRate", t.hitRate);
        writeDouble(c, "teacherMatrixRecentHitRate", t.comboHitRate);
        writeDouble(c, "teacherMatrixConfidence", t.confidence);
        writeDouble(c, "teacherMatrixGateAdjust", t.gateAdjust);
        writeDouble(c, "teacherMatrixRiskPenalty", t.riskPenalty);
        writeDouble(c, "teacherMatrixNumberScore", t.hitRate);
        writeDouble(c, "teacherMatrixComboScore", t.comboHitRate);
        writeString(c, "teacherMatrixStatus", t.status);
        writeString(c, "teacherMatrixProfileKey", t.profileKey);
        writeString(c, "teacherMatrixComboKey", t.comboKey);
        writeString(c, "teacherMatrixSummary", t.summary);
    }

    private String buildTeacherMatrixStatus(TeacherMatrixGateSignal t) {
        if (t == null || t.applied == 0) {
            return "NONE";
        }

        if (t.sample <= 0 && t.comboSample <= 0 && t.numberSample <= 0) {
            return STATUS_NO_SAMPLE_NEUTRAL;
        }

        if (t.sample < TEACHER_MATRIX_MIN_SAMPLE
                && t.comboSample < TEACHER_MATRIX_COMBO_MIN_SAMPLE) {
            return "WATCH";
        }

        if (t.riskPenalty >= 0.014d
                || (t.sample >= TEACHER_MATRIX_STRONG_SAMPLE && t.hitRate < 0.290d)) {
            return "RISK";
        }

        if (t.sample >= TEACHER_MATRIX_STRONG_SAMPLE
                && t.hitRate >= 0.400d
                && t.confidence >= 0.520d) {
            return "STRONG";
        }

        if (t.comboSample >= TEACHER_MATRIX_COMBO_STRONG_SAMPLE
                && t.comboHitRate >= 0.400d
                && t.confidence >= 0.420d) {
            return "STRONG";
        }

        if (t.sample >= TEACHER_MATRIX_MIN_SAMPLE
                && t.hitRate >= 0.370d
                && t.confidence >= 0.360d) {
            return "READY";
        }

        if (t.comboSample >= TEACHER_MATRIX_COMBO_MIN_SAMPLE
                && t.comboHitRate >= 0.370d
                && t.confidence >= 0.320d) {
            return "READY";
        }

        return "WATCH";
    }

    private String buildTeacherProfileKey(String stateKey, int position, int number) {
        return "TM|P" + position + "|N" + formatNumber(number) + "|" + safeKey(stateKey);
    }

    private String buildTeacherComboKey(String stateKey, int position, int k3Mask) {
        return "TMC|P" + position + "|K" + k3Mask + "|" + safeKey(stateKey);
    }

    private ArrayList<Models.ComboCandidate> prepareInput(List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> list = new ArrayList<Models.ComboCandidate>();

        if (input == null) {
            return list;
        }

        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);

            if (c == null || !c.isValidK3()) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            normalizeCandidate(copy);
            list.add(copy);
        }

        sortByPreGateScore(list);
        return list;
    }

    private void sortByPreGateScore(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = preGateScore(a);
                        double sb = preGateScore(b);

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private void sortByFinal(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = finalSortScore(a);
                        double sb = finalSortScore(b);

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private double preGateScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = 0.0d;

        score += c.finalP * 0.85d;
        score += c.rawP * 0.16d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.12d;

        score += c.profileScore * 0.008d;
        score += c.fallbackProfileScore * 0.004d;
        score += c.aPlusScore * 0.004d;
        score += c.promotionScore * 0.004d;

        score += readDouble(c, "dynamicCauseScore", 0.0d) * 0.018d;
        score += readDouble(c, "ruleHealthScore", 0.0d) * 0.012d;
        score += readDouble(c, "thirdGuardScore", 0.0d) * 0.012d;
        score += readDouble(c, "shadowHealthScore", 0.0d) * 0.008d;
        score += readDouble(c, "teacherNumberScore", 0.0d) * 0.010d;
        score += readDouble(c, "counterfactualScore", 0.0d) * 0.010d;
        score += readDouble(c, "seedExpandScore", readDouble(c, "seedScore", 0.0d)) * 0.006d;
        score += readDouble(c, "positionHealthScore", 0.0d) * 0.006d;

        score -= c.evidenceRisk * 0.06d;
        score -= c.auditPenalty * 0.05d;
        score -= c.driftRisk / 100.0d * 0.010d;
        score -= c.breakDepth / 100.0d * 0.010d;

        score -= readDouble(c, "dynamicRiskScore", 0.0d) * 0.014d;
        score -= readDouble(c, "ruleHealthRisk", 0.0d) * 0.012d;
        score -= readDouble(c, "thirdGuardRisk", 0.0d) * 0.014d;
        score -= readDouble(c, "falseStrongFreezeRisk", 0.0d) * 0.020d;

        String teacherStatus = readString(c, "teacherMatrixStatus", "");
        double teacherRisk = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);
        double teacherHit = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double teacherConf = readDouble(c, "teacherMatrixConfidence", 0.0d);

        if ("STRONG".equals(teacherStatus)) {
            score += 0.012d;
        } else if ("READY".equals(teacherStatus)) {
            score += 0.007d;
        }

        if (teacherHit >= 0.370d && teacherConf >= 0.360d) {
            score += 0.006d;
        }

        if ("RISK".equals(teacherStatus)) {
            score -= 0.026d;
        }

        if (teacherRisk > 0.0d) {
            score -= teacherRisk * 0.026d;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            score -= 0.030d;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1 || readInt(c, "falseStrongHardFreeze", 0) == 1) {
            score -= 0.050d;
        }

        return score;
    }

    private double finalSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = 0.0d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d && isSoftBlock(c.blockCode)) {
            score += 0.010d;
        }

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 0.040d;
        }

        score += c.edge * 1.20d;
        score += c.finalP * 0.82d;
        score += c.championGap * 0.08d;
        score += c.comboGap * 0.12d;

        score += c.profileScore * 0.008d;
        score += c.fallbackProfileScore * 0.004d;
        score += c.aPlusScore * 0.004d;
        score += c.promotionScore * 0.004d;

        score += readDouble(c, "dynamicCauseScore", 0.0d) * 0.020d;
        score += readDouble(c, "ruleHealthScore", 0.0d) * 0.012d;
        score += readDouble(c, "thirdGuardScore", 0.0d) * 0.014d;
        score += readDouble(c, "shadowHealthScore", 0.0d) * 0.008d;
        score += readDouble(c, "teacherNumberScore", 0.0d) * 0.010d;
        score += readDouble(c, "counterfactualScore", 0.0d) * 0.010d;
        score += readDouble(c, "seedExpandScore", readDouble(c, "seedScore", 0.0d)) * 0.006d;
        score += readDouble(c, "positionHealthScore", 0.0d) * 0.006d;

        score -= c.evidenceRisk * 0.06d;
        score -= c.auditPenalty * 0.05d;
        score -= c.driftRisk / 100.0d * 0.010d;
        score -= c.breakDepth / 100.0d * 0.010d;

        score -= readDouble(c, "dynamicRiskScore", 0.0d) * 0.016d;
        score -= readDouble(c, "ruleHealthRisk", 0.0d) * 0.012d;
        score -= readDouble(c, "thirdGuardRisk", 0.0d) * 0.016d;
        score -= readDouble(c, "falseStrongFreezeRisk", 0.0d) * 0.026d;
        score -= readDouble(c, "counterfactualRisk", 0.0d) * 0.012d;
        score -= readDouble(c, "teacherNumberRisk", 0.0d) * 0.010d;

        String teacherStatus = readString(c, "teacherMatrixStatus", "");
        double teacherRisk = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);
        double teacherHit = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double teacherConf = readDouble(c, "teacherMatrixConfidence", 0.0d);

        if ("STRONG".equals(teacherStatus)) {
            score += 0.014d;
        } else if ("READY".equals(teacherStatus)) {
            score += 0.008d;
        }

        if (teacherHit >= 0.370d && teacherConf >= 0.360d) {
            score += 0.006d;
        }

        if ("RISK".equals(teacherStatus)) {
            score -= 0.030d;
        }

        if (teacherRisk > 0.0d) {
            score -= teacherRisk * 0.030d;
        }

        if (c.blockCode != null && c.blockCode.length() > 0 && !isSoftBlock(c.blockCode)) {
            score -= 0.040d;
        }

        if (readInt(c, "falseStrongFreeze", 0) == 1) {
            score -= 0.036d;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1 || readInt(c, "falseStrongHardFreeze", 0) == 1) {
            score -= 0.060d;
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

    private void markTopGap(List<Models.ComboCandidate> list) {
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

    private String buildDetail(GateSignal signal, Models.ComboCandidate c, GateBatchContext ctx) {
        if (signal == null) {
            return "动态门槛为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("V20动态门槛满血版 Base=");
        sb.append(UiFormatter.probability(signal.baseGate));
        sb.append(" State=");
        sb.append(UiFormatter.signedProbability(signal.stateAdjust));
        sb.append(" Risk=");
        sb.append(UiFormatter.signedProbability(signal.riskAdjust));
        sb.append(" Sample=");
        sb.append(UiFormatter.signedProbability(signal.sampleAdjust));
        sb.append(" Freq=");
        sb.append(UiFormatter.signedProbability(signal.frequencyAdjust));
        sb.append(" Trend=");
        sb.append(UiFormatter.signedProbability(signal.trendAdjust));
        sb.append(" Recovery=");
        sb.append(UiFormatter.signedProbability(signal.recoveryAdjust));
        sb.append(" Similar=");
        sb.append(UiFormatter.signedProbability(signal.similarAdjust));
        sb.append(" Memory=");
        sb.append(UiFormatter.signedProbability(signal.memoryAdjust));
        sb.append(" Cal=");
        sb.append(UiFormatter.signedProbability(signal.calibrationAdjust));
        sb.append(" Audit=");
        sb.append(UiFormatter.signedProbability(signal.auditAdjust));
        sb.append(" Teacher=");
        sb.append(UiFormatter.signedProbability(signal.teacherAdjust));
        sb.append(" TeacherMatrix=");
        sb.append(UiFormatter.signedProbability(signal.teacherMatrixAdjust));
        sb.append(" Validate=");
        sb.append(UiFormatter.signedProbability(signal.validationAdjust));
        sb.append(" Profile=");
        sb.append(UiFormatter.signedProbability(signal.profileAdjust));
        sb.append(" Promotion=");
        sb.append(UiFormatter.signedProbability(signal.promotionAdjust));
        sb.append(" A+=");
        sb.append(UiFormatter.signedProbability(signal.aPlusAdjust));

        sb.append(" Quality=");
        sb.append(UiFormatter.signedProbability(signal.qualityGateAdjust));
        sb.append(" Diag=");
        sb.append(UiFormatter.signedProbability(signal.diagnosisGateAdjust));
        sb.append(" Module=");
        sb.append(UiFormatter.signedProbability(signal.moduleEffectGateAdjust));
        sb.append(" Freeze=");
        sb.append(UiFormatter.signedProbability(signal.freezeGateAdjust));
        sb.append(" ShadowPromo=");
        sb.append(UiFormatter.signedProbability(signal.shadowPromotionGateAdjust));
        sb.append(" V20因链=");
        sb.append(UiFormatter.signedProbability(signal.v20CauseAdjust));
        sb.append(" V20规则=");
        sb.append(UiFormatter.signedProbability(signal.v20RuleAdjust));
        sb.append(" V20防线=");
        sb.append(UiFormatter.signedProbability(signal.v20GuardAdjust));
        sb.append(" V20影子=");
        sb.append(UiFormatter.signedProbability(signal.v20ShadowAdjust));
        sb.append(" V20冻结=");
        sb.append(UiFormatter.signedProbability(signal.v20FreezeAdjust));
        sb.append(" V20反事实=");
        sb.append(UiFormatter.signedProbability(signal.v20RankAdjust));
        sb.append(" V20种子=");
        sb.append(UiFormatter.signedProbability(signal.v20SeedAdjust));
        sb.append(" V20教师号=");
        sb.append(UiFormatter.signedProbability(signal.v20TeacherNumberAdjust));
        sb.append(" V20位置=");
        sb.append(UiFormatter.signedProbability(signal.v20PositionAdjust));
        sb.append(" V20窗口=");
        sb.append(UiFormatter.signedProbability(signal.v20WindowAdjust));
        sb.append(" V20动作=");
        sb.append(UiFormatter.signedProbability(signal.v20ActionAdjust));

        sb.append(" FinalGate=");
        sb.append(UiFormatter.probability(signal.finalGate));
        sb.append(" Edge=");
        sb.append(UiFormatter.signedProbability(signal.edge));
        sb.append(" Strength=");
        sb.append((int) signal.strengthScore);
        sb.append(" RiskScore=");
        sb.append((int) signal.riskScore);
        sb.append(" SampleScore=");
        sb.append((int) signal.sampleScore);
        sb.append(" F100=");
        sb.append(UiFormatter.probability(signal.freqRate100));
        sb.append(" F500=");
        sb.append(UiFormatter.probability(signal.freqRate500));
        sb.append(" F1000=");
        sb.append(UiFormatter.probability(signal.freqRate1000));
        sb.append(
                "；策略=质量风险动态门：频率不硬封顶，强证据低反证不因高频硬压；频率低不救援，只有强证据/样本可信/近期有效才小幅放松；冻结/误导/假强只抬同类状态门槛；最终仍由DecisionGate裁决");

        if (ctx != null && ctx.isOverBudget()) {
            sb.append("；本轮动态门槛接近预算，后续候选走轻量证据，不丢候选");
        }

        if (signal.cacheDetail != null && signal.cacheDetail.length() > 0) {
            sb.append(" Cache=");
            sb.append(signal.cacheDetail);
        }

        if (signal.qualitySummary != null && signal.qualitySummary.length() > 0) {
            sb.append(" 质量门=");
            sb.append(signal.qualitySummary);
        }
        if (signal.frequencyPolicy != null && signal.frequencyPolicy.length() > 0) {
            sb.append(" 频率策略=");
            sb.append(signal.frequencyPolicy);
        }
        if (signal.actionAdvice != null && signal.actionAdvice.length() > 0) {
            sb.append(" 动作建议=");
            sb.append(signal.actionAdvice);
        }

        if (signal.teacherMatrixDetail != null && signal.teacherMatrixDetail.length() > 0) {
            sb.append(" TM=");
            sb.append(signal.teacherMatrixDetail);
        }

        sb.append(" V20字段=");
        sb.append(v20FieldSummary(c));
        if (signal.v20WindowStateKey != null && signal.v20WindowStateKey.length() > 0) {
            sb.append(" 窗口状态=");
            sb.append(signal.v20WindowStateKey);
            sb.append(" 证据=");
            sb.append(UiFormatter.probability(signal.v20EvidenceContribution));
            sb.append(" 反证=");
            sb.append(UiFormatter.probability(signal.v20CounterContribution));
            sb.append(" 样本可信=");
            sb.append(UiFormatter.probability(signal.v20SampleReliability));
            sb.append(" 近期有效=");
            sb.append(UiFormatter.probability(signal.v20RecentValidity));
            sb.append(" 动作=");
            sb.append(signal.v20ActionAdvice);
        }

        if (signal.hardBlock == 1) {
            sb.append("，结论=动态门槛强制HOLD：");
            sb.append(UiFormatter.blockReasonText(signal.blockCode));
        } else if (signal.allowedByGate == 1) {
            sb.append("，结论=概率超过动态门槛，可进入最终决策门");
        } else {
            sb.append("，结论=未过动态门槛：");
            if (signal.blockCode != null && signal.blockCode.length() > 0) {
                sb.append(UiFormatter.blockReasonText(signal.blockCode));
            } else {
                sb.append("FinalP不足或Edge不够");
            }
        }

        return sb.toString();
    }

    private String v20FieldSummary(Models.ComboCandidate c) {
        if (c == null) {
            return "候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("dyn=");
        sb.append(UiFormatter.probability(readDouble(c, "dynamicCauseScore", 0.0d)));
        sb.append("/risk=");
        sb.append(UiFormatter.probability(readDouble(c, "dynamicRiskScore", 0.0d)));
        sb.append("/pass=");
        sb.append(readInt(c, "dynamicPass", 0));

        sb.append(" rule=");
        sb.append(UiFormatter.probability(readDouble(c, "ruleHealthScore", 0.0d)));
        sb.append("/risk=");
        sb.append(UiFormatter.probability(readDouble(c, "ruleHealthRisk", 0.0d)));

        sb.append(" guard=");
        sb.append(UiFormatter.probability(readDouble(c, "thirdGuardScore", 0.0d)));
        sb.append("/risk=");
        sb.append(UiFormatter.probability(readDouble(c, "thirdGuardRisk", 0.0d)));

        sb.append(" shadow=");
        sb.append(UiFormatter.probability(readDouble(c, "shadowHealthScore", 0.0d)));
        sb.append("/risk=");
        sb.append(UiFormatter.probability(readDouble(c, "shadowHealthRisk", 0.0d)));

        sb.append(" freeze=");
        sb.append(readInt(c, "falseStrongFreeze", 0));
        sb.append("/risk=");
        sb.append(UiFormatter.probability(readDouble(c, "falseStrongFreezeRisk", 0.0d)));

        return sb.toString();
    }

    private void normalizeCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            c.k3Mask = AppConfig.buildK3Mask(c.n1, c.n2, c.n3);
        }

        if (c.dynamicGate <= 0.0d
                || Double.isNaN(c.dynamicGate)
                || Double.isInfinite(c.dynamicGate)
                || c.dynamicGate > STALE_GATE_HIGH) {
            c.dynamicGate = readBaseGate();
        }

        if (c.finalP < 0.0d || Double.isNaN(c.finalP) || Double.isInfinite(c.finalP)) {
            c.finalP = 0.0d;
        }

        if (c.finalP > 1.0d) {
            c.finalP = 1.0d;
        }

        if (c.rawP < 0.0d || Double.isNaN(c.rawP) || Double.isInfinite(c.rawP)) {
            c.rawP = 0.0d;
        }

        if (c.rawP > 1.0d) {
            c.rawP = 1.0d;
        }

        c.dynamicGate = AppConfig.clampProbability(c.dynamicGate);
        c.finalP = AppConfig.clampProbability(c.finalP);
        c.rawP = AppConfig.clampProbability(c.rawP);
        c.edge = c.finalP - c.dynamicGate;
    }

    private boolean lowRisk(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return false;
        }

        if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            return false;
        }

        if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            return false;
        }

        if (c.evidenceRisk >= 0.660d) {
            return false;
        }

        if (readDouble(c, "dynamicRiskScore", 0.0d) >= 0.660d) {
            return false;
        }

        if (readDouble(c, "ruleHealthRisk", 0.0d) >= 0.660d) {
            return false;
        }

        if (readDouble(c, "thirdGuardRisk", 0.0d) >= 0.660d) {
            return false;
        }

        if (readDouble(c, "falseStrongFreezeRisk", 0.0d) >= 0.660d) {
            return false;
        }

        if (readInt(c, "dynamicHardRisk", 0) == 1
                || readInt(c, "falseStrongHardFreeze", 0) == 1
                || readInt(c, "thirdGuardHardBlock", 0) == 1) {
            return false;
        }

        return true;
    }

    private boolean isSoftBlock(String blockCode) {
        if (blockCode == null || blockCode.length() == 0) {
            return true;
        }

        if (AppConfig.BLOCK_LOW_PROBABILITY.equals(blockCode)) {
            return true;
        }

        if (freqHighBlockCode().equals(blockCode)) {
            return true;
        }

        if (teacherMatrixBlockCode().equals(blockCode)) {
            return true;
        }

        if ("V20_CAUSE_RISK".equals(blockCode)
                || "DYNAMIC_CAUSE_WEAK".equals(blockCode)
                || "FALSE_STRONG_FREEZE".equals(blockCode)) {
            return true;
        }

        return false;
    }

    private String safeBlock(String block, String fallback) {
        if (block == null || block.length() == 0) {
            return fallback == null ? "" : fallback;
        }
        return block;
    }

    private boolean isWeakPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        if (d.targetIssue == null || d.targetIssue.length() == 0) {
            return true;
        }

        String text = "";

        if (d.reasonSummary != null) {
            text += d.reasonSummary;
        }

        if (d.reasonText != null) {
            text += " " + d.reasonText;
        }

        if (d.deepReason != null) {
            text += " " + d.deepReason;
        }

        if (d.learningSummary != null) {
            text += " " + d.learningSummary;
        }

        if (d.blockCode != null) {
            text += " " + d.blockCode;
        }

        if (text.indexOf("临时") >= 0
                || text.indexOf("占位") >= 0
                || text.indexOf("正在生成") >= 0
                || text.indexOf("等待实时分析") >= 0
                || text.indexOf("未生成有效决策") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("QUICK_OBSERVE") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        if (AppConfig.DECISION_HOLD.equals(d.decision)
                && d.position <= 0
                && d.finalP <= 0.0001d
                && !AppConfig.isValidK3Mask(d.k3Mask)) {
            return true;
        }

        return false;
    }

    private void repairDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (!AppConfig.isValidK3Mask(d.k3Mask)
                && AppConfig.isValidNumber(d.n1)
                && AppConfig.isValidNumber(d.n2)
                && AppConfig.isValidNumber(d.n3)) {
            d.k3Mask = AppConfig.buildK3Mask(d.n1, d.n2, d.n3);
        }
    }

    private String normalizeDecisionText(String decision, String action) {
        String d = decision == null ? "" : decision.trim();
        String a = action == null ? "" : action.trim();

        String x = d.length() > 0 ? d : a;
        x = x.toUpperCase(java.util.Locale.ROOT);

        if ("BET".equals(x) || "REAL".equals(x) || "REAL_TRADE".equals(x) || "投注".equals(x)) {
            return AppConfig.DECISION_BET;
        }

        if ("SHADOW".equals(x)
                || "OBSERVE".equals(x)
                || "WATCH".equals(x)
                || "影子".equals(x)
                || "观察".equals(x)) {
            return AppConfig.DECISION_SHADOW;
        }

        return AppConfig.DECISION_HOLD;
    }

    private boolean isTerminalBadState(String mode) {
        if (mode == null) {
            return false;
        }

        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode);
    }

    private String buildRuntimeStateKey(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(c.position);

        if (c.stateMode != null && c.stateMode.length() > 0) {
            sb.append("|S=");
            sb.append(cleanKeyPart(c.stateMode));
        } else {
            sb.append("|S=");
            sb.append(cleanKeyPart(modeUnknown()));
        }

        if (c.shapeKey != null && c.shapeKey.length() > 0) {
            sb.append("|K=");
            sb.append(cleanKeyPart(c.shapeKey));
        }

        sb.append("|");
        sb.append(bucketText("DRIFT", c.driftRisk));
        sb.append("|");
        sb.append(bucketText("BREAK", c.breakDepth));
        sb.append("|");
        sb.append(bucketText("RECOVER", c.recoveryScore));

        return sb.toString();
    }

    private String bucketText(String prefix, double v) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0d) {
            return prefix + "_0";
        }

        double n = v;

        if (n > 1.0d) {
            n = n / 100.0d;
        }

        if (n < 0.20d) {
            return prefix + "_LOW";
        }

        if (n < 0.45d) {
            return prefix + "_MID";
        }

        if (n < 0.70d) {
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

    private String shortKey(String key) {
        if (key == null) {
            return "";
        }

        if (key.length() <= 60) {
            return key;
        }

        return key.substring(0, 60);
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

    private String formatNumber(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private Object invokeStore(DbStore store, String methodName, Class[] types, Object[] args) {
        if (store == null || methodName == null || methodName.length() == 0) {
            return null;
        }

        try {
            Method m = store.getClass().getMethod(methodName, types);
            return m.invoke(store, args);
        } catch (Throwable e) {
            return null;
        }
    }

    private int readInt(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
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

    private double readDouble(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);

                if (Double.isNaN(d2) || Double.isInfinite(d2)) {
                    return def;
                }

                return d2;
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readString(Object obj, String fieldName, String def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v == null) {
                return def;
            }

            return String.valueOf(v);
        } catch (Throwable e) {
        }

        return def;
    }

    private void writeInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Integer.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Double.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private double readBaseGate() {
        double gate = readAppDouble("DEFAULT_DYNAMIC_GATE", ORIGINAL_BASE_GATE);

        if (gate <= 0.0d || Double.isNaN(gate) || Double.isInfinite(gate)) {
            gate = ORIGINAL_BASE_GATE;
        }

        if (gate < readMinGate()) {
            gate = ORIGINAL_BASE_GATE;
        }

        if (gate > ORIGINAL_NORMAL_CAP) {
            gate = ORIGINAL_BASE_GATE;
        }

        return gate;
    }

    private double readMinGate() {
        double gate = readAppDouble("MIN_EXEC_GATE", ORIGINAL_MIN_GATE);

        if (gate <= 0.0d || Double.isNaN(gate) || Double.isInfinite(gate)) {
            gate = ORIGINAL_MIN_GATE;
        }

        if (gate < 0.330d) {
            gate = 0.330d;
        }

        if (gate > ORIGINAL_BASE_GATE) {
            gate = ORIGINAL_MIN_GATE;
        }

        return gate;
    }

    private static int readWarmupIssues() {
        int v = readAppIntStatic("LIVE_SHADOW_WARMUP_ISSUES", 30);

        if (v <= 0) {
            v = readAppIntStatic("SHADOW_WARMUP_ISSUES", 30);
        }

        if (v <= 0) {
            v = 30;
        }

        return v;
    }

    private static int readAppIntStatic(String fieldName, int def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

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

    private String freqHighBlockCode() {
        return readAppString("BLOCK_FREQ_HIGH", "FREQ_HIGH");
    }

    private String trendWeakBlockCode() {
        return readAppString("BLOCK_TREND_WEAK", "TREND_WEAK");
    }

    private String teacherMatrixBlockCode() {
        return readAppString("BLOCK_TEACHER_MATRIX_WEAK", "TEACHER_MATRIX_WEAK");
    }

    private String modeUnknown() {
        return readAppString("MODE_UNKNOWN", "UNKNOWN");
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private double trendLifeMin() {
        return readAppDouble("TREND_LIFE_MIN", 55.0d);
    }

    private boolean readAppBoolean(String fieldName, boolean def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

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

    private String readAppString(String fieldName, String def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

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

    private double readAppDouble(String fieldName, double def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

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

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0d;
        }

        if (value < min) {
            return min;
        }

        if (value > max) {
            return max;
        }

        return value;
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

    private static final class V20GateEvidence {
        public int position;
        public int k3Mask;
        public String baseIssue;
        public String targetIssue;
        public String stateKey;
        public int sample;
        public int recentSample;
        public double hitRate;
        public double recentHitRate;
        public double hotColdSlope;
        public double omissionScore;
        public double densityScore;
        public double repeatScore;
        public double parityBias;
        public double zoneBias;
        public double neighborScore;
        public double sameTailScore;
        public double crowdedScore;
        public double syncScore;
        public double timingShift;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public int hardRisk;
        public String evidenceText;
        public String counterEvidenceText;
        public String actionAdvice;
        public String summary;

        public V20GateEvidence() {
            position = 0;
            k3Mask = 0;
            baseIssue = "";
            targetIssue = "";
            stateKey = "";
            sample = 0;
            recentSample = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            hotColdSlope = 0.0d;
            omissionScore = 0.0d;
            densityScore = 0.0d;
            repeatScore = 0.0d;
            parityBias = 0.0d;
            zoneBias = 0.0d;
            neighborScore = 0.0d;
            sameTailScore = 0.0d;
            crowdedScore = 0.0d;
            syncScore = 0.0d;
            timingShift = 0.0d;
            evidenceContribution = 0.0d;
            counterContribution = 0.0d;
            sampleReliability = 0.0d;
            recentValidity = 0.0d;
            featureDistance = 1.0d;
            hardRisk = 0;
            evidenceText = "";
            counterEvidenceText = "";
            actionAdvice = "";
            summary = "";
        }
    }

    private static final class GateBatchContext {
        public String mode;
        public long startedAt;
        public long budgetMs;
        public int inputCount;

        public double freqRate50;
        public double freqRate100;
        public double freqRate300;
        public double freqRate500;
        public double freqRate1000;
        public Models.FrequencyState frequencyState;
        public double probeFrequency20;
        public double probeFrequency100;
        public int recentLoseCount;

        public String auditStatus;
        public String moduleEffectStatus;
        public String moduleEffectSummary;
        public String shadowPromotionStatus;
        public String shadowPromotionSummary;
        public String dynamicFreezeStatus;
        public String dynamicFreezeSummary;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeSummary;
        public String diagnosisClosedLoopSummary;
        public double teacherAdjust;

        public int validationBetCount;
        public double validationFrequency;
        public double validationHitRate;
        public double validationPnl;
        public double validationMaxDrawdown;
        public int validationTeacherSample;
        public double validationTeacherHitRate;

        public GateBatchContext() {
            mode = "";
            startedAt = System.currentTimeMillis();
            budgetMs = REALTIME_GATE_BUDGET_MS;
            inputCount = 0;

            freqRate50 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate100 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate300 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate500 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate1000 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            frequencyState = new Models.FrequencyState();
            probeFrequency20 = 0.0d;
            probeFrequency100 = 0.0d;
            recentLoseCount = 0;

            auditStatus = "";
            moduleEffectStatus = "";
            moduleEffectSummary = "";
            shadowPromotionStatus = "";
            shadowPromotionSummary = "";
            dynamicFreezeStatus = "";
            dynamicFreezeSummary = "";
            adaptiveJudgeStatus = "";
            adaptiveJudgeSummary = "";
            diagnosisClosedLoopSummary = "";
            teacherAdjust = 0.0d;

            validationBetCount = 0;
            validationFrequency = 0.0d;
            validationHitRate = 0.0d;
            validationPnl = 0.0d;
            validationMaxDrawdown = 0.0d;
            validationTeacherSample = 0;
            validationTeacherHitRate = 0.0d;
        }

        public boolean isOverBudget() {
            if (budgetMs <= 0L) {
                return false;
            }

            return System.currentTimeMillis() - startedAt >= budgetMs;
        }
    }

    private static final class MainChainGateSignal {
        public double gateAdjust;
        public double evidenceScore;
        public double counterScore;
        public double netScore;
        public String rejectCode;
        public String actionAdvice;
        public String summary;

        public MainChainGateSignal() {
            gateAdjust = 0.0d;
            evidenceScore = 0.0d;
            counterScore = 0.0d;
            netScore = 0.0d;
            rejectCode = "DYNAMIC_GATE_MAIN_CHAIN_WATCH";
            actionAdvice = "MAIN_CHAIN_WATCH";
            summary = "";
        }
    }

    private static final class CacheGateSignal {
        public String stateKey;
        public int sample;
        public double hitRate;
        public double risk;
        public int stateNumberFound;
        public int stateNumberSample;
        public double avgNumberSupport;
        public double weakestNumberSupport;
        public double avgNumberRisk;
        public String summary;

        public CacheGateSignal() {
            stateKey = "";
            sample = 0;
            hitRate = 0.0d;
            risk = 0.0d;
            stateNumberFound = 0;
            stateNumberSample = 0;
            avgNumberSupport = 0.0d;
            weakestNumberSupport = 0.0d;
            avgNumberRisk = 0.0d;
            summary = "";
        }
    }

    private static final class TeacherMatrixGateSignal {
        public int applied;
        public int profileFound;
        public int sample;
        public int numberSample;
        public int comboSample;
        public double hitRate;
        public double comboHitRate;
        public double confidence;
        public double gateAdjust;
        public double riskPenalty;
        public String stateKey;
        public String profileKey;
        public String comboKey;
        public String status;
        public String summary;

        public TeacherMatrixGateSignal() {
            applied = 0;
            profileFound = 0;
            sample = 0;
            numberSample = 0;
            comboSample = 0;
            hitRate = 0.0d;
            comboHitRate = 0.0d;
            confidence = 0.0d;
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

    public static final class GateSignal {
        public int position;
        public int k3Mask;

        public double baseGate;
        public double finalGate;
        public double edge;

        public double stateAdjust;
        public double riskAdjust;
        public double sampleAdjust;
        public double frequencyAdjust;
        public double trendAdjust;
        public double recoveryAdjust;
        public double similarAdjust;
        public double memoryAdjust;
        public double calibrationAdjust;
        public double auditAdjust;
        public double teacherAdjust;
        public double teacherMatrixAdjust;
        public double validationAdjust;
        public double profileAdjust;
        public double promotionAdjust;
        public double aPlusAdjust;

        public double qualityGateAdjust;
        public double diagnosisGateAdjust;
        public double moduleEffectGateAdjust;
        public double freezeGateAdjust;
        public double shadowPromotionGateAdjust;
        public double coverageGateAdjust;
        public double edgeQualityGateAdjust;
        public double mainChainGateAdjust;
        public double mainChainEvidenceScore;
        public double mainChainCounterScore;
        public double mainChainNetScore;
        public String mainChainRejectCode;
        public String mainChainActionAdvice;
        public String mainChainSummary;

        public double v20CauseAdjust;
        public double v20RuleAdjust;
        public double v20GuardAdjust;
        public double v20ShadowAdjust;
        public double v20FreezeAdjust;
        public double v20RankAdjust;
        public double v20SeedAdjust;
        public double v20TeacherNumberAdjust;
        public double v20PositionAdjust;
        public double v20WindowAdjust;
        public double v20ActionAdjust;

        public String v20WindowStateKey;
        public String v20EvidenceText;
        public String v20CounterEvidenceText;
        public String v20ActionAdvice;
        public String v20AuditSummary;
        public double v20EvidenceContribution;
        public double v20CounterContribution;
        public double v20SampleReliability;
        public double v20RecentValidity;
        public double v20FeatureDistance;

        public double strengthScore;
        public double riskScore;
        public double sampleScore;

        public double freqRate100;
        public double freqRate500;
        public double freqRate1000;

        public int cacheSample;
        public double cacheHitRate;
        public double cacheRisk;
        public String cacheDetail;

        public int teacherMatrixSample;
        public int teacherMatrixComboSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixComboHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixRisk;
        public String teacherMatrixStatus;
        public String teacherMatrixDetail;

        public int allowedByGate;
        public int hardBlock;
        public String blockCode;
        public String detail;

        public String qualitySummary;
        public String frequencyPolicy;
        public String actionAdvice;

        public GateSignal() {
            position = 0;
            k3Mask = 0;

            baseGate = ORIGINAL_BASE_GATE;
            finalGate = ORIGINAL_BASE_GATE;
            edge = 0.0d;

            stateAdjust = 0.0d;
            riskAdjust = 0.0d;
            sampleAdjust = 0.0d;
            frequencyAdjust = 0.0d;
            trendAdjust = 0.0d;
            recoveryAdjust = 0.0d;
            similarAdjust = 0.0d;
            memoryAdjust = 0.0d;
            calibrationAdjust = 0.0d;
            auditAdjust = 0.0d;
            teacherAdjust = 0.0d;
            teacherMatrixAdjust = 0.0d;
            validationAdjust = 0.0d;
            profileAdjust = 0.0d;
            promotionAdjust = 0.0d;
            aPlusAdjust = 0.0d;

            qualityGateAdjust = 0.0d;
            diagnosisGateAdjust = 0.0d;
            moduleEffectGateAdjust = 0.0d;
            freezeGateAdjust = 0.0d;
            shadowPromotionGateAdjust = 0.0d;
            coverageGateAdjust = 0.0d;
            edgeQualityGateAdjust = 0.0d;
            mainChainGateAdjust = 0.0d;
            mainChainEvidenceScore = 0.0d;
            mainChainCounterScore = 0.0d;
            mainChainNetScore = 0.0d;
            mainChainRejectCode = "";
            mainChainActionAdvice = "";
            mainChainSummary = "";

            v20CauseAdjust = 0.0d;
            v20RuleAdjust = 0.0d;
            v20GuardAdjust = 0.0d;
            v20ShadowAdjust = 0.0d;
            v20FreezeAdjust = 0.0d;
            v20RankAdjust = 0.0d;
            v20SeedAdjust = 0.0d;
            v20TeacherNumberAdjust = 0.0d;
            v20PositionAdjust = 0.0d;
            v20WindowAdjust = 0.0d;
            v20ActionAdjust = 0.0d;

            v20WindowStateKey = "";
            v20EvidenceText = "";
            v20CounterEvidenceText = "";
            v20ActionAdvice = "";
            v20AuditSummary = "";
            v20EvidenceContribution = 0.0d;
            v20CounterContribution = 0.0d;
            v20SampleReliability = 0.0d;
            v20RecentValidity = 0.0d;
            v20FeatureDistance = 0.0d;

            strengthScore = 0.0d;
            riskScore = 0.0d;
            sampleScore = 0.0d;

            freqRate100 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate500 = FREQ_NEUTRAL_WHEN_UNKNOWN;
            freqRate1000 = FREQ_NEUTRAL_WHEN_UNKNOWN;

            cacheSample = 0;
            cacheHitRate = 0.0d;
            cacheRisk = 0.0d;
            cacheDetail = "";

            teacherMatrixSample = 0;
            teacherMatrixComboSample = 0;
            teacherMatrixHitRate = 0.0d;
            teacherMatrixComboHitRate = 0.0d;
            teacherMatrixConfidence = 0.0d;
            teacherMatrixRisk = 0.0d;
            teacherMatrixStatus = "";
            teacherMatrixDetail = "";

            allowedByGate = 0;
            hardBlock = 0;
            blockCode = "";
            detail = "";
            qualitySummary = "";
            frequencyPolicy = "";
            actionAdvice = "";
            qualitySummary = "";
            frequencyPolicy = "";
            actionAdvice = "";
        }
    }

    private boolean isNeutralEvidenceStatusV6(String v) {
        try {
            return AppConfig.isNeutralEvidenceStatus(v);
        } catch (Throwable e) {
            String s = v == null ? "" : v.toUpperCase(Locale.US);
            return s.length() == 0
                    || s.indexOf("NO_SAMPLE") >= 0
                    || s.indexOf("WATCH") >= 0
                    || s.indexOf("NEUTRAL") >= 0;
        }
    }

    private int progressiveProbeQuota(Models.ComboCandidate c) {
        String p = readString(c, "shadowPromotionStatus", "");
        String t = readString(c, "teacherMatrixStatus", "");
        String d = readString(c, "dynamicClusterStatus", "");
        if (upperLocal(p).indexOf("STRONG") >= 0
                || upperLocal(t).indexOf("STRONG") >= 0
                || upperLocal(d).indexOf("STRONG") >= 0) return Integer.MAX_VALUE;
        if (upperLocal(p).indexOf("READY") >= 0
                || upperLocal(t).indexOf("READY") >= 0
                || upperLocal(d).indexOf("READY") >= 0) return Integer.MAX_VALUE;
        if (upperLocal(p).indexOf("WATCH") >= 0
                || upperLocal(t).indexOf("WATCH") >= 0
                || upperLocal(d).indexOf("WATCH") >= 0) return Integer.MAX_VALUE;
        return Integer.MAX_VALUE;
    }
}
