package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

/**
 * K3RoleComboEngine.java
 *
 * <p>第9/17 新增核心引擎。
 *
 * <p>作用： 1. 固定玩法：单位置 + 固定K=3。 2. K3 不再简单取前三名，而是生成： 主攻号 补强号 防守补位号 3. 计算： roleDiversity pairConflict
 * coverageBalance comboForwardHitRate comboWilsonLower comboValidLevel
 *
 * <p>验收标准： 1. 最终K3有角色说明。 2. 组合冲突高时不能进 Elite45 BET。
 *
 * <p>注意： 本文件只生成角色组合，不直接BET，不结算，不写真实盈亏。 最终是否出手必须交给 DecisionGateEngine.elite45FinalCheck。
 */
public final class K3RoleComboEngine {

    public static final String ROLE_MAIN = "MAIN_ATTACK";
    public static final String ROLE_SUPPORT = "SUPPORT_STRENGTHEN";
    public static final String ROLE_DEFENSE = "DEFENSE_COVER";

    public static final String LEVEL_READY = "READY";
    public static final String LEVEL_SHADOW = "SHADOW";
    public static final String LEVEL_RISK = "RISK";
    public static final String LEVEL_NO_SAMPLE = "NO_SAMPLE";

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;
    private static final int FIXED_K = 3;

    private static final double BASE_K3_RATE = 0.300d;
    private static final double WILSON_Z_95 = 1.96d;

    private static final int TOP_NUMBER_LIMIT = 7;

    private static final double READY_DIVERSITY_MIN = 0.60d;
    private static final double READY_CONFLICT_MAX = 0.30d;
    private static final double READY_COVERAGE_MIN = 0.55d;
    private static final double READY_WILSON_MIN = 0.335d;

    private static final double RISK_CONFLICT_HIGH = 0.55d;
    private static final double RISK_DIVERSITY_LOW = 0.35d;

    public K3RoleComboEngine() {}

    public Models.K3RoleCombo buildBestCombo(
            int position,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        return buildBestCombo("", "", position, reasonMap, contrastiveMap, causalMap, dbStore);
    }

    public Models.K3RoleCombo buildBestCombo(
            String baseIssue,
            String targetIssue,
            int position,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        if (!isValidPosition(position)) {
            return buildEmptyCombo(baseIssue, targetIssue, position, "INVALID_POSITION");
        }

        ArrayList<NumberRoleScore> numbers =
                buildNumberScores(position, reasonMap, contrastiveMap, causalMap);

        if (numbers.size() < FIXED_K) {
            return buildEmptyCombo(baseIssue, targetIssue, position, "NOT_ENOUGH_NUMBER_SCORE");
        }

        ArrayList<Models.K3RoleCombo> combos =
                buildCandidateCombos(
                        baseIssue,
                        targetIssue,
                        position,
                        numbers,
                        reasonMap,
                        contrastiveMap,
                        causalMap,
                        dbStore);

        if (combos.size() <= 0) {
            return buildEmptyCombo(baseIssue, targetIssue, position, "NO_VALID_COMBO");
        }

        Collections.sort(
                combos,
                new Comparator<Models.K3RoleCombo>() {
                    public int compare(Models.K3RoleCombo a, Models.K3RoleCombo b) {
                        double sa = getDouble(a, "comboScore", 0.0d);
                        double sb = getDouble(b, "comboScore", 0.0d);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        double wa = getDouble(a, "comboWilsonLower", 0.0d);
                        double wb = getDouble(b, "comboWilsonLower", 0.0d);

                        if (wa > wb) {
                            return -1;
                        }

                        if (wa < wb) {
                            return 1;
                        }

                        double ca = getDouble(a, "pairConflict", 0.0d);
                        double cb = getDouble(b, "pairConflict", 0.0d);

                        if (ca < cb) {
                            return -1;
                        }

                        if (ca > cb) {
                            return 1;
                        }

                        return 0;
                    }
                });

        return combos.get(0);
    }

    public Map<Integer, Models.K3RoleCombo> buildBestCombosByPosition(
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        LinkedHashMap<Integer, Models.K3RoleCombo> out =
                new LinkedHashMap<Integer, Models.K3RoleCombo>();

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            Models.K3RoleCombo combo =
                    buildBestCombo(
                            baseIssue,
                            targetIssue,
                            p,
                            reasonMap,
                            contrastiveMap,
                            causalMap,
                            dbStore);
            out.put(Integer.valueOf(p), combo);
        }

