package com.ethk3.novaonepro;

import java.util.Locale;

public final class R906MethodParityHarness {
    private static ResearchMethodPlanR906 plan(String rep,String learner,String update){
        return new ResearchMethodPlanR906(rep,
                "raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support",
                learner,update,"PAIRED_LOGLOSS_PLUS_HIT","SELECTION_CONDITIONED_ISOTONIC",
                "ENTROPY_DRIFT_BLOCK_GUARD","SEALED_FUTURE_BLOCK_AND_PERMUTATION_TEST","",1,
                "PARITY_HARNESS",true);
    }
    public static void main(String[] args){
        ResearchMethodPlanR906[] plans={
            plan("BAYESIAN_TRANSITION","DIRICHLET_BAYES_MARKOV","ORDERED_DIRICHLET_NEXT_ISSUE_ONLY"),
            plan("METRIC_RETRIEVAL","KNN_METRIC_RETRIEVAL","FROZEN_CENTROID_NEXT_ISSUE_ONLY"),
            plan("COMPACT_FOREST","COMPACT_OBLIVIOUS_FOREST","ORDERED_TREE_REFRESH_NEXT_ISSUE_ONLY"),
            plan("RESIDUAL_TRANSPORT","FTRL_LINEAR_GRAPH","ONLINE_FTRL_NEXT_ISSUE_ONLY")};
        double[][] fixtures={{.30,.10,.02,.85,.62,.55,.48},{.42,1.8,.04,.65,.78,.72,.69},{.19,-1.2,-.03,.93,.35,.28,.31}};
        for(ResearchMethodPlanR906 p:plans){
            ResearchCompiledGraphRuntimeR906.Compiled c=ResearchCompiledGraphRuntimeR906.compile(p);
            System.out.print(c.family.name()+"\t"+p.canonical()+"\t"+ResearchMethodSignatureR906.of(p)+"\t"+c.artifactHash);
            for(double[] x:fixtures)System.out.print("\t"+String.format(Locale.US,"%.17g",ResearchCompiledGraphRuntimeR906.evaluate(c,x)));
            System.out.println();
        }
    }
}
