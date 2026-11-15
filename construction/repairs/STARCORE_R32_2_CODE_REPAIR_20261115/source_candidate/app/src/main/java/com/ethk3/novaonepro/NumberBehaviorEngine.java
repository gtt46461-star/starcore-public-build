package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * NumberBehaviorEngine.java
 *
 * <p>号码行为分析引擎。
 *
 * <p>负责分析每个位置 01~10 号码的： 1. 热度 2. 冷度 3. 遗漏 4. 周期 5. 周期稳定性 6. 加速 7. 衰减 8. 反杀风险 9. 回补机会 10. 稳定性 11.
 * 波动性 12. 置信度 13. 综合号码分
 *
 * <p>本文件是独立满血版，不依赖其他项目文件。 后续可由 DeepFeatureEngine、CandidateEvidenceEngine、AiEnsembleJudgeEngine 调用。
 *
 * <p>Java 7 / Android API21+ / 无第三方库。
 */
public final class NumberBehaviorEngine {

    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_MIN = 1;
    public static final int NUMBER_MAX = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int K3 = 3;

    public static final int WINDOW_8 = 8;
    public static final int WINDOW_16 = 16;
    public static final int WINDOW_32 = 32;
    public static final int WINDOW_64 = 64;
    public static final int WINDOW_160 = 160;
    public static final int WINDOW_500 = 500;

    public static final String SIGNAL_STRONG_HOT = "STRONG_HOT";
    public static final String SIGNAL_STABLE_HOT = "STABLE_HOT";
    public static final String SIGNAL_COLD_RETURN = "COLD_RETURN";
    public static final String SIGNAL_CYCLE_NEAR = "CYCLE_NEAR";
    public static final String SIGNAL_ACCELERATING = "ACCELERATING";
    public static final String SIGNAL_DECAYING = "DECAYING";
    public static final String SIGNAL_REVERSE_RISK = "REVERSE_RISK";
    public static final String SIGNAL_WEAK = "WEAK";
    public static final String SIGNAL_NOISY = "NOISY";
    public static final String SIGNAL_BALANCED = "BALANCED";

    private static final double EPS = 0.000001D;
    public static final String B06_CONTRACT_VERSION =
            "STARCORE_TIANSHU_B06_NUMBER_BEHAVIOR_EVIDENCE_V1";
    public static final String B06_OWNER = "NUMBER_BEHAVIOR_EVIDENCE_ONLY";

    public NumberBehaviorEngine() {}

    /** 分析全部 10 个位置、每个位置 10 个号码。 */
    public NumberBehaviorResult analyze(List<DrawRecord> inputDraws, String baseIssue) {
        NumberBehaviorResult result = new NumberBehaviorResult();
        result.baseIssue = safe(baseIssue);

        List<DrawRecord> draws = normalizeDraws(inputDraws);
        result.drawCount = draws.size();

        if (draws.size() == 0) {
            result.summaryText = "号码行为：无历史开奖，无法分析";
            result.b06Evidence = buildB06Evidence(result);
            return result;
        }

        if (result.baseIssue.length() == 0) {
            result.baseIssue = draws.get(draws.size() - 1).issue;
        }

        int p;
        for (p = 1; p <= POSITION_COUNT; p++) {
            PositionNumberBehavior pb = analyzePosition(draws, p);
            result.positionBehaviors[p - 1] = pb;

            int n;
            for (n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                result.numberSignals[p - 1][n - 1] = pb.numberSignals[n - 1];
            }
        }

        result.bestPosition = chooseBestPosition(result.positionBehaviors);
        result.bestNumbers = suggestTopNumbers(result, result.bestPosition, K3);
        result.globalHotPressure = calculateGlobalHotPressure(result.positionBehaviors);
        result.globalColdReturnPressure =
                calculateGlobalColdReturnPressure(result.positionBehaviors);
        result.globalReverseRisk = calculateGlobalReverseRisk(result.positionBehaviors);
        result.globalNoise = calculateGlobalNoise(result.positionBehaviors);
        result.b06Evidence = buildB06Evidence(result);
        result.summaryText = buildResultSummary(result) + "；" + result.b06Evidence.summaryText;

        return result;
    }

    /** 分析某一个位置 01~10 的号码行为。 */
    public PositionNumberBehavior analyzePosition(List<DrawRecord> draws, int position) {
        PositionNumberBehavior pb = new PositionNumberBehavior();
        pb.position = position;

        if (!isValidPosition(position)) {
            pb.summaryText = "位置非法";
            return pb;
        }

        if (draws == null || draws.size() == 0) {
            pb.summaryText = "无样本";
            return pb;
        }

        pb.sampleCount = draws.size();

        int n;
        for (n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            pb.numberSignals[n - 1] = analyzeNumber(draws, position, n);
        }

        pb.topNumbers = chooseTopNumbers(pb, K3);
        pb.top5Numbers = chooseTopNumbers(pb, 5);
        pb.positionHotPressure = calculatePositionHotPressure(pb.numberSignals);
        pb.positionColdReturnPressure = calculatePositionColdReturnPressure(pb.numberSignals);
        pb.positionReverseRisk = calculatePositionReverseRisk(pb.numberSignals);
        pb.positionCycleSupport = calculatePositionCycleSupport(pb.numberSignals);
        pb.positionNoise = calculatePositionNoise(pb.numberSignals);
        pb.positionReliability = calculatePositionReliability(pb.numberSignals);
        pb.championGap = calculateChampionGap(pb.numberSignals);
        pb.summaryText = buildPositionSummary(pb);

        return pb;
    }

