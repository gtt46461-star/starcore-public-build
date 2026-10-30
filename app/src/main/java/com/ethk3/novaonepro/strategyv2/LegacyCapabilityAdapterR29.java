package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;

/** The legacy numerical implementations and their checkpoints remain executable. Only dispatch moves to the
 * research pool, after publication and after the old dispatch thread has drained its last fenced receipt.
 * A single pool task retains the original capability fairness and continuation machinery. */
public final class LegacyCapabilityAdapterR29 implements ResearchPoolR27.Task {
    public static final String DDL="CREATE TABLE IF NOT EXISTS r29_scheduler_owner(singleton INTEGER PRIMARY KEY CHECK(singleton=1),session_id TEXT NOT NULL,dispatcher TEXT NOT NULL,epoch INTEGER NOT NULL,changed_at INTEGER NOT NULL)";
    private final RuntimeCoordinatorR26 runtime;
    private static final java.util.concurrent.locks.ReentrantLock CONTINUITY=new java.util.concurrent.locks.ReentrantLock(true);
    private static final ThreadLocal<Boolean> POOL_CONTINUITY=new ThreadLocal<>();
    static final class CutoverLease implements AutoCloseable{public void close(){CONTINUITY.unlock();}}
    /** Drain the original service maintenance owner as well as the new pool before sealing. */
    static CutoverLease drainForCutover()throws InterruptedException{
        return CONTINUITY.tryLock(200,java.util.concurrent.TimeUnit.MILLISECONDS)?new CutoverLease():null;
    }
    LegacyCapabilityAdapterR29(RuntimeCoordinatorR26 runtime){this.runtime=runtime;}
    public String id(){return "r29-legacy-capabilities";}
    public boolean serviceDispatcher(){return true;}
    public boolean enabled(){return runtime.poolDispatchReady();}
    interface Dispatch {ResearchPoolR27.Outcome run()throws Exception;}
    /** Input-mirror bootstrap commits its own bounded pages before a legacy claim can enter. F01 preserves
     * that pending cause; waiting for the next page must not consume the dispatcher's error/quarantine budget. */
    static ResearchPoolR27.Outcome dispatch(Dispatch call)throws Exception{
        try{return call.run();}
        catch(Exception failure){
            if(!ResearchWriteQueueR27.writerBusy(failure))throw failure;
            return ResearchPoolR27.Outcome.idle("COOPERATIVE_WRITER_BUSY:LEGACY_DISPATCH",0,-1);
        }
    }
    public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{
        if(!runtime.poolDispatchReady())return ResearchPoolR27.Outcome.idle("WAITING_VERIFIED_CUTOVER_AND_LEGACY_DRAIN",0,-1);
        return dispatch(runtime::runPoolLegacySlice);
    }
    static void initialize(SQLiteDatabase realtime,String session){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_SCHEDULER_OWNER_TRANSACTION");
        realtime.execSQL(DDL);
        realtime.execSQL("INSERT OR IGNORE INTO r29_scheduler_owner VALUES(1,?,'LEGACY',0,?)",new Object[]{session,System.currentTimeMillis()});
        realtime.execSQL("UPDATE r29_scheduler_owner SET session_id=?,dispatcher='LEGACY',epoch=0,changed_at=? WHERE singleton=1",new Object[]{session,System.currentTimeMillis()});
    }
    static void publish(DbStore store,String session){
        boolean held=false;
        try{
            held=CONTINUITY.tryLock(200,java.util.concurrent.TimeUnit.MILLISECONDS);
            if(!held)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_CONTINUITY_DRAIN");
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_LEGACY_DISPATCH_HANDOFF",db->{publishWithin(db,session);return 0;});
        }catch(InterruptedException stop){Thread.currentThread().interrupt();throw new IllegalStateException("R29_LEGACY_DRAIN_INTERRUPTED",stop);}
        finally{if(held)CONTINUITY.unlock();}
    }
    public static boolean hasPoolOwner(DbStore store){
        if(store==null)return false;
        SQLiteDatabase realtime=store.coordinatorForResearchR952().realtimeReadOnlyR29();
        if(!ResearchRouteR29.active(realtime))return false;
        try(Cursor c=realtime.rawQuery("SELECT 1 FROM r29_scheduler_owner WHERE singleton=1 AND dispatcher='RESEARCH_POOL'",null)){return c.moveToFirst();}
    }
    /** Both the old service entry and the pool plugin use this gate. Publication takes it too, draining old work. */
    public static String enterContinuity(DbStore store,boolean pool){
        if(!CONTINUITY.tryLock())return "COOPERATIVE_LEGACY_CONTINUITY_ALREADY_RUNNING";
        try{
            SQLiteDatabase root=store.coordinatorForResearchR952().realtimeReadOnlyR29();
            if(ResearchRouteR29.pendingHandoff(root)||!pool&&ResearchRouteR29.active(root)){
                CONTINUITY.unlock();return "WAITING_DURABLE_RESEARCH_OWNER_HANDOFF";
            }
            boolean owner=hasPoolOwner(store);
            if(owner!=pool){CONTINUITY.unlock();return owner?"R29_LEGACY_CONTINUITY_OWNED_BY_RESEARCH_POOL":"WAITING_VERIFIED_RESEARCH_POOL_HANDOFF";}
            if(pool)POOL_CONTINUITY.set(true);return null;
        }catch(RuntimeException failed){CONTINUITY.unlock();throw failed;}
    }
    public static boolean onPoolContinuityThread(){return Boolean.TRUE.equals(POOL_CONTINUITY.get());}
    public static void leaveContinuity(){POOL_CONTINUITY.remove();CONTINUITY.unlock();}
    static final class ContinuityTask implements ResearchPoolR27.Task{
        final RuntimeCoordinatorR26 runtime;ContinuityTask(RuntimeCoordinatorR26 runtime){this.runtime=runtime;}
        public String id(){return "r29-legacy-continuity";}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker){
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(worker.store())){
            if(!runtime.poolDispatchReady())return ResearchPoolR27.Outcome.idle("WAITING_VERIFIED_LEGACY_HANDOFF",0,-1);
            if(!worker.store().coordinatorForResearchR952().isR811ResearchSchemaReady())return ResearchPoolR27.Outcome.idle("WAITING_LEGACY_RESEARCH_SCHEMA",0,-1);
            com.ethk3.novaonepro.ResearchRuntimeSqlGateR911.Report gate=com.ethk3.novaonepro.ResearchRuntimeSqlGateR911.verifyAndPublish(worker.store());
            if(!gate.passed)return ResearchPoolR27.Outcome.idle("WAITING_LEGACY_SQL_CONTRACT:"+gate.detail,0,-1);
            com.ethk3.novaonepro.ResearchLearningContinuityR912.Result r=com.ethk3.novaonepro.ResearchLearningContinuityR912.pulseOnResearchPool(worker.store(),500);
            if(r.errorLanes>0||r.status.startsWith("ERROR:"))throw new IllegalStateException("R29_LEGACY_CONTINUITY_FAILED:"+r.status);
            // A completed lane is a maintenance review, never a sample/gradient/certificate count.
            return r.completedLanes>0?ResearchPoolR27.Outcome.progress(r.completedLanes,0):ResearchPoolR27.Outcome.idle(r.status,0,-1);
            }
        }
    }
    static void publishWithin(SQLiteDatabase realtime,String session){
        if(!realtime.inTransaction()||!ResearchRouteR29.active(realtime))throw new IllegalStateException("R29_SCHEDULER_REQUIRES_PUBLISHED_RESEARCH");
        long epoch=MigrationCopyR29.scalar(realtime,"SELECT epoch FROM r29_database_route WHERE singleton=1",null);
        try(Cursor c=realtime.rawQuery("SELECT session_id FROM r29_scheduler_owner WHERE singleton=1",null)){
            if(!c.moveToFirst()||!session.equals(c.getString(0)))throw new IllegalStateException("R29_SCHEDULER_SESSION_FENCE");
        }
        realtime.execSQL("UPDATE r29_scheduler_owner SET dispatcher='RESEARCH_POOL',epoch=?,changed_at=? WHERE singleton=1 AND session_id=?",new Object[]{epoch,System.currentTimeMillis(),session});
    }
    static void requireOwner(SQLiteDatabase realtime,String session,boolean pool){
        try(Cursor c=realtime.rawQuery("SELECT dispatcher,epoch FROM r29_scheduler_owner WHERE singleton=1 AND session_id=?",new String[]{session})){
            if(!c.moveToFirst()||!(pool?"RESEARCH_POOL":"LEGACY").equals(c.getString(0)))throw new IllegalStateException("R29_WRONG_OR_STALE_TASK_DISPATCHER");
            if(pool&&(!ResearchRouteR29.active(realtime)||c.getLong(1)!=MigrationCopyR29.scalar(realtime,"SELECT epoch FROM r29_database_route WHERE singleton=1",null)))throw new IllegalStateException("R29_SCHEDULER_EPOCH_FENCE");
        }
    }
}
