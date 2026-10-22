package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Calls production scanners on disposable real SQLite; not natural evidence. */
public final class FeedbackBootstrapCP12 {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++; System.out.println("PASS " + message);
    }
    private static int count(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor c = db.rawQuery(sql, args)) { return c.getCount(); }
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0], args[1]);
        try {
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                    + "(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,priority INTEGER,completed_at INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R906_ISSUE_CLOSURE
                    + "(target_issue TEXT PRIMARY KEY,settlement_hash TEXT)");
            db.execSQL("CREATE TABLE " + DrawDependencyInputCP12.SOURCE + "(issue TEXT,dirty INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                    + "(target_issue TEXT,pre_draw_frozen INTEGER,settled INTEGER,hit_label INTEGER)");
            for (String issue : new String[]{"eligible", "unfrozen", "dirty", "unfinished"}) {
                db.execSQL("INSERT INTO " + DbHelper.TABLE_R36_SETTLEMENT_TASK + " VALUES(?,?,?,?,?)",
                        new Object[]{issue, "COMPLETED", "synthetic", 1, 1});
                db.execSQL("INSERT INTO " + DbHelper.TABLE_R906_ISSUE_CLOSURE + " VALUES(?,?)",
                        new Object[]{issue, "synthetic"});
                db.execSQL("INSERT INTO " + DrawDependencyInputCP12.SOURCE + " VALUES(?,?)",
                        new Object[]{issue, "dirty".equals(issue) ? 1 : 0});
                db.execSQL("INSERT INTO " + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " VALUES(?,?,?,?)",
                        new Object[]{issue, "unfrozen".equals(issue) ? 0 : 1,
                                "unfinished".equals(issue) ? 0 : 1, 1});
            }
            String[] essential = {"COMPLETED", "16"};
            String[] sidecar = {"COMPLETED", "NO_PREDRAW_CANONICAL_TERMINAL", "FAILED_TERMINAL",
                    "INVALID_DRAW_TERMINAL", "COMPLETED", "32"};
            String sql = EssentialSettlementFeedbackR923.pendingIssuesSql(db);
            try (Cursor c = db.rawQuery(sql, essential)) {
                check(c.getCount() == 1 && c.moveToFirst() && "eligible".equals(c.getString(0)),
                        "ABSENT_TABLE_SELECTS_ONLY_CLEAN_SETTLED_FROZEN_INPUT");
            }
            check(count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), sidecar) == 4,
                    "ABSENT_TABLE_DOES_NOT_BLOCK_CLOSURE_SCAN");
            check(count(db, "SELECT 1 FROM sqlite_master WHERE name=?",
                    new String[]{LegacyPredrawContextBridgeR915.POST_TABLE}) == 0, "READ_SCANS_HAVE_NO_DDL");
            LegacyPredrawContextBridgeR915.ensureSchema(db);
            check(count(db, EssentialSettlementFeedbackR923.pendingIssuesSql(db), essential) == 1,
                    "FIRST_WRITER_SCHEMA_DOES_NOT_LOSE_PENDING_INPUT");
            check(count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), sidecar) == 4,
                    "EMPTY_POST_TABLE_PRESERVES_PENDING_SET");
            db.execSQL("INSERT INTO " + LegacyPredrawContextBridgeR915.POST_TABLE
                    + " VALUES('eligible','TEST',1,1,'test','test','test','test',1,'test')");
            check(count(db, EssentialSettlementFeedbackR923.pendingIssuesSql(db), essential) == 0,
                    "WRITTEN_FEEDBACK_IS_NOT_REQUEUED");
            check(count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), sidecar) == 3,
                    "COMPLETE_CLOSURE_AND_FEEDBACK_IS_NOT_REQUEUED");
            db.execSQL("DELETE FROM " + DbHelper.TABLE_R906_ISSUE_CLOSURE + " WHERE target_issue='eligible'");
            check(count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), sidecar) == 4,
                    "MISSING_CLOSURE_STILL_REQUEUED_WITH_FEEDBACK");
            db.execSQL("DROP TABLE " + LegacyPredrawContextBridgeR915.POST_TABLE);
            db.execSQL("CREATE TABLE " + LegacyPredrawContextBridgeR915.POST_TABLE + "(wrong_column TEXT)");
            boolean rejected = false;
            try { count(db, EssentialSettlementFeedbackR923.pendingIssuesSql(db), essential); }
            catch (RuntimeException expected) { rejected = true; }
            check(rejected, "MALFORMED_EXISTING_SCHEMA_IS_NOT_HIDDEN_AS_ABSENT");
            System.out.println("FEEDBACK_BOOTSTRAP_PASS|checks=" + checks + "|scope=HOST_SQLITE_NOT_NATURAL");
        } finally { db.close(); }
    }
}
