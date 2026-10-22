package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class EvidenceRankerEngine {

    private static final double STRONG_RANK_SCORE = 62.0d;
    private static final double A_PLUS_RANK_SCORE = 72.0d;
    private static final double HARD_RISK_SCORE = 78.0d;
    private static final double WARN_RISK_SCORE = 62.0d;

    private final CandidateEvidenceEngine candidateEvidenceEngine;

    public EvidenceRankerEngine() {
        candidateEvidenceEngine = new CandidateEvidenceEngine();
    }

    public ArrayList<RankedCandidate> rankCandidates(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<RankedCandidate> out = new ArrayList<RankedCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<Models.CandidateEvidence> evidenceList =
                candidateEvidenceEngine.buildEvidenceList(history, candidates);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            Models.CandidateEvidence evidence = findEvidence(evidenceList, copy);

            if (evidence == null) {
                evidence = candidateEvidenceEngine.buildEvidence(history, copy);
            }

            candidateEvidenceEngine.applyToCandidate(copy, evidence);

            RankedCandidate r = buildRanked(copy, evidence);
            out.add(r);
        }

        sortAndRank(out);
        return out;
    }

    public ArrayList<Models.ComboCandidate> rankAndApply(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        ArrayList<RankedCandidate> ranked = rankCandidates(history, candidates);

        for (int i = 0; i < ranked.size(); i++) {
            RankedCandidate r = ranked.get(i);
            if (r == null || r.candidate == null) {
                continue;
            }

            Models.ComboCandidate c = r.candidate.copy();
            c.rank = r.rank;
            c.evidenceScore = AppConfig.clampScore01(r.evidenceScore / 100.0d);
            c.evidenceRisk = AppConfig.clampScore01(r.riskScore / 100.0d);

            if (r.evidence != null) {
                c.candidateEvidenceKey = r.evidence.evidenceKey;
            }

            c.reasonSummary = append(c.reasonSummary, r.summaryText);

            out.add(c);
        }

        return out;
    }

    public Models.CandidateBatch rankBatch(Models.CandidateBatch batch, List<Models.Draw> history) {
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

        ArrayList<Models.ComboCandidate> list = rankAndApply(history, batch.candidates);
        out.addAll(list);

        return out;
    }

    public Models.ComboCandidate pickBest(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<RankedCandidate> ranked = rankCandidates(history, candidates);

        if (ranked.size() == 0) {
            return null;
        }

        RankedCandidate best = ranked.get(0);

        if (best == null || best.candidate == null) {
            return null;
        }

        return best.candidate.copy();
    }

    public RankedCandidate pickBestRanked(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        ArrayList<RankedCandidate> ranked = rankCandidates(history, candidates);

        if (ranked.size() == 0) {
            return null;
        }

        return ranked.get(0);
    }

    public ArrayList<Models.ComboCandidate> topCandidates(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates, int limit) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (limit <= 0) {
            limit = 10;
        }

        ArrayList<RankedCandidate> ranked = rankCandidates(history, candidates);
        int count = Math.min(limit, ranked.size());

        for (int i = 0; i < count; i++) {
            RankedCandidate r = ranked.get(i);
            if (r != null && r.candidate != null) {
                out.add(r.candidate.copy());
            }
        }

        return out;
    }

    public Models.CandidateEvidence bestEvidence(
            List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        RankedCandidate r = pickBestRanked(history, candidates);

        if (r == null) {
            return null;
        }

        return r.evidence;
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set,
            List<Models.ComboCandidate> candidates,
            List<Models.Draw> history) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        RankedCandidate best = pickBestRanked(history, candidates);

        if (best == null || best.candidate == null) {
            return set;
        }

        set.bestPosition = best.candidate.position;
        set.bestNumbers = new int[] {best.candidate.n1, best.candidate.n2, best.candidate.n3};

        set.globalBiasScore = Math.max(set.globalBiasScore, best.evidenceScore);
        set.globalTrendStrength = Math.max(set.globalTrendStrength, best.rankScore);

        if (best.evidence != null) {
            set.profileKey = best.evidence.evidenceKey;
            set.summaryText = append(set.summaryText, best.evidence.summaryText);
        } else {
            set.summaryText = append(set.summaryText, best.summaryText);
        }

        return set;
    }

    public String buildSummary(List<RankedCandidate> ranked) {
        if (ranked == null || ranked.size() == 0) {
            return "证据排序：暂无候选";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(ranked.size(), 3);

        for (int i = 0; i < limit; i++) {
            RankedCandidate r = ranked.get(i);

            if (r == null || r.candidate == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("#");
            sb.append(r.rank);
            sb.append(" P");
            sb.append(r.candidate.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(r.candidate.n1, r.candidate.n2, r.candidate.n3));
            sb.append(" 排名分=");
            sb.append(UiFormatter.score(r.rankScore));
            sb.append(" 证据=");
            sb.append(UiFormatter.score(r.evidenceScore));
            sb.append(" 风险=");
            sb.append(UiFormatter.score(r.riskScore));
        }

        return sb.length() == 0 ? "证据排序：暂无有效候选" : sb.toString();
    }

    public String explainBest(List<Models.Draw> history, List<Models.ComboCandidate> candidates) {
        RankedCandidate best = pickBestRanked(history, candidates);

        if (best == null) {
            return "证据排序：没有可排序候选";
        }

        return best.summaryText;
    }

    private RankedCandidate buildRanked(
            Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        RankedCandidate r = new RankedCandidate();

        if (c == null) {
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "证据排序：候选为空";
            return r;
        }

        r.candidate = c.copy();
        r.evidence = evidence;

        r.position = c.position;
        r.n1 = c.n1;
        r.n2 = c.n2;
        r.n3 = c.n3;
        r.k3Mask = c.k3Mask;

        r.finalP = c.finalP;
        r.dynamicGate = c.dynamicGate;
        r.edge = c.edge;
        r.rawP = c.rawP;
        r.calibratedP = c.calibratedP;
        r.calibrationError = c.calibrationError;

        r.evidenceScore = evidence == null ? c.evidenceScore * 100.0d : evidence.evidenceScore;
        r.riskScore = evidence == null ? c.evidenceRisk * 100.0d : evidence.riskScore;
        r.confidence = evidence == null ? 0.0d : evidence.confidence;

        r.probabilityScore = probabilityScore(c);
        r.gateScore = gateScore(c);
        r.sampleScore = sampleScore(c);
        r.shapeScore = shapeScore(c);
        r.deepScore = deepScore(c);
        r.aPlusScore = c.aPlusScore * 100.0d;
        r.aiScore = aiScore(c);

        r.penaltyScore = penaltyScore(c, evidence);
        r.bonusScore = bonusScore(c, evidence);

        r.rankScore = finalRankScore(r, c, evidence);
        r.blockCode = blockByRank(r, c, evidence);
        r.usable = r.blockCode.length() == 0 ? 1 : 0;
        r.strong = r.usable == 1 && r.rankScore >= STRONG_RANK_SCORE ? 1 : 0;
        r.aPlus = r.usable == 1 && r.rankScore >= A_PLUS_RANK_SCORE ? 1 : 0;
        r.summaryText = buildRankSummary(r, c, evidence);

        if (r.candidate != null) {
            r.candidate.rank = r.rank;
            r.candidate.evidenceScore = AppConfig.clampScore01(r.evidenceScore / 100.0d);
            r.candidate.evidenceRisk = AppConfig.clampScore01(r.riskScore / 100.0d);

            if (r.blockCode.length() > 0 && r.candidate.finalP < 0.430d) {
                r.candidate.blockCode = pickBlockCode(r.candidate.blockCode, r.blockCode);
            }

            r.candidate.reasonSummary = append(r.candidate.reasonSummary, r.summaryText);
        }

        return r;
    }

    private double probabilityScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.finalP * 100.0d * 0.52d;
        score += c.rawP * 100.0d * 0.16d;
        score += c.calibratedP * 100.0d * 0.12d;
        score += Math.max(0.0d, c.edge) * 100.0d * 0.20d;

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            score += 5.0d;
        }

        if (c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            score += 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double gateScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 50.0d;

        double edge = c.finalP - c.dynamicGate;
        score += edge * 220.0d;

        if (edge >= 0.020d) {
            score += 5.0d;
        }

        if (edge < 0.0d) {
            score -= 12.0d;
        }

        if (c.dynamicGate > 0.430d && c.finalP < c.dynamicGate) {
            score -= 5.0d;
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
                        * 32.0d;

        if (c.similarHitRate > 0.0d) {
            score += c.similarHitRate * 100.0d * 0.14d;
        }

        if (c.comboHitRate > 0.0d) {
            score += c.comboHitRate * 100.0d * 0.12d;
        }

        if (c.effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            score -= 8.0d;
        }

        if (c.comboSample < AppConfig.COMBO_SAMPLE_MIN) {
            score -= 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double shapeScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.trendLife * 0.24d;
        score += Math.max(0.0d, 100.0d - c.driftRisk) * 0.22d;
        score += Math.max(0.0d, 100.0d - c.breakDepth) * 0.18d;
        score += c.recoveryScore * 0.14d;
        score += c.championGap * 100.0d * 0.10d;
        score += c.comboGap * 100.0d * 0.12d;

        if (AppConfig.MODE_CONFIRMED.equals(c.stateMode)) {
            score += 4.0d;
        } else if (AppConfig.MODE_ACCELERATING.equals(c.stateMode)) {
            score += 5.0d;
        } else if (AppConfig.MODE_RECOVERING.equals(c.stateMode)) {
            score += 2.0d;
        } else if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            score -= 9.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double deepScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.deepScore * 100.0d * 0.22d;
        score += c.deepConfidence * 100.0d * 0.08d;
        score += c.deepPositionScore * 100.0d * 0.10d;
        score += c.deepNumberScore * 100.0d * 0.10d;
        score += c.deepCycleScore * 100.0d * 0.09d;
        score += c.deepTransitionScore * 100.0d * 0.09d;
        score += c.deepStateScore * 100.0d * 0.08d;
        score += c.biasScore * 100.0d * 0.09d;
        score += c.profileScore * 100.0d * 0.08d;
        score += c.evidenceScore * 100.0d * 0.07d;

        return AppConfig.clampScore100(score);
    }

    private double aiScore(Models.ComboCandidate c) {
        if (c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += c.aiJudgeScore * 100.0d * 0.48d;
        score += c.aiJudgeConfidence * 100.0d * 0.34d;
        score += Math.max(0.0d, 1.0d - c.evidenceRisk) * 100.0d * 0.10d;
        score += c.aPlusScore * 100.0d * 0.08d;

        if (score <= 0.0d) {
            score = 50.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double penaltyScore(Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        if (c == null) {
            return 100.0d;
        }

        double penalty = 0.0d;

        if (evidence != null) {
            penalty += evidence.riskScore * 0.34d;
        } else {
            penalty += c.evidenceRisk * 100.0d * 0.34d;
        }

        penalty += c.auditPenalty * 100.0d * 0.16d;
        penalty += c.driftRisk * 0.12d;
        penalty += c.breakDepth * 0.10d;
        penalty += c.hotReverseRisk * 0.06d;
        penalty += c.falseStrongRisk * 0.06d;

        if (c.calibrationError >= 0.080d) {
            penalty += 7.0d;
        }

        if (c.finalP < AppConfig.MIN_REAL_BET_PROBABILITY) {
            penalty += 6.0d;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            penalty += 9.0d;
        }

        if (c.blockCode != null
                && c.blockCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(c.blockCode)) {
            penalty += 8.0d;
        }

        return AppConfig.clampScore100(penalty);
    }

    private double bonusScore(Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        if (c == null) {
            return 0.0d;
        }

        double bonus = 0.0d;

        if (evidence != null
                && evidence.evidenceScore >= STRONG_RANK_SCORE
                && evidence.riskScore <= 50.0d) {
            bonus += 5.0d;
        }

        if (evidence != null && isEvidenceResonance(evidence)) {
            bonus += 5.0d;
        }

        if (c.finalP >= c.dynamicGate && c.edge >= 0.0d) {
            bonus += 4.0d;
        }

        if (c.trendLife >= 70.0d
                && c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                && c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
            bonus += 4.0d;
        }

        if (c.aPlusScore >= 0.620d) {
            bonus += 3.0d;
        }

        if (c.aiJudgeScore >= 0.620d && c.aiJudgeConfidence >= 0.580d) {
            bonus += 3.0d;
        }

        return AppConfig.clampScore100(bonus);
    }

    private double finalRankScore(
            RankedCandidate r, Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        if (r == null || c == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.probabilityScore * 0.18d;
        score += r.gateScore * 0.12d;
        score += r.evidenceScore * 0.20d;
        score += r.confidence * 0.08d;
        score += r.sampleScore * 0.10d;
        score += r.shapeScore * 0.12d;
        score += r.deepScore * 0.10d;
        score += r.aiScore * 0.05d;
        score += r.aPlusScore * 0.05d;

        score += r.bonusScore;
        score -= r.penaltyScore * 0.16d;

        if (evidence != null && evidence.riskScore >= HARD_RISK_SCORE) {
            score -= 8.0d;
        }

        if (c.finalP < c.dynamicGate && c.edge < 0.0d) {
            score -= 7.0d;
        }

        if (!c.isValidK3()) {
            score = 0.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private boolean isEvidenceResonance(Models.CandidateEvidence e) {
        if (e == null) {
            return false;
        }

        int count = 0;

        if (e.positionEvidence >= STRONG_RANK_SCORE) count++;
        if (e.numberEvidence >= STRONG_RANK_SCORE) count++;
        if (e.cycleEvidence >= STRONG_RANK_SCORE) count++;
        if (e.transitionEvidence >= STRONG_RANK_SCORE) count++;
        if (e.patternEvidence >= STRONG_RANK_SCORE) count++;
        if (e.comboEvidence >= STRONG_RANK_SCORE) count++;

        return count >= 3 && e.riskScore <= 56.0d;
    }

    private String blockByRank(
            RankedCandidate r, Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        if (r == null || c == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (!c.isValidK3()) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (evidence != null && evidence.riskScore >= HARD_RISK_SCORE) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_HARD_BLOCK) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            return AppConfig.BLOCK_TREND_WEAK;
        }

        if (c.calibrationError >= 0.120d && c.finalP < 0.430d) {
            return AppConfig.BLOCK_LOW_PROBABILITY;
        }

        if (r.rankScore < 34.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        return "";
    }

    private Models.CandidateEvidence findEvidence(
            List<Models.CandidateEvidence> list, Models.ComboCandidate candidate) {
        if (list == null || candidate == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateEvidence e = list.get(i);
            if (e == null) {
                continue;
            }

            if (e.position == candidate.position && e.k3Mask == candidate.k3Mask) {
                return e;
            }
        }

        for (int i = 0; i < list.size(); i++) {
            Models.CandidateEvidence e = list.get(i);
            if (e == null) {
                continue;
            }

            if (e.position == candidate.position
                    && e.n1 == candidate.n1
                    && e.n2 == candidate.n2
                    && e.n3 == candidate.n3) {
                return e;
            }
        }

        return null;
    }

    private void sortAndRank(List<RankedCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<RankedCandidate>() {
                    @Override
                    public int compare(RankedCandidate a, RankedCandidate b) {
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
            RankedCandidate r = list.get(i);
            if (r != null) {
                r.rank = i + 1;

                if (r.candidate != null) {
                    r.candidate.rank = r.rank;
                    r.candidate.reasonSummary =
                            append(
                                    r.candidate.reasonSummary,
                                    "证据排序：第"
                                            + r.rank
                                            + "名，排序分="
                                            + UiFormatter.score(r.rankScore)
                                            + "，证据="
                                            + UiFormatter.score(r.evidenceScore)
                                            + "，风险="
                                            + UiFormatter.score(r.riskScore));
                }
            }
        }
    }

    private double scoreForCandidateSort(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.sortScore();
        score += c.evidenceScore * 100.0d * 0.10d;
        score -= c.evidenceRisk * 100.0d * 0.08d;

        return score;
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
                        double sa = scoreForCandidateSort(a);
                        double sb = scoreForCandidateSort(b);

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

    private String pickBlockCode(String oldCode, String newCode) {
        if (oldCode != null
                && oldCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(oldCode)
                && !AppConfig.BLOCK_COMBO_WEAK.equals(oldCode)) {
            return oldCode;
        }

        return newCode == null || newCode.length() == 0 ? AppConfig.BLOCK_COMBO_WEAK : newCode;
    }

    private String buildRankSummary(
            RankedCandidate r, Models.ComboCandidate c, Models.CandidateEvidence evidence) {
        if (r == null || c == null) {
            return "证据排序：候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("证据排序P");
        sb.append(c.position);
        sb.append(" ");
        sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));

        sb.append("：排序分=");
        sb.append(UiFormatter.score(r.rankScore));

        sb.append("，证据=");
        sb.append(UiFormatter.score(r.evidenceScore));

        sb.append("，风险=");
        sb.append(UiFormatter.score(r.riskScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(r.confidence));

        sb.append("，FinalP=");
        sb.append(UiFormatter.probability(c.finalP));

        sb.append("，Gate=");
        sb.append(UiFormatter.probability(c.dynamicGate));

        sb.append("，Edge=");
        sb.append(UiFormatter.signedProbability(c.edge));

        sb.append("，样本=");
        sb.append(c.effectiveSample);
        sb.append("/");
        sb.append(c.comboSample);

        sb.append("，形态=");
        sb.append(UiFormatter.modeText(c.stateMode));

        if (isEvidenceResonance(evidence)) {
            sb.append("，多证据共振");
        }

        if (r.aPlus == 1) {
            sb.append("，A+排序候选");
        } else if (r.strong == 1) {
            sb.append("，强排序候选");
        }

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，排序风险=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        }

        return sb.toString();
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
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

    public static final class RankedCandidate {
        public int rank;

        public int position;
        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public Models.ComboCandidate candidate;
        public Models.CandidateEvidence evidence;

        public double rawP;
        public double calibratedP;
        public double finalP;
        public double dynamicGate;
        public double edge;
        public double calibrationError;

        public double probabilityScore;
        public double gateScore;
        public double evidenceScore;
        public double riskScore;
        public double confidence;
        public double sampleScore;
        public double shapeScore;
        public double deepScore;
        public double aiScore;
        public double aPlusScore;
        public double penaltyScore;
        public double bonusScore;
        public double rankScore;

        public int usable;
        public int strong;
        public int aPlus;

        public String blockCode;
        public String summaryText;

        public RankedCandidate() {
            rank = 0;

            position = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            candidate = null;
            evidence = null;

            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            dynamicGate = 0.0d;
            edge = 0.0d;
            calibrationError = 0.0d;

            probabilityScore = 0.0d;
            gateScore = 0.0d;
            evidenceScore = 0.0d;
            riskScore = 0.0d;
            confidence = 0.0d;
            sampleScore = 0.0d;
            shapeScore = 0.0d;
            deepScore = 0.0d;
            aiScore = 0.0d;
            aPlusScore = 0.0d;
            penaltyScore = 0.0d;
            bonusScore = 0.0d;
            rankScore = 0.0d;

            usable = 0;
            strong = 0;
            aPlus = 0;

            blockCode = "";
            summaryText = "";
        }

        public boolean isUsableForFinalGate() {
            return usable == 1
                    && candidate != null
                    && candidate.isValidK3()
                    && rankScore >= 34.0d
                    && riskScore < HARD_RISK_SCORE;
        }

        public boolean isStrongForFinalGate() {
            return isUsableForFinalGate()
                    && rankScore >= STRONG_RANK_SCORE
                    && evidenceScore >= STRONG_RANK_SCORE
                    && riskScore <= 56.0d;
        }

        public double sortScore() {
            double s = rankScore;
            s += evidenceScore * 0.05d;
            s += confidence * 0.04d;
            s -= riskScore * 0.06d;

            if (usable == 0) {
                s -= 10.0d;
            }

            if (strong == 1) {
                s += 3.0d;
            }

            if (aPlus == 1) {
                s += 4.0d;
            }

            return s;
        }

        public RankedCandidate copy() {
            RankedCandidate r = new RankedCandidate();

            r.rank = rank;

            r.position = position;
            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;

            r.candidate = candidate == null ? null : candidate.copy();
            r.evidence = evidence;

            r.rawP = rawP;
            r.calibratedP = calibratedP;
            r.finalP = finalP;
            r.dynamicGate = dynamicGate;
            r.edge = edge;
            r.calibrationError = calibrationError;

            r.probabilityScore = probabilityScore;
            r.gateScore = gateScore;
            r.evidenceScore = evidenceScore;
            r.riskScore = riskScore;
            r.confidence = confidence;
            r.sampleScore = sampleScore;
            r.shapeScore = shapeScore;
            r.deepScore = deepScore;
            r.aiScore = aiScore;
            r.aPlusScore = aPlusScore;
            r.penaltyScore = penaltyScore;
            r.bonusScore = bonusScore;
            r.rankScore = rankScore;

            r.usable = usable;
            r.strong = strong;
            r.aPlus = aPlus;

            r.blockCode = blockCode;
            r.summaryText = summaryText;

            return r;
        }
    }
}
