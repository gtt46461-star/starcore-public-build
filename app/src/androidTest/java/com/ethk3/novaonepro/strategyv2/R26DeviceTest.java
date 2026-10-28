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
    @Test public void enabledResearchCpuLeaseHasBoundedRenewalAndCannotRestartAfterClose(){
        java.util.concurrent.atomic.AtomicLong now=new java.util.concurrent.atomic.AtomicLong();
        final class TimedBackend implements RuntimePowerLeaseR26.Backend {long deadline;int acquires,releases;boolean held;public void acquire(long millis){assertEquals(60000,millis);acquires++;held=true;deadline=now.get()+millis;}public boolean isHeld(){return held&&now.get()<deadline;}public void release(){releases++;held=false;}}
        TimedBackend backend=new TimedBackend();RuntimePowerLeaseR26 lease=new RuntimePowerLeaseR26(backend,now::get);assertFalse(lease.isHeld());assertEquals(0,lease.renewals());lease.keepAlive();assertTrue(lease.isHeld());assertEquals(1,backend.acquires);assertEquals(60000,lease.expiresAtElapsedMs());now.set(14999);lease.keepAlive();assertEquals(1,backend.acquires);now.set(15000);lease.keepAlive();assertEquals(2,backend.acquires);assertEquals(75000,lease.expiresAtElapsedMs());backend.held=false;now.set(16000);lease.keepAlive();assertEquals(3,backend.acquires);now.set(lease.expiresAtElapsedMs()+1);assertFalse(lease.isHeld());lease.keepAlive();assertEquals(4,backend.acquires);assertEquals(now.get()+60000,lease.expiresAtElapsedMs());lease.close();lease.close();assertEquals(1,backend.releases);assertFalse(lease.isHeld());try{lease.keepAlive();fail("Closed research power owner restarted from a heartbeat");}catch(IllegalStateException expected){}assertEquals(4,backend.acquires);assertEquals(4,lease.renewals());
    }
    @Test public void additiveMigrationPreservesFeedbackAndTransactionRollback(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        db.execSQL(RuntimeSchemaR26.FEEDBACK);db.execSQL("INSERT INTO r26_feedback VALUES('old',1,'s','p',1)");schema(db);schema(db);
        try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_feedback",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
        db.beginTransaction();try{db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism) VALUES('test','P2','test')");}finally{db.endTransaction();}
        try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_task",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}
    }}
    @Test public void measuredTrainingDetailQuotaFoldsCountsAndRejectsUnownedTrimming(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);try{TrainingStepStoreR26.trim(db);fail("Unowned training detail pruning");}catch(IllegalStateException expected){}
        db.beginTransaction();try{
            db.execSQL("WITH RECURSIVE n(x) AS (VALUES(1) UNION ALL SELECT x+1 FROM n WHERE x<8193) INSERT INTO r26_training_step(policy_hash,campaign_id,sample_ordinal,sample_issue,member_key,source_hash,source_kind,kernel_update,loss_before,loss_after,gradient_norm,parameter_delta_norm,input_hash,objective,before_state_hash,after_state_hash,gap_reset,optimizer,scope,batch_checkpoint,receipt_hash,recorded_at) SELECT 'SYNTHETIC_POLICY','SYNTHETIC_QUOTA',x,CAST(x AS TEXT),'SYNTHETIC_MEMBER','SYNTHETIC_SOURCE','SYNTHETIC_QUOTA_FIXTURE',x,0,0,0,0,'SYNTHETIC_INPUT','SYNTHETIC_QUOTA_ONLY','before','after',0,'SYNTHETIC','SYNTHETIC_ONLY','SYNTHETIC_CHECKPOINT',lower(hex(zeroblob(32))),x FROM n");
            TrainingStepStoreR26.trim(db);try(Cursor c=db.rawQuery("SELECT count(*),min(sample_ordinal),max(sample_ordinal) FROM r26_training_step",null)){assertTrue(c.moveToFirst());assertEquals(8192,c.getInt(0));assertEquals(2,c.getInt(1));assertEquals(8193,c.getInt(2));}TrainingStepStoreR26.trim(db);try(Cursor c=db.rawQuery("SELECT folded FROM r26_retention_total WHERE kind='training_step_details'",null)){assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));}
            assertFalse("Synthetic quota placeholders must not validate as measured receipts",TrainingStepStoreR26.verify(db));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void mixedGradientAndStatisticDetailsShareOneQuotaAndSingleEvictionSnapshot(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);try{TrainingStepStoreR26.trim(db);fail("Unowned mixed detail pruning");}catch(IllegalStateException expected){}
        db.beginTransaction();try{
            db.execSQL("WITH RECURSIVE n(x) AS (VALUES(1) UNION ALL SELECT x+1 FROM n WHERE x<4098) INSERT INTO r26_training_step SELECT 'SYNTHETIC_G','QUOTA',x,CAST(x AS TEXT),'MEMBER','SOURCE','SYNTHETIC_QUOTA',x,0,0,0,0,'INPUT','OBJECTIVE','before','after',0,'SYNTHETIC','SYNTHETIC_ONLY','CK',lower(hex(zeroblob(32))),x FROM n");
            db.execSQL("WITH RECURSIVE n(x) AS (VALUES(1) UNION ALL SELECT x+1 FROM n WHERE x<4098) INSERT INTO r26_statistic_training_step SELECT 'SYNTHETIC_S','QUOTA',x,CAST(x AS TEXT),'MEMBER','SOURCE','SYNTHETIC_QUOTA',x,0,0,0,0,'INPUT','OBJECTIVE','STATISTIC','before','after',0,'SYNTHETIC','SYNTHETIC_ONLY','CK',lower(hex(zeroblob(32))),x FROM n");
            TrainingStepStoreR26.trim(db);for(String table:new String[]{"r26_training_step","r26_statistic_training_step"})try(Cursor c=db.rawQuery("SELECT count(*),min(sample_ordinal) FROM "+table,null)){assertTrue(c.moveToFirst());assertEquals(4096,c.getInt(0));assertEquals(3,c.getInt(1));}
            TrainingStepStoreR26.trim(db);try(Cursor c=db.rawQuery("SELECT count(*),sum(folded),min(folded),max(folded) FROM r26_retention_total WHERE kind IN ('training_step_details','statistic_training_step_details')",null)){assertTrue(c.moveToFirst());assertEquals(2,c.getInt(0));assertEquals(4,c.getInt(1));assertEquals(2,c.getInt(2));assertEquals(2,c.getInt(3));}assertFalse(TrainingStepStoreR26.verify(db));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
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
    @Test public void serviceTotalsSurviveReceiptRingAndSessionRetention()throws Exception{try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);db.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC_A',0,'SYNTHETIC','STOPPED',0,0)");db.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC_B',1,'SYNTHETIC','RUNNING',100,0)");ServiceTotalsR26.record(db,"SYNTHETIC_A",10,1100,true);ServiceTotalsR26.record(db,"SYNTHETIC_B",5,20,false);ServiceTotalsR26.record(db,"SYNTHETIC_B",10,1300,true);assertEquals(2,ServiceTotalsR26.failures(db));db.execSQL("DELETE FROM r26_progress");assertEquals(2,ServiceTotalsR26.failures(db));
            for(int i=1;i<=33;i++)db.execSQL("INSERT INTO r26_session VALUES(?,0,'SYNTHETIC','STOPPED',?,0)",new Object[]{"SYNTHETIC_OLD_"+i,i});RetentionR26.prune(db,100);assertEquals(2,ServiceTotalsR26.failures(db));RetentionR26.prune(db,100);assertEquals(2,ServiceTotalsR26.failures(db));JSONObject totals=ServiceTotalsR26.snapshot(db);assertEquals(1,totals.getLong("totalSessionRows"));assertEquals(1,totals.getJSONObject("historicalFoldedTotals").getLong("service_capacity_failures"));assertEquals(3,totals.getJSONArray("sessions").getJSONObject(0).getLong("committedSlices")+totals.getJSONObject("historicalFoldedTotals").getLong("service_slices"));db.execSQL("DROP TABLE r26_service_total");assertTrue(RuntimeSchemaR26.ready(db));assertEquals("NOT_INSTALLED",ServiceTotalsR26.snapshot(db).getString("availability"));assertTrue(ServiceTotalsR26.snapshot(db).isNull("committedCapacityFailuresSinceInstallation"));db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void firstPolicyBirthCannotRegisterAnEmptyOrMutableBenchmark(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);for(String sql:SqlStatementsSU1.SQL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS su1_family_budget")||sql.startsWith("CREATE TABLE IF NOT EXISTS su1_alpha_reservation"))db.execSQL(sql);String policy=HashingSU1.text("SYNTHETIC_FIRST_POLICY_BIRTH");CertificateSU2.preregister(db,policy,100);assertFalse(ModelStoreSU2.league(db).isEmpty());
            try(Cursor c=db.rawQuery("SELECT from_sequence,incumbent,family_alpha FROM r26_cohort WHERE genome_key=?",new String[]{policy})){assertTrue(c.moveToFirst());assertEquals(100,c.getLong(0));assertEquals(IncumbentLoopSU2.KEY,c.getString(1));assertTrue(c.getDouble(2)>0);}
            assertNull(CertificateSU2.read(db,IncumbentLoopSU2.KEY,System.currentTimeMillis()));for(String mutation:new String[]{"incumbent='different'","from_sequence=99","family_alpha=.5"})try{db.execSQL("UPDATE r26_cohort SET "+mutation);fail("birth preregistration changed");}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.getMessage().contains("R26_COHORT_BIRTH_IMMUTABLE"));}db.execSQL("UPDATE r26_cohort SET checked_sequence=101,state='SYNTHETIC_CHECKED'");CertificateSU2.preregister(db,policy,999);try(Cursor c=db.rawQuery("SELECT from_sequence,checked_sequence FROM r26_cohort WHERE genome_key=?",new String[]{policy})){c.moveToFirst();assertEquals(100,c.getLong(0));assertEquals(101,c.getLong(1));}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
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
    private static Models.Draw historical(int issue){Models.Draw row=new Models.Draw();row.issue=String.format(java.util.Locale.ROOT,"20261027%04d",issue);row.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};row.openTime=System.currentTimeMillis()-600000+issue*60000;row.fetchTime=row.openTime+1000;return row;}
    @Test public void preparedForecastRejectsChangedCheckpointAndCorruptPayload()throws Exception{try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);float[][] table=new float[10][10];for(float[] row:table)Arrays.fill(row,.1f);ForecastSnapshotR26 forecast=new ForecastSnapshotR26(new ModelLiveSU2.Decision(1,2,7,true,.3,.3),table);
        db.beginTransaction();try{db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at) VALUES('SYNTHETIC','SYNTHETIC',X'00','state','ck','',100,1,101,1)");assertTrue(PreparedForecastStoreR26.install(db,"SYNTHETIC","ck","state",100,forecast));db.setTransactionSuccessful();}finally{db.endTransaction();}
        assertArrayEquals(forecast.bytes(),PreparedForecastStoreR26.read(db,"SYNTHETIC","ck","state",100).bytes());assertNull(PreparedForecastStoreR26.read(db,"SYNTHETIC","new","state",100));assertNull(PreparedForecastStoreR26.read(db,"SYNTHETIC","ck","new",100));assertNull(PreparedForecastStoreR26.read(db,"SYNTHETIC","ck","state",101));
        db.beginTransaction();try{db.execSQL("UPDATE r26_live_model SET checkpoint_hash='new',through_ordinal=101 WHERE policy_hash='SYNTHETIC'");assertFalse(PreparedForecastStoreR26.install(db,"SYNTHETIC","ck","state",100,forecast));db.setTransactionSuccessful();}finally{db.endTransaction();}
        db.execSQL("UPDATE r26_prepared_forecast SET payload=zeroblob(437) WHERE policy_hash='SYNTHETIC'");try{PreparedForecastStoreR26.read(db,"SYNTHETIC","ck","state",100);fail("corrupt forecast accepted");}catch(java.io.IOException expected){assertEquals("R26_PREPARED_FORECAST_HASH",expected.getMessage());}
    }}
    private static void shadowRow(SQLiteDatabase db,String key,String family,long birth){db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at,family_id) VALUES(?,'SYNTHETIC',X'2601','SYNTHETIC','SYNTHETIC','',?,1,?,1,?)",new Object[]{key,birth-1,birth,family});ShadowAdmissionR26.register(db,key,family,birth,"");}
    @Test public void challengersHaveReservedCapacityWithoutDisplacingFamilyBases(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{shadowRow(db,"base17","F17",100);assertTrue(ShadowAdmissionR26.canAdmit(db,"F17"));shadowRow(db,"child17","F17",100);assertFalse(ShadowAdmissionR26.canAdmit(db,"F17"));shadowRow(db,"base18","F18",100);shadowRow(db,"child18","F18",100);assertFalse(ShadowAdmissionR26.canAdmit(db,"F18"));assertTrue(ShadowAdmissionR26.canAdmit(db,"F19"));
            shadowRow(db,"base19","F19",100);assertFalse(ShadowAdmissionR26.canAdmit(db,"F19"));for(int n=0;n<29;n++)shadowRow(db,"other"+n,"SYNTHETIC"+n,100);assertEquals(ShadowAdmissionR26.LIMIT,ShadowAdmissionR26.count(db,null));assertFalse(ShadowAdmissionR26.canAdmit(db,"F20"));db.setTransactionSuccessful();}finally{db.endTransaction();}
    }}
    @Test public void challengerExpiryPreservesMainlineWeightsAndIrrevocableBirthBudget(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);for(String sql:SqlStatementsSU1.SQL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS su1_family_budget")||sql.startsWith("CREATE TABLE IF NOT EXISTS su1_alpha_reservation"))db.execSQL(sql);
            ModelGenomeSU2 base=FamilyRegistryR26.genome("F17").bindBirth(HashingSU1.text("SYNTHETIC_BASE"),90),child=FamilyRegistryR26.genome("F17").bindBirth(HashingSU1.text("SYNTHETIC_CHILD"),90);long birth=100,expiry=birth+QualificationWindowR26.LENGTH;
            for(ModelGenomeSU2 g:new ModelGenomeSU2[]{base,child}){ModelStoreSU2.saveGenome(db,new ModelEvolutionSU2.Ranked(g,"SYNTHETIC","",""),"SYNTHETIC",90,0,0,"LEAGUE",1);ModelStoreSU2.joinLeague(db,"SU2:"+g.key(),"SU2","SYNTHETIC",g.key(),birth);shadowRow(db,g.key(),"F17",birth);CertificateSU2.preregister(db,g.key(),birth);}
            long budgets;try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_family_budget",null)){c.moveToFirst();budgets=c.getLong(0);}assertEquals(0,ShadowAdmissionR26.retireOne(db,expiry-1,1000));assertEquals(0,ShadowAdmissionR26.retireOne(db,expiry,1000));db.execSQL("UPDATE r26_cohort SET checked_sequence=? WHERE genome_key=?",new Object[]{expiry,child.key()});
            db.execSQL("INSERT INTO su2_mainline_choice VALUES(?, ?,0,NULL,'SYNTHETIC',1,1)",new Object[]{birth,"SU2:"+child.key()});assertEquals(0,ShadowAdmissionR26.retireOne(db,expiry,1000));db.execSQL("INSERT INTO su2_mainline_choice VALUES(?, ?,0,NULL,'SYNTHETIC',1,1)",new Object[]{expiry,"SU2:"+base.key()});
            String certificate=HashingSU1.text("SYNTHETIC_ONLY_CERTIFICATE_FIXTURE");db.execSQL("INSERT INTO su2_certificate VALUES(?,?,?,100,200,1000,400,.31,.15,.30,0,1,.001,0,1,.01,1,2000)",new Object[]{certificate,child.key(),HashingSU1.text(child.text())});db.execSQL("INSERT INTO r26_certificate_protocol VALUES(?,?,0,'SYNTHETIC')",new Object[]{certificate,QualificationWindowR26.PROTOCOL});assertNotNull(CertificateSU2.read(db,child.key(),1000));assertEquals(0,ShadowAdmissionR26.retireOne(db,expiry+1,1000));db.execSQL("UPDATE su2_certificate SET expires_at=999 WHERE certificate_id=?",new Object[]{certificate});
            assertEquals(1,ShadowAdmissionR26.retireOne(db,expiry+1,1000));assertEquals(0,ShadowAdmissionR26.retireOne(db,expiry+1,1000));assertTrue(ShadowAdmissionR26.canAdmit(db,"F17"));
            try(Cursor c=db.rawQuery("SELECT l.status,l.state,q.from_sequence,q.family_alpha FROM r26_live_model l JOIN r26_cohort q ON q.genome_key=l.policy_hash WHERE l.policy_hash=?",new String[]{child.key()})){assertTrue(c.moveToFirst());assertEquals("RETIRED_PREREGISTERED_CHALLENGER",c.getString(0));assertArrayEquals(new byte[]{0x26,1},c.getBlob(1));assertEquals(birth,c.getLong(2));assertTrue(c.getDouble(3)>0);}
            try(Cursor c=db.rawQuery("SELECT count(*) FROM su1_family_budget",null)){c.moveToFirst();assertEquals(budgets,c.getLong(0));}try(Cursor c=db.rawQuery("SELECT status FROM r26_live_model WHERE policy_hash=?",new String[]{base.key()})){c.moveToFirst();assertEquals("ACTIVE",c.getString(0));}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void protectedIncumbentNeverEntersTheMutableGenomeParentCompiler(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{StrategySchemaSU2.ensure(db);ModelGenomeSU2 valid=FamilyRegistryR26.genome("F17").bindBirth(PolicyIdentityR26.sha256("SYNTHETIC_PARENT"),100);ModelStoreSU2.saveGenome(db,new ModelEvolutionSU2.Ranked(valid,"SYNTHETIC","",""),"SYNTHETIC",100,.4,.4,"LEAGUE",1);
            db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,val_fitness,created_at) VALUES(?,?,'SYNTHETIC','F15','INCUMBENT','SYNTHETIC',100,'LEAGUE',99,1)",new Object[]{XiIncumbentSU2.GENOME_KEY,XiIncumbentSU2.CANONICAL});List<ModelGenomeSU2> parents=FoundryCampaignR26.savedParents(db);assertEquals(1,parents.size());assertEquals(valid.key(),parents.get(0).key());try(Cursor c=db.rawQuery("SELECT canonical_text,state FROM su2_model_genome WHERE genome_key=?",new String[]{XiIncumbentSU2.GENOME_KEY})){assertTrue(c.moveToFirst());assertEquals(XiIncumbentSU2.CANONICAL,c.getString(0));assertEquals("LEAGUE",c.getString(1));}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void capabilityServiceIsBoundToNumericalSourceWhileWeightsStayPreserved(){try(SQLiteDatabase db=SQLiteDatabase.create(null)){
        schema(db);db.beginTransaction();try{String before=PolicyIdentityR26.sha256("SYNTHETIC_OLD_ABI"),after=PolicyIdentityR26.sha256("SYNTHETIC_NEW_ABI");assertTrue(RuntimeSchemaR26.bindCapabilitySource(db,before));db.execSQL("INSERT INTO r26_capability VALUES('F01','FAMILY','SYNTHETIC','TRAINED','SYNTHETIC',42)");byte[] weights={2,6,1};db.execSQL("INSERT INTO r26_live_model(policy_hash,canonical_text,state,state_hash,checkpoint_hash,parent_hash,through_ordinal,genesis_ordinal,born_ordinal,updated_at) VALUES('SYNTHETIC','SYNTHETIC',?,'SYNTHETIC','SYNTHETIC','',10,1,11,1)",new Object[]{weights});
            assertFalse(RuntimeSchemaR26.bindCapabilitySource(db,before));try(Cursor c=db.rawQuery("SELECT last_progress FROM r26_capability",null)){c.moveToFirst();assertEquals(42,c.getLong(0));}assertTrue(RuntimeSchemaR26.bindCapabilitySource(db,after));try(Cursor c=db.rawQuery("SELECT last_progress FROM r26_capability",null)){c.moveToFirst();assertEquals(0,c.getLong(0));}try(Cursor c=db.rawQuery("SELECT state,through_ordinal FROM r26_live_model",null)){c.moveToFirst();assertArrayEquals(weights,c.getBlob(0));assertEquals(10,c.getLong(1));}try(Cursor c=db.rawQuery("SELECT status FROM r26_live_model",null)){c.moveToFirst();assertEquals("BLOCKED_NUMERICAL_ABI_PRESERVED",c.getString(0));}db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }}
    @Test public void chronologicalStoreHistoryFeedsTheOriginalFoundryWithoutWeakeningInputChecks(){
        Models.Draw first=historical(1),second=historical(2),third=historical(3);List<Models.Draw> ascending=Arrays.asList(first,second,third);assertEquals(ascending,CampaignStateSU1.chronologicalHistory(ascending));
        List<Models.Draw> descending=Arrays.asList(third,second,first);assertEquals(ascending,CampaignStateSU1.chronologicalHistory(descending));assertSame(third,descending.get(0));
        for(List<Models.Draw> invalid:Arrays.asList(Arrays.asList(first,third),Arrays.asList(first,first,second))){try{CampaignStateSU1.chronologicalHistory(invalid);fail("invalid calendar accepted");}catch(IllegalStateException expected){assertEquals("SU1_HISTORY_NOT_CAUSAL_CONSECUTIVE",expected.getMessage());}}
        third.fetchTime=System.currentTimeMillis()+60000;try{CampaignStateSU1.chronologicalHistory(ascending);fail("future availability accepted");}catch(IllegalStateException expected){assertEquals("SU1_HISTORY_NOT_CAUSAL_CONSECUTIVE",expected.getMessage());}
    }
    @Test public void retiredPlatformJobNeverBecomesTheTrainingOwner(){Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();ReplayBurstSchedulerSU1.schedule(app);JobScheduler jobs=(JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(jobs!=null)assertNull(jobs.getPendingJob(ReplayBurstSchedulerSU1.JOB_ID));}
}
