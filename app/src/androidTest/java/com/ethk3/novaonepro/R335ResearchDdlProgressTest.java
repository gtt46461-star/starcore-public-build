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
                    exercisePreparedJournal(dbFile);
                    return;
                }
                committed++;
            }
        }
        throw new AssertionError("additive objects did not survive restart");
    }

    private static void exercisePreparedJournal(File file) {
        SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
        String step = "R906_RESEARCH_R64_VERIFIED";
        int first;
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            SchemaDomainCoordinatorR926.prepareResearchStep(db, contract, step);
            first = SchemaDomainCoordinatorR926.preparedResearchAttempt(db, contract, step);
            need(first > 0 && !SchemaDomainCoordinatorR926.stepComplete(db, contract, step), "premature completion");
        }
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null)) {
            need(first == SchemaDomainCoordinatorR926.preparedResearchAttempt(db, contract, step), "lost prepared start");
            try {
                SchemaDomainCoordinatorR926.finishStartedStep(db, contract, step, x -> {
                    x.execSQL("CREATE TABLE rejected_research_target(n INTEGER)");
                    throw new IllegalStateException("injected publication failure");
                }, first);
                throw new AssertionError("failed publication succeeded");
            } catch (Exception expected) {
                need("injected publication failure".equals(expected.getMessage()), "unexpected failure");
            }
            need(scalar(db, "SELECT count(*) FROM sqlite_master WHERE name='rejected_research_target'") == 0, "rollback leaked target");
            SchemaDomainCoordinatorR926.prepareResearchStep(db, contract, step);
            int next = SchemaDomainCoordinatorR926.preparedResearchAttempt(db, contract, step);
            need(next > first, "failed attempt was reused");
            try {
                SchemaDomainCoordinatorR926.finishStartedStep(db, contract, step,
                        x -> { throw new AssertionError("retired writer ran"); }, first);
                SchemaDomainCoordinatorR926.finishStartedStep(db, contract, step,
                        x -> x.execSQL("CREATE TABLE accepted_research_target(n INTEGER)"), next);
            } catch (Exception failure) { throw new AssertionError(failure); }
            need(SchemaDomainCoordinatorR926.stepComplete(db, contract, step), "valid publication missing");
            need(scalar(db, "SELECT count(*) FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + " WHERE step_id='R906_RESEARCH_R64_VERIFIED' AND length(before_hash)=64 AND after_hash=before_hash") == 1,
                    "original catalog pair missing for unrelated test tables");
            need(scalar(db, "SELECT count(*) FROM sqlite_master WHERE name='accepted_research_target'") == 1,
                    "committed target missing");
        }
    }
}
