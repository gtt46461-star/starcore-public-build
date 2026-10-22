package android.os;
/** Real host monotonic time. Optional observer makes a service-owner publication at a
 * deterministic Activity read boundary; it never changes, advances or freezes time. */
public final class SystemClock {
 public static volatile Runnable beforeElapsedRead;
 public static long elapsedRealtime(){Runnable observer=beforeElapsedRead;if(observer!=null)observer.run();return System.nanoTime()/1_000_000L;}
 public static long uptimeMillis(){return elapsedRealtime();}
 public static long elapsedRealtimeNanos(){return System.nanoTime();}
 public static void sleep(long millis){try{Thread.sleep(millis);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
