package com.ethk3.novaonepro;

/** Shared exact linear scoring contract for Android and offline JVM parity. */
public final class RankerMathR925 {
    private RankerMathR925() {}
    public static double score(double[] x,double[] mean,double[] scale,double[] weight,double bias){
        if(x==null||x.length!=96||mean.length!=96||scale.length!=96||weight.length!=96)throw new IllegalArgumentException("META96_DIMENSION");
        double z=bias;
        for(int i=0;i<96;i++){
            if(!finite(x[i])||!finite(mean[i])||!finite(scale[i])||!finite(weight[i])||scale[i]<=0)throw new IllegalArgumentException("NONFINITE_MODEL_INPUT");
            z+=((x[i]-mean[i])/Math.max(1e-9,scale[i]))*weight[i];
        }
        if(!finite(z))throw new IllegalArgumentException("NONFINITE_SCORE");return z;
    }
    private static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
}
