package com.ethk3.novaonepro;

/**
 * Dependency-free durable analysis lease policy.
 *
 * <p>The wall-clock request deadline bounds one execution generation, while draw presence and the
 * canonical first-write row remain the two correctness boundaries. R947 may refresh a stale
 * pre-draw generation before claim when no live owner exists; it never extends a request after the
 * target draw. This class is deliberately pure so every ownership transition can be replayed on
 * the host before an APK is built.
 */
public final class R36AnalysisLeasePolicyR365 {
    /** Must track the running repair so older terminal rows are rearmed exactly once on upgrade. */
    public static final String REPAIR_VERSION = AppConfig.R36_RUNTIME_REPAIR_VERSION;

    public enum Decision {
        CLAIM_AVAILABLE,
        CLAIM_STALE_TAKEOVER,
        RESUME_SAME_OWNER,
        DEFER_ACTIVE_OWNER,
        NOT_DUE,
        CANONICAL_ALREADY,
        TARGET_ALREADY_DRAWN,
        TERMINAL,
        STALE_REQUEST,
        INVALID_STATUS
    }

    private R36AnalysisLeasePolicyR365() {}

    public static Decision decide(
            String status,
            long rowGeneration,
            String rowRequestHash,
            long requestGeneration,
            String requestHash,
            String rowOwner,
            String requestedOwner,
            long leaseUntil,
            long nextAttemptAt,
            long now,
            boolean targetDrawn,
            boolean canonicalExists) {
        if (canonicalExists || AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(safe(status)))
            return Decision.CANONICAL_ALREADY;
        if (targetDrawn) return Decision.TARGET_ALREADY_DRAWN;
        if (rowGeneration != requestGeneration || !safe(rowRequestHash).equals(safe(requestHash)))
            return Decision.STALE_REQUEST;

        String s = safe(status);
        if (AnalysisRequestStoreR36.REQUESTED.equals(s)
                || AnalysisRequestStoreR36.RETRY_WAIT.equals(s)) {
            return nextAttemptAt > now ? Decision.NOT_DUE : Decision.CLAIM_AVAILABLE;
        }
        if (AnalysisRequestStoreR36.RUNNING.equals(s)) {
            if (safe(rowOwner).length() > 0 && safe(rowOwner).equals(safe(requestedOwner)))
                return Decision.RESUME_SAME_OWNER;
            return leaseUntil > now
                    ? Decision.DEFER_ACTIVE_OWNER
                    : Decision.CLAIM_STALE_TAKEOVER;
        }
        if (AnalysisRequestStoreR36.EXPIRED.equals(s)
                || AnalysisRequestStoreR36.FAILED_TERMINAL.equals(s)) return Decision.TERMINAL;
        return Decision.INVALID_STATUS;
    }

    public static boolean shouldRefreshPredrawDeadline(
            String status, long deadlineAt, long leaseUntil, long now, long minimumRemainingMs,
            boolean targetDrawn, boolean canonicalExists, boolean liveInProcessOwner) {
        if (targetDrawn || canonicalExists || liveInProcessOwner || deadlineAt <= 0L) return false;
        if (deadlineAt - now >= Math.max(1L, minimumRemainingMs)) return false;
        String s = safe(status);
        if (AnalysisRequestStoreR36.REQUESTED.equals(s)
                || AnalysisRequestStoreR36.RETRY_WAIT.equals(s)) return true;
        return AnalysisRequestStoreR36.RUNNING.equals(s) && leaseUntil <= now;
    }

    public static long retryDelayMs(long attempts) {
        long exponent = Math.max(0L, Math.min(5L, attempts - 1L));
        return Math.min(30000L, 1000L << (int) exponent);
    }

    /** One controlled overwrite-install bridge; canonical and post-draw rows are immutable. */
    public static boolean canUpgradeRearm(
            String status,
            String repairVersion,
            boolean targetDrawn,
            boolean canonicalExists,
            boolean alreadyRearmed) {
        if (targetDrawn || canonicalExists || alreadyRearmed) return false;
        if (REPAIR_VERSION.equals(safe(repairVersion))) return false;
        String s = safe(status);
        return AnalysisRequestStoreR36.EXPIRED.equals(s)
                || AnalysisRequestStoreR36.FAILED_TERMINAL.equals(s);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
