package com.ethk3.novaonepro.r342;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.CheckpointReportAuditR32;
import com.ethk3.novaonepro.R341StorageTestCases;
import com.ethk3.novaonepro.StorageCensusSU2;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.R342RetentionInterruption;
import java.io.File;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public final class R342B10ContractTest {
    private interface Checked {void run(Context app) throws Exception;}
    private void run(String issue,Checked body) throws Exception {
        var args=InstrumentationRegistry.getArguments();
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        String scope=args.getString("r30Sandbox");
        assertTrue(app.getApplicationInfo().dataDir.contains("/r30-instrumentation/"+scope+"/"));
        Map<String,Object> receipt=new LinkedHashMap<>();
        receipt.put("caseId",issue+"-A");
        receipt.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);
        receipt.put("scope",scope);
        receipt.put("phase",args.getString("r342Phase","normal"));
        receipt.put("pid",android.os.Process.myPid());
        receipt.put("event","CASE_ENTER");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION "+JsonR27.write(receipt));
        body.run(app);
        receipt.put("event","CASE_ASSERTIONS_COMPLETE");
        Log.i("STARCORE_R342","R342_INSTRUMENTATION "+JsonR27.write(receipt));
    }

    @Test public void test_r342_045() throws Exception {
        run("R342-045",app -> {
            R341StorageTestCases.protectedDebtAndSnapshotAdmission(app);
            R341StorageTestCases.missingIndexProtectsSnapshot(app);
        });
    }

    @Test public void test_r342_046() throws Exception {
        run("R342-046",app -> {
            R341StorageTestCases.checkpointAuditSeparatesMissingAndVerified(app);
            R341StorageTestCases.checkpointMigrationDoesNotInventRecovery(app);
            File root=new File(app.getCacheDir(),"r342-incomplete-checkpoint");
            assertTrue(root.mkdir()||root.isDirectory());
            String hash="9".repeat(64);
            StringWriter original=new StringWriter();
            JSONObject summary=CheckpointReportAuditR32.observe(root,Set.of(hash),original,()->true);
            JSONObject row=new JSONObject(original.toString().split("\n",2)[0]);
            assertEquals(hash,summary.getString("firstUnverifiedSha256"));
            assertEquals("MISSING_OBJECT",row.getString("availability"));
            assertTrue(row.isNull("verifiedChildHashes"));
            assertFalse(summary.getBoolean("gcOrModelUseAuthorized"));
        });
    }

    @Test public void test_r342_047() throws Exception {
        run("R342-047",R342RetentionInterruption::observe);
    }

    @Test public void test_r342_048() throws Exception {
        run("R342-048",app -> {
            int tables=516;
            try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
                db.beginTransaction();
                try {
                    for(int i=0;i<tables;i++){
                        db.execSQL("CREATE TABLE inventory_"+i+"(x INTEGER)");
                        db.execSQL("CREATE INDEX inventory_index_"+i+" ON inventory_"+i+"(x)");
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                StringWriter output=new StringWriter();
                StorageCensusSU2.writeSqliteSpaceR342(db,output,null);
                JSONObject inventory=new JSONObject(output.toString());
                assertEquals(2*tables,inventory.getInt("objectDenominator"));
                assertEquals(2*tables,inventory.getInt("objectsEnumerated"));
                JSONArray rows=inventory.getJSONArray("dbstatObjects");
                assertEquals(2*tables,rows.length());
                String after="";
                for(int i=0;i<rows.length();i++){
                    JSONObject row=rows.getJSONObject(i);
                    String name=row.getString("object");
                    assertTrue(name.compareTo(after)>0);
                    after=name;
                }
                if(!inventory.getBoolean("dbstatObjectCoverageComplete")){
                    assertEquals("OBJECT_BYTES_PARTIAL_OR_UNAVAILABLE",inventory.getString("dbstatStatus"));
                }
            }
        });
    }
}
