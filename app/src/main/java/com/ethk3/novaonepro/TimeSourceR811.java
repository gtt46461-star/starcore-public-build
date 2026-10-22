package com.ethk3.novaonepro;

import android.os.SystemClock;

/** Explicit wall/monotonic clock separation for leases, traces and persisted epoch timestamps. */
public final class TimeSourceR811 {
    private static final long BOOT_WALL_ANCHOR = System.currentTimeMillis();
    private static final long BOOT_MONO_ANCHOR = SystemClock.elapsedRealtime();

    private static final String CLOCK_SESSION_CP12=java.util.UUID.randomUUID().toString();
    /** Neverpersisted. Restart/reboot discards every previous monotonic anchor. */
    public static String clockSessionCP12(){return CLOCK_SESSION_CP12;}
    private TimeSourceR811() {}

    public static long wallNowMs() {
        return System.currentTimeMillis();
    }

    public static long monoNowMs() {
        return SystemClock.elapsedRealtime();
    }

    public static long elapsed(long startedMonoMs) {
        return elapsedAt(startedMonoMs, monoNowMs());
    }

    public static long elapsedAt(long startedMonoMs, long endedMonoMs) {
        return Math.max(0L, Math.max(0L, endedMonoMs) - Math.max(0L, startedMonoMs));
    }

    public static Skew detectSkew() {
        long expectedWall = BOOT_WALL_ANCHOR + Math.max(0L, monoNowMs() - BOOT_MONO_ANCHOR);
        long delta = wallNowMs() - expectedWall;
        return new Skew(delta, Math.abs(delta) > AppConfig.R811_CLOCK_SKEW_THRESHOLD_MS);
    }

    public static boolean leaseExpired(long persistedWallUntilMs) {
        if (persistedWallUntilMs <= 0L) return true;
        Skew skew = detectSkew();
        return leaseExpiredAt(persistedWallUntilMs, wallNowMs(), skew.excessive);
    }

    public static boolean leaseExpiredAt(
            long persistedWallUntilMs, long currentWallMs, boolean excessiveSkew) {
        return persistedWallUntilMs <= 0L
                || excessiveSkew
                || currentWallMs >= persistedWallUntilMs;
    }

    public static final class Skew {
        public final long deltaMs;
        public final boolean excessive;

        Skew(long delta, boolean excessive) {
            deltaMs = delta;
            this.excessive = excessive;
        }
    }
}
