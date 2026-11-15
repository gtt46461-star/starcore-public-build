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
 * SeedCauseExpansionEngine.java
 *
 * <p>第15/25 新增核心引擎。
 *
 * <p>作用： 1. 从健康候选中提取“种子因子”。 2. 扩展同状态、同位置、相似号码、相似K3结构、相似回归、相似转移、相似冷热形态。 3. 给 CandidateK3 回写：
 * seedScore / seedRisk / seedExpandScore / seedSimilarity / seedCauseSummary。 4. 给 CauseChain 回写：
 * seedCause / seedScore / seedRisk / finalCauseScore / riskScore。
 *
 * <p>红线： - 不创建BET。 - 不结算。 - 不写真实账本。 - 不写真实校准。 - 不把SHADOW当真实交易。 - 不降低最终门槛。 - 不绕过 DecisionGateEngine。
 */
public final class SeedCauseExpansionEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final double SEED_STRONG_LINE = 0.66d;
    private static final double SEED_USABLE_LINE = 0.52d;
    private static final double SEED_RISK_LINE = 0.64d;
    private static final double SEED_HARD_RISK_LINE = 0.78d;

    public SeedCauseExpansionEngine() {}

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
        ArrayList<SeedExpansionRecord> records =
                buildSeedExpansions(baseIssue, targetIssue, bridge);
        applyToCandidates(bridge, records);
        LegacyCandidateBridge.copyBack(bridge, out);
        try {
            if (store != null) {
                store.setState("v43_seed_expansion_apply_count", String.valueOf(out.size()));
                store.setState(
                        "v43_seed_expansion_record_count",
                        String.valueOf(records == null ? 0 : records.size()));
                store.setState("v43_seed_expansion_status", "APPLIED_TYPED_BRIDGE");
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

    public ArrayList<SeedExpansionRecord> buildSeedExpansions(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.FalseStrongRecord> freezeList) {
        ArrayList<Models.CandidateK3> list = normalizeCandidates(candidates);
        ArrayList<SeedExpansionRecord> out = new ArrayList<SeedExpansionRecord>();

        if (list.size() == 0) {
            return out;
        }

        ArrayList<SeedNode> seeds =
                extractSeedNodes(
                        targetIssue,
                        list,
                        featureMap,
                        teacherMap,
                        positionMap,
                        chains,
                        shadowList,
                        ruleList,
                        thirdGuardList,
                        rankList,
                        freezeList);

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateK3 c = list.get(i);

            if (c == null) {
                continue;
            }

            SeedExpansionRecord r =
                    buildOneExpansion(
                            baseIssue,
                            targetIssue,
                            c,
                            i + 1,
                            seeds,
                            featureMap,
                            teacherMap,
                            positionMap,
                            chains,
                            shadowList,
                            ruleList,
                            thirdGuardList,
                            rankList,
                            freezeList);

            if (r != null) {
                out.add(r);
            }
        }

        applyToCandidates(list, out);
        applyToCauseChains(chains, out);
        rerankBySeed(candidates);

        return out;
    }

    public ArrayList<SeedExpansionRecord> buildSeedExpansions(
            String baseIssue, String targetIssue, List<Models.CandidateK3> candidates) {
        return buildSeedExpansions(
                baseIssue,
                targetIssue,
                candidates,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    public ArrayList<SeedExpansionRecord> buildSeedExpansions(List<Models.CandidateK3> candidates) {
        return buildSeedExpansions("", "", candidates);
    }

    public SeedExpansionRecord buildChampionSeedExpansion(
            String baseIssue,
            String targetIssue,
            List<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.FalseStrongRecord> freezeList) {
        ArrayList<SeedExpansionRecord> list =
                buildSeedExpansions(
                        baseIssue,
                        targetIssue,
                        candidates,
                        featureMap,
                        teacherMap,
                        positionMap,
                        chains,
                        shadowList,
                        ruleList,
                        thirdGuardList,
                        rankList,
                        freezeList);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public void applyToCandidates(
            List<Models.CandidateK3> candidates, ArrayList<SeedExpansionRecord> records) {
        if (candidates == null
                || records == null
                || candidates.size() == 0
                || records.size() == 0) {
            return;
        }

        LinkedHashMap<String, SeedExpansionRecord> map =
                new LinkedHashMap<String, SeedExpansionRecord>();

        for (int i = 0; i < records.size(); i++) {
            SeedExpansionRecord r = records.get(i);

            if (r == null) {
                continue;
            }

            map.put(candidateKey(r.position, r.k3Mask), r);
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            int p = getInt(c, "position", 0);
            int mask = getInt(c, "k3Mask", getInt(c, "k3_mask", 0));

            SeedExpansionRecord r = map.get(candidateKey(p, mask));

            if (r == null) {
                continue;
            }

            applyOneToCandidate(c, r);
        }
    }

    public void applyToCauseChains(
            List<Models.CauseChain> chains, ArrayList<SeedExpansionRecord> records) {
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

            SeedExpansionRecord best = findBestRecordForNumber(records, position, number);

            if (best == null) {
                continue;
            }

            double seedCause = clampSigned((best.seedExpandScore - best.seedRisk) * 1.10d);
            double oldFinal =
                    getDouble(chain, "finalCauseScore", getDouble(chain, "causeScore", 0.0d));
            double oldRisk = getDouble(chain, "riskScore", 0.0d);

            double newFinal;

            if (best.seedPass == 1) {
                newFinal = clamp01(oldFinal + posPart(seedCause) * 0.032d);
            } else {
                newFinal = clamp01(oldFinal - Math.max(0.010d, best.seedRisk * 0.030d));
            }

            double newRisk = clamp01(Math.max(oldRisk, best.seedRisk));

            putDouble(chain, "seedCause", seedCause);
            putDouble(chain, "seedScore", best.seedScore);
            putDouble(chain, "seedRisk", best.seedRisk);
            putDouble(chain, "seedExpandScore", best.seedExpandScore);
            putDouble(chain, "seedSimilarity", best.seedSimilarity);
            putInt(chain, "seedPass", best.seedPass);
            putDouble(chain, "finalCauseScore", newFinal);
            putDouble(chain, "causeScore", newFinal);
            putDouble(chain, "riskScore", newRisk);

            String old = getString(chain, "summaryText", "");
            String add =
                    "｜种子因子扩展：seed="
                            + percent(best.seedScore)
                            + "，expand="
                            + percent(best.seedExpandScore)
                            + "，similar="
                            + percent(best.seedSimilarity)
                            + "，risk="
                            + percent(best.seedRisk)
                            + "，pass="
                            + best.seedPass
                            + "，说明=种子扩展只增强因链证据，不直接BET";

            putString(chain, "summaryText", old + add);
        }
    }

    private ArrayList<SeedNode> extractSeedNodes(
            String targetIssue,
            ArrayList<Models.CandidateK3> candidates,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.FalseStrongRecord> freezeList) {
        ArrayList<SeedNode> seeds = new ArrayList<SeedNode>();

        if (candidates == null || candidates.size() == 0) {
            return seeds;
        }

        int max = Math.min(candidates.size(), readMaxSeedSource());

        for (int i = 0; i < max; i++) {
            Models.CandidateK3 c = candidates.get(i);

            if (c == null) {
                continue;
            }

            SeedNode seed =
                    buildSeedNode(
                            targetIssue,
                            c,
                            i + 1,
                            featureMap,
                            teacherMap,
                            positionMap,
                            chains,
                            shadowList,
                            ruleList,
                            thirdGuardList,
                            rankList,
                            freezeList);

            if (seed == null) {
                continue;
            }

            if (seed.seedScore >= SEED_USABLE_LINE && seed.seedRisk < SEED_RISK_LINE) {
                seeds.add(seed);
            }
        }

        Collections.sort(
                seeds,
                new Comparator<SeedNode>() {
                    public int compare(SeedNode a, SeedNode b) {
                        double sa = a == null ? 0.0d : a.seedScore - a.seedRisk * 0.35d;
                        double sb = b == null ? 0.0d : b.seedScore - b.seedRisk * 0.35d;

                        if (sa > sb) {
                            return -1;
                        }

                        if (sa < sb) {
                            return 1;
                        }

                        return 0;
                    }
                });

        return seeds;
    }

    private SeedNode buildSeedNode(
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.FalseStrongRecord> freezeList) {
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

        SeedNode s = new SeedNode();

        s.targetIssue = safe(targetIssue);
        s.position = position;
        s.n1 = n1;
        s.n2 = n2;
        s.n3 = n3;
        s.k3Mask = mask;
        s.rankNo = rankNo;

        s.candidateScore = candidateScore(c);
        s.finalP = getDouble(c, "finalP", 0.0d);
        s.edge = getDouble(c, "edge", 0.0d);
        s.causeScore = getDouble(c, "causeScore", 0.0d);
        s.riskScore = getDouble(c, "riskScore", 0.0d);

        s.teacherScore = calcTeacherScore(position, n1, n2, n3, teacherMap);
        s.teacherRisk = calcTeacherRisk(position, n1, n2, n3, teacherMap);
        s.positionHealth = calcPositionHealth(position, positionMap);
        s.positionRisk = calcPositionRisk(position, positionMap);
        s.chainScore = calcChainScore(position, n1, n2, n3, chains);
        s.chainRisk = calcChainRisk(position, n1, n2, n3, chains);
        s.ruleHealth = calcRuleHealth(position, mask, ruleList);
        s.ruleRisk = calcRuleRisk(position, mask, ruleList);
        s.thirdGuardScore = calcThirdGuardScore(position, mask, thirdGuardList);
        s.thirdGuardRisk = calcThirdGuardRisk(position, mask, thirdGuardList);
        s.rankScore = calcRankScore(position, mask, rankList);
        s.rankRisk = calcRankRisk(position, mask, rankList);
        s.shadowHealth = calcShadowHealth(position, mask, shadowList);
        s.shadowRisk = calcShadowRisk(position, mask, shadowList);
        s.freezeRisk = calcFreezeRisk(position, mask, freezeList);

        FeaturePack fp = calcFeaturePack(position, n1, n2, n3, featureMap);
        s.trendSeed = fp.trend;
        s.gapSeed = fp.gap;
        s.hotColdSeed = fp.hotCold;
        s.transitionSeed = fp.transition;
        s.recoverySeed = fp.recovery;
        s.featureRisk = fp.risk;
        s.featureKey = fp.featureKey;

        s.shapeKey = buildShapeKey(position, n1, n2, n3, fp);
        s.stateKey = buildStateKey(position, n1, n2, n3, fp, s);
        s.sameClassKey = buildSameClassKey(position, fp, s);

        s.seedRisk =
                clamp01(
                        s.riskScore * 0.16d
                                + s.teacherRisk * 0.10d
                                + s.positionRisk * 0.08d
                                + s.chainRisk * 0.10d
                                + s.ruleRisk * 0.10d
                                + s.thirdGuardRisk * 0.10d
                                + s.rankRisk * 0.10d
                                + s.shadowRisk * 0.08d
                                + s.freezeRisk * 0.12d
                                + s.featureRisk * 0.06d);

        s.seedScore =
                clamp01(
                        s.candidateScore * 0.15d
                                + s.finalP * 0.09d
                                + posPart(s.edge * 4.0d) * 0.05d
                                + s.causeScore * 0.14d
                                + s.teacherScore * 0.09d
                                + s.positionHealth * 0.07d
                                + s.chainScore * 0.10d
                                + s.ruleHealth * 0.09d
                                + s.thirdGuardScore * 0.08d
                                + s.rankScore * 0.07d
                                + s.shadowHealth * 0.07d
                                + s.trendSeed * 0.04d
                                + s.gapSeed * 0.04d
                                + s.transitionSeed * 0.04d
                                + s.recoverySeed * 0.04d
                                - s.seedRisk * 0.18d);

        s.seedLevel = seedLevel(s.seedScore, s.seedRisk);
        s.seedKey = buildSeedKey(targetIssue, position, mask, s.seedLevel);
        s.summary = buildSeedNodeSummary(s);

        return s;
    }

    private SeedExpansionRecord buildOneExpansion(
            String baseIssue,
            String targetIssue,
            Models.CandidateK3 c,
            int rankNo,
            ArrayList<SeedNode> seeds,
            Map<String, Models.PreDrawFeature> featureMap,
            Map<String, Models.NumberTeacherCell> teacherMap,
            Map<Integer, Models.PositionHealth> positionMap,
            List<Models.CauseChain> chains,
            ArrayList<Models.ShadowHealth> shadowList,
            ArrayList<Models.RuleHealth> ruleList,
            ArrayList<Models.ThirdGuardStat> thirdGuardList,
            ArrayList<Models.RankCounterfactual> rankList,
            ArrayList<Models.FalseStrongRecord> freezeList) {
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

        SeedNode self =
                buildSeedNode(
                        targetIssue,
                        c,
                        rankNo,
                        featureMap,
                        teacherMap,
                        positionMap,
                        chains,
                        shadowList,
                        ruleList,
                        thirdGuardList,
                        rankList,
                        freezeList);

        if (self == null) {
            return null;
        }

        SimilarityPack sim = calcSimilarityPack(self, seeds);

        double expandSupport =
                clamp01(
                        self.seedScore * 0.38d
                                + sim.bestSimilarity * 0.18d
                                + sim.samePositionSupport * 0.10d
                                + sim.sameStateSupport * 0.10d
                                + sim.sameShapeSupport * 0.08d
                                + sim.numberOverlapSupport * 0.08d
                                + sim.featureSimilaritySupport * 0.08d);

        double expandRisk =
                clamp01(
                        self.seedRisk * 0.46d
                                + sim.conflictRisk * 0.22d
                                + sim.falseSimilarRisk * 0.18d
                                + sim.lowSeedRisk * 0.14d);

        double expandScore = clamp01(expandSupport - expandRisk * 0.36d + 0.05d);

        int pass = expandScore >= SEED_USABLE_LINE && expandRisk < SEED_RISK_LINE ? 1 : 0;
        int strong = expandScore >= SEED_STRONG_LINE && expandRisk < 0.46d ? 1 : 0;
        int hardRisk = expandRisk >= SEED_HARD_RISK_LINE ? 1 : 0;

        SeedExpansionRecord r = new SeedExpansionRecord();

        r.recordKey = buildExpansionKey(targetIssue, position, mask);
        r.baseIssue = safe(baseIssue);
        r.targetIssue = safe(targetIssue);
        r.createdAt = System.currentTimeMillis();

        r.rankNo = rankNo;
        r.position = position;
        r.n1 = n1;
        r.n2 = n2;
        r.n3 = n3;
        r.k3Mask = mask;

        r.seedKey = self.seedKey;
        r.stateKey = self.stateKey;
        r.shapeKey = self.shapeKey;
        r.sameClassKey = self.sameClassKey;
        r.seedLevel = self.seedLevel;

        r.seedScore = self.seedScore;
        r.seedRisk = expandRisk;
        r.selfSeedRisk = self.seedRisk;
        r.seedExpandScore = expandScore;
        r.seedExpandSupport = expandSupport;
        r.seedSimilarity = sim.bestSimilarity;
        r.seedConflictRisk = sim.conflictRisk;
        r.falseSimilarRisk = sim.falseSimilarRisk;

        r.samePositionSupport = sim.samePositionSupport;
        r.sameStateSupport = sim.sameStateSupport;
        r.sameShapeSupport = sim.sameShapeSupport;
        r.numberOverlapSupport = sim.numberOverlapSupport;
        r.featureSimilaritySupport = sim.featureSimilaritySupport;

        r.seedPass = pass;
        r.seedStrong = strong;
        r.seedHardRisk = hardRisk;
        r.seedSourceCount = sim.seedCount;
        r.bestSeedKey = sim.bestSeedKey;
        r.bestSeedSummary = sim.bestSeedSummary;
        r.summaryText = buildExpansionSummary(r, self, sim);

        return r;
    }

    private SimilarityPack calcSimilarityPack(SeedNode self, ArrayList<SeedNode> seeds) {
        SimilarityPack p = new SimilarityPack();

        if (self == null || seeds == null || seeds.size() == 0) {
            p.bestSimilarity = 0.0d;
            p.lowSeedRisk = 0.30d;
            return p;
        }

        double samePositionSum = 0.0d;
        double sameStateSum = 0.0d;
        double sameShapeSum = 0.0d;
        double overlapSum = 0.0d;
        double featureSum = 0.0d;
        double conflictSum = 0.0d;
        double falseSimilarSum = 0.0d;
        int used = 0;
        int samePositionCount = 0;
        int sameStateCount = 0;
        int sameShapeCount = 0;

        for (int i = 0; i < seeds.size(); i++) {
            SeedNode s = seeds.get(i);

            if (s == null) {
                continue;
            }

            double similarity = calcSimilarity(self, s);
            double conflict = calcSeedConflict(self, s);
            double falseSimilar = calcFalseSimilarRisk(self, s, similarity);

            if (similarity <= 0.0d) {
                continue;
            }

            used++;

            if (similarity > p.bestSimilarity) {
                p.bestSimilarity = similarity;
                p.bestSeedKey = s.seedKey;
                p.bestSeedSummary = s.summary;
            }

            if (self.position == s.position) {
                samePositionSum += similarity * Math.max(0.0d, s.seedScore - s.seedRisk * 0.30d);
                samePositionCount++;
            }

            if (safe(self.stateKey).equals(safe(s.stateKey))) {
                sameStateSum += similarity * Math.max(0.0d, s.seedScore - s.seedRisk * 0.30d);
                sameStateCount++;
            }

            if (safe(self.shapeKey).equals(safe(s.shapeKey))) {
                sameShapeSum += similarity * Math.max(0.0d, s.seedScore - s.seedRisk * 0.30d);
                sameShapeCount++;
            }

            overlapSum += calcNumberOverlap(self, s) * similarity;
            featureSum += calcFeatureSimilarity(self, s) * similarity;
            conflictSum += conflict * similarity;
            falseSimilarSum += falseSimilar * similarity;
        }

        p.seedCount = used;

        if (used <= 0) {
            p.lowSeedRisk = 0.30d;
            return p;
        }

        p.samePositionSupport =
                samePositionCount <= 0 ? 0.0d : clamp01(samePositionSum / samePositionCount);
        p.sameStateSupport = sameStateCount <= 0 ? 0.0d : clamp01(sameStateSum / sameStateCount);
        p.sameShapeSupport = sameShapeCount <= 0 ? 0.0d : clamp01(sameShapeSum / sameShapeCount);
        p.numberOverlapSupport = clamp01(overlapSum / used);
        p.featureSimilaritySupport = clamp01(featureSum / used);
        p.conflictRisk = clamp01(conflictSum / used);
        p.falseSimilarRisk = clamp01(falseSimilarSum / used);

        if (used < 3) {
            p.lowSeedRisk = 0.18d;
        } else if (used < 6) {
            p.lowSeedRisk = 0.10d;
        } else {
            p.lowSeedRisk = 0.0d;
        }

        return p;
    }

    private double calcSimilarity(SeedNode a, SeedNode b) {
        if (a == null || b == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (a.position == b.position) {
            score += 0.20d;
        }

        score += calcNumberOverlap(a, b) * 0.18d;

        if (safe(a.stateKey).equals(safe(b.stateKey))) {
            score += 0.16d;
        }

        if (safe(a.shapeKey).equals(safe(b.shapeKey))) {
            score += 0.14d;
        }

        if (safe(a.sameClassKey).equals(safe(b.sameClassKey))) {
            score += 0.12d;
        }

        score += calcFeatureSimilarity(a, b) * 0.20d;

        return clamp01(score);
    }

    private double calcNumberOverlap(SeedNode a, SeedNode b) {
        if (a == null || b == null) {
            return 0.0d;
        }

        int count = 0;

        if (maskContains(b.k3Mask, a.n1)) {
            count++;
        }

        if (maskContains(b.k3Mask, a.n2)) {
            count++;
        }

        if (maskContains(b.k3Mask, a.n3)) {
            count++;
        }

        return clamp01(((double) count) / 3.0d);
    }

    private double calcFeatureSimilarity(SeedNode a, SeedNode b) {
        if (a == null || b == null) {
            return 0.0d;
        }

        double diff = 0.0d;

        diff += Math.abs(a.trendSeed - b.trendSeed) * 0.18d;
        diff += Math.abs(a.gapSeed - b.gapSeed) * 0.18d;
        diff += Math.abs(a.hotColdSeed - b.hotColdSeed) * 0.16d;
        diff += Math.abs(a.transitionSeed - b.transitionSeed) * 0.18d;
        diff += Math.abs(a.recoverySeed - b.recoverySeed) * 0.16d;
        diff += Math.abs(a.positionHealth - b.positionHealth) * 0.14d;

        return clamp01(1.0d - diff);
    }

    private double calcSeedConflict(SeedNode a, SeedNode b) {
        if (a == null || b == null) {
            return 0.0d;
        }

        double conflict = 0.0d;

        if (a.position != b.position && calcNumberOverlap(a, b) >= 0.66d) {
            conflict += 0.10d;
        }

        if (Math.abs(a.seedScore - b.seedScore) > 0.28d) {
            conflict += 0.12d;
        }

        if (a.seedRisk > 0.60d || b.seedRisk > 0.60d) {
            conflict += 0.14d;
        }

        if (a.rankRisk > 0.55d || b.rankRisk > 0.55d) {
            conflict += 0.10d;
        }

        if (a.freezeRisk > 0.55d || b.freezeRisk > 0.55d) {
            conflict += 0.18d;
        }

        return clamp01(conflict);
    }

    private double calcFalseSimilarRisk(SeedNode a, SeedNode b, double similarity) {
        if (a == null || b == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        if (similarity >= 0.60d && b.seedRisk >= 0.62d) {
            risk += 0.20d;
        }

        if (similarity >= 0.58d && b.freezeRisk >= 0.55d) {
            risk += 0.18d;
        }

        if (similarity >= 0.55d && b.shadowRisk >= 0.62d) {
            risk += 0.14d;
        }

        if (similarity >= 0.55d && b.thirdGuardRisk >= 0.62d) {
            risk += 0.14d;
        }

        return clamp01(risk);
    }

    private void applyOneToCandidate(Models.CandidateK3 c, SeedExpansionRecord r) {
        if (c == null || r == null) {
            return;
        }

        double oldCause = getDouble(c, "causeScore", 0.0d);
        double oldRisk = getDouble(c, "riskScore", 0.0d);

        double newCause;

        if (r.seedPass == 1) {
            newCause = clamp01(oldCause * 0.91d + r.seedExpandScore * 0.09d);
        } else {
            newCause = clamp01(oldCause * 0.95d - r.seedRisk * 0.030d);
        }

        if (r.seedHardRisk == 1) {
            newCause = clamp01(newCause - 0.030d);
        }

        double newRisk = clamp01(Math.max(oldRisk, r.seedRisk));

        putString(c, "seedKey", r.seedKey);
        putString(c, "seedStateKey", r.stateKey);
        putString(c, "seedShapeKey", r.shapeKey);
        putString(c, "seedSameClassKey", r.sameClassKey);
        putString(c, "seedLevel", r.seedLevel);

        putDouble(c, "seedScore", r.seedScore);
        putDouble(c, "seedRisk", r.seedRisk);
        putDouble(c, "seedExpandScore", r.seedExpandScore);
        putDouble(c, "seedExpandSupport", r.seedExpandSupport);
        putDouble(c, "seedSimilarity", r.seedSimilarity);
        putDouble(c, "seedConflictRisk", r.seedConflictRisk);
        putDouble(c, "falseSimilarRisk", r.falseSimilarRisk);

        putInt(c, "seedPass", r.seedPass);
        putInt(c, "seedStrong", r.seedStrong);
        putInt(c, "seedHardRisk", r.seedHardRisk);
        putInt(c, "seedSourceCount", r.seedSourceCount);

        putString(c, "bestSeedKey", r.bestSeedKey);
        putString(c, "seedCauseSummary", r.summaryText);

        putDouble(c, "causeScore", newCause);
        putDouble(c, "riskScore", newRisk);

        appendReason(c, r.summaryText);
    }

    private void rerankBySeed(List<Models.CandidateK3> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.CandidateK3>() {
                    public int compare(Models.CandidateK3 a, Models.CandidateK3 b) {
                        double sa = seedCandidateScore(a);
                        double sb = seedCandidateScore(b);

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

    private static double seedCandidateScore(Models.CandidateK3 c) {
        if (c == null) {
            return 0.0d;
        }

        double base = candidateScore(c);
        double seed = getDouble(c, "seedExpandScore", getDouble(c, "seedScore", 0.0d));
        double similarity = getDouble(c, "seedSimilarity", 0.0d);
        double risk = getDouble(c, "seedRisk", 0.0d);
        int pass = getInt(c, "seedPass", 0);
        int hard = getInt(c, "seedHardRisk", 0);

        double score = base * 0.84d + seed * 0.12d + similarity * 0.04d - risk * 0.08d;

        if (pass == 1) {
            score += 0.010d;
        }

        if (hard == 1) {
            score -= 0.055d;
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
        double freeze = getDouble(c, "falseStrongFreezeRisk", 0.0d);
        double risk = getDouble(c, "riskScore", 0.0d);

        double score = 0.0d;

        score += finalP * 0.20d;
        score += posPart(edge * 4.0d) * 0.08d;
        score += cause * 0.20d;
        score += rule * 0.12d;
        score += guard * 0.10d;
        score += shadow * 0.10d;
        score += teacher * 0.08d;
        score += rank * 0.08d;

        score -= risk * 0.10d;
        score -= freeze * 0.06d;

        return clamp01(score);
    }

    private FeaturePack calcFeaturePack(
            int position, int n1, int n2, int n3, Map<String, Models.PreDrawFeature> featureMap) {
        FeaturePack p = new FeaturePack();

        Models.PreDrawFeature f1 = getFeature(featureMap, position, n1);
        Models.PreDrawFeature f2 = getFeature(featureMap, position, n2);
        Models.PreDrawFeature f3 = getFeature(featureMap, position, n3);

        Models.PreDrawFeature[] fs = new Models.PreDrawFeature[] {f1, f2, f3};

        double trend = 0.0d;
        double gap = 0.0d;
        double hotCold = 0.0d;
        double transition = 0.0d;
        double recovery = 0.0d;
        double risk = 0.0d;
        int used = 0;

        StringBuilder key = new StringBuilder();

        for (int i = 0; i < fs.length; i++) {
            Models.PreDrawFeature f = fs[i];

            if (f == null) {
                continue;
            }

            used++;

            double freqSlope = getDouble(f, "freqSlope", 0.0d);
            double freqAccel = getDouble(f, "freqAccel", 0.0d);
            double returnZone = getDouble(f, "returnZoneScore", 0.0d);
            double coldRepair = getDouble(f, "coldRepairPower", 0.0d);
            double hotPressure = getDouble(f, "hotPressure", 0.0d);
            double hotExhaustion = getDouble(f, "hotExhaustion", 0.0d);
            double transitionLift = getDouble(f, "transitionLift", 0.0d);
            double transitionConflict = getDouble(f, "transitionConflict", 0.0d);
            double recoveryQuality = getDouble(f, "recoveryQuality", 0.0d);
            double falseRecovery = getDouble(f, "falseRecoveryRisk", 0.0d);
            double noise = getDouble(f, "positionNoiseScore", 0.0d);
            double breakDepth = getDouble(f, "breakDepth", 0.0d);

            trend += clamp01(posPart(freqSlope) * 0.62d + posPart(freqAccel) * 0.38d);
            gap += clamp01(returnZone * 0.60d + coldRepair * 0.40d);
            hotCold +=
                    clamp01(
                            hotPressure * (1.0d - hotExhaustion) * 0.48d
                                    + coldRepair * 0.34d
                                    + returnZone * 0.18d);
            transition +=
                    clamp01(
                            transitionLift * 0.72d
                                    + Math.max(0.0d, transitionLift - transitionConflict) * 0.28d);
            recovery += clamp01(recoveryQuality * 0.70d + returnZone * 0.30d);
            risk +=
                    clamp01(
                            falseRecovery * 0.34d
                                    + transitionConflict * 0.20d
                                    + noise * 0.20d
                                    + hotExhaustion * 0.14d
                                    + breakDepth * 0.12d);

            if (key.length() > 0) {
                key.append("_");
            }

            key.append(bucketSigned(freqSlope));
            key.append("-");
            key.append(bucket01(returnZone));
            key.append("-");
            key.append(bucket01(transitionLift));
        }

        if (used <= 0) {
            p.featureKey = "NO_FEATURE";
            p.risk = 0.40d;
            return p;
        }

        p.trend = clamp01(trend / used);
        p.gap = clamp01(gap / used);
        p.hotCold = clamp01(hotCold / used);
        p.transition = clamp01(transition / used);
        p.recovery = clamp01(recovery / used);
        p.risk = clamp01(risk / used);
        p.featureKey = key.toString();

        return p;
    }

    private Models.PreDrawFeature getFeature(
            Map<String, Models.PreDrawFeature> map, int position, int number) {
        if (map == null) {
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

    private double calcTeacherScore(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        TeacherPack t = readTeacher(position, n1, n2, n3, map);
        return t.score;
    }

    private double calcTeacherRisk(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        TeacherPack t = readTeacher(position, n1, n2, n3, map);
        return t.risk;
    }

    private TeacherPack readTeacher(
            int position, int n1, int n2, int n3, Map<String, Models.NumberTeacherCell> map) {
        TeacherPack t = new TeacherPack();

        if (map == null) {
            return t;
        }

        int[] nums = new int[] {n1, n2, n3};

        for (int i = 0; i < nums.length; i++) {
            Models.NumberTeacherCell c = getTeacherCell(map, position, nums[i]);

            if (c == null) {
                continue;
            }

            t.score += getDouble(c, "teacherScore", getDouble(c, "numberTeacherScore", 0.0d));
            t.risk += getDouble(c, "teacherRisk", getDouble(c, "numberTeacherRisk", 0.0d));
            t.confidence += getDouble(c, "confidence", getDouble(c, "sampleConfidence", 0.0d));
            t.sample += getInt(c, "sampleCount", getInt(c, "sample", 0));
            t.hitRate += getDouble(c, "hitRate", getDouble(c, "recentHitRate", 0.0d));
            t.used++;
        }

        if (t.used > 0) {
            t.score = clamp01(t.score / t.used);
            t.risk = clamp01(t.risk / t.used);
            t.confidence = clamp01(t.confidence / t.used);
            t.hitRate = clamp01(t.hitRate / t.used);
        }

        return t;
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

    private double calcPositionHealth(int position, Map<Integer, Models.PositionHealth> map) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        return getDouble(h, "healthScore", getDouble(h, "positionHealthScore", 0.0d));
    }

    private double calcPositionRisk(int position, Map<Integer, Models.PositionHealth> map) {
        if (map == null) {
            return 0.0d;
        }

        Models.PositionHealth h = map.get(Integer.valueOf(position));

        if (h == null) {
            return 0.0d;
        }

        double risk = getDouble(h, "riskScore", getDouble(h, "positionRiskScore", 0.0d));
        double wrong = getDouble(h, "wrongRisk", getDouble(h, "positionWrongRisk", 0.0d));
        double drift = getDouble(h, "driftRisk", getDouble(h, "positionDriftRisk", 0.0d));
        double falseRisk =
                getDouble(h, "falseStrongRisk", getDouble(h, "positionFalseStrongRisk", 0.0d));

        return clamp01(risk * 0.34d + wrong * 0.26d + drift * 0.22d + falseRisk * 0.18d);
    }

    private double calcChainScore(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        ChainPack c = readChain(position, n1, n2, n3, chains);
        return c.score;
    }

    private double calcChainRisk(
            int position, int n1, int n2, int n3, List<Models.CauseChain> chains) {
        ChainPack c = readChain(position, n1, n2, n3, chains);
        return c.risk;
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

    private double calcRuleHealth(int position, int mask, ArrayList<Models.RuleHealth> list) {
        RulePack p = readRule(position, mask, list);
        return p.health;
    }

    private double calcRuleRisk(int position, int mask, ArrayList<Models.RuleHealth> list) {
        RulePack p = readRule(position, mask, list);
        return p.risk;
    }

    private RulePack readRule(int position, int mask, ArrayList<Models.RuleHealth> list) {
        RulePack p = new RulePack();

        if (list == null) {
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

            if (getInt(r, "hardBlock", 0) == 1) {
                p.hardCount++;
            }

            if (getInt(r, "rulePass", getInt(r, "pass", 0)) == 1) {
                p.passCount++;
            }

            p.count++;
        }

        if (p.count > 0) {
            p.health = clamp01(p.health / p.count);
            p.risk = clamp01(p.risk / p.count + (p.hardCount > 0 ? 0.12d : 0.0d));
        }

        return p;
    }

    private double calcThirdGuardScore(
            int position, int mask, ArrayList<Models.ThirdGuardStat> list) {
        GuardPack p = readGuard(position, mask, list);
        return p.score;
    }

    private double calcThirdGuardRisk(
            int position, int mask, ArrayList<Models.ThirdGuardStat> list) {
        GuardPack p = readGuard(position, mask, list);
        return p.risk;
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

    private double calcRankScore(
            int position, int mask, ArrayList<Models.RankCounterfactual> list) {
        RankPack p = readRank(position, mask, list);
        return p.score;
    }

    private double calcRankRisk(int position, int mask, ArrayList<Models.RankCounterfactual> list) {
        RankPack p = readRank(position, mask, list);
        return p.risk;
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
                p.missedRisk = getDouble(r, "missedStrongRisk", 0.0d);
                p.nearRisk = getDouble(r, "nearConflictRisk", 0.0d);
                p.falseChampionRisk = getDouble(r, "falseChampionRisk", 0.0d);
                p.risk =
                        clamp01(
                                p.missedRisk * 0.30d
                                        + p.nearRisk * 0.26d
                                        + p.falseChampionRisk * 0.30d);
                return p;
            }
        }

        return p;
    }

    private double calcShadowHealth(int position, int mask, ArrayList<Models.ShadowHealth> list) {
        ShadowPack p = readShadow(position, mask, list);
        return p.health;
    }

    private double calcShadowRisk(int position, int mask, ArrayList<Models.ShadowHealth> list) {
        ShadowPack p = readShadow(position, mask, list);
        return p.risk;
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

            int hp = getInt(h, "position", 0);
            int hm = getInt(h, "k3Mask", getInt(h, "k3_mask", 0));

            if (hp == position && hm == mask) {
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

    private double calcFreezeRisk(
            int position, int mask, ArrayList<Models.FalseStrongRecord> list) {
        if (list == null) {
            return 0.0d;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.FalseStrongRecord r = list.get(i);

            if (r == null) {
                continue;
            }

            int rp = getInt(r, "position", 0);
            int rm = getInt(r, "k3Mask", getInt(r, "k3_mask", 0));

            if (rp == position && rm == mask) {
                return getDouble(r, "freezeRisk", getDouble(r, "falseStrongFreezeRisk", 0.0d));
            }
        }

        return 0.0d;
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

    private SeedExpansionRecord findBestRecordForNumber(
            ArrayList<SeedExpansionRecord> records, int position, int number) {
        SeedExpansionRecord best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < records.size(); i++) {
            SeedExpansionRecord r = records.get(i);

            if (r == null) {
                continue;
            }

            if (r.position != position) {
                continue;
            }

            if (number != r.n1 && number != r.n2 && number != r.n3) {
                continue;
            }

            if (best == null || r.seedExpandScore > bestScore) {
                best = r;
                bestScore = r.seedExpandScore;
            }
        }

        return best;
    }

    private String buildSeedKey(String targetIssue, int position, int mask, String level) {
        return safe(targetIssue) + "|SEED|P" + position + "|K3_" + mask + "|" + safe(level);
    }

    private String buildExpansionKey(String targetIssue, int position, int mask) {
        return safe(targetIssue) + "|SEED_EXPAND|P" + position + "|K3_" + mask;
    }

    private String buildShapeKey(int position, int n1, int n2, int n3, FeaturePack p) {
        return "P"
                + position
                + "|K3_"
                + n1
                + "_"
                + n2
                + "_"
                + n3
                + "|T"
                + bucket01(p.trend)
                + "|G"
                + bucket01(p.gap)
                + "|H"
                + bucket01(p.hotCold)
                + "|X"
                + bucket01(p.transition)
                + "|R"
                + bucket01(p.recovery);
    }

    private String buildStateKey(int position, int n1, int n2, int n3, FeaturePack p, SeedNode s) {
        return "SEED_STATE"
                + "|P"
                + position
                + "|K3_"
                + n1
                + "_"
                + n2
                + "_"
                + n3
                + "|S"
                + bucket01(s.seedScore)
                + "|R"
                + bucket01(s.seedRisk)
                + "|F"
                + safe(p.featureKey);
    }

    private String buildSameClassKey(int position, FeaturePack p, SeedNode s) {
        return "SEED_CLASS"
                + "|P"
                + position
                + "|LV"
                + seedLevel(s.seedScore, s.seedRisk)
                + "|T"
                + bucket01(p.trend)
                + "|G"
                + bucket01(p.gap)
                + "|X"
                + bucket01(p.transition)
                + "|R"
                + bucket01(p.risk);
    }

    private String seedLevel(double score, double risk) {
        if (risk >= SEED_HARD_RISK_LINE) {
            return "HARD_RISK";
        }

        if (score >= SEED_STRONG_LINE && risk < 0.46d) {
            return "STRONG";
        }

        if (score >= SEED_USABLE_LINE && risk < SEED_RISK_LINE) {
            return "USABLE";
        }

        if (risk >= SEED_RISK_LINE) {
            return "RISK";
        }

        return "WEAK";
    }

    private String buildSeedNodeSummary(SeedNode s) {
        if (s == null) {
            return "";
        }

        return "种子节点｜P"
                + s.position
                + "｜K3="
                + s.n1
                + ","
                + s.n2
                + ","
                + s.n3
                + "｜level="
                + s.seedLevel
                + "｜seed="
                + percent(s.seedScore)
                + "｜risk="
                + percent(s.seedRisk)
                + "｜趋势="
                + percent(s.trendSeed)
                + "｜间隔="
                + percent(s.gapSeed)
                + "｜冷热="
                + percent(s.hotColdSeed)
                + "｜转移="
                + percent(s.transitionSeed)
                + "｜恢复="
                + percent(s.recoverySeed)
                + "｜说明=只作为种子扩展来源，不直接BET";
    }

    private String buildExpansionSummary(SeedExpansionRecord r, SeedNode self, SimilarityPack sim) {
        StringBuilder sb = new StringBuilder();

        sb.append("V20种子因子扩展｜rank=");
        sb.append(r.rankNo);
        sb.append("｜位置=");
        sb.append(r.position);
        sb.append("｜K3=");
        sb.append(r.n1);
        sb.append(",");
        sb.append(r.n2);
        sb.append(",");
        sb.append(r.n3);
        sb.append("｜level=");
        sb.append(r.seedLevel);
        sb.append("｜seed=");
        sb.append(percent(r.seedScore));
        sb.append("｜expand=");
        sb.append(percent(r.seedExpandScore));
        sb.append("｜support=");
        sb.append(percent(r.seedExpandSupport));
        sb.append("｜similar=");
        sb.append(percent(r.seedSimilarity));
        sb.append("｜risk=");
        sb.append(percent(r.seedRisk));
        sb.append("｜pass=");
        sb.append(r.seedPass);
        sb.append("｜strong=");
        sb.append(r.seedStrong);
        sb.append("｜hardRisk=");
        sb.append(r.seedHardRisk);
        sb.append("｜种子数=");
        sb.append(r.seedSourceCount);
        sb.append("｜同位置=");
        sb.append(percent(r.samePositionSupport));
        sb.append("｜同状态=");
        sb.append(percent(r.sameStateSupport));
        sb.append("｜同形态=");
        sb.append(percent(r.sameShapeSupport));
        sb.append("｜号码重叠=");
        sb.append(percent(r.numberOverlapSupport));
        sb.append("｜特征相似=");
        sb.append(percent(r.featureSimilaritySupport));
        sb.append("｜冲突=");
        sb.append(percent(r.seedConflictRisk));
        sb.append("｜伪相似=");
        sb.append(percent(r.falseSimilarRisk));

        if (r.bestSeedKey != null && r.bestSeedKey.length() > 0) {
            sb.append("｜最佳种子=");
            sb.append(r.bestSeedKey);
        }

        sb.append("｜说明=种子扩展只增强因链和排序证据，不创建BET，不结算，不污染真实校准");

        return sb.toString();
    }

    private String candidateKey(int position, int mask) {
        return "P" + position + "|K3_" + mask;
    }

    public static String keyOf(int position, int number) {
        return String.valueOf(position) + "_" + String.valueOf(number);
    }

    private boolean maskContains(int mask, int number) {
        if (!isValidNumber(number) || mask <= 0) {
            return false;
        }

        try {
            return (mask & (1 << number)) != 0;
        } catch (Throwable e) {
            return false;
        }
    }

    private static int readMaxSeedSource() {
        int v = readAppInt("SEED_EXPANSION_MAX_SOURCE", 60);

        if (v <= 0) {
            v = 60;
        }

        return v;
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

    private boolean isValidPosition(int p) {
        return p >= POSITION_MIN && p <= POSITION_MAX;
    }

    private boolean isValidNumber(int n) {
        return n >= NUMBER_MIN && n <= NUMBER_MAX;
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

    public static final class SeedExpansionRecord {
        public String recordKey;
        public String baseIssue;
        public String targetIssue;
        public long createdAt;

        public int rankNo;
        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public String seedKey;
        public String stateKey;
        public String shapeKey;
        public String sameClassKey;
        public String seedLevel;

        public double seedScore;
        public double seedRisk;
        public double selfSeedRisk;
        public double seedExpandScore;
        public double seedExpandSupport;
        public double seedSimilarity;
        public double seedConflictRisk;
        public double falseSimilarRisk;

        public double samePositionSupport;
        public double sameStateSupport;
        public double sameShapeSupport;
        public double numberOverlapSupport;
        public double featureSimilaritySupport;

        public int seedPass;
        public int seedStrong;
        public int seedHardRisk;
        public int seedSourceCount;

        public String bestSeedKey;
        public String bestSeedSummary;
        public String summaryText;
    }

    private static final class SeedNode {
        String targetIssue;

        int position;
        int n1;
        int n2;
        int n3;
        int k3Mask;
        int rankNo;

        double finalP;
        double edge;
        double candidateScore;
        double causeScore;
        double riskScore;

        double teacherScore;
        double teacherRisk;
        double positionHealth;
        double positionRisk;
        double chainScore;
        double chainRisk;
        double ruleHealth;
        double ruleRisk;
        double thirdGuardScore;
        double thirdGuardRisk;
        double rankScore;
        double rankRisk;
        double shadowHealth;
        double shadowRisk;
        double freezeRisk;

        double trendSeed;
        double gapSeed;
        double hotColdSeed;
        double transitionSeed;
        double recoverySeed;
        double featureRisk;

        double seedScore;
        double seedRisk;

        String seedKey;
        String seedLevel;
        String featureKey;
        String shapeKey;
        String stateKey;
        String sameClassKey;
        String summary;
    }

    private static final class FeaturePack {
        double trend;
        double gap;
        double hotCold;
        double transition;
        double recovery;
        double risk;
        String featureKey;
    }

    private static final class SimilarityPack {
        int seedCount;

        double bestSimilarity;
        double samePositionSupport;
        double sameStateSupport;
        double sameShapeSupport;
        double numberOverlapSupport;
        double featureSimilaritySupport;

        double conflictRisk;
        double falseSimilarRisk;
        double lowSeedRisk;

        String bestSeedKey;
        String bestSeedSummary;
    }

    private static final class TeacherPack {
        int used;
        int sample;
        double score;
        double risk;
        double confidence;
        double hitRate;
    }

    private static final class ChainPack {
        int sample;
        double score;
        double risk;
    }

    private static final class RulePack {
        int count;
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

    private static final class RankPack {
        double score;
        double risk;
        double missedRisk;
        double nearRisk;
        double falseChampionRisk;
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
}
