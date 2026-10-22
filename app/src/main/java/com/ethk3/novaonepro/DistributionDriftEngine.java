package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DistributionDriftEngine {

    private static final int WINDOW_16 = 16;
    private static final int WINDOW_32 = 32;
    private static final int WINDOW_64 = 64;
    private static final int WINDOW_160 = 160;
    private static final int WINDOW_500 = 500;

    private static final double DRIFT_USABLE_MAX = 58.0d;
    private static final double DRIFT_WARN = 66.0d;
    private static final double DRIFT_HARD = 78.0d;

    public DistributionDriftEngine() {}

    public ArrayList<DistributionDriftResult> analyzeAll(List<Models.Draw> history) {
        ArrayList<DistributionDriftResult> out = new ArrayList<DistributionDriftResult>();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            out.add(analyzePosition(clean, p));
        }

        sortAndRank(out);
        return out;
    }

    public DistributionDriftResult analyzePosition(List<Models.Draw> history, int position) {
        DistributionDriftResult r = new DistributionDriftResult();
        r.position = position;
        r.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);
        r.sampleCount = clean.size();

        if (!AppConfig.isValidPosition(position)) {
            r.blockCode = AppConfig.BLOCK_POSITION_WEAK;
            r.summaryText = "分布漂移：位置非法";
            return r;
        }

        if (clean.size() < WINDOW_32) {
            r.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            r.summaryText = "分布漂移：历史少于32期，无法判断分布迁移";
            return r;
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        r.baseIssue = latest.issue == null ? "" : latest.issue;
        r.lastNumber = latest.getNumberAtPosition(position);

        double[] f16 = frequency(clean, position, WINDOW_16);
        double[] f32 = frequency(clean, position, WINDOW_32);
        double[] f64 = frequency(clean, position, WINDOW_64);
        double[] f160 = frequency(clean, position, WINDOW_160);
        double[] f500 = frequency(clean, position, WINDOW_500);

        r.entropy16 = entropy(f16);
        r.entropy32 = entropy(f32);
        r.entropy64 = entropy(f64);
        r.entropy160 = entropy(f160);
        r.entropy500 = entropy(f500);

        int[] top3_16 = topN(f16, 3);
        int[] top3_64 = topN(f64, 3);
        int[] top3_160 = topN(f160, 3);
        int[] top3_500 = topN(f500, 3);

        int[] top5_16 = topN(f16, 5);
        int[] top5_160 = topN(f160, 5);
        int[] top5_500 = topN(f500, 5);

        r.top3ShortMask = Models.buildMaskFromArray(top3_16);
        r.top3MidMask = Models.buildMaskFromArray(top3_160);
        r.top3LongMask = Models.buildMaskFromArray(top3_500);

        r.top5ShortMask = Models.buildMaskFromArray(top5_16);
        r.top5MidMask = Models.buildMaskFromArray(top5_160);
        r.top5LongMask = Models.buildMaskFromArray(top5_500);

        r.top3ShortText = UiFormatter.numberGroup(top3_16);
        r.top3MidText = UiFormatter.numberGroup(top3_160);
        r.top3LongText = UiFormatter.numberGroup(top3_500);

        r.drift16To64 = distributionDistance(f16, f64);
        r.drift32To160 = distributionDistance(f32, f160);
        r.drift64To500 = distributionDistance(f64, f500);

        r.top3Migration = migrationScore(r.top3ShortMask, r.top3MidMask, r.top3LongMask, 3);
        r.top5Migration = migrationScore(r.top5ShortMask, r.top5MidMask, r.top5LongMask, 5);

        r.strongNumberShift = strongShiftScore(f16, f64, f160, f500);
        r.entropyRise = entropyRiseScore(r);
        r.shortLongConflict = conflictScore(f16, f500);

        r.stabilityScore = stabilityScore(r);
        r.driftRisk = finalDriftRisk(r);

        r.stateMode = stateByRisk(r);
        r.blockCode = blockByRisk(r);
        r.usable = r.blockCode.length() == 0 ? 1 : 0;
        r.stable = r.usable == 1 && r.driftRisk <= AppConfig.DRIFT_RISK_MAX ? 1 : 0;

        r.driftKey = buildDriftKey(r);
        r.summaryText = buildSummary(r);

        return r;
    }

    public DistributionDriftResult analyzeCandidate(
            List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            DistributionDriftResult r = new DistributionDriftResult();
            r.blockCode = AppConfig.BLOCK_COMBO_WEAK;
            r.summaryText = "分布漂移：候选为空或K3非法";
            return r;
        }

        DistributionDriftResult base = analyzePosition(history, candidate.position);
        DistributionDriftResult r = base == null ? new DistributionDriftResult() : base.copy();

        r.candidateMask = candidate.k3Mask;
        r.candidateTop3ShortCoverage = coverage(r.top3ShortMask, candidate.k3Mask);
        r.candidateTop3MidCoverage = coverage(r.top3MidMask, candidate.k3Mask);
        r.candidateTop3LongCoverage = coverage(r.top3LongMask, candidate.k3Mask);

        r.candidateDriftRisk =
                AppConfig.clampScore100(
                        r.driftRisk * 0.58d
                                + (1.0d - r.candidateTop3ShortCoverage) * 100.0d * 0.14d
                                + (1.0d - r.candidateTop3MidCoverage) * 100.0d * 0.12d
                                + r.shortLongConflict * 0.16d);

        r.candidateStableScore = AppConfig.clampScore100(100.0d - r.candidateDriftRisk);

        if (r.candidateDriftRisk >= DRIFT_HARD && candidate.finalP < 0.430d) {
            r.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
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

        ArrayList<DistributionDriftResult> all = analyzeAll(history);

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            Models.ComboCandidate copy = c.copy();
            DistributionDriftResult r = findForPosition(all, copy.position);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate candidate, DistributionDriftResult drift) {
        if (candidate == null) {
            return;
        }

        if (drift == null) {
            candidate.reasonSummary = append(candidate.reasonSummary, "分布漂移：暂无");
            return;
        }

        double shortCoverage = coverage(drift.top3ShortMask, candidate.k3Mask);
        double midCoverage = coverage(drift.top3MidMask, candidate.k3Mask);
        double longCoverage = coverage(drift.top3LongMask, candidate.k3Mask);

        double candidateRisk =
                AppConfig.clampScore100(
                        drift.driftRisk * 0.60d
                                + (1.0d - shortCoverage) * 100.0d * 0.14d
                                + (1.0d - midCoverage) * 100.0d * 0.10d
                                + drift.shortLongConflict * 0.16d);

        if (candidateRisk > candidate.driftRisk) {
            candidate.driftRisk = candidateRisk;
        } else {
            candidate.driftRisk =
                    AppConfig.clampScore100(candidate.driftRisk * 0.70d + candidateRisk * 0.30d);
        }

        candidate.evidenceRisk = blend01(candidate.evidenceRisk, candidateRisk / 100.0d, 0.16d);
        candidate.auditPenalty =
                blend01(
                        candidate.auditPenalty,
                        Math.max(0.0d, candidateRisk - 55.0d) / 100.0d,
                        0.12d);

        if (candidateRisk >= DRIFT_HARD && candidate.finalP < 0.430d) {
            candidate.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
        } else if (candidateRisk >= DRIFT_WARN
                && candidate.finalP < 0.410d
                && (candidate.blockCode == null || candidate.blockCode.length() == 0)) {
            candidate.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (candidateRisk <= AppConfig.DRIFT_RISK_MAX
                && shortCoverage >= 0.34d
                && midCoverage >= 0.34d) {
            candidate.finalP = AppConfig.clampProbability(candidate.finalP + 0.002d);
            candidate.edge = candidate.finalP - candidate.dynamicGate;
        }

        candidate.reasonSummary =
                append(
                        candidate.reasonSummary,
                        "分布漂移P"
                                + drift.position
                                + "：风险="
                                + UiFormatter.score(candidateRisk)
                                + "，短Top3="
                                + drift.top3ShortText
                                + "，中Top3="
                                + drift.top3MidText
                                + "，长Top3="
                                + drift.top3LongText
                                + "，短覆盖="
                                + UiFormatter.probability(shortCoverage)
                                + "，中覆盖="
                                + UiFormatter.probability(midCoverage)
                                + "，熵升="
                                + UiFormatter.score(drift.entropyRise)
                                + "，强号迁移="
                                + UiFormatter.score(drift.strongNumberShift));
    }

    public Models.DriftFeature buildDriftFeature(List<Models.Draw> history, int position) {
        DistributionDriftResult r = analyzePosition(history, position);

        Models.DriftFeature f = new Models.DriftFeature();

        if (r == null) {
            f.summaryText = "分布漂移为空";
            return f;
        }

        f.position = r.position;
        f.drift16To64 = r.drift16To64;
        f.drift32To160 = r.drift32To160;
        f.drift64To500 = r.drift64To500;
        f.entropyRise = r.entropyRise;
        f.strongNumberShift = r.strongNumberShift;
        f.driftRisk = r.driftRisk;
        f.summaryText = r.summaryText;

        return f;
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, List<Models.Draw> history) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        ArrayList<DistributionDriftResult> all = analyzeAll(history);
        DistributionDriftResult bestStable = bestStable(all);

        if (bestStable == null) {
            return set;
        }

        set.driftFeature = buildDriftFeature(history, bestStable.position);
        set.globalDriftRisk = bestStable.driftRisk;

        if (bestStable.usable == 1 && bestStable.stabilityScore >= 50.0d) {
            set.bestPosition = bestStable.position;
        }

        set.summaryText = append(set.summaryText, bestStable.summaryText);
        return set;
    }

    public DistributionDriftResult findForPosition(
            List<DistributionDriftResult> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            DistributionDriftResult r = list.get(i);
            if (r != null && r.position == position) {
                return r;
            }
        }

        return null;
    }

    public DistributionDriftResult bestStable(List<DistributionDriftResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        DistributionDriftResult best = null;

        for (int i = 0; i < list.size(); i++) {
            DistributionDriftResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (best == null || r.stableSortScore() > best.stableSortScore()) {
                best = r;
            }
        }

        return best;
    }

    public String buildSummary(List<DistributionDriftResult> list) {
        if (list == null || list.size() == 0) {
            return "分布漂移：暂无";
        }

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            DistributionDriftResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append(" 风险=");
            sb.append(UiFormatter.score(r.driftRisk));
            sb.append(" 稳定=");
            sb.append(UiFormatter.score(r.stabilityScore));
            sb.append(" ");
            sb.append(UiFormatter.modeText(r.stateMode));

            if (r.blockCode != null && r.blockCode.length() > 0) {
                sb.append(" ");
                sb.append(UiFormatter.blockReasonText(r.blockCode));
            }
        }

        return sb.length() == 0 ? "分布漂移：暂无有效位置" : sb.toString();
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

    private double[] frequency(List<Models.Draw> history, int position, int window) {
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

    private double entropy(double[] freq) {
        if (freq == null) {
            return 0.0d;
        }

        double e = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p = freq[n];
            if (p > 0.0d) {
                e -= p * (Math.log(p) / Math.log(2.0d));
            }
        }

        double max = Math.log(AppConfig.NUMBER_COUNT) / Math.log(2.0d);
        if (max <= 0.0d) {
            return 0.0d;
        }

        return AppConfig.clampScore100((e / max) * 100.0d);
    }

    private double distributionDistance(double[] a, double[] b) {
        if (a == null || b == null) {
            return 100.0d;
        }

        double sum = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            sum += Math.abs(a[n] - b[n]);
        }

        return AppConfig.clampScore100(sum * 50.0d * 2.0d);
    }

    private int[] topN(double[] freq, int count) {
        if (count <= 0) {
            count = 3;
        }

        int[] out = new int[count];

        for (int i = 0; i < count; i++) {
            int best = AppConfig.MIN_NUMBER;
            double bestScore = -1.0d;

            for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                boolean used = false;

                for (int k = 0; k < i; k++) {
                    if (out[k] == n) {
                        used = true;
                        break;
                    }
                }

                if (used) {
                    continue;
                }

                double s = freq == null || n >= freq.length ? 0.0d : freq[n];

                if (s > bestScore) {
                    bestScore = s;
                    best = n;
                }
            }

            out[i] = best;
        }

        sortNumbers(out);
        return out;
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

    private double migrationScore(int shortMask, int midMask, int longMask, int topCount) {
        if (topCount <= 0) {
            topCount = 3;
        }

        double shortMid = overlapRate(shortMask, midMask, topCount);
        double shortLong = overlapRate(shortMask, longMask, topCount);
        double midLong = overlapRate(midMask, longMask, topCount);

        double stable = shortMid * 0.42d + shortLong * 0.28d + midLong * 0.30d;
        return AppConfig.clampScore100((1.0d - stable) * 100.0d);
    }

    private double overlapRate(int aMask, int bMask, int topCount) {
        if (topCount <= 0) {
            topCount = 3;
        }

        int overlap = 0;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(aMask, n) && Models.maskContains(bMask, n)) {
                overlap++;
            }
        }

        return AppConfig.clampProbability(((double) overlap) / ((double) topCount));
    }

    private double strongShiftScore(double[] f16, double[] f64, double[] f160, double[] f500) {
        double score = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double shortP = safeFreq(f16, n);
            double midP = safeFreq(f160, n);
            double longP = safeFreq(f500, n);
            double short64 = safeFreq(f64, n);

            double d1 = Math.abs(shortP - midP);
            double d2 = Math.abs(short64 - longP);

            if (d1 > 0.080d) {
                score += d1 * 90.0d;
            }

            if (d2 > 0.060d) {
                score += d2 * 60.0d;
            }

            if (shortP >= 0.200d && longP <= 0.070d) {
                score += 7.0d;
            }

            if (shortP <= 0.030d && longP >= 0.140d) {
                score += 5.0d;
            }
        }

        return AppConfig.clampScore100(score);
    }

    private double safeFreq(double[] f, int number) {
        if (f == null || number < 0 || number >= f.length) {
            return 0.0d;
        }
        return f[number];
    }

    private double entropyRiseScore(DistributionDriftResult r) {
        if (r == null) {
            return 0.0d;
        }

        double rise1 = r.entropy16 - r.entropy64;
        double rise2 = r.entropy32 - r.entropy160;
        double rise3 = r.entropy64 - r.entropy500;

        double score = 0.0d;

        if (rise1 > 0.0d) score += rise1 * 0.55d;
        if (rise2 > 0.0d) score += rise2 * 0.35d;
        if (rise3 > 0.0d) score += rise3 * 0.10d;

        if (r.entropy16 >= 88.0d && r.entropy64 < 82.0d) {
            score += 6.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double conflictScore(double[] f16, double[] f500) {
        if (f16 == null || f500 == null) {
            return 100.0d;
        }

        double score = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double d = Math.abs(f16[n] - f500[n]);

            if (d >= 0.080d) {
                score += d * 80.0d;
            } else {
                score += d * 30.0d;
            }
        }

        return AppConfig.clampScore100(score);
    }

    private double stabilityScore(DistributionDriftResult r) {
        if (r == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        risk += r.drift16To64 * 0.24d;
        risk += r.drift32To160 * 0.24d;
        risk += r.drift64To500 * 0.16d;
        risk += r.top3Migration * 0.14d;
        risk += r.top5Migration * 0.08d;
        risk += r.strongNumberShift * 0.08d;
        risk += r.entropyRise * 0.06d;

        return AppConfig.clampScore100(100.0d - risk);
    }

    private double finalDriftRisk(DistributionDriftResult r) {
        if (r == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += r.drift16To64 * 0.22d;
        risk += r.drift32To160 * 0.21d;
        risk += r.drift64To500 * 0.15d;
        risk += r.top3Migration * 0.15d;
        risk += r.top5Migration * 0.08d;
        risk += r.strongNumberShift * 0.10d;
        risk += r.entropyRise * 0.05d;
        risk += r.shortLongConflict * 0.04d;

        if (r.top3Migration >= 70.0d && r.strongNumberShift >= 60.0d) {
            risk += 6.0d;
        }

        if (r.entropyRise >= 70.0d && r.drift16To64 >= 60.0d) {
            risk += 5.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private String stateByRisk(DistributionDriftResult r) {
        if (r == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (r.driftRisk >= DRIFT_HARD) {
            return AppConfig.MODE_REVERSING;
        }

        if (r.driftRisk >= DRIFT_WARN) {
            return AppConfig.MODE_BROKEN;
        }

        if (r.driftRisk > DRIFT_USABLE_MAX) {
            return AppConfig.MODE_WEAKENING;
        }

        if (r.driftRisk <= AppConfig.DRIFT_RISK_MAX && r.stabilityScore >= 62.0d) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (r.stabilityScore >= 52.0d) {
            return AppConfig.MODE_FORMING;
        }

        return AppConfig.MODE_CHAOS;
    }

    private String blockByRisk(DistributionDriftResult r) {
        if (r == null) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (r.sampleCount < WINDOW_32) {
            return AppConfig.BLOCK_HISTORY_WEAK;
        }

        if (r.driftRisk >= DRIFT_HARD) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (r.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        return "";
    }

    private double coverage(int topMask, int candidateMask) {
        if (!Models.isValidK3Mask(candidateMask)) {
            return 0.0d;
        }

        int hit = 0;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(candidateMask, n) && Models.maskContains(topMask, n)) {
                hit++;
            }
        }

        return AppConfig.clampProbability(((double) hit) / 3.0d);
    }

    private void sortAndRank(List<DistributionDriftResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<DistributionDriftResult>() {
                    @Override
                    public int compare(DistributionDriftResult a, DistributionDriftResult b) {
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
            DistributionDriftResult r = list.get(i);
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

    private double blend01(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore01(oldValue);
        newValue = AppConfig.clampScore01(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore01(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private String buildDriftKey(DistributionDriftResult r) {
        if (r == null) {
            return "";
        }

        return "DF_P" + r.position + "_R" + bucket(r.driftRisk) + "_S" + bucket(r.stabilityScore);
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

    private String buildSummary(DistributionDriftResult r) {
        if (r == null) {
            return "分布漂移为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("分布漂移P");
        sb.append(r.position);

        sb.append("：风险=");
        sb.append(UiFormatter.score(r.driftRisk));

        sb.append("，稳定=");
        sb.append(UiFormatter.score(r.stabilityScore));

        sb.append("，16-64=");
        sb.append(UiFormatter.score(r.drift16To64));

        sb.append("，32-160=");
        sb.append(UiFormatter.score(r.drift32To160));

        sb.append("，64-500=");
        sb.append(UiFormatter.score(r.drift64To500));

        sb.append("，Top3迁移=");
        sb.append(UiFormatter.score(r.top3Migration));

        sb.append("，强号迁移=");
        sb.append(UiFormatter.score(r.strongNumberShift));

        sb.append("，熵升=");
        sb.append(UiFormatter.score(r.entropyRise));

        sb.append("，短Top3=");
        sb.append(r.top3ShortText);

        sb.append("，中Top3=");
        sb.append(r.top3MidText);

        sb.append("，长Top3=");
        sb.append(r.top3LongText);

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        } else {
            sb.append("，分布可用");
        }

        return sb.toString();
    }

    private String buildCandidateSummary(DistributionDriftResult r, Models.ComboCandidate c) {
        if (r == null) {
            return "分布漂移候选为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(buildSummary(r));

        if (c != null) {
            sb.append("；候选=");
            sb.append(UiFormatter.positionAndNumbers(c.position, c.n1, c.n2, c.n3));
            sb.append("，候选漂移风险=");
            sb.append(UiFormatter.score(r.candidateDriftRisk));
            sb.append("，候选稳定=");
            sb.append(UiFormatter.score(r.candidateStableScore));
            sb.append("，短覆盖=");
            sb.append(UiFormatter.probability(r.candidateTop3ShortCoverage));
            sb.append("，中覆盖=");
            sb.append(UiFormatter.probability(r.candidateTop3MidCoverage));
            sb.append("，长覆盖=");
            sb.append(UiFormatter.probability(r.candidateTop3LongCoverage));
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

    public static final class DistributionDriftResult {
        public int rank;
        public int position;
        public long createdAt;

        public String baseIssue;
        public int sampleCount;
        public int lastNumber;

        public double entropy16;
        public double entropy32;
        public double entropy64;
        public double entropy160;
        public double entropy500;

        public double drift16To64;
        public double drift32To160;
        public double drift64To500;

        public int top3ShortMask;
        public int top3MidMask;
        public int top3LongMask;

        public int top5ShortMask;
        public int top5MidMask;
        public int top5LongMask;

        public String top3ShortText;
        public String top3MidText;
        public String top3LongText;

        public double top3Migration;
        public double top5Migration;
        public double strongNumberShift;
        public double entropyRise;
        public double shortLongConflict;

        public double stabilityScore;
        public double driftRisk;

        public int candidateMask;
        public double candidateTop3ShortCoverage;
        public double candidateTop3MidCoverage;
        public double candidateTop3LongCoverage;
        public double candidateDriftRisk;
        public double candidateStableScore;

        public int usable;
        public int stable;

        public String driftKey;
        public String stateMode;
        public String blockCode;
        public String summaryText;

        public DistributionDriftResult() {
            rank = 0;
            position = 0;
            createdAt = 0L;

            baseIssue = "";
            sampleCount = 0;
            lastNumber = 0;

            entropy16 = 0.0d;
            entropy32 = 0.0d;
            entropy64 = 0.0d;
            entropy160 = 0.0d;
            entropy500 = 0.0d;

            drift16To64 = 0.0d;
            drift32To160 = 0.0d;
            drift64To500 = 0.0d;

            top3ShortMask = 0;
            top3MidMask = 0;
            top3LongMask = 0;

            top5ShortMask = 0;
            top5MidMask = 0;
            top5LongMask = 0;

            top3ShortText = "";
            top3MidText = "";
            top3LongText = "";

            top3Migration = 0.0d;
            top5Migration = 0.0d;
            strongNumberShift = 0.0d;
            entropyRise = 0.0d;
            shortLongConflict = 0.0d;

            stabilityScore = 0.0d;
            driftRisk = 0.0d;

            candidateMask = 0;
            candidateTop3ShortCoverage = 0.0d;
            candidateTop3MidCoverage = 0.0d;
            candidateTop3LongCoverage = 0.0d;
            candidateDriftRisk = 0.0d;
            candidateStableScore = 0.0d;

            usable = 0;
            stable = 0;

            driftKey = "";
            stateMode = AppConfig.MODE_CHAOS;
            blockCode = "";
            summaryText = "";
        }

        public boolean isHardDrift() {
            return driftRisk >= DRIFT_HARD
                    || driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK
                    || AppConfig.BLOCK_DRIFT_HIGH.equals(blockCode);
        }

        public boolean isStableEnough() {
            return usable == 1 && driftRisk <= AppConfig.DRIFT_RISK_MAX && stabilityScore >= 50.0d;
        }

        public double sortScore() {
            double s = driftRisk;
            s += top3Migration * 0.14d;
            s += strongNumberShift * 0.12d;
            s += entropyRise * 0.08d;

            if (usable == 0) {
                s += 8.0d;
            }

            return s;
        }

        public double stableSortScore() {
            double s = stabilityScore;
            s -= driftRisk * 0.22d;

            if (usable == 1) {
                s += 6.0d;
            }

            if (stable == 1) {
                s += 5.0d;
            }

            return s;
        }

        public DistributionDriftResult copy() {
            DistributionDriftResult r = new DistributionDriftResult();

            r.rank = rank;
            r.position = position;
            r.createdAt = createdAt;
            r.baseIssue = baseIssue;
            r.sampleCount = sampleCount;
            r.lastNumber = lastNumber;
            r.entropy16 = entropy16;
            r.entropy32 = entropy32;
            r.entropy64 = entropy64;
            r.entropy160 = entropy160;
            r.entropy500 = entropy500;
            r.drift16To64 = drift16To64;
            r.drift32To160 = drift32To160;
            r.drift64To500 = drift64To500;
            r.top3ShortMask = top3ShortMask;
            r.top3MidMask = top3MidMask;
            r.top3LongMask = top3LongMask;
            r.top5ShortMask = top5ShortMask;
            r.top5MidMask = top5MidMask;
            r.top5LongMask = top5LongMask;
            r.top3ShortText = top3ShortText;
            r.top3MidText = top3MidText;
            r.top3LongText = top3LongText;
            r.top3Migration = top3Migration;
            r.top5Migration = top5Migration;
            r.strongNumberShift = strongNumberShift;
            r.entropyRise = entropyRise;
            r.shortLongConflict = shortLongConflict;
            r.stabilityScore = stabilityScore;
            r.driftRisk = driftRisk;
            r.candidateMask = candidateMask;
            r.candidateTop3ShortCoverage = candidateTop3ShortCoverage;
            r.candidateTop3MidCoverage = candidateTop3MidCoverage;
            r.candidateTop3LongCoverage = candidateTop3LongCoverage;
            r.candidateDriftRisk = candidateDriftRisk;
            r.candidateStableScore = candidateStableScore;
            r.usable = usable;
            r.stable = stable;
            r.driftKey = driftKey;
            r.stateMode = stateMode;
            r.blockCode = blockCode;
            r.summaryText = summaryText;
            return r;
        }
    }

    /* STARCORE_TIANSHU_B05_DRIFT_EVIDENCE_ONLY */
    public static final String B05_DRIFT_VERSION = "STARCORE_TIANSHU_B05_DISTRIBUTION_DRIFT_V1";

    public B05DriftEvidenceBundle buildB05Evidence(V51Models.DrawFrame[] history) {
        long started = System.currentTimeMillis();
        RollingWindowCache.SnapshotSet windows = RollingWindowCache.shared().getOrBuild(history);
        RollingWindowCache.Validation validation = windows.validate();
        if (!validation.ok) return B05DriftEvidenceBundle.error(validation.detail);
        B05DriftEvidencePacket[] packets = new B05DriftEvidencePacket[AppConfig.POSITION_COUNT];
        double meanRisk = 0.0d;
        for (int p = 0; p < packets.length; p++) {
            double[] shortP = positionDistribution(windows, 24, p);
            double[] mediumP = positionDistribution(windows, 96, p);
            double[] longP = positionDistribution(windows, 720, p);
            double shortMedium = jsDistance(shortP, mediumP);
            double mediumLong = jsDistance(mediumP, longP);
            double shortLong = jsDistance(shortP, longP);
            double risk =
                    V51Math.clamp(
                            0.35d * shortMedium + 0.25d * mediumLong + 0.40d * shortLong,
                            0.0d,
                            1.0d);
            double stability = 1.0d - risk;
            DriftUncertaintyStateR932.Result driftState =
                    DriftUncertaintyStateR932.classify(
                            shortMedium, mediumLong, shortLong, true, windows.historyRows);
            String mode = driftState.state.name();
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_DRIFT_VERSION
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + windows.lastIssue
                                    + "|"
                                    + shortMedium
                                    + "|"
                                    + mediumLong
                                    + "|"
                                    + shortLong
                                    + "|"
                                    + risk
                                    + "|"
                                    + stability
                                    + "|"
                                    + mode);
            packets[p] =
                    new B05DriftEvidencePacket(
                            p + 1,
                            mode,
                            shortMedium,
                            mediumLong,
                            shortLong,
                            risk,
                            stability,
                            windows.historyRows,
                            checksum);
            meanRisk += risk;
        }
        meanRisk /= packets.length;
        return new B05DriftEvidenceBundle(
                true,
                packets,
                windows.historyRows,
                meanRisk,
                Math.max(0L, System.currentTimeMillis() - started),
                B05DriftEvidenceBundle.hash(windows.lastIssue, packets, meanRisk),
                "");
    }

    private static double[] positionDistribution(
            RollingWindowCache.SnapshotSet windows, int window, int positionIndex) {
        double[] out = new double[10];
        double sum = 0.0d;
        for (int n = 0; n < out.length; n++) {
            out[n] = Math.max(1.0e-9d, windows.frequency(window, positionIndex, n));
            sum += out[n];
        }
        for (int n = 0; n < out.length; n++) out[n] /= sum;
        return out;
    }

    private static double jsDistance(double[] left, double[] right) {
        if (left == null || right == null || left.length != right.length) return 1.0d;
        double value = 0.0d;
        for (int i = 0; i < left.length; i++) {
            double p = Math.max(1.0e-12d, left[i]);
            double q = Math.max(1.0e-12d, right[i]);
            double m = 0.5d * (p + q);
            value += 0.5d * p * Math.log(p / m) + 0.5d * q * Math.log(q / m);
        }
        return V51Math.clamp(Math.sqrt(Math.max(0.0d, value) / Math.log(2.0d)), 0.0d, 1.0d);
    }

    public static final class B05DriftEvidencePacket {
        public final int position;
        public final String mode;
        public final double shortMedium;
        public final double mediumLong;
        public final double shortLong;
        public final double risk;
        public final double stability;
        public final int sample;
        public final String checksum;

        B05DriftEvidencePacket(
                int position,
                String mode,
                double shortMedium,
                double mediumLong,
                double shortLong,
                double risk,
                double stability,
                int sample,
                String checksum) {
            this.position = position;
            this.mode = mode == null ? "" : mode;
            this.shortMedium = V51Math.clamp(shortMedium, 0, 1);
            this.mediumLong = V51Math.clamp(mediumLong, 0, 1);
            this.shortLong = V51Math.clamp(shortLong, 0, 1);
            this.risk = V51Math.clamp(risk, 0, 1);
            this.stability = V51Math.clamp(stability, 0, 1);
            this.sample = Math.max(0, sample);
            this.checksum = checksum == null ? "" : checksum;
        }

        public String canonical() {
            return position
                    + "|"
                    + mode
                    + "|"
                    + shortMedium
                    + "|"
                    + mediumLong
                    + "|"
                    + shortLong
                    + "|"
                    + risk
                    + "|"
                    + stability
                    + "|"
                    + sample
                    + "|"
                    + checksum;
        }
    }

    public static final class B05DriftEvidenceBundle {
        public final boolean valid;
        public final B05DriftEvidencePacket[] packets;
        public final int sample;
        public final double meanRisk;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        B05DriftEvidenceBundle(
                boolean valid,
                B05DriftEvidencePacket[] packets,
                int sample,
                double meanRisk,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.packets = packets == null ? new B05DriftEvidencePacket[0] : packets;
            this.sample = Math.max(0, sample);
            this.meanRisk = V51Math.clamp(meanRisk, 0, 1);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = checksum == null ? "" : checksum;
            this.detail = detail == null ? "" : detail;
        }

        static B05DriftEvidenceBundle error(String detail) {
            return new B05DriftEvidenceBundle(
                    false,
                    new B05DriftEvidencePacket[0],
                    0,
                    1.0d,
                    0L,
                    V51ChecksumEngine.sha256(B05_DRIFT_VERSION + "|ERROR|" + detail),
                    detail);
        }

        static String hash(String issue, B05DriftEvidencePacket[] packets, double meanRisk) {
            StringBuilder out = new StringBuilder();
            if (packets != null)
                for (int i = 0; i < packets.length; i++)
                    out.append(packets[i].canonical()).append(';');
            return V51ChecksumEngine.sha256(
                    B05_DRIFT_VERSION + "|" + issue + "|" + out.toString() + "|" + meanRisk);
        }
    }
}
