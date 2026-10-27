package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** Causal state-fragment memory. Each bounded slot owns an entry signature, an
 * expert's sufficient statistics, its immutable initial prior and validity times.
 * Only revealed labels update an expert; retrieval never observes the next label.
 * A fragment supplies a prior, never qualification samples or live authority. */
public final class FamilyF20KernelR26 implements TemporalModelR26 {
    private static final int MAGIC=0x26200001,HISTORY=8,DIM=200;
    public enum Retrieval { MATCH, NONE, MISMATCH, EXPIRED }
    private static final class Fragment {
        final double[] signature=new double[DIM],expert=new double[100],initial=new double[100];
        long entered,lastUsed,exited=-1,entries=1,updates,index;
    }
    private final int capacity;private final Retrieval mode;private final Fragment[] fragments;
    private final double[] global=new double[100];private final int[][] recent=new int[HISTORY][10];
    private long t;private int seen,active=-1;
    public FamilyF20KernelR26(){this(16,Retrieval.MATCH);}
    public FamilyF20KernelR26(int capacity,Retrieval mode){
        if(capacity<8||capacity>32||capacity%8!=0||mode==null)throw new IllegalArgumentException("F20_MEMORY_GRAPH");
        this.capacity=capacity;this.mode=mode;fragments=new Fragment[capacity];Arrays.fill(global,.1);
    }
    public int stateWidth(){return 6;}
    /** Context supports 8 observed labels; fragment validity is capped at 5000
     * update periods. It does not claim 32/128/512/2048-label feature encoders. */
    public void resetTemporalContext(){seen=0;active=-1;for(int[] row:recent)Arrays.fill(row,0);}
    private double[] signature(){
        double[] signature=new double[DIM];if(seen<HISTORY)return signature;
        for(int lag=0;lag<HISTORY;lag++){int[] d=recent[(int)((t-1-lag)%HISTORY)];for(int p=0;p<10;p++)signature[p*10+d[p]-1]+=1.0/HISTORY;}
        int[] last=recent[(int)((t-1)%HISTORY)];for(int p=0;p<10;p++)signature[100+p*10+last[p]-1]=1;
        return signature;
    }
    private static double distance(double[] a,double[] b){double sum=0;for(int i=0;i<DIM;i++){double delta=a[i]-b[i];sum+=delta*delta;}return sum/20;}
    private boolean valid(Fragment fragment){return fragment!=null&&fragment.updates>0&&t-fragment.lastUsed<=(mode==Retrieval.EXPIRED?32:5000);}
    private int select(double[] signature,boolean mismatched){
        int selected=-1;double best=mismatched?-1:Double.POSITIVE_INFINITY;
        for(int i=0;i<capacity;i++){ExecutionBudgetR26.poll();if(!valid(fragments[i]))continue;double distance=distance(signature,fragments[i].signature);
            if(mismatched?distance>best:distance<best){best=distance;selected=i;}}
        return selected;
    }
    private static void normalized(double[] counts,float[][] out){
        for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++)sum+=counts[p*10+n];for(int n=0;n<10;n++)out[p][n]=(float)(counts[p*10+n]/sum);}
    }
    public void predict(float[][] out,float[] state){
        ExecutionBudgetR26.poll();normalized(global,out);double[] signature=signature();
        int nearest=seen==HISTORY&&mode!=Retrieval.NONE?select(signature,false):-1;
        int selected=nearest<0?-1:mode==Retrieval.MISMATCH?select(signature,true):nearest;
        double match=nearest<0?Double.POSITIVE_INFINITY:distance(signature,fragments[nearest].signature),blend=0;
        if(selected>=0&&match<=.35){Fragment fragment=fragments[selected];blend=.9*fragment.updates/(fragment.updates+8.0)*Math.exp(-3*match);
            for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++)sum+=fragment.expert[p*10+n];for(int n=0;n<10;n++)out[p][n]=(float)((1-blend)*out[p][n]+blend*fragment.expert[p*10+n]/sum);}}
        int occupied=0,valid=0;for(Fragment fragment:fragments)if(fragment!=null){occupied++;if(valid(fragment))valid++;}
        state[0]=occupied/(float)capacity;state[1]=valid/(float)capacity;state[2]=(float)blend;state[3]=nearest<0?1:(float)Math.min(1,match);state[4]=seen/(float)HISTORY;state[5]=(float)Math.log1p(t);
    }
    private static long index(double[] signature){long hash=0xcbf29ce484222325L;for(double value:signature){hash^=Math.round(value*4096);hash*=0x100000001b3L;}return hash;}
    private int allocate(double[] signature){
        int chosen=0;for(int i=0;i<capacity;i++){if(fragments[i]==null){chosen=i;break;}if(fragments[i].lastUsed<fragments[chosen].lastUsed)chosen=i;}
        Fragment fragment=new Fragment();fragment.entered=t;fragment.lastUsed=t;System.arraycopy(signature,0,fragment.signature,0,DIM);fragment.index=index(signature);
        for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++)sum+=global[p*10+n];for(int n=0;n<10;n++)fragment.expert[p*10+n]=fragment.initial[p*10+n]=global[p*10+n]/sum;}
        fragments[chosen]=fragment;return chosen;
    }
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();double[] signature=signature();
        int selected=seen==HISTORY&&mode!=Retrieval.NONE?select(signature,false):-1;
        // All cooperative polls precede mutation; one update is a bounded 32-slot operation.
        if(seen==HISTORY&&mode!=Retrieval.NONE){
            if(selected<0||distance(signature,fragments[selected].signature)>.18)selected=allocate(signature);
            if(active!=selected){if(active>=0&&fragments[active]!=null)fragments[active].exited=t;if(fragments[selected].updates>0)fragments[selected].entries++;active=selected;}
            Fragment fragment=fragments[selected];double rate=1.0/Math.min(64,fragment.updates+1);
            for(int i=0;i<DIM;i++)fragment.signature[i]+=rate*(signature[i]-fragment.signature[i]);fragment.index=index(fragment.signature);
            for(int i=0;i<100;i++)fragment.expert[i]*=.999;
            for(int p=0;p<10;p++)fragment.expert[p*10+draw[p]-1]++;
            fragment.updates++;fragment.lastUsed=t;
        }
        for(int i=0;i<100;i++)global[i]*=.97;for(int p=0;p<10;p++)global[p*10+draw[p]-1]++;
        recent[(int)(t%HISTORY)]=draw.clone();seen=Math.min(HISTORY,seen+1);t++;
    }
    int occupied(){int n=0;for(Fragment f:fragments)if(f!=null)n++;return n;}
    int validFragments(){int n=0;for(Fragment f:fragments)if(valid(f))n++;return n;}
    long trainedPeriods(){return t;}
    String learnedParameterHash(){StringBuilder s=new StringBuilder("F20_LEARNED_PARAMETERS");for(double value:global)s.append('|').append(Double.toHexString(value));for(Fragment f:fragments)if(f!=null){for(double[] values:new double[][]{f.signature,f.expert,f.initial})for(double value:values)s.append('|').append(Double.toHexString(value));s.append('|').append(f.updates);}return PolicyIdentityR26.sha256(s.toString());}
    public void write(DataOutput out)throws IOException{
        out.writeInt(MAGIC);out.writeInt(capacity);out.writeUTF(mode.name());out.writeLong(t);out.writeInt(seen);out.writeInt(active);
        for(int[] row:recent)for(int value:row)out.writeByte(value);for(double value:global)out.writeDouble(value);
        for(Fragment f:fragments){out.writeBoolean(f!=null);if(f==null)continue;out.writeLong(f.entered);out.writeLong(f.lastUsed);out.writeLong(f.exited);out.writeLong(f.entries);out.writeLong(f.updates);out.writeLong(f.index);for(double[] values:new double[][]{f.signature,f.expert,f.initial})for(double value:values)out.writeDouble(value);}
    }
    public void read(DataInput in)throws IOException{
        if(in.readInt()!=MAGIC||in.readInt()!=capacity||!in.readUTF().equals(mode.name()))throw new IOException("F20_CHECKPOINT_GRAPH");
        long clock=in.readLong();int context=in.readInt(),current=in.readInt();if(clock<0||context<0||context>HISTORY||context>clock||current< -1||current>=capacity)throw new IOException("F20_CHECKPOINT_CURSOR");
        int[][] history=new int[HISTORY][10];for(int[] row:history)for(int i=0;i<10;i++){row[i]=in.readUnsignedByte();if(row[i]>10)throw new IOException("F20_CHECKPOINT_HISTORY");}
        for(int lag=0;lag<context;lag++)try{OnlineModelsSU2.requireDraw(history[(int)((clock-1-lag)%HISTORY)]);}catch(IllegalArgumentException invalid){throw new IOException("F20_CHECKPOINT_DRAW",invalid);}
        double[] base=new double[100];for(int i=0;i<100;i++)base[i]=number(in,10000);
        Fragment[] restored=new Fragment[capacity];for(int slot=0;slot<capacity;slot++){if(!in.readBoolean())continue;Fragment f=new Fragment();f.entered=in.readLong();f.lastUsed=in.readLong();f.exited=in.readLong();f.entries=in.readLong();f.updates=in.readLong();f.index=in.readLong();
            if(f.entered<0||f.lastUsed<f.entered||f.lastUsed>=clock||f.exited< -1||f.exited>clock||f.entries<1||f.entries>clock||f.updates<1||f.updates>clock)throw new IOException("F20_CHECKPOINT_FRAGMENT_CLOCK");
            for(int i=0;i<DIM;i++)f.signature[i]=number(in,1);for(int i=0;i<100;i++)f.expert[i]=number(in,10000);for(int i=0;i<100;i++)f.initial[i]=number(in,1);
            if(f.index!=index(f.signature))throw new IOException("F20_CHECKPOINT_INDEX");for(int p=0;p<10;p++){double initial=0,expert=0,a=0,b=0;for(int n=0;n<10;n++){initial+=f.initial[p*10+n];expert+=f.expert[p*10+n];a+=f.signature[p*10+n];b+=f.signature[100+p*10+n];}if(Math.abs(initial-1)>1e-8||expert<=0||Math.abs(a-1)>1e-8||Math.abs(b-1)>1e-8)throw new IOException("F20_CHECKPOINT_NORMALIZATION");}restored[slot]=f;}
        if(current>=0&&restored[current]==null||mode==Retrieval.NONE&&(current>=0||Arrays.stream(restored).anyMatch(Objects::nonNull)))throw new IOException("F20_CHECKPOINT_ACTIVE");
        for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++)sum+=base[p*10+n];if(sum<=0)throw new IOException("F20_CHECKPOINT_GLOBAL");}
        t=clock;seen=context;active=current;for(int i=0;i<HISTORY;i++)System.arraycopy(history[i],0,recent[i],0,10);System.arraycopy(base,0,global,0,100);System.arraycopy(restored,0,fragments,0,capacity);
    }
    private static double number(DataInput in,double max)throws IOException{double value=in.readDouble();if(!Double.isFinite(value)||value<0||value>max)throw new IOException("F20_CHECKPOINT_NUMBER");return value;}
}
