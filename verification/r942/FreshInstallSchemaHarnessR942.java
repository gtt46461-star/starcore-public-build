package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Executes the production migration scheduler and DDL against real SQLite. */
public final class FreshInstallSchemaHarnessR942 {
    private static long scalar(SQLiteDatabase db, String sql) {
        try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : -1; }
    }
    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException(code);
    }
    private static boolean drive(DatabaseCoordinator owner) {
        boolean ready = false;
        for (int i = 0; i < 8 && !ready; i++) ready = ResearchSchemaCoordinatorR926.ensureOneStep(owner);
        return ready;
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0]);
        try {
            DbStore store = new DbStore(db);
            DatabaseCoordinator owner = new DatabaseCoordinator(db);
            StringBuilder earlyExport = new StringBuilder();
            AlgorithmLifecycleReportR927.writeDetails(store, earlyExport);
            require(earlyExport.indexOf("END_ALL_ALGORITHMS|rowsRead=0|status=READ_ERROR") >= 0,
                    "PREINIT_ALGORITHM_EXPORT_ABORTED");
            require(earlyExport.indexOf("END_ALL_RESEARCH_METHODS|rowsRead=0|status=READ_ERROR") >= 0,
                    "PREINIT_METHOD_EXPORT_ABORTED");
            // Android's DbHelper.onCreate has already established these Critical prerequisites
            // before the lazy Research scheduler is allowed to run.
            DbHelper.ensureR35Schema(db);
            DbHelper.ensureR36Schema(db);
            RuntimeEventLedgerR926.ensureSchema(db);
            DbHelper.ensureR906CriticalSchema(db);
            RealtimePressureGateR811.resetForTests();
            require(drive(owner), "FRESH_DATABASE_NOT_READY:" + ResearchSchemaCoordinatorR926.summary());
            require(scalar(db, "SELECT COUNT(*) FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + " WHERE status='COMPLETED'") == 6, "MIGRATION_STEP_COUNT");
            require(scalar(db, "SELECT attempt FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + " WHERE step_id='R811_RESEARCH'") == 1, "FIRST_STEP_REPEATED");
            String[] required = {DbHelper.TABLE_R908_ALGORITHM_GENOME,
                    SchemaRevisionR925.TABLE_LIFECYCLE_AUDIT,
                    ResearchPlatformStoreR929.TABLE_CERTIFICATE, ResearchLifecycleR932.TABLE,
                    CandidateCalibrationR932.MODEL, FixedResearchWindowR932.WINDOW,
                    AtomCaptureR932.QUALITY, "r952_research_outbox", "r952_candidate_predraw"};
            for (String table : required) require(scalar(db,
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='" + table + "'") == 1,
                    "MISSING_TABLE:" + table);
            require(scalar(db, "SELECT COUNT(*) FROM " + ResearchPlatformStoreR929.TABLE_CERTIFICATE) == 0,
                    "FAKE_CERTIFICATE_CREATED");
            require(scalar(db, "SELECT COUNT(*) FROM " + ResearchLifecycleR932.TABLE) == 0,
                    "FAKE_CHALLENGER_CREATED");
            StringBuilder out = new StringBuilder();
            AlgorithmLifecycleReportR927.writeDetails(store, out);
            require(out.indexOf("END_ALL_ALGORITHMS|rows=0|status=COMPLETE") >= 0,
                    "ALGORITHM_EXPORT_INCOMPLETE");
            require(out.indexOf("END_ALL_RESEARCH_METHODS|rows=0|status=COMPLETE") >= 0,
                    "METHOD_EXPORT_INCOMPLETE");

            db.execSQL("INSERT INTO " + ResearchPlatformStoreR929.TABLE_EVENT
                    + " VALUES('sentinel','TEST','entity','','','OK','preserve',1)");
            db.execSQL("DROP TABLE " + AtomCaptureR932.QUALITY);
            require(drive(owner), "READY_DATABASE_REPAIR_FAILED:" + ResearchSchemaCoordinatorR926.summary());
            require(scalar(db, "SELECT COUNT(*) FROM " + ResearchPlatformStoreR929.TABLE_EVENT
                    + " WHERE event_hash='sentinel'") == 1, "REPAIR_DELETED_RESEARCH_DATA");
            require(scalar(db, "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='"
                    + AtomCaptureR932.QUALITY + "'") == 1, "REPAIR_DID_NOT_RESTORE_TABLE");
            System.out.println("R942_FRESH_INSTALL_SCHEMA_PASS|steps=6|firstAttempt=1"
                    + "|unifiedTables=READY|emptyTruth=0|repair=PRESERVES_DATA"
                    + "|preinitExport=DIAGNOSTIC_CONTINUES|export=COMPLETE");
        } finally { db.shutdown(); }
    }
}
