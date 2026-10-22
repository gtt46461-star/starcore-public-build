package com.ethk3.novaonepro.strategyv2;

import android.app.job.*;
import android.os.*;
import com.ethk3.novaonepro.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Main callbacks only start/cancel a worker. Each numeric slice is <=2 s and checkpointed. */
public final class ReplayBurstJobServiceSU1 extends JobService {
    private static final AtomicBoolean RUNNING=new AtomicBoolean();
    private final ConcurrentHashMap<JobParameters,AtomicBoolean> active=new ConcurrentHashMap<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->new Thread(r,"su1-charging-replay"));
    @Override public boolean onStartJob(JobParameters params){
        AtomicBoolean cancelled=new AtomicBoolean();active.put(params,cancelled);
        worker.execute(()->{
            boolean retry=true;
            if(!RUNNING.compareAndSet(false,true)){finish(params,cancelled,true);return;}
            DbStore store=null;
            try{
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                if(!new ServiceHeartbeatStore(getApplicationContext()).isUserEnabled()){retry=false;return;}
                store=new DbStore(getApplicationContext());long stop=SystemClock.elapsedRealtime()+60000;
                while(!cancelled.get()&&SystemClock.elapsedRealtime()<stop&&!StrategySchemaSU1.ready(store.readable())){
                    if(RealtimePressureGateR811.isUnderPressure()||SettlementExecutionGateR819.isActive())return;
                    ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorForResearchR952(),8);
                }
                if(!StrategySchemaSU1.ready(store.readable()))return;
                LiveShadowLoopSU1.recover(store,32);
                final DbStore owner=store;
                Models.Draw last=store.getLatestDraw();if(last!=null&&TargetIssueResolver.isValid(last.issue))store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_DAILY_CLOSE",db->{LiveShadowLoopSU1.closeDays(db,Integer.parseInt(last.issue.substring(0,8)));return 0;});
                try(CampaignStateSU1 state=CampaignStateSU1.open(store)){
                    if(state==null){retry=false;return;}
                    while(!cancelled.get()&&!state.foundry.done()&&SystemClock.elapsedRealtime()<stop){
                        BudgetTokenSU1 budget=new BudgetTokenSU1(2000,30000000,()->cancelled.get()||SettlementExecutionGateR819.isActive()||RealtimePressureGateR811.isUnderPressure());
                        try{StrategyFoundrySU1.Attempt attempt=state.foundry.next(budget);state.checkpoint(attempt);}
                        catch(BudgetTokenSU1.Yield yielded){state.checkpoint(null);if(cancelled.get()||SettlementExecutionGateR819.isActive()||RealtimePressureGateR811.isUnderPressure())return;}
                    }
                    retry=!state.foundry.done();
                }
            }catch(RuntimeException failure){if(store!=null)StrategyRuntimeSU1.failure(store,"BURST_FAILED",failure);else android.util.Log.w("STARCORE_SU1","burst startup failed",failure);}
            finally{if(store!=null)store.close();RUNNING.set(false);finish(params,cancelled,retry);}
        });
        return true;
    }
    private void finish(JobParameters params,AtomicBoolean cancelled,boolean retry){new Handler(Looper.getMainLooper()).post(()->{if(active.remove(params,cancelled)&&!cancelled.get())jobFinished(params,retry);});}
    @Override public boolean onStopJob(JobParameters params){AtomicBoolean cancelled=active.remove(params);if(cancelled!=null)cancelled.set(true);return true;}
    @Override public void onDestroy(){for(AtomicBoolean c:active.values())c.set(true);worker.shutdown();super.onDestroy();}
}
