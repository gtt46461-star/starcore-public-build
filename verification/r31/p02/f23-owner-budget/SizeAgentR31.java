package com.ethk3.novaonepro.strategyv2;
import java.lang.instrument.Instrumentation;import java.lang.reflect.*;import java.util.*;import java.lang.ref.Reference;
/** HOST object-size observation only; no model/probe/GC replacement. */
public final class SizeAgentR31 {
 private static Instrumentation instrumentation;public static void premain(String args,Instrumentation i){instrumentation=i;}
 public static Map<String,Object> graph(Object root,Set<Object> boundaries)throws Exception{
  if(instrumentation==null)throw new IllegalStateException("ACTUAL_INSTRUMENTATION_REQUIRED");IdentityHashMap<Object,Boolean> seen=new IdentityHashMap<>();ArrayDeque<Object> pending=new ArrayDeque<>();pending.add(root);long bytes=0;Map<String,Long> classes=new TreeMap<>();
  while(!pending.isEmpty()){Object o=pending.remove();if(o==null||boundaries.contains(o)||seen.put(o,true)!=null)continue;if(seen.size()>100000)throw new IllegalStateException("HOST_GRAPH_BOUND");Class<?> type=o.getClass();long n=instrumentation.getObjectSize(o);bytes+=n;classes.put(type.getName(),classes.getOrDefault(type.getName(),0L)+n);if(type.isEnum()||o instanceof Class||o instanceof Reference)continue;if(type.isArray()){if(!type.getComponentType().isPrimitive())for(Object item:(Object[])o)if(item!=null)pending.add(item);continue;}for(Class<?> t=type;t!=null&&t!=Object.class;t=t.getSuperclass())for(Field f:t.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getType().isPrimitive()){f.setAccessible(true);Object value=f.get(o);if(value!=null)pending.add(value);}}
  Map<String,Object> out=new LinkedHashMap<>();out.put("bytes",bytes);out.put("objects",seen.size());out.put("classes",classes);out.put("basis","ACTUAL_HOST_INSTRUMENTATION_SHALLOW_SIZES_SUM_OVER_STRONG_OBJECT_GRAPH; BOUNDARIES_EXPLICIT; WEAK_EDGES_NOT_FOLLOWED; ENUM_CLASS_ROOTS_STOPPED");return out;
 }
}
