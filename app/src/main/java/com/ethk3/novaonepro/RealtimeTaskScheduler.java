package com.ethk3.novaonepro;

/**
 * RealtimeTaskScheduler.java
 *
 * <p>实时任务调度器 · 满血修正版
 *
 * <p>核心原则： 1. 实时开奖抓取优先。 2. 开奖保存与严格结算优先。 3. 下一期正式 Decision 生成优先。 4. 自动回测必须避让开奖窗口、结算窗口、正式分析窗口。 5.
 * 手动完整深度回测只能分片执行，开奖前后自动 yield。 6. busy 状态分通道保存，避免一个任务结束误清另一个任务。 7. 后台 Learning / TeacherMatrix /
 * Backtest 永远给实时正式预测让路。 8. 不在调度器里救援 BET，不在调度器里降低门槛，不参与策略裁决。
 *
 * <p>Java 7 / Android 原生 / 无第三方库。
 */
public final class RealtimeTaskScheduler {

    /** R811 canonical lane order; larger rank always wins and every lane is single-flight. */
    public enum Lane {
        LIVE_SETTLEMENT(600),
        LIVE_ANALYSIS(500),
        LIVE_FETCH_RECOVERY(400),
        LIVE_RETRY(300),
        DEFERRED(200),
        RESEARCH(100);

        public final int rank;

        Lane(int rank) {
            this.rank = rank;
        }
    }

    private final boolean[] r811LaneBusy = new boolean[Lane.values().length];

    public synchronized boolean tryAcquireLane(Lane lane) {
        if (lane == null) return false;
        int index = lane.ordinal();
        if (r811LaneBusy[index]) return false;
        if ((lane == Lane.DEFERRED || lane == Lane.RESEARCH)
                && RealtimePressureGateR811.isUnderPressure()) return false;
        r811LaneBusy[index] = true;
        return true;
    }

    public synchronized void releaseLane(Lane lane) {
        if (lane != null) r811LaneBusy[lane.ordinal()] = false;
    }

    public static int compareLane(Lane left, Lane right) {
        int l = left == null ? Integer.MIN_VALUE : left.rank;
        int r = right == null ? Integer.MIN_VALUE : right.rank;
        return l == r ? 0 : (l > r ? -1 : 1);
    }

    public static boolean r811LaneOrderValid() {
        return Lane.LIVE_SETTLEMENT.rank > Lane.LIVE_ANALYSIS.rank
                && Lane.LIVE_ANALYSIS.rank > Lane.LIVE_FETCH_RECOVERY.rank
                && Lane.LIVE_FETCH_RECOVERY.rank > Lane.LIVE_RETRY.rank
                && Lane.LIVE_RETRY.rank > Lane.DEFERRED.rank
                && Lane.DEFERRED.rank > Lane.RESEARCH.rank;
    }

    public static final int PRIORITY_REALTIME_FETCH = 0;
    public static final int PRIORITY_SAVE_DRAW = 1;
    public static final int PRIORITY_SETTLE_TRADE = 2;
    public static final int PRIORITY_BUILD_DECISION = 3;
    public static final int PRIORITY_BROADCAST_UI = 4;
    public static final int PRIORITY_AUTO_FAST_BACKTEST = 5;
    public static final int PRIORITY_AUTO_NORMAL_BACKTEST = 6;
    public static final int PRIORITY_MANUAL_FULL_BACKTEST_SLICE = 7;
    public static final int PRIORITY_BACKGROUND_LEARNING = 8;
    public static final int PRIORITY_TEACHER_MATRIX = 9;

    public static final int MODE_AUTO_FAST = 1;
    public static final int MODE_AUTO_NORMAL = 2;
    public static final int MODE_MANUAL_FULL = 3;

    public static final String CODE_OK = "OK";
    public static final String CODE_WORKER_BUSY = "WORKER_BUSY";
    public static final String CODE_REALTIME_PRIORITY = "REALTIME_PRIORITY";
    public static final String CODE_NEAR_OPEN_WINDOW = "NEAR_OPEN_WINDOW";
    public static final String CODE_AFTER_OPEN_WINDOW = "AFTER_OPEN_WINDOW";
    public static final String CODE_COOLDOWN = "COOLDOWN";
    public static final String CODE_MANUAL_BUSY = "MANUAL_BUSY";
    public static final String CODE_AUTO_BUSY = "AUTO_BUSY";
    public static final String CODE_REALTIME_BUSY = "REALTIME_BUSY";
    public static final String CODE_DATABASE_BUSY = "DATABASE_BUSY";
    public static final String CODE_ANALYSIS_BUSY = "ANALYSIS_BUSY";
    public static final String CODE_SETTLEMENT_BUSY = "SETTLEMENT_BUSY";
    public static final String CODE_LEARNING_BUSY = "LEARNING_BUSY";
    public static final String CODE_INVALID_MODE = "INVALID_MODE";
    public static final String CODE_INVALID_CLOCK = "INVALID_CLOCK";
    public static final String CODE_FORCE_ALLOW = "FORCE_ALLOW";
    public static final String CODE_YIELD = "YIELD";

    public static final int BLOCK_AUTO_NORMAL_BEFORE_OPEN_SECONDS = 75;
    public static final int BLOCK_AUTO_FAST_BEFORE_OPEN_SECONDS = 45;
    public static final int BLOCK_MANUAL_BEFORE_OPEN_SECONDS = 30;
    public static final int BLOCK_LEARNING_BEFORE_OPEN_SECONDS = 35;
    public static final int BLOCK_TEACHER_MATRIX_BEFORE_OPEN_SECONDS = 35;

    public static final int REALTIME_PRIORITY_AFTER_OPEN_SECONDS = 60;
    public static final int SETTLEMENT_PRIORITY_AFTER_OPEN_SECONDS = 90;
    public static final int TEACHER_MATRIX_AFTER_OPEN_BLOCK_SECONDS = 45;

    public static final long REALTIME_MIN_INTERVAL_MS = 650L;
    public static final long SAVE_DRAW_MIN_INTERVAL_MS = 220L;
    public static final long SETTLE_MIN_INTERVAL_MS = 220L;
    public static final long BUILD_DECISION_MIN_INTERVAL_MS = 650L;

