package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Runs an authoritative frozen settlement through the normal writer before checking the coverage projection. */
@RunWith(AndroidJUnit4.class)
public final class Field173CoverageAndroidTest {
    private static final String TARGET = R30FaultMatrixTest.TARGET;
    private static final String LABELS = DbHelper.TABLE_R35_OOS_LABEL;
    private static final String TASKS = DbHelper.TABLE_R36_SETTLEMENT_TASK;
    private static final String[] ISSUE = {TARGET};

    @Test public void actualSettlementAndScopedCoverageUseSeparateHashes() throws Exception {
        DbStore store = new DbStore(ApplicationProvider.getApplicationContext());
        try {
            R30FaultMatrixTest.seed(store);
            R30FaultMatrixTest.freeze(store);
            SystemClock.sleep(30L);
            R30FaultMatrixTest.actual(store);
            R30FaultMatrixTest.settle(store);
            R30FaultMatrixTest.noFalsePublicationOrReal(store);

            SQLiteDatabase original = store.readable();
            String labelHash = scalar(original, "SELECT record_hash FROM " + LABELS + " WHERE target_issue=?");
            String taskHash = scalar(original, "SELECT settlement_hash FROM " + TASKS + " WHERE target_issue=?");
            assertTrue(labelHash.matches("[a-f0-9]{64}"));
            assertTrue(taskHash.matches("[a-f0-9]{64}"));
            assertNotEquals(labelHash, taskHash);
            RuntimeHealthLedgerR36.BusinessTruth observed = coverage(original);
            assertEquals(observed.canonical(), 1, observed.coverageEligible);
            assertEquals(observed.canonical(), 1, observed.coverageFrozen);
            assertEquals(observed.canonical(), 1, observed.coverageSettled);
            assertEquals(observed.canonical(), 0, observed.coverageNoPredrawTerminal);

            // Keep corruption and missing-receipt injections out of the protected production rows.
            try (SQLiteDatabase mirror = SQLiteDatabase.create(null)) {
                copyTable(original, mirror, DbHelper.TABLE_DRAWS, "issue=?", ISSUE);
                copyTable(original, mirror, DbHelper.TABLE_R35_SHADOW_OBSERVATION, "target_issue=?", ISSUE);
                copyTable(original, mirror, TASKS, "target_issue=?", ISSUE);
                copyTable(original, mirror, LABELS, "target_issue=?", ISSUE);
                copyTable(original, mirror, RuntimeObservationScopeR341.DATABASE, null, null);
                copyTable(original, mirror, RuntimeObservationScopeR341.EVENT_SCOPE, null, null);
                copyTable(original, mirror, RuntimeEventLedgerR926.TABLE_EVENT, "target_issue=?", ISSUE);
                assertEquals(1, coverage(mirror).coverageSettled);
                reject(mirror, "UPDATE " + LABELS + " SET actual_number=CASE actual_number WHEN 10 THEN 1 ELSE actual_number+1 END");
                reject(mirror, "UPDATE " + LABELS + " SET record_hash='" + taskHash + "'");
                reject(mirror, "UPDATE " + LABELS + " SET prediction_created_at=draw_open_time");
                reject(mirror, "UPDATE " + TASKS + " SET completed_at=0");
                reject(mirror, "UPDATE " + TASKS + " SET status='RETRY_WAIT'");
                reject(mirror, "DELETE FROM " + LABELS);
            }
            assertEquals(labelHash, scalar(original, "SELECT record_hash FROM " + LABELS + " WHERE target_issue=?"));
            assertEquals(taskHash, scalar(original, "SELECT settlement_hash FROM " + TASKS + " WHERE target_issue=?"));
            assertEquals(1, coverage(original).coverageSettled);
            System.out.println("FIELD173_COVERAGE|target=" + TARGET + "|eligible=1|frozen=1|settled=1"
                    + "|label_sha256=" + labelHash + "|aggregate_sha256=" + taskHash + "|rejected_receipts=6");
        } finally {
            store.close();
        }
    }

    private static String scalar(SQLiteDatabase db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, ISSUE)) {
            assertTrue(sql, cursor.moveToFirst());
            return cursor.getString(0);
        }
    }

    private static RuntimeHealthLedgerR36.BusinessTruth coverage(SQLiteDatabase db) {
        RuntimeHealthLedgerR36.BusinessTruth out = new RuntimeHealthLedgerR36.BusinessTruth();
        out.latestDrawIssue = TARGET;
        RuntimeHealthLedgerR36.readCoverageR342(null, db, out);
        return out;
    }

    private static void reject(SQLiteDatabase db, String sql) {
        db.beginTransaction();
        try {
            db.execSQL(sql);
            RuntimeHealthLedgerR36.BusinessTruth malformed = coverage(db);
            assertEquals(malformed.canonical(), 1, malformed.coverageEligible);
            assertEquals(malformed.canonical(), 1, malformed.coverageFrozen);
            assertEquals(malformed.canonical(), 0, malformed.coverageSettled);
        } finally {
            db.endTransaction();
        }
        assertEquals(1, coverage(db).coverageSettled);
    }

    private static void copyTable(SQLiteDatabase source, SQLiteDatabase destination, String name,
            String where, String[] args) {
        try (Cursor schema = source.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",
                new String[] {name})) {
            assertTrue(name, schema.moveToFirst());
            destination.execSQL(schema.getString(0));
        }
        try (Cursor rows = source.query(name, null, where, args, null, null, null)) {
            while (rows.moveToNext()) {
                ContentValues values = new ContentValues(rows.getColumnCount());
                for (int i = 0; i < rows.getColumnCount(); i++) {
                    String column = rows.getColumnName(i);
                    switch (rows.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: values.putNull(column); break;
                        case Cursor.FIELD_TYPE_INTEGER: values.put(column, rows.getLong(i)); break;
                        case Cursor.FIELD_TYPE_FLOAT: values.put(column, rows.getDouble(i)); break;
                        case Cursor.FIELD_TYPE_BLOB: values.put(column, rows.getBlob(i)); break;
                        default: values.put(column, rows.getString(i));
                    }
                }
                destination.insertOrThrow(name, null, values);
            }
        }
    }
}
