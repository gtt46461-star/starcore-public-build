package com.ethk3.novaonepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitOutcome;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteClass;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteContext;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteRejectedR31;
import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31;
import com.ethk3.novaonepro.Field173NativeEvidence.Checks;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class R343WriterRuntimeInstrumentedTest {
    private static Context sandbox() {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        assertTrue(app.getDatabasePath(AppConfig.DB_NAME).getPath().contains("/r30-instrumentation/"));
        return app;
    }

    private static WriteContext fixtureMigration(String name) {
        return WriteContext.of(WriteClass.MIGRATION, "R343_TEST_" + name);
    }

    private static long stat(Map<?, ?> snapshot, String key) {
        return ((Number) snapshot.get(key)).longValue();
    }

    private static long scalar(DbStore store, String sql) {
        try (Cursor cursor = store.readable().rawQuery(sql, null)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getLong(0);
        }
    }

    private static void readback(DbStore store, String test, Map<String, Object> values) {
        String identity = RuntimeObservationScopeR341.databaseIdentity(store.readable());
        assertFalse(RuntimeObservationScopeR341.UNRESOLVED.equals(identity));
        Log.i("STARCORE_R343", "R343_TEST_READBACK " + JsonR27.write(Map.of(
                "test", test, "databaseIdentity", identity, "sourceCommit", BuildConfig.SOURCE_COMMIT_R26,
                "values", values)));
    }

    private static void record(Checks checks, DbStore store, String key, Object value) throws Exception {
        String database = RuntimeObservationScopeR341.databaseIdentity(store.readable());
        checks.require(!RuntimeObservationScopeR341.UNRESOLVED.equals(database), "Database identity resolves");
        checks.databaseWithoutTarget("ANDROID_ENGINEERING_FIXTURE", database,
                "Writer and journal fixtures do not consume a PIT issue or cutoff");
        checks.record(key, value);
    }

    private static WriteRejectedR31 rejectedSlice(DatabaseCoordinator coordinator, WriteContext work) {
        try {
            coordinator.runAtomicWriteOrThrowF01(work, db -> {
                fail("Oversize workload executed its SQL body");
                return null;
            });
            throw new AssertionError("Oversize workload was admitted");
        } catch (WriteRejectedR31 rejection) {
            assertEquals("RESOURCE_QUEUE", rejection.category);
            assertEquals("ADMISSION", rejection.phase);
            assertEquals(CommitOutcome.ROLLED_BACK, rejection.commitOutcome);
            assertFalse(FailureDispositionR31.classify(rejection).consumesFaultBudget());
            return rejection;
        }
    }

    @Test public void oversizedSlicesWaitAndSplitAtRealWriterBoundary() {
        Context app = sandbox();
        DbStore store = new DbStore(app);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            coordinator.runAtomicWriteOrThrowF01(fixtureMigration("SLICE_TABLE"), db -> {
                db.execSQL("CREATE TABLE r343_test_slice(id INTEGER PRIMARY KEY)");
                return null;
            });
            WriteContext research = WriteContext.of(WriteClass.RESEARCH_DATA, "R27_REPLAY_DATASET");
            WriteRejectedR31 rows = rejectedSlice(coordinator, research.withWorkload(65, 4096, 1, 0, true));
            rejectedSlice(coordinator, research.withWorkload(1, 65_537, 1, 0, true));
            assertEquals(0, scalar(store, "SELECT count(*) FROM r343_test_slice"));
            StartupStageJournalR341 journal = StartupStageJournalR341.open(app, "r343-slice-test");
            StartupStageJournalR341.Token attempt = journal.begin("research", "oversize-slice",
                    StartupStageJournalR341.Stage.OWNER_BEGIN, null);
            assertTrue(journal.recordUncaught(attempt, rows).durable);
            List<?> frames = (List<?>) journal.snapshot().get("frames");
            Map<?, ?> frame = (Map<?, ?>) frames.get(frames.size() - 1);
            assertEquals("WAIT_RESOURCE", frame.get("outcome"));
            assertEquals("RESOURCE_QUEUE", frame.get("failureCategory"));
            assertEquals("ROLLED_BACK", frame.get("commitOutcome"));
            assertFalse(journal.hasDeterministicFailureLimit());

            coordinator.runAtomicWriteOrThrowF01(research.withWorkload(64, 64 * 1024, 1, 0, true), db -> {
                db.execSQL("WITH RECURSIVE seq(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM seq WHERE n<64)"
                        + " INSERT INTO r343_test_slice SELECT n FROM seq");
                return null;
            });
            coordinator.runAtomicWriteOrThrowF01(research.withWorkload(1, 64, 1, 0, true), db -> {
                db.execSQL("INSERT INTO r343_test_slice VALUES(65)");
                return null;
            });
            long actualRows = scalar(store, "SELECT count(*) FROM r343_test_slice");
            assertEquals(65, actualRows);
            assertEquals(2145, scalar(store, "SELECT sum(id) FROM r343_test_slice"));
            readback(store, "R343-007", Map.of("rows", actualRows, "waitFrame", frame,
                    "writer", WriteBudgetR341.snapshot().get("RESEARCH_DATA")));
        } finally {
            store.close();
        }
    }

    @Test public void oldUnknownAndCommittedDispositionsRemainDistinct() {
        WriteContext research = WriteContext.of(WriteClass.RESEARCH_DATA, "R27_REPLAY_DATASET");
        FailureDispositionR31.Decision old = FailureDispositionR31.classify(
                new WriteRejectedR31("RESOURCE_WAIT", research.operation, "ADMISSION",
                        CommitOutcome.ROLLED_BACK, null));
        FailureDispositionR31.Decision unknown = FailureDispositionR31.classify(
                new WriteRejectedR31("RESOURCE_QUEUE", research.operation, "COMMIT",
                        CommitOutcome.UNKNOWN, null));
        FailureDispositionR31.Decision committed = FailureDispositionR31.classify(
                new WriteRejectedR31("RESOURCE_QUEUE", research.operation, "ACK",
                        CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED, null));
        assertTrue(old.consumesFaultBudget());
        assertTrue(unknown.requiresCommitResolution);
        assertFalse(unknown.alreadyCommitted);
        assertTrue(committed.alreadyCommitted);
        assertFalse(committed.requiresCommitResolution);
    }

    @Test public void fieldOversizeRejectsBeforeSql() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
                WriteContext research = WriteContext.of(WriteClass.RESEARCH_DATA, "R27_REPLAY_DATASET");
                WriteRejectedR31 rows = rejectedSlice(coordinator, research.withWorkload(65, 4096, 1, 0, true));
                Map<String, Object> rowReceipt = WriteBudgetR341.snapshot().get("RESEARCH_DATA");
                WriteRejectedR31 bytes = rejectedSlice(coordinator, research.withWorkload(1, 65_537, 1, 0, true));
                Map<String, Object> byteReceipt = WriteBudgetR341.snapshot().get("RESEARCH_DATA");
                checks.equal("SLICE_TOO_LARGE", rowReceipt.get("admission"), "65 rows rejected at admission");
                checks.equal("SLICE_TOO_LARGE", byteReceipt.get("admission"), "65,537 bind bytes rejected at admission");
                checks.equal(CommitOutcome.ROLLED_BACK, rows.commitOutcome, "No row-limited transaction ran");
                checks.equal(CommitOutcome.ROLLED_BACK, bytes.commitOutcome, "No byte-limited transaction ran");
                checks.require(!FailureDispositionR31.classify(rows).consumesFaultBudget(),
                        "Rejected row count is a resource wait");
                checks.require(!FailureDispositionR31.classify(bytes).consumesFaultBudget(),
                        "Rejected bind count is a resource wait");
                record(checks, store, "writerAdmission", Map.of("rows", rowReceipt, "bytes", byteReceipt));
            } finally {
                store.close();
            }
        });
    }

    @Test public void fieldFrozenProjectionRejectsMismatchedHash() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                String raw = MainWriterFaultProjectionR342.capture(app, store.readable(), null);
                checks.require(raw != null, "Production capture returned a writer sample");
                Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(raw);
                String badHash = V51ChecksumEngine.sha256("wrong-sample");
                checks.require(!badHash.equals(envelope.get("sha256")), "Fixture changed the sample identity");
                checks.require(store.trySetTelemetryStatesR77(Map.of(MainWriterFaultProjectionR342.KEY,
                        JsonR27.write(Map.of("sha256", badHash, "original", envelope.get("original"))))),
                        "Projection row committed");
                Map<String, Object> bad = MainWriterFaultProjectionR342.fromSnapshot(app, store.readable());
                checks.equal("UNKNOWN", bad.get("observationState"), "Hash mismatch has no main-owner claim");
                checks.equal("MAIN_PROJECTION_HASH_MISMATCH", bad.get("reason"), "Cause is retained");
                checks.equal(null, bad.get("pending"), "Missing verified pending total stays unknown");
                checks.equal(null, bad.get("historicalOriginalJson"), "Unverified original is excluded");
                record(checks, store, "snapshot", bad);
            } finally {
                store.close();
            }
        });
    }

    private static Models.Draw fixtureDraw(String issue, long open) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue;
        draw.openTime = open;
        draw.fetchTime = open + 200L;
        for (int pos = 0; pos < 10; pos++) draw.numbers[pos] = pos + 1;
        return draw;
    }

    private static String seedArchivePressure(Context app, DbStore store, DatabaseCoordinator coordinator,
                                               Checks checks) throws Exception {
        WriterHoldFaultsR31.observe("R343_EXISTING_HOT_ORIGINAL", false, 1, 212, false, CommitOutcome.COMMITTED);
        for (int turn = 0; turn < 10 && stat(WriterHoldFaultsR31.snapshot(), "pending") > 0; turn++) {
            WriterHoldFaultsR31.drainControlSliceR32(store);
        }
        coordinator.runAtomicWriteOrThrowF01(fixtureMigration("FIELD_DEBT_BYTES"), db -> {
            db.execSQL("UPDATE r31_runtime_fault SET byte_count=? WHERE operation=?",
                    new Object[]{com.ethk3.novaonepro.strategyv2.ResearchPoolR27.MAX_FAULT_BYTES_R31,
                            "R343_EXISTING_HOT_ORIGINAL"});
            return null;
        });
        String event;
        try (Cursor row = store.readable().rawQuery("SELECT event_id FROM r31_runtime_fault WHERE operation=?",
                new String[]{"R343_EXISTING_HOT_ORIGINAL"})) {
            checks.require(row.moveToFirst(), "The old hot original came from the actual drain");
            event = row.getString(0);
        }
        File files = app.getFilesDir().getCanonicalFile();
        File archive = new File(files, RuntimeFaultOriginalStoreR335.DIRECTORY);
        checks.require(archive.mkdir(), "Fresh archive directory in the instrumented data directory");
        File retained = new File(archive, "old-unrelated-original.row");
        java.nio.file.Files.write(retained.toPath(), new byte[]{1, 7, 3});
        android.system.Os.symlink(retained.getPath(), new File(archive, "old-unrelated-reference").getPath());
        try (StorageBudgetR31.Reservation unused = StorageBudgetR31.forFiles(files).reserve(
                "R343_FIELD_OLD_ARCHIVE_SCOPE", 0, StorageBudgetR31.Category.DIAGNOSTICS,
                "files/" + RuntimeFaultOriginalStoreR335.DIRECTORY, 128L * 1024, false)) {
            throw new AssertionError("The previous whole-directory scope unexpectedly ignored its sibling");
        } catch (StorageBudgetR31.Rejected blocked) {
            checks.equal("SCOPE_SYMLINK", blocked.reason, "Previous archive reservation fails on an unrelated entry");
            checks.record("previousArchiveScopeError", blocked.reason);
        }
        return event;
    }

    @Test public void fieldBackfillResumesAfterWriterFaultReadback() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            DbStore store = new DbStore(app);
            try {
                DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
                coordinator.runAtomicWriteOrThrowF01(fixtureMigration("BACKFILL_SCHEMA"), db -> {
                    SchemaR27.ensureR31Observations(db);
                    SchemaR27.ensureR31WriteProtocol(db);
                    RuntimeFaultOriginalStoreR335.ensureSchema(db);
                    return null;
                });

                String oldEvent = seedArchivePressure(app, store, coordinator, checks);
                byte[] oldBody = RuntimeFaultOriginalStoreR335.hot(store.readable(), oldEvent);
                String oldHash = FaultOriginalCodecR335.sha256(oldBody);
                long opened = System.currentTimeMillis() - 1_000L;
                Models.Draw previous = fixtureDraw("202611250100", opened - 60_000L);
                Models.Draw target = fixtureDraw("202611250101", opened);
                checks.equal(1, store.saveDraws(List.of(previous)), "Previous history write committed");
                for (int n = 0; n < 26; n++) {
                    WriterHoldFaultsR31.observe("R343_TEST_BACKFILL", false, 1, 212, false, CommitOutcome.COMMITTED);
                }
                checks.require(WriterHoldFaultsR31.deferResearch(), "Queue crosses the production defer threshold");
                try {
                    store.saveDraws(List.of(target));
                    fail("Research body entered despite the durable drain backlog");
                } catch (IllegalStateException blocked) {
                    FailureDispositionR31.ClassifiedFailure typed = FailureDispositionR31.firstTypedR31(blocked);
                    checks.require(typed != null, "Backfill failure preserves the typed writer cause");
                    checks.equal("RESOURCE_QUEUE", typed.failureCategory(), "Backfill waits for the same writer queue");
                    checks.equal("ADMISSION", typed.phase(), "The blocked backfill did not run SQL");
                    checks.equal("ROLLED_BACK", typed.commitOutcomeName(), "The attempt made no history commit");
                }
                checks.equal(0L, scalar(store, "SELECT COUNT(*) FROM draws WHERE issue='202611250101'"),
                        "Target is absent while backfill waits");
                long before = stat(WriterHoldFaultsR31.snapshot(), "capacityLoss");
                WriterHoldFaultsR31.drainControlSliceR32(store);
                Map<String, Object> pressure = WriterHoldFaultsR31.snapshot();
                checks.equal("BLOCKED_STORAGE", pressure.get("drainStatus"), "A full hot store waits for the archive owner");
                checks.equal(4L, stat(pressure, "durableInFlightRows"), "The exact four-row outbox stays durable");
                checks.require(DataRetentionGovernorR900.runFaultMaintenanceR335(store, SystemClock.elapsedRealtime() + 10_000L),
                        "Existing maintenance owner services the request");
                checks.equal(RuntimeFaultArchiveB01.State.VERIFIED_RETIRED, RuntimeFaultArchiveB01.observation().state,
                        RuntimeFaultOriginalStoreR335.processDetail());
                checks.equal(null, RuntimeFaultOriginalStoreR335.hot(store.readable(), oldEvent), "Verified hot original retired");
                checks.require(java.util.Arrays.equals(oldBody, RuntimeFaultOriginalStoreR335.readFile(
                                app.getFilesDir(), oldHash, oldBody.length)), "Cold original bytes agree with the old hot row");
                checks.require(java.nio.file.Files.isSymbolicLink(new File(app.getFilesDir(),
                                RuntimeFaultOriginalStoreR335.DIRECTORY + "/old-unrelated-reference").toPath()),
                        "Other retained references are untouched");
                SystemClock.sleep(Math.max(0L, stat(pressure, "nextDrainElapsedMs") - SystemClock.elapsedRealtime() + 10L));
                for (int n = 0; n < 12 && stat(WriterHoldFaultsR31.snapshot(), "pending") > 0; n++) {
                    WriterHoldFaultsR31.drainControlSliceR32(store);
                }
                Map<String, Object> drained = WriterHoldFaultsR31.snapshot();
                checks.require(!WriterHoldFaultsR31.deferResearch(), "The same queue falls below the threshold");
                checks.equal(before, stat(drained, "capacityLoss"), "Drain preserved the injected queue");
                checks.equal(26L, scalar(store, "SELECT COUNT(*) FROM r31_runtime_fault"
                        + " WHERE operation='R343_TEST_BACKFILL'"), "All 26 event IDs are durable");
                checks.equal(1, store.saveDraws(List.of(target)), "The real backfill retry now commits");
                checks.equal(1L, scalar(store, "SELECT COUNT(*) FROM draws WHERE issue='202611250101'"),
                        "The target history row is committed once");
                String identity = RuntimeObservationScopeR341.databaseIdentity(store.readable());
                checks.require(!RuntimeObservationScopeR341.UNRESOLVED.equals(identity), "Database identity resolves");
                checks.database("FIXTURE_HISTORY_BACKFILL", target.issue, opened, identity);
                checks.record("drain", drainFields(drained));
                checks.record("verifiedArchivedOriginal", oldHash);
                checks.record("historyReadback", Map.of("target", target.issue, "persistedEntries", 1,
                        "queueReceipts", 26, "losses", stat(drained, "capacityLoss")));
            } finally {
                store.close();
            }
        });
    }

    @Test public void drainCooldownDoesNotRefreshAttemptAndCommittedBatchReadsBack() {
        Context app = sandbox();
        DbStore store = new DbStore(app);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            coordinator.runAtomicWriteOrThrowF01(fixtureMigration("FAULT_SCHEMA"), db -> {
                SchemaR27.ensureR31Observations(db);
                SchemaR27.ensureR31WriteProtocol(db);
                RuntimeFaultOriginalStoreR335.ensureSchema(db);
                return null;
            });
            WriterHoldFaultsR31.observe("R343_TEST_COOLDOWN", false, 1, 212, false, CommitOutcome.COMMITTED);
            File inFlight = new File(app.getNoBackupFilesDir(), "r342-writer-fault-inflight.json");
            coordinator.runAtomicWriteOrThrowF01(fixtureMigration("RENAME_FAULT_SCHEMA"), db -> {
                db.execSQL("ALTER TABLE r31_runtime_fault RENAME TO r343_test_saved_faults");
                return null;
            });
            WriterHoldFaultsR31.drainControlSliceR32(store);
            Map<String, Object> failure = WriterHoldFaultsR31.snapshot();
            assertEquals("VERIFY_SCHEMA", failure.get("lastDrainFailurePhase"));
            assertEquals("MIGRATION_REQUIRED", failure.get("drainStatus"));
            assertTrue(inFlight.isFile());
            SystemClock.sleep(5L);
            assertTrue(SystemClock.elapsedRealtime() < stat(failure, "nextDrainElapsedMs"));
            WriterHoldFaultsR31.drainControlSliceR32(store);
            Map<String, Object> cooldown = WriterHoldFaultsR31.snapshot();
            assertTrue(stat(cooldown, "lastControlTickWallMs") > stat(failure, "lastControlTickWallMs"));
            assertEquals(stat(failure, "lastDrainWallMs"), stat(cooldown, "lastDrainWallMs"));
            assertEquals(stat(failure, "drainAttempts"), stat(cooldown, "drainAttempts"));
            assertEquals(stat(failure, "durableInFlightRows"), stat(cooldown, "durableInFlightRows"));
            assertEquals(failure.get("durableInFlightHash"), cooldown.get("durableInFlightHash"));

            coordinator.runAtomicWriteOrThrowF01(fixtureMigration("RESTORE_FAULT_SCHEMA"), db -> {
                db.execSQL("ALTER TABLE r343_test_saved_faults RENAME TO r31_runtime_fault");
                return null;
            });
            SystemClock.sleep(Math.max(0, stat(cooldown, "nextDrainElapsedMs") - SystemClock.elapsedRealtime()));
            WriterHoldFaultsR31.drainControlSliceR32(store);
            Map<String, Object> committed = WriterHoldFaultsR31.snapshot();
            assertEquals("COMPLETED", committed.get("drainPhase"));
            assertEquals(stat(cooldown, "drainAttempts") + 1, stat(committed, "drainAttempts"));
            assertTrue(stat(committed, "lastDrainWallMs") >= stat(cooldown, "lastDrainWallMs"));
            assertTrue(stat(committed, "readBackRows") >= 1);
            assertEquals(stat(committed, "attemptedRows"), stat(committed, "readBackRows"));
            assertEquals(1, scalar(store, "SELECT count(*) FROM r31_runtime_fault WHERE operation='R343_TEST_COOLDOWN'"));
            assertFalse(inFlight.exists());
            readback(store, "R343-009-COOLDOWN", Map.of("failedAttempt", drainFields(failure),
                    "cooldownTick", drainFields(cooldown), "committedAttempt", drainFields(committed)));
        } finally {
            store.close();
        }
    }

    private static Map<String, Object> drainFields(Map<String, Object> view) {
        return Map.of("controlWall", view.get("lastControlTickWallMs"), "attemptWall", view.get("lastDrainWallMs"),
                "attempts", view.get("drainAttempts"), "phase", view.get("drainPhase"),
                "attemptedRows", view.get("attemptedRows"), "readBackRows", view.get("readBackRows"));
    }

    @Test public void snapshotPreservesHashedHistoricalSampleWithoutMainOwnerClaim() {
        Context app = sandbox();
        DbStore store = new DbStore(app);
        try {
            String raw = MainWriterFaultProjectionR342.capture(app, store.readable(), null);
            assertNotNull(raw);
            Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(raw);
            Map<?, ?> sample = (Map<?, ?>) JsonR27.parse((String) envelope.get("original"));
            assertTrue(store.trySetTelemetryStatesR77(Map.of(MainWriterFaultProjectionR342.KEY, raw)));
            String frozenIdentity = "d".repeat(64);
            long limit = stat(sample, "validUntilElapsedMs");
            SystemClock.sleep(Math.max(0L, limit - SystemClock.elapsedRealtime()) + 10L);
            Map<String, Object> frozen = MainWriterFaultProjectionR342.fromSnapshot(app, store.readable(), frozenIdentity);
            assertEquals("UNKNOWN", frozen.get("observationState"));
            assertEquals("SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE", frozen.get("reason"));
            assertNull(frozen.get("pending"));
            assertNull(frozen.get("producerPid"));
            assertEquals(frozenIdentity, frozen.get("snapshotSha256"));
            assertEquals(envelope.get("original"), frozen.get("historicalOriginalJson"));
            assertEquals(envelope.get("sha256"), frozen.get("historicalOriginalSha256"));

            assertTrue(store.trySetTelemetryStatesR77(Map.of(MainWriterFaultProjectionR342.KEY,
                    JsonR27.write(Map.of("sha256", "f".repeat(64), "original", envelope.get("original"))))));
            Map<String, Object> bad = MainWriterFaultProjectionR342.fromSnapshot(app, store.readable(), frozenIdentity);
            assertEquals("UNKNOWN", bad.get("observationState"));
            assertEquals("MAIN_PROJECTION_HASH_MISMATCH", bad.get("reason"));
            assertNull(bad.get("pending"));
            assertNull(bad.get("historicalOriginalJson"));
            readback(store, "R343-009-SNAPSHOT", Map.of("historical", frozen, "badHash", bad));
        } finally {
            store.close();
        }
    }
}
