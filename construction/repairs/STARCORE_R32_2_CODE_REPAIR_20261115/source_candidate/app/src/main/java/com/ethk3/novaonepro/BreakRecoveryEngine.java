package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

public final class BreakRecoveryEngine {

    public static final String BREAK_NONE = "NONE";
    public static final String BREAK_LIGHT = "LIGHT";
    public static final String BREAK_MID = "MID";
    public static final String BREAK_HEAVY = "HEAVY";
    public static final String BREAK_REVERSE = "REVERSE";
    public static final String BREAK_CHAOS = "CHAOS";

    public static final String RECOVERY_NONE = "NONE";
    public static final String RECOVERY_WEAK = "WEAK";
    public static final String RECOVERY_NORMAL = "NORMAL";
    public static final String RECOVERY_STRONG = "STRONG";
    public static final String RECOVERY_FALSE = "FALSE";

    public BreakRecoveryEngine() {}

    public RecoveryResult analyze(Models.ShapeVector vector, TrendStateMachine.TrendTrace trace) {
        RecoveryResult r = new RecoveryResult();

        if (vector == null) {
            r.position = 0;
            r.breakType = BREAK_CHAOS;
            r.recoveryType = RECOVERY_NONE;
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_DATA_ERROR;
            r.detail = "形态为空，断点恢复引擎强制HOLD";
            return r;
        }

        r.position = vector.position;
        r.stateMode = vector.stateMode == null ? AppConfig.MODE_CHAOS : vector.stateMode;
        r.transitionCode =
                trace == null ? TrendStateMachine.TRANSITION_UNKNOWN : trace.transitionCode;

        r.breakDepth = ShapeMath.clamp100(vector.breakDepth);
        r.driftRisk = ShapeMath.clamp100(vector.driftRisk);
        r.recoveryScore = ShapeMath.clamp100(vector.recoveryScore);
        r.hotReverseRisk = ShapeMath.clamp100(vector.hotReverseRisk);
        r.falseStrongRisk = ShapeMath.clamp100(vector.falseStrongRisk);

        if (trace != null) {
            r.modeSwitchRisk = ShapeMath.clamp100(trace.modeSwitchRisk);
            r.earlyBreakRisk = ShapeMath.clamp100(trace.earlyBreakRisk);
            r.recoveryMomentum = ShapeMath.clamp100(trace.recoveryMomentum);
            r.trendContinuity = ShapeMath.clamp100(trace.trendContinuity);
            r.confirmCount = trace.confirmCount;
            r.sameModeCount = trace.sameModeCount;
        } else {
            r.modeSwitchRisk = 50.0d;
            r.earlyBreakRisk = vector.breakDepth;
            r.recoveryMomentum = vector.recoveryScore;
            r.trendContinuity = 40.0d;
            r.confirmCount = 0;
            r.sameModeCount = 0;
        }

        r.breakType = decideBreakType(vector, trace);
        r.recoveryType = decideRecoveryType(vector, trace, r.breakType);
        r.breakRisk = calcBreakRisk(vector, trace, r.breakType);
        r.recoveryPower = calcRecoveryPower(vector, trace, r.recoveryType);
        r.falseRecoveryRisk = calcFalseRecoveryRisk(vector, trace, r);

        r.recoveryP = calcRecoveryProbability(r);
        r.gateAdjust = calcGateAdjustment(r);
        r.probabilityAdjust = calcProbabilityAdjustment(r);

        applyFinalProtection(r, vector, trace);

        r.breakKey = buildBreakKey(vector, trace, r);
        r.detail = buildDetail(r);

        return r;
    }

    public ArrayList<RecoveryResult> analyzeAll(
            List<Models.ShapeVector> vectors, List<TrendStateMachine.TrendTrace> traces) {
        ArrayList<RecoveryResult> out = new ArrayList<RecoveryResult>();

        if (vectors == null || vectors.size() == 0) {
            return out;
        }

        TrendStateMachine tsm = new TrendStateMachine();

        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);
            if (v == null) {
                continue;
            }

