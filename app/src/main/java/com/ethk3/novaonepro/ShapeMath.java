package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ShapeMath {

    private static final double EPS = 0.0000001d;

    private ShapeMath() {}

    public static double clamp01(double value) {
        return AppConfig.clampProbability(value);
    }

    public static double clamp100(double value) {
        return AppConfig.clampScore100(value);
    }

    public static double safeDiv(double a, double b) {
        if (b == 0.0d) {
            return 0.0d;
        }
        double v = a / b;
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }
        return v;
    }

    public static int safeWindowSize(List<Models.Draw> history, int window) {
        if (history == null || history.size() == 0) {
            return 0;
        }
        if (window <= 0 || window > history.size()) {
            return history.size();
        }
        return window;
    }

    public static List<Models.Draw> tailWindow(List<Models.Draw> history, int window) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (history == null || history.size() == 0) {
            return out;
        }

        int size = safeWindowSize(history, window);
        int start = history.size() - size;

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    public static int[] countNumbersAtPosition(
            List<Models.Draw> history, int position, int window) {
        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];

        if (!AppConfig.isValidPosition(position)) {
            return counts;
        }

        List<Models.Draw> list = tailWindow(history, window);

        for (int i = 0; i < list.size(); i++) {
            Models.Draw d = list.get(i);
            int n = d.getNumberAtPosition(position);
            if (AppConfig.isValidNumber(n)) {
                counts[n]++;
            }
        }

        return counts;
    }

    public static int totalCount(int[] counts) {
        if (counts == null) {
            return 0;
        }

        int total = 0;
        for (int i = 1; i < counts.length; i++) {
            total += counts[i];
        }
        return total;
    }

    public static double[] probabilityFromCounts(int[] counts, double smooth) {
        double[] probs = new double[AppConfig.NUMBER_COUNT + 1];

        if (counts == null || counts.length < AppConfig.NUMBER_COUNT + 1) {
            fillUniform(probs);
            return probs;
        }

        if (smooth < 0.0d) {
            smooth = 0.0d;
        }

        int total = totalCount(counts);
        double denom = total + smooth * AppConfig.NUMBER_COUNT;

        if (denom <= 0.0d) {
            fillUniform(probs);
            return probs;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            probs[n] = (counts[n] + smooth) / denom;
        }

        normalizeDistribution(probs);
        return probs;
    }

    public static double[] probabilityAtPosition(
            List<Models.Draw> history, int position, int window) {
        int[] counts = countNumbersAtPosition(history, position, window);
        return probabilityFromCounts(counts, AppConfig.SMOOTH_ALPHA);
    }

    public static void fillUniform(double[] probs) {
        if (probs == null) {
            return;
        }

        double v = 1.0d / AppConfig.NUMBER_COUNT;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER && n < probs.length; n++) {
            probs[n] = v;
        }
    }

    public static void normalizeDistribution(double[] probs) {
        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            return;
        }

        double sum = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double v = probs[n];

            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0d) {
                v = 0.0d;
            }

            probs[n] = v;
            sum += v;
        }

        if (sum <= EPS) {
            fillUniform(probs);
            return;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            probs[n] = probs[n] / sum;
        }
    }

    public static double entropyNormalized(double[] probs) {
        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            return 100.0d;
        }

        double entropy = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p = probs[n];
            if (p > EPS) {
                entropy -= p * Math.log(p);
            }
        }

        double max = Math.log(AppConfig.NUMBER_COUNT);
        if (max <= 0.0d) {
            return 100.0d;
        }

        return clamp100(entropy / max * 100.0d);
    }

    public static double topMass(double[] probs, int topK) {
        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            return 0.0d;
        }

        if (topK <= 0) {
            return 0.0d;
        }

        ArrayList<NumberScore> ranked = rankNumbers(probs);
        double sum = 0.0d;

        int limit = Math.min(topK, ranked.size());

        for (int i = 0; i < limit; i++) {
            sum += ranked.get(i).score;
        }

        return clamp01(sum);
    }

    public static double championGap(double[] probs) {
        ArrayList<NumberScore> ranked = rankNumbers(probs);
        if (ranked.size() < 2) {
            return 0.0d;
        }
        return ranked.get(0).score - ranked.get(1).score;
    }

    public static double top3ThirdGap(double[] probs) {
        ArrayList<NumberScore> ranked = rankNumbers(probs);
        if (ranked.size() < 4) {
            return 0.0d;
        }
        return ranked.get(2).score - ranked.get(3).score;
    }

    public static int[] topNumbers(double[] probs, int k) {
        if (k <= 0) {
            return new int[0];
        }

        ArrayList<NumberScore> ranked = rankNumbers(probs);
        int limit = Math.min(k, ranked.size());
        int[] out = new int[limit];

        for (int i = 0; i < limit; i++) {
            out[i] = ranked.get(i).number;
        }

        return out;
    }

    public static ArrayList<NumberScore> rankNumbers(double[] probs) {
        ArrayList<NumberScore> list = new ArrayList<NumberScore>();

        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                list.add(new NumberScore(n, 1.0d / AppConfig.NUMBER_COUNT));
            }
        } else {
            for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                list.add(new NumberScore(n, probs[n]));
            }
        }

        Collections.sort(
                list,
                new Comparator<NumberScore>() {
                    @Override
                    public int compare(NumberScore a, NumberScore b) {
                        if (a.score < b.score) {
                            return 1;
                        }
                        if (a.score > b.score) {
                            return -1;
                        }
                        return a.number - b.number;
                    }
                });

        return list;
    }

    public static int rankOfNumber(double[] probs, int number) {
        if (!AppConfig.isValidNumber(number)) {
            return 0;
        }

        ArrayList<NumberScore> ranked = rankNumbers(probs);

        for (int i = 0; i < ranked.size(); i++) {
            if (ranked.get(i).number == number) {
                return i + 1;
            }
        }

        return 0;
    }

    public static double distributionDistance(double[] a, double[] b) {
        if (a == null
                || b == null
                || a.length < AppConfig.NUMBER_COUNT + 1
                || b.length < AppConfig.NUMBER_COUNT + 1) {
            return 100.0d;
        }

        double sum = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            sum += Math.abs(a[n] - b[n]);
        }

        return clamp100(sum * 50.0d);
    }

    public static double driftRisk(
            List<Models.Draw> history, int position, int shortWindow, int longWindow) {
        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return 100.0d;
        }

        double[] shortP = probabilityAtPosition(history, position, shortWindow);
        double[] longP = probabilityAtPosition(history, position, longWindow);

        double distance = distributionDistance(shortP, longP);
        double entropyShort = entropyNormalized(shortP);
        double entropyLong = entropyNormalized(longP);

        double entropyJump = Math.abs(entropyShort - entropyLong) * 0.25d;
        double risk = distance + entropyJump;

        return clamp100(risk);
    }

    public static double slope(double earlyValue, double lateValue) {
        double s = lateValue - earlyValue;

        if (Double.isNaN(s) || Double.isInfinite(s)) {
            return 0.0d;
        }

        return s;
    }

    public static double concentrationSlope(
            List<Models.Draw> history, int position, int earlyWindow, int lateWindow, int topK) {
        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return 0.0d;
        }

        double[] earlyP = probabilityAtPosition(history, position, earlyWindow);
        double[] lateP = probabilityAtPosition(history, position, lateWindow);

        double early = topMass(earlyP, topK);
        double late = topMass(lateP, topK);

        return slope(early, late);
    }

    public static double trendLifeScore(
            double entropy,
            double top3Mass,
            double slopeShort,
            double slopeMid,
            double championGap) {
        double score = 50.0d;

        score += (100.0d - entropy) * 0.22d;
        score += top3Mass * 100.0d * 0.28d;
        score += positivePart(slopeShort) * 260.0d;
        score += positivePart(slopeMid) * 180.0d;
        score += championGap * 350.0d;

        if (slopeShort < -0.015d) {
            score -= Math.abs(slopeShort) * 260.0d;
        }

        if (slopeMid < -0.010d) {
            score -= Math.abs(slopeMid) * 180.0d;
        }

        return clamp100(score);
    }

    public static double breakDepthScore(
            double slopeShort,
            double slopeMid,
            double driftRisk,
            double entropyShort,
            double top3Drop) {
        double score = 0.0d;

        if (slopeShort < 0.0d) {
            score += Math.abs(slopeShort) * 280.0d;
        }

        if (slopeMid < 0.0d) {
            score += Math.abs(slopeMid) * 200.0d;
        }

        score += driftRisk * 0.45d;

        if (entropyShort > 82.0d) {
            score += (entropyShort - 82.0d) * 1.4d;
        }

        if (top3Drop > 0.0d) {
            score += top3Drop * 240.0d;
        }

        return clamp100(score);
    }

    public static double recoveryScore(
            double slopeShort,
            double slopeMid,
            double driftRisk,
            double breakDepth,
            double top3MassShort,
            double championGap) {
        double score = 50.0d;

        if (slopeShort > 0.0d) {
            score += slopeShort * 320.0d;
        }

        if (slopeMid > 0.0d) {
            score += slopeMid * 180.0d;
        }

        score += top3MassShort * 100.0d * 0.12d;
        score += championGap * 260.0d;
        score -= driftRisk * 0.30d;
        score -= breakDepth * 0.22d;

        return clamp100(score);
    }

    public static double hotReverseRisk(double[] shortP, double[] midP, double[] longP) {
        if (shortP == null || midP == null || longP == null) {
            return 100.0d;
        }

        ArrayList<NumberScore> shortRank = rankNumbers(shortP);
        if (shortRank.size() == 0) {
            return 100.0d;
        }

        int champion = shortRank.get(0).number;
        double shortValue = getProb(shortP, champion);
        double midValue = getProb(midP, champion);
        double longValue = getProb(longP, champion);

        double risk = 0.0d;

        if (shortValue > midValue + 0.040d) {
            risk += 22.0d;
        }

        if (shortValue > longValue + 0.055d) {
            risk += 28.0d;
        }

        double gap = championGap(shortP);
        if (gap > 0.070d) {
            risk += 18.0d;
        }

        double ent = entropyNormalized(shortP);
        if (ent < 72.0d) {
            risk += (72.0d - ent) * 0.8d;
        }

        return clamp100(risk);
    }

    public static double falseStrongRisk(double[] shortP, double[] midP, int effectiveSample) {
        if (shortP == null || midP == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        double shortTop3 = topMass(shortP, 3);
        double midTop3 = topMass(midP, 3);

        if (shortTop3 > midTop3 + 0.065d) {
            risk += 30.0d;
        }

        if (effectiveSample < AppConfig.EFFECTIVE_SAMPLE_MIN) {
            risk += 25.0d;
        }

        if (championGap(shortP) < AppConfig.CHAMPION_GAP_MIN) {
            risk += 18.0d;
        }

        return clamp100(risk);
    }

    public static double k3Probability(double[] probs, int mask) {
        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        double p = 0.0d;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            if (Models.maskContains(mask, n)) {
                p += probs[n];
            }
        }

        return clamp01(p);
    }

    public static double k3Probability(double[] probs, int n1, int n2, int n3) {
        return k3Probability(probs, Models.buildK3Mask(n1, n2, n3));
    }

    public static ArrayList<Integer> allK3Masks() {
        ArrayList<Integer> list = new ArrayList<Integer>();

        for (int a = AppConfig.MIN_NUMBER; a <= AppConfig.MAX_NUMBER - 2; a++) {
            for (int b = a + 1; b <= AppConfig.MAX_NUMBER - 1; b++) {
                for (int c = b + 1; c <= AppConfig.MAX_NUMBER; c++) {
                    list.add(Integer.valueOf(Models.buildK3Mask(a, b, c)));
                }
            }
        }

        return list;
    }

    public static double comboGap(double[] probs, int bestMask) {
        ArrayList<Integer> masks = allK3Masks();

        double best = -1.0d;
        double second = -1.0d;

        for (int i = 0; i < masks.size(); i++) {
            int mask = masks.get(i).intValue();
            double p = k3Probability(probs, mask);

            if (mask == bestMask) {
                best = p;
            } else if (p > second) {
                second = p;
            }
        }

        if (best < 0.0d) {
            best = k3Probability(probs, bestMask);
        }

        if (second < 0.0d) {
            second = 0.0d;
        }

        return best - second;
    }

    public static double historicalK3HitRate(
            List<Models.Draw> history, int position, int mask, int window) {
        if (history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)
                || !Models.isValidK3Mask(mask)) {
            return 0.0d;
        }

        List<Models.Draw> list = tailWindow(history, window);
        int total = 0;
        int hit = 0;

        for (int i = 0; i < list.size(); i++) {
            Models.Draw d = list.get(i);
            int n = d.getNumberAtPosition(position);

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            total++;

            if (Models.maskContains(mask, n)) {
                hit++;
            }
        }

        if (total <= 0) {
            return 0.0d;
        }

        return ((double) hit) / total;
    }

    public static int actualRankInK3Candidates(
            List<Models.ComboCandidate> candidates, int position, int actualNumber) {
        if (candidates == null
                || candidates.size() == 0
                || !AppConfig.isValidPosition(position)
                || !AppConfig.isValidNumber(actualNumber)) {
            return 0;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);
            if (c == null) {
                continue;
            }

            if (c.position == position && Models.maskContains(c.k3Mask, actualNumber)) {
                return i + 1;
            }
        }

        return 0;
    }

    public static double recentHitRateFromBinary(int[] hits, int window) {
        if (hits == null || hits.length == 0) {
            return 0.0d;
        }

        int size = window <= 0 || window > hits.length ? hits.length : window;
        int start = hits.length - size;

        int total = 0;
        int hit = 0;

        for (int i = start; i < hits.length; i++) {
            if (hits[i] < 0) {
                continue;
            }

            total++;

            if (hits[i] > 0) {
                hit++;
            }
        }

        return total > 0 ? ((double) hit) / total : 0.0d;
    }

    public static double weightedAverage(double[] values, double[] weights) {
        if (values == null || weights == null || values.length == 0 || weights.length == 0) {
            return 0.0d;
        }

        int len = Math.min(values.length, weights.length);
        double sum = 0.0d;
        double weightSum = 0.0d;

        for (int i = 0; i < len; i++) {
            double v = values[i];
            double w = weights[i];

            if (Double.isNaN(v) || Double.isInfinite(v)) {
                continue;
            }

            if (Double.isNaN(w) || Double.isInfinite(w) || w <= 0.0d) {
                continue;
            }

            sum += v * w;
            weightSum += w;
        }

        return weightSum > 0.0d ? sum / weightSum : 0.0d;
    }

    public static int bucketScore(double value, int step) {
        if (step <= 0) {
            step = 5;
        }

        double v = clamp100(value);
        return (int) (v / step) * step;
    }

    public static int bucketProbability(double value, int stepPercent) {
        if (stepPercent <= 0) {
            stepPercent = 2;
        }

        double v = clamp01(value) * 100.0d;
        return (int) (v / stepPercent) * stepPercent;
    }

    public static String buildShapeKey(Models.ShapeVector v) {
        if (v == null) {
            return "S_NULL";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(v.position);
        sb.append("_M").append(v.stateMode == null ? AppConfig.MODE_CHAOS : v.stateMode);
        sb.append("_TL").append(bucketScore(v.trendLife, 5));
        sb.append("_DR").append(bucketScore(v.driftRisk, 5));
        sb.append("_BD").append(bucketScore(v.breakDepth, 5));
        sb.append("_RC").append(bucketScore(v.recoveryScore, 5));
        sb.append("_CG").append(bucketProbability(v.championGap, 1));
        sb.append("_KG").append(bucketProbability(v.comboGap, 1));
        return sb.toString();
    }

    public static double positionStrength(Models.ShapeVector v) {
        if (v == null) {
            return 0.0d;
        }

        double score = 0.0d;
        score += v.trendLife * 0.28d;
        score += v.recoveryScore * 0.18d;
        score += (100.0d - v.driftRisk) * 0.20d;
        score += (100.0d - v.breakDepth) * 0.14d;
        score += v.championGap * 100.0d * 0.12d;
        score += v.comboGap * 100.0d * 0.08d;

        if (v.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            score -= (v.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.35d;
        }

        if (v.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            score -= (v.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) * 0.30d;
        }

        return clamp100(score);
    }

    public static ArrayList<PositionScore> rankPositions(List<Models.ShapeVector> vectors) {
        ArrayList<PositionScore> list = new ArrayList<PositionScore>();

        if (vectors == null) {
            return list;
        }

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v == null || !AppConfig.isValidPosition(v.position)) {
                continue;
            }

            list.add(new PositionScore(v.position, positionStrength(v), v));
        }

        Collections.sort(
                list,
                new Comparator<PositionScore>() {
                    @Override
                    public int compare(PositionScore a, PositionScore b) {
                        if (a.score < b.score) {
                            return 1;
                        }
                        if (a.score > b.score) {
                            return -1;
                        }
                        return a.position - b.position;
                    }
                });

        return list;
    }

    public static double positivePart(double value) {
        if (value > 0.0d) {
            return value;
        }
        return 0.0d;
    }

    public static double negativePartAbs(double value) {
        if (value < 0.0d) {
            return Math.abs(value);
        }
        return 0.0d;
    }

    public static double getProb(double[] probs, int number) {
        if (probs == null || !AppConfig.isValidNumber(number) || number >= probs.length) {
            return 0.0d;
        }
        return probs[number];
    }

    public static String maskShortText(int mask) {
        if (!Models.isValidK3Mask(mask)) {
            return "--/--/--";
        }
        return Models.maskToText(mask);
    }

    public static final class NumberScore {
        public int number;
        public double score;

        public NumberScore(int numberValue, double scoreValue) {
            number = numberValue;
            score = scoreValue;
        }
    }

    public static final class PositionScore {
        public int position;
        public double score;
        public Models.ShapeVector vector;

        public PositionScore(int positionValue, double scoreValue, Models.ShapeVector vectorValue) {
            position = positionValue;
            score = scoreValue;
            vector = vectorValue;
        }
    }
}
