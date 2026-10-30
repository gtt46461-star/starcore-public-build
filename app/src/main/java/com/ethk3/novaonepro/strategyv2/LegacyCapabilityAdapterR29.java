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
    LegacyCapabilityAdapterR29(RuntimeCoordinatorR26 runtime){this.runtime=runtime;}
    public String id(){return "r29-legacy-capabilities";}
    public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{
        if(!runtime.poolDispatchReady())return ResearchPoolR27.Outcome.idle("WAITING_VERIFIED_CUTOVER_AND_LEGACY_DRAIN",0,-1);
        return runtime.runPoolLegacySlice();
    }
    static void initialize(SQLiteDatabase realtime,String session){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_SCHEDULER_OWNER_TRANSACTION");
        realtime.execSQL(DDL);
        realtime.execSQL("INSERT OR IGNORE INTO r29_scheduler_owner VALUES(1,?,'LEGACY',0,?)",new Object[]{session,System.currentTimeMillis()});
        realtime.execSQL("UPDATE r29_scheduler_owner SET session_id=?,dispatcher='LEGACY',epoch=0,changed_at=? WHERE singleton=1",new Object[]{session,System.currentTimeMillis()});
    }
    static void publish(DbStore store,String session){
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_LEGACY_DISPATCH_HANDOFF",db->{
            publishWithin(db,session);return 0;
        });
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
