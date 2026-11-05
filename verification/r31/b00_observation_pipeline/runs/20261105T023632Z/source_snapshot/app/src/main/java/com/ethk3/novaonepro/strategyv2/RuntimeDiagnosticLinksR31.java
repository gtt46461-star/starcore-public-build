package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only links to bounded, persisted fault samples. Absence never proves a healthy task. */
public final class RuntimeDiagnosticLinksR31 {
    private RuntimeDiagnosticLinksR31() {}
    public static Map<String,Object> summarize(Object tasks, Object faults) {
        Map<String,Object> result=new LinkedHashMap<>();
        List<Object> rows=new ArrayList<>();
        result.put("scope","DISPLAYED_FAULT_SAMPLES_ONLY_NOT_COMPLETE_ERROR_HISTORY_OR_HEALTH");
        result.put("faultSourceAvailable",faults instanceof List);
        result.put("taskSourceAvailable",tasks instanceof List);
        if(tasks instanceof List) for(Object item:(List<?>)tasks) {
            if(!(item instanceof Map) || rows.size()>=100)continue;
            Map<?,?> task=(Map<?,?>)item;
            Object id=task.get("task_id"),owner=task.get("owner"),fence=task.get("fence");
            Map<String,Object> row=new LinkedHashMap<>();
            row.put("taskId",id);row.put("rawSchedulerState",task.get("status"));
            row.put("ownerEpoch",owner);row.put("fence",fence);
            List<Object> refs=new ArrayList<>();boolean matched=false;
            if(faults instanceof List) {
                List<?> samples=(List<?>)faults;
                for(int i=0;i<Math.min(20,samples.size());i++) {
                    Object sample=samples.get(i);if(!(sample instanceof Map))continue;
                    Map<?,?> fault=(Map<?,?>)sample;
                    if(!(id instanceof String)||!id.equals(fault.get("task_id")))continue;
                    boolean known=owner instanceof String&&fence instanceof Number
                            &&fault.get("owner_epoch") instanceof String&&fault.get("fence") instanceof Number;
                    boolean current=known&&owner.equals(fault.get("owner_epoch"))
                            &&((Number)fence).longValue()==((Number)fault.get("fence")).longValue();
                    Map<String,Object> ref=new LinkedHashMap<>();
                    for(String key:new String[]{"event_id","owner_epoch","fence","operation","phase","category","commit_outcome","root_fingerprint"})ref.put(key,fault.get(key));
                    ref.put("currentOwnerAndFenceMatch",known?current:null);
                    refs.add(ref);matched|=current;
                }
            }
            row.put("faultReferences",refs);
            row.put("currentClaimEvidence",matched?"MATCHED_DISPLAYED_SAMPLES":"UNKNOWN_OR_OUTSIDE_SAMPLE");
            rows.add(row);
        }
        result.put("tasks",rows);return result;
    }
}
