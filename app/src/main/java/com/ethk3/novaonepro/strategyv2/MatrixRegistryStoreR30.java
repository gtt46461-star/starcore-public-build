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
            if(n!=null&&n>0)recorded++;rows.add(r);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("rows",rows);result.put("registeredObjects",(long)rows.size());result.put("r930NativeKinds",19L);result.put("su2FactorTeacherBundles",1L);result.put("verifiedBindings",linked);result.put("traceableRatio",linked/(double)rows.size());result.put("objectsWithRetainedSnapshots",recorded);
        result.put("status",linked==rows.size()?"TRACEABLE":"UNVERIFIED_BINDINGS");result.put("scope","EXISTING_MATRIX_OBJECTS_AND_RETAINED_WATERMARKS");return result;
    }
}
