package android.os;

/** Host transport only: monotonic JVM clock, never an Android/device timing claim. */
public final class SystemClock {
    private SystemClock() {}
    public static long elapsedRealtime() { return System.nanoTime() / 1_000_000L; }
    public static long elapsedRealtimeNanos() { return System.nanoTime(); }
    public static long uptimeMillis() { return elapsedRealtime(); }
}
