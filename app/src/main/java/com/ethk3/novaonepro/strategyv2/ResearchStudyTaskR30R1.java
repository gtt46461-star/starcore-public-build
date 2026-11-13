package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;import java.util.*;import com.ethk3.novaonepro.BuildConfig;

/** Original pool/writer only: at most one prepared 256-row replay slice, or one atomic completed study. */
public final class ResearchStudyTaskR30R1 {
 private ResearchStudyTaskR30R1(){}public static final String PANEL="research-study-r1";
 /** Probability replay also accepts a simple policy with no removable component. */
 static boolean supported(PolicySpecR27 p){return p!=null&&!SetIntersectionPolicyR30.exact(p);}
 /** R31-B08-C03: one component of a parent and whether a same-semantics paired experiment exists for it. */
 public static final class ComponentR31{
  public final String parentHash,path,kind,status,reason;
  ComponentR31(String parent,String path,String kind,String status,String reason){parentHash=parent;this.path=path;this.kind=kind;this.status=status;this.reason=reason==null?"":reason;}
  public boolean supported(){return "SUPPORTED".equals(status);}
  public Map<String,Object> json(){Map<String,Object> m=new LinkedHashMap<>();m.put("parentHash",parentHash);m.put("path",path);m.put("kind",kind);m.put("status",status);m.put("reason",reason);return m;}
 }
 /** Component registry of one parent: AST subtrees (removal), family parameter modules, selector and calibrator. A
  * component without a same-semantics counterfactual is UNSUPPORTED_PAIR with its reason, never silently skipped. */
 public static List<ComponentR31> componentsR31(PolicySpecR27 p,int generation){
  List<ComponentR31> out=new ArrayList<>();String h=p.policyHash();
  componentPathsR31(p,p.ast,"root",generation,out);
  Set<Integer> families=new TreeSet<>();for(PolicyAstR27.Node n:PolicyAstR27.nodes(p.ast))if(n.op.equals("family"))families.add(n.ints[0]);
  for(int family:families)addComponentR31(p,"family:"+family,"FAMILY_PARAMETER_MODULE",generation,out);
  if(p.rule==PolicySpecR27.Rule.ALWAYS)out.add(new ComponentR31(h,"selector","SELECTOR","NOT_APPLICABLE","SELECTOR_ALREADY_ALWAYS"));
  else addComponentR31(p,"selector","SELECTOR",generation,out);
  if(p.fitTemperature||p.temperature!=1)addComponentR31(p,"readout","CALIBRATOR",generation,out);
  else out.add(new ComponentR31(h,"readout","CALIBRATOR","NOT_APPLICABLE","CALIBRATOR_ALREADY_IDENTITY"));
  return out;
 }
 private static void componentPathsR31(PolicySpecR27 p,PolicyAstR27.Node node,String at,int generation,List<ComponentR31> out){
  for(int i=0;i<node.kids.length;i++){String path=at+"/"+i;addComponentR31(p,path,node.op.equals("mix")?"FUSION_MEMBER":"AST_SUBTREE",generation,out);componentPathsR31(p,node.kids[i],path,generation,out);}
 }
 private static void addComponentR31(PolicySpecR27 p,String path,String kind,int generation,List<ComponentR31> out){
  try{DirectedResearchPolicyR30R1.counterfactualR31(p,path,generation);out.add(new ComponentR31(p.policyHash(),path,kind,"SUPPORTED",""));}
  catch(IllegalArgumentException unsupported){out.add(new ComponentR31(p.policyHash(),path,kind,"UNSUPPORTED_PAIR",unsupported.getMessage()));}
 }
 /** Parents studied this round: a window of 8 that rotates over the whole population, so no parent is never studied. */
 public static List<PolicySpecR27> rotationR31(List<EvolutionLoopR27.Member> population,int generation,int window){
  List<PolicySpecR27> out=new ArrayList<>();int n=population.size();if(n==0)return out;int start=(int)Math.floorMod((long)generation*window,(long)n);
  for(int i=0;i<Math.min(window,n);i++)out.add(population.get((start+i)%n).spec);return out;
 }
 static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
  EvolutionLoopR27 loop=rt.evo;if(loop==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-evolution)",0,-1);EvalContextR27 ctx=loop.context();if(ResearchInputPinR30R1.invalid(w.ro,ctx.data.datasetHash))return ResearchPoolR27.Outcome.idle("DEPENDENCY(CORRECTED_DATASET)",0,-1);long revision=ResearchInputPinR30R1.revision(w.ro,ctx.data.datasetHash);
  if(revision<0||!SchemaR27.liveSourceTriggersValid(w.ro))return ResearchPoolR27.Outcome.idle("DEPENDENCY(SOURCE_REVISION)",0,-1);
  List<PolicySpecR27> candidates=rotationR31(new ArrayList<>(loop.population),loop.generation,8);
  // R31-B08-C03: the component plan (supported and UNSUPPORTED_PAIR) is recorded once per plan identity.
  List<Object> plan=new ArrayList<>();List<ComponentR31> runnable=new ArrayList<>();
  for(PolicySpecR27 parent:candidates)for(ComponentR31 c:componentsR31(parent,loop.generation)){plan.add(c.json());if(c.supported())runnable.add(c);}
  String planJson=JsonR27.write(plan),planKey="COMPONENT_PLAN_R31:"+ctx.data.datasetHash;
  try(Cursor c=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,planKey})){if(!c.moveToFirst()||!planJson.equals(c.getString(0))){ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,planKey,planJson,System.currentTimeMillis());RuntimeR27.submit(w,"RESEARCH_STUDY_PLAN",b);return ResearchPoolR27.Outcome.progress(1,0);}}
  Map<String,PolicySpecR27> parents=new HashMap<>();for(PolicySpecR27 p:candidates)parents.put(p.policyHash(),p);
  for(ComponentR31 component:runnable)for(PairedReplayRunnerR30R1.Protocol protocol:PairedReplayRunnerR30R1.Protocol.values()){
   PolicySpecR27 parent=parents.get(component.parentHash);String path=component.path;
   String key=StatsR27.sha256("PAIRED_STUDY_64_4_200_V1|"+ctx.data.datasetHash+'|'+parent.policyHash()+'|'+path+'|'+revision+'|'+BuildConfig.SOURCE_COMMIT_R26+(protocol==PairedReplayRunnerR30R1.Protocol.RETRAIN_REMOVED?"":"|"+protocol.name()));
   PairedReplayRunnerR30R1 runner=null;try(Cursor c=w.ro.rawQuery("SELECT summary_json FROM r30r1_analysis_run WHERE run_id=? AND status IN ('REPLAY_STUDY_PREPARED','REPLAY_STUDY_COMPLETE') UNION ALL SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=? LIMIT 1",new String[]{key,PANEL,key})){if(c.moveToFirst()){@SuppressWarnings("unchecked")Map<String,Object> saved=(Map<String,Object>)JsonR27.parse(c.getString(0));if("DONE".equals(saved.get("status")))continue;runner=PairedReplayRunnerR30R1.restore((String)saved.get("checkpoint"),ctx);}}
   if(runner==null){ResearchContractR30R1 contract=new ResearchContractR30R1(ctx.data.datasetHash,parent.policyHash(),FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,key,ResearchContractR30R1.Role.SELECT,ctx.data.ordinals[ctx.split.selectFrom],ctx.data.ordinals[ctx.split.selectTo-1],System.currentTimeMillis(),revision);runner=new PairedReplayRunnerR30R1(contract,parent,path,ctx,ctx.temperature(parent),loop.generation,64,protocol);}
   runner.advance(ctx,256);long now=System.currentTimeMillis();Map<String,Object> checkpoint=new LinkedHashMap<>();checkpoint.put("status",runner.complete()?"DONE":"PREPARED");checkpoint.put("checkpoint",runner.snapshot());checkpoint.put("role","SELECT");checkpoint.put("protocol",protocol.name());checkpoint.put("loss",runner.lossR31().name());checkpoint.put("effect",runner.effectClaimR31(protocol));checkpoint.put("intervalStatus","DESCRIPTIVE_RESEARCH_ONLY");checkpoint.put("sourceRevision",revision);checkpoint.put("workCursor",runner.cursor());
   // Only the original RETRAIN_REMOVED protocol feeds the paired -> proposal -> consume chain; MASK_FROZEN is reported apart.
   if(runner.complete()&&protocol==PairedReplayRunnerR30R1.Protocol.MASK_FROZEN){ResearchGainR30R1.Estimate e=runner.gain().estimate();checkpoint.put("meanBlockGain",e.meanBlockGain);checkpoint.put("lower95",e.lower95);checkpoint.put("upper95",e.upper95);}
   ResearchWriteQueueR27.Batch b=runner.complete()&&protocol==PairedReplayRunnerR30R1.Protocol.RETRAIN_REMOVED?ResearchStoreR30R1.paired(runner,now):new ResearchWriteQueueR27.Batch();b.guardBefore(db->{ResearchInputPinR30R1.require(db,ctx.data.datasetHash,revision);return 0;});
   String saved=JsonR27.write(checkpoint),digest=StatsR27.sha256(saved),state=runner.complete()?"REPLAY_STUDY_COMPLETE":"REPLAY_STUDY_PREPARED";
   b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",key,ctx.data.datasetHash,parent.policyHash(),"SELECT",BuildConfig.SOURCE_COMMIT_R26,revision,ctx.data.ordinals[ctx.split.selectFrom],ctx.data.ordinals[ctx.split.selectTo-1],digest,state,saved,now);
   b.add("UPDATE r30r1_analysis_run SET contract_hash=?,status=?,summary_json=?,recorded_at=? WHERE run_id=? AND status IN ('REPLAY_STUDY_PREPARED','REPLAY_STUDY_COMPLETE')",digest,state,saved,now,key);
   b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,key,JsonR27.write(checkpoint),now);
   Long oldest=runner.complete()?null:ctx.data.ordinals[runner.cursor()];Long through=runner.cursor()==0?null:ctx.data.ordinals[runner.cursor()-1];
   ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(runner.cursor(),ctx.split.selectTo,(long)ctx.split.selectTo-runner.cursor(),oldest,through,StatsR27.sha256(runner.snapshot())));
   try{RuntimeR27.submit(w,"RESEARCH_STUDY",b);}catch(RuntimeException sourceChanged){if(ResearchStoreR30R1.stale(sourceChanged))return ResearchPoolR27.Outcome.idle("DEPENDENCY(RESEARCH_SOURCE_CHANGED_PREPARED_RETRY)",runner.cursor(),ctx.split.selectTo);throw sourceChanged;}return ResearchPoolR27.Outcome.progress(1,runner.cursor());
  }
  return ResearchPoolR27.Outcome.idle("WAITING_LEGAL_ADDITIVE_RESEARCH_PARENT_OR_NEW_INPUT",0,-1);
 }
}
