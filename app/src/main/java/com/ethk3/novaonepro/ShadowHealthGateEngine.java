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
 * ShadowHealthGateEngine.java
 *
 * <p>第13/25 新增核心引擎。
 *
 * <p>作用： 1. 只做 SHADOW 影子健康门审计。 2. 判断候选是否具备健康的“影子观察画像”。 3. 综合： - 规则健康 - 第三防线 - 反事实排名 - 教师号码 - 位置健康 -
 * 样本置信 - 近期影子表现 - 连挂风险 - 真实交易隔离状态 4. 输出 Models.ShadowHealth。 5. 回写 CandidateK3： shadowHealthScore /
 * shadowHealthRisk / shadowPass / shadowPromoteEligible / shadowHealthSummary。 6. 回写 CauseChain：
 * shadowHealthScore / shadowHealthRisk / shadowPass。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不把SHADOW/HOLD当真实交易统计。 - 不降低最终门槛。 - 不绕过
 * DecisionGateEngine。
 */
public final class ShadowHealthGateEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double PASS_SCORE_MIN = 0.54d;
    private static final double PROMOTE_SCORE_MIN = 0.64d;
    private static final double PASS_RISK_MAX = 0.60d;
    private static final double PROMOTE_RISK_MAX = 0.46d;
    private static final double HARD_RISK = 0.78d;

    public ShadowHealthGateEngine() {}

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
        ArrayList<Models.ShadowHealth> records =
                buildShadowHealthGate(baseIssue, targetIssue, bridge);
        applyToCandidates(bridge, records);
        LegacyCandidateBridge.copyBack(bridge, out);
        try {
            if (store != null) {
                store.setState("v43_shadow_health_apply_count", String.valueOf(out.size()));
                store.setState(
                        "v43_shadow_health_record_count",
                        String.valueOf(records == null ? 0 : records.size()));
                store.setState("v43_shadow_health_status", "APPLIED_TYPED_BRIDGE");
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

    public ArrayList<Models.ShadowHealth> buildShadowHealthGate(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            ArrayList<Models.RuleHealth> ruleHealthList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.ShadowHealth> out = new ArrayList<Models.ShadowHealth>();

        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);

        if (list.size() == 0) {
            return out;
        }

        ShadowRuntime runtime = buildRuntime(recentTrades);

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            Models.ShadowHealth h =
                    buildOneShadowHealth(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            ruleHealthList,
                            thirdGuardList,
                            rankList,
                            teacherMap,
                            positionHealthMap,
                            chains,
                            runtime);

            if (h != null) {
                out.add(h);
            }
        }

        applyToCandidates(list, out);
        applyToCauseChains(chains, out);
        rerankByShadowHealth(candidates);

        return out;
    }

    public ArrayList<Models.ShadowHealth> buildShadowHealthGate(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildShadowHealthGate(
                baseIssue, targetIssue, candidates, null, null, null, null, null, null, null);
    }

    public ArrayList<Models.ShadowHealth> buildShadowHealthGate(
            List<Models.CandidateK3> candidates) {
        return buildShadowHealthGate("", "", candidates, null, null, null, null, null, null, null);
    }

    public Models.ShadowHealth buildChampionShadowHealth(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            ArrayList<Models.RuleHealth> ruleHealthList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            List<Models.Trade> recentTrades) {
        ArrayList<Models.ShadowHealth> list =
                buildShadowHealthGate(
                        baseIssue,
                        targetIssue,
                        candidates,
                        ruleHealthList,
                        thirdGuardList,
                        rankList,
                        teacherMap,
                        positionHealthMap,
                        chains,
                        recentTrades);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<Models.ShadowHealth> healthList) {
        if (candidates == null
                || healthList == null
                || candidates.size() == 0
                || healthList.size() == 0) {
            return;
        }

        LinkedHashMap<String, Models.ShadowHealth> map =
                new LinkedHashMap<String, Models.ShadowHealth>();

        for (int i = 0; i < healthList.size(); i++) {
            Models.ShadowHealth h = healthList.get(i);

            if (h == null) {
                continue;
            }

            int p = getInt(h, "position", 0);
            int mask = getInt(h, "k3Mask", getInt(h, "k3_mask", 0));

            map.put(candidateKey(p, mask), h);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            Models.ShadowHealth h = map.get(candidateKey(p, mask));

            if (h == null) {
                continue;
            }

            applyOneToCandidate(c, h);
        }
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<Models.ShadowHealth> healthList) {
        if (chains == null || healthList == null || chains.size() == 0 || healthList.size() == 0) {
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

            Models.ShadowHealth best = findBestHealthForNumber(healthList, position, number);

            if (best == null) {
                continue;
            }

            double score =
                    getDouble(best, "shadowHealthScore", getDouble(best, "healthScore", 0.0d));
            double risk = getDouble(best, "shadowHealthRisk", getDouble(best, "riskScore", 0.0d));
            int pass = getInt(best, "shadowPass", getInt(best, "pass", 0));
            int promote = getInt(best, "shadowPromoteEligible", getInt(best, "promoteEligible", 0));

            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double oldRisk = getDouble(chain, "riskScore", 0.0d);

            double shadowCause = clampSigned((score - risk) * 1.05d);

            double newFinal;

            if (pass == 1) {
                newFinal = clamp01(oldFinal + posPart(shadowCause) * 0.026d);
            } else {
                newFinal = clamp01(oldFinal - Math.max(0.014d, risk * 0.040d));
            }

            double newRisk = clamp01(Math.max(oldRisk, risk));

            putDouble(chain, "shadowHealthScore", score);
            putDouble(chain, "shadowHealthRisk", risk);
            putDouble(chain, "shadowCause", shadowCause);
            putInt(chain, "shadowPass", pass);
            putInt(chain, "shadowPromoteEligible", promote);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);
            putDouble(chain, "riskScore", newRisk);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜SHADOW健康门：健康="
                            + percent(score)
                            + "，风险="
                            + percent(risk)
                            + "，pass="
                            + pass
                            + "，promoteEligible="
                            + promote
                            + "，说明=影子健康只辅助观察，不直接BET";

            putString(chain, "summaryText", old + add);
        }
    }

    private Models.ShadowHealth buildOneShadowHealth(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<Models.RuleHealth> ruleHealthList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            ShadowRuntime runtime) {
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

        Models.ShadowHealth h = new Models.ShadowHealth();

        ShadowParts sp =
                buildShadowParts(
                        c,
                        rankNo,
                        ruleHealthList,
                        thirdGuardList,
                        rankList,
                        teacherMap,
                        positionHealthMap,
                        chains,
                        runtime);

        double support = 0.0d;
        support += sp.candidateScore * 0.16d;
        support += sp.probEdgeScore * 0.10d;
        support += sp.causeScore * 0.12d;
        support += sp.ruleHealthScore * 0.13d;
        support += sp.thirdGuardScore * 0.12d;
        support += sp.rankScore * 0.10d;
        support += sp.teacherScore * 0.10d;
        support += sp.positionHealthScore * 0.08d;
        support += sp.sampleConfidence * 0.09d;
        support += sp.recentShadowScore * 0.06d;
        support += sp.isolationScore * 0.04d;

        double risk = 0.0d;
        risk += sp.candidateRisk * 0.12d;
        risk += sp.ruleHealthRisk * 0.12d;
        risk += sp.thirdGuardRisk * 0.12d;
        risk += sp.rankRisk * 0.10d;
        risk += sp.teacherRisk * 0.10d;
        risk += sp.positionRisk * 0.08d;
        risk += sp.weakSampleRisk * 0.10d;
        risk += sp.falseStrongRisk * 0.09d;
        risk += sp.loseStreakRisk * 0.08d;
        risk += sp.shadowVolatilityRisk * 0.06d;
        risk += sp.isolationRisk * 0.03d;

        double healthScore = clamp01(support - risk * 0.36d + 0.06d);
        double healthRisk = clamp01(risk + Math.max(0.0d, 0.44d - support) * 0.20d);

        int hardBlock = isHardBlock(sp, healthRisk) ? 1 : 0;
        int pass =
                hardBlock == 0 && healthScore >= PASS_SCORE_MIN && healthRisk <= PASS_RISK_MAX
                        ? 1
                        : 0;

        int warmupOk = sp.warmupReady >= 1.0d ? 1 : 0;
        int promoteEligible =
                hardBlock == 0
                                && pass == 1
                                && warmupOk == 1
                                && healthScore >= PROMOTE_SCORE_MIN
                                && healthRisk <= PROMOTE_RISK_MAX
                                && sp.isolationScore >= 0.88d
                        ? 1
                        : 0;

        String status =
                buildStatus(healthScore, healthRisk, pass, promoteEligible, hardBlock, warmupOk);
        String summary =
                buildSummary(
                        rankNo,
                        position,
                        n1,
                        n2,
                        n3,
                        healthScore,
                        healthRisk,
                        pass,
                        promoteEligible,
                        hardBlock,
                        status,
                        sp);

        putString(h, "shadowKey", buildShadowKey(targetIssue, position, mask));
        putString(h, "healthKey", buildShadowKey(targetIssue, position, mask));
        putString(h, "baseIssue", safe(baseIssue));
        putString(h, "targetIssue", safe(targetIssue));
        putLong(h, "createdAt", System.currentTimeMillis());

        putInt(h, "rankNo", rankNo);
        putInt(h, "rank", rankNo);
        putInt(h, "position", position);
        putInt(h, "n1", n1);
        putInt(h, "n2", n2);
        putInt(h, "n3", n3);
        putInt(h, "k3Mask", mask);
        putInt(h, "k3_mask", mask);

        putDouble(h, "shadowHealthScore", healthScore);
        putDouble(h, "healthScore", healthScore);
        putDouble(h, "shadowHealthRisk", healthRisk);
        putDouble(h, "riskScore", healthRisk);
        putInt(h, "shadowPass", pass);
        putInt(h, "pass", pass);
        putInt(h, "shadowPromoteEligible", promoteEligible);
        putInt(h, "promoteEligible", promoteEligible);
        putInt(h, "hardBlock", hardBlock);
        putInt(h, "warmupOk", warmupOk);

        putDouble(h, "candidateScore", sp.candidateScore);
        putDouble(h, "probEdgeScore", sp.probEdgeScore);
        putDouble(h, "causeScore", sp.causeScore);
        putDouble(h, "ruleHealthScore", sp.ruleHealthScore);
        putDouble(h, "ruleHealthRisk", sp.ruleHealthRisk);
        putDouble(h, "thirdGuardScore", sp.thirdGuardScore);
        putDouble(h, "thirdGuardRisk", sp.thirdGuardRisk);
        putDouble(h, "rankScore", sp.rankScore);
        putDouble(h, "rankRisk", sp.rankRisk);
        putDouble(h, "teacherScore", sp.teacherScore);
        putDouble(h, "teacherRisk", sp.teacherRisk);
        putDouble(h, "positionHealthScore", sp.positionHealthScore);
        putDouble(h, "positionRisk", sp.positionRisk);
        putDouble(h, "sampleConfidence", sp.sampleConfidence);
        putDouble(h, "recentShadowScore", sp.recentShadowScore);
        putDouble(h, "recentShadowRisk", sp.shadowVolatilityRisk);
        putDouble(h, "falseStrongRisk", sp.falseStrongRisk);
        putDouble(h, "weakSampleRisk", sp.weakSampleRisk);
        putDouble(h, "loseStreakRisk", sp.loseStreakRisk);
        putDouble(h, "isolationScore", sp.isolationScore);
        putDouble(h, "isolationRisk", sp.isolationRisk);

        putInt(h, "shadowSample", sp.shadowSample);
        putInt(h, "shadowHit", sp.shadowHit);
        putInt(h, "shadowMiss", sp.shadowMiss);
        putInt(h, "recentShadowSample", sp.recentShadowSample);
        putInt(h, "recentShadowHit", sp.recentShadowHit);
        putDouble(h, "shadowHitRate", sp.shadowHitRate);
        putDouble(h, "recentShadowHitRate", sp.recentShadowHitRate);

        putString(h, "status", status);
        putString(h, "level", status);
        putString(h, "summaryText", summary);
        putString(h, "reasonText", summary);

        return h;
    }

    private ShadowParts buildShadowParts(
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<Models.RuleHealth> ruleHealthList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionHealthMap,
            List<Models.CauseChain> chains,
            ShadowRuntime runtime) {
        ShadowParts sp = new ShadowParts();

        int position = getInt(c, "position", 0);
        int n1 = getInt(c, "n1", 0);
        int n2 = getInt(c, "n2", 0);
        int n3 = getInt(c, "n3", 0);
        int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

        sp.candidateScore = candidateScore(c);
        sp.candidateRisk = getDouble(c, "riskScore", 0.0d);
        sp.probEdgeScore = calcProbEdgeScore(c);
        sp.causeScore = getDouble(c, "causeScore", 0.0d);

        RuleBundle rb = readRuleBundle(position, mask, ruleHealthList);
        sp.ruleHealthScore = rb.health;
        sp.ruleHealthRisk = rb.risk;

        ThirdBundle gb = readThirdGuard(position, mask, thirdGuardList);
        sp.thirdGuardScore = gb.score;
        sp.thirdGuardRisk = gb.risk;
        sp.thirdGuardPass = gb.pass;
        sp.thirdGuardHard = gb.hard;

        RankBundle rk = readRank(position, mask, rankList);
        sp.rankScore = rk.score;
        sp.rankRisk = rk.risk;

        TeacherBundle tb = readTeacher(position, n1, n2, n3, teacherMap);
        sp.teacherScore = tb.score;
        sp.teacherRisk = tb.risk;
        sp.teacherConfidence = tb.confidence;
        sp.teacherSample = tb.sample;

        PositionBundle pb = readPosition(position, positionHealthMap);
        sp.positionHealthScore = pb.health;
        sp.positionRisk = pb.risk;

        ChainBundle cb = readChain(position, n1, n2, n3, chains);
        sp.chainScore = cb.score;
        sp.chainRisk = cb.risk;
        sp.chainSample = cb.sample;

        sp.shadowSample = readShadowInt(c, "shadowSample", "shadow_sample", tb.sample);
        sp.shadowHit = readShadowInt(c, "shadowHit", "shadow_hit", 0);
        sp.shadowMiss = readShadowInt(c, "shadowMiss", "shadow_miss", 0);
        sp.recentShadowSample =
                readShadowInt(
                        c,
                        "recentShadowSample",
                        "recent_shadow_sample",
                        Math.min(sp.shadowSample, 60));
        sp.recentShadowHit = readShadowInt(c, "recentShadowHit", "recent_shadow_hit", 0);

        if (sp.shadowSample > 0) {
            sp.shadowHitRate = safeRate(sp.shadowHit, sp.shadowSample);
        } else {
            sp.shadowHitRate = estimateShadowHitRate(c, tb, rb, gb, rk, pb, cb);
        }

        if (sp.recentShadowSample > 0) {
            sp.recentShadowHitRate = safeRate(sp.recentShadowHit, sp.recentShadowSample);
        } else {
            sp.recentShadowHitRate = sp.shadowHitRate;
        }

        sp.sampleConfidence =
                calcSampleConfidence(
                        sp.shadowSample, sp.teacherSample, sp.chainSample, rb.ruleCount);
        sp.recentShadowScore =
                clamp01(
                        sp.shadowHitRate * 0.48d
                                + sp.recentShadowHitRate * 0.32d
                                + sp.sampleConfidence * 0.20d);

        sp.weakSampleRisk = calcWeakSampleRisk(c, sp);
        sp.falseStrongRisk = calcFalseStrongRisk(c, sp);
        sp.shadowVolatilityRisk = calcShadowVolatilityRisk(sp);
        sp.loseStreakRisk = runtime == null ? 0.0d : runtime.loseStreakRisk;
        sp.warmupReady = calcWarmupReady(sp.shadowSample, sp.teacherSample, runtime);
        sp.isolationScore = calcIsolationScore(c);
        sp.isolationRisk = clamp01(1.0d - sp.isolationScore);

        return sp;
    }

    private double estimateShadowHitRate(
            Models.CandidateK3 c,
            TeacherBundle tb,
            RuleBundle rb,
            ThirdBundle gb,
            RankBundle rk,
            PositionBundle pb,
            ChainBundle cb) {
        double finalP = getDouble(c, "finalP", 0.0d);

        if (finalP <= 0.0d) {
            finalP = 0.30d;
        }

        double estimate = 0.0d;
        estimate += finalP * 0.30d;
        estimate += tb.score * 0.16d;
        estimate += rb.health * 0.14d;
        estimate += gb.score * 0.12d;
        estimate += rk.score * 0.10d;
        estimate += pb.health * 0.08d;
        estimate += cb.score * 0.10d;

        estimate -= tb.risk * 0.06d;
        estimate -= rb.risk * 0.05d;
        estimate -= gb.risk * 0.05d;

        return clamp01(estimate);
    }

    private double calcSampleConfidence(
            int shadowSample, int teacherSample, int chainSample, int ruleCount) {
        double shadow = confidenceFromSample(shadowSample, readWarmupIssues());
        double teacher = confidenceFromSample(teacherSample, 90);
        double chain = confidenceFromSample(chainSample, 30);
        double rule = confidenceFromSample(ruleCount, 10);

        return clamp01(shadow * 0.40d + teacher * 0.24d + chain * 0.18d + rule * 0.18d);
    }

    private double calcWeakSampleRisk(Models.CandidateK3 c, ShadowParts sp) {
        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double risk = 0.0d;

        if (sp.sampleConfidence < 0.22d) {
            risk += 0.26d;
        } else if (sp.sampleConfidence < 0.40d) {
            risk += 0.14d;
        }

        if (finalP >= 0.39d && sp.sampleConfidence < 0.35d) {
            risk += 0.16d;
        }

        if (edge > 0.018d && sp.shadowSample < readWarmupIssues()) {
            risk += 0.12d;
        }

        if (sp.teacherSample < 20 && sp.teacherScore >= 0.56d) {
            risk += 0.10d;
        }

        return clamp01(risk);
    }

    private double calcFalseStrongRisk(Models.CandidateK3 c, ShadowParts sp) {
        double risk = 0.0d;

        risk += getDouble(c, "falseStrongRisk", 0.0d) * 0.22d;
        risk += getDouble(c, "falseChampionRisk", 0.0d) * 0.16d;
        risk += sp.thirdGuardRisk * 0.14d;
        risk += sp.ruleHealthRisk * 0.12d;
        risk += sp.teacherRisk * 0.10d;
        risk += sp.rankRisk * 0.10d;
        risk += sp.positionRisk * 0.08d;

        if (sp.candidateScore >= 0.58d && sp.causeScore < 0.42d) {
            risk += 0.12d;
        }

        if (sp.shadowHitRate < 0.30d && sp.shadowSample >= 30) {
            risk += 0.12d;
        }

        return clamp01(risk);
    }

    private double calcShadowVolatilityRisk(ShadowParts sp) {
        double risk = 0.0d;

        if (sp.shadowSample >= 30) {
            double diff = Math.abs(sp.shadowHitRate - sp.recentShadowHitRate);

            if (diff > 0.18d) {
                risk += clamp01(diff * 1.20d) * 0.28d;
            }

            if (sp.recentShadowHitRate < 0.28d) {
                risk += 0.18d;
            }
        }

        if (sp.shadowMiss >= 4 && sp.shadowHit <= 0) {
            risk += 0.16d;
        }

        return clamp01(risk);
    }

    private double calcWarmupReady(int shadowSample, int teacherSample, ShadowRuntime runtime) {
        int need = readWarmupIssues();

        if (need <= 0) {
            need = 30;
        }

        double sampleReady = confidenceFromSample(shadowSample, need);
        double teacherReady = confidenceFromSample(teacherSample, 60);
        double runtimeReady =
                runtime == null ? 0.0d : confidenceFromSample(runtime.checkedTrades, 30);

        return clamp01(sampleReady * 0.60d + teacherReady * 0.24d + runtimeReady * 0.16d);
    }

    private double calcIsolationScore(Models.CandidateK3 c) {
        String action = getString(c, "action", "");
        String statScope = getString(c, "statScope", getString(c, "stat_scope", ""));
        String sourceType = getString(c, "sourceType", getString(c, "source_type", ""));
        int realFlag = getInt(c, "isRealtimeReal", getInt(c, "is_realtime_real", 0));

        double score = 1.0d;

        if ("BET".equals(action) && !"REAL_TRADE".equals(statScope)) {
            score -= 0.18d;
        }

        if ("BET".equals(action) && realFlag != 1) {
            score -= 0.12d;
        }

        if ("SHADOW".equals(action) || "HOLD".equals(action) || action.length() == 0) {
            score = Math.max(score, 0.96d);
        }

        if ("BACKGROUND".equals(sourceType) || "TEACHER".equals(sourceType)) {
            score -= 0.08d;
        }

        return clamp01(score);
    }

    private boolean isHardBlock(ShadowParts sp, double risk) {
        if (risk >= HARD_RISK) {
            return true;
        }

        if (sp == null) {
            return false;
        }

        if (sp.isolationScore < 0.72d) {
            return true;
        }

        if (sp.thirdGuardHard == 1) {
            return true;
        }

        if (sp.falseStrongRisk >= 0.78d) {
            return true;
        }

        if (sp.weakSampleRisk >= 0.72d && sp.candidateScore < 0.52d) {
            return true;
        }

        if (sp.loseStreakRisk >= 0.72d && sp.shadowHitRate < 0.34d) {
            return true;
        }

        return false;
    }

    private ShadowRuntime buildRuntime(List<Models.Trade> recentTrades) {
        ShadowRuntime rt = new ShadowRuntime();

        if (recentTrades == null || recentTrades.size() == 0) {
            return rt;
        }

        int loseStreak = 0;
        int win = 0;
        int miss = 0;
        int checked = 0;

        for (int i = recentTrades.size() - 1; i >= 0; i--) {
            Models.Trade t = recentTrades.get(i);

            if (t == null) {
                continue;
            }

            String result = getString(t, "result", "");
            String action = getString(t, "action", "");
            String statScope = getString(t, "statScope", getString(t, "stat_scope", ""));

            if (!"BET".equals(action)
                    && !"SHADOW".equals(action)
                    && !"HOLD".equals(action)
                    && action.length() > 0) {
                continue;
            }

            checked++;

            if ("WIN".equals(result) || "HIT".equals(result) || "命中".equals(result)) {
                win++;

                if (loseStreak <= 0) {
                    break;
                }
            } else if ("MISS".equals(result) || "LOSE".equals(result) || "未中".equals(result)) {
                miss++;
                loseStreak++;
            }

            if ("REAL_TRADE".equals(statScope)) {
                rt.realTradeSamples++;
            }

            if (checked >= 60) {
                break;
            }
        }

        rt.checkedTrades = checked;
        rt.recentWin = win;
        rt.recentMiss = miss;
        rt.loseStreak = loseStreak;

        if (loseStreak <= 0) {
            rt.loseStreakRisk = 0.0d;
        } else if (loseStreak >= 5) {
            rt.loseStreakRisk = 0.76d;
        } else {
            rt.loseStreakRisk = clamp01(((double) loseStreak) / 7.0d);
        }

        return rt;
    }

    private RuleBundle readRuleBundle(int position, int mask, ArrayList<Models.RuleHealth> list) {
        RuleBundle b = new RuleBundle();

        if (list == null || list.size() == 0) {
            b.health = 0.0d;
            b.risk = 0.0d;
            return b;
        }

        double health = 0.0d;
        double risk = 0.0d;
        int pass = 0;
        int hard = 0;
        int count = 0;

        for (int i = 0; i < list.size(); i++) {
            Models.RuleHealth r = list.get(i);

            if (r == null) {
                continue;
            }

            int p = getInt(r, "position", 0);
            int m = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            if (p != position || m != mask) {
                continue;
            }

            health += getDouble(r, "ruleHealthScore", getDouble(r, "healthScore", 0.0d));
            risk += getDouble(r, "ruleRiskScore", getDouble(r, "riskScore", 0.0d));

            if (getInt(r, "rulePass", getInt(r, "pass", 0)) == 1) {
                pass++;
            }

            if (getInt(r, "hardBlock", 0) == 1) {
                hard++;
            }

            count++;
        }

        if (count <= 0) {
            b.health = 0.0d;
            b.risk = 0.0d;
            return b;
        }

        b.ruleCount = count;
        b.passCount = pass;
        b.hardCount = hard;
        b.health = clamp01(health / count);
        b.risk = clamp01(risk / count + (hard > 0 ? 0.12d : 0.0d));

        return b;
    }

    private ThirdBundle readThirdGuard(
            int position, int mask, ArrayList<Models.ThirdGuardStat> list) {
        ThirdBundle b = new ThirdBundle();

        if (list == null) {
            return b;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ThirdGuardStat g = list.get(i);

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

    private RankBundle readRank(int position, int mask, ArrayList<Models.RankCounterfactual> list) {
        RankBundle b = new RankBundle();

        if (list == null) {
            return b;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.RankCounterfactual r = list.get(i);

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
                                        + getDouble(r, "nearConflictRisk", 0.0d) * 0.24d
                                        + getDouble(r, "falseChampionRisk", 0.0d) * 0.26d
                                        + getDouble(r, "samePositionReplaceRisk", 0.0d) * 0.10d
                                        + getDouble(r, "crossPositionReplaceRisk", 0.0d) * 0.10d);
                return b;
            }
        }

        return b;
    }

    private TeacherBundle readTeacher(
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

    private PositionBundle readPosition(int position, Map<Integer, Models.PositionHealth> map) {
        PositionBundle b = new PositionBundle();

        if (map == null) {
            return b;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return b;
        }

        b.health = getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
        b.risk =
                getDouble(
                        h,
                        "riskScore",
                        getDouble(h, "positionRiskScore", getDouble(h, "wrongRisk", 0.0d)));

        return b;
    }

    private ChainBundle readChain(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        ChainBundle b = new ChainBundle();

        if (chains == null) {
            return b;
        }

        double score = 0.0d;
        double risk = 0.0d;
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

            score += getDouble(c, "finalCauseScore", getDouble(c, "causeScore", 0.0d));
            risk += getDouble(c, "riskScore", 0.0d);
            used++;
        }

        if (used > 0) {
            b.score = clamp01(score / used);
            b.risk = clamp01(risk / used);
            b.sample = used;
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
                        double sa = shadowCandidateScore(a);
                        double sb = shadowCandidateScore(b);

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

    private void applyOneToCandidate(Models.CandidateK3 c, Models.ShadowHealth h) {
        double score = getDouble(h, "shadowHealthScore", getDouble(h, "healthScore", 0.0d));
        double risk = getDouble(h, "shadowHealthRisk", getDouble(h, "riskScore", 0.0d));
        int pass = getInt(h, "shadowPass", getInt(h, "pass", 0));
        int promote = getInt(h, "shadowPromoteEligible", getInt(h, "promoteEligible", 0));
        int hard = getInt(h, "hardBlock", 0);

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;

        if (pass == 1) {
            newCause = clamp01(oldCause * 0.92d + score * 0.08d);
        } else {
            newCause = clamp01(oldCause * 0.95d - risk * 0.040d);
        }

        double newRisk = clamp01(Math.max(oldRisk, risk));

        putDouble(c, "shadowHealthScore", score);
        putDouble(c, "shadowHealthRisk", risk);
        putInt(c, "shadowPass", pass);
        putInt(c, "shadowPromoteEligible", promote);
        putInt(c, "shadowHardBlock", hard);
        putString(c, "shadowHealthStatus", getString(h, "status", ""));
        putString(c, "shadowHealthSummary", getString(h, "summaryText", ""));

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendReason(c, getString(h, "summaryText", ""));
    }

    private void rerankByShadowHealth(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = shadowCandidateScore(a);
                        double sb = shadowCandidateScore(b);

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

    private static double shadowCandidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double finalP = getDouble(c, "finalP", 0.0d);
        double edge = getDouble(c, "edge", 0.0d);
        double cause = getDouble(c, "causeScore", 0.0d);
        double rule = getDouble(c, "ruleHealthScore", 0.0d);
        double guard = getDouble(c, "thirdGuardScore", 0.0d);
        double shadow = getDouble(c, "shadowHealthScore", 0.0d);
        double teacher = getDouble(c, "teacherNumberScore", 0.0d);
        double rank = getDouble(c, "counterfactualScore", 0.0d);
        double risk = getDouble(c, "riskScore", 0.0d);
        double shadowRisk = getDouble(c, "shadowHealthRisk", 0.0d);

        double score = 0.0d;

        score += finalP * 0.20d;
        score += posPart(edge * 4.0d) * 0.08d;
        score += cause * 0.18d;
        score += rule * 0.12d;
        score += guard * 0.12d;
        score += shadow * 0.12d;
        score += teacher * 0.08d;
        score += rank * 0.08d;

        score -= risk * 0.10d;
        score -= shadowRisk * 0.08d;

        if (getInt(c, "shadowHardBlock", 0) == 1) {
            score -= 0.070d;
        }

        if (getInt(c, "shadowPass", 0) == 1) {
            score += 0.012d;
        }

        return clamp01(score);
    }

    private double calcProbEdgeScore(Models.CandidateK3 c) {
        double finalP = getDouble(c, "finalP", 0.0d);
        double gate = getDouble(c, "dynamicGate", getDouble(c, "gate", 0.0d));
        double edge = getDouble(c, "edge", finalP - gate);

        double edgeScore = clamp01((edge + 0.030d) / 0.090d);
        double pScore = clamp01((finalP - 0.30d) / 0.18d);

        return clamp01(edgeScore * 0.58d + pScore * 0.42d);
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
        double teacher =
                getDouble(c, "teacherNumberScore", getDouble(c, "teacherComboScore", 0.0d));
        double rank = getDouble(c, "counterfactualScore", 0.0d);
        double position =
                getDouble(c, "positionCauseScore", getDouble(c, "positionHealthScore", 0.0d));
        double risk = getDouble(c, "riskScore", 0.0d);

        double score = 0.0d;

        score += finalP * 0.22d;
        score += posPart(edge * 4.0d) * 0.08d;
        score += cause * 0.22d;
        score += rule * 0.13d;
        score += guard * 0.12d;
        score += teacher * 0.09d;
        score += rank * 0.08d;
        score += position * 0.06d;

        score -= risk * 0.12d;

        return clamp01(score);
    }

    private Models.ShadowHealth findBestHealthForNumber(
            ArrayList<Models.ShadowHealth> list, int position, int number) {
        Models.ShadowHealth best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < list.size(); i++) {
            Models.ShadowHealth h = list.get(i);

            if (h == null) {
                continue;
            }

            int p = getInt(h, "position", 0);

            if (p != position) {
                continue;
            }

            int n1 = getInt(h, "n1", 0);
            int n2 = getInt(h, "n2", 0);
            int n3 = getInt(h, "n3", 0);

            if (number != n1 && number != n2 && number != n3) {
                continue;
            }

            double score = getDouble(h, "shadowHealthScore", getDouble(h, "healthScore", 0.0d));

            if (best == null || score > bestScore) {
                best = h;
                bestScore = score;
            }
        }

        return best;
    }

    private String buildSummary(
            int rankNo,
            int position,
            int n1,
            int n2,
            int n3,
            double score,
            double risk,
            int pass,
            int promote,
            int hard,
            String status,
            ShadowParts sp) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20影子健康门｜rank=");
        sb.append(rankNo);
        sb.append("｜位置=");
        sb.append(position);
        sb.append("｜K3=");
        sb.append(n1);
        sb.append(",");
        sb.append(n2);
        sb.append(",");
        sb.append(n3);
        sb.append("｜健康=");
        sb.append(percent(score));
        sb.append("｜风险=");
        sb.append(percent(risk));
        sb.append("｜pass=");
        sb.append(pass);
        sb.append("｜promoteEligible=");
        sb.append(promote);
        sb.append("｜hard=");
        sb.append(hard);
        sb.append("｜状态=");
        sb.append(status);
        sb.append("｜候选=");
        sb.append(percent(sp.candidateScore));
        sb.append("｜规则=");
        sb.append(percent(sp.ruleHealthScore));
        sb.append("｜第三防线=");
        sb.append(percent(sp.thirdGuardScore));
        sb.append("｜反事实=");
        sb.append(percent(sp.rankScore));
        sb.append("｜教师=");
        sb.append(percent(sp.teacherScore));
        sb.append("｜位置=");
        sb.append(percent(sp.positionHealthScore));
        sb.append("｜样本置信=");
        sb.append(percent(sp.sampleConfidence));
        sb.append("｜影子命中=");
        sb.append(percent(sp.shadowHitRate));
        sb.append("｜近期影子=");
        sb.append(percent(sp.recentShadowHitRate));
        sb.append("｜弱样本风险=");
        sb.append(percent(sp.weakSampleRisk));
        sb.append("｜伪强=");
        sb.append(percent(sp.falseStrongRisk));
        sb.append("｜连挂=");
        sb.append(percent(sp.loseStreakRisk));
        sb.append("｜隔离=");
        sb.append(percent(sp.isolationScore));
        sb.append("｜说明=SHADOW健康门只做观察层健康审计，不创建BET，不写真实账本，不污染真实校准");

        return sb.toString();
    }

    private String buildStatus(
            double score, double risk, int pass, int promote, int hard, int warmupOk) {
        if (hard == 1) {
            return "HARD_RISK";
        }

        if (warmupOk == 0) {
            if (score >= PASS_SCORE_MIN && risk <= PASS_RISK_MAX) {
                return "WARMUP_SHADOW";
            }

            return "WARMUP_WEAK";
        }

        if (promote == 1) {
            return "PROMOTE_READY";
        }

        if (pass == 1) {
            return "SHADOW_PASS";
        }

        if (risk >= 0.70d) {
            return "SHADOW_RISK";
        }

        if (score >= 0.46d) {
            return "SHADOW_WATCH";
        }

        return "SHADOW_WEAK";
    }

    private String buildShadowKey(String targetIssue, int position, int mask) {
        return safe(targetIssue) + "|P" + position + "|K3_" + mask;
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private int readShadowInt(Object obj, String a, String b, int fallback) {
        int v = getInt(obj, a, -999999);

        if (v != -999999) {
            return v;
        }

        return getInt(obj, b, fallback);
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

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
    }

    private double safeRate(int hit, int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        return clamp01(((double) hit) / ((double) sample));
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

    private double clampSigned(double v) {
        if (v < -1.0d) {
            return -1.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private static double clamp01(double v) {
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

    private static final class ShadowParts {
        double candidateScore;
        double candidateRisk;
        double probEdgeScore;
        double causeScore;

        double ruleHealthScore;
        double ruleHealthRisk;

        double thirdGuardScore;
        double thirdGuardRisk;
        int thirdGuardPass;
        int thirdGuardHard;

        double rankScore;
        double rankRisk;

        double teacherScore;
        double teacherRisk;
        double teacherConfidence;
        int teacherSample;

        double positionHealthScore;
        double positionRisk;

        double chainScore;
        double chainRisk;
        int chainSample;

        int shadowSample;
        int shadowHit;
        int shadowMiss;
        int recentShadowSample;
        int recentShadowHit;

        double shadowHitRate;
        double recentShadowHitRate;

        double sampleConfidence;
        double recentShadowScore;
        double weakSampleRisk;
        double falseStrongRisk;
        double shadowVolatilityRisk;
        double loseStreakRisk;
        double warmupReady;
        double isolationScore;
        double isolationRisk;
    }

    private static final class RuleBundle {
        int ruleCount;
        int passCount;
        int hardCount;
        double health;
        double risk;
    }

    private static final class ThirdBundle {
        double score;
        double risk;
        int pass;
        int hard;
    }

    private static final class RankBundle {
        double score;
        double risk;
    }

    private static final class TeacherBundle {
        int used;
        int sample;
        double score;
        double risk;
        double confidence;
    }

    private static final class PositionBundle {
        double health;
        double risk;
    }

    private static final class ChainBundle {
        int sample;
        double score;
        double risk;
    }

    private static final class ShadowRuntime {
        int checkedTrades;
        int recentWin;
        int recentMiss;
        int loseStreak;
        int realTradeSamples;
        double loseStreakRisk;
    }
}
