package com.ethk3.novaonepro;

import android.app.ActivityManager;
import android.app.ApplicationExitInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import java.util.List;

/** UI lifecycle IO and bounded system-exit evidence. No business/model execution or DB writer. */
public final class LifecycleHealthR963 {
    private static final String PREF="r963_lifecycle_health";
    private static Handler worker;
    private static volatile long mainAck;
    private static boolean pingPending;
    private static long pingSent;
    private static boolean stallRecorded;
    private static long lastRecovery;
    private LifecycleHealthR963(){}
    public static synchronized void install(Context context){
        if(worker!=null||context==null)return;
        final Context app=context.getApplicationContext();
        HandlerThread thread=new HandlerThread("R963-Lifecycle-IO",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();worker=new Handler(thread.getLooper());
        final Handler main=new Handler(Looper.getMainLooper());
        worker.post(new Runnable(){public void run(){
            try{recordSystemExits(app);}catch(Exception e){prefs(app).edit().putString("system_exits","READ_ERROR:"+e.getClass().getSimpleName()).apply();}
        }});
        worker.post(new Runnable(){public void run(){
            long now=SystemClock.uptimeMillis();
            if(!pingPending){
                pingPending=true;pingSent=now;stallRecorded=false;
                main.post(new Runnable(){public void run(){mainAck=SystemClock.uptimeMillis();}});
            }else if(mainAck>=pingSent){pingPending=false;}
            else if(now-pingSent>=4000L&&!stallRecorded){
                stallRecorded=true;
                StringBuilder stack=new StringBuilder();
                StackTraceElement[] frames=Looper.getMainLooper().getThread().getStackTrace();
                for(int i=0;i<frames.length&&i<24;i++)stack.append(frames[i]).append('\n');
                prefs(app).edit().putLong("stall_at",System.currentTimeMillis()).putLong("stall_ms",now-pingSent)
                        .putString("stall_stack",stack.toString()).putString("stall_version",ReleaseManifestR926.VERSION_NAME).apply();
            }
            worker.postDelayed(this,1000L);
        }});
    }
    public static boolean post(Context context,Runnable task){install(context);return worker!=null&&worker.post(task);}
    public static void keepService(final Context context,final String reason){
        final Context app=context.getApplicationContext();
        post(app,new Runnable(){public void run(){
            long now=SystemClock.elapsedRealtime();
            if(now-lastRecovery<1500L)return;lastRecovery=now;
            try{
                ServiceHeartbeatStore hb=new ServiceHeartbeatStore(app);
                if(hb.isUserEnabled()){
                    BootReceiver.scheduleAllRecovery(app,"ui_"+reason);
                    boolean reconciled=BootReceiver.reconcileAutoFetchService(app,"ui_"+reason+"_keep_alive_r922");
                    hb.markBusinessStage("UI_EXIT_FORCE_RECONCILE_R922",reason+"|requested="+(reconciled?1:0)+"|offMain=1");
                }
            }catch(Exception e){prefs(app).edit().putString("recovery_error",e.getClass().getSimpleName()+":"+e.getMessage()).apply();}
        }});
    }
    private static SharedPreferences prefs(Context app){return app.getSharedPreferences(PREF,Context.MODE_PRIVATE);}
    private static void recordSystemExits(Context app){
        if(Build.VERSION.SDK_INT<30){prefs(app).edit().putString("system_exits","UNAVAILABLE_API_BELOW_30").apply();return;}
        ActivityManager am=(ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
        if(am==null){prefs(app).edit().putString("system_exits","ACTIVITY_MANAGER_UNAVAILABLE").apply();return;}
        List<ApplicationExitInfo> exits=am.getHistoricalProcessExitReasons(app.getPackageName(),0,5);
        StringBuilder b=new StringBuilder("SYSTEM_EXIT_HISTORY|noRecordsDoesNotProveNoCrash=1\n");
        for(ApplicationExitInfo x:exits){
            String detail=x.getDescription();if(detail==null)detail="";if(detail.length()>600)detail=detail.substring(0,600);
            b.append("at=").append(x.getTimestamp()).append("|reason=").append(x.getReason())
                    .append("|status=").append(x.getStatus()).append("|importance=").append(x.getImportance())
                    .append("|pssKiB=").append(x.getPss()).append("|rssKiB=").append(x.getRss())
                    .append("|detail=").append(detail).append('\n');
        }
        prefs(app).edit().putString("system_exits",b.toString()).apply();
    }
    public static String summary(Context context){
        SharedPreferences p=prefs(context.getApplicationContext());
        return "R963_LIFECYCLE_HEALTH|stallIsObservedDelayNotAnrVerdict=1\n"+p.getString("system_exits","NOT_READ")
                +"\nmainStallAt="+p.getLong("stall_at",0)+"|delayMs="+p.getLong("stall_ms",0)+"|version="+p.getString("stall_version","")
                +"\nmainStack="+p.getString("stall_stack","")+"\nrecoveryError="+p.getString("recovery_error","");
    }
}
