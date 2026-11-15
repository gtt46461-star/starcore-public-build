package com.ethk3.novaonepro.strategyv2;
import java.util.Arrays;
/** Exact subset/last-number inference for a bounded pairwise chain over a legal permutation. */
public final class PermutationChainR26 {
    private PermutationChainR26(){}
    public static final class Result {public final double z;public final double[][] p;public final double[][][] edge;Result(int n,double z){this.z=z;p=new double[n][n];edge=new double[n-1][n][n];}}
    private static double add(double a,double b){if(a==Double.NEGATIVE_INFINITY)return b;if(b==Double.NEGATIVE_INFINITY)return a;double m=Math.max(a,b);return m+Math.log1p(Math.exp(Math.min(a,b)-m));}
    public static Result infer(double[][] s,double[][][] pair,int[] order){
        int n=s.length;if(n<1||n>10||pair.length!=n-1||order.length!=n)throw new IllegalArgumentException("R26_CHAIN_SHAPE");int positions=0;for(int pos:order){if(pos<0||pos>=n||(positions&(1<<pos))!=0)throw new IllegalArgumentException("R26_CHAIN_ORDER");positions|=1<<pos;}
        for(double[] row:s){if(row.length!=n)throw new IllegalArgumentException("R26_CHAIN_SHAPE");for(double x:row)if(!Double.isFinite(x))throw new IllegalArgumentException("R26_CHAIN_VALUE");}for(double[][] e:pair){if(e.length!=n)throw new IllegalArgumentException("R26_CHAIN_SHAPE");for(double[] row:e){if(row.length!=n)throw new IllegalArgumentException("R26_CHAIN_SHAPE");for(double x:row)if(!Double.isFinite(x))throw new IllegalArgumentException("R26_CHAIN_VALUE");}}
        int full=(1<<n)-1;double[][] f=new double[full+1][n],b=new double[full+1][n];for(double[] row:f)Arrays.fill(row,Double.NEGATIVE_INFINITY);for(double[] row:b)Arrays.fill(row,Double.NEGATIVE_INFINITY);for(int j=0;j<n;j++){f[1<<j][j]=s[order[0]][j];b[full][j]=0;}
        for(int mask=1;mask<full;mask++){if((mask&31)==0)ExecutionBudgetR26.poll();int pos=Integer.bitCount(mask);for(int last=0;last<n;last++)if((mask&(1<<last))!=0)for(int next=0;next<n;next++)if((mask&(1<<next))==0){int after=mask|1<<next;f[after][next]=add(f[after][next],f[mask][last]+s[order[pos]][next]+pair[pos-1][last][next]);}}
        for(int mask=full-1;mask>=1;mask--){if((mask&31)==0)ExecutionBudgetR26.poll();int pos=Integer.bitCount(mask);for(int last=0;last<n;last++)if((mask&(1<<last))!=0)for(int next=0;next<n;next++)if((mask&(1<<next))==0)b[mask][last]=add(b[mask][last],s[order[pos]][next]+pair[pos-1][last][next]+b[mask|1<<next][next]);}
        double z=Double.NEGATIVE_INFINITY;for(int j=0;j<n;j++)z=add(z,f[full][j]);Result r=new Result(n,z);for(int j=0;j<n;j++)r.p[order[0]][j]=Math.exp(s[order[0]][j]+b[1<<j][j]-z);
        for(int mask=1;mask<full;mask++){if((mask&31)==0)ExecutionBudgetR26.poll();int pos=Integer.bitCount(mask);for(int last=0;last<n;last++)if((mask&(1<<last))!=0)for(int next=0;next<n;next++)if((mask&(1<<next))==0){double mass=Math.exp(f[mask][last]+s[order[pos]][next]+pair[pos-1][last][next]+b[mask|1<<next][next]-z);r.p[order[pos]][next]+=mass;r.edge[pos-1][last][next]+=mass;}}
        return r;
    }
}
