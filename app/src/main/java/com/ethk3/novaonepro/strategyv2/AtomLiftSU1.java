package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.AtomEvaluatorCP4;
import com.ethk3.novaonepro.r952.AtomWindowCP4;
import com.ethk3.novaonepro.r952.MemoryBudgetCP4;
import java.util.*;

/** All lifted values use the existing production evaluator, including its missing semantics. */
public final class AtomLiftSU1 implements AutoCloseable {
    private final int[][] draws;
    private final AtomWindowCP4[] windows=new AtomWindowCP4[10];
    private int through=-1;
    private final MemoryBudgetCP4 memory=new MemoryBudgetCP4(24L<<20);
    public AtomLiftSU1(int[][] history){this.draws=history;for(int p=0;p<10;p++)windows[p]=new AtomWindowCP4(memory);}
    private void seek(int target,BudgetTokenSU1 budget){
        if(target<0||target>draws.length)throw new IllegalArgumentException("SU1_ATOM_TARGET");
        if(through>=target){for(int p=0;p<10;p++){windows[p].close();windows[p]=new AtomWindowCP4(memory);}through=-1;}
        // Shared windows are bounded. Replay visits targets in chronological order.
        for(int u=through+1;u<target;u++){budget.checkpointOrYield();for(int p=0;p<10;p++)windows[p].append(u,draws[u][p]-1);through=u;}
    }
    private static Map<String,Double> params(String text){Map<String,Double> out=new TreeMap<>();if(!text.isEmpty())for(String kv:text.split(";")){String[] a=kv.split("=",-1);if(a.length!=2||out.put(a[0],Double.valueOf(a[1]))!=null)throw new IllegalArgumentException("SU1_ATOM_PARAMS");}return out;}
    private static int at(Map<String,Double> p,String k){return p.containsKey(k)?p.get(k).intValue():0;}
    static AtomEvaluatorCP4.Spec spec(String family,String text,int digit){
        Map<String,Double> p=params(text);
        return new AtomEvaluatorCP4.Spec(family,at(p,"window"),at(p,"lag"),digit,0,0,at(p,"recent"),at(p,"reference"),at(p,"period"),p.getOrDefault("alpha",.5),"ALL",p.containsKey("order")?at(p,"order"):1,0);
    }
    public double[][] score(String family,String params,int target,BudgetTokenSU1 budget){
        AtomCatalogSU1.requireScore(family,params);seek(target,budget);double[][] out=new double[10][10];
        for(int p=0;p<10;p++){
            budget.checkpointOrYield();double[] raw=new double[10];boolean[] present=new boolean[10];double sum=0;int n=0;
            for(int d=0;d<10;d++){AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec(family,params,d),windows[p],null,target);if(r.present){raw[d]=r.value;present[d]=true;sum+=r.value;n++;}}
            if(n==0)continue;for(int d=0;d<10;d++)if(!present[d])raw[d]=sum/n;
            double mean=0;for(double v:raw)mean+=v;mean/=10;double variance=0;for(double v:raw)variance+=(v-mean)*(v-mean);double sd=Math.sqrt(variance/10)+1e-6;
            for(int d=0;d<10;d++)out[p][d]=(double)(float)((raw[d]-mean)/sd);
        }
        // A candidate may inspect many different windows; retain only a bounded working set.
        if(memory.used()>20L<<20)for(AtomWindowCP4 w:windows)w.releaseUnused(Collections.emptySet());
        return out;
    }
    private double scalar(String family,int q,String params,int t,BudgetTokenSU1 budget){seek(t,budget);AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec(family,params,0),windows[q],null,t);return r.present?Math.rint(r.value*1e12)/1e12:0;}
    public boolean condition(String family,int q,String params,int t,BudgetTokenSU1 budget){
        AtomCatalogSU1.requireCondition(family,params);if(t<300)return false;
        int block=300+((t-300)/50)*50;double[] past=new double[300];
        // This secondary cursor is independent of the current-target cursor and never consumes the label at t.
        try(AtomLiftSU1 prior=new AtomLiftSU1(draws)){for(int i=0;i<300;i++)past[i]=prior.scalar(family,q,params,block-300+i,budget);}
        Arrays.sort(past);double median=(past[149]+past[150])*.5;
        return scalar(family,q,params,t,budget)>median;
    }
    @Override public void close(){for(AtomWindowCP4 w:windows)w.close();}
}
