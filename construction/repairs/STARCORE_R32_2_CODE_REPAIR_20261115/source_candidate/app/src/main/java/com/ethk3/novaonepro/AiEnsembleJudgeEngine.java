package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Compatibility ensemble judge used by manual deep backtest. It never creates BET/Trade and only
 * ranks candidates plus returns advice.
 */
public final class AiEnsembleJudgeEngine {
    public static final String VERSION = AppConfig.OUMAN_GALAXY_REBUILD_VERSION;
    private final CandidateEvidenceEngine evidenceEngine;

    public AiEnsembleJudgeEngine() {
        evidenceEngine = new CandidateEvidenceEngine();
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<Models.ComboCandidate> out =
                evidenceEngine.applyToCandidates(candidates, history);
        Collections.sort(
                out,
                new Comparator<Models.ComboCandidate>() {
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = score(a);
                        double sb = score(b);
                        if (sa > sb) return -1;
                        if (sa < sb) return 1;
                        return 0;
                    }
                });
        return out;
    }

    public Models.AiJudgeResult judge(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        Models.AiJudgeResult r = new Models.AiJudgeResult();
        r.createdAt = System.currentTimeMillis();
        if (candidates == null || candidates.size() == 0) {
            r.betAdvice = AppConfig.AI_JUDGE_HOLD;
            r.holdReason = "NO_CANDIDATE";
            r.summaryText = "AI组合审判：无候选";
            return r;
        }
        ArrayList<Models.CandidateEvidence> ev =
                evidenceEngine.buildEvidenceList(history, candidates);
        if (ev.size() > 0) r.bestCandidate = ev.get(0);
        if (ev.size() > 1) r.secondCandidate = ev.get(1);
        double best = r.bestCandidate == null ? 0.0d : normalize(r.bestCandidate.evidenceScore);
        double second =
                r.secondCandidate == null ? 0.0d : normalize(r.secondCandidate.evidenceScore);
        double risk = r.bestCandidate == null ? 1.0d : normalize(r.bestCandidate.riskScore);
        double confidence = r.bestCandidate == null ? 0.0d : normalize(r.bestCandidate.confidence);
        r.championGap = Math.max(0.0d, best - second);
        r.judgeScore = clamp01(best * 0.68d + confidence * 0.22d + r.championGap * 0.10d);
        r.judgeConfidence =
                clamp01(confidence * 0.72d + Math.min(1.0d, candidates.size() / 40.0d) * 0.28d);
        r.conflictLevel = clamp01(risk);
        if (r.conflictLevel >= AppConfig.AI_JUDGE_CONFLICT_HARD) {
            r.betAdvice = AppConfig.AI_JUDGE_RISK;
            r.holdReason = "HARD_CONFLICT";
        } else if (r.judgeScore >= AppConfig.AI_JUDGE_SCORE_MIN
                && r.judgeConfidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN
                && r.conflictLevel <= AppConfig.AI_JUDGE_CONFLICT_MAX) {
            r.betAdvice = AppConfig.AI_JUDGE_BET;
            r.holdReason = "";
        } else {
            r.betAdvice = AppConfig.AI_JUDGE_HOLD;
            r.holdReason = "EVIDENCE_NOT_READY";
        }
        r.summaryText =
                "AI组合审判：score="
                        + UiFormatter.probability(r.judgeScore)
                        + "，confidence="
                        + UiFormatter.probability(r.judgeConfidence)
                        + "，conflict="
                        + UiFormatter.probability(r.conflictLevel)
                        + "，advice="
                        + r.betAdvice;
        return r;
    }

    private static double score(Models.ComboCandidate c) {
        if (c == null) return -1.0d;
        double p = c.finalP;
        double e = c.evidenceScore;
        double r = c.evidenceRisk;
        double q = c.deepScore;
        return p * 0.45d + e * 0.25d + q * 0.20d - r * 0.10d;
    }

    private static double normalize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.0d) v /= 100.0d;
        return clamp01(v);
    }

    private static double clamp01(double v) {
        if (v < 0.0d) return 0.0d;
        if (v > 1.0d) return 1.0d;
        return v;
    }
}
