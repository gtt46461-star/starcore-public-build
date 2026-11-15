package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class ResearchFailureSU1Test {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(60000,100000000,null);}
    public static void main(String[] args){
        check(ResearchFailureSU1.code("SU1_PREEMPTED").category==ResearchFailureSU1.Category.RESOURCE&&!ResearchFailureSU1.code("SU1_PREEMPTED").rejectCandidate,"scheduler preemption became a model failure");
        check(!ResearchFailureSU1.code("unknown infrastructure error").rejectCandidate,"unknown failure silently rejected a candidate");
        check(ResearchFailureSU1.code("SU1_FITTED_MODEL_TRAINING_BOUNDARY").category==ResearchFailureSU1.Category.PIT_TIMING,"training cutoff is not a timing boundary");
        check(ResearchFailureSU1.code("SU1_CALIBRATION_NOT_VALIDATED").category==ResearchFailureSU1.Category.CALIBRATION,"missing calibration classified as ranking");
        check(ResearchFailureSU1.code("PAIRED_IMPROVEMENT_NOT_ESTABLISHED").category==ResearchFailureSU1.Category.RANKING_EVIDENCE,"evidence diagnosis missing");
        ResearchDirectorPolicySU1 baseline=new ResearchDirectorPolicySU1(),failureAware=new ResearchDirectorPolicySU1();baseline.context("fixed");failureAware.context("fixed");
        for(String arm:ResearchDirectorPolicySU1.FAMILIES){baseline.record(arm,"mutate",.3,.3,100,false);failureAware.record(arm,"mutate",.3,.3,100,arm.equals("EXPERT_MIX"));}
        Map<String,Integer> oneUsed=Collections.singletonMap("alreadyAllocated",1);
        check(baseline.pick(8,oneUsed,24).equals("EXPERT_MIX"),"baseline reward/cost tie changed");check(failureAware.pick(8,oneUsed,24).equals("RELATION"),"equal rewards/costs ignored the observed failure type");
        check(ResearchFailureSU1.code("SU1_GRAPH_NONFINITE_GRADIENT").schedulingPenalty()>ResearchFailureSU1.code("SU1_FITTED_MODEL_DEPENDENCY_MISSING").schedulingPenalty(),"controller penalty ignores the observed failure category");
        int[][] draws=new int[3400][10];for(int t=0;t<draws.length;t++)for(int p=0;p<10;p++)draws[t][p]=(p+t)%10+1;
        String data=HashingSU1.text("failure replay dataset"),text="MODEL("+HashingSU1.text("missing weights")+",8)";
        StrategyFoundrySU1.Genome missing=new StrategyFoundrySU1.Genome(text,"GRAMMAR","DEEP","mutate","",-1,1,false),valid=new StrategyFoundrySU1.Genome("REL(PLUS1,1,0,1)","GRAMMAR","RELATION","mutate","",-1,1,false);
        try(StrategyFoundrySU1 foundry=new StrategyFoundrySU1(draws,data,11,8,1)){
            foundry.startRefresh(Arrays.asList(missing,valid,missing));String before=foundry.checkpoint();
            try{foundry.next(new BudgetTokenSU1(1000,1000000,()->true));throw new AssertionError("preemption ignored");}catch(BudgetTokenSU1.Yield expected){check(before.equals(foundry.checkpoint()),"preemption changed attempts or policy");}
            StrategyFoundrySU1.Attempt rejected=foundry.next(budget());check(rejected.result.equals("REJECTED")&&rejected.detail.contains("DEPENDENCY"),"deterministic model failure aborted the campaign or lost its type");check(foundry.evaluated==0,"failed candidate credited as evaluated");
            String saved=foundry.checkpoint();try(StrategyFoundrySU1 restored=StrategyFoundrySU1.restore(draws,data,saved)){
                restored.restoreSeen(Collections.singleton(missing.key()));StrategyFoundrySU1.Attempt actual=restored.next(budget());check(actual.ranked!=null&&actual.result.equals("EVALUATED"),"campaign did not continue after failed model");
                StrategyFoundrySU1.Attempt duplicate=restored.next(budget());check(duplicate.result.equals("REJECTED")&&duplicate.detail.contains("REDUNDANT_PROPOSAL"),"restarted campaign repeated deterministic model failure instead of recording its duplicate");
            }
        }
        System.out.println("SU1_RESEARCH_FAILURE_PASS checks="+checks+" scope=typed_contracts_and_real_campaign_failure_recovery_no_causal_gain_claim");
    }
}
