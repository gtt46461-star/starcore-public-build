package com.ethk3.novaonepro.strategyv2;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Synthetic owner-budget and original preference byte tests, not forecast quality evidence. */
public final class R26IncumbentBudgetTest {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static void same(XiIncumbentSU2.Decision a,XiIncumbentSU2.Decision b){
        require(a.act==b.act&&a.position==b.position&&a.mask()==b.mask(),"original selected decision changed");
        require(Double.doubleToLongBits(a.phat)==Double.doubleToLongBits(b.phat),"original gate changed");
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)require(Float.floatToIntBits(a.table[p][n])==Float.floatToIntBits(b.table[p][n]),"original preference changed");
    }
    public static void main(String[] args)throws Exception{
        float[][] raw=new float[10][10];for(float[] row:raw){Arrays.fill(row,.08f);row[0]=.28f;}
        byte[] bytes=LegacyPreferenceTableR26.encode(raw);float[][] decoded=LegacyPreferenceTableR26.decode(bytes);require(bytes.length==400,"preference byte count");
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)require(Float.floatToIntBits(raw[p][n])==Float.floatToIntBits(decoded[p][n]),"preference projected");
        require(Math.abs(decoded[0][0]*10-2.8)<1e-5,"non-joint column was changed");
        try{ForecastSnapshotR26.encodeTable(raw);throw new AssertionError("original preference mislabeled as legal joint marginals");}catch(java.io.IOException|IllegalArgumentException expected){}
        try{LegacyPreferenceTableR26.decode(Arrays.copyOf(bytes,399));throw new AssertionError("truncated preferences accepted");}catch(java.io.IOException expected){}
        byte[] bad=bytes.clone();bad[0]=0x7f;bad[1]=(byte)0xc0;bad[2]=0;bad[3]=0;
        try{LegacyPreferenceTableR26.decode(bad);throw new AssertionError("NaN preferences accepted");}catch(java.io.IOException expected){}
        int[][] history=new int[1800][10];Random random=new Random(9261647);for(int[] row:history){for(int p=0;p<10;p++)row[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),x=row[p];row[p]=row[q];row[q]=x;}}
        XiIncumbentSU2.Decision continuous=XiIncumbentSU2.decideNextFull(history);
        ThreadMXBean cpu=ManagementFactory.getThreadMXBean();require(cpu.isCurrentThreadCpuTimeSupported(),"worker CPU clock unavailable");if(!cpu.isThreadCpuTimeEnabled())cpu.setThreadCpuTimeEnabled(true);
        AtomicBoolean critical=new AtomicBoolean(false);int slices=0;long workerCpu=0;XiIncumbentSU2.Decision resumed=null;
        String owner="SYNTHETIC_XI_BUDGET";
        try(CooperativeContinuationR26<XiIncumbentSU2.Decision> work=new CooperativeContinuationR26<>("synthetic-original-xi",()->XiIncumbentSU2.decideNextFull(history),critical::get,()->{},cpu::getCurrentThreadCpuTime,owner)){
            long end=System.nanoTime()+60_000_000_000L;
            while(System.nanoTime()<end){CooperativeContinuationR26.Slice<XiIncumbentSU2.Decision> slice=work.slice(3);slices++;workerCpu+=slice.cpuNanos;if(slice.error!=null)throw new AssertionError(slice.error);if(slice.done){resumed=slice.result;break;}if(slices==1){critical.set(true);CooperativeContinuationR26.Slice<XiIncumbentSU2.Decision> paused=work.slice(3);workerCpu+=paused.cpuNanos;require(!paused.done,"original work ignored critical pressure");critical.set(false);}}
        }
        require(slices>1&&resumed!=null&&workerCpu>0,"original replay did not resume with measured work");same(continuous,resumed);require(CooperativeContinuationR26.awaitOwnerStopped(owner,5000),"completed original worker leaked");
        AtomicBoolean held=new AtomicBoolean(true);String cancelled="SYNTHETIC_XI_CANCEL";
        CooperativeContinuationR26<XiIncumbentSU2.Decision> abandoned=new CooperativeContinuationR26<>("synthetic-cancel-original",()->XiIncumbentSU2.decideNextFull(history),held::get,()->{},cpu::getCurrentThreadCpuTime,cancelled);
        require(!abandoned.slice(3).done,"critical computation was not paused");abandoned.close();require(CooperativeContinuationR26.awaitOwnerStopped(cancelled,5000),"cancelled original worker leaked");
        System.out.println("R26_INCUMBENT_BUDGET_PASS|slices="+slices+"|workerCpuNanos="+workerCpu+"|exactOriginalDecisionAndPreferences=true|notNaturalEvidence=true");
    }
}
