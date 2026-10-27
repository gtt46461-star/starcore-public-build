package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Forecast caching must preserve exact output and may not change the following learning step. */
public final class R26ForecastSnapshotTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void rejects(byte[] bytes)throws Exception{try{ForecastSnapshotR26.read(bytes);throw new AssertionError("bad cache accepted");}catch(IOException expected){}}
    public static void main(String[] args)throws Exception{
        ModelGenomeSU2 genome=FamilyRegistryR26.genome("F17");LiveStateR26 live=new LiveStateR26(genome);int[] draw={1,2,3,4,5,6,7,8,9,10};
        for(int t=0;t<64;t++){live.forecast();live.learn(draw);int first=draw[0];System.arraycopy(draw,1,draw,0,9);draw[9]=first;}
        byte[] checkpoint=live.encode();LiveStateR26 a=LiveStateR26.decode(genome.text(),checkpoint,LiveStateR26.hash(checkpoint)),b=LiveStateR26.decode(genome.text(),checkpoint,LiveStateR26.hash(checkpoint));
        ForecastSnapshotR26 cached=new ForecastSnapshotR26(a.forecast(),a.table());byte[] bytes=cached.bytes();check(bytes.length==437,"unbounded forecast");ForecastSnapshotR26 restored=ForecastSnapshotR26.read(bytes);ModelLiveSU2.Decision direct=b.forecast();
        check(direct.position==restored.decision.position&&direct.mask==restored.decision.mask&&direct.act==restored.decision.act&&Double.doubleToLongBits(direct.phat)==Double.doubleToLongBits(restored.decision.phat),"cache changed selected action/probability");check(Arrays.equals(ForecastSnapshotR26.encodeTable(b.table()),restored.tableBytes()),"cache changed full marginal");
        LiveStateR26 c=LiveStateR26.decode(genome.text(),a.encode(),LiveStateR26.hash(a.encode()));ForecastSnapshotR26 recomputed=new ForecastSnapshotR26(c.forecast(),c.table());check(Arrays.equals(recomputed.bytes(),restored.bytes()),"prepared checkpoint reload changed next forecast");
        a.learn(draw);b.learn(draw);c.learn(draw);check(Arrays.equals(a.encode(),b.encode())&&Arrays.equals(c.encode(),b.encode()),"preparation changed training continuation");byte[] table=restored.tableBytes();table[0]^=1;check(!Arrays.equals(table,restored.tableBytes()),"mutable cache");
        rejects(Arrays.copyOf(bytes,bytes.length-1));rejects(Arrays.copyOf(bytes,bytes.length+1));byte[] invalid=bytes.clone();invalid[0]^=1;rejects(invalid);invalid=bytes.clone();invalid[15]=0;rejects(invalid);
        float[][] uniform=new float[10][10];for(float[] row:uniform)Arrays.fill(row,.1f);ForecastSnapshotR26 pending=new ForecastSnapshotR26(new ModelLiveSU2.Decision(0,1,7,false,Double.NaN,Double.NaN),uniform);check(Double.isNaN(ForecastSnapshotR26.read(pending.bytes()).decision.phat),"unavailable probability invented");uniform[0]=new float[2];try{ForecastSnapshotR26.encodeTable(uniform);throw new AssertionError("ragged table accepted");}catch(IOException expected){}
        System.out.println("R26_FORECAST_SNAPSHOT_PASS");
    }
}
