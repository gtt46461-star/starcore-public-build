package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Relation-symbol suffix automaton with supported state splits and hierarchical shrinkage. */
public final class FamilyF12KernelR26 implements TemporalModelR26,StatisticalTrainingDiagnosticsR26 {
    private double[][][] count=new double[111][10][10];private long[] support=new long[111];private int previous=-1,older=-1;private long t;private StatisticalTrainingDiagnosticsR26.Step last;
    public void resetTemporalContext(){previous=-1;older=-1;last=null;}
    public int stateWidth(){return 3;}
    private int state(){if(previous<0)return 0;int first=1+previous,deep=older<0?-1:11+older*10+previous;return deep>=0&&support[first]>=20&&support[deep]>=4?deep:first;}
    private double prob(int state,int p,int n){if(state==0)return (count[0][p][n]+1)/(support[0]+10.0);int parent=state<11?0:1+(state-11)%10;return (count[state][p][n]+10*prob(parent,p,n))/(support[state]+10.0);}
    public void predict(float[][] p,float[] st){int state=state();for(int i=0;i<10;i++)for(int j=0;j<10;j++)p[i][j]=(float)prob(state,i,j);st[0]=state;st[1]=support[state];int nodes=1;for(int i=1;i<111;i++)if(support[i]>0)nodes++;st[2]=nodes;}
    public StatisticalTrainingDiagnosticsR26.Step lastStatisticalTrainingStep(){return last;}
    public long completedTrainingUpdates(){return t;}
    public String optimizerProtocol(){return "SUFFIX_CATEGORICAL_COUNTS_HIERARCHICAL_SHRINKAGE_V1|KNOWN_LIFETIME";}
    private double updatedProb(int state,int p,int n,int[] target,int[] touched){
        boolean changed=false;for(int x:touched)if(x==state)changed=true;
        double c=count[state][p][n]+(changed&&target[p]==n+1?1:0);long total=support[state]+(changed?1:0);
        if(state==0)return (c+1)/(total+10.0);int parent=state<11?0:1+(state-11)%10;
        return (c+10*updatedProb(parent,p,n,target,touched))/(total+10.0);
    }
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(t==Long.MAX_VALUE)throw new IllegalStateException("F12_UPDATE_CLOCK_CAPACITY");
        int selected=state();int[] touched={0,previous<0?-1:1+previous,previous<0||older<0?-1:11+older*10+previous};
        double before=0,after=0;for(int p=0;p<10;p++){before-=Math.log(prob(selected,p,draw[p]-1))/10;after-=Math.log(updatedProb(selected,p,draw[p]-1,draw,touched))/10;}
        double countDelta=0,supportDelta=0;for(int node:touched)if(node>=0){double ds=(double)((support[node]+1)-support[node]);supportDelta+=ds*ds;for(int p=0;p<10;p++){double old=count[node][p][draw[p]-1],delta=(old+1)-old;countDelta+=delta*delta;}}
        String input=PolicyIdentityR26.sha256("F12_PRE_LABEL_SUFFIX_V1|"+t+"|"+older+"|"+previous+"|"+selected);
        StatisticalTrainingDiagnosticsR26.Step step=new StatisticalTrainingDiagnosticsR26.Step(t+1,before,after,Math.sqrt(countDelta+supportDelta),Math.sqrt(countDelta),input,"MEAN_MARGINAL_CATEGORICAL_NLL_AT_FIXED_PRE_LABEL_SUFFIX_NODE;HIERARCHICAL_COUNT_UPDATE_NOT_SGD;PARAMETER_DELTA=CATEGORICAL_COUNTS_EXCLUDING_SUPPORT_AND_CONTEXT","SUFFIX_CATEGORICAL_COUNTS_AND_SUPPORT");
        ExecutionBudgetR26.poll();for(int node:touched)if(node>=0){support[node]++;for(int p=0;p<10;p++)count[node][p][draw[p]-1]++;}older=previous;previous=Math.floorMod(draw[0]-draw[1],10);t++;last=step;
    }
    public void write(DataOutput o)throws IOException{o.writeInt(0x26120001);o.writeLong(t);o.writeInt(previous);o.writeInt(older);for(int s=0;s<111;s++){o.writeLong(support[s]);for(double[] row:count[s])for(double x:row)o.writeDouble(x);}}
    public void read(DataInput in)throws IOException{
        if(in.readInt()!=0x26120001)throw new IOException("F12_ABI");long restoredT=in.readLong();int restoredPrevious=in.readInt(),restoredOlder=in.readInt();
        if(restoredT<0||restoredPrevious< -1||restoredPrevious>9||restoredOlder< -1||restoredOlder>9)throw new IOException("F12_STATE");
        long[] restoredSupport=new long[111];double[][][] restoredCount=new double[111][10][10];
        for(int s=0;s<111;s++){restoredSupport[s]=in.readLong();if(restoredSupport[s]<0||restoredSupport[s]>restoredT)throw new IOException("F12_SUPPORT");for(double[] row:restoredCount[s])for(int j=0;j<10;j++){row[j]=in.readDouble();if(!Double.isFinite(row[j])||row[j]<0||row[j]>restoredSupport[s])throw new IOException("F12_COUNT");}}
        t=restoredT;previous=restoredPrevious;older=restoredOlder;support=restoredSupport;count=restoredCount;last=null;
    }
}
