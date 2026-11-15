package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class CycleRhythmEngine {

    private static final int SHORT_WINDOW = 64;
    private static final int MID_WINDOW = 160;
    private static final int LONG_WINDOW = 500;
    private static final int DEEP_WINDOW = 1000;

    private static final double SCORE_USABLE = 52.0d;
    private static final double SCORE_STRONG = 62.0d;
    private static final double SCORE_A_PLUS = 72.0d;

    public CycleRhythmEngine() {}

    public ArrayList<CycleRhythmResult> analyzeAll(List<Models.Draw> history) {
        ArrayList<CycleRhythmResult> out = new ArrayList<CycleRhythmResult>();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            CycleRhythmResult r = analyzePosition(clean, p);
            out.add(r);
        }

        sortAndRank(out);
        return out;
    }

    public CycleRhythmResult analyzePosition(List<Models.Draw> history, int position) {
        CycleRhythmResult r = new CycleRhythmResult();
        r.position = position;
        r.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        r.sampleCount = clean.size();

        if (!AppConfig.isValidPosition(position)) {
            r.blockCode = AppConfig.BLOCK_POSITION_WEAK;
            r.summaryText = "周期节奏：位置非法";
            return r;
        }

        if (clean.size() < 20) {
            r.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            r.summaryText = "周期节奏：历史少于20期，不能判断号码周期";
            return r;
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        r.baseIssue = latest.issue == null ? "" : latest.issue;
        r.lastNumber = latest.getNumberAtPosition(position);

        ArrayList<NumberCycleSignal> signals = new ArrayList<NumberCycleSignal>();

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            NumberCycleSignal s = analyzeNumber(clean, position, n);
            signals.add(s);
        }

        sortSignals(signals);

        r.numberSignals = signals;

        if (signals.size() >= 3) {
            r.n1 = signals.get(0).number;
            r.n2 = signals.get(1).number;
            r.n3 = signals.get(2).number;
            sortNumbers3(r);
            r.k3Mask = Models.buildK3Mask(r.n1, r.n2, r.n3);
        }

        fillPositionStats(r, signals);

        r.cycleScore = buildCycleScore(r);
        r.recoveryScore = buildRecoveryScore(r);
        r.hotContinuationScore = buildHotContinuationScore(r);
        r.coldRepairScore = buildColdRepairScore(r);
        r.reverseRisk = buildReverseRisk(r);
        r.rhythmScore = buildFinalRhythmScore(r);
        r.score01 = AppConfig.clampScore01(r.rhythmScore / 100.0d);

        r.stateMode = stateByScore(r);
        r.blockCode = blockByRisk(r);
        r.usable = r.blockCode.length() == 0 ? 1 : 0;
        r.strong = r.usable == 1 && r.rhythmScore >= SCORE_STRONG ? 1 : 0;
        r.aPlusSupport = r.usable == 1 && r.rhythmScore >= SCORE_A_PLUS ? 1 : 0;

        r.cycleKey = buildCycleKey(r);
        r.summaryText = buildSummary(r);

        return r;
    }

    public CycleRhythmResult analyzeCandidate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            CycleRhythmResult r = new CycleRhythmResult();
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "周期节奏：候选为空或K3非法";
            return r;
        }

        CycleRhythmResult base = analyzePosition(history, candidate.position);
        CycleRhythmResult r = base == null ? new CycleRhythmResult() : base.copy();

        r.candidateMask = candidate.k3Mask;
        r.candidateCoverage = candidateCoverage(r, candidate.k3Mask);
        r.candidateCycleMass = candidateCycleMass(r, candidate.k3Mask);
        r.candidateRhythmScore =
                AppConfig.clampScore100(
                        r.rhythmScore * 0.58d
                                + r.candidateCoverage * 100.0d * 0.18d
                                + r.candidateCycleMass * 100.0d * 0.24d);

        if (r.candidateRhythmScore < 38.0d && candidate.finalP < 0.430d) {
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
        }

        r.summaryText = buildCandidateSummary(r, candidate);

        return r;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<Models.Draw> history) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        ArrayList<CycleRhythmResult> all = analyzeAll(history);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            CycleRhythmResult r = findForPosition(all, copy.position);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, CycleRhythmResult rhythm) {
        if (candidate == null) {
            return;
        }

        if (rhythm == null) {
            candidate.deepCycleScore = 0.0d;
            candidate.reasonSummary = append(candidate.reasonSummary, "周期节奏：暂无");
            return;
        }

        double coverage = candidateCoverage(rhythm, candidate.k3Mask);
        double mass = candidateCycleMass(rhythm, candidate.k3Mask);

        double candidateScore =
                AppConfig.clampScore100(
                        rhythm.rhythmScore * 0.55d
                                + coverage * 100.0d * 0.18d
                                + mass * 100.0d * 0.27d);

        candidate.deepCycleScore = AppConfig.clampScore01(candidateScore / 100.0d);
        candidate.evidenceScore = blend01(candidate.evidenceScore, candidate.deepCycleScore, 0.15d);
        candidate.deepScore = blend01(candidate.deepScore, candidate.deepCycleScore, 0.13d);

        if (rhythm.strong == 1 && mass >= 0.38d && rhythm.reverseRisk <= 48.0d) {
            candidate.rawP = AppConfig.clampProbability(candidate.rawP + 0.003d);
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + 0.003d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        if (rhythm.reverseRisk >= 72.0d && candidate.finalP < 0.430d) {
            if (candidate.blockCode == null
                    || candidate.blockCode.length() == 0
                    || AppConfig.BLOCK_LOW_PROBABILITY.equals(candidate.blockCode)) {
                candidate.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            }
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "周期节奏P"
                                + rhythm.position
                                + "："
                                + UiFormatter.numberGroup(rhythm.n1, rhythm.n2, rhythm.n3)
                                + "，节奏分="
                                + UiFormatter.score(rhythm.rhythmScore)
                                + "，回补="
                                + UiFormatter.score(rhythm.recoveryScore)
                                + "，延续="
                                + UiFormatter.score(rhythm.hotContinuationScore)
                                + "，反转风险="
                                + UiFormatter.score(rhythm.reverseRisk)
                                + "，候选周期覆盖="
                                + UiFormatter.probability(coverage));
    }

    public Models.CycleFeature buildCycleFeature(List<Models.Draw> history, int position) {
        CycleRhythmResult r = analyzePosition(history, position);

        Models.CycleFeature f = new Models.CycleFeature();

        if (r == null) {
            f.summaryText = "周期节奏为空";
            return f;
        }

        f.position = r.position;
        f.number = r.n1;
        f.avgGap = r.bestAvgGap;
        f.lastSeenGap = r.bestRecentGap;
        f.gapVariance = r.bestGapVariance;
        f.cycleScore = r.rhythmScore;
        f.cycleBucket = bucket(r.rhythmScore);
        f.isNearCyclePoint = r.nearCyclePoint == 1 ? 1 : 0;
        f.summaryText = r.summaryText;

        return f;
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, List<Models.Draw> history) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        ArrayList<CycleRhythmResult> all = analyzeAll(history);
        CycleRhythmResult best = best(all);

        if (best == null) {
            return set;
        }

        set.cycleFeature = buildCycleFeature(history, best.position);

        if (set.bestPosition <= 0 || best.rhythmScore >= SCORE_STRONG) {
            set.bestPosition = best.position;
            set.bestNumbers = new int[] {best.n1, best.n2, best.n3};
        }

        set.globalTrendStrength = Math.max(set.globalTrendStrength, best.rhythmScore);
        set.profileKey = best.cycleKey;
        set.summaryText = append(set.summaryText, best.summaryText);

        return set;
    }

    public CycleRhythmResult findForPosition(List<CycleRhythmResult> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            CycleRhythmResult r = list.get(i);
            if (r != null && r.position == position) {
                return r;
            }
        }

        return null;
    }

    public CycleRhythmResult best(List<CycleRhythmResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        CycleRhythmResult best = null;

        for (int i = 0; i < list.size(); i++) {
            CycleRhythmResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (best == null || r.sortScore() > best.sortScore()) {
                best = r;
            }
        }

        return best;
    }

    public String buildSummary(List<CycleRhythmResult> list) {
        if (list == null || list.size() == 0) {
            return "周期节奏：暂无";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            CycleRhythmResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append("=");
            sb.append(UiFormatter.score(r.rhythmScore));
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));
            sb.append(" ");
            sb.append(UiFormatter.modeText(r.stateMode));

            if (r.blockCode != null && r.blockCode.length() > 0) {
                sb.append(" ");
                sb.append(UiFormatter.blockReasonText(r.blockCode));
            }
        }

        return sb.length() == 0 ? "周期节奏：暂无有效位置" : sb.toString();
    }

    private NumberCycleSignal analyzeNumber(List<Models.Draw> history, int position, int number) {
        NumberCycleSignal s = new NumberCycleSignal();
        s.position = position;
        s.number = number;

        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            s.summaryText = "周期号码无效";
            return s;
        }

        ArrayList<Integer> hitIndexes = new ArrayList<Integer>();

        for (int i = 0; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);
            if (n == number) {
                hitIndexes.add(Integer.valueOf(i));
            }
        }

        s.sampleCount = history.size();
        s.hitCount = hitIndexes.size();

        if (s.hitCount <= 0) {
            s.recentGap = history.size();
            s.avgGap = history.size();
            s.gapVariance = 0.0d;
            s.cycleScore = 28.0d;
            s.recoverySignal = 44.0d;
            s.reverseRisk = 8.0d;
            s.summaryText = "号码" + UiFormatter.number(number) + "暂无出现，按冷号观察";
            return s;
        }

        int lastIndex = hitIndexes.get(hitIndexes.size() - 1).intValue();
        s.recentGap = history.size() - 1 - lastIndex;

        ArrayList<Integer> gaps = new ArrayList<Integer>();

        for (int i = 1; i < hitIndexes.size(); i++) {
            int gap = hitIndexes.get(i).intValue() - hitIndexes.get(i - 1).intValue();
            if (gap > 0) {
                gaps.add(Integer.valueOf(gap));
            }
        }

        if (gaps.size() == 0) {
            s.avgGap = history.size();
            s.gapVariance = 0.0d;
        } else {
            s.avgGap = average(gaps);
            s.gapVariance = variance(gaps, s.avgGap);
        }

        s.frequency64 = frequency(history, position, number, SHORT_WINDOW);
        s.frequency160 = frequency(history, position, number, MID_WINDOW);
        s.frequency500 = frequency(history, position, number, LONG_WINDOW);
        s.frequency1000 = frequency(history, position, number, DEEP_WINDOW);

        s.nearCyclePoint = nearCyclePoint(s);
        s.hotScore = hotScore(s);
        s.coldScore = coldScore(s);
        s.recoverySignal = recoverySignal(s);
        s.stabilityScore = stabilityScore(s);
        s.reverseRisk = reverseRisk(s);
        s.cycleScore = finalNumberCycleScore(s);
        s.confidence = confidenceScore(s);
        s.signalType = signalType(s);
        s.summaryText = buildNumberSummary(s);

        return s;
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

    private double frequency(List<Models.Draw> history, int position, int number, int window) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(number)) {
            return 0.0d;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int hit = 0;

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            total++;

            if (d.getNumberAtPosition(position) == number) {
                hit++;
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        return ((double) hit) / ((double) total);
    }

    private double average(List<Integer> values) {
        if (values == null || values.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;

        for (int i = 0; i < values.size(); i++) {
            sum += values.get(i).intValue();
        }

        return sum / ((double) values.size());
    }

    private double variance(List<Integer> values, double avg) {
        if (values == null || values.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;

        for (int i = 0; i < values.size(); i++) {
            double d = values.get(i).intValue() - avg;
            sum += d * d;
        }

        return sum / ((double) values.size());
    }

    private int nearCyclePoint(NumberCycleSignal s) {
        if (s == null || s.avgGap <= 0.0d) {
            return 0;
        }

        double diff = Math.abs(((double) s.recentGap) - s.avgGap);
        double tolerance = Math.max(2.0d, s.avgGap * 0.28d);

        return diff <= tolerance ? 1 : 0;
    }

    private double hotScore(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 50.0d;

        score += (s.frequency64 - s.frequency160) * 260.0d;
        score += (s.frequency160 - s.frequency500) * 120.0d;

        if (s.frequency64 >= 0.150d) {
            score += 6.0d;
        }

        if (s.recentGap <= 2) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double coldScore(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 50.0d;

        if (s.avgGap > 0.0d) {
            double ratio = ((double) s.recentGap) / s.avgGap;
            score += (ratio - 1.0d) * 26.0d;
        }

        if (s.frequency64 <= s.frequency160 && s.frequency160 <= s.frequency500) {
            score += 5.0d;
        }

        if (s.recentGap >= 12) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double recoverySignal(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        if (s.nearCyclePoint == 1) {
            score += 34.0d;
        }

        score += s.coldScore * 0.34d;
        score += s.stabilityScore * 0.20d;
        score += Math.max(0.0d, 100.0d - s.reverseRisk) * 0.12d;

        if (s.recentGap > s.avgGap && s.avgGap > 0.0d) {
            score += 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double stabilityScore(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        if (s.avgGap <= 0.0d) {
            return 0.0d;
        }

        double normalizedVar = s.gapVariance / (s.avgGap * s.avgGap + 1.0d);
        double score = 100.0d - normalizedVar * 90.0d;

        if (s.hitCount < 3) {
            score -= 18.0d;
        } else if (s.hitCount < 6) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double reverseRisk(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        if (s.frequency64 > s.frequency160 + 0.080d) {
            risk += 18.0d;
        }

        if (s.frequency64 >= 0.220d) {
            risk += 14.0d;
        }

        if (s.recentGap <= 1 && s.hotScore >= 70.0d) {
            risk += 16.0d;
        }

        if (s.gapVariance > s.avgGap * s.avgGap * 1.60d) {
            risk += 12.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double finalNumberCycleScore(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += s.recoverySignal * 0.32d;
        score += s.hotScore * 0.20d;
        score += s.coldScore * 0.18d;
        score += s.stabilityScore * 0.20d;
        score += s.confidence * 0.10d;

        score -= s.reverseRisk * 0.14d;

        if (s.nearCyclePoint == 1) {
            score += 5.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double confidenceScore(NumberCycleSignal s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(1.0d, ((double) s.sampleCount) / 240.0d) * 34.0d;
        score += Math.min(1.0d, ((double) s.hitCount) / 24.0d) * 34.0d;
        score += s.stabilityScore * 0.20d;
        score += Math.max(0.0d, 100.0d - s.reverseRisk) * 0.12d;

        return AppConfig.clampScore100(score);
    }

    private String signalType(NumberCycleSignal s) {
        if (s == null) {
            return "NONE";
        }

        if (s.reverseRisk >= 70.0d) {
            return "REVERSE_RISK";
        }

        if (s.recoverySignal >= 68.0d && s.coldScore >= 58.0d) {
            return "COLD_REPAIR";
        }

        if (s.hotScore >= 68.0d && s.reverseRisk < 55.0d) {
            return "HOT_CONTINUE";
        }

        if (s.nearCyclePoint == 1 && s.stabilityScore >= 52.0d) {
            return "NEAR_CYCLE";
        }

        return "WATCH";
    }

    private void fillPositionStats(CycleRhythmResult r, ArrayList<NumberCycleSignal> signals) {
        if (r == null || signals == null || signals.size() == 0) {
            return;
        }

        NumberCycleSignal a = signals.size() > 0 ? signals.get(0) : null;
        NumberCycleSignal b = signals.size() > 1 ? signals.get(1) : null;
        NumberCycleSignal c = signals.size() > 2 ? signals.get(2) : null;

        if (a != null) {
            r.bestNumber = a.number;
            r.bestRecentGap = a.recentGap;
            r.bestAvgGap = a.avgGap;
            r.bestGapVariance = a.gapVariance;
            r.bestNumberScore = a.cycleScore;
            r.bestSignalType = a.signalType;
        }

        r.top3CycleMass = 0.0d;
        r.top3RecoveryMass = 0.0d;
        r.top3HotMass = 0.0d;
        r.top3ReverseRisk = 0.0d;

        if (a != null) {
            r.top3CycleMass += a.cycleScore / 100.0d;
            r.top3RecoveryMass += a.recoverySignal / 100.0d;
            r.top3HotMass += a.hotScore / 100.0d;
            r.top3ReverseRisk += a.reverseRisk / 100.0d;
        }

        if (b != null) {
            r.top3CycleMass += b.cycleScore / 100.0d;
            r.top3RecoveryMass += b.recoverySignal / 100.0d;
            r.top3HotMass += b.hotScore / 100.0d;
            r.top3ReverseRisk += b.reverseRisk / 100.0d;
        }

        if (c != null) {
            r.top3CycleMass += c.cycleScore / 100.0d;
            r.top3RecoveryMass += c.recoverySignal / 100.0d;
            r.top3HotMass += c.hotScore / 100.0d;
            r.top3ReverseRisk += c.reverseRisk / 100.0d;
        }

        r.top3CycleMass = AppConfig.clampProbability(r.top3CycleMass / 3.0d);
        r.top3RecoveryMass = AppConfig.clampProbability(r.top3RecoveryMass / 3.0d);
        r.top3HotMass = AppConfig.clampProbability(r.top3HotMass / 3.0d);
        r.top3ReverseRisk = AppConfig.clampProbability(r.top3ReverseRisk / 3.0d);

        r.nearCyclePoint = 0;
        for (int i = 0; i < Math.min(3, signals.size()); i++) {
            NumberCycleSignal s = signals.get(i);
            if (s != null && s.nearCyclePoint == 1) {
                r.nearCyclePoint = 1;
                break;
            }
        }
    }

    private double buildCycleScore(CycleRhythmResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;
        score += r.top3CycleMass * 100.0d * 0.55d;
        score += r.top3RecoveryMass * 100.0d * 0.20d;
        score += r.top3HotMass * 100.0d * 0.12d;
        score += Math.max(0.0d, 100.0d - r.top3ReverseRisk * 100.0d) * 0.13d;

        if (r.nearCyclePoint == 1) {
            score += 4.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double buildRecoveryScore(CycleRhythmResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = r.top3RecoveryMass * 100.0d;

        if (r.nearCyclePoint == 1) {
            score += 5.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double buildHotContinuationScore(CycleRhythmResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = r.top3HotMass * 100.0d;

        if (r.top3ReverseRisk > 0.62d) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double buildColdRepairScore(CycleRhythmResult r) {
        if (r == null || r.numberSignals == null || r.numberSignals.size() == 0) {
            return 0.0d;
        }

        double sum = 0.0d;
        int count = 0;

        for (int i = 0; i < Math.min(3, r.numberSignals.size()); i++) {
            NumberCycleSignal s = r.numberSignals.get(i);
            if (s == null) {
                continue;
            }

            sum += s.coldScore;
            count++;
        }

        if (count <= 0) {
            return 0.0d;
        }

        return AppConfig.clampScore100(sum / ((double) count));
    }

    private double buildReverseRisk(CycleRhythmResult r) {
        if (r == null) {
            return 0.0d;
        }

        double risk = r.top3ReverseRisk * 100.0d;

        if (r.hotContinuationScore >= 72.0d && r.recoveryScore < 42.0d) {
            risk += 8.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double buildFinalRhythmScore(CycleRhythmResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.cycleScore * 0.34d;
        score += r.recoveryScore * 0.22d;
        score += r.hotContinuationScore * 0.16d;
        score += r.coldRepairScore * 0.14d;
        score += r.bestNumberScore * 0.14d;

        score -= r.reverseRisk * 0.18d;

        if (r.nearCyclePoint == 1) {
            score += 4.0d;
        }

        if (r.bestSignalType != null && "COLD_REPAIR".equals(r.bestSignalType)) {
            score += 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private String stateByScore(CycleRhythmResult r) {
        if (r == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (r.reverseRisk >= 76.0d) {
            return AppConfig.MODE_REVERSING;
        }

        if (r.rhythmScore >= SCORE_A_PLUS) {
            return AppConfig.MODE_ACCELERATING;
        }

        if (r.rhythmScore >= SCORE_STRONG) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (r.rhythmScore >= SCORE_USABLE) {
            return AppConfig.MODE_FORMING;
        }

        if (r.recoveryScore >= 58.0d && r.reverseRisk < 62.0d) {
            return AppConfig.MODE_RECOVERING;
        }

        if (r.rhythmScore >= 42.0d) {
            return AppConfig.MODE_WEAKENING;
        }

        return AppConfig.MODE_CHAOS;
    }

    private String blockByRisk(CycleRhythmResult r) {
        if (r == null) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (!Models.isValidK3Mask(r.k3Mask)) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (r.reverseRisk >= 82.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (r.rhythmScore < 36.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        if (r.rhythmScore < SCORE_USABLE
                && r.recoveryScore < 45.0d
                && r.hotContinuationScore < 45.0d) {
            return AppConfig.BLOCK_COMBO_WEAK;
        }

        return "";
    }

    private double candidateCoverage(CycleRhythmResult r, int mask) {
        if (r == null || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        int hit = 0;

        if (Models.maskContains(mask, r.n1)) hit++;
        if (Models.maskContains(mask, r.n2)) hit++;
        if (Models.maskContains(mask, r.n3)) hit++;

        return ((double) hit) / 3.0d;
    }

    private double candidateCycleMass(CycleRhythmResult r, int mask) {
        if (r == null || r.numberSignals == null || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        double sum = 0.0d;
        int count = 0;

        for (int i = 0; i < r.numberSignals.size(); i++) {
            NumberCycleSignal s = r.numberSignals.get(i);
            if (s == null) {
                continue;
            }

            if (Models.maskContains(mask, s.number)) {
                sum += s.cycleScore / 100.0d;
                count++;
            }
        }

        if (count <= 0) {
            return 0.0d;
        }

        return AppConfig.clampProbability(sum / ((double) count));
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

    private void sortSignals(List<NumberCycleSignal> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<NumberCycleSignal>() {
                    @Override
                    public int compare(NumberCycleSignal a, NumberCycleSignal b) {
                        double sa = a == null ? -999.0d : a.sortScore();
                        double sb = b == null ? -999.0d : b.sortScore();

                        if (sa < sb) return 1;
                        if (sa > sb) return -1;

                        int na = a == null ? 999 : a.number;
                        int nb = b == null ? 999 : b.number;

                        return na - nb;
                    }
                });

        for (int i = 0; i < list.size(); i++) {
            NumberCycleSignal s = list.get(i);
            if (s != null) {
                s.rank = i + 1;
            }
        }
    }

    private void sortAndRank(List<CycleRhythmResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<CycleRhythmResult>() {
                    @Override
                    public int compare(CycleRhythmResult a, CycleRhythmResult b) {
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
            CycleRhythmResult r = list.get(i);
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

    private void sortNumbers3(CycleRhythmResult r) {
        if (r == null) {
            return;
        }

        int[] nums = new int[] {r.n1, r.n2, r.n3};

        for (int i = 0; i < nums.length - 1; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[j] < nums[i]) {
                    int t = nums[i];
                    nums[i] = nums[j];
                    nums[j] = t;
                }
            }
        }

        r.n1 = nums[0];
        r.n2 = nums[1];
        r.n3 = nums[2];
    }

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildCycleKey(CycleRhythmResult r) {
        if (r == null) {
            return "";
        }

        return "CY_P"
                + r.position
                + "_B"
                + bucket(r.rhythmScore)
                + "_N"
                + r.n1
                + "_"
                + r.n2
                + "_"
                + r.n3;
    }

    private String buildNumberSummary(NumberCycleSignal s) {
        if (s == null) {
            return "周期号码为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("N");
        sb.append(UiFormatter.number(s.number));
        sb.append(" Gap=");
        sb.append(s.recentGap);
        sb.append("/");
        sb.append(UiFormatter.score(s.avgGap));
        sb.append(" Cycle=");
        sb.append(UiFormatter.score(s.cycleScore));
        sb.append(" Type=");
        sb.append(s.signalType);

        return sb.toString();
    }

    private String buildSummary(CycleRhythmResult r) {
        if (r == null) {
            return "周期节奏为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("周期节奏P");
        sb.append(r.position);
        sb.append("：号码");
        sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));

        sb.append("，节奏分=");
        sb.append(UiFormatter.score(r.rhythmScore));

        sb.append("，周期=");
        sb.append(UiFormatter.score(r.cycleScore));

        sb.append("，回补=");
        sb.append(UiFormatter.score(r.recoveryScore));

        sb.append("，热延续=");
        sb.append(UiFormatter.score(r.hotContinuationScore));

        sb.append("，冷回补=");
        sb.append(UiFormatter.score(r.coldRepairScore));

        sb.append("，反转风险=");
        sb.append(UiFormatter.score(r.reverseRisk));

        sb.append("，最佳号=");
        sb.append(UiFormatter.number(r.bestNumber));

        sb.append("，最近间隔=");
        sb.append(r.bestRecentGap);

        sb.append("，均间隔=");
        sb.append(UiFormatter.score(r.bestAvgGap));

        if (r.nearCyclePoint == 1) {
            sb.append("，接近周期点");
        }

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        } else {
            sb.append("，可作为候选证据");
        }

        return sb.toString();
    }

    private String buildCandidateSummary(CycleRhythmResult r, Models.ComboCandidate c) {
        if (r == null) {
            return "周期节奏候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(buildSummary(r));

        if (c != null) {
            sb.append("；候选=");
            sb.append(UiFormatter.positionAndNumbers(c.position, c.n1, c.n2, c.n3));
            sb.append("，覆盖=");
            sb.append(UiFormatter.probability(r.candidateCoverage));
            sb.append("，候选周期分=");
            sb.append(UiFormatter.score(r.candidateRhythmScore));
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

    public static final class NumberCycleSignal {
        public int rank;
        public int position;
        public int number;

        public int sampleCount;
        public int hitCount;
        public int recentGap;

        public double avgGap;
        public double gapVariance;

        public double frequency64;
        public double frequency160;
        public double frequency500;
        public double frequency1000;

        public int nearCyclePoint;

        public double hotScore;
        public double coldScore;
        public double recoverySignal;
        public double stabilityScore;
        public double reverseRisk;
        public double cycleScore;
        public double confidence;

        public String signalType;
        public String summaryText;

        public NumberCycleSignal() {
            rank = 0;
            position = 0;
            number = 0;

            sampleCount = 0;
            hitCount = 0;
            recentGap = 0;

            avgGap = 0.0d;
            gapVariance = 0.0d;

            frequency64 = 0.0d;
            frequency160 = 0.0d;
            frequency500 = 0.0d;
            frequency1000 = 0.0d;

            nearCyclePoint = 0;

            hotScore = 0.0d;
            coldScore = 0.0d;
            recoverySignal = 0.0d;
            stabilityScore = 0.0d;
            reverseRisk = 0.0d;
            cycleScore = 0.0d;
            confidence = 0.0d;

            signalType = "";
            summaryText = "";
        }

        public double sortScore() {
            double s = cycleScore;
            s += recoverySignal * 0.12d;
            s += hotScore * 0.06d;
            s += coldScore * 0.06d;
            s += confidence * 0.08d;
            s -= reverseRisk * 0.10d;

            if (nearCyclePoint == 1) {
                s += 3.0d;
            }

            if ("REVERSE_RISK".equals(signalType)) {
                s -= 8.0d;
            }

            return s;
        }

        public NumberCycleSignal copy() {
            NumberCycleSignal s = new NumberCycleSignal();

            s.rank = rank;
            s.position = position;
            s.number = number;

            s.sampleCount = sampleCount;
            s.hitCount = hitCount;
            s.recentGap = recentGap;

            s.avgGap = avgGap;
            s.gapVariance = gapVariance;

            s.frequency64 = frequency64;
            s.frequency160 = frequency160;
            s.frequency500 = frequency500;
            s.frequency1000 = frequency1000;

            s.nearCyclePoint = nearCyclePoint;

            s.hotScore = hotScore;
            s.coldScore = coldScore;
            s.recoverySignal = recoverySignal;
            s.stabilityScore = stabilityScore;
            s.reverseRisk = reverseRisk;
            s.cycleScore = cycleScore;
            s.confidence = confidence;

            s.signalType = signalType;
            s.summaryText = summaryText;

            return s;
        }
    }

    public static final class CycleRhythmResult {
        public int rank;
        public int position;
        public long createdAt;

        public String baseIssue;

        public int sampleCount;
        public int lastNumber;

        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public int bestNumber;
        public int bestRecentGap;
        public double bestAvgGap;
        public double bestGapVariance;
        public double bestNumberScore;
        public String bestSignalType;

        public double top3CycleMass;
        public double top3RecoveryMass;
        public double top3HotMass;
        public double top3ReverseRisk;
        public double cycleScore;
        public double recoveryScore;
        public double hotContinuationScore;
        public double coldRepairScore;
        public double reverseRisk;
        public double rhythmScore;
        public double score01;
        public int nearCyclePoint;
        public int candidateMask;
        public double candidateCoverage;
        public double candidateCycleMass;
        public double candidateRhythmScore;
        public int usable;
        public int strong;
        public int aPlusSupport;
        public String cycleKey;
        public String stateMode;
        public String blockCode;
        public String summaryText;
        public ArrayList<NumberCycleSignal> numberSignals;

        public CycleRhythmResult() {
            rank = 0;
            position = 0;
            createdAt = 0L;
            baseIssue = "";
            sampleCount = 0;
            lastNumber = 0;
            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;
            bestNumber = 0;
            bestRecentGap = 0;
            bestAvgGap = 0.0d;
            bestGapVariance = 0.0d;
            bestNumberScore = 0.0d;
            bestSignalType = "";
            top3CycleMass = 0.0d;
            top3RecoveryMass = 0.0d;
            top3HotMass = 0.0d;
            top3ReverseRisk = 0.0d;
            cycleScore = 0.0d;
            recoveryScore = 0.0d;
            hotContinuationScore = 0.0d;
            coldRepairScore = 0.0d;
            reverseRisk = 0.0d;
            rhythmScore = 0.0d;
            score01 = 0.0d;
            nearCyclePoint = 0;
            candidateMask = 0;
            candidateCoverage = 0.0d;
            candidateCycleMass = 0.0d;
            candidateRhythmScore = 0.0d;
            usable = 0;
            strong = 0;
            aPlusSupport = 0;
            cycleKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            blockCode = "";
            summaryText = "";
            numberSignals = new ArrayList<NumberCycleSignal>();
        }

        public boolean isValidK3Numbers() {
            return AppConfig.isValidK3Numbers(n1, n2, n3) && Models.isValidK3Mask(k3Mask);
        }

        public boolean isUsableForBet() {
            return usable == 1
                    && rhythmScore >= SCORE_USABLE
                    && reverseRisk < 78.0d
                    && isValidK3Numbers();
        }

        public double sortScore() {
            double s = rhythmScore;
            s += cycleScore * 0.08d;
            s += recoveryScore * 0.08d;
            s += hotContinuationScore * 0.05d;
            s += coldRepairScore * 0.05d;
            s -= reverseRisk * 0.08d;
            if (usable == 0) {
                s -= 10.0d;
            }
            if (strong == 1) {
                s += 3.0d;
            }
            if (aPlusSupport == 1) {
                s += 4.0d;
            }
            if (nearCyclePoint == 1) {
                s += 2.0d;
            }
            return s;
        }

        public CycleRhythmResult copy() {
            CycleRhythmResult r = new CycleRhythmResult();
            r.rank = rank;
            r.position = position;
            r.createdAt = createdAt;
            r.baseIssue = baseIssue;
            r.sampleCount = sampleCount;
            r.lastNumber = lastNumber;
            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;
            r.bestNumber = bestNumber;
            r.bestRecentGap = bestRecentGap;
            r.bestAvgGap = bestAvgGap;
            r.bestGapVariance = bestGapVariance;
            r.bestNumberScore = bestNumberScore;
            r.bestSignalType = bestSignalType;
            r.top3CycleMass = top3CycleMass;
            r.top3RecoveryMass = top3RecoveryMass;
            r.top3HotMass = top3HotMass;
            r.top3ReverseRisk = top3ReverseRisk;
            r.cycleScore = cycleScore;
            r.recoveryScore = recoveryScore;
            r.hotContinuationScore = hotContinuationScore;
            r.coldRepairScore = coldRepairScore;
            r.reverseRisk = reverseRisk;
            r.rhythmScore = rhythmScore;
            r.score01 = score01;
            r.nearCyclePoint = nearCyclePoint;
            r.candidateMask = candidateMask;
            r.candidateCoverage = candidateCoverage;
            r.candidateCycleMass = candidateCycleMass;
            r.candidateRhythmScore = candidateRhythmScore;
            r.usable = usable;
            r.strong = strong;
            r.aPlusSupport = aPlusSupport;
            r.cycleKey = cycleKey;
            r.stateMode = stateMode;
            r.blockCode = blockCode;
            r.summaryText = summaryText;
            r.numberSignals = new ArrayList<NumberCycleSignal>();
            if (numberSignals != null) {
                for (int i = 0; i < numberSignals.size(); i++) {
                    NumberCycleSignal s = numberSignals.get(i);
                    if (s != null) {
                        r.numberSignals.add(s.copy());
                    }
                }
            }
            return r;
        }
    }

    /* STARCORE_TIANSHU_B05_CYCLE_EVIDENCE_ONLY */
    public static final String B05_CYCLE_VERSION = "STARCORE_TIANSHU_B05_CYCLE_EVIDENCE_V1";

    public B05CycleEvidenceBundle buildB05Evidence(V51Models.DrawFrame[] history) {
        long started = System.currentTimeMillis();
        V51Models.DrawFrame[] clean = V51PermutationFeatureEngine.normalizeHistory(history);
        if (clean.length < 24) return B05CycleEvidenceBundle.error("HISTORY_LT_24");
        B05CycleEvidencePacket[] packets = new B05CycleEvidencePacket[AppConfig.POSITION_COUNT];
        double meanSupport = 0.0d;
        double meanRisk = 0.0d;
        for (int p = 0; p < packets.length; p++) {
            int bestLag = 0;
            double best = 0.0d;
            double second = 0.0d;
            for (int lag = 2; lag <= 12; lag++) {
                double score = lagAgreement(clean, p, lag, 240);
                if (score > best) {
                    second = best;
                    best = score;
                    bestLag = lag;
                } else if (score > second) {
                    second = score;
                }
            }
            double margin = Math.max(0.0d, best - second);
            double freshness = recentLagAgreement(clean, p, bestLag, 48);
            double support =
                    V51Math.clamp(0.50d * best + 0.24d * margin + 0.26d * freshness, 0.0d, 1.0d);
            double counter =
                    V51Math.clamp(
                            0.55d * (1.0d - freshness)
                                    + 0.25d * (1.0d - margin)
                                    + 0.20d * (bestLag == 0 ? 1.0d : 0.0d),
                            0.0d,
                            1.0d);
            double risk = V51Math.clamp(0.60d * counter + 0.40d * (1.0d - best), 0.0d, 1.0d);
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_CYCLE_VERSION
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + clean[clean.length - 1].issue
                                    + "|"
                                    + bestLag
                                    + "|"
                                    + best
                                    + "|"
                                    + second
                                    + "|"
                                    + margin
                                    + "|"
                                    + freshness
                                    + "|"
                                    + support
                                    + "|"
                                    + counter
                                    + "|"
                                    + risk);
            packets[p] =
                    new B05CycleEvidencePacket(
                            p + 1,
                            bestLag,
                            best,
                            margin,
                            freshness,
                            support,
                            counter,
                            risk,
                            clean.length,
                            checksum);
            meanSupport += support;
            meanRisk += risk;
        }
        meanSupport /= packets.length;
        meanRisk /= packets.length;
        return new B05CycleEvidenceBundle(
                true,
                packets,
                clean.length,
                meanSupport,
                meanRisk,
                Math.max(0L, System.currentTimeMillis() - started),
                B05CycleEvidenceBundle.hash(
                        clean[clean.length - 1].issue, packets, meanSupport, meanRisk),
                "");
    }

    private static double lagAgreement(
            V51Models.DrawFrame[] history, int positionIndex, int lag, int window) {
        if (history == null || lag <= 0 || history.length <= lag) return 0.0d;
        int start = Math.max(lag, history.length - Math.max(lag + 1, window));
        int equal = 0;
        int compared = 0;
        for (int i = start; i < history.length; i++) {
            if (history[i].permutation[positionIndex]
                    == history[i - lag].permutation[positionIndex]) equal++;
            compared++;
        }
        double observed = ((double) equal) / Math.max(1, compared);
        return V51Math.clamp((observed - 0.10d) / 0.30d, 0.0d, 1.0d);
    }

    private static double recentLagAgreement(
            V51Models.DrawFrame[] history, int positionIndex, int lag, int window) {
        return lag <= 0 ? 0.0d : lagAgreement(history, positionIndex, lag, window);
    }

    public static final class B05CycleEvidencePacket {
        public final int position;
        public final int bestLag;
        public final double cycleStrength;
        public final double lagMargin;
        public final double freshness;
        public final double support;
        public final double counterEvidence;
        public final double risk;
        public final int sample;
        public final String checksum;

        B05CycleEvidencePacket(
                int position,
                int bestLag,
                double cycleStrength,
                double lagMargin,
                double freshness,
                double support,
                double counterEvidence,
                double risk,
                int sample,
                String checksum) {
            this.position = position;
            this.bestLag = Math.max(0, bestLag);
            this.cycleStrength = V51Math.clamp(cycleStrength, 0.0d, 1.0d);
            this.lagMargin = V51Math.clamp(lagMargin, 0.0d, 1.0d);
            this.freshness = V51Math.clamp(freshness, 0.0d, 1.0d);
            this.support = V51Math.clamp(support, 0.0d, 1.0d);
            this.counterEvidence = V51Math.clamp(counterEvidence, 0.0d, 1.0d);
            this.risk = V51Math.clamp(risk, 0.0d, 1.0d);
            this.sample = Math.max(0, sample);
            this.checksum = checksum == null ? "" : checksum;
        }

        public String canonical() {
            return position
                    + "|"
                    + bestLag
                    + "|"
                    + cycleStrength
                    + "|"
                    + lagMargin
                    + "|"
                    + freshness
                    + "|"
                    + support
                    + "|"
                    + counterEvidence
                    + "|"
                    + risk
                    + "|"
                    + sample
                    + "|"
                    + checksum;
        }
    }

    public static final class B05CycleEvidenceBundle {
        public final boolean valid;
        public final B05CycleEvidencePacket[] packets;
        public final int sample;
        public final double meanSupport;
        public final double meanRisk;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        B05CycleEvidenceBundle(
                boolean valid,
                B05CycleEvidencePacket[] packets,
                int sample,
                double meanSupport,
                double meanRisk,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.packets = packets == null ? new B05CycleEvidencePacket[0] : packets;
            this.sample = Math.max(0, sample);
            this.meanSupport = V51Math.clamp(meanSupport, 0.0d, 1.0d);
            this.meanRisk = V51Math.clamp(meanRisk, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = checksum == null ? "" : checksum;
            this.detail = detail == null ? "" : detail;
        }

        static B05CycleEvidenceBundle error(String detail) {
            return new B05CycleEvidenceBundle(
                    false,
                    new B05CycleEvidencePacket[0],
                    0,
                    0.0d,
                    1.0d,
                    0L,
                    V51ChecksumEngine.sha256(B05_CYCLE_VERSION + "|ERROR|" + detail),
                    detail);
        }

        static String hash(
                String issue,
                B05CycleEvidencePacket[] packets,
                double meanSupport,
                double meanRisk) {
            StringBuilder out = new StringBuilder();
            if (packets != null) {
                for (int i = 0; i < packets.length; i++)
                    out.append(packets[i].canonical()).append(';');
            }
            return V51ChecksumEngine.sha256(
                    B05_CYCLE_VERSION
                            + "|"
                            + issue
                            + "|"
                            + out.toString()
                            + "|"
                            + meanSupport
                            + "|"
                            + meanRisk);
        }
    }
}
