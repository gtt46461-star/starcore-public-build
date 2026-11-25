package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.BuildConfig;import java.util.*;

/** Generation-private research input. Reads original measures, never chart caches or formal outcomes. */
public final class ResearchContextR30R1 {
 public static final String MARK=";R1REF[";public final String datasetHash;public final long revision;
 private final Map<String,Map<String,Object>> frames=new LinkedHashMap<>();private final Map<String,String> runs=new LinkedHashMap<>();private final LinkedHashSet<String> used=new LinkedHashSet<>();
 private final Map<String,String> sourceHashesR31=new LinkedHashMap<>();
 private final Map<String,Map<String,Object>> sourceContractsR31=new LinkedHashMap<>();
 private final Map<String,String> sourceInputsR31=new LinkedHashMap<>();
 private final Map<String,Map<String,Object>> ablationRecipesR31=new LinkedHashMap<>();
 private final Map<String,TypedGenerationRuleR342> ruleByChildR342=new LinkedHashMap<>();
 private List<TypedGenerationRuleR342> typedRulesR342=Collections.emptyList();
 private long actionOrdinalR335;private ResearchContextR30R1(String dataset,long revision){datasetHash=dataset;this.revision=revision;}
 /** Bounded real generator counterfactual. Its private RNG never changes the original birth stream. */
 PolicySpecR27 proposeFamilyR31(IdeaFamilyR27 family,StatsR27.SplitMix rng,IdeaFamilyR27.Context ctx){
  long seed=rng.state;PolicySpecR27 child=family.propose(rng,ctx);List<String> sources=references(child);
  if(!sources.isEmpty()&&ablationRecipesR31.size()<2&&!ablationRecipesR31.containsKey(child.policyHash())){
   String source=sources.get(0);ResearchContextR30R1 removed=withoutSourceRunR31(source);
   IdeaFamilyR27.Context control=new IdeaFamilyR27.Context();control.generation=ctx.generation;control.maxDepth=ctx.maxDepth;control.championActRate=ctx.championActRate;control.champion=ctx.champion;control.failureCluster=ctx.failureCluster;control.archive=ctx.archive;control.legacySeeds=ctx.legacySeeds;control.leafWeight=ctx.leafWeight;control.derivedLeaves=ctx.derivedLeaves;control.evaluation=ctx.evaluation;control.research=removed;
   Map<String,Object> recipe=new LinkedHashMap<>();recipe.put("sourceRun",source);recipe.put("seed",seed);recipe.put("family",family.id());
   try{PolicySpecR27 without=family.propose(new StatsR27.SplitMix(seed),control);if(without!=null&&!references(without).contains(source))recipe.put("withoutSpec",without.fullJson());}
   catch(UnsupportedOperationException unavailable){recipe.put("state","COUNTERFACTUAL_GENERATOR_UNSUPPORTED");}
   ablationRecipesR31.put(child.policyHash(),recipe);
  }
  return child;
 }
 String stageBoundR31(ResearchWriteQueueR27.Batch batch,PolicySpecR27 child,EvalContextR27 ctx,long budget,long now){
  if(child!=null&&ruleByChildR342.containsKey(child.policyHash()))return stageRuleTransferR342(batch,child,ctx,now);
  List<String> refs=references(child);if(refs.isEmpty())return null;
  Map<String,Object> action=new LinkedHashMap<>(),views=new LinkedHashMap<>(),hashes=new LinkedHashMap<>(),contracts=new LinkedHashMap<>(),inputs=new LinkedHashMap<>();
  for(String run:refs){String view=null;for(Map.Entry<String,String> entry:runs.entrySet())if(run.equals(entry.getValue())){view=entry.getKey();break;}if(view==null||!sourceHashesR31.containsKey(run)||!sourceContractsR31.containsKey(run))return stage(batch,child,datasetHash,revision,now);views.put(run,view);hashes.put(run,sourceHashesR31.get(run));contracts.put(run,sourceContractsR31.get(run));inputs.put(run,sourceInputsR31.get(run));}
  action.put("version",ResearchConsumptionStoreR31.VERSION);action.put("datasetHash",datasetHash);action.put("sourceRevision",revision);action.put("sourceViews",views);action.put("sourceHashes",hashes);action.put("sourceContracts",contracts);action.put("sourceInputs",inputs);action.put("target","POSITION:"+child.position);action.put("decisionOrdinal",Math.addExact(ctx.data.ordinals[ctx.data.size()-1],1));action.put("budget",budget);action.put("preparedAt",now);
  action.put("beforeHash",child.meta!=null&&!child.meta.parents.isEmpty()?child.meta.parents.get(0):StatsR27.sha256("R31_ABSENT_CANDIDATE_BEFORE_BIRTH|"+child.policyHash()));
  List<Object> usages=new ArrayList<>();for(Map<String,Object> usage:usagesR31())if(child.policyHash().equals(usage.get("childHash")))usages.add(new LinkedHashMap<>(usage));action.put("usages",usages);
  List<Object> ablations=new ArrayList<>();Map<String,Object> recipe=ablationRecipesR31.get(child.policyHash());if(recipe!=null)ablations.add(ResearchConsumptionStoreR31.ablation(child,recipe,ctx,budget));action.put("ablations",ablations);action.put("ablationState",recipe==null?"UNSUPPORTED_SOURCE_REPLAY_ADAPTER_OR_ROUND_BUDGET":"RECORDED_SELECT_COUNTERFACTUAL");
  return stage(batch,child,datasetHash,revision,now,action);
 }
 @SuppressWarnings("unchecked") public static ResearchContextR30R1 read(SQLiteDatabase db,EvalContextR27 context){
  long revision=ResearchInputPinR30R1.revision(db,context.data.datasetHash);
  ResearchContextR30R1 result=new ResearchContextR30R1(context.data.datasetHash,revision);
  result.actionOrdinalR335=Math.addExact(context.data.ordinals[context.data.size()-1],1);
  String sql="SELECT m.measure_key,m.payload_json,r.run_id,r.summary_json,r.contract_hash FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE (r.role='SELECT' OR (r.role='TRAIN' AND m.measure_key='R08' AND m.kind='BEHAVIOR_DIVERSITY')) AND r.status='COMMITTED' AND r.dataset_hash=? AND r.source_revision=? AND r.source_commit=? AND ("+ResearchInputPinR30R1.validRunSql("r")+") AND (m.kind IN ('STRUCTURE_STATE','STRUCTURE_FORECAST_ERROR','POSITION_COMPATIBILITY','MOLECULE_SYNERGY','DRIFT_REPLAY_VALUE','OPERATOR_GAIN_COST','SET_INTERSECTION_SUPPORT','FAILURE_PRESCRIPTION','FUSION_COMPLEMENTARITY','CALIBRATION_SUPPORT','EXPERT_EXECUTABLE_CREDIT','RESEARCH_BUDGET','FAILED_MECHANISM_MEMORY','BEHAVIOR_DIVERSITY') OR m.kind LIKE 'MATRIX_%') AND r.run_id=(SELECT nr.run_id FROM r30r1_research_measure nm JOIN r30r1_analysis_run nr ON nr.run_id=nm.run_id WHERE nm.measure_key=m.measure_key AND nr.dataset_hash=r.dataset_hash AND nr.source_revision=r.source_revision AND nr.source_commit=r.source_commit AND nr.role=r.role AND nr.status='COMMITTED' ORDER BY nr.recorded_at DESC,nr.run_id DESC LIMIT 1) ORDER BY r.recorded_at DESC,r.run_id LIMIT 64";
  try(Cursor rows=db.rawQuery(sql,new String[]{context.data.datasetHash,Long.toString(revision),BuildConfig.SOURCE_COMMIT_R26})){
   while(rows.moveToNext()){
    String view=rows.getString(0),original=rows.getString(1),run=rows.getString(2);
    if(result.frames.containsKey(view)||ResearchActionPolicyR33.READ_ONLY.contains(view))continue;
    try{
     if(!ResearchStoreR30R1.currentSource(BuildConfig.SOURCE_COMMIT_R26,rows.getString(3)))continue;
     Object parsed=JsonR27.parse(original);
     if(!(parsed instanceof Map))throw new IllegalArgumentException("ORIGINAL_RESEARCH_MEASURE_TYPE");
     Map<String,Object> source=(Map<String,Object>)parsed;
     Object body=source.get("frame"),contract=source.get("sourceContract");
     if(!(body instanceof Map)||!(contract instanceof Map)||!original.equals(rows.getString(3))
        ||!StatsR27.sha256(JsonR27.write(body)).equals(rows.getString(4)))
      throw new IllegalArgumentException("ORIGINAL_RESEARCH_MEASURE_HASH");
     Map<String,Object> frame=(Map<String,Object>)body;
     if(!("R08".equals(view)?"TRAIN":"SELECT").equals(frame.get("role"))||!context.data.datasetHash.equals(frame.get("datasetHash")))continue;
     if(Boolean.FALSE.equals(frame.get("usableResearchProduct")))continue;
     result.frames.put(view,frame);result.runs.put(view,run);
     result.sourceHashesR31.put(run,StatsR27.sha256(original));
     result.sourceContractsR31.put(run,new LinkedHashMap<>((Map<String,Object>)contract));
     result.sourceInputsR31.put(run,String.valueOf(source.get("inputHash")));
    }catch(IllegalArgumentException invalid){
     result.isolatedSourcesR341.put(view,run+":"+invalid.getMessage());
    }
   }
  }
  if(context.split.selectTo>context.split.trainFrom){
   long start=context.data.ordinals[context.split.trainFrom];
   result.typedRulesR342=AtomPublicationStoreR31.readRulesR342(db,context.data.datasetHash,start);
  }
  return result;
 }

