package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Read-only inventory over existing tables. Stored observations are not statistical validation. */
public final class MatrixRegistryStoreR30 {
    private MatrixRegistryStoreR30(){}
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Map<String,Object>> nativeRows=new HashMap<>();String nativeUnavailable=null,su2Unavailable=null;Map<String,Object> su2=null;
        try(Cursor c=db.rawQuery("SELECT kind,count(*),max(target_issue),max(frozen_at) FROM research_matrix_snapshot_r930 GROUP BY kind",null)){
            while(c.moveToNext()){Map<String,Object> m=new LinkedHashMap<>();m.put("retainedSnapshotCount",c.getLong(1));m.put("lastProducedIssue",c.isNull(2)?null:c.getString(2));m.put("lastProducedAt",c.isNull(3)?null:c.getLong(3));nativeRows.put(c.getString(0),m);}
        }catch(RuntimeException missing){nativeUnavailable="R930_SNAPSHOT_SOURCE_UNAVAILABLE:"+missing.getClass().getSimpleName();}
        try(Cursor c=db.rawQuery("SELECT count(*),max(checkpoint_sequence),max(updated_at) FROM su2_matrix_snapshot",null)){
            if(c.moveToFirst()){su2=new LinkedHashMap<>();su2.put("retainedSnapshotCount",c.getLong(0));su2.put("lastProducedCheckpoint",c.isNull(1)?null:c.getLong(1));su2.put("lastProducedAt",c.isNull(2)?null:c.getLong(2));su2.put("periodKind","SU2_CHECKPOINT_SEQUENCE");}
        }catch(RuntimeException missing){su2Unavailable="SU2_SNAPSHOT_SOURCE_UNAVAILABLE:"+missing.getClass().getSimpleName();}
        Map<String,Map<String,Object>> receipts=new HashMap<>();
        try(Cursor c=db.rawQuery("SELECT key,payload_json FROM r27_chart_cache WHERE panel_id=?",new String[]{MatrixResearchR30R1.PANEL})){while(c.moveToNext()){@SuppressWarnings("unchecked") Map<String,Object> q=(Map<String,Object>)JsonR27.parse(c.getString(1));receipts.put(c.getString(0),q);}}catch(RuntimeException unavailable){}
        Map<String,Long> typedCounts=new HashMap<>();try(Cursor c=db.rawQuery("SELECT kind,count(*) FROM research_matrix_snapshot_r930 WHERE producer_version=? AND payload_json IS NOT NULL GROUP BY kind",new String[]{MatrixTensorR30R1.VERSION})){while(c.moveToNext())typedCounts.put(c.getString(0),c.getLong(1));}catch(RuntimeException unavailable){}
        Map<String,Map<String,Object>> consumers=new HashMap<>();boolean consumerSource=false;
        try(Cursor c=db.rawQuery("SELECT m.kind,count(DISTINCT m.matrix_hash),count(DISTINCT c.proposal_id),max(c.consumed_at) FROM research_matrix_snapshot_r930 m JOIN r30r1_proposal p ON p.run_id=m.analysis_run_id JOIN r30r1_consume c ON c.proposal_id=p.proposal_id GROUP BY m.kind",null)){consumerSource=true;while(c.moveToNext()){Map<String,Object> q=new LinkedHashMap<>();q.put("consumedSnapshotCount",c.getLong(1));q.put("exactChildReceipts",c.getLong(2));q.put("lastConsumedAt",c.getLong(3));consumers.put(c.getString(0),q);}}catch(RuntimeException missing){}
        try(Cursor c=db.rawQuery("SELECT sum(consumed_actions),max(last_consumed_at),max(input_through_ordinal),max(available_at) FROM su2_matrix_snapshot",null)){if(c.moveToFirst()&&su2!=null){su2.put("consumedActions",c.isNull(0)?null:c.getLong(0));su2.put("lastConsumedAt",c.isNull(1)?null:c.getLong(1));su2.put("inputThroughOrdinal",c.isNull(2)?null:c.getLong(2));su2.put("availableAt",c.isNull(3)?null:c.getLong(3));}}catch(RuntimeException oldSchema){}
        List<Object> rows=new ArrayList<>();long linked=0,recorded=0;
        for(MatrixRegistryR30.Definition d:MatrixRegistryR30.all()){
            Map<String,Object> r=d.json();boolean links=MatrixRegistryR30.declared(d.producerClass,d.producerMethod);for(String m:d.consumerMethods)links&=MatrixRegistryR30.declared(d.consumerClass,m);
            if(links)linked++;r.put("bindingsVerified",links);r.put("callableImplementation",links);
            Map<String,Object> observation=d.namespace.equals("SU2")?su2:nativeRows.get(d.kind);String unavailable=d.namespace.equals("SU2")?su2Unavailable:nativeUnavailable;
            if(observation!=null)r.putAll(observation);
            Long n;if(observation!=null)n=((Number)observation.get("retainedSnapshotCount")).longValue();else if(unavailable==null)n=0L;else n=null;
            r.put("retainedSnapshotCount",n);r.putIfAbsent("lastProducedIssue",null);r.putIfAbsent("lastProducedAt",null);
            r.put("evidenceState",n!=null&&n>0?"RECORDED_UNVALIDATED":unavailable!=null?"BLOCKED_SOURCE":"NOT_RUN");
            r.put("notProducedReason",unavailable!=null?unavailable:n!=null&&n>0?null:"NO_RETAINED_PRODUCTION_SNAPSHOT");
            r.put("bindingReason",links?null:"DECLARED_PRODUCER_OR_CONSUMER_METHOD_UNAVAILABLE");r.put("qualityEffective",null);
            Map<String,Object> receipt=receipts.get(d.kind);r.put("productionReceipt",receipt);r.put("typedSnapshotCount",typedCounts.getOrDefault(d.kind,0L));
            r.put("staticBindingStatus",links?"TRACEABLE":"UNVERIFIED_BINDINGS");r.put("runtimeSnapshotStatus",receipt==null?r.get("evidenceState"):receipt.get("status"));r.put("qualityStatus","UNKNOWN_NOT_VALIDATED");
            r.put("inputAvailable",receipt==null?null:!"BLOCKED_DEPENDENCY".equals(receipt.get("status")));r.put("produced",n==null?null:n>0);Map<String,Object> used=consumers.get(d.kind);if(used!=null)r.putAll(used);Object actionUse=d.namespace.equals("SU2")&&su2!=null?su2.get("consumedActions"):null;Boolean consumed=null;if(used!=null)consumed=Boolean.TRUE;else if(actionUse instanceof Number)consumed=Boolean.valueOf(((Number)actionUse).longValue()>0);else if(d.namespace.equals("R930")&&consumerSource)consumed=Boolean.FALSE;r.put("consumed",consumed);r.put("consumptionReason",Boolean.TRUE.equals(consumed)?"ACTUAL_SOURCE_LINKED_ALGORITHM_RECEIPT":consumed==null?"EXACT_CONSUMPTION_SOURCE_UNAVAILABLE":"NO_EXACT_CHILD_CONSUMPTION_RECEIPT");r.put("effective",null);
            if(receipt!=null&&receipt.get("reason")!=null)r.put("notProducedReason",receipt.get("reason"));
            if(n!=null&&n>0)recorded++;rows.add(r);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("rows",rows);result.put("registeredObjects",(long)rows.size());result.put("r930NativeKinds",19L);result.put("su2FactorTeacherBundles",1L);result.put("verifiedBindings",linked);result.put("traceableRatio",linked/(double)rows.size());result.put("objectsWithRetainedSnapshots",recorded);
        result.put("staticBindingStatus",linked==rows.size()?"TRACEABLE":"UNVERIFIED_BINDINGS");
        result.put("runtimeSnapshotStatus",nativeUnavailable!=null||su2Unavailable!=null?"BLOCKED_SOURCE":recorded==0?"NO_RETAINED_PRODUCTION":"RECORDED_UNVALIDATED");
        result.put("qualityStatus","UNKNOWN_NOT_VALIDATED");
        result.put("status",nativeUnavailable!=null||su2Unavailable!=null?"BLOCKED_SOURCE":recorded==rows.size()?"RECORDED_UNVALIDATED":"INCOMPLETE_PRODUCTION");
        result.put("scope","EXISTING_MATRIX_OBJECTS_AND_RETAINED_WATERMARKS");return result;
    }
}
