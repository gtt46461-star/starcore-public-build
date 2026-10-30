package android.os;
/** Host monotonic clock only; never included in the APK. */
public final class SystemClock {
    private SystemClock(){}
    public static long elapsedRealtime(){return System.nanoTime()/1_000_000L;}
}
