package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Synthetic permutation history measures engineering cost and exact arithmetic preservation only. */
public final class IncumbentProfileR31HostTest {
    private static String digest(XiIncumbentSU2.Decision decision)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        out.writeBoolean(decision.act);out.writeInt(decision.position);out.writeInt(decision.mask());out.writeLong(Double.doubleToLongBits(decision.phat));
        for(float[] row:decision.table)for(float value:row)out.writeInt(Float.floatToIntBits(value));out.flush();
        StringBuilder text=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()))text.append(String.format(Locale.ROOT,"%02x",value&255));return text.toString();
    }
    public static void main(String[] args)throws Exception{
        int[][] history=new int[1800][10];Random random=new Random(9261647);
        for(int[] row:history){for(int p=0;p<10;p++)row[p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),value=row[p];row[p]=row[q];row[q]=value;}}
        String continuous=digest(XiIncumbentSU2.decideNextFull(history));
        com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
        if(!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        AtomicLong initialAllocation=new AtomicLong(-1),finalAllocation=new AtomicLong();
        int quanta=0;long cpu=0,maxGap=0,maxSlice=0;String resumed=null;Set<String> phases=new TreeSet<>();
        CooperativeContinuationR26<XiIncumbentSU2.Decision> work=new CooperativeContinuationR26<>("r31-profile-xi",()->XiIncumbentSU2.decideNextFull(history),()->false,null,()->{
            long bytes=bean.getThreadAllocatedBytes(Thread.currentThread().getId());initialAllocation.compareAndSet(-1,bytes);finalAllocation.set(bytes);return bean.getCurrentThreadCpuTime();
        },"r31-profile-xi");
        long start=System.nanoTime();
        try{
            while(System.nanoTime()-start<60_000_000_000L){
                long sliceStart=System.nanoTime();CooperativeContinuationR26.Slice<XiIncumbentSU2.Decision> slice=work.slice(3);maxSlice=Math.max(maxSlice,System.nanoTime()-sliceStart);
                quanta++;cpu+=slice.cpuNanos;maxGap=Math.max(maxGap,slice.maxPollGapNanos);
                try{Object snapshot=work.getClass().getMethod("snapshot").invoke(work);phases.add(String.valueOf(snapshot.getClass().getField("phase").get(snapshot)));}catch(NoSuchMethodException unsupported){phases.add("BASELINE_UNOBSERVED");}
                if(slice.error!=null)throw new AssertionError(slice.error);if(slice.done){resumed=digest(slice.result);break;}
            }
        }finally{work.close();if(!CooperativeContinuationR26.awaitOwnerStopped("r31-profile-xi",5000))throw new AssertionError("profile worker leaked");}
        if(!continuous.equals(resumed))throw new AssertionError("continuous/sliced arithmetic changed");
        System.out.println("{\"scope\":\"SYNTHETIC_ENGINEERING_PROFILE_NOT_NATURAL_QUALITY\",\"history\":1800,\"quanta\":"+quanta+",\"workerCpuNanos\":"+cpu+",\"workerAllocatedBytes\":"+(finalAllocation.get()-initialAllocation.get())+",\"maxPollGapNanos\":"+maxGap+",\"maxSliceWallNanos\":"+maxSlice+",\"continuousAndSlicedSha256\":\""+continuous+"\",\"phaseCount\":"+phases.size()+"}");
    }
}
