package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * AdaptiveLearningJudgeEngine
 *
 * <p>V20 满级诊断 + 智能审判闭环升级 · 第6/14 修正版 SAFE
 *
 * <p>这版专门修复 AIDE 编译问题： 1. 只保留一个 public 类：AdaptiveLearningJudgeEngine； 2. 所有数据结构改为同文件
 * package-private 顶层类； 3. 不再使用 public static 内部类，避免 AIDE 误判“此处不允许 public/static”； 4.
 * CandidateSnapshot 内不再调用外部 private static 方法； 5. 文件名必须是 AdaptiveLearningJudgeEngine.java。
 *
 * <p>职责： 候选 → 概率/Teacher/V20/Gate字段 → 模块评分 → SHADOW晋级 → 冻结矩阵 → 本引擎审判 → DecisionGateEngine最终裁决。
 *
 * <p>红线： 不出号、不创建Trade、不保存Decision、不结算、不写真实盈亏、 不把SHADOW转BET、不绕过DecisionGateEngine。
 *
 * <p>输出： PASS / WATCH / DOWNGRADE_TO_SHADOW / FREEZE / REJECT_FAKE_STRONG /
 * PROMOTE_SHADOW_CANDIDATE。
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class AdaptiveLearningJudgeEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final String VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;

    public static final String MAIN_CHAIN_FIX_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String CLOSED_LOOP_FIX_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String FIELD_CHAIN_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String STATUS_NO_SAMPLE_NEUTRAL = "NO_SAMPLE_NEUTRAL";
    public static final String ACTION_REVIEW_BY_DECISION_GATE = "REVIEW_BY_DECISION_GATE";
    public static final String REJECT_NONE = "ADAPTIVE_OK";
    public static final String REJECT_NO_SAMPLE_NEUTRAL = "ADAPTIVE_NO_SAMPLE_NEUTRAL";
    public static final String REJECT_NEGATIVE_EDGE = "ADAPTIVE_NEGATIVE_EDGE";
    public static final String REJECT_TEACHER_RISK = "ADAPTIVE_TEACHER_RISK";
    public static final String REJECT_V20_RISK = "ADAPTIVE_V20_RISK";
    public static final String REJECT_RULE_DECAY = "ADAPTIVE_RULE_DECAY";
    public static final String REJECT_THIRD_DRAG = "ADAPTIVE_THIRD_DRAG";
    public static final String REJECT_COUNTERFACTUAL_RISK = "ADAPTIVE_COUNTERFACTUAL_RISK";
    public static final String REJECT_FREEZE = "ADAPTIVE_FREEZE";

    public static final String STATUS_PASS = "PASS";
    public static final String STATUS_WATCH = "WATCH";
    public static final String STATUS_DOWNGRADE_TO_SHADOW = "DOWNGRADE_TO_SHADOW";
    public static final String STATUS_FREEZE = "FREEZE";
    public static final String STATUS_REJECT_FAKE_STRONG = "REJECT_FAKE_STRONG";
    public static final String STATUS_PROMOTE_SHADOW_CANDIDATE = "PROMOTE_SHADOW_CANDIDATE";

    public static final String ACTION_ALLOW_DECISION_GATE = "ALLOW_DECISION_GATE";
    public static final String ACTION_SHADOW_ONLY = "SHADOW_ONLY";
    public static final String ACTION_HOLD_OR_SHADOW = "HOLD_OR_SHADOW";
    public static final String ACTION_REJECT = "REJECT";
    public static final String STATUS_DYNAMIC_READY = "DYNAMIC_READY";
    public static final String ACTION_DYNAMIC_READY_REVIEW = "DYNAMIC_READY_REVIEW";
    public static final String ACTION_DYNAMIC_FREEZE = "DYNAMIC_FREEZE";

    private static final int DEFAULT_HISTORY_LIMIT = 120;
    private static final int DEFAULT_DECISION_SCAN_LIMIT = 300;
    private static final int POSITION_COUNT = 10;
    private static final int MAX_SIGNALS = 64;
    private static final int MAX_REPORT_LEN = 20000;

    private static final double BASE_K3_RATE = 0.300d;
    private static final double PASS_SCORE = 0.680d;
    private static final double WATCH_SCORE = 0.520d;
    private static final double DOWNGRADE_RISK = 0.620d;
    private static final double FREEZE_RISK = 0.780d;
    private static final double FAKE_STRONG_RISK = 0.700d;

    private static final String LINK_V2_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final int REALTIME_INPUT_LIMIT = 40;
    private static final int BACKGROUND_INPUT_LIMIT = 120;
    private static final long REALTIME_BUDGET_MS = 900L;
    private static final long CONTEXT_CACHE_MS = 10L * 60L * 1000L;
    private static volatile AdaptiveJudgeContext cachedContext;
    private static volatile long cachedContextAt;
    private static final String STATUS_WATCH_STALE = "WATCH_STALE";

    public AdaptiveLearningJudgeEngine() {}

    public AdaptiveJudgeResult judge(DbStore store, Object candidate) {
        AdaptiveJudgeContext ctx = buildRealtimeContext(store);
        return judge(store, candidate, ctx);
    }

    public AdaptiveJudgeResult judge(
            DbStore store,
            Object candidate,
            Object moduleEffectReport,
            Object shadowPromotionReport,
            Object freezeReport) {
        AdaptiveJudgeContext ctx = new AdaptiveJudgeContext();
        ctx.createdAt = System.currentTimeMillis();
        ctx.moduleEffectReport = moduleEffectReport;
        ctx.shadowPromotionReport = shadowPromotionReport;
        ctx.freezeReport = freezeReport;
        if (store != null) {
            fillHistoryContext(store, ctx);
        }
        return judge(store, candidate, ctx);
    }

    public AdaptiveJudgeResult judge(
            DbStore store, Object candidate, AdaptiveJudgeContext context) {
        AdaptiveJudgeCandidateSnapshot snap = makeCandidateSnapshot(candidate);
        if (context == null) {
            context = buildContext(store);
        }
        return judgeSnapshot(snap, context);
    }

    public AdaptiveJudgeResult judgeSnapshot(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext context) {
        AdaptiveJudgeResult r = new AdaptiveJudgeResult();
        r.version = LINK_V2_VERSION;
        r.createdAt = System.currentTimeMillis();
        r.candidate = c;

        if (c == null) {
            r.status = STATUS_WATCH;
            r.action = ACTION_SHADOW_ONLY;
            r.pass = 0;
            r.downgradeToShadow = 1;
            r.reason = "候选为空，不能进入真实BET";
            return r;
        }

        if (context == null) {
            context = new AdaptiveJudgeContext();
            context.createdAt = System.currentTimeMillis();
        }

        r.finalP = c.finalP;
        r.dynamicGate = c.dynamicGate;
        r.edge = c.edge;
        r.position = c.position;
        r.teacherStatus = c.teacherBucket;
        r.v20Status = c.v20Bucket;
        r.windowStateKey = c.windowBucket;

        double evidenceScore = computeEvidenceScore(c, context, r);
        double riskScore = computeRiskScore(c, context, r);
        double moduleScore = computeModuleScore(c, context, r);
        double promotionScore = computeShadowPromotionScore(c, context, r);
        double freezeRisk = computeFreezeRisk(c, context, r);
        double historyScore = computeHistoryScore(c, context, r);

        r.evidenceScore = evidenceScore;
        r.judgeRisk = riskScore;
        r.moduleScore = moduleScore;
        r.shadowPromotionScore = promotionScore;
        r.freezeRisk = freezeRisk;
        r.historyScore = historyScore;

        double score =
                evidenceScore * 0.34d
                        + moduleScore * 0.18d
                        + promotionScore * 0.16d
                        + historyScore * 0.12d
                        + edgeScore(c.edge) * 0.10d
                        + finalPScore(c.finalP) * 0.10d
                        - riskScore * 0.30d
                        - freezeRisk * 0.36d;

        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        r.judgeScore = score;

        applyFinalStatus(c, context, r);
        finalizeRejectCode(c, r);
        buildJudgeText(c, context, r);
        return r;
    }

    public AdaptiveJudgeReport judgeCandidates(DbStore store, List candidates) {
        AdaptiveJudgeReport report = buildJudgeReport(store, candidates);
        saveJudgeState(store, report);
        return report;
    }

    public AdaptiveJudgeReport applyRealtime(DbStore store, List candidates) {
        return judgeCandidates(store, candidates);
    }

    public AdaptiveJudgeReport applyToAll(DbStore store, List candidates) {
        return judgeCandidates(store, candidates);
    }

    public AdaptiveJudgeReport applyRealtimeCandidates(DbStore store, List candidates) {
        return judgeCandidates(store, candidates);
    }

    public AdaptiveJudgeReport enrichCandidates(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport analyzeCandidates(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport applyToAll(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport applyRealtime(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport runRealtime(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport run(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public AdaptiveJudgeReport apply(List candidates) {
        return judgeCandidates(null, candidates);
    }

    public int applyAndCount(List candidates) {
        AdaptiveJudgeReport r = judgeCandidates(null, candidates);
        return r == null || r.results == null ? 0 : r.results.size();
    }

    public AdaptiveJudgeReport buildJudgeReport(DbStore store) {
        return buildJudgeReport(store, null);
    }

    public AdaptiveJudgeReport buildJudgeReport(DbStore store, List candidates) {
        AdaptiveJudgeReport report = new AdaptiveJudgeReport();
        report.version = LINK_V2_VERSION;
        report.createdAt = System.currentTimeMillis();
        long startedAt = report.createdAt;
        if (AppConfig.REALTIME_ADAPTIVE_EMPTY_FAST_RETURN
                && (candidates == null || candidates.size() == 0)) {
            report.ok = 1;
            report.summary =
                    "AdaptiveJudge无候选快速返回：NO_CANDIDATE_NEUTRAL；不加载历史、不运行SHADOW/Freeze/Module重链";
            report.detail = report.summary;
            return report;
        }
        AdaptiveJudgeContext ctx = buildRealtimeContext(store);
        report.context = ctx;
        int deferred = 0;
        int inputSize = candidates == null ? 0 : candidates.size();
        int hardLimit = store == null ? REALTIME_INPUT_LIMIT : BACKGROUND_INPUT_LIMIT;
        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                Object obj = candidates.get(i);
                if (obj == null) continue;
                boolean overBudget = System.currentTimeMillis() - startedAt >= REALTIME_BUDGET_MS;
                if (i >= hardLimit || overBudget) {
                    applyDeferredAdaptiveState(
                            obj,
                            i + 1,
                            overBudget ? "ADAPTIVE_BUDGET_DEFERRED" : "ADAPTIVE_RANK_DEFERRED");
                    deferred++;
                    continue;
                }
                AdaptiveJudgeResult r = judge(store, obj, ctx);
                applyResultToCandidate(obj, r);
                report.results.add(r);
                addReportCount(report, r);
            }
        }
        report.summary =
                buildReportSummary(report)
                        + "｜输入="
                        + inputSize
                        + "｜已审="
                        + report.results.size()
                        + "｜延后="
                        + deferred
                        + "｜耗时="
                        + (System.currentTimeMillis() - startedAt)
                        + "ms";
        report.detail = buildReportDetail(report);
        report.ok = 1;
        if (store != null) {
            setStateSafe(store, "adaptive_judge_deferred", String.valueOf(deferred));
            setStateSafe(store, "adaptive_judge_input_count", String.valueOf(inputSize));
            setStateSafe(
                    store,
                    "adaptive_judge_elapsed_ms",
                    String.valueOf(System.currentTimeMillis() - startedAt));
            setStateSafe(store, "adaptive_judge_version_v2", LINK_V2_VERSION);
        }
        return report;
    }

    public AdaptiveJudgeReport saveJudgeState(DbStore store) {
        AdaptiveJudgeReport r = buildJudgeReport(store);
        saveJudgeState(store, r);
        return r;
    }

    public void saveJudgeState(DbStore store, AdaptiveJudgeReport report) {
        if (store == null || report == null) return;
        setStateSafe(store, "adaptive_judge_version", safe(report.version));
        setStateSafe(store, "adaptive_judge_updated_at", String.valueOf(report.createdAt));
        setStateSafe(store, "adaptive_judge_status", report.ok == 1 ? "OK" : "ERROR");
        setStateSafe(store, "adaptive_judge_total", String.valueOf(report.results.size()));
        setStateSafe(store, "adaptive_judge_pass", String.valueOf(report.passCount));
        setStateSafe(store, "adaptive_judge_watch", String.valueOf(report.watchCount));
        setStateSafe(store, "adaptive_judge_downgrade", String.valueOf(report.downgradeCount));
        setStateSafe(store, "adaptive_judge_freeze", String.valueOf(report.freezeCount));
        setStateSafe(store, "adaptive_judge_fake_strong", String.valueOf(report.fakeStrongCount));
        setStateSafe(store, "adaptive_judge_promote", String.valueOf(report.promoteCount));
        setStateSafe(store, "adaptive_judge_summary", safe(report.summary));
        setStateSafe(store, "adaptive_judge_detail", safe(report.detail));
        setStateSafe(store, "adaptive_judge_compact_json", safe(buildCompactJson(report)));
        saveRealtimeJudgeSnapshots(store, report);
    }

    public String buildJudgeSummary(DbStore store) {
        AdaptiveJudgeReport r = buildJudgeReport(store);
        return r == null ? "" : safe(r.summary);
    }

    public String buildJudgeDetail(DbStore store) {
        AdaptiveJudgeReport r = buildJudgeReport(store);
        return r == null ? "" : safe(r.detail);
    }

    public String buildJudgeCopyText(DbStore store) {
        AdaptiveJudgeReport r = buildJudgeReport(store);
        if (r == null) return "V20智能审判为空";
        return safe(r.detail);
    }

    private void applyResultToCandidate(Object candidate, AdaptiveJudgeResult r) {
        if (candidate == null || r == null) return;
        setFieldSafe(candidate, "adaptiveJudgeStatus", safe(r.status));
        setFieldSafe(candidate, "adaptiveStatus", safe(r.status));
        setFieldSafe(candidate, "adaptiveLearningJudgeStatus", safe(r.status));
        setFieldSafe(candidate, "adaptiveJudgeAction", safe(r.action));
        setFieldSafe(candidate, "adaptiveAction", safe(r.action));
        setFieldSafe(candidate, "adaptiveLearningJudgeAction", safe(r.action));
        setFieldSafe(candidate, "adaptiveJudgeReason", safe(r.reason));
        setFieldSafe(candidate, "adaptiveActionAdvice", safe(r.moduleAdvice));
        setFieldSafe(candidate, "adaptiveJudgeActionAdvice", safe(r.moduleAdvice));
        setFieldSafe(candidate, "adaptiveJudgeDomain", buildAdaptiveDomain(r));
        setFieldSafe(candidate, "adaptiveCauseScore", Double.valueOf(r.evidenceScore));
        setFieldSafe(candidate, "adaptiveEvidenceScore", Double.valueOf(r.evidenceScore));
        setFieldSafe(candidate, "adaptiveCounterScore", Double.valueOf(r.judgeRisk));
        setFieldSafe(candidate, "adaptiveRiskScore", Double.valueOf(r.judgeRisk));
        setFieldSafe(candidate, "adaptivePromoteScore", Double.valueOf(r.shadowPromotionScore));
        setFieldSafe(
                candidate,
                "adaptiveFreezeScore",
                Double.valueOf(Math.max(r.freezeRisk, r.judgeRisk)));
        setFieldSafe(candidate, "adaptiveJudgeScore", Double.valueOf(r.judgeScore));
        setFieldSafe(candidate, "adaptiveRejectCode", safe(r.rejectCode));
        boolean hardReject =
                STATUS_FREEZE.equals(r.status)
                        || STATUS_REJECT_FAKE_STRONG.equals(r.status)
                        || ACTION_REJECT.equals(r.action);
        setFieldSafe(
                candidate,
                "riskSeverity",
                hardReject
                        ? AppConfig.EVIDENCE_SEVERITY_HARD_BLOCK
                        : (r.judgeRisk >= 0.62d
                                ? AppConfig.EVIDENCE_SEVERITY_SOFT_RISK
                                : (r.pass == 1
                                        ? AppConfig.EVIDENCE_SEVERITY_WATCH
                                        : AppConfig.EVIDENCE_SEVERITY_NEUTRAL)));
        setFieldSafe(candidate, "hardBlockFlag", Integer.valueOf(hardReject ? 1 : 0));
        setFieldSafe(candidate, "hardBlockSource", hardReject ? "AdaptiveLearningJudgeEngine" : "");
        setFieldSafe(
                candidate,
                "blockTier",
                hardReject ? "HARD_RISK" : (r.pass == 1 ? "PROBE_REVIEW" : "OBSERVE"));
        setFieldSafe(candidate, "hardRiskCode", hardReject ? safe(r.rejectCode) : "");
        setFieldSafe(candidate, "observeReason", hardReject ? "" : safe(r.reason));
        if (hardReject) {
            String oldReject = getString(candidate, "rejectCode", "");
            if (oldReject == null
                    || oldReject.length() == 0
                    || "OK".equals(oldReject)
                    || "ADAPTIVE_OK".equals(oldReject)) {
                setFieldSafe(candidate, "rejectCode", safe(r.rejectCode));
            }
            String oldFinalReject = getString(candidate, "finalDecisionRejectCode", "");
            if (oldFinalReject == null
                    || oldFinalReject.length() == 0
                    || "OK".equals(oldFinalReject)
                    || "ADAPTIVE_OK".equals(oldFinalReject)) {
                setFieldSafe(candidate, "finalDecisionRejectCode", safe(r.rejectCode));
            }
        }
        setFieldSafe(
                candidate,
                "antiEvidenceScore",
                Double.valueOf(
                        Math.max(r.judgeRisk, getDouble(candidate, "antiEvidenceScore", 0.0d))));
        setFieldSafe(
                candidate,
                "counterEvidenceScore",
                Double.valueOf(
                        Math.max(r.judgeRisk, getDouble(candidate, "counterEvidenceScore", 0.0d))));
        setFieldSafe(
                candidate,
                "netEvidenceScore",
                Double.valueOf(clampSignedForField(r.evidenceScore - r.judgeRisk)));
        setFieldSafe(candidate, "adaptiveProbeSupport", Integer.valueOf(r.pass == 1 ? 1 : 0));
        setFieldSafe(candidate, "fieldChainVersion", LINK_V2_VERSION);
        setFieldSafe(candidate, "adaptiveInputFlag", Integer.valueOf(1));
        setFieldSafe(candidate, "adaptiveFreshness", "WARM_CACHED");
        setFieldSafe(candidate, "whyNoBet", r.pass == 1 ? "" : safe(r.reason));
        setFieldSafe(candidate, "whyBet", r.pass == 1 ? safe(r.reason) : "");
        setFieldSafe(
                candidate,
                "dynamicCausalSummary",
                "AdaptiveJudge="
                        + safe(r.status)
                        + "｜score="
                        + pct(r.judgeScore)
                        + "｜risk="
                        + pct(r.judgeRisk)
                        + "｜reject="
                        + safe(r.rejectCode)
                        + "｜"
                        + safe(r.reason));
    }

    private void applyDeferredAdaptiveState(Object candidate, int rankNo, String reason) {
        if (candidate == null) return;
        setFieldSafe(candidate, "adaptiveJudgeStatus", STATUS_WATCH_STALE);
        setFieldSafe(candidate, "adaptiveStatus", STATUS_WATCH_STALE);
        setFieldSafe(candidate, "adaptiveLearningJudgeStatus", STATUS_WATCH_STALE);
        setFieldSafe(candidate, "adaptiveJudgeAction", ACTION_REVIEW_BY_DECISION_GATE);
        setFieldSafe(candidate, "adaptiveAction", ACTION_REVIEW_BY_DECISION_GATE);
        setFieldSafe(
                candidate,
                "adaptiveJudgeReason",
                "智能审判延后｜rank=" + rankNo + "｜reason=" + safe(reason));
        setFieldSafe(candidate, "adaptiveFreshness", "DEFERRED_BACKGROUND");
        setFieldSafe(candidate, "adaptiveInputFlag", Integer.valueOf(0));
        setFieldSafe(candidate, "adaptiveProbeSupport", Integer.valueOf(0));
        setFieldSafe(candidate, "fieldChainVersion", LINK_V2_VERSION);
        /*
         * WATCH_STALE是中性缺样本，不覆盖其他引擎已经写入的硬RejectCode，
         * 也不允许以“缺失Adaptive”直接进入频率探针。
         */
    }

    private String buildAdaptiveDomain(AdaptiveJudgeResult r) {
        if (r == null) return "ADAPTIVE:EMPTY";
        return "ADAPTIVE|" + safe(r.status) + "|" + safe(r.action);
    }

    private void saveRealtimeJudgeSnapshots(DbStore store, AdaptiveJudgeReport report) {
        if (store == null || report == null) return;
        try {
            setStateSafe(store, "adaptive_judge_last_run", String.valueOf(report.createdAt));
            setStateSafe(store, "adaptive_judge_summary", safe(report.summary));
            if (report.results != null) {
                for (int i = 0; i < report.results.size(); i++) {
                    AdaptiveJudgeResult r = report.results.get(i);
                    if (r == null) continue;
                    Models.AdaptiveJudgeResult mr = toModelsAdaptiveJudgeResult(r);
                    store.saveAdaptiveJudgeSample(mr);
                }
            }
        } catch (Throwable e) {
        }
    }

    private Models.AdaptiveJudgeResult toModelsAdaptiveJudgeResult(AdaptiveJudgeResult r) {
        Models.AdaptiveJudgeResult m = new Models.AdaptiveJudgeResult();
        if (r == null) return m;
        m.targetIssue = "";
        m.clusterId = r.candidate == null ? "" : r.candidate.dynamicClusterId;
        m.clusterStatus = r.candidate == null ? "" : r.candidate.dynamicClusterStatus;
        m.status = r.status;
        m.action = r.action;
        m.domain = buildAdaptiveDomain(r);
        m.candidateCount = 1;
        m.clusterHitRate = r.candidate == null ? 0.0d : r.candidate.dynamicClusterHitRate;
        m.clusterRecentHitRate = 0.0d;
        m.clusterSample = r.candidate == null ? 0 : r.candidate.dynamicClusterSample;
        m.clusterConfidence = 0.0d;
        m.causeScore = r.evidenceScore;
        m.counterScore = r.judgeRisk;
        m.promoteScore = r.shadowPromotionScore;
        m.freezeScore = Math.max(r.freezeRisk, r.judgeRisk);
        m.reason = r.reason;
        m.version = LINK_V2_VERSION;
        try {
            m.recalc();
        } catch (Throwable e) {
        }
        return m;
    }

    private void setFieldSafe(Object obj, String field, Object value) {
        if (obj == null || field == null) return;
        try {
            Field f = obj.getClass().getField(field);
            if (f.getType() == int.class && value instanceof Number) {
                f.setInt(obj, ((Number) value).intValue());
            } else if (f.getType() == double.class && value instanceof Number) {
                f.setDouble(obj, ((Number) value).doubleValue());
            } else if (f.getType() == long.class && value instanceof Number) {
                f.setLong(obj, ((Number) value).longValue());
            } else {
                f.set(obj, value);
            }
        } catch (Throwable e) {
        }
    }

    private AdaptiveJudgeContext buildRealtimeContext(DbStore store) {
        AdaptiveJudgeContext ctx = new AdaptiveJudgeContext();
        ctx.createdAt = System.currentTimeMillis();
        if (store != null) {
            try {
                Models.FrequencyState fs = store.loadFrequencyState();
                if (fs != null) {
                    ctx.totalBet = Math.max(0, fs.realBetCount);
                    ctx.winRate = 0.0d;
                }
            } catch (Throwable e) {
            }
        }
        return ctx;
    }

    private AdaptiveJudgeContext buildContext(DbStore store) {
        long now = System.currentTimeMillis();
        AdaptiveJudgeContext cached = cachedContext;
        if (cached != null && now - cachedContextAt < CONTEXT_CACHE_MS) {
            return cached;
        }
        AdaptiveJudgeContext ctx = new AdaptiveJudgeContext();
        ctx.createdAt = now;
        if (store != null) {
            fillHistoryContext(store, ctx);
            /*
             * 实时链不再反射调用ShadowPromotionVerifier.verify扫描全部SHADOW，
             * Promotion字段由Settlement增量域和候选字段直接承接。
             */
            ctx.moduleEffectReport = null;
            ctx.shadowPromotionReport = null;
            ctx.freezeReport = null;
        }
        cachedContext = ctx;
        cachedContextAt = now;
        return ctx;
    }

    private void fillHistoryContext(DbStore store, AdaptiveJudgeContext ctx) {
        if (store == null || ctx == null) return;
        ArrayList<AdaptiveJudgeHistoryCase> cases =
                loadRecentRealBetCases(store, DEFAULT_HISTORY_LIMIT);
        ctx.historyCases = cases;

        for (int i = 0; i < cases.size(); i++) {
            AdaptiveJudgeHistoryCase c = cases.get(i);
            if (c == null) continue;
            if (c.settled != 1) continue;
            if (!isWinResult(c.result) && !isMissResult(c.result)) continue;

            ctx.totalBet++;
            if (c.hit == 1 || isWinResult(c.result)) ctx.hitBet++;
            else ctx.missBet++;

            addDomain(ctx.positionMap, "P" + c.position, c);
            addDomain(ctx.finalPMap, c.finalPBucket, c);
            addDomain(ctx.edgeMap, c.edgeBucket, c);
            addDomain(ctx.teacherMap, c.teacherBucket, c);
            addDomain(ctx.v20Map, c.v20Bucket, c);
            addDomain(ctx.windowMap, c.windowBucket, c);
            addDomain(ctx.comboMap, comboDomainKey(c), c);
            addDomain(ctx.k3Map, k3LayerBucket(c), c);
        }

        if (ctx.totalBet > 0) {
            ctx.winRate = ((double) ctx.hitBet) / ((double) ctx.totalBet);
        }
    }

    private void addDomain(
            HashMap<String, AdaptiveJudgeDomainStats> map, String key, AdaptiveJudgeHistoryCase c) {
        if (map == null || c == null) return;
        key = normalizeBucket(key);
        AdaptiveJudgeDomainStats d = map.get(key);
        if (d == null) {
            d = new AdaptiveJudgeDomainStats();
            d.key = key;
            map.put(key, d);
        }
        d.total++;
        if (c.hit == 1 || isWinResult(c.result)) d.hit++;
        else d.miss++;
        d.sumFinalP += c.finalP;
        d.sumEdge += c.edge;
    }

    private double computeEvidenceScore(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        double score = 0.0d;

        if (c.finalP >= 0.420d) {
            score += 0.24d;
            addSignal(r, "PROB", "FinalP>=42%，概率证据强", 0.24d, 0.0d);
        } else if (c.finalP >= 0.400d) {
            score += 0.20d;
            addSignal(r, "PROB", "FinalP>=40%，概率证据较强", 0.20d, 0.0d);
        } else if (c.finalP >= 0.380d) {
            score += 0.14d;
            addSignal(r, "PROB", "FinalP>=38%，概率证据可观察", 0.14d, 0.0d);
        } else if (c.finalP >= 0.360d) {
            score += 0.07d;
            addSignal(r, "PROB", "FinalP低位，仅弱证据", 0.07d, 0.0d);
        }

        if (c.edge >= 0.030d) {
            score += 0.20d;
            addSignal(r, "EDGE", "Edge>=3%，门槛优势强", 0.20d, 0.0d);
        } else if (c.edge >= 0.020d) {
            score += 0.16d;
            addSignal(r, "EDGE", "Edge>=2%，门槛优势有效", 0.16d, 0.0d);
        } else if (c.edge >= 0.010d) {
            score += 0.09d;
            addSignal(r, "EDGE", "Edge>=1%，优势偏薄", 0.09d, 0.0d);
        } else if (c.edge >= 0.0d) {
            score += 0.03d;
            addSignal(r, "EDGE", "Edge接近0，仅观察", 0.03d, 0.0d);
        }

        if (isTeacherStrong(c)) {
            score += 0.16d;
            addSignal(r, "TEACHER", "Teacher STRONG/READY 支持", 0.16d, 0.0d);
        }
        if (hasV20Support(c)) {
            score += 0.18d;
            addSignal(r, "V20", "V20因链有证据支持", 0.18d, 0.0d);
        }
        if (hasTrustedWindow(c)) {
            score += 0.12d;
            addSignal(r, "WINDOW", "WindowState/相似窗口可信", 0.12d, 0.0d);
        }
        if (c.sampleReliability >= 0.65d || c.effectiveSample >= 80 || c.comboSample >= 80) {
            score += 0.10d;
            addSignal(r, "SAMPLE", "样本可信度较高", 0.10d, 0.0d);
        }
        if (c.recentValidity >= 0.65d
                || c.decisionEvidenceElite > 0
                || c.decisionEvidenceStrong > 0) {
            score += 0.10d;
            addSignal(r, "RECENT", "近期有效性较高", 0.10d, 0.0d);
        }

        score += computeDynamicCausalEvidence(c, r);
        score += computeMainChainBridgeEvidence(c, r);
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private double computeRiskScore(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        double risk = 0.0d;

        if (c.finalP < 0.360d) {
            risk += 0.18d;
            addSignal(r, "PROB_RISK", "FinalP低于36%，弱证据风险", 0.0d, 0.18d);
        }
        if (c.edge < 0.0d) {
            risk += 0.28d;
            addSignal(r, "EDGE_RISK", "负Edge候选，禁止低频救援", 0.0d, 0.28d);
        } else if (c.edge < 0.008d) {
            risk += 0.12d;
            addSignal(r, "EDGE_RISK", "Edge过薄", 0.0d, 0.12d);
        }

        if (isTeacherRisk(c)) {
            risk += 0.32d;
            addSignal(r, "TEACHER_RISK", "Teacher RISK/FREEZE", 0.0d, 0.32d);
        } else if (isTeacherNoSampleNeutral(c)) {
            addSignal(r, "TEACHER_NO_SAMPLE", "Teacher NO_SAMPLE/空样本按中性处理，不硬杀，不救援BET", 0.0d, 0.0d);
        } else if (isTeacherWeakOrEmpty(c)) {
            risk += 0.10d;
            addSignal(r, "TEACHER_WEAK", "Teacher WEAK 只做软风险观察，不等同FREEZE", 0.0d, 0.10d);
        }

        if (hasV20Risk(c)) {
            risk += 0.34d;
            addSignal(r, "V20_RISK", "V20反证/硬风险高", 0.0d, 0.34d);
        } else if (!hasV20Support(c)) {
            risk += 0.08d;
            addSignal(r, "V20_WATCH", "V20因链为空或WATCH，按软观察处理，不硬杀", 0.0d, 0.08d);
        }

        if (!hasTrustedWindow(c)) {
            risk += 0.18d;
            addSignal(r, "WINDOW_RISK", "WindowState样本不可信", 0.0d, 0.18d);
        }
        if (c.sampleReliability > 0.0d && c.sampleReliability < 0.45d) {
            risk += 0.12d;
            addSignal(r, "SAMPLE_RISK", "样本可信度低", 0.0d, 0.12d);
        }
        if (c.recentValidity > 0.0d && c.recentValidity < 0.42d) {
            risk += 0.12d;
            addSignal(r, "RECENT_RISK", "近期有效性低", 0.0d, 0.12d);
        }
        if (c.counterContribution > c.evidenceContribution + 0.080d
                && c.counterContribution > 0.0d) {
            risk += 0.20d;
            addSignal(r, "COUNTER_RISK", "反证贡献高于证据贡献", 0.0d, 0.20d);
        }
        if (c.falseStrongRisk >= 0.60d) {
            risk += 0.22d;
            addSignal(r, "FALSE_STRONG", "候选存在假强风险", 0.0d, 0.22d);
        }

        risk += computeMainChainBridgeRisk(c, r);
        risk += computeDynamicCausalRisk(c, r);
        if (risk > 1.0d) risk = 1.0d;
        return risk;
    }

    private double computeDynamicCausalEvidence(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null) return 0.0d;
        double score = 0.0d;
        if (AppConfig.CLUSTER_STATUS_STRONG.equals(c.dynamicClusterStatus)
                || (c.dynamicClusterHitRate >= AppConfig.DYNAMIC_CLUSTER_STRONG_HIT_RATE
                        && c.dynamicClusterSample >= AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_STRONG)) {
            score += 0.18d;
            addSignal(r, "DYNAMIC_CLUSTER", "动态窗口簇 STRONG 支持", 0.18d, 0.0d);
        } else if (AppConfig.CLUSTER_STATUS_READY.equals(c.dynamicClusterStatus)
                || (c.dynamicClusterHitRate >= AppConfig.DYNAMIC_CLUSTER_READY_HIT_RATE
                        && c.dynamicClusterSample >= AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_READY)) {
            score += 0.12d;
            addSignal(r, "DYNAMIC_CLUSTER", "动态窗口簇 READY 支持", 0.12d, 0.0d);
        }
        if (c.needSupplyMatchScore >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE
                && c.needSupplyThirdQuality >= AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY
                && c.needSupplyRisk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK) {
            score += 0.14d;
            addSignal(r, "NEED_SUPPLY", "NeedSupply 需求供给匹配通过", 0.14d, 0.0d);
        }
        if (c.causeAblationScore >= AppConfig.CAUSE_ABLATION_MIN_SCORE
                && c.causeAblationFalseCauseCount <= 2
                && c.counterfactualWhyNotRisk <= AppConfig.COUNTERFACTUAL_MAX_WHY_NOT_RISK) {
            score += 0.12d;
            addSignal(r, "COUNTERFACTUAL", "反事实/因子剥离支持，假因较少", 0.12d, 0.0d);
        }
        return score;
    }

    private double computeDynamicCausalRisk(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null) return 0.0d;
        double risk = 0.0d;
        if (AppConfig.isDynamicClusterStatusRisk(c.dynamicClusterStatus)
                || (c.dynamicClusterHitRate > 0.0d
                        && c.dynamicClusterHitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE)
                || c.dynamicClusterMaxLose >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE) {
            risk += 0.32d;
            addSignal(r, "DYNAMIC_CLUSTER_RISK", "动态窗口簇低于随机/冻结/连挂过高", 0.0d, 0.32d);
        }
        if (c.needSupplyRisk >= AppConfig.NEED_SUPPLY_HARD_RISK
                || c.needSupplyMatchScore < AppConfig.NEED_SUPPLY_MIN_MATCH_SCORE
                || c.needSupplyThirdQuality < AppConfig.NEED_SUPPLY_MIN_THIRD_QUALITY) {
            risk += 0.24d;
            addSignal(r, "NEED_SUPPLY_RISK", "NeedSupply 匹配不足或第三号拖拽", 0.0d, 0.24d);
        }
        if (c.counterfactualWhyNotRisk >= AppConfig.COUNTERFACTUAL_HARD_WHY_NOT_RISK
                || c.causeAblationFalseCauseCount >= 4) {
            risk += 0.28d;
            addSignal(r, "COUNTERFACTUAL_RISK", "反事实反证强或疑似假因过多", 0.0d, 0.28d);
        }
        if (risk > 1.0d) risk = 1.0d;
        return risk;
    }

    private boolean isDynamicHardFreeze(AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null) return false;
        if (AppConfig.isDynamicClusterStatusRisk(c.dynamicClusterStatus)
                && c.dynamicClusterSample >= AppConfig.DYNAMIC_HARD_BLOCK_MIN_SAMPLE) return true;
        if (c.dynamicClusterHitRate > 0.0d
                && c.dynamicClusterHitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE
                && c.dynamicClusterSample >= AppConfig.DYNAMIC_HARD_BLOCK_MIN_SAMPLE) return true;
        if (c.dynamicClusterMaxLose >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE) return true;
        if (c.needSupplyRisk >= AppConfig.NEED_SUPPLY_HARD_RISK) return true;
        if (c.counterfactualWhyNotRisk >= AppConfig.COUNTERFACTUAL_HARD_WHY_NOT_RISK) return true;
        return false;
    }

    private boolean isDynamicReady(AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null || r == null) return false;
        boolean clusterOk =
                AppConfig.CLUSTER_STATUS_READY.equals(c.dynamicClusterStatus)
                        || AppConfig.CLUSTER_STATUS_STRONG.equals(c.dynamicClusterStatus)
                        || (c.dynamicClusterSample >= AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_READY
                                && c.dynamicClusterHitRate
                                        >= AppConfig.DYNAMIC_CLUSTER_READY_HIT_RATE);
        boolean needOk =
                c.needSupplyMatchScore >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE
                        && c.needSupplyThirdQuality >= AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY
                        && c.needSupplyRisk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK;
        boolean cfOk =
                c.counterfactualWhyNotRisk <= AppConfig.COUNTERFACTUAL_MAX_WHY_NOT_RISK
                        && c.causeAblationFalseCauseCount <= 3;
        return clusterOk && needOk && cfOk;
    }

    private double computeMainChainBridgeEvidence(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null) return 0.0d;
        double score = 0.0d;
        if (c.shadowPromoteScore >= 0.38d || isPromotionReady(c.shadowPromotionStatus)) {
            score += Math.min(0.14d, 0.06d + c.shadowPromoteScore * 0.12d);
            addSignal(
                    r,
                    "SHADOW_PROMOTION",
                    "SHADOW晋级域只做加分证据，仍需DecisionGate复核",
                    Math.min(0.14d, 0.06d + c.shadowPromoteScore * 0.12d),
                    0.0d);
        }
        if (c.ruleHealthScore >= 0.52d && c.ruleDecayRisk < 0.45d && c.ruleHealthRisk < 0.58d) {
            score += 0.08d;
            addSignal(r, "RULE_HEALTH", "规则健康矩阵通过，近期未明显衰减", 0.08d, 0.0d);
        }
        if (c.thirdQuality >= 0.36d && c.thirdDragRisk < 0.55d) {
            score += 0.06d;
            addSignal(r, "THIRD_GUARD", "第三号质量不拖拽", 0.06d, 0.0d);
        }
        if (c.rankStability >= 0.48d && c.counterfactualRisk < 0.58d) {
            score += 0.06d;
            addSignal(r, "COUNTERFACTUAL", "反事实排名稳定", 0.06d, 0.0d);
        }
        return clamp01(score);
    }

    private double computeMainChainBridgeRisk(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (c == null) return 0.0d;
        double risk = 0.0d;
        if (isPromotionBlocked(c.shadowPromotionStatus) || containsRiskWord(c.shadowRejectCode)) {
            risk += 0.16d;
            addSignal(
                    r,
                    "SHADOW_PROMOTION_RISK",
                    "SHADOW晋级验证为BLOCK/FREEZE/NO_PROMOTE，只写反证不直接封杀",
                    0.0d,
                    0.16d);
        }
        if (c.ruleDecayRisk >= 0.62d || containsRiskWord(c.ruleRejectCode)) {
            risk += 0.18d;
            addSignal(r, "RULE_DECAY_RISK", "规则健康近期衰减或过期", 0.0d, 0.18d);
        }
        if (c.thirdDragRisk >= 0.62d
                || c.thirdWeakness >= 0.62d
                || containsRiskWord(c.thirdRejectCode)) {
            risk += 0.20d;
            addSignal(r, "THIRD_DRAG_RISK", "第三号拖拽/低供给/虚强", 0.0d, 0.20d);
        }
        if (c.counterfactualRisk >= 0.68d
                || c.replaceGain >= 0.10d
                || containsRiskWord(c.counterfactualRejectCode)) {
            risk += 0.20d;
            addSignal(r, "COUNTERFACTUAL_RISK", "反事实替代收益或反证风险过高", 0.0d, 0.20d);
        }
        return clamp01(risk);
    }

    private double computeModuleScore(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        double score = 0.50d;
        if (ctx == null || ctx.moduleEffectReport == null) {
            addSignal(r, "MODULE", "模块评分报告为空，按中性处理", 0.0d, 0.0d);
            return score;
        }

        score += moduleAdjustment(ctx.moduleEffectReport, "ProbabilityEngine", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "TeacherNumberMatrixEngine", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "DynamicCauseChainEngine", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "WindowState", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "DynamicGateEngine", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "DecisionGateEngine", r);
        score += moduleAdjustment(ctx.moduleEffectReport, "K3Coverage", r);

        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private double moduleAdjustment(Object report, String moduleName, AdaptiveJudgeResult r) {
        Object effect = getEffectObject(report, moduleName);
        if (effect == null) return 0.0d;

        String status = getString(effect, "effectStatus", "");
        double hitRate = getDouble(effect, "hitRate", 0.0d);
        double reliability = getDouble(effect, "reliability", 0.0d);
        int betCount = getInt(effect, "betCount", 0);

        if ("EFFECTIVE".equals(status) && reliability >= 0.45d) {
            double add = 0.040d + Math.min(0.060d, Math.max(0.0d, hitRate - BASE_K3_RATE) * 0.40d);
            addSignal(r, "MODULE_EFFECT", moduleName + " 历史有效 " + betCount + "样本", add, 0.0d);
            return add;
        }
        if ("MISLEADING".equals(status)) {
            double sub = -0.070d - Math.min(0.080d, reliability * 0.080d);
            addSignal(r, "MODULE_RISK", moduleName + " 被评分为MISLEADING", 0.0d, -sub);
            return sub;
        }
        if ("CONFLICT".equals(status)) {
            addSignal(r, "MODULE_CONFLICT", moduleName + " 历史/近期冲突", 0.0d, 0.045d);
            return -0.045d;
        }
        if ("INSUFFICIENT".equals(status)) {
            return -0.010d;
        }
        return 0.0d;
    }

    private Object getEffectObject(Object report, String moduleName) {
        if (report == null || moduleName == null) return null;

        try {
            Method m = report.getClass().getMethod("getEffect", new Class[] {String.class});
            Object obj = m.invoke(report, new Object[] {moduleName});
            if (obj != null) return obj;
        } catch (Throwable e) {
        }

        Object effectsObj = getObject(report, "effects", null);
        if (effectsObj instanceof List) {
            List list = (List) effectsObj;
            for (int i = 0; i < list.size(); i++) {
                Object e = list.get(i);
                if (e == null) continue;
                String n = getString(e, "moduleName", "");
                if (moduleName.equals(n)) return e;
            }
        }

        return null;
    }

    private double computeShadowPromotionScore(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        if (ctx == null || ctx.shadowPromotionReport == null || c == null) {
            return 0.0d;
        }

        double best = 0.0d;
        String matched = "";

        Object signalsObj = getObject(ctx.shadowPromotionReport, "signals", null);
        if (signalsObj instanceof List) {
            List list = (List) signalsObj;
            for (int i = 0; i < list.size(); i++) {
                Object s = list.get(i);
                if (s == null) continue;
                String status = getString(s, "status", "");
                String domainKey = getString(s, "domainKey", "");
                double score = getDouble(s, "score", 0.0d);
                if (matchesPromotionDomain(c, domainKey)) {
                    if ("PROMOTE_WATCH".equals(status)) {
                        double v = 0.18d + score * 0.18d;
                        if (v > best) {
                            best = v;
                            matched = domainKey;
                        }
                    } else if ("WATCH".equals(status)) {
                        double v = 0.06d + score * 0.08d;
                        if (v > best) {
                            best = v;
                            matched = domainKey;
                        }
                    }
                }
            }
        }

        Object promotedObj = getObject(ctx.shadowPromotionReport, "promotedDomains", null);
        if (promotedObj instanceof List) {
            List list = (List) promotedObj;
            for (int i = 0; i < list.size(); i++) {
                Object d = list.get(i);
                if (d == null) continue;
                String domainKey = getString(d, "domainKey", "");
                if (matchesPromotionDomain(c, domainKey)) {
                    double rate = getDouble(d, "wouldHitRate", 0.0d);
                    double reliability = getDouble(d, "reliability", 0.0d);
                    double v =
                            0.16d
                                    + Math.max(0.0d, rate - BASE_K3_RATE) * 0.50d
                                    + reliability * 0.10d;
                    if (v > best) {
                        best = v;
                        matched = domainKey;
                    }
                }
            }
        }

        if (best > 0.0d) {
            addSignal(r, "SHADOW_PROMOTE", "命中SHADOW晋级域：" + matched, best, 0.0d);
        }

        if (best > 0.45d) best = 0.45d;
        return best;
    }

    private boolean matchesPromotionDomain(AdaptiveJudgeCandidateSnapshot c, String domainKey) {
        if (c == null || domainKey == null || domainKey.length() == 0) return false;
        if (domainKey.indexOf("P" + c.position) >= 0) return true;
        if (domainKey.indexOf(c.finalPBucket) >= 0) return true;
        if (domainKey.indexOf(c.edgeBucket) >= 0) return true;
        if (c.teacherBucket.length() > 0 && domainKey.indexOf(shortBucket(c.teacherBucket)) >= 0)
            return true;
        if (c.v20Bucket.length() > 0 && domainKey.indexOf(shortBucket(c.v20Bucket)) >= 0)
            return true;
        if (c.windowBucket.length() > 0 && domainKey.indexOf(shortBucket(c.windowBucket)) >= 0)
            return true;
        if (c.comboDomain.length() > 0 && domainKey.indexOf(shortBucket(c.comboDomain)) >= 0)
            return true;
        return false;
    }

    private double computeFreezeRisk(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        if (ctx == null || ctx.freezeReport == null || c == null) {
            return 0.0d;
        }

        Object rulesObj = getObject(ctx.freezeReport, "freezeRules", null);
        if (rulesObj instanceof List) {
            List list = (List) rulesObj;
            double maxRisk = 0.0d;
            String matched = "";
            String matchedLevel = "";
            String matchedReason = "";
            for (int i = 0; i < list.size(); i++) {
                Object rule = list.get(i);
                if (rule == null) continue;
                String key = getString(rule, "freezeKey", "");
                String level = getString(rule, "freezeLevel", "");
                String reason = getString(rule, "freezeReason", "");
                if (matchesFreezeKey(c, key)) {
                    double risk = 0.0d;
                    if ("HARD_FREEZE".equals(level)) risk = 1.0d;
                    else if ("SOFT_FREEZE".equals(level)) risk = 0.78d;
                    else if ("RECOVERING".equals(level)) risk = 0.18d;
                    else if ("WATCH".equals(level)) risk = 0.10d;
                    if (risk > maxRisk) {
                        maxRisk = risk;
                        matched = key;
                        matchedLevel = level;
                        matchedReason = reason;
                    }
                }
            }
            if (maxRisk > 0.0d) {
                r.freezeKey = matched;
                r.freezeLevel = matchedLevel;
                r.freezeReason = matchedReason;
                addSignal(r, "FREEZE", "候选命中冻结规则：" + matched + " / " + matchedLevel, 0.0d, maxRisk);
                return maxRisk;
            }
        }

        return 0.0d;
    }

    private boolean matchesFreezeKey(AdaptiveJudgeCandidateSnapshot c, String key) {
        if (c == null || key == null || key.length() == 0) return false;
        if (containsDomainToken(key, "P" + c.position)) return true;
        if (key.indexOf(c.finalPBucket) >= 0) return true;
        if (key.indexOf(c.edgeBucket) >= 0) return true;
        if (c.teacherBucket.length() > 0 && key.indexOf(shortBucket(c.teacherBucket)) >= 0)
            return true;
        if (c.v20Bucket.length() > 0 && key.indexOf(shortBucket(c.v20Bucket)) >= 0) return true;
        if (c.windowBucket.length() > 0 && key.indexOf(shortBucket(c.windowBucket)) >= 0)
            return true;
        if (c.comboDomain.length() > 0 && key.indexOf(shortBucket(c.comboDomain)) >= 0) return true;
        return false;
    }

    private double computeHistoryScore(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        if (c == null || ctx == null) return 0.0d;

        double score = 0.50d;
        score += domainHistoryAdjustment(ctx.positionMap, "P" + c.position, "POSITION", r);
        score += domainHistoryAdjustment(ctx.finalPMap, c.finalPBucket, "FINALP", r);
        score += domainHistoryAdjustment(ctx.edgeMap, c.edgeBucket, "EDGE", r);
        score += domainHistoryAdjustment(ctx.teacherMap, c.teacherBucket, "TEACHER", r);
        score += domainHistoryAdjustment(ctx.v20Map, c.v20Bucket, "V20", r);
        score += domainHistoryAdjustment(ctx.windowMap, c.windowBucket, "WINDOW", r);
        score += domainHistoryAdjustment(ctx.comboMap, c.comboDomain, "COMBO", r);

        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private double domainHistoryAdjustment(
            HashMap<String, AdaptiveJudgeDomainStats> map,
            String key,
            String name,
            AdaptiveJudgeResult r) {
        if (map == null || key == null) return 0.0d;
        AdaptiveJudgeDomainStats d = map.get(normalizeBucket(key));
        if (d == null || d.total < 8) return 0.0d;
        double rate = d.rate();
        if (d.total >= 18 && rate >= 0.380d) {
            double add = Math.min(0.06d, (rate - BASE_K3_RATE) * 0.28d);
            addSignal(r, "HISTORY_" + name, name + "历史域有效 " + d.shortText(), add, 0.0d);
            return add;
        }
        if (d.total >= 18 && rate <= 0.270d) {
            double sub = -Math.min(0.08d, (BASE_K3_RATE - rate) * 0.35d + 0.02d);
            addSignal(r, "HISTORY_" + name, name + "历史域低效 " + d.shortText(), 0.0d, -sub);
            return sub;
        }
        return 0.0d;
    }

    private void applyFinalStatus(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        if (r == null || c == null) return;
        if (isDynamicHardFreeze(c, r)) {
            r.status = STATUS_FREEZE;
            r.action = ACTION_DYNAMIC_FREEZE;
            r.pass = 0;
            r.freeze = 1;
            r.downgradeToShadow = 1;
            r.reason = "动态窗口簇/NeedSupply/反事实命中硬冻结，禁止真实 BET";
            return;
        }
        if (isDynamicReady(c, r)
                && r.judgeScore >= WATCH_SCORE
                && r.judgeRisk < 0.560d
                && r.freezeRisk < 0.450d) {
            r.status = STATUS_DYNAMIC_READY;
            r.action = ACTION_DYNAMIC_READY_REVIEW;
            r.pass = 1;
            r.promoteShadowCandidate = 1;
            r.downgradeToShadow = 0;
            r.reason = "动态窗口簇/NeedSupply/反事实通过，可进入 DecisionGate 复审";
            return;
        }

        if (r.freezeRisk >= FREEZE_RISK) {
            r.status = STATUS_FREEZE;
            r.action = ACTION_HOLD_OR_SHADOW;
            r.pass = 0;
            r.freeze = 1;
            r.downgradeToShadow = 1;
            r.reason = "命中冻结矩阵，禁止真实BET";
            return;
        }

        if (isTeacherRisk(c) || hasV20Risk(c)) {
            r.status = STATUS_REJECT_FAKE_STRONG;
            r.action = ACTION_SHADOW_ONLY;
            r.pass = 0;
            r.freeze = 0;
            r.downgradeToShadow = 1;
            r.reason = "Teacher/V20存在风险，表面强也不能真实BET";
            return;
        }

        if (r.judgeRisk >= FAKE_STRONG_RISK && c.finalP >= 0.390d) {
            r.status = STATUS_REJECT_FAKE_STRONG;
            r.action = ACTION_SHADOW_ONLY;
            r.pass = 0;
            r.downgradeToShadow = 1;
            r.reason = "FinalP表面较高但反证/模块/冻结风险过高，判定假强";
            return;
        }

        /* NO_SAMPLE/WATCH/RECOVERING是真中性：不得升级为FREEZE。命中晋级域时送DecisionGate受控探针复审。 */
        boolean neutralTeacher = isTeacherNoSampleNeutral(c);
        boolean promotionReview =
                r.shadowPromotionScore >= 0.15d
                        || containsPositivePromotion(c.shadowPromotionStatus);
        if (neutralTeacher
                && r.freezeRisk < 0.45d
                && r.judgeRisk < DOWNGRADE_RISK
                && !isTeacherRisk(c)
                && !hasV20Risk(c)) {
            r.status = STATUS_WATCH;
            r.action = promotionReview ? ACTION_REVIEW_BY_DECISION_GATE : ACTION_SHADOW_ONLY;
            r.pass = promotionReview ? 1 : 0;
            r.promoteShadowCandidate = promotionReview ? 1 : 0;
            r.downgradeToShadow = promotionReview ? 0 : 1;
            r.reason =
                    promotionReview
                            ? "Teacher无样本中性且晋级域有效：不冻结，送DecisionGate受控探针复审"
                            : "Teacher无样本中性：保持WATCH，不作为硬拒绝";
            return;
        }

        if (r.freezeRisk >= 0.45d || r.judgeRisk >= DOWNGRADE_RISK) {
            r.status = STATUS_DOWNGRADE_TO_SHADOW;
            r.action = ACTION_SHADOW_ONLY;
            r.pass = 0;
            r.downgradeToShadow = 1;
            r.reason = "风险高于降级线，建议SHADOW观察";
            return;
        }

        if (r.shadowPromotionScore >= 0.22d && r.judgeScore >= WATCH_SCORE && r.judgeRisk < 0.56d) {
            r.status = STATUS_PROMOTE_SHADOW_CANDIDATE;
            r.action = ACTION_ALLOW_DECISION_GATE;
            r.pass = 1;
            r.promoteShadowCandidate = 1;
            r.downgradeToShadow = 0;
            r.reason = "命中SHADOW晋级强域，可提高排序并进入DecisionGate复审";
            return;
        }

        if (r.judgeScore >= PASS_SCORE
                && r.judgeRisk < 0.52d
                && r.freezeRisk < 0.35d
                && c.finalP >= Math.max(0.360d, c.dynamicGate - 0.005d)
                && c.edge >= -0.003d) {
            r.status = STATUS_PASS;
            r.action = ACTION_ALLOW_DECISION_GATE;
            r.pass = 1;
            r.downgradeToShadow = 0;
            r.reason = "历史同类、模块评分、风险控制均允许进入DecisionGate";
            return;
        }

        if (r.judgeScore >= WATCH_SCORE) {
            r.status = STATUS_WATCH;
            r.action = ACTION_SHADOW_ONLY;
            r.pass = 0;
            r.downgradeToShadow = 1;
            r.reason = "信号有观察价值，但不足以支持真实BET";
            return;
        }

        r.status = STATUS_DOWNGRADE_TO_SHADOW;
        r.action = ACTION_SHADOW_ONLY;
        r.pass = 0;
        r.downgradeToShadow = 1;
        r.reason = "证据不足，降级为SHADOW";
    }

    private void finalizeRejectCode(AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeResult r) {
        if (r == null) return;
        if (STATUS_FREEZE.equals(r.status) || ACTION_DYNAMIC_FREEZE.equals(r.action)) {
            r.rejectCode = REJECT_FREEZE;
            return;
        }
        if (c != null && c.edge < 0.0d) {
            r.rejectCode = REJECT_NEGATIVE_EDGE;
            return;
        }
        if (c != null && isTeacherRisk(c)) {
            r.rejectCode = REJECT_TEACHER_RISK;
            return;
        }
        if (c != null && hasV20Risk(c)) {
            r.rejectCode = REJECT_V20_RISK;
            return;
        }
        if (c != null && (c.ruleDecayRisk >= 0.62d || containsRiskWord(c.ruleRejectCode))) {
            r.rejectCode = REJECT_RULE_DECAY;
            return;
        }
        if (c != null
                && (c.thirdDragRisk >= 0.62d
                        || c.thirdWeakness >= 0.62d
                        || containsRiskWord(c.thirdRejectCode))) {
            r.rejectCode = REJECT_THIRD_DRAG;
            return;
        }
        if (c != null
                && (c.counterfactualRisk >= 0.68d
                        || containsRiskWord(c.counterfactualRejectCode))) {
            r.rejectCode = REJECT_COUNTERFACTUAL_RISK;
            return;
        }
        if (c != null && isTeacherNoSampleNeutral(c)) {
            r.rejectCode = REJECT_NONE;
            return;
        }
        r.rejectCode = REJECT_NONE;
    }

    private void buildJudgeText(
            AdaptiveJudgeCandidateSnapshot c, AdaptiveJudgeContext ctx, AdaptiveJudgeResult r) {
        if (r == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("【V20智能审判结果】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("状态=").append(r.status).append("，动作建议=").append(r.action).append("\n");
        sb.append("位置=P")
                .append(r.position)
                .append("，号码=")
                .append(c == null ? "" : c.n1 + "/" + c.n2 + "/" + c.n3)
                .append("，FinalP=")
                .append(pct(r.finalP))
                .append("，Gate=")
                .append(pct(r.dynamicGate))
                .append("，Edge=")
                .append(signedPct(r.edge))
                .append("\n");
        sb.append("JudgeScore=")
                .append(pct(r.judgeScore))
                .append("，RejectCode=")
                .append(safe(r.rejectCode))
                .append("，Risk=")
                .append(pct(r.judgeRisk))
                .append("，ModuleScore=")
                .append(pct(r.moduleScore))
                .append("，ShadowPromotion=")
                .append(pct(r.shadowPromotionScore))
                .append("，FreezeRisk=")
                .append(pct(r.freezeRisk))
                .append("，History=")
                .append(pct(r.historyScore))
                .append("\n");
        sb.append("动态证据：cluster=")
                .append(c == null ? "" : c.dynamicClusterBucket)
                .append("，NeedSupply=")
                .append(c == null ? "" : c.needSupplyBucket)
                .append("，Counterfactual=")
                .append(c == null ? "" : c.counterfactualBucket)
                .append("\n");
        sb.append("原因=").append(safe(r.reason)).append("\n");

        if (r.freezeKey != null && r.freezeKey.length() > 0) {
            sb.append("冻结命中=")
                    .append(r.freezeKey)
                    .append("，级别=")
                    .append(r.freezeLevel)
                    .append("，原因=")
                    .append(limit(r.freezeReason, 260))
                    .append("\n");
        }

        sb.append("证据信号：\n");
        for (int i = 0; i < r.signals.size(); i++) {
            AdaptiveJudgeSignal s = r.signals.get(i);
            if (s == null) continue;
            sb.append(i + 1).append(". ").append(s.text()).append("\n");
        }

        sb.append("红线=本审判不直接BET，只给DecisionGateEngine最终裁决使用。\n");

        r.detail = limit(sb.toString(), MAX_REPORT_LEN);
        r.moduleAdvice = buildModuleAdvice(r);
    }

    private String buildModuleAdvice(AdaptiveJudgeResult r) {
        if (r == null) return "";
        String advice = "";
        if (STATUS_FREEZE.equals(r.status)) {
            advice = append(advice, "DynamicFreezeMatrixEngine命中冻结，DecisionGate应HOLD或SHADOW");
        }
        if (STATUS_REJECT_FAKE_STRONG.equals(r.status)) {
            advice = append(advice, "ProbabilityEngine不要仅凭FinalP高放行，Teacher/V20反证必须折扣");
        }
        if (STATUS_PROMOTE_SHADOW_CANDIDATE.equals(r.status)) {
            advice = append(advice, "ShadowPromotionVerifierEngine命中晋级域，可提高候选排序，但仍需DecisionGate复审");
        }
        if (STATUS_WATCH.equals(r.status) || STATUS_DOWNGRADE_TO_SHADOW.equals(r.status)) {
            advice = append(advice, "建议保存SHADOW观察，不计真实胜率盈亏");
        }
        if (advice.length() == 0) {
            advice = "允许进入DecisionGate，但不得绕过FinalP/Gate/结算红线";
        }
        return advice;
    }

    private void addSignal(
            AdaptiveJudgeResult r, String type, String text, double support, double risk) {
        if (r == null) return;
        if (r.signals.size() >= MAX_SIGNALS) return;
        AdaptiveJudgeSignal s = new AdaptiveJudgeSignal();
        s.type = safe(type);
        s.text = safe(text);
        s.support = support;
        s.risk = risk;
        s.weight = support - risk;
        r.signals.add(s);
    }

    private void addReportCount(AdaptiveJudgeReport report, AdaptiveJudgeResult r) {
        if (report == null || r == null) return;
        if (STATUS_PASS.equals(r.status)) report.passCount++;
        else if (STATUS_WATCH.equals(r.status)) report.watchCount++;
        else if (STATUS_DOWNGRADE_TO_SHADOW.equals(r.status)) report.downgradeCount++;
        else if (STATUS_FREEZE.equals(r.status)) report.freezeCount++;
        else if (STATUS_REJECT_FAKE_STRONG.equals(r.status)) report.fakeStrongCount++;
        else if (STATUS_PROMOTE_SHADOW_CANDIDATE.equals(r.status)) report.promoteCount++;
    }

    private String buildReportSummary(AdaptiveJudgeReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("智能审判FIX16：候选=")
                .append(r.results.size())
                .append(" PASS=")
                .append(r.passCount)
                .append(" WATCH=")
                .append(r.watchCount)
                .append(" DOWNGRADE=")
                .append(r.downgradeCount)
                .append(" FREEZE=")
                .append(r.freezeCount)
                .append(" FAKE=")
                .append(r.fakeStrongCount)
                .append(" PROMOTE=")
                .append(r.promoteCount);
        return sb.toString();
    }

    private String buildReportDetail(AdaptiveJudgeReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("【V20智能动态学习审判报告】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("口径=只审判，不直接BET；真实BET仍由DecisionGateEngine唯一决定。\n\n");
        sb.append("汇总：").append(buildReportSummary(r)).append("\n\n");

        if (r.results.size() == 0) {
            sb.append("当前未传入候选列表：FIX16按中性状态保存，不输出硬杀、不生成BET、不救援BET。\n");
        } else {
            for (int i = 0; i < r.results.size(); i++) {
                AdaptiveJudgeResult jr = r.results.get(i);
                if (jr == null) continue;
                sb.append(i + 1)
                        .append(". ")
                        .append(jr.status)
                        .append(" P")
                        .append(jr.position)
                        .append(" score=")
                        .append(pct(jr.judgeScore))
                        .append(" risk=")
                        .append(pct(jr.judgeRisk))
                        .append(" reason=")
                        .append(jr.reason)
                        .append("\n");
            }
        }
        return limit(sb.toString(), MAX_REPORT_LEN);
    }

    private String buildCompactJson(AdaptiveJudgeReport r) {
        if (r == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"version\":\"").append(jsonSafe(r.version)).append("\",");
        sb.append("\"total\":").append(r.results.size()).append(",");
        sb.append("\"pass\":").append(r.passCount).append(",");
        sb.append("\"watch\":").append(r.watchCount).append(",");
        sb.append("\"downgrade\":").append(r.downgradeCount).append(",");
        sb.append("\"freeze\":").append(r.freezeCount).append(",");
        sb.append("\"fakeStrong\":").append(r.fakeStrongCount).append(",");
        sb.append("\"promote\":").append(r.promoteCount);
        sb.append("}");
        return sb.toString();
    }

    private Object tryCallEngine(String className, String methodName, DbStore store) {
        if (className == null || methodName == null || store == null) return null;
        try {
            Class cls = Class.forName(className);
            Object engine = cls.newInstance();
            Method m = cls.getMethod(methodName, new Class[] {DbStore.class});
            return m.invoke(engine, new Object[] {store});
        } catch (Throwable e) {
        }
        return null;
    }

    private boolean isTeacherStrong(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return false;
        String s = upper(c.teacherBucket);
        if (s.indexOf("STRONG") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("强") >= 0
                || s.indexOf("准备") >= 0) return true;
        if (c.teacherMatrixSample >= 60
                && c.teacherMatrixHitRate >= 0.360d
                && c.teacherMatrixConfidence >= 0.50d) return true;
        return c.teacherEvidenceContribution > c.teacherCounterContribution + 0.080d
                && c.teacherEvidenceContribution > 0.0d;
    }

    private boolean isTeacherRisk(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null || isTeacherNoSampleNeutral(c)) return false;
        if (c.hardBlockFlag == 1 && "TeacherNumberMatrixEngine".equals(c.hardBlockSource))
            return true;
        String s = upper(c.teacherBucket);
        boolean explicitFreeze =
                s.indexOf("FREEZE") >= 0 || s.indexOf("HARD_BLOCK") >= 0 || s.indexOf("冻结") >= 0;
        boolean settledEnough = c.teacherMatrixSample >= 30;
        if (explicitFreeze && settledEnough) return true;
        if (settledEnough && c.teacherMatrixRisk >= 0.72d) return true;
        return settledEnough
                && c.teacherCounterContribution > c.teacherEvidenceContribution + 0.160d
                && c.teacherCounterContribution >= 0.62d;
    }

    private boolean isTeacherNoSampleNeutral(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return true;
        String s = upper(c.teacherBucket);
        if (s.length() == 0 || "空".equals(s)) return true;
        if (s.indexOf("NO_SAMPLE") >= 0
                || s.indexOf("NOSAMPLE") >= 0
                || s.indexOf("NO SAMPLE") >= 0) return true;
        if (s.indexOf("NONE") >= 0 || s.indexOf("EMPTY") >= 0 || s.indexOf("UNKNOWN") >= 0)
            return true;
        return c.teacherMatrixSample <= 0
                && c.teacherMatrixHitRate <= 0.0d
                && c.teacherMatrixRisk <= 0.0d
                && c.teacherEvidenceContribution <= 0.0d
                && c.teacherCounterContribution <= 0.0d;
    }

    private boolean isTeacherWeakOrEmpty(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return false;
        if (isTeacherNoSampleNeutral(c)) return false;
        String s = upper(c.teacherBucket);
        if (s.indexOf("WEAK") >= 0 || s.indexOf("弱") >= 0) return true;
        return c.teacherMatrixSample > 0 && c.teacherMatrixSample < 30;
    }

    private boolean hasV20Support(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return false;
        String s = upper(c.v20Bucket);
        if (s.length() == 0 || "空".equals(s)) return false;
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("风险") >= 0
                || s.indexOf("冻结") >= 0) return false;
        if (s.indexOf("SUPPORT") >= 0
                || s.indexOf("PASS") >= 0
                || s.indexOf("STRONG") >= 0
                || s.indexOf("READY") >= 0) return true;
        if (s.indexOf("有效") >= 0 || s.indexOf("支持") >= 0 || s.indexOf("强") >= 0) return true;
        if (c.evidenceContribution > c.counterContribution + 0.060d
                && c.evidenceContribution > 0.0d) return true;
        if (c.v20SupportScore > c.v20RiskScore + 0.060d && c.v20SupportScore > 0.0d) return true;
        return c.decisionEvidenceStrong > 0 || c.decisionEvidenceElite > 0;
    }

    private boolean hasV20Risk(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return false;
        if (c.hardBlockFlag == 1 && AppConfig.isExplicitHardBlockStatus(c.riskSeverity))
            return true;
        String s = upper(c.v20Bucket);
        boolean explicitFreeze =
                s.indexOf("FREEZE") >= 0 || s.indexOf("HARD_BLOCK") >= 0 || s.indexOf("冻结") >= 0;
        if (explicitFreeze && (c.decisionHardRisk > 0 || c.v20RiskScore >= 0.72d)) return true;
        if (c.decisionHardRisk > 0 && c.decisionCounterRiskHigh > 0) return true;
        return c.v20RiskScore >= 0.78d && c.counterContribution >= 0.62d;
    }

    private boolean hasTrustedWindow(AdaptiveJudgeCandidateSnapshot c) {
        if (c == null) return false;
        String s = c.windowBucket;
        if (s == null || s.length() == 0 || "空".equals(s)) return false;
        if (c.similarWindowSample >= 30) return true;
        if (c.effectiveSample >= 60) return true;
        if (c.comboSample >= 60) return true;
        return c.sampleReliability >= 0.60d && c.recentValidity >= 0.45d;
    }

    private boolean isPromotionReady(String status) {
        String s = upper(status);
        return s.indexOf("PROMOTE") >= 0 || s.indexOf("READY") >= 0 || s.indexOf("STRONG") >= 0;
    }

    private boolean isPromotionBlocked(String status) {
        String s = upper(status);
        return s.indexOf("BLOCK") >= 0 || s.indexOf("FREEZE") >= 0 || s.indexOf("HARD_BLOCK") >= 0;
    }

    private boolean containsRiskWord(String s) {
        return AppConfig.isExplicitHardBlockStatus(s);
    }

    private double clampSignedForField(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v < -1.0d) return -1.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private AdaptiveJudgeCandidateSnapshot makeCandidateSnapshot(Object o) {
        AdaptiveJudgeCandidateSnapshot c = new AdaptiveJudgeCandidateSnapshot();
        if (o == null) return c;

        c.position = getInt(o, "position", 0);
        c.n1 = getInt(o, "n1", 0);
        c.n2 = getInt(o, "n2", 0);
        c.n3 = getInt(o, "n3", 0);
        c.finalP = getDouble(o, "finalP", 0.0d);
        c.rawP = getDouble(o, "rawP", 0.0d);
        c.calibratedP = getDouble(o, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(o, "dynamicGate", 0.0d);
        c.edge = getDouble(o, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;

        c.effectiveSample = getInt(o, "effectiveSample", 0);
        c.comboSample = getInt(o, "comboSample", 0);
        c.similarWindowSample =
                firstNonZeroInt(
                        getInt(o, "similarWindowSample", 0),
                        getInt(o, "teacherSimilarWindowSample", 0),
                        0);
        c.sampleReliability =
                firstNonZero(
                        getDouble(o, "sampleReliability", 0.0d),
                        getDouble(o, "decisionSampleReliability", 0.0d),
                        getDouble(o, "dynamicGateSampleReliability", 0.0d),
                        0.0d);
        c.recentValidity =
                firstNonZero(
                        getDouble(o, "recentValidity", 0.0d),
                        getDouble(o, "decisionRecentValidity", 0.0d),
                        getDouble(o, "dynamicGateRecentValidity", 0.0d),
                        0.0d);
        c.featureDistance =
                firstNonZero(
                        getDouble(o, "featureDistance", 0.0d),
                        getDouble(o, "decisionFeatureDistance", 0.0d),
                        getDouble(o, "dynamicGateFeatureDistance", 0.0d),
                        0.0d);
        c.similarWindowHitRate = getDouble(o, "similarWindowHitRate", 0.0d);
        c.comboHitRate = getDouble(o, "comboHitRate", 0.0d);

        c.teacherMatrixStatus = getString(o, "teacherMatrixStatus", "");
        c.teacherNumberStatus = getString(o, "teacherNumberStatus", "");
        c.teacherStatus = getString(o, "teacherStatus", "");
        c.teacherMatrixSample = getInt(o, "teacherMatrixSample", 0);
        c.teacherMatrixHitRate = getDouble(o, "teacherMatrixHitRate", 0.0d);
        c.teacherMatrixConfidence = getDouble(o, "teacherMatrixConfidence", 0.0d);
        c.teacherMatrixRisk = getDouble(o, "teacherMatrixRisk", 0.0d);
        c.teacherEvidenceContribution =
                firstNonZero(
                        getDouble(o, "teacherEvidenceContribution", 0.0d),
                        getDouble(o, "teacherMatrixEvidenceContribution", 0.0d),
                        0.0d,
                        0.0d);
        c.teacherCounterContribution =
                firstNonZero(
                        getDouble(o, "teacherCounterContribution", 0.0d),
                        getDouble(o, "teacherMatrixCounterContribution", 0.0d),
                        0.0d,
                        0.0d);
        c.teacherBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus));

        c.v20Status =
                firstNonEmpty(getString(o, "v20Status", ""), getString(o, "v20CauseStatus", ""));
        c.dynamicStatus =
                firstNonEmpty(
                        getString(o, "dynamicStatus", ""), getString(o, "dynamicCauseStatus", ""));
        c.v20SupportScore =
                firstNonZero(
                        getDouble(o, "v20SupportScore", 0.0d),
                        getDouble(o, "dynamicSupportScore", 0.0d),
                        0.0d,
                        0.0d);
        c.v20RiskScore =
                firstNonZero(
                        getDouble(o, "v20RiskScore", 0.0d),
                        getDouble(o, "dynamicRiskScore", 0.0d),
                        0.0d,
                        0.0d);
        c.evidenceContribution =
                firstNonZero(
                        getDouble(o, "evidenceContribution", 0.0d),
                        getDouble(o, "decisionEvidenceContribution", 0.0d),
                        getDouble(o, "dynamicGateEvidenceContribution", 0.0d),
                        0.0d);
        c.counterContribution =
                firstNonZero(
                        getDouble(o, "counterContribution", 0.0d),
                        getDouble(o, "decisionCounterContribution", 0.0d),
                        getDouble(o, "dynamicGateCounterContribution", 0.0d),
                        0.0d);
        c.falseStrongRisk = getDouble(o, "falseStrongRisk", 0.0d);
        c.decisionHardRisk = getInt(o, "decisionHardRisk", 0);
        c.decisionCounterRiskHigh = getInt(o, "decisionCounterRiskHigh", 0);
        c.decisionEvidenceStrong = getInt(o, "decisionEvidenceStrong", 0);
        c.decisionEvidenceElite = getInt(o, "decisionEvidenceElite", 0);
        c.v20Bucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.v20Status, c.dynamicStatus, getString(o, "actionAdvice", "")));

        c.windowStateKey = getString(o, "windowStateKey", "");
        c.dynamicWindowStateKey = getString(o, "dynamicWindowStateKey", "");
        c.probabilityWindowStateKey = getString(o, "probabilityWindowStateKey", "");
        c.windowBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.windowStateKey,
                                c.dynamicWindowStateKey,
                                c.probabilityWindowStateKey));

        c.finalPBucket = finalPBucket(c.finalP);
        c.edgeBucket = edgeBucket(c.edge);
        c.k3RankPattern = normalizeBucket(getString(o, "k3RankPattern", "K3_UNKNOWN"));
        c.comboDomain =
                "P"
                        + c.position
                        + "|"
                        + c.finalPBucket
                        + "|"
                        + shortBucket(c.teacherBucket)
                        + "|"
                        + shortBucket(c.v20Bucket)
                        + "|"
                        + shortBucket(c.windowBucket)
                        + "|"
                        + c.edgeBucket;

        c.dynamicClusterId = getString(o, "dynamicClusterId", "");
        c.dynamicClusterStatus = getString(o, "dynamicClusterStatus", "");
        c.dynamicClusterHitRate = getDouble(o, "dynamicClusterHitRate", 0.0d);
        c.dynamicClusterSample = getInt(o, "dynamicClusterSample", 0);
        c.dynamicClusterMaxLose = getInt(o, "dynamicClusterMaxLose", 0);
        c.needSupplyMatchScore = getDouble(o, "needSupplyMatchScore", 0.0d);
        c.needSupplyThirdQuality = getDouble(o, "needSupplyThirdQuality", 0.0d);
        c.needSupplyRisk = getDouble(o, "needSupplyRisk", 0.0d);
        c.counterfactualWhyNotRisk = getDouble(o, "counterfactualWhyNotRisk", 0.0d);
        c.causeAblationScore = getDouble(o, "causeAblationScore", 0.0d);
        c.causeAblationFalseCauseCount = getInt(o, "causeAblationFalseCauseCount", 0);
        c.dynamicClusterBucket =
                normalizeBucket(firstNonEmpty(c.dynamicClusterId, c.dynamicClusterStatus));
        c.needSupplyBucket = needSupplyBucket(c.needSupplyMatchScore, c.needSupplyRisk);
        c.counterfactualBucket =
                counterfactualBucket(
                        c.counterfactualWhyNotRisk,
                        c.causeAblationScore,
                        c.causeAblationFalseCauseCount);
        c.shadowPromotionStatus =
                getString(o, "shadowPromotionStatus", getString(o, "promotionStatus", ""));
        c.shadowPromoteScore =
                firstNonZero(
                        getDouble(o, "shadowPromoteScore", 0.0d),
                        getDouble(o, "shadowPromotionScore", 0.0d),
                        0.0d,
                        0.0d);
        c.shadowRejectCode =
                firstNonEmpty(
                        getString(o, "shadowRejectCode", ""),
                        getString(o, "shadowPromotionRejectCode", ""));
        c.promotionSampleCount =
                firstNonZeroInt(
                        getInt(o, "promotionSampleCount", 0),
                        getInt(o, "shadowPromotionSampleCount", 0),
                        0);
        c.shadowPromotionHitRate = getDouble(o, "shadowPromotionHitRate", 0.0d);
        c.shadowPromotionRecentHitRate = getDouble(o, "shadowPromotionRecentHitRate", 0.0d);
        c.shadowPromotionReliability = getDouble(o, "shadowPromotionReliability", 0.0d);
        c.ruleHealthScore = getDouble(o, "ruleHealthScore", 0.0d);
        c.ruleHealthRisk = getDouble(o, "ruleHealthRisk", 0.0d);
        c.ruleDecayRisk = getDouble(o, "ruleDecayRisk", 0.0d);
        c.ruleRejectCode =
                firstNonEmpty(
                        getString(o, "ruleRejectCode", ""),
                        getString(o, "ruleHealthRejectCode", ""));
        c.ruleActionAdvice =
                firstNonEmpty(
                        getString(o, "ruleActionAdvice", ""),
                        getString(o, "ruleHealthActionAdvice", ""));
        c.thirdQuality =
                firstNonZero(
                        getDouble(o, "thirdQuality", 0.0d),
                        getDouble(o, "thirdNumberQualityScore", 0.0d),
                        c.needSupplyThirdQuality,
                        0.0d);
        c.thirdDragRisk =
                firstNonZero(
                        getDouble(o, "thirdDragRisk", 0.0d),
                        getDouble(o, "thirdDragScore", 0.0d),
                        0.0d,
                        0.0d);
        c.thirdWeakness = getDouble(o, "thirdWeakness", 0.0d);
        c.thirdRejectCode =
                firstNonEmpty(
                        getString(o, "thirdRejectCode", ""),
                        getString(o, "thirdGuardRejectCode", ""));
        c.counterfactualRisk =
                firstNonZero(
                        getDouble(o, "counterfactualRisk", 0.0d),
                        c.counterfactualWhyNotRisk,
                        0.0d,
                        0.0d);
        c.rankStability = getDouble(o, "rankStability", 0.0d);
        c.replaceGain = getDouble(o, "replaceGain", 0.0d);
        c.counterfactualRejectCode =
                firstNonEmpty(
                        getString(o, "counterfactualRejectCode", ""),
                        getString(o, "counterfactualRankRejectCode", ""));
        c.positionNeedScore = getDouble(o, "positionNeedScore", 0.0d);
        c.numberSupplyScore = getDouble(o, "numberSupplyScore", 0.0d);
        c.coverageQuality = getDouble(o, "coverageQuality", 0.0d);
        c.hardBlockFlag = getInt(o, "hardBlockFlag", 0);
        c.riskSeverity = getString(o, "riskSeverity", "");
        c.hardBlockSource = getString(o, "hardBlockSource", "");
        c.comboDomain =
                c.comboDomain
                        + "|CL="
                        + shortBucket(c.dynamicClusterBucket)
                        + "|NS="
                        + shortBucket(c.needSupplyBucket)
                        + "|CF="
                        + shortBucket(c.counterfactualBucket)
                        + "|SP="
                        + shortBucket(c.shadowPromotionStatus)
                        + "|RH="
                        + shortBucket(c.ruleRejectCode)
                        + "|TQ="
                        + shortBucket(c.thirdRejectCode);
        return c;
    }

    private ArrayList<AdaptiveJudgeHistoryCase> loadRecentRealBetCases(DbStore store, int limit) {
        ArrayList<AdaptiveJudgeHistoryCase> out = new ArrayList<AdaptiveJudgeHistoryCase>();
        if (store == null) return out;

        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();

        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) break;
            Models.Decision d = decisions.get(i);
            if (d == null || !isBetDecision(d)) continue;
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue)) continue;
            Models.Trade t = getTradeByTargetIssueSafe(store, issue);
            if (t == null || !isRealBetTrade(t)) continue;
            AdaptiveJudgeHistoryCase c = fromTrade(store, t, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        if (out.size() < limit) {
            ArrayList<Models.Trade> trades = loadRecentTradesByReflection(store, limit * 2);
            for (int i = 0; i < trades.size(); i++) {
                if (out.size() >= limit) break;
                Models.Trade t = trades.get(i);
                if (t == null || !isRealBetTrade(t)) continue;
                String issue = safe(getString(t, "targetIssue", ""));
                if (issue.length() == 0 || used.containsKey(issue)) continue;
                Models.Decision d = getDecisionByTargetIssueSafe(store, issue);
                AdaptiveJudgeHistoryCase c = fromTrade(store, t, d);
                if (c != null) {
                    out.add(c);
                    used.put(issue, "1");
                }
            }
        }

        sortHistoryDesc(out);
        if (out.size() > limit) {
            return new ArrayList<AdaptiveJudgeHistoryCase>(out.subList(0, limit));
        }
        return out;
    }

    private AdaptiveJudgeHistoryCase fromTrade(DbStore store, Models.Trade t, Models.Decision d) {
        if (t == null) return null;
        AdaptiveJudgeHistoryCase c = new AdaptiveJudgeHistoryCase();
        c.kind = "BET";
        c.targetIssue = safe(getString(t, "targetIssue", ""));
        c.baseIssue = safe(getString(t, "baseIssue", ""));
        c.createdAt = getLong(t, "createdAt", 0L);
        if (c.createdAt <= 0L) c.createdAt = getLong(t, "betTime", 0L);
        c.position = getInt(t, "position", 0);
        c.n1 = getInt(t, "n1", 0);
        c.n2 = getInt(t, "n2", 0);
        c.n3 = getInt(t, "n3", 0);
        c.actualNumber = getInt(t, "actualNumber", 0);
        c.settled = getInt(t, "settled", 0);
        c.result = normalizeResult(getString(t, "result", ""));
        c.hit = isWinResult(c.result) ? 1 : 0;
        c.finalP = getDouble(t, "finalP", 0.0d);
        c.dynamicGate = getDouble(t, "dynamicGate", 0.0d);
        c.edge = getDouble(t, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;
        c.teacherBucket =
                normalizeBucket(
                        firstNonEmpty(
                                getString(t, "teacherStatus", ""),
                                getString(t, "teacherMatrixStatus", ""),
                                getString(t, "teacherNumberStatus", "")));
        c.v20Bucket =
                normalizeBucket(
                        firstNonEmpty(
                                getString(t, "v20Status", ""),
                                getString(t, "dynamicStatus", ""),
                                getString(t, "actionAdvice", "")));
        c.windowBucket =
                normalizeBucket(
                        firstNonEmpty(
                                getString(t, "windowStateKey", ""),
                                getString(t, "dynamicWindowStateKey", ""),
                                getString(t, "probabilityWindowStateKey", "")));

        if (d != null) {
            if (c.finalP <= 0.0d) c.finalP = getDouble(d, "finalP", 0.0d);
            if (c.dynamicGate <= 0.0d) c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
            if (c.edge == 0.0d) c.edge = getDouble(d, "edge", 0.0d);
            if (c.teacherBucket.length() == 0 || "空".equals(c.teacherBucket)) {
                c.teacherBucket =
                        normalizeBucket(
                                firstNonEmpty(
                                        getString(d, "teacherStatus", ""),
                                        getString(d, "teacherMatrixStatus", ""),
                                        getString(d, "teacherNumberStatus", "")));
            }
            if (c.v20Bucket.length() == 0 || "空".equals(c.v20Bucket)) {
                c.v20Bucket =
                        normalizeBucket(
                                firstNonEmpty(
                                        getString(d, "v20Status", ""),
                                        getString(d, "dynamicStatus", ""),
                                        getString(d, "actionAdvice", "")));
            }
            if (c.windowBucket.length() == 0 || "空".equals(c.windowBucket)) {
                c.windowBucket =
                        normalizeBucket(
                                firstNonEmpty(
                                        getString(d, "windowStateKey", ""),
                                        getString(d, "dynamicWindowStateKey", ""),
                                        getString(d, "probabilityWindowStateKey", "")));
            }
        }

        fillActualFromDraw(store, c);
        c.finalPBucket = finalPBucket(c.finalP);
        c.edgeBucket = edgeBucket(c.edge);
        return c;
    }

    private void fillActualFromDraw(DbStore store, AdaptiveJudgeHistoryCase c) {
        if (store == null || c == null) return;
        if (isValidNumber(c.actualNumber) && c.settled == 1) {
            c.hit = containsK3(c, c.actualNumber) ? 1 : 0;
            c.result = c.hit == 1 ? resultWin() : resultMiss();
            return;
        }
        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && isDrawValid(draw) && isValidPosition(c.position)) {
            int n = getNumberAtPositionSafe(draw, c.position);
            if (isValidNumber(n)) {
                c.actualNumber = n;
                c.settled = 1;
                c.hit = containsK3(c, n) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            }
        }
    }

    private String comboDomainKey(AdaptiveJudgeHistoryCase c) {
        if (c == null) return "COMBO:空";
        return "P"
                + c.position
                + "|"
                + c.finalPBucket
                + "|"
                + shortBucket(c.teacherBucket)
                + "|"
                + shortBucket(c.v20Bucket)
                + "|"
                + shortBucket(c.windowBucket)
                + "|"
                + c.edgeBucket;
    }

    private String k3LayerBucket(AdaptiveJudgeHistoryCase c) {
        if (c == null) return "K3空";
        if (c.actualNumber == c.n1) return "RANK1_HIT";
        if (c.actualNumber == c.n2) return "RANK2_HIT";
        if (c.actualNumber == c.n3) return "RANK3_HIT";
        if (c.actualNumber > 0) return "K3_MISS";
        return "K3_WAIT";
    }

    private boolean containsK3(AdaptiveJudgeHistoryCase c, int actual) {
        if (c == null) return false;
        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private ArrayList<Models.Decision> loadRecentDecisions(DbStore store, int limit) {
        ArrayList<Models.Decision> out = new ArrayList<Models.Decision>();
        if (store == null) return out;
        try {
            Method m = store.getClass().getMethod("getRecentDecisions", new Class[] {int.class});
            Object obj = m.invoke(store, new Object[] {Integer.valueOf(limit)});
            if (obj instanceof List) {
                List list = (List) obj;
                for (int i = 0; i < list.size(); i++) {
                    Object o = list.get(i);
                    if (o instanceof Models.Decision) out.add((Models.Decision) o);
                }
            }
        } catch (Throwable e) {
        }
        return out;
    }

    private ArrayList<Models.Trade> loadRecentTradesByReflection(DbStore store, int limit) {
        ArrayList<Models.Trade> out = new ArrayList<Models.Trade>();
        if (store == null) return out;
        String[] names =
                new String[] {
                    "getRecentTrades",
                    "getRecentTradeList",
                    "listRecentTrades",
                    "getTrades",
                    "getAllRecentTrades"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {int.class});
                Object obj = m.invoke(store, new Object[] {Integer.valueOf(limit)});
                if (obj instanceof List) {
                    List list = (List) obj;
                    for (int j = 0; j < list.size(); j++) {
                        Object o = list.get(j);
                        if (o instanceof Models.Trade) out.add((Models.Trade) o);
                    }
                    if (out.size() > 0) return out;
                }
            } catch (Throwable e) {
            }
        }
        return out;
    }

    private Models.Trade getTradeByTargetIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        String[] names =
                new String[] {
                    "getTradeByTargetIssue", "loadTradeByTargetIssue", "getRealTradeByTargetIssue"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Trade) return (Models.Trade) obj;
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private Models.Decision getDecisionByTargetIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        String[] names =
                new String[] {
                    "getDecisionByTargetIssue",
                    "loadDecisionByTargetIssue",
                    "getLatestDecisionByTargetIssue"
                };
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Decision) return (Models.Decision) obj;
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private Models.Draw getDrawByIssueSafe(DbStore store, String issue) {
        if (store == null || issue == null || issue.length() == 0) return null;
        String[] names =
                new String[] {"getDrawByIssue", "loadDrawByIssue", "getDraw", "findDrawByIssue"};
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = store.getClass().getMethod(names[i], new Class[] {String.class});
                Object obj = m.invoke(store, new Object[] {issue});
                if (obj instanceof Models.Draw) return (Models.Draw) obj;
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private boolean isBetDecision(Models.Decision d) {
        if (d == null) return false;
        return isBetText(getString(d, "decision", "")) || isBetText(getString(d, "action", ""));
    }

    private boolean isRealBetTrade(Models.Trade t) {
        if (t == null) return false;
        if (!isRealScope(getString(t, "statScope", ""))) return false;
        String action = safe(getString(t, "action", ""));
        if (action.length() > 0 && !isBetText(action)) return false;
        if (!isValidPosition(getInt(t, "position", 0))) return false;
        if (!isValidNumber(getInt(t, "n1", 0))
                || !isValidNumber(getInt(t, "n2", 0))
                || !isValidNumber(getInt(t, "n3", 0))) return false;
        String issue = safe(getString(t, "targetIssue", ""));
        return issue.length() > 0;
    }

    private boolean isRealScope(String scope) {
        String s = safe(scope).trim();
        if (s.length() == 0) return true;
        if (constEquals("STAT_SCOPE_REAL_TRADE", s)) return true;
        return "REAL".equalsIgnoreCase(s)
                || "REAL_TRADE".equalsIgnoreCase(s)
                || "REAL_BET".equalsIgnoreCase(s)
                || "REALBET".equalsIgnoreCase(s);
    }

    private boolean isBetText(String text) {
        String s = safe(text).trim();
        if (s.length() == 0) return false;
        if (constEquals("DECISION_BET", s)) return true;
        if (constEquals("ACTION_BET", s)) return true;
        return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s);
    }

    private boolean constEquals(String fieldName, String value) {
        if (fieldName == null || value == null) return false;
        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);
            return v != null && value.equals(String.valueOf(v));
        } catch (Throwable e) {
            return false;
        }
    }

    private String resultWin() {
        try {
            Field f = AppConfig.class.getField("RESULT_WIN");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "WIN";
    }

    private String resultMiss() {
        try {
            Field f = AppConfig.class.getField("RESULT_MISS");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "MISS";
    }

    private boolean isWinResult(String r) {
        String s = normalizeResult(r);
        return resultWin().equals(s)
                || "WIN".equalsIgnoreCase(s)
                || "HIT".equalsIgnoreCase(s)
                || "命中".equals(s);
    }

    private boolean isMissResult(String r) {
        String s = normalizeResult(r);
        return resultMiss().equals(s)
                || "MISS".equalsIgnoreCase(s)
                || "LOSE".equalsIgnoreCase(s)
                || "LOSS".equalsIgnoreCase(s)
                || "未中".equals(s)
                || "不中".equals(s);
    }

    private String normalizeResult(String r) {
        String s = safe(r).trim();
        if (s.length() == 0) return "WAIT";
        String u = s.toUpperCase(java.util.Locale.ROOT);
        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(s)) return resultWin();
        if ("MISS".equals(u)
                || "LOSE".equals(u)
                || "LOSS".equals(u)
                || "未中".equals(s)
                || "不中".equals(s)) return resultMiss();
        return s;
    }

    private boolean isValidPosition(int p) {
        try {
            Method m = AppConfig.class.getMethod("isValidPosition", new Class[] {int.class});
            Object obj = m.invoke(null, new Object[] {Integer.valueOf(p)});
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return p >= 1 && p <= POSITION_COUNT;
    }

    private boolean isValidNumber(int n) {
        try {
            Method m = AppConfig.class.getMethod("isValidNumber", new Class[] {int.class});
            Object obj = m.invoke(null, new Object[] {Integer.valueOf(n)});
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return n >= 1 && n <= 10;
    }

    private boolean isDrawValid(Models.Draw draw) {
        if (draw == null) return false;
        try {
            Method m = draw.getClass().getMethod("isValid", new Class[] {});
            Object obj = m.invoke(draw, new Object[] {});
            if (obj instanceof Boolean) return ((Boolean) obj).booleanValue();
        } catch (Throwable e) {
        }
        return true;
    }

    private int getNumberAtPositionSafe(Models.Draw draw, int position) {
        if (draw == null) return 0;
        try {
            Method m = draw.getClass().getMethod("getNumberAtPosition", new Class[] {int.class});
            Object obj = m.invoke(draw, new Object[] {Integer.valueOf(position)});
            if (obj instanceof Number) return ((Number) obj).intValue();
        } catch (Throwable e) {
        }
        return 0;
    }

    private void sortHistoryDesc(ArrayList<AdaptiveJudgeHistoryCase> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<AdaptiveJudgeHistoryCase>() {
                    public int compare(AdaptiveJudgeHistoryCase a, AdaptiveJudgeHistoryCase b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.createdAt < b.createdAt) return 1;
                        if (a.createdAt > b.createdAt) return -1;
                        return safe(b.targetIssue).compareTo(safe(a.targetIssue));
                    }
                });
    }

    private Object getObject(Object obj, String field, Object def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            return v == null ? def : v;
        } catch (Throwable e) {
        }
        return def;
    }

    private double getDouble(Object obj, String field, double def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).doubleValue();
            if (v != null) return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
        }
        return def;
    }

    private int getInt(Object obj, String field, int def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v != null) return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
        }
        return def;
    }

    private long getLong(Object obj, String field, long def) {
        if (obj == null || field == null || field.length() == 0) return def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) return ((Number) v).longValue();
            if (v != null) return Long.parseLong(String.valueOf(v));
        } catch (Throwable e) {
        }
        return def;
    }

    private String getString(Object obj, String field, String def) {
        if (obj == null || field == null || field.length() == 0) return def == null ? "" : def;
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    private double firstNonZero(double a, double b, double c, double d) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        if (c != 0.0d) return c;
        return d;
    }

    private int firstNonZeroInt(int a, int b, int c) {
        if (a != 0) return a;
        if (b != 0) return b;
        return c;
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) return a;
        return b == null ? "" : b;
    }

    private String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        return c == null ? "" : c;
    }

    private String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        return d == null ? "" : d;
    }

    private String needSupplyBucket(double score, double risk) {
        if (score >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE
                && risk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK) return "NS_READY";
        if (score >= AppConfig.NEED_SUPPLY_MIN_MATCH_SCORE) return "NS_WATCH";
        if (risk >= AppConfig.NEED_SUPPLY_HARD_RISK) return "NS_RISK";
        return "NS_WEAK";
    }

    private String counterfactualBucket(
            double whyNotRisk, double ablationScore, int falseCauseCount) {
        if (whyNotRisk >= AppConfig.COUNTERFACTUAL_HARD_WHY_NOT_RISK || falseCauseCount >= 4)
            return "CF_HARD_RISK";
        if (whyNotRisk >= AppConfig.COUNTERFACTUAL_MAX_WHY_NOT_RISK) return "CF_RISK";
        if (ablationScore >= AppConfig.CAUSE_ABLATION_MIN_SCORE && falseCauseCount <= 2)
            return "CF_SUPPORT";
        return "CF_WATCH";
    }

    private String finalPBucket(double p) {
        if (p <= 0.0d) return "FinalP空";
        if (p < 0.340d) return "P<34%";
        if (p < 0.360d) return "P34-36%";
        if (p < 0.380d) return "P36-38%";
        if (p < 0.400d) return "P38-40%";
        if (p < 0.420d) return "P40-42%";
        return "P>=42%";
    }

    private String edgeBucket(double e) {
        if (e < -0.010d) return "Edge<-1%";
        if (e < 0.0d) return "Edge负";
        if (e < 0.010d) return "Edge0-1%";
        if (e < 0.020d) return "Edge1-2%";
        return "Edge>=2%";
    }

    private double finalPScore(double p) {
        if (p >= 0.420d) return 1.0d;
        if (p >= 0.400d) return 0.82d;
        if (p >= 0.380d) return 0.62d;
        if (p >= 0.360d) return 0.42d;
        return 0.18d;
    }

    private double edgeScore(double e) {
        if (e >= 0.030d) return 1.0d;
        if (e >= 0.020d) return 0.78d;
        if (e >= 0.010d) return 0.55d;
        if (e >= 0.0d) return 0.30d;
        return 0.0d;
    }

    private String normalizeBucket(String s) {
        s = safe(s).trim();
        if (s.length() == 0) return "空";
        if (s.length() > 100) s = s.substring(0, 100);
        return s;
    }

    private String shortBucket(String s) {
        s = normalizeBucket(s);
        if (s.length() <= 26) return s;
        return s.substring(0, 26);
    }

    private String append(String a, String b) {
        if (b == null || b.length() == 0) return safe(a);
        if (a == null || a.length() == 0) return b;
        return a + "；" + b;
    }

    private String upper(String s) {
        return safe(s).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private String limit(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private String jsonSafe(String s) {
        s = safe(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\') sb.append("\\\\");
            else if (ch == '"') sb.append("\\\"");
            else if (ch == '\n') sb.append("\\n");
            else if (ch == '\r') sb.append("\\r");
            else if (ch == '\t') sb.append("\\t");
            else sb.append(ch);
        }
        return sb.toString();
    }

    private double clamp01(double v) {
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

    private String pct(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }

    private String signedPct(double v) {
        try {
            return UiFormatter.signedProbability(v);
        } catch (Throwable e) {
            String s = String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
            return v >= 0.0d ? "+" + s : s;
        }
    }

    private void setStateSafe(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) return;
        try {
            Method m =
                    store.getClass()
                            .getMethod("setState", new Class[] {String.class, String.class});
            m.invoke(store, new Object[] {key, value == null ? "" : value});
        } catch (Throwable e) {
        }
    }

    public AdaptiveJudgeReport judgeRealtimeCandidates(
            List<Models.ComboCandidate> candidates, String baseIssue, String targetIssue) {
        AdaptiveJudgeReport report = new AdaptiveJudgeReport();
        report.version = VERSION;
        report.createdAt = System.currentTimeMillis();

        int total = candidates == null ? 0 : candidates.size();
        if (candidates == null || candidates.size() == 0) {
            report.summary = "候选=0，实时未传入候选";
            report.detail = report.summary;
            return report;
        }

        int pass = 0;
        int watch = 0;
        int freeze = 0;
        int downgrade = 0;
        int promote = 0;

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }
            AdaptiveJudgeResult r = judgeOneRealtimeCandidate(c, i + 1);
            if (r == null) {
                continue;
            }
            report.results.add(r);

            if (STATUS_FREEZE.equals(r.status) || ACTION_REJECT.equals(r.action)) {
                freeze++;
            } else if (STATUS_DOWNGRADE_TO_SHADOW.equals(r.status)) {
                downgrade++;
            } else if (STATUS_PASS.equals(r.status)) {
                pass++;
            } else if (STATUS_PROMOTE_SHADOW_CANDIDATE.equals(r.status)) {
                promote++;
            } else {
                watch++;
            }
        }

        report.passCount = pass;
        report.watchCount = watch;
        report.freezeCount = freeze;
        report.downgradeCount = downgrade;
        report.promoteCount = promote;
        report.ok = 1;
        report.summary =
                "实时候选="
                        + total
                        + " 通过="
                        + pass
                        + " WATCH="
                        + watch
                        + " 降级="
                        + downgrade
                        + " 冻结="
                        + freeze
                        + " PROMOTE="
                        + promote
                        + "｜仍由DecisionGate最终裁决";
        report.detail = report.summary;
        return report;
    }

    public AdaptiveJudgeResult judgeOneRealtimeCandidate(
            Models.ComboCandidate candidate, int rank) {
        AdaptiveJudgeResult r = new AdaptiveJudgeResult();
        r.version = VERSION;
        r.createdAt = System.currentTimeMillis();

        if (candidate == null) {
            r.status = STATUS_WATCH;
            r.action = ACTION_SHADOW_ONLY;
            r.reason = "候选为空";
            return r;
        }

        AdaptiveJudgeCandidateSnapshot c = makeCandidateSnapshot(candidate);
        r.candidate = c;
        r.position = c.position;
        r.finalP = c.finalP;
        r.dynamicGate = c.dynamicGate;
        r.edge = c.edge;
        r.teacherStatus =
                firstNonEmpty(c.teacherMatrixStatus, c.teacherStatus, c.teacherNumberStatus);
        r.v20Status = firstNonEmpty(c.v20Status, c.dynamicStatus);
        r.windowStateKey =
                firstNonEmpty(
                        c.dynamicWindowStateKey, c.windowStateKey, c.probabilityWindowStateKey);

        double need = Math.max(c.needSupplyMatchScore, c.numberSupplyScore);
        double third = Math.max(c.needSupplyThirdQuality, c.thirdQuality);
        double counterRisk =
                Math.max(
                        Math.max(c.counterfactualWhyNotRisk, c.counterfactualRisk),
                        c.needSupplyRisk);
        double clusterSupport =
                Math.max(
                        Math.max(c.dynamicClusterHitRate, c.v20SupportScore),
                        Math.max(c.ruleHealthScore, c.shadowPromoteScore));
        double risk =
                Math.max(
                        Math.max(counterRisk, c.ruleHealthRisk),
                        Math.max(c.thirdDragRisk, Math.max(c.teacherMatrixRisk, c.v20RiskScore)));

        boolean teacherRisk = isTeacherRisk(c);
        boolean v20Risk = hasV20Risk(c);
        boolean ready =
                c.finalP >= 0.370d
                        && c.edge >= -0.012d
                        && need >= 0.520d
                        && third >= 0.300d
                        && counterRisk < 0.620d
                        && !teacherRisk
                        && !v20Risk;

        boolean promoteReady =
                ready
                        && (c.finalP >= 0.385d || clusterSupport >= 0.330d || need >= 0.620d)
                        && risk < 0.560d;

        boolean explicitHard =
                c.hardBlockFlag == 1
                        || AppConfig.isExplicitHardBlockStatus(c.riskSeverity)
                        || containsRiskWord(c.ruleRejectCode)
                        || containsRiskWord(c.thirdRejectCode)
                        || containsRiskWord(c.counterfactualRejectCode);
        boolean freeze = explicitHard || teacherRisk || v20Risk || risk >= 0.780d;

        r.evidenceScore =
                clamp01(
                        c.finalP * 0.34d
                                + need * 0.28d
                                + third * 0.16d
                                + Math.max(0.0d, c.edge + 0.020d) * 2.0d
                                + clusterSupport * 0.12d);
        r.judgeRisk = clamp01(risk);
        r.moduleScore = 0.0d;
        r.shadowPromotionScore = promoteReady ? Math.max(0.520d, r.evidenceScore) : 0.0d;
        r.freezeRisk = freeze ? Math.max(0.720d, risk) : risk;
        r.historyScore = 0.0d;
        r.judgeScore =
                clamp01(r.evidenceScore - r.judgeRisk * 0.35d + r.shadowPromotionScore * 0.15d);

        if (freeze) {
            r.status = STATUS_FREEZE;
            r.action = ACTION_REJECT;
            r.freeze = 1;
            r.freezeKey = "REALTIME_FREEZE";
            r.freezeLevel = "HARD";
            r.freezeReason =
                    "实时Adaptive冻结：teacherRisk="
                            + teacherRisk
                            + " v20Risk="
                            + v20Risk
                            + " risk="
                            + pct(risk)
                            + " rule="
                            + safe(c.ruleRejectCode)
                            + " third="
                            + safe(c.thirdRejectCode)
                            + " cf="
                            + safe(c.counterfactualRejectCode);
        } else if (promoteReady) {
            r.status = STATUS_PROMOTE_SHADOW_CANDIDATE;
            r.action = ACTION_ALLOW_DECISION_GATE;
            r.pass = 1;
            r.promoteShadowCandidate = 1;
        } else if (ready) {
            r.status = STATUS_PASS;
            r.action = ACTION_ALLOW_DECISION_GATE;
            r.pass = 1;
        } else {
            r.status = STATUS_WATCH;
            r.action = ACTION_SHADOW_ONLY;
        }

        r.reason =
                "实时Adaptive候选审判：rank="
                        + rank
                        + " P="
                        + pct(c.finalP)
                        + " Gate="
                        + pct(c.dynamicGate)
                        + " Edge="
                        + signedPct(c.edge)
                        + " Need="
                        + pct(need)
                        + " Third="
                        + pct(third)
                        + " Risk="
                        + pct(risk)
                        + " Status="
                        + r.status;
        finalizeRejectCode(c, r);
        r.detail = r.reason + "｜reject=" + safe(r.rejectCode);
        applyResultToCandidate(candidate, r);
        return r;
    }

    private boolean containsDomainToken(String source, String token) {
        if (source == null || token == null || token.length() == 0) return false;
        String s = "|" + source.toUpperCase(java.util.Locale.ROOT).replace('_', '|').replace('-', '|') + "|";
        String t = "|" + token.toUpperCase(java.util.Locale.ROOT) + "|";
        return s.indexOf(t) >= 0;
    }

    private boolean containsPositivePromotion(String status) {
        String s = upper(status);
        return s.indexOf("PROMOTE_WATCH") >= 0
                || s.indexOf("READY") >= 0
                || s.indexOf("STRONG") >= 0;
    }
}

