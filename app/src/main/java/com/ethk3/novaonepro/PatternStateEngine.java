package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * PatternStateEngine.java
 *
 * <p>状态 / 走势 / 趋势 / 形态识别引擎。
 *
 * <p>负责把“状态、走势、趋势、形态”变成真实可计算结果：
 *
 * <p>状态： - CONFIRMED：确认态 - ACCELERATING：加速态 - RECOVERING：恢复态 - WEAKENING：转弱态 - BROKEN：破位态 -
 * CHAOS：混沌态 - REVERSAL：反转态 - STABLE：稳定态 - TRANSITION：切换态
 *
 * <p>走势： - 近窗口与中长窗口的变化方向
 *
 * <p>趋势： - trendStrength - trendSlope - trendLife - trendConsistency - trendBreakRisk
 *
 * <p>形态： - position + state + trendBucket + driftBucket + breakBucket + recoveryBucket +
 * entropyBucket
 *
 * <p>Java 7 / Android API21+ / 无第三方库。
 */
public final class PatternStateEngine {

    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_MIN = 1;
    public static final int NUMBER_MAX = 10;
    public static final int NUMBER_COUNT = 10;

    public static final int WINDOW_16 = 16;
    public static final int WINDOW_32 = 32;
    public static final int WINDOW_64 = 64;
    public static final int WINDOW_160 = 160;

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
    public static final String B06_CONTRACT_VERSION = "STARCORE_TIANSHU_B06_PATTERN_EVIDENCE_V1";
    public static final String B06_OWNER = "PATTERN_EVIDENCE_ONLY";

    public PatternStateEngine() {}

    /** 分析全局状态、走势、趋势、形态。 */
    public PatternStateResult analyze(List<DrawRecord> inputDraws, String baseIssue) {
        PatternStateResult result = new PatternStateResult();
        result.baseIssue = safe(baseIssue);

        List<DrawRecord> draws = normalizeDraws(inputDraws);
        result.drawCount = draws.size();

        if (draws.size() == 0) {
            result.stateMode = STATE_UNKNOWN;
            result.summaryText = "状态识别：无历史开奖，无法判断";
            result.b06Evidence = buildB06Evidence(result);
            return result;
        }

        if (result.baseIssue.length() == 0) {
            result.baseIssue = draws.get(draws.size() - 1).issue;
        }

        int i;
        for (i = 0; i < POSITION_COUNT; i++) {
            result.positionPatterns[i] = analyzePosition(draws, i + 1);
        }

        result.bestPosition = chooseBestPosition(result.positionPatterns);
        result.secondPosition = chooseSecondPosition(result.positionPatterns, result.bestPosition);

        PositionPattern best = getPattern(result, result.bestPosition);
        PositionPattern second = getPattern(result, result.secondPosition);

        result.globalTrendStrength = calculateGlobalTrendStrength(result.positionPatterns);
        result.globalTrendSlope = calculateGlobalTrendSlope(result.positionPatterns);
        result.globalEntropy = calculateGlobalEntropy(result.positionPatterns);
        result.globalDriftRisk = calculateGlobalDriftRisk(result.positionPatterns);
        result.globalBreakRisk = calculateGlobalBreakRisk(result.positionPatterns);
        result.globalRecoveryScore = calculateGlobalRecoveryScore(result.positionPatterns);
        result.globalReversalRisk = calculateGlobalReversalRisk(result.positionPatterns);
        result.globalChaosScore = calculateGlobalChaosScore(result.positionPatterns);

        if (best != null && second != null) {
            result.championGap = clamp(best.patternScore - second.patternScore, 0.0D, 1.0D);
        } else {
            result.championGap = 0.0D;
        }

        result.stateMode = detectGlobalState(result);
        result.shapeKey = buildShapeKey(result);
        result.b06Evidence = buildB06Evidence(result);
        result.summaryText = buildResultSummary(result) + "；" + result.b06Evidence.summaryText;

        return result;
    }

