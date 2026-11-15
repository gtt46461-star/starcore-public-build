package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Each relation timestep is rebuilt from its own prefix; a current matrix is never broadcast into the past. */
public final class RelationTensorAdapterSU1 {
    public static final String SCHEMA=HashingSU1.text("SU1_RELGRAPH_FEATURES_V1|POSITION_NUMBER_EVENTS|FREQ24:PROBABILITY_ESTIMATE|XT3_1000:PROBABILITY_ESTIMATE|MASK_SEPARATE|XT3_XT1_CONTEXT_HYPEREDGES_LOG_LIFT|2_ROLES_2_TYPES|SUPPORT_REAL_ISSUES|FIXED_SCALE_NO_FIT");
    private RelationTensorAdapterSU1() {}
    public static final class Factory {
        private final int[][] draws;
        private final long[] issues,available;
        private final long target,cutoff;
        private final String provenance;
        private final SparseConditionalCountsSU1 counts,secondCounts;
        private final LinkedHashMap<Integer,RelGraphModelSU1.Step> cache=new LinkedHashMap<>(128,.75f,true);
        private final List<String> roots=new ArrayList<>();
        public Factory(int[][] draws){
            if(draws==null||draws.length>20000)throw new IllegalArgumentException("SU1_GRAPH_DATASET");this.draws=draws;issues=new long[draws.length];available=new long[draws.length];for(int i=0;i<draws.length;i++){issues[i]=i+1;available[i]=i+1;}target=draws.length+1;cutoff=Math.max(1,draws.length);provenance="HISTORICAL_RECONSTRUCTED";
            counts=new SparseConditionalCountsSU1(draws,RelationSpecSU1.xt(3),HashingSU1.text("RELGRAPH_PREFIX_OWNED_COUNT_STATE_V1"));
            secondCounts=new SparseConditionalCountsSU1(draws,RelationSpecSU1.xt(1),HashingSU1.text("RELGRAPH_PREFIX_OWNED_COUNT_STATE_V1"));
        }
        public Factory(EventFrameSU1 frame){
            draws=frame.history();issues=new long[frame.size()];available=new long[frame.size()];for(int i=0;i<frame.size();i++){issues[i]=frame.issue(i);available[i]=frame.availableAt(i);}target=frame.targetIssue;cutoff=frame.cutoff;provenance="ONLINE_FROZEN";
            counts=new SparseConditionalCountsSU1(draws,RelationSpecSU1.xt(3),frame.sourceVersionHash);
            secondCounts=new SparseConditionalCountsSU1(draws,RelationSpecSU1.xt(1),frame.sourceVersionHash);
        }
        public boolean sameNumbers(int[][] other){return Arrays.deepEquals(draws,other);}
        private String root(int u){
            while(roots.size()<=u){int i=roots.size();RelationalStateInference.validateDraw(draws[i]);StringBuilder b=new StringBuilder(i==0?SCHEMA:roots.get(i-1)).append('|').append(issues[i]).append('|').append(available[i]);for(int n:draws[i])b.append('|').append(n);roots.add(HashingSU1.text(b.toString()));}return roots.get(u);
        }
        public RelGraphModelSU1.Step step(int u,BudgetTokenSU1 budget){
            if(u<0||u>=draws.length)throw new IllegalArgumentException("SU1_GRAPH_PREFIX_INDEX");RelGraphModelSU1.Step existing=cache.get(u);if(existing!=null)return existing;
            budget.checkpointOrYield();SparseConditionalCountsSU1.Estimate estimate=counts.at(u+1,budget),otherEstimate=secondCounts.at(u+1,budget);double[][] probabilities=estimate.probabilities(),otherProbabilities=otherEstimate.probabilities();int[] positions=new int[100],numbers=new int[100];double[] features=new double[200];boolean[] mask=new boolean[200];RelGraphModelSU1.Edge[] edges=new RelGraphModelSU1.Edge[200];String root=root(u);
            for(int p=0;p<10;p++)for(int n=0;n<10;n++){
                int node=p*10+n;positions[node]=p;numbers[node]=n;int lo=Math.max(0,u-23),k=0;for(int at=lo;at<=u;at++)if(draws[at][p]==n+1)k++;
                features[node*2]=k/(double)(u-lo+1);features[node*2+1]=probabilities[p][n];mask[node*2]=true;mask[node*2+1]=estimate.support(p)>0;
                for(int type=0;type<2;type++){int q=(p+(type==0?3:1))%10,r=(q+5)%10,source=q*10+draws[u][q]-1,context=r*10+draws[u][r]-1;double probability=type==0?probabilities[p][n]:otherProbabilities[p][n];int support=type==0?estimate.support(p):otherEstimate.support(p);edges[node*2+type]=new RelGraphModelSU1.Edge(node,type,new int[]{source,context},new int[]{0,1},Math.log(Math.max(1e-12,probability)/.1),support,issues[u],root);}
            }
            RelGraphModelSU1.Step step=new RelGraphModelSU1.Step(issues[u],available[u],root,draws[u],positions,numbers,features,mask,2,edges);cache.put(u,step);while(cache.size()>128)cache.remove(cache.keySet().iterator().next());return step;
        }
        public RelGraphModelSU1.Input input(int throughExclusive,int window,BudgetTokenSU1 budget){
            if(window<1||window>256||throughExclusive<window||throughExclusive>draws.length)throw new IllegalArgumentException("SU1_GRAPH_COMPLETE_WINDOW_REQUIRED");RelGraphModelSU1.Step[] steps=new RelGraphModelSU1.Step[window];for(int i=0;i<window;i++)steps[i]=step(throughExclusive-window+i,budget);
            // A historical training slice uses its own target and reconstructed availability; a live slice binds the actual frozen target.
            if(throughExclusive==draws.length&&provenance.equals("ONLINE_FROZEN"))return new RelGraphModelSU1.Input(SCHEMA,target,cutoff,provenance,steps);
            long t=throughExclusive<issues.length?issues[throughExclusive]:target,availableCutoff=0;for(RelGraphModelSU1.Step step:steps)availableCutoff=Math.max(availableCutoff,step.availableAt);
            return new RelGraphModelSU1.Input(SCHEMA,t,availableCutoff,"HISTORICAL_RECONSTRUCTED",steps);
        }
    }
}
