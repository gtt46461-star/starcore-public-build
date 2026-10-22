package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DeepProfileEngine {

    private static final int RECENT_LIMIT = 240;
    private static final int RECENT_MIN_FOR_JUDGE = 30;

    private static final double PROFILE_USABLE_SCORE = 52.0d;
    private static final double PROFILE_STRONG_SCORE = 62.0d;
    private static final double PROFILE_A_PLUS_SCORE = 72.0d;

    private static final double STALE_HIT_RATE = 0.300d;
    private static final double RECOVER_HIT_RATE = 0.380d;
    private static final double STRONG_HIT_RATE = 0.420d;
    private static final double USABLE_HIT_RATE = 0.370d;

    private static final int FAIL_STALE_COUNT = 10;
    private static final int FAIL_INACTIVE_COUNT = 8;
    private static final int FAIL_GUARD_COUNT = 5;

    private static final int MAX_REASON_TEXT = 420;
    private static final int MAX_PROFILE_SUMMARY = 520;

    public DeepProfileEngine() {}

    public Models.DeepProfile learnAndSaveProfile(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.Trade trade,
            Models.Draw actualDraw) {
        Models.DeepProfile p = updateProfileByTrade(profiles, trade, actualDraw);
        saveProfileState(store, p, "TRADE");
        return p;
    }

    public Models.DeepProfile learnAndSaveProfile(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.Decision decision,
            Models.Draw actualDraw) {
        Models.DeepProfile p = updateProfileByDecision(profiles, decision, actualDraw);
        saveProfileState(store, p, "DECISION");
        return p;
    }

    public Models.DeepProfile learnAndSaveProfile(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        Models.DeepProfile p = updateProfileByCandidate(profiles, candidate, actualDraw);
        saveProfileState(store, p, "CANDIDATE");
        return p;
    }

    public Models.DeepProfile updateProfileByTrade(
            List<Models.DeepProfile> profiles, Models.Trade trade, Models.Draw actualDraw) {
        if (trade == null) {
            return null;
        }

        String key = buildProfileKey(trade);
        Models.DeepProfile old = findProfile(profiles, key);
        Models.DeepProfile updated = learnProfile(old, trade, actualDraw);

        replaceProfile(profiles, updated);

        return updated;
    }

    public Models.DeepProfile updateProfileByDecision(
            List<Models.DeepProfile> profiles, Models.Decision decision, Models.Draw actualDraw) {
        if (decision == null) {
            return null;
        }

        String key = buildProfileKey(decision);
        Models.DeepProfile old = findProfile(profiles, key);
        Models.DeepProfile updated = learnProfile(old, decision, actualDraw);

        replaceProfile(profiles, updated);

        return updated;
    }

    public Models.DeepProfile updateProfileByCandidate(
            List<Models.DeepProfile> profiles,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        if (candidate == null) {
            return null;
        }

        String key = buildProfileKey(candidate);
        Models.DeepProfile old = findProfile(profiles, key);
        Models.DeepProfile updated = learnProfile(old, candidate, actualDraw);

        replaceProfile(profiles, updated);

        return updated;
    }

    public Models.DeepProfile buildProfile(Models.ComboCandidate candidate) {
        Models.DeepProfile p = new Models.DeepProfile();

        if (candidate == null) {
            p.profileKey = "";
            p.activeFlag = 0;
            p.staleFlag = 1;
            return p;
        }

        p.profileKey = buildProfileKey(candidate);
        p.position = candidate.position;
        p.stateMode = safe(candidate.stateMode);
        p.trendBucket = trendBucket(candidate.trendLife);
        p.driftBucket = driftBucket(candidate.driftRisk);
        p.breakBucket = breakBucket(candidate.breakDepth);
        p.recoveryBucket = recoveryBucket(candidate.recoveryScore);
        p.cycleBucket = cycleBucket(candidate.deepCycleScore);
        p.relationBucket = relationBucket(candidate.deepTransitionScore);
        p.totalSample = 0;
        p.totalHit = 0;
        p.totalHitRate = 0.0d;
        p.recentSample = 0;
        p.recentHit = 0;
        p.recentHitRate = 0.0d;
        p.decayWeight = 1.0d;
        p.activeFlag = 1;
        p.staleFlag = 0;
        p.recoverFlag = 0;
        p.failCount = 0;
        p.recoverCount = 0;
        p.avgFinalP = candidate.finalP;
        p.avgEdge = candidate.edge;
        p.avgDeepScore = candidate.deepScore;
        p.updatedAt = System.currentTimeMillis();

        return p;
    }

    public Models.DeepProfile buildProfile(Models.Decision decision) {
        Models.DeepProfile p = new Models.DeepProfile();

        if (decision == null) {
            p.profileKey = "";
            p.activeFlag = 0;
            p.staleFlag = 1;
            return p;
        }

        p.profileKey = buildProfileKey(decision);
        p.position = decision.position;
        p.stateMode = safe(decision.stateMode);
        p.trendBucket = trendBucket(decision.trendLife);
        p.driftBucket = driftBucket(decision.driftRisk);
        p.breakBucket = breakBucket(decision.breakDepth);
        p.recoveryBucket = recoveryBucket(decision.recoveryScore);
        p.cycleBucket = cycleBucket(decision.deepScore);
        p.relationBucket = relationBucket(decision.aiJudgeScore);
        p.totalSample = 0;
        p.totalHit = 0;
        p.totalHitRate = 0.0d;
        p.recentSample = 0;
        p.recentHit = 0;
        p.recentHitRate = 0.0d;
        p.decayWeight = 1.0d;
        p.activeFlag = 1;
        p.staleFlag = 0;
        p.recoverFlag = 0;
        p.failCount = 0;
        p.recoverCount = 0;
        p.avgFinalP = decision.finalP;
        p.avgEdge = decision.edge;
        p.avgDeepScore = decision.deepScore;
        p.updatedAt = System.currentTimeMillis();

        return p;
    }

    public Models.DeepProfile buildProfile(Models.Trade trade) {
        Models.DeepProfile p = new Models.DeepProfile();

        if (trade == null) {
            p.profileKey = "";
            p.activeFlag = 0;
            p.staleFlag = 1;
            return p;
        }

        p.profileKey = buildProfileKey(trade);
        p.position = trade.position;
        p.stateMode = safe(trade.stateMode);
        p.trendBucket = trendBucket(trade.trendLife);
        p.driftBucket = driftBucket(trade.driftRisk);
        p.breakBucket = breakBucket(trade.breakDepth);
        p.recoveryBucket = recoveryBucket(trade.recoveryScore);
        p.cycleBucket = cycleBucket(trade.deepScore);
        p.relationBucket = relationBucket(trade.aiJudgeScore);
        p.totalSample = 0;
        p.totalHit = 0;
        p.totalHitRate = 0.0d;
        p.recentSample = 0;
        p.recentHit = 0;
        p.recentHitRate = 0.0d;
        p.decayWeight = 1.0d;
        p.activeFlag = 1;
        p.staleFlag = 0;
        p.recoverFlag = 0;
        p.failCount = 0;
        p.recoverCount = 0;
        p.avgFinalP = trade.finalP;
        p.avgEdge = trade.edge;
        p.avgDeepScore = trade.deepScore;
        p.updatedAt = System.currentTimeMillis();

        return p;
    }

    public String buildProfileKey(Models.ComboCandidate candidate) {
        if (candidate == null) {
            return "";
        }

        return buildProfileKey(
                candidate.position,
                candidate.stateMode,
                candidate.trendLife,
                candidate.driftRisk,
                candidate.breakDepth,
                candidate.recoveryScore,
                candidate.deepCycleScore,
                candidate.deepTransitionScore);
    }

    public String buildProfileKey(Models.Decision decision) {
        if (decision == null) {
            return "";
        }

        return buildProfileKey(
                decision.position,
                decision.stateMode,
                decision.trendLife,
                decision.driftRisk,
                decision.breakDepth,
                decision.recoveryScore,
                decision.deepScore,
                decision.aiJudgeScore);
    }

    public String buildProfileKey(Models.Trade trade) {
        if (trade == null) {
            return "";
        }

        return buildProfileKey(
                trade.position,
                trade.stateMode,
                trade.trendLife,
                trade.driftRisk,
                trade.breakDepth,
                trade.recoveryScore,
                trade.deepScore,
                trade.aiJudgeScore);
    }

    public String buildProfileKey(
            int position,
            String stateMode,
            double trendLife,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double cycleScore,
            double relationScore) {
        if (!AppConfig.isValidPosition(position)) {
            return "";
        }

        String mode = safe(stateMode);

        if (mode.length() == 0) {
            mode = AppConfig.MODE_UNKNOWN;
        }

        return "DP_P"
                + position
                + "_M"
                + normalizeMode(mode)
                + "_T"
                + trendBucket(trendLife)
                + "_D"
                + driftBucket(driftRisk)
                + "_B"
                + breakBucket(breakDepth)
                + "_R"
                + recoveryBucket(recoveryScore)
                + "_C"
                + cycleBucket(cycleScore)
                + "_L"
                + relationBucket(relationScore);
    }

    public Models.DeepProfile learnProfile(
            Models.DeepProfile oldProfile, Models.Trade trade, Models.Draw actualDraw) {
        Models.DeepProfile base;

        if (oldProfile == null) {
            base = buildProfile(trade);
        } else {
            base = copyProfile(oldProfile);
        }

        if (trade == null || actualDraw == null || !actualDraw.isValid()) {
            return base;
        }

        if (!isSameIssue(trade.targetIssue, actualDraw.issue)) {
            return base;
        }

        if (!AppConfig.isValidPosition(trade.position) || !Models.isValidK3Mask(trade.k3Mask)) {
            return base;
        }

        int actual = actualDraw.getNumberAtPosition(trade.position);

        if (!AppConfig.isValidNumber(actual)) {
            return base;
        }

        int hit = Models.maskContains(trade.k3Mask, actual) ? 1 : 0;
        double edge = trade.edge;

        if (edge == 0.0d && trade.finalP > 0.0d && trade.dynamicGate > 0.0d) {
            edge = trade.finalP - trade.dynamicGate;
        }

        updateProfileWithResult(
                base, trade.finalP, edge, trade.deepScore, hit, System.currentTimeMillis());

        return base;
    }

    public Models.DeepProfile learnProfile(
            Models.DeepProfile oldProfile, Models.Decision decision, Models.Draw actualDraw) {
        Models.DeepProfile base;

        if (oldProfile == null) {
            base = buildProfile(decision);
        } else {
            base = copyProfile(oldProfile);
        }

        if (decision == null || actualDraw == null || !actualDraw.isValid()) {
            return base;
        }

        if (!isSameIssue(decision.targetIssue, actualDraw.issue)) {
            return base;
        }

        if (!AppConfig.isValidPosition(decision.position)
                || !Models.isValidK3Mask(decision.k3Mask)) {
            return base;
        }

        int actual = actualDraw.getNumberAtPosition(decision.position);

        if (!AppConfig.isValidNumber(actual)) {
            return base;
        }

        int hit = Models.maskContains(decision.k3Mask, actual) ? 1 : 0;
        double edge = decision.edge;

        if (edge == 0.0d && decision.finalP > 0.0d && decision.dynamicGate > 0.0d) {
            edge = decision.finalP - decision.dynamicGate;
        }

        updateProfileWithResult(
                base, decision.finalP, edge, decision.deepScore, hit, System.currentTimeMillis());

        return base;
    }

    public Models.DeepProfile learnProfile(
            Models.DeepProfile oldProfile,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        Models.DeepProfile base;

        if (oldProfile == null) {
            base = buildProfile(candidate);
        } else {
            base = copyProfile(oldProfile);
        }

        if (candidate == null || actualDraw == null || !actualDraw.isValid()) {
            return base;
        }

        if (!isSameIssue(candidate.targetIssue, actualDraw.issue)) {
            return base;
        }

        if (!AppConfig.isValidPosition(candidate.position)
                || !Models.isValidK3Mask(candidate.k3Mask)) {
            return base;
        }

        int actual = actualDraw.getNumberAtPosition(candidate.position);

        if (!AppConfig.isValidNumber(actual)) {
            return base;
        }

        int hit = Models.maskContains(candidate.k3Mask, actual) ? 1 : 0;
        double edge = candidate.edge;

        if (edge == 0.0d && candidate.finalP > 0.0d && candidate.dynamicGate > 0.0d) {
            edge = candidate.finalP - candidate.dynamicGate;
        }

        updateProfileWithResult(
                base, candidate.finalP, edge, candidate.deepScore, hit, System.currentTimeMillis());

        return base;
    }

    public void updateProfileWithResult(
            Models.DeepProfile p,
            double finalP,
            double edge,
            double deepScore,
            int hit,
            long time) {
        if (p == null) {
            return;
        }

        if (time <= 0L) {
            time = System.currentTimeMillis();
        }

        p.totalSample++;

        if (hit == 1) {
            p.totalHit++;
            p.lastHitAt = time;
            p.failCount = 0;
            p.recoverCount++;

            if (p.staleFlag == 1) {
                p.recoverFlag = 1;
            }
        } else {
            p.lastMissAt = time;
            p.failCount++;
            p.recoverFlag = 0;
        }

        updateRecentWindow(p, hit);

        p.avgFinalP = movingAverage(p.avgFinalP, finalP, p.totalSample);
        p.avgEdge = movingAverage(p.avgEdge, edge, p.totalSample);
        p.avgDeepScore = movingAverage(p.avgDeepScore, deepScore, p.totalSample);

        p.updatedAt = time;

        recalcProfile(p);
    }

    private void updateRecentWindow(Models.DeepProfile p, int hit) {
        if (p == null) {
            return;
        }

        if (p.recentSample < RECENT_LIMIT) {
            p.recentSample++;

            if (hit == 1) {
                p.recentHit++;
            }
        } else {
            double oldHit = (double) p.recentHit;
            double keep = ((double) (RECENT_LIMIT - 1)) / ((double) RECENT_LIMIT);
            int newRecentHit = (int) Math.round(oldHit * keep + (hit == 1 ? 1.0d : 0.0d));

            if (newRecentHit < 0) {
                newRecentHit = 0;
            }

            if (newRecentHit > RECENT_LIMIT) {
                newRecentHit = RECENT_LIMIT;
            }

            p.recentHit = newRecentHit;
            p.recentSample = RECENT_LIMIT;
        }

        if (p.recentHit > p.recentSample) {
            p.recentHit = p.recentSample;
        }

        if (p.recentHit < 0) {
            p.recentHit = 0;
        }
    }

    public void recalcProfile(Models.DeepProfile p) {
        if (p == null) {
            return;
        }

        p.recalc();

        int stale = isStale(p) ? 1 : 0;
        p.staleFlag = stale;
        p.recoverFlag = isRecovering(p) ? 1 : p.recoverFlag;
        p.activeFlag = isActive(p) ? 1 : 0;
        p.decayWeight = calculateDecayWeight(p);

        if (p.decayWeight <= 0.0d) {
            p.decayWeight = 1.0d;
        }
    }

    public DeepProfileSignal analyzeProfile(Models.DeepProfile profile) {
        DeepProfileSignal s = new DeepProfileSignal();

        if (profile == null) {
            s.profileKey = "";
            s.status = "EMPTY";
            s.summaryText = "深度画像：暂无";
            return s;
        }

        recalcProfile(profile);

        s.profileKey = safe(profile.profileKey);
        s.position = profile.position;
        s.stateMode = safe(profile.stateMode);
        s.totalSample = profile.totalSample;
        s.totalHitRate = profile.totalHitRate;
        s.recentSample = profile.recentSample;
        s.recentHitRate = profile.recentHitRate;
        s.decayWeight = profile.decayWeight;
        s.activeFlag = profile.activeFlag;
        s.staleFlag = profile.staleFlag;
        s.recoverFlag = profile.recoverFlag;
        s.failCount = profile.failCount;
        s.profileScore = profileScore(profile);
        s.riskScore = profileRisk(profile);
        s.confidence = profileConfidence(profile);
        s.status = profileStatus(profile);
        s.summaryText = buildProfileSummary(profile, s);

        return s;
    }

    public DeepProfileSignal analyzeCandidate(
            List<Models.DeepProfile> profiles, Models.ComboCandidate candidate) {
        DeepProfileSignal s = new DeepProfileSignal();

        if (candidate == null) {
            s.status = "EMPTY";
            s.summaryText = "深度画像：候选为空";
            return s;
        }

        String key = buildProfileKey(candidate);
        Models.DeepProfile p = findProfile(profiles, key);

        if (p != null) {
            s = analyzeProfile(copyProfile(p));
            s.profileKey = key;
            s.matchType = "EXACT";
            return s;
        }

        Models.DeepProfile best = findBestProfileForCandidate(profiles, candidate);

        if (best != null) {
            s = analyzeProfile(copyProfile(best));
            s.profileKey = safe(best.profileKey);
            s.matchType = "FUZZY";
            s.summaryText = append(s.summaryText, "模糊匹配当前候选画像，非完全同态，仅弱反哺");
            s.profileScore *= 0.86d;
            s.confidence *= 0.82d;
            return s;
        }

        p = buildProfile(candidate);
        s = analyzeProfile(p);
        s.profileKey = key;
        s.matchType = "NEW";
        s.summaryText = "深度画像：新状态，无历史画像，只记录观察，不强反哺";

        return s;
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
            DeepProfileSignal signal = analyzeCandidate(profiles, copy);
            applyToCandidate(copy, signal);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, DeepProfileSignal signal) {
        if (candidate == null) {
            return;
        }

        if (signal == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "深度画像：暂无");
            return;
        }

        double profile01 = AppConfig.clampScore01(signal.profileScore / 100.0d);
        double confidence01 = AppConfig.clampScore01(signal.confidence / 100.0d);
        double risk01 = AppConfig.clampScore01(signal.riskScore / 100.0d);

        candidate.deepProfileKey = signal.profileKey;
        candidate.profileScore =
                blend01(
                        candidate.profileScore,
                        profile01,
                        "EXACT".equals(signal.matchType) ? 0.22d : 0.12d);

        if (signal.confidence >= 50.0d) {
            candidate.deepConfidence = blend01(candidate.deepConfidence, confidence01, 0.12d);
        }

        if (signal.isStrong() && candidate.finalP >= 0.360d && candidate.evidenceRisk <= 0.620d) {
            double boost = "EXACT".equals(signal.matchType) ? 0.0035d : 0.0018d;
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + boost);
            candidate.rawP = AppConfig.clampProbability(candidate.rawP + boost * 0.60d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        if (signal.profileScore >= PROFILE_A_PLUS_SCORE
                && signal.riskScore <= 42.0d
                && signal.confidence >= 62.0d
                && "EXACT".equals(signal.matchType)) {
            candidate.aPlusScore = blend01(candidate.aPlusScore, profile01, 0.12d);
        }

        if (signal.staleFlag == 1 && signal.recoverFlag == 0) {
            candidate.evidenceRisk =
                    blend01(
                            candidate.evidenceRisk,
                            Math.max(candidate.evidenceRisk, risk01),
                            0.12d);

            if (candidate.finalP < 0.430d) {
                candidate.blockCode =
                        pickBlockCode(candidate.blockCode, AppConfig.BLOCK_BACKTEST_WEAK);
            }
        }

        if (signal.failCount >= FAIL_GUARD_COUNT && candidate.finalP < 0.420d) {
            candidate.blockCode =
                    pickBlockCode(candidate.blockCode, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        if (signal.riskScore >= 78.0d && candidate.finalP < 0.430d) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, AppConfig.BLOCK_BACKTEST_WEAK);
        }

        if (signal.recoverFlag == 1
                && signal.recentHitRate >= RECOVER_HIT_RATE
                && candidate.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            candidate.recoveryP = blend01(candidate.recoveryP, signal.recentHitRate, 0.08d);
            candidate.recoveryScore = Math.max(candidate.recoveryScore, 55.0d);
        }

        candidate.edge = candidate.finalP - candidate.dynamicGate;

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "深度画像：key="
                                + signal.profileKey
                                + "，匹配="
                                + signal.matchType
                                + "，画像分="
                                + UiFormatter.score(signal.profileScore)
                                + "，风险="
                                + UiFormatter.score(signal.riskScore)
                                + "，置信="
                                + UiFormatter.score(signal.confidence)
                                + "，样本="
                                + signal.totalSample
                                + "，总命中="
                                + UiFormatter.probability(signal.totalHitRate)
                                + "，近期="
                                + UiFormatter.probability(signal.recentHitRate)
                                + "，状态="
                                + signal.status);
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

        DeepProfileSignal signal = analyzeCandidate(profiles, candidate);

        set.profileKey = signal.profileKey;
        set.globalTrendStrength = Math.max(set.globalTrendStrength, signal.profileScore);
        set.globalBiasScore = Math.max(set.globalBiasScore, signal.confidence);
        set.summaryText = append(set.summaryText, signal.summaryText);

        return set;
    }

    public Models.DeepProfile findProfile(List<Models.DeepProfile> profiles, String profileKey) {
        if (profiles == null || profileKey == null || profileKey.length() == 0) {
            return null;
        }

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            if (profileKey.equals(p.profileKey)) {
                return p;
            }
        }

        return null;
    }

    public Models.DeepProfile findBestProfileForCandidate(
            List<Models.DeepProfile> profiles, Models.ComboCandidate candidate) {
        if (profiles == null || profiles.size() == 0 || candidate == null) {
            return null;
        }

        Models.DeepProfile best = null;
        double bestScore = -999.0d;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            double score = profileMatchScore(candidate, p);

            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        if (bestScore >= 42.0d) {
            return best;
        }

        return null;
    }

    private double profileMatchScore(Models.ComboCandidate candidate, Models.DeepProfile p) {
        if (candidate == null || p == null) {
            return -999.0d;
        }

        double score = 0.0d;

        if (candidate.position == p.position) {
            score += 30.0d;
        }

        if (safe(candidate.stateMode).equals(safe(p.stateMode))) {
            score += 22.0d;
        }

        if (trendBucket(candidate.trendLife) == p.trendBucket) {
            score += 8.0d;
        }

        if (driftBucket(candidate.driftRisk) == p.driftBucket) {
            score += 8.0d;
        }

        if (breakBucket(candidate.breakDepth) == p.breakBucket) {
            score += 8.0d;
        }

        if (recoveryBucket(candidate.recoveryScore) == p.recoveryBucket) {
            score += 8.0d;
        }

        if (cycleBucket(candidate.deepCycleScore) == p.cycleBucket) {
            score += 5.0d;
        }

        if (relationBucket(candidate.deepTransitionScore) == p.relationBucket) {
            score += 5.0d;
        }

        score += Math.min(8.0d, ((double) p.totalSample) / 60.0d * 8.0d);

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            score -= 16.0d;
        }

        if (p.activeFlag == 0) {
            score -= 18.0d;
        }

        return score;
    }

    public void replaceProfile(List<Models.DeepProfile> profiles, Models.DeepProfile updated) {
        if (profiles == null
                || updated == null
                || updated.profileKey == null
                || updated.profileKey.length() == 0) {
            return;
        }

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            if (updated.profileKey.equals(p.profileKey)) {
                profiles.set(i, updated);
                return;
            }
        }

        profiles.add(updated);
    }

    public ArrayList<Models.DeepProfile> sortProfiles(List<Models.DeepProfile> profiles) {
        ArrayList<Models.DeepProfile> out = new ArrayList<Models.DeepProfile>();

        if (profiles == null) {
            return out;
        }

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p != null) {
                Models.DeepProfile copy = copyProfile(p);
                recalcProfile(copy);
                out.add(copy);
            }
        }

        Collections.sort(
                out,
                new Comparator<Models.DeepProfile>() {
                    @Override
                    public int compare(Models.DeepProfile a, Models.DeepProfile b) {
                        double sa = profileScore(a) - profileRisk(a) * 0.16d;
                        double sb = profileScore(b) - profileRisk(b) * 0.16d;

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        String ka = a == null ? "" : safe(a.profileKey);
                        String kb = b == null ? "" : safe(b.profileKey);

                        return ka.compareTo(kb);
                    }
                });

        return out;
    }

    public String buildSummary(List<Models.DeepProfile> profiles) {
        if (profiles == null || profiles.size() == 0) {
            return "深度画像：暂无";
        }

        ArrayList<Models.DeepProfile> list = sortProfiles(profiles);
        StringBuilder sb = new StringBuilder();

        int limit = Math.min(3, list.size());

        for (int i = 0; i < limit; i++) {
            Models.DeepProfile p = list.get(i);

            if (p == null) {
                continue;
            }

            DeepProfileSignal s = analyzeProfile(p);

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(p.position);
            sb.append(" ");
            sb.append(s.status);
            sb.append(" 分=");
            sb.append(UiFormatter.score(s.profileScore));
            sb.append(" 风险=");
            sb.append(UiFormatter.score(s.riskScore));
            sb.append(" 样本=");
            sb.append(p.totalSample);
            sb.append(" 命中=");
            sb.append(UiFormatter.probability(p.totalHitRate));
            sb.append(" 近=");
            sb.append(UiFormatter.probability(p.recentHitRate));
        }

        return sb.length() == 0 ? "深度画像：暂无有效画像" : limitText(sb.toString(), MAX_PROFILE_SUMMARY);
    }

    public Models.DeepProfile copyProfile(Models.DeepProfile old) {
        Models.DeepProfile p = new Models.DeepProfile();

        if (old == null) {
            return p;
        }

        p.profileKey = old.profileKey;
        p.position = old.position;
        p.stateMode = old.stateMode;
        p.trendBucket = old.trendBucket;
        p.driftBucket = old.driftBucket;
        p.breakBucket = old.breakBucket;
        p.recoveryBucket = old.recoveryBucket;
        p.cycleBucket = old.cycleBucket;
        p.relationBucket = old.relationBucket;
        p.totalSample = old.totalSample;
        p.totalHit = old.totalHit;
        p.totalHitRate = old.totalHitRate;
        p.recentSample = old.recentSample;
        p.recentHit = old.recentHit;
        p.recentHitRate = old.recentHitRate;
        p.decayWeight = old.decayWeight;
        p.activeFlag = old.activeFlag;
        p.staleFlag = old.staleFlag;
        p.recoverFlag = old.recoverFlag;
        p.lastHitAt = old.lastHitAt;
        p.lastMissAt = old.lastMissAt;
        p.failCount = old.failCount;
        p.recoverCount = old.recoverCount;
        p.avgFinalP = old.avgFinalP;
        p.avgEdge = old.avgEdge;
        p.avgDeepScore = old.avgDeepScore;
        p.updatedAt = old.updatedAt;

        return p;
    }

    private void saveProfileState(DbStore store, Models.DeepProfile p, String source) {
        if (store == null || p == null) {
            return;
        }

        try {
            recalcProfile(p);
            store.saveDeepProfile(p);

            DeepProfileSignal s = analyzeProfile(copyProfile(p));

            store.setState("deep_profile_summary", s.summaryText);
            store.setState("deep_profile_key", safe(p.profileKey));
            store.setState("deep_profile_source", safe(source));
            store.setState("deep_profile_status", s.status);
            store.setState("deep_profile_score", String.valueOf(s.profileScore));
            store.setState("deep_profile_risk", String.valueOf(s.riskScore));
            store.setState("deep_profile_sample", String.valueOf(p.totalSample));
            store.setState("deep_profile_recent_hit_rate", String.valueOf(p.recentHitRate));
        } catch (Throwable e) {
        }
    }

    private boolean isActive(Models.DeepProfile p) {
        if (p == null) {
            return false;
        }

        if (p.totalSample <= 0) {
            return true;
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            return false;
        }

        if (p.failCount >= FAIL_INACTIVE_COUNT && p.recentHitRate < RECOVER_HIT_RATE) {
            return false;
        }

        return true;
    }

    private boolean isStale(Models.DeepProfile p) {
        if (p == null) {
            return true;
        }

        if (p.totalSample < AppConfig.PROMOTION_BASE_SAMPLE_MIN) {
            return false;
        }

        if (p.recentSample >= RECENT_MIN_FOR_JUDGE && p.recentHitRate < STALE_HIT_RATE) {
            return true;
        }

        if (p.failCount >= FAIL_STALE_COUNT) {
            return true;
        }

        return false;
    }

    private boolean isRecovering(Models.DeepProfile p) {
        if (p == null) {
            return false;
        }

        if (p.staleFlag == 0) {
            return false;
        }

        if (p.recentSample >= RECENT_MIN_FOR_JUDGE && p.recentHitRate >= RECOVER_HIT_RATE) {
            return true;
        }

        if (p.recoverCount >= 3 && p.failCount <= 2) {
            return true;
        }

        return false;
    }

    private double calculateDecayWeight(Models.DeepProfile p) {
        if (p == null) {
            return 0.0d;
        }

        double w = 1.0d;

        if (p.totalSample < AppConfig.PROMOTION_BASE_SAMPLE_MIN) {
            w *= 0.72d;
        }

        if (p.staleFlag == 1) {
            w *= 0.58d;
        }

        if (p.recoverFlag == 1) {
            w *= 0.88d;
        }

        if (p.failCount >= 6) {
            w *= 0.72d;
        }

        if (p.recentSample >= RECENT_MIN_FOR_JUDGE && p.recentHitRate >= RECOVER_HIT_RATE) {
            w *= 1.08d;
        }

        if (p.totalSample >= 80
                && p.totalHitRate >= STRONG_HIT_RATE
                && p.recentHitRate >= USABLE_HIT_RATE) {
            w *= 1.05d;
        }

        if (w < 0.10d) {
            w = 0.10d;
        }

        if (w > 1.20d) {
            w = 1.20d;
        }

        return w;
    }

    private double profileScore(Models.DeepProfile p) {
        if (p == null) {
            return 0.0d;
        }

        p.recalc();

        double score = 0.0d;

        score += p.totalHitRate * 100.0d * 0.34d;
        score += p.recentHitRate * 100.0d * 0.30d;
        score += sampleScore(p.totalSample) * 0.12d;
        score += sampleScore(p.recentSample) * 0.08d;
        score += p.avgFinalP * 100.0d * 0.06d;
        score += Math.max(0.0d, p.avgEdge) * 100.0d * 0.04d;
        score += AppConfig.clampScore01(p.avgDeepScore) * 100.0d * 0.06d;

        if (p.decayWeight > 0.0d) {
            score *= p.decayWeight;
        }

        if (p.activeFlag == 0) {
            score -= 12.0d;
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            score -= 10.0d;
        }

        if (p.recoverFlag == 1) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double profileRisk(Models.DeepProfile p) {
        if (p == null) {
            return 100.0d;
        }

        p.recalc();

        double risk = 0.0d;

        if (p.totalSample < AppConfig.PROMOTION_BASE_SAMPLE_MIN) {
            risk += 24.0d;
        }

        if (p.recentSample < RECENT_MIN_FOR_JUDGE) {
            risk += 14.0d;
        }

        if (p.totalSample >= AppConfig.PROMOTION_BASE_SAMPLE_MIN
                && p.totalHitRate < STALE_HIT_RATE) {
            risk += 22.0d;
        }

        if (p.recentSample >= RECENT_MIN_FOR_JUDGE && p.recentHitRate < STALE_HIT_RATE) {
            risk += 24.0d;
        }

        if (p.failCount >= FAIL_GUARD_COUNT) {
            risk += Math.min(28.0d, p.failCount * 3.0d);
        }

        if (p.staleFlag == 1) {
            risk += 18.0d;
        }

        if (p.activeFlag == 0) {
            risk += 16.0d;
        }

        if (p.recoverFlag == 1) {
            risk -= 8.0d;
        }

        if (p.totalSample >= 80
                && p.totalHitRate >= STRONG_HIT_RATE
                && p.recentHitRate >= USABLE_HIT_RATE) {
            risk -= 6.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double profileConfidence(Models.DeepProfile p) {
        if (p == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) p.totalSample) / 500.0d) * 38.0d;
        score += Math.min(1.0d, ((double) p.recentSample) / 160.0d) * 24.0d;
        score += Math.max(0.0d, 100.0d - profileRisk(p)) * 0.22d;
        score += profileScore(p) * 0.16d;

        return AppConfig.clampScore100(score);
    }

    private String profileStatus(Models.DeepProfile p) {
        if (p == null) {
            return "EMPTY";
        }

        double score = profileScore(p);
        double risk = profileRisk(p);

        if (p.activeFlag == 0) {
            return "INACTIVE";
        }

        if (p.staleFlag == 1 && p.recoverFlag == 0) {
            return "STALE";
        }

        if (p.recoverFlag == 1) {
            return "RECOVERING";
        }

        if (score >= PROFILE_A_PLUS_SCORE && risk <= 42.0d) {
            return "A_PLUS_PROFILE";
        }

        if (score >= PROFILE_STRONG_SCORE && risk <= 55.0d) {
            return "STRONG_PROFILE";
        }

        if (score >= PROFILE_USABLE_SCORE && risk <= 68.0d) {
            return "USABLE_PROFILE";
        }

        return "WEAK_PROFILE";
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

    private double movingAverage(double oldValue, double newValue, int sample) {
        if (sample <= 1) {
            return newValue;
        }

        double weightNew = 1.0d / ((double) sample);

        if (weightNew < 0.020d) {
            weightNew = 0.020d;
        }

        if (weightNew > 0.250d) {
            weightNew = 0.250d;
        }

        return oldValue * (1.0d - weightNew) + newValue * weightNew;
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

    private int cycleBucket(double v) {
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

    private String normalizeMode(String mode) {
        String m = safe(mode);

        if (m.length() == 0) {
            return "UNKNOWN";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < m.length(); i++) {
            char ch = m.charAt(i);

            if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                sb.append(ch);
            } else {
                sb.append("_");
            }
        }

        return sb.toString();
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

        score += c.profileScore * 100.0d * 0.10d;
        score += c.deepScore * 100.0d * 0.06d;
        score += c.aPlusScore * 100.0d * 0.05d;
        score += c.promotionScore * 100.0d * 0.04d;
        score -= c.evidenceRisk * 100.0d * 0.06d;
        score -= c.auditPenalty * 100.0d * 0.04d;

        if (c.deepProfileKey != null && c.deepProfileKey.length() > 0) {
            score += 2.0d;
        }

        if (c.blockCode != null && c.blockCode.length() > 0) {
            score -= 5.0d;
        }

        return score;
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) {
            weightNew = 0.0d;
        }

        if (weightNew > 1.0d) {
            weightNew = 1.0d;
        }

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
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

    private String buildProfileSummary(Models.DeepProfile p, DeepProfileSignal s) {
        if (p == null || s == null) {
            return "深度画像：暂无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("深度画像P");
        sb.append(p.position);
        sb.append("：");
        sb.append(s.profileKey);

        sb.append("，画像分=");
        sb.append(UiFormatter.score(s.profileScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(s.riskScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(s.confidence));

        sb.append("，样本=");
        sb.append(p.totalSample);

        sb.append("，命中=");
        sb.append(UiFormatter.probability(p.totalHitRate));

        sb.append("，近期=");
        sb.append(UiFormatter.probability(p.recentHitRate));

        sb.append("，失败=");
        sb.append(p.failCount);

        sb.append("，衰减=");
        sb.append(UiFormatter.probability(p.decayWeight));

        sb.append("，状态=");
        sb.append(s.status);

        if (s.matchType != null && s.matchType.length() > 0) {
            sb.append("，匹配=");
            sb.append(s.matchType);
        }

        return limitText(sb.toString(), MAX_PROFILE_SUMMARY);
    }

    private boolean isSameIssue(String targetIssue, String actualIssue) {
        if (targetIssue == null || actualIssue == null) {
            return false;
        }

        return targetIssue.equals(actualIssue);
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return limitText(addText, MAX_REASON_TEXT);
        }

        if (oldText.indexOf(addText) >= 0) {
            return limitText(oldText, MAX_REASON_TEXT);
        }

        return limitText(oldText + "；" + addText, MAX_REASON_TEXT);
    }

    private String limitText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (max <= 0) {
            return text;
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class DeepProfileSignal {
        public String profileKey;
        public String matchType;
        public int position;
        public String stateMode;

        public int totalSample;
        public double totalHitRate;
        public int recentSample;
        public double recentHitRate;
        public double decayWeight;

        public int activeFlag;
        public int staleFlag;
        public int recoverFlag;
        public int failCount;

        public double profileScore;
        public double riskScore;
        public double confidence;

        public String status;
        public String summaryText;

        public DeepProfileSignal() {
            profileKey = "";
            matchType = "";
            position = 0;
            stateMode = "";

            totalSample = 0;
            totalHitRate = 0.0d;
            recentSample = 0;
            recentHitRate = 0.0d;
            decayWeight = 1.0d;

            activeFlag = 0;
            staleFlag = 0;
            recoverFlag = 0;
            failCount = 0;

            profileScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;

            status = "EMPTY";
            summaryText = "";
        }

        public boolean isUsable() {
            return activeFlag == 1
                    && staleFlag == 0
                    && profileScore >= PROFILE_USABLE_SCORE
                    && riskScore <= 68.0d;
        }

        public boolean isStrong() {
            return isUsable()
                    && profileScore >= PROFILE_STRONG_SCORE
                    && confidence >= 52.0d
                    && riskScore <= 55.0d;
        }

        public boolean isAPlus() {
            return isStrong()
                    && profileScore >= PROFILE_A_PLUS_SCORE
                    && confidence >= 62.0d
                    && riskScore <= 42.0d;
        }
    }
}
