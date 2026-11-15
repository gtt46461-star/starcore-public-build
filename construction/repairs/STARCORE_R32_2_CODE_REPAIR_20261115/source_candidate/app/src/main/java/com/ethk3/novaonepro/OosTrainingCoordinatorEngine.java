package com.ethk3.novaonepro;

/**
 * V40 Top-K OOS background training coordinator.
 *
 * <p>It never calculates probability and never creates BET. It only guarantees that the strict
 * walk-forward publisher receives a bounded protected slice after the current target decision
 * exists. A running slice yields only when a real fetch/analysis task actually starts; countdown
 * changes alone cannot erase progress.
 */
public final class OosTrainingCoordinatorEngine {
    public static final String MODULE_VERSION = AppConfig.OOS_TRAINING_COORDINATOR_VERSION;
    public static final String STATE_LAST_PROGRESS_AT = "v26_oos_last_progress_at";
    public static final String STATE_LAST_SAMPLE = "v26_oos_last_sample";
    public static final String STATE_LAST_START_AT = "v26_oos_last_start_at";
    public static final String STATE_LAST_RESULT = "v26_oos_last_result";
    public static final String STATE_STARVATION_COUNT = "v26_oos_starvation_count";

    private OosTrainingCoordinatorEngine() {}

    public static boolean mayStart(
            DbStore store,
            boolean running,
            boolean analysisBusy,
            boolean fetchBusy,
            boolean backfillBusy,
            boolean realtimeNeed,
            long countdownMs,
            boolean urgent) {
        if (!running || analysisBusy || fetchBusy || backfillBusy || realtimeNeed) return false;
        if (countdownMs <= 0L) return true;
        if (countdownMs >= AppConfig.MICRO_WALK_FORWARD_START_GUARD_MS) return true;
        return urgent
                && isStarved(store)
                && countdownMs >= Math.min(3500L, AppConfig.MICRO_WALK_FORWARD_START_GUARD_MS);
    }

    /** Once a protected slice starts, only a real realtime task may interrupt it. */
    public static boolean shouldYieldInside(
            boolean running, boolean analysisBusy, boolean fetchBusy, boolean backfillBusy) {
        return !running || analysisBusy || fetchBusy || backfillBusy;
    }

    public static boolean isStarved(DbStore store) {
        if (store == null) return true;
        long last = parseLong(store.getState(STATE_LAST_PROGRESS_AT, "0"), 0L);
        return last <= 0L
                || System.currentTimeMillis() - last >= AppConfig.MICRO_WALK_FORWARD_STARVATION_MS;
    }

    public static void noteStart(DbStore store) {
        if (store == null) return;
        try {
            store.setState(STATE_LAST_START_AT, String.valueOf(System.currentTimeMillis()));
            store.setState(STATE_LAST_RESULT, "RUNNING|" + MODULE_VERSION);
        } catch (Throwable ignore) {
        }
    }

    public static void noteResult(
            DbStore store, int beforeSample, MicroWalkForwardEngine.Report report) {
        if (store == null) return;
        int after = report == null ? beforeSample : report.totalPublishedSample;
        try {
            store.setState(STATE_LAST_SAMPLE, String.valueOf(after));
            store.setState(STATE_LAST_RESULT, report == null ? "NO_REPORT" : report.summary());
            if (after > beforeSample) {
                store.setState(STATE_LAST_PROGRESS_AT, String.valueOf(System.currentTimeMillis()));
                store.setState(STATE_STARVATION_COUNT, "0");
            } else {
                int n = parseInt(store.getState(STATE_STARVATION_COUNT, "0"), 0) + 1;
                store.setState(STATE_STARVATION_COUNT, String.valueOf(n));
            }
        } catch (Throwable ignore) {
        }
    }

    private static int parseInt(String s, int d) {
        try {
            return Integer.parseInt(s);
        } catch (Throwable e) {
            return d;
        }
    }

    private static long parseLong(String s, long d) {
        try {
            return Long.parseLong(s);
        } catch (Throwable e) {
            return d;
        }
    }
}
