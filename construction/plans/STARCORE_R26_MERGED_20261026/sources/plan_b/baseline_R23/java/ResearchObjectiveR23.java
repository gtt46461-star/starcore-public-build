package com.ethk3.novaonepro.strategyv2;
/** Research ranking only. Not a fresh-evidence test or an authorization gate.
 * Teacher expectations cannot enter this interface. All inputs are actual outcomes.
 */
public final class ResearchObjectiveR23 {
    private ResearchObjectiveR23() {}
    public static double score(boolean[] act, boolean[] hit, int a, int b, int folds) {
        if(act==null||hit==null||act.length!=hit.length||a<0||b>act.length||a>=b||folds<1||folds>b-a)
            throw new IllegalArgumentException("R23_SCORE_INPUT");
        int n=0,k=0; for(int i=a;i<b;i++)if(act[i]){n++;if(hit[i])k++;}
        if(n==0)return -1.0;
        double q=(double)k/n,z=1.6448536269514722,z2=z*z;
        double lower=(q+z2/(2*n)-z*Math.sqrt(q*(1-q)/n+z2/(4.0*n*n)))/(1+z2/n);
        double min=1,max=0;int observed=0;
        for(int f=0;f<folds;f++){
            int x=a+(b-a)*f/folds,y=a+(b-a)*(f+1)/folds,fn=0,fk=0;
            for(int t=x;t<y;t++)if(act[t]){fn++;if(hit[t])fk++;}
            if(fn>0){double rate=(double)fk/fn;min=Math.min(min,rate);max=Math.max(max,rate);observed++;}
        }
        double cov=n/(double)(b-a);
        // Fixed-sample Wilson is only a descriptive search penalty here. It is NOT
        // repeatedly reinterpreted as a sequential promotion confidence bound.
        return lower-0.25*(max-min)-2.0*Math.max(0,0.15-cov)-0.05*(folds-observed)/folds;
    }
}
