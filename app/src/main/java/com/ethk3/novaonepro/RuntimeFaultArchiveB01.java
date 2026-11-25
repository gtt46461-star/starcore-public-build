package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** B01/B02 wake-up boundary. The maintenance owner supplies the verified physical archive. */
public final class RuntimeFaultArchiveB01 {
    private static final String TABLE = "r31_runtime_fault";
    private static final String DEBT = "DEPENDENCY(WRITER_HOLD_FAULT_PROTECTED_CAPACITY_DEBT)";
    private static final long PERIODIC_MS = 60_000L;
    private static final AtomicLong REQUESTED = new AtomicLong();
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static volatile View view = new View(0, 0, 0, 0, State.STARTUP_SCAN, 0);
    private RuntimeFaultArchiveB01() {}

    public static boolean isProtectedDebt(Throwable error) {
        for (int depth = 0; error != null && depth < 8; depth++) {
            if (error instanceof IllegalStateException && DEBT.equals(error.getMessage())) return true;
            Throwable next = error.getCause();
            if (next == error) break;
            error = next;
        }
        return false;
    }

    /** Called on the fault drain path. Do not touch store, lastError, DB, exporter, files or scheduling here. */
    public static void request(DbStore store, String sourceTable, String lastError) {
        if (store != null && TABLE.equals(sourceTable)) REQUESTED.incrementAndGet();
    }

    public enum State {
        STARTUP_SCAN, NO_ELIGIBLE_VERIFIED, VERIFIED_RETIRED, RETRY_STALE,
        BLOCKED_PROTECTED, BLOCKED_BUDGET, WAIT_READER, WAIT_VERIFIED, IO_ERROR,
        ADAPTER_MISSING, YIELDED
    }

    public static final class Page {
        public final State state;
        public final int retired;
        private Page(State state, int retired) { this.state = state; this.retired = retired; }
        public static Page retain(State reason) {
            if (reason == null || reason == State.STARTUP_SCAN || reason == State.VERIFIED_RETIRED)
                throw new IllegalArgumentException("archive retain state");
            return new Page(reason, 0);
        }
        /** Call only after committing the typed verified receipt and reading it back by identity. */
        public static Page committed(int readBackRemovedRows) {
            if (readBackRemovedRows < 1 || readBackRemovedRows > 64)
                throw new IllegalArgumentException("archive receipt row bound");
            return new Page(State.VERIFIED_RETIRED, readBackRemovedRows);
        }
    }

    /** Called on the existing background maintenance turn, never while its caller owns a SQL writer. */
    public interface Worker { Page runOne(DbStore store, long deadlineElapsedMs); }

    public static final class View {
        public final long acknowledged, observedAtElapsedMs, nextAttemptElapsedMs;
        public final int streak, retiredThisPage;
        public final State state;
        private View(long acknowledged, long observed, long next, int streak, State state, int retired) {
            this.acknowledged = acknowledged; this.observedAtElapsedMs = observed;
            this.nextAttemptElapsedMs = next; this.streak = streak;
            this.state = state; this.retiredThisPage = retired;
        }
    }
    public static View observation() { return view; }
    public static long requestedGeneration() { return REQUESTED.get(); }

    /** now and deadline use elapsedRealtime in this process. No durable cursor or DB success is implied by a signal. */
    public static boolean step(DbStore store, long now, long deadline, Worker worker) {
        if (store == null || now < 0 || deadline <= now || !RUNNING.compareAndSet(false, true)) return false;
        try {
            View before = view;
            long sampled = REQUESTED.get();
            boolean newWakeAfterIdle = sampled != before.acknowledged && before.state == State.NO_ELIGIBLE_VERIFIED;
            if (now < before.nextAttemptElapsedMs && !newWakeAfterIdle) return false;
            Page page;
            try {
                page = worker == null ? Page.retain(State.ADAPTER_MISSING) : worker.runOne(store, deadline);
                if (page == null) page = Page.retain(State.WAIT_VERIFIED);
                if (page.state == State.VERIFIED_RETIRED
                        && com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31.deferResearch()) {
                    int retired = page.retired;
                    for (int slice = 1; slice < 4 && android.os.SystemClock.elapsedRealtime() < deadline; slice++) {
                        Page more = worker.runOne(store, deadline);
                        if (more == null || more.state != State.VERIFIED_RETIRED) break;
                        retired += more.retired;
                    }
                    page = Page.committed(retired);
                }
            } catch (RuntimeException failure) {
                Error fatal = com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);
                if (fatal != null) throw fatal;
                page = Page.retain(State.IO_ERROR);
            }
            boolean idle = page.state == State.NO_ELIGIBLE_VERIFIED;
            boolean progressed = page.state == State.VERIFIED_RETIRED;
            int streak = idle || progressed ? 0 : Math.min(before.streak + 1, 7);
            long retry = idle ? PERIODIC_MS : progressed ? 1_000L : Math.min(PERIODIC_MS, 1_000L << (streak - 1));
            long next = now <= Long.MAX_VALUE - retry ? now + retry : Long.MAX_VALUE;
            view = new View(idle ? sampled : before.acknowledged, now, next, streak, page.state, page.retired);
            return true;
        } finally { RUNNING.set(false); }
    }
}
