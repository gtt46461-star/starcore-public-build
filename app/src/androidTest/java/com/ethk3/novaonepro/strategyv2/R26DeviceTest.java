package com.ethk3.novaonepro.strategyv2;
import android.app.job.JobScheduler;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.Models;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class R26DeviceTest {
    private static void schema(SQLiteDatabase db){db.beginTransaction();try{RuntimeSchemaR26.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    @Test public void additiveMigrationPreservesFeedbackAndTransactionRollback(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        db.execSQL(RuntimeSchemaR26.FEEDBACK);db.execSQL("INSERT INTO r26_feedback VALUES('old',1,'s','p',1)");schema(db);schema(db);
        try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_feedback",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
        db.beginTransaction();try{db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism) VALUES('test','P2','test')");}finally{db.endTransaction();}
        try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_task",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
    }}
    @Test public void feedbackIsUniqueAndConflictingSourceRollsBack(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);Models.Draw d=new Models.Draw();d.issue="202610270001";d.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};d.openTime=1000;d.fetchTime=1001;
        db.beginTransaction();try{RuntimeSchemaR26.feedbackWithinTransaction(db,d);RuntimeSchemaR26.feedbackWithinTransaction(db,d);db.setTransactionSuccessful();}finally{db.endTransaction();}
        try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_feedback",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
        d.numbers=new int[]{2,1,3,4,5,6,7,8,9,10};db.beginTransaction();try{RuntimeSchemaR26.feedbackWithinTransaction(db,d);fail("conflict accepted");}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("CONFLICT"));}finally{db.endTransaction();}
    }}
    @Test public void oldWorkerCannotCommitAfterFenceOrLeaseChanges(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);long now=System.currentTimeMillis();db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('t','P2','kernel',2,?,'new','RUNNING')",new Object[]{now+60000});
        try{RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","old",1,0));fail("old fence");}catch(IllegalStateException expected){}
        RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","new",2,0));db.execSQL("UPDATE r26_task SET lease_until=0");
        try{RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","new",2,0));fail("expired lease");}catch(IllegalStateException expected){}
    }}
    @Test public void fortyChaptersKeepMissingEvidenceVisible()throws Exception{try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);JSONObject report=new JSONObject(RuntimeReportR26.json(db));assertEquals("INCOMPLETE",report.getString("overallAcceptance"));JSONArray sections=report.getJSONArray("sections");assertEquals(40,sections.length());
        for(int i=0;i<sections.length();i++){JSONObject s=sections.getJSONObject(i);assertFalse(s.getString("status").equals("PASS"));assertTrue(s.has("omittedRows"));assertTrue(s.has("errorChain"));}
    }}
    @Test public void originalPublicAssetTeacherBindsStateAndCannotReadTargetLabel()throws Exception {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();com.ethk3.novaonepro.LegacyTeacherOwnerR26.register(app);
        assertTrue(FamilyRegistryR26.EXECUTABLE.contains("F23"));ModelGenomeSU2 g=FamilyRegistryR26.genome("F23");assertTrue(g.members.get(0).params.get("scope").startsWith("LEGACY_REPORTED"));
        long[] issues=new long[60],open=new long[60],available=new long[60];int[][] draws=new int[60][10];
        for(int i=0;i<60;i++){issues[i]=202610270001L+i;open[i]=1793059200000L+i*60000;available[i]=open[i]+1000;for(int p=0;p<10;p++)draws[i][p]=(p+i)%10+1;}
        int[] observed=draws[40].clone();OnlineModelsSU2.Model a=FamilyRegistryR26.build("F23"),b=FamilyRegistryR26.build("F23");
        try{com.ethk3.novaonepro.LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);a.update(observed);}finally{com.ethk3.novaonepro.LegacyTeacherOwnerR26.leave();}
        draws[40]=new int[]{10,9,8,7,6,5,4,3,2,1};
        try{com.ethk3.novaonepro.LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);b.update(observed);}finally{com.ethk3.novaonepro.LegacyTeacherOwnerR26.leave();}
        java.io.ByteArrayOutputStream left=new java.io.ByteArrayOutputStream(),right=new java.io.ByteArrayOutputStream();a.write(new java.io.DataOutputStream(left));b.write(new java.io.DataOutputStream(right));assertArrayEquals(left.toByteArray(),right.toByteArray());
        float[] stats=new float[3];a.predict(new float[10][10],stats);assertTrue(stats[1]>0);
    }
    @Test public void retiredPlatformJobNeverBecomesTheTrainingOwner(){Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();ReplayBurstSchedulerSU1.schedule(app);JobScheduler jobs=(JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(jobs!=null)assertNull(jobs.getPendingJob(ReplayBurstSchedulerSU1.JOB_ID));}
}
