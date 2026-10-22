package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PositionStrengthEngine {

    private static final int W_SHORT = 32;
    private static final int W_MID = 64;
    private static final int W_LONG = 160;
    private static final int W_DEEP = 500;

    private static final double SCORE_PASS = 56.0d;
    private static final double SCORE_STRONG = 64.0d;
    private static final double SCORE_A_PLUS = 72.0d;

    public PositionStrengthEngine() {}

    public ArrayList<PositionStrengthResult> analyzeAll(List<Models.Draw> history) {
        return analyzeAll(history, null);
    }

    public ArrayList<PositionStrengthResult> analyzeAll(
            List<Models.Draw> history, List<Models.ShapeVector> vectors) {
        ArrayList<PositionStrengthResult> out = new ArrayList<PositionStrengthResult>();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        for (int p = 1; p <= AppConfig.POSITION_COUNT; p++) {
            PositionStrengthResult r = analyzePosition(clean, vectors, p);
            out.add(r);
        }

        sortAndRank(out);
        return out;
    }

    public PositionStrengthResult analyzePosition(List<Models.Draw> history, int position) {
        return analyzePosition(history, null, position);
    }

    public PositionStrengthResult analyzePosition(
            List<Models.Draw> history, List<Models.ShapeVector> vectors, int position) {
        PositionStrengthResult r = new PositionStrengthResult();
        r.position = position;
        r.createdAt = System.currentTimeMillis();

        ArrayList<Models.Draw> clean = cleanHistory(history);

        if (!AppConfig.isValidPosition(position)) {
            r.blockCode = AppConfig.BLOCK_POSITION_WEAK;
            r.summaryText = "位置非法，不能参与单位置K3";
            return r;
        }

        r.sampleCount = clean.size();

        if (clean.size() < 16) {
            r.blockCode = AppConfig.BLOCK_HISTORY_WEAK;
            r.summaryText = "位置强度不足：历史少于16期";
            return r;
        }

        Models.ShapeVector v = findVector(vectors, position);

        int[] count32 = countWindow(clean, position, W_SHORT);
        int[] count64 = countWindow(clean, position, W_MID);
        int[] count160 = countWindow(clean, position, W_LONG);
        int[] count500 = countWindow(clean, position, W_DEEP);

        int total32 = countTotal(count32);
        int total64 = countTotal(count64);
        int total160 = countTotal(count160);
        int total500 = countTotal(count500);

        r.top3Mass32 = topMass(count32, total32, 3);
        r.top3Mass64 = topMass(count64, total64, 3);
        r.top3Mass160 = topMass(count160, total160, 3);
        r.top3Mass500 = topMass(count500, total500, 3);

        r.top5Mass32 = topMass(count32, total32, 5);
        r.top5Mass160 = topMass(count160, total160, 5);

        r.entropy32 = entropyScore(count32, total32);
        r.entropy160 = entropyScore(count160, total160);
        r.entropy500 = entropyScore(count500, total500);

        r.concentrationScore = concentrationScore(r.top3Mass32, r.top3Mass64, r.top3Mass160);
        r.momentumScore = momentumScore(r.top3Mass32, r.top3Mass64, r.top3Mass160);
        r.stabilityScore = stabilityScore(count32, total32, count160, total160);
        r.deepStabilityScore = stabilityScore(count64, total64, count500, total500);

        r.driftRisk = driftRisk(count32, total32, count160, total160, count500, total500);
        r.recentBiasScore = recentBiasScore(count32, total32, count64, total64);
        r.hotReverseRisk = hotReverseRisk(count32, total32, count160, total160);

        int[] top3 = topNumbers(count32, count64, count160);
        r.n1 = top3[0];
        r.n2 = top3[1];
        r.n3 = top3[2];
        r.k3Mask = Models.buildK3Mask(r.n1, r.n2, r.n3);

        r.championGap = championGap(count32, total32);
        r.comboGap = comboGap(count32, total32, count160, total160, r.k3Mask);

        if (v != null) {
            mergeVector(r, v);
        } else {
            fillFallbackShape(r);
        }

        r.riskScore = buildRiskScore(r);
        r.trendScore = buildTrendScore(r);
        r.recoveryBlendScore = buildRecoveryBlendScore(r);

        r.score = calculateFinalScore(r);
        r.score01 = AppConfig.clampScore01(r.score / 100.0d);

        r.stateMode = stateByScore(r);
        r.blockCode = blockByRisk(r);
        r.usable = r.blockCode.length() == 0 ? 1 : 0;
        r.strong = r.score >= SCORE_STRONG && r.usable == 1 ? 1 : 0;
        r.aPlusSupport = r.score >= SCORE_A_PLUS && r.usable == 1 ? 1 : 0;
        r.summaryText = buildSummary(r);

        return r;
    }

    public PositionStrengthResult findForPosition(List<PositionStrengthResult> list, int position) {
        if (list == null || list.size() == 0) {
            return null;
        }

        for (int i = 0; i < list.size(); i++) {
            PositionStrengthResult r = list.get(i);
            if (r != null && r.position == position) {
                return r;
            }
        }

        return null;
    }

    public PositionStrengthResult best(List<PositionStrengthResult> list) {
        if (list == null || list.size() == 0) {
            return null;
        }

        PositionStrengthResult best = null;

        for (int i = 0; i < list.size(); i++) {
            PositionStrengthResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (best == null || r.sortScore() > best.sortScore()) {
                best = r;
            }
        }

        return best;
    }

    public int bestPosition(List<PositionStrengthResult> list) {
        PositionStrengthResult r = best(list);
        return r == null ? 0 : r.position;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            List<Models.ComboCandidate> candidates, List<PositionStrengthResult> strengths) {
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
            PositionStrengthResult r = findForPosition(strengths, copy.position);
            applyToCandidate(copy, r);
            out.add(copy);
        }

        sortCandidates(out);
        return out;
    }

    public void applyToCandidate(Models.ComboCandidate c, PositionStrengthResult r) {
        if (c == null) {
            return;
        }

        if (r == null) {
            c.deepPositionScore = 0.0d;
            c.reasonSummary = append(c.reasonSummary, "位置强度：暂无位置证据");
            return;
        }

        c.deepPositionScore = r.score01;

        if (c.deepScore < r.score01) {
            c.deepScore = blend01(c.deepScore, r.score01, 0.22d);
        }

        if (c.evidenceScore < r.score01) {
            c.evidenceScore = blend01(c.evidenceScore, r.score01, 0.18d);
        }

        if (c.position == r.position && r.isValidK3Numbers()) {
            if (c.weakestNumberScore <= 0.0d) {
                c.weakestNumberScore = weakestNumberSupport(c, r);
            }
        }

        if (r.usable == 0 && c.finalP < 0.430d) {
            c.blockCode =
                    r.blockCode == null || r.blockCode.length() == 0
                            ? AppConfig.BLOCK_POSITION_WEAK
                            : r.blockCode;
        }

        if (r.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            c.driftRisk = Math.max(c.driftRisk, r.driftRisk);
        }

        if (r.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            c.breakDepth = Math.max(c.breakDepth, r.breakDepth);
        }

        if (r.trendLife > c.trendLife) {
            c.trendLife = blendScore(c.trendLife, r.trendLife, 0.18d);
        }

        if (r.recoveryScore > c.recoveryScore) {
            c.recoveryScore = blendScore(c.recoveryScore, r.recoveryScore, 0.18d);
        }

        if (r.championGap > c.championGap) {
            c.championGap = blendProbability(c.championGap, r.championGap, 0.16d);
        }

        if (r.comboGap > c.comboGap) {
            c.comboGap = blendProbability(c.comboGap, r.comboGap, 0.16d);
        }

        c.reasonSummary =
                append(
                        c.reasonSummary,
                        "位置强度P"
                                + r.position
                                + "="
                                + UiFormatter.score(r.score)
                                + "，集中="
                                + UiFormatter.score(r.concentrationScore)
                                + "，稳定="
                                + UiFormatter.score(r.stabilityScore)
                                + "，漂移="
                                + UiFormatter.score(r.driftRisk)
                                + "，状态="
                                + UiFormatter.modeText(r.stateMode));
    }

    public Models.DeepFeatureSet applyToDeepFeatureSet(
            Models.DeepFeatureSet set, List<PositionStrengthResult> strengths) {
        if (set == null) {
            set = new Models.DeepFeatureSet();
        }

        PositionStrengthResult best = best(strengths);
        if (best == null) {
            return set;
        }

        set.bestPosition = best.position;
        set.bestNumbers = new int[] {best.n1, best.n2, best.n3};
        set.globalDriftRisk = best.driftRisk;
        set.globalTrendStrength = best.trendScore;
        set.globalBiasScore = best.recentBiasScore;
        set.globalEntropy = best.entropy160;
        set.stateMode = best.stateMode;
        set.profileKey = best.profileKey;
        set.summaryText = best.summaryText;

        return set;
    }

    public String buildStrengthSummary(List<PositionStrengthResult> list) {
        if (list == null || list.size() == 0) {
            return "位置强度：暂无";
        }

        StringBuilder sb = new StringBuilder();

        int limit = Math.min(list.size(), 3);

        for (int i = 0; i < limit; i++) {
            PositionStrengthResult r = list.get(i);
            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("P");
            sb.append(r.position);
            sb.append("=");
            sb.append(UiFormatter.score(r.score));
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));
            sb.append(" ");
            sb.append(UiFormatter.modeText(r.stateMode));

            if (r.blockCode != null && r.blockCode.length() > 0) {
                sb.append(" ");
                sb.append(UiFormatter.blockReasonText(r.blockCode));
            }
        }

        return sb.length() == 0 ? "位置强度：暂无有效位置" : sb.toString();
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

    private int[] countWindow(List<Models.Draw> history, int position, int window) {
        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return counts;
        }

        int start = history.size() - window;
        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d == null || !d.isValid()) {
                continue;
            }

            int n = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(n)) {
                counts[n]++;
            }
        }

        return counts;
    }

    private int countTotal(int[] counts) {
        if (counts == null) {
            return 0;
        }

        int total = 0;
        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            total += counts[n];
        }
        return total;
    }

    private double topMass(int[] counts, int total, int topN) {
        if (counts == null || total <= 0 || topN <= 0) {
            return 0.0d;
        }

        int[] copy = new int[AppConfig.NUMBER_COUNT];

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            copy[n - 1] = counts[n];
        }

        for (int i = 0; i < copy.length - 1; i++) {
            for (int j = i + 1; j < copy.length; j++) {
                if (copy[j] > copy[i]) {
                    int t = copy[i];
                    copy[i] = copy[j];
                    copy[j] = t;
                }
            }
        }

        int sum = 0;
        int limit = Math.min(topN, copy.length);

        for (int i = 0; i < limit; i++) {
            sum += copy[i];
        }

        return ((double) sum) / ((double) total);
    }

    private int[] topNumbers(int[] count32, int[] count64, int[] count160) {
        double[] score = new double[AppConfig.NUMBER_COUNT + 1];

        int t32 = countTotal(count32);
        int t64 = countTotal(count64);
        int t160 = countTotal(count160);

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p32 = t32 > 0 ? ((double) count32[n]) / ((double) t32) : 0.0d;
            double p64 = t64 > 0 ? ((double) count64[n]) / ((double) t64) : 0.0d;
            double p160 = t160 > 0 ? ((double) count160[n]) / ((double) t160) : 0.0d;

            score[n] = p32 * 0.52d + p64 * 0.30d + p160 * 0.18d;

            if (p32 > p64 && p64 >= p160) {
                score[n] += 0.006d;
            }

            if (p32 < p64 && p64 < p160) {
                score[n] -= 0.004d;
            }
        }

        int[] nums = new int[] {1, 2, 3};

        for (int i = 0; i < AppConfig.FIXED_K; i++) {
            int best = 1;
            double bestScore = -1.0d;

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

                if (score[n] > bestScore) {
                    best = n;
                    bestScore = score[n];
                }
            }

            nums[i] = best;
        }

        sortNumbers(nums);
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

    private double entropyScore(int[] counts, int total) {
        if (counts == null || total <= 0) {
            return 0.0d;
        }

        double entropy = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (counts[n] <= 0) {
                continue;
            }

            double p = ((double) counts[n]) / ((double) total);
            entropy += -p * Math.log(p);
        }

        double maxEntropy = Math.log((double) AppConfig.NUMBER_COUNT);
        if (maxEntropy <= 0.0d) {
            return 0.0d;
        }

        double normalized = entropy / maxEntropy;
        return AppConfig.clampScore100(normalized * 100.0d);
    }

    private double concentrationScore(double top3Short, double top3Mid, double top3Long) {
        double base = top3Short * 55.0d + top3Mid * 30.0d + top3Long * 15.0d;

        if (top3Short >= top3Mid && top3Mid >= top3Long) {
            base += 5.0d;
        }

        if (top3Short < 0.330d) {
            base -= 4.0d;
        }

        return AppConfig.clampScore100(base);
    }

    private double momentumScore(double top3Short, double top3Mid, double top3Long) {
        double diff1 = top3Short - top3Mid;
        double diff2 = top3Mid - top3Long;

        double score = 50.0d;
        score += diff1 * 180.0d;
        score += diff2 * 90.0d;

        if (top3Short >= 0.420d) {
            score += 5.0d;
        }

        if (top3Short < 0.300d) {
            score -= 8.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private double stabilityScore(
            int[] shortCounts, int shortTotal, int[] longCounts, int longTotal) {
        if (shortCounts == null || longCounts == null || shortTotal <= 0 || longTotal <= 0) {
            return 0.0d;
        }

        double distance = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double ps = ((double) shortCounts[n]) / ((double) shortTotal);
            double pl = ((double) longCounts[n]) / ((double) longTotal);
            distance += Math.abs(ps - pl);
        }

        double score = 100.0d - distance * 100.0d;

        return AppConfig.clampScore100(score);
    }

    private double driftRisk(
            int[] count32,
            int total32,
            int[] count160,
            int total160,
            int[] count500,
            int total500) {
        if (count32 == null || count160 == null || total32 <= 0 || total160 <= 0) {
            return 50.0d;
        }

        double d1 = distributionDistance(count32, total32, count160, total160);
        double d2 = 0.0d;

        if (count500 != null && total500 > 0) {
            d2 = distributionDistance(count160, total160, count500, total500);
        }

        double risk = d1 * 78.0d + d2 * 32.0d;

        return AppConfig.clampScore100(risk);
    }

    private double distributionDistance(int[] a, int ta, int[] b, int tb) {
        if (a == null || b == null || ta <= 0 || tb <= 0) {
            return 0.0d;
        }

        double distance = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double pa = ((double) a[n]) / ((double) ta);
            double pb = ((double) b[n]) / ((double) tb);
            distance += Math.abs(pa - pb);
        }

        return distance;
    }

    private double recentBiasScore(int[] count32, int total32, int[] count64, int total64) {
        if (count32 == null || count64 == null || total32 <= 0 || total64 <= 0) {
            return 0.0d;
        }

        double bestLift = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p32 = ((double) count32[n]) / ((double) total32);
            double p64 = ((double) count64[n]) / ((double) total64);
            double lift = p32 - p64;

            if (lift > bestLift) {
                bestLift = lift;
            }
        }

        return AppConfig.clampScore100(50.0d + bestLift * 220.0d);
    }

    private double hotReverseRisk(int[] count32, int total32, int[] count160, int total160) {
        if (count32 == null || count160 == null || total32 <= 0 || total160 <= 0) {
            return 0.0d;
        }

        double risk = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p32 = ((double) count32[n]) / ((double) total32);
            double p160 = ((double) count160[n]) / ((double) total160);

            if (p160 >= 0.120d && p32 <= 0.040d) {
                risk += 12.0d;
            }

            if (p32 >= 0.220d && p160 <= 0.080d) {
                risk += 8.0d;
            }
        }

        return AppConfig.clampScore100(risk);
    }

    private double championGap(int[] count32, int total32) {
        if (count32 == null || total32 <= 0) {
            return 0.0d;
        }

        double first = 0.0d;
        double second = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p = ((double) count32[n]) / ((double) total32);

            if (p > first) {
                second = first;
                first = p;
            } else if (p > second) {
                second = p;
            }
        }

        return AppConfig.clampProbability(first - second);
    }

    private double comboGap(int[] count32, int total32, int[] count160, int total160, int k3Mask) {
        if (!Models.isValidK3Mask(k3Mask) || count32 == null || total32 <= 0) {
            return 0.0d;
        }

        double shortMass = 0.0d;
        double longMass = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(k3Mask, n)) {
                shortMass += ((double) count32[n]) / ((double) total32);
                if (count160 != null && total160 > 0) {
                    longMass += ((double) count160[n]) / ((double) total160);
                }
            }
        }

        double baseline = ((double) AppConfig.FIXED_K) / ((double) AppConfig.NUMBER_COUNT);
        double gap = (shortMass - baseline) * 0.70d + (longMass - baseline) * 0.30d;

        return AppConfig.clampProbability(gap);
    }

    private Models.ShapeVector findVector(List<Models.ShapeVector> vectors, int position) {
        if (vectors == null || vectors.size() == 0) {
            return null;
        }

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v != null && v.position == position) {
                return v;
            }
        }

        return null;
    }

    private void mergeVector(PositionStrengthResult r, Models.ShapeVector v) {
        if (r == null || v == null) {
            return;
        }

        r.shapeKey = v.shapeKey == null ? "" : v.shapeKey;
        r.profileKey = "P" + r.position + "_" + r.shapeKey;

        r.vectorTrendLife = v.trendLife;
        r.vectorDriftRisk = v.driftRisk;
        r.vectorBreakDepth = v.breakDepth;
        r.vectorRecoveryScore = v.recoveryScore;

        r.trendLife = blendScore(r.trendLife, v.trendLife, 0.62d);
        r.driftRisk = blendScore(r.driftRisk, v.driftRisk, 0.58d);
        r.breakDepth = blendScore(r.breakDepth, v.breakDepth, 0.58d);
        r.recoveryScore = blendScore(r.recoveryScore, v.recoveryScore, 0.58d);

        if (v.championGap > r.championGap) {
            r.championGap = blendProbability(r.championGap, v.championGap, 0.50d);
        }

        if (v.comboGap > r.comboGap) {
            r.comboGap = blendProbability(r.comboGap, v.comboGap, 0.50d);
        }

        if (v.hotReverseRisk > r.hotReverseRisk) {
            r.hotReverseRisk = blendScore(r.hotReverseRisk, v.hotReverseRisk, 0.55d);
        }

        r.falseStrongRisk = v.falseStrongRisk;

        if (v.stateMode != null && v.stateMode.length() > 0) {
            r.vectorStateMode = v.stateMode;
        }
    }

    private void fillFallbackShape(PositionStrengthResult r) {
        if (r == null) {
            return;
        }

        r.trendLife =
                AppConfig.clampScore100(
                        r.stabilityScore * 0.42d
                                + r.concentrationScore * 0.35d
                                + r.momentumScore * 0.23d);

        r.breakDepth =
                AppConfig.clampScore100(
                        (100.0d - r.stabilityScore) * 0.45d
                                + r.driftRisk * 0.35d
                                + r.hotReverseRisk * 0.20d);

        r.recoveryScore =
                AppConfig.clampScore100(
                        r.concentrationScore * 0.36d
                                + r.momentumScore * 0.32d
                                + r.deepStabilityScore * 0.32d);

        r.profileKey = "P" + r.position + "_AUTO";
    }

    private double buildRiskScore(PositionStrengthResult r) {
        if (r == null) {
            return 100.0d;
        }

        double risk = 0.0d;
        risk += r.driftRisk * 0.36d;
        risk += r.breakDepth * 0.32d;
        risk += r.hotReverseRisk * 0.18d;
        risk += r.falseStrongRisk * 0.14d;

        if (AppConfig.MODE_BROKEN.equals(r.vectorStateMode)
                || AppConfig.MODE_REVERSING.equals(r.vectorStateMode)) {
            risk += 10.0d;
        }

        return AppConfig.clampScore100(risk);
    }

    private double buildTrendScore(PositionStrengthResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;
        score += r.trendLife * 0.34d;
        score += r.stabilityScore * 0.26d;
        score += r.concentrationScore * 0.20d;
        score += r.momentumScore * 0.12d;
        score += r.recentBiasScore * 0.08d;

        return AppConfig.clampScore100(score);
    }

    private double buildRecoveryBlendScore(PositionStrengthResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;
        score += r.recoveryScore * 0.48d;
        score += r.deepStabilityScore * 0.22d;
        score += r.concentrationScore * 0.18d;
        score += (100.0d - r.breakDepth) * 0.12d;

        return AppConfig.clampScore100(score);
    }

    private double calculateFinalScore(PositionStrengthResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += r.concentrationScore * 0.22d;
        score += r.stabilityScore * 0.18d;
        score += r.deepStabilityScore * 0.10d;
        score += r.trendScore * 0.18d;
        score += r.recoveryBlendScore * 0.12d;
        score += r.momentumScore * 0.08d;
        score += r.recentBiasScore * 0.06d;
        score += AppConfig.clampScore100(r.championGap * 100.0d * 4.0d) * 0.03d;
        score += AppConfig.clampScore100(r.comboGap * 100.0d * 3.0d) * 0.03d;

        score -= r.riskScore * 0.18d;

        if (r.top3Mass32 >= 0.430d && r.driftRisk <= AppConfig.DRIFT_RISK_MAX) {
            score += 3.0d;
        }

        if (r.trendLife >= AppConfig.TREND_LIFE_MIN
                && r.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                && r.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
            score += 3.0d;
        }

        if (r.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX) {
            score -= 3.0d;
        }

        return AppConfig.clampScore100(score);
    }

    private String stateByScore(PositionStrengthResult r) {
        if (r == null) {
            return AppConfig.MODE_CHAOS;
        }

        if (r.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.MODE_CHAOS;
        }

        if (r.breakDepth >= AppConfig.BREAK_DEPTH_HARD_BLOCK) {
            return AppConfig.MODE_BROKEN;
        }

        if (r.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX && r.momentumScore < 48.0d) {
            return AppConfig.MODE_REVERSING;
        }

        if (r.score >= SCORE_A_PLUS) {
            return AppConfig.MODE_ACCELERATING;
        }

        if (r.score >= SCORE_STRONG) {
            return AppConfig.MODE_CONFIRMED;
        }

        if (r.score >= SCORE_PASS) {
            return AppConfig.MODE_FORMING;
        }

        if (r.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                && r.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
            return AppConfig.MODE_RECOVERING;
        }

        if (r.score >= 45.0d) {
            return AppConfig.MODE_WEAKENING;
        }

        return AppConfig.MODE_CHAOS;
    }

    private String blockByRisk(PositionStrengthResult r) {
        if (r == null) {
            return AppConfig.BLOCK_POSITION_WEAK;
        }

        if (r.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return AppConfig.BLOCK_DRIFT_HIGH;
        }

        if (r.breakDepth >= AppConfig.BREAK_DEPTH_HARD_BLOCK) {
            return AppConfig.BLOCK_BREAK_HIGH;
        }

        if (AppConfig.MODE_BROKEN.equals(r.stateMode)
                || AppConfig.MODE_REVERSING.equals(r.stateMode)) {
            return AppConfig.BLOCK_TREND_WEAK;
        }

        if (r.score < 42.0d) {
            return AppConfig.BLOCK_POSITION_WEAK;
        }

        if (r.trendLife < AppConfig.TREND_LIFE_MIN && r.score < SCORE_STRONG) {
            return AppConfig.BLOCK_POSITION_WEAK;
        }

        if (r.recoveryScore < AppConfig.RECOVERY_SCORE_MIN
                && r.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return AppConfig.BLOCK_RECOVERY_WEAK;
        }

        return "";
    }

    private double weakestNumberSupport(Models.ComboCandidate c, PositionStrengthResult r) {
        if (c == null || r == null) {
            return 0.0d;
        }

        double a = numberSupport(c.n1, r);
        double b = numberSupport(c.n2, r);
        double d = numberSupport(c.n3, r);

        double min = a;
        if (b < min) min = b;
        if (d < min) min = d;

        return AppConfig.clampScore01(min);
    }

    private double numberSupport(int n, PositionStrengthResult r) {
        if (!AppConfig.isValidNumber(n) || r == null) {
            return 0.0d;
        }

        if (n == r.n1 || n == r.n2 || n == r.n3) {
            return Math.max(0.55d, r.score01);
        }

        return Math.max(0.10d, r.score01 * 0.45d);
    }

    private void sortAndRank(List<PositionStrengthResult> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<PositionStrengthResult>() {
                    @Override
                    public int compare(PositionStrengthResult a, PositionStrengthResult b) {
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
            PositionStrengthResult r = list.get(i);
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

    private double blendScore(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampScore100(oldValue);
        newValue = AppConfig.clampScore100(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampScore100(oldValue * (1.0d - weightNew) + newValue * weightNew);
    }

    private double blendProbability(double oldValue, double newValue, double weightNew) {
        oldValue = AppConfig.clampProbability(oldValue);
        newValue = AppConfig.clampProbability(newValue);

        if (weightNew < 0.0d) weightNew = 0.0d;
        if (weightNew > 1.0d) weightNew = 1.0d;

        return AppConfig.clampProbability(oldValue * (1.0d - weightNew) + newValue * weightNew);
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

    private String buildSummary(PositionStrengthResult r) {
        if (r == null) {
            return "位置强度为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(r.position);
        sb.append("位置强度=");
        sb.append(UiFormatter.score(r.score));

        sb.append("，推荐号码=");
        sb.append(UiFormatter.numberGroup(r.n1, r.n2, r.n3));

        sb.append("，状态=");
        sb.append(UiFormatter.modeText(r.stateMode));

        sb.append("，集中=");
        sb.append(UiFormatter.score(r.concentrationScore));

        sb.append("，稳定=");
        sb.append(UiFormatter.score(r.stabilityScore));

        sb.append("，趋势=");
        sb.append(UiFormatter.score(r.trendLife));

        sb.append("，恢复=");
        sb.append(UiFormatter.score(r.recoveryScore));

        sb.append("，漂移=");
        sb.append(UiFormatter.score(r.driftRisk));

        sb.append("，破位=");
        sb.append(UiFormatter.score(r.breakDepth));

        if (r.blockCode != null && r.blockCode.length() > 0) {
            sb.append("，拦截=");
            sb.append(UiFormatter.blockReasonText(r.blockCode));
        } else {
            sb.append("，可参与候选竞争");
        }

        return sb.toString();
    }

    public static final class PositionStrengthResult {
        public int rank;
        public int position;
        public long createdAt;

        public int sampleCount;

        public int n1;
        public int n2;
        public int n3;
        public int k3Mask;

        public double top3Mass32;
        public double top3Mass64;
        public double top3Mass160;
        public double top3Mass500;

        public double top5Mass32;
        public double top5Mass160;

        public double entropy32;
        public double entropy160;
        public double entropy500;

        public double concentrationScore;
        public double momentumScore;
        public double stabilityScore;
        public double deepStabilityScore;
        public double recentBiasScore;

        public double trendLife;
        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double championGap;
        public double comboGap;
        public double hotReverseRisk;
        public double falseStrongRisk;

        public double vectorTrendLife;
        public double vectorDriftRisk;
        public double vectorBreakDepth;
        public double vectorRecoveryScore;

        public double trendScore;
        public double recoveryBlendScore;
        public double riskScore;

        public double score;
        public double score01;

        public int usable;
        public int strong;
        public int aPlusSupport;

        public String stateMode;
        public String vectorStateMode;
        public String shapeKey;
        public String profileKey;
        public String blockCode;
        public String summaryText;

        public PositionStrengthResult() {
            rank = 0;
            position = 0;
            createdAt = 0L;

            sampleCount = 0;

            n1 = 0;
            n2 = 0;
            n3 = 0;
            k3Mask = 0;

            top3Mass32 = 0.0d;
            top3Mass64 = 0.0d;
            top3Mass160 = 0.0d;
            top3Mass500 = 0.0d;

            top5Mass32 = 0.0d;
            top5Mass160 = 0.0d;

            entropy32 = 0.0d;
            entropy160 = 0.0d;
            entropy500 = 0.0d;

            concentrationScore = 0.0d;
            momentumScore = 0.0d;
            stabilityScore = 0.0d;
            deepStabilityScore = 0.0d;
            recentBiasScore = 0.0d;

            trendLife = 0.0d;
            driftRisk = 0.0d;
            breakDepth = 0.0d;
            recoveryScore = 0.0d;
            championGap = 0.0d;
            comboGap = 0.0d;
            hotReverseRisk = 0.0d;
            falseStrongRisk = 0.0d;

            vectorTrendLife = 0.0d;
            vectorDriftRisk = 0.0d;
            vectorBreakDepth = 0.0d;
            vectorRecoveryScore = 0.0d;

            trendScore = 0.0d;
            recoveryBlendScore = 0.0d;
            riskScore = 0.0d;

            score = 0.0d;
            score01 = 0.0d;

            usable = 0;
            strong = 0;
            aPlusSupport = 0;

            stateMode = AppConfig.MODE_CHAOS;
            vectorStateMode = "";
            shapeKey = "";
            profileKey = "";
            blockCode = "";
            summaryText = "";
        }

        public boolean isValidK3Numbers() {
            return AppConfig.isValidK3Numbers(n1, n2, n3) && Models.isValidK3Mask(k3Mask);
        }

        public boolean isUsableForBet() {
            return usable == 1
                    && score >= SCORE_PASS
                    && driftRisk <= AppConfig.DRIFT_RISK_MAX
                    && breakDepth <= AppConfig.BREAK_DEPTH_MAX;
        }

        public double sortScore() {
            double s = score;
            s += score01 * 3.0d;
            s += championGap * 10.0d;
            s += comboGap * 8.0d;
            s -= riskScore * 0.06d;

            if (usable == 0) {
                s -= 12.0d;
            }

            if (strong == 1) {
                s += 3.0d;
            }

            if (aPlusSupport == 1) {
                s += 4.0d;
            }

            return s;
        }

        public PositionStrengthResult copy() {
            PositionStrengthResult r = new PositionStrengthResult();

            r.rank = rank;
            r.position = position;
            r.createdAt = createdAt;

            r.sampleCount = sampleCount;

            r.n1 = n1;
            r.n2 = n2;
            r.n3 = n3;
            r.k3Mask = k3Mask;

            r.top3Mass32 = top3Mass32;
            r.top3Mass64 = top3Mass64;
            r.top3Mass160 = top3Mass160;
            r.top3Mass500 = top3Mass500;

            r.top5Mass32 = top5Mass32;
            r.top5Mass160 = top5Mass160;

            r.entropy32 = entropy32;
            r.entropy160 = entropy160;
            r.entropy500 = entropy500;

            r.concentrationScore = concentrationScore;
            r.momentumScore = momentumScore;
            r.stabilityScore = stabilityScore;
            r.deepStabilityScore = deepStabilityScore;
            r.recentBiasScore = recentBiasScore;

            r.trendLife = trendLife;
            r.driftRisk = driftRisk;
            r.breakDepth = breakDepth;
            r.recoveryScore = recoveryScore;
            r.championGap = championGap;
            r.comboGap = comboGap;
            r.hotReverseRisk = hotReverseRisk;
            r.falseStrongRisk = falseStrongRisk;

            r.vectorTrendLife = vectorTrendLife;
            r.vectorDriftRisk = vectorDriftRisk;
            r.vectorBreakDepth = vectorBreakDepth;
            r.vectorRecoveryScore = vectorRecoveryScore;

            r.trendScore = trendScore;
            r.recoveryBlendScore = recoveryBlendScore;
            r.riskScore = riskScore;

            r.score = score;
            r.score01 = score01;

            r.usable = usable;
            r.strong = strong;
            r.aPlusSupport = aPlusSupport;

            r.stateMode = stateMode;
            r.vectorStateMode = vectorStateMode;
            r.shapeKey = shapeKey;
            r.profileKey = profileKey;
            r.blockCode = blockCode;
            r.summaryText = summaryText;

            return r;
        }
    }
}