    /** 分析单个号码。 */
    public NumberSignal analyzeNumber(List<DrawRecord> draws, int position, int number) {
        NumberSignal s = new NumberSignal();
        s.position = position;
        s.number = number;

        if (!isValidPosition(position) || !isValidNumber(number)) {
            s.signalType = SIGNAL_WEAK;
            s.summaryText = "位置或号码非法";
            return s;
        }

        if (draws == null || draws.size() == 0) {
            s.signalType = SIGNAL_WEAK;
            s.summaryText = "无样本";
            return s;
        }

        s.sampleCount = draws.size();

        s.freq8 = frequency(draws, position, number, WINDOW_8);
        s.freq16 = frequency(draws, position, number, WINDOW_16);
        s.freq32 = frequency(draws, position, number, WINDOW_32);
        s.freq64 = frequency(draws, position, number, WINDOW_64);
        s.freq160 = frequency(draws, position, number, WINDOW_160);
        s.freq500 = frequency(draws, position, number, WINDOW_500);

        s.lastGap = calculateLastGap(draws, position, number);
        s.avgGap = calculateAverageGap(draws, position, number, WINDOW_500);
        s.maxGap = calculateMaxGap(draws, position, number, WINDOW_500);
        s.gapVariance = calculateGapVariance(draws, position, number, WINDOW_500, s.avgGap);
        s.gapPressure = calculateGapPressure(s.lastGap, s.avgGap, s.maxGap);

        s.hotScore = calculateHotScore(s);
        s.coldScore = calculateColdScore(s);
        s.accelerationScore = calculateAccelerationScore(s);
        s.decayScore = calculateDecayScore(s);
        s.cycleScore = calculateCycleScore(s.lastGap, s.avgGap, s.gapVariance);
        s.cycleStable = calculateCycleStable(s.avgGap, s.gapVariance);
        s.nearCyclePoint = s.cycleScore >= 0.58D;
        s.returnScore = calculateReturnScore(s);
        s.reverseRisk = calculateReverseRisk(draws, position, number, s);
        s.overheatScore = calculateOverheatScore(draws, position, number, s);
        s.stabilityScore = calculateStabilityScore(s);
        s.volatilityScore = clamp(s.gapVariance / 30.0D, 0.0D, 1.0D);
        s.noiseScore = calculateNoiseScore(s);
        s.confidence = calculateConfidence(s);
        s.behaviorScore = calculateBehaviorScore(s);
        s.signalType = detectSignalType(s);
        s.summaryText = buildNumberSummary(s);

        return s;
    }

    /** 对 K3 号码组合进行行为评分。 */
    public K3BehaviorScore scoreK3(
            NumberBehaviorResult result, int position, int n1, int n2, int n3) {
        K3BehaviorScore score = new K3BehaviorScore();
        score.position = position;
        score.numbers = new int[] {n1, n2, n3};

        if (result == null) {
            score.valid = false;
            score.reasonText = "号码行为结果为空";
            return score;
        }

        if (!isValidPosition(position)) {
            score.valid = false;
            score.reasonText = "位置非法";
            return score;
        }

        if (!isValidK3(n1, n2, n3)) {
            score.valid = false;
            score.reasonText = "K3 号码非法或重复";
            return score;
        }

        NumberSignal a = result.numberSignals[position - 1][n1 - 1];
        NumberSignal b = result.numberSignals[position - 1][n2 - 1];
        NumberSignal c = result.numberSignals[position - 1][n3 - 1];

        if (a == null || b == null || c == null) {
            score.valid = false;
            score.reasonText = "号码行为信号缺失";
            return score;
        }

        score.valid = true;
        score.avgBehaviorScore = average(a.behaviorScore, b.behaviorScore, c.behaviorScore);
        score.minBehaviorScore =
                Math.min(a.behaviorScore, Math.min(b.behaviorScore, c.behaviorScore));
        score.maxBehaviorScore =
                Math.max(a.behaviorScore, Math.max(b.behaviorScore, c.behaviorScore));
        score.balanceScore =
                1.0D - clamp(score.maxBehaviorScore - score.minBehaviorScore, 0.0D, 1.0D);
        score.hotScore = average(a.hotScore, b.hotScore, c.hotScore);
        score.cycleScore = average(a.cycleScore, b.cycleScore, c.cycleScore);
        score.returnScore = average(a.returnScore, b.returnScore, c.returnScore);
        score.confidence = average(a.confidence, b.confidence, c.confidence);
        score.reverseRisk = average(a.reverseRisk, b.reverseRisk, c.reverseRisk);
        score.noiseRisk = average(a.noiseScore, b.noiseScore, c.noiseScore);
        score.weakestNumber = weakestNumber(a, b, c);
        score.weakestNumberScore = score.minBehaviorScore;
        score.comboQuality = calculateK3ComboQuality(score, a, b, c);
        score.riskPenalty = calculateK3RiskPenalty(score, a, b, c);
        score.finalScore = clamp(score.comboQuality - score.riskPenalty, 0.0D, 1.0D);
        score.layerText = buildK3LayerText(score);
        score.reasonText = buildK3ReasonText(score, a, b, c);

        return score;
    }

    public int[] suggestTopNumbers(NumberBehaviorResult result, int position, int k) {
        if (result == null || !isValidPosition(position)) {
            return defaultNumbers(k);
        }

        PositionNumberBehavior pb = result.positionBehaviors[position - 1];
        if (pb == null) {
            return defaultNumbers(k);
        }

        return chooseTopNumbers(pb, k);
    }

    public boolean isStrongNumber(NumberSignal signal) {
        if (signal == null) {
            return false;
        }
        return signal.behaviorScore >= 0.62D
                && signal.confidence >= 0.50D
                && signal.reverseRisk <= 0.58D
                && signal.noiseScore <= 0.62D;
    }

    public boolean isRiskNumber(NumberSignal signal) {
        if (signal == null) {
            return true;
        }
        return signal.reverseRisk >= 0.70D
                || signal.noiseScore >= 0.75D
                || signal.decayScore >= 0.72D
                || signal.confidence <= 0.20D;
    }

    private double frequency(List<DrawRecord> draws, int position, int number, int window) {
        if (draws == null || draws.size() == 0) {
            return 0.0D;
        }

        int start = draws.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int hit = 0;
        int i;
        for (i = start; i < draws.size(); i++) {
            int actual = getNumberAt(draws.get(i), position);
            if (isValidNumber(actual)) {
                total++;
                if (actual == number) {
                    hit++;
                }
            }
        }

        if (total <= 0) {
            return 0.0D;
        }

        return ((double) hit) / ((double) total);
    }

    private int calculateLastGap(List<DrawRecord> draws, int position, int number) {
        if (draws == null || draws.size() == 0) {
            return -1;
        }

        int gap = 0;
        int i;
        for (i = draws.size() - 1; i >= 0; i--) {
            int actual = getNumberAt(draws.get(i), position);
            if (actual == number) {
                return gap;
            }
            gap++;
        }

        return gap;
    }

    private double calculateAverageGap(
            List<DrawRecord> draws, int position, int number, int window) {
        List<Integer> gaps = collectGaps(draws, position, number, window);
        if (gaps.size() == 0) {
            return 10.0D;
        }

        double sum = 0.0D;
        int i;
        for (i = 0; i < gaps.size(); i++) {
            sum += gaps.get(i).intValue();
        }
        return sum / gaps.size();
    }

