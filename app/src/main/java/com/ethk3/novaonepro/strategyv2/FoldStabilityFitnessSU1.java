package com.ethk3.novaonepro.strategyv2;

/** Search objective only; these descriptive bounds never issue a quality certificate. */
public final class FoldStabilityFitnessSU1 {
    private FoldStabilityFitnessSU1() {}
    public static double wilsonLower(int k,int n) {
        if(n<0||k<0||k>n)throw new IllegalArgumentException("SU1_COUNTS");if(n==0)return 0;
        double p=k/(double)n,z=1.6449,den=1+z*z/n;
        return (p+z*z/(2*n)-z*Math.sqrt(p*(1-p)/n+z*z/(4.0*n*n)))/den;
    }
    public static double fitness(HistoricalReplayEngineSU1.Replay r,int a,int b) {
        int n=0,k=0;for(int t=a;t<b;t++)if(r.act[t]){n++;if(r.hit[t])k++;}if(n<30)return -1;
        double rate=k/(double)n,worst=1;int step=(b-a)/4;
        for(int i=0;i<4;i++){int bn=0,bk=0;for(int t=a+i*step;t<(i==3?b:a+(i+1)*step);t++)if(r.act[t]){bn++;if(r.hit[t])bk++;}if(bn>0)worst=Math.min(worst,bk/(double)bn);}
        return wilsonLower(k,n)-.25*Math.max(0,rate-worst);
    }
    public static double folds(HistoricalReplayEngineSU1.Replay r,int a,int b) {
        if(fitness(r,a,b)<=-1)return -1;double sum=0,min=1,max=-1;int step=(b-a)/3;
        for(int i=0;i<3;i++){double f=fitness(r,a+i*step,i==2?b:a+(i+1)*step);sum+=f;min=Math.min(min,f);max=Math.max(max,f);}
        return sum/3-.5*(max-min);
    }
}
