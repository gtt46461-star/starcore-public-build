package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ProfileFallbackEngine {

    private static final int LEVEL_EXACT = 0;
    private static final int LEVEL_SAME_POSITION_STATE = 1;
    private static final int LEVEL_SAME_POSITION_TREND = 2;
    private static final int LEVEL_SAME_POSITION = 3;
    private static final int LEVEL_SAME_STATE = 4;
    private static final int LEVEL_GLOBAL_SIMILAR = 5;
    private static final int LEVEL_NONE = 99;

    private static final int MIN_EXACT_SAMPLE = 24;
    private static final int MIN_FALLBACK_SAMPLE = 36;
    private static final int MIN_RECENT_SAMPLE = 16;

    private static final double MIN_USABLE_HIT_RATE = 0.330d;
    private static final double STRONG_HIT_RATE = 0.380d;
    private static final double FALLBACK_SCORE_USABLE = 52.0d;
    private static final double FALLBACK_SCORE_STRONG = 62.0d;
    private static final double FALLBACK_SCORE_A_PLUS = 72.0d;

    private final DeepProfileEngine deepProfileEngine;

    public ProfileFallbackEngine() {
        deepProfileEngine = new DeepProfileEngine();
    }

    public FallbackResult analyzeCandidate(
            List<Models.DeepProfile> profiles, Models.ComboCandidate candidate) {
        FallbackRequest request = buildRequest(candidate);
        return analyze(profiles, request);
    }

    public FallbackResult analyzeDecision(
            List<Models.DeepProfile> profiles, Models.Decision decision) {
        FallbackRequest request = buildRequest(decision);
        return analyze(profiles, request);
    }

    public FallbackResult analyzeTrade(List<Models.DeepProfile> profiles, Models.Trade trade) {
        FallbackRequest request = buildRequest(trade);
        return analyze(profiles, request);
    }

    public FallbackResult analyze(List<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackResult result = new FallbackResult();
        result.createdAt = System.currentTimeMillis();

        if (request == null || !request.isValid()) {
            result.fallbackLevel = LEVEL_NONE;
            result.status = "INVALID";
            result.summaryText = "画像回退：请求无效";
            return result;
        }

        result.requestKey = request.profileKey;
        result.position = request.position;
        result.stateMode = request.stateMode;

        ArrayList<Models.DeepProfile> clean = cleanProfiles(profiles);

        if (clean.size() == 0) {
            result.fallbackLevel = LEVEL_NONE;
            result.status = "NO_PROFILE";
            result.summaryText = "画像回退：暂无任何画像样本";
            return result;
        }

        Models.DeepProfile exact = deepProfileEngine.findProfile(clean, request.profileKey);

        if (isExactUsable(exact)) {
            fillFromProfile(result, exact, LEVEL_EXACT, request, 1.00d, "EXACT");
            return result;
        }

        FallbackCandidate best = null;

        FallbackCandidate a = searchSamePositionState(clean, request);
        best = better(best, a);

        FallbackCandidate b = searchSamePositionTrend(clean, request);
        best = better(best, b);

        FallbackCandidate c = searchSamePosition(clean, request);
        best = better(best, c);

        FallbackCandidate d = searchSameState(clean, request);
        best = better(best, d);

        FallbackCandidate e = searchGlobalSimilar(clean, request);
        best = better(best, e);

        if (best == null || best.profile == null) {
            result.fallbackLevel = LEVEL_NONE;
            result.status = "NO_MATCH";
            result.summaryText = "画像回退：没有找到可用相似画像";
            return result;
        }

        fillFromProfile(result, best.profile, best.level, request, best.penalty, best.status);

        return result;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.DeepProfile> profiles) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            FallbackResult r = analyzeCandidate(profiles, copy);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, FallbackResult result) {
        if (candidate == null) {
            return;
        }

        if (result == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "画像回退：暂无");
            return;
        }

        candidate.fallbackProfileKey = result.fallbackKey;
        candidate.deepFallbackLevel = result.fallbackLevel;
        candidate.fallbackProfileScore =
                blend01(candidate.fallbackProfileScore, result.fallbackScore / 100.0d, 0.22d);

        if (result.confidence >= 50.0d) {
            candidate.deepConfidence =
                    blend01(candidate.deepConfidence, result.confidence / 100.0d, 0.08d);
        }

        if (result.fallbackScore >= FALLBACK_SCORE_STRONG
                && result.riskScore <= 52.0d
                && result.confidence >= 52.0d
                && candidate.finalP >= 0.360d) {
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + 0.002d);
            candidate.rawP = AppConfig.clampProbability(candidate.rawP + 0.001d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        if (result.fallbackScore >= FALLBACK_SCORE_A_PLUS
                && result.riskScore <= 42.0d
                && result.confidence >= 62.0d) {
            candidate.aPlusScore =
                    blend01(candidate.aPlusScore, result.fallbackScore / 100.0d, 0.08d);
        }

        if (result.riskScore >= 82.0d && candidate.finalP < 0.430d) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, AppConfig.BLOCK_BACKTEST_WEAK);
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "画像回退：level="
                                + result.fallbackLevel
                                + "，key="
                                + result.fallbackKey
                                + "，分="
                                + UiFormatter.score(result.fallbackScore)
                                + "，风险="
                                + UiFormatter.score(result.riskScore)
                                + "，置信="
                                + UiFormatter.score(result.confidence)
                                + "，命中="
                                + UiFormatter.probability(result.hitRate)
                                + "，近期="
                                + UiFormatter.probability(result.recentHitRate)
                                + "，样本="
                                + result.sampleCount
                                + "，状态="
                                + result.status);
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set,
            Models.ComboCandidate candidate,
            List<Models.DeepProfile> profiles) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        if (candidate == null) {
            return set;
        }

        FallbackResult r = analyzeCandidate(profiles, candidate);

        set.profileKey = r.fallbackKey;
        set.globalTrendStrength = Math.max(set.globalTrendStrength, r.fallbackScore);
        set.summaryText = append(set.summaryText, r.summaryText);

        return set;
    }

    public String buildSummary(FallbackResult result) {
        if (result == null) {
            return "画像回退：暂无";
        }

        return result.summaryText == null || result.summaryText.length() == 0
                ? "画像回退：" + result.status
                : result.summaryText;
    }

    public String buildSummaryList(List<FallbackResult> list) {
        if (list == null || list.size() == 0) {
            return "画像回退：暂无结果";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(5, list.size());

        for (int i = 0; i < limit; i++) {
            FallbackResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append(" L");
            sb.append(r.fallbackLevel);
            sb.append(" 分=");
            sb.append(UiFormatter.score(r.fallbackScore));
            sb.append(" 样本=");
            sb.append(r.sampleCount);
            sb.append(" 命中=");
            sb.append(UiFormatter.probability(r.hitRate));
        }

        return sb.length() == 0 ? "画像回退：暂无有效结果" : sb.toString();
    }

    private FallbackRequest buildRequest(Models.ComboCandidate candidate) {
        FallbackRequest r = new FallbackRequest();

        if (candidate == null) {
            return r;
        }

        r.position = candidate.position;
        r.stateMode = safe(candidate.stateMode);
        r.trendBucket = trendBucket(candidate.trendLife);
        r.driftBucket = driftBucket(candidate.driftRisk);
        r.breakBucket = breakBucket(candidate.breakDepth);
        r.recoveryBucket = recoveryBucket(candidate.recoveryScore);
        r.cycleBucket = relationBucket(candidate.deepCycleScore);
        r.relationBucket = relationBucket(candidate.deepTransitionScore);
        r.profileKey = deepProfileEngine.buildProfileKey(candidate);

        return r;
    }

    private FallbackRequest buildRequest(Models.Decision decision) {
        FallbackRequest r = new FallbackRequest();

        if (decision == null) {
            return r;
        }

        r.position = decision.position;
        r.stateMode = safe(decision.stateMode);
        r.trendBucket = trendBucket(decision.trendLife);
        r.driftBucket = driftBucket(decision.driftRisk);
        r.breakBucket = breakBucket(decision.breakDepth);
        r.recoveryBucket = recoveryBucket(decision.recoveryScore);
        r.cycleBucket = relationBucket(decision.deepScore);
        r.relationBucket = relationBucket(decision.aiJudgeScore);
        r.profileKey = deepProfileEngine.buildProfileKey(decision);

        return r;
    }

    private FallbackRequest buildRequest(Models.Trade trade) {
        FallbackRequest r = new FallbackRequest();

        if (trade == null) {
            return r;
        }

        r.position = trade.position;
        r.stateMode = safe(trade.stateMode);
        r.trendBucket = trendBucket(trade.trendLife);
        r.driftBucket = driftBucket(trade.driftRisk);
        r.breakBucket = breakBucket(trade.breakDepth);
        r.recoveryBucket = recoveryBucket(trade.recoveryScore);
        r.cycleBucket = relationBucket(trade.deepScore);
        r.relationBucket = relationBucket(trade.aiJudgeScore);
        r.profileKey = deepProfileEngine.buildProfileKey(trade);

        return r;
    }

    private ArrayList<Models.DeepProfile> cleanProfiles(List<Models.DeepProfile> profiles) {
        ArrayList<Models.DeepProfile> out = new ArrayList<Models.DeepProfile>();

        if (profiles == null || profiles.size() == 0) {
            return out;
        }

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            if (p.profileKey == null || p.profileKey.length() == 0) {
                continue;
            }

            Models.DeepProfile copy = deepProfileEngine.copyProfile(p);
            copy.recalc();
            out.add(copy);
        }

        return out;
    }

    private boolean isExactUsable(Models.DeepProfile p) {
        if (p == null) {
            return false;
        }

        p.recalc();

        if (p.totalSample < MIN_EXACT_SAMPLE) {
            return false;
        }

        if (p.totalHitRate < MIN_USABLE_HIT_RATE && p.recentHitRate < MIN_USABLE_HIT_RATE) {
            return false;
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            return false;
        }

        if (p.activeFlag == 0) {
            return false;
        }

        return true;
    }

    private boolean isFallbackUsable(Models.DeepProfile p) {
        if (p == null) {
            return false;
        }

        p.recalc();

        if (p.totalSample < MIN_FALLBACK_SAMPLE) {
            return false;
        }

        if (p.totalHitRate < MIN_USABLE_HIT_RATE && p.recentHitRate < MIN_USABLE_HIT_RATE) {
            return false;
        }

        if (p.activeFlag == 0) {
            return false;
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            return false;
        }

        return true;
    }

    private FallbackCandidate searchSamePositionState(
            ArrayList<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackCandidate best = null;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (!isFallbackUsable(p)) {
                continue;
            }

            if (p.position != request.position) {
                continue;
            }

            if (!safe(p.stateMode).equals(request.stateMode)) {
                continue;
            }

            double sim = similarity(p, request);
            FallbackCandidate c = new FallbackCandidate();
            c.profile = p;
            c.level = LEVEL_SAME_POSITION_STATE;
            c.penalty = 0.88d;
            c.status = "SAME_POSITION_STATE";
            c.score = profileBaseScore(p) * c.penalty + sim * 8.0d;

            best = better(best, c);
        }

        return best;
    }

    private FallbackCandidate searchSamePositionTrend(
            ArrayList<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackCandidate best = null;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (!isFallbackUsable(p)) {
                continue;
            }

            if (p.position != request.position) {
                continue;
            }

            if (p.trendBucket != request.trendBucket) {
                continue;
            }

            double sim = similarity(p, request);
            FallbackCandidate c = new FallbackCandidate();
            c.profile = p;
            c.level = LEVEL_SAME_POSITION_TREND;
            c.penalty = 0.78d;
            c.status = "SAME_POSITION_TREND";
            c.score = profileBaseScore(p) * c.penalty + sim * 7.0d;

            best = better(best, c);
        }

        return best;
    }

    private FallbackCandidate searchSamePosition(
            ArrayList<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackCandidate best = null;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (!isFallbackUsable(p)) {
                continue;
            }

            if (p.position != request.position) {
                continue;
            }

            double sim = similarity(p, request);
            FallbackCandidate c = new FallbackCandidate();
            c.profile = p;
            c.level = LEVEL_SAME_POSITION;
            c.penalty = 0.68d;
            c.status = "SAME_POSITION";
            c.score = profileBaseScore(p) * c.penalty + sim * 6.0d;

            best = better(best, c);
        }

        return best;
    }

    private FallbackCandidate searchSameState(
            ArrayList<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackCandidate best = null;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (!isFallbackUsable(p)) {
                continue;
            }

            if (!safe(p.stateMode).equals(request.stateMode)) {
                continue;
            }

            double sim = similarity(p, request);
            FallbackCandidate c = new FallbackCandidate();
            c.profile = p;
            c.level = LEVEL_SAME_STATE;
            c.penalty = 0.58d;
            c.status = "SAME_STATE";
            c.score = profileBaseScore(p) * c.penalty + sim * 5.0d;

            best = better(best, c);
        }

        return best;
    }

    private FallbackCandidate searchGlobalSimilar(
            ArrayList<Models.DeepProfile> profiles, FallbackRequest request) {
        FallbackCandidate best = null;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (!isFallbackUsable(p)) {
                continue;
            }

            double sim = similarity(p, request);

            if (sim < 0.42d) {
                continue;
            }

            FallbackCandidate c = new FallbackCandidate();
            c.profile = p;
            c.level = LEVEL_GLOBAL_SIMILAR;
            c.penalty = 0.48d;
            c.status = "GLOBAL_SIMILAR";
            c.score = profileBaseScore(p) * c.penalty + sim * 4.0d;

            best = better(best, c);
        }

        return best;
    }

    private FallbackCandidate better(FallbackCandidate oldOne, FallbackCandidate newOne) {
        if (newOne == null || newOne.profile == null) {
            return oldOne;
        }

        if (oldOne == null || oldOne.profile == null) {
            return newOne;
        }

        if (newOne.score > oldOne.score) {
            return newOne;
        }

        if (newOne.score < oldOne.score) {
            return oldOne;
        }

        if (newOne.level < oldOne.level) {
            return newOne;
        }

        return oldOne;
    }

    private double similarity(Models.DeepProfile p, FallbackRequest r) {
        if (p == null || r == null) {
            return 0.0d;
        }

        double score = 0.0d;
        double weight = 0.0d;

        weight += 0.22d;
        if (p.position == r.position) {
            score += 0.22d;
        }

        weight += 0.18d;
        if (safe(p.stateMode).equals(r.stateMode)) {
            score += 0.18d;
        }

        weight += 0.16d;
        score += bucketSimilarity(p.trendBucket, r.trendBucket) * 0.16d;

        weight += 0.13d;
        score += bucketSimilarity(p.driftBucket, r.driftBucket) * 0.13d;

        weight += 0.12d;
        score += bucketSimilarity(p.breakBucket, r.breakBucket) * 0.12d;

        weight += 0.10d;
        score += bucketSimilarity(p.recoveryBucket, r.recoveryBucket) * 0.10d;

        weight += 0.05d;
        score += bucketSimilarity(p.cycleBucket, r.cycleBucket) * 0.05d;

        weight += 0.04d;
        score += bucketSimilarity(p.relationBucket, r.relationBucket) * 0.04d;

        if (weight <= 0.0d) {
            return 0.0d;
        }

        return AppConfig.clampProbability(score / weight);
    }

    private double bucketSimilarity(int a, int b) {
        if (a <= 0 || b <= 0) {
            return 0.0d;
        }

        int diff = Math.abs(a - b);

        if (diff == 0) {
            return 1.0d;
        }

        if (diff == 1) {
            return 0.78d;
        }

        if (diff == 2) {
            return 0.52d;
        }

        if (diff == 3) {
            return 0.28d;
        }

        return 0.08d;
    }

    private void fillFromProfile(
            FallbackResult result,
            Models.DeepProfile p,
            int level,
            FallbackRequest request,
            double penalty,
            String status) {
        if (result == null || p == null) {
            return;
        }

        p.recalc();

        result.fallbackKey = safe(p.profileKey);
        result.fallbackLevel = level;
        result.position = request == null ? p.position : request.position;
        result.stateMode = request == null ? safe(p.stateMode) : safe(request.stateMode);
        result.status = status == null ? "" : status;

        result.sampleCount = p.totalSample;
        result.hitCount = p.totalHit;
        result.hitRate = p.totalHitRate;
        result.recentSample = p.recentSample;
        result.recentHitRate = p.recentHitRate;
        result.decayWeight = p.decayWeight;
        result.failCount = p.failCount;
        result.staleFlag = p.staleFlag;
        result.recoverFlag = p.recoverFlag;

        result.similarity = request == null ? 1.0d : similarity(p, request);
        result.levelPenalty = penalty;

        result.baseScore = profileBaseScore(p);
        result.riskScore = fallbackRisk(p, level, result.similarity);
        result.confidence = fallbackConfidence(p, level, result.similarity);
        result.fallbackScore = finalFallbackScore(p, level, penalty, result.similarity);

        if (level == LEVEL_EXACT) {
            result.status = "EXACT";
        }

        result.summaryText = buildResultSummary(result);
    }

    private double profileBaseScore(Models.DeepProfile p) {
        if (p == null) {
            return 0.0d;
        }

        p.recalc();

        double score = 0.0d;

        score += p.totalHitRate * 100.0d * 0.34d;
        score += p.recentHitRate * 100.0d * 0.32d;
        score += sampleScore(p.totalSample) * 0.12d;
        score += sampleScore(p.recentSample) * 0.08d;
        score += p.avgFinalP * 100.0d * 0.06d;
        score += Math.max(0.0d, p.avgEdge) * 100.0d * 0.04d;
        score += p.avgDeepScore * 100.0d * 0.04d;

        score *= p.decayWeight;

        if (p.recoverFlag == 1) {
            score += 3.0d;
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            score -= 10.0d;
        }

        if (p.activeFlag == 0) {
            score -= 12.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double fallbackRisk(Models.DeepProfile p, int level, double similarity) {
        if (p == null) {
            return 100.0d;
        }

        p.recalc();

        double risk = 0.0d;

        if (p.totalSample < MIN_FALLBACK_SAMPLE) {
            risk += 22.0d;
        }

        if (p.recentSample < MIN_RECENT_SAMPLE) {
            risk += 14.0d;
        }

        if (p.totalHitRate < MIN_USABLE_HIT_RATE) {
            risk += 20.0d;
        }

        if (p.recentSample >= MIN_RECENT_SAMPLE && p.recentHitRate < MIN_USABLE_HIT_RATE) {
            risk += 20.0d;
        }

        if (p.failCount >= 5) {
            risk += Math.min(26.0d, p.failCount * 3.0d);
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            risk += 18.0d;
        }

        if (p.activeFlag == 0) {
            risk += 16.0d;
        }

        risk += levelPenaltyRisk(level);
        risk += Math.max(0.0d, 1.0d - similarity) * 100.0d * 0.22d;

        if (p.recoverFlag == 1) {
            risk -= 6.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double fallbackConfidence(Models.DeepProfile p, int level, double similarity) {
        if (p == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) p.totalSample) / 500.0d) * 34.0d;
        score += Math.min(1.0d, ((double) p.recentSample) / 160.0d) * 20.0d;
        score += similarity * 100.0d * 0.20d;
        score += Math.max(0.0d, 100.0d - fallbackRisk(p, level, similarity)) * 0.16d;
        score += Math.max(0.0d, 6.0d - level) * 1.4d;

        return AppConfig.clampScore100(score);
    }

    private double finalFallbackScore(
            Models.DeepProfile p, int level, double penalty, double similarity) {
        if (p == null) {
            return 0.0d;
        }

        double base = profileBaseScore(p);
        double risk = fallbackRisk(p, level, similarity);
        double confidence = fallbackConfidence(p, level, similarity);

        double score = 0.0d;

        score += base * 0.56d;
        score += p.totalHitRate * 100.0d * 0.14d;
        score += p.recentHitRate * 100.0d * 0.14d;
        score += similarity * 100.0d * 0.08d;
        score += confidence * 0.08d;

        score *= penalty;
        score -= risk * 0.14d;

        if (p.totalHitRate >= STRONG_HIT_RATE && p.recentHitRate >= STRONG_HIT_RATE) {
            score += 4.0d;
        }

        if (p.recoverFlag == 1) {
            score += 2.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double levelPenaltyRisk(int level) {
        if (level == LEVEL_EXACT) return 0.0d;
        if (level == LEVEL_SAME_POSITION_STATE) return 6.0d;
        if (level == LEVEL_SAME_POSITION_TREND) return 10.0d;
        if (level == LEVEL_SAME_POSITION) return 14.0d;
        if (level == LEVEL_SAME_STATE) return 18.0d;
        if (level == LEVEL_GLOBAL_SIMILAR) return 24.0d;
        return 40.0d;
    }

    private double sampleScore(int sample) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample >= 500) {
            return 100.0d;
        }

        return AppConfig.clampScore100(((double) sample) / 500.0d * 100.0d);
    }

    private int trendBucket(double v) {
        if (v >= 80.0d) return 8;
        if (v >= 70.0d) return 7;
        if (v >= 60.0d) return 6;
        if (v >= 50.0d) return 5;
        if (v >= 40.0d) return 4;
        if (v >= 30.0d) return 3;
        if (v >= 20.0d) return 2;
        return 1;
    }

    private int driftBucket(double v) {
        if (v >= 85.0d) return 8;
        if (v >= 75.0d) return 7;
        if (v >= 65.0d) return 6;
        if (v >= 55.0d) return 5;
        if (v >= 45.0d) return 4;
        if (v >= 35.0d) return 3;
        if (v >= 25.0d) return 2;
        return 1;
    }

    private int breakBucket(double v) {
        if (v >= 85.0d) return 8;
        if (v >= 75.0d) return 7;
        if (v >= 65.0d) return 6;
        if (v >= 55.0d) return 5;
        if (v >= 45.0d) return 4;
        if (v >= 35.0d) return 3;
        if (v >= 25.0d) return 2;
        return 1;
    }

    private int recoveryBucket(double v) {
        if (v >= 80.0d) return 8;
        if (v >= 70.0d) return 7;
        if (v >= 60.0d) return 6;
        if (v >= 50.0d) return 5;
        if (v >= 40.0d) return 4;
        if (v >= 30.0d) return 3;
        if (v >= 20.0d) return 2;
        return 1;
    }

    private int relationBucket(double v) {
        double score = v <= 1.0d ? v * 100.0d : v;

        if (score >= 80.0d) return 8;
        if (score >= 70.0d) return 7;
        if (score >= 60.0d) return 6;
        if (score >= 50.0d) return 5;
        if (score >= 40.0d) return 4;
        if (score >= 30.0d) return 3;
        if (score >= 20.0d) return 2;
        return 1;
    }

    private void sortCandidates(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = candidateSortScore(a);
                        double sb = candidateSortScore(b);

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private double candidateSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.sortScore();

        score += c.fallbackProfileScore * 100.0d * 0.10d;
        score += c.profileScore * 100.0d * 0.08d;
        score -= c.evidenceRisk * 100.0d * 0.05d;

        if (c.fallbackProfileKey != null && c.fallbackProfileKey.length() > 0) {
            score += 2.0d;
        }

        return score;
    }

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(oldCode)) {
            return oldCode;
        }

        return newCode == null || newCode.length() == 0 ? AppConfig.BLOCK_BACKTEST_WEAK : newCode;
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildResultSummary(FallbackResult r) {
        if (r == null) {
            return "画像回退为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("画像回退P");
        sb.append(r.position);

        sb.append("：level=");
        sb.append(r.fallbackLevel);

        sb.append("，状态=");
        sb.append(r.status);

        sb.append("，key=");
        sb.append(r.fallbackKey);

        sb.append("，分=");
        sb.append(UiFormatter.score(r.fallbackScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(r.riskScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(r.confidence));

        sb.append("，相似=");
        sb.append(UiFormatter.probability(r.similarity));

        sb.append("，样本=");
        sb.append(r.sampleCount);

        sb.append("，命中=");
        sb.append(UiFormatter.probability(r.hitRate));

        sb.append("，近期=");
        sb.append(UiFormatter.probability(r.recentHitRate));

        sb.append("，失败=");
        sb.append(r.failCount);

        if (r.staleFlag == 1) {
            sb.append("，画像过期");
        }

        if (r.recoverFlag == 1) {
            sb.append("，画像恢复中");
        }

        if (r.fallbackScore >= FALLBACK_SCORE_STRONG && r.riskScore <= 55.0d) {
            sb.append("，可作为回退支撑");
        } else if (r.fallbackScore >= FALLBACK_SCORE_USABLE) {
            sb.append("，弱回退支撑");
        } else {
            sb.append("，回退支撑不足");
        }

        return sb.toString();
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return addText;
        }

        return oldText + "；" + addText;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class FallbackRequest {
        public String profileKey;
        public int position;
        public String stateMode;
        public int trendBucket;
        public int driftBucket;
        public int breakBucket;
        public int recoveryBucket;
        public int cycleBucket;
        public int relationBucket;

        public FallbackRequest() {
            profileKey = "";
            position = 0;
            stateMode = "";
            trendBucket = 0;
            driftBucket = 0;
            breakBucket = 0;
            recoveryBucket = 0;
            cycleBucket = 0;
            relationBucket = 0;
        }

        public boolean isValid() {
            return AppConfig.isValidPosition(position)
                    && profileKey != null
                    && profileKey.length() > 0;
        }
    }

    private static final class FallbackCandidate {
        public Models.DeepProfile profile;
        public int level;
        public double penalty;
        public double score;
        public String status;

        public FallbackCandidate() {
            profile = null;
            level = LEVEL_NONE;
            penalty = 0.0d;
            score = 0.0d;
            status = "";
        }
    }

    public static final class FallbackResult {
        public long createdAt;

        public String requestKey;
        public String fallbackKey;
        public int fallbackLevel;

        public int position;
        public String stateMode;

        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int recentSample;
        public double recentHitRate;

        public double decayWeight;
        public int failCount;
        public int staleFlag;
        public int recoverFlag;

        public double similarity;
        public double levelPenalty;
        public double baseScore;
        public double fallbackScore;
        public double riskScore;
        public double confidence;

        public String status;
        public String summaryText;

        public FallbackResult() {
            createdAt = 0L;

            requestKey = "";
            fallbackKey = "";
            fallbackLevel = LEVEL_NONE;

            position = 0;
            stateMode = "";

            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            recentSample = 0;
            recentHitRate = 0.0d;

            decayWeight = 1.0d;
            failCount = 0;
            staleFlag = 0;
            recoverFlag = 0;

            similarity = 0.0d;
            levelPenalty = 0.0d;
            baseScore = 0.0d;
            fallbackScore = 0.0d;
            riskScore = 100.0d;
            confidence = 0.0d;

            status = "NONE";
            summaryText = "";
        }

        public boolean isUsable() {
            return fallbackLevel != LEVEL_NONE
                    && fallbackScore >= FALLBACK_SCORE_USABLE
                    && riskScore <= 72.0d
                    && confidence >= 38.0d;
        }

        public boolean isStrong() {
            return isUsable()
                    && fallbackScore >= FALLBACK_SCORE_STRONG
                    && riskScore <= 55.0d
                    && confidence >= 52.0d;
        }
    }
}
