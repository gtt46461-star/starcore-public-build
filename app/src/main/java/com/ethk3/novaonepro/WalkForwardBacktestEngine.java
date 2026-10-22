package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class WalkForwardBacktestEngine {
    public static final String V46_ROLE = "STRICT_TIME_ORDERED_V46_REPLAY_NO_LEGACY_REAL_GATE";

    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final String WALK_FORWARD_MAIN_CHAIN_VERSION = AppConfig.V46_VERSION;
    private static final String WALK_FORWARD_CLOSED_LOOP_VERSION = AppConfig.V46_VERSION;
    private static final String WALK_FORWARD_NO_FUTURE_LEAK_VERSION = AppConfig.V46_VERSION;

    private static final int VECTOR_MEMORY_MAX_ROUNDS = 60;

    private static final int BACKTEST_PROBABILITY_CAP = AppConfig.V46_PROBABILITY_CANDIDATE_COUNT;
    private static final int BACKTEST_DECISION_CAP = AppConfig.V46_FORMAL_CANDIDATE_COUNT;

    private static final int FAST_CANDIDATE_CAP = BACKTEST_PROBABILITY_CAP;
    private static final int NORMAL_CANDIDATE_CAP = BACKTEST_PROBABILITY_CAP;
    private static final int FULL_CANDIDATE_CAP = BACKTEST_PROBABILITY_CAP;

    private static final int FAST_MAX_STEPS = 80;
    private static final int NORMAL_MAX_STEPS = 160;
    private static final int FULL_SAFE_MAX_STEPS = 360;
    private static final int FULL_SAFE_MIN_STEPS = 80;

    private static final int FAST_LOOKBACK_CAP = 420;
    private static final int NORMAL_LOOKBACK_CAP = 720;
    private static final int FULL_LOOKBACK_CAP = 900;

    private static final int AUTO_LEARN_WINDOW_SMALL = 300;
    private static final int AUTO_LEARN_WINDOW_BIG = 500;
    private static final int AUTO_LEARN_FLUSH_CASES = 120;
    private static final int AUTO_LEARN_PROGRESS_STEPS = 5;

    private static final long FAST_TIME_BUDGET_MS = 2200L;
    private static final long NORMAL_TIME_BUDGET_MS = 4500L;

    private static final double SIM_FREQ_LOW = AppConfig.TARGET_FREQ_MIN;
    private static final double SIM_FREQ_HIGH = AppConfig.TARGET_FREQ_MAX;
    private static final double SIM_FREQ_HARD_HIGH = AppConfig.TARGET_FREQ_HARD_MAX;
    private static final double SIM_MIN_REAL_P = AppConfig.MIN_REAL_BET_PROBABILITY;

    private static final double SIM_NON_STRONG_MIN_P = 0.395d;
    private static final double SIM_HIGH_FREQ_STRONG_P = 0.420d;
    private static final double SIM_HARD_FREQ_EXTRA_EDGE = 0.030d;

    private static final int V20_TEACHER_MIN_SAMPLE = 30;
    private static final double V20_TEACHER_STRONG_SCORE = 0.620d;
    private static final double V20_TEACHER_RISK_SCORE = 0.620d;
    private static final double V20_TEACHER_MAX_SORT_WEIGHT = 0.035d;
    private static final String V20_AUDIT_PREFIX = "V20严格前推";

    private final DataGuard dataGuard;
    private final WindowFeatureEngine windowFeatureEngine;
    private final ShapeStateEngine shapeStateEngine;
    private final TrendStateMachine trendStateMachine;
    private final MarkovEngine markovEngine;
    private final BreakRecoveryEngine breakRecoveryEngine;
    private final ComboScannerEngine comboScannerEngine;
    private final ProbabilityEngine probabilityEngine;
    private final CalibrationEngine calibrationEngine;
    private final DynamicGateEngine dynamicGateEngine;
    private final DecisionGateEngine decisionGateEngine;
    private final LearningEngine learningEngine;
    private final TeacherNumberMatrixEngine teacherNumberMatrixEngine;

    public WalkForwardBacktestEngine() {
        dataGuard = new DataGuard();
        windowFeatureEngine = new WindowFeatureEngine();
        shapeStateEngine = new ShapeStateEngine();
        trendStateMachine = new TrendStateMachine();
        markovEngine = new MarkovEngine();
        breakRecoveryEngine = new BreakRecoveryEngine();
        comboScannerEngine = new ComboScannerEngine();
        probabilityEngine = new ProbabilityEngine();
        calibrationEngine = new CalibrationEngine();
        dynamicGateEngine = new DynamicGateEngine();
        decisionGateEngine = new DecisionGateEngine();
        learningEngine = new LearningEngine();
        teacherNumberMatrixEngine = new TeacherNumberMatrixEngine();
    }

    public BacktestResult run(DbStore store, List<Models.Draw> history, int testWindow) {
        return runWithMode(store, history, testWindow, AppConfig.BACKTEST_MODE_FAST);
    }

    public BacktestResult runFast(DbStore store, List<Models.Draw> history, int testWindow) {
        return runWithMode(store, history, testWindow, AppConfig.BACKTEST_MODE_FAST);
    }

    public BacktestResult runNormal(DbStore store, List<Models.Draw> history, int testWindow) {
        return runWithMode(store, history, testWindow, AppConfig.BACKTEST_MODE_NORMAL);
    }

    public BacktestResult runFull(DbStore store, List<Models.Draw> history, int testWindow) {
        return runWithMode(store, history, testWindow, AppConfig.BACKTEST_MODE_FULL);
    }

    public BacktestResult runRecent(DbStore store, List<Models.Draw> history) {
        return runWithMode(
                store, history, AppConfig.BACKTEST_MID_WINDOW, AppConfig.BACKTEST_MODE_FAST);
    }

    public BacktestResult runRecentNormal(DbStore store, List<Models.Draw> history) {
        return runWithMode(
                store, history, AppConfig.BACKTEST_MID_WINDOW, AppConfig.BACKTEST_MODE_NORMAL);
    }

    public BacktestResult runRealtimeValidate(DbStore store, List<Models.Draw> history) {
        return runRealtimeValidate(store, history, AppConfig.AUTO_VALIDATE_DEFAULT_WINDOW);
    }

    public BacktestResult runRealtimeValidate(
            DbStore store, List<Models.Draw> history, int requestedWindow) {
        int window = AppConfig.normalizeAutoValidateWindow(requestedWindow);
        return runWithMode(null, history, window, AppConfig.BACKTEST_MODE_FAST);
    }

    public BacktestResult runAutoLearningSegment(DbStore store, List<Models.Draw> history) {
        return runAutoLearningSegment(store, history, AUTO_LEARN_WINDOW_SMALL);
    }

    public BacktestResult runAutoLearningSegment500(DbStore store, List<Models.Draw> history) {
        return runAutoLearningSegment(store, history, AUTO_LEARN_WINDOW_BIG);
    }

    public BacktestResult runAutoLearningSegment(
            DbStore store, List<Models.Draw> history, int requestedWindow) {
        BacktestResult empty = new BacktestResult();
        empty.mode = AppConfig.BACKTEST_MODE_FULL;
        empty.window = requestedWindow;
        empty.requestedWindow = requestedWindow;
        empty.createdAt = System.currentTimeMillis();
        empty.startedAt = empty.createdAt;

        if (store == null) {
            return failResult(empty, "后台学习失败：DbStore为空");
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean == null) {
            clean = new ArrayList<Models.Draw>();
        }

        int window = normalizeAutoLearnWindow(requestedWindow);
        empty.window = window;
        empty.requestedWindow = window;

        if (clean.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return failResult(empty, "后台学习失败：有效历史少于" + (AppConfig.MIN_DRAWS_ANALYZE + 2) + "期");
        }

        int need = AppConfig.MIN_DRAWS_ANALYZE + window + 1;
        String autoTaskKey = "AUTO_WF_W" + window;
        int startIndex = 0;
        int maxStart = clean.size() - need;

        if (maxStart < 0) {
            maxStart = 0;
        }

        try {
            Models.AutoLearnTask cursor = store.getAutoLearnCursor(autoTaskKey);

            if (cursor != null && cursor.lastStartIndex >= 0) {
                startIndex = cursor.lastStartIndex + Math.max(60, window / 2);
            }

            if (startIndex > maxStart) {
                startIndex = 0;
            }
        } catch (Throwable e) {
            startIndex = 0;
        }

        ArrayList<Models.Draw> segment = new ArrayList<Models.Draw>();

        if (clean.size() <= need) {
            for (int i = 0; i < clean.size(); i++) {
                segment.add(clean.get(i));
            }
        } else {
            int endExclusive = startIndex + need;

            if (endExclusive > clean.size()) {
                endExclusive = clean.size();
            }

            for (int i = startIndex; i < endExclusive; i++) {
                segment.add(clean.get(i));
            }
        }

        saveAutoCursor(
                store,
                autoTaskKey,
                "AUTO_SEGMENT",
                window,
                startIndex,
                startIndex + segment.size() - 1,
                startIndex + AppConfig.MIN_DRAWS_ANALYZE,
                startIndex,
                safeIssue(segment.size() > 0 ? segment.get(segment.size() - 1) : null),
                "后台自动学习启动：小段="
                        + startIndex
                        + "-"
                        + (startIndex + segment.size() - 1)
                        + "，窗口="
                        + window,
                "RUNNING",
                "");

        BacktestResult result = runWithMode(store, segment, window, AppConfig.BACKTEST_MODE_FULL);
        result.autoLearnSegment = 1;
        result.autoLearnStartIndex = startIndex;
        result.autoLearnEndIndex = startIndex + segment.size() - 1;

        String used =
                String.valueOf(startIndex) + "-" + String.valueOf(startIndex + segment.size() - 1);
        int nextStart = startIndex + Math.max(60, window / 2);

        if (nextStart > maxStart) {
            nextStart = 0;
        }

        saveAutoCursor(
                store,
                autoTaskKey,
                "AUTO_SEGMENT",
                window,
                startIndex,
                startIndex + segment.size() - 1,
                nextStart,
                nextStart,
                result.endIssue,
                "后台自动学习完成：保存位置样本=" + result.learningCaseCount + "，下次起点=" + nextStart,
                "IDLE",
                used);

        try {
            store.setState(
                    "auto_learn_summary",
                    "后台自动学习完成：W"
                            + window
                            + "｜段="
                            + used
                            + "｜样本="
                            + result.learningCaseCount
                            + "｜"
                            + result.summary);
            store.setState(
                    "learning_summary",
                    "后台学习已反哺画像缓存：位置样本=" + result.learningCaseCount + "，实时分析将读取缓存提速。");
        } catch (Throwable e) {
        }

        return result;
    }

    public Models.BacktestReport buildRealtimeValidationReport(
            DbStore store, List<Models.Draw> history) {
        return buildRealtimeValidationReport(
                store, history, AppConfig.AUTO_VALIDATE_DEFAULT_WINDOW);
    }

    public Models.BacktestReport buildRealtimeValidationReport(
            DbStore store, List<Models.Draw> history, int requestedWindow) {
        int window = AppConfig.normalizeAutoValidateWindow(requestedWindow);

        Models.BacktestReport report =
                buildModelReportWithMode(null, history, window, AppConfig.BACKTEST_MODE_FAST);
        report.type = "REALTIME_VALIDATE";
        report.manualTriggered = 0;

        if (report.window <= 0) {
            report.window = window;
        }

        if (report.summary == null || report.summary.length() == 0) {
            report.summary = "实时轻量验证：W" + report.window;
        }

        if (store != null) {
            store.saveRealtimeValidationReport(report);
        }

        return report;
    }

    public Models.BacktestReport buildManualFullReport(
            DbStore store, List<Models.Draw> history, int requestedWindow) {
        int window = AppConfig.normalizeManualBacktestWindow(requestedWindow);

        Models.BacktestReport report =
                buildModelReportWithMode(store, history, window, AppConfig.BACKTEST_MODE_FULL);
        report.type = "MANUAL_FULL_WALK_FORWARD_SAFE";
        report.manualTriggered = 1;

        if (report.summary == null) {
            report.summary = "";
        }

        if (report.summary.indexOf("安全抽样") < 0) {
            report.summary = report.summary + "｜FULL安全抽样：覆盖完整窗口范围，不再暴力逐期卡死";
        }

        if (store != null) {
            store.saveTeacherSnapshotFromBacktest(report);
            store.setState("teacher_snapshot_status", "OK");
            store.setState(
                    "teacher_snapshot_summary",
                    "手动FULL安全深测完成：覆盖完整窗口范围，测试步数="
                            + report.testCount
                            + "，学习样本="
                            + report.profileCount
                            + "，用于教师层校准");
        }

        return report;
    }

    public BacktestResult runWithMode(
            DbStore store, List<Models.Draw> history, int testWindow, String modeValue) {
        BacktestResult result = new BacktestResult();

        String mode = AppConfig.normalizeBacktestMode(modeValue);
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean == null) {
            clean = new ArrayList<Models.Draw>();
        }

        if (testWindow <= 0) {
            if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
                testWindow = AppConfig.MANUAL_BACKTEST_DEFAULT_WINDOW;
            } else {
                testWindow = AppConfig.BACKTEST_MID_WINDOW;
            }
        }

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            testWindow = AppConfig.normalizeManualBacktestWindow(testWindow);
        } else {
            if (testWindow > AppConfig.AUTO_BACKTEST_HEALTH_MAX_WINDOW) {
                testWindow = AppConfig.AUTO_BACKTEST_HEALTH_MAX_WINDOW;
            }

            if (testWindow < AppConfig.AUTO_BACKTEST_LIGHT_MIN_WINDOW) {
                testWindow = AppConfig.AUTO_BACKTEST_LIGHT_MIN_WINDOW;
            }
        }

        result.mode = mode;
        result.window = testWindow;
        result.requestedWindow = testWindow;
        result.createdAt = System.currentTimeMillis();
        result.startedAt = result.createdAt;

        if (clean.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            return failResult(result, "回测失败：有效历史少于" + (AppConfig.MIN_DRAWS_ANALYZE + 2) + "期");
        }

        int end = clean.size() - 1;
        int requestedStart = clean.size() - testWindow - 1;

        if (requestedStart < AppConfig.MIN_DRAWS_ANALYZE) {
            requestedStart = AppConfig.MIN_DRAWS_ANALYZE;
        }

        int start = requestedStart;

        if (!AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            int maxSteps = getMaxStepsForLightMode(mode);
            int lightStart = end - maxSteps;

            if (lightStart > start) {
                start = lightStart;
            }
        }

        if (start < AppConfig.MIN_DRAWS_ANALYZE) {
            start = AppConfig.MIN_DRAWS_ANALYZE;
        }

        if (start >= end) {
            return failResult(result, "回测失败：窗口不足，无法前推验证");
        }

        int fullRangeSteps = end - start;
        int fullSampleStride = 1;

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            int maxFullSteps = getFullSafeMaxSteps(testWindow);

            if (fullRangeSteps > maxFullSteps) {
                fullSampleStride =
                        (int) Math.ceil(((double) fullRangeSteps) / ((double) maxFullSteps));

                if (fullSampleStride < 1) {
                    fullSampleStride = 1;
                }

                result.sampled = 1;
                result.sampleStride = fullSampleStride;
                result.coveredWindow = fullRangeSteps;
            }
        }

        result.actualWindow = end - start;
        result.completeWindow = result.actualWindow >= testWindow ? 1 : 0;
        result.startIssue = safeIssue(clean.get(start));
        result.endIssue = safeIssue(clean.get(end));

        double equity = 0.0d;
        double peak = 0.0d;
        double maxDrawdown = 0.0d;

        int currentLoseStreak = 0;
        int longestLose = 0;

        long deadline = 0L;

        if (!AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            deadline = System.currentTimeMillis() + getLightTimeBudgetMs(mode);
        }

        ArrayList<Models.ShapeVector> recentVectorsMemory = new ArrayList<Models.ShapeVector>();
        ArrayList<Models.LearningCase> learningBuffer = new ArrayList<Models.LearningCase>();
        BacktestRuntime runtime = new BacktestRuntime();

        int trainLookback = getTrainLookback(mode);
        int pauseEvery = AppConfig.getBacktestPauseEverySteps(mode);
        long sleepMs = AppConfig.getBacktestCooperativeSleepMs(mode);

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            pauseEvery = 2;
            sleepMs = Math.max(2L, sleepMs);
        }

        if (pauseEvery <= 0) {
            pauseEvery = 8;
        }

        String taskKey = buildTaskKey(mode, testWindow);
        String segmentKey = String.valueOf(start) + "-" + String.valueOf(end);

        if (store != null) {
            store.setState("backtest_status", AppConfig.BACKTEST_STATUS_RUNNING);
            store.setState("backtest_mode", mode);
            store.setState("backtest_window", String.valueOf(testWindow));
            store.setState("backtest_progress", "0");

            saveAutoCursor(
                    store,
                    taskKey,
                    "WALK_FORWARD",
                    testWindow,
                    start,
                    end,
                    start + 1,
                    start,
                    result.startIssue,
                    "WalkForward学习启动：mode=" + mode + " W" + testWindow + " 段=" + segmentKey,
                    "RUNNING",
                    "");

            if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
                store.setState(
                        "backtest_last_summary",
                        "FULL安全回测启动：覆盖W" + testWindow + "，步长=" + fullSampleStride + "，后台小批量写学习样本");
            } else {
                store.setState(
                        "backtest_last_summary",
                        "轻量回测启动：" + mode + " W" + testWindow + "，后台同步提炼画像");
            }
        }

        for (int i = start; i < end; i++) {
            if (AppConfig.BACKTEST_MODE_FULL.equals(mode) && fullSampleStride > 1) {
                boolean take = ((i - start) % fullSampleStride == 0) || (i == end - 1);

                if (!take) {
                    continue;
                }
            }

            if (!AppConfig.BACKTEST_MODE_FULL.equals(mode)
                    && deadline > 0L
                    && System.currentTimeMillis() > deadline
                    && result.totalSteps >= getMinStepsBeforeTimeout(mode)) {
                result.timedOut = 1;
                result.timeoutReason = "自动轻量回测达到时间预算，已提前返回；FULL手动回测使用安全抽样或小段覆盖";
                break;
            }

            ArrayList<Models.Draw> train = sliceTail(clean, i + 1, trainLookback);
            Models.Draw actualNext = clean.get(i + 1);

            StepResult step =
                    runOneStep(
                            store,
                            train,
                            actualNext,
                            recentVectorsMemory,
                            runtime,
                            mode,
                            taskKey,
                            segmentKey,
                            i + 1);

            result.totalSteps++;
            result.sumCandidateCount += step.candidateCount;
            result.sumEnrichCount += step.enrichCount;
            result.sumDeepCount += step.deepCount;
            result.teacherMatrixAppliedCount += step.teacherMatrixApplied;
            result.teacherMatrixStrongCount += step.teacherMatrixStrong;
            result.teacherMatrixRiskCount += step.teacherMatrixRisk;
            result.teacherMatrixFieldWrittenCount += step.teacherMatrixFieldWritten;

            if (step.futureLeakBlocked) {
                result.futureLeakBlockCount++;
            }

            if (step.frequencyTurnHold) {
                result.frequencyGuardHoldCount++;
            }

            if (step.earlySampleHold) {
                result.earlySampleHoldCount++;
            }

            if (step.exceptionHold) {
                result.exceptionHoldCount++;
            }

            if (step.learningCases != null && step.learningCases.size() > 0) {
                for (int k = 0; k < step.learningCases.size(); k++) {
                    Models.LearningCase x = step.learningCases.get(k);

                    if (x != null) {
                        learningBuffer.add(x);
                    }
                }

                result.learningCaseCount += step.learningCases.size();
                result.learnedPositionCount += step.learningCases.size();
                result.learnedHitCount += step.learningHitCount;
                result.learnedMissCount += step.learningMissCount;
            }

            if (store != null && learningBuffer.size() >= AUTO_LEARN_FLUSH_CASES) {
                result.learningFlushCount += flushLearningCases(store, learningBuffer);
            }

            if (step.decision != null) {
                normalizeDecisionForBacktest(step.decision, train, actualNext);
                runtime.addDecision(step.decision);

                if (isRealBacktestBet(step.decision)) {
                    result.betCount++;

                    if (step.hit) {
                        result.hitCount++;
                        equity += AppConfig.WIN_NET_PROFIT;
                        currentLoseStreak = 0;
                    } else {
                        result.missCount++;
                        equity += AppConfig.LOSE_NET_PROFIT;
                        currentLoseStreak++;

                        if (currentLoseStreak > longestLose) {
                            longestLose = currentLoseStreak;
                        }
                    }

                    if (step.decision.rawP > 0.0d) {
                        result.sumRawP += step.decision.rawP;
                    }

                    if (step.decision.finalP > 0.0d) {
                        result.sumFinalP += step.decision.finalP;
                    }

                    if (step.decision.dynamicGate > 0.0d) {
                        result.sumGate += step.decision.dynamicGate;
                    }

                    result.sumEdge += step.decision.edge;

                    if (step.missReason != null && step.missReason.length() > 0) {
                        result.lastMissReason = step.missReason;
                        addMissReason(result, step.missReason);
                    }
                } else if (AppConfig.DECISION_SHADOW.equals(step.decision.decision)) {
                    result.shadowCount++;
                } else {
                    result.holdCount++;
                    addHoldReason(result, step.decision.blockCode);
                }
            } else {
                result.holdCount++;
                result.holdScopeCount++;
            }

            if (equity > peak) {
                peak = equity;
            }

            double drawdown = peak - equity;

            if (drawdown > maxDrawdown) {
                maxDrawdown = drawdown;
            }

            result.pnl = equity;
            result.maxDrawdown = maxDrawdown;
            result.longestLose = longestLose;

            if (step.currentVectors != null && step.currentVectors.size() > 0) {
                appendVectorMemory(recentVectorsMemory, step.currentVectors);
            }

            if (store != null && result.totalSteps % AUTO_LEARN_PROGRESS_STEPS == 0) {
                int progress = calcProgress(i - start + 1, end - start);
                store.setState("backtest_progress", String.valueOf(progress));

                saveAutoCursor(
                        store,
                        taskKey,
                        "WALK_FORWARD",
                        testWindow,
                        start,
                        end,
                        i + 1,
                        start,
                        safeIssue(actualNext),
                        "学习中："
                                + progress
                                + "%｜步数="
                                + result.totalSteps
                                + "｜位置样本="
                                + result.learningCaseCount
                                + "｜教师="
                                + result.teacherMatrixAppliedCount
                                + "｜BET="
                                + result.betCount
                                + "｜HOLD="
                                + result.holdCount,
                        "RUNNING",
                        "");

                if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
                    store.setState(
                            "backtest_last_summary",
                            "FULL安全回测中 W"
                                    + testWindow
                                    + "｜进度"
                                    + progress
                                    + "%"
                                    + "｜已算步数"
                                    + result.totalSteps
                                    + "｜学习样本"
                                    + result.learningCaseCount
                                    + "｜抽样步长"
                                    + fullSampleStride);
                } else {
                    store.setState(
                            "backtest_last_summary",
                            "回测中 "
                                    + mode
                                    + " W"
                                    + testWindow
                                    + "｜"
                                    + progress
                                    + "%｜学习样本="
                                    + result.learningCaseCount);
                }
            }

            if (result.totalSteps % pauseEvery == 0) {
                cooperativePause(sleepMs);
            }
        }

        if (store != null && learningBuffer.size() > 0) {
            result.learningFlushCount += flushLearningCases(store, learningBuffer);
        }

        finishResult(result);

        result.ok = 1;
        result.finishedAt = System.currentTimeMillis();
        result.costMs = result.finishedAt - result.startedAt;
        result.summary = buildSummary(result);

        if (store != null) {
            store.setState("backtest_status", AppConfig.BACKTEST_STATUS_OK);
            store.setState("backtest_progress", "100");
            store.setState("backtest_last_summary", result.summary);
            store.setState(
                    "learning_summary",
                    "WalkForward学习完成：位置样本="
                            + result.learningCaseCount
                            + "｜教师矩阵候选="
                            + result.teacherMatrixAppliedCount
                            + "｜命中样本="
                            + result.learnedHitCount
                            + "｜偏差样本="
                            + result.learnedMissCount);

            saveAutoCursor(
                    store,
                    taskKey,
                    "WALK_FORWARD",
                    testWindow,
                    start,
                    end,
                    end,
                    start,
                    result.endIssue,
                    "完成：学习样本="
                            + result.learningCaseCount
                            + "｜BET="
                            + result.betCount
                            + "｜胜率="
                            + UiFormatter.probability(result.winRate)
                            + "｜频率="
                            + UiFormatter.probability(result.frequency),
                    "IDLE",
                    segmentKey);
        }

        return result;
    }

    private int flushLearningCases(DbStore store, ArrayList<Models.LearningCase> buffer) {
        if (store == null || buffer == null || buffer.size() == 0) {
            return 0;
        }

        ArrayList<Models.LearningCase> copy = new ArrayList<Models.LearningCase>();

        for (int i = 0; i < buffer.size(); i++) {
            Models.LearningCase x = buffer.get(i);

            if (x != null) {
                copy.add(x);
            }
        }

        buffer.clear();

        if (copy.size() == 0) {
            return 0;
        }

        try {
            return store.saveLearningCases(copy);
        } catch (Throwable e) {
            return 0;
        }
    }

    private void saveAutoCursor(
            DbStore store,
            String taskKey,
            String taskType,
            int windowSize,
            int segmentStart,
            int segmentEnd,
            int nextTarget,
            int lastStart,
            String lastIssue,
            String progress,
            String status,
            String usedSegments) {

        if (store == null || taskKey == null || taskKey.length() == 0) {
            return;
        }

        try {
            Models.AutoLearnTask t = new Models.AutoLearnTask();

            t.taskKey = taskKey;
            t.taskType = taskType == null ? "" : taskType;
            t.windowSize = windowSize;
            t.segmentStartIndex = segmentStart;
            t.segmentEndIndex = segmentEnd;
            t.nextTargetIndex = nextTarget;
            t.lastStartIndex = lastStart;
            t.randomSeed = 0L;
            t.lastIssue = lastIssue == null ? "" : lastIssue;
            t.progress = progress == null ? "" : progress;
            t.status = status == null || status.length() == 0 ? "IDLE" : status;
            t.usedSegments = usedSegments == null ? "" : usedSegments;
            t.updatedAt = System.currentTimeMillis();

            store.saveAutoLearnCursor(t);
        } catch (Throwable e) {
        }
    }

    private int normalizeAutoLearnWindow(int requested) {
        if (requested <= 0) {
            return AUTO_LEARN_WINDOW_SMALL;
        }

        if (requested <= 360) {
            return AUTO_LEARN_WINDOW_SMALL;
        }

        return AUTO_LEARN_WINDOW_BIG;
    }

    private String buildTaskKey(String mode, int window) {
        return "WF_" + AppConfig.normalizeBacktestMode(mode) + "_W" + window;
    }

    private int getFullSafeMaxSteps(int requestedWindow) {
        if (requestedWindow <= 0) {
            return FULL_SAFE_MIN_STEPS;
        }

        if (requestedWindow <= AUTO_LEARN_WINDOW_BIG) {
            return requestedWindow;
        }

        int max = FULL_SAFE_MAX_STEPS;

        if (max < FULL_SAFE_MIN_STEPS) {
            max = FULL_SAFE_MIN_STEPS;
        }

        return max;
    }

    private int calcProgress(int done, int total) {
        if (total <= 0) {
            return 0;
        }

        int p = (int) Math.round(((double) done) * 100.0d / ((double) total));

        if (p < 0) {
            p = 0;
        }

        if (p > 100) {
            p = 100;
        }

        return p;
    }

    public ArrayList<BacktestResult> runStandardWindows(DbStore store, List<Models.Draw> history) {
        return runStandardWindowsWithMode(store, history, AppConfig.BACKTEST_MODE_FAST);
    }

    public ArrayList<BacktestResult> runStandardWindowsWithMode(
            DbStore store, List<Models.Draw> history, String modeValue) {
        ArrayList<BacktestResult> out = new ArrayList<BacktestResult>();

        String mode = AppConfig.normalizeBacktestMode(modeValue);

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            out.add(runWithMode(store, history, AppConfig.BACKTEST_MICRO_WINDOW, mode));
            out.add(runWithMode(store, history, AppConfig.BACKTEST_MID_WINDOW, mode));
            out.add(runWithMode(store, history, AppConfig.BACKTEST_BIG_WINDOW, mode));
            return out;
        }

        out.add(runWithMode(store, history, AppConfig.BACKTEST_MICRO_WINDOW, mode));
        out.add(runWithMode(store, history, AppConfig.BACKTEST_MID_WINDOW, mode));
        out.add(runWithMode(store, history, AppConfig.BACKTEST_BIG_WINDOW, mode));

        return out;
    }

    public String buildStandardSummary(DbStore store, List<Models.Draw> history) {
        ArrayList<BacktestResult> list =
                runStandardWindowsWithMode(store, history, AppConfig.BACKTEST_MODE_FAST);

        if (list.size() == 0) {
            return "暂无回测结果";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            BacktestResult r = list.get(i);

            if (r == null) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" | ");
            }

            sb.append(r.mode);
            sb.append("-W");
            sb.append(r.window);
            sb.append(":");

            if (r.ok == 0) {
                sb.append("不足");
            } else {
                sb.append("频率");
                sb.append(UiFormatter.probability(r.frequency));
                sb.append(" 胜率");
                sb.append(UiFormatter.probability(r.winRate));
                sb.append(" 盈亏");
                sb.append(UiFormatter.money(r.pnl));
                sb.append(" DD");
                sb.append(UiFormatter.money(r.maxDrawdown));
                sb.append(" 学习");
                sb.append(r.learningCaseCount);
            }
        }

        return sb.toString();
    }

    public Models.BacktestReport buildModelReport(
            DbStore store, List<Models.Draw> history, int window) {
        return buildModelReportWithMode(store, history, window, AppConfig.BACKTEST_MODE_FAST);
    }

    public Models.BacktestReport buildModelReportWithMode(
            DbStore store, List<Models.Draw> history, int window, String mode) {
        BacktestResult r = runWithMode(store, history, window, mode);

        Models.BacktestReport report = new Models.BacktestReport();

        report.ok = r.ok;
        report.mode = r.mode;
        report.type =
                AppConfig.BACKTEST_MODE_FULL.equals(r.mode)
                        ? "WALK_FORWARD_FULL_SAFE_LEARNING"
                        : "WALK_FORWARD_SPLIT_LEARNING";
        report.window = r.window;
        report.rangeStart = r.startIssue;
        report.rangeEnd = r.endIssue;
        report.startIssue = r.startIssue;
        report.endIssue = r.endIssue;
        report.trainCount = AppConfig.MIN_DRAWS_ANALYZE;
        report.totalCount = r.totalSteps;
        report.testCount = r.totalSteps;

        report.betCount = r.betCount;
        report.hitCount = r.hitCount;
        report.missCount = r.missCount;
        report.holdCount = r.holdCount;
        report.shadowCount = r.shadowCount;

        report.candidateCount = r.avgCandidateCount;
        report.enrichCount = r.avgEnrichCount;
        report.deepCount = r.avgDeepCount;

        report.hitRate = r.winRate;
        report.winRate = r.winRate;
        report.frequency = r.frequency;
        report.pnl = r.pnl;
        report.maxDrawdown = r.maxDrawdown;
        report.longestLose = r.longestLose;

        report.avgRawP = r.avgRawP;
        report.avgFinalP = r.avgFinalP;
        report.avgGate = r.avgGate;
        report.avgEdge = r.avgEdge;
        report.calibrationError = 0.0d;

        report.status = r.ok == 1 ? AppConfig.BACKTEST_STATUS_OK : AppConfig.BACKTEST_STATUS_ERROR;
        report.summary = r.summary;
        report.startedAt = r.startedAt;
        report.finishedAt = r.finishedAt;
        report.costMs = r.costMs;
        report.createdAt = r.createdAt > 0L ? r.createdAt : System.currentTimeMillis();
        report.progress = 100;

        report.manualTriggered = AppConfig.BACKTEST_MODE_FULL.equals(r.mode) ? 1 : 0;
        report.profileCount = r.learningCaseCount;
        report.attributionCount = r.learnedPositionCount;
        report.auditSummary = buildAuditSummary(r);
        report.topMissReasons = r.topMissReason;
        report.topHitReasons = "";
        report.learningImpactSummary = buildLearningImpactSummary(r);

        if (report.summary == null || report.summary.length() == 0) {
            report.summary = UiFormatter.backtestSummary(report);
        }

        if (store != null) {
            store.saveBacktestReport(report);
        }

        return report;
    }

    public void saveStandardReports(DbStore store, List<Models.Draw> history) {
        if (store == null) {
            return;
        }

        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean == null) {
            clean = new ArrayList<Models.Draw>();
        }

        if (clean.size() < AppConfig.MIN_DRAWS_ANALYZE + 2) {
            Models.BacktestReport report = new Models.BacktestReport();
            report.mode = AppConfig.BACKTEST_MODE_FAST;
            report.window = AppConfig.BACKTEST_MID_WINDOW;
            report.ok = 0;
            report.status = "NOT_ENOUGH";
            report.summary = "回测不足：有效历史少于" + (AppConfig.MIN_DRAWS_ANALYZE + 2) + "期";
            report.createdAt = System.currentTimeMillis();
            store.saveBacktestReport(report);
            return;
        }

        Models.BacktestReport r50 =
                buildModelReportWithMode(
                        store,
                        clean,
                        AppConfig.BACKTEST_MICRO_WINDOW,
                        AppConfig.BACKTEST_MODE_FAST);
        cooperativePause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        Models.BacktestReport r120 =
                buildModelReportWithMode(
                        store,
                        clean,
                        AppConfig.AUTO_VALIDATE_DEFAULT_WINDOW,
                        AppConfig.BACKTEST_MODE_FAST);
        r120.type = "REALTIME_VALIDATE";
        store.saveRealtimeValidationReport(r120);
        cooperativePause(AppConfig.BACKTEST_COOPERATIVE_SLEEP_FAST_MS);

        Models.BacktestReport r500 =
                buildModelReportWithMode(
                        store,
                        clean,
                        AppConfig.BACKTEST_BIG_WINDOW,
                        AppConfig.BACKTEST_MODE_NORMAL);
        r500.type = "AUTO_HEALTH_BACKTEST";

        store.setState("backtest_status", AppConfig.BACKTEST_STATUS_OK);
        store.setState("backtest_progress", "100");
        store.setState(
                "backtest_last_summary",
                "自动回测完成：W50/W120/W500；学习样本="
                        + (r50.profileCount + r120.profileCount + r500.profileCount)
                        + "；实时出号优先。");
    }

    public Models.BacktestReport saveFullReport(
            DbStore store, List<Models.Draw> history, int window) {
        if (store == null) {
            return null;
        }

        window = AppConfig.normalizeManualBacktestWindow(window);

        Models.BacktestReport report =
                buildModelReportWithMode(store, history, window, AppConfig.BACKTEST_MODE_FULL);
        report.type = "MANUAL_FULL_WALK_FORWARD_SAFE";
        report.manualTriggered = 1;

        store.saveTeacherSnapshotFromBacktest(report);
        store.setState("teacher_snapshot_status", "OK");
        store.setState(
                "teacher_snapshot_summary",
                "FULL安全回测已保存教师快照：窗口W"
                        + report.window
                        + "，测试步数="
                        + report.testCount
                        + "，学习样本="
                        + report.profileCount);

        return report;
    }

    public String explain(BacktestResult r) {
        if (r == null) {
            return "回测结果为空";
        }

        if (r.ok == 0) {
            return r.summary == null ? "回测失败" : r.summary;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("模式=");
        sb.append(r.mode);
        sb.append("，请求窗口=");
        sb.append(r.requestedWindow);
        sb.append("，覆盖窗口=");
        sb.append(r.actualWindow);
        sb.append("，实际步数=");
        sb.append(r.totalSteps);

        if (r.sampled == 1) {
            sb.append("，安全抽样=是，步长=");
            sb.append(r.sampleStride);
        }

        if (r.autoLearnSegment == 1) {
            sb.append("，后台学习段=");
            sb.append(r.autoLearnStartIndex);
            sb.append("-");
            sb.append(r.autoLearnEndIndex);
        }

        sb.append("，完整覆盖=");
        sb.append(r.completeWindow == 1 ? "是" : "否");
        sb.append("，BET=");
        sb.append(r.betCount);
        sb.append("，HOLD=");
        sb.append(r.holdCount);
        sb.append("，SHADOW=");
        sb.append(r.shadowCount);
        if (r.shadowCount > 0) {
            sb.append("，SHADOW反事实率=");
            sb.append(UiFormatter.probability(r.shadowHitRate));
        }
        if (r.mainChainFieldWrittenCount > 0) {
            sb.append("，主链字段=");
            sb.append(r.mainChainFieldWrittenCount);
        }
        sb.append("，频率=");
        sb.append(UiFormatter.probability(r.frequency));
        sb.append("，胜率=");
        sb.append(UiFormatter.probability(r.winRate));
        sb.append("，盈利=");
        sb.append(UiFormatter.money(r.pnl));
        sb.append("，最大回撤=");
        sb.append(UiFormatter.money(r.maxDrawdown));
        sb.append("，最长连错=");
        sb.append(r.longestLose);
        sb.append("，均FinalP=");
        sb.append(UiFormatter.probability(r.avgFinalP));
        sb.append("，均Gate=");
        sb.append(UiFormatter.probability(r.avgGate));
        sb.append("，均Edge=");
        sb.append(UiFormatter.signedProbability(r.avgEdge));
        sb.append("，候选均=");
        sb.append(r.avgCandidateCount);
        sb.append("，概率均=");
        sb.append(r.avgEnrichCount);
        sb.append("，深算均=");
        sb.append(r.avgDeepCount);
        sb.append("，学习位置样本=");
        sb.append(r.learningCaseCount);
        sb.append("，教师矩阵应用=");
        sb.append(r.teacherMatrixAppliedCount);
        sb.append("，教师强=");
        sb.append(r.teacherMatrixStrongCount);
        sb.append("，教师风险=");
        sb.append(r.teacherMatrixRiskCount);

        if (r.timedOut == 1) {
            sb.append("，自动轻量截断=是");
        }

        if (r.frequencyGuardHoldCount > 0) {
            sb.append("，频率门转HOLD=");
            sb.append(r.frequencyGuardHoldCount);
        }

        if (r.futureLeakBlockCount > 0) {
            sb.append("，防泄漏拦截=");
            sb.append(r.futureLeakBlockCount);
        }

        if (r.exceptionHoldCount > 0) {
            sb.append("，异常保护HOLD=");
            sb.append(r.exceptionHoldCount);
        }

        return sb.toString();
    }

    private StepResult runOneStep(
            DbStore store,
            List<Models.Draw> train,
            Models.Draw actualNext,
            List<Models.ShapeVector> recentVectorMemory,
            BacktestRuntime runtime,
            String modeValue,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        StepResult step = new StepResult();

        String mode = AppConfig.normalizeBacktestMode(modeValue);

        try {
            if (train == null || train.size() == 0 || actualNext == null || !actualNext.isValid()) {
                return step;
            }

            List<Models.Draw> safeTrain = sanitizeTrainBeforeTarget(train, actualNext);
            if (safeTrain == null || safeTrain.size() == 0) {
                step.futureLeakBlocked = true;
                step.decision = buildLeakHold(train, actualNext);
                return step;
            }
            if (safeTrain.size() != train.size()) {
                step.futureLeakBlocked = true;
            }
            train = safeTrain;

            if (hasFutureLeak(train, actualNext)) {
                step.futureLeakBlocked = true;
                step.decision = buildLeakHold(train, actualNext);
                return step;
            }

            List<Models.ShapeVector> vectors = windowFeatureEngine.buildAllPositions(train);
            vectors = shapeStateEngine.refineAll(vectors);
            vectors = markovEngine.applyToVectors(vectors, train);

            ArrayList<TrendStateMachine.TrendTrace> traces =
                    trendStateMachine.analyzeAll(recentVectorMemory, vectors);
            vectors = trendStateMachine.refineAllByTrace(recentVectorMemory, vectors);

            ArrayList<BreakRecoveryEngine.RecoveryResult> recoveries =
                    breakRecoveryEngine.analyzeAll(vectors, traces);

            Models.CandidateBatch batch = comboScannerEngine.scan(train, vectors);

            ArrayList<Models.ComboCandidate> scannedCandidates =
                    batch == null ? new ArrayList<Models.ComboCandidate>() : batch.candidates;

            step.candidateCount = scannedCandidates == null ? 0 : scannedCandidates.size();

            ArrayList<Models.ComboCandidate> probabilityCandidates =
                    capCandidates(scannedCandidates, getCandidateCap(mode));

            probabilityCandidates =
                    enrichBacktestCandidatesFullMainChain(
                            store, probabilityCandidates, train, traces, recoveries, mode);

            step.enrichCount = probabilityCandidates == null ? 0 : probabilityCandidates.size();
            step.deepCount = Math.min(step.enrichCount, BACKTEST_DECISION_CAP);

            probabilityCandidates = capCandidates(probabilityCandidates, BACKTEST_PROBABILITY_CAP);
            probabilityCandidates =
                    calibrationEngine.applyToCandidatesForBacktest(
                            store, probabilityCandidates, mode);
            probabilityCandidates =
                    dynamicGateEngine.applyToCandidatesForBacktest(
                            store, probabilityCandidates, traces, recoveries, train, mode);
            probabilityCandidates = capCandidates(probabilityCandidates, BACKTEST_PROBABILITY_CAP);

            TeacherApplyReport teacherApplyReport =
                    applyTeacherMatrixForBacktest(store, probabilityCandidates, train, mode);

            probabilityCandidates = teacherApplyReport.candidates;
            step.teacherMatrixApplied = teacherApplyReport.appliedCount;
            step.teacherMatrixStrong = teacherApplyReport.strongCount;
            step.teacherMatrixRisk = teacherApplyReport.riskCount;
            step.teacherMatrixFieldWritten = teacherApplyReport.fieldWrittenCount;
            step.teacherMatrixSummary = teacherApplyReport.summary;

            annotateBacktestMainChainCandidates(
                    probabilityCandidates, train, actualNext, mode, step);

            probabilityCandidates = capCandidates(probabilityCandidates, BACKTEST_PROBABILITY_CAP);

            ArrayList<Models.ComboCandidate> decisionCandidates =
                    capCandidates(probabilityCandidates, BACKTEST_DECISION_CAP);

            Models.CandidateBatch finalBatch = new Models.CandidateBatch();

            if (batch != null) {
                finalBatch.baseIssue = batch.baseIssue;
                finalBatch.targetIssue = batch.targetIssue;
                finalBatch.scanMode = batch.scanMode;
                finalBatch.fullScan = batch.fullScan;
                finalBatch.scannedCount = batch.scannedCount;
                finalBatch.keptCount = decisionCandidates == null ? 0 : decisionCandidates.size();
            }

            finalBatch.probabilityMode = mode;
            finalBatch.enrichCount = step.enrichCount;
            finalBatch.deepCount = step.deepCount;

            if (decisionCandidates != null) {
                for (int i = 0; i < decisionCandidates.size(); i++) {
                    finalBatch.add(decisionCandidates.get(i));
                }

                if (decisionCandidates.size() > 0) {
                    finalBatch.best = decisionCandidates.get(0);
                }
            }

            Models.Decision decision =
                    decisionGateEngine.decide(store, finalBatch, train, traces, recoveries);

            normalizeDecisionForBacktest(decision, train, actualNext);

            Models.Decision beforeFreqGuard = decision;

            decision = applySimulatedFrequencyGuard(decision, runtime);
            normalizeDecisionForBacktest(decision, train, actualNext);
            annotateBacktestDecision(decision, train, actualNext, step);

            if (beforeFreqGuard != null
                    && decision != null
                    && AppConfig.DECISION_BET.equals(beforeFreqGuard.decision)
                    && AppConfig.DECISION_HOLD.equals(decision.decision)) {
                step.frequencyTurnHold = true;
            }

            if (decision != null) {
                decision.backtestMode = mode;
                decision.probabilityMode = mode;
                decision.freq100 = runtime == null ? 0.0d : runtime.recentBetFrequency(100);
                decision.freq500 = runtime == null ? 0.0d : runtime.recentBetFrequency(500);
                decision.freq1000 = runtime == null ? 0.0d : runtime.recentBetFrequency(1000);
                decision.reasonSummary =
                        appendReason(
                                decision.reasonSummary,
                                buildDecisionBacktestAudit(decision, step.teacherMatrixSummary));

                if (decision.reasonSummary != null
                        && decision.reasonSummary.indexOf("1000期前") >= 0) {
                    step.earlySampleHold = true;
                }
            }

            step.decision = decision;
            step.currentVectors = vectors;

            if (decision != null && isRealBacktestBet(decision)) {
                int actualNumber = actualNext.getNumberAtPosition(decision.position);
                step.actualNumber = actualNumber;

                if (AppConfig.isValidNumber(actualNumber)) {
                    step.hit = strictDecisionContains(decision, actualNumber);

                    if (!step.hit) {
                        step.missReason = learningEngine.classifyMissReason(decision, actualNext);
                    }
                }
            } else if (decision != null && AppConfig.DECISION_SHADOW.equals(decision.decision)) {
                int actualNumber = actualNext.getNumberAtPosition(decision.position);
                step.shadowActualNumber = actualNumber;
                if (AppConfig.isValidNumber(actualNumber)) {
                    step.shadowWouldHit = strictDecisionContains(decision, actualNumber);
                }
            }

            step.learningCases =
                    buildLearningCasesForStep(
                            train,
                            actualNext,
                            probabilityCandidates,
                            vectors,
                            decision,
                            mode,
                            taskKey,
                            segmentKey,
                            targetIndex);

            for (int i = 0; i < step.learningCases.size(); i++) {
                Models.LearningCase x = step.learningCases.get(i);

                if (x == null) {
                    continue;
                }

                if (x.hit == 1) {
                    step.learningHitCount++;
                } else {
                    step.learningMissCount++;
                }
            }

            return step;
        } catch (Throwable e) {
            step.exceptionHold = true;
            step.decision = buildExceptionHold(train, actualNext, safeError(e));
            return step;
        }
    }

    private ArrayList<Models.ComboCandidate> enrichBacktestCandidatesFullMainChain(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> train,
            ArrayList<TrendStateMachine.TrendTrace> traces,
            ArrayList<BreakRecoveryEngine.RecoveryResult> recoveries,
            String mode) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (candidates == null || candidates.size() == 0) {
            return out;
        }

        try {
            Method m =
                    ProbabilityEngine.class.getMethod(
                            "enrichAllWithMode",
                            DbStore.class,
                            List.class,
                            List.class,
                            List.class,
                            List.class,
                            String.class,
                            int.class,
                            int.class,
                            boolean.class,
                            boolean.class);

            Object obj =
                    m.invoke(
                            probabilityEngine,
                            store,
                            candidates,
                            train,
                            traces,
                            recoveries,
                            mode,
                            Integer.valueOf(BACKTEST_PROBABILITY_CAP),
                            Integer.valueOf(BACKTEST_DECISION_CAP),
                            Boolean.valueOf(false),
                            Boolean.valueOf(false));

            if (obj instanceof List) {
                List list = (List) obj;

                for (int i = 0; i < list.size(); i++) {
                    Object x = list.get(i);

                    if (x instanceof Models.ComboCandidate) {
                        out.add((Models.ComboCandidate) x);
                    }
                }

                if (out.size() > 0) {
                    return capCandidates(out, BACKTEST_PROBABILITY_CAP);
                }
            }
        } catch (Throwable e) {
        }

        try {
            Method m2 =
                    ProbabilityEngine.class.getMethod(
                            "enrichAllForBacktest",
                            DbStore.class,
                            ArrayList.class,
                            List.class,
                            ArrayList.class,
                            ArrayList.class,
                            String.class);

            Object obj2 =
                    m2.invoke(
                            probabilityEngine, store, candidates, train, traces, recoveries, mode);

            if (obj2 instanceof List) {
                List list2 = (List) obj2;
                ArrayList<Models.ComboCandidate> tmp = new ArrayList<Models.ComboCandidate>();

                for (int i = 0; i < list2.size(); i++) {
                    Object x = list2.get(i);

                    if (x instanceof Models.ComboCandidate) {
                        tmp.add((Models.ComboCandidate) x);
                    }
                }

                return capCandidates(tmp, BACKTEST_PROBABILITY_CAP);
            }
        } catch (Throwable e2) {
        }

        return capCandidates(candidates, BACKTEST_PROBABILITY_CAP);
    }

    private void normalizeDecisionForBacktest(
            Models.Decision d, List<Models.Draw> train, Models.Draw target) {
        if (d == null) {
            return;
        }

        d.decision = normalizeDecisionText(d.decision);
        d.action = d.decision;

        if (train != null && train.size() > 0) {
            Models.Draw base = train.get(train.size() - 1);
            if (base != null && base.issue != null && base.issue.length() > 0) {
                d.baseIssue = base.issue;
            }
        }

        if (target != null && target.issue != null && target.issue.length() > 0) {
            d.targetIssue = target.issue;
        }

        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }

        if (AppConfig.DECISION_BET.equals(d.decision)) {
            if (!AppConfig.isValidPosition(d.position) || !hasStrictK3Numbers(d.n1, d.n2, d.n3)) {
                d.decision = AppConfig.DECISION_HOLD;
                d.action = AppConfig.DECISION_HOLD;
                d.blockCode = AppConfig.BLOCK_COMBO_WEAK;
                d.statScope = AppConfig.STAT_SCOPE_HOLD;
                d.reasonSummary = appendReason(d.reasonSummary, "回测保护：BET内容不是单位置K3，强制HOLD");
                return;
            }

            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
            d.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        } else if (AppConfig.DECISION_SHADOW.equals(d.decision)) {
            d.statScope = AppConfig.STAT_SCOPE_SHADOW;
        } else {
            d.decision = AppConfig.DECISION_HOLD;
            d.action = AppConfig.DECISION_HOLD;
            d.statScope = AppConfig.STAT_SCOPE_HOLD;
        }
    }

    private boolean isRealBacktestBet(Models.Decision d) {
        if (d == null) {
            return false;
        }

        if (!AppConfig.DECISION_BET.equals(d.decision)) {
            return false;
        }

        if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        if (!hasStrictK3Numbers(d.n1, d.n2, d.n3)) {
            return false;
        }

        d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);

        if (!Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        return true;
    }

    private String normalizeDecisionText(String decision) {
        if (decision == null) {
            return AppConfig.DECISION_HOLD;
        }

        String d = decision.trim();

        if (d.length() == 0) {
            return AppConfig.DECISION_HOLD;
        }

        if (AppConfig.DECISION_BET.equals(d)
                || "BET".equalsIgnoreCase(d)
                || "REAL_BET".equalsIgnoreCase(d)
                || "REAL_TRADE".equalsIgnoreCase(d)
                || "下注".equals(d)
                || "出手".equals(d)) {
            return AppConfig.DECISION_BET;
        }

        if (AppConfig.DECISION_SHADOW.equals(d)
                || "SHADOW".equalsIgnoreCase(d)
                || "SHADOW_BET".equalsIgnoreCase(d)
                || "影子".equals(d)
                || "影子观察".equals(d)
                || "观察".equals(d)) {
            return AppConfig.DECISION_SHADOW;
        }

        return AppConfig.DECISION_HOLD;
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

    private ArrayList<Models.LearningCase> buildLearningCasesForStep(
            List<Models.Draw> train,
            Models.Draw actualNext,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.ShapeVector> vectors,
            Models.Decision finalDecision,
            String mode,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        ArrayList<Models.LearningCase> list = new ArrayList<Models.LearningCase>();

        if (actualNext == null || !actualNext.isValid()) {
            return list;
        }

        String baseIssue = "";

        if (train != null && train.size() > 0) {
            baseIssue = safeIssue(train.get(train.size() - 1));
        }

        Models.ComboCandidate[] bestByPosition =
                new Models.ComboCandidate[AppConfig.POSITION_COUNT + 1];

        if (candidates != null) {
            for (int i = 0; i < candidates.size(); i++) {
                Models.ComboCandidate c = candidates.get(i);

                if (c == null || !AppConfig.isValidPosition(c.position)) {
                    continue;
                }

                Models.ComboCandidate old = bestByPosition[c.position];

                if (old == null || caseCandidateScore(c) > caseCandidateScore(old)) {
                    bestByPosition[c.position] = c;
                }
            }
        }

        Models.ShapeVector[] vectorByPosition =
                new Models.ShapeVector[AppConfig.POSITION_COUNT + 1];

        if (vectors != null) {
            for (int i = 0; i < vectors.size(); i++) {
                Models.ShapeVector v = vectors.get(i);

                if (v == null || !AppConfig.isValidPosition(v.position)) {
                    continue;
                }

                vectorByPosition[v.position] = v;
            }
        }

        for (int position = 1; position <= AppConfig.POSITION_COUNT; position++) {
            int actualNumber = actualNext.getNumberAtPosition(position);

            if (!AppConfig.isValidNumber(actualNumber)) {
                continue;
            }

            Models.ComboCandidate c = bestByPosition[position];
            Models.ShapeVector v = vectorByPosition[position];
            Models.LearningCase x;

            if (c != null && c.isValidK3()) {
                x =
                        buildLearningCaseFromCandidate(
                                c,
                                finalDecision,
                                baseIssue,
                                actualNext,
                                actualNumber,
                                mode,
                                taskKey,
                                segmentKey,
                                targetIndex);
            } else {
                x =
                        buildLearningCaseFromVector(
                                v,
                                baseIssue,
                                actualNext,
                                actualNumber,
                                position,
                                mode,
                                taskKey,
                                segmentKey,
                                targetIndex);
            }

            if (x != null) {
                list.add(x);
            }
        }

        return list;
    }

    private Models.LearningCase buildLearningCaseFromCandidate(
            Models.ComboCandidate c,
            Models.Decision finalDecision,
            String baseIssue,
            Models.Draw actualNext,
            int actualNumber,
            String mode,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        if (c == null || actualNext == null || !actualNext.isValid()) {
            return null;
        }

        Models.LearningCase x = new Models.LearningCase();

        boolean hit = strictCandidateContains(c, actualNumber);
        String stateKey =
                buildCaseStateKey(
                        c.position,
                        c.stateMode,
                        c.shapeKey,
                        c.driftRisk,
                        c.breakDepth,
                        c.recoveryScore,
                        c.finalP,
                        c.dynamicGate);
        String expectedShape = buildK3ShapeText(c.n1, c.n2, c.n3);
        String actualShape = buildActualShapeText(actualNumber, c.n1, c.n2, c.n3);
        String missReason = "";

        if (!hit) {
            Models.Decision d = candidateToDecisionForLearning(c, baseIssue, actualNext.issue);
            missReason = learningEngine.classifyMissReason(d, actualNext);
        }

        x.taskKey = taskKey == null ? "" : taskKey;
        x.segmentKey = segmentKey == null ? "" : segmentKey;
        x.baseIssue = baseIssue == null ? "" : baseIssue;
        x.targetIssue = actualNext.issue == null ? "" : actualNext.issue;
        x.createdAt = System.currentTimeMillis();
        x.targetIndex = targetIndex;
        x.position = c.position;
        x.actualNumber = actualNumber;
        x.predictedK3 = Models.buildK3Mask(c.n1, c.n2, c.n3);
        x.k3Mask = x.predictedK3;
        x.hit = hit ? 1 : 0;

        x.stateKey = stateKey;
        x.stateMode = c.stateMode == null ? "" : c.stateMode;
        x.trendKey = bucketText("TREND", c.trendLife);
        x.shapeKey = c.shapeKey == null ? "" : c.shapeKey;
        x.driftKey = bucketText("DRIFT", c.driftRisk);
        x.breakKey = bucketText("BREAK", c.breakDepth);
        x.recoveryKey = bucketText("RECOVERY", c.recoveryScore);
        x.transitionKey = bucketText("TRANS", c.deepTransitionScore);

        x.expectedShape = expectedShape;
        x.actualShape = actualShape;
        x.whyExpectedHit = buildWhyExpectedHitText(c, hit);
        x.whyActualHit = buildWhyActualText(c, actualNumber, hit);
        x.whyMiss = hit ? "" : buildWhyMissText(c, actualNumber, missReason);
        x.nextAdvice = buildNextAdviceText(c, hit, missReason, actualNumber);
        x.mainReasonCode = hit ? "CASE_HIT" : missReason;
        x.affectedModule = chooseAffectedModule(c, hit, missReason);

        x.finalP = c.finalP;
        x.dynamicGate = c.dynamicGate;
        x.edge = c.edge;
        x.trendScore = c.trendLife;
        x.shapeScore = c.shapeP;
        x.driftRisk = c.driftRisk;
        x.breakRisk = c.breakDepth;
        x.recoveryScore = c.recoveryScore;
        x.profileScore = c.profileScore;
        x.numberBias = c.biasScore;
        x.comboBias = c.comboP;
        x.riskScore =
                Math.max(c.evidenceRisk, Math.max(c.driftRisk / 100.0d, c.breakDepth / 100.0d));
        x.teacherWeight = c.aiJudgeConfidence > 0.0d ? c.aiJudgeConfidence : 1.0d;
        x.stateConfidence = c.deepConfidence;
        x.stateSample = c.effectiveSample;
        x.cacheSample = c.comboSample;
        x.recentSample = c.deepProfileSample;
        x.recentHitRate = c.deepProfileHitRate;
        putDoubleField(
                x,
                "teacherMatrixScore",
                getDoubleField(
                        c, "teacherMatrixScore", getDoubleField(c, "teacherNumberScore", 0.0d)));
        putDoubleField(
                x,
                "teacherMatrixHitRate",
                getDoubleField(
                        c,
                        "teacherMatrixHitRate",
                        getDoubleField(c, "teacherNumberHitRate", 0.0d)));
        putDoubleField(
                x,
                "teacherMatrixConfidence",
                getDoubleField(
                        c,
                        "teacherMatrixConfidence",
                        getDoubleField(c, "teacherNumberConfidence", 0.0d)));
        putIntField(
                x,
                "teacherMatrixSample",
                getIntField(c, "teacherMatrixSample", getIntField(c, "teacherNumberSample", 0)));
        putStringField(
                x,
                "teacherMatrixStatus",
                getStringField(
                        c, "teacherMatrixStatus", getStringField(c, "teacherNumberStatus", "")));
        putStringField(
                x,
                "teacherMatrixSummary",
                getStringField(
                        c, "teacherMatrixSummary", getStringField(c, "teacherNumberSummary", "")));
        putStringField(x, "v20WalkForwardAudit", buildLearningCaseAudit(c, hit));
        putStringField(x, "walkForwardVersion", WALK_FORWARD_MAIN_CHAIN_VERSION);
        putStringField(x, "walkForwardNoFutureLeakVersion", WALK_FORWARD_NO_FUTURE_LEAK_VERSION);
        putStringField(
                x,
                "walkForwardStatScope",
                isFinalRealBetForPosition(finalDecision, c.position)
                        ? AppConfig.STAT_SCOPE_REAL_TRADE
                        : AppConfig.STAT_SCOPE_SHADOW);
        putStringField(
                x,
                "finalDecisionRejectCode",
                getStringField(c, "finalDecisionRejectCode", getStringField(c, "rejectCode", "")));
        putDoubleField(
                x,
                "finalDecisionQualityScore",
                getDoubleField(
                        c,
                        "finalDecisionQualityScore",
                        getDoubleField(c, "mainChainEvidenceScore", 0.0d)));
        putDoubleField(
                x,
                "finalDecisionRiskScore",
                getDoubleField(
                        c,
                        "finalDecisionRiskScore",
                        getDoubleField(c, "mainChainCounterScore", 0.0d)));

        if (finalDecision != null
                && AppConfig.isValidPosition(finalDecision.position)
                && finalDecision.position == c.position
                && AppConfig.DECISION_BET.equals(finalDecision.decision)
                && AppConfig.STAT_SCOPE_REAL_TRADE.equals(finalDecision.statScope)) {
            x.affectedModule = "REAL_BET_DECISION";
        }

        return x;
    }

    private Models.LearningCase buildLearningCaseFromVector(
            Models.ShapeVector v,
            String baseIssue,
            Models.Draw actualNext,
            int actualNumber,
            int position,
            String mode,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        if (actualNext == null || !actualNext.isValid() || !AppConfig.isValidPosition(position)) {
            return null;
        }

        int k3Mask = 0;
        int n1 = 0;
        int n2 = 0;
        int n3 = 0;

        if (v != null) {
            int[] top = top3FromVector(v);
            n1 = top[0];
            n2 = top[1];
            n3 = top[2];

            if (hasStrictK3Numbers(n1, n2, n3)) {
                k3Mask = Models.buildK3Mask(n1, n2, n3);
            }
        }

        boolean hit = Models.isValidK3Mask(k3Mask) && strictContains(n1, n2, n3, actualNumber);
        String stateMode = v == null ? AppConfig.MODE_UNKNOWN : safeString(v.stateMode);
        String shapeKey = v == null ? "" : safeString(v.shapeKey);
        double driftRisk = v == null ? 0.0d : v.driftRisk;
        double breakDepth = v == null ? 0.0d : v.breakDepth;
        double recoveryScore = v == null ? 0.0d : v.recoveryScore;
        double trendLife = v == null ? 0.0d : v.trendLife;

        Models.LearningCase x = new Models.LearningCase();

        x.taskKey = taskKey == null ? "" : taskKey;
        x.segmentKey = segmentKey == null ? "" : segmentKey;
        x.baseIssue = baseIssue == null ? "" : baseIssue;
        x.targetIssue = actualNext.issue == null ? "" : actualNext.issue;
        x.createdAt = System.currentTimeMillis();
        x.targetIndex = targetIndex;
        x.position = position;
        x.actualNumber = actualNumber;
        x.predictedK3 = k3Mask;
        x.k3Mask = k3Mask;
        x.hit = hit ? 1 : 0;

        x.stateKey =
                buildCaseStateKey(
                        position,
                        stateMode,
                        shapeKey,
                        driftRisk,
                        breakDepth,
                        recoveryScore,
                        0.0d,
                        0.0d);
        x.stateMode = stateMode;
        x.trendKey = bucketText("TREND", trendLife);
        x.shapeKey = shapeKey;
        x.driftKey = bucketText("DRIFT", driftRisk);
        x.breakKey = bucketText("BREAK", breakDepth);
        x.recoveryKey = bucketText("RECOVERY", recoveryScore);
        x.transitionKey = v == null ? "" : bucketText("TRANS", v.transitionScore);

        x.expectedShape = Models.isValidK3Mask(k3Mask) ? buildK3ShapeText(n1, n2, n3) : "无有效K3候选";
        x.actualShape = buildActualShapeText(actualNumber, n1, n2, n3);
        x.whyExpectedHit =
                Models.isValidK3Mask(k3Mask) ? "窗口向量给出该位置Top3号码层，作为非投注观察样本" : "该位置候选不足，仅记录实际状态";
        x.whyActualHit = hit ? "实际号码落入向量Top3，增强该状态号码画像" : "实际号码未落入向量Top3，记录为状态偏差样本";
        x.whyMiss = hit ? "" : "向量观察偏差：Top3未覆盖实际号码";
        x.nextAdvice = hit ? "同状态下可保留该位置号码层观察" : "同状态下需要调整该位置号码层和区间覆盖";
        x.mainReasonCode = hit ? "VECTOR_OBSERVE_HIT" : "VECTOR_OBSERVE_MISS";
        x.affectedModule = "VECTOR_PROFILE";

        x.finalP = 0.0d;
        x.dynamicGate = 0.0d;
        x.edge = 0.0d;
        x.trendScore = trendLife;
        x.shapeScore = v == null ? 0.0d : v.deepScore;
        x.driftRisk = driftRisk;
        x.breakRisk = breakDepth;
        x.recoveryScore = recoveryScore;
        x.profileScore = v == null ? 0.0d : v.profileScore;
        x.numberBias = v == null ? 0.0d : v.biasScore;
        x.comboBias = 0.0d;
        x.riskScore = Math.max(driftRisk / 100.0d, breakDepth / 100.0d);
        x.teacherWeight = 1.0d;
        x.stateConfidence = v == null ? 0.0d : v.deepScore;
        x.stateSample = 0;
        x.cacheSample = 0;
        x.recentSample = 0;
        x.recentHitRate = 0.0d;

        return x;
    }

    private Models.Decision candidateToDecisionForLearning(
            Models.ComboCandidate c, String baseIssue, String targetIssue) {
        Models.Decision d = new Models.Decision();

        if (c == null) {
            return d;
        }

        d.baseIssue = baseIssue == null ? "" : baseIssue;
        d.targetIssue = targetIssue == null ? "" : targetIssue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_SHADOW;
        d.action = AppConfig.DECISION_SHADOW;
        d.statScope = AppConfig.STAT_SCOPE_SHADOW;

        d.position = c.position;
        d.setNumbers(c.n1, c.n2, c.n3);
        d.k3Mask = Models.buildK3Mask(c.n1, c.n2, c.n3);

        d.rawP = c.rawP;
        d.calibratedP = c.calibratedP;
        d.finalP = c.finalP;
        d.dynamicGate = c.dynamicGate;
        d.edge = c.edge;

        d.modelP = c.modelP;
        d.shapeP = c.shapeP;
        d.comboP = c.comboP;
        d.recoveryP = c.recoveryP;
        d.similarP = c.similarP;
        d.memoryP = c.memoryP;

        d.similarHitRate = c.similarHitRate;
        d.comboHitRate = c.comboHitRate;
        d.effectiveSample = c.effectiveSample;
        d.comboSample = c.comboSample;

        d.trendLife = c.trendLife;
        d.driftRisk = c.driftRisk;
        d.breakDepth = c.breakDepth;
        d.recoveryScore = c.recoveryScore;
        d.championGap = c.championGap;
        d.comboGap = c.comboGap;
        d.hotReverseRisk = c.hotReverseRisk;
        d.falseStrongRisk = c.falseStrongRisk;

        d.calibrationError = c.calibrationError;
        d.stateMode = c.stateMode;
        d.shapeKey = c.shapeKey;
        d.reasonSummary = c.reasonSummary;

        d.deepScore = c.deepScore;
        d.deepConfidence = c.deepConfidence;
        d.deepPositionScore = c.deepPositionScore;
        d.deepNumberScore = c.deepNumberScore;
        d.deepCycleScore = c.deepCycleScore;
        d.deepTransitionScore = c.deepTransitionScore;
        d.deepStateScore = c.deepStateScore;
        d.biasScore = c.biasScore;
        d.profileScore = c.profileScore;
        d.auditPenalty = c.auditPenalty;
        d.auditBoost = c.auditBoost;
        d.aiJudgeScore = c.aiJudgeScore;
        d.aiJudgeConfidence = c.aiJudgeConfidence;
        d.evidenceScore = c.evidenceScore;
        d.evidenceRisk = c.evidenceRisk;

        return d;
    }

    private int[] top3FromVector(Models.ShapeVector v) {
        int[] top = new int[] {0, 0, 0};
        double[] score = new double[] {-1.0d, -1.0d, -1.0d};

        if (v == null) {
            return top;
        }

        for (int n = 1; n <= 10; n++) {
            double p = v.getNumberProbability(n);
            insertTop(top, score, n, p);
        }

        return top;
    }

    private void insertTop(int[] top, double[] score, int n, double p) {
        if (top == null || score == null || top.length < 3 || score.length < 3) {
            return;
        }

        if (p > score[0]) {
            top[2] = top[1];
            score[2] = score[1];
            top[1] = top[0];
            score[1] = score[0];
            top[0] = n;
            score[0] = p;
        } else if (p > score[1]) {
            top[2] = top[1];
            score[2] = score[1];
            top[1] = n;
            score[1] = p;
        } else if (p > score[2]) {
            top[2] = n;
            score[2] = p;
        }
    }

    private double caseCandidateScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = 0.0d;

        score += c.finalP * 100.0d * 0.35d;
        score += c.edge * 100.0d * 0.20d;
        score += c.profileScore * 100.0d * 0.10d;
        score += c.biasScore * 100.0d * 0.08d;
        score += c.deepScore * 100.0d * 0.08d;
        score += c.evidenceScore * 100.0d * 0.06d;
        score -= c.evidenceRisk * 100.0d * 0.08d;

        if (c.blockCode != null && c.blockCode.length() > 0) {
            score -= 8.0d;
        }

        return score;
    }

    private String buildCaseStateKey(
            int position,
            String stateMode,
            String shapeKey,
            double driftRisk,
            double breakDepth,
            double recoveryScore,
            double finalP,
            double gate) {

        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(position);
        sb.append("|M=");
        sb.append(
                cleanKeyPart(
                        stateMode == null || stateMode.length() == 0
                                ? AppConfig.MODE_UNKNOWN
                                : stateMode));
        sb.append("|S=");
        sb.append(cleanKeyPart(shapeKey));
        sb.append("|");
        sb.append(bucketText("DRIFT", driftRisk));
        sb.append("|");
        sb.append(bucketText("BREAK", breakDepth));
        sb.append("|");
        sb.append(bucketText("RECOVERY", recoveryScore));
        sb.append("|");
        sb.append(bucketProb("FP", finalP));
        sb.append("|");
        sb.append(bucketProb("GATE", gate));

        return sb.toString();
    }

    private String buildK3ShapeText(int n1, int n2, int n3) {
        int[] nums = new int[] {n1, n2, n3};
        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;
        int min = 99;
        int max = 0;

        for (int i = 0; i < nums.length; i++) {
            int n = nums[i];

            if (!AppConfig.isValidNumber(n)) {
                continue;
            }

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }

            if (n < min) {
                min = n;
            }

            if (n > max) {
                max = n;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("K3=");
        sb.append(formatNumber(n1));
        sb.append("/");
        sb.append(formatNumber(n2));
        sb.append("/");
        sb.append(formatNumber(n3));
        sb.append("｜低");
        sb.append(low);
        sb.append("/中");
        sb.append(mid);
        sb.append("/高");
        sb.append(high);
        sb.append("｜奇");
        sb.append(odd);
        sb.append("/偶");
        sb.append(even);

        if (max > 0 && min <= max) {
            sb.append("｜跨度");
            sb.append(max - min);
        }

        return sb.toString();
    }

    private String buildActualShapeText(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return "实际号码非法";
        }

        StringBuilder sb = new StringBuilder();

        sb.append(formatNumber(actual));
        sb.append("=");
        sb.append(zoneText(actual));
        sb.append("/");
        sb.append(actual % 2 == 0 ? "偶" : "奇");

        if (actual == n1 || actual == n2 || actual == n3) {
            sb.append("｜落入K3");
        } else {
            int dist = nearestDistance(actual, n1, n2, n3);

            if (dist == 1) {
                sb.append("｜K3邻号");
            } else if (dist == 2) {
                sb.append("｜K3近层");
            } else {
                sb.append("｜K3外层");
            }

            if (!coversZone(actual, n1, n2, n3)) {
                sb.append("｜区间未覆盖");
            }
        }

        return sb.toString();
    }

    private String buildWhyExpectedHitText(Models.ComboCandidate c, boolean hit) {
        if (c == null) {
            return "";
        }

        if (hit) {
            return "预测K3命中，说明该状态下候选层、位置层、号码层存在有效偏差";
        }

        if (c.finalP >= c.dynamicGate && c.finalP >= AppConfig.MIN_REAL_BET_PROBABILITY) {
            return "模型认为FinalP越过门槛，但实际未覆盖，需反查号码层和风险层";
        }

        return "候选用于SHADOW/HOLD观察，记录该状态下K3覆盖偏差";
    }

    private String buildWhyActualText(Models.ComboCandidate c, int actual, boolean hit) {
        if (c == null || !AppConfig.isValidNumber(actual)) {
            return "";
        }

        if (hit) {
            if (c.driftRisk <= AppConfig.DRIFT_RISK_MAX
                    && c.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
                return "漂移和断点未破坏主趋势，实际号码进入K3覆盖";
            }

            if (c.recoveryScore >= AppConfig.RECOVERY_SCORE_MIN) {
                return "恢复链有效，实际号码回到候选层";
            }

            return "实际号码在该状态下落入模型候选层";
        }

        if (nearestDistance(actual, c.n1, c.n2, c.n3) == 1) {
            return "实际号码贴近K3邻号，方向接近但号码层级差一格";
        }

        if (!coversZone(actual, c.n1, c.n2, c.n3)) {
            return "实际号码落在未覆盖区间，本期区间层接管";
        }

        if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
            return "漂移风险偏高，实际号码脱离原热层";
        }

        if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
            return "断点风险偏高，实际号码像破位后新层";
        }

        return "实际号码来自K3外层，属于短周期层级切换";
    }

    private String buildWhyMissText(Models.ComboCandidate c, int actual, String missReason) {
        StringBuilder sb = new StringBuilder();

        if (missReason != null && missReason.length() > 0) {
            sb.append(UiFormatter.missReasonText(missReason));
        }

        if (c != null) {
            if (nearestDistance(actual, c.n1, c.n2, c.n3) == 1) {
                appendPart(sb, "K3邻号未纳入");
            }

            if (!coversZone(actual, c.n1, c.n2, c.n3)) {
                appendPart(sb, "实际区间未覆盖");
            }

            if (c.comboGap < AppConfig.COMBO_GAP_MIN) {
                appendPart(sb, "组合差距弱");
            }

            if (c.championGap < AppConfig.CHAMPION_GAP_MIN) {
                appendPart(sb, "位置优势弱");
            }

            if (c.driftRisk >= AppConfig.DRIFT_RISK_MAX) {
                appendPart(sb, "漂移偏高");
            }

            if (c.breakDepth >= AppConfig.BREAK_DEPTH_MAX) {
                appendPart(sb, "断点偏高");
            }
        }

        if (sb.length() == 0) {
            sb.append("未发现单一强错因，归入状态噪声或短周期切换");
        }

        return sb.toString();
    }

    private String buildNextAdviceText(
            Models.ComboCandidate c, boolean hit, String missReason, int actual) {
        if (c == null) {
            return "";
        }

        if (hit) {
            if (c.finalP >= AppConfig.TARGET_HIT_RATE_IDEAL && c.evidenceRisk <= 0.520d) {
                return "同状态可强化该画像，但不盲目扩频";
            }

            return "命中但优势未极强，只强化状态记忆和号码画像";
        }

        if (missReason != null && missReason.length() > 0) {
            if (AppConfig.MISS_K3_RANK_4_6.equals(missReason)) {
                return "同状态优先修正第三号码层，增加邻号和4-6层候选权重";
            }

            if (AppConfig.MISS_DRIFT_SPIKE.equals(missReason)) {
                return "漂移高时提高门槛，降低旧热号权重";
            }

            if (AppConfig.MISS_TREND_BROKEN.equals(missReason)) {
                return "断点深时转HOLD，等待恢复确认";
            }

            if (AppConfig.MISS_SAMPLE_WEAK.equals(missReason)) {
                return "样本弱时只进SHADOW，不进真实BET";
            }
        }

        if (!coversZone(actual, c.n1, c.n2, c.n3)) {
            return "同状态要求K3覆盖实际高发区间，否则转SHADOW";
        }

        return "同状态收紧真实BET，先积累SHADOW证据";
    }

    private String chooseAffectedModule(Models.ComboCandidate c, boolean hit, String missReason) {
        if (c == null) {
            return "UNKNOWN";
        }

        if (hit) {
            if (c.profileScore >= 0.620d) {
                return "RUNTIME_PROFILE";
            }

            if (c.biasScore >= 0.620d) {
                return "NUMBER_PROFILE";
            }

            return "COMBO_SCORE";
        }

        if (AppConfig.MISS_DRIFT_SPIKE.equals(missReason)) {
            return "DRIFT_RISK";
        }

        if (AppConfig.MISS_TREND_BROKEN.equals(missReason)) {
            return "BREAK_RISK";
        }

        if (AppConfig.MISS_SAMPLE_WEAK.equals(missReason)) {
            return "SAMPLE_WEAK";
        }

        if (c.evidenceRisk >= 0.620d) {
            return "EVIDENCE_RISK";
        }

        return "NUMBER_LAYER";
    }

    private List<Models.Draw> sanitizeTrainBeforeTarget(
            List<Models.Draw> train, Models.Draw target) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (train == null || target == null || target.issue == null || target.issue.length() == 0) {
            return out;
        }

        String targetIssue = target.issue;

        for (int i = 0; i < train.size(); i++) {
            Models.Draw d = train.get(i);

            if (d == null || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            if (compareIssue(d.issue, targetIssue) < 0) {
                out.add(d);
            }
        }

        return out;
    }

    private void annotateBacktestMainChainCandidates(
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> train,
            Models.Draw actualNext,
            String mode,
            StepResult step) {
        if (candidates == null || candidates.size() == 0) {
            return;
        }

        String baseIssue = "";
        String targetIssue = actualNext == null || actualNext.issue == null ? "" : actualNext.issue;

        if (train != null && train.size() > 0) {
            baseIssue = safeIssue(train.get(train.size() - 1));
        }

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            putStringField(c, "walkForwardVersion", WALK_FORWARD_MAIN_CHAIN_VERSION);
            putStringField(c, "walkForwardClosedLoopVersion", WALK_FORWARD_CLOSED_LOOP_VERSION);
            putStringField(
                    c, "walkForwardNoFutureLeakVersion", WALK_FORWARD_NO_FUTURE_LEAK_VERSION);
            putStringField(c, "walkForwardMode", safeString(mode));
            putStringField(c, "walkForwardBaseIssue", baseIssue);
            putStringField(c, "walkForwardTargetIssue", targetIssue);
            putIntField(c, "walkForwardNoFutureLeakFlag", 1);
            putStringField(
                    c,
                    "walkForwardStatPolicy",
                    "REAL_BET_ONLY_COUNTS_PNL_SHADOW_IS_COUNTERFACTUAL");
            putStringField(c, "fieldChainVersion", WALK_FORWARD_MAIN_CHAIN_VERSION);
            putStringField(c, "walkForwardMainChainSummary", buildWalkForwardCandidateAudit(c));

            if (step != null) {
                step.mainChainFieldWritten++;
                if (getStringField(
                                        c,
                                        "finalDecisionRejectCode",
                                        getStringField(c, "rejectCode", ""))
                                .length()
                        > 0) {
                    step.finalDecisionRejectCount++;
                }
                if (isTeacherNoSampleNeutralText(
                        getStringField(
                                c,
                                "teacherMatrixStatus",
                                getStringField(c, "teacherStatus", "")))) {
                    step.noSampleNeutralCount++;
                }
                if (isV20WatchNeutralText(
                        getStringField(
                                c,
                                "v20Status",
                                getStringField(
                                        c,
                                        "dynamicStatus",
                                        getStringField(c, "dynamicCauseStatus", ""))))) {
                    step.v20WatchNeutralCount++;
                }
                if (isAdaptiveWatchNeutralText(
                        getStringField(
                                c, "adaptiveStatus", getStringField(c, "adaptiveAction", "")))) {
                    step.adaptiveWatchNeutralCount++;
                }
            }
        }
    }

    private void annotateBacktestDecision(
            Models.Decision d, List<Models.Draw> train, Models.Draw actualNext, StepResult step) {
        if (d == null) {
            return;
        }

        String baseIssue = "";
        String targetIssue = actualNext == null || actualNext.issue == null ? "" : actualNext.issue;

        if (train != null && train.size() > 0) {
            baseIssue = safeIssue(train.get(train.size() - 1));
        }

        putStringField(d, "walkForwardVersion", WALK_FORWARD_MAIN_CHAIN_VERSION);
        putStringField(d, "walkForwardClosedLoopVersion", WALK_FORWARD_CLOSED_LOOP_VERSION);
        putStringField(d, "walkForwardNoFutureLeakVersion", WALK_FORWARD_NO_FUTURE_LEAK_VERSION);
        putStringField(d, "walkForwardBaseIssue", baseIssue);
        putStringField(d, "walkForwardTargetIssue", targetIssue);
        putIntField(d, "walkForwardNoFutureLeakFlag", 1);
        putStringField(d, "fieldChainVersion", WALK_FORWARD_MAIN_CHAIN_VERSION);
        putStringField(
                d,
                "walkForwardStatPolicy",
                AppConfig.DECISION_BET.equals(d.decision)
                        ? "REAL_TRADE_ONLY"
                        : (AppConfig.DECISION_SHADOW.equals(d.decision)
                                ? "SHADOW_COUNTERFACTUAL_ONLY"
                                : "HOLD_NO_REAL_STATS"));

        String reject =
                getStringField(
                        d,
                        "finalDecisionRejectCode",
                        getStringField(d, "rejectCode", d.blockCode == null ? "" : d.blockCode));
        if (reject.length() > 0) {
            putStringField(d, "walkForwardRejectCode", reject);
        }

        d.reasonSummary =
                appendReason(
                        d.reasonSummary,
                        "WalkForward严格前推：base="
                                + baseIssue
                                + "，target="
                                + targetIssue
                                + "，训练集只允许 target 之前开奖；版本="
                                + WALK_FORWARD_MAIN_CHAIN_VERSION
                                + "；BET才计真实胜率/盈亏，SHADOW只做反事实，HOLD不计真实统计");

        if (step != null) {
            if (reject.length() > 0) {
                step.finalDecisionRejectCount++;
            }
            if (isTeacherNoSampleNeutralText(
                    getStringField(
                            d, "teacherMatrixStatus", getStringField(d, "teacherStatus", "")))) {
                step.noSampleNeutralCount++;
            }
            if (isV20WatchNeutralText(
                    getStringField(
                            d,
                            "v20Status",
                            getStringField(
                                    d,
                                    "dynamicStatus",
                                    getStringField(d, "dynamicCauseStatus", ""))))) {
                step.v20WatchNeutralCount++;
            }
            if (isAdaptiveWatchNeutralText(
                    getStringField(d, "adaptiveStatus", getStringField(d, "adaptiveAction", "")))) {
                step.adaptiveWatchNeutralCount++;
            }
        }
    }

    private String buildWalkForwardCandidateAudit(Models.ComboCandidate c) {
        if (c == null) {
            return "WalkForward候选为空";
        }

        String reject =
                getStringField(c, "finalDecisionRejectCode", getStringField(c, "rejectCode", ""));
        String teacher =
                getStringField(c, "teacherMatrixStatus", getStringField(c, "teacherStatus", ""));
        String v20 =
                getStringField(
                        c,
                        "v20Status",
                        getStringField(
                                c, "dynamicStatus", getStringField(c, "dynamicCauseStatus", "")));
        String adaptive =
                getStringField(c, "adaptiveStatus", getStringField(c, "adaptiveAction", ""));

        return "WalkForward主链审计：FinalP="
                + UiFormatter.probability(c.finalP)
                + "，Gate="
                + UiFormatter.probability(c.dynamicGate)
                + "，Edge="
                + UiFormatter.signedProbability(c.edge)
                + "，Teacher="
                + (teacher.length() == 0 ? "NEUTRAL" : teacher)
                + "，V20="
                + (v20.length() == 0 ? "WATCH" : v20)
                + "，Adaptive="
                + (adaptive.length() == 0 ? "WATCH" : adaptive)
                + "，Reject="
                + (reject.length() == 0 ? "NONE" : reject)
                + "；仅供前推验证与学习样本，不创建BET";
    }

    private boolean isFinalRealBetForPosition(Models.Decision d, int position) {
        return d != null
                && AppConfig.DECISION_BET.equals(d.decision)
                && AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)
                && d.position == position;
    }

    private boolean isTeacherNoSampleNeutralText(String s) {
        String t = upperSafe(s);
        return t.length() == 0
                || t.indexOf("NO_SAMPLE") >= 0
                || t.indexOf("NONE") >= 0
                || t.indexOf("EMPTY") >= 0
                || t.indexOf("UNKNOWN") >= 0
                || t.indexOf("NEUTRAL") >= 0;
    }

    private boolean isV20WatchNeutralText(String s) {
        String t = upperSafe(s);
        return t.length() == 0
                || t.indexOf("WATCH") >= 0
                || t.indexOf("PENDING") >= 0
                || t.indexOf("UNKNOWN") >= 0
                || t.indexOf("NEUTRAL") >= 0;
    }

    private boolean isAdaptiveWatchNeutralText(String s) {
        String t = upperSafe(s);
        return t.length() == 0
                || t.indexOf("WATCH") >= 0
                || t.indexOf("PENDING") >= 0
                || t.indexOf("NEUTRAL") >= 0;
    }

    private String upperSafe(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
    }

    private TeacherApplyReport applyTeacherMatrixForBacktest(
            DbStore store,
            ArrayList<Models.ComboCandidate> candidates,
            List<Models.Draw> train,
            String mode) {
        TeacherApplyReport report = new TeacherApplyReport();
        report.candidates = capCandidates(candidates, BACKTEST_PROBABILITY_CAP);

        if (report.candidates == null) {
            report.candidates = new ArrayList<Models.ComboCandidate>();
        }

        if (report.candidates.size() == 0) {
            report.summary = "教师矩阵跳过：候选为空";
            return report;
        }

        String baseIssue = "";
        String targetIssue = "";

        if (train != null && train.size() > 0) {
            Models.Draw base = train.get(train.size() - 1);
            baseIssue = safeIssue(base);
            targetIssue = buildNextIssue(baseIssue);
        }

        try {
            Method m =
                    TeacherNumberMatrixEngine.class.getMethod(
                            "apply",
                            DbStore.class,
                            List.class,
                            List.class,
                            List.class,
                            List.class,
                            List.class,
                            String.class,
                            String.class);

            Object obj =
                    m.invoke(
                            teacherNumberMatrixEngine,
                            new Object[] {
                                store,
                                report.candidates,
                                train,
                                null,
                                null,
                                null,
                                baseIssue,
                                targetIssue
                            });

            if (obj instanceof List) {
                report.candidates = castComboCandidateList((List) obj);
            }
        } catch (Throwable e) {
            try {
                Method m2 =
                        TeacherNumberMatrixEngine.class.getMethod(
                                "enrich",
                                DbStore.class,
                                List.class,
                                List.class,
                                List.class,
                                List.class);

                Object obj2 =
                        m2.invoke(
                                teacherNumberMatrixEngine,
                                new Object[] {store, report.candidates, train, null, null});

                if (obj2 instanceof List) {
                    report.candidates = castComboCandidateList((List) obj2);
                }
            } catch (Throwable ignored) {
                report.summary = "教师矩阵回测桥接失败：" + safeError(e);
            }
        }

        if (report.candidates == null) {
            report.candidates = new ArrayList<Models.ComboCandidate>();
        }

        for (int i = 0; i < report.candidates.size(); i++) {
            Models.ComboCandidate c = report.candidates.get(i);

            if (c == null) {
                continue;
            }

            int applied =
                    getIntField(
                            c, "teacherMatrixApplied", getIntField(c, "teacherNumberApplied", 0));
            int sample =
                    getIntField(c, "teacherMatrixSample", getIntField(c, "teacherNumberSample", 0));
            double score =
                    getDoubleField(
                            c, "teacherMatrixScore", getDoubleField(c, "teacherNumberScore", 0.0d));
            double risk =
                    getDoubleField(
                            c, "teacherMatrixRisk", getDoubleField(c, "teacherNumberRisk", 0.0d));
            double conf =
                    getDoubleField(
                            c,
                            "teacherMatrixConfidence",
                            getDoubleField(c, "teacherNumberConfidence", 0.0d));
            String status =
                    getStringField(
                            c, "teacherMatrixStatus", getStringField(c, "teacherNumberStatus", ""));
            String summary =
                    getStringField(
                            c,
                            "teacherMatrixSummary",
                            getStringField(c, "teacherNumberSummary", ""));

            if (applied == 1 || sample > 0 || score > 0.0d || summary.length() > 0) {
                report.appliedCount++;
            }

            if (summary.length() > 0) {
                report.fieldWrittenCount++;
            }

            if ("STRONG".equals(status)
                    || (sample >= V20_TEACHER_MIN_SAMPLE
                            && score >= V20_TEACHER_STRONG_SCORE
                            && risk < V20_TEACHER_RISK_SCORE)) {
                report.strongCount++;
            }

            if ("RISK".equals(status) || risk >= V20_TEACHER_RISK_SCORE) {
                report.riskCount++;
            }

            applyTeacherSortAuditToCandidate(c, score, risk, conf, sample, status, summary, mode);

            if (report.summary.length() == 0 && summary.length() > 0) {
                report.summary = shortText(summary, 180);
            }
        }

        rankCandidatesByV20Teacher(report.candidates);

        if (report.summary.length() == 0) {
            report.summary =
                    "教师矩阵回测桥接：候选="
                            + report.candidates.size()
                            + "｜应用="
                            + report.appliedCount
                            + "｜强="
                            + report.strongCount
                            + "｜风险="
                            + report.riskCount
                            + "｜说明=只反哺候选排序/解释，不创建BET";
        }

        return report;
    }

    private ArrayList<Models.ComboCandidate> castComboCandidateList(List raw) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (raw == null || raw.size() == 0) {
            return out;
        }

        for (int i = 0; i < raw.size(); i++) {
            Object o = raw.get(i);

            if (o instanceof Models.ComboCandidate) {
                out.add((Models.ComboCandidate) o);
            }
        }

        return out;
    }

    private void applyTeacherSortAuditToCandidate(
            Models.ComboCandidate c,
            double score,
            double risk,
            double confidence,
            int sample,
            String status,
            String summary,
            String mode) {
        if (c == null) {
            return;
        }

        double oldCause = getDoubleField(c, "causeScore", 0.0d);
        double oldRisk = getDoubleField(c, "riskScore", 0.0d);
        double causeAdd = score * V20_TEACHER_MAX_SORT_WEIGHT * (0.50d + confidence * 0.50d);
        double riskAdd = risk * 0.018d * (0.60d + (1.0d - confidence) * 0.40d);

        putDoubleField(c, "causeScore", clampProbability(oldCause + causeAdd));
        putDoubleField(c, "riskScore", clampProbability(Math.max(oldRisk, riskAdd)));
        putStringField(
                c,
                "walkForwardTeacherAudit",
                "mode="
                        + safeString(mode)
                        + "｜sample="
                        + sample
                        + "｜score="
                        + UiFormatter.probability(score)
                        + "｜risk="
                        + UiFormatter.probability(risk)
                        + "｜conf="
                        + UiFormatter.probability(confidence)
                        + "｜status="
                        + safeString(status)
                        + "｜只排序解释不放行BET");

        if (summary != null && summary.length() > 0) {
            String oldReason = getStringField(c, "reasonSummary", "");
            if (oldReason.indexOf("WalkForward教师审计") < 0) {
                putStringField(
                        c,
                        "reasonSummary",
                        appendReason(oldReason, "WalkForward教师审计：" + shortText(summary, 120)));
            }
        }
    }

    private void rankCandidatesByV20Teacher(ArrayList<Models.ComboCandidate> list) {
        if (list == null || list.size() <= 1) {
            return;
        }

        for (int i = 0; i < list.size() - 1; i++) {
            int best = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (v20BacktestCandidateScore(list.get(j))
                        > v20BacktestCandidateScore(list.get(best))) {
                    best = j;
                }
            }
            if (best != i) {
                Models.ComboCandidate tmp = list.get(i);
                list.set(i, list.get(best));
                list.set(best, tmp);
            }
        }

        for (int k = 0; k < list.size(); k++) {
            Models.ComboCandidate c = list.get(k);
            if (c != null) {
                putIntField(c, "rank", k + 1);
                putIntField(c, "rankNo", k + 1);
            }
        }
    }

    private double v20BacktestCandidateScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999999.0d;
        }

        double finalP = getDoubleField(c, "finalP", 0.0d);
        double edge = getDoubleField(c, "edge", 0.0d);
        double rawP = getDoubleField(c, "rawP", finalP);
        double cause = getDoubleField(c, "causeScore", 0.0d);
        double risk = getDoubleField(c, "riskScore", 0.0d);
        double teacher =
                getDoubleField(
                        c, "teacherMatrixScore", getDoubleField(c, "teacherNumberScore", 0.0d));
        double teacherRisk =
                getDoubleField(
                        c, "teacherMatrixRisk", getDoubleField(c, "teacherNumberRisk", 0.0d));
        double teacherConf =
                getDoubleField(
                        c,
                        "teacherMatrixConfidence",
                        getDoubleField(c, "teacherNumberConfidence", 0.0d));
        int sample =
                getIntField(c, "teacherMatrixSample", getIntField(c, "teacherNumberSample", 0));
        double sampleConf = sample <= 0 ? 0.0d : Math.min(1.0d, ((double) sample) / 120.0d);

        return finalP * 1.000d
                + rawP * 0.080d
                + edge * 0.320d
                + cause * 0.100d
                + teacher
                        * V20_TEACHER_MAX_SORT_WEIGHT
                        * (0.50d + teacherConf * 0.30d + sampleConf * 0.20d)
                - risk * 0.055d
                - teacherRisk * 0.025d;
    }

    private String buildDecisionBacktestAudit(Models.Decision d, String teacherSummary) {
        if (d == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(V20_AUDIT_PREFIX);
        sb.append("：base=");
        sb.append(safeString(d.baseIssue));
        sb.append("｜target=");
        sb.append(safeString(d.targetIssue));
        sb.append("｜action=");
        sb.append(safeString(d.decision));
        sb.append("｜scope=");
        sb.append(safeString(d.statScope));
        sb.append("｜位置=");
        sb.append(d.position);
        sb.append("｜K3=");
        sb.append(formatNumber(d.n1));
        sb.append("/");
        sb.append(formatNumber(d.n2));
        sb.append("/");
        sb.append(formatNumber(d.n3));
        sb.append("｜FinalP=");
        sb.append(UiFormatter.probability(d.finalP));
        sb.append("｜Gate=");
        sb.append(UiFormatter.probability(d.dynamicGate));
        sb.append("｜Edge=");
        sb.append(UiFormatter.signedProbability(d.edge));
        sb.append("｜freq100=");
        sb.append(UiFormatter.probability(d.freq100));
        sb.append("｜说明=只用目标期之前训练窗口；真实胜率/盈亏只计最终BET");

        if (teacherSummary != null && teacherSummary.length() > 0) {
            sb.append("｜教师矩阵=");
            sb.append(shortText(teacherSummary, 120));
        }

        return sb.toString();
    }

    private String buildLearningCaseAudit(Models.ComboCandidate c, boolean hit) {
        if (c == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("V20学习样本审计：结果=");
        sb.append(hit ? "HIT" : "MISS");
        sb.append("｜教师样本=");
        sb.append(getIntField(c, "teacherMatrixSample", getIntField(c, "teacherNumberSample", 0)));
        sb.append("｜教师分=");
        sb.append(
                UiFormatter.probability(
                        getDoubleField(
                                c,
                                "teacherMatrixScore",
                                getDoubleField(c, "teacherNumberScore", 0.0d))));
        sb.append("｜教师风险=");
        sb.append(
                UiFormatter.probability(
                        getDoubleField(
                                c,
                                "teacherMatrixRisk",
                                getDoubleField(c, "teacherNumberRisk", 0.0d))));
        sb.append("｜状态键=");
        sb.append(
                cleanKeyPart(
                        buildCaseStateKey(
                                c.position,
                                c.stateMode,
                                c.shapeKey,
                                c.driftRisk,
                                c.breakDepth,
                                c.recoveryScore,
                                c.finalP,
                                c.dynamicGate)));
        sb.append("｜说明=学习样本只反哺画像，不写真实盈亏");
        return sb.toString();
    }

    private String buildNextIssue(String issue) {
        if (issue == null || issue.length() == 0) {
            return "";
        }

        String s = issue.trim();
        int end = s.length() - 1;

        while (end >= 0) {
            char ch = s.charAt(end);

            if (ch < '0' || ch > '9') {
                break;
            }

            end--;
        }

        String prefix = s.substring(0, end + 1);
        String digits = s.substring(end + 1);

        if (digits.length() == 0) {
            return s + "_NEXT";
        }

        try {
            long value = Long.parseLong(digits);
            long next = value + 1L;
            String nextText = String.valueOf(next);

            while (nextText.length() < digits.length()) {
                nextText = "0" + nextText;
            }

            return prefix + nextText;
        } catch (Throwable e) {
            return "";
        }
    }

    private ArrayList<Models.ComboCandidate> capCandidates(
            ArrayList<Models.ComboCandidate> source, int cap) {
        ArrayList<Models.ComboCandidate> out = new ArrayList<Models.ComboCandidate>();

        if (source == null || source.size() == 0) {
            return out;
        }

        if (cap <= 0) {
            cap = BACKTEST_PROBABILITY_CAP;
        }

        int max = Math.min(source.size(), cap);

        for (int i = 0; i < max; i++) {
            Models.ComboCandidate c = source.get(i);

            if (c != null) {
                out.add(c);
            }
        }

        return out;
    }

    private Models.Decision applySimulatedFrequencyGuard(
            Models.Decision decision, BacktestRuntime runtime) {
        if (decision == null) {
            return null;
        }

        decision.decision = normalizeDecisionText(decision.decision);
        decision.action = decision.decision;

        double freq100 = 0.0d;
        double freq500 = 0.0d;
        double freq1000 = 0.0d;

        if (runtime != null) {
            freq100 = runtime.recentBetFrequency(100);
            freq500 = runtime.recentBetFrequency(500);
            freq1000 = runtime.recentBetFrequency(1000);
        }

        decision.freq100 = freq100;
        decision.freq500 = freq500;
        decision.freq1000 = freq1000;

        if (!AppConfig.DECISION_BET.equals(decision.decision)) {
            return decision;
        }

        if (!AppConfig.isValidPosition(decision.position)
                || !hasStrictK3Numbers(decision.n1, decision.n2, decision.n3)) {
            return turnBetToHold(
                    decision, AppConfig.BLOCK_COMBO_WEAK, "老版回测频率门：BET内容不是严格单位置K3，转HOLD");
        }

        double finalP = decision.finalP;
        double dynamicGate = decision.dynamicGate;

        if (finalP <= 0.0d) {
            if (decision.calibratedP > 0.0d) {
                finalP = decision.calibratedP;
            } else if (decision.rawP > 0.0d) {
                finalP = decision.rawP;
            }
        }

        if (dynamicGate <= 0.0d) {
            dynamicGate = AppConfig.DEFAULT_DYNAMIC_GATE;
        }

        boolean strong = isStrongDecision(decision);

        if (decision.evidenceScore >= AppConfig.EVIDENCE_SCORE_STRONG
                && decision.evidenceRisk <= AppConfig.EVIDENCE_RISK_MAX
                && decision.aiJudgeScore >= AppConfig.AI_JUDGE_SCORE_STRONG
                && decision.aiJudgeConfidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN) {
            strong = true;
        }

        if (decision.deepScore >= AppConfig.DEEP_SCORE_STRONG
                && decision.deepConfidence >= AppConfig.AI_JUDGE_CONFIDENCE_MIN
                && decision.driftRisk <= AppConfig.DRIFT_RISK_MAX
                && decision.breakDepth <= AppConfig.BREAK_DEPTH_MAX) {
            strong = true;
        }

        if (finalP < SIM_MIN_REAL_P) {
            return turnBetToHold(
                    decision,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "老版回测频率门：FinalP低于最低真实出手线 "
                            + UiFormatter.probability(finalP)
                            + " < "
                            + UiFormatter.probability(SIM_MIN_REAL_P)
                            + "，BET转HOLD");
        }

        if (freq100 >= SIM_FREQ_HARD_HIGH) {
            return turnBetToHold(
                    decision,
                    AppConfig.BLOCK_FREQ_HIGH,
                    "老版回测频率门：近100期模拟出手频率达到硬上限 " + UiFormatter.probability(freq100) + "，BET转HOLD");
        }

        if (freq100 >= SIM_FREQ_HIGH) {
            if (!strong) {
                return turnBetToHold(
                        decision,
                        AppConfig.BLOCK_FREQ_HIGH,
                        "老版回测频率门：近100期频率偏高 "
                                + UiFormatter.probability(freq100)
                                + "，且不是强证据，BET转HOLD");
            }

            if (finalP < SIM_HIGH_FREQ_STRONG_P) {
                return turnBetToHold(
                        decision,
                        AppConfig.BLOCK_FREQ_HIGH,
                        "老版回测频率门：高频状态下强信号FinalP不足 "
                                + UiFormatter.probability(finalP)
                                + " < "
                                + UiFormatter.probability(SIM_HIGH_FREQ_STRONG_P)
                                + "，BET转HOLD");
            }
        }

        if (!strong && finalP < SIM_NON_STRONG_MIN_P) {
            return turnBetToHold(
                    decision,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "老版回测频率门：非强证据FinalP不足 "
                            + UiFormatter.probability(finalP)
                            + " < "
                            + UiFormatter.probability(SIM_NON_STRONG_MIN_P)
                            + "，BET转HOLD");
        }

        if (!strong && finalP < dynamicGate + 0.005d) {
            return turnBetToHold(
                    decision,
                    AppConfig.BLOCK_LOW_PROBABILITY,
                    "老版回测频率门：普通信号仅贴近动态门槛，保护转HOLD，FinalP="
                            + UiFormatter.probability(finalP)
                            + " Gate="
                            + UiFormatter.probability(dynamicGate));
        }

        if (freq100 >= AppConfig.SMART_FREQ_OVERHEAT) {
            if (finalP < dynamicGate + SIM_HARD_FREQ_EXTRA_EDGE) {
                return turnBetToHold(
                        decision,
                        AppConfig.BLOCK_FREQ_HIGH,
                        "老版回测频率门：频率过热，FinalP没有超过动态门槛足够边际，BET转HOLD");
            }
        }

        if (decision.driftRisk > AppConfig.DRIFT_RISK_HARD_BLOCK
                || decision.breakDepth > AppConfig.BREAK_DEPTH_HARD_BLOCK
                || decision.evidenceRisk > AppConfig.EVIDENCE_RISK_HARD) {
            return turnBetToHold(decision, AppConfig.BLOCK_BACKTEST_WEAK, "老版回测频率门：风险层过高，BET转HOLD");
        }

        decision.statScope = AppConfig.STAT_SCOPE_REAL_TRADE;
        decision.reasonSummary =
                appendReason(
                        decision.reasonSummary,
                        "老版回测频率门通过：freq100="
                                + UiFormatter.probability(freq100)
                                + "，FinalP="
                                + UiFormatter.probability(finalP)
                                + "，Gate="
                                + UiFormatter.probability(dynamicGate)
                                + "，strong="
                                + (strong ? "Y" : "N"));

        return decision;
    }

    private boolean isStrongDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        if (isBadState(d.stateMode)) {
            return false;
        }

        if (d.trendLife < AppConfig.TREND_LIFE_MIN) {
            return false;
        }

        if (d.driftRisk > AppConfig.DRIFT_RISK_MAX) {
            return false;
        }

        if (d.breakDepth > AppConfig.BREAK_DEPTH_MAX) {
            return false;
        }

        if (d.recoveryScore < AppConfig.RECOVERY_SCORE_MIN) {
            return false;
        }

        return true;
    }

    private Models.Decision turnBetToHold(Models.Decision source, String blockCode, String reason) {
        if (source == null) {
            return null;
        }

        Models.Decision d = source.copy();

        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.blockCode = blockCode == null ? AppConfig.BLOCK_LOW_PROBABILITY : blockCode;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;

        if (source.reasonSummary == null || source.reasonSummary.length() == 0) {
            d.reasonSummary = reason == null ? "回测频率门转HOLD" : reason;
        } else {
            d.reasonSummary = source.reasonSummary + "；" + (reason == null ? "回测频率门转HOLD" : reason);
        }

        return d;
    }

    private boolean hasFutureLeak(List<Models.Draw> train, Models.Draw target) {
        if (train == null || target == null || target.issue == null || target.issue.length() == 0) {
            return false;
        }

        String targetIssue = target.issue;

        for (int i = 0; i < train.size(); i++) {
            Models.Draw d = train.get(i);

            if (d == null || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            if (isSameIssue(d.issue, targetIssue)) {
                return true;
            }

            if (compareIssue(d.issue, targetIssue) > 0) {
                return true;
            }
        }

        return false;
    }

    private Models.Decision buildLeakHold(List<Models.Draw> train, Models.Draw target) {
        Models.Decision d = new Models.Decision();

        String baseIssue = "";

        if (train != null && train.size() > 0) {
            Models.Draw last = train.get(train.size() - 1);

            if (last != null && last.issue != null) {
                baseIssue = last.issue;
            }
        }

        d.baseIssue = baseIssue;
        d.targetIssue = target == null || target.issue == null ? "" : target.issue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.position = 0;
        d.setNumbers(0, 0, 0);
        d.k3Mask = 0;
        d.rawP = 0.0d;
        d.finalP = 0.0d;
        d.dynamicGate = AppConfig.HIGH_ACCURACY_GATE;
        d.edge = -1.0d;
        d.blockCode = AppConfig.BLOCK_DATA_ERROR;
        d.reasonSummary = "回测防泄漏：训练窗口混入目标期或未来期，强制HOLD";
        d.statScope = AppConfig.STAT_SCOPE_HOLD;

        return d;
    }

    private Models.Decision buildExceptionHold(
            List<Models.Draw> train, Models.Draw target, String error) {
        Models.Decision d = new Models.Decision();

        String baseIssue = "";

        if (train != null && train.size() > 0) {
            Models.Draw last = train.get(train.size() - 1);

            if (last != null && last.issue != null) {
                baseIssue = last.issue;
            }
        }

        d.baseIssue = baseIssue;
        d.targetIssue = target == null || target.issue == null ? "" : target.issue;
        d.createdAt = System.currentTimeMillis();
        d.decision = AppConfig.DECISION_HOLD;
        d.action = AppConfig.DECISION_HOLD;
        d.position = 0;
        d.setNumbers(0, 0, 0);
        d.k3Mask = 0;
        d.rawP = 0.0d;
        d.finalP = 0.0d;
        d.dynamicGate = AppConfig.HIGH_ACCURACY_GATE;
        d.edge = -1.0d;
        d.blockCode = AppConfig.BLOCK_DATA_ERROR;
        d.reasonSummary = "回测单步异常保护HOLD：" + error;
        d.statScope = AppConfig.STAT_SCOPE_HOLD;

        return d;
    }

    private ArrayList<Models.Draw> sliceTail(
            ArrayList<Models.Draw> source, int endExclusive, int lookback) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (source == null || source.size() == 0) {
            return out;
        }

        if (endExclusive > source.size()) {
            endExclusive = source.size();
        }

        if (endExclusive <= 0) {
            return out;
        }

        if (lookback <= 0) {
            lookback = FAST_LOOKBACK_CAP;
        }

        int start = endExclusive - lookback;

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < endExclusive; i++) {
            Models.Draw d = source.get(i);

            if (d != null && d.isValid()) {
                out.add(d);
            }
        }

        return out;
    }

    private void appendVectorMemory(
            ArrayList<Models.ShapeVector> memory, List<Models.ShapeVector> currentVectors) {
        if (memory == null || currentVectors == null) {
            return;
        }

        for (int i = 0; i < currentVectors.size(); i++) {
            Models.ShapeVector v = currentVectors.get(i);

            if (v != null) {
                memory.add(v.copyLight());
            }
        }

        int max = AppConfig.POSITION_COUNT * VECTOR_MEMORY_MAX_ROUNDS;

        while (memory.size() > max) {
            memory.remove(0);
        }
    }

    private void finishResult(BacktestResult r) {
        if (r == null) {
            return;
        }

        if (r.totalSteps > 0) {
            r.frequency = ((double) r.betCount) / ((double) r.totalSteps);
            r.avgCandidateCount = r.sumCandidateCount / r.totalSteps;
            r.avgEnrichCount = r.sumEnrichCount / r.totalSteps;
            r.avgDeepCount = r.sumDeepCount / r.totalSteps;
        } else {
            r.frequency = 0.0d;
            r.avgCandidateCount = 0;
            r.avgEnrichCount = 0;
            r.avgDeepCount = 0;
        }

        if (r.betCount > 0) {
            r.winRate = ((double) r.hitCount) / ((double) r.betCount);
            r.avgRawP = r.sumRawP / ((double) r.betCount);
            r.avgFinalP = r.sumFinalP / ((double) r.betCount);
            r.avgGate = r.sumGate / ((double) r.betCount);
            r.avgEdge = r.sumEdge / ((double) r.betCount);
        } else {
            r.winRate = 0.0d;
            r.avgRawP = 0.0d;
            r.avgFinalP = 0.0d;
            r.avgGate = 0.0d;
            r.avgEdge = 0.0d;
        }

        if (r.shadowCount > 0) {
            r.shadowHitRate = ((double) r.shadowHitCount) / ((double) r.shadowCount);
        } else {
            r.shadowHitRate = 0.0d;
        }

        r.frequency = AppConfig.clampProbability(r.frequency);
        r.winRate = AppConfig.clampProbability(r.winRate);
        r.shadowHitRate = AppConfig.clampProbability(r.shadowHitRate);
        r.avgRawP = AppConfig.clampProbability(r.avgRawP);
        r.avgFinalP = AppConfig.clampProbability(r.avgFinalP);
        r.avgGate = AppConfig.clampProbability(r.avgGate);

        r.topMissReason = mostCommon(r.missReasons);
        r.topHoldReason = mostCommon(r.holdReasons);
    }

    private String buildSummary(BacktestResult r) {
        if (r == null) {
            return "回测结果为空";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("WalkForward ");
        sb.append(r.mode);
        sb.append(" W");
        sb.append(r.window);
        sb.append("：覆盖=");
        sb.append(r.actualWindow);
        sb.append("，实际计算=");
        sb.append(r.totalSteps);

        if (r.autoLearnSegment == 1) {
            sb.append("，后台段=");
            sb.append(r.autoLearnStartIndex);
            sb.append("-");
            sb.append(r.autoLearnEndIndex);
        }

        if (AppConfig.BACKTEST_MODE_FULL.equals(r.mode)) {
            sb.append("，FULL安全抽样=");
            sb.append(r.sampled == 1 ? "是" : "否");

            if (r.sampled == 1) {
                sb.append("，步长=");
                sb.append(r.sampleStride);
            }

            sb.append("，完整覆盖=");
            sb.append(r.completeWindow == 1 ? "是" : "否");
        } else if (r.requestedWindow > 0
                && r.actualWindow > 0
                && r.actualWindow < r.requestedWindow) {
            sb.append("/");
            sb.append(r.requestedWindow);
            sb.append("轻量抽样");
        }

        sb.append("，BET=");
        sb.append(r.betCount);
        sb.append("/");
        sb.append(r.totalSteps);
        sb.append("，HOLD=");
        sb.append(r.holdCount);
        sb.append("，SHADOW=");
        sb.append(r.shadowCount);
        if (r.shadowCount > 0) {
            sb.append("，SHADOW反事实率=");
            sb.append(UiFormatter.probability(r.shadowHitRate));
        }
        if (r.mainChainFieldWrittenCount > 0) {
            sb.append("，主链字段=");
            sb.append(r.mainChainFieldWrittenCount);
        }
        sb.append("，频率=");
        sb.append(UiFormatter.probability(r.frequency));
        sb.append("，胜率=");
        sb.append(UiFormatter.probability(r.winRate));
        sb.append("，盈利=");
        sb.append(UiFormatter.money(r.pnl));
        sb.append("，最大回撤=");
        sb.append(UiFormatter.money(r.maxDrawdown));
        sb.append("，最长连错=");
        sb.append(r.longestLose);
        sb.append("，候选均=");
        sb.append(r.avgCandidateCount);
        sb.append("，概率均=");
        sb.append(r.avgEnrichCount);
        sb.append("，深算均=");
        sb.append(r.avgDeepCount);
        sb.append("，学习样本=");
        sb.append(r.learningCaseCount);
        sb.append("，教师矩阵应用=");
        sb.append(r.teacherMatrixAppliedCount);
        sb.append("，教师强=");
        sb.append(r.teacherMatrixStrongCount);
        sb.append("，教师风险=");
        sb.append(r.teacherMatrixRiskCount);

        if (r.timedOut == 1) {
            sb.append("，轻量截断=是");
        }

        if (r.frequencyGuardHoldCount > 0) {
            sb.append("，频率门HOLD=");
            sb.append(r.frequencyGuardHoldCount);
        }

        if (r.earlySampleHoldCount > 0) {
            sb.append("，早期样本HOLD=");
            sb.append(r.earlySampleHoldCount);
        }

        if (r.futureLeakBlockCount > 0) {
            sb.append("，防泄漏拦截=");
            sb.append(r.futureLeakBlockCount);
        }

        if (r.exceptionHoldCount > 0) {
            sb.append("，异常保护HOLD=");
            sb.append(r.exceptionHoldCount);
        }

        if (r.topMissReason != null && r.topMissReason.length() > 0) {
            sb.append("，主要错因=");
            sb.append(r.topMissReason);
        }

        if (r.topHoldReason != null && r.topHoldReason.length() > 0) {
            sb.append("，主要HOLD=");
            sb.append(r.topHoldReason);
        }

        if (r.betCount == 0) {
            sb.append("，结论=近期无真实BET，策略偏保守");
        } else if (r.winRate >= 0.40d
                && r.frequency >= AppConfig.TARGET_FREQ_MIN
                && r.frequency <= AppConfig.TARGET_FREQ_HARD_MAX
                && r.pnl > 0.0d) {
            sb.append("，结论=近期前推表现达标");
        } else if (r.winRate >= 0.37d && r.pnl >= 0.0d) {
            sb.append("，结论=胜率接近目标，继续观察频率");
        } else if (r.frequency < AppConfig.TARGET_FREQ_MIN) {
            sb.append("，结论=频率偏低，应检查门槛或候选质量");
        } else if (r.frequency > AppConfig.TARGET_FREQ_HARD_MAX) {
            sb.append("，结论=频率偏高，老版频率门已参与BET转HOLD保护");
        } else {
            sb.append("，结论=近期前推偏弱，应检查候选质量和教师矩阵反哺");
        }

        return sb.toString();
    }

    private String buildAuditSummary(BacktestResult r) {
        if (r == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("回测审计：只使用目标期之前历史，不写真实交易，不污染真实胜率");

        if (AppConfig.BACKTEST_MODE_FULL.equals(r.mode)) {
            sb.append("｜FULL小段/安全抽样，避免手机暴力逐期卡死");

            if (r.sampled == 1) {
                sb.append("｜抽样步长=");
                sb.append(r.sampleStride);
                sb.append("｜覆盖窗口=");
                sb.append(r.actualWindow);
                sb.append("｜实际计算=");
                sb.append(r.totalSteps);
            }
        } else {
            sb.append("｜自动轻量回测允许限时返回");
        }

        if (r.completeWindow == 1) {
            sb.append("｜窗口完整覆盖");
        } else {
            sb.append("｜实际窗口=");
            sb.append(r.actualWindow);
            sb.append("/");
            sb.append(r.requestedWindow);
        }

        sb.append("｜学习样本=");
        sb.append(r.learningCaseCount);
        sb.append("｜教师矩阵候选应用=");
        sb.append(r.teacherMatrixAppliedCount);
        sb.append("｜教师强=");
        sb.append(r.teacherMatrixStrongCount);
        sb.append("｜教师风险=");
        sb.append(r.teacherMatrixRiskCount);
        sb.append("｜候选链路=120候选学习/40候选裁决");
        sb.append("｜频率门按老版保护参与BET转HOLD");
        sb.append("｜真实统计只计DecisionGateEngine最终BET，HOLD/SHADOW/教师样本不计真实盈亏");
        sb.append("｜版本=");
        sb.append(WALK_FORWARD_MAIN_CHAIN_VERSION);
        sb.append("｜主链字段=");
        sb.append(r.mainChainFieldWrittenCount);
        sb.append("｜拒绝码=");
        sb.append(r.finalDecisionRejectCount);
        sb.append("｜NO_SAMPLE中性=");
        sb.append(r.noSampleNeutralCount);
        sb.append("｜V20_WATCH中性=");
        sb.append(r.v20WatchNeutralCount);
        sb.append("｜Adaptive_WATCH中性=");
        sb.append(r.adaptiveWatchNeutralCount);
        if (r.shadowCount > 0) {
            sb.append("｜SHADOW反事实=");
            sb.append(r.shadowHitCount);
            sb.append("/");
            sb.append(r.shadowCount);
            sb.append("=");
            sb.append(UiFormatter.probability(r.shadowHitRate));
        }

        if (r.timedOut == 1) {
            sb.append("｜轻量截断");
        }

        if (r.exceptionHoldCount > 0) {
            sb.append("｜异常保护步数=");
            sb.append(r.exceptionHoldCount);
        }

        if (r.futureLeakBlockCount > 0) {
            sb.append("｜防未来泄漏=");
            sb.append(r.futureLeakBlockCount);
        }

        return sb.toString();
    }

    private String buildLearningImpactSummary(BacktestResult r) {
        if (r == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("回测学习输入：mode=");
        sb.append(r.mode);
        sb.append("，freq=");
        sb.append(UiFormatter.probability(r.frequency));
        sb.append("，hr=");
        sb.append(UiFormatter.probability(r.winRate));
        sb.append("，pnl=");
        sb.append(UiFormatter.money(r.pnl));
        sb.append("，位置样本=");
        sb.append(r.learningCaseCount);
        sb.append("，样本命中=");
        sb.append(r.learnedHitCount);
        sb.append("，样本偏差=");
        sb.append(r.learnedMissCount);
        sb.append("，候选链路=120学习/40裁决");
        sb.append("，教师矩阵应用=");
        sb.append(r.teacherMatrixAppliedCount);
        sb.append("，教师强=");
        sb.append(r.teacherMatrixStrongCount);
        sb.append("，教师风险=");
        sb.append(r.teacherMatrixRiskCount);

        if (r.sampled == 1) {
            sb.append("，FULL安全抽样覆盖=");
            sb.append(r.actualWindow);
            sb.append("期，实际计算=");
            sb.append(r.totalSteps);
            sb.append("步");
        }

        if (r.topHoldReason != null && r.topHoldReason.length() > 0) {
            sb.append("，主要HOLD=");
            sb.append(r.topHoldReason);
        }

        if (r.topMissReason != null && r.topMissReason.length() > 0) {
            sb.append("，主要错因=");
            sb.append(r.topMissReason);
        }

        if (r.timeoutReason != null && r.timeoutReason.length() > 0) {
            sb.append("，备注=");
            sb.append(r.timeoutReason);
        }

        return sb.toString();
    }

    private BacktestResult failResult(BacktestResult r, String summary) {
        if (r == null) {
            r = new BacktestResult();
        }

        r.ok = 0;
        r.finishedAt = System.currentTimeMillis();

        if (r.startedAt > 0L) {
            r.costMs = r.finishedAt - r.startedAt;
        } else {
            r.costMs = 0L;
        }

        r.summary = summary == null ? "回测失败" : summary;

        return r;
    }

    private int getCandidateCap(String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            return FULL_CANDIDATE_CAP;
        }

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return NORMAL_CANDIDATE_CAP;
        }

        return FAST_CANDIDATE_CAP;
    }

    private int getMaxStepsForLightMode(String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return NORMAL_MAX_STEPS;
        }

        return FAST_MAX_STEPS;
    }

    private int getTrainLookback(String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        int configLookback = AppConfig.getBacktestTrainLookback(mode);

        if (configLookback <= 0) {
            configLookback = FAST_LOOKBACK_CAP;
        }

        if (AppConfig.BACKTEST_MODE_FULL.equals(mode)) {
            return Math.min(configLookback, FULL_LOOKBACK_CAP);
        }

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return Math.min(configLookback, NORMAL_LOOKBACK_CAP);
        }

        return Math.min(configLookback, FAST_LOOKBACK_CAP);
    }

    private long getLightTimeBudgetMs(String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return NORMAL_TIME_BUDGET_MS;
        }

        return FAST_TIME_BUDGET_MS;
    }

    private int getMinStepsBeforeTimeout(String mode) {
        mode = AppConfig.normalizeBacktestMode(mode);

        if (AppConfig.BACKTEST_MODE_NORMAL.equals(mode)) {
            return 16;
        }

        return 10;
    }

    private void cooperativePause(long sleepMs) {
        try {
            Thread.yield();

            if (sleepMs < 1L) {
                sleepMs = 1L;
            }

            Thread.sleep(sleepMs);
        } catch (Throwable e) {
        }
    }

    private boolean isBadState(String mode) {
        if (mode == null) {
            return true;
        }

        return AppConfig.MODE_BROKEN.equals(mode)
                || AppConfig.MODE_REVERSING.equals(mode)
                || "REVERSAL".equals(mode);
    }

    private boolean hasStrictK3Numbers(int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(n1)
                || !AppConfig.isValidNumber(n2)
                || !AppConfig.isValidNumber(n3)) {
            return false;
        }

        if (n1 == n2 || n1 == n3 || n2 == n3) {
            return false;
        }

        return true;
    }

    private boolean strictContains(int n1, int n2, int n3, int actualNumber) {
        if (!AppConfig.isValidNumber(actualNumber)) {
            return false;
        }

        return actualNumber == n1 || actualNumber == n2 || actualNumber == n3;
    }

    private boolean strictCandidateContains(Models.ComboCandidate c, int actualNumber) {
        if (c == null) {
            return false;
        }

        return strictContains(c.n1, c.n2, c.n3, actualNumber);
    }

    private boolean strictDecisionContains(Models.Decision d, int actualNumber) {
        if (d == null) {
            return false;
        }

        return strictContains(d.n1, d.n2, d.n3, actualNumber);
    }

    private void addMissReason(BacktestResult result, String reason) {
        if (result == null || reason == null || reason.length() == 0) {
            return;
        }

        if (result.missReasons == null) {
            result.missReasons = new ArrayList<String>();
        }

        result.missReasons.add(reason);
    }

    private void addHoldReason(BacktestResult result, String reason) {
        if (result == null || reason == null || reason.length() == 0) {
            return;
        }

        if (result.holdReasons == null) {
            result.holdReasons = new ArrayList<String>();
        }

        result.holdReasons.add(reason);
    }

    private String mostCommon(ArrayList<String> list) {
        if (list == null || list.size() == 0) {
            return "";
        }

        String best = "";
        int bestCount = 0;

        for (int i = 0; i < list.size(); i++) {
            String v = list.get(i);

            if (v == null || v.length() == 0) {
                continue;
            }

            int count = 0;

            for (int j = 0; j < list.size(); j++) {
                if (v.equals(list.get(j))) {
                    count++;
                }
            }

            if (count > bestCount) {
                best = v;
                bestCount = count;
            }
        }

        return best;
    }

    private boolean isSameIssue(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        return normalizeIssue(a).equals(normalizeIssue(b));
    }

    private int compareIssue(String a, String b) {
        String aa = normalizeIssue(a);
        String bb = normalizeIssue(b);

        if (aa.length() == 0 && bb.length() == 0) {
            return 0;
        }

        if (aa.length() == 0) {
            return -1;
        }

        if (bb.length() == 0) {
            return 1;
        }

        if (aa.length() > bb.length()) {
            return 1;
        }

        if (aa.length() < bb.length()) {
            return -1;
        }

        return aa.compareTo(bb);
    }

    private String normalizeIssue(String issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < issue.length(); i++) {
            char c = issue.charAt(i);

            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }

        String s = sb.toString();

        while (s.length() > 1 && s.startsWith("0")) {
            s = s.substring(1);
        }

        return s;
    }

    private String safeIssue(Models.Draw d) {
        if (d == null || d.issue == null) {
            return "";
        }

        return d.issue;
    }

    private String safeError(Throwable e) {
        if (e == null) {
            return "";
        }

        String msg = e.getMessage();

        if (msg == null || msg.length() == 0) {
            msg = e.toString();
        }

        return safeError(msg);
    }

    private String safeError(String msg) {
        if (msg == null) {
            return "";
        }

        String out = msg;

        try {
            String key = KeyProvider.getApiKey();

            if (key != null && key.length() > 0) {
                out = out.replace(key, "***");
            }
        } catch (Throwable e) {
        }

        if (out.length() > 120) {
            out = out.substring(0, 120);
        }

        return out;
    }

    private int getIntField(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Integer) {
                return ((Integer) v).intValue();
            }

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private double getDoubleField(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Double) {
                return ((Double) v).doubleValue();
            }

            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }

            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private String getStringField(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private void putIntField(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private void putDoubleField(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                Field f = obj.getClass().getField(fieldName);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private void putStringField(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private double clampProbability(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (max <= 0) {
            max = 120;
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max);
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private String cleanKeyPart(String value) {
        if (value == null) {
            return "";
        }

        String out = value.replace('|', '_');
        out = out.replace(' ', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');

        if (out.length() > 80) {
            out = out.substring(0, 80);
        }

        return out;
    }

    private String bucketText(String prefix, double value) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "B";
        }

        if (value <= 0.0d) {
            return prefix + "_0";
        }

        double v = value;

        if (v <= 1.0d) {
            v = v * 100.0d;
        }

        if (v < 30.0d) {
            return prefix + "_LOW";
        }

        if (v < 55.0d) {
            return prefix + "_MID";
        }

        if (v < 75.0d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private String bucketProb(String prefix, double p) {
        if (prefix == null || prefix.length() == 0) {
            prefix = "P";
        }

        if (p <= 0.0d) {
            return prefix + "_0";
        }

        if (p < 0.340d) {
            return prefix + "_LOW";
        }

        if (p < 0.375d) {
            return prefix + "_SAFE";
        }

        if (p < 0.400d) {
            return prefix + "_BASE";
        }

        if (p < 0.430d) {
            return prefix + "_HIGH";
        }

        return prefix + "_EXTREME";
    }

    private int nearestDistance(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return 99;
        }

        int best = 99;

        if (AppConfig.isValidNumber(n1)) {
            best = Math.min(best, Math.abs(actual - n1));
        }

        if (AppConfig.isValidNumber(n2)) {
            best = Math.min(best, Math.abs(actual - n2));
        }

        if (AppConfig.isValidNumber(n3)) {
            best = Math.min(best, Math.abs(actual - n3));
        }

        return best;
    }

    private boolean coversZone(int actual, int n1, int n2, int n3) {
        if (!AppConfig.isValidNumber(actual)) {
            return false;
        }

        int z = numberZone(actual);
        return numberZone(n1) == z || numberZone(n2) == z || numberZone(n3) == z;
    }

    private int numberZone(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return 0;
        }

        if (n <= 3) {
            return 1;
        }

        if (n <= 7) {
            return 2;
        }

        return 3;
    }

    private String zoneText(int n) {
        int z = numberZone(n);

        if (z == 1) {
            return "低区";
        }

        if (z == 2) {
            return "中区";
        }

        if (z == 3) {
            return "高区";
        }

        return "未知区";
    }

    private String formatNumber(int n) {
        if (!AppConfig.isValidNumber(n)) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private void appendPart(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("，");
        }

        sb.append(text);
    }

    private static final class BacktestRuntime {
        private final ArrayList<Models.Decision> recentDecisions;

        public BacktestRuntime() {
            recentDecisions = new ArrayList<Models.Decision>();
        }

        public void addDecision(Models.Decision d) {
            if (d == null) {
                return;
            }

            recentDecisions.add(d.copy());

            while (recentDecisions.size() > 1000) {
                recentDecisions.remove(0);
            }
        }

        public double recentBetFrequency(int window) {
            if (window <= 0) {
                window = 100;
            }

            if (recentDecisions.size() == 0) {
                return 0.0d;
            }

            int start = recentDecisions.size() - window;

            if (start < 0) {
                start = 0;
            }

            int total = 0;
            int bet = 0;

            for (int i = start; i < recentDecisions.size(); i++) {
                Models.Decision d = recentDecisions.get(i);

                if (d == null || d.decision == null || d.decision.length() == 0) {
                    continue;
                }

                total++;

                if (AppConfig.DECISION_BET.equals(d.decision)
                        && AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)) {
                    bet++;
                }
            }

            if (total <= 0) {
                return 0.0d;
            }

            return ((double) bet) / ((double) total);
        }
    }

    private static final class TeacherApplyReport {
        public ArrayList<Models.ComboCandidate> candidates;
        public int appliedCount;
        public int strongCount;
        public int riskCount;
        public int fieldWrittenCount;
        public String summary;

        public TeacherApplyReport() {
            candidates = new ArrayList<Models.ComboCandidate>();
            appliedCount = 0;
            strongCount = 0;
            riskCount = 0;
            fieldWrittenCount = 0;
            summary = "";
        }
    }

    private static final class StepResult {
        public Models.Decision decision;
        public List<Models.ShapeVector> currentVectors;
        public boolean hit;
        public int actualNumber;
        public String missReason;
        public int candidateCount;
        public int enrichCount;
        public int deepCount;

        public boolean frequencyTurnHold;
        public boolean futureLeakBlocked;
        public boolean earlySampleHold;
        public boolean exceptionHold;

        public int teacherMatrixApplied;
        public int teacherMatrixStrong;
        public int teacherMatrixRisk;
        public int teacherMatrixFieldWritten;
        public String teacherMatrixSummary;

        public int mainChainFieldWritten;
        public int finalDecisionRejectCount;
        public int noSampleNeutralCount;
        public int v20WatchNeutralCount;
        public int adaptiveWatchNeutralCount;
        public boolean shadowWouldHit;
        public int shadowActualNumber;

        public ArrayList<Models.LearningCase> learningCases;
        public int learningHitCount;
        public int learningMissCount;

        public StepResult() {
            decision = null;
            currentVectors = new ArrayList<Models.ShapeVector>();
            hit = false;
            actualNumber = 0;
            missReason = "";
            candidateCount = 0;
            enrichCount = 0;
            deepCount = 0;

            frequencyTurnHold = false;
            futureLeakBlocked = false;
            earlySampleHold = false;
            exceptionHold = false;

            teacherMatrixApplied = 0;
            teacherMatrixStrong = 0;
            teacherMatrixRisk = 0;
            teacherMatrixFieldWritten = 0;
            teacherMatrixSummary = "";

            mainChainFieldWritten = 0;
            finalDecisionRejectCount = 0;
            noSampleNeutralCount = 0;
            v20WatchNeutralCount = 0;
            adaptiveWatchNeutralCount = 0;
            shadowWouldHit = false;
            shadowActualNumber = 0;

            learningCases = new ArrayList<Models.LearningCase>();
            learningHitCount = 0;
            learningMissCount = 0;
        }
    }

    public static final class BacktestResult {
        public int ok;
        public String mode;
        public int window;
        public int requestedWindow;
        public int actualWindow;
        public int completeWindow;
        public int coveredWindow;
        public int sampled;
        public int sampleStride;
        public String startIssue;
        public String endIssue;
        public long createdAt;
        public long startedAt;
        public long finishedAt;
        public long costMs;

        public int totalSteps;
        public int betCount;
        public int holdCount;
        public int shadowCount;
        public int hitCount;
        public int missCount;
        public int realScopeBetCount;
        public int shadowScopeCount;
        public int holdScopeCount;
        public int shadowHitCount;
        public int shadowMissCount;

        public double frequency;
        public double winRate;
        public double shadowHitRate;
        public double pnl;
        public double maxDrawdown;
        public int longestLose;

        public double avgRawP;
        public double avgFinalP;
        public double avgGate;
        public double avgEdge;

        public double sumRawP;
        public double sumFinalP;
        public double sumGate;
        public double sumEdge;

        public int sumCandidateCount;
        public int sumEnrichCount;
        public int sumDeepCount;
        public int avgCandidateCount;
        public int avgEnrichCount;
        public int avgDeepCount;

        public int frequencyGuardHoldCount;
        public int futureLeakBlockCount;
        public int earlySampleHoldCount;
        public int exceptionHoldCount;

        public int teacherMatrixAppliedCount;
        public int teacherMatrixStrongCount;
        public int teacherMatrixRiskCount;
        public int teacherMatrixFieldWrittenCount;
        public int mainChainFieldWrittenCount;
        public int finalDecisionRejectCount;
        public int noSampleNeutralCount;
        public int v20WatchNeutralCount;
        public int adaptiveWatchNeutralCount;

        public int learningCaseCount;
        public int learnedPositionCount;
        public int learnedHitCount;
        public int learnedMissCount;
        public int learningFlushCount;

        public int autoLearnSegment;
        public int autoLearnStartIndex;
        public int autoLearnEndIndex;

        public int timedOut;
        public String timeoutReason;

        public String lastMissReason;
        public String topMissReason;
        public String topHoldReason;
        public String summary;

        public ArrayList<String> missReasons;
        public ArrayList<String> holdReasons;

        public BacktestResult() {
            ok = 0;
            mode = AppConfig.BACKTEST_MODE_FAST;
            window = 0;
            requestedWindow = 0;
            actualWindow = 0;
            completeWindow = 0;
            coveredWindow = 0;
            sampled = 0;
            sampleStride = 1;
            startIssue = "";
            endIssue = "";
            createdAt = 0L;
            startedAt = 0L;
            finishedAt = 0L;
            costMs = 0L;

            totalSteps = 0;
            betCount = 0;
            holdCount = 0;
            shadowCount = 0;
            hitCount = 0;
            missCount = 0;
            realScopeBetCount = 0;
            shadowScopeCount = 0;
            holdScopeCount = 0;
            shadowHitCount = 0;
            shadowMissCount = 0;

            frequency = 0.0d;
            winRate = 0.0d;
            shadowHitRate = 0.0d;
            pnl = 0.0d;
            maxDrawdown = 0.0d;
            longestLose = 0;

            avgRawP = 0.0d;
            avgFinalP = 0.0d;
            avgGate = 0.0d;
            avgEdge = 0.0d;

            sumRawP = 0.0d;
            sumFinalP = 0.0d;
            sumGate = 0.0d;
            sumEdge = 0.0d;

            sumCandidateCount = 0;
            sumEnrichCount = 0;
            sumDeepCount = 0;
            avgCandidateCount = 0;
            avgEnrichCount = 0;
            avgDeepCount = 0;

            frequencyGuardHoldCount = 0;
            futureLeakBlockCount = 0;
            earlySampleHoldCount = 0;
            exceptionHoldCount = 0;

            teacherMatrixAppliedCount = 0;
            teacherMatrixStrongCount = 0;
            teacherMatrixRiskCount = 0;
            teacherMatrixFieldWrittenCount = 0;
            mainChainFieldWrittenCount = 0;
            finalDecisionRejectCount = 0;
            noSampleNeutralCount = 0;
            v20WatchNeutralCount = 0;
            adaptiveWatchNeutralCount = 0;

            learningCaseCount = 0;
            learnedPositionCount = 0;
            learnedHitCount = 0;
            learnedMissCount = 0;
            learningFlushCount = 0;

            autoLearnSegment = 0;
            autoLearnStartIndex = 0;
            autoLearnEndIndex = 0;

            timedOut = 0;
            timeoutReason = "";

            lastMissReason = "";
            topMissReason = "";
            topHoldReason = "";
            summary = "";

            missReasons = new ArrayList<String>();
            holdReasons = new ArrayList<String>();
        }
    }
}