        return out;
    }

    public ArrayList<Models.K3RoleCombo> buildBestComboList(
            String baseIssue,
            String targetIssue,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        Map<Integer, Models.K3RoleCombo> map =
                buildBestCombosByPosition(
                        baseIssue, targetIssue, reasonMap, contrastiveMap, causalMap, dbStore);
        ArrayList<Models.K3RoleCombo> list = new ArrayList<Models.K3RoleCombo>();

        for (Models.K3RoleCombo c : map.values()) {
            if (c != null) {
                list.add(c);
            }
        }

        Collections.sort(
                list,
                new Comparator<Models.K3RoleCombo>() {
                    public int compare(Models.K3RoleCombo a, Models.K3RoleCombo b) {
                        double sa = getDouble(a, "comboScore", 0.0d);
                        double sb = getDouble(b, "comboScore", 0.0d);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        return getInt(a, "position", 0) - getInt(b, "position", 0);
                    }
                });

        return list;
    }

    public ArrayList<Models.K3RoleCombo> buildCandidateCombos(
            String baseIssue,
            String targetIssue,
            int position,
            ArrayList<NumberRoleScore> numbers,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        ArrayList<Models.K3RoleCombo> out = new ArrayList<Models.K3RoleCombo>();

        if (!isValidPosition(position) || numbers == null || numbers.size() < FIXED_K) {
            return out;
        }

        ArrayList<NumberRoleScore> top = new ArrayList<NumberRoleScore>();

        for (int i = 0; i < numbers.size() && i < TOP_NUMBER_LIMIT; i++) {
            top.add(numbers.get(i));
        }

        for (int i = 0; i < top.size(); i++) {
            NumberRoleScore main = top.get(i);

            if (main == null || !isValidNumber(main.number)) {
                continue;
            }

            for (int j = 0; j < top.size(); j++) {
                if (j == i) {
                    continue;
                }

                NumberRoleScore support = top.get(j);

                if (support == null
                        || !isValidNumber(support.number)
                        || support.number == main.number) {
                    continue;
                }

                for (int k = 0; k < top.size(); k++) {
                    if (k == i || k == j) {
                        continue;
                    }

                    NumberRoleScore defense = top.get(k);

                    if (defense == null || !isValidNumber(defense.number)) {
                        continue;
                    }

                    if (defense.number == main.number || defense.number == support.number) {
                        continue;
                    }

                    Models.K3RoleCombo combo =
                            evaluateCombo(
                                    baseIssue,
                                    targetIssue,
                                    position,
                                    main,
                                    support,
                                    defense,
                                    reasonMap,
                                    contrastiveMap,
                                    causalMap,
                                    dbStore);

                    out.add(combo);
                }
            }
        }

        Collections.sort(
                out,
                new Comparator<Models.K3RoleCombo>() {
                    public int compare(Models.K3RoleCombo a, Models.K3RoleCombo b) {
                        double sa = getDouble(a, "comboScore", 0.0d);
                        double sb = getDouble(b, "comboScore", 0.0d);

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        return 0;
                    }
                });

        return out;
    }

    private ArrayList<NumberRoleScore> buildNumberScores(
            int position,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap) {
        ArrayList<NumberRoleScore> list = new ArrayList<NumberRoleScore>();

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            String key = keyOf(position, n);

            Models.PreDrawReason reason = reasonMap == null ? null : reasonMap.get(key);
            Models.ContrastiveProfile contrastive =
                    contrastiveMap == null ? null : contrastiveMap.get(key);
            Models.ReasonCausalProfile causal = causalMap == null ? null : causalMap.get(key);

            NumberRoleScore s = buildNumberScore(position, n, reason, contrastive, causal);
            list.add(s);
        }

        Collections.sort(
                list,
                new Comparator<NumberRoleScore>() {
                    public int compare(NumberRoleScore a, NumberRoleScore b) {
                        if (a.finalScore > b.finalScore) {
                            return -1;
                        }

                        if (a.finalScore < b.finalScore) {
                            return 1;
                        }

                        if (a.attackScore > b.attackScore) {
                            return -1;
                        }

                        if (a.attackScore < b.attackScore) {
                            return 1;
                        }

                        return a.number - b.number;
                    }
                });

        return list;
    }

    private NumberRoleScore buildNumberScore(
            int position,
            int number,
            Models.PreDrawReason reason,
            Models.ContrastiveProfile contrastive,
            Models.ReasonCausalProfile causal) {
        NumberRoleScore s = new NumberRoleScore();

        s.position = position;
        s.number = number;
        s.key = keyOf(position, number);

        s.reasonScore = getDouble(reason, "reasonScore", 0.0d);
        s.reasonEdge = getDouble(reason, "reasonEdge", 0.0d);
        s.validReasonCount = getInt(reason, "validReasonCount", 0);
        s.falseReasonCount = getInt(reason, "falseReasonCount", 0);
        s.positiveAtoms = getString(reason, "positiveAtoms", "");
        s.negativeAtoms = getString(reason, "negativeAtoms", "");
        s.contradictionAtoms = getString(reason, "contradictionAtoms", "");
        s.reasonFingerprint = getString(reason, "reasonFingerprint", "");
        s.reasonText =
                firstNonEmpty(
                        getString(reason, "reasonTextPreDraw", ""),
                        getString(reason, "reasonText", ""));

        s.hitSimilarity = getDouble(contrastive, "hitSimilarityScore", 0.0d);
        s.missSimilarity = getDouble(contrastive, "missSimilarityScore", 0.0d);
        s.contrastiveEdge = getDouble(contrastive, "reasonEdge", 0.0d);
        s.hitFactors = getString(contrastive, "hitFactorList", "");
        s.missFactors = getString(contrastive, "missFactorList", "");
        s.differenceSignature = getString(contrastive, "differenceSignature", "");

        s.causalLevel = getString(causal, "validLevel", getString(causal, "causalLevel", ""));
        s.wilsonLower = getDouble(causal, "wilsonLower", 0.0d);
        s.recentWilsonLower = getDouble(causal, "recentWilsonLower", 0.0d);
        s.causalLift = getDouble(causal, "causalLift", 0.0d);
        s.causalConfidence =
                getDouble(causal, "causalConfidence", getDouble(causal, "confidence", 0.0d));
        s.decayScore = getDouble(causal, "decayScore", 0.0d);
        s.falseScore = getDouble(causal, "falseScore", 0.0d);
        s.causalRiskScore = getDouble(causal, "riskScore", 0.0d);
        s.validAtoms = getString(causal, "validReasonAtoms", "");
        s.usefulAtoms = getString(causal, "usefulReasonAtoms", "");
        s.falseAtoms = getString(causal, "falseAtoms", "");
        s.decayAtoms = getString(causal, "decayAtoms", "");

        double causalBoost = causalLevelBoost(s.causalLevel, s.causalLift, s.causalConfidence);
        double causalPenalty = causalLevelPenalty(s.causalLevel, s.falseScore, s.decayScore);

        s.attackScore =
                clamp01(
                        s.reasonScore * 0.28d
                                + Math.max(0.0d, s.reasonEdge) * 0.18d
                                + s.hitSimilarity * 0.20d
                                + Math.max(0.0d, s.contrastiveEdge) * 0.12d
                                + s.wilsonLower * 0.15d
                                + causalBoost * 0.16d
                                - s.missSimilarity * 0.18d
                                - causalPenalty * 0.16d);

        s.supportScore =
                clamp01(
                        s.reasonScore * 0.18d
                                + s.hitSimilarity * 0.16d
                                + s.causalConfidence * 0.16d
                                + s.wilsonLower * 0.15d
                                + atomStrength(s.positiveAtoms, s.validAtoms, s.usefulAtoms) * 0.14d
                                + Math.max(0.0d, s.reasonEdge) * 0.10d
                                - s.missSimilarity * 0.12d
                                - s.falseScore * 0.12d);

        s.defenseScore =
                clamp01(
                        s.reasonScore * 0.12d
                                + s.hitSimilarity * 0.10d
                                + s.causalConfidence * 0.10d
                                + (1.0d - s.missSimilarity) * 0.20d
                                + (1.0d - s.falseScore) * 0.14d
                                + (1.0d - s.decayScore) * 0.12d
                                + Math.max(0.0d, s.wilsonLower - 0.25d) * 0.30d
                                - Math.max(0.0d, -s.reasonEdge) * 0.10d);

        s.riskScore =
                clamp01(
                        s.missSimilarity * 0.34d
                                + s.falseScore * 0.26d
                                + s.decayScore * 0.18d
                                + Math.max(0.0d, -s.reasonEdge) * 0.12d
                                + Math.max(0, s.falseReasonCount) * 0.035d);

        s.finalScore =
                clamp01(
                        s.attackScore * 0.44d
                                + s.supportScore * 0.30d
                                + s.defenseScore * 0.18d
                                - s.riskScore * 0.24d);

        return s;
    }

    private Models.K3RoleCombo evaluateCombo(
            String baseIssue,
            String targetIssue,
            int position,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense,
            Map<String, Models.PreDrawReason> reasonMap,
            Map<String, Models.ContrastiveProfile> contrastiveMap,
            Map<String, Models.ReasonCausalProfile> causalMap,
            DbStore dbStore) {
        Models.K3RoleCombo c = new Models.K3RoleCombo();

        int n1 = main.number;
        int n2 = support.number;
        int n3 = defense.number;
        int k3Mask = buildMask(n1, n2, n3);

        ComboHistory h = loadComboHistory(dbStore, position, k3Mask, main, support, defense);

        double roleDiversity = calcRoleDiversity(main, support, defense);
        double pairConflict = calcPairConflict(main, support, defense);
        double coverageBalance = calcCoverageBalance(main, support, defense);
        double comboForwardHitRate =
                h.sampleCount > 0 ? h.hitRate : estimateForwardHitRate(main, support, defense);
        double comboWilsonLower =
                h.sampleCount > 0
                        ? h.wilsonLower
                        : estimateWilson(comboForwardHitRate, h.sampleCount);
        double comboScore =
                calcComboScore(
                        main,
                        support,
                        defense,
                        roleDiversity,
                        pairConflict,
                        coverageBalance,
                        comboForwardHitRate,
                        comboWilsonLower,
                        h.confidence);

        String comboValidLevel =
                judgeComboLevel(
                        roleDiversity,
                        pairConflict,
                        coverageBalance,
                        comboForwardHitRate,
                        comboWilsonLower,
                        h.sampleCount,
                        comboScore,
                        main,
                        support,
                        defense);

        String roleSummary =
                buildRoleSummary(
                        position,
                        n1,
                        n2,
                        n3,
                        main,
                        support,
                        defense,
                        roleDiversity,
                        pairConflict,
                        coverageBalance,
                        comboForwardHitRate,
                        comboWilsonLower,
                        comboValidLevel);

        String comboKey = buildComboKey(position, n1, n2, n3, main, support, defense);

        putString(c, "comboKey", comboKey);
        putString(c, "baseIssue", safe(baseIssue));
        putString(c, "targetIssue", safe(targetIssue));
        putString(c, "roleSummary", roleSummary);
        putString(c, "summaryText", roleSummary);
        putString(c, "comboValidLevel", comboValidLevel);
        putString(c, "validLevel", comboValidLevel);
        putString(c, "mainRole", ROLE_MAIN);
        putString(c, "supportRole", ROLE_SUPPORT);
        putString(c, "defenseRole", ROLE_DEFENSE);
        putString(c, "mainReason", main.reasonText);
        putString(c, "supportReason", support.reasonText);
        putString(c, "defenseReason", defense.reasonText);
        putString(c, "mainReasonFingerprint", main.reasonFingerprint);
        putString(c, "supportReasonFingerprint", support.reasonFingerprint);
        putString(c, "defenseReasonFingerprint", defense.reasonFingerprint);
        putString(
                c,
                "hitFactorList",
                mergeText(main.hitFactors, support.hitFactors, defense.hitFactors));
        putString(
                c,
                "missFactorList",
                mergeText(main.missFactors, support.missFactors, defense.missFactors));
        putString(
                c,
                "differenceSignature",
                buildDifferenceSignature(position, n1, n2, n3, main, support, defense));

        putInt(c, "position", position);
        putInt(c, "mainNumber", n1);
        putInt(c, "supportNumber", n2);
        putInt(c, "defenseNumber", n3);
        putInt(c, "n1", n1);
        putInt(c, "n2", n2);
        putInt(c, "n3", n3);
        putInt(c, "k3Mask", k3Mask);
        putInt(c, "sampleCount", h.sampleCount);
        putInt(c, "hitCount", h.hitCount);
        putInt(c, "missCount", h.missCount);
        putLong(c, "createdAt", System.currentTimeMillis());

        putDouble(c, "mainScore", main.attackScore);
        putDouble(c, "supportScore", support.supportScore);
        putDouble(c, "defenseScore", defense.defenseScore);
        putDouble(c, "comboScore", comboScore);
        putDouble(c, "roleDiversity", roleDiversity);
        putDouble(c, "pairConflict", pairConflict);
        putDouble(c, "coverageBalance", coverageBalance);
        putDouble(c, "comboForwardHitRate", comboForwardHitRate);
        putDouble(c, "comboWilsonLower", comboWilsonLower);
        putDouble(c, "comboConfidence", h.confidence);
        putDouble(
                c,
                "avgHitSimilarity",
                avg(main.hitSimilarity, support.hitSimilarity, defense.hitSimilarity));
        putDouble(
                c,
                "avgMissSimilarity",
                avg(main.missSimilarity, support.missSimilarity, defense.missSimilarity));
        putDouble(c, "avgReasonEdge", avg(main.reasonEdge, support.reasonEdge, defense.reasonEdge));
        putDouble(
                c,
                "avgWilsonLower",
                avg(main.wilsonLower, support.wilsonLower, defense.wilsonLower));
        putDouble(
                c,
                "riskScore",
                clamp01(
                        pairConflict * 0.38d
                                + avg(main.riskScore, support.riskScore, defense.riskScore) * 0.42d
                                + (1.0d - roleDiversity) * 0.10d
                                + (1.0d - coverageBalance) * 0.10d));

        saveComboProfileIfPossible(dbStore, c);

        return c;
    }

    private Models.K3RoleCombo buildEmptyCombo(
            String baseIssue, String targetIssue, int position, String reason) {
        Models.K3RoleCombo c = new Models.K3RoleCombo();

        putString(c, "comboKey", "EMPTY|P" + position);
        putString(c, "baseIssue", safe(baseIssue));
        putString(c, "targetIssue", safe(targetIssue));
        putString(c, "comboValidLevel", LEVEL_NO_SAMPLE);
        putString(c, "validLevel", LEVEL_NO_SAMPLE);
        putString(c, "roleSummary", "K3角色组合失败：" + safe(reason) + "｜位置=" + position);
        putString(c, "summaryText", "K3角色组合失败：" + safe(reason) + "｜位置=" + position);

        putInt(c, "position", position);
        putInt(c, "mainNumber", 0);
        putInt(c, "supportNumber", 0);
        putInt(c, "defenseNumber", 0);
        putInt(c, "n1", 0);
        putInt(c, "n2", 0);
        putInt(c, "n3", 0);
        putInt(c, "k3Mask", 0);
        putLong(c, "createdAt", System.currentTimeMillis());

        putDouble(c, "comboScore", 0.0d);
        putDouble(c, "roleDiversity", 0.0d);
        putDouble(c, "pairConflict", 1.0d);
        putDouble(c, "coverageBalance", 0.0d);
        putDouble(c, "comboForwardHitRate", 0.0d);
        putDouble(c, "comboWilsonLower", 0.0d);
        putDouble(c, "riskScore", 1.0d);

        return c;
    }

    private double calcComboScore(
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense,
            double roleDiversity,
            double pairConflict,
            double coverageBalance,
            double forwardHitRate,
            double wilson,
            double confidence) {
        double roleScore =
                main.attackScore * 0.42d
                        + support.supportScore * 0.30d
                        + defense.defenseScore * 0.18d;
        double forwardScore = forwardHitRate * 0.30d + wilson * 0.35d + confidence * 0.10d;

        double score =
                roleScore
                        + roleDiversity * 0.15d
                        + coverageBalance * 0.12d
                        + forwardScore * 0.25d
                        - pairConflict * 0.25d
                        - avg(main.riskScore, support.riskScore, defense.riskScore) * 0.16d;

        return clamp01(score);
    }

    private String judgeComboLevel(
            double roleDiversity,
            double pairConflict,
            double coverageBalance,
            double forwardHitRate,
            double wilson,
            int sample,
            double comboScore,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense) {
        if (pairConflict >= RISK_CONFLICT_HIGH) {
            return LEVEL_RISK;
        }

        if (roleDiversity <= RISK_DIVERSITY_LOW) {
            return LEVEL_RISK;
        }

        if (main.riskScore >= 0.70d || support.riskScore >= 0.75d || defense.riskScore >= 0.78d) {
            return LEVEL_RISK;
        }

        if (sample <= 0) {
            if (comboScore >= 0.58d
                    && roleDiversity >= READY_DIVERSITY_MIN
                    && pairConflict <= READY_CONFLICT_MAX
                    && coverageBalance >= READY_COVERAGE_MIN
                    && avg(main.missSimilarity, support.missSimilarity, defense.missSimilarity)
                            <= 0.38d) {
                return LEVEL_SHADOW;
            }

            return LEVEL_NO_SAMPLE;
        }

        if (roleDiversity >= READY_DIVERSITY_MIN
                && pairConflict <= READY_CONFLICT_MAX
                && coverageBalance >= READY_COVERAGE_MIN
                && wilson >= READY_WILSON_MIN
                && forwardHitRate >= BASE_K3_RATE
                && comboScore >= 0.55d) {
            return LEVEL_READY;
        }

        if (comboScore >= 0.42d && pairConflict <= 0.45d) {
            return LEVEL_SHADOW;
        }

        return LEVEL_RISK;
    }

    private double calcRoleDiversity(
            NumberRoleScore main, NumberRoleScore support, NumberRoleScore defense) {
        LinkedHashSet<String> mainAtoms =
                parseAtoms(mergeText(main.positiveAtoms, main.validAtoms, main.usefulAtoms));
        LinkedHashSet<String> supportAtoms =
                parseAtoms(
                        mergeText(support.positiveAtoms, support.validAtoms, support.usefulAtoms));
        LinkedHashSet<String> defenseAtoms =
                parseAtoms(
                        mergeText(defense.positiveAtoms, defense.validAtoms, defense.usefulAtoms));

        double d12 = 1.0d - jaccard(mainAtoms, supportAtoms);
        double d13 = 1.0d - jaccard(mainAtoms, defenseAtoms);
        double d23 = 1.0d - jaccard(supportAtoms, defenseAtoms);

        double score = avg(d12, d13, d23);

        if (main.number != support.number
                && main.number != defense.number
                && support.number != defense.number) {
            score += 0.08d;
        }

        if (!safe(main.causalLevel).equals(safe(support.causalLevel))) {
            score += 0.04d;
        }

        if (!safe(main.causalLevel).equals(safe(defense.causalLevel))) {
            score += 0.04d;
        }

        return clamp01(score);
    }

    private double calcPairConflict(
            NumberRoleScore main, NumberRoleScore support, NumberRoleScore defense) {
        double c12 = pairConflict(main, support);
        double c13 = pairConflict(main, defense);
        double c23 = pairConflict(support, defense);

        double base = avg(c12, c13, c23);

        double miss = avg(main.missSimilarity, support.missSimilarity, defense.missSimilarity);
        double falseRisk = avg(main.falseScore, support.falseScore, defense.falseScore);
        double decay = avg(main.decayScore, support.decayScore, defense.decayScore);

        return clamp01(base * 0.52d + miss * 0.18d + falseRisk * 0.18d + decay * 0.12d);
    }

    private double pairConflict(NumberRoleScore a, NumberRoleScore b) {
        if (a == null || b == null) {
            return 1.0d;
        }

        LinkedHashSet<String> aMiss =
                parseAtoms(mergeText(a.negativeAtoms, a.missFactors, a.falseAtoms, a.decayAtoms));
        LinkedHashSet<String> bMiss =
                parseAtoms(mergeText(b.negativeAtoms, b.missFactors, b.falseAtoms, b.decayAtoms));
        LinkedHashSet<String> aPos =
                parseAtoms(mergeText(a.positiveAtoms, a.validAtoms, a.usefulAtoms));
        LinkedHashSet<String> bPos =
                parseAtoms(mergeText(b.positiveAtoms, b.validAtoms, b.usefulAtoms));

        double missOverlap = jaccard(aMiss, bMiss);
        double posConflict = crossOverlap(aPos, bMiss);
        double reverseConflict = crossOverlap(bPos, aMiss);

        double riskAvg = avg(a.riskScore, b.riskScore);
        double missAvg = avg(a.missSimilarity, b.missSimilarity);

        return clamp01(
                missOverlap * 0.32d
                        + posConflict * 0.22d
                        + reverseConflict * 0.22d
                        + riskAvg * 0.14d
                        + missAvg * 0.10d);
    }

    private double calcCoverageBalance(
            NumberRoleScore main, NumberRoleScore support, NumberRoleScore defense) {
        double a = clamp01(main.attackScore);
        double b = clamp01(support.supportScore);
        double c = clamp01(defense.defenseScore);

        double avg = avg(a, b, c);
        double var = ((a - avg) * (a - avg) + (b - avg) * (b - avg) + (c - avg) * (c - avg)) / 3.0d;

        double balance = 1.0d - Math.sqrt(var);

        double roleFit = 0.0d;
        roleFit += a >= b ? 0.16d : 0.06d;
        roleFit += b >= c * 0.75d ? 0.16d : 0.06d;
        roleFit += c >= 0.20d ? 0.16d : 0.04d;

        return clamp01(balance * 0.70d + roleFit);
    }

    private double estimateForwardHitRate(
            NumberRoleScore main, NumberRoleScore support, NumberRoleScore defense) {
        double avgWilson = avg(main.wilsonLower, support.wilsonLower, defense.wilsonLower);
        double hitSim = avg(main.hitSimilarity, support.hitSimilarity, defense.hitSimilarity);
        double missSim = avg(main.missSimilarity, support.missSimilarity, defense.missSimilarity);
        double edge = avg(main.reasonEdge, support.reasonEdge, defense.reasonEdge);

        double estimate =
                BASE_K3_RATE
                        + Math.max(0.0d, avgWilson - BASE_K3_RATE) * 0.65d
                        + hitSim * 0.055d
                        + Math.max(0.0d, edge) * 0.040d
                        - missSim * 0.045d
                        - avg(main.falseScore, support.falseScore, defense.falseScore) * 0.030d;

        return clamp01(estimate);
    }

    private double estimateWilson(double forwardHitRate, int sample) {
        if (sample <= 0) {
            sample = 30;
        }

        int hit = (int) Math.round(forwardHitRate * sample);
        return wilsonLower(hit, sample);
    }

    private ComboHistory loadComboHistory(
            DbStore dbStore,
            int position,
            int k3Mask,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense) {
        ComboHistory h = new ComboHistory();

        if (dbStore == null || !isValidPosition(position) || k3Mask <= 0) {
            h.sampleCount = 0;
            h.hitCount = 0;
            h.missCount = 0;
            h.hitRate = 0.0d;
            h.wilsonLower = 0.0d;
            h.confidence = 0.0d;
            return h;
        }

        String roleKey = buildRoleKey(main, support, defense);

        Object cached =
                call(
                        dbStore,
                        "loadComboRoleProfile",
                        new Class[] {String.class, int.class, int.class},
                        new Object[] {roleKey, Integer.valueOf(position), Integer.valueOf(k3Mask)});

        if (cached == null) {
            cached =
                    call(
                            dbStore,
                            "getComboRoleProfileValues",
                            new Class[] {String.class, int.class, int.class},
                            new Object[] {
                                roleKey, Integer.valueOf(position), Integer.valueOf(k3Mask)
                            });
        }

        if (cached == null) {
            cached =
                    call(
                            dbStore,
                            "loadComboRoleProfile",
                            new Class[] {String.class},
                            new Object[] {buildComboProfileKey(roleKey, position, k3Mask)});
        }

        h.sampleCount = getProfileInt(cached, "sampleCount", "sample_count", 0);
        h.hitCount = getProfileInt(cached, "hitCount", "hit_count", 0);
        h.missCount = getProfileInt(cached, "missCount", "miss_count", 0);

        if (h.sampleCount <= 0) {
            h.sampleCount = h.hitCount + h.missCount;
        }

        if (h.missCount <= 0 && h.sampleCount > h.hitCount) {
            h.missCount = h.sampleCount - h.hitCount;
        }

        h.hitRate = getProfileDouble(cached, "hitRate", "hit_rate", -1.0d);

        if (h.hitRate < 0.0d) {
            h.hitRate = h.sampleCount > 0 ? ((double) h.hitCount) / ((double) h.sampleCount) : 0.0d;
        }

        h.hitRate = clamp01(h.hitRate);
        h.wilsonLower = wilsonLower(h.hitCount, h.sampleCount);
        h.confidence = confidenceFromSample(h.sampleCount);

        return h;
    }

    private void saveComboProfileIfPossible(DbStore dbStore, Models.K3RoleCombo combo) {
        if (dbStore == null || combo == null) {
            return;
        }

        Object v =
                call(
                        dbStore,
                        "saveComboRoleProfile",
                        new Class[] {Models.K3RoleCombo.class},
                        new Object[] {combo});

        if (v != null) {
            return;
        }

        call(dbStore, "saveComboRoleProfile", new Class[] {Object.class}, new Object[] {combo});
    }

    private Object call(Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return null;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            return null;
        }
    }

    private double causalLevelBoost(String level, double lift, double confidence) {
        if ("A_VALID".equals(level)) {
            return clamp01(0.55d + Math.max(0.0d, lift) * 2.0d + confidence * 0.20d);
        }

        if ("B_USEFUL".equals(level)) {
            return clamp01(0.28d + Math.max(0.0d, lift) * 1.2d + confidence * 0.12d);
        }

        return 0.0d;
    }

    private double causalLevelPenalty(String level, double falseScore, double decayScore) {
        if ("E_FALSE".equals(level)) {
            return clamp01(0.65d + falseScore * 0.30d);
        }

        if ("D_DECAY".equals(level)) {
            return clamp01(0.42d + decayScore * 0.30d);
        }

        if ("C_EXPLAIN_ONLY".equals(level)) {
            return 0.08d;
        }

        return 0.0d;
    }

    private double atomStrength(String positiveAtoms, String validAtoms, String usefulAtoms) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();
        addAll(set, parseAtoms(positiveAtoms));
        addAll(set, parseAtoms(validAtoms));
        addAll(set, parseAtoms(usefulAtoms));

        if (set.size() <= 0) {
            return 0.0d;
        }

        if (set.size() >= 6) {
            return 1.0d;
        }

        return ((double) set.size()) / 6.0d;
    }

    private String buildRoleSummary(
            int position,
            int n1,
            int n2,
            int n3,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense,
            double roleDiversity,
            double pairConflict,
            double coverageBalance,
            double forwardHitRate,
            double wilson,
            String level) {
        StringBuilder sb = new StringBuilder();

        sb.append("【K3角色组合】");
        sb.append("位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(formatNumber(n1));
        sb.append(",");
        sb.append(formatNumber(n2));
        sb.append(",");
        sb.append(formatNumber(n3));
        sb.append("｜级别=");
        sb.append(level);
        sb.append("\n");

        sb.append("【主攻号】");
        sb.append(formatNumber(n1));
        sb.append("｜attack=");
        sb.append(percent(main.attackScore));
        sb.append("｜hitSim=");
        sb.append(percent(main.hitSimilarity));
        sb.append("｜missSim=");
        sb.append(percent(main.missSimilarity));
        sb.append("｜causal=");
        sb.append(safe(main.causalLevel));
        sb.append("\n");

        sb.append("【补强号】");
        sb.append(formatNumber(n2));
        sb.append("｜support=");
        sb.append(percent(support.supportScore));
        sb.append("｜reasonEdge=");
        sb.append(signed(support.reasonEdge));
        sb.append("｜wilson=");
        sb.append(percent(support.wilsonLower));
        sb.append("｜causal=");
        sb.append(safe(support.causalLevel));
        sb.append("\n");

        sb.append("【防守补位号】");
        sb.append(formatNumber(n3));
        sb.append("｜defense=");
        sb.append(percent(defense.defenseScore));
        sb.append("｜risk=");
        sb.append(percent(defense.riskScore));
        sb.append("｜false=");
        sb.append(percent(defense.falseScore));
        sb.append("｜decay=");
        sb.append(percent(defense.decayScore));
        sb.append("\n");

        sb.append("【组合检查】");
        sb.append("roleDiversity=");
        sb.append(percent(roleDiversity));
        sb.append("｜pairConflict=");
        sb.append(percent(pairConflict));
        sb.append("｜coverageBalance=");
        sb.append(percent(coverageBalance));
        sb.append("｜comboForwardHitRate=");
        sb.append(percent(forwardHitRate));
        sb.append("｜comboWilsonLower=");
        sb.append(percent(wilson));
        sb.append("\n");

        sb.append("【结论】");
        if (LEVEL_READY.equals(level)) {
            sb.append("组合角色分工清楚、冲突低、前向表现可用，允许进入后续 Elite45FinalCheck。");
        } else if (LEVEL_SHADOW.equals(level)) {
            sb.append("组合有一定结构，但样本或边际不足，只能作为 SHADOW 或候选观察。");
        } else if (LEVEL_RISK.equals(level)) {
            sb.append("组合冲突或风险偏高，不能进入 Elite45 BET。");
        } else {
            sb.append("组合样本不足，不能直接作为真实BET依据。");
        }

        sb.append("\n");
        sb.append("说明=本引擎只生成固定K=3角色组合，不直接BET。最终出手必须由 DecisionGateEngine.elite45FinalCheck 决定。");

        return sb.toString();
    }

    private String buildDifferenceSignature(
            int position,
            int n1,
            int n2,
            int n3,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense) {
        String raw =
                "P"
                        + position
                        + "|M="
                        + n1
                        + ":"
                        + shortMd5(main.reasonFingerprint)
                        + "|S="
                        + n2
                        + ":"
                        + shortMd5(support.reasonFingerprint)
                        + "|D="
                        + n3
                        + ":"
                        + shortMd5(defense.reasonFingerprint)
                        + "|HF="
                        + shortMd5(
                                mergeText(main.hitFactors, support.hitFactors, defense.hitFactors))
                        + "|MF="
                        + shortMd5(
                                mergeText(
                                        main.missFactors,
                                        support.missFactors,
                                        defense.missFactors));

        return "K3ROLE|" + raw + "|H=" + shortMd5(raw);
    }

    private String buildComboKey(
            int position,
            int n1,
            int n2,
            int n3,
            NumberRoleScore main,
            NumberRoleScore support,
            NumberRoleScore defense) {
        String raw =
                "P"
                        + position
                        + "|K3="
                        + n1
                        + ","
                        + n2
                        + ","
                        + n3
                        + "|M="
                        + safe(main.reasonFingerprint)
                        + "|S="
                        + safe(support.reasonFingerprint)
                        + "|D="
                        + safe(defense.reasonFingerprint);

        return "K3ROLE|P" + position + "|K3_" + buildMask(n1, n2, n3) + "|H=" + shortMd5(raw);
    }

    private String buildRoleKey(
            NumberRoleScore main, NumberRoleScore support, NumberRoleScore defense) {
        String raw =
                "M="
                        + safe(main.causalLevel)
                        + "|"
                        + safe(main.validAtoms)
                        + "|S="
                        + safe(support.causalLevel)
                        + "|"
                        + safe(support.validAtoms)
                        + "|D="
                        + safe(defense.causalLevel)
                        + "|"
                        + safe(defense.validAtoms)
                        + "|HF="
                        + mergeText(main.hitFactors, support.hitFactors, defense.hitFactors)
                        + "|MF="
                        + mergeText(main.missFactors, support.missFactors, defense.missFactors);

        return "ROLE|" + shortMd5(raw);
    }

    private String buildComboProfileKey(String roleKey, int position, int k3Mask) {
        return "CR|" + safe(roleKey) + "|P" + position + "|K3_" + k3Mask;
    }

    private int buildMask(int n1, int n2, int n3) {
        int mask = 0;

        if (isValidNumber(n1)) {
            mask |= 1 << n1;
        }

        if (isValidNumber(n2)) {
            mask |= 1 << n2;
        }

        if (isValidNumber(n3)) {
            mask |= 1 << n3;
        }

        return mask;
    }

    private boolean isValidPosition(int position) {
        return position >= POSITION_MIN && position <= POSITION_MAX;
    }

    private boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private double wilsonLower(int hit, int total) {
        if (total <= 0 || hit <= 0) {
            return 0.0d;
        }

        if (hit > total) {
            hit = total;
        }

        double n = (double) total;
        double phat = ((double) hit) / n;
        double z = WILSON_Z_95;
        double z2 = z * z;

        double numerator =
                phat
                        + z2 / (2.0d * n)
                        - z * Math.sqrt((phat * (1.0d - phat) + z2 / (4.0d * n)) / n);
        double denominator = 1.0d + z2 / n;

        return clamp01(numerator / denominator);
    }

    private double confidenceFromSample(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample >= 120) {
            return 1.0d;
        }

        return ((double) sample) / 120.0d;
    }

    private double jaccard(LinkedHashSet<String> a, LinkedHashSet<String> b) {
        if ((a == null || a.size() == 0) && (b == null || b.size() == 0)) {
            return 0.0d;
        }

        if (a == null || b == null || a.size() == 0 || b.size() == 0) {
            return 0.0d;
        }

        LinkedHashSet<String> all = new LinkedHashSet<String>();

        for (String s : a) {
            if (s != null && s.length() > 0) {
                all.add(s);
            }
        }

        for (String s : b) {
            if (s != null && s.length() > 0) {
                all.add(s);
            }
        }

        if (all.size() <= 0) {
            return 0.0d;
        }

        int inter = 0;

        for (String s : a) {
            if (s != null && b.contains(s)) {
                inter++;
            }
        }

        return clamp01(((double) inter) / ((double) all.size()));
    }

    private double crossOverlap(LinkedHashSet<String> a, LinkedHashSet<String> b) {
        return jaccard(a, b);
    }

    private LinkedHashSet<String> parseAtoms(String text) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        if (text == null || text.length() == 0) {
            return set;
        }

        String normalized = text.replace('|', ',');
        normalized = normalized.replace(';', ',');
        normalized = normalized.replace('，', ',');

        String[] arr = normalized.split(",");

        for (int i = 0; i < arr.length; i++) {
            String s = cleanAtom(arr[i]);

            if (s.length() > 0) {
                set.add(s);
            }
        }

        return set;
    }

    private String cleanAtom(String s) {
        if (s == null) {
            return "";
        }

        String out = s.trim();

        if (out.length() <= 0) {
            return "";
        }

        out = out.replace("\n", "");
        out = out.replace("\r", "");
        out = out.replace("\t", "");

        if (out.length() > 80) {
            out = out.substring(0, 80);
        }

        return out;
    }

    private void addAll(LinkedHashSet<String> out, LinkedHashSet<String> add) {
        if (out == null || add == null) {
            return;
        }

        for (String s : add) {
            if (s != null && s.length() > 0) {
                out.add(s);
            }
        }
    }

    private String mergeText(String a, String b, String c) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        addAll(set, parseAtoms(a));
        addAll(set, parseAtoms(b));
        addAll(set, parseAtoms(c));

        return joinAtoms(set);
    }

    private String mergeText(String a, String b, String c, String d) {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        addAll(set, parseAtoms(a));
        addAll(set, parseAtoms(b));
        addAll(set, parseAtoms(c));
        addAll(set, parseAtoms(d));

        return joinAtoms(set);
    }

    private String joinAtoms(LinkedHashSet<String> set) {
        if (set == null || set.size() == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : set) {
            if (s == null || s.length() == 0) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(s);
        }

        return sb.toString();
    }

    private int getProfileInt(Object obj, String camel, String snake, int fallback) {
        int v = getInt(obj, camel, Integer.MIN_VALUE);

        if (v != Integer.MIN_VALUE) {
            return v;
        }

        v = getInt(obj, snake, Integer.MIN_VALUE);

        if (v != Integer.MIN_VALUE) {
            return v;
        }

        return fallback;
    }

    private double getProfileDouble(Object obj, String camel, String snake, double fallback) {
        double v = getDouble(obj, camel, Double.NaN);

        if (!Double.isNaN(v)) {
            return v;
        }

        v = getDouble(obj, snake, Double.NaN);

        if (!Double.isNaN(v)) {
            return v;
        }

        return fallback;
    }

    private double avg(double a, double b) {
        return (a + b) / 2.0d;
    }

    private double avg(double a, double b, double c) {
        return (a + b + c) / 3.0d;
    }

    private double clamp01(double v) {
        if (v < 0.0d) {
            return 0.0d;
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
        String sign = v >= 0.0d ? "+" : "";
        return sign + String.format(Locale.US, "%.3f", Double.valueOf(v));
    }

    private String formatNumber(int n) {
        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null) {
            return b;
        }

        return "";
    }

    private String shortMd5(String raw) {
        if (raw == null) {
            raw = "";
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < bytes.length; i++) {
                int v = bytes[i] & 0xff;

                if (v < 16) {
                    sb.append("0");
                }

                sb.append(Integer.toHexString(v));
            }

            String s = sb.toString();

            if (s.length() > 12) {
                return s.substring(0, 12);
            }

            return s;
        } catch (Throwable e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static int getInt(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseIntObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseIntObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseIntObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static long getLong(Object obj, String fieldName, long fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseLongObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseLongObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseLongObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static double getDouble(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return parseDoubleObj(v, fallback);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return parseDoubleObj(v, fallback);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return parseDoubleObj(v, fallback);
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static String getString(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            if (obj instanceof Map) {
                Object v = ((Map) obj).get(fieldName);
                return v == null ? fallback : String.valueOf(v);
            }

            Method getMethod = obj.getClass().getMethod("get", new Class[] {String.class});
            Object v = getMethod.invoke(obj, new Object[] {fieldName});
            return v == null ? fallback : String.valueOf(v);
        } catch (Throwable e) {
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private static int parseIntObj(Object v, int fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Long) {
                return (int) ((Long) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static long parseLongObj(Object v, long fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Long) {
                return ((Long) v).longValue();
            }

            if (v instanceof Integer) {
                return ((Integer) v).longValue();
            }

            if (v instanceof Number) {
                return ((Number) v).longValue();
            }

            return Long.parseLong(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static double parseDoubleObj(Object v, double fallback) {
        if (v == null) {
            return fallback;
        }

        try {
            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Float) {
                return ((Float) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            return Double.parseDouble(String.valueOf(v));
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static void putInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putLong(Object obj, String fieldName, long value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private static void putString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static final class NumberRoleScore {
        int position;
        int number;
        String key;

        String causalLevel;
        String positiveAtoms;
        String negativeAtoms;
        String contradictionAtoms;
        String validAtoms;
        String usefulAtoms;
        String falseAtoms;
        String decayAtoms;
        String hitFactors;
        String missFactors;
        String differenceSignature;
        String reasonFingerprint;
        String reasonText;

        int validReasonCount;
        int falseReasonCount;

        double reasonScore;
        double reasonEdge;
        double hitSimilarity;
        double missSimilarity;
        double contrastiveEdge;
        double wilsonLower;
        double recentWilsonLower;
        double causalLift;
        double causalConfidence;
        double decayScore;
        double falseScore;
        double causalRiskScore;

        double attackScore;
        double supportScore;
        double defenseScore;
        double riskScore;
        double finalScore;
    }

    private static final class ComboHistory {
        int sampleCount;
        int hitCount;
        int missCount;
        double hitRate;
        double wilsonLower;
        double confidence;
    }
}
