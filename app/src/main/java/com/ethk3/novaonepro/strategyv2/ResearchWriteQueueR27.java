package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.List;

/** Research batch boundary. A batch's CAS, outputs and cursor are ONE transaction, including batches over 500
 * statements. Callers must prepare bounded slices before submitting; splitting a logical batch inside this queue
 * exposed partial model state and let a retry apply non-idempotent increments twice. R29-005's independent database
 * cutover is still pending: this correctness fix deliberately retains the existing writer and its pressure guard. */
public final class ResearchWriteQueueR27 {
    public static final int MAX_ROWS = 500; public static final long MAX_CHUNK_MS = 20, MAX_WAIT_MS = 200, DEADLINE_GUARD_MS = 3000;
    private final DbStore store; private volatile long nextDeadline;
    interface AtomicWriter { void write(String operation, Batch batch); }
    private final AtomicWriter writer;
    private final java.util.function.BooleanSupplier critical;
    private long hourStart, submits, longWaits, maxWait, requeues;

    public ResearchWriteQueueR27(DbStore store) {
        this.store = store; this.critical = ResearchWriteQueueR27::p0Writing;
        this.writer = (operation, batch) -> store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                "R27_" + operation, db -> { execute(db, batch, operation); return 0; });
    }
    // Same production submit path with a transactional test sink, without an Android process/DbStore.
    ResearchWriteQueueR27(AtomicWriter writer, java.util.function.BooleanSupplier critical) {
        this.store = null; this.writer = writer; this.critical = critical;
    }

    /** One prepared statement and its bind arguments. */
    public static final class Op { final String sql; final Object[] args; public Op(String sql, Object... args) { this.sql = sql; this.args = args; } }
    /** A batch built in memory; optional callback runs first inside the same transaction (fence checks, CAS). */
    public static final class Batch {
        public final List<Op> ops = new ArrayList<>(); public DatabaseCoordinator.WriteCallable<Integer> before, after;
        public Batch add(String sql, Object... args) { ops.add(new Op(sql, args)); return this; }
        public boolean isEmpty() { return ops.isEmpty() && before == null && after == null; }
    }

    public void setNextDeadline(long wallMs) { nextDeadline = wallMs; }
    public long nextDeadline() { return nextDeadline; }
    public boolean freezeWindow(long now) { long d = nextDeadline; return d > 0 && now < d && d - now < DEADLINE_GUARD_MS; }
    public static boolean p0Writing() { return SettlementExecutionGateR819.isActive() || RealtimePressureGateR811.hasActiveCriticalWork(); }

    /** Blocks the calling research thread for at most 200 ms per attempt; returns false if it had to re-queue
     * (the caller keeps the batch and retries on its next slice). */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled) { return submit(operation, batch, cancelled, false); }

    /** @param freezeWrite the R27 live freeze itself: exempt from the deadline guard (it is the work the guard protects),
     *                     still yields to P0 settlement for at most 200 ms. */
    public boolean submit(String operation, Batch batch, java.util.function.BooleanSupplier cancelled, boolean freezeWrite) {
        if (batch.isEmpty()) return true;
        long start = System.nanoTime(), waited = 0;
        while (critical.getAsBoolean() || (!freezeWrite && freezeWindow(System.currentTimeMillis()))) {
            if (cancelled.getAsBoolean()) return false;
            if (waited >= MAX_WAIT_MS) { stat(waited, true); return false; }
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            waited = (System.nanoTime() - start) / 1_000_000L;
        }
        if (cancelled.getAsBoolean()) return false;
        try {
            writer.write(operation, batch);
        } catch (IllegalStateException failure) {
            // A retry is safe only because the entire writer transaction rolled back.
            if (!writerBusy(failure)) throw failure;
            stat((System.nanoTime() - start) / 1_000_000L, true); return false;
        }
        stat(waited, false);
        return true;
    }

    static void execute(SQLiteDatabase db, Batch batch, String operation) throws Exception {
        if (!db.inTransaction()) throw new IllegalStateException("R29_BATCH_TRANSACTION_REQUIRED");
        if (batch.before != null) require(batch.before.call(db), operation);
        exec(db, batch);
        if (batch.after != null) require(batch.after.call(db), operation);
    }
    private static void require(Integer ok, String operation) {
        if (ok != null && ok < 0) throw new IllegalStateException("R27_WRITE_PRECONDITION:" + operation);
    }

    /** True when the failure is the single writer being busy (deadline/interrupt), as opposed to a real write error. */
    static boolean writerBusy(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            String m = String.valueOf(t.getMessage());
            if (m.contains("CP3_DB_BUSY_") || t instanceof InterruptedException || t instanceof android.database.sqlite.SQLiteDatabaseLockedException) return true;
            if (t.getCause() == t) break;
        }
        return false;
    }

    private synchronized void stat(long waited, boolean requeued) {
        long hour = System.currentTimeMillis() / 3_600_000L * 3_600_000L;
        if (hour != hourStart) { flush(); hourStart = hour; submits = longWaits = maxWait = requeues = 0; }
        submits++; if (waited >= MAX_WAIT_MS) longWaits++; maxWait = Math.max(maxWait, waited); if (requeued) requeues++;
    }
    /** Persists the hourly write-wait counters (called by the health task). */
    public synchronized void flush() {
        if (hourStart == 0 || store == null) return;
        final long h = hourStart, s = submits, w = longWaits, m = maxWait, r = requeues;
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_WRITE_WAIT", db -> {
                // minSdk 21: no UPSERT (SQLite < 3.24 on old devices).
                db.execSQL("INSERT OR IGNORE INTO r27_write_wait(hour_start) VALUES(?)", new Object[] {h});
                db.execSQL("UPDATE r27_write_wait SET submits=?,waits_over_200ms=?,max_wait_ms=max(max_wait_ms,?),requeues=? WHERE hour_start=?", new Object[] {s, w, m, r, h});
                return 0;
            });
        } catch (RuntimeException busy) { android.util.Log.w("STARCORE_R27", "write-wait receipt deferred", busy); }
    }

    static void exec(SQLiteDatabase db, Batch b) { for (Op op : b.ops) db.execSQL(op.sql, op.args); }
}
