package android.os;
/** Actual monotonic host time; not a simulated recovery implementation. */
public final class SystemClock {
    public static long elapsedRealtime(){return System.nanoTime()/1000000L;}
}