    /** 分析单个位置的状态形态。 */
    public PositionPattern analyzePosition(List<DrawRecord> draws, int position) {
        PositionPattern p = new PositionPattern();
        p.position = position;

        if (!isValidPosition(position)) {
            p.stateMode = STATE_UNKNOWN;
            p.summaryText = "位置非法";
            return p;
        }

        if (draws == null || draws.size() == 0) {
            p.stateMode = STATE_UNKNOWN;
            p.summaryText = "无样本";
            return p;
        }

        p.sampleCount = draws.size();

        double[] dist16 = distribution(draws, position, WINDOW_16);
        double[] dist32 = distribution(draws, position, WINDOW_32);
        double[] dist64 = distribution(draws, position, WINDOW_64);
        double[] dist160 = distribution(draws, position, WINDOW_160);

        p.entropy16 = normalizedEntropy(dist16);
        p.entropy32 = normalizedEntropy(dist32);
        p.entropy64 = normalizedEntropy(dist64);
        p.entropy160 = normalizedEntropy(dist160);

        p.top1Mass16 = topMass(dist16, 1);
        p.top1Mass32 = topMass(dist32, 1);
        p.top1Mass64 = topMass(dist64, 1);
        p.top1Mass160 = topMass(dist160, 1);

        p.top3Mass16 = topMass(dist16, 3);
        p.top3Mass32 = topMass(dist32, 3);
        p.top3Mass64 = topMass(dist64, 3);
        p.top3Mass160 = topMass(dist160, 3);

        p.top5Mass32 = topMass(dist32, 5);
        p.top5Mass64 = topMass(dist64, 5);

        p.concentrationScore = calculateConcentrationScore(p);
        p.trendStrength = calculateTrendStrength(p);
        p.trendSlope = calculateTrendSlope(p);
        p.trendLife = calculateTrendLife(draws, position);
        p.trendConsistency = calculateTrendConsistency(draws, position);

        p.driftRisk = calculateDriftRisk(dist32, dist160, p.entropy32, p.entropy160);
        p.breakDepth = calculateBreakDepth(draws, position, dist64);
        p.trendBreakRisk = calculateTrendBreakRisk(p);

        p.recoveryScore = calculateRecoveryScore(p);
        p.accelerationScore = calculateAccelerationScore(p);
        p.weakeningScore = calculateWeakeningScore(p);
        p.chaosScore = calculateChaosScore(p);
        p.reversalRisk = calculateReversalRisk(draws, position, p);

        p.stabilityScore = calculateStabilityScore(p);
        p.patternScore = calculatePatternScore(p);

        p.stateMode = detectPositionState(p);
        p.shapeKey = buildPositionShapeKey(p);
        p.summaryText = buildPositionSummary(p);

        return p;
    }

    /** 判断某个结果是否适合参与实盘。 */
    public boolean isStateRiskHigh(PatternStateResult result) {
        if (result == null) {
            return true;
        }

        if (STATE_CHAOS.equals(result.stateMode)) {
            return true;
        }

        if (STATE_BROKEN.equals(result.stateMode)) {
            return true;
        }

        if (result.globalDriftRisk >= 0.68D) {
            return true;
        }

        if (result.globalBreakRisk >= 0.70D) {
            return true;
        }

        return false;
    }

    /** 判断是否强证据状态。 */
    public boolean isStrongPattern(PatternStateResult result) {
        if (result == null) {
            return false;
        }

        if (!STATE_CONFIRMED.equals(result.stateMode)
                && !STATE_ACCELERATING.equals(result.stateMode)
                && !STATE_RECOVERING.equals(result.stateMode)
                && !STATE_STABLE.equals(result.stateMode)) {
            return false;
        }

        PositionPattern best = getPattern(result, result.bestPosition);
        if (best == null) {
            return false;
        }

        return best.patternScore >= 0.62D
                && best.trendStrength >= 0.55D
                && best.driftRisk <= 0.52D
                && best.breakDepth <= 0.52D
                && result.championGap >= 0.04D;
    }

    /** 根据分析结果给动态门槛一个建议调节值。 负数代表可轻微降低门槛，正数代表应该提高门槛。 */
    public double suggestGateAdjustment(PatternStateResult result) {
        /*
         * B06 owner boundary: pattern state is evidence only.  Older callers keep
         * their API, but this module can no longer lower/raise the formal gate.
         * DecisionGateEngine remains the single action owner.
         */
        return 0.0D;
    }

    private String detectGlobalState(PatternStateResult r) {
        if (r == null || r.drawCount < 32) {
            return STATE_UNKNOWN;
        }

        PositionPattern best = getPattern(r, r.bestPosition);
        if (best == null) {
            return STATE_UNKNOWN;
        }

        double trend = r.globalTrendStrength;
        double drift = r.globalDriftRisk;
        double entropy = r.globalEntropy;
        double breakRisk = r.globalBreakRisk;
        double recovery = r.globalRecoveryScore;
        double reversal = r.globalReversalRisk;
        double chaos = r.globalChaosScore;

        if (chaos >= 0.72D || (entropy >= 0.83D && drift >= 0.60D)) {
            return STATE_CHAOS;
        }

        if (breakRisk >= 0.68D && drift >= 0.52D) {
            return STATE_BROKEN;
        }

        if (best.weakeningScore >= 0.62D && trend <= 0.52D) {
            return STATE_WEAKENING;
        }

        if (reversal >= 0.64D && drift >= 0.48D) {
            return STATE_REVERSAL;
        }

        if (recovery >= 0.58D && breakRisk <= 0.58D && drift <= 0.55D) {
            return STATE_RECOVERING;
        }

        if (best.accelerationScore >= 0.62D && trend >= 0.52D && drift <= 0.58D) {
            return STATE_ACCELERATING;
        }

        if (trend >= 0.62D
                && best.top3Mass32 >= 0.36D
                && best.patternScore >= 0.60D
                && drift <= 0.50D
                && breakRisk <= 0.48D) {
            return STATE_CONFIRMED;
        }

        if (trend >= 0.48D && drift <= 0.46D && entropy <= 0.78D && breakRisk <= 0.52D) {
            return STATE_STABLE;
        }

        return STATE_TRANSITION;
    }

