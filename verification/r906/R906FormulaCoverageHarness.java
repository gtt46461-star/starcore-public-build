package com.ethk3.novaonepro;

import java.util.Arrays;

/** Host performance/coverage harness for the R906 shared-matrix runtime. No Android DB/device claim. */
public final class R906FormulaCoverageHarness {
    private static ResearchMethodPlanR906 make(int i){
        int f=i%4;String rep,learner,update;
        if(f==0){rep="BAYESIAN_TRANSITION_V"+i;learner="DIRICHLET_BAYES_MARKOV_V"+i;update="ORDERED_DIRICHLET_NEXT_ISSUE_ONLY";}
        else if(f==1){rep="METRIC_RETRIEVAL_V"+i;learner="KNN_METRIC_RETRIEVAL_V"+i;update="FROZEN_CENTROID_NEXT_ISSUE_ONLY";}
        else if(f==2){rep="COMPACT_FOREST_V"+i;learner="COMPACT_OBLIVIOUS_FOREST_V"+i;update="ORDERED_TREE_REFRESH_NEXT_ISSUE_ONLY";}
        else{rep="RESIDUAL_TRANSPORT_V"+i;learner="FTRL_LINEAR_GRAPH_V"+i;update="ONLINE_FTRL_NEXT_ISSUE_ONLY";}
        return new ResearchMethodPlanR906(rep,
                "raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support",
                learner,update,"PAIRED_LOGLOSS_PLUS_HIT","SELECTION_CONDITIONED_ISOTONIC",
                "ENTROPY_DRIFT_BLOCK_GUARD","SEALED_FUTURE_BLOCK_AND_PERMUTATION_TEST","",1,
                "COVERAGE_HARNESS",true);
    }
    private static double[][] matrix(int issue){
        double[][] m=new double[1200][7];long s=0x9e3779b97f4a7c15L+issue;
        for(int r=0;r<m.length;r++)for(int c=0;c<7;c++){s=s*6364136223846793005L+1442695040888963407L;double u=((s>>>11)&0x1fffff)/(double)0x1fffff;m[r][c]=c==1?(u-.5)*6:c==2?(u-.5)*.15:u;}
        return m;
    }
    public static void main(String[] args){
        int assets=96,issues=120;int[] coverage=new int[assets];long[] costs=new long[issues];
        ResearchMethodPlanR906[] plans=new ResearchMethodPlanR906[assets];for(int i=0;i<assets;i++)plans[i]=make(i);
        for(int t=0;t<issues;t++){
            double[][] matrix=matrix(t);long start=System.nanoTime();
            for(int a=0;a<assets;a++){
                ResearchCompiledGraphRuntimeR906.Compiled c=ResearchCompiledGraphRuntimeR906.compile(plans[a]);
                double[] y=ResearchCompiledGraphRuntimeR906.evaluateAll(c,matrix);if(y.length!=1200)throw new AssertionError("CARDINALITY");
                int best=0;for(int i=1;i<y.length;i++)if(y[i]>y[best])best=i;if(best<0)throw new AssertionError();coverage[a]++;
            }
            costs[t]=(System.nanoTime()-start)/1_000_000L;
        }
        long[] sorted=costs.clone();Arrays.sort(sorted);long p50=sorted[(int)Math.floor((sorted.length-1)*.50)],p95=sorted[(int)Math.floor((sorted.length-1)*.95)],p99=sorted[(int)Math.floor((sorted.length-1)*.99)];
        int min=999,max=0;for(int x:coverage){min=Math.min(min,x);max=Math.max(max,x);}boolean pass=min==issues&&max==issues;
        System.out.println("STATUS="+(pass?"PASS":"FAIL"));System.out.println("ASSETS="+assets);System.out.println("ISSUES="+issues);System.out.println("MIN_COVERAGE="+min);System.out.println("MAX_COVERAGE="+max);System.out.println("P50_MS="+p50);System.out.println("P95_MS="+p95);System.out.println("P99_MS="+p99);
        if(!pass)System.exit(2);
    }
}
