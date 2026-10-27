package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class R26ReportDeviceTest {
    private static void schema(SQLiteDatabase db){db.beginTransaction();try{RuntimeSchemaR26.ensure(db);StrategySchemaSU2.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    @Test public void frozenSurfacesAreCompleteAndIndependentOfLaterRevealedOutcome()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){schema(db);ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){for(int i=0;i<100;i++)out.writeFloat(.1f);}
            db.execSQL("INSERT INTO r26_frozen VALUES('SYNTHETIC',10,9,'checkpoint','state','source',1,1000,1001,?)",new Object[]{bytes.toByteArray()});
            db.beginTransaction();try{ModelStoreSU2.insertDecision(db,"SYNTHETIC",10,new ModelLiveSU2.Decision(9,7,7,false,-.8,.3),1000,1001);db.setTransactionSuccessful();}finally{db.endTransaction();}
            JSONObject before=FrozenSurfaceStoreR26.json(db);assertEquals("PASS",before.getString("status"));assertEquals(100,before.getJSONArray("surfaces").length());assertEquals(1200,before.getJSONArray("actions").length());assertEquals(7,before.getJSONArray("actions").getJSONObject(0).getInt("position"));assertFalse(before.getBoolean("newInferencePerformed"));assertFalse(before.getBoolean("qualityQualificationGranted"));
            db.execSQL("UPDATE su2_live_decision SET hit=1,settled_at=1002");assertEquals(before.toString(),FrozenSurfaceStoreR26.json(db).toString());
        }
    }
    @Test public void allRequiredReportMetricsHaveEvidenceOrExplicitMissingStateAndBoundIdentity()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){schema(db);String sha=PolicyIdentityR26.sha256("SYNTHETIC_SNAPSHOT"),commit="0123456789012345678901234567890123456789";JSONObject source=new JSONObject().put("sourceCommit",commit).put("snapshotSha256",sha).put("apkSha256",sha).put("sourceTreeSha256",sha).put("generatedRuleSha256",sha);
            JSONObject report=new JSONObject(RuntimeReportR26.json(db,source));assertEquals("INCOMPLETE",report.getString("overallAcceptance"));assertEquals(40,report.getJSONArray("sections").length());
            for(int i=0;i<40;i++){JSONObject section=report.getJSONArray("sections").getJSONObject(i);assertEquals(commit,section.getString("sourceCommit"));assertEquals(sha,section.getString("apkHash"));assertEquals(sha,section.getString("sourceSnapshotId"));JSONObject metrics=section.getJSONObject("metricResults");JSONArray required=section.getJSONArray("requiredMetrics");for(int m=0;m<required.length();m++){JSONObject metric=metrics.getJSONObject(required.getString(m));assertTrue(metric.has("value"));assertTrue(metric.has("definition"));assertTrue(metric.has("evidenceRef"));if(metric.isNull("value"))assertTrue(metric.has("reason"));}}
        }
    }
}
