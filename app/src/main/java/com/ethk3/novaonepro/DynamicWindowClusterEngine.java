package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V20 第6/14：动态窗口簇自动分类引擎。
 *
 * <p>职责： 1. 把 WindowFeatureVector 自动归入 DynamicWindowCluster。 2. 没有相似簇时创建 NEW 簇。 3.
 * 根据同簇样本、命中率、近期命中率、最大连挂、波动、反证强度自动升级/降级。 4. 低于随机基准、样本不足、近期转弱、最大连挂过高时强制 WATCH/RISK/FREEZE。 5. 给
 * Candidate 写入 cluster / adaptive / promotion 字段桥。
 *
 * <p>红线： - 不创建 Trade。 - 不结算。 - 不写真账本。 - 不改变真实盈亏。 - 不绕过 DecisionGateEngine。 - STRONG/READY
 * 也只是送最终门复核，真实 BET 仍由 DecisionGateEngine 唯一裁决。
 */
public final class DynamicWindowClusterEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = AppConfig.ACTIVE_QUALITY_BUILD_VERSION;

    private static final String ENGINE_NAME = "DynamicWindowClusterEngine";
    private static final double EPS = 0.000001d;

    private final DbStore store;

    public DynamicWindowClusterEngine() {
        this(null);
    }

    public DynamicWindowClusterEngine(DbStore s) {
        store = s;
    }

    public String getVersion() {
        return VERSION;
    }

    public Models.ClusterMatchResult classifyAndApply(
            Models.ComboCandidate c,
            Models.WindowFeatureVector vector,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.DynamicWindowCluster> active = loadActiveClusters();
        return classifyAndApply(c, vector, active, baseIssue, targetIssue);
    }

    public Models.ClusterMatchResult classifyAndApply(
            Models.ComboCandidate c,
            Models.WindowFeatureVector vector,
            ArrayList<Models.DynamicWindowCluster> active,
            String baseIssue,
            String targetIssue) {
        if (c == null) {
            return emptyMatch("EMPTY_CANDIDATE");
        }

        Models.WindowFeatureVector v = normalizeVector(c, vector, targetIssue);
        ArrayList<Models.DynamicWindowCluster> pool =
                active == null ? loadActiveClusters() : active;

        Models.ClusterMatchResult best = findBestCluster(v, pool);
        Models.DynamicWindowCluster cluster;

        if (best == null || !best.matched()) {
            cluster = createNewCluster(v, c, "未找到足够近的历史簇，创建NEW观察簇");
            best = toMatch(cluster, cluster.prototypeDistance, "NEW：无相似簇，只观察不直接BET");
        } else {
            cluster = mergeRealtimeView(best, v, c);
            best =
                    toMatch(
                            cluster,
                            best.distance,
                            "匹配既有动态簇｜distance="
                                    + percent(best.distance)
                                    + "｜status="
                                    + cluster.status);
        }

        Models.AdaptiveJudgeResult judge = buildAdaptiveJudge(c, cluster, best);
        Models.PromotionDomainState promo = buildPromotionState(cluster, judge);

        applyClusterBridge(c, best, judge, promo);
        saveRealtimeClassification(c, v, cluster, best, judge, promo, baseIssue, targetIssue);

        return best;
    }

    public ArrayList<Models.ClusterMatchResult> classifyCandidates(
            List<Models.ComboCandidate> candidates,
            List<Models.WindowFeatureVector> vectors,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ClusterMatchResult> results = new ArrayList<Models.ClusterMatchResult>();
        if (candidates == null || candidates.size() == 0) {
            return results;
        }

        ArrayList<Models.DynamicWindowCluster> active = loadActiveClusters();

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            Models.WindowFeatureVector v = null;
            if (vectors != null && i >= 0 && i < vectors.size()) {
                v = vectors.get(i);
            }
            results.add(classifyAndApply(c, v, active, baseIssue, targetIssue));
        }

        sortMatches(results);
        return results;
    }

    public Models.DynamicWindowCluster updateClusterAfterShadowResult(
            Models.ComboCandidate c, boolean wouldHit, int actualNumber, String resultIssue) {
        if (c == null) {
            return null;
        }
        String clusterId = safe(c.dynamicClusterId);
        Models.DynamicWindowCluster cluster = null;
        if (store != null && clusterId.length() > 0) {
            try {
                cluster = store.getDynamicWindowCluster(clusterId);
            } catch (Throwable e) {
                cluster = null;
            }
        }
        if (cluster == null) {
            cluster = clusterFromCandidate(c);
        }
        if (cluster == null) {
            return null;
        }

        applyOutcome(cluster, wouldHit);
        cluster.reason = buildOutcomeReason(cluster, wouldHit, actualNumber, resultIssue);
        cluster.updatedAt = System.currentTimeMillis();
        try {
            cluster.recalc();
        } catch (Throwable e) {
        }

        if (store != null) {
            try {
                store.saveDynamicWindowCluster(cluster);
                if (!wouldHit) {
                    store.saveMissReasonSample(
                            buildMissReason(c, cluster, actualNumber, resultIssue));
                }
                store.savePromotionDomainSnapshot(
                        buildPromotionState(
                                cluster,
                                buildAdaptiveJudge(
                                        c,
                                        cluster,
                                        toMatch(cluster, cluster.prototypeDistance, "结算后刷新"))));
                store.updateDynamicCausalState(
                        AppConfig.STATE_DYNAMIC_CLUSTER_LAST_RUN,
                        String.valueOf(System.currentTimeMillis()));
                store.updateDynamicCausalState(
                        AppConfig.STATE_DYNAMIC_CLUSTER_SUMMARY, cluster.reason);
            } catch (Throwable e) {
            }
        }
        return cluster;
    }

    public Models.DynamicWindowCluster updateClusterAfterWindowSample(
            Models.WindowFeatureVector vector, boolean wouldHit, int maxLoseHint, String note) {
        if (vector == null) {
            return null;
        }
        Models.ClusterMatchResult best = findBestCluster(vector, loadActiveClusters());
        Models.DynamicWindowCluster cluster;
        if (best != null && best.matched()) {
            cluster = matchToCluster(best);
        } else {
            cluster = createNewCluster(vector, null, "窗口样本创建NEW簇");
        }

        applyOutcome(cluster, wouldHit);
        if (maxLoseHint > cluster.maxLoseStreak) {
            cluster.maxLoseStreak = maxLoseHint;
        }
        cluster.reason =
                "窗口样本学习｜hit="
                        + (wouldHit ? "1" : "0")
                        + "｜status="
                        + cluster.status
                        + "｜note="
                        + safe(note);
        cluster.updatedAt = System.currentTimeMillis();
        try {
            cluster.recalc();
        } catch (Throwable e) {
        }
        if (store != null) {
            try {
                store.saveDynamicWindowCluster(cluster);
            } catch (Throwable e) {
            }
        }
        return cluster;
    }

    public Models.ClusterMatchResult findBestCluster(
            Models.WindowFeatureVector vector, List<Models.DynamicWindowCluster> clusters) {
        if (vector == null) {
            return emptyMatch("EMPTY_VECTOR");
        }
        normalizeVectorOnly(vector);
        if (clusters == null || clusters.size() == 0) {
            return emptyMatch("NO_CLUSTER");
        }

        Models.ClusterMatchResult best = null;
        double bestDistance = 9.0d;

        for (int i = 0; i < clusters.size(); i++) {
            Models.DynamicWindowCluster c = clusters.get(i);
            if (c == null) {
                continue;
            }
            String status = AppConfig.normalizeDynamicClusterStatus(c.status);
            if (AppConfig.CLUSTER_STATUS_DEAD.equals(status)) {
                continue;
            }
            double d = distance(vector, c);
            if (d < bestDistance) {
                bestDistance = d;
                best = toMatch(c, d, "候选簇匹配");
            }
        }

        if (best == null) {
            return emptyMatch("NO_VALID_CLUSTER");
        }
        return best;
    }

    public ArrayList<Models.ClusterMatchResult> topMatches(
            Models.WindowFeatureVector vector,
            List<Models.DynamicWindowCluster> clusters,
            int topN) {
        ArrayList<Models.ClusterMatchResult> out = new ArrayList<Models.ClusterMatchResult>();
        if (vector == null || clusters == null || clusters.size() == 0) {
            return out;
        }
        for (int i = 0; i < clusters.size(); i++) {
            Models.DynamicWindowCluster c = clusters.get(i);
            if (c == null) {
                continue;
            }
            if (AppConfig.CLUSTER_STATUS_DEAD.equals(
                    AppConfig.normalizeDynamicClusterStatus(c.status))) {
                continue;
            }
            out.add(toMatch(c, distance(vector, c), "TOP_MATCH"));
        }
        sortMatches(out);
        int max = topN <= 0 ? AppConfig.DYNAMIC_CLUSTER_MATCH_TOP_N : topN;
        while (out.size() > max) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    public Models.DynamicWindowCluster createNewCluster(
            Models.WindowFeatureVector vector, Models.ComboCandidate c, String reason) {
        Models.WindowFeatureVector v =
                normalizeVector(c, vector, vector == null ? "" : vector.targetIssue);
        Models.DynamicWindowCluster cluster = new Models.DynamicWindowCluster();
        cluster.prototypeKey = buildPrototypeKey(v, c);
        cluster.prototypeVector = vectorSummary(v);
        cluster.clusterId =
                AppConfig.buildDynamicClusterKey(
                        v.vectorKey, v.position, v.n1, v.n2, v.n3, "", "", cluster.prototypeKey);
        cluster.sampleCount = 0;
        cluster.hitCount = 0;
        cluster.missCount = 0;
        cluster.recentSample = 0;
        cluster.recentHitCount = 0;
        cluster.hitRate = 0.0d;
        cluster.recentHitRate = 0.0d;
        cluster.maxLoseStreak = 0;
        cluster.confidence = 0.0d;
        cluster.volatility = volatilityOf(v);
        cluster.counterScore = AppConfig.clampDynamicCausalScore(v.counterEvidence);
        cluster.prototypeDistance = 0.0d;
        cluster.status = AppConfig.CLUSTER_STATUS_NEW;
        cluster.reason = safe(reason) + "｜prototype=" + cluster.prototypeKey;
        cluster.createdAt = System.currentTimeMillis();
        cluster.updatedAt = cluster.createdAt;
        return cluster;
    }

    public String buildPrototypeKey(Models.WindowFeatureVector v, Models.ComboCandidate c) {
        if (v == null) {
            return "DWC|EMPTY";
        }
        normalizeVectorOnly(v);
        String p = "P" + v.position;
        String k = "K" + AppConfig.normalizeK3Part(v.n1, v.n2, v.n3);
        String trend = "TR" + signedBucket(v.trendSlope24 + v.trendSlope48 + v.trendSlope96);
        String hotCold = "HC" + signedBucket(v.hotColdTransition);
        String omit = "OM" + bucket(v.omissionPressure);
        String den = "DEN" + bucket(v.densityScore);
        String rep = "REP" + bucket(v.repeatRhythm);
        String oe = "OE" + bucket(v.oddEvenBias);
        String hl = "HL" + bucket(v.highLowBias);
        String tail = "TAIL" + bucket(v.tailPressure);
        String nb = "NB" + bucket(v.neighborPressure);
        String cross = "CP" + bucket(v.crossPositionCrowd);
        String time = "TM" + signedBucket(v.timingOffset);
        String ce = "CE" + bucket(v.counterEvidence);
        String tq = "TQ" + bucket(v.thirdQuality);
        String ns = c == null ? "NSNA" : "NS" + bucket(c.needSupplyMatchScore);
        return AppConfig.safeKeyPart(
                "DWC|" + p + "|" + k + "|" + trend + "|" + hotCold + "|" + omit + "|" + den + "|"
                        + rep + "|" + oe + "|" + hl + "|" + tail + "|" + nb + "|" + cross + "|"
                        + time + "|" + ce + "|" + tq + "|" + ns);
    }

    public double distance(Models.WindowFeatureVector v, Models.DynamicWindowCluster cluster) {
        if (v == null || cluster == null) {
            return 1.0d;
        }
        normalizeVectorOnly(v);
        double d = 0.0d;

        String proto = cluster.prototypeKey == null ? "" : cluster.prototypeKey;
        String current = buildPrototypeKey(v, null);
        if (proto.length() > 0 && proto.equals(current)) {
            d += 0.000d;
        } else {
            d += 0.060d;
        }

        d += Math.abs(v.trendSlope24 - v.trendSlope96) * 0.060d;
        d += Math.abs(v.omissionPressure - midFromPrototype(proto, "OM")) * 0.100d;
        d += Math.abs(v.densityScore - midFromPrototype(proto, "DEN")) * 0.080d;
        d += Math.abs(v.repeatRhythm - midFromPrototype(proto, "REP")) * 0.060d;
        d += Math.abs(v.tailPressure - midFromPrototype(proto, "TAIL")) * 0.080d;
        d += Math.abs(v.neighborPressure - midFromPrototype(proto, "NB")) * 0.075d;
        d += Math.abs(v.crossPositionCrowd - midFromPrototype(proto, "CP")) * 0.100d;
        d += Math.abs(v.timingOffset) * 0.050d;
        d += Math.abs(v.counterEvidence - cluster.counterScore) * 0.130d;
        d +=
                Math.abs(
                                v.thirdQuality
                                        - Math.max(
                                                AppConfig.RANDOM_NUMBER_BASELINE,
                                                cluster.hitRate / 3.0d))
                        * 0.070d;

        if (cluster.sampleCount < AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_WATCH) {
            d += 0.040d;
        }
        if (cluster.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE) {
            d += 0.080d;
        }
        if (cluster.recentSample >= AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                && cluster.recentHitRate < AppConfig.DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE) {
            d += 0.090d;
        }
        return AppConfig.clampDynamicCausalScore(d);
    }

    public Models.AdaptiveJudgeResult buildAdaptiveJudge(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match) {
        Models.AdaptiveJudgeResult r = new Models.AdaptiveJudgeResult();
        r.targetIssue = c == null ? "" : safe(c.targetIssue);
        r.clusterId = cluster == null ? "" : safe(cluster.clusterId);
        r.clusterStatus =
                cluster == null
                        ? AppConfig.CLUSTER_STATUS_UNKNOWN
                        : AppConfig.normalizeDynamicClusterStatus(cluster.status);
        r.status = r.clusterStatus;
        r.candidateCount = c == null ? 0 : 1;
        r.clusterHitRate = cluster == null ? 0.0d : cluster.hitRate;
        r.clusterRecentHitRate = cluster == null ? 0.0d : cluster.recentHitRate;
        r.clusterSample = cluster == null ? 0 : cluster.sampleCount;
        r.clusterConfidence = cluster == null ? 0.0d : cluster.confidence;
        r.causeScore = causeScore(c, cluster, match);
        r.counterScore = counterScore(c, cluster, match);
        r.promoteScore = promoteScore(c, cluster, match);
        r.freezeScore = freezeScore(c, cluster, match);
        boolean needAllows =
                c != null
                        && AppConfig.needSupplyAllowsBet(
                                c.needSupplyMatchScore,
                                c.needSupplyThirdQuality,
                                c.needSupplyRisk,
                                c.needSupplyRoleDiversity);
        String riskTier =
                AppConfig.dynamicRiskTier(r.clusterStatus, r.clusterSample, r.clusterHitRate, 0.0d);
        if (r.clusterSample < AppConfig.DYNAMIC_SOFT_RISK_MIN_SAMPLE) {
            r.action = AppConfig.ADAPTIVE_ACTION_WATCH;
            r.domain = "DWC_WATCH_INSUFFICIENT";
            r.clusterStatus = "WATCH";
        } else if ("HARD_BLOCK".equals(riskTier)) {
            r.action = AppConfig.ADAPTIVE_ACTION_FREEZE;
            r.domain = "DWC_MATURE_HARD_BLOCK";
        } else if ("SOFT_RISK".equals(riskTier) || r.freezeScore >= 0.680d) {
            r.action = AppConfig.ADAPTIVE_ACTION_DOWNGRADE;
            r.domain = "DWC_SOFT_RISK";
        } else if (needAllows && r.promoteScore >= 0.620d) {
            r.action = AppConfig.ADAPTIVE_ACTION_STRONG;
            r.domain = "DWC_STRONG";
        } else if (needAllows && r.promoteScore >= 0.540d) {
            r.action = AppConfig.ADAPTIVE_ACTION_READY;
            r.domain = "DWC_READY";
        } else if (r.counterScore >= 0.620d) {
            r.action = AppConfig.ADAPTIVE_ACTION_DOWNGRADE;
            r.domain = "DWC_DOWNGRADE";
        } else {
            r.action = AppConfig.ADAPTIVE_ACTION_WATCH;
            r.domain = "DWC_WATCH";
        }
        r.reason =
                "动态窗口簇审判：sample="
                        + r.clusterSample
                        + " hit="
                        + percent(r.clusterHitRate)
                        + " riskTier="
                        + riskTier
                        + " action="
                        + r.action
                        + "；样本<30只WATCH，30-59最多SOFT_RISK，>=60才可能HARD_BLOCK";
        r.version = VERSION;
        try {
            r.recalc();
        } catch (Throwable ignore) {
        }
        return r;
    }

    public Models.PromotionDomainState buildPromotionState(
            Models.DynamicWindowCluster cluster, Models.AdaptiveJudgeResult judge) {
        Models.PromotionDomainState p = new Models.PromotionDomainState();
        if (cluster != null) {
            p.clusterId = cluster.clusterId;
            p.domainKey = "PROMO|" + safe(cluster.clusterId);
            p.status = cluster.status;
            p.sampleCount = cluster.sampleCount;
            p.hitCount = cluster.hitCount;
            p.recentSample = cluster.recentSample;
            p.recentHitCount = cluster.recentHitCount;
            p.maxLoseStreak = cluster.maxLoseStreak;
            p.confidence = cluster.confidence;
        }
        p.action = judge == null ? AppConfig.ADAPTIVE_ACTION_WATCH : judge.action;
        p.reason = judge == null ? "" : judge.reason;
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        return p;
    }

    public Models.MissReasonResult buildMissReason(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            int actualNumber,
            String resultIssue) {
        Models.MissReasonResult m = new Models.MissReasonResult();
        if (c == null) {
            return m;
        }
        m.targetIssue = safe(resultIssue);
        m.position = c.position;
        m.n1 = c.n1;
        m.n2 = c.n2;
        m.n3 = c.n3;
        m.k3Mask = c.k3Mask;
        m.actualNumber = actualNumber;
        m.clusterId = cluster == null ? safe(c.dynamicClusterId) : safe(cluster.clusterId);
        m.windowWrongScore = cluster == null ? 0.0d : riskFromCluster(cluster);
        m.positionWrongScore = c.positionNeedChaosRisk;
        m.mainNumberWrongScore = c.numberSupplyRisk;
        m.thirdDragScore =
                Math.max(
                        0.0d, AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY - c.needSupplyThirdQuality);
        m.counterIgnoredScore = Math.max(c.counterfactualWhyNotRisk, c.causeAblationScore);
        m.missType = detectMissType(m);
        m.detail =
                "动态簇未中反推｜cluster="
                        + m.clusterId
                        + "｜actual="
                        + actualNumber
                        + "｜windowWrong="
                        + percent(m.windowWrongScore)
                        + "｜positionWrong="
                        + percent(m.positionWrongScore)
                        + "｜mainWrong="
                        + percent(m.mainNumberWrongScore)
                        + "｜thirdDrag="
                        + percent(m.thirdDragScore)
                        + "｜counterIgnored="
                        + percent(m.counterIgnoredScore);
        m.nextAdvice = nextAdviceForMiss(m);
        try {
            m.recalc();
        } catch (Throwable e) {
        }
        return m;
    }

    private void applyClusterBridge(
            Models.ComboCandidate c,
            Models.ClusterMatchResult match,
            Models.AdaptiveJudgeResult judge,
            Models.PromotionDomainState promo) {
        if (c == null) {
            return;
        }
        if (match != null) {
            try {
                c.applyClusterMatch(match);
            } catch (Throwable e) {
            }
        }
        if (judge != null) {
            try {
                c.applyAdaptiveJudge(judge);
            } catch (Throwable e) {
            }
        }
        if (promo != null) {
            c.promotionClusterStatus = promo.status;
            c.promotionClusterHitRate = promo.hitRate;
            c.promotionClusterRecentHitRate = promo.recentHitRate;
            c.promotionClusterSample = promo.sampleCount;
            c.promotionClusterReason = promo.reason;
        }
        c.dynamicClusterVersion = VERSION;

        String why = buildWhy(c, match, judge);
        if (judge != null && AppConfig.adaptiveActionAllowsBet(judge.action)) {
            c.whyBet = why;
            c.whyNoBet = "";
        } else {
            c.whyBet = "";
            c.whyNoBet = why;
        }
        c.dynamicCausalSummary = why;
        try {
            Models.normalizeDynamicCausalBridgeFields(c);
        } catch (Throwable e) {
        }
    }

    private void saveRealtimeClassification(
            Models.ComboCandidate c,
            Models.WindowFeatureVector vector,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match,
            Models.AdaptiveJudgeResult judge,
            Models.PromotionDomainState promo,
            String baseIssue,
            String targetIssue) {
        if (store == null || c == null || cluster == null) {
            return;
        }
        try {
            store.saveDynamicWindowCluster(cluster);
            store.saveAdaptiveJudgeSample(judge);
            store.savePromotionDomainSnapshot(promo);
            store.saveWindowFeatureSample(
                    vector,
                    baseIssue,
                    "REALTIME_CLUSTER_CLASSIFY",
                    false,
                    cluster.clusterId,
                    cluster.status,
                    match == null ? 1.0d : match.distance);
            store.updateDynamicCausalState(
                    AppConfig.STATE_DYNAMIC_CLUSTER_SNAPSHOT_VERSION, VERSION);
            store.updateDynamicCausalState(
                    AppConfig.STATE_DYNAMIC_CLUSTER_LAST_RUN,
                    String.valueOf(System.currentTimeMillis()));
            store.updateDynamicCausalState(
                    AppConfig.STATE_DYNAMIC_CLUSTER_SUMMARY, buildWhy(c, match, judge));
        } catch (Throwable e) {
        }
    }

    private ArrayList<Models.DynamicWindowCluster> loadActiveClusters() {
        ArrayList<Models.DynamicWindowCluster> list = new ArrayList<Models.DynamicWindowCluster>();
        if (store == null) {
            return list;
        }
        try {
            list = store.loadActiveDynamicWindowClusters(AppConfig.DYNAMIC_CLUSTER_MAX_ACTIVE);
        } catch (Throwable e) {
            list = new ArrayList<Models.DynamicWindowCluster>();
        }
        return list == null ? new ArrayList<Models.DynamicWindowCluster>() : list;
    }

    private Models.DynamicWindowCluster mergeRealtimeView(
            Models.ClusterMatchResult best, Models.WindowFeatureVector v, Models.ComboCandidate c) {
        Models.DynamicWindowCluster cluster = matchToCluster(best);
        cluster.prototypeDistance = best.distance;
        cluster.prototypeKey =
                cluster.prototypeKey == null || cluster.prototypeKey.length() == 0
                        ? buildPrototypeKey(v, c)
                        : cluster.prototypeKey;
        cluster.prototypeVector = vectorSummary(v);
        cluster.volatility = Math.max(cluster.volatility, volatilityOf(v));
        cluster.counterScore =
                AppConfig.clampDynamicCausalScore(
                        cluster.counterScore * 0.72d + v.counterEvidence * 0.28d);
        cluster.confidence =
                AppConfig.clampDynamicCausalScore(
                        cluster.confidence * 0.75d + confidenceFromVector(v, c) * 0.25d);
        cluster.status =
                AppConfig.decideDynamicClusterStatus(
                        cluster.sampleCount,
                        cluster.recentSample,
                        cluster.hitRate,
                        cluster.recentHitRate,
                        cluster.maxLoseStreak,
                        cluster.confidence,
                        cluster.volatility,
                        cluster.counterScore);
        cluster.reason =
                "实时匹配刷新｜distance="
                        + percent(best.distance)
                        + "｜status="
                        + cluster.status
                        + "｜hit="
                        + percent(cluster.hitRate)
                        + "｜recent="
                        + percent(cluster.recentHitRate)
                        + "｜conf="
                        + percent(cluster.confidence)
                        + "｜counter="
                        + percent(cluster.counterScore)
                        + "｜真实BET仍由DecisionGateEngine裁决";
        cluster.updatedAt = System.currentTimeMillis();
        return cluster;
    }

    private Models.DynamicWindowCluster matchToCluster(Models.ClusterMatchResult m) {
        Models.DynamicWindowCluster c = new Models.DynamicWindowCluster();
        if (m == null) {
            return c;
        }
        c.clusterId = safe(m.clusterId);
        c.status = AppConfig.normalizeDynamicClusterStatus(m.clusterStatus);
        c.prototypeKey = safe(m.prototypeKey);
        c.prototypeDistance = m.distance;
        c.sampleCount = m.sampleCount;
        c.recentSample = m.recentSample;
        c.hitRate = AppConfig.clampDynamicCausalScore(m.hitRate);
        c.recentHitRate = AppConfig.clampDynamicCausalScore(m.recentHitRate);
        c.hitCount = (int) Math.round(c.hitRate * Math.max(1, c.sampleCount));
        c.recentHitCount = (int) Math.round(c.recentHitRate * Math.max(1, c.recentSample));
        c.missCount = Math.max(0, c.sampleCount - c.hitCount);
        c.confidence = AppConfig.clampDynamicCausalScore(m.confidence);
        c.counterScore = AppConfig.clampDynamicCausalScore(m.counterScore);
        c.maxLoseStreak = m.maxLoseStreak;
        c.reason = safe(m.reason);
        return c;
    }

    private void applyOutcome(Models.DynamicWindowCluster cluster, boolean hit) {
        if (cluster == null) {
            return;
        }
        cluster.sampleCount++;
        cluster.recentSample++;
        if (hit) {
            cluster.hitCount++;
            cluster.recentHitCount++;
            cluster.maxLoseStreak = 0;
        } else {
            cluster.missCount++;
            cluster.maxLoseStreak++;
        }
        if (cluster.recentSample > 60) {
            cluster.recentSample = 60;
            int recentHits = (int) Math.round(cluster.recentHitRate * 59.0d);
            cluster.recentHitCount = Math.max(0, Math.min(60, recentHits + (hit ? 1 : 0)));
        }
        cluster.hitRate = AppConfig.safeRate(cluster.hitCount, cluster.sampleCount);
        cluster.recentHitRate = AppConfig.safeRate(cluster.recentHitCount, cluster.recentSample);
        cluster.confidence = confidenceFromCluster(cluster);
        cluster.counterScore = counterAfterOutcome(cluster, hit);
        cluster.status =
                AppConfig.decideDynamicClusterStatus(
                        cluster.sampleCount,
                        cluster.recentSample,
                        cluster.hitRate,
                        cluster.recentHitRate,
                        cluster.maxLoseStreak,
                        cluster.confidence,
                        cluster.volatility,
                        cluster.counterScore);
    }

    private Models.ClusterMatchResult toMatch(
            Models.DynamicWindowCluster c, double distance, String reason) {
        Models.ClusterMatchResult r = new Models.ClusterMatchResult();
        if (c == null) {
            r.reason = reason;
            return r;
        }
        r.clusterId = c.clusterId;
        r.clusterStatus = AppConfig.normalizeDynamicClusterStatus(c.status);
        r.prototypeKey = c.prototypeKey;
        r.distance = AppConfig.clampDynamicCausalScore(distance);
        r.sampleCount = c.sampleCount;
        r.recentSample = c.recentSample;
        r.hitRate = c.hitRate;
        r.recentHitRate = c.recentHitRate;
        r.confidence = c.confidence;
        r.counterScore = c.counterScore;
        r.maxLoseStreak = c.maxLoseStreak;
        r.reason =
                safe(reason)
                        + "｜sample="
                        + c.sampleCount
                        + "｜hit="
                        + percent(c.hitRate)
                        + "｜recent="
                        + percent(c.recentHitRate);
        return r;
    }

    private Models.ClusterMatchResult emptyMatch(String reason) {
        Models.ClusterMatchResult r = new Models.ClusterMatchResult();
        r.clusterStatus = AppConfig.CLUSTER_STATUS_UNKNOWN;
        r.distance = 1.0d;
        r.reason = reason;
        return r;
    }

    private void sortMatches(ArrayList<Models.ClusterMatchResult> list) {
        if (list == null || list.size() < 2) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<Models.ClusterMatchResult>() {
                    public int compare(Models.ClusterMatchResult a, Models.ClusterMatchResult b) {
                        double da = a == null ? 9.0d : a.distance;
                        double db = b == null ? 9.0d : b.distance;
                        if (da < db) return -1;
                        if (da > db) return 1;
                        double ca = a == null ? 0.0d : a.confidence;
                        double cb = b == null ? 0.0d : b.confidence;
                        if (ca > cb) return -1;
                        if (ca < cb) return 1;
                        return 0;
                    }
                });
    }

    private Models.WindowFeatureVector normalizeVector(
            Models.ComboCandidate c, Models.WindowFeatureVector vector, String targetIssue) {
        Models.WindowFeatureVector v =
                vector == null ? new Models.WindowFeatureVector() : vector.copy();
        if (c != null) {
            if (v.position <= 0) v.position = c.position;
            if (v.n1 <= 0) v.n1 = c.n1;
            if (v.n2 <= 0) v.n2 = c.n2;
            if (v.n3 <= 0) v.n3 = c.n3;
            if (v.targetIssue == null || v.targetIssue.length() == 0) {
                v.targetIssue = safe(c.targetIssue);
            }
            if (v.thirdQuality <= 0.0d) {
                v.thirdQuality = Math.max(c.thirdNumberQualityScore, c.needSupplyThirdQuality);
            }
            if (v.counterEvidence <= 0.0d) {
                v.counterEvidence =
                        Math.max(
                                c.v20RiskScore,
                                Math.max(c.needSupplyRisk, c.counterfactualWhyNotRisk));
            }
            if (v.densityScore <= 0.0d) {
                v.densityScore = Math.max(c.coverageQualityScore, c.needSupplyMatchScore);
            }
        }
        if (v.targetIssue == null || v.targetIssue.length() == 0) {
            v.targetIssue = safe(targetIssue);
        }
        normalizeVectorOnly(v);
        return v;
    }

    private void normalizeVectorOnly(Models.WindowFeatureVector v) {
        if (v == null) {
            return;
        }
        v.trendSlope12 = clampSigned(v.trendSlope12);
        v.trendSlope24 = clampSigned(v.trendSlope24);
        v.trendSlope48 = clampSigned(v.trendSlope48);
        v.trendSlope96 = clampSigned(v.trendSlope96);
        v.trendSlope360 = clampSigned(v.trendSlope360);
        v.trendLife = AppConfig.clampDynamicCausalScore(v.trendLife);
        v.trendFatigue = AppConfig.clampDynamicCausalScore(v.trendFatigue);
        v.omissionPressure = AppConfig.clampDynamicCausalScore(v.omissionPressure);
        v.hotColdTransition = clampSigned(v.hotColdTransition);
        v.densityScore = AppConfig.clampDynamicCausalScore(v.densityScore);
        v.repeatRhythm = AppConfig.clampDynamicCausalScore(v.repeatRhythm);
        v.oddEvenBias = AppConfig.clampDynamicCausalScore(v.oddEvenBias);
        v.highLowBias = AppConfig.clampDynamicCausalScore(v.highLowBias);
        v.tailPressure = AppConfig.clampDynamicCausalScore(v.tailPressure);
        v.neighborPressure = AppConfig.clampDynamicCausalScore(v.neighborPressure);
        v.crossPositionFlow = AppConfig.clampDynamicCausalScore(v.crossPositionFlow);
        v.crossPositionCrowd = AppConfig.clampDynamicCausalScore(v.crossPositionCrowd);
        v.sameNumberCrowd = AppConfig.clampDynamicCausalScore(v.sameNumberCrowd);
        v.timingOffset = clampSigned(v.timingOffset);
        v.counterEvidence = AppConfig.clampDynamicCausalScore(v.counterEvidence);
        v.thirdQuality = AppConfig.clampDynamicCausalScore(v.thirdQuality);
        try {
            v.rebuildKey();
        } catch (Throwable e) {
        }
    }

    private Models.DynamicWindowCluster clusterFromCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return null;
        }
        Models.DynamicWindowCluster cluster = new Models.DynamicWindowCluster();
        cluster.clusterId = safe(c.dynamicClusterId);
        cluster.status = AppConfig.normalizeDynamicClusterStatus(c.dynamicClusterStatus);
        cluster.prototypeKey = safe(c.dynamicClusterPrototypeKey);
        cluster.prototypeVector = safe(c.dynamicClusterFeatureKey);
        cluster.sampleCount = c.dynamicClusterSample;
        cluster.hitCount = c.dynamicClusterHitCount;
        cluster.missCount = c.dynamicClusterMissCount;
        cluster.recentSample = c.dynamicClusterRecentSample;
        cluster.recentHitCount = c.dynamicClusterRecentHitCount;
        cluster.hitRate = c.dynamicClusterHitRate;
        cluster.recentHitRate = c.dynamicClusterRecentHitRate;
        cluster.confidence = c.dynamicClusterConfidence;
        cluster.prototypeDistance = c.dynamicClusterDistance;
        cluster.volatility = c.dynamicClusterVolatility;
        cluster.maxLoseStreak = c.dynamicClusterMaxLose;
        cluster.counterScore = c.dynamicClusterCounterScore;
        cluster.reason = safe(c.dynamicClusterReason);
        if (cluster.clusterId.length() == 0) {
            Models.WindowFeatureVector v = normalizeVector(c, null, c.targetIssue);
            cluster = createNewCluster(v, c, "候选缺少clusterId，按当前窗口创建NEW簇");
        }
        return cluster;
    }

    private double promoteScore(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match) {
        if (cluster == null) {
            return 0.0d;
        }
        double score = 0.0d;
        score += lift(cluster.hitRate) * 0.28d;
        score += lift(cluster.recentHitRate) * 0.22d;
        score += cluster.confidence * 0.18d;
        score += (1.0d - cluster.counterScore) * 0.12d;
        score += (1.0d - cluster.volatility) * 0.10d;
        score += (match == null ? 0.0d : (1.0d - match.distance)) * 0.10d;
        if (c != null) {
            score += c.needSupplyMatchScore * 0.10d;
            score += c.needSupplyRoleDiversity * 0.04d;
            score -= c.needSupplyRisk * 0.08d;
            score -= c.counterfactualWhyNotRisk * 0.06d;
        }
        return AppConfig.clampDynamicCausalScore(score);
    }

    private double freezeScore(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match) {
        if (cluster == null) {
            return 0.50d;
        }
        double score = 0.0d;
        if (cluster.hitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE) score += 0.32d;
        if (cluster.recentSample >= AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                && cluster.recentHitRate < AppConfig.DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE)
            score += 0.20d;
        if (cluster.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE) score += 0.18d;
        score += cluster.counterScore * 0.18d;
        score += cluster.volatility * 0.12d;
        score += (match == null ? 0.0d : match.distance) * 0.10d;
        if (c != null) {
            score += c.needSupplyRisk * 0.08d;
            score += c.counterfactualWhyNotRisk * 0.08d;
            score += c.causeAblationScore * 0.06d;
        }
        return AppConfig.clampDynamicCausalScore(score);
    }

    private double causeScore(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match) {
        double score = 0.0d;
        if (cluster != null) {
            score += lift(cluster.hitRate) * 0.30d;
            score += lift(cluster.recentHitRate) * 0.22d;
            score += cluster.confidence * 0.20d;
        }
        if (match != null) {
            score += (1.0d - match.distance) * 0.10d;
        }
        if (c != null) {
            score += c.needSupplyMatchScore * 0.12d;
            score += c.needSupplyThirdQuality * 0.06d;
        }
        return AppConfig.clampDynamicCausalScore(score);
    }

    private double counterScore(
            Models.ComboCandidate c,
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match) {
        double score = 0.0d;
        if (cluster != null) {
            score += cluster.counterScore * 0.30d;
            score += cluster.volatility * 0.18d;
            if (cluster.hitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE) score += 0.20d;
            if (cluster.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_MAX_LOSE_READY) score += 0.12d;
        }
        if (match != null) {
            score += match.distance * 0.10d;
        }
        if (c != null) {
            score += c.needSupplyRisk * 0.10d;
            score += c.counterfactualWhyNotRisk * 0.10d;
        }
        return AppConfig.clampDynamicCausalScore(score);
    }

    private double lift(double rate) {
        return AppConfig.clampDynamicCausalScore(
                (rate - AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE + 0.120d) / 0.240d);
    }

    private double confidenceFromVector(Models.WindowFeatureVector v, Models.ComboCandidate c) {
        double s = 0.0d;
        if (v != null) {
            s += (1.0d - Math.abs(v.trendSlope24 - v.trendSlope96)) * 0.16d;
            s += v.densityScore * 0.12d;
            s += (1.0d - v.counterEvidence) * 0.18d;
            s += v.thirdQuality * 0.12d;
            s += (1.0d - volatilityOf(v)) * 0.14d;
        }
        if (c != null) {
            s += c.needSupplyMatchScore * 0.16d;
            s += c.needSupplyRoleDiversity * 0.06d;
            s += (1.0d - c.needSupplyRisk) * 0.08d;
        }
        return AppConfig.clampDynamicCausalScore(s);
    }

    private double confidenceFromCluster(Models.DynamicWindowCluster c) {
        if (c == null) {
            return 0.0d;
        }
        double sampleScore =
                Math.min(
                        1.0d,
                        ((double) c.sampleCount) / AppConfig.DYNAMIC_CLUSTER_MIN_SAMPLE_STRONG);
        double recentScore =
                c.recentSample <= 0
                        ? 0.45d
                        : Math.min(
                                1.0d,
                                ((double) c.recentSample)
                                        / AppConfig.DYNAMIC_CLUSTER_RECENT_STRONG_SAMPLE);
        double liftScore = lift(c.hitRate);
        double recentLift = lift(c.recentHitRate);
        double loseScore =
                1.0d
                        - Math.min(
                                1.0d,
                                ((double) c.maxLoseStreak)
                                        / Math.max(1, AppConfig.DYNAMIC_CLUSTER_FREEZE_LOSE));
        return AppConfig.clampDynamicCausalScore(
                sampleScore * 0.26d
                        + recentScore * 0.16d
                        + liftScore * 0.22d
                        + recentLift * 0.18d
                        + loseScore * 0.18d);
    }

    private double counterAfterOutcome(Models.DynamicWindowCluster c, boolean hit) {
        if (c == null) {
            return 0.0d;
        }
        double score = c.counterScore * 0.72d;
        if (!hit) {
            score += 0.10d;
        }
        if (c.hitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE) {
            score += 0.12d;
        }
        if (c.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_MAX_LOSE_READY) {
            score += 0.10d;
        }
        if (c.recentSample >= AppConfig.DYNAMIC_CLUSTER_RECENT_MIN_SAMPLE
                && c.recentHitRate < AppConfig.DYNAMIC_CLUSTER_RECENT_FREEZE_HIT_RATE) {
            score += 0.10d;
        }
        return AppConfig.clampDynamicCausalScore(score);
    }

    private double riskFromCluster(Models.DynamicWindowCluster c) {
        if (c == null) {
            return 0.0d;
        }
        double s = c.counterScore * 0.40d + c.volatility * 0.20d;
        if (c.hitRate < AppConfig.DYNAMIC_CLUSTER_RANDOM_BASELINE) s += 0.20d;
        if (c.maxLoseStreak >= AppConfig.DYNAMIC_CLUSTER_MAX_LOSE_READY) s += 0.20d;
        return AppConfig.clampDynamicCausalScore(s);
    }

    private double volatilityOf(Models.WindowFeatureVector v) {
        if (v == null) {
            return 0.0d;
        }
        double a = Math.abs(v.trendSlope12 - v.trendSlope48);
        double b = Math.abs(v.trendSlope24 - v.trendSlope96);
        double c = Math.abs(v.hotColdTransition);
        double d = Math.abs(v.timingOffset);
        return AppConfig.clampDynamicCausalScore((a + b + c + d) / 4.0d);
    }

    private double midFromPrototype(String proto, String prefix) {
        if (proto == null || prefix == null) {
            return 0.50d;
        }
        String keyH = prefix + "H";
        String keyM = prefix + "M";
        String keyL = prefix + "L";
        String keyZ = prefix + "Z";
        if (proto.indexOf(keyH) >= 0) return 0.82d;
        if (proto.indexOf(keyM) >= 0) return 0.60d;
        if (proto.indexOf(keyL) >= 0) return 0.38d;
        if (proto.indexOf(keyZ) >= 0) return 0.16d;
        return 0.50d;
    }

    private String detectMissType(Models.MissReasonResult m) {
        if (m == null) {
            return AppConfig.MISS_REASON_UNKNOWN;
        }
        double best = m.windowWrongScore;
        String type = AppConfig.MISS_REASON_WINDOW_WRONG;
        if (m.positionWrongScore > best) {
            best = m.positionWrongScore;
            type = AppConfig.MISS_REASON_POSITION_WRONG;
        }
        if (m.mainNumberWrongScore > best) {
            best = m.mainNumberWrongScore;
            type = AppConfig.MISS_REASON_MAIN_NUMBER_WRONG;
        }
        if (m.thirdDragScore > best) {
            best = m.thirdDragScore;
            type = AppConfig.MISS_REASON_THIRD_DRAG;
        }
        if (m.counterIgnoredScore > best) {
            type = AppConfig.MISS_REASON_COUNTER_IGNORED;
        }
        return type;
    }

    private String nextAdviceForMiss(Models.MissReasonResult m) {
        if (m == null) {
            return "";
        }
        if (AppConfig.MISS_REASON_WINDOW_WRONG.equals(m.missType)) {
            return "下次降低该窗口簇权重，要求更近相似度和更高近期有效性。";
        }
        if (AppConfig.MISS_REASON_POSITION_WRONG.equals(m.missType)) {
            return "下次加强位置需求排序，位置需求混乱时只允许WATCH。";
        }
        if (AppConfig.MISS_REASON_MAIN_NUMBER_WRONG.equals(m.missType)) {
            return "下次检查主号供给，主号供给不足时禁止晋级。";
        }
        if (AppConfig.MISS_REASON_THIRD_DRAG.equals(m.missType)) {
            return "下次提高第三号质量要求，第三号拖拽时降级。";
        }
        if (AppConfig.MISS_REASON_COUNTER_IGNORED.equals(m.missType)) {
            return "下次把反事实反证权重上调，反证强时冻结。";
        }
        return "保持观察，不进入真实BET。";
    }

    private String buildJudgeReason(
            Models.DynamicWindowCluster cluster,
            Models.ClusterMatchResult match,
            Models.AdaptiveJudgeResult judge,
            boolean needAllows) {
        String status = cluster == null ? AppConfig.CLUSTER_STATUS_UNKNOWN : cluster.status;
        double hit = cluster == null ? 0.0d : cluster.hitRate;
        double recent = cluster == null ? 0.0d : cluster.recentHitRate;
        int sample = cluster == null ? 0 : cluster.sampleCount;
        int maxLose = cluster == null ? 0 : cluster.maxLoseStreak;
        double dist = match == null ? 1.0d : match.distance;
        String action = judge == null ? AppConfig.ADAPTIVE_ACTION_WATCH : judge.action;

        return "动态窗口簇审判="
                + action
                + "｜status="
                + status
                + "｜sample="
                + sample
                + "｜hit="
                + percent(hit)
                + "｜recent="
                + percent(recent)
                + "｜distance="
                + percent(dist)
                + "｜maxLose="
                + maxLose
                + "｜NeedSupply="
                + (needAllows ? "PASS" : "WATCH")
                + "｜低于随机/样本不足/近期转弱/连挂过高均禁止晋级"
                + "｜真实BET仍由DecisionGateEngine唯一裁决";
    }

    private String buildWhy(
            Models.ComboCandidate c,
            Models.ClusterMatchResult match,
            Models.AdaptiveJudgeResult judge) {
        String action = judge == null ? AppConfig.ADAPTIVE_ACTION_WATCH : judge.action;
        String status = judge == null ? AppConfig.CLUSTER_STATUS_UNKNOWN : judge.status;
        double ns = c == null ? 0.0d : c.needSupplyMatchScore;
        double third = c == null ? 0.0d : c.needSupplyThirdQuality;
        double supplyRisk = c == null ? 0.0d : c.needSupplyRisk;
        double distance = match == null ? 1.0d : match.distance;
        if (judge != null && AppConfig.adaptiveActionAllowsBet(action)) {
            return "动态簇可送最终门复核：action="
                    + action
                    + "｜status="
                    + status
                    + "｜NeedSupply="
                    + percent(ns)
                    + "｜third="
                    + percent(third)
                    + "｜supplyRisk="
                    + percent(supplyRisk)
                    + "｜distance="
                    + percent(distance)
                    + "｜最终BET仍由DecisionGateEngine裁决";
        }
        return "动态簇暂不晋级：action="
                + action
                + "｜status="
                + status
                + "｜NeedSupply="
                + percent(ns)
                + "｜third="
                + percent(third)
                + "｜supplyRisk="
                + percent(supplyRisk)
                + "｜distance="
                + percent(distance)
                + "｜原因=未形成稳定高于随机基准的同类窗口或存在反证";
    }

    private String buildOutcomeReason(
            Models.DynamicWindowCluster cluster,
            boolean hit,
            int actualNumber,
            String resultIssue) {
        if (cluster == null) {
            return "";
        }
        return "动态簇结算学习｜issue="
                + safe(resultIssue)
                + "｜hit="
                + (hit ? "WIN" : "MISS")
                + "｜actual="
                + actualNumber
                + "｜sample="
                + cluster.sampleCount
                + "｜hitRate="
                + percent(cluster.hitRate)
                + "｜recent="
                + percent(cluster.recentHitRate)
                + "｜maxLose="
                + cluster.maxLoseStreak
                + "｜status="
                + cluster.status
                + "｜此处只学习SHADOW/候选证据，不写真账本";
    }

    private String vectorSummary(Models.WindowFeatureVector v) {
        if (v == null) {
            return "";
        }
        return "P"
                + v.position
                + "|K"
                + AppConfig.normalizeK3Part(v.n1, v.n2, v.n3)
                + "|trend="
                + signedBucket(v.trendSlope24 + v.trendSlope48 + v.trendSlope96)
                + "|om="
                + bucket(v.omissionPressure)
                + "|den="
                + bucket(v.densityScore)
                + "|rep="
                + bucket(v.repeatRhythm)
                + "|tail="
                + bucket(v.tailPressure)
                + "|cross="
                + bucket(v.crossPositionCrowd)
                + "|counter="
                + bucket(v.counterEvidence)
                + "|third="
                + bucket(v.thirdQuality);
    }

    private String bucket(double v) {
        double x = AppConfig.clampDynamicCausalScore(v);
        if (x >= 0.750d) return "H";
        if (x >= 0.550d) return "M";
        if (x >= 0.350d) return "L";
        return "Z";
    }

    private String signedBucket(double v) {
        if (v >= 0.180d) return "UP";
        if (v <= -0.180d) return "DOWN";
        if (Math.abs(v) <= 0.060d) return "FLAT";
        return v > 0.0d ? "WEAK_UP" : "WEAK_DOWN";
    }

    private double clampSigned(double v) {
        if (v > 1.0d) return 1.0d;
        if (v < -1.0d) return -1.0d;
        return v;
    }

    private String percent(double v) {
        return String.format(java.util.Locale.US, "%.1f%%", v * 100.0d);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