final class AdaptiveJudgeReport {
    public int ok;
    public String version;
    public long createdAt;
    public AdaptiveJudgeContext context;
    public ArrayList<AdaptiveJudgeResult> results;
    public int passCount;
    public int watchCount;
    public int downgradeCount;
    public int freezeCount;
    public int fakeStrongCount;
    public int promoteCount;
    public String summary;
    public String detail;

    public AdaptiveJudgeReport() {
        ok = 0;
        version = "";
        createdAt = 0L;
        results = new ArrayList<AdaptiveJudgeResult>();
        summary = "";
        detail = "";
    }
}

final class AdaptiveJudgeContext {
    public long createdAt;
    public Object moduleEffectReport;
    public Object shadowPromotionReport;
    public Object freezeReport;
    public ArrayList<AdaptiveJudgeHistoryCase> historyCases;
    public int totalBet;
    public int hitBet;
    public int missBet;
    public double winRate;
    public HashMap<String, AdaptiveJudgeDomainStats> positionMap;
    public HashMap<String, AdaptiveJudgeDomainStats> finalPMap;
    public HashMap<String, AdaptiveJudgeDomainStats> edgeMap;
    public HashMap<String, AdaptiveJudgeDomainStats> teacherMap;
    public HashMap<String, AdaptiveJudgeDomainStats> v20Map;
    public HashMap<String, AdaptiveJudgeDomainStats> windowMap;
    public HashMap<String, AdaptiveJudgeDomainStats> comboMap;
    public HashMap<String, AdaptiveJudgeDomainStats> k3Map;

