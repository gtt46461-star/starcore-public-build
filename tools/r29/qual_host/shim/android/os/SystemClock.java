package android.os;
/** HOST monotonic and thread CPU clocks; never included in the APK. */
public final class SystemClock {
    private SystemClock(){}
    public static long elapsedRealtime(){return System.nanoTime()/1_000_000L;}
    /** Real JVM current-thread CPU time. A JVM without CPU accounting uses HOST monotonic milliseconds;
     * that fallback is not an Android CPU measurement and is never packaged into the APK. */
    public static long currentThreadTimeMillis(){
        try{
            java.lang.management.ThreadMXBean bean=java.lang.management.ManagementFactory.getThreadMXBean();
            if(bean.isCurrentThreadCpuTimeSupported()){
                if(!bean.isThreadCpuTimeEnabled())bean.setThreadCpuTimeEnabled(true);
                long nanos=bean.getCurrentThreadCpuTime();if(nanos>=0)return nanos/1_000_000L;
            }
        }catch(RuntimeException|LinkageError unavailable){}
        return System.nanoTime()/1_000_000L;
    }
}
