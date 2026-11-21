package com.ethk3.novaonepro.strategyv2;

import android.os.CancellationSignal;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads the detached projections of ReportSnapshotR30's one SELECT. No requery,
 * database mutation, replay, or causal verdict occurs here. */
public final class DiagnosisTraceReaderR33 {
    private DiagnosisTraceReaderR33() {}
    static final int MAX_BYTES=196608, MAX_TASKS=100, MAX_FAULTS=20;
    static Map<?,?> map(Object v){return v instanceof Map?(Map<?,?>)v:Collections.emptyMap();}
    static List<?> list(Object v){return v instanceof List?(List<?>)v:Collections.emptyList();}
    static String text(Object v){return v instanceof String?(String)v:"";}
    static boolean hash(Object v){return v instanceof String&&((String)v).matches("[0-9a-f]{64}");}
    static void check(CancellationSignal c){if(c!=null)c.throwIfCanceled();}
    static boolean integer(Object v){return v instanceof Long||v instanceof Integer;}

    /** Only called by the SELECT owner after schema/watermark validation and before
     * any hot/cold or process-local observations replace its source rows. */
    static Map<String,Object> captureSingleSelect(Map<String,Object> watermark,Map<String,Object> data,CancellationSignal cancel){
        check(cancel);Map<String,Object> out=new LinkedHashMap<>();
        out.put("schema","R335_CAPTURED_FAULT_SOURCES_1");out.put("sourceCommit",watermark.get("sourceCommit"));
        out.put("readWatermark",watermark.get("factsReadAt"));
        out.put("scope","SAME_SINGLE_SELECT_BOUNDED_ROW_PROJECTIONS; NOT_FULL_INPUT_ARTIFACTS_OR_CAUSAL_PROOF");
        List<Object> originals=new ArrayList<>();Map<String,Object> coverage=new LinkedHashMap<>();int used=0;
        for(String key:new String[]{"tasks","r31RuntimeFaults"}){
            check(cancel);String table=key.equals("tasks")?"r26_task":"r31_runtime_fault",pk=key.equals("tasks")?"task_id":"event_id";
            int limit=key.equals("tasks")?MAX_TASKS:MAX_FAULTS,accepted=0,omitted=0;Set<String> seen=new HashSet<>();
            Object source=data.get(key);List<?> rows=list(source);
            for(int i=0;i<Math.min(rows.size(),limit);i++){
                check(cancel);Map<?,?> row=map(rows.get(i));String identity=text(row.get(pk));
                if(!table.equals(row.get("sourceTable"))||identity.isEmpty()||identity.length()>256||!seen.add(identity)){omitted++;continue;}
                String raw=JsonR27.write(row);int bytes=raw.getBytes(StandardCharsets.UTF_8).length;
                if(bytes>32768||bytes>MAX_BYTES-used){omitted++;continue;}used+=bytes;
                Map<String,Object> original=new LinkedHashMap<>();original.put("rowKey",table+":"+identity);
                original.put("sourceTable",table);original.put("primaryKey",Collections.singletonMap(pk,identity));
                original.put("sha256",StatsR27.sha256(raw));original.put("projectedRow",JsonR27.parse(raw));
                originals.add(original);accepted++;
            }
            Map<String,Object> state=new LinkedHashMap<>();state.put("sourceAvailable",source instanceof List);
            state.put("sampleRows",rows.size());state.put("retainedRows",accepted);state.put("rejectedOrOverBudget",omitted+Math.max(0,rows.size()-limit));
            for(Object index:list(data.get("rowsetIndexes")))if(key.equals(map(index).get("key"))){state.put("rowsetIndex",index);break;}
            state.put("absenceMeaning","NOT_OBSERVED_IN_BOUNDED_SAMPLE; NEVER_NO_FAULT_OR_NO_CAPABILITY");coverage.put(key,state);
        }
        out.put("originals",originals);out.put("coverage",coverage);out.put("projectedBytes",used);
        out.put("sourcePacketSha256",StatsR27.sha256(JsonR27.write(out)));return out;
    }

