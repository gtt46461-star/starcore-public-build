package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;import java.util.*;import com.ethk3.novaonepro.BuildConfig;

/** Original pool/writer only: at most one prepared 256-row replay slice, or one atomic completed study. */
public final class ResearchStudyTaskR30R1 {
 private ResearchStudyTaskR30R1(){}public static final String PANEL="research-study-r1";
 static boolean supported(PolicySpecR27 p){for(PolicyAstR27.Node n:PolicyAstR27.nodes(p.ast))if(n.op.equals("family"))return false;return true;}
 static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
  EvolutionLoopR27 loop=rt.evo;if(loop==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)",0,-1);EvalContextR27 ctx=loop.context();long revision=SchemaR27.liveSourceRevision(w.ro);
  if(revision<0||!SchemaR27.liveSourceTriggersValid(w.ro))return ResearchPoolR27.Outcome.idle("DEPENDENCY(SOURCE_REVISION)",0,-1);
  List<PolicySpecR27> candidates=new ArrayList<>();for(EvolutionLoopR27.Member m:new ArrayList<>(loop.population))if(supported(m.spec)){candidates.add(m.spec);if(candidates.size()==8)break;}
  for(PolicySpecR27 parent:candidates)for(String path:Arrays.asList("root/0","root/1")){
   try{DirectedResearchPolicyR30R1.withoutChild(parent,path,loop.generation);}catch(IllegalArgumentException unsupported){continue;}
   String key=StatsR27.sha256("PAIRED_STUDY_64_4_200_V1|"+ctx.data.datasetHash+'|'+parent.policyHash()+'|'+path+'|'+revision+'|'+BuildConfig.SOURCE_COMMIT_R26);
   PairedReplayRunnerR30R1 runner=null;try(Cursor c=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,key})){if(c.moveToFirst()){@SuppressWarnings("unchecked")Map<String,Object> saved=(Map<String,Object>)JsonR27.parse(c.getString(0));if("DONE".equals(saved.get("status")))continue;runner=PairedReplayRunnerR30R1.restore((String)saved.get("checkpoint"),ctx);}}
   if(runner==null){ResearchContractR30R1 contract=new ResearchContractR30R1(ctx.data.datasetHash,parent.policyHash(),FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,key,ResearchContractR30R1.Role.SELECT,ctx.data.ordinals[ctx.split.selectFrom],ctx.data.ordinals[ctx.split.selectTo-1],System.currentTimeMillis(),revision);runner=new PairedReplayRunnerR30R1(contract,parent,path,ctx,ctx.temperature(parent),loop.generation,64);}
   runner.advance(ctx,256);long now=System.currentTimeMillis();Map<String,Object> checkpoint=new LinkedHashMap<>();checkpoint.put("status",runner.complete()?"DONE":"PREPARED");checkpoint.put("checkpoint",runner.snapshot());checkpoint.put("role","SELECT");checkpoint.put("sourceRevision",revision);checkpoint.put("workCursor",runner.cursor());
   ResearchWriteQueueR27.Batch b=runner.complete()?ResearchStoreR30R1.paired(runner,now):new ResearchWriteQueueR27.Batch();b.guardBefore(db->{if(SchemaR27.liveSourceRevision(db)!=revision||!SchemaR27.liveSourceTriggersValid(db))throw new IllegalStateException("RESEARCH_STUDY_SOURCE_CHANGED");return 0;});
   b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,key,JsonR27.write(checkpoint),now);
   Long oldest=runner.complete()?null:ctx.data.ordinals[runner.cursor()];Long through=runner.cursor()==0?null:ctx.data.ordinals[runner.cursor()-1];
   ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(runner.cursor(),ctx.split.selectTo,(long)ctx.split.selectTo-runner.cursor(),oldest,through,StatsR27.sha256(runner.snapshot())));
   RuntimeR27.submit(w,"RESEARCH_STUDY",b);return ResearchPoolR27.Outcome.progress(1,runner.cursor());
  }
  return ResearchPoolR27.Outcome.idle("WAITING_LEGAL_ADDITIVE_RESEARCH_PARENT_OR_NEW_INPUT",0,-1);
 }
}
