package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AtomicFile;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.Permission;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.JSONObject;

/** Actual owner calls with Android preferences/file transport adapters. Durability sleeps
 * are injected observations, not claims about Android fsync latency or phone throughput. */
public final class runtime_observation {
    static int checks;static final List<String> names=new ArrayList<>();
    static final sun.misc.Unsafe U;
    static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;names.add(name);System.out.println("PASS|"+name);}
    static void set(Object o,Class<?> c,String n,Object v)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static Object get(Object o,Class<?> c,String n)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}

    /** Each edit buffers privately. apply updates memory immediately and queues a flush;
     * commit flushes all preceding queued edits in order and pays injected disk delay. */
    static final class Prefs {
        final Map<String,Object> memory=new LinkedHashMap<>(),disk=new LinkedHashMap<>();
        final ArrayList<Map<String,Object>> queued=new ArrayList<>();
        int commits,applies;long delayMs=20;
        final SharedPreferences proxy=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{
            String n=m.getName();if(n.equals("edit"))return editor();if(n.equals("getAll"))return new LinkedHashMap<>(memory);
            if(n.equals("contains"))return memory.containsKey(a[0]);if(n.startsWith("get"))return memory.getOrDefault(a[0],a[1]);
            if(n.startsWith("register")||n.startsWith("unregister"))return null;throw new UnsupportedOperationException(n);
        });
        SharedPreferences.Editor editor(){
            Map<String,Object> patch=new LinkedHashMap<>();boolean[] clear={false};
            return (SharedPreferences.Editor)Proxy.newProxyInstance(SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(p,m,a)->{
                String n=m.getName();if(n.startsWith("put")){patch.put((String)a[0],a[1]);return p;}
                if(n.equals("remove")){patch.put((String)a[0],null);return p;}if(n.equals("clear")){clear[0]=true;return p;}
                if(n.equals("apply")||n.equals("commit")){
                    if(clear[0])memory.clear();for(Map.Entry<String,Object> e:patch.entrySet())if(e.getValue()==null)memory.remove(e.getKey());else memory.put(e.getKey(),e.getValue());
                    queued.add(new LinkedHashMap<>(memory));
                    if(n.equals("apply")){applies++;return null;}commits++;if(delayMs>0)Thread.sleep(delayMs);flush();return true;
                }throw new UnsupportedOperationException(n);
            });
        }
        void flush(){for(Map<String,Object> snapshot:queued){disk.clear();disk.putAll(snapshot);}queued.clear();}
    }
    static final class HostContext extends android.app.Application {
        final File files;final Prefs pref=new Prefs();HostContext device;
        HostContext(Path path)throws Exception{files=Files.createDirectories(path).toFile();}
        @Override public Context getApplicationContext(){return this;}
        @Override public File getFilesDir(){return files;}
        @Override public File getCacheDir(){return files;}
        @Override public SharedPreferences getSharedPreferences(String n,int mode){return pref.proxy;}
        @Override public Context createDeviceProtectedStorageContext(){return device;}
    }
    interface Action{void run()throws Exception;}
    static void telemetry(HostContext c,String name,Action action,String key,Object expected)throws Exception{
        int commits=c.pref.commits+c.device.pref.commits,applies=c.pref.applies+c.device.pref.applies;
        long began=System.nanoTime();action.run();long us=(System.nanoTime()-began)/1000;
        System.out.println("OBSERVE|"+name+"|us="+us+"|syncCommits="+(c.pref.commits+c.device.pref.commits-commits));
        check(c.pref.commits+c.device.pref.commits==commits,"TELEMETRY_NO_SYNC_COMMIT_"+name);
        check(c.pref.applies+c.device.pref.applies==applies+2,"TELEMETRY_APPLY_BOTH_STORES_"+name);
        check(Objects.equals(c.pref.memory.get(key),expected)&&Objects.equals(c.device.pref.memory.get(key),expected),"TELEMETRY_IMMEDIATE_MEMORY_"+name);
    }
    static void durable(HostContext c,String name,Action action,String key,Object expected)throws Exception{
        int commits=c.pref.commits+c.device.pref.commits;action.run();
        check(c.pref.commits+c.device.pref.commits==commits+2,"CONTROL_DUAL_SYNC_COMMIT_"+name);
        check(Objects.equals(c.pref.disk.get(key),expected)&&Objects.equals(c.device.pref.disk.get(key),expected),"CONTROL_DURABLE_"+name);
    }
    static void heartbeat(Path dir)throws Exception{
        android.os.Build.VERSION.SDK_INT=34;HostContext c=new HostContext(dir.resolve("credential"));c.device=new HostContext(dir.resolve("device"));
        ServiceHeartbeatStore h=new ServiceHeartbeatStore(c);
        durable(c,"USER_STOP",()->h.setUserEnabled(false),"user_enabled",false);
        telemetry(c,"PENDING",()->h.setPendingSettlementCount(7),"pending_settlement_count",7);
        check(!c.pref.disk.containsKey("pending_settlement_count")&&!c.device.pref.disk.containsKey("pending_settlement_count"),"TELEMETRY_NOT_FALSELY_DURABLE_BEFORE_FLUSH");
        telemetry(c,"STAGE",()->h.markAnalysisRunning("202610130495"),"analysis_stage_issue","202610130495");
        telemetry(c,"FETCH_SUCCESS",()->h.markFetchSuccess("202610130494"),"last_fetch_issue","202610130494");
        telemetry(c,"ANALYSIS_SUCCESS",()->h.markAnalysisSuccess("202610130495"),"last_analysis_issue","202610130495");
        telemetry(c,"SETTLEMENT_SUCCESS",()->h.markSettlementSuccess("202610130494"),"last_settlement_issue","202610130494");
        telemetry(c,"BUSINESS_STAGE",()->h.markBusinessStage("OBSERVED","detail"),"business_stage","OBSERVED");
        RuntimeHealthLedgerR36.BusinessTruth t=new RuntimeHealthLedgerR36.BusinessTruth();t.status="FAIL";t.rootCause="SLA_NOT_MET";t.analysisP95Ms=26000L;
        telemetry(c,"BUSINESS_TRUTH",()->h.writeBusinessTruth(t,"WAITING","WAITING"),"business_analysis_p95_ms",26000L);
        telemetry(c,"CANONICAL_OBSERVATION",()->h.markAnalysisCanonicalCommitted("202610130495",12345L),"last_analysis_success_at",12345L);
        telemetry(c,"TERMINAL_OBSERVATION",()->h.markSettlementTerminal("202610130494","COMPLETED","detail",23456L),"last_settlement_success_at",23456L);
        telemetry(c,"TEACHER_SLICE",()->h.markTeacherSlice("COMPLETE",8,"cursor8",25L),"last_teacher_slice_processed",8);
        telemetry(c,"ANALYSIS_PROGRESS",()->h.markAnalysisProgress("RUNNING",37,"RANK","202610130495"),"analysis_progress_percent",37);
        telemetry(c,"LOOP_PROGRESS",()->h.markLearningPump("PROGRESS","numeric"),"learning_pump_state","PROGRESS");
        long progress=h.getLearningPumpAt();telemetry(c,"ATTEMPT_DISTINCT",()->h.markLearningAttempt("YIELDED","budget"),"learning_attempt_state","YIELDED");
        check(h.getLearningPumpAt()==progress,"ATTEMPT_DOES_NOT_ADVANCE_PROGRESS");
        check(!h.isUserEnabled()&&Boolean.FALSE.equals(c.pref.disk.get("user_enabled")),"OBSERVATIONS_DO_NOT_UNDO_USER_STOP");
        check("202610130494".equals(h.getLastFetchIssue())&&h.getLastAnalysisSuccessAt()==12345L&&h.getLastSettlementSuccessAt()==23456L,"OWNER_GETTERS_SEE_APPLY_IMMEDIATELY");
        c.pref.flush();c.device.pref.flush();check(c.pref.memory.equals(c.pref.disk)&&c.device.pref.memory.equals(c.device.pref.disk),"ORDERED_TELEMETRY_FLUSH_PRESERVES_LATEST_VALUES");
        h.markBusinessStage("QUEUED_BEFORE_START","test");
        durable(c,"UI_START",()->h.markUiStartRequested("test"),"user_enabled",true);
        check("QUEUED_BEFORE_START".equals(c.pref.disk.get("business_stage"))&&c.pref.queued.isEmpty()&&c.device.pref.queued.isEmpty(),"CONTROL_COMMIT_FLUSHES_PRIOR_APPLIES_IN_ORDER");
        c.pref.flush();c.device.pref.flush();check(h.isUserEnabled()&&Boolean.TRUE.equals(c.pref.disk.get("user_enabled")),"NO_LATE_TELEMETRY_REVERTS_COMMITTED_USER_PERMISSION");
        durable(c,"SERVICE_START",()->h.markServiceStarting("test"),"process_alive",1);
        durable(c,"STOP",()->h.markStop("test"),"runtime_lease_until",0L);
        int commits=c.pref.commits+c.device.pref.commits;
        check(h.tryAcquireRecoveryLease("test"),"RECOVERY_LEASE_ACQUIRED");
        check(c.pref.commits+c.device.pref.commits==commits+2&&(Long)c.pref.disk.get("recovery_lease_until")>System.currentTimeMillis(),"RECOVERY_LEASE_STILL_DURABLE");
        commits=c.pref.commits+c.device.pref.commits;check(!h.tryAcquireRecoveryLease("duplicate")&&c.pref.commits+c.device.pref.commits==commits,"RECOVERY_DUPLICATE_REJECTED_NO_WRITE");
        durable(c,"LEASE_RELEASE",()->h.releaseRecoveryLease("test"),"recovery_lease_until",0L);
        commits=c.pref.commits+c.device.pref.commits;h.renewRuntimeLease("test");
        check(c.pref.commits+c.device.pref.commits==commits+2&&h.isRuntimeLeaseFresh(),"RUNTIME_LEASE_REMAINS_DURABLE");
        commits=c.pref.commits+c.device.pref.commits;h.writeHeartbeat("test");
        check(c.pref.commits+c.device.pref.commits==commits+2&&Objects.equals(c.pref.memory.get("heartbeat_at"),c.pref.disk.get("heartbeat_at")),"LIVENESS_HEARTBEAT_REMAINS_DURABLE");
    }
    static JSONObject json(File f)throws Exception{return new JSONObject(Files.readString(f.toPath()));}
    static boolean reportContinues(HostContext context,File root)throws Exception{
        ReportExportServiceCP4 service=(ReportExportServiceCP4)U.allocateInstance(ReportExportServiceCP4.class);
        set(service,ReportExportServiceCP4.class,"destroyed",new AtomicBoolean());set(service,android.content.ContextWrapper.class,"mBase",context);
        String hash="a".repeat(64);ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(hash,"b".repeat(64),"c".repeat(64),"RETENTION_PAGE",System.currentTimeMillis());
        Class<?> cl=Class.forName("com.ethk3.novaonepro.ReportExportServiceCP4$ActiveControl");
        Constructor<?> ctor=cl.getDeclaredConstructor(ReportExportServiceCP4.class,ReportJobJournalCP12.Job.class,File.class);ctor.setAccessible(true);
        Object control=ctor.newInstance(service,job,root);Method run=cl.getDeclaredMethod("shouldContinue");run.setAccessible(true);
        boolean result=(Boolean)run.invoke(control);System.out.println("REPORT_CONTROL|continue="+result+"|reason="+get(control,cl,"stopReason"));return result;
    }
    static void pressure(Path dir)throws Exception{
        HostContext c=new HostContext(dir);RealtimePressureGateR811.configureCrossProcess(c);
        for(String f:new String[]{"sessionCP4","ownerCP4"})set(null,RealtimePressureGateR811.class,f,"");
        set(null,RealtimePressureGateR811.class,"generationCP4",0L);set(null,RealtimePressureGateR811.class,"lastPublishedCP4",0L);
        File root=new File(c.files,"r981_reports");root.mkdirs();File pressure=new File(root,"realtime-pressure.json"),session=new File(root,"runtime-session.json");
        Files.writeString(pressure.toPath(),"uncommitted-old-current");Files.writeString(Path.of(pressure+".bak"),"legacy-backup");
        AtomicFile.reset();RealtimePressureGateR811.initializeCrossProcessOffMain();
        check(AtomicFile.finishes==1&&AtomicFile.durableNames.equals(List.of("runtime-session.json")),"SESSION_USES_DURABLE_ATOMIC_FILE");
        boolean recovered=AtomicFile.recoveries==1&&Files.readString(pressure.toPath()).equals("legacy-backup")&&!Files.exists(Path.of(pressure+".bak"));
        int durable=AtomicFile.finishes;long start=System.nanoTime();RealtimePressureGateR811.beginCrossProcess("analysis-owner",7L);
        System.out.println("OBSERVE|PRESSURE_BEGIN|us="+(System.nanoTime()-start)/1000+"|atomicFinishes="+(AtomicFile.finishes-durable));
        check(AtomicFile.finishes==durable,"PRESSURE_BEGIN_HAS_NO_ATOMIC_FSYNC");
        check(recovered,"LEGACY_BACKUP_RECOVERED_BEFORE_FIRST_NEW_PRESSURE");
        JSONObject p=json(pressure),s=json(session);check(p.getString("session").equals(s.getString("session"))&&p.getLong("leaseGeneration")==7L&&p.getString("owner").equals("analysis-owner"),"PRESSURE_PRESERVES_OWNER_SESSION_GENERATION");
        check(p.getLong("validUntilElapsedMs")-p.getLong("elapsedRealtimeMs")==2000L,"PRESSURE_EXPIRY_EXACT_TWO_SECONDS");
        check(!reportContinues(c,root),"ACTUAL_REPORT_CONSUMER_PREEMPTS_FOR_VALID_PRESSURE");
        byte[] before=Files.readAllBytes(pressure.toPath());long published=(Long)get(null,RealtimePressureGateR811.class,"lastPublishedCP4");
        RealtimePressureGateR811.renewCrossProcess("wrong",7L);RealtimePressureGateR811.endCrossProcess("analysis-owner",6L);
        check(Arrays.equals(before,Files.readAllBytes(pressure.toPath()))&&(Long)get(null,RealtimePressureGateR811.class,"lastPublishedCP4")==published,"WRONG_OWNER_AND_GENERATION_CANNOT_CHANGE_LEASE");
        RealtimePressureGateR811.renewCrossProcess("analysis-owner",7L);check(Arrays.equals(before,Files.readAllBytes(pressure.toPath())),"RENEW_THROTTLED_WITHIN_250_MS");
        set(null,RealtimePressureGateR811.class,"lastPublishedCP4",0L);RealtimePressureGateR811.renewCrossProcess("analysis-owner",7L);
        check(AtomicFile.finishes==durable&&json(pressure).getLong("leaseGeneration")==7L,"PRESSURE_RENEW_HAS_NO_FSYNC");
        before=Files.readAllBytes(pressure.toPath());set(null,RealtimePressureGateR811.class,"lastPublishedCP4",0L);
        File pending=new File(root,"realtime-pressure.r76-pending");pending.mkdir();Files.writeString(new File(pending,"block").toPath(),"block");
        RealtimePressureGateR811.renewCrossProcess("analysis-owner",7L);
        check(Arrays.equals(before,Files.readAllBytes(pressure.toPath()))&&(Long)get(null,RealtimePressureGateR811.class,"lastPublishedCP4")==0L,"FAILED_PRESSURE_WRITE_PRESERVES_OLD_RECORD_AND_RETRY_CLOCK");
        Files.delete(new File(pending,"block").toPath());Files.delete(pending.toPath());
        final String denied=pressure.getAbsolutePath();
        SecurityManager previous=System.getSecurityManager();
        try{System.setSecurityManager(new SecurityManager(){@Override public void checkPermission(Permission p){}@Override public void checkWrite(String file){if(new File(file).getAbsolutePath().equals(denied))throw new SecurityException("INJECTED_RENAME_DESTINATION_DENIED");}});
            RealtimePressureGateR811.renewCrossProcess("analysis-owner",7L);
        }finally{System.setSecurityManager(previous);}
        check(Arrays.equals(before,Files.readAllBytes(pressure.toPath()))&&!pending.exists()&&(Long)get(null,RealtimePressureGateR811.class,"lastPublishedCP4")==0L,"FAILED_ATOMIC_RENAME_PRESERVES_OLD_RECORD_AND_CLEANS_TEMP");
        RealtimePressureGateR811.renewCrossProcess("analysis-owner",7L);check((Long)get(null,RealtimePressureGateR811.class,"lastPublishedCP4")>0L,"PRESSURE_RECOVERS_AFTER_TRANSIENT_FAILURE");
        RealtimePressureGateR811.endCrossProcess("analysis-owner",7L);p=json(pressure);
        check(p.getLong("validUntilElapsedMs")==p.getLong("elapsedRealtimeMs")&&reportContinues(c,root),"ACTUAL_REPORT_CONSUMER_ACCEPTS_EXPIRED_END_RECORD");
        check(AtomicFile.finishes==durable,"ALL_PRESSURE_PUBLICATIONS_AVOID_DURABILITY_BARRIER");
        long now=android.os.SystemClock.elapsedRealtime();
        JSONObject valid=json(pressure).put("elapsedRealtimeMs",now).put("validUntilElapsedMs",now+2000L);
        Files.writeString(pressure.toPath(),valid.put("leaseGeneration",0L).toString());check(reportContinues(c,root),"ACTUAL_CONSUMER_REJECTS_ZERO_GENERATION");
        Files.writeString(pressure.toPath(),valid.put("leaseGeneration",7L).put("session","old-process").toString());check(reportContinues(c,root),"ACTUAL_CONSUMER_REJECTS_OLD_PROCESS_SESSION");
        Files.writeString(pressure.toPath(),valid.put("session",s.getString("session")).put("validUntilElapsedMs",now-1L).toString());check(reportContinues(c,root),"ACTUAL_CONSUMER_REJECTS_EXPIRED_RECORD");
        Files.writeString(pressure.toPath(),valid.put("elapsedRealtimeMs",now+10000L).put("validUntilElapsedMs",now+11000L).toString());check(reportContinues(c,root),"ACTUAL_CONSUMER_REJECTS_FUTURE_MONOTONIC_SAMPLE");
        Files.writeString(pressure.toPath(),valid.put("elapsedRealtimeMs",now).put("validUntilElapsedMs",now+3000L).toString());check(reportContinues(c,root),"ACTUAL_CONSUMER_REJECTS_OVERSIZED_HINT");
        Files.writeString(pressure.toPath(),"broken-json");check(!reportContinues(c,root),"ACTUAL_CONSUMER_READ_ERROR_IS_NOT_SUCCESS");
    }
    public static void main(String[] args)throws Exception{
        Path dir=Path.of(args[1]);Files.createDirectories(dir);
        for(Class<?> c:new Class[]{ServiceHeartbeatStore.class,RealtimePressureGateR811.class,ReportExportServiceCP4.class})System.out.println("ORIGIN|"+c.getName()+"|"+c.getProtectionDomain().getCodeSource().getLocation());
        if(!args[0].equals("pressure"))heartbeat(dir.resolve("heartbeat"));
        if(!args[0].equals("heartbeat"))pressure(dir.resolve("pressure"));
        JSONObject result=new JSONObject().put("status","PASS").put("checks",checks).put("case",args[0]).put("scope","ACTUAL_OWNER_HOST_IO_BOUNDARIES_NO_PHONE_TIMING").put("assertions",new org.json.JSONArray(names));
        Files.writeString(Path.of(args[2]),result.toString(2)+"\n");System.out.println(result);
    }
}
