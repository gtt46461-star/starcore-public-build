package com.ethk3.novaonepro;

import android.app.job.JobParameters;
import android.app.job.JobService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Android main callbacks only KICK the existing RecoveryCoordinator, never access preferences/DB. */
public final class ServiceWatchdogJobService extends JobService {
    public static final String OUMAN_GALAXY_REBUILD_VERSION=AppConfig.OUMAN_GALAXY_REBUILD_VERSION;
    public static final String VERSION=AppConfig.R36_RUNTIME_REPAIR_VERSION+"|CP4_JOB_KICK_ONLY";
    private final ConcurrentHashMap<JobParameters,AtomicBoolean> active=new ConcurrentHashMap<>();
    @Override public boolean onStartJob(final JobParameters params){
        final AtomicBoolean cancelled=new AtomicBoolean();
        final AtomicBoolean retry=new AtomicBoolean(true);
        active.put(params,cancelled);
        RecoveryDispatchR965.submit(()->{
            if(cancelled.get())return;
            ServiceHeartbeatStore state=new ServiceHeartbeatStore(getApplicationContext());
            if(!state.isUserEnabled()){
                retry.set(false);BootReceiver.cancelAllRecovery(getApplicationContext(),"CP4_JOB_USER_DISABLED");return;
            }
            state.markJobTriggered(state.getHeartbeatAgeMs());
            if(cancelled.get())return;
            RecoveryCoordinatorR815.Result result=RecoveryCoordinatorR815.request(
                    getApplicationContext(),"CP4_JOB_KICK",0L);
            retry.set(!result.accepted);
            state.markJobFinished(result.status);
        },()->{
            if(active.remove(params,cancelled)&&!cancelled.get())jobFinished(params,retry.get());
        },Math.min(8000L,AppConfig.B07_JOB_WORK_TIMEOUT_MS));
        return true;
    }
    @Override public boolean onStopJob(JobParameters params){
        AtomicBoolean cancelled=active.remove(params);
        if(cancelled!=null)cancelled.set(true);
        return true; // Framework reschedules; no synchronous preference commit on main thread.
    }
}
