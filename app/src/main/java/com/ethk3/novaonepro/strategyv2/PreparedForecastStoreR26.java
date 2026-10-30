package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.function.Supplier;
/** One bounded forecast per policy; checkpoint CAS makes a stale prepared forecast unusable. */
public final class PreparedForecastStoreR26 {
    private PreparedForecastStoreR26(){}
    public static ForecastSnapshotR26 read(SQLiteDatabase db,String policy,String checkpoint,String stateHash,long through)throws IOException{
        try(Cursor c=db.rawQuery("SELECT payload,payload_hash FROM r26_prepared_forecast WHERE policy_hash=? AND checkpoint_hash=? AND state_hash=? AND input_through=? AND target_ordinal=?",new String[]{policy,checkpoint,stateHash,String.valueOf(through),String.valueOf(Math.addExact(through,1))})){if(!c.moveToFirst())return null;byte[] bytes=c.getBlob(0);if(!LiveStateR26.hash(bytes).equals(c.getString(1)))throw new IOException("R26_PREPARED_FORECAST_HASH");return ForecastSnapshotR26.read(bytes);}
    }
    static boolean install(SQLiteDatabase db,String policy,String checkpoint,String hash,long through,ForecastSnapshotR26 forecast)throws IOException{
        if(!db.inTransaction())throw new IllegalStateException("R26_FORECAST_WRITER");byte[] bytes=forecast.bytes();
        try(Cursor c=db.rawQuery("SELECT 1 FROM r26_live_model WHERE policy_hash=? AND checkpoint_hash=? AND state_hash=? AND through_ordinal=? AND status='ACTIVE'",new String[]{policy,checkpoint,hash,String.valueOf(through)})){if(!c.moveToFirst())return false;}
        db.execSQL("INSERT OR REPLACE INTO r26_prepared_forecast VALUES(?,?,?,?,?,?,?,?)",new Object[]{policy,checkpoint,hash,through,Math.addExact(through,1),bytes,LiveStateR26.hash(bytes),System.currentTimeMillis()});return true;
    }
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws IOException{
        Models.Draw latest=store.getLatestDraw();if(latest==null)return 0;long through=TargetIssueResolver.sequenceOrdinalSU1(latest.issue);String policy=null,text=null,checkpoint=null,hash=null;byte[] state=null;
        try(Cursor c=LegacyResearchScopeR29.observationReader(store).rawQuery("SELECT l.policy_hash,l.canonical_text,l.checkpoint_hash,l.state_hash,l.state FROM r26_live_model l LEFT JOIN r26_prepared_forecast p ON p.policy_hash=l.policy_hash WHERE l.status='ACTIVE' AND l.through_ordinal=? AND (p.checkpoint_hash IS NULL OR p.checkpoint_hash<>l.checkpoint_hash OR p.state_hash<>l.state_hash OR p.input_through<>l.through_ordinal) ORDER BY coalesce(p.prepared_at,0),l.policy_hash LIMIT 1",new String[]{String.valueOf(through)})){if(c.moveToFirst()){policy=c.getString(0);text=c.getString(1);checkpoint=c.getString(2);hash=c.getString(3);state=c.getBlob(4);}}
        if(policy==null)return 0;if(!CurrentPolicyR26.matches(text,policy))return 0;budget.checkpointOrYield();PolicyRuntimeR26 learner=PolicyCheckpointR26.restore(CheckpointArtifactsR26.forStore(store),text,state,hash);ForecastSnapshotR26 forecast=new ForecastSnapshotR26(learner.forecast(),learner.table());
        final String key=policy,parent=checkpoint,source=hash;return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_PREPARE_NEXT_FORECAST",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());return install(db,key,parent,source,through,forecast)?1L:0L;});
    }
}
