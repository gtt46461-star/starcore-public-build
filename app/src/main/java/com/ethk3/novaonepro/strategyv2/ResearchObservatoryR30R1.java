package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.util.*;

/** One analytical view per original-pool continuation. Measures are authoritative; chart frames are projections. */
public final class ResearchObservatoryR30R1 {
 private ResearchObservatoryR30R1(){}
 public static final String VERSION="R30_REPAIR_OBSERVATORY1",PANEL="research-observatory-r1";
 private static final String MISSING_REAL_RECEIPT_SOURCE = "MISSING_SOURCE:r974_exact_real_receipt";
 static Map<String,Object> map(){return new LinkedHashMap<>();}
 static Map<String,Object> point(String label,Object value,long n,String details){Map<String,Object> p=map();p.put("label",label);p.put("value",value);p.put("n",n);p.put("details",details);return p;}
 static Map<String,Object> header(String view,EvalContextR27 c){Map<String,Object> f=map();f.put("viewId",view);f.put("datasetHash",c.data.datasetHash);f.put("role","SELECT");f.put("researchOnly",true);f.put("qualityEffective",null);f.put("status","MEASURED_RESEARCH_ONLY");f.put("points",new ArrayList<Object>());f.put("edges",new ArrayList<Object>());f.put("fromOrdinal",c.data.ordinals[c.split.selectFrom]);f.put("throughOrdinal",c.data.ordinals[c.split.selectTo-1]);return f;}
 @SuppressWarnings("unchecked") static List<Object> points(Map<String,Object> f){return (List<Object>)f.get("points");}
 static Map<String,Object> blocked(Map<String,Object> f,String why){f.put("status","BLOCKED_DEPENDENCY");f.put("reason",why);return f;}
 static void estimate(Map<String,Object> p,ResearchGainR30R1 gain,double scale){ResearchGainR30R1.Estimate e=gain.estimate();p.put("value",e.meanBlockGain==null?null:e.meanBlockGain*scale);p.put("lower",e.lower95==null?null:e.lower95*scale);p.put("upper",e.upper95==null?null:e.upper95*scale);p.put("n",e.evaluatedIssues);p.put("blocks",gain.blocks());p.put("completeBlocks",e.completeBlocks);p.put("missingIssues",e.missingIssues);p.put("excludedTailIssues",e.excludedTailIssues);p.put("analysisContractHash",e.analysisContractHash);p.put("lossTarget",e.loss.name());p.put("statisticScale",scale);}
 static List<PolicySpecR27> supported(List<PolicySpecR27> in){List<PolicySpecR27> out=new ArrayList<>();for(PolicySpecR27 p:in)if(ResearchStudyTaskR30R1.supported(p)){out.add(p);if(out.size()==8)break;}return out;}
 @SuppressWarnings("unchecked") public static Map<String,Object> compute(String view,EvalContextR27 c,List<PolicySpecR27> population,long revision){
  if(c.labels!=c.data.draws||c.split.selectFrom>=c.split.selectTo)throw new IllegalArgumentException("OBSERVATORY_ORIGINAL_SELECT_REQUIRED");Map<String,Object> f=header(view,c);List<PolicySpecR27> ps=supported(population);PolicySpecR27 first=ps.isEmpty()?null:ps.get(0);
  if(view.equals("R01")||view.equals("R02")){
   StructureStudyR30R1.Product product=StructureStudyR30R1.compute(c,ResearchContractR30R1.Role.SELECT);Map<String,Object> original=(Map<String,Object>)JsonR27.parse(view.equals("R01")?product.statePayload:product.errorPayload);f.putAll(original);
   for(Object o:(List<?>)original.get("cells")){Map<String,Object> cell=(Map<String,Object>)o;String label="W"+cell.get("window")+":"+cell.get("target")+(cell.containsKey("position")?":P"+cell.get("position"):"");Map<String,Object> p=point(label,cell.get(view.equals("R01")?"value":"meanPairedGain"),cell.get(view.equals("R01")?"supportCalendarIssues":"calendarIssues") instanceof Number?((Number)cell.get(view.equals("R01")?"supportCalendarIssues":"calendarIssues")).longValue():0,JsonR27.write(cell));p.put("lower",cell.get("lower95"));p.put("upper",cell.get("upper95"));points(f).add(p);}
   if(view.equals("R01")){List<Object> ordered=new ArrayList<>();List<String> rowNames=new ArrayList<>(),columns=new ArrayList<>();for(int pos=1;pos<=10;pos++)columns.add("P"+pos);for(int window:StructureStudyR30R1.WINDOWS)for(String target:StructureStudyR30R1.TARGETS){rowNames.add("W"+window+":"+target);for(int pos=1;pos<=10;pos++)for(Object o:points(f))if(((Map<?,?>)o).get("label").equals("W"+window+":"+target+":P"+pos))ordered.add(o);}f.put("points",ordered);f.put("matrixRows",rowNames);f.put("matrixColumns",columns);}
   else {TreeMap<Long,List<Object>> byBlock=new TreeMap<>();List<String> series=new ArrayList<>();int at=0;for(Object o:(List<?>)original.get("cells")){Map<String,Object> cell=(Map<String,Object>)o;series.add("W"+cell.get("window")+":"+cell.get("target"));for(Object raw:(List<?>)cell.get("blocks")){Map<?,?> block=(Map<?,?>)raw;long start=((Number)block.get("fromOrdinal")).longValue();List<Object> values=byBlock.computeIfAbsent(start,k->new ArrayList<>(Collections.nCopies(12,null)));if(Boolean.TRUE.equals(block.get("included")))values.set(at,block.get("meanGain"));}at++;}List<Object> rows=new ArrayList<>();for(Map.Entry<Long,List<Object>> e:byBlock.entrySet()){Map<String,Object> row=point("ordinal:"+e.getKey(),e.getValue().get(0),64,"固定64期块；缺失/尾块断线；各系列 aggregate CI与loss目标见原cells");row.put("stack",e.getValue());rows.add(row);}f.put("points",rows);f.put("series",series);f.put("xAxis","PREDECLARED_CALENDAR_BLOCK_START_ORDINAL");}
   return f;
  }
  if(view.equals("R03")){
   MatrixTensorR30R1.Tensor a=MatrixResearchR30R1.build("NUMBER_POSITION_COMPATIBILITY",c,null,revision),b=MatrixResearchR30R1.build("CROSS_POSITION_FLOW",c,null,revision);f.put("numberPosition",JsonR27.parse(a.payload));f.put("directedLag",JsonR27.parse(b.payload));f.put("sourceHashes",Arrays.asList(a.hash,b.hash));
   for(int pos=1;pos<=10;pos++)for(int src=1;src<=10;src++){int n=0,match=0;for(int t=c.split.selectFrom;t<c.split.selectTo;t++)if(c.data.consecutive(t,1)){n++;if(c.data.draws[t][pos-1]==c.data.draws[t-1][src-1])match++;}points(f).add(point("P"+src+"@1→P"+pos,n==0?null:match/(double)n-.1,n,"原SELECT同类滞后超额关联；不是因果或自然优势证书"));}return f;
  }
  if(view.equals("R04")){
   for(int pos=1;pos<=10;pos++){PolicySpecR27 p=SetIntersectionPolicyR30.spec(pos,32,128,IdeaFamilyR27.meta("I14","RESEARCH",0,0,"FIXED_SET_RESEARCH"));ReplayEvaluatorR27.Trace tr=FusionStudyR30R1.trace(p,c);int available=0,act=0,hits=0,singleHits=0,common=0;List<Object> support=new ArrayList<>();for(int t=c.split.selectFrom;t<c.split.selectTo;t++){Map<String,Object> s=SetIntersectionPolicyR30.support(c.cache,t,pos,32,128);if(!"BLOCKED_HISTORY_OR_GAP".equals(s.get("status")))available++;int a=((Number)s.get("sourceAMask")).intValue();if(a!=0&&(a&(1<<(c.data.draws[t][pos-1]-1)))!=0)singleHits++;if(!tr.skipped[t]){act++;hits+=tr.hit[t];if((a&(1<<(c.data.draws[t][pos-1]-1)))!=0)common++;}if(support.size()<3&&t>=c.split.selectTo-3)support.add(s);}Map<String,Object> row=point("P"+pos,available==0?null:act/(double)available,available,"合法K3覆盖率；不足3弃权，随机同大小K3命中基线0.30");row.put("position",pos);row.put("shortWindow",32);row.put("longWindow",128);row.put("singleSetHits",singleHits);row.put("intersectionActs",act);row.put("intersectionHits",hits);row.put("hitRate",act==0?null:hits/(double)act);row.put("abstentions",available-act);row.put("missedSingleSetHits",singleHits-common);row.put("lastSupport",support);row.put("stack",available==0?null:Arrays.asList(act,available-act));f.put("series",Arrays.asList("支持K3的期数","不足3或条件不符而弃权的期数"));row.put("randomK3ExpectedHits",.3*act);points(f).add(row);}return f;
  }
  if(view.equals("R05")){
   if(first==null)return blocked(f,"ORIGINAL_SELECT_FAILURE_PARENT_REQUIRED");
   FailureProfileR30 profile=c.failureProfile(first);if(!profile.ready())return blocked(f,"SUPPORTED_SELECT_FAILURE_WINDOW_REQUIRED");
   List<PolicyAstR27.Node> nodes=PolicyAstR27.nodes(first.ast);List<Integer> indices=MutationOpsR27.indices(nodes,n->n.leaf()&&!"c".equals(n.op)&&MutationOpsR27.windowed(n)>=0);
   for(int index:profile.prioritize(nodes,indices)){
    PolicyAstR27.Node oldNode=nodes.get(index);int slot=MutationOpsR27.windowed(oldNode);int[] args=oldNode.ints.clone();
    int replacement=profile.window;if(replacement==args[slot])for(int window:FeatureCacheR27.WINDOWS)if(window!=args[slot]){replacement=window;break;}
    args[slot]=replacement;PolicyAstR27.Node replacementNode=PolicyAstR27.leaf(oldNode.op,args);
    PolicySpecR27 child=first.withAst(PolicyAstR27.replace(first.ast,index,replacementNode),IdeaFamilyR27.meta("I10","RESEARCH_FAILURE_LEAF",first.meta==null?0:first.meta.generation,0,"Exact SELECT failure-node development prescription",first.policyHash()));
    if(!FailureProfileR30.legal(child)||child.policyHash().equals(first.policyHash()))continue;
    Map<String,Object> row=point(first.policyHash()+":"+index,profile.hitRate(),profile.nAct,"Measured SELECT failure diagnosis; replacement is an unqualified new hypothesis, never a claimed repair gain");
    row.put("parentHash",first.policyHash());row.put("nodeIndex",index);row.put("beforeNode",oldNode.toString());row.put("afterNode",replacementNode.toString());row.put("childHash",child.policyHash());row.put("replacementSpec",child.fullJson());row.put("failureProfile",profile.json());points(f).add(row);break;
   }
   return points(f).isEmpty()?blocked(f,"NO_LEGAL_WINDOWED_LEAF_REPLACEMENT"):f;
  }
  if(view.equals("R07")){
   PolicySpecR27 p=null;for(PolicySpecR27 x:ps)if(x.ast.op.equals("add")&&x.ast.kids.length==2){p=x;break;}if(p==null){outer:for(int i=0;i<ps.size();i++)for(int j=0;j<i;j++)if(ps.get(i).position==ps.get(j).position&&!ps.get(i).policyHash().equals(ps.get(j).policyHash())){
    PolicySpecR27 a=ps.get(i),b=ps.get(j);PolicySpecR27 candidate=a.withAst(PolicyAstR27.op("add",a.ast,b.ast),new PolicySpecR27.Meta("I11","RESEARCH_MOLECULE_PROBE",Arrays.asList(a.policyHash(),b.policyHash()),Math.max(a.meta==null?0:a.meta.generation,b.meta==null?0:b.meta.generation),0,"Unqualified same-target two-component hypothesis; four-arm SELECT research only"));
    if(FailureProfileR30.legal(candidate)&&candidate.ast.depth()<=6&&candidate.ast.size()<=64){p=candidate;f.put("parentOrigin","UNQUALIFIED_RESEARCH_COMPOSITION_FROM_TWO_ORIGINAL_PARENTS");break outer;}
   }}if(p==null)return blocked(f,"LEGAL_TWO_COMPONENT_ADD_PARENT_REQUIRED");PolicySpecR27 a=p.withAst(p.ast.kids[0],p.meta),b=p.withAst(p.ast.kids[1],p.meta);if(!FailureProfileR30.legal(a)||!FailureProfileR30.legal(b))return blocked(f,"SINGLE_ARMS_MUST_RETAIN_TYPED_POLICY_CONTRACT");ReplayEvaluatorR27.Trace ab=FusionStudyR30R1.trace(p,c),ta=FusionStudyR30R1.trace(a,c),tb=FusionStudyR30R1.trace(b,c);ResearchGainR30R1 synergy=new ResearchGainR30R1(c.data.ordinals[c.split.selectFrom],c.data.ordinals[c.split.selectTo-1],64,4,200);double[] sums=new double[4];int n=0;double[] uniform=new double[10];Arrays.fill(uniform,.1);
   for(int t=c.split.selectFrom;t<c.split.selectTo;t++){if((t&31)==0)CooperativeContinuationR26.poll();int y=c.data.draws[t][p.position-1];double la=FusionStudyR30R1.loss(ta.probabilityRows[t],y),lb=FusionStudyR30R1.loss(tb.probabilityRows[t],y),lab=FusionStudyR30R1.loss(ab.probabilityRows[t],y),l0=FusionStudyR30R1.loss(uniform,y);if(!Double.isFinite(la)||!Double.isFinite(lb)||!Double.isFinite(lab))continue;synergy.add(c.data.ordinals[t],.5+(la+lb-lab-l0)/4,.5);sums[0]+=l0;sums[1]+=la;sums[2]+=lb;sums[3]+=lab;n++;}Map<String,Object> row=point(p.policyHash(),null,n,"协同=L_A+L_B-L_AB-L_none；同一目标/期，原更新器，所有温度仅TRAIN拟合");estimate(row,synergy,4);row.put("parent",p.fullJson());row.put("aHash",a.policyHash());row.put("bHash",b.policyHash());row.put("abHash",p.policyHash());row.put("nodeCost",p.ast.size());row.put("fourArmLossSums",sums);f.put("matrixRows",Collections.singletonList("A:"+a.policyHash().substring(0,8)));f.put("matrixColumns",Collections.singletonList("B:"+b.policyHash().substring(0,8)));row.put("formalRowsRead",0);points(f).add(row);return f;
  }
  if(view.equals("R08")){
   int end=c.split.trainTo;f.put("role","TRAIN");f.put("fromOrdinal",c.data.ordinals[Math.max(0,end-BirthGateR27.PROBE)]);f.put("throughOrdinal",c.data.ordinals[end-1]);BirthGateR27 gate=new BirthGateR27(c.cache,end);List<int[]> signatures=new ArrayList<>();for(PolicySpecR27 p:ps){BirthGateR27.Verdict v=gate.check(p);Map<String,Object> row=point(p.policyHash(),v.ok?(double)v.signature.length:null,Math.min(500,end),"原固定TRAIN探针的决策签名；不等质量");row.put("position",p.position);row.put("idea",p.meta==null?null:p.meta.idea);row.put("signatureHash",v.ok?StatsR27.sha256(Arrays.toString(v.signature)):null);row.put("gateReason",v.ok?"ACCEPTED_ORIGINAL_GATE":v.reason+":"+v.detail);points(f).add(row);signatures.add(v.ok?v.signature:null);}for(int i=0;i<ps.size();i++)for(int j=0;j<i;j++)if(signatures.get(i)!=null&&signatures.get(j)!=null&&(ps.get(i).position==ps.get(j).position||signatures.get(i).length==0&&signatures.get(j).length==0)){Map<String,Object> edge=map();edge.put("from",ps.get(i).policyHash());edge.put("to",ps.get(j).policyHash());edge.put("label","Jaccard="+BirthGateR27.jaccard(signatures.get(i),signatures.get(j)));((List<Object>)f.get("edges")).add(edge);}return f;
  }
  if(view.equals("R12")){
   for(int i=0;i<ps.size();i++)for(int j=0;j<i;j++)if(ps.get(i).position==ps.get(j).position&&!SetIntersectionPolicyR30.exact(ps.get(i))&&!SetIntersectionPolicyR30.exact(ps.get(j))){FusionStudyR30R1.Pair pair=FusionStudyR30R1.pair(ps.get(i),ps.get(j),c);Map<String,Object> row=point(ps.get(i).policyHash()+"/"+ps.get(j).policyHash(),Double.isFinite(pair.residualCorrelation)?pair.residualCorrelation:null,pair.n,"同目标十分类残差与共同失败；候选融合必须另测独立SELECT增益");row.putAll(pair.json());row.put("parentA",ps.get(i).policyHash());row.put("parentB",ps.get(j).policyHash());points(f).add(row);}if(points(f).isEmpty())return blocked(f,"TWO_SAME_TARGET_PARENTS_REQUIRED");List<String> pairs=new ArrayList<>();for(Object o:points(f))pairs.add(String.valueOf(((Map<?,?>)o).get("label")));f.put("matrixRows",Collections.singletonList("同目标父策略对残差相关"));f.put("matrixColumns",pairs);return f;
  }
  if(view.equals("R13")){
   if(first==null)return blocked(f,"ORIGINAL_SELECT_STUDENT_REQUIRED");ReplayEvaluatorR27.Trace tr=FusionStudyR30R1.trace(first,c);int[] count=new int[10],hits=new int[10];double[] sum=new double[10],sq=new double[10];int total=0;for(int t=c.split.selectFrom;t<c.split.selectTo;t++)if(!tr.skipped[t]&&tr.act[t]){double p=tr.pHit[t];int bin=Math.min(9,(int)(p*10));count[bin]++;hits[bin]+=tr.hit[t];sum[bin]+=p;sq[bin]+=(p-tr.hit[t])*(p-tr.hit[t]);total++;}for(int i=0;i<10;i++){Map<String,Object> row=point("confidence["+i/10.0+","+(i+1)/10.0+")",count[i]==0?null:hits[i]/(double)count[i],count[i],"SELECT开发诊断：原事前K3事件；二元目标，不代替正式FUTURE校准验收");row.put("expected",count[i]==0?null:sum[i]/count[i]);row.put("binaryBrier",count[i]==0?null:sq[i]/count[i]);row.put("supported",count[i]>=20);points(f).add(row);}f.put("parentHash",first.policyHash());f.put("position",first.position);f.put("event","ORIGINAL_ACT_K3");f.put("eligiblePredeclaredActs",total);f.put("futureLabelsUsed",0);f.put("fitStatus","NO_FIT_IN_THIS_DIAGNOSTIC");return f;
  }
  if(view.equals("R15")){
   int end=c.split.selectTo,mid=Math.max(c.split.selectFrom,end-256),start=Math.max(c.split.selectFrom,mid-256);if(mid-start<64||end-mid<64)return blocked(f,"TWO_PREDECLARED_256_CALENDAR_INPUT_WINDOWS_REQUIRED");for(int pos=0;pos<10;pos++){double[] a=new double[10],b=new double[10];Arrays.fill(a,1);Arrays.fill(b,1);for(int t=start;t<mid;t++)a[c.data.draws[t][pos]-1]++;for(int t=mid;t<end;t++)b[c.data.draws[t][pos]-1]++;double js=0;for(int k=0;k<10;k++){a[k]/=mid-start+10;b[k]/=end-mid+10;double m=(a[k]+b[k])/2;js+=.5*(a[k]*Math.log(a[k]/m)+b[k]*Math.log(b[k]/m));}Map<String,Object> row=point("P"+(pos+1),js,end-start,"输入Jensen-Shannon散度，阈值0.05只报警，不重置模型/资格");row.put("inputAlarm",js>.05);row.put("performanceDegradation",null);row.put("oldNewModelPairedLoss",null);row.put("action",js>.05?"REQUEST_LEGAL_RESEARCH_REPLAY_COMPARISON":"KEEP_CURRENT_MODEL");points(f).add(row);}f.put("performanceReason","CURRENT_AND_PREVIOUS_MODEL_SAME_SAMPLE_OUTPUT_RECEIPTS_REQUIRED");f.put("series",Collections.singletonList("输入JS距离"));f.put("xAxis","CATEGORICAL_POSITION_NOT_ELAPSED_TIME");for(Object o:points(f)){Map<String,Object> row=(Map<String,Object>)o;row.put("stack",Collections.singletonList(row.get("value")));}return f;
  }
  return blocked(f,"ORIGINAL_STORE_RECEIPTS_REQUIRED");
 }
 static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static Map<String,Object> stored(String view,EvalContextR27 c,SQLiteDatabase db,RuntimeR27 rt){Map<String,Object> f=header(view,c);
  if(view.equals("R09")){
   List<String[]> pairs=new ArrayList<>();try(Cursor q=db.rawQuery("SELECT e.expert,p.spec_json,a.spec_json FROM r27_expert_proposal e JOIN r27_policy p ON p.policy_hash=e.policy_hash JOIN r27_lineage l ON l.child_hash=p.policy_hash JOIN r27_policy a ON a.policy_hash=l.parent_hash WHERE p.dataset_hash=? AND a.dataset_hash=? ORDER BY e.generation DESC,e.expert,p.policy_hash,a.policy_hash LIMIT 16",new String[]{c.data.datasetHash,c.data.datasetHash})){while(q.moveToNext())pairs.add(new String[]{q.getString(0),q.getString(1),q.getString(2)});}
   Set<String> seen=new HashSet<>();for(String[] original:pairs){PolicySpecR27 child=PolicySpecR27.parse(original[1]),parent=PolicySpecR27.parse(original[2]);String key=original[0]+"|"+parent.policyHash()+"|"+child.policyHash();if(!seen.add(key)||child.position!=parent.position||!ResearchStudyTaskR30R1.supported(child)||!ResearchStudyTaskR30R1.supported(parent))continue;
    ResearchGainR30R1 gain=FusionStudyR30R1.gain(parent,child,c);Map<String,Object> row=point(key,null,0,"Expert credit uses original same-target SELECT paired loss; proposal/birth counts confer no credit");estimate(row,gain,1);row.put("expert",original[0]);row.put("parentHash",parent.policyHash());row.put("childHash",child.policyHash());row.put("actualAst",child.ast.toString());row.put("position",child.position);points(f).add(row);if(points(f).size()>=4)break;
   }
   f.put("recordedExpertProposals",scalar(db,"SELECT count(*) FROM r27_expert_proposal"));f.put("denominator","BOUNDED_ORIGINAL_PARENT_CHILD_SELECT_PAIRS;AUDIT_EXPERTS_RETAINED_WITHOUT_FAKE_CHILDREN");
  }else if(view.equals("R10")){
   try(Cursor q=db.rawQuery("SELECT l.child_hash,l.parent_hash,p.spec_json,a.spec_json FROM r27_lineage l JOIN r27_policy p ON p.policy_hash=l.child_hash JOIN r27_policy a ON a.policy_hash=l.parent_hash WHERE p.dataset_hash=? ORDER BY l.created_at DESC,l.child_hash,l.parent_hash LIMIT 16",new String[]{c.data.datasetHash})){java.util.Set<String> seen=new java.util.HashSet<>();while(q.moveToNext()&&points(f).size()<4){String identity=q.getString(0)+":"+q.getString(1);if(!seen.add(identity))continue;PolicySpecR27 child=PolicySpecR27.parse(q.getString(2)),parent=PolicySpecR27.parse(q.getString(3));if(parent.position!=child.position||!ResearchStudyTaskR30R1.supported(parent)||!ResearchStudyTaskR30R1.supported(child))continue;ResearchGainR30R1 g=FusionStudyR30R1.gain(parent,child,c);Map<String,Object> row=point(identity,null,0,"原父子同目标SELECT十分类proper score；固定64期块，所有温度只TRAIN拟合；不继承证书");estimate(row,g,1);row.put("parentHash",parent.policyHash());row.put("childHash",child.policyHash());row.put("operator",child.meta==null?null:child.meta.operator);row.put("family",child.meta==null?null:child.meta.idea);row.put("nodeCost",child.ast.size());row.put("perObjectCpuNs",null);row.put("costScope","AST_NODE_COMPLEXITY_ONLY_CPU_UNKNOWN_NOT_POOL_CPU");points(f).add(row);}}f.put("explorationFloor",.25);f.put("budgetRole","SELECT_ONLY");f.put("series",Collections.singletonList("父子十分类proper配对收益"));f.put("xAxis","CATEGORICAL_OPERATOR_AND_AST_NODE_COST_NOT_CPU_SCATTER_OR_TIME");for(Object o:points(f)){Map<String,Object> row=(Map<String,Object>)o;row.put("label",row.get("operator")+"/nodes="+row.get("nodeCost"));row.put("stack",Collections.singletonList(row.get("value")));}
  }else if(view.equals("R17")){
   String kind=view.equals("R17")?"FAILURE_MEMORY":"ATOM_PAIRED_GAIN";try(Cursor q=db.rawQuery("SELECT m.parent_hash,m.measure_key,m.mean_gain,m.lower95,m.upper95,m.evaluated_calendar_issues,m.payload_json,r.run_id FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id WHERE m.kind=? AND r.role='SELECT' AND r.dataset_hash=? AND r.source_commit=? ORDER BY r.recorded_at DESC LIMIT 80",new String[]{kind,c.data.datasetHash,BuildConfig.SOURCE_COMMIT_R26})){while(q.moveToNext()){Map<String,Object> row=point(q.getString(0)+":"+q.getString(1),q.isNull(2)?null:q.getDouble(2),q.getLong(5),q.getString(6));row.put("lower",q.isNull(3)?null:q.getDouble(3));row.put("upper",q.isNull(4)?null:q.getDouble(4));row.put("runId",q.getString(7));row.put("parentHash",q.getString(0));row.put("negativeContribution",JsonR27.parse(q.getString(6)));row.put("perObjectCpuNs",null);row.put("costReason","NO_OBJECT_CPU_RECEIPT_WHOLE_POOL_CPU_NOT_SUBSTITUTED");points(f).add(row);}}f.put("explorationFloor",.25);f.put("familyPermanentlyDisabled",false);
  }else if(view.equals("R11")){
   f.put("role","CONFIRM");f.put("formalReadOnly",true);try(Cursor q=db.rawQuery("SELECT task_key,policy_hash,state,verified,registration_state,registration_experiment_id,result_json FROM r30_frozen_confirm ORDER BY completed_at DESC,created_at DESC LIMIT 80",null)){while(q.moveToNext()){Map<String,Object> row=point(q.getString(0),q.getInt(3),1,"独立冻结确认；verified=0为合法终态，不反馈共享选择");row.put("childHash",q.getString(1));row.put("state",q.getString(2));row.put("registration",q.getString(4));row.put("experimentId",q.getString(5));row.put("result",q.isNull(6)?null:JsonR27.parse(q.getString(6)));points(f).add(row);}}
  }else if(view.equals("R14")){
   f.put("role","FUTURE");f.put("formalReadOnly",true);f.put("consumption",ModelSurfaceStoreR30.report(db));
   try(Cursor q=db.rawQuery("SELECT g.publication_id,g.source_key,g.role,g.gradient_norm,g.parameter_change,g.state_hash,p.model_hash"
       +" FROM r34_atomnet_optimizer_step g JOIN r31_atomnet_publication p ON p.publication_id=g.publication_id"
       +" AND p.state_hash=g.state_hash WHERE p.status='PUBLISHED' ORDER BY g.recorded_at DESC,g.publication_id LIMIT 24",null)){
    while(q.moveToNext()){
     Map<String,Object> row=point(q.getString(1),q.getDouble(4),1,"已提交的实际权重变化，预测消费原键见同帧 consumption");
     row.put("publication",q.getString(0));row.put("trainingRole",q.getString(2));row.put("gradientNorm",q.getDouble(3));
     row.put("stateHash",q.getString(5));row.put("modelHash",q.getString(6));points(f).add(row);
    }
   }
   for(String table:new String[]{"r30_model_artifact","r30_model_surface","r30_model_call"})
    points(f).add(point(table,scalar(db,"SELECT count(*) FROM "+table),-1,"历史原表保留水位；实际调用按模型与冻结果身份读取"));
   f.put("gradientSource","r34_atomnet_optimizer_step/r31_atomnet_publication");
   f.put("qualifyingQualityState","INDEPENDENT_FUTURE_MEASURE_REQUIRED");
  }else if(view.equals("R16")){
   List<Map<String,Object>> costs=rt==null?Collections.emptyList():rt.pool.researchCosts();f.put("actualTaskCpu",costs);Map<String,Object> cpu=new HashMap<>();for(Map<String,Object> cost:costs)cpu.put(String.valueOf(cost.get("task")),((Number)cost.get("actualContinuationCpuNs")).doubleValue()/1000000);f.put("series",Collections.singletonList("本会话计算CPU毫秒"));try(Cursor q=db.rawQuery("SELECT task_id,work_cursor,pending,last_commit_clock_ordinal,completed_input_ordinal,work_unit,artifact_identity FROM r30r1_task_progress ORDER BY task_id LIMIT 80",null)){while(q.moveToNext()){Map<String,Object> row=point(q.getString(0),cpu.get(q.getString(0)),q.isNull(2)?-1:q.getLong(2),q.getString(5));row.put("workCursor",q.getLong(1));row.put("stack",cpu.containsKey(q.getString(0))?Collections.singletonList(cpu.get(q.getString(0))):null);row.put("artifactIdentity",q.getString(6));row.put("calendarCommitClock",q.isNull(3)?null:q.getLong(3));row.put("completedInput",q.isNull(4)?null:q.getLong(4));points(f).add(row);}}f.put("admittedResearchChildren",scalar(db,"SELECT count(*) FROM r30r1_consume"));f.put("pendingResearchProposals",scalar(db,"SELECT count(*) FROM r30r1_proposal WHERE state='READY'"));f.put("explorationFloor",.25);
  }else if(view.equals("R18")){
   f.put("role","FUTURE");f.put("formalReadOnly",true);
   Map<String,Map<String,Object>> actual=new LinkedHashMap<>();
   boolean receiptTablePresent;
   try (Cursor schema = db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",
           new String[]{"r974_exact_real_receipt"})) {
    receiptTablePresent = schema.moveToFirst();
   }
   if (receiptTablePresent) {
    for (Map<String,Object> receipt : com.ethk3.novaonepro.ResearchMainlineContributionAuditR922.exactRealAttributionR34(db,80)) {
     actual.putIfAbsent(String.valueOf(receipt.get("policy")), receipt);
    }
   }
   String attributionUnavailable = receiptTablePresent
           ? "NO_EXACT_ORIGINAL_POLICY_AUTHORIZATION_AND_SETTLEMENT_KEY" : MISSING_REAL_RECEIPT_SOURCE;
   f.put("settlementAttributionScope", receiptTablePresent
           ? "EXACT_REAL_DECISION_AND_AUTHORITATIVE_DRAW_READBACK" : MISSING_REAL_RECEIPT_SOURCE);
   f.put("naturalQualityState","EVALUATE_INDEPENDENT_REGISTERED_FUTURE_RECEIPTS");
   try(Cursor q=db.rawQuery("SELECT p.run_id,p.proposal_id,c.child_policy_hash,s.state,f.task_key,f.state,f.verified,f.registration_experiment_id,k.status,e.state FROM r30r1_proposal p LEFT JOIN r30r1_consume c ON c.proposal_id=p.proposal_id LEFT JOIN r27_policy s ON s.policy_hash=c.child_policy_hash LEFT JOIN r30_frozen_confirm f ON f.policy_hash=c.child_policy_hash LEFT JOIN r27_certificate k ON k.policy_hash=f.registration_experiment_id LEFT JOIN r27_eprocess e ON e.policy_hash=f.registration_experiment_id ORDER BY p.created_at DESC,p.proposal_id,f.task_key LIMIT 80",null)){while(q.moveToNext()){Map<String,Object> row=point(q.getString(1),q.isNull(2)?0:1,1,"逐真实外键；无child/确认/资格则链停在对应段，不能按同一期归因REAL");row.put("analysisRun",q.getString(0));row.put("proposal",q.getString(1));row.put("childHash",q.getString(2));row.put("admissionState",q.getString(3));row.put("confirmationTask",q.getString(4));row.put("confirmationState",q.getString(5));row.put("verified",q.isNull(6)?null:q.getInt(6));row.put("experiment",q.getString(7));row.put("certificateStatus",q.getString(8));row.put("sequentialState",q.getString(9));Map<String,Object> receipt=actual.get(MainlineFreezeProofR31.PREFIX+q.getString(7));row.put("realAuthorization",receipt==null?null:receipt.get("authorizationHash"));row.put("attributedRealSettlement",receipt);row.put("attributionReason",receipt==null?attributionUnavailable:"MATCHED_ORIGINAL_POLICY_MODEL_AUTHORIZATION_AND_TRADE_KEYS");points(f).add(row);}}f.put("uniqueRealOwner","DecisionGateEngine_AND_SettlementEngine");f.put("naturalQuality","NOT_PROVEN");
  }else return null;
  ResearchProvenanceGraphR30R1.project(view,f,db);
  if (points(f).isEmpty()) {
   f.put("status", "INSUFFICIENT");
   f.put("reason", "NO_ORIGINAL_RECEIPTS_YET");
  }
  if (MISSING_REAL_RECEIPT_SOURCE.equals(f.get("settlementAttributionScope"))) {
   f.put("reason", MISSING_REAL_RECEIPT_SOURCE);
  }
  return f;
 }
 public static ResearchWriteQueueR27.Batch product(String view,String measure,EvalContextR27 c,long revision,Map<String,Object> frame,long costNs,long now){
  classifyFrameR335(frame);String policy=StatsR27.sha256("RESEARCH_OBSERVATORY_SOURCE_ONLY"),input=StatsR27.sha256(JsonR27.write(frame));ResearchContractR30R1.Role role=ResearchContractR30R1.Role.valueOf(String.valueOf(frame.get("role")));ResearchContractR30R1 contract=new ResearchContractR30R1(c.data.datasetHash,policy,FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,input,role,((Number)frame.get("fromOrdinal")).longValue(),((Number)frame.get("throughOrdinal")).longValue(),now,revision);String run=StatsR27.sha256(VERSION+"|R335|"+view+'|'+revision+'|'+input+'|'+BuildConfig.SOURCE_COMMIT_R26);
  Map<String,Object> source=map();source.put("sourceContract",PairedReplayRunnerR30R1.contractJson(contract));source.put("viewId",view);source.put("inputHash",input);source.put("frame",frame);source.put("computationNs",costNs);source.put("quality","RESEARCH_OR_FORMAL_READONLY_OBSERVATION_NO_CERTIFICATE");String original=JsonR27.write(source);frame.put("windowScope",role==ResearchContractR30R1.Role.SELECT?"ORIGINAL_SELECT_RESEARCH_WINDOW":role==ResearchContractR30R1.Role.TRAIN?"ORIGINAL_FIXED_TRAIN_PROBE_WINDOW":"INPUT_CONTEXT_ONLY_FORMAL_RECORDS_HAVE_THEIR_OWN_ORIGINAL_BOUNDARIES_NO_FORMAL_SCORE_DENOMINATOR_INFERRED");frame.put("runId",run);frame.put("originalMachineFrameSha256",input);frame.put("machineMeasure",measure);frame.put("computationNs",costNs);String payload=JsonR27.write(ResearchFrameBoundR30R1.project(frame)),hash=StatsR27.sha256(payload);ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();b.guardBefore(db->{ResearchInputPinR30R1.require(db,c.data.datasetHash,revision);return 0;});
  b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,c.data.datasetHash,policy,role.name(),BuildConfig.SOURCE_COMMIT_R26,revision,contract.fromOrdinal,contract.throughOrdinal,input,"COMMITTED",original,now);b.add("INSERT OR IGNORE INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",run,view,measure,policy,"PREDECLARED_RESEARCH_OR_READONLY_FORMAL_SCOPE",contract.fromOrdinal,contract.throughOrdinal,role==ResearchContractR30R1.Role.SELECT?c.split.selectSize():0,null,null,null,original);b.add("INSERT OR IGNORE INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",view,hash,run,payload,hash,now);return b;
 }
 /** Same logical frame must keep its first original availability and measured cost on rediscovery. */
 @SuppressWarnings("unchecked") static ResearchWriteQueueR27.Batch productR335(SQLiteDatabase db,String view,String measure,EvalContextR27 c,long revision,Map<String,Object> frame,long costNs,long now){
  classifyFrameR335(frame);String input=StatsR27.sha256(JsonR27.write(frame)),run=StatsR27.sha256(VERSION+"|R335|"+view+'|'+revision+'|'+input+'|'+BuildConfig.SOURCE_COMMIT_R26);
  try(Cursor q=db.rawQuery("SELECT contract_hash,status FROM r30r1_analysis_run WHERE run_id=?",new String[]{run})){
   if(!q.moveToFirst())return product(view,measure,c,revision,frame,costNs,now);
   if(!input.equals(q.getString(0))||!"COMMITTED".equals(q.getString(1)))throw new IllegalStateException("R335_RESEARCH_IMMUTABLE_FRAME_DRIFT");
  }
  ResearchOriginalR335 original=ResearchOriginalR335.read(db,run,null);
  Map<String,Object> source=original.fields;
  if(!view.equals(source.get("viewId"))||!input.equals(source.get("inputHash"))||!input.equals(original.frameHash))
   throw new IllegalStateException("R335_RESEARCH_ORIGINAL_MISMATCH");
  String role=String.valueOf(frame.get("role"));frame.put("windowScope",role.equals("SELECT")?"ORIGINAL_SELECT_RESEARCH_WINDOW":role.equals("TRAIN")?"ORIGINAL_FIXED_TRAIN_PROBE_WINDOW":"INPUT_CONTEXT_ONLY_FORMAL_RECORDS_HAVE_THEIR_OWN_ORIGINAL_BOUNDARIES_NO_FORMAL_SCORE_DENOMINATOR_INFERRED");frame.put("runId",run);frame.put("originalMachineFrameSha256",input);frame.put("machineMeasure",measure);frame.put("computationNs",source.get("computationNs"));String payload=JsonR27.write(ResearchFrameBoundR30R1.project(frame)),hash=StatsR27.sha256(payload),originalHash=original.originalHash;
  ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();batch.guardBefore(tx->{ResearchInputPinR30R1.require(tx,c.data.datasetHash,revision);return originalHash.equals(ResearchOriginalR335.read(tx,run,null).originalHash)?0:-1;});batch.add("INSERT OR IGNORE INTO r30r1_chart_cache VALUES(?,?,?,?,?,?)",view,hash,run,payload,hash,now);return batch;
 }
 static final String SCHEDULER_R335="r335-research-scheduler";
 static final class ViewCursor {
  final String group,previous;
  final long updated;
  final int index;

  private ViewCursor(String group,String previous,long updated,int count) {
   this.group=group;
   this.previous=previous;
   this.updated=updated;
   int slot=0;
   if(previous!=null)try {slot=Integer.parseInt(previous);}catch(NumberFormatException ignored) {}
   this.index=Math.floorMod(slot,count);
  }

  static ViewCursor read(SQLiteDatabase db,String group,int count) {
   try(Cursor q=db.rawQuery("SELECT payload_json,updated_at FROM r27_chart_cache WHERE panel_id=? AND key=?",
           new String[]{SCHEDULER_R335,group})) {
    return q.moveToFirst()?new ViewCursor(group,q.getString(0),q.getLong(1),count):new ViewCursor(group,null,0,count);
   }
  }

  private boolean current(SQLiteDatabase db) {
   ViewCursor live=read(db,group,1);
   return Objects.equals(previous,live.previous)&&updated==live.updated;
  }

  String snapshot(String dependency) {
   return StatsR27.sha256(JsonR27.write(Arrays.asList(group,previous,updated,dependency)));
  }

  void advance(ResearchWriteQueueR27.Batch batch,int next,long wall) {
   batch.guardBefore(db->{if(!current(db))throw new ResearchStoreR30R1.StaleResearch();return 0;});
   long stamp=wall>updated?wall:Math.addExact(updated,1);
   batch.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",SCHEDULER_R335,group,Integer.toString(next),stamp);
  }

  // A replayed or superseded rotation is a no-op. A scan owns only a cursor, an optional
  // student-page hint and a missing-source diagnostic. It never creates a scientific receipt.
  void scan(ResearchPoolR27.Worker w,ResearchInputContextR33 page,int next,String errorView,String error) throws InterruptedException {
   RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();
   if(claim==null)throw new IllegalStateException("R31_DEPENDENCY_TASK_CLAIM_REQUIRED");
   long now=System.currentTimeMillis();
   ResearchWriteQueueR27.Batch hints=new ResearchWriteQueueR27.Batch();
   if(page!=null)page.appendScan(hints,now);
   if(errorView!=null)hints.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",SCHEDULER_R335,"SOURCE_ERROR_"+errorView,error,now);
   advance(hints,next,now);
   w.writes().commitViewHints(w,claim,db->{
    if(current(db))ResearchWriteQueueR27.execute(db,hints,"RESEARCH_VIEW_SCAN");
    return 0;
   });
  }
 }
 static void classifyFrameR335(Map<String,Object> frame){
  boolean formal=Boolean.TRUE.equals(frame.get("formalReadOnly"));long usable=0;
  Object raw=frame.get("points");if(raw instanceof List)for(Object o:(List<?>)raw)if(o instanceof Map){Map<?,?> p=(Map<?,?>)o;Object v=p.get("value"),n=p.get("n");boolean finite=v instanceof Number&&Double.isFinite(((Number)v).doubleValue());if(p.get("stack") instanceof List)for(Object cell:(List<?>)p.get("stack"))finite|=cell instanceof Number&&Double.isFinite(((Number)cell).doubleValue());if(finite&&n instanceof Number&&((Number)n).longValue()>0)usable++;}
  if("R16".equals(frame.get("viewId"))&&frame.get("actualTaskCpu") instanceof List)for(Object value:(List<?>)frame.get("actualTaskCpu"))if(value instanceof Map){Object cpu=((Map<?,?>)value).get("actualContinuationCpuNs");if(cpu instanceof Number&&Double.isFinite(((Number)cpu).doubleValue())&&((Number)cpu).doubleValue()>0)usable++;}
  frame.put("validPointCount",usable);frame.put("diagnosticFrameRecorded",true);frame.put("usableResearchProduct",!formal&&usable>0&&!"BLOCKED_DEPENDENCY".equals(frame.get("status")));
  if(!formal&&usable==0&&!"BLOCKED_DEPENDENCY".equals(frame.get("status"))){
   frame.put("status","INSUFFICIENT");
   if(!(frame.get("reason") instanceof String)||((String)frame.get("reason")).isEmpty())frame.put("reason","NO_SUPPORTED_NUMERICAL_POINT");
  }
 }
 static void appendHeadR335(ResearchWriteQueueR27.Batch b,String view,String dependency,Map<String,Object> frame,long now){
  String status=Boolean.TRUE.equals(frame.get("formalReadOnly"))?"FORMAL_READ_ONLY":Boolean.TRUE.equals(frame.get("usableResearchProduct"))?"PRODUCED":String.valueOf(frame.get("reason")).contains("PARENT")||String.valueOf(frame.get("reason")).contains("STUDENT")?"WAITING_STUDENT":"BLOCKED_DEPENDENCY".equals(frame.get("status"))?"WAITING_SOURCE":"INSUFFICIENT";
  b.guardBefore(db->{ResearchActionSchemaR33.requireReady(db);return 0;});
  b.add("INSERT OR REPLACE INTO r33_research_input_head(view_id,dependency_key,state,reason,source_run,producer_method,input_hash,output_hash,observed_at) VALUES(?,?,?,?,?,?,?,?,?)",view,dependency,status,String.valueOf(frame.getOrDefault("reason","")),frame.get("runId"),"ResearchObservatoryR30R1.step",frame.get("originalMachineFrameSha256"),frame.get("originalMachineFrameSha256"),now);
 }
 static String sourceWatermarkR335(SQLiteDatabase db,String view){
  String sql;
  switch(view){
   case "R09":sql="SELECT count(*)||':'||coalesce(max(rowid),0) FROM r27_expert_proposal";break;
   case "R10":sql="SELECT count(*)||':'||coalesce(max(rowid),0) FROM r27_lineage";break;
   case "R11":sql="SELECT count(*)||':'||coalesce(max(completed_at),0) FROM r30_frozen_confirm";break;
   case "R14":sql="SELECT (SELECT count(*)||':'||coalesce(max(recorded_at),0) FROM r34_atomnet_optimizer_step)||':'||(SELECT count(*)||':'||coalesce(max(rowid),0) FROM r30_model_surface)";break;
   case "R17":sql="SELECT count(*)||':'||coalesce(max(rowid),0) FROM r30r1_research_measure WHERE kind='FAILURE_MEMORY'";break;
   default:return Arrays.asList("R01","R02","R03","R04","R05","R07","R08","R12","R13","R15").contains(view)?"DATASET_AND_POLICIES":Long.toString(Math.floorDiv(ResearchStoreR30R1.calendar(db),64));
  }
  try(Cursor q=db.rawQuery(sql,null)){return (q.moveToFirst()?q.getString(0):"EMPTY")+":"+Math.floorDiv(ResearchStoreR30R1.calendar(db),64);}
 }
 static ResearchPoolR27.Outcome step(RuntimeR27 rt,ResearchPoolR27.Worker w)throws Exception{
  if(rt.nextModelStudyTurnR335() || rt.evoData==null){
   try{
    if(ModelReplayStudyR30R1.settle(w))return ResearchPoolR27.Outcome.progress(1,0);
    if(rt.atomSessionR30!=null&&ModelReplayStudyR30R1.bindSelectRequestR335(rt,w,rt.atomSessionR30))return ResearchPoolR27.Outcome.progress(1,0);
   }catch(RuntimeException fault){
    String reason=localFailureR341(fault);
    if(reason==null)throw fault;
    ViewCursor study=ViewCursor.read(w.ro,"MODEL_STUDY",1);
    study.scan(w,null,0,"MODEL_STUDY",viewFailureR341("MODEL_STUDY",reason));
   }
  }
  if(rt.evoData==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(IMMUTABLE_REPLAY_DATASET)",0,-1);
  int count=ResearchGraphSpecsR30R1.ALL.size();
  ViewCursor cursor=ViewCursor.read(w.ro,"OBSERVATORY",count);
  for(int scan=0;scan<count;scan++){
   int index=(cursor.index+scan)%count;ResearchGraphSpecsR30R1.Spec spec=ResearchGraphSpecsR30R1.ALL.get(index);if(spec.id.equals("R06"))continue;
   boolean needs=Arrays.asList("R05","R07","R08","R12","R13").contains(spec.id);
   try(ResearchInputContextR33 owned=ResearchInputContextR33.acquire(rt,w,"OBSERVATORY:"+spec.id,needs)){
    EvalContextR27 c=owned.context;long revision=owned.revision;StringBuilder ids=new StringBuilder();for(PolicySpecR27 p:owned.policies)ids.append(p.policyHash());
    Map<String,Object> formal=spec.id.equals("R18")?stored(spec.id,c,w.ro,rt):null;
    String receipts = formal == null ? sourceWatermarkR335(w.ro,spec.id)
            : StatsR27.sha256(JsonR27.write(Arrays.asList(formal.get("settlementAttributionScope"), formal.get("sourceRecords"))));
    String identity=StatsR27.sha256(VERSION+"|R335|"+spec.id+"|"+c.data.datasetHash+"|"+revision+"|"+ids+"|"+receipts+"|"+BuildConfig.SOURCE_COMMIT_R26);
    boolean decided=false;try(Cursor q=w.ro.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{PANEL,spec.id})){decided=q.moveToFirst()&&identity.equals(q.getString(0));}
    long now=System.currentTimeMillis();ResearchWriteQueueR27.Batch b;
    if(decided){
     cursor.scan(w,owned,(index+1)%count,null,null);
     return ResearchPoolR27.Outcome.idle("WAITING_NEW_RESEARCH_SOURCE_PAGE",0,-1);
    }
    long started=System.nanoTime();Map<String,Object> frame=formal!=null?formal:stored(spec.id,c,w.ro,rt);if(frame==null)frame=compute(spec.id,c,owned.policies,revision);
    frame.put("isolatedStudentFailures",owned.rejectedPolicies);if(spec.id.equals("R15"))frame.put("modelReplayPerformance",ModelReplayStudyR30R1.summary(w.ro));
    b=productR335(w.ro,spec.id,spec.measure,c,revision,frame,System.nanoTime()-started,now);appendHeadR335(b,spec.id,identity,frame,now);owned.appendScan(b,now);cursor.advance(b,(index+1)%count,now);
    b.add("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,?,?)",PANEL,spec.id,identity,now);
    b.add("DELETE FROM r27_chart_cache WHERE panel_id=? AND key=?",SCHEDULER_R335,"SOURCE_ERROR_"+spec.id);
    ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(c.split.selectTo,c.split.selectTo,0L,null,c.data.ordinals[c.split.selectTo-1],StatsR27.sha256(identity+"|"+spec.id)));
    try{RuntimeR27.submit(w,"RESEARCH_OBSERVATORY_"+spec.id,b,false,cursor.snapshot(identity));}catch(RuntimeException failure){if(ResearchStoreR30R1.stale(failure))return ResearchPoolR27.Outcome.idle("DEPENDENCY(OBSERVATORY_INPUT_CHANGED)",0,-1);throw failure;}
    return ResearchPoolR27.Outcome.progress(1,c.split.selectTo);
   }catch(RuntimeException fault){
    String reason=localFailureR341(fault);
    if(reason==null)throw fault;
    cursor.scan(w,null,(index+1)%count,spec.id,viewFailureR341(spec.id,reason));
    return ResearchPoolR27.Outcome.idle("DEPENDENCY(OBSERVATORY_SOURCE:"+spec.id+")",0,-1);
   }
  }
  return ResearchPoolR27.Outcome.idle("WAITING_NEW_RESEARCH_POPULATION_OR_INPUT",0,-1);
 }
 static String localFailureR341(RuntimeException fault){
  Error fatal=FailureDispositionR31.processFatalCause(fault);if(fatal!=null)throw fatal;
  String message=String.valueOf(fault.getMessage());
  if(fault instanceof android.database.sqlite.SQLiteException)
   return message.contains("no such table")||message.contains("no such column")?message:null;
  if(MatrixResearchR30R1.isStorageFaultR31(fault))return null;
  if((fault instanceof IllegalArgumentException||fault instanceof ArithmeticException||fault instanceof IndexOutOfBoundsException)
          &&FailureDispositionR31.classify(fault).disposition==FailureDispositionR31.Disposition.FAULT)
   return fault.getClass().getSimpleName()+":"+message;
  return null;
 }
 static String viewFailureR341(String view,String reason){
  Map<String,Object> failure=map();failure.put("viewId",view);failure.put("source",BuildConfig.SOURCE_COMMIT_R26);
  failure.put("reason",RuntimeFaultEvidenceR31.clean(reason,256));
  return JsonR27.write(failure);
 }
}
