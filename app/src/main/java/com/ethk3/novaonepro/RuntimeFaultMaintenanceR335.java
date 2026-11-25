package com.ethk3.novaonepro;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Service-owned bounded background delivery. Guards only signal; file work never runs on a guard Looper. */
public final class RuntimeFaultMaintenanceR335 implements AutoCloseable {
    private final AtomicBoolean closed=new AtomicBoolean(),queued=new AtomicBoolean();
    private volatile long nextRecoveryElapsed, nextAuditElapsed;private boolean lastWasRecovery;
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0L,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1),task->new Thread(()->{
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);task.run();
        },"starcore_r335_fault_original"),new ThreadPoolExecutor.AbortPolicy());
    public void signal(DbStore store){
        if(store==null||closed.get())return;
        long now=android.os.SystemClock.elapsedRealtime();RuntimeFaultArchiveB01.View view=RuntimeFaultArchiveB01.observation();
        boolean archiveWasWoken = view.state == RuntimeFaultArchiveB01.State.NO_ELIGIBLE_VERIFIED
                && view.acknowledged != RuntimeFaultArchiveB01.requestedGeneration();
        if((!archiveWasWoken && now<view.nextAttemptElapsedMs && now<nextRecoveryElapsed && now<nextAuditElapsed)
                || !queued.compareAndSet(false,true))return;
        try{executor.execute(()->{
            try{if(!closed.get()&&!Thread.currentThread().isInterrupted()){
                long started=android.os.SystemClock.elapsedRealtime();
                boolean recovery=started>=nextRecoveryElapsed&&(!lastWasRecovery||started<RuntimeFaultArchiveB01.observation().nextAttemptElapsedMs);
                if(recovery){String state=DataRetentionGovernorR900.runCheckpointRecoveryR335(store,started+10_000L);nextRecoveryElapsed=android.os.SystemClock.elapsedRealtime()+("NO_PERSISTED_REQUEST".equals(state)?60_000L:5_000L);}
                else DataRetentionGovernorR900.runFaultMaintenanceR335(store,started+10_000L);
                lastWasRecovery=recovery;
                long auditAt = android.os.SystemClock.elapsedRealtime();
                if (auditAt >= nextAuditElapsed) {
                    nextAuditElapsed = auditAt + 5_000L;
                    RuntimeFaultOriginalStoreR335.auditSlice(store, auditAt + 200L);
                }
            }}
            catch(RuntimeException failure){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;android.util.Log.w("R335FaultOriginal","maintenance unavailable; original retained",failure);}
            finally{queued.set(false);}
        });}catch(RejectedExecutionException rejected){queued.set(false);}
    }
    public boolean hasOutstanding(){return queued.get();}
    @Override public void close(){if(closed.compareAndSet(false,true))executor.shutdownNow();}
}
