package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public final class ResearchCoreTest {
 static int checks;
 static void ok(boolean x,String message){checks++;if(!x)throw new AssertionError(message);}
 static void reject(Runnable r,String message){boolean rejected=false;try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){rejected=true;}ok(rejected,message);}
 static String h(String s){return ResearchContractR30R1.hash(s);}
 static ResearchContractR30R1 contract(ResearchContractR30R1.Role role){return new ResearchContractR30R1(h("data"),h("parent"),"ABI", "b225ab204db043b347f9d76d6c5c1376fd9a2590",h("input"),role,1000,1199,2000,1);}
 public static void main(String[] args)throws Exception{
  for(ResearchContractR30R1.Role role:ResearchContractR30R1.Role.values()){
   if(role==ResearchContractR30R1.Role.TRAIN||role==ResearchContractR30R1.Role.SELECT||role==ResearchContractR30R1.Role.ONLINE_RESEARCH){contract(role).requireAdvice(1200,2000);checks++;}
   else reject(()->contract(role).requireAdvice(1200,2000),"FORMAL/SYNTHETIC_ROLE_LEAK");
  }
  reject(()->contract(ResearchContractR30R1.Role.SELECT).requireAdvice(1199,2000),"FUTURE_FEATURE");
  reject(()->contract(ResearchContractR30R1.Role.SELECT).requireAdvice(1200,1999),"UNAVAILABLE_INPUT");
  ok(!contract(ResearchContractR30R1.Role.TRAIN).key("v").equals(contract(ResearchContractR30R1.Role.SELECT).key("v")),"ROLE_NOT_IN_IDENTITY");
  ok(ResearchGainR30R1.critical95(3)>3.18&&ResearchGainR30R1.critical95(3)<3.19,"TWO_SIDED_95_CRITICAL");
  ResearchGainR30R1 gain=new ResearchGainR30R1(1000,1199,50,4,200);
  for(int i=1000;i<1200;i++)gain.add(i,.2,.3);
  ResearchGainR30R1.Estimate e=gain.estimate();ok(e.completeBlocks==4&&e.observedCalendarIssues==200,"ISSUE_CLUSTERING");ok(e.upper95<0,"HARMFUL_COMPONENT_NOT_FOUND");
  ResearchAdviceR30R1 a=ResearchAdviceR30R1.component(contract(ResearchContractR30R1.Role.SELECT),"root/0",e,1200,2000);
  ok(a.action==ResearchAdviceR30R1.Action.PRUNE_COMPONENT,"MEASURED_NEGATIVE_ADVICE");
  reject(()->gain.add(1000,.2,.3),"DUPLICATE_ISSUE_ACCEPTED");
  reject(()->gain.add(1200,.2,.3),"UNREGISTERED_WINDOW");
  reject(()->ResearchAdviceR30R1.component(contract(ResearchContractR30R1.Role.CONFIRM),"root/0",e,1200,2000),"CONFIRM_ADAPTATION");
  ResearchGainR30R1 holes=new ResearchGainR30R1(1000,1249,50,4,200);
  for(int i=1000;i<1250;i++)if(i!=1249)holes.add(i,.2,.3);
  ResearchGainR30R1.Estimate gap=holes.estimate();ok(gap.supported()&&gap.missingIssues==1&&gap.incompleteBlocks==1,"GAP_DISCLOSURE");
  ResearchContractR30R1 gapContract=new ResearchContractR30R1(h("data"),h("parent"),"ABI","b225ab204db043b347f9d76d6c5c1376fd9a2590",h("input"),ResearchContractR30R1.Role.SELECT,1000,1249,2000,1);
  reject(()->ResearchAdviceR30R1.component(contract(ResearchContractR30R1.Role.SELECT),"root/0",gap,1250,2000),"GAIN_WINDOW_NOT_BOUND");
  ok(ResearchAdviceR30R1.component(gapContract,"root/0",gap,1250,2000).action==ResearchAdviceR30R1.Action.NONE,"SELECTIVE_COMPLETE_BLOCK_ADVICE");
  ResearchGainR30R1 tail=new ResearchGainR30R1(1000,1229,50,4,200);for(int i=1000;i<1230;i++)tail.add(i,.2,.3);
  ok(tail.estimate().supported()&&tail.estimate().evaluatedIssues==200&&tail.estimate().excludedTailIssues==30&&tail.estimate().incompleteFullBlocks==0,"PREDECLARED_TAIL_RULE");
  ResearchGainR30R1 noise=new ResearchGainR30R1(1000,1199,50,4,200);
  for(int i=1000;i<1200;i++)noise.add(i,.2,.2);
  ok(ResearchAdviceR30R1.component(contract(ResearchContractR30R1.Role.SELECT),"root/0",noise.estimate(),1200,2000).action==ResearchAdviceR30R1.Action.NONE,"ZERO_GAIN_CLAIM");
  TaskProgressR30R1 p=new TaskProgressR30R1(TaskProgressR30R1.Unit.PHASE,18460,18461,1,1190,1199);
  ok(p.state(1200,false,true)==TaskProgressR30R1.State.RUNNING,"WORK_UNIT_AS_CALENDAR");ok(p.oldestLag(1200)==10L,"BACKLOG_OLDEST");
  ok(p.state(1204,false,false)==TaskProgressR30R1.State.STALLED,"FIVE_PERIOD_STALL");
  ok(p.state(1204,true,false)==TaskProgressR30R1.State.BLOCKED_DEPENDENCY,"DEPENDENCY_FALSE_STALL");
  TaskProgressR30R1 idle=new TaskProgressR30R1(TaskProgressR30R1.Unit.POLICY,12,12,0,-1,1);
  ok(idle.state(1200,false,false)==TaskProgressR30R1.State.IDLE_WAITING_INPUT,"IDLE_FALSE_STALL");
  ok(idle.oldestLag(1200)==null,"IDLE_AGE_NOT_APPLICABLE");
  byte[] state={1,2,3};CommitEnvelopeR30R1 env=new CommitEnvelopeR30R1(h("step"),2,3,state);state[0]=9;
  reject(env::publishedState,"PUBLISHED_BEFORE_COMMIT");
  ok(env.attempt((id,f,r,b)->CommitEnvelopeR30R1.Outcome.RETRY_LATER)==CommitEnvelopeR30R1.Outcome.RETRY_LATER,"RETRY_RESULT");
  reject(env::publishedState,"RETRY_PUBLISHED_STATE");
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger commits=new AtomicInteger();
  Thread t=new Thread(()->env.attempt((id,f,r,b)->{entered.countDown();try{release.await();}catch(InterruptedException x){throw new IllegalStateException(x);}commits.incrementAndGet();return CommitEnvelopeR30R1.Outcome.COMMITTED;}));t.start();entered.await();
  ok(env.attempt((id,f,r,b)->{throw new AssertionError("CONCURRENT_DOUBLE_COMMIT");})==CommitEnvelopeR30R1.Outcome.RETRY_LATER,"CONCURRENT_ATTEMPT");release.countDown();t.join();
  ok(commits.get()==1&&env.publishedState()[0]==1,"COMMIT_OR_DEFENSIVE_COPY");
  env.attempt((id,f,r,b)->{throw new AssertionError("COMMITTED_REPLAY");});checks++;
  CommitEnvelopeR30R1 conflict=new CommitEnvelopeR30R1(h("other"),2,3,new byte[]{1});conflict.attempt((id,f,r,b)->CommitEnvelopeR30R1.Outcome.SOURCE_CONFLICT);reject(conflict::publishedState,"CONFLICT_PUBLISH");
  ok(ConfirmationViewR30R1.state("DONE","COMPLETED",false,0).equals("COMPLETED_NO_VERIFIED_IMPROVEMENT"),"NEGATIVE_CONFIRMATION_MISCLASSIFIED");
  ok(ConfirmationViewR30R1.state("DONE","INSUFFICIENT",false,0).equals("COMPLETED_WITH_INSUFFICIENT_EVIDENCE"),"INSUFFICIENT_AS_NEGATIVE");
  ok(ResearchGraphSpecsR30R1.ALL.size()==18,"GRAPH_COUNT");for(int g=0;g<3;g++)ok(ResearchGraphSpecsR30R1.group(g).size()==6,"GROUP_LIMIT");
  Set<String> ids=new HashSet<>();for(ResearchGraphSpecsR30R1.Spec spec:ResearchGraphSpecsR30R1.ALL){ok(ids.add(spec.id),"DUPLICATE_GRAPH_ID");ok(!spec.consumer.isEmpty()&&!spec.verification.isEmpty(),"DECORATIVE_GRAPH");}
  System.out.println("RESEARCH_CORE_HOST_PASS checks="+checks+" scope=core_role_statistics_atomicity_and_graph_contracts_not_Android_or_natural_quality");
 }
}
