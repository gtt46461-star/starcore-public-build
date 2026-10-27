package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;

/** Exact O(n 2^n) inference for UNARY permutation energy only. */
public final class ExactPermutationR26 {
    private ExactPermutationR26() {}
    public static final class Result {
        public final double logZ; public final double[][] marginal;
        Result(double z,double[][] p){logZ=z;marginal=p;}
        public double k3(int position,int a,int b,int c){if(a==b||a==c||b==c)throw new IllegalArgumentException("K3_DISTINCT");return marginal[position][a]+marginal[position][b]+marginal[position][c];}
    }
    private static double add(double a,double b){if(a==Double.NEGATIVE_INFINITY)return b;if(b==Double.NEGATIVE_INFINITY)return a;double m=Math.max(a,b);return m+Math.log1p(Math.exp(Math.min(a,b)-m));}
    public static Result infer(double[][] s,BudgetTokenSU1 budget){
        int n=s.length;if(n<1||n>10)throw new IllegalArgumentException("R26_SCORE_SHAPE");
        for(double[] row:s){if(row.length!=n)throw new IllegalArgumentException("R26_SCORE_SHAPE");for(double x:row)if(!Double.isFinite(x))throw new IllegalArgumentException("R26_SCORE_NON_FINITE");}
        int total=1<<n,full=total-1;double[] f=new double[total],b=new double[total];Arrays.fill(f,Double.NEGATIVE_INFINITY);Arrays.fill(b,Double.NEGATIVE_INFINITY);f[0]=0;b[full]=0;
        for(int mask=0;mask<full;mask++){if(budget!=null&&(mask&31)==0)budget.checkpointOrYield();int p=Integer.bitCount(mask);for(int j=0;j<n;j++)if((mask&(1<<j))==0)f[mask|1<<j]=add(f[mask|1<<j],f[mask]+s[p][j]);}
        for(int mask=full-1;mask>=0;mask--){if(budget!=null&&(mask&31)==0)budget.checkpointOrYield();int p=Integer.bitCount(mask);for(int j=0;j<n;j++)if((mask&(1<<j))==0)b[mask]=add(b[mask],s[p][j]+b[mask|1<<j]);}
        double[][] p=new double[n][n];for(double[] row:p)Arrays.fill(row,Double.NEGATIVE_INFINITY);
        for(int mask=0;mask<full;mask++){if(budget!=null&&(mask&31)==0)budget.checkpointOrYield();int i=Integer.bitCount(mask);for(int j=0;j<n;j++)if((mask&(1<<j))==0)p[i][j]=add(p[i][j],f[mask]+s[i][j]+b[mask|1<<j]-f[full]);}
        for(double[] row:p)for(int j=0;j<n;j++)row[j]=Math.exp(row[j]);return new Result(f[full],p);
    }
    public static double nll(double[][] scores,int[] draw,Result r){if(draw.length!=scores.length)throw new IllegalArgumentException("R26_DRAW");int mask=0;double loss=r.logZ;for(int p=0;p<draw.length;p++){int j=draw[p];if(j<0||j>=draw.length||(mask&(1<<j))!=0)throw new IllegalArgumentException("R26_DRAW");mask|=1<<j;loss-=scores[p][j];}return loss;}
}
