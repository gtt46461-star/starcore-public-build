package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DeepFeatureEngine.java
 *
 * <p>深度特征总入口。
 *
 * <p>作用： 1. 将历史开奖转换成可计算的深度特征。 2. 分析位置强度、号码行为、周期节奏、趋势状态、分布漂移。 3. 对单位置 + K3 候选进行评分。 4. 为后续
 * CandidateEvidenceEngine、AiEnsembleJudgeEngine、DecisionGateEngine 提供基础证据。
 *
 * <p>注意： - 本文件是独立满血版，内部包含真实计算逻辑。 - 后续新增
 * PatternStateEngine、NumberBehaviorEngine、PositionStrengthEngine 等专门文件后， DeepFeatureEngine
 * 可以作为总入口调度它们。
 *
 * <p>Java 7 / Android API21+ / 无第三方库。
 */
public final class DeepFeatureEngine {

    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_MIN = 1;
    public static final int NUMBER_MAX = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int K3 = 3;

    public static final int WINDOW_16 = 16;
    public static final int WINDOW_32 = 32;
    public static final int WINDOW_64 = 64;
    public static final int WINDOW_160 = 160;
    public static final int WINDOW_500 = 500;

    public static final String STATE_UNKNOWN = "UNKNOWN";
    public static final String STATE_CONFIRMED = "CONFIRMED";
    public static final String STATE_ACCELERATING = "ACCELERATING";
    public static final String STATE_RECOVERING = "RECOVERING";
    public static final String STATE_WEAKENING = "WEAKENING";
    public static final String STATE_BROKEN = "BROKEN";
    public static final String STATE_CHAOS = "CHAOS";
    public static final String STATE_REVERSAL = "REVERSAL";
    public static final String STATE_STABLE = "STABLE";
    public static final String STATE_TRANSITION = "TRANSITION";

    private static final double EPS = 0.000001D;

    public DeepFeatureEngine() {}

    /** 构建完整深度特征。 */
    public DeepFeatureSet buildFeatureSet(List<DrawRecord> inputDraws, String baseIssue) {
        DeepFeatureSet set = new DeepFeatureSet();
        set.baseIssue = safe(baseIssue);

        List<DrawRecord> draws = normalizeDraws(inputDraws);
        set.drawCount = draws.size();

        if (draws.size() == 0) {
            set.stateMode = STATE_UNKNOWN;
            set.summaryText = "深度特征：无历史开奖，无法分析";
            return set;
        }

        if (set.baseIssue.length() == 0) {
            set.baseIssue = draws.get(draws.size() - 1).issue;
        }

        int i;
        for (i = 0; i < POSITION_COUNT; i++) {
            set.positionFeatures[i] = buildPositionFeature(draws, i + 1);
        }

        for (i = 0; i < POSITION_COUNT; i++) {
            int n;
            for (n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                set.numberFeatures[i][n - 1] = buildNumberFeature(draws, i + 1, n);
            }
        }

        set.globalEntropy = calculateGlobalEntropy(draws, WINDOW_64);
        set.globalDriftRisk = calculateGlobalDriftRisk(draws);
        set.globalTrendStrength = calculateGlobalTrendStrength(set.positionFeatures);
        set.globalRecoveryScore = calculateGlobalRecoveryScore(set.positionFeatures);
        set.globalBreakRisk = calculateGlobalBreakRisk(set.positionFeatures);

        set.bestPosition = chooseBestPosition(set.positionFeatures);
        set.bestNumbers = chooseBestNumbers(set, set.bestPosition, K3);

        set.stateMode = detectStateMode(set);
        set.profileKey = buildProfileKey(set);
        set.summaryText = buildSummaryText(set);

        return set;
    }

    /** 构建某个位置的强弱特征。 */
    public PositionFeature buildPositionFeature(List<DrawRecord> draws, int position) {
        PositionFeature f = new PositionFeature();
        f.position = position;

        if (!isValidPosition(position)) {
            f.summaryText = "位置非法";
            return f;
        }

        int total = draws == null ? 0 : draws.size();
        f.sampleCount = total;

        if (total == 0) {
            f.summaryText = "无样本";
            return f;
        }

        double[] freq16 = numberDistribution(draws, position, WINDOW_16);
        double[] freq32 = numberDistribution(draws, position, WINDOW_32);
        double[] freq64 = numberDistribution(draws, position, WINDOW_64);
        double[] freq160 = numberDistribution(draws, position, WINDOW_160);
        double[] freq500 = numberDistribution(draws, position, WINDOW_500);

        f.entropy16 = entropy(freq16);
        f.entropy32 = entropy(freq32);
        f.entropy64 = entropy(freq64);
        f.entropy160 = entropy(freq160);

        f.top3Mass16 = topMass(freq16, 3);
        f.top3Mass32 = topMass(freq32, 3);
        f.top3Mass64 = topMass(freq64, 3);
        f.top3Mass160 = topMass(freq160, 3);
        f.top5Mass64 = topMass(freq64, 5);

        f.championMass32 = topMass(freq32, 1);
        f.championMass160 = topMass(freq160, 1);
        f.championGap = clamp(f.championMass32 - f.championMass160, -1.0D, 1.0D);

        f.trendStrength =
                calculateTrendStrength(f.top3Mass16, f.top3Mass32, f.top3Mass64, f.top3Mass160);
        f.trendSlope = calculateTrendSlope(f.top3Mass16, f.top3Mass32, f.top3Mass64, f.top3Mass160);
        f.trendLife = estimateTrendLife(draws, position);

        f.driftRisk = calculatePositionDriftRisk(freq32, freq160, f.entropy32, f.entropy160);
        f.breakDepth = calculateBreakDepth(draws, position, freq64);
        f.recoveryScore = calculateRecoveryScore(f);
        f.stabilityScore = calculateStabilityScore(f);

        f.accelerationScore =
                clamp((f.top3Mass16 - f.top3Mass64) * 2.5D + positive(f.trendSlope), 0.0D, 1.0D);
        f.weakeningScore =
                clamp(
                        (f.top3Mass160 - f.top3Mass32) * 2.4D + positive(f.driftRisk - 0.45D),
                        0.0D,
                        1.0D);
        f.chaosScore =
                clamp((f.entropy32 / maxEntropy()) * 0.55D + f.driftRisk * 0.45D, 0.0D, 1.0D);

        f.positionScore = calculatePositionScore(f);
        f.summaryText = buildPositionSummary(f);

        return f;
    }

