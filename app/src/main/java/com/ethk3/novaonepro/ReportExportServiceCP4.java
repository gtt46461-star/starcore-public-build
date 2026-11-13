package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.JSONObject;import org.json.JSONArray;

/** Sole diagnostics-process export owner. Requests are durable before a KICK; no business DB is opened writable. */
public final class ReportExportServiceCP4 extends Service {
    public static final String ACTION_EXPORT="com.ethk3.novaonepro.REPORT_EXPORT_CP4",ACTION_RESUME="com.ethk3.novaonepro.REPORT_RESUME_CP4",ACTION_CANCEL="com.ethk3.novaonepro.REPORT_CANCEL_CP4";
    public enum ExportMode { FULL_REPORT }
    private static final String CHANNEL="starcore_report_cp4";
    private static final int NOTIFICATION=934;
    private static final long MiB=1024L*1024L,NATIVE_BUDGET=4*MiB,SQLITE_BACKUP_BUDGET=32*MiB;
    private static final ThreadPoolExecutor REQUESTS=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(2),r->{Thread t=new Thread(r,"report-request-cp12");t.setDaemon(true);return t;});
    private static final AtomicLong USER_REQUEST_GENERATION=new AtomicLong();
    private ThreadPoolExecutor worker;private ScheduledExecutorService controlTimer;
    private final AtomicBoolean drainQueued=new AtomicBoolean(),destroyed=new AtomicBoolean();
    private volatile ActiveControl control;private volatile Future<?> workerFuture;private volatile int lastStartId;
    private final Handler main=new Handler(Looper.getMainLooper());

    /** Metadata scope only. The enclosing ReportInputsLease remains the actual CAS file/GC pin. */
    static final class CheckpointSourceScopeR31 implements AutoCloseable {
        private final com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26.DatabaseRootLeaseR31 mapping;
        final String state;final long observedAt;
        CheckpointSourceScopeR31(com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26.DatabaseRootLeaseR31 mapping,String state){this.mapping=mapping;this.state=state;observedAt=System.currentTimeMillis();}
        Map<String,Object> observation(){Map<String,Object> value=new LinkedHashMap<>();value.put("state",state);value.put("observedAt",observedAt);value.put("scope","EXPLICIT_LIVE_FILES_ROOT_UNDER_REPORT_INPUT_PIN; OUTSIDE_SQL_WATERMARK; OBJECT_VALIDATION_SEPARATE");value.put("checkpointObjectsVerified",null);return value;}
        @Override public void close(){if(mapping!=null)mapping.close();}
    }
    /** Caller supplies the same filesDir used by its enclosing FULL_REPORT input-pin resource. */
    static CheckpointSourceScopeR31 openCheckpointSourceR31(SQLiteDatabase snapshot,File pinnedFilesDir)throws IOException {
        if(!ReportInputsLeaseCP12.currentThreadHoldsInput(pinnedFilesDir))throw new IOException("REPORT_CHECKPOINT_INPUT_PIN_REQUIRED");
        // Android API29 can supply /data/user/0 while the trusted application
        // root resolves through /data/data. Normalize the already pinned root,
        // then retain the strict no-symlink check on the actual CAS child.
        File checkpointDirectory=new File(pinnedFilesDir.getCanonicalFile(),"r26-checkpoints");
        if(checkpointDirectory.exists()&&!checkpointDirectory.getCanonicalFile().equals(checkpointDirectory.getAbsoluteFile()))throw new IOException("REPORT_CHECKPOINT_SOURCE_SYMLINK");
        // Inline checkpoints can still be reported. A real reference without its source stays typed DEPENDENCY.
        if(!checkpointDirectory.isDirectory())return new CheckpointSourceScopeR31(null,"DEPENDENCY_CAS_SOURCE_DIRECTORY_UNAVAILABLE");
        return new CheckpointSourceScopeR31(com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26.openDatabaseRootR31(snapshot,checkpointDirectory),"SCOPED_TO_PINNED_CAS_ROOT_OBJECTS_UNVERIFIED");
    }
    /** The snapshot path never determines a live keep directory. The enclosing
     * report input lease excludes maintenance; this explicit mapping expires
     * when the immutable snapshot reader closes. */
    static com.ethk3.novaonepro.strategyv2.KeepMigrationStoreR31.ReaderRootLease openKeepSourceR31(SQLiteDatabase snapshot,File pinnedFilesDir)throws IOException {
        if(!ReportInputsLeaseCP12.currentThreadHoldsInput(pinnedFilesDir))throw new IOException("REPORT_KEEP_INPUT_PIN_REQUIRED");
        File directory=new File(pinnedFilesDir.getCanonicalFile(),"r31-keep");
        if(!directory.isDirectory())return null;
        if(!directory.getCanonicalFile().equals(directory.getAbsoluteFile()))throw new IOException("REPORT_KEEP_SOURCE_SYMLINK");
        return com.ethk3.novaonepro.strategyv2.KeepMigrationStoreR31.openReaderRoot(snapshot,directory);
    }

    /** May be called on UI. Heavy observation/file work stays on the request executor; no Activity is captured. */
    public static String requestExport(Context context,ExportMode mode) {
        return requestExport(context,mode,false);
    }
    public static String requestExport(Context context,ExportMode mode,boolean includeRawEvidence) {
        if(mode!=ExportMode.FULL_REPORT)throw new IllegalArgumentException("REPORT_MODE");
        final Context app=context.getApplicationContext();final String id=id("FULL_REPORT");final long generation=USER_REQUEST_GENERATION.get();
        try{REQUESTS.execute(()->{boolean durable=false;try{
            JSONObject request=new JSONObject();request.put("revision","REPORT_REQUEST_CP12_1");request.put("requestId",id);request.put("mode","FULL_REPORT");
            request.put("releaseHash",ReleaseManifestR926.MANIFEST_SHA256);request.put("createdWallMs",System.currentTimeMillis());
            request.put("includeRawEvidence",includeRawEvidence);
            File root=requestRoot(app);
            request.put("submissionState","PERSISTED_WAITING_FOR_SERVICE");request.put("observationState","UNKNOWN_NOT_COLLECTED");
            // This envelope is consumable before any optional observer runs.
            // Only process-local atomic schema state is sampled here: no SQL,
            // filesystem observer or business-writer acquisition is permitted.
            JSONObject initialObservation=new JSONObject().put("observationState","UNKNOWN_NOT_COLLECTED")
                    .put("sampleWallMs",System.currentTimeMillis()).put("sampleElapsedMs",SystemClock.elapsedRealtime())
                    .put("pid",android.os.Process.myPid()).put("process",app.getPackageName());
            DatabaseCoordinator initialCoordinator=DatabaseCoordinator.peek();
            if(initialCoordinator!=null){initialObservation.put("researchSchemaReady",initialCoordinator.isR811ResearchSchemaReady());initialObservation.put("schemaStartupSummary",initialCoordinator.startupSchemaSummary());}
            request.put("observation",initialObservation);
            synchronized(USER_REQUEST_GENERATION){
                if(generation!=USER_REQUEST_GENERATION.get())throw new IOException("REPORT_SUPERSEDED_BY_USER");
                ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),request.toString());durable=true;
            }
            // Request publication is the durable intent. Queue/kick can be reconstructed
            // after process death; optional observations can never erase that intent.
            try{enqueueCommittedRequestR31(root,id,"FULL_REPORT",generation);kick(app,ACTION_EXPORT,id);
                lastRequestOutcomeR31Q="KICKED|request="+id+"|at="+System.currentTimeMillis();
            }catch(Exception wait){lastRequestOutcomeR31Q="PERSISTED_WAITING_FOR_SERVICE|request="+id+"|"+describeR31Q(wait);}
            JSONObject observation=new JSONObject();observation.put("pid",android.os.Process.myPid());observation.put("sampleWallMs",System.currentTimeMillis());observation.put("sampleElapsedMs",SystemClock.elapsedRealtime());
            ReportJobJournalCP12.collectObservation(root,id,observation,"frameSummary",()->FrameMetricsR906.latestSummary());observation.put("process",app.getPackageName());
            // Filesystem observations are sampled before backup and remain distinct from
            // immutable snapshot page/size evidence. A missing source is not zero bytes.
            File liveDatabase=app.getDatabasePath(AppConfig.DB_NAME);
            observation.put("storageSampleWallMs",System.currentTimeMillis());
            ReportJobJournalCP12.collectObservation(root,id,observation,"storageMaintenanceSummary",()->StorageMaintenanceServiceR82.summary(app));
            observation.put("liveDatabaseFilePresent",liveDatabase.isFile());
            if(liveDatabase.isFile()){
                observation.put("liveDatabaseFileBytes",liveDatabase.length());
                File walFile=new File(liveDatabase.getPath()+"-wal");
                observation.put("liveWalFilePresent",walFile.isFile());
                observation.put("liveWalFileBytes",walFile.isFile()?walFile.length():0L);
            }

            ReportJobJournalCP12.collectObservation(root,id,observation,"crashSummary",()->CrashRecorder.detailedSummary(app));
            ServiceHeartbeatStore heartbeat=new ServiceHeartbeatStore(app);
            observation.put("servicePrefsReadStartedWallMs",System.currentTimeMillis()).put("servicePrefsReadStartedElapsedMs",SystemClock.elapsedRealtime()).put("servicePrefsObserverPid",android.os.Process.myPid());
            ReportJobJournalCP12.collectObservation(root,id,observation,"heartbeatSummary",heartbeat::detailedSummary);
            ReportJobJournalCP12.collectObservation(root,id,observation,"fetchCadenceR31",heartbeat::fetchCadenceJsonR31);
            observation.put("servicePrefsReadFinishedWallMs",System.currentTimeMillis()).put("servicePrefsReadFinishedElapsedMs",SystemClock.elapsedRealtime())
                    .put("servicePrefsEvidenceScope","PROCESS_LOCAL_SHARED_PREFERENCES_OBSERVATION; CROSS_PID_VISIBILITY_NOT_ATOMIC; NOT_SQL_SNAPSHOT_WATERMARK");
            ReportJobJournalCP12.collectObservation(root,id,observation,"backgroundRuntimeCapabilityR31",()->backgroundRuntimeObservationR31(app));
            DatabaseCoordinator coordinator=DatabaseCoordinator.peek();if(coordinator!=null){observation.put("dbWaitMs",coordinator.lastDbLockWaitMs());observation.put("dbWaitMaxMs",coordinator.longestDbLockWaitMs());observation.put("researchSchemaReady",coordinator.isR811ResearchSchemaReady());ReportJobJournalCP12.collectObservation(root,id,observation,"schemaStartupSummary",()->coordinator.startupSchemaSummary());}observation.put("realtimePressure",RealtimePressureGateR811.isUnderPressure());ReportJobJournalCP12.collectObservation(root,id,observation,"fetchWriteError",()->FetchLedgerR925.reportWriteErrorObservationCP12());
            // R27.3: where every app thread is right now (a stalled business loop names its blocking frame).
            ReportJobJournalCP12.collectObservation(root,id,observation,"threadStacks",ReportExportServiceCP4::threadStacksR27);
            ReportJobJournalCP12.collectObservation(root,id,observation,"ftrlWritePhases",()->com.ethk3.novaonepro.strategyv2.JsonR27.write(FtrlWriteTraceR30.snapshot()));
            observation.put("timelineCommitObservations",new JSONObject(com.ethk3.novaonepro.strategyv2.JsonR27.write(com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.commitObservationState())));
            RealtimePressureGateR811.Snapshot pressure=RealtimePressureGateR811.snapshot();
            observation.put("analysisDepth",pressure.analysisDepth).put("settlementDepth",pressure.settlementDepth)
                    .put("quietForMs",pressure.quietForMs).put("settlementExecutionActive",SettlementExecutionGateR819.isActive());
            request.put("observation",observation);
            observation.put("observationState","COLLECTED_OUTSIDE_SQL_WATERMARK");
            request.put("observationState","COLLECTED_OUTSIDE_SQL_WATERMARK");
            synchronized(USER_REQUEST_GENERATION){
                if(generation!=USER_REQUEST_GENERATION.get())throw new IOException("REPORT_SUPERSEDED_BY_USER");
                ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),request.toString());
            }
        }catch(Throwable error){
            if(!durable)requestFailure(app,id,error);
            else {lastRequestOutcomeR31Q="PERSISTED_OBSERVATION_OR_KICK_PENDING|request="+id+"|"+describeR31Q(error);android.util.Log.w("ReportExportCP12","Optional request observation unavailable; durable export retained",error);}
        }});}catch(RejectedExecutionException full){requestFailure(app,id,new IOException("REPORT_REQUEST_QUEUE_FULL"));}
        return id;
    }
    private static String backgroundRuntimeObservationR31(Context app)throws Exception{
        long startedWall=System.currentTimeMillis(),startedElapsed=SystemClock.elapsedRealtime();
        BackgroundRuntimeCapabilityR811.Snapshot value=BackgroundRuntimeCapabilityR811.snapshot(app);
        return new JSONObject().put("status",value.status).put("fingerprint",value.certificateFingerprint())
                .put("compileSdk",value.compileSdk).put("targetSdk",value.targetSdk).put("runtimeSdk",value.runtimeSdk)
                .put("ignoringBatteryOptimizations",value.ignoringBatteryOptimizations).put("exactAlarmRuntimeGranted",value.exactAlarmRuntimeGranted)
                .put("declaredPermissions",value.declaredPermissions).put("grantedPermissions",value.grantedPermissions)
                .put("observerPid",android.os.Process.myPid()).put("readStartedWallMs",startedWall).put("readStartedElapsedMs",startedElapsed)
                .put("readFinishedWallMs",System.currentTimeMillis()).put("readFinishedElapsedMs",SystemClock.elapsedRealtime())
                .put("runtimeOperatingState",new JSONObject(com.ethk3.novaonepro.strategyv2.JsonR27.write(runtimeOperatingStateR31(app)))).put("evidenceScope","RUNTIME_OS_OBSERVATION; NOT_BACKGROUND_CONTINUITY_CERTIFICATE; NOT_SQL_WATERMARK").toString();
    }
    private static java.util.Map<String,Object> runtimeOperatingStateR31(Context app){
        BackgroundRuntimeCapabilityR811.RuntimeObservationR31 value=BackgroundRuntimeCapabilityR811.observeRuntimeR31(app);
        java.util.Map<String,Object> out=new java.util.LinkedHashMap<>();out.put("status",value.status);out.put("reason",value.reason);out.put("recoveryCondition",value.recoveryCondition);out.put("snapshotId",value.snapshotId);
        out.put("sampleWallMs",value.sampleWallMs);out.put("sampleElapsedMs",value.sampleElapsedMs);out.put("observerPid",android.os.Process.myPid());out.put("runtimeSdk",value.runtimeSdk);out.put("targetSdk",value.targetSdk);out.put("userEnabled",value.userEnabled);
        out.put("batteryExemption",value.batteryExemption.name());out.put("deviceIdle",value.deviceIdle.name());out.put("powerSave",value.powerSave.name());out.put("backgroundRestricted",value.backgroundRestricted.name());out.put("foregroundService",value.foregroundService.name());out.put("notifications",value.notifications.name());out.put("exactAlarm",value.exactAlarm.name());out.put("errors",value.errors);out.put("continuous24h","NOT_RUN");out.put("continuous72h","NOT_RUN");return out;
    }
    /** R31Q: request-thread state for the quick diagnostic (a blocked request never reaches the service). */
    static String requestQueueStateR31Q(){return "active="+REQUESTS.getActiveCount()+"|queued="+REQUESTS.getQueue().size()+"|completed="+REQUESTS.getCompletedTaskCount()+"|taskCount="+REQUESTS.getTaskCount();}
    static String threadStacksR27() {
        StringBuilder b = new StringBuilder();
        java.util.List<java.util.Map.Entry<Thread, StackTraceElement[]>> all = new java.util.ArrayList<>(Thread.getAllStackTraces().entrySet());
        java.util.Collections.sort(all, (x, y) -> x.getKey().getName().compareTo(y.getKey().getName()));
        for (java.util.Map.Entry<Thread, StackTraceElement[]> e : all) {
            Thread t = e.getKey(); StackTraceElement[] st = e.getValue();
            b.append("THREAD|").append(t.getName()).append('|').append(t.getState()).append("|prio=").append(t.getPriority()).append('\n');
            for (int i = 0; i < st.length && i < 30; i++) b.append("  at ").append(st[i]).append('\n');
            if (b.length() > 200000) { b.append("TRUNCATED\n"); break; }
        }
        return b.toString();
    }
    public static String requestRetentionPage(Context context,File immutableRequest)throws IOException {
        try {
        Context app=context.getApplicationContext();File allowed=new File(app.getFilesDir(),"retention-requests-cp12").getCanonicalFile();
        File input=immutableRequest.getCanonicalFile();if(!input.equals(immutableRequest.getAbsoluteFile())||!input.getParentFile().equals(allowed))throw new IOException("RETENTION_REQUEST_PATH");
        String body=ReportJobJournalCP12.readAtomic(input,65536);RetentionArchiveProtocolCP12.Page page=RetentionArchiveProtocolCP12.Page.parse(body);
        if(!input.getName().equals(page.requestId+".json")||!ReleaseManifestR926.MANIFEST_SHA256.equals(page.releaseHash))throw new IOException("RETENTION_REQUEST_IDENTITY");
        JSONObject request=new JSONObject();request.put("revision","REPORT_REQUEST_CP12_1");request.put("requestId",page.requestId);request.put("mode","RETENTION_PAGE");
        request.put("releaseHash",page.releaseHash);request.put("createdWallMs",System.currentTimeMillis());request.put("retentionPage",new JSONObject(body));
        File root=requestRoot(app);ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+page.requestId+".json"),request.toString());
        ReportRequestQueueCP12.enqueue(root,page.requestId,"RETENTION_PAGE");kick(app,ACTION_EXPORT,page.requestId);return page.requestId;
        } catch(IOException error){throw error;}catch(Exception error){throw new IOException("RETENTION_REQUEST_INVALID",error);}
    }
    public static void requestCancel(Context context,String jobId)throws Exception {
        requireHash(jobId);Context app=context.getApplicationContext();ReportJobJournalCP12.requestCancellation(requestRoot(app),jobId);kick(app,ACTION_CANCEL,jobId);
    }
    /** Called by the UI I/O executor, independent of bulky report observation collection. */
    public static String requestReportControl(Context context,boolean clear)throws Exception {
        Context app=context.getApplicationContext();String command;
        synchronized(USER_REQUEST_GENERATION){USER_REQUEST_GENERATION.incrementAndGet();command=ReportTaskControlR69.request(app.getFilesDir(),clear);}
        kick(app,ACTION_RESUME,command);return command;
    }
    private static void enqueueUserRequest(File root,String id,String mode,JSONObject request,long generation)throws Exception {
        synchronized(USER_REQUEST_GENERATION){
            if(generation!=USER_REQUEST_GENERATION.get())throw new IOException("REPORT_SUPERSEDED_BY_USER");
            ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),request.toString());ReportRequestQueueCP12.enqueue(root,id,mode);
        }
    }
    private static void enqueueCommittedRequestR31(File root,String id,String mode,long generation)throws Exception {
        synchronized(USER_REQUEST_GENERATION){
            if(generation!=USER_REQUEST_GENERATION.get())throw new IOException("REPORT_SUPERSEDED_BY_USER");
            ReportRequestQueueCP12.enqueue(root,id,mode);
        }
    }
    public static void resumeExport(Context context,String jobId)throws IOException {
        requireHash(jobId);
        kick(context.getApplicationContext(),ACTION_RESUME,jobId);
    }
    private static void kick(Context app,String action,String id){Intent intent=new Intent(app,ReportExportServiceCP4.class).setAction(action).putExtra("id",id);
        if(Build.VERSION.SDK_INT>=26)app.startForegroundService(intent);else app.startService(intent);}
    private static File requestRoot(Context app)throws IOException{return ReportDirectoriesCP12.root(app.getFilesDir());}
    /** R31Q 019: the last request-side outcome of this process; a request must never end without a trace. */
    private static volatile String lastRequestOutcomeR31Q="NONE";
    static String lastRequestOutcomeR31Q(){return lastRequestOutcomeR31Q;}
    private static String describeR31Q(Throwable error){StringBuilder b=new StringBuilder();int depth=0;for(Throwable t=error;t!=null&&depth<4;t=t.getCause(),depth++){if(depth>0)b.append(" <- ");b.append(t.getClass().getName()).append(':').append(t.getMessage());StackTraceElement[] st=t.getStackTrace();for(int i=0;i<st.length&&i<6;i++)b.append(" @").append(st[i]);}return b.length()<=3000?b.toString():b.substring(0,3000);}
    private static void requestFailure(Context app,String id,Throwable error){
        lastRequestOutcomeR31Q="FAILED|request="+id+"|at="+System.currentTimeMillis()+"|"+describeR31Q(error);
        // Publish to the submitting process before trying any fallible filesystem work.
        // Both the full and minimal durable ACK use admission, so neither can be
        // relied on when the admission coordinator itself is the failing dependency.
        ReportRequestStatusR31.failed(id,describeR31Q(error));
        try{requestFailureCP12(app,id,error);if(!new File(requestRoot(app),"requests/"+id+".ack.json").isFile())throw new IOException("REPORT_FAILURE_ACK_NOT_WRITTEN");}
        catch(Throwable receipt){
            lastRequestOutcomeR31Q=lastRequestOutcomeR31Q+"|ackError="+describeR31Q(receipt);
            // Minimal receipt so the UI reports the failure instead of waiting for an acknowledgement.
            try{JSONObject failure=new JSONObject();failure.put("requestId",id);failure.put("stage","FAILED");failure.put("reason",boundedReason(error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage())));ReportJobJournalCP12.writeAtomic(new File(requestRoot(app),"requests/"+id+".ack.json"),failure.toString());}
            catch(Throwable second){lastRequestOutcomeR31Q=lastRequestOutcomeR31Q+"|minimalAckError="+describeR31Q(second);}
        }
    }
    private static void requestFailureCP12(Context app,String id,Throwable error){try{JSONObject failure=new JSONObject();failure.put("requestId",id);failure.put("stage","FAILED");failure.put("reason",boundedReason(error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage())));File root=requestRoot(app);try{failure.put("recovery",ReportJobJournalCP12.writeFailureArtifact(root,id,error));}catch(Exception unavailable){failure.put("recoveryFailure",boundedReason(unavailable.getClass().getSimpleName()+":"+unavailable.getMessage()));}ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".ack.json"),failure.toString());}catch(Exception ignored){/* No success acknowledgement is generated. */}}
    private static String id(String mode){return RetentionArchiveProtocolCP12.sha(("REPORT_CP12|"+ReleaseManifestR926.MANIFEST_SHA256+"|"+mode+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));}
    private static void requireHash(String value)throws IOException{if(value==null||!value.matches("[0-9a-f]{64}"))throw new IOException("REPORT_HASH");}

    @Override public void onCreate(){super.onCreate();worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(1),r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"report-owner-cp12"));
        controlTimer=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"report-control-cp12"));
        controlTimer.scheduleWithFixedDelay(()->{ActiveControl active=control;if(active!=null&&!active.shouldContinue()&&control==active){active.cancelReadR30();ReportCatalogCP4.cancelActive();}},ReportTaskControlR69.CHECKPOINT_MS_R30,ReportTaskControlR69.CHECKPOINT_MS_R30,TimeUnit.MILLISECONDS);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){lastStartId=startId;foreground("报告等待处理");
        if(intent!=null&&ACTION_CANCEL.equals(intent.getAction())){String key=intent.getStringExtra("id");if(key!=null&&key.matches("[0-9a-f]{64}")){ActiveControl active=control;if(active!=null&&active.cancelKey.equals(key))active.cancel.set(true);try{REQUESTS.execute(()->{try{ReportJobJournalCP12.requestCancellation(requestRoot(getApplicationContext()),key);}catch(Exception error){android.util.Log.e("ReportExportCP12","cancel command persistence failed",error);}});}catch(RejectedExecutionException full){android.util.Log.w("ReportExportCP12","active task cancel is set; redelivered intent will persist after restart",full);}}}
        if(drainQueued.compareAndSet(false,true)){try{workerFuture=worker.submit(this::drain);}catch(RejectedExecutionException full){drainQueued.set(false);}}
        return START_REDELIVER_INTENT;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){destroyed.set(true);ActiveControl active=control;if(active!=null){active.stopR31("REPORT_PROCESS_SHUTDOWN",null);active.cancelReadR30();}ReportCatalogCP4.cancelActive();Future<?> future=workerFuture;if(future!=null)future.cancel(true);worker.shutdownNow();controlTimer.shutdownNow();super.onDestroy();}
    private void foreground(String text){NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"STARCORE 检测导出",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        ActiveControl active=control;if(active!=null){Intent cancel=new Intent(this,ReportExportServiceCP4.class).setAction(ACTION_CANCEL).putExtra("id",active.cancelKey);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;PendingIntent command=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,NOTIFICATION+1,cancel,flags):PendingIntent.getService(this,NOTIFICATION+1,cancel,flags);builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,"取消报告任务",command);}
        startForeground(NOTIFICATION,builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("STARCORE 检测报告").setContentText(text).setOngoing(true).build());}

    private void drain(){
        final int ownerStartId=lastStartId;
        String currentRequest="";
        try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(getFilesDir())) {
            if(journal==null)return;
            journal.reconcileAcknowledgementsR31();
            ReportRequestQueueCP12.reconcileRequestsR31(journal.root());
            String queued=ReportRequestQueueCP12.head(journal.root());if(queued!=null)currentRequest=queued;
            ReportTaskControlR69.applyPending(journal);
            ReportJobJournalCP12.Job publishedCleanup=journal.current();if(publishedCleanup!=null&&publishedCleanup.stage.equals("READY_TO_SHARE"))enqueueCleanupR31(journal,publishedCleanup);
            runCleanupR31(journal);
            ReportPublicationCP12.pruneRequestMetadata(journal.root(),System.currentTimeMillis());
            ReportJobJournalCP12.Job job=journal.current();if(job!=null&&job.stage.equals("CANCELLED"))ReportTaskControlR69.finishCancellationR30(journal.root(),journal.directory(job),job);if(job!=null&&isActive(job))currentRequest=job.requestId;
            if(job!=null&&isActive(job)&&job.mode.equals("RETENTION_PAGE")&&ReportRequestQueueCP12.interactiveHead(journal.root())!=null){
                journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());
                ReportRequestQueueCP12.enqueue(journal.root(),job.requestId,"RETENTION_PAGE");job=null;
            }
            requireDiagnosticProcess();ReportNativeCP4.loadForDiagnosticProcess(processName(),getPackageName());JSONObject request=null;
            if(job!=null&&isActive(job)){currentRequest=job.requestId;request=loadRequest(journal.root(),job.requestId);}
            else {request=nextRequest(journal.root());if(request==null)return;
                String requestId=request.getString("requestId"),mode=request.getString("mode");requireHash(requestId);
                if(mode.equals("SAF_TRANSFER")){runSafRequest(request,journal.root());return;}
                if(job!=null&&(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY"))&&job.requestId.equals(requestId)){ActiveControl ready=new ActiveControl(job,journal.root());try(ReportIoR31.Scope outputScope=ReportIoR31.bind(journal.directory(job),ready)){verifyReady(job,journal.directory(job),ready);enqueueCleanupR31(journal,job);journal.acknowledge(requestId,job);clearPending(journal.root(),request);}catch(Exception callback){if(!ready.validatedDeliveryR31)throw callback;android.util.Log.w("ReportExportCP12","Verified READY replay callback pending",callback);}finally{ready.releaseResourcesR31();}runCleanupR31(journal);return;}
                job=journal.recoverOrCreateR31(requestId,request.getString("releaseHash"),mode);ReportPublicationCP12.pruneTerminal(journal.root(),job.jobId);clearPending(journal.root(),request);}
            journal.acknowledge(job.requestId,job);ActiveControl active=new ActiveControl(job,journal.root());control=active;foreground("报告采集与核验进行中");
            try(ReportIoR31.Scope outputScope=ReportIoR31.bind(journal.directory(job),active);ReportInputsLeaseCP12 inputPin=job.mode.equals("FULL_REPORT")?ReportInputsLeaseCP12.acquire(getFilesDir()):null) {
                if(!job.releaseHash.equals(ReleaseManifestR926.MANIFEST_SHA256))throw new IOException("REPORT_VERSION_CHANGED");
                long budget=Math.min(8*MiB,Runtime.getRuntime().maxMemory()/8);if(budget<8*MiB)throw new IOException("REPORT_MEMORY_BUDGET");
                File directory=journal.directory(job),snapshot=new File(directory,"snapshot.db");
                job.attempt=Math.addExact(job.attempt,1);
                // Reclaim expired deliveries before checking space for a new complete export.
                ReportPublicationCP12.pruneOldReports(journal.root(),directory,System.currentTimeMillis());
                ReportPublicationCP12.pruneTerminal(journal.root(),job.jobId);
                if(job.mode.equals("RETENTION_PAGE")) {
                    RetentionArchiveProtocolCP12.Page page=RetentionArchiveProtocolCP12.Page.parse(ReportJobJournalCP12.readAtomic(new File(getFilesDir(),"retention-requests-cp12/"+job.requestId+".json"),65536));
                    if(!job.requestId.equals(page.requestId))throw new IOException("RETENTION_REQUEST_IDENTITY");
                    // Retire an older release's per-page copy. The shared private copy
                    // below never substitutes for a FULL_REPORT snapshot.
                    deleteFile(snapshot);deleteFile(new File(snapshot.getPath()+"-journal"));
                    job.snapshotSha="";job.snapshotBytes=0;
                    active.enter("BACKUP",120000);journal.transition(job,ReportJobJournalCP12.Stage.BACKUP,System.currentTimeMillis());
                    RetentionArchiveProtocolCP12.Receipt recovered=RetentionArchiveIoCP12.recoverPublishedReceipt(getFilesDir(),page,active);
                    if(recovered!=null){
                        job.snapshotSha=recovered.snapshotHash;job.lastReason="RETENTION_RECEIPT_RECOVERED";
                        journal.transition(job,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,System.currentTimeMillis());
                        journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_ENCODING,System.currentTimeMillis());
                    }else try(RetentionSnapshotCacheR73.Lease cache=RetentionSnapshotCacheR73.acquire(journal,getDatabasePath(AppConfig.DB_NAME),page,active,System.currentTimeMillis(),new StatFs(getFilesDir().getPath()).getAvailableBytes())){
                        job.snapshotWallMs=cache.capturedWallMs;job.snapshotSha=cache.verified.sha256;job.snapshotBytes=cache.verified.byteCount;
                        job.lastReason=cache.reused?"RETENTION_SNAPSHOT_REUSED":"RETENTION_SNAPSHOT_CREATED";
                        journal.transition(job,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,System.currentTimeMillis());
                        active.enter("RETENTION_PAGE",120000);journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_ENCODING,System.currentTimeMillis());
                        RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.runVerified(getFilesDir(),page,directory,cache.file,cache.verified,active);
                        File replies=new File(getFilesDir(),"retention-replies-cp12");if(!replies.isDirectory()&&!replies.mkdirs())throw new IOException("RETENTION_REPLY_DIRECTORY");
                        RetentionArchiveIoCP12.writeReceipt(new File(replies,job.requestId+".receipt"),receipt);
                    }
                    active.validatedDeliveryR31=true;job.evidenceComplete=true;job.checksComplete=false;job.allChecksPassed=false;
                    journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_RECEIPT_DURABLE,System.currentTimeMillis());
                    journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_READY,System.currentTimeMillis());journal.acknowledge(job.requestId,job);return;
                }
                RetentionSnapshotCacheR73.discard(journal); // release optional cleanup copy before reserving full-report space
                reserveDisk(job,journal,active);
                boolean snapshotValid=validSnapshot(job,snapshot,active);
                if(snapshotValid&&job.mode.equals("FULL_REPORT")&&!ReportAttachmentClosureCP12.isComplete(directory,job.snapshotSha,active))snapshotValid=false;
                if(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY")){verifyReady(job,directory,active);enqueueCleanupR31(journal,job);journal.acknowledge(job.requestId,job);return;}
                File existing=job.publishedDir.isEmpty()?null:ReportArchiveVerifierCP12.inside(directory,job.publishedDir);
                if(job.stage.equals("VERIFYING")&&existing!=null&&new File(existing,"manifest.jsonl").isFile()) {
                    publish(job,journal,directory,existing,active);journal.acknowledge(job.requestId,job);return; // raw temporaries may already be gone.
                }
                if(!snapshotValid) {
                    active.enter("BACKUP",120000);journal.transition(job,ReportJobJournalCP12.Stage.BACKUP,System.currentTimeMillis());
                    deleteFile(snapshot);deleteFile(new File(snapshot.getPath()+"-journal"));
                    // Native backup pins BEGIN + schema_version before copying pages. Its
                    // read view is the start, not the end of a potentially two-minute copy.
                    job.snapshotWallMs=System.currentTimeMillis();
                    active.backupTargetR31=snapshot;ReportNativeCP4.backup(getDatabasePath(AppConfig.DB_NAME),snapshot,SQLITE_BACKUP_BUDGET,active);
                    active.enter("SNAPSHOT_HASH",120000);
                    sync(snapshot);job.snapshotSha=ReportArchiveCP4.hash(snapshot,active);job.snapshotBytes=snapshot.length();
                    journal.transition(job,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,System.currentTimeMillis());
                }
                active.enter("INDEXING",120000);journal.transition(job,ReportJobJournalCP12.Stage.INDEXING,System.currentTimeMillis());
                File memberInput=new File(directory,"members-unsorted-"+job.attempt+".jsonl");ReportCatalogCP4.Result catalog;
                try(FileOutputStream fd=new FileOutputStream(memberInput);Writer members=new BufferedWriter(new OutputStreamWriter(active.protectOutput(memberInput,fd),StandardCharsets.UTF_8),8192);
                    SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                    CheckpointSourceScopeR31 checkpointSource=openCheckpointSourceR31(copy,getFilesDir());
                    com.ethk3.novaonepro.strategyv2.KeepMigrationStoreR31.ReaderRootLease keepSource=openKeepSourceR31(copy,getFilesDir())) {
                    if(!copy.isReadOnly())throw new IOException("REPORT_READONLY_SNAPSHOT_REQUIRED");
                    if(ReportAttachmentClosureCP12.isComplete(directory,job.snapshotSha,active))ReportAttachmentClosureCP12.appendComplete(directory,members,active);
                    else {
                        // The initial intent may have been claimed before its
                        // optional observers completed. Refresh only evidence;
                        // preserve the claimed export mode/profile/identity.
                        JSONObject latest=loadRequest(journal.root(),job.requestId);
                        if(!job.requestId.equals(latest.getString("requestId"))||!job.releaseHash.equals(latest.getString("releaseHash")))throw new IOException("REPORT_REQUEST_OBSERVATION_IDENTITY");
                        JSONObject frozenRequest=new JSONObject(request.toString());
                        frozenRequest.put("observation",ReportAttachmentClosureCP12.requestObservationR31(latest));
                        ReportAttachmentClosureCP12.capture(this,copy,directory,frozenRequest,active,members,job.snapshotSha,job.snapshotBytes);
                    }
                    android.os.CancellationSignal cancellation=new android.os.CancellationSignal();
                    try(ReportReadContextCP4 ctx=new ReportReadContextCP4(copy,job.jobId,job.snapshotSha,job.snapshotWallMs,
                            new ReportReadContextCP4.RuntimeObservationCP4(ReportJobJournalCP12.readAtomic(new File(directory,"runtime-observation.json"),65536)),
                            new ReportReadContextCP4.SourceManifestCP4(ReportJobJournalCP12.readAtomic(new File(directory,"source-manifest.json"),65536)),cancellation,3*MiB)) {
                        catalog=ReportCatalogCP4.write(ctx,directory,(phase,completed)->active.checkpoint());ctx.finishArtifacts();
                        File readable=ReportCatalogCP4.writeReadableSummary(directory,catalog,active);ReportMemberPlanCP12.append(members,readable.getName(),readable.getName(),ReportArchiveCP4.hash(readable,active),readable.length());
                    }
                    if(request.optBoolean("includeRawEvidence", false)) {
                        ReportMemberPlanCP12.append(members,"snapshot.db","snapshot.db",job.snapshotSha,job.snapshotBytes);
                    } else {
                        File scope = ReportDiagnosticEvidenceR64.write(copy, directory, job.snapshotSha, active);
                        ReportMemberPlanCP12.append(members,scope.getName(),scope.getName(),ReportArchiveCP4.hash(scope,active),scope.length());
                    }
                    File inventory=ReportDiagnosticEvidenceR64.writeFileInventory(getFilesDir().getParentFile(),directory,active);
                    // R13-P31: byte census by category on the first page of every report
                    File census=new File(directory,"STORAGE_CENSUS.txt");android.os.CancellationSignal spaceCancellation=new android.os.CancellationSignal();active.readCancellationR30=spaceCancellation;
                    try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(census,new java.io.FileOutputStream(census)),StandardCharsets.UTF_8)){
                        w.write(StorageCensusSU2.measure(getFilesDir().getParentFile(),java.util.Collections.<String>emptyList(),()->!active.shouldContinue()).line());w.write('\n');active.checkpoint();
                        w.write(com.ethk3.novaonepro.strategyv2.JsonR27.write(StorageCensusSU2.sqliteSpace(copy,spaceCancellation)));w.write('\n');
                    }finally{active.readCancellationR30=null;}
                    ReportMemberPlanCP12.append(members,census.getName(),census.getName(),ReportArchiveCP4.hash(census,active),census.length());
                    File r26Report=new File(directory,"R26_REPORT.json");
                    try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(r26Report,new java.io.FileOutputStream(r26Report)),StandardCharsets.UTF_8)){w.write(com.ethk3.novaonepro.strategyv2.RuntimeReportR26.json(copy,new org.json.JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"source-manifest.json"),65536))));}
                    ReportMemberPlanCP12.append(members,r26Report.getName(),r26Report.getName(),ReportArchiveCP4.hash(r26Report,active),r26Report.length());
                    // R27-051: readable automatic R27 chapters and the summary used for "compare with previous report".
                    File r27Summary=new File(new File(getFilesDir(),"r27"),"last_report_summary.json");
                    File r27Report=new File(directory,"R27_REPORT.md");
                    try{
                        if(r27Summary.isFile())com.ethk3.novaonepro.strategyv2.ReportDataR27.previousSummary=(java.util.Map<String,Object>)com.ethk3.novaonepro.strategyv2.JsonR27.parse(readR27Summary(r27Summary));
                        java.util.List<java.util.Map<String,Object>> r27Chapters=com.ethk3.novaonepro.strategyv2.ReportDataR27.chapters(copy);
                        try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(r27Report,new java.io.FileOutputStream(r27Report)),StandardCharsets.UTF_8)){w.write(com.ethk3.novaonepro.strategyv2.ReportR27.markdown(r27Chapters));}
                        Object summary=r27Chapters.isEmpty()?null:r27Chapters.get(0).get("summary");
                        if(summary!=null){r27Summary.getParentFile().mkdirs();ReportJobJournalCP12.writeAtomic(r27Summary,com.ethk3.novaonepro.strategyv2.JsonR27.write(summary));}
                    }catch(Exception r27Failure){try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(r27Report,new java.io.FileOutputStream(r27Report)),StandardCharsets.UTF_8)){w.write("# R27 报告生成失败\n\n"+r27Failure);}}
                    ReportMemberPlanCP12.append(members,r27Report.getName(),r27Report.getName(),ReportArchiveCP4.hash(r27Report,active),r27Report.length());
                    // R30 primary report: capture once, then serialize the identical twelve chapters twice.
                    java.util.Map<String,Object> r30Source=(java.util.Map<String,Object>)com.ethk3.novaonepro.strategyv2.JsonR27.parse(ReportJobJournalCP12.readAtomic(new File(directory,"source-manifest.json"),65536));
                    r30Source.put("reportJobId",job.jobId);r30Source.put("snapshotSha256",job.snapshotSha);
                    r30Source.put("checkpointCasSource",checkpointSource.observation());
                    File r30Previous=new File(new File(getFilesDir(),"r30"),"last_successful_report_summary.json");
                    if(r30Previous.isFile())try{r30Source.put("previousSuccessfulReport",com.ethk3.novaonepro.strategyv2.JsonR27.parse(ReportJobJournalCP12.readAtomic(r30Previous,1024*1024)));}
                    catch(Exception unreadablePrevious){java.util.Map<String,Object> previousFailure=new java.util.LinkedHashMap<>();previousFailure.put("readStatus","UNKNOWN_PREVIOUS_SUMMARY_UNREADABLE");previousFailure.put("failureType",unreadablePrevious.getClass().getSimpleName());r30Source.put("previousSuccessfulReport",previousFailure);}
                    active.checkpoint();android.os.CancellationSignal r30ReadCancellation=new android.os.CancellationSignal();active.readCancellationR30=r30ReadCancellation;
                    java.util.List<java.util.Map<String,Object>> r30Chapters;
                    try{r30Chapters=com.ethk3.novaonepro.strategyv2.ReportR29.chapters(copy,r30Source,r30ReadCancellation);}finally{active.readCancellationR30=null;}
                    active.checkpoint();
                    File r30Json=new File(directory,"R30_REPORT.json"),r30Markdown=new File(directory,"R30_REPORT.md");
                    java.util.Map<String,Object> r31Document=com.ethk3.novaonepro.strategyv2.ReportR29.document(r30Chapters);
                    ReportSemanticVerifierR31.validateDocument(r31Document,job);
                    try(java.io.Writer w=new java.io.BufferedWriter(new java.io.OutputStreamWriter(active.protectOutput(r30Json,new java.io.FileOutputStream(r30Json)),StandardCharsets.UTF_8),16384)){ReportJsonWriterR31.write(r31Document,w);}
                    try(java.io.Writer w=new java.io.BufferedWriter(new java.io.OutputStreamWriter(active.protectOutput(r30Markdown,new java.io.FileOutputStream(r30Markdown)),StandardCharsets.UTF_8),16384)){ReportJsonWriterR31.markdownR30(r30Chapters,w);}
                    ReportMemberPlanCP12.append(members,r30Json.getName(),r30Json.getName(),ReportArchiveCP4.hash(r30Json,active),r30Json.length());
                    ReportMemberPlanCP12.append(members,r30Markdown.getName(),r30Markdown.getName(),ReportArchiveCP4.hash(r30Markdown,active),r30Markdown.length());
                    File r31Receipt=ReportSemanticVerifierR31.writeReceipt(directory,r31Document,catalog,job,request.optBoolean("includeRawEvidence",false),active);
                    ReportMemberPlanCP12.append(members,r31Receipt.getName(),r31Receipt.getName(),ReportArchiveCP4.hash(r31Receipt,active),r31Receipt.length());
                    job.semanticVerificationR31="REQUIRES_ZIP_READBACK";
                    File r30SummaryCandidate=new File(directory,"R30_SUCCESSFUL_SUMMARY_CANDIDATE.json");
                    ReportJobJournalCP12.writeAtomic(r30SummaryCandidate,com.ethk3.novaonepro.strategyv2.JsonR27.write(r30Chapters.get(11).get("successfulSummaryCandidate")));
                    ReportMemberPlanCP12.append(members,r30SummaryCandidate.getName(),r30SummaryCandidate.getName(),ReportArchiveCP4.hash(r30SummaryCandidate,active),r30SummaryCandidate.length());
                    File r26Identities=new File(directory,"R26_POLICY_IDENTITIES.jsonl");
                    com.ethk3.novaonepro.strategyv2.PolicyIdentityLedgerR26.export(copy,r26Identities,job.snapshotSha,request.optBoolean("includeRawEvidence",false),active::checkpoint);
                    ReportMemberPlanCP12.append(members,r26Identities.getName(),r26Identities.getName(),ReportArchiveCP4.hash(r26Identities,active),r26Identities.length());
                    File r26Surface=new File(directory,"R26_FROZEN_SURFACE.json");
                    try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(r26Surface,new java.io.FileOutputStream(r26Surface)),StandardCharsets.UTF_8)){w.write(com.ethk3.novaonepro.strategyv2.FrozenSurfaceStoreR26.exportJson(copy));}
                    ReportMemberPlanCP12.append(members,r26Surface.getName(),r26Surface.getName(),ReportArchiveCP4.hash(r26Surface,active),r26Surface.length());
                    String[] su2Diagnosis=com.ethk3.novaonepro.strategyv2.DiagnosisStoreSU2.report(copy);
                    for(int su2i=0;su2i<2;su2i++){File diag=new File(directory,su2i==0?"UPGRADE_DIAGNOSIS.md":"UPGRADE_DIAGNOSIS.json");
                        try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(diag,new java.io.FileOutputStream(diag)),StandardCharsets.UTF_8)){w.write(su2Diagnosis[su2i]);}
                        ReportMemberPlanCP12.append(members,diag.getName(),diag.getName(),ReportArchiveCP4.hash(diag,active),diag.length());}
                    File su2Matrices=new File(directory,"STRATEGY_MATRICES.json");
                    try(java.io.Writer w=new java.io.OutputStreamWriter(active.protectOutput(su2Matrices,new java.io.FileOutputStream(su2Matrices)),StandardCharsets.UTF_8)){w.write(com.ethk3.novaonepro.strategyv2.MatrixStoreSU2.report(copy));}
                    ReportMemberPlanCP12.append(members,su2Matrices.getName(),su2Matrices.getName(),ReportArchiveCP4.hash(su2Matrices,active),su2Matrices.length());
                    ReportMemberPlanCP12.append(members,inventory.getName(),inventory.getName(),ReportArchiveCP4.hash(inventory,active),inventory.length());
                    for(File page:catalog.indexFiles)ReportMemberPlanCP12.append(members,page.getName(),"catalog/"+page.getName(),ReportArchiveCP4.hash(page,active),page.length());
                    File renderRefs=new File(directory,"render-artifacts.jsonl");if(renderRefs.exists())try(ReportArchiveVerifierCP12.LineReader refs=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(renderRefs),8192)){byte[] line;while((line=refs.next())!=null){active.checkpoint();members.write(new String(line,StandardCharsets.UTF_8));}}
                    members.flush();fd.getFD().sync();
                }
                job.evidenceComplete=catalog.evidenceCatalogComplete;job.checksComplete=catalog.checksComplete;
                job.allChecksPassed=catalog.allChecksPassed;
                journal.transition(job,ReportJobJournalCP12.Stage.INDEX_DURABLE,System.currentTimeMillis());
                // Android snapshot connection is closed before the next native allocation stage.
                File sorted=ReportMemberPlanCP12.sort(directory,memberInput,active);long plannedRaw=0,plannedMembers=0;
                try(ReportArchiveVerifierCP12.LineReader plan=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192)){byte[] row;while((row=plan.next())!=null){active.checkpoint();JSONObject member=new JSONObject(new String(row,StandardCharsets.UTF_8));plannedRaw=Math.addExact(plannedRaw,member.getLong("bytes"));plannedMembers++;}}
                active.reserveFutureBytesR31(ReportPhaseBudgetR31.archiveFuture(plannedRaw,plannedMembers));
                job.compressionAttempt=Math.addExact(job.compressionAttempt,1);
                File archive=new File(directory,"archive-"+job.compressionAttempt);if(!archive.mkdir())throw new IOException("REPORT_ARCHIVE_EXCLUSIVE_DIRECTORY");job.publishedDir=archive.getName();
                active.enter("COMPRESSING",120000);journal.transition(job,ReportJobJournalCP12.Stage.COMPRESSING,System.currentTimeMillis());
                try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,job.jobId,active);ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192)) {
                    byte[] line;while((line=rows.next())!=null){active.checkpoint();JSONObject member=new JSONObject(new String(line,StandardCharsets.UTF_8));File input=ReportArchiveVerifierCP12.inside(directory,member.getString("path"));
                        if(input.length()!=member.getLong("bytes")||!member.getString("sha256").equals(ReportArchiveCP4.hash(input,active)))throw new IOException("REPORT_MEMBER_CHANGED");
                        writer.add(member.getString("name"),input,NATIVE_BUDGET,active);}
                    writer.finish(job.evidenceComplete,job.checksComplete,request.optBoolean("includeRawEvidence",false)?"FULL_REPORT_SINGLE_SNAPSHOT_AND_FROZEN_ATTACHMENTS":"COMPLETE_DIAGNOSTIC_R64_RAW_PAYLOADS_EXCLUDED");
                }
                journal.transition(job,ReportJobJournalCP12.Stage.VERIFYING,System.currentTimeMillis());publish(job,journal,directory,archive,active);journal.acknowledge(job.requestId,job);
            } catch(Throwable error) {
                active.releaseResourcesR31(); // Native/stream/input try scopes have actually exited before the release receipt.
                // The durable READY transition is authoritative after verified delivery. An ack,
                // observer or cleanup callback failure must not create a second report or demote it.
                if(active.validatedDeliveryR31&&(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY"))){
                    android.util.Log.w("ReportExportCP12","Verified report committed; post-publication callback needs retry",error);
                    try{journal.acknowledge(job.requestId,job);}catch(Exception ack){android.util.Log.w("ReportExportCP12","Published acknowledgement retry pending",ack);}
                    return;
                }
                Thread.interrupted();String reason=active.stopReason.isEmpty()?classify(error):active.stopReason;job.lastReason=reason;
                Throwable original=active.stopCauseR31==null?error:active.stopCauseR31;
                if(original!=error&&original.getSuppressed().length<8)original.addSuppressed(error);
                if(reason.equals("REPORT_USER_CANCEL")){
                    journal.transition(job,ReportJobJournalCP12.Stage.CANCELLED,System.currentTimeMillis());
                    try{ReportTaskControlR69.finishCancellationR30(journal.root(),journal.directory(job),job);}catch(Exception cleanup){job.lastReason="REPORT_CANCEL_CLEANUP_FAILED:"+cleanup.getClass().getSimpleName();journal.checkpoint(job,System.currentTimeMillis());android.util.Log.e("ReportExportCP12","cancelled job cleanup requires retry",cleanup);}
                }
                else if(reason.equals("REPORT_REALTIME_PREEMPT")||reason.equals("REPORT_PROCESS_SHUTDOWN")||reason.equals("REPORT_WORKER_INTERRUPTED")){job.preemptions++;job.resumeStage=job.snapshotSha.isEmpty()?"BACKUP":"INDEXING";if(job.preemptions>30){job.lastReason="REPORT_PREEMPTION_LIMIT";journal.transition(job,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());}else{journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());scheduleResume(job.jobId);}}
                else if(isRetryable(reason)&&++job.failureAttempts<30){job.resumeStage=job.snapshotSha.isEmpty()?"BACKUP":"INDEXING";journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());scheduleResume(job.jobId,Math.min(60000L,1000L<<Math.min(6,job.failureAttempts)));}
                else {journal.transition(job,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());}
                if(job.stage.equals("FAILED")&&job.mode.equals("FULL_REPORT"))try{ReportJobJournalCP12.writeFailureArtifact(journal.root(),job.requestId,original,job,active::recoveryCancelledR31);}catch(Exception unavailable){android.util.Log.e("ReportExportCP12","recovery evidence unavailable",unavailable);}
                journal.acknowledge(job.requestId,job);
            } finally {active.releaseResourcesR31();control=null;runCleanupR31(journal);try{StorageLedgerR31.sampleIfDue(getFilesDir(),"REPORT_OWNER_RELEASED",job.jobId,true);}catch(IOException censusFailure){android.util.Log.w("ReportExportCP12","Storage sample incomplete",censusFailure);}}
        }catch(Throwable error){if(!currentRequest.isEmpty()){
            requestFailure(getApplicationContext(),currentRequest,error);
            try(ReportJobJournalCP12 recovery=ReportJobJournalCP12.tryClaim(getFilesDir())){if(recovery!=null){
                ReportJobJournalCP12.Job failed=recovery.current();
                if(failed!=null&&failed.requestId.equals(currentRequest)&&isActive(failed)){failed.lastReason=classify(error);recovery.transition(failed,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());recovery.acknowledge(currentRequest,failed);}
                ReportRequestQueueCP12.remove(recovery.root(),currentRequest);
            }}catch(Exception recoveryFailure){android.util.Log.e("ReportExportCP12","report failure receipt recovery failed",recoveryFailure);}
        }android.util.Log.e("ReportExportCP12","report owner failed",error);}
        finally{drainQueued.set(false);main.post(()->{if(lastStartId==ownerStartId){stopForeground(false);stopSelfResult(ownerStartId);}});if(!destroyed.get()){try{File root=requestRoot(this);long cleanupDelay=ReportCleanupR31.nextDelayMs(root);if(ReportRequestQueueCP12.head(root)!=null||ReportTaskControlR69.hasPending(root))scheduleResume("");else if(cleanupDelay>=0)scheduleResume("",cleanupDelay);}catch(Exception ignored){}}}
    }
    private void reserveDisk(ReportJobJournalCP12.Job job,ReportJobJournalCP12 journal,ActiveControl active)throws Exception {
        File source=getDatabasePath(AppConfig.DB_NAME);long raw=Math.addExact(source.length(),new File(source.getPath()+"-wal").length());
        if(!source.isFile()||raw<1)throw new IOException("REPORT_SOURCE_MISSING");JSONObject request=loadRequest(journal.root(),job.requestId);
        long snapshot=Math.addExact(raw,Math.addExact(raw/16,16*MiB));
        // This is a capped initial workspace promise. Exact attachment and archive plans
        // must enlarge it before materialization; 256MiB is not a source-data worstcase.
        job.temporaryReservationBytes=ReportPhaseBudgetR31.initial(snapshot);job.publishedQuotaBytes=ReportBudgetSU2.PUBLISHED_QUOTA_BYTES;
        String relative="files/r981_reports/jobs/"+job.jobId;
        try{active.storageReservationR31=StorageBudgetR31.forFiles(getFilesDir()).reserve("report:"+job.jobId,job.revision,StorageBudgetR31.Category.REPORT_WORKSPACE,relative,job.temporaryReservationBytes,Math.multiplyExact(job.temporaryReservationBytes,4L),true);}
        catch(StorageBudgetR31.Rejected unavailable){throw new IOException("REPORT_STORAGE_BUDGET_"+unavailable.reason,unavailable);}
        journal.checkpoint(job,System.currentTimeMillis());
        try{StorageLedgerR31.sampleIfDue(getFilesDir(),"REPORT_ADMITTED",job.jobId,true);}catch(IOException censusFailure){android.util.Log.w("ReportExportCP12","Storage sample incomplete",censusFailure);}
    }
    private boolean validSnapshot(ReportJobJournalCP12.Job job,File snapshot,ActiveControl active)throws Exception {
        if(job.snapshotSha.isEmpty()||!snapshot.isFile()||snapshot.length()!=job.snapshotBytes||!job.snapshotSha.equals(ReportArchiveCP4.hash(snapshot,active)))return false;
        try(SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);Cursor c=copy.rawQuery("PRAGMA quick_check",null)) {return c.moveToFirst()&&"ok".equals(c.getString(0))&&!c.moveToNext();}
    }
    private void publish(ReportJobJournalCP12.Job job,ReportJobJournalCP12 journal,File directory,File archive,ActiveControl active)throws Exception {
        active.enter("VERIFYING",120000);ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,job.jobId,ReportArchiveVerifierCP12.nativeDecoder(NATIVE_BUDGET),active,null);
        if(!proof.evidenceComplete)throw new IOException("REPORT_EVIDENCE_CLOSURE");
        job.manifestSha=proof.manifestSha256;job.evidenceComplete=proof.evidenceComplete;job.checksComplete=proof.checksComplete;
        JSONObject pointer=new JSONObject();pointer.put("jobId",job.jobId);pointer.put("relativeDir",job.publishedDir);pointer.put("manifestSha256",job.manifestSha);pointer.put("evidenceComplete",job.evidenceComplete);pointer.put("checksComplete",job.checksComplete);
        active.enter("ZIP_DELIVERY",120000);
        ReportPublicationCP12.prepareBoundedZipR30(directory,archive,job.jobId,job.manifestSha,job.evidenceComplete,job.checksComplete,job.allChecksPassed,active);
        ReportSemanticVerifierR31.verifyDelivery(directory,job,active);
        job.semanticVerificationR31="ZIP_READBACK_VERIFIED";
        pointer.put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(directory,"delivery/index.json"),active));
        ReportPublicationCP12.pruneOldReports(journal.root(),directory,System.currentTimeMillis());
        ReportPublicationCP12.enforceQuota(journal.root(),directory,job.publishedQuotaBytes);   // R13-P34: the quota existed but was never called
        ReportJobJournalCP12.writeAtomic(new File(directory,"published.json"),pointer.toString());
        active.releaseResourcesForPublicationR31();
        active.validatedDeliveryR31=true;journal.transition(job,ReportJobJournalCP12.Stage.READY_TO_SHARE,System.currentTimeMillis());
        recordSuccessfulSummaryR30(job,directory);
        // The sealed verified ZIP is now the durable delivery. Remove redundant private parts only
        // after its hash and publication identity have been checked; legacy multipart stays intact.
        enqueueCleanupR31(journal,job);
    }
    private void enqueueCleanupR31(ReportJobJournalCP12 journal,ReportJobJournalCP12.Job job){
        try{ReportCleanupR31.enqueue(journal,job,"PUBLISHED_WAITING_FOR_OWNER_RESOURCE_CLOSE");}
        catch(Exception cleanup){android.util.Log.w("ReportExportCP12","Published report cleanup enrollment pending",cleanup);try{scheduleResume("",600000L);}catch(RuntimeException alarm){android.util.Log.w("ReportExportCP12","Cleanup alarm registration pending",alarm);}}
    }
    private void runCleanupR31(ReportJobJournalCP12 journal){
        try{ReportCleanupR31.runDue(journal,System.currentTimeMillis(),2);}
        catch(Exception cleanup){android.util.Log.w("ReportExportCP12","Published report cleanup needs retry",cleanup);}
    }
    private void verifyReady(ReportJobJournalCP12.Job job,File directory,ActiveControl active)throws Exception {
        if(job.mode.equals("RETENTION_PAGE")){RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.readReceipt(new File(getFilesDir(),"retention-replies-cp12/"+job.requestId+".receipt"));if(!receipt.page.requestId.equals(job.requestId))throw new IOException("RETENTION_REPLY_IDENTITY");active.validatedDeliveryR31=true;return;}
        active.enter("VERIFYING",120000);JSONObject pointer=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"published.json"),4096));
        if(!pointer.getString("jobId").equals(job.jobId)||!pointer.getString("relativeDir").equals(job.publishedDir))throw new IOException("REPORT_PUBLISH_IDENTITY");
        if(ReportPublicationCP12.verifyPublishedZip(directory,active)){
            // Historical deliveries remain readable with an explicit legacy boundary.
            // A new-format report must retain and recheck its actual semantic receipt.
            if(!job.semanticVerificationR31.equals("LEGACY_NO_SEMANTIC_RECEIPT"))ReportSemanticVerifierR31.verifyDelivery(directory,job,active);
            active.validatedDeliveryR31=true;recordSuccessfulSummaryR30(job,directory);return;
        }
        ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(ReportArchiveVerifierCP12.inside(directory,job.publishedDir),job.jobId,ReportArchiveVerifierCP12.nativeDecoder(NATIVE_BUDGET),active,null);
        if(!proof.manifestSha256.equals(job.manifestSha)||!proof.manifestSha256.equals(pointer.getString("manifestSha256")))throw new IOException("REPORT_PUBLISH_HASH");ReportPublicationCP12.delivery(directory);active.validatedDeliveryR31=true;
    }
    @SuppressWarnings("unchecked") private void recordSuccessfulSummaryR30(ReportJobJournalCP12.Job job,File directory){
        try{
            File candidate=new File(directory,"R30_SUCCESSFUL_SUMMARY_CANDIDATE.json");if(!candidate.isFile())return;
            java.util.Map<String,Object> summary=(java.util.Map<String,Object>)com.ethk3.novaonepro.strategyv2.JsonR27.parse(ReportJobJournalCP12.readAtomic(candidate,1024*1024));
            if(!job.jobId.equals(summary.get("jobId"))||!job.snapshotSha.equals(summary.get("snapshotSha256")))throw new IOException("R30_SUMMARY_PUBLISHED_IDENTITY");
            File destination=new File(new File(getFilesDir(),"r30"),"last_successful_report_summary.json");
            if(destination.isFile()){
                java.util.Map<String,Object> old=(java.util.Map<String,Object>)com.ethk3.novaonepro.strategyv2.JsonR27.parse(ReportJobJournalCP12.readAtomic(destination,1024*1024));
                if(job.jobId.equals(old.get("jobId")))return;
                if(old.get("publishedAt") instanceof Number&&((Number)old.get("publishedAt")).longValue()>job.updatedWallMs)return;
            }
            summary.put("publicationStatus","READY_TO_SHARE_VERIFIED_ZIP");summary.put("publishedManifestSha256",job.manifestSha);summary.put("publishedAt",job.updatedWallMs);
            ReportJobJournalCP12.writeAtomic(destination,com.ethk3.novaonepro.strategyv2.JsonR27.write(summary));
        }catch(Exception failure){android.util.Log.w("ReportExportCP12","Verified ZIP remains published; R30 summary persistence requires retry",failure);}
    }
    private static boolean isActive(ReportJobJournalCP12.Job job){return !(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY")||job.stage.equals("CANCELLED")||job.stage.equals("FAILED"));}
    private static JSONObject loadRequest(File root,String id)throws Exception{return new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"requests/"+id+".json"),65536));}
    /** A fresh durable intent gives optional observers one bounded grace window.
     * The owner returns immediately, releases its file lease, then its existing
     * pending-FIFO alarm retries. No sleep, SQL or writer is held. Clock rollback
     * and process death cannot create an indefinite dependency on observers. */
    static boolean observationPreparationPendingR31(JSONObject request,long wallNow){
        if(!"FULL_REPORT".equals(request.optString("mode"))||!"UNKNOWN_NOT_COLLECTED".equals(request.optString("observationState")))return false;
        long created=request.optLong("createdWallMs",0);
        return created>0&&wallNow>=created&&wallNow-created<5000;
    }
    private static JSONObject nextRequest(File root)throws Exception{
        String id=ReportRequestQueueCP12.interactiveHead(root);if(id==null)id=ReportRequestQueueCP12.head(root);
        if(id==null)return null;JSONObject request=loadRequest(root,id);
        return observationPreparationPendingR31(request,System.currentTimeMillis())?null:request;
    }
    private static void clearPending(File root,JSONObject request)throws Exception{ReportRequestQueueCP12.remove(root,request.getString("requestId"));}
    private void scheduleResume(String id){
        scheduleResume(id,1000);
    }
    private void scheduleResume(String id,long delayMs){
        // OS alarm survives stopSelf/process death; user cancel remains a separate durable command.
        Intent intent=new Intent(getApplicationContext(),ReportExportServiceCP4.class).setAction(ACTION_RESUME).putExtra("id",id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent callback=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,NOTIFICATION,intent,flags):PendingIntent.getService(this,NOTIFICATION,intent,flags);
        AlarmManager alarms=(AlarmManager)getSystemService(ALARM_SERVICE);if(alarms==null)throw new IllegalStateException("REPORT_RESUME_ALARM_UNAVAILABLE");
        long due=SystemClock.elapsedRealtime()+Math.max(1000,Math.min(600000,delayMs));
        if(Build.VERSION.SDK_INT>=23)alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,due,callback);else alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,due,callback);
    }
    private static void deleteFile(File file)throws IOException{if(file.exists()&&!file.delete())throw new IOException("REPORT_TEMP_CLEANUP");}
    private static void sync(File file)throws IOException{try(RandomAccessFile io=new RandomAccessFile(file,"rw")){io.getFD().sync();}}
    private static String boundedReason(String value){if(value==null)return "REPORT_UNCLASSIFIED_FAILURE";return value.length()<=512?value:value.substring(0,512);}
    private static boolean storageFull(Throwable error){for(int depth=0;error!=null&&depth<16;depth++,error=error.getCause())if(error instanceof android.system.ErrnoException&&((android.system.ErrnoException)error).errno==android.system.OsConstants.ENOSPC)return true;return false;}
    private static String classify(Throwable error){
        int depth=0;for(Throwable cause=error;cause!=null&&depth++<16;cause=cause.getCause()) {
            if(cause instanceof StorageBudgetR31.Rejected) {
                StorageBudgetR31.Rejected rejection=(StorageBudgetR31.Rejected)cause;
                if("CANCELLED".equals(rejection.reason))return "REPORT_WORKER_INTERRUPTED";
                return boundedReason(("DEPENDENCY".equals(rejection.failureCategory())?"REPORT_RESOURCE_WAIT|":"REPORT_STORAGE_ADMISSION_FAILURE|")+rejection.reason);
            }
            if(cause.getCause()==cause)break;
        }
        if(storageFull(error))return "REPORT_STORAGE_FULL";if(error instanceof ReportNativeCP4.Failure){ReportNativeCP4.Failure failure=(ReportNativeCP4.Failure)error;return boundedReason("REPORT_NATIVE_"+failure.stage+"_"+failure.nativeCode);}String text=error.getMessage();return boundedReason(text!=null&&(text.startsWith("REPORT_")||text.startsWith("RETENTION_")||text.startsWith("ARCHIVE_")||text.equals("STALE_PAGE"))?text:error.getClass().getSimpleName());}
    private static void recordSafStorageFailure(JSONObject state,Throwable error,String reason)throws Exception {
        if(!reason.equals("REPORT_STORAGE_FULL"))return;
        // Retain the original typed errno without persisting arbitrary provider text/credentials.
        StringBuilder detail=new StringBuilder();for(int depth=0;error!=null&&depth<16;depth++,error=error.getCause()){
            if(detail.length()>0)detail.append(" -> ");detail.append(error.getClass().getSimpleName());
            if(error instanceof android.system.ErrnoException){int errno=((android.system.ErrnoException)error).errno;detail.append("(errno=").append(errno);if(errno==android.system.OsConstants.ENOSPC)detail.append(",ENOSPC");detail.append(')');}
        }
        state.put("failureDetail",boundedReason(detail.toString()));state.put("recovery","FREE_DESTINATION_SPACE_AND_START_NEW_SAF_TRANSFER");
    }
    private void requireDiagnosticProcess()throws IOException{if(!(getPackageName()+":diagnostics").equals(processName()))throw new IOException("REPORT_WRONG_PROCESS");}
    private String processName(){if(Build.VERSION.SDK_INT>=28)return Application.getProcessName();ActivityManager manager=(ActivityManager)getSystemService(ACTIVITY_SERVICE);List<ActivityManager.RunningAppProcessInfo> processes=manager.getRunningAppProcesses();if(processes!=null)for(ActivityManager.RunningAppProcessInfo p:processes)if(p.pid==android.os.Process.myPid())return p.processName;return "UNKNOWN";}
    private final class ActiveControl implements ReportNativeCP4.Control {
        final ReportJobJournalCP12.Job job;final File root;final String cancelKey;final AtomicBoolean cancel=new AtomicBoolean();volatile String stopReason="";volatile Throwable stopCauseR31;volatile long deadline=Long.MAX_VALUE,lastRead;private long stageStarted;
        private String progressStageR31="CLAIMED";
        private final long[] nextProgressR31=new long[2],progressFailuresR31=new long[2];
        private final Throwable[] firstProgressFailureR31=new Throwable[2];
        synchronized void stopR31(String reason,Throwable cause){if(stopReason.isEmpty()){stopReason=reason;stopCauseR31=cause;}else if(cause!=null&&stopCauseR31!=null&&cause!=stopCauseR31&&stopCauseR31.getSuppressed().length<8)stopCauseR31.addSuppressed(cause);}
        private void sampleR31(int slot,JSONObject value,long now,boolean finalSample){
            if(now<nextProgressR31[slot]&&(!finalSample||progressFailuresR31[slot]>0))return;
            String name=slot==0?"stage-progress.json":"backup-progress.json";
            try{ReportJobJournalCP12.writeAtomic(new File(root,"jobs/"+job.jobId+"/"+name),value.toString());nextProgressR31[slot]=SystemClock.elapsedRealtime()+1000;}
            catch(IOException|RuntimeException error){
                progressFailuresR31[slot]++;if(firstProgressFailureR31[slot]==null)firstProgressFailureR31[slot]=error;
                nextProgressR31[slot]=SystemClock.elapsedRealtime()+5000;
                android.util.Log.w("ReportExportCP12",ReportJobJournalCP12.redactFailureR31("UI sample unavailable file="+name+" stage="+progressStageR31+" job="+job.jobId+" failures="+progressFailuresR31[slot]+" cause="+describeR31Q(error)));
            }
        }
        volatile android.os.CancellationSignal readCancellationR30;
        volatile boolean validatedDeliveryR31;volatile StorageBudgetR31.Reservation storageReservationR31;private File backupTargetR31;private long backupChargeR31,backupBeforeR31;
        private com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Lease phaseMemoryR31;
        boolean recoveryCancelledR31(){return cancel.get()||destroyed.get()||Thread.currentThread().isInterrupted();}
        void cancelReadR30(){android.os.CancellationSignal active=readCancellationR30;if(active!=null)active.cancel();}
        ActiveControl(ReportJobJournalCP12.Job job,File root){this(job,root,job.jobId);}
        ActiveControl(ReportJobJournalCP12.Job job,File root,String cancelKey){this.job=job;this.root=root;this.cancelKey=cancelKey;stageStarted=SystemClock.elapsedRealtime();deadline=ReportBudgetSU2.deadline(stageStarted,120000,job.mode.equals("FULL_REPORT"));}
        void enter(String stage,long milliseconds)throws IOException{checkpoint();closePhaseR31();progressStageR31=stage;stageStarted=SystemClock.elapsedRealtime();deadline=ReportBudgetSU2.deadline(stageStarted,milliseconds,job.mode.equals("FULL_REPORT"));
            com.ethk3.novaonepro.strategyv2.MemoryBudgetR31 budget=com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.processBudget();
            budget.registerNativeCapacity(com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.NATIVE_BACKUP,SQLITE_BACKUP_BUDGET,"EXISTING_STAGE_BUDGET_NOT_DEVICE_CALIBRATION");budget.registerNativeCapacity(com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.NATIVE_DECODER,NATIVE_BUDGET,"EXISTING_STAGE_BUDGET_NOT_DEVICE_CALIBRATION");
            boolean backup=stage.equals("BACKUP")||stage.equals("RETENTION_PAGE");boolean codec=stage.equals("COMPRESSING")||stage.equals("VERIFYING")||stage.equals("ZIP_DELIVERY");
            com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind kind=backup?com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.NATIVE_BACKUP:codec?com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.NATIVE_DECODER:com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.RESEARCH;
            com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Admission admitted=budget.tryAcquire("report:"+job.jobId,job.revision,kind,backup?SQLITE_BACKUP_BUDGET:codec?NATIVE_BUDGET:8*MiB,()->cancel.get()||destroyed.get());
            if(!admitted.acquired())throw new IOException("REPORT_MEMORY_BUDGET_WAIT:"+admitted.reason);phaseMemoryR31=admitted.lease;
            try{sampleR31(0,new JSONObject().put("stage",stage).put("startedWallMs",System.currentTimeMillis()),stageStarted,true);}
            catch(org.json.JSONException error){throw new IOException("REPORT_STAGE_SAMPLE_FORMAT",error);}}
        @Override public synchronized void onBackupProgress(long totalPages,long remainingPages,int nativeCode){
            // SQLite has already returned this error before reconciliation.
            // BUSY/LOCKED retain the native bounded retry policy.
            if(nativeCode!=0&&nativeCode!=101&&nativeCode!=5&&nativeCode!=6)
                stopR31("REPORT_BACKUP_FAILED|native="+nativeCode,new ReportNativeCP4.Failure("BACKUP",nativeCode));
            if(storageReservationR31!=null&&backupChargeR31>0&&backupTargetR31!=null){try{long after=backupPhysicalR31();storageReservationR31.refundUnwritten(backupChargeR31,Math.max(0,after-backupBeforeR31));backupChargeR31=0;}catch(IOException|IllegalArgumentException|ArithmeticException failed){stopR31("REPORT_STORAGE_BUDGET_RECONCILIATION_FAILED",failed);}}
            long now=SystemClock.elapsedRealtime();
            try{JSONObject progress=new JSONObject();progress.put("stage","BACKUP");progress.put("totalPages",totalPages);progress.put("copiedPages",Math.max(0L,totalPages-remainingPages));progress.put("elapsedMs",Math.max(0L,now-stageStarted));progress.put("nativeCode",nativeCode);progress.put("sampledElapsedMs",now);
                sampleR31(1,progress,now,nativeCode==101);
            }catch(org.json.JSONException error){android.util.Log.w("ReportExportCP12","UI backup sample format unavailable",error);}
        }
        private long backupPhysicalR31()throws IOException{
            long bytes=0;for(File file:new File[]{backupTargetR31,new File(backupTargetR31.getPath()+"-journal")})if(file.exists()){
                long allocated=StorageCensusSU2.allocatedBytes(file);if(allocated<0)throw new IOException("REPORT_BACKUP_ALLOCATED_BYTES_UNKNOWN");bytes=Math.addExact(bytes,allocated);
            }return bytes;
        }
        @Override public synchronized void beforeBackupChunk(long bytes)throws IOException{checkpoint();if(storageReservationR31==null)return;try{backupBeforeR31=backupPhysicalR31();storageReservationR31.beforeChunk(bytes);backupChargeR31=bytes;}catch(IOException|RuntimeException error){stopR31(classify(error),error);throw error;}}
        @Override public boolean hasDiskAdmissionR31(){return storageReservationR31!=null;}
        @Override public void beforeOutputBytesR31(File file,long bytes)throws IOException{if(storageReservationR31==null)throw new IOException("REPORT_DISK_ADMISSION_MISSING");if(!shouldContinue())throw new InterruptedIOException(stopReason);storageReservationR31.beforeChunk(bytes);}
        @Override public void reserveFutureBytesR31(long bytes)throws IOException{if(storageReservationR31==null)throw new IOException("REPORT_DISK_ADMISSION_MISSING");storageReservationR31.ensureAdditional(bytes,ReportPhaseBudgetR31.MAX_WRITTEN,true);job.temporaryReservationBytes=storageReservationR31.maximumGrowthBytes();}
        @Override public OutputStream protectOutput(File file,OutputStream sink)throws IOException{
            try{
                if(storageReservationR31==null)return sink;
                OutputStream admitted=storageReservationR31.wrap(file,sink);
                // The UTF-8 encoder emits small chunks. Coalesce them before
                // durable admission, while every actual sink write (including
                // close-time flush) still checks cancellation and its grant.
                return new BufferedOutputStream(new FilterOutputStream(admitted){
                    @Override public void write(int value)throws IOException{checkpoint();out.write(value);}
                    @Override public void write(byte[] bytes,int offset,int count)throws IOException{checkpoint();out.write(bytes,offset,count);}
                },65536);
            }
            catch(IOException|RuntimeException|Error error){try{sink.close();}catch(IOException close){error.addSuppressed(close);}throw error;}
        }
        private void closePhaseR31(){if(phaseMemoryR31!=null){phaseMemoryR31.close();phaseMemoryR31=null;}}
        void releaseResourcesForPublicationR31()throws IOException {
            closePhaseR31();StorageBudgetR31.Reservation reserved=storageReservationR31;storageReservationR31=null;
            if(reserved!=null)reserved.close();
        }
        void releaseResourcesR31(){closePhaseR31();StorageBudgetR31.Reservation reserved=storageReservationR31;storageReservationR31=null;if(reserved!=null)try{reserved.close();}catch(IOException release){android.util.Log.w("ReportExportCP12","Disk reservation release needs recovery",release);}}
        public synchronized boolean shouldContinue(){if(!stopReason.isEmpty())return false;if(cancel.get()){stopReason="REPORT_USER_CANCEL";return false;}if(destroyed.get()){stopReason="REPORT_PROCESS_SHUTDOWN";return false;}if(Thread.currentThread().isInterrupted()){stopReason="REPORT_WORKER_INTERRUPTED";return false;}
            long now=SystemClock.elapsedRealtime();if(now>=deadline){stopReason="REPORT_STAGE_TIMEOUT";return false;}
            if(now-lastRead>=ReportTaskControlR69.CHECKPOINT_MS_R30){lastRead=now;try{
                if(ReportTaskControlR69.cancelled(root,job.requestId,cancelKey)){cancel.set(true);stopReason="REPORT_USER_CANCEL";return false;}
                if(job.mode.equals("RETENTION_PAGE")&&(ReportRequestQueueCP12.hasInteractive(root)||ReportTaskControlR69.hasPending(root))){stopReason="REPORT_REALTIME_PREEMPT";return false;}
                if(ReportJobJournalCP12.isCancellationRequested(root,cancelKey)){cancel.set(true);stopReason="REPORT_USER_CANCEL";return false;}
                File pressure=new File(root,"realtime-pressure.json"),session=new File(root,"runtime-session.json");
                if(!job.mode.equals("FULL_REPORT")&&pressure.exists()&&session.exists()){
                    JSONObject json=new JSONObject(ReportJobJournalCP12.readAtomic(pressure,4096)),owner=new JSONObject(ReportJobJournalCP12.readAtomic(session,4096));
                    long sampled=json.getLong("elapsedRealtimeMs"),valid=json.getLong("validUntilElapsedMs");
                    if(json.getString("session").equals(owner.getString("session"))&&sampled>=owner.getLong("startElapsed")&&sampled<=now&&valid>now&&valid-sampled<=2000&&json.getLong("leaseGeneration")>0){stopReason="REPORT_REALTIME_PREEMPT";return false;}
                }
                if(new StatFs(getFilesDir().getPath()).getAvailableBytes()<32*MiB){stopReason="REPORT_DISK_BUDGET";return false;}
            }catch(Exception bad){stopR31("REPORT_CONTROL_READ_FAILED",bad);return false;}}
            return true;}
        void checkpoint()throws IOException{if(!shouldContinue()){InterruptedIOException failure=new InterruptedIOException(stopReason);if(stopCauseR31!=null)failure.initCause(stopCauseR31);throw failure;}}
    }

    /** SAF transfers only verified completed parts. The caller supplies one URI per part; every copy is reopened and hashed. */
    private static void transferPart(Context context,File completedPart,String expectedSha,long expectedBytes,Uri destination,ReportNativeCP4.Control control)throws Exception {
        Context app=context.getApplicationContext();ReportSafCopyCP12.copy(completedPart,expectedSha,expectedBytes,new ReportSafCopyCP12.Destination(){public OutputStream openOutputTruncating()throws IOException{return app.getContentResolver().openOutputStream(destination,"wt");}public InputStream openInput()throws IOException{return app.getContentResolver().openInputStream(destination);}},control);
    }

    private static boolean isRetryable(String reason){return reason.startsWith("REPORT_RESOURCE_WAIT|")||reason.equals("REPORT_STAGE_TIMEOUT")||reason.equals("REPORT_INPUT_LEASE_BUSY")||reason.equals("REPORT_ATTACHMENT_LEASE_BUSY")||reason.startsWith("REPORT_MEMORY_BUDGET_WAIT")||reason.equals("IOException");}

    public static String requestSafTransfer(Context context,String jobId,String volumeName,Uri destination){final Context app=context.getApplicationContext();final String transfer=id("SAF_TRANSFER");final long generation=USER_REQUEST_GENERATION.get();
        try{REQUESTS.execute(()->{try{requireHash(jobId);if(destination==null||!"content".equals(destination.getScheme())||destination.toString().length()>2048||volumeName==null||!(volumeName.equals("*")||volumeName.matches("STARCORE_FULL_REPORT\\.zip|STARCORE_FULL_REPORT\\.zip\\.part[0-9]{2,3}|index\\.json|P[0-9]{6}\\.bin|M[0-9]{6}\\.manifest")))throw new IOException("REPORT_SAF_REQUEST");if(volumeName.equals("*")&&!(destination.getPathSegments().size()==2&&"tree".equals(destination.getPathSegments().get(0))&&!android.provider.DocumentsContract.getTreeDocumentId(destination).isEmpty()))throw new IOException("REPORT_SAF_TREE_REQUIRED");JSONObject request=new JSONObject();request.put("requestId",transfer);request.put("mode","SAF_TRANSFER");request.put("jobId",jobId);request.put("volumeName",volumeName);request.put("destination",destination.toString());request.put("state","PENDING");request.put("attempt",0);File root=requestRoot(app);enqueueUserRequest(root,transfer,"SAF_TRANSFER",request,generation);kick(app,ACTION_EXPORT,transfer);}catch(Exception error){requestFailure(app,transfer,error);}});}catch(RejectedExecutionException full){requestFailure(app,transfer,full);}return transfer;
    }
    private void runSafRequest(JSONObject request,File root)throws Exception{
        if(request.getString("volumeName").equals("*")){runSafTreeRequest(request,root);return;}
        String transfer=request.getString("requestId");File journal=new File(root,"saf/"+transfer+".json");JSONObject state=journal.exists()?new JSONObject(ReportJobJournalCP12.readAtomic(journal,8192)):new JSONObject(request.toString());
        if(state.optString("state").equals("VERIFIED_READBACK")){ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());ReportRequestQueueCP12.remove(root,transfer);return;}
        if(state.optString("state").equals("FAILED")||state.optString("state").equals("CANCELLED")){ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());ReportRequestQueueCP12.remove(root,transfer);return;}
        ActiveControl active=null;
        try{
            state.put("state","COPYING");state.put("attempt",state.optInt("attempt",0)+1);ReportJobJournalCP12.writeAtomic(journal,state.toString());
            File sourceJob=ReportPublicationCP12.job(getFilesDir(),request.getString("jobId"));ReportJobJournalCP12.Job sourceState=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(sourceJob,"job.json"),65536));
            active=new ActiveControl(sourceState,root,transfer);control=active;foreground("报告分卷保存与回读核验中");active.enter("SAF_COPY",120000);
            try(ReportPublicationCP12.Lease pin=ReportPublicationCP12.shared(sourceJob)){
                JSONObject index=ReportPublicationCP12.delivery(sourceJob);String name=request.getString("volumeName"),path="",sha="";long bytes=0;
                if(name.equals("index.json")){path="delivery/index.json";File file=new File(sourceJob,path);bytes=file.length();sha=ReportArchiveCP4.hash(file,active);}else{JSONArray files=index.getJSONArray("files");for(int i=0;i<files.length();i++){JSONObject row=files.getJSONObject(i);if(row.getString("name").equals(name)){path=row.getString("path");sha=row.getString("sha256");bytes=row.getLong("bytes");break;}}}
                if(path.isEmpty())throw new IOException("REPORT_SAF_VOLUME_UNLISTED");state.put("expectedSha256",sha);state.put("expectedBytes",bytes);ReportJobJournalCP12.writeAtomic(journal,state.toString());
                transferPart(this,ReportArchiveVerifierCP12.inside(sourceJob,path),sha,bytes,Uri.parse(request.getString("destination")),active);
                active.checkpoint();
                state.put("state","VERIFIED_READBACK");state.put("actualSha256",sha);state.put("actualBytes",bytes);state.put("localReceiptDurable",true);state.put("destinationGuarantee","CLOSED_PROVIDER_STREAM_AND_EXACT_READBACK");state.put("verifiedWallMs",System.currentTimeMillis());
                ReportJobJournalCP12.writeAtomic(journal,state.toString());ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());ReportRequestQueueCP12.remove(root,transfer);
            }
        }catch(Exception error){String reason=active==null||active.stopReason.isEmpty()?classify(error):active.stopReason;state.put("reason",reason);recordSafStorageFailure(state,error,reason);
            boolean preempt=reason.equals("REPORT_REALTIME_PREEMPT")||reason.equals("REPORT_PROCESS_SHUTDOWN")||reason.equals("REPORT_WORKER_INTERRUPTED");if(preempt){state.put("attempt",Math.max(0,state.optInt("attempt",0)-1));state.put("preemptions",state.optInt("preemptions",0)+1);}
            boolean retry=(preempt||state.optInt("attempt",0)<3)&&state.optInt("preemptions",0)<=30&&!reason.equals("REPORT_USER_CANCEL")&&!reason.equals("REPORT_STORAGE_FULL");if(state.optInt("preemptions",0)>30)state.put("reason","REPORT_PREEMPTION_LIMIT");
            state.put("state",reason.equals("REPORT_USER_CANCEL")?"CANCELLED":retry?"PAUSED":"FAILED");ReportJobJournalCP12.writeAtomic(journal,state.toString());ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());if(retry)scheduleResume(transfer);else ReportRequestQueueCP12.remove(root,transfer);
        }finally{if(active!=null)active.releaseResourcesR31();control=null;}
    }

    /** One folder selection saves every verified volume; index is written last, never for a partial set. */
    private void runSafTreeRequest(JSONObject request,File root)throws Exception {
        String transfer=request.getString("requestId");File journal=new File(root,"saf/"+transfer+".json");
        JSONObject state=journal.exists()?new JSONObject(ReportJobJournalCP12.readAtomic(journal,65536)):new JSONObject(request.toString());
        String previous=state.optString("state");if(previous.equals("VERIFIED_READBACK")||previous.equals("FAILED")||previous.equals("CANCELLED")){ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());ReportRequestQueueCP12.remove(root,transfer);return;}
        ActiveControl active=null;
        try {
            state.put("state","COPYING");state.put("attempt",state.optInt("attempt",0)+1);ReportJobJournalCP12.writeAtomic(journal,state.toString());
            File sourceJob=ReportPublicationCP12.job(getFilesDir(),request.getString("jobId"));ReportJobJournalCP12.Job sourceState=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(sourceJob,"job.json"),65536));
            active=new ActiveControl(sourceState,root,transfer);control=active;foreground("正在保存完整报告全部分卷");active.enter("SAF_COPY",120000);
            try(ReportPublicationCP12.Lease pin=ReportPublicationCP12.shared(sourceJob)) {
                JSONObject index=ReportPublicationCP12.delivery(sourceJob);JSONArray volumes=index.getJSONArray("files");int total=volumes.length()+1;
                Uri tree=Uri.parse(request.getString("destination"));Uri directory;
                if(state.has("directoryUri"))directory=Uri.parse(state.getString("directoryUri"));
                else {Uri parent=android.provider.DocumentsContract.buildDocumentUriUsingTree(tree,android.provider.DocumentsContract.getTreeDocumentId(tree));
                    directory=android.provider.DocumentsContract.createDocument(getContentResolver(),parent,android.provider.DocumentsContract.Document.MIME_TYPE_DIR,"STARCORE-"+sourceState.jobId);
                    if(directory==null)throw new IOException("REPORT_SAF_DIRECTORY_CREATE");state.put("directoryUri",directory.toString());state.put("nextVolume",0);ReportJobJournalCP12.writeAtomic(journal,state.toString());}
                int next=state.optInt("nextVolume",0);if(next<0||next>total)throw new IOException("REPORT_SAF_CURSOR");
                while(next<total) {
                    active.enter("SAF_COPY",120000);String name,path,sha;long bytes;
                    if(next==volumes.length()){name="index.json";path="delivery/index.json";File file=new File(sourceJob,path);bytes=file.length();sha=ReportArchiveCP4.hash(file,active);}
                    else {JSONObject row=volumes.getJSONObject(next);name=row.getString("name");path=row.getString("path");sha=row.getString("sha256");bytes=row.getLong("bytes");}
                    Uri destination;
                    if(state.has("currentUri"))destination=Uri.parse(state.getString("currentUri"));
                    else {destination=android.provider.DocumentsContract.createDocument(getContentResolver(),directory,"application/octet-stream",name);if(destination==null)throw new IOException("REPORT_SAF_DOCUMENT_CREATE");state.put("currentUri",destination.toString());ReportJobJournalCP12.writeAtomic(journal,state.toString());}
                    if(next==volumes.length())verifySavedTreeParts(root,transfer,next,sourceJob,index,active);
                    transferPart(this,ReportArchiveVerifierCP12.inside(sourceJob,path),sha,bytes,destination,active);
                    JSONObject saved=new JSONObject();saved.put("uri",destination.toString());saved.put("sha256",sha);saved.put("bytes",bytes);saved.put("name",name);ReportJobJournalCP12.writeAtomic(new File(root,"saf-parts/"+transfer+"/"+next+".json"),saved.toString());
                    next++;state.put("nextVolume",next);state.remove("currentUri");state.put("verifiedVolumes",next);ReportJobJournalCP12.writeAtomic(journal,state.toString());
                    // A kick after the final cursor write can finish without recopying any part.
                }
                verifySavedTreeParts(root,transfer,total,sourceJob,index,active);active.checkpoint();
                state.put("state","VERIFIED_READBACK");state.put("allVolumesSaved",true);state.put("volumeCount",total);state.put("manifestSha256",index.getString("manifestSha256"));state.put("verifiedWallMs",System.currentTimeMillis());
                ReportJobJournalCP12.writeAtomic(journal,state.toString());ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());ReportRequestQueueCP12.remove(root,transfer);
            }
        }catch(Exception error){String reason=active==null||active.stopReason.isEmpty()?classify(error):active.stopReason;state.put("reason",reason);recordSafStorageFailure(state,error,reason);boolean preempt=reason.equals("REPORT_REALTIME_PREEMPT")||reason.equals("REPORT_PROCESS_SHUTDOWN")||reason.equals("REPORT_WORKER_INTERRUPTED");
            if(preempt){state.put("attempt",Math.max(0,state.optInt("attempt",0)-1));state.put("preemptions",state.optInt("preemptions",0)+1);}boolean retry=(preempt||state.optInt("attempt",0)<3)&&state.optInt("preemptions",0)<=30&&!reason.equals("REPORT_USER_CANCEL")&&!reason.equals("REPORT_STORAGE_FULL");
            state.put("state",reason.equals("REPORT_USER_CANCEL")?"CANCELLED":retry?"PAUSED":"FAILED");state.put("allVolumesSaved",false);ReportJobJournalCP12.writeAtomic(journal,state.toString());ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),state.toString());if(retry)scheduleResume(transfer);else ReportRequestQueueCP12.remove(root,transfer);
        }finally{if(active!=null)active.releaseResourcesR31();control=null;}
    }
    private void verifySavedTreeParts(File root,String transfer,int count,File sourceJob,JSONObject index,ActiveControl active)throws Exception {
        JSONArray volumes=index.getJSONArray("files");
        if(volumes.length()<1||volumes.length()>128||count<0||count>volumes.length()+1)throw new IOException("REPORT_SAF_RECEIPT_SET_BOUND");
        Set<String> destinations=new HashSet<>();
        for(int i=0;i<count;i++) {
            active.enter("SAF_VERIFY_SET",120000);
            JSONObject saved=new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"saf-parts/"+transfer+"/"+i+".json"),8192));
            Uri uri=Uri.parse(saved.getString("uri"));
            if(!"content".equals(uri.getScheme())||!destinations.add(uri.toString()))throw new IOException("REPORT_SAF_RECEIPT_DESTINATION");
            String expectedName,expectedSha;long expectedBytes;
            if(i==volumes.length()){
                File deliveryIndex=ReportArchiveVerifierCP12.inside(sourceJob,"delivery/index.json");
                expectedName="index.json";expectedBytes=deliveryIndex.length();expectedSha=ReportArchiveCP4.hash(deliveryIndex,active);
            }else{
                JSONObject expected=volumes.getJSONObject(i);expectedName=expected.getString("name");expectedSha=expected.getString("sha256");expectedBytes=expected.getLong("bytes");
            }
            ReportSafCopyCP12.verifySavedReceipt(saved,expectedName,expectedSha,expectedBytes,new ReportSafCopyCP12.Destination(){
                public OutputStream openOutputTruncating()throws IOException{throw new IOException("REPORT_VERIFY_READ_ONLY");}
                public InputStream openInput()throws IOException{return getContentResolver().openInputStream(uri);}
            },active);
        }
    }

    /** API-21 safe read of the previous R27 report summary (java.nio.file needs API 26). */
    private static String readR27Summary(File f)throws java.io.IOException{return ReportJobJournalCP12.readAtomic(f,1024*1024);}
}
