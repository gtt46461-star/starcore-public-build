package android.os;
/** Deterministic SQL-cost clock; optional real monotonic mode. Test adapter, never product. */
public final class SystemClock {
 private static long now=1000000L,step=0L;
 private static boolean real=false;
 private static int checkpoints;private static Runnable hook;
 public static synchronized void interruptDevelopmentAt(int count,Runnable action){checkpoints=count;hook=action;}
 public static synchronized long elapsedRealtime(){
 if(hook!=null){StackTraceElement[] t=Thread.currentThread().getStackTrace();for(int i=0;i+1<t.length;i++)
  if(t[i].getClassName().endsWith("ResearchWorkBudgetR927")&&t[i].getMethodName().equals("checkpoint")
   &&t[i+1].getClassName().endsWith("AlgorithmInventionLabR908")&&t[i+1].getMethodName().equals("trainDevelopmentStep")){
    if(--checkpoints==0){Runnable h=hook;hook=null;h.run();}break;}}
 if(real)return System.nanoTime()/1000000L;long n=now;now+=step;return n;}
 public static synchronized long uptimeMillis(){return elapsedRealtime();}
 public static synchronized long elapsedRealtimeNanos(){return elapsedRealtime()*1000000L;}
 public static synchronized void setForTest(long n,long s){real=false;now=n;step=s;hook=null;}
 public static synchronized void charge(long ms){if(!real)now+=ms;}
 public static synchronized void useReal(){real=true;}
 public static void sleep(long millis){charge(millis);}
}
