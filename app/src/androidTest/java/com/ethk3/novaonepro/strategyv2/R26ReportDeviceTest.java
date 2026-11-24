package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            schema(db);
            String snapshot=PolicyIdentityR26.sha256("SYNTHETIC_SNAPSHOT");
            String apk=PolicyIdentityR26.sha256("SYNTHETIC_APK");
            String commit="0123456789012345678901234567890123456789";
            JSONObject source=new JSONObject().put("sourceCommit",commit).put("snapshotSha256",snapshot)
                    .put("apkSha256",apk).put("sourceTreeSha256",snapshot).put("generatedRuleSha256",snapshot);
            JSONObject report=new JSONObject(RuntimeReportR26.json(db,source));
            assertEquals("INCOMPLETE",report.getString("overallAcceptance"));
            JSONArray sections=report.getJSONArray("sections");
            assertEquals(44,sections.length());
            assertEquals(16,section(report,"EXPERTS").getInt("missingInventoryRows"));
            assertEquals(0,section(report,"EXPERTS").getInt("omittedRows"));
            assertFalse(section(report,"EXPERTS").getBoolean("truncated"));
            Set<String> absentR27=new HashSet<>(List.of("R27_01_WIN_RATE_TRUTH","R27_02_EVOLUTION_TRUTH",
                    "R27_03_CONTINUOUS_RUNTIME","R27_04_UPGRADE_CONTINUITY"));
            Set<String> seen=new HashSet<>();
            int metricChapters=0;
            for(int i=0;i<sections.length();i++){
                JSONObject chapter=sections.getJSONObject(i);
                String id=chapter.getString("sectionId");
                assertTrue(id,seen.add(id));
                assertEquals(commit,chapter.getString("sourceCommit"));
                assertEquals(apk,chapter.getString("apkHash"));
                assertEquals(snapshot,chapter.getString("sourceSnapshotId"));
                if(absentR27.remove(id)){
                    assertEquals("INSUFFICIENT",chapter.getString("status"));
                    assertTrue(chapter.getString("reason").contains("R27 schema"));
                    continue;
                }
                assertEquals("R26_REPORT_2",chapter.getString("schemaVersion"));
                JSONObject metrics=chapter.getJSONObject("metricResults");
                JSONArray required=chapter.getJSONArray("requiredMetrics");
                for(int m=0;m<required.length();m++){
                    JSONObject metric=metrics.getJSONObject(required.getString(m));
                    assertTrue(metric.has("value"));
                    assertTrue(metric.has("definition"));
                    assertTrue(metric.has("evidenceRef"));
                    if(metric.isNull("value"))assertTrue(metric.has("reason"));
                }
                metricChapters++;
            }
            assertEquals(40,metricChapters);
            assertTrue(absentR27.isEmpty());
        }
    }

    @Test public void oldFactoryRegistrationRemainsVisibleWithoutCreditingTheCurrentSource(){
        String current="0123456789012345678901234567890123456789";
        String old="fedcba9876543210fedcba9876543210fedcba98";
        Map<String,Object> previous=Map.of("sourceCommit",old,"plannedTrials",120L,"completedOriginalTrials",0L);
        Map<String,Object> registry=Map.of("status","READ_OK_NOT_A_HEALTH_VERDICT","readOnly",true,"value",previous);
        ReportR27.R30Input capture=new ReportR27.R30Input(Map.of(),Map.of("sourceCommit",current),
                Map.of("lifecycleEvidenceR31",Map.of("evidenceContractR31",registry)),List.of());
        Map<String,Object> result=capture.metricResults.stream().filter(row->"Q08".equals(row.get("metricId")))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("UNKNOWN_STALE_SYNTHETIC_SOURCE",result.get("status"));
        assertEquals("SYNTHETIC",result.get("role"));
        assertEquals(false,result.get("realPermission"));
        assertNull(result.get("sampleCount"));
        Map<?,?> observations=(Map<?,?>)result.get("observations");
        assertEquals(registry,observations.get("registeredFactoryValidation"));
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
