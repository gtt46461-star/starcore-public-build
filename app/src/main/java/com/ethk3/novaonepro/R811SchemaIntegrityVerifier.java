package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only verifier for the DB36/R811 same-version additive extension. */
public final class R811SchemaIntegrityVerifier {
    private R811SchemaIntegrityVerifier() {}

    public static Report verifyCritical(SQLiteDatabase db) {
        ArrayList<String> errors = new ArrayList<String>();
        requireUserVersion36(db, errors);
        requireColumns(
                db,
                DbHelper.TABLE_R36_SETTLEMENT_TASK,
                new String[] {"task_class", "priority", "terminal_code", "source"},
                errors);
        requireColumns(
                db,
                DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                new String[] {"core_materialized", "next_action_ordinal", "cursor_version"},
                errors);
        requireIndex(db, "idx_r811_settlement_runnable", errors);
        requireIndex(db, "idx_r811_deferred_runnable", errors);
        if (!tableExists(db, DbHelper.TABLE_R811_DRAW_PROVENANCE))
            errors.add("TABLE:" + DbHelper.TABLE_R811_DRAW_PROVENANCE);
        if (!tableExists(db, DbHelper.TABLE_R811_DRAW_CANONICAL))
            errors.add("TABLE:" + DbHelper.TABLE_R811_DRAW_CANONICAL);
        requireIndex(db, "idx_r811_draw_provenance_issue", errors);
        String revision = state(db, "r811_schema_revision");
        if (!String.valueOf(AppConfig.R811_SCHEMA_REVISION).equals(revision))
            errors.add("REVISION:" + revision);
        return new Report(errors);
    }

    public static Report verifyResearch(SQLiteDatabase db) {
        ArrayList<String> errors = new ArrayList<String>();
        String[] tables =
                new String[] {
                    "v57_research_run_r811",
                    "v57_research_hypothesis_r811",
                    "v57_research_candidate_r811",
                    "v57_research_experiment_r811",
                    "v57_research_codegen_r811",
                    "v57_research_expert_predraw_r811",
                    "v57_research_expert_outcome_r811",
                    "v57_research_action_label_r811",
                    "v57_research_candidate_outcome_r811",
                    "v57_research_artifact_r811",
                    DbHelper.TABLE_R817_RESEARCH_PREDRAW
                };
        for (int i = 0; i < tables.length; i++) if (!tableExists(db, tables[i])) errors.add("TABLE:" + tables[i]);
        requireColumns(
                db,
                "v57_research_candidate_r811",
                new String[] {
                    "generation",
                    "genetic_operator",
                    "parent_formula_ids",
                    "feature_dependencies",
                    "dataset_hash"
                },
                errors);
        requireColumns(
                db,
                "v57_research_codegen_r811",
                new String[] {
                    "java_source", "patch_text", "manifest_text", "fixtures_text", "bundle_hash"
                },
                errors);
        requireColumns(
                db,
                "v57_research_experiment_r811",
                new String[] {"eligible_n", "selected_n", "hit_n"},
                errors);
        return new Report(errors);
    }

    public static void assertCritical(SQLiteDatabase db) {
        Report report = verifyCritical(db);
        if (!report.passed) throw new IllegalStateException("HARD_FAIL_R811_SCHEMA:" + report.detail);
    }

    /** Hot-path replacement for V51Repository.ensure(): only observes, never executes DDL. */
    public static void assertV51ReadyReadOnly(SQLiteDatabase db) {
        String[] tables =
                new String[] {
                    V51Repository.ISSUE,
                    V51Repository.ACTION_SPACE,
                    V51Repository.CHAMPION,
                    V51Repository.SELECTED,
                    V51Repository.AUTH
                };
        for (int i = 0; i < tables.length; i++)
            if (!tableExists(db, tables[i]))
                throw new IllegalStateException("V51_READ_SCHEMA_MISSING:" + tables[i]);
    }

    private static void requireUserVersion36(SQLiteDatabase db, List<String> errors) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA user_version", null);
            if (!c.moveToFirst() || c.getInt(0) != 36) errors.add("USER_VERSION");
        } catch (Throwable e) {
            errors.add("USER_VERSION_READ");
        } finally {
            if (c != null) c.close();
        }
    }

    private static void requireColumns(SQLiteDatabase db, String table, String[] names, List<String> errors) {
        for (int i = 0; i < names.length; i++) if (!columnExists(db, table, names[i])) errors.add("COLUMN:" + table + '.' + names[i]);
    }

    private static void requireIndex(SQLiteDatabase db, String index, List<String> errors) {
        if (!objectExists(db, "index", index)) errors.add("INDEX:" + index);
    }

    private static boolean tableExists(SQLiteDatabase db, String table) {
        return objectExists(db, "table", table);
    }

    private static boolean objectExists(SQLiteDatabase db, String type, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type=? AND name=? LIMIT 1", new String[] {type, name});
            return c.moveToFirst();
        } catch (Throwable e) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int index = c.getColumnIndex("name");
            while (c.moveToNext()) if (index >= 0 && name.equals(c.getString(index))) return true;
            return false;
        } catch (Throwable e) {
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String state(SQLiteDatabase db, String key) {
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT state_value FROM " + DbHelper.TABLE_APP_STATE + " WHERE state_key=? LIMIT 1", new String[] {key});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable e) {
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    public static final class Report {
        public final boolean passed;
        public final String detail;
        public final List<String> errors;

        Report(ArrayList<String> errors) {
            this.errors = Collections.unmodifiableList(new ArrayList<String>(errors));
            passed = errors.isEmpty();
            detail = passed ? "PASS" : errors.toString();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
