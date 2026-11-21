package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Additional receipts for actual non-BORN mutations. BORN stays ResearchStoreR30R1.consume.
 * Caller adds the real CAS mutation first; this class only verifies the observed result in SAME transaction.
 * Readback must query the persisted canonical object, never return a prepared object captured by a lambda.
 */
public final class ResearchActionStoreR33 {
 private ResearchActionStoreR33(){}
 public interface Readback { String canonicalJson(SQLiteDatabase db); }
 public static final class Plan {
  public final ResearchActionPolicyR33.Source source;public final String kind,object,beforeHash,afterHash,key;
  public final long budget,decisionOrdinal,preparedAt;
  public final String decisionDataset; public final long decisionRevision;
  public Plan(ResearchActionPolicyR33.Source source,String kind,String object,String beforeJson,String afterJson,long budget,long decisionOrdinal,long preparedAt,String decisionDataset,long decisionRevision){
   if(!Arrays.asList("BUDGET_APPLIED","DUPLICATE_REJECTED","REPLAY_PLAN_CREATED","MECHANISM_SKIPPED").contains(kind))throw new IllegalArgumentException("ACTION_KIND_OR_BORN_OWNER");
   if(object==null||object.isEmpty()||budget<=0)throw new IllegalArgumentException("ACTION_INPUT");
   this.decisionDataset=ResearchActionPolicyR33.hash(decisionDataset);this.decisionRevision=decisionRevision;
   source.requireAction(this.decisionDataset,this.decisionRevision,FrozenConfirmationR30.currentSemantics(),com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,decisionOrdinal,preparedAt);
   this.source=source;this.kind=kind;this.object=object;beforeHash=StatsR27.sha256(beforeJson);afterHash=StatsR27.sha256(afterJson);this.budget=budget;this.decisionOrdinal=decisionOrdinal;this.preparedAt=preparedAt;
   if(beforeHash.equals(afterHash))throw new IllegalArgumentException("NO_STATE_CHANGE_NOT_APPLIED");
   key=StatsR27.sha256("R33_ACTION1|"+source.run+"|"+source.view+"|"+kind+"|"+object+"|"+afterHash);
  }
 }
 /** On redelivery, call before enqueuing any business mutation. After a writer race,
  * repeat this query on a fresh snapshot. A receipt has to match the original plan. */
 public static boolean alreadyApplied(SQLiteDatabase db, Plan p) {
  ResearchActionSchemaR33.requireReady(db);
  try(Cursor q = db.rawQuery("SELECT source_run,view_id,action_kind,role,dataset_hash,source_revision,source_commit,source_abi,input_hash,product_hash,before_hash,after_hash,object_key,action_budget,action_ordinal,latest_label_ordinal,applied_json FROM r33_research_applied WHERE consume_key=?",new String[]{p.key})) {
   if(!q.moveToFirst()) return false;
   ResearchActionPolicyR33.Source s = p.source;
   boolean equal = s.run.equals(q.getString(0)) && s.view.equals(q.getString(1)) && p.kind.equals(q.getString(2))
     && s.role.equals(q.getString(3)) && s.dataset.equals(q.getString(4)) && s.revision==q.getLong(5)
     && s.commit.equals(q.getString(6)) && s.abi.equals(q.getString(7)) && s.inputHash.equals(q.getString(8))
     && s.productHash.equals(q.getString(9)) && p.beforeHash.equals(q.getString(10)) && p.afterHash.equals(q.getString(11))
     && p.object.equals(q.getString(12)) && p.budget==q.getLong(13) && p.decisionOrdinal==q.getLong(14)
     && s.throughOrdinal==q.getLong(15) && p.afterHash.equals(StatsR27.sha256(q.getString(16)));
   if(!equal) throw new IllegalStateException("ACTION_IMMUTABLE_KEY_DRIFT");
   return true;
  }
 }
 public static void append(ResearchWriteQueueR27.Batch batch,Plan p,Readback readback){
  ResearchActionPolicyR33.Source s=p.source;
  s.requireAction(p.decisionDataset,p.decisionRevision,FrozenConfirmationR30.currentSemantics(),com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,p.decisionOrdinal,p.preparedAt);
  batch.guardBefore(db->{
   ResearchActionSchemaR33.requireReady(db);
   ResearchInputPinR30R1.require(db,s.dataset,s.revision);
   try(Cursor q=db.rawQuery("SELECT role,source_revision,source_commit,summary_json FROM r30r1_analysis_run WHERE run_id=? AND dataset_hash=? AND status='COMMITTED'",new String[]{s.run,s.dataset})){
    if(!q.moveToFirst()||!s.role.equals(q.getString(0))||s.revision!=q.getLong(1)||!s.commit.equals(q.getString(2))||!ResearchStoreR30R1.currentSource(q.getString(2),q.getString(3))||!s.productHash.equals(StatsR27.sha256(q.getString(3))))throw new ResearchStoreR30R1.StaleResearch();
    String original=q.getString(3);if(original.length()>262144)throw new IllegalStateException("ACTION_ORIGINAL_BOUND");
    Map<?,?> root=(Map<?,?>)JsonR27.parse(original),contract=(Map<?,?>)root.get("sourceContract");
    if(s.view.startsWith("MATRIX:")) {
     if(!s.inputHash.equals(root.get("matrixHash")))throw new IllegalStateException("ACTION_MATRIX_HASH_MISMATCH");
     try(Cursor matrix=db.rawQuery("SELECT 1 FROM research_matrix_snapshot_r930 WHERE matrix_hash=? AND snapshot_hash=? AND kind=? AND role=? AND bundle_hash=? AND source_revision=? AND present_cells>0",new String[]{s.inputHash,s.inputHash,s.view.substring(7),s.role,s.dataset,Long.toString(s.revision)})){
      if(!matrix.moveToFirst())throw new IllegalStateException("ACTION_MATRIX_ORIGINAL_MISSING");
     }
    } else if(!s.view.equals(root.get("viewId")))throw new IllegalStateException("ACTION_VIEW_ORIGINAL_MISMATCH");
    if(!s.inputHash.equals(root.get("inputHash"))||contract==null||!s.dataset.equals(contract.get("dataset"))||!s.commit.equals(contract.get("source"))||!(contract.get("revision") instanceof Number)||((Number)contract.get("revision")).longValue()!=s.revision||!s.role.equals(contract.get("role"))||!s.inputHash.equals(contract.get("input"))||!s.abi.equals(contract.get("abi"))||!(contract.get("through") instanceof Number)||((Number)contract.get("through")).longValue()!=s.throughOrdinal||!(contract.get("available") instanceof Number)||((Number)contract.get("available")).longValue()!=s.availableAt)throw new IllegalStateException("ACTION_SOURCE_CONTRACT_SUBSTITUTED");
   }
   try(Cursor q=db.rawQuery("SELECT 1 FROM r30r1_research_measure WHERE run_id=? AND measure_key=?",new String[]{s.run,s.view})){if(!q.moveToFirst())throw new IllegalStateException("ACTION_VIEW_ORIGINAL_MISSING");}
   if(!p.beforeHash.equals(StatsR27.sha256(readback.canonicalJson(db))))throw new IllegalStateException("ACTION_CAS_BEFORE_CHANGED");
   return 0;
  });
  final com.ethk3.novaonepro.DatabaseCoordinator.WriteCallable<Integer> prior=batch.after;
  batch.after=db->{
   int result=prior==null?0:prior.call(db);
   if(!db.inTransaction())throw new IllegalStateException("ACTION_NOT_ATOMIC_WITH_CANONICAL_MUTATION");
   String actual=readback.canonicalJson(db);
   if(!p.afterHash.equals(StatsR27.sha256(actual)))throw new IllegalStateException("ACTION_CAS_AFTER_NOT_APPLIED");
   db.execSQL("INSERT INTO r33_research_applied(consume_key,source_run,view_id,action_kind,role,dataset_hash,source_revision,source_commit,source_abi,input_hash,product_hash,before_hash,after_hash,object_key,action_budget,action_ordinal,latest_label_ordinal,applied_json,applied_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{p.key,s.run,s.view,p.kind,s.role,s.dataset,s.revision,s.commit,s.abi,s.inputHash,s.productHash,p.beforeHash,p.afterHash,p.object,p.budget,p.decisionOrdinal,s.throughOrdinal,actual,System.currentTimeMillis()});
   return result;
  };
 }
}
