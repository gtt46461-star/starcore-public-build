package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class MarkovEngine {

    private final DataGuard dataGuard;

    public MarkovEngine() {
        dataGuard = new DataGuard();
    }

    public MarkovResult buildForPosition(List<Models.Draw> history, int position) {
        MarkovResult result = new MarkovResult();
        result.position = position;

        if (!AppConfig.isValidPosition(position)) {
            result.usable = 0;
            result.reason = "位置非法";
            ShapeMath.fillUniform(result.finalProbs);
            return result;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() < 5) {
            result.usable = 0;
            result.reason = "历史少于5期，Markov只使用均匀兜底";
            ShapeMath.fillUniform(result.finalProbs);
            return result;
        }

        Models.Draw last = clean.get(clean.size() - 1);
        result.lastNumber = last.getNumberAtPosition(position);

        if (clean.size() >= 2) {
            Models.Draw prev = clean.get(clean.size() - 2);
            result.prevNumber = prev.getNumberAtPosition(position);
        }

        double[] windowProbs =
                ShapeMath.probabilityAtPosition(clean, position, AppConfig.WINDOW_128);
        double[] order1 = buildOrder1(clean, position, result.lastNumber, result);
        double[] order2 =
                buildOrder2(clean, position, result.prevNumber, result.lastNumber, result);

        result.order1Probs = order1;
        result.order2Probs = order2;
        result.windowProbs = windowProbs;

        blend(result);

        result.topNumbers = ShapeMath.topNumbers(result.finalProbs, AppConfig.FIXED_K);
        if (result.topNumbers != null && result.topNumbers.length >= AppConfig.FIXED_K) {
            result.topMask =
                    Models.buildK3Mask(
                            result.topNumbers[0], result.topNumbers[1], result.topNumbers[2]);
            result.topK3Probability = ShapeMath.k3Probability(result.finalProbs, result.topMask);
            result.comboGap = ShapeMath.comboGap(result.finalProbs, result.topMask);
        }

        result.championGap = ShapeMath.championGap(result.finalProbs);
        result.entropy = ShapeMath.entropyNormalized(result.finalProbs);
        result.supportScore = computeSupportScore(result);
        result.usable = result.supportScore >= 45.0d ? 1 : 0;

        if (result.usable == 1) {
            result.reason =
                    "Markov可用，一阶样本"
                            + result.order1Sample
                            + "，二阶样本"
                            + result.order2Sample
                            + "，支持度"
                            + UiFormatter.score(result.supportScore);
        } else {
            result.reason = "Markov样本或集中度不足，仅作辅助证据";
        }

        return result;
    }

    public ArrayList<MarkovResult> buildAllPositions(List<Models.Draw> history) {
        ArrayList<MarkovResult> out = new ArrayList<MarkovResult>();

        for (int position = AppConfig.MIN_POSITION;
                position <= AppConfig.MAX_POSITION;
                position++) {
            out.add(buildForPosition(history, position));
        }

        return out;
    }

    public ArrayList<Models.ShapeVector> applyToVectors(
            List<Models.ShapeVector> vectors, List<Models.Draw> history) {
        ArrayList<Models.ShapeVector> out = new ArrayList<Models.ShapeVector>();

        if (vectors == null || vectors.size() == 0) {
            return out;
        }

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector source = vectors.get(i);
            if (source == null) {
                continue;
            }

            MarkovResult mr = buildForPosition(history, source.position);
            Models.ShapeVector v = copyVector(source);

            if (mr != null) {
                copyDistribution(mr.finalProbs, v.markovProbs);
                v.markov1Support = mr.order1Strength;
                v.markov2Support = mr.order2Strength;

                if (mr.usable == 1) {
                    for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
                        v.numberProbs[n] = v.numberProbs[n] * 0.82d + mr.finalProbs[n] * 0.18d;
                    }
                    ShapeMath.normalizeDistribution(v.numberProbs);
                }
            }

            out.add(v);
        }

        return out;
    }

    public double k3MarkovProbability(List<Models.Draw> history, int position, int mask) {
        if (!Models.isValidK3Mask(mask) || !AppConfig.isValidPosition(position)) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        MarkovResult result = buildForPosition(history, position);
        return ShapeMath.k3Probability(result.finalProbs, mask);
    }

    public double k3MarkovSupport(List<Models.Draw> history, int position, int mask) {
        if (!Models.isValidK3Mask(mask) || !AppConfig.isValidPosition(position)) {
            return 0.0d;
        }

        MarkovResult result = buildForPosition(history, position);
        double p = ShapeMath.k3Probability(result.finalProbs, mask);
        double edge = p - AppConfig.RANDOM_K3_BASELINE;

        double score = 50.0d;
        score += edge * 180.0d;
        score += result.supportScore * 0.30d;
        score += result.championGap * 100.0d * 0.30d;
        score += result.comboGap * 100.0d * 0.25d;

        if (result.usable == 0) {
            score -= 12.0d;
        }

        return ShapeMath.clamp100(score);
    }

    public boolean supportsCandidate(List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            return false;
        }

        double p = k3MarkovProbability(history, candidate.position, candidate.k3Mask);
        double support = k3MarkovSupport(history, candidate.position, candidate.k3Mask);

        return p >= 0.335d && support >= 52.0d;
    }

    public void enrichCandidate(List<Models.Draw> history, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            return;
        }

        MarkovResult mr = buildForPosition(history, candidate.position);

        double markovP = ShapeMath.k3Probability(mr.finalProbs, candidate.k3Mask);
        double support = k3MarkovSupport(history, candidate.position, candidate.k3Mask);

        candidate.modelP = candidate.modelP * 0.88d + markovP * 0.12d;

        if (support >= 60.0d) {
            candidate.rawP += 0.006d;
        } else if (support < 42.0d) {
            candidate.rawP -= 0.008d;
        }

        candidate.rawP = AppConfig.clampProbability(candidate.rawP);
    }

    public String explain(MarkovResult result) {
        if (result == null) {
            return "Markov结果为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(result.position);
        sb.append(" 上期=");
        sb.append(UiFormatter.number(result.lastNumber));

        if (AppConfig.isValidNumber(result.prevNumber)) {
            sb.append(" 前期=");
            sb.append(UiFormatter.number(result.prevNumber));
        }

        sb.append(" 一阶样本=");
        sb.append(result.order1Sample);

        sb.append(" 二阶样本=");
        sb.append(result.order2Sample);

        sb.append(" TopK3=");
        sb.append(Models.maskToText(result.topMask));

        sb.append(" K3P=");
        sb.append(UiFormatter.probability(result.topK3Probability));

        sb.append(" 支持度=");
        sb.append(UiFormatter.score(result.supportScore));

        sb.append(" 可用=");
        sb.append(result.usable == 1 ? "是" : "否");

        return sb.toString();
    }

    private double[] buildOrder1(
            ArrayList<Models.Draw> history, int position, int lastNumber, MarkovResult result) {
        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];

        if (!AppConfig.isValidNumber(lastNumber)) {
            result.order1Sample = 0;
            result.order1Strength = 0.0d;
            return ShapeMath.probabilityFromCounts(counts, 2.0d);
        }

        int sample = 0;

        for (int i = 1; i < history.size(); i++) {
            Models.Draw prevDraw = history.get(i - 1);
            Models.Draw curDraw = history.get(i);

            int prevNumber = prevDraw.getNumberAtPosition(position);
            int curNumber = curDraw.getNumberAtPosition(position);

            if (!AppConfig.isValidNumber(prevNumber) || !AppConfig.isValidNumber(curNumber)) {
                continue;
            }

            if (prevNumber == lastNumber) {
                counts[curNumber]++;
                sample++;
            }
        }

        result.order1Sample = sample;
        double[] probs = ShapeMath.probabilityFromCounts(counts, sample < 8 ? 2.0d : 1.0d);
        result.order1Strength = strengthFromDistribution(probs, sample);
        return probs;
    }

    private double[] buildOrder2(
            ArrayList<Models.Draw> history,
            int position,
            int prevNumber,
            int lastNumber,
            MarkovResult result) {
        int[] counts = new int[AppConfig.NUMBER_COUNT + 1];

        if (!AppConfig.isValidNumber(prevNumber) || !AppConfig.isValidNumber(lastNumber)) {
            result.order2Sample = 0;
            result.order2Strength = 0.0d;
            return ShapeMath.probabilityFromCounts(counts, 3.0d);
        }

        int sample = 0;

        for (int i = 2; i < history.size(); i++) {
            Models.Draw d0 = history.get(i - 2);
            Models.Draw d1 = history.get(i - 1);
            Models.Draw d2 = history.get(i);

            int a = d0.getNumberAtPosition(position);
            int b = d1.getNumberAtPosition(position);
            int c = d2.getNumberAtPosition(position);

            if (!AppConfig.isValidNumber(a)
                    || !AppConfig.isValidNumber(b)
                    || !AppConfig.isValidNumber(c)) {
                continue;
            }

            if (a == prevNumber && b == lastNumber) {
                counts[c]++;
                sample++;
            }
        }

        result.order2Sample = sample;
        double[] probs = ShapeMath.probabilityFromCounts(counts, sample < 5 ? 3.0d : 1.5d);
        result.order2Strength = strengthFromDistribution(probs, sample);
        return probs;
    }

    private void blend(MarkovResult r) {
        if (r == null) {
            return;
        }

        if (r.finalProbs == null || r.finalProbs.length < AppConfig.NUMBER_COUNT + 1) {
            r.finalProbs = new double[AppConfig.NUMBER_COUNT + 1];
        }

        double w1 = weightBySample(r.order1Sample, 6, 35) * 0.40d;
        double w2 = weightBySample(r.order2Sample, 4, 20) * 0.32d;
        double ww = 0.28d;

        if (r.order1Sample < 4) {
            w1 *= 0.35d;
        }

        if (r.order2Sample < 3) {
            w2 *= 0.25d;
        }

        double sum = w1 + w2 + ww;
        if (sum <= 0.0d) {
            ShapeMath.fillUniform(r.finalProbs);
            return;
        }

        w1 = w1 / sum;
        w2 = w2 / sum;
        ww = ww / sum;

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER; n++) {
            double p1 = getProb(r.order1Probs, n);
            double p2 = getProb(r.order2Probs, n);
            double pw = getProb(r.windowProbs, n);

            r.finalProbs[n] = p1 * w1 + p2 * w2 + pw * ww;
        }

        ShapeMath.normalizeDistribution(r.finalProbs);
    }

    private double computeSupportScore(MarkovResult r) {
        if (r == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += Math.min(35.0d, r.order1Sample * 2.2d);
        score += Math.min(25.0d, r.order2Sample * 3.0d);
        score += r.order1Strength * 0.18d;
        score += r.order2Strength * 0.14d;

        if (r.topK3Probability > AppConfig.RANDOM_K3_BASELINE) {
            score += (r.topK3Probability - AppConfig.RANDOM_K3_BASELINE) * 120.0d;
        }

        score += r.championGap * 100.0d * 0.20d;
        score += r.comboGap * 100.0d * 0.18d;

        if (r.entropy > 92.0d) {
            score -= (r.entropy - 92.0d) * 1.2d;
        }

        return ShapeMath.clamp100(score);
    }

    private double strengthFromDistribution(double[] probs, int sample) {
        if (probs == null || probs.length < AppConfig.NUMBER_COUNT + 1) {
            return 0.0d;
        }

        double top3 = ShapeMath.topMass(probs, 3);
        double entropy = ShapeMath.entropyNormalized(probs);
        double gap = ShapeMath.championGap(probs);

        double score = 0.0d;
        score += top3 * 100.0d * 0.45d;
        score += (100.0d - entropy) * 0.25d;
        score += gap * 100.0d * 0.30d;
        score += Math.min(18.0d, sample * 1.2d);

        return ShapeMath.clamp100(score);
    }

    private double weightBySample(int sample, int minStrong, int fullStrong) {
        if (sample <= 0) {
            return 0.0d;
        }

        if (sample <= minStrong) {
            return 0.25d;
        }

        if (sample >= fullStrong) {
            return 1.0d;
        }

        double x = ((double) (sample - minStrong)) / (double) (fullStrong - minStrong);
        return 0.25d + x * 0.75d;
    }

    private double getProb(double[] probs, int number) {
        if (probs == null || !AppConfig.isValidNumber(number) || number >= probs.length) {
            return 0.0d;
        }
        return probs[number];
    }

    private Models.ShapeVector copyVector(Models.ShapeVector source) {
        Models.ShapeVector v = source.copyLight();

        if (source.numberProbs != null) {
            v.numberProbs = new double[source.numberProbs.length];
            for (int i = 0; i < source.numberProbs.length; i++) {
                v.numberProbs[i] = source.numberProbs[i];
            }
        }

        if (source.markovProbs != null) {
            v.markovProbs = new double[source.markovProbs.length];
            for (int i = 0; i < source.markovProbs.length; i++) {
                v.markovProbs[i] = source.markovProbs[i];
            }
        }

        return v;
    }

    private void copyDistribution(double[] from, double[] to) {
        if (to == null) {
            return;
        }

        if (from == null || from.length < AppConfig.NUMBER_COUNT + 1) {
            ShapeMath.fillUniform(to);
            return;
        }

        for (int n = AppConfig.MIN_NUMBER; n <= AppConfig.MAX_NUMBER && n < to.length; n++) {
            to[n] = from[n];
        }

        ShapeMath.normalizeDistribution(to);
    }

    public static final class MarkovResult {
        public int position;
        public int prevNumber;
        public int lastNumber;

        public int order1Sample;
        public int order2Sample;

        public double order1Strength;
        public double order2Strength;
        public double supportScore;

        public double[] order1Probs;
        public double[] order2Probs;
        public double[] windowProbs;
        public double[] finalProbs;

        public int[] topNumbers;
        public int topMask;
        public double topK3Probability;
        public double championGap;
        public double comboGap;
        public double entropy;

        public int usable;
        public String reason;

        public MarkovResult() {
            position = 0;
            prevNumber = 0;
            lastNumber = 0;
            order1Sample = 0;
            order2Sample = 0;
            order1Strength = 0.0d;
            order2Strength = 0.0d;
            supportScore = 0.0d;

            order1Probs = new double[AppConfig.NUMBER_COUNT + 1];
            order2Probs = new double[AppConfig.NUMBER_COUNT + 1];
            windowProbs = new double[AppConfig.NUMBER_COUNT + 1];
            finalProbs = new double[AppConfig.NUMBER_COUNT + 1];

            topNumbers = new int[0];
            topMask = 0;
            topK3Probability = AppConfig.RANDOM_K3_BASELINE;
            championGap = 0.0d;
            comboGap = 0.0d;
            entropy = 100.0d;
            usable = 0;
            reason = "";
        }
    }
}
