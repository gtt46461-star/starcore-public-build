package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Actual continuation and scheduler-owner entry points over real WAL files. The Android Root RW
 * SELECT monitor is modelled explicitly; this is HOST evidence, not device timing acceptance. */
public final class ContinuationObservationR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_CONTINUATION_OBSERVATION|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventYieldR29HostTest.n(db,sql);}
    static final class Primary extends SQLiteDatabase {
        final ReentrantLock monitor=new ReentrantLock();final AtomicInteger reads=new AtomicInteger();
        Primary(SQLiteDatabase original){super(original.jdbc);}
        @Override public Cursor rawQuery(String sql,String[]args){
            boolean observation=sql.contains("r26_candidate")||sql.contains("r26_foundry")||sql.contains("r29_database_route")||sql.contains("r29_scheduler_owner");
            if(observation){reads.incrementAndGet();monitor.lock();}
            try{return super.rawQuery(sql,args);}finally{if(observation)monitor.unlock();}
        }
    }
    interface Work<T>{T run()throws Exception;}
    static <T>T whileHeld(Primary primary,String label,Work<T> work)throws Exception{
        CountDownLatch held=new CountDownLatch(1);AtomicBoolean released=new AtomicBoolean();AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread holder=new Thread(()->{primary.monitor.lock();try{held.countDown();Thread.sleep(1500);}catch(Throwable e){failure.set(e);}finally{released.set(true);primary.monitor.unlock();}},"synthetic-root-rw-select-monitor");holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("HOLDER_REQUIRED");
        T result;long start=System.nanoTime(),completedMs;boolean during=false;
        try{result=work.run();during=!released.get();completedMs=(System.nanoTime()-start)/1000000;}catch(IllegalStateException e){if("R26_UNSLICED_COMPUTATION_CAPACITY".equals(e.getMessage()))throw new AssertionError(label+"_BORROWED_BLOCKED_ROOT_RW_POOL",e);throw e;}finally{holder.join(2500);}
        check(label+"_COMPLETES_DURING_RW_SELECT_MONITOR_STALL",during&&failure.get()==null);
        System.out.println("R29_CONTINUATION_OBSERVATION|MEASURED|"+label+"|completedMs="+completedMs+"|modelledRootRwStallMs=1500");return result;
    }
    static long continuation(EventYieldR29HostTest.Fixture f,String task)throws Exception{
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim(task,"SYNTHETIC_CONTINUATION",1,0);
        long result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),TargetIssueResolver.sequenceOrdinalSU1("202610300001")+1);
        Field active=TaskContinuationR26.class.getDeclaredField("ACTIVE");active.setAccessible(true);int count=0;
        while(((Map<?,?>)active.get(null)).containsKey(task)&&count++<100)result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),TargetIssueResolver.sequenceOrdinalSU1("202610300001")+1);
        if(((Map<?,?>)active.get(null)).containsKey(task))throw new AssertionError("CONTINUATION_MUST_COMPLETE");return result;
    }
    static boolean ownerProbe(EventYieldR29HostTest.Fixture f)throws Exception{
        try(CooperativeContinuationR26<Boolean> work=new CooperativeContinuationR26<>("starcore-r26-scheduler-owner-probe",()->LegacyCapabilityAdapterR29.hasPoolOwner(f.store),()->false,null,System::nanoTime)){
            CooperativeContinuationR26.Slice<Boolean> result=work.slice(40);for(int i=0;!result.done&&i<100;i++)result=work.slice(40);
            if(result.error!=null)throw new AssertionError("OWNER_PROBE_FAILED",result.error);if(!result.done)throw new AssertionError("OWNER_PROBE_MUST_COMPLETE");return result.result;
        }
    }
    static void seed(EventYieldR29HostTest.Fixture f)throws Exception{
        DbStoreObservationR29HostTest.drawColumns(f.root);
        f.root.beginTransaction();try{
            RuntimeSchemaR26.ensure(f.root);LegacyCapabilityAdapterR29.initialize(f.root,"SYNTHETIC_CONTINUATION");
            f.root.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC_CONTINUATION',1,'HOST','RUNNING',1,0)");
            for(String task:new String[]{"warm-candidates","foundry-su2"})f.root.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,status,owner,fence,lease_until) VALUES(?,'P2',?,'RUNNING','SYNTHETIC_CONTINUATION',1,?)",new Object[]{task,task,System.currentTimeMillis()+120000});
            f.root.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi) VALUES('SYNTHETIC_DONE',?,?,X'','DONE',1,?)",new Object[]{StatsR27.sha256("SOURCE"),TargetIssueResolver.sequenceOrdinalSU1("202610300001"),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
            f.root.setTransactionSuccessful();
        }finally{f.root.endTransaction();}
    }
    public static void main(String[]args)throws Exception{
        String mode=args.length==0?"":args[0];
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            seed(f);Primary primary=new Primary(f.root);DbHelper helper=(DbHelper)EventYieldR29HostTest.get(f.coordinator,"helper");SQLiteOpenHelper.bind(helper,primary);
            if(mode.isEmpty()||mode.equals("--old-warm-control")){
                primary.reads.set(0);long result=whileHeld(primary,"WARM_CONTINUATION",()->continuation(f,"warm-candidates"));
                check("O1_ACTUAL_EMPTY_WARM_CONTINUATION_HAS_ZERO_PROGRESS_AND_NO_ROOT_RW_SELECT",result==0&&primary.reads.get()==0&&n(f.root,"SELECT count(*) FROM r26_progress")==0);
                if(!mode.isEmpty())return;
            }
            if(mode.isEmpty()||mode.equals("--old-foundry-control")){
                primary.reads.set(0);long result=whileHeld(primary,"FOUNDRY_CONTINUATION",()->continuation(f,"foundry-su2"));
                check("O2_ACTUAL_FOUNDATION_ABI_WRITER_AND_DONE_CAMPAIGN_SELECT_HAVE_ZERO_FAKE_PROGRESS",result==0&&primary.reads.get()==0&&n(f.root,"SELECT count(*) FROM r26_foundry WHERE campaign_id='SYNTHETIC_DONE' AND status='DONE'")==1&&n(f.root,"SELECT count(*) FROM r26_candidate")==0);
                if(!mode.isEmpty())return;
            }
            if(mode.isEmpty()||mode.equals("--old-owner-control")){
                primary.reads.set(0);boolean owner=whileHeld(primary,"SCHEDULER_PROBE",()->ownerProbe(f));
                check("O3_UNPUBLISHED_SCHEDULER_PROBE_NEVER_LEASES_ROOT_RW_SELECT_POOL",!owner&&primary.reads.get()==0);
                if(!mode.isEmpty())return;
            }
            SQLiteDatabase observation=LegacyResearchScopeR29.observationReader(f.store);
            check("O4_NO_SCOPE_OBSERVATION_IS_SAME_ROOT_FILE_PHYSICAL_READONLY",observation!=primary&&observation.isReadOnly()&&observation.getPath().equals(primary.getPath()));
            f.root.beginTransaction();try{f.root.execSQL("INSERT INTO app_state VALUES('OWN_UNCOMMITTED','visible',1)");SQLiteDatabase own=LegacyResearchScopeR29.observationReader(f.store);check("O5_ROOT_TRANSACTION_RETAINS_HANDLE_AND_UNCOMMITTED_OWN_WRITES",own==primary&&n(own,"SELECT count(*) FROM app_state WHERE state_key='OWN_UNCOMMITTED'")==1);}finally{f.root.endTransaction();}
            check("O6_ROLLBACK_DOES_NOT_LEAK_UNCOMMITTED_STATE",n(observation,"SELECT count(*) FROM app_state WHERE state_key='OWN_UNCOMMITTED'")==0);
            Path rpPath=Paths.get(primary.getPath()).getParent().resolve(ResearchRouteR29.FILE_NAME);
            try(SQLiteDatabase rp=SQLiteDatabase.openOrCreateDatabase(rpPath.toFile(),null)){
                rp.execSQL("PRAGMA journal_mode=WAL");rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','SCHEMA','ROOT',17,1)");rp.execSQL("CREATE TABLE observation_owner_probe(v INTEGER)");rp.execSQL("INSERT INTO observation_owner_probe VALUES(29)");rp.execSQL(LegacyCapabilityAdapterR29.DDL);rp.execSQL("INSERT INTO r29_scheduler_owner VALUES(1,'RP_FAKE','RESEARCH_POOL',17,1)");
                f.root.execSQL(ResearchRouteR29.ROUTE_DDL);f.root.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'SCHEMA','ROOT',17,1)",new Object[]{ResearchRouteR29.FILE_NAME});
                SQLiteDatabase outside=LegacyResearchScopeR29.observationReader(f.store);check("O7_ACTIVE_SCOPE_OUTSIDE_STILL_OBSERVES_ROOT",outside.getPath().equals(primary.getPath())&&outside.isReadOnly());
                rp.execSQL("UPDATE r29_research_owner SET schema_hash='WRONG_SCHEMA'");boolean mismatch=false;
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){try{LegacyResearchScopeR29.observationReader(f.store);}catch(IllegalStateException failure){mismatch=failure.getMessage().contains("OWNER_MISMATCH");}}
                check("O12_FIRST_RP_OWNER_MISMATCH_FAILS_CLOSED_WITHOUT_ROOT_FALLBACK",mismatch);
                rp.execSQL("UPDATE r29_research_owner SET schema_hash='SCHEMA'");
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){
                    SQLiteDatabase inside=LegacyResearchScopeR29.observationReader(f.store);check("O8_ACTIVE_RESEARCH_SCOPE_OBSERVES_VERIFIED_QUERY_ONLY_RP",inside.getPath().equals(rpPath.toString())&&n(inside,"PRAGMA query_only")==1&&n(inside,"SELECT v FROM observation_owner_probe")==29);
                    check("O9_RP_FAKE_POOL_OWNER_CANNOT_OVERRIDE_LEGACY_ROOT_OWNER",!LegacyCapabilityAdapterR29.hasPoolOwner(f.store));
                }
                f.root.execSQL("UPDATE r29_scheduler_owner SET dispatcher='RESEARCH_POOL' WHERE singleton=1");rp.execSQL("UPDATE r29_scheduler_owner SET dispatcher='LEGACY' WHERE singleton=1");
                check("O10_REAL_ROOT_POOL_OWNER_VISIBLE_OUTSIDE_SCOPE",LegacyCapabilityAdapterR29.hasPoolOwner(f.store));
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){check("O11_REAL_ROOT_POOL_OWNER_VISIBLE_INSIDE_ACTIVE_RESEARCH_SCOPE",LegacyCapabilityAdapterR29.hasPoolOwner(f.store));}
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(f.store)){
                    boolean own=LegacyResearchScopeR29.realtimeWrite(f.store,"R29_HOST_OBSERVATION_ROOT_OWN_WRITE",db->{
                        db.execSQL("INSERT INTO app_state VALUES('ACTIVE_REALTIME_OWN','visible',1)");
                        SQLiteDatabase selected=LegacyResearchScopeR29.observationReader(f.store);
                        return selected==db&&n(selected,"SELECT count(*) FROM app_state WHERE state_key='ACTIVE_REALTIME_OWN'")==1;
                    });
                    check("O14_EXPLICIT_REALTIME_TRANSACTION_INSIDE_ACTIVE_SCOPE_READS_OWN_ROOT_WRITE",own&&!LegacyResearchScopeR29.realtimeTransaction()&&LegacyResearchScopeR29.observationReader(f.store).getPath().equals(rpPath.toString()));
                }
            }
            f.root.execSQL("DROP TABLE r26_candidate");boolean missing=false;try{continuation(f,"warm-candidates");}catch(android.database.sqlite.SQLiteException failure){missing=failure.getMessage().contains("no such table");}
            // ACTIVE's RP intentionally lacks the candidate table as well: the real SQL error must survive.
            check("O13_REAL_MISSING_CANDIDATE_SCHEMA_NOT_REPORTED_AS_EMPTY_PROGRESS",missing);
        }
        System.out.println("R29_CONTINUATION_OBSERVATION|ALL_PASS|checks="+checks+"|REAL_CONTINUATIONS_AND_OWNER_HOST_MODEL_ONLY");
    }
}
