package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Exact BPTT of RELSTATE10. Every relation, embedding and recurrent parameter is trainable. */
public final class RelationalTrainerSU1 {
    private RelationalTrainerSU1() {}
    static double[][] zeros(int a,int b){return new double[a][b];}
    static double[] softmax(double[] x){double max=-Double.MAX_VALUE,sum=0;for(double v:x)max=Math.max(max,v);double[] p=new double[x.length];for(int i=0;i<x.length;i++){p[i]=Math.exp(x[i]-max);sum+=p[i];}for(int i=0;i<p.length;i++)p[i]/=sum;return p;}
    private static double[][] random(Random r,int a,int b,double scale){double[][] x=zeros(a,b);for(double[] row:x)for(int i=0;i<row.length;i++)row[i]=r.nextGaussian()*scale;return x;}
    public static RelationalStateInference.Weights initial(int width,long seed){
        if(width<2||width>16)throw new IllegalArgumentException("SU1_TRAIN_WIDTH");Random r=new Random(seed);
        return new RelationalStateInference.Weights(random(r,10,width,.25),random(r,10,width,.05),random(r,10,10,.15),random(r,width,width,1/Math.sqrt(width)),random(r,width,width,1/Math.sqrt(width)),random(r,width,width,.4/Math.sqrt(width)),new double[width],random(r,width,10,.3),new double[10]);
    }
    public static final class Gradient {
        public final double loss;
        final double[][] e,u,r,wx,wm,wh,wo;final double[] b,c;
        Gradient(int d,double loss){this.loss=loss;e=zeros(10,d);u=zeros(10,d);r=zeros(10,10);wx=zeros(d,d);wm=zeros(d,d);wh=zeros(d,d);wo=zeros(d,10);b=new double[d];c=new double[10];}
        double squaredNorm(){double s=0;for(double[][] a:new double[][][]{e,u,r,wx,wm,wh,wo})for(double[] row:a)for(double v:row)s+=v*v;for(double v:b)s+=v*v;for(double v:c)s+=v*v;return s;}
    }
    public static Gradient gradient(RelationalStateInference.Weights w,int[][] history,int[] actual,BudgetTokenSU1 budget){
        RelationalStateInference.validateDraw(actual);if(history.length<1||history.length>256)throw new IllegalArgumentException("SU1_TRAIN_WINDOW");
        int length=history.length,d=w.d;double[][] relation=zeros(10,10);for(int p=0;p<10;p++)relation[p]=softmax(w.r[p]);
        double[][][] zs=new double[length][][],ms=new double[length][][],states=new double[length+1][][];states[0]=zeros(10,d);
        for(int t=0;t<length;t++){
            budget.checkpointOrYield();RelationalStateInference.validateDraw(history[t]);double[][] z=zeros(10,d),m=zeros(10,d),h=zeros(10,d);
            for(int p=0;p<10;p++)for(int k=0;k<d;k++)z[p][k]=w.e[history[t][p]-1][k]+w.u[p][k];
            for(int p=0;p<10;p++)for(int q=0;q<10;q++)for(int k=0;k<d;k++)m[p][k]+=relation[p][q]*z[q][k];
            for(int p=0;p<10;p++)for(int j=0;j<d;j++){double v=w.b[j];for(int k=0;k<d;k++)v+=z[p][k]*w.wx[k][j]+m[p][k]*w.wm[k][j]+states[t][p][k]*w.wh[k][j];h[p][j]=Math.tanh(v);}
            zs[t]=z;ms[t]=m;states[t+1]=h;
        }
        double loss=0;double[][] delta=zeros(10,10),last=states[length];
        for(int p=0;p<10;p++){double[] logits=w.c.clone();for(int n=0;n<10;n++)for(int k=0;k<d;k++)logits[n]+=last[p][k]*w.wo[k][n];double[] probabilities=softmax(logits);loss-=Math.log(Math.max(1e-300,probabilities[actual[p]-1]))/10;for(int n=0;n<10;n++)delta[p][n]=(probabilities[n]-(n==actual[p]-1?1:0))/10;}
        Gradient g=new Gradient(d,loss);double[][] dh=zeros(10,d),gr=zeros(10,10);
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){g.c[n]+=delta[p][n];for(int k=0;k<d;k++){g.wo[k][n]+=last[p][k]*delta[p][n];dh[p][k]+=delta[p][n]*w.wo[k][n];}}
        for(int t=length-1;t>=0;t--){
            budget.checkpointOrYield();double[][] da=zeros(10,d),gm=zeros(10,d),gz=zeros(10,d),previousGradient=zeros(10,d);
            for(int p=0;p<10;p++)for(int j=0;j<d;j++){da[p][j]=dh[p][j]*(1-states[t+1][p][j]*states[t+1][p][j]);g.b[j]+=da[p][j];for(int k=0;k<d;k++){g.wx[k][j]+=zs[t][p][k]*da[p][j];g.wm[k][j]+=ms[t][p][k]*da[p][j];g.wh[k][j]+=states[t][p][k]*da[p][j];gm[p][k]+=da[p][j]*w.wm[k][j];gz[p][k]+=da[p][j]*w.wx[k][j];previousGradient[p][k]+=da[p][j]*w.wh[k][j];}}
            for(int p=0;p<10;p++)for(int q=0;q<10;q++)for(int k=0;k<d;k++){gr[p][q]+=gm[p][k]*zs[t][q][k];gz[q][k]+=relation[p][q]*gm[p][k];}
            for(int p=0;p<10;p++)for(int k=0;k<d;k++){g.e[history[t][p]-1][k]+=gz[p][k];g.u[p][k]+=gz[p][k];}dh=previousGradient;
        }
        for(int p=0;p<10;p++){double mean=0;for(int q=0;q<10;q++)mean+=gr[p][q]*relation[p][q];for(int q=0;q<10;q++)g.r[p][q]=relation[p][q]*(gr[p][q]-mean);}
        if(!Double.isFinite(loss)||!Double.isFinite(g.squaredNorm()))throw new IllegalArgumentException("SU1_NONFINITE_GRADIENT");return g;
    }
    private static double[][] step(double[][] v,double[][] gradient,double rate){double[][] out=new double[v.length][];for(int i=0;i<v.length;i++)out[i]=step(v[i],gradient[i],rate);return out;}
    private static double[] step(double[] v,double[] gradient,double rate){double[] out=v.clone();for(int i=0;i<v.length;i++)out[i]-=rate*gradient[i];return out;}
    /** Copy-on-publish: a cancelled/failed gradient cannot mutate the active model. */
    public static RelationalStateInference.Weights update(RelationalStateInference.Weights w,Gradient g,double learningRate,double clip){
        if(!Double.isFinite(learningRate)||learningRate<=0||!Double.isFinite(clip)||clip<=0)throw new IllegalArgumentException("SU1_OPTIMIZER");
        double rate=learningRate*Math.min(1,clip/Math.max(1e-300,Math.sqrt(g.squaredNorm())));
        return new RelationalStateInference.Weights(step(w.e,g.e,rate),step(w.u,g.u,rate),step(w.r,g.r,rate),step(w.wx,g.wx,rate),step(w.wm,g.wm,rate),step(w.wh,g.wh,rate),step(w.b,g.b,rate),step(w.wo,g.wo,rate),step(w.c,g.c,rate));
    }
    public static RelationalStateInference.Weights trainCausal(int[][] draws,int throughExclusive,int width,int window,long seed,BudgetTokenSU1 budget){
        RelationalStateInference.Weights w=initial(width,seed);
        int first=Math.max(window,throughExclusive-32);
        for(int u=first;u<throughExclusive;u++){
            int[][] history=Arrays.copyOfRange(draws,u-window,u);Gradient g=gradient(w,history,draws[u],budget);w=update(w,g,.03,1);
        }
        return w;
    }
}
