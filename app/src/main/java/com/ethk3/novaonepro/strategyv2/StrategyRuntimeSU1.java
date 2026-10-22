package com.ethk3.novaonepro.strategyv2;

import android.util.Log;
import com.ethk3.novaonepro.*;

/** Optional research boundary. Failures are diagnosed and the durable pending ledger is retained. */
public final class StrategyRuntimeSU1 {
    private StrategyRuntimeSU1() {}
    public static void failure(DbStore store,String phase,RuntimeException failure){
        Log.w("STARCORE_SU1",phase,failure);
        try{if(StrategySchemaSU1.ready(store.readable()))store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_HEALTH",db->{StrategyRepositorySU1.health(db,phase,DiagnosticsSU1.cause(failure,1800),0);return 0;});}
        catch(RuntimeException unavailable){Log.w("STARCORE_SU1","health receipt deferred",unavailable);}
    }
    public static void capture(DbStore store,String target){
        try{LiveShadowLoopSU1.capture(store,target);}catch(BudgetTokenSU1.Yield yield){failure(store,"PREDRAW_YIELD",yield);}
        catch(RuntimeException failed){failure(store,"PREDRAW_FAILED",failed);}
    }
    public static void completed(DbStore store,Models.Draw draw,String hash,long completedAt){
        try{if(StrategySchemaSU1.ready(store.readable()))store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_COMPLETED_BRIDGE",db->LiveShadowLoopSU1.settleWithin(db,draw,hash,completedAt));}
        catch(RuntimeException failed){failure(store,"SETTLEMENT_PENDING_RECOVERY",failed);}
    }
}
