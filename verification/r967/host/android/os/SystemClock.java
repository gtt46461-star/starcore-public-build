package android.os;
public final class SystemClock {
 private static long now=1000000L,step=1L;
 public static synchronized long elapsedRealtime(){long n=now;now+=step;return n;}
 public static synchronized long uptimeMillis(){return elapsedRealtime();}
 public static synchronized long elapsedRealtimeNanos(){return elapsedRealtime()*1000000L;}
 public static synchronized void setForTest(long n,long s){now=n;step=s;}
 public static void sleep(long millis){synchronized(SystemClock.class){now+=millis;}}
}