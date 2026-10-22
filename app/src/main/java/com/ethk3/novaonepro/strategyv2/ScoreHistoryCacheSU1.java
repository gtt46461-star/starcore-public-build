package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** One dataset-owned bounded LRU. It is never shared between real and control experiments. */
public final class ScoreHistoryCacheSU1 {
    private final int capacity;
    private final LinkedHashMap<String,double[][]> values=new LinkedHashMap<>(64,.75f,true);
    public ScoreHistoryCacheSU1(int capacity){if(capacity<1||capacity>4096)throw new IllegalArgumentException("SU1_CACHE_CAP");this.capacity=capacity;}
    public double[][] score(String program,int target,Supplier<double[][]> compute) {
        String key=target+"|"+program;double[][] v=values.get(key);
        if(v==null){v=compute.get();values.put(key,v);while(values.size()>capacity){String first=values.keySet().iterator().next();values.remove(first);}}
        return v;
    }
    public int size(){return values.size();}
    public void clear(){values.clear();}
}
