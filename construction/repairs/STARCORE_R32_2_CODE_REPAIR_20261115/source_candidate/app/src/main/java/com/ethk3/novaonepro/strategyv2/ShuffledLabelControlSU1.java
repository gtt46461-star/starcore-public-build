package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Negative control: causal feature rows stay fixed; only already available training labels are permuted. */
public final class ShuffledLabelControlSU1 {
    public static final String PROTOCOL="SU1_SHUFFLED_LABEL_WINDOW1000_V1";
    private ShuffledLabelControlSU1() {}
    public static double[][] score(int[][] history,String seed,BudgetTokenSU1 budget){
        HashingSU1.sha(seed);int n=history.length;double[][] out=new double[10][10];if(n<3)return out;
        int first=Math.max(1,n-1000),length=n-first;int[] labels=new int[length];for(int i=0;i<length;i++)labels[i]=first+i;
        Random random=new Random(Long.parseUnsignedLong(seed.substring(0,16),16));for(int i=length-1;i>0;i--){int j=random.nextInt(i+1),v=labels[i];labels[i]=labels[j];labels[j]=v;}
        for(int p=0;p<10;p++){budget.checkpointOrYield();int source=history[n-1][p],support=0;for(int i=0;i<length;i++)if(history[first+i-1][p]==source){out[p][history[labels[i]][p]-1]++;support++;}for(int d=0;d<10;d++)out[p][d]=(out[p][d]+1)/(support+10d);}
        return ProgramTreeSU1.Experts.znorm(out);
    }
}
