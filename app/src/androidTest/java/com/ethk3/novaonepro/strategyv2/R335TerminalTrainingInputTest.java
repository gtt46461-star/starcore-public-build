package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.SettlementTaskStoreR36;
import com.ethk3.novaonepro.TargetIssueResolver;
import org.junit.Test;
import static org.junit.Assert.*;

public final class R335TerminalTrainingInputTest {
    @Test public void pendingFailedConflictingAndRolledBackTasksCannotLabelALiveModel() {
        String issue = "202611210803";
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL(RuntimeSchemaR26.FEEDBACK);
            String task = DbHelper.TABLE_R36_SETTLEMENT_TASK;
            db.execSQL("CREATE TABLE " + task + "(target_issue TEXT PRIMARY KEY,draw_issue TEXT,draw_hash TEXT,"
                    + "status TEXT,completed_at INTEGER,settlement_hash TEXT)");
            db.execSQL("INSERT INTO r26_feedback VALUES('event',?,'v2','original',1)",
                    new Object[]{TargetIssueResolver.sequenceOrdinalSU1(issue)});
            db.execSQL("INSERT INTO " + task + " VALUES(?,?,'v2','PENDING',0,'')",new Object[]{issue,issue});
            assertFalse(admitted(db,issue));
            db.beginTransaction();
            try {
                db.execSQL("UPDATE " + task + " SET status='COMPLETED',completed_at=10,settlement_hash='proof'");
                assertTrue(admitted(db,issue));
            } finally {
                db.endTransaction();
            }
            assertFalse(admitted(db,issue));
            db.execSQL("UPDATE " + task + " SET status='FAILED_TERMINAL',completed_at=10,settlement_hash='proof'");
            assertFalse(admitted(db,issue));
            db.execSQL("UPDATE " + task + " SET status=?,draw_hash='other'",
                    new Object[]{SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL});
            assertFalse(admitted(db,issue));
            db.execSQL("UPDATE " + task + " SET draw_hash='v1'");
            assertTrue(admitted(db,issue));
            db.execSQL("UPDATE " + task + " SET status='COMPLETED',draw_hash='v2'");
            assertTrue(admitted(db,issue));
            db.execSQL("UPDATE " + task + " SET settlement_hash=''");
            assertFalse(admitted(db,issue));
        }
    }

    private static boolean admitted(SQLiteDatabase db, String issue) {
        return LegacyReplayR26.terminalFeedback(db,"event",issue,"v2","v1");
    }
}
