package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ComboScannerEngine
 *
 * <p>原版高胜率主链恢复满血版：
 *
 * <p>1. 实时 scan() 完整生成 10位置 × 120组合 = 1200 候选。 2. 不再每个位置提前裁剪，避免真正强候选在 ProbabilityEngine 前被删掉。 3.
 * 实时只在全局排序后统一保留候选，且最少保留 DEFAULT_KEEP_LIMIT=60。 4. ComboScanner 只做候选生成和初步排序，不做最终 BET 裁决。 5.
 * 预拦截只保留硬风险： - 漂移硬风险 - 断点硬风险 - BROKEN - REVERSING 6. CHAOS 不是硬坏状态，只轻扣分。 7. 不接入 TeacherMatrix，不接入
 * Learning，不接入 Rescue。 8. finalP / edge 这里只是初筛参考，最终仍交给： ProbabilityEngine / CalibrationEngine /
 * DynamicGateEngine / DecisionGateEngine。
 *
 * <p>Java7 / AIDE 兼容。
 */
public final class ComboScannerEngine {
    public static final String V46_ROLE = "FULL_SCAN1200_LIGHT_BACKGROUND_COMPATIBILITY_PRODUCER";

    private static final int DEFAULT_KEEP_LIMIT = AppConfig.V46_PROBABILITY_CANDIDATE_COUNT;

    private final DataGuard dataGuard;
    private final MarkovEngine markovEngine;
    private final ArrayList<Integer> allMasks;

    public ComboScannerEngine() {
        dataGuard = new DataGuard();
        markovEngine = new MarkovEngine();
        allMasks = ShapeMath.allK3Masks();
    }

    /*
     * 实时扫描：
     * 恢复原版高胜率主链候选池逻辑。
     * 实时完整生成1200候选，然后只做全局Top保留。
     */
    public Models.CandidateBatch scan(List<Models.Draw> history, List<Models.ShapeVector> vectors) {
        return scanInternal(history, vectors, false);
    }

    /*
     * 后台完整扫描：回测/完整学习专用。
     * 保留 10位置 × 120组合 = 1200候选。
     */
    public Models.CandidateBatch scanFullForBacktest(
            List<Models.Draw> history, List<Models.ShapeVector> vectors) {
        return scanInternal(history, vectors, true);
    }

    /*
     * 兼容旧名字。
     */
    public Models.CandidateBatch scanFull(
            List<Models.Draw> history, List<Models.ShapeVector> vectors) {
        return scanFullForBacktest(history, vectors);
    }

    private Models.CandidateBatch scanInternal(
            List<Models.Draw> history, List<Models.ShapeVector> vectors, boolean fullMode) {
        Models.CandidateBatch batch = new Models.CandidateBatch();

        batch.scanMode = fullMode ? "FULL_1200" : "REALTIME_FULL_1200_TOP60";
        batch.fullScan = fullMode ? 1 : 0;

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean.size() == 0 || vectors == null || vectors.size() == 0) {
            batch.scannedCount = 0;
            batch.keptCount = 0;
            return batch;
        }

        Models.Draw latest = clean.get(clean.size() - 1);
        String baseIssue = latest.issue == null ? "" : latest.issue;
        String targetIssue = dataGuard.buildNextIssue(baseIssue);

        batch.baseIssue = baseIssue;
        batch.targetIssue = targetIssue;

        int scannedTotal = 0;

