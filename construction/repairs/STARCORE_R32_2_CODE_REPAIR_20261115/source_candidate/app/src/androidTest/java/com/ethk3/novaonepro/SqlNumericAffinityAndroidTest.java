package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Executes the production revocation writer on isolated Android SQLite. */
@RunWith(AndroidJUnit4.class)
public final class SqlNumericAffinityAndroidTest {
    @Test public void driftRevocationUsesNumericStringBoundThresholds() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R906_RESEARCH_CERTIFICATE
                    + "(asset_id TEXT PRIMARY KEY,status TEXT,revocation_code TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE " + DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE
                    + "(asset_id TEXT PRIMARY KEY,drift_score REAL,ece REAL,brier REAL)");
            String[] ids = {"good", "drift", "ece", "brier", "missing"};
            for (String id : ids) db.execSQL("INSERT INTO "
                    + DbHelper.TABLE_R906_RESEARCH_CERTIFICATE
                    + " VALUES(?,'ACTIVE_SHADOW','',0)", new Object[]{id});
            Object[][] rows = {{"good",0d,0d,0d}, {"drift",1d,0d,0d},
                    {"ece",0d,1d,0d}, {"brier",0d,0d,1d}, {"missing",null,null,null}};
            for (Object[] row : rows) db.execSQL("INSERT INTO "
                    + DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE + " VALUES(?,?,?,?)", row);
            db.beginTransaction();
            try {
                assertEquals("all four out-of-envelope certificates must be revoked", 4,
                        ResearchCertificateR906.revokeDriftedWithin(db));
                assertEquals("repeated revocation is idempotent", 0,
                        ResearchCertificateR906.revokeDriftedWithin(db));
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            try (Cursor c = db.rawQuery("SELECT status FROM "
                    + DbHelper.TABLE_R906_RESEARCH_CERTIFICATE + " WHERE asset_id='good'", null)) {
                assertTrue(c.moveToFirst());
                assertEquals("ACTIVE_SHADOW", c.getString(0));
            }
        }
    }
}