    /** 构建某位置某号码的行为特征。 */
    public NumberFeature buildNumberFeature(List<DrawRecord> draws, int position, int number) {
        NumberFeature f = new NumberFeature();
        f.position = position;
        f.number = number;

        if (!isValidPosition(position) || !isValidNumber(number)) {
            f.summaryText = "位置或号码非法";
            return f;
        }

        int total = draws == null ? 0 : draws.size();
        f.sampleCount = total;

        if (total == 0) {
            f.summaryText = "无样本";
            return f;
        }

        f.freq16 = frequencyOfNumber(draws, position, number, WINDOW_16);
        f.freq32 = frequencyOfNumber(draws, position, number, WINDOW_32);
        f.freq64 = frequencyOfNumber(draws, position, number, WINDOW_64);
        f.freq160 = frequencyOfNumber(draws, position, number, WINDOW_160);
        f.freq500 = frequencyOfNumber(draws, position, number, WINDOW_500);

        f.hotScore = calculateHotScore(f);
        f.coldScore = calculateColdScore(f);
        f.accelerationScore = calculateNumberAcceleration(f);
        f.decayScore = calculateNumberDecay(f);

        f.lastGap = calculateLastGap(draws, position, number);
        f.avgGap = calculateAverageGap(draws, position, number, WINDOW_500);
        f.maxGap = calculateMaxGap(draws, position, number, WINDOW_500);
        f.gapVariance = calculateGapVariance(draws, position, number, WINDOW_500, f.avgGap);

        f.gapPressure = calculateGapPressure(f.lastGap, f.avgGap, f.maxGap);
        f.cycleScore = calculateCycleScore(f.lastGap, f.avgGap, f.gapVariance);
        f.nearCyclePoint = f.cycleScore >= 0.58D;

        f.reverseRisk = calculateReverseRisk(draws, position, number, f);
        f.returnScore = calculateReturnScore(f);
        f.stabilityScore = calculateNumberStability(f);
        f.volatilityScore = clamp(f.gapVariance / 30.0D, 0.0D, 1.0D);

        f.confidence = calculateNumberConfidence(f);
        f.numberScore = calculateNumberScore(f);

        f.summaryText = buildNumberSummary(f);

        return f;
    }

    /** 对一个“位置 + 三号码”候选进行深度评分。 */
    public CandidateScore scoreCandidate(DeepFeatureSet set, int position, int n1, int n2, int n3) {
        CandidateScore s = new CandidateScore();
        s.position = position;
        s.numbers = new int[] {n1, n2, n3};

        if (set == null) {
            s.valid = false;
            s.reasonText = "DeepFeatureSet 为空";
            return s;
        }

        if (!isValidPosition(position)) {
            s.valid = false;
            s.reasonText = "位置非法";
            return s;
        }

        if (!isValidK3(n1, n2, n3)) {
            s.valid = false;
            s.reasonText = "K3号码非法或重复";
            return s;
        }

        PositionFeature pf = set.positionFeatures[position - 1];
        if (pf == null) {
            s.valid = false;
            s.reasonText = "位置特征为空";
            return s;
        }

        NumberFeature f1 = set.numberFeatures[position - 1][n1 - 1];
        NumberFeature f2 = set.numberFeatures[position - 1][n2 - 1];
        NumberFeature f3 = set.numberFeatures[position - 1][n3 - 1];

        if (f1 == null || f2 == null || f3 == null) {
            s.valid = false;
            s.reasonText = "号码特征为空";
            return s;
        }

        s.valid = true;

        double numberAvg = (f1.numberScore + f2.numberScore + f3.numberScore) / 3.0D;
        double numberMin = Math.min(f1.numberScore, Math.min(f2.numberScore, f3.numberScore));
        double numberMax = Math.max(f1.numberScore, Math.max(f2.numberScore, f3.numberScore));
        double balance = 1.0D - clamp(numberMax - numberMin, 0.0D, 1.0D);

        double cycleAvg = (f1.cycleScore + f2.cycleScore + f3.cycleScore) / 3.0D;
        double returnAvg = (f1.returnScore + f2.returnScore + f3.returnScore) / 3.0D;
        double confidenceAvg = (f1.confidence + f2.confidence + f3.confidence) / 3.0D;
        double reverseRiskAvg = (f1.reverseRisk + f2.reverseRisk + f3.reverseRisk) / 3.0D;

        s.positionScore = pf.positionScore;
        s.numberScore = numberAvg;
        s.weakestNumberScore = numberMin;
        s.balanceScore = balance;
        s.cycleScore = cycleAvg;
        s.returnScore = returnAvg;
        s.confidence = confidenceAvg;

        s.driftRisk = pf.driftRisk;
        s.breakRisk = pf.breakDepth;
        s.reverseRisk = reverseRiskAvg;
        s.chaosRisk = pf.chaosScore;

        s.deepScore = calculateCandidateDeepScore(s, pf);
        s.riskPenalty = calculateCandidateRiskPenalty(s, pf);
        s.finalScore = clamp(s.deepScore - s.riskPenalty, 0.0D, 1.0D);

        s.layerText = buildCandidateLayerText(s);
        s.reasonText = buildCandidateReasonText(set, s, pf, f1, f2, f3);

        return s;
    }

