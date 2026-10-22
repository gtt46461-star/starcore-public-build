package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CounterfactualRankEngine.java
 *
 * <p>第12/22 重型计算升级主链闭环修复文件。
 *
 * <p>作用： 1. 对 CandidateK3 做反事实排名审计。 2. 判断当前候选是否只是“表面第一”，还是经得起替代组合比较。 3. 分析： - 同位置换号码是否更优 -
 * 跨位置换组合是否更优 - 第一名与第二名差距是否足够 - 是否存在错过强候选风险 - 是否存在相近候选冲突 - 是否存在伪强第一名 4. 输出
 * Models.RankCounterfactual。 5. 把反事实排名分、替代风险、冠军差距、错过风险回写 CandidateK3 / ComboCandidate / CauseChain。
 * 6. FIX12：补齐 bestAlternative / replaceGain / thirdWeakness / counterfactualRisk / rankStability。
 * 7. FIX12：反事实只影响候选排序、概率/门槛建议字段和 Decision 诊断字段，不直接 BET。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不降低门槛。 - 不直接改 finalP / dynamicGate / edge。 - 不绕过
 * DecisionGateEngine。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class CounterfactualRankEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double EPS = 0.0000001d;

    public static final String V20_COUNTERFACTUAL_RANK_MAIN_CHAIN_FIX_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    public static final String V20_COUNTERFACTUAL_RANK_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final double THIRD_WEAK_LINE = 0.58d;
    private static final double REPLACE_GAIN_WARN = 0.025d;
    private static final double REPLACE_GAIN_HARD = 0.060d;
    private static final double RANK_STABLE_LINE = 0.58d;
    private static final String LINK_MODIFICATION_V2_COUNTERFACTUAL_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final int REALTIME_COUNTERFACTUAL_FULL_LIMIT =
            AppConfig.REALTIME_PROBABILITY_ENRICH_LIMIT;
    private static final int REALTIME_COUNTERFACTUAL_AUDIT_LIMIT =
            AppConfig.REALTIME_PROBABILITY_AUDIT_LIMIT;
    private static final int COUNTERFACTUAL_CACHE_MAX = 600;
    private static final HashMap<String, CounterfactualCacheEntry> COUNTERFACTUAL_CACHE =
            new HashMap<String, CounterfactualCacheEntry>();

    public CounterfactualRankEngine() {}

    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(candidates, baseIssue, targetIssue, "applyToAll");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                candidates, baseIssue, targetIssue, "enrichCandidates");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                candidates, baseIssue, targetIssue, "analyzeCandidates");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(candidates, baseIssue, targetIssue, "applyRealtime");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(candidates, baseIssue, targetIssue, "runRealtime");
    }

    public int applyAndCount(
            DbStore store, List rawCandidates, List history, String baseIssue, String targetIssue) {
        ArrayList<Object> list = normalizeObjectCandidates(rawCandidates);
        applyToObjectCandidates(list, baseIssue, targetIssue, "applyAndCount");
        return list.size();
    }

    private ArrayList<Models.ComboCandidate> applyToComboCandidatesRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            String baseIssue,
            String targetIssue,
            String source) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        ArrayList<Object> list = new ArrayList<Object>();
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i) != null) {
                list.add(out.get(i));
            }
        }
        applyToObjectCandidates(list, baseIssue, targetIssue, source);
        return out;
    }

    private String buildCounterfactualCacheKey(Object c, String baseIssue, String targetIssue) {
        String state = getString(c, "stateKey", getString(c, "windowStateKey", ""));
        int p = getInt(c, "position", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        return safe(baseIssue) + "|" + safe(targetIssue) + "|" + state + "|P" + p + "|K" + mask;
    }

    private CounterfactualCacheEntry getCounterfactualCache(String key) {
        if (key == null || key.length() == 0) return null;
        synchronized (COUNTERFACTUAL_CACHE) {
            return COUNTERFACTUAL_CACHE.get(key);
        }
    }

    private void putCounterfactualCache(String key, Object c) {
        if (key == null || key.length() == 0 || c == null) return;
        CounterfactualCacheEntry e = new CounterfactualCacheEntry();
        e.rankScore =
                getDouble(c, "counterfactualRankScore", getDouble(c, "counterfactualScore", 0.0d));
        e.risk = getDouble(c, "counterfactualRisk", 0.0d);
        e.whyNotRisk = getDouble(c, "whyNotRisk", e.risk);
        e.replaceGain = getDouble(c, "replaceGain", 0.0d);
        e.thirdWeakness = getDouble(c, "thirdWeakness", 0.0d);
        e.rankStability = getDouble(c, "rankStability", 0.0d);
        e.falseCauseCount = getInt(c, "causeAblationFalseCauseCount", 0);
        e.status = getString(c, "counterfactualStatus", "WATCH");
        e.summary = getString(c, "counterfactualSummary", "");
        e.createdAt = System.currentTimeMillis();
        synchronized (COUNTERFACTUAL_CACHE) {
            if (COUNTERFACTUAL_CACHE.size() >= COUNTERFACTUAL_CACHE_MAX)
                COUNTERFACTUAL_CACHE.clear();
            COUNTERFACTUAL_CACHE.put(key, e);
        }
    }

    private void applyCounterfactualCache(Object c, CounterfactualCacheEntry e) {
        if (c == null || e == null) return;
        putDouble(c, "counterfactualRankScore", e.rankScore);
        putDouble(c, "counterfactualScore", e.rankScore);
        putDouble(c, "counterfactualRisk", e.risk);
        putDouble(c, "whyNotRisk", e.whyNotRisk);
        putDouble(c, "replaceGain", e.replaceGain);
        putDouble(c, "thirdWeakness", e.thirdWeakness);
        putDouble(c, "rankStability", e.rankStability);
        putInt(c, "causeAblationFalseCauseCount", e.falseCauseCount);
        putInt(c, "counterfactualHardRisk", e.risk >= 0.72d || e.falseCauseCount >= 4 ? 1 : 0);
        putString(c, "counterfactualStatus", e.status);
        putString(c, "counterfactualSummary", e.summary + "｜CACHE_HIT");
        putString(c, "counterfactualVersion", LINK_MODIFICATION_V2_COUNTERFACTUAL_VERSION);
    }

    private void applyLightCounterfactualAudit(
            Object c,
            Object champion,
            int rank,
            String baseIssue,
            String targetIssue,
            String source) {
        if (c == null) return;
        double score = scoreCandidateObject(c);
        double championScore = champion == null ? score : scoreCandidateObject(champion);
        double gap = Math.max(0.0d, championScore - score);
        double risk = clamp01(gap * 2.2d + (rank > 80 ? 0.08d : 0.03d));
        putDouble(c, "counterfactualRankScore", clamp01(1.0d - risk));
        putDouble(c, "counterfactualRisk", risk);
        putDouble(c, "whyNotRisk", risk);
        putInt(c, "causeAblationFalseCauseCount", risk >= 0.72d ? 4 : (risk >= 0.62d ? 2 : 0));
        putInt(c, "counterfactualHardRisk", risk >= 0.72d ? 1 : 0);
        putDouble(c, "whyNotRisk", risk);
        putDouble(c, "rankStability", clamp01(1.0d - gap * 3.0d));
        putInt(c, "causeAblationFalseCauseCount", 0);
        putInt(c, "counterfactualHardRisk", risk >= 0.72d ? 1 : 0);
        putString(c, "counterfactualStatus", "LIGHT_AUDIT");
        putString(c, "counterfactualSummary", "前41-120候选只做轻量反事实审计，不全量重跑替代组合");
        putString(c, "counterfactualVersion", LINK_MODIFICATION_V2_COUNTERFACTUAL_VERSION);
    }

    private ArrayList<Object> normalizeObjectCandidates(List rawCandidates) {
        ArrayList<Object> out = new ArrayList<Object>();
        if (rawCandidates == null || rawCandidates.size() == 0) {
            return out;
        }
        for (int i = 0; i < rawCandidates.size(); i++) {
            Object c = rawCandidates.get(i);
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
            out.add(c);
        }
        return out;
    }

    private void applyToObjectCandidates(
            final ArrayList<Object> list, String baseIssue, String targetIssue, String source) {
        if (list == null || list.size() == 0) {
            return;
        }
        Collections.sort(
                list,
                new Comparator<Object>() {
                    public int compare(Object a, Object b) {
                        double sa = scoreCandidateObject(a);
                        double sb = scoreCandidateObject(b);
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);
                        if (pa != pb) return pa - pb;
                        return getInt(a, "k3Mask", getInt(a, "k3_mask", 0))
                                - getInt(b, "k3Mask", getInt(b, "k3_mask", 0));
                    }
                });
        Object champion = list.get(0);
        int auditMax = Math.min(list.size(), REALTIME_COUNTERFACTUAL_AUDIT_LIMIT);
        for (int i = 0; i < auditMax; i++) {
            Object c = list.get(i);
            if (c == null) continue;
            putInt(c, "rankNo", i + 1);
            putInt(c, "rank", i + 1);
            if (i < REALTIME_COUNTERFACTUAL_FULL_LIMIT) {
                String cacheKey = buildCounterfactualCacheKey(c, baseIssue, targetIssue);
                CounterfactualCacheEntry cached = getCounterfactualCache(cacheKey);
                if (cached != null) applyCounterfactualCache(c, cached);
                else {
                    applyCounterfactualFieldsToObject(
                            c, champion, list, i + 1, baseIssue, targetIssue, source);
                    putCounterfactualCache(cacheKey, c);
                }
            } else {
                applyLightCounterfactualAudit(c, champion, i + 1, baseIssue, targetIssue, source);
            }
        }
        for (int i = auditMax; i < list.size(); i++) {
            Object c = list.get(i);
            if (c == null) continue;
            putInt(c, "rankNo", i + 1);
            putInt(c, "rank", i + 1);
            putString(c, "counterfactualStatus", "DEFERRED_OUTSIDE_TOP120");
            putString(c, "counterfactualVersion", LINK_MODIFICATION_V2_COUNTERFACTUAL_VERSION);
        }
        Collections.sort(
                list,
                new Comparator<Object>() {
                    public int compare(Object a, Object b) {
                        double sa =
                                scoreCandidateObject(a) * 0.72d
                                        + getDouble(a, "counterfactualScore", 0.0d) * 0.16d
                                        + getDouble(a, "rankStability", 0.0d) * 0.08d
                                        - getDouble(a, "counterfactualRisk", 0.0d) * 0.12d
                                        - posPart(getDouble(a, "replaceGain", 0.0d) * 4.0d) * 0.06d;
                        double sb =
                                scoreCandidateObject(b) * 0.72d
                                        + getDouble(b, "counterfactualScore", 0.0d) * 0.16d
                                        + getDouble(b, "rankStability", 0.0d) * 0.08d
                                        - getDouble(b, "counterfactualRisk", 0.0d) * 0.12d
                                        - posPart(getDouble(b, "replaceGain", 0.0d) * 4.0d) * 0.06d;
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);
                        if (pa != pb) return pa - pb;
                        return getInt(a, "k3Mask", getInt(a, "k3_mask", 0))
                                - getInt(b, "k3Mask", getInt(b, "k3_mask", 0));
                    }
                });
        for (int i = 0; i < list.size(); i++) {
            Object c = list.get(i);
            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
                putString(
                        c,
                        "counterfactualRankVersion",
                        V20_COUNTERFACTUAL_RANK_MAIN_CHAIN_FIX_VERSION);
            }
        }
    }

    public ArrayList<Models.RankCounterfactual> buildCounterfactuals(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.CauseChain> chains) {
        ArrayList<Models.CandidateK3> ranked = normalizeAndRank(candidates);
        ArrayList<Models.RankCounterfactual> out = new ArrayList<Models.RankCounterfactual>();

        if (ranked.size() == 0) {
            return out;
        }

        Models.CandidateK3 champion = ranked.get(0);

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null) {
                continue;
            }

            Models.RankCounterfactual r =
                    buildOneCounterfactual(
                            baseIssue,
                            targetIssue,
                            c,
                            champion,
                            ranked,
                            i + 1,
                            positionHealthMap,
                            teacherMap,
                            chains);

            if (r != null) {
                out.add(r);
            }
        }

        applyCounterfactualsToCandidates(ranked, out);
        applyCounterfactualsToCauseChains(chains, out);

        return out;
    }

    public ArrayList<Models.RankCounterfactual> buildCounterfactuals(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildCounterfactuals(baseIssue, targetIssue, candidates, null, null, null);
    }

    public ArrayList<Models.RankCounterfactual> buildCounterfactuals(
            List<Models.CandidateK3> candidates) {
        return buildCounterfactuals("", "", candidates, null, null, null);
    }

    public Models.RankCounterfactual buildChampionAudit(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.CauseChain> chains) {
        ArrayList<Models.RankCounterfactual> list =
                buildCounterfactuals(
                        baseIssue, targetIssue, candidates, positionHealthMap, teacherMap, chains);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates,
            ArrayList<Models.RankCounterfactual> counterfactuals) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }

        applyCounterfactualsToCandidates(candidates, counterfactuals);
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.RankCounterfactual> counterfactuals) {
        applyCounterfactualsToCauseChains(chains, counterfactuals);
    }

    private Models.RankCounterfactual buildOneCounterfactual(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 current,
            Models.CandidateK3 champion,
            ArrayList<Models.CandidateK3> ranked,
            int rankNo,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.CauseChain> chains) {
        if (current == null) {
            return null;
        }

        Models.RankCounterfactual r = new Models.RankCounterfactual();

        int position = getInt(current, "position", 0);
        int n1 = getInt(current, "n1", 0);
        int n2 = getInt(current, "n2", 0);
        int n3 = getInt(current, "n3", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));

        double currentScore = scoreCandidate(current);
        double championScore = scoreCandidate(champion);
        double rankGap = championScore - currentScore;

        AlternativeResult samePositionAlt = findBestSamePositionAlternative(current, ranked);
        AlternativeResult crossPositionAlt = findBestCrossPositionAlternative(current, ranked);
        AlternativeResult oneNumberAlt = findBestOneNumberAlternative(current, ranked);

        AlternativeResult bestAlt =
                chooseBestAlternative(samePositionAlt, crossPositionAlt, oneNumberAlt);
        double bestAlternativeScore = bestAlt.exists ? bestAlt.score : 0.0d;
        double replaceGain = bestAlt.exists ? bestAlt.score - currentScore : 0.0d;
        double thirdWeakness = calcThirdWeakness(current, oneNumberAlt, teacherMap, chains);

        double positionHealth = getPositionHealth(positionHealthMap, position);
        double positionRisk = getPositionRisk(positionHealthMap, position);
        double teacherScore = calcTeacherComboScore(position, n1, n2, n3, teacherMap);
        double teacherRisk = calcTeacherComboRisk(position, n1, n2, n3, teacherMap);
        double chainScore = calcChainScore(position, n1, n2, n3, chains);
        double chainRisk = calcChainRisk(position, n1, n2, n3, chains);

        double samePositionRisk =
                samePositionAlt.exists
                        ? clamp01((samePositionAlt.score - currentScore + 0.08d) / 0.22d)
                        : 0.0d;
        double crossPositionRisk =
                crossPositionAlt.exists
                        ? clamp01((crossPositionAlt.score - currentScore + 0.08d) / 0.22d)
                        : 0.0d;
        double oneNumberRisk =
                oneNumberAlt.exists
                        ? clamp01((oneNumberAlt.score - currentScore + 0.06d) / 0.18d)
                        : 0.0d;

        double missedStrongRisk =
                calcMissedStrongRisk(currentScore, samePositionAlt, crossPositionAlt, oneNumberAlt);
        double nearConflictRisk = calcNearConflictRisk(current, ranked);
        double falseChampionRisk =
                calcFalseChampionRisk(
                        rankNo,
                        currentScore,
                        championScore,
                        positionHealth,
                        positionRisk,
                        teacherScore,
                        teacherRisk,
                        chainScore,
                        chainRisk,
                        nearConflictRisk);

        double alternativeStrength = bestAlt.exists ? clamp01(bestAlt.score) : 0.0d;
        double counterfactualRisk =
                calcCounterfactualRisk(
                        replaceGain,
                        thirdWeakness,
                        samePositionRisk,
                        crossPositionRisk,
                        oneNumberRisk,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk);
        double rankStability =
                calcRankStability(
                        rankNo,
                        currentScore,
                        championScore,
                        replaceGain,
                        counterfactualRisk,
                        nearConflictRisk);

        double counterScore =
                calcCounterScore(
                        currentScore,
                        championScore,
                        rankNo,
                        positionHealth,
                        teacherScore,
                        chainScore,
                        samePositionRisk,
                        crossPositionRisk,
                        oneNumberRisk,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk);

        String status =
                statusOf(
                        rankNo,
                        counterScore,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        currentScore,
                        championScore,
                        rankGap,
                        samePositionAlt,
                        crossPositionAlt,
                        oneNumberAlt,
                        counterScore,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk,
                        status);
        summary =
                appendFix12Summary(
                        summary,
                        bestAlt,
                        replaceGain,
                        thirdWeakness,
                        rankStability,
                        counterfactualRisk);

        putString(r, "rankKey", buildRankKey(targetIssue, position, mask, rankNo));
        putString(r, "counterfactualKey", buildRankKey(targetIssue, position, mask, rankNo));
        putString(r, "baseIssue", safe(baseIssue));
        putString(r, "targetIssue", safe(targetIssue));
        putLong(r, "createdAt", System.currentTimeMillis());

        putInt(r, "rankNo", rankNo);
        putInt(r, "rank", rankNo);
        putInt(r, "position", position);
        putInt(r, "chosenPosition", position);
        putInt(r, "n1", n1);
        putInt(r, "n2", n2);
        putInt(r, "n3", n3);
        putInt(r, "k3Mask", mask);
        putInt(r, "chosenK3Mask", mask);

        putDouble(r, "currentScore", currentScore);
        putDouble(r, "championScore", championScore);
        putDouble(r, "rankGap", rankGap);
        putDouble(r, "counterfactualScore", counterScore);
        putDouble(r, "rankScore", counterScore);

        putDouble(r, "positionHealthScore", positionHealth);
        putDouble(r, "positionRiskScore", positionRisk);
        putDouble(r, "teacherScore", teacherScore);
        putDouble(r, "teacherRisk", teacherRisk);
        putDouble(r, "chainScore", chainScore);
        putDouble(r, "chainRisk", chainRisk);

        putDouble(r, "samePositionReplaceRisk", samePositionRisk);
        putDouble(r, "crossPositionReplaceRisk", crossPositionRisk);
        putDouble(r, "oneNumberReplaceRisk", oneNumberRisk);
        putDouble(r, "missedStrongRisk", missedStrongRisk);
        putDouble(r, "nearConflictRisk", nearConflictRisk);
        putDouble(r, "falseChampionRisk", falseChampionRisk);

        putInt(r, "betterPosition", crossPositionAlt.exists ? crossPositionAlt.position : 0);
        putInt(r, "betterK3Mask", crossPositionAlt.exists ? crossPositionAlt.k3Mask : 0);
        putDouble(r, "betterScore", crossPositionAlt.exists ? crossPositionAlt.score : 0.0d);

        putInt(r, "samePositionBetterMask", samePositionAlt.exists ? samePositionAlt.k3Mask : 0);
        putDouble(
                r,
                "samePositionBetterScore",
                samePositionAlt.exists ? samePositionAlt.score : 0.0d);

        putInt(r, "oneNumberBetterMask", oneNumberAlt.exists ? oneNumberAlt.k3Mask : 0);
        putDouble(r, "oneNumberBetterScore", oneNumberAlt.exists ? oneNumberAlt.score : 0.0d);

        putString(r, "status", status);
        putString(r, "summaryText", summary);
        putString(r, "reasonText", summary);

        return r;
    }

    private ArrayList<Models.CandidateK3> normalizeAndRank(List<Models.CandidateK3> candidates) {
        ArrayList<Models.CandidateK3> list = new ArrayList<Models.CandidateK3>();

        if (candidates == null || candidates.size() == 0) {
            return list;
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

            list.add(c);
        }

        Collections.sort(
                list,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = scoreCandidate(a);
                        double sb = scoreCandidate(b);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        double pa = getDouble(a, "finalP", 0.0d);
                        double pb = getDouble(b, "finalP", 0.0d);

                        if (pa > pb) {
                            return -1;
                        }

                        if (pa < pb) {
                            return 1;
                        }

                        int posa = getInt(a, "position", 0);
                        int posb = getInt(b, "position", 0);

                        if (posa != posb) {
                            return posa - posb;
                        }

                        int ma = getInt(a, "k3Mask", getInt(a, "k3_mask", 0));
                        int mb = getInt(b, "k3Mask", getInt(b, "k3_mask", 0));

                        return ma - mb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);
            putInt(c, "rankNo", i + 1);
            putInt(c, "rank", i + 1);
        }

        return list;
    }

    private double scoreCandidate(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double chain = getDouble(c, "chainScore", getDouble(c, "finalCauseScore", 0.0d));
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double position =
                getDouble(c, "positionCauseScore", getDouble(c, "positionHealthScore", 0.0d));
        double risk = getDouble(c, "riskScore", 0.0d);
        double teacherRisk = getDouble(c, "teacherNumberRisk", 0.0d);
        double positionRisk = getDouble(c, "positionWrongRisk", 0.0d);
        double confidence =
                getDouble(c, "confidence", getDouble(c, "teacherNumberConfidence", 0.0d));

        double score = 0.0d;

        score += finalP * 0.26d;
        score += posPart(edge * 4.0d) * 0.12d;
        score += cause * 0.20d;
        score += chain * 0.14d;
        score += teacher * 0.10d;
        score += position * 0.08d;
        score += confidence * 0.06d;

        score -= risk * 0.14d;
        score -= teacherRisk * 0.06d;
        score -= positionRisk * 0.06d;

        return clamp01(score);
    }

    private AlternativeResult findBestSamePositionAlternative(
            Models.CandidateK3 current, ArrayList<Models.CandidateK3> ranked) {
        AlternativeResult best = new AlternativeResult();

        if (current == null || ranked == null || ranked.size() == 0) {
            return best;
        }

        int position = getInt(current, "position", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null || c == current) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            if (p != position || m == mask) {
                continue;
            }

            double s = scoreCandidate(c);

            if (!best.exists || s > best.score) {
                fillAlternative(best, c, s);
            }
        }

        return best;
    }

    private AlternativeResult findBestCrossPositionAlternative(
            Models.CandidateK3 current, ArrayList<Models.CandidateK3> ranked) {
        AlternativeResult best = new AlternativeResult();

        if (current == null || ranked == null || ranked.size() == 0) {
            return best;
        }

        int position = getInt(current, "position", 0);

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null || c == current) {
                continue;
            }

            int p = getInt(c, "position", 0);

            if (p == position) {
                continue;
            }

            double s = scoreCandidate(c);

            if (!best.exists || s > best.score) {
                fillAlternative(best, c, s);
            }
        }

        return best;
    }

    private AlternativeResult findBestOneNumberAlternative(
            Models.CandidateK3 current, ArrayList<Models.CandidateK3> ranked) {
        AlternativeResult best = new AlternativeResult();

        if (current == null || ranked == null || ranked.size() == 0) {
            return best;
        }

        int position = getInt(current, "position", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null || c == current) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            if (p != position || m == mask) {
                continue;
            }

            int diff = maskDiffCount(mask, m);

            if (diff != 2) {
                continue;
            }

            double s = scoreCandidate(c);

            if (!best.exists || s > best.score) {
                fillAlternative(best, c, s);
            }
        }

        return best;
    }

    private void fillAlternative(AlternativeResult r, Models.CandidateK3 c, double score) {
        if (r == null || c == null) {
            return;
        }

        r.exists = true;
        r.position = getInt(c, "position", 0);
        r.n1 = getInt(c, "n1", 0);
        r.n2 = getInt(c, "n2", 0);
        r.n3 = getInt(c, "n3", 0);
        r.k3Mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        r.score = score;
        r.finalP = getDouble(c, "finalP", 0.0d);
        r.edge = getDouble(c, "edge", 0.0d);
        r.reason =
                "P"
                        + r.position
                        + "｜"
                        + r.n1
                        + ","
                        + r.n2
                        + ","
                        + r.n3
                        + "｜score="
                        + percent(score);
    }

    private int maskDiffCount(int a, int b) {
        int diff = 0;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            boolean ca = maskContains(a, n);
            boolean cb = maskContains(b, n);

            if (ca != cb) {
                diff++;
            }
        }

        return diff;
    }

    private double calcMissedStrongRisk(
            double currentScore,
            AlternativeResult samePositionAlt,
            AlternativeResult crossPositionAlt,
            AlternativeResult oneNumberAlt) {
        double risk = 0.0d;

        if (samePositionAlt != null
                && samePositionAlt.exists
                && samePositionAlt.score > currentScore) {
            risk += clamp01((samePositionAlt.score - currentScore) / 0.16d) * 0.38d;
        }

        if (crossPositionAlt != null
                && crossPositionAlt.exists
                && crossPositionAlt.score > currentScore) {
            risk += clamp01((crossPositionAlt.score - currentScore) / 0.18d) * 0.34d;
        }

        if (oneNumberAlt != null && oneNumberAlt.exists && oneNumberAlt.score > currentScore) {
            risk += clamp01((oneNumberAlt.score - currentScore) / 0.12d) * 0.28d;
        }

        return clamp01(risk);
    }

    private double calcNearConflictRisk(
            Models.CandidateK3 current, ArrayList<Models.CandidateK3> ranked) {
        if (current == null || ranked == null || ranked.size() <= 1) {
            return 0.0d;
        }

        double currentScore = scoreCandidate(current);
        int currentPosition = getInt(current, "position", 0);
        int currentMask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));

        int nearCount = 0;
        double risk = 0.0d;

        for (int i = 0; i < ranked.size(); i++) {
            Models.CandidateK3 c = ranked.get(i);

            if (c == null || c == current) {
                continue;
            }

            double s = scoreCandidate(c);
            double gap = Math.abs(currentScore - s);

            if (gap > 0.055d) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            nearCount++;

            if (p != currentPosition) {
                risk += 0.08d;
            } else if (maskDiffCount(currentMask, m) <= 4) {
                risk += 0.06d;
            } else {
                risk += 0.04d;
            }

            if (s > currentScore) {
                risk += 0.08d;
            }
        }

        if (nearCount >= 3) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private double calcFalseChampionRisk(
            int rankNo,
            double currentScore,
            double championScore,
            double positionHealth,
            double positionRisk,
            double teacherScore,
            double teacherRisk,
            double chainScore,
            double chainRisk,
            double nearConflictRisk) {
        double risk = 0.0d;

        if (rankNo == 1) {
            if (currentScore < 0.48d) {
                risk += 0.18d;
            }

            if (championScore - currentScore < 0.025d) {
                risk += 0.10d;
            }
        } else {
            if (championScore - currentScore < 0.035d) {
                risk += 0.12d;
            }
        }

        if (positionHealth < 0.46d) {
            risk += 0.14d;
        }

        risk += positionRisk * 0.16d;

        if (teacherScore < 0.38d) {
            risk += 0.10d;
        }

        risk += teacherRisk * 0.16d;

        if (chainScore < 0.38d) {
            risk += 0.08d;
        }

        risk += chainRisk * 0.12d;
        risk += nearConflictRisk * 0.20d;

        return clamp01(risk);
    }

    private double calcCounterScore(
            double currentScore,
            double championScore,
            int rankNo,
            double positionHealth,
            double teacherScore,
            double chainScore,
            double samePositionRisk,
            double crossPositionRisk,
            double oneNumberRisk,
            double missedStrongRisk,
            double nearConflictRisk,
            double falseChampionRisk) {
        double rankBoost;

        if (rankNo == 1) {
            rankBoost = 0.12d;
        } else if (rankNo == 2) {
            rankBoost = 0.06d;
        } else if (rankNo <= 5) {
            rankBoost = 0.03d;
        } else {
            rankBoost = 0.0d;
        }

        double championGapSupport = clamp01((championScore - currentScore + 0.10d) / 0.20d);

        double score = 0.0d;

        score += currentScore * 0.36d;
        score += positionHealth * 0.14d;
        score += teacherScore * 0.14d;
        score += chainScore * 0.12d;
        score += rankBoost;
        score += championGapSupport * 0.06d;

        score -= samePositionRisk * 0.10d;
        score -= crossPositionRisk * 0.10d;
        score -= oneNumberRisk * 0.08d;
        score -= missedStrongRisk * 0.12d;
        score -= nearConflictRisk * 0.10d;
        score -= falseChampionRisk * 0.14d;

        return clamp01(score);
    }

    private double getPositionHealth(Map<Integer, Models.PositionHealth> map, int position) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        return getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
    }

    private double getPositionRisk(Map<Integer, Models.PositionHealth> map, int position) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        return getDouble(
                h, "wrongRisk", getDouble(h, "positionWrongRisk", getDouble(h, "riskScore", 0.0d)));
    }

    private double calcTeacherComboScore(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        if (map == null) {
            return 0.0d;
        }

        int[] nums = new int[] {n1, n2, n3};
        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            sum += getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private double calcTeacherComboRisk(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        if (map == null) {
            return 0.0d;
        }

        int[] nums = new int[] {n1, n2, n3};
        double sum = 0.0d;
        int used = 0;

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            sum += getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private Models.NumberTeacherCell getTeacherCell(
            Map<String, Models.NumberTeacherCell> map, int position, int number) {
        if (map == null) {
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

    private double calcChainScore(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null || chains.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;
        int used = 0;

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

            sum += getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private double calcChainRisk(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null || chains.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;
        int used = 0;

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

            sum += getDouble(c, "riskScore", 0.0d);
            used++;
        }

        if (used <= 0) {
            return 0.0d;
        }

        return clamp01(sum / used);
    }

    private void applyCounterfactualsToCandidates(
            List<Models.CandidateK3> candidates,
            ArrayList<Models.RankCounterfactual> counterfactuals) {
        if (candidates == null
                || counterfactuals == null
                || candidates.size() == 0
                || counterfactuals.size() == 0) {
            return;
        }

        LinkedHashMap<String, Models.RankCounterfactual> map =
                new LinkedHashMap<String, Models.RankCounterfactual>();

        for (int i = 0; i < counterfactuals.size(); i++) {
            Models.RankCounterfactual r = counterfactuals.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", getInt(r, "chosenPosition", 0));
            int mask = getInt(r, "k3Mask", getInt(r, "chosenK3Mask", 0));

            map.put(candidateKey(p, mask), r);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            Models.RankCounterfactual r = map.get(candidateKey(p, mask));

            if (r == null) {
                continue;
            }

            double rankScore = getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));
            double missedStrongRisk = getDouble(r, "missedStrongRisk", 0.0d);
            double nearConflictRisk = getDouble(r, "nearConflictRisk", 0.0d);
            double falseChampionRisk = getDouble(r, "falseChampionRisk", 0.0d);
            double rankGap = getDouble(r, "rankGap", 0.0d);

            double replaceGain = getDouble(r, "replaceGain", 0.0d);
            double thirdWeakness = getDouble(r, "thirdWeakness", 0.0d);
            double rankStability = getDouble(r, "rankStability", 0.0d);
            double cfRisk = getDouble(r, "counterfactualRisk", 0.0d);
            double bestAlternativeScore = getDouble(r, "bestAlternativeScore", 0.0d);

            double oldCause = getDouble(c, "causeScore", 0.0d);
            double oldRisk = getDouble(c, "riskScore", 0.0d);

            double newCause = clamp01(oldCause * 0.84d + rankScore * 0.10d + rankStability * 0.06d);
            double newRisk =
                    clamp01(
                            Math.max(
                                    oldRisk,
                                    Math.max(
                                            cfRisk,
                                            missedStrongRisk * 0.34d
                                                    + nearConflictRisk * 0.20d
                                                    + falseChampionRisk * 0.20d
                                                    + thirdWeakness * 0.14d
                                                    + posPart(replaceGain * 4.0d) * 0.12d)));

            putDouble(c, "counterfactualScore", rankScore);
            putDouble(c, "counterfactualRankScore", rankScore);
            putDouble(c, "counterfactualRisk", newRisk);
            putDouble(c, "counterfactualRawRisk", cfRisk);
            putDouble(c, "replaceGain", replaceGain);
            putDouble(c, "bestAlternativeScore", bestAlternativeScore);
            putDouble(c, "thirdWeakness", thirdWeakness);
            putDouble(
                    c,
                    "thirdDragScore",
                    Math.max(getDouble(c, "thirdDragScore", 0.0d), thirdWeakness));
            putDouble(c, "rankStability", rankStability);
            putDouble(c, "rankCounterEdge", clampSigned(rankScore - newRisk));
            putDouble(
                    c,
                    "antiEvidenceScore",
                    Math.max(getDouble(c, "antiEvidenceScore", 0.0d), newRisk));
            putDouble(
                    c,
                    "counterEvidenceScore",
                    Math.max(getDouble(c, "counterEvidenceScore", 0.0d), newRisk));
            putDouble(
                    c,
                    "netEvidenceScore",
                    clampSigned(getDouble(c, "evidenceScore", 0.0d) - newRisk));
            putDouble(c, "missedStrongRisk", missedStrongRisk);
            putDouble(c, "nearConflictRisk", nearConflictRisk);
            putDouble(c, "falseChampionRisk", falseChampionRisk);
            putDouble(c, "championGap", rankGap);
            putDouble(c, "rankGap", rankGap);
            putDouble(c, "causeScore", newCause);
            putDouble(c, "riskScore", newRisk);

            putString(c, "counterfactualStatus", getString(r, "status", ""));
            putString(c, "counterfactualSummary", getString(r, "summaryText", ""));

            appendReason(c, getString(r, "summaryText", ""));
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa =
                                scoreCandidate(a) * 0.76d
                                        + getDouble(a, "counterfactualScore", 0.0d) * 0.18d
                                        - getDouble(a, "counterfactualRisk", 0.0d) * 0.06d;

                        double sb =
                                scoreCandidate(b) * 0.76d
                                        + getDouble(b, "counterfactualScore", 0.0d) * 0.18d
                                        - getDouble(b, "counterfactualRisk", 0.0d) * 0.06d;

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

                        return getInt(a, "k3Mask", getInt(a, "k3_mask", 0))
                                - getInt(b, "k3Mask", getInt(b, "k3_mask", 0));
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

    private void applyCounterfactualsToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.RankCounterfactual> counterfactuals) {
        if (chains == null
                || counterfactuals == null
                || chains.size() == 0
                || counterfactuals.size() == 0) {
            return;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain chain = chains.get(i);

            if (chain == null) {
                continue;
            }

            int p = getInt(chain, "position", 0);
            int n = getInt(chain, "number", 0);

            if (!isValidPosition(p) || !isValidNumber(n)) {
                continue;
            }

            Models.RankCounterfactual best = findBestCounterForPosition(counterfactuals, p);

            if (best == null) {
                continue;
            }

            int mask = getInt(best, "k3Mask", getInt(best, "chosenK3Mask", 0));

            if (!maskContains(mask, n)) {
                continue;
            }

            double rankScore =
                    getDouble(best, "rankScore", getDouble(best, "counterfactualScore", 0.0d));
            double missedRisk = getDouble(best, "missedStrongRisk", 0.0d);
            double falseRisk = getDouble(best, "falseChampionRisk", 0.0d);
            double oldRankCause = getDouble(chain, "rankCause", 0.0d);
            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double oldRisk = getDouble(chain, "riskScore", 0.0d);

            double rankCause = clampSigned((rankScore - missedRisk - falseRisk) * 1.15d);
            double merged = clampSigned(oldRankCause * 0.66d + rankCause * 0.34d);
            double newFinal =
                    clamp01(oldFinal + posPart(merged) * 0.038d - negPart(merged) * 0.052d);
            double newRisk = clamp01(Math.max(oldRisk, missedRisk * 0.46d + falseRisk * 0.36d));

            putDouble(chain, "rankCause", merged);
            putDouble(chain, "counterfactualScore", rankScore);
            putDouble(chain, "missedStrongRisk", missedRisk);
            putDouble(chain, "falseChampionRisk", falseRisk);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);
            putDouble(chain, "riskScore", newRisk);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜反事实排名：rankScore="
                            + percent(rankScore)
                            + "，rankCause="
                            + signed(merged)
                            + "，错过强候选风险="
                            + percent(missedRisk)
                            + "，伪强风险="
                            + percent(falseRisk);

            putString(chain, "summaryText", old + add);
        }
    }

    private Models.RankCounterfactual findBestCounterForPosition(
            ArrayList<Models.RankCounterfactual> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        Models.RankCounterfactual best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < list.size(); i++) {
            Models.RankCounterfactual r = list.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", getInt(r, "chosenPosition", 0));

            if (p != position) {
                continue;
            }

            double s = getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));

            if (best == null || s > bestScore) {
                best = r;
                bestScore = s;
            }
        }

        return best;
    }

    private double scoreCandidateObject(Object c) {
        if (c == null) {
            return 0.0d;
        }
        double finalP = getDouble(c, "finalP", getDouble(c, "finalPAfterDiagnosis", 0.0d));
        double edge =
                getDouble(
                        c,
                        "edge",
                        finalP - getDouble(c, "gate", getDouble(c, "dynamicGate", 0.0d)));
        double cause = getDouble(c, "causeScore", getDouble(c, "dynamicCauseScore", 0.0d));
        double chain =
                getDouble(
                        c,
                        "chainScore",
                        getDouble(c, "finalCauseScore", getDouble(c, "v20SupportScore", 0.0d)));
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherMatrixScore", 0.0d));
        double position =
                getDouble(
                        c,
                        "positionNeedScore",
                        getDouble(
                                c,
                                "positionCauseScore",
                                getDouble(c, "positionHealthScore", 0.0d)));
        double coverage =
                getDouble(c, "coverageQuality", getDouble(c, "coverageQualityScore", 0.0d));
        double third = getDouble(c, "thirdQuality", getDouble(c, "thirdNumberQualityScore", 0.0d));
        double risk = getDouble(c, "riskScore", getDouble(c, "v20RiskScore", 0.0d));
        double teacherRisk =
                getDouble(c, "teacherNumberRisk", getDouble(c, "teacherMatrixRisk", 0.0d));
        double cfRisk = getDouble(c, "counterfactualRisk", 0.0d);
        double confidence =
                getDouble(
                        c,
                        "confidence",
                        getDouble(
                                c, "teacherNumberConfidence", getDouble(c, "v20Confidence", 0.0d)));
        double score = 0.0d;
        score += finalP * 0.22d;
        score += posPart(edge * 4.0d) * 0.10d;
        score += cause * 0.18d;
        score += chain * 0.12d;
        score += teacher * 0.10d;
        score += position * 0.08d;
        score += coverage * 0.08d;
        score += third * 0.06d;
        score += confidence * 0.06d;
        score -= risk * 0.12d;
        score -= teacherRisk * 0.05d;
        score -= cfRisk * 0.05d;
        return clamp01(score);
    }

    private void applyCounterfactualFieldsToObject(
            Object current,
            Object champion,
            ArrayList<Object> ranked,
            int rankNo,
            String baseIssue,
            String targetIssue,
            String source) {
        if (current == null) {
            return;
        }
        int position = getInt(current, "position", 0);
        int n1 = getInt(current, "n1", 0);
        int n2 = getInt(current, "n2", 0);
        int n3 = getInt(current, "n3", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));
        if (mask <= 0) {
            mask = buildMask(n1, n2, n3);
            putInt(current, "k3Mask", mask);
            putInt(current, "k3_mask", mask);
        }
        double currentScore = scoreCandidateObject(current);
        double championScore = scoreCandidateObject(champion);
        double rankGap = championScore - currentScore;
        AlternativeResult sameAlt = findBestSamePositionAlternativeObject(current, ranked);
        AlternativeResult crossAlt = findBestCrossPositionAlternativeObject(current, ranked);
        AlternativeResult oneAlt = findBestOneNumberAlternativeObject(current, ranked);
        AlternativeResult bestAlt = chooseBestAlternative(sameAlt, crossAlt, oneAlt);
        double replaceGain = bestAlt.exists ? bestAlt.score - currentScore : 0.0d;
        double thirdWeakness = calcThirdWeaknessObject(current, oneAlt);
        double sameRisk =
                sameAlt.exists ? clamp01((sameAlt.score - currentScore + 0.08d) / 0.22d) : 0.0d;
        double crossRisk =
                crossAlt.exists ? clamp01((crossAlt.score - currentScore + 0.08d) / 0.22d) : 0.0d;
        double oneRisk =
                oneAlt.exists ? clamp01((oneAlt.score - currentScore + 0.06d) / 0.18d) : 0.0d;
        double missedStrongRisk = calcMissedStrongRisk(currentScore, sameAlt, crossAlt, oneAlt);
        double nearConflictRisk = calcNearConflictRiskObject(current, ranked);
        double falseChampionRisk =
                calcFalseChampionRisk(
                        rankNo,
                        currentScore,
                        championScore,
                        getDouble(
                                current,
                                "positionHealthScore",
                                getDouble(current, "positionNeedScore", 0.0d)),
                        getDouble(
                                current,
                                "positionWrongRisk",
                                getDouble(current, "positionRiskScore", 0.0d)),
                        getDouble(
                                current,
                                "teacherNumberScore",
                                getDouble(current, "teacherMatrixScore", 0.0d)),
                        getDouble(
                                current,
                                "teacherNumberRisk",
                                getDouble(current, "teacherMatrixRisk", 0.0d)),
                        getDouble(
                                current, "causeScore", getDouble(current, "v20SupportScore", 0.0d)),
                        getDouble(current, "riskScore", getDouble(current, "v20RiskScore", 0.0d)),
                        nearConflictRisk);
        double cfRisk =
                calcCounterfactualRisk(
                        replaceGain,
                        thirdWeakness,
                        sameRisk,
                        crossRisk,
                        oneRisk,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk);
        double rankStability =
                calcRankStability(
                        rankNo, currentScore, championScore, replaceGain, cfRisk, nearConflictRisk);
        double rankScore =
                calcCounterScore(
                        currentScore,
                        championScore,
                        rankNo,
                        getDouble(
                                current,
                                "positionHealthScore",
                                getDouble(current, "positionNeedScore", 0.0d)),
                        getDouble(
                                current,
                                "teacherNumberScore",
                                getDouble(current, "teacherMatrixScore", 0.0d)),
                        getDouble(
                                current, "causeScore", getDouble(current, "v20SupportScore", 0.0d)),
                        sameRisk,
                        crossRisk,
                        oneRisk,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk);
        String status =
                statusOf(rankNo, rankScore, missedStrongRisk, nearConflictRisk, falseChampionRisk);
        String advice =
                buildCounterfactualActionAdvice(
                        status, cfRisk, replaceGain, thirdWeakness, rankStability);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        currentScore,
                        championScore,
                        rankGap,
                        sameAlt,
                        crossAlt,
                        oneAlt,
                        rankScore,
                        missedStrongRisk,
                        nearConflictRisk,
                        falseChampionRisk,
                        status);
        summary =
                appendFix12Summary(
                        summary, bestAlt, replaceGain, thirdWeakness, rankStability, cfRisk);

        putString(
                current,
                "baseIssue",
                firstNonEmpty(baseIssue, getString(current, "baseIssue", "")));
        putString(
                current,
                "targetIssue",
                firstNonEmpty(targetIssue, getString(current, "targetIssue", "")));
        putDouble(current, "counterfactualScore", rankScore);
        putDouble(current, "counterfactualRankScore", rankScore);
        putDouble(current, "counterfactualRisk", cfRisk);
        putDouble(current, "counterfactualRawRisk", cfRisk);
        putDouble(current, "missedStrongRisk", missedStrongRisk);
        putDouble(current, "nearConflictRisk", nearConflictRisk);
        putDouble(current, "falseChampionRisk", falseChampionRisk);
        putDouble(current, "championGap", rankGap);
        putDouble(current, "rankGap", rankGap);
        putDouble(current, "replaceGain", replaceGain);
        putDouble(current, "thirdWeakness", thirdWeakness);
        putDouble(
                current,
                "thirdDragScore",
                Math.max(getDouble(current, "thirdDragScore", 0.0d), thirdWeakness));
        putDouble(current, "rankStability", rankStability);
        putDouble(current, "bestAlternativeScore", bestAlt.exists ? bestAlt.score : 0.0d);
        putInt(current, "bestAlternativePosition", bestAlt.exists ? bestAlt.position : 0);
        putInt(current, "bestAlternativeK3Mask", bestAlt.exists ? bestAlt.k3Mask : 0);
        putString(current, "bestAlternative", bestAlternativeText(bestAlt));
        putString(current, "counterfactualStatus", status);
        putString(current, "counterfactualSummary", summary);
        putString(current, "counterfactualActionAdvice", advice);
        putString(
                current,
                "counterfactualRankVersion",
                V20_COUNTERFACTUAL_RANK_MAIN_CHAIN_FIX_VERSION);
        putString(current, "fieldChainVersion", V20_COUNTERFACTUAL_RANK_MAIN_CHAIN_FIX_VERSION);
        putString(
                current, "rejectCode", buildRejectCode(status, cfRisk, replaceGain, thirdWeakness));
        putString(
                current,
                "finalDecisionRejectCode",
                buildRejectCode(status, cfRisk, replaceGain, thirdWeakness));
        putDouble(
                current,
                "antiEvidenceScore",
                Math.max(getDouble(current, "antiEvidenceScore", 0.0d), cfRisk));
        putDouble(
                current,
                "counterEvidenceScore",
                Math.max(getDouble(current, "counterEvidenceScore", 0.0d), cfRisk));
        putDouble(
                current,
                "netEvidenceScore",
                clampSigned(getDouble(current, "evidenceScore", 0.0d) - cfRisk));
        appendReasonObject(current, summary);
    }

    private AlternativeResult findBestSamePositionAlternativeObject(
            Object current, ArrayList<Object> ranked) {
        AlternativeResult best = new AlternativeResult();
        if (current == null || ranked == null || ranked.size() == 0) return best;
        int position = getInt(current, "position", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));
        for (int i = 0; i < ranked.size(); i++) {
            Object c = ranked.get(i);
            if (c == null || c == current) continue;
            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
            if (p != position || m == mask) continue;
            double s = scoreCandidateObject(c);
            if (!best.exists || s > best.score) fillAlternativeObject(best, c, s);
        }
        return best;
    }

    private AlternativeResult findBestCrossPositionAlternativeObject(
            Object current, ArrayList<Object> ranked) {
        AlternativeResult best = new AlternativeResult();
        if (current == null || ranked == null || ranked.size() == 0) return best;
        int position = getInt(current, "position", 0);
        for (int i = 0; i < ranked.size(); i++) {
            Object c = ranked.get(i);
            if (c == null || c == current) continue;
            int p = getInt(c, "position", 0);
            if (p == position) continue;
            double s = scoreCandidateObject(c);
            if (!best.exists || s > best.score) fillAlternativeObject(best, c, s);
        }
        return best;
    }

    private AlternativeResult findBestOneNumberAlternativeObject(
            Object current, ArrayList<Object> ranked) {
        AlternativeResult best = new AlternativeResult();
        if (current == null || ranked == null || ranked.size() == 0) return best;
        int position = getInt(current, "position", 0);
        int mask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));
        for (int i = 0; i < ranked.size(); i++) {
            Object c = ranked.get(i);
            if (c == null || c == current) continue;
            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
            if (p != position || m == mask) continue;
            if (maskDiffCount(mask, m) != 2) continue;
            double s = scoreCandidateObject(c);
            if (!best.exists || s > best.score) fillAlternativeObject(best, c, s);
        }
        return best;
    }

    private void fillAlternativeObject(AlternativeResult r, Object c, double score) {
        if (r == null || c == null) return;
        r.exists = true;
        r.position = getInt(c, "position", 0);
        r.n1 = getInt(c, "n1", 0);
        r.n2 = getInt(c, "n2", 0);
        r.n3 = getInt(c, "n3", 0);
        r.k3Mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
        r.score = score;
        r.finalP = getDouble(c, "finalP", 0.0d);
        r.edge = getDouble(c, "edge", 0.0d);
        r.reason =
                "P"
                        + r.position
                        + "｜"
                        + r.n1
                        + ","
                        + r.n2
                        + ","
                        + r.n3
                        + "｜score="
                        + percent(score);
    }

    private double calcNearConflictRiskObject(Object current, ArrayList<Object> ranked) {
        if (current == null || ranked == null || ranked.size() <= 1) return 0.0d;
        double currentScore = scoreCandidateObject(current);
        int currentPosition = getInt(current, "position", 0);
        int currentMask = getInt(current, "k3Mask", getInt(current, "k3_mask", 0));
        int nearCount = 0;
        double risk = 0.0d;
        for (int i = 0; i < ranked.size(); i++) {
            Object c = ranked.get(i);
            if (c == null || c == current) continue;
            double s = scoreCandidateObject(c);
            double gap = Math.abs(currentScore - s);
            if (gap > 0.055d) continue;
            int p = getInt(c, "position", 0);
            int m = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));
            nearCount++;
            if (p != currentPosition) risk += 0.08d;
            else if (maskDiffCount(currentMask, m) <= 4) risk += 0.06d;
            else risk += 0.04d;
            if (s > currentScore) risk += 0.08d;
        }
        if (nearCount >= 3) risk += 0.12d;
        return clamp01(risk);
    }

    private AlternativeResult chooseBestAlternative(
            AlternativeResult sameAlt, AlternativeResult crossAlt, AlternativeResult oneAlt) {
        AlternativeResult best = new AlternativeResult();
        if (sameAlt != null && sameAlt.exists) {
            copyAlternative(best, sameAlt);
        }
        if (crossAlt != null && crossAlt.exists && (!best.exists || crossAlt.score > best.score)) {
            copyAlternative(best, crossAlt);
        }
        if (oneAlt != null && oneAlt.exists && (!best.exists || oneAlt.score > best.score)) {
            copyAlternative(best, oneAlt);
        }
        return best;
    }

    private void copyAlternative(AlternativeResult to, AlternativeResult from) {
        if (to == null || from == null) return;
        to.exists = from.exists;
        to.position = from.position;
        to.n1 = from.n1;
        to.n2 = from.n2;
        to.n3 = from.n3;
        to.k3Mask = from.k3Mask;
        to.score = from.score;
        to.finalP = from.finalP;
        to.edge = from.edge;
        to.reason = from.reason;
    }

    private double calcCounterfactualRisk(
            double replaceGain,
            double thirdWeakness,
            double samePositionRisk,
            double crossPositionRisk,
            double oneNumberRisk,
            double missedStrongRisk,
            double nearConflictRisk,
            double falseChampionRisk) {
        double risk = 0.0d;
        risk += posPart(replaceGain / REPLACE_GAIN_HARD) * 0.20d;
        risk += thirdWeakness * 0.18d;
        risk += samePositionRisk * 0.10d;
        risk += crossPositionRisk * 0.08d;
        risk += oneNumberRisk * 0.16d;
        risk += missedStrongRisk * 0.18d;
        risk += nearConflictRisk * 0.10d;
        risk += falseChampionRisk * 0.14d;
        return clamp01(risk);
    }

    private double calcRankStability(
            int rankNo,
            double currentScore,
            double championScore,
            double replaceGain,
            double counterfactualRisk,
            double nearConflictRisk) {
        double base = rankNo == 1 ? 0.68d : (rankNo <= 3 ? 0.54d : 0.42d);
        double gapSupport =
                rankNo == 1
                        ? clamp01((currentScore - championScore + 0.10d) / 0.10d)
                        : clamp01((championScore - currentScore + 0.10d) / 0.20d);
        double stable =
                base
                        + gapSupport * 0.18d
                        - posPart(replaceGain / REPLACE_GAIN_WARN) * 0.18d
                        - counterfactualRisk * 0.26d
                        - nearConflictRisk * 0.16d;
        return clamp01(stable);
    }

    private double calcThirdWeakness(
            Models.CandidateK3 current,
            AlternativeResult oneAlt,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.CauseChain> chains) {
        if (current == null) {
            return 0.0d;
        }
        int p = getInt(current, "position", 0);
        int n3 = getInt(current, "n3", 0);
        double teacherRisk = calcTeacherComboRisk(p, n3, n3, n3, teacherMap);
        double chainRisk = calcChainRisk(p, n3, n3, n3, chains);
        double directWeak =
                clamp01(
                        (teacherRisk * 0.46d)
                                + (chainRisk * 0.34d)
                                + (1.0d
                                                - getDouble(
                                                        current,
                                                        "thirdQuality",
                                                        getDouble(
                                                                current,
                                                                "thirdNumberQualityScore",
                                                                0.0d)))
                                        * 0.20d);
        double altGain =
                oneAlt != null && oneAlt.exists
                        ? posPart((oneAlt.score - scoreCandidate(current)) / REPLACE_GAIN_HARD)
                        : 0.0d;
        return clamp01(directWeak * 0.62d + altGain * 0.38d);
    }

    private double calcThirdWeaknessObject(Object current, AlternativeResult oneAlt) {
        if (current == null) {
            return 0.0d;
        }
        double thirdQuality =
                getDouble(
                        current,
                        "thirdQuality",
                        getDouble(
                                current,
                                "thirdNumberQualityScore",
                                getDouble(current, "needSupplyThirdQuality", 0.0d)));
        double thirdDrag = getDouble(current, "thirdDragScore", 0.0d);
        double altGain =
                oneAlt != null && oneAlt.exists
                        ? posPart(
                                (oneAlt.score - scoreCandidateObject(current)) / REPLACE_GAIN_HARD)
                        : 0.0d;
        return clamp01((1.0d - thirdQuality) * 0.34d + thirdDrag * 0.28d + altGain * 0.38d);
    }

    private String bestAlternativeText(AlternativeResult alt) {
        if (alt == null || !alt.exists) {
            return "";
        }
        return "P"
                + alt.position
                + "|K3="
                + alt.n1
                + ","
                + alt.n2
                + ","
                + alt.n3
                + "|score="
                + percent(alt.score);
    }

    private String appendFix12Summary(
            String summary,
            AlternativeResult bestAlt,
            double replaceGain,
            double thirdWeakness,
            double rankStability,
            double counterfactualRisk) {
        StringBuilder sb = new StringBuilder();
        sb.append(summary == null ? "" : summary);
        if (bestAlt != null && bestAlt.exists) {
            sb.append("｜最佳替代=");
            sb.append(bestAlternativeText(bestAlt));
        }
        sb.append("｜替换收益=");
        sb.append(signed(replaceGain));
        sb.append("｜第三号拖累=");
        sb.append(percent(thirdWeakness));
        sb.append("｜排名稳定=");
        sb.append(percent(rankStability));
        sb.append("｜反事实风险=");
        sb.append(percent(counterfactualRisk));
        return sb.toString();
    }

    private String buildCounterfactualActionAdvice(
            String status,
            double counterfactualRisk,
            double replaceGain,
            double thirdWeakness,
            double rankStability) {
        if ("RISK".equals(status)
                || counterfactualRisk >= 0.72d
                || replaceGain >= REPLACE_GAIN_HARD) {
            return "COUNTERFACTUAL_RISK_FREEZE：替代组合明显更优或反证过强，只允许最终门谨慎处理";
        }
        if (thirdWeakness >= THIRD_WEAK_LINE) {
            return "COUNTERFACTUAL_WATCH_THIRD_WEAK：第三号拖累，需要 ThirdGuard / DecisionGate 复核";
        }
        if (rankStability >= RANK_STABLE_LINE
                && counterfactualRisk <= 0.46d
                && replaceGain <= REPLACE_GAIN_WARN) {
            return "COUNTERFACTUAL_PASS_STABLE：排名经替代检验稳定，可送最终门复核";
        }
        return "COUNTERFACTUAL_WATCH：替代差距不足或样本稳定性一般，只能作为排序和诊断字段";
    }

    private String buildRejectCode(
            String status, double counterfactualRisk, double replaceGain, double thirdWeakness) {
        if ("RISK".equals(status)
                || counterfactualRisk >= 0.72d
                || replaceGain >= REPLACE_GAIN_HARD) {
            return "COUNTERFACTUAL_REPLACE_HARD_RISK";
        }
        if (thirdWeakness >= THIRD_WEAK_LINE) {
            return "COUNTERFACTUAL_THIRD_WEAK";
        }
        if ("CONFLICT".equals(status)) {
            return "COUNTERFACTUAL_NEAR_CONFLICT";
        }
        return "";
    }

    private int buildMask(int n1, int n2, int n3) {
        int mask = 0;
        if (isValidNumber(n1)) mask |= (1 << n1);
        if (isValidNumber(n2)) mask |= (1 << n2);
        if (isValidNumber(n3)) mask |= (1 << n3);
        return mask;
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }
        return b == null ? "" : b;
    }

    private void appendReasonObject(Object c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }
        String old = getString(c, "reasonSummary", "");
        if (old == null || old.length() == 0) {
            putString(c, "reasonSummary", add);
        } else if (old.indexOf("反事实排名") < 0) {
            putString(c, "reasonSummary", old + "｜" + add);
        }
        String deep = getString(c, "deepReason", "");
        if (deep == null || deep.length() == 0) {
            putString(c, "deepReason", add);
        } else if (deep.indexOf("反事实排名") < 0) {
            putString(c, "deepReason", deep + "｜" + add);
        }
    }

    private String buildSummary(
            int rankNo,
            int position,
            int n1,
            int n2,
            int n3,
            double currentScore,
            double championScore,
            double rankGap,
            AlternativeResult sameAlt,
            AlternativeResult crossAlt,
            AlternativeResult oneAlt,
            double counterScore,
            double missedStrongRisk,
            double nearConflictRisk,
            double falseChampionRisk,
            String status) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20反事实排名｜rank=");
        sb.append(rankNo);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);
        sb.append("｜当前分=");
        sb.append(percent(currentScore));
        sb.append("｜冠军分=");
        sb.append(percent(championScore));
        sb.append("｜冠军差=");
        sb.append(signed(rankGap));
        sb.append("｜反事实分=");
        sb.append(percent(counterScore));
        sb.append("｜错过强候选=");
        sb.append(percent(missedStrongRisk));
        sb.append("｜相近冲突=");
        sb.append(percent(nearConflictRisk));
        sb.append("｜伪强风险=");
        sb.append(percent(falseChampionRisk));
        sb.append("｜状态=");
        sb.append(status);

        if (sameAlt != null && sameAlt.exists) {
            sb.append("｜同位置替代=");
            sb.append(sameAlt.reason);
        }

        if (crossAlt != null && crossAlt.exists) {
            sb.append("｜跨位置替代=");
            sb.append(crossAlt.reason);
        }

        if (oneAlt != null && oneAlt.exists) {
            sb.append("｜一号替代=");
            sb.append(oneAlt.reason);
        }

        sb.append("｜说明=反事实排名只做审计和风险，不直接放行BET");

        return sb.toString();
    }

    private String statusOf(
            int rankNo,
            double counterScore,
            double missedStrongRisk,
            double nearConflictRisk,
            double falseChampionRisk) {
        if (falseChampionRisk >= 0.72d || missedStrongRisk >= 0.72d) {
            return "RISK";
        }

        if (rankNo == 1
                && counterScore >= 0.62d
                && missedStrongRisk < 0.42d
                && nearConflictRisk < 0.48d) {
            return "CHAMPION_STABLE";
        }

        if (rankNo <= 3 && counterScore >= 0.54d && falseChampionRisk < 0.58d) {
            return "TOP_USABLE";
        }

        if (nearConflictRisk >= 0.62d) {
            return "CONFLICT";
        }

        if (counterScore >= 0.44d) {
            return "WATCH";
        }

        return "WEAK";
    }

    private String buildRankKey(String targetIssue, int position, int mask, int rankNo) {
        return safe(targetIssue) + "|P" + position + "|K3_" + mask + "|R" + rankNo;
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
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

    private boolean maskContains(int mask, int number) {
        if (!isValidNumber(number)) {
            return false;
        }

        if (mask <= 0) {
            return false;
        }

        try {
            return (mask & (1 << number)) != 0;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private double posPart(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v <= 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private double negPart(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v >= 0.0d) {
            return 0.0d;
        }

        v = -v;

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
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

    private double clampSigned(double v) {
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

    private String percent(double v) {
        return String.format(Locale.US, "%.1f%%", Double.valueOf(clamp01(v) * 100.0d));
    }

    private String signed(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            v = 0.0d;
        }

        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String safe(String s) {
        return s == null ? "" : s;
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

    private static void putLong(Object obj, String field, long value) {
        if (obj == null || field == null || field.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(field);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(field);
                f.set(obj, Long.valueOf(value));
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

    private static final class CounterfactualCacheEntry {
        public double rankScore;
        public double risk;
        public double whyNotRisk;
        public double replaceGain;
        public double thirdWeakness;
        public double rankStability;
        public int falseCauseCount;
        public String status;
        public String summary;
        public long createdAt;
    }

    private static final class AlternativeResult {
        boolean exists;
        int position;
        int n1;
        int n2;
        int n3;
        int k3Mask;
        double score;
        double finalP;
        double edge;
        String reason;
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:CounterfactualRankEngine";

    public enum V57B08Mode {
        PRE_DRAW_INFERENCE,
        POST_DRAW_RESEARCH
    }

    public static final class V57CounterfactualRankEvidence {
        public V57B08Mode mode;
        public long cutoffTime;
        public long newestFeatureTime;
        public double observedScore;
        public double alternateScore;
        public double delta;
        public int sample;
        public boolean labelVisible;
        public boolean pitValid;
        public String status = "";
        public String hash = "";

        public String canonical() {
            return String.valueOf(mode)
                    + '|'
                    + cutoffTime
                    + '|'
                    + newestFeatureTime
                    + '|'
                    + observedScore
                    + '|'
                    + alternateScore
                    + '|'
                    + delta
                    + '|'
                    + sample
                    + '|'
                    + labelVisible
                    + '|'
                    + pitValid
                    + '|'
                    + status;
        }
    }

    public static V57CounterfactualRankEvidence v57b08Evaluate(
            V57B08Mode mode,
            long cutoff,
            long newestFeature,
            double observed,
            double alternate,
            int sample,
            boolean labelVisible) {
        V57CounterfactualRankEvidence e = new V57CounterfactualRankEvidence();
        e.mode = mode == null ? V57B08Mode.PRE_DRAW_INFERENCE : mode;
        e.cutoffTime = cutoff;
        e.newestFeatureTime = newestFeature;
        e.observedScore = observed;
        e.alternateScore = alternate;
        e.delta = observed - alternate;
        e.sample = Math.max(0, sample);
        e.labelVisible = labelVisible;
        e.pitValid =
                newestFeature <= cutoff
                        && (e.mode == V57B08Mode.POST_DRAW_RESEARCH || !labelVisible);
        e.status = e.pitValid ? (e.sample >= 60 ? "VALID" : "WATCH_SAMPLE") : "PIT_REJECT";
        e.hash = v57b08Hash(e.canonical());
        return e;
    }

    public static boolean v57b08PreDrawMayReadTargetLabel() {
        return false;
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
