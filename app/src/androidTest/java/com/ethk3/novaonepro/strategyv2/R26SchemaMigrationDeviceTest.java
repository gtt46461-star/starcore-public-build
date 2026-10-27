package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual legacy migration and same-version repair, isolated synthetic DB; not a full P0 or phone SLO. */
@RunWith(AndroidJUnit4.class)
public final class R26SchemaMigrationDeviceTest {
    private static void measured(JSONObject profile,String name,Runnable action)throws Exception{
        long wall=SystemClock.elapsedRealtime(),cpu=SystemClock.currentThreadTimeMillis();action.run();profile.put(name,new JSONObject().put("wallMs",SystemClock.elapsedRealtime()-wall).put("cpuMs",SystemClock.currentThreadTimeMillis()-cpu));
    }
    private static JSONObject profile(SQLiteDatabase db)throws Exception{
        JSONObject profile=new JSONObject().put("scope","INDIVIDUAL_SCHEMA_FUNCTION_DIAGNOSTIC; NOT_FULL_OPPORTUNITY_SLO");
        try(android.database.Cursor c=db.rawQuery("SELECT count(*),coalesce(sum(length(sql)),0) FROM sqlite_master",null)){assertTrue(c.moveToFirst());profile.put("catalogueObjects",c.getLong(0)).put("catalogueSqlCharacters",c.getLong(1));}
        SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.researchContract();
        measured(profile,"constructContract",()->SchemaDomainCoordinatorR926.researchContract());
        measured(profile,"freshFingerprint",()->SchemaDomainCoordinatorR926.fingerprint(db,contract));
        measured(profile,"requiredObjects",()->SchemaDomainCoordinatorR926.verifyObjects(db,contract));
        measured(profile,"infrastructure",()->SchemaDomainCoordinatorR926.ensureInfrastructure(db));
        measured(profile,"journalRead",()->assertTrue(SchemaDomainCoordinatorR926.stepComplete(db,contract,"R811_RESEARCH")));
        measured(profile,"strategyReadiness",()->assertTrue(StrategySchemaSU1.ready(db)));
        return profile;
    }
    private static long complete(DbStore store,JSONArray timings)throws Exception{
        long stop=SystemClock.elapsedRealtime()+240000,max=0;int calls=0;
        while(SystemClock.elapsedRealtime()<stop){String before=ResearchSchemaCoordinatorR926.summary();long start=SystemClock.elapsedRealtime(),cpu=SystemClock.currentThreadTimeMillis();ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(store.coordinatorForResearchR952());long wall=SystemClock.elapsedRealtime()-start;max=Math.max(max,wall);calls++;
            if(wall>=100)timings.put(new JSONObject().put("before",before).put("after",ResearchSchemaCoordinatorR926.summary()).put("wallMs",wall).put("cpuMs",SystemClock.currentThreadTimeMillis()-cpu).put("status",result.status));
            if(result.cause!=null)throw new AssertionError(result.detail,result.cause);if(result.ready)return max;
        }throw new AssertionError("Legacy schema did not finish; calls="+calls+"; "+ResearchSchemaCoordinatorR926.summary());
    }
    @Test public void legacySchemaAndDriftRepairKeepDataAndStayBelowCapacityFailureBoundary()throws Exception{
        android.content.Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());R26RuntimeDeviceTest.Sandbox app=new R26RuntimeDeviceTest.Sandbox(original);DbStore store=null;
        JSONObject receipt=new JSONObject().put("scope","SYNTHETIC_LEGACY_SCHEMA_ONLY; NOT_FULL_PREDRAW_SLO_OR_USER_PHONE").put("capacityFailureBoundaryMs",1000);JSONArray cold=new JSONArray(),repair=new JSONArray();receipt.put("coldSlowCalls",cold).put("repairSlowCalls",repair);
        try{
            store=new DbStore(app);store.setState("r26_schema_fixture_marker","PRESERVE");SQLiteDatabase db=store.readable();store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_RUNTIME_SCHEMA",w->{RuntimeSchemaR26.ensure(w);return 0;});
            boolean own=false;for(int i=0;i<1000&&!own;i++){ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureStrategyStorageOneStep(store.coordinatorForResearchR952());if(result.cause!=null)throw new AssertionError(result.detail,result.cause);own=result.ready;}assertTrue(own);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_SU2_SCHEMA",w->{StrategySchemaSU2.ensure(w);return 0;});
            long coldMax=complete(store,cold);receipt.put("coldMaximumWriterCallMs",coldMax);assertTrue(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()));receipt.put("completedCatalogueProfile",profile(db));
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_SCHEMA_DRIFT",w->{w.execSQL("DROP TRIGGER r969_run_no_replace");return 0;});assertFalse(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()));
            long repairMax=complete(store,repair);receipt.put("repairMaximumWriterCallMs",repairMax);assertTrue(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()));assertEquals("PRESERVE",store.getState("r26_schema_fixture_marker",""));
            assertTrue("Cold legacy migration exceeded the unchanged capacity-failure boundary: "+coldMax,coldMax<=1000);assertTrue("Repair exceeded the unchanged capacity-failure boundary: "+repairMax,repairMax<=1000);receipt.put("result","PASS_SCHEMA_ONLY_WITH_STATED_SCOPE");
        }catch(Throwable failure){receipt.put("result","FAIL").put("error",failure.toString());throw failure;}
        finally{
            if(store!=null)store.close();File target=new File(original.getCacheDir(),"r26-schema-migration-synthetic.json");try(FileOutputStream output=new FileOutputStream(target)){output.write(receipt.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));output.getFD().sync();}System.out.println("R26_SCHEMA_MIGRATION_RECEIPT="+receipt);R26RuntimeDeviceTest.remove(app.root);
        }
    }
}
