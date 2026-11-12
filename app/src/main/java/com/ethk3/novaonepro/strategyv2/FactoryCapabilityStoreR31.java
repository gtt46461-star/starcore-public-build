package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Existing research owner/cache, isolated source+protocol namespace. State,
 * complete search ledger and trial receipt commit in the same fenced batch. */
public final class FactoryCapabilityStoreR31 {
    private FactoryCapabilityStoreR31() {}
    private static String read(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r31-factory' AND key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):null;}}
    public static void advance(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String source,long at,boolean allowed){
        String key=source+":"+FactoryCapabilityExperimentR31.protocolHash(),old=read(read,key),oldHash=old==null?null:StatsR27.sha256(old);
        try(FactoryCapabilityExperimentR31 state=old==null?new FactoryCapabilityExperimentR31(source):FactoryCapabilityExperimentR31.restore(old)){
            if(!source.equals(state.source()))throw new IllegalStateException("FACTORY_SOURCE_IDENTITY");
            if(allowed&&!state.complete())state.step();
            String checkpoint=state.snapshot();if(checkpoint.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>8*1024*1024)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);
            Map<String,Object> summary=new LinkedHashMap<>(state.summary());summary.put("schedulerState",allowed?"COMMITTED_STEP_OR_COMPLETED":"WAIT_REALTIME_WINDOW");summary.put("stateKey",key);
            batch.guardBefore(db->{String current=read(db,key);return Objects.equals(oldHash,current==null?null:StatsR27.sha256(current))?0:-1;});
            batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r31-factory',?,?,?)",key,checkpoint,at);
            Map<String,Object> artifact=state.pendingArtifact();if(artifact!=null){String identity=key+":"+artifact.get("arm")+":"+artifact.get("seed");batch.add("INSERT OR IGNORE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r31-factory-trial',?,?,?)",identity,JsonR27.write(artifact),at);}
            batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30','q08-factory-r31',?,?)",JsonR27.write(summary),at);
        }
    }
}
