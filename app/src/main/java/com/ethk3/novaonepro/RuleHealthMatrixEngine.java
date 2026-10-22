package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RuleHealthMatrixEngine.java
 *
 * <p>第12/25 新增核心引擎。
 *
 * <p>作用： 1. 对实时候选K3做“规则级健康矩阵”审计。 2. 把候选是否稳，拆成多条规则看： - 概率边际健康 - 因链健康 - 教师号码健康 - 反事实排名健康 - 第三防线健康 -
 * 位置健康 - 复合风险健康 - 连挂保护健康 - 样本置信健康 - K3结构均衡健康 3. 输出 Models.RuleHealth。 4. 回写 CandidateK3：
 * ruleHealthScore / ruleHealthRisk / ruleHealthPass / ruleHealthSummary。 5. 回写 CauseChain：
 * ruleHealthScore / ruleHealthRisk / ruleHealthPass / finalCauseScore。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不降低门槛。 - 不绕过 DecisionGateEngine。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class RuleHealthMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final String RULE_PROB_EDGE = "PROB_EDGE";
    private static final String RULE_CAUSE_CHAIN = "CAUSE_CHAIN";
    private static final String RULE_TEACHER_NUMBER = "TEACHER_NUMBER";
    private static final String RULE_COUNTERFACTUAL_RANK = "COUNTERFACTUAL_RANK";
    private static final String RULE_THIRD_GUARD = "THIRD_GUARD";
    private static final String RULE_POSITION_HEALTH = "POSITION_HEALTH";
    private static final String RULE_COMPOSITE_RISK = "COMPOSITE_RISK";
    private static final String RULE_STREAK_PROTECT = "STREAK_PROTECT";
    private static final String RULE_SAMPLE_CONFIDENCE = "SAMPLE_CONFIDENCE";
    private static final String RULE_K3_BALANCE = "K3_BALANCE";
    private static final String RULE_RECENT_DECAY = "RECENT_DECAY";
    private static final String RULE_HEALTH_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String RULE_HEALTH_CLOSED_LOOP_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";

    private static final double PASS_HEALTH_MIN = 0.50d;
    private static final double PASS_RISK_MAX = 0.62d;
    private static final double HARD_BAD_RISK = 0.80d;

    private static final String LINK_V2_VERSION =
            "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final int REALTIME_FULL_LIMIT = 40;
    private static final int REALTIME_AUDIT_LIMIT = 120;
    private static final long REALTIME_BUDGET_MS = 1100L;
    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_WATCH = "WATCH";
    private static final String STATUS_WATCH_STALE = "WATCH_STALE";
    private static final String STATUS_SOFT_RISK = "SOFT_RISK";
    private static final String STATUS_HARD_RISK = "HARD_RISK";

    public RuleHealthMatrixEngine() {}

    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store, candidates, history, baseIssue, targetIssue, "applyToAll_rule_health_fix14");
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store,
                candidates,
                history,
                baseIssue,
                targetIssue,
                "enrichCandidates_rule_health_fix14");
    }

    public ArrayList<Models.ComboCandidate> analyzeCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                store,
                candidates,
                history,
                baseIssue,
                targetIssue,
                "analyzeCandidates_rule_health_fix14");
    }

    public ArrayList<Models.ComboCandidate> applyRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                null,
                candidates,
                history,
                baseIssue,
                targetIssue,
                "applyRealtime_rule_health_fix14");
    }

    public ArrayList<Models.ComboCandidate> runRealtime(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToComboCandidatesRealtime(
                null, candidates, history, baseIssue, targetIssue, "runRealtime_rule_health_fix14");
    }

    public int applyAndCount(
            DbStore store, List candidates, List history, String baseIssue, String targetIssue) {
        ArrayList<Models.ComboCandidate> cast = castComboCandidates(candidates);
        applyToComboCandidatesRealtime(
                store, cast, history, baseIssue, targetIssue, "applyAndCount_rule_health_fix14");
        return cast.size();
    }

    private ArrayList<Models.ComboCandidate> applyToComboCandidatesRealtime(
            DbStore store,
            List candidates,
            List history,
            String baseIssue,
            String targetIssue,
            String source) {
        long startAt = System.currentTimeMillis();
        ArrayList<Models.ComboCandidate> list = castComboCandidates(candidates);
        ArrayList<Models.Trade> recentTrades = new ArrayList<Models.Trade>();
        if (store != null) {
            try {
                List<Models.Trade> t = store.getRecentTrades(20);
                if (t != null) {
                    recentTrades.addAll(t);
                }
            } catch (Throwable e) {
            }
        }
        String snapshotFreshness = resolveRuleSnapshotFreshness(store, targetIssue);
        int pass = 0;
        int hard = 0;
        int written = 0;
        int light = 0;
        int deferred = 0;
        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);
            if (c == null) {
                continue;
            }
            boolean overBudget = System.currentTimeMillis() - startAt >= REALTIME_BUDGET_MS;
            if (i >= REALTIME_AUDIT_LIMIT || overBudget) {
                applyDeferredRuleHealth(
                        c,
                        i + 1,
                        snapshotFreshness,
                        overBudget ? "RULE_HEALTH_BUDGET_DEFERRED" : "RULE_HEALTH_RANK_DEFERRED");
                deferred++;
                continue;
            }
            CandidateRuleBundle b;
            if (i >= REALTIME_FULL_LIMIT) {
                b = buildLightweightRuleBundle(c, i + 1, snapshotFreshness);
                light++;
            } else {
                b =
                        buildCandidateRuleBundle(
                                baseIssue,
                                targetIssue,
                                c,
                                i + 1,
                                null,
                                null,
                                null,
                                null,
                                null,
                                recentTrades);
                normalizeBundleForV2(b, snapshotFreshness);
            }
            if (b == null) {
                continue;
            }
            applyBundleToCandidate(c, b);
            putString(c, "ruleHealthFreshness", snapshotFreshness);
            putString(c, "ruleHealthVersion", LINK_V2_VERSION);
            written++;
            if (b.pass == 1) {
                pass++;
            }
            if (b.hardCount > 0 || STATUS_HARD_RISK.equals(b.status)) {
                hard++;
            }
        }
        rerankComboByRuleHealth(list);
        writeRealtimeRuleHealthState(store, source, list.size(), written, pass, hard);
        if (store != null) {
            try {
                store.setState("rule_health_realtime_light", String.valueOf(light));
                store.setState("rule_health_realtime_deferred", String.valueOf(deferred));
                store.setState("rule_health_realtime_freshness", snapshotFreshness);
                store.setState(
                        "rule_health_realtime_elapsed_ms",
                        String.valueOf(System.currentTimeMillis() - startAt));
                store.setState("rule_health_realtime_version_v2", LINK_V2_VERSION);
            } catch (Throwable e) {
            }
        }
        return list;
    }

    public ArrayList<Models.RuleHealth> buildRuleHealthMatrix(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> rankList,
            List<Models.ThirdGuardStat> thirdGuardList,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.RuleHealth> out = new ArrayList<Models.RuleHealth>();

        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);

        if (list.size() == 0) {
            return out;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            CandidateRuleBundle bundle =
                    buildCandidateRuleBundle(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            teacherMap,
                            rankList,
                            thirdGuardList,
                            positionHealthMap,
                            chains,
                            recentTrades);

            if (bundle == null) {
                continue;
            }

            out.addAll(bundle.rules);
            applyBundleToCandidate(c, bundle);
        }

        applyRulesToCauseChains(chains, out);
        rerankByRuleHealth(candidates);

        return out;
    }

    public ArrayList<Models.RuleHealth> buildRuleHealthMatrix(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildRuleHealthMatrix(
                baseIssue, targetIssue, candidates, null, null, null, null, null, null);
    }

    public ArrayList<Models.RuleHealth> buildRuleHealthMatrix(List<Models.CandidateK3> candidates) {
        return buildRuleHealthMatrix("", "", candidates, null, null, null, null, null, null);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<Models.RuleHealth> rules) {
        if (candidates == null || rules == null || candidates.size() == 0 || rules.size() == 0) {
            return;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int position = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            CandidateRuleBundle bundle = summarizeRulesForCandidate(position, mask, rules);

            if (bundle != null) {
                applyBundleToCandidate(c, bundle);
            }
        }

        rerankByRuleHealth(candidates);
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.RuleHealth> rules) {
        applyRulesToCauseChains(chains, rules);
    }

    private CandidateRuleBundle buildCandidateRuleBundle(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> rankList,
            List<Models.ThirdGuardStat> thirdGuardList,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        if (c == null) {
            return null;
        }

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        if (!isValidPosition(position)
                || !isValidNumber(n1)
                || !isValidNumber(n2)
                || !isValidNumber(n3)) {
            return null;
        }

        CandidateRuleBundle b = new CandidateRuleBundle();

        b.position = position;
        b.n1 = n1;
        b.n2 = n2;
        b.n3 = n3;
        b.k3Mask = mask;

        b.rules.add(buildProbEdgeRule(baseIssue, targetIssue, c, rankNo));
        b.rules.add(buildCauseChainRule(baseIssue, targetIssue, c, rankNo, chains));
        b.rules.add(buildTeacherNumberRule(baseIssue, targetIssue, c, rankNo, teacherMap));
        b.rules.add(buildCounterfactualRankRule(baseIssue, targetIssue, c, rankNo, rankList));
        b.rules.add(buildThirdGuardRule(baseIssue, targetIssue, c, rankNo, thirdGuardList));
        b.rules.add(buildPositionHealthRule(baseIssue, targetIssue, c, rankNo, positionHealthMap));
        b.rules.add(buildCompositeRiskRule(baseIssue, targetIssue, c, rankNo));
        b.rules.add(buildStreakProtectRule(baseIssue, targetIssue, c, rankNo, recentTrades));
        b.rules.add(
                buildSampleConfidenceRule(
                        baseIssue, targetIssue, c, rankNo, teacherMap, rankList, thirdGuardList));
        b.rules.add(buildK3BalanceRule(baseIssue, targetIssue, c, rankNo, teacherMap));
        b.rules.add(buildRecentDecayRule(baseIssue, targetIssue, c, rankNo));

        recalcBundle(b);

        return b;
    }

    private Models.RuleHealth buildProbEdgeRule(
            String baseIssue, String targetIssue, Object c, int rankNo) {
        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - gate);
        double rawP = getDouble(c, "rawP", finalP);
        double calibratedP = getDouble(c, "calibratedP", finalP);
        double calibrationError = Math.abs(finalP - calibratedP);

        double edgeScore = clamp01((edge + 0.030d) / 0.090d);
        double pScore = clamp01((finalP - 0.30d) / 0.18d);
        double calibrationScore = clamp01(1.0d - calibrationError * 6.0d);
        double rawAlign = clamp01(1.0d - Math.abs(rawP - finalP) * 4.0d);

        double health =
                clamp01(
                        edgeScore * 0.38d
                                + pScore * 0.28d
                                + calibrationScore * 0.18d
                                + rawAlign * 0.16d);
        double risk = 0.0d;

        if (gate > 0.0d && finalP < gate) {
            risk += 0.32d;
        }

        if (edge < 0.006d) {
            risk += 0.18d;
        }

        if (calibrationError > 0.030d) {
            risk += 0.16d;
        }

        if (finalP > 0.0d && finalP < 0.345d) {
            risk += 0.20d;
        }

        risk = clamp01(risk);

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_PROB_EDGE,
                health,
                risk,
                "概率边际健康｜finalP="
                        + percent(finalP)
                        + "｜gate="
                        + percent(gate)
                        + "｜edge="
                        + signed(edge)
                        + "｜校准差="
                        + percent(calibrationError)
                        + "｜说明=只审计概率边际，不直接放行BET");
    }

    private Models.RuleHealth buildCauseChainRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            List<Models.CauseChain> chains) {
        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);

        double candidateCause = getDouble(c, "causeScore", 0.0d);
        double chainScore = calcChainScore(position, n1, n2, n3, chains);
        double chainRisk = calcChainRisk(position, n1, n2, n3, chains);
        int chainSample = calcChainSample(position, n1, n2, n3, chains);

        double sampleConfidence = confidenceFromSample(chainSample, 30);
        double health =
                clamp01(candidateCause * 0.36d + chainScore * 0.42d + sampleConfidence * 0.22d);
        double risk = clamp01(chainRisk * 0.58d + (1.0d - sampleConfidence) * 0.16d);

        if (health < 0.36d && getDouble(c, "finalP", 0.0d) >= 0.39d) {
            risk = clamp01(risk + 0.18d);
        }

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_CAUSE_CHAIN,
                health,
                risk,
                "因链健康｜候选因链="
                        + percent(candidateCause)
                        + "｜号码因链="
                        + percent(chainScore)
                        + "｜因链风险="
                        + percent(chainRisk)
                        + "｜样本="
                        + chainSample
                        + "｜说明=因链只做证据健康，不直接创建BET");
    }

    private Models.RuleHealth buildTeacherNumberRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            Map<String, Models.NumberTeacherCell> teacherMap) {
        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);

        TeacherBundle tb = readTeacherBundle(position, n1, n2, n3, teacherMap);

        double health =
                clamp01(
                        tb.score * 0.48d
                                + tb.confidence * 0.25d
                                + tb.hitRate * 0.17d
                                + tb.sampleScore * 0.10d);
        double risk =
                clamp01(tb.risk * 0.48d + tb.conflict * 0.28d + (1.0d - tb.confidence) * 0.10d);

        if (tb.used <= 0) {
            risk = clamp01(risk + 0.16d);
        }

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_TEACHER_NUMBER,
                health,
                risk,
                "教师号码健康｜教师分="
                        + percent(tb.score)
                        + "｜教师风险="
                        + percent(tb.risk)
                        + "｜置信="
                        + percent(tb.confidence)
                        + "｜命中画像="
                        + percent(tb.hitRate)
                        + "｜样本="
                        + tb.sample
                        + "｜冲突="
                        + percent(tb.conflict)
                        + "｜说明=教师号码只提供画像字段，不直接救援BET");
    }

    private Models.RuleHealth buildCounterfactualRankRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            List<Models.RankCounterfactual> rankList) {
        int position = getInt(c, "position", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        Models.RankCounterfactual r = findRank(position, mask, rankList);

        double rankScore;
        double missedRisk;
        double conflictRisk;
        double falseRisk;
        double replaceRisk;

        if (r == null) {
            rankScore = getDouble(c, "counterfactualScore", 0.0d);
            missedRisk = getDouble(c, "missedStrongRisk", 0.0d);
            conflictRisk = getDouble(c, "nearConflictRisk", 0.0d);
            falseRisk = getDouble(c, "falseChampionRisk", 0.0d);
            replaceRisk = getDouble(c, "counterfactualRisk", 0.0d);
        } else {
            rankScore = getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));
            missedRisk = getDouble(r, "missedStrongRisk", 0.0d);
            conflictRisk = getDouble(r, "nearConflictRisk", 0.0d);
            falseRisk = getDouble(r, "falseChampionRisk", 0.0d);
            replaceRisk =
                    Math.max(
                            getDouble(r, "samePositionReplaceRisk", 0.0d),
                            getDouble(r, "crossPositionReplaceRisk", 0.0d));
        }

        double health =
                clamp01(
                        rankScore * 0.62d
                                + (1.0d - missedRisk) * 0.16d
                                + (1.0d - conflictRisk) * 0.12d
                                + (1.0d - replaceRisk) * 0.10d);
        double risk =
                clamp01(
                        missedRisk * 0.30d
                                + conflictRisk * 0.24d
                                + falseRisk * 0.24d
                                + replaceRisk * 0.22d);

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_COUNTERFACTUAL_RANK,
                health,
                risk,
                "反事实排名健康｜rankScore="
                        + percent(rankScore)
                        + "｜错过强候选="
                        + percent(missedRisk)
                        + "｜相近冲突="
                        + percent(conflictRisk)
                        + "｜伪强="
                        + percent(falseRisk)
                        + "｜替代风险="
                        + percent(replaceRisk)
                        + "｜说明=反事实只审计替代风险，不直接放行BET");
    }

    private Models.RuleHealth buildThirdGuardRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            List<Models.ThirdGuardStat> thirdGuardList) {
        int position = getInt(c, "position", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        Models.ThirdGuardStat g = findThirdGuard(position, mask, thirdGuardList);

        double guardScore;
        double guardRisk;
        int pass;
        int hard;

        if (g == null) {
            guardScore = getDouble(c, "thirdGuardScore", 0.0d);
            guardRisk = getDouble(c, "thirdGuardRisk", 0.0d);
            pass = getInt(c, "thirdGuardPass", 0);
            hard = getInt(c, "thirdGuardHardBlock", 0);
        } else {
            guardScore = getDouble(g, "thirdGuardScore", getDouble(g, "guardScore", 0.0d));
            guardRisk = getDouble(g, "thirdGuardRisk", getDouble(g, "guardRisk", 0.0d));
            pass = getInt(g, "thirdGuardPass", getInt(g, "pass", 0));
            hard = getInt(g, "hardBlock", 0);
        }

        double health =
                clamp01(
                        guardScore * 0.74d
                                + (pass == 1 ? 0.14d : 0.0d)
                                + (hard == 0 ? 0.12d : 0.0d));
        double risk = clamp01(guardRisk * 0.78d + (hard == 1 ? 0.22d : 0.0d));

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_THIRD_GUARD,
                health,
                risk,
                "第三防线健康｜guardScore="
                        + percent(guardScore)
                        + "｜guardRisk="
                        + percent(guardRisk)
                        + "｜pass="
                        + pass
                        + "｜hard="
                        + hard
                        + "｜说明=第三防线只审计风险，最终出手仍由DecisionGateEngine裁决");
    }

    private Models.RuleHealth buildPositionHealthRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            Map<Integer, Models.PositionHealth> positionHealthMap) {
        int position = getInt(c, "position", 0);
        Models.PositionHealth h =
                positionHealthMap == null ? null : positionHealthMap.get(Integer.valueOf(position));

        double healthScore =
                h == null
                        ? getDouble(
                                c, "positionHealthScore", getDouble(c, "positionCauseScore", 0.0d))
                        : getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));

        double riskScore =
                h == null
                        ? getDouble(c, "positionWrongRisk", 0.0d)
                        : getDouble(
                                h,
                                "riskScore",
                                getDouble(h, "wrongRisk", getDouble(h, "positionWrongRisk", 0.0d)));

        double driftRisk =
                h == null
                        ? getDouble(c, "positionDriftRisk", 0.0d)
                        : getDouble(h, "driftRisk", getDouble(h, "positionDriftRisk", 0.0d));

        double falseRisk =
                h == null
                        ? getDouble(c, "positionFalseStrongRisk", 0.0d)
                        : getDouble(
                                h,
                                "falseStrongRisk",
                                getDouble(h, "positionFalseStrongRisk", 0.0d));

        double health =
                clamp01(
                        healthScore * 0.70d
                                + (1.0d - driftRisk) * 0.16d
                                + (1.0d - falseRisk) * 0.14d);
        double risk = clamp01(riskScore * 0.46d + driftRisk * 0.30d + falseRisk * 0.24d);

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_POSITION_HEALTH,
                health,
                risk,
                "位置健康｜位置="
                        + position
                        + "｜健康="
                        + percent(healthScore)
                        + "｜错位风险="
                        + percent(riskScore)
                        + "｜漂移="
                        + percent(driftRisk)
                        + "｜位置伪强="
                        + percent(falseRisk)
                        + "｜说明=位置画像只参与健康审计，不直接放行BET");
    }

    private Models.RuleHealth buildCompositeRiskRule(
            String baseIssue, String targetIssue, Object c, int rankNo) {
        double risk = getDouble(c, "riskScore", 0.0d);
        double falseStrong = getDouble(c, "falseStrongRisk", 0.0d);
        double hotReverse = getDouble(c, "hotReverseRisk", getDouble(c, "hotExhaustionRisk", 0.0d));
        double coldFake = getDouble(c, "coldFakeRecoveryRisk", 0.0d);
        double teacherConflict = getDouble(c, "teacherConflictRisk", 0.0d);
        double rankConflict = getDouble(c, "rankConflictRisk", 0.0d);
        double weakSample = getDouble(c, "weakSampleAmplifyRisk", 0.0d);

        double compositeRisk =
                clamp01(
                        risk * 0.24d
                                + falseStrong * 0.18d
                                + hotReverse * 0.12d
                                + coldFake * 0.12d
                                + teacherConflict * 0.12d
                                + rankConflict * 0.12d
                                + weakSample * 0.10d);

        double health = clamp01(1.0d - compositeRisk);

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_COMPOSITE_RISK,
                health,
                compositeRisk,
                "复合风险健康｜综合风险="
                        + percent(compositeRisk)
                        + "｜原始风险="
                        + percent(risk)
                        + "｜伪强="
                        + percent(falseStrong)
                        + "｜热反="
                        + percent(hotReverse)
                        + "｜假冷补="
                        + percent(coldFake)
                        + "｜教师冲突="
                        + percent(teacherConflict)
                        + "｜排名冲突="
                        + percent(rankConflict)
                        + "｜弱样本="
                        + percent(weakSample)
                        + "｜说明=风险项只收紧健康分，不创建BET");
    }

    private Models.RuleHealth buildStreakProtectRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            List<Models.Trade> recentTrades) {
        int loseStreak = calcLoseStreak(recentTrades);
        double streakRisk;

        if (loseStreak <= 0) {
            streakRisk = 0.0d;
        } else if (loseStreak >= 5) {
            streakRisk = 0.76d;
        } else {
            streakRisk = clamp01(((double) loseStreak) / 7.0d);
        }

        double candidateSupport = candidateScore(c);
        double health = clamp01((1.0d - streakRisk) * 0.62d + candidateSupport * 0.38d);
        double risk = streakRisk;

        if (loseStreak >= 3 && candidateSupport < 0.55d) {
            risk = clamp01(risk + 0.12d);
        }

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_STREAK_PROTECT,
                health,
                risk,
                "连挂保护健康｜当前连挂="
                        + loseStreak
                        + "｜连挂风险="
                        + percent(streakRisk)
                        + "｜候选支撑="
                        + percent(candidateSupport)
                        + "｜说明=连挂保护只提供健康审计，是否HOLD由最终门决定");
    }

    private Models.RuleHealth buildSampleConfidenceRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            Map<String, Models.NumberTeacherCell> teacherMap,
            List<Models.RankCounterfactual> rankList,
            List<Models.ThirdGuardStat> thirdGuardList) {
        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        TeacherBundle tb = readTeacherBundle(position, n1, n2, n3, teacherMap);
        Models.RankCounterfactual rank = findRank(position, mask, rankList);
        Models.ThirdGuardStat guard = findThirdGuard(position, mask, thirdGuardList);

        double candidateConf =
                getDouble(c, "confidence", getDouble(c, "teacherNumberConfidence", 0.0d));
        double teacherConf = tb.confidence;
        double rankConf =
                rank == null
                        ? getDouble(c, "counterfactualScore", 0.0d)
                        : getDouble(
                                rank, "rankScore", getDouble(rank, "counterfactualScore", 0.0d));
        double guardConf =
                guard == null
                        ? getDouble(c, "thirdGuardScore", 0.0d)
                        : getDouble(guard, "thirdGuardScore", getDouble(guard, "guardScore", 0.0d));
        double sampleScore = tb.sampleScore;

        double health =
                clamp01(
                        candidateConf * 0.22d
                                + teacherConf * 0.24d
                                + rankConf * 0.18d
                                + guardConf * 0.18d
                                + sampleScore * 0.18d);
        double risk = clamp01((1.0d - health) * 0.56d);

        if (tb.sample < 30 && getDouble(c, "finalP", 0.0d) >= 0.39d) {
            risk = clamp01(risk + 0.12d);
        }

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_SAMPLE_CONFIDENCE,
                health,
                risk,
                "样本置信健康｜候选置信="
                        + percent(candidateConf)
                        + "｜教师置信="
                        + percent(teacherConf)
                        + "｜rank置信="
                        + percent(rankConf)
                        + "｜guard置信="
                        + percent(guardConf)
                        + "｜教师样本="
                        + tb.sample
                        + "｜说明=样本置信不足时只加风险，不直接否定或救援BET");
    }

    private Models.RuleHealth buildK3BalanceRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            Map<String, Models.NumberTeacherCell> teacherMap) {
        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);

        int odd = 0;
        int high = 0;
        int nearPair = 0;

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
            nearPair++;
        }

        if (Math.abs(n1 - n3) <= 1) {
            nearPair++;
        }

        if (Math.abs(n2 - n3) <= 1) {
            nearPair++;
        }

        TeacherBundle tb = readTeacherBundle(position, n1, n2, n3, teacherMap);

        double structure = 0.0d;

        if (odd == 1 || odd == 2) {
            structure += 0.28d;
        } else {
            structure += 0.12d;
        }

        if (high == 1 || high == 2) {
            structure += 0.26d;
        } else {
            structure += 0.12d;
        }

        if (nearPair <= 1) {
            structure += 0.18d;
        } else {
            structure += 0.08d;
        }

        structure += tb.score * 0.18d;
        structure += (1.0d - tb.risk) * 0.10d;

        double health = clamp01(structure);
        double risk = 0.0d;

        if (odd == 0 || odd == 3) {
            risk += 0.10d;
        }

        if (high == 0 || high == 3) {
            risk += 0.10d;
        }

        if (nearPair >= 2) {
            risk += 0.12d;
        }

        risk += tb.risk * 0.22d;
        risk = clamp01(risk);

        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_K3_BALANCE,
                health,
                risk,
                "K3结构健康｜奇数个数="
                        + odd
                        + "｜高位个数="
                        + high
                        + "｜近邻对="
                        + nearPair
                        + "｜教师结构分="
                        + percent(tb.score)
                        + "｜结构风险="
                        + percent(risk)
                        + "｜说明=结构健康只辅助排序与解释，不改变固定K=3玩法");
    }

    private Models.RuleHealth buildRecentDecayRule(
            String baseIssue, String targetIssue, Object c, int rankNo) {
        double v20Recent = getDouble(c, "v20RecentValidity", getDouble(c, "recentValidity", 0.0d));
        double teacherRecent =
                getDouble(
                        c,
                        "teacherRecentValidity",
                        getDouble(c, "teacherMatrixRecentValidity", 0.0d));
        double sampleReliability =
                getDouble(c, "sampleReliability", getDouble(c, "teacherSampleReliability", 0.0d));
        double ruleOldHealth = getDouble(c, "ruleHealthScore", 0.0d);
        double hotDecay = getDouble(c, "hotExhaustionRisk", getDouble(c, "hotReverseRisk", 0.0d));
        double falseStrong = getDouble(c, "falseStrongRisk", 0.0d);
        double weakSample = getDouble(c, "weakSampleAmplifyRisk", 0.0d);
        double thirdDrag = getDouble(c, "thirdDragRisk", getDouble(c, "thirdDragScore", 0.0d));
        double recentMix =
                clamp01(
                        v20Recent * 0.34d
                                + teacherRecent * 0.26d
                                + sampleReliability * 0.24d
                                + ruleOldHealth * 0.16d);
        double decayRisk =
                clamp01(
                        (1.0d - recentMix) * 0.42d
                                + hotDecay * 0.18d
                                + falseStrong * 0.16d
                                + weakSample * 0.14d
                                + thirdDrag * 0.10d);
        double health = clamp01(recentMix * 0.72d + (1.0d - decayRisk) * 0.28d);
        if (getDouble(c, "finalP", 0.0d) >= 0.39d && recentMix < 0.34d) {
            decayRisk = clamp01(decayRisk + 0.12d);
        }
        return newRule(
                baseIssue,
                targetIssue,
                c,
                rankNo,
                RULE_RECENT_DECAY,
                health,
                decayRisk,
                "规则近期衰减健康｜V20近期="
                        + percent(v20Recent)
                        + "｜教师近期="
                        + percent(teacherRecent)
                        + "｜样本可信="
                        + percent(sampleReliability)
                        + "｜热区衰减="
                        + percent(hotDecay)
                        + "｜伪强="
                        + percent(falseStrong)
                        + "｜弱样本="
                        + percent(weakSample)
                        + "｜第三号拖拽="
                        + percent(thirdDrag)
                        + "｜说明=近期衰减只收紧健康与诊断，不救援BET");
    }

    private Models.RuleHealth newRule(
            String baseIssue,
            String targetIssue,
            Object c,
            int rankNo,
            String ruleCode,
            double health,
            double risk,
            String summary) {
        Models.RuleHealth r = new Models.RuleHealth();

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        health = clamp01(health);
        risk = clamp01(risk);

        int pass = health >= PASS_HEALTH_MIN && risk <= PASS_RISK_MAX ? 1 : 0;
        int hard = risk >= HARD_BAD_RISK ? 1 : 0;
        String status = statusOf(health, risk, pass, hard);
        double decayRisk = calcRuleDecayRisk(c, ruleCode, health, risk);
        String rejectCode = buildRuleRejectCode(ruleCode, health, risk, decayRisk, hard);
        String actionAdvice =
                buildRuleActionAdvice(ruleCode, health, risk, decayRisk, pass, hard, rejectCode);

        putString(r, "ruleKey", buildRuleKey(targetIssue, position, mask, ruleCode));
        putString(r, "healthKey", buildRuleKey(targetIssue, position, mask, ruleCode));
        putString(r, "baseIssue", safe(baseIssue));
        putString(r, "targetIssue", safe(targetIssue));
        putLong(r, "createdAt", System.currentTimeMillis());

        putString(r, "ruleCode", ruleCode);
        putString(r, "ruleName", ruleCode);
        putString(r, "ruleType", ruleCode);

        putInt(r, "rankNo", rankNo);
        putInt(r, "rank", rankNo);
        putInt(r, "position", position);
        putInt(r, "n1", n1);
        putInt(r, "n2", n2);
        putInt(r, "n3", n3);
        putInt(r, "k3Mask", mask);
        putInt(r, "k3_mask", mask);

        putDouble(r, "healthScore", health);
        putDouble(r, "ruleHealthScore", health);
        putDouble(r, "riskScore", risk);
        putDouble(r, "ruleRiskScore", risk);
        putInt(r, "pass", pass);
        putInt(r, "rulePass", pass);
        putInt(r, "hardBlock", hard);

        putString(r, "status", status);
        putString(r, "level", status);
        putDouble(r, "ruleDecayRisk", decayRisk);
        putDouble(r, "decayRisk", decayRisk);
        putDouble(r, "antiEvidenceScore", risk);
        putDouble(r, "counterEvidenceScore", risk);
        putDouble(r, "netEvidenceScore", clamp01(health - risk * 0.55d));
        putString(r, "ruleRejectCode", rejectCode);
        putString(r, "rejectCode", rejectCode);
        putString(r, "finalDecisionRejectCode", rejectCode);
        putString(r, "ruleActionAdvice", actionAdvice);
        putString(r, "actionAdvice", actionAdvice);
        putString(r, "fieldChainVersion", RULE_HEALTH_CLOSED_LOOP_VERSION);
        putString(r, "ruleHealthVersion", RULE_HEALTH_VERSION);
        putString(
                r,
                "summaryText",
                summary
                        + "｜ruleDecay="
                        + percent(decayRisk)
                        + "｜rejectCode="
                        + rejectCode
                        + "｜action="
                        + actionAdvice);
        putString(
                r,
                "reasonText",
                summary
                        + "｜ruleDecay="
                        + percent(decayRisk)
                        + "｜rejectCode="
                        + rejectCode
                        + "｜action="
                        + actionAdvice);

        return r;
    }

    private CandidateRuleBundle summarizeRulesForCandidate(
            int position, int mask, ArrayList<Models.RuleHealth> rules) {
        if (!isValidPosition(position) || rules == null || rules.size() == 0) {
            return null;
        }

        CandidateRuleBundle b = new CandidateRuleBundle();
        b.position = position;
        b.k3Mask = mask;

        for (int i = 0; i < rules.size(); i++) {
            Models.RuleHealth r = rules.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", 0);
            int m = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            if (p == position && m == mask) {
                b.rules.add(r);
            }
        }

        if (b.rules.size() == 0) {
            return null;
        }

        recalcBundle(b);

        return b;
    }

    private void recalcBundle(CandidateRuleBundle b) {
        if (b == null || b.rules == null || b.rules.size() == 0) {
            return;
        }

        double healthSum = 0.0d;
        double riskSum = 0.0d;
        int passCount = 0;
        int hardCount = 0;
        int count = 0;

        double minHealth = 1.0d;
        double maxRisk = 0.0d;
        double maxDecay = 0.0d;
        String firstReject = "";
        String firstAction = "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < b.rules.size(); i++) {
            Models.RuleHealth r = b.rules.get(i);

            if (r == null) {
                continue;
            }

            double h = getDouble(r, "ruleHealthScore", getDouble(r, "healthScore", 0.0d));
            double risk = getDouble(r, "ruleRiskScore", getDouble(r, "riskScore", 0.0d));
            int pass = getInt(r, "rulePass", getInt(r, "pass", 0));
            int hard = getInt(r, "hardBlock", 0);

            healthSum += h;
            riskSum += risk;
            count++;

            if (pass == 1) {
                passCount++;
            }

            if (hard == 1) {
                hardCount++;
            }

            if (h < minHealth) {
                minHealth = h;
            }

            if (risk > maxRisk) {
                maxRisk = risk;
            }
            double decay = getDouble(r, "ruleDecayRisk", getDouble(r, "decayRisk", 0.0d));
            if (decay > maxDecay) {
                maxDecay = decay;
            }
            String rc = getString(r, "ruleRejectCode", getString(r, "rejectCode", ""));
            if (firstReject.length() == 0 && rc.length() > 0 && !"RULE_OK".equals(rc)) {
                firstReject = rc;
            }
            String act = getString(r, "ruleActionAdvice", getString(r, "actionAdvice", ""));
            if (firstAction.length() == 0 && act.length() > 0) {
                firstAction = act;
            }

            String code = getString(r, "ruleCode", getString(r, "ruleType", ""));
            String status = getString(r, "status", "");

            if (sb.length() > 0) {
                sb.append("；");
            }

            sb.append(code);
            sb.append("=");
            sb.append(status);
        }

        if (count <= 0) {
            return;
        }

        b.ruleCount = count;
        b.passCount = passCount;
        b.hardCount = hardCount;
        b.avgHealth = clamp01(healthSum / count);
        b.avgRisk = clamp01(riskSum / count);
        b.minHealth = minHealth;
        b.maxRisk = maxRisk;
        b.ruleDecayRisk = maxDecay;
        b.ruleRejectCode = firstReject.length() == 0 ? "RULE_OK" : firstReject;
        b.ruleActionAdvice = firstAction.length() == 0 ? "RULE_PASS" : firstAction;

        double passRate = ((double) passCount) / ((double) count);

        b.healthScore = clamp01(b.avgHealth * 0.48d + minHealth * 0.22d + passRate * 0.30d);
        b.riskScore =
                clamp01(
                        b.avgRisk * 0.42d
                                + maxRisk * 0.30d
                                + maxDecay * 0.12d
                                + (hardCount > 0 ? 0.20d : 0.0d));
        b.pass =
                b.healthScore >= PASS_HEALTH_MIN
                                && b.riskScore <= PASS_RISK_MAX
                                && maxDecay < 0.72d
                                && hardCount == 0
                        ? 1
                        : 0;
        b.status = statusOf(b.healthScore, b.riskScore, b.pass, hardCount > 0 ? 1 : 0);
        b.summary =
                "V20规则健康矩阵｜规则数="
                        + count
                        + "｜通过="
                        + passCount
                        + "｜硬风险="
                        + hardCount
                        + "｜健康="
                        + percent(b.healthScore)
                        + "｜风险="
                        + percent(b.riskScore)
                        + "｜衰减="
                        + percent(b.ruleDecayRisk)
                        + "｜拒绝码="
                        + b.ruleRejectCode
                        + "｜动作="
                        + b.ruleActionAdvice
                        + "｜状态="
                        + b.status
                        + "｜细项="
                        + sb.toString()
                        + "｜说明=规则健康只做审计与排序，最终BET仍由DecisionGateEngine裁决";
    }

    private CandidateRuleBundle buildLightweightRuleBundle(Object c, int rankNo, String freshness) {
        if (c == null) return null;
        CandidateRuleBundle b = new CandidateRuleBundle();
        double probability = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "riskAdjustedGate", getDouble(c, "dynamicGate", 0.0d));
        double edge = getDouble(c, "rawEdge", getDouble(c, "edge", probability - gate));
        double thirdRisk =
                Math.max(getDouble(c, "thirdGuardRisk", 0.0d), getDouble(c, "thirdConflict", 0.0d));
        double counterRisk =
                Math.max(
                        getDouble(c, "counterRisk", 0.0d),
                        getDouble(c, "counterfactualWhyNotRisk", 0.0d));
        double adaptiveRisk =
                Math.max(
                        getDouble(c, "adaptiveRiskScore", 0.0d),
                        getDouble(c, "adaptiveFreezeScore", 0.0d));
        double evidence =
                Math.max(
                        getDouble(c, "netEvidenceScore", 0.0d),
                        getDouble(c, "evidenceScore", 0.0d));
        b.ruleCount = 4;
        b.passCount = 0;
        b.hardCount =
                (getInt(c, "thirdHardRisk", 0) == 1
                                || getInt(c, "counterfactualHardRisk", 0) == 1
                                || containsHardStatus(
                                        getString(
                                                c,
                                                "adaptiveStatus",
                                                getString(c, "adaptiveJudgeStatus", ""))))
                        ? 1
                        : 0;
        b.avgHealth =
                clamp01(
                        probability * 0.42d
                                + Math.max(0.0d, edge + 0.02d) * 4.0d * 0.18d
                                + evidence * 0.24d
                                + (1.0d - Math.max(thirdRisk, counterRisk)) * 0.16d);
        b.avgRisk = clamp01(Math.max(Math.max(thirdRisk, counterRisk), adaptiveRisk));
        b.minHealth = b.avgHealth;
        b.maxRisk = b.avgRisk;
        b.ruleDecayRisk =
                "STALE".equals(freshness) || "HARD_STALE".equals(freshness) ? 0.72d : 0.30d;
        b.healthScore = b.avgHealth;
        b.riskScore = b.avgRisk;
        b.pass =
                b.hardCount == 0 && b.healthScore >= PASS_HEALTH_MIN && b.riskScore <= PASS_RISK_MAX
                        ? 1
                        : 0;
        b.status =
                normalizeRuleStatusV2(
                        statusOf(b.healthScore, b.riskScore, b.pass, b.hardCount > 0 ? 1 : 0),
                        b.healthScore,
                        b.riskScore,
                        b.hardCount);
        if (b.pass == 1) b.passCount = b.ruleCount;
        b.ruleRejectCode =
                b.hardCount > 0
                        ? "RULE_HEALTH_HARD_RISK"
                        : (b.pass == 1 ? "RULE_OK" : "RULE_WATCH_NEUTRAL");
        b.ruleActionAdvice =
                b.hardCount > 0 ? "HOLD_OR_SHADOW" : (b.pass == 1 ? "RULE_PASS" : "WATCH_NEUTRAL");
        b.summary =
                "规则健康轻量审计｜rank="
                        + rankNo
                        + "｜健康="
                        + percent(b.healthScore)
                        + "｜风险="
                        + percent(b.riskScore)
                        + "｜状态="
                        + b.status
                        + "｜fresh="
                        + freshness
                        + "｜不创建BET";
        return b;
    }

    private void normalizeBundleForV2(CandidateRuleBundle b, String freshness) {
        if (b == null) return;
        b.status = normalizeRuleStatusV2(b.status, b.healthScore, b.riskScore, b.hardCount);
        if (("STALE".equals(freshness) || "HARD_STALE".equals(freshness))
                && b.hardCount == 0
                && !STATUS_HARD_RISK.equals(b.status)) {
            b.status = STATUS_WATCH_STALE;
            b.pass = 0;
            b.ruleActionAdvice = "WATCH_STALE";
        }
    }

    private String normalizeRuleStatusV2(String status, double health, double risk, int hardCount) {
        String x = status == null ? "" : status.toUpperCase(Locale.US);
        if (hardCount > 0 || x.indexOf("HARD") >= 0 || risk >= HARD_BAD_RISK)
            return STATUS_HARD_RISK;
        if (x.indexOf("STALE") >= 0) return STATUS_WATCH_STALE;
        if (risk >= 0.68d || x.indexOf("RISK") >= 0) return STATUS_SOFT_RISK;
        if (health >= PASS_HEALTH_MIN
                && risk <= PASS_RISK_MAX
                && ("PASS".equals(x) || "HEALTHY".equals(x))) return STATUS_PASS;
        return STATUS_WATCH;
    }

    private void applyDeferredRuleHealth(Object c, int rankNo, String freshness, String reason) {
        if (c == null) return;
        putInt(c, "ruleHealthPass", 0);
        putInt(c, "ruleHealthHardCount", 0);
        putString(c, "ruleHealthStatus", STATUS_WATCH_STALE);
        putString(c, "ruleHealthFreshness", freshness);
        putString(c, "ruleHealthVersion", LINK_V2_VERSION);
        putString(c, "ruleHealthActionAdvice", "BACKGROUND_RECHECK");
        putString(
                c,
                "ruleHealthSummary",
                "规则健康延后｜rank="
                        + rankNo
                        + "｜reason="
                        + safe(reason)
                        + "｜状态="
                        + STATUS_WATCH_STALE
                        + "｜未计算不等于PASS，也不直接判硬风险");
    }

    private String resolveRuleSnapshotFreshness(DbStore store, String targetIssue) {
        if (store == null) return "NO_SAMPLE";
        try {
            Models.LayerSnapshot warm = store.loadWarmSnapshot();
            Models.LayerSnapshot cold = store.loadColdSnapshot();
            if (warm != null && warm.isSafeForTarget(targetIssue)) {
                long now = System.currentTimeMillis();
                if (warm.expiresAt > 0L && now > warm.expiresAt) return "STALE";
                return warm.freshnessStatus == null || warm.freshnessStatus.length() == 0
                        ? "WARM_READY"
                        : warm.freshnessStatus;
            }
            if (cold != null && cold.isSafeForTarget(targetIssue)) {
                return "COLD_FALLBACK";
            }
        } catch (Throwable e) {
        }
        return "NO_SAMPLE";
    }

    private boolean containsHardStatus(String status) {
        if (status == null) return false;
        String x = status.toUpperCase(Locale.US);
        return x.indexOf("FREEZE") >= 0 || x.indexOf("REJECT") >= 0 || x.indexOf("HARD_RISK") >= 0;
    }

    private void applyBundleToCandidate(Object c, CandidateRuleBundle b) {
        if (c == null || b == null) {
            return;
        }

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;
        if (b.pass == 1) {
            newCause = clamp01(oldCause * 0.90d + b.healthScore * 0.10d);
        } else {
            newCause = clamp01(oldCause * 0.94d - b.riskScore * 0.050d);
        }

        double newRisk = clamp01(Math.max(oldRisk, b.riskScore));

        putDouble(c, "ruleHealthScore", b.healthScore);
        putDouble(c, "ruleHealthRisk", b.riskScore);
        putInt(c, "ruleHealthPass", b.pass);
        putInt(c, "ruleHealthRuleCount", b.ruleCount);
        putInt(c, "ruleHealthPassCount", b.passCount);
        putInt(c, "ruleHealthHardCount", b.hardCount);
        putString(
                c,
                "ruleHealthStatus",
                normalizeRuleStatusV2(b.status, b.healthScore, b.riskScore, b.hardCount));
        putString(c, "ruleHealthSummary", b.summary);
        putString(c, "ruleHealthVersion", LINK_V2_VERSION);
        putString(c, "ruleHealthFreshness", getString(c, "ruleHealthFreshness", "HOT_REALTIME"));
        putDouble(c, "ruleDecayRisk", b.ruleDecayRisk);
        putString(c, "ruleRejectCode", b.ruleRejectCode);
        putString(c, "ruleActionAdvice", b.ruleActionAdvice);
        putString(c, "ruleHealthActionAdvice", b.ruleActionAdvice);
        putDouble(
                c,
                "antiEvidenceScore",
                Math.max(getDouble(c, "antiEvidenceScore", 0.0d), b.riskScore));
        putDouble(
                c,
                "counterEvidenceScore",
                Math.max(getDouble(c, "counterEvidenceScore", 0.0d), b.riskScore));
        putDouble(
                c,
                "netEvidenceScore",
                clamp01(
                        getDouble(c, "netEvidenceScore", 0.0d)
                                + b.healthScore * 0.10d
                                - b.riskScore * 0.08d));
        putString(c, "fieldChainVersion", LINK_V2_VERSION);
        if (b.pass != 1 || b.hardCount > 0) {
            putString(c, "rejectCode", b.ruleRejectCode);
            putString(c, "finalDecisionRejectCode", b.ruleRejectCode);
        }

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendCandidateReason(c, b.summary);
    }

    private void applyRulesToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.RuleHealth> rules) {
        if (chains == null || rules == null || chains.size() == 0 || rules.size() == 0) {
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

            RuleSummary rs = summarizeRulesForChain(position, number, rules);

            if (rs == null || rs.count <= 0) {
                continue;
            }

            double oldRisk = getDouble(chain, "riskScore", 0.0d);
            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));

            double newFinal;
            if (rs.pass == 1) {
                newFinal = clamp01(oldFinal + rs.health * 0.026d);
            } else {
                newFinal = clamp01(oldFinal - Math.max(0.018d, rs.risk * 0.045d));
            }

            double newRisk = clamp01(Math.max(oldRisk, rs.risk));

            putDouble(chain, "ruleHealthScore", rs.health);
            putDouble(chain, "ruleHealthRisk", rs.risk);
            putInt(chain, "ruleHealthPass", rs.pass);
            putString(chain, "ruleHealthSummary", rs.summary);
            putDouble(chain, "riskScore", newRisk);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);

            String old = getString(chain, "summaryText", "");
            putString(chain, "summaryText", old + "｜" + rs.summary);
        }
    }

    private RuleSummary summarizeRulesForChain(
            int position, int number, ArrayList<Models.RuleHealth> rules) {
        RuleSummary rs = new RuleSummary();

        for (int i = 0; i < rules.size(); i++) {
            Models.RuleHealth r = rules.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", 0);

            if (p != position) {
                continue;
            }

            int n1 = getInt(r, "n1", 0);
            int n2 = getInt(r, "n2", 0);
            int n3 = getInt(r, "n3", 0);

            if (number != n1 && number != n2 && number != n3) {
                continue;
            }

            double h = getDouble(r, "ruleHealthScore", getDouble(r, "healthScore", 0.0d));
            double risk = getDouble(r, "ruleRiskScore", getDouble(r, "riskScore", 0.0d));

            rs.health += h;
            rs.risk += risk;
            rs.count++;

            if (getInt(r, "rulePass", getInt(r, "pass", 0)) == 1) {
                rs.passCount++;
            }

            if (getInt(r, "hardBlock", 0) == 1) {
                rs.hardCount++;
            }
        }

        if (rs.count <= 0) {
            return null;
        }

        rs.health = clamp01(rs.health / rs.count);
        rs.risk = clamp01(rs.risk / rs.count);
        rs.pass =
                rs.health >= PASS_HEALTH_MIN && rs.risk <= PASS_RISK_MAX && rs.hardCount <= 0
                        ? 1
                        : 0;
        rs.summary =
                "规则健康回写因链｜位置="
                        + position
                        + "｜号码="
                        + number
                        + "｜健康="
                        + percent(rs.health)
                        + "｜风险="
                        + percent(rs.risk)
                        + "｜通过="
                        + rs.passCount
                        + "/"
                        + rs.count
                        + "｜hard="
                        + rs.hardCount
                        + "｜说明=只回写因链健康，不直接BET";

        return rs;
    }

    private void rerankByRuleHealth(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = finalCandidateScore(a);
                        double sb = finalCandidateScore(b);

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

    private static double finalCandidateScore(Object c) {
        double base = candidateScore(c);
        double health = getDouble(c, "ruleHealthScore", 0.0d);
        double risk = getDouble(c, "ruleHealthRisk", 0.0d);
        int pass = getInt(c, "ruleHealthPass", 0);
        int hard = getInt(c, "ruleHealthHardCount", 0);

        double score = base * 0.82d + health * 0.18d - risk * 0.08d;

        if (pass == 1) {
            score += 0.014d;
        }

        if (hard > 0) {
            score -= 0.065d;
        }

        return clamp01(score);
    }

    private static double candidateScore(Object c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - gate);
        double rawP = getDouble(c, "rawP", finalP);
        double calibratedP = getDouble(c, "calibratedP", finalP);

        double cause = getDouble(c, "causeScore", 0.0d);
        double chain = getDouble(c, "chainScore", getDouble(c, "finalCauseScore", 0.0d));
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double teacherRisk =
                getDouble(c, "teacherNumberRisk", getDouble(c, "teacherComboRisk", 0.0d));
        double position =
                getDouble(c, "positionCauseScore", getDouble(c, "positionHealthScore", 0.0d));
        double positionRisk =
                getDouble(c, "positionWrongRisk", getDouble(c, "positionRiskScore", 0.0d));
        double counter =
                getDouble(c, "counterfactualScore", getDouble(c, "counterfactualRankScore", 0.0d));
        double thirdGuard = getDouble(c, "thirdGuardScore", 0.0d);
        double shadow = getDouble(c, "shadowHealthScore", 0.0d);
        double seed = getDouble(c, "seedExpandScore", getDouble(c, "seedScore", 0.0d));
        double confidence =
                getDouble(c, "confidence", getDouble(c, "teacherNumberConfidence", 0.0d));
        double risk = getDouble(c, "riskScore", 0.0d);

        double edgeScore = clamp01((edge + 0.030d) / 0.090d);
        double pScore = clamp01((finalP - 0.30d) / 0.18d);
        double rawAlign = clamp01(1.0d - Math.abs(rawP - finalP) * 4.0d);
        double calAlign = clamp01(1.0d - Math.abs(calibratedP - finalP) * 5.0d);
        double gateScore = gate <= 0.0d ? 0.50d : (finalP >= gate ? 0.68d : 0.30d);

        double score = 0.0d;

        score += pScore * 0.16d;
        score += edgeScore * 0.12d;
        score += gateScore * 0.06d;
        score += rawAlign * 0.04d;
        score += calAlign * 0.04d;
        score += cause * 0.16d;
        score += chain * 0.10d;
        score += teacher * 0.09d;
        score += position * 0.07d;
        score += counter * 0.08d;
        score += thirdGuard * 0.06d;
        score += shadow * 0.05d;
        score += seed * 0.04d;
        score += confidence * 0.05d;

        score -= risk * 0.12d;
        score -= teacherRisk * 0.05d;
        score -= positionRisk * 0.05d;

        return clamp01(score);
    }

    private double calcRuleDecayRisk(Object c, String ruleCode, double health, double risk) {
        double recent = getDouble(c, "recentValidity", getDouble(c, "v20RecentValidity", 0.0d));
        double teacherRecent =
                getDouble(
                        c,
                        "teacherRecentValidity",
                        getDouble(c, "teacherMatrixRecentValidity", 0.0d));
        double sampleReliability =
                getDouble(c, "sampleReliability", getDouble(c, "teacherSampleReliability", 0.0d));
        double falseStrong = getDouble(c, "falseStrongRisk", 0.0d);
        double hotDecay = getDouble(c, "hotExhaustionRisk", getDouble(c, "hotReverseRisk", 0.0d));
        double recentMix =
                clamp01(
                        recent * 0.38d
                                + teacherRecent * 0.24d
                                + sampleReliability * 0.22d
                                + health * 0.16d);
        double decay =
                clamp01(
                        (1.0d - recentMix) * 0.45d
                                + risk * 0.22d
                                + falseStrong * 0.18d
                                + hotDecay * 0.15d);
        if (RULE_RECENT_DECAY.equals(ruleCode)) {
            decay = Math.max(decay, risk);
        }
        return clamp01(decay);
    }

    private String buildRuleRejectCode(
            String ruleCode, double health, double risk, double decay, int hard) {
        if (hard == 1 || risk >= HARD_BAD_RISK) {
            return "RULE_HARD_RISK_" + safe(ruleCode);
        }
        if (decay >= 0.72d) {
            return "RULE_DECAY_RISK_" + safe(ruleCode);
        }
        if (risk >= 0.70d) {
            return "RULE_RECENT_INVALID_" + safe(ruleCode);
        }
        if (health < 0.36d) {
            return "RULE_HEALTH_WEAK_" + safe(ruleCode);
        }
        return "RULE_OK";
    }

    private String buildRuleActionAdvice(
            String ruleCode,
            double health,
            double risk,
            double decay,
            int pass,
            int hard,
            String rejectCode) {
        if (hard == 1 || rejectCode.indexOf("HARD_RISK") >= 0) {
            return "RULE_FREEZE_OR_SHADOW：" + safe(ruleCode) + "硬风险，只能交最终门收紧";
        }
        if (decay >= 0.72d || rejectCode.indexOf("DECAY") >= 0) {
            return "RULE_WATCH_DECAY：" + safe(ruleCode) + "近期衰减，禁止规则救援";
        }
        if (risk >= 0.70d) {
            return "RULE_WATCH_RISK：" + safe(ruleCode) + "风险偏高，建议SHADOW/HOLD";
        }
        if (pass == 1) {
            return "RULE_PASS：" + safe(ruleCode) + "健康可送DecisionGate复核";
        }
        return "RULE_WATCH：" + safe(ruleCode) + "健康不足，只观察不放行";
    }

    private ArrayList<Models.ComboCandidate> castComboCandidates(List raw) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();
        if (raw == null) {
            return out;
        }
        for (int i = 0; i < raw.size(); i++) {
            Object o = raw.get(i);
            if (o instanceof Models.ComboCandidate) {
                out.add((Models.ComboCandidate) o);
            }
        }
        return out;
    }

    private void rerankComboByRuleHealth(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }
        Collections.sort(
                candidates,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = finalCandidateScore(a);
                        double sb = finalCandidateScore(b);
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        int pa = getInt(a, "position", 0);
                        int pb = getInt(b, "position", 0);
                        if (pa != pb) return pa - pb;
                        return getInt(a, "k3Mask", getInt(a, "k3_mask", 0))
                                - getInt(b, "k3Mask", getInt(b, "k3_mask", 0));
                    }
                });
        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }
    }

    private void writeRealtimeRuleHealthState(
            DbStore store, String source, int total, int written, int pass, int hard) {
        if (store == null) {
            return;
        }
        try {
            store.setState("rule_health_realtime_version", LINK_V2_VERSION);
            store.setState("rule_health_realtime_source", safe(source));
            store.setState("rule_health_realtime_total", String.valueOf(total));
            store.setState("rule_health_realtime_written", String.valueOf(written));
            store.setState("rule_health_realtime_pass", String.valueOf(pass));
            store.setState("rule_health_realtime_hard", String.valueOf(hard));
            store.setState(
                    "rule_health_realtime_summary",
                    "RuleHealth实时完成｜候选="
                            + total
                            + "｜写入="
                            + written
                            + "｜通过="
                            + pass
                            + "｜硬风险="
                            + hard
                            + "｜只审计规则健康，不创建BET");
        } catch (Throwable e) {
        }
    }

    private double calcChainScore(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null) {
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
        if (chains == null) {
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

    private int calcChainSample(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        if (chains == null) {
            return 0;
        }

        int used = 0;

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int n = getInt(c, "number", 0);

            if (p == position && (n == n1 || n == n2 || n == n3)) {
                used++;
            }
        }

        return used;
    }

    private TeacherBundle readTeacherBundle(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        TeacherBundle b = new TeacherBundle();

        if (map == null) {
            return b;
        }

        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            double score = getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            double risk = getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            double confidence = getDouble(c, "confidence", getDouble(c, "sampleConfidence", 0.0d));
            double hitRate = getDouble(c, "hitRate", getDouble(c, "recentHitRate", 0.0d));
            int sample = getInt(c, "sampleCount", getInt(c, "sample", 0));
            double conflict =
                    getDouble(c, "conflictRisk", getDouble(c, "teacherConflictRisk", 0.0d));

            b.score += score;
            b.risk += risk;
            b.confidence += confidence;
            b.hitRate += hitRate;
            b.sample += sample;
            b.conflict += conflict;
            b.used++;
        }

        if (b.used > 0) {
            b.score = clamp01(b.score / b.used);
            b.risk = clamp01(b.risk / b.used);
            b.confidence = clamp01(b.confidence / b.used);
            b.hitRate = clamp01(b.hitRate / b.used);
            b.conflict = clamp01(b.conflict / b.used);
        }

        b.sampleScore = confidenceFromSample(b.sample, 90);

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

    private Models.RankCounterfactual findRank(
            int position, int mask, List<Models.RankCounterfactual> list) {
        if (list == null) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.RankCounterfactual r = list.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", getInt(r, "chosenPosition", 0));
            int m = getInt(r, "k3Mask", getInt(r, "chosenK3Mask", 0));

            if (p == position && m == mask) {
                return r;
            }
        }

        return null;
    }

    private Models.ThirdGuardStat findThirdGuard(
            int position, int mask, List<Models.ThirdGuardStat> list) {
        if (list == null) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ThirdGuardStat g = list.get(i);

            if (g == null) {
                continue;
            }

            int p = getInt(g, "position", 0);
            int m = getInt(g, "k3Mask", getInt(g, "k3_mask", 0));

            if (p == position && m == mask) {
                return g;
            }
        }

        return null;
    }

    private int calcLoseStreak(List<Models.Trade> trades) {
        if (trades == null || trades.size() == 0) {
            return 0;
        }

        int streak = 0;
        int checked = 0;

        for (int i = trades.size() - 1; i >= 0; i--) {
            Models.Trade t = trades.get(i);

            if (t == null) {
                continue;
            }

            String r = getString(t, "result", "");

            checked++;

            if ("MISS".equals(r) || "LOSE".equals(r) || "未中".equals(r)) {
                streak++;
            } else if ("WIN".equals(r) || "HIT".equals(r) || "命中".equals(r)) {
                break;
            }

            if (checked >= 12) {
                break;
            }
        }

        return streak;
    }

    private ArrayList<Models.CandidateK3> normalizeCandidates(List<Models.CandidateK3> candidates) {
        ArrayList<Models.CandidateK3> list = new ArrayList<Models.CandidateK3>();

        if (candidates == null) {
            return list;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int position = getInt(c, "position", 0);
            int n1 = getInt(c, "n1", 0);
            int n2 = getInt(c, "n2", 0);
            int n3 = getInt(c, "n3", 0);

            if (!isValidPosition(position)
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

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c != null) {
                putInt(c, "rankNo", i + 1);
                putInt(c, "rank", i + 1);
            }
        }

        return list;
    }

    private String statusOf(double health, double risk, int pass, int hard) {
        if (hard == 1) {
            return "HARD_RISK";
        }

        if (pass == 1 && health >= 0.66d && risk <= 0.42d) {
            return "HEALTHY";
        }

        if (pass == 1) {
            return "PASS";
        }

        if (risk >= 0.72d) {
            return "RISK";
        }

        if (health >= 0.42d) {
            return "WATCH";
        }

        return "WEAK";
    }

    private String buildRuleKey(String targetIssue, int position, int mask, String ruleCode) {
        return safe(targetIssue) + "|P" + position + "|K3_" + mask + "|" + safe(ruleCode);
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private static double confidenceFromSample(int sample, int full) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (full <= 0) {
            full = 80;
        }

        if (sample >= full) {
            return 1.0d;
        }

        return clamp01(((double) sample) / ((double) full));
    }

    private static double posPart(double v) {
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

    private void appendCandidateReason(Object c, String add) {
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

    private static final class CandidateRuleBundle {
        int position;
        int n1;
        int n2;
        int n3;
        int k3Mask;

        ArrayList<Models.RuleHealth> rules = new ArrayList<Models.RuleHealth>();

        int ruleCount;
        int passCount;
        int hardCount;
        int pass;

        double avgHealth;
        double avgRisk;
        double minHealth;
        double maxRisk;
        double healthScore;
        double riskScore;
        double ruleDecayRisk;
        String ruleRejectCode;
        String ruleActionAdvice;

        String status;
        String summary;
    }

    private static final class TeacherBundle {
        int used;
        int sample;

        double score;
        double risk;
        double confidence;
        double hitRate;
        double conflict;
        double sampleScore;
    }

    private static final class RuleSummary {
        int count;
        int passCount;
        int hardCount;
        int pass;

        double health;
        double risk;
        String summary;
    }
}
