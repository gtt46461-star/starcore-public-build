package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.r952.AtomCatalogImportCP12;
import com.ethk3.novaonepro.r952.AtomCatalogPinnedCP12;
import com.ethk3.novaonepro.r952.DevelopmentProductionCP12;
import com.ethk3.novaonepro.r952.ModernSeedFactoryCP12;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual packaged catalogs and production import owners; no training/qualification claim. */
@RunWith(AndroidJUnit4.class)
public final class FoundationImportAndroidCP12Test {
    private static long scalar(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) {
            assertTrue(c.moveToFirst());
            return c.getLong(0);
        }
    }

    private static String seedCheckpoint(SQLiteDatabase db) {
        StringBuilder snapshot = new StringBuilder();
        String sql = "SELECT scan_row,scan_offset,scan_prefix,hex(scan_sha_state),material_row,material_offset,"
                + "material_prefix,hex(material_sha_state),generation,state FROM "
                + ModernSeedFactoryCP12.CHECKPOINT + " ORDER BY source_hash";
        try (Cursor c = db.rawQuery(sql, null)) {
            while (c.moveToNext()) for (int i = 0; i < c.getColumnCount(); i++)
                snapshot.append('|').append(c.getString(i));
        }
        try (Cursor c = db.rawQuery("SELECT next_row,state,generation FROM "
                + ModernSeedFactoryCP12.IMPORT + " ORDER BY source_hash", null)) {
            while (c.moveToNext()) for (int i = 0; i < c.getColumnCount(); i++)
                snapshot.append('|').append(c.getString(i));
        }
        return snapshot.append("|rows=").append(scalar(db,
                "SELECT COUNT(*) FROM research_molecule_seed_v2")).toString();
    }

    private static boolean preWriteDeadline(IllegalStateException failure) {
        Throwable cause = failure.getCause();
        // These coordinator failures occur before task.call(); never absorb SQL/body failures.
        return failure.getMessage() != null
                && (failure.getMessage().startsWith("F01_WRITE_FAILED|operation=CP12_MOLECULE_")
                    || failure.getMessage().equals("F01_WRITE_FAILED|operation=CP12_ATOM_CATALOG_IMPORT"))
                && cause instanceof IllegalStateException
                && ("CP3_DB_BUSY_DEADLINE".equals(cause.getMessage())
                    || "CP3_DB_BUSY_DEADLINE_AFTER_ACQUIRE".equals(cause.getMessage()));
    }

    private static void assertSameAtomCursor(AtomCatalogImportCP12.ImportCursor before,
            AtomCatalogImportCP12.ImportCursor after) {
        assertEquals(before.row, after.row);
        assertEquals(before.offset, after.offset);
        assertEquals(before.generation, after.generation);
        assertEquals(before.prefixHash, after.prefixHash);
        assertEquals(before.state, after.state);
        assertArrayEquals(before.shaState(), after.shaState());
    }

    @Test public void completePinnedCatalogsThroughBoundedProductionOwners() {
        DbStore store = new DbStore(ApplicationProvider.getApplicationContext());
        long deadline = SystemClock.elapsedRealtime() + 600000L;
        try {
            while (!store.ensureResearchSidecarR811()) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(),
                        SystemClock.elapsedRealtime() < deadline);
                SystemClock.sleep(20L);
            }
            SQLiteDatabase db = store.readable();
            assertEquals("isolated cold registry", 0,
                    scalar(db, "SELECT COUNT(*) FROM research_atom_spec_v2"));
            AtomCatalogImportCP12.ImportCursor empty = AtomCatalogImportCP12.readCursorCP12(db);
            assertEquals("ATOMS_IMPORT_YIELD",
                    AtomCatalogImportCP12.drainOneCP12(store, System.nanoTime()));
            assertEquals(empty.row, AtomCatalogImportCP12.readCursorCP12(db).row);
            assertEquals(empty.prefixHash, AtomCatalogImportCP12.readCursorCP12(db).prefixHash);

            try (DatabaseCoordinator.WriterDeadlineScopeCP3 expired =
                    DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs() - 1L)) {
                boolean sawOriginalDeadline = false;
                // Preparation may cooperatively yield on a slow cold ART process.
                // Keep requesting one bounded page until an actual commit is attempted.
                while (!sawOriginalDeadline && SystemClock.elapsedRealtime() < deadline) {
                    try {
                        assertEquals("ATOMS_IMPORT_YIELD", AtomCatalogImportCP12.drainOneCP12(store,
                                System.nanoTime() + 250000000L));
                    } catch (IllegalStateException failure) {
                        if (!preWriteDeadline(failure)) throw failure;
                        assertEquals("F01_WRITE_FAILED|operation=CP12_ATOM_CATALOG_IMPORT", failure.getMessage());
                        sawOriginalDeadline = true;
                    }
                }
                assertTrue("original atom writer-deadline cause must remain observable", sawOriginalDeadline);
            }
            assertSameAtomCursor(empty, AtomCatalogImportCP12.readCursorCP12(db));
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM research_atom_spec_v2"));

            int atomPages = 0, atomWriterDeferrals = 0;
            while (!AtomCatalogImportCP12.completeCP12(db)) {
                AtomCatalogImportCP12.ImportCursor before = AtomCatalogImportCP12.readCursorCP12(db);
                long beforeRows = scalar(db, "SELECT COUNT(*) FROM research_atom_spec_v2");
                String result;
                try {
                    result = AtomCatalogImportCP12.drainOneCP12(store, System.nanoTime() + 250000000L);
                } catch (IllegalStateException failure) {
                    if (!preWriteDeadline(failure)) throw failure;
                    assertSameAtomCursor(before, AtomCatalogImportCP12.readCursorCP12(db));
                    assertEquals(beforeRows, scalar(db, "SELECT COUNT(*) FROM research_atom_spec_v2"));
                    atomWriterDeferrals++;
                    assertTrue("atom import must converge", SystemClock.elapsedRealtime() < deadline);
                    SystemClock.sleep(1000L);
                    continue;
                }
                AtomCatalogImportCP12.ImportCursor after = AtomCatalogImportCP12.readCursorCP12(db);
                assertTrue(result, after.row >= before.row);
                assertTrue(result, after.row - before.row <= AtomCatalogImportCP12.PAGE_ROWS);
                assertTrue(result, after.offset - before.offset <= AtomCatalogImportCP12.PAGE_BYTES);
                assertTrue(result, SystemClock.elapsedRealtime() < deadline);
                if (after.row > before.row) atomPages++;
            }
            AtomCatalogImportCP12.ImportCursor done = AtomCatalogImportCP12.readCursorCP12(db);
            assertEquals(AtomCatalogPinnedCP12.ROWS, done.row);
            assertEquals(AtomCatalogPinnedCP12.BYTES, done.offset);
            assertEquals(AtomCatalogPinnedCP12.SHA256, done.prefixHash);
            assertEquals(25350, scalar(db, "SELECT COUNT(*) FROM research_atom_spec_v2"));
            assertEquals(25350, scalar(db, "SELECT COUNT(DISTINCT spec_hash) FROM research_atom_spec_v2"));
            assertEquals("ATOMS_COMPLETE:25350",
                    AtomCatalogImportCP12.drainOneCP12(store, System.nanoTime() + 250000000L));
            assertEquals(done.generation, AtomCatalogImportCP12.readCursorCP12(db).generation);

            String beforeExpired = seedCheckpoint(db);
            try (DatabaseCoordinator.WriterDeadlineScopeCP3 expired =
                    DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs() - 1L)) {
                try {
                    ModernSeedFactoryCP12.maintain(store, System.nanoTime() + 250000000L);
                    fail("Expired writer deadline must not initialize the seed checkpoint");
                } catch (IllegalStateException failure) {
                    if (!preWriteDeadline(failure)) throw failure;
                }
            }
            assertEquals("expired write must leave all import state unchanged", beforeExpired, seedCheckpoint(db));

            int seedSlices = 0, writerDeferrals = 0;
            while (!ModernSeedFactoryCP12.completeCP12(db)) {
                String beforeSeed = seedCheckpoint(db);
                boolean deferred = false;
                ResearchWorkBudgetR927.Scope scope = ResearchWorkBudgetR927.enter(250L);
                try {
                    ModernSeedFactoryCP12.maintain(store, System.nanoTime() + 250000000L);
                } catch (ResearchWorkBudgetR927.Yield cooperativeYield) {
                    // The next slice must resume from the durable production cursor.
                } catch (DevelopmentProductionCP12.Wait wait) {
                    if (!"TIME_BUDGET_EXCEEDED".equals(wait.reason)) throw wait;
                } catch (IllegalStateException failure) {
                    if (!preWriteDeadline(failure)) throw failure;
                    assertEquals("pre-write failure must preserve the durable cursor and rows",
                            beforeSeed, seedCheckpoint(db));
                    writerDeferrals++;
                    deferred = true;
                } finally {
                    ResearchWorkBudgetR927.leave(scope);
                }
                // ResearchLearningContinuityR912 uses 1000ms backoff for owner ERROR outcomes.
                if (deferred) SystemClock.sleep(1000L);
                seedSlices++;
                assertTrue("seed import must converge: slices=" + seedSlices,
                        SystemClock.elapsedRealtime() < deadline);
            }
            assertEquals(4200, scalar(db, "SELECT COUNT(*) FROM research_molecule_seed_v2"));
            assertEquals(4200, scalar(db, "SELECT COUNT(DISTINCT seed_hash) FROM research_molecule_seed_v2"));
            assertEquals(0, scalar(db, "SELECT COUNT(*) FROM " + DbHelper.TABLE_TRADES));
            try (Cursor c = db.rawQuery("PRAGMA integrity_check", null)) {
                assertTrue(c.moveToFirst());
                assertEquals("ok", c.getString(0));
            }
            System.out.println("CP12_ANDROID_FOUNDATION_IMPORT_PASS|atoms=25350|seeds=4200"
                    + "|atom_pages=" + atomPages + "|seed_slices=" + seedSlices
                    + "|atom_writer_cause=PASS|atom_writer_deferrals=" + atomWriterDeferrals
                    + "|expired_writer_rollback=PASS|writer_deferrals=" + writerDeferrals
                    + "|training=NOT_CLAIMED|natural=NOT_PROVEN|physical_device=false");
        } finally {
            store.close();
        }
    }
}