    public AdaptiveJudgeContext() {
        createdAt = 0L;
        historyCases = new ArrayList<AdaptiveJudgeHistoryCase>();
        positionMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        finalPMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        edgeMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        teacherMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        v20Map = new HashMap<String, AdaptiveJudgeDomainStats>();
        windowMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        comboMap = new HashMap<String, AdaptiveJudgeDomainStats>();
        k3Map = new HashMap<String, AdaptiveJudgeDomainStats>();
    }
}

final class AdaptiveJudgeResult {
    public String version;
    public long createdAt;
    public AdaptiveJudgeCandidateSnapshot candidate;
    public String status;
    public String action;
    public int pass;
    public int downgradeToShadow;
    public int freeze;
    public int promoteShadowCandidate;
    public int position;
    public double finalP;
    public double dynamicGate;
    public double edge;
    public String teacherStatus;
    public String v20Status;
    public String windowStateKey;
    public String rejectCode;
    public double judgeScore;
    public double judgeRisk;
    public double evidenceScore;
    public double moduleScore;
    public double shadowPromotionScore;
    public double freezeRisk;
    public double historyScore;
    public String freezeKey;
    public String freezeLevel;
    public String freezeReason;
    public String reason;
    public String moduleAdvice;
    public String detail;
    public ArrayList<AdaptiveJudgeSignal> signals;