    public int chooseBestPosition(PositionFeature[] features) {
        if (features == null || features.length == 0) {
            return 1;
        }

        int bestPosition = 1;
        double bestScore = -999.0D;

        int i;
        for (i = 0; i < features.length; i++) {
            PositionFeature f = features[i];
            if (f == null) {
                continue;
            }

            double score = f.positionScore;
            if (score > bestScore) {
                bestScore = score;
                bestPosition = f.position;
            }
        }

        if (!isValidPosition(bestPosition)) {
            return 1;
        }

        return bestPosition;
    }

    public int[] chooseBestNumbers(DeepFeatureSet set, int position, int k) {
        int realK = k <= 0 ? K3 : k;
        if (realK > 10) {
            realK = 10;
        }

        int[] result = new int[realK];
        if (set == null || !isValidPosition(position)) {
            int i;
            for (i = 0; i < realK; i++) {
                result[i] = i + 1;
            }
            return result;
        }

        ArrayList<NumberFeature> list = new ArrayList<NumberFeature>();
        int n;
        for (n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            NumberFeature f = set.numberFeatures[position - 1][n - 1];
            if (f != null) {
                list.add(f);
            }
        }

        Collections.sort(
                list,
                new Comparator<NumberFeature>() {
                    public int compare(NumberFeature a, NumberFeature b) {
                        if (a == null && b == null) {
                            return 0;
                        }
                        if (a == null) {
                            return 1;
                        }
                        if (b == null) {
                            return -1;
                        }
                        if (a.numberScore < b.numberScore) {
                            return 1;
                        }
                        if (a.numberScore > b.numberScore) {
                            return -1;
                        }
                        return a.number - b.number;
                    }
                });

        int i2;
        for (i2 = 0; i2 < realK; i2++) {
            if (i2 < list.size()) {
                result[i2] = list.get(i2).number;
            } else {
                result[i2] = i2 + 1;
            }
        }

        return result;
    }

    public String detectStateMode(DeepFeatureSet set) {
        if (set == null || set.drawCount <= 0) {
            return STATE_UNKNOWN;
        }

        double entropy = set.globalEntropy;
        double drift = set.globalDriftRisk;
        double trend = set.globalTrendStrength;
        double recovery = set.globalRecoveryScore;
        double breakRisk = set.globalBreakRisk;

        PositionFeature best = null;
        if (isValidPosition(set.bestPosition)) {
            best = set.positionFeatures[set.bestPosition - 1];
        }

        double bestTop3 = best == null ? 0.0D : best.top3Mass32;
        double bestChaos = best == null ? 0.0D : best.chaosScore;
        double bestAcceleration = best == null ? 0.0D : best.accelerationScore;
        double bestWeakening = best == null ? 0.0D : best.weakeningScore;

        if (set.drawCount < 32) {
            return STATE_UNKNOWN;
        }

        if (bestChaos >= 0.72D || (entropy >= 0.88D && drift >= 0.62D)) {
            return STATE_CHAOS;
        }

        if (breakRisk >= 0.68D && drift >= 0.52D) {
            return STATE_BROKEN;
        }

        if (bestWeakening >= 0.62D && trend < 0.48D) {
            return STATE_WEAKENING;
        }

        if (recovery >= 0.58D && drift <= 0.52D && breakRisk <= 0.55D) {
            return STATE_RECOVERING;
        }

        if (bestAcceleration >= 0.62D && trend >= 0.52D && drift <= 0.58D) {
            return STATE_ACCELERATING;
        }

        if (trend >= 0.62D && bestTop3 >= 0.36D && drift <= 0.48D && breakRisk <= 0.45D) {
            return STATE_CONFIRMED;
        }

        if (Math.abs(trend - drift) <= 0.12D && trend >= 0.45D && drift >= 0.40D) {
            return STATE_TRANSITION;
        }

        if (trend >= 0.48D && drift <= 0.45D && entropy <= 0.78D) {
            return STATE_STABLE;
        }

        if (drift >= 0.55D && recovery >= 0.45D) {
            return STATE_REVERSAL;
        }

        return STATE_TRANSITION;
    }

