package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class RepairPairedResearchR30Test {
 static final String SOURCE="b225ab204db043b347f9d76d6c5c1376fd9a2590";
 static PolicySpecR27 parent(){return new PolicySpecR27(PolicyAstR27.op("add",PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1))),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.03,.01,IdeaFamilyR27.meta("I09","TEST",1,30,"HOST_SYNTHETIC_RESEARCH_FIXTURE"));}
 static EvalContextR27 context(){ReplayDatasetR27 d=ReplayDatasetR27.synthetic(3000,30,new int[0]);return new EvalContextR27(new FeatureCacheR27(d),SplitR27.of(d,null),null);}
 static ResearchContractR30R1 contract(EvalContextR27 c,PolicySpecR27 p){return new ResearchContractR30R1(c.data.datasetHash,p.policyHash(),"TEST_ABI",SOURCE,StatsR27.sha256("input"),ResearchContractR30R1.Role.SELECT,c.data.ordinals[c.split.selectFrom],c.data.ordinals[c.split.selectTo-1],0,1);}
 @Test public void resumableRunnerMatchesActualOriginalProbabilitiesAndNeverReadsFormalLabels(){
  EvalContextR27 ctx=context();PolicySpecR27 p=parent();PairedReplayRunnerR30R1 r=new PairedReplayRunnerR30R1(contract(ctx,p),p,"root/0",ctx,1,1,64);r=PairedReplayRunnerR30R1.restore(r.snapshot(),ctx);
  while(!r.complete()){r.advance(ctx,127);r=PairedReplayRunnerR30R1.restore(r.snapshot(),ctx);}
  ResearchGainR30R1 expected=new ResearchGainR30R1(r.contract.fromOrdinal,r.contract.throughOrdinal,64,4,200);ReplayEvaluatorR27.Trace a=ReplayEvaluatorR27.selectTraceWithProbabilities(p,ctx,1),b=ReplayEvaluatorR27.selectTraceWithProbabilities(r.without,ctx,1);
  for(int t=ctx.split.selectFrom;t<ctx.split.selectTo;t++)if(!a.skipped[t]&&!b.skipped[t]){double la=0,lb=0;int actual=ctx.data.draws[t][p.position-1]-1;for(int k=0;k<10;k++){double y=k==actual?1:0;la+=.5*(a.probabilityRows[t][k]-y)*(a.probabilityRows[t][k]-y);lb+=.5*(b.probabilityRows[t][k]-y)*(b.probabilityRows[t][k]-y);}expected.add(ctx.data.ordinals[t],lb,la);}
  assertEquals(JsonR27.write(expected.blocks()),JsonR27.write(r.gain().blocks()));
  int[][] changed=ctx.data.draws.clone();for(int t=ctx.split.selectTo;t<changed.length;t++){changed[t]=changed[t].clone();int tmp=changed[t][0];changed[t][0]=changed[t][1];changed[t][1]=tmp;}ReplayDatasetR27 d=ctx.data.withDraws(changed);EvalContextR27 other=new EvalContextR27(new FeatureCacheR27(d),SplitR27.of(d,null),null);PairedReplayRunnerR30R1 z=new PairedReplayRunnerR30R1(contract(other,p),p,"root/0",other,1,1,64);while(!z.complete())z.advance(other,256);assertEquals(JsonR27.write(r.gain().blocks()),JsonR27.write(z.gain().blocks()));
  try{PairedReplayRunnerR30R1.restore(r.snapshot().replace("\"cursor\":"+r.cursor(),"\"cursor\":0"),ctx);fail();}catch(IllegalArgumentException expectedCorruption){}
 }
 @Test public void fixedCalendarBlocksDiscloseMissingAndTailWithoutSelectingGoodBlocks(){ResearchGainR30R1 g=new ResearchGainR30R1(1000,1255,64,4,200);for(long t=1000;t<=1255;t++)g.add(t,.2,.3);assertTrue(g.estimate().upper95<0);try{g.add(1000,.2,.3);fail();}catch(IllegalStateException expected){}ResearchGainR30R1 holes=new ResearchGainR30R1(1000,1329,64,4,200);for(long t=1000;t<=1329;t++)if(t!=1255)holes.add(t,.2,.3);assertEquals(1,holes.estimate().missingIssues);assertEquals(10,holes.estimate().excludedTailIssues);assertNotEquals(g.estimate().analysisContractHash,new ResearchGainR30R1(1000,1255,32,4,200).estimate().analysisContractHash);}
}
