package android.os;
public final class SystemClock {
    public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000;}
    public static long elapsedRealtime(){return System.nanoTime()/1000000;}
}