            TrendStateMachine.TrendTrace trace = tsm.findTraceForPosition(traces, v.position);
            out.add(analyze(v, trace));
        }

        return out;
    }

    public RecoveryResult findForPosition(List<RecoveryResult> results, int position) {
        if (results == null || !AppConfig.isValidPosition(position)) {
            return null;
        }

        for (int i = 0; i < results.size(); i++) {
            RecoveryResult r = results.get(i);
            if (r != null && r.position == position) {
                return r;
            }
        }

        return null;
    }

    public boolean isHardBlock(RecoveryResult r) {
        if (r == null) {
            return true;
        }

        if (r.hardBlock == 1) {
            return true;
        }

        if (BREAK_HEAVY.equals(r.breakType)
                || BREAK_REVERSE.equals(r.breakType)
                || BREAK_CHAOS.equals(r.breakType)) {
            return true;
        }

        if (RECOVERY_FALSE.equals(r.recoveryType)) {
            return true;
        }

        if (r.breakRisk >= 62.0d) {
            return true;
        }

        if (r.falseRecoveryRisk >= 60.0d) {
            return true;
        }

        return false;
    }

    public boolean isSafeRecovery(RecoveryResult r) {
        if (r == null) {
            return false;
        }

        if (isHardBlock(r)) {
            return false;
        }

        if (RECOVERY_STRONG.equals(r.recoveryType)) {
            return r.recoveryPower >= 72.0d && r.breakRisk <= 42.0d && r.falseRecoveryRisk <= 42.0d;
        }

        if (RECOVERY_NORMAL.equals(r.recoveryType)) {
            return r.recoveryPower >= 66.0d
                    && r.breakRisk <= 36.0d
                    && r.falseRecoveryRisk <= 38.0d
                    && r.trendContinuity >= 62.0d;
        }

        if (BREAK_NONE.equals(r.breakType) || BREAK_LIGHT.equals(r.breakType)) {
            return r.breakRisk <= 34.0d && r.falseRecoveryRisk <= 40.0d;
        }

        return false;
    }

    public void enrichCandidate(Models.ComboCandidate candidate, RecoveryResult r) {
        if (candidate == null || r == null) {
            return;
        }

        candidate.recoveryP = r.recoveryP;

        if (isHardBlock(r)) {
            candidate.rawP -= 0.030d;
            candidate.finalP -= 0.030d;
            candidate.blockCode =
                    r.blockCode == null || r.blockCode.length() == 0
                            ? AppConfig.BLOCK_BREAK_HIGH
                            : r.blockCode;
            candidate.reasonSummary =
                    appendReason(candidate.reasonSummary, "断点恢复保护：" + r.recoveryType);
        } else {
            candidate.rawP += r.probabilityAdjust;
            candidate.finalP += r.probabilityAdjust;
            candidate.reasonSummary =
                    appendReason(candidate.reasonSummary, "恢复证据：" + r.recoveryType);
        }

        if (candidate.rawP < 0.0d) {
            candidate.rawP = 0.0d;
        }
        if (candidate.finalP < 0.0d) {
            candidate.finalP = 0.0d;
        }

        candidate.rawP = AppConfig.clampProbability(candidate.rawP);
        candidate.finalP = AppConfig.clampProbability(candidate.finalP);
    }

    public double gateAdjustment(RecoveryResult r) {
        if (r == null) {
            return 0.030d;
        }
        return r.gateAdjust;
    }

    public double probabilityAdjustment(RecoveryResult r) {
        if (r == null) {
            return -0.015d;
        }
        return r.probabilityAdjust;
    }

    public void learnAfterSettle(
            DbStore store, Models.ComboCandidate candidate, RecoveryResult r, int actualNumber) {
        if (store == null || candidate == null || r == null) {
            return;
        }

        if (!candidate.isValidK3()) {
            return;
        }

        boolean hit = Models.maskContains(candidate.k3Mask, actualNumber);
        boolean fail = !hit;
        boolean reverse =
                BREAK_REVERSE.equals(r.breakType)
                        || RECOVERY_FALSE.equals(r.recoveryType)
                        || r.hotReverseRisk >= AppConfig.HOT_REVERSE_RISK_MAX + 15.0d;

        int recoverStep = 0;
        if (hit && RECOVERY_STRONG.equals(r.recoveryType)) {
            recoverStep = 1;
        } else if (hit && RECOVERY_NORMAL.equals(r.recoveryType)) {
            recoverStep = 2;
        } else if (hit && RECOVERY_WEAK.equals(r.recoveryType)) {
            recoverStep = 3;
        }

        store.updateBreakMemory(
                r.breakKey,
                candidate.position,
                candidate.stateMode,
                r.breakType,
                ShapeMath.bucketScore(r.breakDepth, 5),
                ShapeMath.bucketScore(candidate.trendLife, 5),
                ShapeMath.bucketScore(r.driftRisk, 5),
                ShapeMath.bucketProbability(candidate.modelP, 2),
                recoverStep,
                fail,
                reverse);
    }

    public String explain(RecoveryResult r) {
        if (r == null) {
            return "断点恢复为空";
        }
        return r.detail == null ? "" : r.detail;
    }

    private String decideBreakType(Models.ShapeVector v, TrendStateMachine.TrendTrace trace) {
        if (v == null) {
            return BREAK_CHAOS;
        }

        String mode = v.stateMode == null ? AppConfig.MODE_CHAOS : v.stateMode;
        String transition =
                trace == null ? TrendStateMachine.TRANSITION_UNKNOWN : trace.transitionCode;

        if (AppConfig.MODE_CHAOS.equals(mode)
                || TrendStateMachine.TRANSITION_CHAOS.equals(transition)
                || v.driftRisk >= 65.0d) {
            return BREAK_CHAOS;
        }

        if (AppConfig.MODE_REVERSING.equals(mode)
                || TrendStateMachine.TRANSITION_REVERSE.equals(transition)
                || v.hotReverseRisk >= 62.0d) {
            return BREAK_REVERSE;
        }

        if (AppConfig.MODE_BROKEN.equals(mode)
                || TrendStateMachine.TRANSITION_BREAK.equals(transition)
                || v.breakDepth >= 58.0d) {
            return BREAK_HEAVY;
        }

        if (v.breakDepth >= 42.0d
                || v.driftRisk >= 45.0d
                || (trace != null && trace.earlyBreakRisk >= 48.0d)) {
            return BREAK_MID;
        }

        if (v.breakDepth >= 22.0d
                || AppConfig.MODE_PULLBACK.equals(mode)
                || AppConfig.MODE_WEAKENING.equals(mode)) {
            return BREAK_LIGHT;
        }

        return BREAK_NONE;
    }

    private String decideRecoveryType(
            Models.ShapeVector v, TrendStateMachine.TrendTrace trace, String breakType) {
        if (v == null) {
            return RECOVERY_NONE;
        }

        if (BREAK_CHAOS.equals(breakType)
                || BREAK_REVERSE.equals(breakType)
                || BREAK_HEAVY.equals(breakType)) {
            if (v.recoveryScore >= 78.0d && v.driftRisk <= 28.0d && v.breakDepth <= 35.0d) {
                return RECOVERY_WEAK;
            }
            return RECOVERY_NONE;
        }

        double momentum = trace == null ? v.recoveryScore : trace.recoveryMomentum;
        double continuity = trace == null ? 40.0d : trace.trendContinuity;
        double switchRisk = trace == null ? 50.0d : trace.modeSwitchRisk;

        if (v.recoveryScore >= 75.0d
                && momentum >= 74.0d
                && v.driftRisk <= 30.0d
                && v.breakDepth <= 32.0d
                && switchRisk <= 35.0d
                && continuity >= 62.0d) {
            return RECOVERY_STRONG;
        }

        if (v.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN
                && momentum >= 66.0d
                && v.driftRisk <= 38.0d
                && v.breakDepth <= AppConfig.BREAK_DEPTH_MAX
                && switchRisk <= 45.0d) {
            return RECOVERY_NORMAL;
        }

        if (v.recoveryScore >= 60.0d && momentum >= 58.0d && v.driftRisk <= 45.0d) {
            return RECOVERY_WEAK;
        }

        if (v.recoveryScore >= 68.0d
                && (v.hotReverseRisk >= 55.0d
                        || v.falseStrongRisk >= 55.0d
                        || switchRisk >= 55.0d)) {
            return RECOVERY_FALSE;
        }

        return RECOVERY_NONE;
    }

    private double calcBreakRisk(
            Models.ShapeVector v, TrendStateMachine.TrendTrace trace, String breakType) {
        if (v == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += v.breakDepth * 0.34d;
        risk += v.driftRisk * 0.24d;
        risk += v.hotReverseRisk * 0.14d;
        risk += v.falseStrongRisk * 0.12d;
        risk += ShapeMath.negativePartAbs(v.slopeShort) * 420.0d;
        risk += ShapeMath.negativePartAbs(v.slopeMid) * 220.0d;

        if (trace != null) {
            risk += trace.modeSwitchRisk * 0.10d;
            risk += trace.earlyBreakRisk * 0.12d;
        }

        if (BREAK_HEAVY.equals(breakType)) {
            risk += 14.0d;
        } else if (BREAK_REVERSE.equals(breakType)) {
            risk += 18.0d;
        } else if (BREAK_CHAOS.equals(breakType)) {
            risk += 22.0d;
        } else if (BREAK_MID.equals(breakType)) {
            risk += 6.0d;
        } else if (BREAK_LIGHT.equals(breakType)) {
            risk += 2.0d;
        }

        return ShapeMath.clamp100(risk);
    }

    private double calcRecoveryPower(
            Models.ShapeVector v, TrendStateMachine.TrendTrace trace, String recoveryType) {
        if (v == null) {
            return 0.0d;
        }

        double power = 0.0d;

        power += v.recoveryScore * 0.34d;
        power += v.trendLife * 0.18d;
        power += (100.0d - v.driftRisk) * 0.16d;
        power += (100.0d - v.breakDepth) * 0.12d;
        power += ShapeMath.positivePart(v.slopeShort) * 420.0d;
        power += ShapeMath.positivePart(v.slopeMid) * 160.0d;
        power += v.championGap * 100.0d * 0.10d;
        power += v.comboGap * 100.0d * 0.10d;

        if (trace != null) {
            power += trace.trendContinuity * 0.08d;
            power += trace.recoveryMomentum * 0.12d;
            power -= trace.modeSwitchRisk * 0.08d;
        }

        if (RECOVERY_STRONG.equals(recoveryType)) {
            power += 8.0d;
        } else if (RECOVERY_NORMAL.equals(recoveryType)) {
            power += 4.0d;
        } else if (RECOVERY_WEAK.equals(recoveryType)) {
            power -= 4.0d;
        } else if (RECOVERY_FALSE.equals(recoveryType)) {
            power -= 18.0d;
        } else {
            power -= 8.0d;
        }

        return ShapeMath.clamp100(power);
    }

    private double calcFalseRecoveryRisk(
            Models.ShapeVector v, TrendStateMachine.TrendTrace trace, RecoveryResult r) {
        if (v == null) {
            return 100.0d;
        }

        double risk = 0.0d;

        risk += v.falseStrongRisk * 0.28d;
        risk += v.hotReverseRisk * 0.25d;
        risk += v.driftRisk * 0.18d;

        if (v.recoveryScore >= 68.0d && v.breakDepth >= 42.0d) {
            risk += 12.0d;
        }

        if (v.slopeShort > 0.0d && v.slopeMid < -0.010d) {
            risk += 10.0d;
        }

        if (v.top3Mass16 > v.top3Mass240 + 0.070d) {
            risk += 12.0d;
        }

        if (trace != null) {
            risk += trace.modeSwitchRisk * 0.14d;
            if (trace.sameModeCount <= 1 && trace.confirmCount <= 1) {
                risk += 10.0d;
            }
        }

        if (RECOVERY_FALSE.equals(r.recoveryType)) {
            risk += 16.0d;
        }

        return ShapeMath.clamp100(risk);
    }

    private double calcRecoveryProbability(RecoveryResult r) {
        if (r == null) {
            return 0.0d;
        }

        double p = 0.30d;

        p += (r.recoveryPower - 60.0d) / 100.0d * 0.20d;
        p -= (r.breakRisk - 35.0d) / 100.0d * 0.16d;
        p -= (r.falseRecoveryRisk - 35.0d) / 100.0d * 0.14d;

        if (RECOVERY_STRONG.equals(r.recoveryType)) {
            p += 0.030d;
        } else if (RECOVERY_NORMAL.equals(r.recoveryType)) {
            p += 0.014d;
        } else if (RECOVERY_WEAK.equals(r.recoveryType)) {
            p -= 0.006d;
        } else if (RECOVERY_FALSE.equals(r.recoveryType)) {
            p -= 0.045d;
        } else {
            p -= 0.020d;
        }

        if (BREAK_NONE.equals(r.breakType)) {
            p += 0.010d;
        } else if (BREAK_LIGHT.equals(r.breakType)) {
            p += 0.002d;
        } else if (BREAK_MID.equals(r.breakType)) {
            p -= 0.012d;
        } else if (BREAK_HEAVY.equals(r.breakType)) {
            p -= 0.040d;
        } else if (BREAK_REVERSE.equals(r.breakType) || BREAK_CHAOS.equals(r.breakType)) {
            p -= 0.060d;
        }

        return AppConfig.clampProbability(p);
    }

    private double calcGateAdjustment(RecoveryResult r) {
        if (r == null) {
            return 0.035d;
        }

        double adjust = 0.0d;

        if (BREAK_NONE.equals(r.breakType)) {
            adjust -= 0.002d;
        } else if (BREAK_LIGHT.equals(r.breakType)) {
            adjust += 0.004d;
        } else if (BREAK_MID.equals(r.breakType)) {
            adjust += 0.016d;
        } else if (BREAK_HEAVY.equals(r.breakType)) {
            adjust += 0.038d;
        } else if (BREAK_REVERSE.equals(r.breakType) || BREAK_CHAOS.equals(r.breakType)) {
            adjust += 0.050d;
        }

        if (RECOVERY_STRONG.equals(r.recoveryType) && r.falseRecoveryRisk <= 38.0d) {
            adjust -= 0.008d;
        } else if (RECOVERY_NORMAL.equals(r.recoveryType) && r.falseRecoveryRisk <= 40.0d) {
            adjust -= 0.003d;
        } else if (RECOVERY_FALSE.equals(r.recoveryType)) {
            adjust += 0.026d;
        } else if (RECOVERY_NONE.equals(r.recoveryType)) {
            adjust += 0.012d;
        }

        if (r.modeSwitchRisk >= 50.0d) {
            adjust += 0.012d;
        }

        if (r.falseRecoveryRisk >= 55.0d) {
            adjust += 0.016d;
        }

        if (adjust < -0.012d) {
            adjust = -0.012d;
        }

        if (adjust > 0.065d) {
            adjust = 0.065d;
        }

        return adjust;
    }

    private double calcProbabilityAdjustment(RecoveryResult r) {
        if (r == null) {
            return -0.015d;
        }

        double adjust = 0.0d;

        if (RECOVERY_STRONG.equals(r.recoveryType)) {
            adjust += 0.012d;
        } else if (RECOVERY_NORMAL.equals(r.recoveryType)) {
            adjust += 0.006d;
        } else if (RECOVERY_WEAK.equals(r.recoveryType)) {
            adjust -= 0.004d;
        } else if (RECOVERY_FALSE.equals(r.recoveryType)) {
            adjust -= 0.026d;
        } else {
            adjust -= 0.010d;
        }

        if (BREAK_MID.equals(r.breakType)) {
            adjust -= 0.008d;
        } else if (BREAK_HEAVY.equals(r.breakType)) {
            adjust -= 0.022d;
        } else if (BREAK_REVERSE.equals(r.breakType) || BREAK_CHAOS.equals(r.breakType)) {
            adjust -= 0.035d;
        }

        if (r.falseRecoveryRisk >= 50.0d) {
            adjust -= 0.010d;
        }

        if (r.breakRisk >= 50.0d) {
            adjust -= 0.010d;
        }

        if (adjust < -0.040d) {
            adjust = -0.040d;
        }

        if (adjust > 0.016d) {
            adjust = 0.016d;
        }

        return adjust;
    }

    private void applyFinalProtection(
            RecoveryResult r, Models.ShapeVector v, TrendStateMachine.TrendTrace trace) {
        r.hardBlock = 0;
        r.blockCode = "";

        if (BREAK_CHAOS.equals(r.breakType)) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_DATA_ERROR;
            return;
        }

        if (BREAK_REVERSE.equals(r.breakType)) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
            return;
        }

        if (BREAK_HEAVY.equals(r.breakType) && !RECOVERY_STRONG.equals(r.recoveryType)) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_BREAK_HIGH;
            return;
        }

        if (RECOVERY_FALSE.equals(r.recoveryType)) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_RECOVERY_WEAK;
            return;
        }

        if (r.falseRecoveryRisk >= 64.0d) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_RECOVERY_WEAK;
            return;
        }

        if (r.breakRisk >= 66.0d) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_BREAK_HIGH;
            return;
        }

        if (trace != null && trace.hardBlock == 1) {
            r.hardBlock = 1;
            r.blockCode = AppConfig.BLOCK_TREND_WEAK;
        }
    }

    private String buildBreakKey(
            Models.ShapeVector v, TrendStateMachine.TrendTrace trace, RecoveryResult r) {
        if (v == null || r == null) {
            return "BRK_NULL";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("P").append(v.position);
        sb.append("_M").append(v.stateMode == null ? AppConfig.MODE_CHAOS : v.stateMode);
        sb.append("_T")
                .append(
                        trace == null
                                ? TrendStateMachine.TRANSITION_UNKNOWN
                                : trace.transitionCode);
        sb.append("_B").append(r.breakType);
        sb.append("_R").append(r.recoveryType);
        sb.append("_BD").append(ShapeMath.bucketScore(v.breakDepth, 5));
        sb.append("_DR").append(ShapeMath.bucketScore(v.driftRisk, 5));
        sb.append("_RC").append(ShapeMath.bucketScore(v.recoveryScore, 5));
        sb.append("_FR").append(ShapeMath.bucketScore(r.falseRecoveryRisk, 5));

        return sb.toString();
    }

    private String buildDetail(RecoveryResult r) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(r.position);

        sb.append(" 断点=");
        sb.append(r.breakType);

        sb.append(" 恢复=");
        sb.append(r.recoveryType);

        sb.append(" BreakRisk=");
        sb.append(UiFormatter.score(r.breakRisk));

        sb.append(" RecoveryPower=");
        sb.append(UiFormatter.score(r.recoveryPower));

        sb.append(" FalseRecovery=");
        sb.append(UiFormatter.score(r.falseRecoveryRisk));

        sb.append(" RecoveryP=");
        sb.append(UiFormatter.probability(r.recoveryP));

        sb.append(" GateAdj=");
        sb.append(UiFormatter.signedProbability(r.gateAdjust));

        if (r.hardBlock == 1) {
            sb.append(" 结论=断点恢复不安全，强制HOLD");
        } else if (isSafeRecovery(r)) {
            sb.append(" 结论=恢复有效，可进入候选");
        } else {
            sb.append(" 结论=恢复不足，收紧观察");
        }

        return sb.toString();
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        return oldText + "；" + add;
    }

    public static final class RecoveryResult {
        public int position;
        public String stateMode;
        public String transitionCode;

        public String breakType;
        public String recoveryType;
        public String breakKey;

        public double breakDepth;
        public double driftRisk;
        public double recoveryScore;
        public double hotReverseRisk;
        public double falseStrongRisk;

        public double modeSwitchRisk;
        public double earlyBreakRisk;
        public double recoveryMomentum;
        public double trendContinuity;

        public int confirmCount;
        public int sameModeCount;

        public double breakRisk;
        public double recoveryPower;
        public double falseRecoveryRisk;
        public double recoveryP;
        public double gateAdjust;
        public double probabilityAdjust;

        public int hardBlock;
        public String blockCode;
        public String detail;

        public RecoveryResult() {
            position = 0;
            stateMode = AppConfig.MODE_CHAOS;
            transitionCode = TrendStateMachine.TRANSITION_UNKNOWN;
            breakType = BREAK_CHAOS;
            recoveryType = RECOVERY_NONE;
            breakKey = "";
            breakDepth = 100.0d;
            driftRisk = 100.0d;
            recoveryScore = 0.0d;
            hotReverseRisk = 100.0d;
            falseStrongRisk = 100.0d;
            modeSwitchRisk = 100.0d;
            earlyBreakRisk = 100.0d;
            recoveryMomentum = 0.0d;
            trendContinuity = 0.0d;
            confirmCount = 0;
            sameModeCount = 0;
            breakRisk = 100.0d;
            recoveryPower = 0.0d;
            falseRecoveryRisk = 100.0d;
            recoveryP = 0.0d;
            gateAdjust = 0.035d;
            probabilityAdjust = -0.015d;
            hardBlock = 1;
            blockCode = AppConfig.BLOCK_BREAK_HIGH;
            detail = "";
        }
    }

    /* STARCORE_TIANSHU_B05_BREAK_RECOVERY_EVIDENCE_ONLY */

    public static final String B05_BREAK_VERSION = "STARCORE_TIANSHU_B05_BREAK_RECOVERY_V1";

    public B05BreakEvidenceBundle buildB05Evidence(
            V51Models.DrawFrame[] history, V51Models.StateTrajectorySnapshot[] states) {
        long started = System.currentTimeMillis();
        V51Models.DrawFrame[] clean = V51PermutationFeatureEngine.normalizeHistory(history);
        if (clean.length < 12) return B05BreakEvidenceBundle.error("HISTORY_LT_12");
        B05BreakEvidencePacket[] packets = new B05BreakEvidencePacket[AppConfig.POSITION_COUNT];
        double meanBreak = 0.0d;
        double meanRecovery = 0.0d;
        double meanRisk = 0.0d;
        for (int p = 0; p < packets.length; p++) {
            V51Models.StateTrajectorySnapshot state =
                    states == null || p >= states.length ? null : states[p];
            double changeRisk = state == null ? 1.0d : state.changePointRisk;
            double confidence = state == null ? 0.0d : state.confidence;
            double remaining = state == null ? 0.0d : state.remainingLife;
            double shock = recentShock(clean, p, 6, 24);
            double reversal = recentReversal(clean, p, 12);
            double breakScore =
                    V51Math.clamp(
                            0.42d * changeRisk
                                    + 0.24d * shock
                                    + 0.18d * reversal
                                    + 0.16d * (1.0d - confidence),
                            0.0d,
                            1.0d);
            double recoveryScore =
                    V51Math.clamp(
                            0.34d
                                            * (state != null
                                                            && state.lifecycle
                                                                    == V51Enums.Lifecycle.RECOVER
                                                    ? 1.0d
                                                    : 0.0d)
                                    + 0.24d * (1.0d - changeRisk)
                                    + 0.18d * confidence
                                    + 0.14d * (1.0d - reversal)
                                    + 0.10d * Math.min(1.0d, remaining / 10.0d),
                            0.0d,
                            1.0d);
            double counter =
                    V51Math.clamp(
                            0.50d * (1.0d - breakScore)
                                    + 0.30d * recoveryScore
                                    + 0.20d * confidence,
                            0.0d,
                            1.0d);
            double risk =
                    V51Math.clamp(
                            0.68d * breakScore + 0.18d * shock + 0.14d * (1.0d - recoveryScore),
                            0.0d,
                            1.0d);
            String mode =
                    breakScore >= 0.78d
                            ? "BREAK"
                            : (recoveryScore >= 0.62d
                                    ? "RECOVER"
                                    : (breakScore >= 0.55d ? "WATCH" : "STABLE"));
            String checksum =
                    V51ChecksumEngine.sha256(
                            B05_BREAK_VERSION
                                    + "|"
                                    + (p + 1)
                                    + "|"
                                    + clean[clean.length - 1].issue
                                    + "|"
                                    + breakScore
                                    + "|"
                                    + recoveryScore
                                    + "|"
                                    + counter
                                    + "|"
                                    + risk
                                    + "|"
                                    + shock
                                    + "|"
                                    + reversal
                                    + "|"
                                    + confidence
                                    + "|"
                                    + remaining
                                    + "|"
                                    + mode);
            packets[p] =
                    new B05BreakEvidencePacket(
                            p + 1,
                            mode,
                            breakScore,
                            recoveryScore,
                            counter,
                            risk,
                            shock,
                            reversal,
                            confidence,
                            remaining,
                            clean.length,
                            checksum);
            meanBreak += breakScore;
            meanRecovery += recoveryScore;
            meanRisk += risk;
        }
        meanBreak /= packets.length;
        meanRecovery /= packets.length;
        meanRisk /= packets.length;
        return new B05BreakEvidenceBundle(
                true,
                packets,
                clean.length,
                clean[0].issue,
                clean[clean.length - 1].issue,
                meanBreak,
                meanRecovery,
                meanRisk,
                Math.max(0L, System.currentTimeMillis() - started),
                B05BreakEvidenceBundle.hash(
                        clean[clean.length - 1].issue, packets, meanBreak, meanRecovery, meanRisk),
                "");
    }

    public static List<B05BreakContractRule> b05BreakContractRules() {
        ArrayList<B05BreakContractRule> rules = new ArrayList<B05BreakContractRule>();
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-001",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_001",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-002",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_002",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-003",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_003",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-004",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_004",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-005",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_005",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-006",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_006",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-007",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_007",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-008",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_008",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-009",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_009",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-010",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_010",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-011",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_011",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-012",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_012",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-013",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_013",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-014",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_014",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-015",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_015",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-016",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_016",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-017",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_017",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-018",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_018",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-019",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_019",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-020",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_020",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-021",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_021",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-022",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_022",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-023",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_023",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-024",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_024",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-025",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_025",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-026",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_026",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-027",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_027",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-028",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_028",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-029",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_029",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-030",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_030",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-031",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_031",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-032",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_032",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-033",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_033",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-034",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_034",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-035",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_035",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-036",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_036",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-037",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_037",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-038",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_038",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-039",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_039",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-040",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_040",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-041",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_041",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-042",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_042",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-043",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_043",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-044",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_044",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-045",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_045",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-046",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_046",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-047",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_047",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-048",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_048",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-049",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_049",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-050",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_050",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-051",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_051",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-052",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_052",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-053",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_053",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-054",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_054",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-055",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_055",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-056",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_056",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-057",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_057",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-058",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_058",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-059",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_059",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-060",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_060",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-061",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_061",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-062",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_062",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-063",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_063",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-064",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_064",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-065",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_065",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-066",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_066",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-067",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_067",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-068",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_068",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-069",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_069",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-070",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_070",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-071",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_071",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-072",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_072",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-073",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_073",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-074",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_074",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-075",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_075",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-076",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_076",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-077",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_077",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-078",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_078",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-079",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_079",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-080",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_080",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-081",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_081",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-082",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_082",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-083",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_083",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-084",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_084",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-085",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_085",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-086",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_086",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-087",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_087",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-088",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_088",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-089",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_089",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-090",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_090",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-091",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_091",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-092",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_092",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-093",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_093",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-094",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_094",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-095",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_095",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-096",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_096",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-097",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_097",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-098",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_098",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-099",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_099",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-100",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_100",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-101",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_101",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-102",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_102",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-103",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_103",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-104",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_104",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-105",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_105",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-106",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_106",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-107",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_107",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-108",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_108",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-109",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_109",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-110",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_110",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-111",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_111",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-112",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_112",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-113",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_113",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-114",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_114",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-115",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_115",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-116",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_116",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-117",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_117",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-118",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_118",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-119",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_119",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-120",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_120",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-121",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_121",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-122",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_122",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-123",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_123",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-124",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_124",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-125",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_125",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-126",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_126",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-127",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_127",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-128",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_128",
                        true,
                        3));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-129",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_129",
                        true,
                        4));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-130",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_130",
                        true,
                        5));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-131",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_131",
                        true,
                        6));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-132",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_132",
                        true,
                        7));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-133",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_133",
                        true,
                        1));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-134",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_134",
                        true,
                        2));
        rules.add(
                new B05BreakContractRule(
                        "B05-BREAK-135",
                        "BreakRecoveryEngine",
                        "EVIDENCE_ONLY_BREAK_RECOVERY_135",
                        true,
                        3));
        return rules;
    }

    private static double recentShock(
            V51Models.DrawFrame[] history, int positionIndex, int shortWindow, int longWindow) {
        if (history == null || history.length < 2) return 1.0d;
        double shortMean = meanPosition(history, positionIndex, shortWindow);
        double longMean = meanPosition(history, positionIndex, longWindow);
        return V51Math.clamp(Math.abs(shortMean - longMean) / 9.0d, 0.0d, 1.0d);
    }

    private static double meanPosition(
            V51Models.DrawFrame[] history, int positionIndex, int window) {
        int start = Math.max(0, history.length - Math.max(1, window));
        double sum = 0.0d;
        int count = 0;
        for (int i = start; i < history.length; i++) {
            sum += history[i].permutation[positionIndex];
            count++;
        }
        return count == 0 ? 0.0d : sum / count;
    }

    private static double recentReversal(
            V51Models.DrawFrame[] history, int positionIndex, int window) {
        if (history == null || history.length < 3) return 0.0d;
        int start = Math.max(1, history.length - Math.max(3, window));
        int previousDirection = 0;
        int reverse = 0;
        int compared = 0;
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

    public static final class B05BreakEvidencePacket {
        public final int position;
        public final String mode;
        public final double breakScore;
        public final double recoveryScore;
        public final double counterEvidence;
        public final double risk;
        public final double shock;
        public final double reversal;
        public final double confidence;
        public final double remainingLife;
        public final int sample;
        public final String checksum;

        B05BreakEvidencePacket(
                int position,
                String mode,
                double breakScore,
                double recoveryScore,
                double counterEvidence,
                double risk,
                double shock,
                double reversal,
                double confidence,
                double remainingLife,
                int sample,
                String checksum) {
            this.position = position;
            this.mode = mode == null ? "" : mode;
            this.breakScore = V51Math.clamp(breakScore, 0.0d, 1.0d);
            this.recoveryScore = V51Math.clamp(recoveryScore, 0.0d, 1.0d);
            this.counterEvidence = V51Math.clamp(counterEvidence, 0.0d, 1.0d);
            this.risk = V51Math.clamp(risk, 0.0d, 1.0d);
            this.shock = V51Math.clamp(shock, 0.0d, 1.0d);
            this.reversal = V51Math.clamp(reversal, 0.0d, 1.0d);
            this.confidence = V51Math.clamp(confidence, 0.0d, 1.0d);
            this.remainingLife = Math.max(0.0d, V51Math.finite(remainingLife));
            this.sample = Math.max(0, sample);
            this.checksum = checksum == null ? "" : checksum;
        }

        public String canonical() {
            return position
                    + "|"
                    + mode
                    + "|"
                    + breakScore
                    + "|"
                    + recoveryScore
                    + "|"
                    + counterEvidence
                    + "|"
                    + risk
                    + "|"
                    + shock
                    + "|"
                    + reversal
                    + "|"
                    + confidence
                    + "|"
                    + remainingLife
                    + "|"
                    + sample
                    + "|"
                    + checksum;
        }
    }

    public static final class B05BreakEvidenceBundle {
        public final boolean valid;
        public final B05BreakEvidencePacket[] packets;
        public final int sample;
        public final String firstIssue;
        public final String lastIssue;
        public final double meanBreak;
        public final double meanRecovery;
        public final double meanRisk;
        public final long elapsedMs;
        public final String checksum;
        public final String detail;

        B05BreakEvidenceBundle(
                boolean valid,
                B05BreakEvidencePacket[] packets,
                int sample,
                String firstIssue,
                String lastIssue,
                double meanBreak,
                double meanRecovery,
                double meanRisk,
                long elapsedMs,
                String checksum,
                String detail) {
            this.valid = valid;
            this.packets = packets == null ? new B05BreakEvidencePacket[0] : packets;
            this.sample = Math.max(0, sample);
            this.firstIssue = firstIssue == null ? "" : firstIssue;
            this.lastIssue = lastIssue == null ? "" : lastIssue;
            this.meanBreak = V51Math.clamp(meanBreak, 0.0d, 1.0d);
            this.meanRecovery = V51Math.clamp(meanRecovery, 0.0d, 1.0d);
            this.meanRisk = V51Math.clamp(meanRisk, 0.0d, 1.0d);
            this.elapsedMs = Math.max(0L, elapsedMs);
            this.checksum = checksum == null ? "" : checksum;
            this.detail = detail == null ? "" : detail;
        }

        static B05BreakEvidenceBundle error(String detail) {
            return new B05BreakEvidenceBundle(
                    false,
                    new B05BreakEvidencePacket[0],
                    0,
                    "",
                    "",
                    1.0d,
                    0.0d,
                    1.0d,
                    0L,
                    V51ChecksumEngine.sha256(B05_BREAK_VERSION + "|ERROR|" + detail),
                    detail);
        }

        static String hash(
                String lastIssue,
                B05BreakEvidencePacket[] packets,
                double meanBreak,
                double meanRecovery,
                double meanRisk) {
            StringBuilder out = new StringBuilder();
            if (packets != null) {
                for (int i = 0; i < packets.length; i++)
                    out.append(packets[i].canonical()).append(';');
            }
            return V51ChecksumEngine.sha256(
                    B05_BREAK_VERSION
                            + "|"
                            + lastIssue
                            + "|"
                            + out.toString()
                            + "|"
                            + meanBreak
                            + "|"
                            + meanRecovery
                            + "|"
                            + meanRisk);
        }
    }

    public static final class B05BreakContractRule {
        public final String id;
        public final String owner;
        public final String invariant;
        public final boolean mandatory;
        public final int severity;

        B05BreakContractRule(
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
