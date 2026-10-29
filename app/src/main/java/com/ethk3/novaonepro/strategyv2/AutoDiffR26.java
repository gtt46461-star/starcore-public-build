package com.ethk3.novaonepro.strategyv2;
import java.util.Arrays;
/** Bounded reverse-mode scalar tape for trainable causal mechanisms. */
public final class AutoDiffR26 {
    private static final int CAP=64000;private int size;private final double[] value=new double[CAP],gradient=new double[CAP];private final int[] left=new int[CAP],right=new int[CAP],op=new int[CAP],parameter=new int[CAP];private final int[] paramNode;
    public AutoDiffR26(int parameters){paramNode=new int[parameters];reset();}
    public void reset(){size=0;Arrays.fill(paramNode,-1);}
    private int node(int code,int a,int b,double v,int p){if(size==CAP||!Double.isFinite(v))throw new IllegalArgumentException("R26_TAPE_CAPACITY_OR_NON_FINITE");if((size&255)==0)ExecutionBudgetR26.poll();int n=size++;op[n]=code;left[n]=a;right[n]=b;value[n]=v;gradient[n]=0;parameter[n]=p;return n;}
    public int constant(double x){return node(0,-1,-1,x,-1);}
    public int weight(int i,double[] w){if(i<0||i>=w.length)throw new IllegalArgumentException("R26_PARAMETER_INDEX");if(paramNode[i]<0)paramNode[i]=node(0,-1,-1,w[i],i);return paramNode[i];}
    public int add(int a,int b){return node(1,a,b,value[a]+value[b],-1);}
    public int multiply(int a,int b){return node(2,a,b,value[a]*value[b],-1);}
    public int tanh(int a){return node(3,a,-1,Math.tanh(value[a]),-1);}
    public int sigmoid(int a){return node(4,a,-1,1/(1+Math.exp(-Math.max(-40,Math.min(40,value[a])))),-1);}
    public int exp(int a){return node(5,a,-1,Math.exp(value[a]),-1);}
    public int divide(int a,int b){if(!(value[b]>0))throw new IllegalArgumentException("R26_ZERO_DIVISOR");return node(6,a,b,value[a]/value[b],-1);}
    public double value(int n){return value[n];}
    public double[] backward(int[] output,double[] seed){if(output.length!=seed.length)throw new IllegalArgumentException("R26_GRADIENT_SHAPE");for(int i=0;i<output.length;i++)gradient[output[i]]+=seed[i];double[] g=new double[paramNode.length];
        for(int n=size-1;n>=0;n--){if((n&255)==0)ExecutionBudgetR26.poll();double d=gradient[n];int a=left[n],b=right[n];switch(op[n]){case 1:gradient[a]+=d;gradient[b]+=d;break;case 2:gradient[a]+=d*value[b];gradient[b]+=d*value[a];break;case 3:gradient[a]+=d*(1-value[n]*value[n]);break;case 4:gradient[a]+=d*value[n]*(1-value[n]);break;case 5:gradient[a]+=d*value[n];break;case 6:gradient[a]+=d/value[b];gradient[b]-=d*value[a]/(value[b]*value[b]);break;default:break;}if(parameter[n]>=0)g[parameter[n]]+=d;}return g;
    }
}
