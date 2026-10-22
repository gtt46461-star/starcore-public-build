package android.os;
/** Host-only memory adapter: JVM managed heap is measured; Android native heap is not available. */
public final class Debug {private Debug(){} public static long getNativeHeapAllocatedSize(){return 0;}public static String getRuntimeStat(String key){return "0";}}
