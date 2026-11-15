package com.ethk3.novaonepro.strategyv2;
import java.io.*;import java.lang.reflect.*;import java.lang.management.*;import java.util.*;
/** Actual constructor allocations and retained shape, not device calibrated working-set evidence. */
public final class FamilyShapeR31Host {
 public static void main(String[] args)throws Exception{
  Method shape=FeatureCacheR27.FamilyStream.class.getDeclaredMethod("modelShapeR31",Object.class);shape.setAccessible(true);com.sun.management.ThreadMXBean bean=(com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();List<Object> rows=new ArrayList<>();
  for(String id:FamilyRegistryR26.EXECUTABLE){OnlineModelsSU2.Model warm=FamilyRegistryR26.build(id);warm.write(new DataOutputStream(new ByteArrayOutputStream()));long before=bean.getThreadAllocatedBytes(Thread.currentThread().getId());OnlineModelsSU2.Model model=FamilyRegistryR26.build(id);long allocated=bean.getThreadAllocatedBytes(Thread.currentThread().getId())-before;long retained=((Number)shape.invoke(null,model)).longValue();ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));Map<String,Object> row=new LinkedHashMap<>();row.put("id",id);row.put("constructorAllocatedBytes",allocated);row.put("conservativeShapeBytes",retained);row.put("initialCodecBytes",bytes.size());row.put("minimumRoundedDeclaration",((Math.max(allocated,retained)*5/4+8192+4095)/4096)*4096);rows.add(row);}
  System.out.println(JsonR27.write(rows));
 }
}
