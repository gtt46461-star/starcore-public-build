package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;
import java.util.List;

/** R27-004 research write queue (docs/02 2.2). Research threads compute in memory on read-only connections and
 * submit writes here. Writes go through the single process writer (DatabaseCoordinator) in chunks of at most 500
 * statements; a chunk that took longer than 20 ms halves the next chunk. Research yields only in millisecond write
 * windows: while P0 settlement/analysis is writing, or within 3 s of the next freeze deadline, a submit waits at
 * most 200 ms, then is re-queued behind the caller's other work (counted per hour in r27_write_wait). */
public final class ResearchWriteQueueR27 {
    public static final int MAX_ROWS = 500; public static final long MAX_CHUNK_MS = 20, MAX_WAIT_MS = 200, DEADLINE_GUARD_MS = 3000;
    private final DbStore store; private volatile long nextDeadline;
    private long hourStart, submits, longWaits, maxWait, requeues;

    public ResearchWriteQueueR27(DbStore store) { this.store = store; }

    /** One prepared statement and its bind arguments. */
    public static final class Op { final String sql; final Object[] args; public Op(String sql, Object... args) { this.sql = sql; this.args = args; } }
    /** A batch built in memory; optional callback runs first inside the same transaction (fence checks, CAS). */
    public static final class Batch {
        public final List<Op> ops = new ArrayList<>(); public DatabaseCoordinator.WriteCallable<Integer> before;
        public Batch add(String sql, Object... args) { ops.add(new Op(sql, args)); return this; }
        public boolean isEmpty() { return ops.isEmpty() && before == null; }
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
        long start = System.currentTimeMillis(), waited = 0;
        while (p0Writing() || (!freezeWrite && freezeWindow(System.currentTimeMillis()))) {
            if (cancelled.getAsBoolean()) return false;
            if (waited >= MAX_WAIT_MS) { stat(waited, true); return false; }
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            waited = System.currentTimeMillis() - start;
        }
        int chunk = MAX_ROWS, i = 0; boolean first = true;
        while (i < batch.ops.size() || first) {
            final int from = i, to = Math.min(batch.ops.size(), i + chunk); final boolean runBefore = first;
            long t0 = System.nanoTime();
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_" + operation, db -> {
                if (runBefore && batch.before != null) { Integer ok = batch.before.call(db); if (ok != null && ok < 0) throw new IllegalStateException("R27_WRITE_PRECONDITION:" + operation); }
                for (int k = from; k < to; k++) { Op op = batch.ops.get(k); db.execSQL(op.sql, op.args); }
                return 0;
            });
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            if (ms > MAX_CHUNK_MS && chunk > 25) chunk /= 2;
            i = to; first = false;
            if (i < batch.ops.size() && (p0Writing() || freezeWindow(System.currentTimeMillis()))) {
                // Remaining chunks are independent idempotent statements; let P0 pass, then continue.
                long w0 = System.currentTimeMillis();
                while ((p0Writing() || freezeWindow(System.currentTimeMillis())) && System.currentTimeMillis() - w0 < MAX_WAIT_MS) {
                    try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        stat(waited, false);
        return true;
    }

    private synchronized void stat(long waited, boolean requeued) {
        long hour = System.currentTimeMillis() / 3_600_000L * 3_600_000L;
        if (hour != hourStart) { flush(); hourStart = hour; submits = longWaits = maxWait = requeues = 0; }
        submits++; if (waited >= MAX_WAIT_MS) longWaits++; maxWait = Math.max(maxWait, waited); if (requeued) requeues++;
    }
    /** Persists the hourly write-wait counters (called by the health task). */
    public synchronized void flush() {
        if (hourStart == 0) return;
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
