package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitIdentityR31;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitOutcome;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteClass;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteContext;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteRejectedR31;
import com.ethk3.novaonepro.strategyv2.ResearchPoolR27;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class R341DatabaseTestCases {
    private static final String PROBE = "r341_writer_probe";
    private static final String BASE = "202611240005";
    private static final String TARGET = "202611240006";
    private R341DatabaseTestCases() {}

    private static DbStore open(Context app) {
        assertTrue(app.getDatabasePath(AppConfig.DB_NAME).getAbsolutePath().contains("/r30-instrumentation/"));
        DbStore store = new DbStore(app);
        try {
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                    WriteContext.of(WriteClass.MIGRATION, "R341_B02_FIXTURE_SCHEMA"), db -> {
                        SchemaR27.ensureR31Observations(db);
                        SchemaR27.ensureR31WriteProtocol(db);
                        db.execSQL("CREATE TABLE IF NOT EXISTS " + PROBE + "(id INTEGER PRIMARY KEY, value INTEGER)");
                        return null;
                    });
            for (int i = 0; i < 10 && pending() > 0; i++) WriterHoldFaultsR31.drainControlSliceR32(store);
            return store;
        } catch (RuntimeException | Error failure) {
            store.close();
            throw failure;
        }
    }

    private static CommitIdentityR31 identity(String name, long seq) {
        return new CommitIdentityR31("R341_B02:" + name + ":" + seq, "payload:" + name,
                "r341-b02-sqlite-probe", "isolated", 1, "input:" + name, seq, "R341_ENGINEERING");
    }

    private static long count(DbStore store, String sql) {
        try (Cursor row = store.readable().rawQuery(sql, null)) {
            assertTrue(row.moveToFirst());
            return row.getLong(0);
        }
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    public static void deadlines(Context app) throws Exception {
        DbStore store = open(app);
        ExecutorService threads = Executors.newFixedThreadPool(7);
        CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            Future<?> blocker = threads.submit(() -> {
                try (DatabaseCoordinator.WriterGuardCP3 ignored = DatabaseCoordinator.acquireWriterGuardCP3()) {
                    held.countDown();
                    await(release);
                }
                return null;
            });
            await(held);
            AtomicInteger bodies = new AtomicInteger();
            List<Future<String>> waits = new ArrayList<>();
            for (WriteClass lane : new WriteClass[] {WriteClass.DRAW_INGEST, WriteClass.LIVE_FREEZE,
                    WriteClass.SETTLEMENT, WriteClass.RESEARCH_CONTROL, WriteClass.RESEARCH_DATA, WriteClass.MIGRATION}) {
                waits.add(threads.submit(() -> {
                    try {
                        coordinator.runAtomicWriteOrThrowF01(WriteContext.of(lane, "R341_CONTENDED_" + lane)
                                .withDeadlineMonoMs(SystemClock.elapsedRealtime() + 100), db -> bodies.incrementAndGet());
                        return "UNEXPECTED_WRITE";
                    } catch (WriteRejectedR31 wait) {
                        assertEquals(CommitOutcome.ROLLED_BACK, wait.commitOutcome);
                        return wait.category;
                    }
                }));
            }
            for (Future<String> wait : waits) assertEquals("TEMP_TIMEOUT", wait.get(4, TimeUnit.SECONDS));
            assertEquals(0, bodies.get());
            release.countDown();
            blocker.get(5, TimeUnit.SECONDS);
            CommitIdentityR31 id = identity("lost-diagnostic", 1);
            DatabaseCoordinator.diagnosticFaultR31.set(phase -> {
                if ("AFTER_COMMIT".equals(phase)) throw new IllegalStateException("INJECTED_DIAGNOSTIC_LOSS");
            });
            DatabaseCoordinator.CommitResultR31<Integer> committed;
            try {
                committed = coordinator.runIdempotentAtomicWriteR31(
                        WriteContext.of(WriteClass.LIVE_FREEZE, "R341_RECOVER_COMMIT"), id,
                        db -> { db.execSQL("INSERT INTO " + PROBE + " VALUES(1,1)"); return 1; }, 0);
            } finally { DatabaseCoordinator.diagnosticFaultR31.remove(); }
            assertEquals(CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED, committed.outcome);
            assertEquals(CommitOutcome.COMMITTED, coordinator.resolveCommitReceiptR31(id));
            assertTrue(coordinator.runIdempotentAtomicWriteR31(
                    WriteContext.of(WriteClass.LIVE_FREEZE, "R341_RECOVER_COMMIT"), id,
                    db -> { throw new AssertionError("A durable body ran twice"); }, 0).replay);
            assertEquals(1L, count(store, "SELECT count(*) FROM " + PROBE));
        } finally {
            release.countDown();
            threads.shutdownNow();
            threads.awaitTermination(5, TimeUnit.SECONDS);
            store.close();
        }
    }

    public static void slicedWrites(Context app) {
        DbStore store = open(app);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            WriteContext invalid = WriteContext.of(WriteClass.MIGRATION, "R341_DDL_PLAN")
                    .withWorkload(0, 0, 2, 2, true);
            try {
                coordinator.runAtomicWriteOrThrowF01(invalid, db -> { throw new AssertionError("multi-DDL slice entered SQL"); });
                fail("Oversize DDL admitted");
            } catch (WriteRejectedR31 rejected) { assertEquals("RESOURCE_WAIT", rejected.category); }
            int next = 0;
            while (next < 10000) {
                int start = next, end = Math.min(next + WriteBudgetR341.MAX_ROWS, 10000);
                WriteContext context = WriteContext.of(WriteClass.RESEARCH_DATA, "R341_HISTORY_SLICE")
                        .withWorkload(end - start, (end - start) * 16L, end - start, 0, true);
                CommitIdentityR31 id = identity("history-page", start);
                if (start == 128) {
                    try {
                        coordinator.runIdempotentAtomicWriteR31(context, id, db -> {
                            db.execSQL("INSERT INTO " + PROBE + " VALUES(128,1)");
                            throw new IllegalStateException("INJECTED_PREPARED_CANCELLATION");
                        }, 0);
                        fail("Interrupted slice committed");
                    } catch (WriteRejectedR31 expected) { assertEquals(CommitOutcome.ROLLED_BACK, expected.commitOutcome); }
                    assertEquals(start, count(store, "SELECT count(*) FROM " + PROBE));
                    assertEquals(CommitOutcome.ROLLED_BACK, coordinator.resolveCommitReceiptR31(id));
                }
                DatabaseCoordinator.CommitResultR31<Integer> committed = coordinator.runIdempotentAtomicWriteR31(context, id, db -> {
                    for (int i = start; i < end; i++)
                        db.execSQL("INSERT INTO " + PROBE + " VALUES(?,1)", new Object[]{i});
                    return end;
                }, start);
                assertEquals(CommitOutcome.COMMITTED, committed.outcome);
                next = committed.value;
            }
            assertEquals(10000L, count(store, "SELECT count(*) FROM " + PROBE));
            assertEquals(157L, count(store, "SELECT count(*) FROM r31_commit_receipt WHERE operation='R341_HISTORY_SLICE'"));
        } finally { store.close(); }
    }

    private static int pending() { return ((Number) WriterHoldFaultsR31.snapshot().get("pending")).intValue(); }

    private static void enqueueFaults(int amount) {
        for (int i = 0; i < amount; i++)
            WriterHoldFaultsR31.observe("R341_FAULT_FIXTURE", false, 1L, 210L, false, CommitOutcome.COMMITTED);
    }

    public static void drainFaultBacklog(Context app) {
        DbStore store = open(app);
        try {
            int before = pending();
            long storedBefore = count(store, "SELECT count(*) FROM r31_runtime_fault WHERE category='WRITER_HOLD'");
            enqueueFaults(WriterHoldFaultsR31.MAX_PENDING + 2);
            assertEquals(WriterHoldFaultsR31.MAX_PENDING, pending());
            assertTrue(WriterHoldFaultsR31.deferResearch());
            assertTrue(((Number) WriterHoldFaultsR31.snapshot().get("capacityLoss")).longValue() >= before + 2L);
            long loops = 0;
            while (pending() > 0 && loops++ < 12) WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals(0, pending());
            assertFalse(WriterHoldFaultsR31.deferResearch());
            assertEquals(storedBefore + WriterHoldFaultsR31.MAX_PENDING,
                    count(store, "SELECT count(*) FROM r31_runtime_fault WHERE category='WRITER_HOLD'"));
            WriterHoldFaultsR31.observe("R31_WRITER_HOLD_FAULT", false, 1L, 900L, false, CommitOutcome.COMMITTED);
            assertEquals(0, pending());
        } finally { store.close(); }
    }

    public static void faultRetry(Context app) throws Exception {
        DbStore store = open(app);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            WriteContext migration = WriteContext.of(WriteClass.MIGRATION, "R341_FAULT_FIXTURE_MIGRATION");
            coordinator.runAtomicWriteOrThrowF01(migration, db -> {
                db.execSQL("ALTER TABLE r31_runtime_fault RENAME TO r341_saved_faults");
                return null;
            });
            enqueueFaults(1);
            int pending = pending();
            WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals("MIGRATION_REQUIRED", WriterHoldFaultsR31.snapshot().get("drainStatus"));
            assertEquals(pending, pending());
            coordinator.runAtomicWriteOrThrowF01(migration, db -> {
                db.execSQL("ALTER TABLE r341_saved_faults RENAME TO r31_runtime_fault");
                return null;
            });
            waitForDrain();
            WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals(0, pending());
            assertTrue(count(store, "SELECT count(*) FROM r31_runtime_fault") > 0L);
            coordinator.runAtomicWriteOrThrowF01(migration, db -> {
                db.execSQL("UPDATE r31_runtime_fault SET byte_count=?", new Object[]{ResearchPoolR27.MAX_FAULT_BYTES_R31});
                return null;
            });
            enqueueFaults(1);
            WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals("BLOCKED_STORAGE", WriterHoldFaultsR31.snapshot().get("drainStatus"));
            assertEquals(1, pending());
        } finally { store.close(); }
    }

    private static void waitForDrain() throws InterruptedException {
        long next = ((Number) WriterHoldFaultsR31.snapshot().get("nextDrainElapsedMs")).longValue();
        Thread.sleep(Math.max(0L, next - SystemClock.elapsedRealtime() + 20L));
    }

    private static Map<?,?> datasetSnapshot() {
        return (Map<?,?>) DatabaseCoordinator.writeProtocolSnapshotR31().get("replayDatasetServiceR341");
    }

    public static void fairService(Context app) throws Exception {
        DbStore store = open(app);
        try {
            WriteBudgetR341.DatasetService simulated = new WriteBudgetR341.DatasetService();
            simulated.ready(BASE, 100L);
            assertFalse(simulated.due(5000L, 0));
            assertFalse(simulated.due(31000L, 1));
            assertEquals("SATURATED_CURRENT_FREEZE", simulated.snapshot(31000L, 1).get("status"));
            assertTrue(simulated.due(31001L, 0));
            assertFalse(DatabaseCoordinator.replayDatasetDueR341(BASE));
            long ready = ((Number) datasetSnapshot().get("oldestReadyElapsedMs")).longValue();
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            WriteContext other = WriteContext.of(WriteClass.RESEARCH_DATA, "R341_OTHER_RESEARCH");
            for (int i = 1; i <= 4; i++) {
                int n = i;
                coordinator.runIdempotentAtomicWriteR31(other, identity("other", n),
                        db -> { db.execSQL("INSERT INTO " + PROBE + " VALUES(?,1)", new Object[]{n}); return n; }, 0);
                assertEquals(ready, datasetSnapshot().get("oldestReadyElapsedMs"));
                Thread.sleep(1250L);
            }
            assertTrue(DatabaseCoordinator.replayDatasetDueR341(BASE));
            enqueueFaults(24);
            assertTrue(WriterHoldFaultsR31.deferResearch());
            submitDataset(coordinator, "fair-service", 10, false);
            assertEquals(5L, count(store, "SELECT count(*) FROM " + PROBE));
            assertEquals("R341_B02:fair-service:10", datasetSnapshot().get("lastCommit"));
        } finally { store.close(); }
    }

    private static boolean submitDataset(DatabaseCoordinator db, String key, int row, boolean rollback) {
        CommitIdentityR31 id = identity(key, row);
        DatabaseCoordinator.prepareReplayDatasetR341(id.inputSnapshot);
        try {
            return db.runIdempotentAtomicWriteR31(WriteContext.of(WriteClass.RESEARCH_DATA, "R27_REPLAY_DATASET")
                    .withReadyDataset(id.inputSnapshot), id, tx -> {
                tx.execSQL("INSERT INTO " + PROBE + " VALUES(?,1)", new Object[]{row});
                if (rollback) throw new IllegalStateException("INJECTED_SQL_ROLLBACK");
                return row;
            }, row).replay;
        } finally { DatabaseCoordinator.releaseReplayDatasetR341(id.inputSnapshot); }
    }

    public static void rollbackDoesNotAdvanceAge(Context app) {
        DbStore store = open(app);
        try {
            DatabaseCoordinator.replayDatasetDueR341(BASE);
            Object ready = datasetSnapshot().get("oldestReadyElapsedMs");
            Object priorCommit = datasetSnapshot().get("lastCommittedElapsedMs");
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            try {
                submitDataset(coordinator, "dataset-rollback", 63, true);
                fail("Rollback unexpectedly acknowledged");
            } catch (WriteRejectedR31 expected) { assertEquals(CommitOutcome.ROLLED_BACK, expected.commitOutcome); }
            assertEquals(ready, datasetSnapshot().get("oldestReadyElapsedMs"));
            assertEquals(priorCommit, datasetSnapshot().get("lastCommittedElapsedMs"));
            assertFalse(submitDataset(coordinator, "dataset-rollback", 63, false));
            Object committedAt = datasetSnapshot().get("lastCommittedElapsedMs");
            assertTrue(submitDataset(coordinator, "dataset-rollback", 63, false));
            assertEquals(committedAt, datasetSnapshot().get("lastCommittedElapsedMs"));
            assertEquals(1L, count(store, "SELECT count(*) FROM " + PROBE));
        } finally { store.close(); }
    }

    private static RuntimeReadSnapshotR341.Snapshot read(Object scope, String base, String target,
            RuntimeReadSnapshotR341.Status state) {
        long now = SystemClock.elapsedRealtime();
        return new RuntimeReadSnapshotR341.Snapshot(scope, base, target, System.currentTimeMillis(),
                now + 60000L, now + 5000L, state);
    }

    public static void blockedReadDoesNotBlockControl(Context app) throws Exception {
        DbStore store = open(app);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch reading = new CountDownLatch(1), release = new CountDownLatch(1);
        try {
            RuntimeReadSnapshotR341 exchange = new RuntimeReadSnapshotR341();
            AtomicInteger loaders = new AtomicInteger();
            exchange.refreshAsync(store, worker, () -> {
                try (Cursor cursor = store.readable().rawQuery("SELECT count(*) FROM " + PROBE, null)) {
                    assertTrue(cursor.moveToFirst());
                    loaders.incrementAndGet();
                    reading.countDown();
                    try { await(release); } catch (InterruptedException failure) { throw new IllegalStateException(failure); }
                    return read(store, BASE, TARGET, RuntimeReadSnapshotR341.Status.FROZEN);
                }
            });
            await(reading);
            long started = SystemClock.elapsedRealtime();
            for (int i = 0; i < 100; i++) {
                assertEquals(RuntimeReadSnapshotR341.Status.UNKNOWN_NEEDS_REFRESH, exchange.read(store, started));
                exchange.refreshAsync(store, worker, () -> { throw new AssertionError("parallel read"); });
            }
            assertTrue(SystemClock.elapsedRealtime() - started < 1000L);
            exchange.publishAfterCommit(read(store, TARGET, "202611240007", RuntimeReadSnapshotR341.Status.NEEDS_FREEZE));
            enqueueFaults(1);
            WriterHoldFaultsR31.drainControlSliceR32(store);
            release.countDown();
            worker.submit(() -> {}).get(4, TimeUnit.SECONDS);
            assertEquals(1, loaders.get());
            assertEquals(0, pending());
            assertEquals(RuntimeReadSnapshotR341.Status.NEEDS_FREEZE, exchange.read(store, SystemClock.elapsedRealtime()));
        } finally {
            release.countDown();
            worker.shutdownNow();
            worker.awaitTermination(5, TimeUnit.SECONDS);
            store.close();
        }
    }

    public static void readScopeChange(Context app) {
        DbStore store = open(app);
        try {
            RuntimeReadSnapshotR341 exchange = new RuntimeReadSnapshotR341();
            Object active = store;
            for (int i = 0; i < 100; i++) {
                exchange.invalidate();
                assertEquals(RuntimeReadSnapshotR341.Status.UNKNOWN_NEEDS_REFRESH,
                        exchange.read(active, SystemClock.elapsedRealtime()));
                final Object prior = active;
                final Object replacement = new Object();
                exchange.refreshAsync(prior, Runnable::run, () -> {
                    try (Cursor row = store.readable().rawQuery("SELECT count(*) FROM " + PROBE, null)) {
                        assertTrue(row.moveToFirst());
                        exchange.invalidate();
                        exchange.freezeCommitted(replacement, TARGET, "202611240007",
                                System.currentTimeMillis(), SystemClock.elapsedRealtime());
                        throw new IllegalStateException("OLD_CURSOR_INTERUPTED");
                    }
                });
                assertEquals(RuntimeReadSnapshotR341.Status.UNKNOWN_NEEDS_REFRESH,
                        exchange.read(prior, SystemClock.elapsedRealtime()));
                assertEquals(RuntimeReadSnapshotR341.Status.FROZEN, exchange.read(replacement, SystemClock.elapsedRealtime()));
                active = replacement;
            }
        } finally { store.close(); }
    }
}
