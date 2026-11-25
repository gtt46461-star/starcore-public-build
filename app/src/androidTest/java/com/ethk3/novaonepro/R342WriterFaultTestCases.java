package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.os.SystemClock;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitOutcome;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteClass;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteContext;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class R342WriterFaultTestCases {
    private R342WriterFaultTestCases() {}

    private static DbStore open(Context context) {
        assertTrue(context.getDatabasePath(AppConfig.DB_NAME).getPath().contains("/r30-instrumentation/"));
        DbStore store = new DbStore(context);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(schema("R342_FIXTURE_SCHEMA"), db -> {
            SchemaR27.ensureR31Observations(db);
            SchemaR27.ensureR31WriteProtocol(db);
            RuntimeFaultOriginalStoreR335.ensureSchema(db);
            return null;
        });
        return store;
    }

    private static WriteContext schema(String operation) {
        return WriteContext.of(WriteClass.MIGRATION, operation);
    }

    private static long count(DbStore store, String sql, String... values) {
        try (Cursor row = store.readable().rawQuery(sql, values)) {
            assertTrue(row.moveToFirst());
            return row.getLong(0);
        }
    }

    private static void observe(String operation, int rows) {
        for (int i = 0; i < rows; i++)
            WriterHoldFaultsR31.observe(operation, false, 1, 212, false, CommitOutcome.COMMITTED);
    }

    private static long stat(String key) {
        return ((Number) WriterHoldFaultsR31.snapshot().get(key)).longValue();
    }

    public static void startupOriginal(Context app) throws Exception {
        StartupStageJournalR341 startup = StartupStageJournalR341.open(app, "r342-test");
        String[] categories = {"RESOURCE_MEMORY", "DEPENDENCY", "MISSING_SOURCE", "CANCELLED", "BUG"};
        String[] outcomes = {"WAIT_RESOURCE", "WAIT_DEPENDENCY", "MISSING_SOURCE", "INTERRUPTED", "BUG"};
        for (int i = 0; i < categories.length; i++) {
            StartupStageJournalR341.Token token = startup.begin("test", "run-" + i,
                    StartupStageJournalR341.Stage.OWNER_BEGIN, null);
            Throwable typed = new DatabaseCoordinator.WriteRejectedR31(categories[i], "STARTUP_FIXTURE", "ADMISSION",
                    CommitOutcome.ROLLED_BACK, new IllegalStateException("NESTED_TEST_CAUSE secret=TEST_ONLY_SECRET"));
            assertTrue(startup.recordUncaught(token, new IllegalStateException("WRAPPER", typed)).durable);
            List<?> rows = (List<?>) startup.snapshot().get("frames");
            Map<?, ?> frame = (Map<?, ?>) rows.get(rows.size() - 1);
            assertEquals(categories[i], frame.get("failureCategory"));
            assertEquals(outcomes[i], frame.get("outcome"));
            assertEquals("ADMISSION", frame.get("failurePhase"));
            assertEquals("ROLLED_BACK", frame.get("commitOutcome"));
            File original = StartupStageJournalR341.verifiedFailureOriginal(app, (String) frame.get("failureOriginalSha256"));
            String raw = ReportJobJournalCP12.readAtomic(original, 65536);
            assertTrue(raw.contains("NESTED_TEST_CAUSE"));
            assertFalse(raw.contains("TEST_ONLY_SECRET"));
            assertFalse(startup.hasDeterministicFailureLimit());
        }
        for (int i = 0; i < 2; i++) {
            StartupStageJournalR341.Token token = startup.begin("test", "repeat-" + i,
                    StartupStageJournalR341.Stage.OWNER_BEGIN, null);
            Throwable typed = new DatabaseCoordinator.WriteRejectedR31("BUG", "STARTUP_FIXTURE", "ADMISSION",
                    CommitOutcome.ROLLED_BACK, new IllegalStateException("NESTED_TEST_CAUSE secret=TEST_ONLY_SECRET"));
            assertTrue(startup.recordUncaught(token, typed).durable);
        }
        assertTrue(startup.hasDeterministicFailureLimit());
    }

    public static void drainAndBackpressure(Context app, String phase) throws Exception {
        DbStore store = open(app);
        try {
            DatabaseCoordinator coordinator = store.coordinatorForResearchR952();
            File outbox = new File(app.getNoBackupFilesDir(), "r342-writer-fault-inflight.json");
            if ("restart".equals(phase)) {
                assertTrue("persisted outbox must survive the previous target process", outbox.isFile());
                Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(ReportJobJournalCP12.readAtomic(outbox, 65536));
                Map<?, ?> body = (Map<?, ?>) JsonR27.parse((String) envelope.get("body"));
                assertNotEquals(com.ethk3.novaonepro.strategyv2.WriterHoldLedgerR30.PROCESS_SESSION, body.get("session"));
                List<?> events = (List<?>) body.get("rows");
                WriterHoldFaultsR31.drainControlSliceR32(store);
                assertFalse(outbox.exists());
                for (Object candidate : events) {
                    String id = (String) ((Map<?, ?>) candidate).get("id");
                    assertEquals(1L, count(store, "SELECT count(*) FROM r31_runtime_fault WHERE event_id=?", id));
                }
                String receipt = (String) WriterHoldFaultsR31.snapshot().get("lastBatchReceiptKey");
                assertEquals(1L, count(store, "SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?", receipt));
                return;
            }

            long losses = stat("capacityLoss");
            for (int i = 0; i < 8 && stat("pending") > 0; i++) WriterHoldFaultsR31.drainControlSliceR32(store);
            long before = count(store, "SELECT count(*) FROM r31_runtime_fault WHERE operation='R342_HOLD'");
            boolean stageRestart = "stage-restart".equals(phase);
            int observed = stageRestart ? 4 : 26;
            observe("R342_HOLD", observed);
            assertEquals(!stageRestart, WriterHoldFaultsR31.deferResearch());
            assertEquals((long) observed, stat("pending"));
            assertEquals("DRAW_INGEST", DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R32_PREDRAW_SUFFIX").name());
            assertEquals("RESEARCH_DATA", DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R914_DRAW_HISTORY_BATCH").name());
            coordinator.runAtomicWriteOrThrowF01(schema("R342_FIXTURE_RENAME"), db -> {
                db.execSQL("ALTER TABLE r31_runtime_fault RENAME TO r342_saved_faults");
                return null;
            });
            WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals("MIGRATION_REQUIRED", WriterHoldFaultsR31.snapshot().get("drainStatus"));
            assertEquals((long) observed, stat("pending"));
            assertEquals(4L, stat("durableInFlightRows"));
            assertTrue(outbox.isFile());
            coordinator.runAtomicWriteOrThrowF01(schema("R342_FIXTURE_RESTORE"), db -> {
                db.execSQL("ALTER TABLE r342_saved_faults RENAME TO r31_runtime_fault");
                return null;
            });
            if (stageRestart) return;
            long wake = stat("nextDrainElapsedMs");
            SystemClock.sleep(Math.max(0L, wake - SystemClock.elapsedRealtime() + 20L));
            for (int i = 0; i < 10 && stat("pending") > 0; i++) WriterHoldFaultsR31.drainControlSliceR32(store);
            assertFalse(WriterHoldFaultsR31.deferResearch());
            assertEquals(0L, stat("pending"));
            assertEquals(losses, stat("capacityLoss"));
            assertEquals(before + 26, count(store, "SELECT count(*) FROM r31_runtime_fault WHERE operation='R342_HOLD'"));
        } finally {
            store.close();
        }
    }

    @SuppressWarnings("unchecked")
    public static void mainProjection(Context app) {
        DbStore store = open(app);
        try {
            observe("R342_PROJECTION", 25);
            assertTrue(WriterHoldFaultsR31.deferResearch());
            ServiceHeartbeatStore heartbeat = new ServiceHeartbeatStore(app);
            heartbeat.markActualProcessStart("R342_DIAGNOSTIC_PROJECTION");
            String raw = MainWriterFaultProjectionR342.capture(app, store.readable(), new long[]{1, 2, 3});
            assertNotNull(raw);
            assertTrue(store.trySetTelemetryStatesR77(java.util.Collections.singletonMap(MainWriterFaultProjectionR342.KEY, raw)));
            Map<String, Object> captured = MainWriterFaultProjectionR342.fromSnapshot(app, store.readable());
            assertEquals("OBSERVED_MAIN_OWNER", captured.get("observationState"));
            assertEquals(25L, captured.get("pending"));
            int producer = android.os.Process.myPid(), observer = producer + 1;
            long sampled = ((Number) captured.get("sampleElapsedMs")).longValue();
            String db = RuntimeObservationScopeR341.databaseIdentity(store.readable());
            String boot = (String) captured.get("bootId");
            String epoch = ProcessBootIdentityR906.id();
            Map<String, Object> other = MainWriterFaultProjectionR342.validate(raw, observer, producer, epoch, boot, db, sampled);
            assertEquals(producer, ((Number) other.get("producerPid")).intValue());
            assertEquals(observer, ((Number) other.get("observerPid")).intValue());
            assertEquals(captured.get("originalSha256"), other.get("originalSha256"));
            assertEquals(25L, other.get("pending"));
            assertEquals("UNKNOWN", MainWriterFaultProjectionR342.validate(raw, observer, 0, epoch, boot, db, sampled)
                    .get("observationState"));
            assertNull(MainWriterFaultProjectionR342.validate(raw, observer, producer, epoch, boot + "changed", db, sampled).get("pending"));
            assertNull(MainWriterFaultProjectionR342.validate(raw, observer, producer, epoch, boot, db, sampled + 12100).get("pending"));
            assertNull(MainWriterFaultProjectionR342.validate(raw, observer, producer, epoch + "changed", boot, db, sampled).get("pending"));
            Map<String, Object> tampered = new LinkedHashMap<>((Map<String, Object>) JsonR27.parse(raw));
            tampered.put("sha256", "0".repeat(64));
            assertNull(MainWriterFaultProjectionR342.validate(JsonR27.write(tampered), observer, producer, epoch, boot, db, sampled)
                    .get("pending"));
            for (int i = 0; i < 10 && stat("pending") > 0; i++) WriterHoldFaultsR31.drainControlSliceR32(store);
            assertEquals(0L, stat("pending"));
            assertEquals(25L, count(store, "SELECT count(*) FROM r31_runtime_fault WHERE operation='R342_PROJECTION'"));
        } finally {
            store.close();
        }
    }

    public static void coldAudit(Context app) throws Exception {
        DbStore store = open(app);
        try {
            observe("R342_ARCHIVE", 5);
            for (int i = 0; i < 8 && stat("pending") > 0; i++) WriterHoldFaultsR31.drainControlSliceR32(store);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(schema("R342_FIXTURE_QUOTA"), db -> {
                db.execSQL("UPDATE r31_runtime_fault SET byte_count=? WHERE operation='R342_ARCHIVE'", new Object[]{2 * 1024 * 1024});
                return null;
            });
            RuntimeFaultOriginalStoreR335 archive = new RuntimeFaultOriginalStoreR335();
            for (int i = 0; i < 4; i++) {
                RuntimeFaultArchiveB01.Page result = archive.runOne(store, SystemClock.elapsedRealtime() + 10_000L);
                assertEquals(RuntimeFaultOriginalStoreR335.processDetail(), RuntimeFaultArchiveB01.State.VERIFIED_RETIRED, result.state);
            }
            assertEquals(4L, count(store, "SELECT count(*) FROM r335_fault_original"));
            String corrupt;
            try (Cursor row = store.readable().rawQuery("SELECT original_hash FROM r335_fault_original ORDER BY event_id LIMIT 1", null)) {
                assertTrue(row.moveToFirst());
                corrupt = row.getString(0);
            }
            File damaged = RuntimeFaultOriginalStoreR335.path(app.getFilesDir(), corrupt);
            try (FileOutputStream out = new FileOutputStream(damaged, true)) { out.write(1); out.getFD().sync(); }
            RuntimeFaultOriginalStoreR335.auditSlice(store, SystemClock.elapsedRealtime() + 5000);
            Map<String, Object> intermediate = RuntimeFaultOriginalStoreR335.auditObservation(store.readable());
            assertEquals("IN_PROGRESS", intermediate.get("status"));
            assertNotEquals("", intermediate.get("afterEventId"));
            RuntimeFaultOriginalStoreR335.auditSlice(store, SystemClock.elapsedRealtime() + 5000);
            Map<String, Object> audited = RuntimeFaultOriginalStoreR335.auditObservation(store.readable());
            assertEquals(intermediate.get("cycleId"), audited.get("cycleId"));
            assertEquals("COMPLETE", audited.get("status"));
            assertEquals(3L, audited.get("verified"));
            assertEquals(1L, audited.get("unavailable"));
            assertEquals(0L, audited.get("pending"));
            assertTrue("failed verification must preserve the original", damaged.exists());
            assertEquals(4L, count(store, "SELECT count(*) FROM r335_fault_original"));
        } finally {
            store.close();
        }
    }
}