    public AdaptiveJudgeResult() {
        version = "";
        status = AdaptiveLearningJudgeEngine.STATUS_WATCH;
        action = AdaptiveLearningJudgeEngine.ACTION_SHADOW_ONLY;
        teacherStatus = "";
        v20Status = "";
        windowStateKey = "";
        rejectCode = AdaptiveLearningJudgeEngine.REJECT_NONE;
        freezeKey = "";
        freezeLevel = "";
        freezeReason = "";
        reason = "";
        moduleAdvice = "";
        detail = "";
        signals = new ArrayList<AdaptiveJudgeSignal>();
    }

    public boolean allowEnterDecisionGate() {
        return pass == 1
                && (AdaptiveLearningJudgeEngine.STATUS_PASS.equals(status)
                        || AdaptiveLearningJudgeEngine.STATUS_PROMOTE_SHADOW_CANDIDATE.equals(
                                status));
    }

    public boolean shouldShadowOnly() {
        return downgradeToShadow == 1
                || AdaptiveLearningJudgeEngine.ACTION_SHADOW_ONLY.equals(action)
                || AdaptiveLearningJudgeEngine.ACTION_HOLD_OR_SHADOW.equals(action);
    }
}

final class AdaptiveJudgeSignal {
    public String type;
    public String text;
    public double support;
    public double risk;
    public double weight;

