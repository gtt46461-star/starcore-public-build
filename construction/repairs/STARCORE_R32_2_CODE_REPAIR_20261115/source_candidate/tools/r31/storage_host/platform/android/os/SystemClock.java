package android.os;
/** Host monotonic time only. */
public final class SystemClock {
 public static long elapsedRealtime(){return System.nanoTime()/1000000L;}
 public static long elapsedRealtimeNanos(){return System.nanoTime();}
 public static long uptimeMillis(){return elapsedRealtime();}
}
