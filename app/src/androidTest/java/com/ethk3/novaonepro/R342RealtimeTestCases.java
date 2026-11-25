package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public final class R342RealtimeTestCases {
    private R342RealtimeTestCases() {}

    private static DbStore open(Context app) {
        assertTrue(app.getDatabasePath(AppConfig.DB_NAME).getAbsolutePath().contains("/r30-instrumentation/"));
        return new DbStore(app);
    }

    private static String issue(int sequence) {
        return String.format(Locale.ROOT, "20261125%04d", sequence);
    }

    private static Models.Draw draw(int sequence, long openAt) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue(sequence);
        draw.openTime = openAt;
        draw.fetchTime = openAt + 200L;
        for (int i = 0; i < 10; i++) draw.numbers[i] = (sequence + i) % 10 + 1;
        return draw;
    }

    private static void write(DbStore store, SchemaDomainCoordinatorR926.Step apply) {
        store.coordinatorR36().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(
                DatabaseCoordinator.WriteClass.MIGRATION, "R342_REALTIME_FIXTURE"), db -> {
                    apply.run(db);
                    return null;
                });
    }

    public static void historyReceipts(Context app) {
        long now = System.currentTimeMillis();
        List<Models.Draw> page = new ArrayList<>();
        for (int i = 1; i <= 12; i++) page.add(draw(i, now - 1000L - (12 - i) * 60000L));
        try (TestStore connection = new TestStore(app)) {
            DbStore store = connection.store;
            assertEquals(2, store.saveDraws(page.subList(10, 12)));
            assertEquals("TARGET_SUPERSEDED", store.savePredrawSuffixWithReceiptR342(page, issue(12)).state);
            DbStore.SuffixReceiptR342 incomplete = store.savePredrawSuffixWithReceiptR342(page.subList(3,12), issue(13));
            assertEquals("SOURCE_SUFFIX_INCOMPLETE", incomplete.state);
            assertEquals(2, incomplete.observedSuffix);
            assertEquals(0, incomplete.inserted);
            DbStore.SuffixReceiptR342 filled = store.savePredrawSuffixWithReceiptR342(page, issue(13));
            assertEquals("DURABLE_SUFFIX_READBACK", filled.state);
            assertEquals(12, filled.observedSuffix);
            assertTrue(filled.ready);
        }
        try (TestStore recovered = new TestStore(app)) {
            DbStore.SuffixReceiptR342 reread = recovered.store.savePredrawSuffixWithReceiptR342(page, issue(13));
            assertEquals("ALREADY_DURABLE", reread.state);
            assertEquals(0, reread.inserted);
            try (Cursor count = recovered.store.readable().rawQuery(
                    "SELECT count(*) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION, null)) {
                assertTrue(count.moveToFirst());
                assertEquals(0, count.getInt(0));
            }
        }
    }

    private static void shadow(SQLiteDatabase db, String target, String base, long at, boolean settled) {
        String hash = V51ChecksumEngine.sha256(target);
        db.execSQL("INSERT INTO " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                + "(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,"
                + "bundle_hash,contract_hash,authorization_hash,record_hash,created_at,commit_at,snapshot_hash,settled)"
                + " VALUES(?,?,?,1,1,2,3,'SHADOW','NO_BET','','','','',?,?,?,?,?)",
                new Object[]{target, ReleaseManifestR926.GAME_CODE, base, hash, at, at, hash, settled ? 1 : 0});
    }

    public static void coverageAndMissingSource(Context app) {
        try (TestStore connection = new TestStore(app)) {
            DbStore store = connection.store;
            long now = System.currentTimeMillis();
            long openAt = now - 1000L;
            List<Models.Draw> draws = new ArrayList<>();
            for (int i = 2; i <= 31; i++) draws.add(draw(i, openAt - (31 - i) * 60000L));
            assertEquals(30, store.saveDraws(draws));
            write(store, db -> {
                shadow(db, issue(1), "202611240000", openAt - 1L, false);
                shadow(db, issue(32), issue(31), openAt + 10L, false);
                shadow(db, issue(33), issue(32), openAt + 20L, false);
                shadow(db, issue(30), issue(29), openAt - 61000L, true);
                shadow(db, issue(31), issue(30), openAt - 100L, true);
                for (int sequence : new int[]{2, 30, 31}) {
                    String target = issue(sequence);
                    RuntimeEventLedgerR926.Event event = RuntimeEventLedgerR926.Event.of(
                            target, "epoch", "attempt", "CANONICAL", "FIXTURE",
                            RuntimeEventLedgerR926.RANK_CANONICAL, "", "R342_FIXTURE", target);
                    assertTrue(RuntimeEventLedgerR926.appendWithinTransaction(db, event));
                    long sample = openAt - (31 - sequence) * 60000L - 100L;
                    db.execSQL("UPDATE " + RuntimeObservationScopeR341.EVENT_SCOPE
                                    + " SET sample_wall_at=?,source_commit=? WHERE event_id=?",
                            new Object[]{sample, sequence == 30 ? "older-source" : BuildConfig.SOURCE_COMMIT_R26,
                                    event.eventId()});
                }
            });
            RuntimeHealthLedgerR36.BusinessTruth truth = new RuntimeHealthLedgerR36.BusinessTruth();
            truth.latestDrawIssue = issue(31);
            truth.latestCanonicalIssue = issue(32);
            truth.expectedSettledIssue = issue(31);
            truth.latestSettledIssue = issue(31);
            truth.latestOosIssue = issue(31);
            truth.businessAlive = true;
            RuntimeHealthLedgerR36.readCoverageR342(null, store.readable(), truth);
            RuntimeHealthLedgerR36.classifyBusinessR77(truth, 1);
            assertEquals(truth.canonical(), 30, truth.coverageEligible);
            assertEquals(truth.canonical(), 1, truth.coverageFrozen);
            assertEquals(0, truth.drawToSettledLag);
            assertEquals("PREDRAW_COVERAGE_MISSING", truth.rootCause);
            SettlementBacklogAuditR3611.Summary audit = SettlementBacklogAuditR3611.scan(store, 20);
            assertEquals(audit.compact(), 1, audit.drawSourceMissing);
            assertEquals(audit.compact(), 1, audit.drawSourceUnknown);
            assertEquals(audit.compact(), 1, audit.waitDraw);
            assertEquals(issue(1), audit.sourceGaps.get(0).get("targetIssue"));
        }
    }

    public static void deferredLeaseAndOriginal(Context app) {
        byte[] original = new byte[137000];
        new Random(124421L).nextBytes(original);
        String target = issue(17);
        String incorrectHash = V51ChecksumEngine.sha256("different snapshot");
        long now = System.currentTimeMillis();
        try (TestStore connection = new TestStore(app)) {
            write(connection.store, db -> db.execSQL("INSERT INTO " + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                            + "(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,"
                            + "snapshot_hash,lease_owner,lease_until) VALUES(?,'FULL','RUNNING',?,?,?,?,?,'stopped-owner',?)",
                    new Object[]{target, now, now, incorrectHash, original, incorrectHash, now - 1L}));
        }
        try (TestStore recovered = new TestStore(app)) {
            DbStore store = recovered.store;
            assertEquals(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,
                    DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R36_DEFERRED_CLAIM"));
            DeferredEvidenceWriterR36.WriteResult result = DeferredEvidenceWriterR36.writeOne(store, target, "current-owner");
            assertFalse(result.success);
            assertTrue(result.status, result.status.contains("SNAPSHOT_HASH_MISMATCH"));
            try (Cursor row = store.readable().rawQuery("SELECT status,attempts,core_materialized,"
                    + "next_action_ordinal,snapshot_blob,last_error,lease_owner FROM "
                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB + " WHERE target_issue=?", new String[]{target})) {
                assertTrue(row.moveToFirst());
                assertEquals("FAILED_TERMINAL", row.getString(0));
                assertEquals(1, row.getInt(1));
                assertEquals(0, row.getInt(2));
                assertEquals(0, row.getInt(3));
                assertArrayEquals(original, row.getBlob(4));
                assertTrue(row.getString(5).contains("SNAPSHOT_HASH_MISMATCH"));
                assertEquals("", row.getString(6));
            }
            assertEquals("NOT_CLAIMED", DeferredEvidenceWriterR36.writeOne(store, target, "retry").status);
        }
    }

    private static final class TestStore implements AutoCloseable {
        final DbStore store;
        TestStore(Context app) { store = open(app); }
        @Override public void close() { store.close(); }
    }
}