    public AdaptiveJudgeSignal() {
        type = "";
        text = "";
    }

    public String text() {
        return type + "：support=" + support + "，risk=" + risk + "，weight=" + weight + "，" + text;
    }
}

final class AdaptiveJudgeDomainStats {
    public String key;
    public int total;
    public int hit;
    public int miss;
    public double sumFinalP;
    public double sumEdge;

    public AdaptiveJudgeDomainStats() {
        key = "";
    }

    public double rate() {
        if (total <= 0) return 0.0d;
        return ((double) hit) / ((double) total);
    }

    public String shortText() {
        return key + " " + total + "单/中" + hit + "/胜率" + rate();
    }
}

final class AdaptiveJudgeCandidateSnapshot {
    public int position;
    public int n1;
    public int n2;
    public int n3;
    public double finalP;
    public double rawP;
    public double calibratedP;
    public double dynamicGate;
    public double edge;

    public int effectiveSample;
    public int comboSample;
    public int similarWindowSample;
    public double sampleReliability;
    public double recentValidity;
    public double featureDistance;
    public double similarWindowHitRate;
    public double comboHitRate;

    public String finalPBucket;
    public String edgeBucket;
    public String teacherBucket;
    public String v20Bucket;
    public String windowBucket;
    public String comboDomain;
    public String dynamicClusterId;
    public String dynamicClusterStatus;
    public double dynamicClusterHitRate;
    public int dynamicClusterSample;
    public int dynamicClusterMaxLose;
    public String dynamicClusterBucket;
    public double needSupplyMatchScore;
    public double needSupplyThirdQuality;
    public double needSupplyRisk;
    public String needSupplyBucket;
    public double counterfactualWhyNotRisk;
    public double causeAblationScore;
    public int causeAblationFalseCauseCount;
    public String counterfactualBucket;
    public String k3RankPattern;

