package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import org.junit.Test;

public final class R335ResearchDdlProgressTest {
    private static void need(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }

    private static long scalar(SQLiteDatabase db, String query) {
        try (Cursor c = db.rawQuery(query, null)) {
            need(c.moveToFirst(), "missing scalar");
            return c.getLong(0);
        }
    }

    @Test public void additiveObjectsResumeBetweenForegroundWrites() throws Exception {
        File dbFile = File.createTempFile("r335-research-ddl-", ".db",
                InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir());
        try { exercise(dbFile); }
        finally { SQLiteDatabase.deleteDatabase(dbFile); }
    }

    static void exercise(File dbFile) {
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            db.execSQL("CREATE TABLE prior_research_marker(id INTEGER PRIMARY KEY)");
            db.execSQL("INSERT INTO prior_research_marker VALUES(37)");
            long before = scalar(db, "PRAGMA schema_version");
            ResearchWorkBudgetR927.Scope slice = ResearchWorkBudgetR927.enter(1_000);
            RealtimePressureGateR811.beginAnalysis();
            try {
                DbHelper.prepareResearchObjectsR906(db);
                throw new AssertionError("preempted owner created research schema");
            } catch (ResearchWorkBudgetR927.Yield expected) {
                need(before == scalar(db, "PRAGMA schema_version"), "preempted callback changed schema");
            } finally {
                RealtimePressureGateR811.endAnalysis();
                ResearchWorkBudgetR927.leave(slice);
            }
        }
        int committed = 0;
        for (int i = 0; i < 60; i++) {
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
                long before = scalar(db, "PRAGMA schema_version");
                boolean ready = DbHelper.prepareResearchObjectsR906(db);
                long changed = scalar(db, "PRAGMA schema_version") - before;
                need(changed == (ready ? 0 : 1), "each callback may create one object");
                need(scalar(db, "SELECT id FROM prior_research_marker") == 37, "previous data lost");
                if (ready) {
                    need(committed > 20, "incomplete production definitions");
                    need(scalar(db, "SELECT count(*) FROM sqlite_master WHERE name IN "
                            + "('v57_algorithm_genome_r908','research_new_atom_proposal_v41',"
                            + "'research_new_asset_genome_v41','idx_r915_evolution_type')") == 4,
                            "required research definitions missing");
                    need(scalar(db, "SELECT count(*) FROM sqlite_master WHERE name IN "
                            + "('research_legacy_fixed_registry_v41','research_atom_schema_v41')") == 0,
                            "definition preparation published the immutable registries");
                    return;
                }
                committed++;
            }
        }
        throw new AssertionError("additive objects did not survive restart");
    }
}
