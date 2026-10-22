package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class BiasEvidenceEngine {

    private static final int WINDOW_SHORT = 32;
    private static final int WINDOW_MID = 160;
    private static final int WINDOW_LONG = 500;

    private static final double BIAS_USABLE = 52.0d;
    private static final double BIAS_STRONG = 62.0d;
    private static final double BIAS_A_PLUS = 72.0d;

    private final PositionStrengthEngine positionStrengthEngine;
    private final TransitionRelationEngine transitionRelationEngine;
    private final CycleRhythmEngine cycleRhythmEngine;
    private final DistributionDriftEngine distributionDriftEngine;

    public BiasEvidenceEngine() {
        positionStrengthEngine = new PositionStrengthEngine();
        transitionRelationEngine = new TransitionRelationEngine();
        cycleRhythmEngine = new CycleRhythmEngine();
        distributionDriftEngine = new DistributionDriftEngine();
    }

    public Models.BiasEvidenceResult analyze(List<Models.Draw> history) {
        ArrayList<BiasSignal> signals = analyzeAllPositions(history);
        return buildResultFromSignals(history, signals);
    }

    public Models.BiasEvidenceResult analyzeCandidate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        Models.BiasEvidenceResult result = new Models.BiasEvidenceResult();
        result.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        result.sampleCount = clean.size();
        result.recentSample = Math.min(clean.size(), WINDOW_SHORT);

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            result.issue = latest.issue == null ? "" : latest.issue;
        }

        if (candidate == null || !candidate.isValidK3()) {
            result.status = AppConfig.BIAS_STATUS_NOISY;
            result.summaryText = "偏差证据：候选为空或不是固定K3";
            return result;
        }

        BiasSignal signal = analyzeCandidateSignal(clean, candidate);

        result.evidenceId = signal.evidenceKey;
        result.biasScore = signal.biasScore;
        result.positionBiasScore = signal.positionBiasScore;
        result.numberBiasScore = signal.numberBiasScore;
        result.transitionBiasScore = signal.transitionBiasScore;
        result.cycleBiasScore = signal.cycleBiasScore;
        result.comboBiasScore = signal.comboBiasScore;
        result.stateBiasScore = signal.stateBiasScore;
        result.confidence = signal.confidence;
        result.status = signal.status;
        result.summaryText = signal.summaryText;

        return result;
    }

    public ArrayList<BiasSignal> analyzeAllPositions(List<Models.Draw> history) {
        ArrayList<BiasSignal> out = new ArrayList<BiasSignal>();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        ArrayList<PositionStrengthEngine.PositionStrengthResult> positions =
                positionStrengthEngine.analyzeAll(clean);

        ArrayList<TransitionRelationEngine.TransitionRelationResult> transitions =
                transitionRelationEngine.analyzeAll(clean);

        ArrayList<CycleRhythmEngine.CycleRhythmResult> cycles = cycleRhythmEngine.analyzeAll(clean);

        ArrayList<DistributionDriftEngine.DistributionDriftResult> drifts =
                distributionDriftEngine.analyzeAll(clean);

        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            BiasSignal s = analyzePositionSignal(clean, p, positions, transitions, cycles, drifts);
            out.add(s);
        }

        sortAndRank(out);
        return out;
    }

    public BiasSignal analyzePosition(List<Models.Draw> history, int position) {
        ArrayList<Models.Draw> clean = cleanHistory(history);

        ArrayList<PositionStrengthEngine.PositionStrengthResult> positions =
                positionStrengthEngine.analyzeAll(clean);

        ArrayList<TransitionRelationEngine.TransitionRelationResult> transitions =
                transitionRelationEngine.analyzeAll(clean);

        ArrayList<CycleRhythmEngine.CycleRhythmResult> cycles = cycleRhythmEngine.analyzeAll(clean);

        ArrayList<DistributionDriftEngine.DistributionDriftResult> drifts =
                distributionDriftEngine.analyzeAll(clean);

        return analyzePositionSignal(clean, position, positions, transitions, cycles, drifts);
    }

    public BiasSignal analyzeCandidateSignal(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        BiasSignal s = new BiasSignal();
        s.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        s.sampleCount = clean.size();

        if (candidate == null || !candidate.isValidK3()) {
            s.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            s.status = AppConfig.BIAS_STATUS_NOISY;
            s.summaryText = "偏差证据：候选无效";
            return s;
        }

        s.position = candidate.position;
        s.n1 = candidate.n1;
        s.n2 = candidate.n2;
        s.n3 = candidate.n3;
        s.k3Mask = candidate.k3Mask;

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            s.baseIssue = latest.issue == null ? "" : latest.issue;
        }

        BiasSignal base = analyzePosition(clean, candidate.position);

        s.positionBiasScore = base.positionBiasScore;
        s.transitionBiasScore = base.transitionBiasScore;
        s.cycleBiasScore = base.cycleBiasScore;
        s.stateBiasScore = base.stateBiasScore;
        s.driftRisk = base.driftRisk;
        s.noiseRisk = base.noiseRisk;

        s.numberBiasScore = candidateNumberBias(clean, candidate.position, candidate.k3Mask);
        s.comboBiasScore = candidateComboBias(clean, candidate.position, candidate.k3Mask);

        s.coverageScore = candidateCoverageAgainstBase(candidate, base);
        s.confidence = buildCandidateConfidence(s, base);
        s.biasScore = finalCandidateBiasScore(s);
        s.riskScore = finalCandidateRiskScore(s);
        s.status = statusBySignal(s);
        s.blockCode = blockBySignal(s);
        s.evidenceKey = buildEvidenceKey(s);
        s.summaryText = buildCandidateSummary(s, candidate);

        return s;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<Models.Draw> clean = cleanHistory(history);
        ArrayList<BiasSignal> positionSignals = analyzeAllPositions(clean);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            BiasSignal base = findForPosition(positionSignals, copy.position);
            BiasSignal candidateSignal = analyzeCandidateSignalFromBase(clean, copy, base);
            applyToCandidate(copy, candidateSignal);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, BiasSignal signal) {
        if (candidate == null) {
            return;
        }

        if (signal == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "偏差证据：暂无");
            return;
        }

        candidate.biasScore = AppConfig.clampScore01(signal.biasScore / 100.0d);
        candidate.biasConfidence = AppConfig.clampScore01(signal.confidence / 100.0d);

        candidate.evidenceScore = blend01(candidate.evidenceScore, candidate.biasScore, 0.18d);
        candidate.evidenceRisk = blend01(candidate.evidenceRisk, signal.riskScore / 100.0d, 0.14d);

        if (signal.biasScore >= BIAS_STRONG
                && signal.confidence >= 58.0d
                && signal.riskScore <= 48.0d
                && candidate.finalP >= 0.360d) {
            candidate.rawP = AppConfig.clampProbability(candidate.rawP + 0.004d);
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + 0.004d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        if (signal.biasScore >= BIAS_A_PLUS
                && signal.confidence >= 66.0d
                && signal.riskScore <= 42.0d) {
            candidate.aPlusScore = blend01(candidate.aPlusScore, signal.biasScore / 100.0d, 0.16d);
        }

        if (signal.riskScore >= 76.0d && candidate.finalP < 0.430d) {
            candidate.blockCode =
                    signal.blockCode == null || signal.blockCode.length() == 0
                            ? AppConfig.BLOCK_COMBO_WEAK
                            : signal.blockCode;
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "偏差证据P"
                                + signal.position
                                + "：总分="
                                + UiFormatter.score(signal.biasScore)
                                + "，位置="
                                + UiFormatter.score(signal.positionBiasScore)
                                + "，号码="
                                + UiFormatter.score(signal.numberBiasScore)
                                + "，组合="
                                + UiFormatter.score(signal.comboBiasScore)
                                + "，周期="
                                + UiFormatter.score(signal.cycleBiasScore)
                                + "，转移="
                                + UiFormatter.score(signal.transitionBiasScore)
                                + "，置信="
                                + UiFormatter.score(signal.confidence)
                                + "，风险="
                                + UiFormatter.score(signal.riskScore));
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, List<Models.Draw> history) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        ArrayList<BiasSignal> signals = analyzeAllPositions(history);
        BiasSignal best = best(signals);

        if (best == null) {
            return set;
        }

        set.globalBiasScore = best.biasScore;
        set.globalTrendStrength = Math.max(set.globalTrendStrength, best.biasScore);
        set.profileKey = best.evidenceKey;
        set.summaryText = append(set.summaryText, best.summaryText);

        if (best.biasScore >= BIAS_STRONG && best.isUsable()) {
            set.bestPosition = best.position;
            set.bestNumbers = new int[] {best.n1, best.n2, best.n3};
        }

        return set;
    }

    public BiasSignal findForPosition(List<BiasSignal> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            BiasSignal s = list.get(i);
            if (s != null && s.position == position) {
                return s;
            }
        }

        return null;
    }

    public BiasSignal best(List<BiasSignal> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        BiasSignal best = null;

        for (int i = 0; i < list.size(); i++) {
            BiasSignal s = list.get(i);
            if (s == null) {
                continue;
            }

            if (best == null || s.sortScore() > best.sortScore()) {
                best = s;
            }
        }

        return best;
    }

    public String buildSummary(List<BiasSignal> list) {
        if (list == null || list.size() == 0) {
            return "偏差证据：暂无";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            BiasSignal s = list.get(i);
            if (s == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(s.position);
            sb.append("=");
            sb.append(UiFormatter.score(s.biasScore));
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(s.n1, s.n2, s.n3));
            sb.append(" 置信");
            sb.append(UiFormatter.score(s.confidence));

            if (s.blockCode != null && s.blockCode.length() > 0) {
                sb.append(" ");
                sb.append(UiFormatter.blockReasonText(s.blockCode));
            }
        }

        return sb.length() == 0 ? "偏差证据：暂无有效位置" : sb.toString();
    }

    private BiasSignal analyzePositionSignal(
            ArrayList<Models.Draw> clean,
            int position,
            ArrayList<PositionStrengthEngine.PositionStrengthResult> positions,
            ArrayList<TransitionRelationEngine.TransitionRelationResult> transitions,
            ArrayList<CycleRhythmEngine.CycleRhythmResult> cycles,
            ArrayList<DistributionDriftEngine.DistributionDriftResult> drifts) {
        BiasSignal s = new BiasSignal();
        s.position = position;
        s.createdAt = System.currentTimeMillis();
        s.sampleCount = clean == null ? 0 : clean.size();

        if (clean != null && clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            s.baseIssue = latest.issue == null ? "" : latest.issue;
        }

        if (!AppConfig.isValidPosition(position)) {
            s.blockCode = AppConfig.BLOCK_POSITION_WEAK;
            s.status = AppConfig.BIAS_STATUS_NOISY;
            s.summaryText = "偏差证据：位置非法";
            return s;
        }

        if (clean == null || clean.size() < WINDOW_SHORT) {
            s.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            s.status = AppConfig.BIAS_STATUS_NOISY;
            s.summaryText = "偏差证据：历史少于32期";
            return s;
        }

        PositionStrengthEngine.PositionStrengthResult ps =
                positionStrengthEngine.findForPosition(positions, position);

        TransitionRelationEngine.TransitionRelationResult tr =
                transitionRelationEngine.findForPosition(transitions, position);

        CycleRhythmEngine.CycleRhythmResult cy =
                cycleRhythmEngine.findForPosition(cycles, position);

        DistributionDriftEngine.DistributionDriftResult df =
                distributionDriftEngine.findForPosition(drifts, position);

        int[] bestNumbers = chooseBestNumbers(clean, position, ps, tr, cy);

        s.n1 = bestNumbers[0];
        s.n2 = bestNumbers[1];
        s.n3 = bestNumbers[2];
        s.k3Mask = Models.buildK3Mask(s.n1, s.n2, s.n3);

        s.positionBiasScore = ps == null ? 0.0d : ps.score;
        s.transitionBiasScore = tr == null ? 0.0d : tr.relationScore;
        s.cycleBiasScore = cy == null ? 0.0d : cy.rhythmScore;
        s.stateBiasScore = stateBiasScore(ps, tr, cy, df);
        s.numberBiasScore = candidateNumberBias(clean, position, s.k3Mask);
        s.comboBiasScore = candidateComboBias(clean, position, s.k3Mask);

        s.driftRisk = df == null ? 50.0d : df.driftRisk;
        s.noiseRisk = noiseRisk(s, df);
        s.coverageScore = 100.0d;
        s.confidence = buildPositionConfidence(s);
        s.biasScore = finalPositionBiasScore(s);
        s.riskScore = finalPositionRiskScore(s);
        s.status = statusBySignal(s);
        s.blockCode = blockBySignal(s);
        s.evidenceKey = buildEvidenceKey(s);
        s.summaryText = buildSummary(s);

        return s;
    }

    private BiasSignal analyzeCandidateSignalFromBase(
            ArrayList<Models.Draw> clean, Models.ComboCandidate candidate, BiasSignal base) {
        if (base == null) {
            return analyzeCandidateSignal(clean, candidate);
        }

        BiasSignal s = base.copy();

        s.n1 = candidate.n1;
        s.n2 = candidate.n2;
        s.n3 = candidate.n3;
        s.k3Mask = candidate.k3Mask;

        s.numberBiasScore = candidateNumberBias(clean, candidate.position, candidate.k3Mask);
        s.comboBiasScore = candidateComboBias(clean, candidate.position, candidate.k3Mask);
        s.coverageScore = candidateCoverageAgainstBase(candidate, base);
        s.confidence = buildCandidateConfidence(s, base);
        s.biasScore = finalCandidateBiasScore(s);
        s.riskScore = finalCandidateRiskScore(s);
        s.status = statusBySignal(s);
        s.blockCode = blockBySignal(s);
        s.evidenceKey = buildEvidenceKey(s);
        s.summaryText = buildCandidateSummary(s, candidate);

        return s;
    }

    private Models.BiasEvidenceResult buildResultFromSignals(
            List<Models.Draw> history, ArrayList<BiasSignal> signals) {
        Models.BiasEvidenceResult result = new Models.BiasEvidenceResult();
        result.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        result.sampleCount = clean.size();
        result.recentSample = Math.min(clean.size(), WINDOW_SHORT);

        if (clean.size() > 0) {
            Models.Draw latest = clean.get(clean.size() - 1);
            result.issue = latest.issue == null ? "" : latest.issue;
        }

        BiasSignal best = best(signals);

        if (best == null) {
            result.status = AppConfig.BIAS_STATUS_NOISY;
            result.summaryText = "偏差证据：暂无有效结果";
            return result;
        }

        result.evidenceId = best.evidenceKey;
        result.biasScore = best.biasScore;
        result.positionBiasScore = best.positionBiasScore;
        result.numberBiasScore = best.numberBiasScore;
        result.transitionBiasScore = best.transitionBiasScore;
        result.cycleBiasScore = best.cycleBiasScore;
        result.comboBiasScore = best.comboBiasScore;
        result.stateBiasScore = best.stateBiasScore;
        result.confidence = best.confidence;
        result.status = best.status;
        result.summaryText = buildSummary(signals);

        return result;
    }

    private int[] chooseBestNumbers(
            ArrayList<Models.Draw> history,
            int position,
            PositionStrengthEngine.PositionStrengthResult ps,
            TransitionRelationEngine.TransitionRelationResult tr,
            CycleRhythmEngine.CycleRhythmResult cy) {
        double[] score = new double[AppConfig.NUMBER_COUNT + 1];

        double[] f32 = frequency(history, position, WINDOW_SHORT);
        double[] f160 = frequency(history, position, WINDOW_MID);
        double[] f500 = frequency(history, position, WINDOW_LONG);

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            score[n] += f32[n] * 0.40d;
            score[n] += f160[n] * 0.24d;
            score[n] += f500[n] * 0.12d;
        }

        addTopNumbersScore(score, ps == null ? 0 : ps.k3Mask, 0.080d);
        addTopNumbersScore(score, tr == null ? 0 : tr.k3Mask, 0.090d);
        addTopNumbersScore(score, cy == null ? 0 : cy.k3Mask, 0.090d);

        if (ps != null && ps.n1 > 0) score[ps.n1] += 0.020d;
        if (tr != null && tr.n1 > 0) score[tr.n1] += 0.020d;
        if (cy != null && cy.n1 > 0) score[cy.n1] += 0.020d;

        int[] nums = top3(score);
        sortNumbers(nums);
        return nums;
    }

    private void addTopNumbersScore(double[] score, int mask, double add) {
        if (score == null || !Models.isValidK3Mask(mask)) {
            return;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(mask, n)) {
                score[n] += add;
            }
        }
    }

    private double candidateNumberBias(ArrayList<Models.Draw> history, int position, int mask) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        double[] f32 = frequency(history, position, WINDOW_SHORT);
        double[] f160 = frequency(history, position, WINDOW_MID);
        double[] f500 = frequency(history, position, WINDOW_LONG);

        double shortMass = maskMass(f32, mask);
        double midMass = maskMass(f160, mask);
        double longMass = maskMass(f500, mask);

        double base = ((double) AppConfig.FIXED_K) / ((double) AppConfig.NUMBER_COUNT);

        double score = 50.0d;
        score += (shortMass - base) * 105.0d;
        score += (midMass - base) * 62.0d;
        score += (longMass - base) * 28.0d;

        if (shortMass >= midMass && midMass >= longMass) {
            score += 4.0d;
        }

        if (shortMass < 0.240d) {
            score -= 7.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double candidateComboBias(ArrayList<Models.Draw> history, int position, int mask) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        int hit32 = comboHit(history, position, mask, WINDOW_SHORT);
        int hit160 = comboHit(history, position, mask, WINDOW_MID);
        int hit500 = comboHit(history, position, mask, WINDOW_LONG);

        int total32 = Math.min(history.size(), WINDOW_SHORT);
        int total160 = Math.min(history.size(), WINDOW_MID);
        int total500 = Math.min(history.size(), WINDOW_LONG);

        double p32 = total32 > 0 ? ((double) hit32) / ((double) total32) : 0.0d;
        double p160 = total160 > 0 ? ((double) hit160) / ((double) total160) : 0.0d;
        double p500 = total500 > 0 ? ((double) hit500) / ((double) total500) : 0.0d;

        double base = ((double) AppConfig.FIXED_K) / ((double) AppConfig.NUMBER_COUNT);

        double score = 50.0d;
        score += (p32 - base) * 95.0d;
        score += (p160 - base) * 58.0d;
        score += (p500 - base) * 22.0d;

        if (p32 >= p160 && p160 >= p500) {
            score += 4.0d;
        }

        if (p32 < 0.240d) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double stateBiasScore(
            PositionStrengthEngine.PositionStrengthResult ps,
            TransitionRelationEngine.TransitionRelationResult tr,
            CycleRhythmEngine.CycleRhythmResult cy,
            DistributionDriftEngine.DistributionDriftResult df) {
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

        if (df != null) {
            score += Math.max(0.0d, 100.0d - df.driftRisk);
            count++;
        }

        if (count <= 0) {
            return 0.0d;
        }

        return AppConfig.clampScore100(score / ((double) count));
    }

    private double noiseRisk(BiasSignal s, DistributionDriftEngine.DistributionDriftResult df) {
        if (s == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += s.driftRisk * 0.38d;

        if (df != null) {
            risk += df.strongNumberShift * 0.16d;
            risk += df.entropyRise * 0.14d;
            risk += df.shortLongConflict * 0.12d;
        }

        if (s.positionBiasScore < 40.0d) {
            risk += 8.0d;
        }

        if (s.numberBiasScore < 40.0d) {
            risk += 8.0d;
        }

        if (s.comboBiasScore < 40.0d) {
            risk += 8.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double buildPositionConfidence(BiasSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) s.sampleCount) / 500.0d) * 30.0d;
        score += s.positionBiasScore * 0.16d;
        score += s.numberBiasScore * 0.14d;
        score += s.comboBiasScore * 0.14d;
        score += s.transitionBiasScore * 0.12d;
        score += s.cycleBiasScore * 0.12d;
        score += Math.max(0.0d, 100.0d - s.noiseRisk) * 0.22d;

        return AppConfig.clampScore100(score);
    }

    private double buildCandidateConfidence(BiasSignal s, BiasSignal base) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) s.sampleCount) / 500.0d) * 26.0d;
        score += s.coverageScore * 0.16d;
        score += s.numberBiasScore * 0.16d;
        score += s.comboBiasScore * 0.16d;
        score += s.positionBiasScore * 0.12d;
        score += s.transitionBiasScore * 0.10d;
        score += s.cycleBiasScore * 0.10d;
        score += Math.max(0.0d, 100.0d - s.noiseRisk) * 0.14d;

        if (base != null && base.biasScore >= BIAS_STRONG) {
            score += 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalPositionBiasScore(BiasSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += s.positionBiasScore * 0.22d;
        score += s.numberBiasScore * 0.20d;
        score += s.comboBiasScore * 0.18d;
        score += s.transitionBiasScore * 0.13d;
        score += s.cycleBiasScore * 0.13d;
        score += s.stateBiasScore * 0.10d;
        score += s.confidence * 0.04d;

        score -= s.noiseRisk * 0.14d;

        if (s.positionBiasScore >= BIAS_STRONG
                && s.numberBiasScore >= BIAS_STRONG
                && s.comboBiasScore >= 56.0d) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalCandidateBiasScore(BiasSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += s.positionBiasScore * 0.16d;
        score += s.numberBiasScore * 0.24d;
        score += s.comboBiasScore * 0.24d;
        score += s.transitionBiasScore * 0.11d;
        score += s.cycleBiasScore * 0.11d;
        score += s.stateBiasScore * 0.06d;
        score += s.coverageScore * 0.05d;
        score += s.confidence * 0.03d;

        score -= s.noiseRisk * 0.13d;

        if (s.coverageScore >= 66.0d && s.numberBiasScore >= BIAS_STRONG) {
            score += 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double finalPositionRiskScore(BiasSignal s) {
        if (s == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += s.noiseRisk * 0.56d;
        risk += s.driftRisk * 0.24d;

        if (s.biasScore < 42.0d) {
            risk += 10.0d;
        }

        if (s.confidence < 42.0d) {
            risk += 8.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double finalCandidateRiskScore(BiasSignal s) {
        if (s == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += s.noiseRisk * 0.48d;
        risk += s.driftRisk * 0.22d;

        if (s.coverageScore < 34.0d) {
            risk += 10.0d;
        }

        if (s.numberBiasScore < 42.0d) {
            risk += 8.0d;
        }

        if (s.comboBiasScore < 42.0d) {
            risk += 8.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private String statusBySignal(BiasSignal s) {
        if (s == null) {
            return AppConfig.BIAS_STATUS_NOISY;
        }

        if (s.riskScore >= 75.0d) {
            return AppConfig.BIAS_STATUS_NOISY;
        }

        if (s.biasScore >= AppConfig.BIAS_SCORE_STRONG
                && s.confidence >= AppConfig.BIAS_CONFIDENCE_STRONG) {
            return "STRONG";
        }

        if (s.biasScore >= BIAS_USABLE && s.confidence >= 45.0d) {
            return "USABLE";
        }

        return AppConfig.BIAS_STATUS_NONE;
    }

    private String blockBySignal(BiasSignal s) {
        if (s == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (s.sampleCount < WINDOW_SHORT) {
            return AppConfig.BLOCK_HISTORY_WEAK;
        }

        if (s.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (s.riskScore >= 82.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (s.biasScore < 36.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (s.biasScore < BIAS_USABLE && s.numberBiasScore < 44.0d && s.comboBiasScore < 44.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        return "";
    }

    private double candidateCoverageAgainstBase(Models.ComboCandidate candidate, BiasSignal base) {
        if (candidate == null || base == null || !candidate.isValidK3()) {
            return 0.0d;
        }

        int hit = 0;

        if (Models.maskContains(base.k3Mask, candidate.n1)) hit++;
        if (Models.maskContains(base.k3Mask, candidate.n2)) hit++;
        if (Models.maskContains(base.k3Mask, candidate.n3)) hit++;

        return AppConfig.clampScore100((((double) hit) / 3.0d) * 100.0d);
    }

    private double[] frequency(ArrayList<Models.Draw> history, int position, int window) {
        double[] out = new double[AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return out;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(n)) {
                out[n] += 1.0d;
                total++;
            }
        }

        if (total <= 0) {
            return out;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            out[n] = out[n] / ((double) total);
        }

        return out;
    }

    private double maskMass(double[] freq, int mask) {
        if (freq == null || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        double sum = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(mask, n)) {
                sum += freq[n];
            }
        }

        return AppConfig.clampProbability(sum);
    }

    private int comboHit(ArrayList<Models.Draw> history, int position, int mask, int window) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(mask)) {
            return 0;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);
            if (Models.maskContains(mask, n)) {
                hit++;
            }
        }

        return hit;
    }

    private int[] top3(double[] score) {
        int[] nums = new int[] {1, 2, 3};

        for (int i = 0; i < AppConfig.FIXED_K; i++) {
            int best = 1;
            double bestScore = -999.0d;

            for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                boolean used = false;

                for (int k = 0; k < i; k++) {
                    if (nums[k] == n) {
                        used = true;
                        break;
                    }
                }

                if (used) {
                    continue;
                }

                double s = score == null || n >= score.length ? 0.0d : score[n];

                if (s > bestScore) {
                    bestScore = s;
                    best = n;
                }
            }

            nums[i] = best;
        }

        return nums;
    }

    private void sortNumbers(int[] nums) {
        if (nums == null) {
            return;
        }

        for (int i = 0; i < nums.length - 1; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] < nums[i]) {
                    int t = nums[i];
                    nums[i] = nums[j];
                    nums[j] = t;
                }
            }
        }
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

    private void sortAndRank(List<BiasSignal> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<BiasSignal>() {
                    @Override
                    public int compare(BiasSignal a, BiasSignal b) {
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
            BiasSignal s = list.get(i);
            if (s != null) {
                s.rank = i + 1;
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

    private String buildEvidenceKey(BiasSignal s) {
        if (s == null) {
            return "";
        }

        return "BIAS_P"
                + s.position
                + "_B"
                + bucket(s.biasScore)
                + "_R"
                + bucket(s.riskScore)
                + "_N"
                + s.n1
                + "_"
                + s.n2
                + "_"
                + s.n3;
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

    private String buildSummary(BiasSignal s) {
        if (s == null) {
            return "偏差证据为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("偏差证据P");
        sb.append(s.position);
        sb.append("：");
        sb.append(UiFormatter.numberGroup(s.n1, s.n2, s.n3));

        sb.append("，总分=");
        sb.append(UiFormatter.score(s.biasScore));

        sb.append("，位置=");
        sb.append(UiFormatter.score(s.positionBiasScore));

        sb.append("，号码=");
        sb.append(UiFormatter.score(s.numberBiasScore));

        sb.append("，组合=");
        sb.append(UiFormatter.score(s.comboBiasScore));

        sb.append("，转移=");
        sb.append(UiFormatter.score(s.transitionBiasScore));

        sb.append("，周期=");
        sb.append(UiFormatter.score(s.cycleBiasScore));

        sb.append("，状态=");
        sb.append(UiFormatter.score(s.stateBiasScore));

        sb.append("，置信=");
        sb.append(UiFormatter.score(s.confidence));

        sb.append("，风险=");
        sb.append(UiFormatter.score(s.riskScore));

        if (s.blockCode != null && s.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(s.blockCode));
        } else {
            sb.append("，偏差可用");
        }

        return sb.toString();
    }

    private String buildCandidateSummary(BiasSignal s, Models.ComboCandidate c) {
        if (s == null) {
            return "偏差候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(buildSummary(s));

        if (c != null) {
            sb.append("；候选=");
            sb.append(UiFormatter.positionAndNumbers(c.position, c.n1, c.n2, c.n3));
            sb.append("，覆盖=");
            sb.append(UiFormatter.score(s.coverageScore));
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

    public static final class BiasSignal {
        public int rank;
        public int position;
        public long createdAt;

        public String baseIssue;
        public int sampleCount;

        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public double positionBiasScore;
        public double numberBiasScore;
        public double comboBiasScore;
        public double transitionBiasScore;
        public double cycleBiasScore;
        public double stateBiasScore;

        public double coverageScore;
        public double biasScore;
        public double confidence;
        public double driftRisk;
        public double noiseRisk;
        public double riskScore;

        public String status;
        public String blockCode;
        public String evidenceKey;
        public String summaryText;

        public BiasSignal() {
            rank = 0;
            position = 0;
            createdAt = 0L;

            baseIssue = "";
            sampleCount = 0;

            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            positionBiasScore = 0.0d;
            numberBiasScore = 0.0d;
            comboBiasScore = 0.0d;
            transitionBiasScore = 0.0d;
            cycleBiasScore = 0.0d;
            stateBiasScore = 0.0d;

            coverageScore = 0.0d;
            biasScore = 0.0d;
            confidence = 0.0d;
            driftRisk = 0.0d;
            noiseRisk = 0.0d;
            riskScore = 0.0d;

            status = AppConfig.BIAS_STATUS_NONE;
            blockCode = "";
            evidenceKey = "";
            summaryText = "";
        }

        public boolean isUsable() {
            return blockCode == null
                    || blockCode.length() == 0
                            && biasScore >= BIAS_USABLE
                            && confidence >= 42.0d
                            && riskScore <= 72.0d;
        }

        public boolean isStrong() {
            return isUsable()
                    && biasScore >= BIAS_STRONG
                    && confidence >= 55.0d
                    && riskScore <= 60.0d;
        }

        public double sortScore() {
            double s = biasScore;

            s += confidence * 0.10d;
            s += coverageScore * 0.06d;
            s -= riskScore * 0.12d;

            if (blockCode != null && blockCode.length() > 0) {
                s -= 10.0d;
            }

            if (isStrong()) {
                s += 4.0d;
            }

            return s;
        }

        public BiasSignal copy() {
            BiasSignal s = new BiasSignal();

            s.rank = rank;
            s.position = position;
            s.createdAt = createdAt;

            s.baseIssue = baseIssue;
            s.sampleCount = sampleCount;

            s.n1 = n1;
            s.n2 = n2;
            s.n3 = n3;
            s.k3Mask = k3Mask;

            s.positionBiasScore = positionBiasScore;
            s.numberBiasScore = numberBiasScore;
            s.comboBiasScore = comboBiasScore;
            s.transitionBiasScore = transitionBiasScore;
            s.cycleBiasScore = cycleBiasScore;
            s.stateBiasScore = stateBiasScore;

            s.coverageScore = coverageScore;
            s.biasScore = biasScore;
            s.confidence = confidence;
            s.driftRisk = driftRisk;
            s.noiseRisk = noiseRisk;
            s.riskScore = riskScore;

            s.status = status;
            s.blockCode = blockCode;
            s.evidenceKey = evidenceKey;
            s.summaryText = summaryText;

            return s;
        }
    }
}
