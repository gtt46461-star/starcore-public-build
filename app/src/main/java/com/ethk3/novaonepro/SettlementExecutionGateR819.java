package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Process-wide settlement single-flight gate.
 *
 * <p>The R815 coordinator used a {@link ThreadLocal}; that proved call provenance but did not stop
 * fetch, watchdog and maintenance HandlerThreads from claiming different settlement rows at the
 * same time.  SQLite then became the de-facto lock and durable tasks remained RUNNING until their
 * leases expired.  This gate is deliberately process-local and non-blocking: durable tasks remain
 * the retry source of truth and callers yield instead of waiting while holding another runtime
 * resource.
 */
public final class SettlementExecutionGateR819 {
    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    private static final AtomicLong OWNER_THREAD = new AtomicLong(0L);
    private static final AtomicReference<String> OWNER_KIND =
            new AtomicReference<String>("");

    private SettlementExecutionGateR819() {}

    public static boolean tryEnter() {
        return acquire("EXECUTION");
    }

    /** Owns the identical fence while orphan RUNNING leases are repaired. */
    public static boolean tryEnterRecovery() {
        return acquire("RECOVERY");
    }

    private static boolean acquire(String kind) {
        if (!ACTIVE.compareAndSet(false, true)) return false;
        OWNER_THREAD.set(Thread.currentThread().getId());
        OWNER_KIND.set(kind == null ? "" : kind);
        return true;
    }

    public static void exit() {
        release("EXECUTION");
    }

    public static void exitRecovery() {
        release("RECOVERY");
    }

    private static void release(String expectedKind) {
        long owner = OWNER_THREAD.get();
        if (owner != Thread.currentThread().getId())
            throw new IllegalStateException("R819_SETTLEMENT_GATE_OWNER_MISMATCH");
        String actualKind = OWNER_KIND.get();
        if (!expectedKind.equals(actualKind))
            throw new IllegalStateException(
                    "R914_SETTLEMENT_GATE_KIND_MISMATCH:"
                            + expectedKind
                            + ":"
                            + actualKind);
        OWNER_KIND.set("");
        OWNER_THREAD.set(0L);
        ACTIVE.set(false);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    public static String ownerKind() {
        return OWNER_KIND.get();
    }

    static void resetForTests() {
        OWNER_KIND.set("");
        OWNER_THREAD.set(0L);
        ACTIVE.set(false);
    }
}
