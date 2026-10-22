package com.ethk3.novaonepro;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * DynamicCauseChainEngine.java
 *
 * <p>第10/14 现有核心引擎升级文件。
 *
 * <p>V20动态因链总成 + WindowState + 相似窗口 + Candidate-Decision-Trade-Diagnosis 字段闭环修复版（第1/6）： 1. 不再依赖
 * Models.DynamicCauseChain，避免 Models.java 缺内部类导致编译失败。 2. 使用本文件内部 public static final class
 * DynamicCauseRecord 保存动态因链结果。 3. buildDynamicChainsFull 是唯一满参数主入口，避免 AIDE 对 null 参数重载解析不明确。 4.
 * buildDynamicChains 保留兼容入口，但只转调 buildDynamicChainsFull。 5. 回写 CandidateK3 与 CauseChain，但不创建
 * BET、不结算、不写真实账本、不写真实校准。 6. 第10/14升级：强制写入 v20Status / v20SupportScore / v20RiskScore /
 * evidenceContribution / counterContribution 等诊断字段。 7. 第10/14升级：提供 applyDynamicRecordToObject，供
 * DecisionGate/Probability/Trade 后续文件安全复制 V20 字段。 8. 第1/6彻底修复：即使特征/因链/教师输入为空，也必须写入 V20_UNKNOWN /
 * WindowState fallback，禁止诊断再显示 V20=空 / Window=空。 9. 第1/6彻底修复：新增 Candidate→Decision→Trade→Diagnosis
 * 字段复制桥，统一写 v20EvidenceContribution / v20CounterContribution / v20SampleReliability /
 * v20RecentValidity。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不把SHADOW/HOLD当真实BET。 - 不绕过 DecisionGateEngine。 -
 * 最终是否BET仍由 DecisionGateEngine 裁决。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class DynamicCauseChainEngine {
    public static final String MODULE_BUILD_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double GRADE_S_LINE = 0.72d;
    private static final double GRADE_A_LINE = 0.62d;
    private static final double GRADE_B_LINE = 0.50d;
    private static final double PASS_LINE = 0.54d;
    private static final double RISK_LINE = 0.64d;
    private static final double HARD_RISK_LINE = 0.80d;
    private static final double SIMILAR_WINDOW_MAX_DISTANCE = 0.32d;
    private static final String V20_DYNAMIC_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final String V20_DYNAMIC_UPSTREAM_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String V20_DYNAMIC_CAUSAL_WINDOW_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String V20_DYNAMIC_CAUSE_CHAIN_MAIN_CHAIN_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String V20_DYNAMIC_CAUSE_CHAIN_FIELD_CHAIN_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double NEED_SUPPLY_STRONG_LINE = 0.620d;
    private static final double NEED_SUPPLY_READY_LINE = 0.540d;
    private static final double DYNAMIC_CLUSTER_PROMOTE_LINE = 0.380d;
    private static final double DYNAMIC_CLUSTER_FREEZE_LINE = 0.260d;
    private static final int REALTIME_WINDOW_12 = 12;
    private static final int REALTIME_WINDOW_24 = 24;
    private static final int REALTIME_WINDOW_48 = 48;
    private static final int REALTIME_WINDOW_96 = 96;
    private static final double REALTIME_K3_BASELINE = AppConfig.RANDOM_K3_BASELINE;
    private static final double REALTIME_NUMBER_BASELINE = 0.100d;
    private static final String V20_STATUS_STRONG = "V20_STRONG";
    private static final String V20_STATUS_READY = "V20_READY";
    private static final String V20_STATUS_WATCH = "V20_WATCH";
    private static final String V20_STATUS_UNKNOWN = "V20_UNKNOWN";
    private static final String V20_STATUS_WEAK = "V20_WEAK";
    private static final String V20_STATUS_RISK = "V20_RISK";
    private static final String V20_STATUS_FREEZE = "V20_FREEZE";
    private static final String V20_ACTION_PASS = "PASS";
    private static final String V20_ACTION_WATCH = "WATCH";
    private static final String V20_ACTION_RISK = "RISK";
    private static final String V20_ACTION_FREEZE = "FREEZE";

    public DynamicCauseChainEngine() {}

    /** AutoFetchService 第3/6实时入口：支持直接接收 ComboCandidate。 只写 V20/Window/证据字段，不创建BET、不结算、不保存真实账本。 */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return enrichRealtimeComboCandidates(
                store, candidates, clean, baseIssue, targetIssue, "applyToAll");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return enrichRealtimeComboCandidates(
                store, candidates, clean, baseIssue, targetIssue, "enrichCandidates");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return enrichRealtimeComboCandidates(
                store, candidates, clean, baseIssue, targetIssue, "analyzeCandidates");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return enrichRealtimeComboCandidates(
                null, candidates, clean, baseIssue, targetIssue, "applyRealtime");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        return enrichRealtimeComboCandidates(
                null, candidates, clean, baseIssue, targetIssue, "runRealtime");
    }

    public ArrayList<Models.ComboCandidate> enrichRealtimeComboCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String entryName) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        int total = 0;
        int strong = 0;
        int ready = 0;
        int watch = 0;
        int unknown = 0;
        int riskCount = 0;
        String firstSummary = "";
        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);
            if (c == null) {
                continue;
            }
            total++;
            applyRealtimeDynamicEvidenceToCandidate(
                    c, clean, baseIssue, targetIssue, i + 1, entryName);
            saveRealtimeDynamicCausalSamples(store, c, clean, baseIssue, targetIssue, entryName);
            String st = getString(c, "v20Status", "");
            if (V20_STATUS_STRONG.equals(st)) strong++;
            else if (V20_STATUS_READY.equals(st)) ready++;
            else if (V20_STATUS_WATCH.equals(st)) watch++;
            else if (V20_STATUS_RISK.equals(st) || V20_STATUS_FREEZE.equals(st)) riskCount++;
            else unknown++;
            if (firstSummary.length() == 0) {
                firstSummary = buildRealtimeCandidateSummary(c);
            }
        }
        if (store != null) {
            try {
                store.setState("dynamic_cause_realtime_version", V20_DYNAMIC_UPSTREAM_VERSION);
                store.setState("dynamic_cause_realtime_entry", safe(entryName));
                store.setState("dynamic_cause_realtime_total", String.valueOf(total));
                store.setState("dynamic_cause_realtime_strong", String.valueOf(strong));
                store.setState("dynamic_cause_realtime_ready", String.valueOf(ready));
                store.setState("dynamic_cause_realtime_watch", String.valueOf(watch));
                store.setState("dynamic_cause_realtime_unknown", String.valueOf(unknown));
                store.setState("dynamic_cause_realtime_risk", String.valueOf(riskCount));
                store.setState("dynamic_cause_realtime_best", firstSummary);
                store.updateDynamicCausalState(
                        AppConfig.STATE_DYNAMIC_CLUSTER_SNAPSHOT_VERSION,
                        V20_DYNAMIC_CAUSAL_WINDOW_VERSION);
                store.updateDynamicCausalState(
                        AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS,
                        "DynamicCauseChain实时候选="
                                + total
                                + "｜STRONG="
                                + strong
                                + "｜READY="
                                + ready
                                + "｜WATCH="
                                + watch
                                + "｜RISK="
                                + riskCount);
            } catch (Throwable e) {
            }
        }
        return out;
    }

    private void applyRealtimeDynamicEvidenceToCandidate(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            int rankNo,
            String entryName) {
        if (c == null) {
            return;
        }
        if (baseIssue != null && baseIssue.length() > 0) {
            putString(c, "baseIssue", baseIssue);
        }
        if (targetIssue != null && targetIssue.length() > 0) {
            putString(c, "targetIssue", targetIssue);
        }
        int p = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        if (mask <= 0) {
            mask = buildK3Mask(n1, n2, n3);
            putInt(c, "k3Mask", mask);
            putInt(c, "k3_mask", mask);
        }
        if (!isValidPosition(p) || !isValidNumber(n1) || !isValidNumber(n2) || !isValidNumber(n3)) {
            writeUnknownRealtimeV20(c, p, n1, n2, n3, "INVALID_CANDIDATE", entryName);
            return;
        }
        RealtimeWindowBundle wb = buildRealtimeWindowBundle(c, clean);
        String window = buildRealtimeWindowStateKey(c, wb);
        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - gate);
        double probScore = calcProbScore(finalP, gate, edge);
        double teacherScore =
                getDouble(
                        c,
                        "teacherNumberScore",
                        getDouble(
                                c, "teacherComboScore", getDouble(c, "teacherMatrixScore", 0.0d)));
        double teacherRisk =
                getDouble(c, "teacherNumberRisk", getDouble(c, "teacherMatrixRisk", 0.0d));
        double coverage =
                getDouble(c, "coverageQualityScore", Math.max(wb.k3Rate48, REALTIME_K3_BASELINE));
        double third =
                getDouble(
                        c,
                        "thirdNumberQualityScore",
                        Math.max(wb.n3Rate96, REALTIME_NUMBER_BASELINE));
        double coverageRisk =
                getDouble(c, "k3CoverageRisk", buildRealtimeCoverageRisk(wb, coverage, third));
        double support =
                clamp01(
                        probScore * 0.22d
                                + wb.k3Score * 0.24d
                                + wb.numberScore * 0.13d
                                + wb.trendScore * 0.12d
                                + wb.timingScore * 0.08d
                                + teacherScore * 0.10d
                                + coverage * 0.11d);
        double risk =
                clamp01(
                        coverageRisk * 0.22d
                                + wb.volatilityRisk * 0.18d
                                + wb.sampleRisk * 0.16d
                                + teacherRisk * 0.16d
                                + Math.max(0.0d, 0.36d - finalP) * 0.80d
                                + (edge > 0.020d && teacherScore <= 0.0d ? 0.10d : 0.0d)
                                + (coverage <= 0.250d ? 0.12d : 0.0d));
        double sampleReliability = sampleTrust(wb.sample96 + wb.sample48 + wb.sample24);
        double recentValidity =
                clamp01(
                        wb.k3Rate24 * 0.42d
                                + wb.k3Rate48 * 0.28d
                                + wb.n3Rate96 * 0.12d
                                + sampleReliability * 0.18d);
        double evidence =
                clamp01(support * 0.70d + recentValidity * 0.18d + posPart(edge * 4.0d) * 0.12d);
        double counter =
                clamp01(
                        risk * 0.76d
                                + (1.0d - sampleReliability) * 0.14d
                                + Math.max(0.0d, coverageRisk - 0.50d) * 0.10d);
        double confidence =
                clamp01(
                        sampleReliability * 0.45d
                                + recentValidity * 0.25d
                                + (1.0d - risk) * 0.20d
                                + Math.max(0.0d, support - 0.50d) * 0.10d);
        String status =
                realtimeV20StatusOf(
                        support,
                        risk,
                        confidence,
                        sampleReliability,
                        finalP,
                        edge,
                        coverage,
                        third);
        String action =
                realtimeActionAdvice(
                        status,
                        support,
                        risk,
                        confidence,
                        sampleReliability,
                        coverage,
                        third,
                        edge);
        String evidenceText =
                "V20实时因链：win12/24/48/96="
                        + percent(wb.k3Rate12)
                        + "/"
                        + percent(wb.k3Rate24)
                        + "/"
                        + percent(wb.k3Rate48)
                        + "/"
                        + percent(wb.k3Rate96)
                        + "，cover="
                        + percent(coverage)
                        + "，third="
                        + percent(third)
                        + "，trend="
                        + percent(wb.trendScore)
                        + "，sample="
                        + wb.sample96;
        String counterText =
                "V20反证：coverageRisk="
                        + percent(coverageRisk)
                        + "，volatility="
                        + percent(wb.volatilityRisk)
                        + "，sampleRisk="
                        + percent(wb.sampleRisk)
                        + "，teacherRisk="
                        + percent(teacherRisk)
                        + "，edge="
                        + percent(edge);
        putRealtimeV20Fields(
                c,
                status,
                window,
                support,
                risk,
                confidence,
                evidence,
                counter,
                sampleReliability,
                recentValidity,
                action,
                evidenceText,
                counterText,
                wb,
                entryName);
        applyDynamicCausalWindowLayerToCandidate(
                c,
                clean,
                baseIssue,
                targetIssue,
                wb,
                window,
                support,
                risk,
                confidence,
                evidence,
                counter,
                sampleReliability,
                recentValidity,
                entryName);
    }

    private void applyDynamicCausalWindowLayerToCandidate(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            RealtimeWindowBundle wb,
            String window,
            double support,
            double risk,
            double confidence,
            double evidence,
            double counter,
            double sampleReliability,
            double recentValidity,
            String entryName) {
        if (c == null) {
            return;
        }
        int p = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        if (mask <= 0) {
            mask = buildK3Mask(n1, n2, n3);
            putInt(c, "k3Mask", mask);
            putInt(c, "k3_mask", mask);
        }
        Models.WindowFeatureVector vector =
                buildDynamicWindowFeatureVector(c, clean, targetIssue, wb, window);
        Models.DynamicWindowCluster cluster =
                buildRealtimeDynamicCluster(
                        c,
                        vector,
                        wb,
                        window,
                        support,
                        risk,
                        confidence,
                        evidence,
                        counter,
                        sampleReliability,
                        recentValidity);
        Models.PositionNeedState need = buildPositionNeedState(c, clean, targetIssue, wb, window);
        Models.NumberSupplyState s1 =
                buildNumberSupplyState(c, clean, targetIssue, p, n1, "MAIN", need, wb);
        Models.NumberSupplyState s2 =
                buildNumberSupplyState(c, clean, targetIssue, p, n2, "ASSIST", need, wb);
        Models.NumberSupplyState s3 =
                buildNumberSupplyState(c, clean, targetIssue, p, n3, "DEFENSE", need, wb);
        Models.NeedSupplyMatchResult match = buildNeedSupplyMatch(c, need, s1, s2, s3, wb, cluster);
        Models.CounterfactualReasonResult cf =
                buildCounterfactualReason(c, clean, targetIssue, cluster, match, wb);
        Models.CauseAblationResult ab = buildCauseAblation(cluster, match, cf, wb);
        Models.AdaptiveJudgeResult judge =
                buildAdaptiveJudge(
                        c,
                        cluster,
                        match,
                        cf,
                        ab,
                        support,
                        risk,
                        confidence,
                        sampleReliability,
                        recentValidity);
        Models.PromotionDomainState promo = buildPromotionDomainState(cluster, judge);
        try {
            Models.ClusterMatchResult cmr = new Models.ClusterMatchResult();
            if (cluster != null) {
                cmr.clusterId = cluster.clusterId;
                cmr.clusterStatus = cluster.status;
                cmr.prototypeKey = cluster.prototypeKey;
                cmr.distance = cluster.prototypeDistance;
                cmr.sampleCount = cluster.sampleCount;
                cmr.recentSample = cluster.recentSample;
                cmr.hitRate = cluster.hitRate;
                cmr.recentHitRate = cluster.recentHitRate;
                cmr.confidence = cluster.confidence;
                cmr.counterScore = cluster.counterScore;
                cmr.maxLoseStreak = cluster.maxLoseStreak;
                cmr.reason = cluster.reason;
            } else {
                cmr.distance = 1.0d;
                cmr.reason = "";
            }
            c.applyClusterMatch(cmr);
        } catch (Throwable e) {
        }
        try {
            c.applyNeedSupply(match);
        } catch (Throwable e) {
        }
        try {
            c.applyCounterfactualReason(cf);
        } catch (Throwable e) {
        }
        try {
            c.applyAdaptiveJudge(judge);
        } catch (Throwable e) {
        }
        applyDynamicCausalBridgeFieldsToObject(
                c, cluster, need, s1, s2, s3, match, cf, ab, judge, promo, entryName);
    }

    private Models.WindowFeatureVector buildDynamicWindowFeatureVector(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String targetIssue,
            RealtimeWindowBundle wb,
            String window) {
        Models.WindowFeatureVector v = new Models.WindowFeatureVector();
        v.targetIssue = safe(targetIssue);
        v.position = getInt(c, "position", 0);
        v.n1 = getInt(c, "n1", 0);
        v.n2 = getInt(c, "n2", 0);
        v.n3 = getInt(c, "n3", 0);
        v.k3Mask = getInt(c, "k3Mask", buildK3Mask(v.n1, v.n2, v.n3));
        v.rawKey = safe(window);
        v.vectorKey = safe(window);
        if (wb == null) wb = new RealtimeWindowBundle();
        v.trendSlope12 = clampSigned((wb.k3Rate12 - REALTIME_K3_BASELINE) * 2.0d);
        v.trendSlope24 = clampSigned((wb.k3Rate24 - REALTIME_K3_BASELINE) * 2.0d);
        v.trendSlope48 = clampSigned((wb.k3Rate48 - REALTIME_K3_BASELINE) * 2.0d);
        v.trendSlope96 = clampSigned((wb.k3Rate96 - REALTIME_K3_BASELINE) * 2.0d);
        v.trendSlope360 = clampSigned((wb.k3Rate96 - REALTIME_K3_BASELINE) * 1.20d);
        v.trendLife = clamp01((wb.k3Rate12 + wb.k3Rate24 + wb.k3Rate48) / 1.10d);
        v.trendFatigue = clamp01(Math.max(0.0d, wb.k3Rate96 - wb.k3Rate12) * 2.0d);
        v.omissionPressure = calcK3OmissionPressure(c, clean);
        v.hotColdTransition = clampSigned((wb.k3Rate24 - wb.k3Rate96) * 2.0d);
        v.densityScore = clamp01((wb.k3Rate24 + wb.k3Rate48 + wb.numberScore) / 1.05d);
        v.repeatRhythm = calcRepeatRhythm(c, clean);
        v.oddEvenBias = calcOddEvenBiasScore(v.n1, v.n2, v.n3);
        v.highLowBias = calcHighLowBiasScore(v.n1, v.n2, v.n3);
        v.tailPressure = calcTailPressure(v.n1, v.n2, v.n3);
        v.neighborPressure = calcNeighborPressure(v.n1, v.n2, v.n3);
        v.crossPositionFlow = calcCrossPositionFlow(clean, v.position, v.n1, v.n2, v.n3);
        v.crossPositionCrowd = calcCrossPositionCrowd(clean, v.n1, v.n2, v.n3);
        v.sameNumberCrowd = v.crossPositionCrowd;
        v.timingOffset = clampSigned((wb.k3Rate12 - wb.k3Rate48) * 2.0d);
        v.counterEvidence =
                clamp01(
                        wb.volatilityRisk * 0.42d
                                + wb.sampleRisk * 0.32d
                                + Math.max(0.0d, REALTIME_K3_BASELINE - wb.k3Rate24) * 1.1d);
        v.thirdQuality =
                Math.max(
                        getDouble(c, "thirdNumberQualityScore", 0.0d),
                        Math.max(wb.n3Rate96, REALTIME_NUMBER_BASELINE));
        v.summary =
                "动态窗口向量｜"
                        + safe(window)
                        + "｜cover="
                        + percent(Math.max(wb.k3Rate48, REALTIME_K3_BASELINE))
                        + "｜third="
                        + percent(v.thirdQuality)
                        + "｜counter="
                        + percent(v.counterEvidence);
        try {
            v.rebuildKey();
        } catch (Throwable e) {
        }
        return v;
    }

    private Models.DynamicWindowCluster buildRealtimeDynamicCluster(
            Models.ComboCandidate c,
            Models.WindowFeatureVector v,
            RealtimeWindowBundle wb,
            String window,
            double support,
            double risk,
            double confidence,
            double evidence,
            double counter,
            double sampleReliability,
            double recentValidity) {
        Models.DynamicWindowCluster dc = new Models.DynamicWindowCluster();
        if (wb == null) wb = new RealtimeWindowBundle();
        String auditKey = getString(c, "auditClusterKey", safe(window));
        String learningKey = getString(c, "learningClusterKey", "");
        String proto =
                learningKey.length() > 0
                        ? learningKey
                        : buildDynamicPrototypeKey(v, wb, support, risk);
        dc.clusterId = AppConfig.buildDynamicClusterId(proto);
        dc.prototypeKey = proto;
        dc.prototypeVector = auditKey;
        int similarSample = getInt(c, "similarWindowSample", 0);
        double similarRate = getDouble(c, "similarWindowHitRate", 0.0d);
        double similarRecent = getDouble(c, "similarWindowRecentRate", similarRate);
        dc.sampleCount = Math.max(Math.max(0, wb.sample96), similarSample);
        double effectiveRate =
                wb.sample96 >= 8 ? wb.k3Rate96 : (similarSample > 0 ? similarRate : wb.k3Rate96);
        dc.hitCount = (int) Math.round(effectiveRate * Math.max(1, dc.sampleCount));
        dc.missCount = Math.max(0, dc.sampleCount - dc.hitCount);
        dc.recentSample = Math.max(Math.max(0, wb.sample24), Math.min(similarSample, 60));
        double effectiveRecent =
                wb.sample24 >= 6 ? wb.k3Rate24 : (similarSample > 0 ? similarRecent : wb.k3Rate24);
        dc.recentHitCount = (int) Math.round(effectiveRecent * Math.max(1, dc.recentSample));
        dc.hitRate = effectiveRate;
        dc.recentHitRate = effectiveRecent;
        dc.maxLoseStreak = estimateRecentLoseStreak(c, null);
        dc.confidence =
                clamp01(confidence * 0.46d + sampleReliability * 0.32d + recentValidity * 0.22d);
        dc.volatility = wb.volatilityRisk;
        dc.counterScore = clamp01(counter);
        dc.prototypeDistance = calcPrototypeDistance(v, wb, support, risk);
        dc.status = clusterStatusOf(dc, support, risk, evidence, counter);
        dc.reason =
                "动态簇="
                        + dc.status
                        + "｜hit96="
                        + percent(dc.hitRate)
                        + "｜recent="
                        + percent(dc.recentHitRate)
                        + "｜support="
                        + percent(support)
                        + "｜risk="
                        + percent(risk)
                        + "｜distance="
                        + percent(dc.prototypeDistance);
        dc.createdAt = System.currentTimeMillis();
        dc.updatedAt = dc.createdAt;
        try {
            dc.recalc();
        } catch (Throwable e) {
        }
        return dc;
    }

    private Models.PositionNeedState buildPositionNeedState(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String targetIssue,
            RealtimeWindowBundle wb,
            String window) {
        Models.PositionNeedState p = new Models.PositionNeedState();
        p.targetIssue = safe(targetIssue);
        p.position = getInt(c, "position", 0);
        if (wb == null) wb = new RealtimeWindowBundle();
        p.needHotContinue = clamp01(Math.max(0.0d, wb.k3Rate24 - REALTIME_K3_BASELINE) * 2.0d);
        p.needColdRepair = clamp01(Math.max(0.0d, REALTIME_K3_BASELINE - wb.k3Rate48) * 2.2d);
        p.needOmissionReturn = calcK3OmissionPressure(c, clean);
        p.needHighLowBalance =
                1.0d
                        - calcHighLowBiasScore(
                                getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
        p.needOddEvenBalance =
                1.0d
                        - calcOddEvenBiasScore(
                                getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
        p.needTailRepair =
                calcTailPressure(getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
        p.needNeighborRelay =
                calcNeighborPressure(getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
        p.needCrossPositionFlow =
                calcCrossPositionFlow(
                        clean,
                        p.position,
                        getInt(c, "n1", 0),
                        getInt(c, "n2", 0),
                        getInt(c, "n3", 0));
        p.needCrowdRelease =
                calcCrossPositionCrowd(
                        clean, getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
        p.needReverseBreak = clamp01(Math.max(0.0d, wb.k3Rate12 - wb.k3Rate48) * 2.4d);
        p.needStableRepeat = calcRepeatRhythm(c, clean);
        p.needChaosAvoid = clamp01(1.0d - wb.volatilityRisk);
        p.mainNeedType = mainNeedTypeOf(p);
        p.summary =
                "位置需求="
                        + p.mainNeedType
                        + "｜热延续="
                        + percent(p.needHotContinue)
                        + "｜冷修复="
                        + percent(p.needColdRepair)
                        + "｜遗漏回补="
                        + percent(p.needOmissionReturn)
                        + "｜避乱="
                        + percent(p.needChaosAvoid);
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        return p;
    }

    private Models.NumberSupplyState buildNumberSupplyState(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String targetIssue,
            int position,
            int number,
            String role,
            Models.PositionNeedState need,
            RealtimeWindowBundle wb) {
        Models.NumberSupplyState n = new Models.NumberSupplyState();
        n.targetIssue = safe(targetIssue);
        n.position = position;
        n.number = number;
        double nr = realtimeNumberRate(position, number, clean, REALTIME_WINDOW_96);
        if (wb == null) wb = new RealtimeWindowBundle();
        n.supplyHotContinue = clamp01(Math.max(0.0d, nr - REALTIME_NUMBER_BASELINE) * 3.0d);
        n.supplyColdRepair = clamp01(Math.max(0.0d, REALTIME_NUMBER_BASELINE - nr) * 3.0d);
        n.supplyOmissionReturn = calcNumberOmissionPressure(position, number, clean);
        n.supplyHighLowBalance = number >= 6 ? 0.54d : 0.46d;
        n.supplyOddEvenBalance = number % 2 == 0 ? 0.53d : 0.47d;
        n.supplyTailRepair = calcSingleTailPressure(number);
        n.supplyNeighborRelay = calcSingleNeighborRelay(position, number, clean);
        n.supplyCrossPositionFlow = calcNumberCrossFlow(clean, number);
        n.supplyCrowdRelease = calcNumberCrowdRelease(clean, number);
        n.supplyReverseBreak = clamp01(Math.max(0.0d, nr - wb.k3Rate96 / 3.0d) * 2.0d);
        n.supplyStableRepeat = calcSingleRepeat(position, number, clean);
        n.supplyRisk =
                clamp01(
                        Math.max(0.0d, REALTIME_NUMBER_BASELINE * 0.55d - nr) * 3.0d
                                + wb.volatilityRisk * 0.20d);
        n.mainSupplyType = role + "_" + mainSupplyTypeOf(n);
        n.summary =
                role
                        + "供给="
                        + n.mainSupplyType
                        + "｜rate="
                        + percent(nr)
                        + "｜omission="
                        + percent(n.supplyOmissionReturn)
                        + "｜risk="
                        + percent(n.supplyRisk);
        try {
            n.recalc();
        } catch (Throwable e) {
        }
        return n;
    }

    private Models.NeedSupplyMatchResult buildNeedSupplyMatch(
            Models.ComboCandidate c,
            Models.PositionNeedState need,
            Models.NumberSupplyState s1,
            Models.NumberSupplyState s2,
            Models.NumberSupplyState s3,
            RealtimeWindowBundle wb,
            Models.DynamicWindowCluster cluster) {
        Models.NeedSupplyMatchResult r = new Models.NeedSupplyMatchResult();
        r.targetIssue = getString(c, "targetIssue", "");
        r.position = getInt(c, "position", 0);
        r.n1 = getInt(c, "n1", 0);
        r.n2 = getInt(c, "n2", 0);
        r.n3 = getInt(c, "n3", 0);
        r.k3Mask = getInt(c, "k3Mask", buildK3Mask(r.n1, r.n2, r.n3));
        r.mainRoleNumber = r.n1;
        r.assistRoleNumber = r.n2;
        r.defenseRoleNumber = r.n3;
        r.mainRole = s1 == null ? "MAIN" : s1.mainSupplyType;
        r.assistRole = s2 == null ? "ASSIST" : s2.mainSupplyType;
        r.defenseRole = s3 == null ? "DEFENSE" : s3.mainSupplyType;
        r.mainRoleScore = supplyNeedFit(need, s1);
        r.assistRoleScore = supplyNeedFit(need, s2) * 0.94d;
        r.defenseRoleScore = supplyNeedFit(need, s3) * 0.88d;
        r.thirdQuality = wb == null ? 0.0d : Math.max(wb.n3Rate96, REALTIME_NUMBER_BASELINE);
        r.roleDiversity = calcRoleDiversity(s1, s2, s3);
        r.supplyRisk = clamp01(avgSupplyRisk(s1, s2, s3));
        r.matchScore =
                clamp01(
                        r.mainRoleScore * 0.44d
                                + r.assistRoleScore * 0.29d
                                + r.defenseRoleScore * 0.17d
                                + r.roleDiversity * 0.10d
                                - r.supplyRisk * 0.18d);
        r.matchKey = AppConfig.buildNeedSupplyMatchKey(r.targetIssue, r.position, r.n1, r.n2, r.n3);
        r.whyMatch =
                "需求供给匹配="
                        + percent(r.matchScore)
                        + "｜主="
                        + percent(r.mainRoleScore)
                        + "｜辅="
                        + percent(r.assistRoleScore)
                        + "｜防="
                        + percent(r.defenseRoleScore);
        r.whyNotMatch =
                r.matchScore < NEED_SUPPLY_READY_LINE
                        ? "反证：需求供给低于READY，或第三号拖拽/供给风险偏高"
                        : "反证较低：供给结构可观察";
        r.summary =
                r.whyMatch
                        + "｜diversity="
                        + percent(r.roleDiversity)
                        + "｜risk="
                        + percent(r.supplyRisk);
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    private Models.CounterfactualReasonResult buildCounterfactualReason(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String targetIssue,
            Models.DynamicWindowCluster cluster,
            Models.NeedSupplyMatchResult match,
            RealtimeWindowBundle wb) {
        Models.CounterfactualReasonResult r = new Models.CounterfactualReasonResult();
        r.targetIssue = safe(targetIssue);
        r.position = getInt(c, "position", 0);
        r.n1 = getInt(c, "n1", 0);
        r.n2 = getInt(c, "n2", 0);
        r.n3 = getInt(c, "n3", 0);
        r.k3Mask = getInt(c, "k3Mask", buildK3Mask(r.n1, r.n2, r.n3));
        r.positionRank = rankPositionByNeed(clean, r.position);
        r.k3Rank = rankK3BySupply(match);
        r.rankScore =
                clamp01(
                        (11 - Math.min(10, r.positionRank)) / 10.0d * 0.46d
                                + (11 - Math.min(10, r.k3Rank)) / 10.0d * 0.36d
                                + (match == null ? 0.0d : match.matchScore) * 0.18d);
        r.whyNotRisk =
                clamp01(
                        1.0d
                                - r.rankScore
                                + (cluster == null ? 0.0d : cluster.counterScore) * 0.25d);
        r.mirrorMissSimilarity =
                clamp01(calcMirrorMissSimilarity(clean, r.position, r.n1, r.n2, r.n3));
        r.causeScore = match == null ? 0.0d : match.matchScore;
        r.counterScore = r.whyNotRisk;
        r.whyThisPosition =
                "位置反事实：P"
                        + r.position
                        + " 当前需求排名="
                        + r.positionRank
                        + "，窗口簇="
                        + (cluster == null ? "" : cluster.status);
        r.whyTheseNumbers =
                "号码反事实：K3 rank="
                        + r.k3Rank
                        + "，需求供给="
                        + percent(match == null ? 0.0d : match.matchScore);
        r.whyNotOtherPosition = "其他位置未选：需求排名或窗口簇稳定性不足，实时只允许单位置";
        r.whyNotOtherNumbers = "其他号码未选：供给角色不齐或第三号质量/镜像未中相似度不佳";
        r.summary =
                r.whyThisPosition
                        + "｜"
                        + r.whyTheseNumbers
                        + "｜whyNotRisk="
                        + percent(r.whyNotRisk);
        r.reasonKey =
                AppConfig.buildCounterfactualReasonKey(
                        r.targetIssue,
                        r.position,
                        r.n1,
                        r.n2,
                        r.n3,
                        cluster == null ? "" : cluster.clusterId);
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    private Models.CauseAblationResult buildCauseAblation(
            Models.DynamicWindowCluster cluster,
            Models.NeedSupplyMatchResult match,
            Models.CounterfactualReasonResult cf,
            RealtimeWindowBundle wb) {
        Models.CauseAblationResult r = new Models.CauseAblationResult();
        r.clusterId = cluster == null ? "" : cluster.clusterId;
        r.ablationKey = "ABL|" + safe(r.clusterId) + "|" + System.currentTimeMillis();
        r.sampleCount = cluster == null ? 0 : cluster.sampleCount;
        r.omissionDrop = wb == null ? 0.0d : Math.max(0.0d, REALTIME_K3_BASELINE - wb.k3Rate48);
        r.tailDrop =
                match == null
                        ? 0.0d
                        : Math.max(0.0d, NEED_SUPPLY_READY_LINE - match.roleDiversity) * 0.35d;
        r.neighborDrop =
                match == null
                        ? 0.0d
                        : Math.max(0.0d, NEED_SUPPLY_READY_LINE - match.thirdQuality) * 0.30d;
        r.crossPositionDrop = cluster == null ? 0.0d : cluster.volatility * 0.22d;
        r.needSupplyDrop =
                match == null ? 0.20d : Math.max(0.0d, NEED_SUPPLY_STRONG_LINE - match.matchScore);
        r.thirdQualityDrop =
                match == null ? 0.12d : Math.max(0.0d, 0.100d - match.thirdQuality) * 2.2d;
        r.falseCauseCount = 0;
        if (r.omissionDrop > 0.08d) r.falseCauseCount++;
        if (r.needSupplyDrop > 0.12d) r.falseCauseCount++;
        if (r.thirdQualityDrop > 0.08d) r.falseCauseCount++;
        r.falseCauseSummary =
                "剥离反证：遗漏drop="
                        + percent(r.omissionDrop)
                        + "｜needSupplyDrop="
                        + percent(r.needSupplyDrop)
                        + "｜thirdDrop="
                        + percent(r.thirdQualityDrop);
        r.summary = r.falseCauseSummary;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    private Models.AdaptiveJudgeResult buildAdaptiveJudge(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.NeedSupplyMatchResult match,
            Models.CounterfactualReasonResult cf,
            Models.CauseAblationResult ab,
            double support,
            double risk,
            double confidence,
            double sampleReliability,
            double recentValidity) {
        Models.AdaptiveJudgeResult r = new Models.AdaptiveJudgeResult();
        r.targetIssue = getString(c, "targetIssue", "");
        r.candidateCount = 1;
        r.clusterId = cluster == null ? "" : cluster.clusterId;
        r.clusterStatus = cluster == null ? AppConfig.CLUSTER_STATUS_WATCH : cluster.status;
        r.status = r.clusterStatus;
        r.clusterHitRate = cluster == null ? 0.0d : cluster.hitRate;
        r.clusterRecentHitRate = cluster == null ? 0.0d : cluster.recentHitRate;
        r.clusterSample = cluster == null ? 0 : cluster.sampleCount;
        r.clusterConfidence = cluster == null ? 0.0d : cluster.confidence;
        r.causeScore = support;
        r.counterScore = risk;
        r.promoteScore =
                clamp01(
                        (match == null ? 0.0d : match.matchScore) * 0.34d
                                + confidence * 0.26d
                                + recentValidity * 0.20d
                                + sampleReliability * 0.20d);
        r.freezeScore =
                clamp01(
                        risk * 0.46d
                                + (ab == null ? 0.0d : ab.totalAblationScore) * 0.24d
                                + (cf == null ? 0.0d : cf.whyNotRisk) * 0.30d);
        if (r.freezeScore >= 0.64d) {
            r.action = AppConfig.ADAPTIVE_ACTION_FREEZE;
            r.domain = "FREEZE_RISK";
        } else if (r.promoteScore >= 0.58d && r.freezeScore <= 0.46d) {
            r.action = AppConfig.ADAPTIVE_ACTION_PROMOTE;
            r.domain = "PROMOTE_CAUSAL_WINDOW";
        } else {
            r.action = AppConfig.ADAPTIVE_ACTION_WATCH;
            r.domain = "WATCH_CAUSAL_WINDOW";
        }
        r.reason =
                "智能审判=FULL_LOOP|"
                        + r.action
                        + "｜promote="
                        + percent(r.promoteScore)
                        + "｜freeze="
                        + percent(r.freezeScore)
                        + "｜NeedSupply="
                        + percent(match == null ? 0.0d : match.matchScore)
                        + "｜仍由DecisionGate最终裁决";
        r.version = V20_DYNAMIC_CAUSAL_WINDOW_VERSION;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    private Models.PromotionDomainState buildPromotionDomainState(
            Models.DynamicWindowCluster cluster, Models.AdaptiveJudgeResult judge) {
        Models.PromotionDomainState p = new Models.PromotionDomainState();
        p.clusterId = cluster == null ? "" : cluster.clusterId;
        p.domainKey = "PROMO|" + safe(p.clusterId);
        p.status = judge == null ? AppConfig.CLUSTER_STATUS_WATCH : judge.status;
        p.sampleCount = cluster == null ? 0 : cluster.sampleCount;
        p.hitCount = cluster == null ? 0 : cluster.hitCount;
        p.recentSample = cluster == null ? 0 : cluster.recentSample;
        p.recentHitCount = cluster == null ? 0 : cluster.recentHitCount;
        p.hitRate = cluster == null ? 0.0d : cluster.hitRate;
        p.recentHitRate = cluster == null ? 0.0d : cluster.recentHitRate;
        p.maxLoseStreak = cluster == null ? 0 : cluster.maxLoseStreak;
        p.confidence = cluster == null ? 0.0d : cluster.confidence;
        p.action = judge == null ? AppConfig.ADAPTIVE_ACTION_WATCH : judge.action;
        p.reason = judge == null ? "" : judge.reason;
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        return p;
    }

    private void applyDynamicCausalBridgeFieldsToObject(
            Object target,
            Models.DynamicWindowCluster cluster,
            Models.PositionNeedState need,
            Models.NumberSupplyState s1,
            Models.NumberSupplyState s2,
            Models.NumberSupplyState s3,
            Models.NeedSupplyMatchResult match,
            Models.CounterfactualReasonResult cf,
            Models.CauseAblationResult ab,
            Models.AdaptiveJudgeResult judge,
            Models.PromotionDomainState promo,
            String entryName) {
        if (target == null) return;
        if (cluster != null) {
            putString(target, "dynamicClusterId", cluster.clusterId);
            putString(target, "dynamicClusterStatus", cluster.status);
            putString(target, "dynamicClusterPrototypeKey", cluster.prototypeKey);
            putString(target, "dynamicClusterFeatureKey", cluster.prototypeVector);
            putString(target, "learningClusterKey", cluster.prototypeKey);
            putString(target, "auditClusterKey", cluster.prototypeVector);
            putString(
                    target,
                    "dynamicLearningClusterVersion",
                    AppConfig.DYNAMIC_LEARNING_CLUSTER_VERSION);
            putInt(target, "dynamicClusterSample", cluster.sampleCount);
            putInt(target, "dynamicClusterHitCount", cluster.hitCount);
            putInt(target, "dynamicClusterMissCount", cluster.missCount);
            putInt(target, "dynamicClusterRecentSample", cluster.recentSample);
            putInt(target, "dynamicClusterRecentHitCount", cluster.recentHitCount);
            putDouble(target, "dynamicClusterHitRate", cluster.hitRate);
            putDouble(target, "dynamicClusterRecentHitRate", cluster.recentHitRate);
            putDouble(target, "dynamicClusterConfidence", cluster.confidence);
            putDouble(target, "dynamicClusterDistance", cluster.prototypeDistance);
            putDouble(target, "dynamicClusterVolatility", cluster.volatility);
            putInt(target, "dynamicClusterMaxLose", cluster.maxLoseStreak);
            putDouble(target, "dynamicClusterCounterScore", cluster.counterScore);
            putString(target, "dynamicClusterVersion", V20_DYNAMIC_CAUSAL_WINDOW_VERSION);
            putString(target, "dynamicClusterReason", cluster.reason);
        }
        if (need != null) {
            putString(target, "positionNeedKey", need.needKey);
            putString(target, "positionNeedSummary", need.summary);
            putDouble(target, "positionNeedStrength", need.totalNeedStrength);
            putDouble(target, "positionNeedChaosRisk", need.chaosRisk);
            putDouble(
                    target,
                    "positionNeedRepairScore",
                    need.needColdRepair + need.needOmissionReturn);
            putDouble(
                    target,
                    "positionNeedRelayScore",
                    need.needNeighborRelay + need.needCrossPositionFlow);
        }
        if (s1 != null) {
            putString(target, "numberSupplyKey", s1.supplyKey);
            putString(
                    target,
                    "numberSupplySummary",
                    s1.summary
                            + "｜"
                            + (s2 == null ? "" : s2.summary)
                            + "｜"
                            + (s3 == null ? "" : s3.summary));
            putDouble(
                    target,
                    "numberSupplyStrength",
                    (valSupply(s1) + valSupply(s2) + valSupply(s3)) / 3.0d);
            putDouble(target, "numberSupplyRisk", avgSupplyRisk(s1, s2, s3));
        }
        if (match != null) {
            putString(target, "needSupplyMatchKey", match.matchKey);
            putDouble(target, "needSupplyMatchScore", match.matchScore);
            putDouble(target, "needSupplyThirdQuality", match.thirdQuality);
            putDouble(target, "needSupplyRoleDiversity", match.roleDiversity);
            putDouble(target, "needSupplyRisk", match.supplyRisk);
            putString(target, "needSupplySummary", match.summary);
            putString(target, "k3RoleMain", match.mainRole);
            putString(target, "k3RoleAssist", match.assistRole);
            putString(target, "k3RoleDefense", match.defenseRole);
            putInt(target, "k3RoleMainNumber", match.mainRoleNumber);
            putInt(target, "k3RoleAssistNumber", match.assistRoleNumber);
            putInt(target, "k3RoleDefenseNumber", match.defenseRoleNumber);
            putString(
                    target,
                    "k3RoleStructure",
                    match.mainRole + "/" + match.assistRole + "/" + match.defenseRole);
        }
        if (cf != null) {
            putString(target, "counterfactualReasonKey", cf.reasonKey);
            putInt(target, "counterfactualRank", cf.k3Rank);
            putDouble(target, "counterfactualRankScore", cf.rankScore);
            putDouble(target, "counterfactualWhyNotRisk", cf.whyNotRisk);
            putString(
                    target,
                    "counterfactualWhyNot",
                    cf.whyNotOtherPosition + "｜" + cf.whyNotOtherNumbers);
            putString(target, "counterfactualSummary", cf.summary);
        }
        if (ab != null) {
            putString(target, "causeAblationKey", ab.ablationKey);
            putDouble(target, "causeAblationScore", ab.totalAblationScore);
            putInt(target, "causeAblationFalseCauseCount", ab.falseCauseCount);
            putString(target, "causeAblationSummary", ab.summary);
        }
        if (judge != null) {
            putInt(target, "adaptiveJudgeCandidateCount", judge.candidateCount);
            putString(target, "adaptiveJudgeStatus", judge.status);
            putString(target, "adaptiveJudgeAction", judge.action);
            putString(target, "adaptiveJudgeDomain", judge.domain);
            putDouble(target, "adaptiveClusterHitRate", judge.clusterHitRate);
            putDouble(target, "adaptiveClusterRecentHitRate", judge.clusterRecentHitRate);
            putInt(target, "adaptiveClusterSample", judge.clusterSample);
            putDouble(target, "adaptiveClusterConfidence", judge.clusterConfidence);
            putDouble(target, "adaptiveCauseScore", judge.causeScore);
            putDouble(target, "adaptiveCounterScore", judge.counterScore);
            putDouble(target, "adaptivePromoteScore", judge.promoteScore);
            putDouble(target, "adaptiveFreezeScore", judge.freezeScore);
            putString(target, "adaptiveJudgeReason", judge.reason);
            putString(target, "adaptiveJudgeVersion", V20_DYNAMIC_CAUSAL_WINDOW_VERSION);
        }
        if (promo != null) {
            putString(target, "promotionClusterStatus", promo.status);
            putDouble(target, "promotionClusterHitRate", promo.hitRate);
            putDouble(target, "promotionClusterRecentHitRate", promo.recentHitRate);
            putInt(target, "promotionClusterSample", promo.sampleCount);
            putString(target, "promotionClusterReason", promo.reason);
        }
        String why = buildWhyNoBetOrBet(target);
        putString(target, "whyBet", why.indexOf("可送最终门") >= 0 ? why : "");
        putString(target, "whyNoBet", why.indexOf("可送最终门") >= 0 ? "" : why);
        putString(target, "dynamicCausalSummary", why);
        try {
            Models.normalizeDynamicCausalBridgeFields(target);
        } catch (Throwable e) {
        }
    }

    private void saveRealtimeDynamicCausalSamples(
            DbStore store,
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue,
            String entryName) {
        if (store == null || c == null) return;
        try {
            RealtimeWindowBundle wb = buildRealtimeWindowBundle(c, clean);
            String window =
                    firstNonEmpty(
                            getString(c, "dynamicWindowStateKey", ""),
                            getString(c, "windowStateKey", ""));
            Models.WindowFeatureVector v =
                    buildDynamicWindowFeatureVector(c, clean, targetIssue, wb, window);
            Models.DynamicWindowCluster cluster =
                    buildRealtimeDynamicCluster(
                            c,
                            v,
                            wb,
                            window,
                            getDouble(c, "v20SupportScore", 0.0d),
                            getDouble(c, "v20RiskScore", 0.0d),
                            getDouble(c, "v20Confidence", 0.0d),
                            getDouble(c, "v20EvidenceContribution", 0.0d),
                            getDouble(c, "v20CounterContribution", 0.0d),
                            getDouble(c, "v20SampleReliability", 0.0d),
                            getDouble(c, "v20RecentValidity", 0.0d));
            Models.PositionNeedState need =
                    buildPositionNeedState(c, clean, targetIssue, wb, window);
            Models.NumberSupplyState s1 =
                    buildNumberSupplyState(
                            c, clean, targetIssue, c.position, c.n1, "MAIN", need, wb);
            Models.NumberSupplyState s2 =
                    buildNumberSupplyState(
                            c, clean, targetIssue, c.position, c.n2, "ASSIST", need, wb);
            Models.NumberSupplyState s3 =
                    buildNumberSupplyState(
                            c, clean, targetIssue, c.position, c.n3, "DEFENSE", need, wb);
            Models.NeedSupplyMatchResult match =
                    buildNeedSupplyMatch(c, need, s1, s2, s3, wb, cluster);
            Models.CounterfactualReasonResult cf =
                    buildCounterfactualReason(c, clean, targetIssue, cluster, match, wb);
            Models.CauseAblationResult ab = buildCauseAblation(cluster, match, cf, wb);
            Models.AdaptiveJudgeResult judge =
                    buildAdaptiveJudge(
                            c,
                            cluster,
                            match,
                            cf,
                            ab,
                            getDouble(c, "v20SupportScore", 0.0d),
                            getDouble(c, "v20RiskScore", 0.0d),
                            getDouble(c, "v20Confidence", 0.0d),
                            getDouble(c, "v20SampleReliability", 0.0d),
                            getDouble(c, "v20RecentValidity", 0.0d));
            Models.PromotionDomainState promo = buildPromotionDomainState(cluster, judge);
            store.saveDynamicWindowCluster(cluster);
            store.saveWindowFeatureSample(
                    v,
                    baseIssue,
                    getString(c, "decision", "CANDIDATE"),
                    false,
                    cluster.clusterId,
                    cluster.status,
                    cluster.prototypeDistance);
            store.savePositionNeedSample(need, 0);
            store.saveNumberSupplySample(s1, 0, false);
            store.saveNumberSupplySample(s2, 0, false);
            store.saveNumberSupplySample(s3, 0, false);
            store.saveNeedSupplyMatchSample(match);
            store.saveCounterfactualReasonSample(cf, cluster.clusterId, false);
            store.saveCauseAblationSample(ab);
            store.saveAdaptiveJudgeSample(judge);
            store.savePromotionDomainSnapshot(promo);
        } catch (Throwable e) {
        }
    }

    private String buildWhyNoBetOrBet(Object o) {
        String st = getString(o, "v20Status", "");
        String act = getString(o, "adaptiveJudgeAction", "");
        double ns = getDouble(o, "needSupplyMatchScore", 0.0d);
        double risk =
                getDouble(o, "v20RiskScore", getDouble(o, "dynamicClusterCounterScore", 0.0d));
        double cover = getDouble(o, "coverageQualityScore", 0.0d);
        double third =
                getDouble(
                        o, "thirdNumberQualityScore", getDouble(o, "needSupplyThirdQuality", 0.0d));
        if ((V20_STATUS_STRONG.equals(st)
                        || V20_STATUS_READY.equals(st)
                        || AppConfig.ADAPTIVE_ACTION_PROMOTE.equals(act))
                && ns >= NEED_SUPPLY_READY_LINE
                && risk <= 0.58d
                && third >= 0.088d) {
            return "可送最终门复核：V20="
                    + st
                    + "｜NeedSupply="
                    + percent(ns)
                    + "｜risk="
                    + percent(risk)
                    + "｜third="
                    + percent(third)
                    + "｜真实BET仍由DecisionGateEngine决定";
        }
        return "暂不直接出手：V20="
                + st
                + "｜NeedSupply="
                + percent(ns)
                + "｜risk="
                + percent(risk)
                + "｜cover="
                + percent(cover)
                + "｜third="
                + percent(third)
                + "｜原因=证据未形成稳定晋级域或存在反事实反证";
    }

    private void writeUnknownRealtimeV20(
            Models.ComboCandidate c,
            int position,
            int n1,
            int n2,
            int n3,
            String reason,
            String entryName) {
        String window = buildFallbackWindowStateKey(position, n1, n2, n3, reason);
        putRealtimeV20Fields(
                c,
                V20_STATUS_UNKNOWN,
                window,
                0.18d,
                0.58d,
                0.18d,
                0.18d,
                0.58d,
                0.0d,
                0.0d,
                "实时V20无法计算：" + safe(reason) + "，只允许SHADOW/HOLD，不支持普通BET",
                "V20_UNKNOWN：缺少有效候选或窗口",
                "V20_UNKNOWN反证：字段无效或样本不足",
                new RealtimeWindowBundle(),
                entryName);
    }

    private void putRealtimeV20Fields(
            Models.ComboCandidate c,
            String status,
            String window,
            double support,
            double risk,
            double confidence,
            double evidence,
            double counter,
            double sampleReliability,
            double recentValidity,
            String action,
            String evidenceText,
            String counterText,
            RealtimeWindowBundle wb,
            String entryName) {
        if (c == null) {
            return;
        }
        status = normalizeV20Status(status);
        if (window == null || window.length() == 0) {
            window =
                    buildFallbackWindowStateKey(
                            getInt(c, "position", 0),
                            getInt(c, "n1", 0),
                            getInt(c, "n2", 0),
                            getInt(c, "n3", 0),
                            "REALTIME_EMPTY_WINDOW");
            status = V20_STATUS_UNKNOWN;
        }
        putString(c, "v20Status", status);
        putString(c, "v20CauseStatus", status);
        putString(c, "v20DynamicStatus", status);
        putString(c, "dynamicStatus", status);
        putString(c, "dynamicCauseStatus", status);
        putString(c, "dynamicChainStatus", status);
        putString(c, "windowStateKey", window);
        putString(c, "dynamicWindowStateKey", window);
        putString(c, "probabilityWindowStateKey", window);
        putString(c, "finalDecisionWindowStateKey", window);
        putString(c, "v20WindowStateKey", window);
        putString(c, "v20ActionAdvice", safe(action));
        putString(c, "actionAdvice", safe(action));
        putString(c, "dynamicActionAdvice", safe(action));
        putString(c, "v20EvidenceText", safe(evidenceText));
        putString(c, "v20CounterText", safe(counterText));
        putString(c, "evidenceText", safe(evidenceText));
        putString(c, "counterText", safe(counterText));
        putString(c, "dynamicEvidenceText", safe(evidenceText));
        putString(c, "dynamicCounterEvidenceText", safe(counterText));
        putString(c, "v20Summary", buildRealtimeCandidateSummary(c));
        putString(c, "v20DiagnosisSummary", buildRealtimeCandidateSummary(c));
        putString(c, "dynamicCauseSummary", buildRealtimeCandidateSummary(c));
        putString(c, "dynamicSummary", buildRealtimeCandidateSummary(c));
        putDouble(c, "v20SupportScore", clamp01(support));
        putDouble(c, "v20RiskScore", clamp01(risk));
        putDouble(c, "v20Confidence", clamp01(confidence));
        putDouble(c, "v20EvidenceContribution", clamp01(evidence));
        putDouble(c, "v20CounterContribution", clamp01(counter));
        putDouble(c, "v20SampleReliability", clamp01(sampleReliability));
        putDouble(c, "v20RecentValidity", clamp01(recentValidity));
        putDouble(c, "evidenceContribution", clamp01(evidence));
        putDouble(c, "counterContribution", clamp01(counter));
        putDouble(c, "sampleReliability", clamp01(sampleReliability));
        putDouble(c, "recentValidity", clamp01(recentValidity));
        putDouble(c, "dynamicEvidenceContribution", clamp01(evidence));
        putDouble(c, "dynamicCounterContribution", clamp01(counter));
        putDouble(c, "dynamicSampleReliability", clamp01(sampleReliability));
        putDouble(c, "dynamicRecentValidity", clamp01(recentValidity));
        putDouble(
                c,
                "dynamicCauseScore",
                clamp01(support * 0.70d + confidence * 0.20d - risk * 0.32d + 0.08d));
        putDouble(c, "dynamicSupportScore", clamp01(support));
        putDouble(c, "dynamicRiskScore", clamp01(risk));
        putDouble(c, "dynamicConfidence", clamp01(confidence));
        putDouble(
                c,
                "causeScore",
                clamp01(Math.max(getDouble(c, "causeScore", 0.0d), support * 0.40d)));
        putDouble(c, "riskScore", clamp01(Math.max(getDouble(c, "riskScore", 0.0d), risk)));
        putInt(
                c,
                "dynamicPass",
                (V20_STATUS_READY.equals(status) || V20_STATUS_STRONG.equals(status)) ? 1 : 0);
        putInt(c, "dynamicHighQuality", V20_STATUS_STRONG.equals(status) ? 1 : 0);
        putInt(
                c,
                "dynamicHardRisk",
                (V20_STATUS_RISK.equals(status) || V20_STATUS_FREEZE.equals(status)) ? 1 : 0);
        putInt(
                c,
                "v20Pass",
                (V20_STATUS_READY.equals(status) || V20_STATUS_STRONG.equals(status)) ? 1 : 0);
        putInt(c, "v20HighQuality", V20_STATUS_STRONG.equals(status) ? 1 : 0);
        putInt(
                c,
                "v20HardRisk",
                (V20_STATUS_RISK.equals(status) || V20_STATUS_FREEZE.equals(status)) ? 1 : 0);
        putInt(
                c,
                "v20EmptyFlag",
                V20_STATUS_UNKNOWN.equals(status) || isNoWindowState(window) ? 1 : 0);
        putInt(c, "similarWindowSample", wb == null ? 0 : wb.sample96);
        putDouble(c, "similarWindowHitRate", wb == null ? 0.0d : wb.k3Rate96);
        putString(c, "v20FieldSource", "DynamicCauseChainEngine." + safe(entryName));
        putString(c, "v20FieldVersion", V20_DYNAMIC_UPSTREAM_VERSION);
        ensurePredrawStateCompatibilityV6(c);
        putString(c, "v20CalculationVersion", V20_DYNAMIC_UPSTREAM_VERSION);
        putString(c, "dynamicCalculationVersion", V20_DYNAMIC_UPSTREAM_VERSION);
    }

    private RealtimeWindowBundle buildRealtimeWindowBundle(
            Models.ComboCandidate c, List<Models.Draw> clean) {
        RealtimeWindowBundle wb = new RealtimeWindowBundle();
        if (c == null || clean == null || clean.size() == 0) {
            wb.sampleRisk = 1.0d;
            return wb;
        }
        wb.k3Rate12 = realtimeK3Rate(c, clean, REALTIME_WINDOW_12, wb, 12);
        wb.k3Rate24 = realtimeK3Rate(c, clean, REALTIME_WINDOW_24, wb, 24);
        wb.k3Rate48 = realtimeK3Rate(c, clean, REALTIME_WINDOW_48, wb, 48);
        wb.k3Rate96 = realtimeK3Rate(c, clean, REALTIME_WINDOW_96, wb, 96);
        wb.n1Rate96 = realtimeNumberRate(c.position, c.n1, clean, REALTIME_WINDOW_96);
        wb.n2Rate96 = realtimeNumberRate(c.position, c.n2, clean, REALTIME_WINDOW_96);
        wb.n3Rate96 = realtimeNumberRate(c.position, c.n3, clean, REALTIME_WINDOW_96);
        wb.k3Score =
                clamp01(
                        wb.k3Rate12 * 0.28d
                                + wb.k3Rate24 * 0.32d
                                + wb.k3Rate48 * 0.24d
                                + wb.k3Rate96 * 0.16d);
        wb.numberScore = clamp01((wb.n1Rate96 + wb.n2Rate96 + wb.n3Rate96) / 3.0d);
        wb.trendScore = clamp01(0.50d + (wb.k3Rate24 - wb.k3Rate96) * 1.50d);
        wb.timingScore = clamp01(0.50d + (wb.k3Rate12 - wb.k3Rate48) * 1.80d);
        wb.volatilityRisk =
                clamp01(
                        Math.abs(wb.k3Rate12 - wb.k3Rate48) * 1.40d
                                + Math.abs(wb.k3Rate24 - wb.k3Rate96) * 0.90d);
        wb.sampleRisk = wb.sample96 < 48 ? 0.70d : (wb.sample96 < 80 ? 0.48d : 0.28d);
        return wb;
    }

    private double realtimeK3Rate(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            int window,
            RealtimeWindowBundle wb,
            int which) {
        if (c == null || clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int hit = 0;
        int start = clean.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int actual = d.getNumberAtPosition(c.position);
            if (!isValidNumber(actual)) continue;
            sample++;
            if (actual == c.n1 || actual == c.n2 || actual == c.n3) {
                hit++;
            }
        }
        if (which == 12) wb.sample12 = sample;
        else if (which == 24) wb.sample24 = sample;
        else if (which == 48) wb.sample48 = sample;
        else if (which == 96) wb.sample96 = sample;
        return sample <= 0 ? 0.0d : ((double) hit) / ((double) sample);
    }

    private double realtimeNumberRate(
            int position, int number, List<Models.Draw> clean, int window) {
        if (!isValidPosition(position)
                || !isValidNumber(number)
                || clean == null
                || clean.size() == 0) {
            return 0.0d;
        }
        int sample = 0;
        int hit = 0;
        int start = clean.size() - window;
        if (start < 0) start = 0;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int actual = d.getNumberAtPosition(position);
            if (!isValidNumber(actual)) continue;
            sample++;
            if (actual == number) {
                hit++;
            }
        }
        return sample <= 0 ? 0.0d : ((double) hit) / ((double) sample);
    }

    private String buildDynamicPrototypeKey(
            Models.WindowFeatureVector v, RealtimeWindowBundle wb, double support, double risk) {
        if (v == null) return "DYN_PROTO|EMPTY";
        String hot =
                wb == null
                        ? "NO"
                        : (wb.k3Rate48 >= 0.340d
                                ? "HOT"
                                : (wb.k3Rate48 <= 0.260d ? "COLD" : "MID"));
        String tr =
                wb == null
                        ? "NA"
                        : (wb.k3Rate24 >= wb.k3Rate96 + 0.030d
                                ? "UP"
                                : (wb.k3Rate24 + 0.030d < wb.k3Rate96 ? "DOWN" : "FLAT"));
        String ev = support >= 0.60d ? "EV_HIGH" : (support >= 0.45d ? "EV_MID" : "EV_LOW");
        String rk = risk >= 0.62d ? "RISK_HIGH" : (risk >= 0.45d ? "RISK_MID" : "RISK_LOW");
        return "DYN_PROTO|P"
                + v.position
                + "|"
                + hot
                + "|"
                + tr
                + "|"
                + ev
                + "|"
                + rk
                + "|OB"
                + bucket(v.omissionPressure)
                + "|TP"
                + bucket(v.tailPressure)
                + "|CP"
                + bucket(v.crossPositionCrowd);
    }

    private String clusterStatusOf(
            Models.DynamicWindowCluster dc,
            double support,
            double risk,
            double evidence,
            double counter) {
        if (dc == null) return AppConfig.CLUSTER_STATUS_WATCH;
        if (risk >= 0.72d || counter >= 0.70d) return AppConfig.CLUSTER_STATUS_FREEZE;
        if (dc.sampleCount < 30) {
            if (support >= 0.520d && risk <= 0.620d && evidence >= 0.460d)
                return AppConfig.CLUSTER_STATUS_OBSERVE_READY;
            return AppConfig.CLUSTER_STATUS_NEW;
        }
        if (dc.hitRate >= DYNAMIC_CLUSTER_PROMOTE_LINE
                && dc.recentHitRate >= DYNAMIC_CLUSTER_PROMOTE_LINE
                && support >= 0.58d
                && risk <= 0.52d) return AppConfig.CLUSTER_STATUS_STRONG;
        if (dc.hitRate >= 0.310d && support >= 0.490d && risk <= 0.620d)
            return AppConfig.CLUSTER_STATUS_READY;
        if (dc.hitRate <= DYNAMIC_CLUSTER_FREEZE_LINE || risk >= 0.66d)
            return AppConfig.CLUSTER_STATUS_WEAK;
        return AppConfig.CLUSTER_STATUS_WATCH;
    }

    private double calcPrototypeDistance(
            Models.WindowFeatureVector v, RealtimeWindowBundle wb, double support, double risk) {
        if (v == null) return 1.0d;
        double d = 0.0d;
        d += Math.abs(v.omissionPressure - 0.50d) * 0.16d;
        d += Math.abs(v.hotColdTransition) * 0.12d;
        d += Math.abs(v.timingOffset) * 0.12d;
        d += Math.abs(v.crossPositionCrowd - 0.50d) * 0.12d;
        d += Math.max(0.0d, 0.55d - support) * 0.25d;
        d += Math.max(0.0d, risk - 0.45d) * 0.25d;
        if (wb != null) d += wb.volatilityRisk * 0.16d + wb.sampleRisk * 0.10d;
        return clamp01(d);
    }

    private int estimateRecentLoseStreak(Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null || clean == null || clean.size() == 0) return 0;
        int p = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int miss = 0;
        for (int i = clean.size() - 1; i >= 0 && i >= clean.size() - 20; i--) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int a = d.getNumberAtPosition(p);
            if (a == n1 || a == n2 || a == n3) break;
            miss++;
        }
        return miss;
    }

    private double calcK3OmissionPressure(Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null || clean == null || clean.size() == 0) return 0.0d;
        int miss = estimateRecentLoseStreak(c, clean);
        return clamp01(((double) miss) / 18.0d);
    }

    private double calcNumberOmissionPressure(int position, int number, List<Models.Draw> clean) {
        if (!isValidPosition(position)
                || !isValidNumber(number)
                || clean == null
                || clean.size() == 0) return 0.0d;
        int miss = 0;
        for (int i = clean.size() - 1; i >= 0 && i >= clean.size() - 40; i--) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int a = d.getNumberAtPosition(position);
            if (a == number) break;
            miss++;
        }
        return clamp01(((double) miss) / 28.0d);
    }

    private double calcRepeatRhythm(Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null || clean == null || clean.size() < 3) return 0.0d;
        int p = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int sample = 0;
        int near = 0;
        for (int i = Math.max(1, clean.size() - 24); i < clean.size(); i++) {
            Models.Draw cur = clean.get(i);
            Models.Draw prev = clean.get(i - 1);
            if (cur == null || prev == null || !cur.isValid() || !prev.isValid()) continue;
            int a = cur.getNumberAtPosition(p);
            int b = prev.getNumberAtPosition(p);
            if (!isValidNumber(a) || !isValidNumber(b)) continue;
            sample++;
            if (a == b || a == n1 || a == n2 || a == n3) near++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) near) / sample);
    }

    private double calcOddEvenBiasScore(int n1, int n2, int n3) {
        int odd = 0;
        if (n1 % 2 != 0) odd++;
        if (n2 % 2 != 0) odd++;
        if (n3 % 2 != 0) odd++;
        return Math.abs(odd - 1.5d) / 1.5d;
    }

    private double calcHighLowBiasScore(int n1, int n2, int n3) {
        int high = 0;
        if (n1 >= 6) high++;
        if (n2 >= 6) high++;
        if (n3 >= 6) high++;
        return Math.abs(high - 1.5d) / 1.5d;
    }

    private double calcTailPressure(int n1, int n2, int n3) {
        int t1 = n1 % 5;
        int t2 = n2 % 5;
        int t3 = n3 % 5;
        int same = 0;
        if (t1 == t2) same++;
        if (t1 == t3) same++;
        if (t2 == t3) same++;
        return clamp01(((double) same) / 3.0d);
    }

    private double calcSingleTailPressure(int number) {
        return (number == 1 || number == 10 || number == 5 || number == 6) ? 0.56d : 0.44d;
    }

    private double calcNeighborPressure(int n1, int n2, int n3) {
        int near = 0;
        if (Math.abs(n1 - n2) == 1) near++;
        if (Math.abs(n1 - n3) == 1) near++;
        if (Math.abs(n2 - n3) == 1) near++;
        return clamp01(((double) near) / 3.0d);
    }

    private double calcCrossPositionFlow(
            List<Models.Draw> clean, int position, int n1, int n2, int n3) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int hit = 0;
        for (int i = Math.max(0, clean.size() - 24); i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            for (int p = 1; p <= 10; p++) {
                if (p == position) continue;
                int a = d.getNumberAtPosition(p);
                if (!isValidNumber(a)) continue;
                sample++;
                if (a == n1 || a == n2 || a == n3) hit++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 3.0d);
    }

    private double calcCrossPositionCrowd(List<Models.Draw> clean, int n1, int n2, int n3) {
        return calcCrossPositionFlow(clean, 0, n1, n2, n3);
    }

    private double calcSingleNeighborRelay(int position, int number, List<Models.Draw> clean) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int a1 = number > 1 ? number - 1 : number;
        int a2 = number < 10 ? number + 1 : number;
        int sample = 0;
        int hit = 0;
        for (int i = Math.max(0, clean.size() - 24); i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int a = d.getNumberAtPosition(position);
            if (!isValidNumber(a)) continue;
            sample++;
            if (a == a1 || a == a2) hit++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample);
    }

    private double calcNumberCrossFlow(List<Models.Draw> clean, int number) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int hit = 0;
        for (int i = Math.max(0, clean.size() - 18); i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            for (int p = 1; p <= 10; p++) {
                sample++;
                if (d.getNumberAtPosition(p) == number) hit++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 10.0d);
    }

    private double calcNumberCrowdRelease(List<Models.Draw> clean, int number) {
        return 1.0d - Math.min(1.0d, calcNumberCrossFlow(clean, number));
    }

    private double calcSingleRepeat(int position, int number, List<Models.Draw> clean) {
        if (clean == null || clean.size() < 2) return 0.0d;
        int sample = 0;
        int hit = 0;
        for (int i = Math.max(1, clean.size() - 20); i < clean.size(); i++) {
            Models.Draw cur = clean.get(i);
            Models.Draw prev = clean.get(i - 1);
            if (cur == null || prev == null || !cur.isValid() || !prev.isValid()) continue;
            int a = cur.getNumberAtPosition(position);
            int b = prev.getNumberAtPosition(position);
            if (!isValidNumber(a) || !isValidNumber(b)) continue;
            sample++;
            if (a == number || a == b) hit++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample);
    }

    private String mainNeedTypeOf(Models.PositionNeedState p) {
        if (p == null) return "UNKNOWN";
        double max = p.needHotContinue;
        String type = "HOT_CONTINUE";
        if (p.needColdRepair > max) {
            max = p.needColdRepair;
            type = "COLD_REPAIR";
        }
        if (p.needOmissionReturn > max) {
            max = p.needOmissionReturn;
            type = "OMISSION_RETURN";
        }
        if (p.needNeighborRelay > max) {
            max = p.needNeighborRelay;
            type = "NEIGHBOR_RELAY";
        }
        if (p.needCrossPositionFlow > max) {
            max = p.needCrossPositionFlow;
            type = "CROSS_FLOW";
        }
        if (p.needChaosAvoid > max) {
            type = "CHAOS_AVOID";
        }
        return type;
    }

    private String mainSupplyTypeOf(Models.NumberSupplyState n) {
        if (n == null) return "UNKNOWN";
        double max = n.supplyHotContinue;
        String type = "HOT";
        if (n.supplyColdRepair > max) {
            max = n.supplyColdRepair;
            type = "COLD_REPAIR";
        }
        if (n.supplyOmissionReturn > max) {
            max = n.supplyOmissionReturn;
            type = "OMISSION";
        }
        if (n.supplyNeighborRelay > max) {
            max = n.supplyNeighborRelay;
            type = "NEIGHBOR";
        }
        if (n.supplyCrossPositionFlow > max) {
            type = "CROSS";
        }
        return type;
    }

    private double supplyNeedFit(Models.PositionNeedState need, Models.NumberSupplyState s) {
        if (need == null || s == null) return 0.0d;
        double fit = 0.0d;
        fit += Math.min(need.needHotContinue, s.supplyHotContinue) * 0.13d;
        fit += Math.min(need.needColdRepair, s.supplyColdRepair) * 0.13d;
        fit += Math.min(need.needOmissionReturn, s.supplyOmissionReturn) * 0.14d;
        fit += Math.min(need.needHighLowBalance, s.supplyHighLowBalance) * 0.08d;
        fit += Math.min(need.needOddEvenBalance, s.supplyOddEvenBalance) * 0.08d;
        fit += Math.min(need.needTailRepair, s.supplyTailRepair) * 0.08d;
        fit += Math.min(need.needNeighborRelay, s.supplyNeighborRelay) * 0.11d;
        fit += Math.min(need.needCrossPositionFlow, s.supplyCrossPositionFlow) * 0.10d;
        fit += Math.min(need.needCrowdRelease, s.supplyCrowdRelease) * 0.06d;
        fit += Math.min(need.needStableRepeat, s.supplyStableRepeat) * 0.05d;
        fit += Math.min(need.needChaosAvoid, 1.0d - s.supplyRisk) * 0.04d;
        return clamp01(fit);
    }

    private double valSupply(Models.NumberSupplyState s) {
        if (s == null) return 0.0d;
        return clamp01(s.totalSupplyStrength);
    }

    private double avgSupplyRisk(
            Models.NumberSupplyState s1, Models.NumberSupplyState s2, Models.NumberSupplyState s3) {
        return clamp01(
                ((s1 == null ? 0.0d : s1.supplyRisk)
                                + (s2 == null ? 0.0d : s2.supplyRisk)
                                + (s3 == null ? 0.0d : s3.supplyRisk))
                        / 3.0d);
    }

    private double calcRoleDiversity(
            Models.NumberSupplyState s1, Models.NumberSupplyState s2, Models.NumberSupplyState s3) {
        String a = s1 == null ? "" : s1.mainSupplyType;
        String b = s2 == null ? "" : s2.mainSupplyType;
        String c = s3 == null ? "" : s3.mainSupplyType;
        int diff = 1;
        if (!a.equals(b)) diff++;
        if (!a.equals(c) && !b.equals(c)) diff++;
        return clamp01(((double) diff) / 3.0d);
    }

    private int rankPositionByNeed(List<Models.Draw> clean, int position) {
        if (!isValidPosition(position)) return 10;
        if (clean == null || clean.size() == 0) return position;
        return Math.max(1, Math.min(10, position));
    }

    private int rankK3BySupply(Models.NeedSupplyMatchResult match) {
        double s = match == null ? 0.0d : match.matchScore;
        if (s >= 0.70d) return 1;
        if (s >= 0.62d) return 2;
        if (s >= 0.54d) return 3;
        if (s >= 0.46d) return 5;
        if (s >= 0.36d) return 7;
        return 9;
    }

    private double calcMirrorMissSimilarity(
            List<Models.Draw> clean, int position, int n1, int n2, int n3) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int mirrorMiss = 0;
        for (int i = Math.max(0, clean.size() - 30); i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (d == null || !d.isValid()) continue;
            int a = d.getNumberAtPosition(position);
            if (!isValidNumber(a)) continue;
            sample++;
            if (a != n1 && a != n2 && a != n3) mirrorMiss++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) mirrorMiss) / sample);
    }

    private String bucket(double v) {
        if (v >= 0.70d) return "H";
        if (v >= 0.45d) return "M";
        if (v >= 0.22d) return "L";
        return "Z";
    }

    private String buildRealtimeWindowStateKey(Models.ComboCandidate c, RealtimeWindowBundle wb) {
        if (c == null) {
            return "DYN_WS|P0|REALTIME_EMPTY";
        }
        if (wb == null) {
            wb = new RealtimeWindowBundle();
        }
        String k3 =
                wb.k3Rate48 >= 0.360d ? "K3_HOT" : (wb.k3Rate48 <= 0.240d ? "K3_COLD" : "K3_MID");
        String trend =
                wb.k3Rate24 >= wb.k3Rate96 + 0.030d
                        ? "UP"
                        : (wb.k3Rate24 + 0.030d < wb.k3Rate96 ? "DOWN" : "FLAT");
        String third =
                wb.n3Rate96 >= 0.120d ? "N3_HOT" : (wb.n3Rate96 <= 0.075d ? "N3_COLD" : "N3_MID");
        return "DYN_WS|P"
                + c.position
                + "|K"
                + c.n1
                + "_"
                + c.n2
                + "_"
                + c.n3
                + "|"
                + k3
                + "|"
                + trend
                + "|"
                + third
                + "|S"
                + wb.sample96;
    }

    private double buildRealtimeCoverageRisk(RealtimeWindowBundle wb, double cover, double third) {
        if (wb == null) {
            return 0.70d;
        }
        if (wb.sample96 < 30) {
            return 0.70d;
        }
        if (cover < 0.250d || third < 0.075d) {
            return 0.70d;
        }
        if (cover < 0.285d || third < 0.088d) {
            return 0.62d;
        }
        if (cover >= 0.340d && third >= 0.105d) {
            return 0.36d;
        }
        return 0.48d;
    }

    private String realtimeV20StatusOf(
            double support,
            double risk,
            double confidence,
            double sample,
            double finalP,
            double edge,
            double cover,
            double third) {
        if (risk >= HARD_RISK_LINE || cover < 0.230d || third < 0.065d) {
            return V20_STATUS_RISK;
        }
        if (sample < 0.22d) {
            return V20_STATUS_UNKNOWN;
        }
        if (support >= 0.68d && risk <= 0.42d && confidence >= 0.56d && finalP >= 0.380d) {
            return V20_STATUS_STRONG;
        }
        if (support >= 0.58d && risk <= 0.54d && confidence >= 0.44d && finalP >= 0.360d) {
            return V20_STATUS_READY;
        }
        if (support >= 0.42d && risk <= 0.66d) {
            return V20_STATUS_WATCH;
        }
        if (risk >= 0.72d || edge > 0.020d && support < 0.50d) {
            return V20_STATUS_RISK;
        }
        return V20_STATUS_WEAK;
    }

    private String realtimeActionAdvice(
            String status,
            double support,
            double risk,
            double confidence,
            double sample,
            double cover,
            double third,
            double edge) {
        return "V20实时证据="
                + status
                + "，support="
                + percent(support)
                + "，risk="
                + percent(risk)
                + "，confidence="
                + percent(confidence)
                + "，sample="
                + percent(sample)
                + "，cover="
                + percent(cover)
                + "，third="
                + percent(third)
                + "，edge="
                + percent(edge)
                + "；本引擎只写证据字段，不直接BET";
    }

    private String v20GateActionOf(
            String status,
            double support,
            double risk,
            double confidence,
            double evidence,
            double counter,
            double sampleReliability,
            double recentValidity) {
        status = normalizeV20Status(status);
        if (V20_STATUS_FREEZE.equals(status) || risk >= HARD_RISK_LINE || counter >= 0.78d) {
            return V20_ACTION_FREEZE;
        }
        if (V20_STATUS_RISK.equals(status) || risk >= 0.72d || counter >= 0.68d) {
            return V20_ACTION_RISK;
        }
        if ((V20_STATUS_STRONG.equals(status) || V20_STATUS_READY.equals(status))
                && evidence >= counter + 0.04d
                && sampleReliability >= 0.22d
                && recentValidity >= 0.20d
                && confidence >= 0.34d) {
            return V20_ACTION_PASS;
        }
        return V20_ACTION_WATCH;
    }

    private String buildDynamicRejectCode(
            String action,
            String status,
            double risk,
            double counter,
            double sampleReliability,
            double recentValidity) {
        if (V20_ACTION_FREEZE.equals(action)) {
            return "V20_FREEZE_HARD_RISK";
        }
        if (V20_ACTION_RISK.equals(action)) {
            return "V20_RISK_COUNTER_STRONG";
        }
        if (sampleReliability < 0.18d) {
            return "V20_SAMPLE_WEAK";
        }
        if (recentValidity < 0.16d) {
            return "V20_RECENT_VALIDITY_WEAK";
        }
        if (V20_STATUS_UNKNOWN.equals(normalizeV20Status(status))) {
            return "V20_UNKNOWN_WINDOW";
        }
        if (V20_ACTION_PASS.equals(action)) {
            return "";
        }
        if (counter >= 0.58d || risk >= 0.60d) {
            return "V20_WATCH_COUNTER";
        }
        return "V20_WATCH_NOT_READY";
    }

    private String buildV20ExecutableActionFromRecord(DynamicCauseRecord d) {
        if (d == null) {
            return V20_ACTION_WATCH;
        }
        return v20GateActionOf(
                getString(d, "v20Status", getString(d, "status", "")),
                getDouble(d, "v20SupportScore", getDouble(d, "dynamicSupportScore", 0.0d)),
                getDouble(d, "v20RiskScore", getDouble(d, "dynamicRiskScore", 0.0d)),
                getDouble(d, "v20Confidence", getDouble(d, "dynamicConfidence", 0.0d)),
                getDouble(d, "v20EvidenceContribution", getDouble(d, "evidenceContribution", 0.0d)),
                getDouble(d, "v20CounterContribution", getDouble(d, "counterContribution", 0.0d)),
                getDouble(
                        d,
                        "v20SampleReliability",
                        getDouble(d, "sampleReliability", getDouble(d, "sampleTrust", 0.0d))),
                getDouble(
                        d,
                        "v20RecentValidity",
                        getDouble(d, "recentValidity", getDouble(d, "recentEffectiveness", 0.0d))));
    }

    private String buildRealtimeCandidateSummary(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }
        return "Teacher="
                + getString(c, "teacherMatrixStatus", getString(c, "teacherStatus", ""))
                + " V20="
                + getString(c, "v20Status", "")
                + " Window="
                + getString(c, "windowStateKey", "")
                + " Support="
                + percent(getDouble(c, "v20SupportScore", 0.0d))
                + " Risk="
                + percent(getDouble(c, "v20RiskScore", 0.0d))
                + " Evidence="
                + percent(getDouble(c, "v20EvidenceContribution", 0.0d))
                + " Counter="
                + percent(getDouble(c, "v20CounterContribution", 0.0d));
    }

    private static final class RealtimeWindowBundle {
        int sample12;
        int sample24;
        int sample48;
        int sample96;
        double k3Rate12;
        double k3Rate24;
        double k3Rate48;
        double k3Rate96;
        double n1Rate96;
        double n2Rate96;
        double n3Rate96;
        double k3Score;
        double numberScore;
        double trendScore;
        double timingScore;
        double volatilityRisk;
        double sampleRisk;
    }

    public ArrayList<DynamicCauseRecord> buildDynamicChainsFull(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            List<Models.CauseChain> causeChains,
            Map<Integer, Models.PositionHealth> positionMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.FalseStrongRecord> freezeList,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList,
            List<Models.Trade> recentTrades) {
        ArrayList<DynamicCauseRecord> out = new ArrayList<DynamicCauseRecord>();
        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);

        if (list.size() == 0) {
            return out;
        }

        RuntimeStat runtime = buildRuntimeStat(recentTrades);

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            DynamicCauseRecord d =
                    buildOneDynamicChain(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            featureMap,
                            causeChains,
                            positionMap,
                            teacherMap,
                            rankList,
                            thirdGuardList,
                            ruleList,
                            shadowList,
                            freezeList,
                            seedList,
                            runtime);

            if (d != null) {
                out.add(d);
            }
        }

        applyToCandidates(list, out);
        applyToCauseChains(causeChains, out);
        rerankByDynamic(candidates);
        sortDynamicRecords(out);

        return out;
    }

    public ArrayList<DynamicCauseRecord> buildDynamicChains(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            List<Models.CauseChain> causeChains,
            Map<Integer, Models.PositionHealth> positionMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.FalseStrongRecord> freezeList,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList,
            List<Models.Trade> recentTrades) {
        return buildDynamicChainsFull(
                baseIssue,
                targetIssue,
                candidates,
                featureMap,
                causeChains,
                positionMap,
                teacherMap,
                rankList,
                thirdGuardList,
                ruleList,
                shadowList,
                freezeList,
                seedList,
                recentTrades);
    }

    public ArrayList<DynamicCauseRecord> buildDynamicChains(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildDynamicChainsFull(
                baseIssue,
                targetIssue,
                candidates,
                (Map<String, Models.PreDrawFeature>) null,
                (List<Models.CauseChain>) null,
                (Map<Integer, Models.PositionHealth>) null,
                (Map<String, Models.NumberTeacherCell>) null,
                (ArrayList<Models.RankCounterfactual>) null,
                (ArrayList<Models.ThirdGuardStat>) null,
                (ArrayList<Models.RuleHealth>) null,
                (ArrayList<Models.ShadowHealth>) null,
                (ArrayList<Models.FalseStrongRecord>) null,
                (ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord>) null,
                (List<Models.Trade>) null);
    }

    public ArrayList<DynamicCauseRecord> buildDynamicChains(List<Models.CandidateK3> candidates) {
        return buildDynamicChains("", "", candidates);
    }

    public DynamicCauseRecord buildChampionDynamicChain(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            List<Models.CauseChain> causeChains,
            Map<Integer, Models.PositionHealth> positionMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.FalseStrongRecord> freezeList,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList,
            List<Models.Trade> recentTrades) {
        ArrayList<DynamicCauseRecord> list =
                buildDynamicChainsFull(
                        baseIssue,
                        targetIssue,
                        candidates,
                        featureMap,
                        causeChains,
                        positionMap,
                        teacherMap,
                        rankList,
                        thirdGuardList,
                        ruleList,
                        shadowList,
                        freezeList,
                        seedList,
                        recentTrades);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public DynamicCauseRecord buildChampionDynamicChain(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        ArrayList<DynamicCauseRecord> list = buildDynamicChains(baseIssue, targetIssue, candidates);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<DynamicCauseRecord> dynamicChains) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }
        if (dynamicChains == null || dynamicChains.size() == 0) {
            for (int i = 0; i < candidates.size(); i++) {
                Models.CandidateK3 c = candidates.get(i);
                if (c != null) {
                    String wk =
                            buildFallbackWindowStateKey(
                                    getInt(c, "position", 0),
                                    getInt(c, "n1", 0),
                                    getInt(c, "n2", 0),
                                    getInt(c, "n3", 0),
                                    "NO_DYNAMIC_RECORD");
                    putString(c, "v20Status", V20_STATUS_UNKNOWN);
                    putString(c, "dynamicStatus", V20_STATUS_UNKNOWN);
                    putString(c, "dynamicCauseStatus", V20_STATUS_UNKNOWN);
                    putString(c, "windowStateKey", wk);
                    putString(c, "dynamicWindowStateKey", wk);
                    putString(c, "v20WindowStateKey", wk);
                    putInt(c, "v20EmptyFlag", 1);
                    putString(
                            c,
                            "v20FieldSource",
                            "DynamicCauseChainEngine.applyToCandidatesFallback");
                    putString(c, "v20FieldVersion", V20_DYNAMIC_UPSTREAM_VERSION);
                }
            }
            return;
        }

        LinkedHashMap<String, DynamicCauseRecord> map =
                new LinkedHashMap<String, DynamicCauseRecord>();

        for (int i = 0; i < dynamicChains.size(); i++) {
            DynamicCauseRecord d = dynamicChains.get(i);

            if (d == null) {
                continue;
            }

            int p = getInt(d, "position", 0);
            int mask = getInt(d, "k3Mask", getInt(d, "k3_mask", 0));

            map.put(candidateKey(p, mask), d);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            if (mask <= 0) {
                mask = buildK3Mask(getInt(c, "n1", 0), getInt(c, "n2", 0), getInt(c, "n3", 0));
                putInt(c, "k3Mask", mask);
                putInt(c, "k3_mask", mask);
            }

            DynamicCauseRecord d = map.get(candidateKey(p, mask));

            if (d == null) {
                continue;
            }

            applyOneToCandidate(c, d);
        }
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<DynamicCauseRecord> dynamicChains) {
        if (chains == null
                || dynamicChains == null
                || chains.size() == 0
                || dynamicChains.size() == 0) {
            return;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain chain = chains.get(i);

            if (chain == null) {
                continue;
            }

            int position = getInt(chain, "position", 0);
            int number = getInt(chain, "number", 0);

            if (!isValidPosition(position) || !isValidNumber(number)) {
                continue;
            }

            DynamicCauseRecord best = findBestDynamicForNumber(dynamicChains, position, number);

            if (best == null) {
                continue;
            }

            double score =
                    getDouble(best, "dynamicCauseScore", getDouble(best, "causeScore", 0.0d));
            double risk = getDouble(best, "dynamicRiskScore", getDouble(best, "riskScore", 0.0d));
            double confidence =
                    getDouble(best, "dynamicConfidence", getDouble(best, "confidence", 0.0d));
            int pass = getInt(best, "dynamicPass", getInt(best, "pass", 0));
            int hard = getInt(best, "dynamicHardRisk", 0);

            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double oldRisk = getDouble(chain, "riskScore", 0.0d);

            double dynamicCause = clampSigned((score - risk) * (0.88d + confidence * 0.24d));
            double newFinal;

            if (pass == 1) {
                newFinal = clamp01(oldFinal + posPart(dynamicCause) * 0.040d);
            } else {
                newFinal = clamp01(oldFinal - Math.max(0.014d, risk * 0.045d));
            }

            if (hard == 1) {
                newFinal = clamp01(newFinal - 0.040d);
            }

            double newRisk = clamp01(Math.max(oldRisk, risk));

            putDouble(chain, "dynamicCause", dynamicCause);
            putDouble(chain, "dynamicCauseScore", score);
            putDouble(chain, "dynamicRiskScore", risk);
            putDouble(chain, "dynamicConfidence", confidence);
            putInt(chain, "dynamicPass", pass);
            putInt(chain, "dynamicHardRisk", hard);
            putString(
                    chain,
                    "dynamicGrade",
                    getString(best, "dynamicGrade", getString(best, "grade", "")));

            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);
            putDouble(chain, "riskScore", newRisk);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜动态因链：score="
                            + percent(score)
                            + "，risk="
                            + percent(risk)
                            + "，confidence="
                            + percent(confidence)
                            + "，pass="
                            + pass
                            + "，hard="
                            + hard
                            + "，说明=动态因链只汇总证据，不直接BET";

            putString(chain, "summaryText", old + add);
        }
    }

    private DynamicCauseRecord buildOneDynamicChain(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            Map<String, Models.PreDrawFeature> featureMap,
            List<Models.CauseChain> causeChains,
            Map<Integer, Models.PositionHealth> positionMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.FalseStrongRecord> freezeList,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList,
            RuntimeStat runtime) {
        if (c == null) {
            return null;
        }

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        if (mask <= 0) {
            mask = buildK3Mask(n1, n2, n3);
            putInt(c, "k3Mask", mask);
            putInt(c, "k3_mask", mask);
        }

        if (!isValidPosition(position)
                || !isValidNumber(n1)
                || !isValidNumber(n2)
                || !isValidNumber(n3)) {
            return null;
        }

        DynamicParts p =
                buildParts(
                        c,
                        position,
                        n1,
                        n2,
                        n3,
                        mask,
                        rankNo,
                        featureMap,
                        causeChains,
                        positionMap,
                        teacherMap,
                        rankList,
                        thirdGuardList,
                        ruleList,
                        shadowList,
                        freezeList,
                        seedList,
                        runtime);

        double support = 0.0d;
        support += p.probScore * 0.12d;
        support += p.scanScore * 0.06d;
        support += p.featureScore * 0.09d;
        support += p.causalScore * 0.13d;
        support += p.positionScore * 0.08d;
        support += p.teacherScore * 0.09d;
        support += p.rankScore * 0.08d;
        support += p.thirdGuardScore * 0.08d;
        support += p.ruleScore * 0.08d;
        support += p.shadowScore * 0.07d;
        support += p.seedScore * 0.07d;
        support += p.similarWindowScore * 0.05d;
        support += p.evidenceContribution * 0.05d;
        support += p.structureScore * 0.04d;
        support += p.runtimeHealth * 0.03d;
        support += p.confidence * 0.06d;

        double risk = 0.0d;
        risk += p.rawRisk * 0.10d;
        risk += p.featureRisk * 0.07d;
        risk += p.causalRisk * 0.10d;
        risk += p.positionRisk * 0.08d;
        risk += p.teacherRisk * 0.08d;
        risk += p.rankRisk * 0.08d;
        risk += p.thirdGuardRisk * 0.10d;
        risk += p.ruleRisk * 0.10d;
        risk += p.shadowRisk * 0.08d;
        risk += p.freezeRisk * 0.10d;
        risk += p.seedRisk * 0.06d;
        risk += p.windowStateRisk * 0.05d;
        risk += p.counterContribution * 0.05d;
        risk += p.weakSampleRisk * 0.04d;
        risk += p.runtimeRisk * 0.04d;
        risk += p.conflictRisk * 0.04d;

        double dynamicSupport = clamp01(support);
        double dynamicRisk = clamp01(risk + Math.max(0.0d, 0.48d - support) * 0.18d);
        double dynamicConfidence =
                clamp01(
                        p.confidence * 0.58d
                                + p.sampleConfidence * 0.24d
                                + (1.0d - dynamicRisk) * 0.18d);

        double dynamicScore =
                clamp01(
                        dynamicSupport * 0.72d
                                + dynamicConfidence * 0.18d
                                - dynamicRisk * 0.38d
                                + 0.08d);

        int hardRisk = isHardRisk(p, dynamicRisk) ? 1 : 0;
        int pass = hardRisk == 0 && dynamicScore >= PASS_LINE && dynamicRisk <= RISK_LINE ? 1 : 0;
        int highQuality =
                hardRisk == 0
                                && dynamicScore >= GRADE_A_LINE
                                && dynamicRisk <= 0.50d
                                && dynamicConfidence >= 0.48d
                        ? 1
                        : 0;

        String grade = gradeOf(dynamicScore, dynamicRisk, dynamicConfidence, hardRisk);
        String status = statusOfV20(pass, highQuality, hardRisk, dynamicScore, dynamicRisk, p);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        dynamicScore,
                        dynamicSupport,
                        dynamicRisk,
                        dynamicConfidence,
                        pass,
                        highQuality,
                        hardRisk,
                        grade,
                        status,
                        p);

        DynamicCauseRecord d = new DynamicCauseRecord();

        d.chainKey = buildDynamicKey(targetIssue, position, mask);
        d.dynamicKey = d.chainKey;
        d.recordKey = d.chainKey;
        d.baseIssue = safe(baseIssue);
        d.targetIssue = safe(targetIssue);
        d.createdAt = System.currentTimeMillis();

        d.rankNo = rankNo;
        d.rank = rankNo;
        d.position = position;
        d.n1 = n1;
        d.n2 = n2;
        d.n3 = n3;
        d.k3Mask = mask;
        d.k3_mask = mask;

        d.dynamicCauseScore = dynamicScore;
        d.causeScore = dynamicScore;
        d.dynamicSupportScore = dynamicSupport;
        d.supportScore = dynamicSupport;
        d.dynamicRiskScore = dynamicRisk;
        d.riskScore = dynamicRisk;
        d.dynamicConfidence = dynamicConfidence;
        d.confidence = dynamicConfidence;

        d.dynamicPass = pass;
        d.pass = pass;
        d.dynamicHighQuality = highQuality;
        d.highQuality = highQuality;
        d.dynamicHardRisk = hardRisk;
        d.hardRisk = hardRisk;

        d.dynamicGrade = grade;
        d.grade = grade;
        d.status = status;
        d.level = status;
        d.v20Status = status;
        d.v20CauseStatus = status;
        d.v20SupportScore = dynamicSupport;
        d.v20RiskScore = dynamicRisk;
        d.v20Confidence = dynamicConfidence;
        d.v20EvidenceContribution = p.evidenceContribution;
        d.v20CounterContribution = p.counterContribution;
        d.v20SampleReliability = p.sampleTrust;
        d.v20RecentValidity = p.recentEffectiveness;
        d.v20ActionAdvice = p.actionAdvice;
        d.v20WindowStateKey = p.windowStateKey;
        d.v20EmptyFlag =
                V20_STATUS_UNKNOWN.equals(status) || isNoWindowState(p.windowStateKey) ? 1 : 0;
        d.positionCauseScore = p.positionScore;
        d.numberCauseScore =
                clamp01(
                        p.causalScore * 0.45d
                                + p.teacherScore * 0.30d
                                + p.structureScore * 0.15d
                                + p.similarWindowScore * 0.10d);
        d.counterfactualScore = p.rankScore;
        d.falseStrongRisk = p.freezeRisk;
        d.seedExpansionScore = p.seedScore;
        d.sampleReliability = p.sampleTrust;
        d.recentValidity = p.recentEffectiveness;
        d.v20DiagnosisSummary = summary;
        d.v20Action = p.v20Action;
        d.dynamicAction = p.v20Action;
        d.evidenceTotal = p.evidenceContribution;
        d.antiEvidenceTotal = p.counterContribution;
        d.antiEvidenceScore = p.counterContribution;
        d.counterEvidenceScore = p.counterContribution;
        d.netEvidenceScore = clampSigned(p.evidenceContribution - p.counterContribution);
        d.positionNeedScore = p.positionNeedScore;
        d.numberSupplyScore = p.numberSupplyScore;
        d.coverageQuality = p.coverageQuality;
        d.thirdQuality = p.thirdQuality;
        d.thirdDragScore = p.thirdDragScore;
        d.counterfactualRisk = p.counterfactualRisk;
        d.similarWindowConfidence = p.similarWindowConfidence;
        d.v20RejectCode =
                buildDynamicRejectCode(
                        p.v20Action,
                        status,
                        dynamicRisk,
                        p.counterContribution,
                        p.sampleTrust,
                        p.recentEffectiveness);
        d.fieldChainVersion = V20_DYNAMIC_CAUSE_CHAIN_FIELD_CHAIN_VERSION;

        d.probScore = p.probScore;
        d.scanScore = p.scanScore;
        d.featureScore = p.featureScore;
        d.causalScore = p.causalScore;
        d.positionScore = p.positionScore;
        d.teacherScore = p.teacherScore;
        d.rankScore = p.rankScore;
        d.thirdGuardScore = p.thirdGuardScore;
        d.ruleScore = p.ruleScore;
        d.shadowScore = p.shadowScore;
        d.seedScore = p.seedScore;
        d.seedExpansionScore = p.seedScore;
        d.structureScore = p.structureScore;

        d.rawRisk = p.rawRisk;
        d.featureRisk = p.featureRisk;
        d.causalRisk = p.causalRisk;
        d.positionRisk = p.positionRisk;
        d.teacherRisk = p.teacherRisk;
        d.rankRisk = p.rankRisk;
        d.thirdGuardRisk = p.thirdGuardRisk;
        d.ruleRisk = p.ruleRisk;
        d.shadowRisk = p.shadowRisk;
        d.freezeRisk = p.freezeRisk;
        d.falseStrongRisk = p.freezeRisk;
        d.seedRisk = p.seedRisk;
        d.weakSampleRisk = p.weakSampleRisk;
        d.runtimeRisk = p.runtimeRisk;
        d.conflictRisk = p.conflictRisk;

        d.sampleConfidence = p.sampleConfidence;
        d.teacherSample = p.teacherSample;
        d.shadowSample = p.shadowSample;
        d.chainSample = p.chainSample;
        d.ruleCount = p.ruleCount;

        d.stateKey = p.stateKey;
        d.shapeKey = p.shapeKey;
        d.sameClassKey = p.sameClassKey;
        d.seedKey = p.seedKey;
        d.windowStateKey = p.windowStateKey;
        d.hotColdBucket = p.hotColdBucket;
        d.omissionBucket = p.omissionBucket;
        d.densityBucket = p.densityBucket;
        d.repeatBucket = p.repeatBucket;
        d.oddEvenBias = p.oddEvenBias;
        d.highLowBias = p.highLowBias;
        d.neighborTailState = p.neighborTailState;
        d.positionCrowdState = p.positionCrowdState;
        d.crossPositionSync = p.crossPositionSync;
        d.crossPositionSqueeze = p.crossPositionSqueeze;
        d.recentLoseStreak = p.recentLoseStreak;
        d.timingShift = p.timingShift;
        d.featureDistance = p.featureDistance;
        d.similarWindowSample = p.similarWindowSample;
        d.similarWindowHitRate = p.similarWindowHitRate;
        d.similarWindowRecentRate = p.similarWindowRecentRate;
        d.sampleTrust = p.sampleTrust;
        d.sampleReliability = p.sampleTrust;
        d.recentEffectiveness = p.recentEffectiveness;
        d.recentValidity = p.recentEffectiveness;
        d.evidenceContribution = p.evidenceContribution;
        d.counterContribution = p.counterContribution;
        d.evidenceText = p.evidenceText;
        d.counterText = p.counterText;
        d.actionAdvice = p.actionAdvice;
        d.calculationVersion = V20_DYNAMIC_VERSION;

        d.summaryText = summary;
        d.reasonText = summary;

        return d;
    }

    private DynamicParts buildParts(
            Models.CandidateK3 c,
            int position,
            int n1,
            int n2,
            int n3,
            int mask,
            int rankNo,
            Map<String, Models.PreDrawFeature> featureMap,
            List<Models.CauseChain> causeChains,
            Map<Integer, Models.PositionHealth> positionMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.FalseStrongRecord> freezeList,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList,
            RuntimeStat runtime) {
        DynamicParts p = new DynamicParts();

        p.finalP = getDouble(c, "finalP", 0.0d);
        p.dynamicGate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        p.edge = getDouble(c, "edge", p.finalP - p.dynamicGate);
        p.rawRisk = getDouble(c, "riskScore", 0.0d);

        p.probScore = calcProbScore(p.finalP, p.dynamicGate, p.edge);
        p.scanScore = getDouble(c, "scanScore", getDouble(c, "featureScore", 0.0d));

        FeatureBundle fb = readFeatures(position, n1, n2, n3, featureMap);
        p.featureScore = fb.score;
        p.featureRisk = fb.risk;
        p.shapeKey = fb.shapeKey;
        p.stateKey = fb.stateKey;
        p.windowStateKey = fb.windowStateKey;
        p.hotColdBucket = fb.hotColdBucket;
        p.omissionBucket = fb.omissionBucket;
        p.densityBucket = fb.densityBucket;
        p.repeatBucket = fb.repeatBucket;
        p.oddEvenBias = buildOddEvenBias(n1, n2, n3);
        p.highLowBias = buildHighLowBias(n1, n2, n3);
        p.neighborTailState = fb.neighborTailState;
        p.positionCrowdState = fb.positionCrowdState;
        p.crossPositionSync = fb.crossPositionSync;
        p.crossPositionSqueeze = fb.crossPositionSqueeze;
        p.recentLoseStreak = fb.recentLoseStreak;
        p.timingShift = fb.timingShift;
        p.featureDistance = fb.featureDistance;

        ChainBundle cb = readCauseChains(position, n1, n2, n3, causeChains);
        p.causalScore = Math.max(getDouble(c, "causeScore", 0.0d), cb.score);
        p.causalRisk = cb.risk;
        p.chainSample = cb.sample;

        PositionBundle pb = readPosition(position, positionMap);
        p.positionScore = Math.max(getDouble(c, "positionCauseScore", 0.0d), pb.health);
        p.positionRisk = Math.max(getDouble(c, "positionWrongRisk", 0.0d), pb.risk);

        TeacherBundle tb = readTeacher(position, n1, n2, n3, teacherMap);
        p.teacherScore = Math.max(getDouble(c, "teacherNumberScore", 0.0d), tb.score);
        p.teacherRisk = Math.max(getDouble(c, "teacherNumberRisk", 0.0d), tb.risk);
        p.teacherSample = tb.sample;
        p.teacherConfidence = tb.confidence;

        RankBundle rb = readRank(position, mask, rankList);
        p.rankScore = Math.max(getDouble(c, "counterfactualScore", 0.0d), rb.score);
        p.rankRisk = Math.max(getDouble(c, "counterfactualRisk", 0.0d), rb.risk);

        GuardBundle gb = readThirdGuard(position, mask, thirdGuardList);
        p.thirdGuardScore = Math.max(getDouble(c, "thirdGuardScore", 0.0d), gb.score);
        p.thirdGuardRisk = Math.max(getDouble(c, "thirdGuardRisk", 0.0d), gb.risk);
        p.thirdGuardPass = gb.pass;
        p.thirdGuardHard = gb.hard;

        RuleBundle rule = readRules(position, mask, ruleList);
        p.ruleScore = Math.max(getDouble(c, "ruleHealthScore", 0.0d), rule.health);
        p.ruleRisk = Math.max(getDouble(c, "ruleHealthRisk", 0.0d), rule.risk);
        p.ruleCount = rule.count;
        p.rulePassCount = rule.passCount;
        p.ruleHardCount = rule.hardCount;

        ShadowBundle shadow = readShadow(position, mask, shadowList);
        p.shadowScore = Math.max(getDouble(c, "shadowHealthScore", 0.0d), shadow.health);
        p.shadowRisk = Math.max(getDouble(c, "shadowHealthRisk", 0.0d), shadow.risk);
        p.shadowSample = shadow.sample;
        p.shadowPass = shadow.pass;
        p.shadowPromote = shadow.promote;
        p.shadowHard = shadow.hard;

        FreezeBundle freeze = readFreeze(position, mask, freezeList);
        p.freezeRisk = Math.max(getDouble(c, "falseStrongFreezeRisk", 0.0d), freeze.risk);
        p.freezeFlag = freeze.freeze;
        p.freezeHard = freeze.hard;

        SeedBundle seed = readSeed(position, mask, seedList);
        p.seedScore =
                Math.max(
                        getDouble(c, "seedExpandScore", getDouble(c, "seedScore", 0.0d)),
                        seed.score);
        p.seedRisk = Math.max(getDouble(c, "seedRisk", 0.0d), seed.risk);
        p.seedSimilarity = seed.similarity;
        p.seedPass = seed.pass;
        p.seedHard = seed.hard;
        p.seedKey = seed.seedKey;

        p.structureScore = calcStructureScore(n1, n2, n3, tb);
        p.weakSampleRisk = calcWeakSampleRisk(c, p);
        p.conflictRisk = calcConflictRisk(p);
        p.runtimeHealth = runtime == null ? 0.0d : runtime.health;
        p.runtimeRisk = runtime == null ? 0.0d : runtime.risk;

        p.sampleConfidence =
                calcSampleConfidence(p.teacherSample, p.shadowSample, p.chainSample, p.ruleCount);
        p.confidence =
                clamp01(
                        p.sampleConfidence * 0.34d
                                + p.teacherConfidence * 0.18d
                                + p.featureScore * 0.16d
                                + p.ruleScore * 0.12d
                                + p.thirdGuardScore * 0.10d
                                + p.shadowScore * 0.10d);

        p.similarWindowSample = p.teacherSample + p.shadowSample + p.chainSample + p.ruleCount;
        p.similarWindowHitRate =
                clamp01(
                        p.teacherScore * 0.34d
                                + p.shadowScore * 0.22d
                                + p.causalScore * 0.22d
                                + p.ruleScore * 0.14d
                                + p.seedScore * 0.08d);
        p.similarWindowRecentRate =
                clamp01(p.similarWindowHitRate * 0.70d + p.runtimeHealth * 0.30d);
        p.sampleTrust = sampleTrust(p.similarWindowSample);
        p.recentEffectiveness = p.similarWindowRecentRate;
        p.similarWindowScore =
                clamp01(
                        p.similarWindowHitRate * 0.58d
                                + p.recentEffectiveness * 0.24d
                                + p.sampleTrust * 0.18d);
        p.windowStateRisk =
                clamp01(
                        p.featureDistance * 0.28d
                                + (1.0d - p.sampleTrust) * 0.22d
                                + p.conflictRisk * 0.22d
                                + p.weakSampleRisk * 0.16d
                                + p.runtimeRisk * 0.12d);
        p.evidenceContribution =
                clamp01(
                        p.probScore * 0.16d
                                + p.featureScore * 0.12d
                                + p.causalScore * 0.15d
                                + p.teacherScore * 0.15d
                                + p.rankScore * 0.10d
                                + p.thirdGuardScore * 0.10d
                                + p.ruleScore * 0.10d
                                + p.similarWindowScore * 0.12d);
        p.counterContribution =
                clamp01(
                        p.rawRisk * 0.12d
                                + p.featureRisk * 0.10d
                                + p.causalRisk * 0.12d
                                + p.teacherRisk * 0.12d
                                + p.rankRisk * 0.10d
                                + p.thirdGuardRisk * 0.12d
                                + p.ruleRisk * 0.12d
                                + p.freezeRisk * 0.10d
                                + p.windowStateRisk * 0.10d);
        p.evidenceText = buildDynamicEvidenceText(p);
        p.counterText = buildDynamicCounterText(p);
        p.actionAdvice = buildDynamicActionAdvice(p);
        p.positionNeedScore =
                clamp01(
                        Math.max(
                                getDouble(c, "positionNeedScore", 0.0d),
                                getDouble(c, "positionNeedStrength", 0.0d)));
        p.numberSupplyScore =
                clamp01(
                        Math.max(
                                getDouble(c, "numberSupplyScore", 0.0d),
                                getDouble(c, "numberSupplyStrength", 0.0d)));
        p.coverageQuality =
                clamp01(
                        Math.max(
                                getDouble(c, "coverageQuality", 0.0d),
                                getDouble(c, "coverageQualityScore", Math.max(0.0d, p.probScore))));
        p.thirdQuality =
                clamp01(
                        Math.max(
                                getDouble(c, "thirdQuality", 0.0d),
                                getDouble(
                                        c,
                                        "thirdNumberQualityScore",
                                        getDouble(
                                                c, "needSupplyThirdQuality", p.thirdGuardScore))));
        p.thirdDragScore = clamp01(1.0d - p.thirdQuality);
        p.counterfactualRisk =
                clamp01(
                        Math.max(
                                getDouble(c, "counterfactualRisk", 0.0d),
                                getDouble(c, "counterfactualWhyNotRisk", p.rankRisk)));
        p.similarWindowConfidence =
                clamp01(
                        p.sampleTrust * 0.42d
                                + p.recentEffectiveness * 0.34d
                                + p.similarWindowScore * 0.24d);
        p.v20Action = buildV20ExecutableAction(p);

        p.sameClassKey = buildSameClassKey(position, fb, p);

        return p;
    }

    private void applyOneToCandidate(Models.CandidateK3 c, DynamicCauseRecord d) {
        if (c == null || d == null) {
            return;
        }

        double score = getDouble(d, "dynamicCauseScore", getDouble(d, "causeScore", 0.0d));
        double support = getDouble(d, "dynamicSupportScore", getDouble(d, "supportScore", 0.0d));
        double risk = getDouble(d, "dynamicRiskScore", getDouble(d, "riskScore", 0.0d));
        double confidence = getDouble(d, "dynamicConfidence", getDouble(d, "confidence", 0.0d));
        int pass = getInt(d, "dynamicPass", getInt(d, "pass", 0));
        int high = getInt(d, "dynamicHighQuality", getInt(d, "highQuality", 0));
        int hard = getInt(d, "dynamicHardRisk", getInt(d, "hardRisk", 0));

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;

        if (pass == 1) {
            newCause = clamp01(oldCause * 0.88d + score * 0.12d);
        } else {
            newCause = clamp01(oldCause * 0.94d - risk * 0.040d);
        }

        if (hard == 1) {
            newCause = clamp01(newCause - 0.045d);
        }

        double newRisk = clamp01(Math.max(oldRisk, risk));

        putDouble(c, "dynamicCauseScore", score);
        putDouble(c, "dynamicSupportScore", support);
        putDouble(c, "dynamicRiskScore", risk);
        putDouble(c, "dynamicConfidence", confidence);
        putInt(c, "dynamicPass", pass);
        putInt(c, "dynamicHighQuality", high);
        putInt(c, "dynamicHardRisk", hard);
        putString(c, "dynamicGrade", getString(d, "dynamicGrade", getString(d, "grade", "")));
        putString(c, "dynamicStatus", getString(d, "status", ""));
        putString(c, "dynamicCauseStatus", getString(d, "status", ""));
        putString(c, "dynamicCauseSummary", getString(d, "summaryText", ""));
        putString(c, "dynamicWindowStateKey", getString(d, "windowStateKey", ""));
        putString(c, "dynamicEvidenceText", getString(d, "evidenceText", ""));
        putString(c, "dynamicCounterEvidenceText", getString(d, "counterText", ""));
        putString(c, "dynamicActionAdvice", getString(d, "actionAdvice", ""));
        putDouble(c, "dynamicEvidenceContribution", getDouble(d, "evidenceContribution", 0.0d));
        putDouble(c, "dynamicCounterContribution", getDouble(d, "counterContribution", 0.0d));
        putDouble(c, "dynamicFeatureDistance", getDouble(d, "featureDistance", 1.0d));
        putInt(c, "dynamicSimilarWindowSample", getInt(d, "similarWindowSample", 0));
        putDouble(c, "dynamicSimilarWindowHitRate", getDouble(d, "similarWindowHitRate", 0.0d));
        putDouble(c, "dynamicSampleTrust", getDouble(d, "sampleTrust", 0.0d));
        putDouble(c, "dynamicRecentEffectiveness", getDouble(d, "recentEffectiveness", 0.0d));
        putString(c, "dynamicCalculationVersion", V20_DYNAMIC_VERSION);
        putString(c, "v20Action", getString(d, "v20Action", buildV20ExecutableActionFromRecord(d)));
        putString(
                c,
                "dynamicAction",
                getString(d, "dynamicAction", getString(c, "v20Action", "WATCH")));
        putString(c, "dynamicV20Action", getString(c, "v20Action", "WATCH"));
        putDouble(
                c,
                "evidenceTotal",
                getDouble(d, "evidenceTotal", getDouble(d, "evidenceContribution", 0.0d)));
        putDouble(
                c,
                "antiEvidenceTotal",
                getDouble(d, "antiEvidenceTotal", getDouble(d, "counterContribution", 0.0d)));
        putDouble(
                c,
                "antiEvidenceScore",
                getDouble(d, "antiEvidenceScore", getDouble(d, "counterEvidenceScore", 0.0d)));
        putDouble(
                c,
                "counterEvidenceScore",
                getDouble(d, "counterEvidenceScore", getDouble(d, "counterContribution", 0.0d)));
        putDouble(
                c,
                "netEvidenceScore",
                getDouble(
                        d,
                        "netEvidenceScore",
                        getDouble(c, "evidenceTotal", 0.0d)
                                - getDouble(c, "antiEvidenceTotal", 0.0d)));
        putDouble(
                c,
                "positionNeedScore",
                getDouble(d, "positionNeedScore", getDouble(c, "positionNeedScore", 0.0d)));
        putDouble(
                c,
                "numberSupplyScore",
                getDouble(d, "numberSupplyScore", getDouble(c, "numberSupplyScore", 0.0d)));
        putDouble(
                c,
                "coverageQuality",
                getDouble(d, "coverageQuality", getDouble(c, "coverageQuality", 0.0d)));
        putDouble(
                c,
                "thirdQuality",
                getDouble(d, "thirdQuality", getDouble(c, "thirdQuality", 0.0d)));
        putDouble(
                c,
                "thirdDragScore",
                getDouble(d, "thirdDragScore", getDouble(c, "thirdDragScore", 0.0d)));
        putDouble(
                c,
                "counterfactualRisk",
                getDouble(d, "counterfactualRisk", getDouble(c, "counterfactualRisk", 0.0d)));
        putDouble(
                c,
                "similarWindowConfidence",
                getDouble(
                        d,
                        "similarWindowConfidence",
                        getDouble(c, "similarWindowConfidence", 0.0d)));
        putString(c, "rejectCode", getString(d, "v20RejectCode", getString(c, "rejectCode", "")));
        putString(c, "finalDecisionRejectCode", getString(c, "rejectCode", ""));
        putString(c, "fieldChainVersion", V20_DYNAMIC_CAUSE_CHAIN_FIELD_CHAIN_VERSION);

        applyV20DiagnosisAliasesToCandidate(c, d, newCause, newRisk);

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendReason(c, getString(d, "summaryText", ""));
    }

    private void applyV20DiagnosisAliasesToCandidate(
            Models.CandidateK3 c, DynamicCauseRecord d, double newCause, double newRisk) {
        if (c == null || d == null) {
            return;
        }

        String status = normalizeV20Status(getString(d, "v20Status", getString(d, "status", "")));
        String action = getString(d, "actionAdvice", "");
        String summary = getString(d, "summaryText", "");
        String evidence = getString(d, "evidenceText", "");
        String counter = getString(d, "counterText", "");
        String windowKey = getString(d, "windowStateKey", "");

        double support =
                getDouble(
                        d,
                        "v20SupportScore",
                        getDouble(d, "dynamicSupportScore", getDouble(d, "supportScore", 0.0d)));
        double risk =
                getDouble(
                        d,
                        "v20RiskScore",
                        getDouble(d, "dynamicRiskScore", getDouble(d, "riskScore", 0.0d)));
        double confidence =
                getDouble(
                        d,
                        "v20Confidence",
                        getDouble(d, "dynamicConfidence", getDouble(d, "confidence", 0.0d)));
        double evidenceContribution = getDouble(d, "evidenceContribution", 0.0d);
        double counterContribution = getDouble(d, "counterContribution", 0.0d);
        double sampleReliability =
                getDouble(d, "sampleReliability", getDouble(d, "sampleTrust", 0.0d));
        double recentValidity =
                getDouble(d, "recentValidity", getDouble(d, "recentEffectiveness", 0.0d));
        int similarSample = getInt(d, "similarWindowSample", 0);
        double similarRate = getDouble(d, "similarWindowHitRate", 0.0d);

        putString(c, "v20Status", status);
        putString(c, "v20CauseStatus", status);
        putString(c, "v20DynamicStatus", status);
        putString(c, "v20ActionAdvice", action);
        putString(c, "actionAdvice", action);
        putString(c, "v20Summary", summary);
        putString(c, "v20CauseSummary", summary);
        putString(c, "v20EvidenceText", evidence);
        putString(c, "v20CounterText", counter);
        putString(c, "evidenceText", evidence);
        putString(c, "counterText", counter);
        putString(c, "windowStateKey", windowKey);
        putString(c, "dynamicWindowStateKey", windowKey);
        putString(c, "v20WindowStateKey", windowKey);
        putString(c, "v20CalculationVersion", V20_DYNAMIC_VERSION);

        putDouble(c, "v20SupportScore", support);
        putDouble(c, "v20RiskScore", risk);
        putDouble(c, "v20Confidence", confidence);
        putDouble(c, "v20EvidenceContribution", evidenceContribution);
        putDouble(c, "v20CounterContribution", counterContribution);
        putDouble(c, "v20SampleReliability", sampleReliability);
        putDouble(c, "v20RecentValidity", recentValidity);
        putInt(
                c,
                "v20EmptyFlag",
                isNoWindowState(windowKey) || V20_STATUS_UNKNOWN.equals(status) ? 1 : 0);
        putString(c, "v20DiagnosisSummary", summary);
        putString(c, "v20WindowStateKey", windowKey);
        putString(c, "v20FieldSource", "DynamicCauseChainEngine");
        putString(c, "v20FieldVersion", V20_DYNAMIC_VERSION);
        putDouble(
                c,
                "v20CauseScore",
                getDouble(d, "dynamicCauseScore", getDouble(d, "causeScore", 0.0d)));
        putDouble(
                c,
                "v20DynamicScore",
                getDouble(d, "dynamicCauseScore", getDouble(d, "causeScore", 0.0d)));
        putDouble(c, "causeScore", newCause);
        putDouble(
                c,
                "positionCauseScore",
                getDouble(d, "positionCauseScore", getDouble(d, "positionScore", 0.0d)));
        putDouble(
                c,
                "numberCauseScore",
                getDouble(d, "numberCauseScore", getDouble(d, "causalScore", 0.0d)));
        putDouble(
                c,
                "counterfactualScore",
                getDouble(d, "counterfactualScore", getDouble(d, "rankScore", 0.0d)));
        putDouble(c, "thirdGuardScore", getDouble(d, "thirdGuardScore", 0.0d));
        putDouble(
                c,
                "ruleHealthScore",
                getDouble(d, "ruleHealthScore", getDouble(d, "ruleScore", 0.0d)));
        putDouble(
                c,
                "shadowHealthScore",
                getDouble(d, "shadowHealthScore", getDouble(d, "shadowScore", 0.0d)));
        putDouble(
                c,
                "falseStrongRisk",
                getDouble(d, "falseStrongRisk", getDouble(d, "freezeRisk", 0.0d)));
        putDouble(
                c,
                "falseStrongFreezeRisk",
                getDouble(d, "falseStrongRisk", getDouble(d, "freezeRisk", 0.0d)));
        putDouble(
                c,
                "seedExpansionScore",
                getDouble(d, "seedExpansionScore", getDouble(d, "seedScore", 0.0d)));
        putDouble(
                c,
                "seedExpandScore",
                getDouble(d, "seedExpansionScore", getDouble(d, "seedScore", 0.0d)));
        putDouble(c, "evidenceContribution", evidenceContribution);
        putDouble(c, "counterContribution", counterContribution);
        putDouble(c, "sampleReliability", sampleReliability);
        putDouble(c, "recentValidity", recentValidity);
        putDouble(c, "dynamicEvidenceContribution", evidenceContribution);
        putDouble(c, "dynamicCounterContribution", counterContribution);
        putDouble(c, "dynamicSampleReliability", sampleReliability);
        putDouble(c, "dynamicRecentValidity", recentValidity);
        putDouble(c, "sampleTrust", sampleReliability);
        putDouble(c, "recentEffectiveness", recentValidity);
        putDouble(c, "featureDistance", getDouble(d, "featureDistance", 1.0d));
        putInt(c, "similarWindowSample", similarSample);
        putDouble(c, "similarWindowHitRate", similarRate);
        putDouble(c, "riskScore", newRisk);

        putInt(c, "v20Pass", getInt(d, "dynamicPass", getInt(d, "pass", 0)));
        putInt(c, "v20HighQuality", getInt(d, "dynamicHighQuality", getInt(d, "highQuality", 0)));
        putInt(c, "v20HardRisk", getInt(d, "dynamicHardRisk", getInt(d, "hardRisk", 0)));

        try {
            putString(c, "dynamicClusterId", getString(d, "dynamicClusterId", ""));
            putString(
                    c,
                    "dynamicClusterStatus",
                    getString(d, "dynamicClusterStatus", getString(d, "status", "")));
            putString(c, "positionNeedSummary", getString(d, "positionNeedSummary", ""));
            putString(c, "numberSupplySummary", getString(d, "numberSupplySummary", ""));
            putDouble(c, "needSupplyMatchScore", getDouble(d, "needSupplyMatchScore", 0.0d));
            putString(c, "counterfactualWhyNot", getString(d, "counterfactualWhyNot", ""));
            putDouble(c, "causeAblationScore", getDouble(d, "causeAblationScore", 0.0d));
            putString(c, "adaptiveJudgeAction", getString(d, "adaptiveJudgeAction", ""));
            putString(c, "whyNoBet", getString(d, "whyNoBet", ""));
            putString(c, "whyBet", getString(d, "whyBet", ""));
            putString(c, "dynamicCausalSummary", getString(d, "dynamicCausalSummary", ""));
            Models.normalizeDynamicCausalBridgeFields(c);
        } catch (Throwable e) {
        }
    }

    public void applyDynamicRecordToObject(Object target, DynamicCauseRecord d) {
        if (target == null || d == null) {
            return;
        }
        applyV20DiagnosisAliasesToObject(target, d);
    }

    public void applyBestDynamicRecordToObject(
            Object target, List<DynamicCauseRecord> records, int position, int n1, int n2, int n3) {
        if (target == null || records == null || records.size() == 0) {
            return;
        }
        int mask = buildK3Mask(n1, n2, n3);
        DynamicCauseRecord best = null;
        for (int i = 0; i < records.size(); i++) {
            DynamicCauseRecord d = records.get(i);
            if (d == null) {
                continue;
            }
            if (d.position == position && d.k3Mask == mask) {
                best = d;
                break;
            }
        }
        if (best != null) {
            applyV20DiagnosisAliasesToObject(target, best);
        }
    }

    public void applyV20FieldsFromCandidateToObject(Object target, Object candidate) {
        if (target == null || candidate == null) {
            return;
        }
        String status =
                firstNonEmpty(
                        getString(candidate, "v20Status", ""),
                        getString(candidate, "v20CauseStatus", ""),
                        getString(candidate, "dynamicStatus", ""),
                        getString(candidate, "dynamicCauseStatus", ""));
        String windowKey =
                firstNonEmpty(
                        getString(candidate, "v20WindowStateKey", ""),
                        getString(candidate, "windowStateKey", ""),
                        getString(candidate, "dynamicWindowStateKey", ""));
        if (status.length() == 0) {
            status = isNoWindowState(windowKey) ? V20_STATUS_UNKNOWN : V20_STATUS_WATCH;
        }
        status = normalizeV20Status(status);
        if (windowKey.length() == 0) {
            int p = getInt(candidate, "position", 0);
            int n1 = getInt(candidate, "n1", 0);
            int n2 = getInt(candidate, "n2", 0);
            int n3 = getInt(candidate, "n3", 0);
            windowKey = buildFallbackWindowStateKey(p, n1, n2, n3, "COPY_FALLBACK");
            status = V20_STATUS_UNKNOWN;
        }
        putString(target, "v20Status", status);
        putString(target, "v20CauseStatus", status);
        putString(target, "v20DynamicStatus", status);
        putString(target, "dynamicStatus", status);
        putString(target, "dynamicCauseStatus", status);
        putString(target, "dynamicChainStatus", status);
        putString(target, "v20WindowStateKey", windowKey);
        putString(target, "windowStateKey", windowKey);
        putString(target, "dynamicWindowStateKey", windowKey);
        putString(
                target,
                "v20ActionAdvice",
                firstNonEmpty(
                        getString(candidate, "v20ActionAdvice", ""),
                        getString(candidate, "dynamicActionAdvice", ""),
                        getString(candidate, "actionAdvice", "")));
        putString(
                target,
                "actionAdvice",
                firstNonEmpty(
                        getString(candidate, "v20ActionAdvice", ""),
                        getString(candidate, "dynamicActionAdvice", ""),
                        getString(candidate, "actionAdvice", "")));

        putString(
                target,
                "v20Action",
                firstNonEmpty(
                        getString(candidate, "v20Action", ""),
                        getString(candidate, "dynamicAction", ""),
                        v20GateActionOf(
                                status,
                                getDouble(candidate, "v20SupportScore", 0.0d),
                                getDouble(candidate, "v20RiskScore", 0.0d),
                                getDouble(candidate, "v20Confidence", 0.0d),
                                getDouble(candidate, "v20EvidenceContribution", 0.0d),
                                getDouble(candidate, "v20CounterContribution", 0.0d),
                                getDouble(candidate, "v20SampleReliability", 0.0d),
                                getDouble(candidate, "v20RecentValidity", 0.0d))));
        putString(target, "dynamicAction", getString(target, "v20Action", "WATCH"));
        putString(target, "dynamicV20Action", getString(target, "v20Action", "WATCH"));

        putString(
                target,
                "v20Summary",
                firstNonEmpty(
                        getString(candidate, "v20Summary", ""),
                        getString(candidate, "v20DiagnosisSummary", ""),
                        getString(candidate, "dynamicCauseSummary", "")));
        putString(
                target,
                "v20DiagnosisSummary",
                firstNonEmpty(
                        getString(candidate, "v20DiagnosisSummary", ""),
                        getString(candidate, "v20Summary", ""),
                        getString(candidate, "dynamicCauseSummary", "")));
        putString(
                target,
                "dynamicCauseSummary",
                firstNonEmpty(
                        getString(candidate, "dynamicCauseSummary", ""),
                        getString(candidate, "v20Summary", ""),
                        getString(candidate, "v20DiagnosisSummary", "")));
        putDouble(
                target,
                "v20SupportScore",
                getDouble(
                        candidate,
                        "v20SupportScore",
                        getDouble(candidate, "dynamicSupportScore", 0.0d)));
        putDouble(
                target,
                "v20RiskScore",
                getDouble(
                        candidate, "v20RiskScore", getDouble(candidate, "dynamicRiskScore", 0.0d)));
        putDouble(
                target,
                "v20Confidence",
                getDouble(
                        candidate,
                        "v20Confidence",
                        getDouble(candidate, "dynamicConfidence", 0.0d)));
        putDouble(
                target,
                "v20EvidenceContribution",
                getDouble(
                        candidate,
                        "v20EvidenceContribution",
                        getDouble(
                                candidate,
                                "evidenceContribution",
                                getDouble(candidate, "dynamicEvidenceContribution", 0.0d))));
        putDouble(
                target,
                "v20CounterContribution",
                getDouble(
                        candidate,
                        "v20CounterContribution",
                        getDouble(
                                candidate,
                                "counterContribution",
                                getDouble(candidate, "dynamicCounterContribution", 0.0d))));
        putDouble(
                target,
                "v20SampleReliability",
                getDouble(
                        candidate,
                        "v20SampleReliability",
                        getDouble(
                                candidate,
                                "sampleReliability",
                                getDouble(candidate, "dynamicSampleReliability", 0.0d))));
        putDouble(
                target,
                "v20RecentValidity",
                getDouble(
                        candidate,
                        "v20RecentValidity",
                        getDouble(
                                candidate,
                                "recentValidity",
                                getDouble(candidate, "dynamicRecentValidity", 0.0d))));
        putDouble(
                target,
                "evidenceTotal",
                getDouble(
                        candidate,
                        "evidenceTotal",
                        getDouble(candidate, "v20EvidenceContribution", 0.0d)));
        putDouble(
                target,
                "antiEvidenceTotal",
                getDouble(
                        candidate,
                        "antiEvidenceTotal",
                        getDouble(candidate, "v20CounterContribution", 0.0d)));
        putDouble(
                target,
                "counterEvidenceScore",
                getDouble(
                        candidate,
                        "counterEvidenceScore",
                        getDouble(candidate, "antiEvidenceTotal", 0.0d)));
        putDouble(
                target,
                "netEvidenceScore",
                getDouble(
                        candidate,
                        "netEvidenceScore",
                        getDouble(candidate, "evidenceTotal", 0.0d)
                                - getDouble(candidate, "antiEvidenceTotal", 0.0d)));
        putDouble(
                target,
                "positionNeedScore",
                getDouble(
                        candidate,
                        "positionNeedScore",
                        getDouble(candidate, "positionNeedStrength", 0.0d)));
        putDouble(
                target,
                "numberSupplyScore",
                getDouble(
                        candidate,
                        "numberSupplyScore",
                        getDouble(candidate, "numberSupplyStrength", 0.0d)));
        putDouble(
                target,
                "coverageQuality",
                getDouble(
                        candidate,
                        "coverageQuality",
                        getDouble(candidate, "coverageQualityScore", 0.0d)));
        putDouble(
                target,
                "thirdQuality",
                getDouble(
                        candidate,
                        "thirdQuality",
                        getDouble(
                                candidate,
                                "thirdNumberQualityScore",
                                getDouble(candidate, "needSupplyThirdQuality", 0.0d))));
        putDouble(
                target,
                "thirdDragScore",
                getDouble(
                        candidate,
                        "thirdDragScore",
                        1.0d - getDouble(target, "thirdQuality", 0.0d)));
        putDouble(
                target,
                "counterfactualRisk",
                getDouble(
                        candidate,
                        "counterfactualRisk",
                        getDouble(candidate, "counterfactualWhyNotRisk", 0.0d)));
        putDouble(
                target,
                "similarWindowConfidence",
                getDouble(candidate, "similarWindowConfidence", 0.0d));
        putString(target, "rejectCode", getString(candidate, "rejectCode", ""));
        putString(
                target,
                "finalDecisionRejectCode",
                getString(
                        candidate,
                        "finalDecisionRejectCode",
                        getString(candidate, "rejectCode", "")));
        putString(target, "fieldChainVersion", V20_DYNAMIC_CAUSE_CHAIN_FIELD_CHAIN_VERSION);

        putInt(
                target,
                "v20EmptyFlag",
                isNoWindowState(windowKey) || V20_STATUS_UNKNOWN.equals(status) ? 1 : 0);
        putString(target, "v20FieldSource", "DynamicCauseChainEngine.copyCandidate");
        putString(target, "v20FieldVersion", V20_DYNAMIC_VERSION);
    }

    private void applyV20DiagnosisAliasesToObject(Object target, DynamicCauseRecord d) {
        if (target == null || d == null) {
            return;
        }
        String status = normalizeV20Status(getString(d, "v20Status", getString(d, "status", "")));
        String action = getString(d, "actionAdvice", "");
        String summary = getString(d, "summaryText", "");
        String evidence = getString(d, "evidenceText", "");
        String counter = getString(d, "counterText", "");
        String windowKey = getString(d, "windowStateKey", "");

        putString(target, "v20Status", status);
        putString(target, "v20CauseStatus", status);
        putString(target, "v20DynamicStatus", status);
        putString(target, "dynamicStatus", status);
        putString(target, "dynamicCauseStatus", status);
        putString(target, "dynamicChainStatus", status);
        putString(target, "actionAdvice", action);
        putString(target, "v20ActionAdvice", action);
        putString(target, "v20Summary", summary);
        putString(target, "v20DiagnosisSummary", summary);
        putString(target, "dynamicCauseSummary", summary);
        putString(target, "dynamicSummary", summary);
        putString(target, "evidenceText", evidence);
        putString(target, "counterText", counter);
        putString(target, "windowStateKey", windowKey);
        putString(target, "dynamicWindowStateKey", windowKey);
        putString(target, "v20WindowStateKey", windowKey);

        putDouble(
                target,
                "v20SupportScore",
                getDouble(d, "v20SupportScore", getDouble(d, "dynamicSupportScore", 0.0d)));
        putDouble(
                target,
                "v20RiskScore",
                getDouble(d, "v20RiskScore", getDouble(d, "dynamicRiskScore", 0.0d)));
        putDouble(
                target,
                "v20Confidence",
                getDouble(d, "v20Confidence", getDouble(d, "dynamicConfidence", 0.0d)));
        putDouble(
                target,
                "v20EvidenceContribution",
                getDouble(
                        d, "v20EvidenceContribution", getDouble(d, "evidenceContribution", 0.0d)));
        putDouble(
                target,
                "v20CounterContribution",
                getDouble(d, "v20CounterContribution", getDouble(d, "counterContribution", 0.0d)));
        putDouble(
                target,
                "v20SampleReliability",
                getDouble(
                        d,
                        "v20SampleReliability",
                        getDouble(d, "sampleReliability", getDouble(d, "sampleTrust", 0.0d))));
        putDouble(
                target,
                "v20RecentValidity",
                getDouble(
                        d,
                        "v20RecentValidity",
                        getDouble(d, "recentValidity", getDouble(d, "recentEffectiveness", 0.0d))));
        putInt(
                target,
                "v20EmptyFlag",
                getInt(
                        d,
                        "v20EmptyFlag",
                        isNoWindowState(windowKey) || V20_STATUS_UNKNOWN.equals(status) ? 1 : 0));
        putString(target, "v20FieldSource", "DynamicCauseChainEngine");
        putString(target, "v20FieldVersion", V20_DYNAMIC_VERSION);
        putString(
                target,
                "v20Action",
                getString(d, "v20Action", buildV20ExecutableActionFromRecord(d)));
        putString(
                target,
                "dynamicAction",
                getString(d, "dynamicAction", getString(target, "v20Action", "WATCH")));
        putString(target, "dynamicV20Action", getString(target, "v20Action", "WATCH"));
        putDouble(
                target,
                "evidenceTotal",
                getDouble(d, "evidenceTotal", getDouble(d, "evidenceContribution", 0.0d)));
        putDouble(
                target,
                "antiEvidenceTotal",
                getDouble(d, "antiEvidenceTotal", getDouble(d, "counterContribution", 0.0d)));
        putDouble(
                target,
                "antiEvidenceScore",
                getDouble(d, "antiEvidenceScore", getDouble(d, "counterEvidenceScore", 0.0d)));
        putDouble(
                target,
                "counterEvidenceScore",
                getDouble(d, "counterEvidenceScore", getDouble(d, "counterContribution", 0.0d)));
        putDouble(
                target,
                "netEvidenceScore",
                getDouble(
                        d,
                        "netEvidenceScore",
                        getDouble(target, "evidenceTotal", 0.0d)
                                - getDouble(target, "antiEvidenceTotal", 0.0d)));
        putDouble(
                target,
                "positionNeedScore",
                getDouble(d, "positionNeedScore", getDouble(d, "positionCauseScore", 0.0d)));
        putDouble(
                target,
                "numberSupplyScore",
                getDouble(d, "numberSupplyScore", getDouble(d, "numberCauseScore", 0.0d)));
        putDouble(target, "coverageQuality", getDouble(d, "coverageQuality", 0.0d));
        putDouble(target, "thirdQuality", getDouble(d, "thirdQuality", 0.0d));
        putDouble(target, "thirdDragScore", getDouble(d, "thirdDragScore", 0.0d));
        putDouble(target, "counterfactualRisk", getDouble(d, "counterfactualRisk", 0.0d));
        putDouble(target, "similarWindowConfidence", getDouble(d, "similarWindowConfidence", 0.0d));
        putString(
                target,
                "rejectCode",
                getString(d, "v20RejectCode", getString(target, "rejectCode", "")));
        putString(target, "finalDecisionRejectCode", getString(target, "rejectCode", ""));
        putString(target, "fieldChainVersion", V20_DYNAMIC_CAUSE_CHAIN_FIELD_CHAIN_VERSION);

        putDouble(target, "causeScore", getDouble(d, "causeScore", 0.0d));
        putDouble(
                target,
                "positionCauseScore",
                getDouble(d, "positionCauseScore", getDouble(d, "positionScore", 0.0d)));
        putDouble(
                target,
                "numberCauseScore",
                getDouble(d, "numberCauseScore", getDouble(d, "causalScore", 0.0d)));
        putDouble(
                target,
                "counterfactualScore",
                getDouble(d, "counterfactualScore", getDouble(d, "rankScore", 0.0d)));
        putDouble(target, "thirdGuardScore", getDouble(d, "thirdGuardScore", 0.0d));
        putDouble(
                target,
                "ruleHealthScore",
                getDouble(d, "ruleHealthScore", getDouble(d, "ruleScore", 0.0d)));
        putDouble(
                target,
                "shadowHealthScore",
                getDouble(d, "shadowHealthScore", getDouble(d, "shadowScore", 0.0d)));
        putDouble(
                target,
                "falseStrongRisk",
                getDouble(d, "falseStrongRisk", getDouble(d, "freezeRisk", 0.0d)));
        putDouble(
                target,
                "seedExpansionScore",
                getDouble(d, "seedExpansionScore", getDouble(d, "seedScore", 0.0d)));
        putDouble(target, "evidenceContribution", getDouble(d, "evidenceContribution", 0.0d));
        putDouble(target, "counterContribution", getDouble(d, "counterContribution", 0.0d));
        putDouble(
                target,
                "sampleReliability",
                getDouble(d, "sampleReliability", getDouble(d, "sampleTrust", 0.0d)));
        putDouble(
                target,
                "recentValidity",
                getDouble(d, "recentValidity", getDouble(d, "recentEffectiveness", 0.0d)));
        putDouble(
                target,
                "dynamicSampleReliability",
                getDouble(d, "sampleReliability", getDouble(d, "sampleTrust", 0.0d)));
        putDouble(
                target,
                "dynamicRecentValidity",
                getDouble(d, "recentValidity", getDouble(d, "recentEffectiveness", 0.0d)));
        putInt(target, "similarWindowSample", getInt(d, "similarWindowSample", 0));
        putDouble(target, "similarWindowHitRate", getDouble(d, "similarWindowHitRate", 0.0d));
    }

    private void rerankByDynamic(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = dynamicCandidateScore(a);
                        double sb = dynamicCandidateScore(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int ma = getInt(a, "k3Mask", getInt(a, "k3_mask", 0));
                        int mb = getInt(b, "k3Mask", getInt(b, "k3_mask", 0));

                        return ma - mb;
                    }
                });

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }
    }

    private void sortDynamicRecords(ArrayList<DynamicCauseRecord> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<DynamicCauseRecord>() {
                    public int compare(DynamicCauseRecord a, DynamicCauseRecord b) {
                        double sa = dynamicRecordScore(a);
                        double sb = dynamicRecordScore(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            DynamicCauseRecord d = list.get(i);

            if (d != null) {
                d.rank = i + 1;
                d.rankNo = i + 1;
            }
        }
    }

    private static double dynamicRecordScore(DynamicCauseRecord d) {
        if (d == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += d.dynamicCauseScore * 0.44d;
        score += d.dynamicSupportScore * 0.22d;
        score += d.dynamicConfidence * 0.18d;
        score -= d.dynamicRiskScore * 0.20d;

        if (d.dynamicPass == 1) {
            score += 0.020d;
        }

        if (d.dynamicHighQuality == 1) {
            score += 0.016d;
        }

        if (d.dynamicHardRisk == 1) {
            score -= 0.080d;
        }

        return clamp01(score);
    }

    private static double dynamicCandidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double base = candidateScore(c);
        double dynamic = getDouble(c, "dynamicCauseScore", 0.0d);
        double support = getDouble(c, "dynamicSupportScore", 0.0d);
        double risk = getDouble(c, "dynamicRiskScore", 0.0d);
        double confidence = getDouble(c, "dynamicConfidence", 0.0d);
        int pass = getInt(c, "dynamicPass", 0);
        int high = getInt(c, "dynamicHighQuality", 0);
        int hard = getInt(c, "dynamicHardRisk", 0);

        double score =
                base * 0.72d
                        + dynamic * 0.18d
                        + support * 0.06d
                        + confidence * 0.04d
                        - risk * 0.10d;

        if (pass == 1) {
            score += 0.012d;
        }

        if (high == 1) {
            score += 0.010d;
        }

        if (hard == 1) {
            score -= 0.070d;
        }

        return clamp01(score);
    }

    private static double candidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - gate);
        double cause = getDouble(c, "causeScore", 0.0d);
        double rule = getDouble(c, "ruleHealthScore", 0.0d);
        double guard = getDouble(c, "thirdGuardScore", 0.0d);
        double shadow = getDouble(c, "shadowHealthScore", 0.0d);
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double rank = getDouble(c, "counterfactualScore", 0.0d);
        double seed = getDouble(c, "seedExpandScore", getDouble(c, "seedScore", 0.0d));
        double freeze = getDouble(c, "falseStrongFreezeRisk", 0.0d);
        double risk = getDouble(c, "riskScore", 0.0d);

        double score = 0.0d;

        score += finalP * 0.18d;
        score += posPart(edge * 4.0d) * 0.07d;
        score += cause * 0.18d;
        score += rule * 0.10d;
        score += guard * 0.10d;
        score += shadow * 0.09d;
        score += teacher * 0.08d;
        score += rank * 0.08d;
        score += seed * 0.08d;

        score -= risk * 0.10d;
        score -= freeze * 0.06d;

        return clamp01(score);
    }

    private double calcProbScore(double finalP, double gate, double edge) {
        double pScore = clamp01((finalP - 0.30d) / 0.18d);
        double edgeScore = clamp01((edge + 0.030d) / 0.090d);
        double gateScore = gate <= 0.0d ? 0.50d : (finalP >= gate ? 0.72d : 0.30d);

        return clamp01(pScore * 0.38d + edgeScore * 0.42d + gateScore * 0.20d);
    }

    private FeatureBundle readFeatures(
            int position, int n1, int n2, int n3, Map<String, Models.PreDrawFeature> featureMap) {
        FeatureBundle b = new FeatureBundle();

        if (featureMap == null) {
            fillNoFeatureBundle(b, position, n1, n2, n3, "NO_FEATURE_MAP");
            return b;
        }

        Models.PreDrawFeature f1 = getFeature(featureMap, position, n1);
        Models.PreDrawFeature f2 = getFeature(featureMap, position, n2);
        Models.PreDrawFeature f3 = getFeature(featureMap, position, n3);

        Models.PreDrawFeature[] arr = new Models.PreDrawFeature[] {f1, f2, f3};

        double score = 0.0d;
        double risk = 0.0d;
        double trend = 0.0d;
        double gap = 0.0d;
        double transition = 0.0d;
        double recovery = 0.0d;
        double density = 0.0d;
        double repeat = 0.0d;
        double neighborTail = 0.0d;
        double crowd = 0.0d;
        double sync = 0.0d;
        double squeeze = 0.0d;
        double timing = 0.0d;
        int maxMissing = 0;
        int maxLose = 0;
        int used = 0;

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < arr.length; i++) {
            Models.PreDrawFeature f = arr[i];

            if (f == null) {
                continue;
            }

            used++;

            double featureScore = getDouble(f, "featureScore", 0.0d);
            double confidence = getDouble(f, "confidence", 0.0d);
            double freqSlope = getDouble(f, "freqSlope", 0.0d);
            double returnZone = getDouble(f, "returnZoneScore", 0.0d);
            double transitionLift = getDouble(f, "transitionLift", 0.0d);
            double recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
            double falseRecovery = getDouble(f, "falseRecoveryRisk", 0.0d);
            double conflict = getDouble(f, "transitionConflict", 0.0d);
            double noise = getDouble(f, "positionNoiseScore", 0.0d);
            double densityScore = getDouble(f, "densityScore", featureScore);
            double repeatScore = getDouble(f, "repeatScore", getDouble(f, "repeatDensity", 0.0d));
            double neighborScore =
                    getDouble(f, "neighborScore", getDouble(f, "nearNumberScore", 0.0d));
            double tailScore = getDouble(f, "sameTailScore", getDouble(f, "tailScore", 0.0d));
            double crowdScore =
                    getDouble(f, "positionCrowdScore", getDouble(f, "crowdScore", 0.0d));
            double syncScore =
                    getDouble(f, "crossPositionSync", getDouble(f, "positionSyncScore", 0.0d));
            double squeezeScore =
                    getDouble(f, "crossPositionSqueeze", getDouble(f, "squeezeScore", 0.0d));
            double timingScore = getDouble(f, "timingShiftScore", transitionLift);
            int missingGap = getInt(f, "missingGap", getInt(f, "omissionGap", 0));
            int loseStreak = getInt(f, "recentLoseStreak", getInt(f, "loseStreak", 0));

            double localScore =
                    clamp01(
                            featureScore * 0.34d
                                    + confidence * 0.18d
                                    + posPart(freqSlope) * 0.12d
                                    + returnZone * 0.14d
                                    + transitionLift * 0.12d
                                    + recoveryQuality * 0.10d);

            double localRisk =
                    clamp01(
                            falseRecovery * 0.30d
                                    + conflict * 0.24d
                                    + noise * 0.22d
                                    + (1.0d - confidence) * 0.12d);

            score += localScore;
            risk += localRisk;
            trend += posPart(freqSlope);
            gap += returnZone;
            transition += transitionLift;
            recovery += recoveryQuality;
            density += densityScore;
            repeat += repeatScore;
            neighborTail += Math.max(neighborScore, tailScore);
            crowd += crowdScore;
            sync += syncScore;
            squeeze += squeezeScore;
            timing += timingScore;
            if (missingGap > maxMissing) {
                maxMissing = missingGap;
            }
            if (loseStreak > maxLose) {
                maxLose = loseStreak;
            }

            if (sb.length() > 0) {
                sb.append("_");
            }

            sb.append(bucketSigned(freqSlope));
            sb.append("-");
            sb.append(bucket01(returnZone));
            sb.append("-");
            sb.append(bucket01(transitionLift));
        }

        if (used <= 0) {
            fillNoFeatureBundle(b, position, n1, n2, n3, "NO_FEATURE_ROWS");
            return b;
        }

        b.score = clamp01(score / used);
        b.risk = clamp01(risk / used);
        b.trend = clamp01(trend / used);
        b.gap = clamp01(gap / used);
        b.transition = clamp01(transition / used);
        b.recovery = clamp01(recovery / used);
        b.density = clamp01(density / used);
        b.repeat = clamp01(repeat / used);
        b.neighborTail = clamp01(neighborTail / used);
        b.crowd = clamp01(crowd / used);
        b.sync = clamp01(sync / used);
        b.squeeze = clamp01(squeeze / used);
        b.timing = clamp01(timing / used);
        b.maxMissingGap = maxMissing;
        b.recentLoseStreak = maxLose;
        b.hotColdBucket = "HC_" + bucketSigned(b.trend);
        b.omissionBucket = buildOmissionBucket(maxMissing, b.gap);
        b.densityBucket = "DEN_" + bucket01(b.density);
        b.repeatBucket = "REP_" + bucket01(b.repeat);
        b.neighborTailState = "NT_" + bucket01(b.neighborTail);
        b.positionCrowdState = "CROWD_" + bucket01(b.crowd);
        b.crossPositionSync = "SYNC_" + bucket01(b.sync);
        b.crossPositionSqueeze = "SQUEEZE_" + bucket01(b.squeeze);
        b.timingShift = "TIME_" + bucket01(b.timing);
        b.featureDistance = calcFeatureDistance(b);
        b.shapeKey = "DYN_SHAPE|P" + position + "|" + sb.toString();
        b.windowStateKey = buildWindowStateKey(position, n1, n2, n3, b);
        b.stateKey = b.windowStateKey;

        return b;
    }

    private void fillNoFeatureBundle(
            FeatureBundle b, int position, int n1, int n2, int n3, String reason) {
        if (b == null) {
            return;
        }
        b.score = 0.0d;
        b.risk = 0.42d;
        b.trend = 0.0d;
        b.gap = 0.0d;
        b.transition = 0.0d;
        b.recovery = 0.0d;
        b.density = 0.0d;
        b.repeat = 0.0d;
        b.neighborTail = 0.0d;
        b.crowd = 0.0d;
        b.sync = 0.0d;
        b.squeeze = 0.0d;
        b.timing = 0.0d;
        b.featureDistance = 1.0d;
        b.hotColdBucket = "HC_UNKNOWN";
        b.omissionBucket = "MISS_UNKNOWN";
        b.densityBucket = "DEN_UNKNOWN";
        b.repeatBucket = "REP_UNKNOWN";
        b.neighborTailState = "NT_UNKNOWN";
        b.positionCrowdState = "CROWD_UNKNOWN";
        b.crossPositionSync = "SYNC_UNKNOWN";
        b.crossPositionSqueeze = "SQUEEZE_UNKNOWN";
        b.timingShift = "TIME_UNKNOWN";
        b.shapeKey = "DYN_SHAPE|P" + position + "|" + reason;
        b.windowStateKey = buildFallbackWindowStateKey(position, n1, n2, n3, reason);
        b.stateKey = b.windowStateKey;
    }

    private String buildFallbackWindowStateKey(
            int position, int n1, int n2, int n3, String reason) {
        String r = safe(reason);
        if (r.length() == 0) {
            r = "UNKNOWN";
        }
        return "DYN_WS|P"
                + position
                + "|K"
                + n1
                + "_"
                + n2
                + "_"
                + n3
                + "|V20_"
                + r
                + "|"
                + buildOddEvenBias(n1, n2, n3)
                + "|"
                + buildHighLowBias(n1, n2, n3);
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> map, int position, int number) {
        if (map == null || !isValidPosition(position) || !isValidNumber(number)) {
            return null;
        }

        Models.PreDrawFeature f = map.get(keyOf(position, number));

        if (f != null) {
            return f;
        }

        f = map.get(String.valueOf(position) + "|" + String.valueOf(number));

        if (f != null) {
            return f;
        }

        return map.get("P" + position + "_N" + number);
    }

    private ChainBundle readCauseChains(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        ChainBundle b = new ChainBundle();

        if (chains == null) {
            return b;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n = getInt(c, "number", 0);

            if (p != position) {
                continue;
            }

            if (n != n1 && n != n2 && n != n3) {
                continue;
            }

            b.score += getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            b.risk += getDouble(c, "riskScore", 0.0d);
            b.sample++;
        }

        if (b.sample > 0) {
            b.score = clamp01(b.score / b.sample);
            b.risk = clamp01(b.risk / b.sample);
        }

        return b;
    }

    private PositionBundle readPosition(
            int position, Map<Integer, Models.PositionHealth> positionMap) {
        PositionBundle b = new PositionBundle();

        if (positionMap == null) {
            return b;
        }

        Models.PositionHealth h = positionMap.get(Integer.valueOf(position));

        if (h == null) {
            return b;
        }

        b.health = getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
        b.risk =
                getDouble(
                        h,
                        "riskScore",
                        getDouble(h, "positionRiskScore", getDouble(h, "wrongRisk", 0.0d)));
        b.driftRisk = getDouble(h, "driftRisk", getDouble(h, "positionDriftRisk", 0.0d));

        return b;
    }

    private TeacherBundle readTeacher(
            int position,
            int n1,
            int n2,
            int n3,
            Map<String, Models.NumberTeacherCell> teacherMap) {
        TeacherBundle b = new TeacherBundle();

        if (teacherMap == null) {
            return b;
        }

        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(teacherMap, position, nums[i]);

            if (c == null) {
                continue;
            }

            b.score += getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            b.risk += getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            b.confidence += getDouble(c, "confidence", getDouble(c, "sampleConfidence", 0.0d));
            b.sample += getInt(c, "sampleCount", getInt(c, "sample", 0));
            b.used++;
        }

        if (b.used > 0) {
            b.score = clamp01(b.score / b.used);
            b.risk = clamp01(b.risk / b.used);
            b.confidence = clamp01(b.confidence / b.used);
        }

        return b;
    }

    private Models.NumberTeacherCell getTeacherCell(
            Map<String, Models.NumberTeacherCell> map, int position, int number) {
        if (map == null || !isValidPosition(position) || !isValidNumber(number)) {
            return null;
        }

        Models.NumberTeacherCell c = map.get(keyOf(position, number));

        if (c != null) {
            return c;
        }

        c = map.get(String.valueOf(position) + "|" + String.valueOf(number));

        if (c != null) {
            return c;
        }

        return map.get("P" + position + "_N" + number);
    }

    private RankBundle readRank(
            int position, int mask, ArrayList<Models.RankCounterfactual> rankList) {
        RankBundle b = new RankBundle();

        if (rankList == null) {
            return b;
        }

        for (int i = 0; i < rankList.size(); i++) {
            Models.RankCounterfactual r = rankList.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", getInt(r, "chosenPosition", 0));
            int m = getInt(r, "k3Mask", getInt(r, "chosenK3Mask", 0));

            if (p == position && m == mask) {
                b.score = getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));
                b.risk =
                        clamp01(
                                getDouble(r, "missedStrongRisk", 0.0d) * 0.30d
                                        + getDouble(r, "nearConflictRisk", 0.0d) * 0.25d
                                        + getDouble(r, "falseChampionRisk", 0.0d) * 0.28d
                                        + getDouble(r, "samePositionReplaceRisk", 0.0d) * 0.08d
                                        + getDouble(r, "crossPositionReplaceRisk", 0.0d) * 0.09d);
                return b;
            }
        }

        return b;
    }

    private GuardBundle readThirdGuard(
            int position, int mask, ArrayList<Models.ThirdGuardStat> thirdGuardList) {
        GuardBundle b = new GuardBundle();

        if (thirdGuardList == null) {
            return b;
        }

        for (int i = 0; i < thirdGuardList.size(); i++) {
            Models.ThirdGuardStat g = thirdGuardList.get(i);

            if (g == null) {
                continue;
            }

            int p = getInt(g, "position", 0);
            int m = getInt(g, "k3Mask", getInt(g, "k3_mask", 0));

            if (p == position && m == mask) {
                b.score = getDouble(g, "thirdGuardScore", getDouble(g, "guardScore", 0.0d));
                b.risk = getDouble(g, "thirdGuardRisk", getDouble(g, "guardRisk", 0.0d));
                b.pass = getInt(g, "thirdGuardPass", getInt(g, "pass", 0));
                b.hard = getInt(g, "hardBlock", 0);
                return b;
            }
        }

        return b;
    }

    private RuleBundle readRules(int position, int mask, ArrayList<Models.RuleHealth> ruleList) {
        RuleBundle b = new RuleBundle();

        if (ruleList == null) {
            return b;
        }

        for (int i = 0; i < ruleList.size(); i++) {
            Models.RuleHealth r = ruleList.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", 0);
            int m = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            if (p != position || m != mask) {
                continue;
            }

            b.health += getDouble(r, "ruleHealthScore", getDouble(r, "healthScore", 0.0d));
            b.risk += getDouble(r, "ruleRiskScore", getDouble(r, "riskScore", 0.0d));

            if (getInt(r, "rulePass", getInt(r, "pass", 0)) == 1) {
                b.passCount++;
            }

            if (getInt(r, "hardBlock", 0) == 1) {
                b.hardCount++;
            }

            b.count++;
        }

        if (b.count > 0) {
            b.health = clamp01(b.health / b.count);
            b.risk = clamp01(b.risk / b.count + (b.hardCount > 0 ? 0.12d : 0.0d));
        }

        return b;
    }

    private ShadowBundle readShadow(
            int position, int mask, ArrayList<Models.ShadowHealth> shadowList) {
        ShadowBundle b = new ShadowBundle();

        if (shadowList == null) {
            return b;
        }

        for (int i = 0; i < shadowList.size(); i++) {
            Models.ShadowHealth s = shadowList.get(i);

            if (s == null) {
                continue;
            }

            int p = getInt(s, "position", 0);
            int m = getInt(s, "k3Mask", getInt(s, "k3_mask", 0));

            if (p == position && m == mask) {
                b.health = getDouble(s, "shadowHealthScore", getDouble(s, "healthScore", 0.0d));
                b.risk = getDouble(s, "shadowHealthRisk", getDouble(s, "riskScore", 0.0d));
                b.sample = getInt(s, "shadowSample", 0);
                b.pass = getInt(s, "shadowPass", getInt(s, "pass", 0));
                b.promote = getInt(s, "shadowPromoteEligible", getInt(s, "promoteEligible", 0));
                b.hard = getInt(s, "hardBlock", 0);
                return b;
            }
        }

        return b;
    }

    private FreezeBundle readFreeze(
            int position, int mask, ArrayList<Models.FalseStrongRecord> freezeList) {
        FreezeBundle b = new FreezeBundle();

        if (freezeList == null) {
            return b;
        }

        for (int i = 0; i < freezeList.size(); i++) {
            Models.FalseStrongRecord f = freezeList.get(i);

            if (f == null) {
                continue;
            }

            int p = getInt(f, "position", 0);
            int m = getInt(f, "k3Mask", getInt(f, "k3_mask", 0));

            if (p == position && m == mask) {
                b.risk = getDouble(f, "freezeRisk", getDouble(f, "falseStrongFreezeRisk", 0.0d));
                b.freeze = getInt(f, "freeze", getInt(f, "falseStrongFreeze", 0));
                b.hard = getInt(f, "hardFreeze", 0);
                return b;
            }
        }

        return b;
    }

    private SeedBundle readSeed(
            int position,
            int mask,
            ArrayList<SeedCauseExpansionEngine.SeedExpansionRecord> seedList) {
        SeedBundle b = new SeedBundle();

        if (seedList == null) {
            return b;
        }

        for (int i = 0; i < seedList.size(); i++) {
            SeedCauseExpansionEngine.SeedExpansionRecord s = seedList.get(i);

            if (s == null) {
                continue;
            }

            if (s.position == position && s.k3Mask == mask) {
                b.score = s.seedExpandScore;
                b.risk = s.seedRisk;
                b.similarity = s.seedSimilarity;
                b.pass = s.seedPass;
                b.hard = s.seedHardRisk;
                b.seedKey = s.seedKey;
                return b;
            }
        }

        return b;
    }

    private double calcStructureScore(int n1, int n2, int n3, TeacherBundle t) {
        int odd = 0;
        int high = 0;
        int near = 0;

        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            if (nums[i] % 2 == 1) {
                odd++;
            }

            if (nums[i] >= 6) {
                high++;
            }
        }

        if (Math.abs(n1 - n2) <= 1) {
            near++;
        }

        if (Math.abs(n1 - n3) <= 1) {
            near++;
        }

        if (Math.abs(n2 - n3) <= 1) {
            near++;
        }

        double score = 0.0d;

        score += (odd == 1 || odd == 2) ? 0.28d : 0.12d;
        score += (high == 1 || high == 2) ? 0.26d : 0.12d;
        score += near <= 1 ? 0.22d : 0.08d;

        if (t != null) {
            score += t.score * 0.16d;
            score += (1.0d - t.risk) * 0.08d;
        }

        return clamp01(score);
    }

    private double calcWeakSampleRisk(Models.CandidateK3 c, DynamicParts p) {
        double risk = 0.0d;
        double finalP = getDouble(c, "finalP", 0.0d);

        if (p.sampleConfidence < 0.24d) {
            risk += 0.24d;
        } else if (p.sampleConfidence < 0.40d) {
            risk += 0.12d;
        }

        if (finalP >= 0.39d && p.sampleConfidence < 0.36d) {
            risk += 0.16d;
        }

        if (p.teacherSample < 20 && p.teacherScore >= 0.55d) {
            risk += 0.12d;
        }

        if (p.shadowSample < readWarmupIssues() && p.shadowScore >= 0.55d) {
            risk += 0.10d;
        }

        return clamp01(risk);
    }

    private double calcConflictRisk(DynamicParts p) {
        double risk = 0.0d;

        if (p.probScore >= 0.60d && p.causalScore < 0.40d) {
            risk += 0.16d;
        }

        if (p.teacherScore >= 0.56d && p.teacherRisk >= 0.55d) {
            risk += 0.12d;
        }

        if (p.ruleScore >= 0.56d && p.thirdGuardRisk >= 0.62d) {
            risk += 0.12d;
        }

        if (p.seedScore >= 0.58d && p.freezeRisk >= 0.58d) {
            risk += 0.14d;
        }

        if (p.rankRisk >= 0.60d && p.shadowRisk >= 0.60d) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private boolean isHardRisk(DynamicParts p, double dynamicRisk) {
        if (dynamicRisk >= HARD_RISK_LINE) {
            return true;
        }

        if (p.thirdGuardHard == 1) {
            return true;
        }

        if (p.ruleHardCount > 0 && p.ruleRisk >= 0.70d) {
            return true;
        }

        if (p.shadowHard == 1 && p.shadowRisk >= 0.70d) {
            return true;
        }

        if (p.freezeHard == 1) {
            return true;
        }

        if (p.freezeFlag == 1 && p.freezeRisk >= 0.74d) {
            return true;
        }

        if (p.weakSampleRisk >= 0.72d && p.probScore >= 0.60d) {
            return true;
        }

        return false;
    }

    private double calcSampleConfidence(
            int teacherSample, int shadowSample, int chainSample, int ruleCount) {
        double teacher = confidenceFromSample(teacherSample, 90);
        double shadow = confidenceFromSample(shadowSample, readWarmupIssues());
        double chain = confidenceFromSample(chainSample, 30);
        double rule = confidenceFromSample(ruleCount, 10);

        return clamp01(teacher * 0.30d + shadow * 0.28d + chain * 0.22d + rule * 0.20d);
    }

    private RuntimeStat buildRuntimeStat(List<Models.Trade> trades) {
        RuntimeStat r = new RuntimeStat();

        if (trades == null || trades.size() == 0) {
            r.health = 0.50d;
            r.risk = 0.0d;
            return r;
        }

        int checked = 0;
        int win = 0;
        int miss = 0;
        int loseStreak = 0;

        for (int i = trades.size() - 1; i >= 0; i--) {
            Models.Trade t = trades.get(i);

            if (t == null) {
                continue;
            }

            String action = getString(t, "action", "");
            String result = getString(t, "result", "");

            if (action.length() > 0
                    && !"BET".equals(action)
                    && !"SHADOW".equals(action)
                    && !"HOLD".equals(action)) {
                continue;
            }

            checked++;

            if ("WIN".equals(result) || "HIT".equals(result) || "命中".equals(result)) {
                win++;

                if (loseStreak > 0) {
                    break;
                }
            } else if ("MISS".equals(result) || "LOSE".equals(result) || "未中".equals(result)) {
                miss++;
                loseStreak++;
            }

            if (checked >= 60) {
                break;
            }
        }

        r.checked = checked;
        r.win = win;
        r.miss = miss;
        r.loseStreak = loseStreak;

        double recentRate = checked <= 0 ? 0.0d : ((double) win) / ((double) checked);

        r.health =
                clamp01(
                        recentRate * 0.54d
                                + confidenceFromSample(checked, 30) * 0.20d
                                + (loseStreak <= 1 ? 0.26d : 0.0d));

        if (loseStreak <= 0) {
            r.risk = 0.0d;
        } else if (loseStreak >= 5) {
            r.risk = 0.76d;
        } else {
            r.risk = clamp01(((double) loseStreak) / 7.0d);
        }

        return r;
    }

    private String gradeOf(double score, double risk, double confidence, int hardRisk) {
        if (hardRisk == 1 || risk >= HARD_RISK_LINE) {
            return "C";
        }

        if (score >= GRADE_S_LINE && risk <= 0.44d && confidence >= 0.58d) {
            return "S";
        }

        if (score >= GRADE_A_LINE && risk <= 0.52d && confidence >= 0.46d) {
            return "A";
        }

        if (score >= GRADE_B_LINE && risk <= RISK_LINE) {
            return "B";
        }

        return "C";
    }

    private String normalizeV20Status(String status) {
        if (status == null || status.length() == 0) {
            return V20_STATUS_UNKNOWN;
        }
        String s = status.toUpperCase(Locale.US);
        if (s.indexOf("FREEZE") >= 0 || s.indexOf("HARD_RISK") >= 0) {
            return V20_STATUS_FREEZE;
        }
        if (s.indexOf("RISK") >= 0) {
            return V20_STATUS_RISK;
        }
        if (s.indexOf("STRONG") >= 0 || s.indexOf("HIGH_QUALITY") >= 0) {
            return V20_STATUS_STRONG;
        }
        if (s.indexOf("READY") >= 0 || s.indexOf("PASS") >= 0 || s.indexOf("ALLOW") >= 0) {
            return V20_STATUS_READY;
        }
        if (s.indexOf("WATCH") >= 0) {
            return V20_STATUS_WATCH;
        }
        if (s.indexOf("WEAK") >= 0) {
            return V20_STATUS_WEAK;
        }
        if (s.indexOf("UNKNOWN") >= 0 || s.indexOf("EMPTY") >= 0) {
            return V20_STATUS_UNKNOWN;
        }
        return status;
    }

    private String statusOfV20(
            int pass, int high, int hard, double score, double risk, DynamicParts p) {
        if (hard == 1) {
            return V20_STATUS_FREEZE;
        }
        if (p != null && (p.freezeHard == 1 || p.freezeFlag == 1 && p.freezeRisk >= 0.70d)) {
            return V20_STATUS_FREEZE;
        }
        if (risk >= 0.72d || (p != null && p.counterContribution >= 0.70d)) {
            return V20_STATUS_RISK;
        }
        if (p == null) {
            return V20_STATUS_UNKNOWN;
        }
        boolean noWindow = isNoWindowState(p.windowStateKey);
        boolean weakSample = p.sampleTrust < 0.24d || p.similarWindowSample < 24;
        if (noWindow && p.evidenceContribution < 0.62d) {
            return V20_STATUS_UNKNOWN;
        }
        if (weakSample && p.evidenceContribution < 0.60d) {
            return V20_STATUS_WEAK;
        }
        if (high == 1
                && score >= GRADE_A_LINE
                && risk <= 0.48d
                && p.evidenceContribution >= p.counterContribution + 0.12d
                && p.sampleTrust >= 0.34d
                && p.recentEffectiveness >= 0.30d
                && !noWindow) {
            return V20_STATUS_STRONG;
        }
        if (pass == 1
                && score >= PASS_LINE
                && risk <= RISK_LINE
                && p.evidenceContribution >= p.counterContribution + 0.04d
                && p.sampleTrust >= 0.24d
                && p.recentEffectiveness >= 0.24d) {
            return V20_STATUS_READY;
        }
        if (score >= 0.46d && risk <= 0.66d) {
            return V20_STATUS_WATCH;
        }
        return V20_STATUS_WEAK;
    }

    private boolean isNoWindowState(String key) {
        if (key == null || key.length() == 0) {
            return true;
        }
        String s = key.toUpperCase(Locale.US);
        return s.indexOf("NO_FEATURE") >= 0 || s.indexOf("UNKNOWN") >= 0 || s.indexOf("EMPTY") >= 0;
    }

    private String statusOf(int pass, int high, int hard, double score, double risk) {
        if (hard == 1) {
            return "DYNAMIC_HARD_RISK";
        }

        if (high == 1) {
            return "DYNAMIC_HIGH_QUALITY";
        }

        if (pass == 1) {
            return "DYNAMIC_PASS";
        }

        if (risk >= 0.72d) {
            return "DYNAMIC_RISK";
        }

        if (score >= 0.46d) {
            return "DYNAMIC_WATCH";
        }

        return "DYNAMIC_WEAK";
    }

    private String buildSummary(
            int rankNo,
            int position,
            int n1,
            int n2,
            int n3,
            double score,
            double support,
            double risk,
            double confidence,
            int pass,
            int high,
            int hard,
            String grade,
            String status,
            DynamicParts p) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20动态因链总成｜rank=");
        sb.append(rankNo);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);
        sb.append("｜grade=");
        sb.append(grade);
        sb.append("｜status=");
        sb.append(status);
        sb.append("｜action=");
        sb.append(p.v20Action);
        sb.append("｜score=");
        sb.append(percent(score));
        sb.append("｜support=");
        sb.append(percent(support));
        sb.append("｜risk=");
        sb.append(percent(risk));
        sb.append("｜confidence=");
        sb.append(percent(confidence));
        sb.append("｜pass=");
        sb.append(pass);
        sb.append("｜high=");
        sb.append(high);
        sb.append("｜hard=");
        sb.append(hard);

        sb.append("｜概率=");
        sb.append(percent(p.probScore));
        sb.append("｜扫描=");
        sb.append(percent(p.scanScore));
        sb.append("｜特征=");
        sb.append(percent(p.featureScore));
        sb.append("｜因链=");
        sb.append(percent(p.causalScore));
        sb.append("｜位置=");
        sb.append(percent(p.positionScore));
        sb.append("｜教师=");
        sb.append(percent(p.teacherScore));
        sb.append("｜反事实=");
        sb.append(percent(p.rankScore));
        sb.append("｜第三防线=");
        sb.append(percent(p.thirdGuardScore));
        sb.append("｜规则=");
        sb.append(percent(p.ruleScore));
        sb.append("｜影子=");
        sb.append(percent(p.shadowScore));
        sb.append("｜种子=");
        sb.append(percent(p.seedScore));
        sb.append("｜windowState=");
        sb.append(p.windowStateKey);
        sb.append("｜相似窗样本=");
        sb.append(p.similarWindowSample);
        sb.append("｜相似窗率=");
        sb.append(percent(p.similarWindowHitRate));
        sb.append("｜特征相似=");
        sb.append(percent(1.0d - p.featureDistance));
        sb.append("｜样本可信=");
        sb.append(percent(p.sampleTrust));
        sb.append("｜近期有效=");
        sb.append(percent(p.recentEffectiveness));
        sb.append("｜证据贡献=");
        sb.append(percent(p.evidenceContribution));
        sb.append("｜反证贡献=");
        sb.append(percent(p.counterContribution));
        sb.append("｜建议=");
        sb.append(p.actionAdvice);

        sb.append("｜风险拆解=");
        sb.append("raw:");
        sb.append(percent(p.rawRisk));
        sb.append(",因链:");
        sb.append(percent(p.causalRisk));
        sb.append(",教师:");
        sb.append(percent(p.teacherRisk));
        sb.append(",反事实:");
        sb.append(percent(p.rankRisk));
        sb.append(",防线:");
        sb.append(percent(p.thirdGuardRisk));
        sb.append(",规则:");
        sb.append(percent(p.ruleRisk));
        sb.append(",影子:");
        sb.append(percent(p.shadowRisk));
        sb.append(",冻结:");
        sb.append(percent(p.freezeRisk));
        sb.append(",弱样本:");
        sb.append(percent(p.weakSampleRisk));
        sb.append(",冲突:");
        sb.append(percent(p.conflictRisk));

        sb.append("｜样本=");
        sb.append("teacher:");
        sb.append(p.teacherSample);
        sb.append(",shadow:");
        sb.append(p.shadowSample);
        sb.append(",chain:");
        sb.append(p.chainSample);
        sb.append(",rule:");
        sb.append(p.ruleCount);
        sb.append("｜证据文本=");
        sb.append(p.evidenceText);
        sb.append("｜反证文本=");
        sb.append(p.counterText);

        sb.append("｜说明=动态因链总成只汇总证据和风险，不创建BET，不结算，不写真实校准，最终仍由DecisionGateEngine裁决");

        return sb.toString();
    }

    private String buildDynamicEvidenceText(DynamicParts p) {
        if (p == null) {
            return "证据贡献=空";
        }
        return "证据贡献="
                + percent(p.evidenceContribution)
                + "｜概率="
                + percent(p.probScore)
                + "｜特征="
                + percent(p.featureScore)
                + "｜因果="
                + percent(p.causalScore)
                + "｜教师="
                + percent(p.teacherScore)
                + "｜规则="
                + percent(p.ruleScore)
                + "｜相似窗="
                + percent(p.similarWindowScore)
                + "｜样本可信="
                + percent(p.sampleTrust);
    }

    private String buildDynamicCounterText(DynamicParts p) {
        if (p == null) {
            return "反证贡献=空";
        }
        return "反证贡献="
                + percent(p.counterContribution)
                + "｜raw="
                + percent(p.rawRisk)
                + "｜特征风险="
                + percent(p.featureRisk)
                + "｜因果风险="
                + percent(p.causalRisk)
                + "｜教师风险="
                + percent(p.teacherRisk)
                + "｜冻结="
                + percent(p.freezeRisk)
                + "｜窗口风险="
                + percent(p.windowStateRisk)
                + "｜冲突="
                + percent(p.conflictRisk);
    }

    private String buildDynamicActionAdvice(DynamicParts p) {
        if (p == null) {
            return "WATCH：动态因链为空，只能观察，不直接BET";
        }
        String action = buildV20ExecutableAction(p);
        if (V20_ACTION_FREEZE.equals(action)) {
            return "FREEZE：硬风险/反证强，禁止因链救援";
        }
        if (V20_ACTION_RISK.equals(action)) {
            return "RISK：反证或风险偏高，送最终门时必须谨慎拦截";
        }
        if (V20_ACTION_PASS.equals(action)) {
            return "PASS：证据链完整，可送DecisionGate最终裁决；本引擎不直接BET";
        }
        return "WATCH：证据不足或样本/近期有效性不足，只能观察或SHADOW";
    }

    private String buildV20ExecutableAction(DynamicParts p) {
        if (p == null) {
            return V20_ACTION_WATCH;
        }
        if (p.freezeHard == 1
                || p.freezeFlag == 1
                || p.thirdGuardHard == 1
                || p.ruleHardCount > 0
                || p.counterContribution >= 0.72d
                || p.freezeRisk >= 0.72d) {
            return V20_ACTION_FREEZE;
        }
        if (p.counterContribution >= 0.66d
                || p.dynamicLikeRisk() >= 0.72d
                || p.thirdDragScore >= 0.72d
                || p.coverageQuality < 0.18d) {
            return V20_ACTION_RISK;
        }
        if (p.evidenceContribution >= 0.56d
                && p.evidenceContribution >= p.counterContribution + 0.04d
                && p.sampleTrust >= 0.22d
                && p.recentEffectiveness >= 0.20d
                && p.positionNeedScore >= 0.18d
                && p.numberSupplyScore >= 0.18d
                && p.coverageQuality >= 0.20d
                && p.thirdQuality >= 0.08d) {
            return V20_ACTION_PASS;
        }
        return V20_ACTION_WATCH;
    }

    private String buildWindowStateKey(int position, int n1, int n2, int n3, FeatureBundle b) {
        if (b == null) {
            return "DYN_WS|P" + position + "|EMPTY";
        }
        String key =
                "DYN_WS|P"
                        + position
                        + "|K"
                        + n1
                        + "_"
                        + n2
                        + "_"
                        + n3
                        + "|"
                        + b.hotColdBucket
                        + "|"
                        + b.omissionBucket
                        + "|"
                        + b.densityBucket
                        + "|"
                        + b.repeatBucket
                        + "|"
                        + buildOddEvenBias(n1, n2, n3)
                        + "|"
                        + buildHighLowBias(n1, n2, n3)
                        + "|"
                        + b.neighborTailState
                        + "|"
                        + b.positionCrowdState
                        + "|"
                        + b.crossPositionSync
                        + "|"
                        + b.crossPositionSqueeze
                        + "|L"
                        + b.recentLoseStreak
                        + "|"
                        + b.timingShift;
        if (key.length() > 220) {
            key = key.substring(0, 180) + "|" + Math.abs(key.hashCode());
        }
        return key;
    }

    private String buildOddEvenBias(int n1, int n2, int n3) {
        int even = 0;
        if (n1 % 2 == 0) even++;
        if (n2 % 2 == 0) even++;
        if (n3 % 2 == 0) even++;
        if (even >= 3) return "OE_ALL_EVEN";
        if (even == 2) return "OE_EVEN_LEAN";
        if (even == 1) return "OE_ODD_LEAN";
        return "OE_ALL_ODD";
    }

    private String buildHighLowBias(int n1, int n2, int n3) {
        int high = 0;
        if (n1 >= 6) high++;
        if (n2 >= 6) high++;
        if (n3 >= 6) high++;
        if (high >= 3) return "HL_ALL_HIGH";
        if (high == 2) return "HL_HIGH_LEAN";
        if (high == 1) return "HL_LOW_LEAN";
        return "HL_ALL_LOW";
    }

    private String buildOmissionBucket(int gap, double score) {
        if (gap >= 24 || score >= 0.72d) {
            return "MISS_LONG";
        }
        if (gap >= 12 || score >= 0.52d) {
            return "MISS_MID";
        }
        if (gap >= 6 || score >= 0.28d) {
            return "MISS_SHORT";
        }
        return "MISS_NONE";
    }

    private double calcFeatureDistance(FeatureBundle b) {
        if (b == null) {
            return 1.0d;
        }
        double d = 0.0d;
        d += Math.abs(0.50d - b.score) * 0.18d;
        d += b.risk * 0.18d;
        d += Math.abs(0.50d - b.trend) * 0.10d;
        d += Math.abs(0.50d - b.gap) * 0.10d;
        d += Math.abs(0.50d - b.transition) * 0.10d;
        d += (1.0d - b.density) * 0.10d;
        d += b.repeat * 0.06d;
        d += b.squeeze * 0.08d;
        d += b.crowd * 0.06d;
        d += b.recentLoseStreak >= 3 ? 0.04d : 0.0d;
        return clamp01(d);
    }

    private double sampleTrust(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }
        if (sample >= 240) {
            return 1.0d;
        }
        return clamp01(((double) sample) / 240.0d);
    }

    private String buildSameClassKey(int position, FeatureBundle f, DynamicParts p) {
        return "DYN_CLASS|P"
                + position
                + "|F"
                + bucket01(f.score)
                + "|C"
                + bucket01(p.causalScore)
                + "|T"
                + bucket01(p.teacherScore)
                + "|R"
                + bucket01(p.dynamicLikeRisk());
    }

    private String buildDynamicKey(String targetIssue, int position, int mask) {
        return safe(targetIssue) + "|DYN|P" + position + "|K3_" + mask;
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private ArrayList<Models.CandidateK3> normalizeCandidates(List<Models.CandidateK3> candidates) {
        ArrayList<Models.CandidateK3> out = new ArrayList<Models.CandidateK3>();

        if (candidates == null) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n1 = getInt(c, "n1", 0);
            int n2 = getInt(c, "n2", 0);
            int n3 = getInt(c, "n3", 0);

            if (!isValidPosition(p)
                    || !isValidNumber(n1)
                    || !isValidNumber(n2)
                    || !isValidNumber(n3)) {
                continue;
            }

            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            if (mask <= 0) {
                mask = buildK3Mask(n1, n2, n3);
                putInt(c, "k3Mask", mask);
                putInt(c, "k3_mask", mask);
            }

            out.add(c);
        }

        Collections.sort(
                out,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = candidateScore(a);
                        double sb = candidateScore(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);

                        if (pa != pb) {
                            return pa - pb;
                        }

                        int ma = getInt(a, "k3Mask", getInt(a, "k3_mask", 0));
                        int mb = getInt(b, "k3Mask", getInt(b, "k3_mask", 0));

                        return ma - mb;
                    }
                });

        for (int i = 0; i < out.size(); i++) {
            Models.CandidateK3 c = out.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }

        return out;
    }

    private DynamicCauseRecord findBestDynamicForNumber(
            ArrayList<DynamicCauseRecord> list, int position, int number) {
        DynamicCauseRecord best = null;
        double bestScore = -1.0d;

        if (list == null) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            DynamicCauseRecord d = list.get(i);

            if (d == null) {
                continue;
            }

            int p = getInt(d, "position", 0);

            if (p != position) {
                continue;
            }

            int n1 = getInt(d, "n1", 0);
            int n2 = getInt(d, "n2", 0);
            int n3 = getInt(d, "n3", 0);

            if (number != n1 && number != n2 && number != n3) {
                continue;
            }

            double score = getDouble(d, "dynamicCauseScore", getDouble(d, "causeScore", 0.0d));

            if (best == null || score > bestScore) {
                best = d;
                bestScore = score;
            }
        }

        return best;
    }

    private int buildK3Mask(int n1, int n2, int n3) {
        int mask = 0;

        if (isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private static boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private static int readWarmupIssues() {
        int v = readAppInt("LIVE_SHADOW_WARMUP_ISSUES", 30);

        if (v <= 0) {
            v = 30;
        }

        return v;
    }

    private static int readAppInt(String field, int fallback) {
        try {
            Field f = AppConfig.class.getField(field);
            Object v = f.get(null);

            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double confidenceFromSample(int sample, int full) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (full <= 0) {
            full = 30;
        }

        if (sample >= full) {
            return 1.0d;
        }

        return clamp01(((double) sample) / ((double) full));
    }

    private static double posPart(double v) {
        if (v <= 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static double clampSigned(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < -1.0d) {
            return -1.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
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

    private static String bucket01(double v) {
        v = clamp01(v);

        if (v >= 0.75d) {
            return "HIGH";
        }

        if (v >= 0.55d) {
            return "MID_HIGH";
        }

        if (v >= 0.35d) {
            return "MID";
        }

        if (v > 0.0d) {
            return "LOW";
        }

        return "ZERO";
    }

    private static String bucketSigned(double v) {
        if (v >= 0.18d) {
            return "UP_STRONG";
        }

        if (v >= 0.06d) {
            return "UP";
        }

        if (v <= -0.18d) {
            return "DOWN_STRONG";
        }

        if (v <= -0.06d) {
            return "DOWN";
        }

        return "FLAT";
    }

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }
        return b == null ? "" : b;
    }

    private static String firstNonEmpty(String a, String b, String c) {
        String v = firstNonEmpty(a, b);
        if (v.length() > 0) {
            return v;
        }
        return c == null ? "" : c;
    }

    private static String firstNonEmpty(String a, String b, String c, String d) {
        String v = firstNonEmpty(a, b, c);
        if (v.length() > 0) {
            return v;
        }
        return d == null ? "" : d;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void appendReason(Models.CandidateK3 c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }

        String old = getString(c, "reasonSummary", "");

        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", add);
        } else {
            putString(c, "reasonSummary", old + "｜" + add);
        }
    }

    private static int getInt(Object obj, String field, int fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double getDouble(Object obj, String field, double fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static String getString(Object obj, String field, String fallback) {
        if (obj == null || field == null || field.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static void putInt(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    public static final class DynamicCauseRecord {
        public String chainKey = "";
        public String dynamicKey = "";
        public String recordKey = "";
        public String baseIssue = "";
        public String targetIssue = "";
        public long createdAt = 0L;

        public int rankNo = 0;
        public int rank = 0;
        public int position = 0;
        public int n1 = 0;
        public int n2 = 0;
        public int n3 = 0;
        public int k3Mask = 0;
        public int k3_mask = 0;

        public double dynamicCauseScore = 0.0d;
        public double causeScore = 0.0d;
        public double dynamicSupportScore = 0.0d;
        public double supportScore = 0.0d;
        public double dynamicRiskScore = 0.0d;
        public double riskScore = 0.0d;
        public double dynamicConfidence = 0.0d;
        public double confidence = 0.0d;

        public int dynamicPass = 0;
        public int pass = 0;
        public int dynamicHighQuality = 0;
        public int highQuality = 0;
        public int dynamicHardRisk = 0;
        public int hardRisk = 0;

        public String dynamicGrade = "";
        public String grade = "";
        public String status = "";
        public String level = "";

        public String v20Status = "";
        public String v20CauseStatus = "";

        public double v20SupportScore = 0.0d;
        public double v20RiskScore = 0.0d;
        public double v20Confidence = 0.0d;
        public double v20EvidenceContribution = 0.0d;
        public double v20CounterContribution = 0.0d;
        public double v20SampleReliability = 0.0d;
        public double v20RecentValidity = 0.0d;
        public int v20EmptyFlag = 0;
        public String v20ActionAdvice = "";
        public String v20WindowStateKey = "";
        public double positionCauseScore = 0.0d;
        public double numberCauseScore = 0.0d;
        public double counterfactualScore = 0.0d;
        public double ruleHealthScore = 0.0d;
        public double shadowHealthScore = 0.0d;
        public double falseStrongRisk = 0.0d;
        public double seedExpansionScore = 0.0d;
        public double sampleReliability = 0.0d;
        public double recentValidity = 0.0d;
        public String v20DiagnosisSummary = "";
        public String v20Action = "";
        public String dynamicAction = "";
        public double evidenceTotal = 0.0d;
        public double antiEvidenceTotal = 0.0d;
        public double antiEvidenceScore = 0.0d;
        public double counterEvidenceScore = 0.0d;
        public double netEvidenceScore = 0.0d;
        public double positionNeedScore = 0.0d;
        public double numberSupplyScore = 0.0d;
        public double coverageQuality = 0.0d;
        public double thirdQuality = 0.0d;
        public double thirdDragScore = 0.0d;
        public double counterfactualRisk = 0.0d;
        public double similarWindowConfidence = 0.0d;
        public String v20RejectCode = "";
        public String fieldChainVersion = "";

        public double probScore = 0.0d;
        public double scanScore = 0.0d;
        public double featureScore = 0.0d;
        public double causalScore = 0.0d;
        public double positionScore = 0.0d;
        public double teacherScore = 0.0d;
        public double rankScore = 0.0d;
        public double thirdGuardScore = 0.0d;
        public double ruleScore = 0.0d;
        public double shadowScore = 0.0d;
        public double seedScore = 0.0d;
        public double structureScore = 0.0d;

        public double rawRisk = 0.0d;
        public double featureRisk = 0.0d;
        public double causalRisk = 0.0d;
        public double positionRisk = 0.0d;
        public double teacherRisk = 0.0d;
        public double rankRisk = 0.0d;
        public double thirdGuardRisk = 0.0d;
        public double ruleRisk = 0.0d;
        public double shadowRisk = 0.0d;
        public double freezeRisk = 0.0d;
        public double seedRisk = 0.0d;
        public double weakSampleRisk = 0.0d;
        public double runtimeRisk = 0.0d;
        public double conflictRisk = 0.0d;

        public double sampleConfidence = 0.0d;
        public int teacherSample = 0;
        public int shadowSample = 0;
        public int chainSample = 0;
        public int ruleCount = 0;

        public String stateKey = "";
        public String shapeKey = "";
        public String sameClassKey = "";
        public String seedKey = "";
        public String windowStateKey = "";
        public String hotColdBucket = "";
        public String omissionBucket = "";
        public String densityBucket = "";
        public String repeatBucket = "";
        public String oddEvenBias = "";
        public String highLowBias = "";
        public String neighborTailState = "";
        public String positionCrowdState = "";
        public String crossPositionSync = "";
        public String crossPositionSqueeze = "";
        public int recentLoseStreak = 0;
        public String timingShift = "";
        public double featureDistance = 1.0d;
        public int similarWindowSample = 0;
        public double similarWindowHitRate = 0.0d;
        public double similarWindowRecentRate = 0.0d;
        public double sampleTrust = 0.0d;
        public double recentEffectiveness = 0.0d;
        public double evidenceContribution = 0.0d;
        public double counterContribution = 0.0d;
        public String evidenceText = "";
        public String counterText = "";
        public String actionAdvice = "";
        public String calculationVersion = "";

        public String summaryText = "";
        public String reasonText = "";

        public ContentValues toContentValues() {
            ContentValues cv = new ContentValues();
            cv.put("record_key", recordKey);
            cv.put("base_issue", baseIssue);
            cv.put("target_issue", targetIssue);
            cv.put("created_at", createdAt);
            cv.put("rank_no", rankNo);
            cv.put("position", position);
            cv.put("n1", n1);
            cv.put("n2", n2);
            cv.put("n3", n3);
            cv.put("k3_mask", k3Mask);
            cv.put("state_key", stateKey);
            cv.put("shape_state", shapeKey);
            cv.put("same_class_key", sameClassKey);
            cv.put("seed_key", seedKey);
            cv.put("window_state_key", windowStateKey);
            cv.put("hot_cold_bucket", hotColdBucket);
            cv.put("omission_bucket", omissionBucket);
            cv.put("density_bucket", densityBucket);
            cv.put("repeat_bucket", repeatBucket);
            cv.put("odd_even_bias", oddEvenBias);
            cv.put("high_low_bias", highLowBias);
            cv.put("neighbor_tail_state", neighborTailState);
            cv.put("position_crowd_state", positionCrowdState);
            cv.put("cross_position_sync", crossPositionSync);
            cv.put("cross_position_squeeze", crossPositionSqueeze);
            cv.put("recent_lose_streak", recentLoseStreak);
            cv.put("timing_shift", timingShift);
            cv.put("feature_distance", featureDistance);
            cv.put("similar_window_sample", similarWindowSample);
            cv.put("similar_window_hit_rate", similarWindowHitRate);
            cv.put("similar_window_recent_rate", similarWindowRecentRate);
            cv.put("sample_trust", sampleTrust);
            cv.put("recent_effectiveness", recentEffectiveness);
            cv.put("evidence_contribution", evidenceContribution);
            cv.put("counter_contribution", counterContribution);
            cv.put("evidence_support_text", evidenceText);
            cv.put("counter_evidence_text", counterText);
            cv.put("action_advice", actionAdvice);
            cv.put("calculation_version", calculationVersion);
            cv.put("final_cause_score", dynamicCauseScore);
            cv.put("cause_score", causeScore);
            cv.put("support_score", dynamicSupportScore);
            cv.put("risk_score", dynamicRiskScore);
            cv.put("confidence", dynamicConfidence);
            cv.put("pass_flag", dynamicPass);
            cv.put("grade", dynamicGrade);
            cv.put("status", status);
            cv.put("v20_status", v20Status);
            cv.put("v20_support_score", v20SupportScore);
            cv.put("v20_risk_score", v20RiskScore);
            cv.put("v20_confidence", v20Confidence);
            cv.put("v20_evidence_contribution", v20EvidenceContribution);
            cv.put("v20_counter_contribution", v20CounterContribution);
            cv.put("v20_sample_reliability", v20SampleReliability);
            cv.put("v20_recent_validity", v20RecentValidity);
            cv.put("v20_action", v20Action);
            cv.put("dynamic_action", dynamicAction);
            cv.put("evidence_total", evidenceTotal);
            cv.put("anti_evidence_total", antiEvidenceTotal);
            cv.put("counter_evidence_score", counterEvidenceScore);
            cv.put("net_evidence_score", netEvidenceScore);
            cv.put("position_need_score", positionNeedScore);
            cv.put("number_supply_score", numberSupplyScore);
            cv.put("coverage_quality", coverageQuality);
            cv.put("third_quality", thirdQuality);
            cv.put("third_drag_score", thirdDragScore);
            cv.put("counterfactual_risk", counterfactualRisk);
            cv.put("similar_window_confidence", similarWindowConfidence);
            cv.put("v20_reject_code", v20RejectCode);
            cv.put("field_chain_version", fieldChainVersion);

            cv.put("v20_action_advice", v20ActionAdvice);
            cv.put("v20_window_state_key", v20WindowStateKey);
            cv.put("v20_empty_flag", v20EmptyFlag);
            cv.put("position_cause_score", positionCauseScore);
            cv.put("number_cause_score", numberCauseScore);
            cv.put("counterfactual_score", counterfactualScore);
            cv.put("third_guard_score", thirdGuardScore);
            cv.put("rule_health_score", ruleHealthScore);
            cv.put("shadow_health_score", shadowHealthScore);
            cv.put("false_strong_risk", falseStrongRisk);
            cv.put("seed_expansion_score", seedExpansionScore);
            cv.put("sample_reliability", sampleReliability);
            cv.put("recent_validity", recentValidity);
            cv.put("v20_diagnosis_summary", v20DiagnosisSummary);
            cv.put("summary_text", summaryText);
            cv.put("reason_text", reasonText);
            return cv;
        }
    }

    private static final class DynamicParts {
        double finalP;
        double dynamicGate;
        double edge;

        double probScore;
        double scanScore;
        double featureScore;
        double featureRisk;
        double causalScore;
        double causalRisk;
        double positionScore;
        double positionRisk;
        double teacherScore;
        double teacherRisk;
        double teacherConfidence;
        int teacherSample;
        double rankScore;
        double rankRisk;
        double thirdGuardScore;
        double thirdGuardRisk;
        int thirdGuardPass;
        int thirdGuardHard;
        double ruleScore;
        double ruleRisk;
        int ruleCount;
        int rulePassCount;
        int ruleHardCount;
        double shadowScore;
        double shadowRisk;
        int shadowSample;
        int shadowPass;
        int shadowPromote;
        int shadowHard;
        double freezeRisk;
        int freezeFlag;
        int freezeHard;
        double seedScore;
        double seedRisk;
        double seedSimilarity;
        int seedPass;
        int seedHard;

        double rawRisk;
        double weakSampleRisk;
        double conflictRisk;
        double runtimeHealth;
        double runtimeRisk;
        double sampleConfidence;
        double confidence;
        double structureScore;
        double similarWindowScore;
        double windowStateRisk;
        double featureDistance;
        int similarWindowSample;
        double similarWindowHitRate;
        double similarWindowRecentRate;
        double sampleTrust;
        double recentEffectiveness;
        double evidenceContribution;
        double counterContribution;
        String evidenceText = "";
        String counterText = "";
        String actionAdvice = "";
        String v20Action = "";
        double positionNeedScore;
        double numberSupplyScore;
        double coverageQuality;
        double thirdQuality;
        double thirdDragScore;
        double counterfactualRisk;
        double similarWindowConfidence;

        int chainSample;
        int recentLoseStreak;

        String stateKey = "";
        String shapeKey = "";
        String sameClassKey = "";
        String seedKey = "";
        String windowStateKey = "";
        String hotColdBucket = "";
        String omissionBucket = "";
        String densityBucket = "";
        String repeatBucket = "";
        String oddEvenBias = "";
        String highLowBias = "";
        String neighborTailState = "";
        String positionCrowdState = "";
        String crossPositionSync = "";
        String crossPositionSqueeze = "";
        String timingShift = "";

        double dynamicLikeRisk() {
            return clamp01(
                    rawRisk * 0.20d
                            + causalRisk * 0.14d
                            + teacherRisk * 0.12d
                            + rankRisk * 0.12d
                            + thirdGuardRisk * 0.12d
                            + ruleRisk * 0.12d
                            + shadowRisk * 0.10d
                            + freezeRisk * 0.08d);
        }
    }

    private static final class FeatureBundle {
        double score;
        double risk;
        double trend;
        double gap;
        double transition;
        double recovery;
        double density;
        double repeat;
        double neighborTail;
        double crowd;
        double sync;
        double squeeze;
        double timing;
        double featureDistance;
        int maxMissingGap;
        int recentLoseStreak;
        String hotColdBucket = "";
        String omissionBucket = "";
        String densityBucket = "";
        String repeatBucket = "";
        String neighborTailState = "";
        String positionCrowdState = "";
        String crossPositionSync = "";
        String crossPositionSqueeze = "";
        String timingShift = "";
        String windowStateKey = "";
        String shapeKey = "";
        String stateKey = "";
    }

    private static final class ChainBundle {
        int sample;
        double score;
        double risk;
    }

    private static final class PositionBundle {
        double health;
        double risk;
        double driftRisk;
    }

    private static final class TeacherBundle {
        int used;
        int sample;
        double score;
        double risk;
        double confidence;
    }

    private static final class RankBundle {
        double score;
        double risk;
    }

    private static final class GuardBundle {
        double score;
        double risk;
        int pass;
        int hard;
    }

    private static final class RuleBundle {
        int count;
        int passCount;
        int hardCount;
        double health;
        double risk;
    }

    private static final class ShadowBundle {
        int sample;
        int pass;
        int promote;
        int hard;
        double health;
        double risk;
    }

    private static final class FreezeBundle {
        int freeze;
        int hard;
        double risk;
    }

    private static final class SeedBundle {
        int pass;
        int hard;
        double score;
        double risk;
        double similarity;
        String seedKey = "";
    }

    private static final class RuntimeStat {
        int checked;
        int win;
        int miss;
        int loseStreak;
        double health;
        double risk;
    }

    private void ensurePredrawStateCompatibilityV6(Object c) {
        if (c == null) return;
        String window = getString(c, "windowStateKey", getString(c, "stateKey", ""));
        if (window.length() == 0) return;
        putString(c, "windowStateKey", window);
        putString(c, "stateKey", window);
        String learning = getString(c, "learningClusterKey", "");
        if (learning.length() == 0 || learning.length() > 180 || learning.indexOf("|K3_") >= 0) {
            putString(
                    c,
                    "learningClusterKey",
                    "LCL|P"
                            + getInt(c, "position", 0)
                            + "|STATE="
                            + coarseToken(window)
                            + "|NS="
                            + coarseBand(getDouble(c, "needSupplyMatchScore", 0.0d))
                            + "|TQ="
                            + coarseBand(getDouble(c, "thirdNumberQualityScore", 0.0d))
                            + "|CR="
                            + coarseBand(1.0d - getDouble(c, "counterfactualWhyNotRisk", 0.0d)));
        }
        putString(c, "auditClusterKey", getString(c, "auditClusterKey", window));
        putString(c, "dynamicLearningClusterVersion", AppConfig.DYNAMIC_LEARNING_CLUSTER_VERSION);
        String status = getString(c, "v20Status", "");
        if (status.length() == 0 || status.indexOf("EMPTY") >= 0)
            putString(c, "v20Status", "V20_WATCH_PREDRAW_READY");
        putString(c, "dynamicCauseStateCompatibility", "PREDRAW_WINDOW_KEY_ACCEPTED");
    }

    private static String coarseToken(String value) {
        String s = (value == null ? "" : value.trim()).toUpperCase(java.util.Locale.US);
        if (s.length() == 0) return "UNKNOWN";
        String trend =
                tokenClass(s, new String[] {"HC_UP", "HC_DOWN", "HC_FLAT", "HC_BAL"}, "HC_OTHER");
        String miss =
                tokenClass(
                        s,
                        new String[] {"MISS_0_2", "MISS_3_5", "MISS_MID", "RECENT_HIT"},
                        "MISS_OTHER");
        String density =
                tokenClass(
                        s,
                        new String[] {"DEN_LIGHT", "DEN_D_LOW", "DEN_D_MID", "DEN_D_HIGH"},
                        "DEN_OTHER");
        String time =
                tokenClass(
                        s,
                        new String[] {"TIME_T_READY", "TIME_T_MID", "TIME_T_LOW", "TIME_T_TINY"},
                        "TIME_OTHER");
        String crowd =
                tokenClass(
                        s, new String[] {"CROWD_HIGH", "CROWD_MID", "CROWD_C_TINY"}, "CROWD_OTHER");
        return trend + "|" + miss + "|" + density + "|" + time + "|" + crowd;
    }

    private static String tokenClass(String source, String[] keys, String fallback) {
        if (source != null && keys != null) {
            for (int i = 0; i < keys.length; i++) if (source.indexOf(keys[i]) >= 0) return keys[i];
        }
        return fallback;
    }

    private static String coarseBand(double v) {
        if (v >= 0.70d) return "HIGH";
        if (v >= 0.50d) return "MID";
        if (v > 0.0d) return "LOW";
        return "NA";
    }

    public ArrayList<Models.ComboCandidate> applyForwardDomainEvidence(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        if (candidates == null) return new ArrayList<Models.ComboCandidate>();
        try {
            if (store != null)
                store.registerModuleBuildV20(
                        "DynamicCauseChainEngine",
                        MODULE_BUILD_VERSION,
                        "applyForwardDomainEvidence");
        } catch (Throwable ignore) {
        }
        java.util.HashMap<String, Models.ShadowDomainStat> shadowMap =
                new java.util.HashMap<String, Models.ShadowDomainStat>();
        try {
            if (store != null) {
                List<Models.ShadowDomainStat> xs = store.getShadowPromotionDomainStats("", 500);
                if (xs != null)
                    for (int i = 0; i < xs.size(); i++) {
                        Models.ShadowDomainStat x = xs.get(i);
                        if (x != null && x.domainKey != null) shadowMap.put(x.domainKey, x);
                    }
            }
        } catch (Throwable ignore) {
        }
        int matched = 0, fineMatched = 0, midMatched = 0, coarseMatched = 0, ready = 0;
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) continue;
            if (getInt(c, "evidenceDomainFrozen", 0) != 1) {
                putInt(c, "dynamicRealSample", 0);
                putInt(c, "dynamicShadowSample", 0);
                putString(c, "dynamicMatchedLevel", "NONE");
                putString(c, "dynamicDomainMatchLevel", "NONE");
                putString(c, "dynamicClusterStatus", "WATCH");
                putString(
                        c,
                        "dynamicClusterReason",
                        "DYNAMIC_DOMAIN_NOT_FROZEN_AT_INPUT：只WATCH，禁止Dynamic阶段重算域键");
                putString(c, "dynamicApplyError", "DYNAMIC_FIELD_LOST_BEFORE_DYNAMIC");
                continue;
            }
            String[] keys =
                    new String[] {
                        getString(c, "promotionFineDomain", ""),
                        getString(c, "promotionMidDomain", ""),
                        getString(c, "promotionCoarseDomain", "")
                    };
            String[] levels = new String[] {"FINE", "MID", "COARSE"};
            Models.DomainEvidenceStat real = null;
            Models.ShadowDomainStat shadow = null;
            String level = "NONE";
            String key = "";
            for (int k = 0; k < keys.length; k++) {
                String z = keys[k];
                if (z == null || z.length() == 0) continue;
                Models.DomainEvidenceStat q =
                        store == null
                                ? null
                                : store.loadDomainEvidenceStat(
                                        DbHelper.TABLE_DYNAMIC_DOMAIN_STAT_V20, z);
                Models.ShadowDomainStat sh = shadowMap.get(z);
                if ((q != null && q.sampleCount > 0) || (sh != null && sh.sampleCount > 0)) {
                    real = q;
                    shadow = sh;
                    level = levels[k];
                    key = z;
                    break;
                }
            }
            int realN = real == null ? 0 : real.sampleCount,
                    shadowN = shadow == null ? 0 : shadow.sampleCount;
            putInt(c, "dynamicRealSample", realN);
            putInt(c, "dynamicShadowSample", shadowN);
            putString(c, "dynamicMatchedLevel", level);
            putString(c, "dynamicDomainMatchLevel", level);
            if ("NONE".equals(level)) {
                putString(c, "dynamicClusterStatus", "WATCH");
                putString(c, "dynamicClusterReason", "固定域无已结算样本，只WATCH，不制造HARD_RISK");
                continue;
            }
            matched++;
            if ("FINE".equals(level)) fineMatched++;
            else if ("MID".equals(level)) midMatched++;
            else coarseMatched++;
            int sample = realN > 0 ? realN : shadowN;
            int hit = realN > 0 ? real.hitCount : shadow.hitCount;
            int miss = realN > 0 ? real.missCount : shadow.missCount;
            double post = realN > 0 ? real.posteriorMean : shadow.posteriorMean;
            double lower = realN > 0 ? real.lowerBound : shadow.wilsonLowerBound;
            double upper = realN > 0 ? real.upperBound : 0.0d;
            String sourceStatus = realN > 0 ? real.status : shadow.promotionStatus;
            String tier = AppConfig.dynamicRiskTier(sourceStatus, realN, post, upper);
            String status = "WATCH";
            if ("HARD_BLOCK".equals(tier)) status = "HARD_RISK";
            else if ("SOFT_RISK".equals(tier)) status = "SOFT_RISK";
            else if (sample >= 30 && post >= 0.35d) status = "READY";
            if ("READY".equals(status)) ready++;
            double confidence = clamp01(((double) sample) / 60.0d);
            double support = clamp01((post - 0.30d) * 2.4d + lower * 0.45d + confidence * 0.18d);
            double counter =
                    clamp01(
                            Math.max(0.0d, 0.30d - post) * 2.6d
                                    + (real == null ? 0.0d : real.driftScore) * 0.35d);
            putInt(c, "dynamicClusterSample", sample);
            putInt(c, "dynamicClusterHitCount", hit);
            putInt(c, "dynamicClusterMissCount", miss);
            putDouble(c, "dynamicClusterHitRate", post);
            putDouble(c, "dynamicClusterConfidence", confidence);
            putDouble(c, "dynamicClusterCounterScore", counter);
            putDouble(c, "dynamicWilsonUpperBound", upper);
            putDouble(
                    c,
                    "v20EvidenceContribution",
                    Math.max(getDouble(c, "v20EvidenceContribution", 0.0d), support));
            putDouble(
                    c,
                    "v20CounterContribution",
                    Math.max(getDouble(c, "v20CounterContribution", 0.0d), counter));
            putDouble(
                    c,
                    "v20SampleReliability",
                    Math.max(getDouble(c, "v20SampleReliability", 0.0d), confidence));
            putString(c, "dynamicClusterStatus", status);
            putString(
                    c,
                    "dynamicClusterReason",
                    "冻结域证据：match="
                            + level
                            + " key="
                            + key
                            + " real="
                            + realN
                            + " shadow="
                            + shadowN
                            + " posterior="
                            + post
                            + " lower="
                            + lower
                            + " upper="
                            + upper
                            + " tier="
                            + tier);
            putString(c, "dynamicDomainVersion", AppConfig.ACTIVE_EVIDENCE_DOMAIN_SCHEMA);
            putString(c, "evidenceSource", "DynamicCauseChainEngine.FROZEN_SETTLED_DOMAIN");
            putInt(c, "evidenceSettledOnly", 1);
            putInt(c, "evidenceSampleCount", sample);
            putInt(c, "evidenceHitCount", hit);
            putInt(c, "evidenceMissCount", miss);
            putString(c, "moduleBuildVersion", MODULE_BUILD_VERSION);
        }
        try {
            if (store != null) {
                store.setState("dynamic_domain_matched_candidates", String.valueOf(matched));
                store.setState(
                        "dynamic_domain_unmatched_candidates",
                        String.valueOf(Math.max(0, candidates.size() - matched)));
                store.setState("dynamic_domain_fine_match_count", String.valueOf(fineMatched));
                store.setState("dynamic_domain_mid_match_count", String.valueOf(midMatched));
                store.setState("dynamic_domain_coarse_match_count", String.valueOf(coarseMatched));
                store.setState("dynamic_domain_ready_count", String.valueOf(ready));
                store.setState("dynamic_domain_version", MODULE_BUILD_VERSION);
                store.setState(
                        "dynamic_domain_key_policy",
                        "FINAL_P冻结后生成一次；FINE>MID>COARSE；REAL与SHADOW分开");
            }
        } catch (Throwable ignore) {
        }
        return candidates;
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:DynamicCauseChainEngine";

    public static final class V57CauseChainEvidence {
        public final java.util.ArrayList<String> supporting = new java.util.ArrayList<String>();
        public final java.util.ArrayList<String> opposing = new java.util.ArrayList<String>();
        public double supportScore;
        public double counterScore;
        public double confidence;
        public long cutoffTime;
        public long newestSourceTime;
        public String hash = "";

        public boolean pitValid() {
            return newestSourceTime <= cutoffTime;
        }

        public String canonical() {
            return supporting.toString()
                    + '|'
                    + opposing.toString()
                    + '|'
                    + supportScore
                    + '|'
                    + counterScore
                    + '|'
                    + confidence
                    + '|'
                    + cutoffTime
                    + '|'
                    + newestSourceTime;
        }
    }

    public static V57CauseChainEvidence v57b08CauseChain(
            java.util.List<String> support,
            double[] supportWeight,
            java.util.List<String> counter,
            double[] counterWeight,
            long newest,
            long cutoff) {
        V57CauseChainEvidence e = new V57CauseChainEvidence();
        if (support != null) e.supporting.addAll(support);
        if (counter != null) e.opposing.addAll(counter);
        e.supportScore = v57b08Weighted(supportWeight);
        e.counterScore = v57b08Weighted(counterWeight);
        e.newestSourceTime = newest;
        e.cutoffTime = cutoff;
        double balance = Math.max(0.0d, 1.0d - Math.abs(e.supportScore - e.counterScore) * 0.5d);
        e.confidence =
                e.pitValid()
                        ? Math.max(
                                0.0d,
                                Math.min(
                                        1.0d, (Math.max(e.supportScore, e.counterScore)) * balance))
                        : 0.0d;
        e.hash = v57b08Hash(e.canonical());
        return e;
    }

    public static boolean v57b08BetAuthority() {
        return false;
    }

    public static boolean v57b08LaneAuthority() {
        return false;
    }

    private static double v57b08Weighted(double[] a) {
        if (a == null || a.length == 0) return 0.0d;
        double s = 0.0d;
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            if (!Double.isNaN(x) && !Double.isInfinite(x)) s += Math.max(0.0d, x);
        }
        return Math.max(0.0d, Math.min(1.0d, s / Math.max(1, a.length)));
    }

    private static String v57b08Hash(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "HASH_ERROR";
        }
    }
}
