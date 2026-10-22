package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide owner for the complete V51 analysis chain.
 *
 * <p>A watchdog may request cooperative cancellation, but it may never create a second owner while
 * the first owner is alive. After the canonical pre-draw row is committed, cancellation is ignored
 * so a later generation cannot overwrite the frozen result.
 */
public final class AnalysisSingleFlightR35 {
    public static final String VERSION = AppConfig.R35_RUNTIME_REPAIR_VERSION;
    private static final AnalysisSingleFlightR35 INSTANCE = new AnalysisSingleFlightR35();
    private final AtomicReference<Lease> owner = new AtomicReference<Lease>();
    private final AtomicInteger active = new AtomicInteger(0);
    private final AtomicInteger maximum = new AtomicInteger(0);

    private AnalysisSingleFlightR35() {}

    public static AnalysisSingleFlightR35 shared() {
        return INSTANCE;
    }

    public Lease tryAcquire(DbStore store, String baseIssue, String targetIssue, long generation) {
        Lease candidate = new Lease(baseIssue, targetIssue, generation);
        if (!owner.compareAndSet(null, candidate)) {
            Lease current = owner.get();
            if (current != null && !safe(current.targetIssue).equals(safe(targetIssue))) {
                current.token.requestCancel("NEWER_TARGET:" + safe(targetIssue));
            }
            writeState(store, "R35_SINGLE_FLIGHT_BUSY", current, targetIssue);
            RuntimeHealthLedgerR35.record(
                    store,
                    "ANALYSIS_SINGLE_FLIGHT",
                    targetIssue,
                    "BUSY_REJECTED",
                    0L,
                    active.get(),
                    maximum.get(),
                    0,
                    "existing=" + (current == null ? "" : current.targetIssue));
            return null;
        }
        int now = active.incrementAndGet();
        updateMaximum(now);
        if (now > 1) {
            owner.compareAndSet(candidate, null);
            active.decrementAndGet();
            throw new IllegalStateException("R35_SINGLE_FLIGHT_INVARIANT_ACTIVE_" + now);
        }
        writeState(store, "R35_SINGLE_FLIGHT_ACQUIRED", candidate, targetIssue);
        RuntimeHealthLedgerR35.record(
                store,
                "ANALYSIS_SINGLE_FLIGHT",
                targetIssue,
                "ACQUIRED",
                0L,
                now,
                maximum.get(),
                0,
                "generation=" + generation);
        return candidate;
    }

    public void release(DbStore store, Lease lease, boolean success, String detail) {
        if (lease == null) return;
        if (!owner.compareAndSet(lease, null)) return;
        int now = active.decrementAndGet();
        if (now < 0) {
            active.set(0);
            now = 0;
        }
        writeState(
                store,
                success ? "R35_SINGLE_FLIGHT_RELEASED_OK" : "R35_SINGLE_FLIGHT_RELEASED_ERROR",
                lease,
                lease.targetIssue);
        RuntimeHealthLedgerR35.record(
                store,
                "ANALYSIS_SINGLE_FLIGHT",
                lease.targetIssue,
                success ? "RELEASED_OK" : "RELEASED_ERROR",
                Math.max(0L, System.currentTimeMillis() - lease.acquiredAt),
                now,
                maximum.get(),
                0,
                safe(detail));
    }

    public int activeCount() {
        return active.get();
    }

    public int maximumObserved() {
        return maximum.get();
    }

    public Lease currentLease() {
        return owner.get();
    }

    public V51RuntimeResult busyResult(String targetIssue) {
        String status = "R35_ANALYSIS_SINGLE_FLIGHT_BUSY|" + safe(targetIssue);
        return new V51RuntimeResult(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                status,
                0L,
                V51ChecksumEngine.sha256(status));
    }

    private void updateMaximum(int value) {
        while (true) {
            int old = maximum.get();
            if (value <= old || maximum.compareAndSet(old, value)) return;
        }
    }

    private static void writeState(
            DbStore store, String status, Lease lease, String requestedTarget) {
        if (store == null) return;
        try {
            store.setState("r35_single_flight_status", status);
            store.setState("r35_single_flight_active", String.valueOf(INSTANCE.active.get()));
            store.setState("r35_single_flight_max", String.valueOf(INSTANCE.maximum.get()));
            store.setState(
                    "r35_single_flight_owner_target", lease == null ? "" : lease.targetIssue);
            store.setState("r35_single_flight_requested_target", safe(requestedTarget));
            store.setState(
                    "r35_single_flight_updated_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    public static final class Lease {
        public final String baseIssue;
        public final String targetIssue;
        public final long generation;
        public final long acquiredAt;
        public final CancellationToken token;

        Lease(String base, String target, long generation) {
            this.baseIssue = safe(base);
            this.targetIssue = safe(target);
            this.generation = generation;
            this.acquiredAt = System.currentTimeMillis();
            this.token = new CancellationToken(this.targetIssue);
        }
    }

    public static final class CancellationToken {
        private final String targetIssue;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean canonicalCommitted = new AtomicBoolean(false);
        private volatile String reason = "";

        CancellationToken(String target) {
            targetIssue = safe(target);
        }

        public void requestCancel(String why) {
            reason = safe(why);
            if (!canonicalCommitted.get()) cancelled.set(true);
        }

        public void checkpoint(String stage) {
            if (cancelled.get() && !canonicalCommitted.get()) {
                throw new CancelledBeforeCommitException(targetIssue, stage, reason);
            }
        }

        public void markCanonicalCommitted() {
            canonicalCommitted.set(true);
            cancelled.set(false);
        }

        public boolean isCanonicalCommitted() {
            return canonicalCommitted.get();
        }
    }

    public static final class CancelledBeforeCommitException extends RuntimeException {
        public CancelledBeforeCommitException(String target, String stage, String reason) {
            super(
                    "R35_CANCELLED_BEFORE_COMMIT|target="
                            + safe(target)
                            + "|stage="
                            + safe(stage)
                            + "|reason="
                            + safe(reason));
        }
    }
}
