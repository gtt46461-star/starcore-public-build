package com.ethk3.novaonepro;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class CountdownClock {

    public interface TickListener {
        void onTick(
                long countdownMs, long nextOpenTime, int nextFetchIntervalSec, boolean fastMode);
    }

    private static final long ONE_SECOND_MS = 1000L;
    private static final long ONE_MINUTE_MS = 60L * 1000L;

    /*
     * Binance1-PK10 开奖源时间兜底值。
     * 没有历史数据、接口时间异常、openTime 偏移过大时，用系统分钟 + OPEN_SECOND 兜底。
     */
    private static final int OPEN_SECOND = 11;

    private static final long MIN_ROUND_INTERVAL_MS = 30L * 1000L;
    private static final long MAX_ROUND_INTERVAL_MS = 120L * 1000L;
    private static final long DEFAULT_ROUND_INTERVAL_MS = ONE_MINUTE_MS;

    private static final long CLOCK_STALE_FACTOR = 3L;
    private static final long MOVE_ROUND_GUARD_MS = 500L;

    /*
     * 接口 openTime 与本机时间偏移过大时，不硬信接口时间。
     */
    private static final long API_TIME_FUTURE_MAX_FACTOR = 2L;
    private static final long API_TIME_PAST_MAX_FACTOR = 10L;

    private final Handler handler;
    private final DataGuard dataGuard;

    private volatile TickListener listener;
    private volatile boolean running;
    private static volatile CountdownClock liveOwnerR72;

    private long nextOpenTime;
    private long roundIntervalMs;
    private CountdownProjectionR971.Anchor clockAnchorCP12;

    private long lastFetchAt;
    private long lastFetchMonoR72=-1L;
    private long lastSyncAt;
    private long lastTickAt;
    private long lastMoveRoundAt;

    private String currentIssue;
    private String targetIssue;
    private final AtomicLong clockRevision = new AtomicLong(0L);

    private String realtimeIssueKey;
    private int realtimeFetchCount;
    private int totalFetchCount;

    private String lastModeText;
    private String lastClockWarning;
    private String lastSyncSource;

    private final Runnable tickRunnable =
            new Runnable() {
                @Override
                public void run() {
                    if (!running) {
                        return;
                    }

                    try {
                        tickOnceInternal();
                    } catch (RuntimeException failure) {
                        lastClockWarning = "R973_TICK_CALLBACK_FAILED:" + failure.getClass().getSimpleName();
                        android.util.Log.e("CountdownClock", lastClockWarning, failure);
                    } finally {
                        // A single listener exception must not permanently lose the clock loop.
                        long delay = AppConfig.TICK_INTERVAL_MS;
                        if (delay <= 0L) delay = ONE_SECOND_MS;
                        if (running) handler.postDelayed(this, delay);
                    }
                }
            };

    public CountdownClock() { this(Looper.getMainLooper()); }

    /** Service supplies its clock-only Looper; this class never creates or owns that thread. */
    public CountdownClock(Looper clockLooper) {
        if (clockLooper == null) throw new IllegalArgumentException("R973_CLOCK_LOOPER_REQUIRED");
        handler = new Handler(clockLooper);
        dataGuard = new DataGuard();

        listener = null;
        running = false;

        nextOpenTime = 0L;
        roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;

        lastFetchAt = 0L;
        lastSyncAt = 0L;
        lastTickAt = 0L;
        lastMoveRoundAt = 0L;

        currentIssue = "";
        targetIssue = "";

        realtimeIssueKey = "";
        realtimeFetchCount = 0;
        totalFetchCount = 0;

        lastModeText = "未启动";
        lastClockWarning = "";
        lastSyncSource = "";
    }

    public synchronized void setListener(TickListener tickListener) {
        listener = tickListener;
    }

    public synchronized void start() {
        if (running) {
            return;
        }

        running = true;
        liveOwnerR72=this;
        handler.removeCallbacks(tickRunnable);

        if (nextOpenTime <= 0L) {
            nextOpenTime = calcNextOfficialOpenTime(System.currentTimeMillis());
            lastSyncSource = "system_start";
        }

        normalizeRoundInterval();
        normalizeNextOpenTime();
        resetRealtimeCounterIfIssueChanged();

        handler.post(tickRunnable);
    }

    public synchronized void stop() {
        if(liveOwnerR72==this)liveOwnerR72=null;
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public void tickOnceForTestOrManualRefresh() {
        tickOnceInternal();
    }

    private void tickOnceInternal() {
        final TickListener callback;
        final long countdown, next;
        final int fetchInterval;
        final boolean fast;
        synchronized (this) {
            normalizeRoundInterval();
            normalizeNextOpenTime();
            resetRealtimeCounterIfIssueChanged();
            lastTickAt = System.currentTimeMillis();
            lastModeText = fetchModeText();
            countdown = getCountdownMs();
            next = nextOpenTime;
            fetchInterval = getNextFetchIntervalSeconds();
            fast = isFastMode();
            callback = listener;
        }
        // R978: external callbacks may broadcast or enter a different owner. Never
        // retain the clock monitor across them: fetch/sync/stop must remain live.
        if (callback != null) callback.onTick(countdown, next, fetchInterval, fast);
    }

    public synchronized void syncByHistory(List<Models.Draw> history) {
        ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);

        if (clean == null || clean.size() == 0) {
            syncBySystemClock();
            return;
        }

        long inferred = dataGuard.inferRoundInterval(clean);
        if (isReasonableRoundInterval(inferred)) {
            roundIntervalMs = inferred;
        } else {
            roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;
        }

        Models.Draw latest = clean.get(clean.size() - 1);

        if (!acceptAuthoritativeIssue(latest.issue)) return;

        nextOpenTime = estimateNextOpenTimeFromDraw(latest);
        normalizeNextOpenTime();

        lastSyncAt = System.currentTimeMillis();
        lastClockWarning = "";
        lastSyncSource = "history";
        captureClockAnchorCP12(lastSyncAt);

        resetFetchTimerForNewIssue();
    }

    public synchronized void syncByLatestDraw(Models.Draw latestDraw, List<Models.Draw> history) {
        if (history != null && history.size() > 1) {
            ArrayList<Models.Draw> clean = dataGuard.normalizeHistory(history);
            long inferred = dataGuard.inferRoundInterval(clean);

            if (isReasonableRoundInterval(inferred)) {
                roundIntervalMs = inferred;
            } else {
                roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;
            }
        } else {
            roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;
        }

        if (latestDraw == null || !latestDraw.isValid()) {
            syncBySystemClock();
            return;
        }

        String oldTarget = targetIssue;
        if (!acceptAuthoritativeIssue(latestDraw.issue)) {
            lastClockWarning = "忽略倒退开奖期=" + safeText(latestDraw.issue);
            lastSyncSource = "latest_draw_rejected_stale";
            return;
        }

        nextOpenTime = estimateNextOpenTimeFromDraw(latestDraw);
        normalizeNextOpenTime();

        lastSyncAt = System.currentTimeMillis();
        lastClockWarning = "";
        lastSyncSource = "latest_draw";
        captureClockAnchorCP12(lastSyncAt);

        if (!sameText(oldTarget, targetIssue)) {
            resetFetchTimerForNewIssue();
        } else {
            resetRealtimeCounterIfIssueChanged();
        }
    }

    public synchronized void syncBySystemClock() {
        normalizeRoundInterval();nextOpenTime=0L;clockAnchorCP12=null;
        lastSyncAt=System.currentTimeMillis();lastClockWarning="STALE_CLOCK|WAIT_AUTHORITATIVE_DRAW";
        lastSyncSource="WAIT_AUTHORITATIVE_DRAW";resetFetchTimerForNewIssue();
    }

    public synchronized void syncIssueOnly(String currentIssueValue) {
        if (!acceptAuthoritativeIssue(currentIssueValue)) return;
        if(clockAnchorCP12!=null&&!clockAnchorCP12.targetIssue.equals(targetIssue))clockAnchorCP12=null;
        resetRealtimeCounterIfIssueChanged();
    }

    public synchronized void forceNextOpenTime(long value) {
        if (value <= 0L) {
            return;
        }

        nextOpenTime = value;
        normalizeNextOpenTime();

        lastSyncAt = System.currentTimeMillis();
        lastClockWarning = "手动指定开奖时间";
        lastSyncSource = "manual_next_open_time";
        captureClockAnchorCP12(lastSyncAt);

        resetFetchTimerForNewIssue();
    }

    public synchronized void forceRoundInterval(long intervalMs) {
        if (!isReasonableRoundInterval(intervalMs)) {
            intervalMs = DEFAULT_ROUND_INTERVAL_MS;
        }

        roundIntervalMs = intervalMs;
        normalizeNextOpenTime();
        resetRealtimeCounterIfIssueChanged();
    }

    public synchronized long getCountdownMs() {
        long value=getSignedCountdownMs();
        return value==CountdownProjectionR971.STALE_CLOCK?0L:Math.max(0L,value);
    }

    public synchronized long getSignedCountdownMs() {
        if(clockAnchorCP12==null)return 0L;
        long signed=clockAnchorCP12.signedRemaining(targetIssue,getClockRevision(),TimeSourceR811.clockSessionCP12(),TimeSourceR811.monoNowMs());
        if(signed==CountdownProjectionR971.STALE_CLOCK){lastClockWarning="STALE_CLOCK|WAIT_AUTHORITATIVE_DRAW";return 0L;}
        return signed;
    }
    public synchronized CountdownProjectionR971.Anchor clockAnchorCP12(){return clockAnchorCP12;}
    /** Same immutable anchor from the sole running clock; no wall-clock reconstruction,
     * database access, broadcast wait or new countdown is needed when UI resumes. */
    public static CountdownProjectionR971.Anchor liveAnchorR72(){
        CountdownClock owner=liveOwnerR72;
        if(owner==null)return null;
        synchronized(owner){return owner.running&&liveOwnerR72==owner?owner.clockAnchorCP12:null;}
    }
    private void captureClockAnchorCP12(long sampledWall){
        try{
            final long sampledMono=TimeSourceR811.monoNowMs();
            final String session=TimeSourceR811.clockSessionCP12();
            final long revision=getClockRevision();
            final CountdownProjectionR971.Anchor previous=clockAnchorCP12;
            CountdownProjectionR971.Anchor captured=CountdownProjectionR971.Anchor.capture(
                    targetIssue,revision,session,nextOpenTime,sampledWall,sampledMono);
            // Resynchronizing the same durable period cannot renew its monotonic budget.
            if(previous!=null && previous.targetIssue.equals(targetIssue)
                    && previous.revision==revision && previous.session.equals(session)
                    && previous.deadlineWallMs==nextOpenTime) {
                if(sampledMono<previous.sampledMonoMs)
                    throw new IllegalArgumentException("CLOCK_ANCHOR_MONO_ROLLBACK");
                captured=CountdownProjectionR971.Anchor.received(targetIssue,revision,session,
                        nextOpenTime,Math.min(previous.deadlineMonoMs,captured.deadlineMonoMs),sampledMono);
            }
            clockAnchorCP12=captured;
        }catch(IllegalArgumentException stale){clockAnchorCP12=null;lastClockWarning="STALE_CLOCK|"+stale.getMessage();}
    }

    public synchronized long getNextOpenTime() {
        normalizeNextOpenTime();
        return nextOpenTime;
    }

    public synchronized long getRoundIntervalMs() {
        normalizeRoundInterval();
        return roundIntervalMs;
    }

    public synchronized String getCurrentIssue() {
        return currentIssue == null ? "" : currentIssue;
    }

    public synchronized String getTargetIssue() {
        return targetIssue == null ? "" : targetIssue;
    }

    public synchronized long getClockRevision() {
        return clockRevision.get();
    }

    public synchronized long getLastSyncAt() {
        return lastSyncAt;
    }

    public synchronized long getLastTickAt() {
        return lastTickAt;
    }

    public synchronized long getLastFetchAt() {
        return lastFetchAt;
    }

    public synchronized int getRealtimeFetchCount() {
        resetRealtimeCounterIfIssueChanged();
        return realtimeFetchCount;
    }

    public synchronized int getTotalFetchCount() {
        return totalFetchCount;
    }

    public synchronized String getLastModeText() {
        return lastModeText == null ? "" : lastModeText;
    }

    public synchronized String getLastClockWarning() {
        return lastClockWarning == null ? "" : lastClockWarning;
    }

    public synchronized String getLastSyncSource() {
        return lastSyncSource == null ? "" : lastSyncSource;
    }

    public synchronized boolean isExpired() {
        return getCountdownMs() <= 0L;
    }

    public synchronized boolean isBeforeOpen() {
        return getSignedCountdownMs() > 0L;
    }

    public synchronized boolean isAfterOpen() {
        return getSignedCountdownMs() <= 0L;
    }

    public synchronized boolean isNearOpen() {
        long left = getCountdownMs();
        return left <= AppConfig.FETCH_FAST_WINDOW_SEC * ONE_SECOND_MS;
    }

    public synchronized boolean isMidOpenWindow() {
        long left = getCountdownMs();

        return left <= AppConfig.FETCH_MID_WINDOW_SEC * ONE_SECOND_MS
                && left > AppConfig.FETCH_FAST_WINDOW_SEC * ONE_SECOND_MS;
    }

    public synchronized boolean isRealtimeFetchWindow() {
        normalizeNextOpenTime();

        long now = System.currentTimeMillis();

        if (nextOpenTime <= 0L) {
            return true;
        }

        long diff = nextOpenTime - now;

        if (diff >= 0L && diff <= AppConfig.REALTIME_OPEN_PREPARE_SEC * ONE_SECOND_MS) {
            return true;
        }

        return diff < 0L && -diff <= AppConfig.REALTIME_OPEN_AFTER_SEC * ONE_SECOND_MS;
    }

    public synchronized boolean isRealtimePrepareWindow() {
        normalizeNextOpenTime();

        long diff = nextOpenTime - System.currentTimeMillis();

        return diff >= 0L && diff <= AppConfig.REALTIME_OPEN_PREPARE_SEC * ONE_SECOND_MS;
    }

    public synchronized boolean isAfterOpenFastWindow() {
        normalizeNextOpenTime();

        long now = System.currentTimeMillis();

        if (nextOpenTime <= 0L) {
            return false;
        }

        long diff = now - nextOpenTime;

        return diff >= 0L && diff <= AppConfig.REALTIME_OPEN_AFTER_SEC * ONE_SECOND_MS;
    }

    public synchronized boolean isFastMode() {
        return isRealtimeFetchWindow() || isNearOpen();
    }

    public synchronized boolean isRealtimeFetchLimitReached() {
        resetRealtimeCounterIfIssueChanged();

        int max = AppConfig.REALTIME_MAX_FETCH_PER_ISSUE;
        if (max <= 0) {
            return false;
        }

        return realtimeFetchCount >= max;
    }

    public synchronized int getNextFetchIntervalSeconds() {
        if (isRealtimeFetchWindow()) {
            return 1;
        }

        long left = getCountdownMs();

        if (left <= 0L) {
            return AppConfig.FETCH_FAST_INTERVAL_SEC;
        }

        return dataGuard.nextFetchIntervalSeconds(left);
    }

    public synchronized long getNextFetchDelayMs() {
        if (isRealtimeFetchWindow()) {
            long interval = AppConfig.REALTIME_FETCH_INTERVAL_MS;
            if (interval <= 0L) {
                interval = ONE_SECOND_MS;
            }
            return interval;
        }

        int sec = getNextFetchIntervalSeconds();

        if (sec <= 0) {
            sec = 1;
        }

        return sec * ONE_SECOND_MS;
    }

    public synchronized boolean shouldFetchNow() {
        long now = TimeSourceR811.monoNowMs();

        normalizeNextOpenTime();
        resetRealtimeCounterIfIssueChanged();

        if (lastFetchMonoR72 < 0L || now < lastFetchMonoR72) {
            return true;
        }

        if (isRealtimeFetchWindow()) {
            if (isRealtimeFetchLimitReached()) {
                return false;
            }

            long interval = AppConfig.REALTIME_FETCH_INTERVAL_MS;
            if (interval <= 0L) {
                interval = ONE_SECOND_MS;
            }

            return now - lastFetchMonoR72 >= interval;
        }

        long delay = getNextFetchDelayMs();

        return now - lastFetchMonoR72 >= delay;
    }

    public synchronized boolean shouldForceHistory100(int missFetchCount) {
        if (missFetchCount >= AppConfig.REALTIME_FORCE_HISTORY100_AFTER_MISS) {
            return true;
        }

        int every = AppConfig.REALTIME_HISTORY100_EVERY_N_FETCH;

        if (every <= 0) {
            return false;
        }

        if (totalFetchCount <= 0) {
            return false;
        }

        return totalFetchCount % every == 0;
    }

    public synchronized boolean shouldForceBackfill(int missFetchCount) {
        return missFetchCount >= AppConfig.REALTIME_FORCE_BACKFILL_AFTER_MISS;
    }

    public synchronized boolean isStaleLatestDraw(long latestDrawOpenTime) {
        if (latestDrawOpenTime <= 0L) {
            return true;
        }

        long now = System.currentTimeMillis();
        return now - latestDrawOpenTime >= AppConfig.REALTIME_STALE_WARN_MS;
    }

    public synchronized void markFetched() {
        resetRealtimeCounterIfIssueChanged();

        lastFetchAt = System.currentTimeMillis();
        lastFetchMonoR72 = TimeSourceR811.monoNowMs();
        totalFetchCount++;

        if (isRealtimeFetchWindow()) {
            realtimeFetchCount++;
        }
    }

    public synchronized void markFetched(boolean success) {
        markFetched();

        if (success) {
            lastClockWarning = "";
        }
    }

    public synchronized void resetFetchTimer() {
        lastFetchAt = 0L;
        lastFetchMonoR72 = -1L;
    }

    public synchronized void resetFetchTimerForNewIssue() {
        lastFetchAt = 0L;
        lastFetchMonoR72 = -1L;
        resetRealtimeCounterHard();
    }

    public synchronized boolean needsResync() {
        if (lastSyncAt <= 0L) {
            return true;
        }

        normalizeRoundInterval();

        long now = System.currentTimeMillis();

        return now - lastSyncAt > roundIntervalMs * CLOCK_STALE_FACTOR;
    }

    public synchronized boolean needsEmergencyResync() {
        if (nextOpenTime <= 0L) {
            return true;
        }

        normalizeRoundInterval();

        long now = System.currentTimeMillis();
        long diff = nextOpenTime - now;

        if (diff > roundIntervalMs * 2L) {
            return true;
        }

        return diff < -((long) AppConfig.REALTIME_OPEN_AFTER_SEC * ONE_SECOND_MS + roundIntervalMs);
    }

    public synchronized void moveToNextRound() {
        // Retiredsynthetic issue advancement. Fetch/new draw is the sole authoritative source.
        clockAnchorCP12=null;lastClockWarning="STALE_CLOCK|WAIT_AUTHORITATIVE_DRAW";
        lastSyncSource="WAIT_AUTHORITATIVE_DRAW";
    }

    public synchronized void onNewDrawArrived(Models.Draw newDraw, List<Models.Draw> history) {
        if (newDraw == null || !newDraw.isValid()) {
            return;
        }

        String oldCurrent = currentIssue;
        syncByLatestDraw(newDraw, history);

        if (!sameText(oldCurrent, currentIssue)) {
            resetFetchTimerForNewIssue();
        }
    }

    public synchronized String countdownText() {
        return UiFormatter.countdown(getCountdownMs());
    }

    public synchronized String fetchModeText() {
        if (isAfterOpenFastWindow()) {
            return "开奖后实时快抓";
        }

        if (isRealtimePrepareWindow()) {
            return "开奖前最后快抓";
        }

        if (isRealtimeFetchWindow()) {
            return "实时快抓";
        }

        if (isNearOpen()) {
            return "开奖前快速抓取";
        }

        if (isMidOpenWindow()) {
            return "开奖前中速抓取";
        }

        return "常规慢速等待";
    }

    public synchronized String buildShortStatusText() {
        StringBuilder sb = new StringBuilder();

        sb.append("倒计时 ");
        sb.append(countdownText());
        sb.append("｜");
        sb.append(fetchModeText());
        sb.append("｜目标期 ");
        sb.append(getTargetIssue().length() == 0 ? "-" : getTargetIssue());
        sb.append("｜本期抓取 ");
        sb.append(getRealtimeFetchCount());
        sb.append("/");
        sb.append(AppConfig.REALTIME_MAX_FETCH_PER_ISSUE);

        return sb.toString();
    }

    public synchronized String buildFullStatusText() {
        StringBuilder sb = new StringBuilder();

        sb.append("当前期：");
        sb.append(getCurrentIssue().length() == 0 ? "-" : getCurrentIssue());
        sb.append("\n目标期：");
        sb.append(getTargetIssue().length() == 0 ? "-" : getTargetIssue());

        sb.append("\n倒计时：");
        sb.append(countdownText());
        sb.append("｜模式：");
        sb.append(fetchModeText());

        sb.append("\n开奖时间：");
        sb.append(nextOpenTime);
        sb.append("｜间隔：");
        sb.append(getRoundIntervalMs() / ONE_SECOND_MS);
        sb.append("秒");

        sb.append("\n本期实时抓取：");
        sb.append(getRealtimeFetchCount());
        sb.append("/");
        sb.append(AppConfig.REALTIME_MAX_FETCH_PER_ISSUE);
        sb.append("｜总抓取：");
        sb.append(totalFetchCount);

        sb.append("\n同步源：");
        sb.append(getLastSyncSource());

        if (lastClockWarning != null && lastClockWarning.length() > 0) {
            sb.append("\n提醒：");
            sb.append(lastClockWarning);
        }

        return sb.toString();
    }

    public synchronized ClockSnapshot buildSnapshot() {
        ClockSnapshot s = new ClockSnapshot();

        s.running = running ? 1 : 0;
        s.currentIssue = getCurrentIssue();
        s.targetIssue = getTargetIssue();
        s.nextOpenTime = getNextOpenTime();
        s.countdownMs = getCountdownMs();
        s.signedCountdownMs = getSignedCountdownMs();
        s.roundIntervalMs = getRoundIntervalMs();
        s.lastFetchAt = lastFetchAt;
        s.lastSyncAt = lastSyncAt;
        s.lastTickAt = lastTickAt;
        s.nextFetchIntervalSec = getNextFetchIntervalSeconds();
        s.realtimeWindow = isRealtimeFetchWindow() ? 1 : 0;
        s.prepareWindow = isRealtimePrepareWindow() ? 1 : 0;
        s.afterOpenWindow = isAfterOpenFastWindow() ? 1 : 0;
        s.realtimeFetchCount = getRealtimeFetchCount();
        s.totalFetchCount = totalFetchCount;
        s.realtimeLimitReached = isRealtimeFetchLimitReached() ? 1 : 0;
        s.needsResync = needsResync() ? 1 : 0;
        s.needsEmergencyResync = needsEmergencyResync() ? 1 : 0;
        s.modeText = fetchModeText();
        s.warningText = getLastClockWarning();
        s.syncSource = getLastSyncSource();
        s.clockRevision = getClockRevision();

        return s;
    }

    private long estimateNextOpenTimeFromDraw(Models.Draw latestDraw) {
        long now = System.currentTimeMillis();

        if (latestDraw == null) {
            return 0L;
        }

        long open = latestDraw.openTime;

        if (open <= 0L) {
            lastClockWarning = "开奖缺少openTime，使用系统时间兜底";
            return 0L;
        }

        normalizeRoundInterval();

        long candidate = TargetIssueResolver.predrawDeadlineWallR20261008(latestDraw);
        long diff = candidate - now;

        if (diff > API_TIME_FUTURE_MAX_FACTOR * roundIntervalMs
                || diff < -API_TIME_PAST_MAX_FACTOR * roundIntervalMs) {
            lastClockWarning = "接口开奖时间偏离本地时间，使用系统时间兜底";
            return 0L;
        }

        return candidate;
    }

    private void normalizeNextOpenTime() {
        normalizeRoundInterval();
        // Thetargetdeadline is immutable until a real source synchronizes it. No getterrolls an old target into another minute.
    }

    private void normalizeRoundInterval() {
        if (!isReasonableRoundInterval(roundIntervalMs)) {
            roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;
        }
    }

    private boolean isReasonableRoundInterval(long intervalMs) {
        return intervalMs >= MIN_ROUND_INTERVAL_MS && intervalMs <= MAX_ROUND_INTERVAL_MS;
    }

    private long calcNextOfficialOpenTime(long now) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        cal.set(Calendar.MILLISECOND, 0);

        int second = cal.get(Calendar.SECOND);

        if (second < OPEN_SECOND) {
            cal.set(Calendar.SECOND, OPEN_SECOND);
        } else {
            cal.add(Calendar.MINUTE, 1);
            cal.set(Calendar.SECOND, OPEN_SECOND);
        }

        return cal.getTimeInMillis();
    }

    private void resetRealtimeCounterIfIssueChanged() {
        String key = buildRealtimeIssueKey();

        if (!sameText(realtimeIssueKey, key)) {
            realtimeIssueKey = key;
            realtimeFetchCount = 0;
        }
    }

    private void resetRealtimeCounterHard() {
        realtimeIssueKey = buildRealtimeIssueKey();
        realtimeFetchCount = 0;
    }

    private String buildRealtimeIssueKey() {
        String issuePart;

        if (targetIssue != null && targetIssue.length() > 0) {
            issuePart = targetIssue;
        } else if (currentIssue != null && currentIssue.length() > 0) {
            issuePart = dataGuard.buildNextIssue(currentIssue);
        } else {
            issuePart = "UNKNOWN";
        }

        /*
         * 必须带 nextOpenTime。
         * 否则接口延迟时 targetIssue 没变，下一分钟实时抓取计数不会清零。
         */
        return issuePart + "@" + nextOpenTime;
    }

    private boolean sameText(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    public static final class ClockSnapshot {
        public int running;
        public String currentIssue;
        public String targetIssue;
        public long nextOpenTime;
        public long countdownMs;
        public long signedCountdownMs;
        public long roundIntervalMs;
        public long lastFetchAt;
        public long lastSyncAt;
        public long lastTickAt;
        public int nextFetchIntervalSec;
        public int realtimeWindow;
        public int prepareWindow;
        public int afterOpenWindow;
        public int realtimeFetchCount;
        public int totalFetchCount;
        public int realtimeLimitReached;
        public int needsResync;
        public int needsEmergencyResync;
        public String modeText;
        public String warningText;
        public String syncSource;
        public long clockRevision;

        public ClockSnapshot() {
            running = 0;
            currentIssue = "";
            targetIssue = "";
            nextOpenTime = 0L;
            countdownMs = 0L;
            signedCountdownMs = 0L;
            roundIntervalMs = DEFAULT_ROUND_INTERVAL_MS;
            lastFetchAt = 0L;
            lastSyncAt = 0L;
            lastTickAt = 0L;
            nextFetchIntervalSec = AppConfig.FETCH_SLOW_INTERVAL_SEC;
            realtimeWindow = 0;
            prepareWindow = 0;
            afterOpenWindow = 0;
            realtimeFetchCount = 0;
            totalFetchCount = 0;
            realtimeLimitReached = 0;
            needsResync = 0;
            needsEmergencyResync = 0;
            modeText = "";
            warningText = "";
            syncSource = "";
            clockRevision = 0L;
        }

        public String toText() {
            StringBuilder sb = new StringBuilder();

            sb.append("running=");
            sb.append(running);
            sb.append("｜current=");
            sb.append(currentIssue);
            sb.append("｜target=");
            sb.append(targetIssue);
            sb.append("｜countdown=");
            sb.append(countdownMs);
            sb.append("｜mode=");
            sb.append(modeText);
            sb.append("｜revision=");
            sb.append(clockRevision);
            sb.append("｜fetch=");
            sb.append(realtimeFetchCount);
            sb.append("/");
            sb.append(AppConfig.REALTIME_MAX_FETCH_PER_ISSUE);

            if (syncSource != null && syncSource.length() > 0) {
                sb.append("｜sync=");
                sb.append(syncSource);
            }

            if (warningText != null && warningText.length() > 0) {
                sb.append("｜warn=");
                sb.append(warningText);
            }

            return sb.toString();
        }
    }

    /* STARCORE_TIANSHU_B02_MATERIALIZED_20260805 */
    private final DrawIntegrityRecoveryEngine b02Integrity = new DrawIntegrityRecoveryEngine();
    private long providerOpenTime;
    private long providerReceivedAt;
    private String providerIssue;
    private String providerClockFingerprint;
    private String providerClockState;

    public synchronized ProviderClockSnapshot syncFromBinanceProvider(
            Models.Draw draw,
            String providerIssueValue,
            long providerOpenTimeValue,
            long receivedAt,
            long nowMs) {
        DrawIntegrityRecoveryEngine.VerificationResult verified =
                b02Integrity.verifySingle(draw, AppConfig.LOTTERY_CODE, nowMs);
        ProviderClockSnapshot snapshot = new ProviderClockSnapshot();
        snapshot.gameCode = AppConfig.LOTTERY_CODE;
        snapshot.receivedAt = receivedAt;
        snapshot.providerOpenTime = providerOpenTimeValue;
        snapshot.providerIssue = safeText(providerIssueValue);
        snapshot.localNow = nowMs;
        snapshot.sourceLine = draw == null ? "" : safeText(draw.sourceLine);
        if (!verified.accepted) {
            snapshot.accepted = false;
            snapshot.state = "CLOCK_BLOCKED_" + verified.code;
            snapshot.publicMessage = verified.publicMessage;
            snapshot.fingerprint = verified.canonicalHash;
            providerClockState = snapshot.state;
            providerClockFingerprint = snapshot.fingerprint;
            return snapshot;
        }
        String expectedTarget = dataGuard.buildNextIssue(verified.issue);
        DrawIntegrityRecoveryEngine.TargetFreshness freshness =
                b02Integrity.checkTargetFreshness(
                        verified.issue, expectedTarget, providerOpenTimeValue, receivedAt, nowMs);
        snapshot.accepted = freshness.fresh;
        snapshot.currentIssue = verified.issue;
        snapshot.targetIssue = expectedTarget;
        snapshot.nextOpenTime =
                providerOpenTimeValue > 0L
                        ? providerOpenTimeValue + verified.estimatedIntervalMs
                        : nowMs + verified.estimatedIntervalMs;
        snapshot.countdownMs = Math.max(0L, snapshot.nextOpenTime - nowMs);
        snapshot.intervalMs = verified.estimatedIntervalMs;
        snapshot.ageMs = freshness.ageMs;
        snapshot.untilOpenMs = freshness.untilOpenMs;
        snapshot.state =
                freshness.fresh ? "BINANCE_PROVIDER_SYNCED" : "CLOCK_BLOCKED_" + freshness.code;
        snapshot.publicMessage = freshness.publicMessage;
        snapshot.fingerprint = freshness.fingerprint;
        if (freshness.fresh) {
            if (!acceptAuthoritativeIssue(verified.issue)) {
                snapshot.accepted = false;
                snapshot.state = "CLOCK_BLOCKED_CURRENT_DRAW_STALE";
                snapshot.publicMessage = "开奖期倒退，拒绝覆盖当前时钟";
                return snapshot;
            }
            this.nextOpenTime = snapshot.nextOpenTime;
            captureClockAnchorCP12(nowMs);
            this.roundIntervalMs = verified.estimatedIntervalMs;
            this.lastSyncAt = nowMs;
            this.lastSyncSource = "BINANCE_PROVIDER_TIME";
            this.providerOpenTime = providerOpenTimeValue;
            this.providerReceivedAt = receivedAt;
            this.providerIssue = safeText(providerIssueValue);
            this.providerClockFingerprint = snapshot.fingerprint;
            this.providerClockState = snapshot.state;
        }
        return snapshot;
    }

    public synchronized ProviderClockSnapshot b02ProviderClockSnapshot(long nowMs) {
        ProviderClockSnapshot snapshot = new ProviderClockSnapshot();
        snapshot.accepted = "BINANCE_PROVIDER_SYNCED".equals(providerClockState);
        snapshot.gameCode = AppConfig.LOTTERY_CODE;
        snapshot.currentIssue = safeText(currentIssue);
        snapshot.targetIssue = safeText(targetIssue);
        snapshot.providerIssue = safeText(providerIssue);
        snapshot.providerOpenTime = providerOpenTime;
        snapshot.receivedAt = providerReceivedAt;
        snapshot.localNow = nowMs;
        snapshot.nextOpenTime = nextOpenTime;
        snapshot.countdownMs = Math.max(0L, nextOpenTime - nowMs);
        snapshot.intervalMs = roundIntervalMs;
        snapshot.ageMs =
                providerReceivedAt <= 0L
                        ? Long.MAX_VALUE
                        : Math.max(0L, nowMs - providerReceivedAt);
        snapshot.untilOpenMs = nextOpenTime - nowMs;
        snapshot.state = safeText(providerClockState);
        snapshot.fingerprint = safeText(providerClockFingerprint);
        snapshot.sourceLine = safeText(lastSyncSource);
        snapshot.publicMessage = snapshot.accepted ? "币安开奖源时钟已同步" : "币安开奖源时钟尚未建立";
        return snapshot;
    }

    public synchronized boolean b02ProviderClockFresh(long nowMs) {
        ProviderClockSnapshot snapshot = b02ProviderClockSnapshot(nowMs);
        long maxAge = Math.max(ONE_MINUTE_MS * 3L, snapshot.intervalMs * 3L);
        return snapshot.accepted
                && snapshot.ageMs <= maxAge
                && snapshot.targetIssue.length() > 0
                && snapshot.nextOpenTime > 0L;
    }

    public synchronized String b02TargetClockAudit(long nowMs) {
        ProviderClockSnapshot s = b02ProviderClockSnapshot(nowMs);
        return "game="
                + s.gameCode
                + "|current="
                + s.currentIssue
                + "|target="
                + s.targetIssue
                + "|providerIssue="
                + s.providerIssue
                + "|providerOpenTime="
                + s.providerOpenTime
                + "|receivedAt="
                + s.receivedAt
                + "|nextOpenTime="
                + s.nextOpenTime
                + "|countdownMs="
                + s.countdownMs
                + "|ageMs="
                + s.ageMs
                + "|state="
                + s.state
                + "|fingerprint="
                + s.fingerprint;
    }

    private static String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean acceptAuthoritativeIssue(String issue) {
        String incoming = safeText(issue);
        if (incoming.length() == 0) return false;
        String current = safeText(currentIssue);
        if (current.length() > 0 && BinanceGameContract.compareIssue(incoming, current) < 0) {
            return false;
        }
        String next = dataGuard.buildNextIssue(incoming);
        boolean changed = !incoming.equals(current) || !next.equals(safeText(targetIssue));
        currentIssue = incoming;
        targetIssue = next;
        if (changed) clockRevision.incrementAndGet();
        return true;
    }

    public static final class ProviderClockSnapshot {
        public boolean accepted;
        public String gameCode;
        public String currentIssue;
        public String targetIssue;
        public String providerIssue;
        public long providerOpenTime;
        public long receivedAt;
        public long localNow;
        public long nextOpenTime;
        public long countdownMs;
        public long intervalMs;
        public long ageMs;
        public long untilOpenMs;
        public String state;
        public String sourceLine;
        public String publicMessage;
        public String fingerprint;

        public ProviderClockSnapshot() {
            accepted = false;
            gameCode = "Binance1-PK10";
            currentIssue = "";
            targetIssue = "";
            providerIssue = "";
            providerOpenTime = 0L;
            receivedAt = 0L;
            localNow = 0L;
            nextOpenTime = 0L;
            countdownMs = 0L;
            intervalMs = ONE_MINUTE_MS;
            ageMs = Long.MAX_VALUE;
            untilOpenMs = 0L;
            state = "UNSYNCED";
            sourceLine = "";
            publicMessage = "";
            fingerprint = "";
        }

        public String canonical() {
            return gameCode
                    + '|'
                    + currentIssue
                    + '|'
                    + targetIssue
                    + '|'
                    + providerIssue
                    + '|'
                    + providerOpenTime
                    + '|'
                    + receivedAt
                    + '|'
                    + localNow
                    + '|'
                    + nextOpenTime
                    + '|'
                    + countdownMs
                    + '|'
                    + intervalMs
                    + '|'
                    + ageMs
                    + '|'
                    + untilOpenMs
                    + '|'
                    + state
                    + '|'
                    + sourceLine
                    + '|'
                    + fingerprint;
        }
    }

    public static final class TargetClockContract {
        public final String id;
        public final String field;
        public final String owner;
        public final boolean mandatory;
        public final String failureMode;

        public TargetClockContract(
                String id, String field, String owner, boolean mandatory, String failureMode) {
            this.id = id == null ? "" : id;
            this.field = field == null ? "" : field;
            this.owner = owner == null ? "" : owner;
            this.mandatory = mandatory;
            this.failureMode = failureMode == null ? "" : failureMode;
        }

        public String canonical() {
            return id
                    + ':'
                    + field
                    + ':'
                    + owner
                    + ':'
                    + (mandatory ? '1' : '0')
                    + ':'
                    + failureMode;
        }
    }

    public static List<TargetClockContract> b02ClockContracts() {
        ArrayList<TargetClockContract> out = new ArrayList<TargetClockContract>();
        String[] fields =
                new String[] {
                    "gameCode", "providerIssue", "currentIssue", "targetIssue",
                    "providerOpenTime", "receivedAt", "localNow", "nextOpenTime",
                    "roundIntervalMs", "countdownMs", "ageMs", "untilOpenMs",
                    "sourceLine", "freshnessState", "fingerprint", "shadowLock"
                };
        for (int i = 0; i < fields.length; i++) {
            out.add(
                    new TargetClockContract(
                            "TIANSHU-B02-CLOCK-" + (i + 1),
                            fields[i],
                            "CountdownClock",
                            true,
                            i < 12 ? "HOLD_OR_SHADOW" : "AUDIT_ONLY"));
        }
        return java.util.Collections.unmodifiableList(out);
    }
}
