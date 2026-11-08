package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Durable bounded two-arm replay. Bias updates exactly match the original evaluator; formal labels are inaccessible. */
public final class PairedReplayRunnerR30R1 {
 public static final String VERSION="R30_REPAIR_PAIRED_RUNNER_1";
 /** R31-B08-C03: RETRAIN_REMOVED trains the removed arm's own online state (original behaviour); MASK_FROZEN scores
  * the removed arm with the parent's state frozen at each step and never updates it. The two answer different
  * questions, so their results are kept and labelled separately. */
 public enum Protocol{RETRAIN_REMOVED,MASK_FROZEN}
 public final Protocol protocol;
 public final ResearchContractR30R1 contract;public final PolicySpecR27 parent,without;public final String path;public final double tau;public final int generation,blockPeriods,from,to;
 private int cursor;private final double[] withBias=new double[10],withoutBias=new double[10];
 private final List<Object> observations=new ArrayList<>();
 public PairedReplayRunnerR30R1(ResearchContractR30R1 c,PolicySpecR27 p,String path,EvalContextR27 ctx,double tau,int generation,int blockPeriods){this(c,p,path,ctx,tau,generation,blockPeriods,Protocol.RETRAIN_REMOVED);}
 public PairedReplayRunnerR30R1(ResearchContractR30R1 c,PolicySpecR27 p,String path,EvalContextR27 ctx,double tau,int generation,int blockPeriods,Protocol protocol){
  this.protocol=java.util.Objects.requireNonNull(protocol);
  c.requireAdvice(c.throughOrdinal+1,System.currentTimeMillis());if(c.role!=ResearchContractR30R1.Role.SELECT||!c.datasetHash.equals(ctx.data.datasetHash)||!c.policyHash.equals(p.policyHash())||!Double.isFinite(tau)||tau<=0||ctx.labels!=ctx.data.draws)throw new IllegalArgumentException("PAIRED_RUNNER_SOURCE_OR_ROLE");
  contract=c;parent=PolicySpecR27.parse(p.fullJson());without=DirectedResearchPolicyR30R1.withoutChild(parent,path,generation);this.path=path;this.tau=tau;this.generation=generation;this.blockPeriods=blockPeriods;from=ctx.split.selectFrom;to=ctx.split.selectTo;
  if(from>=to||to>10000||c.fromOrdinal!=ctx.data.ordinals[from]||c.throughOrdinal!=ctx.data.ordinals[to-1]||blockPeriods<1)throw new IllegalArgumentException("PAIRED_REGISTERED_WINDOW");
 }
 private boolean arm(PolicySpecR27 p,double[] scores,ReplayDatasetR27 d,int t,double[] bias,double[] output){
  double[] z=new double[10];if(!ReplayEvaluatorR27.standardize(scores,t,z))return false;
  ReplayEvaluatorR27.softmax(z,tau,bias,output);int actual=d.draws[t][p.position-1]-1;
  for(int k=0;k<10;k++)bias[k]=bias[k]*(1-p.forget)+p.lr*((k==actual?1:0)-output[k]);return true;
 }
 public int advance(EvalContextR27 ctx,int maxSamples){
  if(!contract.datasetHash.equals(ctx.data.datasetHash)||ctx.split.selectFrom!=from||ctx.split.selectTo!=to||ctx.labels!=ctx.data.draws||maxSamples<1||maxSamples>256)throw new IllegalArgumentException("PAIRED_SLICE_SOURCE_OR_BUDGET");
  double[] a=ctx.scores(parent),b=ctx.scores(without),pa=new double[10],pb=new double[10];int end=Math.min(to,cursor+maxSamples),start=cursor;
  for(;cursor<end;cursor++){CooperativeContinuationR26.poll();double[] frozen=protocol==Protocol.MASK_FROZEN?withBias.clone():null;boolean va=arm(parent,a,ctx.data,cursor,withBias,pa),vb=protocol==Protocol.MASK_FROZEN?maskedR31(b,cursor,frozen,pb):arm(without,b,ctx.data,cursor,withoutBias,pb);if(cursor<from||!va||!vb)continue;
   int[] order=new int[10];ReplayEvaluatorR27.top3(pa,order);int[] ka={order[0]+1,order[1]+1,order[2]+1};ReplayEvaluatorR27.top3(pb,order);int[] kb={order[0]+1,order[1]+1,order[2]+1};
   if(!ReplayEvaluatorR27.pitReplaySampleR30(parent,ctx.data,cursor,ka,pa)||!ReplayEvaluatorR27.pitReplaySampleR30(without,ctx.data,cursor,kb,pb))continue;
   double la=0,lb=0;int actual=ctx.data.draws[cursor][parent.position-1]-1;for(int k=0;k<10;k++){double y=k==actual?1:0;la+=.5*(pa[k]-y)*(pa[k]-y);lb+=.5*(pb[k]-y)*(pb[k]-y);}
   observations.add(Arrays.asList(ctx.data.ordinals[cursor],lb,la));
  }return cursor-start;
 }
 /** MASK_FROZEN arm: the parent's state before this step, no update of any state. */
 private boolean maskedR31(double[] scores,int t,double[] frozenParentBias,double[] output){double[] z=new double[10];if(!ReplayEvaluatorR27.standardize(scores,t,z))return false;ReplayEvaluatorR27.softmax(z,tau,frozenParentBias,output);return true;}
 /** The effect this runner can claim; asking for the other protocol's effect is refused. */
 public String effectClaimR31(Protocol asked){if(asked!=protocol)throw new IllegalArgumentException("R31_PAIRED_PROTOCOL_MISMATCH:"+protocol+"!="+asked);return protocol==Protocol.MASK_FROZEN?"MASKED_CONTRIBUTION_UNDER_SHARED_PARENT_STATE":"REMOVED_AND_SEPARATELY_RETRAINED_EFFECT";}
 public double[] biasR31(boolean withArm){return (withArm?withBias:withoutBias).clone();}
 public int cursor(){return cursor;}public boolean complete(){return cursor==to;}
 public ResearchGainR30R1 gain(){ResearchGainR30R1 g=new ResearchGainR30R1(contract.fromOrdinal,contract.throughOrdinal,blockPeriods,4,200);for(Object v:observations){List<?> row=(List<?>)v;g.add(((Number)row.get(0)).longValue(),((Number)row.get(1)).doubleValue(),((Number)row.get(2)).doubleValue());}return g;}
 public ResearchAdviceR30R1 advice(){if(!complete())throw new IllegalStateException("PAIRED_RUNNER_NOT_COMPLETE");return ResearchAdviceR30R1.component(contract,path,gain().estimate(),contract.throughOrdinal+1,System.currentTimeMillis());}
 static Map<String,Object> contractJson(ResearchContractR30R1 c){Map<String,Object> m=new LinkedHashMap<>();m.put("dataset",c.datasetHash);m.put("policy",c.policyHash);m.put("abi",c.abi);m.put("source",c.sourceCommit);m.put("input",c.inputHash);m.put("role",c.role.name());m.put("from",c.fromOrdinal);m.put("through",c.throughOrdinal);m.put("available",c.availableAt);m.put("revision",c.sourceRevision);return m;}
 static ResearchContractR30R1 contractParse(Map<?,?> c){return new ResearchContractR30R1((String)c.get("dataset"),(String)c.get("policy"),(String)c.get("abi"),(String)c.get("source"),(String)c.get("input"),ResearchContractR30R1.Role.valueOf((String)c.get("role")),((Number)c.get("from")).longValue(),((Number)c.get("through")).longValue(),((Number)c.get("available")).longValue(),((Number)c.get("revision")).longValue());}
 public String snapshot(){Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("contract",contractJson(contract));m.put("parent",parent.fullJson());m.put("without",without.fullJson());m.put("path",path);m.put("tau",tau);m.put("generation",generation);m.put("blockPeriods",blockPeriods);m.put("cursor",cursor);m.put("withBias",withBias);m.put("withoutBias",withoutBias);m.put("observations",observations);if(protocol!=Protocol.RETRAIN_REMOVED)m.put("protocol",protocol.name());String canonical=JsonR27.write(m);m.put("stateHash",StatsR27.sha256(canonical));String encoded=JsonR27.write(m);if(encoded.length()>1048576)throw new IllegalStateException("PAIRED_CHECKPOINT_BUDGET");return encoded;}
 @SuppressWarnings("unchecked")public static PairedReplayRunnerR30R1 restore(String text,EvalContextR27 ctx){
  if(text.length()>1048576)throw new IllegalArgumentException("PAIRED_CHECKPOINT_BUDGET");Map<String,Object> m=(Map<String,Object>)JsonR27.parse(text);Object hash=m.remove("stateHash");if(!VERSION.equals(m.get("version"))||!StatsR27.sha256(JsonR27.write(m)).equals(hash))throw new IllegalArgumentException("PAIRED_CHECKPOINT_HASH");
  PairedReplayRunnerR30R1 r=new PairedReplayRunnerR30R1(contractParse((Map<?,?>)m.get("contract")),PolicySpecR27.parse((String)m.get("parent")),(String)m.get("path"),ctx,((Number)m.get("tau")).doubleValue(),((Number)m.get("generation")).intValue(),((Number)m.get("blockPeriods")).intValue(),m.get("protocol")==null?Protocol.RETRAIN_REMOVED:Protocol.valueOf((String)m.get("protocol")));
  if(!r.without.fullJson().equals(m.get("without")))throw new IllegalArgumentException("PAIRED_CHECKPOINT_COUNTERFACTUAL");r.cursor=((Number)m.get("cursor")).intValue();if(r.cursor<0||r.cursor>r.to)throw new IllegalArgumentException("PAIRED_CHECKPOINT_CURSOR");
  for(String key:new String[]{"withBias","withoutBias"}){List<?> values=(List<?>)m.get(key);if(values.size()!=10)throw new IllegalArgumentException("PAIRED_CHECKPOINT_BIAS");double[] a=key.equals("withBias")?r.withBias:r.withoutBias;for(int k=0;k<10;k++){a[k]=((Number)values.get(k)).doubleValue();if(!Double.isFinite(a[k]))throw new IllegalArgumentException("PAIRED_CHECKPOINT_NONFINITE");}}
  List<?> rows=(List<?>)m.get("observations");if(rows.size()>r.to-r.from)throw new IllegalArgumentException("PAIRED_CHECKPOINT_OBSERVATIONS");long previous=-1;for(Object value:rows){List<?> row=(List<?>)value;long at=((Number)row.get(0)).longValue();if(row.size()!=3||at<=previous||at<r.contract.fromOrdinal||at>r.contract.throughOrdinal||r.cursor==0||at>=ctx.data.ordinals[r.cursor-1]+1)throw new IllegalArgumentException("PAIRED_CHECKPOINT_ORDER");previous=at;}r.observations.addAll((List<Object>)rows);r.gain();return r;
 }
}
