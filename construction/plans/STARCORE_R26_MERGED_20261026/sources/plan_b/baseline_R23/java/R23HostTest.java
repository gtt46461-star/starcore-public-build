package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class R23HostTest {
    static int tests=0;static void check(boolean x,String s){tests++;if(!x)throw new AssertionError(s);}
    public static void main(String[]args){
        double[][] p=new double[10][10];for(double[]r:p)Arrays.fill(r,.1);
        K3ProbabilityR23.Action a=K3ProbabilityR23.best(p);
        check(a.position==1&&a.mask==7&&Math.abs(a.probability-.3)<1e-12,"stable ties");
        Random rng=new Random(23);
        for(int trial=0;trial<100;trial++){
            for(double[]r:p){double z=0;for(int j=0;j<10;j++){r[j]=rng.nextDouble();z+=r[j];}for(int j=0;j<10;j++)r[j]/=z;}
            a=K3ProbabilityR23.best(p);double best=0;
            for(double[]r:p)for(int i=0;i<8;i++)for(int j=i+1;j<9;j++)for(int k=j+1;k<10;k++)best=Math.max(best,r[i]+r[j]+r[k]);
            check(Math.abs(best-a.probability)<1e-12,"1200-action equality");
        }
        boolean[] act=new boolean[1000],h=new boolean[1000],better=new boolean[1000];
        for(int t=0;t<1000;t++){act[t]=t%5==0;h[t]=t%20==0;better[t]=t%10==0;}
        check(ResearchObjectiveR23.score(act,better,0,1000,4)>ResearchObjectiveR23.score(act,h,0,1000,4),"actual hits drive rank");
        check(ResearchObjectiveR23.score(new boolean[1000],h,0,1000,4)==-1,"empty is not good");
        boolean threw=false;try{ResearchObjectiveR23.score(act,h,1,1,4);}catch(IllegalArgumentException e){threw=true;}check(threw,"invalid interval rejected");
        System.out.println("{\"checks\":"+tests+",\"passed\":true,\"scope\":\"HOST_REFERENCE_ONLY\"}");
    }
}
