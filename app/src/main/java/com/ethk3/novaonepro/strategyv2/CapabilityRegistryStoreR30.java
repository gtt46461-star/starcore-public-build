package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Read-only census. A class, receipt, checkpoint or snapshot is not an effectiveness certificate. */
public final class CapabilityRegistryStoreR30 {
    private CapabilityRegistryStoreR30(){}
    static Long positive(long value){return value>0?value:null;}
    static Long number(Object value){return value instanceof Number?((Number)value).longValue():null;}
    static final class Facts {Long outputs,last;String state,reason;final Map<String,Object> more=new LinkedHashMap<>();}
    @SuppressWarnings("unchecked")public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Facts> legacy=new HashMap<>(),families=new HashMap<>(),experts=new HashMap<>(),ideas=new HashMap<>();boolean capSource=false,familySource=false,expertSource=false,ideaSource=false;String legacyRule=null;
        try(Cursor c=db.rawQuery("SELECT mechanism,state,reason,last_progress FROM r26_capability",null)){capSource=true;while(c.moveToNext()){Facts f=new Facts();f.state=c.getString(1);f.reason=c.getString(2);f.last=positive(c.getLong(3));legacy.put(c.getString(0),f);}}catch(RuntimeException absent){}
        try(Cursor c=db.rawQuery("SELECT numerical_abi FROM r26_capability_source WHERE singleton=1",null)){if(c.moveToFirst())legacyRule=c.getString(0);}catch(RuntimeException absent){}
        try(Cursor c=db.rawQuery("SELECT family_id,count(*),max(updated_at) FROM r26_live_model GROUP BY family_id",null)){familySource=true;while(c.moveToNext()){Facts f=new Facts();f.outputs=c.getLong(1);f.last=positive(c.getLong(2));families.put(c.getString(0),f);}}catch(RuntimeException absent){}
        try(Cursor c=db.rawQuery("SELECT expert,count(*),sum(policy_hash IS NOT NULL),max(recorded_at) FROM r27_expert_proposal GROUP BY expert",null)){expertSource=true;while(c.moveToNext()){Facts f=new Facts();f.outputs=c.getLong(2);f.last=positive(c.getLong(3));f.more.put("retainedReceipts",c.getLong(1));f.more.put("auditReceipts",c.getLong(1)-c.getLong(2));experts.put(c.getString(0),f);}}catch(RuntimeException absent){}
        try(Cursor c=db.rawQuery("SELECT idea_family,count(*),max(updated_at) FROM r27_policy GROUP BY idea_family",null)){ideaSource=true;while(c.moveToNext()){Facts f=new Facts();f.outputs=c.getLong(1);f.last=positive(c.getLong(2));ideas.put(c.getString(0),f);}}catch(RuntimeException absent){}
        Facts net=new Facts();boolean netSource=false,checkpoint=false,checkpointAbi=false;String checkpointDataset=null;
        try(Cursor c=db.rawQuery("SELECT feature_abi,trained_through,updated_at,dataset_hash,state_hash,artifact FROM r29_model_checkpoint WHERE owner=?",new String[]{AtomNetSessionR30.OWNER})){netSource=true;if(c.moveToFirst()){checkpoint=true;checkpointAbi=AtomNetSessionR30.ABI.equals(c.getString(0));checkpointDataset=c.getString(3);
                try{AtomNetSessionR30 saved=AtomNetSessionR30.decode(c.getBlob(5),c.getString(4));checkpointAbi&=saved.datasetHash.equals(checkpointDataset)&&(saved.completeIssue.isEmpty()?saved.observedThrough:saved.trainedThrough()).equals(c.getString(1));}catch(RuntimeException corrupt){checkpoint=false;net.reason="CORRUPT_FULL_MODEL_CHECKPOINT";}
                net.last=positive(c.getLong(2));net.more.put("featureAbi",c.getString(0));net.more.put("featureAbiMatches",AtomNetSessionR30.ABI.equals(c.getString(0)));net.more.put("trainedThrough",c.getString(1));net.state="CHECKPOINT_RECORDED";}}catch(RuntimeException absent){}
        net.more.put("fullCheckpointVerified",checkpoint&&checkpointAbi);
        try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='atomnet' AND key='ten-position-r30'",null)){if(c.moveToFirst()){Map<String,Object> q=(Map<String,Object>)JsonR27.parse(c.getString(0));if(checkpoint&&checkpointAbi&&checkpointDataset.equals(q.get("datasetHash"))){net.outputs=number(q.get("onlineSamples"));net.more.put("materializedSamples",q.get("materializedSamples"));net.more.put("trainingPhase",q.get("phase"));}else if(net.reason==null){net.reason="CACHE_WITHOUT_MATCHING_FULL_MODEL_CHECKPOINT";}}}catch(RuntimeException absent){}
        Map<String,Map<String,Object>> matrices=new HashMap<>();for(Object value:(List<?>)MatrixRegistryStoreR30.report(db).get("rows")){Map<String,Object> r=(Map<String,Object>)value;matrices.put((String)r.get("id"),r);}
        List<Object> rows=new ArrayList<>();long implemented=0,executable=0,observed=0;Map<String,Long> groups=new LinkedHashMap<>();
        for(CapabilityRegistryR30.Definition d:CapabilityRegistryR30.all()){
            Map<String,Object> r=d.json();if(d.implemented)implemented++;if(d.executable)executable++;groups.put(d.kind,groups.getOrDefault(d.kind,0L)+1);
            Facts f=new Facts();boolean source=false;String sourceName,unit;
            switch(d.kind){
                case "FAMILY":{
                    Facts cap=legacy.get(d.id),model=families.get(d.id);source=capSource||familySource;sourceName="r26_capability + r26_live_model";unit="RETAINED_LIVE_MODELS";
                    if(model!=null){f.outputs=model.outputs;f.last=model.last;}else if(familySource)f.outputs=0L;
                    if(cap!=null){f.state=cap.state;f.reason=cap.reason;if(cap.last!=null&&(f.last==null||cap.last>f.last))f.last=cap.last;}
                    f.more.put("legacyNumericalAbi",legacyRule);f.more.put("legacyRuleMatchesCurrent",legacyRule==null?null:legacyRule.equals(com.ethk3.novaonepro.ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256));break;
                }
                case "EXPERT":{
                    Facts e=experts.get(d.id),cap=legacy.get(d.id);source=expertSource||capSource;sourceName="r27_expert_proposal + r26_capability";unit="RETAINED_POLICY_PROPOSALS";
                    if(e!=null)f=e;else if(expertSource){f.outputs=0L;f.more.put("retainedReceipts",0L);f.more.put("auditReceipts",0L);}if(cap!=null){f.state=cap.state;f.reason=cap.reason;if(cap.last!=null&&(f.last==null||cap.last>f.last))f.last=cap.last;}break;
                }
                case "IDEA":{Facts e=ideas.get(d.id);source=ideaSource;sourceName="r27_policy.idea_family";unit="RETAINED_POLICY_OBJECTS";if(e!=null)f=e;else if(source)f.outputs=0L;break;}
                case "MODEL":{f=net;source=netSource;sourceName="r29_model_checkpoint + atomnet/ten-position-r30";unit="COMMITTED_ONLINE_POSITION_SAMPLES";break;}
                default:{Map<String,Object> m=matrices.get(d.matrixId);sourceName="MatrixRegistryR30 original snapshot sources";unit="RETAINED_MATRIX_SNAPSHOTS";
                    if(m!=null){f.outputs=number(m.get("retainedSnapshotCount"));f.last=number(m.get("lastProducedAt"));f.reason=(String)m.get("notProducedReason");f.state=(String)m.get("evidenceState");f.more.put("lastProducedIssue",m.get("lastProducedIssue"));f.more.put("lastProducedCheckpoint",m.get("lastProducedCheckpoint"));source=f.outputs!=null;}break;}
            }
            boolean runtime=f.last!=null||f.outputs!=null&&f.outputs>0||number(f.more.get("retainedReceipts"))!=null&&number(f.more.get("retainedReceipts"))>0;
            if(runtime)observed++;r.put("lastRecordedRunAt",f.last);r.put("retainedOutputCount",f.outputs);r.put("outputUnit",unit);r.put("runtimeObserved",runtime);r.put("runtimeState",f.state);r.put("runtimeReason",f.reason);r.putAll(f.more);
            r.put("evidenceStatus",runtime?"RUNTIME_RECORDS_UNVALIDATED":source?"NOT_RUN":"BLOCKED_SOURCE");r.put("evidenceReason",runtime?null:source?"NO_RETAINED_RUNTIME_RECEIPT":"RUNTIME_SOURCE_UNAVAILABLE");r.put("measurementSource",sourceName);r.put("effective",null);rows.add(r);
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("rows",rows);out.put("registeredObjects",(long)rows.size());out.put("byKind",groups);out.put("implementedObjects",implemented);out.put("executableObjects",executable);out.put("objectsWithRuntimeRecords",observed);out.put("effectiveObjects",null);out.put("effectivenessStatus","REQUIRES_AUTHORITATIVE_PROSPECTIVE_QUALIFICATION");out.put("status",rows.size()==82?"COMPLETE_INVENTORY":"INCOMPLETE_INVENTORY");return out;
    }
}
