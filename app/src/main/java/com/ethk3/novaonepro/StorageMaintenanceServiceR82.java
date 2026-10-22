package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.os.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Explicit offline maintenance. Never schedules or restarts live prediction. */
public final class StorageMaintenanceServiceR82 extends Service {
    public static final String ACTION_REBUILD="com.ethk3.novaonepro.R82_REBUILD_DATABASE";
    public static final String ACTION_CLEAN_BACKUP="com.ethk3.novaonepro.R82_DELETE_VERIFIED_BACKUP";
    private static final String PREF="r82_storage_maintenance";
    private static final AtomicBoolean WORKER=new AtomicBoolean();
    public static void request(Context context,boolean cleanup) {
        Context app=context.getApplicationContext();
        synchronized(DatabaseCoordinator.RUNTIME_TRANSITION_R82) {
            new ServiceHeartbeatStore(app).setUserEnabled(false);
            DatabaseCoordinator.beginExplicitMaintenanceR82(app);
        }
        try{
            app.stopService(new Intent(app,AutoFetchService.class));
            Intent intent=new Intent(app,StorageMaintenanceServiceR82.class).setAction(cleanup?ACTION_CLEAN_BACKUP:ACTION_REBUILD);
            if(Build.VERSION.SDK_INT>=26)app.startForegroundService(intent);else app.startService(intent);
        }catch(RuntimeException failure){DatabaseCoordinator.endExplicitMaintenanceR82();record(app,"START_FAILED",failure.toString());throw failure;}
    }
    private static void record(Context c,String phase,String detail) {
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString("phase",phase).putString("detail",detail)
                .putLong("sample_wall_ms",System.currentTimeMillis()).commit();
    }
    public static String summary(Context context) {
        Context app=context.getApplicationContext();android.content.SharedPreferences p=app.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        return "STORAGE_MAINTENANCE_R82|phase="+p.getString("phase","NEVER_REQUESTED")+"|sampleWallMs="+p.getLong("sample_wall_ms",0)+
                "|explicitWindowActive="+DatabaseCoordinator.isExplicitMaintenanceActiveR82()+"|runtimeAutoRestart=false|detail="+p.getString("detail","")+
                "|"+StorageMaintenanceFileR82.backupSummary(app.getDatabasePath(AppConfig.DB_NAME));
    }
    @Override public void onCreate(){super.onCreate();String channel="r82_database_maintenance";NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(channel,"数据库离线空间维护",NotificationManager.IMPORTANCE_LOW));
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
        startForeground(982,b.setSmallIcon(android.R.drawable.stat_notify_sync).setContentTitle("数据库离线维护")
                .setContentText("正在校验完整副本，完成后请手动启动运行。安全换库阶段请勿强制停止。").setOngoing(true).build());
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"":intent.getAction();
        if((!ACTION_REBUILD.equals(action)&&!ACTION_CLEAN_BACKUP.equals(action))||!DatabaseCoordinator.isExplicitMaintenanceActiveR82()){stopSelf();return START_NOT_STICKY;}
        if(!WORKER.compareAndSet(false,true))return START_NOT_STICKY;
        new Thread(()->{
            try{
                record(this,"WAITING_RUNTIME_STOP","runtime remains explicitly stopped");
                long end=SystemClock.elapsedRealtime()+30000;
                while(AutoFetchService.isLifecycleActiveR82()){
                    if(SystemClock.elapsedRealtime()>end)throw new IOException("runtime shutdown did not complete; source preserved");Thread.sleep(50);
                }
                System.loadLibrary("starcore_report_cp4");
                if(ACTION_CLEAN_BACKUP.equals(action)){
                    try(SourceReadLeaseR82 lease=SourceReadLeaseR82.exclusive(getDatabasePath(AppConfig.DB_NAME))){
                        long bytes=StorageMaintenanceFileR82.deleteVerifiedBackup(getDatabasePath(AppConfig.DB_NAME));
                        record(this,"BACKUP_EXPLICITLY_REMOVED","verified_backup_file_bytes_removed="+bytes+"; actual free-space change depends on filesystem");
                    }
                }else{
                    record(this,"BUILDING_VERIFIED_COPY","original source unchanged until durable verified swap");
                    StorageMaintenanceFileR82.Receipt receipt=DatabaseCoordinator.rebuildExplicitR82(this);
                    record(this,"COMPLETE_BACKUP_RETAINED",receipt.summary());
                }
            }catch(Throwable failure){record(this,"FAILED_OR_RECOVERY_REQUIRED",DatabaseCoordinator.describeCauseChain(failure,2000));}
            finally{DatabaseCoordinator.endExplicitMaintenanceR82();WORKER.set(false);stopForeground(true);stopSelf(startId);}
        },"r82-explicit-offline-maintenance").start();
        return START_NOT_STICKY;
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
