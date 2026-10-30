package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;

/** The remaining legacy realtime owner sends lineage/error outputs through the durable inbox. It cannot keep
 * mutating a research table after cutover. Legacy VM/weights/cursors remain untouched by this adapter. */
public final class LegacyResearchTransferR29 {
    private LegacyResearchTransferR29(){}
    public static final String LINEAGE="LEGACY_LINEAGE",ERROR="LEGACY_TASK_ERROR";
    static boolean supports(String kind){return LINEAGE.equals(kind)||ERROR.equals(kind);}
    public static void lineage(SQLiteDatabase owner,String child,String parent,long now){
        Map<String,Object> p=new LinkedHashMap<>();p.put("abi","R29_LEGACY_LINEAGE_V1");p.put("child",child);p.put("parent",parent);p.put("role","ABI_REBIRTH");p.put("operator","ABI_REBIRTH");p.put("generation",-1);p.put("origin","R26_LEGACY");
        send(owner,LINEAGE,p,now);
    }
    public static void taskError(SQLiteDatabase owner,String execution,String task,String errorClass,String message,String stack,long now){
        Map<String,Object> p=new LinkedHashMap<>();p.put("abi","R29_LEGACY_ERROR_V1");p.put("execution",execution);p.put("task",task);p.put("errorClass",errorClass);p.put("message",message);p.put("stack",stack);send(owner,ERROR,p,now);
    }
    private static void send(SQLiteDatabase owner,String kind,Map<String,Object> p,long now){
        if(!owner.inTransaction())throw new IllegalStateException("R29_LEGACY_TRANSFER_TRANSACTION_REQUIRED");
        String payload=JsonR27.write(p),hash=StatsR27.sha256(payload);
        ResearchEventBridgeR29.Event event=new ResearchEventBridgeR29.Event(0,StatsR27.sha256(kind+"||"+hash),"",payload,hash,"LEGACY_COMPATIBILITY_EVENT",now,kind);
        if(ResearchRouteR29.realtimeForWriter(owner)!=owner){consume(owner,event);return;}
        ResearchEventBridgeR29.ensureRealtime(owner);ResearchEventBridgeR29.append(owner,event);
    }
    static void validate(String kind,Map<String,Object> p){
        if(LINEAGE.equals(kind)){
            if(!"R29_LEGACY_LINEAGE_V1".equals(p.get("abi")))throw new IllegalArgumentException("R29_LEGACY_LINEAGE_ABI");
            for(String key:new String[]{"child","parent","role","operator","origin"})text(p,key);
            ExperimentRegistrationR29.integer(p,"generation");
        }else if(ERROR.equals(kind)){
            if(!"R29_LEGACY_ERROR_V1".equals(p.get("abi")))throw new IllegalArgumentException("R29_LEGACY_ERROR_ABI");
            for(String key:new String[]{"execution","task","errorClass","message","stack"})text(p,key);
        }else throw new IllegalArgumentException("R29_LEGACY_EVENT_KIND");
    }
    static String text(Map<String,Object> p,String key){Object value=p.get(key);if(!(value instanceof String)||((String)value).length()>65536)throw new IllegalArgumentException("R29_LEGACY_FIELD:"+key);return (String)value;}
    @SuppressWarnings("unchecked")
    static void consume(SQLiteDatabase research,ResearchEventBridgeR29.Event event){
        Map<String,Object> p=(Map<String,Object>)JsonR27.parse(event.payload);validate(event.kind,p);
        if(LINEAGE.equals(event.kind))research.execSQL("INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin) VALUES(?,?,?,?,?,?,?)",new Object[]{text(p,"child"),text(p,"parent"),text(p,"role"),text(p,"operator"),ExperimentRegistrationR29.integer(p,"generation"),event.committedAt,text(p,"origin")});
        else research.execSQL("INSERT INTO r27_task_error(task_id,error_class,message,stack,recorded_at) VALUES(?,?,?,?,?)",new Object[]{text(p,"task"),text(p,"errorClass"),text(p,"message"),text(p,"stack"),event.committedAt});
    }
}
