package com.ethk3.novaonepro.strategyv2;
import android.app.job.JobService;
import android.app.job.JobParameters;
/** Compatibility component for already persisted jobs; never starts a second database owner. */
public final class ReplayBurstJobServiceSU1 extends JobService {
    @Override public boolean onStartJob(JobParameters p){ReplayBurstSchedulerSU1.cancel(getApplicationContext());RuntimeCoordinatorR26.wake();return false;}
    @Override public boolean onStopJob(JobParameters p){return false;}
}
