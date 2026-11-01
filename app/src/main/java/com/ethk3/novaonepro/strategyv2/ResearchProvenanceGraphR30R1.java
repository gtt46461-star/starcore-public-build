package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.util.*;
/** Bounded graph projections. Edges require the original key, never a shared issue number alone. */
final class ResearchProvenanceGraphR30R1 {
 private ResearchProvenanceGraphR30R1(){}
 static Map<String,Object> node(String id,Object value,String detail){return ResearchObservatoryR30R1.point(id,value,-1,detail);}
 @SuppressWarnings("unchecked") static void edge(Map<String,Object> f,String a,String b,String proof){Map<String,Object> e=new LinkedHashMap<>();e.put("from",a);e.put("to",b);e.put("label",proof);((List<Object>)f.get("edges")).add(e);}
 static String shortId(String s){return s==null?"缺证":s.substring(0,Math.min(10,s.length()));}
 static void gradient(ResearchWriteQueueR27.Batch b,AtomNetSessionR30 s,String checkpoint,long now){
  if(!s.ready())return;String weights=StatsR27.sha256(s.trainer.net.encode()),run=StatsR27.sha256("GRADIENT_PUBLICATION1|"+checkpoint+"|"+BuildConfig.SOURCE_COMMIT_R26);Map<String,Object> p=new LinkedHashMap<>();p.put("checkpointHash",checkpoint);p.put("weightsHash",weights);p.put("gradientPrimarySamplesCommitted",s.pretrainSamples+s.trainer.onlineSteps);p.put("throughOrdinal",s.trainedThroughOrdinal());p.put("featureAbi",AtomNetSessionR30.ABI);p.put("learningProtocol",AtomNetSessionR30.LEARNING_PROTOCOL);p.put("publication","ORIGINAL_IMMUTABLE_MODEL_AFTER_THIS_TRANSACTION_COMMITS");p.put("publicationAvailableAt",null);p.put("naturalQuality","NOT_PROVEN");String payload=JsonR27.write(p);String role=s.trainer.onlineSteps==0?"SELECT":"ONLINE_RESEARCH";
  b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,s.datasetHash,weights,role,BuildConfig.SOURCE_COMMIT_R26,-1,s.trainedThroughOrdinal(),s.trainedThroughOrdinal(),StatsR27.sha256(payload),"COMMITTED",payload,now);
  b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,weights,"NEURAL_GRADIENT_PUBLICATION",checkpoint,"ORIGINAL_CHECKPOINT_SAME_COMMIT_WEIGHTS_HASH",s.trainedThroughOrdinal(),s.trainedThroughOrdinal(),0,null,null,null,payload);
 }
 @SuppressWarnings("unchecked") static void project(String view,Map<String,Object> f,SQLiteDatabase db){
  if(!Arrays.asList("R05","R14","R18").contains(view))return;
  List<Object> records=new ArrayList<>(ResearchObservatoryR30R1.points(f));f.put("sourceRecords",records);ResearchObservatoryR30R1.points(f).clear();List<Object> out=ResearchObservatoryR30R1.points(f);
  if(view.equals("R14")){
   try(Cursor q=db.rawQuery("SELECT c.call_key,c.policy_hash,c.model_hash,c.target_ordinal,c.position,c.probability_hash,a.weights,s.probability_hash,f.hit,f.settled_at,f.frozen_at,c.frozen_at FROM r30_model_call c JOIN r30_model_artifact a ON a.model_hash=c.model_hash JOIN r30_model_surface s ON s.model_hash=c.model_hash AND s.target_ordinal=c.target_ordinal AND s.position=c.position AND s.probability_hash=c.probability_hash JOIN r27_live_frozen f ON f.policy_hash=c.policy_hash AND f.target_ordinal=c.target_ordinal AND f.frozen_at=c.frozen_at WHERE c.status='MATCHED' AND f.status='FROZEN' AND a.feature_abi=? ORDER BY c.called_at DESC,c.call_key LIMIT 8",new String[]{AtomNetSessionR30.ABI})){
    while(q.moveToNext()){String key=shortId(q.getString(0))+":"+q.getLong(3)+":P"+q.getInt(4),weights=StatsR27.sha256(q.getBlob(6)),gradient=null;try(Cursor g=db.rawQuery("SELECT payload_json FROM r30r1_research_measure WHERE kind='NEURAL_GRADIENT_PUBLICATION' AND measure_key=? ORDER BY rowid DESC LIMIT 1",new String[]{weights})){if(g.moveToFirst())gradient=g.getString(0);}
     String a=key+" 梯度",b=key+" 模型",c=key+" 概率面",d=key+" AST调用",e=key+" 冻结",z=key+" 结算";out.add(node(a,gradient==null?null:1,gradient==null?"旧模型未保存梯度→权重原键；UNKNOWN":gradient));out.add(node(b,1,"modelHash="+q.getString(2)+"; weightsHash="+weights));out.add(node(c,1,"probabilityHash="+q.getString(5)+"; 原目标/位置/模型/概率hash全匹配"));out.add(node(d,1,"policy="+q.getString(1)+"; call="+q.getString(0)));out.add(node(e,1,"原frozen_at精确相同="+q.getLong(10)));out.add(node(z,q.getInt(8)<0?null:q.getInt(8),"原冻结hit="+q.getInt(8)+"; settled_at="+q.getLong(9)+"; 非REAL结算"));if(gradient!=null)edge(f,a,b,"EXACT_WEIGHTS_HASH");edge(f,b,c,"EXACT_MODEL_HASH");edge(f,c,d,"EXACT_TARGET_POSITION_PROBABILITY_HASH");edge(f,d,e,"EXACT_POLICY_TARGET_FROZEN_AT");if(q.getInt(8)>=0&&q.getLong(9)>0)edge(f,e,z,"ORIGINAL_FROZEN_ROW_SETTLED");
    }
   }f.put("chainScope","LATEST_8_EXACT_CALL_CHAINS; FULL_DENOMINATOR_IN_CONSUMPTION; MISSING_GRADIENT_RECEIPTS_STAY_UNKNOWN");
  }else{
   for(int i=0;i<Math.min(view.equals("R18")?10:16,records.size());i++){
    Map<String,Object> r=(Map<String,Object>)records.get(i);String key=String.valueOf(i+1);if(view.equals("R05")){
     String a=key+" 失败/处方",b=key+" 子代",c=key+" SELECT回放";out.add(node(a,null,String.valueOf(r.get("details"))));if(r.get("childHash")!=null){out.add(node(b,1,"hash="+r.get("childHash")+"; AST="+r.get("actualAst")+"; admission="+r.get("admissionState")));edge(f,a,b,"EXPERT_PROPOSAL_EXACT_CHILD_HASH");if(r.get("selectBinaryBrier") instanceof Number){out.add(node(c,r.get("selectBinaryBrier"),"开发回放，不能从出生产量推断有效"));edge(f,b,c,"EXACT_CHILD_DATASET_SELECT");}}
    }else{
     String a=key+" 分析",b=key+" 提案",c=key+" 子代",d=key+" 独立确认",e=key+" 实验/资格",z=key+" REAL缺证";out.add(node(a,null,"run="+r.get("analysisRun")));out.add(node(b,null,"proposal="+r.get("proposal")));edge(f,a,b,"EXACT_RUN_ID");if(r.get("childHash")!=null){out.add(node(c,1,"child="+r.get("childHash")+"; "+r.get("admissionState")));edge(f,b,c,"ORIGINAL_CONSUME_CHILD_HASH");}if(r.get("confirmationTask")!=null){out.add(node(d,r.get("verified"),"task="+r.get("confirmationTask")+"; "+r.get("confirmationState")));edge(f,c,d,"EXACT_POLICY_HASH");}if(r.get("experiment")!=null){out.add(node(e,null,"experiment="+r.get("experiment")+"; certificate="+r.get("certificateStatus")+"; sequential="+r.get("sequentialState")));edge(f,d,e,"EXACT_REGISTERED_EXPERIMENT_ID");}out.add(node(z,null,String.valueOf(r.get("attributionReason"))));
    }
   }
  }
  if(out.isEmpty()){f.put("status","INSUFFICIENT");f.put("reason","NO_EXACT_ORIGINAL_GRAPH_KEYS_YET");}
 }
}
