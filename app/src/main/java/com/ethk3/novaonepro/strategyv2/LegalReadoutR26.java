package com.ethk3.novaonepro.strategyv2;
/** Exact legal-permutation readout of a positive score table; output K3 mass is a genuine union marginal. */
public final class LegalReadoutR26 {
    private LegalReadoutR26() {}
    public static float[][] project(float[][] table){
        if(table==null||table.length!=10)throw new IllegalArgumentException("R26_PROBABILITY_SHAPE");double[][] score=new double[10][10];
        for(int p=0;p<10;p++){if(table[p].length!=10)throw new IllegalArgumentException("R26_PROBABILITY_SHAPE");for(int n=0;n<10;n++){double q=table[p][n];if(!Double.isFinite(q)||q<0||q>1.000001)throw new IllegalArgumentException("R26_PROBABILITY_RANGE");score[p][n]=Math.log(Math.max(1e-12,q));}}
        boolean coherent=true;for(int p=0;p<10;p++){double row=0,column=0;for(int n=0;n<10;n++){row+=table[p][n];column+=table[n][p];}if(Math.abs(row-1)>1e-5||Math.abs(column-1)>1e-5)coherent=false;}if(coherent){float[][] copy=new float[10][];for(int p=0;p<10;p++)copy[p]=table[p].clone();return copy;}
        ExactPermutationR26.Result result=ExactPermutationR26.infer(score,ExecutionBudgetR26.current());float[][] probability=new float[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)probability[p][n]=(float)result.marginal[p][n];return probability;
    }
}