 List<PolicySpecR27> proposeRuleR342(EvalContextR27 context,int generation,int capacity){
  if(capacity<2||typedRulesR342.isEmpty()||context.split.selectTo<=context.split.trainFrom)return Collections.emptyList();
  long from=context.data.ordinals[context.split.trainFrom], through=context.data.ordinals[context.split.selectTo-1];
  TypedGenerationRuleR342 rule=typedRulesR342.get(0);
  List<PolicySpecR27> variants=rule.instantiate(datasetHash,from,through,generation);
  for(PolicySpecR27 variant:variants)ruleByChildR342.put(variant.policyHash(),rule);
  return variants;
 }

 private String stageRuleTransferR342(ResearchWriteQueueR27.Batch batch,PolicySpecR27 child,EvalContextR27 ctx,long now){
  TypedGenerationRuleR342 rule=ruleByChildR342.get(child.policyHash());
  long from=ctx.data.ordinals[ctx.split.trainFrom], through=ctx.data.ordinals[ctx.split.selectTo-1];
  String original=rule.encodeCanonical();
  String run=HashingSU1.fields("R342_RULE_TRANSFER",rule.ruleId,datasetHash,Long.toString(revision),
          Long.toString(from),Long.toString(through),BuildConfig.SOURCE_COMMIT_R26);
  String proposal=HashingSU1.fields("R342_RULE_PROPOSAL",run,child.policyHash());
  Map<String,Object> source=new LinkedHashMap<>();
  source.put("sourceContract",Collections.singletonMap("abi",rule.abi));
  source.put("ruleId",rule.ruleId);source.put("discoveryDataset",rule.dataset);source.put("transferDataset",datasetHash);
  source.put("discoveryThrough",rule.through);source.put("transferFrom",from);source.put("transferThrough",through);
  String summary=JsonR27.write(source);
  Map<String,Object> product=new LinkedHashMap<>();
  product.put("candidate",child.fullJson());product.put("candidateHash",child.policyHash());
  product.put("typedRule",rule.ruleId);product.put("ruleTransferDataset",datasetHash);
  String payload=JsonR27.write(product);
  batch.guardBefore(db->{
   if(ResearchInputPinR30R1.admittedRevisionR341(db,rule.dataset)!=rule.revision
      ||ResearchInputPinR30R1.admittedRevisionR341(db,datasetHash)!=revision)throw new ResearchStoreR30R1.StaleResearch();
   try(Cursor saved=db.rawQuery("SELECT canonical FROM "+AtomPublicationStoreR31.RULES_R342+" WHERE rule_id=?",new String[]{rule.ruleId})){
    if(!saved.moveToFirst()||!original.equals(saved.getString(0)))throw new ResearchStoreR30R1.StaleResearch();
   }
   return 0;
  });
  batch.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
          run,datasetHash,rule.parents().get(0),"SELECT",BuildConfig.SOURCE_COMMIT_R26,revision,from,through,
          StatsR27.sha256(summary),"COMMITTED",summary,now);
  batch.add("INSERT OR IGNORE INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",
          proposal,run,rule.parents().get(0),"TYPED_GENERATION_RULE",payload,StatsR27.sha256(payload),"READY",
          "Original typed rule applied to later independent development input",now);
  return proposal;
 }
 private final Map<String,String> isolatedSourcesR341=new LinkedHashMap<>();
 Map<String,String> isolatedSourcesR341(){return Collections.unmodifiableMap(isolatedSourcesR341);}

 public void begin(){used.clear();}void use(String view){String run=runs.get(view);if(run!=null)used.add(run);}
 @SuppressWarnings("unchecked") public int window(StatsR27.SplitMix r,int fallback){
  if(r.chance(.25))return fallback;
  Map<String,Object> errors=frames.get("R02");List<Integer> supported=new ArrayList<>();
  if(errors!=null&&errors.get("cells") instanceof List)for(Object o:(List<?>)errors.get("cells"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;Object lower=row.get("lower95"),w=row.get("window");if(lower instanceof Number&&Double.isFinite(((Number)lower).doubleValue())&&((Number)lower).doubleValue()>0&&w instanceof Number&&!supported.contains(((Number)w).intValue()))supported.add(((Number)w).intValue());}
  if(!supported.isEmpty()){use("R02");return supported.get(r.nextInt(supported.size()));}
  Map<String,Object> structure=frames.get("R01");if(structure==null||!(structure.get("cells") instanceof List))return fallback;
  Set<Integer> allowed=new HashSet<>();for(int w:FeatureCacheR27.WINDOWS)allowed.add(w);List<ResearchActionPolicyR33.Window> rows=new ArrayList<>();
  for(Object o:(List<?>)structure.get("cells"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;Object pos=row.get("position"),w=row.get("window"),n=row.get("supportCalendarIssues"),v=row.get("value");if(pos instanceof Number&&w instanceof Number&&n instanceof Number&&v instanceof Number)rows.add(new ResearchActionPolicyR33.Window(((Number)pos).intValue(),((Number)w).intValue(),((Number)n).longValue(),((Number)v).doubleValue()));}
  ResearchActionPolicyR33.Window chosen=ResearchActionPolicyR33.structuralWindow(rows,allowed,200,r.nextDouble());if(chosen==null)return fallback;use("R01");return chosen.size;
 }
 /** Exact K3 source-window hypothesis. Coverage is support, never proof of predictive advantage. */
 public int[] intersectionHintR335(StatsR27.SplitMix r,int target){Map<String,Object> f=frames.get("R04");if(f==null||r.chance(.25)||!(f.get("points") instanceof List))return null;List<int[]> choices=new ArrayList<>();
  for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;Object p=row.get("position"),a=row.get("shortWindow"),b=row.get("longWindow"),n=row.get("n"),acts=row.get("intersectionActs");if(p instanceof Number&&((Number)p).intValue()==target&&a instanceof Number&&b instanceof Number&&n instanceof Number&&((Number)n).longValue()>=200&&acts instanceof Number&&((Number)acts).longValue()>0){int sw=((Number)a).intValue(),lw=((Number)b).intValue();if(sw>0&&sw<lw&&lw<=2048)choices.add(new int[]{sw,lw});}}
  if(choices.isEmpty())return null;use("R04");return choices.get(r.nextInt(choices.size()));
 }
 @SuppressWarnings("unchecked") public int[] cross(StatsR27.SplitMix r,int target){Map<String,Object> frame=frames.get("MATRIX:CROSS_POSITION_FLOW");boolean matrix=frame!=null;if(frame==null)frame=frames.get("R03");if(frame==null||r.chance(.25))return null;Object raw=matrix?frame:frame.get("directedLag");if(!(raw instanceof Map))return null;Map<String,Object> tensor=(Map<String,Object>)raw;List<?> values=(List<?>)tensor.get("values"),support=(List<?>)tensor.get("support");if(values==null||support==null||target<1||target>10)return null;List<int[]> eligible=new ArrayList<>();List<?> row=(List<?>)values.get(target-1),n=(List<?>)support.get(target-1);for(int source=1;source<=10;source++)for(int k=0;k<2;k++){int at=(source-1)*4+k;if(row.get(at) instanceof Number&&n.get(at) instanceof Number&&((Number)n.get(at)).longValue()>=200&&((Number)row.get(at)).doubleValue()>0)eligible.add(new int[]{source,k+1});}if(eligible.isEmpty())return null;use(matrix?"MATRIX:CROSS_POSITION_FLOW":"R03");return eligible.get(r.nextInt(eligible.size()));}
 @SuppressWarnings("unchecked") public PolicyAstR27.Node molecule(StatsR27.SplitMix r){Map<String,Object> frame=frames.get("R07");if(frame==null||r.chance(.25))return null;for(Object o:(List<?>)frame.get("points")){Map<String,Object> p=(Map<String,Object>)o;if(p.get("lower") instanceof Number&&((Number)p.get("lower")).doubleValue()>0&&p.get("parent") instanceof String){PolicySpecR27 parent=PolicySpecR27.parse((String)p.get("parent"));if(parent.ast.depth()<=3&&parent.ast.size()<=16){use("R07");return parent.ast;}}}return null;}
 private Map<String,Object> budgetReceipt;
 @SuppressWarnings("unchecked") private Map<String,Object> budgetFrame(){Map<String,Object> matrix=frames.get("MATRIX:EVOLUTION_CREDIT");if(matrix!=null&&matrix.get("details") instanceof Map){Object rows=((Map<?,?>)matrix.get("details")).get("pairedChildren");if(rows instanceof List){Map<String,Object> f=new LinkedHashMap<>();f.put("points",rows);return f;}}return frames.get("R10");}
 private String budgetRun(){return runs.containsKey("MATRIX:EVOLUTION_CREDIT")?runs.get("MATRIX:EVOLUTION_CREDIT"):runs.get("R10");}
 @SuppressWarnings("unchecked") public double[] weights(double[] prior){
  Map<String,Object> f=budgetFrame();if(f==null||!(f.get("points") instanceof List))return prior;
  Map<String,Double> priors=new LinkedHashMap<>();double[] fixed=MutationOpsR27.defaultWeights();for(int i=0;i<MutationOpsR27.OPS.length;i++)priors.put(MutationOpsR27.OPS[i],fixed[i]);
  List<ResearchActionPolicyR33.Credit> credits=new ArrayList<>();List<Object> evidence=new ArrayList<>();
  for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<String,Object> row=(Map<String,Object>)o;Object lo=row.get("lower"),hi=row.get("upper"),n=row.get("n"),a=row.get("parentHash"),b=row.get("childHash");if(lo instanceof Number&&hi instanceof Number&&n instanceof Number&&((Number)n).longValue()>=200&&Double.isFinite(((Number)lo).doubleValue())&&Double.isFinite(((Number)hi).doubleValue())&&(((Number)lo).doubleValue()>0||((Number)hi).doubleValue()<0)&&a instanceof String&&b instanceof String&&((String)a).matches("[0-9a-f]{64}")&&((String)b).matches("[0-9a-f]{64}")){
   credits.add(new ResearchActionPolicyR33.Credit(String.valueOf(row.get("operator")),a+"|"+b+"|"+datasetHash+"|"+revision,datasetHash,((Number)n).longValue(),((Number)lo).doubleValue(),((Number)hi).doubleValue()));evidence.add(row);}}
  if(credits.isEmpty())return prior;Map<String,Double> weights=ResearchActionPolicyR33.weights(priors,credits,datasetHash,200,.25);double[] result=new double[fixed.length];for(int i=0;i<result.length;i++)result[i]=weights.get(MutationOpsR27.OPS[i]);
  budgetReceipt=new LinkedHashMap<>();budgetReceipt.put("version","RESEARCH_PAIRED_BUDGET2_R335");budgetReceipt.put("role","SELECT");budgetReceipt.put("sourceRun",budgetRun());budgetReceipt.put("beforeWeights",prior.clone());budgetReceipt.put("afterWeights",result.clone());budgetReceipt.put("fixedPriors",fixed);budgetReceipt.put("explorationMass",.25);budgetReceipt.put("evidence",evidence);budgetReceipt.put("permanentFamilyDeletion",false);return result;
 }
 @SuppressWarnings("unchecked") public int familyCredit(String family){Map<String,Object> f=budgetFrame();if(f==null)return 0;Set<String> identities=new HashSet<>();for(Object o:(List<?>)f.get("points")){Map<String,Object> p=(Map<String,Object>)o;if(family.equals(p.get("family"))&&p.get("lower") instanceof Number&&((Number)p.get("lower")).doubleValue()>0)identities.add(String.valueOf(p.get("childHash")));}return identities.size();}
 private Map<String,Object> familyBudgetR335,expertBudgetR335;
 private Map<String,Double> expertWeightsR335;
 public Map<String,Double> expertWeightsR335(){return expertWeightsR335==null?Collections.<String,Double>emptyMap():expertWeightsR335;}
 private ResearchActionPolicyR33.Source actionSourceR335(String view){String run=runs.get(view);if(run==null)return null;Map<String,Object> c=sourceContractsR31.get(run);if(c==null)return null;return new ResearchActionPolicyR33.Source(view,run,datasetHash,String.valueOf(c.get("abi")),String.valueOf(c.get("source")),String.valueOf(c.get("role")),sourceInputsR31.get(run),sourceHashesR31.get(run),revision,((Number)c.get("through")).longValue(),((Number)c.get("available")).longValue());}
 public int mechanismCreditR335(String family){Map<String,Object> f=frames.get("R08");if(f==null||!(f.get("points") instanceof List))return 0;Set<String> signatures=new HashSet<>();for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;if(family.equals(row.get("idea"))&&row.get("signatureHash") instanceof String&&row.get("n") instanceof Number&&((Number)row.get("n")).longValue()>0)signatures.add(String.valueOf(row.get("position"))+"|"+row.get("signatureHash"));}return Math.min(4,signatures.size());}
 public void familyBudgetR335(Map<String,Integer> before,Map<String,Integer> after){if(!frames.containsKey("R08"))return;familyBudgetR335=new LinkedHashMap<>();familyBudgetR335.put("before",new TreeMap<>(before));familyBudgetR335.put("after",new TreeMap<>(after));}
 @SuppressWarnings("unchecked") public String expertR335(StatsR27.SplitMix rng){Map<String,Object> f=frames.get("R09");if(f==null||!(f.get("points") instanceof List))return ExpertProposalsR27.EXPERTS[rng.nextInt(16)][0];
  if(expertWeightsR335==null){Map<String,Double> prior=new LinkedHashMap<>();for(String[] e:ExpertProposalsR27.EXPERTS)prior.put(e[0],1.0);List<ResearchActionPolicyR33.Credit> credits=new ArrayList<>();for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;Object n=row.get("n"),lo=row.get("lower"),hi=row.get("upper"),a=row.get("parentHash"),b=row.get("childHash");if(n instanceof Number&&((Number)n).longValue()>=200&&lo instanceof Number&&hi instanceof Number&&Double.isFinite(((Number)lo).doubleValue())&&Double.isFinite(((Number)hi).doubleValue())&&(((Number)lo).doubleValue()>0||((Number)hi).doubleValue()<0)&&a instanceof String&&b instanceof String)credits.add(new ResearchActionPolicyR33.Credit(String.valueOf(row.get("expert")),a+"|"+b+"|"+datasetHash+"|"+revision,datasetHash,((Number)n).longValue(),((Number)lo).doubleValue(),((Number)hi).doubleValue()));}
   if(credits.isEmpty())return ExpertProposalsR27.EXPERTS[rng.nextInt(16)][0];expertWeightsR335=ResearchActionPolicyR33.weights(prior,credits,datasetHash,200,.25);expertBudgetR335=new LinkedHashMap<>();expertBudgetR335.put("before",Collections.emptyMap());expertBudgetR335.put("after",expertWeightsR335);
  }
  double pick=rng.nextDouble(),sum=0;for(Map.Entry<String,Double> e:expertWeightsR335.entrySet()){sum+=e.getValue();if(pick<sum)return e.getKey();}return expertWeightsR335.keySet().iterator().next();
 }
 public void appendBudgetReceipt(ResearchWriteQueueR27.Batch b,int generation,long from,long through){
  if(budgetReceipt!=null){String run=(String)budgetReceipt.get("sourceRun");b.guardBefore(db->{ResearchInputPinR30R1.require(db,datasetHash,revision);return 0;});b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,"BUDGET_APPLIED_GENERATION:"+generation,"RESEARCH_BUDGET",StatsR27.sha256("ORIGINAL_EVOLUTION_BUDGET"),"ORIGINAL_EVOLUTION_CURSOR_SAME_TRANSACTION",from,through,0,null,null,null,JsonR27.write(budgetReceipt));appendAppliedR335(b,generation,through,viewForRunR335(run),"OP_WEIGHTS",budgetReceipt.get("beforeWeights"),budgetReceipt.get("afterWeights"));}
  if(familyBudgetR335!=null)appendAppliedR335(b,generation,through,"R08","FAMILY_ALLOCATION",familyBudgetR335.get("before"),familyBudgetR335.get("after"));
  if(expertBudgetR335!=null)appendAppliedR335(b,generation,through,"R09","EXPERT_WEIGHTS",expertBudgetR335.get("before"),expertBudgetR335.get("after"));
 }
 private String viewForRunR335(String run){for(Map.Entry<String,String> e:runs.entrySet())if(e.getValue().equals(run))return e.getKey();throw new IllegalStateException("R335_BUDGET_SOURCE_RUN_MISSING");}
 private void appendAppliedR335(ResearchWriteQueueR27.Batch batch,int generation,long through,String view,String object,Object before,Object after){
  String a=JsonR27.write(before),z=JsonR27.write(after);if(a.equals(z))return;
  if(view.equals("R10")&&!runs.containsKey("R10")&&runs.containsKey("MATRIX:EVOLUTION_CREDIT"))view="MATRIX:EVOLUTION_CREDIT";
  ResearchActionPolicyR33.Source source=actionSourceR335(view);if(source==null)return;
  ResearchActionStoreR33.Plan plan=new ResearchActionStoreR33.Plan(source,"BUDGET_APPLIED",RuntimeR27.LOOP+"|GEN:"+generation+"|"+object,a,z,1,actionOrdinalR335,System.currentTimeMillis(),datasetHash,revision);
  ResearchActionStoreR33.append(batch,plan,db->allocationReadbackR335(db,object));
 }
 private static String allocationReadbackR335(SQLiteDatabase db,String object){
  try(Cursor q=db.rawQuery("SELECT snapshot FROM r27_evolution_cursor WHERE loop IN (?,?) ORDER BY CASE loop WHEN ? THEN 0 ELSE 1 END LIMIT 1",new String[]{RuntimeR27.LOOP,"MAIN",RuntimeR27.LOOP})){
   if(!q.moveToFirst())return JsonR27.write("OP_WEIGHTS".equals(object)?MutationOpsR27.defaultWeights():Collections.emptyMap());
   Map<?,?> snapshot=(Map<?,?>)JsonR27.parse(EvolutionPublicationStoreR31.resolveSnapshot(db,q.getString(0)));Object value;
   if("OP_WEIGHTS".equals(object))value=snapshot.get("opWeights");
   else if("FAMILY_ALLOCATION".equals(object)){Object creation=snapshot.get("creationBudgetR31");value=creation instanceof Map?((Map<?,?>)creation).get("lastAllocation"):Collections.emptyMap();}
   else {Object stats=snapshot.get("genStats");value=stats instanceof Map?((Map<?,?>)stats).get("expertWeightsR335"):Collections.emptyMap();}
   return JsonR27.write(value==null?Collections.emptyMap():value);
  }
 }
 @SuppressWarnings("unchecked") public PolicySpecR27 measuredPrune(PolicySpecR27 parent,int generation){Map<String,Object> matrix=frames.get("MATRIX:COUNTER_EVIDENCE");if(matrix==null||!(matrix.get("details") instanceof Map))return pruneNegativeMemoryR335(parent,generation);Object receipts=((Map<?,?>)matrix.get("details")).get("sourceReceipts");if(!(receipts instanceof List))return pruneNegativeMemoryR335(parent,generation);for(Object o:(List<?>)receipts){Map<String,Object> p=(Map<String,Object>)o;if(!(p.get("parent") instanceof String)||!(p.get("upper95") instanceof Number)||((Number)p.get("upper95")).doubleValue()>=0)continue;PolicySpecR27 measuredParent=PolicySpecR27.parse((String)p.get("parent"));if(!parent.policyHash().equals(measuredParent.policyHash()))continue;PolicySpecR27 child=DirectedResearchPolicyR30R1.counterfactualR31(parent,(String)p.get("componentPath"),generation);if(!child.policyHash().equals(p.get("counterfactualHash")))throw new IllegalStateException("COUNTER_MATRIX_CHILD_SUBSTITUTION");Map<String,Object> causal=frames.get("MATRIX:CAUSAL_SUPPORT");if(causal!=null){int row=axis(causal,"rows").indexOf(parent.policyHash()+":"+p.get("componentPath"));if(row>=0){if(cell(causal,row,0)!=1||cell(causal,row,1)!=1||cell(causal,row,2)<4)return null;use("MATRIX:CAUSAL_SUPPORT");}}use("MATRIX:COUNTER_EVIDENCE");if(p.get("sourceRun") instanceof String)used.add(ResearchContractR30R1.sha((String)p.get("sourceRun")));return annotate(child);}return pruneNegativeMemoryR335(parent,generation);}
 /** R17 reuses only the exact measured parent/component/input. The negative sign refers to the
  * WITH component arm, so the real mutation is its verified removal; the better removal arm is never skipped. */
 private PolicySpecR27 pruneNegativeMemoryR335(PolicySpecR27 parent,int generation){Map<String,Object> frame=frames.get("R17");if(frame==null||!(frame.get("points") instanceof List))return null;
  for(Object raw:(List<?>)frame.get("points"))if(raw instanceof Map){Map<?,?> row=(Map<?,?>)raw;if(!parent.policyHash().equals(row.get("parentHash"))||!(row.get("negativeContribution") instanceof Map))continue;Map<?,?> negative=(Map<?,?>)row.get("negativeContribution");Object source=negative.get("sourceContract");if(!(source instanceof Map))continue;Map<?,?> c=(Map<?,?>)source;Object n=row.get("n"),upper=row.get("upper");
   if(!datasetHash.equals(c.get("dataset"))||!(c.get("revision") instanceof Number)||((Number)c.get("revision")).longValue()!=revision||!FrozenConfirmationR30.currentSemantics().equals(c.get("abi"))||!BuildConfig.SOURCE_COMMIT_R26.equals(c.get("source"))||!"SELECT".equals(c.get("role"))||!parent.policyHash().equals(c.get("policy"))||!(n instanceof Number)||((Number)n).longValue()<200||!(upper instanceof Number)||!Double.isFinite(((Number)upper).doubleValue())||((Number)upper).doubleValue()>=0||!(negative.get("completeBlocks") instanceof Number)||((Number)negative.get("completeBlocks")).longValue()<4||!(negative.get("missingIssues") instanceof Number)||((Number)negative.get("missingIssues")).longValue()!=0||!(negative.get("incompleteFullBlocks") instanceof Number)||((Number)negative.get("incompleteFullBlocks")).longValue()!=0)continue;
   Object path=negative.get("componentPath");if(!(path instanceof String)||!ResearchContractR30R1.hash(parent.policyHash()+"|"+path).equals(negative.get("mechanismHash")))continue;
   PolicySpecR27 child=DirectedResearchPolicyR30R1.counterfactualR31(parent,(String)path,generation);if(!child.policyHash().equals(negative.get("counterfactualHash"))||child.policyHash().equals(parent.policyHash())||child.position!=parent.position||!FailureProfileR30.legal(child))throw new IllegalStateException("R335_NEGATIVE_MEMORY_COUNTERFACTUAL_CHANGED");
   use("R17");return annotate(child);
  }return null;
 }

 @SuppressWarnings("unchecked") private List<?> axis(Map<String,Object> m,String key){Object x=m.get(key);return x instanceof List?(List<?>)x:Collections.emptyList();}
 @SuppressWarnings("unchecked") private Map<String,Object> details(Map<String,Object> m){Object x=m.get("details");return x instanceof Map?(Map<String,Object>)x:Collections.emptyMap();}
 private double cell(Map<String,Object> m,int row,int column){List<?> rows=axis(m,"values");if(row<0||row>=rows.size())return Double.NaN;List<?> values=(List<?>)rows.get(row);if(column<0||column>=values.size()||!(values.get(column) instanceof Number))return Double.NaN;return ((Number)values.get(column)).doubleValue();}
 private long support(Map<String,Object> m,int row,int column){List<?> rows=axis(m,"support");if(row<0||row>=rows.size())return 0;List<?> values=(List<?>)rows.get(row);return column>=0&&column<values.size()&&values.get(column) instanceof Number?((Number)values.get(column)).longValue():0;}
 public int position(StatsR27.SplitMix r,int fallback){Map<String,Object> m=frames.get("MATRIX:NUMBER_POSITION_COMPATIBILITY");if(m==null||r.chance(.25))return fallback;double best=-1;List<Integer> positions=new ArrayList<>();for(int p=0;p<10;p++){double deviation=0;boolean known=true;for(int k=0;k<10;k++){double value=cell(m,k,p);if(!Double.isFinite(value)||support(m,k,p)<200){known=false;break;}deviation+=(value-.1)*(value-.1);}if(known){if(deviation>best+1e-12){positions.clear();best=deviation;}if(Math.abs(deviation-best)<1e-12)positions.add(p+1);}}if(positions.isEmpty()||best<=1e-12)return fallback;/* R31-B08-C04: a flat (no-signal) frame is no product, not a random consumption */use("MATRIX:NUMBER_POSITION_COMPATIBILITY");return positions.get(r.nextInt(positions.size()));}
 public List<PolicySpecR27> fusionParents(List<PolicySpecR27> archive,StatsR27.SplitMix r){String kind=frames.containsKey("MATRIX:ALGORITHM_COMPLEMENTARITY")&&(!frames.containsKey("MATRIX:TEACHER_DIVERSITY")||r.chance(.5))?"MATRIX:ALGORITHM_COMPLEMENTARITY":"MATRIX:TEACHER_DIVERSITY";Map<String,Object> m=frames.get(kind);if(r.chance(.25))return archive;if(m==null)return fusionFrameParentsR335(archive,r);Map<String,PolicySpecR27> policies=new HashMap<>();for(PolicySpecR27 p:archive)policies.put(p.policyHash(),p);List<?> rows=axis(m,"rows"),cols=axis(m,"columns");List<List<PolicySpecR27>> eligible=new ArrayList<>();for(int i=0;i<rows.size();i++)for(int j=0;j<cols.size();j++){if(i==j)continue;double correlation=cell(m,i,j);PolicySpecR27 a=policies.get(String.valueOf(rows.get(i))),b=policies.get(String.valueOf(cols.get(j)));if(a!=null&&b!=null&&a.position==b.position&&support(m,i,j)>=200&&Double.isFinite(correlation)&&(kind.equals("MATRIX:TEACHER_DIVERSITY")?correlation>.01:correlation>.2))eligible.add(Arrays.asList(a,b));}if(eligible.isEmpty())return fusionFrameParentsR335(archive,r);use(kind);return eligible.get(r.nextInt(eligible.size()));}
 private List<PolicySpecR27> fusionFrameParentsR335(List<PolicySpecR27> archive,StatsR27.SplitMix r){Map<String,Object> f=frames.get("R12");if(f==null||!(f.get("points") instanceof List))return archive;Map<String,PolicySpecR27> byHash=new HashMap<>();for(PolicySpecR27 p:archive)byHash.put(p.policyHash(),p);List<List<PolicySpecR27>> eligible=new ArrayList<>();for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;PolicySpecR27 a=byHash.get(row.get("parentA")),b=byHash.get(row.get("parentB"));Object n=row.get("n"),corr=row.get("residualCorrelation");if(a!=null&&b!=null&&!a.policyHash().equals(b.policyHash())&&a.position==b.position&&n instanceof Number&&((Number)n).longValue()>=200&&corr instanceof Number&&Double.isFinite(((Number)corr).doubleValue())&&Math.abs(((Number)corr).doubleValue())<=1&&Math.abs(((Number)corr).doubleValue())<FusionOpsR27.MAX_CORRELATION)eligible.add(Arrays.asList(a,b));}if(eligible.isEmpty())return archive;use("R12");return eligible.get(r.nextInt(eligible.size()));}
 public PolicySpecR27 premiseHypothesis(IdeaFamilyR27.Context ctx,StatsR27.SplitMix r){if(r.chance(.25))return null;String source=frames.containsKey("MATRIX:FORWARD_REASONING")?"MATRIX:FORWARD_REASONING":"MATRIX:EVIDENCE_TEACHER";Map<String,Object> m=frames.get(source);if(m==null)return null;Object programs=details(m).get(source.equals("MATRIX:FORWARD_REASONING")?"programsAndPremises":"teacherPrograms");if(!(programs instanceof List)||((List<?>)programs).isEmpty())return null;int index=r.nextInt(((List<?>)programs).size());PolicySpecR27 base=PolicySpecR27.parse(String.valueOf(((List<?>)programs).get(index)));if(ctx.champion!=null&&base.position!=ctx.champion.position)return null;if(source.equals("MATRIX:FORWARD_REASONING")&&(cell(m,index,0)!=1||cell(m,index,1)<.8||cell(m,index,2)<.9))return null;
  Map<String,Object> contradiction=frames.get("MATRIX:CONTRADICTION"),invariance=frames.get("MATRIX:INVARIANCE"),reverse=frames.get("MATRIX:REVERSE_REASONING");int at=contradiction==null?-1:axis(contradiction,"rows").indexOf(base.policyHash());if(at>=0&&(cell(contradiction,at,0)!=0||cell(contradiction,at,2)>0))return null;int transform=invariance==null?-1:axis(invariance,"rows").indexOf(base.policyHash());if(transform>=0&&(cell(invariance,transform,0)<1-1e-12||cell(invariance,transform,1)<1-1e-12))return null;if(reverse==null||!Integer.valueOf(base.position).equals(details(reverse).get("targetPosition") instanceof Number?((Number)details(reverse).get("targetPosition")).intValue():null))return null;int src=((Number)details(reverse).get("sourcePosition")).intValue();double nonuniform=0;for(int y=0;y<10;y++)for(int x=0;x<10;x++)if(support(reverse,y,x)>=20){double value=cell(reverse,y,x);if(Double.isFinite(value))nonuniform+=Math.abs(value-.1);}if(nonuniform<=.1)return null;
  PolicyAstR27.Node ast=PolicyAstR27.op("mix",.5,PolicyAstR27.op("zscore",base.ast),PolicyAstR27.op("zscore",PolicyAstR27.leaf("crosscond",src)));PolicySpecR27 child=new PolicySpecR27(ast,base.position,base.rule,base.threshold,base.rate,1,true,base.lr,base.forget,IdeaFamilyR27.meta("I10","RESEARCH_PREMISE_HYPOTHESIS",ctx.generation,r.state,"Executable premise/reverse-association hypothesis; research screening only, independent confirmation required",base.policyHash()));if(!FailureProfileR30.legal(child)||ast.depth()>ctx.maxDepth)return null;use(source);use("MATRIX:REVERSE_REASONING");if(at>=0)use("MATRIX:CONTRADICTION");if(transform>=0)use("MATRIX:INVARIANCE");return annotate(child);}
 public PolicySpecR27 calibrationHypothesis(PolicySpecR27 base,IdeaFamilyR27.Context ctx,StatsR27.SplitMix r){Map<String,Object> feedback=frames.get("MATRIX:FEEDBACK"),error=frames.get("MATRIX:STUDENT_ERROR");if(feedback==null||error==null)return calibrationFrameR335(base,ctx,r);if(r.chance(.25)||!base.policyHash().equals(feedback.get("policyHash"))||!base.policyHash().equals(error.get("policyHash"))||support(error,0,2)<200||SetIntersectionPolicyR30.exact(base))return null;Object means=details(feedback).get("confidenceBinMeanProbability");if(!(means instanceof List))return null;double difference=0;int eligible=0;for(int bin=0;bin<3;bin++)if(support(feedback,bin,0)>=200&&((List<?>)means).get(bin) instanceof Number){double observed=cell(feedback,bin,0),predicted=((Number)((List<?>)means).get(bin)).doubleValue();difference+=predicted-observed;eligible++;}if(eligible==0||Math.abs(difference/eligible)<.05)return null;double fitted=ctx.evaluation==null?base.temperature:ctx.evaluation.temperature(base),temperature=Math.max(.05,Math.min(20,fitted*(difference>0?1.5:2.0/3)));PolicySpecR27 child=new PolicySpecR27(base.ast,base.position,base.rule,base.threshold,base.rate,temperature,false,base.lr,base.forget,IdeaFamilyR27.meta("I13","RESEARCH_TEMPERATURE",ctx.generation,r.state,"SELECT-only frozen-readout hypothesis from actual confidence error; no formal calibrator replaced",base.policyHash()));use("MATRIX:FEEDBACK");use("MATRIX:STUDENT_ERROR");return annotate(child);}

 private PolicySpecR27 calibrationFrameR335(PolicySpecR27 base,IdeaFamilyR27.Context ctx,StatsR27.SplitMix r){Map<String,Object> f=frames.get("R13");if(f==null||!base.policyHash().equals(f.get("parentHash"))||!"ORIGINAL_ACT_K3".equals(f.get("event"))||r.chance(.25)||SetIntersectionPolicyR30.exact(base)||!(f.get("points") instanceof List))return null;double sum=0;long n=0;for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;Object nn=row.get("n"),pred=row.get("expected"),hit=row.get("value");if(nn instanceof Number&&((Number)nn).longValue()>=20&&pred instanceof Number&&hit instanceof Number){double d=((Number)pred).doubleValue()-((Number)hit).doubleValue();if(Double.isFinite(d)){long count=((Number)nn).longValue();n+=count;sum+=d*count;}}}if(n<200||Math.abs(sum/n)<.05)return null;double fitted=ctx.evaluation==null?base.temperature:ctx.evaluation.temperature(base);double temperature=Math.max(.05,Math.min(20,fitted*(sum>0?1.5:2.0/3)));PolicySpecR27 child=new PolicySpecR27(base.ast,base.position,base.rule,base.threshold,base.rate,temperature,false,base.lr,base.forget,IdeaFamilyR27.meta("I13","RESEARCH_TEMPERATURE",ctx.generation,r.state,"Original ACT-K3 SELECT calibration hypothesis; original event retained; independent qualification required",base.policyHash()));use("R13");return annotate(child);}

 public PolicySpecR27 failurePrescriptionR335(PolicySpecR27 parent,IdeaFamilyR27.Context ctx,StatsR27.SplitMix rng){Map<String,Object> f=frames.get("R05");if(f==null||rng.chance(.25)||!(f.get("points") instanceof List)||SetIntersectionPolicyR30.exact(parent))return null;
  for(Object o:(List<?>)f.get("points"))if(o instanceof Map){Map<?,?> row=(Map<?,?>)o;if(!parent.policyHash().equals(row.get("parentHash"))||!(row.get("replacementSpec") instanceof String)||!(row.get("nodeIndex") instanceof Number))continue;PolicySpecR27 child=PolicySpecR27.parse((String)row.get("replacementSpec"));if(child.position!=parent.position||!child.policyHash().equals(row.get("childHash"))||!FailureProfileR30.legal(child)||child.policyHash().equals(parent.policyHash()))continue;
   List<PolicyAstR27.Node> nodes=PolicyAstR27.nodes(parent.ast);int index=((Number)row.get("nodeIndex")).intValue();if(index<0||index>=nodes.size()||!nodes.get(index).toString().equals(row.get("beforeNode")))throw new IllegalStateException("R335_FAILURE_LEAF_CHANGED");
   use("R05");return annotate(child.withMeta(IdeaFamilyR27.meta("I10","RESEARCH_FAILURE_LEAF",ctx.generation,rng.state,"Exact SELECT failure-window parent/node prescription; independent confirmation remains required",parent.policyHash())));
  }return null;
 }
 public PolicySpecR27 residualHypothesis(PolicySpecR27 base,IdeaFamilyR27.Context ctx,StatsR27.SplitMix r){Map<String,Object> residual=frames.get("MATRIX:RESIDUAL_MECHANISM");if(residual==null||r.chance(.25)||!base.policyHash().equals(residual.get("policyHash"))||SetIntersectionPolicyR30.exact(base))return null;double largest=0;for(int bin=0;bin<3;bin++)for(int k=0;k<10;k++)if(support(residual,bin,k)>=200&&Double.isFinite(cell(residual,bin,k)))largest=Math.max(largest,Math.abs(cell(residual,bin,k)));if(largest<.05)return null;PolicyAstR27.Node ast=PolicyAstR27.op("mix",.5,PolicyAstR27.op("zscore",base.ast),PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",base.position,32)));PolicySpecR27 child=base.withAst(ast,IdeaFamilyR27.meta("I10","RESEARCH_RESIDUAL_SMOOTHING",ctx.generation,r.state,"Observed class residual motivates a refutable causal-frequency smoothing hypothesis; no claimed gain",base.policyHash()));if(!FailureProfileR30.legal(child)||ast.depth()>ctx.maxDepth)return null;use("MATRIX:RESIDUAL_MECHANISM");return annotate(child);}
 public PolicySpecR27 actionHypothesis(PolicySpecR27 base,IdeaFamilyR27.Context ctx,StatsR27.SplitMix r){Map<String,Object> m=frames.get("MATRIX:ACTION_UTILITY");if(m==null||r.chance(.25)||!(details(m).get("actionPolicy") instanceof String)||!base.policyHash().equals(PolicySpecR27.parse((String)details(m).get("actionPolicy")).policyHash())||support(m,1,2)<200||!Double.isFinite(cell(m,1,2))||cell(m,1,2)>=0||SetIntersectionPolicyR30.exact(base))return null;double rate=Math.max(.02,Math.min(.5,base.rate>0?base.rate/2:.15));PolicySpecR27 child=SelectorPolicyR30.combine(base,base.position,PolicySpecR27.Rule.QUANTILE,0,rate,ctx.evaluation,IdeaFamilyR27.meta("I13","RESEARCH_ACTION_DOMAIN",ctx.generation,r.state,"Negative SELECT unit-utility suggests a new preregistered action-domain hypothesis; no old certificate borrowed",base.policyHash()));use("MATRIX:ACTION_UTILITY");return annotate(child);}
 public PolicySpecR27 annotate(PolicySpecR27 p){if(p==null||used.isEmpty())return p;PolicySpecR27.Meta m=p.meta;if(m==null)throw new IllegalStateException("RESEARCH_PROVENANCE_META_REQUIRED");String refs=String.join(",",used);PolicySpecR27 annotated=p.withMeta(new PolicySpecR27.Meta(m.idea,m.operator,m.parents,m.generation,m.seed,m.reason+MARK+refs+"]"));recordUsageR31(annotated,new ArrayList<>(used));return annotated;}
 /** R31-B08-C02: every actual use of a research source: which runs, which child, which parents and operator. A use
  * whose child equals a parent changed nothing and is recorded as NO_EFFECTIVE_CHANGE, never as a consumption. */
 private final List<Map<String,Object>> usagesR31=new ArrayList<>();
 private void recordUsageR31(PolicySpecR27 child,List<String> runs){Map<String,Object> u=new LinkedHashMap<>();u.put("sourceRuns",runs);u.put("childHash",child.policyHash());u.put("operator",child.meta.operator);u.put("idea",child.meta.idea);u.put("parents",new ArrayList<Object>(child.meta.parents));
  boolean changed=!child.meta.parents.contains(child.policyHash());u.put("effect",changed?"STRUCTURE_OR_PARAMETER_CHANGED":"NO_EFFECTIVE_CHANGE");u.put("datasetHash",datasetHash);u.put("sourceRevision",revision);usagesR31.add(u);}
 public List<Map<String,Object>> usagesR31(){return Collections.unmodifiableList(usagesR31);}
 /** Ablation: the same context without one source run (its frames are invisible), to measure that run's influence. */
 public ResearchContextR30R1 withoutSourceRunR31(String run){ResearchContextR30R1 copy=new ResearchContextR30R1(datasetHash,revision);copy.actionOrdinalR335=actionOrdinalR335;for(Map.Entry<String,Map<String,Object>> e:frames.entrySet())if(!run.equals(runs.get(e.getKey()))){copy.frames.put(e.getKey(),e.getValue());if(runs.containsKey(e.getKey())){String source=runs.get(e.getKey());copy.runs.put(e.getKey(),source);if(sourceHashesR31.containsKey(source))copy.sourceHashesR31.put(source,sourceHashesR31.get(source));if(sourceContractsR31.containsKey(source))copy.sourceContractsR31.put(source,sourceContractsR31.get(source));if(sourceInputsR31.containsKey(source))copy.sourceInputsR31.put(source,sourceInputsR31.get(source));}}return copy;}
 public Set<String> sourceRunsR31(){return Collections.unmodifiableSet(new LinkedHashSet<>(runs.values()));}
 public static List<String> references(PolicySpecR27 p){if(p==null||p.meta==null||p.meta.reason==null)return Collections.emptyList();String reason=p.meta.reason;int start=reason.lastIndexOf(MARK);if(start<0)return Collections.emptyList();int end=reason.indexOf(']',start+MARK.length());if(end<0)throw new IllegalStateException("RESEARCH_REFERENCE_FORMAT");List<String> ids=Arrays.asList(reason.substring(start+MARK.length(),end).split(","));if(ids.size()>4)throw new IllegalStateException("RESEARCH_REFERENCE_BUDGET");for(String id:ids)ResearchContractR30R1.sha(id);return ids;}
 static String proposal(PolicySpecR27 p,String dataset,List<String> refs){return StatsR27.sha256("RESEARCH_HYPOTHESIS1|"+dataset+'|'+p.policyHash()+'|'+String.join(",",refs));}
 public static String stage(ResearchWriteQueueR27.Batch b,PolicySpecR27 p,String dataset,long revision,long now){return stage(b,p,dataset,revision,now,Collections.emptyMap());}
 private static String stage(ResearchWriteQueueR27.Batch b,PolicySpecR27 p,String dataset,long revision,long now,Map<String,Object> action){List<String> refs=references(p);if(refs.isEmpty())return null;String id=proposal(p,dataset,refs);Map<String,Object> payload=new LinkedHashMap<>();payload.put("candidate",p.fullJson());payload.put("candidateHash",p.policyHash());payload.put("sourceRuns",refs);payload.put("consumer",p.meta.idea);payload.put("role","SELECT");if(!action.isEmpty())payload.put("actionContractR31",action);String json=JsonR27.write(payload);b.guardBefore(db->{ResearchInputPinR30R1.require(db,dataset,revision);for(String ref:refs)try(Cursor q=db.rawQuery("SELECT role,source_revision,source_commit,summary_json FROM r30r1_analysis_run WHERE run_id=? AND dataset_hash=? AND status='COMMITTED'",new String[]{ref,dataset})){if(!q.moveToFirst()||!"SELECT".equals(q.getString(0))||q.getLong(1)!=revision||!ResearchStoreR30R1.currentSource(q.getString(2),q.getString(3)))throw new ResearchStoreR30R1.StaleResearch();}return 0;});b.add("INSERT OR IGNORE INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)",id,refs.get(0),p.meta.parents.isEmpty()?StatsR27.sha256("NO_PARENT_NEW_HYPOTHESIS"):p.meta.parents.get(0),"SUPPORTED_RESEARCH_HYPOTHESIS",json,StatsR27.sha256(json),"READY","Research source consumed to construct exact typed candidate; original birth gate and independent confirmation remain",now);return id;}
}
