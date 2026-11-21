package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import android.database.Cursor;

/** Immutable pinned inputs; only a bounded student page is read. A bad student never blocks source-only work. */
public final class ResearchInputContextR33 implements AutoCloseable {
 public final EvalContextR27 context;public final long revision;public final List<PolicySpecR27> policies;public final int generation;
 public final List<Map<String,Object>> rejectedPolicies;
 private final FeatureCacheR27 cache;
 private final String scanKey,nextAfter;private final int examined;
 private static final String SCAN_PANEL="r335-research-policy-scan";
 private ResearchInputContextR33(EvalContextR27 c,FeatureCacheR27 cache,long revision,List<PolicySpecR27> policies,int generation,List<Map<String,Object>> rejected,String scanKey,String nextAfter,int examined){
  context=c;this.cache=cache;this.revision=revision;this.policies=Collections.unmodifiableList(new ArrayList<>(policies));this.generation=generation;
  rejectedPolicies=Collections.unmodifiableList(new ArrayList<>(rejected));this.scanKey=scanKey;this.nextAfter=nextAfter;this.examined=examined;
 }
 static ResearchInputContextR33 acquire(RuntimeR27 rt,ResearchPoolR27.Worker w){return acquire(rt,w,"SHARED",true);}
 static ResearchInputContextR33 acquire(RuntimeR27 rt,ResearchPoolR27.Worker w,String scope,boolean needStudents) {
  ReplayDatasetR27 data=rt.evoData;if(data==null)throw new IllegalStateException("R33_RESEARCH_WAITING_IMMUTABLE_REPLAY_DATASET");
  if(ResearchInputPinR30R1.invalid(w.ro,data.datasetHash))throw new ResearchStoreR30R1.StaleResearch();
  long revision=ResearchInputPinR30R1.revision(w.ro,data.datasetHash);if(revision<0)throw new IllegalStateException("R33_RESEARCH_SOURCE_REVISION_MISSING");
  List<PolicySpecR27> policies=new ArrayList<>();List<Map<String,Object>> rejected=new ArrayList<>();int generation=0,examined=0;String after="",last="";
  String scanKey=scope;String identity=data.datasetHash+"|"+revision;
  if(needStudents){
   try(Cursor q=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{SCAN_PANEL,scanKey})){
    if(q.moveToFirst()){try{Object raw=JsonR27.parse(q.getString(0));if(raw instanceof Map){Map<?,?> m=(Map<?,?>)raw;if(identity.equals(m.get("identity"))&&m.get("after") instanceof String&&((String)m.get("after")).matches("[a-f0-9]{64}|"))after=(String)m.get("after");}}catch(IllegalArgumentException invalidHint){after="";}}
   }
   try(Cursor q=w.ro.rawQuery("SELECT policy_hash,spec_json,birth_generation FROM r27_policy WHERE dataset_hash=? AND state IN ('ACTIVE','BORN') AND policy_hash>? ORDER BY policy_hash LIMIT 64",new String[]{data.datasetHash,after})) {
    while(policies.size()<8&&q.moveToNext()){
     examined++;last=q.getString(0);
     try{PolicySpecR27 policy=PolicySpecR27.parse(q.getString(1));if(!last.equals(policy.policyHash()))throw new IllegalArgumentException("R33_RESEARCH_POLICY_HASH");
      if(!FailureProfileR30.legal(policy))throw new IllegalArgumentException("R335_RESEARCH_POLICY_CONTRACT");
      policies.add(policy);generation=Math.max(generation,q.getInt(2));
     }catch(IllegalArgumentException|IllegalStateException invalid){
      Error fatal=FailureDispositionR31.processFatalCause(invalid);if(fatal!=null)throw fatal;
      if(MatrixResearchR30R1.isStorageFaultR31(invalid)||FailureDispositionR31.classify(invalid).disposition!=FailureDispositionR31.Disposition.FAULT)throw invalid;
      Map<String,Object> bad=new LinkedHashMap<>();bad.put("policyHash",last);bad.put("reason",invalid.getClass().getSimpleName()+":"+invalid.getMessage());bad.put("disposition","ISOLATED_ORIGINAL_RETAINED");rejected.add(Collections.unmodifiableMap(bad));
     }
    }
   }
  }
  SplitR27 split=SplitR27.of(data,Collections.<Long>emptySet());if(split.selectFrom>=split.selectTo)throw new IllegalStateException("R33_RESEARCH_SELECT_EMPTY");
  FeatureCacheR27 privateCache=new FeatureCacheR27(data,8);
  try {String abi=rt.operatorAbiHash;if(abi!=null)privateCache.bindSharedFeaturesR31(abi);EvalContextR27 context=new EvalContextR27(privateCache,split,null);
   if(context.labels!=data.draws)throw new IllegalStateException("R33_RESEARCH_LABEL_ROLE");
   return new ResearchInputContextR33(context,privateCache,revision,policies,generation,rejected,needStudents?scanKey:null,last,examined);
  }catch(RuntimeException|Error failure){privateCache.close();throw failure;}
 }
 /** A scheduling hint only. It never serves as a numerical source or an authorization receipt. */
 void appendScan(ResearchWriteQueueR27.Batch batch,long now){if(scanKey==null)return;Map<String,Object> m=new LinkedHashMap<>();m.put("identity",context.data.datasetHash+"|"+revision);m.put("after",nextAfter);m.put("examined",examined);m.put("rejected",rejectedPolicies);m.put("authority","BOUNDED_KEYSET_SCHEDULING_HINT_ONLY");batch.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",SCAN_PANEL,scanKey,JsonR27.write(m),now);}
 @Override public void close(){try{context.close();}finally{cache.close();}}
}
