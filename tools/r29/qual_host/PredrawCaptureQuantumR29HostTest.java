package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.PITSnapshotContractR952;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Full real 5000-row DbStore/PIT/frame pipeline. Delays are explicit HOST-only bounded read
 * pressure; they do not claim Android CPU measurements, device acceptance, or natural quality. */
public final class PredrawCaptureQuantumR29HostTest {
    static int checks;static final String TARGET="202610300101";
    static void check(String id,boolean ok){System.out.println("R29_PREDRAW_CAPTURE|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static void populate(SQLiteDatabase db){
        db.execSQL("ALTER TABLE draws ADD COLUMN _id INTEGER DEFAULT 0");db.execSQL("ALTER TABLE draws ADD COLUMN open_time INTEGER DEFAULT 0");db.execSQL("ALTER TABLE draws ADD COLUMN fetch_time INTEGER DEFAULT 0");db.execSQL("ALTER TABLE draws ADD COLUMN source_line TEXT DEFAULT 'SYNTHETIC'");db.execSQL("ALTER TABLE draws ADD COLUMN dirty_reason TEXT DEFAULT ''");
        long last=TargetIssueResolver.sequenceOrdinalSU1("202610300100"),opened=System.currentTimeMillis()-1000;
        db.beginTransaction();try{db.execSQL("DELETE FROM draws");for(int i=0;i<5000;i++)db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,_id,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?,?,?)",new Object[]{TargetIssueResolver.fromSequenceOrdinalSU1(last-4999+i),i+1,opened-(4999-i)*60000L,opened-(4999-i)*60000L+1});db.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");db.execSQL("CREATE TABLE IF NOT EXISTS su1_source_clock(singleton INTEGER PRIMARY KEY,generation INTEGER NOT NULL)");db.execSQL("INSERT OR REPLACE INTO su1_source_clock VALUES(1,7)");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    static AtomicInteger boundedReads(){
        AtomicInteger reads=new AtomicInteger();SQLiteDatabase.hostBeforeQuery=(db,sql)->{
            if(sql.startsWith("SELECT generation FROM su1_source_clock")||sql.contains(" FROM draws")&&sql.contains("dirty=0 AND issue<")){
                reads.incrementAndGet();try{Thread.sleep(600);}catch(InterruptedException cancelled){Thread.currentThread().interrupt();}
            }
        };return reads;
    }
    static CooperativeContinuationR26<EventFrameSU1> capture(EventYieldR29HostTest.Fixture f,String owner){return new CooperativeContinuationR26<>("actual-full-5000-predraw-capture",()->EventFrameBuilderSU1.capture(f.store,TARGET),()->false,null,System::nanoTime,owner);}
    static EventFrameSU1 pressuredCapture(EventYieldR29HostTest.Fixture f)throws Exception{
        AtomicInteger reads=boundedReads();long started=System.nanoTime(),max=0;int slices=0;
        try(CooperativeContinuationR26<EventFrameSU1> continuation=capture(f,"SYNTHETIC_CAPTURE_PRESSURE")){
            CooperativeContinuationR26.Slice<EventFrameSU1> slice;do{long entered=System.nanoTime();slice=continuation.slice(40);max=Math.max(max,(System.nanoTime()-entered)/1000000);slices++;}while(!slice.done&&slices<500);
            if(slice.error!=null)throw new AssertionError("FULL_CAPTURE_BODY_FAILURE",slice.error);
            check("Q1_COMPLETE_5000_FRAME_YIELDS_BETWEEN_INDEPENDENT_BOUNDED_READS",slice.done&&slices>=3&&reads.get()==3&&max<1040);
            System.out.println("R29_PREDRAW_CAPTURE|MEASURED|wallMs="+(System.nanoTime()-started)/1000000+"|slices="+slices+"|maxSliceMs="+max+"|hostBoundedReads="+reads.get()+"|eachWaitMs=600|CPU_NOT_CLAIMED");return slice.result;
        }catch(IllegalStateException capacity){if("R26_UNSLICED_COMPUTATION_CAPACITY".equals(capacity.getMessage()))throw new AssertionError("PREDRAW_CAPTURE_MISSING_POLL_BETWEEN_BOUNDED_READS",capacity);throw capacity;}
        finally{SQLiteDatabase.hostBeforeQuery=null;if(!CooperativeContinuationR26.awaitOwnerStopped("SYNTHETIC_CAPTURE_PRESSURE",3000))throw new AssertionError("CAPTURE_PRESSURE_OWNER_MUST_DRAIN");}
    }
    static void cancellation(EventYieldR29HostTest.Fixture f)throws Exception{
        boundedReads();String owner="SYNTHETIC_CAPTURE_CANCEL";
        try(CooperativeContinuationR26<EventFrameSU1> continuation=capture(f,owner)){
            check("Q3_CAPTURE_PAUSED_BEFORE_NEXT_READ_WITHOUT_WRITER_TRANSACTION",!continuation.slice(40).done&&!f.root.inTransaction());continuation.close();
            check("Q4_CANCEL_DRAINS_CAPTURE_OWNER",CooperativeContinuationR26.awaitOwnerStopped(owner,3000));
        }finally{SQLiteDatabase.hostBeforeQuery=null;}
        check("Q5_CANCEL_MAKES_NO_DURABLE_SOURCE_CHANGE",EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM draws")==5000&&EventYieldR29HostTest.n(f.root,"SELECT generation FROM su1_source_clock")==7);
    }
    static void criticalRollback(EventYieldR29HostTest.Fixture f)throws Exception{
        boundedReads();String owner="SYNTHETIC_CAPTURE_CRITICAL";boolean failed=false,diagnosed=false;
        try(CooperativeContinuationR26<EventFrameSU1> continuation=new CooperativeContinuationR26<>("synthetic-capture-in-actual-sqlite-transaction",()->{
            try(CooperativeContinuationR26.Critical critical=CooperativeContinuationR26.critical()){
                f.root.beginTransaction();try{f.root.execSQL("INSERT INTO app_state VALUES('capture-critical-uncommitted','probe',1)");EventFrameSU1 frame=EventFrameBuilderSU1.capture(f.store,TARGET);f.root.setTransactionSuccessful();return frame;}finally{f.root.endTransaction();}
            }
        },()->false,null,System::nanoTime,owner)){
            try{continuation.slice(40);}catch(IllegalStateException expected){failed="R26_UNSLICED_COMPUTATION_CAPACITY".equals(expected.getMessage());for(Throwable diagnostic:expected.getSuppressed())if(diagnostic.getMessage().contains("R26_UNSLICED_WORKER_DIAGNOSTIC")&&diagnostic.getMessage().contains("criticalDepth=1")&&diagnostic.getStackTrace().length>0)diagnosed=true;}
            check("Q6_REAL_TRANSACTION_CANNOT_PARK_OR_HIDE_ORIGINAL_CAPACITY_ERROR",failed);check("Q7_CAPACITY_RETAINS_FAILURE_TIME_WORKER_STACK_AND_CRITICAL_DEPTH",diagnosed);
            continuation.close();check("Q8_CRITICAL_WORKER_DRAINS_AFTER_CANCEL",CooperativeContinuationR26.awaitOwnerStopped(owner,3000));
        }finally{SQLiteDatabase.hostBeforeQuery=null;}
        check("Q9_CANCELLED_REAL_TRANSACTION_ROLLS_BACK_UNCOMMITTED_ROW",!f.root.inTransaction()&&EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM app_state WHERE state_key='capture-critical-uncommitted'")==0);
    }
    static void sourceChange(EventYieldR29HostTest.Fixture f)throws Exception{
        AtomicBoolean changed=new AtomicBoolean();SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(sql.contains(" FROM draws")&&sql.contains("dirty=0 AND issue<")&&changed.compareAndSet(false,true))f.root.execSQL("UPDATE su1_source_clock SET generation=8");};boolean rejected=false;
        try(CooperativeContinuationR26<EventFrameSU1> continuation=capture(f,"SYNTHETIC_CAPTURE_CHANGED")){
            CooperativeContinuationR26.Slice<EventFrameSU1> slice;do{slice=continuation.slice(40);}while(!slice.done);rejected=slice.error instanceof IllegalStateException&&"SU1_SOURCE_CHANGED_DURING_READ".equals(slice.error.getMessage());
        }finally{SQLiteDatabase.hostBeforeQuery=null;}
        check("Q10_REAL_CONCURRENT_SOURCE_CHANGE_REMAINS_REJECTED",changed.get()&&rejected);f.root.execSQL("UPDATE su1_source_clock SET generation=7");
    }
    static void incumbentWholePipeline(EventYieldR29HostTest.Fixture f)throws Exception{
        // A real history gap keeps the original incumbent's 1560-row numerical minimum unsatisfied,
        // while its real PIT loader must still capture/validate all5000 source rows first.
        long last=TargetIssueResolver.sequenceOrdinalSU1("202610300100"),seq=last+1;
        f.root.beginTransaction();try{
            StrategySchemaSU2.ensure(f.root);f.root.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{TargetIssueResolver.fromSequenceOrdinalSU1(last-20)});
            f.root.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,_id,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,5001,1,2)",new Object[]{TargetIssueResolver.fromSequenceOrdinalSU1(last-5000)});f.root.setTransactionSuccessful();
        }finally{f.root.endTransaction();}
        AtomicInteger reads=boundedReads();RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("incumbent-forecast","SYNTHETIC_FULL_PIT_TASK_"+UUID.randomUUID(),1,0);long result=0,max=0;int slices=0;
        java.lang.reflect.Field active=TaskContinuationR26.class.getDeclaredField("ACTIVE");active.setAccessible(true);
        try{
            do{long entered=System.nanoTime();result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),seq);max=Math.max(max,(System.nanoTime()-entered)/1000000);slices++;}while(((Map<?,?>)active.get(null)).containsKey(claim.task)&&slices<500);
            check("Q12_ACTUAL_INCUMBENT_TASK_CAPTURES_ALL5000_WITH_BOUNDED_QUANTA",!((Map<?,?>)active.get(null)).containsKey(claim.task)&&reads.get()==3&&slices>=3&&max<1040&&EventYieldR29HostTest.n(f.root,"SELECT count(*) FROM draws")==5000);
            check("Q13_ORIGINAL_HISTORY_MINIMUM_AND_ZERO_PROGRESS_REMAIN",result==0&&"INCUMBENT_HISTORY_SHORT".equals(EventYieldR29HostTest.get(claim,"reason")));
            System.out.println("R29_PREDRAW_CAPTURE|ACTUAL_INCUMBENT_TASK|slices="+slices+"|maxStepMs="+max+"|sourceRows=5000|newestContiguousRows=20|numericalProgress=0");
        }catch(IllegalStateException capacity){if("R26_UNSLICED_COMPUTATION_CAPACITY".equals(capacity.getMessage()))throw new AssertionError("PREDRAW_TASK_MISSING_CAPTURE_POLL",capacity);throw capacity;}
        finally{SQLiteDatabase.hostBeforeQuery=null;TaskContinuationR26.cancel(claim.owner);if(!CooperativeContinuationR26.awaitOwnerStopped(claim.owner,3000))throw new AssertionError("INCUMBENT_CAPTURE_TASK_MUST_DRAIN");}
    }
    public static void main(String[]args)throws Exception{
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            populate(f.root);if(args.length>0&&"old-control".equals(args[0])){pressuredCapture(f);return;}if(args.length>0&&"old-task-control".equals(args[0])){incumbentWholePipeline(f);return;}
            EventFrameSU1 baseline=EventFrameBuilderSU1.capture(f.store,TARGET),actual=pressuredCapture(f);
            check("Q2_ALL_5000_TYPED_ROWS_SOURCE_HASH_AND_PAYLOAD_UNCHANGED",actual.size()==5000&&actual.sourceVersionHash.equals(baseline.sourceVersionHash)&&actual.sourceGeneration==baseline.sourceGeneration&&actual.payload().equals(baseline.payload())&&Arrays.deepEquals(actual.history(),baseline.history()));
            PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(f.store,TARGET,5000),restored=PITSnapshotContractR952.fromFrozenHistory(TARGET,pit.history,pit.snapshotHash);restored.requireConsecutive();
            check("Q11_FROZEN_PIT_HASH_AND_ALL_5000_ORIGINAL_ROWS_RESTORE",restored.snapshotHash.equals(pit.snapshotHash)&&restored.history.size()==5000&&restored.firstIssue.equals(pit.firstIssue)&&restored.lastIssue.equals(pit.lastIssue));
            cancellation(f);criticalRollback(f);sourceChange(f);incumbentWholePipeline(f);
        }
        System.out.println("R29_PREDRAW_CAPTURE|ALL_PASS|checks="+checks+"|REAL_5000_ROW_PIPELINE_BOUNDED_HOST_READ_PRESSURE_NOT_ANDROID_CPU_ACCEPTANCE");
    }
}
