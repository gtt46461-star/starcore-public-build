package com.ethk3.novaonepro.strategyv2;
/** The event at one position is categorical; a three-number set probability is a sum. */
public final class K3ProbabilityR23 {
    private K3ProbabilityR23() {}
    public static final class Action {
        public final int position,mask; public final double probability;
        Action(int p,int m,double q){position=p;mask=m;probability=q;}
    }
    public static Action best(double[][] p){
        if(p==null||p.length!=10)throw new IllegalArgumentException("SHAPE");
        Action best=null;
        for(int i=0;i<10;i++){
            if(p[i]==null||p[i].length!=10)throw new IllegalArgumentException("SHAPE");
            double z=0;for(double v:p[i]){if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("PROB");z+=v;}
            if(Math.abs(z-1)>1e-6)throw new IllegalArgumentException("NORMALIZE");
            int mask=0;double mass=0;
            for(int k=0;k<3;k++){
                int n=-1;double v=-1;for(int j=0;j<10;j++)if((mask&(1<<j))==0&&p[i][j]>v){n=j;v=p[i][j];}
                mask|=1<<n;mass+=v;
            }
            if(best==null||mass>best.probability)best=new Action(i+1,mask,mass);
        }
        return best;
    }
}
