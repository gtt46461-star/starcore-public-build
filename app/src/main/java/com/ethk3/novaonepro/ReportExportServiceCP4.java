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

    /** May be called on UI. Heavy observation/file work stays on the request executor; no Activity is captured. */
    public static String requestExport(Context context,ExportMode mode) {
        return requestExport(context,mode,false);
    }
    public static String requestExport(Context context,ExportMode mode,boolean includeRawEvidence) {
        if(mode!=ExportMode.FULL_REPORT)throw new IllegalArgumentException("REPORT_MODE");
        final Context app=context.getApplicationContext();final String id=id("FULL_REPORT");final long generation=USER_REQUEST_GENERATION.get();
        try{REQUESTS.execute(()->{try{
            JSONObject request=new JSONObject();request.put("revision","REPORT_REQUEST_CP12_1");request.put("requestId",id);request.put("mode","FULL_REPORT");
            request.put("releaseHash",ReleaseManifestR926.MANIFEST_SHA256);request.put("createdWallMs",System.currentTimeMillis());
            request.put("includeRawEvidence",includeRawEvidence);
            File root=requestRoot(app);JSONObject observation=new JSONObject();observation.put("pid",android.os.Process.myPid());observation.put("sampleWallMs",System.currentTimeMillis());observation.put("sampleElapsedMs",SystemClock.elapsedRealtime());
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

            ReportJobJournalCP12.collectObservation(root,id,observation,"crashSummary",()->CrashRecorder.detailedSummary(app));ReportJobJournalCP12.collectObservation(root,id,observation,"heartbeatSummary",()->new ServiceHeartbeatStore(app).detailedSummary());
            DatabaseCoordinator coordinator=DatabaseCoordinator.peek();if(coordinator!=null){observation.put("dbWaitMs",coordinator.lastDbLockWaitMs());observation.put("dbWaitMaxMs",coordinator.longestDbLockWaitMs());observation.put("researchSchemaReady",coordinator.isR811ResearchSchemaReady());ReportJobJournalCP12.collectObservation(root,id,observation,"schemaStartupSummary",()->coordinator.startupSchemaSummary());}observation.put("realtimePressure",RealtimePressureGateR811.isUnderPressure());ReportJobJournalCP12.collectObservation(root,id,observation,"fetchWriteError",()->FetchLedgerR925.reportWriteErrorObservationCP12());
            RealtimePressureGateR811.Snapshot pressure=RealtimePressureGateR811.snapshot();
            observation.put("analysisDepth",pressure.analysisDepth).put("settlementDepth",pressure.settlementDepth)
                    .put("quietForMs",pressure.quietForMs).put("settlementExecutionActive",SettlementExecutionGateR819.isActive());
            request.put("observation",observation);
            enqueueUserRequest(root,id,"FULL_REPORT",request,generation);kick(app,ACTION_EXPORT,id);
        }catch(Throwable error){requestFailure(app,id,error);}});}catch(RejectedExecutionException full){requestFailure(app,id,new IOException("REPORT_REQUEST_QUEUE_FULL"));}
        return id;
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
    public static void resumeExport(Context context,String jobId)throws IOException {
        requireHash(jobId);
        kick(context.getApplicationContext(),ACTION_RESUME,jobId);
    }
    private static void kick(Context app,String action,String id){Intent intent=new Intent(app,ReportExportServiceCP4.class).setAction(action).putExtra("id",id);
        if(Build.VERSION.SDK_INT>=26)app.startForegroundService(intent);else app.startService(intent);}
    private static File requestRoot(Context app)throws IOException{File root=new File(app.getFilesDir(),"r981_reports");if(!root.isDirectory()&&!root.mkdirs())throw new IOException("REPORT_ROOT_CREATE");return root;}
    private static void requestFailure(Context app,String id,Throwable error){try{JSONObject failure=new JSONObject();failure.put("requestId",id);failure.put("stage","FAILED");failure.put("reason",boundedReason(error.getClass().getSimpleName()+":"+String.valueOf(error.getMessage())));File root=requestRoot(app);try{failure.put("recovery",ReportJobJournalCP12.writeFailureArtifact(root,id,error));}catch(Exception unavailable){failure.put("recoveryFailure",boundedReason(unavailable.getClass().getSimpleName()+":"+unavailable.getMessage()));}ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".ack.json"),failure.toString());}catch(Exception ignored){/* No success acknowledgement is generated. */}}
    private static String id(String mode){return RetentionArchiveProtocolCP12.sha(("REPORT_CP12|"+ReleaseManifestR926.MANIFEST_SHA256+"|"+mode+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));}
    private static void requireHash(String value)throws IOException{if(value==null||!value.matches("[0-9a-f]{64}"))throw new IOException("REPORT_HASH");}

    @Override public void onCreate(){super.onCreate();worker=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(1),r->new Thread(()->{android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);r.run();},"report-owner-cp12"));
        controlTimer=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"report-control-cp12"));
        controlTimer.scheduleWithFixedDelay(()->{ActiveControl active=control;if(active!=null&&!active.shouldContinue()&&control==active){ReportCatalogCP4.cancelActive();}},250,250,TimeUnit.MILLISECONDS);}
    @Override public int onStartCommand(Intent intent,int flags,int startId){lastStartId=startId;foreground("报告等待处理");
        if(intent!=null&&ACTION_CANCEL.equals(intent.getAction())){String key=intent.getStringExtra("id");if(key!=null&&key.matches("[0-9a-f]{64}")){ActiveControl active=control;if(active!=null&&active.cancelKey.equals(key))active.cancel.set(true);try{REQUESTS.execute(()->{try{ReportJobJournalCP12.requestCancellation(requestRoot(getApplicationContext()),key);}catch(Exception error){android.util.Log.e("ReportExportCP12","cancel command persistence failed",error);}});}catch(RejectedExecutionException full){android.util.Log.w("ReportExportCP12","active task cancel is set; redelivered intent will persist after restart",full);}}}
        if(drainQueued.compareAndSet(false,true)){try{workerFuture=worker.submit(this::drain);}catch(RejectedExecutionException full){drainQueued.set(false);}}
        return START_REDELIVER_INTENT;}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onDestroy(){destroyed.set(true);ActiveControl active=control;if(active!=null)active.stopReason="REPORT_PROCESS_SHUTDOWN";ReportCatalogCP4.cancelActive();Future<?> future=workerFuture;if(future!=null)future.cancel(true);worker.shutdownNow();controlTimer.shutdownNow();super.onDestroy();}
    private void foreground(String text){NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"STARCORE 检测导出",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);
        ActiveControl active=control;if(active!=null){Intent cancel=new Intent(this,ReportExportServiceCP4.class).setAction(ACTION_CANCEL).putExtra("id",active.cancelKey);int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;PendingIntent command=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,NOTIFICATION+1,cancel,flags):PendingIntent.getService(this,NOTIFICATION+1,cancel,flags);builder.addAction(android.R.drawable.ic_menu_close_clear_cancel,"取消报告任务",command);}
        startForeground(NOTIFICATION,builder.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("STARCORE 检测报告").setContentText(text).setOngoing(true).build());}

    private void drain(){
        final int ownerStartId=lastStartId;
        String currentRequest="";
        try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(getFilesDir())) {
            if(journal==null)return;String queued=ReportRequestQueueCP12.head(journal.root());if(queued!=null)currentRequest=queued;
            ReportTaskControlR69.applyPending(journal);
            ReportPublicationCP12.pruneRequestMetadata(journal.root(),System.currentTimeMillis());
            ReportJobJournalCP12.Job job=journal.current();if(job!=null&&isActive(job))currentRequest=job.requestId;
            if(job!=null&&isActive(job)&&job.mode.equals("RETENTION_PAGE")&&ReportRequestQueueCP12.interactiveHead(journal.root())!=null){
                journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());
                ReportRequestQueueCP12.enqueue(journal.root(),job.requestId,"RETENTION_PAGE");job=null;
            }
            requireDiagnosticProcess();ReportNativeCP4.loadForDiagnosticProcess(processName(),getPackageName());JSONObject request=null;
            if(job!=null&&isActive(job)){currentRequest=job.requestId;request=loadRequest(journal.root(),job.requestId);}
            else {request=nextRequest(journal.root());if(request==null)return;
                String requestId=request.getString("requestId"),mode=request.getString("mode");requireHash(requestId);
                if(mode.equals("SAF_TRANSFER")){runSafRequest(request,journal.root());return;}
                if(job!=null&&(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY"))&&job.requestId.equals(requestId)){ActiveControl ready=new ActiveControl(job,journal.root());verifyReady(job,journal.directory(job),ready);journal.acknowledge(requestId,job);clearPending(journal.root(),request);return;}
                job=new ReportJobJournalCP12.Job(id(mode),requestId,request.getString("releaseHash"),mode,System.currentTimeMillis());journal.create(job);ReportPublicationCP12.pruneTerminal(journal.root(),job.jobId);clearPending(journal.root(),request);}
            journal.acknowledge(job.requestId,job);ActiveControl active=new ActiveControl(job,journal.root());control=active;foreground("报告采集与核验进行中");
            try(ReportInputsLeaseCP12 inputPin=job.mode.equals("FULL_REPORT")?ReportInputsLeaseCP12.acquire(getFilesDir()):null) {
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
                    job.evidenceComplete=true;job.checksComplete=false;job.allChecksPassed=false;
                    journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_RECEIPT_DURABLE,System.currentTimeMillis());
                    journal.transition(job,ReportJobJournalCP12.Stage.RETENTION_READY,System.currentTimeMillis());journal.acknowledge(job.requestId,job);return;
                }
                RetentionSnapshotCacheR73.discard(journal); // release optional cleanup copy before reserving full-report space
                reserveDisk(job,journal);
                boolean snapshotValid=validSnapshot(job,snapshot,active);
                if(snapshotValid&&job.mode.equals("FULL_REPORT")&&!ReportAttachmentClosureCP12.isComplete(directory,job.snapshotSha,active))snapshotValid=false;
                if(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY")){verifyReady(job,directory,active);journal.acknowledge(job.requestId,job);return;}
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
                    ReportNativeCP4.backup(getDatabasePath(AppConfig.DB_NAME),snapshot,SQLITE_BACKUP_BUDGET,active);
                    active.enter("SNAPSHOT_HASH",120000);
                    sync(snapshot);job.snapshotSha=ReportArchiveCP4.hash(snapshot,active);job.snapshotBytes=snapshot.length();
                    journal.transition(job,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,System.currentTimeMillis());
                }
                active.enter("INDEXING",120000);journal.transition(job,ReportJobJournalCP12.Stage.INDEXING,System.currentTimeMillis());
                File memberInput=new File(directory,"members-unsorted-"+job.attempt+".jsonl");ReportCatalogCP4.Result catalog;
                try(FileOutputStream fd=new FileOutputStream(memberInput);Writer members=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),8192);
                    SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                    if(!copy.isReadOnly())throw new IOException("REPORT_READONLY_SNAPSHOT_REQUIRED");
                    if(ReportAttachmentClosureCP12.isComplete(directory,job.snapshotSha,active))ReportAttachmentClosureCP12.appendComplete(directory,members,active);
                    else ReportAttachmentClosureCP12.capture(this,copy,directory,request,active,members,job.snapshotSha,job.snapshotBytes);
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
                    File census=new File(directory,"STORAGE_CENSUS.txt");try(java.io.Writer w=new java.io.OutputStreamWriter(new java.io.FileOutputStream(census),StandardCharsets.UTF_8)){w.write(StorageCensusSU2.measure(getFilesDir().getParentFile()).line());w.write('\n');}
                    ReportMemberPlanCP12.append(members,census.getName(),census.getName(),ReportArchiveCP4.hash(census,active),census.length());
                    ReportMemberPlanCP12.append(members,inventory.getName(),inventory.getName(),ReportArchiveCP4.hash(inventory,active),inventory.length());
                    for(File page:catalog.indexFiles)ReportMemberPlanCP12.append(members,page.getName(),"catalog/"+page.getName(),ReportArchiveCP4.hash(page,active),page.length());
                    File renderRefs=new File(directory,"render-artifacts.jsonl");if(renderRefs.exists())try(ReportArchiveVerifierCP12.LineReader refs=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(renderRefs),8192)){byte[] line;while((line=refs.next())!=null){active.checkpoint();members.write(new String(line,StandardCharsets.UTF_8));}}
                    members.flush();fd.getFD().sync();
                }
                job.evidenceComplete=catalog.evidenceCatalogComplete;job.checksComplete=catalog.checksComplete;
                job.allChecksPassed=catalog.allChecksPassed;
                journal.transition(job,ReportJobJournalCP12.Stage.INDEX_DURABLE,System.currentTimeMillis());
                // Android snapshot connection is closed before the next native allocation stage.
                File sorted=ReportMemberPlanCP12.sort(directory,memberInput,active);job.compressionAttempt=Math.addExact(job.compressionAttempt,1);
                File archive=new File(directory,"archive-"+job.compressionAttempt);if(!archive.mkdir())throw new IOException("REPORT_ARCHIVE_EXCLUSIVE_DIRECTORY");job.publishedDir=archive.getName();
                active.enter("COMPRESSING",120000);journal.transition(job,ReportJobJournalCP12.Stage.COMPRESSING,System.currentTimeMillis());
                try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,job.jobId);ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192)) {
                    byte[] line;while((line=rows.next())!=null){active.checkpoint();JSONObject member=new JSONObject(new String(line,StandardCharsets.UTF_8));File input=ReportArchiveVerifierCP12.inside(directory,member.getString("path"));
                        if(input.length()!=member.getLong("bytes")||!member.getString("sha256").equals(ReportArchiveCP4.hash(input,active)))throw new IOException("REPORT_MEMBER_CHANGED");
                        writer.add(member.getString("name"),input,NATIVE_BUDGET,active);}
                    writer.finish(job.evidenceComplete,job.checksComplete,request.optBoolean("includeRawEvidence",false)?"FULL_REPORT_SINGLE_SNAPSHOT_AND_FROZEN_ATTACHMENTS":"COMPLETE_DIAGNOSTIC_R64_RAW_PAYLOADS_EXCLUDED");
                }
                journal.transition(job,ReportJobJournalCP12.Stage.VERIFYING,System.currentTimeMillis());publish(job,journal,directory,archive,active);journal.acknowledge(job.requestId,job);
            } catch(Throwable error) {
                Thread.interrupted();String reason=active.stopReason.isEmpty()?classify(error):active.stopReason;job.lastReason=reason;
                if(reason.equals("REPORT_USER_CANCEL"))journal.transition(job,ReportJobJournalCP12.Stage.CANCELLED,System.currentTimeMillis());
                else if(reason.equals("REPORT_REALTIME_PREEMPT")||reason.equals("REPORT_PROCESS_SHUTDOWN")||reason.equals("REPORT_WORKER_INTERRUPTED")){job.preemptions++;job.resumeStage=job.snapshotSha.isEmpty()?"BACKUP":"INDEXING";if(job.preemptions>30){job.lastReason="REPORT_PREEMPTION_LIMIT";journal.transition(job,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());}else{journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());scheduleResume(job.jobId);}}
                else if(isRetryable(reason)&&++job.failureAttempts<3){job.resumeStage=job.snapshotSha.isEmpty()?"BACKUP":"INDEXING";journal.transition(job,ReportJobJournalCP12.Stage.PAUSED,System.currentTimeMillis());scheduleResume(job.jobId);}
                else {journal.transition(job,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());}
                if(job.stage.equals("FAILED")&&job.mode.equals("FULL_REPORT"))try{ReportJobJournalCP12.writeFailureArtifact(journal.root(),job.requestId,error,job);}catch(Exception unavailable){android.util.Log.e("ReportExportCP12","recovery evidence unavailable",unavailable);}
                journal.acknowledge(job.requestId,job);
            } finally {control=null;}
        }catch(Throwable error){if(!currentRequest.isEmpty()){
            requestFailure(getApplicationContext(),currentRequest,error);
            try(ReportJobJournalCP12 recovery=ReportJobJournalCP12.tryClaim(getFilesDir())){if(recovery!=null){
                ReportJobJournalCP12.Job failed=recovery.current();
                if(failed!=null&&failed.requestId.equals(currentRequest)&&isActive(failed)){failed.lastReason=classify(error);recovery.transition(failed,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());recovery.acknowledge(currentRequest,failed);}
                ReportRequestQueueCP12.remove(recovery.root(),currentRequest);
            }}catch(Exception recoveryFailure){android.util.Log.e("ReportExportCP12","report failure receipt recovery failed",recoveryFailure);}
        }android.util.Log.e("ReportExportCP12","report owner failed",error);}
        finally{drainQueued.set(false);main.post(()->{if(lastStartId==ownerStartId){stopForeground(false);stopSelfResult(ownerStartId);}});if(!destroyed.get()){try{File root=requestRoot(this);if(ReportRequestQueueCP12.head(root)!=null||ReportTaskControlR69.hasPending(root))scheduleResume("");}catch(Exception ignored){}}}
    }
    private void reserveDisk(ReportJobJournalCP12.Job job,ReportJobJournalCP12 journal)throws Exception {
        File source=getDatabasePath(AppConfig.DB_NAME);long raw=Math.addExact(source.length(),new File(source.getPath()+"-wal").length());
        long available=new StatFs(getFilesDir().getPath()).getAvailableBytes();job.temporaryReservationBytes=Math.addExact(Math.multiplyExact(2L,raw),64*MiB);job.publishedQuotaBytes=ReportBudgetSU2.PUBLISHED_QUOTA_BYTES;
        if(available<job.temporaryReservationBytes)throw new IOException("REPORT_DISK_BUDGET");journal.checkpoint(job,System.currentTimeMillis());
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
        ReportPublicationCP12.prepareSingleZip(directory,archive,job.jobId,job.manifestSha,job.evidenceComplete,job.checksComplete,job.allChecksPassed,active);
        pointer.put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(directory,"delivery/index.json"),active));
        ReportPublicationCP12.pruneOldReports(journal.root(),directory,System.currentTimeMillis());
        ReportPublicationCP12.enforceQuota(journal.root(),directory,job.publishedQuotaBytes);   // R13-P34: the quota existed but was never called
        ReportJobJournalCP12.writeAtomic(new File(directory,"published.json"),pointer.toString());
        journal.transition(job,ReportJobJournalCP12.Stage.READY_TO_SHARE,System.currentTimeMillis());
        // The sealed verified ZIP is now the durable delivery. Remove redundant private parts only
        // after its hash and publication identity have been checked; legacy multipart stays intact.
        try{ReportPublicationCP12.cleanRaw(directory,job.publishedDir);}catch(IOException cleanup){job.lastReason="REPORT_RAW_CLEANUP_PENDING";journal.checkpoint(job,System.currentTimeMillis());android.util.Log.w("ReportExportCP12","Published evidence remains valid; private raw cleanup will retry",cleanup);}
    }
    private void verifyReady(ReportJobJournalCP12.Job job,File directory,ActiveControl active)throws Exception {
        if(job.mode.equals("RETENTION_PAGE")){RetentionArchiveProtocolCP12.Receipt receipt=RetentionArchiveIoCP12.readReceipt(new File(getFilesDir(),"retention-replies-cp12/"+job.requestId+".receipt"));if(!receipt.page.requestId.equals(job.requestId))throw new IOException("RETENTION_REPLY_IDENTITY");return;}
        active.enter("VERIFYING",120000);JSONObject pointer=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"published.json"),4096));
        if(!pointer.getString("jobId").equals(job.jobId)||!pointer.getString("relativeDir").equals(job.publishedDir))throw new IOException("REPORT_PUBLISH_IDENTITY");
        if(ReportPublicationCP12.verifyPublishedZip(directory,active))return;
        ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(ReportArchiveVerifierCP12.inside(directory,job.publishedDir),job.jobId,ReportArchiveVerifierCP12.nativeDecoder(NATIVE_BUDGET),active,null);
        if(!proof.manifestSha256.equals(job.manifestSha)||!proof.manifestSha256.equals(pointer.getString("manifestSha256")))throw new IOException("REPORT_PUBLISH_HASH");ReportPublicationCP12.delivery(directory);
    }
    private static boolean isActive(ReportJobJournalCP12.Job job){return !(job.stage.equals("READY_TO_SHARE")||job.stage.equals("RETENTION_READY")||job.stage.equals("CANCELLED")||job.stage.equals("FAILED"));}
    private static JSONObject loadRequest(File root,String id)throws Exception{return new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"requests/"+id+".json"),65536));}
    private static JSONObject nextRequest(File root)throws Exception{String id=ReportRequestQueueCP12.interactiveHead(root);if(id==null)id=ReportRequestQueueCP12.head(root);return id==null?null:loadRequest(root,id);}
    private static void clearPending(File root,JSONObject request)throws Exception{ReportRequestQueueCP12.remove(root,request.getString("requestId"));}
    private void scheduleResume(String id){
        // OS alarm survives stopSelf/process death; user cancel remains a separate durable command.
        Intent intent=new Intent(getApplicationContext(),ReportExportServiceCP4.class).setAction(ACTION_RESUME).putExtra("id",id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT;if(Build.VERSION.SDK_INT>=23)flags|=PendingIntent.FLAG_IMMUTABLE;
        PendingIntent callback=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(this,NOTIFICATION,intent,flags):PendingIntent.getService(this,NOTIFICATION,intent,flags);
        AlarmManager alarms=(AlarmManager)getSystemService(ALARM_SERVICE);if(alarms==null)throw new IllegalStateException("REPORT_RESUME_ALARM_UNAVAILABLE");
        long due=SystemClock.elapsedRealtime()+1000;
        if(Build.VERSION.SDK_INT>=23)alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,due,callback);else alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,due,callback);
    }
    private static void deleteFile(File file)throws IOException{if(file.exists()&&!file.delete())throw new IOException("REPORT_TEMP_CLEANUP");}
    private static void sync(File file)throws IOException{try(RandomAccessFile io=new RandomAccessFile(file,"rw")){io.getFD().sync();}}
    private static String boundedReason(String value){if(value==null)return "REPORT_UNCLASSIFIED_FAILURE";return value.length()<=512?value:value.substring(0,512);}
    private static boolean storageFull(Throwable error){for(int depth=0;error!=null&&depth<16;depth++,error=error.getCause())if(error instanceof android.system.ErrnoException&&((android.system.ErrnoException)error).errno==android.system.OsConstants.ENOSPC)return true;return false;}
    private static String classify(Throwable error){if(storageFull(error))return "REPORT_STORAGE_FULL";if(error instanceof ReportNativeCP4.Failure){ReportNativeCP4.Failure failure=(ReportNativeCP4.Failure)error;return boundedReason("REPORT_NATIVE_"+failure.stage+"_"+failure.nativeCode);}String text=error.getMessage();return boundedReason(text!=null&&(text.startsWith("REPORT_")||text.startsWith("RETENTION_")||text.startsWith("ARCHIVE_")||text.equals("STALE_PAGE"))?text:error.getClass().getSimpleName());}
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
        final ReportJobJournalCP12.Job job;final File root;final String cancelKey;final AtomicBoolean cancel=new AtomicBoolean();volatile String stopReason="";volatile long deadline=Long.MAX_VALUE,lastRead;private long stageStarted,lastProgressWrite;
        ActiveControl(ReportJobJournalCP12.Job job,File root){this(job,root,job.jobId);}
        ActiveControl(ReportJobJournalCP12.Job job,File root,String cancelKey){this.job=job;this.root=root;this.cancelKey=cancelKey;}
        void enter(String stage,long milliseconds){stopReason="";stageStarted=SystemClock.elapsedRealtime();lastProgressWrite=0;deadline=ReportBudgetSU2.deadline(stageStarted,milliseconds,job.mode.equals("FULL_REPORT"));
            try{ReportJobJournalCP12.writeAtomic(new File(new File(root,"jobs/"+job.jobId),"stage-progress.json"),new JSONObject().put("stage",stage).put("startedWallMs",System.currentTimeMillis()).toString());}
            catch(Exception error){stopReason="REPORT_PROGRESS_WRITE_FAILED";}}
        @Override public synchronized void onBackupProgress(long totalPages,long remainingPages,int nativeCode){
            long now=SystemClock.elapsedRealtime();if(nativeCode!=101&&lastProgressWrite>0&&now-lastProgressWrite<1000)return;
            try{JSONObject progress=new JSONObject();progress.put("stage","BACKUP");progress.put("totalPages",totalPages);progress.put("copiedPages",Math.max(0L,totalPages-remainingPages));progress.put("elapsedMs",Math.max(0L,now-stageStarted));progress.put("nativeCode",nativeCode);progress.put("sampledElapsedMs",now);
                ReportJobJournalCP12.writeAtomic(new File(new File(root,"jobs/"+job.jobId),"backup-progress.json"),progress.toString());lastProgressWrite=now;
            }catch(Exception error){stopReason="REPORT_PROGRESS_WRITE_FAILED";}
        }
        public synchronized boolean shouldContinue(){if(!stopReason.isEmpty())return false;if(cancel.get()){stopReason="REPORT_USER_CANCEL";return false;}if(destroyed.get()){stopReason="REPORT_PROCESS_SHUTDOWN";return false;}if(Thread.currentThread().isInterrupted()){stopReason="REPORT_WORKER_INTERRUPTED";return false;}
            long now=SystemClock.elapsedRealtime();if(now>=deadline){stopReason="REPORT_STAGE_TIMEOUT";return false;}
            if(now-lastRead>=250){lastRead=now;try{
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
            }catch(Exception bad){stopReason="REPORT_CONTROL_READ_FAILED";return false;}}
            return true;}
        void checkpoint()throws IOException{if(!shouldContinue())throw new InterruptedIOException(stopReason);}
    }

    /** SAF transfers only verified completed parts. The caller supplies one URI per part; every copy is reopened and hashed. */
    private static void transferPart(Context context,File completedPart,String expectedSha,long expectedBytes,Uri destination,ReportNativeCP4.Control control)throws Exception {
        Context app=context.getApplicationContext();ReportSafCopyCP12.copy(completedPart,expectedSha,expectedBytes,new ReportSafCopyCP12.Destination(){public OutputStream openOutputTruncating()throws IOException{return app.getContentResolver().openOutputStream(destination,"wt");}public InputStream openInput()throws IOException{return app.getContentResolver().openInputStream(destination);}},control);
    }

    private static boolean isRetryable(String reason){return reason.equals("REPORT_STAGE_TIMEOUT")||reason.equals("REPORT_INPUT_LEASE_BUSY")||reason.equals("REPORT_ATTACHMENT_LEASE_BUSY")||reason.equals("IOException");}

    public static String requestSafTransfer(Context context,String jobId,String volumeName,Uri destination){final Context app=context.getApplicationContext();final String transfer=id("SAF_TRANSFER");final long generation=USER_REQUEST_GENERATION.get();
        try{REQUESTS.execute(()->{try{requireHash(jobId);if(destination==null||!"content".equals(destination.getScheme())||destination.toString().length()>2048||volumeName==null||!(volumeName.equals("*")||volumeName.matches("STARCORE_FULL_REPORT\\.zip|index\\.json|P[0-9]{6}\\.bin|M[0-9]{6}\\.manifest")))throw new IOException("REPORT_SAF_REQUEST");if(volumeName.equals("*")&&!(destination.getPathSegments().size()==2&&"tree".equals(destination.getPathSegments().get(0))&&!android.provider.DocumentsContract.getTreeDocumentId(destination).isEmpty()))throw new IOException("REPORT_SAF_TREE_REQUIRED");JSONObject request=new JSONObject();request.put("requestId",transfer);request.put("mode","SAF_TRANSFER");request.put("jobId",jobId);request.put("volumeName",volumeName);request.put("destination",destination.toString());request.put("state","PENDING");request.put("attempt",0);File root=requestRoot(app);enqueueUserRequest(root,transfer,"SAF_TRANSFER",request,generation);kick(app,ACTION_EXPORT,transfer);}catch(Exception error){requestFailure(app,transfer,error);}});}catch(RejectedExecutionException full){requestFailure(app,transfer,full);}return transfer;
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
        }finally{control=null;}
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
        }finally{control=null;}
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

}
