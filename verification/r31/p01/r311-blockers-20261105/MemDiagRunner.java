import org.junit.runner.*;import org.junit.runner.notification.*;import java.util.*;
/** Runs test classes sequentially in ONE JVM (Gradle semantics) and records post-GC live heap and budget leases per class. */
public class MemDiagRunner{
  public static void main(String[] a)throws Exception{
    Object budget=Class.forName("com.ethk3.novaonepro.strategyv2.MemoryBudgetR31").getMethod("processBudget").invoke(null);
    java.lang.reflect.Method snap=budget.getClass().getMethod("snapshot");
    for(String name:a){
      Result r=new JUnitCore().run(Class.forName(name));
      long rawUsed=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
      for(int i=0;i<3;i++){System.gc();Thread.sleep(50);}
      long live=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
      Map<?,?> s=(Map<?,?>)snap.invoke(budget);
      System.out.println("MEMDIAG|"+name.substring(name.lastIndexOf('.')+1)+"|run="+r.getRunCount()+"|fail="+r.getFailureCount()+"|rawUsedMB="+rawUsed/1048576+"|liveAfterGcMB="+live/1048576+"|leases="+s.get("activeLeaseCount")+"|declaredJava="+s.get("declaredJavaWorkingBytes")+"|owners="+String.valueOf(s.get("largestOwners")).substring(0,Math.min(300,String.valueOf(s.get("largestOwners")).length())));
      for(Failure f:r.getFailures())System.out.println("MEMDIAG_FAIL|"+f.getTestHeader()+"|"+String.valueOf(f.getMessage()).substring(0,Math.min(160,String.valueOf(f.getMessage()).length())));
    }
  }
}
