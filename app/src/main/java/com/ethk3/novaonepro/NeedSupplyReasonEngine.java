package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * V20 第7/14：NeedSupplyReasonEngine
 *
 * <p>核心目标： 把“这个位置渴了，这些号码是水”做成可计算、可解释、可落库、可回放的实体链路。
 *
 * <p>位置需求 PositionNeedState： - 这个位置当前到底需要什么结构补充。
 *
 * <p>号码供给 NumberSupplyState： - 每个号码能提供什么角色和风险。
 *
 * <p>NeedSupplyMatchResult： - 三个号码是否组成主攻 / 辅助 / 防守的有效K3角色结构。
 *
 * <p>CounterfactualReasonResult： - 为什么是这个位置、为什么是这三个号码、为什么不是其他位置/号码。
 *
 * <p>CauseAblationResult： - 因子剥离：去掉某个原因后是否还成立，防止假因。
 *
 * <p>红线： - 不创建 Trade。 - 不结算。 - 不写真账本。 - 不改真实盈亏。 - 不绕过 DecisionGateEngine。 - 只给 DecisionGateEngine
 * 提供证据字段。
 */
public final class NeedSupplyReasonEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final int MAX_POSITION = 10;
    private static final int MIN_NUMBER = 1;
    private static final int MAX_NUMBER = 10;
    private static final double K3_BASELINE = 0.300d;
    private static final double NUM_BASELINE = 0.100d;
    private static final double EPS = 0.000001d;

    private final DbStore store;

    public NeedSupplyReasonEngine() {
        this(null);
    }

    public NeedSupplyReasonEngine(DbStore s) {
        store = s;
    }

    public String getVersion() {
        return VERSION;
    }

    public NeedSupplyBundle analyzeAndApply(
            Models.ComboCandidate c,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        if (c == null) {
            return NeedSupplyBundle.empty("EMPTY_CANDIDATE");
        }

        ArrayList<Models.Draw> clean = cleanHistory(history);
        NeedSupplyBundle b = analyzeCandidate(c, clean, baseIssue, targetIssue);
        applyBundleToCandidate(c, b);
        saveBundle(c, b, baseIssue, targetIssue);
        return b;
    }

    public ArrayList<NeedSupplyBundle> analyzeCandidates(
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<NeedSupplyBundle> out = new ArrayList<NeedSupplyBundle>();
        if (candidates == null || candidates.size() == 0) {
            return out;
        }
        ArrayList<Models.Draw> clean = cleanHistory(history);
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            NeedSupplyBundle b = analyzeCandidate(c, clean, baseIssue, targetIssue);
            applyBundleToCandidate(c, b);
            saveBundle(c, b, baseIssue, targetIssue);
            out.add(b);
        }
        sortBundles(out);
        return out;
    }

    public NeedSupplyBundle analyzeCandidate(
            Models.ComboCandidate c,
            List<Models.Draw> clean,
            String baseIssue,
            String targetIssue) {
        if (c == null) {
            return NeedSupplyBundle.empty("EMPTY_CANDIDATE");
        }

        NeedSupplyBundle b = new NeedSupplyBundle();
        b.baseIssue = safe(baseIssue);
        b.targetIssue = firstNonEmpty(targetIssue, c.targetIssue);
        b.position = normalizePosition(c.position);
        b.n1 = normalizeNumber(c.n1);
        b.n2 = normalizeNumber(c.n2);
        b.n3 = normalizeNumber(c.n3);
        b.k3Mask = buildK3Mask(b.n1, b.n2, b.n3);

        b.need = buildPositionNeedState(b.position, clean, b.targetIssue, c);
        b.supplyMain =
                buildNumberSupplyState(b.position, b.n1, clean, b.targetIssue, b.need, "MAIN", c);
        b.supplyAssist =
                buildNumberSupplyState(b.position, b.n2, clean, b.targetIssue, b.need, "ASSIST", c);
        b.supplyDefense =
                buildNumberSupplyState(
                        b.position, b.n3, clean, b.targetIssue, b.need, "DEFENSE", c);
        b.match =
                buildNeedSupplyMatch(
                        b.need, b.supplyMain, b.supplyAssist, b.supplyDefense, c, b.targetIssue);
        b.counterfactual = buildCounterfactualReason(b, clean, c);
        b.ablation = buildCauseAblation(b, clean, c);
        b.finalScore = finalNeedSupplyScore(b);
        b.finalRisk = finalNeedSupplyRisk(b);
        b.action = decideAction(b);
        b.summary = buildBundleSummary(b);
        return b;
    }

    public Models.PositionNeedState buildPositionNeedState(
            int position, List<Models.Draw> clean, String targetIssue, Models.ComboCandidate c) {
        Models.PositionNeedState p = new Models.PositionNeedState();
        p.position = normalizePosition(position);
        p.targetIssue = safe(targetIssue);

        double recentHit12 = positionK3Rate(p.position, c, clean, 12);
        double recentHit24 = positionK3Rate(p.position, c, clean, 24);
        double hit48 = positionK3Rate(p.position, c, clean, 48);
        double hit96 = positionK3Rate(p.position, c, clean, 96);

        double numberHot = candidateNumberHeat(p.position, c, clean, 96);
        double numberCold = 1.0d - numberHot;
        double omission = candidateOmissionPressure(p.position, c, clean);
        double positionChaos = positionChaosRisk(p.position, clean, 48);
        double trendLift = clampSigned(recentHit24 - hit96);
        double shortBurst = clampSigned(recentHit12 - hit48);

        p.needHotContinue =
                clamp01(
                        Math.max(0.0d, recentHit24 - K3_BASELINE) * 2.4d
                                + Math.max(0.0d, shortBurst) * 1.2d);
        p.needColdRepair = clamp01(Math.max(0.0d, K3_BASELINE - hit96) * 2.2d + numberCold * 0.18d);
        p.needOmissionReturn = clamp01(omission);
        p.needHighLowBalance = highLowNeed(p.position, clean);
        p.needOddEvenBalance = oddEvenNeed(p.position, clean);
        p.needTailRepair = tailNeed(p.position, clean);
        p.needNeighborRelay = neighborNeed(p.position, clean);
        p.needCrossPositionFlow = crossPositionNeed(p.position, c, clean);
        p.needCrowdRelease = crowdReleaseNeed(c, clean);
        p.needReverseBreak =
                clamp01(Math.max(0.0d, trendLift) * 1.6d + Math.max(0.0d, shortBurst) * 0.7d);
        p.needStableRepeat = stableRepeatNeed(p.position, clean);
        p.needChaosAvoid = clamp01(1.0d - positionChaos);
        p.chaosRisk = positionChaos;
        p.summary =
                "位置需求｜P"
                        + p.position
                        + "｜主需求="
                        + mainNeedText(p)
                        + "｜热延续="
                        + percent(p.needHotContinue)
                        + "｜冷修复="
                        + percent(p.needColdRepair)
                        + "｜遗漏回补="
                        + percent(p.needOmissionReturn)
                        + "｜跨位流入="
                        + percent(p.needCrossPositionFlow)
                        + "｜避乱="
                        + percent(p.needChaosAvoid);
        try {
            p.recalc();
        } catch (Throwable e) {
        }
        return p;
    }

    public Models.NumberSupplyState buildNumberSupplyState(
            int position,
            int number,
            List<Models.Draw> clean,
            String targetIssue,
            Models.PositionNeedState need,
            String roleName,
            Models.ComboCandidate c) {
        Models.NumberSupplyState n = new Models.NumberSupplyState();
        n.position = normalizePosition(position);
        n.number = normalizeNumber(number);
        n.targetIssue = safe(targetIssue);

        double rate12 = numberRate(n.position, n.number, clean, 12);
        double rate24 = numberRate(n.position, n.number, clean, 24);
        double rate48 = numberRate(n.position, n.number, clean, 48);
        double rate96 = numberRate(n.position, n.number, clean, 96);
        double omission = numberOmissionPressure(n.position, n.number, clean);
        double crossFlow = numberCrossPositionFlow(n.number, clean, 24);
        double repeat = numberStableRepeat(n.position, n.number, clean, 24);
        double neigh = numberNeighborRelay(n.position, n.number, clean, 24);
        double tail = numberTailRepair(n.number, clean, 48);

        n.supplyHotContinue =
                clamp01(
                        Math.max(0.0d, rate24 - NUM_BASELINE) * 3.2d
                                + Math.max(0.0d, rate12 - rate48) * 1.5d);
        n.supplyColdRepair =
                clamp01(Math.max(0.0d, NUM_BASELINE - rate96) * 2.6d + omission * 0.28d);
        n.supplyOmissionReturn = clamp01(omission);
        n.supplyHighLowBalance = supplyHighLow(n.number);
        n.supplyOddEvenBalance = supplyOddEven(n.number);
        n.supplyTailRepair = clamp01(tail);
        n.supplyNeighborRelay = clamp01(neigh);
        n.supplyCrossPositionFlow = clamp01(crossFlow);
        n.supplyCrowdRelease = clamp01(1.0d - numberCrowdRisk(n.number, clean, 24));
        n.supplyReverseBreak =
                clamp01(
                        Math.max(0.0d, rate12 - rate96) * 2.0d
                                + Math.max(0.0d, rate24 - rate96) * 1.0d);
        n.supplyStableRepeat = clamp01(repeat);
        n.supplyRisk = numberSupplyRisk(n, need, rate12, rate24, rate96, clean);
        n.summary =
                roleName
                        + "号码供给｜N"
                        + n.number
                        + "｜主供给="
                        + mainSupplyText(n)
                        + "｜rate96="
                        + percent(rate96)
                        + "｜遗漏="
                        + percent(n.supplyOmissionReturn)
                        + "｜邻号="
                        + percent(n.supplyNeighborRelay)
                        + "｜跨位="
                        + percent(n.supplyCrossPositionFlow)
                        + "｜风险="
                        + percent(n.supplyRisk);
        try {
            n.recalc();
        } catch (Throwable e) {
        }
        return n;
    }

    public Models.NeedSupplyMatchResult buildNeedSupplyMatch(
            Models.PositionNeedState need,
            Models.NumberSupplyState s1,
            Models.NumberSupplyState s2,
            Models.NumberSupplyState s3,
            Models.ComboCandidate c,
            String targetIssue) {
        Models.NeedSupplyMatchResult r = new Models.NeedSupplyMatchResult();
        r.targetIssue = safe(targetIssue);
        r.position = c == null ? (need == null ? 0 : need.position) : c.position;
        r.n1 = s1 == null ? 0 : s1.number;
        r.n2 = s2 == null ? 0 : s2.number;
        r.n3 = s3 == null ? 0 : s3.number;
        r.k3Mask = buildK3Mask(r.n1, r.n2, r.n3);

        RolePick[] picks = rankRoles(need, s1, s2, s3);
        RolePick main = picks[0];
        RolePick assist = picks[1];
        RolePick defense = picks[2];

        r.mainRoleNumber = main.number;
        r.assistRoleNumber = assist.number;
        r.defenseRoleNumber = defense.number;
        r.mainRole = "MAIN_" + main.type;
        r.assistRole = "ASSIST_" + assist.type;
        r.defenseRole = "DEFENSE_" + defense.type;
        r.mainRoleScore = main.score;
        r.assistRoleScore = assist.score;
        r.defenseRoleScore = defense.score;
        r.thirdQuality = thirdQuality(main, assist, defense, need);
        r.roleDiversity = roleDiversity(main, assist, defense);
        r.supplyRisk = avgRisk(s1, s2, s3);
        r.matchScore =
                clamp01(
                        r.mainRoleScore * 0.42d
                                + r.assistRoleScore * 0.27d
                                + r.defenseRoleScore * 0.18d
                                + r.thirdQuality * 0.08d
                                + r.roleDiversity * 0.05d
                                - r.supplyRisk * 0.16d);

        r.whyMatch =
                "NeedSupply匹配｜主="
                        + r.mainRole
                        + "("
                        + r.mainRoleNumber
                        + "/"
                        + percent(r.mainRoleScore)
                        + ")"
                        + "｜辅="
                        + r.assistRole
                        + "("
                        + r.assistRoleNumber
                        + "/"
                        + percent(r.assistRoleScore)
                        + ")"
                        + "｜防="
                        + r.defenseRole
                        + "("
                        + r.defenseRoleNumber
                        + "/"
                        + percent(r.defenseRoleScore)
                        + ")"
                        + "｜第三号="
                        + percent(r.thirdQuality)
                        + "｜角色差异="
                        + percent(r.roleDiversity);

        r.whyNotMatch = buildWhyNotMatch(r, need);
        r.summary =
                r.whyMatch
                        + "｜总匹配="
                        + percent(r.matchScore)
                        + "｜供给风险="
                        + percent(r.supplyRisk)
                        + "｜"
                        + r.whyNotMatch;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    public Models.CounterfactualReasonResult buildCounterfactualReason(
            NeedSupplyBundle b, List<Models.Draw> clean, Models.ComboCandidate c) {
        Models.CounterfactualReasonResult r = new Models.CounterfactualReasonResult();
        if (b == null) {
            return r;
        }
        r.targetIssue = b.targetIssue;
        r.position = b.position;
        r.n1 = b.n1;
        r.n2 = b.n2;
        r.n3 = b.n3;
        r.k3Mask = b.k3Mask;
        r.positionRank = positionNeedRank(b.position, b.need, clean);
        r.k3Rank = k3SupplyRank(b.match);
        r.rankScore =
                clamp01(
                        (11 - Math.min(10, r.positionRank)) / 10.0d * 0.34d
                                + (11 - Math.min(10, r.k3Rank)) / 10.0d * 0.30d
                                + (b.match == null ? 0.0d : b.match.matchScore) * 0.26d
                                + (b.ablation == null ? 0.0d : b.ablation.totalAblationScore)
                                        * 0.10d);
        r.mirrorMissSimilarity = mirrorMissSimilarity(b, clean);
        r.whyNotRisk =
                clamp01(
                        (1.0d - r.rankScore) * 0.46d
                                + r.mirrorMissSimilarity * 0.30d
                                + (b.match == null ? 0.20d : b.match.supplyRisk) * 0.24d);
        r.causeScore = b.match == null ? 0.0d : b.match.matchScore;
        r.counterScore = r.whyNotRisk;
        r.whyThisPosition =
                "为什么这个位置：P"
                        + b.position
                        + " 的需求排名="
                        + r.positionRank
                        + "，主需求="
                        + (b.need == null ? "" : b.need.mainNeedType)
                        + "，需求强度="
                        + percent(b.need == null ? 0.0d : b.need.totalNeedStrength);
        r.whyTheseNumbers =
                "为什么这三个号码：K3="
                        + AppConfig.normalizeK3Part(b.n1, b.n2, b.n3)
                        + "，组合供给排名="
                        + r.k3Rank
                        + "，NeedSupply="
                        + percent(b.match == null ? 0.0d : b.match.matchScore);
        r.whyNotOtherPosition = "为什么不是其他位置：其他位置需要重新满足同一需求结构；当前只允许单位置，未达到更高需求排名的不抢位。";
        r.whyNotOtherNumbers = "为什么不是其他号码：其他号码若不能同时覆盖主攻/辅助/防守三角色，或第三号拖拽/镜像未中相似度高，则降级。";
        r.summary =
                r.whyThisPosition
                        + "｜"
                        + r.whyTheseNumbers
                        + "｜whyNotRisk="
                        + percent(r.whyNotRisk)
                        + "｜mirrorMiss="
                        + percent(r.mirrorMissSimilarity);
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    public Models.CauseAblationResult buildCauseAblation(
            NeedSupplyBundle b, List<Models.Draw> clean, Models.ComboCandidate c) {
        Models.CauseAblationResult r = new Models.CauseAblationResult();
        if (b == null) {
            return r;
        }
        r.clusterId = c == null ? "" : safe(c.dynamicClusterId);
        r.sampleCount = Math.min(clean == null ? 0 : clean.size(), 96);
        r.omissionDrop = ablationDrop(b, "OMISSION");
        r.tailDrop = ablationDrop(b, "TAIL");
        r.neighborDrop = ablationDrop(b, "NEIGHBOR");
        r.crossPositionDrop = ablationDrop(b, "CROSS");
        r.needSupplyDrop =
                clamp01(
                        AppConfig.NEED_SUPPLY_READY_MATCH_SCORE
                                - (b.match == null ? 0.0d : b.match.matchScore));
        r.thirdQualityDrop =
                clamp01(
                        AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY
                                - (b.match == null ? 0.0d : b.match.thirdQuality));

        r.falseCauseCount = 0;
        if (r.omissionDrop < AppConfig.CAUSE_ABLATION_FALSE_CAUSE_DROP) r.falseCauseCount++;
        if (r.tailDrop < AppConfig.CAUSE_ABLATION_FALSE_CAUSE_DROP) r.falseCauseCount++;
        if (r.neighborDrop < AppConfig.CAUSE_ABLATION_FALSE_CAUSE_DROP) r.falseCauseCount++;
        if (r.crossPositionDrop < AppConfig.CAUSE_ABLATION_FALSE_CAUSE_DROP) r.falseCauseCount++;
        if (b.match != null && b.match.matchScore < AppConfig.NEED_SUPPLY_MIN_MATCH_SCORE)
            r.falseCauseCount++;

        r.falseCauseSummary =
                "因子剥离｜遗漏drop="
                        + percent(r.omissionDrop)
                        + "｜同尾drop="
                        + percent(r.tailDrop)
                        + "｜邻号drop="
                        + percent(r.neighborDrop)
                        + "｜跨位drop="
                        + percent(r.crossPositionDrop)
                        + "｜NeedSupply缺口="
                        + percent(r.needSupplyDrop)
                        + "｜第三号缺口="
                        + percent(r.thirdQualityDrop)
                        + "｜疑似假因="
                        + r.falseCauseCount;

        r.summary = r.falseCauseSummary;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    public Models.AdaptiveJudgeResult buildNeedSupplyJudge(
            NeedSupplyBundle b, Models.ComboCandidate c) {
        Models.AdaptiveJudgeResult r = new Models.AdaptiveJudgeResult();
        r.targetIssue = b == null ? "" : b.targetIssue;
        r.candidateCount = 1;
        r.clusterId = c == null ? "" : safe(c.dynamicClusterId);
        r.clusterStatus =
                c == null ? AppConfig.CLUSTER_STATUS_UNKNOWN : safe(c.dynamicClusterStatus);
        r.status = AppConfig.CLUSTER_STATUS_WATCH;
        r.clusterHitRate = c == null ? 0.0d : c.dynamicClusterHitRate;
        r.clusterRecentHitRate = c == null ? 0.0d : c.dynamicClusterRecentHitRate;
        r.clusterSample = c == null ? 0 : c.dynamicClusterSample;
        r.clusterConfidence = c == null ? 0.0d : c.dynamicClusterConfidence;
        r.causeScore = b == null ? 0.0d : b.finalScore;
        r.counterScore = b == null ? 1.0d : b.finalRisk;
        r.promoteScore =
                b == null
                        ? 0.0d
                        : clamp01(
                                b.finalScore * 0.58d
                                        + (1.0d - b.finalRisk) * 0.26d
                                        + (b.match == null ? 0.0d : b.match.roleDiversity) * 0.16d);
        r.freezeScore =
                b == null
                        ? 1.0d
                        : clamp01(
                                b.finalRisk * 0.62d
                                        + (b.ablation == null
                                                        ? 0.0d
                                                        : b.ablation.falseCauseCount / 5.0d)
                                                * 0.22d
                                        + (b.counterfactual == null
                                                        ? 0.0d
                                                        : b.counterfactual.whyNotRisk)
                                                * 0.16d);

        if (r.freezeScore >= 0.680d
                || (b != null && b.finalRisk >= AppConfig.NEED_SUPPLY_HARD_RISK)) {
            r.action = AppConfig.ADAPTIVE_ACTION_FREEZE;
            r.domain = "NEED_SUPPLY_FREEZE";
            r.status = AppConfig.CLUSTER_STATUS_FREEZE;
        } else if (b != null && b.actionAllowsGate()) {
            r.action = AppConfig.ADAPTIVE_ACTION_READY;
            r.domain = "NEED_SUPPLY_READY";
            r.status = AppConfig.CLUSTER_STATUS_READY;
        } else {
            r.action = AppConfig.ADAPTIVE_ACTION_WATCH;
            r.domain = "NEED_SUPPLY_WATCH";
            r.status = AppConfig.CLUSTER_STATUS_WATCH;
        }
        r.reason =
                "NeedSupply审判="
                        + r.action
                        + "｜score="
                        + percent(b == null ? 0.0d : b.finalScore)
                        + "｜risk="
                        + percent(b == null ? 1.0d : b.finalRisk)
                        + "｜match="
                        + percent(b == null || b.match == null ? 0.0d : b.match.matchScore)
                        + "｜third="
                        + percent(b == null || b.match == null ? 0.0d : b.match.thirdQuality)
                        + "｜role="
                        + percent(b == null || b.match == null ? 0.0d : b.match.roleDiversity)
                        + "｜只给DecisionGate证据，不直接BET";
        r.version = VERSION;
        try {
            r.recalc();
        } catch (Throwable e) {
        }
        return r;
    }

    private void applyBundleToCandidate(Models.ComboCandidate c, NeedSupplyBundle b) {
        if (c == null || b == null) {
            return;
        }
        try {
            c.applyNeedSupply(b.match);
        } catch (Throwable e) {
        }
        try {
            c.applyCounterfactualReason(b.counterfactual);
        } catch (Throwable e) {
        }
        try {
            Models.CauseAblationResult ab = b.ablation;
            if (ab != null) {
                c.causeAblationKey = ab.ablationKey;
                c.causeAblationScore = ab.totalAblationScore;
                c.causeAblationFalseCauseCount = ab.falseCauseCount;
                c.causeAblationSummary = ab.summary;
            }
        } catch (Throwable e) {
        }

        Models.AdaptiveJudgeResult judge = buildNeedSupplyJudge(b, c);
        try {
            c.applyAdaptiveJudge(judge);
        } catch (Throwable e) {
        }

        c.positionNeedKey = b.need == null ? "" : b.need.needKey;
        c.positionNeedSummary = b.need == null ? "" : b.need.summary;
        c.positionNeedStrength = b.need == null ? 0.0d : b.need.totalNeedStrength;
        c.positionNeedChaosRisk = b.need == null ? 0.0d : b.need.chaosRisk;
        c.positionNeedRepairScore =
                b.need == null ? 0.0d : clamp01(b.need.needColdRepair + b.need.needOmissionReturn);
        c.positionNeedRelayScore =
                b.need == null
                        ? 0.0d
                        : clamp01(b.need.needNeighborRelay + b.need.needCrossPositionFlow);

        c.numberSupplyKey = b.supplyMain == null ? "" : b.supplyMain.supplyKey;
        c.numberSupplySummary = buildSupplySummary(b);
        c.numberSupplyStrength =
                clamp01(
                        (supplyStrength(b.supplyMain)
                                        + supplyStrength(b.supplyAssist)
                                        + supplyStrength(b.supplyDefense))
                                / 3.0d);
        c.numberSupplyRisk =
                clamp01(
                        (supplyRisk(b.supplyMain)
                                        + supplyRisk(b.supplyAssist)
                                        + supplyRisk(b.supplyDefense))
                                / 3.0d);

        c.whyBet = b.actionAllowsGate() ? "NeedSupply可送最终门复核｜" + b.summary : "";
        c.whyNoBet = b.actionAllowsGate() ? "" : "NeedSupply暂不晋级｜" + b.summary;
        c.dynamicCausalSummary = b.summary;
        try {
            Models.normalizeDynamicCausalBridgeFields(c);
        } catch (Throwable e) {
        }
    }

    private void saveBundle(
            Models.ComboCandidate c, NeedSupplyBundle b, String baseIssue, String targetIssue) {
        if (store == null || b == null) {
            return;
        }
        try {
            store.savePositionNeedSample(b.need, 0);
            store.saveNumberSupplySample(b.supplyMain, 0, false);
            store.saveNumberSupplySample(b.supplyAssist, 0, false);
            store.saveNumberSupplySample(b.supplyDefense, 0, false);
            store.saveNeedSupplyMatchSample(b.match);
            store.saveCounterfactualReasonSample(
                    b.counterfactual, c == null ? "" : c.dynamicClusterId, false);
            store.saveCauseAblationSample(b.ablation);
            store.saveAdaptiveJudgeSample(buildNeedSupplyJudge(b, c));
            store.updateDynamicCausalState(
                    AppConfig.STATE_NEED_SUPPLY_LAST_RUN,
                    String.valueOf(System.currentTimeMillis()));
            store.updateDynamicCausalState(
                    AppConfig.STATE_DYNAMIC_CAUSAL_DIAGNOSIS,
                    "NeedSupplyReasonEngine｜" + b.summary);
        } catch (Throwable e) {
        }
    }

    private double finalNeedSupplyScore(NeedSupplyBundle b) {
        if (b == null) {
            return 0.0d;
        }
        double match = b.match == null ? 0.0d : b.match.matchScore;
        double third = b.match == null ? 0.0d : b.match.thirdQuality;
        double div = b.match == null ? 0.0d : b.match.roleDiversity;
        double need = b.need == null ? 0.0d : b.need.totalNeedStrength;
        double cf = b.counterfactual == null ? 0.0d : b.counterfactual.rankScore;
        double ab = b.ablation == null ? 0.0d : b.ablation.totalAblationScore;
        return clamp01(
                match * 0.42d
                        + third * 0.12d
                        + div * 0.10d
                        + need * 0.12d
                        + cf * 0.14d
                        + ab * 0.10d);
    }

    private double finalNeedSupplyRisk(NeedSupplyBundle b) {
        if (b == null) {
            return 1.0d;
        }
        double supply = b.match == null ? 0.40d : b.match.supplyRisk;
        double chaos = b.need == null ? 0.30d : b.need.chaosRisk;
        double whyNot = b.counterfactual == null ? 0.35d : b.counterfactual.whyNotRisk;
        double falseCause =
                b.ablation == null ? 0.20d : Math.min(1.0d, b.ablation.falseCauseCount / 5.0d);
        double thirdGap =
                b.match == null
                        ? 0.10d
                        : Math.max(
                                0.0d,
                                AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY - b.match.thirdQuality);
        return clamp01(
                supply * 0.32d
                        + chaos * 0.18d
                        + whyNot * 0.28d
                        + falseCause * 0.12d
                        + thirdGap * 0.10d);
    }

    private String decideAction(NeedSupplyBundle b) {
        if (b == null || b.match == null) {
            return AppConfig.ADAPTIVE_ACTION_WATCH;
        }
        if (b.finalRisk >= AppConfig.NEED_SUPPLY_HARD_RISK
                || b.match.supplyRisk >= AppConfig.NEED_SUPPLY_HARD_RISK
                || (b.counterfactual != null
                        && b.counterfactual.whyNotRisk
                                >= AppConfig.COUNTERFACTUAL_HARD_WHY_NOT_RISK)) {
            return AppConfig.ADAPTIVE_ACTION_FREEZE;
        }
        if (b.match.matchScore >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE
                && b.match.thirdQuality >= AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY
                && b.match.roleDiversity >= AppConfig.NEED_SUPPLY_ROLE_DIVERSITY_MIN
                && b.match.supplyRisk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK
                && b.finalRisk <= AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK) {
            return AppConfig.ADAPTIVE_ACTION_READY;
        }
        return AppConfig.ADAPTIVE_ACTION_WATCH;
    }

    private String buildBundleSummary(NeedSupplyBundle b) {
        if (b == null) {
            return "";
        }
        return "NeedSupply完整链路｜P"
                + b.position
                + "｜K3="
                + AppConfig.normalizeK3Part(b.n1, b.n2, b.n3)
                + "｜action="
                + b.action
                + "｜score="
                + percent(b.finalScore)
                + "｜risk="
                + percent(b.finalRisk)
                + "｜match="
                + percent(b.match == null ? 0.0d : b.match.matchScore)
                + "｜third="
                + percent(b.match == null ? 0.0d : b.match.thirdQuality)
                + "｜role="
                + percent(b.match == null ? 0.0d : b.match.roleDiversity)
                + "｜why="
                + (b.match == null ? "" : b.match.whyMatch)
                + "｜真实BET仍由DecisionGateEngine唯一裁决";
    }

    private RolePick[] rankRoles(
            Models.PositionNeedState need,
            Models.NumberSupplyState s1,
            Models.NumberSupplyState s2,
            Models.NumberSupplyState s3) {
        ArrayList<RolePick> list = new ArrayList<RolePick>();
        list.add(toRolePick(need, s1));
        list.add(toRolePick(need, s2));
        list.add(toRolePick(need, s3));
        Collections.sort(
                list,
                new Comparator<RolePick>() {
                    public int compare(RolePick a, RolePick b) {
                        if (a.score > b.score) return -1;
                        if (a.score < b.score) return 1;
                        if (a.risk < b.risk) return -1;
                        if (a.risk > b.risk) return 1;
                        return 0;
                    }
                });
        while (list.size() < 3) {
            list.add(new RolePick());
        }
        return new RolePick[] {list.get(0), list.get(1), list.get(2)};
    }

    private RolePick toRolePick(Models.PositionNeedState need, Models.NumberSupplyState s) {
        RolePick r = new RolePick();
        if (s == null) {
            return r;
        }
        r.number = s.number;
        r.type = s.mainSupplyType;
        r.score = supplyNeedFit(need, s);
        r.risk = s.supplyRisk;
        return r;
    }

    private double supplyNeedFit(Models.PositionNeedState need, Models.NumberSupplyState s) {
        if (need == null || s == null) {
            return 0.0d;
        }
        double fit = 0.0d;
        fit += Math.min(need.needHotContinue, s.supplyHotContinue) * 0.120d;
        fit += Math.min(need.needColdRepair, s.supplyColdRepair) * 0.120d;
        fit += Math.min(need.needOmissionReturn, s.supplyOmissionReturn) * 0.140d;
        fit += Math.min(need.needHighLowBalance, s.supplyHighLowBalance) * 0.080d;
        fit += Math.min(need.needOddEvenBalance, s.supplyOddEvenBalance) * 0.080d;
        fit += Math.min(need.needTailRepair, s.supplyTailRepair) * 0.085d;
        fit += Math.min(need.needNeighborRelay, s.supplyNeighborRelay) * 0.115d;
        fit += Math.min(need.needCrossPositionFlow, s.supplyCrossPositionFlow) * 0.110d;
        fit += Math.min(need.needCrowdRelease, s.supplyCrowdRelease) * 0.070d;
        fit += Math.min(need.needReverseBreak, s.supplyReverseBreak) * 0.040d;
        fit += Math.min(need.needStableRepeat, s.supplyStableRepeat) * 0.040d;
        fit += Math.min(need.needChaosAvoid, 1.0d - s.supplyRisk) * 0.040d;
        return clamp01(fit);
    }

    private double thirdQuality(
            RolePick main, RolePick assist, RolePick defense, Models.PositionNeedState need) {
        if (defense == null) {
            return 0.0d;
        }
        double score =
                defense.score * 0.60d
                        + (1.0d - defense.risk) * 0.22d
                        + roleDiversity(main, assist, defense) * 0.18d;
        if (need != null && AppConfig.NEED_TYPE_CHAOS_AVOID.equals(need.mainNeedType)) {
            score += 0.04d;
        }
        return clamp01(score);
    }

    private double roleDiversity(RolePick a, RolePick b, RolePick c) {
        String x = a == null ? "" : safe(a.type);
        String y = b == null ? "" : safe(b.type);
        String z = c == null ? "" : safe(c.type);
        int diff = 1;
        if (!x.equals(y)) diff++;
        if (!x.equals(z) && !y.equals(z)) diff++;
        return clamp01(diff / 3.0d);
    }

    private String buildWhyNotMatch(Models.NeedSupplyMatchResult r, Models.PositionNeedState need) {
        if (r == null) {
            return "无匹配结果";
        }
        StringBuilder sb = new StringBuilder();
        if (r.matchScore < AppConfig.NEED_SUPPLY_MIN_MATCH_SCORE) {
            sb.append("匹配分不足;");
        }
        if (r.thirdQuality < AppConfig.NEED_SUPPLY_READY_THIRD_QUALITY) {
            sb.append("第三号质量不足;");
        }
        if (r.roleDiversity < AppConfig.NEED_SUPPLY_ROLE_DIVERSITY_MIN) {
            sb.append("三号角色过于同质;");
        }
        if (r.supplyRisk > AppConfig.NEED_SUPPLY_MAX_SUPPLY_RISK) {
            sb.append("供给风险偏高;");
        }
        if (need != null && need.chaosRisk > 0.62d) {
            sb.append("位置混乱风险偏高;");
        }
        if (sb.length() == 0) {
            sb.append("反证较低，可送最终门复核但不直接BET;");
        }
        return sb.toString();
    }

    private double positionK3Rate(
            int position, Models.ComboCandidate c, List<Models.Draw> clean, int window) {
        if (c == null || clean == null || clean.size() == 0) {
            return K3_BASELINE;
        }
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a == c.n1 || a == c.n2 || a == c.n3) hit++;
        }
        return sample <= 0 ? K3_BASELINE : clamp01(((double) hit) / sample);
    }

    private double numberRate(int position, int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) {
            return NUM_BASELINE;
        }
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a == number) hit++;
        }
        return sample <= 0 ? NUM_BASELINE : clamp01(((double) hit) / sample);
    }

    private double candidateNumberHeat(
            int position, Models.ComboCandidate c, List<Models.Draw> clean, int window) {
        if (c == null) {
            return NUM_BASELINE;
        }
        return clamp01(
                (numberRate(position, c.n1, clean, window)
                                + numberRate(position, c.n2, clean, window)
                                + numberRate(position, c.n3, clean, window))
                        / 0.300d
                        / 3.0d);
    }

    private double candidateOmissionPressure(
            int position, Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null || clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int miss = 0;
        for (int i = clean.size() - 1; i >= 0 && i >= clean.size() - 60; i--) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (a == c.n1 || a == c.n2 || a == c.n3) break;
            miss++;
        }
        return clamp01(miss / 30.0d);
    }

    private double numberOmissionPressure(int position, int number, List<Models.Draw> clean) {
        if (clean == null || clean.size() == 0) {
            return 0.0d;
        }
        int miss = 0;
        for (int i = clean.size() - 1; i >= 0 && i >= clean.size() - 80; i--) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (a == number) break;
            miss++;
        }
        return clamp01(miss / 45.0d);
    }

    private double positionChaosRisk(int position, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() < 6) {
            return 0.30d;
        }
        int start = Math.max(0, clean.size() - Math.max(6, window));
        int sample = 0;
        int jump = 0;
        int prev = -1;
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            if (prev >= 0) {
                sample++;
                if (Math.abs(a - prev) >= 5) jump++;
            }
            prev = a;
        }
        return sample <= 0 ? 0.30d : clamp01(((double) jump) / sample);
    }

    private double highLowNeed(int position, List<Models.Draw> clean) {
        double high = positionHighRate(position, clean, 48);
        return clamp01(Math.abs(0.50d - high) * 2.0d);
    }

    private double oddEvenNeed(int position, List<Models.Draw> clean) {
        double odd = positionOddRate(position, clean, 48);
        return clamp01(Math.abs(0.50d - odd) * 2.0d);
    }

    private double tailNeed(int position, List<Models.Draw> clean) {
        if (clean == null || clean.size() == 0) return 0.35d;
        int sample = 0;
        int sameTail = 0;
        int start = Math.max(1, clean.size() - 48);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw a = clean.get(i);
            Models.Draw b = clean.get(i - 1);
            if (!validDraw(a) || !validDraw(b)) continue;
        }
        return tailNeedSafe(position, clean);
    }

    private double tailNeedSafe(int position, List<Models.Draw> clean) {
        if (clean == null || clean.size() < 2) return 0.35d;
        int sample = 0;
        int sameTail = 0;
        int start = Math.max(1, clean.size() - 48);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw a = clean.get(i);
            Models.Draw b = clean.get(i - 1);
            if (!validDraw(a) || !validDraw(b)) continue;
            int x = a.getNumberAtPosition(position);
            int y = b.getNumberAtPosition(position);
            if (!validNumber(x) || !validNumber(y)) continue;
            sample++;
            if (x % 5 == y % 5) sameTail++;
        }
        return sample <= 0 ? 0.35d : clamp01(((double) sameTail) / sample);
    }

    private double neighborNeed(int position, List<Models.Draw> clean) {
        if (clean == null || clean.size() < 2) return 0.35d;
        int sample = 0;
        int near = 0;
        int start = Math.max(1, clean.size() - 48);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw a = clean.get(i);
            Models.Draw b = clean.get(i - 1);
            if (!validDraw(a) || !validDraw(b)) continue;
            int x = a.getNumberAtPosition(position);
            int y = b.getNumberAtPosition(position);
            if (!validNumber(x) || !validNumber(y)) continue;
            sample++;
            if (Math.abs(x - y) == 1) near++;
        }
        return sample <= 0 ? 0.35d : clamp01(((double) near) / sample * 2.4d);
    }

    private double crossPositionNeed(
            int position, Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null || clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int flow = 0;
        int start = Math.max(0, clean.size() - 24);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            for (int p = 1; p <= MAX_POSITION; p++) {
                if (p == position) continue;
                int a = d.getNumberAtPosition(p);
                if (!validNumber(a)) continue;
                sample++;
                if (a == c.n1 || a == c.n2 || a == c.n3) flow++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) flow) / sample * 3.0d);
    }

    private double crowdReleaseNeed(Models.ComboCandidate c, List<Models.Draw> clean) {
        if (c == null) return 0.0d;
        double crowd =
                (numberCrowdRisk(c.n1, clean, 24)
                                + numberCrowdRisk(c.n2, clean, 24)
                                + numberCrowdRisk(c.n3, clean, 24))
                        / 3.0d;
        return clamp01(1.0d - crowd);
    }

    private double stableRepeatNeed(int position, List<Models.Draw> clean) {
        if (clean == null || clean.size() < 2) return 0.0d;
        int sample = 0;
        int rep = 0;
        int start = Math.max(1, clean.size() - 36);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw a = clean.get(i);
            Models.Draw b = clean.get(i - 1);
            if (!validDraw(a) || !validDraw(b)) continue;
            int x = a.getNumberAtPosition(position);
            int y = b.getNumberAtPosition(position);
            if (!validNumber(x) || !validNumber(y)) continue;
            sample++;
            if (x == y) rep++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) rep) / sample * 4.0d);
    }

    private double supplyHighLow(int number) {
        return number >= 6 ? 0.62d : 0.38d;
    }

    private double supplyOddEven(int number) {
        return number % 2 == 0 ? 0.58d : 0.42d;
    }

    private double numberTailRepair(int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) return 0.45d;
        int tail = number % 5;
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            for (int p = 1; p <= MAX_POSITION; p++) {
                int a = d.getNumberAtPosition(p);
                if (!validNumber(a)) continue;
                sample++;
                if (a % 5 == tail) hit++;
            }
        }
        return sample <= 0 ? 0.45d : clamp01(((double) hit) / sample * 5.0d);
    }

    private double numberNeighborRelay(
            int position, int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int nA = number > MIN_NUMBER ? number - 1 : number;
        int nB = number < MAX_NUMBER ? number + 1 : number;
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a == nA || a == nB) hit++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 2.0d);
    }

    private double numberCrossPositionFlow(int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int hit = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            for (int p = 1; p <= MAX_POSITION; p++) {
                int a = d.getNumberAtPosition(p);
                if (!validNumber(a)) continue;
                sample++;
                if (a == number) hit++;
            }
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample * 10.0d);
    }

    private double numberCrowdRisk(int number, List<Models.Draw> clean, int window) {
        return clamp01(numberCrossPositionFlow(number, clean, window));
    }

    private double numberStableRepeat(
            int position, int number, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() < 2) return 0.0d;
        int sample = 0;
        int hit = 0;
        int start = Math.max(1, clean.size() - Math.max(2, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw a = clean.get(i);
            Models.Draw b = clean.get(i - 1);
            if (!validDraw(a) || !validDraw(b)) continue;
            int x = a.getNumberAtPosition(position);
            int y = b.getNumberAtPosition(position);
            if (!validNumber(x) || !validNumber(y)) continue;
            sample++;
            if (x == number || x == y) hit++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) hit) / sample);
    }

    private double numberSupplyRisk(
            Models.NumberSupplyState n,
            Models.PositionNeedState need,
            double rate12,
            double rate24,
            double rate96,
            List<Models.Draw> clean) {
        if (n == null) return 1.0d;
        double risk = 0.0d;
        risk += Math.max(0.0d, NUM_BASELINE * 0.45d - rate96) * 2.4d;
        risk += Math.max(0.0d, rate96 - NUM_BASELINE * 2.6d) * 0.6d;
        risk += Math.max(0.0d, rate12 - rate24) * 0.8d;
        risk += numberCrowdRisk(n.number, clean, 12) * 0.18d;
        if (need != null) {
            risk += need.chaosRisk * 0.15d;
        }
        return clamp01(risk);
    }

    private double positionHighRate(int position, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) return 0.50d;
        int sample = 0;
        int high = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a >= 6) high++;
        }
        return sample <= 0 ? 0.50d : clamp01(((double) high) / sample);
    }

    private double positionOddRate(int position, List<Models.Draw> clean, int window) {
        if (clean == null || clean.size() == 0) return 0.50d;
        int sample = 0;
        int odd = 0;
        int start = Math.max(0, clean.size() - Math.max(1, window));
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(position);
            if (!validNumber(a)) continue;
            sample++;
            if (a % 2 != 0) odd++;
        }
        return sample <= 0 ? 0.50d : clamp01(((double) odd) / sample);
    }

    private double positionNeedRankScore(Models.PositionNeedState need) {
        if (need == null) return 0.0d;
        return clamp01(need.totalNeedStrength * 0.62d + (1.0d - need.chaosRisk) * 0.38d);
    }

    private int positionNeedRank(
            int position, Models.PositionNeedState need, List<Models.Draw> clean) {
        double score = positionNeedRankScore(need);
        if (score >= 0.72d) return 1;
        if (score >= 0.64d) return 2;
        if (score >= 0.56d) return 3;
        if (score >= 0.48d) return 5;
        if (score >= 0.38d) return 7;
        return Math.max(8, Math.min(10, position));
    }

    private int k3SupplyRank(Models.NeedSupplyMatchResult match) {
        double s = match == null ? 0.0d : match.matchScore;
        if (s >= AppConfig.NEED_SUPPLY_STRONG_MATCH_SCORE) return 1;
        if (s >= AppConfig.NEED_SUPPLY_READY_MATCH_SCORE) return 2;
        if (s >= AppConfig.NEED_SUPPLY_MIN_MATCH_SCORE) return 3;
        if (s >= 0.460d) return 5;
        if (s >= 0.360d) return 7;
        return 9;
    }

    private double mirrorMissSimilarity(NeedSupplyBundle b, List<Models.Draw> clean) {
        if (b == null || clean == null || clean.size() == 0) return 0.0d;
        int sample = 0;
        int miss = 0;
        int start = Math.max(0, clean.size() - 36);
        for (int i = start; i < clean.size(); i++) {
            Models.Draw d = clean.get(i);
            if (!validDraw(d)) continue;
            int a = d.getNumberAtPosition(b.position);
            if (!validNumber(a)) continue;
            sample++;
            if (a != b.n1 && a != b.n2 && a != b.n3) miss++;
        }
        return sample <= 0 ? 0.0d : clamp01(((double) miss) / sample);
    }

    private double ablationDrop(NeedSupplyBundle b, String type) {
        if (b == null || b.need == null || b.match == null) return 0.0d;
        double before = b.match.matchScore;
        double after = before;
        if ("OMISSION".equals(type)) {
            after -= b.need.needOmissionReturn * 0.08d;
        } else if ("TAIL".equals(type)) {
            after -= b.need.needTailRepair * 0.07d;
        } else if ("NEIGHBOR".equals(type)) {
            after -= b.need.needNeighborRelay * 0.08d;
        } else if ("CROSS".equals(type)) {
            after -= b.need.needCrossPositionFlow * 0.08d;
        }
        return clamp01(before - Math.max(0.0d, after));
    }

    private String mainNeedText(Models.PositionNeedState p) {
        if (p == null) return "UNKNOWN";
        double best = p.needHotContinue;
        String name = AppConfig.NEED_TYPE_HOT_CONTINUE;
        if (p.needColdRepair > best) {
            best = p.needColdRepair;
            name = AppConfig.NEED_TYPE_COLD_REPAIR;
        }
        if (p.needOmissionReturn > best) {
            best = p.needOmissionReturn;
            name = AppConfig.NEED_TYPE_OMISSION_RETURN;
        }
        if (p.needNeighborRelay > best) {
            best = p.needNeighborRelay;
            name = AppConfig.NEED_TYPE_NEIGHBOR_RELAY;
        }
        if (p.needCrossPositionFlow > best) {
            best = p.needCrossPositionFlow;
            name = AppConfig.NEED_TYPE_CROSS_POSITION_FLOW;
        }
        if (p.needChaosAvoid > best) {
            name = AppConfig.NEED_TYPE_CHAOS_AVOID;
        }
        return name;
    }

    private String mainSupplyText(Models.NumberSupplyState n) {
        if (n == null) return "UNKNOWN";
        double best = n.supplyHotContinue;
        String name = AppConfig.NEED_TYPE_HOT_CONTINUE;
        if (n.supplyColdRepair > best) {
            best = n.supplyColdRepair;
            name = AppConfig.NEED_TYPE_COLD_REPAIR;
        }
        if (n.supplyOmissionReturn > best) {
            best = n.supplyOmissionReturn;
            name = AppConfig.NEED_TYPE_OMISSION_RETURN;
        }
        if (n.supplyNeighborRelay > best) {
            best = n.supplyNeighborRelay;
            name = AppConfig.NEED_TYPE_NEIGHBOR_RELAY;
        }
        if (n.supplyCrossPositionFlow > best) {
            best = n.supplyCrossPositionFlow;
            name = AppConfig.NEED_TYPE_CROSS_POSITION_FLOW;
        }
        if (n.supplyCrowdRelease > best) {
            name = AppConfig.NEED_TYPE_CROWD_RELEASE;
        }
        return name;
    }

    private String buildSupplySummary(NeedSupplyBundle b) {
        if (b == null) return "";
        String a = b.supplyMain == null ? "" : b.supplyMain.summary;
        String x = b.supplyAssist == null ? "" : b.supplyAssist.summary;
        String d = b.supplyDefense == null ? "" : b.supplyDefense.summary;
        return a + "｜" + x + "｜" + d;
    }

    private double supplyStrength(Models.NumberSupplyState s) {
        return s == null ? 0.0d : s.totalSupplyStrength;
    }

    private double supplyRisk(Models.NumberSupplyState s) {
        return s == null ? 0.0d : s.supplyRisk;
    }

    private double avgRisk(
            Models.NumberSupplyState a, Models.NumberSupplyState b, Models.NumberSupplyState c) {
        return clamp01((supplyRisk(a) + supplyRisk(b) + supplyRisk(c)) / 3.0d);
    }

    private void sortBundles(ArrayList<NeedSupplyBundle> bundles) {
        if (bundles == null || bundles.size() < 2) return;
        Collections.sort(
                bundles,
                new Comparator<NeedSupplyBundle>() {
                    public int compare(NeedSupplyBundle a, NeedSupplyBundle b) {
                        double sa = a == null ? 0.0d : a.finalScore - a.finalRisk;
                        double sb = b == null ? 0.0d : b.finalScore - b.finalRisk;
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return 0;
                    }
                });
    }

    private ArrayList<Models.Draw> cleanHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
        if (history == null) return out;
        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (validDraw(d)) out.add(d);
        }
        return out;
    }

    private boolean validDraw(Models.Draw d) {
        try {
            return d != null && d.isValid();
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean validNumber(int n) {
        return n >= MIN_NUMBER && n <= MAX_NUMBER;
    }

    private int normalizePosition(int p) {
        if (p < 1) return 1;
        if (p > MAX_POSITION) return MAX_POSITION;
        return p;
    }

    private int normalizeNumber(int n) {
        if (n < MIN_NUMBER) return MIN_NUMBER;
        if (n > MAX_NUMBER) return MAX_NUMBER;
        return n;
    }

    private int buildK3Mask(int n1, int n2, int n3) {
        int m = 0;
        if (validNumber(n1)) m |= (1 << n1);
        if (validNumber(n2)) m |= (1 << n2);
        if (validNumber(n3)) m |= (1 << n3);
        return m;
    }

    private String firstNonEmpty(String a, String b) {
        return a != null && a.length() > 0 ? a : safe(b);
    }

    private double clamp01(double v) {
        return AppConfig.clampDynamicCausalScore(v);
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

    private static final class RolePick {
        int number;
        String type;
        double score;
        double risk;

        RolePick() {
            number = 0;
            type = "UNKNOWN";
            score = 0.0d;
            risk = 1.0d;
        }
    }

    public static final class NeedSupplyBundle {
        public String baseIssue;
        public String targetIssue;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;
        public Models.PositionNeedState need;
        public Models.NumberSupplyState supplyMain;
        public Models.NumberSupplyState supplyAssist;
        public Models.NumberSupplyState supplyDefense;
        public Models.NeedSupplyMatchResult match;
        public Models.CounterfactualReasonResult counterfactual;
        public Models.CauseAblationResult ablation;
        public double finalScore;
        public double finalRisk;
        public String action;
        public String summary;

        public NeedSupplyBundle() {
            baseIssue = "";
            targetIssue = "";
            action = AppConfig.ADAPTIVE_ACTION_WATCH;
            summary = "";
        }

        public boolean actionAllowsGate() {
            return AppConfig.ADAPTIVE_ACTION_READY.equals(action)
                    || AppConfig.ADAPTIVE_ACTION_STRONG.equals(action)
                    || AppConfig.ADAPTIVE_ACTION_PROMOTE.equals(action);
        }

        public static NeedSupplyBundle empty(String reason) {
            NeedSupplyBundle b = new NeedSupplyBundle();
            b.summary = reason == null ? "" : reason;
            b.finalRisk = 1.0d;
            b.action = AppConfig.ADAPTIVE_ACTION_WATCH;
            return b;
        }
    }
}
