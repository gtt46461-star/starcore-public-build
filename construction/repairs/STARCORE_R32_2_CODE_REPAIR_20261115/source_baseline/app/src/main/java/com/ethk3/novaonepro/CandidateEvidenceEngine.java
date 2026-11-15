package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CandidateEvidenceEngine {

    private static final double EVIDENCE_USABLE = 52.0d;
    private static final double EVIDENCE_STRONG = 62.0d;
    private static final double EVIDENCE_A_PLUS = 72.0d;

    private static final double RISK_WARN = 62.0d;
    private static final double RISK_HARD = 78.0d;

    private final PositionStrengthEngine positionStrengthEngine;
    private final TransitionRelationEngine transitionRelationEngine;
    private final CycleRhythmEngine cycleRhythmEngine;
    private final DistributionDriftEngine distributionDriftEngine;
    private final BiasEvidenceEngine biasEvidenceEngine;

    public CandidateEvidenceEngine() {
        positionStrengthEngine = new PositionStrengthEngine();
        transitionRelationEngine = new TransitionRelationEngine();
        cycleRhythmEngine = new CycleRhythmEngine();
        distributionDriftEngine = new DistributionDriftEngine();
        biasEvidenceEngine = new BiasEvidenceEngine();
    }

    public Models.CandidateEvidence buildEvidence(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        EvidenceContext ctx = buildContext(history);
        return buildEvidenceWithContext(ctx, candidate);
    }

    public ArrayList<Models.CandidateEvidence> buildEvidenceList(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<Models.CandidateEvidence> out = new ArrayList<Models.CandidateEvidence>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        EvidenceContext ctx = buildContext(history);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.CandidateEvidence e = buildEvidenceWithContext(ctx, c);
            out.add(e);
        }

        sortEvidence(out);
        return out;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        EvidenceContext ctx = buildContext(history);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            Models.CandidateEvidence e = buildEvidenceWithContext(ctx, copy);
            applyToCandidate(copy, e);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public Models.CandidateBatch applyToBatch(
            Models.CandidateBatch batch, List<Models.Draw> history) {
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

        ArrayList<Models.ComboCandidate> list = applyToCandidates(batch.candidates, history);
        out.addAll(list);

        return out;
    }

    public void applyToCandidate(
            Models.ComboCandidate candidate, Models.CandidateEvidence evidence) {
        if (candidate == null) {
            return;
        }

        if (evidence == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "候选证据：暂无");
            return;
        }

        candidate.candidateEvidenceKey = evidence.evidenceKey;
        candidate.evidenceScore = AppConfig.clampScore01(evidence.evidenceScore / 100.0d);
        candidate.evidenceRisk = AppConfig.clampScore01(evidence.riskScore / 100.0d);

        candidate.deepNumberScore =
                blend01(candidate.deepNumberScore, evidence.numberEvidence / 100.0d, 0.18d);
        candidate.deepPositionScore =
                blend01(candidate.deepPositionScore, evidence.positionEvidence / 100.0d, 0.18d);
        candidate.deepCycleScore =
                blend01(candidate.deepCycleScore, evidence.cycleEvidence / 100.0d, 0.16d);
        candidate.deepTransitionScore =
                blend01(candidate.deepTransitionScore, evidence.transitionEvidence / 100.0d, 0.16d);
        candidate.deepStateScore =
                blend01(candidate.deepStateScore, evidence.patternEvidence / 100.0d, 0.14d);

        candidate.deepScore = blend01(candidate.deepScore, evidence.evidenceScore / 100.0d, 0.20d);
        candidate.auditPenalty =
                blend01(
                        candidate.auditPenalty,
                        Math.max(0.0d, evidence.riskScore - 55.0d) / 100.0d,
                        0.14d);

        if (evidence.evidenceScore >= EVIDENCE_STRONG
                && evidence.riskScore <= 48.0d
                && evidence.confidence >= 58.0d
                && candidate.finalP >= 0.360d) {
            candidate.rawP = AppConfig.clampProbability(candidate.rawP + 0.004d);
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + 0.004d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        if (evidence.evidenceScore >= EVIDENCE_A_PLUS
                && evidence.riskScore <= 42.0d
                && evidence.confidence >= 66.0d) {
            candidate.aPlusScore =
                    blend01(candidate.aPlusScore, evidence.evidenceScore / 100.0d, 0.18d);
            if (candidate.aPlusStatus == null
                    || candidate.aPlusStatus.length() == 0
                    || AppConfig.A_PLUS_NONE.equals(candidate.aPlusStatus)) {
                candidate.aPlusStatus = AppConfig.A_PLUS_CANDIDATE;
            }
        }

        if (evidence.riskScore >= RISK_HARD && candidate.finalP < 0.430d) {
            candidate.blockCode = pickBlockCode(candidate.blockCode, AppConfig.BLOCK_COMBO_WEAK);
        } else if (evidence.riskScore >= RISK_WARN
                && candidate.finalP < 0.400d
                && (candidate.blockCode == null || candidate.blockCode.length() == 0)) {
            candidate.blockCode = AppConfig.BLOCK_COMBO_WEAK;
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "候选证据：总分="
                                + UiFormatter.score(evidence.evidenceScore)
                                + "，风险="
                                + UiFormatter.score(evidence.riskScore)
                                + "，置信="
                                + UiFormatter.score(evidence.confidence)
                                + "，位置="
                                + UiFormatter.score(evidence.positionEvidence)
                                + "，号码="
                                + UiFormatter.score(evidence.numberEvidence)
                                + "，周期="
                                + UiFormatter.score(evidence.cycleEvidence)
                                + "，转移="
                                + UiFormatter.score(evidence.transitionEvidence)
                                + "，组合="
                                + UiFormatter.score(evidence.comboEvidence));
    }

    public Models.CandidateEvidence bestEvidence(List<Models.CandidateEvidence> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        Models.CandidateEvidence best = null;

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateEvidence e = list.get(i);

            if (e == null) {
                continue;
            }

            if (best == null || scoreForSort(e) > scoreForSort(best)) {
                best = e;
            }
        }

        return best;
    }

    public Models.ComboCandidate bestCandidateAfterEvidence(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> list = applyToCandidates(candidates, history);

        if (list.size() == 0) {
            return null;
        }

        return list.get(0);
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        ArrayList<Models.CandidateEvidence> list = buildEvidenceList(history, candidates);
        Models.CandidateEvidence best = bestEvidence(list);

        if (best == null) {
            return set;
        }

        set.bestPosition = best.position;
        set.bestNumbers = new int[] {best.n1, best.n2, best.n3};
        set.globalBiasScore = Math.max(set.globalBiasScore, best.evidenceScore);
        set.globalTrendStrength = Math.max(set.globalTrendStrength, best.evidenceScore);
        set.profileKey = best.evidenceKey;
        set.summaryText = append(set.summaryText, best.summaryText);

        return set;
    }

    public String buildSummary(List<Models.CandidateEvidence> list) {
        if (list == null || list.size() == 0) {
            return "候选证据：暂无";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            Models.CandidateEvidence e = list.get(i);
            if (e == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(e.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(e.n1, e.n2, e.n3));
            sb.append(" 分=");
            sb.append(UiFormatter.score(e.evidenceScore));
            sb.append(" 风险=");
            sb.append(UiFormatter.score(e.riskScore));
        }

        return sb.length() == 0 ? "候选证据：暂无有效候选" : sb.toString();
    }

    private Models.CandidateEvidence buildEvidenceWithContext(
            EvidenceContext ctx, Models.ComboCandidate candidate) {
        Models.CandidateEvidence e = new Models.CandidateEvidence();
        e.createdAt = System.currentTimeMillis();

        if (ctx != null) {
            e.baseIssue = ctx.baseIssue;
            e.targetIssue = ctx.targetIssue;
        }

        if (candidate == null || !candidate.isValidK3()) {
            e.evidenceKey = "EV_INVALID";
            e.summaryText = "候选证据：候选为空或不是固定K3";
            e.riskText = "候选非法，不能作为真实出手依据";
            e.riskScore = 100.0d;
            e.confidence = 0.0d;
            return e;
        }

        e.position = candidate.position;
        e.n1 = candidate.n1;
        e.n2 = candidate.n2;
        e.n3 = candidate.n3;
        e.k3Mask = candidate.k3Mask;
        e.baseIssue =
                candidate.baseIssue == null || candidate.baseIssue.length() == 0
                        ? e.baseIssue
                        : candidate.baseIssue;
        e.targetIssue =
                candidate.targetIssue == null || candidate.targetIssue.length() == 0
                        ? e.targetIssue
                        : candidate.targetIssue;

        PositionStrengthEngine.PositionStrengthResult ps =
                ctx == null
                        ? null
                        : positionStrengthEngine.findForPosition(
                                ctx.positionSignals, candidate.position);

        TransitionRelationEngine.TransitionRelationResult tr =
                ctx == null
                        ? null
                        : transitionRelationEngine.findForPosition(
                                ctx.transitionSignals, candidate.position);

        CycleRhythmEngine.CycleRhythmResult cy =
                ctx == null
                        ? null
                        : cycleRhythmEngine.findForPosition(ctx.cycleSignals, candidate.position);

        DistributionDriftEngine.DistributionDriftResult df =
                ctx == null
                        ? null
                        : distributionDriftEngine.findForPosition(
                                ctx.driftSignals, candidate.position);

        BiasEvidenceEngine.BiasSignal bs =
                ctx == null
                        ? null
                        : biasEvidenceEngine.findForPosition(ctx.biasSignals, candidate.position);

        e.positionEvidence = positionEvidence(candidate, ps);
        e.numberEvidence = numberEvidence(candidate, ps, tr, cy, bs);
        e.cycleEvidence = cycleEvidence(candidate, cy);
        e.transitionEvidence = transitionEvidence(candidate, tr);
        e.patternEvidence = patternEvidence(candidate, ps, tr, cy, df, bs);
        e.comboEvidence = comboEvidence(candidate, bs);
        e.profileEvidence = profileEvidence(candidate);
        e.auditEvidence = auditEvidence(candidate, df);

        e.riskScore = finalRiskScore(candidate, e, df, bs);
        e.confidence = finalConfidence(candidate, e);
        e.evidenceScore = finalEvidenceScore(candidate, e);

        e.supportText = buildSupportText(e, candidate, ps, tr, cy, bs);
        e.riskText = buildRiskText(e, candidate, df, bs);
        e.summaryText = buildEvidenceSummary(e, candidate);
        e.evidenceKey = buildEvidenceKey(e);

        return e;
    }

    private EvidenceContext buildContext(List<Models.Draw> history) {
        EvidenceContext ctx = new EvidenceContext();

        ctx.history = cleanHistory(history);

        if (ctx.history.size() > 0) {
            Models.Draw latest = ctx.history.get(ctx.history.size() - 1);
            ctx.baseIssue = latest.issue == null ? "" : latest.issue;
            ctx.targetIssue = AppConfig.nextIssueText(ctx.baseIssue);
        }

        ctx.positionSignals = positionStrengthEngine.analyzeAll(ctx.history);
        ctx.transitionSignals = transitionRelationEngine.analyzeAll(ctx.history);
        ctx.cycleSignals = cycleRhythmEngine.analyzeAll(ctx.history);
        ctx.driftSignals = distributionDriftEngine.analyzeAll(ctx.history);
        ctx.biasSignals = biasEvidenceEngine.analyzeAllPositions(ctx.history);

        return ctx;
    }

    private double positionEvidence(
            Models.ComboCandidate c, PositionStrengthEngine.PositionStrengthResult ps) {
        if (c == null || ps == null) {
            return 0.0d;
        }

        double score = ps.score;

        if (ps.position == c.position) {
            score += 4.0d;
        }

        if (ps.usable == 1) {
            score += 4.0d;
        }

        if (ps.strong == 1) {
            score += 4.0d;
        }

        if (ps.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            score -= 6.0d;
        }

        if (ps.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double numberEvidence(
            Models.ComboCandidate c,
            PositionStrengthEngine.PositionStrengthResult ps,
            TransitionRelationEngine.TransitionRelationResult tr,
            CycleRhythmEngine.CycleRhythmResult cy,
            BiasEvidenceEngine.BiasSignal bs) {
        if (c == null || !c.isValidK3()) {
            return 0.0d;
        }

        double score = 0.0d;
        int count = 0;

        if (ps != null) {
            score += coverageScore(ps.k3Mask, c.k3Mask) * 100.0d;
            count++;
        }

        if (tr != null) {
            score += coverageScore(tr.k3Mask, c.k3Mask) * 100.0d;
            count++;
        }

        if (cy != null) {
            score += coverageScore(cy.k3Mask, c.k3Mask) * 100.0d;
            count++;
        }

        if (bs != null) {
            score += coverageScore(bs.k3Mask, c.k3Mask) * 100.0d;
            count++;
        }

        double base = count > 0 ? score / ((double) count) : 0.0d;

        if (c.weakestNumberScore > 0.0d) {
            base = base * 0.84d + c.weakestNumberScore * 100.0d * 0.16d;
        }

        if (c.balanceScore > 0.0d) {
            base = base * 0.90d + c.balanceScore * 100.0d * 0.10d;
        }

        return AppConfig.clampScore100(base);
    }

    private double cycleEvidence(Models.ComboCandidate c, CycleRhythmEngine.CycleRhythmResult cy) {
        if (c == null || cy == null) {
            return 0.0d;
        }

        double coverage = coverageScore(cy.k3Mask, c.k3Mask);
        double score =
                cy.rhythmScore * 0.62d
                        + coverage * 100.0d * 0.24d
                        + cy.recoveryScore * 0.08d
                        + cy.hotContinuationScore * 0.06d;

        score -= cy.reverseRisk * 0.12d;

        return AppConfig.clampScore100(score);
    }

    private double transitionEvidence(
            Models.ComboCandidate c, TransitionRelationEngine.TransitionRelationResult tr) {
        if (c == null || tr == null) {
            return 0.0d;
        }

        double coverage = coverageScore(tr.k3Mask, c.k3Mask);

        double score =
                tr.relationScore * 0.62d
                        + coverage * 100.0d * 0.20d
                        + tr.transitionSupport * 0.08d
                        + tr.transitionLift * 0.06d
                        + tr.recentTransitionScore * 0.04d;

        if (tr.usable == 0) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double patternEvidence(
            Models.ComboCandidate c,
            PositionStrengthEngine.PositionStrengthResult ps,
            TransitionRelationEngine.TransitionRelationResult tr,
            CycleRhythmEngine.CycleRhythmResult cy,
            DistributionDriftEngine.DistributionDriftResult df,
            BiasEvidenceEngine.BiasSignal bs) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;
        int count = 0;

        if (ps != null) {
            score += ps.score;
            count++;
        }

        if (tr != null) {
            score += tr.relationScore;
            count++;
        }

        if (cy != null) {
            score += cy.rhythmScore;
            count++;
        }

        if (bs != null) {
            score += bs.biasScore;
            count++;
        }

        if (df != null) {
            score += Math.max(0.0d, 100.0d - df.driftRisk);
            count++;
        }

        if (count <= 0) {
            return 0.0d;
        }

        double out = score / ((double) count);

        if (AppConfig.MODE_CONFIRMED.equals(c.stateMode)
                || AppConfig.MODE_ACCELERATING.equals(c.stateMode)) {
            out += 4.0d;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            out -= 8.0d;
        }

        return AppConfig.clampScore100(out);
    }

    private double comboEvidence(Models.ComboCandidate c, BiasEvidenceEngine.BiasSignal bs) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.comboGap * 100.0d * 0.28d;
        score += c.championGap * 100.0d * 0.18d;
        score += c.comboHitRate * 100.0d * 0.18d;
        score += c.finalP * 100.0d * 0.18d;
        score += c.edge * 100.0d * 0.08d;

        if (bs != null) {
            score += bs.comboBiasScore * 0.10d;
        }

        if (c.comboSample >= AppConfig.COMBO_SAMPLE_HIGH) {
            score += 4.0d;
        } else if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double profileEvidence(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.profileScore * 100.0d * 0.26d;
        score += c.fallbackProfileScore * 100.0d * 0.14d;
        score += c.deepScore * 100.0d * 0.16d;
        score += c.aiJudgeScore * 100.0d * 0.18d;
        score += c.aiJudgeConfidence * 100.0d * 0.10d;
        score += c.aPlusScore * 100.0d * 0.10d;
        score += c.promotionScore * 100.0d * 0.06d;

        if (c.deepProfileKey != null && c.deepProfileKey.length() > 0) {
            score += 3.0d;
        }

        if (c.fallbackProfileKey != null && c.fallbackProfileKey.length() > 0) {
            score += 1.5d;
        }

        return AppConfig.clampScore100(score);
    }

    private double auditEvidence(
            Models.ComboCandidate c, DistributionDriftEngine.DistributionDriftResult df) {
        if (c == null) {
            return 0.0d;
        }

        double score = 70.0d;

        score -= c.auditPenalty * 100.0d * 0.46d;
        score += c.auditBoost * 100.0d * 0.18d;

        if (df != null) {
            score -= df.driftRisk * 0.20d;
            score += df.stabilityScore * 0.16d;
        }

        if (c.calibrationError >= 0.080d) {
            score -= 10.0d;
        } else if (c.calibrationError <= 0.030d && c.calibratedP > 0.0d) {
            score += 4.0d;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalRiskScore(
            Models.ComboCandidate c,
            Models.CandidateEvidence e,
            DistributionDriftEngine.DistributionDriftResult df,
            BiasEvidenceEngine.BiasSignal bs) {
        if (c == null || e == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += (100.0d - e.auditEvidence) * 0.22d;

        if (df != null) {
            risk += df.driftRisk * 0.24d;
            risk += df.strongNumberShift * 0.07d;
            risk += df.entropyRise * 0.05d;
        } else {
            risk += 12.0d;
        }

        if (bs != null) {
            risk += bs.riskScore * 0.16d;
        }

        risk += c.evidenceRisk * 100.0d * 0.08d;
        risk += c.auditPenalty * 100.0d * 0.08d;
        risk += c.driftRisk * 0.10d;

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            risk += 10.0d;
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 6.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            risk += 5.0d;
        }

        if (c.calibrationError >= 0.100d) {
            risk += 8.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double finalConfidence(Models.ComboCandidate c, Models.CandidateEvidence e) {
        if (c == null || e == null) {
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

        score += e.positionEvidence * 0.12d;
        score += e.numberEvidence * 0.12d;
        score += e.cycleEvidence * 0.10d;
        score += e.transitionEvidence * 0.10d;
        score += e.patternEvidence * 0.10d;
        score += e.comboEvidence * 0.10d;
        score += e.profileEvidence * 0.08d;
        score += e.auditEvidence * 0.08d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 4.0d;
        }

        if (c.aiJudgeConfidence > 0.0d) {
            score = score * 0.90d + c.aiJudgeConfidence * 100.0d * 0.10d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalEvidenceScore(Models.ComboCandidate c, Models.CandidateEvidence e) {
        if (c == null || e == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += e.positionEvidence * 0.14d;
        score += e.numberEvidence * 0.16d;
        score += e.cycleEvidence * 0.12d;
        score += e.transitionEvidence * 0.12d;
        score += e.patternEvidence * 0.13d;
        score += e.comboEvidence * 0.15d;
        score += e.profileEvidence * 0.10d;
        score += e.auditEvidence * 0.08d;

        score += e.confidence * 0.05d;
        score -= e.riskScore * 0.14d;

        if (isResonance(e)) {
            score += 5.0d;
        }

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 3.0d;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            score -= 4.0d;
        }

        if (e.riskScore >= RISK_HARD) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private boolean isResonance(Models.CandidateEvidence e) {
        if (e == null) {
            return false;
        }

        int count = 0;

        if (e.positionEvidence >= EVIDENCE_STRONG) count++;
        if (e.numberEvidence >= EVIDENCE_STRONG) count++;
        if (e.cycleEvidence >= EVIDENCE_STRONG) count++;
        if (e.transitionEvidence >= EVIDENCE_STRONG) count++;
        if (e.comboEvidence >= EVIDENCE_STRONG) count++;
        if (e.patternEvidence >= EVIDENCE_STRONG) count++;

        return count >= 3 && e.riskScore <= 56.0d;
    }

    private double coverageScore(int baseMask, int candidateMask) {
        if (!Models.isValidK3Mask(baseMask) || !Models.isValidK3Mask(candidateMask)) {
            return 0.0d;
        }

        int hit = 0;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(baseMask, n) && Models.maskContains(candidateMask, n)) {
                hit++;
            }
        }

        return AppConfig.clampProbability(((double) hit) / 3.0d);
    }

    private double scoreForSort(Models.CandidateEvidence e) {
        if (e == null) {
            return -999.0d;
        }

        double s = e.evidenceScore;
        s += e.confidence * 0.08d;
        s -= e.riskScore * 0.10d;

        if (e.isUsable()) {
            s += 4.0d;
        }

        if (e.evidenceScore >= EVIDENCE_A_PLUS && e.riskScore <= 42.0d) {
            s += 4.0d;
        }

        return s;
    }

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)) {
            return oldCode;
        }

        return newCode == null || newCode.length() == 0 ? AppConfig.BLOCK_COMBO_WEAK : newCode;
    }

    private ArrayList<Models.Draw> cleanHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return out;
        }

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private void sortEvidence(List<Models.CandidateEvidence> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.CandidateEvidence>() {
                    @Override
                    public int compare(Models.CandidateEvidence a, Models.CandidateEvidence b) {
                        double sa = scoreForSort(a);
                        double sb = scoreForSort(b);

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int pa = a == null ? 999 : a.position;
                        int pb = b == null ? 999 : b.position;

                        return pa - pb;
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
            Models.ComboCandidate c = list.get(i);
            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildEvidenceKey(Models.CandidateEvidence e) {
        if (e == null) {
            return "";
        }

        return "EV_P"
                + e.position
                + "_N"
                + e.n1
                + "_"
                + e.n2
                + "_"
                + e.n3
                + "_S"
                + bucket(e.evidenceScore)
                + "_R"
                + bucket(e.riskScore);
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

    private String buildSupportText(
            Models.CandidateEvidence e,
            Models.ComboCandidate c,
            PositionStrengthEngine.PositionStrengthResult ps,
            TransitionRelationEngine.TransitionRelationResult tr,
            CycleRhythmEngine.CycleRhythmResult cy,
            BiasEvidenceEngine.BiasSignal bs) {
        if (e == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("支持：位置");
        sb.append(UiFormatter.score(e.positionEvidence));
        sb.append("，号码");
        sb.append(UiFormatter.score(e.numberEvidence));
        sb.append("，组合");
        sb.append(UiFormatter.score(e.comboEvidence));
        sb.append("，周期");
        sb.append(UiFormatter.score(e.cycleEvidence));
        sb.append("，转移");
        sb.append(UiFormatter.score(e.transitionEvidence));
        sb.append("，形态");
        sb.append(UiFormatter.score(e.patternEvidence));

        if (ps != null && ps.strong == 1) {
            sb.append("；位置强");
        }

        if (tr != null && tr.strong == 1) {
            sb.append("；转移强");
        }

        if (cy != null && cy.strong == 1) {
            sb.append("；周期强");
        }

        if (bs != null && bs.isStrong()) {
            sb.append("；偏差强");
        }

        if (c != null && c.finalP >= c.dynamicGate) {
            sb.append("；概率过门");
        }

        return sb.toString();
    }

    private String buildRiskText(
            Models.CandidateEvidence e,
            Models.ComboCandidate c,
            DistributionDriftEngine.DistributionDriftResult df,
            BiasEvidenceEngine.BiasSignal bs) {
        if (e == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("风险：总风险");
        sb.append(UiFormatter.score(e.riskScore));

        if (df != null) {
            sb.append("，漂移");
            sb.append(UiFormatter.score(df.driftRisk));
        }

        if (bs != null) {
            sb.append("，偏差噪声");
            sb.append(UiFormatter.score(bs.noiseRisk));
        }

        if (c != null) {
            sb.append("，校准误差");
            sb.append(UiFormatter.probability(c.calibrationError));

            if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                    || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
                sb.append("，状态风险=");
                sb.append(UiFormatter.modeText(c.stateMode));
            }
        }

        if (e.riskScore >= RISK_HARD) {
            sb.append("，强风险拦截");
        } else if (e.riskScore >= RISK_WARN) {
            sb.append("，风险偏高");
        } else {
            sb.append("，风险可控");
        }

        return sb.toString();
    }

    private String buildEvidenceSummary(Models.CandidateEvidence e, Models.ComboCandidate c) {
        if (e == null) {
            return "候选证据为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("候选证据P");
        sb.append(e.position);
        sb.append("：");
        sb.append(UiFormatter.numberGroup(e.n1, e.n2, e.n3));

        sb.append("，总分=");
        sb.append(UiFormatter.score(e.evidenceScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(e.riskScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(e.confidence));

        sb.append("，位置=");
        sb.append(UiFormatter.score(e.positionEvidence));

        sb.append("，号码=");
        sb.append(UiFormatter.score(e.numberEvidence));

        sb.append("，周期=");
        sb.append(UiFormatter.score(e.cycleEvidence));

        sb.append("，转移=");
        sb.append(UiFormatter.score(e.transitionEvidence));

        sb.append("，组合=");
        sb.append(UiFormatter.score(e.comboEvidence));

        if (isResonance(e)) {
            sb.append("，多证据共振");
        }

        if (c != null) {
            sb.append("，FinalP=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append("，Gate=");
            sb.append(UiFormatter.probability(c.dynamicGate));
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

    private static final class EvidenceContext {
        public ArrayList<Models.Draw> history;
        public String baseIssue;
        public String targetIssue;

        public ArrayList<PositionStrengthEngine.PositionStrengthResult> positionSignals;
        public ArrayList<TransitionRelationEngine.TransitionRelationResult> transitionSignals;
        public ArrayList<CycleRhythmEngine.CycleRhythmResult> cycleSignals;
        public ArrayList<DistributionDriftEngine.DistributionDriftResult> driftSignals;
        public ArrayList<BiasEvidenceEngine.BiasSignal> biasSignals;

        public EvidenceContext() {
            history = new ArrayList<Models.Draw>();
            baseIssue = "";
            targetIssue = "";

            positionSignals = new ArrayList<PositionStrengthEngine.PositionStrengthResult>();
            transitionSignals = new ArrayList<TransitionRelationEngine.TransitionRelationResult>();
            cycleSignals = new ArrayList<CycleRhythmEngine.CycleRhythmResult>();
            driftSignals = new ArrayList<DistributionDriftEngine.DistributionDriftResult>();
            biasSignals = new ArrayList<BiasEvidenceEngine.BiasSignal>();
        }
    }
}