    public static final long AUTO_FAST_MIN_INTERVAL_MS = 3500L;
    public static final long AUTO_NORMAL_MIN_INTERVAL_MS = 8000L;
    public static final long MANUAL_SLICE_MIN_INTERVAL_MS = 300L;
    public static final long LEARNING_MIN_INTERVAL_MS = 5000L;
    public static final long TEACHER_MATRIX_MIN_INTERVAL_MS = 1800L;

    public static final long MANUAL_SLICE_MAX_RUNNING_MS = 180L;
    public static final long MANUAL_SLICE_HARD_YIELD_MS = 260L;

    public static final long SUGGEST_RETRY_FAST_MS = 300L;
    public static final long SUGGEST_RETRY_NORMAL_MS = 1200L;
    public static final long SUGGEST_RETRY_SLOW_MS = 3000L;

    private boolean realtimeFetchBusy;
    private boolean saveDrawBusy;
    private boolean settleTradeBusy;
    private boolean buildDecisionBusy;
    private boolean broadcastUiBusy;
    private boolean autoFastBacktestBusy;
    private boolean autoNormalBacktestBusy;
    private boolean manualBacktestBusy;
    private boolean databaseWriteBusy;
    private boolean backgroundLearningBusy;
    private boolean teacherMatrixBusy;

    private boolean databaseWriteBySaveDraw;
    private boolean databaseWriteBySettle;
    private boolean databaseWriteByDecision;
    private boolean databaseWriteByExternal;

    private long lastRealtimeRunAt;
    private long lastSaveDrawRunAt;
    private long lastSettleRunAt;
    private long lastBuildDecisionRunAt;
    private long lastAutoFastRunAt;
    private long lastAutoNormalRunAt;
    private long lastManualSliceRunAt;
    private long lastLearningRunAt;
    private long lastTeacherMatrixRunAt;
    private long lastBroadcastUiAt;

    private long manualSliceStartedAt;
    private long realtimePriorityUntilAt;

    private int lastSecondsToNextOpen;
    private int lastSecondsAfterOpen;
    private int lastFetchAttemptInWindow;

    private String lastStatusCode;
    private String lastStatusText;
    private String lastOwner;
    private String databaseOwner;

    public RealtimeTaskScheduler() {
        lastRealtimeRunAt = 0L;
        lastSaveDrawRunAt = 0L;
        lastSettleRunAt = 0L;
        lastBuildDecisionRunAt = 0L;
        lastAutoFastRunAt = 0L;
        lastAutoNormalRunAt = 0L;
        lastManualSliceRunAt = 0L;
        lastLearningRunAt = 0L;
        lastTeacherMatrixRunAt = 0L;
        lastBroadcastUiAt = 0L;

        manualSliceStartedAt = 0L;
        realtimePriorityUntilAt = 0L;

        lastSecondsToNextOpen = -1;
        lastSecondsAfterOpen = -1;
        lastFetchAttemptInWindow = 0;

        lastStatusCode = CODE_OK;
        lastStatusText = "调度器已就绪";
        lastOwner = "";
        databaseOwner = "";

        resetAllBusyFlags();
    }

    public synchronized void updateClockWindow(
            int secondsToNextOpen, int secondsAfterOpen, int fetchAttemptInWindow) {
        lastSecondsToNextOpen = secondsToNextOpen;
        lastSecondsAfterOpen = secondsAfterOpen;

        if (fetchAttemptInWindow < 0) {
            lastFetchAttemptInWindow = 0;
        } else {
            lastFetchAttemptInWindow = fetchAttemptInWindow;
        }
    }

    public synchronized void requestRealtimePriority(long nowMs, long keepMs, String reason) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (keepMs < 1L) {
            keepMs = 1L;
        }

        long until = nowMs + keepMs;

        if (until > realtimePriorityUntilAt) {
            realtimePriorityUntilAt = until;
        }

