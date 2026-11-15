package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Unique persistence owner for R926 runtime events produced outside an already-open business
 * transaction.
 *
 * <p>Business owners such as canonical commit, settlement and FULL evidence keep using
 * {@link RuntimeEventLedgerR926#appendWithinTransaction} so their event evidence shares the exact
 * same transaction as the business row.  Every other producer must enter here: a PENDING outbox
 * row is committed first, then that exact outbox row is drained into immutable event + issue projection in
 * one local SQLite transaction.  A process death after the handoff but before drain therefore
 * leaves durable replay work for {@link RuntimeEventLedgerR926#reconcileStartupOnce}.</p>
 */
public final class RuntimeExternalEventWriterR926 {
    private RuntimeExternalEventWriterR926() {}

    /**
     * Bounded retry for durable handoffs left pending after process startup. The service's
     * existing serial recovery Handler invokes this; no new executor or business writer exists.
     * A busy writer or live owner leaves the handoff durable for the next pulse.
     */
    public static int recoverPendingWhenIdleR81(final DbStore store) {
        if (store == null || Thread.currentThread().isInterrupted()
                || RealtimePressureGateR811.hasActiveCriticalWork()) return 0;
        final DatabaseCoordinator coordinator = store.coordinatorR36();
        if (coordinator == null) return 0;
        final long started = TimeSourceR811.monoNowMs();
        try (DatabaseCoordinator.WriterDeadlineScopeCP3 scope =
                     DatabaseCoordinator.withWriterDeadlineCP3(started + 2L)) {
            return coordinator.runWrite("R926_TELEMETRY_EVENT_OUTBOX_RECOVERY",
                    new DatabaseCoordinator.WriteCallable<Integer>() {
                        @Override public Integer call(SQLiteDatabase db) {
                            return RuntimeEventLedgerR926.drainOutboxRecoveryR81(
                                    db, 4, started + 50L);
                        }
                    }, Integer.valueOf(0));
        }
    }

    /** Returns true only when the immutable event can be read back from SQLite. */
    public static boolean append(DbStore store, final RuntimeEventLedgerR926.Event event) {
        if (store == null || event == null || !event.valid()) return false;
        final DatabaseCoordinator coordinator = store.coordinatorR36();
        if (coordinator == null) return false;

        final boolean[] handedOff = new boolean[] {false};
        boolean enqueueOk =
                coordinator.runWrite(
                        "R926_EXTERNAL_EVENT_OUTBOX_ENQUEUE_" + event.type,
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override
                            public void run(SQLiteDatabase db) {
                                boolean owns = !db.inTransaction();
                                if (owns) db.beginTransaction();
                                try {
                                    if (eventDurable(db, event.dedupKey)) {
                                        handedOff[0] = true;
                                    } else {
                                        RuntimeEventLedgerR926.enqueueExternal(db, event);
                                        handedOff[0] =
                                                outboxDurable(db, event.dedupKey)
                                                        || eventDurable(db, event.dedupKey);
                                    }
                                    if (!handedOff[0]) {
                                        throw new IllegalStateException(
                                                "R926_EXTERNAL_EVENT_OUTBOX_ENQUEUE_FAILED|"
                                                        + event.dedupKey);
                                    }
                                    if (owns) db.setTransactionSuccessful();
                                } finally {
                                    if (owns) db.endTransaction();
                                }
                            }
                        });
        if (!enqueueOk || !handedOff[0]) return false;

        // Drain only this producer's exact event. A pre-draw caller must not pay for up
        // to 64 old per-row commits or return false merely because it is behind them.
        // Historical rows remain durable and are drained by the existing recovery owner.
        // Drain failure is not data loss: the committed PENDING row stays replayable.
        coordinator.runWrite(
                "R926_EXTERNAL_EVENT_OUTBOX_DRAIN_" + event.type,
                new DatabaseCoordinator.WriteRunnable() {
                    @Override
                    public void run(SQLiteDatabase db) {
                        RuntimeEventLedgerR926.drainOutboxForEventR81(db, event.dedupKey);
                    }
                });

        // Verification is a read. Acquiring the sole writer here used to add a third
        // queue wait, even though the exact event transaction had already committed.
        try { return eventDurable(store.readable(), event.dedupKey); }
        catch (RuntimeException unavailable) { return false; }
    }

    private static boolean eventDurable(SQLiteDatabase db, String dedupKey) {
        return existsByDedup(db, RuntimeEventLedgerR926.TABLE_EVENT, dedupKey);
    }

    private static boolean outboxDurable(SQLiteDatabase db, String dedupKey) {
        return existsByDedup(db, RuntimeEventLedgerR926.TABLE_OUTBOX, dedupKey);
    }

    private static boolean existsByDedup(SQLiteDatabase db, String table, String dedupKey) {
        String key = safe(dedupKey);
        if (db == null || key.length() == 0) return false;
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT 1 FROM " + table + " WHERE dedup_key=? LIMIT 1",
                    new String[] {key});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
