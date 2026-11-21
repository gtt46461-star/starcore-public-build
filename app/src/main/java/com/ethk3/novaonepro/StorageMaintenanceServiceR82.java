package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Explicit offline maintenance. Never schedules or restarts live prediction. */
public final class StorageMaintenanceServiceR82 extends Service {
    public static final String ACTION_REBUILD="com.ethk3.novaonepro.R82_REBUILD_DATABASE";
    public static final String ACTION_CLEAN_BACKUP="com.ethk3.novaonepro.R82_DELETE_VERIFIED_BACKUP";
    public static final String ACTION_VERIFY_LEGACY_R335="com.ethk3.novaonepro.R335_VERIFY_LEGACY_BACKUP";
    public static void requestLegacyBackupProofR335(Context context,String fileName){
        LegacyBackupRecoveryR335.requireName(fileName);
        requestActionR335(context,ACTION_VERIFY_LEGACY_R335,fileName);
    }
    public static final String ACTION_CUSTODY_EXPORT_R335="com.ethk3.novaonepro.R335_CUSTODY_EXPORT";
    public static final String ACTION_CUSTODY_VERIFY_R335="com.ethk3.novaonepro.R335_CUSTODY_VERIFY";
    public static final String ACTION_CUSTODY_RECONCILE_R335="com.ethk3.novaonepro.R335_CUSTODY_RECONCILE";
    public static final String ACTION_CUSTODY_RELEASE_R335="com.ethk3.novaonepro.R335_CUSTODY_RELEASE";
    public static final String ACTION_CUSTODY_IMPORT_R335="com.ethk3.novaonepro.R335_CUSTODY_IMPORT";
    public static final String ACTION_CUSTODY_RETIRE_R335="com.ethk3.novaonepro.R335_CUSTODY_RETIRE";
    public static void requestCustodyReconcileR335(Context context,String id){requestActionR335(context,ACTION_CUSTODY_RECONCILE_R335,null,null,id,null);}
    public static void requestCustodyReleaseR335(Context context,String id){requestActionR335(context,ACTION_CUSTODY_RELEASE_R335,null,null,id,null);}
    public static void requestCustodyImportR335(Context context,android.net.Uri document){requestActionR335(context,ACTION_CUSTODY_IMPORT_R335,null,document==null?null:document.toString(),null,null);}
    public static void requestCustodyExportR335(Context context,String name,android.net.Uri tree){LegacyBackupRecoveryR335.requireName(name);requestActionR335(context,ACTION_CUSTODY_EXPORT_R335,name,tree==null?null:tree.toString(),null,null);}
    public static void requestCustodyVerifyR335(Context context,String id){requestActionR335(context,ACTION_CUSTODY_VERIFY_R335,null,null,id,null);}
    public static void requestCustodyRetireR335(Context context,String id,String token){requestActionR335(context,ACTION_CUSTODY_RETIRE_R335,null,null,id,token);}
    private static final String PREF="r82_storage_maintenance";
    private static final AtomicBoolean WORKER=new AtomicBoolean();
    public static void request(Context context,boolean cleanup) {
        requestActionR335(context,cleanup?ACTION_CLEAN_BACKUP:ACTION_REBUILD,null);
    }
    private static void requestActionR335(Context context,String action,String legacyName){requestActionR335(context,action,legacyName,null,null,null);}
    private static void requestActionR335(Context context,String action,String legacyName,String tree,String caseId,String token){
        Context app=context.getApplicationContext();
        synchronized(DatabaseCoordinator.RUNTIME_TRANSITION_R82) {
            new ServiceHeartbeatStore(app).setUserEnabled(false);
            DatabaseCoordinator.beginExplicitMaintenanceR82(app);
        }
        try{
            app.stopService(new Intent(app,AutoFetchService.class));
            Intent intent=new Intent(app,StorageMaintenanceServiceR82.class).setAction(action).putExtra("legacy_backup_name",legacyName).putExtra("custody_tree",tree).putExtra("custody_case",caseId).putExtra("custody_authorization",token);
            if(Build.VERSION.SDK_INT>=26)app.startForegroundService(intent);else app.startService(intent);
        }catch(RuntimeException failure){DatabaseCoordinator.endExplicitMaintenanceR82();record(app,"START_FAILED",failure.toString());throw failure;}
    }
    // Durable phase receipt is used for interrupted maintenance recovery; apply() cannot acknowledge disk failure.
    @android.annotation.SuppressLint("ApplySharedPref")
    private static void record(Context c,String phase,String detail) {
        boolean durable=c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("phase",phase).putString("detail",detail)
                .putLong("sample_wall_ms",System.currentTimeMillis()).commit();
        if(!durable)throw new IllegalStateException("R82_MAINTENANCE_PHASE_NOT_DURABLE");
    }
    public static String summary(Context context) {
        Context app=context.getApplicationContext();android.content.SharedPreferences p=app.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        return "STORAGE_MAINTENANCE_R82|phase="+p.getString("phase","NEVER_REQUESTED")+"|sampleWallMs="+p.getLong("sample_wall_ms",0)+
                "|explicitWindowActive="+DatabaseCoordinator.isExplicitMaintenanceActiveR82()+"|runtimeAutoRestart=false|detail="+p.getString("detail","")+
                "|"+StorageMaintenanceFileR82.backupSummary(app.getDatabasePath(AppConfig.DB_NAME))+"\n"+LegacyBackupCustodyR335.summary(app);
    }
    @Override public void onCreate(){super.onCreate();String channel="r82_database_maintenance";NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(channel,"数据库离线空间维护",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
        startForeground(982,b.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("数据库离线维护")
                .setContentText("正在校验完整副本，完成后请手动启动运行。安全换库阶段请勿强制停止。").setOngoing(true).build());
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"":intent.getAction();
        if((!ACTION_REBUILD.equals(action)&&!ACTION_CLEAN_BACKUP.equals(action)&&!ACTION_VERIFY_LEGACY_R335.equals(action)&&!ACTION_CUSTODY_EXPORT_R335.equals(action)&&!ACTION_CUSTODY_VERIFY_R335.equals(action)&&!ACTION_CUSTODY_RETIRE_R335.equals(action)&&!ACTION_CUSTODY_IMPORT_R335.equals(action)&&!ACTION_CUSTODY_RELEASE_R335.equals(action)&&!ACTION_CUSTODY_RECONCILE_R335.equals(action))||!DatabaseCoordinator.isExplicitMaintenanceActiveR82()){stopSelf();return START_NOT_STICKY;}
        if(!WORKER.compareAndSet(false,true))return START_NOT_STICKY;
        new Thread(()->{
            try{
                record(this,"WAITING_RUNTIME_STOP","runtime remains explicitly stopped");
                long end=SystemClock.elapsedRealtime()+30000;
                while(AutoFetchService.isLifecycleActiveR82()){
                    if(SystemClock.elapsedRealtime()>end)throw new IOException("runtime shutdown did not complete; source preserved");Thread.sleep(50);
                }
                System.loadLibrary("starcore_report_cp4");
                try(com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Lease memory=action.startsWith("com.ethk3.novaonepro.R335_CUSTODY_")?LegacyBackupCustodyR335.memory():null){
                if(ACTION_CUSTODY_RECONCILE_R335.equals(action)){
                    LegacyRetirementFileR335.Result result=LegacyBackupCustodyR335.reconcile(this,intent.getStringExtra("custody_case"));record(this,result.state,"case="+intent.getStringExtra("custody_case")+"; read_only_ack_reconciliation; new_unlink=false; freeDelta="+result.observedFreeDelta());
                }else if(ACTION_CUSTODY_RELEASE_R335.equals(action)){
                    LegacyBackupCustodyR335.releaseIsolated(this,intent.getStringExtra("custody_case"));record(this,"R335_VERIFIED_PRIVATE_RECOVERY_COPY_RELEASED","case="+intent.getStringExtra("custody_case")+"; original .bak/manifest/external archive unchanged");
                }else if(ACTION_CUSTODY_IMPORT_R335.equals(action)){
                    record(this,"R335_EXTERNAL_IMPORT_TO_ISOLATED_TREE","no active database replacement; exact external members and original consumers required");
                    StorageRestoreProofR31.Session imported=LegacyBackupCustodyR335.importExternal(this,android.net.Uri.parse(intent.getStringExtra("custody_tree")));String caseId=imported.custody.id();String workspace=runRestoreSessionR335(imported);
                    record(this,"R335_EXTERNAL_ISOLATED_RECOVERY_VERIFIED","case="+caseId+"; current live DB unchanged; verified recovered tree retained; "+workspace);
                }else if(ACTION_CUSTODY_EXPORT_R335.equals(action)||ACTION_CUSTODY_VERIFY_R335.equals(action)){
                    record(this,"R335_EXTERNAL_CUSTODY_VERIFYING","external write never authorizes deletion; original retained; independent restore requires admitted local capacity");
                    StorageRestoreProofR31.Session custody=ACTION_CUSTODY_EXPORT_R335.equals(action)?LegacyBackupCustodyR335.exportAndPrepare(this,intent.getStringExtra("legacy_backup_name"),android.net.Uri.parse(intent.getStringExtra("custody_tree"))):LegacyBackupCustodyR335.verifyExisting(this,intent.getStringExtra("custody_case"));
                    String caseId=custody.custody.id(),workspace=runRestoreSessionR335(custody);
                    record(this,"R335_EXTERNAL_CUSTODY_VERIFIED_RETAINED","case="+caseId+"; independent restore verified; deletion_authorization=NONE; "+workspace);
                }else if(ACTION_CUSTODY_RETIRE_R335.equals(action)){
                    LegacyRetirementFileR335.Result result=LegacyBackupCustodyR335.retire(this,intent.getStringExtra("custody_case"),intent.getStringExtra("custody_authorization"));
                    record(this,result.state,"case="+intent.getStringExtra("custody_case")+"; removed_file_length="+result.removedLength+"; observed_available_before="+result.availableBefore+"; observed_available_after="+result.availableAfter+"; observed_free_delta="+result.observedFreeDelta()+"; other originals/manifest/proofs retained");
                }else if(ACTION_VERIFY_LEGACY_R335.equals(action)){
                    String name=intent.getStringExtra("legacy_backup_name");LegacyBackupRecoveryR335.requireName(name);
                    record(this,"VERIFYING_R926_ORIGINAL_IN_PRIVATE_PROCESS","source="+name+"; active database is not replaced; original retained");
                    String workspace=collectRestoreProofR31(name);
                    record(this,"R926_ORIGINAL_RESTORE_VERIFIED_RETAINED","source="+name+"; independent original-consumer proof recorded; cleanup_authority=NONE; "+workspace);
                }else if(ACTION_CLEAN_BACKUP.equals(action)){
                    long bytes=DatabaseCoordinator.withClosedDatabaseR31(this,StorageMaintenanceFileR82::deleteVerifiedBackup);
                    record(this,"BACKUP_EXPLICITLY_REMOVED","verified_backup_file_bytes_removed="+bytes+"; actual free-space change depends on filesystem");
                }else{
                    File db=getDatabasePath(AppConfig.DB_NAME);
                    if(!new File(db.getPath()+".r82-backup-receipt").isFile()){
                        record(this,"BUILDING_VERIFIED_COPY","original source unchanged until durable verified swap");
                        StorageMaintenanceFileR82.Receipt receipt=DatabaseCoordinator.rebuildExplicitR82(this);
                        record(this,"REBUILT_BACKUP_RETAINED",receipt.summary());
                    }
                    record(this,"VERIFYING_REAL_RESTORE_PROCESS","backup remains protected; no runtime restart");
                    String workspace=collectRestoreProofR31(null);
                    record(this,"RESTORE_VERIFIED_BACKUP_RETAINED","independent Android clone startup/consumer receipt recorded; backup cleanup remains a separate explicit user action; "+workspace);
                }
                }
            }catch(Throwable failure){record(this,"FAILED_OR_RECOVERY_REQUIRED",DatabaseCoordinator.describeCauseChain(failure,2000));}
            finally{
                // Finish Android IPC before admitting a new maintenance action.
                // Admission and worker release share the original transition
                // monitor, so no active window can be left without a worker.
                try{stopForeground(true);stopSelf(startId);}
                finally{synchronized(DatabaseCoordinator.RUNTIME_TRANSITION_R82){WORKER.set(false);DatabaseCoordinator.endExplicitMaintenanceR82();}}
            }
        },"r82-explicit-offline-maintenance").start();
        return START_NOT_STICKY;
    }
    private String collectRestoreProofR31(String legacyBackup)throws Exception {
        String nonce=StorageRestoreProofR31.digest(java.util.UUID.randomUUID().toString()+"|"+SystemClock.elapsedRealtimeNanos());
        StorageRestoreProofR31.Session session=DatabaseCoordinator.withClosedDatabaseR31(this,db->legacyBackup==null?StorageRestoreProofR31.prepare(db,new File(getFilesDir(),"r31-restore-probes"),nonce,android.os.Process.myPid(),BuildConfig.SOURCE_COMMIT_R26):LegacyBackupRecoveryR335.prepare(db,new File(getFilesDir(),"r31-restore-probes"),legacyBackup,nonce,android.os.Process.myPid(),BuildConfig.SOURCE_COMMIT_R26));
        return runRestoreSessionR335(session);
    }
    private String runRestoreSessionR335(StorageRestoreProofR31.Session session)throws Exception {
        final String nonce=session.nonce;
        try{LegacyBackupCustodyR335.markProbeSubmitted(session);}catch(IOException failure){try{session.releaseLegacyBudget();}catch(IOException suppressed){failure.addSuppressed(suppressed);}throw failure;}
        CountDownLatch connected=new CountDownLatch(1),terminal=new CountDownLatch(1);AtomicReference<Exception> failure=new AtomicReference<>();
        AtomicReference<IBinder> lifetime=new AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger actualProbePid=new java.util.concurrent.atomic.AtomicInteger();AtomicReference<String> reportedResultHash=new AtomicReference<>();
        // ResultReceiver is oneway Binder: its caller PID can be zero. It is
        // only a completion hint; identity comes from the synchronous bound
        // process query and the durable private result after actual death.
        ResultReceiver receiver=new ResultReceiver(null){@Override protected void onReceiveResult(int code,Bundle result){
            try{if(code!=0||result==null)throw new IOException("WAIT_RESTORE_PROBE_NOT_VERIFIED");String hash=result.getString("result_sha256");if(!nonce.equals(result.getString("nonce"))||hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("RESTORE_RESULT_NOTICE_IDENTITY");reportedResultHash.set(hash);}
            catch(Exception rejected){failure.set(rejected);}
        }};
        ServiceConnection connection=new ServiceConnection(){
            private void disconnected(){IBinder remote=lifetime.get();if(remote!=null&&!remote.isBinderAlive())terminal.countDown();}
            @Override public void onServiceConnected(ComponentName name,IBinder remote){
                lifetime.set(remote);
                try{remote.linkToDeath(terminal::countDown,0);Parcel input=Parcel.obtain(),reply=Parcel.obtain();try{input.writeInterfaceToken(StorageRestoreProofR31.METHOD);if(!remote.transact(StorageRestoreProbeServiceR31.QUERY_PROCESS_R31,input,reply,0))throw new IOException("RESTORE_PROCESS_BINDER_QUERY_REJECTED");reply.readException();int pid=reply.readInt();if(pid<=0||pid==session.parentPid)throw new IOException("RESTORE_PROCESS_BINDER_IDENTITY");actualProbePid.set(pid);}finally{input.recycle();reply.recycle();}}
                catch(Exception rejected){failure.set(rejected);disconnected();}finally{connected.countDown();}
            }
            @Override public void onServiceDisconnected(ComponentName name){disconnected();}
            @Override public void onBindingDied(ComponentName name){failure.compareAndSet(null,new IOException("WAIT_RESTORE_BINDING_DIED"));disconnected();}
            @Override public void onNullBinding(ComponentName name){failure.set(new IOException("WAIT_RESTORE_NULL_BINDING"));connected.countDown();}
        };
        // Both original actor locks were released before Android IPC or waiting.
        Intent probe=new Intent(this,StorageRestoreProbeServiceR31.class).putExtra("nonce",nonce).putExtra("receiver",receiver);
        boolean bound;
        try{bound=bindService(probe,connection,Context.BIND_AUTO_CREATE);}catch(RuntimeException bindingFailure){session.releaseLegacyBudget();throw bindingFailure;}
        if(!bound){session.releaseLegacyBudget();throw new IOException("WAIT_RESTORE_BIND_REJECTED");}
        boolean interrupted=false;
        try{
            for(;;)try{if(connected.await(60,TimeUnit.SECONDS))break;record(this,"WAIT_RESTORE_PROCESS_BINDING","no worker has been submitted; backup retained");}catch(InterruptedException cancelled){interrupted=true;}
            if(failure.get()!=null)throw failure.get();
            try{if(startService(probe)==null)throw new IOException("WAIT_RESTORE_START_REJECTED");}
            catch(Exception notAcknowledged){
                failure.set(notAcknowledged);
                // A start failure is not proof that Android did not deliver it.
                // The child atomically refuses a late start only if no worker
                // entered; otherwise its existing worker drains normally.
                IBinder remote=lifetime.get();Parcel input=Parcel.obtain(),reply=Parcel.obtain();try{input.writeInterfaceToken(StorageRestoreProofR31.METHOD);remote.transact(StorageRestoreProbeServiceR31.ABANDON_UNSTARTED_R31,input,reply,0);reply.readException();}catch(Exception uncertain){if(remote!=null&&!remote.isBinderAlive())terminal.countDown();}finally{input.recycle();reply.recycle();}
            }
            for(;;)try{
                if(terminal.await(60,TimeUnit.SECONDS))break;
                try{record(this,"WAIT_RESTORE_PROCESS_STILL_RUNNING","timeout is not termination; backup retained; runtime remains explicitly stopped");}catch(RuntimeException journalFailure){failure.compareAndSet(null,journalFailure);}
            }catch(InterruptedException cancelled){interrupted=true;failure.compareAndSet(null,new IOException("WAIT_RESTORE_CANCELLED_AWAITING_ACTUAL_PROCESS_EXIT"));}
            if(failure.get()!=null)throw failure.get();
            if(actualProbePid.get()<=0||!session.result.isFile())throw new IOException("WAIT_RESTORE_PROCESS_EXIT_WITHOUT_VERIFIED_RESULT");
            String durableHash=StorageMaintenanceFileR82.sha256(session.result),noticeHash=reportedResultHash.get();
            if(noticeHash!=null&&!noticeHash.equals(durableHash))throw new IOException("RESTORE_RESULT_NOTICE_CHANGED");
            // Also recovers an ACK lost when the child exits. The actual PID
            // was observed by synchronous Binder RPC before starting the job.
            StorageRestoreProofR31.authenticateCallback(session,nonce,actualProbePid.get(),durableHash);
            DatabaseCoordinator.withClosedDatabaseR31(this,db->{StorageRestoreProofR31.recordCollected(session);return null;});
            try{return StorageRestoreProofR31.discardVerifiedWorkspace(session)?"probe_workspace=RELEASED":"probe_workspace=RETAINED_DEBT";}catch(IOException debt){return "probe_workspace=RETAINED_DEBT";}
        }finally{
            // The worker-submitted path above exits only after actual lifetime
            // Binder death. Neither a timeout nor a component disconnect does so.
            try{unbindService(connection);}finally{session.releaseLegacyBudget();if(interrupted)Thread.currentThread().interrupt();}
        }

    }
    @Override public IBinder onBind(Intent intent){return null;}
}
