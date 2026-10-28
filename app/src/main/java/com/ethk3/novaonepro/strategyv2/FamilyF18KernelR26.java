package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Masked autoregression whose dependency is the used set. Exact subset-flow marginals. */
public final class FamilyF18KernelR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    private final double[][] bias=new double[10][10],context=new double[100][10],prefix=new double[10][10];private int[] previous;private long t;private TrainingDiagnosticsR26.Step last;
    public void resetTemporalContext(){previous=null;last=null;}
    public TrainingDiagnosticsR26.Step lastTrainingStep(){return last;}
    public long completedTrainingUpdates(){return t;}
    public String optimizerProtocol(){return "MASKED_AUTOREGRESSIVE_SGD_003_PARAM_CLIP8_V1";}
    private String inputHash(){StringBuilder input=new StringBuilder("F18_FIXED_PRE_LABEL_LAG_CONTEXT_V1|").append(t);if(previous==null)input.append("|NO_PREVIOUS");else for(int value:previous)input.append('|').append(value);return PolicyIdentityR26.sha256(input.toString());}
    public int stateWidth(){return 2;}
    private double[] conditional(int mask){return conditional(mask,bias,context,prefix,previous);}
    private static double[] conditional(int mask,double[][] bias,double[][] context,double[][] prefix,int[] previous){int p=Integer.bitCount(mask);double[] prob=new double[10];double max=-1e30,z=0;for(int n=0;n<10;n++)if((mask&(1<<n))==0){double s=bias[p][n]+(previous==null?0:context[p*10+n][previous[p]-1]);if(p>0)for(int used=0;used<10;used++)if((mask&(1<<used))!=0)s+=prefix[n][used]/p;prob[n]=s;max=Math.max(max,s);}for(int n=0;n<10;n++)if((mask&(1<<n))==0){prob[n]=Math.exp(prob[n]-max);z+=prob[n];}for(int n=0;n<10;n++)if((mask&(1<<n))==0)prob[n]/=z;return prob;}
    public void predict(float[][] p,float[] st){double[] mass=new double[1024];mass[0]=1;double[][] result=new double[10][10];for(int mask=0;mask<1023;mask++){if((mask&31)==0)ExecutionBudgetR26.poll();int position=Integer.bitCount(mask);double[] prob=conditional(mask);for(int n=0;n<10;n++)if((mask&(1<<n))==0){double flow=mass[mask]*prob[n];result[position][n]+=flow;mass[mask|1<<n]+=flow;}}for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)result[i][j];st[0]=(float)Math.log1p(t);st[1]=(float)mass[1023];}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(t==Long.MAX_VALUE)throw new IllegalStateException("F18_UPDATE_CLOCK_CAPACITY");double[][] gb=new double[10][10],gc=new double[100][10],gp=new double[10][10];int mask=0;double before=0;
        for(int p=0;p<10;p++){ExecutionBudgetR26.poll();double[] probability=conditional(mask);before-=Math.log(probability[draw[p]-1]);for(int n=0;n<10;n++)if((mask&(1<<n))==0){double gradient=probability[n]-(draw[p]==n+1?1:0);gb[p][n]=gradient;if(previous!=null)gc[p*10+n][previous[p]-1]=gradient;if(p>0)for(int used=0;used<10;used++)if((mask&(1<<used))!=0)gp[n][used]+=gradient/p;}mask|=1<<(draw[p]-1);}
        double[][][] params={bias,context,prefix},gradients={gb,gc,gp},next={new double[10][10],new double[100][10],new double[10][10]};double gradientSquared=0,deltaSquared=0;for(int k=0;k<params.length;k++)for(int i=0;i<params[k].length;i++)for(int j=0;j<params[k][i].length;j++){double gradient=gradients[k][i][j],value=Math.max(-8,Math.min(8,params[k][i][j]-.03*gradient)),delta=value-params[k][i][j];next[k][i][j]=value;gradientSquared+=gradient*gradient;deltaSquared+=delta*delta;}
        mask=0;double after=0;for(int p=0;p<10;p++){after-=Math.log(conditional(mask,next[0],next[1],next[2],previous)[draw[p]-1]);mask|=1<<(draw[p]-1);}TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(t+1,before,after,Math.sqrt(gradientSquared),Math.sqrt(deltaSquared),inputHash(),"MASKED_AUTOREGRESSIVE_JOINT_NLL_TARGET_PREFIX_FACTORIZATION_FIXED_PRE_LABEL_LAG_CONTEXT;DELTA=BIAS_CONTEXT_AND_USED_SET_PREFIX_WEIGHTS");ExecutionBudgetR26.poll();
        for(int k=0;k<params.length;k++)for(int i=0;i<params[k].length;i++)System.arraycopy(next[k][i],0,params[k][i],0,params[k][i].length);previous=draw.clone();t++;last=step;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26180001);o.writeLong(t);o.writeBoolean(previous!=null);if(previous!=null)for(int n:previous)o.writeInt(n);for(double[][] m:new double[][][]{bias,context,prefix})for(double[] row:m)for(double x:row)o.writeDouble(x);}
    public void read(DataInput in)throws IOException{
        if(in.readInt()!=0x26180001)throw new IOException("F18_ABI");long clock=in.readLong();if(clock<0)throw new IOException("F18_CURSOR");int[] history=null;if(in.readBoolean()){history=new int[10];for(int i=0;i<10;i++)history[i]=in.readInt();OnlineModelsSU2.requireDraw(history);}double[][][] next={new double[10][10],new double[100][10],new double[10][10]};for(double[][] matrix:next)for(double[] row:matrix)for(int j=0;j<row.length;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||Math.abs(row[j])>8)throw new IOException("F18_PARAMETER");}
        double[][][] params={bias,context,prefix};for(int k=0;k<params.length;k++)for(int i=0;i<params[k].length;i++)System.arraycopy(next[k][i],0,params[k][i],0,params[k][i].length);t=clock;previous=history;last=null;
    }
}
