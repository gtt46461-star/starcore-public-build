import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** HOST evidence only: no transformer, no probe substitution, no collector request, no owner mutation. */
public final class ReadonlyBudgetObserverR31 {
 static Object get(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 public static void agentmain(String output,Instrumentation instrumentation)throws Exception{
  Class<?> memory=null,json=null;for(Class<?> c:instrumentation.getAllLoadedClasses()){if(c.getName().equals("com.ethk3.novaonepro.strategyv2.MemoryBudgetR31"))memory=c;if(c.getName().equals("com.ethk3.novaonepro.strategyv2.JsonR27"))json=c;}
  if(memory==null||json==null)throw new IllegalStateException("R31_OBSERVER_TARGET_NOT_ALREADY_LOADED");
  final Object budget=memory.getMethod("processBudget").invoke(null);final Method snapshot=memory.getMethod("snapshot"),write=json.getMethod("write",Object.class);
  Thread observer=new Thread(()->{try{
   for(int n=0;n<180;n++){
    Map<String,Object> sample=new LinkedHashMap<>();sample.put("observedAt",java.time.Instant.now().toString());sample.put("sampleIndex",n);sample.put("observer","READ_ONLY_INSTRUMENTATION_NO_CLASS_TRANSFORM_NO_PROBE_GC_OR_OWNER_MUTATION");Runtime rt=Runtime.getRuntime();sample.put("actualJvmHeapUsedBytes",rt.totalMemory()-rt.freeMemory());sample.put("actualJvmMaxHeapBytes",rt.maxMemory());
    synchronized(budget){sample.put("productionBudgetSnapshot",snapshot.invoke(budget));List<Object> leases=new ArrayList<>();for(Object lease:(Collection<?>)get(budget,"leases")){Map<String,Object> m=new LinkedHashMap<>();for(String k:new String[]{"owner","kind","bytes","fence","closed","allocatedPayloadBytes","allocatedAtNanos","fixedPrivateBacking"})m.put(k,String.valueOf(get(lease,k)));leases.add(m);}sample.put("actualOwnerLeases",leases);}
    String encoded=(String)write.invoke(null,sample);Files.write(Paths.get(output),(encoded+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);Thread.sleep(2000);
   }
  }catch(Throwable failure){try{Files.write(Paths.get(output),("OBSERVER_FAILURE:"+failure+"\n").getBytes(StandardCharsets.UTF_8),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}},"r31-ci015-readonly-owner-observer");observer.setDaemon(true);observer.start();
 }
 public static void main(String[] args)throws Exception{com.sun.tools.attach.VirtualMachine vm=com.sun.tools.attach.VirtualMachine.attach(args[0]);try{vm.loadAgent(args[1],args[2]);}finally{vm.detach();}}
}
