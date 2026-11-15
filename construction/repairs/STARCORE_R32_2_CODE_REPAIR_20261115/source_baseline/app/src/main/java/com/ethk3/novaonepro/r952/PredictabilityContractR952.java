package com.ethk3.novaonepro.r952;

public final class PredictabilityContractR952 {
    private PredictabilityContractR952(){}
    public static Decision evaluate(int futureN,double wilsonLower,double brier,double baselineBrier,int independentWindows){
        if(futureN<0||independentWindows<0||!finite(wilsonLower)||!finite(brier)||!finite(baselineBrier))throw new IllegalArgumentException("R952_PREDICTABILITY_INPUT");
        if(futureN<120)return new Decision(false,"WAIT_FUTURE_N",0d);
        if(independentWindows<2)return new Decision(false,"WAIT_INDEPENDENT_WINDOWS",0d);
        if(wilsonLower<=0.30d)return new Decision(false,"WILSON_NOT_ABOVE_RANDOM",0d);
        if(brier>=baselineBrier)return new Decision(false,"BRIER_NOT_BETTER_BASELINE",0d);
        return new Decision(true,"EVIDENCE_READY",Math.min(1d,(wilsonLower-0.30d)*10d));
    }
    private static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
    public static final class Decision{public final boolean pass;public final String reason;public final double score;Decision(boolean p,String r,double s){pass=p;reason=r;score=s;}}
}
