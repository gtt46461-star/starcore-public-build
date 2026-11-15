package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PromotionGateEngine {

    public static final String PROMOTION_NONE = "NONE";
    public static final String PROMOTION_WATCH = "WATCH";
    public static final String PROMOTION_CANDIDATE = "A_PLUS_CANDIDATE";
    public static final String PROMOTION_APPROVED = "A_PLUS_APPROVED";
    public static final String PROMOTION_DOWNGRADED = "DOWNGRADED";
    public static final String PROMOTION_REJECTED = "REJECTED";

    private static final double PROMOTION_WATCH_SCORE = 48.0d;
    private static final double PROMOTION_CANDIDATE_SCORE = 58.0d;
    private static final double PROMOTION_APPROVED_SCORE = 68.0d;
    private static final double PROMOTION_A_PLUS_SCORE = 76.0d;

    private static final double RISK_WARN = 62.0d;
    private static final double RISK_HARD = 78.0d;

    private static final int MAX_REASON_LEN = 1600;

    private final ProbabilityAuditEngine probabilityAuditEngine;

    public PromotionGateEngine() {
        probabilityAuditEngine = new ProbabilityAuditEngine();
    }

    public PromotionResult evaluateCandidate(Models.ComboCandidate candidate) {
        ProbabilityAuditEngine.AuditResult audit =
                candidate == null ? null : probabilityAuditEngine.auditCandidate(candidate);

        return evaluateCandidate(candidate, audit);
    }

    public PromotionResult evaluateCandidate(
            Models.ComboCandidate candidate, ProbabilityAuditEngine.AuditResult audit) {
        PromotionResult r = new PromotionResult();
        r.createdAt = System.currentTimeMillis();

        if (candidate == null) {
            r.status = PROMOTION_REJECTED;
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "A+晋级：候选为空";
            return r;
        }

        normalizeCandidate(candidate);
        fillBase(r, candidate);

        if (!candidate.isValidK3()) {
            r.status = PROMOTION_REJECTED;
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "A+晋级：候选不是单位置固定K3";
            return r;
        }

        r.probabilityScore = probabilityScore(candidate);
        r.evidenceScore = evidenceScore(candidate);
        r.aiScore = aiScore(candidate);
        r.profileScore = profileScore(candidate);
        r.auditScore = auditScore(candidate, audit);
        r.sampleScore = sampleScore(candidate);
        r.shapeScore = shapeScore(candidate);
        r.edgeScore = edgeScore(candidate);

        r.riskScore = riskScore(candidate, audit);
        r.penaltyScore = penaltyScore(candidate, audit);
        r.bonusScore = bonusScore(candidate, audit);

        r.promotionScore = finalPromotionScore(candidate, audit, r);
        r.confidence = confidenceScore(candidate, audit, r);

        r.status = statusByPromotion(candidate, audit, r);
        r.blockCode = blockByPromotion(candidate, audit, r);
        r.approved = PROMOTION_APPROVED.equals(r.status) ? 1 : 0;
        r.aPlus =
                r.promotionScore >= PROMOTION_A_PLUS_SCORE
                                && r.riskScore <= 42.0d
                                && r.confidence >= 62.0d
                        ? 1
                        : 0;

        r.promotionKey = buildPromotionKey(r);
        r.summaryText = buildSummary(r, candidate, audit);

        return r;
    }

    public ArrayList<PromotionResult> evaluateCandidates(List<Models.ComboCandidate> candidates) {
        ArrayList<PromotionResult> out = new ArrayList<PromotionResult>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<ProbabilityAuditEngine.AuditResult> audits =
                probabilityAuditEngine.auditCandidates(candidates);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            ProbabilityAuditEngine.AuditResult audit = findAudit(audits, c);
            PromotionResult r = evaluateCandidate(c, audit);
            out.add(r);
        }

        sortPromotionResults(out);
        return out;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<ProbabilityAuditEngine.AuditResult> audits =
                probabilityAuditEngine.auditCandidates(candidates);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            normalizeCandidate(copy);

            ProbabilityAuditEngine.AuditResult audit = findAudit(audits, copy);
            PromotionResult r = evaluateCandidate(copy, audit);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public Models.CandidateBatch applyToBatch(Models.CandidateBatch batch) {
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

        ArrayList<Models.ComboCandidate> list = applyToCandidates(batch.candidates);
        out.addAll(list);

        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, PromotionResult result) {
        if (candidate == null) {
            return;
        }

        normalizeCandidate(candidate);

        if (result == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "A+晋级：暂无");
            return;
        }

        candidate.promotionScore = AppConfig.clampScore01(result.promotionScore / 100.0d);
        candidate.aPlusScore =
                blend01(
                        candidate.aPlusScore,
                        result.aPlusScore01(),
                        result.approved == 1 ? 0.22d : 0.10d);

        candidate.promotionStatus = result.status;

        if (PROMOTION_APPROVED.equals(result.status)) {
            if (result.aPlus == 1) {
                candidate.aPlusStatus = aPlusApprovedText();
            } else {
                candidate.aPlusStatus = aPlusCandidateText();
            }

            /*
             * 核心红线：
             * A+晋级只提升排序、评分、解释；
             * 不直接修改 finalP / rawP；
             * 不绕过 DecisionGateEngine 最终裁决。
             */
            if (candidate.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY
                    && candidate.finalP >= candidate.dynamicGate
                    && result.riskScore <= 48.0d
                    && candidate.driftRisk < AppConfig.DRIFT_RISK_HARD_BLOCK
                    && candidate.breakDepth < breakDepthHardBlock()) {
                candidate.edge = candidate.finalP - candidate.dynamicGate;
                candidate.reasonSummary =
                        append(
                                candidate.reasonSummary,
                                "A+晋级：强候选只提升排序和辅助评分，不直接修改FinalP/RawP，最终BET仍由DecisionGateEngine裁决");
            }
        } else if (PROMOTION_CANDIDATE.equals(result.status)) {
            if (candidate.aPlusStatus == null || candidate.aPlusStatus.length() == 0) {
                candidate.aPlusStatus = aPlusCandidateText();
            }
        } else if (PROMOTION_DOWNGRADED.equals(result.status)
                || PROMOTION_REJECTED.equals(result.status)) {
            if (candidate.aPlusStatus == null || candidate.aPlusStatus.length() == 0) {
                candidate.aPlusStatus = aPlusNoneText();
            }
        }

        if (result.riskScore >= RISK_HARD && candidate.finalP < 0.430d) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, result.blockCode);
        }

        if (PROMOTION_REJECTED.equals(result.status)
                && result.blockCode != null
                && result.blockCode.length() > 0
                && candidate.finalP < 0.410d) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, result.blockCode);
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "A+晋级：状态="
                                + result.status
                                + "，晋级分="
                                + UiFormatter.score(result.promotionScore)
                                + "，风险="
                                + UiFormatter.score(result.riskScore)
                                + "，置信="
                                + UiFormatter.score(result.confidence)
                                + "，概率="
                                + UiFormatter.score(result.probabilityScore)
                                + "，证据="
                                + UiFormatter.score(result.evidenceScore)
                                + "，AI="
                                + UiFormatter.score(result.aiScore)
                                + "，画像="
                                + UiFormatter.score(result.profileScore)
                                + "，审计="
                                + UiFormatter.score(result.auditScore)
                                + "，说明=A+只提升候选排序，不修改FinalP/RawP，不绕过最终决策门");
    }

    public PromotionResult bestPromotion(List<PromotionResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        PromotionResult best = null;

        for (int i = 0; i < list.size(); i++) {
            PromotionResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (best == null || r.sortScore() > best.sortScore()) {
                best = r;
            }
        }

        return best;
    }

    public Models.ComboCandidate bestCandidateAfterPromotion(
            List<Models.ComboCandidate> candidates) {
        ArrayList<Models.ComboCandidate> list = applyToCandidates(candidates);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0).copy();
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, Models.ComboCandidate candidate) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        if (candidate == null) {
            return set;
        }

        PromotionResult r = evaluateCandidate(candidate);

        set.globalTrendStrength = Math.max(set.globalTrendStrength, r.promotionScore);
        set.globalBiasScore = Math.max(set.globalBiasScore, r.confidence);

        if (PROMOTION_APPROVED.equals(r.status) || PROMOTION_CANDIDATE.equals(r.status)) {
            set.bestPosition = candidate.position;
            set.bestNumbers = new int[] {candidate.n1, candidate.n2, candidate.n3};
        }

        set.profileKey = r.promotionKey;
        set.summaryText = append(set.summaryText, r.summaryText);

        return set;
    }

    public String buildSummary(List<PromotionResult> list) {
        if (list == null || list.size() == 0) {
            return "A+晋级：暂无候选";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(3, list.size());

        for (int i = 0; i < limit; i++) {
            PromotionResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));
            sb.append(" 晋级=");
            sb.append(UiFormatter.score(r.promotionScore));
            sb.append(" 风险=");
            sb.append(UiFormatter.score(r.riskScore));
            sb.append(" ");
            sb.append(r.status);
        }

        return sb.length() == 0 ? "A+晋级：暂无有效结果" : sb.toString();
    }

    private void fillBase(PromotionResult r, Models.ComboCandidate c) {
        if (r == null || c == null) {
            return;
        }

        r.baseIssue = safe(c.baseIssue);
        r.targetIssue = safe(c.targetIssue);

        r.position = c.position;
        r.n1 = c.n1;
        r.n2 = c.n2;
        r.n3 = c.n3;
        r.k3Mask = c.k3Mask;

        r.finalP = c.finalP;
        r.dynamicGate = c.dynamicGate;
        r.edge = c.edge;

        r.evidenceScore01 = c.evidenceScore;
        r.evidenceRisk01 = c.evidenceRisk;
        r.aiJudgeScore01 = c.aiJudgeScore;
        r.aiJudgeConfidence01 = c.aiJudgeConfidence;
        r.aPlusScoreInput = c.aPlusScore;
        r.profileScore01 = c.profileScore;
        r.fallbackProfileScore01 = c.fallbackProfileScore;
        r.deepScore01 = c.deepScore;
        r.auditPenalty01 = c.auditPenalty;
        r.auditBoost01 = c.auditBoost;
    }

    private double probabilityScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.finalP * 100.0d * 0.48d;
        score += c.rawP * 100.0d * 0.12d;
        score += c.calibratedP * 100.0d * 0.10d;
        score += Math.max(0.0d, c.edge) * 100.0d * 0.22d;
        score += c.comboP * 100.0d * 0.08d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 5.0d;
        }

        if (c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            score += 3.0d;
        }

        if (c.finalP < c.dynamicGate) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double evidenceScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.evidenceScore * 100.0d * 0.34d;
        score += c.biasScore * 100.0d * 0.12d;
        score += c.deepPositionScore * 100.0d * 0.10d;
        score += c.deepNumberScore * 100.0d * 0.10d;
        score += c.deepCycleScore * 100.0d * 0.08d;
        score += c.deepTransitionScore * 100.0d * 0.08d;
        score += c.deepStateScore * 100.0d * 0.08d;
        score += c.weakestNumberScore * 100.0d * 0.05d;
        score += c.balanceScore * 100.0d * 0.05d;

        if (c.evidenceRisk <= 0.420d && c.evidenceScore >= 0.580d) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double aiScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.aiJudgeScore * 100.0d * 0.52d;
        score += c.aiJudgeConfidence * 100.0d * 0.34d;
        score += Math.max(0.0d, 1.0d - c.conflictLevel) * 100.0d * 0.08d;
        score += c.aPlusScore * 100.0d * 0.06d;

        if (score <= 0.0d) {
            score = 45.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double profileScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.profileScore * 100.0d * 0.36d;
        score += c.fallbackProfileScore * 100.0d * 0.22d;
        score += c.deepScore * 100.0d * 0.18d;
        score += c.deepConfidence * 100.0d * 0.12d;
        score += c.promotionScore * 100.0d * 0.12d;

        if (c.deepProfileKey != null && c.deepProfileKey.length() > 0) {
            score += 3.0d;
        }

        if (c.fallbackProfileKey != null && c.fallbackProfileKey.length() > 0) {
            score += 2.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double auditScore(Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (audit != null) {
            score += audit.reliabilityScore * 0.64d;
            score += Math.max(0.0d, 100.0d - audit.riskScore) * 0.20d;
            score += audit.boost * 100.0d * 0.10d;
            score += Math.max(0.0d, 1.0d - audit.penalty) * 100.0d * 0.06d;
        } else {
            score += 50.0d;
            score -= c.auditPenalty * 100.0d * 0.24d;
            score += c.auditBoost * 100.0d * 0.18d;
        }

        return AppConfig.clampScore100(score);
    }

    private double sampleScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score +=
                Math.min(
                                1.0d,
                                ((double) c.effectiveSample)
                                        / ((double) AppConfig.EFFECTIVE_SAMPLE_HIGH))
                        * 42.0d;
        score +=
                Math.min(1.0d, ((double) c.comboSample) / ((double) AppConfig.COMBO_SAMPLE_HIGH))
                        * 34.0d;
        score += c.similarHitRate * 100.0d * 0.12d;
        score += c.comboHitRate * 100.0d * 0.12d;

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            score -= 10.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double shapeScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.trendLife * 0.22d;
        score += Math.max(0.0d, 100.0d - c.driftRisk) * 0.22d;
        score += Math.max(0.0d, 100.0d - c.breakDepth) * 0.18d;
        score += c.recoveryScore * 0.14d;
        score += c.championGap * 100.0d * 0.10d;
        score += c.comboGap * 100.0d * 0.14d;

        if (AppConfig.MODE_CONFIRMED.equals(c.stateMode)) {
            score += 4.0d;
        } else if (AppConfig.MODE_ACCELERATING.equals(c.stateMode)) {
            score += 5.0d;
        } else if (AppConfig.MODE_RECOVERING.equals(c.stateMode)) {
            score += 2.0d;
        } else if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            score -= 10.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double edgeScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 50.0d;
        score += c.edge * 230.0d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 5.0d;
        }

        if (c.edge >= 0.024d) {
            score += 5.0d;
        }

        if (c.edge < 0.0d) {
            score -= 12.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double riskScore(Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit) {
        if (c == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += c.evidenceRisk * 100.0d * 0.18d;
        risk += c.auditPenalty * 100.0d * 0.14d;
        risk += c.driftRisk * 0.16d;
        risk += c.breakDepth * 0.14d;
        risk += c.hotReverseRisk * 0.08d;
        risk += c.falseStrongRisk * 0.08d;

        if (audit != null) {
            risk += audit.riskScore * 0.18d;
        } else {
            risk += 8.0d;
        }

        if (c.calibrationError >= 0.100d) {
            risk += 8.0d;
        }

        if (c.finalP < c.dynamicGate) {
            risk += 8.0d;
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 6.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 6.0d;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            risk += 10.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double penaltyScore(Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit) {
        if (c == null) {
            return 100.0d;
        }

        double penalty = 0.0d;

        penalty += c.auditPenalty * 100.0d * 0.20d;
        penalty += c.evidenceRisk * 100.0d * 0.14d;

        if (audit != null) {
            penalty += audit.penalty * 100.0d * 0.18d;
            penalty += audit.riskScore * 0.12d;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            penalty += 8.0d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            penalty += 8.0d;
        }

        if (c.finalP < c.dynamicGate) {
            penalty += 8.0d;
        }

        if (c.calibrationError >= 0.100d) {
            penalty += 8.0d;
        }

        return AppConfig.clampScore100(penalty);
    }

    private double bonusScore(Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit) {
        if (c == null) {
            return 0.0d;
        }

        double bonus = 0.0d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            bonus += 4.0d;
        }

        if (c.evidenceScore >= 0.620d && c.evidenceRisk <= 0.480d) {
            bonus += 4.0d;
        }

        if (c.aiJudgeScore >= 0.620d && c.aiJudgeConfidence >= 0.580d) {
            bonus += 3.0d;
        }

        if (c.profileScore >= 0.580d || c.fallbackProfileScore >= 0.580d) {
            bonus += 3.0d;
        }

        if (audit != null && audit.reliabilityScore >= 62.0d && audit.riskScore <= 52.0d) {
            bonus += 3.0d;
        }

        if (c.trendLife >= 70.0d
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                && c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
            bonus += 3.0d;
        }

        return AppConfig.clampScore100(bonus);
    }

    private double finalPromotionScore(
            Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit, PromotionResult r) {
        if (c == null || r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.probabilityScore * 0.15d;
        score += r.evidenceScore * 0.20d;
        score += r.aiScore * 0.12d;
        score += r.profileScore * 0.12d;
        score += r.auditScore * 0.12d;
        score += r.sampleScore * 0.10d;
        score += r.shapeScore * 0.11d;
        score += r.edgeScore * 0.08d;

        score += r.bonusScore;
        score -= r.penaltyScore * 0.12d;
        score -= r.riskScore * 0.10d;

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            score -= 7.0d;
        }

        if (c.finalP < c.dynamicGate) {
            score -= 7.0d;
        }

        if (audit != null && audit.riskScore >= RISK_HARD) {
            score -= 10.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double confidenceScore(
            Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit, PromotionResult r) {
        if (c == null || r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score +=
                Math.min(
                                1.0d,
                                ((double) c.effectiveSample)
                                        / ((double) AppConfig.EFFECTIVE_SAMPLE_HIGH))
                        * 20.0d;
        score +=
                Math.min(1.0d, ((double) c.comboSample) / ((double) AppConfig.COMBO_SAMPLE_HIGH))
                        * 16.0d;
        score += r.evidenceScore * 0.14d;
        score += r.aiScore * 0.12d;
        score += r.profileScore * 0.10d;
        score += r.auditScore * 0.12d;
        score += Math.max(0.0d, 100.0d - r.riskScore) * 0.16d;
        score += r.shapeScore * 0.10d;

        if (audit != null && audit.reliabilityScore >= 62.0d) {
            score += 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private String statusByPromotion(
            Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit, PromotionResult r) {
        if (c == null || r == null) {
            return PROMOTION_REJECTED;
        }

        String hard = hardRejectReason(c, audit, r);
        if (hard.length() > 0) {
            return PROMOTION_REJECTED;
        }

        if (r.promotionScore >= PROMOTION_APPROVED_SCORE
                && r.riskScore <= 52.0d
                && r.confidence >= 56.0d
                && c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY
                && c.finalP >= c.dynamicGate) {
            return PROMOTION_APPROVED;
        }

        if (r.promotionScore >= PROMOTION_CANDIDATE_SCORE
                && r.riskScore <= RISK_WARN
                && r.confidence >= 46.0d) {
            return PROMOTION_CANDIDATE;
        }

        if (r.promotionScore >= PROMOTION_WATCH_SCORE && r.riskScore <= 72.0d) {
            return PROMOTION_WATCH;
        }

        if (r.riskScore >= RISK_WARN || c.finalP < c.dynamicGate) {
            return PROMOTION_DOWNGRADED;
        }

        return PROMOTION_NONE;
    }

    private String blockByPromotion(
            Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit, PromotionResult r) {
        if (c == null || r == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (!c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (audit != null && audit.riskScore >= 86.0d && c.finalP < 0.430d) {
            return audit.blockCode == null || audit.blockCode.length() == 0
                    ? AppConfig.BLOCK_LOW_PROBABILITY
                    : audit.blockCode;
        }

        if (r.riskScore >= 86.0d && c.finalP < 0.430d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        return "";
    }

    private String hardRejectReason(
            Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit, PromotionResult r) {
        if (c == null || r == null) {
            return "候选为空";
        }

        if (!c.isValidK3()) {
            return "不是固定K3";
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            return "概率低于真实出手最低线";
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return "漂移硬风险";
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return "破位硬风险";
        }

        if (audit != null && audit.riskScore >= 86.0d) {
            return "概率审计硬风险";
        }

        if (r.riskScore >= 90.0d) {
            return "综合风险极高";
        }

        return "";
    }

    private ProbabilityAuditEngine.AuditResult findAudit(
            List<ProbabilityAuditEngine.AuditResult> list, Models.ComboCandidate c) {
        if (list == null || c == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            ProbabilityAuditEngine.AuditResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (r.position == c.position && r.k3Mask == c.k3Mask) {
                return r;
            }
        }

        for (int i = 0; i < list.size(); i++) {
            ProbabilityAuditEngine.AuditResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (r.position == c.position && r.n1 == c.n1 && r.n2 == c.n2 && r.n3 == c.n3) {
                return r;
            }
        }

        return null;
    }

    private void sortPromotionResults(List<PromotionResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<PromotionResult>() {
                    @Override
                    public int compare(PromotionResult a, PromotionResult b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            PromotionResult r = list.get(i);
            if (r != null) {
                r.rank = i + 1;
            }
        }
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
        score += c.promotionScore * 100.0d * 0.14d;
        score += c.aPlusScore * 100.0d * 0.08d;
        score -= c.evidenceRisk * 100.0d * 0.06d;
        score -= c.auditPenalty * 100.0d * 0.06d;

        if (PROMOTION_APPROVED.equals(c.promotionStatus)) {
            score += 4.0d;
        } else if (PROMOTION_CANDIDATE.equals(c.promotionStatus)) {
            score += 2.0d;
        }

        return score;
    }

    private String buildPromotionKey(PromotionResult r) {
        if (r == null) {
            return "";
        }

        return "PG_P"
                + r.position
                + "_N"
                + r.n1
                + "_"
                + r.n2
                + "_"
                + r.n3
                + "_S"
                + bucket(r.promotionScore)
                + "_R"
                + bucket(r.riskScore);
    }

    private int bucket(double score) {
        if (score >= 80.0d) return 8;
        if (score >= 70.0d) return 7;
        if (score >= 60.0d) return 6;
        if (score >= 50.0d) return 5;
        if (score >= 40.0d) return 4;
        if (score >= 30.0d) return 3;
        if (score >= 20.0d) return 2;
        return 1;
    }

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(oldCode)) {
            return oldCode;
        }

        return newCode == null || newCode.length() == 0 ? AppConfig.BLOCK_COMBO_WEAK : newCode;
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildSummary(
            PromotionResult r, Models.ComboCandidate c, ProbabilityAuditEngine.AuditResult audit) {
        if (r == null) {
            return "A+晋级为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("A+晋级P");
        sb.append(r.position);
        sb.append(" ");
        sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));

        sb.append("：状态=");
        sb.append(r.status);

        sb.append("，晋级分=");
        sb.append(UiFormatter.score(r.promotionScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(r.riskScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(r.confidence));

        sb.append("，概率=");
        sb.append(UiFormatter.score(r.probabilityScore));

        sb.append("，证据=");
        sb.append(UiFormatter.score(r.evidenceScore));

        sb.append("，AI=");
        sb.append(UiFormatter.score(r.aiScore));

        sb.append("，画像=");
        sb.append(UiFormatter.score(r.profileScore));

        sb.append("，审计=");
        sb.append(UiFormatter.score(r.auditScore));

        sb.append("，FinalP=");
        sb.append(UiFormatter.probability(r.finalP));

        sb.append("，Gate=");
        sb.append(UiFormatter.probability(r.dynamicGate));

        sb.append("，Edge=");
        sb.append(UiFormatter.signedProbability(r.edge));

        if (audit != null) {
            sb.append("，审计状态=");
            sb.append(audit.status);
        }

        if (r.approved == 1) {
            sb.append("，允许进入A+候选层，但最终是否BET仍由DecisionGateEngine裁决");
        } else if (PROMOTION_DOWNGRADED.equals(r.status)) {
            sb.append("，降级观察");
        } else if (PROMOTION_REJECTED.equals(r.status)) {
            sb.append("，拒绝晋级");
        }

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        }

        return sb.toString();
    }

    private void normalizeCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
        }

        if (c.dynamicGate <= 0.0d
                || Double.isNaN(c.dynamicGate)
                || Double.isInfinite(c.dynamicGate)) {
            c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        if (Double.isNaN(c.finalP) || Double.isInfinite(c.finalP) || c.finalP < 0.0d) {
            c.finalP = 0.0d;
        }

        if (Double.isNaN(c.rawP) || Double.isInfinite(c.rawP) || c.rawP < 0.0d) {
            c.rawP = 0.0d;
        }

        if (Double.isNaN(c.calibratedP)
                || Double.isInfinite(c.calibratedP)
                || c.calibratedP < 0.0d) {
            c.calibratedP = 0.0d;
        }

        c.finalP = AppConfig.clampProbability(c.finalP);
        c.rawP = AppConfig.clampProbability(c.rawP);
        c.calibratedP = AppConfig.clampProbability(c.calibratedP);
        c.dynamicGate = AppConfig.clampProbability(c.dynamicGate);
        c.edge = c.finalP - c.dynamicGate;
    }

    private String append(String oldText, String addText) {
        if (addText == null || addText.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return safeReason(addText);
        }

        if (oldText.indexOf(addText) >= 0) {
            return safeReason(oldText);
        }

        return safeReason(oldText + "；" + addText);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeReason(String text) {
        if (text == null) {
            return "";
        }

        if (text.length() <= MAX_REASON_LEN) {
            return text;
        }

        return text.substring(0, MAX_REASON_LEN);
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private String aPlusCandidateText() {
        return readAppString("A_PLUS_CANDIDATE", "A_PLUS_CANDIDATE");
    }

    private String aPlusApprovedText() {
        return readAppString("A_PLUS_APPROVED", "A_PLUS_APPROVED");
    }

    private String aPlusNoneText() {
        return readAppString("A_PLUS_NONE", "A_PLUS_NONE");
    }

    private String readAppString(String fieldName, String def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readAppDouble(String fieldName, double def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);
                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    public static final class PromotionResult {
        public int rank;
        public long createdAt;

        public String promotionKey;
        public String baseIssue;
        public String targetIssue;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public double finalP;
        public double dynamicGate;
        public double edge;

        public double evidenceScore01;
        public double evidenceRisk01;
        public double aiJudgeScore01;
        public double aiJudgeConfidence01;
        public double aPlusScoreInput;
        public double profileScore01;
        public double fallbackProfileScore01;
        public double deepScore01;
        public double auditPenalty01;
        public double auditBoost01;

        public double probabilityScore;
        public double evidenceScore;
        public double aiScore;
        public double profileScore;
        public double auditScore;
        public double sampleScore;
        public double shapeScore;
        public double edgeScore;

        public double riskScore;
        public double penaltyScore;
        public double bonusScore;
        public double promotionScore;
        public double confidence;

        public int approved;
        public int aPlus;

        public String status;
        public String blockCode;
        public String summaryText;

        public PromotionResult() {
            rank = 0;
            createdAt = 0L;

            promotionKey = "";
            baseIssue = "";
            targetIssue = "";

            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            finalP = 0.0d;
            dynamicGate = 0.0d;
            edge = 0.0d;

            evidenceScore01 = 0.0d;
            evidenceRisk01 = 0.0d;
            aiJudgeScore01 = 0.0d;
            aiJudgeConfidence01 = 0.0d;
            aPlusScoreInput = 0.0d;
            profileScore01 = 0.0d;
            fallbackProfileScore01 = 0.0d;
            deepScore01 = 0.0d;
            auditPenalty01 = 0.0d;
            auditBoost01 = 0.0d;

            probabilityScore = 0.0d;
            evidenceScore = 0.0d;
            aiScore = 0.0d;
            profileScore = 0.0d;
            auditScore = 0.0d;
            sampleScore = 0.0d;
            shapeScore = 0.0d;
            edgeScore = 0.0d;

            riskScore = 100.0d;
            penaltyScore = 0.0d;
            bonusScore = 0.0d;
            promotionScore = 0.0d;
            confidence = 0.0d;

            approved = 0;
            aPlus = 0;

            status = PROMOTION_NONE;
            blockCode = "";
            summaryText = "";
        }

        public double aPlusScore01() {
            double score = promotionScore;

            if (approved == 1) {
                score += 6.0d;
            }

            if (aPlus == 1) {
                score += 6.0d;
            }

            score -= riskScore * 0.10d;

            return AppConfig.clampScore01(AppConfig.clampScore100(score) / 100.0d);
        }

        public boolean isApproved() {
            return approved == 1
                    && PROMOTION_APPROVED.equals(status)
                    && promotionScore >= PROMOTION_APPROVED_SCORE
                    && riskScore <= 52.0d
                    && confidence >= 56.0d;
        }

        public boolean isAPlus() {
            return isApproved()
                    && promotionScore >= PROMOTION_A_PLUS_SCORE
                    && riskScore <= 42.0d
                    && confidence >= 62.0d;
        }

        public double sortScore() {
            double score = promotionScore;
            score += confidence * 0.08d;
            score += bonusScore * 0.08d;
            score -= riskScore * 0.10d;
            score -= penaltyScore * 0.06d;

            if (PROMOTION_APPROVED.equals(status)) {
                score += 5.0d;
            } else if (PROMOTION_CANDIDATE.equals(status)) {
                score += 2.5d;
            } else if (PROMOTION_REJECTED.equals(status)) {
                score -= 8.0d;
            }

            if (aPlus == 1) {
                score += 4.0d;
            }

            return score;
        }

        public PromotionResult copy() {
            PromotionResult r = new PromotionResult();

            r.rank = rank;
            r.createdAt = createdAt;

            r.promotionKey = promotionKey;
            r.baseIssue = baseIssue;
            r.targetIssue = targetIssue;

            r.position = position;
            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;

            r.finalP = finalP;
            r.dynamicGate = dynamicGate;
            r.edge = edge;

            r.evidenceScore01 = evidenceScore01;
            r.evidenceRisk01 = evidenceRisk01;
            r.aiJudgeScore01 = aiJudgeScore01;
            r.aiJudgeConfidence01 = aiJudgeConfidence01;
            r.aPlusScoreInput = aPlusScoreInput;
            r.profileScore01 = profileScore01;
            r.fallbackProfileScore01 = fallbackProfileScore01;
            r.deepScore01 = deepScore01;
            r.auditPenalty01 = auditPenalty01;
            r.auditBoost01 = auditBoost01;

            r.probabilityScore = probabilityScore;
            r.evidenceScore = evidenceScore;
            r.aiScore = aiScore;
            r.profileScore = profileScore;
            r.auditScore = auditScore;
            r.sampleScore = sampleScore;
            r.shapeScore = shapeScore;
            r.edgeScore = edgeScore;

            r.riskScore = riskScore;
            r.penaltyScore = penaltyScore;
            r.bonusScore = bonusScore;
            r.promotionScore = promotionScore;
            r.confidence = confidence;

            r.approved = approved;
            r.aPlus = aPlus;

            r.status = status;
            r.blockCode = blockCode;
            r.summaryText = summaryText;

            return r;
        }
    }
}
