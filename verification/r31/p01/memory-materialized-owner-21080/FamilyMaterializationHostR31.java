package com.ethk3.novaonepro.strategyv2;

import java.lang.reflect.*;
import java.util.*;

/** Actual production model allocations and default process RuntimeProbe, original 512MiB.
 * This is an allocation-accounting counterexample, never a natural-quality certificate. */
public final class FamilyMaterializationHostR31 {
    private static Object field(Object x,String name)throws Exception{Field f=x.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(x);}
    private static long n(Map<String,Object> x,String name){return((Number)x.get(name)).longValue();}
    private static long actualPrimitivePayload(Object root)throws Exception{
        IdentityHashMap<Object,Boolean> seen=new IdentityHashMap<>();ArrayDeque<Object> pending=new ArrayDeque<>();pending.add(root);long bytes=0;
        while(!pending.isEmpty()){
            Object x=pending.removeFirst();if(seen.put(x,Boolean.TRUE)!=null)continue;Class<?> t=x.getClass();
            if(t.isArray()){
                Class<?> c=t.getComponentType();int size=Array.getLength(x);
                if(c.isPrimitive())bytes+=(long)size*(c==long.class||c==double.class?8:c==float.class||c==int.class?4:c==char.class||c==short.class?2:1);
                else for(Object item:(Object[])x)if(item!=null)pending.add(item);
            }else if(x instanceof Map<?,?>){for(Map.Entry<?,?> entry:((Map<?,?>)x).entrySet()){if(entry.getKey()!=null)pending.add(entry.getKey());if(entry.getValue()!=null)pending.add(entry.getValue());}}
            else if(x instanceof Collection<?>){for(Object item:(Collection<?>)x)if(item!=null)pending.add(item);}
            else if(!t.getName().startsWith("java."))for(Class<?> owner=t;owner!=null&&owner!=Object.class;owner=owner.getSuperclass())for(Field f:owner.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getType().isPrimitive()){f.setAccessible(true);Object value=f.get(x);if(value!=null)pending.add(value);}
            if(seen.size()>200000)throw new AssertionError("ENGINEERING_GRAPH_BOUND");
        }
        return bytes;
    }
    public static void main(String[] args)throws Exception{
        if(Runtime.getRuntime().maxMemory()!=512L*1024*1024)throw new AssertionError("ORIGINAL_512_MIB_REQUIRED");
        MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();Map<String,Object> before=memory.sample();
        List<FeatureCacheR27.FamilyStream> streams=new ArrayList<>();List<Object> rows=new ArrayList<>();long primitive=0,declared=0,recorded=0;
        try{
            for(int family:new int[]{3,4,5}){
                FeatureCacheR27.FamilyStream stream=new FeatureCacheR27.FamilyStream(family);streams.add(stream);
                MemoryBudgetR31.Lease lease=(MemoryBudgetR31.Lease)field(field(stream,"ownerR31"),"model");
                long actual=actualPrimitivePayload(stream.model),covered=((Number)field(lease,"allocatedPayloadBytes")).longValue();primitive+=actual;declared+=lease.bytes();recorded+=covered;
                Map<String,Object> row=new LinkedHashMap<>();row.put("family",family);row.put("actualOwnedPrimitivePayloadBytes",actual);row.put("leaseDeclaredBytes",lease.bytes());row.put("registeredPrimitivePayloadBytes",covered);rows.add(row);
            }
            memory.sample();Map<String,Object> after=memory.snapshot();Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL_MODEL_CONSTRUCTORS_RUNTIME_PROBE_ACCOUNTING_ONLY_NOT_ANDROID_OR_QUALITY");result.put("maxHeapBytes",Runtime.getRuntime().maxMemory());result.put("before",before);result.put("after",after);result.put("rows",rows);result.put("primitivePayloadBytes",primitive);result.put("declaredModelBytes",declared);result.put("registeredPrimitivePayloadBytes",recorded);result.put("forcedGc",false);System.out.println("RESULT "+JsonR27.write(result));
            if(primitive<=6L*1024*1024)throw new AssertionError("ACTUAL_THREE_PRIVATE_NEURAL_TAPES_REQUIRED");
            if(recorded!=primitive)throw new AssertionError("ACTUAL_MODEL_PRIMITIVES_DOUBLE_COUNTED_AS_FUTURE_PROMISES:"+recorded+"/"+primitive);
            if(n(after,"pendingJavaAllocationBytes")!=declared-primitive)throw new AssertionError("PENDING_MUST_RETAIN_METADATA_AND_UNALLOCATED_MARGIN");
            System.out.println("PASS_ACTUAL_MODEL_ACCOUNTING_NOT_FULL485");
        }finally{
            for(FeatureCacheR27.FamilyStream stream:streams)stream.closeForkOwnerR31();
            memory.sample();Map<String,Object> closed=memory.snapshot();System.out.println("CLOSED "+JsonR27.write(closed));
            if(n(closed,"activeLeaseCount")!=0)throw new AssertionError("ACTUAL_MODEL_OWNERS_NOT_CLOSED");
        }
    }
}
