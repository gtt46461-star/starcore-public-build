package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;

/** Trainable CP tensor with explicit position/state modes and complete AdaGrad state.
 * The prior ABI-1 SGD kernel remains a separate executable and unchanged codec. */
public final class FamilyF19TensorR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    private static final int MAGIC=0x26190201,R=3,L=3;
    private static final int[] HEIGHTS={10,10,10,10,L,2};
    private final double lr;private final boolean positionMode,stateMode;
    // U[p,k], V[n,k], A[q,k], B[r,k], C[lag,k], optional D[causal state,k].
    private final double[][][] factor=new double[6][][],accumulator=new double[6][][];
    private final int[][] ring=new int[L][10];private long updates;private int seen;
    private Step last;
    public FamilyF19TensorR26(double lr,boolean positionMode,boolean stateMode){
        if(!Double.isFinite(lr)||lr<=0||lr>.1||!positionMode&&stateMode)throw new IllegalArgumentException("F19_TENSOR_GRAPH");this.lr=lr;this.positionMode=positionMode;this.stateMode=stateMode;
        long seed=1719;for(int m=0;m<factor.length;m++){factor[m]=new double[HEIGHTS[m]][R];accumulator[m]=new double[HEIGHTS[m]][R];for(double[] row:factor[m])for(int k=0;k<R;k++){
            if(m==5)row[k]=1;else{seed^=seed<<13;seed^=seed>>>7;seed^=seed<<17;row[k]=(Math.floorMod(seed,10000)/10000.0-.5)*.4;}
        }}
    }
    public int stateWidth(){return 4;}
    public long completedUpdates(){return updates;}
    public long completedTrainingUpdates(){return updates;}
    public String optimizerProtocol(){return "ADAGRAD_V1";}
    public Step lastTrainingStep(){return last;}
    public void resetTemporalContext(){seen=0;last=null;for(int[] row:ring)Arrays.fill(row,0);}
    private int state(){return seen==0?0:ring[(int)((updates-1)%L)][0]<=5?0:1;}
    private static double mean(double[][] values,int k){double sum=0;for(double[] row:values)sum+=row[k];return sum/values.length;}
    private double[] rawContext(double[][][] f){
        double[] h=new double[R];for(int lag=0;lag<seen;lag++)for(int q=0;q<10;q++){int n=ring[(int)((updates-1-lag)%L)][q]-1;for(int k=0;k<R;k++)h[k]+=(positionMode?f[2][q][k]:mean(f[2],k))*f[3][n][k]*f[4][lag][k];}return h;
    }
    private double[] context(double[][][] f){double[] h=rawContext(f);if(stateMode&&seen>0)for(int k=0;k<R;k++)h[k]*=f[5][state()][k];return h;}
    private static double[][] scores(double[][][] f,double[] h){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int k=0;k<R;k++)s[p][n]+=f[0][p][k]*f[1][n][k]*h[k];return s;}
    public void predict(float[][] out,float[] st){
        ExactPermutationR26.Result result=ExactPermutationR26.infer(scores(factor,context(factor)),null);for(int p=0;p<10;p++)for(int n=0;n<10;n++)out[p][n]=(float)result.marginal[p][n];
        st[0]=(float)Math.log1p(updates);st[1]=seen/(float)L;st[2]=stateMode&&seen>0?state():-1;st[3]=R;
    }
    private String inputHash(){StringBuilder b=new StringBuilder("F19_FIXED_CAUSAL_CONTEXT_V2|").append(positionMode).append('|').append(stateMode).append('|').append(updates).append('|').append(seen);for(int lag=0;lag<seen;lag++)for(int x:ring[(int)((updates-1-lag)%L)])b.append('|').append(x);return PolicyIdentityR26.sha256(b.toString());}
    private static double objective(ExactPermutationR26.Result distribution,double[][] scores,int[] draw){double loss=distribution.logZ;for(int p=0;p<10;p++)loss-=scores[p][draw[p]-1];return loss;}
    private static double[][][] zeros(){double[][][] values=new double[6][][];for(int m=0;m<values.length;m++)values[m]=new double[HEIGHTS[m]][R];return values;}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(updates==Long.MAX_VALUE)throw new IllegalStateException("F19_UPDATE_CLOCK_CAPACITY");
        double[] h=context(factor),gh=new double[R];double[][] beforeScores=scores(factor,h);ExactPermutationR26.Result prediction=ExactPermutationR26.infer(beforeScores,null);double before=objective(prediction,beforeScores,draw);
        double[][][] gradient=zeros();
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){double g=prediction.marginal[p][n]-(draw[p]==n+1?1:0);for(int k=0;k<R;k++){gradient[0][p][k]+=g*factor[1][n][k]*h[k];gradient[1][n][k]+=g*factor[0][p][k]*h[k];gh[k]+=g*factor[0][p][k]*factor[1][n][k];}}
        if(stateMode&&seen>0){double[] raw=rawContext(factor);for(int k=0;k<R;k++){gradient[5][state()][k]=gh[k]*raw[k];gh[k]*=factor[5][state()][k];}}
        for(int lag=0;lag<seen;lag++)for(int q=0;q<10;q++){int n=ring[(int)((updates-1-lag)%L)][q]-1;for(int k=0;k<R;k++){
            if(positionMode)gradient[2][q][k]+=gh[k]*factor[3][n][k]*factor[4][lag][k];else for(int z=0;z<10;z++)gradient[2][z][k]+=gh[k]*factor[3][n][k]*factor[4][lag][k]/10;
            double a=positionMode?factor[2][q][k]:mean(factor[2],k);gradient[3][n][k]+=gh[k]*a*factor[4][lag][k];gradient[4][lag][k]+=gh[k]*a*factor[3][n][k];
        }}
        // Stage all changes. Exact inference and cooperative polls can still cancel
        // before any factor, optimizer slot, raw context or update clock is modified.
        double[][][] next=zeros(),nextAccumulator=zeros();double norm=0,delta=0;
        for(int m=0;m<factor.length;m++)for(int i=0;i<HEIGHTS[m];i++)for(int k=0;k<R;k++){
            double g=gradient[m][i][k],old=factor[m][i][k];norm+=g*g;double a=accumulator[m][i][k]+g*g;nextAccumulator[m][i][k]=a;
            double value=m==5&&!stateMode?old:Math.max(-3,Math.min(3,old-lr*g/(Math.sqrt(a)+1e-8)-lr*1e-5*old));next[m][i][k]=value;delta+=(value-old)*(value-old);
            if(!Double.isFinite(a)||!Double.isFinite(value))throw new IllegalStateException("F19_NONFINITE_UPDATE");
        }
        double[][] afterScores=scores(next,context(next));double after=objective(ExactPermutationR26.infer(afterScores,null),afterScores,draw);
        Step completed=new Step(updates+1,before,after,Math.sqrt(norm),Math.sqrt(delta),inputHash(),"EXACT_PERMUTATION_JOINT_NLL_FIXED_PRE_LABEL_CONTEXT");ExecutionBudgetR26.poll();
        for(int m=0;m<factor.length;m++)for(int i=0;i<HEIGHTS[m];i++){System.arraycopy(next[m][i],0,factor[m][i],0,R);System.arraycopy(nextAccumulator[m][i],0,accumulator[m][i],0,R);}
        System.arraycopy(draw,0,ring[(int)(updates%L)],0,10);updates++;seen=Math.min(L,seen+1);last=completed;
    }
    public void write(DataOutput out)throws IOException{
        out.writeInt(MAGIC);out.writeDouble(lr);out.writeBoolean(positionMode);out.writeBoolean(stateMode);out.writeLong(updates);out.writeInt(seen);for(int[] row:ring)for(int n:row)out.writeInt(n);for(double[][][] values:new double[][][][]{factor,accumulator})for(double[][] matrix:values)for(double[] row:matrix)for(double value:row)out.writeDouble(value);
    }
    public void read(DataInput in)throws IOException{
        if(in.readInt()!=MAGIC||in.readDouble()!=lr||in.readBoolean()!=positionMode||in.readBoolean()!=stateMode)throw new IOException("F19_TENSOR_CHECKPOINT_GRAPH");long clock=in.readLong();int context=in.readInt();if(clock<0||context<0||context>L||context>clock)throw new IOException("F19_TENSOR_CHECKPOINT_CURSOR");
        int[][] history=new int[L][10];for(int[] row:history)for(int n=0;n<10;n++){row[n]=in.readInt();if(row[n]<0||row[n]>10)throw new IOException("F19_TENSOR_CHECKPOINT_HISTORY");}
        boolean[] occupied=new boolean[L];for(int lag=0;lag<context;lag++){int slot=(int)((clock-1-lag)%L);occupied[slot]=true;try{OnlineModelsSU2.requireDraw(history[slot]);}catch(IllegalArgumentException invalid){throw new IOException("F19_TENSOR_CHECKPOINT_DRAW",invalid);}}for(int slot=0;slot<L;slot++)if(!occupied[slot])for(int n:history[slot])if(n!=0)throw new IOException("F19_TENSOR_UNDECLARED_RAW_CONTEXT");
        double[][][] next=zeros(),nextAccumulator=zeros();for(int part=0;part<2;part++){double[][][] values=part==0?next:nextAccumulator;for(int m=0;m<values.length;m++)for(int i=0;i<HEIGHTS[m];i++)for(int k=0;k<R;k++){double value=in.readDouble();if(!Double.isFinite(value)||part==0&&Math.abs(value)>3||part==1&&value<0)throw new IOException("F19_TENSOR_CHECKPOINT_NUMBER");values[m][i][k]=value;}}
        if(!stateMode)for(int i=0;i<2;i++)for(int k=0;k<R;k++)if(next[5][i][k]!=1||nextAccumulator[5][i][k]!=0)throw new IOException("F19_INACTIVE_STATE_MODE");
        for(int m=0;m<factor.length;m++)for(int i=0;i<HEIGHTS[m];i++){System.arraycopy(next[m][i],0,factor[m][i],0,R);System.arraycopy(nextAccumulator[m][i],0,accumulator[m][i],0,R);}for(int slot=0;slot<L;slot++)System.arraycopy(history[slot],0,ring[slot],0,10);updates=clock;seen=context;last=null;
    }
}
