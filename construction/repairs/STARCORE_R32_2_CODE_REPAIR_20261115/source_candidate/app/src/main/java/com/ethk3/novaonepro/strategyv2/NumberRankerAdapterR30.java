package com.ethk3.novaonepro.strategyv2;

/** The existing AtomNet number head is the sole learned number surface. Sorting stays in ReplayEvaluatorR27.
 * No separate ranker weights, heuristic blend, clipping or silent renormalization. */
public final class NumberRankerAdapterR30 {
    private NumberRankerAdapterR30(){}
    public static double[] probabilities(AtomNetR27 net,double[] inputs){
        if(net==null)throw new IllegalArgumentException("R30_NUMBER_MODEL_REQUIRED");
        return probabilities(net.forward(inputs,null));
    }
    public static double[] probabilities(AtomNetR27.Out output){
        if(output==null||output.pClass.length!=10)throw new IllegalArgumentException("R30_NUMBER_SURFACE_SHAPE");
        double[] p=output.pClass.clone();double sum=0;
        for(double value:p){if(!Double.isFinite(value)||value<0||value>1)throw new IllegalArgumentException("R30_NUMBER_SURFACE_RANGE");sum+=value;}
        if(Math.abs(sum-1)>PitGuardR30.PROB_SUM_TOLERANCE)throw new IllegalArgumentException("R30_NUMBER_SURFACE_SUM");
        return p;
    }
}
