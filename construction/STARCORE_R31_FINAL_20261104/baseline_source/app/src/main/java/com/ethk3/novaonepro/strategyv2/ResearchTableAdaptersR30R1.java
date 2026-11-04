package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Stable indexed logical research views. Full machine inputs remain in original tables and snapshot.db. */
public final class ResearchTableAdaptersR30R1 {
 private ResearchTableAdaptersR30R1(){}private static final Map<String,String> SQL=new LinkedHashMap<>();
 static {
  SQL.put("T01","SELECT run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,contract_hash,status,summary_json FROM r30r1_analysis_run ORDER BY recorded_at DESC,run_id");
  SQL.put("T02","SELECT atom_id,dataset_hash,n_select,missing_rate,state,definition,computed_at FROM r27_atom_score ORDER BY atom_id");
  SQL.put("T03","SELECT policy_hash,target_issue,target_ordinal,position,probabilities,frozen_at,deadline_at,hit,settled_at,'FUTURE_READ_ONLY' AS role FROM r27_live_frozen ORDER BY target_ordinal DESC,policy_hash");
  SQL.put("T04","SELECT run_id,measure_key,kind,parent_hash,from_ordinal,to_ordinal,payload_json FROM r30r1_research_measure WHERE kind IN ('FAILURE_PRESCRIPTION','FAILURE_MEMORY','STRUCTURE_FORECAST_ERROR') ORDER BY run_id,measure_key");
  SQL.put("T05","SELECT run_id,measure_key,parent_hash,from_ordinal,to_ordinal,evaluated_calendar_issues,mean_gain,lower95,upper95,payload_json FROM r30r1_research_measure WHERE kind='ATOM_PAIRED_GAIN' ORDER BY run_id,measure_key");
  SQL.put("T06","SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='MOLECULE_SYNERGY' ORDER BY run_id,measure_key");
  SQL.put("T07","SELECT dataset_hash,probe_set_hash,signature,kept_policy,dropped_policy,stage,recorded_at FROM r30_behavior_dup ORDER BY recorded_at DESC,kept_policy,dropped_policy");
  SQL.put("T08","SELECT generation,expert,proposal_json,policy_hash,recorded_at FROM r27_expert_proposal ORDER BY generation DESC,expert");
  SQL.put("T09","SELECT r.run_id,r.role,r.policy_hash,m.measure_key,m.kind,m.mean_gain,m.payload_json FROM r30r1_analysis_run r JOIN r30r1_research_measure m ON m.run_id=r.run_id WHERE m.kind IN ('OPERATOR_COST','PARENT_CHILD_GAIN','RESEARCH_BUDGET') ORDER BY r.recorded_at DESC,m.measure_key");
  SQL.put("T10","SELECT run_id,measure_key,parent_hash,payload_json FROM r30r1_research_measure WHERE kind='FUSION_COMPLEMENTARITY' ORDER BY run_id,measure_key");
  SQL.put("T11","SELECT target_ordinal,position,model_hash,history_hash,probabilities,probability_hash,trained_through_ordinal,model_available_at,feature_through_ordinal,frozen_at,deadline_at FROM r30_model_surface ORDER BY target_ordinal DESC,position,model_hash");
  SQL.put("T12","SELECT key AS policy_hash,payload_json,updated_at FROM r27_chart_cache WHERE panel_id='calibration-r30' ORDER BY key");
  SQL.put("T13","SELECT task_key,policy_hash,dataset_hash,block_id,state,verified,registration_state,registration_experiment_id,packet_json,result_json,'FORMAL_READ_ONLY' AS role FROM r30_frozen_confirm ORDER BY created_at DESC,task_key");
  SQL.put("T14","SELECT p.*,t.lane,t.status,t.blocked_reason FROM r30r1_task_progress p LEFT JOIN r26_task t ON t.task_id=p.task_id ORDER BY p.task_id");
  SQL.put("T15","SELECT p.proposal_id,p.run_id,p.parent_hash,p.kind,p.payload_json,p.state,p.reason,c.child_policy_hash,c.source_generation,c.consumer_version,c.receipt_hash,c.consumed_at FROM r30r1_proposal p LEFT JOIN r30r1_consume c ON c.proposal_id=p.proposal_id ORDER BY p.created_at DESC,p.proposal_id");
  SQL.put("T16","SELECT issue,decision_key,entry_point,original_allow,original_reason,certificate_active,items_json,source_json,recorded_at,'FORMAL_READ_ONLY' AS role FROM r30_decision_explain ORDER BY recorded_at DESC,issue");
}
 public static String query(String id){return SQL.get(id);}
 public static List<Map<String,Object>> contracts(){List<Map<String,Object>> out=new ArrayList<>();for(Map.Entry<String,String> e:SQL.entrySet()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getKey());m.put("sourceSql",e.getValue());m.put("owner","ORIGINAL_SOURCE_OWNER_AND_SchemaR27");m.put("newPhysicalTable",false);m.put("displayIsFullDenominator",false);m.put("fullSource","verified snapshot.db; construction/r30-repair/RESEARCH_TABLE_CONTRACTS.json");out.add(m);}return Collections.unmodifiableList(out);}
}
