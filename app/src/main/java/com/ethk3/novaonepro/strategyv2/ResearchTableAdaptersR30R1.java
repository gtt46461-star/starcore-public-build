package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Stable indexed logical research views. Full machine inputs remain in original tables and snapshot.db. */
public final class ResearchTableAdaptersR30R1 {
 private ResearchTableAdaptersR30R1(){}private static final Map<String,String> SQL=new LinkedHashMap<>();
 static {
  SQL.put("T01","SELECT run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,contract_hash,status,summary_json FROM r30r1_analysis_run ORDER BY recorded_at DESC,run_id");
  SQL.put("T02","SELECT atom_id,dataset_hash,n_select,missing_rate,state,definition,computed_at FROM r31_atom_score_visible ORDER BY atom_id");
  SQL.put("T03","SELECT policy_hash,target_issue,target_ordinal,position,probabilities,frozen_at,deadline_at,hit,settled_at,'FUTURE_READ_ONLY' AS role FROM r27_live_frozen ORDER BY target_ordinal DESC,policy_hash");
  SQL.put("T04","SELECT run_id,measure_key,kind,parent_hash,from_ordinal,to_ordinal,payload_json FROM r30r1_research_measure WHERE kind IN ('FAILURE_PRESCRIPTION','FAILURE_MEMORY','STRUCTURE_FORECAST_ERROR') ORDER BY run_id,measure_key");
  SQL.put("T05","SELECT run_id,measure_key,parent_hash,from_ordinal,to_ordinal,evaluated_calendar_issues,mean_gain,lower95,upper95,payload_json FROM r30r1_research_measure WHERE kind='ATOM_PAIRED_GAIN' ORDER BY run_id,measure_key");
  SQL.put("T06","SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='MOLECULE_SYNERGY' ORDER BY run_id,measure_key");
  SQL.put("T07","SELECT dataset_hash,probe_set_hash,signature,kept_policy,dropped_policy,stage,recorded_at FROM r30_behavior_dup ORDER BY recorded_at DESC,kept_policy,dropped_policy");
  SQL.put("T08","SELECT generation,expert,proposal_json,policy_hash,recorded_at FROM r27_expert_proposal ORDER BY generation DESC,expert");
  // R31-B08-C05: R10 writes OPERATOR_GAIN_COST (ResearchMeasureKindR31); pre-R31 names are listed only as HISTORICAL rows.
  SQL.put("T09","SELECT r.run_id,r.role,r.policy_hash,m.measure_key,m.kind,CASE m.kind "+unitCases(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+" ELSE 'HISTORICAL_UNIT_UNVERIFIED' END AS unit,CASE WHEN m.kind IN ("+ResearchMeasureKindR31.sqlList(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+") THEN 'CURRENT' ELSE 'HISTORICAL' END AS tier,m.mean_gain,m.payload_json FROM r30r1_analysis_run r JOIN r30r1_research_measure m ON m.run_id=r.run_id WHERE m.kind IN ("+ResearchMeasureKindR31.sqlList(ResearchMeasureKindR31.OPERATOR_GAIN_COST,ResearchMeasureKindR31.PARENT_CHILD_PAIRED,ResearchMeasureKindR31.RESEARCH_BUDGET)+",'OPERATOR_COST','PARENT_CHILD_GAIN') ORDER BY r.recorded_at DESC,m.measure_key");
  SQL.put("T10","SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='FUSION_COMPLEMENTARITY' ORDER BY run_id,measure_key");
  SQL.put("T11","SELECT target_ordinal,position,model_hash,history_hash,probabilities,probability_hash,trained_through_ordinal,model_available_at,feature_through_ordinal,frozen_at,deadline_at FROM r30_model_surface ORDER BY target_ordinal DESC,position,model_hash");
  SQL.put("T12","SELECT key AS policy_hash,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='calibration-r30' ORDER BY key");
  SQL.put("T13","SELECT task_key,policy_hash,dataset_hash,block_id,state,verified,registration_state,registration_experiment_id,packet_json,result_json,'FORMAL_READ_ONLY' AS role FROM r30_frozen_confirm ORDER BY created_at DESC,task_key");
  SQL.put("T14","SELECT p.*,t.lane,t.status,t.blocked_reason FROM r30r1_task_progress p LEFT JOIN r26_task t ON t.task_id=p.task_id ORDER BY p.task_id");
  SQL.put("T15","SELECT p.proposal_id,p.run_id,p.parent_hash,p.kind,p.payload_json,p.state,p.reason,c.child_policy_hash,c.source_generation,c.consumer_version,c.receipt_hash,c.consumed_at FROM r30r1_proposal p LEFT JOIN r30r1_consume c ON c.proposal_id=p.proposal_id ORDER BY p.created_at DESC,p.proposal_id");
  SQL.put("T16","SELECT issue,decision_key,entry_point,original_allow,original_reason,certificate_active,items_json,source_json,recorded_at,'FORMAL_READ_ONLY' AS role FROM r30_decision_explain ORDER BY recorded_at DESC,issue");
}
 private static String unitCases(ResearchMeasureKindR31... kinds){StringBuilder b=new StringBuilder();for(ResearchMeasureKindR31 k:kinds)b.append("WHEN '").append(k.name()).append("' THEN '").append(k.unit).append("' ");return b.toString();}
 public static String query(String id){return SQL.get(id);}
 public static final int MAX_PAGE_R31=200;
 /** Columns a reader may bind per view; everything else is fixed by the view's own SQL. */
 private static final Map<String,List<String>> FILTERS_R31=new LinkedHashMap<>();
 static{FILTERS_R31.put("T01",Arrays.asList("dataset_hash","role"));FILTERS_R31.put("T02",Collections.singletonList("dataset_hash"));FILTERS_R31.put("T07",Collections.singletonList("dataset_hash"));FILTERS_R31.put("T09",Collections.singletonList("role"));FILTERS_R31.put("T13",Collections.singletonList("dataset_hash"));}
 public static final class Page{public final String sql;public final String[] args;Page(String sql,String[] args){this.sql=sql;this.args=args;}}
 /** R31-B08-C05: one bound, bounded page of a logical view: declared filter columns only, LIMIT/OFFSET as parameters. */
 public static Page page(String id,Map<String,String> filters,long offset,int pageSize){
  String base=SQL.get(id);if(base==null)throw new IllegalArgumentException("R31_RESEARCH_TABLE_UNKNOWN:"+id);
  if(pageSize<1||pageSize>MAX_PAGE_R31||offset<0)throw new IllegalArgumentException("R31_RESEARCH_TABLE_PAGE");
  List<String> allowed=FILTERS_R31.getOrDefault(id,Collections.<String>emptyList());StringBuilder where=new StringBuilder();List<String> args=new ArrayList<>();
  if(filters!=null)for(Map.Entry<String,String> f:new TreeMap<>(filters).entrySet()){if(!allowed.contains(f.getKey())||f.getValue()==null)throw new IllegalArgumentException("R31_RESEARCH_TABLE_FILTER:"+id+":"+f.getKey());where.append(where.length()==0?" WHERE ":" AND ").append(f.getKey()).append("=?");args.add(f.getValue());}
  args.add(Integer.toString(pageSize));args.add(Long.toString(offset));
  return new Page("SELECT * FROM ("+base+")"+where+" LIMIT ? OFFSET ?",args.toArray(new String[0]));
 }
 /** Shared actual reader for report export and the paged research-table screen. */
 public static Map<String,Object> readPageR31(android.database.sqlite.SQLiteDatabase db,String id,Map<String,String> filters,long offset,int size,android.os.CancellationSignal cancellation) {
  Page query=page(id,filters,offset,size);Map<String,Object> out=new LinkedHashMap<>();List<Object> rows=new ArrayList<>();
  out.put("id",id);out.put("offset",offset);out.put("pageSize",size);out.put("filters",filters);out.put("readStartedAt",System.currentTimeMillis());out.put("readOnly",true);
  long bytes=0;
  try(android.database.Cursor cursor=db.rawQuery(query.sql,query.args,cancellation)) {
   while(cursor.moveToNext()) {
    if(cancellation!=null)cancellation.throwIfCanceled();Map<String,Object> row=new LinkedHashMap<>();
    for(int column=0;column<cursor.getColumnCount();column++) {
     String name=cursor.getColumnName(column);Object value=null;
     if(!cursor.isNull(column))switch(cursor.getType(column)) {
      case android.database.Cursor.FIELD_TYPE_INTEGER:value=cursor.getLong(column);break;
      case android.database.Cursor.FIELD_TYPE_FLOAT:value=cursor.getDouble(column);break;
      case android.database.Cursor.FIELD_TYPE_BLOB:value="BINARY_ORIGINAL_IN_SOURCE";break;
      case android.database.Cursor.FIELD_TYPE_NULL:break;
      case android.database.Cursor.FIELD_TYPE_STRING:String raw=cursor.getString(column);if(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalStateException("TABLE_CELL_BYTE_BUDGET:"+name);value=raw;break;
      default:throw new IllegalStateException("TABLE_CELL_UNKNOWN_TYPE:"+name);
     }
     row.put(name,value);
    }
    if("T09".equals(id)&&row.get("payload_json") instanceof String) {
     String kind=String.valueOf(row.get("kind"));
     if(kind.equals("OPERATOR_GAIN_COST")||kind.equals("PARENT_CHILD_PAIRED")||kind.equals("RESEARCH_BUDGET")) {
      List<Object> objects=new ArrayList<>();for(ResearchMeasureProjectionR31.ObjectRow object:ResearchMeasureProjectionR31.projectObjects(kind,(String)row.get("payload_json")))objects.add(object.json());
      row.put("objects",objects);row.put("valueSource","payload_json.frame.points; top-level NULL is not zero");
     }
    }
    int rowBytes=JsonR27.write(row).getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    if(bytes+rowBytes>524288){out.put("truncatedReason","TABLE_PAGE_BYTE_BUDGET");break;}bytes+=rowBytes;rows.add(row);
   }
   out.put("status",rows.isEmpty()?"EMPTY_NO_ROWS":"READ_OK");
  }catch(android.os.OperationCanceledException cancelled){throw cancelled;}
   catch(RuntimeException error){out.put("status","READ_ERROR");out.put("reason",error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage()));}
  out.put("rows",rows);out.put("shown",rows.size());out.put("hasMore",rows.size()==size||out.containsKey("truncatedReason"));out.put("bytes",bytes);out.put("readFinishedAt",System.currentTimeMillis());out.put("receiptSha256",StatsR27.sha256(JsonR27.write(out)));return Collections.unmodifiableMap(out);
 }
 public static Set<String> ids(){return Collections.unmodifiableSet(SQL.keySet());}
 public static List<Map<String,Object>> contracts(){List<Map<String,Object>> out=new ArrayList<>();for(Map.Entry<String,String> e:SQL.entrySet()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getKey());m.put("sourceSql",e.getValue());m.put("owner","ORIGINAL_SOURCE_OWNER_AND_SchemaR27");m.put("filters",FILTERS_R31.getOrDefault(e.getKey(),Collections.<String>emptyList()));m.put("maxPage",(long)MAX_PAGE_R31);m.put("newPhysicalTable",false);m.put("displayIsFullDenominator",false);m.put("fullSource","verified snapshot.db; construction/r30-repair/RESEARCH_TABLE_CONTRACTS.json");out.add(m);}return Collections.unmodifiableList(out);}
}
