package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class SchemaStampReasonAndroidTest {
    private static final String[] COMPLETED_STEPS = {"F02_SETTLEMENT_RECOVERY_AUDIT_V1", "CP12_CRITICAL_CANONICAL_REASON_V1",
            "R815_CRITICAL_SCHEMA", "R906_CRITICAL_SCHEMA", "R925_CRITICAL_SCHEMA", "R926_EVENT_LEDGER_SCHEMA",
            "R35_SCHEMA", "R36_SCHEMA", "R811_CRITICAL_SCHEMA", "V51_RUNTIME_SCHEMA", "B03_RUNTIME_SCHEMA", "R26_FEEDBACK_SCHEMA"};
    @Test public void reportsMismatchWithoutAcceptingDrift() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            SchemaDomainCoordinatorR926.Contract c = new SchemaDomainCoordinatorR926.Contract("TEST", 1, new String[]{"fixture"});
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE fixture(id INTEGER)");
            assertEquals("STAMP_MISSING", SchemaDomainCoordinatorR926.matchReason(db, c));
            SchemaDomainCoordinatorR926.verifyAndStamp(db, c);
            assertEquals("MATCH", SchemaDomainCoordinatorR926.matchReason(db, c));
            assertTrue(SchemaDomainCoordinatorR926.matches(db, c));
            db.execSQL("CREATE INDEX fixture_extra ON fixture(id)");
            assertEquals("FINGERPRINT_MISMATCH", SchemaDomainCoordinatorR926.matchReason(db, c));
            assertFalse(SchemaDomainCoordinatorR926.matches(db, c));
            db.execSQL("UPDATE " + SchemaDomainCoordinatorR926.TABLE_DOMAIN + " SET state='MIGRATING' WHERE domain='TEST'");
            assertEquals("STATE_NOT_READY", SchemaDomainCoordinatorR926.matchReason(db, c));
        }
    }

    @Test public void completedUpgradeAcceptsOnlyVerifiedResearchPinAdditions() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            SchemaDomainCoordinatorR926.Contract c = new SchemaDomainCoordinatorR926.Contract("CRITICAL", 6, new String[]{"draws"});
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE draws(issue TEXT,open_time INTEGER,dirty INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
            db.execSQL("CREATE TABLE r27_history_import(issue TEXT,ordinal INTEGER,numbers TEXT,status TEXT)");
            db.execSQL("CREATE TABLE r30r1_analysis_run(status TEXT)");
            SchemaDomainCoordinatorR926.verifyAndStamp(db, c);
            for (String step : COMPLETED_STEPS)
                db.execSQL("INSERT INTO " + SchemaDomainCoordinatorR926.TABLE_JOURNAL + "(domain,revision,step_id,status) VALUES('CRITICAL',6,?,'COMPLETED')", new Object[]{step});
            db.beginTransaction();
            try {
                com.ethk3.novaonepro.strategyv2.ResearchInputPinR30R1.ensure(db);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            assertEquals("MATCH_R30_PIN_DRAW_ADDITION", SchemaDomainCoordinatorR926.matchReason(db,c));
            assertTrue(SchemaDomainCoordinatorR926.matches(db,c));
            db.execSQL("CREATE INDEX unrelated_drift ON draws(issue)");
            assertEquals("FINGERPRINT_MISMATCH", SchemaDomainCoordinatorR926.matchReason(db,c));
            db.execSQL("DROP INDEX unrelated_drift");
            db.execSQL("UPDATE " + SchemaDomainCoordinatorR926.TABLE_JOURNAL + " SET status='RUNNING'");
            assertEquals("FINGERPRINT_MISMATCH", SchemaDomainCoordinatorR926.matchReason(db,c));
            db.execSQL("UPDATE " + SchemaDomainCoordinatorR926.TABLE_JOURNAL + " SET status='COMPLETED'");
            db.execSQL("DROP TRIGGER r30r1_pin_draws_insert");
            assertEquals("FINGERPRINT_MISMATCH", SchemaDomainCoordinatorR926.matchReason(db,c));
        }
    }

    @Test public void report003SchemaReproducesLegacyStampWithoutMigration() throws Exception {
        try (SQLiteDatabase db = SQLiteDatabase.create(null);
             java.io.InputStream in = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
                     .getContext().getAssets().open("r30_report_003_critical_schema.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            org.json.JSONArray statements = new org.json.JSONObject(bytes.toString("UTF-8")).getJSONArray("statements");
            assertEquals(253, statements.length());
            for (int i = 0; i < statements.length(); i++) db.execSQL(statements.getString(i));
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            SchemaDomainCoordinatorR926.Contract c = SchemaDomainCoordinatorR926.criticalContract();
            assertEquals("66e933caae1af5b4105350333a333b60141c84344750865904ce7305f42b6f88", c.contractHash);
            assertEquals("74acb5870df3cbfbe0561e2284307a34a2bacc35822cd9a74dfebf9d4b18c2d9", SchemaDomainCoordinatorR926.fingerprint(db,c));
            db.execSQL("INSERT INTO " + SchemaDomainCoordinatorR926.TABLE_DOMAIN
                    + "(domain,revision,contract_hash,fingerprint,state,updated_at) VALUES('CRITICAL',6,?,?,'READY',0)",
                    new Object[]{c.contractHash,"5b5a44cf15a923b2e2287beb613b0be460848a11dd9ebc2d220302341d203c67"});
            for (String step : COMPLETED_STEPS)
                db.execSQL("INSERT INTO " + SchemaDomainCoordinatorR926.TABLE_JOURNAL + "(domain,revision,step_id,status) VALUES('CRITICAL',6,?,'COMPLETED')", new Object[]{step});
            assertEquals("MATCH_R30_PIN_DRAW_ADDITION", SchemaDomainCoordinatorR926.matchReason(db,c));
            SchemaDomainCoordinatorR926.verifyObjects(db,c);
        }
    }
}
