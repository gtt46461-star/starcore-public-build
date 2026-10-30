package android.os;

/** HOST real JVM CPU time; Android ART-only counters remain explicitly unavailable. */
public final class Debug {
    private Debug(){}
    public static long threadCpuTimeNanos(){java.lang.management.ThreadMXBean bean=java.lang.management.ManagementFactory.getThreadMXBean();if(!bean.isCurrentThreadCpuTimeSupported())return -1L;if(!bean.isThreadCpuTimeEnabled())try{bean.setThreadCpuTimeEnabled(true);}catch(RuntimeException unavailable){return -1L;}return bean.getCurrentThreadCpuTime();}
    public static String getRuntimeStat(String stat){return null;}
}
