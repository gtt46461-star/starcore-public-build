package android.os;
/** Explicit test-only Android clock adapter, not a product class. */
public final class SystemClock {
 private static long now=1000000L,step=0;private static int remaining;private static Runnable hook;
 public static synchronized void armDirectDevelopmentCheckpoint(int at,Runnable action){remaining=at;hook=action;}
 public static synchronized long elapsedRealtime(){
  if(hook!=null){StackTraceElement[] trace=Thread.currentThread().getStackTrace();for(int i=0;i+1<trace.length;i++)
   if(trace[i].getClassName().endsWith("ResearchWorkBudgetR927")&&trace[i].getMethodName().equals("checkpoint")
    &&trace[i+1].getClassName().endsWith("ResearchDynamicBootstrapR922")&&trace[i+1].getMethodName().equals("trainSlice")){
     if(--remaining==0){Runnable r=hook;hook=null;r.run();}break;}}
  if(step<0)return System.nanoTime()/1000000L;long n=now;now+=step;return n;
 }
 public static synchronized void advanceForTest(long delta){now+=delta;}
 public static synchronized long uptimeMillis(){return elapsedRealtime();}public static long elapsedRealtimeNanos(){return elapsedRealtime()*1000000L;}
 public static synchronized void setForTest(long n,long s){now=n;step=s;hook=null;}public static synchronized void sleep(long ms){now+=ms;}
}