        /*
         * 关键恢复：
         * 实时和后台都先完整生成每位置120个K3候选。
         * 不再使用每位置提前Top裁剪。
         */
        for (int i = 0; i < vectors.size(); i++) {
            Models.ShapeVector v = vectors.get(i);

            if (v == null || !AppConfig.isValidPosition(v.position)) {
                continue;
            }

            ArrayList<Models.ComboCandidate> list =
                    scanPositionFull(clean, v, baseIssue, targetIssue);

            scannedTotal += allMasks == null ? 0 : allMasks.size();

            for (int j = 0; j < list.size(); j++) {
                Models.ComboCandidate c = list.get(j);

                if (c == null || !c.isValidK3()) {
                    continue;
                }

                c.backtestMode =
                        fullMode ? AppConfig.BACKTEST_MODE_NORMAL : AppConfig.BACKTEST_MODE_FAST;
                c.probabilityMode =
                        fullMode ? AppConfig.BACKTEST_MODE_NORMAL : AppConfig.BACKTEST_MODE_FAST;
                batch.add(c);
            }
        }

        batch.scannedCount = scannedTotal;

        sortCandidates(batch.candidates);
        reRank(batch.candidates);

        /*
         * 实时模式只做一次全局裁剪。
         * 保底120，避免配置过小导致候选池被提前砍残。
         */
        if (!fullMode) {
            int keep = safeRealtimeKeepTotal();

            trimCandidateList(batch.candidates, keep);
            reRank(batch.candidates);
        }

        batch.keptCount = batch.candidates == null ? 0 : batch.candidates.size();

        if (batch.candidates != null && batch.candidates.size() > 0) {
            batch.best = batch.candidates.get(0);
        }

