package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;
/** Revealed failed looks may guide NEW research; their results never transfer to a child. */
public final class FailureDirectedResearchR26 {
 private FailureDirectedResearchR26(){}
 public static final class Batch {
  final String parent,text,digest,reason,oldDigest,payload;final long windowEnd,oldEnd;final List<StructuralProposalR26.Proposal> proposals;
  Batch(String parent,String text,String digest,String reason,String oldDigest,long end,long oldEnd,String payload,List<StructuralProposalR26.Proposal> proposals){this.parent=parent;this.text=text;this.digest=digest;this.reason=reason;this.oldDigest=oldDigest;windowEnd=end;this.oldEnd=oldEnd;this.payload=payload;this.proposals=Collections.unmodifiableList(proposals);}
 }
 static boolean installed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_failure_research_cursor'",null)){return c.moveToFirst();}}
 public static List<Batch> select(SQLiteDatabase db,String snapshot,long sourceThrough)throws JSONException{return select(db,snapshot,sourceThrough,4096);}
 public static List<Batch> select(SQLiteDatabase db,String snapshot,long sourceThrough,int sourceSamples)throws JSONException{
  if(!installed(db)||!HypothesisStoreR31.canAdmitResearchR31(db,sourceSamples))return Collections.emptyList();List<Batch> batches=new ArrayList<>();
  String sql="SELECT l.policy_hash,l.canonical_text,q.evidence_digest,q.reason,q.window_start,q.window_end,q.acts,q.hits,q.rate,coalesce(f.window_end,-1),coalesce(f.evidence_digest,''),cohort.from_sequence,q.look,q.worst_block,q.paired_lower,q.ece,coalesce(f.proposal_json,'') FROM r26_qualification_result q JOIN r26_live_model l ON l.policy_hash=q.policy_hash JOIN r26_cohort cohort ON cohort.genome_key=l.policy_hash LEFT JOIN r26_failure_research_cursor f ON f.parent_policy=l.policy_hash WHERE l.status='ACTIVE' AND cohort.protocol=? AND q.window_end<=? AND q.window_end>coalesce(f.window_end,-1) AND q.reason IN ('OBSERVED_HIT_RATE_LT_035','BLOCK_BELOW_FLOOR','PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001','COVERAGE_LT_0.15','HELD_LATER_ECE_GT_004') ORDER BY q.checked_at,l.policy_hash LIMIT 16";
  try(Cursor c=db.rawQuery(sql,new String[]{QualificationWindowR26.PROTOCOL,Long.toString(Math.addExact(sourceThrough,1))})){while(c.moveToNext()){ExecutionBudgetR26.poll();
   String parent=c.getString(0),text=c.getString(1),digest=c.getString(2),reason=c.getString(3);long end=c.getLong(5),acts=c.getLong(6),hits=c.getLong(7);double rate=c.getDouble(8);
   if(!digest.matches("[0-9a-f]{64}")||end-c.getLong(4)!=QualificationWindowR26.LENGTH||acts<1||acts>QualificationWindowR26.LENGTH||hits<0||hits>acts||!Double.isFinite(rate)||Math.abs(rate-hits/(double)acts)>1e-10||c.getLong(4)<c.getLong(11)||(c.getLong(4)-c.getLong(11))%QualificationWindowR26.STRIDE!=0||c.getInt(12)!=(c.getLong(4)-c.getLong(11))/QualificationWindowR26.STRIDE+1||!CurrentPolicyR26.matches(text,parent))continue;
   boolean observedFailure=reason.equals("OBSERVED_HIT_RATE_LT_035")?rate<.35:reason.equals("COVERAGE_LT_0.15")?acts/(double)QualificationWindowR26.LENGTH<.15:reason.equals("BLOCK_BELOW_FLOOR")?Double.isFinite(c.getDouble(13))&&c.getDouble(13)<.28:reason.equals("PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001")?!c.isNull(14)&&Double.isFinite(c.getDouble(14))&&c.getDouble(14)<-.01:!c.isNull(15)&&Double.isFinite(c.getDouble(15))&&c.getDouble(15)>.04;if(!observedFailure)continue;
   ModelGenomeSU2 original=ModelGenomeSU2.parse(text);if(WarmModelSeedR26.has(original)||StructuralProposalR26.control(original))continue;
   ResearchHypothesisR31.Hypothesis hypothesis=ResearchHypothesisR31.fromFailure(new ResearchHypothesisR31.Failure(parent,digest,ResearchHypothesisR31.mechanismOf(reason),reason,"","REVEALED_FAILED_LOOK",end,end-1,acts,rate));
   ResearchHypothesisR31.Registry registry=new ResearchHypothesisR31.Registry();registry.register(hypothesis);
   String dispatch=registry.dispatch(hypothesis);
   Map<String,Object> regime=hypothesis.mechanism==ResearchHypothesisR31.Mechanism.SHORT_WINDOW_DRIFT?regimeR31(db,parent,c.getLong(4),end,c.getString(16)):Collections.emptyMap();
   List<StructuralProposalR26.Proposal> selected=routeR31(original,hypothesis,regime);
   JSONObject metadata=new JSONObject().put("schema","R26_FAILURE_GUIDED_RESEARCH_V1").put("parentPolicy",parent).put("failureReason",reason).put("failureEvidenceDigest",digest).put("failureWindowFrom",c.getLong(4)).put("failureWindowToExclusive",end).put("failureActs",acts).put("failureHits",hits).put("failureRate",rate).put("researchSnapshot",snapshot).put("sourceThrough",sourceThrough).put("initialization","NEW_FROZEN_SOURCE_RETRAINING_NO_WEIGHTS_OR_EVIDENCE_INHERITED").put("nextResearch",StrategyAssessmentR26.repair(reason)).put("evidenceScope","FAILED_LOOK_IS_RESEARCH_INPUT_ONLY; CHILD_REQUIRES_NEW_BIRTH_AND_INDEPENDENT_FUTURE_EVIDENCE");
   // R31-B07-C06: typed failure attribution travels with the batch; the original parent-structure control and cursor stay unchanged.
   metadata.put("hypothesisR31",new JSONObject(hypothesis.json()));metadata.put("dispatchR31",dispatch);
   if(!regime.isEmpty())metadata.put("regimeR31",new JSONObject(regime));
   metadata.put("adapterStateR31",selected.isEmpty()?"UNSUPPORTED":"EXECUTABLE_CANDIDATE_REQUIRES_TRAIN_SELECT_CONFIRM_FUTURE");
   if(selected.isEmpty())metadata.put("adapterReasonR31",!hypothesis.supported()?hypothesis.unsupportedReason:hypothesis.mechanism==ResearchHypothesisR31.Mechanism.CROSS_POSITION_SUPPORT?"R26_F15_IS_A_RISK_HEAD_NOT_THE_R27_STRICT_INTERSECTION_ADAPTER":"NO_DISTINCT_EXECUTABLE_ADAPTER_OR_NO_MEASURED_ACTIVE_REGIME");
   // A fresh, equally scheduled parent-structure control accompanies at most two real edits.
   if(!selected.isEmpty())selected.add(0,new StructuralProposalR26.Proposal(original,parent,"PRESERVE_PARENT_STRUCTURE","NO_OP"));List<StructuralProposalR26.Proposal> bound=new ArrayList<>();JSONArray children=new JSONArray();
   for(StructuralProposalR26.Proposal p:selected){ModelGenomeSU2 child=p.genome.bindBirth(snapshot,sourceThrough);JSONObject event=new JSONObject(metadata.toString()).put("operator",p.operation).put("childPolicy",child.key()).put("category",p.category).put("birthStatus","REGISTERED_NOT_TRAINED_OR_QUALIFIED_AT_BIRTH");String operation="FAILURE_GUIDED_V1:"+event.toString();bound.add(new StructuralProposalR26.Proposal(child,parent,operation,p.category));children.put(new JSONObject().put("policy",child.key()).put("operator",p.operation).put("category",p.category));}
   metadata.put("children",children);batches.add(new Batch(parent,text,digest,reason,c.getString(10),end,c.getLong(9),metadata.toString(),bound));
  }}
  // Reserve the oldest eligible failure for exploration; only the other two
  // slots use completed same-opportunity SELECT credit. No formal look feeds
  // this ordering, and sparse/missing credit stays at the exploration prior.
  if(batches.size()>1){Batch exploration=batches.remove(0);Map<Batch,Double> credit=new IdentityHashMap<>();for(Batch batch:batches)credit.put(batch,creditR31(db,batch));batches.sort((a,b)->Double.compare(credit.get(b),credit.get(a)));batches.add(0,exploration);}
  return distinctBatchesR31(batches);

 }
 /** Birth rebinding can collapse different historical parent identities onto
  * the same new child. Admit that actual training once; an unselected failure
  * retains its cursor and can be reconsidered in a later campaign. */
 static List<Batch> distinctBatchesR31(List<Batch> ordered){
  List<Batch> out=new ArrayList<>();Set<String> children=new HashSet<>();
  for(Batch batch:ordered){
   Set<String> proposed=new HashSet<>();boolean duplicate=false;
   for(StructuralProposalR26.Proposal proposal:batch.proposals)if(!"NO_OP".equals(proposal.category)){
    String key=proposal.genome.key();if(children.contains(key)||!proposed.add(key)){duplicate=true;break;}
   }
   if(duplicate)continue;
   out.add(batch);children.addAll(proposed);if(out.size()==3)break;
  }
  return out;
 }
 private static double creditR31(SQLiteDatabase db,Batch batch){
  try{Map<?,?> metadata=(Map<?,?>)JsonR27.parse(batch.payload);Map<?,?> hypothesis=(Map<?,?>)metadata.get("hypothesisR31");return HypothesisStoreR31.creditR31(db,(String)hypothesis.get("family"));}
  catch(IllegalArgumentException malformed){throw new IllegalStateException("R31_HYPOTHESIS_CREDIT_INPUT",malformed);}
 }
 /** The reason chooses the executable intervention before birth. Existing family
  * kernels/readouts do the work; unimplemented interventions produce no child. */
 static List<StructuralProposalR26.Proposal> routeR31(ModelGenomeSU2 parent,ResearchHypothesisR31.Hypothesis hypothesis) {
  return routeR31(parent,hypothesis,Collections.emptyMap());
 }
 static List<StructuralProposalR26.Proposal> routeR31(ModelGenomeSU2 parent,ResearchHypothesisR31.Hypothesis hypothesis,Map<String,Object> regime) {
  if(!hypothesis.supported())return new ArrayList<>();
  List<StructuralProposalR26.Proposal> out=new ArrayList<>();ModelGenomeSU2 child=null;String operation=hypothesis.action;
  switch(hypothesis.mechanism) {
   case CALIBRATION:
    if(!parent.calibrated)child=new ModelGenomeSU2(parent.members,parent.pool,parent.sink,parent.gate,parent.coverage,parent.metaC,true);
    break;
   case COVERAGE:
    for(double coverage:ModelGenomeSU2.COVERAGES)if(coverage>parent.coverage){child=new ModelGenomeSU2(parent.members,parent.pool,parent.sink,parent.gate,coverage,parent.metaC,parent.calibrated);break;}
    break;
   case SHORT_WINDOW_DRIFT: {
    if(!"REGIME".equals(regime.get("route"))||!(regime.get("selectedWindow") instanceof Number)||parent.members.size()>=4)break;
    int window=((Number)regime.get("selectedWindow")).intValue();Map<String,String> params=new TreeMap<>(ModelEvolutionSU2.centralParams("BOCPD"));params.put("R",Integer.toString(window));params.put("hazard",Double.toString(1.0/window));
    List<ModelGenomeSU2.Member> members=new ArrayList<>(parent.members);members.add(new ModelGenomeSU2.Member("BOCPD",params,1));
    child=new ModelGenomeSU2(members,"GATED",parent.sink,parent.gate,parent.coverage,parent.metaC,parent.calibrated);operation="PAST_CUSUM_NESTED_WINDOW_BOCPD_GATED_ROUTER";
    break;
   }
   case CROSS_POSITION_SUPPORT:
    // R26 F15 is a trained risk head over 1200 actions. The strict lagged
    // intersection is the separate R27 I14 AST; substituting F15 would mislabel
    // an unrelated mechanism. Keep both existing implementations and abstain.
    break;
   case RESOURCE_FAILURE:
    if(parent.members.size()>1)child=new ModelGenomeSU2(Collections.singletonList(parent.members.get(0)),"STATIC",parent.sink,parent.gate,parent.coverage,parent.metaC,parent.calibrated);
    break;
   case COMPONENT_NEGATIVE:
    // A policy-level failed look does not identify an offending component.
    // Only a separately measured component adapter may implement this action.
    break;
  }
  if(child!=null&&!child.key().equals(parent.key())&&!StructuralProposalR26.control(child))out.add(new StructuralProposalR26.Proposal(child,parent.key(),operation,"STRUCTURAL_CANDIDATE"));
  return out;
 }
 /** Revealed failed-look ACT outcomes only, strictly before the next decision.
  * Detector and nested-window decisions travel with the original cursor CAS. */
 static Map<String,Object> regimeR31(SQLiteDatabase db,String parent,long from,long end,String prior)throws JSONException {
  RegimeHypothesisR31.Protocol protocol=RegimeHypothesisR31.Protocol.registered();RegimeHypothesisR31.Detector detector=new RegimeHypothesisR31.Detector(protocol);
  if(prior!=null&&!prior.isEmpty()){JSONObject old=new JSONObject(prior);JSONObject previous=old.optJSONObject("regimeR31");if(previous!=null&&previous.has("detectorState"))detector=RegimeHypothesisR31.Detector.decode(protocol,previous.getString("detectorState"));}
  List<long[]> outcomes=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT sequence_ordinal,hit FROM su2_live_decision WHERE genome_key=? AND act=1 AND hit IS NOT NULL AND sequence_ordinal>=? AND sequence_ordinal<? ORDER BY sequence_ordinal DESC LIMIT 4096",new String[]{parent,Long.toString(from),Long.toString(end)})){while(c.moveToNext())outcomes.add(new long[]{c.getLong(0),c.getInt(1)});}
  Collections.reverse(outcomes);StringBuilder original=new StringBuilder(protocol.hash());for(long[] value:outcomes){if(value[0]>=end)throw new IllegalStateException("R31_REGIME_FUTURE_INPUT");original.append('|').append(value[0]).append('=').append(value[1]);if(value[0]>detector.observedThrough())detector.observe(value[0],value[1]==1);}
  Map<String,Object> out=new LinkedHashMap<>();out.put("protocol",protocol.hash());out.put("detectorState",detector.encode());out.put("observedThrough",detector.observedThrough());out.put("inputRole","REVEALED_FAILED_LOOK_ACT_RESEARCH_ONLY");out.put("inputDigest",StatsR27.sha256(original.toString()));out.put("samples",outcomes.size());out.put("decisionOrdinal",end);out.put("onsets",detector.onsets());out.put("expiries",detector.expiries());out.put("route",RegimeHypothesisR31.routePastState(detector,end,"REGIME","BASE"));
  if(outcomes.size()>=300){RegimeHypothesisR31.WindowSelection window=RegimeHypothesisR31.proposeWindow(new int[]{16,64,256},(width,lo,hi)->{double loss=0;for(int i=lo;i<hi;i++){double sum=3,count=10;for(int j=Math.max(0,i-width);j<i;j++){sum+=outcomes.get(j)[1];count++;}double error=sum/count-outcomes.get(i)[1];loss-=error*error;}return loss/Math.max(1,hi-lo);},0,outcomes.size(),5);out.put("selectedWindow",window.window);out.put("foldChoices",window.foldChoices);out.put("foldStarts",window.foldStarts);}
  else out.put("state","INSUFFICIENT_PAST_ACT_SUPPORT");return out;
 }
 static JSONObject hypothesisR31(String parent,String digest,String reason,long windowEnd,long acts,double rate)throws JSONException{
  try{ResearchHypothesisR31.Hypothesis h=ResearchHypothesisR31.fromFailure(new ResearchHypothesisR31.Failure(parent,digest,ResearchHypothesisR31.mechanismOf(reason),reason,"","REVEALED_FAILED_LOOK",windowEnd,windowEnd-1,acts,rate));
   return new JSONObject(h.json());}
  catch(IllegalArgumentException unsupported){return new JSONObject().put("version",ResearchHypothesisR31.VERSION).put("status","UNSUPPORTED").put("unsupportedReason",unsupported.getMessage());}
 }
 /** Called in the same fenced campaign-birth transaction after every candidate and lineage row. */
 public static void recordWithin(SQLiteDatabase db,String campaign,List<Batch> batches){
  if(!db.inTransaction())throw new IllegalStateException("R26_FAILURE_RESEARCH_WRITER");for(Batch b:batches){
   try(Cursor c=db.rawQuery("SELECT 1 FROM r26_qualification_result q JOIN r26_live_model l ON l.policy_hash=q.policy_hash WHERE q.policy_hash=? AND q.evidence_digest=? AND q.reason=? AND q.window_end=? AND l.canonical_text=? AND l.status='ACTIVE'",new String[]{b.parent,b.digest,b.reason,Long.toString(b.windowEnd),b.text})){if(!c.moveToFirst())throw new IllegalStateException("R26_FAILURE_EVIDENCE_CHANGED");}
   for(StructuralProposalR26.Proposal p:b.proposals)try(Cursor c=db.rawQuery("SELECT 1 FROM r26_candidate c JOIN r26_evolution e ON e.campaign_id=c.campaign_id AND e.policy_hash=c.policy_hash WHERE c.campaign_id=? AND c.policy_hash=? AND e.parent=? AND e.operation=?",new String[]{campaign,p.genome.key(),b.parent,p.operation})){if(!c.moveToFirst())throw new IllegalStateException("R26_FAILURE_CHILD_NOT_REGISTERED");}
   db.execSQL("INSERT OR IGNORE INTO r26_failure_research_cursor VALUES(?,'',-1,'','')",new Object[]{b.parent});db.execSQL("UPDATE r26_failure_research_cursor SET evidence_digest=?,window_end=?,campaign_id=?,proposal_json=? WHERE parent_policy=? AND evidence_digest=? AND window_end=?",new Object[]{b.digest,b.windowEnd,campaign,b.payload,b.parent,b.oldDigest,b.oldEnd});
   try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_FAILURE_RESEARCH_STALE_CURSOR");}
  }
 }
}
