package com.ethk3.novaonepro.strategyv2;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Runs unchanged against both actual baseline and current production guard. HOST evidence only. */
public final class PitGuardAllocationR31Host {
    private static volatile Object escaped;
    private static PitGuardR30.Freeze legal() {
        PitGuardR30.Freeze f = new PitGuardR30.Freeze();
        f.targetOrdinal=100; f.trainedThroughOrdinal=99; f.featureThroughOrdinal=99;
        f.position=4; f.k3=new int[]{1,5,9}; f.probabilities=new double[10];
        Arrays.fill(f.probabilities,.1);
        f.modelAvailableAt=1000; f.featureAvailableAt=1000; f.preparedAt=2000;
        f.frozenAt=3000; f.deadlineAt=9000;
        return f;
    }
    private static final long[] TIMES={Long.MIN_VALUE,-1,0,1,2999,3000,3001,4000,4001,9000,Long.MAX_VALUE};
    private static final double[] PROBS={Double.NaN,Double.NEGATIVE_INFINITY,-1,-Double.MIN_VALUE,0,.1,1,Math.nextUp(1.0),Double.POSITIVE_INFINITY};
    private static String hex(byte[] bytes) {
        StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
    }
    public static void main(String[] args)throws Exception {
        Random random=new Random(3185);MessageDigest digest=MessageDigest.getInstance("SHA-256");
        long violations=0,valid=0;final int cases=20000;
        for(int i=0;i<cases;i++) {
            PitGuardR30.Freeze f=legal();
            if(i%31==0)f=null;
            else if((i&1)==1) {
                f.targetOrdinal=TIMES[random.nextInt(TIMES.length)];
                f.trainedThroughOrdinal=TIMES[random.nextInt(TIMES.length)];
                f.featureThroughOrdinal=TIMES[random.nextInt(TIMES.length)];
                f.position=random.nextInt(14)-1;
                int length=random.nextInt(6);f.k3=length==5?null:new int[length];
                if(f.k3!=null)for(int j=0;j<f.k3.length;j++)f.k3[j]=random.nextInt(15)-2;
                length=random.nextInt(14);f.probabilities=length==13?null:new double[length];
                if(f.probabilities!=null)for(int j=0;j<f.probabilities.length;j++)f.probabilities[j]=PROBS[random.nextInt(PROBS.length)];
                f.modelAvailableAt=TIMES[random.nextInt(TIMES.length)];
                f.featureAvailableAt=TIMES[random.nextInt(TIMES.length)];
                f.preparedAt=TIMES[random.nextInt(TIMES.length)];
                f.frozenAt=TIMES[random.nextInt(TIMES.length)];
                f.deadlineAt=TIMES[random.nextInt(TIMES.length)];
                f.replay=random.nextBoolean();
            }
            List<String> result=PitGuardR30.check(f);violations+=result.size();if(result.isEmpty())valid++;
            digest.update((i+"|"+result.toString()+"\n").getBytes(StandardCharsets.UTF_8));
        }
        PitGuardR30.Freeze f=legal();for(int i=0;i<100000;i++)escaped=PitGuardR30.check(f);
        java.lang.management.ThreadMXBean bean=ManagementFactory.getThreadMXBean();
        Long allocated=null;final int repetitions=1000000;
        if(bean instanceof com.sun.management.ThreadMXBean) {
            com.sun.management.ThreadMXBean allocation=(com.sun.management.ThreadMXBean)bean;
            if(allocation.isThreadAllocatedMemorySupported()) {
                if(!allocation.isThreadAllocatedMemoryEnabled())allocation.setThreadAllocatedMemoryEnabled(true);
                long id=Thread.currentThread().getId(),before=allocation.getThreadAllocatedBytes(id);
                for(int i=0;i<repetitions;i++)escaped=PitGuardR30.check(f);
                allocated=allocation.getThreadAllocatedBytes(id)-before;
            }
        }
        System.out.println("{\"scope\":\"HOST_ACTUAL_GUARD_ORDERED_DIFFERENTIAL_AND_THREAD_ALLOCATIONS_NOT_DEVICE_OR_QUALITY\","
                +"\"cases\":"+cases+",\"valid\":"+valid+",\"violations\":"+violations+","
                +"\"orderedViolationSha256\":\""+hex(digest.digest())+"\",\"legalChecksMeasured\":"+repetitions+","
                +"\"actualThreadAllocatedBytes\":"+allocated+",\"forcedGc\":false}");
    }
}