    public String buildProfileKey(DeepFeatureSet set) {
        if (set == null) {
            return "P0_UNKNOWN";
        }

        int trendBucket = bucket10(set.globalTrendStrength);
        int driftBucket = bucket10(set.globalDriftRisk);
        int recoveryBucket = bucket10(set.globalRecoveryScore);
        int breakBucket = bucket10(set.globalBreakRisk);
        int entropyBucket = bucket10(set.globalEntropy);

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(set.bestPosition);
        sb.append("_").append(safe(set.stateMode));
        sb.append("_T").append(trendBucket);
        sb.append("_D").append(driftBucket);
        sb.append("_R").append(recoveryBucket);
        sb.append("_B").append(breakBucket);
        sb.append("_E").append(entropyBucket);
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
            if (d == null) {
                continue;
            }
            if (safe(d.issue).length() == 0) {
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

    private double[] numberDistribution(List<DrawRecord> draws, int position, int window) {
        double[] count = new double[NUMBER_COUNT];

        if (draws == null || draws.size() == 0 || !isValidPosition(position)) {
            return count;
        }

        int start = draws.size() - window;
        if (start < 0) {
            start = 0;
        }

        int total = 0;
        int i;
        for (i = start; i < draws.size(); i++) {
            DrawRecord d = draws.get(i);
            int number = getNumberAt(d, position);
            if (isValidNumber(number)) {
                count[number - 1] += 1.0D;
                total++;
            }
        }

        if (total <= 0) {
            return count;
        }

        for (i = 0; i < count.length; i++) {
            count[i] = count[i] / total;
        }

        return count;
    }

    private double frequencyOfNumber(List<DrawRecord> draws, int position, int number, int window) {
        if (draws == null
                || draws.size() == 0
                || !isValidPosition(position)
                || !isValidNumber(number)) {
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
            int n = getNumberAt(draws.get(i), position);
            if (isValidNumber(n)) {
                total++;
                if (n == number) {
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
            int n = getNumberAt(draws.get(i), position);
            if (n == number) {
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
        if (gaps.size() == 0) {
            return 0;
        }

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
            double diff = gaps.get(i).intValue() - avgGap;
            sum += diff * diff;
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

        int lastIndex = -1;
        int i;
        for (i = start; i < draws.size(); i++) {
            int n = getNumberAt(draws.get(i), position);
            if (n == number) {
                if (lastIndex >= 0) {
                    gaps.add(Integer.valueOf(i - lastIndex));
                }
                lastIndex = i;
            }
        }

        return gaps;
    }

    private double calculateHotScore(NumberFeature f) {
        double shortHot = normalizeRate(f.freq16, 0.10D, 0.24D);
        double midHot = normalizeRate(f.freq64, 0.10D, 0.20D);
        double longHot = normalizeRate(f.freq160, 0.10D, 0.18D);
        return clamp(shortHot * 0.45D + midHot * 0.35D + longHot * 0.20D, 0.0D, 1.0D);
    }

    private double calculateColdScore(NumberFeature f) {
        double coldFreq = 1.0D - normalizeRate(f.freq64, 0.04D, 0.16D);
        double gapCold = normalizeRate(f.lastGap, 5.0D, 25.0D);
        return clamp(coldFreq * 0.45D + gapCold * 0.55D, 0.0D, 1.0D);
    }

    private double calculateNumberAcceleration(NumberFeature f) {
        double a = (f.freq16 - f.freq64) * 6.0D;
        double b = (f.freq32 - f.freq160) * 4.0D;
        return clamp(a * 0.55D + b * 0.45D, 0.0D, 1.0D);
    }

    private double calculateNumberDecay(NumberFeature f) {
        double a = (f.freq160 - f.freq32) * 5.0D;
        double b = (f.freq64 - f.freq16) * 5.0D;
        return clamp(a * 0.55D + b * 0.45D, 0.0D, 1.0D);
    }

    private double calculateGapPressure(int lastGap, double avgGap, int maxGap) {
        if (lastGap < 0) {
            return 0.0D;
        }

        double avg = avgGap <= EPS ? 10.0D : avgGap;
        double p1 = normalizeRate(lastGap, avg * 0.55D, avg * 1.25D);
        double p2 = maxGap <= 0 ? 0.0D : normalizeRate(lastGap, maxGap * 0.30D, maxGap * 0.85D);

        return clamp(p1 * 0.72D + p2 * 0.28D, 0.0D, 1.0D);
    }

    private double calculateCycleScore(int lastGap, double avgGap, double gapVariance) {
        if (lastGap < 0 || avgGap <= EPS) {
            return 0.0D;
        }

        double diff = Math.abs(lastGap - avgGap);
        double closeness = 1.0D - clamp(diff / Math.max(2.0D, avgGap), 0.0D, 1.0D);
        double stable = 1.0D - clamp(gapVariance / 25.0D, 0.0D, 1.0D);

        return clamp(closeness * 0.65D + stable * 0.35D, 0.0D, 1.0D);
    }

    private double calculateReverseRisk(
            List<DrawRecord> draws, int position, int number, NumberFeature f) {
        int recentUse = 0;
        int start = draws.size() - 6;
        if (start < 0) {
            start = 0;
        }

        int i;
        for (i = start; i < draws.size(); i++) {
            if (getNumberAt(draws.get(i), position) == number) {
                recentUse++;
            }
        }

        double overuse = normalizeRate(recentUse, 2.0D, 4.0D);
        double hot = f.hotScore;
        double decay = f.decayScore;

        return clamp(overuse * 0.35D + hot * 0.35D + decay * 0.30D, 0.0D, 1.0D);
    }

    private double calculateReturnScore(NumberFeature f) {
        double coldReturn = f.coldScore * f.gapPressure;
        double cycleReturn = f.cycleScore;
        double notDecayed = 1.0D - f.decayScore;
        return clamp(coldReturn * 0.42D + cycleReturn * 0.40D + notDecayed * 0.18D, 0.0D, 1.0D);
    }

    private double calculateNumberStability(NumberFeature f) {
        double freqStable = 1.0D - clamp(Math.abs(f.freq32 - f.freq160) * 5.0D, 0.0D, 1.0D);
        double gapStable = 1.0D - clamp(f.gapVariance / 25.0D, 0.0D, 1.0D);
        double notReverse = 1.0D - f.reverseRisk;
        return clamp(freqStable * 0.40D + gapStable * 0.35D + notReverse * 0.25D, 0.0D, 1.0D);
    }

    private double calculateNumberConfidence(NumberFeature f) {
        double sample = normalizeRate(f.sampleCount, 30.0D, 160.0D);
        double stable = f.stabilityScore;
        double notChaos = 1.0D - f.volatilityScore;
        return clamp(sample * 0.35D + stable * 0.45D + notChaos * 0.20D, 0.0D, 1.0D);
    }

    private double calculateNumberScore(NumberFeature f) {
        double hot = f.hotScore * 0.24D;
        double acceleration = f.accelerationScore * 0.17D;
        double cycle = f.cycleScore * 0.18D;
        double ret = f.returnScore * 0.14D;
        double stable = f.stabilityScore * 0.17D;
        double confidence = f.confidence * 0.10D;

        double risk = f.reverseRisk * 0.16D + f.decayScore * 0.10D;

        return clamp(hot + acceleration + cycle + ret + stable + confidence - risk, 0.0D, 1.0D);
    }

    private double calculateTrendStrength(double top16, double top32, double top64, double top160) {
        double base = top32 * 0.35D + top64 * 0.35D + top160 * 0.15D + top16 * 0.15D;
        double slopeBonus = positive(top32 - top160) * 0.9D;
        return clamp(normalizeRate(base, 0.30D, 0.48D) * 0.78D + slopeBonus * 0.22D, 0.0D, 1.0D);
    }

    private double calculateTrendSlope(double top16, double top32, double top64, double top160) {
        double shortSlope = top16 - top64;
        double midSlope = top32 - top160;
        return clamp((shortSlope * 0.55D + midSlope * 0.45D) * 4.0D, -1.0D, 1.0D);
    }

    private int estimateTrendLife(List<DrawRecord> draws, int position) {
        if (draws == null || draws.size() < 12) {
            return 0;
        }

        int life = 0;
        int maxCheck = Math.min(96, draws.size());
        int i;

        for (i = 16; i <= maxCheck; i += 8) {
            double[] dist = numberDistributionLast(draws, position, i);
            double mass = topMass(dist, 3);
            if (mass >= 0.34D) {
                life = i;
            } else if (life > 0) {
                break;
            }
        }

        return life;
    }

    private double[] numberDistributionLast(List<DrawRecord> draws, int position, int lastCount) {
        return numberDistribution(draws, position, lastCount);
    }

    private double calculatePositionDriftRisk(
            double[] freq32, double[] freq160, double entropy32, double entropy160) {
        double distance = distributionDistance(freq32, freq160);
        double entropyRise = positive(entropy32 - entropy160);
        return clamp(distance * 0.68D + entropyRise * 0.32D, 0.0D, 1.0D);
    }

    private double calculateBreakDepth(List<DrawRecord> draws, int position, double[] freq64) {
        if (draws == null || draws.size() == 0) {
            return 0.0D;
        }

        int[] rank = rankNumbers(freq64);
        int missDepthSum = 0;
        int check = Math.min(12, draws.size());
        int counted = 0;

        int i;
        for (i = draws.size() - check; i < draws.size(); i++) {
            if (i < 0) {
                continue;
            }
            int actual = getNumberAt(draws.get(i), position);
            if (!isValidNumber(actual)) {
                continue;
            }

            int layer = findRankLayer(rank, actual);
            if (layer > 3) {
                missDepthSum += layer - 3;
            }
            counted++;
        }

        if (counted <= 0) {
            return 0.0D;
        }

        return clamp(((double) missDepthSum) / ((double) counted * 5.0D), 0.0D, 1.0D);
    }

    private double calculateRecoveryScore(PositionFeature f) {
        double fromBreak = f.breakDepth > 0.20D ? 1.0D - f.breakDepth : 0.25D;
        double trendReturn = positive(f.top3Mass32 - f.top3Mass160) * 3.0D;
        double entropyDown = positive(f.entropy160 - f.entropy32);
        double driftOk = 1.0D - f.driftRisk;

        return clamp(
                fromBreak * 0.18D + trendReturn * 0.34D + entropyDown * 0.22D + driftOk * 0.26D,
                0.0D,
                1.0D);
    }

    private double calculateStabilityScore(PositionFeature f) {
        double entropyStable = 1.0D - clamp(f.entropy32 / maxEntropy(), 0.0D, 1.0D);
        double driftStable = 1.0D - f.driftRisk;
        double breakStable = 1.0D - f.breakDepth;
        double trendStable = f.trendStrength;

        return clamp(
                entropyStable * 0.25D
                        + driftStable * 0.30D
                        + breakStable * 0.20D
                        + trendStable * 0.25D,
                0.0D,
                1.0D);
    }

    private double calculatePositionScore(PositionFeature f) {
        double score = 0.0D;
        score += f.trendStrength * 0.26D;
        score += f.stabilityScore * 0.22D;
        score += normalizeRate(f.top3Mass32, 0.30D, 0.48D) * 0.18D;
        score += f.recoveryScore * 0.12D;
        score += f.accelerationScore * 0.08D;
        score += (1.0D - f.driftRisk) * 0.08D;
        score += (1.0D - f.breakDepth) * 0.06D;

        double penalty = f.chaosScore * 0.15D + f.weakeningScore * 0.10D;
        return clamp(score - penalty, 0.0D, 1.0D);
    }

    private double calculateGlobalEntropy(List<DrawRecord> draws, int window) {
        if (draws == null || draws.size() == 0) {
            return 1.0D;
        }

        double sum = 0.0D;
        int count = 0;

        int p;
        for (p = 1; p <= POSITION_COUNT; p++) {
            double[] dist = numberDistribution(draws, p, window);
            sum += entropy(dist) / maxEntropy();
            count++;
        }

        if (count <= 0) {
            return 1.0D;
        }

        return clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateGlobalDriftRisk(List<DrawRecord> draws) {
        if (draws == null || draws.size() == 0) {
            return 0.0D;
        }

        double sum = 0.0D;
        int p;
        for (p = 1; p <= POSITION_COUNT; p++) {
            double[] f32 = numberDistribution(draws, p, WINDOW_32);
            double[] f160 = numberDistribution(draws, p, WINDOW_160);
            sum += distributionDistance(f32, f160);
        }

        return clamp(sum / POSITION_COUNT, 0.0D, 1.0D);
    }

    private double calculateGlobalTrendStrength(PositionFeature[] features) {
        if (features == null || features.length == 0) {
            return 0.0D;
        }

        double top1 = 0.0D;
        double top2 = 0.0D;

        int i;
        for (i = 0; i < features.length; i++) {
            PositionFeature f = features[i];
            if (f == null) {
                continue;
            }
            double v = f.trendStrength;
            if (v > top1) {
                top2 = top1;
                top1 = v;
            } else if (v > top2) {
                top2 = v;
            }
        }

        return clamp(top1 * 0.70D + top2 * 0.30D, 0.0D, 1.0D);
    }

    private double calculateGlobalRecoveryScore(PositionFeature[] features) {
        if (features == null || features.length == 0) {
            return 0.0D;
        }

        double best = 0.0D;
        int i;
        for (i = 0; i < features.length; i++) {
            PositionFeature f = features[i];
            if (f != null && f.recoveryScore > best) {
                best = f.recoveryScore;
            }
        }

        return clamp(best, 0.0D, 1.0D);
    }

    private double calculateGlobalBreakRisk(PositionFeature[] features) {
        if (features == null || features.length == 0) {
            return 0.0D;
        }

        double sum = 0.0D;
        int count = 0;
        int i;
        for (i = 0; i < features.length; i++) {
            PositionFeature f = features[i];
            if (f != null) {
                sum += f.breakDepth;
                count++;
            }
        }

        if (count <= 0) {
            return 0.0D;
        }

        return clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateCandidateDeepScore(CandidateScore s, PositionFeature pf) {
        double score = 0.0D;
        score += s.positionScore * 0.24D;
        score += s.numberScore * 0.26D;
        score += s.weakestNumberScore * 0.13D;
        score += s.balanceScore * 0.10D;
        score += s.cycleScore * 0.09D;
        score += s.returnScore * 0.06D;
        score += s.confidence * 0.08D;
        score += pf.trendStrength * 0.04D;
        return clamp(score, 0.0D, 1.0D);
    }

    private double calculateCandidateRiskPenalty(CandidateScore s, PositionFeature pf) {
        double penalty = 0.0D;
        penalty += s.driftRisk * 0.12D;
        penalty += s.breakRisk * 0.13D;
        penalty += s.reverseRisk * 0.12D;
        penalty += s.chaosRisk * 0.14D;

        if (s.weakestNumberScore < 0.30D) {
            penalty += 0.08D;
        }

        if (s.balanceScore < 0.45D) {
            penalty += 0.05D;
        }

        if (pf.weakeningScore > 0.60D) {
            penalty += 0.06D;
        }

        return clamp(penalty, 0.0D, 0.55D);
    }

    private String buildSummaryText(DeepFeatureSet set) {
        StringBuilder sb = new StringBuilder();
        sb.append("深度特征：");
        sb.append("样本=").append(set.drawCount);
        sb.append(" | 状态=").append(set.stateMode);
        sb.append(" | 最强位置=").append(set.bestPosition);
        sb.append(" | 推荐号码=");
        if (set.bestNumbers != null) {
            int i;
            for (i = 0; i < set.bestNumbers.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(two(set.bestNumbers[i]));
            }
        }
        sb.append(" | 趋势=").append(fmt(set.globalTrendStrength));
        sb.append(" | 漂移=").append(fmt(set.globalDriftRisk));
        sb.append(" | 熵=").append(fmt(set.globalEntropy));
        sb.append(" | 恢复=").append(fmt(set.globalRecoveryScore));
        sb.append(" | 破位=").append(fmt(set.globalBreakRisk));
        sb.append(" | profile=").append(set.profileKey);
        return sb.toString();
    }

    private String buildPositionSummary(PositionFeature f) {
        StringBuilder sb = new StringBuilder();
        sb.append("位置").append(f.position);
        sb.append(" 强度=").append(fmt(f.positionScore));
        sb.append(" 趋势=").append(fmt(f.trendStrength));
        sb.append(" Top3=").append(fmt(f.top3Mass32));
        sb.append(" 漂移=").append(fmt(f.driftRisk));
        sb.append(" 破位=").append(fmt(f.breakDepth));
        sb.append(" 恢复=").append(fmt(f.recoveryScore));
        sb.append(" 稳定=").append(fmt(f.stabilityScore));
        return sb.toString();
    }

    private String buildNumberSummary(NumberFeature f) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(f.position).append("-").append(two(f.number));
        sb.append(" 分=").append(fmt(f.numberScore));
        sb.append(" 热=").append(fmt(f.hotScore));
        sb.append(" 冷=").append(fmt(f.coldScore));
        sb.append(" 遗漏=").append(f.lastGap);
        sb.append(" 周期=").append(fmt(f.cycleScore));
        sb.append(" 回补=").append(fmt(f.returnScore));
        sb.append(" 反杀=").append(fmt(f.reverseRisk));
        sb.append(" 置信=").append(fmt(f.confidence));
        return sb.toString();
    }

    private String buildCandidateLayerText(CandidateScore s) {
        if (s.finalScore >= 0.72D && s.confidence >= 0.62D && s.riskPenalty <= 0.18D) {
            return "A+强证据候选";
        }
        if (s.finalScore >= 0.62D && s.confidence >= 0.52D) {
            return "强候选";
        }
        if (s.finalScore >= 0.52D) {
            return "可观察候选";
        }
        if (s.riskPenalty >= 0.35D) {
            return "高风险候选";
        }
        return "普通候选";
    }

    private String buildCandidateReasonText(
            DeepFeatureSet set,
            CandidateScore s,
            PositionFeature pf,
            NumberFeature f1,
            NumberFeature f2,
            NumberFeature f3) {
        StringBuilder sb = new StringBuilder();
        sb.append("候选 P").append(s.position).append(" ");
        sb.append(two(s.numbers[0])).append(",");
        sb.append(two(s.numbers[1])).append(",");
        sb.append(two(s.numbers[2]));
        sb.append(" | ").append(s.layerText);
        sb.append(" | 总分=").append(fmt(s.finalScore));
        sb.append(" | 位置=").append(fmt(s.positionScore));
        sb.append(" | 号码=").append(fmt(s.numberScore));
        sb.append(" | 最弱号=").append(fmt(s.weakestNumberScore));
        sb.append(" | 均衡=").append(fmt(s.balanceScore));
        sb.append(" | 周期=").append(fmt(s.cycleScore));
        sb.append(" | 风险=").append(fmt(s.riskPenalty));
        sb.append(" | 状态=").append(set.stateMode);

        if (s.weakestNumberScore < 0.30D) {
            sb.append(" | 警告：三号中存在弱号");
        }
        if (s.driftRisk > 0.60D) {
            sb.append(" | 警告：位置漂移偏高");
        }
        if (s.reverseRisk > 0.58D) {
            sb.append(" | 警告：号码反杀风险偏高");
        }
        if (pf.trendStrength >= 0.60D) {
            sb.append(" | 趋势支持");
        }
        if (f1.nearCyclePoint || f2.nearCyclePoint || f3.nearCyclePoint) {
            sb.append(" | 至少一个号码接近周期点");
        }

        return sb.toString();
    }

    private int[] rankNumbers(double[] dist) {
        ArrayList<RankItem> list = new ArrayList<RankItem>();
        int i;
        for (i = 0; i < NUMBER_COUNT; i++) {
            RankItem item = new RankItem();
            item.number = i + 1;
            item.value = dist == null || i >= dist.length ? 0.0D : dist[i];
            list.add(item);
        }

        Collections.sort(
                list,
                new Comparator<RankItem>() {
                    public int compare(RankItem a, RankItem b) {
                        if (a.value < b.value) {
                            return 1;
                        }
                        if (a.value > b.value) {
                            return -1;
                        }
                        return a.number - b.number;
                    }
                });

        int[] rank = new int[NUMBER_COUNT];
        for (i = 0; i < list.size(); i++) {
            rank[i] = list.get(i).number;
        }

        return rank;
    }

    private int findRankLayer(int[] rank, int actual) {
        if (rank == null || !isValidNumber(actual)) {
            return 10;
        }

        int i;
        for (i = 0; i < rank.length; i++) {
            if (rank[i] == actual) {
                return i + 1;
            }
        }

        return 10;
    }

    private double topMass(double[] dist, int topN) {
        if (dist == null || dist.length == 0 || topN <= 0) {
            return 0.0D;
        }

        double[] copy = new double[dist.length];
        int i;
        for (i = 0; i < dist.length; i++) {
            copy[i] = dist[i];
        }

        sortDesc(copy);

        double sum = 0.0D;
        int limit = Math.min(topN, copy.length);
        for (i = 0; i < limit; i++) {
            sum += copy[i];
        }

        return clamp(sum, 0.0D, 1.0D);
    }

    private double entropy(double[] dist) {
        if (dist == null || dist.length == 0) {
            return 0.0D;
        }

        double e = 0.0D;
        int i;
        for (i = 0; i < dist.length; i++) {
            double p = dist[i];
            if (p > EPS) {
                e -= p * Math.log(p);
            }
        }

        return e;
    }

    private double maxEntropy() {
        return Math.log(10.0D);
    }

    private double distributionDistance(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0D;
        }

        int len = Math.min(a.length, b.length);
        double sum = 0.0D;

        int i;
        for (i = 0; i < len; i++) {
            sum += Math.abs(a[i] - b[i]);
        }

        return clamp(sum / 2.0D, 0.0D, 1.0D);
    }

    private void sortDesc(double[] values) {
        if (values == null) {
            return;
        }

        int i;
        int j;
        for (i = 0; i < values.length - 1; i++) {
            for (j = i + 1; j < values.length; j++) {
                if (values[j] > values[i]) {
                    double tmp = values[i];
                    values[i] = values[j];
                    values[j] = tmp;
                }
            }
        }
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
            int v = nums.get(i).intValue();
            if (!isValidNumber(v)) {
                return null;
            }
            arr[i] = v;
        }

        return arr;
    }

    private void flushNumber(StringBuilder buf, ArrayList<Integer> nums) {
        if (buf == null || buf.length() == 0) {
            return;
        }

        try {
            int v = Integer.parseInt(buf.toString());
            nums.add(Integer.valueOf(v));
        } catch (Exception e) {
            // 忽略非法数字片段
        }

        buf.setLength(0);
    }

    private String formatNumbers(int[] nums) {
        if (nums == null) {
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

        String sa = safe(a);
        String sb = safe(b);
        int cmp2 = sa.compareTo(sb);
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

    private double normalizeRate(double value, double low, double high) {
        if (high <= low) {
            return 0.0D;
        }
        return clamp((value - low) / (high - low), 0.0D, 1.0D);
    }

    private double positive(double v) {
        return v > 0.0D ? v : 0.0D;
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

    private int bucket10(double value) {
        int b = (int) Math.floor(clamp(value, 0.0D, 1.0D) * 10.0D);
        if (b < 0) {
            b = 0;
        }
        if (b > 9) {
            b = 9;
        }
        return b;
    }

    private String fmt(double value) {
        int v = (int) Math.round(value * 100.0D);
        return String.valueOf(v) + "%";
    }

    private static String two(int n) {
        if (n < 10) {
            return "0" + n;
        }
        return String.valueOf(n);
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private static final class RankItem {
        int number;
        double value;
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

    public static final class DeepFeatureSet {
        public String baseIssue;
        public int drawCount;

        public PositionFeature[] positionFeatures;
        public NumberFeature[][] numberFeatures;

        public int bestPosition;
        public int[] bestNumbers;

        public double globalEntropy;
        public double globalDriftRisk;
        public double globalTrendStrength;
        public double globalRecoveryScore;
        public double globalBreakRisk;

        public String stateMode;
        public String profileKey;
        public String summaryText;

        public DeepFeatureSet() {
            this.baseIssue = "";
            this.drawCount = 0;
            this.positionFeatures = new PositionFeature[POSITION_COUNT];
            this.numberFeatures = new NumberFeature[POSITION_COUNT][NUMBER_COUNT];
            this.bestPosition = 1;
            this.bestNumbers = new int[] {1, 2, 3};
            this.globalEntropy = 1.0D;
            this.globalDriftRisk = 0.0D;
            this.globalTrendStrength = 0.0D;
            this.globalRecoveryScore = 0.0D;
            this.globalBreakRisk = 0.0D;
            this.stateMode = STATE_UNKNOWN;
            this.profileKey = "";
            this.summaryText = "";
        }
    }

    public static final class PositionFeature {
        public int position;
        public int sampleCount;

        public double entropy16;
        public double entropy32;
        public double entropy64;
        public double entropy160;

        public double top3Mass16;
        public double top3Mass32;
        public double top3Mass64;
        public double top3Mass160;
        public double top5Mass64;

        public double championMass32;
        public double championMass160;
        public double championGap;

        public double trendStrength;
        public double trendSlope;
        public int trendLife;

        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double stabilityScore;

        public double accelerationScore;
        public double weakeningScore;
        public double chaosScore;

        public double positionScore;
        public String summaryText;

        public PositionFeature() {
            this.position = 0;
            this.sampleCount = 0;
            this.summaryText = "";
        }
    }

    public static final class NumberFeature {
        public int position;
        public int number;
        public int sampleCount;

        public double freq16;
        public double freq32;
        public double freq64;
        public double freq160;
        public double freq500;

        public double hotScore;
        public double coldScore;
        public double accelerationScore;
        public double decayScore;

        public int lastGap;
        public double avgGap;
        public int maxGap;
        public double gapVariance;
        public double gapPressure;

        public double cycleScore;
        public boolean nearCyclePoint;

        public double reverseRisk;
        public double returnScore;
        public double stabilityScore;
        public double volatilityScore;
        public double confidence;

        public double numberScore;
        public String summaryText;

        public NumberFeature() {
            this.position = 0;
            this.number = 0;
            this.sampleCount = 0;
            this.lastGap = -1;
            this.summaryText = "";
        }
    }

    public static final class CandidateScore {
        public boolean valid;
        public int position;
        public int[] numbers;

        public double positionScore;
        public double numberScore;
        public double weakestNumberScore;
        public double balanceScore;
        public double cycleScore;
        public double returnScore;
        public double confidence;

        public double driftRisk;
        public double breakRisk;
        public double reverseRisk;
        public double chaosRisk;

        public double deepScore;
        public double riskPenalty;
        public double finalScore;

        public String layerText;
        public String reasonText;

        public CandidateScore() {
            this.valid = false;
            this.position = 0;
            this.numbers = new int[] {0, 0, 0};
            this.layerText = "";
            this.reasonText = "";
        }
    }
}
