package android.os;
/** Real host monotonic clock for bounded-duration performance/restart contracts.
 * Unlike call-count clocks, a checkpoint does not consume a fictitious millisecond. */
public final class SystemClock {
 public static long elapsedRealtime(){return System.nanoTime()/1_000_000L;}
 public static long uptimeMillis(){return elapsedRealtime();}
 public static long elapsedRealtimeNanos(){return System.nanoTime();}
 public static void sleep(long millis){try{Thread.sleep(millis);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
}
