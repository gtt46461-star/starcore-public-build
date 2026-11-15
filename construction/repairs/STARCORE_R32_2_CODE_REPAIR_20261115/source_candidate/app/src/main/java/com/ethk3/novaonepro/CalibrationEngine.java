package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CalibrationEngine
 *
 * <p>原版高胜率主链保护版
 *
 * <p>核心原则： 1. 校准层是胜率保护器，不是出号救援器。 2. 不允许校准主动抬高 finalP。 3. 不允许校准主动降低 dynamicGate。 4. 校准桶样本弱时
 * fail-open，只轻微保护，不能压死原版主链。 5. 校准桶样本足且证明概率虚高时，才允许强下压。 6. REAL_TRADE + BET + WIN/MISS 才进入真实校准学习。 7.
 * SHADOW / HOLD / DATA_ERROR / VOID 不污染真实校准桶。 8. REAL_TRADE 只学习 rawP，不 rawP + finalP 双写，避免反馈自激。 9.
 * applyToDecision 不改变 BET/HOLD/SHADOW 裁决结果，最终裁决只归 DecisionGateEngine。 10. Java7 / AIDE 兼容，无
 * lambda，无 stream，无第三方库。
 */
public final class CalibrationEngine {
    public static final String FREQUENCY_FLOOR_VERSION = AppConfig.FREQUENCY_FLOOR_VERSION;
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    /*
     * 胜率优先版：
     * 校准不主动向上抬概率，最多只保持原值。
     */
    private static final double MAX_UPWARD_ADJUST = 0.000d;

    /*
     * 总体最大下压上限。
     * 只有样本足、真实桶证明虚高时才允许接近这个上限。
     */
    private static final double MAX_DOWNWARD_ADJUST = 0.055d;

    /*
     * 样本弱保护：
     * 样本不足时不能按 stored bucket 重压。
     */
    private static final int MIN_BUCKET_SAMPLE_ANY = 12;
    private static final int MIN_BUCKET_SAMPLE_STABLE = 36;
    private static final int MIN_BUCKET_SAMPLE_STRONG = 80;
    private static final int MIN_BUCKET_SAMPLE_HARD = 160;

    private static final double SAMPLE_WEAK_MAX_DOWN = 0.012d;
    private static final double SAMPLE_LIGHT_MAX_DOWN = 0.020d;
    private static final double SAMPLE_STABLE_MAX_DOWN = 0.034d;
    private static final double SAMPLE_STRONG_MAX_DOWN = 0.046d;
    private static final double SAMPLE_HARD_MAX_DOWN = 0.055d;

    private static final double DOWN_LOW = 0.010d;
    private static final double DOWN_MID = 0.018d;
    private static final double DOWN_HIGH = 0.030d;
    private static final double DOWN_STRONG = 0.042d;
    private static final double DOWN_MAX = 0.055d;

    private static final double EXTREME_ERROR = 0.120d;
    private static final double WEAK_BUCKET_ERROR = 0.100d;

    private static final double SUSPICIOUS_NEGATIVE_ADJUST = -0.018d;
    private static final double HARD_NEGATIVE_ADJUST = -0.026d;
    private static final double EXTREME_NEGATIVE_ADJUST = -0.036d;

    public CalibrationEngine() {}

    public CalibrationSignal inspect(DbStore store, double rawP) {
        CalibrationSignal signal = new CalibrationSignal();

        rawP = safeProbability(rawP);
        signal.rawP = rawP;
        signal.bucketKey = bucketKey(rawP);

        if (rawP <= 0.0d) {
            signal.calibratedP = 0.0d;
            signal.finalP = 0.0d;
            signal.error = 1.0d;
            signal.adjust = 0.0d;
            signal.gateAdjust = 0.0d;
            signal.reliable = 0;
            signal.sampleCount = 0;
            signal.hitRate = 0.0d;
            signal.bucketLoaded = 0;
            signal.detail = buildDetail(signal);
            return signal;
        }

        CalibrationBucketView view = loadCalibrationBucketView(store, rawP);

        signal.sampleCount = view.sampleCount;
        signal.hitRate = view.hitRate;
        signal.recentSample = view.recentSample;
        signal.recentHitRate = view.recentHitRate;
        signal.bucketLoaded = view.loaded;

        double fallback = fallbackCalibrated(rawP, view);
        double stored = safeCalibratedFromStore(store, rawP, view);
        double calibrated = chooseStableCalibrated(rawP, fallback, stored, view);

        calibrated = hardLimitCalibrated(rawP, calibrated, view);
        calibrated = AppConfig.clampProbability(calibrated);

        signal.calibratedP = calibrated;
        signal.error = Math.abs(rawP - calibrated);
        signal.adjust = calibrated - rawP;

        /*
         * 胜率优先：校准不向上抬概率。
         */
        if (signal.adjust > MAX_UPWARD_ADJUST) {
            signal.adjust = MAX_UPWARD_ADJUST;
        }

        double maxDown = effectiveHardDownLimit(rawP, view);
        if (signal.adjust < -maxDown) {
            signal.adjust = -maxDown;
        }

        if (signal.adjust < -MAX_DOWNWARD_ADJUST) {
            signal.adjust = -MAX_DOWNWARD_ADJUST;
        }

        signal.finalP = AppConfig.clampProbability(rawP + signal.adjust);
        signal.error = Math.abs(rawP - signal.finalP);

        /*
         * reliable 只代表“校准温和可靠”，不是放行 BET。
         */
        if (view.sampleCount >= MIN_BUCKET_SAMPLE_STABLE && signal.error <= WEAK_BUCKET_ERROR) {
            signal.reliable = 1;
        } else if (view.sampleCount < MIN_BUCKET_SAMPLE_STABLE && signal.error <= 0.030d) {
            signal.reliable = 1;
        } else {
            signal.reliable = 0;
        }

        /*
         * gateAdjust 不允许为负。
         * 校准不能降低 dynamicGate，只能中性或提高门槛。
         */
        signal.gateAdjust = gateAdjustByError(signal.error, signal.adjust, view);
        if (signal.gateAdjust < 0.0d) {
            signal.gateAdjust = 0.0d;
        }

        signal.detail = buildDetail(signal);

        return signal;
    }

