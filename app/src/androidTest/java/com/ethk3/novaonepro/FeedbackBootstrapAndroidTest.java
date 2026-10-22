package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android SQLite queries from the production feedback scanners. */
@RunWith(AndroidJUnit4.class)
public final class FeedbackBootstrapAndroidTest {
    private static SQLiteDatabase fixture() {
        SQLiteDatabase db = SQLiteDatabase.create(null);
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
        return db;
    }

    private static int count(SQLiteDatabase db, String sql, String[] args) {
        try (Cursor c = db.rawQuery(sql, args)) { return c.getCount(); }
    }

    private static boolean postTableExists(SQLiteDatabase db) {
        return count(db, "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{LegacyPredrawContextBridgeR915.POST_TABLE}) == 1;
    }

    @Test public void missingOptionalTableDoesNotBlockEligibleFeedback() {
        try (SQLiteDatabase db = fixture()) {
            String[] args = {"COMPLETED", "16"};
            assertFalse(postTableExists(db));
            String sql = EssentialSettlementFeedbackR923.pendingIssuesSql(db);
            try (Cursor c = db.rawQuery(sql, args)) {
                assertEquals(1, c.getCount()); assertTrue(c.moveToFirst());
                assertEquals("eligible", c.getString(0));
            }
            assertFalse("read scan must not create schema", postTableExists(db));
            LegacyPredrawContextBridgeR915.ensureSchema(db);
            assertEquals(1, count(db, EssentialSettlementFeedbackR923.pendingIssuesSql(db), args));
            db.execSQL("INSERT INTO " + LegacyPredrawContextBridgeR915.POST_TABLE
                    + " VALUES('eligible','TEST',1,1,'test','test','test','test',1,'test')");
            assertEquals(0, count(db, EssentialSettlementFeedbackR923.pendingIssuesSql(db), args));
        }
    }

    @Test public void sidecarStillFindsMissingClosuresAfterFeedbackTableAppears() {
        try (SQLiteDatabase db = fixture()) {
            String[] args = {"COMPLETED", "NO_PREDRAW_CANONICAL_TERMINAL",
                    "FAILED_TERMINAL", "INVALID_DRAW_TERMINAL", "COMPLETED", "32"};
            assertEquals(4, count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), args));
            assertFalse(postTableExists(db));
            LegacyPredrawContextBridgeR915.ensureSchema(db);
            assertEquals(4, count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), args));
            db.execSQL("INSERT INTO " + LegacyPredrawContextBridgeR915.POST_TABLE
                    + " VALUES('eligible','TEST',1,1,'test','test','test','test',1,'test')");
            assertEquals(3, count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), args));
            db.execSQL("DELETE FROM " + DbHelper.TABLE_R906_ISSUE_CLOSURE + " WHERE target_issue='eligible'");
            assertEquals(4, count(db, SettlementPostCommitSidecarR909.pendingIssuesSql(db), args));
        }
    }
}