    private String detectPositionState(PositionPattern p) {
        if (p == null || p.sampleCount < 32) {
            return STATE_UNKNOWN;
        }

        if (p.chaosScore >= 0.72D) {
            return STATE_CHAOS;
        }

        if (p.breakDepth >= 0.68D && p.driftRisk >= 0.52D) {
            return STATE_BROKEN;
        }

        if (p.weakeningScore >= 0.64D) {
            return STATE_WEAKENING;
        }

        if (p.reversalRisk >= 0.65D && p.driftRisk >= 0.45D) {
            return STATE_REVERSAL;
        }

        if (p.recoveryScore >= 0.58D && p.breakDepth <= 0.55D) {
            return STATE_RECOVERING;
        }

        if (p.accelerationScore >= 0.62D && p.trendStrength >= 0.50D) {
            return STATE_ACCELERATING;
        }

        if (p.trendStrength >= 0.60D
                && p.top3Mass32 >= 0.36D
                && p.driftRisk <= 0.50D
                && p.breakDepth <= 0.48D) {
            return STATE_CONFIRMED;
        }

        if (p.stabilityScore >= 0.58D && p.driftRisk <= 0.45D) {
            return STATE_STABLE;
        }

        return STATE_TRANSITION;
    }

    private double calculateConcentrationScore(PositionPattern p) {
        double top3 =
                p.top3Mass32 * 0.42D
                        + p.top3Mass64 * 0.34D
                        + p.top3Mass160 * 0.14D
                        + p.top3Mass16 * 0.10D;
        double entropyGood = 1.0D - p.entropy32;
        return clamp(normalize(top3, 0.30D, 0.48D) * 0.72D + entropyGood * 0.28D, 0.0D, 1.0D);
    }

    private double calculateTrendStrength(PositionPattern p) {
        double multiWindow =
                p.top3Mass32 * 0.36D
                        + p.top3Mass64 * 0.34D
                        + p.top3Mass160 * 0.18D
                        + p.top3Mass16 * 0.12D;
        double base = normalize(multiWindow, 0.30D, 0.48D);
        double consistency = 1.0D - clamp(Math.abs(p.top3Mass32 - p.top3Mass64) * 3.5D, 0.0D, 1.0D);
        return clamp(base * 0.76D + consistency * 0.24D, 0.0D, 1.0D);
    }

    private double calculateTrendSlope(PositionPattern p) {
        double shortSlope = p.top3Mass16 - p.top3Mass64;
        double midSlope = p.top3Mass32 - p.top3Mass160;
        return clamp(shortSlope * 3.0D * 0.55D + midSlope * 3.5D * 0.45D, -1.0D, 1.0D);
    }

    private int calculateTrendLife(List<DrawRecord> draws, int position) {
        if (draws == null || draws.size() < 16) {
            return 0;
        }

        int maxCheck = Math.min(128, draws.size());
        int life = 0;
        int window;

        for (window = 16; window <= maxCheck; window += 8) {
            double[] dist = distribution(draws, position, window);
            double mass = topMass(dist, 3);
            double e = normalizedEntropy(dist);

            if (mass >= 0.34D && e <= 0.86D) {
                life = window;
            } else if (life > 0) {
                break;
            }
        }

        return life;
    }

    private double calculateTrendConsistency(List<DrawRecord> draws, int position) {
        if (draws == null || draws.size() < 48) {
            return 0.35D;
        }

        double[] m = new double[4];
        m[0] = topMass(distribution(draws, position, 16), 3);
        m[1] = topMass(distribution(draws, position, 32), 3);
        m[2] = topMass(distribution(draws, position, 64), 3);
        m[3] = topMass(distribution(draws, position, 96), 3);

        double avg = 0.0D;
        int i;
        for (i = 0; i < m.length; i++) {
            avg += m[i];
        }
        avg = avg / m.length;

        double var = 0.0D;
        for (i = 0; i < m.length; i++) {
            double d = m[i] - avg;
            var += d * d;
        }
        var = var / m.length;

        return clamp(1.0D - var * 40.0D, 0.0D, 1.0D);
    }

    private double calculateDriftRisk(
            double[] dist32, double[] dist160, double entropy32, double entropy160) {
        double distance = distributionDistance(dist32, dist160);
        double entropyRise = positive(entropy32 - entropy160);
        return clamp(distance * 0.70D + entropyRise * 0.30D, 0.0D, 1.0D);
    }

