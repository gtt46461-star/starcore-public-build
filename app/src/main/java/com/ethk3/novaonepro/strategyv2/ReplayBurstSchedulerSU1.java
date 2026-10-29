package com.ethk3.novaonepro.strategyv2;
import android.app.job.JobScheduler;
import android.content.Context;
/** Legacy persisted job is retired. User-opened AutoFetchService owns continuous research. */
public final class ReplayBurstSchedulerSU1 {
    public static final int JOB_ID=0x5131;
    private ReplayBurstSchedulerSU1() {}
    public static void schedule(Context context){cancel(context);RuntimeCoordinatorR26.wake();}
    public static void cancel(Context context){JobScheduler jobs=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);if(jobs!=null)jobs.cancel(JOB_ID);}
}