    public Models.ComboCandidate applyToCandidate(DbStore store, Models.ComboCandidate source) {
        if (source == null) {
            return null;
        }

        Models.ComboCandidate c = source.copy();

        if (!c.isValidK3()) {
            c.finalP = 0.0d;
            c.edge = -1.0d;
            c.blockCode = AppConfig.BLOCK_DATA_ERROR;
            c.reasonSummary = appendReason(c.reasonSummary, "校准失败：候选K3非法");
            return c;
        }

        normalizeCandidate(c);

        CalibrationSignal signal = inspect(store, c.rawP);

        c.calibratedP = signal.calibratedP;
        c.calibrationError = signal.error;

        c.finalP = mergeCandidateProbability(c, signal);

        /*
         * 校准不能降低门槛。
         */
        double gateAdjust = signal.gateAdjust;
        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        c.dynamicGate = AppConfig.clampProbability(c.dynamicGate + gateAdjust);
        c.edge = c.finalP - c.dynamicGate;

        /*
         * 校准只做风险标记，最终 BET/HOLD 仍由 DecisionGateEngine 裁决。
         * 样本弱时不允许校准层强行 block 原版主链。
         */
        if (c.finalP < 0.350d
                && c.edge < -0.012d
                && signal.sampleCount >= MIN_BUCKET_SAMPLE_STABLE) {
            setSoftBlock(c, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        if (signal.sampleCount >= MIN_BUCKET_SAMPLE_STRONG
                && signal.error > calibrationErrorHard()
                && signal.adjust <= HARD_NEGATIVE_ADJUST
                && c.edge < 0.010d) {
            setSoftBlock(c, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        if (isHardRiskCandidate(c) && c.edge < 0.014d) {
            setSoftBlock(c, AppConfig.BLOCK_LOW_PROBABILITY);
        }

        c.reasonSummary = appendReason(c.reasonSummary, signal.detail);

        return c;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidates(
            DbStore store, List<Models.ComboCandidate> input) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (input == null || input.size() == 0) {
            return out;
        }

        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);

            if (c == null) {
                continue;
            }

            Models.ComboCandidate calibrated = applyToCandidate(store, c);

            if (calibrated != null) {
                out.add(calibrated);
            }
        }

        sortByFinal(out);
        rerank(out);

        return out;
    }

    public ArrayList<Models.ComboCandidate> applyToCandidatesForBacktest(
            DbStore store, List<Models.ComboCandidate> input, String mode) {
        ArrayList<Models.ComboCandidate> out = applyToCandidates(store, input);

        mode = AppConfig.normalizeBacktestMode(mode);

        for (int i = 0; i < out.size(); i++) {
            Models.ComboCandidate c = out.get(i);

            if (c != null) {
                c.backtestMode = mode;
            }
        }

        return out;
    }

    public Models.ProbabilityResult applyToProbabilityResult(
            DbStore store, Models.ProbabilityResult source) {
        Models.ProbabilityResult out = new Models.ProbabilityResult();

        if (source == null) {
            out.rawP = 0.0d;
            out.calibratedP = 0.0d;
            out.finalP = 0.0d;
            out.dynamicGate = AppConfig.HIGH_ACCURACY_GATE;
            out.calibrationError = 1.0d;
            out.bucketKey = "NULL";
            return out;
        }

        copyProbabilityResult(source, out);

        out.rawP = safeProbability(source.rawP);
        out.dynamicGate = source.dynamicGate;
        out.bucketKey = bucketKey(out.rawP);

        CalibrationSignal signal = inspect(store, out.rawP);

        out.calibratedP = signal.calibratedP;
        out.finalP = mergeProbabilityResultProbability(source, signal);
        out.calibrationError = signal.error;
        out.gateAdjust = signal.gateAdjust;

        /*
         * 校准不能降低 dynamicGate。
         */
        double gateAdjust = signal.gateAdjust;
        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        out.dynamicGate = AppConfig.clampProbability(out.dynamicGate + gateAdjust);
        out.bucketKey = signal.bucketKey;

        return out;
    }

    public Models.Decision applyToDecision(DbStore store, Models.Decision source) {
        if (source == null) {
            return null;
        }

        Models.Decision d = source.copy();

        CalibrationSignal signal = inspect(store, d.rawP);

        d.calibratedP = signal.calibratedP;
        d.calibrationError = signal.error;

        /*
         * 注意：
         * 这里不改变 d.decision，不把 BET 改 HOLD，不把 HOLD 改 BET。
         * 最终裁决只归 DecisionGateEngine。
         */
        d.finalP = mergeDecisionProbability(d, signal);

        double gateAdjust = signal.gateAdjust;
        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        d.dynamicGate = AppConfig.clampProbability(d.dynamicGate + gateAdjust);
        d.edge = d.finalP - d.dynamicGate;

        /*
         * 已经是BET的正式裁决不在这里改裁决，只给风险说明。
         * 非BET才允许补低概率blockCode，避免影响已通过的真实主链保存。
         */
        if (signal.sampleCount >= MIN_BUCKET_SAMPLE_STRONG
                && signal.error > calibrationErrorHard()
                && signal.adjust <= HARD_NEGATIVE_ADJUST
                && d.edge < 0.010d
                && !AppConfig.DECISION_BET.equals(d.decision)) {
            d.blockCode = AppConfig.BLOCK_LOW_PROBABILITY;
        }

        d.reasonSummary = appendReason(d.reasonSummary, signal.detail);
        d.reasonText = d.reasonSummary;

        try {
            d.syncAction();
        } catch (Throwable e) {
        }

        return d;
    }

    public void learnRealBet(DbStore store, Models.Decision decision, Models.Draw actualDraw) {
        if (store == null || decision == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        if (!isCleanDecisionForCalibration(decision, actualDraw)) {
            return;
        }

        repairDecisionK3(decision);

        int actual = actualDraw.getNumberAtPosition(decision.position);

        if (!AppConfig.isValidNumber(actual)) {
            return;
        }

        boolean hit = strictContains(decision.n1, decision.n2, decision.n3, actual);

        /*
         * 胜率优先校准：
         * 真实校准只学习 rawP。
         * 不再 rawP + finalP 双写，避免 finalP 反馈自激。
         */
        updateCalibrationSafe(store, decision.rawP, hit);
    }

    public void learnTrade(DbStore store, Models.Trade trade, Models.Draw actualDraw) {
        if (store == null || trade == null || actualDraw == null || !actualDraw.isValid()) {
            return;
        }

        if (!isCleanTradeForCalibration(trade, actualDraw)) {
            return;
        }

        repairTradeK3(trade);

        int actual = actualDraw.getNumberAtPosition(trade.position);

        if (!AppConfig.isValidNumber(actual)) {
            return;
        }

        boolean hit = strictContains(trade.n1, trade.n2, trade.n3, actual);

        /*
         * 真实交易校准只学习 rawP。
         */
        updateCalibrationSafe(store, trade.rawP, hit);
    }

    public void learnShadowCandidate(
            DbStore store, Models.ComboCandidate candidate, Models.Draw actualDraw) {
        /*
         * 固定规则：
         * SHADOW 不写真实校准桶。
         * SHADOW 的中/不中交给 TeacherMatrixEngine / LearningEngine / AttributionEngine。
         */
        return;
    }

    public void learnShadowList(
            DbStore store, List<Models.ComboCandidate> candidates, Models.Draw actualDraw) {
        /*
         * 固定规则：
         * SHADOW 列表不写真实校准桶，避免观察样本污染 REAL_TRADE 校准。
         */
        return;
    }

    public void learnHoldObservation(
            DbStore store, Models.Decision holdDecision, Models.Draw actualDraw) {
        /*
         * 固定规则：
         * HOLD 不写真实校准桶。
         * HOLD 只做观察解释和教师矩阵反推，不进入真实概率校准。
         */
        return;
    }

    public boolean calibrationAllowsBet(CalibrationSignal signal, double edge) {
        if (signal == null) {
            return true;
        }

        /*
         * 校准只做辅助拦截，最终仍交给 DecisionGateEngine。
         * 样本弱时不强拦，避免压死原版主链。
         */
        if (signal.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            if (signal.finalP < 0.335d && edge < -0.020d) {
                return false;
            }
            return true;
        }

        if (signal.finalP < 0.350d && edge < -0.010d) {
            return false;
        }

        if (signal.sampleCount >= MIN_BUCKET_SAMPLE_STRONG
                && signal.error > calibrationErrorHard()
                && signal.adjust <= HARD_NEGATIVE_ADJUST
                && edge < 0.010d) {
            return false;
        }

        if (signal.sampleCount >= MIN_BUCKET_SAMPLE_HARD
                && signal.error > EXTREME_ERROR
                && signal.adjust <= EXTREME_NEGATIVE_ADJUST
                && edge < 0.018d) {
            return false;
        }

        return true;
    }

    public boolean candidateCalibrationAllowsBet(DbStore store, Models.ComboCandidate candidate) {
        if (candidate == null || !candidate.isValidK3()) {
            return false;
        }

        CalibrationSignal signal = inspect(store, candidate.rawP);
        double finalP = mergeCandidateProbability(candidate, signal);

        double gateAdjust = signal.gateAdjust;
        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        double edge = finalP - (candidate.dynamicGate + gateAdjust);

        return calibrationAllowsBet(signal, edge);
    }

    public double gateAdjustment(DbStore store, double rawP) {
        CalibrationSignal signal = inspect(store, rawP);

        /*
         * 校准不能降低门槛。
         */
        if (signal.gateAdjust < 0.0d) {
            return 0.0d;
        }

        return signal.gateAdjust;
    }

    public double calibratedProbability(DbStore store, double rawP) {
        CalibrationSignal signal = inspect(store, rawP);
        return signal.finalP;
    }

    public String explain(DbStore store, double rawP) {
        CalibrationSignal signal = inspect(store, rawP);
        return signal.detail == null ? "" : signal.detail;
    }

    public String explainCandidate(DbStore store, Models.ComboCandidate candidate) {
        if (candidate == null) {
            return "校准候选为空";
        }

        CalibrationSignal signal = inspect(store, candidate.rawP);

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(candidate.position);
        sb.append(" ");
        sb.append(UiFormatter.numberGroup(candidate.n1, candidate.n2, candidate.n3));

        sb.append(" RawP=");
        sb.append(UiFormatter.probability(candidate.rawP));

        sb.append(" Calibrated=");
        sb.append(UiFormatter.probability(signal.calibratedP));

        sb.append(" Final=");
        sb.append(UiFormatter.probability(signal.finalP));

        sb.append(" Error=");
        sb.append(UiFormatter.probability(signal.error));

        sb.append(" Adj=");
        sb.append(UiFormatter.signedProbability(signal.adjust));

        sb.append(" GateAdj=");
        sb.append(UiFormatter.signedProbability(signal.gateAdjust));

        sb.append(" Sample=");
        sb.append(signal.sampleCount);

        sb.append(" HitRate=");
        sb.append(UiFormatter.probability(signal.hitRate));

        if (candidateCalibrationAllowsBet(store, candidate)) {
            sb.append(" 结论=校准未发现足样本极端虚高");
        } else {
            sb.append(" 结论=校准发现足样本虚高，需要最终门拦截或转SHADOW/HOLD");
        }

        return sb.toString();
    }

    private double safeCalibratedFromStore(DbStore store, double rawP, CalibrationBucketView view) {
        double fallback = fallbackCalibrated(rawP, view);

        if (store == null) {
            return fallback;
        }

        /*
         * 校准桶样本不足时，不信任数据库低值，防止旧脏桶压死主链。
         */
        if (view == null || view.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            return fallback;
        }

        try {
            double stored = store.getCalibratedProbability(rawP);

            if (Double.isNaN(stored) || Double.isInfinite(stored) || stored <= 0.0d) {
                return fallback;
            }

            stored = AppConfig.clampProbability(stored);

            /*
             * 胜率优先：
             * 如果数据库桶显示更高，不允许用它抬高概率。
             */
            if (stored > rawP) {
                return fallback;
            }

            /*
             * 如果桶真实命中率并不差，不能因为旧存储值偏低就猛压。
             */
            if (view.hitRate > 0.0d && view.hitRate >= rawP - 0.015d) {
                double mildFloor = rawP - Math.min(effectiveHardDownLimit(rawP, view), 0.008d);
                if (stored < mildFloor) {
                    stored = mildFloor;
                }
            }

            double floor = rawP - effectiveHardDownLimit(rawP, view);

            if (stored < floor) {
                stored = floor;
            }

            return stored;
        } catch (Throwable e) {
            return fallback;
        }
    }

    private double chooseStableCalibrated(
            double rawP, double fallback, double stored, CalibrationBucketView view) {
        rawP = AppConfig.clampProbability(rawP);
        fallback = AppConfig.clampProbability(fallback);
        stored = AppConfig.clampProbability(stored);

        /*
         * 禁止校准向上抬概率。
         */
        if (stored > rawP) {
            stored = rawP;
        }

        if (fallback > rawP) {
            fallback = rawP;
        }

        if (view == null || view.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            return fallback;
        }

        /*
         * 如果 stored 比 fallback 更低，说明数据库桶发现风险。
         * 但是否强信 stored 要看样本强度。
         */
        if (stored < fallback) {
            double diff = fallback - stored;

            if (diff <= 0.006d) {
                return stored;
            }

            if (view.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
                return stored * 0.35d + fallback * 0.65d;
            }

            if (view.sampleCount < MIN_BUCKET_SAMPLE_STRONG) {
                return stored * 0.50d + fallback * 0.50d;
            }

            if (view.sampleCount < MIN_BUCKET_SAMPLE_HARD) {
                return stored * 0.62d + fallback * 0.38d;
            }

            return stored * 0.72d + fallback * 0.28d;
        }

        /*
         * 如果 stored 不比 fallback 更低，不允许它抬升。
         */
        return fallback;
    }

    private double hardLimitCalibrated(double rawP, double calibrated, CalibrationBucketView view) {
        rawP = AppConfig.clampProbability(rawP);
        calibrated = AppConfig.clampProbability(calibrated);

        double downLimit = effectiveHardDownLimit(rawP, view);

        if (calibrated < rawP - downLimit) {
            calibrated = rawP - downLimit;
        }

        if (calibrated > rawP + MAX_UPWARD_ADJUST) {
            calibrated = rawP + MAX_UPWARD_ADJUST;
        }

        return AppConfig.clampProbability(calibrated);
    }

    private double effectiveHardDownLimit(double rawP, CalibrationBucketView view) {
        double base = hardDownLimit(rawP);

        if (view == null || view.sampleCount <= 0) {
            return Math.min(base, SAMPLE_WEAK_MAX_DOWN);
        }

        if (view.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            return Math.min(base, SAMPLE_WEAK_MAX_DOWN);
        }

        if (view.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            return Math.min(base, SAMPLE_LIGHT_MAX_DOWN);
        }

        if (view.sampleCount < MIN_BUCKET_SAMPLE_STRONG) {
            return Math.min(base, SAMPLE_STABLE_MAX_DOWN);
        }

        if (view.sampleCount < MIN_BUCKET_SAMPLE_HARD) {
            return Math.min(base, SAMPLE_STRONG_MAX_DOWN);
        }

        return Math.min(base, SAMPLE_HARD_MAX_DOWN);
    }

    private double hardDownLimit(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.360d) {
            return DOWN_LOW;
        }

        if (rawP < 0.400d) {
            return DOWN_MID;
        }

        if (rawP < 0.430d) {
            return DOWN_HIGH;
        }

        if (rawP < 0.460d) {
            return DOWN_STRONG;
        }

        return DOWN_MAX;
    }

    private double fallbackCalibrated(double rawP, CalibrationBucketView view) {
        rawP = AppConfig.clampProbability(rawP);

        /*
         * fallback 也只允许下修，不允许上修。
         * 无样本或样本弱时，fallback 下修必须非常轻。
         */
        boolean weak = view == null || view.sampleCount < MIN_BUCKET_SAMPLE_STABLE;

        if (rawP < 0.340d) {
            return rawP;
        }

        if (rawP < 0.360d) {
            return rawP - (weak ? 0.000d : 0.001d);
        }

        if (rawP < 0.380d) {
            return rawP - (weak ? 0.001d : 0.003d);
        }

        if (rawP < 0.400d) {
            return rawP - (weak ? 0.002d : 0.006d);
        }

        if (rawP < 0.420d) {
            return rawP - (weak ? 0.004d : 0.010d);
        }

        if (rawP < 0.440d) {
            return rawP - (weak ? 0.006d : 0.014d);
        }

        if (rawP < 0.460d) {
            return rawP - (weak ? 0.008d : 0.018d);
        }

        return rawP - (weak ? 0.010d : 0.022d);
    }

    private double mergeCandidateProbability(Models.ComboCandidate c, CalibrationSignal signal) {
        if (c == null || signal == null) {
            return 0.0d;
        }

        double base = c.finalP > 0.0d ? c.finalP : c.rawP;
        base = safeProbability(base);

        double adjust = signal.adjust;

        /*
         * 校准不向上抬。
         */
        if (adjust > 0.0d) {
            adjust = 0.0d;
        }

        double strength = 0.50d;

        /*
         * 样本弱时，只轻校准，不能压死原版主链。
         */
        if (signal.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            strength = 0.20d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            strength = 0.35d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STRONG) {
            strength = 0.55d;
        } else {
            strength = 0.72d;
        }

        if (signal.error > calibrationErrorMax()) {
            strength += 0.08d;
        }

        if (signal.error > calibrationErrorHard()) {
            strength += 0.10d;
        }

        if (signal.adjust <= HARD_NEGATIVE_ADJUST) {
            strength += 0.08d;
        }

        if (isHardRiskCandidate(c)) {
            strength += 0.06d;
        }

        if (isStrongCandidate(c)
                && signal.error <= calibrationErrorMax()
                && signal.adjust > -0.010d) {
            strength -= 0.10d;
        }

        if (c.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            strength -= 0.04d;
        }

        if (c.comboSample >= AppConfig.COMBO_SAMPLE_HIGH) {
            strength -= 0.03d;
        }

        strength = clamp(strength, 0.15d, 1.00d);

        double finalP = base + adjust * strength;

        /*
         * 硬风险只轻扣，不在校准层直接打死。
         */
        if (isHardRiskCandidate(c) && signal.sampleCount >= MIN_BUCKET_SAMPLE_STABLE) {
            finalP -= 0.003d;
        }

        if (finalP > base) {
            finalP = base;
        }

        if (finalP < base - MAX_DOWNWARD_ADJUST) {
            finalP = base - MAX_DOWNWARD_ADJUST;
        }

        return AppConfig.clampProbability(finalP);
    }

    private double mergeDecisionProbability(Models.Decision d, CalibrationSignal signal) {
        if (d == null || signal == null) {
            return 0.0d;
        }

        double base = d.finalP > 0.0d ? d.finalP : d.rawP;
        base = safeProbability(base);

        double adjust = signal.adjust;

        if (adjust > 0.0d) {
            adjust = 0.0d;
        }

        double strength = 0.42d;

        if (signal.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            strength = 0.16d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            strength = 0.28d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STRONG) {
            strength = 0.46d;
        } else {
            strength = 0.60d;
        }

        if (signal.error > calibrationErrorMax()) {
            strength += 0.10d;
        }

        if (signal.error > calibrationErrorHard()) {
            strength += 0.10d;
        }

        if (signal.adjust <= HARD_NEGATIVE_ADJUST) {
            strength += 0.08d;
        }

        if (d.effectiveSample >= AppConfig.EFFECTIVE_SAMPLE_HIGH) {
            strength -= 0.04d;
        }

        if (d.comboSample >= AppConfig.COMBO_SAMPLE_HIGH) {
            strength -= 0.03d;
        }

        strength = clamp(strength, 0.12d, 1.00d);

        double finalP = base + adjust * strength;

        if (finalP > base) {
            finalP = base;
        }

        if (finalP < base - MAX_DOWNWARD_ADJUST) {
            finalP = base - MAX_DOWNWARD_ADJUST;
        }

        return AppConfig.clampProbability(finalP);
    }

    private double mergeProbabilityResultProbability(
            Models.ProbabilityResult source, CalibrationSignal signal) {
        if (source == null || signal == null) {
            return 0.0d;
        }

        double base = source.finalP > 0.0d ? source.finalP : source.rawP;
        base = safeProbability(base);

        double adjust = signal.adjust;

        if (adjust > 0.0d) {
            adjust = 0.0d;
        }

        double strength = 0.42d;

        if (signal.sampleCount < MIN_BUCKET_SAMPLE_ANY) {
            strength = 0.16d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            strength = 0.28d;
        } else if (signal.sampleCount < MIN_BUCKET_SAMPLE_STRONG) {
            strength = 0.46d;
        } else {
            strength = 0.60d;
        }

        if (signal.error > calibrationErrorMax()) {
            strength += 0.08d;
        }

        if (signal.error > calibrationErrorHard()) {
            strength += 0.10d;
        }

        strength = clamp(strength, 0.12d, 1.00d);

        double finalP = base + adjust * strength;

        if (finalP > base) {
            finalP = base;
        }

        if (finalP < base - MAX_DOWNWARD_ADJUST) {
            finalP = base - MAX_DOWNWARD_ADJUST;
        }

        return AppConfig.clampProbability(finalP);
    }

    private boolean isStrongCandidate(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (isBadState(c.stateMode)) {
            return false;
        }

        if (c.trendLife < trendLifeMin()) {
            return false;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            return false;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return false;
        }

        if (c.recoveryScore < AppConfig.RECOVERY_SCORE_MIN) {
            return false;
        }

        if (c.comboGap < AppConfig.COMBO_GAP_MIN && c.championGap < AppConfig.CHAMPION_GAP_MIN) {
            return false;
        }

        return true;
    }

    private boolean isHardRiskCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return false;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return true;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return true;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)
                || "REVERSAL".equals(c.stateMode)) {
            return true;
        }

