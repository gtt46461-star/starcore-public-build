package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Past-only episodic retrieval with trained diagonal categorical distance. No future neighbors. */
public final class FamilyF11KernelR26 implements TemporalModelR26,TrainingDiagnosticsR26 {
    private static final int CAP=256;private int[][] keys=new int[CAP][10],labels=new int[CAP][10];private double[] metric=new double[10];private int[] previous;private int size,next;private long t,updates;private boolean completeUpdateHistory=true;private TrainingDiagnosticsR26.Step last;private final double lr;
    public FamilyF11KernelR26(double lr){if(!Double.isFinite(lr)||lr<=0||lr>.1)throw new IllegalArgumentException("F11_LR");this.lr=lr;Arrays.fill(metric,1);}
    public void resetTemporalContext(){previous=null;last=null;}
    public int stateWidth(){return 2;}
    private double[] weights(){return weights(metric);}
    private double[] weights(double[] parameters){double[] w=new double[size];if(previous==null||size==0)return w;double max=-1e30,z=0;for(int i=0;i<size;i++){double s=0;for(int q=0;q<10;q++)if(keys[i][q]!=previous[q])s-=parameters[q];w[i]=s;max=Math.max(max,s);}for(int i=0;i<size;i++){w[i]=Math.exp(w[i]-max);z+=w[i];}for(int i=0;i<size;i++)w[i]/=z;return w;}
    private double[][] table(double[] w){double[][] p=new double[10][10];for(double[] row:p)Arrays.fill(row,.01);if(previous==null||size==0){for(double[] row:p)Arrays.fill(row,.1);return p;}for(int i=0;i<size;i++)for(int q=0;q<10;q++)p[q][labels[i][q]-1]+=.9*w[i];return p;}
    public void predict(float[][] out,float[] st){double[][] p=table(weights());for(int i=0;i<10;i++)for(int j=0;j<10;j++)out[i][j]=(float)p[i][j];st[0]=size;st[1]=(float)Math.log1p(t);}
    public TrainingDiagnosticsR26.Step lastTrainingStep(){return last;}
    public long completedTrainingUpdates(){return updates;}
    public String optimizerProtocol(){return "DIAGONAL_RETRIEVAL_METRIC_SGD_L2_CLIP_001_8|lr="+Double.toHexString(lr)+"|"+(completeUpdateHistory?"KNOWN_LIFETIME":"UNKNOWN_PRIOR_UPDATES_COUNTED_SINCE_IMPORT");}
    private double loss(double[] parameters,int[] target){double[][] p=table(weights(parameters));double value=0;for(int q=0;q<10;q++)value-=Math.log(p[q][target[q]-1]);for(double x:parameters)value+=.5e-4*x*x;return value;}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(t==Long.MAX_VALUE||updates==Long.MAX_VALUE)throw new IllegalStateException("F11_UPDATE_CLOCK_CAPACITY");
        if(previous==null){int[] first=draw.clone();ExecutionBudgetR26.poll();previous=first;t++;last=null;return;}
        double[] w=weights();double[][] p=table(w);double[] gw=new double[size],grad=new double[10];double mean=0;
        for(int i=0;i<size;i++){for(int q=0;q<10;q++)if(labels[i][q]==draw[q])gw[i]-=.9/Math.max(1e-12,p[q][draw[q]-1]);mean+=w[i]*gw[i];}
        for(int i=0;i<size;i++){double gs=w[i]*(gw[i]-mean);for(int q=0;q<10;q++)if(keys[i][q]!=previous[q])grad[q]-=gs;}
        double[] proposed=new double[10];double gradSquared=0,deltaSquared=0;for(int q=0;q<10;q++){double g=grad[q]+1e-4*metric[q];gradSquared+=g*g;proposed[q]=Math.max(.001,Math.min(8,metric[q]-lr*g));double d=proposed[q]-metric[q];deltaSquared+=d*d;}
        StringBuilder context=new StringBuilder("F11_FIXED_PRE_LABEL_RETRIEVAL_V1|").append(t).append('|').append(size).append('|').append(next);for(int x:previous)context.append('|').append(x);
        TrainingDiagnosticsR26.Step step=new TrainingDiagnosticsR26.Step(updates+1,loss(metric,draw),loss(proposed,draw),Math.sqrt(gradSquared),Math.sqrt(deltaSquared),PolicyIdentityR26.sha256(context.toString()),"SUM_MARGINAL_CATEGORICAL_NLL_PLUS_0.5E-4_METRIC_L2;FIXED_PRE_LABEL_QUERY_AND_EPISODES;DIAGONAL_METRIC_ONLY;INSERTION_IS_MEMORY_NOT_SGD");
        int[] newKey=previous.clone(),newLabel=draw.clone(),newPrevious=draw.clone();ExecutionBudgetR26.poll();metric=proposed;keys[next]=newKey;labels[next]=newLabel;next=(next+1)%CAP;size=Math.min(CAP,size+1);previous=newPrevious;t++;updates++;last=step;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26110002);o.writeDouble(lr);o.writeLong(t);o.writeInt(size);o.writeInt(next);o.writeBoolean(previous!=null);if(previous!=null)for(int x:previous)o.writeInt(x);for(double x:metric)o.writeDouble(x);for(int i=0;i<size;i++){for(int x:keys[i])o.writeInt(x);for(int x:labels[i])o.writeInt(x);}o.writeLong(updates);o.writeBoolean(completeUpdateHistory);}
    public void read(DataInput in)throws IOException{
        int magic=in.readInt();if((magic!=0x26110001&&magic!=0x26110002)||in.readDouble()!=lr)throw new IOException("F11_ABI");
        long restoredT=in.readLong();int restoredSize=in.readInt(),restoredNext=in.readInt(),present=in.readUnsignedByte();if(restoredT<0||restoredSize<0||restoredSize>CAP||restoredNext<0||restoredNext>=CAP||present>1)throw new IOException("F11_STATE");
        int[] restoredPrevious=null;int[][] restoredKeys=new int[CAP][10],restoredLabels=new int[CAP][10];double[] restoredMetric=new double[10];
        try{if(present==1){restoredPrevious=new int[10];for(int i=0;i<10;i++)restoredPrevious[i]=in.readInt();OnlineModelsSU2.requireDraw(restoredPrevious);}for(int i=0;i<10;i++){restoredMetric[i]=in.readDouble();if(!Double.isFinite(restoredMetric[i])||restoredMetric[i]<.001||restoredMetric[i]>8)throw new IOException("F11_METRIC");}for(int i=0;i<restoredSize;i++){for(int j=0;j<10;j++)restoredKeys[i][j]=in.readInt();for(int j=0;j<10;j++)restoredLabels[i][j]=in.readInt();OnlineModelsSU2.requireDraw(restoredKeys[i]);OnlineModelsSU2.requireDraw(restoredLabels[i]);}}catch(IllegalArgumentException bad){throw new IOException("F11_DRAW",bad);}
        long restoredUpdates=0;boolean known=false;if(magic==0x26110002){restoredUpdates=in.readLong();int flag=in.readUnsignedByte();if(restoredUpdates<0||restoredUpdates>restoredT||flag>1)throw new IOException("F11_UPDATE_HISTORY");known=flag==1;}
        t=restoredT;size=restoredSize;next=restoredNext;previous=restoredPrevious;metric=restoredMetric;keys=restoredKeys;labels=restoredLabels;updates=restoredUpdates;completeUpdateHistory=known;last=null;
    }
}
