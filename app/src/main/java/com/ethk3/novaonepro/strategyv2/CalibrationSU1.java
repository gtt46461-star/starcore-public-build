package com.ethk3.novaonepro.strategyv2;

/** Frozen logistic calibration fitted and checked on disjoint time segments. No-data metrics are NaN. */
public final class CalibrationSU1 {
    public final double intercept,slope,brier,ece;
    public final int fitN,validationN;
    public final String hash;
    private CalibrationSU1(double a,double b,double brier,double ece,int fitN,int validationN,String source){intercept=a;slope=b;this.brier=brier;this.ece=ece;this.fitN=fitN;this.validationN=validationN;hash=HashingSU1.fields("SU1_CAL_LOGISTIC_V1",Double.toHexString(a),Double.toHexString(b),String.valueOf(fitN),String.valueOf(validationN),source);}
    private static double feature(double score){if(!Double.isFinite(score)||score<0||score>1)throw new IllegalArgumentException("SU1_CAL_SCORE");double p=Math.max(1e-6,Math.min(1-1e-6,score));return Math.log(p/(1-p));}
    private static double sigmoid(double x){return 1/(1+Math.exp(-Math.max(-40,Math.min(40,x))));}
    public double predict(double raw){return sigmoid(intercept+slope*feature(raw));}
    public static CalibrationSU1 fit(double[] scores,int[] hits,int trainEnd,int validationStart,String sourceHash){
        HashingSU1.sha(sourceHash);if(scores.length!=hits.length||trainEnd<120||validationStart<=trainEnd||scores.length-validationStart<120)throw new IllegalArgumentException("SU1_CAL_TIME_SPLIT");
        for(int h:hits)if(h!=0&&h!=1)throw new IllegalArgumentException("SU1_CAL_LABEL");double a=Math.log(.3/.7),b=0;
        for(int step=0;step<500;step++){double ga=0,gb=0;for(int i=0;i<trainEnd;i++){double x=feature(scores[i]),res=sigmoid(a+b*x)-hits[i];ga+=res/trainEnd;gb+=res*x/trainEnd;}a-=.05*ga;b-=.05*(gb+.001*b);}
        int n=scores.length-validationStart;double brier=0,ece=0;double[] prob=new double[10],actual=new double[10];int[] counts=new int[10];
        for(int i=validationStart;i<scores.length;i++){double p=sigmoid(a+b*feature(scores[i]));brier+=(p-hits[i])*(p-hits[i])/n;int bin=Math.min(9,(int)(p*10));prob[bin]+=p;actual[bin]+=hits[i];counts[bin]++;}
        for(int bin=0;bin<10;bin++)if(counts[bin]>0)ece+=Math.abs(prob[bin]-actual[bin])/n;
        return new CalibrationSU1(a,b,brier,ece,trainEnd,n,sourceHash);
    }
    public boolean valid(){return validationN>=120&&Double.isFinite(brier)&&Double.isFinite(ece)&&brier<=.24&&ece<=.04;}
}