    public String teacherMatrixStatus;
    public String teacherNumberStatus;
    public String teacherStatus;
    public int teacherMatrixSample;
    public double teacherMatrixHitRate;
    public double teacherMatrixConfidence;
    public double teacherMatrixRisk;
    public double teacherEvidenceContribution;
    public double teacherCounterContribution;

    public String v20Status;
    public String dynamicStatus;
    public double v20SupportScore;
    public double v20RiskScore;
    public double evidenceContribution;
    public double counterContribution;
    public double falseStrongRisk;

    public String windowStateKey;
    public String dynamicWindowStateKey;
    public String probabilityWindowStateKey;
    public int decisionHardRisk;
    public int decisionCounterRiskHigh;
    public int decisionEvidenceStrong;
    public int decisionEvidenceElite;
    public String shadowPromotionStatus;
    public double shadowPromoteScore;
    public String shadowRejectCode;
    public int promotionSampleCount;
    public double shadowPromotionHitRate;
    public double shadowPromotionRecentHitRate;
    public double shadowPromotionReliability;
    public double ruleHealthScore;
    public double ruleHealthRisk;
    public double ruleDecayRisk;
    public String ruleRejectCode;
    public String ruleActionAdvice;
    public double thirdQuality;
    public double thirdDragRisk;
    public double thirdWeakness;
    public String thirdRejectCode;
    public double counterfactualRisk;
    public double rankStability;
    public double replaceGain;
    public String counterfactualRejectCode;
    public double positionNeedScore;
    public double numberSupplyScore;
    public double coverageQuality;
    public int hardBlockFlag;
    public String riskSeverity;
    public String hardBlockSource;