    private int calculateMaxGap(List<DrawRecord> draws, int position, int number, int window) {
        List<Integer> gaps = collectGaps(draws, position, number, window);
        int max = 0;
        int i;
        for (i = 0; i < gaps.size(); i++) {
            int v = gaps.get(i).intValue();
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    private double calculateGapVariance(
            List<DrawRecord> draws, int position, int number, int window, double avgGap) {
        List<Integer> gaps = collectGaps(draws, position, number, window);
        if (gaps.size() <= 1) {
            return 0.0D;
        }

        double sum = 0.0D;
        int i;
        for (i = 0; i < gaps.size(); i++) {
            double d = gaps.get(i).intValue() - avgGap;
            sum += d * d;
        }
        return sum / gaps.size();
    }

    private List<Integer> collectGaps(
            List<DrawRecord> draws, int position, int number, int window) {
        ArrayList<Integer> gaps = new ArrayList<Integer>();
        if (draws == null || draws.size() == 0) {
            return gaps;
        }

        int start = draws.size() - window;
        if (start < 0) {
            start = 0;
        }

        int last = -1;
        int i;
        for (i = start; i < draws.size(); i++) {
            int actual = getNumberAt(draws.get(i), position);
            if (actual == number) {
                if (last >= 0) {
                    gaps.add(Integer.valueOf(i - last));
                }
                last = i;
            }
        }
        return gaps;
    }

    private double calculateHotScore(NumberSignal s) {
        double shortHot = normalize(s.freq16, 0.10D, 0.25D);
        double midHot = normalize(s.freq64, 0.10D, 0.20D);
        double longHot = normalize(s.freq160, 0.10D, 0.18D);
        double recentHit = normalize(s.freq8, 0.10D, 0.30D);
        return clamp(
                recentHit * 0.18D + shortHot * 0.34D + midHot * 0.32D + longHot * 0.16D,
                0.0D,
                1.0D);
    }

    private double calculateColdScore(NumberSignal s) {
        double lowFreq = 1.0D - normalize(s.freq64, 0.04D, 0.16D);
        double gapCold = normalize(s.lastGap, 5.0D, 25.0D);
        double longLow = 1.0D - normalize(s.freq160, 0.06D, 0.15D);
        return clamp(lowFreq * 0.42D + gapCold * 0.42D + longLow * 0.16D, 0.0D, 1.0D);
    }

    private double calculateAccelerationScore(NumberSignal s) {
        double shortBoost = positive(s.freq16 - s.freq64) * 6.0D;
        double midBoost = positive(s.freq32 - s.freq160) * 4.5D;
        double latestBoost = positive(s.freq8 - s.freq32) * 4.0D;
        return clamp(shortBoost * 0.38D + midBoost * 0.38D + latestBoost * 0.24D, 0.0D, 1.0D);
    }

    private double calculateDecayScore(NumberSignal s) {
        double midDecay = positive(s.freq160 - s.freq32) * 5.0D;
        double shortDecay = positive(s.freq64 - s.freq16) * 5.0D;
        double latestDecay = positive(s.freq32 - s.freq8) * 4.0D;
        return clamp(midDecay * 0.36D + shortDecay * 0.40D + latestDecay * 0.24D, 0.0D, 1.0D);
    }

    private double calculateGapPressure(int lastGap, double avgGap, int maxGap) {
        if (lastGap < 0) {
            return 0.0D;
        }

        double avg = avgGap <= EPS ? 10.0D : avgGap;
        double p1 = normalize(lastGap, avg * 0.55D, avg * 1.30D);
        double p2 = maxGap <= 0 ? 0.0D : normalize(lastGap, maxGap * 0.28D, maxGap * 0.88D);
        return clamp(p1 * 0.70D + p2 * 0.30D, 0.0D, 1.0D);
    }

    private double calculateCycleScore(int lastGap, double avgGap, double gapVariance) {
        if (lastGap < 0 || avgGap <= EPS) {
            return 0.0D;
        }

        double diff = Math.abs(lastGap - avgGap);
        double closeness = 1.0D - clamp(diff / Math.max(2.0D, avgGap), 0.0D, 1.0D);
        double stable = 1.0D - clamp(gapVariance / 25.0D, 0.0D, 1.0D);
        return clamp(closeness * 0.64D + stable * 0.36D, 0.0D, 1.0D);
    }

    private double calculateCycleStable(double avgGap, double gapVariance) {
        if (avgGap <= EPS) {
            return 0.0D;
        }
        double relative = gapVariance / Math.max(1.0D, avgGap * avgGap);
        return clamp(1.0D - relative, 0.0D, 1.0D);
    }

    private double calculateReturnScore(NumberSignal s) {
        double coldReturn = s.coldScore * s.gapPressure;
        double cycleReturn = s.cycleScore;
        double notDecay = 1.0D - s.decayScore;
        double notOverheat = 1.0D - s.overheatScore;
        return clamp(
                coldReturn * 0.34D + cycleReturn * 0.34D + notDecay * 0.18D + notOverheat * 0.14D,
                0.0D,
                1.0D);
    }

    private double calculateReverseRisk(
            List<DrawRecord> draws, int position, int number, NumberSignal s) {
        int recentUse = countRecentHits(draws, position, number, 6);
        int recentUse12 = countRecentHits(draws, position, number, 12);
        double overuse6 = normalize(recentUse, 2.0D, 4.0D);
        double overuse12 = normalize(recentUse12, 3.0D, 6.0D);
        double hotDecay = s.hotScore * s.decayScore;
        double overHot = s.overheatScore;
        return clamp(
                overuse6 * 0.28D + overuse12 * 0.22D + hotDecay * 0.25D + overHot * 0.25D,
                0.0D,
                1.0D);
    }

    private double calculateOverheatScore(
            List<DrawRecord> draws, int position, int number, NumberSignal s) {
        int recent6 = countRecentHits(draws, position, number, 6);
        int recent10 = countRecentHits(draws, position, number, 10);
        double use6 = normalize(recent6, 2.0D, 4.0D);
        double use10 = normalize(recent10, 3.0D, 6.0D);
        double tooHot = normalize(s.freq16, 0.18D, 0.35D);
        return clamp(use6 * 0.36D + use10 * 0.30D + tooHot * 0.34D, 0.0D, 1.0D);
    }

    private int countRecentHits(List<DrawRecord> draws, int position, int number, int window) {
        if (draws == null || draws.size() == 0) {
            return 0;
        }
        int start = draws.size() - window;
        if (start < 0) {
            start = 0;
        }
        int count = 0;
        int i;
        for (i = start; i < draws.size(); i++) {
            if (getNumberAt(draws.get(i), position) == number) {
                count++;
            }
        }
        return count;
    }

    private double calculateStabilityScore(NumberSignal s) {
        double freqStable = 1.0D - clamp(Math.abs(s.freq32 - s.freq160) * 5.0D, 0.0D, 1.0D);
        double gapStable = s.cycleStable;
        double notReverse = 1.0D - s.reverseRisk;
        double notNoise = 1.0D - s.volatilityScore;
        return clamp(
                freqStable * 0.32D + gapStable * 0.30D + notReverse * 0.20D + notNoise * 0.18D,
                0.0D,
                1.0D);
    }

    private double calculateNoiseScore(NumberSignal s) {
        double freqJump = clamp(Math.abs(s.freq8 - s.freq64) * 4.5D, 0.0D, 1.0D);
        double gapNoise = clamp(s.gapVariance / 35.0D, 0.0D, 1.0D);
        double conflict = calculateConflictScore(s);
        return clamp(freqJump * 0.30D + gapNoise * 0.34D + conflict * 0.36D, 0.0D, 1.0D);
    }

    private double calculateConflictScore(NumberSignal s) {
        double hotColdConflict = s.hotScore * s.coldScore;
        double accelDecayConflict = s.accelerationScore * s.decayScore;
        double returnReverseConflict = s.returnScore * s.reverseRisk;
        return clamp(
                hotColdConflict * 0.32D
                        + accelDecayConflict * 0.36D
                        + returnReverseConflict * 0.32D,
                0.0D,
                1.0D);
    }

    private double calculateConfidence(NumberSignal s) {
        double sample = normalize(s.sampleCount, 30.0D, 160.0D);
        double stable = s.stabilityScore;
        double cycle = s.cycleStable;
        double notNoise = 1.0D - s.noiseScore;
        return clamp(
                sample * 0.28D + stable * 0.34D + cycle * 0.18D + notNoise * 0.20D, 0.0D, 1.0D);
    }

    private double calculateBehaviorScore(NumberSignal s) {
        double score = 0.0D;
        score += s.hotScore * 0.20D;
        score += s.accelerationScore * 0.14D;
        score += s.cycleScore * 0.18D;
        score += s.returnScore * 0.16D;
        score += s.stabilityScore * 0.16D;
        score += s.confidence * 0.12D;
        score += s.gapPressure * 0.04D;

        double risk = s.reverseRisk * 0.14D + s.decayScore * 0.09D + s.noiseScore * 0.08D;
        return clamp(score - risk, 0.0D, 1.0D);
    }

    private String detectSignalType(NumberSignal s) {
        if (s == null) {
            return SIGNAL_WEAK;
        }
        if (s.noiseScore >= 0.74D) {
            return SIGNAL_NOISY;
        }
        if (s.reverseRisk >= 0.70D || s.overheatScore >= 0.72D) {
            return SIGNAL_REVERSE_RISK;
        }
        if (s.decayScore >= 0.68D && s.accelerationScore <= 0.30D) {
            return SIGNAL_DECAYING;
        }
        if (s.accelerationScore >= 0.62D && s.hotScore >= 0.45D) {
            return SIGNAL_ACCELERATING;
        }
        if (s.returnScore >= 0.60D && s.coldScore >= 0.45D) {
            return SIGNAL_COLD_RETURN;
        }
        if (s.cycleScore >= 0.62D && s.cycleStable >= 0.48D) {
            return SIGNAL_CYCLE_NEAR;
        }
        if (s.hotScore >= 0.68D && s.stabilityScore >= 0.52D) {
            return SIGNAL_STRONG_HOT;
        }
        if (s.hotScore >= 0.50D && s.stabilityScore >= 0.58D) {
            return SIGNAL_STABLE_HOT;
        }
        if (s.behaviorScore >= 0.48D && s.confidence >= 0.40D) {
            return SIGNAL_BALANCED;
        }
        return SIGNAL_WEAK;
    }

    private int[] chooseTopNumbers(PositionNumberBehavior pb, int k) {
        int realK = k <= 0 ? K3 : k;
        if (realK > NUMBER_COUNT) {
            realK = NUMBER_COUNT;
        }

        int[] out = new int[realK];
        if (pb == null || pb.numberSignals == null) {
            return defaultNumbers(realK);
        }

        ArrayList<NumberSignal> list = new ArrayList<NumberSignal>();
        int i;
        for (i = 0; i < pb.numberSignals.length; i++) {
            NumberSignal s = pb.numberSignals[i];
            if (s != null) {
                list.add(s);
            }
        }

        Collections.sort(
                list,
                new Comparator<NumberSignal>() {
                    public int compare(NumberSignal a, NumberSignal b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (a.behaviorScore < b.behaviorScore) {
                            return 1;
                        }
                        if (a.behaviorScore > b.behaviorScore) {
                            return -1;
                        }
                        if (a.confidence < b.confidence) {
                            return 1;
                        }
                        if (a.confidence > b.confidence) {
                            return -1;
                        }
                        return a.number - b.number;
                    }
                });

        for (i = 0; i < realK; i++) {
            if (i < list.size()) {
                out[i] = list.get(i).number;
            } else {
                out[i] = i + 1;
            }
        }
        return out;
    }

    private int chooseBestPosition(PositionNumberBehavior[] positions) {
        if (positions == null || positions.length == 0) {
            return 1;
        }

        int best = 1;
        double bestScore = -999.0D;
        int i;
        for (i = 0; i < positions.length; i++) {
            PositionNumberBehavior p = positions[i];
            if (p == null) {
                continue;
            }
            double score =
                    p.positionReliability * 0.36D
                            + p.positionCycleSupport * 0.22D
                            + (1.0D - p.positionReverseRisk) * 0.18D
                            + (1.0D - p.positionNoise) * 0.14D
                            + p.championGap * 0.10D;
            if (score > bestScore) {
                bestScore = score;
                best = p.position;
            }
        }
        return isValidPosition(best) ? best : 1;
    }

    private double calculatePositionHotPressure(NumberSignal[] signals) {
        return averageTop(signals, "hot", 3);
    }

    private double calculatePositionColdReturnPressure(NumberSignal[] signals) {
        return averageTop(signals, "return", 3);
    }

    private double calculatePositionReverseRisk(NumberSignal[] signals) {
        return averageTop(signals, "reverse", 3);
    }

    private double calculatePositionCycleSupport(NumberSignal[] signals) {
        return averageTop(signals, "cycle", 3);
    }

    private double calculatePositionNoise(NumberSignal[] signals) {
        double sum = 0.0D;
        int count = 0;
        int i;
        for (i = 0; signals != null && i < signals.length; i++) {
            if (signals[i] != null) {
                sum += signals[i].noiseScore;
                count++;
            }
        }
        return count <= 0 ? 0.0D : clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculatePositionReliability(NumberSignal[] signals) {
        double topScore = averageTop(signals, "score", 3);
        double topConfidence = averageTop(signals, "confidence", 3);
        double reverse = averageTop(signals, "reverse", 3);
        double noise = calculatePositionNoise(signals);
        return clamp(
                topScore * 0.42D
                        + topConfidence * 0.30D
                        + (1.0D - reverse) * 0.16D
                        + (1.0D - noise) * 0.12D,
                0.0D,
                1.0D);
    }

    private double calculateChampionGap(NumberSignal[] signals) {
        if (signals == null || signals.length == 0) {
            return 0.0D;
        }
        double first = -1.0D;
        double second = -1.0D;
        int i;
        for (i = 0; i < signals.length; i++) {
            NumberSignal s = signals[i];
            if (s == null) {
                continue;
            }
            double v = s.behaviorScore;
            if (v > first) {
                second = first;
                first = v;
            } else if (v > second) {
                second = v;
            }
        }
        if (first < 0.0D || second < 0.0D) {
            return 0.0D;
        }
        return clamp(first - second, 0.0D, 1.0D);
    }

    private double averageTop(NumberSignal[] signals, String field, int topN) {
        if (signals == null || signals.length == 0) {
            return 0.0D;
        }
        double[] values = new double[signals.length];
        int count = 0;
        int i;
        for (i = 0; i < signals.length; i++) {
            NumberSignal s = signals[i];
            if (s == null) {
                continue;
            }
            values[count] = getField(s, field);
            count++;
        }
        if (count <= 0) {
            return 0.0D;
        }
        sortDesc(values, count);
        int limit = Math.min(topN, count);
        double sum = 0.0D;
        for (i = 0; i < limit; i++) {
            sum += values[i];
        }
        return sum / limit;
    }

    private double getField(NumberSignal s, String field) {
        if (s == null) {
            return 0.0D;
        }
        if ("hot".equals(field)) {
            return s.hotScore;
        }
        if ("return".equals(field)) {
            return s.returnScore;
        }
        if ("reverse".equals(field)) {
            return s.reverseRisk;
        }
        if ("cycle".equals(field)) {
            return s.cycleScore;
        }
        if ("confidence".equals(field)) {
            return s.confidence;
        }
        return s.behaviorScore;
    }

    private void sortDesc(double[] values, int count) {
        int i;
        int j;
        for (i = 0; i < count - 1; i++) {
            for (j = i + 1; j < count; j++) {
                if (values[j] > values[i]) {
                    double t = values[i];
                    values[i] = values[j];
                    values[j] = t;
                }
            }
        }
    }

    private double calculateGlobalHotPressure(PositionNumberBehavior[] positions) {
        return averagePositionField(positions, "hot");
    }

    private double calculateGlobalColdReturnPressure(PositionNumberBehavior[] positions) {
        return averagePositionField(positions, "coldReturn");
    }

    private double calculateGlobalReverseRisk(PositionNumberBehavior[] positions) {
        return averagePositionField(positions, "reverse");
    }

    private double calculateGlobalNoise(PositionNumberBehavior[] positions) {
        return averagePositionField(positions, "noise");
    }

    private double averagePositionField(PositionNumberBehavior[] positions, String field) {
        if (positions == null || positions.length == 0) {
            return 0.0D;
        }
        double sum = 0.0D;
        int count = 0;
        int i;
        for (i = 0; i < positions.length; i++) {
            PositionNumberBehavior p = positions[i];
            if (p == null) {
                continue;
            }
            if ("hot".equals(field)) {
                sum += p.positionHotPressure;
            } else if ("coldReturn".equals(field)) {
                sum += p.positionColdReturnPressure;
            } else if ("reverse".equals(field)) {
                sum += p.positionReverseRisk;
            } else if ("noise".equals(field)) {
                sum += p.positionNoise;
            } else {
                sum += p.positionReliability;
            }
            count++;
        }
        return count <= 0 ? 0.0D : clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateK3ComboQuality(
            K3BehaviorScore score, NumberSignal a, NumberSignal b, NumberSignal c) {
        double signalMix = calculateSignalMixScore(a, b, c);
        return clamp(
                score.avgBehaviorScore * 0.28D
                        + score.minBehaviorScore * 0.15D
                        + score.balanceScore * 0.14D
                        + score.cycleScore * 0.13D
                        + score.returnScore * 0.10D
                        + score.confidence * 0.13D
                        + signalMix * 0.07D,
                0.0D,
                1.0D);
    }

    private double calculateSignalMixScore(NumberSignal a, NumberSignal b, NumberSignal c) {
        int hot = 0;
        int cycle = 0;
        int ret = 0;
        int risk = 0;
        NumberSignal[] arr = new NumberSignal[] {a, b, c};
        int i;
        for (i = 0; i < arr.length; i++) {
            NumberSignal s = arr[i];
            if (s == null) {
                continue;
            }
            if (s.hotScore >= 0.50D) {
                hot++;
            }
            if (s.cycleScore >= 0.55D) {
                cycle++;
            }
            if (s.returnScore >= 0.55D) {
                ret++;
            }
            if (s.reverseRisk >= 0.62D || s.noiseScore >= 0.68D) {
                risk++;
            }
        }
        double diversity = 0.0D;
        if (hot > 0) {
            diversity += 0.30D;
        }
        if (cycle > 0) {
            diversity += 0.30D;
        }
        if (ret > 0) {
            diversity += 0.25D;
        }
        if (hot >= 3) {
            diversity -= 0.15D;
        }
        if (risk > 0) {
            diversity -= risk * 0.12D;
        }
        diversity += 0.15D;
        return clamp(diversity, 0.0D, 1.0D);
    }

    private double calculateK3RiskPenalty(
            K3BehaviorScore score, NumberSignal a, NumberSignal b, NumberSignal c) {
        double penalty = 0.0D;
        penalty += score.reverseRisk * 0.16D;
        penalty += score.noiseRisk * 0.13D;
        if (score.minBehaviorScore < 0.30D) {
            penalty += 0.08D;
        }
        if (score.balanceScore < 0.42D) {
            penalty += 0.06D;
        }
        if (a.overheatScore >= 0.70D || b.overheatScore >= 0.70D || c.overheatScore >= 0.70D) {
            penalty += 0.06D;
        }
        if (a.decayScore >= 0.70D || b.decayScore >= 0.70D || c.decayScore >= 0.70D) {
            penalty += 0.05D;
        }
        return clamp(penalty, 0.0D, 0.50D);
    }

    private int weakestNumber(NumberSignal a, NumberSignal b, NumberSignal c) {
        NumberSignal weak = a;
        if (b.behaviorScore < weak.behaviorScore) {
            weak = b;
        }
        if (c.behaviorScore < weak.behaviorScore) {
            weak = c;
        }
        return weak.number;
    }

    private String buildResultSummary(NumberBehaviorResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("号码行为：样本=").append(r.drawCount);
        sb.append(" | 最强位置=").append(r.bestPosition);
        sb.append(" | 推荐号码=").append(formatNumbers(r.bestNumbers));
        sb.append(" | 热压=").append(fmt(r.globalHotPressure));
        sb.append(" | 冷回补=").append(fmt(r.globalColdReturnPressure));
        sb.append(" | 反杀=").append(fmt(r.globalReverseRisk));
        sb.append(" | 噪声=").append(fmt(r.globalNoise));
        PositionNumberBehavior pb = null;
        if (isValidPosition(r.bestPosition)) {
            pb = r.positionBehaviors[r.bestPosition - 1];
        }
        if (pb != null) {
            sb.append(" | 最强说明：").append(pb.summaryText);
        }
        return sb.toString();
    }

    private String buildPositionSummary(PositionNumberBehavior pb) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(pb.position);
        sb.append(" Top3=").append(formatNumbers(pb.topNumbers));
        sb.append(" 可靠=").append(fmt(pb.positionReliability));
        sb.append(" 周期=").append(fmt(pb.positionCycleSupport));
        sb.append(" 回补=").append(fmt(pb.positionColdReturnPressure));
        sb.append(" 反杀=").append(fmt(pb.positionReverseRisk));
        sb.append(" 噪声=").append(fmt(pb.positionNoise));
        sb.append(" 冠军差=").append(fmt(pb.championGap));
        return sb.toString();
    }

    private String buildNumberSummary(NumberSignal s) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(s.position).append("-").append(two(s.number));
        sb.append(" ").append(s.signalType);
        sb.append(" 分=").append(fmt(s.behaviorScore));
        sb.append(" 热=").append(fmt(s.hotScore));
        sb.append(" 冷=").append(fmt(s.coldScore));
        sb.append(" 遗漏=").append(s.lastGap);
        sb.append(" 均遗=").append(fmtRaw(s.avgGap));
        sb.append(" 周期=").append(fmt(s.cycleScore));
        sb.append(" 回补=").append(fmt(s.returnScore));
        sb.append(" 加速=").append(fmt(s.accelerationScore));
        sb.append(" 衰减=").append(fmt(s.decayScore));
        sb.append(" 反杀=").append(fmt(s.reverseRisk));
        sb.append(" 稳定=").append(fmt(s.stabilityScore));
        sb.append(" 置信=").append(fmt(s.confidence));
        return sb.toString();
    }

    private String buildK3LayerText(K3BehaviorScore s) {
        if (s.finalScore >= 0.72D && s.confidence >= 0.60D && s.riskPenalty <= 0.18D) {
            return "A+号码组合";
        }
        if (s.finalScore >= 0.62D && s.weakestNumberScore >= 0.42D) {
            return "强号码组合";
        }
        if (s.finalScore >= 0.52D) {
            return "可观察号码组合";
        }
        if (s.riskPenalty >= 0.35D) {
            return "高风险号码组合";
        }
        return "普通号码组合";
    }

    private String buildK3ReasonText(
            K3BehaviorScore score, NumberSignal a, NumberSignal b, NumberSignal c) {
        StringBuilder sb = new StringBuilder();
        sb.append("K3行为 P").append(score.position).append(" ").append(formatNumbers(score.numbers));
        sb.append(" | ").append(score.layerText);
        sb.append(" | 总分=").append(fmt(score.finalScore));
        sb.append(" | 平均=").append(fmt(score.avgBehaviorScore));
        sb.append(" | 最弱=")
                .append(two(score.weakestNumber))
                .append("/")
                .append(fmt(score.weakestNumberScore));
        sb.append(" | 均衡=").append(fmt(score.balanceScore));
        sb.append(" | 周期=").append(fmt(score.cycleScore));
        sb.append(" | 回补=").append(fmt(score.returnScore));
        sb.append(" | 置信=").append(fmt(score.confidence));
        sb.append(" | 风险=").append(fmt(score.riskPenalty));
        sb.append(" | 号码说明：");
        sb.append(a.summaryText).append("；");
        sb.append(b.summaryText).append("；");
        sb.append(c.summaryText);
        if (score.weakestNumberScore < 0.30D) {
            sb.append(" | 警告：存在弱号凑数");
        }
        if (score.reverseRisk >= 0.62D) {
            sb.append(" | 警告：组合反杀风险偏高");
        }
        return sb.toString();
    }

    private List<DrawRecord> normalizeDraws(List<DrawRecord> input) {
        ArrayList<DrawRecord> list = new ArrayList<DrawRecord>();
        if (input == null) {
            return list;
        }

        int i;
        for (i = 0; i < input.size(); i++) {
            DrawRecord d = input.get(i);
            if (d == null || safe(d.issue).length() == 0) {
                continue;
            }
            int[] nums = parseOpenCode(d.openCode);
            if (nums == null || nums.length != POSITION_COUNT) {
                continue;
            }
            DrawRecord c = new DrawRecord();
            c.issue = safe(d.issue);
            c.openCode = formatNumbers(nums);
            c.openTimeMs = d.openTimeMs;
            list.add(c);
        }

        Collections.sort(
                list,
                new Comparator<DrawRecord>() {
                    public int compare(DrawRecord a, DrawRecord b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return -1;
                        }
                        if (b == null) {
                            return 1;
                        }
                        return compareIssue(a.issue, b.issue);
                    }
                });

        return list;
    }

    private int getNumberAt(DrawRecord draw, int position) {
        if (draw == null || !isValidPosition(position)) {
            return -1;
        }
        int[] nums = parseOpenCode(draw.openCode);
        if (nums == null || nums.length < position) {
            return -1;
        }
        return nums[position - 1];
    }

    public int[] parseOpenCode(String code) {
        String c = safe(code);
        ArrayList<Integer> nums = new ArrayList<Integer>();
        StringBuilder buf = new StringBuilder();
        int i;
        for (i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if (ch >= '0' && ch <= '9') {
                buf.append(ch);
            } else {
                flushNumber(buf, nums);
            }
        }
        flushNumber(buf, nums);

        if (nums.size() != POSITION_COUNT) {
            return null;
        }

        int[] arr = new int[POSITION_COUNT];
        for (i = 0; i < POSITION_COUNT; i++) {
            int n = nums.get(i).intValue();
            if (!isValidNumber(n)) {
                return null;
            }
            arr[i] = n;
        }
        return arr;
    }

    private void flushNumber(StringBuilder buf, ArrayList<Integer> nums) {
        if (buf == null || buf.length() == 0) {
            return;
        }
        try {
            nums.add(Integer.valueOf(Integer.parseInt(buf.toString())));
        } catch (Exception e) {
            // 忽略非法片段
        }
        buf.setLength(0);
    }

    private boolean isValidPosition(int position) {
        return position >= 1 && position <= POSITION_COUNT;
    }

    private boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private boolean isValidK3(int n1, int n2, int n3) {
        if (!isValidNumber(n1) || !isValidNumber(n2) || !isValidNumber(n3)) {
            return false;
        }
        return n1 != n2 && n1 != n3 && n2 != n3;
    }

    private int[] defaultNumbers(int k) {
        int realK = k <= 0 ? K3 : k;
        if (realK > NUMBER_COUNT) {
            realK = NUMBER_COUNT;
        }
        int[] out = new int[realK];
        int i;
        for (i = 0; i < realK; i++) {
            out[i] = i + 1;
        }
        return out;
    }

    private double average(double a, double b, double c) {
        return (a + b + c) / 3.0D;
    }

    private double normalize(double value, double low, double high) {
        if (high <= low) {
            return 0.0D;
        }
        return clamp((value - low) / (high - low), 0.0D, 1.0D);
    }

    private double positive(double value) {
        return value > 0.0D ? value : 0.0D;
    }

    private double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static int compareIssue(String a, String b) {
        String da = digitsOnly(a);
        String db = digitsOnly(b);
        if (da.length() > 0 && db.length() > 0) {
            da = stripLeadingZero(da);
            db = stripLeadingZero(db);
            if (da.length() != db.length()) {
                return da.length() < db.length() ? -1 : 1;
            }
            int cmp = da.compareTo(db);
            if (cmp < 0) {
                return -1;
            }
            if (cmp > 0) {
                return 1;
            }
            return 0;
        }
        int cmp2 = safe(a).compareTo(safe(b));
        if (cmp2 < 0) {
            return -1;
        }
        if (cmp2 > 0) {
            return 1;
        }
        return 0;
    }

    public static String digitsOnly(String value) {
        String v = safe(value);
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String stripLeadingZero(String value) {
        String v = safe(value);
        int i = 0;
        while (i < v.length() - 1 && v.charAt(i) == '0') {
            i++;
        }
        return v.substring(i);
    }

    private static String formatNumbers(int[] nums) {
        if (nums == null || nums.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i;
        for (i = 0; i < nums.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(two(nums[i]));
        }
        return sb.toString();
    }

    private static String fmt(double value) {
        int v = (int) Math.round(clampStatic(value, 0.0D, 1.0D) * 100.0D);
        return String.valueOf(v) + "%";
    }

    private static String fmtRaw(double value) {
        int v = (int) Math.round(value * 10.0D);
        return String.valueOf(v / 10.0D);
    }

    private static double clampStatic(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String two(int n) {
        if (n < 10) {
            return "0" + n;
        }
        return String.valueOf(n);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class DrawRecord {
        public String issue;
        public String openCode;
        public long openTimeMs;

        public DrawRecord() {
            this.issue = "";
            this.openCode = "";
            this.openTimeMs = 0L;
        }

        public DrawRecord(String issue, String openCode, long openTimeMs) {
            this.issue = safe(issue);
            this.openCode = safe(openCode);
            this.openTimeMs = openTimeMs;
        }
    }

    public static final class NumberBehaviorResult {
        public String baseIssue;
        public int drawCount;
        public PositionNumberBehavior[] positionBehaviors;
        public NumberSignal[][] numberSignals;
        public int bestPosition;
        public int[] bestNumbers;
        public double globalHotPressure;
        public double globalColdReturnPressure;
        public double globalReverseRisk;
        public double globalNoise;
        public String summaryText;
        public B06NumberEvidenceBundle b06Evidence;

        public NumberBehaviorResult() {
            this.baseIssue = "";
            this.drawCount = 0;
            this.positionBehaviors = new PositionNumberBehavior[POSITION_COUNT];
            this.numberSignals = new NumberSignal[POSITION_COUNT][NUMBER_COUNT];
            this.bestPosition = 1;
            this.bestNumbers = new int[] {1, 2, 3};
            this.summaryText = "";
            this.b06Evidence = new B06NumberEvidenceBundle();
        }
    }

    public static final class PositionNumberBehavior {
        public int position;
        public int sampleCount;
        public NumberSignal[] numberSignals;
        public int[] topNumbers;
        public int[] top5Numbers;
        public double positionHotPressure;
        public double positionColdReturnPressure;
        public double positionReverseRisk;
        public double positionCycleSupport;
        public double positionNoise;
        public double positionReliability;
        public double championGap;
        public String summaryText;

        public PositionNumberBehavior() {
            this.position = 0;
            this.sampleCount = 0;
            this.numberSignals = new NumberSignal[NUMBER_COUNT];
            this.topNumbers = new int[] {1, 2, 3};
            this.top5Numbers = new int[] {1, 2, 3, 4, 5};
            this.summaryText = "";
        }
    }

    public static final class NumberSignal {
        public int position;
        public int number;
        public int sampleCount;

        public double freq8;
        public double freq16;
        public double freq32;
        public double freq64;
        public double freq160;
        public double freq500;

        public int lastGap;
        public double avgGap;
        public int maxGap;
        public double gapVariance;
        public double gapPressure;

        public double hotScore;
        public double coldScore;
        public double accelerationScore;
        public double decayScore;
        public double cycleScore;
        public double cycleStable;
        public boolean nearCyclePoint;
        public double returnScore;
        public double reverseRisk;
        public double overheatScore;
        public double stabilityScore;
        public double volatilityScore;
        public double noiseScore;
        public double confidence;
        public double behaviorScore;

        public String signalType;
        public String summaryText;

        public NumberSignal() {
            this.position = 0;
            this.number = 0;
            this.sampleCount = 0;
            this.lastGap = -1;
            this.signalType = SIGNAL_WEAK;
            this.summaryText = "";
        }
    }

    public static final class K3BehaviorScore {
        public boolean valid;
        public int position;
        public int[] numbers;
        public double avgBehaviorScore;
        public double minBehaviorScore;
        public double maxBehaviorScore;
        public double weakestNumberScore;
        public int weakestNumber;
        public double balanceScore;
        public double hotScore;
        public double cycleScore;
        public double returnScore;
        public double confidence;
        public double reverseRisk;
        public double noiseRisk;
        public double comboQuality;
        public double riskPenalty;
        public double finalScore;
        public String layerText;
        public String reasonText;

        public K3BehaviorScore() {
            this.valid = false;
            this.position = 0;
            this.numbers = new int[] {0, 0, 0};
            this.weakestNumber = 0;
            this.layerText = "";
            this.reasonText = "";
        }
    }

    public B06NumberEvidenceBundle buildB06Evidence(NumberBehaviorResult result) {
        B06NumberEvidenceBundle bundle = new B06NumberEvidenceBundle();
        bundle.baseIssue = result == null ? "" : safe(result.baseIssue);
        if (result == null || result.numberSignals == null) {
            bundle.summaryText = "B06号码行为证据：结果为空，不参与概率和BET";
            bundle.fingerprint = V51ChecksumEngine.sha256(bundle.stablePayload());
            return bundle;
        }
        double support = 0.0D, counter = 0.0D, risk = 0.0D, confidence = 0.0D, freshness = 0.0D;
        int count = 0;
        for (int p = 0; p < POSITION_COUNT; p++) {
            for (int n = 0; n < NUMBER_COUNT; n++) {
                NumberSignal signal = result.numberSignals[p][n];
                if (signal == null) continue;
                B06NumberEvidencePacket packet = buildB06NumberPacket(result, signal);
                bundle.packets[p][n] = packet;
                support += packet.support;
                counter += packet.counterEvidence;
                risk += packet.risk;
                confidence += packet.confidence;
                freshness += packet.freshness;
                count++;
            }
        }
        bundle.packetCount = count;
        bundle.support = count > 0 ? support / count : 0.0D;
        bundle.counterEvidence = count > 0 ? counter / count : 1.0D;
        bundle.risk = count > 0 ? risk / count : 1.0D;
        bundle.confidence = count > 0 ? confidence / count : 0.0D;
        bundle.freshness = count > 0 ? freshness / count : 0.0D;
        bundle.sample = result.drawCount;
        bundle.diversity = clampStatic(count / 100.0D, 0.0D, 1.0D);
        bundle.evidenceReady = count == 100 && result.drawCount >= WINDOW_32 ? 1 : 0;
        bundle.summaryText =
                "B06号码行为证据：10位置×10号码="
                        + count
                        + "，支持="
                        + fmt(bundle.support)
                        + "，反证="
                        + fmt(bundle.counterEvidence)
                        + "，风险="
                        + fmt(bundle.risk)
                        + "，置信="
                        + fmt(bundle.confidence)
                        + "，所有权=EvidenceOnly";
        bundle.fingerprint = V51ChecksumEngine.sha256(bundle.stablePayload());
        return bundle;
    }

    private B06NumberEvidencePacket buildB06NumberPacket(
            NumberBehaviorResult result, NumberSignal s) {
        B06NumberEvidencePacket p = new B06NumberEvidencePacket();
        p.baseIssue = result == null ? "" : safe(result.baseIssue);
        p.position = s.position;
        p.number = s.number;
        p.signalType = safe(s.signalType);
        p.sample = s.sampleCount;
        p.support =
                clampStatic(
                        s.behaviorScore * 0.34D
                                + s.cycleScore * 0.18D
                                + s.returnScore * 0.16D
                                + s.stabilityScore * 0.18D
                                + s.confidence * 0.14D,
                        0.0D,
                        1.0D);
        p.counterEvidence =
                clampStatic(
                        s.decayScore * 0.20D
                                + s.reverseRisk * 0.30D
                                + s.overheatScore * 0.22D
                                + s.noiseScore * 0.28D,
                        0.0D,
                        1.0D);
        p.risk =
                clampStatic(
                        s.reverseRisk * 0.38D
                                + s.overheatScore * 0.22D
                                + s.noiseScore * 0.30D
                                + s.volatilityScore * 0.10D,
                        0.0D,
                        1.0D);
        p.confidence =
                clampStatic(
                        s.confidence * 0.60D
                                + Math.min(1.0D, s.sampleCount / (double) WINDOW_160) * 0.30D
                                + (1.0D - p.risk) * 0.10D,
                        0.0D,
                        1.0D);
        p.freshness = clampStatic(1.0D - Math.abs(s.freq8 - s.freq32) * 2.5D, 0.0D, 1.0D);
        p.usable = p.sample >= WINDOW_32 && p.confidence >= 0.38D ? 1 : 0;
        p.ablationGroup = "B06_NUMBER_BEHAVIOR";
        p.reason = "支持来自行为/周期/回归/稳定，反证来自衰减/反向/过热/噪声；不直接改FinalP或Gate";
        p.fingerprint = V51ChecksumEngine.sha256(p.stablePayload());
        return p;
    }

    public static final class B06NumberEvidencePacket {
        public String baseIssue = "";
        public int position = 0;
        public int number = 0;
        public String signalType = SIGNAL_WEAK;
        public double support = 0.0D;
        public double counterEvidence = 1.0D;
        public double risk = 1.0D;
        public double confidence = 0.0D;
        public int sample = 0;
        public double freshness = 0.0D;
        public int usable = 0;
        public String ablationGroup = "B06_NUMBER_BEHAVIOR";
        public String reason = "";
        public String fingerprint = "";

        public String stablePayload() {
            return B06_CONTRACT_VERSION
                    + "|"
                    + baseIssue
                    + "|"
                    + position
                    + "|"
                    + number
                    + "|"
                    + signalType
                    + "|"
                    + Double.doubleToLongBits(support)
                    + "|"
                    + Double.doubleToLongBits(counterEvidence)
                    + "|"
                    + Double.doubleToLongBits(risk)
                    + "|"
                    + Double.doubleToLongBits(confidence)
                    + "|"
                    + sample
                    + "|"
                    + Double.doubleToLongBits(freshness)
                    + "|"
                    + usable
                    + "|"
                    + ablationGroup;
        }
    }

    public static final class B06NumberEvidenceBundle {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public B06NumberEvidencePacket[][] packets =
                new B06NumberEvidencePacket[POSITION_COUNT][NUMBER_COUNT];
        public int packetCount = 0;
        public double support = 0.0D;
        public double counterEvidence = 1.0D;
        public double risk = 1.0D;
        public double confidence = 0.0D;
        public int sample = 0;
        public double freshness = 0.0D;
        public double diversity = 0.0D;
        public int evidenceReady = 0;
        public String summaryText = "";
        public String fingerprint = "";

        public String stablePayload() {
            StringBuilder sb = new StringBuilder();
            sb.append(contractVersion)
                    .append('|')
                    .append(owner)
                    .append('|')
                    .append(baseIssue)
                    .append('|')
                    .append(packetCount)
                    .append('|')
                    .append(sample)
                    .append('|')
                    .append(Double.doubleToLongBits(support))
                    .append('|')
                    .append(Double.doubleToLongBits(counterEvidence))
                    .append('|')
                    .append(Double.doubleToLongBits(risk))
                    .append('|')
                    .append(Double.doubleToLongBits(confidence))
                    .append('|')
                    .append(Double.doubleToLongBits(freshness))
                    .append('|')
                    .append(Double.doubleToLongBits(diversity))
                    .append('|')
                    .append(evidenceReady);
            for (int p = 0; p < packets.length; p++)
                for (int n = 0; n < packets[p].length; n++) {
                    B06NumberEvidencePacket packet = packets[p][n];
                    sb.append('|').append(packet == null ? "null" : packet.fingerprint);
                }
            return sb.toString();
        }
    }
}
