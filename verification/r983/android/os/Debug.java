package android.os;
/** Host metrics only. ART and Android native heap remain unavailable rather than fabricated. */
public final class Debug {
 private Debug(){}
 public static long threadCpuTimeNanos(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime();}
 public static long getNativeHeapAllocatedSize(){return -1L;}
 public static String getRuntimeStat(String key){return null;}
}
