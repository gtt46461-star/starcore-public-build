package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Actual incumbent task and frame builder against real SQLite. The Android shared RW-pool
 * monitor stall is explicitly modelled by a host lock, not claimed as native Android evidence. */
public final class IncumbentObservationR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_INCUMBENT_OBSERVATION|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static final class Primary extends SQLiteDatabase {
        final ReentrantLock lock=new ReentrantLock();final AtomicInteger observations=new AtomicInteger();
        Primary(SQLiteDatabase db){super(db.jdbc);}
        @Override public Cursor rawQuery(String sql,String[] args){boolean observed=sql.contains("sqlite_master")||sql.contains("su2_live_decision")||sql.contains("su1_source_clock")||sql.contains("r26_expert_audit");if(observed){observations.incrementAndGet();lock.lock();}try{return super.rawQuery(sql,args);}finally{if(observed)lock.unlock();}}
    }
    interface Body<T>{T run()throws Exception;}
    static <T>T whileRwPoolHeld(Primary primary,String phase,Body<T> body)throws Exception{
        CountDownLatch held=new CountDownLatch(1);AtomicBoolean released=new AtomicBoolean();AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread holder=new Thread(()->{primary.lock.lock();try{held.countDown();Thread.sleep(1500);}catch(Throwable e){failure.set(e);}finally{released.set(true);primary.lock.unlock();}},"synthetic-root-wal-secondary-held");holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("ROOT_POOL_REQUIRED");
        long start=System.nanoTime();T result;
        try{result=body.run();check("I1_"+phase+"_COMPLETES_DURING_RW_POOL_STALL",!released.get()&&failure.get()==null);}
        catch(IllegalStateException e){if("R26_UNSLICED_COMPUTATION_CAPACITY".equals(e.getMessage()))throw new AssertionError(phase.startsWith("AUDIT_")?"AUDIT_BORROWED_BLOCKED_ROOT_RW_POOL":"INCUMBENT_BORROWED_BLOCKED_ROOT_RW_POOL",e);throw e;}
        finally{long millis=(System.nanoTime()-start)/1000000;System.out.println("R29_INCUMBENT_OBSERVATION|MEASURED|"+phase+"|bodyMs="+millis+"|modelledRootRwLeaseMs=1500");holder.join(2500);}
        check("I2_"+phase+"_NO_SHARED_ROOT_RW_OBSERVATION",primary.observations.get()==0);return result;
    }
    static long turn(EventYieldR29HostTest.Fixture f,long seq,String reason)throws Exception{
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("incumbent-forecast","SYNTHETIC_INCUMBENT_RO_"+UUID.randomUUID(),1,0);
        long result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),seq);
        java.lang.reflect.Field active=TaskContinuationR26.class.getDeclaredField("ACTIVE");active.setAccessible(true);
        for(int n=0;((Map<?,?>)active.get(null)).containsKey(claim.task)&&n<100;n++)result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),seq);
        if(((Map<?,?>)active.get(null)).containsKey(claim.task))throw new AssertionError("INCUMBENT_TASK_MUST_COMPLETE");
        check("I3_"+reason+"_ORIGINAL_REASON_NO_FAKE_PROGRESS",result==0&&reason.equals(EventYieldR29HostTest.get(claim,"reason")));return result;
    }
    static long auditTurn(EventYieldR29HostTest.Fixture f,long seq,String expert)throws Exception{
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("expert-"+expert,"SYNTHETIC_AUDIT_RO_"+UUID.randomUUID(),1,0);
        long result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),seq);
        java.lang.reflect.Field active=TaskContinuationR26.class.getDeclaredField("ACTIVE");active.setAccessible(true);
        for(int n=0;((Map<?,?>)active.get(null)).containsKey(claim.task)&&n<100;n++)result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),seq);
        if(((Map<?,?>)active.get(null)).containsKey(claim.task))throw new AssertionError("AUDIT_TASK_MUST_COMPLETE");
        check("A1_"+expert+"_COMMITTED_AUDIT_NOT_RECOUNTED_AS_PROGRESS",result==0);return result;
    }
    static void source(SQLiteDatabase root){
        root.execSQL("ALTER TABLE draws ADD COLUMN _id INTEGER DEFAULT 0");root.execSQL("ALTER TABLE draws ADD COLUMN open_time INTEGER DEFAULT 0");root.execSQL("ALTER TABLE draws ADD COLUMN fetch_time INTEGER DEFAULT 0");root.execSQL("ALTER TABLE draws ADD COLUMN source_line TEXT DEFAULT 'SYNTHETIC'");root.execSQL("ALTER TABLE draws ADD COLUMN dirty_reason TEXT DEFAULT ''");
        root.execSQL("DELETE FROM draws");long now=System.currentTimeMillis();
        for(int i=0;i<20;i++){long opened=now-10000-(19-i)*60000L;root.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,_id,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?,?,?)",new Object[]{String.format(Locale.ROOT,"20261030%04d",i+1),i+1,opened,opened+1000});}
        root.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");root.execSQL("CREATE TABLE IF NOT EXISTS su1_source_clock(singleton INTEGER PRIMARY KEY,generation INTEGER NOT NULL)");root.execSQL("INSERT OR REPLACE INTO su1_source_clock VALUES(1,7)");
    }
    public static void main(String[]args)throws Exception{
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            Primary primary=new Primary(f.root);DbHelper helper=(DbHelper)EventYieldR29HostTest.get(f.coordinator,"helper");SQLiteOpenHelper.bind(helper,primary);
            long seq=TargetIssueResolver.sequenceOrdinalSU1("202610300021");
            whileRwPoolHeld(primary,"SCHEMA_PENDING",()->turn(f,seq,"SU2_SCHEMA_PENDING"));
            f.root.beginTransaction();try{StrategySchemaSU2.ensure(f.root);f.root.setTransactionSuccessful();}finally{f.root.endTransaction();}source(f.root);primary.observations.set(0);
            whileRwPoolHeld(primary,"FULL_FRAME_HISTORY_SHORT",()->turn(f,seq,"INCUMBENT_HISTORY_SHORT"));
            EventFrameSU1 before=EventFrameBuilderSU1.capture(f.store,"202610300021");primary.observations.set(0);
            EventFrameSU1 after=whileRwPoolHeld(primary,"FRAME_SOURCE",()->{
                try(CooperativeContinuationR26<EventFrameSU1> continuation=new CooperativeContinuationR26<>("synthetic-incumbent-frame",()->EventFrameBuilderSU1.capture(f.store,"202610300021"),()->false,null,System::nanoTime)){
                    CooperativeContinuationR26.Slice<EventFrameSU1> slice=continuation.slice(40);for(int n=0;!slice.done&&n<100;n++)slice=continuation.slice(40);if(slice.error!=null)throw new AssertionError("REAL_FRAME_FAILED",slice.error);if(!slice.done)throw new AssertionError("FRAME_MUST_COMPLETE");return slice.result;
                }
            });
            check("I4_FRAME_SAME_TYPED_HISTORY_AND_SOURCE_IDENTITY",before.sourceVersionHash.equals(after.sourceVersionHash)&&before.payload().equals(after.payload())&&Arrays.deepEquals(before.history(),after.history())&&before.sourceGeneration==after.sourceGeneration&&after.size()==20);
            f.root.execSQL("INSERT INTO su2_live_decision VALUES(?,?,1,7,1,0.25,0.4,1,2,NULL,NULL)",new Object[]{IncumbentLoopSU2.KEY,seq});primary.observations.set(0);
            whileRwPoolHeld(primary,"ALREADY_FROZEN",()->turn(f,seq,"INCUMBENT_ALREADY_FROZEN; MISSING_OLD_PREFERENCES_NOT_RECONSTRUCTED"));
            f.root.beginTransaction();try{f.root.execSQL("UPDATE su1_source_clock SET generation=99");check("I5_CURRENT_ROOT_TRANSACTION_STILL_READS_OWN_GENERATION",ExpertWorkshopR26.snapshotReader(f.store)==primary&&!EventFrameBuilderSU1.unchanged(f.store,before));}finally{f.root.endTransaction();}
            check("I6_PREPARE_PROBES_PRESERVE_FROZEN_ROW_AND_NO_NEW_PROGRESS",EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM su2_live_decision")==1&&EventYieldR29HostTest.n(f.root,"SELECT generation FROM su1_source_clock")==7);
            f.root.beginTransaction();try{RuntimeSchemaR26.ensure(f.root);f.root.setTransactionSuccessful();}finally{f.root.endTransaction();}
            for(String expert:new String[]{"E01","E02","E15","E16"}){
                String id=HashingSU1.fields("R26_EXPERT_AUDIT_V1",expert,String.valueOf(seq),"7",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
                f.root.execSQL("INSERT INTO r26_expert_audit VALUES(?,?,?,?,?)",new Object[]{id,expert,seq,"SYNTHETIC_PRIOR_COMMITTED_AUDIT",1});primary.observations.set(0);
                whileRwPoolHeld(primary,"AUDIT_"+expert,()->auditTurn(f,seq,expert));
            }
            check("A2_ORIGINAL_AUDIT_ROWS_REMAIN_EXACT",EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM r26_expert_audit WHERE receipt_json='SYNTHETIC_PRIOR_COMMITTED_AUDIT'")==4);
        }
        System.out.println("R29_INCUMBENT_OBSERVATION|ALL_PASS|checks="+checks+"|ACTUAL_TASK_AND_FRAME_REAL_SQLITE_MODELLED_RW_POOL_STALL_NOT_ANDROID_ACCEPTANCE");
    }
}