    private double calculateBreakDepth(List<DrawRecord> draws, int position, double[] dist64) {
        if (draws == null || draws.size() == 0) {
            return 0.0D;
        }

        int[] rank = rankNumbers(dist64);
        int check = Math.min(12, draws.size());
        int start = draws.size() - check;
        if (start < 0) {
            start = 0;
        }

        int counted = 0;
        double depthSum = 0.0D;

        int i;
        for (i = start; i < draws.size(); i++) {
            int actual = getNumberAt(draws.get(i), position);
            if (!isValidNumber(actual)) {
                continue;
            }

            int layer = findRankLayer(rank, actual);
            if (layer > 3) {
                depthSum += (layer - 3) / 7.0D;
            }
            counted++;
        }

        if (counted <= 0) {
            return 0.0D;
        }

        return clamp(depthSum / counted, 0.0D, 1.0D);
    }

    private double calculateTrendBreakRisk(PositionPattern p) {
        double risk = 0.0D;
        risk += p.breakDepth * 0.42D;
        risk += p.driftRisk * 0.26D;
        risk += positive(0.45D - p.top3Mass32) * 1.4D * 0.16D;
        risk += positive(p.entropy32 - p.entropy160) * 0.16D;
        return clamp(risk, 0.0D, 1.0D);
    }

    private double calculateRecoveryScore(PositionPattern p) {
        double oldStrong = normalize(p.top3Mass160, 0.32D, 0.46D);
        double recentReturn = positive(p.top3Mass32 - p.top3Mass64) * 3.2D;
        double entropyImprove = positive(p.entropy160 - p.entropy32);
        double breakOk = 1.0D - p.breakDepth;
        double driftOk = 1.0D - p.driftRisk;

        return clamp(
                oldStrong * 0.18D
                        + recentReturn * 0.30D
                        + entropyImprove * 0.18D
                        + breakOk * 0.17D
                        + driftOk * 0.17D,
                0.0D,
                1.0D);
    }

    private double calculateAccelerationScore(PositionPattern p) {
        double shortBoost = positive(p.top3Mass16 - p.top3Mass64) * 3.4D;
        double midBoost = positive(p.top3Mass32 - p.top3Mass160) * 3.0D;
        double top1Boost = positive(p.top1Mass32 - p.top1Mass160) * 4.0D;
        double entropyOk = 1.0D - p.entropy32;

        return clamp(
                shortBoost * 0.36D + midBoost * 0.34D + top1Boost * 0.15D + entropyOk * 0.15D,
                0.0D,
                1.0D);
    }

    private double calculateWeakeningScore(PositionPattern p) {
        double topWeak = positive(p.top3Mass160 - p.top3Mass32) * 3.5D;
        double entropyBad = positive(p.entropy32 - p.entropy160);
        double drift = p.driftRisk;
        double breakRisk = p.breakDepth;

        return clamp(
                topWeak * 0.34D + entropyBad * 0.22D + drift * 0.22D + breakRisk * 0.22D,
                0.0D,
                1.0D);
    }

    private double calculateChaosScore(PositionPattern p) {
        double entropyHigh = p.entropy32;
        double drift = p.driftRisk;
        double lowConcentration = 1.0D - p.concentrationScore;
        double lowConsistency = 1.0D - p.trendConsistency;

        return clamp(
                entropyHigh * 0.34D
                        + drift * 0.28D
                        + lowConcentration * 0.22D
                        + lowConsistency * 0.16D,
                0.0D,
                1.0D);
    }

    private double calculateReversalRisk(List<DrawRecord> draws, int position, PositionPattern p) {
        if (draws == null || draws.size() < 24) {
            return 0.0D;
        }

        double[] dist16 = distribution(draws, position, 16);
        double[] dist64 = distribution(draws, position, 64);

        int[] rank64 = rankNumbers(dist64);
        int[] rank16 = rankNumbers(dist16);

        int changedTop3 = 0;
        int i;
        for (i = 0; i < 3; i++) {
            if (!containsTop(rank64, rank16[i], 3)) {
                changedTop3++;
            }
        }

        double topChange = changedTop3 / 3.0D;
        double slopeDown = positive(-p.trendSlope);
        double drift = p.driftRisk;
        double recentBreak = p.breakDepth;

        return clamp(
                topChange * 0.34D + slopeDown * 0.20D + drift * 0.24D + recentBreak * 0.22D,
                0.0D,
                1.0D);
    }

    private double calculateStabilityScore(PositionPattern p) {
        double entropyStable = 1.0D - p.entropy32;
        double driftStable = 1.0D - p.driftRisk;
        double breakStable = 1.0D - p.breakDepth;
        double consistency = p.trendConsistency;

        return clamp(
                entropyStable * 0.26D
                        + driftStable * 0.26D
                        + breakStable * 0.22D
                        + consistency * 0.26D,
                0.0D,
                1.0D);
    }

    private double calculatePatternScore(PositionPattern p) {
        double score = 0.0D;
        score += p.concentrationScore * 0.19D;
        score += p.trendStrength * 0.22D;
        score += p.trendConsistency * 0.11D;
        score += p.recoveryScore * 0.11D;
        score += p.accelerationScore * 0.08D;
        score += p.stabilityScore * 0.14D;
        score += (1.0D - p.driftRisk) * 0.08D;
        score += (1.0D - p.breakDepth) * 0.07D;

        double penalty = p.chaosScore * 0.13D + p.reversalRisk * 0.08D + p.weakeningScore * 0.09D;

        return clamp(score - penalty, 0.0D, 1.0D);
    }

