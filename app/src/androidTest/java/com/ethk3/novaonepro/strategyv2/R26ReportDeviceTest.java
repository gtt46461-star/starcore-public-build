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
    private static JSONObject section(JSONObject report,String id)throws Exception {JSONArray rows=report.getJSONArray("sections");for(int i=0;i<rows.length();i++)if(rows.getJSONObject(i).getString("sectionId").equals(id))return rows.getJSONObject(i);throw new AssertionError(id);}
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
            JSONObject report=new JSONObject(RuntimeReportR26.json(db,source));assertEquals("INCOMPLETE",report.getString("overallAcceptance"));assertEquals(40,report.getJSONArray("sections").length());assertEquals(16,section(report,"EXPERTS").getInt("missingInventoryRows"));assertEquals(0,section(report,"EXPERTS").getInt("omittedRows"));assertFalse(section(report,"EXPERTS").getBoolean("truncated"));
            for(int i=0;i<40;i++){JSONObject section=report.getJSONArray("sections").getJSONObject(i);assertEquals(commit,section.getString("sourceCommit"));assertEquals(sha,section.getString("apkHash"));assertEquals(sha,section.getString("sourceSnapshotId"));JSONObject metrics=section.getJSONObject("metricResults");JSONArray required=section.getJSONArray("requiredMetrics");for(int m=0;m<required.length();m++){JSONObject metric=metrics.getJSONObject(required.getString(m));assertTrue(metric.has("value"));assertTrue(metric.has("definition"));assertTrue(metric.has("evidenceRef"));if(metric.isNull("value"))assertTrue(metric.has("reason"));}}
        }
    }
    @Test public void repeatedExpertHistoryCannotHideAnUnservedRegisteredExpert()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){schema(db);db.beginTransaction();try{
            for(int i=1;i<=16;i++)db.execSQL("INSERT INTO r26_capability VALUES(?,'EXPERT','SYNTHETIC','EXECUTABLE','SYNTHETIC_REPORT_FIXTURE',?)",new Object[]{String.format(java.util.Locale.ROOT,"E%02d",i),i==1||i==3?100:0});
            for(int i=0;i<100;i++)db.execSQL("INSERT INTO r26_expert_experiment(experiment_id,expert_id,campaign_id,state,state_hash,checkpoint_hash,numerical_abi,proposal_json,created_at) VALUES(?,'E03',?,X'00','SYNTHETIC','SYNTHETIC','SYNTHETIC','{}',?)",new Object[]{"exp"+i,"history"+i,i});
            for(int i=0;i<10;i++)db.execSQL("INSERT INTO r26_expert_audit VALUES(?,'E01',?,'{}',?)",new Object[]{"audit"+i,i,i});db.setTransactionSuccessful();
        }finally{db.endTransaction();}
            JSONObject report=new JSONObject(RuntimeReportR26.json(db));JSONObject experts=section(report,"EXPERTS");assertEquals(16,experts.getInt("exportedRows"));assertFalse(experts.getBoolean("truncated"));JSONArray rows=experts.getJSONArray("metrics");assertEquals("audit9",rows.getJSONObject(0).getString("audit_id"));assertEquals("exp99",rows.getJSONObject(2).getString("experiment_id"));assertEquals("E16",rows.getJSONObject(15).getString("expert_id"));assertEquals("UNSERVED",rows.getJSONObject(15).getString("status"));
            JSONArray never=section(report,"FAIR_CAPABILITY_SERVICE").getJSONObject("metricResults").getJSONObject("enabledButNeverRun").getJSONArray("value");assertEquals(14,never.length());assertEquals("E16",never.getJSONObject(13).getString("mechanism"));assertEquals("INCOMPLETE",report.getString("overallAcceptance"));
        }
    }
    @Test public void researchCpuQuantilesHaveTheirOwnScopeAndDoNotInventPredrawLatency()throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){schema(db);db.beginTransaction();try{
            db.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',1000,100)");
            for(int i=0;i<100;i++)db.execSQL("INSERT INTO r26_progress(task_id,fence,cursor_before,cursor_after,cpu_ms,recorded_at) VALUES('SYNTHETIC',1,?,?,?,?)",new Object[]{i,i+1,i,1000+i});db.setTransactionSuccessful();
        }finally{db.endTransaction();}
            JSONObject report=new JSONObject(RuntimeReportR26.json(db)),metrics=section(report,"PERFORMANCE").getJSONObject("metricResults");assertEquals(49,metrics.getJSONObject("P50").getInt("value"));assertEquals(94,metrics.getJSONObject("P95").getInt("value"));assertEquals(98,metrics.getJSONObject("P99").getInt("value"));assertTrue(metrics.getJSONObject("P95").getString("definition").contains("not P0"));assertTrue(metrics.getJSONObject("writerWait").isNull("value"));
            JSONObject slo=section(report,"SLO_BUDGET").getJSONObject("metricResults");assertEquals("RESEARCH_SLICE_CPU_MS",slo.getJSONObject("metric").getString("value"));assertEquals(100,slo.getJSONObject("n").getInt("value"));assertTrue(slo.getJSONObject("deadlineMisses").isNull("value"));assertEquals("RUNNING",section(report,"CONTINUOUS_RUNTIME").getJSONObject("metricResults").getJSONObject("session").getJSONArray("value").getJSONObject(0).getString("state"));
        }
    }
}