        return batch;
    }

    private int safeRealtimeKeepTotal() {
        int keep = readAppInt("SCAN_REALTIME_KEEP_TOTAL", DEFAULT_KEEP_LIMIT);

        if (keep < DEFAULT_KEEP_LIMIT) {
            keep = DEFAULT_KEEP_LIMIT;
        }

        int maskCount = allMasks == null ? 120 : allMasks.size();

        if (maskCount <= 0) {
            maskCount = 120;
        }

        int maxAll = AppConfig.POSITION_COUNT * maskCount;

        if (keep > maxAll) {
            keep = maxAll;
        }

        if (keep <= 0) {
            keep = DEFAULT_KEEP_LIMIT;
        }

        return keep;
    }

    /*
     * 实时单位置扫描：
     * 为了恢复原版候选池，不再每位置提前Top裁剪。
     */
    public ArrayList<Models.ComboCandidate> scanPosition(
            List<Models.Draw> history,
            Models.ShapeVector vector,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        return scanPositionFull(clean, vector, baseIssue, targetIssue);
    }

    /*
     * 后台完整单位置扫描：保留该位置全部120个K3组合。
     */
    public ArrayList<Models.ComboCandidate> scanPositionFullForBacktest(
            List<Models.Draw> history,
            Models.ShapeVector vector,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        return scanPositionFull(clean, vector, baseIssue, targetIssue);
    }

    private ArrayList<Models.ComboCandidate> scanPositionFull(
            List<Models.Draw> clean,
            Models.ShapeVector vector,
            String baseIssue,
            String targetIssue) {
        return scanPositionFast(clean, vector, baseIssue, targetIssue, 0);
    }

    /*
     * keepLimit > 0 时才裁剪。
     * 当前实时主链已经不使用每位置裁剪。
     */
    private ArrayList<Models.ComboCandidate> scanPositionFast(
            List<Models.Draw> clean,
            Models.ShapeVector vector,
            String baseIssue,
            String targetIssue,
            int keepLimit) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (clean == null
                || clean.size() == 0
                || vector == null
                || !AppConfig.isValidPosition(vector.position)) {
            return out;
        }

        MarkovEngine.MarkovResult markov = null;

        try {
            markov = markovEngine.buildForPosition(clean, vector.position);
        } catch (Throwable e) {
            markov = null;
        }

        HistRateCache cache = buildHistRateCache(clean, vector.position);

        if (allMasks == null || allMasks.size() == 0) {
            return out;
        }

        for (int i = 0; i < allMasks.size(); i++) {
            int mask = allMasks.get(i).intValue();

            Models.ComboCandidate c =
                    buildCandidateFast(clean, vector, markov, cache, mask, baseIssue, targetIssue);

            if (c != null && c.isValidK3()) {
                out.add(c);
            }
        }

        sortCandidates(out);
        reRank(out);

        if (keepLimit > 0) {
            trimCandidateList(out, keepLimit);
            reRank(out);
        }

        return out;
    }

    public Models.ComboCandidate buildCandidate(
            List<Models.Draw> history,
            Models.ShapeVector vector,
            MarkovEngine.MarkovResult markov,
            int k3Mask,
            String baseIssue,
            String targetIssue) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
        HistRateCache cache = vector == null ? null : buildHistRateCache(clean, vector.position);

        return buildCandidateFast(clean, vector, markov, cache, k3Mask, baseIssue, targetIssue);
    }

    private Models.ComboCandidate buildCandidateFast(
            List<Models.Draw> history,
            Models.ShapeVector vector,
            MarkovEngine.MarkovResult markov,
            HistRateCache cache,
            int k3Mask,
            String baseIssue,
            String targetIssue) {
        if (vector == null
                || !AppConfig.isValidPosition(vector.position)
                || !Models.isValidK3Mask(k3Mask)) {
            return null;
        }

        int[] nums = Models.maskToNumbers(k3Mask);

        if (nums == null || nums.length < AppConfig.FIXED_K) {
            return null;
        }

        Models.ComboCandidate c = new Models.ComboCandidate();

        c.baseIssue = baseIssue == null ? "" : baseIssue;
        c.targetIssue = targetIssue == null ? "" : targetIssue;
        c.createdAt = System.currentTimeMillis();

        c.position = vector.position;
        c.setNumbers(nums[0], nums[1], nums[2]);
        c.k3Mask = k3Mask;

        c.shapeKey = vector.shapeKey == null ? "" : vector.shapeKey;
        c.stateMode = vector.stateMode == null ? AppConfig.MODE_CHAOS : vector.stateMode;

        c.trendLife = vector.trendLife;
        c.driftRisk = vector.driftRisk;
        c.breakDepth = vector.breakDepth;
        c.recoveryScore = vector.recoveryScore;
        c.championGap = vector.championGap;
        c.comboGap = ShapeMath.comboGap(vector.numberProbs, k3Mask);
        c.hotReverseRisk = vector.hotReverseRisk;
        c.falseStrongRisk = vector.falseStrongRisk;

        double windowP = ShapeMath.k3Probability(vector.numberProbs, k3Mask);
        double markovP =
                markov == null ? windowP : ShapeMath.k3Probability(markov.finalProbs, k3Mask);

        double hist64 = cache == null ? 0.0d : cache.hitRate(k3Mask, AppConfig.WINDOW_64);
        double hist128 = cache == null ? 0.0d : cache.hitRate(k3Mask, AppConfig.WINDOW_128);
        double hist240 = cache == null ? 0.0d : cache.hitRate(k3Mask, AppConfig.WINDOW_240);
        double hist480 = cache == null ? 0.0d : cache.hitRate(k3Mask, AppConfig.WINDOW_480);

        int historySize = history == null ? 0 : history.size();

        c.modelP =
                blendModelProbability(
                        windowP, markovP, hist64, hist128, hist240, hist480, historySize);

        c.shapeP = shapeProbability(vector);
        c.comboP = comboProbability(hist64, hist128, hist240, hist480, c.comboGap, historySize);
        c.recoveryP = recoveryProbability(vector);

        c.rawP = rawProbability(c, windowP, markovP);
        c.dynamicGate = baseGate(c);
        c.finalP = AppConfig.clampProbability(c.rawP - riskPenalty(c));
        c.edge = c.finalP - c.dynamicGate;

        c.comboHitRate = c.comboP;
        c.comboSample = Math.min(historySize, AppConfig.WINDOW_240);
        c.effectiveSample = estimateEffectiveSample(history, vector, c);

        c.backtestMode = AppConfig.BACKTEST_MODE_FAST;
        c.probabilityMode = AppConfig.BACKTEST_MODE_FAST;

        /*
         * 只做硬风险预标记。
         * 普通趋势弱、组合弱、位置弱，不在 ComboScanner 阶段提前软封杀。
         */
        applyPreBlock(c, vector);

        c.reasonSummary = buildReasonSummary(c, windowP, markovP, hist128, hist240);

        return c;
    }

    public Models.ComboCandidate pickBest(Models.CandidateBatch batch) {
        if (batch == null || batch.candidates == null || batch.candidates.size() == 0) {
            return null;
        }

        sortCandidates(batch.candidates);
        reRank(batch.candidates);

        return batch.candidates.get(0);
    }

    public ArrayList<Models.ComboCandidate> keepTop(List<Models.ComboCandidate> input, int limit) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (input == null || input.size() == 0) {
            return out;
        }

        if (limit <= 0) {
            limit = DEFAULT_KEEP_LIMIT;
        }

        ArrayList<Models.ComboCandidate> copy = new ArrayList<Models.ComboCandidate>();

        for (int i = 0; i < input.size(); i++) {
            Models.ComboCandidate c = input.get(i);

            if (c != null && c.isValidK3()) {
                copy.add(c.copy());
            }
        }

        sortCandidates(copy);
        reRank(copy);

        int max = Math.min(limit, copy.size());

        for (int i = 0; i < max; i++) {
            out.add(copy.get(i));
        }

        return out;
    }

    public ArrayList<Models.ComboCandidate> keepTopForShadow(Models.CandidateBatch batch) {
        if (batch == null) {
            return new ArrayList<Models.ComboCandidate>();
        }

        return keepTop(batch.candidates, AppConfig.SHADOW_SAVE_TOP_N);
    }

    public void sortCandidates(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        Collections.sort(
                candidates,
                new Comparator<Models.ComboCandidate>() {
                    @Override
                    public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                        if (a == null && b == null) {
                            return 0;
                        }

                        if (a == null) {
                            return 1;
                        }

                        if (b == null) {
                            return -1;
                        }

                        double sa = finalSortScore(a);
                        double sb = finalSortScore(b);

                        if (sa < sb) {
                            return 1;
                        }

                        if (sa > sb) {
                            return -1;
                        }

                        if (a.position != b.position) {
                            return a.position - b.position;
                        }

                        return a.k3Mask - b.k3Mask;
                    }
                });
    }

    public String explainTop(List<Models.ComboCandidate> candidates, int limit) {
        if (candidates == null || candidates.size() == 0) {
            return "候选池为空";
        }

        ArrayList<Models.ComboCandidate> top = keepTop(candidates, limit);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < top.size(); i++) {
            Models.ComboCandidate c = top.get(i);

            if (c == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append("#");
            sb.append(c.rank);
            sb.append(" P");
            sb.append(c.position);
            sb.append(" ");
            sb.append(UiFormatter.numberGroup(c.n1, c.n2, c.n3));
            sb.append(" FinalP=");
            sb.append(UiFormatter.probability(c.finalP));
            sb.append(" Gate=");
            sb.append(UiFormatter.probability(c.dynamicGate));
        }

        return sb.toString();
    }

    public boolean candidateBasicUsable(Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) {
            return false;
        }

        if (c.finalP <= 0.0d || c.dynamicGate <= 0.0d) {
            return false;
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            return false;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            return false;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            return false;
        }

        return true;
    }

    public double positionCandidateGap(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() < 2) {
            return 0.0d;
        }

        ArrayList<Models.ComboCandidate> top = keepTop(candidates, 20);

        if (top.size() < 2) {
            return 0.0d;
        }

        Models.ComboCandidate first = top.get(0);

        if (first == null) {
            return 0.0d;
        }

        for (int i = 1; i < top.size(); i++) {
            Models.ComboCandidate c = top.get(i);

            if (c == null) {
                continue;
            }

            if (c.position != first.position) {
                return finalSortScore(first) - finalSortScore(c);
            }
        }

        return finalSortScore(first) - finalSortScore(top.get(1));
    }

    private double blendModelProbability(
            double windowP,
            double markovP,
            double hist64,
            double hist128,
            double hist240,
            double hist480,
            int historySize) {
        double p;

        if (historySize < AppConfig.WINDOW_128) {
            p = windowP * 0.55d + markovP * 0.25d + hist64 * 0.20d;
        } else if (historySize < AppConfig.WINDOW_480) {
            p = windowP * 0.40d + markovP * 0.24d + hist64 * 0.16d + hist128 * 0.20d;
        } else {
            p =
                    windowP * 0.32d
                            + markovP * 0.22d
                            + hist64 * 0.12d
                            + hist128 * 0.14d
                            + hist240 * 0.14d
                            + hist480 * 0.06d;
        }

        if (p <= 0.0d) {
            p = AppConfig.RANDOM_K3_BASELINE;
        }

        return AppConfig.clampProbability(p);
    }

    private double shapeProbability(Models.ShapeVector v) {
        if (v == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        double p = AppConfig.RANDOM_K3_BASELINE;

        p += (v.trendLife - 70.0d) / 100.0d * 0.070d;
        p += (v.recoveryScore - 65.0d) / 100.0d * 0.050d;
        p += (35.0d - v.driftRisk) / 100.0d * 0.060d;
        p += (40.0d - v.breakDepth) / 100.0d * 0.050d;
        p += v.championGap * 0.35d;
        p += v.comboGap * 0.25d;

        if (AppConfig.MODE_ACCELERATING.equals(v.stateMode)) {
            p += 0.018d;
        } else if (AppConfig.MODE_CONFIRMED.equals(v.stateMode)) {
            p += 0.012d;
        } else if (AppConfig.MODE_RECOVERING.equals(v.stateMode)) {
            p += 0.006d;
        } else if (AppConfig.MODE_PULLBACK.equals(v.stateMode)) {
            p -= 0.008d;
        } else if (AppConfig.MODE_WEAKENING.equals(v.stateMode)) {
            p -= 0.016d;
        } else if (AppConfig.MODE_BROKEN.equals(v.stateMode)
                || AppConfig.MODE_REVERSING.equals(v.stateMode)) {
            p -= 0.040d;
        } else if (AppConfig.MODE_CHAOS.equals(v.stateMode)) {
            p -= 0.010d;
        }

        return AppConfig.clampProbability(p);
    }

    private double comboProbability(
            double hist64,
            double hist128,
            double hist240,
            double hist480,
            double comboGap,
            int historySize) {
        double p;

        if (historySize < AppConfig.WINDOW_128) {
            p = hist64;
        } else if (historySize < AppConfig.WINDOW_480) {
            p = hist64 * 0.45d + hist128 * 0.35d + hist240 * 0.20d;
        } else {
            p = hist64 * 0.25d + hist128 * 0.28d + hist240 * 0.32d + hist480 * 0.15d;
        }

        if (p <= 0.0d) {
            p = AppConfig.RANDOM_K3_BASELINE;
        }

        p += comboGap * 0.22d;

        return AppConfig.clampProbability(p);
    }

    private double recoveryProbability(Models.ShapeVector v) {
        if (v == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        double p = AppConfig.RANDOM_K3_BASELINE;

        p += (v.recoveryScore - 65.0d) / 100.0d * 0.060d;
        p -= (v.breakDepth - 35.0d) / 100.0d * 0.050d;
        p -= (v.driftRisk - 35.0d) / 100.0d * 0.040d;

        if (p < 0.26d) {
            p = 0.26d;
        }

        return AppConfig.clampProbability(p);
    }

    private double rawProbability(Models.ComboCandidate c, double windowP, double markovP) {
        if (c == null) {
            return AppConfig.RANDOM_K3_BASELINE;
        }

        double p =
                c.modelP * 0.38d
                        + c.shapeP * 0.22d
                        + c.comboP * 0.20d
                        + c.recoveryP * 0.10d
                        + windowP * 0.06d
                        + markovP * 0.04d;

        p += c.championGap * 0.10d;
        p += c.comboGap * 0.14d;

        if (p < 0.0d) {
            p = 0.0d;
        }

        return AppConfig.clampProbability(p);
    }

    /*
     * ComboScanner 阶段只是给初始 gate。
     * 后续 DynamicGateEngine 会重新计算/修正门槛。
     */
    private double baseGate(Models.ComboCandidate c) {
        double gate = AppConfig.DEFAULT_DYNAMIC_GATE;

        if (c == null) {
            return gate;
        }

        if (AppConfig.MODE_ACCELERATING.equals(c.stateMode)) {
            gate -= 0.004d;
        } else if (AppConfig.MODE_CONFIRMED.equals(c.stateMode)) {
            gate -= 0.002d;
        } else if (AppConfig.MODE_RECOVERING.equals(c.stateMode)) {
            gate += 0.003d;
        } else if (AppConfig.MODE_FORMING.equals(c.stateMode)) {
            gate += 0.010d;
        } else if (AppConfig.MODE_PULLBACK.equals(c.stateMode)) {
            gate += 0.016d;
        } else if (AppConfig.MODE_WEAKENING.equals(c.stateMode)) {
            gate += 0.022d;
        } else if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            gate += 0.035d;
        } else if (AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            gate += 0.010d;
        }

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            gate += (c.driftRisk - AppConfig.DRIFT_RISK_MAX) / 100.0d * 0.060d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            gate += (c.breakDepth - AppConfig.BREAK_DEPTH_MAX) / 100.0d * 0.055d;
        }

        if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            gate += (c.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) / 100.0d * 0.045d;
        }

        if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            gate += (c.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) / 100.0d * 0.040d;
        }

        if (gate < AppConfig.MIN_EXEC_GATE) {
            gate = AppConfig.MIN_EXEC_GATE;
        }

        if (gate > AppConfig.RISK_GATE_HIGH) {
            gate = AppConfig.RISK_GATE_HIGH;
        }

        return AppConfig.clampProbability(gate);
    }

    private double riskPenalty(Models.ComboCandidate c) {
        if (c == null) {
            return 0.040d;
        }

        double penalty = 0.0d;

        if (c.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            penalty += (c.driftRisk - AppConfig.DRIFT_RISK_MAX) / 100.0d * 0.045d;
        }

        if (c.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            penalty += (c.breakDepth - AppConfig.BREAK_DEPTH_MAX) / 100.0d * 0.045d;
        }

        if (c.hotReverseRisk > AppConfig.HOT_REVERSE_RISK_MAX) {
            penalty += (c.hotReverseRisk - AppConfig.HOT_REVERSE_RISK_MAX) / 100.0d * 0.035d;
        }

        if (c.falseStrongRisk > AppConfig.FALSE_STRONG_RISK_MAX) {
            penalty += (c.falseStrongRisk - AppConfig.FALSE_STRONG_RISK_MAX) / 100.0d * 0.035d;
        }

        /*
         * 这里只保留极轻惩罚，不在 ComboScanner 阶段软封杀。
         */
        if (c.comboGap < AppConfig.COMBO_GAP_MIN) {
            penalty += 0.002d;
        }

        if (c.championGap < AppConfig.CHAMPION_GAP_MIN) {
            penalty += 0.002d;
        }

        if (AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            penalty += 0.003d;
        }

        if (penalty > 0.045d) {
            penalty = 0.045d;
        }

        return penalty;
    }

    /*
     * 关键恢复：
     * ComboScanner 只标记硬风险，不再提前设置普通软拦截。
     *
     * 普通趋势弱、恢复弱、位置弱、组合弱，要交给：
     * ProbabilityEngine / CalibrationEngine / DynamicGateEngine / DecisionGateEngine。
     */
    private void applyPreBlock(Models.ComboCandidate c, Models.ShapeVector v) {
        if (c == null || v == null) {
            return;
        }

        c.blockCode = "";

        if (c.driftRisk >= AppConfig.DRIFT_RISK_HARD_BLOCK) {
            c.blockCode = AppConfig.BLOCK_DRIFT_HIGH;
            return;
        }

        if (c.breakDepth >= breakDepthHardBlock()) {
            c.blockCode = AppConfig.BLOCK_BREAK_HIGH;
            return;
        }

        if (AppConfig.MODE_BROKEN.equals(c.stateMode)
                || AppConfig.MODE_REVERSING.equals(c.stateMode)) {
            c.blockCode = trendWeakBlockCode();
        }
    }

    private int estimateEffectiveSample(
            List<Models.Draw> history, Models.ShapeVector v, Models.ComboCandidate c) {
        int size = history == null ? 0 : history.size();
        int sample = Math.min(size, AppConfig.WINDOW_240);

        if (v == null || c == null) {
            return sample;
        }

        if (v.driftRisk <= 30.0d) {
            sample += 40;
        }

        if (v.trendLife >= trendLifeMin()) {
            sample += 40;
        }

        if (c.comboGap >= AppConfig.COMBO_GAP_MIN) {
            sample += 30;
        }

        if (sample > size) {
            sample = size;
        }

        if (sample < 0) {
            sample = 0;
        }

        return sample;
    }

    /*
     * 初筛排序：
     * 这里不能替代最终门。
     * 不再因为普通软 blockCode 大幅惩罚候选。
     */
    private double finalSortScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP;

        score += c.edge * 0.55d;
        score += c.championGap * 0.10d;
        score += c.comboGap * 0.16d;
        score += (c.trendLife / 100.0d) * 0.014d;
        score += (c.recoveryScore / 100.0d) * 0.010d;
        score -= (c.driftRisk / 100.0d) * 0.016d;
        score -= (c.breakDepth / 100.0d) * 0.014d;

        if (AppConfig.MODE_CHAOS.equals(c.stateMode)) {
            score -= 0.006d;
        }

        /*
         * 只惩罚硬风险预标记。
         */
        if (isHardPreBlock(c.blockCode)) {
            score -= 0.040d;
        }

        if (!candidateBasicUsable(c)) {
            score -= 0.018d;
        }

        return score;
    }

    private boolean isHardPreBlock(String blockCode) {
        if (blockCode == null || blockCode.length() == 0) {
            return false;
        }

        return AppConfig.BLOCK_DRIFT_HIGH.equals(blockCode)
                || AppConfig.BLOCK_BREAK_HIGH.equals(blockCode)
                || trendWeakBlockCode().equals(blockCode);
    }

    private void reRank(List<Models.ComboCandidate> candidates) {
        if (candidates == null) {
            return;
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c != null) {
                c.rank = i + 1;
            }
        }
    }

    private void trimCandidateList(List<Models.ComboCandidate> candidates, int limit) {
        if (candidates == null || limit <= 0) {
            return;
        }

        while (candidates.size() > limit) {
            candidates.remove(candidates.size() - 1);
        }
    }

    private String buildReasonSummary(
            Models.ComboCandidate c,
            double windowP,
            double markovP,
            double hist128,
            double hist240) {
        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("ComboScanner=实时完整1200扫描");

        sb.append("，窗口P=");
        sb.append(UiFormatter.probability(windowP));

        sb.append("，MarkovP=");
        sb.append(UiFormatter.probability(markovP));

        sb.append("，H128=");
        sb.append(UiFormatter.probability(hist128));

        sb.append("，H240=");
        sb.append(UiFormatter.probability(hist240));

        sb.append("，形态=");
        sb.append(c.stateMode);

        sb.append("，TL=");
        sb.append(UiFormatter.score(c.trendLife));

        sb.append("，DR=");
        sb.append(UiFormatter.score(c.driftRisk));

        sb.append("，BD=");
        sb.append(UiFormatter.score(c.breakDepth));

        sb.append("，ComboGap=");
        sb.append(UiFormatter.probability(c.comboGap));

        if (c.blockCode != null && c.blockCode.length() > 0) {
            sb.append("，硬预警=");
            sb.append(c.blockCode);
        }

        sb.append("，说明=本层只生成候选和初筛排序，最终BET交给后续概率/校准/动态门/最终门");

        return sb.toString();
    }

    private HistRateCache buildHistRateCache(List<Models.Draw> history, int position) {
        HistRateCache cache = new HistRateCache();

        if (history == null || history.size() == 0 || !AppConfig.isValidPosition(position)) {
            return cache;
        }

        fillWindowFreq(cache.freq64, history, position, AppConfig.WINDOW_64);
        fillWindowFreq(cache.freq128, history, position, AppConfig.WINDOW_128);
        fillWindowFreq(cache.freq240, history, position, AppConfig.WINDOW_240);
        fillWindowFreq(cache.freq480, history, position, AppConfig.WINDOW_480);

        return cache;
    }

    private void fillWindowFreq(
            WindowFreq freq, List<Models.Draw> history, int position, int window) {
        if (freq == null
                || history == null
                || history.size() == 0
                || !AppConfig.isValidPosition(position)) {
            return;
        }

        int size = history.size();
        int start = size - window;

        if (start < 0) {
            start = 0;
        }

        int index = position - 1;

        for (int i = start; i < size; i++) {
            Models.Draw d = history.get(i);

            if (d == null || d.numbers == null || d.numbers.length < AppConfig.NUMBER_COUNT) {
                continue;
            }

            int n = d.numbers[index];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n >= 0 && n < freq.counts.length) {
                freq.counts[n]++;
                freq.sample++;
            }
        }
    }

    private String trendWeakBlockCode() {
        return readAppString("BLOCK_TREND_WEAK", "TREND_WEAK");
    }

    private double breakDepthHardBlock() {
        return readAppDouble("BREAK_DEPTH_HARD_BLOCK", AppConfig.BREAK_DEPTH_MAX + 18.0d);
    }

    private double trendLifeMin() {
        return readAppDouble("TREND_LIFE_MIN", 55.0d);
    }

    private int readAppInt(String fieldName, int def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

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

    private String readAppString(String fieldName, String def) {
        if (fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = AppConfig.class.getField(fieldName);
            Object v = f.get(null);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private static final class HistRateCache {
        final WindowFreq freq64 = new WindowFreq();
        final WindowFreq freq128 = new WindowFreq();
        final WindowFreq freq240 = new WindowFreq();
        final WindowFreq freq480 = new WindowFreq();

        double hitRate(int mask, int window) {
            WindowFreq f;

            if (window == AppConfig.WINDOW_64) {
                f = freq64;
            } else if (window == AppConfig.WINDOW_128) {
                f = freq128;
            } else if (window == AppConfig.WINDOW_240) {
                f = freq240;
            } else {
                f = freq480;
            }

            return f.hitRate(mask);
        }
    }

    private static final class WindowFreq {
        /*
         * 号码是 01-10，数组固定 11，避免依赖 AppConfig.MAX_NUMBER 导致旧项目编译失败。
         */
        final int[] counts = new int[11];
        int sample;

        double hitRate(int mask) {
            if (sample <= 0 || !Models.isValidK3Mask(mask)) {
                return 0.0d;
            }

            int[] nums = Models.maskToNumbers(mask);

            if (nums == null || nums.length < AppConfig.FIXED_K) {
                return 0.0d;
            }

            int hit = 0;

            for (int i = 0; i < AppConfig.FIXED_K; i++) {
                int n = nums[i];

                if (n >= 0 && n < counts.length) {
                    hit += counts[n];
                }
            }

            return ((double) hit) / ((double) sample);
        }
    }
}