        return false;
    }

    private boolean isBadState(String mode) {
        if (mode == null) {
            return true;
        }

        /*
         * CHAOS 不能在校准层当硬坏状态。
         * CHAOS 只属于状态风险，由 DecisionGate / DynamicGate 最终确认。
         */
        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode);
    }

    private double gateAdjustByError(double error, double adjust, CalibrationBucketView view) {
        double gateAdjust = 0.0d;

        /*
         * 样本不足时，校准不能主动抬门槛太多。
         */
        if (view == null || view.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            if (error > EXTREME_ERROR && adjust <= EXTREME_NEGATIVE_ADJUST) {
                return 0.003d;
            }
            return 0.0d;
        }

        /*
         * 校准层只允许抬门槛，不允许降门槛。
         */
        if (error > calibrationErrorMax() && adjust <= SUSPICIOUS_NEGATIVE_ADJUST) {
            gateAdjust += 0.003d;
        }

        if (error > calibrationErrorHard() && adjust <= HARD_NEGATIVE_ADJUST) {
            gateAdjust += 0.005d;
        }

        if (view.sampleCount >= MIN_BUCKET_SAMPLE_HARD
                && error > EXTREME_ERROR
                && adjust <= EXTREME_NEGATIVE_ADJUST) {
            gateAdjust += 0.007d;
        }

        if (adjust <= HARD_NEGATIVE_ADJUST) {
            gateAdjust += 0.002d;
        }

        if (gateAdjust < 0.0d) {
            gateAdjust = 0.0d;
        }

        if (gateAdjust > 0.014d) {
            gateAdjust = 0.014d;
        }

        return gateAdjust;
    }

    private String bucketKey(double rawP) {
        rawP = AppConfig.clampProbability(rawP);

        if (rawP < 0.320d) {
            return "P00_32";
        }

        if (rawP < 0.340d) {
            return "P32_34";
        }

        if (rawP < 0.360d) {
            return "P34_36";
        }

        if (rawP < 0.380d) {
            return "P36_38";
        }

        if (rawP < 0.400d) {
            return "P38_40";
        }

        if (rawP < 0.420d) {
            return "P40_42";
        }

        if (rawP < 0.440d) {
            return "P42_44";
        }

        if (rawP < 0.460d) {
            return "P44_46";
        }

        if (rawP < 0.480d) {
            return "P46_48";
        }

        return "P48_PLUS";
    }

    private void sortByFinal(List<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        Collections.sort(
                list,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        double sa = sortScore(a);
                        double sb = sortScore(b);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return 1;
                        }

                        if (b == null) {
                            return -1;
                        }

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    private double sortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP;

        score += c.edge * 0.60d;
        score += c.comboGap * 0.15d;
        score += c.championGap * 0.08d;

        score -= c.driftRisk / 100.0d * 0.014d;
        score -= c.breakDepth / 100.0d * 0.014d;

        if (c.calibrationError > calibrationErrorMax()) {
            score -= 0.004d;
        }

        if (c.calibrationError > calibrationErrorHard()) {
            score -= 0.008d;
        }

        if (c.blockCode != null
                && c.blockCode.length() > 0
                && !AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)) {
            score -= 0.030d;
        }

        return score;
    }

    private void rerank(List<Models.ComboCandidate> list) {
        if (list == null) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.ComboCandidate c = list.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private void normalizeCandidate(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0
                && AppConfig.isValidNumber(c.n1)
                && AppConfig.isValidNumber(c.n2)
                && AppConfig.isValidNumber(c.n3)) {
            try {
                c.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);
            } catch (Throwable e) {
                c.k3Mask = buildK3MaskLocal(c.n1, c.n2, c.n3);
            }
        }

        c.rawP = safeProbability(c.rawP);

        if (c.dynamicGate <= 0.0d
                || Double.isNaN(c.dynamicGate)
                || Double.isInfinite(c.dynamicGate)) {
            c.dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        c.dynamicGate = AppConfig.clampProbability(c.dynamicGate);

        if (c.finalP <= 0.0d || Double.isNaN(c.finalP) || Double.isInfinite(c.finalP)) {
            c.finalP = c.rawP;
        }

        c.finalP = AppConfig.clampProbability(c.finalP);
        c.edge = c.finalP - c.dynamicGate;
    }

    private void updateCalibrationSafe(DbStore store, double p, boolean hit) {
        if (store == null) {
            return;
        }

        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0d) {
            return;
        }

        p = AppConfig.clampProbability(p);

        try {
            store.updateCalibration(p, hit);
        } catch (Throwable e) {
        }
    }

    private String buildDetail(CalibrationSignal s) {
        if (s == null) {
            return "校准为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("胜率保护校准桶=");
        sb.append(s.bucketKey);

        sb.append(" RawP=");
        sb.append(UiFormatter.probability(s.rawP));

        sb.append(" CalP=");
        sb.append(UiFormatter.probability(s.calibratedP));

        sb.append(" FinalP=");
        sb.append(UiFormatter.probability(s.finalP));

        sb.append(" Error=");
        sb.append(UiFormatter.probability(s.error));

        sb.append(" Adj=");
        sb.append(UiFormatter.signedProbability(s.adjust));

        sb.append(" GateAdj=");
        sb.append(UiFormatter.signedProbability(s.gateAdjust));

        sb.append(" Sample=");
        sb.append(s.sampleCount);

        sb.append(" HitRate=");
        sb.append(UiFormatter.probability(s.hitRate));

        if (s.sampleCount < MIN_BUCKET_SAMPLE_STABLE) {
            sb.append("，结论=校准桶样本不足，轻校准保护，不压死原版主链");
        } else if (s.adjust < 0.0d) {
            sb.append("，结论=足样本发现概率虚高风险，已下修");
        } else {
            sb.append("，结论=未发现需上修信号，保持中性");
        }

        if (s.reliable == 1) {
            sb.append("，校准温和可信");
        } else {
            sb.append("，校准差异偏大，最终门需加严确认");
        }

        sb.append("，规则=校准不救援BET、不降低门槛、SHADOW/HOLD不进真实校准");

        return sb.toString();
    }

    private boolean isCleanDecisionForCalibration(Models.Decision d, Models.Draw draw) {
        if (d == null || draw == null || !draw.isValid()) {
            return false;
        }

        if (!isBetText(d.decision) && !isBetText(d.action)) {
            return false;
        }

        if (!isRealScopeStrict(d.statScope)) {
            return false;
        }

        if (d.settled != 1) {
            return false;
        }

        String result = normalizeResultText(d.result);

        if (!AppConfig.RESULT_WIN.equals(result) && !AppConfig.RESULT_MISS.equals(result)) {
            return false;
        }

        if (!sameIssue(d.targetIssue, draw.issue)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        if (d.n1 == d.n2 || d.n1 == d.n3 || d.n2 == d.n3) {
            return false;
        }

        return true;
    }

    private boolean isCleanTradeForCalibration(Models.Trade t, Models.Draw draw) {
        if (t == null || draw == null || !draw.isValid()) {
            return false;
        }

        if (!isBetText(t.action)) {
            return false;
        }

        if (!isRealScopeStrict(t.statScope)) {
            return false;
        }

        if (t.settled != 1) {
            return false;
        }

        String result = normalizeResultText(t.result);

        if (!AppConfig.RESULT_WIN.equals(result) && !AppConfig.RESULT_MISS.equals(result)) {
            return false;
        }

        if (!sameIssue(t.targetIssue, draw.issue)) {
            return false;
        }

        if (!AppConfig.isValidPosition(t.position)) {
            return false;
        }

        if (!AppConfig.isValidNumber(t.n1)
                || !AppConfig.isValidNumber(t.n2)
                || !AppConfig.isValidNumber(t.n3)) {
            return false;
        }

        if (t.n1 == t.n2 || t.n1 == t.n3 || t.n2 == t.n3) {
            return false;
        }

        return true;
    }

    private boolean isRealScopeStrict(String scope) {
        if (scope == null) {
            return false;
        }

        return AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope);
    }

    private boolean isRealScope(String scope) {
        if (scope == null || scope.length() == 0) {
            return false;
        }

        return AppConfig.STAT_SCOPE_REAL_TRADE.equals(scope);
    }

    private boolean isBetText(String value) {
        if (value == null) {
            return false;
        }

        String s = value.trim();

        if (s.length() == 0) {
            return false;
        }

        if (AppConfig.DECISION_BET.equals(s)) {
            return true;
        }

        try {
            if (AppConfig.ACTION_BET.equals(s)) {
                return true;
            }
        } catch (Throwable e) {
        }

        try {
            return AppConfig.DECISION_BET.equals(AppConfig.normalizeDecision(s));
        } catch (Throwable e) {
            return "BET".equalsIgnoreCase(s) || "出手".equals(s) || "下注".equals(s);
        }
    }

    private boolean strictContains(int n1, int n2, int n3, int actual) {
        if (!AppConfig.isValidNumber(actual)) {
            return false;
        }

        return actual == n1 || actual == n2 || actual == n3;
    }

    private boolean maskContains(int mask, int number) {
        if (!AppConfig.isValidNumber(number)) {
            return false;
        }

        return (mask & (1 << number)) != 0;
    }

    private void repairDecisionK3(Models.Decision d) {
        if (d == null) {
            return;
        }

        if (!Models.isValidK3Mask(d.k3Mask)
                && AppConfig.isValidNumber(d.n1)
                && AppConfig.isValidNumber(d.n2)
                && AppConfig.isValidNumber(d.n3)) {
            try {
                d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            } catch (Throwable e) {
                d.k3Mask = buildK3MaskLocal(d.n1, d.n2, d.n3);
            }
        }
    }

    private void repairTradeK3(Models.Trade t) {
        if (t == null) {
            return;
        }

        if (!Models.isValidK3Mask(t.k3Mask)
                && AppConfig.isValidNumber(t.n1)
                && AppConfig.isValidNumber(t.n2)
                && AppConfig.isValidNumber(t.n3)) {
            try {
                t.k3Mask = Models.buildK3Mask(t.n1, t.n2, t.n3);
            } catch (Throwable e) {
                t.k3Mask = buildK3MaskLocal(t.n1, t.n2, t.n3);
            }
        }
    }

    private int buildK3MaskLocal(int n1, int n2, int n3) {
        int mask = 0;

        if (AppConfig.isValidNumber(n1)) {
            mask |= (1 << n1);
        }

        if (AppConfig.isValidNumber(n2)) {
            mask |= (1 << n2);
        }

        if (AppConfig.isValidNumber(n3)) {
            mask |= (1 << n3);
        }

        return mask;
    }

    private double safeProbability(double p) {
        if (Double.isNaN(p) || Double.isInfinite(p) || p <= 0.0d) {
            return 0.0d;
        }

        return AppConfig.clampProbability(p);
    }

    private double calibrationErrorMax() {
        return readAppDouble("CALIBRATION_ERROR_MAX", 0.060d);
    }

    private double calibrationErrorHard() {
        return readAppDouble("CALIBRATION_ERROR_HARD", 0.095d);
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private double trendLifeMin() {
        return readAppDouble("TREND_LIFE_MIN", 55.0d);
    }

    private double similarHitRateMin() {
        return readAppDouble("SIMILAR_HIT_RATE_MIN", 0.360d);
    }

    private double readAppDouble(String fieldName, double def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);

                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double clamp(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }

        if (v < min) {
            return min;
        }

        if (v > max) {
            return max;
        }

        return v;
    }

    private String appendReason(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        if (oldText.indexOf(add) >= 0) {
            return oldText;
        }

        return oldText + "；" + add;
    }

    private void setSoftBlock(Models.ComboCandidate c, String block) {
        if (c == null || block == null || block.length() == 0) {
            return;
        }

        if (c.blockCode == null || c.blockCode.length() == 0) {
            c.blockCode = block;
            return;
        }

        if (AppConfig.BLOCK_LOW_PROBABILITY.equals(c.blockCode)
                || AppConfig.BLOCK_COMBO_WEAK.equals(c.blockCode)) {
            c.blockCode = block;
        }
    }

    private void copyProbabilityResult(Models.ProbabilityResult from, Models.ProbabilityResult to) {
        if (from == null || to == null) {
            return;
        }

        to.modelP = from.modelP;
        to.shapeP = from.shapeP;
        to.comboP = from.comboP;
        to.recoveryP = from.recoveryP;
        to.similarP = from.similarP;
        to.memoryP = from.memoryP;
        to.rawP = from.rawP;
        to.calibratedP = from.calibratedP;
        to.finalP = from.finalP;
        to.dynamicGate = from.dynamicGate;
        to.calibrationError = from.calibrationError;
        to.gateAdjust = from.gateAdjust;
        to.bucketKey = from.bucketKey;
        to.mode = from.mode;
        to.enrichLimit = from.enrichLimit;
        to.deepLimit = from.deepLimit;

        to.deepFeatureP = from.deepFeatureP;
        to.biasEvidenceP = from.biasEvidenceP;
        to.profileP = from.profileP;
        to.runtimeProfileP = from.runtimeProfileP;
        to.stateNumberProfileP = from.stateNumberProfileP;
        to.teacherMatrixP = from.teacherMatrixP;
        to.teacherMatrixAdjust = from.teacherMatrixAdjust;
        to.fallbackProfileP = from.fallbackProfileP;
        to.transitionP = from.transitionP;
        to.cycleP = from.cycleP;
        to.auditAdjustedP = from.auditAdjustedP;
        to.promotionP = from.promotionP;
        to.aPlusP = from.aPlusP;
        to.aiJudgeP = from.aiJudgeP;
        to.auditPenalty = from.auditPenalty;
        to.auditBoost = from.auditBoost;
    }

    private CalibrationBucketView loadCalibrationBucketView(DbStore store, double rawP) {
        CalibrationBucketView view = new CalibrationBucketView();
        view.bucketKey = bucketKey(rawP);

        if (store == null) {
            return view;
        }

        Object obj = null;

        obj =
                invokeStoreCalibrationMethod(
                        store,
                        "getCalibrationBucket",
                        new Class[] {double.class},
                        new Object[] {Double.valueOf(rawP)});
        if (obj == null) {
            obj =
                    invokeStoreCalibrationMethod(
                            store,
                            "getCalibration",
                            new Class[] {double.class},
                            new Object[] {Double.valueOf(rawP)});
        }
        if (obj == null) {
            obj =
                    invokeStoreCalibrationMethod(
                            store,
                            "readCalibration",
                            new Class[] {double.class},
                            new Object[] {Double.valueOf(rawP)});
        }
        if (obj == null) {
            obj =
                    invokeStoreCalibrationMethod(
                            store,
                            "getCalibrationBucket",
                            new Class[] {String.class},
                            new Object[] {view.bucketKey});
        }
        if (obj == null) {
            obj =
                    invokeStoreCalibrationMethod(
                            store,
                            "getCalibration",
                            new Class[] {String.class},
                            new Object[] {view.bucketKey});
        }
        if (obj == null) {
            obj =
                    invokeStoreCalibrationMethod(
                            store,
                            "readCalibration",
                            new Class[] {String.class},
                            new Object[] {view.bucketKey});
        }

        if (obj != null) {
            fillCalibrationBucketView(view, obj);
        }

        return view;
    }

    private Object invokeStoreCalibrationMethod(
            DbStore store, String name, Class[] types, Object[] args) {
        if (store == null || name == null || name.length() == 0) {
            return null;
        }

        try {
            Method m = store.getClass().getMethod(name, types);
            return m.invoke(store, args);
        } catch (Throwable e) {
            return null;
        }
    }

    private void fillCalibrationBucketView(CalibrationBucketView view, Object obj) {
        if (view == null || obj == null) {
            return;
        }

        view.loaded = 1;

        view.sampleCount = readIntField(obj, "sampleCount", view.sampleCount);
        view.hitCount = readIntField(obj, "hitCount", view.hitCount);
        view.hitRate = readDoubleField(obj, "realHitRate", view.hitRate);

        if (view.hitRate <= 0.0d) {
            view.hitRate = readDoubleField(obj, "hitRate", view.hitRate);
        }

        view.recentSample = readIntField(obj, "last240Sample", view.recentSample);
        view.recentHit = readIntField(obj, "last240Hit", view.recentHit);
        view.recentHitRate = readDoubleField(obj, "last240HitRate", view.recentHitRate);

        if (view.recentSample <= 0) {
            view.recentSample = readIntField(obj, "recentSample", view.recentSample);
        }

        if (view.recentHit <= 0) {
            view.recentHit = readIntField(obj, "recentHit", view.recentHit);
        }

        if (view.recentHitRate <= 0.0d) {
            view.recentHitRate = readDoubleField(obj, "recentHitRate", view.recentHitRate);
        }

        if (view.sampleCount > 0 && view.hitRate <= 0.0d && view.hitCount >= 0) {
            view.hitRate = ((double) view.hitCount) / ((double) view.sampleCount);
        }

        if (view.recentSample > 0 && view.recentHitRate <= 0.0d && view.recentHit >= 0) {
            view.recentHitRate = ((double) view.recentHit) / ((double) view.recentSample);
        }

        view.hitRate = AppConfig.clampProbability(view.hitRate);
        view.recentHitRate = AppConfig.clampProbability(view.recentHitRate);
    }

    private int readIntField(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readDoubleField(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();
                if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                    return d;
                }
            }

            if (v instanceof String) {
                double d2 = Double.parseDouble((String) v);
                if (!Double.isNaN(d2) && !Double.isInfinite(d2)) {
                    return d2;
                }
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private boolean sameIssue(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        if (a.equals(b)) {
            return true;
        }

        try {
            DataGuard guard = new DataGuard();
            return guard.isSameIssue(a, b);
        } catch (Throwable e) {
            return a.equals(b);
        }
    }

    private String normalizeResultText(String result) {
        if (result == null) {
            return "";
        }

        String r = result.trim();

        if (r.length() == 0) {
            return "";
        }

        String u = r.toUpperCase(java.util.Locale.ROOT);

        if ("WIN".equals(u) || "HIT".equals(u) || "命中".equals(r)) {
            return AppConfig.RESULT_WIN;
        }

        if ("MISS".equals(u) || "LOSE".equals(u) || "未中".equals(r)) {
            return AppConfig.RESULT_MISS;
        }

        if ("WAIT".equals(u) || "WAITING".equals(u) || "等待".equals(r)) {
            return AppConfig.RESULT_WAIT;
        }

        if ("DATA_ERROR".equals(u) || "ERROR".equals(u) || "数据异常".equals(r)) {
            return AppConfig.RESULT_DATA_ERROR;
        }

        if ("VOID".equals(u) || "作废".equals(r)) {
            return AppConfig.RESULT_VOID;
        }

        try {
            return AppConfig.normalizeResult(result);
        } catch (Throwable e) {
            return u;
        }
    }

    private static final class CalibrationBucketView {
        public String bucketKey;
        public int sampleCount;
        public int hitCount;
        public double hitRate;
        public int recentSample;
        public int recentHit;
        public double recentHitRate;
        public int loaded;

        public CalibrationBucketView() {
            bucketKey = "";
            sampleCount = 0;
            hitCount = 0;
            hitRate = 0.0d;
            recentSample = 0;
            recentHit = 0;
            recentHitRate = 0.0d;
            loaded = 0;
        }
    }

    public static final class CalibrationSignal {
        public String bucketKey;
        public double rawP;
        public double calibratedP;
        public double finalP;
        public double error;
        public double adjust;
        public double gateAdjust;
        public int reliable;
        public String detail;

        public int sampleCount;
        public int recentSample;
        public double hitRate;
        public double recentHitRate;
        public int bucketLoaded;

        public CalibrationSignal() {
            bucketKey = "";
            rawP = 0.0d;
            calibratedP = 0.0d;
            finalP = 0.0d;
            error = 1.0d;
            adjust = 0.0d;
            gateAdjust = 0.0d;
            reliable = 0;
            detail = "";

            sampleCount = 0;
            recentSample = 0;
            hitRate = 0.0d;
            recentHitRate = 0.0d;
            bucketLoaded = 0;
        }
    }
}
