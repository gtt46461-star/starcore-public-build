package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** A frozen development probe is a redundancy diagnostic, never a proof of global equivalence. */
public final class BehaviorSignatureSU1 {
    public final String source,distributionHash,actionHash;
    private final short[] choices;
    public final long active,errors;
    private BehaviorSignatureSU1(String source,String distribution,String actions,short[] choices,long active,long errors){this.source=source;distributionHash=distribution;actionHash=actions;this.choices=choices.clone();this.active=active;this.errors=errors;}
    public static int probeIndex(int t,int lo,int hi){int n=Math.min(64,hi-lo);for(int i=0;i<n;i++)if(t==lo+(int)((long)i*(hi-lo)/n))return i;return -1;}
    public static String scoreHash(double[][] scores){StringBuilder b=new StringBuilder("SU1_F32_DISTRIBUTION_PROBE_V1|");for(double[] row:scores)for(double value:row)b.append(Double.toHexString(value)).append(',');return HashingSU1.text(b.toString());}
    public static BehaviorSignatureSU1 from(HistoricalReplayEngineSU1.Replay replay,int lo,int hi,String dataset){
        int n=Math.min(64,hi-lo);short[] choices=new short[n];long active=0,errors=0;StringBuilder dist=new StringBuilder(),act=new StringBuilder();
        for(int i=0;i<n;i++){
            int t=lo+(int)((long)i*(hi-lo)/n);choices[i]=(short)((replay.position[t]-1)*1024+replay.mask[t]);
            if(replay.act[t])active|=1L<<i;if(!replay.hit[t])errors|=1L<<i;
            dist.append(replay.probeScores[i]==null?"UNAVAILABLE":replay.probeScores[i]).append('|');act.append(choices[i]).append(':').append(replay.act[t]).append('|');
        }
        return new BehaviorSignatureSU1(HashingSU1.fields(dataset,String.valueOf(lo),String.valueOf(hi)),HashingSU1.text(dist.toString()),HashingSU1.text(act.toString()),choices,active,errors);
    }
    public int matched(BehaviorSignatureSU1 other){requireSame(other);return Long.bitCount(active&other.active);}
    private void requireSame(BehaviorSignatureSU1 other){if(!source.equals(other.source)||choices.length!=other.choices.length)throw new IllegalArgumentException("SU1_PROBE_COHORT_MISMATCH");}
    public double similarity(BehaviorSignatureSU1 other){requireSame(other);long mask=active&other.active;int n=Long.bitCount(mask),same=0;for(int i=0;i<choices.length;i++)if((mask&(1L<<i))!=0&&choices[i]==other.choices[i])same++;return n==0?1:same/(double)n;}
    public double complementarity(BehaviorSignatureSU1 other){
        requireSame(other);long common=active&other.active;int n=Long.bitCount(common);
        int a=Long.bitCount(errors&~other.errors&common),b=Long.bitCount(other.errors&~errors&common);
        if(n<20||a<2||b<2||similarity(other)>=.98)return Double.NEGATIVE_INFINITY;
        return (a+b)/(double)n-.1*similarity(other);
    }
    public void write(DataOutput out)throws IOException{out.writeUTF(source);out.writeUTF(distributionHash);out.writeUTF(actionHash);out.writeLong(active);out.writeLong(errors);out.writeInt(choices.length);for(short choice:choices)out.writeShort(choice);}
    public static BehaviorSignatureSU1 read(DataInput in)throws IOException{String source=HashingSU1.sha(in.readUTF()),distribution=HashingSU1.sha(in.readUTF()),actions=HashingSU1.sha(in.readUTF());long active=in.readLong(),errors=in.readLong();int n=in.readInt();if(n<1||n>64)throw new IOException("SU1_PROBE_SIZE");short[] choices=new short[n];for(int i=0;i<n;i++){choices[i]=in.readShort();if(choices[i]<0||choices[i]>=10240)throw new IOException("SU1_PROBE_CHOICE");}return new BehaviorSignatureSU1(source,distribution,actions,choices,active,errors);}
}
