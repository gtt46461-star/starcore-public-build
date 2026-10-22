package android.os;
/** Host-only elapsed clock. The nonzero offset detects cross-domain subtraction regressions. */
public final class SystemClock {
    private static long ticks=-1;
    public static void startTicksForTests(){ticks=System.nanoTime()/1000000L+78516000L;}
    public static void endTicksForTests(){ticks=-1;}
    public static long elapsedRealtime(){return ticks>=0?ticks++:System.nanoTime()/1000000L+78516000L;}
    public static long uptimeMillis(){return System.nanoTime()/1000000L;}
}
