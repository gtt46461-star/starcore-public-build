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
        schema(db);long now=System.currentTimeMillis();db.execSQL("INSERT INTO r26_session VALUES('new',1,'boot','RUNNING',?,0)",new Object[]{now});db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('t','P2','kernel',2,?,'new','RUNNING')",new Object[]{now+60000});
        try{RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","old",1,0));fail("old fence");}catch(IllegalStateException expected){}
        RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","new",2,0));db.execSQL("UPDATE r26_session SET state='STOPPED',user_enabled=0");
        try{RuntimeCoordinatorR26.requireFence(db,new RuntimeCoordinatorR26.Claim("t","new",2,0));fail("stopped session");}catch(IllegalStateException expected){}
        db.execSQL("UPDATE r26_session SET state='RUNNING',user_enabled=1");db.execSQL("UPDATE r26_task SET lease_until=0");
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
    @Test public void originalVmRepeatsExactlyAndRejectsLateAvailableTeacherInputs()throws Exception{
        com.ethk3.novaonepro.LegacyTeacherOwnerR26.register(InstrumentationRegistry.getInstrumentation().getTargetContext());
        int count=45;long[] issues=new long[count],open=new long[count],available=new long[count];int[][] draws=new int[count][10];
        for(int i=0;i<count;i++){issues[i]=202610270001L+i;open[i]=1793059200000L+i*60000;available[i]=open[i]+1;for(int p=0;p<10;p++)draws[i][p]=(i+p)%10+1;}
        Map<String,Object> a,b;try{com.ethk3.novaonepro.LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);a=com.ethk3.novaonepro.LegacyTeacherOwnerR26.auditCurrentFrame();}finally{com.ethk3.novaonepro.LegacyTeacherOwnerR26.leave();}
        assertEquals("ORIGINAL_VM_EXECUTED",a.get("availability"));assertTrue(a.containsKey("originalVMActionDigest"));
        draws[40]=new int[]{10,9,8,7,6,5,4,3,2,1};try{com.ethk3.novaonepro.LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);b=com.ethk3.novaonepro.LegacyTeacherOwnerR26.auditCurrentFrame();}finally{com.ethk3.novaonepro.LegacyTeacherOwnerR26.leave();}assertEquals(a,b);
        available[39]=open[40]+1;try{com.ethk3.novaonepro.LegacyTeacherOwnerR26.enter(issues,open,available,draws,40);assertEquals("INSUFFICIENT_CAUSAL_INPUT",com.ethk3.novaonepro.LegacyTeacherOwnerR26.auditCurrentFrame().get("availability"));}finally{com.ethk3.novaonepro.LegacyTeacherOwnerR26.leave();}
    }
    @Test public void birthBudgetIsIrrevocableAndFrozenForecastsAreImmutable(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{
            StrategySchemaSU2.ensure(db);
            for(String sql:SqlStatementsSU1.SQL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS su1_family_budget")||sql.startsWith("CREATE TABLE IF NOT EXISTS su1_alpha_reservation"))db.execSQL(sql);
            String policy=HashingSU1.text("synthetic-fixed-birth");
            CertificateSU2.preregister(db,policy,100);CertificateSU2.preregister(db,policy,999);
            try(Cursor c=db.rawQuery("SELECT from_sequence,family_alpha,protocol FROM r26_cohort",null)){assertTrue(c.moveToFirst());assertEquals(100,c.getLong(0));assertTrue(c.getDouble(1)>0);assertEquals(QualificationWindowR26.PROTOCOL,c.getString(2));}
            long before;try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_family_budget",null)){c.moveToFirst();before=c.getLong(0);}
            CertificateSU2.preregister(db,policy,1000);
            try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_family_budget",null)){c.moveToFirst();assertEquals(before,c.getLong(0));}
            db.execSQL("INSERT INTO r26_frozen VALUES(?,101,100,'checkpoint','state','source',1,1000,1001,zeroblob(400))",new Object[]{policy});
            try{db.execSQL("UPDATE r26_frozen SET state_hash='different'");fail("frozen state changed");}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.getMessage().contains("IMMUTABLE"));}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void retentionPreservesRecentFeedbackAndFoldsOldRowsOnce(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);
            db.execSQL("INSERT INTO r26_feedback VALUES('old',1,'s','p',1)");db.execSQL("INSERT INTO r26_feedback VALUES('new',6000,'s','p',1)");
            db.execSQL("INSERT INTO r26_consumption VALUES('old','p','checkpoint','REVEALED_LIVE_LABEL')");
            RetentionR26.prune(db,6001);RetentionR26.prune(db,6001);
            try(Cursor c=db.rawQuery("SELECT event_id FROM r26_feedback",null)){assertTrue(c.moveToFirst());assertEquals("new",c.getString(0));assertFalse(c.moveToNext());}
            try(Cursor c=db.rawQuery("SELECT folded FROM r26_retention_total WHERE kind='feedback_rows'",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getLong(0));}
            try(Cursor c=db.rawQuery("SELECT folded FROM r26_retention_total WHERE kind='consumed_feedback_rows'",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getLong(0));}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void lagRecoveryKeepsWeightsAndRespectsFamilyCapacity(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);byte[] weights={2,6,10};String active=null,recovered=null,blocked=null;
            for(int i=0;i<3;i++){ModelGenomeSU2 g=FamilyRegistryR26.genome(i==1?"F18":"F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_LAG:"+i),0);if(i==0)active=g.key();else if(i==1)recovered=g.key();else blocked=g.key();
                db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,status,updated_at,family_id) VALUES(?,?,?,?,?,'',1,1,2,?,?,?)",new Object[]{g.key(),g.text(),weights,"SYNTHETIC_HASH","SYNTHETIC_CHECKPOINT:"+i,i==0?"ACTIVE":"BLOCKED_RETENTION_LAG",i,i==1?"F18":"F17"});}
            String certificate=PolicyIdentityR26.sha256("SYNTHETIC_LAG_CERT");db.execSQL("INSERT INTO su2_certificate VALUES(?,?,'h',0,1,600,240,.31,.15,.28,NULL,1,.01,0,1,.01,1000,2000)",new Object[]{certificate,active});
            RetentionR26.prune(db,6001);RetentionR26.prune(db,6001);
            try(Cursor c=db.rawQuery("SELECT policy_hash,status,state,through_ordinal FROM r26_live_model",null)){int count=0;while(c.moveToNext()){count++;assertArrayEquals(weights,c.getBlob(2));assertEquals(1,c.getLong(3));assertEquals(c.getString(0).equals(blocked)?"BLOCKED_RETENTION_LAG":"ACTIVE",c.getString(1));}assertEquals(3,count);}
            try(Cursor c=db.rawQuery("SELECT certificate_id,reason FROM r26_certificate_revocation",null)){assertTrue(c.moveToFirst());assertEquals(certificate,c.getString(0));assertEquals("FEEDBACK_RETENTION_LAG",c.getString(1));assertFalse(c.moveToNext());}
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void retiredPlatformJobNeverBecomesTheTrainingOwner(){Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();ReplayBurstSchedulerSU1.schedule(app);JobScheduler jobs=(JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(jobs!=null)assertNull(jobs.getPendingJob(ReplayBurstSchedulerSU1.JOB_ID));}
}
