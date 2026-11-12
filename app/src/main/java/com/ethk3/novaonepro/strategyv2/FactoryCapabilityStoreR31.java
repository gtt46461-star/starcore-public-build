package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Existing research owner/cache, isolated source+protocol namespace. State,
 * complete search ledger and trial receipt commit in the same fenced batch. */
public final class FactoryCapabilityStoreR31 {
    private FactoryCapabilityStoreR31() {}
    /** An optional capability experiment cannot roll back unrelated health or
     * feedback work. Only merge a completely prepared, bounded factory batch. */
    public static void advanceIsolated(SQLiteDatabase read,ResearchWriteQueueR27.Batch health,String source,long at,boolean allowed){
        ResearchWriteQueueR27.Batch factory=new ResearchWriteQueueR27.Batch();
        try{
            advance(read,factory,source,at,allowed);
            if(factory.before!=null)health.guardBefore(factory.before);
            if(factory.after!=null)health.guardAfter(factory.after);
            health.ops.addAll(factory.ops);
        }catch(RuntimeException failure){
            FailureDispositionR31.Disposition disposition=FailureDispositionR31.classify(failure).disposition;
            if(!(failure instanceof MemoryBudgetR31.CapacityWait)&&(disposition==FailureDispositionR31.Disposition.CANCELLED||disposition==FailureDispositionR31.Disposition.DEPENDENCY||disposition==FailureDispositionR31.Disposition.STALE_OWNER))throw failure;
            Map<String,Object> summary=new LinkedHashMap<>();
            try(Cursor c=read.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r30' AND key='q08-factory-r31' AND length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER)",new String[]{Integer.toString(FactoryStateCodecR31.MAX_CELL_BYTES)})){
                if(c.moveToFirst())try{Object prior=JsonR27.parse(c.getString(0));if(prior instanceof Map&&source.equals(((Map<?,?>)prior).get("sourceCommit")))for(Map.Entry<?,?> entry:((Map<?,?>)prior).entrySet())summary.put(String.valueOf(entry.getKey()),entry.getValue());}catch(IllegalArgumentException corruptSummary){summary.put("previousSummary","MALFORMED_ORIGINAL_PRESERVED");}
            }
            summary.put("sourceCommit",source);summary.put("protocolHash",FactoryCapabilityExperimentR31.protocolHash());summary.put("stateCodec",FactoryStateCodecR31.VERSION);
            summary.put("status",failure instanceof MemoryBudgetR31.CapacityWait?"INCOMPLETE_RESOURCE_WAIT":"FACTORY_TRANSPORT_FAILED");summary.put("schedulerState",summary.get("status"));summary.put("transportFailure",RuntimeFaultEvidenceR31.capture(failure,"FACTORY_TRANSPORT").causeChainJson);summary.put("observedAt",at);summary.put("realPermission",false);summary.put("naturalQuality","NOT_PROVEN_SYNTHETIC_ONLY");
            health.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30','q08-factory-r31',?,?)",JsonR27.write(summary),at);
        }
    }
    private static String read(SQLiteDatabase db,String key){
        // The length predicate is evaluated by SQLite before a large cell enters
        // Android's CursorWindow. Preserve an oversized legacy row and wait.
        try(Cursor c=db.rawQuery("SELECT CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END,length(CAST(payload_json AS BLOB)) FROM r27_chart_cache WHERE panel_id='r31-factory' AND key=?",new String[]{Integer.toString(FactoryStateCodecR31.MAX_CELL_BYTES),key})){
            if(!c.moveToFirst())return null;if(c.getLong(1)>FactoryStateCodecR31.MAX_CELL_BYTES)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);return c.getString(0);
        }
    }
    public static void advance(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String source,long at,boolean allowed){
        String key=source+":"+FactoryCapabilityExperimentR31.protocolHash(),old=read(read,key),oldHash=old==null?null:StatsR27.sha256(old);
        try(FactoryCapabilityExperimentR31 state=old==null?new FactoryCapabilityExperimentR31(source):FactoryCapabilityExperimentR31.restore(FactoryStateCodecR31.decode(old))){
            if(!source.equals(state.source()))throw new IllegalStateException("FACTORY_SOURCE_IDENTITY");
            if(allowed&&!state.complete())state.step();
            String checkpoint=FactoryStateCodecR31.encode(state.snapshot());
            Map<String,Object> summary=new LinkedHashMap<>(state.summary());summary.put("schedulerState",allowed?"COMMITTED_STEP_OR_COMPLETED":"WAIT_REALTIME_WINDOW");summary.put("stateKey",key);summary.put("stateCodec",FactoryStateCodecR31.VERSION);summary.put("maximumSqlCellBytes",FactoryStateCodecR31.MAX_CELL_BYTES);
            Map<String,Object> artifact=state.pendingArtifact();String artifactCell=artifact==null?null:FactoryStateCodecR31.encode(JsonR27.write(artifact));
            batch.guardBefore(db->{String current=read(db,key);return Objects.equals(oldHash,current==null?null:StatsR27.sha256(current))?0:-1;});
            batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r31-factory',?,?,?)",key,checkpoint,at);
            if(artifact!=null){String identity=key+":"+artifact.get("arm")+":"+artifact.get("seed");batch.add("INSERT OR IGNORE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r31-factory-trial',?,?,?)",identity,artifactCell,at);}
            batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r30','q08-factory-r31',?,?)",JsonR27.write(summary),at);
        }
    }
}
