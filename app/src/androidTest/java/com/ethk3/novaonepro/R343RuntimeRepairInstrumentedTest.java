package com.ethk3.novaonepro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.StartupCheckpointIdentityR31;
import com.ethk3.novaonepro.strategyv2.StartupProbePolicyR343;
import java.io.File;
import java.util.UUID;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Disposable real Android SQLite tests; never use the installed user's database. */
@RunWith(AndroidJUnit4.class)
public final class R343RuntimeRepairInstrumentedTest {
    @Test public void legitimateTempCacheFlushDoesNotBlockCheckpoint() throws Exception {
        Context app = ApplicationProvider.getApplicationContext();
        File path = new File(app.getCacheDir(), "r343-startup-" + UUID.randomUUID() + ".db");
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(path, null)) {
            db.enableWriteAheadLogging();
            db.beginTransaction();
            try {
                db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,trained_through INTEGER,state_hash TEXT,artifact BLOB,updated_at INTEGER)");
                for (String ddl : StartupCheckpointIdentityR31.ddlR31()) db.execSQL(ddl);
                StartupCheckpointIdentityR31.seedWithinMigrationR31(db);
                db.execSQL("CREATE TEMP TABLE IF NOT EXISTS r31q_ddl_cache_flush(x INTEGER)");
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            StartupCheckpointIdentityR31.Prepared prepared = StartupCheckpointIdentityR31.prepare(db, null);
            assertEquals(0, prepared.sourceRows);
            db.beginTransaction();
            try {
                prepared.requireWithin(db);
                db.execSQL("CREATE TEMP TABLE r29_model_checkpoint(owner TEXT)");
                try {
                    prepared.requireWithin(db);
                    fail("Unexpected TEMP shadow must remain blocked");
                } catch (IllegalStateException blocked) {
                    assertTrue(blocked.getMessage().startsWith("R31_STARTUP_CHECKPOINT_TEMP_SCHEMA:"));
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        } finally { SQLiteDatabase.deleteDatabase(path); }
    }

    private static final class Clock implements RuntimeObservationScopeR341.Clock {
        long elapsed = 1000L;
        @Override public long wall() { return 1_700_000_000_000L + elapsed; }
        @Override public long monotonic() { return elapsed; }
    }

    @Test public void circuitProbeIsDurableAndCannotHotLoop() {
        Context app = ApplicationProvider.getApplicationContext();
        Clock clock = new Clock();
        String source = "0123456789012345678901234567890123456789";
        String hash = "0123456789012345678901234567890123456789012345678901234567890123";
        RuntimeObservationScopeR341 scope = new RuntimeObservationScopeR341(hash, source,
                "test-process", "test-boot", "test-db", clock.wall(), clock.monotonic(), null);
        File root = new File(app.getCacheDir(), "r343-circuit-" + UUID.randomUUID());
        StartupStageJournalR341 journal = StartupStageJournalR341.open(root,"research",clock,scope);
        for (int i=0; i<3; i++) {
            StartupStageJournalR341.Token begin = journal.begin("owner","attempt-"+i,
                    StartupStageJournalR341.Stage.OWNER_BEGIN,hash);
            assertTrue(journal.recordUncaught(begin,new IllegalStateException("FIXTURE_DETERMINISTIC_BUG")).durable);
        }
        assertTrue(journal.hasDeterministicFailureLimit(hash));
        assertFalse(journal.reserveDeterministicProbeR343(hash));
        clock.elapsed += StartupProbePolicyR343.RETRY_MS;
        assertTrue(journal.reserveDeterministicProbeR343(hash));
        assertFalse(journal.reserveDeterministicProbeR343(hash));
        StartupStageJournalR341 reopened = StartupStageJournalR341.open(root,"research",clock,scope);
        assertFalse(reopened.reserveDeterministicProbeR343(hash));
        // Real repeated BUG outcomes do not become READY and renew the cooldown.
        for (int probe=2; probe<=StartupProbePolicyR343.MAX_PROBES_PER_DEPENDENCY; probe++) {
            StartupStageJournalR341.Token failed = reopened.begin("owner","probe-failed-"+probe,
                    StartupStageJournalR341.Stage.OWNER_BEGIN,hash);
            assertTrue(reopened.recordUncaught(failed,new IllegalStateException("FIXTURE_DETERMINISTIC_BUG")).durable);
            assertTrue(reopened.hasDeterministicFailureLimit(hash));
            assertFalse(reopened.reserveDeterministicProbeR343(hash));
            clock.elapsed += StartupProbePolicyR343.RETRY_MS;
            assertTrue(reopened.reserveDeterministicProbeR343(hash));
        }
        // Force journal rotation with unrelated stages. Failure history and the
        // finite reservation counter must survive compaction and process reopen.
        for (int i=0; i<400; i++) {
            StartupStageJournalR341.Token noise = reopened.begin("fixture","noise-"+i,
                    StartupStageJournalR341.Stage.RUNTIME_BASE,hash);
            assertTrue(reopened.end(noise,StartupStageJournalR341.Outcome.VERIFIED,"fixture",0L).durable);
        }
        StartupStageJournalR341 rotated = StartupStageJournalR341.open(root,"research",clock,scope);
        assertTrue(rotated.hasDeterministicFailureLimit(hash));
        clock.elapsed += StartupProbePolicyR343.RETRY_MS;
        assertFalse(rotated.reserveDeterministicProbeR343(hash));
        assertEquals("PROBE_LIMIT_REACHED_DEPENDENCY_CHANGE_REQUIRED", rotated.deterministicProbeStatusR343());
    }
}
