package com.ethk3.novaonepro.strategyv2;

import android.app.job.*;
import android.content.*;
import android.os.Build;

/** Periodic charging/idle work; the JobService also yields to settlement and live pressure. */
public final class ReplayBurstSchedulerSU1 {
    public static final int JOB_ID=0x5131;
    private ReplayBurstSchedulerSU1() {}
    public static void schedule(Context context){
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler==null)return;
        JobInfo.Builder job=new JobInfo.Builder(JOB_ID,new ComponentName(context,ReplayBurstJobServiceSU1.class)).setRequiresCharging(true).setPersisted(true).setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE).setPeriodic(15*60*1000L);
        if(Build.VERSION.SDK_INT>=23)job.setRequiresDeviceIdle(true);
        if(scheduler.schedule(job.build())!=JobScheduler.RESULT_SUCCESS)throw new IllegalStateException("SU1_JOB_SCHEDULE_REJECTED");
    }
    public static void cancel(Context context){JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(scheduler!=null)scheduler.cancel(JOB_ID);}
}
