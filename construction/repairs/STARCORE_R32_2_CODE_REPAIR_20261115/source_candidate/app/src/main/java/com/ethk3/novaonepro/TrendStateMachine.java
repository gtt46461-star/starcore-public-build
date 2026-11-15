package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class TrendStateMachine {

    public static final String TRANSITION_UNKNOWN = "UNKNOWN";
    public static final String TRANSITION_STABLE = "STABLE";
    public static final String TRANSITION_ACCELERATE = "ACCELERATE";
    public static final String TRANSITION_PULLBACK = "PULLBACK";
    public static final String TRANSITION_RECOVERY = "RECOVERY";
    public static final String TRANSITION_WEAKEN = "WEAKEN";
    public static final String TRANSITION_BREAK = "BREAK";
    public static final String TRANSITION_REVERSE = "REVERSE";
    public static final String TRANSITION_CHAOS = "CHAOS";

    public TrendStateMachine() {}

    public TrendTrace analyzePosition(
            List<Models.ShapeVector> recentVectors, Models.ShapeVector current) {
        TrendTrace trace = new TrendTrace();

        if (current == null || !AppConfig.isValidPosition(current.position)) {
            trace.position = 0;
            trace.currentMode = AppConfig.MODE_CHAOS;
            trace.transitionCode = TRANSITION_CHAOS;
            trace.detail = "当前形态为空，状态机强制混乱保护";
            trace.hardBlock = 1;
            trace.shouldTighten = 1;
            return trace;
        }

        trace.position = current.position;
        trace.currentMode = safeMode(current.stateMode);
        trace.currentQuality = quality(current);

        ArrayList<Models.ShapeVector> list = filterSamePosition(recentVectors, current.position);

        if (list.size() == 0) {
            trace.previousMode = "";
            trace.transitionCode = TRANSITION_UNKNOWN;
            trace.sameModeCount = 1;
            trace.confirmCount = isGoodMode(trace.currentMode) ? 1 : 0;
            trace.stateStability = 45.0d;
            trace.trendContinuity = 45.0d;
            trace.modeSwitchRisk = 35.0d;
            trace.earlyBreakRisk = current.breakDepth;
            trace.recoveryMomentum = current.recoveryScore;
            trace.detail = "历史状态不足，只能按当前形态保守判断";
            applyProtection(trace, current);
            return trace;
        }

        Models.ShapeVector previous = list.get(list.size() - 1);
        trace.previousMode = safeMode(previous.stateMode);

        countStateRuns(list, current, trace);
        computeAverages(list, current, trace);
        computeRisks(list, current, trace);
        decideTransition(previous, current, trace);
        applyProtection(trace, current);
        buildDetail(current, trace);

        return trace;
    }

    public ArrayList<TrendTrace> analyzeAll(
            List<Models.ShapeVector> recentVectors, List<Models.ShapeVector> currentVectors) {
        ArrayList<TrendTrace> out = new ArrayList<TrendTrace>();

        if (currentVectors == null || currentVectors.size() == 0) {
            return out;
        }

        for (int i = 0; i < currentVectors.size(); i++) {
            Models.ShapeVector v = currentVectors.get(i);
            if (v == null) {
                continue;
            }
            out.add(analyzePosition(recentVectors, v));
        }

        return out;
    }

    public Models.ShapeVector refineVectorByTrace(Models.ShapeVector source, TrendTrace trace) {
        if (source == null) {
            return null;
        }

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

        if (trace == null) {
            return v;
        }

        if (trace.hardBlock == 1) {
            if (TRANSITION_BREAK.equals(trace.transitionCode)) {
                v.stateMode = AppConfig.MODE_BROKEN;
            } else if (TRANSITION_REVERSE.equals(trace.transitionCode)) {
                v.stateMode = AppConfig.MODE_REVERSING;
            } else {
                v.stateMode = AppConfig.MODE_CHAOS;
            }
        } else {
            if (TRANSITION_ACCELERATE.equals(trace.transitionCode)
                    && v.trendLife >= AppConfig.TREND_LIFE_HIGH
                    && v.driftRisk <= 30.0d) {
                v.stateMode = AppConfig.MODE_ACCELERATING;
            } else if (TRANSITION_RECOVERY.equals(trace.transitionCode)
                    && v.recoveryScore >= AppConfig.RECOVERY_SCORE_STRONG
                    && v.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
                v.stateMode = AppConfig.MODE_RECOVERING;
            } else if (TRANSITION_STABLE.equals(trace.transitionCode)
                    && trace.confirmCount >= 3
                    && v.trendLife >= AppConfig.TREND_LIFE_MIN
                    && v.driftRisk <= AppConfig.DRIFT_RISK_MAX) {
                v.stateMode = AppConfig.MODE_CONFIRMED;
            } else if (TRANSITION_PULLBACK.equals(trace.transitionCode)) {
                v.stateMode = AppConfig.MODE_PULLBACK;
            } else if (TRANSITION_WEAKEN.equals(trace.transitionCode)) {
                v.stateMode = AppConfig.MODE_WEAKENING;
            }
        }

        v.shapeKey = ShapeMath.buildShapeKey(v);
        return v;
    }

    public ArrayList<Models.ShapeVector> refineAllByTrace(
            List<Models.ShapeVector> recentVectors, List<Models.ShapeVector> currentVectors) {
        ArrayList<Models.ShapeVector> out = new ArrayList<Models.ShapeVector>();

        if (currentVectors == null || currentVectors.size() == 0) {
            return out;
        }

        for (int i = 0; i < currentVectors.size(); i++) {
            Models.ShapeVector v = currentVectors.get(i);
            TrendTrace trace = analyzePosition(recentVectors, v);
            Models.ShapeVector refined = refineVectorByTrace(v, trace);
            if (refined != null) {
                out.add(refined);
            }
        }

        return out;
    }

    public double gateAdjustment(TrendTrace trace) {
        if (trace == null) {
            return 0.030d;
        }

        double adjust = 0.0d;

        if (trace.hardBlock == 1) {
            return 0.060d;
        }

        if (trace.shouldTighten == 1) {
            adjust += 0.020d;
        }

        if (trace.shouldLoosen == 1) {
            adjust -= 0.006d;
        }

        if (trace.modeSwitchRisk >= 55.0d) {
            adjust += 0.018d;
        } else if (trace.modeSwitchRisk <= 20.0d) {
            adjust -= 0.004d;
        }

        if (trace.trendContinuity >= 75.0d && trace.confirmCount >= 3) {
            adjust -= 0.006d;
        }

        if (trace.earlyBreakRisk >= 45.0d) {
            adjust += 0.018d;
        }

        if (trace.recoveryMomentum >= 74.0d
                && TRANSITION_RECOVERY.equals(trace.transitionCode)
                && trace.modeSwitchRisk <= 38.0d) {
            adjust -= 0.003d;
        }

        if (adjust < -0.012d) {
            adjust = -0.012d;
        }

        if (adjust > 0.065d) {
            adjust = 0.065d;
        }

        return adjust;
    }

    public boolean isTrendUsableForBet(TrendTrace trace) {
        if (trace == null) {
            return false;
        }

        if (trace.hardBlock == 1) {
            return false;
        }

        if (TRANSITION_BREAK.equals(trace.transitionCode)
                || TRANSITION_REVERSE.equals(trace.transitionCode)
                || TRANSITION_CHAOS.equals(trace.transitionCode)) {
            return false;
        }

        if (trace.modeSwitchRisk >= 55.0d) {
            return false;
        }

        if (trace.earlyBreakRisk >= 48.0d) {
            return false;
        }

        if (trace.trendContinuity >= 68.0d && trace.confirmCount >= 2) {
            return true;
        }

        if (TRANSITION_RECOVERY.equals(trace.transitionCode)
                && trace.recoveryMomentum >= AppConfig.RECOVERY_SCORE_STRONG
                && trace.modeSwitchRisk <= 38.0d) {
            return true;
        }

        return false;
    }

    public String bestTraceSummary(List<TrendTrace> traces) {
        if (traces == null || traces.size() == 0) {
            return "趋势状态机暂无数据";
        }

        TrendTrace best = null;
        double bestScore = -1.0d;

        for (int i = 0; i < traces.size(); i++) {
            TrendTrace t = traces.get(i);
            if (t == null) {
                continue;
            }

            double score =
                    t.trendContinuity
                            - t.modeSwitchRisk * 0.45d
                            - t.earlyBreakRisk * 0.30d
                            + t.recoveryMomentum * 0.20d;

            if (best == null || score > bestScore) {
                best = t;
                bestScore = score;
            }
        }

        if (best == null) {
            return "趋势状态机暂无有效位置";
        }

        return "P"
                + best.position
                + " "
                + best.transitionCode
                + " 连续="
                + best.sameModeCount
                + " 确认="
                + best.confirmCount
                + " 稳定="
                + UiFormatter.score(best.stateStability)
                + " 切换风险="
                + UiFormatter.score(best.modeSwitchRisk);
    }

    public TrendTrace findTraceForPosition(List<TrendTrace> traces, int position) {
        if (traces == null || !AppConfig.isValidPosition(position)) {
            return null;
        }

        for (int i = 0; i < traces.size(); i++) {
            TrendTrace t = traces.get(i);
            if (t != null && t.position == position) {
                return t;
            }
        }

        return null;
    }

    private ArrayList<Models.ShapeVector> filterSamePosition(
            List<Models.ShapeVector> input, int position) {
        ArrayList<Models.ShapeVector> list = new ArrayList<Models.ShapeVector>();

        if (input == null || !AppConfig.isValidPosition(position)) {
            return list;
        }

        for (int i = 0; i < input.size(); i++) {
            Models.ShapeVector v = input.get(i);
            if (v == null) {
                continue;
            }

            if (v.position == position) {
                list.add(v);
            }
        }

        return list;
    }

    private void countStateRuns(
            ArrayList<Models.ShapeVector> list, Models.ShapeVector current, TrendTrace trace) {
        String currentMode = safeMode(current.stateMode);

        trace.sameModeCount = 1;
        trace.confirmCount = isGoodMode(currentMode) ? 1 : 0;
        trace.breakCount = isBadMode(currentMode) ? 1 : 0;
        trace.recoverCount = AppConfig.MODE_RECOVERING.equals(currentMode) ? 1 : 0;
        trace.flipCount = 0;

        String lastMode = currentMode;

        for (int i = list.size() - 1; i >= 0; i--) {
            Models.ShapeVector v = list.get(i);
            String mode = safeMode(v.stateMode);

            if (mode.equals(currentMode)) {
                trace.sameModeCount++;
            } else {
                break;
            }
        }

        int scan = Math.min(12, list.size());

        for (int i = list.size() - scan; i < list.size(); i++) {
            if (i < 0) {
                continue;
            }

            Models.ShapeVector v = list.get(i);
            String mode = safeMode(v.stateMode);

            if (isGoodMode(mode)) {
                trace.confirmCount++;
            }

            if (isBadMode(mode)) {
                trace.breakCount++;
            }

            if (AppConfig.MODE_RECOVERING.equals(mode)) {
                trace.recoverCount++;
            }

            if (!mode.equals(lastMode)) {
                trace.flipCount++;
            }

            lastMode = mode;
        }
    }

    private void computeAverages(
            ArrayList<Models.ShapeVector> list, Models.ShapeVector current, TrendTrace trace) {
        int scan = Math.min(12, list.size());
        double qualitySum = quality(current);
        double continuitySum = 0.0d;
        int qualityCount = 1;

        for (int i = list.size() - scan; i < list.size(); i++) {
            if (i < 0) {
                continue;
            }

            Models.ShapeVector v = list.get(i);
            qualitySum += quality(v);
            qualityCount++;
        }

        trace.avgQuality = qualityCount > 0 ? qualitySum / qualityCount : quality(current);

        String currentMode = safeMode(current.stateMode);

        for (int i = list.size() - scan; i < list.size(); i++) {
            if (i < 0) {
                continue;
            }

            Models.ShapeVector v = list.get(i);
            String mode = safeMode(v.stateMode);

            if (mode.equals(currentMode)) {
                continuitySum += 1.0d;
            } else if (isGoodMode(mode) && isGoodMode(currentMode)) {
                continuitySum += 0.75d;
            } else if (AppConfig.MODE_PULLBACK.equals(mode)
                    && AppConfig.MODE_RECOVERING.equals(currentMode)) {
                continuitySum += 0.65d;
            } else if (AppConfig.MODE_FORMING.equals(mode) && isGoodMode(currentMode)) {
                continuitySum += 0.55d;
            } else {
                continuitySum += 0.15d;
            }
        }

        double baseContinuity = scan > 0 ? continuitySum / scan * 100.0d : 45.0d;
        baseContinuity += Math.min(18.0d, trace.sameModeCount * 3.0d);

        if (trace.confirmCount >= 4) {
            baseContinuity += 8.0d;
        }

        trace.trendContinuity = ShapeMath.clamp100(baseContinuity);
        trace.stateStability =
                ShapeMath.clamp100(
                        trace.trendContinuity * 0.65d + (100.0d - trace.flipCount * 10.0d) * 0.35d);
    }

    private void computeRisks(
            ArrayList<Models.ShapeVector> list, Models.ShapeVector current, TrendTrace trace) {
        int scan = Math.min(12, list.size());

        double switchRisk = trace.flipCount * 9.0d;

        if (trace.sameModeCount <= 1) {
            switchRisk += 18.0d;
        }

        if (trace.breakCount >= 3) {
            switchRisk += 12.0d;
        }

        if (trace.currentQuality < trace.avgQuality - 8.0d) {
            switchRisk += 10.0d;
        }

        if (scan < 4) {
            switchRisk += 12.0d;
        }

        trace.modeSwitchRisk = ShapeMath.clamp100(switchRisk);

        double breakRisk =
                current.breakDepth * 0.55d
                        + current.driftRisk * 0.25d
                        + ShapeMath.negativePartAbs(current.slopeShort) * 500.0d
                        + ShapeMath.negativePartAbs(current.slopeMid) * 250.0d;

        if (current.trendLife < AppConfig.TREND_LIFE_MIN) {
            breakRisk += (AppConfig.TREND_LIFE_MIN - current.trendLife) * 0.5d;
        }

        if (current.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            breakRisk += (current.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) * 0.5d;
        }

        trace.earlyBreakRisk = ShapeMath.clamp100(breakRisk);

        double recovery =
                current.recoveryScore * 0.55d
                        + ShapeMath.positivePart(current.slopeShort) * 420.0d
                        + ShapeMath.positivePart(current.slopeMid) * 180.0d
                        + (100.0d - current.driftRisk) * 0.12d
                        + (100.0d - current.breakDepth) * 0.10d;

        if (trace.recoverCount >= 2) {
            recovery += 6.0d;
        }

        if (trace.breakCount >= 4 && current.recoveryScore < AppConfig.RECOVERY_SCORE_STRONG) {
            recovery -= 8.0d;
        }

        trace.recoveryMomentum = ShapeMath.clamp100(recovery);
    }

    private void decideTransition(
            Models.ShapeVector previous, Models.ShapeVector current, TrendTrace trace) {
        String prev = safeMode(previous.stateMode);
        String cur = safeMode(current.stateMode);

        if (AppConfig.MODE_CHAOS.equals(cur)) {
            trace.transitionCode = TRANSITION_CHAOS;
            return;
        }

        if (AppConfig.MODE_REVERSING.equals(cur)) {
            trace.transitionCode = TRANSITION_REVERSE;
            return;
        }

        if (AppConfig.MODE_BROKEN.equals(cur)) {
            trace.transitionCode = TRANSITION_BREAK;
            return;
        }

        if (AppConfig.MODE_ACCELERATING.equals(cur)) {
            if (isGoodMode(prev) || AppConfig.MODE_FORMING.equals(prev)) {
                trace.transitionCode = TRANSITION_ACCELERATE;
            } else {
                trace.transitionCode = TRANSITION_RECOVERY;
            }
            return;
        }

        if (AppConfig.MODE_CONFIRMED.equals(cur)) {
            if (isGoodMode(prev)) {
                trace.transitionCode = TRANSITION_STABLE;
            } else if (AppConfig.MODE_RECOVERING.equals(prev)
                    || AppConfig.MODE_PULLBACK.equals(prev)) {
                trace.transitionCode = TRANSITION_RECOVERY;
            } else {
                trace.transitionCode = TRANSITION_STABLE;
            }
            return;
        }

        if (AppConfig.MODE_RECOVERING.equals(cur)) {
            trace.transitionCode = TRANSITION_RECOVERY;
            return;
        }

        if (AppConfig.MODE_PULLBACK.equals(cur)) {
            trace.transitionCode = TRANSITION_PULLBACK;
            return;
        }

        if (AppConfig.MODE_WEAKENING.equals(cur)) {
            trace.transitionCode = TRANSITION_WEAKEN;
            return;
        }

        trace.transitionCode = TRANSITION_UNKNOWN;
    }

    private void applyProtection(TrendTrace trace, Models.ShapeVector current) {
        trace.shouldTighten = 0;
        trace.shouldLoosen = 0;
        trace.hardBlock = 0;

        if (current == null) {
            trace.hardBlock = 1;
            trace.shouldTighten = 1;
            return;
        }

        if (TRANSITION_CHAOS.equals(trace.transitionCode)
                || TRANSITION_BREAK.equals(trace.transitionCode)
                || TRANSITION_REVERSE.equals(trace.transitionCode)) {
            trace.hardBlock = 1;
            trace.shouldTighten = 1;
            return;
        }

        if (trace.modeSwitchRisk >= 60.0d) {
            trace.hardBlock = 1;
            trace.shouldTighten = 1;
            return;
        }

        if (trace.earlyBreakRisk >= 60.0d) {
            trace.hardBlock = 1;
            trace.shouldTighten = 1;
            return;
        }

        if (trace.modeSwitchRisk >= 45.0d || trace.earlyBreakRisk >= 45.0d) {
            trace.shouldTighten = 1;
        }

        if (trace.trendContinuity >= 78.0d
                && trace.stateStability >= 72.0d
                && trace.confirmCount >= 3
                && trace.modeSwitchRisk <= 25.0d
                && trace.earlyBreakRisk <= 32.0d) {
            trace.shouldLoosen = 1;
        }
    }

    private void buildDetail(Models.ShapeVector current, TrendTrace trace) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(trace.position);
        sb.append(" ");
        sb.append(
                trace.previousMode == null || trace.previousMode.length() == 0
                        ? "--"
                        : trace.previousMode);
        sb.append(" -> ");
        sb.append(trace.currentMode);
        sb.append("，转移=");
        sb.append(trace.transitionCode);

        sb.append("，连续=");
        sb.append(trace.sameModeCount);

        sb.append("，确认=");
        sb.append(trace.confirmCount);

        sb.append("，稳定=");
        sb.append(UiFormatter.score(trace.stateStability));

        sb.append("，切换风险=");
        sb.append(UiFormatter.score(trace.modeSwitchRisk));

        sb.append("，破位风险=");
        sb.append(UiFormatter.score(trace.earlyBreakRisk));

        sb.append("，恢复动能=");
        sb.append(UiFormatter.score(trace.recoveryMomentum));

        if (trace.hardBlock == 1) {
            sb.append("，结论=强制HOLD");
        } else if (trace.shouldTighten == 1) {
            sb.append("，结论=收紧门槛");
        } else if (trace.shouldLoosen == 1) {
            sb.append("，结论=形态稳定可略放宽");
        } else {
            sb.append("，结论=正常观察");
        }

        trace.detail = sb.toString();
    }

    private double quality(Models.ShapeVector v) {
        if (v == null) {
            return 0.0d;
        }

        double score = 0.0d;
        score += v.trendLife * 0.24d;
        score += v.recoveryScore * 0.18d;
        score += (100.0d - v.driftRisk) * 0.20d;
        score += (100.0d - v.breakDepth) * 0.16d;
        score += v.championGap * 100.0d * 0.10d;
        score += v.comboGap * 100.0d * 0.08d;
        score += v.top3Mass64 * 100.0d * 0.04d;

        if (AppConfig.MODE_ACCELERATING.equals(v.stateMode)) {
            score += 5.0d;
        } else if (AppConfig.MODE_CONFIRMED.equals(v.stateMode)) {
            score += 3.0d;
        } else if (AppConfig.MODE_RECOVERING.equals(v.stateMode)) {
            score += 1.5d;
        } else if (AppConfig.MODE_WEAKENING.equals(v.stateMode)) {
            score -= 7.0d;
        } else if (AppConfig.MODE_BROKEN.equals(v.stateMode)) {
            score -= 16.0d;
        } else if (AppConfig.MODE_REVERSING.equals(v.stateMode)) {
            score -= 18.0d;
        } else if (AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            score -= 20.0d;
        }

        return ShapeMath.clamp100(score);
    }

    private boolean isGoodMode(String mode) {
        return AppConfig.MODE_CONFIRMED.equals(mode)
                || AppConfig.MODE_ACCELERATING.equals(mode)
                || AppConfig.MODE_RECOVERING.equals(mode);
    }

    private boolean isBadMode(String mode) {
        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || AppConfig.MODE_CHAOS.equals(mode)
                || AppConfig.MODE_WEAKENING.equals(mode);
    }

    private String safeMode(String mode) {
        if (mode == null || mode.length() == 0) {
            return AppConfig.MODE_CHAOS;
        }
        return mode;
    }

    public HashMap<Integer, TrendTrace> toMap(List<TrendTrace> traces) {
        HashMap<Integer, TrendTrace> map = new HashMap<Integer, TrendTrace>();

        if (traces == null) {
            return map;
        }

        for (int i = 0; i < traces.size(); i++) {
            TrendTrace t = traces.get(i);
            if (t != null && AppConfig.isValidPosition(t.position)) {
                map.put(Integer.valueOf(t.position), t);
            }
        }

        return map;
    }

    public static final class TrendTrace {
        public int position;
        public String currentMode;
        public String previousMode;
        public String transitionCode;

        public int sameModeCount;
        public int confirmCount;
        public int breakCount;
        public int recoverCount;
        public int flipCount;

        public double currentQuality;
        public double avgQuality;
        public double trendContinuity;
        public double stateStability;
        public double modeSwitchRisk;
        public double earlyBreakRisk;
        public double recoveryMomentum;

        public int shouldTighten;
        public int shouldLoosen;
        public int hardBlock;

        public String detail;

        public TrendTrace() {
            position = 0;
            currentMode = AppConfig.MODE_CHAOS;
            previousMode = "";
            transitionCode = TRANSITION_UNKNOWN;
            sameModeCount = 0;
            confirmCount = 0;
            breakCount = 0;
            recoverCount = 0;
            flipCount = 0;
            currentQuality = 0.0d;
            avgQuality = 0.0d;
            trendContinuity = 0.0d;
            stateStability = 0.0d;
            modeSwitchRisk = 100.0d;
            earlyBreakRisk = 100.0d;
            recoveryMomentum = 0.0d;
            shouldTighten = 1;
            shouldLoosen = 0;
            hardBlock = 1;
            detail = "";
        }
    }

    /* STARCORE_TIANSHU_B05_TREND_EVIDENCE_ONLY */

    public static final String B05_TREND_VERSION = "STARCORE_TIANSHU_B05_TREND_STATE_V1";

    public B05TrendEvidenceBundle buildB05Evidence(
            V51Models.DrawFrame[] history, V51Models.StateTrajectorySnapshot[] states) {
        long started = System.currentTimeMillis();
        V51Models.DrawFrame[] clean = V51PermutationFeatureEngine.normalizeHistory(history);
        if (clean.length < 12) return B05TrendEvidenceBundle.error("HISTORY_LT_12");
        B05TrendEvidencePacket[] packets = new B05TrendEvidencePacket[AppConfig.POSITION_COUNT];
        double meanSupport = 0.0d;
        double meanCounter = 0.0d;
        double meanRisk = 0.0d;
        for (int p = 0; p < packets.length; p++) {
            double shortSlope = numericSlope(clean, p, 12);
            double mediumSlope = numericSlope(clean, p, 48);
            double longSlope = numericSlope(clean, p, 96);
            double agreement = slopeAgreement(shortSlope, mediumSlope, longSlope);
            double persistence = directionalPersistence(clean, p, 24);
            double reversal = reversalRate(clean, p, 24);
            double stateConfidence = stateConfidence(states, p);
            double changeRisk = stateChangeRisk(states, p);
            double support =
                    V51Math.clamp(
                            0.24d * agreement
                                    + 0.22d * persistence
                                    + 0.18d * stateConfidence
                                    + 0.18d * (1.0d - changeRisk)
                                    + 0.18d
                                            * Math.min(
                                                    1.0d,
                                                    Math.abs(shortSlope) + Math.abs(mediumSlope)),
                            0.0d,
                            1.0d);
            double counter =
                    V51Math.clamp(
                            0.34d * reversal
                                    + 0.28d * changeRisk
                                    + 0.20d * (1.0d - agreement)
                                    + 0.18d * Math.min(1.0d, Math.abs(shortSlope - longSlope)),
                            0.0d,
                            1.0d);
            double risk =
                    V51Math.clamp(
                            0.55d * counter + 0.25d * changeRisk + 0.20d * reversal, 0.0d, 1.0d);
            double confidence =
                    V51Math.clamp(
                            0.45d * Math.min(1.0d, ((double) clean.length) / 96.0d)
                                    + 0.30d * stateConfidence
                                    + 0.25d * agreement,
                            0.0d,
                            1.0d);
            String mode = trendMode(shortSlope, mediumSlope, longSlope, changeRisk);
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_TREND_VERSION
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + clean[clean.length - 1].issue
                                    + "|"
                                    + shortSlope
                                    + "|"
                                    + mediumSlope
                                    + "|"
                                    + longSlope
                                    + "|"
                                    + agreement
                                    + "|"
                                    + persistence
                                    + "|"
                                    + reversal
                                    + "|"
                                    + support
                                    + "|"
                                    + counter
                                    + "|"
                                    + risk
                                    + "|"
                                    + confidence
                                    + "|"
                                    + mode);
            packets[p] =
                    new B05TrendEvidencePacket(
                            p + 1,
                            mode,
                            shortSlope,
                            mediumSlope,
                            longSlope,
                            agreement,
                            persistence,
                            reversal,
                            support,
                            counter,
                            risk,
                            confidence,
                            clean.length,
                            checksum);
            meanSupport += support;
            meanCounter += counter;
            meanRisk += risk;
        }
        meanSupport /= packets.length;
        meanCounter /= packets.length;
        meanRisk /= packets.length;
        String checksum =
                B05TrendEvidenceBundle.hash(
                        clean[clean.length - 1].issue, packets, meanSupport, meanCounter, meanRisk);
        return new B05TrendEvidenceBundle(
                true,
                packets,
                clean.length,
                clean[0].issue,
                clean[clean.length - 1].issue,
                meanSupport,
                meanCounter,
                meanRisk,
                Math.max(0L, System.currentTimeMillis() - started),
                checksum,
                "");
    }

    public static List<B05TrendContractRule> b05TrendContractRules() {
        ArrayList<B05TrendContractRule> rules = new ArrayList<B05TrendContractRule>();
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-001", "TrendStateMachine", "EVIDENCE_ONLY_TREND_001", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-002", "TrendStateMachine", "EVIDENCE_ONLY_TREND_002", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-003", "TrendStateMachine", "EVIDENCE_ONLY_TREND_003", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-004", "TrendStateMachine", "EVIDENCE_ONLY_TREND_004", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-005", "TrendStateMachine", "EVIDENCE_ONLY_TREND_005", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-006", "TrendStateMachine", "EVIDENCE_ONLY_TREND_006", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-007", "TrendStateMachine", "EVIDENCE_ONLY_TREND_007", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-008", "TrendStateMachine", "EVIDENCE_ONLY_TREND_008", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-009", "TrendStateMachine", "EVIDENCE_ONLY_TREND_009", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-010", "TrendStateMachine", "EVIDENCE_ONLY_TREND_010", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-011", "TrendStateMachine", "EVIDENCE_ONLY_TREND_011", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-012", "TrendStateMachine", "EVIDENCE_ONLY_TREND_012", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-013", "TrendStateMachine", "EVIDENCE_ONLY_TREND_013", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-014", "TrendStateMachine", "EVIDENCE_ONLY_TREND_014", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-015", "TrendStateMachine", "EVIDENCE_ONLY_TREND_015", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-016", "TrendStateMachine", "EVIDENCE_ONLY_TREND_016", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-017", "TrendStateMachine", "EVIDENCE_ONLY_TREND_017", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-018", "TrendStateMachine", "EVIDENCE_ONLY_TREND_018", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-019", "TrendStateMachine", "EVIDENCE_ONLY_TREND_019", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-020", "TrendStateMachine", "EVIDENCE_ONLY_TREND_020", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-021", "TrendStateMachine", "EVIDENCE_ONLY_TREND_021", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-022", "TrendStateMachine", "EVIDENCE_ONLY_TREND_022", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-023", "TrendStateMachine", "EVIDENCE_ONLY_TREND_023", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-024", "TrendStateMachine", "EVIDENCE_ONLY_TREND_024", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-025", "TrendStateMachine", "EVIDENCE_ONLY_TREND_025", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-026", "TrendStateMachine", "EVIDENCE_ONLY_TREND_026", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-027", "TrendStateMachine", "EVIDENCE_ONLY_TREND_027", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-028", "TrendStateMachine", "EVIDENCE_ONLY_TREND_028", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-029", "TrendStateMachine", "EVIDENCE_ONLY_TREND_029", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-030", "TrendStateMachine", "EVIDENCE_ONLY_TREND_030", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-031", "TrendStateMachine", "EVIDENCE_ONLY_TREND_031", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-032", "TrendStateMachine", "EVIDENCE_ONLY_TREND_032", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-033", "TrendStateMachine", "EVIDENCE_ONLY_TREND_033", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-034", "TrendStateMachine", "EVIDENCE_ONLY_TREND_034", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-035", "TrendStateMachine", "EVIDENCE_ONLY_TREND_035", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-036", "TrendStateMachine", "EVIDENCE_ONLY_TREND_036", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-037", "TrendStateMachine", "EVIDENCE_ONLY_TREND_037", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-038", "TrendStateMachine", "EVIDENCE_ONLY_TREND_038", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-039", "TrendStateMachine", "EVIDENCE_ONLY_TREND_039", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-040", "TrendStateMachine", "EVIDENCE_ONLY_TREND_040", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-041", "TrendStateMachine", "EVIDENCE_ONLY_TREND_041", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-042", "TrendStateMachine", "EVIDENCE_ONLY_TREND_042", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-043", "TrendStateMachine", "EVIDENCE_ONLY_TREND_043", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-044", "TrendStateMachine", "EVIDENCE_ONLY_TREND_044", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-045", "TrendStateMachine", "EVIDENCE_ONLY_TREND_045", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-046", "TrendStateMachine", "EVIDENCE_ONLY_TREND_046", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-047", "TrendStateMachine", "EVIDENCE_ONLY_TREND_047", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-048", "TrendStateMachine", "EVIDENCE_ONLY_TREND_048", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-049", "TrendStateMachine", "EVIDENCE_ONLY_TREND_049", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-050", "TrendStateMachine", "EVIDENCE_ONLY_TREND_050", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-051", "TrendStateMachine", "EVIDENCE_ONLY_TREND_051", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-052", "TrendStateMachine", "EVIDENCE_ONLY_TREND_052", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-053", "TrendStateMachine", "EVIDENCE_ONLY_TREND_053", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-054", "TrendStateMachine", "EVIDENCE_ONLY_TREND_054", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-055", "TrendStateMachine", "EVIDENCE_ONLY_TREND_055", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-056", "TrendStateMachine", "EVIDENCE_ONLY_TREND_056", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-057", "TrendStateMachine", "EVIDENCE_ONLY_TREND_057", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-058", "TrendStateMachine", "EVIDENCE_ONLY_TREND_058", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-059", "TrendStateMachine", "EVIDENCE_ONLY_TREND_059", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-060", "TrendStateMachine", "EVIDENCE_ONLY_TREND_060", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-061", "TrendStateMachine", "EVIDENCE_ONLY_TREND_061", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-062", "TrendStateMachine", "EVIDENCE_ONLY_TREND_062", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-063", "TrendStateMachine", "EVIDENCE_ONLY_TREND_063", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-064", "TrendStateMachine", "EVIDENCE_ONLY_TREND_064", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-065", "TrendStateMachine", "EVIDENCE_ONLY_TREND_065", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-066", "TrendStateMachine", "EVIDENCE_ONLY_TREND_066", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-067", "TrendStateMachine", "EVIDENCE_ONLY_TREND_067", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-068", "TrendStateMachine", "EVIDENCE_ONLY_TREND_068", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-069", "TrendStateMachine", "EVIDENCE_ONLY_TREND_069", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-070", "TrendStateMachine", "EVIDENCE_ONLY_TREND_070", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-071", "TrendStateMachine", "EVIDENCE_ONLY_TREND_071", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-072", "TrendStateMachine", "EVIDENCE_ONLY_TREND_072", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-073", "TrendStateMachine", "EVIDENCE_ONLY_TREND_073", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-074", "TrendStateMachine", "EVIDENCE_ONLY_TREND_074", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-075", "TrendStateMachine", "EVIDENCE_ONLY_TREND_075", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-076", "TrendStateMachine", "EVIDENCE_ONLY_TREND_076", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-077", "TrendStateMachine", "EVIDENCE_ONLY_TREND_077", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-078", "TrendStateMachine", "EVIDENCE_ONLY_TREND_078", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-079", "TrendStateMachine", "EVIDENCE_ONLY_TREND_079", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-080", "TrendStateMachine", "EVIDENCE_ONLY_TREND_080", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-081", "TrendStateMachine", "EVIDENCE_ONLY_TREND_081", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-082", "TrendStateMachine", "EVIDENCE_ONLY_TREND_082", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-083", "TrendStateMachine", "EVIDENCE_ONLY_TREND_083", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-084", "TrendStateMachine", "EVIDENCE_ONLY_TREND_084", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-085", "TrendStateMachine", "EVIDENCE_ONLY_TREND_085", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-086", "TrendStateMachine", "EVIDENCE_ONLY_TREND_086", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-087", "TrendStateMachine", "EVIDENCE_ONLY_TREND_087", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-088", "TrendStateMachine", "EVIDENCE_ONLY_TREND_088", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-089", "TrendStateMachine", "EVIDENCE_ONLY_TREND_089", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-090", "TrendStateMachine", "EVIDENCE_ONLY_TREND_090", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-091", "TrendStateMachine", "EVIDENCE_ONLY_TREND_091", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-092", "TrendStateMachine", "EVIDENCE_ONLY_TREND_092", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-093", "TrendStateMachine", "EVIDENCE_ONLY_TREND_093", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-094", "TrendStateMachine", "EVIDENCE_ONLY_TREND_094", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-095", "TrendStateMachine", "EVIDENCE_ONLY_TREND_095", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-096", "TrendStateMachine", "EVIDENCE_ONLY_TREND_096", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-097", "TrendStateMachine", "EVIDENCE_ONLY_TREND_097", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-098", "TrendStateMachine", "EVIDENCE_ONLY_TREND_098", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-099", "TrendStateMachine", "EVIDENCE_ONLY_TREND_099", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-100", "TrendStateMachine", "EVIDENCE_ONLY_TREND_100", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-101", "TrendStateMachine", "EVIDENCE_ONLY_TREND_101", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-102", "TrendStateMachine", "EVIDENCE_ONLY_TREND_102", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-103", "TrendStateMachine", "EVIDENCE_ONLY_TREND_103", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-104", "TrendStateMachine", "EVIDENCE_ONLY_TREND_104", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-105", "TrendStateMachine", "EVIDENCE_ONLY_TREND_105", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-106", "TrendStateMachine", "EVIDENCE_ONLY_TREND_106", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-107", "TrendStateMachine", "EVIDENCE_ONLY_TREND_107", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-108", "TrendStateMachine", "EVIDENCE_ONLY_TREND_108", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-109", "TrendStateMachine", "EVIDENCE_ONLY_TREND_109", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-110", "TrendStateMachine", "EVIDENCE_ONLY_TREND_110", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-111", "TrendStateMachine", "EVIDENCE_ONLY_TREND_111", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-112", "TrendStateMachine", "EVIDENCE_ONLY_TREND_112", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-113", "TrendStateMachine", "EVIDENCE_ONLY_TREND_113", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-114", "TrendStateMachine", "EVIDENCE_ONLY_TREND_114", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-115", "TrendStateMachine", "EVIDENCE_ONLY_TREND_115", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-116", "TrendStateMachine", "EVIDENCE_ONLY_TREND_116", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-117", "TrendStateMachine", "EVIDENCE_ONLY_TREND_117", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-118", "TrendStateMachine", "EVIDENCE_ONLY_TREND_118", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-119", "TrendStateMachine", "EVIDENCE_ONLY_TREND_119", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-120", "TrendStateMachine", "EVIDENCE_ONLY_TREND_120", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-121", "TrendStateMachine", "EVIDENCE_ONLY_TREND_121", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-122", "TrendStateMachine", "EVIDENCE_ONLY_TREND_122", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-123", "TrendStateMachine", "EVIDENCE_ONLY_TREND_123", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-124", "TrendStateMachine", "EVIDENCE_ONLY_TREND_124", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-125", "TrendStateMachine", "EVIDENCE_ONLY_TREND_125", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-126", "TrendStateMachine", "EVIDENCE_ONLY_TREND_126", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-127", "TrendStateMachine", "EVIDENCE_ONLY_TREND_127", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-128", "TrendStateMachine", "EVIDENCE_ONLY_TREND_128", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-129", "TrendStateMachine", "EVIDENCE_ONLY_TREND_129", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-130", "TrendStateMachine", "EVIDENCE_ONLY_TREND_130", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-131", "TrendStateMachine", "EVIDENCE_ONLY_TREND_131", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-132", "TrendStateMachine", "EVIDENCE_ONLY_TREND_132", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-133", "TrendStateMachine", "EVIDENCE_ONLY_TREND_133", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-134", "TrendStateMachine", "EVIDENCE_ONLY_TREND_134", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-135", "TrendStateMachine", "EVIDENCE_ONLY_TREND_135", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-136", "TrendStateMachine", "EVIDENCE_ONLY_TREND_136", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-137", "TrendStateMachine", "EVIDENCE_ONLY_TREND_137", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-138", "TrendStateMachine", "EVIDENCE_ONLY_TREND_138", true, 6));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-139", "TrendStateMachine", "EVIDENCE_ONLY_TREND_139", true, 7));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-140", "TrendStateMachine", "EVIDENCE_ONLY_TREND_140", true, 1));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-141", "TrendStateMachine", "EVIDENCE_ONLY_TREND_141", true, 2));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-142", "TrendStateMachine", "EVIDENCE_ONLY_TREND_142", true, 3));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-143", "TrendStateMachine", "EVIDENCE_ONLY_TREND_143", true, 4));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-144", "TrendStateMachine", "EVIDENCE_ONLY_TREND_144", true, 5));
        rules.add(
                new B05TrendContractRule(
                        "B05-TREND-145", "TrendStateMachine", "EVIDENCE_ONLY_TREND_145", true, 6));
        return rules;
    }

    private static double numericSlope(
            V51Models.DrawFrame[] history, int positionIndex, int window) {
        if (history == null || history.length < 2) return 0.0d;
        int start = Math.max(0, history.length - Math.max(2, window));
        int n = history.length - start;
        double meanX = (n - 1) / 2.0d;
        double meanY = 0.0d;
        for (int i = start; i < history.length; i++) meanY += history[i].permutation[positionIndex];
        meanY /= n;
        double numerator = 0.0d;
        double denominator = 0.0d;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            double dy = history[start + i].permutation[positionIndex] - meanY;
            numerator += dx * dy;
            denominator += dx * dx;
        }
        if (!(denominator > 0.0d)) return 0.0d;
        return V51Math.clamp((numerator / denominator) / 9.0d, -1.0d, 1.0d);
    }

    private static double slopeAgreement(double a, double b, double c) {
        int positive = 0;
        int negative = 0;
        double[] value = new double[] {a, b, c};
        for (int i = 0; i < value.length; i++) {
            if (value[i] > 0.01d) positive++;
            else if (value[i] < -0.01d) negative++;
        }
        return ((double) Math.max(positive, negative)) / 3.0d;
    }

    private static double directionalPersistence(
            V51Models.DrawFrame[] history, int positionIndex, int window) {
        if (history == null || history.length < 3) return 0.0d;
        int start = Math.max(1, history.length - Math.max(3, window));
        int same = 0;
        int compared = 0;
        int previousDirection = 0;
        for (int i = start; i < history.length; i++) {
            int delta =
                    history[i].permutation[positionIndex]
                            - history[i - 1].permutation[positionIndex];
            int direction = delta > 0 ? 1 : (delta < 0 ? -1 : 0);
            if (previousDirection != 0 && direction != 0) {
                if (direction == previousDirection) same++;
                compared++;
            }
            if (direction != 0) previousDirection = direction;
        }
        return ((double) same) / Math.max(1, compared);
    }

    private static double reversalRate(
            V51Models.DrawFrame[] history, int positionIndex, int window) {
        if (history == null || history.length < 3) return 0.0d;
        int start = Math.max(1, history.length - Math.max(3, window));
        int reverse = 0;
        int compared = 0;
        int previousDirection = 0;
        for (int i = start; i < history.length; i++) {
            int delta =
                    history[i].permutation[positionIndex]
                            - history[i - 1].permutation[positionIndex];
            int direction = delta > 0 ? 1 : (delta < 0 ? -1 : 0);
            if (previousDirection != 0 && direction != 0) {
                if (direction != previousDirection) reverse++;
                compared++;
            }
            if (direction != 0) previousDirection = direction;
        }
        return ((double) reverse) / Math.max(1, compared);
    }

    private static double stateConfidence(V51Models.StateTrajectorySnapshot[] states, int index) {
        return states == null || index < 0 || index >= states.length || states[index] == null
                ? 0.0d
                : states[index].confidence;
    }

    private static double stateChangeRisk(V51Models.StateTrajectorySnapshot[] states, int index) {
        return states == null || index < 0 || index >= states.length || states[index] == null
                ? 1.0d
                : states[index].changePointRisk;
    }

    private static String trendMode(
            double shortSlope, double mediumSlope, double longSlope, double changeRisk) {
        if (changeRisk >= 0.78d) return "BREAK";
        double mean = (shortSlope + mediumSlope + longSlope) / 3.0d;
        if (Math.abs(mean) < 0.015d) return "FLAT";
        if (mean > 0.0d) return shortSlope >= longSlope ? "UP_ACCELERATING" : "UP_DECELERATING";
        return shortSlope <= longSlope ? "DOWN_ACCELERATING" : "DOWN_DECELERATING";
    }

    public static final class B05TrendEvidencePacket {
        public final int position;
        public final String mode;
        public final double shortSlope;
        public final double mediumSlope;
        public final double longSlope;
        public final double agreement;
        public final double persistence;
        public final double reversal;
        public final double support;
        public final double counterEvidence;
        public final double risk;
        public final double confidence;
        public final int sample;
        public final String checksum;

        B05TrendEvidencePacket(
                int position,
                String mode,
                double shortSlope,
                double mediumSlope,
                double longSlope,
                double agreement,
                double persistence,
                double reversal,
                double support,
                double counterEvidence,
                double risk,
                double confidence,
                int sample,
                String checksum) {
            this.position = position;
            this.mode = mode == null ? "" : mode;
            this.shortSlope = V51Math.finite(shortSlope);
            this.mediumSlope = V51Math.finite(mediumSlope);
            this.longSlope = V51Math.finite(longSlope);
            this.agreement = V51Math.clamp(agreement, 0.0d, 1.0d);
            this.persistence = V51Math.clamp(persistence, 0.0d, 1.0d);
            this.reversal = V51Math.clamp(reversal, 0.0d, 1.0d);
            this.support = V51Math.clamp(support, 0.0d, 1.0d);
            this.counterEvidence = V51Math.clamp(counterEvidence, 0.0d, 1.0d);
            this.risk = V51Math.clamp(risk, 0.0d, 1.0d);
            this.confidence = V51Math.clamp(confidence, 0.0d, 1.0d);
            this.sample = Math.max(0, sample);
            this.checksum = checksum == null ? "" : checksum;
        }

        public String canonical() {
            return position
                    + "|"
                    + mode
                    + "|"
                    + shortSlope
                    + "|"
                    + mediumSlope
                    + "|"
                    + longSlope
                    + "|"
                    + agreement
                    + "|"
                    + persistence
                    + "|"
                    + reversal
                    + "|"
                    + support
                    + "|"
                    + counterEvidence
                    + "|"
                    + risk
                    + "|"
                    + confidence
                    + "|"
                    + sample
                    + "|"
                    + checksum;
        }
    }

    public static final class B05TrendEvidenceBundle {
        public final boolean valid;
        public final B05TrendEvidencePacket[] packets;
        public final int sample;
        public final String firstIssue;
        public final String lastIssue;
        public final double meanSupport;
        public final double meanCounterEvidence;
        public final double meanRisk;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        B05TrendEvidenceBundle(
                boolean valid,
                B05TrendEvidencePacket[] packets,
                int sample,
                String firstIssue,
                String lastIssue,
                double meanSupport,
                double meanCounterEvidence,
                double meanRisk,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.packets = packets == null ? new B05TrendEvidencePacket[0] : packets;
            this.sample = Math.max(0, sample);
            this.firstIssue = firstIssue == null ? "" : firstIssue;
            this.lastIssue = lastIssue == null ? "" : lastIssue;
            this.meanSupport = V51Math.clamp(meanSupport, 0.0d, 1.0d);
            this.meanCounterEvidence = V51Math.clamp(meanCounterEvidence, 0.0d, 1.0d);
            this.meanRisk = V51Math.clamp(meanRisk, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = checksum == null ? "" : checksum;
            this.detail = detail == null ? "" : detail;
        }

        static B05TrendEvidenceBundle error(String detail) {
            return new B05TrendEvidenceBundle(
                    false,
                    new B05TrendEvidencePacket[0],
                    0,
                    "",
                    "",
                    0.0d,
                    0.0d,
                    1.0d,
                    0L,
                    V51ChecksumEngine.sha256(B05_TREND_VERSION + "|ERROR|" + detail),
                    detail);
        }

        static String hash(
                String lastIssue,
                B05TrendEvidencePacket[] packets,
                double meanSupport,
                double meanCounter,
                double meanRisk) {
            StringBuilder out = new StringBuilder();
            if (packets != null) {
                for (int i = 0; i < packets.length; i++)
                    out.append(packets[i].canonical()).append(';');
            }
            return V51ChecksumEngine.sha256(
                    B05_TREND_VERSION
                            + "|"
                            + lastIssue
                            + "|"
                            + out.toString()
                            + "|"
                            + meanSupport
                            + "|"
                            + meanCounter
                            + "|"
                            + meanRisk);
        }
    }

    public static final class B05TrendContractRule {
        public final String id;
        public final String owner;
        public final String invariant;
        public final boolean mandatory;
        public final int severity;

        B05TrendContractRule(
                String id, String owner, String invariant, boolean mandatory, int severity) {
            this.id = id == null ? "" : id;
            this.owner = owner == null ? "" : owner;
            this.invariant = invariant == null ? "" : invariant;
            this.mandatory = mandatory;
            this.severity = Math.max(1, severity);
        }

        public String canonical() {
            return id + "|" + owner + "|" + invariant + "|" + (mandatory ? 1 : 0) + "|" + severity;
        }
    }
}