    private int chooseBestPosition(PositionPattern[] patterns) {
        if (patterns == null || patterns.length == 0) {
            return 1;
        }

        int bestPosition = 1;
        double bestScore = -999.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p == null) {
                continue;
            }

            double score = p.patternScore;
            if (score > bestScore) {
                bestScore = score;
                bestPosition = p.position;
            }
        }

        return isValidPosition(bestPosition) ? bestPosition : 1;
    }

    private int chooseSecondPosition(PositionPattern[] patterns, int bestPosition) {
        if (patterns == null || patterns.length == 0) {
            return 1;
        }

        int secondPosition = bestPosition == 1 ? 2 : 1;
        double secondScore = -999.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p == null) {
                continue;
            }
            if (p.position == bestPosition) {
                continue;
            }

            double score = p.patternScore;
            if (score > secondScore) {
                secondScore = score;
                secondPosition = p.position;
            }
        }

        return isValidPosition(secondPosition) ? secondPosition : 1;
    }

    private PositionPattern getPattern(PatternStateResult result, int position) {
        if (result == null || !isValidPosition(position)) {
            return null;
        }
        if (result.positionPatterns == null || result.positionPatterns.length < position) {
            return null;
        }
        return result.positionPatterns[position - 1];
    }

    private double calculateGlobalTrendStrength(PositionPattern[] patterns) {
        double top1 = 0.0D;
        double top2 = 0.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p == null) {
                continue;
            }

            double v = p.trendStrength;
            if (v > top1) {
                top2 = top1;
                top1 = v;
            } else if (v > top2) {
                top2 = v;
            }
        }

        return clamp(top1 * 0.70D + top2 * 0.30D, 0.0D, 1.0D);
    }

    private double calculateGlobalTrendSlope(PositionPattern[] patterns) {
        double sum = 0.0D;
        int count = 0;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null) {
                sum += p.trendSlope;
                count++;
            }
        }

        if (count <= 0) {
            return 0.0D;
        }

        return clamp(sum / count, -1.0D, 1.0D);
    }

    private double calculateGlobalEntropy(PositionPattern[] patterns) {
        double sum = 0.0D;
        int count = 0;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null) {
                sum += p.entropy32;
                count++;
            }
        }

        if (count <= 0) {
            return 1.0D;
        }

        return clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateGlobalDriftRisk(PositionPattern[] patterns) {
        double sum = 0.0D;
        int count = 0;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null) {
                sum += p.driftRisk;
                count++;
            }
        }

        if (count <= 0) {
            return 0.0D;
        }

        return clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateGlobalBreakRisk(PositionPattern[] patterns) {
        double sum = 0.0D;
        int count = 0;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null) {
                sum += p.breakDepth;
                count++;
            }
        }

        if (count <= 0) {
            return 0.0D;
        }

        return clamp(sum / count, 0.0D, 1.0D);
    }

    private double calculateGlobalRecoveryScore(PositionPattern[] patterns) {
        double best = 0.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null && p.recoveryScore > best) {
                best = p.recoveryScore;
            }
        }

        return clamp(best, 0.0D, 1.0D);
    }

    private double calculateGlobalReversalRisk(PositionPattern[] patterns) {
        double best = 0.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null && p.reversalRisk > best) {
                best = p.reversalRisk;
            }
        }

        return clamp(best, 0.0D, 1.0D);
    }

    private double calculateGlobalChaosScore(PositionPattern[] patterns) {
        double sum = 0.0D;
        int count = 0;
        double best = 0.0D;

        int i;
        for (i = 0; i < patterns.length; i++) {
            PositionPattern p = patterns[i];
            if (p != null) {
                sum += p.chaosScore;
                if (p.chaosScore > best) {
                    best = p.chaosScore;
                }
                count++;
            }
        }

        if (count <= 0) {
            return 0.0D;
        }

        return clamp((sum / count) * 0.65D + best * 0.35D, 0.0D, 1.0D);
    }

    private String buildShapeKey(PatternStateResult r) {
        if (r == null) {
            return "SHAPE_UNKNOWN";
        }

        PositionPattern best = getPattern(r, r.bestPosition);

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(r.bestPosition);
        sb.append("_").append(safe(r.stateMode));
        sb.append("_T").append(bucket10(r.globalTrendStrength));
        sb.append("_D").append(bucket10(r.globalDriftRisk));
        sb.append("_B").append(bucket10(r.globalBreakRisk));
        sb.append("_R").append(bucket10(r.globalRecoveryScore));
        sb.append("_E").append(bucket10(r.globalEntropy));
        sb.append("_G").append(bucket10(r.championGap));

        if (best != null) {
            sb.append("_L").append(best.trendLife);
        }

        return sb.toString();
    }

    private String buildPositionShapeKey(PositionPattern p) {
        if (p == null) {
            return "P0_UNKNOWN";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("P").append(p.position);
        sb.append("_").append(safe(p.stateMode));
        sb.append("_T").append(bucket10(p.trendStrength));
        sb.append("_S").append(bucket10(p.trendSlopePositive()));
        sb.append("_D").append(bucket10(p.driftRisk));
        sb.append("_B").append(bucket10(p.breakDepth));
        sb.append("_R").append(bucket10(p.recoveryScore));
        sb.append("_C").append(bucket10(p.concentrationScore));
        return sb.toString();
    }

    private String buildResultSummary(PatternStateResult r) {
        StringBuilder sb = new StringBuilder();
        sb.append("形态状态：");
        sb.append(r.stateMode);
        sb.append(" | 样本=").append(r.drawCount);
        sb.append(" | 最强位置=").append(r.bestPosition);
        sb.append(" | 第二位置=").append(r.secondPosition);
        sb.append(" | 冠军差=").append(fmt(r.championGap));
        sb.append(" | 趋势=").append(fmt(r.globalTrendStrength));
        sb.append(" | 走势斜率=").append(fmtSigned(r.globalTrendSlope));
        sb.append(" | 漂移=").append(fmt(r.globalDriftRisk));
        sb.append(" | 破位=").append(fmt(r.globalBreakRisk));
        sb.append(" | 恢复=").append(fmt(r.globalRecoveryScore));
        sb.append(" | 反转=").append(fmt(r.globalReversalRisk));
        sb.append(" | 混沌=").append(fmt(r.globalChaosScore));
        sb.append(" | shape=").append(r.shapeKey);

        PositionPattern best = getPattern(r, r.bestPosition);
        if (best != null) {
            sb.append(" | 最强说明：").append(best.summaryText);
        }

        return sb.toString();
    }

    private String buildPositionSummary(PositionPattern p) {
        StringBuilder sb = new StringBuilder();
        sb.append("P").append(p.position);
        sb.append(" 状态=").append(p.stateMode);
        sb.append(" 分=").append(fmt(p.patternScore));
        sb.append(" Top3=").append(fmt(p.top3Mass32));
        sb.append(" 趋势=").append(fmt(p.trendStrength));
        sb.append(" 斜率=").append(fmtSigned(p.trendSlope));
        sb.append(" 寿命=").append(p.trendLife);
        sb.append(" 一致=").append(fmt(p.trendConsistency));
        sb.append(" 漂移=").append(fmt(p.driftRisk));
        sb.append(" 破位=").append(fmt(p.breakDepth));
        sb.append(" 恢复=").append(fmt(p.recoveryScore));
        sb.append(" 加速=").append(fmt(p.accelerationScore));
        sb.append(" 转弱=").append(fmt(p.weakeningScore));
        sb.append(" 反转=").append(fmt(p.reversalRisk));
        sb.append(" 混沌=").append(fmt(p.chaosScore));
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

            DrawRecord copy = new DrawRecord();
            copy.issue = safe(d.issue);
            copy.openCode = formatNumbers(nums);
            copy.openTimeMs = d.openTimeMs;
            list.add(copy);
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

    private double[] distribution(List<DrawRecord> draws, int position, int window) {
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
            int n = getNumberAt(draws.get(i), position);
            if (isValidNumber(n)) {
                count[n - 1] += 1.0D;
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

    private double normalizedEntropy(double[] dist) {
        double e = entropy(dist);
        double max = Math.log(10.0D);
        if (max <= EPS) {
            return 1.0D;
        }
        return clamp(e / max, 0.0D, 1.0D);
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

    private double topMass(double[] dist, int topN) {
        if (dist == null || dist.length == 0 || topN <= 0) {
            return 0.0D;
        }

        double[] copy = new double[dist.length];
        int i;
        for (i = 0; i < dist.length; i++) {
            copy[i] = dist[i];
        }

        int a;
        int b;
        for (a = 0; a < copy.length - 1; a++) {
            for (b = a + 1; b < copy.length; b++) {
                if (copy[b] > copy[a]) {
                    double t = copy[a];
                    copy[a] = copy[b];
                    copy[b] = t;
                }
            }
        }

        double sum = 0.0D;
        int limit = Math.min(topN, copy.length);
        for (i = 0; i < limit; i++) {
            sum += copy[i];
        }

        return clamp(sum, 0.0D, 1.0D);
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

    private int[] rankNumbers(double[] dist) {
        ArrayList<RankItem> list = new ArrayList<RankItem>();

        int i;
        for (i = 0; i < NUMBER_COUNT; i++) {
            RankItem item = new RankItem();
            item.number = i + 1;
            if (dist != null && i < dist.length) {
                item.value = dist[i];
            } else {
                item.value = 0.0D;
            }
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

    private int findRankLayer(int[] rank, int number) {
        if (rank == null || !isValidNumber(number)) {
            return 10;
        }

        int i;
        for (i = 0; i < rank.length; i++) {
            if (rank[i] == number) {
                return i + 1;
            }
        }

        return 10;
    }

    private boolean containsTop(int[] rank, int number, int topN) {
        if (rank == null || !isValidNumber(number)) {
            return false;
        }

        int limit = Math.min(topN, rank.length);
        int i;
        for (i = 0; i < limit; i++) {
            if (rank[i] == number) {
                return true;
            }
        }

        return false;
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

    private static boolean isValidPosition(int position) {
        return position >= 1 && position <= POSITION_COUNT;
    }

    private static boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private static double normalize(double value, double low, double high) {
        if (high <= low) {
            return 0.0D;
        }
        return clamp((value - low) / (high - low), 0.0D, 1.0D);
    }

    private static double positive(double v) {
        return v > 0.0D ? v : 0.0D;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int bucket10(double value) {
        int b = (int) Math.floor(clamp(value, 0.0D, 1.0D) * 10.0D);
        if (b < 0) {
            b = 0;
        }
        if (b > 9) {
            b = 9;
        }
        return b;
    }

    private static String fmt(double value) {
        int v = (int) Math.round(clamp(value, 0.0D, 1.0D) * 100.0D);
        return String.valueOf(v) + "%";
    }

    private static String fmtSigned(double value) {
        int v = (int) Math.round(value * 100.0D);
        if (v > 0) {
            return "+" + v + "%";
        }
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

    public static final class PatternStateResult {
        public String baseIssue;
        public int drawCount;

        public PositionPattern[] positionPatterns;

        public int bestPosition;
        public int secondPosition;

        public double championGap;
        public double globalTrendStrength;
        public double globalTrendSlope;
        public double globalEntropy;
        public double globalDriftRisk;
        public double globalBreakRisk;
        public double globalRecoveryScore;
        public double globalReversalRisk;
        public double globalChaosScore;

        public String stateMode;
        public String shapeKey;
        public String summaryText;
        public B06PatternEvidenceBundle b06Evidence;

        public PatternStateResult() {
            this.baseIssue = "";
            this.drawCount = 0;
            this.positionPatterns = new PositionPattern[POSITION_COUNT];
            this.bestPosition = 1;
            this.secondPosition = 2;
            this.championGap = 0.0D;
            this.globalTrendStrength = 0.0D;
            this.globalTrendSlope = 0.0D;
            this.globalEntropy = 1.0D;
            this.globalDriftRisk = 0.0D;
            this.globalBreakRisk = 0.0D;
            this.globalRecoveryScore = 0.0D;
            this.globalReversalRisk = 0.0D;
            this.globalChaosScore = 0.0D;
            this.stateMode = STATE_UNKNOWN;
            this.shapeKey = "";
            this.summaryText = "";
            this.b06Evidence = new B06PatternEvidenceBundle();
        }
    }

    public static final class PositionPattern {
        public int position;
        public int sampleCount;

        public double entropy16;
        public double entropy32;
        public double entropy64;
        public double entropy160;

        public double top1Mass16;
        public double top1Mass32;
        public double top1Mass64;
        public double top1Mass160;

        public double top3Mass16;
        public double top3Mass32;
        public double top3Mass64;
        public double top3Mass160;

        public double top5Mass32;
        public double top5Mass64;

        public double concentrationScore;

        public double trendStrength;
        public double trendSlope;
        public int trendLife;
        public double trendConsistency;
        public double trendBreakRisk;

        public double driftRisk;
        public double breakDepth;
        public double recoveryScore;
        public double accelerationScore;
        public double weakeningScore;
        public double chaosScore;
        public double reversalRisk;
        public double stabilityScore;
        public double patternScore;

        public String stateMode;
        public String shapeKey;
        public String summaryText;

        public PositionPattern() {
            this.position = 0;
            this.sampleCount = 0;
            this.stateMode = STATE_UNKNOWN;
            this.shapeKey = "";
            this.summaryText = "";
        }

        public double trendSlopePositive() {
            return (trendSlope + 1.0D) / 2.0D;
        }
    }

    public B06PatternEvidenceBundle buildB06Evidence(PatternStateResult result) {
        B06PatternEvidenceBundle bundle = new B06PatternEvidenceBundle();
        bundle.baseIssue = result == null ? "" : safe(result.baseIssue);
        bundle.contractVersion = B06_CONTRACT_VERSION;
        bundle.owner = B06_OWNER;
        if (result == null || result.positionPatterns == null) {
            bundle.evidenceReady = 0;
            bundle.summaryText = "B06形态证据：结果为空，不参与概率和BET";
            bundle.fingerprint = V51ChecksumEngine.sha256(bundle.contractVersion + "|EMPTY");
            return bundle;
        }
        double supportSum = 0.0D;
        double counterSum = 0.0D;
        double riskSum = 0.0D;
        double confidenceSum = 0.0D;
        double freshnessSum = 0.0D;
        int valid = 0;
        for (int i = 0; i < result.positionPatterns.length; i++) {
            PositionPattern p = result.positionPatterns[i];
            if (p == null || !isValidPosition(p.position)) continue;
            B06PatternEvidencePacket packet = buildB06PatternPacket(result, p);
            bundle.packets[p.position - 1] = packet;
            supportSum += packet.support;
            counterSum += packet.counterEvidence;
            riskSum += packet.risk;
            confidenceSum += packet.confidence;
            freshnessSum += packet.freshness;
            valid++;
        }
        bundle.packetCount = valid;
        bundle.support = valid > 0 ? supportSum / valid : 0.0D;
        bundle.counterEvidence = valid > 0 ? counterSum / valid : 1.0D;
        bundle.risk = valid > 0 ? riskSum / valid : 1.0D;
        bundle.confidence = valid > 0 ? confidenceSum / valid : 0.0D;
        bundle.freshness = valid > 0 ? freshnessSum / valid : 0.0D;
        bundle.sample = result.drawCount;
        bundle.diversity = clamp((double) valid / (double) POSITION_COUNT, 0.0D, 1.0D);
        bundle.evidenceReady = valid == POSITION_COUNT && result.drawCount >= WINDOW_32 ? 1 : 0;
        bundle.summaryText =
                "B06形态证据：支持="
                        + fmt(bundle.support)
                        + "，反证="
                        + fmt(bundle.counterEvidence)
                        + "，风险="
                        + fmt(bundle.risk)
                        + "，置信="
                        + fmt(bundle.confidence)
                        + "，样本="
                        + bundle.sample
                        + "，所有权=EvidenceOnly";
        bundle.fingerprint = V51ChecksumEngine.sha256(bundle.stablePayload());
        return bundle;
    }

    private B06PatternEvidencePacket buildB06PatternPacket(
            PatternStateResult result, PositionPattern p) {
        B06PatternEvidencePacket packet = new B06PatternEvidencePacket();
        packet.baseIssue = result == null ? "" : safe(result.baseIssue);
        packet.position = p.position;
        packet.stateMode = safe(p.stateMode);
        packet.shapeKey = safe(p.shapeKey);
        packet.sample = p.sampleCount;
        packet.support =
                clamp(
                        p.patternScore * 0.30D
                                + p.trendStrength * 0.22D
                                + p.trendConsistency * 0.16D
                                + p.recoveryScore * 0.12D
                                + p.stabilityScore * 0.20D,
                        0.0D,
                        1.0D);
        packet.counterEvidence =
                clamp(
                        p.weakeningScore * 0.22D
                                + p.reversalRisk * 0.24D
                                + p.chaosScore * 0.24D
                                + p.trendBreakRisk * 0.30D,
                        0.0D,
                        1.0D);
        packet.risk =
                clamp(
                        p.driftRisk * 0.28D
                                + p.breakDepth * 0.25D
                                + p.reversalRisk * 0.19D
                                + p.chaosScore * 0.28D,
                        0.0D,
                        1.0D);
        packet.confidence =
                clamp(
                        Math.min(1.0D, p.sampleCount / (double) WINDOW_160) * 0.45D
                                + p.stabilityScore * 0.25D
                                + p.trendConsistency * 0.20D
                                + (1.0D - packet.risk) * 0.10D,
                        0.0D,
                        1.0D);
        packet.freshness = clamp(1.0D - Math.abs(p.entropy16 - p.entropy32), 0.0D, 1.0D);
        packet.usable = packet.sample >= WINDOW_32 && packet.confidence >= 0.42D ? 1 : 0;
        packet.ablationGroup = "B06_PATTERN_STATE";
        packet.reason = "支持来自趋势/稳定/恢复，反证来自转弱/反转/破位/混沌；不直接改FinalP或Gate";
        packet.fingerprint = V51ChecksumEngine.sha256(packet.stablePayload());
        return packet;
    }

    public static final class B06PatternEvidencePacket {
        public String baseIssue = "";
        public int position = 0;
        public String stateMode = STATE_UNKNOWN;
        public String shapeKey = "";
        public double support = 0.0D;
        public double counterEvidence = 1.0D;
        public double risk = 1.0D;
        public double confidence = 0.0D;
        public int sample = 0;
        public double freshness = 0.0D;
        public int usable = 0;
        public String ablationGroup = "B06_PATTERN_STATE";
        public String reason = "";
        public String fingerprint = "";

        public String stablePayload() {
            return B06_CONTRACT_VERSION
                    + "|"
                    + baseIssue
                    + "|"
                    + position
                    + "|"
                    + stateMode
                    + "|"
                    + shapeKey
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

    public static final class B06PatternEvidenceBundle {
        public String contractVersion = B06_CONTRACT_VERSION;
        public String owner = B06_OWNER;
        public String baseIssue = "";
        public B06PatternEvidencePacket[] packets = new B06PatternEvidencePacket[POSITION_COUNT];
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
            for (int i = 0; i < packets.length; i++) {
                B06PatternEvidencePacket p = packets[i];
                sb.append('|').append(p == null ? "null" : p.fingerprint);
            }
            return sb.toString();
        }
    }
}
