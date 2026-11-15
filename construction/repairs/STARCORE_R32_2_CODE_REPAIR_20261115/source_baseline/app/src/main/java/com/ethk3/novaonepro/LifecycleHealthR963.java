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
            refreshInterruptionObservationR31(app);
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
        long observed=System.currentTimeMillis();
        if(Build.VERSION.SDK_INT<30){prefs(app).edit().putString("system_exits","UNAVAILABLE_API_BELOW_30").putString("r31_interruption_summary",interruptionSummaryR31(observed,0,null,"UNSUPPORTED_API_BELOW_35","UNSUPPORTED_API_BELOW_30")).apply();return;}
        ActivityManager am=(ActivityManager)app.getSystemService(Context.ACTIVITY_SERVICE);
        if(am==null){prefs(app).edit().putString("system_exits","ACTIVITY_MANAGER_UNAVAILABLE").putString("r31_interruption_summary",interruptionSummaryR31(observed,0,null,"UNKNOWN_ACTIVITY_MANAGER_UNAVAILABLE","UNKNOWN_ACTIVITY_MANAGER_UNAVAILABLE")).apply();return;}
        List<ApplicationExitInfo> exits=am.getHistoricalProcessExitReasons(app.getPackageName(),0,5);
        StringBuilder b=new StringBuilder("SYSTEM_EXIT_HISTORY|noRecordsDoesNotProveNoCrash=1\n");
        String mainProcess=app.getApplicationInfo().processName;long latestMainExit=0;String latestReason="NO_MATCHING_MAIN_PROCESS_EXIT";int count=0;
        for(ApplicationExitInfo x:exits==null?java.util.Collections.<ApplicationExitInfo>emptyList():exits){
            if(count++==5)break;
            String detail=x.getDescription();if(detail==null)detail="";if(detail.length()>600)detail=detail.substring(0,600);
            b.append("at=").append(x.getTimestamp()).append("|reason=").append(x.getReason())
                    .append("|process=").append(x.getProcessName())
                    .append("|status=").append(x.getStatus()).append("|importance=").append(x.getImportance())
                    .append("|pssKiB=").append(x.getPss()).append("|rssKiB=").append(x.getRss())
                    .append("|detail=").append(detail).append('\n');
            if(mainProcess!=null&&mainProcess.equals(x.getProcessName())&&x.getTimestamp()>latestMainExit){latestMainExit=x.getTimestamp();latestReason="ApplicationExitInfo.reason="+x.getReason()+"（USER_REQUESTED等原因不能独自认定force-stop）";}
        }
        Boolean forceStopped=null;String startSource=Build.VERSION.SDK_INT<35?"UNSUPPORTED_API_BELOW_35":"UNKNOWN_NO_MATCHING_CURRENT_PROCESS_START";
        if(Build.VERSION.SDK_INT>=35)try{forceStopped=Api35R31.currentStartWasForceStopped(am,mainProcess);if(forceStopped!=null)startSource="ApplicationStartInfo.wasForceStopped/current_pid_and_process";}catch(RuntimeException failed){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failed);if(fatal!=null)throw fatal;startSource="UNKNOWN_START_HISTORY:"+failed.getClass().getSimpleName();}
        prefs(app).edit().putString("system_exits",b.toString()).putString("r31_interruption_summary",interruptionSummaryR31(observed,latestMainExit,forceStopped,startSource,latestReason)).putLong("r31_exit_observed_at",observed).apply();
    }
    @androidx.annotation.RequiresApi(35)
    private static final class Api35R31 {
        static Boolean currentStartWasForceStopped(ActivityManager manager,String process){
            List<android.app.ApplicationStartInfo> starts=manager.getHistoricalProcessStartReasons(5);if(starts==null)return null;int count=0;
            for(android.app.ApplicationStartInfo start:starts){if(count++==5)break;if(start.getPid()==android.os.Process.myPid()&&process!=null&&process.equals(start.getProcessName()))return start.wasForceStopped();}return null;
        }
    }
    public static String interruptionSummaryR31(long observed,long latestExit,Boolean forceStopped,String startSource,String exitReason){
        String interval=latestExit<=0?"UNKNOWN（没有主进程退出样本，不代表无中断）":latestExit>observed?"UNKNOWN_WALL_CLOCK_ORDER（时钟变化，不推算停跑时长）":"UNOBSERVED_INTERVAL["+latestExit+","+observed+"]（退出到本次进程观察；区间内实际运行未知）";
        return "最近启动前force-stop="+(forceStopped==null?startSource.startsWith("UNSUPPORTED")?"UNSUPPORTED":"UNKNOWN":forceStopped)+"\n启动来源="+startSource+"\n主进程最近退出="+latestExit+"｜"+exitReason+"\n运行缺口="+interval+"\n观察wallMs="+observed+"\nforce-stop不能保证自启，需用户再次交互；重新打开后允许补回真实开奖历史，不补造过去赛前预测，也不把重启补作连续运行";
    }
    /** Bounded system-history query; only the existing lifecycle IO owner calls this. */
    public static void refreshInterruptionObservationR31(Context context){
        if(context==null)return;if(Looper.myLooper()==Looper.getMainLooper()&&Looper.myLooper()!=null)throw new IllegalStateException("R31_EXIT_HISTORY_REQUIRES_IO_OWNER");
        Context app=context.getApplicationContext();if(app==null)app=context;
        try{recordSystemExits(app);}catch(Exception failed){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failed);if(fatal!=null)throw fatal;prefs(app).edit().putString("system_exits","READ_ERROR:"+failed.getClass().getSimpleName()).putString("r31_interruption_summary",interruptionSummaryR31(System.currentTimeMillis(),0,null,"UNKNOWN_READ_ERROR:"+failed.getClass().getSimpleName(),"UNKNOWN_READ_ERROR")).apply();}
    }
    public static String interruptionSummaryR31(Context context){if(context==null)return "UNKNOWN_CONTEXT_NULL";return prefs(context.getApplicationContext()).getString("r31_interruption_summary","UNKNOWN_NOT_YET_READ（后台正在采集系统退出与启动历史）");}
    public static String summary(Context context){
        SharedPreferences p=prefs(context.getApplicationContext());
        return "R963_LIFECYCLE_HEALTH|stallIsObservedDelayNotAnrVerdict=1\n"+p.getString("system_exits","NOT_READ")
                +"\nmainStallAt="+p.getLong("stall_at",0)+"|delayMs="+p.getLong("stall_ms",0)+"|version="+p.getString("stall_version","")
                +"\nmainStack="+p.getString("stall_stack","")+"\nrecoveryError="+p.getString("recovery_error","")+"\n"+interruptionSummaryR31(context);
    }
}
