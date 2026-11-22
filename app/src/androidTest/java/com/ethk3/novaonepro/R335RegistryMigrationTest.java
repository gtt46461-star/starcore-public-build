package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import org.junit.Test;

public final class R335RegistryMigrationTest {
    private static void need(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    private static long count(SQLiteDatabase db, String query) {
        try (Cursor c = db.rawQuery(query, null)) {
            need(c.moveToFirst(), "missing scalar");
            return c.getLong(0);
        }
    }

    @Test public void signedRegistriesResumeAndPublishTogether() throws Exception {
        File file = File.createTempFile("r335-seed-", ".db", InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try { exercise(file); }
        finally { SQLiteDatabase.deleteDatabase(file); }
    }

    static void exercise(File file) {
        String legacy = AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY;
        String atoms = AssetGenesisEngineR915.TABLE_ATOM_SCHEMA;
        String pending = legacy + "_r335_pending";
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE v57_algorithm_genome_r908(genome_id TEXT PRIMARY KEY)");
            db.execSQL("INSERT INTO v57_algorithm_genome_r908 VALUES('preserved')");
            int columns = 0;
            while (!DbHelper.prepareExistingGenomeColumn(db)) {
                need(++columns <= 20, "column unit did not advance");
                need(count(db, "SELECT count(*) FROM v57_algorithm_genome_r908 WHERE genome_id='preserved'") == 1, "old genome lost");
            }
            need(columns == 20, "missing old columns");
            need(!AssetGenesisEngineR915.prepareRegistrySlice(db), "published partial first slice");
            long first = count(db, "SELECT count(*) FROM " + pending);
            need(first > 0 && first < LegacyFixedAssetRegistryR915.entries().length, "unbounded first slice");
            ResearchWorkBudgetR927.Scope scope = ResearchWorkBudgetR927.enter(2000);
            RealtimePressureGateR811.beginAnalysis();
            boolean interrupted = false;
            try { AssetGenesisEngineR915.prepareRegistrySlice(db); }
            catch (ResearchWorkBudgetR927.Yield expected) { interrupted = true; }
            finally { RealtimePressureGateR811.endAnalysis(); ResearchWorkBudgetR927.leave(scope); }
            need(interrupted && count(db, "SELECT count(*) FROM " + pending) == first, "lost progress on preemption");
            db.execSQL("UPDATE " + pending + " SET consumer='damaged-pending-row' WHERE rowid=1");
        }
        boolean ready = false;
        for (int i = 0; i < 40 && !ready; i++) {
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                ready = AssetGenesisEngineR915.prepareRegistrySlice(db);
                need(count(db, "SELECT count(*) FROM sqlite_master WHERE name='" + legacy + "'") == 0, "partial registry exposed");
                need(count(db, "SELECT count(*) FROM " + pending + " WHERE consumer='damaged-pending-row'") == 0, "damaged prefix reused");
            }
        }
        need(ready, "no progress across reopen");
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            String atomStage = atoms + "_r335_pending";
            long atomCount = GenesisSchemaRegistryR915.atoms().size();
            // Count stays unchanged in all three cases. Publication must validate every field
            // and row position, including a displaced last row and a row before position one.
            db.execSQL("UPDATE " + atomStage + " SET rowid=? WHERE rowid=?",
                    new Object[] {atomCount + 17, atomCount});
            rejectDamagedStage(db);
            need(!AssetGenesisEngineR915.prepareRegistrySlice(db), "missing tail was accepted");
            need(count(db, "SELECT max(rowid) FROM " + atomStage) == atomCount, "surplus row survived");
            db.execSQL("UPDATE " + atomStage + " SET window_spec=window_spec||char(39) WHERE rowid=23");
            rejectDamagedStage(db);
            need(!AssetGenesisEngineR915.prepareRegistrySlice(db), "changed field was accepted");
            need(count(db, "SELECT count(*) FROM " + atomStage) == 46, "verified prefix was lost");
            finishStage(db);
            db.execSQL("UPDATE " + pending + " SET rowid=-4 WHERE rowid=9");
            rejectDamagedStage(db);
            need(!AssetGenesisEngineR915.prepareRegistrySlice(db), "nonpositive row was accepted");
            need(count(db, "SELECT min(rowid) FROM " + pending) == 1, "nonpositive row survived");
            finishStage(db);
            db.beginTransaction();
            try { AssetGenesisEngineR915.ensureSchema(db, true); }
            finally { db.endTransaction(); }
            need(count(db, "SELECT count(*) FROM sqlite_master WHERE name='" + legacy + "'") == 0, "rollback leaked publication");
            need(count(db, "SELECT count(*) FROM " + pending) == LegacyFixedAssetRegistryR915.entries().length, "rollback lost stage");
            db.beginTransaction();
            try { AssetGenesisEngineR915.ensureSchema(db, true); db.setTransactionSuccessful(); }
            finally { db.endTransaction(); }
            need(count(db, "SELECT count(*) FROM " + legacy + " WHERE locked=1 AND registered_ms>0") == LegacyFixedAssetRegistryR915.entries().length, "incomplete legacy manifest");
            need(count(db, "SELECT count(*) FROM " + atoms) == GenesisSchemaRegistryR915.atoms().size(), "incomplete atom manifest");
            need(count(db, "SELECT count(*) FROM " + pending) == 0, "legacy staging rows retained");
            need(count(db, "SELECT count(*) FROM " + atoms + "_r335_pending") == 0, "atom staging rows retained");
        }
    }

    private static void rejectDamagedStage(SQLiteDatabase db) {
        boolean rejected = false;
        db.beginTransaction();
        try {
            AssetGenesisEngineR915.ensureSchema(db, true);
        } catch (IllegalStateException mismatch) {
            rejected = mismatch.getMessage() != null &&
                    mismatch.getMessage().startsWith("R335_REGISTRY_STAGE_MISMATCH:");
            if (!rejected) throw mismatch;
        } finally { db.endTransaction(); }
        need(rejected, "damaged stage was publishable");
    }

    private static void finishStage(SQLiteDatabase db) {
        for (int i = 0; i < 40; i++)
            if (AssetGenesisEngineR915.prepareRegistrySlice(db)) return;
        throw new AssertionError("repair did not converge");
    }
}
