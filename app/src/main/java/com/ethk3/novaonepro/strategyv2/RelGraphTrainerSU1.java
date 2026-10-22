package com.ethk3.novaonepro.strategyv2;

/** Every graph parameter and its Adam moments are trained; input facts, masks, support and receipt roots are read-only. */
public final class RelGraphTrainerSU1 {
    public static final String UPDATE="SU1_GDEEP_ADAM_CAUSAL_RESET_V1|BLOCK50|8_SETTLED_DRAWS|LR_001|CLIP_1|WINDOW_RESET";
    private RelGraphTrainerSU1() {}
    public static RelGraphOptimizerSU1 trainCausal(RelationTensorAdapterSU1.Factory factory,int[][] draws,int through,int width,int window,long seed,BudgetTokenSU1 budget){
        if(through<0||through>draws.length||window<1||window>32||width<2||width>8)throw new IllegalArgumentException("SU1_RELGRAPH_TRAIN_BOUNDARY");
        RelGraphOptimizerSU1 state=RelGraphOptimizerSU1.initial(RelGraphModelSU1.initial(width,2,2,2,RelationTensorAdapterSU1.SCHEMA,seed));
        for(int target=Math.max(window,through-8);target<through;target++){
            budget.checkpointOrYield();RelGraphModelSU1.Input input=factory.input(target,window,budget);RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(state.weights,input,draws[target],budget);state=state.apply(gradient,.001,1);
        }return state;
    }
}
