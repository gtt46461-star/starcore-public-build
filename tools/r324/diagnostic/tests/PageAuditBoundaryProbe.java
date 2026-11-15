package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class PageAuditBoundaryProbe {
 static int checks=0,failures=0;
 static Map<String,Object> row(Object...kv){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)r.put((String)kv[i],kv[i+1]);return r;}
 static Map<String,Object> page(Object rows){return row("status","READ_OK","rows",rows,"offset",0,"hasMore",false);}
 static Map<?,?> audit(String id,Object...rows){return ResearchTableAuditR32.audit(id,page(Arrays.asList(rows)));}
 static boolean code(Map<?,?> r,String c){return ((Map<?,?>)r.get("observationCounts")).containsKey(c);}
 static void check(boolean v,String s){checks++;if(!v){failures++;System.out.println("FAIL "+s);}}
 public static void main(String[] args){
  Map<?,?> r=audit("T03",row("frozen_at",10000,"deadline_at",20000,"settled_at",0,"hit",-1));
  check(code(r,"FROZEN_NOT_SETTLED"),"schema default settled_at zero is unobserved");
  check(!"PAGE_CHECKS_CLEAR_EFFECT_NOT_PROVEN".equals(r.get("status")),"default unclosed future row is not clear");
  r=audit("T03",row("frozen_at",10000,"deadline_at",20000,"settled_at",30000,"hit",-1));
  check(code(r,"SETTLED_HIT_INVALID"),"settled hit cannot be minus one");
  r=audit("T03",row("frozen_at",10000,"deadline_at",20000,"settled_at",9000,"hit",1));
  check(code(r,"TEMPORAL_ORDER_VIOLATION:frozen_at:settled_at"),"settlement cannot precede freeze");
  r=audit("T03",row("frozen_at",10000,"deadline_at",20000,"settled_at",30000,"hit",0));
  check("PAGE_CHECKS_CLEAR_EFFECT_NOT_PROVEN".equals(r.get("status")),"valid loss observation remains valid without inventing gain");
  r=audit("T13",row("verified",1.9));check(code(r,"CONFIRMATION_NOT_VERIFIED"),"fractional flag not accepted as true");
  r=audit("T07",(Object)null);check(code(r,"ROW_SHAPE_INVALID"),"null row cannot become empty valid map");
  r=audit("T07","invalid-row");check(code(r,"ROW_SHAPE_INVALID"),"scalar row rejected");
  r=ResearchTableAuditR32.audit("T07",page(null));check(code(r,"PAGE_ROWS_UNAVAILABLE"),"missing rows observable");
  r=ResearchTableAuditR32.audit("T07",page(Collections.emptyList()));check(code(r,"READ_OK_WITHOUT_ROWS"),"zero checks not reported clear");
  List<Object> rows=new ArrayList<>();for(int i=0;i<201;i++)rows.add(row("kept_policy","a","dropped_policy","b"));rows.set(200,row("kept_policy","a","dropped_policy","a"));
  r=ResearchTableAuditR32.audit("T07",page(rows));check(code(r,"PAGE_AUDIT_ROW_BUDGET"),"uninspected tail explicit");
  check(r.get("rowsNotInspected") instanceof Number&&((Number)r.get("rowsNotInspected")).intValue()==1,"one omitted row counted");
  Map<String,Object> p=page(Collections.singletonList(row("kept_policy","a","dropped_policy","b")));p.put("shown",2);
  r=ResearchTableAuditR32.audit("T07",p);check(code(r,"PAGE_SHOWN_ROWS_MISMATCH"),"claimed row count checked");
  p.put("status","EMPTY_NO_ROWS");r=ResearchTableAuditR32.audit("T07",p);check(code(r,"PAGE_STATUS_ROWS_CONFLICT"),"empty status with rows rejected");
  r=ResearchTableAuditR32.audit("T07",row("status","EMPTY_NO_ROWS","rows",Collections.emptyList()));check("EMPTY_PAGE_NO_MEASURED_EFFECT".equals(r.get("status")),"genuine empty source stays distinct from query failure");
  r=ResearchTableAuditR32.audit("T99",page(Collections.emptyList()));check(code(r,"UNKNOWN_TABLE_ID"),"unknown empty ID not silently accepted");
  r=audit("T02",row("n_select",1.5,"missing_rate",0.1));check(code(r,"ATOM_SUPPORT_VALUE_INVALID"),"fractional independent support invalid");
  r=audit("T05",row("evaluated_calendar_issues",1.5,"mean_gain",0,"lower95",-1,"upper95",1));check(code(r,"PAIRED_SUPPORT_INVALID"),"fractional paired support invalid");
  p=page(Arrays.asList(row("model_hash","m","trained_through_ordinal",10,"target_ordinal",12,"feature_through_ordinal",11,"model_available_at",1000,"frozen_at",2000,"deadline_at",3000)));
  String before=JsonR27.write(p);r=ResearchTableAuditR32.audit("T11",p);check(before.equals(JsonR27.write(p)),"input immutable");check("NONE_READ_ONLY".equals(r.get("qualityGateEffect")),"no gate effect");
  System.out.println("RESULT checks="+checks+" failures="+failures);if(failures!=0)System.exit(1);
 }
}
