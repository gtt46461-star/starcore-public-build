package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Shared state vocabulary for diagnostics, pages, and charts. Counts are scoped to a stated read. */
public final class ReportSourceStateR341 {
    public static String reason(String original) {
        String code=original==null?"":original.toUpperCase(Locale.ROOT);
        if(code.contains("NO SUCH TABLE")||code.contains("NO SUCH COLUMN")||code.contains("MISSING_SCHEMA")
                ||code.contains("WAITING_SOURCE")||code.contains("SOURCE_MISSING"))return "WAITING_SOURCE";
        if(code.contains("CANCEL"))return "CANCELLED";
        if(code.contains("TIMED_OUT")||code.contains("TIMEOUT"))return "TIMED_OUT";
        if(code.contains("PERMISSION")||code.contains("NOT AUTHORIZED"))return "PERMISSION_DENIED";
        if(code.contains("STALE"))return "STALE";
        if(code.contains("TRUNC")||code.contains("PARTIAL")||code.contains("BYTE_BUDGET"))return "PARTIAL";
        if(code.contains("ERROR")||code.contains("FAILED")||code.contains("UNAVAILABLE")
                ||code.contains("UNKNOWN"))return "SOURCE_ERROR";
        if(code.contains("EMPTY"))return "OBSERVED_EMPTY";
        if(code.contains("WAIT")||code.contains("INSUFFICIENT"))return "WAITING_RUN";
        return "";
    }

    public static Map<String,Object> observation(String id,String scope,String state,String detail,Integer rows,Long total) {
        String code=reason(state+" "+detail);
        boolean empty="OBSERVED_EMPTY".equals(code);
        boolean good=code.isEmpty()||empty;
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("sourceId",id);result.put("scope",scope);
        result.put("state",good?(empty?"SOURCE_EMPTY":"SOURCE_OK"):"SOURCE_UNAVAILABLE");
        result.put("sourceQueryStatus",state);result.put("reasonCode",code);
        result.put("reason",detail);
        result.put("errorRef",good?null:V51ChecksumEngine.sha256(id+"|"+scope+"|"+state+"|"+detail));
        result.put("rowsReturned",rows);result.put("totalRowsKnown",total!=null);result.put("totalRows",total);
        return result;
    }

    public static Map<String,Object> merge(Collection<? extends Map<?,?>> observed) {
        int ok=0,empty=0,unavailable=0;
        List<Object> sources=new ArrayList<>();
        for(Map<?,?> item:observed){
            switch(String.valueOf(item.get("state"))){
                case "SOURCE_OK":ok++;break;
                case "SOURCE_EMPTY":empty++;break;
                default:unavailable++;break;
            }
            Map<String,Object> projection=new LinkedHashMap<>();
            for(String key:new String[]{"sourceId","scope","scopeId","state","sourceQueryStatus","reasonCode","reason","errorRef","rowsReturned","totalRowsKnown","totalRows"})
                if(item.containsKey(key))projection.put(key,item.get(key));
            sources.add(projection);
        }
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("scope","PROJECTED_SOURCE_READS_R341");result.put("health",unavailable>0?"UNKNOWN_SOURCE_UNAVAILABLE":sources.isEmpty()?"UNKNOWN_NOT_SAMPLED":"READABLE");
        result.put("sourceCount",sources.size());result.put("ok",ok);result.put("empty",empty);result.put("unavailable",unavailable);result.put("sources",sources);
        return result;
    }

    private ReportSourceStateR341() {}
}
