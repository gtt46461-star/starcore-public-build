package com.ethk3.novaonepro;

/**
 * R955 separates "research was posted/attempted" from "research completed a useful pulse".
 * A fresh dispatcher/yield heartbeat must never mask a stalled training/evolution pipeline.
 */
public final class BackgroundResearchProgressContractR955 {
    public static final String VERSION = "R955_RESEARCH_PROGRESS_TRUTH_V1";

    private BackgroundResearchProgressContractR955() {}

    public static final class Result {
        public final String state;
        public final String reason;
        public final long progressAgeMs;
        public final long attemptAgeMs;

        Result(String state, String reason, long progressAgeMs, long attemptAgeMs) {
            this.state = state;
            this.reason = reason;
            this.progressAgeMs = progressAgeMs;
            this.attemptAgeMs = attemptAgeMs;
        }

        public boolean stale() {
            return "STALE".equals(state);
        }

        public String canonical() {
            return VERSION
                    + "|state=" + state
                    + "|reason=" + reason
                    + "|progressAge=" + age(progressAgeMs)
                    + "|attemptAge=" + age(attemptAgeMs);
        }
    }

    public static Result evaluate(
            long now,
            long serviceStartAt,
            long serviceReadyAt,
            long progressAt,
            long attemptAt,
            String attemptState,
            long staleMs,
            long startGraceMs) {
        long safeNow = Math.max(0L, now);
        long anchor = Math.max(serviceStartAt, serviceReadyAt);
        long progressAge = age(safeNow, progressAt);
        long attemptAge = age(safeNow, attemptAt);
        long safeStale = Math.max(1L, staleMs);
        long safeGrace = Math.max(0L, startGraceMs);
        String attempt = safe(attemptState).toUpperCase(java.util.Locale.US);

        if (anchor <= 0L) {
            return new Result("WAITING", "SERVICE_NOT_READY", progressAge, attemptAge);
        }
        long serviceAge = Math.max(0L, safeNow - anchor);
        if (serviceAge < safeGrace) {
            return new Result("WAITING", "START_GRACE", progressAge, attemptAge);
        }

        // On a restart, a pre-restart success is not proof that this process can still make
        // progress.  Give the new process one normal stale window to produce its first success.
        long effectiveProgressAt = progressAt > anchor ? progressAt : anchor;
        long effectiveProgressAge = Math.max(0L, safeNow - effectiveProgressAt);
        if (progressAt > anchor && effectiveProgressAge <= safeStale) {
            return new Result("OK", "PROGRESS_FRESH", effectiveProgressAge, attemptAge);
        }
        if (progressAt <= anchor && effectiveProgressAge <= safeStale) {
            return new Result("WAITING", "FIRST_COMPLETION_WINDOW", effectiveProgressAge, attemptAge);
        }

        String reason;
        if (attempt.indexOf("ERROR") >= 0 || attempt.indexOf("FAIL") >= 0) {
            reason = "RESEARCH_PROGRESS_STALE_AFTER_ERROR";
        } else if (attempt.indexOf("YIELD") >= 0) {
            reason = "RESEARCH_PROGRESS_STALE_AFTER_YIELD";
        } else if (attempt.indexOf("POSTED") >= 0 || attempt.indexOf("PENDING") >= 0) {
            reason = "RESEARCH_PROGRESS_STALE_AFTER_ATTEMPT";
        } else if (attemptAt <= 0L) {
            reason = "RESEARCH_NEVER_ATTEMPTED";
        } else {
            reason = "RESEARCH_PROGRESS_STALE";
        }
        return new Result("STALE", reason, effectiveProgressAge, attemptAge);
    }

    private static long age(long now, long at) {
        return at <= 0L ? Long.MAX_VALUE : Math.max(0L, now - at);
    }

    private static String age(long value) {
        return value == Long.MAX_VALUE ? "NA" : String.valueOf(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