    public AdaptiveJudgeCandidateSnapshot() {
        finalPBucket = "";
        edgeBucket = "";
        teacherBucket = "";
        v20Bucket = "";
        windowBucket = "";
        comboDomain = "";
        dynamicClusterId = "";
        dynamicClusterStatus = "";
        dynamicClusterBucket = "";
        needSupplyBucket = "";
        counterfactualBucket = "";
        k3RankPattern = "";
        teacherMatrixStatus = "";
        teacherNumberStatus = "";
        teacherStatus = "";
        v20Status = "";
        dynamicStatus = "";
        windowStateKey = "";
        dynamicWindowStateKey = "";
        probabilityWindowStateKey = "";
        shadowPromotionStatus = "";
        shadowRejectCode = "";
        ruleRejectCode = "";
        ruleActionAdvice = "";
        thirdRejectCode = "";
        counterfactualRejectCode = "";
    }
}

final class AdaptiveJudgeHistoryCase {
    public String kind;
    public String targetIssue;
    public String baseIssue;
    public long createdAt;
    public int position;
    public int n1;
    public int n2;
    public int n3;
    public int actualNumber;
    public int settled;
    public int hit;
    public String result;
    public double finalP;
    public double dynamicGate;
    public double edge;
    public String finalPBucket;
    public String edgeBucket;
    public String teacherBucket;
    public String v20Bucket;
    public String windowBucket;

    public AdaptiveJudgeHistoryCase() {
        kind = "";
        targetIssue = "";
        baseIssue = "";
        result = "";
        finalPBucket = "";
        edgeBucket = "";
        teacherBucket = "";
        v20Bucket = "";
        windowBucket = "";
    }
}
