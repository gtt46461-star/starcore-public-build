package com.ethk3.novaonepro;

import android.content.ContentValues;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * ShadowPromotionVerifierEngine
 *
 * <p>V20 重型计算升级主链闭环修复 · 第15/22
 *
 * <p>新增核心引擎：SHADOW反事实晋级验证引擎。
 *
 * <p>作用： 1. 读取近期 SHADOW 决策； 2. 读取对应 targetIssue 开奖； 3. 计算“如果当时BET，会不会命中”； 4.
 * 按位置、FinalP、Edge、Teacher、V20、WindowState、样本可信、近期有效性、组合域分桶； 5. 找出真正值得晋级观察的 SHADOW 强域； 6. 输出
 * promotedDomains / watchDomains / blockedDomains； 7. 给后续
 * DynamicFreezeMatrixEngine、AdaptiveLearningJudgeEngine、DecisionGateEngine 使用。
 *
 * <p>红线： 1. 不出号； 2. 不结算； 3. 不创建Trade； 4. 不保存BET； 5. 不修改Decision； 6. 不写真实盈亏； 7. 不把SHADOW直接转BET； 8.
 * 不绕过 DecisionGateEngine。
 *
 * <p>说明： PROMOTE_WATCH 只是“晋级观察建议”，不是BET。 最终真实BET仍必须由 DecisionGateEngine 唯一决定。
 *
 * <p>Java7 / AIDE 兼容：无 lambda、无 stream、无第三方库。
 */
public final class ShadowPromotionVerifierEngine {
    public static final String FREQUENCY_FLOOR_VERSION = AppConfig.FREQUENCY_FLOOR_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String DEADLOCK_RECOVERY_BUILD =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final String VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    public static final String MAIN_CHAIN_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    public static final String STATUS_PROMOTE_WATCH = "PROMOTE_WATCH";
    public static final String STATUS_WATCH = "WATCH";
    public static final String STATUS_BLOCK = "BLOCK";
    public static final String STATUS_INSUFFICIENT = "INSUFFICIENT";
    public static final String STATUS_NO_PROMOTE = "NO_PROMOTE";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_STRONG = "STRONG";
    public static final String STATUS_FREEZE = "FREEZE";

    public static final String DOMAIN_POSITION = "POSITION";
    public static final String DOMAIN_FINALP = "FINALP";
    public static final String DOMAIN_EDGE = "EDGE";
    public static final String DOMAIN_TEACHER = "TEACHER";
    public static final String DOMAIN_V20 = "V20";
    public static final String DOMAIN_WINDOW = "WINDOW";
    public static final String DOMAIN_SAMPLE = "SAMPLE";
    public static final String DOMAIN_RECENT = "RECENT";
    public static final String DOMAIN_COMBO = "COMBO";
    public static final String DOMAIN_K3_LAYER = "K3_LAYER";
    public static final String DOMAIN_GATE = "GATE";
    public static final String DOMAIN_DYNAMIC_CLUSTER = "DYNAMIC_CLUSTER";
    public static final String DOMAIN_NEED_SUPPLY = "NEED_SUPPLY";
    public static final String DOMAIN_COUNTERFACTUAL = "COUNTERFACTUAL";
    public static final String DOMAIN_THIRD_QUALITY = "THIRD_QUALITY";
    public static final String DOMAIN_ADAPTIVE = "ADAPTIVE";
    public static final String DOMAIN_CAUSAL_COMBO = "CAUSAL_COMBO";

    private static final int DEFAULT_SHADOW_LIMIT = 360;
    private static final int DEFAULT_BET_LIMIT = 240;
    private static final int DEFAULT_DECISION_SCAN_LIMIT = 900;
    private static final int MIN_PROMOTE_SAMPLE = AppConfig.R35_PROMOTION_MIN_OOS_SAMPLE;
    private static final int MIN_WATCH_SAMPLE = 30;
    private static final int MIN_BLOCK_SAMPLE = 60;
    private static final int MAX_REPORT_LEN = 19000;
    private static final int MAX_CASES_IN_DOMAIN = 18;
    private static final int POSITION_COUNT = 10;

    private static final double BASE_K3_RATE = 0.300d;
    private static final double PROMOTE_HIT_RATE = 0.380d;
    private static final double STRONG_PROMOTE_RATE = 0.400d;
    private static final double WATCH_HIT_RATE = 0.340d;
    private static final double BLOCK_RATE = 0.270d;
    private static final double PROMOTE_OVER_BET_MARGIN = 0.055d;
    private static final double STRONG_EDGE_MARGIN = 0.080d;
    private static final int RECENT30_LIMIT = 30;
    private static final int RECENT60_LIMIT = 60;
    private static final int RECENT120_LIMIT = 120;
    private static final double DYNAMIC_CLUSTER_READY_RATE =
            AppConfig.DYNAMIC_CLUSTER_READY_HIT_RATE;
    private static final double DYNAMIC_CLUSTER_STRONG_RATE =
            AppConfig.DYNAMIC_CLUSTER_STRONG_HIT_RATE;
    private static final double DYNAMIC_CLUSTER_FREEZE_RATE =
            AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE;
    private static final double DYNAMIC_CLUSTER_RECENT_READY_RATE =
            AppConfig.DYNAMIC_CLUSTER_RECENT_READY_HIT_RATE;
    private static final double NEED_SUPPLY_READY_LINE = AppConfig.NEED_SUPPLY_READY_MATCH_SCORE;
    private static final double COUNTERFACTUAL_HARD_LINE =
            AppConfig.COUNTERFACTUAL_HARD_WHY_NOT_RISK;
    private static final double PROMOTION_READY_SCORE = 0.520d;
    private static final double PROMOTION_STRONG_SCORE = 0.620d;
    private static final String PROMOTION_REJECT_OK = "SHADOW_PROMOTION_OK";
    private static final String PROMOTION_REJECT_SAMPLE_WEAK = "SHADOW_PROMOTION_SAMPLE_WEAK";
    private static final String PROMOTION_REJECT_WATCH_ONLY = "SHADOW_PROMOTION_WATCH_ONLY";
    private static final String PROMOTION_REJECT_NO_PROMOTE = "SHADOW_PROMOTION_NOT_READY";
    private static final String PROMOTION_REJECT_BLOCKED = "SHADOW_PROMOTION_BLOCKED_DOMAIN";
    private static final String PROMOTION_REJECT_FREEZE = "SHADOW_PROMOTION_FREEZE_DOMAIN";

    private static final String LINK_V2_VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;
    private static final long REALTIME_REPORT_CACHE_MS = 10L * 60L * 1000L;
    private static final int REALTIME_DOMAIN_LOAD_LIMIT = 240;
    private static final int REALTIME_CANDIDATE_LIMIT = 120;
    private static volatile ShadowPromotionReport cachedFullReport;
    private static volatile long cachedFullReportAt;

