package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DeepLearningBridgeEngine {

    public static final String LEARN_SOURCE_TRADE = "TRADE";
    public static final String LEARN_SOURCE_DECISION = "DECISION";
    public static final String LEARN_SOURCE_CANDIDATE = "CANDIDATE";
    public static final String LEARN_SOURCE_BACKTEST = "BACKTEST";
    public static final String LEARN_SOURCE_MANUAL = "MANUAL";
    public static final String LEARN_SOURCE_RUNTIME = "RUNTIME";

    public static final String ACTION_REWARD = "REWARD";
    public static final String ACTION_PUNISH = "PUNISH";
    public static final String ACTION_HOLD = "HOLD";
    public static final String ACTION_WATCH = "WATCH";

    private static final double REWARD_STRONG = 0.080d;
    private static final double REWARD_NORMAL = 0.040d;
    private static final double PUNISH_STRONG = 0.090d;
    private static final double PUNISH_NORMAL = 0.045d;

    private static final double PROFILE_HIT_STRONG = 0.420d;
    private static final double PROFILE_HIT_OK = 0.370d;
    private static final double PROFILE_HIT_WEAK = 0.330d;
    private static final int PROFILE_SAMPLE_MIN = 8;
    private static final int PROFILE_SAMPLE_STRONG = 18;
    private static final int PROFILE_FAIL_HARD = 4;

    private static final double LEARNING_MAX_BOOST = 0.018d;
    private static final double LEARNING_MAX_PENALTY = 0.026d;
    private static final double GATE_MAX_DOWN = 0.006d;
    private static final double GATE_MAX_UP = 0.010d;

    private static final int MAX_SUMMARY_LEN = 420;
    private static final int MAX_REASON_LEN = 360;
    private static final int MAX_CANDIDATE_OUT = 80;
    private static final int MAX_LEARN_CANDIDATES_ONCE = 48;

    private static final Object NO_METHOD = new Object();

    private final AttributionEngine attributionEngine;
    private final DeepProfileEngine deepProfileEngine;
    private final ProfileFallbackEngine profileFallbackEngine;
    private final ProbabilityAuditEngine probabilityAuditEngine;
    private final PromotionGateEngine promotionGateEngine;

    public DeepLearningBridgeEngine() {
        attributionEngine = new AttributionEngine();
        deepProfileEngine = new DeepProfileEngine();
        profileFallbackEngine = new ProfileFallbackEngine();
        probabilityAuditEngine = new ProbabilityAuditEngine();
        promotionGateEngine = new PromotionGateEngine();
    }

    public BridgeResult learnAndPersistFromTrade(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.Trade trade,
            Models.Draw actualDraw) {
        BridgeResult result = learnFromTrade(profiles, trade, actualDraw);
        persistBridgeResult(store, result);
        return result;
    }

    public BridgeResult learnAndPersistFromDecision(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.Decision decision,
            Models.Draw actualDraw) {
        BridgeResult result = learnFromDecision(profiles, decision, actualDraw);
        persistBridgeResult(store, result);
        return result;
    }

    public BridgeResult learnAndPersistFromCandidate(
            DbStore store,
            List<Models.DeepProfile> profiles,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        BridgeResult result = learnFromCandidate(profiles, candidate, actualDraw);
        persistBridgeResult(store, result);
        return result;
    }

    public ArrayList<BridgeResult> learnAndPersistFromCandidates(
            DbStore store,
            List<Models.DeepProfile> profiles,
            List<Models.ComboCandidate> candidates,
            Models.Draw actualDraw,
            int limit) {
        ArrayList<BridgeResult> out = new ArrayList<BridgeResult>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        if (limit <= 0 || limit > candidates.size()) {
            limit = candidates.size();
        }

        if (limit > MAX_LEARN_CANDIDATES_ONCE) {
            limit = MAX_LEARN_CANDIDATES_ONCE;
        }

        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            BridgeResult r = learnFromCandidate(profiles, c, actualDraw);
            persistBridgeResult(store, r);
            out.add(r);

            if (i % 8 == 0) {
                cooperativePause(1L);
            }
        }

        sortBridgeResults(out);

        BridgeResult merged = mergeResults(out);
        persistMergedSummary(store, merged);

        return out;
    }

    public BridgeResult learnFromTrade(
            List<Models.DeepProfile> profiles, Models.Trade trade, Models.Draw actualDraw) {
        BridgeResult result = new BridgeResult();
        result.createdAt = System.currentTimeMillis();
        result.sourceType = LEARN_SOURCE_TRADE;

        if (trade == null) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：Trade为空";
            return result;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：实际开奖号无效";
            return result;
        }

        if (!isTargetMatched(trade.targetIssue, actualDraw.issue)) {
            result.status = "DATA_OFFSET";
            result.summaryText =
                    "学习桥接拒绝：Trade目标期="
                            + safe(trade.targetIssue)
                            + "，实际开奖期="
                            + safe(actualDraw.issue)
                            + "，防止错期反哺。";
            return result;
        }

        fillBasicFromTrade(result, trade, actualDraw);

        Models.AttributionResult attribution = safeAnalyzeTrade(trade, actualDraw, result);
        Models.LearningSample sample = safeBuildLearningSampleFromTrade(trade, actualDraw);
        Models.DeepProfile learnedProfile = updateProfileByTrade(profiles, trade, actualDraw);

        fillResultFromAttribution(result, attribution);
        result.attribution = attribution;
        result.learningSample = sample;
        result.deepProfile = learnedProfile;
        result.profileKey =
                learnedProfile == null
                        ? safe(trade.deepProfileKey)
                        : safe(learnedProfile.profileKey);

        CognitiveSignal signal = buildSignalFromTrade(trade, actualDraw, result.hit == 1);
        fillResultFromSignal(result, signal);

        result.moduleAction = moduleAction(attribution);
        result.moduleWeightDelta =
                moduleWeightDelta(
                        attribution, trade.finalP, trade.evidenceScore, trade.evidenceRisk);
        result.snapshot = buildSnapshot(result);
        result.status = result.hit == 1 ? "LEARN_HIT" : "LEARN_MISS";
        result.summaryText = buildBridgeSummary(result);

        safeApplyAttributionToTrade(trade, attribution);

        return result;
    }

    public BridgeResult learnFromDecision(
            List<Models.DeepProfile> profiles, Models.Decision decision, Models.Draw actualDraw) {
        BridgeResult result = new BridgeResult();
        result.createdAt = System.currentTimeMillis();
        result.sourceType = LEARN_SOURCE_DECISION;

        if (decision == null) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：Decision为空";
            return result;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：实际开奖号无效";
            return result;
        }

        if (!isTargetMatched(decision.targetIssue, actualDraw.issue)) {
            result.status = "DATA_OFFSET";
            result.summaryText =
                    "学习桥接拒绝：Decision目标期="
                            + safe(decision.targetIssue)
                            + "，实际开奖期="
                            + safe(actualDraw.issue)
                            + "，防止错期反哺。";
            return result;
        }

        fillBasicFromDecision(result, decision, actualDraw);

        Models.AttributionResult attribution = safeAnalyzeDecision(decision, actualDraw, result);
        Models.LearningSample sample = safeBuildLearningSampleFromDecision(decision, actualDraw);
        Models.DeepProfile learnedProfile = updateProfileByDecision(profiles, decision, actualDraw);

        fillResultFromAttribution(result, attribution);
        result.attribution = attribution;
        result.learningSample = sample;
        result.deepProfile = learnedProfile;
        result.profileKey =
                learnedProfile == null
                        ? safe(decision.deepProfileKey)
                        : safe(learnedProfile.profileKey);

        CognitiveSignal signal = buildSignalFromDecision(decision, actualDraw, result.hit == 1);
        fillResultFromSignal(result, signal);

        result.moduleAction = moduleAction(attribution);
        result.moduleWeightDelta =
                moduleWeightDelta(
                        attribution,
                        decision.finalP,
                        decision.evidenceScore,
                        decision.evidenceRisk);
        result.snapshot = buildSnapshot(result);
        result.status = result.hit == 1 ? "LEARN_HIT" : "LEARN_MISS";
        result.summaryText = buildBridgeSummary(result);

        safeApplyAttributionToDecision(decision, attribution);

        return result;
    }

    public BridgeResult learnFromCandidate(
            List<Models.DeepProfile> profiles,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        BridgeResult result = new BridgeResult();
        result.createdAt = System.currentTimeMillis();
        result.sourceType = LEARN_SOURCE_CANDIDATE;

        if (candidate == null) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：候选为空";
            return result;
        }

        if (actualDraw == null || !actualDraw.isValid()) {
            result.status = "INVALID";
            result.summaryText = "学习桥接：实际开奖号无效";
            return result;
        }

        if (!isTargetMatched(candidate.targetIssue, actualDraw.issue)) {
            result.status = "DATA_OFFSET";
            result.summaryText =
                    "学习桥接拒绝：Candidate目标期="
                            + safe(candidate.targetIssue)
                            + "，实际开奖期="
                            + safe(actualDraw.issue)
                            + "，防止错期反哺。";
            return result;
        }

        fillBasicFromCandidate(result, candidate, actualDraw);

        Models.AttributionResult attribution = safeAnalyzeCandidate(candidate, actualDraw, result);
        Models.LearningSample sample = safeBuildLearningSampleFromCandidate(candidate, actualDraw);
        Models.DeepProfile learnedProfile =
                updateProfileByCandidate(profiles, candidate, actualDraw);

        fillResultFromAttribution(result, attribution);
        result.attribution = attribution;
        result.learningSample = sample;
        result.deepProfile = learnedProfile;
        result.profileKey =
                learnedProfile == null
                        ? safe(candidate.deepProfileKey)
                        : safe(learnedProfile.profileKey);

        CognitiveSignal signal = buildSignalFromCandidate(candidate, actualDraw, result.hit == 1);
        fillResultFromSignal(result, signal);

        result.moduleAction = moduleAction(attribution);
        result.moduleWeightDelta =
                moduleWeightDelta(
                        attribution,
                        candidate.finalP,
                        candidate.evidenceScore,
                        candidate.evidenceRisk);
        result.snapshot = buildSnapshot(result);
        result.status = result.hit == 1 ? "LEARN_HIT" : "LEARN_MISS";
        result.summaryText = buildBridgeSummary(result);

        return result;
    }

    public ArrayList<BridgeResult> learnFromTrades(
            List<Models.DeepProfile> profiles, List<Models.Trade> trades, Models.Draw actualDraw) {
        ArrayList<BridgeResult> out = new ArrayList<BridgeResult>();

        if (trades == null || trades.size() == 0) {
            return out;
        }

        for (int i = 0; i < trades.size(); i++) {
            Models.Trade t = trades.get(i);

            if (t == null) {
                continue;
            }

            BridgeResult r = learnFromTrade(profiles, t, actualDraw);
            out.add(r);
        }

        sortBridgeResults(out);
        return out;
    }

    public ArrayList<BridgeResult> learnFromDecisions(
            List<Models.DeepProfile> profiles,
            List<Models.Decision> decisions,
            Models.Draw actualDraw) {
        ArrayList<BridgeResult> out = new ArrayList<BridgeResult>();

        if (decisions == null || decisions.size() == 0) {
            return out;
        }

        for (int i = 0; i < decisions.size(); i++) {
            Models.Decision d = decisions.get(i);

            if (d == null) {
                continue;
            }

            BridgeResult r = learnFromDecision(profiles, d, actualDraw);
            out.add(r);
        }

        sortBridgeResults(out);
        return out;
    }

    public ArrayList<BridgeResult> learnFromCandidates(
            List<Models.DeepProfile> profiles,
            List<Models.ComboCandidate> candidates,
            Models.Draw actualDraw,
            int limit) {
        ArrayList<BridgeResult> out = new ArrayList<BridgeResult>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        if (limit <= 0 || limit > candidates.size()) {
            limit = candidates.size();
        }

        if (limit > MAX_LEARN_CANDIDATES_ONCE) {
            limit = MAX_LEARN_CANDIDATES_ONCE;
        }

        for (int i = 0; i < limit; i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            BridgeResult r = learnFromCandidate(profiles, c, actualDraw);
            out.add(r);
        }

        sortBridgeResults(out);
        return out;
    }

    public ArrayList<Models.ComboCandidate> applyLearningToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.DeepProfile> profiles) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<Models.ComboCandidate> safeInput = new ArrayList<Models.ComboCandidate>();

        int inputLimit = Math.min(candidates.size(), MAX_CANDIDATE_OUT);

        for (int i = 0; i < inputLimit; i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            try {
                safeInput.add(c.copy());
            } catch (Throwable e) {
                safeInput.add(c);
            }
        }

        ArrayList<Models.ComboCandidate> step1 = safeApplyDeepProfile(safeInput, profiles);
        ArrayList<Models.ComboCandidate> step2 = safeApplyFallback(step1, profiles);
        ArrayList<Models.ComboCandidate> step3 = safeApplyAudit(step2);
        ArrayList<Models.ComboCandidate> step4 = safeApplyPromotion(step3);

        for (int i = 0; i < step4.size(); i++) {
            Models.ComboCandidate c = step4.get(i);

            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy;

            try {
                copy = c.copy();
            } catch (Throwable e) {
                copy = c;
            }

            Models.DeepProfile directProfile = findBestProfileForCandidate(profiles, copy);
            LearningAdjustment adjustment = buildLearningAdjustment(copy, directProfile);

            applyLearningAdjustment(copy, adjustment);
            applyLearningGuard(copy);

            out.add(copy);

            if (out.size() >= MAX_CANDIDATE_OUT) {
                break;
            }
        }

        sortCandidates(out);
        return out;
    }

    public Models.CandidateBatch applyLearningToBatch(
            Models.CandidateBatch batch, List<Models.DeepProfile> profiles) {
        Models.CandidateBatch out = new Models.CandidateBatch();

        if (batch == null) {
            return out;
        }

        out.baseIssue = batch.baseIssue;
        out.targetIssue = batch.targetIssue;
        out.scanMode = batch.scanMode;
        out.probabilityMode = batch.probabilityMode;
        out.fullScan = batch.fullScan;
        out.scannedCount = batch.scannedCount;
        out.enrichCount = batch.enrichCount;
        out.deepCount = batch.deepCount;

        ArrayList<Models.ComboCandidate> list =
                applyLearningToCandidates(batch.candidates, profiles);

        try {
            out.addAll(list);
        } catch (Throwable e) {
            for (int i = 0; i < list.size(); i++) {
                out.add(list.get(i));
            }
        }

        return out;
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

        Models.ComboCandidate copy;

        try {
            copy = candidate.copy();
        } catch (Throwable e) {
            copy = candidate;
        }

        ArrayList<Models.ComboCandidate> one = new ArrayList<Models.ComboCandidate>();
        one.add(copy);

        ArrayList<Models.ComboCandidate> enhanced = applyLearningToCandidates(one, profiles);

        if (enhanced.size() == 0) {
            return set;
        }

        Models.ComboCandidate best = enhanced.get(0);

        set.bestPosition = best.position;
        set.bestNumbers = new int[] {best.n1, best.n2, best.n3};
        set.profileKey = safe(best.deepProfileKey);
        set.globalBiasScore = Math.max(set.globalBiasScore, best.profileScore * 100.0d);
        set.globalTrendStrength = Math.max(set.globalTrendStrength, best.deepScore * 100.0d);
        set.summaryText =
                append(
                        set.summaryText,
                        "学习桥接增强：画像="
                                + safe(best.deepProfileKey)
                                + "，回退="
                                + safe(best.fallbackProfileKey)
                                + "，profile="
                                + UiFormatter.probability(best.profileScore)
                                + "，fallback="
                                + UiFormatter.probability(best.fallbackProfileScore)
                                + "，promotion="
                                + UiFormatter.probability(best.promotionScore)
                                + "，finalP="
                                + UiFormatter.probability(best.finalP)
                                + "，gate="
                                + UiFormatter.probability(best.dynamicGate)
                                + "，edge="
                                + UiFormatter.signedProbability(best.edge));

        return set;
    }

    public Models.RuntimeLearningSnapshot buildSnapshot(BridgeResult result) {
        Models.RuntimeLearningSnapshot snapshot = new Models.RuntimeLearningSnapshot();

        if (result == null) {
            return snapshot;
        }

        set(snapshot, "snapshotId", "LS_" + result.createdAt + "_" + safe(result.targetIssue));
        set(snapshot, "createdAt", Long.valueOf(result.createdAt));
        set(snapshot, "updatedAt", Long.valueOf(System.currentTimeMillis()));
        set(snapshot, "sourceType", result.sourceType);
        set(snapshot, "source", "DeepLearningBridgeEngine");
        set(snapshot, "targetIssue", result.targetIssue);
        set(snapshot, "baseIssue", result.baseIssue);
        set(snapshot, "profileKey", result.profileKey);
        set(snapshot, "hit", Integer.valueOf(result.hit));
        set(snapshot, "affectedModule", result.affectedModule);
        set(snapshot, "moduleAction", result.moduleAction);
        set(snapshot, "moduleWeightDelta", Double.valueOf(result.moduleWeightDelta));
        set(snapshot, "confidence", Double.valueOf(result.confidence));
        set(snapshot, "mainReasonCode", result.mainReasonCode);
        set(snapshot, "summary", result.summaryText);
        set(snapshot, "summaryText", result.summaryText);
        set(snapshot, "learningSummary", result.summaryText);
        set(snapshot, "learningSummaryText", result.summaryText);

        set(snapshot, "profileSummary", result.profileSummary);
        set(snapshot, "gateAdjustSummary", result.gateAdjustSummary);
        set(snapshot, "probabilityAdjustSummary", result.probabilityAdjustSummary);
        set(snapshot, "auditSummary", result.auditSummary);
        set(snapshot, "biasSummary", result.biasSummary);
        set(snapshot, "promotionSummary", result.promotionSummary);
        set(snapshot, "aPlusSummary", result.aPlusSummary);
        set(snapshot, "activeJson", buildSnapshotLiteJson(result));

        if (result.deepProfile != null) {
            set(snapshot, "totalSample", Integer.valueOf(result.deepProfile.totalSample));
            set(snapshot, "totalHitRate", Double.valueOf(result.deepProfile.totalHitRate));
            set(snapshot, "recentSample", Integer.valueOf(result.deepProfile.recentSample));
            set(snapshot, "recentHitRate", Double.valueOf(result.deepProfile.recentHitRate));
            set(snapshot, "decayWeight", Double.valueOf(result.deepProfile.decayWeight));
            set(snapshot, "failCount", Integer.valueOf(result.deepProfile.failCount));
            set(snapshot, "staleFlag", Integer.valueOf(result.deepProfile.staleFlag));
            set(snapshot, "recoverFlag", Integer.valueOf(result.deepProfile.recoverFlag));
        }

        return snapshot;
    }

    public Models.DeepProfile updateProfileByTrade(
            List<Models.DeepProfile> profiles, Models.Trade trade, Models.Draw actualDraw) {
        if (trade == null) {
            return null;
        }

        try {
            String key = deepProfileEngine.buildProfileKey(trade);
            Models.DeepProfile old = findProfile(profiles, key);
            Models.DeepProfile updated = deepProfileEngine.learnProfile(old, trade, actualDraw);

            replaceProfile(profiles, updated);

            return updated;
        } catch (Throwable e) {
            return null;
        }
    }

    public Models.DeepProfile updateProfileByDecision(
            List<Models.DeepProfile> profiles, Models.Decision decision, Models.Draw actualDraw) {
        if (decision == null) {
            return null;
        }

        try {
            String key = deepProfileEngine.buildProfileKey(decision);
            Models.DeepProfile old = findProfile(profiles, key);
            Models.DeepProfile updated = deepProfileEngine.learnProfile(old, decision, actualDraw);

            replaceProfile(profiles, updated);

            return updated;
        } catch (Throwable e) {
            return null;
        }
    }

    public Models.DeepProfile updateProfileByCandidate(
            List<Models.DeepProfile> profiles,
            Models.ComboCandidate candidate,
            Models.Draw actualDraw) {
        if (candidate == null) {
            return null;
        }

        try {
            String key = deepProfileEngine.buildProfileKey(candidate);
            Models.DeepProfile old = findProfile(profiles, key);
            Models.DeepProfile updated = deepProfileEngine.learnProfile(old, candidate, actualDraw);

            replaceProfile(profiles, updated);

            return updated;
        } catch (Throwable e) {
            return null;
        }
    }

    public Models.DeepProfile findProfile(List<Models.DeepProfile> profiles, String key) {
        if (profiles == null || key == null || key.length() == 0) {
            return null;
        }

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            if (key.equals(p.profileKey)) {
                return p;
            }
        }

        return null;
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

    public BridgeResult mergeResults(List<BridgeResult> results) {
        BridgeResult out = new BridgeResult();
        out.createdAt = System.currentTimeMillis();
        out.sourceType = "MERGED";

        if (results == null || results.size() == 0) {
            out.status = "EMPTY";
            out.summaryText = "学习桥接：暂无学习结果";
            return out;
        }

        int hit = 0;
        int miss = 0;
        double confidenceSum = 0.0d;
        double deltaSum = 0.0d;

        String moduleText = "";
        String reasonText = "";
        String stateText = "";
        String adviceText = "";

        for (int i = 0; i < results.size(); i++) {
            BridgeResult r = results.get(i);

            if (r == null || "INVALID".equals(r.status) || "DATA_OFFSET".equals(r.status)) {
                continue;
            }

            if (r.hit == 1) {
                hit++;
            } else {
                miss++;
            }

            confidenceSum += r.confidence;
            deltaSum += r.moduleWeightDelta;

            moduleText = append(moduleText, r.affectedModule);
            reasonText = append(reasonText, r.mainReasonCode);
            stateText = append(stateText, r.stateKey);
            adviceText = append(adviceText, r.nextAdvice);
        }

        int total = hit + miss;

        out.hit = hit > 0 && hit >= miss ? 1 : 0;
        out.confidence = total > 0 ? confidenceSum / ((double) total) : 0.0d;
        out.moduleWeightDelta = deltaSum;
        out.affectedModule = moduleText;
        out.mainReasonCode = reasonText;
        out.stateKey = stateText;
        out.nextAdvice = adviceText;
        out.moduleAction = deltaSum >= 0.0d ? ACTION_REWARD : ACTION_PUNISH;
        out.status = "MERGED";
        out.summaryText =
                safeLen(
                        "学习桥接汇总：样本="
                                + total
                                + "，命中="
                                + hit
                                + "，未中="
                                + miss
                                + "，平均置信="
                                + UiFormatter.probability(out.confidence)
                                + "，权重变化="
                                + UiFormatter.signedProbability(out.moduleWeightDelta)
                                + "，模块="
                                + moduleText
                                + "，状态="
                                + stateText
                                + "，修正="
                                + adviceText);

        out.snapshot = buildSnapshot(out);

        return out;
    }

    public String buildSummary(BridgeResult result) {
        if (result == null) {
            return "学习桥接：暂无";
        }

        return result.summaryText == null || result.summaryText.length() == 0
                ? buildBridgeSummary(result)
                : result.summaryText;
    }

    public String buildSummaryList(List<BridgeResult> list) {
        if (list == null || list.size() == 0) {
            return "学习桥接：暂无结果";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(5, list.size());

        for (int i = 0; i < limit; i++) {
            BridgeResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append(r.hit == 1 ? "中" : "未中");
            sb.append(" ");
            sb.append(safe(r.affectedModule));
            sb.append(" ");
            sb.append(safe(r.moduleAction));
            sb.append(" Δ=");
            sb.append(UiFormatter.signedProbability(r.moduleWeightDelta));
            sb.append(" ");
            sb.append(safe(r.mainReasonCode));

            if (r.nextAdvice != null && r.nextAdvice.length() > 0) {
                sb.append(" 修正=");
                sb.append(r.nextAdvice);
            }
        }

        return sb.length() == 0 ? "学习桥接：暂无有效结果" : sb.toString();
    }

    private void persistBridgeResult(DbStore store, BridgeResult result) {
        if (store == null || result == null) {
            return;
        }

        if ("INVALID".equals(result.status) || "DATA_OFFSET".equals(result.status)) {
            safeSetState(store, "deep_bridge_status", safe(result.status));
            safeSetState(store, "deep_bridge_summary", safe(result.summaryText));
            return;
        }

        if (result.attribution != null) {
            invokeStoreOptional(
                    store,
                    "saveAttributionResult",
                    new Class[] {Models.AttributionResult.class},
                    new Object[] {result.attribution});
        }

        if (result.deepProfile != null) {
            invokeStoreOptional(
                    store,
                    "saveDeepProfile",
                    new Class[] {Models.DeepProfile.class},
                    new Object[] {result.deepProfile});
        }

        if (result.snapshot == null) {
            result.snapshot = buildSnapshot(result);
        }

        if (result.snapshot != null) {
            invokeStoreOptional(
                    store,
                    "saveLearningSnapshot",
                    new Class[] {Models.RuntimeLearningSnapshot.class},
                    new Object[] {result.snapshot});
            invokeStoreOptional(
                    store,
                    "saveRuntimeLearningSnapshot",
                    new Class[] {Models.RuntimeLearningSnapshot.class},
                    new Object[] {result.snapshot});
            invokeStoreOptional(
                    store,
                    "saveTeacherSnapshot",
                    new Class[] {Models.RuntimeLearningSnapshot.class},
                    new Object[] {result.snapshot});
        }

        Models.LearningCase c = buildLearningCase(result);

        if (c != null) {
            Object r =
                    invokeStoreOptional(
                            store,
                            "saveLearningCase",
                            new Class[] {Models.LearningCase.class},
                            new Object[] {c});

            if (r == NO_METHOD) {
                ArrayList<Models.LearningCase> one = new ArrayList<Models.LearningCase>();
                one.add(c);
                invokeStoreOptional(
                        store,
                        "saveLearningCases",
                        new Class[] {ArrayList.class},
                        new Object[] {one});
                invokeStoreOptional(
                        store, "saveLearningCases", new Class[] {List.class}, new Object[] {one});
            }
        }

        safeSetState(store, "deep_bridge_summary", safe(result.summaryText));
        safeSetState(store, "deep_bridge_status", safe(result.status));
        safeSetState(store, "deep_bridge_action", safe(result.moduleAction));
        safeSetState(store, "deep_bridge_reason", safe(result.mainReasonCode));
        safeSetState(store, "deep_bridge_state_key", safe(result.stateKey));
        safeSetState(store, "deep_bridge_next_advice", safe(result.nextAdvice));
        safeSetState(store, "learning_summary", safe(result.summaryText));

        if (result.profileSummary != null && result.profileSummary.length() > 0) {
            safeSetState(store, "runtime_profile_summary", result.profileSummary);
        }

        if (result.snapshot != null) {
            String id = getFieldText(result.snapshot, "snapshotId");
            if (id.length() > 0) {
                safeSetState(store, "runtime_learning_snapshot_id", id);
                safeSetState(store, "last_snapshot_id", id);
                safeSetState(store, "lastSnapshotId", id);
            }
        }
    }

    private void persistMergedSummary(DbStore store, BridgeResult result) {
        if (store == null || result == null) {
            return;
        }

        safeSetState(store, "deep_bridge_merged_summary", safe(result.summaryText));
        safeSetState(store, "deep_bridge_merged_action", safe(result.moduleAction));
        safeSetState(store, "deep_bridge_merged_delta", String.valueOf(result.moduleWeightDelta));
    }

    private Models.LearningCase buildLearningCase(BridgeResult r) {
        if (r == null || r.targetIssue == null || r.targetIssue.length() == 0) {
            return null;
        }

        if (!AppConfig.isValidPosition(r.position)) {
            return null;
        }

        Models.LearningCase c = new Models.LearningCase();

        c.caseKey =
                "BRIDGE|"
                        + safe(r.sourceType)
                        + "|"
                        + safe(r.targetIssue)
                        + "|P"
                        + r.position
                        + "|K"
                        + r.k3Mask;
        c.taskKey = "DEEP_BRIDGE";
        c.segmentKey = safe(r.sourceType);
        c.baseIssue = safe(r.baseIssue);
        c.targetIssue = safe(r.targetIssue);
        c.createdAt = r.createdAt > 0L ? r.createdAt : System.currentTimeMillis();
        c.targetIndex = -1;
        c.position = r.position;
        c.actualNumber = r.actualNumber;
        c.predictedK3 = r.k3Mask;
        c.k3Mask = r.k3Mask;
        c.hit = r.hit;

        c.stateKey = safe(r.stateKey);
        c.stateMode = safe(r.stateMode);
        c.trendKey = bucketLabel("TR", normalizeScore01(r.trendLife));
        c.shapeKey = safe(r.shapeKey);
        c.driftKey = bucketLabel("DR", normalizeScore01(r.driftRisk));
        c.breakKey = bucketLabel("BR", normalizeScore01(r.breakDepth));
        c.recoveryKey = bucketLabel("RC", normalizeScore01(r.recoveryScore));
        c.transitionKey = safe(r.probabilityMode);

        c.expectedShape = safe(r.expectedShape);
        c.actualShape = safe(r.actualShape);
        c.whyExpectedHit = "深度桥接学习：保存画像、归因、门槛、概率、错因，用于下一次实时候选反哺。";
        c.whyActualHit = r.hit == 1 ? safe(r.actualReason) : "";
        c.whyMiss = r.hit == 1 ? "" : safe(r.failReason);
        c.nextAdvice = safe(r.nextAdvice);
        c.mainReasonCode = safe(r.mainReasonCode);
        c.affectedModule = safe(r.affectedModule);

        c.finalP = r.finalP;
        c.dynamicGate = r.dynamicGate;
        c.edge = r.edge;
        c.trendScore = normalizeScore01(r.trendLife);
        c.shapeScore = normalizeScore01(r.deepScore);
        c.driftRisk = normalizeScore01(r.driftRisk);
        c.breakRisk = normalizeScore01(r.breakDepth);
        c.recoveryScore = normalizeScore01(r.recoveryScore);
        c.profileScore = r.profileScore;
        c.numberBias = r.biasScore;
        c.comboBias = r.comboGap;
        c.riskScore = normalizeScore01(Math.max(r.driftRisk, r.breakDepth));
        c.teacherWeight = r.confidence > 0.0d ? r.confidence : 1.0d;
        c.stateConfidence = r.confidence;
        c.stateSample = r.deepProfile == null ? 0 : r.deepProfile.totalSample;
        c.cacheSample = r.deepProfile == null ? 0 : r.deepProfile.recentSample;
        c.recentSample = r.deepProfile == null ? 0 : r.deepProfile.recentSample;
        c.recentHitRate = r.deepProfile == null ? 0.0d : r.deepProfile.recentHitRate;

        return c;
    }

    private void fillBasicFromTrade(BridgeResult result, Models.Trade t, Models.Draw actual) {
        if (result == null || t == null) {
            return;
        }

        result.baseIssue = safe(t.baseIssue);
        result.targetIssue = safe(t.targetIssue);
        result.position = t.position;
        result.n1 = t.n1;
        result.n2 = t.n2;
        result.n3 = t.n3;
        result.k3Mask = t.k3Mask;
        result.actualNumber = actual == null ? 0 : actual.getNumberAtPosition(t.position);
        result.rawP = t.rawP;
        result.finalP = t.finalP;
        result.dynamicGate = t.dynamicGate;
        result.edge = t.edge == 0.0d ? t.finalP - t.dynamicGate : t.edge;
        result.stateMode = safe(t.stateMode);
        result.shapeKey = safe(t.shapeKey);
        result.probabilityMode = "";
        result.trendLife = t.trendLife;
        result.driftRisk = t.driftRisk;
        result.breakDepth = t.breakDepth;
        result.recoveryScore = t.recoveryScore;
        result.comboGap = t.comboGap;
        result.championGap = t.championGap;
        result.deepScore = t.deepScore;
        result.profileScore = t.profileScore;
        result.biasScore = t.biasScore;
    }

    private void fillBasicFromDecision(BridgeResult result, Models.Decision d, Models.Draw actual) {
        if (result == null || d == null) {
            return;
        }

        result.baseIssue = safe(d.baseIssue);
        result.targetIssue = safe(d.targetIssue);
        result.position = d.position;
        result.n1 = d.n1;
        result.n2 = d.n2;
        result.n3 = d.n3;
        result.k3Mask = d.k3Mask;
        result.actualNumber = actual == null ? 0 : actual.getNumberAtPosition(d.position);
        result.rawP = d.rawP;
        result.finalP = d.finalP;
        result.dynamicGate = d.dynamicGate;
        result.edge = d.edge == 0.0d ? d.finalP - d.dynamicGate : d.edge;
        result.stateMode = safe(d.stateMode);
        result.shapeKey = safe(d.shapeKey);
        result.probabilityMode = safe(d.probabilityMode);
        result.trendLife = d.trendLife;
        result.driftRisk = d.driftRisk;
        result.breakDepth = d.breakDepth;
        result.recoveryScore = d.recoveryScore;
        result.comboGap = d.comboGap;
        result.championGap = d.championGap;
        result.deepScore = d.deepScore;
        result.profileScore = d.profileScore;
        result.biasScore = d.biasScore;
    }

    private void fillBasicFromCandidate(
            BridgeResult result, Models.ComboCandidate c, Models.Draw actual) {
        if (result == null || c == null) {
            return;
        }

        result.baseIssue = safe(c.baseIssue);
        result.targetIssue = safe(c.targetIssue);
        result.position = c.position;
        result.n1 = c.n1;
        result.n2 = c.n2;
        result.n3 = c.n3;
        result.k3Mask = c.k3Mask;
        result.actualNumber = actual == null ? 0 : actual.getNumberAtPosition(c.position);
        result.rawP = c.rawP;
        result.finalP = c.finalP;
        result.dynamicGate = c.dynamicGate;
        result.edge = c.edge == 0.0d ? c.finalP - c.dynamicGate : c.edge;
        result.stateMode = safe(c.stateMode);
        result.shapeKey = safe(c.shapeKey);
        result.probabilityMode = safe(c.probabilityMode);
        result.trendLife = c.trendLife;
        result.driftRisk = c.driftRisk;
        result.breakDepth = c.breakDepth;
        result.recoveryScore = c.recoveryScore;
        result.comboGap = c.comboGap;
        result.championGap = c.championGap;
        result.deepScore = c.deepScore;
        result.profileScore = c.profileScore;
        result.biasScore = c.biasScore;
    }

    private void fillResultFromAttribution(
            BridgeResult result, Models.AttributionResult attribution) {
        if (result == null || attribution == null) {
            return;
        }

        if (attribution.targetIssue != null && attribution.targetIssue.length() > 0) {
            result.targetIssue = safe(attribution.targetIssue);
        }

        result.hit = attribution.hit;
        result.mainReasonCode = safe(attribution.mainReasonCode);
        result.mainReasonText = safe(attribution.mainReasonText);
        result.affectedModule = safe(attribution.affectedModule);
        result.confidence = attribution.confidence;
        result.shouldReward = attribution.shouldReward;
        result.shouldPunish = attribution.shouldPunish;
        result.positionImpact = attribution.positionImpact;
        result.numberImpact = attribution.numberImpact;
        result.cycleImpact = attribution.cycleImpact;
        result.transitionImpact = attribution.transitionImpact;
        result.calibrationImpact = attribution.calibrationImpact;
        result.learningText = safe(attribution.learningText);
    }

    private void fillResultFromSignal(BridgeResult result, CognitiveSignal signal) {
        if (result == null || signal == null) {
            return;
        }

        result.position = signal.position;
        result.actualNumber = signal.actualNumber;
        result.predictedText = signal.predictedText;
        result.stateKey = signal.stateKey;
        result.expectedShape = signal.expectedShape;
        result.actualShape = signal.actualShape;
        result.actualReason = signal.actualReason;
        result.failReason = signal.failReason;
        result.nextAdvice = signal.nextAdvice;
        result.profileSummary = signal.profileSummary;
        result.gateAdjustSummary = signal.gateAdjustSummary;
        result.probabilityAdjustSummary = signal.probabilityAdjustSummary;
        result.auditSummary = signal.auditSummary;
        result.biasSummary = signal.biasSummary;
        result.promotionSummary = signal.promotionSummary;
        result.aPlusSummary = signal.aPlusSummary;
    }

    private Models.AttributionResult safeAnalyzeTrade(
            Models.Trade trade, Models.Draw draw, BridgeResult base) {
        try {
            Models.AttributionResult r = attributionEngine.analyzeTrade(trade, draw);
            if (r != null) {
                return r;
            }
        } catch (Throwable e) {
        }

        return buildFallbackAttribution(base);
    }

    private Models.AttributionResult safeAnalyzeDecision(
            Models.Decision d, Models.Draw draw, BridgeResult base) {
        try {
            Models.AttributionResult r = attributionEngine.analyzeDecision(d, draw);
            if (r != null) {
                return r;
            }
        } catch (Throwable e) {
        }

        return buildFallbackAttribution(base);
    }

    private Models.AttributionResult safeAnalyzeCandidate(
            Models.ComboCandidate c, Models.Draw draw, BridgeResult base) {
        try {
            Models.AttributionResult r = attributionEngine.analyzeCandidate(c, draw);
            if (r != null) {
                return r;
            }
        } catch (Throwable e) {
        }

        return buildFallbackAttribution(base);
    }

    private Models.AttributionResult buildFallbackAttribution(BridgeResult base) {
        Models.AttributionResult r = new Models.AttributionResult();

        if (base == null) {
            return r;
        }

        boolean hit = maskContains(base.k3Mask, base.actualNumber);

        r.targetIssue = safe(base.targetIssue);
        r.hit = hit ? 1 : 0;
        r.mainReasonCode = hit ? "BRIDGE_FALLBACK_HIT" : "BRIDGE_FALLBACK_MISS";
        r.mainReasonText = hit ? "桥接兜底归因：实际号码落入K3" : "桥接兜底归因：实际号码未落入K3";
        r.affectedModule = "DEEP_BRIDGE_FALLBACK";
        r.confidence = 0.42d;
        r.shouldReward = hit ? 1 : 0;
        r.shouldPunish = hit ? 0 : 1;
        r.positionImpact = 0.20d;
        r.numberImpact = 0.35d;
        r.cycleImpact = 0.10d;
        r.transitionImpact = 0.10d;
        r.calibrationImpact = 0.10d;
        r.learningText = r.mainReasonText;

        return r;
    }

    private Models.LearningSample safeBuildLearningSampleFromTrade(
            Models.Trade trade, Models.Draw draw) {
        try {
            return attributionEngine.buildLearningSample(trade, draw);
        } catch (Throwable e) {
            return null;
        }
    }

    private Models.LearningSample safeBuildLearningSampleFromDecision(
            Models.Decision d, Models.Draw draw) {
        try {
            return attributionEngine.buildLearningSample(d, draw);
        } catch (Throwable e) {
            return null;
        }
    }

    private Models.LearningSample safeBuildLearningSampleFromCandidate(
            Models.ComboCandidate c, Models.Draw draw) {
        try {
            return attributionEngine.buildLearningSample(c, draw);
        } catch (Throwable e) {
            return null;
        }
    }

    private void safeApplyAttributionToTrade(
            Models.Trade trade, Models.AttributionResult attribution) {
        try {
            attributionEngine.applyAttributionToTrade(trade, attribution);
        } catch (Throwable e) {
        }
    }

    private void safeApplyAttributionToDecision(
            Models.Decision decision, Models.AttributionResult attribution) {
        try {
            attributionEngine.applyAttributionToDecision(decision, attribution);
        } catch (Throwable e) {
        }
    }

    private String moduleAction(Models.AttributionResult attribution) {
        if (attribution == null) {
            return ACTION_WATCH;
        }

        if (attribution.shouldReward == 1 && attribution.hit == 1) {
            return ACTION_REWARD;
        }

        if (attribution.shouldPunish == 1 && attribution.hit == 0) {
            return ACTION_PUNISH;
        }

        return ACTION_WATCH;
    }

    private double moduleWeightDelta(
            Models.AttributionResult attribution,
            double finalP,
            double evidenceScore,
            double evidenceRisk) {
        if (attribution == null) {
            return 0.0d;
        }

        double confidence = AppConfig.clampScore01(attribution.confidence);
        double delta;

        if (attribution.hit == 1) {
            delta = confidence >= 0.620d ? REWARD_STRONG : REWARD_NORMAL;

            if (finalP >= 0.400d) {
                delta += 0.010d;
            }

            if (evidenceScore >= 0.620d && evidenceRisk <= 0.480d) {
                delta += 0.010d;
            }

            if (isModuleLike(attribution.affectedModule, "A_PLUS")
                    || isModuleLike(attribution.affectedModule, "AI")
                    || isModuleLike(attribution.affectedModule, "EVIDENCE")) {
                delta += 0.010d;
            }
        } else {
            delta = confidence >= 0.620d ? -PUNISH_STRONG : -PUNISH_NORMAL;

            if (finalP >= 0.410d) {
                delta -= 0.012d;
            }

            if (evidenceRisk >= 0.620d) {
                delta -= 0.010d;
            }

            if (isModuleLike(attribution.affectedModule, "CALIBRATION")
                    || isModuleLike(attribution.affectedModule, "DRIFT")
                    || isModuleLike(attribution.affectedModule, "BREAK")) {
                delta -= 0.010d;
            }
        }

        if (delta > 0.120d) {
            delta = 0.120d;
        }

        if (delta < -0.140d) {
            delta = -0.140d;
        }

        return delta;
    }

    private boolean isModuleLike(String module, String key) {
        if (module == null || key == null) {
            return false;
        }

        return module.indexOf(key) >= 0;
    }

    private Models.DeepProfile findBestProfileForCandidate(
            List<Models.DeepProfile> profiles, Models.ComboCandidate c) {
        if (profiles == null || c == null) {
            return null;
        }

        String directKey = "";

        try {
            directKey = deepProfileEngine.buildProfileKey(c);
        } catch (Throwable e) {
            directKey = "";
        }

        Models.DeepProfile direct = findProfile(profiles, directKey);

        if (direct != null) {
            return direct;
        }

        if (c.deepProfileKey != null && c.deepProfileKey.length() > 0) {
            direct = findProfile(profiles, c.deepProfileKey);

            if (direct != null) {
                return direct;
            }
        }

        Models.DeepProfile best = null;
        double bestScore = -999.0d;

        for (int i = 0; i < profiles.size(); i++) {
            Models.DeepProfile p = profiles.get(i);

            if (p == null) {
                continue;
            }

            double score = profileMatchScore(c, p);

            if (score > bestScore) {
                bestScore = score;
                best = p;
            }
        }

        return bestScore >= 40.0d ? best : null;
    }

    private double profileMatchScore(Models.ComboCandidate c, Models.DeepProfile p) {
        if (c == null || p == null) {
            return -999.0d;
        }

        double score = 0.0d;

        if (p.position == c.position) {
            score += 30.0d;
        }

        if (safe(p.stateMode).equals(safe(c.stateMode))) {
            score += 24.0d;
        }

        if (bucketInt(c.trendLife) == p.trendBucket) {
            score += 8.0d;
        }

        if (bucketInt(c.driftRisk) == p.driftBucket) {
            score += 8.0d;
        }

        if (bucketInt(c.breakDepth) == p.breakBucket) {
            score += 8.0d;
        }

        if (bucketInt(c.recoveryScore) == p.recoveryBucket) {
            score += 8.0d;
        }

        score += Math.min(10.0d, p.totalSample * 0.20d);

        if (p.staleFlag == 1) {
            score -= 18.0d;
        }

        return score;
    }

    private LearningAdjustment buildLearningAdjustment(
            Models.ComboCandidate c, Models.DeepProfile profile) {
        LearningAdjustment a = new LearningAdjustment();

        if (c == null) {
            return a;
        }

        a.stateKey = buildStateKey(c);
        a.expectedShape = buildCandidateShape(c);
        a.profileKey = profile == null ? safe(c.deepProfileKey) : safe(profile.profileKey);

        if (profile == null) {
            a.reason = "无匹配深度画像，仅保留原候选评分";
            return a;
        }

        double totalHr = AppConfig.clampProbability(profile.totalHitRate);
        double recentHr = AppConfig.clampProbability(profile.recentHitRate);
        double blendedHr;

        if (profile.recentSample >= PROFILE_SAMPLE_MIN) {
            blendedHr = totalHr * 0.45d + recentHr * 0.55d;
        } else {
            blendedHr = totalHr;
        }

        double sampleConfidence = profileSampleConfidence(profile);
        double risk = 0.0d;
        double support = 0.0d;

        if (profile.totalSample >= PROFILE_SAMPLE_MIN && blendedHr >= PROFILE_HIT_OK) {
            support += (blendedHr - PROFILE_HIT_OK) * 0.70d;
            support += sampleConfidence * 0.012d;
        }

        if (profile.totalSample >= PROFILE_SAMPLE_STRONG && blendedHr >= PROFILE_HIT_STRONG) {
            support += 0.008d;
        }

        if (profile.recoverFlag == 1 && blendedHr >= PROFILE_HIT_OK) {
            support += 0.004d;
        }

        if (profile.staleFlag == 1) {
            risk += 0.014d;
        }

        if (profile.failCount >= PROFILE_FAIL_HARD) {
            risk += 0.012d;
        }

        if (profile.totalSample >= PROFILE_SAMPLE_MIN && blendedHr < PROFILE_HIT_WEAK) {
            risk += (PROFILE_HIT_WEAK - blendedHr) * 0.85d;
        }

        if (c.evidenceRisk >= 0.620d) {
            risk += 0.006d;
        }

        if (c.auditPenalty >= 0.300d) {
            risk += 0.005d;
        }

        if (support > LEARNING_MAX_BOOST) {
            support = LEARNING_MAX_BOOST;
        }

        if (risk > LEARNING_MAX_PENALTY) {
            risk = LEARNING_MAX_PENALTY;
        }

        a.probabilityBoost = support;
        a.probabilityPenalty = risk;
        a.gateDown = Math.min(GATE_MAX_DOWN, support * 0.35d);
        a.gateUp = Math.min(GATE_MAX_UP, risk * 0.45d);
        a.supportScore = blendedHr;
        a.confidence = sampleConfidence;
        a.profileSample = profile.totalSample;
        a.profileRecentSample = profile.recentSample;
        a.profileFailCount = profile.failCount;
        a.stale = profile.staleFlag;

        StringBuilder sb = new StringBuilder();
        sb.append("画像样本=");
        sb.append(profile.totalSample);
        sb.append("，总HR=");
        sb.append(UiFormatter.probability(totalHr));
        sb.append("，近HR=");
        sb.append(UiFormatter.probability(recentHr));
        sb.append("，融合HR=");
        sb.append(UiFormatter.probability(blendedHr));

        if (support > 0.0d) {
            sb.append("，概率增强=");
            sb.append(UiFormatter.signedProbability(support));
        }

        if (risk > 0.0d) {
            sb.append("，风险惩罚=");
            sb.append(UiFormatter.signedProbability(-risk));
        }

        if (profile.staleFlag == 1) {
            sb.append("，画像陈旧");
        }

        if (profile.failCount > 0) {
            sb.append("，连续失败=");
            sb.append(profile.failCount);
        }

        a.reason = sb.toString();

        return a;
    }

    private void applyLearningAdjustment(Models.ComboCandidate c, LearningAdjustment a) {
        if (c == null || a == null) {
            return;
        }

        double oldFinal = c.finalP;
        double oldGate = c.dynamicGate;

        double delta = a.probabilityBoost - a.probabilityPenalty;

        if (Math.abs(delta) > 0.000001d) {
            c.finalP = AppConfig.clampProbability(c.finalP + delta);
        }

        if (a.gateDown > 0.0d && a.probabilityBoost > a.probabilityPenalty) {
            c.dynamicGate = AppConfig.clampProbability(c.dynamicGate - a.gateDown);
        }

        if (a.gateUp > 0.0d && a.probabilityPenalty > a.probabilityBoost) {
            c.dynamicGate = AppConfig.clampProbability(c.dynamicGate + a.gateUp);
        }

        c.edge = c.finalP - c.dynamicGate;

        if (a.supportScore > 0.0d) {
            c.profileScore = Math.max(c.profileScore, a.supportScore);
            c.deepScore = blend01(c.deepScore, a.supportScore, 0.10d);
            c.evidenceScore = blend01(c.evidenceScore, a.supportScore, 0.08d);
        }

        if (a.probabilityPenalty > a.probabilityBoost) {
            c.evidenceRisk =
                    blend01(c.evidenceRisk, Math.min(1.0d, a.probabilityPenalty * 18.0d), 0.08d);
        }

        if (a.stale == 1 && c.finalP < 0.430d) {
            c.blockCode = pickBlockCode(c.blockCode, AppConfig.BLOCK_BACKTEST_WEAK);
        }

        if (a.profileFailCount >= PROFILE_FAIL_HARD && c.finalP < 0.410d) {
            c.blockCode = pickBlockCode(c.blockCode, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        String text =
                "学习画像接入："
                        + a.reason
                        + "，FinalP "
                        + UiFormatter.probability(oldFinal)
                        + "→"
                        + UiFormatter.probability(c.finalP)
                        + "，Gate "
                        + UiFormatter.probability(oldGate)
                        + "→"
                        + UiFormatter.probability(c.dynamicGate)
                        + "，Edge="
                        + UiFormatter.signedProbability(c.edge);

        c.reasonSummary = append(c.reasonSummary, text);
    }

    private void applyLearningGuard(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        double support = 0.0d;
        int count = 0;

        if (c.profileScore > 0.0d) {
            support += c.profileScore;
            count++;
        }

        if (c.fallbackProfileScore > 0.0d) {
            support += c.fallbackProfileScore;
            count++;
        }

        if (c.promotionScore > 0.0d) {
            support += c.promotionScore;
            count++;
        }

        if (c.auditBoost > 0.0d) {
            support += c.auditBoost;
            count++;
        }

        double avgSupport = count > 0 ? support / ((double) count) : 0.0d;

        if (avgSupport >= 0.620d
                && c.evidenceRisk <= 0.520d
                && c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY
                && c.finalP >= c.dynamicGate) {
            c.deepScore = blend01(c.deepScore, avgSupport, 0.08d);
            c.evidenceScore = blend01(c.evidenceScore, avgSupport, 0.08d);
        }

        if (c.profileScore < 0.300d
                && c.fallbackProfileScore < 0.300d
                && c.evidenceRisk >= 0.660d
                && c.finalP < 0.430d) {
            c.blockCode = pickBlockCode(c.blockCode, AppConfig.BLOCK_BACKTEST_WEAK);
        }

        if (c.auditPenalty >= 0.360d
                && c.finalP < 0.430d
                && (c.blockCode == null || c.blockCode.length() == 0)) {
            c.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY
                && c.profileScore < 0.620d
                && c.promotionScore < 0.620d) {
            c.blockCode = pickBlockCode(c.blockCode, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        c.edge = c.finalP - c.dynamicGate;

        c.reasonSummary =
                append(
                        c.reasonSummary,
                        "学习守门：profile="
                                + UiFormatter.probability(c.profileScore)
                                + "，fallback="
                                + UiFormatter.probability(c.fallbackProfileScore)
                                + "，promotion="
                                + UiFormatter.probability(c.promotionScore)
                                + "，auditBoost="
                                + UiFormatter.probability(c.auditBoost)
                                + "，auditPenalty="
                                + UiFormatter.probability(c.auditPenalty)
                                + "，risk="
                                + UiFormatter.probability(c.evidenceRisk));
    }

    private CognitiveSignal buildSignalFromTrade(Models.Trade t, Models.Draw actual, boolean hit) {
        CognitiveSignal s = new CognitiveSignal();

        if (t == null || actual == null || !actual.isValid()) {
            return s;
        }

        s.position = t.position;
        s.actualNumber = actual.getNumberAtPosition(t.position);
        s.predictedText = formatK3(t.n1, t.n2, t.n3);
        s.stateKey =
                buildStateKey(
                        t.position,
                        t.stateMode,
                        t.trendLife,
                        t.driftRisk,
                        t.breakDepth,
                        t.recoveryScore,
                        t.finalP,
                        t.dynamicGate);
        s.expectedShape =
                buildShapeText(
                        t.n1,
                        t.n2,
                        t.n3,
                        t.stateMode,
                        t.trendLife,
                        t.driftRisk,
                        t.breakDepth,
                        t.recoveryScore);
        s.actualShape = buildActualShapeText(s.actualNumber, t.n1, t.n2, t.n3);
        s.actualReason =
                buildActualReason(
                        hit,
                        s.actualNumber,
                        t.n1,
                        t.n2,
                        t.n3,
                        t.stateMode,
                        t.driftRisk,
                        t.breakDepth,
                        t.recoveryScore,
                        t.comboGap,
                        t.championGap);
        s.failReason =
                hit
                        ? "命中，不惩罚"
                        : buildFailReason(
                                s.actualNumber,
                                t.n1,
                                t.n2,
                                t.n3,
                                t.stateMode,
                                t.driftRisk,
                                t.breakDepth,
                                t.recoveryScore,
                                t.comboGap,
                                t.championGap);
        s.nextAdvice =
                buildNextAdvice(
                        hit,
                        s.actualNumber,
                        t.n1,
                        t.n2,
                        t.n3,
                        t.stateMode,
                        t.driftRisk,
                        t.breakDepth,
                        t.recoveryScore,
                        t.comboGap,
                        t.championGap);
        fillSignalSummaries(s);

        return s;
    }

    private CognitiveSignal buildSignalFromDecision(
            Models.Decision d, Models.Draw actual, boolean hit) {
        CognitiveSignal s = new CognitiveSignal();

        if (d == null || actual == null || !actual.isValid()) {
            return s;
        }

        s.position = d.position;
        s.actualNumber = actual.getNumberAtPosition(d.position);
        s.predictedText = formatK3(d.n1, d.n2, d.n3);
        s.stateKey =
                buildStateKey(
                        d.position,
                        d.stateMode,
                        d.trendLife,
                        d.driftRisk,
                        d.breakDepth,
                        d.recoveryScore,
                        d.finalP,
                        d.dynamicGate);
        s.expectedShape =
                buildShapeText(
                        d.n1,
                        d.n2,
                        d.n3,
                        d.stateMode,
                        d.trendLife,
                        d.driftRisk,
                        d.breakDepth,
                        d.recoveryScore);
        s.actualShape = buildActualShapeText(s.actualNumber, d.n1, d.n2, d.n3);
        s.actualReason =
                buildActualReason(
                        hit,
                        s.actualNumber,
                        d.n1,
                        d.n2,
                        d.n3,
                        d.stateMode,
                        d.driftRisk,
                        d.breakDepth,
                        d.recoveryScore,
                        d.comboGap,
                        d.championGap);
        s.failReason =
                hit
                        ? "命中，不惩罚"
                        : buildFailReason(
                                s.actualNumber,
                                d.n1,
                                d.n2,
                                d.n3,
                                d.stateMode,
                                d.driftRisk,
                                d.breakDepth,
                                d.recoveryScore,
                                d.comboGap,
                                d.championGap);
        s.nextAdvice =
                buildNextAdvice(
                        hit,
                        s.actualNumber,
                        d.n1,
                        d.n2,
                        d.n3,
                        d.stateMode,
                        d.driftRisk,
                        d.breakDepth,
                        d.recoveryScore,
                        d.comboGap,
                        d.championGap);
        fillSignalSummaries(s);

        return s;
    }

    private CognitiveSignal buildSignalFromCandidate(
            Models.ComboCandidate c, Models.Draw actual, boolean hit) {
        CognitiveSignal s = new CognitiveSignal();

        if (c == null || actual == null || !actual.isValid()) {
            return s;
        }

        s.position = c.position;
        s.actualNumber = actual.getNumberAtPosition(c.position);
        s.predictedText = formatK3(c.n1, c.n2, c.n3);
        s.stateKey = buildStateKey(c);
        s.expectedShape = buildCandidateShape(c);
        s.actualShape = buildActualShapeText(s.actualNumber, c.n1, c.n2, c.n3);
        s.actualReason =
                buildActualReason(
                        hit,
                        s.actualNumber,
                        c.n1,
                        c.n2,
                        c.n3,
                        c.stateMode,
                        c.driftRisk,
                        c.breakDepth,
                        c.recoveryScore,
                        c.comboGap,
                        c.championGap);
        s.failReason =
                hit
                        ? "命中，不惩罚"
                        : buildFailReason(
                                s.actualNumber,
                                c.n1,
                                c.n2,
                                c.n3,
                                c.stateMode,
                                c.driftRisk,
                                c.breakDepth,
                                c.recoveryScore,
                                c.comboGap,
                                c.championGap);
        s.nextAdvice =
                buildNextAdvice(
                        hit,
                        s.actualNumber,
                        c.n1,
                        c.n2,
                        c.n3,
                        c.stateMode,
                        c.driftRisk,
                        c.breakDepth,
                        c.recoveryScore,
                        c.comboGap,
                        c.championGap);
        fillSignalSummaries(s);

        return s;
    }

    private void fillSignalSummaries(CognitiveSignal s) {
        if (s == null) {
            return;
        }

        s.profileSummary =
                "状态指纹=" + s.stateKey + "，预测形态=" + s.expectedShape + "，实际形态=" + s.actualShape;
        s.gateAdjustSummary = "门槛修正依据：" + s.nextAdvice;
        s.probabilityAdjustSummary = "概率修正依据：" + s.actualReason + "；偏差=" + s.failReason;
        s.auditSummary = "学习审计：只学习目标期已开奖结果，不把SHADOW/HOLD混入真实胜率";
        s.biasSummary = "偏差分析：" + s.failReason;
        s.promotionSummary = "晋级建议：" + s.nextAdvice;
        s.aPlusSummary = "A+观察：" + s.stateKey;
    }

    private String buildStateKey(Models.ComboCandidate c) {
        if (c == null) {
            return "STATE_EMPTY";
        }

        return buildStateKey(
                c.position,
                c.stateMode,
                c.trendLife,
                c.driftRisk,
                c.breakDepth,
                c.recoveryScore,
                c.finalP,
                c.dynamicGate);
    }

    private String buildStateKey(
            int position,
            String stateMode,
            double trendLife,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double finalP,
            double gate) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(position);
        sb.append("_");
        sb.append(safeMode(stateMode));
        sb.append("_TL");
        sb.append(bucketScore(trendLife));
        sb.append("_DR");
        sb.append(bucketScore(driftRisk));
        sb.append("_BD");
        sb.append(bucketScore(breakDepth));
        sb.append("_RC");
        sb.append(bucketScore(recoveryScore));
        sb.append("_FP");
        sb.append(bucketProb(finalP));
        sb.append("_G");
        sb.append(bucketProb(gate));

        return sb.toString();
    }

    private String buildCandidateShape(Models.ComboCandidate c) {
        if (c == null) {
            return "";
        }

        return buildShapeText(
                c.n1,
                c.n2,
                c.n3,
                c.stateMode,
                c.trendLife,
                c.driftRisk,
                c.breakDepth,
                c.recoveryScore);
    }

    private String buildShapeText(
            int n1,
            int n2,
            int n3,
            String stateMode,
            double trendLife,
            double driftRisk,
            double breakDepth,
            double recoveryScore) {
        int[] nums = new int[] {n1, n2, n3};

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int min = 99;
        int max = 0;

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        StringBuilder sb = new StringBuilder();

        sb.append("低");
        sb.append(low);
        sb.append("/中");
        sb.append(mid);
        sb.append("/高");
        sb.append(high);
        sb.append("，奇");
        sb.append(odd);
        sb.append("/偶");
        sb.append(even);

        if (max >= min && max > 0) {
            sb.append("，跨度");
            sb.append(max - min);
        }

        if (trendLife >= 60.0d && driftRisk <= 42.0d && breakDepth <= 42.0d) {
            sb.append("，趋势可续");
        } else if (recoveryScore >= 60.0d) {
            sb.append("，恢复链");
        } else if (driftRisk >= 58.0d) {
            sb.append("，漂移高");
        } else if (breakDepth >= 55.0d) {
            sb.append("，断点深");
        } else if ("REVERSAL".equals(stateMode) || AppConfig.MODE_REVERSING.equals(stateMode)) {
            sb.append("，反转态");
        } else {
            sb.append("，中性态");
        }

        return sb.toString();
    }

    private String buildActualShapeText(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return "实际号码非法";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(formatNumber(actual));
        sb.append("=");
        sb.append(zoneText(actual));
        sb.append("/");
        sb.append(actual % 2 == 0 ? "偶" : "奇");

        if (actual == n1 || actual == n2 || actual == n3) {
            sb.append("，落入K3");
            return sb.toString();
        }

        int dist = nearestDistance(actual, n1, n2, n3);

        if (dist == 1) {
            sb.append("，K3邻号");
        } else if (dist == 2) {
            sb.append("，K3近层");
        } else {
            sb.append("，K3外层");
        }

        if (!coversZone(actual, n1, n2, n3)) {
            sb.append("，区间未覆盖");
        }

        return sb.toString();
    }

    private String buildActualReason(
            boolean hit,
            int actual,
            int n1,
            int n2,
            int n3,
            String stateMode,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double comboGap,
            double championGap) {
        if (!AppConfig.isValidNumber(actual)) {
            return "实际号码非法，不能学习";
        }

        if (hit) {
            if (driftRisk <= 42.0d && breakDepth <= 42.0d) {
                return "命中说明趋势未被漂移和断点破坏，K3覆盖有效";
            }

            if (recoveryScore >= 55.0d) {
                return "命中说明恢复链有效，实际号码回到候选覆盖层";
            }

            return "命中说明该状态下K3仍有正偏差";
        }

        if (nearestDistance(actual, n1, n2, n3) == 1) {
            return "实际号码是K3邻号，说明方向接近但号码层级差一格";
        }

        if (!coversZone(actual, n1, n2, n3)) {
            return "实际号码落在未覆盖区间，说明本期区间层判断偏差";
        }

        if (driftRisk >= 58.0d) {
            return "漂移风险偏高，实际号码容易离开原热层";
        }

        if (breakDepth >= 55.0d) {
            return "断点深，实际号码更像破位后新层号码";
        }

        if (recoveryScore < 45.0d && AppConfig.MODE_RECOVERING.equals(stateMode)) {
            return "恢复分不足，实际没有继续走恢复链";
        }

        if (comboGap < 0.018d) {
            return "组合优势差距小，实际号码从候选边缘层冒出";
        }

        if (championGap < 0.018d) {
            return "位置优势不足，实际号码被其他层接管";
        }

        return "实际号码在K3外层，属于短周期层级切换";
    }

    private String buildFailReason(
            int actual,
            int n1,
            int n2,
            int n3,
            String stateMode,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double comboGap,
            double championGap) {
        StringBuilder sb = new StringBuilder();

        if (nearestDistance(actual, n1, n2, n3) == 1) {
            appendPart(sb, "第三号码层偏差");
        }

        if (!coversZone(actual, n1, n2, n3)) {
            appendPart(sb, "区间覆盖不足");
        }

        if (driftRisk >= 58.0d) {
            appendPart(sb, "漂移高");
        }

        if (breakDepth >= 55.0d) {
            appendPart(sb, "断点深");
        }

        if (recoveryScore < 45.0d && AppConfig.MODE_RECOVERING.equals(stateMode)) {
            appendPart(sb, "假恢复");
        }

        if (comboGap < 0.018d) {
            appendPart(sb, "组合差距弱");
        }

        if (championGap < 0.018d) {
            appendPart(sb, "位置优势弱");
        }

        if ("REVERSAL".equals(stateMode) || AppConfig.MODE_REVERSING.equals(stateMode)) {
            appendPart(sb, "反转态不稳");
        }

        if (sb.length() == 0) {
            sb.append("未发现单一强错因，可能是短周期随机切换");
        }

        return sb.toString();
    }

    private String buildNextAdvice(
            boolean hit,
            int actual,
            int n1,
            int n2,
            int n3,
            String stateMode,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double comboGap,
            double championGap) {
        if (hit) {
            if (driftRisk <= 42.0d && breakDepth <= 42.0d && comboGap >= 0.020d) {
                return "同状态可小幅强化画像和组合层，但不降低真实BET底线";
            }

            return "命中但证据未极强，只强化状态记忆，不盲目扩频";
        }

        if (nearestDistance(actual, n1, n2, n3) == 1) {
            return "同状态保留主方向，增加邻号层/第四到第六层权重";
        }

        if (!coversZone(actual, n1, n2, n3)) {
            return "同状态要求K3至少覆盖实际高发区间，否则转SHADOW";
        }

        if (driftRisk >= 58.0d) {
            return "漂移高时提高门槛，降低旧热号权重";
        }

        if (breakDepth >= 55.0d) {
            return "断点深时不抢BET，等待恢复确认";
        }

        if (recoveryScore < 45.0d && AppConfig.MODE_RECOVERING.equals(stateMode)) {
            return "恢复分不足时不能提前押恢复链";
        }

        if (comboGap < 0.018d || championGap < 0.018d) {
            return "组合/位置优势不足时只做观察，不进真实BET";
        }

        return "同状态下先收紧真实BET，进入SHADOW积累证据";
    }

    private double profileSampleConfidence(Models.DeepProfile p) {
        if (p == null) {
            return 0.0d;
        }

        double c = 0.0d;

        if (p.totalSample > 0) {
            c += Math.min(0.55d, ((double) p.totalSample) / 40.0d);
        }

        if (p.recentSample > 0) {
            c += Math.min(0.35d, ((double) p.recentSample) / 24.0d);
        }

        if (p.activeFlag == 1 && p.staleFlag == 0) {
            c += 0.10d;
        }

        if (p.failCount >= PROFILE_FAIL_HARD) {
            c -= 0.12d;
        }

        return AppConfig.clampScore01(c);
    }

    private ArrayList<Models.ComboCandidate> safeApplyDeepProfile(
            ArrayList<Models.ComboCandidate> source, List<Models.DeepProfile> profiles) {
        try {
            ArrayList<Models.ComboCandidate> r =
                    deepProfileEngine.applyToCandidates(source, profiles);
            return r == null ? source : r;
        } catch (Throwable e) {
            return source;
        }
    }

    private ArrayList<Models.ComboCandidate> safeApplyFallback(
            ArrayList<Models.ComboCandidate> source, List<Models.DeepProfile> profiles) {
        try {
            ArrayList<Models.ComboCandidate> r =
                    profileFallbackEngine.applyToCandidates(source, profiles);
            return r == null ? source : r;
        } catch (Throwable e) {
            return source;
        }
    }

    private ArrayList<Models.ComboCandidate> safeApplyAudit(
            ArrayList<Models.ComboCandidate> source) {
        try {
            ArrayList<Models.ComboCandidate> r = probabilityAuditEngine.applyToCandidates(source);
            return r == null ? source : r;
        } catch (Throwable e) {
            return source;
        }
    }

    private ArrayList<Models.ComboCandidate> safeApplyPromotion(
            ArrayList<Models.ComboCandidate> source) {
        try {
            ArrayList<Models.ComboCandidate> r = promotionGateEngine.applyToCandidates(source);
            return r == null ? source : r;
        } catch (Throwable e) {
            return source;
        }
    }

    private int bucketInt(double v) {
        if (v <= 0.0d) {
            return 0;
        }

        if (v < 30.0d) {
            return 1;
        }

        if (v < 55.0d) {
            return 2;
        }

        if (v < 75.0d) {
            return 3;
        }

        return 4;
    }

    private String bucketScore(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 30.0d) {
            return "L";
        }

        if (v < 55.0d) {
            return "M";
        }

        if (v < 75.0d) {
            return "H";
        }

        return "X";
    }

    private String bucketProb(double v) {
        if (v <= 0.0d) {
            return "0";
        }

        if (v < 0.340d) {
            return "L";
        }

        if (v < 0.375d) {
            return "S";
        }

        if (v < 0.400d) {
            return "B";
        }

        if (v < 0.430d) {
            return "H";
        }

        return "X";
    }

    private String bucketLabel(String prefix, double value) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (value <= 0.0d) {
            return prefix + "_0";
        }

        if (value < 0.25d) {
            return prefix + "_LOW";
        }

        if (value < 0.50d) {
            return prefix + "_MID";
        }

        if (value < 0.75d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private String safeMode(String mode) {
        if (mode == null || mode.length() == 0) {
            return "UNKNOWN";
        }

        return mode;
    }

    private int nearestDistance(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return 99;
        }

        int best = 99;

        if (AppConfig.isValidNumber(n1)) {
            best = Math.min(best, Math.abs(actual - n1));
        }

        if (AppConfig.isValidNumber(n2)) {
            best = Math.min(best, Math.abs(actual - n2));
        }

        if (AppConfig.isValidNumber(n3)) {
            best = Math.min(best, Math.abs(actual - n3));
        }

        return best;
    }

    private boolean coversZone(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return false;
        }

        int z = numberZone(actual);
        return numberZone(n1) == z || numberZone(n2) == z || numberZone(n3) == z;
    }

    private int numberZone(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return 0;
        }

        if (n <= 3) {
            return 1;
        }

        if (n <= 7) {
            return 2;
        }

        return 3;
    }

    private String zoneText(int n) {
        int z = numberZone(n);

        if (z == 1) {
            return "低区";
        }

        if (z == 2) {
            return "中区";
        }

        if (z == 3) {
            return "高区";
        }

        return "未知区";
    }

    private String formatK3(int n1, int n2, int n3) {
        return formatNumber(n1) + "/" + formatNumber(n2) + "/" + formatNumber(n3);
    }

    private String formatNumber(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private void sortBridgeResults(List<BridgeResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<BridgeResult>() {
                    @Override
                    public int compare(BridgeResult a, BridgeResult b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        String ia = a == null ? "" : safe(a.targetIssue);
                        String ib = b == null ? "" : safe(b.targetIssue);

                        return ia.compareTo(ib);
                    }
                });
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

        score += c.finalP * 100.0d * 0.14d;
        score += c.edge * 100.0d * 0.20d;
        score += c.profileScore * 100.0d * 0.10d;
        score += c.fallbackProfileScore * 100.0d * 0.08d;
        score += c.promotionScore * 100.0d * 0.10d;
        score += c.auditBoost * 100.0d * 0.06d;
        score += c.deepScore * 100.0d * 0.04d;
        score += c.evidenceScore * 100.0d * 0.05d;

        score -= c.auditPenalty * 100.0d * 0.10d;
        score -= c.evidenceRisk * 100.0d * 0.08d;

        if (c.blockCode != null && c.blockCode.length() > 0) {
            score -= 6.0d;
        }

        if (c.finalP >= c.dynamicGate && c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            score += 3.0d;
        }

        return score;
    }

    private String buildBridgeSummary(BridgeResult r) {
        if (r == null) {
            return "学习桥接：暂无";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("学习桥接：");

        if (r.hit == 1) {
            sb.append("命中学习");
        } else {
            sb.append("未中学习");
        }

        sb.append("，来源=");
        sb.append(safe(r.sourceType));

        sb.append("，模块=");
        sb.append(safe(r.affectedModule));

        sb.append("，动作=");
        sb.append(safe(r.moduleAction));

        sb.append("，权重变化=");
        sb.append(UiFormatter.signedProbability(r.moduleWeightDelta));

        sb.append("，主因=");
        sb.append(safe(r.mainReasonCode));

        sb.append("，置信=");
        sb.append(UiFormatter.probability(r.confidence));

        if (r.profileKey != null && r.profileKey.length() > 0) {
            sb.append("，画像=");
            sb.append(r.profileKey);
        }

        if (r.deepProfile != null) {
            sb.append("，画像样本=");
            sb.append(r.deepProfile.totalSample);

            sb.append("，总命中=");
            sb.append(UiFormatter.probability(r.deepProfile.totalHitRate));

            sb.append("，近期=");
            sb.append(UiFormatter.probability(r.deepProfile.recentHitRate));

            sb.append("，失败=");
            sb.append(r.deepProfile.failCount);
        }

        if (r.stateKey != null && r.stateKey.length() > 0) {
            sb.append("，状态=");
            sb.append(r.stateKey);
        }

        if (r.expectedShape != null && r.expectedShape.length() > 0) {
            sb.append("，预测形态=");
            sb.append(r.expectedShape);
        }

        if (r.actualShape != null && r.actualShape.length() > 0) {
            sb.append("，实际形态=");
            sb.append(r.actualShape);
        }

        if (r.actualReason != null && r.actualReason.length() > 0) {
            sb.append("，实际原因=");
            sb.append(r.actualReason);
        }

        if (r.failReason != null && r.failReason.length() > 0) {
            sb.append("，偏差原因=");
            sb.append(r.failReason);
        }

        if (r.nextAdvice != null && r.nextAdvice.length() > 0) {
            sb.append("，下次修正=");
            sb.append(r.nextAdvice);
        }

        if (r.mainReasonText != null && r.mainReasonText.length() > 0) {
            sb.append("，说明=");
            sb.append(r.mainReasonText);
        }

        return safeLen(sb.toString());
    }

    private String buildSnapshotLiteJson(BridgeResult r) {
        if (r == null) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("{");
        sb.append("\"source\":\"").append(escapeJson(r.sourceType)).append("\",");
        sb.append("\"target\":\"").append(escapeJson(r.targetIssue)).append("\",");
        sb.append("\"hit\":").append(r.hit).append(",");
        sb.append("\"profile\":\"").append(escapeJson(r.profileKey)).append("\",");
        sb.append("\"state\":\"").append(escapeJson(r.stateKey)).append("\",");
        sb.append("\"action\":\"").append(escapeJson(r.moduleAction)).append("\",");
        sb.append("\"delta\":").append(r.moduleWeightDelta).append(",");
        sb.append("\"reason\":\"").append(escapeJson(r.mainReasonCode)).append("\",");
        sb.append("\"advice\":\"").append(escapeJson(r.nextAdvice)).append("\"");
        sb.append("}");

        return sb.toString();
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(" ");
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
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

        if (weightNew < 0.0d) {
            weightNew = 0.0d;
        }

        if (weightNew > 1.0d) {
            weightNew = 1.0d;
        }

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private double normalizeScore01(double value) {
        if (value <= 0.0d) {
            return 0.0d;
        }

        if (value > 1.0d) {
            value = value / 100.0d;
        }

        if (value > 1.0d) {
            return 1.0d;
        }

        return value;
    }

    private boolean isTargetMatched(String targetIssue, String actualIssue) {
        if (targetIssue == null || targetIssue.length() == 0) {
            return true;
        }

        if (actualIssue == null || actualIssue.length() == 0) {
            return false;
        }

        return normalizeIssue(targetIssue).equals(normalizeIssue(actualIssue));
    }

    private String normalizeIssue(String issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < issue.length(); i++) {
            char c = issue.charAt(i);

            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }

        String s = sb.toString();

        while (s.length() > 1 && s.startsWith("0")) {
            s = s.substring(1);
        }

        return s;
    }

    private boolean maskContains(int mask, int number) {
        if (!AppConfig.isValidNumber(number)) {
            return false;
        }

        try {
            return AppConfig.maskContainsNumber(mask, number);
        } catch (Throwable e) {
            return (mask & (1 << number)) != 0;
        }
    }

    private void appendPart(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("，");
        }

        sb.append(text);
    }

    private void set(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = target.getClass().getField(fieldName);
            Class type = f.getType();

            if (type == int.class) {
                if (value instanceof Number) {
                    f.setInt(target, ((Number) value).intValue());
                }
            } else if (type == long.class) {
                if (value instanceof Number) {
                    f.setLong(target, ((Number) value).longValue());
                }
            } else if (type == double.class) {
                if (value instanceof Number) {
                    f.setDouble(target, ((Number) value).doubleValue());
                }
            } else if (type == String.class) {
                f.set(target, value == null ? "" : String.valueOf(value));
            } else {
                f.set(target, value);
            }
        } catch (Throwable ignore) {
        }
    }

    private Object invokeStoreOptional(
            Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return NO_METHOD;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            return NO_METHOD;
        }
    }

    private void safeSetState(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) {
            return;
        }

        try {
            store.setState(key, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private String getFieldText(Object obj, String fieldName) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return "";
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private void cooperativePause(long sleepMs) {
        try {
            Thread.yield();

            if (sleepMs > 0L) {
                Thread.sleep(sleepMs);
            }
        } catch (Throwable e) {
        }
    }

    private static String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return addText;
        }

        if (oldText.indexOf(addText) >= 0) {
            return oldText;
        }

        String out = oldText + "；" + addText;

        if (out.length() > MAX_REASON_LEN) {
            return out.substring(0, MAX_REASON_LEN);
        }

        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeLen(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_SUMMARY_LEN) {
            return text;
        }

        return text.substring(0, MAX_SUMMARY_LEN);
    }

    private static final class CognitiveSignal {
        public int position;
        public int actualNumber;
        public String predictedText;
        public String stateKey;
        public String expectedShape;
        public String actualShape;
        public String actualReason;
        public String failReason;
        public String nextAdvice;
        public String profileSummary;
        public String gateAdjustSummary;
        public String probabilityAdjustSummary;
        public String auditSummary;
        public String biasSummary;
        public String promotionSummary;
        public String aPlusSummary;

        public CognitiveSignal() {
            position = 0;
            actualNumber = 0;
            predictedText = "";
            stateKey = "";
            expectedShape = "";
            actualShape = "";
            actualReason = "";
            failReason = "";
            nextAdvice = "";
            profileSummary = "";
            gateAdjustSummary = "";
            probabilityAdjustSummary = "";
            auditSummary = "";
            biasSummary = "";
            promotionSummary = "";
            aPlusSummary = "";
        }
    }

    private static final class LearningAdjustment {
        public String stateKey;
        public String expectedShape;
        public String profileKey;
        public double probabilityBoost;
        public double probabilityPenalty;
        public double gateDown;
        public double gateUp;
        public double supportScore;
        public double confidence;
        public int profileSample;
        public int profileRecentSample;
        public int profileFailCount;
        public int stale;
        public String reason;

        public LearningAdjustment() {
            stateKey = "";
            expectedShape = "";
            profileKey = "";
            probabilityBoost = 0.0d;
            probabilityPenalty = 0.0d;
            gateDown = 0.0d;
            gateUp = 0.0d;
            supportScore = 0.0d;
            confidence = 0.0d;
            profileSample = 0;
            profileRecentSample = 0;
            profileFailCount = 0;
            stale = 0;
            reason = "";
        }
    }

    public static final class BridgeResult {
        public long createdAt;

        public String sourceType;
        public String status;
        public String baseIssue;
        public String targetIssue;

        public int hit;

        public String mainReasonCode;
        public String mainReasonText;
        public String affectedModule;
        public String moduleAction;
        public double moduleWeightDelta;
        public double confidence;

        public int shouldReward;
        public int shouldPunish;

        public double positionImpact;
        public double numberImpact;
        public double cycleImpact;
        public double transitionImpact;
        public double calibrationImpact;

        public String profileKey;
        public String learningText;
        public String summaryText;

        public int position;
        public int actualNumber;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public double rawP;
        public double finalP;
        public double dynamicGate;
        public double edge;

        public String stateMode;
        public String shapeKey;
        public String probabilityMode;

        public double trendLife;
        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double comboGap;
        public double championGap;
        public double deepScore;
        public double profileScore;
        public double biasScore;

        public String predictedText;
        public String stateKey;
        public String expectedShape;
        public String actualShape;
        public String actualReason;
        public String failReason;
        public String nextAdvice;

        public String profileSummary;
        public String gateAdjustSummary;
        public String probabilityAdjustSummary;
        public String auditSummary;
        public String biasSummary;
        public String promotionSummary;
        public String aPlusSummary;

        public Models.AttributionResult attribution;
        public Models.LearningSample learningSample;
        public Models.DeepProfile deepProfile;
        public Models.RuntimeLearningSnapshot snapshot;

        public BridgeResult() {
            createdAt = 0L;

            sourceType = "";
            status = "";
            baseIssue = "";
            targetIssue = "";

            hit = 0;

            mainReasonCode = "";
            mainReasonText = "";
            affectedModule = "";
            moduleAction = ACTION_WATCH;
            moduleWeightDelta = 0.0d;
            confidence = 0.0d;

            shouldReward = 0;
            shouldPunish = 0;

            positionImpact = 0.0d;
            numberImpact = 0.0d;
            cycleImpact = 0.0d;
            transitionImpact = 0.0d;
            calibrationImpact = 0.0d;

            profileKey = "";
            learningText = "";
            summaryText = "";

            position = 0;
            actualNumber = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            rawP = 0.0d;
            finalP = 0.0d;
            dynamicGate = 0.0d;
            edge = 0.0d;

            stateMode = "";
            shapeKey = "";
            probabilityMode = "";

            trendLife = 0.0d;
            driftRisk = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            comboGap = 0.0d;
            championGap = 0.0d;
            deepScore = 0.0d;
            profileScore = 0.0d;
            biasScore = 0.0d;

            predictedText = "";
            stateKey = "";
            expectedShape = "";
            actualShape = "";
            actualReason = "";
            failReason = "";
            nextAdvice = "";

            profileSummary = "";
            gateAdjustSummary = "";
            probabilityAdjustSummary = "";
            auditSummary = "";
            biasSummary = "";
            promotionSummary = "";
            aPlusSummary = "";

            attribution = null;
            learningSample = null;
            deepProfile = null;
            snapshot = null;
        }

        public boolean isReward() {
            return ACTION_REWARD.equals(moduleAction) && moduleWeightDelta > 0.0d;
        }

        public boolean isPunish() {
            return ACTION_PUNISH.equals(moduleAction) && moduleWeightDelta < 0.0d;
        }

        public double sortScore() {
            double score = 50.0d;

            score += Math.abs(moduleWeightDelta) * 100.0d * 0.40d;
            score += confidence * 100.0d * 0.20d;

            if (isReward()) {
                score += 4.0d;
            }

            if (isPunish()) {
                score += 5.0d;
            }

            if (deepProfile != null) {
                score += Math.min(10.0d, deepProfile.totalSample * 0.02d);

                if (deepProfile.staleFlag == 1) {
                    score -= 8.0d;
                }

                if (deepProfile.recentHitRate >= 0.420d) {
                    score += 5.0d;
                }
            }

            if (nextAdvice != null && nextAdvice.length() > 0) {
                score += 2.0d;
            }

            return AppConfig.clampScore100(score);
        }
    }
}
