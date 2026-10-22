package com.ethk3.novaonepro;

/**
 * Pure classifier for foreground-service startup evidence.
 *
 * <p>It prevents a foreground-entered process from being reported as "never started" merely
 * because the database/engine graph has not reached business-ready yet.
 */
public final class ServiceStartupStateR363 {
    public enum Phase {
        STOPPED_BY_USER,
        NEVER_REQUESTED,
        INITIALIZING,
        INIT_STALLED,
        INIT_FAILED,
        READY
    }

    public static final class Evidence {
        public final boolean userEnabled;
        public final long now,
                serviceStartingAt,
                lifecycleStartAt,
                foregroundAt,
                onStartCommandAt,
                readyAt;
        public final String initStatus, initStage, initError;

        public Evidence(
                boolean userEnabled,
                long now,
                long serviceStartingAt,
                long lifecycleStartAt,
                long foregroundAt,
                long onStartCommandAt,
                long readyAt,
                String initStatus,
                String initStage,
                String initError) {
            this.userEnabled = userEnabled;
            this.now = Math.max(0L, now);
            this.serviceStartingAt = Math.max(0L, serviceStartingAt);
            this.lifecycleStartAt = Math.max(0L, lifecycleStartAt);
            this.foregroundAt = Math.max(0L, foregroundAt);
            this.onStartCommandAt = Math.max(0L, onStartCommandAt);
            this.readyAt = Math.max(0L, readyAt);
            this.initStatus = safe(initStatus);
            this.initStage = safe(initStage);
            this.initError = safe(initError);
        }

        public long enteredAt() {
            return max(serviceStartingAt, lifecycleStartAt, foregroundAt, onStartCommandAt);
        }
    }

    private ServiceStartupStateR363() {}

    public static Phase classify(Evidence e, long graceMs) {
        if (e == null) return Phase.NEVER_REQUESTED;
        if (!e.userEnabled) return Phase.STOPPED_BY_USER;
        if ("FAILED".equals(e.initStatus)) return Phase.INIT_FAILED;
        if (e.readyAt > 0L && "READY".equals(e.initStatus)) return Phase.READY;
        long entered = e.enteredAt();
        if (entered <= 0L) return Phase.NEVER_REQUESTED;
        long age = Math.max(0L, e.now - entered);
        return age <= Math.max(1L, graceMs) ? Phase.INITIALIZING : Phase.INIT_STALLED;
    }

    public static String preReadyStaleReason(Evidence e, long graceMs) {
        Phase phase = classify(e, graceMs);
        if (phase == Phase.INITIALIZING || phase == Phase.READY || phase == Phase.STOPPED_BY_USER)
            return "";
        if (phase == Phase.NEVER_REQUESTED) return "SERVICE_NEVER_REQUESTED";
        String stage = e == null || e.initStage.length() == 0 ? "UNKNOWN" : e.initStage;
        if (phase == Phase.INIT_FAILED)
            return "SERVICE_INIT_FAILED["
                    + stage
                    + ']'
                    + (e.initError.length() == 0 ? "" : ":" + e.initError);
        return "SERVICE_INIT_STALLED[" + stage + ']';
    }

    public static String truthState(Evidence e, long graceMs) {
        Phase phase = classify(e, graceMs);
        switch (phase) {
            case STOPPED_BY_USER:
                return "STOPPED_BY_USER";
            case NEVER_REQUESTED:
                return "NOT_REQUESTED";
            case INITIALIZING:
                return "STARTING";
            case INIT_STALLED:
                return "INIT_STALLED";
            case INIT_FAILED:
                return "INIT_FAILED";
            case READY:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }

    private static long max(long a, long b, long c, long d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
