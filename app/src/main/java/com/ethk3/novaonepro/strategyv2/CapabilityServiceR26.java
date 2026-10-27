package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** CPU service is shared by the mechanisms carried by a task, with a measured P2 reserve. */
public final class CapabilityServiceR26 {
    private final Map<String,Long> costs=new HashMap<>();
    private long p2Cpu,totalCpu;
    public static int weight(String task,int families,int liveModels){return task.equals("foundry-su2")?Math.max(1,families):task.equals("live-model-training")?Math.max(1,liveModels):1;}
    public double score(String task,int weight){if(weight<1)throw new IllegalArgumentException("R26_SERVICE_WEIGHT");return costs.getOrDefault(task,0L)/(double)weight;}
    public void served(String task,String lane,long cpuMillis){
        if(cpuMillis<0)throw new IllegalArgumentException("R26_SERVICE_CPU");costs.put(task,Math.addExact(costs.getOrDefault(task,0L),Math.max(1,cpuMillis)));
        totalCpu+=cpuMillis;if(lane.equals("P2"))p2Cpu+=cpuMillis;if(totalCpu>=60000){totalCpu/=2;p2Cpu/=2;}
    }
    public boolean needsP2(){return totalCpu>0&&p2Cpu<totalCpu*.20;}
}
