package com.ethk3.novaonepro.strategyv2;

import java.io.*;

/** F17: trained cross-position lag-one scores with exact legal-permutation readout.
 * Online SGD minimizes permutation NLL; no row-softmax probability substitution. */
public final class PermutationModelR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    private final double lr;private final double[][] bias=new double[10][10];
    private final double[][][] weights=new double[100][10][10];
    private int[] previous;private long updates;private double lastLoss;private TrainingDiagnosticsR26.Step last;
    public PermutationModelR26(double lr){if(!Double.isFinite(lr)||lr<=0||lr>0.1)throw new IllegalArgumentException("R26_MATCH_LR");this.lr=lr;}
    public void resetTemporalContext(){previous=null;last=null;}
    public TrainingDiagnosticsR26.Step lastTrainingStep(){return last;}
    public long completedTrainingUpdates(){return updates;}
    public String optimizerProtocol(){return "SGD_ACTIVE_COORDINATE_L2_0_0001_CLIP12_V1";}
    private String inputHash(){StringBuilder input=new StringBuilder("F17_FIXED_CONTEXT_V1|").append(Double.toHexString(lr)).append('|').append(updates);if(previous==null)input.append("|NO_PREVIOUS");else for(int value:previous)input.append('|').append(value);return PolicyIdentityR26.sha256(input.toString());}
    public int stateWidth(){return 3;}
    private double[][] scores(){double[][] s=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){s[p][n]=bias[p][n];if(previous!=null)for(int q=0;q<10;q++)s[p][n]+=weights[p*10+n][q][previous[q]-1]/10;}return s;}
    public void predict(float[][] p,float[] st){ExactPermutationR26.Result r=ExactPermutationR26.infer(scores(),null);for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)r.marginal[i][j];st[0]=(float)Math.log1p(updates);st[1]=(float)lastLoss;st[2]=previous==null?0:1;}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(updates==Long.MAX_VALUE)throw new IllegalStateException("F17_UPDATE_CLOCK_CAPACITY");
        double[][] scores=scores();ExactPermutationR26.Result distribution=ExactPermutationR26.infer(scores,null);int[] zero=new int[10];for(int p=0;p<10;p++)zero[p]=draw[p]-1;double nll=ExactPermutationR26.nll(scores,zero,distribution),regularizerBefore=0,regularizerAfter=0,gradientSquared=0,deltaSquared=0;
        double[][] nextBias=new double[10][10],nextActive=new double[100][10],afterScores=new double[10][10];
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){
            double gradient=distribution.marginal[p][n]-(draw[p]-1==n?1:0),value=bias[p][n],g=gradient+1e-4*value,next=clamp(value-lr*g);nextBias[p][n]=next;afterScores[p][n]=next;gradientSquared+=g*g;regularizerBefore+=value*value;regularizerAfter+=next*next;double delta=next-value;deltaSquared+=delta*delta;
            if(previous!=null)for(int q=0;q<10;q++){value=weights[p*10+n][q][previous[q]-1];g=gradient/10+1e-4*value;next=clamp(value-lr*g);nextActive[p*10+n][q]=next;afterScores[p][n]+=next/10;gradientSquared+=g*g;regularizerBefore+=value*value;regularizerAfter+=next*next;delta=next-value;deltaSquared+=delta*delta;}
        }
        double after=ExactPermutationR26.nll(afterScores,zero,ExactPermutationR26.infer(afterScores,null));
        TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(updates+1,nll+.00005*regularizerBefore,after+.00005*regularizerAfter,Math.sqrt(gradientSquared),Math.sqrt(deltaSquared),inputHash(),"EXACT_PERMUTATION_JOINT_NLL_PLUS_ACTIVE_COORDINATE_L2_FIXED_PRE_LABEL_CONTEXT;L2=0.0001;DELTA=BIAS_AND_ACTIVE_LAG_COORDINATES");ExecutionBudgetR26.poll();
        for(int p=0;p<10;p++)for(int n=0;n<10;n++){bias[p][n]=nextBias[p][n];if(previous!=null)for(int q=0;q<10;q++)weights[p*10+n][q][previous[q]-1]=nextActive[p*10+n][q];}
        // Existing feature/checkpoint lastLoss remains the original unregularized NLL.
        lastLoss=nll;previous=draw.clone();updates++;last=step;
    }
    private static double clamp(double x){return Math.max(-12,Math.min(12,x));}
    public void write(DataOutput out)throws IOException{out.writeInt(0x52323617);out.writeDouble(lr);out.writeLong(updates);out.writeDouble(lastLoss);out.writeBoolean(previous!=null);if(previous!=null)for(int x:previous)out.writeInt(x);for(double[] r:bias)for(double x:r)out.writeDouble(x);for(double[][] m:weights)for(double[] r:m)for(double x:r)out.writeDouble(x);}
    public void read(DataInput in)throws IOException{
        if(in.readInt()!=0x52323617||Double.doubleToRawLongBits(in.readDouble())!=Double.doubleToRawLongBits(lr))throw new IOException("R26_MATCH_ABI");long clock=in.readLong();double loss=finite(in.readDouble());if(clock<0)throw new IOException("R26_CURSOR");int[] context=null;if(in.readBoolean()){context=new int[10];for(int i=0;i<10;i++)context[i]=in.readInt();try{OnlineModelsSU2.requireDraw(context);}catch(IllegalArgumentException e){throw new IOException("R26_DRAW_STATE",e);}}
        double[][] restoredBias=new double[10][10];double[][][] restoredWeights=new double[100][10][10];for(double[] row:restoredBias)for(int i=0;i<row.length;i++)row[i]=finite(in.readDouble());for(double[][] matrix:restoredWeights)for(double[] row:matrix)for(int i=0;i<row.length;i++)row[i]=finite(in.readDouble());
        for(int p=0;p<10;p++)System.arraycopy(restoredBias[p],0,bias[p],0,10);for(int i=0;i<100;i++)for(int q=0;q<10;q++)System.arraycopy(restoredWeights[i][q],0,weights[i][q],0,10);updates=clock;lastLoss=loss;previous=context;last=null;
    }
    private static double finite(double x)throws IOException{if(!Double.isFinite(x)||Math.abs(x)>1e9)throw new IOException("R26_NON_FINITE_STATE");return x;}
}
