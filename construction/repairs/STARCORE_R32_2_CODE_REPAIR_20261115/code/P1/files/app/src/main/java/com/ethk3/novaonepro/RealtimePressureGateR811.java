package com.ethk3.novaonepro;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Process-local realtime pressure signal. It never reads SQLite or owns scheduling. */
public final class RealtimePressureGateR811 {
    private static final AtomicInteger ANALYSIS_DEPTH = new AtomicInteger();
    private static final AtomicInteger SETTLEMENT_DEPTH = new AtomicInteger();
    private static final AtomicLong LAST_LIVE_PULSE_AT = new AtomicLong();

    private RealtimePressureGateR811() {}

    public static void beginAnalysis() {
        ANALYSIS_DEPTH.incrementAndGet();
    }

    public static void endAnalysis() {
        decrementFloorZero(ANALYSIS_DEPTH);
    }

    public static void beginSettlement() {
        SETTLEMENT_DEPTH.incrementAndGet();
    }

    public static void endSettlement() {
        decrementFloorZero(SETTLEMENT_DEPTH);
    }

    public static void pulseLiveRequest() {
        LAST_LIVE_PULSE_AT.set(TimeSourceR811.monoNowMs());
    }

    public static boolean isUnderPressure() {
        return isUnderPressureAt(TimeSourceR811.monoNowMs());
    }

    /** True only while a critical owner is executing, excluding the post-pulse quiet window. */
    public static boolean hasActiveCriticalWork() {
        return ANALYSIS_DEPTH.get() > 0 || SETTLEMENT_DEPTH.get() > 0;
    }

    static boolean isUnderPressureAt(long nowMonoMs) {
        if (ANALYSIS_DEPTH.get() > 0 || SETTLEMENT_DEPTH.get() > 0) return true;
        long pulse = LAST_LIVE_PULSE_AT.get();
        return pulse > 0L
                && Math.max(0L, nowMonoMs - pulse) < AppConfig.R811_LIVE_QUIET_MS;
    }

    public static Snapshot snapshot() {
        long now = TimeSourceR811.monoNowMs();
        long pulse = LAST_LIVE_PULSE_AT.get();
        return new Snapshot(
                ANALYSIS_DEPTH.get(),
                SETTLEMENT_DEPTH.get(),
                pulse,
                pulse <= 0L ? Long.MAX_VALUE : Math.max(0L, now - pulse),
                isUnderPressureAt(now));
    }

    static void resetForTests() {
        ANALYSIS_DEPTH.set(0);
        SETTLEMENT_DEPTH.set(0);
        LAST_LIVE_PULSE_AT.set(0L);
    }

    private static void decrementFloorZero(AtomicInteger value) {
        for (; ; ) {
            int current = value.get();
            if (current <= 0) return;
            if (value.compareAndSet(current, current - 1)) return;
        }
    }

    public static final class Snapshot {
        public final int analysisDepth, settlementDepth;
        public final long lastLivePulseMonoMs, quietForMs;
        public final boolean underPressure;

        Snapshot(int analysis, int settlement, long pulse, long quiet, boolean pressure) {
            analysisDepth = Math.max(0, analysis);
            settlementDepth = Math.max(0, settlement);
            lastLivePulseMonoMs = Math.max(0L, pulse);
            quietForMs = Math.max(0L, quiet);
            underPressure = pressure;
        }
    }

