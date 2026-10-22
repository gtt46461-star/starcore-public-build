package com.ethk3.novaonepro.r952;

public final class CalibrationContractR952 {
    public static final int BINS=10;
    private CalibrationContractR952(){}
    public static Result evaluate(double[] probabilities,int[] labels){
        if(probabilities==null||labels==null||probabilities.length==0||probabilities.length!=labels.length)throw new IllegalArgumentException("R952_CAL_INPUT");
        double brier=0d;double[] sumP=new double[BINS];int[] sumY=new int[BINS],count=new int[BINS];
        for(int i=0;i<probabilities.length;i++){double p=probabilities[i];if(Double.isNaN(p)||Double.isInfinite(p)||p<0d||p>1d||(labels[i]!=0&&labels[i]!=1))throw new IllegalArgumentException("R952_CAL_VALUE");double e=p-labels[i];brier+=e*e;int bin=Math.min(BINS-1,(int)(p*BINS));sumP[bin]+=p;sumY[bin]+=labels[i];count[bin]++;}
        double ece=0d;for(int b=0;b<BINS;b++)if(count[b]>0)ece+=(double)count[b]/probabilities.length*Math.abs(sumP[b]/count[b]-(double)sumY[b]/count[b]);
        return new Result(brier/probabilities.length,ece,probabilities.length);
    }
    public static final class Result{public final double brier,ece;public final int n;Result(double b,double e,int count){brier=b;ece=e;n=count;}}
}
