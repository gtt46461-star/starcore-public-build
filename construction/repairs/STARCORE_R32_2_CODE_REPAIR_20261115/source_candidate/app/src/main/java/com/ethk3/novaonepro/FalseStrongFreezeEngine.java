package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FalseStrongFreezeEngine.java
 *
 * <p>第14/25 新增核心引擎。
 *
 * <p>作用： 1. 识别“伪强候选”。 2. 对看起来强但风险结构不干净的候选做冻结、观察、降权。 3. 检查： - 高 finalP 但因链弱 - 高 edge 但第三防线弱 -
 * 教师号码强但样本弱 - SHADOW健康未成熟 - 反事实伪冠军 - 规则健康硬风险 - 位置漂移/位置错判 - 热尽/假冷补/弱样本放大 - 近期连挂
 *
 * <p>输出： 1. Models.FalseStrongRecord 2. 回写 CandidateK3： falseStrongFreeze / falseStrongFreezeRisk /
 * falseStrongFreezeLevel / falseStrongFreezeSummary 3. 回写 CauseChain： falseStrongFreeze /
 * falseStrongRisk / finalCauseScore / riskScore
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不直接改变最终裁决，只给 DecisionGateEngine 提供风险字段。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class FalseStrongFreezeEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double FREEZE_RISK_LINE = 0.72d;
    private static final double HARD_FREEZE_RISK_LINE = 0.84d;
    private static final double WATCH_RISK_LINE = 0.56d;

    public FalseStrongFreezeEngine() {}

    /** V43 typed ComboCandidate adapter preserving the full legacy calculation. */
    public ArrayList<Models.ComboCandidate> applyToAll(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.ComboCandidate> out =
                candidates == null ? new ArrayList<Models.ComboCandidate>() : candidates;
        ArrayList<Models.CandidateK3> bridge = LegacyCandidateBridge.toCandidateK3(out);
        ArrayList<Models.FalseStrongRecord> records =
                buildFalseStrongRecords(baseIssue, targetIssue, bridge);
        applyToCandidates(bridge, records);
        LegacyCandidateBridge.copyBack(bridge, out);
        try {
            if (store != null) {
                store.setState("v43_false_strong_apply_count", String.valueOf(out.size()));
                store.setState(
                        "v43_false_strong_record_count",
                        String.valueOf(records == null ? 0 : records.size()));
                store.setState("v43_false_strong_status", "APPLIED_TYPED_BRIDGE");
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    public ArrayList<Models.ComboCandidate> enrichCandidates(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> history,
            String baseIssue,
            String targetIssue) {
        return applyToAll(store, candidates, history, baseIssue, targetIssue);
    }

    public ArrayList<Models.FalseStrongRecord> buildFalseStrongRecords(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.FalseStrongRecord> out = new ArrayList<Models.FalseStrongRecord>();

        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);

        if (list.size() == 0) {
            return out;
        }

        RuntimeRisk runtime = buildRuntimeRisk(recentTrades);

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            Models.FalseStrongRecord r =
                    buildOneRecord(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            list,
                            shadowList,
                            ruleList,
                            thirdGuardList,
                            rankList,
                            teacherMap,
                            positionMap,
                            chains,
                            runtime);

            if (r != null) {
                out.add(r);
            }
        }

        applyToCandidates(list, out);
        applyToCauseChains(chains, out);
        rerankByFreeze(candidates);

        return out;
    }

    public ArrayList<Models.FalseStrongRecord> buildFalseStrongRecords(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildFalseStrongRecords(
                baseIssue, targetIssue, candidates, null, null, null, null, null, null, null, null);
    }

    public ArrayList<Models.FalseStrongRecord> buildFalseStrongRecords(
            List<Models.CandidateK3> candidates) {
        return buildFalseStrongRecords(
                "", "", candidates, null, null, null, null, null, null, null, null);
    }

    public Models.FalseStrongRecord buildChampionFreezeRecord(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.FalseStrongRecord> list =
                buildFalseStrongRecords(
                        baseIssue,
                        targetIssue,
                        candidates,
                        shadowList,
                        ruleList,
                        thirdGuardList,
                        rankList,
                        teacherMap,
                        positionMap,
                        chains,
                        recentTrades);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<Models.FalseStrongRecord> records) {
        if (candidates == null
                || records == null
                || candidates.size() == 0
                || records.size() == 0) {
            return;
        }

        LinkedHashMap<String, Models.FalseStrongRecord> map =
                new LinkedHashMap<String, Models.FalseStrongRecord>();

        for (int i = 0; i < records.size(); i++) {
            Models.FalseStrongRecord r = records.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", 0);
            int mask = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            map.put(candidateKey(p, mask), r);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            Models.FalseStrongRecord r = map.get(candidateKey(p, mask));

            if (r == null) {
                continue;
            }

            applyOneToCandidate(c, r);
        }
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.FalseStrongRecord> records) {
        if (chains == null || records == null || chains.size() == 0 || records.size() == 0) {
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

            Models.FalseStrongRecord best = findBestRecordForNumber(records, position, number);

            if (best == null) {
                continue;
            }

            double freezeRisk =
                    getDouble(best, "freezeRisk", getDouble(best, "falseStrongFreezeRisk", 0.0d));
            int freeze = getInt(best, "freeze", getInt(best, "falseStrongFreeze", 0));
            int hard = getInt(best, "hardFreeze", 0);
            double score =
                    getDouble(best, "freezeScore", getDouble(best, "falseStrongScore", 0.0d));

            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double oldRisk = getDouble(chain, "riskScore", 0.0d);

            double newFinal;
            if (freeze == 1) {
                newFinal = clamp01(oldFinal - Math.max(0.020d, freezeRisk * 0.055d));
            } else {
                newFinal = clamp01(oldFinal + Math.max(0.0d, score - freezeRisk) * 0.018d);
            }

            if (hard == 1) {
                newFinal = clamp01(newFinal - 0.035d);
            }

            double newRisk = clamp01(Math.max(oldRisk, freezeRisk));

            putDouble(chain, "falseStrongRisk", freezeRisk);
            putDouble(chain, "falseStrongScore", score);
            putInt(chain, "falseStrongFreeze", freeze);
            putInt(chain, "falseStrongHardFreeze", hard);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);
            putDouble(chain, "riskScore", newRisk);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜伪强冻结：freeze="
                            + freeze
                            + "，hard="
                            + hard
                            + "，risk="
                            + percent(freezeRisk)
                            + "，score="
                            + percent(score)
                            + "，说明=只写冻结风险，不直接创建BET";

            putString(chain, "summaryText", old + add);
        }
    }

    private Models.FalseStrongRecord buildOneRecord(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<Models.CandidateK3> ranked,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            RuntimeRisk runtime) {
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

        FalseStrongParts p =
                buildParts(
                        c,
                        rankNo,
                        ranked,
                        shadowList,
                        ruleList,
                        thirdGuardList,
                        rankList,
                        teacherMap,
                        positionMap,
                        chains,
                        runtime);

        double support = 0.0d;
        support += p.candidateScore * 0.16d;
        support += p.probHealth * 0.10d;
        support += p.causeScore * 0.14d;
        support += p.ruleHealth * 0.12d;
        support += p.thirdGuardScore * 0.10d;
        support += p.shadowHealth * 0.10d;
        support += p.teacherScore * 0.09d;
        support += p.rankScore * 0.09d;
        support += p.positionHealth * 0.06d;
        support += p.sampleConfidence * 0.08d;
        support += p.balanceScore * 0.06d;

        double risk = 0.0d;
        risk += p.highProbWeakCauseRisk * 0.12d;
        risk += p.highEdgeLowSupportRisk * 0.10d;
        risk += p.teacherWeakSampleRisk * 0.10d;
        risk += p.thirdGuardHardRisk * 0.12d;
        risk += p.ruleHardRisk * 0.10d;
        risk += p.shadowImmatureRisk * 0.09d;
        risk += p.falseChampionRisk * 0.09d;
        risk += p.positionDriftRisk * 0.08d;
        risk += p.hotExhaustionRisk * 0.07d;
        risk += p.coldFakeRecoveryRisk * 0.07d;
        risk += p.nearConflictRisk * 0.06d;
        risk += p.loseStreakRisk * 0.08d;
        risk += p.rawRisk * 0.10d;

        double freezeRisk = clamp01(risk + Math.max(0.0d, 0.48d - support) * 0.20d);
        double freezeScore = clamp01(support - freezeRisk * 0.38d + 0.05d);

        int hardFreeze = isHardFreeze(p, freezeRisk) ? 1 : 0;
        int freeze = hardFreeze == 1 || freezeRisk >= FREEZE_RISK_LINE ? 1 : 0;
        int watch = freeze == 0 && freezeRisk >= WATCH_RISK_LINE ? 1 : 0;

        int thawEligible =
                freeze == 0
                                && freezeRisk <= 0.46d
                                && freezeScore >= 0.58d
                                && p.sampleConfidence >= 0.40d
                                && p.thirdGuardHardRisk < 0.30d
                                && p.ruleHardRisk < 0.30d
                        ? 1
                        : 0;

        String level = buildLevel(freeze, hardFreeze, watch, thawEligible, freezeRisk, freezeScore);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        freeze,
                        hardFreeze,
                        watch,
                        thawEligible,
                        freezeRisk,
                        freezeScore,
                        level,
                        p);

        Models.FalseStrongRecord r = new Models.FalseStrongRecord();

        putString(r, "recordKey", buildRecordKey(targetIssue, position, mask));
        putString(r, "freezeKey", buildRecordKey(targetIssue, position, mask));
        putString(r, "baseIssue", safe(baseIssue));
        putString(r, "targetIssue", safe(targetIssue));
        putLong(r, "createdAt", System.currentTimeMillis());

        putInt(r, "rankNo", rankNo);
        putInt(r, "rank", rankNo);
        putInt(r, "position", position);
        putInt(r, "n1", n1);
        putInt(r, "n2", n2);
        putInt(r, "n3", n3);
        putInt(r, "k3Mask", mask);
        putInt(r, "k3_mask", mask);

        putDouble(r, "freezeScore", freezeScore);
        putDouble(r, "falseStrongScore", freezeScore);
        putDouble(r, "freezeRisk", freezeRisk);
        putDouble(r, "falseStrongFreezeRisk", freezeRisk);
        putInt(r, "freeze", freeze);
        putInt(r, "falseStrongFreeze", freeze);
        putInt(r, "hardFreeze", hardFreeze);
        putInt(r, "watch", watch);
        putInt(r, "thawEligible", thawEligible);

        putDouble(r, "candidateScore", p.candidateScore);
        putDouble(r, "probHealth", p.probHealth);
        putDouble(r, "causeScore", p.causeScore);
        putDouble(r, "ruleHealth", p.ruleHealth);
        putDouble(r, "ruleRisk", p.ruleRisk);
        putDouble(r, "thirdGuardScore", p.thirdGuardScore);
        putDouble(r, "thirdGuardRisk", p.thirdGuardRisk);
        putDouble(r, "shadowHealth", p.shadowHealth);
        putDouble(r, "shadowRisk", p.shadowRisk);
        putDouble(r, "teacherScore", p.teacherScore);
        putDouble(r, "teacherRisk", p.teacherRisk);
        putDouble(r, "rankScore", p.rankScore);
        putDouble(r, "rankRisk", p.rankRisk);
        putDouble(r, "positionHealth", p.positionHealth);
        putDouble(r, "positionRisk", p.positionRisk);
        putDouble(r, "sampleConfidence", p.sampleConfidence);
        putDouble(r, "balanceScore", p.balanceScore);

        putDouble(r, "highProbWeakCauseRisk", p.highProbWeakCauseRisk);
        putDouble(r, "highEdgeLowSupportRisk", p.highEdgeLowSupportRisk);
        putDouble(r, "teacherWeakSampleRisk", p.teacherWeakSampleRisk);
        putDouble(r, "thirdGuardHardRisk", p.thirdGuardHardRisk);
        putDouble(r, "ruleHardRisk", p.ruleHardRisk);
        putDouble(r, "shadowImmatureRisk", p.shadowImmatureRisk);
        putDouble(r, "falseChampionRisk", p.falseChampionRisk);
        putDouble(r, "positionDriftRisk", p.positionDriftRisk);
        putDouble(r, "hotExhaustionRisk", p.hotExhaustionRisk);
        putDouble(r, "coldFakeRecoveryRisk", p.coldFakeRecoveryRisk);
        putDouble(r, "nearConflictRisk", p.nearConflictRisk);
        putDouble(r, "loseStreakRisk", p.loseStreakRisk);
        putDouble(r, "rawRisk", p.rawRisk);

        putString(r, "level", level);
        putString(r, "status", level);
        putString(r, "summaryText", summary);
        putString(r, "reasonText", summary);

        return r;
    }

    private FalseStrongParts buildParts(
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<Models.CandidateK3> ranked,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            RuntimeRisk runtime) {
        FalseStrongParts p = new FalseStrongParts();

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        p.finalP = getDouble(c, "finalP", 0.0d);
        p.dynamicGate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        p.edge = getDouble(c, "edge", p.finalP - p.dynamicGate);
        p.rawRisk = getDouble(c, "riskScore", 0.0d);
        p.causeScore = getDouble(c, "causeScore", 0.0d);
        p.candidateScore = candidateScore(c);
        p.probHealth = calcProbHealth(p.finalP, p.dynamicGate, p.edge);

        RulePack rule = readRule(position, mask, ruleList);
        p.ruleHealth = rule.health;
        p.ruleRisk = rule.risk;
        p.ruleHardCount = rule.hardCount;
        p.rulePassCount = rule.passCount;
        p.ruleCount = rule.ruleCount;

        GuardPack guard = readGuard(position, mask, thirdGuardList);
        p.thirdGuardScore = guard.score;
        p.thirdGuardRisk = guard.risk;
        p.thirdGuardHard = guard.hard;
        p.thirdGuardPass = guard.pass;

        ShadowPack shadow = readShadow(position, mask, shadowList);
        p.shadowHealth = shadow.health;
        p.shadowRisk = shadow.risk;
        p.shadowPass = shadow.pass;
        p.shadowPromote = shadow.promote;
        p.shadowHard = shadow.hard;
        p.shadowSample = shadow.sample;
        p.shadowHitRate = shadow.hitRate;

        TeacherPack teacher = readTeacher(position, n1, n2, n3, teacherMap);
        p.teacherScore = teacher.score;
        p.teacherRisk = teacher.risk;
        p.teacherConfidence = teacher.confidence;
        p.teacherSample = teacher.sample;

        RankPack rank = readRank(position, mask, rankList);
        p.rankScore = rank.score;
        p.rankRisk = rank.risk;
        p.falseChampionRisk = rank.falseChampionRisk;
        p.nearConflictRisk = rank.nearConflictRisk;
        p.missedStrongRisk = rank.missedStrongRisk;

        PositionPack pos = readPosition(position, positionMap);
        p.positionHealth = pos.health;
        p.positionRisk = pos.risk;
        p.positionDriftRisk = pos.driftRisk;
        p.positionFalseStrongRisk = pos.falseStrongRisk;

        ChainPack chain = readChain(position, n1, n2, n3, chains);
        p.chainScore = chain.score;
        p.chainRisk = chain.risk;
        p.chainSample = chain.sample;

        p.sampleConfidence =
                calcSampleConfidence(p.teacherSample, p.shadowSample, p.chainSample, p.ruleCount);
        p.balanceScore = calcK3BalanceScore(n1, n2, n3, teacher);

        p.highProbWeakCauseRisk = calcHighProbWeakCauseRisk(p);
        p.highEdgeLowSupportRisk = calcHighEdgeLowSupportRisk(p);
        p.teacherWeakSampleRisk = calcTeacherWeakSampleRisk(p);
        p.thirdGuardHardRisk = p.thirdGuardHard == 1 ? 0.90d : clamp01(p.thirdGuardRisk * 0.62d);
        p.ruleHardRisk = p.ruleHardCount > 0 ? 0.88d : clamp01(p.ruleRisk * 0.62d);
        p.shadowImmatureRisk = calcShadowImmatureRisk(p);
        p.positionDriftRisk = Math.max(p.positionDriftRisk, p.positionRisk * 0.64d);
        p.hotExhaustionRisk =
                getDouble(c, "hotExhaustionRisk", getDouble(c, "hotReverseRisk", 0.0d));
        p.coldFakeRecoveryRisk = getDouble(c, "coldFakeRecoveryRisk", 0.0d);
        p.loseStreakRisk = runtime == null ? 0.0d : runtime.loseStreakRisk;

        if (rankNo == 1 && ranked != null && ranked.size() > 1) {
            double first = candidateScore(ranked.get(0));
            double second = candidateScore(ranked.get(1));

            if (first - second < 0.030d) {
                p.falseChampionRisk = Math.max(p.falseChampionRisk, 0.42d);
            }
        }

        return p;
    }

    private double calcHighProbWeakCauseRisk(FalseStrongParts p) {
        double risk = 0.0d;

        if (p.finalP >= 0.395d && p.causeScore < 0.48d) {
            risk += 0.22d;
        }

        if (p.finalP >= 0.420d && p.causeScore < 0.55d) {
            risk += 0.20d;
        }

        if (p.finalP >= 0.390d && p.chainScore < 0.38d) {
            risk += 0.14d;
        }

        if (p.finalP >= 0.385d && p.thirdGuardScore < 0.42d) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private double calcHighEdgeLowSupportRisk(FalseStrongParts p) {
        double risk = 0.0d;

        if (p.edge >= 0.018d && p.ruleHealth < 0.44d) {
            risk += 0.18d;
        }

        if (p.edge >= 0.014d && p.teacherScore < 0.36d) {
            risk += 0.16d;
        }

        if (p.edge >= 0.014d && p.shadowHealth < 0.42d) {
            risk += 0.14d;
        }

        if (p.edge >= 0.020d && p.sampleConfidence < 0.34d) {
            risk += 0.18d;
        }

        return clamp01(risk);
    }

    private double calcTeacherWeakSampleRisk(FalseStrongParts p) {
        double risk = 0.0d;

        if (p.teacherScore >= 0.56d && p.teacherSample < 30) {
            risk += 0.18d;
        }

        if (p.teacherScore >= 0.62d && p.teacherConfidence < 0.32d) {
            risk += 0.20d;
        }

        if (p.teacherRisk > p.teacherScore + 0.12d) {
            risk += 0.18d;
        }

        if (p.teacherSample <= 0 && p.finalP >= 0.385d) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private double calcShadowImmatureRisk(FalseStrongParts p) {
        double risk = 0.0d;
        int warmup = readWarmupIssues();

        if (p.shadowSample < warmup) {
            risk +=
                    clamp01(((double) (warmup - p.shadowSample)) / Math.max(1.0d, (double) warmup))
                            * 0.24d;
        }

        if (p.shadowPass == 0 && p.finalP >= 0.385d) {
            risk += 0.16d;
        }

        if (p.shadowHard == 1) {
            risk += 0.36d;
        }

        if (p.shadowHitRate > 0.0d && p.shadowHitRate < 0.30d && p.shadowSample >= 20) {
            risk += 0.18d;
        }

        return clamp01(risk);
    }

    private boolean isHardFreeze(FalseStrongParts p, double risk) {
        if (risk >= HARD_FREEZE_RISK_LINE) {
            return true;
        }

        if (p == null) {
            return false;
        }

        if (p.thirdGuardHard == 1) {
            return true;
        }

        if (p.ruleHardCount > 0 && p.ruleRisk >= 0.72d) {
            return true;
        }

        if (p.shadowHard == 1 && p.shadowRisk >= 0.70d) {
            return true;
        }

        if (p.highProbWeakCauseRisk >= 0.58d && p.sampleConfidence < 0.30d) {
            return true;
        }

        if (p.loseStreakRisk >= 0.72d && p.falseChampionRisk >= 0.55d) {
            return true;
        }

        return false;
    }

    private void applyOneToCandidate(Models.CandidateK3 c, Models.FalseStrongRecord r) {
        if (c == null || r == null) {
            return;
        }

        double freezeRisk = getDouble(r, "freezeRisk", getDouble(r, "falseStrongFreezeRisk", 0.0d));
        double freezeScore = getDouble(r, "freezeScore", getDouble(r, "falseStrongScore", 0.0d));
        int freeze = getInt(r, "freeze", getInt(r, "falseStrongFreeze", 0));
        int hard = getInt(r, "hardFreeze", 0);
        int watch = getInt(r, "watch", 0);
        int thawEligible = getInt(r, "thawEligible", 0);

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;

        if (freeze == 1) {
            newCause = clamp01(oldCause * 0.94d - freezeRisk * 0.060d);
        } else {
            newCause = clamp01(oldCause * 0.93d + freezeScore * 0.070d);
        }

        if (hard == 1) {
            newCause = clamp01(newCause - 0.040d);
        }

        double newRisk = clamp01(Math.max(oldRisk, freezeRisk));

        putDouble(c, "falseStrongFreezeRisk", freezeRisk);
        putDouble(
                c, "falseStrongRisk", Math.max(getDouble(c, "falseStrongRisk", 0.0d), freezeRisk));
        putDouble(c, "falseStrongScore", freezeScore);
        putInt(c, "falseStrongFreeze", freeze);
        putInt(c, "falseStrongHardFreeze", hard);
        putInt(c, "falseStrongWatch", watch);
        putInt(c, "falseStrongThawEligible", thawEligible);
        putString(c, "falseStrongFreezeLevel", getString(r, "level", getString(r, "status", "")));
        putString(c, "falseStrongFreezeSummary", getString(r, "summaryText", ""));

        putDouble(c, "highProbWeakCauseRisk", getDouble(r, "highProbWeakCauseRisk", 0.0d));
        putDouble(c, "highEdgeLowSupportRisk", getDouble(r, "highEdgeLowSupportRisk", 0.0d));
        putDouble(c, "teacherWeakSampleRisk", getDouble(r, "teacherWeakSampleRisk", 0.0d));
        putDouble(c, "shadowImmatureRisk", getDouble(r, "shadowImmatureRisk", 0.0d));

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendReason(c, getString(r, "summaryText", ""));
    }

    private void rerankByFreeze(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = frozenCandidateScore(a);
                        double sb = frozenCandidateScore(b);

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

    private static double frozenCandidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double base = candidateScore(c);
        double freezeRisk =
                getDouble(c, "falseStrongFreezeRisk", getDouble(c, "falseStrongRisk", 0.0d));
        double freezeScore = getDouble(c, "falseStrongScore", 0.0d);
        int freeze = getInt(c, "falseStrongFreeze", 0);
        int hard = getInt(c, "falseStrongHardFreeze", 0);
        int thaw = getInt(c, "falseStrongThawEligible", 0);

        double score = base * 0.86d + freezeScore * 0.14d - freezeRisk * 0.12d;

        if (freeze == 1) {
            score -= 0.045d;
        }

        if (hard == 1) {
            score -= 0.070d;
        }

        if (thaw == 1) {
            score += 0.012d;
        }

        return clamp01(score);
    }

    private static double candidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double rule = getDouble(c, "ruleHealthScore", 0.0d);
        double guard = getDouble(c, "thirdGuardScore", 0.0d);
        double shadow = getDouble(c, "shadowHealthScore", 0.0d);
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double rank = getDouble(c, "counterfactualScore", 0.0d);
        double risk = getDouble(c, "riskScore", 0.0d);

        double score = 0.0d;

        score += finalP * 0.20d;
        score += posPart(edge * 4.0d) * 0.08d;
        score += cause * 0.20d;
        score += rule * 0.12d;
        score += guard * 0.12d;
        score += shadow * 0.10d;
        score += teacher * 0.09d;
        score += rank * 0.09d;

        score -= risk * 0.12d;

        return clamp01(score);
    }

    private double calcProbHealth(double finalP, double gate, double edge) {
        double pScore = clamp01((finalP - 0.30d) / 0.18d);
        double edgeScore = clamp01((edge + 0.030d) / 0.090d);
        double gateScore = gate <= 0.0d ? 0.50d : (finalP >= gate ? 0.72d : 0.32d);

        return clamp01(pScore * 0.38d + edgeScore * 0.40d + gateScore * 0.22d);
    }

    private RulePack readRule(int position, int mask, ArrayList<Models.RuleHealth> list) {
        RulePack p = new RulePack();

        if (list == null || list.size() == 0) {
            return p;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.RuleHealth r = list.get(i);

            if (r == null) {
                continue;
            }

            int rp = getInt(r, "position", 0);
            int rm = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            if (rp != position || rm != mask) {
                continue;
            }

            p.health += getDouble(r, "ruleHealthScore", getDouble(r, "healthScore", 0.0d));
            p.risk += getDouble(r, "ruleRiskScore", getDouble(r, "riskScore", 0.0d));

            if (getInt(r, "rulePass", getInt(r, "pass", 0)) == 1) {
                p.passCount++;
            }

            if (getInt(r, "hardBlock", 0) == 1) {
                p.hardCount++;
            }

            p.ruleCount++;
        }

        if (p.ruleCount > 0) {
            p.health = clamp01(p.health / p.ruleCount);
            p.risk = clamp01(p.risk / p.ruleCount + (p.hardCount > 0 ? 0.12d : 0.0d));
        }

        return p;
    }

    private GuardPack readGuard(int position, int mask, ArrayList<Models.ThirdGuardStat> list) {
        GuardPack p = new GuardPack();

        if (list == null) {
            return p;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ThirdGuardStat g = list.get(i);

            if (g == null) {
                continue;
            }

            int gp = getInt(g, "position", 0);
            int gm = getInt(g, "k3Mask", getInt(g, "k3_mask", 0));

            if (gp == position && gm == mask) {
                p.score = getDouble(g, "thirdGuardScore", getDouble(g, "guardScore", 0.0d));
                p.risk = getDouble(g, "thirdGuardRisk", getDouble(g, "guardRisk", 0.0d));
                p.pass = getInt(g, "thirdGuardPass", getInt(g, "pass", 0));
                p.hard = getInt(g, "hardBlock", 0);
                return p;
            }
        }

        return p;
    }

    private ShadowPack readShadow(int position, int mask, ArrayList<Models.ShadowHealth> list) {
        ShadowPack p = new ShadowPack();

        if (list == null) {
            return p;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ShadowHealth h = list.get(i);

            if (h == null) {
                continue;
            }

            int sp = getInt(h, "position", 0);
            int sm = getInt(h, "k3Mask", getInt(h, "k3_mask", 0));

            if (sp == position && sm == mask) {
                p.health = getDouble(h, "shadowHealthScore", getDouble(h, "healthScore", 0.0d));
                p.risk = getDouble(h, "shadowHealthRisk", getDouble(h, "riskScore", 0.0d));
                p.pass = getInt(h, "shadowPass", getInt(h, "pass", 0));
                p.promote = getInt(h, "shadowPromoteEligible", getInt(h, "promoteEligible", 0));
                p.hard = getInt(h, "hardBlock", 0);
                p.sample = getInt(h, "shadowSample", 0);
                p.hitRate = getDouble(h, "shadowHitRate", 0.0d);
                return p;
            }
        }

        return p;
    }

    private RankPack readRank(int position, int mask, ArrayList<Models.RankCounterfactual> list) {
        RankPack p = new RankPack();

        if (list == null) {
            return p;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.RankCounterfactual r = list.get(i);

            if (r == null) {
                continue;
            }

            int rp = getInt(r, "position", getInt(r, "chosenPosition", 0));
            int rm = getInt(r, "k3Mask", getInt(r, "chosenK3Mask", 0));

            if (rp == position && rm == mask) {
                p.score = getDouble(r, "rankScore", getDouble(r, "counterfactualScore", 0.0d));
                p.missedStrongRisk = getDouble(r, "missedStrongRisk", 0.0d);
                p.nearConflictRisk = getDouble(r, "nearConflictRisk", 0.0d);
                p.falseChampionRisk = getDouble(r, "falseChampionRisk", 0.0d);
                p.risk =
                        clamp01(
                                p.missedStrongRisk * 0.30d
                                        + p.nearConflictRisk * 0.26d
                                        + p.falseChampionRisk * 0.30d);
                return p;
            }
        }

        return p;
    }

    private TeacherPack readTeacher(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        TeacherPack p = new TeacherPack();

        if (map == null) {
            return p;
        }

        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            p.score += getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            p.risk += getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            p.confidence += getDouble(c, "confidence", getDouble(c, "sampleConfidence", 0.0d));
            p.sample += getInt(c, "sampleCount", getInt(c, "sample", 0));
            p.hitRate += getDouble(c, "hitRate", getDouble(c, "recentHitRate", 0.0d));
            p.used++;
        }

        if (p.used > 0) {
            p.score = clamp01(p.score / p.used);
            p.risk = clamp01(p.risk / p.used);
            p.confidence = clamp01(p.confidence / p.used);
            p.hitRate = clamp01(p.hitRate / p.used);
        }

        return p;
    }

    private PositionPack readPosition(int position, Map<Integer, Models.PositionHealth> map) {
        PositionPack p = new PositionPack();

        if (map == null) {
            return p;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return p;
        }

        p.health = getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
        p.risk =
                getDouble(
                        h,
                        "riskScore",
                        getDouble(h, "positionRiskScore", getDouble(h, "wrongRisk", 0.0d)));
        p.driftRisk = getDouble(h, "driftRisk", getDouble(h, "positionDriftRisk", 0.0d));
        p.falseStrongRisk =
                getDouble(h, "falseStrongRisk", getDouble(h, "positionFalseStrongRisk", 0.0d));

        return p;
    }

    private ChainPack readChain(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        ChainPack p = new ChainPack();

        if (chains == null) {
            return p;
        }

        for (int i = 0; i < chains.size(); i++) {
            Models.CauseChain c = chains.get(i);

            if (c == null) {
                continue;
            }

            int cp = getInt(c, "position", 0);
            int cn = getInt(c, "number", 0);

            if (cp != position) {
                continue;
            }

            if (cn != n1 && cn != n2 && cn != n3) {
                continue;
            }

            p.score += getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            p.risk += getDouble(c, "riskScore", 0.0d);
            p.sample++;
        }

        if (p.sample > 0) {
            p.score = clamp01(p.score / p.sample);
            p.risk = clamp01(p.risk / p.sample);
        }

        return p;
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

    private RuntimeRisk buildRuntimeRisk(List<Models.Trade> trades) {
        RuntimeRisk r = new RuntimeRisk();

        if (trades == null || trades.size() == 0) {
            return r;
        }

        int checked = 0;
        int loseStreak = 0;
        int win = 0;
        int miss = 0;

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

        if (loseStreak <= 0) {
            r.loseStreakRisk = 0.0d;
        } else if (loseStreak >= 5) {
            r.loseStreakRisk = 0.76d;
        } else {
            r.loseStreakRisk = clamp01(((double) loseStreak) / 7.0d);
        }

        return r;
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

    private Models.FalseStrongRecord findBestRecordForNumber(
            ArrayList<Models.FalseStrongRecord> records, int position, int number) {
        Models.FalseStrongRecord best = null;
        double bestRisk = -1.0d;

        for (int i = 0; i < records.size(); i++) {
            Models.FalseStrongRecord r = records.get(i);

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

            double risk = getDouble(r, "freezeRisk", getDouble(r, "falseStrongFreezeRisk", 0.0d));

            if (best == null || risk > bestRisk) {
                best = r;
                bestRisk = risk;
            }
        }

        return best;
    }

    private double calcSampleConfidence(
            int teacherSample, int shadowSample, int chainSample, int ruleCount) {
        double teacher = confidenceFromSample(teacherSample, 90);
        double shadow = confidenceFromSample(shadowSample, readWarmupIssues());
        double chain = confidenceFromSample(chainSample, 30);
        double rule = confidenceFromSample(ruleCount, 10);

        return clamp01(teacher * 0.32d + shadow * 0.30d + chain * 0.20d + rule * 0.18d);
    }

    private double calcK3BalanceScore(int n1, int n2, int n3, TeacherPack teacher) {
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

        if (teacher != null) {
            score += teacher.score * 0.16d;
            score += (1.0d - teacher.risk) * 0.08d;
        }

        return clamp01(score);
    }

    private String buildLevel(
            int freeze, int hard, int watch, int thaw, double risk, double score) {
        if (hard == 1) {
            return "HARD_FREEZE";
        }

        if (freeze == 1) {
            return "FREEZE";
        }

        if (watch == 1) {
            return "WATCH";
        }

        if (thaw == 1) {
            return "THAW_READY";
        }

        if (score >= 0.62d && risk <= 0.42d) {
            return "CLEAN";
        }

        return "NORMAL";
    }

    private String buildSummary(
            int rankNo,
            int position,
            int n1,
            int n2,
            int n3,
            int freeze,
            int hard,
            int watch,
            int thaw,
            double risk,
            double score,
            String level,
            FalseStrongParts p) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20伪强冻结｜rank=");
        sb.append(rankNo);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);
        sb.append("｜level=");
        sb.append(level);
        sb.append("｜freeze=");
        sb.append(freeze);
        sb.append("｜hard=");
        sb.append(hard);
        sb.append("｜watch=");
        sb.append(watch);
        sb.append("｜thaw=");
        sb.append(thaw);
        sb.append("｜冻结风险=");
        sb.append(percent(risk));
        sb.append("｜冻结分=");
        sb.append(percent(score));
        sb.append("｜finalP=");
        sb.append(percent(p.finalP));
        sb.append("｜edge=");
        sb.append(signed(p.edge));
        sb.append("｜因链=");
        sb.append(percent(p.causeScore));
        sb.append("｜规则风险=");
        sb.append(percent(p.ruleRisk));
        sb.append("｜第三防线风险=");
        sb.append(percent(p.thirdGuardRisk));
        sb.append("｜SHADOW风险=");
        sb.append(percent(p.shadowRisk));
        sb.append("｜教师风险=");
        sb.append(percent(p.teacherRisk));
        sb.append("｜反事实风险=");
        sb.append(percent(p.rankRisk));
        sb.append("｜位置风险=");
        sb.append(percent(p.positionRisk));
        sb.append("｜高P弱因=");
        sb.append(percent(p.highProbWeakCauseRisk));
        sb.append("｜高edge弱支撑=");
        sb.append(percent(p.highEdgeLowSupportRisk));
        sb.append("｜教师弱样本=");
        sb.append(percent(p.teacherWeakSampleRisk));
        sb.append("｜影子未熟=");
        sb.append(percent(p.shadowImmatureRisk));
        sb.append("｜伪冠军=");
        sb.append(percent(p.falseChampionRisk));
        sb.append("｜热尽=");
        sb.append(percent(p.hotExhaustionRisk));
        sb.append("｜假冷补=");
        sb.append(percent(p.coldFakeRecoveryRisk));
        sb.append("｜连挂=");
        sb.append(percent(p.loseStreakRisk));
        sb.append("｜说明=伪强冻结只写风险字段，不创建BET，不结算，不污染真实校准，最终仍由DecisionGateEngine裁决");

        return sb.toString();
    }

    private String buildRecordKey(String targetIssue, int position, int mask) {
        return safe(targetIssue) + "|P" + position + "|K3_" + mask;
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private static int readWarmupIssues() {
        int v = readAppInt("LIVE_SHADOW_WARMUP_ISSUES", 30);

        if (v <= 0) {
            v = readAppInt("SHADOW_WARMUP_ISSUES", 30);
        }

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

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
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

    private static final class FalseStrongParts {
        double finalP;
        double dynamicGate;
        double edge;

        double candidateScore;
        double probHealth;
        double causeScore;
        double chainScore;
        double chainRisk;
        int chainSample;

        double ruleHealth;
        double ruleRisk;
        int ruleCount;
        int rulePassCount;
        int ruleHardCount;
        double ruleHardRisk;

        double thirdGuardScore;
        double thirdGuardRisk;
        int thirdGuardPass;
        int thirdGuardHard;
        double thirdGuardHardRisk;

        double shadowHealth;
        double shadowRisk;
        int shadowPass;
        int shadowPromote;
        int shadowHard;
        int shadowSample;
        double shadowHitRate;

        double teacherScore;
        double teacherRisk;
        double teacherConfidence;
        int teacherSample;

        double rankScore;
        double rankRisk;
        double falseChampionRisk;
        double nearConflictRisk;
        double missedStrongRisk;

        double positionHealth;
        double positionRisk;
        double positionDriftRisk;
        double positionFalseStrongRisk;

        double sampleConfidence;
        double balanceScore;

        double highProbWeakCauseRisk;
        double highEdgeLowSupportRisk;
        double teacherWeakSampleRisk;
        double shadowImmatureRisk;
        double hotExhaustionRisk;
        double coldFakeRecoveryRisk;
        double loseStreakRisk;
        double rawRisk;
    }

    private static final class RulePack {
        int ruleCount;
        int passCount;
        int hardCount;
        double health;
        double risk;
    }

    private static final class GuardPack {
        double score;
        double risk;
        int pass;
        int hard;
    }

    private static final class ShadowPack {
        double health;
        double risk;
        int pass;
        int promote;
        int hard;
        int sample;
        double hitRate;
    }

    private static final class RankPack {
        double score;
        double risk;
        double missedStrongRisk;
        double nearConflictRisk;
        double falseChampionRisk;
    }

    private static final class TeacherPack {
        int used;
        int sample;
        double score;
        double risk;
        double confidence;
        double hitRate;
    }

    private static final class PositionPack {
        double health;
        double risk;
        double driftRisk;
        double falseStrongRisk;
    }

    private static final class ChainPack {
        int sample;
        double score;
        double risk;
    }

    private static final class RuntimeRisk {
        int checked;
        int win;
        int miss;
        int loseStreak;
        double loseStreakRisk;
    }
}
