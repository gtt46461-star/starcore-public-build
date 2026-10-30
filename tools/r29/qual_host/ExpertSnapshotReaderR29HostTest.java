package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;

/** Actual expert snapshot restore and 40ms task continuation, real WAL files, and a modelled Android RW-pool
 * monitor. It does not claim that JDBC reproduces Android's native connection-pool implementation. */
public final class ExpertSnapshotReaderR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_EXPERT_SNAPSHOT|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static final class Primary extends SQLiteDatabase {
        final ReentrantLock lock;final AtomicInteger expertReads=new AtomicInteger(),routeReads=new AtomicInteger();
        Primary(SQLiteDatabase original,ReentrantLock lock){super(original.jdbc);this.lock=lock;}
        @Override public Cursor rawQuery(String sql,String[] args){boolean snapshot=sql.contains("r26_expert_experiment")||sql.contains("r26_foundry")||sql.contains("r26_capability"),route=sql.contains("r29_database_route");if(snapshot)expertReads.incrementAndGet();if(route)routeReads.incrementAndGet();if(snapshot||route)lock.lock();try{return super.rawQuery(sql,args);}finally{if(snapshot||route)lock.unlock();}}
    }
    static FoundryCampaignR26.Snapshot source(){
        FoundryCampaignR26.Snapshot s=new FoundryCampaignR26.Snapshot();s.ordinal=new long[32];s.issues=new long[32];s.opened=new long[32];s.available=new long[32];s.draws=new int[32][10];long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001");
        for(int i=0;i<32;i++){s.ordinal[i]=first+i;s.issues[i]=Long.parseLong(TargetIssueResolver.fromSequenceOrdinalSU1(first+i));s.opened[i]=1000L+i*60000;s.available[i]=s.opened[i]+1000;for(int p=0;p<10;p++)s.draws[i][p]=(p+i)%10+1;}s.through=s.ordinal[31];return s;
    }
    static long turn(EventYieldR29HostTest.Fixture f,String expert)throws Exception{
        RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("expert-"+expert,"SYNTHETIC_EXPERT_RO_"+UUID.randomUUID(),1,0);BudgetTokenSU1 budget=new BudgetTokenSU1(10000,100000000,()->false);
        long result=TaskContinuationR26.step(f.store,claim,budget,source().through+1);int slices=1;
        Field active=TaskContinuationR26.class.getDeclaredField("ACTIVE");active.setAccessible(true);
        while(((Map<?,?>)active.get(null)).containsKey(claim.task)&&slices++<200){result=TaskContinuationR26.step(f.store,claim,new BudgetTokenSU1(10000,100000000,()->false),source().through+1);}
        return result;
    }
    static void runtimeReadWhileHeld(EventYieldR29HostTest.Fixture f,Primary primary,String expert,String phase)throws Exception{
        CountDownLatch held=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();AtomicBoolean released=new AtomicBoolean();
        Thread holder=new Thread(()->{primary.lock.lock();try(DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){held.countDown();Thread.sleep(1500);}catch(Throwable e){failure.set(e);held.countDown();}finally{released.set(true);primary.lock.unlock();}},"synthetic-root-primary-held");holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("ROOT_POOL_GUARD_REQUIRED");
        long start=System.nanoTime();long result,completedMs=0;boolean completedWhileHeld=false;
        try{result=turn(f,expert);completedWhileHeld=!released.get();completedMs=(System.nanoTime()-start)/1000000;}catch(IllegalStateException e){if(e.getMessage().equals("R26_UNSLICED_COMPUTATION_CAPACITY"))throw new AssertionError("EXPERT_SNAPSHOT_BORROWED_BLOCKED_ROOT_RW_POOL",e);throw e;}finally{holder.join(2500);}
        // Measure through completion of the task, excluding the deliberate holder's remaining lifetime.
        System.out.println("R29_EXPERT_SNAPSHOT|MEASURED|"+phase+"|completedMs="+completedMs+"|modelledRootRwLeaseMs=1500");
        check("X1_"+phase+"_ACTUAL_CONTINUATION_HAS_NO_FAKE_PROGRESS",result==0&&failure.get()==null&&completedWhileHeld);
        check("X2_"+phase+"_PURE_SNAPSHOT_QUERIES_NEVER_LEASE_ROOT_RW_POOL",primary.expertReads.get()==0);
    }
    static void routeProbeWhileHeld(EventYieldR29HostTest.Fixture f,Primary primary)throws Exception{
        CountDownLatch held=new CountDownLatch(1);AtomicBoolean released=new AtomicBoolean();AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread holder=new Thread(()->{primary.lock.lock();try{held.countDown();Thread.sleep(1500);}catch(Throwable e){failure.set(e);}finally{released.set(true);primary.lock.unlock();}},"synthetic-root-secondary-open-held");holder.start();if(!held.await(2,TimeUnit.SECONDS))throw new AssertionError("ROOT_POOL_REQUIRED");
        boolean completedWhileHeld=false;SQLiteDatabase observed=null;
        try(CooperativeContinuationR26<SQLiteDatabase> continuation=new CooperativeContinuationR26<>("starcore-r26-expert-E12-route-probe",()->{
            SQLiteDatabase authority=LegacyResearchScopeR29.rawRealtime(f.store);if(ResearchRouteR29.active(authority))throw new AssertionError("PREPUBLICATION_ROUTE_REQUIRED");return authority;
        },()->false,null,System::nanoTime)){
            CooperativeContinuationR26.Slice<SQLiteDatabase> slice=continuation.slice(40);for(int n=0;!slice.done&&n<20;n++)slice=continuation.slice(40);
            if(slice.error!=null)throw new AssertionError("REAL_ROUTE_PROBE_FAILED",slice.error);if(!slice.done)throw new AssertionError("ROUTE_PROBE_MUST_COMPLETE");observed=slice.result;completedWhileHeld=!released.get();
        }catch(IllegalStateException e){if("R26_UNSLICED_COMPUTATION_CAPACITY".equals(e.getMessage()))throw new AssertionError("ROUTE_PROBE_BORROWED_BLOCKED_ROOT_RW_POOL",e);throw e;}finally{holder.join(2500);}
        check("X10_AUTHORITY_ROUTE_PROBE_COMPLETES_DURING_ROOT_RW_POOL_STALL",completedWhileHeld&&failure.get()==null&&primary.routeReads.get()==0);
        check("X11_AUTHORITY_ROUTE_PROBE_USES_SAME_FILE_PHYSICAL_READONLY",observed!=primary&&observed.isReadOnly()&&observed.getPath().equals(primary.getPath()));
    }
    public static void main(String[]args)throws Exception{
        try(EventYieldR29HostTest.Fixture f=new EventYieldR29HostTest.Fixture()){
            for(SQLiteDatabase db:new SQLiteDatabase[]{f.root,f.research}){db.beginTransaction();try{RuntimeSchemaR26.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
            Primary primary=new Primary(f.root,new ReentrantLock());DbHelper helper=(DbHelper)EventYieldR29HostTest.get(f.coordinator,"helper");SQLiteOpenHelper.bind(helper,primary);
            // First exercise the product task before referencing the new helper, so old-source controls fail on
            // the actual unsliced-capacity bug rather than a missing helper method.
            runtimeReadWhileHeld(f,primary,"E07","EMPTY_E07");
            routeProbeWhileHeld(f,primary);
            SQLiteDatabase reader=ExpertWorkshopR26.snapshotReader(f.store);
            check("X3_ROOT_OBSERVATION_IS_INDEPENDENT_READONLY_AND_SAME_FILE",reader!=primary&&reader.isReadOnly()&&reader.getPath().equals(primary.getPath())&&EventYieldR29HostTest.n(reader,"PRAGMA query_only")==1);
            f.root.beginTransaction();try{f.root.execSQL("INSERT INTO app_state VALUES('uncommitted-expert-probe','visible',1)");SQLiteDatabase transaction=ExpertWorkshopR26.snapshotReader(f.store);check("X4_SAME_TRANSACTION_RETAINS_ORIGINAL_HANDLE_AND_OWN_WRITES",transaction==primary&&LegacyResearchScopeR29.rawRealtime(f.store)==primary&&EventYieldR29HostTest.n(transaction,"SELECT count(*) FROM app_state WHERE state_key='uncommitted-expert-probe'")==1);}finally{f.root.endTransaction();}
            FoundryCampaignR26.Snapshot source=source();byte[] snapshot=source.encode();String sourceHash=LiveStateR26.hash(snapshot);
            for(String expert:new String[]{"E07","E10"}){
                ExpertExperimentR26.Pair pair=new ExpertExperimentR26.Pair(ExpertExperimentR26.spec(expert),sourceHash,source.through);for(int[] row:source.draws)pair.advance(row,false);byte[] state=pair.encode();String id="SYNTHETIC_"+expert;
                f.root.execSQL("INSERT OR REPLACE INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,?, 'RUNNING',1,?,?)",new Object[]{id,sourceHash,source.through,snapshot,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,source.ordinal[0]});
                f.root.execSQL("INSERT INTO r26_expert_experiment(experiment_id,expert_id,campaign_id,cursor,state,state_hash,checkpoint_hash,numerical_abi,proposal_json,status,created_at) VALUES(?,?,?,?,?,?,'',?,'{}','RUNNING',1)",new Object[]{id,expert,id,32,state,LiveStateR26.hash(state),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
                primary.expertReads.set(0);runtimeReadWhileHeld(f,primary,expert,"RESTORED_"+expert);
                byte[] restored;try(Cursor c=ExpertWorkshopR26.snapshotReader(f.store).rawQuery("SELECT state FROM r26_expert_experiment WHERE experiment_id=?",new String[]{id})){if(!c.moveToFirst())throw new AssertionError("FULL_STATE_REQUIRED");restored=c.getBlob(0);}
                ExpertExperimentR26.Pair copy=ExpertExperimentR26.Pair.decode(restored);copy.requireSource(sourceHash,source.ordinal[0],source.through);
                check("X5_"+expert+"_FULL_SNAPSHOT_STATE_AND_CURSOR_ARE_BYTE_EXACT",Arrays.equals(state,restored)&&Arrays.equals(state,copy.encode())&&copy.cursor==32&&copy.left.samples()==32&&copy.right.samples()==32);
                int[] next={10,9,8,7,6,5,4,3,2,1};pair.advance(next,true);copy.advance(next,true);
                check("X6_"+expert+"_NEXT_PAIRED_UPDATE_AND_COMPLETE_STATE_UNCHANGED",Arrays.equals(pair.encode(),copy.encode()));
            }
            f.root.execSQL("DROP TABLE r26_expert_experiment");boolean missing=false;try{turn(f,"E07");}catch(android.database.sqlite.SQLiteException expected){missing=expected.getMessage().contains("no such table");}
            check("X7_REAL_MISSING_EXPERT_SCHEMA_STILL_FAILS",missing);
            Path activePath=Paths.get(primary.getPath()).getParent().resolve(ResearchRouteR29.FILE_NAME);
            try(SQLiteDatabase rp=SQLiteDatabase.openOrCreateDatabase(activePath.toFile(),null)){
                rp.execSQL("PRAGMA journal_mode=WAL");rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY',?,?,17,1)",new Object[]{StatsR27.sha256("WRONG_SCHEMA"),StatsR27.sha256("ROOT")});rp.execSQL("CREATE TABLE expert_owner_probe(v INTEGER)");rp.execSQL("INSERT INTO expert_owner_probe VALUES(29)");
                f.root.execSQL(ResearchRouteR29.ROUTE_DDL);f.root.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,?,?,17,1)",new Object[]{ResearchRouteR29.FILE_NAME,StatsR27.sha256("SCHEMA"),StatsR27.sha256("ROOT")});
                boolean owner=false;try{ExpertWorkshopR26.snapshotReader(f.store);}catch(IllegalStateException expected){owner=expected.getMessage().contains("OWNER_MISMATCH");}
                check("X8_ACTIVE_ROUTE_OWNER_MISMATCH_CANNOT_FALL_BACK_TO_ROOT",owner);
                rp.execSQL("UPDATE r29_research_owner SET schema_hash=?",new Object[]{StatsR27.sha256("SCHEMA")});
                SQLiteDatabase selected=ExpertWorkshopR26.snapshotReader(f.store);
                check("X9_ACTIVE_ROUTE_SELECTS_VERIFIED_QUERY_ONLY_RP",selected.getPath().equals(activePath.toString())&&EventYieldR29HostTest.n(selected,"PRAGMA query_only")==1&&EventYieldR29HostTest.n(selected,"SELECT v FROM expert_owner_probe")==29);
            }
        }
        System.out.println("R29_EXPERT_SNAPSHOT|ALL_PASS|checks="+checks+"|ACTUAL_EXPERT_CONTINUATION_REAL_WAL_MODELLED_ANDROID_RW_POOL_NOT_DEVICE_ACCEPTANCE");
    }
}
