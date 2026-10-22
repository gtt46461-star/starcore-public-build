package com.ethk3.novaonepro.strategyv2;

/** Closed kernel registry. A graph artifact is never silently evaluated by the raw-number kernel. */
public final class ModelKernelRegistrySU1 {
    public static final String RELSTATE="RELSTATE10_SU1",RELGRAPH="RELGRAPH10_SU1";
    private ModelKernelRegistrySU1() {}
    public static String protocol(String modelKey){
        if(RELSTATE.equals(modelKey))return "SU1-RELSTATE10-REFERENCE-V1|FLOAT64|WINDOW_RESET";
        if(RELGRAPH.equals(modelKey))return RelGraphModelSU1.PROTOCOL;
        throw new IllegalArgumentException("SU1_UNKNOWN_MODEL_KERNEL:"+modelKey);
    }
    public static double[][] predict(String modelKey,RelationalStateInference.Weights weights,int[][] history){
        if(!RELSTATE.equals(modelKey))throw new IllegalArgumentException("SU1_GRAPH_REQUIRES_TYPED_RELATION_INPUT");return RelationalStateInference.predict(weights,history);
    }
    public static double[][] predict(String modelKey,RelGraphModelSU1.Weights weights,RelGraphModelSU1.Input input,BudgetTokenSU1 budget){
        if(!RELGRAPH.equals(modelKey))throw new IllegalArgumentException("SU1_RAW_NUMBER_KERNEL_CANNOT_CONSUME_GRAPH_WEIGHTS");return RelGraphModelSU1.predict(weights,input,budget);
    }
}
