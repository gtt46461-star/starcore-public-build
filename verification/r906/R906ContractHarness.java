package com.ethk3.novaonepro;

import java.util.HashSet;
import java.util.Random;

/** Pure-host executable invariants for R906 contracts. No Android device claim. */
public final class R906ContractHarness {
    private static String issue(int i){return String.format(java.util.Locale.US,"20260827%04d",i);}
    private static String h(String x){return V51ChecksumEngine.sha256(x);}
    private static ResearchMethodPlanR906 plan(int i){
        String rep=i%4==0?"BAYESIAN_TRANSITION":i%4==1?"METRIC_RETRIEVAL":i%4==2?"COMPACT_FOREST":"RESIDUAL_TRANSPORT";
        String learner=i%4==0?"DIRICHLET_BAYES_MARKOV":i%4==1?"KNN_METRIC_RETRIEVAL":i%4==2?"COMPACT_OBLIVIOUS_FOREST":"FTRL_LINEAR_GRAPH";
        String update=i%4==0?"ORDERED_DIRICHLET_NEXT_ISSUE_ONLY":i%4==1?"FROZEN_CENTROID_NEXT_ISSUE_ONLY":i%4==2?"ORDERED_TREE_REFRESH_NEXT_ISSUE_ONLY":"ONLINE_FTRL_NEXT_ISSUE_ONLY";
        return new ResearchMethodPlanR906(rep,"raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support",learner,update,
                "PAIRED_LOGLOSS_PLUS_HIT","SELECTION_CONDITIONED_ISOTONIC","ENTROPY_DRIFT_BLOCK_GUARD","SEALED_FUTURE_BLOCK_AND_PERMUTATION_TEST","",1+i/4,"HOST_CONTRACT",true);
    }
    public static void main(String[] args){
        int eligible=0,excluded=0,settled=0;
        for(int i=1;i<=200;i++){
            String issue=issue(i);
            if(i%13==0){
                IssueClosureContractR906.Receipt r=new IssueClosureContractR906.Receipt(issue,h("d"+i),"","","",IssueClosureContractR906.Eligibility.EXCLUDED_NO_PREDRAW,IssueClosureContractR906.CoreStatus.EXCLUDED,0,-1,1000L+i);
                if(r.countsAsSettledPrediction())throw new AssertionError("EXCLUDED_COUNTED"); excluded++;
            }else{
                int hit=i%3==0?1:0;
                IssueClosureContractR906.Receipt r=new IssueClosureContractR906.Receipt(issue,h("d"+i),h("p"+i),h("o"+i),h("s"+i),IssueClosureContractR906.Eligibility.ELIGIBLE_PREDRAW,IssueClosureContractR906.CoreStatus.COMPLETED,(i%10)+1,hit,1000L+i);
                if(!r.countsAsSettledPrediction()||r.recordHash.length()!=64)throw new AssertionError("ELIGIBLE_NOT_COUNTED"); eligible++;settled++;
            }
        }
        if(eligible!=185||settled!=185||excluded!=15)throw new AssertionError("DENOMINATOR");
        if(!SettlementFailureClassR906.classify("database is locked").retryable())throw new AssertionError("LOCK_RETRY");
        if(SettlementFailureClassR906.classify("DRAW_HASH_MISMATCH").retryable())throw new AssertionError("CONFLICT_RETRY");
        if(SettlementFailureClassR906.classify("NO_PREDRAW").retryable())throw new AssertionError("EXCLUSION_RETRY");
        HashSet<String> names=new HashSet<>(),sigs=new HashSet<>();
        for(int i=0;i<1000;i++){
            ResearchMethodPlanR906 p=plan(i);String sig=ResearchMethodSignatureR906.of(p);String name=ResearchChineseNamingR906.name(p);
            if(name.length()==0||!name.equals(ResearchChineseNamingR906.name(p)))throw new AssertionError("NAME_DETERMINISM");
            names.add(ResearchChineseNamingR906.normalized(name));sigs.add(sig);
        }
        if(names.size()!=1000||sigs.size()!=4)throw new AssertionError("NAME_OR_SIGNATURE_SEMANTICS:"+names.size()+":"+sigs.size());
        // Latest-wins UI generation fence + four-card error truth.
        HomeTruthPresenterR906 presenter=new HomeTruthPresenterR906();Random rnd=new Random(906L);long latest=0;
        for(int i=0;i<10000;i++){
            long g=presenter.nextGeneration();latest=g;
            RuntimeTruthSnapshotR906 s=RuntimeTruthSnapshotR906.error("HOST_RACE_"+i);
            if(s.whyCards.length!=4||s.stateBoxes.length!=16)throw new AssertionError("UI_CARDINALITY");
            if(rnd.nextBoolean()&&g>1)presenter.accept(g-1,s);
            if(!presenter.accept(g,s))throw new AssertionError("LATEST_REJECTED");
        }
        if(presenter.renderedGeneration()!=latest)throw new AssertionError("LATEST_NOT_RENDERED");
        System.out.println("R906_CONTRACT_HARNESS=PASS");
        System.out.println("SETTLEMENT_ELIGIBLE="+eligible+"|SETTLED="+settled+"|EXCLUDED="+excluded);
        System.out.println("METHOD_NAMES_UNIQUE="+names.size()+"|SIGNATURES_UNIQUE="+sigs.size());
        System.out.println("UI_RACE_PERMUTATIONS=10000|WHY_CARDS=4|STATE_BOXES=16|LATEST_WINS=PASS");
    }
}
