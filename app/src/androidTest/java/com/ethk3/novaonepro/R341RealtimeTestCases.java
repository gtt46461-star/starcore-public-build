package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class R341RealtimeTestCases {
    private static final String BASE = "202611240012";
    private static final String TARGET = "202611240013";
    private static final String OWNER = "r341-engineering-fixture";
    private R341RealtimeTestCases() {}

    private static DbStore open(Context app) {
        assertTrue(app.getDatabasePath(AppConfig.DB_NAME).getAbsolutePath().contains("/r30-instrumentation/"));
        return new DbStore(app);
    }

    private static long number(SQLiteDatabase db, String sql, String...args) {
        try (Cursor rows = db.rawQuery(sql, args.length == 0 ? null : args)) {
            assertTrue(rows.moveToFirst());
            return rows.getLong(0);
        }
    }

    private static void write(DbStore store, SchemaDomainCoordinatorR926.Step body) {
        store.coordinatorR36().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(
                DatabaseCoordinator.WriteClass.MIGRATION, "R341_REALTIME_FIXTURE"), db -> { body.run(db); return null; });
    }

    private static void preparePartial(DbStore store) {
        write(store, db -> {
            db.execSQL("INSERT INTO " + LegacyPredrawContextBridgeR915.TABLE
                    + " VALUES(?,1,'teacher','why','reverse','causal','feedback','trend','shape','relation','legacy',1,'fixture-preserved')",
                    new Object[]{BASE});
            db.execSQL("DROP TABLE " + LegacyPredrawContextBridgeR915.POST_TABLE);
            db.execSQL("DELETE FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL + " WHERE step_id=?",
                    new Object[]{SchemaDomainCoordinatorR926.LEGACY_PREDRAW_CRITICAL_STEP_R341});
            SchemaDomainCoordinatorR926.verifyAndStamp(db, SchemaDomainCoordinatorR926.previousCriticalContractR341());
        });
    }

    private static void migrate(DbStore store) throws Exception {
        try (DatabaseCoordinator.WriterGuardCP3 ignored = DatabaseCoordinator.acquireWriterGuardCP3()) {
            SchemaDomainCoordinatorR926.migrateLegacyPredrawCriticalFromVerifiedStampR341(store.writable());
        }
    }

    public static void migrateKnownPartial(Context app) throws Exception {
        DbStore store = open(app);
        try {
            LegacyPredrawContextBridgeR915.requireReady(store.readable());
            assertTrue(store.coordinatorR36().isCriticalSchemaReady());
            preparePartial(store);
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(), SchemaDomainCoordinatorR926.previousCriticalContractR341()));
            migrate(store);
            long schema = number(store.readable(), "PRAGMA schema_version");
            migrate(store);
            LegacyPredrawContextBridgeR915.requireReady(store.readable());
            assertEquals(schema, number(store.readable(), "PRAGMA schema_version"));
            assertEquals("fixture-preserved", LegacyPredrawContextBridgeR915.read(store.readable(), BASE).recordHash);
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(), SchemaDomainCoordinatorR926.criticalContract()));
        } finally { store.close(); }
    }

    public static void rollbackKnownSchemaUnit(Context app) throws Exception {
        DbStore store = open(app);
        try {
            preparePartial(store);
            SchemaDomainCoordinatorR926.installCutPointsR31((domain, step, point) -> {
                if (SchemaDomainCoordinatorR926.LEGACY_PREDRAW_CRITICAL_STEP_R341.equals(step) && "STEP_BODY_DONE".equals(point))
                    throw new IllegalStateException("INJECTED_SAME_TRANSACTION_ROLLBACK");
            });
            try {
                migrate(store);
                fail("Injected migration finished");
            } catch (IllegalStateException injected) {
                assertTrue(injected.getMessage().contains("INJECTED"));
            } finally { SchemaDomainCoordinatorR926.installCutPointsR31(null); }
            assertEquals(0L, number(store.readable(), "SELECT count(*) FROM sqlite_master WHERE name=?", LegacyPredrawContextBridgeR915.POST_TABLE));
            assertEquals("fixture-preserved", LegacyPredrawContextBridgeR915.read(store.readable(), BASE).recordHash);
            migrate(store);
            LegacyPredrawContextBridgeR915.requireReady(store.readable());
            assertEquals(1L, number(store.readable(), "SELECT count(*) FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + " WHERE step_id=? AND status='COMPLETED'", SchemaDomainCoordinatorR926.LEGACY_PREDRAW_CRITICAL_STEP_R341));
        } finally { store.close(); }
    }

    public static void refuseUnrecognizedDrift(Context app) throws Exception {
        DbStore store = open(app);
        try {
            preparePartial(store);
            write(store, db -> db.execSQL("ALTER TABLE draws ADD COLUMN r341_unknown_drift INTEGER"));
            try {
                migrate(store);
                fail("Critical drift was stamped as an allowed delta");
            } catch (IllegalStateException conflict) { assertTrue(conflict.getMessage().contains("UNRECOGNIZED_CRITICAL_STAMP")); }
            assertEquals(0L, number(store.readable(), "SELECT count(*) FROM sqlite_master WHERE name=?", LegacyPredrawContextBridgeR915.POST_TABLE));
            assertEquals(1L, number(store.readable(), "SELECT count(*) FROM " + LegacyPredrawContextBridgeR915.TABLE + " WHERE record_hash='fixture-preserved'"));
        } finally { store.close(); }
    }

    private static Models.Draw draw(int sequence, long openAt) {
        Models.Draw draw = new Models.Draw();
        draw.issue = String.format(Locale.ROOT, "20261124%04d", sequence);
        draw.openTime = openAt;
        draw.fetchTime = openAt + 200L;
        for (int i = 0; i < 10; i++) draw.numbers[i] = (sequence + i) % 10 + 1;
        return draw;
    }

    private static List<Models.Draw> history(long now) {
        List<Models.Draw> rows = new ArrayList<>();
        for (int i = 1; i <= 12; i++) rows.add(draw(i, now - 1000L - (12 - i) * 60000L));
        return rows;
    }

    private static AnalysisSingleFlightR36.Request request(String base, String target, long generation, long now, long deadline) {
        return new AnalysisSingleFlightR36.Request(base, target, generation, now, deadline, OWNER,
                V51ChecksumEngine.sha256(base + "|" + target + "|" + generation), "");
    }

    public static void exactHistoryRoundTrip(Context app) {
        DbStore store = open(app);
        try {
            long now = System.currentTimeMillis();
            List<Models.Draw> rows = history(now);
            assertEquals(12, store.saveDraws(rows));
            AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
            AnalysisSingleFlightR36.Request request = requests.ensurePending(BASE, TARGET,
                    TargetIssueResolver.predrawDeadlineWallR20261008(rows.get(11)), OWNER);
            AnalysisRequestStoreR36.ClaimResult claim = requests.claimDetailed(request, OWNER, System.currentTimeMillis());
            assertTrue(claim.code, claim.claimed);
            String database = RuntimeObservationScopeR341.databaseIdentity(store.readable());
            AnalysisInputEnvelopeR341 pinned = AnalysisInputEnvelopeR341.create(
                    store.getDrawsBeforeIssue(TARGET, 240), claim.request, OWNER, database);
            AnalysisSingleFlightR36.Request bound = requests.bindHistoryHash(claim.request, pinned, OWNER);
            AnalysisInputEnvelopeR341 reread = requests.readBoundInput(bound);
            assertNotNull(reread);
            assertArrayEquals(pinned.encode(), reread.encode());
            assertEquals(bound.historyHash, reread.input.historyHash);
            assertEquals(12, reread.input.history.size());
            assertEquals(1L, number(store.readable(), "SELECT count(*) FROM " + AnalysisInputEnvelopeR341.TABLE));
            assertEquals(bound.historyHash, requests.bindHistoryHash(bound, reread, OWNER).historyHash);
        } finally { store.close(); }
    }

    public static void denyChangedHistoryIdentity(Context app) {
        DbStore store = open(app);
        try {
            long now = System.currentTimeMillis();
            List<Models.Draw> rows = history(now);
            String database = RuntimeObservationScopeR341.databaseIdentity(store.readable());
            AnalysisSingleFlightR36.Request request = request(BASE, TARGET, 1, now, now + 60000L);
            AnalysisInputEnvelopeR341 first = AnalysisInputEnvelopeR341.create(rows, request, OWNER, database);
            Collections.reverse(rows);
            assertArrayEquals(first.encode(), AnalysisInputEnvelopeR341.create(rows, request, OWNER, database).encode());
            byte[] encoded = first.encode();
            encoded[encoded.length - 1] = 42;
            assertThrows(IllegalStateException.class, () -> AnalysisInputEnvelopeR341.decode(encoded, request, database));
            assertThrows(IllegalStateException.class, () -> AnalysisInputEnvelopeR341.decode(first.encode(),
                    request(BASE,TARGET,2,now,now+60000L),database));
        } finally { store.close(); }
    }

    public static void onlyOneAnalysisOwner(Context app) {
        long now = System.currentTimeMillis();
        AnalysisSingleFlightR36 gate = AnalysisSingleFlightR36.shared();
        AnalysisSingleFlightR36.Lease active = gate.submit(request(BASE,TARGET,1,now,now+60000L)).lease;
        try {
            AnalysisSingleFlightR36.SubmitResult successor = gate.submit(request(TARGET,"202611240014",2,now,now+120000L));
            assertTrue(successor.queued);
            assertTrue(active.token.supersededByNewerTarget());
            assertEquals(1, gate.activeCount());
            assertEquals("202611240014", gate.releaseAndTakeNext(active).targetIssue);
        } finally { gate.releaseAndTakeNext(active); }
    }

    public static void expiredAnalysisNeverOwnsWriter(Context app) {
        long now = System.currentTimeMillis();
        AnalysisSingleFlightR36 gate = AnalysisSingleFlightR36.shared();
        assertThrows(IllegalStateException.class, () -> gate.submit(request(BASE,TARGET,1,now-60000L,now-1000L)));
        assertEquals(0, gate.activeCount());
        DbStore store = open(app);
        try {
            DatabaseCoordinator.TransactionResult<Boolean> missed = store.coordinatorR36().runCanonicalTransactionR36(
                    TARGET, "expired-fixture", SystemClock.elapsedRealtime() - 1L,
                    (db,tx) -> { throw new AssertionError("expired transaction reached the canonical writer"); }, false);
            assertFalse(missed.success);
        } finally { store.close(); }
    }

    public static void canonicalRollback(Context app) {
        R36DatabaseAtomicityTest inherited = new R36DatabaseAtomicityTest();
        inherited.openThroughProductionStartupOwner();
        inherited.coordinatorRollsBackInjectedCanonicalFailure();
    }

    public static void missingCanonicalProducesNoLearning(Context app) {
        DbStore store = open(app);
        try {
            DatabaseCoordinator.TransactionResult<Integer> result = store.coordinatorR36().runSettlementTransactionR36(
                    TARGET, V51ChecksumEngine.sha256("exact-fixture-draw"), (db,tx) -> {
                        RuntimeEventHooksR926.settledWithinTransaction(db,TARGET,V51ChecksumEngine.sha256("excluded"),
                                SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL,V51ChecksumEngine.sha256("exact-fixture-draw"));
                        return 1;
                    }, 0);
            assertTrue(result.success);
            assertEquals(1L, number(store.readable(), "SELECT count(*) FROM " + RuntimeEventLedgerR926.TABLE_EVENT
                    + " WHERE target_issue=? AND event_type='SETTLED'",TARGET));
            assertEquals(0L, number(store.readable(), "SELECT count(*) FROM " + RuntimeEventLedgerR926.TABLE_EVENT
                    + " WHERE target_issue=? AND event_type='LEARNING_PENDING'",TARGET));
        } finally { store.close(); }
    }

    public static void deferredAdmissionBound(Context app) {
        DbStore store = open(app);
        try {
            DatabaseCoordinator.TransactionResult<Integer> refused = store.coordinatorR36().runDeferredTransactionR36(
                    TARGET, "oversize-fixture", 65, 65537L,
                    (db,tx) -> { throw new AssertionError("oversize deferred slice entered the sole writer"); }, 0);
            assertFalse(refused.success);
            assertTrue(refused.code,refused.code.contains("RESOURCE_WAIT"));
        } finally { store.close(); }
    }

    public static void deferredReplayDoesNotReexecute(Context app) {
        DbStore store = open(app);
        try {
            write(store, db -> db.execSQL("CREATE TABLE r341_deferred_cursor_probe(value INTEGER NOT NULL)"));
            DatabaseCoordinator owner = store.coordinatorR36();
            assertTrue(owner.runDeferredTransactionR36(TARGET,"cursor-0",3,128L,(db,tx) -> {
                db.execSQL("INSERT INTO r341_deferred_cursor_probe VALUES(1)");
                return 1;
            },0).success);
            DatabaseCoordinator.TransactionResult<Integer> replay = owner.runDeferredTransactionR36(TARGET,"cursor-0",3,128L,
                    (db,tx) -> {throw new AssertionError("deferred slice replay duplicated a write");},0);
            assertTrue(replay.success);
            assertTrue(replay.replay);
            assertEquals(1L,number(store.readable(),"SELECT count(*) FROM r341_deferred_cursor_probe"));
        } finally { store.close(); }
    }

    public static void rawBindingIsFirstWrite(Context app) {
        Models.ApiResult raw = new Models.ApiResult();
        raw.rawJson = "first-body";
        FetchLedgerR925.bindRawResultR82(raw,"engineering-first");
        FetchLedgerR925.bindRawResultR82(raw,"engineering-first");
        assertThrows(IllegalStateException.class, () -> FetchLedgerR925.bindRawResultR82(raw,"engineering-foreign"));
        raw.rawJson = "mismatched-body";
        assertThrows(IllegalStateException.class, () -> FetchLedgerR925.bindRawResultR82(raw,"engineering-first"));
    }

    public static void duplicatePreservesFirstClock(Context app) {
        DbStore store = open(app);
        try {
            long now = System.currentTimeMillis();
            Models.Draw first = draw(12,now-2000L);
            assertTrue(store.saveDraw(first) > 0L);
            long observed = number(store.readable(),"SELECT first_seen_at FROM draws WHERE issue=?",BASE);
            Models.Draw later = first.copy();
            later.fetchTime += 300L;
            assertTrue(store.saveDraw(later) <= 0L);
            assertEquals(observed,number(store.readable(),"SELECT first_seen_at FROM draws WHERE issue=?",BASE));
            assertArrayEquals(first.numbers,store.getDrawByIssue(BASE).numbers);
        } finally { store.close(); }
    }

    public static void completeSuffixRequiresCleanHistory(Context app) {
        List<Models.Draw> rows = history(System.currentTimeMillis());
        assertEquals(12, R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(rows, BASE).size());
        rows.get(7).dirty = 1;
        assertTrue(R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(rows, BASE).isEmpty());
        assertEquals(4,R36AnalysisHistoryPolicyR364.contiguousSuffixCount(rows,BASE));
    }
}