        setStatus(
                CODE_REALTIME_PRIORITY,
                reason == null ? "实时任务进入优先窗口" : reason,
                "REALTIME_PRIORITY");
    }

    public synchronized void requestRealtimePrioritySeconds(int keepSeconds, String reason) {
        if (keepSeconds <= 0) {
            keepSeconds = REALTIME_PRIORITY_AFTER_OPEN_SECONDS;
        }

        requestRealtimePriority(System.currentTimeMillis(), ((long) keepSeconds) * 1000L, reason);
    }

    public synchronized void clearRealtimePriority() {
        realtimePriorityUntilAt = 0L;
        setStatus(CODE_OK, "实时优先窗口已释放", "");
    }

    public synchronized boolean isRealtimePriorityActive(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        return realtimePriorityUntilAt > 0L && nowMs < realtimePriorityUntilAt;
    }

    public synchronized long getRealtimePriorityRemainMs(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (realtimePriorityUntilAt <= 0L || nowMs >= realtimePriorityUntilAt) {
            return 0L;
        }

        return realtimePriorityUntilAt - nowMs;
    }

    public synchronized ScheduleDecision canRunRealtimeNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (realtimeFetchBusy) {
            return deny(
                    CODE_REALTIME_BUSY,
                    "实时抓取正在运行，避免重复触发",
                    PRIORITY_REALTIME_FETCH,
                    SUGGEST_RETRY_FAST_MS);
        }

        long remain = remainCooldown(nowMs, lastRealtimeRunAt, REALTIME_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "实时抓取冷却中，防止重复触发", PRIORITY_REALTIME_FETCH, remain);
        }

        if (databaseWriteBusy && !isSettlementPriorityWindow(secondsAfterOpen)) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "数据库正在写入，实时抓取稍后重试",
                    PRIORITY_REALTIME_FETCH,
                    SUGGEST_RETRY_FAST_MS);
        }

        return allow("允许实时抓取", PRIORITY_REALTIME_FETCH);
    }

    public synchronized ScheduleDecision canSaveDrawNow(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (saveDrawBusy) {
            return deny(
                    CODE_WORKER_BUSY, "开奖保存正在执行，避免重复写库", PRIORITY_SAVE_DRAW, SUGGEST_RETRY_FAST_MS);
        }

        if (databaseWriteBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "数据库写入通道忙，开奖保存稍后重试",
                    PRIORITY_SAVE_DRAW,
                    SUGGEST_RETRY_FAST_MS);
        }

        long remain = remainCooldown(nowMs, lastSaveDrawRunAt, SAVE_DRAW_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "开奖保存冷却中", PRIORITY_SAVE_DRAW, remain);
        }

        return allow("允许保存开奖", PRIORITY_SAVE_DRAW);
    }

    public synchronized ScheduleDecision canSettleNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (settleTradeBusy) {
            return deny(
                    CODE_SETTLEMENT_BUSY,
                    "结算通道正在执行，避免重复结算",
                    PRIORITY_SETTLE_TRADE,
                    SUGGEST_RETRY_FAST_MS);
        }

        if (databaseWriteBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "数据库写入通道忙，结算稍后重试",
                    PRIORITY_SETTLE_TRADE,
                    SUGGEST_RETRY_FAST_MS);
        }

        long remain = remainCooldown(nowMs, lastSettleRunAt, SETTLE_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "结算冷却中，防止同一期重复写入", PRIORITY_SETTLE_TRADE, remain);
        }

        return allow("允许严格结算", PRIORITY_SETTLE_TRADE);
    }

    public synchronized ScheduleDecision canBuildDecisionNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (buildDecisionBusy) {
            return deny(
                    CODE_ANALYSIS_BUSY,
                    "正式分析正在运行，避免重复生成 Decision",
                    PRIORITY_BUILD_DECISION,
                    SUGGEST_RETRY_FAST_MS);
        }

        if (saveDrawBusy || settleTradeBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "保存开奖或结算正在写库，正式分析稍后接管",
                    PRIORITY_BUILD_DECISION,
                    SUGGEST_RETRY_FAST_MS);
        }

        if (databaseWriteBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "数据库写入通道忙，正式分析稍后接管",
                    PRIORITY_BUILD_DECISION,
                    SUGGEST_RETRY_FAST_MS);
        }

        long remain = remainCooldown(nowMs, lastBuildDecisionRunAt, BUILD_DECISION_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "正式分析冷却中", PRIORITY_BUILD_DECISION, remain);
        }

        return allow("允许生成下一期正式 Decision", PRIORITY_BUILD_DECISION);
    }

    public synchronized ScheduleDecision canBroadcastUiNow(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (broadcastUiBusy) {
            return deny(
                    CODE_WORKER_BUSY,
                    "UI广播正在执行，避免重复广播",
                    PRIORITY_BROADCAST_UI,
                    SUGGEST_RETRY_FAST_MS);
        }

        return allow("允许UI广播", PRIORITY_BROADCAST_UI);
    }

    public synchronized ScheduleDecision canRunAutoBacktestNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen, int autoMode) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        int priority = priorityForAutoMode(autoMode);

        if (autoMode != MODE_AUTO_FAST && autoMode != MODE_AUTO_NORMAL) {
            return deny(CODE_INVALID_MODE, "未知自动回测模式", priority, SUGGEST_RETRY_NORMAL_MS);
        }

        if (manualBacktestBusy) {
            return deny(CODE_MANUAL_BUSY, "手动深度回测分片运行中，自动回测让路", priority, SUGGEST_RETRY_SLOW_MS);
        }

        if (autoFastBacktestBusy || autoNormalBacktestBusy) {
            return deny(CODE_AUTO_BUSY, "自动回测已有任务运行，禁止重复启动", priority, SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimeCoreBusy()) {
            return deny(
                    CODE_REALTIME_BUSY,
                    "实时抓取/保存/结算/分析正在运行，自动回测让路",
                    priority,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (databaseWriteBusy) {
            return deny(CODE_DATABASE_BUSY, "数据库写入中，自动回测让路", priority, SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimePriorityWindow(secondsToNextOpen, secondsAfterOpen)
                || isRealtimePriorityActive(nowMs)) {
            return deny(CODE_REALTIME_PRIORITY, "处于实时优先窗口，自动回测暂停", priority, SUGGEST_RETRY_SLOW_MS);
        }

        if (autoMode == MODE_AUTO_FAST) {
            if (isBeforeOpen(secondsToNextOpen, BLOCK_AUTO_FAST_BEFORE_OPEN_SECONDS)) {
                return deny(
                        CODE_NEAR_OPEN_WINDOW,
                        "开奖前45秒内禁止快速自动回测",
                        PRIORITY_AUTO_FAST_BACKTEST,
                        SUGGEST_RETRY_SLOW_MS);
            }

            long remain = remainCooldown(nowMs, lastAutoFastRunAt, AUTO_FAST_MIN_INTERVAL_MS);

            if (remain > 0L) {
                return deny(CODE_COOLDOWN, "快速自动回测冷却中", PRIORITY_AUTO_FAST_BACKTEST, remain);
            }

            return allow("允许快速自动回测", PRIORITY_AUTO_FAST_BACKTEST);
        }

        if (autoMode == MODE_AUTO_NORMAL) {
            if (isBeforeOpen(secondsToNextOpen, BLOCK_AUTO_NORMAL_BEFORE_OPEN_SECONDS)) {
                return deny(
                        CODE_NEAR_OPEN_WINDOW,
                        "开奖前75秒内禁止普通自动回测",
                        PRIORITY_AUTO_NORMAL_BACKTEST,
                        SUGGEST_RETRY_SLOW_MS);
            }

            long remain = remainCooldown(nowMs, lastAutoNormalRunAt, AUTO_NORMAL_MIN_INTERVAL_MS);

            if (remain > 0L) {
                return deny(CODE_COOLDOWN, "普通自动回测冷却中", PRIORITY_AUTO_NORMAL_BACKTEST, remain);
            }

            return allow("允许普通自动回测", PRIORITY_AUTO_NORMAL_BACKTEST);
        }

        return deny(CODE_INVALID_MODE, "未知自动回测模式", priority, SUGGEST_RETRY_NORMAL_MS);
    }

    public synchronized ScheduleDecision canRunManualSliceNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (isRealtimeCoreBusy()) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    "实时抓取/保存/结算/分析优先，手动深测分片让路",
                    PRIORITY_MANUAL_FULL_BACKTEST_SLICE,
                    600L);
        }

        if (databaseWriteBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    "数据库写入中，手动深测分片让路",
                    PRIORITY_MANUAL_FULL_BACKTEST_SLICE,
                    600L);
        }

        if (shouldYieldForOpenWindow(secondsToNextOpen, secondsAfterOpen)) {
            return deny(
                    CODE_NEAR_OPEN_WINDOW,
                    "接近开奖或开奖后实时窗口，手动深测分片暂停",
                    PRIORITY_MANUAL_FULL_BACKTEST_SLICE,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        long remain = remainCooldown(nowMs, lastManualSliceRunAt, MANUAL_SLICE_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(
                    CODE_COOLDOWN,
                    "手动深测分片冷却中，避免CPU持续占用",
                    PRIORITY_MANUAL_FULL_BACKTEST_SLICE,
                    remain);
        }

        return allow("允许手动深度回测分片执行", PRIORITY_MANUAL_FULL_BACKTEST_SLICE);
    }

    public synchronized ScheduleDecision canRunBackgroundLearningNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (backgroundLearningBusy) {
            return deny(
                    CODE_LEARNING_BUSY,
                    "后台学习正在运行，禁止重复启动",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimeCoreBusy() || databaseWriteBusy) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    "实时任务或写库优先，后台学习延后",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimePriorityActive(nowMs)) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    "实时优先窗口未释放，后台学习延后",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_SLOW_MS);
        }

        if (isBeforeOpen(secondsToNextOpen, BLOCK_LEARNING_BEFORE_OPEN_SECONDS)) {
            return deny(
                    CODE_NEAR_OPEN_WINDOW,
                    "开奖前35秒内后台学习让路",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_SLOW_MS);
        }

        if (secondsAfterOpen >= 0 && secondsAfterOpen <= REALTIME_PRIORITY_AFTER_OPEN_SECONDS) {
            return deny(
                    CODE_AFTER_OPEN_WINDOW,
                    "开奖后60秒内后台学习让路",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_SLOW_MS);
        }

        long remain = remainCooldown(nowMs, lastLearningRunAt, LEARNING_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "后台学习冷却中", PRIORITY_BACKGROUND_LEARNING, remain);
        }

        return allow("允许后台小批量学习", PRIORITY_BACKGROUND_LEARNING);
    }

    public synchronized ScheduleDecision canRunTeacherMatrixNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (teacherMatrixBusy) {
            return deny(
                    CODE_LEARNING_BUSY,
                    "Teacher Matrix正在运行，禁止重复启动",
                    PRIORITY_TEACHER_MATRIX,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimeCoreBusy() || databaseWriteBusy) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    "实时任务或写库优先，Teacher Matrix延后",
                    PRIORITY_TEACHER_MATRIX,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimePriorityActive(nowMs)) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    "实时优先窗口未释放，Teacher Matrix延后",
                    PRIORITY_TEACHER_MATRIX,
                    SUGGEST_RETRY_SLOW_MS);
        }

        if (isBeforeOpen(secondsToNextOpen, BLOCK_TEACHER_MATRIX_BEFORE_OPEN_SECONDS)) {
            return deny(
                    CODE_NEAR_OPEN_WINDOW,
                    "开奖前35秒内Teacher Matrix让路",
                    PRIORITY_TEACHER_MATRIX,
                    SUGGEST_RETRY_SLOW_MS);
        }

        if (secondsAfterOpen >= 0 && secondsAfterOpen <= TEACHER_MATRIX_AFTER_OPEN_BLOCK_SECONDS) {
            return deny(
                    CODE_AFTER_OPEN_WINDOW,
                    "开奖后45秒内Teacher Matrix让路，先结算和正式出号",
                    PRIORITY_TEACHER_MATRIX,
                    SUGGEST_RETRY_SLOW_MS);
        }

        long remain = remainCooldown(nowMs, lastTeacherMatrixRunAt, TEACHER_MATRIX_MIN_INTERVAL_MS);

        if (remain > 0L) {
            return deny(CODE_COOLDOWN, "Teacher Matrix冷却中", PRIORITY_TEACHER_MATRIX, remain);
        }

        return allow("允许Teacher Matrix后台小批量学习", PRIORITY_TEACHER_MATRIX);
    }

    public synchronized ScheduleDecision canRunHeavyBackgroundNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen, String taskName) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        String name = taskName == null ? "后台任务" : taskName;

        if (isRealtimeCoreBusy()) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    name + "让路：实时抓取/保存/结算/正式分析正在运行",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (databaseWriteBusy) {
            return deny(
                    CODE_DATABASE_BUSY,
                    name + "让路：数据库写入中",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_NORMAL_MS);
        }

        if (isRealtimePriorityActive(nowMs)) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    name + "让路：实时优先窗口未释放",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_SLOW_MS);
        }

        if (isRealtimePriorityWindow(secondsToNextOpen, secondsAfterOpen)) {
            return deny(
                    CODE_REALTIME_PRIORITY,
                    name + "让路：开奖前后实时窗口",
                    PRIORITY_BACKGROUND_LEARNING,
                    SUGGEST_RETRY_SLOW_MS);
        }

        return allow(name + "允许运行", PRIORITY_BACKGROUND_LEARNING);
    }

    public synchronized boolean shouldYieldForOpenWindow(
            int secondsToNextOpen, int secondsAfterOpen) {
        if (isBeforeOpen(secondsToNextOpen, BLOCK_MANUAL_BEFORE_OPEN_SECONDS)) {
            setStatus(CODE_NEAR_OPEN_WINDOW, "开奖前30秒内，手动深测必须让路", "CLOCK");
            return true;
        }

        if (secondsAfterOpen >= 0 && secondsAfterOpen <= REALTIME_PRIORITY_AFTER_OPEN_SECONDS) {
            setStatus(CODE_AFTER_OPEN_WINDOW, "开奖后60秒内，实时抓取、结算、出号优先", "CLOCK");
            return true;
        }

        return false;
    }

    public synchronized boolean shouldYieldManualSliceByCost(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (!manualBacktestBusy || manualSliceStartedAt <= 0L) {
            return false;
        }

        long cost = nowMs - manualSliceStartedAt;

        if (cost >= MANUAL_SLICE_HARD_YIELD_MS) {
            setStatus(CODE_MANUAL_BUSY, "手动深测分片运行超过硬让路时间，必须yield", "MANUAL");
            return true;
        }

        if (cost >= MANUAL_SLICE_MAX_RUNNING_MS
                && isRealtimePriorityWindow(lastSecondsToNextOpen, lastSecondsAfterOpen)) {
            setStatus(CODE_REALTIME_PRIORITY, "手动深测接近开奖窗口，主动yield", "MANUAL");
            return true;
        }

        return false;
    }

    public synchronized boolean shouldYieldBackgroundNow(
            long nowMs, int secondsToNextOpen, int secondsAfterOpen) {
        updateClockWindow(secondsToNextOpen, secondsAfterOpen, lastFetchAttemptInWindow);

        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (isRealtimeCoreBusy()) {
            setStatus(CODE_REALTIME_PRIORITY, "后台任务让路：实时核心通道运行中", "BACKGROUND");
            return true;
        }

        if (databaseWriteBusy) {
            setStatus(CODE_DATABASE_BUSY, "后台任务让路：数据库写入中", "BACKGROUND");
            return true;
        }

        if (isRealtimePriorityActive(nowMs)) {
            setStatus(CODE_REALTIME_PRIORITY, "后台任务让路：实时优先窗口未释放", "BACKGROUND");
            return true;
        }

        if (isRealtimePriorityWindow(secondsToNextOpen, secondsAfterOpen)) {
            setStatus(CODE_REALTIME_PRIORITY, "后台任务让路：开奖前后实时窗口", "BACKGROUND");
            return true;
        }

        return false;
    }

    public synchronized boolean isRealtimePriorityWindow(
            int secondsToNextOpen, int secondsAfterOpen) {
        if (isBeforeOpen(secondsToNextOpen, BLOCK_AUTO_FAST_BEFORE_OPEN_SECONDS)) {
            return true;
        }

        if (secondsAfterOpen >= 0 && secondsAfterOpen <= REALTIME_PRIORITY_AFTER_OPEN_SECONDS) {
            return true;
        }

        return false;
    }

    public synchronized boolean isSettlementPriorityWindow(int secondsAfterOpen) {
        return secondsAfterOpen >= 0 && secondsAfterOpen <= SETTLEMENT_PRIORITY_AFTER_OPEN_SECONDS;
    }

    public synchronized boolean isBeforeOpen(int secondsToNextOpen, int blockSeconds) {
        return secondsToNextOpen >= 0 && secondsToNextOpen <= blockSeconds;
    }

    public synchronized void markRealtimeBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        realtimeFetchBusy = true;
        lastRealtimeRunAt = nowMs;
        requestRealtimePriority(nowMs, 5000L, "实时抓取运行中，后台任务让路");
        setStatus(CODE_REALTIME_BUSY, "实时抓取运行中", "REALTIME_FETCH");
    }

    public synchronized void markRealtimeIdle() {
        realtimeFetchBusy = false;
        setStatus(CODE_OK, "实时抓取已完成", "");
    }

    public synchronized void markSaveDrawBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        saveDrawBusy = true;
        databaseWriteBySaveDraw = true;
        recomputeDatabaseWriteBusy();
        lastSaveDrawRunAt = nowMs;
        requestRealtimePriority(nowMs, 8000L, "开奖保存写库中，结算和正式分析优先");
        setStatus(CODE_DATABASE_BUSY, "开奖保存写库中", "SAVE_DRAW");
    }

    public synchronized void markSaveDrawIdle() {
        saveDrawBusy = false;
        databaseWriteBySaveDraw = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "开奖保存写库完成", "");
    }

    public synchronized void markSettleBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        settleTradeBusy = true;
        databaseWriteBySettle = true;
        recomputeDatabaseWriteBusy();
        lastSettleRunAt = nowMs;
        requestRealtimePriority(nowMs, 10000L, "严格结算写库中，正式分析随后接管");
        setStatus(CODE_SETTLEMENT_BUSY, "严格结算写库中", "SETTLE");
    }

    public synchronized void markSettleIdle() {
        settleTradeBusy = false;
        databaseWriteBySettle = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "严格结算完成", "");
    }

    public synchronized void markBuildDecisionBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        buildDecisionBusy = true;
        lastBuildDecisionRunAt = nowMs;
        requestRealtimePriority(nowMs, 18000L, "正式分析生成Decision中，后台学习/回测/教师矩阵让路");
        setStatus(CODE_ANALYSIS_BUSY, "正式分析生成Decision中", "BUILD_DECISION");
    }

    public synchronized void markBuildDecisionSaveBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        databaseWriteByDecision = true;
        recomputeDatabaseWriteBusy();
        lastBuildDecisionRunAt = nowMs;
        setStatus(CODE_DATABASE_BUSY, "正式Decision保存写库中", "BUILD_DECISION_SAVE");
    }

    public synchronized void markBuildDecisionSaveIdle() {
        databaseWriteByDecision = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "正式Decision保存完成", "");
    }

    public synchronized void markBuildDecisionIdle() {
        buildDecisionBusy = false;
        databaseWriteByDecision = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "正式分析完成", "");
    }

    public synchronized void markBroadcastUiBusy() {
        broadcastUiBusy = true;
        lastBroadcastUiAt = System.currentTimeMillis();
        setStatus(CODE_WORKER_BUSY, "UI广播中", "BROADCAST_UI");
    }

    public synchronized void markBroadcastUiIdle() {
        broadcastUiBusy = false;
        setStatus(CODE_OK, "UI广播完成", "");
    }

    public synchronized void markAutoBacktestBusy(long nowMs, int autoMode) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        if (autoMode == MODE_AUTO_FAST) {
            autoFastBacktestBusy = true;
            lastAutoFastRunAt = nowMs;
            setStatus(CODE_AUTO_BUSY, "快速自动回测运行中", "AUTO_FAST");
            return;
        }

        if (autoMode == MODE_AUTO_NORMAL) {
            autoNormalBacktestBusy = true;
            lastAutoNormalRunAt = nowMs;
            setStatus(CODE_AUTO_BUSY, "普通自动回测运行中", "AUTO_NORMAL");
            return;
        }

        autoNormalBacktestBusy = true;
        lastAutoNormalRunAt = nowMs;
        setStatus(CODE_AUTO_BUSY, "自动回测运行中", "AUTO_UNKNOWN");
    }

    public synchronized void markAutoBacktestIdle() {
        autoFastBacktestBusy = false;
        autoNormalBacktestBusy = false;
        setStatus(CODE_OK, "自动回测已完成", "");
    }

    public synchronized void markManualBacktestBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        manualBacktestBusy = true;
        lastManualSliceRunAt = nowMs;
        manualSliceStartedAt = nowMs;
        setStatus(CODE_MANUAL_BUSY, "手动深度回测分片运行中", "MANUAL_FULL");
    }

    public synchronized void markManualBacktestIdle() {
        manualBacktestBusy = false;
        manualSliceStartedAt = 0L;
        setStatus(CODE_OK, "手动深度回测分片已让出", "");
    }

    public synchronized void markManualBacktestFinished() {
        manualBacktestBusy = false;
        manualSliceStartedAt = 0L;
        setStatus(CODE_OK, "手动深度回测已完成", "");
    }

    public synchronized void markDatabaseWriteBusy() {
        databaseWriteByExternal = true;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_DATABASE_BUSY, "数据库写入中", "DB_WRITE");
    }

    public synchronized void markDatabaseWriteBusy(String owner) {
        databaseWriteByExternal = true;
        databaseOwner = owner == null ? "DB_WRITE" : owner;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_DATABASE_BUSY, "数据库写入中：" + databaseOwner, databaseOwner);
    }

    public synchronized void markDatabaseWriteIdle() {
        databaseWriteByExternal = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "数据库写入完成", "");
    }

    public synchronized void markDatabaseWriteIdle(String owner) {
        databaseWriteByExternal = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "数据库写入完成：" + (owner == null ? "" : owner), "");
    }

    public synchronized void markBackgroundLearningBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        backgroundLearningBusy = true;
        lastLearningRunAt = nowMs;
        setStatus(CODE_LEARNING_BUSY, "后台学习运行中", "LEARNING");
    }

    public synchronized void markBackgroundLearningIdle() {
        backgroundLearningBusy = false;
        setStatus(CODE_OK, "后台学习已完成", "");
    }

    public synchronized void markTeacherMatrixBusy(long nowMs) {
        if (nowMs <= 0L) {
            nowMs = System.currentTimeMillis();
        }

        teacherMatrixBusy = true;
        lastTeacherMatrixRunAt = nowMs;
        setStatus(CODE_LEARNING_BUSY, "Teacher Matrix运行中", "TEACHER_MATRIX");
    }

    public synchronized void markTeacherMatrixIdle() {
        teacherMatrixBusy = false;
        setStatus(CODE_OK, "Teacher Matrix已完成", "");
    }

    public synchronized boolean isAnyWorkerBusy() {
        return realtimeFetchBusy
                || saveDrawBusy
                || settleTradeBusy
                || buildDecisionBusy
                || broadcastUiBusy
                || autoFastBacktestBusy
                || autoNormalBacktestBusy
                || manualBacktestBusy
                || databaseWriteBusy
                || backgroundLearningBusy
                || teacherMatrixBusy;
    }

    public synchronized boolean isRealtimeCoreBusy() {
        return realtimeFetchBusy || saveDrawBusy || settleTradeBusy || buildDecisionBusy;
    }

    public synchronized boolean isBackgroundBusy() {
        return autoFastBacktestBusy
                || autoNormalBacktestBusy
                || manualBacktestBusy
                || backgroundLearningBusy
                || teacherMatrixBusy;
    }

    public synchronized boolean isRealtimeBusy() {
        return realtimeFetchBusy;
    }

    public synchronized boolean isSaveDrawBusy() {
        return saveDrawBusy;
    }

    public synchronized boolean isSettleTradeBusy() {
        return settleTradeBusy;
    }

    public synchronized boolean isBuildDecisionBusy() {
        return buildDecisionBusy;
    }

    public synchronized boolean isBroadcastUiBusy() {
        return broadcastUiBusy;
    }

    public synchronized boolean isAutoBacktestBusy() {
        return autoFastBacktestBusy || autoNormalBacktestBusy;
    }

    public synchronized boolean isManualBacktestBusy() {
        return manualBacktestBusy;
    }

    public synchronized boolean isDatabaseWriteBusy() {
        return databaseWriteBusy;
    }

    public synchronized boolean isBackgroundLearningBusy() {
        return backgroundLearningBusy;
    }

    public synchronized boolean isTeacherMatrixBusy() {
        return teacherMatrixBusy;
    }

    public synchronized int getLastSecondsToNextOpen() {
        return lastSecondsToNextOpen;
    }

    public synchronized int getLastSecondsAfterOpen() {
        return lastSecondsAfterOpen;
    }

    public synchronized int getLastFetchAttemptInWindow() {
        return lastFetchAttemptInWindow;
    }

    public synchronized long getRealtimePriorityUntilAt() {
        return realtimePriorityUntilAt;
    }

    public synchronized long getManualSliceStartedAt() {
        return manualSliceStartedAt;
    }

    public synchronized long getLastRealtimeRunAt() {
        return lastRealtimeRunAt;
    }

    public synchronized long getLastBuildDecisionRunAt() {
        return lastBuildDecisionRunAt;
    }

    public synchronized String getLastStatusCode() {
        return lastStatusCode;
    }

    public synchronized String getLastStatusText() {
        return lastStatusText;
    }

    public synchronized String getLastOwner() {
        return lastOwner;
    }

    public synchronized String getDatabaseOwner() {
        return databaseOwner;
    }

    public synchronized String buildSchedulerStatusText() {
        StringBuilder sb = new StringBuilder();

        sb.append("调度：");

        if (realtimeFetchBusy) {
            sb.append("实时抓取");
        } else if (saveDrawBusy) {
            sb.append("保存开奖");
        } else if (settleTradeBusy) {
            sb.append("严格结算");
        } else if (buildDecisionBusy) {
            sb.append("正式分析");
        } else if (autoFastBacktestBusy) {
            sb.append("快速回测");
        } else if (autoNormalBacktestBusy) {
            sb.append("普通回测");
        } else if (manualBacktestBusy) {
            sb.append("手动深测");
        } else if (databaseWriteBusy) {
            sb.append("写库中");
        } else if (teacherMatrixBusy) {
            sb.append("矩阵学习");
        } else if (backgroundLearningBusy) {
            sb.append("后台学习");
        } else {
            sb.append("空闲");
        }

        sb.append("｜距开奖=");
        if (lastSecondsToNextOpen >= 0) {
            sb.append(lastSecondsToNextOpen).append("秒");
        } else {
            sb.append("未知");
        }

        sb.append("｜开奖后=");
        if (lastSecondsAfterOpen >= 0) {
            sb.append(lastSecondsAfterOpen).append("秒");
        } else {
            sb.append("未知");
        }

        sb.append("｜抓取次数=").append(lastFetchAttemptInWindow);

        if (realtimePriorityUntilAt > 0L) {
            long left = realtimePriorityUntilAt - System.currentTimeMillis();
            if (left > 0L) {
                sb.append("｜实时优先剩余=").append(left / 1000L).append("秒");
            }
        }

        if (databaseWriteBusy) {
            sb.append("｜DB=");
            sb.append(buildDatabaseBusyText());
        }

        if (lastOwner != null && lastOwner.length() > 0) {
            sb.append("｜owner=").append(lastOwner);
        }

        sb.append("｜").append(lastStatusText);

        String out = sb.toString();

        if (out.length() > 420) {
            out = out.substring(0, 420);
        }

        return out;
    }

    public synchronized String buildShortBadgeText() {
        if (realtimeFetchBusy) {
            return "抓取";
        }

        if (saveDrawBusy) {
            return "开奖";
        }

        if (settleTradeBusy) {
            return "结算";
        }

        if (buildDecisionBusy) {
            return "分析";
        }

        if (databaseWriteBusy) {
            return "写库";
        }

        if (autoFastBacktestBusy || autoNormalBacktestBusy) {
            return "回测";
        }

        if (manualBacktestBusy) {
            return "深测";
        }

        if (teacherMatrixBusy) {
            return "矩阵";
        }

        if (backgroundLearningBusy) {
            return "学习";
        }

        if (isRealtimePriorityWindow(lastSecondsToNextOpen, lastSecondsAfterOpen)
                || isRealtimePriorityActive(System.currentTimeMillis())) {
            return "等待";
        }

        return "就绪";
    }

    public synchronized void resetAllBusyFlags() {
        realtimeFetchBusy = false;
        saveDrawBusy = false;
        settleTradeBusy = false;
        buildDecisionBusy = false;
        broadcastUiBusy = false;
        autoFastBacktestBusy = false;
        autoNormalBacktestBusy = false;
        manualBacktestBusy = false;
        databaseWriteBusy = false;
        backgroundLearningBusy = false;
        teacherMatrixBusy = false;

        databaseWriteBySaveDraw = false;
        databaseWriteBySettle = false;
        databaseWriteByDecision = false;
        databaseWriteByExternal = false;
        databaseOwner = "";

        manualSliceStartedAt = 0L;
        setStatus(CODE_OK, "所有任务状态已重置", "");
    }

    public synchronized void resetBackgroundBusyFlags() {
        autoFastBacktestBusy = false;
        autoNormalBacktestBusy = false;
        manualBacktestBusy = false;
        backgroundLearningBusy = false;
        teacherMatrixBusy = false;
        manualSliceStartedAt = 0L;
        setStatus(CODE_OK, "后台任务状态已重置，实时通道不受影响", "");
    }

    public synchronized void resetRealtimeBusyFlags() {
        realtimeFetchBusy = false;
        saveDrawBusy = false;
        settleTradeBusy = false;
        buildDecisionBusy = false;
        databaseWriteBySaveDraw = false;
        databaseWriteBySettle = false;
        databaseWriteByDecision = false;
        recomputeDatabaseWriteBusy();
        setStatus(CODE_OK, "实时核心任务状态已重置", "");
    }

    private void recomputeDatabaseWriteBusy() {
        databaseWriteBusy =
                databaseWriteBySaveDraw
                        || databaseWriteBySettle
                        || databaseWriteByDecision
                        || databaseWriteByExternal;

        if (!databaseWriteBusy) {
            databaseOwner = "";
            return;
        }

        if (databaseWriteBySaveDraw) {
            databaseOwner = appendOwner(databaseOwner, "SAVE_DRAW");
        }

        if (databaseWriteBySettle) {
            databaseOwner = appendOwner(databaseOwner, "SETTLE");
        }

        if (databaseWriteByDecision) {
            databaseOwner = appendOwner(databaseOwner, "DECISION");
        }

        if (databaseWriteByExternal) {
            databaseOwner = appendOwner(databaseOwner, "EXTERNAL");
        }
    }

    private String appendOwner(String oldText, String add) {
        if (add == null || add.length() == 0) {
            return oldText == null ? "" : oldText;
        }

        if (oldText == null || oldText.length() == 0) {
            return add;
        }

        if (oldText.indexOf(add) >= 0) {
            return oldText;
        }

        return oldText + "+" + add;
    }

    private String buildDatabaseBusyText() {
        StringBuilder sb = new StringBuilder();

        if (databaseWriteBySaveDraw) {
            appendTextPart(sb, "保存开奖");
        }

        if (databaseWriteBySettle) {
            appendTextPart(sb, "结算");
        }

        if (databaseWriteByDecision) {
            appendTextPart(sb, "Decision保存");
        }

        if (databaseWriteByExternal) {
            appendTextPart(
                    sb,
                    databaseOwner == null || databaseOwner.length() == 0 ? "外部写库" : databaseOwner);
        }

        if (sb.length() == 0) {
            sb.append("BUSY");
        }

        return sb.toString();
    }

    private void appendTextPart(StringBuilder sb, String text) {
        if (sb == null || text == null || text.length() == 0) {
            return;
        }

        if (sb.length() > 0) {
            sb.append("/");
        }

        sb.append(text);
    }

    private int priorityForAutoMode(int autoMode) {
        if (autoMode == MODE_AUTO_FAST) {
            return PRIORITY_AUTO_FAST_BACKTEST;
        }

        if (autoMode == MODE_AUTO_NORMAL) {
            return PRIORITY_AUTO_NORMAL_BACKTEST;
        }

        return PRIORITY_AUTO_NORMAL_BACKTEST;
    }

    private long remainCooldown(long nowMs, long lastRunAt, long cooldownMs) {
        if (cooldownMs <= 0L) {
            return 0L;
        }

        if (lastRunAt <= 0L) {
            return 0L;
        }

        long used = nowMs - lastRunAt;

        if (used < 0L) {
            return cooldownMs;
        }

        if (used >= cooldownMs) {
            return 0L;
        }

        long remain = cooldownMs - used;

        if (remain < 1L) {
            remain = 1L;
        }

        return remain;
    }

    private ScheduleDecision allow(String reasonText, int priority) {
        setStatus(CODE_OK, reasonText, "");
        return new ScheduleDecision(true, CODE_OK, reasonText, priority, 0L);
    }

    private ScheduleDecision forceAllow(String reasonText, int priority) {
        setStatus(CODE_FORCE_ALLOW, reasonText, "");
        return new ScheduleDecision(true, CODE_FORCE_ALLOW, reasonText, priority, 0L);
    }

    private ScheduleDecision deny(
            String code, String reasonText, int priority, long suggestedDelayMs) {
        setStatus(code, reasonText, "");
        return new ScheduleDecision(false, code, reasonText, priority, suggestedDelayMs);
    }

    private void setStatus(String code, String text, String owner) {
        if (code == null || code.length() == 0) {
            lastStatusCode = CODE_OK;
        } else {
            lastStatusCode = code;
        }

        if (text == null || text.length() == 0) {
            lastStatusText = "无状态说明";
        } else {
            lastStatusText = text;
        }

        lastOwner = owner == null ? "" : owner;
    }

    public static final class ScheduleDecision {
        public final boolean allowed;
        public final String code;
        public final String reasonText;
        public final int priority;
        public final long suggestedDelayMs;

        public ScheduleDecision(
                boolean allowed,
                String code,
                String reasonText,
                int priority,
                long suggestedDelayMs) {
            this.allowed = allowed;
            this.code = safe(code);
            this.reasonText = safe(reasonText);
            this.priority = priority;

            if (suggestedDelayMs < 0L) {
                this.suggestedDelayMs = 0L;
            } else {
                this.suggestedDelayMs = suggestedDelayMs;
            }
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isDenied() {
            return !allowed;
        }

        public String getCode() {
            return code;
        }

        public String getReasonText() {
            return reasonText;
        }

        public int getPriority() {
            return priority;
        }

        public long getSuggestedDelayMs() {
            return suggestedDelayMs;
        }

        public String toSimpleText() {
            StringBuilder sb = new StringBuilder();

            sb.append(allowed ? "允许" : "拒绝");
            sb.append("｜code=").append(code);
            sb.append("｜priority=").append(priority);
            sb.append("｜delay=").append(suggestedDelayMs).append("ms");
            sb.append("｜").append(reasonText);

            return sb.toString();
        }

        @Override
        public String toString() {
            return toSimpleText();
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }
    }

    // V57_B13_DUAL_CHANNEL_SCHEDULER
    public static final String V57_RUNTIME_CHANNEL = "REALTIME_SINGLE_QUEUE";
    public static final String V57_RESEARCH_CHANNEL = "LOW_PRIORITY_RESEARCH_QUEUE";
    private long v57RealtimeGeneration;
    private long v57ResearchGeneration;
    private long v57RealtimeHeartbeatAt;
    private long v57ResearchHeartbeatAt;
    private String v57RealtimeStage = "IDLE";
    private String v57ResearchStage = "IDLE";
    private String v57RecoveryReason = "";

    public static final class V57QueueSnapshot {
        public final long realtimeGeneration,
                researchGeneration,
                realtimeHeartbeatAt,
                researchHeartbeatAt;
        public final String realtimeStage, researchStage, recoveryReason;
        public final boolean realtimeBusy, researchBusy;

        public V57QueueSnapshot(
                long rg,
                long bg,
                long rh,
                long bh,
                String rs,
                String bs,
                String reason,
                boolean rb,
                boolean bb) {
            realtimeGeneration = rg;
            researchGeneration = bg;
            realtimeHeartbeatAt = rh;
            researchHeartbeatAt = bh;
            realtimeStage = rs == null ? "" : rs;
            researchStage = bs == null ? "" : bs;
            recoveryReason = reason == null ? "" : reason;
            realtimeBusy = rb;
            researchBusy = bb;
        }

        public String summary() {
            return "realtime="
                    + realtimeGeneration
                    + '@'
                    + realtimeHeartbeatAt
                    + '|'
                    + realtimeStage
                    + '|'
                    + realtimeBusy
                    + "|research="
                    + researchGeneration
                    + '@'
                    + researchHeartbeatAt
                    + '|'
                    + researchStage
                    + '|'
                    + researchBusy
                    + "|recovery="
                    + recoveryReason;
        }
    }

    public synchronized long beginV57Realtime(String stage) {
        v57RealtimeGeneration++;
        v57RealtimeHeartbeatAt = System.currentTimeMillis();
        v57RealtimeStage = stage == null ? "RUNNING" : stage;
        return v57RealtimeGeneration;
    }

    public synchronized void pulseV57Realtime(long generation, String stage) {
        if (generation != v57RealtimeGeneration) return;
        v57RealtimeHeartbeatAt = System.currentTimeMillis();
        v57RealtimeStage = stage == null ? v57RealtimeStage : stage;
    }

    public synchronized void finishV57Realtime(long generation, String stage) {
        if (generation != v57RealtimeGeneration) return;
        v57RealtimeHeartbeatAt = System.currentTimeMillis();
        v57RealtimeStage = stage == null ? "IDLE" : stage;
    }

    public synchronized long beginV57Research(String stage) {
        if (isRealtimeCoreBusy() || isRealtimePriorityActive(System.currentTimeMillis()))
            return -1L;
        v57ResearchGeneration++;
        v57ResearchHeartbeatAt = System.currentTimeMillis();
        v57ResearchStage = stage == null ? "RUNNING" : stage;
        return v57ResearchGeneration;
    }

    public synchronized void finishV57Research(long generation, String stage) {
        if (generation != v57ResearchGeneration) return;
        v57ResearchHeartbeatAt = System.currentTimeMillis();
        v57ResearchStage = stage == null ? "IDLE" : stage;
    }

    public synchronized void markV57RecoveryReason(String reason) {
        v57RecoveryReason = reason == null ? "" : reason;
    }

    public synchronized V57QueueSnapshot v57QueueSnapshot() {
        return new V57QueueSnapshot(
                v57RealtimeGeneration,
                v57ResearchGeneration,
                v57RealtimeHeartbeatAt,
                v57ResearchHeartbeatAt,
                v57RealtimeStage,
                v57ResearchStage,
                v57RecoveryReason,
                isRealtimeCoreBusy(),
                autoFastBacktestBusy
                        || autoNormalBacktestBusy
                        || manualBacktestBusy
                        || backgroundLearningBusy
                        || teacherMatrixBusy);
    }

    public static boolean v57SchedulerCanCreateBet() {
        return false;
    }
}
