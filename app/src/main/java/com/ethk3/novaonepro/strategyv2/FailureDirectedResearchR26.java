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
 public static List<Batch> select(SQLiteDatabase db,String snapshot,long sourceThrough)throws JSONException{
  if(!installed(db))return Collections.emptyList();List<Batch> batches=new ArrayList<>();
  String sql="SELECT l.policy_hash,l.canonical_text,q.evidence_digest,q.reason,q.window_start,q.window_end,q.acts,q.hits,q.rate,coalesce(f.window_end,-1),coalesce(f.evidence_digest,''),cohort.from_sequence,q.look,q.worst_block,q.paired_lower,q.ece FROM r26_qualification_result q JOIN r26_live_model l ON l.policy_hash=q.policy_hash JOIN r26_cohort cohort ON cohort.genome_key=l.policy_hash LEFT JOIN r26_failure_research_cursor f ON f.parent_policy=l.policy_hash WHERE l.status='ACTIVE' AND cohort.protocol=? AND q.window_end<=? AND q.window_end>coalesce(f.window_end,-1) AND q.reason IN ('OBSERVED_HIT_RATE_LT_035','BLOCK_BELOW_FLOOR','PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001','COVERAGE_LT_0.15','HELD_LATER_ECE_GT_004') ORDER BY q.checked_at,l.policy_hash LIMIT 16";
  try(Cursor c=db.rawQuery(sql,new String[]{QualificationWindowR26.PROTOCOL,Long.toString(Math.addExact(sourceThrough,1))})){while(c.moveToNext()&&batches.size()<3){ExecutionBudgetR26.poll();
   String parent=c.getString(0),text=c.getString(1),digest=c.getString(2),reason=c.getString(3);long end=c.getLong(5),acts=c.getLong(6),hits=c.getLong(7);double rate=c.getDouble(8);
   if(!digest.matches("[0-9a-f]{64}")||end-c.getLong(4)!=QualificationWindowR26.LENGTH||acts<1||acts>QualificationWindowR26.LENGTH||hits<0||hits>acts||!Double.isFinite(rate)||Math.abs(rate-hits/(double)acts)>1e-10||c.getLong(4)<c.getLong(11)||(c.getLong(4)-c.getLong(11))%QualificationWindowR26.STRIDE!=0||c.getInt(12)!=(c.getLong(4)-c.getLong(11))/QualificationWindowR26.STRIDE+1||!CurrentPolicyR26.matches(text,parent))continue;
   boolean observedFailure=reason.equals("OBSERVED_HIT_RATE_LT_035")?rate<.35:reason.equals("COVERAGE_LT_0.15")?acts/(double)QualificationWindowR26.LENGTH<.15:reason.equals("BLOCK_BELOW_FLOOR")?Double.isFinite(c.getDouble(13))&&c.getDouble(13)<.28:reason.equals("PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001")?!c.isNull(14)&&Double.isFinite(c.getDouble(14))&&c.getDouble(14)<-.01:!c.isNull(15)&&Double.isFinite(c.getDouble(15))&&c.getDouble(15)>.04;if(!observedFailure)continue;
   ModelGenomeSU2 original=ModelGenomeSU2.parse(text);if(WarmModelSeedR26.has(original)||StructuralProposalR26.control(original))continue;
   boolean hmm=false;for(ModelGenomeSU2.Member m:original.members)hmm|=m.kind.equals("R26F08");ModelGenomeSU2.Member complement=FamilyRegistryR26.genome(hmm?"F17":"F08").members.get(0);
   List<StructuralProposalR26.Proposal> selected=new ArrayList<>();for(StructuralProposalR26.Proposal p:StructuralProposalR26.from(original,complement))if(p.category.equals("STRUCTURAL_CANDIDATE")&&!StructuralProposalR26.control(p.genome)){selected.add(p);if(selected.size()==2)break;}
   if(selected.isEmpty())continue;
   JSONObject metadata=new JSONObject().put("schema","R26_FAILURE_GUIDED_RESEARCH_V1").put("parentPolicy",parent).put("failureReason",reason).put("failureEvidenceDigest",digest).put("failureWindowFrom",c.getLong(4)).put("failureWindowToExclusive",end).put("failureActs",acts).put("failureHits",hits).put("failureRate",rate).put("researchSnapshot",snapshot).put("sourceThrough",sourceThrough).put("initialization","NEW_FROZEN_SOURCE_RETRAINING_NO_WEIGHTS_OR_EVIDENCE_INHERITED").put("nextResearch",StrategyAssessmentR26.repair(reason)).put("evidenceScope","FAILED_LOOK_IS_RESEARCH_INPUT_ONLY; CHILD_REQUIRES_NEW_BIRTH_AND_INDEPENDENT_FUTURE_EVIDENCE");
   // A fresh, equally scheduled parent-structure control accompanies at most two real edits.
   selected.add(0,new StructuralProposalR26.Proposal(original,parent,"PRESERVE_PARENT_STRUCTURE","NO_OP"));List<StructuralProposalR26.Proposal> bound=new ArrayList<>();JSONArray children=new JSONArray();
   for(StructuralProposalR26.Proposal p:selected){ModelGenomeSU2 child=p.genome.bindBirth(snapshot,sourceThrough);JSONObject event=new JSONObject(metadata.toString()).put("operator",p.operation).put("childPolicy",child.key()).put("category",p.category).put("birthStatus","REGISTERED_NOT_TRAINED_OR_QUALIFIED_AT_BIRTH");String operation="FAILURE_GUIDED_V1:"+event.toString();bound.add(new StructuralProposalR26.Proposal(child,parent,operation,p.category));children.put(new JSONObject().put("policy",child.key()).put("operator",p.operation).put("category",p.category));}
   metadata.put("children",children);batches.add(new Batch(parent,text,digest,reason,c.getString(10),end,c.getLong(9),metadata.toString(),bound));
  }}return batches;
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