    static boolean validPacket(Map<?,?> packet,Object commit,Object at){
        if(!text(commit).matches("[0-9a-f]{40}")||!integer(at)||((Number)at).longValue()<=0)return false;
        if(!"R335_CAPTURED_FAULT_SOURCES_1".equals(packet.get("schema"))||!Objects.equals(commit,packet.get("sourceCommit"))||!Objects.equals(at,packet.get("readWatermark")))return false;
        Map<String,Object> body=new LinkedHashMap<>();for(Map.Entry<?,?> e:packet.entrySet())if(e.getKey() instanceof String&&!"sourcePacketSha256".equals(e.getKey()))body.put((String)e.getKey(),e.getValue());
        if(!Objects.equals(packet.get("sourcePacketSha256"),StatsR27.sha256(JsonR27.write(body))))return false;
        List<?> rows=list(packet.get("originals"));if(rows.size()>MAX_TASKS+MAX_FAULTS)return false;
        for(Object item:rows){Map<?,?> row=map(item);if(!hash(row.get("sha256"))||!row.get("sha256").equals(StatsR27.sha256(JsonR27.write(row.get("projectedRow")))))return false;}
        return true;
    }

    static String[] owners(String group,String id){
        if(group.equals("capabilities")){
            if(id.matches("F(?:0[1-9]|1[0-9]|2[0-4])"))return new String[]{"foundry-su2"};
            if(id.matches("E(?:0[1-9]|1[0-6])"))return new String[]{"expert-"+id};
            if(id.matches("I(?:0[0-9]|1[0-9]|20)"))return new String[]{"r27-evolution"};
            if(id.equals("ATOMNET"))return new String[]{"r27-atomnet","r27-atomnet-archive"};
            if(id.startsWith("MATRIX:"))return new String[]{"r27-matrices-r1"};
        }
        if(group.equals("researchCharts"))return new String[]{"r27-observatory-r1"};
        if(group.equals("matrices"))return new String[]{"r27-matrices-r1"};
        return new String[0];
    }
    static boolean sameFence(Map<?,?> task,Map<?,?> fault){
        return !text(task.get("owner")).isEmpty()&&task.get("owner").equals(fault.get("owner_epoch"))
            &&integer(task.get("fence"))&&integer(fault.get("fence"))&&((Number)task.get("fence")).longValue()==((Number)fault.get("fence")).longValue();
    }
    /** A shared scheduler task is labelled as a shared owner, never a family root cause. */
    static Map<String,Object> forObject(Map<?,?> packet,String group,String id,Object commit,Object at){
        if(!validPacket(packet,commit,at))return Collections.emptyMap();
        return forVerifiedObject(packet,group,id,commit,at);
    }
    private static Map<String,Object> forVerifiedObject(Map<?,?> packet,String group,String id,Object commit,Object at){
        Set<String> wanted=new LinkedHashSet<>(Arrays.asList(owners(group,id)));if(wanted.isEmpty())return Collections.emptyMap();
        Map<String,Map<?,?>> tasks=new LinkedHashMap<>();Map<String,Map<?,?>> refs=new LinkedHashMap<>();
        for(Object item:list(packet.get("originals"))){Map<?,?> ref=map(item),row=map(ref.get("projectedRow"));
            if("r26_task".equals(ref.get("sourceTable"))&&wanted.contains(text(row.get("task_id")))){tasks.put(text(row.get("task_id")),row);refs.put(text(row.get("task_id")),ref);}}
        List<Object> matches=new ArrayList<>();
        for(String owner:wanted){Map<?,?> task=tasks.get(owner);if(task==null)continue;
            List<Object> faultRefs=new ArrayList<>();
            for(Object item:list(packet.get("originals"))){Map<?,?> ref=map(item),fault=map(ref.get("projectedRow"));
                if(!"r31_runtime_fault".equals(ref.get("sourceTable"))||!owner.equals(fault.get("task_id")))continue;
                Map<String,Object> f=new LinkedHashMap<>();for(String k:new String[]{"event_id","operation","phase","category","commit_outcome","root_fingerprint","owner_epoch","fence"})f.put(k,fault.get(k));
                f.put("sourceRowKey",ref.get("rowKey"));f.put("sourceRowSha256",ref.get("sha256"));f.put("currentOwnerAndFenceMatch",sameFence(task,fault));faultRefs.add(f);
            }
            Object reason=task.get("blocked_reason");boolean blocked=reason instanceof String&&!((String)reason).isEmpty();
            if(!blocked&&faultRefs.isEmpty())continue;
            Map<String,Object> m=new LinkedHashMap<>();m.put("taskId",owner);m.put("rawTaskStatus",task.get("status"));m.put("reportedBlocker",reason);
            m.put("blockerMeaning","PERSISTED_SCHEDULER_REASON; MAY_BE_NORMAL_WAIT_OR_STALE; NOT_AUTOMATIC_FAULT");
            m.put("sourceRowKey",refs.get(owner).get("rowKey"));m.put("sourceRowSha256",refs.get(owner).get("sha256"));m.put("faultReferences",faultRefs);matches.add(m);
        }
        if(matches.isEmpty())return Collections.emptyMap();
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema","R335_OWNER_TRACE_1");out.put("traceKind","PERSISTED_OWNER_OBSERVATION");
        out.put("group",group);out.put("objectId",id);out.put("sourceCommit",commit);out.put("readWatermark",at);
        out.put("sourcePacketSha256",packet.get("sourcePacketSha256"));out.put("sourcePacketPath","R30_REPORT.json#/objectIndex/faultTraceSourcesR335");
        out.put("ownerObservations",matches);out.put("sourceVerification","SINGLE_SELECT_PROJECTION_READ");
        out.put("attribution",id.matches("E(?:0[1-9]|1[0-6])")?"EXACT_EXPERT_TASK_ID":"SHARED_OWNER_TASK_NOT_OBJECT_ROOT_CAUSE");
        out.put("inputArtifactBytesVerified",false);out.put("numericReplayExecuted",false);out.put("mayAuthorizeReal",false);
        out.put("nextAction","Use the original row key, owner/fence and operation to acquire that invocation's original input; reproduce independently before any root-cause or repair-success claim.");
        out.put("traceEnvelopeSha256",StatsR27.sha256(JsonR27.write(out)));return out;
    }
    /** Verifies the shared source packet once; no O(objects * source-bytes) rehash. */
    static void attach(Map<String,Object> index,Object packetValue){
        Map<?,?> packet=map(packetValue);Object commit=index.get("sourceCommit"),at=index.get("sourceSnapshot");
        if(!validPacket(packet,commit,at)){index.put("faultTraceCaptureStatusR335","EVIDENCE_MISSING_OR_ENVELOPE_MISMATCH");return;}
        int attached=0;
        for(String group:new String[]{"capabilities","matrices","researchCharts","legacyCharts","logicalTables"})
            for(Object item:list(map(index.get(group)).get("objects"))){
                if(!(item instanceof Map))continue;
                @SuppressWarnings("unchecked") Map<String,Object> row=(Map<String,Object>)item;
                String id=text(row.get("objectId"));if(id.isEmpty())id=text(row.get("id"));
                Map<String,Object> trace=forVerifiedObject(packet,group,id,commit,at);
                if(!trace.isEmpty()){row.put("verifiedFaultTraceR33",trace);attached++;}
            }
        index.put("faultTraceSourcesR335",packet);index.put("faultTraceAttachedObjectsR335",attached);
        index.put("faultTraceCaptureStatusR335","CAPTURED_BOUNDED_OWNER_OBSERVATIONS_NOT_CAUSAL_PROOF");
    }
    static boolean validTrace(Map<?,?> trace){
        if(!"R335_OWNER_TRACE_1".equals(trace.get("schema"))||!hash(trace.get("sourcePacketSha256"))||list(trace.get("ownerObservations")).isEmpty())return false;
        Map<String,Object> body=new LinkedHashMap<>();for(Map.Entry<?,?> e:trace.entrySet())if(e.getKey() instanceof String&&!"traceEnvelopeSha256".equals(e.getKey()))body.put((String)e.getKey(),e.getValue());
        return Objects.equals(trace.get("traceEnvelopeSha256"),StatsR27.sha256(JsonR27.write(body)));
    }
}