    public ShadowPromotionVerifierEngine() {}

    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        return applyPromotionToComboCandidates(store, candidates, "applyToAll_fix15");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        return applyPromotionToComboCandidates(store, candidates, "enrichCandidates_fix15");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        return applyPromotionToComboCandidates(store, candidates, "analyzeCandidates_fix15");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        return applyPromotionToComboCandidates(store, candidates, "applyRealtime_fix15");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            DbStore store, ArrayList<Models.ComboCandidate> candidates) {
        return applyPromotionToComboCandidates(store, candidates, "runRealtime_fix15");
    }

    public ArrayList<Models.ComboCandidate> apply(DbStore store, List candidates) {
        return applyPromotionToComboCandidates(store, candidates, "apply_raw_fix15");
    }

    public ArrayList<Models.ComboCandidate> run(DbStore store, List candidates) {
        return applyPromotionToComboCandidates(store, candidates, "run_raw_fix15");
    }

    public int applyAndCount(DbStore store, List candidates) {
        return applyPromotionFieldsToCandidates(
                store, candidates, null, "applyAndCount_fix15", loadV2DomainMap(store));
    }

    private ShadowPromotionReport safeVerify(DbStore store) {
        long now = System.currentTimeMillis();
        ShadowPromotionReport cached = cachedFullReport;
        if (cached != null && now - cachedFullReportAt < REALTIME_REPORT_CACHE_MS) {
            return cached;
        }
        try {
            ShadowPromotionReport report = verify(store, DEFAULT_SHADOW_LIMIT, DEFAULT_BET_LIMIT);
            cachedFullReport = report;
            cachedFullReportAt = now;
            return report;
        } catch (Throwable e) {
            return cached;
        }
    }

    private ArrayList<Models.ComboCandidate> applyPromotionToComboCandidates(
            DbStore store, List rawCandidates, String source) {
        HashMap<String, Models.ShadowDomainStat> v2Map = loadV2DomainMap(store);
        try {
            if (store != null)
                store.registerModuleBuildV20("ShadowPromotionVerifierEngine", VERSION, source);
        } catch (Throwable ignore) {
        }
        ShadowPromotionReport report = null;
        long now = System.currentTimeMillis();
        if (cachedFullReport != null && now - cachedFullReportAt < REALTIME_REPORT_CACHE_MS) {
            report = cachedFullReport;
        }
        applyPromotionFieldsToCandidates(store, rawCandidates, report, source, v2Map);
        return castComboCandidates(rawCandidates);
    }

    private ArrayList<Models.ComboCandidate> castComboCandidates(List rawCandidates) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (rawCandidates == null) {
            return out;
        }
        for (int i = 0; i < rawCandidates.size(); i++) {
            Object o = rawCandidates.get(i);
            if (o instanceof Models.ComboCandidate) {
                out.add((Models.ComboCandidate) o);
            }
        }
        return out;
    }

    private int applyPromotionFieldsToCandidates(
            DbStore store, List rawCandidates, ShadowPromotionReport report, String source) {
        return applyPromotionFieldsToCandidates(
                store, rawCandidates, report, source, loadV2DomainMap(store));
    }

    private int applyPromotionFieldsToCandidates(
            DbStore store,
            List rawCandidates,
            ShadowPromotionReport report,
            String source,
            HashMap<String, Models.ShadowDomainStat> v2Map) {
        if (rawCandidates == null || rawCandidates.size() == 0) {
            writePromotionApplyState(store, 0, 0, 0, 0, "候选为空，SHADOW晋级验证不回写；不救援BET");
            return 0;
        }
        int applied = 0;
        int promote = 0;
        int watch = 0;
        int block = 0;
        int v2Hit = 0;
        int limit = Math.min(rawCandidates.size(), REALTIME_CANDIDATE_LIMIT);
        for (int i = 0; i < rawCandidates.size(); i++) {
            Object c = rawCandidates.get(i);
            if (c == null) {
                continue;
            }
            if (i >= limit) {
                writePromotionDeferredFields(c, i + 1, source);
                continue;
            }
            if (getInt(c, "evidenceDomainFrozen", 0) != 1) {
                putStringSafe(c, "shadowPromotionStatus", STATUS_WATCH);
                putStringSafe(c, "shadowPromotionReason", "FinalP域尚未冻结，Promotion只WATCH，禁止自行重算概率段");
                continue;
            }
            PromotionCandidateSignal sig = resolveV2PromotionCandidateSignal(c, v2Map);
            if (sig != null) {
                v2Hit++;
            } else {
                sig = resolvePromotionCandidateSignal(c, report);
            }
            if (sig == null) {
                continue;
            }
            writePromotionFieldsToCandidate(c, sig, source);
            applied++;
            if (STATUS_PROMOTE_WATCH.equals(sig.rawStatus)
                    || STATUS_READY.equals(sig.shadowPromotionStatus)
                    || STATUS_STRONG.equals(sig.shadowPromotionStatus)) {
                promote++;
            } else if (STATUS_WATCH.equals(sig.shadowPromotionStatus)
                    || AppConfig.PROMOTION_WATCH_POSITIVE.equals(sig.shadowPromotionStatus)) {
                watch++;
            } else if (STATUS_BLOCK.equals(sig.rawStatus) || STATUS_FREEZE.equals(sig.rawStatus)) {
                block++;
            }
        }
        writePromotionApplyState(
                store,
                applied,
                promote,
                watch,
                block,
                "ShadowPromotion实时回写完成：候选="
                        + rawCandidates.size()
                        + "｜回写="
                        + applied
                        + "｜V2增量域命中="
                        + v2Hit
                        + "｜晋级观察="
                        + promote
                        + "｜继续观察="
                        + watch
                        + "｜冻结/屏蔽="
                        + block
                        + "｜说明=优先读取Settlement增量域，不每期重扫900条Decision；不创建BET，不改真实统计");
        return applied;
    }

    private void writePromotionApplyState(
            DbStore store, int applied, int promote, int watch, int block, String summary) {
        if (store == null) {
            return;
        }
        setStateSafe(store, "shadow_promotion_candidate_apply", applied > 0 ? "1" : "0");
        setStateSafe(store, "shadow_promotion_candidate_apply_count", String.valueOf(applied));
        setStateSafe(store, "shadow_promotion_candidate_promote_count", String.valueOf(promote));
        setStateSafe(store, "shadow_promotion_candidate_watch_count", String.valueOf(watch));
        setStateSafe(store, "shadow_promotion_candidate_block_count", String.valueOf(block));
        setStateSafe(store, "shadow_promotion_candidate_version", LINK_V2_VERSION);
        setStateSafe(store, "shadow_promotion_candidate_summary", summary == null ? "" : summary);
        setStateSafe(
                store,
                "shadow_promotion_candidate_updated_at",
                String.valueOf(System.currentTimeMillis()));
    }

    public ShadowPromotionReport verify(DbStore store) {
        return verify(store, DEFAULT_SHADOW_LIMIT, DEFAULT_BET_LIMIT);
    }

    public ShadowPromotionReport verify(DbStore store, int shadowLimit, int betLimit) {
        ShadowPromotionReport report = new ShadowPromotionReport();
        report.version = VERSION;
        report.createdAt = System.currentTimeMillis();

        if (shadowLimit <= 0) {
            shadowLimit = DEFAULT_SHADOW_LIMIT;
        }
        if (betLimit <= 0) {
            betLimit = DEFAULT_BET_LIMIT;
        }
        report.shadowLimit = shadowLimit;
        report.betLimit = betLimit;

        if (store == null) {
            report.ok = 0;
            report.summary = "SHADOW晋级验证失败：DbStore为空";
            report.detail = report.summary;
            return report;
        }

        ArrayList<ShadowCase> shadowCases = loadRecentShadowCases(store, shadowLimit);
        ArrayList<ShadowCase> realBetCases = loadRecentRealBetCases(store, betLimit);

        report.ok = 1;
        report.shadowCases = shadowCases;
        report.realBetCases = realBetCases;

        analyzeRealBetBaseline(report, realBetCases);
        analyzeShadowCases(report, shadowCases);
        finalizeDomains(report);
        buildReportText(report);

        return report;
    }

    public String buildShadowPromotionSummary(DbStore store) {
        ShadowPromotionReport r = verify(store);
        return r == null ? "" : safe(r.summary);
    }

    public String buildShadowPromotionDetail(DbStore store) {
        ShadowPromotionReport r = verify(store);
        return r == null ? "" : safe(r.detail);
    }

    public String buildShadowPromotionCopyText(DbStore store) {
        ShadowPromotionReport r = verify(store);
        if (r == null) {
            return "V20 SHADOW晋级验证为空";
        }
        return safe(r.detail);
    }

    public ShadowPromotionReport buildReport(DbStore store) {
        return verify(store);
    }

    public ShadowPromotionReport buildShadowPromotionReport(DbStore store) {
        return verify(store);
    }

    public ShadowPromotionReport analyze(DbStore store) {
        return verify(store);
    }

    public ShadowPromotionReport refresh(DbStore store) {
        return saveShadowPromotionState(store);
    }

    public ShadowPromotionReport saveShadowPromotionState(DbStore store) {
        ShadowPromotionReport r = verify(store);
        saveShadowPromotionState(store, r);
        return r;
    }

    public void saveShadowPromotionState(DbStore store, ShadowPromotionReport report) {
        if (store == null || report == null) {
            return;
        }
        setStateSafe(store, "shadow_promotion_version", safe(report.version));
        setStateSafe(store, "shadow_promotion_updated_at", String.valueOf(report.createdAt));
        setStateSafe(store, "shadow_promotion_status", report.ok == 1 ? "OK" : "ERROR");
        setStateSafe(store, "shadow_promotion_total", String.valueOf(report.totalShadow));
        setStateSafe(store, "shadow_promotion_settled", String.valueOf(report.settledShadow));
        setStateSafe(store, "shadow_promotion_hit", String.valueOf(report.wouldHit));
        setStateSafe(store, "shadow_promotion_miss", String.valueOf(report.wouldMiss));
        setStateSafe(store, "shadow_promotion_rate", String.valueOf(report.wouldHitRate));
        setStateSafe(store, "shadow_promotion_promote_count", String.valueOf(report.promoteCount));
        setStateSafe(store, "shadow_promotion_watch_count", String.valueOf(report.watchCount));
        setStateSafe(store, "shadow_promotion_block_count", String.valueOf(report.blockCount));
        setStateSafe(
                store,
                "shadow_promotion_best_domain",
                report.bestDomain == null ? "" : report.bestDomain.domainKey);
        setStateSafe(
                store,
                "shadow_promotion_worst_domain",
                report.worstDomain == null ? "" : report.worstDomain.domainKey);
        setStateSafe(store, "shadow_promotion_main_signal", safe(report.mainSignal));
        setStateSafe(store, "shadow_promotion_next_advice", safe(report.nextAdvice));
        setStateSafe(store, "shadow_promotion_summary", safe(report.summary));
        setStateSafe(store, "shadow_promotion_detail", safe(report.detail));
        setStateSafe(store, "shadow_promotion_compact_json", safe(buildCompactJson(report)));
        saveDynamicClusterPromotionSnapshots(store, report);
    }

    private void analyzeRealBetBaseline(
            ShadowPromotionReport report, ArrayList<ShadowCase> betCases) {
        if (report == null || betCases == null) {
            return;
        }

        for (int i = 0; i < betCases.size(); i++) {
            ShadowCase c = betCases.get(i);
            if (c == null || !"BET".equals(c.kind)) {
                continue;
            }
            if (c.settled != 1) {
                report.realBetWait++;
                continue;
            }
            if (!isWinResult(c.result) && !isMissResult(c.result)) {
                report.realBetError++;
                continue;
            }

            report.realBetTotal++;
            if (c.hit == 1 || isWinResult(c.result)) {
                report.realBetHit++;
            } else {
                report.realBetMiss++;
            }
            report.realBetAvgFinalP += c.finalP;
            report.realBetAvgGate += c.dynamicGate;
            report.realBetAvgEdge += c.edge;

            addRealBetDomain(report, DOMAIN_POSITION, "P" + c.position, c);
            addRealBetDomain(report, DOMAIN_FINALP, c.finalPBucket, c);
            addRealBetDomain(report, DOMAIN_EDGE, c.edgeBucket, c);
            addRealBetDomain(report, DOMAIN_TEACHER, c.teacherBucket, c);
            addRealBetDomain(report, DOMAIN_V20, c.v20Bucket, c);
            addRealBetDomain(report, DOMAIN_WINDOW, c.windowBucket, c);
            addRealBetDomain(report, DOMAIN_SAMPLE, c.sampleBucket, c);
            addRealBetDomain(report, DOMAIN_RECENT, c.recentBucket, c);
            addRealBetDomain(report, DOMAIN_GATE, c.gateBucket, c);
            addRealBetDomain(report, DOMAIN_COMBO, comboKey(c), c);
            addRealBetDomain(report, DOMAIN_DYNAMIC_CLUSTER, c.dynamicClusterBucket, c);
            addRealBetDomain(report, DOMAIN_NEED_SUPPLY, c.needSupplyBucket, c);
            addRealBetDomain(report, DOMAIN_COUNTERFACTUAL, c.counterfactualBucket, c);
            addRealBetDomain(report, DOMAIN_THIRD_QUALITY, c.thirdQualityBucket, c);
            addRealBetDomain(report, DOMAIN_ADAPTIVE, c.adaptiveBucket, c);
            addRealBetDomain(report, DOMAIN_CAUSAL_COMBO, causalComboKey(c), c);
        }

        if (report.realBetTotal > 0) {
            report.realBetRate = ((double) report.realBetHit) / ((double) report.realBetTotal);
            report.realBetAvgFinalP = report.realBetAvgFinalP / (double) report.realBetTotal;
            report.realBetAvgGate = report.realBetAvgGate / (double) report.realBetTotal;
            report.realBetAvgEdge = report.realBetAvgEdge / (double) report.realBetTotal;
        }
    }

    private void analyzeShadowCases(ShadowPromotionReport report, ArrayList<ShadowCase> cases) {
        if (report == null || cases == null) {
            return;
        }

        for (int i = 0; i < cases.size(); i++) {
            ShadowCase c = cases.get(i);
            if (c == null || !"SHADOW".equals(c.kind)) {
                continue;
            }
            report.totalShadow++;

            if (c.settled != 1) {
                report.waitShadow++;
                continue;
            }

            report.settledShadow++;
            if (c.hit == 1 || isWinResult(c.result)) {
                report.wouldHit++;
            } else {
                report.wouldMiss++;
            }
            report.avgFinalP += c.finalP;
            report.avgGate += c.dynamicGate;
            report.avgEdge += c.edge;

            addShadowDomain(report, DOMAIN_POSITION, "P" + c.position, c, "位置SHADOW反事实");
            addShadowDomain(report, DOMAIN_FINALP, c.finalPBucket, c, "FinalP区间SHADOW反事实");
            addShadowDomain(report, DOMAIN_EDGE, c.edgeBucket, c, "Edge区间SHADOW反事实");
            addShadowDomain(report, DOMAIN_TEACHER, c.teacherBucket, c, "Teacher状态SHADOW反事实");
            addShadowDomain(report, DOMAIN_V20, c.v20Bucket, c, "V20因链SHADOW反事实");
            addShadowDomain(report, DOMAIN_WINDOW, c.windowBucket, c, "WindowState SHADOW反事实");
            addShadowDomain(report, DOMAIN_SAMPLE, c.sampleBucket, c, "样本可信SHADOW反事实");
            addShadowDomain(report, DOMAIN_RECENT, c.recentBucket, c, "近期有效SHADOW反事实");
            addShadowDomain(report, DOMAIN_GATE, c.gateBucket, c, "动态门槛SHADOW反事实");
            addShadowDomain(report, DOMAIN_COMBO, comboKey(c), c, "组合域SHADOW反事实");

            addShadowDomain(
                    report, DOMAIN_DYNAMIC_CLUSTER, c.dynamicClusterBucket, c, "动态窗口簇 SHADOW 反事实");
            addShadowDomain(
                    report,
                    DOMAIN_NEED_SUPPLY,
                    c.needSupplyBucket,
                    c,
                    "NeedSupply 需求供给 SHADOW 反事实");
            addShadowDomain(
                    report, DOMAIN_COUNTERFACTUAL, c.counterfactualBucket, c, "反事实反证 SHADOW 反事实");
            addShadowDomain(
                    report, DOMAIN_THIRD_QUALITY, c.thirdQualityBucket, c, "第三号质量 SHADOW 反事实");
            addShadowDomain(
                    report, DOMAIN_ADAPTIVE, c.adaptiveBucket, c, "AdaptiveJudge 审判域 SHADOW 反事实");
            addShadowDomain(
                    report, DOMAIN_CAUSAL_COMBO, causalComboKey(c), c, "动态因果组合域 SHADOW 反事实");
            buildShadowCaseHint(c);
        }

        if (report.settledShadow > 0) {
            report.wouldHitRate = ((double) report.wouldHit) / ((double) report.settledShadow);
            report.avgFinalP = report.avgFinalP / (double) report.settledShadow;
            report.avgGate = report.avgGate / (double) report.settledShadow;
            report.avgEdge = report.avgEdge / (double) report.settledShadow;
        }
    }

    private void finalizeDomains(ShadowPromotionReport report) {
        if (report == null) {
            return;
        }

        report.promotedDomains.clear();
        report.watchDomains.clear();
        report.blockedDomains.clear();
        report.allDomains.clear();

        for (String key : report.domainMap.keySet()) {
            ShadowDomainStats d = report.domainMap.get(key);
            if (d == null) {
                continue;
            }
            if (isUnsafeOutcomeDomain(d.domainType, d.domainKey, d.domainValue)) {
                report.insufficientCount++;
                continue;
            }
            RealBetDomainStats real = report.realBetDomainMap.get(key);
            if (real != null) {
                d.realBetCount = real.total;
                d.realBetHit = real.hit;
                d.realBetMiss = real.miss;
                d.realBetHitRate = real.rate();
            } else {
                d.realBetCount = 0;
                d.realBetHit = 0;
                d.realBetMiss = 0;
                d.realBetHitRate = report.realBetRate;
            }

            finalizeDomain(d, report);
            report.allDomains.add(d);

            if (STATUS_PROMOTE_WATCH.equals(d.promotionStatus)) {
                report.promotedDomains.add(d);
                report.promoteCount++;
            } else if (STATUS_WATCH.equals(d.promotionStatus)) {
                report.watchDomains.add(d);
                report.watchCount++;
            } else if (STATUS_BLOCK.equals(d.promotionStatus)) {
                report.blockedDomains.add(d);
                report.blockCount++;
            } else {
                report.insufficientCount++;
            }

            if (report.bestDomain == null || isBetterDomain(d, report.bestDomain)) {
                report.bestDomain = d;
            }
            if (report.worstDomain == null || isWorseDomain(d, report.worstDomain)) {
                report.worstDomain = d;
            }
        }

        sortDomains(report.allDomains);
        sortDomains(report.promotedDomains);
        sortDomains(report.watchDomains);
        sortDomains(report.blockedDomains);

        buildPromotionSignals(report);
        buildMainSignalAndAdvice(report);
    }

    private void finalizeDomain(ShadowDomainStats d, ShadowPromotionReport report) {
        if (d == null) {
            return;
        }

        if (d.total > 0) {
            d.wouldHitRate = ((double) d.wouldHit) / ((double) d.total);
            d.avgFinalP = d.sumFinalP / (double) d.total;
            d.avgGate = d.sumGate / (double) d.total;
            d.avgEdge = d.sumEdge / (double) d.total;
        } else {
            d.wouldHitRate = 0.0d;
            d.avgFinalP = 0.0d;
            d.avgGate = 0.0d;
            d.avgEdge = 0.0d;
        }

        d.virtualEdge = d.wouldHitRate - BASE_K3_RATE;
        d.overRealBetEdge = d.wouldHitRate - d.realBetHitRate;
        finalizeRecentStats(d);
        d.reliability = computeReliability(d);
        if (finalizeDynamicClusterDomain(d, report)) {
            return;
        }

        if (d.total < MIN_WATCH_SAMPLE) {
            d.promotionStatus = STATUS_INSUFFICIENT;
            d.reason = "SHADOW样本不足，不能晋级。";
            d.advice = "继续观察，不允许直接BET。";
            return;
        }

        if (d.total >= MIN_PROMOTE_SAMPLE
                && d.wouldHitRate >= STRONG_PROMOTE_RATE
                && d.virtualEdge >= STRONG_EDGE_MARGIN
                && d.overRealBetEdge >= PROMOTE_OVER_BET_MARGIN) {
            d.promotionStatus = STATUS_PROMOTE_WATCH;
            d.reason = "SHADOW反事实高于40%，且明显高于同域真实BET，属于强晋级观察域。";
            d.advice = "交给 AdaptiveLearningJudgeEngine 提高排序权重；最终仍需 DecisionGateEngine 判断，不得直接BET。";
            return;
        }

        if (d.total >= MIN_PROMOTE_SAMPLE
                && d.wouldHitRate >= PROMOTE_HIT_RATE
                && d.virtualEdge >= 0.060d
                && (d.overRealBetEdge >= PROMOTE_OVER_BET_MARGIN
                        || d.realBetCount < MIN_WATCH_SAMPLE)) {
            d.promotionStatus = STATUS_PROMOTE_WATCH;
            d.reason = "SHADOW反事实达到晋级线，真实BET同域不足或明显更弱。";
            d.advice = "标记 PROMOTE_WATCH，下一期审判可加分但不能绕过FinalP/Gate/冻结。";
            return;
        }

        if (d.total >= MIN_BLOCK_SAMPLE && d.wouldHitRate <= BLOCK_RATE) {
            d.promotionStatus = STATUS_BLOCK;
            d.reason = "SHADOW反事实低于基础线，属于噪声或假强域。";
            d.advice = "交给 DynamicFreezeMatrixEngine 纳入 blockedDomains，禁止该域低证据晋级。";
            return;
        }

        if (d.total >= MIN_WATCH_SAMPLE && d.wouldHitRate >= WATCH_HIT_RATE) {
            d.promotionStatus = STATUS_WATCH;
            d.reason = "SHADOW反事实有观察价值，但未达到晋级线。";
            d.advice = "继续观察，不提高真实BET权限。";
            return;
        }

        d.promotionStatus = STATUS_NO_PROMOTE;
        d.reason = "SHADOW反事实不够强，暂不晋级。";
        d.advice = "保持SHADOW观察。";
    }

    private void fillShadowPromotionDomainAliases(ShadowDomainStats d) {
        if (d == null) {
            return;
        }
        d.promotionSampleCount = d.total;
        d.shadowPromoteScore = promotionScore(d);
        d.shadowActionAdvice = safe(d.advice);
        if (STATUS_BLOCK.equals(d.promotionStatus)) {
            d.shadowRejectCode = PROMOTION_REJECT_BLOCKED;
        } else if (STATUS_FREEZE.equals(d.promotionStatus)
                || AppConfig.CLUSTER_STATUS_FREEZE.equals(d.clusterStatus)) {
            d.shadowRejectCode = PROMOTION_REJECT_FREEZE;
        } else if (STATUS_INSUFFICIENT.equals(d.promotionStatus)) {
            d.shadowRejectCode = PROMOTION_REJECT_SAMPLE_WEAK;
        } else if (STATUS_NO_PROMOTE.equals(d.promotionStatus)) {
            d.shadowRejectCode = PROMOTION_REJECT_NO_PROMOTE;
        } else if (STATUS_WATCH.equals(d.promotionStatus)) {
            d.shadowRejectCode = PROMOTION_REJECT_WATCH_ONLY;
        } else {
            d.shadowRejectCode = PROMOTION_REJECT_OK;
        }
    }

    private double computeReliability(ShadowDomainStats d) {
        if (d == null) return 0.0d;
        double sampleScore;
        if (d.total >= 80) sampleScore = 1.0d;
        else if (d.total >= 40) sampleScore = 0.78d;
        else if (d.total >= 18) sampleScore = 0.55d;
        else if (d.total >= 10) sampleScore = 0.32d;
        else sampleScore = 0.12d;

        double realScore;
        if (d.realBetCount >= 60) realScore = 0.75d;
        else if (d.realBetCount >= 20) realScore = 0.45d;
        else if (d.realBetCount > 0) realScore = 0.22d;
        else realScore = 0.08d;

        double stability = 0.5d;
        if (d.total >= MIN_PROMOTE_SAMPLE) {
            double diff = Math.abs(d.wouldHitRate - d.realBetHitRate);
            if (diff >= 0.10d) stability = 0.85d;
            else if (diff >= 0.055d) stability = 0.65d;
            else stability = 0.42d;
        }

        double recentScore =
                d.recent60Total >= AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                        ? clamp01(d.recent60Rate)
                        : 0.30d;
        double loseScore =
                1.0d
                        - Math.min(
                                1.0d,
                                ((double) d.maxLoseStreak)
                                        / Math.max(1, AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE));
        double score =
                sampleScore * 0.42d
                        + realScore * 0.14d
                        + stability * 0.18d
                        + recentScore * 0.16d
                        + loseScore * 0.10d;
        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private void buildPromotionSignals(ShadowPromotionReport report) {
        if (report == null) {
            return;
        }
        report.signals.clear();

        int limit = Math.min(report.promotedDomains.size(), 12);
        for (int i = 0; i < limit; i++) {
            ShadowDomainStats d = report.promotedDomains.get(i);
            if (d == null) continue;
            PromotionSignal s = new PromotionSignal();
            s.domainType = d.domainType;
            s.domainKey = d.domainKey;
            s.status = d.promotionStatus;
            s.score = promotionScore(d);
            s.sample = d.total;
            s.wouldHitRate = d.wouldHitRate;
            s.realBetHitRate = d.realBetHitRate;
            s.virtualEdge = d.virtualEdge;
            s.overRealBetEdge = d.overRealBetEdge;
            s.reliability = d.reliability;
            s.clusterStatus = d.clusterStatus;
            s.action = d.promotionAction;
            s.recentHitRate = d.recent60Rate;
            s.maxLoseStreak = d.maxLoseStreak;
            s.reason = d.reason;
            s.actionAdvice = "PROMOTE_WATCH：只提高后续候选排序/审判加分，不直接BET，最终仍由DecisionGateEngine决定。";
            report.signals.add(s);
        }

        int watchLimit = Math.min(report.watchDomains.size(), 6);
        for (int i = 0; i < watchLimit; i++) {
            ShadowDomainStats d = report.watchDomains.get(i);
            if (d == null) continue;
            PromotionSignal s = new PromotionSignal();
            s.domainType = d.domainType;
            s.domainKey = d.domainKey;
            s.status = d.promotionStatus;
            s.score = promotionScore(d);
            s.sample = d.total;
            s.wouldHitRate = d.wouldHitRate;
            s.realBetHitRate = d.realBetHitRate;
            s.virtualEdge = d.virtualEdge;
            s.overRealBetEdge = d.overRealBetEdge;
            s.reliability = d.reliability;
            s.clusterStatus = d.clusterStatus;
            s.action = d.promotionAction;
            s.recentHitRate = d.recent60Rate;
            s.maxLoseStreak = d.maxLoseStreak;
            s.reason = d.reason;
            s.actionAdvice = "WATCH：继续SHADOW观察，不进入BET加权。";
            report.signals.add(s);
        }
    }

    private double promotionScore(ShadowDomainStats d) {
        if (d == null) return 0.0d;
        double rateScore = d.wouldHitRate;
        double edgeScore = d.virtualEdge + 0.30d;
        if (edgeScore < 0.0d) edgeScore = 0.0d;
        if (edgeScore > 1.0d) edgeScore = 1.0d;
        double overScore = d.overRealBetEdge + 0.30d;
        if (overScore < 0.0d) overScore = 0.0d;
        if (overScore > 1.0d) overScore = 1.0d;
        double score =
                rateScore * 0.42d + edgeScore * 0.22d + overScore * 0.18d + d.reliability * 0.18d;
        if (score < 0.0d) score = 0.0d;
        if (score > 1.0d) score = 1.0d;
        return score;
    }

    private void buildMainSignalAndAdvice(ShadowPromotionReport r) {
        if (r == null) {
            return;
        }

        String signal = "";
        String advice = "";

        if (r.settledShadow < MIN_WATCH_SAMPLE) {
            signal = append(signal, "SHADOW已开奖样本少于" + MIN_WATCH_SAMPLE + "，暂不能判断晋级域");
            advice = append(advice, "继续积累SHADOW反事实样本，不允许因为样本少直接放宽BET");
        }

        if (r.promoteCount > 0) {
            signal = append(signal, "发现" + r.promoteCount + "个PROMOTE_WATCH晋级观察域");
            advice =
                    append(
                            advice,
                            "第6/14 AdaptiveLearningJudgeEngine"
                                + " 可读取这些域作为加分信号，但必须同时检查冻结、Teacher、V20、FinalP/Gate");
        }

        if (r.blockCount > 0) {
            signal = append(signal, "发现" + r.blockCount + "个BLOCK噪声域");
            advice = append(advice, "第5/14 DynamicFreezeMatrixEngine 应把这些低效SHADOW域纳入冻结/屏蔽观察");
        }

        if (r.settledShadow >= MIN_PROMOTE_SAMPLE
                && r.realBetTotal >= MIN_WATCH_SAMPLE
                && r.wouldHitRate > r.realBetRate + PROMOTE_OVER_BET_MARGIN) {
            signal = append(signal, "SHADOW整体反事实胜率高于真实BET，可能存在错杀强单");
            advice = append(advice, "不要全局降门槛，要按domain精细晋级，只让验证强域进入审判加分");
        }

        if (r.bestDomain != null && STATUS_PROMOTE_WATCH.equals(r.bestDomain.promotionStatus)) {
            signal = append(signal, "最佳晋级域=" + r.bestDomain.shortText());
        }

        if (r.worstDomain != null && STATUS_BLOCK.equals(r.worstDomain.promotionStatus)) {
            signal = append(signal, "最差噪声域=" + r.worstDomain.shortText());
        }

        if (signal.length() == 0) {
            signal = "暂未发现稳定SHADOW晋级域，保持观察。";
        }
        if (advice.length() == 0) {
            advice = "SHADOW只做反事实，不直接转BET；后续通过冻结矩阵和智能审判逐步使用。";
        }

        r.mainSignal = signal;
        r.nextAdvice = advice;
    }

    private void buildReportText(ShadowPromotionReport r) {
        if (r == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【V20 SHADOW反事实晋级验证报告】\n");
        sb.append("版本=").append(VERSION).append("\n");
        sb.append("口径=只读SHADOW + 已开奖targetIssue；只算反事实wouldHit，不计真实胜率盈亏；不转BET。\n\n");

        sb.append("一、SHADOW总体\n");
        sb.append("SHADOW总数=")
                .append(r.totalShadow)
                .append("，已开奖=")
                .append(r.settledShadow)
                .append("，wouldHit=")
                .append(r.wouldHit)
                .append("，wouldMiss=")
                .append(r.wouldMiss)
                .append("，反事实胜率=")
                .append(pct(r.wouldHitRate))
                .append("，等待=")
                .append(r.waitShadow)
                .append("\n");
        sb.append("均FinalP=")
                .append(pct(r.avgFinalP))
                .append("，均Gate=")
                .append(pct(r.avgGate))
                .append("，均Edge=")
                .append(signedPct(r.avgEdge))
                .append("\n");
        sb.append("真实BET基准：")
                .append(r.realBetTotal)
                .append("单，中")
                .append(r.realBetHit)
                .append("，挂")
                .append(r.realBetMiss)
                .append("，胜率=")
                .append(pct(r.realBetRate))
                .append("，等待=")
                .append(r.realBetWait)
                .append("，异常=")
                .append(r.realBetError)
                .append("\n\n");

        sb.append("二、晋级域统计\n");
        sb.append("PROMOTE_WATCH=")
                .append(r.promoteCount)
                .append("，WATCH=")
                .append(r.watchCount)
                .append("，BLOCK=")
                .append(r.blockCount)
                .append("，INSUFFICIENT/NO_PROMOTE=")
                .append(r.insufficientCount)
                .append("\n");
        if (r.bestDomain != null) {
            sb.append("最佳SHADOW域=").append(r.bestDomain.fullText()).append("\n");
        }
        if (r.worstDomain != null) {
            sb.append("最差SHADOW域=").append(r.worstDomain.fullText()).append("\n");
        }
        sb.append("\n");

        sb.append("三、PROMOTE_WATCH 晋级观察域\n");
        appendDomainList(sb, r.promotedDomains, 15);
        sb.append("\n");

        sb.append("四、WATCH 继续观察域\n");
        appendDomainList(sb, r.watchDomains, 12);
        sb.append("\n");

        sb.append("五、BLOCK 噪声/假强域\n");
        appendDomainList(sb, r.blockedDomains, 12);
        sb.append("\n");

        sb.append("六、全部高频域Top\n");
        appendDomainList(sb, r.allDomains, 20);
        sb.append("\n");

        sb.append("七、动态窗口簇 / NeedSupply / 反事实域\n");
        appendDynamicDomainSection(sb, r);
        sb.append("\n");
        sb.append("八、晋级信号给后续引擎\n");
        if (r.signals.size() == 0) {
            sb.append("暂无可输出晋级信号。\n");
        } else {
            for (int i = 0; i < r.signals.size(); i++) {
                PromotionSignal s = r.signals.get(i);
                if (s == null) continue;
                sb.append(i + 1).append(". ").append(s.fullText()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("九、结论\n");
        sb.append("主信号：").append(safe(r.mainSignal)).append("\n");
        sb.append("下一步建议：").append(safe(r.nextAdvice)).append("\n");
        sb.append("红线：PROMOTE_WATCH 不是BET，只能给第6/14审判加分，最终仍由DecisionGateEngine唯一决定真实BET。\n");

        r.detail = limit(sb.toString(), MAX_REPORT_LEN);
        r.summary = buildShortSummary(r);
    }

    private boolean finalizeDynamicClusterDomain(
            ShadowDomainStats d, ShadowPromotionReport report) {
        if (d == null) return false;
        boolean dynamicDomain = isDynamicPromotionDomain(d.domainType);
        if (!dynamicDomain) return false;
        if (report != null) report.dynamicDomainCount++;
        d.clusterStatus = decidePromotionClusterStatus(d);
        d.promotionAction = decidePromotionAction(d);
        d.promotionReason = buildDynamicPromotionReason(d);
        if (d.total < AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_WATCH) {
            d.promotionStatus = STATUS_INSUFFICIENT;
            d.reason = "动态域样本不足，cluster=" + d.clusterStatus + "，只能继续 SHADOW 观察。";
            d.advice = "样本不足不允许晋级，不允许直接 BET。";
            return true;
        }
        if (d.total >= AppConfig.DYNAMIC_HARD_BLOCK_MIN_SAMPLE
                && (d.wouldHitRate < DYNAMIC_CLUSTER_FREEZE_RATE
                        || (d.recent60Total >= AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                                && d.recent60Rate
                                        < AppConfig.DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE)
                        || d.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE)) {
            d.promotionStatus = STATUS_BLOCK;
            d.clusterStatus = AppConfig.CLUSTER_STATUS_FREEZE;
            d.promotionAction = AppConfig.ADAPTIVE_ACTION_FREEZE;
            if (report != null) report.dynamicFreezeCount++;
            d.reason =
                    "动态域低于随机基准或近期失效/连挂过高，冻结为假强域。 hit="
                            + pct(d.wouldHitRate)
                            + " recent60="
                            + pct(d.recent60Rate)
                            + " maxLose="
                            + d.maxLoseStreak;
            d.advice = "写入 PromotionDomainState=FREEZE，后续 AdaptiveJudge/DecisionGate 不得按该域晋级。";
            return true;
        }
        if (d.total >= AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_STRONG
                && d.wouldHitRate >= DYNAMIC_CLUSTER_STRONG_RATE
                && (d.recent60Total < AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                        || d.recent60Rate >= AppConfig.DYNAMIC_CLUSTER_RECENT_READY_HIT_RATE)
                && d.maxLoseStreak <= AppConfig.DYNAMIC_CLUSTER_MAX_LOSE_STRONG
                && d.reliability >= AppConfig.DYNAMIC_CLUSTER_MIN_CONFIDENCE_STRONG) {
            d.promotionStatus = STATUS_PROMOTE_WATCH;
            d.clusterStatus = AppConfig.CLUSTER_STATUS_STRONG;
            d.promotionAction = AppConfig.ADAPTIVE_ACTION_STRONG;
            if (report != null) report.dynamicReadyCount++;
            d.reason = "动态域达到 STRONG 级 SHADOW 晋级观察：样本、近期、连挂、可靠度均通过。";
            d.advice = "只写 STRONG 晋级证据，不直接 BET；最终仍由 DecisionGateEngine 唯一裁决。";
            return true;
        }
        if (d.total >= AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_READY
                && d.wouldHitRate >= DYNAMIC_CLUSTER_READY_RATE
                && (d.recent60Total < AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                        || d.recent60Rate >= DYNAMIC_CLUSTER_RECENT_READY_RATE)
                && d.maxLoseStreak <= AppConfig.DYNAMIC_CLUSTER_MAX_LOSE_READY
                && d.reliability >= AppConfig.DYNAMIC_CLUSTER_MIN_CONFIDENCE_READY) {
            d.promotionStatus = STATUS_WATCH;
            d.clusterStatus = AppConfig.CLUSTER_STATUS_READY;
            d.promotionAction = AppConfig.ADAPTIVE_ACTION_READY;
            if (report != null) report.dynamicReadyCount++;
            d.reason = "动态域达到 READY 观察线，但仍需 AdaptiveJudge 与 DecisionGate 复核。";
            d.advice = "标记 READY/WATCH 证据，只参与排序和审判，不直接 BET。";
            return true;
        }
        d.promotionStatus = STATUS_NO_PROMOTE;
        d.reason = "动态域未达到稳定高于随机基准的晋级条件。";
        d.advice = "继续 SHADOW 观察；禁止因单一高分放宽。";
        return true;
    }

    private boolean isDynamicPromotionDomain(String type) {
        String t = safe(type);
        return DOMAIN_DYNAMIC_CLUSTER.equals(t)
                || DOMAIN_NEED_SUPPLY.equals(t)
                || DOMAIN_COUNTERFACTUAL.equals(t)
                || DOMAIN_THIRD_QUALITY.equals(t)
                || DOMAIN_ADAPTIVE.equals(t)
                || DOMAIN_CAUSAL_COMBO.equals(t);
    }

    private void finalizeRecentStats(ShadowDomainStats d) {
        if (d == null) return;
        d.recent30Rate =
                d.recent30Total <= 0 ? 0.0d : ((double) d.recent30Hit) / ((double) d.recent30Total);
        d.recent60Rate =
                d.recent60Total <= 0 ? 0.0d : ((double) d.recent60Hit) / ((double) d.recent60Total);
        d.recent120Rate =
                d.recent120Total <= 0
                        ? 0.0d
                        : ((double) d.recent120Hit) / ((double) d.recent120Total);
        d.counterRiskAvg = d.total <= 0 ? 0.0d : d.sumCounterRisk / (double) d.total;
    }

    private String decidePromotionClusterStatus(ShadowDomainStats d) {
        if (d == null) return AppConfig.CLUSTER_STATUS_UNKNOWN;
        return AppConfig.decideDynamicClusterStatus(
                d.total,
                d.recent60Total,
                d.wouldHitRate,
                d.recent60Rate,
                d.maxLoseStreak,
                d.reliability,
                0.0d,
                d.counterRiskAvg);
    }

    private String decidePromotionAction(ShadowDomainStats d) {
        String status = d == null ? AppConfig.CLUSTER_STATUS_UNKNOWN : d.clusterStatus;
        if (AppConfig.CLUSTER_STATUS_STRONG.equals(status)) return AppConfig.ADAPTIVE_ACTION_STRONG;
        if (AppConfig.CLUSTER_STATUS_READY.equals(status)) return AppConfig.ADAPTIVE_ACTION_READY;
        if (AppConfig.isDynamicClusterStatusRisk(status)) return AppConfig.ADAPTIVE_ACTION_FREEZE;
        return AppConfig.ADAPTIVE_ACTION_WATCH;
    }

    private String buildDynamicPromotionReason(ShadowDomainStats d) {
        if (d == null) return "";
        return "动态晋级域｜type="
                + d.domainType
                + "｜key="
                + d.domainKey
                + "｜status="
                + d.clusterStatus
                + "｜action="
                + d.promotionAction
                + "｜sample="
                + d.total
                + "｜hit="
                + pct(d.wouldHitRate)
                + "｜recent30="
                + pct(d.recent30Rate)
                + "｜recent60="
                + pct(d.recent60Rate)
                + "｜maxLose="
                + d.maxLoseStreak
                + "｜counter="
                + pct(d.counterRiskAvg)
                + "｜reliability="
                + pct(d.reliability);
    }

    private void appendDynamicDomainSection(StringBuilder sb, ShadowPromotionReport r) {
        if (sb == null || r == null || r.allDomains == null) return;
        int shown = 0;
        for (int i = 0; i < r.allDomains.size(); i++) {
            ShadowDomainStats d = r.allDomains.get(i);
            if (d == null || !isDynamicPromotionDomain(d.domainType)) continue;
            shown++;
            sb.append(shown)
                    .append(". ")
                    .append(d.fullText())
                    .append("，clusterStatus=")
                    .append(d.clusterStatus)
                    .append("，action=")
                    .append(d.promotionAction)
                    .append("，recent30=")
                    .append(pct(d.recent30Rate))
                    .append("，recent60=")
                    .append(pct(d.recent60Rate))
                    .append("，maxLose=")
                    .append(d.maxLoseStreak)
                    .append("\n");
            if (shown >= 20) break;
        }
        if (shown == 0) sb.append("暂无动态域样本。\n");
    }

    private void saveDynamicClusterPromotionSnapshots(DbStore store, ShadowPromotionReport report) {
        if (store == null || report == null) return;
        try {
            savePromotionDomainList(store, report.promotedDomains, "PROMOTED");
            savePromotionDomainList(store, report.watchDomains, "WATCH");
            savePromotionDomainList(store, report.blockedDomains, "BLOCKED");
            setStateSafe(
                    store,
                    AppConfig.STATE_DYNAMIC_CLUSTER_SUMMARY,
                    "ShadowPromotion动态域：PROMOTE="
                            + report.promoteCount
                            + " WATCH="
                            + report.watchCount
                            + " BLOCK="
                            + report.blockCount);
            setStateSafe(
                    store,
                    AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS,
                    "ShadowPromotionVerifierEngine｜" + safe(report.summary));
        } catch (Throwable e) {
        }
    }

    private void savePromotionDomainList(
            DbStore store, ArrayList<ShadowDomainStats> list, String group) {
        if (store == null || list == null) return;
        int max = Math.min(list.size(), 30);
        for (int i = 0; i < max; i++) {
            ShadowDomainStats d = list.get(i);
            if (d == null || !isDynamicPromotionDomain(d.domainType)) continue;
            Models.PromotionDomainState p = new Models.PromotionDomainState();
            p.domainKey = d.domainKey;
            p.clusterId = extractClusterIdFromDomain(d);
            p.status =
                    d.clusterStatus == null || d.clusterStatus.length() == 0
                            ? AppConfig.CLUSTER_STATUS_WATCH
                            : d.clusterStatus;
            p.sampleCount = d.total;
            p.hitCount = d.wouldHit;
            p.recentSample = d.recent60Total;
            p.recentHitCount = d.recent60Hit;
            p.maxLoseStreak = d.maxLoseStreak;
            p.confidence = d.reliability;
            p.action =
                    d.promotionAction == null || d.promotionAction.length() == 0
                            ? AppConfig.ADAPTIVE_ACTION_WATCH
                            : d.promotionAction;
            p.reason = group + "｜" + buildDynamicPromotionReason(d);
            try {
                p.recalc();
                store.savePromotionDomainSnapshot(p);
            } catch (Throwable e) {
            }
        }
    }

    private String extractClusterIdFromDomain(ShadowDomainStats d) {
        if (d == null) return "";
        if (DOMAIN_DYNAMIC_CLUSTER.equals(d.domainType)) {
            String v = safe(d.domainValue);
            if (v.length() > 0 && !"空".equals(v)) return v;
        }
        return AppConfig.buildDynamicClusterId(safe(d.domainKey));
    }

    private String causalComboKey(ShadowCase c) {
        if (c == null) return "CAUSAL:空";
        return "P"
                + c.position
                + "|CL="
                + shortBucket(c.dynamicClusterBucket)
                + "|NS="
                + shortBucket(c.needSupplyBucket)
                + "|CF="
                + shortBucket(c.counterfactualBucket)
                + "|TQ="
                + shortBucket(c.thirdQualityBucket)
                + "|AD="
                + shortBucket(c.adaptiveBucket);
    }

    private String needSupplyBucket(double score, double risk) {
        if (score >= AppConfig.NEED_SUPPLY_STRONG_MATCH_SCORE
                && risk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK) return "NS_STRONG";
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

    private String thirdQualityBucket(double thirdQuality) {
        if (thirdQuality >= AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY + 0.100d)
            return "THIRD_STRONG";
        if (thirdQuality >= AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY) return "THIRD_READY";
        if (thirdQuality >= AppConfig.NEED_SUPPLY_MIN_THIRD_QUALITY) return "THIRD_WATCH";
        return "THIRD_DRAG";
    }

    private String adaptiveBucket(String action, String status) {
        String a = upper(action);
        String s = upper(status);
        if (a.indexOf("STRONG") >= 0 || s.indexOf("STRONG") >= 0) return "ADAPTIVE_STRONG";
        if (a.indexOf("READY") >= 0 || s.indexOf("READY") >= 0 || a.indexOf("PROMOTE") >= 0)
            return "ADAPTIVE_READY";
        if (a.indexOf("FREEZE") >= 0 || s.indexOf("FREEZE") >= 0 || a.indexOf("RISK") >= 0)
            return "ADAPTIVE_FREEZE";
        if (a.indexOf("DOWN") >= 0) return "ADAPTIVE_DOWNGRADE";
        return "ADAPTIVE_WATCH";
    }

    private double clamp01(double v) {
        return AppConfig.clampDynamicCausalScore(v);
    }

    private void appendDomainList(StringBuilder sb, ArrayList<ShadowDomainStats> list, int max) {
        if (sb == null) return;
        if (list == null || list.size() == 0) {
            sb.append("暂无\n");
            return;
        }
        int count = Math.min(list.size(), max);
        for (int i = 0; i < count; i++) {
            ShadowDomainStats d = list.get(i);
            if (d == null) continue;
            sb.append(i + 1).append(". ").append(d.fullText()).append("\n");
            if (d.reason != null && d.reason.length() > 0) {
                sb.append("   原因：").append(limit(d.reason, 220)).append("\n");
            }
            if (d.advice != null && d.advice.length() > 0) {
                sb.append("   建议：").append(limit(d.advice, 240)).append("\n");
            }
        }
    }

    private String buildShortSummary(ShadowPromotionReport r) {
        if (r == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("SHADOW晋级：样本=")
                .append(r.settledShadow)
                .append(" wouldHit=")
                .append(r.wouldHit)
                .append(" 胜率=")
                .append(pct(r.wouldHitRate))
                .append("；PROMOTE=")
                .append(r.promoteCount)
                .append(" WATCH=")
                .append(r.watchCount)
                .append(" BLOCK=")
                .append(r.blockCount);
        if (r.bestDomain != null) {
            sb.append("；最佳=")
                    .append(r.bestDomain.domainKey)
                    .append("/")
                    .append(pct(r.bestDomain.wouldHitRate));
        }
        if (r.mainSignal != null && r.mainSignal.length() > 0) {
            sb.append("；信号=").append(limit(r.mainSignal, 260));
        }
        return limit(sb.toString(), 720);
    }

    private String buildCompactJson(ShadowPromotionReport r) {
        if (r == null) return "{}";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"version\":\"").append(jsonSafe(r.version)).append("\",");
        sb.append("\"totalShadow\":").append(r.totalShadow).append(",");
        sb.append("\"settledShadow\":").append(r.settledShadow).append(",");
        sb.append("\"wouldHitRate\":").append(r.wouldHitRate).append(",");
        sb.append("\"realBetRate\":").append(r.realBetRate).append(",");
        sb.append("\"promoteCount\":").append(r.promoteCount).append(",");
        sb.append("\"watchCount\":").append(r.watchCount).append(",");
        sb.append("\"blockCount\":").append(r.blockCount).append(",");
        sb.append("\"bestDomain\":\"")
                .append(jsonSafe(r.bestDomain == null ? "" : r.bestDomain.domainKey))
                .append("\",");
        sb.append("\"mainSignal\":\"").append(jsonSafe(limit(r.mainSignal, 360))).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private void addShadowDomain(
            ShadowPromotionReport report, String type, String key, ShadowCase c, String note) {
        if (report == null || c == null) return;
        String domainKey = buildDomainKey(type, key);
        ShadowDomainStats d = report.domainMap.get(domainKey);
        if (d == null) {
            d = new ShadowDomainStats();
            d.domainType = safe(type);
            d.domainKey = domainKey;
            d.domainValue = normalizeBucket(key);
            d.note = safe(note);
            report.domainMap.put(domainKey, d);
        }
        d.add(c);
        if (d.sampleCases.size() < MAX_CASES_IN_DOMAIN) {
            d.sampleCases.add(copyCaseBrief(c));
        }
    }

    private void addRealBetDomain(
            ShadowPromotionReport report, String type, String key, ShadowCase c) {
        if (report == null || c == null) return;
        String domainKey = buildDomainKey(type, key);
        RealBetDomainStats d = report.realBetDomainMap.get(domainKey);
        if (d == null) {
            d = new RealBetDomainStats();
            d.domainType = safe(type);
            d.domainKey = domainKey;
            d.domainValue = normalizeBucket(key);
            report.realBetDomainMap.put(domainKey, d);
        }
        d.add(c);
    }

    private String buildDomainKey(String type, String key) {
        return normalizeBucket(type) + ":" + normalizeBucket(key);
    }

    private boolean isUnsafeOutcomeDomain(String type, String key, String value) {
        String t = safe(type).toUpperCase(java.util.Locale.ROOT);
        if (DOMAIN_K3_LAYER.equals(t)) return true;
        return AppConfig.isUnsafePromotionDomainKey(key)
                || AppConfig.isUnsafePromotionDomainKey(value);
    }

    private ShadowCaseBrief copyCaseBrief(ShadowCase c) {
        ShadowCaseBrief b = new ShadowCaseBrief();
        if (c == null) return b;
        b.kind = c.kind;
        b.targetIssue = c.targetIssue;
        b.position = c.position;
        b.n1 = c.n1;
        b.n2 = c.n2;
        b.n3 = c.n3;
        b.actualNumber = c.actualNumber;
        b.hit = c.hit;
        b.result = c.result;
        b.finalP = c.finalP;
        b.dynamicGate = c.dynamicGate;
        b.edge = c.edge;
        b.teacherStatus = c.teacherBucket;
        b.v20Status = c.v20Bucket;
        b.windowStateKey = c.windowBucket;
        b.dynamicClusterStatus = c.dynamicClusterStatus;
        b.needSupplyMatchScore = c.needSupplyMatchScore;
        b.counterfactualWhyNotRisk = c.counterfactualWhyNotRisk;
        b.thirdQuality = c.needSupplyThirdQuality;
        b.adaptiveAction = c.adaptiveJudgeAction;
        b.hint = c.promotionHint;
        return b;
    }

    private boolean isBetterDomain(ShadowDomainStats a, ShadowDomainStats b) {
        if (a == null) return false;
        if (b == null) return true;
        int sa = statusRank(a.promotionStatus);
        int sb = statusRank(b.promotionStatus);
        if (sa != sb) return sa > sb;
        if (a.wouldHitRate != b.wouldHitRate) return a.wouldHitRate > b.wouldHitRate;
        if (a.overRealBetEdge != b.overRealBetEdge) return a.overRealBetEdge > b.overRealBetEdge;
        return a.total > b.total;
    }

    private boolean isWorseDomain(ShadowDomainStats a, ShadowDomainStats b) {
        if (a == null) return false;
        if (b == null) return true;
        int sa = statusRiskRank(a.promotionStatus);
        int sb = statusRiskRank(b.promotionStatus);
        if (sa != sb) return sa > sb;
        if (a.wouldHitRate != b.wouldHitRate) return a.wouldHitRate < b.wouldHitRate;
        return a.total > b.total;
    }

    private int statusRank(String status) {
        if (STATUS_STRONG.equals(status)) return 7;
        if (STATUS_READY.equals(status)) return 6;
        if (STATUS_PROMOTE_WATCH.equals(status)) return 5;
        if (STATUS_WATCH.equals(status)) return 3;
        if (STATUS_NO_PROMOTE.equals(status)) return 2;
        if (STATUS_INSUFFICIENT.equals(status)) return 1;
        if (STATUS_BLOCK.equals(status)) return 0;
        return 1;
    }

    private int statusRiskRank(String status) {
        if (STATUS_FREEZE.equals(status)) return 6;
        if (STATUS_BLOCK.equals(status)) return 5;
        if (STATUS_NO_PROMOTE.equals(status)) return 3;
        if (STATUS_INSUFFICIENT.equals(status)) return 2;
        if (STATUS_WATCH.equals(status)) return 1;
        if (STATUS_PROMOTE_WATCH.equals(status)) return 0;
        return 1;
    }

    private void sortDomains(ArrayList<ShadowDomainStats> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<ShadowDomainStats>() {
                    public int compare(ShadowDomainStats a, ShadowDomainStats b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        int sa = statusRank(a.promotionStatus);
                        int sb = statusRank(b.promotionStatus);
                        if (sa != sb) return sb - sa;
                        if (a.total != b.total) return b.total - a.total;
                        if (a.wouldHitRate < b.wouldHitRate) return 1;
                        if (a.wouldHitRate > b.wouldHitRate) return -1;
                        if (a.overRealBetEdge < b.overRealBetEdge) return 1;
                        if (a.overRealBetEdge > b.overRealBetEdge) return -1;
                        return safe(a.domainKey).compareTo(safe(b.domainKey));
                    }
                });
    }

    private String comboKey(ShadowCase c) {
        if (c == null) return "COMBO:空";
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(c.position);
        sb.append("|").append(c.finalPBucket);
        sb.append("|").append(shortBucket(c.teacherBucket));
        sb.append("|").append(shortBucket(c.v20Bucket));
        sb.append("|").append(shortBucket(c.windowBucket));
        sb.append("|").append(c.edgeBucket);
        return sb.toString();
    }

    private String shortBucket(String s) {
        s = normalizeBucket(s);
        if (s.length() <= 26) return s;
        return s.substring(0, 26);
    }

    private String k3LayerBucket(ShadowCase c) {
        if (c == null) return "K3空";
        if (c.actualNumber == c.n1) return "RANK1_HIT";
        if (c.actualNumber == c.n2) return "RANK2_HIT";
        if (c.actualNumber == c.n3) return "RANK3_HIT";
        if (c.actualNumber > 0) return "K3_MISS";
        return "K3_WAIT";
    }

    private void buildShadowCaseHint(ShadowCase c) {
        if (c == null) return;
        String hint = "";
        if (c.hit == 1) {
            hint = append(hint, "SHADOW若BET会命中");
        } else {
            hint = append(hint, "SHADOW若BET仍未中");
        }
        if (c.finalP >= 0.400d) {
            hint = append(hint, "FinalP高");
        } else if (c.finalP >= 0.380d) {
            hint = append(hint, "FinalP可观察");
        }
        if (c.edge >= 0.010d) {
            hint = append(hint, "Edge有效");
        }
        if (isTeacherStrong(c)) {
            hint = append(hint, "Teacher强/READY");
        } else if (isTeacherRisk(c)) {
            hint = append(hint, "Teacher风险");
        }
        if (hasV20Support(c)) {
            hint = append(hint, "V20支持");
        } else if (hasV20Risk(c)) {
            hint = append(hint, "V20反证");
        }
        if (hasTrustedWindow(c)) {
            hint = append(hint, "窗口可信");
        }
        if (c.dynamicClusterId.length() > 0 && !"空".equals(c.dynamicClusterBucket)) {
            hint = append(hint, "动态簇=" + c.dynamicClusterBucket);
        }
        if (c.needSupplyMatchScore >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE) {
            hint = append(hint, "NeedSupply可观察");
        }
        if (c.counterfactualWhyNotRisk >= AppConfig.COUNTERFACTUAL_MAX_WHY_NOT_RISK) {
            hint = append(hint, "反事实反证偏强");
        }
        if (c.needSupplyThirdQuality < AppConfig.NEED_SUPPLY_MIN_THIRD_QUALITY) {
            hint = append(hint, "第三号拖拽");
        }
        c.promotionHint = limit(hint, 360);
    }

    private ArrayList<ShadowCase> loadRecentShadowCases(DbStore store, int limit) {
        ArrayList<ShadowCase> out = new ArrayList<ShadowCase>();
        if (store == null) return out;

        ArrayList<Models.Decision> decisions =
                loadRecentDecisions(store, DEFAULT_DECISION_SCAN_LIMIT);
        HashMap<String, String> used = new HashMap<String, String>();

        for (int i = 0; i < decisions.size(); i++) {
            if (out.size() >= limit) break;
            Models.Decision d = decisions.get(i);
            if (d == null || !isShadowDecision(d)) continue;
            String issue = safe(getString(d, "targetIssue", ""));
            if (issue.length() == 0 || used.containsKey(issue)) continue;
            ShadowCase c = fromShadowDecision(store, d);
            if (c != null) {
                out.add(c);
                used.put(issue, "1");
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<ShadowCase>(out.subList(0, limit));
        }
        return out;
    }

    private ArrayList<ShadowCase> loadRecentRealBetCases(DbStore store, int limit) {
        ArrayList<ShadowCase> out = new ArrayList<ShadowCase>();
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
            ShadowCase c = fromTrade(store, t, d);
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
                ShadowCase c = fromTrade(store, t, d);
                if (c != null) {
                    out.add(c);
                    used.put(issue, "1");
                }
            }
        }

        sortCasesDesc(out);
        if (out.size() > limit) {
            return new ArrayList<ShadowCase>(out.subList(0, limit));
        }
        return out;
    }

    private ShadowCase fromShadowDecision(DbStore store, Models.Decision d) {
        if (d == null) return null;
        ShadowCase c = new ShadowCase();
        c.kind = "SHADOW";
        c.targetIssue = safe(getString(d, "targetIssue", ""));
        c.baseIssue = safe(getString(d, "baseIssue", ""));
        c.createdAt = getLong(d, "createdAt", 0L);
        c.position = getInt(d, "position", 0);
        c.n1 = getInt(d, "n1", 0);
        c.n2 = getInt(d, "n2", 0);
        c.n3 = getInt(d, "n3", 0);
        c.finalP = getDouble(d, "finalP", 0.0d);
        c.rawP = getDouble(d, "rawP", 0.0d);
        c.calibratedP = getDouble(d, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
        c.edge = getDouble(d, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = getInt(d, "effectiveSample", 0);
        c.comboSample = getInt(d, "comboSample", 0);
        c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        c.stateMode = getString(d, "stateMode", "");
        c.reasonSummary = getString(d, "reasonSummary", "");
        copyOptionalFields(c, d);

        Models.Draw draw = getDrawByIssueSafe(store, c.targetIssue);
        if (draw != null && isDrawValid(draw) && isValidPosition(c.position)) {
            int n = getNumberAtPositionSafe(draw, c.position);
            if (isValidNumber(n)) {
                c.actualNumber = n;
                c.settled = 1;
                c.hit = containsK3(c, n) ? 1 : 0;
                c.result = c.hit == 1 ? resultWin() : resultMiss();
            } else {
                c.settled = 0;
                c.result = resultWait();
            }
        } else {
            c.settled = 0;
            c.result = resultWait();
        }

        normalizeCaseBuckets(c);
        return c;
    }

    private ShadowCase fromTrade(DbStore store, Models.Trade t, Models.Decision d) {
        if (t == null) return null;

        ShadowCase c = new ShadowCase();
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
        c.pnl = getDouble(t, "pnl", 0.0d);
        c.missReason = getString(t, "missReason", "");
        c.finalP = getDouble(t, "finalP", 0.0d);
        c.rawP = getDouble(t, "rawP", 0.0d);
        c.calibratedP = getDouble(t, "calibratedP", 0.0d);
        c.dynamicGate = getDouble(t, "dynamicGate", 0.0d);
        c.edge = getDouble(t, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d) {
            c.edge = c.finalP - c.dynamicGate;
        }
        c.effectiveSample = getInt(t, "effectiveSample", 0);
        c.comboSample = getInt(t, "comboSample", 0);
        c.similarHitRate = getDouble(t, "similarHitRate", 0.0d);
        c.comboHitRate = getDouble(t, "comboHitRate", 0.0d);
        c.stateMode = getString(t, "stateMode", "");
        c.reasonSummary = getString(t, "reasonSummary", "");
        copyOptionalFields(c, t);

        if (d != null) {
            mergeDecisionFields(c, d);
        }

        fillActualFromDraw(store, c);
        normalizeCaseBuckets(c);
        return c;
    }

    private void fillActualFromDraw(DbStore store, ShadowCase c) {
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

    private void normalizeCaseBuckets(ShadowCase c) {
        if (c == null) return;
        c.finalPBucket = finalPBucket(c.finalP);
        c.edgeBucket = edgeBucket(c.edge);
        c.gateBucket = gateBucket(c);
        c.teacherBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.teacherStatus, c.teacherMatrixStatus, c.teacherNumberStatus));
        c.v20Bucket =
                normalizeBucket(
                        firstNonEmpty(c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        c.windowBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.windowStateKey,
                                c.decisionWindowStateKey,
                                c.dynamicWindowStateKey,
                                c.probabilityWindowStateKey));
        c.sampleBucket = sampleBucket(c);
        c.recentBucket = recentBucket(c);
        c.dynamicClusterBucket =
                normalizeBucket(
                        firstNonEmpty(
                                c.dynamicClusterId,
                                c.dynamicClusterStatus,
                                c.dynamicClusterPrototypeKey));
        c.needSupplyBucket = needSupplyBucket(c.needSupplyMatchScore, c.needSupplyRisk);
        c.counterfactualBucket =
                counterfactualBucket(
                        c.counterfactualWhyNotRisk,
                        c.causeAblationScore,
                        c.causeAblationFalseCauseCount);
        c.thirdQualityBucket = thirdQualityBucket(c.needSupplyThirdQuality);
        c.adaptiveBucket = adaptiveBucket(c.adaptiveJudgeAction, c.adaptiveJudgeStatus);
    }

    private void mergeDecisionFields(ShadowCase c, Models.Decision d) {
        if (c == null || d == null) return;
        if (c.reasonSummary.length() == 0) c.reasonSummary = getString(d, "reasonSummary", "");
        if (c.finalP <= 0.0d) c.finalP = getDouble(d, "finalP", 0.0d);
        if (c.rawP <= 0.0d) c.rawP = getDouble(d, "rawP", 0.0d);
        if (c.calibratedP <= 0.0d) c.calibratedP = getDouble(d, "calibratedP", 0.0d);
        if (c.dynamicGate <= 0.0d) c.dynamicGate = getDouble(d, "dynamicGate", 0.0d);
        if (c.edge == 0.0d) c.edge = getDouble(d, "edge", 0.0d);
        if (c.edge == 0.0d && c.finalP > 0.0d && c.dynamicGate > 0.0d)
            c.edge = c.finalP - c.dynamicGate;
        if (c.effectiveSample <= 0) c.effectiveSample = getInt(d, "effectiveSample", 0);
        if (c.comboSample <= 0) c.comboSample = getInt(d, "comboSample", 0);
        if (c.similarHitRate <= 0.0d) c.similarHitRate = getDouble(d, "similarHitRate", 0.0d);
        if (c.comboHitRate <= 0.0d) c.comboHitRate = getDouble(d, "comboHitRate", 0.0d);
        if (c.stateMode.length() == 0) c.stateMode = getString(d, "stateMode", "");
        copyOptionalFields(c, d);
    }

    private void copyOptionalFields(ShadowCase c, Object source) {
        if (c == null || source == null) return;

        c.teacherMatrixStatus = getString(source, "teacherMatrixStatus", c.teacherMatrixStatus);
        c.teacherNumberStatus = getString(source, "teacherNumberStatus", c.teacherNumberStatus);
        c.teacherStatus = getString(source, "teacherStatus", c.teacherStatus);
        c.teacherMatrixSummary = getString(source, "teacherMatrixSummary", c.teacherMatrixSummary);
        c.teacherMatrixSample = getInt(source, "teacherMatrixSample", c.teacherMatrixSample);
        c.teacherMatrixHitRate = getDouble(source, "teacherMatrixHitRate", c.teacherMatrixHitRate);
        c.teacherMatrixConfidence =
                getDouble(source, "teacherMatrixConfidence", c.teacherMatrixConfidence);
        c.teacherMatrixRisk = getDouble(source, "teacherMatrixRisk", c.teacherMatrixRisk);
        c.teacherEvidenceContribution =
                firstNonZero(
                        getDouble(source, "teacherEvidenceContribution", 0.0d),
                        getDouble(source, "teacherMatrixEvidenceContribution", 0.0d),
                        c.teacherEvidenceContribution);
        c.teacherCounterContribution =
                firstNonZero(
                        getDouble(source, "teacherCounterContribution", 0.0d),
                        getDouble(source, "teacherMatrixCounterContribution", 0.0d),
                        c.teacherCounterContribution);

        c.windowStateKey =
                firstNonEmpty(
                        getString(source, "windowStateKey", ""),
                        getString(source, "finalDecisionWindowStateKey", ""),
                        getString(source, "teacherWindowStateKey", ""),
                        c.windowStateKey);
        c.dynamicWindowStateKey =
                getString(source, "dynamicWindowStateKey", c.dynamicWindowStateKey);
        c.probabilityWindowStateKey =
                getString(source, "probabilityWindowStateKey", c.probabilityWindowStateKey);
        c.decisionWindowStateKey =
                getString(source, "decisionWindowStateKey", c.decisionWindowStateKey);

        c.dynamicStatus =
                firstNonEmpty(
                        getString(source, "dynamicStatus", ""),
                        getString(source, "dynamicCauseStatus", ""),
                        c.dynamicStatus);
        c.v20Status =
                firstNonEmpty(
                        getString(source, "v20Status", ""),
                        getString(source, "v20CauseStatus", ""),
                        c.v20Status);
        c.v20SupportScore =
                firstNonZero(
                        getDouble(source, "v20SupportScore", 0.0d),
                        getDouble(source, "dynamicSupportScore", 0.0d),
                        c.v20SupportScore);
        c.v20RiskScore =
                firstNonZero(
                        getDouble(source, "v20RiskScore", 0.0d),
                        getDouble(source, "dynamicRiskScore", 0.0d),
                        c.v20RiskScore);

        c.evidenceContribution =
                firstNonZero(
                        getDouble(source, "decisionEvidenceContribution", 0.0d),
                        getDouble(source, "dynamicGateEvidenceContribution", 0.0d),
                        getDouble(source, "probabilityEvidenceContribution", 0.0d),
                        getDouble(source, "evidenceContribution", c.evidenceContribution));
        c.counterContribution =
                firstNonZero(
                        getDouble(source, "decisionCounterContribution", 0.0d),
                        getDouble(source, "dynamicGateCounterContribution", 0.0d),
                        getDouble(source, "probabilityCounterEvidence", 0.0d),
                        getDouble(source, "counterContribution", c.counterContribution));
        c.sampleReliability =
                firstNonZero(
                        getDouble(source, "decisionSampleReliability", 0.0d),
                        getDouble(source, "dynamicGateSampleReliability", 0.0d),
                        getDouble(source, "probabilitySampleReliability", 0.0d),
                        getDouble(source, "sampleReliability", c.sampleReliability));
        c.recentValidity =
                firstNonZero(
                        getDouble(source, "decisionRecentValidity", 0.0d),
                        getDouble(source, "dynamicGateRecentValidity", 0.0d),
                        getDouble(source, "probabilityRecentValidity", 0.0d),
                        getDouble(source, "recentValidity", c.recentValidity));
        c.featureDistance =
                firstNonZero(
                        getDouble(source, "decisionFeatureDistance", 0.0d),
                        getDouble(source, "dynamicGateFeatureDistance", 0.0d),
                        getDouble(source, "probabilityFeatureDistance", 0.0d),
                        getDouble(source, "featureDistance", c.featureDistance));

        c.similarWindowSample =
                firstNonZeroInt(
                        getInt(source, "similarWindowSample", 0),
                        getInt(source, "teacherSimilarWindowSample", 0),
                        c.similarWindowSample);
        c.similarWindowHitRate =
                firstNonZero(
                        getDouble(source, "similarWindowHitRate", 0.0d),
                        getDouble(source, "teacherSimilarWindowHitRate", 0.0d),
                        c.similarWindowHitRate);

        c.decisionActionAdvice =
                firstNonEmpty(
                        getString(source, "decisionActionAdvice", ""),
                        getString(source, "dynamicGateActionAdvice", ""),
                        getString(source, "probabilityActionAdvice", ""),
                        getString(source, "actionAdvice", c.decisionActionAdvice));
        c.decisionHardRisk = getInt(source, "decisionHardRisk", c.decisionHardRisk);
        c.decisionCounterRiskHigh =
                getInt(source, "decisionCounterRiskHigh", c.decisionCounterRiskHigh);
        c.decisionEvidenceStrong =
                getInt(source, "decisionEvidenceStrong", c.decisionEvidenceStrong);
        c.decisionEvidenceElite = getInt(source, "decisionEvidenceElite", c.decisionEvidenceElite);
        c.dynamicClusterId = getString(source, "dynamicClusterId", c.dynamicClusterId);
        c.dynamicClusterStatus = getString(source, "dynamicClusterStatus", c.dynamicClusterStatus);
        c.dynamicClusterPrototypeKey =
                getString(source, "dynamicClusterPrototypeKey", c.dynamicClusterPrototypeKey);
        c.dynamicClusterFeatureKey =
                getString(source, "dynamicClusterFeatureKey", c.dynamicClusterFeatureKey);
        c.dynamicClusterSample = getInt(source, "dynamicClusterSample", c.dynamicClusterSample);
        c.dynamicClusterHitRate =
                getDouble(source, "dynamicClusterHitRate", c.dynamicClusterHitRate);
        c.dynamicClusterRecentHitRate =
                getDouble(source, "dynamicClusterRecentHitRate", c.dynamicClusterRecentHitRate);
        c.dynamicClusterConfidence =
                getDouble(source, "dynamicClusterConfidence", c.dynamicClusterConfidence);
        c.dynamicClusterDistance =
                getDouble(source, "dynamicClusterDistance", c.dynamicClusterDistance);
        c.dynamicClusterVolatility =
                getDouble(source, "dynamicClusterVolatility", c.dynamicClusterVolatility);
        c.dynamicClusterMaxLose = getInt(source, "dynamicClusterMaxLose", c.dynamicClusterMaxLose);
        c.dynamicClusterCounterScore =
                getDouble(source, "dynamicClusterCounterScore", c.dynamicClusterCounterScore);
        c.positionNeedSummary = getString(source, "positionNeedSummary", c.positionNeedSummary);
        c.positionNeedStrength = getDouble(source, "positionNeedStrength", c.positionNeedStrength);
        c.positionNeedChaosRisk =
                getDouble(source, "positionNeedChaosRisk", c.positionNeedChaosRisk);
        c.numberSupplySummary = getString(source, "numberSupplySummary", c.numberSupplySummary);
        c.numberSupplyStrength = getDouble(source, "numberSupplyStrength", c.numberSupplyStrength);
        c.numberSupplyRisk = getDouble(source, "numberSupplyRisk", c.numberSupplyRisk);
        c.needSupplyMatchScore = getDouble(source, "needSupplyMatchScore", c.needSupplyMatchScore);
        c.needSupplyThirdQuality =
                getDouble(source, "needSupplyThirdQuality", c.needSupplyThirdQuality);
        c.needSupplyRoleDiversity =
                getDouble(source, "needSupplyRoleDiversity", c.needSupplyRoleDiversity);
        c.needSupplyRisk = getDouble(source, "needSupplyRisk", c.needSupplyRisk);
        c.needSupplySummary = getString(source, "needSupplySummary", c.needSupplySummary);
        c.counterfactualRank = getInt(source, "counterfactualRank", c.counterfactualRank);
        c.counterfactualRankScore =
                getDouble(source, "counterfactualRankScore", c.counterfactualRankScore);
        c.counterfactualWhyNotRisk =
                getDouble(source, "counterfactualWhyNotRisk", c.counterfactualWhyNotRisk);
        c.counterfactualWhyNot = getString(source, "counterfactualWhyNot", c.counterfactualWhyNot);
        c.causeAblationScore = getDouble(source, "causeAblationScore", c.causeAblationScore);
        c.causeAblationFalseCauseCount =
                getInt(source, "causeAblationFalseCauseCount", c.causeAblationFalseCauseCount);
        c.adaptiveJudgeStatus = getString(source, "adaptiveJudgeStatus", c.adaptiveJudgeStatus);
        c.adaptiveJudgeAction = getString(source, "adaptiveJudgeAction", c.adaptiveJudgeAction);
        c.adaptiveJudgeDomain = getString(source, "adaptiveJudgeDomain", c.adaptiveJudgeDomain);
        c.adaptivePromoteScore = getDouble(source, "adaptivePromoteScore", c.adaptivePromoteScore);
        c.adaptiveFreezeScore = getDouble(source, "adaptiveFreezeScore", c.adaptiveFreezeScore);
        c.whyNoBet = getString(source, "whyNoBet", c.whyNoBet);
        c.dynamicCausalSummary = getString(source, "dynamicCausalSummary", c.dynamicCausalSummary);
    }

    private boolean isTeacherStrong(ShadowCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.indexOf("STRONG") >= 0) return true;
        if (s.indexOf("READY") >= 0) return true;
        if (s.indexOf("强") >= 0) return true;
        if (s.indexOf("准备") >= 0) return true;
        if (c.teacherMatrixSample >= 60
                && c.teacherMatrixHitRate >= 0.360d
                && c.teacherMatrixConfidence >= 0.50d) return true;
        if (c.teacherEvidenceContribution > c.teacherCounterContribution + 0.080d
                && c.teacherEvidenceContribution > 0.0d) return true;
        return false;
    }

    private boolean isTeacherRisk(ShadowCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.teacherBucket,
                                c.teacherStatus,
                                c.teacherMatrixStatus,
                                c.teacherNumberStatus));
        if (s.indexOf("RISK") >= 0) return true;
        if (s.indexOf("FREEZE") >= 0) return true;
        if (s.indexOf("风险") >= 0) return true;
        if (s.indexOf("冻结") >= 0) return true;
        if (c.teacherMatrixRisk >= 0.65d) return true;
        if (c.teacherCounterContribution > c.teacherEvidenceContribution + 0.080d
                && c.teacherCounterContribution > 0.0d) return true;
        return false;
    }

    private boolean hasV20Support(ShadowCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.v20Bucket, c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
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
        if (c.decisionEvidenceStrong > 0 || c.decisionEvidenceElite > 0) return true;
        return false;
    }

    private boolean hasV20Risk(ShadowCase c) {
        if (c == null) return false;
        String s =
                upper(
                        firstNonEmpty(
                                c.v20Bucket, c.v20Status, c.dynamicStatus, c.decisionActionAdvice));
        if (s.indexOf("RISK") >= 0
                || s.indexOf("FREEZE") >= 0
                || s.indexOf("风险") >= 0
                || s.indexOf("冻结") >= 0) return true;
        if (c.decisionHardRisk > 0 || c.decisionCounterRiskHigh > 0) return true;
        if (c.counterContribution > c.evidenceContribution + 0.080d && c.counterContribution > 0.0d)
            return true;
        if (c.v20RiskScore > c.v20SupportScore + 0.080d && c.v20RiskScore > 0.0d) return true;
        return false;
    }

    private boolean hasTrustedWindow(ShadowCase c) {
        if (c == null) return false;
        String s =
                firstNonEmpty(
                        c.windowBucket,
                        c.windowStateKey,
                        c.dynamicWindowStateKey,
                        c.probabilityWindowStateKey);
        if (s == null || s.length() == 0 || "空".equals(s)) return false;
        if (c.similarWindowSample >= 30) return true;
        if (c.effectiveSample >= 60) return true;
        if (c.comboSample >= 60) return true;
        if (c.sampleReliability >= 0.60d && c.recentValidity >= 0.45d) return true;
        return false;
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
                    if (o instanceof Models.Decision) {
                        out.add((Models.Decision) o);
                    }
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
                        if (o instanceof Models.Trade) {
                            out.add((Models.Trade) o);
                        }
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
                if (obj instanceof Models.Trade) {
                    return (Models.Trade) obj;
                }
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
                if (obj instanceof Models.Decision) {
                    return (Models.Decision) obj;
                }
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
                if (obj instanceof Models.Draw) {
                    return (Models.Draw) obj;
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private boolean isBetDecision(Models.Decision d) {
        if (d == null) return false;
        return isBetText(getString(d, "decision", "")) || isBetText(getString(d, "action", ""));
    }

    private boolean isShadowDecision(Models.Decision d) {
        if (d == null) return false;
        String s = safe(getString(d, "decision", "")).trim();
        if (constEquals("DECISION_SHADOW", s)) return true;
        if ("SHADOW".equalsIgnoreCase(s) || "影子观察".equals(s)) return true;
        String action = safe(getString(d, "action", "")).trim();
        return "SHADOW".equalsIgnoreCase(action) || "影子观察".equals(action);
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

    private String resultWait() {
        try {
            Field f = AppConfig.class.getField("RESULT_WAIT");
            Object v = f.get(null);
            if (v != null) return String.valueOf(v);
        } catch (Throwable e) {
        }
        return "WAIT";
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
        if (s.length() == 0) return resultWait();
        String u = s.toUpperCase(java.util.Locale.ROOT);
        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(s)) return resultWin();
        if ("MISS".equals(u)
                || "LOSE".equals(u)
                || "LOSS".equals(u)
                || "未中".equals(s)
                || "不中".equals(s)) return resultMiss();
        if ("WAIT".equals(u) || "WAITING".equals(u) || "等待".equals(s) || "等待结算".equals(s))
            return resultWait();
        try {
            Method m = AppConfig.class.getMethod("normalizeResult", new Class[] {String.class});
            Object obj = m.invoke(null, new Object[] {s});
            if (obj != null) return String.valueOf(obj);
        } catch (Throwable e) {
        }
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

    private boolean containsK3(ShadowCase c, int actual) {
        if (c == null) return false;
        return actual == c.n1 || actual == c.n2 || actual == c.n3;
    }

    private void sortCasesDesc(ArrayList<ShadowCase> list) {
        if (list == null) return;
        Collections.sort(
                list,
                new Comparator<ShadowCase>() {
                    public int compare(ShadowCase a, ShadowCase b) {
                        if (a == null && b == null) return 0;
                        if (a == null) return 1;
                        if (b == null) return -1;
                        if (a.createdAt < b.createdAt) return 1;
                        if (a.createdAt > b.createdAt) return -1;
                        return safe(b.targetIssue).compareTo(safe(a.targetIssue));
                    }
                });
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

    private String gateBucket(ShadowCase c) {
        if (c == null) return "Gate空";
        if (c.dynamicGate <= 0.0d || c.finalP <= 0.0d) return "Gate空";
        double diff = c.finalP - c.dynamicGate;
        if (diff >= 0.030d) return "Gate强通过";
        if (diff >= 0.010d) return "Gate有效通过";
        if (diff >= 0.0d) return "Gate薄通过";
        if (diff >= -0.010d) return "Gate附近被挡";
        return "Gate明显未过";
    }

    private String sampleBucket(ShadowCase c) {
        if (c == null) return "Sample空";
        int sample =
                Math.max(
                        Math.max(c.effectiveSample, c.comboSample),
                        Math.max(c.similarWindowSample, c.teacherMatrixSample));
        if (c.sampleReliability >= 0.75d || sample >= 120) return "Sample强";
        if (c.sampleReliability >= 0.60d || sample >= 60) return "Sample可用";
        if (sample >= 30) return "Sample观察";
        return "Sample不足";
    }

    private String recentBucket(ShadowCase c) {
        if (c == null) return "Recent空";
        if (c.recentValidity >= 0.75d) return "Recent强";
        if (c.recentValidity >= 0.60d) return "Recent可用";
        if (c.recentValidity > 0.0d) return "Recent弱";
        return "Recent空";
    }

    private String normalizeBucket(String s) {
        s = safe(s).trim();
        if (s.length() == 0) return "空";
        if (s.length() > 90) s = s.substring(0, 90);
        return s;
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

    private double firstNonZero(double a, double b, double c) {
        if (a != 0.0d) return a;
        if (b != 0.0d) return b;
        return c;
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

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) return a;
        return b == null ? "" : b;
    }

    private static String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        return c == null ? "" : c;
    }

    private static String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) return a;
        if (b != null && b.length() > 0) return b;
        if (c != null && c.length() > 0) return c;
        return d == null ? "" : d;
    }

    private static String append(String a, String b) {
        if (b == null || b.length() == 0) return safe(a);
        if (a == null || a.length() == 0) return b;
        return a + "；" + b;
    }

    private static String upper(String s) {
        return safe(s).trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String limit(String text, int max) {
        if (text == null) return "";
        if (max <= 0 || text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }

    private static String jsonSafe(String s) {
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

    private static String pct(double v) {
        try {
            return UiFormatter.probability(v);
        } catch (Throwable e) {
            return String.valueOf(Math.round(v * 10000.0d) / 100.0d) + "%";
        }
    }

    private static String signedPct(double v) {
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

    private static boolean isWinTextStatic(String r) {
        String s = safe(r).trim();
        String u = s.toUpperCase(java.util.Locale.ROOT);
        return "WIN".equals(u) || "HIT".equals(u) || "命中".equals(s);
    }

    public String buildPreDrawPromotionDomainKey(Object obj) {
        if (obj == null) {
            return "EMPTY";
        }
        String window = readString(obj, "dynamicClusterPrototypeKey", "");
        if (window.length() == 0) window = readString(obj, "windowStateKey", "");
        String need = readString(obj, "needSupplyMatchKey", "");
        String cf = readString(obj, "counterfactualReasonKey", "");
        String pos = readString(obj, "position", "");
        String v20 = readString(obj, "v20Status", "");
        String key = "P" + pos + "|W=" + window + "|N=" + need + "|CF=" + cf + "|V=" + v20;
        return key.replace(' ', '_');
    }

    public ContentValues buildPromotionCacheFromCandidate(
            Models.ComboCandidate c, String status, String summary) {
        ContentValues cv = new ContentValues();
        String key = buildPreDrawPromotionDomainKey(c);
        cv.put("domain_key", key);
        cv.put("domain_type", "PREDRAW_DYNAMIC_DOMAIN");
        cv.put("promotion_status", status == null || status.length() == 0 ? "WATCH" : status);
        cv.put("summary", summary == null ? "" : summary);
        cv.put("version", VERSION);
        cv.put("updated_at", System.currentTimeMillis());
        return cv;
    }

    private String readString(Object o, String field, String def) {
        try {
            java.lang.reflect.Field f = o.getClass().getField(field);
            Object v = f.get(o);
            return v == null ? def : String.valueOf(v);
        } catch (Throwable e) {
            return def;
        }
    }

    private HashMap<String, Models.ShadowDomainStat> loadV2DomainMap(DbStore store) {
        HashMap<String, Models.ShadowDomainStat> map =
                new HashMap<String, Models.ShadowDomainStat>();
        if (store == null) return map;
        try {
            List<Models.ShadowDomainStat> list =
                    store.getShadowPromotionDomainStats("", REALTIME_DOMAIN_LOAD_LIMIT);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    Models.ShadowDomainStat stat = list.get(i);
                    if (stat != null
                            && stat.domainKey != null
                            && stat.domainKey.length() > 0
                            && !AppConfig.isUnsafePromotionDomainKey(stat.domainKey)) {
                        map.put(stat.domainKey, stat);
                    }
                }
            }
        } catch (Throwable e) {
        }
        return map;
    }

    private PromotionCandidateSignal resolveV2PromotionCandidateSignal(
            Object c, HashMap<String, Models.ShadowDomainStat> map) {
        if (c == null || map == null || map.size() == 0) return null;
        String fine = buildV2SettlementDomainKey(c);
        String mid = buildMidPromotionDomain(c);
        String coarse = buildCoarsePromotionDomain(c);
        Models.ShadowDomainStat stat = map.get(fine);
        if (stat == null) stat = map.get(mid);
        if (stat == null) stat = map.get(coarse);
        if (stat == null) return null;
        PromotionCandidateSignal sig = new PromotionCandidateSignal();
        sig.domainKey = stat.domainKey;
        sig.domainType =
                stat.domainKey != null && stat.domainKey.indexOf("COARSE|") == 0
                        ? "COARSE_SETTLED_DOMAIN"
                        : (stat.domainKey != null && stat.domainKey.indexOf("MID|") == 0
                                ? "MID_SETTLED_DOMAIN"
                                : "FINE_SETTLED_DOMAIN");
        sig.domainValue = stat.parentDomainKey;
        sig.rawStatus = normalizeV2PromotionStatus(stat.promotionStatus);
        sig.shadowPromotionStatus = sig.rawStatus;
        sig.promotionSampleCount = Math.max(0, stat.sampleCount);
        sig.promotionHitCount = Math.max(0, stat.hitCount);
        sig.promotionMissCount = Math.max(0, stat.missCount);
        sig.promotionHitRate = clamp01(stat.hitRate);
        sig.promotionRecentHitRate = clamp01(stat.recentHitRate);
        sig.promotionReliability =
                clamp01(
                        Math.min(1.0d, stat.sampleCount / 40.0d)
                                * (1.0d - clamp01(stat.driftScore)));
        sig.promotionRealBetHitRate = 0.0d;
        sig.promotionOverRealBetEdge = stat.posteriorMean - BASE_K3_RATE;
        sig.maxLoseStreak = Math.max(0, stat.misleadingStreak);
        sig.shadowPromoteScore =
                clamp01(
                        stat.posteriorMean * 0.40d
                                + stat.wilsonLowerBound * 0.30d
                                + sig.promotionReliability * 0.20d
                                + Math.max(0.0d, stat.recentHitRate - BASE_K3_RATE) * 0.10d);
        sig.reason =
                "真实已结算晋级域｜sample="
                        + stat.sampleCount
                        + "｜hit="
                        + pct(stat.hitRate)
                        + "｜recent="
                        + pct(stat.recentHitRate)
                        + "｜posterior="
                        + pct(stat.posteriorMean)
                        + "｜wilson="
                        + pct(stat.wilsonLowerBound)
                        + "｜drift="
                        + pct(stat.driftScore);
        sig.fieldChainVersion = LINK_V2_VERSION;
        if (AppConfig.PROMOTION_STRONG.equals(sig.rawStatus)) {
            sig.shadowPromotionStatus = STATUS_STRONG;
            sig.shadowRejectCode = PROMOTION_REJECT_OK;
            sig.shadowActionAdvice = "STRONG域：仅作为DecisionGate合格探针证据";
        } else if (AppConfig.PROMOTION_READY.equals(sig.rawStatus)) {
            sig.shadowPromotionStatus = STATUS_READY;
            sig.shadowRejectCode = PROMOTION_REJECT_OK;
            sig.shadowActionAdvice = "READY域：仅作为DecisionGate合格探针证据";
        } else if (AppConfig.PROMOTION_PROMOTE_WATCH.equals(sig.rawStatus)) {
            sig.shadowPromotionStatus = STATUS_PROMOTE_WATCH;
            sig.shadowRejectCode = PROMOTION_REJECT_OK;
            sig.shadowActionAdvice = "PROMOTE_WATCH域：可进入DecisionGate受控探针审查";
        } else if (AppConfig.PROMOTION_BLOCK.equals(sig.rawStatus)) {
            sig.shadowPromotionStatus = STATUS_BLOCK;
            sig.shadowRejectCode = PROMOTION_REJECT_BLOCKED;
            sig.shadowActionAdvice = "BLOCK域：结构化阻断频率探针";
        } else {
            sig.shadowPromotionStatus = STATUS_WATCH;
            sig.shadowRejectCode = PROMOTION_REJECT_WATCH_ONLY;
            sig.shadowActionAdvice = "WATCH：继续积累SHADOW反事实样本";
        }
        sig.evidenceScore =
                clamp01(
                        sig.shadowPromoteScore * 0.66d
                                + sig.promotionReliability * 0.20d
                                + sig.promotionHitRate * 0.14d);
        sig.counterEvidenceScore =
                clamp01(
                        stat.driftScore * 0.35d
                                + (stat.misleadingStreak >= 4 ? 0.28d : 0.0d)
                                + (AppConfig.PROMOTION_BLOCK.equals(stat.promotionStatus)
                                        ? 0.45d
                                        : 0.0d));
        sig.netEvidenceScore = clampSignedLocal(sig.evidenceScore - sig.counterEvidenceScore);
        return sig;
    }

    private String normalizeV2PromotionStatus(String status) {
        String x = safe(status).toUpperCase(java.util.Locale.ROOT);
        if (AppConfig.PROMOTION_STRONG.equals(x)) return AppConfig.PROMOTION_STRONG;
        if (AppConfig.PROMOTION_READY.equals(x)) return AppConfig.PROMOTION_READY;
        if (AppConfig.PROMOTION_PROMOTE_WATCH.equals(x)) return AppConfig.PROMOTION_PROMOTE_WATCH;
        if (AppConfig.PROMOTION_BLOCK.equals(x)) return AppConfig.PROMOTION_BLOCK;
        if (AppConfig.PROMOTION_WATCH_POSITIVE.equals(x)) return AppConfig.PROMOTION_WATCH_POSITIVE;
        if (AppConfig.PROMOTION_WATCH.equals(x)) return AppConfig.PROMOTION_WATCH;
        return AppConfig.PROMOTION_NO_SAMPLE;
    }

    private String buildV2SettlementDomainKey(Object c) {
        if (c == null || getInt(c, "evidenceDomainFrozen", 0) != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(readString(c, "promotionFineDomain", ""));
    }

    private String buildV2ParentDomainKey(String domain) {
        if (domain == null) return "";
        int idx = domain.lastIndexOf('_');
        return idx > 0 ? domain.substring(0, idx) : domain;
    }

    private String firstNonEmptyLocal(String a, String b) {
        if (a != null && a.length() > 0) return a;
        return b == null ? "" : b;
    }

    private void writePromotionDeferredFields(Object c, int rankNo, String source) {
        if (c == null) return;
        putStringSafe(c, "shadowPromotionStatus", AppConfig.PROMOTION_WATCH);
        putStringSafe(c, "shadowPromotionRawStatus", AppConfig.PROMOTION_NO_SAMPLE);
        putStringSafe(c, "shadowPromotionActionAdvice", "BACKGROUND_RECHECK");
        putStringSafe(
                c,
                "shadowPromotionReason",
                "晋级验证延后｜rank=" + rankNo + "｜实时只审计前" + REALTIME_CANDIDATE_LIMIT + "候选");
        putStringSafe(c, "shadowPromotionVersion", LINK_V2_VERSION);
        putIntSafe(c, "promotionDomainAvailable", 0);
    }

    private PromotionCandidateSignal resolvePromotionCandidateSignal(
            Object c, ShadowPromotionReport report) {
        PromotionCandidateSignal sig = new PromotionCandidateSignal();
        sig.shadowPromotionStatus = STATUS_INSUFFICIENT;
        sig.rawStatus = STATUS_INSUFFICIENT;
        sig.shadowRejectCode = PROMOTION_REJECT_SAMPLE_WEAK;
        sig.shadowActionAdvice = "SHADOW样本不足：继续反事实观察，不允许直接BET";
        sig.fieldChainVersion = CLOSED_LOOP_VERSION;
        if (c == null) {
            return sig;
        }
        ShadowDomainStats best = findBestPromotionDomainForCandidate(c, report);
        if (best == null) {
            return sig;
        }
        sig.domainKey = best.domainKey;
        sig.domainType = best.domainType;
        sig.domainValue = best.domainValue;
        sig.rawStatus = safe(best.promotionStatus);
        sig.promotionSampleCount = best.total;
        sig.promotionHitCount = best.wouldHit;
        sig.promotionMissCount = best.wouldMiss;
        sig.promotionHitRate = best.wouldHitRate;
        sig.promotionRecentHitRate = best.recent60Rate;
        sig.promotionReliability = best.reliability;
        sig.promotionRealBetHitRate = best.realBetHitRate;
        sig.promotionOverRealBetEdge = best.overRealBetEdge;
        sig.maxLoseStreak = best.maxLoseStreak;
        sig.shadowPromoteScore = promotionScore(best);
        sig.reason = safe(best.reason);
        sig.shadowActionAdvice = safe(best.advice);
        if (sig.shadowActionAdvice.length() == 0) {
            sig.shadowActionAdvice = "SHADOW只做反事实晋级验证，不直接转BET";
        }
        if (STATUS_PROMOTE_WATCH.equals(best.promotionStatus)) {
            if (AppConfig.CLUSTER_STATUS_STRONG.equals(best.clusterStatus)
                    || sig.shadowPromoteScore >= PROMOTION_STRONG_SCORE) {
                sig.shadowPromotionStatus = STATUS_STRONG;
            } else if (AppConfig.CLUSTER_STATUS_READY.equals(best.clusterStatus)
                    || sig.shadowPromoteScore >= PROMOTION_READY_SCORE) {
                sig.shadowPromotionStatus = STATUS_READY;
            } else {
                sig.shadowPromotionStatus = STATUS_PROMOTE_WATCH;
            }
            sig.shadowRejectCode = PROMOTION_REJECT_OK;
        } else if (STATUS_WATCH.equals(best.promotionStatus)) {
            sig.shadowPromotionStatus = STATUS_WATCH;
            sig.shadowRejectCode = PROMOTION_REJECT_WATCH_ONLY;
        } else if (STATUS_BLOCK.equals(best.promotionStatus)) {
            sig.shadowPromotionStatus = STATUS_BLOCK;
            sig.shadowRejectCode = PROMOTION_REJECT_BLOCKED;
        } else if (STATUS_FREEZE.equals(best.promotionStatus)
                || AppConfig.CLUSTER_STATUS_FREEZE.equals(best.clusterStatus)) {
            sig.shadowPromotionStatus = STATUS_FREEZE;
            sig.shadowRejectCode = PROMOTION_REJECT_FREEZE;
        } else if (STATUS_NO_PROMOTE.equals(best.promotionStatus)) {
            sig.shadowPromotionStatus = STATUS_NO_PROMOTE;
            sig.shadowRejectCode = PROMOTION_REJECT_NO_PROMOTE;
        } else {
            sig.shadowPromotionStatus = STATUS_INSUFFICIENT;
            sig.shadowRejectCode = PROMOTION_REJECT_SAMPLE_WEAK;
        }
        sig.evidenceScore =
                clamp01(
                        sig.shadowPromoteScore * 0.58d
                                + sig.promotionHitRate * 0.26d
                                + sig.promotionReliability * 0.16d);
        sig.counterEvidenceScore = calcPromotionCounterEvidence(sig);
        sig.netEvidenceScore = clampSignedLocal(sig.evidenceScore - sig.counterEvidenceScore);
        return sig;
    }

    private double calcPromotionCounterEvidence(PromotionCandidateSignal sig) {
        if (sig == null) {
            return 0.0d;
        }
        double risk = 0.0d;
        if (STATUS_BLOCK.equals(sig.rawStatus) || STATUS_FREEZE.equals(sig.rawStatus)) {
            risk += 0.45d;
        }
        if (STATUS_NO_PROMOTE.equals(sig.rawStatus)) {
            risk += 0.20d;
        }
        if (sig.promotionSampleCount < MIN_WATCH_SAMPLE) {
            risk += 0.22d;
        }
        if (sig.promotionHitRate > 0.0d && sig.promotionHitRate < BASE_K3_RATE) {
            risk += (BASE_K3_RATE - sig.promotionHitRate) * 1.80d;
        }
        if (sig.promotionRecentHitRate > 0.0d
                && sig.promotionRecentHitRate < AppConfig.DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE) {
            risk += 0.16d;
        }
        if (sig.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE) {
            risk += 0.18d;
        }
        return clamp01(risk);
    }

    private ShadowDomainStats findBestPromotionDomainForCandidate(
            Object c, ShadowPromotionReport report) {
        if (c == null
                || report == null
                || report.domainMap == null
                || report.domainMap.size() == 0) {
            return null;
        }
        ArrayList<ShadowDomainStats> hits = new ArrayList<ShadowDomainStats>();
        addCandidateDomain(hits, report, DOMAIN_POSITION, candidatePositionBucket(c));
        addCandidateDomain(hits, report, DOMAIN_FINALP, finalPBucket(getDouble(c, "finalP", 0.0d)));
        addCandidateDomain(hits, report, DOMAIN_EDGE, edgeBucket(readCandidateEdge(c)));
        addCandidateDomain(hits, report, DOMAIN_GATE, gateBucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_TEACHER, teacherBucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_V20, v20BucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_WINDOW, windowBucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_SAMPLE, sampleBucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_RECENT, recentBucketForCandidate(c));
        addCandidateDomain(hits, report, DOMAIN_COMBO, comboKeyForCandidate(c));
        addCandidateDomain(
                hits, report, DOMAIN_DYNAMIC_CLUSTER, dynamicClusterBucketForCandidate(c));
        addCandidateDomain(
                hits,
                report,
                DOMAIN_NEED_SUPPLY,
                needSupplyBucket(
                        getDouble(c, "needSupplyMatchScore", getDouble(c, "coverageQuality", 0.0d)),
                        getDouble(c, "needSupplyRisk", getDouble(c, "numberSupplyRisk", 0.0d))));
        addCandidateDomain(
                hits,
                report,
                DOMAIN_COUNTERFACTUAL,
                counterfactualBucket(
                        getDouble(
                                c,
                                "counterfactualWhyNotRisk",
                                getDouble(c, "counterfactualRisk", 0.0d)),
                        getDouble(c, "causeAblationScore", getDouble(c, "rankStability", 0.0d)),
                        getInt(c, "causeAblationFalseCauseCount", 0)));
        addCandidateDomain(
                hits,
                report,
                DOMAIN_THIRD_QUALITY,
                thirdQualityBucket(
                        getDouble(
                                c,
                                "needSupplyThirdQuality",
                                getDouble(
                                        c,
                                        "thirdQuality",
                                        getDouble(c, "thirdNumberQualityScore", 0.0d)))));
        addCandidateDomain(
                hits,
                report,
                DOMAIN_ADAPTIVE,
                adaptiveBucket(
                        getString(c, "adaptiveJudgeAction", getString(c, "adaptiveStatus", "")),
                        getString(
                                c,
                                "adaptiveJudgeStatus",
                                getString(c, "shadowPromotionStatus", ""))));
        addCandidateDomain(hits, report, DOMAIN_CAUSAL_COMBO, causalComboKeyForCandidate(c));
        if (hits.size() == 0) {
            return null;
        }
        ShadowDomainStats best = null;
        for (int i = 0; i < hits.size(); i++) {
            ShadowDomainStats d = hits.get(i);
            if (d == null) {
                continue;
            }
            if (best == null || isBetterPromotionCandidateDomain(d, best)) {
                best = d;
            }
        }
        return best;
    }

    private boolean isBetterPromotionCandidateDomain(ShadowDomainStats a, ShadowDomainStats b) {
        if (a == null) {
            return false;
        }
        if (b == null) {
            return true;
        }
        int ar = statusRank(a.promotionStatus);
        int br = statusRank(b.promotionStatus);
        if (ar != br) {
            return ar > br;
        }
        double as = promotionScore(a);
        double bs = promotionScore(b);
        if (as != bs) {
            return as > bs;
        }
        if (a.reliability != b.reliability) {
            return a.reliability > b.reliability;
        }
        return a.total > b.total;
    }

    private void addCandidateDomain(
            ArrayList<ShadowDomainStats> hits,
            ShadowPromotionReport report,
            String type,
            String key) {
        if (hits == null || report == null || report.domainMap == null) {
            return;
        }
        String domainKey = buildDomainKey(type, key);
        ShadowDomainStats d = report.domainMap.get(domainKey);
        if (d == null) {
            return;
        }
        if (!containsDomain(hits, d.domainKey)) {
            hits.add(d);
        }
    }

    private boolean containsDomain(ArrayList<ShadowDomainStats> list, String key) {
        if (list == null || key == null) {
            return false;
        }
        for (int i = 0; i < list.size(); i++) {
            ShadowDomainStats d = list.get(i);
            if (d != null && key.equals(d.domainKey)) {
                return true;
            }
        }
        return false;
    }

    private void writePromotionFieldsToCandidate(
            Object c, PromotionCandidateSignal sig, String source) {
        if (c == null || sig == null) {
            return;
        }
        applyThreeLevelPromotionDomains(c);
        putStringSafe(c, "shadowPromotionStatus", sig.shadowPromotionStatus);
        putStringSafe(c, "shadowPromotionRawStatus", sig.rawStatus);
        putStringSafe(c, "shadowPromotionDomainKey", sig.domainKey);
        putStringSafe(c, "shadowPromotionDomain", sig.domainKey);
        putIntSafe(
                c,
                "promotionDomainAvailable",
                sig.domainKey == null || sig.domainKey.length() == 0 ? 0 : 1);
        putStringSafe(c, "shadowPromotionDomainType", sig.domainType);
        putStringSafe(c, "shadowPromotionDomainValue", sig.domainValue);
        putStringSafe(c, "shadowRejectCode", sig.shadowRejectCode);
        putStringSafe(c, "shadowPromotionRejectCode", sig.shadowRejectCode);
        putStringSafe(c, "shadowPromotionActionAdvice", sig.shadowActionAdvice);
        putStringSafe(c, "shadowPromotionReason", sig.reason);
        putStringSafe(c, "shadowPromotionVersion", LINK_V2_VERSION);
        putStringSafe(c, "shadowPromotionFieldSource", source == null ? "" : source);
        putStringSafe(c, "fieldChainVersion", CLOSED_LOOP_VERSION);
        putDoubleSafe(c, "shadowPromoteScore", sig.shadowPromoteScore);
        putDoubleSafe(c, "shadowPromotionScore", sig.shadowPromoteScore);
        putDoubleSafe(c, "shadowPromotionHitRate", sig.promotionHitRate);
        putDoubleSafe(c, "shadowPromotionRecentHitRate", sig.promotionRecentHitRate);
        putDoubleSafe(c, "shadowPromotionReliability", sig.promotionReliability);
        putDoubleSafe(c, "shadowPromotionRealBetHitRate", sig.promotionRealBetHitRate);
        putDoubleSafe(c, "shadowPromotionOverRealBetEdge", sig.promotionOverRealBetEdge);
        putDoubleSafe(c, "shadowPromotionEvidenceScore", sig.evidenceScore);
        putDoubleSafe(c, "shadowPromotionCounterScore", sig.counterEvidenceScore);
        putDoubleSafe(c, "shadowPromotionNetScore", sig.netEvidenceScore);
        putIntSafe(c, "promotionSampleCount", sig.promotionSampleCount);
        putIntSafe(c, "shadowPromotionSampleCount", sig.promotionSampleCount);
        putIntSafe(c, "shadowPromotionHitCount", sig.promotionHitCount);
        putIntSafe(c, "shadowPromotionMissCount", sig.promotionMissCount);
        putStringSafe(
                c,
                "evidenceSource",
                sig.promotionSampleCount > 0
                        ? "SHADOW_SETTLED_PROMOTION"
                        : "NO_SETTLED_PROMOTION_EVIDENCE");
        putIntSafe(c, "evidenceSettledOnly", 1);
        putIntSafe(c, "evidenceSampleCount", sig.promotionSampleCount);
        putIntSafe(c, "evidenceHitCount", sig.promotionHitCount);
        putIntSafe(c, "evidenceMissCount", sig.promotionMissCount);
        putIntSafe(c, "shadowPromotionMaxLoseStreak", sig.maxLoseStreak);
        putIntSafe(
                c,
                "shadowPromotionReadyFlag",
                (STATUS_READY.equals(sig.shadowPromotionStatus)
                                || STATUS_STRONG.equals(sig.shadowPromotionStatus)
                                || STATUS_PROMOTE_WATCH.equals(sig.shadowPromotionStatus))
                        ? 1
                        : 0);
        putIntSafe(
                c,
                "shadowPromotionStrongFlag",
                STATUS_STRONG.equals(sig.shadowPromotionStatus) ? 1 : 0);
        putIntSafe(
                c,
                "shadowPromotionWatchFlag",
                STATUS_WATCH.equals(sig.shadowPromotionStatus) ? 1 : 0);
        putIntSafe(
                c,
                "shadowPromotionBlockFlag",
                (STATUS_BLOCK.equals(sig.shadowPromotionStatus)
                                || STATUS_FREEZE.equals(sig.shadowPromotionStatus))
                        ? 1
                        : 0);
        double wilsonLower = wilsonLowerBound(sig.promotionHitCount, sig.promotionSampleCount);
        boolean probeEligible = isControlledProbeEligible(sig, wilsonLower);
        putDoubleSafe(c, "promotionWilsonLowerBound", wilsonLower);
        putIntSafe(c, "shadowProbeEligible", probeEligible ? 1 : 0);
        putIntSafe(c, "probeEligible", probeEligible ? 1 : 0);
        putStringSafe(c, "promotionTier", promotionTier(sig, wilsonLower));
        putStringSafe(
                c,
                "promotionEligibilityReason",
                probeEligible
                        ? "DOMAIN_SETTLED_EVIDENCE_READY_FOR_DECISION_PROBE_REVIEW"
                        : "WATCH_ONLY_NOT_DIRECT_BET");
        double oldAnti =
                getDouble(c, "antiEvidenceScore", getDouble(c, "counterEvidenceScore", 0.0d));
        double oldCounter = getDouble(c, "counterEvidenceScore", oldAnti);
        double oldNet = getDouble(c, "netEvidenceScore", 0.0d);
        putDoubleSafe(c, "antiEvidenceScore", Math.max(oldAnti, sig.counterEvidenceScore));
        putDoubleSafe(c, "counterEvidenceScore", Math.max(oldCounter, sig.counterEvidenceScore));
        putDoubleSafe(
                c, "netEvidenceScore", clampSignedLocal(Math.max(oldNet, sig.netEvidenceScore)));
        if (STATUS_BLOCK.equals(sig.shadowPromotionStatus)
                || STATUS_FREEZE.equals(sig.shadowPromotionStatus)) {
            String oldReject = getString(c, "rejectCode", "");
            if (oldReject == null || oldReject.length() == 0 || "OK".equals(oldReject)) {
                putStringSafe(c, "rejectCode", sig.shadowRejectCode);
            }
            String oldFinalReject = getString(c, "finalDecisionRejectCode", "");
            if (oldFinalReject == null
                    || oldFinalReject.length() == 0
                    || "OK".equals(oldFinalReject)) {
                putStringSafe(c, "finalDecisionRejectCode", sig.shadowRejectCode);
            }
        }
        String oldReason = getString(c, "reasonSummary", "");
        String add =
                "SHADOW晋级验证="
                        + sig.shadowPromotionStatus
                        + "｜样本="
                        + sig.promotionSampleCount
                        + "｜分="
                        + pct(sig.shadowPromoteScore)
                        + "｜域="
                        + safe(sig.domainKey)
                        + "｜拒绝="
                        + sig.shadowRejectCode
                        + "｜说明=只做反事实晋级，不直接BET";
        if (oldReason == null || oldReason.length() == 0) {
            putStringSafe(c, "reasonSummary", add);
        } else if (oldReason.indexOf("SHADOW晋级验证") < 0) {
            putStringSafe(c, "reasonSummary", oldReason + "｜" + add);
        }
    }

    private String candidatePositionBucket(Object c) {
        return "P" + getInt(c, "position", 0);
    }

    private double readCandidateEdge(Object c) {
        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", 0.0d);
        if (edge == 0.0d && finalP > 0.0d && gate > 0.0d) {
            edge = finalP - gate;
        }
        return edge;
    }

    private String gateBucketForCandidate(Object c) {
        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        if (gate <= 0.0d || finalP <= 0.0d) {
            return "Gate空";
        }
        double diff = finalP - gate;
        if (diff >= 0.030d) return "Gate强通过";
        if (diff >= 0.010d) return "Gate有效通过";
        if (diff >= 0.0d) return "Gate薄通过";
        if (diff >= -0.010d) return "Gate附近被挡";
        return "Gate明显未过";
    }

    private String teacherBucketForCandidate(Object c) {
        return normalizeBucket(
                firstNonEmpty(
                        getString(c, "teacherMatrixStatus", ""),
                        getString(c, "teacherNumberStatus", ""),
                        getString(c, "teacherStatus", ""),
                        getString(c, "teacherStatusClass", "")));
    }

    private String v20BucketForCandidate(Object c) {
        String a =
                firstNonEmpty(
                        getString(c, "v20Status", ""),
                        getString(c, "dynamicStatus", ""),
                        getString(c, "v20Action", ""),
                        getString(c, "dynamicV20Action", ""));
        if (a.length() == 0) {
            a =
                    firstNonEmpty(
                            getString(c, "decisionActionAdvice", ""),
                            getString(c, "actionAdvice", ""));
        }
        return normalizeBucket(a);
    }

    private String windowBucketForCandidate(Object c) {
        String w =
                firstNonEmpty(
                        getString(c, "windowStateKey", ""),
                        getString(c, "finalDecisionWindowStateKey", ""),
                        getString(c, "dynamicWindowStateKey", ""),
                        getString(c, "probabilityWindowStateKey", ""));
        return normalizeBucket(w);
    }

    private String dynamicClusterBucketForCandidate(Object c) {
        String d =
                firstNonEmpty(
                        getString(c, "dynamicClusterId", ""),
                        getString(c, "dynamicClusterStatus", ""),
                        getString(c, "dynamicClusterPrototypeKey", ""),
                        getString(c, "clusterKey", ""));
        return normalizeBucket(d);
    }

    private String sampleBucketForCandidate(Object c) {
        int sample =
                Math.max(
                        Math.max(getInt(c, "effectiveSample", 0), getInt(c, "comboSample", 0)),
                        Math.max(
                                getInt(c, "similarWindowSample", 0),
                                getInt(c, "teacherMatrixSample", 0)));
        double reliability =
                getDouble(c, "sampleReliability", getDouble(c, "teacherSampleReliability", 0.0d));
        if (reliability >= 0.75d || sample >= 120) return "Sample强";
        if (reliability >= 0.60d || sample >= 60) return "Sample可用";
        if (sample >= 30) return "Sample观察";
        return "Sample不足";
    }

    private String recentBucketForCandidate(Object c) {
        double recent =
                getDouble(
                        c,
                        "recentValidity",
                        getDouble(
                                c,
                                "teacherRecentValidity",
                                getDouble(c, "v20RecentValidity", 0.0d)));
        if (recent >= 0.75d) return "Recent强";
        if (recent >= 0.60d) return "Recent可用";
        if (recent > 0.0d) return "Recent弱";
        return "Recent空";
    }

    private String comboKeyForCandidate(Object c) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(getInt(c, "position", 0));
        sb.append("|").append(finalPBucket(getDouble(c, "finalP", 0.0d)));
        sb.append("|").append(shortBucket(teacherBucketForCandidate(c)));
        sb.append("|").append(shortBucket(v20BucketForCandidate(c)));
        sb.append("|").append(shortBucket(windowBucketForCandidate(c)));
        sb.append("|").append(edgeBucket(readCandidateEdge(c)));
        return sb.toString();
    }

    private String causalComboKeyForCandidate(Object c) {
        if (c == null) return "CAUSAL:空";
        return "P"
                + getInt(c, "position", 0)
                + "|CL="
                + shortBucket(dynamicClusterBucketForCandidate(c))
                + "|NS="
                + shortBucket(
                        needSupplyBucket(
                                getDouble(
                                        c,
                                        "needSupplyMatchScore",
                                        getDouble(c, "coverageQuality", 0.0d)),
                                getDouble(
                                        c,
                                        "needSupplyRisk",
                                        getDouble(c, "numberSupplyRisk", 0.0d))))
                + "|CF="
                + shortBucket(
                        counterfactualBucket(
                                getDouble(
                                        c,
                                        "counterfactualWhyNotRisk",
                                        getDouble(c, "counterfactualRisk", 0.0d)),
                                getDouble(
                                        c,
                                        "causeAblationScore",
                                        getDouble(c, "rankStability", 0.0d)),
                                getInt(c, "causeAblationFalseCauseCount", 0)))
                + "|TQ="
                + shortBucket(
                        thirdQualityBucket(
                                getDouble(
                                        c,
                                        "needSupplyThirdQuality",
                                        getDouble(
                                                c,
                                                "thirdQuality",
                                                getDouble(c, "thirdNumberQualityScore", 0.0d)))))
                + "|AD="
                + shortBucket(
                        adaptiveBucket(
                                getString(
                                        c,
                                        "adaptiveJudgeAction",
                                        getString(c, "adaptiveStatus", "")),
                                getString(
                                        c,
                                        "adaptiveJudgeStatus",
                                        getString(c, "shadowPromotionStatus", ""))));
    }

    private double clampSignedLocal(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        if (v < -1.0d) return -1.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }

    private void putStringSafe(Object obj, String field, String value) {
        if (obj == null || field == null || field.length() == 0) return;
        try {
            Field f = obj.getClass().getField(field);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private void putDoubleSafe(Object obj, String field, double value) {
        if (obj == null || field == null || field.length() == 0) return;
        if (Double.isNaN(value) || Double.isInfinite(value)) value = 0.0d;
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

    private void putIntSafe(Object obj, String field, int value) {
        if (obj == null || field == null || field.length() == 0) return;
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

    private static final class PromotionCandidateSignal {
        String shadowPromotionStatus;
        String rawStatus;
        String shadowRejectCode;
        String shadowActionAdvice;
        String domainKey;
        String domainType;
        String domainValue;
        String reason;
        String fieldChainVersion;
        int promotionSampleCount;
        int promotionHitCount;
        int promotionMissCount;
        int maxLoseStreak;
        double shadowPromoteScore;
        double promotionHitRate;
        double promotionRecentHitRate;
        double promotionReliability;
        double promotionRealBetHitRate;
        double promotionOverRealBetEdge;
        double evidenceScore;
        double counterEvidenceScore;
        double netEvidenceScore;

        PromotionCandidateSignal() {
            shadowPromotionStatus = STATUS_INSUFFICIENT;
            rawStatus = STATUS_INSUFFICIENT;
            shadowRejectCode = PROMOTION_REJECT_SAMPLE_WEAK;
            shadowActionAdvice = "";
            domainKey = "";
            domainType = "";
            domainValue = "";
            reason = "";
            fieldChainVersion = CLOSED_LOOP_VERSION;
        }
    }

    public static final class ShadowPromotionReport {
        public int ok;
        public String version;
        public long createdAt;
        public int shadowLimit;
        public int betLimit;

        public ArrayList<ShadowCase> shadowCases;
        public ArrayList<ShadowCase> realBetCases;

        public int totalShadow;
        public int settledShadow;
        public int wouldHit;
        public int wouldMiss;
        public int waitShadow;
        public double wouldHitRate;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;

        public int realBetTotal;
        public int realBetHit;
        public int realBetMiss;
        public int realBetWait;
        public int realBetError;
        public double realBetRate;
        public double realBetAvgFinalP;
        public double realBetAvgGate;
        public double realBetAvgEdge;

        public HashMap<String, ShadowDomainStats> domainMap;
        public HashMap<String, RealBetDomainStats> realBetDomainMap;
        public ArrayList<ShadowDomainStats> allDomains;
        public ArrayList<ShadowDomainStats> promotedDomains;
        public ArrayList<ShadowDomainStats> watchDomains;
        public ArrayList<ShadowDomainStats> blockedDomains;
        public ArrayList<PromotionSignal> signals;

        public ShadowDomainStats bestDomain;
        public ShadowDomainStats worstDomain;
        public int promoteCount;
        public int watchCount;
        public int blockCount;
        public int insufficientCount;

        public String mainSignal;
        public String nextAdvice;
        public int dynamicDomainCount;
        public int dynamicReadyCount;
        public int dynamicFreezeCount;
        public String summary;
        public String detail;

        public ShadowPromotionReport() {
            ok = 0;
            version = "";
            createdAt = 0L;
            shadowLimit = 0;
            betLimit = 0;
            shadowCases = new ArrayList<ShadowCase>();
            realBetCases = new ArrayList<ShadowCase>();
            domainMap = new HashMap<String, ShadowDomainStats>();
            realBetDomainMap = new HashMap<String, RealBetDomainStats>();
            allDomains = new ArrayList<ShadowDomainStats>();
            promotedDomains = new ArrayList<ShadowDomainStats>();
            watchDomains = new ArrayList<ShadowDomainStats>();
            blockedDomains = new ArrayList<ShadowDomainStats>();
            signals = new ArrayList<PromotionSignal>();
            mainSignal = "";
            nextAdvice = "";
            summary = "";
            detail = "";
        }

        public boolean hasPromoteDomain() {
            return promotedDomains != null && promotedDomains.size() > 0;
        }

        public boolean isPromotedDomain(String key) {
            if (key == null || promotedDomains == null) return false;
            for (int i = 0; i < promotedDomains.size(); i++) {
                ShadowDomainStats d = promotedDomains.get(i);
                if (d != null && key.equals(d.domainKey)) return true;
            }
            return false;
        }

        public PromotionSignal bestSignal() {
            if (signals == null || signals.size() == 0) return null;
            return signals.get(0);
        }
    }

    public static final class ShadowDomainStats {
        public String domainType;
        public String domainKey;
        public String domainValue;
        public String note;
        public int total;
        public int wouldHit;
        public int wouldMiss;
        public double wouldHitRate;
        public double sumFinalP;
        public double sumGate;
        public double sumEdge;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;
        public double virtualEdge;

        public int realBetCount;
        public int realBetHit;
        public int realBetMiss;
        public double realBetHitRate;
        public double overRealBetEdge;
        public double reliability;

        public String promotionStatus;
        public String reason;
        public String advice;
        public ArrayList<ShadowCaseBrief> sampleCases;
        public int recent30Total;
        public int recent30Hit;
        public double recent30Rate;
        public int recent60Total;
        public int recent60Hit;
        public double recent60Rate;
        public int recent120Total;
        public int recent120Hit;
        public double recent120Rate;
        public int currentLoseStreak;
        public int maxLoseStreak;
        public double sumCounterRisk;
        public double counterRiskAvg;
        public String clusterStatus;
        public String promotionAction;
        public String promotionReason;
        public String shadowRejectCode;
        public String shadowActionAdvice;
        public double shadowPromoteScore;
        public int promotionSampleCount;

        public ShadowDomainStats() {
            domainType = "";
            domainKey = "";
            domainValue = "";
            note = "";
            promotionStatus = STATUS_INSUFFICIENT;
            reason = "";
            advice = "";
            sampleCases = new ArrayList<ShadowCaseBrief>();
            clusterStatus = AppConfig.CLUSTER_STATUS_UNKNOWN;
            promotionAction = AppConfig.ADAPTIVE_ACTION_WATCH;
            promotionReason = "";
            shadowRejectCode = "";
            shadowActionAdvice = "";
        }

        public void add(ShadowCase c) {
            if (c == null) return;
            total++;
            if (c.hit == 1
                    || "WIN".equalsIgnoreCase(c.result)
                    || "HIT".equalsIgnoreCase(c.result)
                    || "命中".equals(c.result)) {
                wouldHit++;
            } else {
                wouldMiss++;
            }
            sumFinalP += c.finalP;
            sumGate += c.dynamicGate;
            sumEdge += c.edge;
            if (total <= RECENT30_LIMIT) {
                recent30Total++;
                if (c.hit == 1 || isWinTextStatic(c.result)) recent30Hit++;
            }
            if (total <= RECENT60_LIMIT) {
                recent60Total++;
                if (c.hit == 1 || isWinTextStatic(c.result)) recent60Hit++;
            }
            if (total <= RECENT120_LIMIT) {
                recent120Total++;
                if (c.hit == 1 || isWinTextStatic(c.result)) recent120Hit++;
            }
            if (c.hit == 1 || isWinTextStatic(c.result)) {
                currentLoseStreak = 0;
            } else {
                currentLoseStreak++;
                if (currentLoseStreak > maxLoseStreak) maxLoseStreak = currentLoseStreak;
            }
            sumCounterRisk +=
                    Math.max(
                            Math.max(c.dynamicClusterCounterScore, c.counterfactualWhyNotRisk),
                            Math.max(c.needSupplyRisk, c.adaptiveFreezeScore));
            counterRiskAvg = total <= 0 ? 0.0d : sumCounterRisk / (double) total;
        }

        public String shortText() {
            return domainKey
                    + " "
                    + total
                    + "单/中"
                    + wouldHit
                    + "/率"
                    + pct(wouldHitRate)
                    + "/状态"
                    + promotionStatus;
        }

        public String fullText() {
            return domainKey
                    + "：状态="
                    + promotionStatus
                    + "，SHADOW="
                    + total
                    + "，wouldHit="
                    + wouldHit
                    + "，wouldMiss="
                    + wouldMiss
                    + "，反事实胜率="
                    + pct(wouldHitRate)
                    + "，虚拟Edge="
                    + signedPct(virtualEdge)
                    + "，同域真实BET="
                    + realBetCount
                    + "/"
                    + realBetHit
                    + "/"
                    + pct(realBetHitRate)
                    + "，超真实BET="
                    + signedPct(overRealBetEdge)
                    + "，均P="
                    + pct(avgFinalP)
                    + "，均Gate="
                    + pct(avgGate)
                    + "，均Edge="
                    + signedPct(avgEdge)
                    + "，可靠度="
                    + pct(reliability)
                    + "，近期60="
                    + pct(recent60Rate)
                    + "，最大连挂="
                    + maxLoseStreak
                    + "，动态状态="
                    + clusterStatus
                    + "，动作="
                    + promotionAction;
        }
    }

    public static final class RealBetDomainStats {
        public String domainType;
        public String domainKey;
        public String domainValue;
        public int total;
        public int hit;
        public int miss;

        public RealBetDomainStats() {
            domainType = "";
            domainKey = "";
            domainValue = "";
        }

        public void add(ShadowCase c) {
            if (c == null) return;
            total++;
            if (c.hit == 1
                    || "WIN".equalsIgnoreCase(c.result)
                    || "HIT".equalsIgnoreCase(c.result)
                    || "命中".equals(c.result)) {
                hit++;
            } else {
                miss++;
            }
        }

        public double rate() {
            if (total <= 0) return 0.0d;
            return ((double) hit) / ((double) total);
        }
    }

    public static final class PromotionSignal {
        public String domainType;
        public String domainKey;
        public String status;
        public double score;
        public int sample;
        public double wouldHitRate;
        public double realBetHitRate;
        public double virtualEdge;
        public double overRealBetEdge;
        public double reliability;
        public String reason;
        public String actionAdvice;
        public String clusterStatus;
        public String action;
        public double recentHitRate;
        public int maxLoseStreak;
        public String shadowRejectCode;
        public int promotionSampleCount;
        public double shadowPromoteScore;

        public PromotionSignal() {
            domainType = "";
            domainKey = "";
            status = "";
            reason = "";
            actionAdvice = "";
            shadowRejectCode = "";
        }

        public String fullText() {
            return domainKey
                    + "：status="
                    + status
                    + "，score="
                    + pct(score)
                    + "，sample="
                    + sample
                    + "，shadowRate="
                    + pct(wouldHitRate)
                    + "，realBetRate="
                    + pct(realBetHitRate)
                    + "，virtualEdge="
                    + signedPct(virtualEdge)
                    + "，overRealBet="
                    + signedPct(overRealBetEdge)
                    + "，reliability="
                    + pct(reliability)
                    + "，clusterStatus="
                    + clusterStatus
                    + "，action="
                    + action
                    + "，recent="
                    + pct(recentHitRate)
                    + "，maxLose="
                    + maxLoseStreak
                    + "，reason="
                    + reason
                    + "，advice="
                    + actionAdvice;
        }
    }

    public static final class ShadowCaseBrief {
        public String kind;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int actualNumber;
        public int hit;
        public String result;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public String teacherStatus;
        public String v20Status;
        public String windowStateKey;
        public String hint;
        public String dynamicClusterStatus;
        public double needSupplyMatchScore;
        public double counterfactualWhyNotRisk;
        public double thirdQuality;
        public String adaptiveAction;

        public ShadowCaseBrief() {
            kind = "";
            targetIssue = "";
            result = "";
            teacherStatus = "";
            v20Status = "";
            windowStateKey = "";
            hint = "";
        }
    }

    public static final class ShadowCase {
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
        public double pnl;
        public String missReason;

        public double finalP;
        public double rawP;
        public double calibratedP;
        public double dynamicGate;
        public double edge;

        public int effectiveSample;
        public int comboSample;
        public double similarHitRate;
        public double comboHitRate;
        public String stateMode;
        public String reasonSummary;

        public String finalPBucket;
        public String edgeBucket;
        public String gateBucket;
        public String teacherBucket;
        public String v20Bucket;
        public String windowBucket;
        public String sampleBucket;
        public String recentBucket;

        public String teacherMatrixStatus;
        public String teacherNumberStatus;
        public String teacherStatus;
        public String teacherMatrixSummary;
        public int teacherMatrixSample;
        public double teacherMatrixHitRate;
        public double teacherMatrixConfidence;
        public double teacherMatrixRisk;
        public double teacherEvidenceContribution;
        public double teacherCounterContribution;

        public String windowStateKey;
        public String dynamicWindowStateKey;
        public String probabilityWindowStateKey;
        public String decisionWindowStateKey;

        public String dynamicStatus;
        public String v20Status;
        public double v20SupportScore;
        public double v20RiskScore;
        public double evidenceContribution;
        public double counterContribution;
        public double sampleReliability;
        public double recentValidity;
        public double featureDistance;
        public int similarWindowSample;
        public double similarWindowHitRate;
        public String decisionActionAdvice;
        public int decisionHardRisk;
        public int decisionCounterRiskHigh;
        public int decisionEvidenceStrong;
        public int decisionEvidenceElite;
        public String promotionHint;
        public String dynamicClusterId;
        public String dynamicClusterStatus;
        public String dynamicClusterPrototypeKey;
        public String dynamicClusterFeatureKey;
        public int dynamicClusterSample;
        public double dynamicClusterHitRate;
        public double dynamicClusterRecentHitRate;
        public double dynamicClusterConfidence;
        public double dynamicClusterDistance;
        public double dynamicClusterVolatility;
        public int dynamicClusterMaxLose;
        public double dynamicClusterCounterScore;
        public String dynamicClusterBucket;
        public String positionNeedSummary;
        public double positionNeedStrength;
        public double positionNeedChaosRisk;
        public String numberSupplySummary;
        public double numberSupplyStrength;
        public double numberSupplyRisk;
        public double needSupplyMatchScore;
        public double needSupplyThirdQuality;
        public double needSupplyRoleDiversity;
        public double needSupplyRisk;
        public String needSupplySummary;
        public String needSupplyBucket;
        public int counterfactualRank;
        public double counterfactualRankScore;
        public double counterfactualWhyNotRisk;
        public String counterfactualWhyNot;
        public String counterfactualBucket;
        public double causeAblationScore;
        public int causeAblationFalseCauseCount;
        public String thirdQualityBucket;
        public String adaptiveJudgeStatus;
        public String adaptiveJudgeAction;
        public String adaptiveJudgeDomain;
        public double adaptivePromoteScore;
        public double adaptiveFreezeScore;
        public String adaptiveBucket;
        public String whyNoBet;
        public String dynamicCausalSummary;

        public ShadowCase() {
            kind = "";
            targetIssue = "";
            baseIssue = "";
            result = "";
            missReason = "";
            stateMode = "";
            reasonSummary = "";
            finalPBucket = "";
            edgeBucket = "";
            gateBucket = "";
            teacherBucket = "";
            v20Bucket = "";
            windowBucket = "";
            sampleBucket = "";
            recentBucket = "";
            teacherMatrixStatus = "";
            teacherNumberStatus = "";
            teacherStatus = "";
            teacherMatrixSummary = "";
            windowStateKey = "";
            dynamicWindowStateKey = "";
            probabilityWindowStateKey = "";
            decisionWindowStateKey = "";
            dynamicStatus = "";
            v20Status = "";
            decisionActionAdvice = "";
            promotionHint = "";
            dynamicClusterId = "";
            dynamicClusterStatus = "";
            dynamicClusterPrototypeKey = "";
            dynamicClusterFeatureKey = "";
            dynamicClusterBucket = "";
            positionNeedSummary = "";
            numberSupplySummary = "";
            needSupplySummary = "";
            needSupplyBucket = "";
            counterfactualWhyNot = "";
            counterfactualBucket = "";
            thirdQualityBucket = "";
            adaptiveJudgeStatus = "";
            adaptiveJudgeAction = "";
            adaptiveJudgeDomain = "";
            adaptiveBucket = "";
            whyNoBet = "";
            dynamicCausalSummary = "";
        }
    }

    /* FIX73487246：晋级只输出受控探针资格，仍不直接BET。 */
    private double wilsonLowerBound(int hit, int total) {
        if (total <= 0 || hit < 0) return 0.0d;
        double n = (double) total;
        double p = Math.max(0.0d, Math.min(1.0d, ((double) hit) / n));
        double z = 1.281551565545d; /* 80%单侧下界，适合小样本观察，不宣称确定优势 */
        double z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp01((center - margin) / (1.0d + z2 / n));
    }

    private boolean isControlledProbeEligible(PromotionCandidateSignal s, double lower) {
        if (s == null) return false;
        String st = safe(s.shadowPromotionStatus).toUpperCase(java.util.Locale.ROOT);
        boolean stateOk =
                st.indexOf("STRONG") >= 0
                        || st.indexOf("READY") >= 0
                        || st.indexOf("PROMOTE_WATCH") >= 0;
        if (!stateOk || s.promotionSampleCount < 18) return false;
        if (s.promotionHitRate < 0.340d || lower < 0.280d) return false;
        if (s.promotionRecentHitRate > 0.0d && s.promotionRecentHitRate < 0.300d) return false;
        if (s.counterEvidenceScore > 0.620d || s.maxLoseStreak >= 6) return false;
        return true;
    }

    private String promotionTier(PromotionCandidateSignal s, double lower) {
        if (s == null) return "NONE";
        if (s.promotionSampleCount >= 50 && s.promotionHitRate >= 0.390d && lower >= 0.330d)
            return "L3_MATURE";
        if (s.promotionSampleCount >= 30 && s.promotionHitRate >= 0.365d && lower >= 0.300d)
            return "L2_READY";
        if (s.promotionSampleCount >= 18 && s.promotionHitRate >= 0.340d && lower >= 0.280d)
            return "L1_BOOTSTRAP";
        return "WATCH_ONLY";
    }

    private String buildCoarsePromotionDomain(Object c) {
        if (c == null || getInt(c, "evidenceDomainFrozen", 0) != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(readString(c, "promotionCoarseDomain", ""));
    }

    private String buildMidPromotionDomain(Object c) {
        if (c == null || getInt(c, "evidenceDomainFrozen", 0) != 1) return "";
        return AppConfig.sanitizePromotionDomainKey(readString(c, "promotionMidDomain", ""));
    }

    private void applyThreeLevelPromotionDomains(Object c) {
        if (c == null || getInt(c, "evidenceDomainFrozen", 0) != 1) return;
        String coarse = buildCoarsePromotionDomain(c);
        String mid = buildMidPromotionDomain(c);
        String fine = buildV2SettlementDomainKey(c);
        if (coarse.length() > 0) putStringSafe(c, "promotionCoarseDomain", coarse);
        if (mid.length() > 0) putStringSafe(c, "promotionMidDomain", mid);
        if (fine.length() > 0) putStringSafe(c, "promotionFineDomain", fine);
    }
}
