package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.lang.reflect.*;

/** Executes actual compiled production pure decisions. Injected frames are test inputs, never source evidence. */
public final class ResearchConsumerR335Host {
 static int checks;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
 static Map<String,Object> row(Object... pairs){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
 static void reject(Runnable r){try{r.run();throw new AssertionError("accepted invalid source");}catch(IllegalArgumentException expected){checks++;}}
 static ResearchContextR30R1 context()throws Exception{Constructor<ResearchContextR30R1> c=ResearchContextR30R1.class.getDeclaredConstructor(String.class,long.class);c.setAccessible(true);return c.newInstance(StatsR27.sha256("fixture"),2L);}
 @SuppressWarnings("unchecked") static void frame(ResearchContextR30R1 c,String view,Map<String,Object> f)throws Exception{Field fields=ResearchContextR30R1.class.getDeclaredField("frames");fields.setAccessible(true);((Map<String,Object>)fields.get(c)).put(view,f);Field runs=ResearchContextR30R1.class.getDeclaredField("runs");runs.setAccessible(true);((Map<String,String>)runs.get(c)).put(view,StatsR27.sha256(view));}
 public static void main(String[] args)throws Exception{
  List<Object> costs=Arrays.asList(row("session","s","task","a","actualContinuationCpuNs",900_000_000L),row("session","s","task","b","actualContinuationCpuNs",100_000_000L),row("session","s","task","a","actualContinuationCpuNs",900_000_000L),row("session","old","task","c","actualContinuationCpuNs",Long.MAX_VALUE));
  Map<String,Integer> q=ResearchPoolR27.resourceQuantums(costs,"s");check(q.get("a")==100&&q.get("b")==200&&q.size()==2,"duplicate CPU does not bias denominator, old session excluded");
  check(ResearchPoolR27.resourceQuantums(Arrays.asList(row("session","s","task","a","actualContinuationCpuNs",Long.MAX_VALUE),row("session","s","task","b","actualContinuationCpuNs",Long.MAX_VALUE)),"s").get("a")==200,"long sum does not overflow");
  reject(()->ResearchPoolR27.resourceQuantums(Arrays.asList(row("session","s","task","a","actualContinuationCpuNs",1L),row("session","s","task","a","actualContinuationCpuNs",2L)),"s"));
  reject(()->ResearchPoolR27.resourceQuantums(Collections.singletonList(row("session","s","task","a","actualContinuationCpuNs",-1L)),"s"));
  ResearchContextR30R1 c=context();frame(c,"R01",row("cells",Collections.singletonList(row("position",1,"window",32,"supportCalendarIssues",256,"value",.1))));boolean used=false;for(int i=0;i<100;i++)used|=c.window(new StatsR27.SplitMix(i),128)==32;check(used,"actual R01 consumer changes supported window");
  frame(c,"R02",row("cells",Collections.singletonList(row("window",512,"lower95",.1))));for(int i=0;i<100;i++){int w=c.window(new StatsR27.SplitMix(i),128);check(w==128||w==512,"R02 priority or exploration");}
  frame(c,"R04",row("points",Collections.singletonList(row("position",2,"shortWindow",32,"longWindow",128,"n",300,"intersectionActs",80))));boolean intersection=false;for(int i=0;i<100;i++){int[] hint=c.intersectionHintR335(new StatsR27.SplitMix(i),2);if(hint!=null){check(hint[0]==32&&hint[1]==128,"actual intersection windows");intersection=true;}check(c.intersectionHintR335(new StatsR27.SplitMix(i),1)==null,"wrong target rejected");}check(intersection,"R04 consumed");
  PolicySpecR27 parent=new PolicySpecR27(PolicyAstR27.op("add",0,PolicyAstR27.leaf("freq",1,32),PolicyAstR27.leaf("gap",1)),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,.99,IdeaFamilyR27.meta("I09","TEST",1,1,"host"));
  PolicySpecR27 removed=DirectedResearchPolicyR30R1.counterfactualR31(parent,"root/0",2);String dataset=StatsR27.sha256("fixture");Map<String,Object> contract=row("dataset",dataset,"revision",2L,"abi",FrozenConfirmationR30.currentSemantics(),"source",com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,"role","SELECT","policy",parent.policyHash());
  Map<String,Object> negative=row("sourceContract",contract,"componentPath","root/0","mechanismHash",ResearchContractR30R1.hash(parent.policyHash()+"|root/0"),"counterfactualHash",removed.policyHash(),"completeBlocks",4,"missingIssues",0,"incompleteFullBlocks",0);
  Map<String,Object> point=row("parentHash",parent.policyHash(),"n",256,"upper",-.01,"negativeContribution",negative);frame(c,"R17",row("points",Collections.singletonList(point)));
  PolicySpecR27 pruned=c.measuredPrune(parent,2);check(pruned!=null&&pruned.policyHash().equals(removed.policyHash()),"negative WITH-component contribution selects the removal arm");check(!parent.policyHash().equals(pruned.policyHash()),"does not rewrite parent");
  point.put("upper",.01);check(c.measuredPrune(parent,2)==null,"unresolved contribution does not prune");point.put("upper",-.01);contract.put("revision",3L);check(c.measuredPrune(parent,2)==null,"new input revision requires retest");
  Map<String,Object> replay=row("protocol",ModelReplayStudyR30R1.VERSION,"role","SELECT","formalTablesRead",false,"status","DESCRIPTIVE_ONLINE_RESEARCH_ONLY","completeBlocks",4,"calendarIssues",256,"missingObservedIssues",0,"upper95",-.01);
  check("UNIFORM_63".equals(ModelReplayStudyR30R1.replayPolicy(replay,"UNIFORM_63")),"SELECT alarm cannot alter replay");replay.put("role","ONLINE_RESEARCH");check("OLD48_RECENT15".equals(ModelReplayStudyR30R1.replayPolicy(replay,"UNIFORM_63")),"original supported ONLINE gate retained");replay.put("missingObservedIssues",1);check("UNIFORM_63".equals(ModelReplayStudyR30R1.replayPolicy(replay,"UNIFORM_63")),"missing future observation keeps original policy");
  Map<String,Object> multi=row("viewId","R02","points",Collections.singletonList(row("value",null,"n",64,"stack",Arrays.asList(null,.1))));ResearchObservatoryR30R1.classifyFrameR335(multi);check(Boolean.TRUE.equals(multi.get("usableResearchProduct")),"later finite series is not falsely empty");
  Map<String,Object> absent=row("viewId","R02","points",Collections.singletonList(row("value",null,"n",64,"stack",Arrays.asList(null,null))));ResearchObservatoryR30R1.classifyFrameR335(absent);check(Boolean.FALSE.equals(absent.get("usableResearchProduct")),"all-null series remains insufficient");
  Map<String,Object> resource=row("viewId","R16","points",Collections.singletonList(row("value",1.0,"n",0)),"actualTaskCpu",Collections.singletonList(row("actualContinuationCpuNs",100L)));ResearchObservatoryR30R1.classifyFrameR335(resource);check(Boolean.TRUE.equals(resource.get("usableResearchProduct")),"zero pending work does not invalidate a measured CPU resource frame");
  check(!ResearchTableAdaptersR30R1.versionChangedR335(null,null),"unsupported pragma does not break all legacy API tables");
  check(ResearchTableAdaptersR30R1.versionStatusR335(null,null).startsWith("UNAVAILABLE_"),"unsupported is not fake version zero");
  check(ResearchTableAdaptersR30R1.versionStatusR335(1L,null).startsWith("UNAVAILABLE_"),"incomplete probe makes no consistency claim");
  check(ResearchTableAdaptersR30R1.versionChangedR335(1L,2L),"supported source version change requires retry");
  check(!ResearchTableAdaptersR30R1.versionChangedR335(2L,2L),"supported stable version accepted within one request");
  System.out.println("R335_ACTUAL_CONSUMER_PURE checks="+checks+" scope=ACTUAL_METHODS_SYNTHETIC_INPUTS_NO_ANDROID_DB_OR_NATURAL_QUALITY");
 }
}
