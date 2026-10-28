package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;

/** R27-006/007 real wait reasons replacing the blanket DATA_NOT_READY (docs/02 2.3), plus stuck / never-run /
 * quarantine rules. Pure Java so the classification is host-tested. */
public final class WaitKindR27 {
    private WaitKindR27() {}
    public static final String PROGRESS = "PROGRESS", NO_NEW_EVENT = "NO_NEW_EVENT", DEPENDENCY = "DEPENDENCY", DEADLINE_MISSED = "DEADLINE_MISSED",
            RESOURCE = "RESOURCE", ERROR = "ERROR", DISABLED_BY_USER = "DISABLED_BY_USER", QUARANTINED = "QUARANTINED";
    public static final long STUCK_MS = 30 * 60_000L, NEVER_RUN_MS = 60 * 60_000L, ERROR_WINDOW_MS = 10 * 60_000L;
    public static final int MAX_ERRORS_PER_WINDOW = 5;

    public static final class Wait {
        public final String kind, detail;
        Wait(String kind, String detail) { this.kind = kind; this.detail = detail == null ? "" : detail; }
        /** Value stored in r26_task.blocked_reason for compatibility with the R26 report. */
        public String blockedReason() { return kind.equals(PROGRESS) ? null : kind + (detail.isEmpty() ? "" : ":" + detail); }
        @Override public String toString() { return kind + (detail.isEmpty() ? "" : "|" + detail); }
    }

    /** Maps one slice outcome to a wait kind.
     * @param delta    progress units produced by the slice
     * @param reason   the task's own reason string (may be null)
     * @param failure  thrown exception, if any
     * @param cursor   task cursor before the slice; latest = newest event ordinal it could consume */
    public static Wait classify(long delta, String reason, Throwable failure, long cursor, long latest, String resource, boolean userDisabled) {
        if (userDisabled) return new Wait(DISABLED_BY_USER, "");
        if (resource != null) return new Wait(RESOURCE, resource.startsWith("RESOURCE(") ? resource : "RESOURCE(" + resource + ")");
        if (failure != null) {
            String msg = String.valueOf(failure.getMessage());
            return new Wait(ERROR, "ERROR(" + failure.getClass().getSimpleName() + ":" + (msg.length() > 160 ? msg.substring(0, 160) : msg) + ")");
        }
        if (delta > 0) return new Wait(PROGRESS, (reason != null && reason.startsWith("COOPERATIVE") ? reason + ";" : "") + "units=" + delta);
        if (reason != null) {
            String r = reason;
            if (r.startsWith("WAITING_") || r.contains("SCHEMA_PENDING") || r.contains("MISSING_TEACHER") || r.startsWith("DEPENDENCY")) return new Wait(DEPENDENCY, "DEPENDENCY(" + r + ")");
            if (r.contains("AFTER_OPEN") || r.contains("DEADLINE")) return new Wait(DEADLINE_MISSED, r);
            if (r.contains("ALREADY") || r.contains("SUPERSEDED") || r.startsWith("NO_NEW")) return new Wait(NO_NEW_EVENT, r + ";cursor=" + cursor + ",latest=" + latest);
            if (r.startsWith("COOPERATIVE") || r.equals("SLICE_BUDGET")) return new Wait(PROGRESS, r);
            return new Wait(DEPENDENCY, "DEPENDENCY(" + r + ")");
        }
        if (latest >= 0 && cursor >= latest) return new Wait(NO_NEW_EVENT, "cursor=" + cursor + ",latest=" + latest);
        return new Wait(NO_NEW_EVENT, "NO_OUTPUT;cursor=" + cursor + ",latest=" + latest);
    }

    /** Health of a task row for the report/UI alarms. */
    public static final class Health { public final String taskId; public final boolean stuck, neverRun, quarantined; public final String detail;
        Health(String id, boolean s, boolean n, boolean q, String d) { taskId = id; stuck = s; neverRun = n; quarantined = q; detail = d; } }

    public static Health health(String taskId, String waitKind, String waitDetail, long waitSince, long registeredAt, long cursor, long lastProgress, long now) {
        boolean bad = waitKind != null && !waitKind.isEmpty() && !waitKind.equals(PROGRESS) && !waitKind.equals(NO_NEW_EVENT) && !waitKind.equals(DISABLED_BY_USER);
        boolean stuck = bad && waitSince > 0 && now - waitSince >= STUCK_MS;
        boolean never = cursor == 0 && lastProgress == 0 && registeredAt > 0 && now - registeredAt >= NEVER_RUN_MS;
        return new Health(taskId, stuck, never, QUARANTINED.equals(waitKind), waitKind + ":" + waitDetail);
    }

    /** Error rate limiter: returns the error count in the current window after recording this error; at more than
     * five errors in ten minutes the task is QUARANTINED (manual retry from the UI resets it). */
    public static long[] recordError(long windowStart, long count, long now) {
        if (windowStart == 0 || now - windowStart > ERROR_WINDOW_MS) return new long[] {now, 1};
        return new long[] {windowStart, count + 1};
    }
    public static boolean quarantine(long count) { return count > MAX_ERRORS_PER_WINDOW; }

    public static List<String> kinds() { List<String> l = new ArrayList<>(); for (String k : new String[] {PROGRESS, NO_NEW_EVENT, DEPENDENCY, DEADLINE_MISSED, RESOURCE, ERROR, DISABLED_BY_USER, QUARANTINED}) l.add(k); return l; }
}