    private static final Object REPORT_PRESSURE_LOCK_CP4=new Object();
    private static java.io.File reportRootCP4;
    private static String sessionCP4="",ownerCP4="";
    private static long generationCP4,lastPublishedCP4;
    static void configureCrossProcess(android.content.Context context){
        if(context==null)throw new IllegalArgumentException("APPLICATION_CONTEXT_REQUIRED");
        reportRootCP4=new java.io.File(context.getFilesDir(),"r981_reports");
    }
    static void initializeCrossProcessOffMain(){
        synchronized(REPORT_PRESSURE_LOCK_CP4){
            ensureSessionCP4();
            // Finish recovery of any pre-R76 AtomicFile backup once, off the critical owner.
            // Subsequent pressure records are process-scoped, two-second advisory leases.
            if(reportRootCP4!=null){
                java.io.File pressure=new java.io.File(reportRootCP4,"realtime-pressure.json");
                if(pressure.exists()||new java.io.File(pressure.getPath()+".bak").exists()){
                    try(java.io.InputStream ignored=new android.util.AtomicFile(pressure).openRead()){}
                    catch(java.io.IOException ignored){}
                }
            }
        }
    }
    private static void ensureSessionCP4(){
        if(!sessionCP4.isEmpty()||reportRootCP4==null)return;
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())
            throw new IllegalStateException("CP4_PRESSURE_IO_MAIN");
        try{
            String created=java.util.UUID.randomUUID().toString();
            org.json.JSONObject j=new org.json.JSONObject();j.put("session",created);
            j.put("pid",android.os.Process.myPid());j.put("startElapsed",android.os.SystemClock.elapsedRealtime());
            atomicJsonCP4("runtime-session.json",j);sessionCP4=created;
        }catch(Exception e){android.util.Log.e("CP4Pressure","SESSION_WRITE_FAILED",e);}
    }
    static void beginCrossProcess(String owner,long generation){
        synchronized(REPORT_PRESSURE_LOCK_CP4){ensureSessionCP4();ownerCP4=owner;generationCP4=generation;publishCP4(true);}
    }
    static void renewCrossProcess(String owner,long generation){
        synchronized(REPORT_PRESSURE_LOCK_CP4){
            if(!ownerCP4.equals(owner)||generationCP4!=generation)return;
            if(android.os.SystemClock.elapsedRealtime()-lastPublishedCP4>=250L)publishCP4(true);
        }
    }
    static void endCrossProcess(String owner,long generation){
        synchronized(REPORT_PRESSURE_LOCK_CP4){
            if(!ownerCP4.equals(owner)||generationCP4!=generation)return;
            publishCP4(false);ownerCP4="";generationCP4=0;
        }
    }
    private static void publishCP4(boolean active){
        if(sessionCP4.isEmpty()||reportRootCP4==null)return;
        long now=android.os.SystemClock.elapsedRealtime();
        try{
            org.json.JSONObject j=new org.json.JSONObject();j.put("session",sessionCP4);
            j.put("leaseGeneration",generationCP4);j.put("owner",ownerCP4);
            j.put("elapsedRealtimeMs",now);j.put("validUntilElapsedMs",active?now+2000L:now);
            transientPressureJsonR76(j);lastPublishedCP4=now;
        }catch(Exception e){android.util.Log.e("CP4Pressure","PRESSURE_WRITE_FAILED",e);}
    }
    /** Atomic visibility without a disk durability barrier on each cancellation checkpoint.
     * This is a short-lived process/session hint, never a report journal, model receipt or
     * writer lease. The durable session plus elapsed expiry rejects stale reboot/process data.
     * A failed rename preserves the previous record and does not advance lastPublishedCP4. */
    private static void transientPressureJsonR76(org.json.JSONObject value)throws Exception{
        if(!reportRootCP4.isDirectory()&&!reportRootCP4.mkdirs())throw new java.io.IOException("CP4_PRESSURE_DIR");
        java.io.File target=new java.io.File(reportRootCP4,"realtime-pressure.json");
        java.io.File pending=new java.io.File(reportRootCP4,"realtime-pressure.r76-pending");
        boolean replaced=false;
        try{
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(pending,false)){
                out.write(value.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if(!pending.renameTo(target))throw new java.io.IOException("CP4_PRESSURE_RENAME_FAILED");
            replaced=true;
        }finally{
            if(!replaced&&pending.exists())pending.delete();
        }
    }
    private static void atomicJsonCP4(String name,org.json.JSONObject j)throws Exception{
        if(!reportRootCP4.isDirectory()&&!reportRootCP4.mkdirs())throw new java.io.IOException("CP4_PRESSURE_DIR");
        android.util.AtomicFile file=new android.util.AtomicFile(new java.io.File(reportRootCP4,name));
        java.io.FileOutputStream out=null;
        try{out=file.startWrite();out.write(j.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));file.finishWrite(out);}
        catch(Exception failure){if(out!=null)file.failWrite(out);throw failure;}
    }
}
